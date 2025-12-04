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

    // ------------------------------------------------------------------------
    // NEW: Gesture resampling helpers
    // ------------------------------------------------------------------------

    /** Simple container for resampled gesture. */
    public static final class ResampledGesture {
        public final List<PVector> points;
        public final int[] timesMs;

        public ResampledGesture(List<PVector> points, int[] timesMs) {
            this.points = points;
            this.timesMs = timesMs;
        }
    }

    /**
     * Resample a gesture (points + timesMs) to exactly targetCount samples,
     * preserving the original duration and using linear interpolation in time.
     */
    private static ResampledGesture resampleGesture(List<PVector> points,
                                                    int[] timesMs,
                                                    int targetCount) {
        if (points == null || timesMs == null) {
            throw new IllegalArgumentException("points and timesMs must not be null");
        }
        if (points.size() != timesMs.length) {
            throw new IllegalArgumentException("points.size() must equal timesMs.length");
        }
        if (points.isEmpty() || targetCount <= 0) {
            return new ResampledGesture(new ArrayList<>(), new int[0]);
        }

        int originalCount = points.size();
        if (targetCount == originalCount) {
            return new ResampledGesture(new ArrayList<>(points), timesMs.clone());
        }

        int originalDurationMs = timesMs[originalCount - 1];
        if (originalDurationMs <= 0) {
            // Degenerate case: all times same; copy first point
            List<PVector> outPts = new ArrayList<>(targetCount);
            int[] outTimes = new int[targetCount];
            for (int k = 0; k < targetCount; k++) {
                outPts.add(points.get(0).copy());
                outTimes[k] = 0;
            }
            return new ResampledGesture(outPts, outTimes);
        }

        List<PVector> outPoints = new ArrayList<>(targetCount);
        int[] outTimes = new int[targetCount];

        int segIndex = 0;
        for (int k = 0; k < targetCount; k++) {
            float alpha = (targetCount == 1) ? 0f : (float) k / (targetCount - 1);
            float targetTimeF = alpha * originalDurationMs;
            int targetTime = Math.round(targetTimeF);

            // advance segment index until timesMs[segIndex+1] >= targetTime
            while (segIndex < originalCount - 2 && timesMs[segIndex + 1] < targetTime) {
                segIndex++;
            }

            int t0 = timesMs[segIndex];
            int t1 = timesMs[Math.min(segIndex + 1, originalCount - 1)];
            PVector p0 = points.get(segIndex);
            PVector p1 = points.get(Math.min(segIndex + 1, originalCount - 1));

            float u;
            if (t1 == t0) {
                u = 0f;
            } else {
                u = (targetTime - t0) / (float) (t1 - t0);
                if (u < 0f) u = 0f;
                if (u > 1f) u = 1f;
            }

            float x = lerp(p0.x, p1.x, u);
            float y = lerp(p0.y, p1.y, u);

            outPoints.add(new PVector(x, y));
            outTimes[k] = targetTime;
        }

        return new ResampledGesture(outPoints, outTimes);
    }
    
    /**
     * Resample to targetCount samples (preserving original duration),
     * then scale the gesture to a new total duration targetDurationMs.
     */
    private static ResampledGesture resampleGestureToCountAndDuration(List<PVector> points,
                                                                      int[] timesMs,
                                                                      int targetCount,
                                                                      int targetDurationMs) {
        ResampledGesture base = resampleGesture(points, timesMs, targetCount);
        if (base.points.isEmpty() || targetDurationMs <= 0) {
            return base;
        }

        int originalDurationMs = base.timesMs[base.timesMs.length - 1];
        if (originalDurationMs <= 0) {
            return base;
        }

        float scale = targetDurationMs / (float) originalDurationMs;
        int[] scaledTimes = new int[base.timesMs.length];
        for (int i = 0; i < scaledTimes.length; i++) {
            scaledTimes[i] = Math.round(base.timesMs[i] * scale);
        }

        return new ResampledGesture(base.points, scaledTimes);
    }


    /**
     * Build a timed GranularPath from the drag points, but first resample the
     * gesture (time + location) to targetCount samples.
     *
     * This lets you:
     *  - reduce or increase the point count
     *  - preserve the original gesture duration and coarse timing structure
     */
    public static GranularPath fromTimedDragPointsResampled(PACurveMaker curve,
                                                            PixelAudioMapper mapper,
                                                            int canvasWidth,
                                                            int canvasHeight,
                                                            int grainLength,
                                                            int targetCount) {
        List<GranularPath.GrainSpec> grains = new ArrayList<>();
        if (curve == null || mapper == null) return new GranularPath(grains);

        List<PVector> dragPoints = curve.getDragPoints();
        if (dragPoints == null || dragPoints.isEmpty()) return new GranularPath(grains);

        int[] dragTimes = getDragTimes(curve);
        if (dragTimes.length != dragPoints.size()) return new GranularPath(grains);

        ResampledGesture rg = resampleGesture(dragPoints, dragTimes, targetCount);
        if (rg.points.isEmpty()) return new GranularPath(grains);

        for (int i = 0; i < rg.points.size(); i++) {
            PVector p = rg.points.get(i);
            int tMs = rg.timesMs[i];

            int x = clampToCanvas(Math.round(p.x), canvasWidth);
            int y = clampToCanvas(Math.round(p.y), canvasHeight);
            int sampleIndex = mapper.lookupSample(x, y);

            float pan = mapXToPan(p.x, canvasWidth);
            float gain = 1.0f;
            float pitchHint = 0.0f;
            int timeOffsetMs = tMs;

            grains.add(new GranularPath.GrainSpec(
                    sampleIndex,
                    grainLength,
                    pitchHint,
                    gain,
                    pan,
                    timeOffsetMs
            ));
        }

        return new GranularPath(grains);
    }

    
    /**
     * Build a timed GranularPath from drag points, resampling the gesture
     * to targetCount samples and scaling it to a total duration of
     * targetDurationMs milliseconds.
     *
     * This lets you say:
     *   "Give me N grains over D ms, following the same gesture."
     */
    public static GranularPath fromTimedDragPointsResampledToDuration(PACurveMaker curve,
                                                                      PixelAudioMapper mapper,
                                                                      int canvasWidth,
                                                                      int canvasHeight,
                                                                      int grainLength,
                                                                      int targetCount,
                                                                      int targetDurationMs) {
        List<GranularPath.GrainSpec> grains = new ArrayList<>();
        if (curve == null || mapper == null) return new GranularPath(grains);

        List<PVector> dragPoints = curve.getDragPoints();
        if (dragPoints == null || dragPoints.isEmpty()) return new GranularPath(grains);

        int[] dragTimes = getDragTimes(curve);
        if (dragTimes.length != dragPoints.size()) return new GranularPath(grains);

        ResampledGesture rg = resampleGestureToCountAndDuration(
                dragPoints,
                dragTimes,
                targetCount,
                targetDurationMs
        );
        if (rg.points.isEmpty()) return new GranularPath(grains);

        for (int i = 0; i < rg.points.size(); i++) {
            PVector p = rg.points.get(i);
            int tMs = rg.timesMs[i];

            int x = clampToCanvas(Math.round(p.x), canvasWidth);
            int y = clampToCanvas(Math.round(p.y), canvasHeight);
            int sampleIndex = mapper.lookupSample(x, y);

            float pan = mapXToPan(p.x, canvasWidth);
            float gain = 1.0f;
            float pitchHint = 0.0f;
            int timeOffsetMs = tMs;

            grains.add(new GranularPath.GrainSpec(
                    sampleIndex,
                    grainLength,
                    pitchHint,
                    gain,
                    pan,
                    timeOffsetMs
            ));
        }

        return new GranularPath(grains);
    }

	
    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private static float mapXToPan(float x, int width) {
		if (width <= 1) return 0f;
		float norm = x / (float) (width - 1); // 0..1
		return norm * 2f - 1f;                // -1..+1
	}
	
    private static int clampToCanvas(int v, int max) {
        if (max <= 0) return 0;
        if (v < 0) return 0;
        if (v >= max) return max - 1;
        return v;
    }

    private static float lerp(float a, float b, float u) {
        return a + (b - a) * u;
    }

}
