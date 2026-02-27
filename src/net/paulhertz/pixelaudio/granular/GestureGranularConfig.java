package net.paulhertz.pixelaudio.granular;

import java.util.Objects;

import net.paulhertz.pixelaudio.sampler.ADSRParams;
import net.paulhertz.pixelaudio.schedule.GestureSchedule;

public final class GestureGranularConfig {

  // ----- CORE ENUMS ----- //
  public enum PathMode {
    ALL_POINTS,
    REDUCED_POINTS,
    CURVE_POINTS
  }

  public enum HopMode {
    GESTURE,
    FIXED
  }

  public enum TimeTransform {
    RAW_GESTURE,      // no resampling, duration change, or warp
    RESAMPLED_COUNT,  // resample to targetCount
    DURATION_SCALED,  // scale to targetDurationMs
    WARPED            // apply warp f(u)
  }

  public enum WarpShape {
    LINEAR,
    EXP,
    SQRT,
    CUSTOM
  }

  // ----- IMMUTABLE FIELDS ----- //

  // Path selection
  public final PathMode pathMode;
  public final float rdpEpsilon;  // used for REDUCED_POINTS / CURVE_POINTS
  public final int curveSteps;    // polygonized Bezier path divisions
  public final float curveBias;   // reserved

  // Hop
  public final HopMode hopMode;
  public final int hopLengthSamples;   // only meaningful for FIXED

  // Timing
  public final TimeTransform timingMode;
  public final int resampleCount;      // RESAMPLED_COUNT
  public final int targetDurationMs;   // DURATION_SCALED / WARPED
  public final WarpShape warpShape;    // WARPED
  public final float warpExponent;     // EXP / SQRT variants

  // Grain / synthesis
  public final int grainLengthSamples;
  public final ADSRParams env;         // consider copying if ADSRParams is mutable
  // TODO note duration, particularl for Sampler instrument
  public final float gainDb;
  public final float pitchSemitones;

  // Scheduled grain event modeling
  public final int burstGrains;
  public final boolean autoBurstGainComp;

  // Path timing calculation
  public final boolean useArcLengthTime;

  private GestureGranularConfig(Builder b) {
    this.pathMode = Objects.requireNonNull(b.pathMode, "pathMode");
    this.rdpEpsilon = b.rdpEpsilon;
    this.curveSteps = b.curveSteps;
    this.curveBias = b.curveBias;

    this.hopMode = Objects.requireNonNull(b.hopMode, "hopMode");
    this.hopLengthSamples = b.hopLengthSamples;

    this.timingMode = Objects.requireNonNull(b.timingMode, "timingMode");
    this.resampleCount = b.resampleCount;
    this.targetDurationMs = b.targetDurationMs;
    this.warpShape = Objects.requireNonNull(b.warpShape, "warpShape");
    this.warpExponent = b.warpExponent;

    this.grainLengthSamples = b.grainLengthSamples;
    this.env = Objects.requireNonNull(b.env, "env");
    this.gainDb = b.gainDb;
    this.pitchSemitones = b.pitchSemitones;

    this.burstGrains = b.burstGrains;
    this.autoBurstGainComp = b.autoBurstGainComp;
    this.useArcLengthTime = b.useArcLengthTime;
  }
  

  // ----- BUILDER (GUI MUTATES THIS) ----- //
  public static final class Builder {
    // Path
    public PathMode pathMode = PathMode.ALL_POINTS;
    public float rdpEpsilon = 2.0f;
    public int curveSteps = 8;
    public float curveBias = 0f;

    // Hop
    public HopMode hopMode = HopMode.GESTURE;
    public int hopLengthSamples = 512;

    // Timing (single source of truth)
    public TimeTransform timingMode = TimeTransform.RAW_GESTURE;
    // GUI baseline (not used by DSP directly)
    public int basePointCount = 0;
    public int baseDurationMs = 0;
    // GUI override targets ('0' means no override)
    public int resampleCount = 0;
    public int targetDurationMs = 0;
    public WarpShape warpShape = WarpShape.LINEAR;
    public float warpExponent = 2.0f;

    // Grain / synthesis
    public int grainLengthSamples = 2048;
    public ADSRParams env = new ADSRParams(1.0f, 0.02f, 0.06f, 0.9f, 0.10f);
    public float gainDb = -6.0f;
    public float pitchSemitones = 0f;

    // Events
    public int burstGrains = 1;
    public boolean autoBurstGainComp = false;

    // Timing from geometry
    public boolean useArcLengthTime = false;
    

    /** Build an immutable snapshot safe to pass into scheduler/renderer threads. */
    public GestureGranularConfig build() {
      // If you want hard guarantees, uncomment validate().
      // validate();
      return new GestureGranularConfig(this);
    }
    
    public void setBaselinesFromSchedule(GestureSchedule s) {
    	basePointCount = s.size();
    	baseDurationMs = Math.round(s.durationMs());
    }

    /** Optional: throw early if GUI creates impossible states. */
    public void validate() {
    	if (rdpEpsilon <= 0f) throw new IllegalArgumentException("rdpEpsilon must be > 0");
    	if (curveSteps < 1) throw new IllegalArgumentException("curveSteps must be >= 1");
    	if (grainLengthSamples < 1) throw new IllegalArgumentException("grainLengthSamples must be >= 1");
    	if (hopLengthSamples < 1) throw new IllegalArgumentException("hopLengthSamples must be >= 1");
    	if (burstGrains < 1) throw new IllegalArgumentException("burstGrains must be >= 1");

    	switch (timingMode) {
    	case RESAMPLED_COUNT:
    		if (resampleCount < 2) throw new IllegalArgumentException("targetCount must be >= 2");
    		break;
    	case DURATION_SCALED:
    	case WARPED:
    		if (targetDurationMs < 1) throw new IllegalArgumentException("targetDurationMs must be >= 1");
    		break;
    	case RAW_GESTURE:
    	default:
    		break;
    	}
    }
    
    public Builder copy() {
    	Builder b = new Builder();
    	b.pathMode = this.pathMode;
    	b.rdpEpsilon = this.rdpEpsilon;
    	b.curveSteps = this.curveSteps;
    	b.curveBias = this.curveBias;
    	b.hopMode = this.hopMode;
    	b.hopLengthSamples = this.hopLengthSamples;
    	b.resampleCount = this.resampleCount;
    	b.targetDurationMs = this.targetDurationMs;
    	b.warpShape = this.warpShape;
    	b.warpExponent = this.warpExponent;
    	b.grainLengthSamples = this.grainLengthSamples;
    	b.env = this.env; // OK for presets
    	b.gainDb = this.gainDb;
    	b.pitchSemitones = this.pitchSemitones;
    	b.burstGrains = this.burstGrains;
    	b.autoBurstGainComp = this.autoBurstGainComp;
    	b.useArcLengthTime = this.useArcLengthTime;
    	return b;
    }
    
    public void copyFrom(GestureGranularConfig.Builder src) {
    	this.pathMode = src.pathMode;
    	this.rdpEpsilon = src.rdpEpsilon;
    	this.curveSteps = src.curveSteps;
    	this.curveBias = src.curveBias;

    	this.hopMode = src.hopMode;
    	this.hopLengthSamples = src.hopLengthSamples;

    	this.resampleCount = src.resampleCount;
    	this.targetDurationMs = src.targetDurationMs;
    	this.warpShape = src.warpShape;
    	this.warpExponent = src.warpExponent;

    	this.grainLengthSamples = src.grainLengthSamples;
    	this.env = src.env; // presets ok
    	this.gainDb = src.gainDb;
    	this.pitchSemitones = src.pitchSemitones;

    	this.burstGrains = src.burstGrains;
    	this.autoBurstGainComp = src.autoBurstGainComp;
    	this.useArcLengthTime = src.useArcLengthTime;
    }


  }
  
  public static GestureGranularConfig defaultConfig() {
      return new Builder().build();
  }
  
  // ----- UTILITY METHODS (from your class) ----- //

  public float gainLinear() {
    return (float) Math.pow(10.0, gainDb / 20.0);
  }

  public float pitchRatio() {
    return (float) Math.pow(2.0, pitchSemitones / 12.0);
  }

  public boolean isRawGestureTiming() {
    return timingMode == TimeTransform.RAW_GESTURE;
  }
  
  public GestureGranularParams toParams() {

	  return GestureGranularParams.builder()

			  // -------------------------
			  // Core grain synthesis
			  // -------------------------
			  .grainLengthSamples(grainLengthSamples)
			  .hopLengthSamples(hopLengthSamples)
			  .burstGrains(burstGrains)
			  .autoBurstGainComp(autoBurstGainComp)

			  // -------------------------
			  // Gain / pitch
			  // -------------------------
			  .gainLinear(gainLinear())          // convert dB → linear once
			  .pitchRatio(pitchRatio())          // convert semitones → ratio once
			  .pan(0f)                           // default pan (can be overridden per-grain)

			  // -------------------------
			  // Envelope / looping
			  // -------------------------
			  .env(env)
			  .looping(false)                    // granular bursts are one-shot by default

			  // -------------------------
			  // Hop semantics
			  // -------------------------
			  .hopMode(
					  (hopMode == HopMode.GESTURE)
					  ? GestureGranularParams.HopMode.GESTURE
							  : GestureGranularParams.HopMode.FIXED
					  )

			  // -------------------------
			  // Time transform
			  // -------------------------
			  .timeTransform(
					  switch (timingMode) {
					  case RAW_GESTURE      -> GestureGranularParams.TimeTransform.RAW_GESTURE;
					  case RESAMPLED_COUNT  -> GestureGranularParams.TimeTransform.RESAMPLED_COUNT;
					  case DURATION_SCALED  -> GestureGranularParams.TimeTransform.DURATION_SCALED;
					  case WARPED           -> GestureGranularParams.TimeTransform.WARPED;
					  }
					  )
			  .targetCount(resampleCount)
			  .targetDurationMs(targetDurationMs)
			  .warpShape(
					  switch (warpShape) {
					  case LINEAR -> GestureGranularParams.WarpShape.LINEAR;
					  case EXP    -> GestureGranularParams.WarpShape.EXP;
					  case SQRT   -> GestureGranularParams.WarpShape.SQRT;
					  case CUSTOM -> GestureGranularParams.WarpShape.CUSTOM;
					  }
					  )
			  .warpExponent(warpExponent)

			  // -------------------------
			  // Grain window (optional)
			  // -------------------------
			  .grainWindow(null) // or pass a default if you store one in config

			  .build();
  }

  

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("GestureGranularConfig {\n");

    // Path
    sb.append("  Path: ").append(pathMode);
    switch (pathMode) {
      case REDUCED_POINTS:
        sb.append(" (rdpEpsilon=").append(rdpEpsilon).append(")");
        break;
      case CURVE_POINTS:
        sb.append(" (rdpEpsilon=").append(rdpEpsilon)
          .append(", bezierCurveSteps=").append(curveSteps).append(")");
        break;
      default:
        break;
    }
    sb.append("\n");

    // Hop
    sb.append("  Hop: ").append(hopMode);
    if (hopMode == HopMode.FIXED) {
      sb.append(" (hopLengthSamples=").append(hopLengthSamples).append(")");
    }
    sb.append("\n");

    // Timing
    sb.append("  Timing: ").append(timingMode);
    switch (timingMode) {
      case RESAMPLED_COUNT:
        sb.append(" (targetCount=").append(resampleCount).append(")");
        break;
      case DURATION_SCALED:
        sb.append(" (targetDurationMs=").append(targetDurationMs).append(")");
        break;
      case WARPED:
        sb.append(" (targetDurationMs=").append(targetDurationMs)
          .append(", warpShape=").append(warpShape)
          .append(", warpExponent=").append(warpExponent).append(")");
        break;
      default:
        break;
    }
    sb.append("\n");

    // Grain / synthesis
    sb.append("  Grain: length=").append(grainLengthSamples).append(" samples");
    if (hopMode == HopMode.FIXED) {
      sb.append(", hop=").append(hopLengthSamples).append(" samples");
    }
    sb.append("\n");

    sb.append("  ADSR: ").append(env).append("\n");

    sb.append("  Gain: ").append(gainDb).append(" dB (linear=")
      .append(String.format("%.3f", gainLinear())).append(")\n");

    sb.append("  Pitch: ").append(pitchSemitones).append(" st (ratio=")
      .append(String.format("%.3f", pitchRatio())).append(")\n");

    sb.append("  Burst: ").append(burstGrains);
    if (autoBurstGainComp) sb.append(" (autoGainComp)");
    sb.append("\n");

    sb.append("  useArcLengthTime: ").append(useArcLengthTime).append("\n");
    sb.append("}");

    return sb.toString();
  }
}
