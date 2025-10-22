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

    private final Sampler sharedSampler;
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
        this.sharedSampler = sharedSampler;
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

    	if (samplePos < 0) samplePos = 0;
    	if (sampleLen < 0) sampleLen = 0;

    	// When looping, allow playback to wrap without truncation
    	if (!isLooping) {
    		if (samplePos + sampleLen > bufferSize) {
    			sampleLen = Math.max(0, bufferSize - samplePos);
    		}
    	}

    	// Compute effective end index including release
    	int releaseSamples = Math.round(env.getRelease() * sampleRate);
    	int end = samplePos + sampleLen + releaseSamples;
    	if (end >= bufferSize) end = bufferSize - 1;

    	// Apply per-voice amplitude and pan
    	gain.setValue(amplitude);
    	panner.setPan(pan);

    	// Configure the shared Sampler’s snapshot parameters
    	sharedSampler.begin.setLastValue(samplePos);
    	sharedSampler.end.setLastValue(end);
    	sharedSampler.rate.setLastValue(pitch);
    	sharedSampler.looping = isLooping;

    	// Create per-voice envelope and patch chain:
    	// Sampler → gain (per-voice amplitude) → ADSR → Pan → Out
    	currentEnvelope = env.toADSR();
    	sharedSampler.patch(gain);
    	gain.patch(currentEnvelope);
    	currentEnvelope.patch(panner);
    	panner.patch(out);

    	// Trigger playback (snapshots begin/end/rate)
    	sharedSampler.trigger();
    	currentEnvelope.noteOn();

    	// Schedule noteOff and cleanup only if not looping
    	if (!isLooping && sampleLen > 0) {
    		long durationMillis = Math.round((sampleLen / (double) sampleRate) * 1000.0);
    		long releaseMillis = Math.round(env.getRelease() * 1000.0);

    		// Schedule ADSR noteOff
    		scheduler.schedule(() -> {
    			try {
    				currentEnvelope.noteOff();
    				currentEnvelope.unpatchAfterRelease(panner);
    			} 
    			catch (Exception ignored) {}
    		}, durationMillis, TimeUnit.MILLISECONDS);

    		// Unpatch after envelope finishes
    		scheduler.schedule(() -> {
    			try {
    				if (currentEnvelope != null) {
    					gain.unpatch(currentEnvelope);
    					currentEnvelope.unpatch(panner);
    					panner.unpatch(out);
    				}
    			} 
    			catch (Exception ignored) {}
    			isBusy = false;
    		}, durationMillis + releaseMillis + 50, TimeUnit.MILLISECONDS);
    	} 
    	else {
    		// If looping, let caller handle stopLoop()
    		isBusy = false;
    	}
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
            sharedSampler.looping = false;
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
        try { sharedSampler.stop(); } catch (Exception ignored) {}
    }

    public void close() {
        if (isClosed) return;
        scheduler.shutdownNow();
        isClosed = true;
    }
}
