package net.paulhertz.pixelaudio.curves;

import java.util.Arrays;

import net.paulhertz.pixelaudio.schedule.GestureSchedule;

/**
 * PAKeyframeControlCurve
 *
 * Piecewise-linear scalar control curve defined by keyframes:
 *
 *    u ∈ [0,1] -> value
 *
 * where:
 *   - times01 is a strictly ascending list of control times in [0,1]
 *   - values  is a list of corresponding scalar values
 *
 * Typical uses:
 *   - gesture dynamics / gain shaping
 *   - pan curves
 *   - pitch modulation
 *   - other normalized control data
 *
 * This class makes no strong assumptions about the range of values.
 * For dynamics, values will usually be in [0,1], but other ranges are allowed.
 */
public final class PAKeyframeControlCurve implements PAControlCurve {

    private final float[] times01;
    private final float[] values;
    private final int count;

    /**
     * @param times01 strictly ascending control times in [0,1]
     * @param values  scalar values corresponding to each entry in times01
     */
    public PAKeyframeControlCurve(float[] times01, float[] values) {
        validate(times01, values);
        this.times01 = Arrays.copyOf(times01, times01.length);
        this.values  = Arrays.copyOf(values, values.length);
        this.count = times01.length;
    }

    /**
     * @return number of keyframes
     */
    public int getPointCount() {
        return count;
    }

    /**
     * @return defensive copy of normalized keyframe times
     */
    public float[] getTimes01() {
        return Arrays.copyOf(times01, times01.length);
    }

    /**
     * @return defensive copy of keyframe values
     */
    public float[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    /**
     * Sample the control curve at parameter u ∈ [0,1].
     * Values outside [0,1] are clamped before sampling.
     */
    @Override
    public float sample(float u) {
        u = clamp01(u);

        if (u <= times01[0]) {
            return values[0];
        }

        int last = count - 1;
        if (u >= times01[last]) {
            return values[last];
        }

        int i = findSegment(u);
        float t0 = times01[i];
        float t1 = times01[i + 1];
        float v0 = values[i];
        float v1 = values[i + 1];

        float a = (u - t0) / (t1 - t0);
        return lerp(v0, v1, a);
    }

    /**
     * Convenience for dynamics-style use where values are expected in [0,1].
     * The returned sample is clamped to [0,1].
     */
    public float sampleUnit(float u) {
        return clamp01(sample(u));
    }

    /**
     * Find the largest i such that times01[i] <= u,
     * with the guarantee that i < count - 1 for in-range queries.
     */
    private int findSegment(float u) {
        // Linear scan is fine for modest keyframe counts.
        // Can be replaced with binary search later if needed.
        int i = 0;
        while (i < count - 2 && times01[i + 1] < u) {
            i++;
        }
        return i;
    }

    private static void validate(float[] times01, float[] values) {
        if (times01 == null || values == null) {
            throw new IllegalArgumentException("times01 and values must not be null");
        }
        if (times01.length != values.length) {
            throw new IllegalArgumentException("times01 and values must have the same length");
        }
        if (times01.length < 2) {
            throw new IllegalArgumentException("PAKeyframeControlCurve requires at least 2 keyframes");
        }

        float prev = -1f;
        for (int i = 0; i < times01.length; i++) {
            float t = times01[i];
            if (Float.isNaN(t) || Float.isInfinite(t)) {
                throw new IllegalArgumentException("times01 must contain only finite values");
            }
            if (t < 0f || t > 1f) {
                throw new IllegalArgumentException("times01 values must lie in [0,1]");
            }
            if (i > 0 && t <= prev) {
                throw new IllegalArgumentException("times01 values must be strictly ascending");
            }
            prev = t;

            float v = values[i];
            if (Float.isNaN(v) || Float.isInfinite(v)) {
                throw new IllegalArgumentException("values must contain only finite values");
            }
        }
    }

    private static float clamp01(float u) {
        if (u < 0f) return 0f;
        if (u > 1f) return 1f;
        return u;
    }

    private static float lerp(float a, float b, float u) {
        return a + (b - a) * u;
    }
    
    // TODO move to a curve utility class? 
    public static float[] expandToSchedule(PAControlCurve curve, GestureSchedule sched) {
        int n = sched.size();
        float[] out = new float[n];
        if (curve == null || n == 0) return out;
        if (n == 1) {
            out[0] = curve.sample(0.0f);
            return out;
        }
        float t0 = sched.timesMs[0];
        float dur = sched.timesMs[n - 1] - t0;
        for (int i = 0; i < n; i++) {
            float u = (dur <= 0.0f)
                    ? i / (float)(n - 1)
                    : (sched.timesMs[i] - t0) / dur;
            out[i] = curve.sample(u);
        }
        return out;
    }

}