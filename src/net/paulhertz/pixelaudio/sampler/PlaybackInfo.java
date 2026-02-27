package net.paulhertz.pixelaudio.sampler;

/**
 * Holds information about a triggered playback event.
 * Returned by PASharedBufferSampler.play().
 */
public class PlaybackInfo {
    public final long voiceId;

    /** Duration in output samples (pitch-independent). */
    public final int eventSamples;

    /** Duration in milliseconds. */
    public final float durationMS;

    /** Actual number of buffer samples traversed (pitch-dependent). */
    public final float bufferReadSamples;

    /** Whether this event loops indefinitely. */
    public final boolean looping;

    /** Absolute start time of the event, in samples (optional). */
    public final long startSample;

    /** Absolute stop time of the event, in samples (optional). */
    public final long stopSample;

    /** Absolute start time in milliseconds (optional). */
    public final float startMS;

    /** Absolute stop time in milliseconds (optional). */
    public final float stopMS;

    public PlaybackInfo(
            long voiceId,
            int eventSamples,
            float bufferReadSamples,
            float durationMS,
            boolean looping,
            long startSample,
            float sampleRate)
    {
        this.voiceId = voiceId;
        this.eventSamples = eventSamples;
        this.durationMS = durationMS;
        this.bufferReadSamples = bufferReadSamples;
        this.looping = looping;
        this.startSample = startSample;
        this.stopSample = looping ? Long.MAX_VALUE : startSample + eventSamples;
        this.startMS = startSample / sampleRate * 1000f;
        this.stopMS = looping ? Float.POSITIVE_INFINITY : this.stopSample / sampleRate * 1000f;
    }
    
    /**
     * Computes total playback duration (in output samples, pitch-independent).
     * Envelope stage times are assumed to be in seconds.
     *
     * @return duration in output samples (int), or Integer.MAX_VALUE if looping.
     */
    public static int computeVoiceDuration(
    		int samplePos,
    		int sampleLen,
    		int bufferLen,
    		float pitch,
    		ADSRParams env,
    		boolean looping,
    		float sampleRate) {
    	if (looping) return Integer.MAX_VALUE;

    	// Clamp to buffer bounds
    	samplePos = Math.max(0, samplePos);
    	if (sampleLen < 0) sampleLen = 0;
    	if (samplePos + sampleLen > bufferLen) {
    		sampleLen = Math.max(0, bufferLen - samplePos);
    	}

    	// Base playback time in samples (pitch-independent)
    	float baseSamples = sampleLen;

    	// Envelope duration (attack + decay + release) in seconds → samples
    	float envSamples = 0f;
    	if (env != null) {
    		envSamples = (env.getAttack() + env.getDecay() + env.getRelease()) * sampleRate;
    	}

    	float totalSamples = baseSamples + envSamples;
    	return (int) totalSamples;
    }


    @Override
    public String toString() {
        return String.format(
            "[Voice %d] dur=%d samples (%.2f ms), bufferRead≈%.0f, start=%.2f ms stop=%.2f ms looping=%b",
            voiceId, eventSamples, durationMS, bufferReadSamples, startMS, stopMS, looping
        );
    }
}
