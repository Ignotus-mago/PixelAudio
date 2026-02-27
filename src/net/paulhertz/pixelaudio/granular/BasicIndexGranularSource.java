package net.paulhertz.pixelaudio.granular;

import ddf.minim.analysis.WindowFunction;
import net.paulhertz.pixelaudio.sampler.PitchPolicy;

/**
 * BasicIndexGranularSource
 *
 * A simple granular source that:
 *  - reads from a mono float[] source buffer,
 *  - uses a list of grain start indices (sample positions),
 *  - plays grains in order with fixed hop and fixed grain length,
 *  - applies a window (Minim WindowFunction) per grain,
 *  - sums all active grains into outL/outR.
 *
 * Timing:
 *  - Grains are scheduled relative to a "note start" time.
 *  - At note-on, call seekTo(noteStartAbsoluteSample) to define when this
 *    grain train begins.
 *  - Grain k starts at: noteStart + k * hopSamples, lasts grainLengthSamples.
 *
 * This class ignores per-grain pan for now; it writes mono into both outL and outR.
 * 
 */
public class BasicIndexGranularSource implements PASource {

    // Underlying mono source buffer (no Minim dependency here).
    private final float[] source;

    // Grain start positions in source buffer (indices into 'source').
    private final int[] grainStarts;

    // Fixed grain length and hop in samples.
    private final int grainLengthSamples;
    private final int hopSamples;

    // Windowing
    private final WindowFunction windowFunction;
    private final WindowCache windowCache = WindowCache.INSTANCE;
    private final float[] windowCurve; // length == grainLengthSamples

    // Number of grains
    private final int numGrains;

    // Note timing
    private long noteStartSample = Long.MIN_VALUE; // absolute sample where grain train starts
    private boolean noteStarted = false;

    /**
     * @param source             mono audio buffer (float[])
     * @param grainStarts        list of sample indices into 'source' where each grain is anchored
     * @param grainLengthSamples fixed grain length (in samples)
     * @param hopSamples         fixed time between grain starts (in samples)
     * @param windowFunction     Minim window function (Hann, Hamming, Blackman, ...)
     */
    public BasicIndexGranularSource(float[] source,
                                    int[] grainStarts,
                                    int grainLengthSamples,
                                    int hopSamples,
                                    WindowFunction windowFunction) {
        if (source == null) throw new IllegalArgumentException("source buffer must not be null");
        if (grainStarts == null) throw new IllegalArgumentException("grainStarts must not be null");
        if (grainLengthSamples <= 0) throw new IllegalArgumentException("grainLengthSamples must be > 0");
        if (hopSamples <= 0) throw new IllegalArgumentException("hopSamples must be > 0");
        if (windowFunction == null) throw new IllegalArgumentException("windowFunction must not be null");

        this.source = source;
        this.grainStarts = grainStarts;
        this.grainLengthSamples = grainLengthSamples;
        this.hopSamples = hopSamples;
        this.windowFunction = windowFunction;
        this.numGrains = grainStarts.length;

        // Precompute / fetch window curve for this length.
        this.windowCurve = windowCache.getWindowCurve(windowFunction, grainLengthSamples);
    }

    /**
     * Call this at note-on to define when the grain train starts in absolute sample time.
     * Typically: scheduler or voice will call source.seekTo(nowAbsoluteSamples).
     */
    @Override
    public void seekTo(long absoluteSample) {
        this.noteStartSample = absoluteSample;
        this.noteStarted = true;
    }

    @Override
    public void renderBlock(long blockStart,
                            int blockSize,
                            float[] outL,
                            float[] outR) {

        if (!noteStarted || numGrains == 0) {
            return; // nothing to render yet
        }

        long blockEnd = blockStart + blockSize;

        // For simplicity, scan all grains. For modest grain counts this is fine.
        // Later, we can keep a "current grain" index to avoid scanning finished grains.
        for (int g = 0; g < numGrains; g++) {
            long grainStartAbs = noteStartSample + (long) g * hopSamples;
            long grainEndAbs   = grainStartAbs + grainLengthSamples;

            // Quick reject: if this grain is entirely before or after the block, skip.
            if (grainEndAbs <= blockStart || grainStartAbs >= blockEnd) {
                continue;
            }

            // Compute overlap of [grainStartAbs, grainEndAbs) with this block.
            OverlapUtil.Slice slice = OverlapUtil.computeBlockSlice(
                    blockStart, blockSize, grainStartAbs, grainEndAbs
            );
            if (!slice.hasOverlap) {
                continue;
            }

            int i0 = slice.startIndex;
            int i1 = slice.endIndex;

            // For samples i in [i0, i1), compute:
            //   globalSample = blockStart + i
            //   offsetWithinGrain = globalSample - grainStartAbs
            //   sourceIndex = grainStarts[g] + offsetWithinGrain
            for (int i = i0; i < i1; i++) {
                long globalSample = blockStart + i;
                int offsetInGrain = (int) (globalSample - grainStartAbs);
                if (offsetInGrain < 0 || offsetInGrain >= grainLengthSamples) {
                    continue;
                }

                int srcIndex = grainStarts[g] + offsetInGrain;
                if (srcIndex < 0 || srcIndex >= source.length) {
                    // Out of bounds: clamp or treat as silence. Here: silence.
                    continue;
                }

                float s = source[srcIndex] * windowCurve[offsetInGrain];

                // Mono: write same value to L and R (or only L if outR == null).
                outL[i] += s;
                if (outR != null && outR != outL) {
                    outR[i] += s;
                }
            }
        }
    }

    @Override
    public long lengthSamples() {
        if (numGrains == 0) return 0;
        // Total duration from noteStart to end of last grain (relative length):
        long relativeLen = (long) (numGrains - 1) * hopSamples + grainLengthSamples;
        return relativeLen;
    }

    @Override
    public PitchPolicy pitchPolicy() {
        // For now, timing/pitch is purely granular (hop, grainLength, index list).
        // Instrument pitch should not resample this again.
        return PitchPolicy.SOURCE_GRANULAR;
    }

    // Accessors if you need them:

    public float[] getSource() {
        return source;
    }

    public int[] getGrainStarts() {
        return grainStarts;
    }

    public int getGrainLengthSamples() {
        return grainLengthSamples;
    }

    public int getHopSamples() {
        return hopSamples;
    }

    public WindowFunction getWindowFunction() {
        return windowFunction;
    }
}
