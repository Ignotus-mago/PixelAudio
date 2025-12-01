package net.paulhertz.pixelaudio.granular;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GranularPath
 *
 * A fixed sequence of GrainSpecs that describe how to traverse a source buffer.
 * - Works for linear or non-linear paths.
 * - Suitable for both pre-rendered and streaming granular engines.
 *
 * This class is intentionally independent of Minim and any image classes.
 * It just carries numeric parameters (indices, gains, pans, etc.).
 */
public final class GranularPath {

    /**
     * Represents one "grain blueprint":
     * where to read in the source, how long, and optional per-grain parameters.
     */
    public static final class GrainSpec {
        /** Sample index in the source buffer where this grain is anchored (e.g., center or start). */
        public final long sourceSampleIndex;
        /** Grain length in samples; if <= 0, a default grain length may be used. */
        public final int grainLengthSamples;
        /**
         * Per-grain transposition hint.
         * Interpretation depends on the granular engine:
         *  - semitones relative to 1.0
         *  - or direct rate multiplier.
         */
        public final float pitchHint;
        /** Per-grain gain (linear scalar). */
        public final float gain;
        /**
         * Per-grain pan in [-1..+1], where:
         *  -1.0 = left, 0.0 = center, +1.0 = right.
         * Engines may ignore this if they don't support per-grain pan.
         */
        public final float pan;
        /**  */
        public final int timeOffsetMS;

        public GrainSpec(long sourceSampleIndex,
                         int grainLengthSamples,
                         float pitchHint,
                         float gain,
                         float pan,
                         int timeOffsetMS) {
            this.sourceSampleIndex = sourceSampleIndex;
            this.grainLengthSamples = grainLengthSamples;
            this.pitchHint = pitchHint;
            this.gain = gain;
            this.pan = pan;
            this.timeOffsetMS = timeOffsetMS;
        }
    }

    private final List<GrainSpec> grains;

    public GranularPath(List<GrainSpec> grains) {
        this.grains = Collections.unmodifiableList(new ArrayList<>(grains));
    }

    public List<GrainSpec> getGrains() {
        return grains;
    }

    public int size() {
        return grains.size();
    }

    public boolean isEmpty() {
        return grains.isEmpty();
    }
}
