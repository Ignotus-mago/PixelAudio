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

package net.paulhertz.pixelaudio.granular;

/**
 * Minimal, library-agnostic audio source abstraction for PixelAudio voices.
 *
 * <p>{@code PAFloatSource} deals only with floating-point audio buffers and sample indices.
 * It deliberately avoids Minim and Processing types so sources can be rendered by PixelAudio's
 * voice classes without depending on a particular audio backend.</p>
 *
 * <p>Implementations are expected to be safe for audio-thread use:</p>
 * <ul>
 *   <li>mix into output buffers rather than clearing them;</li>
 *   <li>avoid allocation and blocking work in {@link #renderBlock(long, int, float[], float[])};</li>
 *   <li>treat {@code blockStart} as an absolute sample position in the source's playback domain.</li>
 * </ul>
 *
 * @see PASource
 */
public interface PAFloatSource {

    /**
     * Renders audio into the supplied block buffers.
     *
     * <p>Implementations should assume that {@code outL} and {@code outR} have at least
     * {@code blockSize} elements. Samples should be added to the buffers, not assigned over
     * existing contents, so multiple voices can accumulate into the same output block.</p>
     *
     * @param blockStart absolute sample index in the source's playback domain
     * @param blockSize number of samples to render
     * @param outL left channel buffer to mix into
     * @param outR right channel buffer to mix into; may be the same array as {@code outL} for mono
     */
    void renderBlock(long blockStart,
                     int blockSize,
                     float[] outL,
                     float[] outR);

    /**
     * Returns the source duration in samples.
     *
     * @return duration in samples, or {@link Long#MAX_VALUE} for an effectively infinite or streaming source
     */
    long lengthSamples();
}
