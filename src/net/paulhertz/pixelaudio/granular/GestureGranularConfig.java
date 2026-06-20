/*
 *  Copyright (c) 2024 - 2025 by Paul Hertz <ignotus@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.paulhertz.pixelaudio.granular;

import java.util.Objects;

import net.paulhertz.pixelaudio.sampler.ADSRParams;
import net.paulhertz.pixelaudio.schedule.GestureSchedule;

/**
 * End-user configuration for gesture-driven granular and sampler playback.
 *
 * <p>This class is the editable configuration model used by PixelAudio's gesture examples.
 * A {@link Builder} is convenient for GUI controls, presets, and JSON serialization; calling
 * {@link Builder#build()} creates an immutable snapshot that can be used to build a
 * {@link GestureSchedule} and converted to runtime synthesis parameters with {@link #toParams()}.</p>
 *
 * <p>The configuration covers four related areas:</p>
 * <ul>
 * 	 <li>which geometric points from a drawn gesture are selected for display to the screen</li>
 *   <li>and how those points correspond to scheduled events, controlled by {@link PathMode};</li>
 *   <li>how those events are timed, resampled, duration-scaled, warped, or converted to fixed hops;</li>
 *   <li>how each event is rendered, including grain length, envelope, gain, pitch, and burst settings.</li>
 * </ul>
 *
 * <p>See the GesturePlayground and Bagatelle examples for live interfaces that expose these
 * settings and show their effect on playback.</p>
 *
 * @see net.paulhertz.pixelaudio.schedule.GestureScheduleBuilder
 * @see GestureGranularParams
 */
public final class GestureGranularConfig {

  // ----- CORE ENUMS ----- //
  /**
   * Selects which points from a gesture path are used to create a playback schedule.
   */
  public enum PathMode {
    /** Use all recorded gesture points. */
    ALL_POINTS,
    /** Use a reduced point set, typically produced by Ramer-Douglas-Peucker simplification. */
    REDUCED_POINTS,
    /** Use points generated from a curve representation of the gesture. */
    CURVE_POINTS
  }

  /**
   * Selects whether event timing follows the gesture or a fixed sample hop.
   */
  public enum HopMode {
    /** Preserve event timing from the gesture schedule, after any configured timing transforms. */
    GESTURE,
    /** Replace event timing with evenly spaced hops of {@link #hopLengthSamples}. */
    FIXED
  }

  /**
   * Describes the primary timing transform selected in a user interface or preset.
   */
  public enum TimeTransform {
    /** Use the gesture's recorded timing, subject to value-driven overrides. */
    RAW_GESTURE,      // no resampling, duration change, or warp
    /** Resample the gesture to {@link #resampleCount} events. */
    RESAMPLED_COUNT,  
    /** Scale the gesture to {@link #targetDurationMs}. */
    DURATION_SCALED,  
    /** Apply a timing warp after duration scaling or resampling. */
    WARPED            // apply warp f(u)
  }

  /**
   * Selects the curve used to warp event timing along the gesture.
   */
  public enum WarpShape {
    /** No timing warp. */
    LINEAR,
    /** Exponential timing warp controlled by {@link #warpExponent}. */
    EXP,
    /** Square-root timing warp controlled by {@link #warpExponent}. */
    SQRT,
    /** Reserved for caller-defined timing warp behavior. */
    CUSTOM
  }

  // ----- IMMUTABLE FIELDS ----- //

  // Path selection
  /** Strategy for choosing the gesture points that become scheduled events. */
  public final PathMode pathMode;
  /** Simplification tolerance used by {@link PathMode#REDUCED_POINTS} and {@link PathMode#CURVE_POINTS}. */
  public final float rdpEpsilon; 
  /** Number of divisions in each curve segment when polygonizing a curve path derived from a gesture. */
  public final int curveSteps;
  /** Curve-shaping value for Bezier control point scaling, not used for audio synthesis. */
  public final float curveBias;   // reserved

  // Hop
  /** Timing mode for scheduled gesture events. */
  public final HopMode hopMode;
  /** Fixed hop length in samples; meaningful when {@link #hopMode} is {@link HopMode#FIXED}. */
  public final int hopLengthSamples;   // only meaningful for FIXED

  // Timing
  /** User-facing timing mode for resampling, duration scaling, or warping. */
  public final TimeTransform timingMode;
  /** Target event count for {@link TimeTransform#RESAMPLED_COUNT}; values below 2 mean no resampling. */
  public final int resampleCount;
  /** Target gesture duration in milliseconds for duration scaling and warping; 0 means no override. */
  public final int targetDurationMs;
  /** Timing warp shape used when warping is enabled. */
  public final WarpShape warpShape;
  /** Exponent used by exponential and square-root warp shapes. */
  public final float warpExponent;

  // Grain / synthesis
  /** Length of each granular grain in samples. */
  public final int grainLengthSamples;
  /** Envelope applied to granular or sampler playback. */
  public final ADSRParams env;         // consider copying if ADSRParams is mutable
  // TODO note duration, particularly for Sampler instrument
  /** Playback gain in decibels; converted to linear gain by {@link #gainLinear()}. */
  public final float gainDb;
  /** Pitch offset in semitones; converted to a playback ratio by {@link #pitchRatio()}. */
  public final float pitchSemitones;

  // Scheduled grain event modeling
  /** Number of grains created for each scheduled gesture event. */
  public final int burstGrains;
  /** True to compensate gain automatically when one event produces multiple burst grains. */
  public final boolean autoBurstGainComp;

  // Path timing calculation
  /** True to derive curve timing from arc length rather than point index.
   *  TODO provide relevant case use in sample code, in a future release */
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
  /**
   * Mutable builder for {@link GestureGranularConfig}.
   *
   * <p>The builder intentionally exposes public fields so interactive examples can bind GUI
   * controls directly to configuration state. Use {@link #build()} when you need an immutable
   * snapshot for scheduling or playback.</p>
   */
  public static final class Builder {
    // Path
    /** Strategy for choosing gesture points for the schedule. */
    public PathMode pathMode = PathMode.ALL_POINTS;
    /** Simplification tolerance used by reduced and curve path modes. */
    public float rdpEpsilon = 2.0f;
    /** Number of divisions used when constructing a curve-point schedule. */
    public int curveSteps = 8;
    /** Reserved curve-shaping value. */
    public float curveBias = 0f;

    // Hop
    /** Event timing mode for scheduled gesture events. */
    public HopMode hopMode = HopMode.GESTURE;
    /** Fixed hop length in samples when {@link #hopMode} is {@link HopMode#FIXED}. */
    public int hopLengthSamples = 512;

    // Timing (single source of truth)
    /** User-facing timing mode for GUI state and presets. */
    public TimeTransform timingMode = TimeTransform.RAW_GESTURE;
    // GUI baseline (not used by DSP directly)
    /** Original point count recorded from a schedule for GUI reference. */
    public int basePointCount = 0;
    /** Original duration in milliseconds recorded from a schedule for GUI reference. */
    public int baseDurationMs = 0;
    // GUI override targets ('0' means no override)
    /** Target event count for resampling; 0 means no override. */
    public int resampleCount = 0;
    /** Target gesture duration in milliseconds; 0 means no override. */
    public int targetDurationMs = 0;
    /** Timing warp shape. {@link WarpShape#LINEAR} disables warping. */
    public WarpShape warpShape = WarpShape.LINEAR;
    /** Exponent used by exponential and square-root warp shapes. */
    public float warpExponent = 2.0f;

    // Grain / synthesis
    /** Length of each granular grain in samples. */
    public int grainLengthSamples = 2048;
    /** Envelope applied to granular or sampler playback. */
    public ADSRParams env = new ADSRParams(1.0f, 0.02f, 0.06f, 0.9f, 0.10f);
    /** Playback gain in decibels. */
    public float gainDb = -6.0f;
    /** Pitch offset in semitones. */
    public float pitchSemitones = 0f;

    // Events
    /** Number of grains generated for each scheduled gesture event. */
    public int burstGrains = 1;
    /** True to compensate gain automatically when one event produces multiple burst grains. */
    public boolean autoBurstGainComp = false;

    // Timing from geometry
    /** True to derive curve timing from arc length rather than point index. */
    public boolean useArcLengthTime = false;
    
    
    /**
     * Sets the playback gain in decibels.
     *
     * @param gainDb    desired gain in dB
     * @return reference to this Builder
     */
    public Builder gainDb(float gainDb) {
        this.gainDb = gainDb;
        return this;
    }
 
    /**
     * Makes explicit to the caller that the envelope is being set for GRANULAR synthesis.
     *
     * @param env an ADSRParams instance to set the envelope
     * @return this builder
     */
    public Builder granularEnvelope(ADSRParams env) {
    	this.env = env;
    	return this;
    }

    /**
     * Makes explicit to the caller that the envelope is being set for SAMPLER synthesis.
     *
     * @param env an ADSRParams instance to set the envelope, nominally for sampler synthesis
     * @return this builder
     */
    public Builder samplerEnvelope(ADSRParams env) {
    	this.env = env;
    	return this;
    }

    /**
     * Builds an immutable snapshot safe to pass into scheduling and rendering code.
     *
     * <p>This method currently trusts the builder state. Call {@link #validate()} first when
     * you want early exceptions for impossible GUI or preset values.</p>
     *
     * @return an immutable configuration snapshot
     * @throws NullPointerException if any required enum or envelope field is null
     */
    public GestureGranularConfig build() {
      // If you want hard guarantees, uncomment validate().
      // validate();
      return new GestureGranularConfig(this);
    }
    
    /**
     * Stores GUI baseline values from a gesture schedule.
     *
     * <p>The baseline point count and duration are informational and are not used directly by
     * DSP rendering.</p>
     *
     * @param s schedule whose size and duration should be recorded
     * @throws NullPointerException if {@code s} is null
     */
    public void setBaselinesFromSchedule(GestureSchedule s) {
    	basePointCount = s.size();
    	baseDurationMs = Math.round(s.durationMs());
    }

    /**
     * Throws early if the builder contains values that cannot produce a valid schedule or render.
     *
     * @throws IllegalArgumentException if numeric values are outside their supported ranges
     */
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
    
    /**
     * Returns a new builder initialized from this builder.
     *
     * <p>The envelope reference is shared with the original builder.</p>
     *
     * @return a copy of this builder
     */
    public Builder copy() {
    	Builder b = new Builder();
    	b.pathMode = this.pathMode;
    	b.rdpEpsilon = this.rdpEpsilon;
    	b.curveSteps = this.curveSteps;
    	b.curveBias = this.curveBias;
    	b.hopMode = this.hopMode;
    	b.hopLengthSamples = this.hopLengthSamples;
        b.timingMode = this.timingMode;
        b.basePointCount = this.basePointCount;
        b.baseDurationMs = this.baseDurationMs;
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
    
    /**
     * Replaces this builder's fields with values from another builder.
     *
     * <p>The envelope reference is shared with the source builder.</p>
     *
     * @param src source builder to copy
     * @throws NullPointerException if {@code src} is null
     */
    public void copyFrom(GestureGranularConfig.Builder src) {
    	this.pathMode = src.pathMode;
    	this.rdpEpsilon = src.rdpEpsilon;
    	this.curveSteps = src.curveSteps;
    	this.curveBias = src.curveBias;

    	this.hopMode = src.hopMode;
    	this.hopLengthSamples = src.hopLengthSamples;

        this.timingMode = src.timingMode;
        this.basePointCount = src.basePointCount;
        this.baseDurationMs = src.baseDurationMs;
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
  
  /**
   * Creates the default immutable gesture granular configuration.
   *
   * @return a configuration snapshot built from a new default builder
   */
  public static GestureGranularConfig defaultConfig() {
      return new Builder().build();
  }
  
  // ----- UTILITY METHODS (from your class) ----- //

  /**
   * Converts {@link #gainDb} to a linear amplitude scalar.
   *
   * @return linear gain
   */
  public float gainLinear() {
    return (float) Math.pow(10.0, gainDb / 20.0);
  }

  /**
   * Converts {@link #pitchSemitones} to a playback speed or pitch ratio.
   *
   * @return pitch ratio where 1.0 is untransposed playback
   */
  public float pitchRatio() {
    return (float) Math.pow(2.0, pitchSemitones / 12.0);
  }

  /**
   * Reports whether this configuration is set to raw gesture timing.
   *
   * @return true when {@link #timingMode} is {@link TimeTransform#RAW_GESTURE}
   */
  public boolean isRawGestureTiming() {
    return timingMode == TimeTransform.RAW_GESTURE;
  }
  
  /**
   * Converts this user-facing configuration snapshot to runtime granular playback parameters.
   *
   * <p>Path-building settings such as {@link #pathMode}, {@link #rdpEpsilon},
   * {@link #curveSteps}, and {@link #useArcLengthTime} are used by schedule-building code and
   * are not represented in the returned {@link GestureGranularParams}.</p>
   *
   * @return immutable runtime parameters for granular playback
   */
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

  

  /**
   * Returns a multi-line summary of the current configuration snapshot.
   *
   * @return a human-readable description of this configuration
   */
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
