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
			int grainLength) 
	{
		List<GranularPath.GrainSpec> grains = new ArrayList<>();

		if (curve == null || mapper == null) {
			return new GranularPath(grains);
		}

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
					pan,
					0));
		}

		return new GranularPath(grains);
	}

	/**
	 * Build a GranularPath from the polygonized Bezier points of a PACurveMaker.
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
            int curveSteps) 
	{
		List<GranularPath.GrainSpec> grains = new ArrayList<>();

		if (curve == null || mapper == null) {
			return new GranularPath(grains);
		}

		List<PVector> bezPoints = curve.getEventPoints(curveSteps); // polygonized Bezier path

		if (bezPoints == null || bezPoints.isEmpty()) {
			return new GranularPath(grains);
		}

		for (PVector p : bezPoints) {
			// round Bezier points to integers and then clamp to canvas bounds
			int x = Math.round(p.x);
			int y = Math.round(p.y);
			x = Math.min(Math.max(0, x), canvasWidth - 1);
			y = Math.min(Math.max(0, y), canvasHeight - 1);
			// get the audio sample index from the mapper
			int sampleIndex = mapper.lookupSample(x, y);

			float pan = mapXToPan(p.x, canvasWidth);
			float gain = 1.0f;      // placeholder; later we can map from y or speed
			float pitchHint = 0.0f; // reserved for future use

			grains.add(new GranularPath.GrainSpec(
					sampleIndex,
					grainLength,
					pitchHint,
					gain,
					pan,
					0));
		}

		return new GranularPath(grains);
	}

	/**
	 * Build a GranularPath from the drag points of a PACurveMaker, i.e., the 
	 * unique points input by dragging the mouse. 
	 *
	 * @param curve          PACurveMaker holding gesture data
	 * @param mapper         PixelAudioMapper to convert (x,y) → sample index
	 * @param canvasWidth    for mapping x → pan
	 * @param canvasHeight   (not used yet; could map y → gain or pitch)
	 * @param grainLength    grain length in samples (per-grain)
	 * @return GranularPath suitable for PathGranularSource
	 */
	public static GranularPath fromDragPoints(PACurveMaker curve,
			PixelAudioMapper mapper,
			int canvasWidth,
			int canvasHeight,
			int grainLength) 
	{
		List<GranularPath.GrainSpec> grains = new ArrayList<>();

		if (curve == null || mapper == null) {
			return new GranularPath(grains);
		}

		List<PVector> dragPoints = curve.getDragPoints(); // unique points from mouse gesture

		if (dragPoints == null || dragPoints.isEmpty()) {
			return new GranularPath(grains);
		}

		for (PVector p : dragPoints) {
			// round drag points to integers and then clamp to canvas bounds
			// for drag points these may be unecessary precautions
			int x = Math.round(p.x);
			int y = Math.round(p.y);
			x = Math.min(Math.max(0, x), canvasWidth - 1);
			y = Math.min(Math.max(0, y), canvasHeight - 1);
			// get the audio sample index from the mapper
			int sampleIndex = mapper.lookupSample(x, y);

			float pan = mapXToPan(p.x, canvasWidth);
			float gain = 1.0f;      // placeholder; later we can map from y or speed
			float pitchHint = 0.0f; // reserved for future use

			grains.add(new GranularPath.GrainSpec(
					sampleIndex,
					grainLength,
					pitchHint,
					gain,
					pan,
					0));
		}

		return new GranularPath(grains);
	}
	
	/**
	 * Returns a list of time offsets in milliseconds for PACurveMaker curve, where each offset
	 * corresponds to a point in curve.dragPoints. Both arrays are created by a mouse gesture. 
	 * 
	 * @param curve    a PACurveMaker object
	 * @return
	 */
	public static int[] getDragTimes(PACurveMaker curve) {
		if (curve == null) return new int[0];
		return curve.getDragOffsetsAsInts();
	}
	
	/**
	 * Build a GranularPath from the drag points of a PACurveMaker, i.e., the 
	 * unique points input by dragging the mouse, with grain intervals determined by
	 * the time each point was drawn.
	 *
	 * @param curve          PACurveMaker holding gesture data
	 * @param mapper         PixelAudioMapper to convert (x,y) → sample index
	 * @param canvasWidth    for mapping x → pan
	 * @param canvasHeight   (not used yet; could map y → gain or pitch)
	 * @param grainLength    grain length in samples (per-grain)
	 * @return GranularPath suitable for PathGranularSource
	 */
	public static GranularPath fromTimedDragPoints(PACurveMaker curve,
			PixelAudioMapper mapper,
			int canvasWidth,
			int canvasHeight,
			int grainLength) 
	{
		List<GranularPath.GrainSpec> grains = new ArrayList<>();

		if (curve == null || mapper == null) {
			return new GranularPath(grains);
		}

		List<PVector> dragPoints = curve.getDragPoints(); // unique points from mouse gesture

		if (dragPoints == null || dragPoints.isEmpty()) {
			return new GranularPath(grains);
		}

		// Offsets are already relative to gesture start:
		// offsets[0] == 0, offsets[i] == millis since gesture start
		int[] offsets = getDragTimes(curve);

		if (offsets.length == 0) {
			return new GranularPath(grains);
		}

		// Defensive: use the min length in case something is mismatched
		int count = Math.min(dragPoints.size(), offsets.length);

		for (int i = 0; i < count; i++) {
			PVector p = dragPoints.get(i);

			// Round and clamp to canvas bounds
			int x = Math.round(p.x);
			int y = Math.round(p.y);
			x = Math.min(Math.max(0, x), canvasWidth - 1);
			y = Math.min(Math.max(0, y), canvasHeight - 1);

			int sampleIndex = mapper.lookupSample(x, y);

			float pan = mapXToPan(p.x, canvasWidth);
			float gain = 1.0f;      // hook for y/speed mapping later
			float pitchHint = 0.0f; // reserved for future use

			int timeOffsetMs = offsets[i]; // direct 1:1 mapping

			grains.add(new GranularPath.GrainSpec(
					sampleIndex,
					grainLength,
					pitchHint,
					gain,
					pan,
					timeOffsetMs
					));
		}
	    // FWIW: total gesture duration = offsets[count - 1]
	    // int totalGestureMs = offsets[count - 1];

		return new GranularPath(grains);
	}

	
	private static float mapXToPan(float x, int width) {
		if (width <= 1) return 0f;
		float norm = x / (float) (width - 1); // 0..1
		return norm * 2f - 1f;                // -1..+1
	}
}
