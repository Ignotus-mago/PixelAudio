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
	// TODO include "final long srcSampleIndex;  // <-- the mapped read position" in ScheduledPlay
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

    
	// private long blockStartSample = 0;    // NEW, for revised AudioScheduler

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
    // Scheduled play interface (NEW)
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
    
//    public void startAtSampleTime(PASource src,
//            ADSRParams env,
//            float gain,
//            float pan,
//            boolean looping,
//            long startSample,
//            WindowFunction grainWindow,
//            int grainLenSamples) {
//
//        // Store into your ScheduledPlay / VoiceStart / whatever you already enqueue.
//        // For example:
//        // queue.add(new ScheduledPlay(src, env, gain, pan, looping, startSample, grainWindow, grainLenSamples));
//
//        startAtSampleTime(src, env, gain, pan, looping, startSample); // fallback for now if needed
//    }
    
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
    // ------------------------------------------------------------------------
    @Override
    protected synchronized void uGenerate(float[] channels) {
        // 1) Fire point events at this exact sample (blockSize = 1 frame)
        scheduler.processBlock(
                sampleCursor,
                1,
                (ScheduledPlay sp, int offsetInBlock) -> {
                    // offsetInBlock will always be 0 here
                    getAvailableVoice(
                            sp.src, sp.env, sp.gain, sp.pan, sp.looping,
                            sp.grainWindow, sp.grainLenSamples
                    );
                },
                null
        );

        // 2) Clear this sample frame
        Arrays.fill(channels, 0f);

        // 3) Mix one sample frame from all voices
        float leftMix = 0f;
        float rightMix = 0f;

        for (PAGranularVoice v : voices) {
            v.nextSampleStereo(tmpStereo);
            float left  = tmpStereo[0];
            float right = tmpStereo[1];
            if (v.isActive() || v.isReleasing()) {
                leftMix  += left;
                rightMix += right;
            }
        }

        // 4) Write output for this frame
        channels[0] = (channels.length > 0) ? leftMix : 0f;
        if (channels.length > 1) {
            channels[1] = rightMix;
        }

        // 5) Advance global sample cursor by one sample frame
        sampleCursor++;
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
