package net.paulhertz.pixelaudio.granular;

import java.util.ArrayList;
import java.util.List;

import net.paulhertz.pixelaudio.PixelAudioMapper;
import net.paulhertz.pixelaudio.curves.PACurveMaker;
import processing.core.PVector;

/**
 * GranularPathBuilder
 *
 * Utility to convert a PACurveMaker path + PixelAudioMapper into a GranularPath.
 *
 * Uses the reduced point set (RDP) for now.
 * Later we can add variants that use interpolated Bezier points or drag points.
 */
public final class GranularPathBuilder {

	private GranularPathBuilder() {}

	/**
	 * Build a GranularPath from the RDP points of a PACurveMaker.
	 *
	 * @param curve          PACurveMaker holding gesture data
	 * @param mapper         PixelAudioMapper to convert (x,y) → sample index
	 * @param canvasWidth    for mapping x → pan
	 * @param canvasHeight   (not used yet; could map y → gain or pitch)
	 * @param grainLength    grain length in samples (per-grain)
	 * @return GranularPath suitable for PathGranularSource
	 */
	public static GranularPath fromRdpPoints(PACurveMaker curve,
			PixelAudioMapper mapper,
			int canvasWidth,
			int canvasHeight,
			int grainLength) {
		List<GranularPath.GrainSpec> grains = new ArrayList<>();

		if (curve == null || mapper == null) {
			return new GranularPath(grains);
		}

		@SuppressWarnings("unchecked")
		List<PVector> rdpPoints = curve.getRdpPoints(); // adjust if the API returns another type

		if (rdpPoints == null || rdpPoints.isEmpty()) {
			return new GranularPath(grains);
		}

		for (PVector p : rdpPoints) {
			int x = Math.round(p.x);
			int y = Math.round(p.y);

			int sampleIndex = mapper.lookupSample(x, y);

			float pan = mapXToPan(p.x, canvasWidth);
			float gain = 1.0f;      // placeholder; later we can map from y or speed
			float pitchHint = 0.0f; // reserved for future use

			grains.add(new GranularPath.GrainSpec(
					sampleIndex,
					grainLength,
					pitchHint,
					gain,
					pan
					));
		}

		return new GranularPath(grains);
	}

	/**
	 * Build a GranularPath from the RDP points of a PACurveMaker.
	 *
	 * @param curve          PACurveMaker holding gesture data
	 * @param mapper         PixelAudioMapper to convert (x,y) → sample index
	 * @param canvasWidth    for mapping x → pan
	 * @param canvasHeight   (not used yet; could map y → gain or pitch)
	 * @param grainLength    grain length in samples (per-grain)
	 * @param curveSteps     number of divisions in each Bezier segment
	 * @return GranularPath suitable for PathGranularSource
	 */
	public static GranularPath fromBezierPoints(PACurveMaker curve,
			PixelAudioMapper mapper,
			int canvasWidth,
			int canvasHeight,
			int grainLength,
            int curveSteps) {
		List<GranularPath.GrainSpec> grains = new ArrayList<>();

		if (curve == null || mapper == null) {
			return new GranularPath(grains);
		}

		@SuppressWarnings("unchecked")
		List<PVector> bezPoints = curve.getEventPoints(curveSteps); // polygonized Bezier path

		if (bezPoints == null || bezPoints.isEmpty()) {
			return new GranularPath(grains);
		}

		for (PVector p : bezPoints) {
			int x = Math.round(p.x);
			int y = Math.round(p.y);

			int sampleIndex = mapper.lookupSample(x, y);

			float pan = mapXToPan(p.x, canvasWidth);
			float gain = 1.0f;      // placeholder; later we can map from y or speed
			float pitchHint = 0.0f; // reserved for future use

			grains.add(new GranularPath.GrainSpec(
					sampleIndex,
					grainLength,
					pitchHint,
					gain,
					pan
					));
		}

		return new GranularPath(grains);
	}

	
	
	private static float mapXToPan(float x, int width) {
		if (width <= 1) return 0f;
		float norm = x / (float) (width - 1); // 0..1
		return norm * 2f - 1f;                // -1..+1
	}
}
