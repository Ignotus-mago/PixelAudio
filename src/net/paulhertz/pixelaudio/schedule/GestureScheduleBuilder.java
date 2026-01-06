package net.paulhertz.pixelaudio.schedule;

import processing.core.PVector;

import java.util.ArrayList;
import java.util.List;

import net.paulhertz.pixelaudio.curves.PACurveMaker;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig;
//import net.paulhertz.pixelaudio.granular.GestureGranularConfig.HopMode;
//import net.paulhertz.pixelaudio.granular.GestureGranularConfig.WarpShape;

public final class GestureScheduleBuilder {

	  public GestureSchedule build(PACurveMaker brush, GestureGranularConfig cfg, float sampleRate) {
	    if (brush == null) throw new IllegalArgumentException("brush is null");
	    if (cfg == null) throw new IllegalArgumentException("cfg is null");
	    if (sampleRate <= 0) throw new IllegalArgumentException("sampleRate must be > 0");

	    // 1) Base schedule from PACurveMaker (times are ms)
	    GestureSchedule s = getBaseSchedule(brush, cfg);

	    if (s == null || s.isEmpty()) {
	      return (s != null) ? s : new GestureSchedule(new ArrayList<>(), new float[0]);
	    }

	    // Normalize so time starts at 0 (PACurveMaker may already do this, but we guarantee it here)
	    s = new GestureSchedule(s.points, GestureSchedule.normalizeTimesToStartAtZero(s.timesMs));
	    GestureSchedule.enforceNonDecreasing(s.timesMs);

	    // 2) Hop override: FIXED replaces timing with fixed hop (still ms)
	    if (cfg.hopMode == GestureGranularConfig.HopMode.FIXED) {
	      s = toFixedHopScheduleMs(s, cfg.hopLengthSamples, sampleRate);
	    }

	    // 3) Optional resample (value-driven: 0 means "no resample")
	    if (cfg.resampleCount > 0) {
	      s = resampleToCount(s, cfg.resampleCount);
	    }

	    // 4) Optional duration scale (value-driven: 0 means "no duration override")
	    if (cfg.targetDurationMs > 0) {
	      s = scaleToDurationMs(s, cfg.targetDurationMs);
	    }

	    // 5) Optional warp (shape LINEAR means "off")
	    if (cfg.warpShape != GestureGranularConfig.WarpShape.LINEAR) {
	      s = warpScheduleTimesMs(s, cfg.warpShape, cfg.warpExponent);
	    }

	    // Final safety
	    GestureSchedule.enforceNonDecreasing(s.timesMs);
	    return s;
	  }

	  private GestureSchedule getBaseSchedule(PACurveMaker brush, GestureGranularConfig cfg) {
	    switch (cfg.pathMode) {
	      case ALL_POINTS:
	        return brush.getAllPointsSchedule();
	      case REDUCED_POINTS:
	        return brush.getReducedSchedule(cfg.rdpEpsilon);
	      case CURVE_POINTS:
	        return brush.getCurveSchedule(cfg.rdpEpsilon, cfg.curveSteps, cfg.useArcLengthTime);
	      default:
	        return brush.getAllPointsSchedule();
	    }
	  }

	  // ------------------------------------------------------------
	  // HopMode.FIXED: rewrite times using hopLengthSamples
	  // ------------------------------------------------------------

	  private static GestureSchedule toFixedHopScheduleMs(GestureSchedule in, int hopSamples, float sampleRate) {
	    hopSamples = Math.max(1, hopSamples);

	    int n = in.size();
	    float[] tMs = new float[n];
	    if (n == 0) return new GestureSchedule(in.points, tMs);

	    tMs[0] = 0f;
	    float hopMs = 1000f * hopSamples / sampleRate;

	    for (int i = 1; i < n; i++) {
	      tMs[i] = tMs[i - 1] + hopMs;
	    }

	    return new GestureSchedule(in.points, tMs);
	  }

	  // ------------------------------------------------------------
	  // Resample schedule to count (interpolate points + timesMs)
	  // ------------------------------------------------------------

	  private static GestureSchedule resampleToCount(GestureSchedule in, int count) {
	    count = Math.max(2, count);

	    int n = in.size();
	    if (n <= 1 || count == n) return in;

	    // Param u by index in [0..1]
	    float[] u = new float[n];
	    for (int i = 0; i < n; i++) u[i] = (float) i / (n - 1);

	    ArrayList<PVector> outPts = new ArrayList<>(count);
	    float[] outT = new float[count];

	    for (int k = 0; k < count; k++) {
	      float uk = (float) k / (count - 1);
	      outPts.add(lerpPointByU(in.points, u, uk));
	      outT[k] = lerpFloatByU(in.timesMs, u, uk);
	    }

	    GestureSchedule.enforceNonDecreasing(outT);
	    return new GestureSchedule(outPts, outT);
	  }

	  private static PVector lerpPointByU(List<PVector> pts, float[] u, float uk) {
	    int i = findSegment(u, uk);
	    if (i >= pts.size() - 1) return pts.get(pts.size() - 1).copy();

	    float denom = Math.max(1e-9f, (u[i + 1] - u[i]));
	    float t = (uk - u[i]) / denom;

	    return PVector.lerp(pts.get(i), pts.get(i + 1), t);
	  }

	  private static float lerpFloatByU(float[] a, float[] u, float uk) {
	    int i = findSegment(u, uk);
	    if (i >= a.length - 1) return a[a.length - 1];

	    float denom = Math.max(1e-9f, (u[i + 1] - u[i]));
	    float t = (uk - u[i]) / denom;

	    return a[i] + t * (a[i + 1] - a[i]);
	  }

	  private static int findSegment(float[] u, float uk) {
	    int hi = u.length - 1;
	    if (uk <= u[0]) return 0;
	    if (uk >= u[hi]) return hi - 1;

	    for (int i = 0; i < hi; i++) {
	      if (uk >= u[i] && uk <= u[i + 1]) return i;
	    }
	    return hi - 1;
	  }

	  // ------------------------------------------------------------
	  // Scale schedule duration to targetDurationMs
	  // ------------------------------------------------------------

	  private static GestureSchedule scaleToDurationMs(GestureSchedule in, int targetDurationMs) {
	    int n = in.size();
	    if (n == 0) return in;

	    float[] t = in.timesMs.clone();
	    float last = t[n - 1];

	    if (last <= 0f) return in;

	    float s = ((float) targetDurationMs) / last;
	    for (int i = 0; i < n; i++) t[i] *= s;

	    GestureSchedule.enforceNonDecreasing(t);
	    return new GestureSchedule(in.points, t);
	  }

	  // ------------------------------------------------------------
	  // Warp times: index-space warp, preserves start/end, stays in ms
	  // ------------------------------------------------------------

	  private static GestureSchedule warpScheduleTimesMs(
	      GestureSchedule in, GestureGranularConfig.WarpShape shape, float exponent) {

	    int n = in.size();
	    if (n == 0) return in;

	    float last = in.timesMs[n - 1];
	    if (last <= 0f) return in;

	    float[] out = new float[n];
	    for (int i = 0; i < n; i++) {
	      float u = (float) i / (n - 1);
	      float w = warpU(u, shape, exponent);
	      out[i] = w * last;
	    }

	    GestureSchedule.enforceNonDecreasing(out);
	    return new GestureSchedule(in.points, out);
	  }

	  private static float warpU(float u, GestureGranularConfig.WarpShape shape, float exponent) {
	    u = (u < 0f) ? 0f : (u > 1f ? 1f : u);
	    float e = Math.max(1e-6f, exponent);

	    switch (shape) {
	      case EXP:
	        return (float) Math.pow(u, e);
	      case SQRT:
	        return (float) Math.pow(u, 1.0 / e);
	      case CUSTOM:
	        return easeInOutPow(u, exponent);
	      case LINEAR:
	      default:
	        return u;
	    }
	  }
	  
	  static float easeInOutPow(float u, float exp) {
		  u = constrain01(u);
		  float e = Math.max(1e-6f, exp);

		  if (u < 0.5f) {
			  return 0.5f * (float)Math.pow(2f * u, e);
		  } else {
			  return 1f - 0.5f * (float)Math.pow(2f * (1f - u), e);
		  }
	  }

	  static float constrain01(float u) {
		  return (u < 0f) ? 0f : (u > 1f ? 1f : u);
	  }

	  
	}
