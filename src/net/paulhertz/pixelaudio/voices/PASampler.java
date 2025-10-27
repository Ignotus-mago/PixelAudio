package net.paulhertz.pixelaudio.voices;

/**
 * Backend engine contract for sampler implementations.
 * Allows swapping implementations without changing higher layers.
 */
public interface PASampler {
    int play(int samplePos, int sampleLen, float amplitude,
             ADSRParams env, float pitch, float pan);

    /** Returns true if any currently active voice is looping. */
    boolean isLooping();

    /** Stops all voices immediately. */
    void stopAll();

	void setSampleRate(float newRate);
	
}
