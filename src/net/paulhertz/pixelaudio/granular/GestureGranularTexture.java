package net.paulhertz.pixelaudio.granular;

import java.util.Objects;

import ddf.minim.analysis.WindowFunction;
import ddf.minim.analysis.HannWindow;
import ddf.minim.analysis.HammingWindow;
import ddf.minim.analysis.BlackmanWindow;
import ddf.minim.analysis.RectangularWindow;

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
 *             .grainWindow(new HannWindow())
 *             .gainLinear(0.9f)
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

    /** Convenience window presets for typical grain shaping. */
    public enum WindowPreset {
        HANN,
        HAMMING,
        BLACKMAN,
        RECTANGULAR,
        CUSTOM
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

    /** Grain-level window function (may be null -> director/instrument supplies default). */
    public final WindowFunction grainWindow;

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

        this.grainWindow        = b.grainWindow;

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

        private WindowFunction grainWindow = null; // NEW (null means “use default”)

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

        /** Assign a custom grain window function (grain-level envelope). */
        public Builder grainWindow(WindowFunction wf) { this.grainWindow = wf; return this; }

        /**
         * Convenience helper to select a preset and set the corresponding WindowFunction.
         * If preset == CUSTOM, this method does nothing; assign grainWindow(...) manually.
         */
        public Builder selectWindowPreset(WindowPreset preset) {
            Objects.requireNonNull(preset, "preset");
            switch (preset) {
                case HAMMING:
                    grainWindow = new HammingWindow();
                    break;
                case BLACKMAN:
                    grainWindow = new BlackmanWindow();
                    break;
                case RECTANGULAR:
                    grainWindow = new RectangularWindow();
                    break;
                case CUSTOM:
                    // do nothing
                    break;
                case HANN:
                default:
                    grainWindow = new HannWindow();
                    break;
            }
            return this;
        }

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
    
    /*
    public static GestureGranularTexture fromConfig(GestureGranularConfig cfg) {
        if (cfg == null) throw new IllegalArgumentException("cfg must not be null");
        return GestureGranularTexture.builder()
                .grainLengthSamples(cfg.grainLengthSamples)
                .hopLengthSamples(cfg.hopLengthSamples)
                .burstGrains(cfg.burstGrains)
                .gainLinear(cfg.gainLinear())
                // .pan(cfg.pan)               // if present; otherwise omit
                .pitchRatio(cfg.pitchRatio())
                .env(cfg.env)
                // .looping(cfg.)
                .hopMode(cfg.hopMode)       // if present; else omit
                .build();
    }
    */

    
    
}
