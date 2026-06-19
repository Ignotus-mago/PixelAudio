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

package net.paulhertz.pixelaudio.curves;

import java.util.Objects;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig;

/**
 * Abstract class for combining gesture data from PACurveMaker with audio synthesis parameters 
 * from GestureGranularConfig.Builder. 
 * <p> 
 * In the PixelAudio library examples, a "brush" uses gesture to control sound, and can draw the 
 * gesture to the display as an interactive UI element. In other words, brushes advance the core
 * design theme of PixelAudio: combining image and audio through structural correspondences. 
 * See the {@link net.paulhertz.pixelaudio.example.TutorialOne_05_GesturePlayground GesturePlayground} 
 * sketch in the example code for a GUI-driven tour of features. 
 * </p><p>
 * Two included subclasses, SamplerBrush and GranularBrush, model brushes for the PASamplerInstrument 
 * and PAGranularInstrument classes. Subclasses may add fields or behavior, but should preserve:
 * <ul>
 *   <li>a stable PACurveMaker reference</li>
 *   <li>a mutable or snapshot-based config for rendering both image and audio</li>
 * </ul>
 * Audio is typically generated either with the Sampler or Granular synthesis engine in PixelAudio.<br>
 * 
 * TODO links to relevant classes
 * 
 */
public abstract class AudioBrush {
	/** The PACurveMaker instance for curve data and rendering for this brush */
	private final PACurveMaker curve;
	/** The configuration builder for audio synthesis for this brush */
	private final GestureGranularConfig.Builder cfg;
	/** Optional transform state for applying geometric transformations to the curve data */
    private GestureTransformState transformState;


	/**
	 * Constructs an AudioBrush with the given curve and configuration builder.
	 * @param curve    a PACurveMaker instance
	 * @param cfg      a GestureGranularConfig
	 */
	protected AudioBrush(PACurveMaker curve, GestureGranularConfig.Builder cfg) {
		this.curve = Objects.requireNonNull(curve, "curve");
		this.cfg = Objects.requireNonNull(cfg, "cfg");
	}
	
	/**
	 * Returns the PACurveMaker instance associated with this brush. 
	 * @return the PACurveMaker instance for this brush
	 */
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
	
    /** 
	 * Returns the current transform state, or null if no transform is set. 
	 * @return the current GestureTransformState, or null if none is set
	 */
	public GestureTransformState transform() {
        return transformState;
    }

    /** 
	 * Sets the transform state for this brush. 
	 * @param state the GestureTransformState to set
	 */
    public void setTransform(GestureTransformState state) {
        this.transformState = state;
    }

    /** 
	 * Checks if a transform is currently set. 
	 * @return true if a transform is set, false otherwise
	 */
    public boolean hasTransform() {
        return transformState != null;
    }

    
	/**
	 * Ensures that a transform state is available for this brush.
	 * @return the GestureTransformState for this brush
	 */
	public GestureTransformState ensureTransform() {
        if (transformState == null) {
            transformState = curve.createTransformState();
        } else if (!transformState.hasRestPoints()) {
            transformState.captureRestPoints(curve.copyAllPoints());
        }
        return transformState;
    }

    
	/**
	 * Captures the untransformed rest points for the current transform state.
	 */
	public void captureRestPoints() {
        ensureTransform().captureRestPoints(curve.copyAllPoints());
    }

    /**
	 * Applies the current transform to the curve.
	 */
    public void applyTransform() {
        if (transformState != null) {
            curve.applyTransform(transformState);
        }
    }

    
	/**
	 * Restores the original transform for the curve.
	 */
    public void restoreTransform() {
        if (transformState != null) {
            curve.restoreTransform(transformState);
        }
    }


}
