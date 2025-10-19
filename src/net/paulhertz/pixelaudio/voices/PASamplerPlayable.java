package net.paulhertz.pixelaudio.voices;

import ddf.minim.MultiChannelBuffer;

/**
 * Specialized extension of WFPlayable for sampler-based instruments.
 *
 * Supports:
 *  - Playback from a default or supplied MultiChannelBuffer
 *  - Per-note pitch control
 *  - Global pitch scaling for all playback
 */
public interface PASamplerPlayable extends PAPlayable {

    /**
     * Play a sample from the given buffer using ADSR envelope and amplitude.
     *
     * @param buffer     MultiChannelBuffer to play from
     * @param samplePos  start position (samples)
     * @param sampleLen  length to play (samples)
     * @param amplitude  amplitude multiplier
     * @param env        ADSR envelope parameters
     * @return number of samples actually played
     */
    int playSample(MultiChannelBuffer buffer,
                   int samplePos,
                   int sampleLen,
                   float amplitude,
                   ADSRParams env);

    /**
     * Play a sample from the default buffer using ADSR envelope and amplitude.
     *
     * @param samplePos  start position (samples)
     * @param sampleLen  length to play (samples)
     * @param amplitude  amplitude multiplier
     * @param env        ADSR envelope parameters
     * @return number of samples actually played
     */
    @Override
    int playSample(int samplePos,
                   int sampleLen,
                   float amplitude,
                   ADSRParams env);

    /**
     * Play a sample from the given buffer with a specified pitch scaling factor.
     *
     * @param buffer     MultiChannelBuffer to play from
     * @param samplePos  start position (samples)
     * @param sampleLen  length to play (samples)
     * @param amplitude  amplitude multiplier
     * @param pitch      pitch scale (1.0 = normal speed)
     * @param env        ADSR envelope parameters
     * @return number of samples actually played
     */
    int playSample(MultiChannelBuffer buffer,
                   int samplePos,
                   int sampleLen,
                   float amplitude,
                   float pitch,
                   ADSRParams env);

    /**
     * Play a sample from the default buffer with a specified pitch scaling factor.
     *
     * @param samplePos  start position (samples)
     * @param sampleLen  length to play (samples)
     * @param amplitude  amplitude multiplier
     * @param pitch      pitch scale (1.0 = normal speed)
     * @param env        ADSR envelope parameters
     * @return number of samples actually played
     */
    int playSample(int samplePos,
                   int sampleLen,
                   float amplitude,
                   float pitch,
                   ADSRParams env);

    /**
     * Set a global pitch scale for playback.
     * 1.0 = normal speed, 0.5 = half-speed (one octave down),
     * 2.0 = double-speed (one octave up), etc.
     *
     * @param scale pitch scaling factor (must be > 0)
     */
    void setPitchScale(float scale);

    /**
     * Get the current global pitch scale factor.
     *
     * @return pitch scaling factor
     */
    float getPitchScale();
}
