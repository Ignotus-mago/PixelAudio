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
     * @param curveMaker      source of gesture points and gesture times
     * @param cfg             user-configured granular settings
     * @param outputSampleRate sample rate of the audio context (for fixed hops)
     */
    public GestureSchedule build(PACurveMaker curveMaker,
                                 GestureGranularConfig cfg,
                                 float outputSampleRate) {

        // 1) Select base point list
        List<PVector> basePoints = selectPoints(curveMaker, cfg);

        if (basePoints.isEmpty()) {
            return new GestureSchedule(basePoints, new float[0]);
        }

        // 2) Build base time list (gesture vs fixed hop)
        float[] baseTimesMs = buildBaseTimes(curveMaker, cfg, basePoints.size(), outputSampleRate);

        // 3) Apply resampling / duration / warp
        return applyTimingTransforms(basePoints, baseTimesMs, cfg);
    }

    // ---------------------------------------------------------------------
    // Step 1: point selection
    // ---------------------------------------------------------------------
    private List<PVector> selectPoints(PACurveMaker curveMaker,
                                       GestureGranularConfig cfg) {
        switch (cfg.pathMode) {
            case ALL_POINTS:
                return curveMaker.getDragPoints();           // adjust to your API
            case REDUCED_POINTS:
                return curveMaker.getReducedPoints(cfg.rdpEpsilon);
            case CURVE_POINTS:
                return curveMaker.getEventPoints(cfg.bezierCurveSteps);
            default:
                // fallback to all points
                return curveMaker.getDragPoints();
        }
    }

    // ---------------------------------------------------------------------
    // Step 2: timing selection
    // ---------------------------------------------------------------------
    
    private float[] buildBaseTimes(PACurveMaker curveMaker,
                                   GestureGranularConfig cfg,
                                   int pointCount,
                                   float sampleRate) {
        if (cfg.hopMode == HopMode.GESTURE) {
            // assumes curveMaker.dragTimes is already in ms or easily converted
            return copyOrNormalizeGestureTimes(curveMaker, pointCount);
        } 
        else {
            return generateFixedHopTimes(pointCount, cfg.hopLengthSamples, sampleRate);
        }
    }

    /**
     * Copy or adapt gesture times from PACurveMaker.
     * Adjust as needed to match your actual dragTimes structure.
     */
    private float[] copyOrNormalizeGestureTimes(PACurveMaker curveMaker, int pointCount) {
    	//get curveMaker.dragTimes offsets only, starting at 0, time stamp omitted
        int[] src = curveMaker.getDragOffsetsAsInts(); 
        // Ensure we trim or pad to match pointCount
        float[] times = new float[pointCount];
        int n = Math.min(pointCount, src.length);
        System.arraycopy(src, 0, times, 0, n);
        // If src shorter than points, linearly extend based on last delta
        if (n < pointCount && n > 1) {
            float last = src[n - 1];
            float prev = src[n - 2];
            float step = last - prev;
            for (int i = n; i < pointCount; i++) {
                times[i] = last + step * (i - (n - 1));
            }
        }
        return times;
    }

    /**
     * Generate fixed-hop times from a hop length (in samples) and output sample rate.
     */
    private float[] generateFixedHopTimes(int count, int hopLengthSamples, float sampleRate) {
        float hopMs = 1000.0f * hopLengthSamples / sampleRate;
        float[] times = new float[count];
        for (int i = 0; i < count; i++) {
            times[i] = i * hopMs;
        }
        return times;
    }

    // ---------------------------------------------------------------------
    // Step 3: resample / duration / warp
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
        // 3b) Duration scaling + warp
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
            float t = (targetCount == 1) ? 0 : (float) i / (targetCount - 1);
            float fIndex = t * (n - 1);
            int i0 = (int) Math.floor(fIndex);
            int i1 = Math.min(i0 + 1, n - 1);
            float frac = fIndex - i0;

            PVector p0 = src.get(i0);
            PVector p1 = src.get(i1);
            float x = lerp(p0.x, p1.x, frac);
            float y = lerp(p0.y, p1.y, frac);
            out.add(new PVector(x, y));
        }
        return out;
    }

    private float[] resampleTimes(float[] src, int targetCount) {
        float[] out = new float[targetCount];
        int n = src.length;
        for (int i = 0; i < targetCount; i++) {
            float t = (targetCount == 1) ? 0 : (float) i / (targetCount - 1);
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
        int n = timesMs.length;
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

    private float[] applyWarp(float[] tUnit,
                              WarpShape shape,
                              float exponent) {
        float[] out = new float[tUnit.length];
        for (int i = 0; i < tUnit.length; i++) {
            float t = tUnit[i];
            switch (shape) {
                case LINEAR:
                    out[i] = t;
                    break;
                case EXP:
                    out[i] = (float) Math.pow(t, exponent);
                    break;
                case SQRT:
                    out[i] = (float) Math.pow(t, 1.0f / exponent);
                    break;
                case CUSTOM:
                    // plug in whatever custom curve you like
                    out[i] = (float) Math.pow(t, exponent);
                    break;
                default:
                    out[i] = t;
            }
        }
        return out;
    }

    private float[] scaleUnitTimesToMs(float[] unitTimes, int targetDurationMs) {
        float[] out = new float[unitTimes.length];
        float duration = (float) targetDurationMs;
        for (int i = 0; i < unitTimes.length; i++) {
            out[i] = unitTimes[i] * duration;
        }
        return out;
    }
    
	/**
	 * Good old lerp.
	 * @param a		first bound, typically a minimum value
	 * @param b		second bound, typically a maximum value
	 * @param f		scaling value, from 0..1 to interpolate between a and b, but can go over or under
	 * @return		a value between a and b, scaled by f (if 0 <= f >= 1).
	 */
	public final static float lerp(float a, float b, float f) {
	    return a + f * (b - a);
	}

}
