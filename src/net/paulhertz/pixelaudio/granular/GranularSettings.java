package net.paulhertz.pixelaudio.granular;

import ddf.minim.analysis.BlackmanWindow;
import ddf.minim.analysis.HammingWindow;
import ddf.minim.analysis.HannWindow;
import ddf.minim.analysis.RectangularWindow;
import ddf.minim.analysis.WindowFunction;

/**
 * GranularSettings
 *
 * Common configuration for granular processing, whether pre-rendered
 * or streaming. Uses Minim's WindowFunction for window generation.
 * TODO further optimization for the next library version could include parallel processing.
 * TODO optimization when operator frequencies are identical could have a small but measurable effect.
 */
public final class GranularSettings {
	// -------- Core granular parameters --------

	/** Default grain length in samples (used if grainLengthSamples <= 0). */
	public int defaultGrainLength = 2048;
	/** Default hop size in samples: determines overlap factor. */
	public int hopSamples = 256;
	/** Stream-level transposition in semitones (for SOURCE_GRANULAR mode). */
	public float transposeSemitones = 0.0f;
	/** Random jitter applied to grain start indices (interpretation TBD). */
	public float grainJitter = 0.0f;
	/** Normalize by window sum where appropriate to avoid overlap gain bumps. */
	public boolean normalizeByWindowSum = true;
	/** Whether to use per-grain pan from the path (when available). */
	public boolean respectPerGrainPan = true;
	/** Whether per-grain pitch (in GranularPath) overrides stream-level transpose. */
	public boolean perGrainPitchOverrides = false;
	
	/** default gain setting */
	public float gain = 1.0f;
	/** default pan setting */
	public float pan = 0.0f;

	/** How to handle grains near buffer edges: wrap, clamp, reflect, etc. */
	public enum EdgeMode {
		CLAMP,
		WRAP,
		REFLECT
	}

	public EdgeMode edgeMode = EdgeMode.CLAMP;

	/**
	 * Optional preset enum for convenience when choosing a WindowFunction.
	 * CUSTOM means you are directly assigning a WindowFunction instance.
	 */
	public enum WindowPreset {
		HANN,
		HAMMING,
		BLACKMAN,
		RECTANGULAR,
		CUSTOM
	}

	/** Current preset (used only by selectWindowPreset as a record). */
	public WindowPreset windowPreset = WindowPreset.HANN;

	/**
	 * Window function used for grains.
	 * Defaults to HannWindow, can be replaced at runtime.
	 *
	 * NOTE:
	 *  - When grain lengths vary, windowFunction.generateCurve(length)
	 *    should be cached (see WindowCache) to avoid recomputation.
	 */
	public WindowFunction windowFunction = new HannWindow();
	
	public enum TimingMode {
	    FIXED_HOP,
	    GESTURE_TIMED
	}

	private TimingMode timingMode = TimingMode.FIXED_HOP;
	private float timeScale = 1.0f; // stretch/compress gesture duration


	public GranularSettings() {
		// default windowFunction already set to HannWindow()
	}

	public GranularSettings(WindowFunction windowFunction,
			int defaultGrainLength,
			int hopSamples) {
		this.windowFunction   = windowFunction;
		this.defaultGrainLength = defaultGrainLength;
		this.hopSamples       = hopSamples;
	}

	/**
	 * Convenience helper to select a preset and set the corresponding WindowFunction.
	 * If preset == CUSTOM, this method does nothing; assign windowFunction manually.
	 */
    public void selectWindowPreset(WindowPreset preset) {
        switch (preset) {
            case HAMMING:
                windowFunction = new HammingWindow();
                break;
            case BLACKMAN:
                windowFunction = new BlackmanWindow();
                break;
            case RECTANGULAR:
                windowFunction = new RectangularWindow();
                break;
            case HANN:
            default:
                windowFunction = new HannWindow();
                break;
        }
 	}
    
    // --------------------------------------------------------------------
    // Convenience ms-based setters
    // --------------------------------------------------------------------

    /** Set hopSamples from milliseconds + sampleRate. */
    public void setHopMs(float hopMs, float sampleRate) {
        if (sampleRate <= 0f) return;
        this.hopSamples = Math.max(1, Math.round(hopMs * 0.001f * sampleRate));
    }

    /** For callers that think in ms but we store grain length in samples. */
    public void setDefaultGrainLengthMs(float grainMs, float sampleRate) {
        if (sampleRate <= 0f) return;
        this.defaultGrainLength = Math.max(1, Math.round(grainMs * 0.001f * sampleRate));
    }
        
    // --------------------------------------------------------------------
    // Getters and Setters
    // --------------------------------------------------------------------

    public TimingMode getTimingMode() { return timingMode; }
    public void setTimingMode(TimingMode mode) {
        if (mode != null) this.timingMode = mode;
    }

    public float getTimeScale() { return timeScale; }
    public void setTimeScale(float timeScale) {
        this.timeScale = (timeScale > 0f) ? timeScale : 1.0f;
    }
    
    public float getGain() { return gain; }
	public void setGain(float gain) { this.gain = gain; }

	public float getPan() { return pan; }
	public void setPan(float pan) { this.pan = pan; }

	
	public GranularSettings clone() {
    	GranularSettings settings = new GranularSettings(this.windowFunction, this.defaultGrainLength, this.hopSamples);
    	settings.transposeSemitones = this.transposeSemitones;
    	settings.grainJitter = this.grainJitter;
    	settings.normalizeByWindowSum = this.normalizeByWindowSum;
    	settings.respectPerGrainPan = this.respectPerGrainPan;
    	settings.perGrainPitchOverrides = this.perGrainPitchOverrides;
    	settings.timingMode = this.timingMode;
    	settings.timeScale = this.timeScale;
    	settings.gain = this.gain;
    	settings.pan = this.pan;
    	return settings;
    }
    
}
