package net.paulhertz.pixelaudio.granular;

import java.util.Objects;

import ddf.minim.analysis.WindowFunction;
import net.paulhertz.pixelaudio.sampler.ADSRParams;


/**
 * Immutable core parameters for gesture-driven granular playback.
 *
 * Used by:
 *  - PAGranularInstrumentDirector (scheduling + source construction)
 *  - PASource implementations (burst/grain rendering)
 *
 * Not included:
 *  - Path-building parameters (rdpEpsilon, curveSteps, useArcLengthTime, etc.)
 */
public final class GestureGranularParams {

    // --- enums (single shared home)
    public enum HopMode { GESTURE, FIXED }

    public final TimeTransform timingMode;
    /**
     * UI preset hint for timing.
     *
     * IMPORTANT:
     * Playback timing transforms are value-driven and may be combined:
     *   1) resample (targetCount > 1)
     *   2) duration scale (targetDurationMs > 0)
     *   3) warp (warpShape != LINEAR)
     *
     * The enum is used for UI state/presets, not as an exclusive gate.
     */
    public enum TimeTransform { RAW_GESTURE, RESAMPLED_COUNT, DURATION_SCALED, WARPED }

    public enum WarpShape { LINEAR, EXP, SQRT, CUSTOM }

    // --- synthesis + scheduling semantics
    public final int grainLengthSamples;
    public final int hopLengthSamples;     // meaning depends on hopMode; always used for FIXED and for burst spacing
    public final int burstGrains;
    public final boolean autoBurstGainComp;

    public final float gainLinear;
    public final float pan;                // [-1..1]
    public final float pitchRatio;         // >0
    public final ADSRParams env;           // nullable => instrument default
    public final boolean looping;          // for future parity / non-Model-A uses

    // --- timing transform
    public final TimeTransform timeTransform;
    public final int targetCount;
    public final float targetDurationMs;
    public final WarpShape warpShape;
    public final float warpExponent;

    // --- grain window
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

    public static Builder builder() { return new Builder(); }

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

        private HopMode hopMode = HopMode.GESTURE;

        private TimeTransform timeTransform = TimeTransform.RAW_GESTURE;
        private int targetCount = 0;
        private float targetDurationMs = 0f;
        private WarpShape warpShape = WarpShape.LINEAR;
        private float warpExponent = 1.0f;

        private WindowFunction grainWindow = null;

        public Builder grainLengthSamples(int v) { this.grainLengthSamples = Math.max(1, v); return this; }
        public Builder hopLengthSamples(int v)   { this.hopLengthSamples   = Math.max(1, v); return this; }
        public Builder burstGrains(int v)        { this.burstGrains        = Math.max(1, v); return this; }
        public Builder autoBurstGainComp(boolean v) { this.autoBurstGainComp = v; return this; }

        public Builder gainLinear(float v) { this.gainLinear = Math.max(0f, v); return this; }
        public Builder pan(float v)        { this.pan = v; return this; }
        public Builder pitchRatio(float v) { this.pitchRatio = v; return this; }
        public Builder env(ADSRParams v)   { this.env = v; return this; }
        public Builder looping(boolean v)  { this.looping = v; return this; }

        public Builder hopMode(HopMode v)  { this.hopMode = Objects.requireNonNull(v); return this; }

        public Builder timeTransform(TimeTransform v) { this.timeTransform = Objects.requireNonNull(v); return this; }
        public Builder targetCount(int v)             { this.targetCount = Math.max(0, v); return this; }
        public Builder targetDurationMs(float v)      { this.targetDurationMs = Math.max(0f, v); return this; }
        public Builder warpShape(WarpShape v)         { this.warpShape = Objects.requireNonNull(v); return this; }
        public Builder warpExponent(float v)          { this.warpExponent = v; return this; }

        public Builder grainWindow(WindowFunction wf) { this.grainWindow = wf; return this; }

        public GestureGranularParams build() { return new GestureGranularParams(this); }
    }

    private static float clampPan(float p) {
        return (p < -1f) ? -1f : (p > 1f ? 1f : p);
    }
}

