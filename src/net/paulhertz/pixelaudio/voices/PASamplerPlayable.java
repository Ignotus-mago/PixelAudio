package net.paulhertz.pixelaudio.voices;

import ddf.minim.MultiChannelBuffer;

/**
 * Interface for sampler-based instruments with optional multi-buffer playback
 * and per-note pitch control.
 *
 * Extends PAPlayable, which provides the base playSample() definitions.
 */
public interface PASamplerPlayable extends PAPlayable {

    /**
     * Play a sample from the default buffer using the given parameters.
     *
     * @param samplePos  start position in samples
     * @param sampleLen  number of samples to play
     * @param amplitude  amplitude multiplier (0.0–1.0 typical)
     * @param env        ADSR envelope parameters
     * @param pitch      pitch scaling factor (1.0 = normal, 2.0 = octave up)
     * @param pan        stereo position (-1.0 = left, +1.0 = right)
     * @return number of samples actually played
     */
    int playSample(int samplePos,
                   int sampleLen,
                   float amplitude,
                   ADSRParams env,
                   float pitch,
                   float pan);

    /**
     * Optional: play a sample from a supplied buffer (if supported).
     * Implementations may choose to ignore this and always use the default buffer.
     *
     * @param buffer     MultiChannelBuffer to play from
     * @param samplePos  start position in samples
     * @param sampleLen  number of samples to play
     * @param amplitude  amplitude multiplier
     * @param env        ADSR envelope parameters
     * @param pitch      pitch scaling factor
     * @param pan        stereo position
     * @return number of samples actually played
     */
    default int playSample(MultiChannelBuffer buffer,
                           int samplePos,
                           int sampleLen,
                           float amplitude,
                           ADSRParams env,
                           float pitch,
                           float pan) {
        // Default implementation: use main buffer if applicable
        return 0;
    }

    /**
     * Set the global pitch scale factor for all playback.
     * Example: 0.5 = half-speed (one octave down), 2.0 = double-speed (one octave up).
     *
     * @param scale pitch scaling factor (> 0)
     */
    void setPitchScale(float scale);

    /**
     * Get the current global pitch scale factor.
     *
     * @return pitch scaling factor
     */
    float getPitchScale();
}
