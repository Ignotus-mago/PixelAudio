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
    /** True to release stolen voices smoothly instead of stopping them immediately. */
    private boolean smoothSteal = true;
    
    /** Smoothed mix-normalization gain. */
    private float mixNorm = 1f; 
    
    /** Master linear gain applied to mixed output. */
    private volatile float masterGain = 1f;
    
    /** Enables diagnostic voice-trigger logging when true. */
    protected boolean DEBUG = false;
 
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
     * Play command with all the useful arguments in standard order, overrides PASampler.play().
     * TODO We plan to make this the only play() method PASharedBufferSampler. 
     */
     @Override
    public synchronized int play(int samplePos, int sampleLen, float amplitude,
                                 ADSRParams env, float pitch, float pan) {
        if (sampleLen <= 0 || samplePos >= bufferLen) return 0;
        if (samplePos < 0) samplePos = 0;
        if (samplePos + sampleLen > bufferLen) sampleLen = bufferLen - samplePos;
        PASamplerVoice v = getAvailableVoice();
        if (v == null) return 0;
        v.activate(samplePos, sampleLen, amplitude, env, pitch, pan, globalLooping);
        int eventSamples = PlaybackInfo.computeVoiceDuration(
                samplePos,
                sampleLen,
                bufferLen,
                pitch,
                env,
                globalLooping,
                playbackSampleRate
            );
        float bufferReadSamples = sampleLen * Math.abs(pitch);
        float durationMS = eventSamples / playbackSampleRate * 1000f;
        long startSample = 0; // later replace with an actual sample clock
        // information to be shared later
        PlaybackInfo info = new PlaybackInfo(
            v.getVoiceId(),
            eventSamples,
            bufferReadSamples,
            durationMS,
            globalLooping,
            startSample,
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
     * Constant-power panning and soft limiter added to support polyphonic voices. 
     */
    @Override
    protected synchronized void uGenerate(float[] channels) {
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
