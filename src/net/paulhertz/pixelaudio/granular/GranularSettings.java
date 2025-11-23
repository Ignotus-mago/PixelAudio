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
    
}
