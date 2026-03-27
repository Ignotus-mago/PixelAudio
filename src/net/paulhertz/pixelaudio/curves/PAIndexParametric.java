package net.paulhertz.pixelaudio.curves;

import java.util.Arrays;

/**
 * PAIndexParametric
 *
 * Parametric view of an indexed offset list, typically into a float[] buffer
 * (for example, an audio signal or lookup table).
 *
 * u ∈ [0,1] -> f(u)
 *
 * For now, u is interpreted over the index domain of the offset list:
 *   - u = 0   -> first offset
 *   - u = 1   -> last offset
 *   - intermediate u -> linear interpolation between neighboring offsets
 *
 * This class now implements PAControlCurve so it can be used anywhere a
 * normalized scalar control curve is needed.
 */
public final class PAIndexParametric implements PAControlCurve {

    private final int[] offsets;
    private final int count;

    /**
     * @param offsets ordered offsets in a buffer;
     *                must contain at least 2 offsets
     */
    public PAIndexParametric(int[] offsets) {
        if (offsets == null || offsets.length < 2) {
            throw new IllegalArgumentException(
                "PAIndexParametric requires at least 2 offsets."
            );
        }
        this.offsets = Arrays.copyOf(offsets, offsets.length);
        this.count = offsets.length;
    }

    /**
     * @return number of offsets in this parametric series
     */
    public int getPointCount() {
        return count;
    }

    /**
     * @return defensive copy of the underlying offsets
     */
    public int[] getOffsets() {
        return Arrays.copyOf(offsets, offsets.length);
    }

    /**
     * Sample the curve at parameter u ∈ [0,1], returning the interpolated
     * scalar value as a float.
     *
     * u is clamped to [0,1].
     */
    @Override
    public float sample(float u) {
        u = clamp01(u);

        if (count == 2) {
            return lerp(offsets[0], offsets[1], u);
        }

        float pos = u * (count - 1);     // 0 .. count-1
        int i = (int) Math.floor(pos);   // base index
        float frac = pos - i;

        if (i >= count - 1) {
            return offsets[count - 1];
        }

        int p0 = offsets[i];
        int p1 = offsets[i + 1];
        return lerp(p0, p1, frac);
    }

    /**
     * Sample the curve at parameter u ∈ [0,1], returning the interpolated
     * scalar value rounded to the nearest int.
     *
     * Handy when offsets are ultimately used as buffer indices.
     */
    public int sampleInt(float u) {
        return Math.round(sample(u));
    }

    /**
     * Alias for sampleInt(u), preserving the old public API shape if desired.
     */
    public int sampleIndex(float u) {
        return sampleInt(u);
    }

    private static float clamp01(float u) {
        if (u < 0f) return 0f;
        if (u > 1f) return 1f;
        return u;
    }

    private static float lerp(int a, int b, float u) {
        return a + (b - a) * u;
    }
}