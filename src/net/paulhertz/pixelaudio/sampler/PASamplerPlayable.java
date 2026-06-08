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
 * Specialized playable interface for sample-based instruments.
 * Extends PAPlayable with buffer position, length, and envelope parameters.
 * Includes multiple overloads of playSample(...) for backward compatibility.
 * TODO we may want to weed out legacy methods or label them as @Deprecated
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
