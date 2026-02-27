package net.paulhertz.pixelaudio.sampler;

/**
 * Specialized playable interface for sample-based instruments.
 * Extends PAPlayable with buffer position, length, and envelope parameters.
 * Includes multiple overloads of playSample(...) for backward compatibility.
 */
public interface PASamplerPlayable extends PAPlayable {

    /**
     * Core playback method: start playback from a given buffer range
     * with start index, duration, amplitude, envelope, pitch, and pan control,
     * arguments in standard order for PixalAudio library.
     */
    int play(int samplePos, int sampleLen, float amplitude,
             ADSRParams env, float pitch, float pan);

    // --------------------------------------------------------------------
    // Backward-compatible playSample(...) overloads
    // TODO decide behavior of methods with -1 sampleLen argument, at the 
    // moment the expectation is that they return 0, no sound played. 
    // Mostly, they will be ignored when not present in the implementing 
    // class in a more complete version. 
    // --------------------------------------------------------------------

    /** Simplest: play whole buffer at default amplitude and pitch. */
    default int playSample() {
        return play(0, -1, 1.0f, null, 1.0f, 0.0f);
    }

    /** Play with amplitude only. */
    default int playSample(float amplitude) {
        return play(0, -1, amplitude, null, 1.0f, 0.0f);
    }

    /** Play with amplitude and envelope. */
    default int playSample(float amplitude, ADSRParams env) {
        return play(0, -1, amplitude, env, 1.0f, 0.0f);
    }

    /** Play with amplitude, envelope, and pitch. */
    default int playSample(float amplitude, ADSRParams env, float pitch) {
        return play(0, -1, amplitude, env, pitch, 0.0f);
    }

    /** Play with amplitude, envelope, pitch, and pan. */
    default int playSample(float amplitude, ADSRParams env, float pitch, float pan) {
        return play(0, -1, amplitude, env, pitch, pan);
    }

    /** Play a subrange of the buffer with full parameters. */
    default int playSample(int start, int length, float amplitude,
                           ADSRParams env, float pitch, float pan) {
        return play(start, length, amplitude, env, pitch, pan);
    }

    /** Play subrange with amplitude and pitch (no envelope). */
    default int playSample(int start, int length, float amplitude, float pitch) {
        return play(start, length, amplitude, null, pitch, 0.0f);
    }

    /** Play subrange with amplitude only. */
    default int playSample(int start, int length, float amplitude) {
        return play(start, length, amplitude, null, 1.0f, 0.0f);
    }
}
