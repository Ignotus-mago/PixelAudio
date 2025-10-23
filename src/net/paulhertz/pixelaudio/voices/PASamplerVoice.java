package net.paulhertz.pixelaudio.voices;

import ddf.minim.*;
import ddf.minim.ugens.*;
import java.util.concurrent.*;

/**
 * Voice that shares a Sampler but has independent amplitude, envelope, and panning.
 * 
 * Leverages Minim's snapshot behavior: each trigger() call snapshots begin, end, and rate.
 * Only amplitude is shared globally across voices, so this class inserts a per-voice
 * Multiplier for individual amplitude scaling.
 */
public class PASamplerVoice {
    private final AudioOutput out;
    private final float sampleRate;
    private final ScheduledExecutorService scheduler;
    private final ADSRParams defaultEnv;

    private final Sampler sampler;
    private final int bufferSize; 
    private final Multiplier gain;
    private final Pan panner;

    private ADSR currentEnvelope = null;
    private boolean isBusy = false;
    private boolean isClosed = false;
    private boolean isLooping = false;

    /**
     * Construct a voice that shares a Sampler with others but has independent control
     * of amplitude, envelope, and panning.
     */
    public PASamplerVoice(Sampler sharedSampler,
    		              int bufferSize,
                          float sampleRate,
                          AudioOutput out,
                          ADSRParams env) {
        this.sampler = sharedSampler;
        this.bufferSize = bufferSize;
        this.sampleRate = sampleRate;
        this.out = out;
        this.defaultEnv = env;

        this.gain = new Multiplier(1.0f);
        this.panner = new Pan(0.0f);

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PASamplerVoice-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    // -------------------------------------------------------------------------
    // Core playback
    // -------------------------------------------------------------------------

    /**
     * Trigger playback using independent per-voice parameters.
     *
     * @param samplePos  start position (samples)
     * @param sampleLen  playback length (samples)
     * @param amplitude  per-voice amplitude
     * @param env        ADSR parameters
     * @param pitch      playback rate (1.0 = normal)
     * @param pan        stereo position (-1.0 left, +1.0 right)
     */
    public synchronized int play(int samplePos,
    		int sampleLen,
    		float amplitude,
    		ADSRParams env,
    		float pitch,
    		float pan) {
    	if (isClosed || isBusy) return 0;
    	isBusy = true;
    	// clamp samplePos and sampleLen to positive numbers or 0
    	samplePos = Math.max(0, samplePos);
    	if (sampleLen < 0) sampleLen = 0;
    	// Clamp to buffer
    	if (!isLooping && samplePos + sampleLen > bufferSize) {
    		sampleLen = Math.max(0, bufferSize - samplePos);
    	}
    	// Compute sample end (no release extension)
    	int end = Math.min(samplePos + sampleLen - 1, bufferSize - 1);
    	// Configure sampler snapshot
    	sampler.begin.setLastValue(samplePos);
    	sampler.end.setLastValue(end);
    	sampler.rate.setLastValue(pitch);
    	sampler.looping = isLooping;
    	// Set amplitude and pan
    	gain.setValue(amplitude);
    	panner.setPan(pan);
    	// Build envelope chain
    	currentEnvelope = env.toADSR();
    	sampler.patch(gain);
    	gain.patch(currentEnvelope);
    	currentEnvelope.patch(panner);
    	panner.patch(out);
    	// and play the samples
    	try {
    		sampler.trigger();
    		currentEnvelope.noteOn();
    	} 
    	catch (Exception e) {
    		isBusy = false;
    		e.printStackTrace();
    		return 0;
    	}
    	// if we're not looping, handle note off and unpatch the processing chain
    	if (!isLooping && sampleLen > 0) {
    		// if you want to adjust duration to frequency, the next line does that:
    		// long durationMillis = Math.round((sampleLen / (sampleRate * Math.abs(pitch))) * 1000.0);
    		// We generally want note duration to be independent of pitch: pitch affects frequency only,
    		// not playback length or envelope timing.
    		long durationMillis = Math.round((sampleLen / sampleRate) * 1000.0);
    		long releaseMillis = Math.round(env.getRelease() * 1000.0);
    		final ADSR envUGen = currentEnvelope;
    		// schedule note off and cleanup 
    		scheduler.schedule(() -> {
    			try {
    				envUGen.noteOff();
    				envUGen.unpatchAfterRelease(panner);
    			} catch (Exception ignored) {}
    		}, durationMillis, TimeUnit.MILLISECONDS);
    		// unpatch in reverse order
    		scheduler.schedule(() -> {
    			try {
    				sampler.unpatch(gain);
    				gain.unpatch(envUGen);
    				envUGen.unpatch(panner);
    				panner.unpatch(out);
    			} 
    			catch (Exception ignored) {}
    			isBusy = false;
    		}, durationMillis + releaseMillis + 50, TimeUnit.MILLISECONDS);
    	} 
    	else {
    		// Looping playback managed externally
    		isBusy = false;
    	}
    	// return the actual duration of the event
    	return sampleLen;
    }
    
    public int play(int samplePos, int sampleLen, float amplitude, float pitch) {
        return play(samplePos, sampleLen, amplitude, defaultEnv, pitch, 0.0f);
    }
    

    // -------------------------------------------------------------------------
    // Loop control
    // -------------------------------------------------------------------------

    /**
     * Gracefully stop a looping sample and release its envelope.
     */
    public synchronized void stopLoop() {
        if (isClosed || !isLooping) return;

        try {
            sampler.looping = false;
            isLooping = false;

            if (currentEnvelope != null) {
                currentEnvelope.noteOff();
                currentEnvelope.unpatchAfterRelease(panner);
            }

            scheduler.schedule(() -> {
                try {
                    gain.unpatch(currentEnvelope);
                    currentEnvelope.unpatch(panner);
                    panner.unpatch(out);
                } 
                catch (Exception ignored) {}
                isBusy = false;
            }, Math.round(defaultEnv.getRelease() * 1000.0) + 50, TimeUnit.MILLISECONDS);
        } 
        catch (Exception e) {
            e.printStackTrace();
            isBusy = false;
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public void setPan(float pan) { panner.setPan(pan); }
    public float getPan() { return panner.pan.getLastValue(); }

    public void setIsLooping(boolean looping) { this.isLooping = looping; }
    public boolean isLooping() { return isLooping; }

    public boolean isBusy() { return isBusy; }
    public boolean isClosed() { return isClosed; }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void stop() {
        try { sampler.stop(); } catch (Exception ignored) {}
    }

    public void close() {
        if (isClosed) return;
        scheduler.shutdownNow();
        isClosed = true;
    }
}
