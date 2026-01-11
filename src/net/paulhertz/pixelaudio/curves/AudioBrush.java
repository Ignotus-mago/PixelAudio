package net.paulhertz.pixelaudio.curves;

import java.util.Objects;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig;

/**
 * Abstract class for combining gesture data from PACurveMaker with settings for 
 * modifying gesture data and setting audio generation parameters with GestureGranularConfig.Builder. 
 * In the PixelAudio library examples, a "brush" uses gesture to control sound, and can draw the 
 * gesture to the display as an interactive UI element. In other words, brushes advance the core
 * design theme of PixelAudio: combining image and audio through structural correspondences. 
 * See the GesturePlayground applet in the example code for a GUI-driven tour of features. 
 * 
 * Two included subclasses, SamplerBrush and GranularBrush, model brushes for the PASamplerInstrument 
 * and PAGranularInstrument classes. Subclasses may add fields or behavior, but should preserve:
 *   - a stable PACurveMaker reference
 *   - a mutable or snapshot-based config for rendering both image and audio
 * 
 */
public abstract class AudioBrush {
	private final PACurveMaker curve;
	private final GestureGranularConfig.Builder cfg;

	protected AudioBrush(PACurveMaker curve, GestureGranularConfig.Builder cfg) {
		this.curve = Objects.requireNonNull(curve, "curve");
		this.cfg = Objects.requireNonNull(cfg, "cfg");
	}
	
	public PACurveMaker curve() { return curve; }
	
	/**
	 * Returns the configuration builder associated with this brush.
	 * Mutating the builder affects subsequent rendering unless a snapshot
	 * is taken via {@code cfg().build()} or {@link #snapshot()}.
	 */
	public GestureGranularConfig.Builder cfg() { return cfg; }
	
	/** 
	 * Returns an immutable snapshot of the current configuration state. 
	 */
	public GestureGranularConfig snapshot() {
	    return cfg.build();
	}

}
