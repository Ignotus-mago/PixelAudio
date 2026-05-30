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
 * Abstract class for combining gesture data from PACurveMaker with settings for 
 * modifying gesture data and setting audio generation parameters with GestureGranularConfig.Builder.
 * <p> 
 * In the PixelAudio library examples, a "brush" uses gesture to control sound, and can draw the 
 * gesture to the display as an interactive UI element. In other words, brushes advance the core
 * design theme of PixelAudio: combining image and audio through structural correspondences. 
 * See the GesturePlayground applet in the example code for a GUI-driven tour of features. 
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
	private final PACurveMaker curve;
	private final GestureGranularConfig.Builder cfg;
    private GestureTransformState transformState;


	/**
	 * @param curve    a PACurveMaker instance
	 * @param cfg      a GestureGranularConfig
	 */
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
	
    public GestureTransformState transform() {
        return transformState;
    }

    public void setTransform(GestureTransformState state) {
        this.transformState = state;
    }

    public boolean hasTransform() {
        return transformState != null;
    }

    public GestureTransformState ensureTransform() {
        if (transformState == null) {
            transformState = curve.createTransformState();
        } else if (!transformState.hasRestPoints()) {
            transformState.captureRestPoints(curve.copyAllPoints());
        }
        return transformState;
    }

    public void captureRestPoints() {
        ensureTransform().captureRestPoints(curve.copyAllPoints());
    }

    public void applyTransform() {
        if (transformState != null) {
            curve.applyTransform(transformState);
        }
    }

    public void restoreTransform() {
        if (transformState != null) {
            curve.restoreTransform(transformState);
        }
    }


}
