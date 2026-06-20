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

package net.paulhertz.pixelaudio.granular;

import ddf.minim.UGen;
import ddf.minim.AudioOutput;
import ddf.minim.analysis.WindowFunction;
import net.paulhertz.pixelaudio.sampler.ADSRParams;
import net.paulhertz.pixelaudio.schedule.AudioScheduler;


import java.util.*;

/**
 * UGen-based multi-voice sampler for granular {@link PASource} playback.
 *
 * <p>{@code PAGranularSampler} is the voice-pool and sample-accurate scheduling layer in
 * PixelAudio's granular synthesis chain. {@link PAGranularInstrument} passes immediate and
 * scheduled playback requests to this class; the sampler allocates or reuses
 * {@link PAGranularVoice} instances, starts scheduled voices at the requested sample time,
 * and mixes active voices into Minim's audio callback.</p>
 *
 * <p>Core responsibilities:</p>
 * <ul>
 *   <li>manage a bounded pool of {@link PAGranularVoice} instances;</li>
 *   <li>start sources immediately or through {@link AudioScheduler};</li>
 *   <li>carry per-voice envelope, gain, pan, looping, and grain-window settings;</li>
 *   <li>mix active voices sample by sample in {@link #uGenerate(float[])};</li>
 *   <li>apply light mix normalization and soft clipping to reduce overload.</li>
 * </ul>
 *
 * <p>When the voice pool is full, the sampler may steal the oldest active voice. With
 * smooth stealing enabled, the old voice is released before reuse; otherwise it is stopped
 * immediately.</p>
 *
 * @see PAGranularInstrumentDirector
 * @see PAGranularInstrument
 * @see PAGranularVoice
 */
public class PAGranularSampler extends UGen {
    // ------------------------------------------------------------------------
    // Internal scheduled-play payload
    // ------------------------------------------------------------------------
	// TODO include "final long srcSampleIndex;  // <-- the mapped read position in ScheduledPlay
    private static final class ScheduledPlay {
        final PASource src;
        final ADSRParams env;
        final float gain;
        final float pan;
        final boolean looping;
        final WindowFunction grainWindow; // may be null -> voice should default
        final int grainLenSamples;        // >= 1

        ScheduledPlay(PASource src,
        		ADSRParams env,
        		float gain,
        		float pan,
        		boolean looping,
        		WindowFunction grainWindow,
        		int grainLenSamples) {
        	this.src = src;
        	this.env = env;
        	this.gain = gain;
        	this.pan = pan;
        	this.looping = looping;
        	this.grainWindow = grainWindow;
        	this.grainLenSamples = Math.max(1, grainLenSamples);
        }
        
        // Backward-friendly convenience if you still enqueue without window data
        ScheduledPlay(PASource src,
        		ADSRParams env,
        		float gain,
        		float pan,
        		boolean looping) {
        	this(src, env, gain, pan, looping, null, 1);
        }

    }	

    // ------------------------------------------------------------------------
    // Core fields
    // ------------------------------------------------------------------------
    private final AudioOutput out;

    private final List<PAGranularVoice> voices = new ArrayList<>();
    private int maxVoices = 32;
    private int blockSize;

    private boolean smoothSteal = true;
    
    // Sample-accurate scheduler for launching new voices
    private final AudioScheduler<ScheduledPlay> scheduler = new AudioScheduler<>();

    // Absolute sample counter (across the life of this UGen)
    private long sampleCursor = 0L;
    
    private final float[] tmpStereo = new float[2];

    private float mixNorm = 1f;    // NEW, mixing and normalization
    private float globalMakeUpGain = 2.5f;

	// private long blockStartSample = 0;    // NEW, for revised AudioScheduler

    /**
     * Initializes this PAGranularSampler, which extends UGen and gets patched to an AudioOutput, 
     * with the result that its uGenerate method is called on each audio block.
     * 
     * @param out          a Minim AudioOutput that this PAGranularSampler will patch to
     * @param maxVoices    maximum number of voices to allocate
     */
    public PAGranularSampler(AudioOutput out, int maxVoices) {
        this.out = out;
        this.maxVoices = Math.max(1, maxVoices);
        this.blockSize = out.bufferSize();
        this.patch(out);             // UGen connected to AudioOutput
    }

    /**
     * Creates a granular sampler with a default maximum of 32 voices.
     *
     * @param out Minim audio output this sampler patches to
     */
    public PAGranularSampler(AudioOutput out) {
        this(out, 32);
    }

    // ------------------------------------------------------------------------
    // Voice allocation
    // ------------------------------------------------------------------------
 
    /**
     * Allocates or reuses a voice for a source.
     *
     * <p>If no inactive voice is available and the pool is below {@link #maxVoices}, a new
     * voice is created. If the pool is full, the oldest active voice is stolen. Smooth stealing
     * releases the old voice first; hard stealing stops it immediately.</p>
     *  
     * @param src                source to render
     * @param env                ADSRParams envelope, or null for the voice default
     * @param gain               linear voice gain
     * @param pan                stereo pan in the range [-1, 1]
     * @param looping            true to loop the source path where supported
     * @param grainWindow        window function for shaping grain amplitude, or null for source default
     * @param grainLenSamples    number of samples in one grain
     * @return allocated voice, or null if no voice could be allocated
     */
    private PAGranularVoice getAvailableVoice(PASource src, ADSRParams env,
    		float gain, float pan, boolean looping,
    		WindowFunction grainWindow, int grainLenSamples) {
    	// 1. find free voice
    	for (PAGranularVoice v : voices) {
    		if (!v.isActive() && !v.isReleasing()) {
    			v.activate(src, env, gain, pan, looping, grainWindow, Math.max(1, grainLenSamples));
    			return v;
    		}
    	}

    	// 2. expand pool if allowed
    	if (voices.size() < maxVoices) {
    		PAGranularVoice v = new PAGranularVoice(src, blockSize, out.sampleRate());
    		voices.add(v);
    		v.activate(src, env, gain, pan, looping, grainWindow, Math.max(1, grainLenSamples));
    		return v;
    	}

    	// 3. recycle oldest active voice
    	PAGranularVoice oldest = null;
    	for (PAGranularVoice v : voices) {
    		if (v.isActive() && (oldest == null || v.getVoiceId() < oldest.getVoiceId())) {
    			oldest = v;
    		}
    	}
    	if (oldest != null) {
    		if (smoothSteal) oldest.release();
    		else oldest.stop();
    		oldest.activate(src, env, gain, pan, looping, grainWindow, Math.max(1, grainLenSamples));
    		return oldest;
    	}

    	return null; // should not occur
    }

    // Preserve old internal helper signature (calls new one with null window)
    private PAGranularVoice getAvailableVoice(PASource src, ADSRParams env,
    		float gain, float pan, boolean looping) {
    	return getAvailableVoice(src, env, gain, pan, looping, null, 1);
    }

    // ------------------------------------------------------------------------
    // Play interface
    // ------------------------------------------------------------------------
    
    /**
     * Plays a granular source immediately as a voice.
     *
     * @param src       source to render
     * @param env       ADSR for the macro envelope
     * @param gain      linear voice gain
     * @param pan       stereo pan in the range [-1, 1]
     * @param looping   true to loop the source path where supported
     * @return voice id, or -1 if playback could not start
     */
    public synchronized long play(PASource src,
    		ADSRParams env,
    		float gain,
    		float pan,
    		boolean looping) {
    	if (src == null) return -1;

    	PAGranularVoice v = getAvailableVoice(src, env, gain, pan, looping);
    	if (v == null) return -1;

    	return v.getVoiceId();
    }

    /** Convenience overload that disables looping. */
    public synchronized long play(PASource src,
    		ADSRParams env,
    		float gain,
    		float pan) {
    	return play(src, env, gain, pan, false);
    }

    /** Convenience overload for callers that supply an already-resolved default envelope. */
    public synchronized long play(PASource src,
    		float gain,
    		float pan,
    		ADSRParams defaultEnv,
    		boolean looping) {
    	return play(src, defaultEnv, gain, pan, looping);
    }

    // ------------------------------------------------------------------------
    // Scheduled play interface, the preferred method of triggering audio
    // ------------------------------------------------------------------------

    /**
     * Schedules a new voice to start at an absolute sample time.
     *
     * @param src           source to render
     * @param env           ADSR (already resolved: either custom or default)
     * @param gain          final linear voice gain
     * @param pan           final stereo pan
     * @param looping       true to loop the source path where supported
     * @param startSample   absolute sample index at which to start the voice
     */
    public synchronized void startAtSampleTime(PASource src,
            ADSRParams env,
            float gain,
            float pan,
            boolean looping,
            long startSample) {
        if (src == null) return;
        ScheduledPlay happening = new ScheduledPlay(src, env, gain, pan, looping);
        scheduler.schedulePoint(startSample, happening);
    }
        
    /**
     * Schedules a new voice to start at an absolute sample time with grain-window settings.
     *
     * <p>Called by {@link PAGranularInstrument#startAtSampleTime(PASource, float, float, ADSRParams, boolean, long, WindowFunction, int)}.
     * This method packages the source and rendering settings in a scheduled payload that is
     * launched from Minim's {@link #uGenerate(float[])} callback.</p>
     * 
     * @param src                source to render
     * @param env                ADSR (already resolved: either custom or default)
     * @param gain               final linear voice gain
     * @param pan                final stereo pan
     * @param looping            true to loop the source path where supported
     * @param startSample        absolute sample index at which to start the voice
     * @param grainWindow        a window function for shaping grain amplitude
     * @param grainLenSamples    number of samples in one grain
     */
    public synchronized void startAtSampleTime(PASource src,
    		ADSRParams env,
    		float gain,
    		float pan,
    		boolean looping,
    		long startSample,
    		WindowFunction grainWindow,
    		int grainLenSamples) {
    	if (src == null) return;
    	ScheduledPlay happening = new ScheduledPlay(src, env, gain, pan, looping, grainWindow, grainLenSamples);
    	scheduler.schedulePoint(startSample, happening);
    }
    
    /**
     * Schedules a new voice to start after a delay in samples.
     *
     * @param src            source to render
     * @param env            ADSR
     * @param gain           final linear voice gain
     * @param pan            final stereo pan
     * @param looping        true to loop the source path where supported
     * @param delaySamples   how many samples from "now" to start
     */
    public synchronized void startAfterDelaySamples(PASource src,
    		ADSRParams env,
    		float gain,
    		float pan,
    		boolean looping,
    		long delaySamples) {
    	long startSample = this.sampleCursor + Math.max(0, delaySamples);
    	startAtSampleTime(src, env, gain, pan, looping, startSample);
    }

    /**
     * Returns the current absolute sample cursor for higher-level scheduling.
     *
     * @return current sample time maintained by this sampler
     */
    public synchronized long getCurrentSampleTime() {
    	return sampleCursor;
    }
    

    /**
     * Generates one audio sample frame for Minim.
     *
     * <p>This method advances the scheduler, launches voices whose start time has arrived,
     * renders active voices, applies mix normalization and soft clipping, and increments the
     * sample cursor.</p>
     */
    @Override
    protected synchronized void uGenerate(float[] channels) {
        scheduler.processBlock(
            sampleCursor,
            1,
            (ScheduledPlay sp, int offsetInBlock) -> {
                getAvailableVoice(
                    sp.src, sp.env, sp.gain, sp.pan, sp.looping,
                    sp.grainWindow, sp.grainLenSamples
                );
            },
            null
        );

        Arrays.fill(channels, 0f);

        float leftMix = 0f;
        float rightMix = 0f;
        int activeCount = 0;

        for (PAGranularVoice v : voices) {
            v.nextSampleStereo(tmpStereo);
            if (v.isActive() || v.isReleasing()) {
                activeCount++;
                leftMix  += tmpStereo[0];
                rightMix += tmpStereo[1];
            }
        }

        // power normalization, smoothed
        float targetNorm = (activeCount > 1)
                ? (float)Math.pow(activeCount, -0.25f)  // gentler than 1/sqrt
                : 1f;

        float alpha = 0.12f;
        mixNorm += alpha * (targetNorm - mixNorm);

        // makeup is MULTIPLICATIVE
        float postGain = mixNorm * globalMakeUpGain; // e.g., 1.5f..3.0f
        leftMix  *= postGain;
        rightMix *= postGain;

        // limiter
        float drive = 2.0f;
        leftMix  = softClipSoftsign(leftMix, drive);
        rightMix = softClipSoftsign(rightMix, drive);
        channels[0] = leftMix;
        if (channels.length > 1) channels[1] = rightMix;

        sampleCursor++;
    }
    
    private static float softClipSoftsign(float x, float drive) {
        float y = drive * x;
        return y / (1f + Math.abs(y));
    }
   
    
    // ------------------------------------------------------------------------
    // Controls
    // ------------------------------------------------------------------------
    /**
     * Immediately stops all voices without clearing pending scheduled starts.
     *
     * <p>A stop halts voice processing directly and is more abrupt than a release.</p>
     */
    public void stopAll() {
        synchronized (this) {
            for (PAGranularVoice v : voices) v.stop();
        }
    }
    
    /**
     * Remove all pending scheduled starts that have not yet been launched.
     * Does not affect currently active voices.
     */
    public synchronized void clearScheduled() {
        scheduler.clear();
    }

    /**
     * Release all currently active or releasing voices.
     *
     * <p>A release is generally less abrupt than a stop, because each active voice is allowed
     * to pass through its envelope release stage. This is useful for a musical stop after
     * clearing pending scheduled starts.</p>
     */
    public synchronized void releaseAll() {
        for (PAGranularVoice v : voices) {
            if (v.isActive() || v.isReleasing()) {
                v.release();
            }
        }
    }

    /**
     * Clear pending scheduled starts and immediately stop all active voices.
     *
     * <p>A stop halts voice processing directly and is therefore more abrupt than a release.</p>
     */
    public synchronized void cancelAndStopAll() {
        scheduler.clear();
        for (PAGranularVoice v : voices) {
            v.stop();
        }
    }

    /**
     * Clear pending scheduled starts and release currently sounding voices.
     *
     * <p>This is the less abrupt counterpart to {@link #cancelAndStopAll()}.</p>
     */
    public synchronized void cancelAndReleaseAll() {
        scheduler.clear();
        for (PAGranularVoice v : voices) {
            if (v.isActive() || v.isReleasing()) {
                v.release();
            }
        }
    }

    /**
     * Sets the maximum number of voices in the pool.
     *
     * @param maxVoices maximum voices; values below 1 are clamped to 1
     */
    public void setMaxVoices(int maxVoices) {
        this.maxVoices = Math.max(1, maxVoices);
    }

    public int getMaxVoices() {
        return maxVoices;
    }

    /**
     * Enables or disables smooth voice stealing.
     *
     * <p>Voice stealing occurs when all voices are busy and a new source must start. With
     * smooth stealing enabled, the oldest active voice is released before reuse; with it
     * disabled, the oldest active voice is stopped immediately.</p>
     *
     * @param smoothSteal true to release stolen voices, false to stop them immediately
     */
    public void setSmoothSteal(boolean smoothSteal) {
        this.smoothSteal = smoothSteal;
    }

    /**
     * Reports whether smooth voice stealing is enabled.
     *
     * @return true when stolen voices are released instead of stopped immediately
     */
    public boolean isSmoothSteal() {
        return smoothSteal;
    }

    public List<PAGranularVoice> getVoices() {
        return Collections.unmodifiableList(voices);
    }

    public AudioOutput getAudioOutput() {
        return out;
    }
}
