package net.paulhertz.pixelaudio.granular;

import ddf.minim.UGen;
import ddf.minim.AudioOutput;

import net.paulhertz.pixelaudio.voices.ADSRParams;
import net.paulhertz.pixelaudio.voices.PASource;
import net.paulhertz.pixelaudio.schedule.SampleAccurateScheduler;


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
 * Now also supports sample-accurate scheduling of new voices
 * via SampleAccurateScheduler.
 * 
 */
public class PAGranularSampler extends UGen {
    // ------------------------------------------------------------------------
    // Internal scheduled-play payload
    // ------------------------------------------------------------------------
    private static final class ScheduledPlay {
        final PASource src;
        final ADSRParams env;
        final float gain;
        final float pan;
        final boolean looping;

        ScheduledPlay(PASource src,
                      ADSRParams env,
                      float gain,
                      float pan,
                      boolean looping) {
            this.src = src;
            this.env = env;
            this.gain = gain;
            this.pan = pan;
            this.looping = looping;
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
    private final SampleAccurateScheduler<ScheduledPlay> scheduler =
            new SampleAccurateScheduler<>();

    // Absolute sample counter (across the life of this UGen)
    private long sampleCursor = 0L;

    public PAGranularSampler(AudioOutput out, int maxVoices) {
        this.out = out;
        this.maxVoices = Math.max(1, maxVoices);
        this.blockSize = out.bufferSize();
        this.patch(out);
    }

    // Convenience
    public PAGranularSampler(AudioOutput out) {
        this(out, 16);
    }

    // ------------------------------------------------------------------------
    // Voice allocation
    // ------------------------------------------------------------------------
    private PAGranularVoice getAvailableVoice(PASource src, ADSRParams env,
                                              float gain, float pan, boolean looping) {
        // 1. find free voice
        for (PAGranularVoice v : voices) {
            if (!v.isActive() && !v.isReleasing()) {
                v.activate(src, env, gain, pan, looping);
                return v;
            }
        }

        // 2. expand pool if allowed
        if (voices.size() < maxVoices) {
            PAGranularVoice v = new PAGranularVoice(src, blockSize, out.sampleRate());
            voices.add(v);
            v.activate(src, env, gain, pan, looping);
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
            oldest.activate(src, env, gain, pan, looping);
            return oldest;
        }

        return null; // should not occur
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
    public synchronized void schedulePlayAtSample(PASource src,
    		ADSRParams env,
    		float gain,
    		float pan,
    		boolean looping,
    		long startSample) {
    	if (src == null) return;
    	ScheduledPlay timeEvent = new ScheduledPlay(src, env, gain, pan, looping);
    	scheduler.schedule(startSample, timeEvent);
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
    public synchronized void schedulePlayInSamples(PASource src,
    		ADSRParams env,
    		float gain,
    		float pan,
    		boolean looping,
    		long delaySamples) {
    	long startSample = this.sampleCursor + Math.max(0, delaySamples);
    	schedulePlayAtSample(src, env, gain, pan, looping, startSample);
    }

    /**
     * Expose the current absolute sample cursor (for higher-level scheduling).
     */
    public synchronized long getSampleCursor() {
    	return sampleCursor;
    }

    // ------------------------------------------------------------------------
    // uGenerate — per-sample mixing + scheduler tick
    // ------------------------------------------------------------------------
    @Override
    protected synchronized void uGenerate(float[] channels) {
        // sampleCursor is absolute sample across the life of this UGen
        scheduler.tick(sampleCursor, scheduled -> {
            ScheduledPlay sp = scheduled.timeEvent;
            // Launch a new voice exactly at this sample
            getAvailableVoice(sp.src, sp.env, sp.gain, sp.pan, sp.looping);
        });

        Arrays.fill(channels, 0f);

        float[] tmp = new float[2]; // left, right

        for (PAGranularVoice v : voices) {

            v.nextSampleStereo(tmp);
            float left  = tmp[0];
            float right = tmp[1];

            if (v.isActive() || v.isReleasing()) {
                // For now, *ignore voice-level pan* and trust the per-grain pan
                channels[0] += left;
                if (channels.length > 1) {
                    channels[1] += right;
                }
            }

            if (v.isFinished()) {
                // recyclable; nothing else needed
            }
        }

        // Advance global sample cursor by one sample frame
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
