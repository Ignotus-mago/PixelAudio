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

import ddf.minim.analysis.WindowFunction;
import net.paulhertz.pixelaudio.sampler.ADSRParams;


/**
 * Immutable runtime parameters for gesture-driven granular playback.
 *
 * <p>{@code GestureGranularParams} is the DSP-facing counterpart to
 * {@link GestureGranularConfig}. A configuration object describes the editable, user-facing
 * gesture settings; this class stores the values needed by the granular playback engine after
 * unit conversions such as decibels to linear gain and semitones to pitch ratio.</p>
 *
 * <p><b>Used by:</b></p>
 * <ul>
 *   <li>{@link PAGranularInstrumentDirector}, for gesture scheduling and granular source creation;</li>
 *   <li>{@link PASource} implementations, for burst and grain rendering behavior.</li>
 * </ul>
 *
 * <p><b>Not included:</b></p>
 * <ul>
 *   <li>gesture path-building settings such as reduction tolerance, curve steps, and arc-length timing;</li>
 *   <li>per-event overrides such as start indices, pan, gain, and pitch arrays, which belong to
 *   {@link GestureEventParams}.</li>
 * </ul>
 *
 * <p><b>Timing transform rules:</b> playback transforms are value-driven and may be combined:</p>
 * <ol>
 *   <li>resample when {@link #targetCount} is greater than 1;</li>
 *   <li>scale duration when {@link #targetDurationMs} is greater than 0;</li>
 *   <li>warp event times when {@link #warpShape} is not {@link WarpShape#LINEAR}.</li>
 * </ol>
 *
 * @see GestureGranularConfig#toParams()
 * @see GestureEventParams
 */
public final class GestureGranularParams {

    // --- enums (single shared home)
    /**
     * Selects whether event timing follows the transformed gesture schedule or fixed sample hops.
     */
    public enum HopMode {
        /** Use event offsets from the prepared gesture schedule. */
        GESTURE,
        /** Replace event offsets with evenly spaced hops of {@link #hopLengthSamples}. */
        FIXED
    }

    /**
     * UI-oriented timing label retained for compatibility with preset and control code.
     *
     * <p>Runtime playback uses {@link #timeTransform}, {@link #targetCount},
     * {@link #targetDurationMs}, and {@link #warpShape} for the actual transform decisions.</p>
     */
    public final TimeTransform timingMode;

    /**
     * Describes the primary timing transform selected by a configuration or user interface.
     */
    public enum TimeTransform {
        /** Use gesture timing unless value-driven target fields request additional transforms. */
        RAW_GESTURE,
        /** Resample the event sequence to {@link #targetCount}. */
        RESAMPLED_COUNT,
        /** Scale event times to {@link #targetDurationMs}. */
        DURATION_SCALED,
        /** Apply a timing warp with {@link #warpShape} and {@link #warpExponent}. */
        WARPED
    }

    /**
     * Shape used to remap normalized gesture time.
     */
    public enum WarpShape {
        /** No timing warp. */
        LINEAR,
        /** Exponential warp using {@link #warpExponent}. */
        EXP,
        /** Square-root-style warp using {@link #warpExponent}. */
        SQRT,
        /** Reserved for custom timing behavior supplied by higher-level code. */
        CUSTOM
    }

    // --- synthesis + scheduling semantics
    /** Length of each granular grain in samples. Values passed to the builder are clamped to at least 1. */
    public final int grainLengthSamples;
    /** Hop length in samples, used for fixed event timing and burst spacing. */
    public final int hopLengthSamples;     // meaning depends on hopMode; always used for FIXED and for burst spacing
    /** Number of grains produced for each scheduled event. Values passed to the builder are clamped to at least 1. */
    public final int burstGrains;
    /** True to apply automatic gain compensation when one event produces multiple burst grains. */
    public final boolean autoBurstGainComp;

    /** Linear gain scalar applied to each event before any per-event gain override. */
    public final float gainLinear;
    /** Default stereo pan in the range [-1, 1]; builder values are clamped when the object is built. */
    public final float pan;                // [-1..1]
    /** Default playback pitch ratio. Values are clamped to a small positive minimum when built. */
    public final float pitchRatio;         // >0
    /** Envelope applied to granular voices; null lets the instrument or sampler use its default envelope. */
    public final ADSRParams env;           // nullable => instrument default
    /** True to loop the granular source path when the lower-level voice supports looping. */
    public final boolean looping;          // for future uses
    /** True to wrap finite grain source-buffer reads at the buffer end. */
    public final boolean wrapAround;

    // --- timing transform
    /** Primary timing transform label supplied by the builder. */
    public final TimeTransform timeTransform;
    /** Target event count for resampling; values less than 2 disable resampling. */
    public final int targetCount;
    /** Target duration in milliseconds; values less than or equal to 0 disable duration scaling. */
    public final float targetDurationMs;
    /** Timing warp shape; {@link WarpShape#LINEAR} disables warping. */
    public final WarpShape warpShape;
    /** Exponent used by exponential and square-root timing warps. */
    public final float warpExponent;

    // --- grain window
    /** Grain amplitude window; null lets the director choose its default window, currently Hann. */
    public final WindowFunction grainWindow; // nullable => Director default (Hann)

    private GestureGranularParams(Builder b) {
        this.grainLengthSamples = b.grainLengthSamples;
        this.hopLengthSamples   = b.hopLengthSamples;
        this.burstGrains        = b.burstGrains;
        this.autoBurstGainComp  = b.autoBurstGainComp;

        this.gainLinear = b.gainLinear;
        this.pan        = clampPan(b.pan);
        this.pitchRatio = Math.max(1e-6f, b.pitchRatio);
        this.env        = b.env;
        this.looping    = b.looping;
        this.wrapAround = b.wrapAround;

        this.timingMode       = TimeTransform.RAW_GESTURE;
        this.timeTransform    = b.timeTransform;
        this.targetCount      = b.targetCount;
        this.targetDurationMs = b.targetDurationMs;
        this.warpShape        = b.warpShape;
        this.warpExponent     = Math.max(1e-6f, b.warpExponent);

        this.hopMode     = b.hopMode;
        this.grainWindow = b.grainWindow;
    }

    public final HopMode hopMode;

    /**
     * Creates a builder initialized with runtime defaults.
     *
     * @return a new builder
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Builder for immutable {@link GestureGranularParams} instances.
     *
     * <p>The builder performs lightweight clamping for values that must remain positive or
     * non-negative. It does not validate musical intent; for example, a very long grain or extreme
     * pitch ratio may be valid for a particular texture.</p>
     */
    public static final class Builder {
        // defaults chosen to match your Director defaults
        private int grainLengthSamples = 1024;
        private int hopLengthSamples   = 256;
        private int burstGrains        = 1;
        private boolean autoBurstGainComp = false;

        private float gainLinear = 1.0f;
        private float pan = 0.0f;
        private float pitchRatio = 1.0f;
        private ADSRParams env = null;
        private boolean looping = false;
        private boolean wrapAround = false;

        private HopMode hopMode = HopMode.GESTURE;

        private TimeTransform timeTransform = TimeTransform.RAW_GESTURE;
        private int targetCount = 0;
        private float targetDurationMs = 0f;
        private WarpShape warpShape = WarpShape.LINEAR;
        private float warpExponent = 1.0f;

        private WindowFunction grainWindow = null;

        /**
         * Sets the grain length in samples.
         *
         * @param v grain length; values below 1 are clamped to 1
         * @return this builder
         */
        public Builder grainLengthSamples(int v) { this.grainLengthSamples = Math.max(1, v); return this; }

        /**
         * Sets the hop length in samples.
         *
         * @param v hop length; values below 1 are clamped to 1
         * @return this builder
         */
        public Builder hopLengthSamples(int v)   { this.hopLengthSamples   = Math.max(1, v); return this; }

        /**
         * Sets the number of grains generated for each scheduled event.
         *
         * @param v burst grain count; values below 1 are clamped to 1
         * @return this builder
         */
        public Builder burstGrains(int v)        { this.burstGrains        = Math.max(1, v); return this; }

        /**
         * Enables or disables automatic gain compensation for burst grains.
         *
         * @param v true to compensate gain when an event creates multiple grains
         * @return this builder
         */
        public Builder autoBurstGainComp(boolean v) { this.autoBurstGainComp = v; return this; }

        /**
         * Sets the default linear gain.
         *
         * @param v linear gain; negative values are clamped to 0
         * @return this builder
         */
        public Builder gainLinear(float v) { this.gainLinear = Math.max(0f, v); return this; }

        /**
         * Sets the default pan.
         *
         * @param v pan value; clamped to [-1, 1] when the object is built
         * @return this builder
         */
        public Builder pan(float v)        { this.pan = v; return this; }

        /**
         * Sets the default pitch ratio.
         *
         * @param v pitch ratio; clamped to a small positive minimum when the object is built
         * @return this builder
         */
        public Builder pitchRatio(float v) { this.pitchRatio = v; return this; }

        /**
         * Sets the playback envelope.
         *
         * @param v envelope parameters, or null to use the instrument or sampler default
         * @return this builder
         */
        public Builder env(ADSRParams v)   { this.env = v; return this; }

        /**
         * Sets whether the granular source path should loop.
         *
         * @param v true to enable looping where supported
         * @return this builder
         */
        public Builder looping(boolean v)  { this.looping = v; return this; }

        /**
         * Enables or disables source-buffer wrapping for finite granular events.
         *
         * @param v true to wrap grain reads across the source-buffer boundary
         * @return this builder
         */
        public Builder wrapAround(boolean v) { this.wrapAround = v; return this; }

        /**
         * Sets the hop mode.
         *
         * @param v hop mode
         * @return this builder
         * @throws NullPointerException if {@code v} is null
         */
        public Builder hopMode(HopMode v)  { this.hopMode = Objects.requireNonNull(v); return this; }

        /**
         * Sets the primary timing transform label.
         *
         * @param v timing transform
         * @return this builder
         * @throws NullPointerException if {@code v} is null
         */
        public Builder timeTransform(TimeTransform v) { this.timeTransform = Objects.requireNonNull(v); return this; }

        /**
         * Sets the target event count for resampling.
         *
         * @param v target event count; negative values are clamped to 0
         * @return this builder
         */
        public Builder targetCount(int v)             { this.targetCount = Math.max(0, v); return this; }

        /**
         * Sets the target duration in milliseconds.
         *
         * @param v target duration; negative values are clamped to 0
         * @return this builder
         */
        public Builder targetDurationMs(float v)      { this.targetDurationMs = Math.max(0f, v); return this; }

        /**
         * Sets the timing warp shape.
         *
         * @param v warp shape
         * @return this builder
         * @throws NullPointerException if {@code v} is null
         */
        public Builder warpShape(WarpShape v)         { this.warpShape = Objects.requireNonNull(v); return this; }

        /**
         * Sets the timing warp exponent.
         *
         * @param v warp exponent; clamped to a small positive minimum when the object is built
         * @return this builder
         */
        public Builder warpExponent(float v)          { this.warpExponent = v; return this; }

        /**
         * Sets the grain amplitude window.
         *
         * @param wf window function, or null to use the director default
         * @return this builder
         */
        public Builder grainWindow(WindowFunction wf) { this.grainWindow = wf; return this; }

        /**
         * Builds an immutable runtime parameter set.
         *
         * @return a new immutable parameter set
         */
        public GestureGranularParams build() { return new GestureGranularParams(this); }
    }

    private static float clampPan(float p) {
        return (p < -1f) ? -1f : (p > 1f ? 1f : p);
    }
}
