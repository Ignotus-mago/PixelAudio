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
 * Utility methods for converting absolute sample spans to block-local overlap ranges.
 *
 * <p>{@code OverlapUtil} works with half-open sample intervals. A span is represented as
 * {@code [spanStart, spanEnd)}, and the current audio block is represented as
 * {@code [blockStart, blockStart + blockSize)}. The returned {@link Slice} gives the portion
 * of the block that overlaps the span, expressed as indices into the current block.</p>
 *
 * <p>The main use is granular rendering, where each grain occupies an absolute sample span
 * but each audio callback renders only one block at a time. {@link PABurstGranularSource}
 * uses this class to find the loop bounds for samples that belong to the active grain.</p>
 */
public final class OverlapUtil {

    private OverlapUtil() {
        // utility class
    }

    /**
     * Block-local overlap range returned by {@link #computeBlockSlice(long, int, long, long)}.
     *
     * <p>When {@link #hasOverlap} is true, {@code startIndex} is inclusive and
     * {@code endIndex} is exclusive, so the overlapping samples can be processed with
     * {@code for (int i = startIndex; i < endIndex; i++)}. When {@code hasOverlap} is false,
     * both indices are 0.</p>
     */
    public static final class Slice {
        /** First block-local index where the span overlaps, inclusive. */
        public final int startIndex;

        /** One past the last block-local index where the span overlaps. */
        public final int endIndex;

        /** True if there is any overlap at all. */
        public final boolean hasOverlap;

        /**
         * Creates a block-local overlap range.
         *
         * @param startIndex first overlapping index in the block, inclusive
         * @param endIndex one past the last overlapping index in the block
         * @param hasOverlap true if the range contains at least one sample
         */
        public Slice(int startIndex, int endIndex, boolean hasOverlap) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.hasOverlap = hasOverlap;
        }
    }

    /**
     * Computes the portion of the current block overlapped by an absolute sample span.
     *
     * <p>The span and block are treated as half-open intervals. Touching endpoints do not
     * count as overlap: for example, a span ending exactly at {@code blockStart}, or starting
     * exactly at {@code blockStart + blockSize}, produces {@code hasOverlap == false}.</p>
     *
     * <p>For overlapping intervals, the returned indices are clamped to the range
     * {@code [0, blockSize]} and are suitable as loop bounds into block-sized audio buffers.</p>
     *
     * @param blockStart    absolute sample index of the first sample in the block
     * @param blockSize     number of samples in the block
     * @param spanStart     absolute start of the span, inclusive
     * @param spanEnd       absolute end of the span, exclusive
     * @return a {@code Slice} indicating {@code [startIndex, endIndex)} within the block,
     *         or {@code hasOverlap == false} if there is no overlap
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
