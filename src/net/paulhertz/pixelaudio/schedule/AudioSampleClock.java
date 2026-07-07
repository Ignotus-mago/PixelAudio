/*
 *  Copyright (c) 2024 - 2026 by Paul Hertz <ignotus@gmail.com>
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

package net.paulhertz.pixelaudio.schedule;

/**
 * Minimal contract for an audio-rate sample clock.
 *
 * <p>{@code AudioSampleClock} lets application code schedule multiple synth engines against
 * one shared notion of audio time without depending on the implementation that owns the clock.
 * A sampler, granular instrument, dedicated transport UGen, or future external-sync adapter
 * can implement this interface as long as it can report an absolute sample cursor and the
 * sample rate used to interpret that cursor.</p>
 */
public interface AudioSampleClock {

    /**
     * Returns the current absolute sample time for this clock.
     *
     * @return current sample cursor
     */
    long getCurrentSampleTime();

    /**
     * Returns the sample rate used by this clock.
     *
     * @return sample rate in Hz
     */
    float getSampleRate();
}
