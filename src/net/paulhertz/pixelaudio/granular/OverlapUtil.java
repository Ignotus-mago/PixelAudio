package net.paulhertz.pixelaudio.granular;

/**
 * OverlapUtil
 *
 * Small helper for computing overlap between a span [spanStart, spanEnd)
 * and a block [blockStart, blockStart+blockSize).
 *
 * Useful for granular streaming and any span-based scheduling at audio rate.
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
