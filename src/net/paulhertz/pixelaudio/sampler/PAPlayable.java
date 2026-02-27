package net.paulhertz.pixelaudio.sampler;

/**
 * Generic interface for anything that can be "played."
 * Provides a minimal playback API plus backward-compatible overloads
 * for sampler instruments and other PAPlayable classes.
 */
public interface PAPlayable {

    /**
     * Minimal play signature for generic playables.
     * Implementations may ignore parameters they don't need.
     *
     * @param amplitude gain multiplier (0..1+)
     * @param pitch     playback rate or pitch factor (implementation-defined)
     * @param pan       stereo pan (-1 left .. +1 right)
     * @return non-zero if a voice/event was triggered
     */
    int play(float amplitude, float pitch, float pan);

    /** Stop playback immediately (implementation-defined). */
    void stop();

    // --------------------------------------------------------------------
    // Backward-compatible overloads
    // --------------------------------------------------------------------

    /** Simple play call using default amplitude, pitch, and pan. */
    default int play() {
        return play(1.0f, 1.0f, 0.0f);
    }

    /** Play with amplitude only. */
    default int play(float amplitude) {
        return play(amplitude, 1.0f, 0.0f);
    }

    /** Play with amplitude and pitch. */
    default int play(float amplitude, float pitch) {
        return play(amplitude, pitch, 0.0f);
    }

    /** Play with amplitude, pitch, and pan. */
    default int play(float amplitude, float pitch, float pan, boolean unused) {
        // 'unused' parameter is included for legacy signatures
        return play(amplitude, pitch, pan);
    }

    /**
     * Optional interface for playables that support envelopes.
     * Implementations that don't use ADSR can ignore this overload.
     */
    default int play(float amplitude, ADSRParams env, float pitch, float pan) {
        return play(amplitude, pitch, pan);
    }
}
