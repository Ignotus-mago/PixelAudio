package net.paulhertz.pixelaudio.voices;

import ddf.minim.MultiChannelBuffer;

/**
 * PASource
 *
 * A PAFloatSource with an additional pitch policy hint for PASamplerInstrument
 * and related voices.
 *
 * Lives in the voices package because it's meant to plug directly into
 * PASamplerInstrument / PASamplerVoice. Minim dependency is allowed here.
 */
public interface PASource extends PAFloatSource {

    /**
     * Pitch policy hint for the instrument:
     * should the instrument apply its pitch (playback rate) on top of this source?
     */
    default PitchPolicy pitchPolicy() {
        return PitchPolicy.INSTRUMENT_RATE;
    }

    /**
     * Optional seek/rewind hook, mainly for transport or "note-on" start time.
     * Default implementation does nothing.
     *
     * @param absoluteSample absolute sample index to seek to.
     */
    default void seekTo(long absoluteSample) {
        // no-op by default
    }

    /**
     * Optional access to an underlying MultiChannelBuffer, if this source is
     * fundamentally buffer-backed (e.g., plain sample playback or a rendered
     * granular "tape").
     *
     * Implementations that are not backed by a fixed buffer (true streams,
     * procedural sources, etc.) should simply return null.
     */
    default MultiChannelBuffer getMultiChannelBuffer() {
        return null;
    }
}
