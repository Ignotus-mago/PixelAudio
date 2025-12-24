package net.paulhertz.pixelaudio.granular;

import net.paulhertz.pixelaudio.voices.ADSRParams;



public final class GestureGranularConfig {
	// ----- CORE ENUMS ----- //	
	// We typically use PACurveMaker as a source for point lists, 
	// it acquires them from GUI user gestures.
	public enum PathMode {
	    ALL_POINTS,             // all points that compose the gesture
	    REDUCED_POINTS,         // RDP reduced points derived from all points, varies with epsilon
	    CURVE_POINTS            // Bezier path derived from reduced points, varies with epsilon and curveSteps
	}

	// How grain *positions* are derived
	public enum HopMode {
	    GESTURE,                // use gesture timing (dragTimes or parametric) for offsets
	    FIXED                   // use a fixed hop in samples for offsets
	}

	// How gesture timing is transformed
	public enum TimeTransform {
		RAW_GESTURE,            // no resampling, duration change, or warp
	    RESAMPLED_COUNT,        // resample to targetCount
	    DURATION_SCALED,        // scale to targetDurationMs
	    WARPED                  // apply warp f(u) (exp, sqrt, etc.)
	}
	
	// Warp curve selection
	public enum WarpShape {
	    LINEAR, 
	    EXP, 
	    SQRT, 
	    CUSTOM
	}
	
	// Path selection
	public PathMode pathMode = PathMode.ALL_POINTS;
	public float rdpEpsilon = 2.0f;   // used for REDUCED_POINTS
	public int curveSteps = 8;        // number of divisions in each segment in polygonized Bezier path
	public float curveBias;           // for future use
	// Hop / timing selection
	public HopMode hopMode = HopMode.GESTURE;
	// default settings: we are using data points direct from the curve
	// with no resampling, duration change, or warping
	public boolean useResampled = false;
	public boolean useNewDuration = false;
	public boolean useWarp = false;
	// parameters for resampling, duration, warp:
	// settings may require knowledge of how many points are in the curve
	// or how long a time is required to play the gesture as audio (duration)
	public TimeTransform timingMode;
	public int targetCount = 64;            // RESAMPLED_COUNT
	public int targetDurationMs = 2000;     // DURATION_SCALED / WARPED
	public WarpShape warpShape = WarpShape.LINEAR; // default if useWarp is false
	public float warpExponent = 2.0f;      // for EXP / SQRT variants
	// Grain / synthesis controls
	public int grainLengthSamples = 2048;     // length in samples of a grain
	public int hopLengthSamples   = 512;      // number of samples in a fixed-length hop 
	// Envelope: max amplitude, attack, decay, sustain, release in decimal seconds
	public ADSRParams env = new ADSRParams(1.0f, 0.02f, 0.06f, 0.9f, 0.10f); 
	// Gain & pitch
	public float gainDb = -6.0f;       // UI in dB
	public float pitchSemitones = 0f;  // up/down in semitones
	// scheduled grain event modeling
	public int burstGrains = 1;
	// scale gain as number of grains increases, default off
	public boolean autoBurstGainComp = false;
	// calculate curve points and times using arc length
	public boolean useArcLengthTime = false;
	
	// Convenience: compute linear gain from dB
	public float gainLinear() {
		return (float) Math.pow(10.0, gainDb / 20.0);
	}

	// conversion from semitones to pitch multiplier
	public float pitchRatio() {
		return (float) Math.pow(2.0, pitchSemitones / 12.0);
	}
	
	public boolean isRawGestureTiming() {
	    return !useResampled && !useNewDuration && !useWarp;
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
			sb.append(" (bezierCurveSteps=").append(curveSteps).append(")");
			break;
		default:
			// ALL_POINTS → no extra params
			break;
		}
		sb.append("\n");

		// Hop
		sb.append("  Hop: ").append(hopMode);
		if (hopMode == HopMode.FIXED) {
			sb.append(" (hopLengthSamples=").append(hopLengthSamples).append(")");
		}
		sb.append("\n");

		// Timing modifiers
		sb.append("  Timing:\n");
		sb.append("    useResampled: ").append(useResampled);
		if (useResampled) {
			sb.append(" (targetCount=").append(targetCount).append(")");
		}
		sb.append("\n");

		sb.append("    useNewDuration: ").append(useNewDuration);
		if (useNewDuration || useWarp) {
			sb.append(" (targetDurationMs=").append(targetDurationMs).append(")");
		}
		sb.append("\n");

		sb.append("    useWarp: ").append(useWarp);
		if (useWarp) {
			sb.append(" (warpShape=").append(warpShape)
			.append(", warpExponent=").append(warpExponent).append(")");
		}
		sb.append("\n");

		// Grain / synthesis
		sb.append("  Grain: length=").append(grainLengthSamples)
		.append(" samples, hop=").append(hopLengthSamples).append(" samples\n");

		// Envelope
		sb.append("  ADSR: ").append(env).append("\n");

		// Gain & pitch
		sb.append("  Gain: ").append(gainDb).append(" dB (linear=")
		.append(String.format("%.3f", gainLinear())).append(")\n");

		sb.append("  Pitch: ").append(pitchSemitones).append(" st (ratio=")
		.append(String.format("%.3f", pitchRatio())).append(")\n");

		sb.append("}");
		return sb.toString();
	}

}
