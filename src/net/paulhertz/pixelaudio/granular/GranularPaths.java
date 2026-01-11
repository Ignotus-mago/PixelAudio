package net.paulhertz.pixelaudio.granular;

import java.util.ArrayList;
import java.util.List;

public final class GranularPaths {

	public enum EdgeMode { WRAP, CLAMP, BOUNCE }

	private GranularPaths() {}

	public static GranularPath fixedHopReport(
			long startIndex,
			int numGrains,
			int hopSamples,
			int grainLengthSamples,
			float pitchHint,
			float gain,
			float pan,
			long startOffsetSamples,
			long hopOffsetSamples,
			int sourceLengthSamples,
			EdgeMode edgeMode) {
		// validate
		if (numGrains <= 0) return new GranularPath(List.of());
		if (hopSamples == 0) hopSamples = 1;
		if (sourceLengthSamples <= 0) throw new IllegalArgumentException("sourceLengthSamples must be > 0");
		// get ready for grains
		List<GranularPath.GrainSpec> grains = new ArrayList<>(numGrains);
		long idx = startIndex;
		long off = startOffsetSamples;
		// For BOUNCE, direction flips at edges
		long dir = 1;
		// fill the list
		for (int i = 0; i < numGrains; i++) {
			long pos = applyEdgeMode(idx, sourceLengthSamples, edgeMode, dir);
			// If BOUNCE and we hit an edge, applyEdgeMode will “reflect” but we need to
			// detect the reflection to flip direction. Simple approach: check next step.
			if (edgeMode == EdgeMode.BOUNCE) {
				long next = idx + dir * hopSamples;
				if (next < 0 || next >= sourceLengthSamples) dir *= -1;
			}
			// another key class, GranularPath
			grains.add(new GranularPath.GrainSpec( pos, grainLengthSamples, pitchHint, gain, pan, off ));
			idx += dir * hopSamples;
			off += hopOffsetSamples;
		}
		return new GranularPath(grains);
	}

	private static long applyEdgeMode(long idx, int len, EdgeMode mode, long dir) {
		return switch (mode) {
		case WRAP -> mod(idx, len);
		case CLAMP -> clamp(idx, 0, len - 1L);
		case BOUNCE -> bounce(idx, len);
		};
	}

	private static long mod(long x, int m) {
		long r = x % m;
		return (r < 0) ? r + m : r;
	}

	private static long clamp(long x, long lo, long hi) {
		return Math.max(lo, Math.min(hi, x));
	}

	/**
	 * Reflect index into [0..len-1] like a bouncing ball.
	 * Example for len=5: ... 3 4 3 2 1 0 1 2 3 4 3 ...
	 */
	private static long bounce(long idx, int len) {
		if (len <= 1) return 0;
		long period = 2L * (len - 1);
		long t = idx % period;
		if (t < 0) t += period;
		return (t <= len - 1) ? t : period - t;
	}
}

