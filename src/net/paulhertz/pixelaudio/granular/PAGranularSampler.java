package net.paulhertz.pixelaudio.granular;

import ddf.minim.UGen;
import ddf.minim.AudioOutput;
import ddf.minim.analysis.WindowFunction;

import net.paulhertz.pixelaudio.voices.ADSRParams;
import net.paulhertz.pixelaudio.schedule.AudioScheduler;


import java.util.*;

/**
 * PAGranularSampler
 *
 * UGen-based multi-voice granular sampler.
 *
 * Features:
 *  - Voice pooling (PAGranularVoice instances)
 *  - Per-voice ADSR, gain, and pan
 *  - Optional looping of the grain path
 *  - Thread-safe play() method
 *  - Per-sample mixing (like PASharedBufferSampler)
 *  
 * 
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

    // Convenience
    public PAGranularSampler(AudioOutput out) {
        this(out, 32);
    }

    // ------------------------------------------------------------------------
    // Voice allocation
    // ------------------------------------------------------------------------
 
    /**
     * Allocate a PAGranularVoice instance. Called from play() and uGenerate() methods.
     *  
     * @param src                A PASource
     * @param env                ADSRParams envelope, could be null
     * @param gain               gain as a decimal value scaling amplitude
     * @param pan                pan in stereo space, but grains can set individually
     * @param looping            looping flag, best be false
     * @param grainWindow        a Minim WindowFunction
     * @param grainLenSamples    number of samples in one grain
     * @return
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
     * Play a granular source as a voice.
     *
     * @param src    PASource (PathGranularSource or BasicIndexGranularSource)
     * @param env    ADSR for the macro envelope
     * @param gain   amplitude
     * @param pan    -1..+1
     * @param looping loop granular path
     * @return voiceId or -1
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

    // Overload (no looping)
    public synchronized long play(PASource src,
    		ADSRParams env,
    		float gain,
    		float pan) {
    	return play(src, env, gain, pan, false);
    }

    // Convenience: default envelope supplied by instrument
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
     * Schedule a new voice to start at an absolute sample time.
     *
     * @param src         PASource
     * @param env         ADSR (already resolved: either custom or default)
     * @param gain        final gain
     * @param pan         final pan
     * @param looping     loop flag
     * @param startSample absolute sample index at which to start the voice
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
     * @param src                PASource
     * @param env                ADSR (already resolved: either custom or default)
     * @param gain               final gain
     * @param pan                final pan
     * @param looping            loop flag
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
     * Schedule a new voice to start after a given delay in samples.
     *
     * @param src          PASource
     * @param env          ADSR
     * @param gain         final gain
     * @param pan          final pan
     * @param looping      loop flag
     * @param delaySamples how many samples from "now" to start
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
     * Expose the current absolute sample cursor (for higher-level scheduling).
     */
    public synchronized long getCurrentSampleTime() {
    	return sampleCursor;
    }
    

    // ------------------------------------------------------------------------
    // uGenerate — per-sample frame processing with AudioScheduler
    // Called through Minim 
    // ------------------------------------------------------------------------
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
    public void stopAll() {
        synchronized (this) {
            for (PAGranularVoice v : voices) v.stop();
        }
    }

    public void setMaxVoices(int maxVoices) {
        this.maxVoices = Math.max(1, maxVoices);
    }

    public int getMaxVoices() {
        return maxVoices;
    }

    public void setSmoothSteal(boolean smoothSteal) {
        this.smoothSteal = smoothSteal;
    }

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
