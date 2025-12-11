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
 *  - can either:
 *      * play grains on a fixed hop grid (hopSamples), or
 *      * use per-grain timeOffsetMs to approximate the original gesture timing,
 *  - uses a Minim WindowFunction.
 */
public class PathGranularSource implements PASource {

	private final float[] source;
	private final GranularPath path;
	private final GranularSettings settings;
	private final float sampleRate;
	private final WindowCache windowCache = WindowCache.INSTANCE;

	private final int hopSamples;
	private final int defaultGrainLength;
	private final WindowFunction windowFunction;
	private final int numGrains;

	private long noteStartSample = Long.MIN_VALUE;
	private boolean noteStarted = false;

	public PathGranularSource(float[] source,
			GranularPath path,
			GranularSettings settings,
			float sampleRate) {
		if (source == null) throw new IllegalArgumentException("source buffer must not be null");
		if (path == null) throw new IllegalArgumentException("path must not be null");
		if (settings == null) throw new IllegalArgumentException("settings must not be null");

		this.source = source;
		this.path = path;
		this.settings = settings;
		this.sampleRate = sampleRate;

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

        if (!noteStarted || numGrains == 0) return;
        if (outL == null || blockSize <= 0) return;


		long blockEnd = blockStart + blockSize;

		for (int g = 0; g < numGrains; g++) {
			GranularPath.GrainSpec spec = path.getGrains().get(g);

			int grainLen = (spec.grainLengthSamples > 0)
					? spec.grainLengthSamples
					: defaultGrainLength;
			
			// Determine grain start in samples according to timing mode
			long grainStartAbs;
			switch(settings.getTimingMode()) {
			case FIXED_HOP: {
				long hop = settings.hopSamples;
				grainStartAbs = noteStartSample + (long) g * hop;
				break;
			}
			case GESTURE_TIMED:
			default: {
				long rel = spec.offsetSamples;    // in samples
				long scaled = (long) Math.round(rel * settings.getTimeScale());
				grainStartAbs = noteStartSample + scaled;
				break;
			}
				
			}			
			
			long grainEndAbs = grainStartAbs + grainLen;

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
			float gain = spec.gain * settings.gain;
			float pan = clampPan(spec.pan + settings.pan); // [-1..+1]

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
        if (settings.getTimingMode() == GranularSettings.TimingMode.GESTURE_TIMED) {
            // compute max(offsetSamples + grainLen)
        	long maxEnd = 0L;
            for (GranularPath.GrainSpec spec : path.getGrains()) {
                int grainLen = (spec.grainLengthSamples > 0)
                        ? spec.grainLengthSamples
                        : settings.defaultGrainLength;
                long rel = spec.offsetSamples;
                long scaled = (long) Math.round(rel * settings.getTimeScale());
                long end = scaled + grainLen;
                if (end > maxEnd) maxEnd = end;
            }
            return maxEnd;
        } 
        else {
            // Original fixed-hop behavior
            long relativeLen = (long) (numGrains - 1) * hopSamples + defaultGrainLength;
            return relativeLen;
        }
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
	
    private static float clampPan(float p) {
        if (p < -1f) return -1f;
        if (p > 1f) return 1f;
        return p;
    }

}
