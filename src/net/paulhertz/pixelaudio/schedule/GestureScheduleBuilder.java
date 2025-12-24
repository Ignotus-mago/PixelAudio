package net.paulhertz.pixelaudio.schedule;

import processing.core.PVector;

import java.util.ArrayList;
import java.util.List;

import net.paulhertz.pixelaudio.curves.PACurveMaker;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig.HopMode;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig.WarpShape;

public final class GestureScheduleBuilder {

    /**
     * Build a GestureSchedule from a PACurveMaker and GestureGranularConfig.
     *
     * Pipeline:
     *   1) Get base schedule from PACurveMaker (points + matching gesture times).
     *   2) Optionally replace times with FIXED hop times.
     *   3) Apply transforms: resample, duration scaling, warp.
     *   4) Normalize times to start at 0 and enforce non-decreasing.
     *
     * @param curveMaker       source of gesture points and gesture times
     * @param cfg              user-configured granular settings
     * @param outputSampleRate sample rate of the audio context (for fixed hops)
     */
    public GestureSchedule build(PACurveMaker curveMaker,
                                 GestureGranularConfig cfg,
                                 float outputSampleRate) {

        if (curveMaker == null) throw new IllegalArgumentException("curveMaker must not be null");
        if (cfg == null) throw new IllegalArgumentException("cfg must not be null");

        GestureSchedule base = baseScheduleFromCurve(curveMaker, cfg);
        if (base == null || base.isEmpty()) {
            // keep invariant: points.size == times.length
            return new GestureSchedule((base != null) ? base.points : new ArrayList<>(), new float[0]);
        }

        // Optional: normalize base times immediately so downstream math is consistent.
        float[] baseTimes = GestureSchedule.normalizeTimesToStartAtZero(base.timesMs);
        GestureSchedule.enforceNonDecreasing(baseTimes);
        base = new GestureSchedule(base.points, baseTimes);

        // Step 2: timing selection override
        if (cfg.hopMode == HopMode.FIXED) {
            float[] fixed = generateFixedHopTimes(base.size(), cfg.hopLengthSamples, outputSampleRate);
            base = new GestureSchedule(base.points, fixed);
        }

        // Step 3: transforms (may replace points/times)
        GestureSchedule out = applyTimingTransforms(base.points, base.timesMs, cfg);

        // Final sanitation (esp. after warp / duration scale)
        float[] t = GestureSchedule.normalizeTimesToStartAtZero(out.timesMs);
        GestureSchedule.enforceNonDecreasing(t);
        return new GestureSchedule(out.points, t);
    }

    // ---------------------------------------------------------------------
    // Base schedule selection (delegates to PACurveMaker)
    // ---------------------------------------------------------------------

    private GestureSchedule baseScheduleFromCurve(PACurveMaker curveMaker, GestureGranularConfig cfg) {
        switch (cfg.pathMode) {
            case ALL_POINTS:
                return curveMaker.getAllPointsSchedule();
            case REDUCED_POINTS:
                return curveMaker.getReducedSchedule(cfg.rdpEpsilon);
            case CURVE_POINTS:
            default:
                // cfg.useArcLengthTime should exist in your config
                return curveMaker.getCurveSchedule(cfg.rdpEpsilon, cfg.curveSteps, cfg.useArcLengthTime);
        }
    }

    // ---------------------------------------------------------------------
    // Fixed timing
    // ---------------------------------------------------------------------

    /**
     * Generate fixed-hop times from a hop length (in samples) and output sample rate.
     * Returns a float[] in milliseconds starting at 0.
     */
    private float[] generateFixedHopTimes(int count, int hopLengthSamples, float sampleRate) {
        if (count <= 0) return new float[0];
        if (sampleRate <= 0f) throw new IllegalArgumentException("sampleRate must be > 0");
        int hop = Math.max(1, hopLengthSamples);

        float hopMs = 1000.0f * hop / sampleRate;
        float[] times = new float[count];
        for (int i = 0; i < count; i++) {
            times[i] = i * hopMs;
        }
        return times;
    }

    // ---------------------------------------------------------------------
    // Transforms: resample / duration / warp
    // ---------------------------------------------------------------------

    private GestureSchedule applyTimingTransforms(List<PVector> basePoints,
                                                  float[] baseTimesMs,
                                                  GestureGranularConfig cfg) {

        List<PVector> pts = basePoints;
        float[] times = baseTimesMs;

        // 3a) Resample count (points & times) if requested
        if (cfg.useResampled) {
            int targetCount = cfg.targetCount;
            if (targetCount > 1 && pts.size() > 1) {
                pts   = resamplePoints(pts, targetCount);
                times = resampleTimes(times, targetCount);
            }
        }

        // 3b) Duration scaling and/or warp
        if (cfg.useNewDuration || cfg.useWarp) {
            float[] unitTimes = normalizeTimesToUnit(times);
            if (cfg.useWarp) {
                unitTimes = applyWarp(unitTimes, cfg.warpShape, cfg.warpExponent);
            }
            times = scaleUnitTimesToMs(unitTimes, cfg.targetDurationMs);
        }

        return new GestureSchedule(pts, times);
    }

    // ---- Resampling utilities -------------------------------------------

    private List<PVector> resamplePoints(List<PVector> src, int targetCount) {
        List<PVector> out = new ArrayList<>(targetCount);
        int n = src.size();

        for (int i = 0; i < targetCount; i++) {
            float t = (targetCount == 1) ? 0f : (float) i / (targetCount - 1);
            float fIndex = t * (n - 1);
            int i0 = (int) Math.floor(fIndex);
            int i1 = Math.min(i0 + 1, n - 1);
            float frac = fIndex - i0;

            PVector p0 = src.get(i0);
            PVector p1 = src.get(i1);

            out.add(new PVector(
                    lerp(p0.x, p1.x, frac),
                    lerp(p0.y, p1.y, frac)
            ));
        }
        return out;
    }

    private float[] resampleTimes(float[] src, int targetCount) {
        if (targetCount <= 0) return new float[0];
        if (src == null || src.length == 0) return new float[targetCount];

        float[] out = new float[targetCount];
        int n = src.length;

        for (int i = 0; i < targetCount; i++) {
            float t = (targetCount == 1) ? 0f : (float) i / (targetCount - 1);
            float fIndex = t * (n - 1);
            int i0 = (int) Math.floor(fIndex);
            int i1 = Math.min(i0 + 1, n - 1);
            float frac = fIndex - i0;
            out[i] = src[i0] + frac * (src[i1] - src[i0]);
        }
        return out;
    }

    // ---- Normalize, warp, scale -----------------------------------------

    private float[] normalizeTimesToUnit(float[] timesMs) {
        int n = (timesMs == null) ? 0 : timesMs.length;
        float[] out = new float[n];
        if (n == 0) return out;

        float start = timesMs[0];
        float end   = timesMs[n - 1];
        float duration = Math.max(1e-6f, end - start);

        for (int i = 0; i < n; i++) {
            out[i] = (timesMs[i] - start) / duration;
        }
        return out;
    }

    private float[] applyWarp(float[] tUnit, WarpShape shape, float exponent) {
        float[] out = new float[tUnit.length];
        float e = (exponent <= 0f) ? 1f : exponent;

        for (int i = 0; i < tUnit.length; i++) {
            float t = tUnit[i];

            switch (shape) {
                case LINEAR:
                    out[i] = t;
                    break;
                case EXP:
                    out[i] = (float) Math.pow(t, e);
                    break;
                case SQRT:
                    out[i] = (float) Math.pow(t, 1.0f / e);
                    break;
                case CUSTOM:
                    // same as EXP for now; plug in a custom curve later
                    out[i] = (float) Math.pow(t, e);
                    break;
                default:
                    out[i] = t;
            }
        }
        return out;
    }

    private float[] scaleUnitTimesToMs(float[] unitTimes, int targetDurationMs) {
        float[] out = new float[unitTimes.length];
        float duration = Math.max(0f, (float) targetDurationMs);

        for (int i = 0; i < unitTimes.length; i++) {
            out[i] = unitTimes[i] * duration;
        }
        return out;
    }

    /**
     * Good old lerp.
     *
     * @param a first bound, typically minimum
     * @param b second bound, typically maximum
     * @param f scaling value (0..1 typical, but can go outside)
     */
    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }
}
