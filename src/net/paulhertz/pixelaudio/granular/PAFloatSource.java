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
 * PAFloatSource
 *
 * Minimal, library-agnostic "audio source" abstraction for PixelAudio voices.
 * It deals only with float[] buffers and sample indices, no Minim types.
 *
 * Implementations are expected to:
 *  - be called on the audio thread,
 *  - be allocation-free inside renderBlock().
 *  
 *  @See PASource.
 */
public interface PAFloatSource {

    /**
     * Render audio into the given block buffers.
     *
     * Implementations should:
     *  - Assume outL/outR length >= blockSize.
     *  - Mix into outL/outR (add), not clear them.
     *  - Avoid allocation on the audio thread.
     *
     * @param blockStart the absolute sample index in the source’s own sample domain (e.g., buffer index space).
     * @param blockSize  number of samples in this block.
     * @param outL       left channel buffer to mix into.
     * @param outR       right channel buffer to mix into (may be same as outL for mono).
     */
    void renderBlock(long blockStart,
                     int blockSize,
                     float[] outL,
                     float[] outR);

    /**
     * Duration in samples, or Long.MAX_VALUE if effectively infinite/streaming.
     */
    long lengthSamples();
}
