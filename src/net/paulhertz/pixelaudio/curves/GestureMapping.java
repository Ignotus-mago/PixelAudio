package net.paulhertz.pixelaudio.curves;

import processing.core.PVector;

/**
 * Maps gesture points to indices into a mappingSnapshot array.
 *
 * mappingSnapshot is assumed to be a dense array of sample indices
 * indexed by a raster address (x,y)->i, or any other scheme defined by the implementation.
 */
public interface GestureMapping {
    /**
     * @param p gesture point (typically in pixel coordinates)
     * @param mappingSnapshot array of mapped buffer indices (immutable snapshot)
     * @return an index into mappingSnapshot (NOT the sample value), or -1 if invalid
     */
    int pointToSnapshotIndex(PVector p, int[] mappingSnapshot);

    /**
     * Default: interpret p as pixel coordinates and mappingSnapshot as width*height raster.
     */
    final class DefaultMapping implements GestureMapping {
        private final int width;
        private final int height;

        public DefaultMapping(int width, int height) {
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
        }

        @Override
        public int pointToSnapshotIndex(PVector p, int[] mappingSnapshot) {
            if (p == null || mappingSnapshot == null) return -1;
            int x = clamp((int)Math.floor(p.x), 0, width - 1);
            int y = clamp((int)Math.floor(p.y), 0, height - 1);
            int idx = y * width + x;
            return (idx >= 0 && idx < mappingSnapshot.length) ? idx : -1;
        }

        private static int clamp(int v, int lo, int hi) {
            return (v < lo) ? lo : (v > hi ? hi : v);
        }
    }
}

