package net.paulhertz.pixelaudio.granular;

import ddf.minim.analysis.WindowFunction;
import net.paulhertz.pixelaudio.voices.PASource;
import net.paulhertz.pixelaudio.voices.PitchPolicy;

/**
 * PathGranularSource
 *
 * Granular source that:
 *  - reads from a mono float[] source buffer,
 *  - uses a GranularPath for where/how to place grains,
 *  - applies per-grain gain and pan,
 *  - plays grains in order with fixed hop (density from GranularSettings.hopSamples),
 *  - uses a Minim WindowFunction.
 *
 * Timing:
 *   Grain g starts at absolute sample:
 *       noteStartSample + g * hopSamples
 */
public class PathGranularSource implements PASource {

	private final float[] source;
	private final GranularPath path;
	private final GranularSettings settings;
	private final WindowCache windowCache = WindowCache.INSTANCE;

	private final int hopSamples;
	private final int defaultGrainLength;
	private final WindowFunction windowFunction;
	private final int numGrains;

	private long noteStartSample = Long.MIN_VALUE;
	private boolean noteStarted = false;

	public PathGranularSource(float[] source,
			GranularPath path,
			GranularSettings settings) {
		if (source == null) throw new IllegalArgumentException("source buffer must not be null");
		if (path == null) throw new IllegalArgumentException("path must not be null");
		if (settings == null) throw new IllegalArgumentException("settings must not be null");

		this.source = source;
		this.path = path;
		this.settings = settings;

		this.hopSamples = settings.hopSamples;
		this.defaultGrainLength = settings.defaultGrainLength;
		this.windowFunction = settings.windowFunction;

		this.numGrains = path.size();
	}

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
			return;
		}

		long blockEnd = blockStart + blockSize;

		for (int g = 0; g < numGrains; g++) {
			GranularPath.GrainSpec spec = path.getGrains().get(g);

			int grainLen = (spec.grainLengthSamples > 0)
					? spec.grainLengthSamples
					: defaultGrainLength;

			long grainStartAbs = noteStartSample + (long) g * hopSamples;
			long grainEndAbs   = grainStartAbs + grainLen;

			// Skip if no overlap
			if (grainEndAbs <= blockStart || grainStartAbs >= blockEnd) {
				continue;
			}

			OverlapUtil.Slice slice = OverlapUtil.computeBlockSlice(
					blockStart, blockSize, grainStartAbs, grainEndAbs);
			if (!slice.hasOverlap) {
				continue;
			}

			int i0 = slice.startIndex;
			int i1 = slice.endIndex;

			// Window curve for this grain length
			float[] window = windowCache.getWindowCurve(windowFunction, grainLen);

			// Per-grain gain and pan
			float gain = spec.gain;
			float pan = spec.pan; // [-1..+1]

			// Equal-power pan
			float panAngle = (pan + 1.0f) * 0.25f * (float) Math.PI; // -1..+1 → 0..π/2
			float panL = (float) Math.cos(panAngle);
			float panR = (float) Math.sin(panAngle);

			long sourceStart = spec.sourceSampleIndex;

			for (int i = i0; i < i1; i++) {
				long globalSample = blockStart + i;
				int offsetInGrain = (int) (globalSample - grainStartAbs);
				if (offsetInGrain < 0 || offsetInGrain >= grainLen) {
					continue;
				}

				int srcIndex = (int) (sourceStart + offsetInGrain);
				if (srcIndex < 0 || srcIndex >= source.length) {
					continue;
				}

				float w = window[offsetInGrain];
				float s = source[srcIndex] * w * gain;

				float sL = s * panL;
				float sR = s * panR;

				outL[i] += sL;
				if (outR != null && outR != outL) {
					outR[i] += sR;
				}
			}
		}
	}

	@Override
	public long lengthSamples() {
		if (numGrains == 0) return 0;
		long relativeLen = (long) (numGrains - 1) * hopSamples + defaultGrainLength;
		return relativeLen;
	}

	@Override
	public PitchPolicy pitchPolicy() {
		// Time/pitch are fully determined by the granular engine; instrument should not re-pitch.
		return PitchPolicy.SOURCE_GRANULAR;
	}

	public float[] getSource() {
		return source;
	}

	public GranularPath getPath() {
		return path;
	}

	public GranularSettings getSettings() {
		return settings;
	}
}
