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

import ddf.minim.analysis.WindowFunction;
import net.paulhertz.pixelaudio.sampler.PitchPolicy;

/**
 * Windowed granular source that renders one scheduled burst of grains.
 *
 * <p>{@code PABurstGranularSource} is the sample-level renderer created by
 * {@link PAGranularInstrumentDirector}. Each instance represents one gesture event. Within that
 * event it may render one or more grains, spaced in time by {@code timeHopSamples} and advanced
 * through the source buffer by {@code indexHopSamples}.</p>
 *
 * <p>The source reads from a mono {@code float[]} buffer, applies an optional grain window,
 * performs linear interpolation for fractional pitch-ratio playback, and adds its samples to
 * the output block. When wrap-around is enabled, finite grains that pass the source-buffer end
 * continue reading from the beginning rather than becoming silent. When overlapping grains
 * contribute to the same block sample, the source performs in-place overlap-add normalization.</p>
 *
 * <p>This source reports {@link PitchPolicy#SOURCE_GRANULAR} because pitch is already handled
 * internally by {@code pitchRatio}; the instrument should not apply a second playback-rate
 * transposition on top of it.</p>
 *
 * @see PAGranularInstrumentDirector
 * @see PASource
 * @see WindowCache
 */
public final class PABurstGranularSource implements PASource {

    private final float[] source;
    private final int baseIndex;          // startIndices[i]
    private final int burstGrains;        // >= 1
    private final int timeHopSamples;     // >= 1 (intra-burst spacing in time)
    private final int indexHopSamples;    // >= 0 (intra-burst scan in source index)
    private final float pitchRatio;       // > 0
    private boolean wrapAround = false;

    // Optional per-source gain (multiplicative). Default unity.
    // If you already apply gain outside this PASource, leave at 1.
    // Not used, for now.
    private float gain = 1.0f;

    // window + grain length (authoritative)
    private int grainLength;
    private WindowFunction grainWindow = null;
    private float[] windowCurve = null;

    // note-level state (voice time origin)
    private long noteStartSample = Long.MIN_VALUE;
    private boolean noteStarted = false;

    // Scratch buffers (reused) for safe in-source OLA normalization
    private float[] wsum = new float[0];

    /**
     * Creates a burst granular source.
     *
     * @param source                mono source buffer to read from
     * @param baseIndex             start index in {@code source} for the first grain
     * @param grainLengthSamples    grain length in samples; values below 1 are clamped to 1
     * @param burstGrains           number of grains in this event; values below 1 are clamped to 1
     * @param timeHopSamples        spacing, in output samples, between grains in the burst
     * @param indexHopSamples       spacing, in source-buffer samples, between grain start positions
     * @param pitchRatio            playback pitch ratio for each grain; non-positive values use 1.0
     * @throws IllegalArgumentException if {@code source} is null
     */
    public PABurstGranularSource(
            float[] source,
            int baseIndex,
            int grainLengthSamples,
            int burstGrains,
            int timeHopSamples,
            int indexHopSamples,
            float pitchRatio
    ) {
        this(source, baseIndex, grainLengthSamples, burstGrains, timeHopSamples,
                indexHopSamples, pitchRatio, false);
    }

    /**
     * Creates a burst granular source.
     *
     * @param source                mono source buffer to read from
     * @param baseIndex             start index in {@code source} for the first grain
     * @param grainLengthSamples    grain length in samples; values below 1 are clamped to 1
     * @param burstGrains           number of grains in this event; values below 1 are clamped to 1
     * @param timeHopSamples        spacing, in output samples, between grains in the burst
     * @param indexHopSamples       spacing, in source-buffer samples, between grain start positions
     * @param pitchRatio            playback pitch ratio for each grain; non-positive values use 1.0
     * @param wrapAround            true to wrap finite source-buffer reads at the buffer end
     * @throws IllegalArgumentException if {@code source} is null
     */
    public PABurstGranularSource(
            float[] source,
            int baseIndex,
            int grainLengthSamples,
            int burstGrains,
            int timeHopSamples,
            int indexHopSamples,
            float pitchRatio,
            boolean wrapAround
    ) {
        if (source == null) throw new IllegalArgumentException("source must not be null");
        this.source = source;

        this.baseIndex = wrapAround ? wrapIndex(baseIndex, source.length) : Math.max(0, baseIndex);
        this.grainLength = Math.max(1, grainLengthSamples);

        this.burstGrains = Math.max(1, burstGrains);
        this.timeHopSamples = Math.max(1, timeHopSamples);
        this.indexHopSamples = Math.max(0, indexHopSamples);

        this.pitchRatio = (pitchRatio > 0f) ? pitchRatio : 1.0f;
        this.wrapAround = wrapAround;
    }

    /**
     * Sets an optional source-local gain scalar.
     *
     * <p>The current director applies event gain outside this source, and the current render
     * path does not apply this value. It is retained for callers that track source-local gain
     * metadata or for future source-level gain support.</p>
     *
     * @param gain gain scalar; finite negative values are clamped to 0
     */
    public synchronized void setGain(float gain) {
        if (Float.isFinite(gain)) this.gain = Math.max(0f, gain);
    }

    /**
     * Returns the optional source-local gain scalar.
     *
     * @return source-local gain
     */
    public synchronized float getGain() {
        return gain;
    }

    /**
     * Sets the absolute output-sample time at which this burst begins.
     *
     * @param absoluteSample     absolute sample time for the first grain in the burst
     */
    @Override
    public void seekTo(long absoluteSample) {
        this.noteStartSample = absoluteSample;
        this.noteStarted = true;
    }

    /**
     * Renders this burst into an audio block.
     *
     * <p>The method adds samples to {@code outL} and, when supplied, {@code outR}. It assumes
     * that output arrays are at least {@code blockSize} samples long. If this source has not
     * been started with {@link #seekTo(long)}, rendering is skipped.</p>
     *
     * @param blockStart    absolute sample index of the first sample in the output block
     * @param blockSize     number of samples in the output block
     * @param outL          left output buffer to mix into
     * @param outR          right output buffer to mix into; may be null or the same array as {@code outL}
     */
    @Override
    public void renderBlock(long blockStart, int blockSize, float[] outL, float[] outR) {
        if (!noteStarted) return;
        if (outL == null || blockSize <= 0) return;

        final long blockEnd = blockStart + (long) blockSize;

        final float[] window =
                (windowCurve != null && windowCurve.length == grainLength)
                        ? windowCurve
                        : (grainWindow != null && grainLength > 1)
                            ? WindowCache.INSTANCE.getWindowCurve(grainWindow, grainLength)
                            : null;

        final boolean doR = (outR != null && outR != outL);

        ensureWSum(blockSize);

        // Each grain g occupies [grainStartAbs, grainEndAbs)
        for (int g = 0; g < burstGrains; g++) {
            final long grainStartAbs = noteStartSample + (long) g * (long) timeHopSamples;
            final long grainEndAbs   = grainStartAbs + (long) grainLength;

            if (grainEndAbs <= blockStart || grainStartAbs >= blockEnd) continue;

            OverlapUtil.Slice slice = OverlapUtil.computeBlockSlice(
                    blockStart, blockSize, grainStartAbs, grainEndAbs);
            if (!slice.hasOverlap) continue;

            final int i0 = slice.startIndex;
            final int i1 = slice.endIndex;

            final int grainSourceStart = baseIndex + g * indexHopSamples;

            for (int i = i0; i < i1; i++) {
                final long globalSample = blockStart + (long) i;
                final int offsetInGrain = (int) (globalSample - grainStartAbs);
                if (offsetInGrain < 0 || offsetInGrain >= grainLength) continue;

                final float srcPos =
                        (float) grainSourceStart +
                        (float) offsetInGrain * pitchRatio;

                if (!wrapAround && (srcPos < 0f || srcPos >= (source.length - 1))) continue;

                final float w = (window != null) ? window[offsetInGrain] : 1.0f;
                final float s = readLinear(source, srcPos, wrapAround) * w;

                outL[i] += s;
                if (doR) outR[i] += s;

                wsum[i] += w;
            }
        }

        // OLA normalization (Overlap-Add normalization, in-place)
        final float eps = 1e-12f;
        for (int i = 0; i < blockSize; i++) {
            float den = wsum[i];
            if (den > eps) {
                float inv = 1.0f / den;
                outL[i] *= inv;
                if (doR) outR[i] *= inv;
            }
        }
    }
    
    /**
     * Returns the duration of the burst in samples.
     *
     * @return sample span from the start of the first grain to the end of the last grain
     */
    @Override
    public long lengthSamples() {
        return (long) (burstGrains - 1) * (long) timeHopSamples + (long) grainLength;
    }

    /**
     * Reports that this source handles pitch internally.
     *
     * @return {@link PitchPolicy#SOURCE_GRANULAR}
     */
    @Override
    public PitchPolicy pitchPolicy() {
        return PitchPolicy.SOURCE_GRANULAR;
    }

    /**
     * Sets the grain window and authoritative grain length for rendering.
     *
     * <p>When {@code wf} is null, the source uses a rectangular window. Otherwise the requested
     * curve is obtained from {@link WindowCache} and reused during block rendering.</p>
     *
     * @param wf window function to apply, or null for rectangular grains
     * @param grainLenSamples grain length in samples; values below 1 are clamped to 1
     */
    @Override
    public synchronized void setGrainWindow(WindowFunction wf, int grainLenSamples) {
        this.grainWindow = wf;
        this.grainLength = Math.max(1, grainLenSamples);
        if (wf != null && this.grainLength > 1) {
            this.windowCurve = WindowCache.INSTANCE.getWindowCurve(wf, this.grainLength);
        } else {
            this.windowCurve = null; // rectangular
        }
    }

    /**
     * Sets whether finite source-buffer reads wrap at the buffer end.
     *
     * @param wrapAround true to wrap finite grains across the source-buffer boundary
     */
    public synchronized void setWrapAround(boolean wrapAround) {
        this.wrapAround = wrapAround;
    }

    /** @return true when finite source-buffer reads wrap at the buffer end */
    public synchronized boolean isWrapAround() {
        return wrapAround;
    }

    private void ensureWSum(int blockSize) {
        if (wsum.length != blockSize) {
            wsum = new float[blockSize];
        }
        java.util.Arrays.fill(wsum, 0f);
    }
    
    /**
     * Reads a linearly interpolated value from an audio buffer.
     * 
     * @param buf audio buffer used to generate grains
     * @param pos fractional position in the buffer
     * @return interpolated sample value at {@code pos}
     */
    private static float readLinear(float[] buf, float pos, boolean wrapAround) {
        if (buf.length == 0) return 0f;
        if (buf.length == 1) return buf[0];

        int i0 = (int) pos;
        float frac = pos - i0;

        if (wrapAround) {
            int aIndex = wrapIndex(i0, buf.length);
            int bIndex = wrapIndex(i0 + 1, buf.length);
            float a = buf[aIndex];
            float b = buf[bIndex];
            return a + frac * (b - a);
        }

        if (i0 < 0) return buf[0];
        if (i0 >= buf.length - 1) return buf[buf.length - 1];

        float a = buf[i0];
        float b = buf[i0 + 1];
        return a + frac * (b - a);
    }

    private static int wrapIndex(int index, int length) {
        if (length <= 0) return 0;
        int wrapped = index % length;
        return (wrapped < 0) ? wrapped + length : wrapped;
    }
}
