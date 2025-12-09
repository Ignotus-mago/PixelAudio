package net.paulhertz.pixelaudio.curves;


/**
 * PAIndexParametric
 *
 * Parametric view of a indexed offsets, typically into a float[] buffer (audio signal).
 *
 * u ∈ [0,1] → f(u)
 *
 * For now, u is interpreted over the *index* of the offset list:
 *   - u = 0   → first index
 *   - u = 1   → last index
 *   - intermediate u → linear interpolation between neighboring offsets
 *
 */
public final class PAIndexParametric {
    private final int[] offsets;
    private final int count;

    /**
     * @param offsets ordered offsets in a buffer
     *                must contain at least 2 offsets.
     */
    public PAIndexParametric(int[] offsets) {
        if (offsets == null || offsets.length < 2) {
            throw new IllegalArgumentException("PAIndexParametric requires at least 2 offsets.");
        }
        this.offsets = offsets;
        this.count = offsets.length;
    }

    public int getPointCount() {
        return count;
    }

    /**
     * Sample the path at parameter u ∈ [0,1], returning an interpolated point.
     * u is clamped to [0,1].
     */
    public int sample(float u) {
        if (u < 0f) u = 0f;
        if (u > 1f) u = 1f;

        if (count == 2) {
            // Simple case: just interpolate between endpoints
            int p0 = offsets[0];
            int p1 = offsets[1];
            return Math.round(lerp(p0, p1, u));
        }

        float pos = u * (count - 1);     // 0 .. count-1
        int i = (int) Math.floor(pos);   // base index
        float frac = pos - i;

        if (i >= count - 1) {
            // At or beyond last index; clamp to last point
            return offsets[count - 1];
        }

        int p0 = offsets[i];
        int p1 = offsets[i + 1];
        return Math.round(lerp(p0, p1, frac));
    }

    private static float lerp(int a, int b, float u) {
        float x = a + (b - a) * u;
        return x;
    }
}

