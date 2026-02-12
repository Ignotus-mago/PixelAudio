package net.paulhertz.pixelaudio.granular;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, schedule-aligned per-event parameters for Model-A gesture playback.
 *
 * <p>All arrays (when present) are parallel arrays with the same cardinality {@code n}
 * matching the {@link net.paulhertz.pixelaudio.schedule.GestureSchedule#size()} used to
 * generate them.</p>
 *
 * <p>Design intent:
 * <ul>
 *   <li>{@code startIndices} is the primary "mapping result" (typically from PixelAudioMapper).</li>
 *   <li>Optional arrays (pan/gain/pitchRatio) override {@link GestureGranularTexture} defaults per event.</li>
 *   <li>Persist and reuse this object across re-triggers; refresh only the fields that depend on buffer/mapping.</li>
 * </ul>
 * </p>
 *
 * <p>Immutability note: By default, this class defensively copies arrays in {@link Builder#build()}.
 * If you want to avoid copying for performance, call {@link Builder#noCopy()} and treat the provided
 * arrays as immutable thereafter.</p>
 */
public final class GestureEventParams {

    /** Number of events (must match schedule.size()). */
    public final int n;

    /** Required: per-event start indices into mono buffer; length == n. */
    public final int[] startIndices;

    /** Optional: per-event pan in [-1..1]; length == n, or null => use texture.pan. */
    public final float[] pan;

    /** Optional: per-event linear gain scalar; length == n, or null => use texture.gainLinear. */
    public final float[] gain;

    /** Optional: per-event pitch ratio (>0); length == n, or null => use texture.pitchRatio. */
    public final float[] pitchRatio;

    /**
     * Optional user-managed version tag. Handy for invalidation/caching in app code.
     * (e.g., mapperVersion, bufferRotationVersion, etc.)
     */
    public final int version;

    /**
     * Optional: a free-form identity marker for debugging/caching.
     * Example: System.identityHashCode(schedule), or a stable schedule id in your app.
     */
    public final int scheduleId;

    private GestureEventParams(Builder b) {
        this.n = b.n;

        this.startIndices = b.copyArrays ? Arrays.copyOf(b.startIndices, b.startIndices.length) : b.startIndices;

        this.pan = (b.pan != null)
                ? (b.copyArrays ? Arrays.copyOf(b.pan, b.pan.length) : b.pan)
                : null;

        this.gain = (b.gain != null)
                ? (b.copyArrays ? Arrays.copyOf(b.gain, b.gain.length) : b.gain)
                : null;

        this.pitchRatio = (b.pitchRatio != null)
                ? (b.copyArrays ? Arrays.copyOf(b.pitchRatio, b.pitchRatio.length) : b.pitchRatio)
                : null;

        this.version = b.version;
        this.scheduleId = b.scheduleId;
    }

    public static Builder builder(int n) {
        return new Builder(n);
    }

    /** Create a params object with required indices only. */
    public static GestureEventParams ofStartIndices(int[] startIndices) {
        Objects.requireNonNull(startIndices, "startIndices");
        return builder(startIndices.length).startIndices(startIndices).build();
    }

    // ------------------------------------------------------------------------
    // "With" helpers for incremental updates
    // ------------------------------------------------------------------------

    /** Return a copy with updated startIndices (other fields preserved). */
    public GestureEventParams withStartIndices(int[] newStartIndices) {
        Objects.requireNonNull(newStartIndices, "newStartIndices");
        return builder(this.n)
                .startIndices(newStartIndices)
                .pan(this.pan)
                .gain(this.gain)
                .pitchRatio(this.pitchRatio)
                .version(this.version)
                .scheduleId(this.scheduleId)
                .build();
    }

    /** Return a copy with updated pan array (other fields preserved). */
    public GestureEventParams withPan(float[] newPan) {
        return builder(this.n)
                .startIndices(this.startIndices)
                .pan(newPan)
                .gain(this.gain)
                .pitchRatio(this.pitchRatio)
                .version(this.version)
                .scheduleId(this.scheduleId)
                .build();
    }

    /** Return a copy with updated gain array (other fields preserved). */
    public GestureEventParams withGain(float[] newGain) {
        return builder(this.n)
                .startIndices(this.startIndices)
                .pan(this.pan)
                .gain(newGain)
                .pitchRatio(this.pitchRatio)
                .version(this.version)
                .scheduleId(this.scheduleId)
                .build();
    }

    /** Return a copy with updated pitchRatio array (other fields preserved). */
    public GestureEventParams withPitchRatio(float[] newPitchRatio) {
        return builder(this.n)
                .startIndices(this.startIndices)
                .pan(this.pan)
                .gain(this.gain)
                .pitchRatio(newPitchRatio)
                .version(this.version)
                .scheduleId(this.scheduleId)
                .build();
    }
    
    // ------------------------------------------------------------------------
    // Convenience accessors with defaults (optional)
    // ------------------------------------------------------------------------

    /** Returns per-event pan if present; otherwise returns defaultPan. */
    public float panAt(int i, float defaultPan) {
        if (pan == null) return defaultPan;
        return clampPan(pan[i]);
    }

    /** Returns per-event gain if present; otherwise returns defaultGain. */
    public float gainAt(int i, float defaultGain) {
        if (gain == null) return defaultGain;
        float g = gain[i];
        return (g < 0f) ? 0f : g;
    }

    /** Returns per-event pitch ratio if present; otherwise returns defaultPitch. */
    public float pitchAt(int i, float defaultPitch) {
        if (pitchRatio == null) return defaultPitch;
        float p = pitchRatio[i];
        return (p > 0f) ? p : defaultPitch;
    }
    

    @Override
    public String toString() {
        return "GestureEventParams{n=" + n
                + ", startIndices=" + (startIndices != null ? startIndices.length : 0)
                + ", pan=" + (pan != null ? pan.length : 0)
                + ", gain=" + (gain != null ? gain.length : 0)
                + ", pitchRatio=" + (pitchRatio != null ? pitchRatio.length : 0)
                + ", version=" + version
                + ", scheduleId=" + scheduleId
                + "}";
    }
    
    private static float clampPan(float p) {
        return (p < -1f) ? -1f : (p > 1f ? 1f : p);
    }

    // ------------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------------

    public static final class Builder {
        private final int n;

        private int[] startIndices;
        private float[] pan;
        private float[] gain;
        private float[] pitchRatio;

        private int version = 0;
        private int scheduleId = 0;

        private boolean copyArrays = true;

        private Builder(int n) {
            this.n = Math.max(0, n);
            if (this.n <= 0) {
                throw new IllegalArgumentException("n must be >= 1");
            }
        }

        /**
         * By default, build() defensively copies arrays. Call this to keep references as-is.
         * Only do this if you treat the arrays as immutable after building.
         */
        public Builder noCopy() {
            this.copyArrays = false;
            return this;
        }

        /** Required. Length must equal n. */
        public Builder startIndices(int[] v) {
            this.startIndices = Objects.requireNonNull(v, "startIndices");
            return this;
        }

        /** Optional. If non-null, length must equal n. */
        public Builder pan(float[] v) {
            this.pan = v;
            return this;
        }

        /** Optional. If non-null, length must equal n. */
        public Builder gain(float[] v) {
            this.gain = v;
            return this;
        }

        /** Optional. If non-null, length must equal n. */
        public Builder pitchRatio(float[] v) {
            this.pitchRatio = v;
            return this;
        }

        /** Optional caller-defined tag/version. */
        public Builder version(int v) {
            this.version = v;
            return this;
        }

        /** Optional caller-defined id for associated schedule. */
        public Builder scheduleId(int v) {
            this.scheduleId = v;
            return this;
        }

        public GestureEventParams build() {
            if (startIndices == null) {
                throw new IllegalStateException("startIndices is required");
            }
            requireLen("startIndices", startIndices.length, n);

            if (pan != null) requireLen("pan", pan.length, n);
            if (gain != null) requireLen("gain", gain.length, n);
            if (pitchRatio != null) requireLen("pitchRatio", pitchRatio.length, n);

            return new GestureEventParams(this);
        }

        private static void requireLen(String name, int got, int expect) {
            if (got != expect) {
                throw new IllegalArgumentException(name + ".length=" + got + " must equal n=" + expect);
            }
        }
    }
}


