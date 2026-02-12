package net.paulhertz.pixelaudio.granular;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import net.paulhertz.pixelaudio.PixelAudioMapper;
import net.paulhertz.pixelaudio.curves.PACurveMaker;
import net.paulhertz.pixelaudio.curves.PAGestureParametric;
import net.paulhertz.pixelaudio.curves.PAPathParametric;

import processing.core.PVector;

/**
 * TODO Candidate for removal
 * 
 * GranularPathBuilder
 *
 * Utility to convert a PACurveMaker path + PixelAudioMapper into a GranularPath.
 *
 * Uses the reduced point set (RDP). PACurveMaker can change the amount of reduction
 * by varying PACurveMaker.epsilon and calling PACurveMaker.calculateDerivedPoints()
 * to redraw curves.
 * 
 * TODO Pitch and other parameters can be modulated based on gesture dynamics, 
 * for example from a pressure-sensitive tablet.
 * 
 */
@Deprecated(since="9.5", forRemoval=false)
public final class LegacyGranularPathBuilder {

	private LegacyGranularPathBuilder() {}

	/**
	 * Build a GranularPath from the RDP points of a PACurveMaker.
	 * Use GranularSettings.TimingMode.FIXED_HOP when you play samples. 
	 * If you use GESTURE_TIMED all samples will play at once. 
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
	 * Use GranularSettings.TimingMode.FIXED_HOP when you play samples. 
	 * If you use GESTURE_TIMED all samples will play at once. 
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
	 * Use GranularSettings.TimingMode.FIXED_HOP when you play samples. 
	 * If you use GESTURE_TIMED all samples will play at once. 
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
		return curve.getDragTimes();
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
			int grainLength,
			float sampleRate) 
	{
		List<GranularPath.GrainSpec> grains = new ArrayList<>();

		if (curve == null || mapper == null || sampleRate < 0f) {
			return new GranularPath(grains);
		}

		List<PVector> dragPoints = curve.getDragPoints(); // unique points from mouse gesture

		if (dragPoints == null || dragPoints.isEmpty()) {
			return new GranularPath(grains);
		}

		// dragTimes are already relative to gesture start:
		// dragTimes[0] == 0, dragTimes[i] == millis since gesture start
		int[] dragTimes = getDragTimes(curve);
		if (dragTimes.length == 0) {return new GranularPath(grains);}

		// Defensive: use the min length in case something is mismatched
		int count = Math.min(dragPoints.size(), dragTimes.length);

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

			int timeOffsetMs = dragTimes[i]; // direct 1:1 mapping
			long offsetSamples = msToSamples(timeOffsetMs, sampleRate); 

			grains.add(new GranularPath.GrainSpec(
					sampleIndex,
					grainLength,
					pitchHint,
					gain,
					pan,
					offsetSamples
					));
		}
	    // FWIW: total gesture duration = offsets[count - 1]
	    // int totalGestureMs = offsets[count - 1];

		return new GranularPath(grains);
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
    		int targetCount,
    		float sampleRate) {
    	// -1 => keep original duration; null => linear warp f(u) = u
    	return fromTimedDragParametric(curve, mapper, canvasWidth, canvasHeight, 
    			                       grainLength, targetCount, -1, sampleRate, null);
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
    		int targetDurationMs,
    		float sampleRate) {
    	// null => linear warp f(u) = u
    	return fromTimedDragParametric(curve, mapper, canvasWidth, canvasHeight, 
    			                       grainLength, targetCount, targetDurationMs, sampleRate, null);

    }
    
    /**
     * Build a timed GranularPath from the polygonized Bezier path, sampling it
     * parametrically with u ∈ [0,1] over the path indices.
     *
     * Each grain k uses:
     *   u = k / (targetCount - 1)
     *   p = pathParam.sample(u)
     *   timeOffsetMs = u * totalDurationMs
     *
     * @param curve            PACurveMaker holding Bezier data
     * @param mapper           PixelAudioMapper to convert (x,y) → sample index
     * @param canvasWidth      for x → pan and bounds
     * @param canvasHeight     for y bounds
     * @param grainLength      grain length in samples
     * @param curveSteps       number of divisions per Bezier segment
     * @param targetCount      number of grains along the path
     * @param totalDurationMs  total duration of the path in ms
     */
    public static GranularPath fromBezierParametric(PACurveMaker curve,
                                                    PixelAudioMapper mapper,
                                                    int canvasWidth,
                                                    int canvasHeight,
                                                    int grainLength,
                                                    int curveSteps,
                                                    int targetCount,
                                                    int totalDurationMs,
                                                    float sampleRate) {
    	 // linear time mapping u → u, pass null for warp function
        return fromBezierParametricWarped(curve, mapper, canvasWidth, canvasHeight,
                grainLength, curveSteps, targetCount, totalDurationMs, sampleRate, null);
    }

    /**
     * General Bezier-parametric builder with optional time warp.
     *
     * u ∈ [0,1] is:
     *   - the spatial parameter over the Bezier polyline (via PAPathParametric)
     *   - also the base for time mapping; timeOffsetMs = f(u) * totalDurationMs
     *
     * If warp is null, f(u) = u (linear time).
     */
    public static GranularPath fromBezierParametricWarped(PACurveMaker curve,
                                                          PixelAudioMapper mapper,
                                                          int canvasWidth,
                                                          int canvasHeight,
                                                          int grainLength,
                                                          int curveSteps,
                                                          int targetCount,
                                                          int totalDurationMs,
                                                          float sampleRate,
                                                          DoubleUnaryOperator warp) {
        List<GranularPath.GrainSpec> grains = new ArrayList<>();
        if (curve == null || mapper == null || sampleRate <= 0f) return new GranularPath(grains);
        if (targetCount <= 0 || totalDurationMs <= 0) return new GranularPath(grains);

        List<PVector> bezPoints = curve.getEventPoints(curveSteps);
        if (bezPoints == null || bezPoints.size() < 2) return new GranularPath(grains);

        PAPathParametric pathParam = new PAPathParametric(bezPoints);

        for (int k = 0; k < targetCount; k++) {
            float u = (targetCount == 1) ? 0f : (float) k / (targetCount - 1);

            // Spatial sample along Bezier polyline
            PVector p = pathParam.sample(u);

            // Time mapping (warped or linear)
            double s = (warp != null) ? warp.applyAsDouble(u) : u;
            if (s < 0.0) s = 0.0;
            if (s > 1.0) s = 1.0;
            int tMs = (int) Math.round(s * totalDurationMs);
    	    long offsetSamples = msToSamples(tMs, sampleRate);


            int x = clampToCanvas(Math.round(p.x), canvasWidth);
            int y = clampToCanvas(Math.round(p.y), canvasHeight);
            int sampleIndex = mapper.lookupSample(x, y);

            float pan = mapXToPan(p.x, canvasWidth);
            float gain = 1.0f;
            float pitchHint = 0.0f;

            grains.add(new GranularPath.GrainSpec(
                    sampleIndex,
                    grainLength,
                    pitchHint,
                    gain,
                    pan,
                    offsetSamples
            ));
        }
        return new GranularPath(grains);
    }

    /**
     * Build a timed GranularPath that uses:
     *   - the Bezier path for spatial positions (via getEventPoints/PAPathParametric),
     *   - the drag gesture's timing (via getDragOffsetsAsInts) for timeOffsetMs.
     *
     * For each drag time t_i:
     *   u = t_i / totalGestureDuration
     *   p = BezierPath(u)
     *   timeOffsetMs = t_i
     *
     * This maps the *rhythm* of the original gesture onto the *shape* of the Bezier path.
     */
    public static GranularPath fromBezierWithGestureTiming(PACurveMaker curve,
    		PixelAudioMapper mapper,
    		int canvasWidth,
    		int canvasHeight,
    		int grainLength,
    		int curveSteps,
    		float sampleRate) {
    	List<GranularPath.GrainSpec> grains = new ArrayList<>();
    	if (curve == null || mapper == null || sampleRate <= 0f) return new GranularPath(grains);

    	// Gesture timing
    	int[] dragTimes = curve.getDragTimes();
    	if (dragTimes == null || dragTimes.length == 0) return new GranularPath(grains);

    	int countTimes = dragTimes.length;
    	int totalMs = dragTimes[countTimes - 1];
    	if (totalMs < 0) totalMs = 0;

    	// Bezier geometry → polygonized points
    	List<PVector> bezPoints = curve.getEventPoints(curveSteps);
    	if (bezPoints == null || bezPoints.size() < 2) return new GranularPath(grains);

    	// Parametric view of Bezier path (index-based for now)
    	PAPathParametric pathParam = new PAPathParametric(bezPoints);

    	for (int i = 0; i < countTimes; i++) {
    		int tMs = dragTimes[i];
    		float u = (totalMs <= 0) ? 0f : (float) tMs / (float) totalMs;

    		// Spatial position on Bezier path at this normalized time
    		PVector p = pathParam.sample(u);

    		int x = clampToCanvas(Math.round(p.x), canvasWidth);
    		int y = clampToCanvas(Math.round(p.y), canvasHeight);
    		int sampleIndex = mapper.lookupSample(x, y);

    		float pan = mapXToPan(p.x, canvasWidth);
    		float gain = 1.0f;
    		float pitchHint = 0.0f;

    		long offsetSamples = msToSamples(tMs, sampleRate);

    		grains.add(new GranularPath.GrainSpec(
    				sampleIndex,
    				grainLength,
    				pitchHint,
    				gain,
    				pan,
    				offsetSamples
    				));
    	}
    	return new GranularPath(grains);
    }

    public static GranularPath fromBezierWithGestureTimingScaled(PACurveMaker curve,
    		PixelAudioMapper mapper,
    		int canvasWidth,
    		int canvasHeight,
    		int grainLength,
    		int curveSteps,
    		int targetDurationMs,
    		float sampleRate) {
    	List<GranularPath.GrainSpec> grains = new ArrayList<>();
    	if (curve == null || mapper == null || sampleRate <= 0f) return new GranularPath(grains);

    	int[] dragTimes = curve.getDragTimes();
    	if (dragTimes == null || dragTimes.length == 0) return new GranularPath(grains);

    	int countTimes = dragTimes.length;
    	int originalDurationMs = dragTimes[countTimes - 1];
    	if (originalDurationMs <= 0 || targetDurationMs <= 0) {
    		// fall back to unscaled
    		return fromBezierWithGestureTiming(curve, mapper, canvasWidth, canvasHeight, grainLength, curveSteps, sampleRate);
    	}

    	float timeScale = targetDurationMs / (float) originalDurationMs;

    	List<PVector> bezPoints = curve.getEventPoints(curveSteps);
    	if (bezPoints == null || bezPoints.size() < 2) return new GranularPath(grains);

    	PAPathParametric pathParam = new PAPathParametric(bezPoints);

    	for (int i = 0; i < countTimes; i++) {
    		int tOrig = dragTimes[i];
    		float u = tOrig / (float) originalDurationMs;
    		int tScaled = Math.round(tOrig * timeScale); 
    		long offsetSamples = msToSamples(tScaled, sampleRate);

    		PVector p = pathParam.sample(u);

    		int x = clampToCanvas(Math.round(p.x), canvasWidth);
    		int y = clampToCanvas(Math.round(p.y), canvasHeight);
    		int sampleIndex = mapper.lookupSample(x, y);

    		float pan = mapXToPan(p.x, canvasWidth);
    		float gain = 1.0f;
    		float pitchHint = 0.0f;

    		grains.add(new GranularPath.GrainSpec(
    				sampleIndex,
    				grainLength,
    				pitchHint,
    				gain,
    				pan,
    				offsetSamples
    				));
    	}
    	return new GranularPath(grains);
    }
    
    /**
     * Build a linear path from a point, equivalent to "classic" granular synthesis.
     * 
     * @param x                x-coordinate on where user clicked display
     * @param y                y-coordinate
     * @param mapper           PixelAudioMapper for pixel to signal mapping
     * @param canvasWidth      display image width
     * @param canvasHeight     display image height
     * @param grainLength      length of grains in samples
     * @param hopLength        distance to advance from on grain to the next
     * @param numGrains        total number of grains
     * @return                 a GranularPath 
     */
    public static GranularPath fromPointToLinearPath(int x, int y, PixelAudioMapper mapper,
    		int canvasWidth, int canvasHeight,
    		int grainLength, int hopLength, int numGrains,
    		float sampleRate) {
    	List<GranularPath.GrainSpec> grains = new ArrayList<>();
    	if (mapper == null) return new GranularPath(grains);
 
        x = clampToCanvas(x, canvasWidth);
        y = clampToCanvas(y, canvasHeight);
    	int startSampleIndex = mapper.lookupSample(x, y);
    	int maxSampleIndex = mapper.getSize() - grainLength;    	
        if (maxSampleIndex <= 0 || numGrains <= 0) return new GranularPath(grains);

        int i = 0;
    	int sampleIndex = startSampleIndex;
    	int hopSamples = hopLength;
    	while (sampleIndex <= maxSampleIndex && i < numGrains) {
    		float pan = mapXToPan(x, canvasWidth);
    		float gain = 1.0f;
    		float pitchHint = 0.0f;
    		long offsetSamples = (long) i * (long) hopSamples;
			grains.add(new GranularPath.GrainSpec(
					sampleIndex,
					grainLength,
					pitchHint,
					gain,
					pan,
					offsetSamples));
    		i++;
    		sampleIndex += hopLength;
    	}   	
    	return new GranularPath(grains);
    }
    
    public static GranularPath fromPointToLinearPathDuration(int x, int y,
            PixelAudioMapper mapper,
            int canvasWidth, int canvasHeight,
            int grainLength, int hopLength,
            float durationMs, float sampleRate) {
        int maxGrains = (int) Math.ceil(
            (durationMs * 0.001f * sampleRate - grainLength) / hopLength
        );
        if (maxGrains < 1) maxGrains = 1;
        return fromPointToLinearPath(
            x, y, mapper,
            canvasWidth, canvasHeight,
            grainLength, hopLength, maxGrains,
            sampleRate
        );
    }

    
    /**
     * Build a linear path from a point, classic granular synthesis with
     * optional time-stretch and pitch-shift.
     *
     * All timing is in samples; no ms inside the builder.
     *
     * @param x                x-coordinate where user clicked display
     * @param y                y-coordinate
     * @param mapper           PixelAudioMapper for pixel to signal mapping
     * @param canvasWidth      display image width
     * @param canvasHeight     display image height
     * @param grainLength      length of grains in samples
     * @param hopLength        base hop in samples (reference hop)
     * @param numGrains        total number of grains
     * @param sampleRate       playback sample rate (e.g. 44100) — only used to sanity-check, not required for math
     * @param timeStretch      factor for overall duration (1.0 = original,
     *                         2.0 = twice as long, 0.5 = twice as fast)
     * @param pitchRatio       factor for how fast we walk the buffer
     *                         (1.0 = original, 2.0 = up an octave-ish, 0.5 = down)
     */
    public static GranularPath fromPointToLinearPathTimedAndPitched(
            int x, int y,
            PixelAudioMapper mapper,
            int canvasWidth, int canvasHeight,
            int grainLength, int hopLength, int numGrains,
            float sampleRate,   // kept for consistency, but not strictly needed
            float timeStretch,
            float pitchRatio) {

        List<GranularPath.GrainSpec> grains = new ArrayList<>();
        if (mapper == null || numGrains <= 0) {
            return new GranularPath(grains);
        }

        // Clamp coordinates to canvas
        x = clampToCanvas(x, canvasWidth);
        y = clampToCanvas(y, canvasHeight);

        int startSampleIndex = mapper.lookupSample(x, y);
        int maxSampleIndex   = mapper.getSize() - grainLength;
        if (maxSampleIndex <= 0) {
            return new GranularPath(grains);
        }

        if (timeStretch <= 0f) timeStretch = 1f;
        if (pitchRatio  <= 0f) pitchRatio  = 1f;

        // Temporal hop in samples (for offsetSamples)
        float hopSamplesF = hopLength * timeStretch;
        if (hopSamplesF <= 0f) hopSamplesF = hopLength;
        // Source hop in samples for buffer traversal
        float sourceHop = hopLength * pitchRatio;

        float currentIndexF   = startSampleIndex;
        float currentOffsetF  = 0f;

        for (int i = 0; i < numGrains; i++) {
            int sampleIndex = Math.round(currentIndexF);
            if (sampleIndex < 0 || sampleIndex > maxSampleIndex) {
                break; // stop if we run out of buffer
            }

            long offsetSamples = Math.round(currentOffsetF);

            float pan       = mapXToPan(x, canvasWidth);
            float gain      = 1.0f;
            float pitchHint = 0.0f;

            grains.add(new GranularPath.GrainSpec(
                    sampleIndex,
                    grainLength,
                    pitchHint,
                    gain,
                    pan,
                    offsetSamples
            ));

            currentIndexF  += sourceHop;
            currentOffsetF += hopSamplesF;
        }

        return new GranularPath(grains);
    }


    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    public static GranularPath fromTimedDragParametric(PACurveMaker curve,
    		PixelAudioMapper mapper,
    		int canvasWidth,
    		int canvasHeight,
    		int grainLength,
    		int targetCount,
    		int targetDurationMs,
    		float sampleRate,
    		DoubleUnaryOperator warp) {
        	return fromTimedDragParametric(curve.getDragPoints(), getDragTimes(curve),
    			                mapper, canvasWidth, canvasHeight,
    			                grainLength, targetCount, targetDurationMs, sampleRate, warp);
    }
    
    public static GranularPath fromTimedDragParametric(ArrayList<PVector> dragPoints,
    	    int[] dragTimes,
    	    PixelAudioMapper mapper,
    	    int canvasWidth,
    	    int canvasHeight,
    	    int grainLength,
    	    int targetCount,
    	    int targetDurationMs,
    	    float sampleRate,
    	    DoubleUnaryOperator warp) {
    	
    	  List<GranularPath.GrainSpec> grains = new ArrayList<>();
    	  if (mapper == null || sampleRate <= 0f) return new GranularPath(grains);
    	  if (dragPoints == null || dragPoints.isEmpty()) return new GranularPath(grains);
    	  if (dragTimes.length != dragPoints.size()) return new GranularPath(grains);

    	  PAGestureParametric gp = new PAGestureParametric(dragPoints, dragTimes);
    	  float originalDurMs = gp.getTotalDurationMs();
    	  if (originalDurMs <= 0 || targetCount <= 0) return new GranularPath(grains);

    	  float timeScale = 1f;
    	  if (targetDurationMs > 0) {
    	    timeScale = targetDurationMs / originalDurMs;
    	  }

    	  for (int k = 0; k < targetCount; k++) {
    	    float u = (targetCount == 1) ? 0f : (float) k / (targetCount - 1);
    	    PAGestureParametric.Sample s =
    	        (warp != null) ? gp.sample(u, warp) : gp.sample(u);

    	    float tScaled = s.tMs * timeScale;
    	    int tMs = Math.round(tScaled);
    	    long offsetSamples = msToSamples(tMs, sampleRate);

    	    int x = clampToCanvas(Math.round(s.x), canvasWidth);
    	    int y = clampToCanvas(Math.round(s.y), canvasHeight);
    	    int sampleIndex = mapper.lookupSample(x, y);

    	    float pan = mapXToPan(s.x, canvasWidth);
    	    float gain = 1.0f;
    	    float pitchHint = 0.0f;

    	    grains.add(new GranularPath.GrainSpec(
    	        sampleIndex,
    	        grainLength,
    	        pitchHint,
    	        gain,
    	        pan,
    	        offsetSamples
    	        ));
    	  }

    	  return new GranularPath(grains);
    	}


    
    private static long msToSamples(int tMs, float sampleRate) {
        if (sampleRate <= 0f) return 0L;
        return Math.round((tMs / 1000f) * sampleRate);
    }

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
