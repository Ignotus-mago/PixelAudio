/*
 *  Copyright (c) 2024 - 2025 by Paul Hertz <ignotus@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.paulhertz.pixelaudio.sampler;

import ddf.minim.MultiChannelBuffer;
import ddf.minim.AudioOutput;
import ddf.minim.UGen;
import net.paulhertz.pixelaudio.schedule.AudioUtility;
import net.paulhertz.pixelaudio.schedule.AudioScheduler;

import java.util.*;

/**
 * UGen-based sampler that plays multiple PASamplerVoice instances
 * from a single shared mono buffer (channel 0 of a MultiChannelBuffer)
 * which is an array of floating point samples over (-1.0..1.0).
 * <p>
 * Features:
 * <ul>
 *    <li>Shared buffer (no duplication)</li>
 *    <li>Polyphony with voice pooling</li>
 *    <li>Looping (global default + per-voice)</li>
 *    <li>ADSR per voice via ADSRParams</li>
 *    <li>Oldest-first voice recycling, optional smooth stealing</li>
 *    <li>Thread-safe triggering</li>
 *    <li>Presets for noise reduction with MixProfile and {@link PASamplerInstrumentPool} cycleMixProfile().</li>
 * </ul>
 * 
 * Automatically patches to the provided AudioOutput.
 * 
 */
public class PASharedBufferSampler extends UGen implements PASampler {
    // ------------------------------------------------------------------------
    // Internal scheduled-play payload
    // ------------------------------------------------------------------------
    private static final class ScheduledPlay {
        final int samplePos;
        final int sampleLen;
        final float amplitude;
        final ADSRParams env;
        final float pitch;
        final float pan;
        final boolean looping;
        final boolean wrapAround;

        ScheduledPlay(int samplePos, int sampleLen, float amplitude,
                ADSRParams env, float pitch, float pan, boolean looping, boolean wrapAround) {
            this.samplePos = samplePos;
            this.sampleLen = sampleLen;
            this.amplitude = amplitude;
            this.env = env;
            this.pitch = pitch;
            this.pan = pan;
            this.looping = looping;
            this.wrapAround = wrapAround;
        }
    }

    /** Mono source buffer, typically channel 0 from a MultiChannelBuffer. */
    private float[] buffer;
    /** Cached source buffer length in samples. */
    private int bufferLen;
    /** Sample rate of the source buffer in Hz. */
    private float playbackSampleRate;

    /** Audio output this sampler is patched to. */
    private final AudioOutput out;
    /** Voice pool used for polyphonic sample playback. */
    private final List<PASamplerVoice> voices = new ArrayList<>();
    /** Maximum number of simultaneous voices. */
    private int maxVoices = 32;
    /** Default looping state for newly triggered voices. */
    private boolean globalLooping = false;
    /** True to wrap source-buffer reads for newly triggered voices. */
    private boolean wrapAround = false;
    /** True to release stolen voices smoothly instead of stopping them immediately. */
    private boolean smoothSteal = true;
    
    /** Smoothed mix-normalization gain. */
    private float mixNorm = 1f; 
    
    /** Master linear gain applied to mixed output. */
    private volatile float masterGain = 1f;
    
    /** Enables diagnostic voice-trigger logging when true. */
    protected boolean DEBUG = false;

    /** Sample-accurate scheduler for launching sampler voices. */
    private final AudioScheduler<ScheduledPlay> scheduler = new AudioScheduler<>();

    /** Absolute sample counter advanced by the audio callback. */
    private long sampleCursor = 0L;
 
    /**
     * Mix-density normalization profiles for polyphonic sampler output,
     * used to control noise and distortion. Described in example code
     * {@link net.paulhertz.pixelaudio.example.TutorialOne_03_Drawing TutorialOne_03_Drawing},
     * which allows you to listen to the effects of different MixProfiles.
     */
    public enum MixProfile {
        /** Minimal normalization and limiting. */
        TRANSPARENT(0.50f, 0.08f, 1.35f, 1.00f),
        /** Moderate normalization for general playback. */
        BALANCED(0.60f, 0.10f, 1.60f, 0.60f),
        /** Stronger normalization for dense voice clusters. */
        PROTECTIVE(0.70f, 0.14f, 1.25f, 0.35f);

        /** targetNorm = 1 / activeWeight^normExponent */
        public final float normExponent;

        /** smoothing coefficient for mixNorm */
        public final float alpha;

        /** drive for softClipSoftsign */
        public final float drive;

        /**
         * How much a releasing voice contributes to density.
         * 1.0 = count fully like active voices
         * 0.0 = ignore release tails in density estimate
         */
        public final float releaseWeight;

        MixProfile(float normExponent, float alpha, float drive, float releaseWeight) {
            this.normExponent = normExponent;
            this.alpha = alpha;
            this.drive = drive;
            this.releaseWeight = releaseWeight;
        }
    }

    private volatile MixProfile mixProfile = MixProfile.BALANCED;
   

    /**
     * Construct a sampler over a shared MultiChannelBuffer.
     * Automatically patches to the provided AudioOutput.
     *
     * @param multiBuffer   shared source buffer (mono or stereo)
     * @param sampleRate    sample rate of the buffer
     * @param out           target AudioOutput for playback
     */
    public PASharedBufferSampler(MultiChannelBuffer multiBuffer, float sampleRate, AudioOutput out) {
        this.buffer = Arrays.copyOf(multiBuffer.getChannel(0), multiBuffer.getBufferSize());
        this.bufferLen = buffer.length;
        this.playbackSampleRate = sampleRate;
        this.out = out;
        this.patch(out);
    }
    
    /**
     * Constructs a sampler over a shared buffer with explicit polyphony.
     *
     * @param multiBuffer          shared source buffer
     * @param playbackSampleRate   sample rate of the source buffer in Hz
     * @param out                  target AudioOutput for playback
     * @param maxVoices            maximum simultaneous voices
     */
    public PASharedBufferSampler(MultiChannelBuffer multiBuffer, float playbackSampleRate, AudioOutput out, int maxVoices) {
    	this.buffer = Arrays.copyOf(multiBuffer.getChannel(0), multiBuffer.getBufferSize());
       	this.bufferLen = buffer.length;
    	this.playbackSampleRate = playbackSampleRate;
    	this.out = out;
    	this.maxVoices = Math.max(1, maxVoices);
       	for (int i = 0; i < this.maxVoices; i++) {
    		voices.add(new PASamplerVoice(buffer, playbackSampleRate));
    	}
    	this.patch(out);   	
    }
    

    /**
     * Constructs a sampler and infers playback sample rate from the output.
     *
     * @param multiBuffer   shared source buffer
     * @param out           target AudioOutput for playback
     */
    public PASharedBufferSampler(MultiChannelBuffer multiBuffer, AudioOutput out) {
        this(multiBuffer, (out != null ? out.sampleRate() : 44100), out);
    }

    /**
     * Plays a buffer region immediately.
     *
     * <p>The requested {@code samplePos} and {@code sampleLen} are normalized against the
     * current source buffer before a voice is activated. This method starts playback from the
     * calling thread's request as soon as the audio callback next renders this sampler; use
     * {@link #startAtSampleTime(int, int, float, ADSRParams, float, float, long)} when the
     * launch must align to a specific sampler-clock sample.</p>
     *
     * @param samplePos   source-buffer index to start playback
     * @param sampleLen   requested source-buffer duration in samples
     * @param amplitude   per-voice gain multiplier
     * @param env         ADSR envelope parameters, or null for no voice envelope
     * @param pitch       playback-rate multiplier
     * @param pan         stereo pan position
     * @return computed voice duration in output samples, or 0 if playback could not start
     */
     @Override
    public synchronized int play(int samplePos, int sampleLen, float amplitude,
                                 ADSRParams env, float pitch, float pan) {
        int[] range = normalizeRange(samplePos, sampleLen);
        if (range == null) return 0;
        PASamplerVoice v = getAvailableVoice();
        if (v == null) return 0;
        v.activate(range[0], range[1], amplitude, env, pitch, pan, globalLooping, wrapAround);
        int eventSamples = computeEventSamples(range[0], range[1], env, pitch);
        float bufferReadSamples = range[1] * Math.abs(pitch);
        float durationMS = eventSamples / playbackSampleRate * 1000f;
        // information to be shared later
        PlaybackInfo info = new PlaybackInfo(
            v.getVoiceId(),
            eventSamples,
            bufferReadSamples,
            durationMS,
            globalLooping,
            sampleCursor,
            playbackSampleRate
        );
        // debugging
        if (DEBUG) 
            System.out.printf("[Voice %d] eventDuration=%d samples (%.2f ms)%n",
            v.getVoiceId(), eventSamples,
            eventSamples / playbackSampleRate * 1000f);        
        return eventSamples;
    }

    /**
     * Schedules a sampler voice to start at an absolute sample time.
     *
     * <p>{@code startSample} is measured on this sampler's local audio-thread clock, returned
     * by {@link #getCurrentSampleTime()}. The event is enqueued through {@link AudioScheduler}
     * and activated inside {@link #uGenerate(float[])} when the clock reaches that sample.</p>
     *
     * @param samplePos     source-buffer index to start playback
     * @param sampleLen     requested source-buffer duration in samples
     * @param amplitude     per-voice gain multiplier
     * @param env           ADSR envelope parameters, or null for no voice envelope
     * @param pitch         playback-rate multiplier
     * @param pan           stereo pan position
     * @param startSample   absolute sample time on this sampler's local clock
     */
    @Override
    public synchronized void startAtSampleTime(int samplePos, int sampleLen, float amplitude,
            ADSRParams env, float pitch, float pan, long startSample) {
        int[] range = normalizeRange(samplePos, sampleLen);
        if (range == null) return;
        scheduler.schedulePoint(startSample,
                new ScheduledPlay(range[0], range[1], amplitude, env, pitch, pan,
                        globalLooping, wrapAround));
    }

    /**
     * Schedules a sampler voice to start after a sample delay from the current sampler clock.
     *
     * @param samplePos      source-buffer index to start playback
     * @param sampleLen      requested source-buffer duration in samples
     * @param amplitude      per-voice gain multiplier
     * @param env            ADSR envelope parameters, or null for no voice envelope
     * @param pitch          playback-rate multiplier
     * @param pan            stereo pan position
     * @param delaySamples   delay from the current sampler clock, clamped to 0 or greater
     */
    @Override
    public synchronized void startAfterDelaySamples(int samplePos, int sampleLen, float amplitude,
            ADSRParams env, float pitch, float pan, long delaySamples) {
        long startSample = sampleCursor + Math.max(0L, delaySamples);
        startAtSampleTime(samplePos, sampleLen, amplitude, env, pitch, pan, startSample);
    }

    /**
     * Returns the sampler-local audio-thread sample clock.
     *
     * <p>The clock advances once for each {@link #uGenerate(float[])} call. It is suitable
     * for scheduling future events on this sampler; applications that need multiple engines
     * to align should choose one shared transport clock and convert all event times to that
     * same absolute sample domain.</p>
     *
     * @return current sampler-local sample cursor
     */
    @Override
    public synchronized long getCurrentSampleTime() {
        return sampleCursor;
    }

    /**
     * Clears pending scheduled starts without stopping active voices.
     */
    @Override
    public synchronized void clearScheduled() {
        scheduler.clear();
    }

    /**
     * Validates and clamps a source-buffer playback range.
     *
     * @param samplePos   requested start position in the source buffer
     * @param sampleLen   requested playback length in source-buffer samples
     * @return a two-element array where index 0 is the clamped start position and index 1
     *         is the clamped length, or null when the request cannot produce playback
     */
    private int[] normalizeRange(int samplePos, int sampleLen) {
        if (buffer == null || bufferLen <= 0 || sampleLen <= 0 || samplePos >= bufferLen) return null;
        int pos = Math.max(0, samplePos);
        int len = sampleLen;
        if (!wrapAround && pos + len > bufferLen) len = bufferLen - pos;
        if (len <= 0) return null;
        return new int[] { pos, len };
    }

    /**
     * Computes the expected rendered duration of a voice in samples.
     *
     * @param samplePos   normalized source-buffer start position
     * @param sampleLen   normalized source-buffer playback length
     * @param env         ADSR envelope parameters
     * @param pitch       playback-rate multiplier
     * @return expected voice duration in samples, including release where applicable
     */
    private int computeEventSamples(int samplePos, int sampleLen, ADSRParams env, float pitch) {
        return PlaybackInfo.computeVoiceDuration(
                samplePos,
                sampleLen,
                bufferLen,
                pitch,
                env,
                globalLooping,
                playbackSampleRate,
                wrapAround
            );
    }

    /**
     * Get a free voice, or recycle the oldest active one if at the polyphony limit.
     */
    private PASamplerVoice getAvailableVoice() {
        // 1) Free voice first
        for (PASamplerVoice v : voices) {
            if (!v.isActive() && !v.isReleasing()) {
                return v;
            }
        }
        // 2) Allocate if under limit
        if (voices.size() < maxVoices) {
            PASamplerVoice v = new PASamplerVoice(buffer, playbackSampleRate);
            voices.add(v);
            return v;
        }
        // 3) Recycle oldest
        PASamplerVoice oldest = null;
        for (PASamplerVoice v : voices) {
            if (v.isActive() && (oldest == null || v.getVoiceId() < oldest.getVoiceId())) {
                oldest = v;
            }
        }
        if (oldest != null) {
            // System.out.println("-- sampler voice recycle: stealing oldest voice " + oldest.getVoiceId());
            if (smoothSteal) oldest.release();
            else oldest.stop();
            return oldest;
        }
        return null; // shouldn't happen
    }

    /**
     * Required by Minim.UGen, core method for audio synthesis called by Minim.
     *
     * <p>Scheduled point events are processed first so voices that start at the current
     * {@code sampleCursor} contribute to this output sample. The method then mixes active
     * voices, applies constant-power panning, density normalization, and soft limiting, and
     * finally advances the sampler-local clock by one sample.</p>
     */
    @Override
    protected synchronized void uGenerate(float[] channels) {
        scheduler.processBlock(
                sampleCursor,
                1,
                (ScheduledPlay sp, int offsetInBlock) -> {
                    PASamplerVoice v = getAvailableVoice();
                    if (v != null) {
                        v.activate(sp.samplePos, sp.sampleLen, sp.amplitude,
                                sp.env, sp.pitch, sp.pan, sp.looping, sp.wrapAround);
                    }
                },
                null
        );

        Arrays.fill(channels, 0f);

        final MixProfile profile = this.mixProfile;
        float activeWeight = 0f;

        Iterator<PASamplerVoice> it = voices.iterator();
        while (it.hasNext()) {
            PASamplerVoice v = it.next();

            float sample;
            try {
                sample = v.nextSample();
            } catch (ArrayIndexOutOfBoundsException e) {
                v.stop();
                continue;
            }

            boolean isActive = v.isActive();
            boolean isReleasing = v.isReleasing();

            if (isActive || isReleasing) {
                if (isActive) {
                    activeWeight += 1.0f;
                } else if (isReleasing) {
                    activeWeight += profile.releaseWeight;
                }

                float pan = v.getPan(); // [-1, +1]
                float theta = (pan + 1f) * (float) (Math.PI * 0.25); // 0..pi/2
                float leftGain = (float) Math.cos(theta);
                float rightGain = (float) Math.sin(theta);

                channels[0] += sample * leftGain;
                if (channels.length > 1) channels[1] += sample * rightGain;
            }

            if (v.isFinished()) {
                v.resetPosition();
            }
        }

        // --- Density-based normalization --- //
        /* targetNorm = 1 / activeWeight^normExponent
         * normExponent controls how aggressively gain drops as voice count rises:
         * 0.5 (square root), common for uncorrelated signals
         * 1.0 full division by voice count, much more conservative
         * MixProfile uses: 
         * TRANSPARENT: 0.5, BALANCED: 0.6, PROTECTIVE: 0.7.
         * So for 4 active voices: 
         * normExponent             targetNorm
         * 0.5    ->    1/sqrt(4) = 0.50
         * 0.6    ->    1/(4^0.6) = 0.435
         * 0.7    ->    1/(4^0.7) = 0.379
         * 1.0    ->    1/(4)     = 0.25
         * alpha smooths how fast mixNorm changes. 
         * mixNorm scales masterGain.
         */
        float targetNorm = (activeWeight > 1f)
                ? 1f / (float) Math.pow(activeWeight, profile.normExponent)
                : 1f;

        mixNorm += profile.alpha * (targetNorm - mixNorm);

        float g = mixNorm * masterGain;
        channels[0] *= g;
        if (channels.length > 1) channels[1] *= g;

        // Soft limiter
        channels[0] = softClipSoftsign(channels[0], profile.drive);
        if (channels.length > 1) {
            channels[1] = softClipSoftsign(channels[1], profile.drive);
        }

        sampleCursor++;
    }
    
    
    /**
     * Applies a softsign limiter to reduce overload without hard clipping.
     *
     * <p>The input is first scaled by {@code drive}, then mapped through
     * {@code y / (1 + abs(y))}. Small values stay close to linear, while large
     * positive or negative values asymptotically approach -1 or 1.</p>
     * <p>A drive value of 1.0 is basically neutral. Typical active values 
     * are in the range 1.2 to 1.6. 2.0 to 3.3 provides obvious compress, 
     * above 4.0 probably distorts. MixProfile provides :TRANSPARENT: 1.35, 
     * BALANCED: 1.60, PROTECTIVE: 1.25.</p>
     *
     * @param x       input sample
     * @param drive   pre-limiter gain controlling how quickly the curve saturates
     * @return softly limited sample
     */
    static float softClipSoftsign(float x, float drive) {
        float y = drive * x;
        return y / (1f + Math.abs(y));
    }
    
    // PASampler methods

    @Override
    public void stopAll() {
        synchronized (this) {
            for (PASamplerVoice v : voices) v.stop();
        }
    }
    
    @Override
    public void releaseAll() {
        synchronized (this) {
            for (PASamplerVoice v : voices) {
                if (v.isActive() || v.isReleasing()) v.release();
            }
        }
    }

    @Override
    public boolean isLooping() {
        for (PASamplerVoice v : voices) {
            if (v.isActive() && v.isLooping()) return true;
        }
        return false;
    }

    // Controls / inspection

    /**
     * Sets default looping for newly triggered voices.
     *
     * @param looping true to loop newly triggered voices
     */
    public void setGlobalLooping(boolean looping) {
        this.globalLooping = looping;
    }

    /** @return true when newly triggered voices loop by default */
    public boolean isGlobalLooping() { return globalLooping; }

    /**
     * Sets whether newly triggered voices wrap source-buffer reads.
     *
     * @param wrapAround true to wrap reads that pass the end of the source buffer
     */
    @Override
    public void setWrapAround(boolean wrapAround) {
        this.wrapAround = wrapAround;
    }

    /** @return true when newly triggered voices wrap source-buffer reads */
    @Override
    public boolean isWrapAround() {
        return wrapAround;
    }

    /**
     * Enable/disable smooth stealing (release envelope) on voice recycle.
     *
     * @param smoothSteal true to release stolen voices smoothly
     */
    public void setSmoothSteal(boolean smoothSteal) {
        this.smoothSteal = smoothSteal;
    }

    /** @return true when stolen voices release smoothly */
    public boolean isSmoothSteal() { return smoothSteal; }

    /**
     * Change maximum polyphony at runtime.
     *
     * @param maxVoices maximum simultaneous voices
     */
    public synchronized void setMaxVoices(int maxVoices) {
        this.maxVoices = Math.max(1, maxVoices);
    }

    /** @return maximum simultaneous voices */
    public int getMaxVoices() { return maxVoices; }

    /**
     * Read-only list of voices for GUI or debugging.
     *
     * @return unmodifiable voice list
     */
    public List<PASamplerVoice> getVoices() {
        return Collections.unmodifiableList(voices);
    }
    
    // ----- Accessors ----- //
    
    /**
     * Sets master output gain.
     *
     * @param linear linear gain value
     */
    public void setMasterGain(float linear) {
        if (Float.isNaN(linear) || Float.isInfinite(linear)) return;
        masterGain = Math.max(0f, linear);
    }

    /** @return master output gain as a linear value */
    public float getMasterGain() {
        return masterGain;
    }

    /**
     * Sets master output gain in decibels.
     *
     * @param db gain in decibels
     */
    public void setMasterGainDb(float db) {
        setMasterGain(AudioUtility.dbToLinear(db));
    }

    /** @return master output gain in decibels */
    public float getMasterGainDb() {
        return AudioUtility.linearToDb(masterGain);
    }
    
    /**
     * Replaces the sampler source buffer.
     *
     * @param buffer mono source sample buffer
     */
    public synchronized void setBuffer(float[] buffer) {
    	this.buffer = buffer;
    	this.bufferLen = (buffer != null) ? buffer.length : 0;
    	for (PASamplerVoice v : this.voices) {
    		v.stop();
    		v.setBuffer(buffer);
    		v.resetPosition();
    	}
    }
 
    /**
     * Replaces the sampler source buffer and playback sample rate.
     *
     * @param buffer mono source sample buffer
     * @param playbackSampleRate sample rate of the source buffer in Hz
     */
    public synchronized void setBuffer(float[] buffer, float playbackSampleRate) {
    	this.buffer = buffer;
    	this.bufferLen = (buffer != null) ? buffer.length : 0;
    	this.playbackSampleRate = playbackSampleRate;
    	for (PASamplerVoice v : this.voices) {
    		v.stop();
    		v.setBuffer(buffer);
    		v.resetPosition();
    	}
    }
        
    /**
     * Counts voices that are neither active nor releasing.
     *
     * @return available voice count
     */
    public int countAvailableVoices() {
        int n = 0;
        for (PASamplerVoice v : voices) if (!v.isActive() && !v.isReleasing()) n++;
        return n;
    }

    /**
     * Sets the mix normalization profile.
     *
     * @param profile mix profile to apply
     */
    public void setMixProfile(MixProfile profile) {
        if (profile != null) this.mixProfile = profile;
    }

    /** @return active mix normalization profile */
    public MixProfile getMixProfile() {
        return mixProfile;
    }
    
    /** @return active mix profile name */
    public String getMixProfileName() {
        return mixProfile.name();
    }
    
    /** Advances to the next mix normalization profile. */
    public void cycleMixProfile() {
        MixProfile[] vals = MixProfile.values();
        int i = (mixProfile.ordinal() + 1) % vals.length;
        mixProfile = vals[i];
    }

    // ------------------------------------------------------------------------
    // Sample rate management
    // ------------------------------------------------------------------------

    /**
     * Returns the current sample rate of this sampler.
     *
     * @return playback sample rate in Hz
     */
    public synchronized float getPlaybackSampleRate() {
    	return playbackSampleRate;
    }

    /**
     * Updates the playback sample rate used for reading from the buffer.
     * Does not affect Minim's UGen sample rate.
     *
     * @param newRate playback sample rate in Hz
     */
    public synchronized void setPlaybackSampleRate(float newRate) {
        if (newRate > 0f && newRate != playbackSampleRate) {
            this.playbackSampleRate = newRate;
            for (PASamplerVoice v : voices) {
                v.setPlaybackSampleRate(newRate);
            }
        }
    }

    /**
     * Convenience: synchronize playback rate with AudioOutput's sample rate.
     * Useful if you want playback speed tied to system rate.
     */
    public synchronized void updatePlaybackRateFromOutput() {
        if (out != null) {
            setPlaybackSampleRate(out.sampleRate());
        }
    }
    
    /** @return source buffer length in samples */
    public int getBufferLength() { return bufferLen; }

    /** @return target audio output */
    public AudioOutput getAudioOutput() { return out; }
    
}
