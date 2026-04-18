package net.paulhertz.pixelaudio.curves;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.paulhertz.pixelaudio.schedule.GestureSchedule;
import processing.core.PVector;

/**
 * Boundary policy for mapping gesture points into a rectangular domain.
 *
 * Intended use:
 * geometric data in PACurveMaker and GestureSchedule may legitimately extend
 * outside mapper/display bounds. This class applies an interpretation policy
 * before point->index conversion.
 *
 * Spatial remap modes:
 *   CLIP     clamp to rectangle
 *   WRAP     wrap modulo width/height
 *   REFLECT  reflect repeatedly across edges
 *
 * Omission/time modes:
 *   SKIP_TIME  drop out-of-bounds points and compress time across removed spans
 *   KEEP_TIME  drop out-of-bounds points and preserve surviving timestamps
 */
public final class PABoundsPolicy {

    public enum PABoundaryMode {
        /** Clamp coordinates to bounds. */
        CLIP,

        /** Wrap coordinates modulo width/height (toroidal space). */
        WRAP,

        /** Reflect coordinates across bounds (mirror behavior). */
        REFLECT,

        /** Drop out-of-bounds points and collapse time across removed spans. */
        SKIP_TIME,

        /** Drop out-of-bounds points but preserve original timing. */
        KEEP_TIME
    }

    private final float minX;
    private final float minY;
    private final float maxX;
    private final float maxY;
    private final float width;
    private final float height;
    private final PABoundaryMode mode;

    /**
     * Rectangle is inclusive on both ends: [minX..maxX], [minY..maxY].
     */
    public PABoundsPolicy(float minX, float minY, float maxX, float maxY, PABoundaryMode mode) {
        if (maxX < minX) throw new IllegalArgumentException("maxX < minX");
        if (maxY < minY) throw new IllegalArgumentException("maxY < minY");
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.width = maxX - minX + 1.0f;
        this.height = maxY - minY + 1.0f;
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    public static PABoundsPolicy fromWidthHeight(int width, int height, PABoundaryMode mode) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("width and height must be >= 1");
        }
        return new PABoundsPolicy(0, 0, width - 1, height - 1, mode);
    }

    public float minX() { return minX; }
    public float minY() { return minY; }
    public float maxX() { return maxX; }
    public float maxY() { return maxY; }
    public float width() { return width; }
    public float height() { return height; }
    public PABoundaryMode mode() { return mode; }

    public boolean contains(float x, float y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    public boolean contains(PVector p) {
        return p != null && contains(p.x, p.y);
    }

    /**
     * Applies the policy to a single point.
     *
     * For CLIP / WRAP / REFLECT:
     *   returns a mapped point.
     *
     * For SKIP_TIME / KEEP_TIME:
     *   returns the original point if in bounds, otherwise null.
     */
    public PVector apply(PVector p) {
        if (p == null) return null;

        switch (mode) {
            case CLIP:
                return new PVector(clamp(p.x, minX, maxX), clamp(p.y, minY, maxY));

            case WRAP:
                return new PVector(wrap(p.x, minX, width), wrap(p.y, minY, height));

            case REFLECT:
                return new PVector(reflect(p.x, minX, maxX), reflect(p.y, minY, maxY));

            case SKIP_TIME:
            case KEEP_TIME:
                return contains(p) ? p.copy() : null;

            default:
                throw new IllegalStateException("Unhandled boundary mode: " + mode);
        }
    }

    /**
     * Applies the policy pointwise to a list.
     *
     * CLIP / WRAP / REFLECT keep cardinality.
     * SKIP_TIME / KEEP_TIME omit out-of-bounds points.
     */
    public ArrayList<PVector> applyPoints(List<PVector> points) {
        ArrayList<PVector> out = new ArrayList<>();
        if (points == null || points.isEmpty()) return out;

        for (PVector p : points) {
            PVector q = apply(p);
            if (q != null) out.add(q);
        }
        return out;
    }

    /**
     * Applies the policy to a GestureSchedule.
     *
     * CLIP / WRAP / REFLECT:
     *   preserve point count and times.
     *
     * KEEP_TIME:
     *   omit out-of-bounds points and keep surviving timestamps unchanged.
     *
     * SKIP_TIME:
     *   omit out-of-bounds points and compress time across removed spans.
     *
     * Returns null if the input is null.
     */
    public GestureSchedule applySchedule(GestureSchedule sched) {
        if (sched == null) return null;
        if (sched.points == null || sched.timesMs == null) {
            throw new IllegalArgumentException("GestureSchedule points/times must not be null");
        }
        if (sched.points.size() != sched.timesMs.length) {
            throw new IllegalArgumentException("GestureSchedule points/times mismatch");
        }
        if (sched.points.isEmpty()) return sched;

        switch (mode) {
            case CLIP:
            case WRAP:
            case REFLECT:
                return applyMappedSchedule(sched);

            case KEEP_TIME:
                return applyKeepTimeSchedule(sched);

            case SKIP_TIME:
                return applySkipTimeSchedule(sched);

            default:
                throw new IllegalStateException("Unhandled boundary mode: " + mode);
        }
    }

    private GestureSchedule applyMappedSchedule(GestureSchedule sched) {
        ArrayList<PVector> pts = new ArrayList<>(sched.points.size());
        float[] times = new float[sched.timesMs.length];

        for (int i = 0; i < sched.points.size(); i++) {
            pts.add(apply(sched.points.get(i)));
            times[i] = sched.timesMs[i];
        }
        return new GestureSchedule(pts, times);
    }

    /**
     * Drop out-of-bounds points, preserve original timestamps of survivors.
     */
    private GestureSchedule applyKeepTimeSchedule(GestureSchedule sched) {
        ArrayList<PVector> pts = new ArrayList<>();
        ArrayList<Float> times = new ArrayList<>();

        for (int i = 0; i < sched.points.size(); i++) {
            PVector p = sched.points.get(i);
            if (contains(p)) {
                pts.add(p.copy());
                times.add(sched.timesMs[i]);
            }
        }
        return new GestureSchedule(pts, toFloatArray(times));
    }

    /**
     * Drop out-of-bounds points, compress time across removed spans.
     *
     * Example:
     * original times  [0, 100, 200, 300, 600]
     * if points at 200,300 are dropped:
     * result times    [0, 100, 400]
     *
     * More precisely: each surviving point keeps its local in-bounds time,
     * but accumulated durations spent entirely in skipped regions are removed.
     */
    private GestureSchedule applySkipTimeSchedule(GestureSchedule sched) {
        ArrayList<PVector> pts = new ArrayList<>();
        ArrayList<Float> times = new ArrayList<>();

        float skippedDuration = 0f;

        for (int i = 0; i < sched.points.size(); i++) {
            PVector p = sched.points.get(i);
            boolean in = contains(p);

            if (i > 0) {
                boolean prevIn = contains(sched.points.get(i - 1));
                float dt = sched.timesMs[i] - sched.timesMs[i - 1];

                // If both endpoints of the span are out of bounds, remove that time.
                // This is the conservative/simple compression rule.
                if (!prevIn && !in) {
                    skippedDuration += dt;
                }
            }

            if (in) {
                pts.add(p.copy());
                times.add(sched.timesMs[i] - skippedDuration);
            }
        }

        return new GestureSchedule(pts, toFloatArray(times));
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /**
     * Wrap v into [origin, origin + size).
     * Since our public bounds are inclusive, callers should pass size=max-min+1.
     */
    private static float wrap(float v, float origin, float size) {
        if (size <= 0f) return origin;
        float r = (v - origin) % size;
        if (r < 0f) r += size;
        return origin + r;
    }

    /**
     * Reflect v repeatedly into [lo, hi].
     * Works for arbitrarily large excursions.
     */
    private static float reflect(float v, float lo, float hi) {
        if (hi <= lo) return lo;

        float span = hi - lo;
        float period = 2.0f * span;

        float t = (v - lo) % period;
        if (t < 0f) t += period;

        if (t <= span) {
            return lo + t;
        } else {
            return hi - (t - span);
        }
    }

    private static float[] toFloatArray(List<Float> vals) {
        float[] out = new float[vals.size()];
        for (int i = 0; i < vals.size(); i++) {
            out[i] = vals.get(i);
        }
        return out;
    }

    @Override
    public String toString() {
        return "PABoundsPolicy[" +
                "minX=" + minX +
                ", minY=" + minY +
                ", maxX=" + maxX +
                ", maxY=" + maxY +
                ", mode=" + mode +
                "]";
    }
}