/*
 *  Copyright (c) 2024 - 2025 by Paul Hertz <ignotus@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.paulhertz.pixelaudio.sampler;

/**
 * Generic interface for anything that can be "played."
 * Provides a minimal playback API plus backward-compatible overloads
 * for sampler instruments and other PAPlayable classes.
 * Used for Sampler audio synthesis engine. 
 * TODO use in Granular engine too? 
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
