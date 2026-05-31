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
 * OverlapUtil
 *
 * Small helper for computing overlap between a span [spanStart, spanEnd)
 * and a block [blockStart, blockStart+blockSize).
 *
 * Useful for granular streaming and any span-based scheduling at audio rate.
 * Critical use in {@link PABurstGranularSource}, as part of PixelAudio's 
 * granular synthesis processing chain. 
 */
public final class OverlapUtil {

    private OverlapUtil() {
        // utility class
    }

    public static final class Slice {
        /** First index in the block (0..blockSize) where the span overlaps. */
        public final int startIndex;

        /** One past the last index in the block where the span overlaps. */
        public final int endIndex;

        /** True if there is any overlap at all. */
        public final boolean hasOverlap;

        public Slice(int startIndex, int endIndex, boolean hasOverlap) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.hasOverlap = hasOverlap;
        }
    }

    /**
     * Compute overlap slice into the current block for a span [spanStart, spanEnd).
     *
     * @param blockStart absolute sample index of the first sample in the block.
     * @param blockSize  number of samples in this block.
     * @param spanStart  absolute start of the span (inclusive).
     * @param spanEnd    absolute end of the span (exclusive).
     * @return a Slice indicating [startIndex, endIndex) within the block, or hasOverlap=false if none.
     */
    public static Slice computeBlockSlice(long blockStart,
                                          int blockSize,
                                          long spanStart,
                                          long spanEnd) {
        long blockEnd = blockStart + blockSize;

        // No overlap cases
        if (spanEnd <= blockStart || spanStart >= blockEnd) {
            return new Slice(0, 0, false);
        }

        long oStart = Math.max(spanStart, blockStart);
        long oEnd = Math.min(spanEnd, blockEnd);

        int startIdx = (int) (oStart - blockStart);
        int endIdx = (int) (oEnd - blockStart);

        if (startIdx < 0) startIdx = 0;
        if (endIdx > blockSize) endIdx = blockSize;

        if (startIdx >= endIdx) {
            return new Slice(0, 0, false);
        }

        return new Slice(startIdx, endIdx, true);
    }
}
