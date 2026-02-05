package net.paulhertz.pixelaudio.granular;

import ddf.minim.MultiChannelBuffer;
import net.paulhertz.pixelaudio.voices.PitchPolicy;

/**
 * PASource
 *
 * A PAFloatSource with an additional pitch policy hint.
 *
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
