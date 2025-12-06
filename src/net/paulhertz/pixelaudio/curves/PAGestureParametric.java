package net.paulhertz.pixelaudio.curves;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

import processing.core.PVector;

/**
 * PAGestureParametric
 *
 * Treats a gesture defined by (time, x, y) samples as a parametric curve:
 *    u ∈ [0,1] → (t(u), x(u), y(u))
 *
 * Under the hood:
 *  - timesMs: int[] of time offsets (ms), ascending, timesMs[0] == 0
 *  - points:  PVector list (x,y) of same length
 *
 * You can:
 *  - sample with a linear parameter: f(u) = u
 *  - pass a warp function f(u) to speed up/slow down traversal
 *  
 *  Exponential warp (speeding up over time)
 *  <code>
 *    DoubleUnaryOperator expWarp = u -> (float_ Math.pow(u, 2.0);   // u^2
 *    GestureParametric.Sample s = gp.sample(0.5f, expWarp);
 *  </code>
 *  Log-like warp (fast at start, slow at end)
 *  <code>
 *    DoubleUnaryOperator sqrtWarp = u -> (float_ Math.sqrt(u);   // 
 *    GestureParametric.Sample s = gp.sample(0.5f, sqrtWarp);
 *  </code>
 *  Something like f(u) = (e^{k *  u} - 1) / (e^{k} - 1) for more dramatic warps.
 *  
 */
public final class PAGestureParametric {

    public static final class Sample {
        public final float tMs; // interpolated time in ms
        public final float x;
        public final float y;

        public Sample(float tMs, float x, float y) {
            this.tMs = tMs;
            this.x = x;
            this.y = y;
        }
    }

    private final List<PVector> points;
    private final int[] timesMs;
    private final int count;
    private final float totalDurationMs;

    /**
     * @param points  gesture points (x,y)
     * @param timesMs time offsets (ms), same length as points,
     *                ascending, timesMs[0] == 0
     */
    public PAGestureParametric(List<PVector> points, int[] timesMs) {
        if (points == null || timesMs == null) {
            throw new IllegalArgumentException("points and timesMs must not be null");
        }
        if (points.size() != timesMs.length) {
            throw new IllegalArgumentException("points.size() must equal timesMs.length");
        }
        if (points.isEmpty()) {
            throw new IllegalArgumentException("points must not be empty");
        }

        this.points = points;
        this.timesMs = timesMs;
        this.count = points.size();
        this.totalDurationMs = timesMs[count - 1];
    }

    /** Total gesture duration in ms. */
    public float getTotalDurationMs() {
        return totalDurationMs;
    }

    /**
     * Sample with linear parameterization: f(u) = u.
     * u is clamped to [0,1].
     */
    public Sample sample(float u) {
        return sample(u, null);
    }

    /**
     * Sample with an optional warp function f(u) → [0,1].
     * If warp is null, identity is used.
     */
    public Sample sample(float u, DoubleUnaryOperator warp) {
        if (u < 0f) u = 0f;
        if (u > 1f) u = 1f;

        double s = (warp != null) ? warp.applyAsDouble(u) : u;
        if (s < 0.0) s = 0.0;
        if (s > 1.0) s = 1.0;

        // Target time in ms
        float tTarget = (float) (s * totalDurationMs);

        // Find segment i such that timesMs[i] <= tTarget <= timesMs[i+1]
        int i = findSegment(tTarget);
        if (i >= count - 1) {
            // At or beyond last sample; clamp to last point
            PVector plast = points.get(count - 1);
            return new Sample(totalDurationMs, plast.x, plast.y);
        }

        int t0 = timesMs[i];
        int t1 = timesMs[i + 1];
        PVector p0 = points.get(i);
        PVector p1 = points.get(i + 1);

        float v;
        if (t1 == t0) {
            v = 0f;
        } else {
            v = (tTarget - t0) / (float) (t1 - t0);
            if (v < 0f) v = 0f;
            if (v > 1f) v = 1f;
        }

        float x = lerp(p0.x, p1.x, v);
        float y = lerp(p0.y, p1.y, v);

        return new Sample(tTarget, x, y);
    }

    // ------------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------------

    /**
     * Find the largest i such that timesMs[i] <= tTarget.
     * Returns count-1 if tTarget is beyond the last sample.
     */
    private int findSegment(float tTarget) {
        // Simple linear scan is fine for modest sizes.
        // Replace with binary search if gestures get long.
        int i = 0;
        while (i < count - 1 && timesMs[i + 1] <= tTarget) {
            i++;
        }
        return i;
    }

    private static float lerp(float a, float b, float u) {
        return a + (b - a) * u;
    }
}
