package net.paulhertz.pixelaudio.voices;

import ddf.minim.MultiChannelBuffer;

/**
 * Interface for any object capable of playing audio samples from a buffer,
 * with optional support for ADSR envelopes and pitch scaling.
 */
public interface PAPlayable {

    /** Plays a sample using the default buffer (or the buffer this object was initialized with). */
    int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env);

    /** Plays a sample from the specified buffer, if supported. */
    default int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
                           float amplitude, ADSRParams env) {
        throw new UnsupportedOperationException("Multiple buffers not supported by this class.");
    }

    /**
     * Plays a sample with pitch scaling (multiplicative playback rate).
     * pitchScale = 1.0 → normal speed, 2.0 → one octave up, 0.5 → one octave down.
     */
    default int playSample(int samplePos, int sampleLen, float amplitude,
                           float pitchScale, ADSRParams env) {
        throw new UnsupportedOperationException("Pitch scaling not supported by this class.");
    }

    /** Same as above, but tied to a particular buffer (for multi-buffer implementations). */
    default int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
                           float amplitude, float pitchScale, ADSRParams env) {
        throw new UnsupportedOperationException("Pitch scaling with multiple buffers not supported by this class.");
    }

    /** Shut down and release resources. */
    void close();
}
