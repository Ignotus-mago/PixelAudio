package net.paulhertz.pixelaudio.granular;

import ddf.minim.analysis.WindowFunction;
import net.paulhertz.pixelaudio.voices.PitchPolicy;

public final class PABurstGranularSource implements PASource {

	private final float[] source;
	private final int baseIndex;          // startIndices[i]
	private final int burstGrains;        // >= 1
	private final int timeHopSamples;     // >= 1 (intra-burst spacing in time)
	private final int indexHopSamples;    // >= 0 (intra-burst scan in source index)
	private final float pitchRatio;       // > 0

	// window + grain length (authoritative)
	private int grainLength;
	private WindowFunction grainWindow = null;
	private float[] windowCurve = null;

	// note-level state (voice time origin)
	private long noteStartSample = Long.MIN_VALUE;
	private boolean noteStarted = false;

	public PABurstGranularSource(
			float[] source,
			int baseIndex,
			int grainLengthSamples,
			int burstGrains,
			int timeHopSamples,
			int indexHopSamples,
			float pitchRatio
			) {
		if (source == null) throw new IllegalArgumentException("source must not be null");
		this.source = source;

		this.baseIndex = Math.max(0, baseIndex);
		this.grainLength = Math.max(1, grainLengthSamples);

		this.burstGrains = Math.max(1, burstGrains);
		this.timeHopSamples = Math.max(1, timeHopSamples);
		this.indexHopSamples = Math.max(0, indexHopSamples);

		this.pitchRatio = (pitchRatio > 0f) ? pitchRatio : 1.0f;
	}

	@Override
	public void seekTo(long absoluteSample) {
		this.noteStartSample = absoluteSample;
		this.noteStarted = true;
	}

	@Override
	public void renderBlock(long blockStart, int blockSize, float[] outL, float[] outR) {
		if (!noteStarted) return;
		if (outL == null || blockSize <= 0) return;

		final long blockEnd = blockStart + (long) blockSize;

		// Resolve window curve once per block (rectangular fallback)
		final float[] window = (windowCurve != null && windowCurve.length == grainLength)
				? windowCurve : (grainWindow != null && grainLength > 1)
						? WindowCache.INSTANCE.getWindowCurve(grainWindow, grainLength) : null;

		final boolean doR = (outR != null && outR != outL);    // 

		// Each grain g occupies [grainStartAbs, grainEndAbs) in voice time
		for (int g = 0; g < burstGrains; g++) {
			final long grainStartAbs = noteStartSample + (long) g * (long) timeHopSamples;
			final long grainEndAbs   = grainStartAbs + (long) grainLength;

			if (grainEndAbs <= blockStart || grainStartAbs >= blockEnd) continue;    // process in next block

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

				final float srcPos = (float) grainSourceStart + (float) offsetInGrain * pitchRatio;

				// Edge policy (current): drop out-of-range samples
				// TODO a wrap around edge policy
				if (srcPos < 0f || srcPos >= (source.length - 1)) continue;

				final float w = (window != null) ? window[offsetInGrain] : 1.0f;
				final float s = readLinear(source, srcPos) * w;

				outL[i] += s;
				if (doR) outR[i] += s;
			}
		}
	}

	@Override
	public long lengthSamples() {
		return (long) (burstGrains - 1) * (long) timeHopSamples + (long) grainLength;
	}

	@Override
	public PitchPolicy pitchPolicy() {
		return PitchPolicy.SOURCE_GRANULAR;
	}

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

	private static float readLinear(float[] buf, float pos) {
		int i0 = (int) pos;
		float frac = pos - i0;

		if (i0 < 0) return buf[0];
		if (i0 >= buf.length - 1) return buf[buf.length - 1];

		float a = buf[i0];
		float b = buf[i0 + 1];
		return a + frac * (b - a);
	}
}
