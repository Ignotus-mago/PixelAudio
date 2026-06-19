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

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, schedule-aligned per-event parameters for gesture playback where each event
 * accepts a buffer sample index and optional pan, gain, and pitch modifiers.
 *
 * <p>All arrays (when present) are parallel arrays with the same cardinality {@code n}
 * matching the {@link net.paulhertz.pixelaudio.schedule.GestureSchedule#size()} used to
 * generate them.</p>
 *
 * <p>Design intent:</p>
 * <ul>
 *   <li>{@code startIndices} is the primary "mapping result" (typically from
 *   {@link net.paulhertz.pixelaudio.PixelAudioMapper PixelAudioMapper}).</li>
 *   <li>Optional arrays ({@code pan}, {@code gain}, and {@code pitchRatio}) override
 *   {@link GestureGranularParams} defaults per event.</li>
 *   <li>Persist and reuse this object across re-triggers; refresh only the fields that depend on buffer/mapping.</li>
 * </ul>
 *
 * <p>Immutability note: By default, this class defensively copies arrays in {@link Builder#build()}.
 * If you want to avoid copying for performance, call {@link Builder#noCopy()} and treat the provided
 * arrays as immutable thereafter.</p>
 */
public final class GestureEventParams {

    /** Number of events; must match the associated gesture schedule size. */
    public final int n;

    /** Required per-event start indices into a mono source buffer; length equals {@link #n}. */
    public final int[] startIndices;

    /** Optional per-event pan in [-1, 1]; length equals {@link #n}, or null to use {@link GestureGranularParams#pan}. */
    public final float[] pan;

    /** Optional per-event linear gain scalar; length equals {@link #n}, or null to use {@link GestureGranularParams#gainLinear}. */
    public final float[] gain;

    /** Optional per-event pitch ratio greater than 0; length equals {@link #n}, or null to use {@link GestureGranularParams#pitchRatio}. */
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

    /**
	 * Private constructor; use Builder to create instances.
	 * @param b    a GestureEventParams.Builder instance
	 */
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

    /**
     * Creates a builder for the given number of gesture events.
     *
     * @param n the number of schedule events this parameter set will describe
     * @return a new builder
     * @throws IllegalArgumentException if {@code n < 1}
     */
	public static Builder builder(int n) {
        return new Builder(n);
    }

    /**
     * Creates a parameter set with only the required start-index array.
     *
     * @param startIndices per-event start indices into the mono source buffer
     * @return a new parameter set whose {@link #n} equals {@code startIndices.length}
     * @throws NullPointerException if {@code startIndices} is null
     */
    public static GestureEventParams ofStartIndices(int[] startIndices) {
        Objects.requireNonNull(startIndices, "startIndices");
        return builder(startIndices.length).startIndices(startIndices).build();
    }

    // ------------------------------------------------------------------------
    // "With" helpers for incremental updates
    // ------------------------------------------------------------------------

    /**
     * Returns a copy with updated start indices and all other fields preserved.
     *
     * @param newStartIndices replacement per-event start indices; length must equal {@link #n}
     * @return a new parameter set with the supplied start indices
     * @throws NullPointerException if {@code newStartIndices} is null
     * @throws IllegalArgumentException if {@code newStartIndices.length != n}
     */
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

    /**
     * Returns a copy with an updated per-event pan array and all other fields preserved.
     *
     * <p>Pass null to clear the per-event override and use {@link GestureGranularParams#pan}.</p>
     *
     * @param newPan replacement per-event pan values; null is allowed, otherwise length must equal {@link #n}
     * @return a new parameter set with the supplied pan array
     * @throws IllegalArgumentException if {@code newPan} is non-null and {@code newPan.length != n}
     */
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

    /**
     * Returns a copy with an updated per-event gain array and all other fields preserved.
     *
     * <p>Pass null to clear the per-event override and use {@link GestureGranularParams#gainLinear}.</p>
     *
     * @param newGain replacement per-event gain values; null is allowed, otherwise length must equal {@link #n}
     * @return a new parameter set with the supplied gain array
     * @throws IllegalArgumentException if {@code newGain} is non-null and {@code newGain.length != n}
     */
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

    /**
     * Returns a copy with an updated per-event pitch-ratio array and all other fields preserved.
     *
     * <p>Pass null to clear the per-event override and use {@link GestureGranularParams#pitchRatio}.</p>
     *
     * @param newPitchRatio replacement per-event pitch ratios; null is allowed, otherwise length must equal {@link #n}
     * @return a new parameter set with the supplied pitch-ratio array
     * @throws IllegalArgumentException if {@code newPitchRatio} is non-null and {@code newPitchRatio.length != n}
     */
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

    /**
     * Returns the pan value for one event.
     *
     * <p>If no per-event pan array is present, this method returns {@code defaultPan}.
     * Otherwise it returns {@code pan[i]} clamped to the range [-1, 1].</p>
     *
     * @param i event index
     * @param defaultPan pan value to use when no per-event pan array is present
     * @return the resolved pan value
     * @throws ArrayIndexOutOfBoundsException if {@code i} is outside the pan array
     */
    public float panAt(int i, float defaultPan) {
        if (pan == null) return defaultPan;
        return clampPan(pan[i]);
    }

    /**
     * Returns the gain value for one event.
     *
     * <p>If no per-event gain array is present, this method returns {@code defaultGain}.
     * Otherwise it returns {@code gain[i]}, with negative values clamped to 0.</p>
     *
     * @param i event index
     * @param defaultGain gain value to use when no per-event gain array is present
     * @return the resolved gain value
     * @throws ArrayIndexOutOfBoundsException if {@code i} is outside the gain array
     */
    public float gainAt(int i, float defaultGain) {
        if (gain == null) return defaultGain;
        float g = gain[i];
        return (g < 0f) ? 0f : g;
    }

    /**
     * Returns the pitch ratio for one event.
     *
     * <p>If no per-event pitch-ratio array is present, this method returns {@code defaultPitch}.
     * Otherwise it returns {@code pitchRatio[i]}, falling back to {@code defaultPitch} for
     * non-positive values.</p>
     *
     * @param i event index
     * @param defaultPitch pitch ratio to use when no per-event pitch-ratio array is present
     * @return the resolved pitch ratio
     * @throws ArrayIndexOutOfBoundsException if {@code i} is outside the pitch-ratio array
     */
    public float pitchAt(int i, float defaultPitch) {
        if (pitchRatio == null) return defaultPitch;
        float p = pitchRatio[i];
        return (p > 0f) ? p : defaultPitch;
    }
    

    /**
	 * String representation of this object's settings and array lengths (not contents).
	 * @return a string representation of this object
	 */
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

    /**
     * A builder for creating instances of GestureEventParams.
     */
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
         * Disables defensive array copies when {@link #build()} creates the parameter set.
         *
         * <p>Call this only when the supplied arrays will be treated as immutable after building.</p>
         *
         * @return this builder
         */
        public Builder noCopy() {
            this.copyArrays = false;
            return this;
        }

        /**
         * Sets the required per-event start indices.
         *
         * @param v per-event start indices into the mono source buffer; length must equal the builder event count
         * @return this builder
         * @throws NullPointerException if {@code v} is null
         */
        public Builder startIndices(int[] v) {
            this.startIndices = Objects.requireNonNull(v, "startIndices");
            return this;
        }

        /**
         * Sets optional per-event pan values.
         *
         * @param v per-event pan values; null is allowed, otherwise length must equal the builder event count
         * @return this builder
         */
        public Builder pan(float[] v) {
            this.pan = v;
            return this;
        }

        /**
         * Sets optional per-event gain values.
         *
         * @param v per-event linear gain values; null is allowed, otherwise length must equal the builder event count
         * @return this builder
         */
        public Builder gain(float[] v) {
            this.gain = v;
            return this;
        }

        /**
         * Sets optional per-event pitch ratios.
         *
         * @param v per-event pitch ratios; null is allowed, otherwise length must equal the builder event count
         * @return this builder
         */
        public Builder pitchRatio(float[] v) {
            this.pitchRatio = v;
            return this;
        }

        /**
         * Sets a caller-defined version tag for cache invalidation or debugging.
         *
         * @param v caller-defined version value
         * @return this builder
         */
        public Builder version(int v) {
            this.version = v;
            return this;
        }

        /**
         * Sets a caller-defined identifier for the associated gesture schedule.
         *
         * @param v caller-defined schedule identifier
         * @return this builder
         */
        public Builder scheduleId(int v) {
            this.scheduleId = v;
            return this;
        }

        /**
         * Validates the configured arrays and creates an immutable parameter set.
         *
         * @return a new gesture event parameter set
         * @throws IllegalStateException if required start indices have not been supplied
         * @throws IllegalArgumentException if any supplied array length differs from the builder event count
         */
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
