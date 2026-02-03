package net.paulhertz.pixelaudio.granular;

import java.util.Objects;

import net.paulhertz.pixelaudio.voices.ADSRParams;

/**
 * Friendly, user-facing subset of {@link GestureGranularConfig}.
 * Covers the common/essential knobs for most gesture playback.
 * Builder methods are chainable, e.g.:
 * 
 * <pre>
 *     GestureGranularTexture texture =
 *         GestureGranularTexture.builder()
 *             .grainLengthSamples(512)
 *             .hopLengthSamples(128)
 *             .maxAmp(0.8f)
 *             .attackSamples(64)
 *             .build();
 * </pre>
 * 
 */
public final class GestureGranularTexture {

    public enum TimeTransform {
        RAW_GESTURE,      // no resampling, duration change, or warp
        RESAMPLED_COUNT,  // resample to targetCount
        DURATION_SCALED,  // scale to targetDurationMs
        WARPED            // apply warp f(u) (typically after duration scale)
    }

    public enum WarpShape {
        LINEAR,
        EXP,
        SQRT,
        CUSTOM
    }

    public enum HopMode {
        GESTURE,  // use schedule times (gesture-driven)
        FIXED     // ignore schedule times; use fixed hop cadence
    }

    // --- essentials
    public final int grainLengthSamples;
    public final int hopLengthSamples;
    public final int burstGrains;       // >=1
    public final float gainLinear;      // final gain scalar
    public final float pan;             // [-1..1]
    public final float pitchRatio;      // 1.0 = no transposition
    public final ADSRParams env;        // may be null -> default handled by instrument
    public final boolean looping;       // keep for parity (often false in tutorial)
    public final HopMode hopMode;

    // --- timing transforms (optional)
    public final TimeTransform timeTransform;
    public final int targetCount;            // used if RESAMPLED_COUNT
    public final float targetDurationMs;     // used if DURATION_SCALED / WARPED
    public final WarpShape warpShape;        // used if WARPED
    public final float warpExponent;         // used if WARPED

    private GestureGranularTexture(Builder b) {
        this.grainLengthSamples = b.grainLengthSamples;
        this.hopLengthSamples   = b.hopLengthSamples;
        this.burstGrains        = b.burstGrains;
        this.gainLinear         = b.gainLinear;
        this.pan                = b.pan;
        this.pitchRatio         = b.pitchRatio;
        this.env                = b.env;
        this.looping            = b.looping;
        this.hopMode            = b.hopMode;

        this.timeTransform      = b.timeTransform;
        this.targetCount        = b.targetCount;
        this.targetDurationMs   = b.targetDurationMs;
        this.warpShape          = b.warpShape;
        this.warpExponent       = b.warpExponent;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int grainLengthSamples = 1024;
        private int hopLengthSamples   = 256;
        private int burstGrains        = 1;
        private float gainLinear       = 1.0f;
        private float pan              = 0.0f;
        private float pitchRatio       = 1.0f;
        private ADSRParams env         = null;
        private boolean looping        = false;
        private HopMode hopMode        = HopMode.GESTURE;

        private TimeTransform timeTransform = TimeTransform.RAW_GESTURE;
        private int targetCount            = 0;
        private float targetDurationMs     = 0f;
        private WarpShape warpShape        = WarpShape.LINEAR;
        private float warpExponent         = 1.0f;

        public Builder grainLengthSamples(int v) { this.grainLengthSamples = Math.max(1, v); return this; }
        public Builder hopLengthSamples(int v)   { this.hopLengthSamples   = Math.max(1, v); return this; }
        public Builder burstGrains(int v)        { this.burstGrains        = Math.max(1, v); return this; }
        public Builder gainLinear(float v)       { this.gainLinear         = Math.max(0f, v); return this; }
        public Builder pan(float v)              { this.pan                = clampPan(v); return this; }
        public Builder pitchRatio(float v)       { this.pitchRatio         = Math.max(1e-6f, v); return this; }
        public Builder env(ADSRParams v)         { this.env                = v; return this; }
        public Builder looping(boolean v)        { this.looping            = v; return this; }
        public Builder hopMode(HopMode v)        { this.hopMode            = Objects.requireNonNull(v); return this; }

        public Builder timeTransform(TimeTransform v) { this.timeTransform = Objects.requireNonNull(v); return this; }
        public Builder targetCount(int v)             { this.targetCount = Math.max(0, v); return this; }
        public Builder targetDurationMs(float v)      { this.targetDurationMs = Math.max(0f, v); return this; }
        public Builder warpShape(WarpShape v)         { this.warpShape = Objects.requireNonNull(v); return this; }
        public Builder warpExponent(float v)          { this.warpExponent = Math.max(1e-6f, v); return this; }

        public GestureGranularTexture build() {
            // Light validation; keep it permissive for tutorial use.
            if (timeTransform == TimeTransform.RESAMPLED_COUNT && targetCount <= 1) {
                // allow but caller probably meant RAW; keep as-is
            }
            return new GestureGranularTexture(this);
        }
    }

    private static float clampPan(float p) {
        return (p < -1f) ? -1f : (p > 1f ? 1f : p);
    }
}


