
package net.paulhertz.pixelaudio.granular;

import java.util.Arrays;
import java.util.Objects;

import ddf.minim.analysis.WindowFunction;
import ddf.minim.analysis.HannWindow;

import net.paulhertz.pixelaudio.schedule.GestureSchedule;
import processing.core.PVector;

/**
 * High-level facade: play a gesture schedule using a {@link PAGranularInstrument}.
 */ 
public final class PAGranularInstrumentDirector {
    private final PAGranularInstrument instrument;
    private final float sampleRate;

    // --- cache (offsets only)
    private int lastScheduleSize = -1;
    private float lastScheduleEndMs = Float.NaN;
    private long[] cachedEventOffsetsSamples = new long[0];
    
    // Cache for schedule transform results (identity-based)
    private GestureSchedule lastRawScheduleRef = null;
    private GestureGranularParams lastOffsetsParamsRef = null;
    private GestureSchedule lastTransformedScheduleRef = null;
    private GestureSchedule lastOffsetsScheduleRef = null;


    private static final WindowFunction DEFAULT_GRAIN_WINDOW = new HannWindow();

    private static WindowFunction resolveGrainWindow(GestureGranularParams params) {
        if (params == null) return DEFAULT_GRAIN_WINDOW;
        WindowFunction wf = params.grainWindow;
        return (wf != null) ? wf : DEFAULT_GRAIN_WINDOW;
    }

    public PAGranularInstrumentDirector(PAGranularInstrument instrument) {
        this.instrument = Objects.requireNonNull(instrument, "instrument");
        this.sampleRate = instrument.getSampleRate();
    }

    public void playGestureNow(float[] monoBuf, GestureSchedule schedule, 
		GestureGranularParams params, int[] startIndices) {
        long now = instrument.getSampleCursor();
        now += (long)(0.005f * sampleRate); // optional lead-in (once)
        playGestureAtSampleTime(monoBuf, schedule, params, startIndices, now);
    }
    
    // include a per-grain pan array
    public void playGestureNow(float[] monoBuf, GestureSchedule schedule, 
		GestureGranularParams params, int[] startIndices, float[] panValues) {
        long now = instrument.getSampleCursor();
        now += (long)(0.005f * sampleRate); // optional lead-in (once)
        playGestureAtSampleTime(monoBuf, schedule, params, startIndices, panValues, now);
    }

    public void playGestureNow(float[] monoBuf,
            GestureSchedule schedule,
            GestureGranularParams params,
            GestureEventParams evtParams) {

        long now = instrument.getSampleCursor();
        now += (long)(0.005f * sampleRate); // optional lead-in (once)
        playGestureAtSampleTime(monoBuf, schedule, params, evtParams, now);
    }

    public void playGestureAtSampleTime(float[] monoBuf,
            GestureSchedule schedule,
            GestureGranularParams params,
            int[] startIndices,
            long startSampleTime) {

        if (schedule == null) return;
        GestureSchedule sched = prepareSchedule(schedule, params);
        final int n = sched.size();
        if (n <= 0) return;

        GestureEventParams evtParams = GestureEventParams.builder(n)
                .startIndices(startIndices)
                .build();

        playGestureAtSampleTime(monoBuf, schedule, params, evtParams, startSampleTime);
    }

    public void playGestureAtSampleTime(float[] monoBuf,
            GestureSchedule schedule,
            GestureGranularParams params,
            int[] startIndices,
            float[] panValues,
            long startSampleTime) {

        if (schedule == null) return;
        GestureSchedule sched = prepareSchedule(schedule, params);
        final int n = sched.size();
        if (n <= 0) return;

        GestureEventParams evtParams = GestureEventParams.builder(n)
                .startIndices(startIndices)
                .pan(panValues)
                .build();

        playGestureAtSampleTime(monoBuf, schedule, params, evtParams, startSampleTime);
    }

    public void playGestureAtSampleTime(float[] monoBuf,
            GestureSchedule schedule,
            GestureGranularParams params,
            GestureEventParams evtParams,
            long startSampleTime) {

        if (monoBuf == null || monoBuf.length == 0) return;
        if (schedule == null || params == null || evtParams == null) return;

        // 1) Transform schedule times (ms domain) according to params
        GestureSchedule sched = prepareSchedule(schedule, params);
        final int n = sched.size();
        if (n <= 0) return;

        // Validate params alignment
        if (evtParams.n != n) return; // or throw IllegalArgumentException for stricter behavior
        if (evtParams.startIndices == null || evtParams.startIndices.length < n) return;

        // 2) Cache offsets
        ensureCache(sched, params);

        // 3) Schedule Model-A events (Director creates sources)
        final WindowFunction grainWf = resolveGrainWindow(params);
        scheduleEvents(monoBuf, sched, params, evtParams, grainWf, startSampleTime);
    }
    
    public void playGestureAtSampleTimeTransformed(float[] monoBuf,
            GestureSchedule transformedSchedule,
            GestureGranularParams params,
            GestureEventParams evtParams,
            long startSampleTime) {

        if (monoBuf == null || monoBuf.length == 0) return;
        if (transformedSchedule == null || transformedSchedule.isEmpty()) return;
        if (params == null || evtParams == null) return;

        final int n = transformedSchedule.size();
        if (evtParams.n != n) return;

        ensureCache(transformedSchedule, params);
        scheduleEvents(monoBuf, transformedSchedule, params, evtParams, resolveGrainWindow(params), startSampleTime);
    }
    
    private void scheduleEvents(float[] monoBuf,
            GestureSchedule sched,
            GestureGranularParams params,
            GestureEventParams evtPparams,
            WindowFunction wf,
            long startSampleTime) {

        final int n = sched.size();
        if (n <= 0) return;

        final int grainLen = Math.max(1, params.grainLengthSamples);
        final int hop      = Math.max(1, params.hopLengthSamples);

        // Model A semantics (unchanged)
        final int burstGrains = Math.max(1, params.burstGrains);
        final int timeHop  = hop;
        final int indexHop = hop;

        final boolean fixedHop = (params.hopMode == GestureGranularParams.HopMode.FIXED);

        // Defaults from params
        final float defaultPan   = params.pan;
        final float defaultGain  = params.gainLinear;
        final float defaultPitch = (params.pitchRatio > 0f) ? params.pitchRatio : 1.0f;

        // Prewarm window curve (avoid first-hit allocation)
        if (wf != null && grainLen > 1) {
            WindowCache.INSTANCE.prewarm(wf, grainLen);
        }

        for (int i = 0; i < n; i++) {
            final long tEvent = fixedHop ? (long)i * (long)hop : cachedEventOffsetsSamples[i];
            final long when = startSampleTime + tEvent;

            int idx = evtPparams.startIndices[i];
            if (idx < 0) idx = 0;
            if (idx >= monoBuf.length) idx = monoBuf.length - 1;

            final float pan = (evtPparams.pan != null) ? clampPan(evtPparams.pan[i]) : defaultPan;
            final float gain = (evtPparams.gain != null) ? Math.max(0f, evtPparams.gain[i]) : defaultGain;
            final float pitchRatio = (evtPparams.pitchRatio != null)
                    ? Math.max(1e-6f, evtPparams.pitchRatio[i])
                    : defaultPitch;

            PASource src = new PABurstGranularSource(
                    monoBuf,
                    idx,
                    grainLen,
                    burstGrains,
                    timeHop,
                    indexHop,
                    pitchRatio
            );

            instrument.startAtSampleTime(
                    src,
                    gain,
                    pan,
                    params.env,
                    false,    // Model A: one-shot burst
                    when,
                    wf,
                    grainLen
            );
        }
    }

    private void ensureCache(GestureSchedule sched, GestureGranularParams params) {
        float endMs = (sched.timesMs.length > 0) ? sched.timesMs[sched.timesMs.length - 1] : 0f;

        boolean same =
        		lastOffsetsScheduleRef == sched
                && lastScheduleSize == sched.size()
                && Float.compare(lastScheduleEndMs, endMs) == 0
                && lastOffsetsParamsRef == params;

        if (same) return;

        int n = sched.size();
        if (cachedEventOffsetsSamples.length != n) {
            cachedEventOffsetsSamples = new long[n];
        }

        float t0 = (n > 0) ? sched.timesMs[0] : 0f;
        for (int i = 0; i < n; i++) {
            float relMs = sched.timesMs[i] - t0;
            if (relMs < 0f) relMs = 0f;
            cachedEventOffsetsSamples[i] = msToSamples(relMs, sampleRate);
        }

        lastOffsetsScheduleRef = sched;
        lastScheduleSize = n;
        lastScheduleEndMs = endMs;
        lastOffsetsParamsRef = params;
    }

    // --------------------------
    // Time transform pipeline
    // --------------------------

    /**
     * @param rawSchedule
     * @param ggParams
     * @return
     */
    private static GestureSchedule applyTimeTransform(GestureSchedule rawSchedule, GestureGranularParams ggParams) {
        if (rawSchedule == null || rawSchedule.size() == 0 || ggParams == null) return rawSchedule;

        GestureSchedule schedule = rawSchedule;

        // ---- 1) Resample (if requested)
        final int targetCount = ggParams.targetCount;
        final boolean doResample =
                targetCount > 1
                && targetCount != schedule.size();

        if (doResample) {
            schedule = resampleToCount(schedule, targetCount);
        }

        // ---- 2) Duration scale (if requested)
        final float targetDur = ggParams.targetDurationMs;
        final boolean doScale =
                targetDur > 0f
                && schedule.size() > 1
                && schedule.timesMs[schedule.size() - 1] > 0f;

        if (doScale) {
            schedule = scaleToDuration(schedule, targetDur);
        }

        // ---- 3) Warp (if requested)
        final GestureGranularParams.WarpShape shape = ggParams.warpShape;
        final boolean doWarp =
                shape != null
                && shape != GestureGranularParams.WarpShape.LINEAR
                && schedule.size() > 1
                && schedule.timesMs[schedule.size() - 1] > 0f;

        if (doWarp) {
            schedule = warpScheduleTimesMs(schedule, shape, ggParams.warpExponent);
        }

        return schedule;
    }

    
    public GestureSchedule prepareSchedule(GestureSchedule rawSchedule, GestureGranularParams params) {
        if (rawSchedule == null || rawSchedule.isEmpty()) return rawSchedule;
        if (params == null || params.timeTransform == GestureGranularParams.TimeTransform.RAW_GESTURE) return rawSchedule;

        if (rawSchedule == lastRawScheduleRef && params == lastOffsetsParamsRef && lastTransformedScheduleRef != null) {
            return lastTransformedScheduleRef;
        }
   	// we need to get a new GestureSchedule and cache some references
    	GestureSchedule transformed = applyTimeTransform(rawSchedule, params);
    	lastRawScheduleRef = rawSchedule;
    	lastOffsetsParamsRef = params;
    	lastTransformedScheduleRef = transformed;
    	return transformed;
    }

    private static long msToSamples(float ms, float sr) {
        // round to nearest sample
        double s = (ms * 0.001) * sr;
        return (long) Math.max(0, Math.round(s));
    }

    /**
     * Placeholder resampler: you likely already have something better.
     * Replace this with your existing GestureScheduleBuilder logic.
     */
    private static GestureSchedule resampleToCount(GestureSchedule in, int targetCount) {
        // TODO: implement using your existing schedule resampling rules.
        // For now: naive linear index resample (keeps endpoints)
        int n = in.size();
        if (targetCount <= 1 || n <= 1) return in;

        PVector[] outPts = new PVector[targetCount];
        float[] outT = new float[targetCount];

        for (int i = 0; i < targetCount; i++) {
            float u = (targetCount == 1) ? 0f : (i / (float)(targetCount - 1));
            float x = u * (n - 1);
            int a = (int)Math.floor(x);
            int b = Math.min(n - 1, a + 1);
            float f = x - a;

            PVector pa = in.points.get(a);
            PVector pb = in.points.get(b);
            outPts[i] = new PVector(
                    lerp(pa.x, pb.x, f),
                    lerp(pa.y, pb.y, f)
            );
            outT[i] = lerp(in.timesMs[a], in.timesMs[b], f);
        }

        return new GestureSchedule(Arrays.asList(outPts), outT);
    }

    private static GestureSchedule scaleToDuration(GestureSchedule in, float targetDurationMs) {
        int n = in.size();
        if (n == 0) return in;

        float last = in.timesMs[n - 1];
        if (last <= 0f) return in;

        float scale = targetDurationMs / last;
        float[] out = new float[n];
        for (int i = 0; i < n; i++) out[i] = in.timesMs[i] * scale;
        GestureSchedule.enforceNonDecreasing(out);
        return new GestureSchedule(in.points, out);
    }

    private static GestureSchedule warpScheduleTimesMs(GestureSchedule in,
                                                      GestureGranularParams.WarpShape shape,
                                                      float exponent) {
        int n = in.size();
        if (n == 0) return in;

        float last = in.timesMs[n - 1];
        if (last <= 0f) return in;

        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            float u = (float)i / (n - 1);
            float w = warpU(u, shape, exponent);
            out[i] = w * last;
        }
        GestureSchedule.enforceNonDecreasing(out);
        return new GestureSchedule(in.points, out);
    }

    private static float warpU(float u, GestureGranularParams.WarpShape shape, float exponent) {
        u = (u < 0f) ? 0f : (u > 1f ? 1f : u);
        float e = Math.max(1e-6f, exponent);

        switch (shape) {
            case EXP:    return (float)Math.pow(u, e);
            case SQRT:   return (float)Math.pow(u, 1.0 / e);
            case CUSTOM: return easeInOutPow(u, e);
            case LINEAR:
            default:     return u;
        }
    }

    private static float easeInOutPow(float u, float exp) {
        u = (u < 0f) ? 0f : (u > 1f ? 1f : u);
        float e = Math.max(1e-6f, exp);

        if (u < 0.5f) return 0.5f * (float)Math.pow(2f * u, e);
        return 1f - 0.5f * (float)Math.pow(2f * (1f - u), e);
    }

    private static float lerp(float a, float b, float u) {
        return a + (b - a) * u;
    }
    
	private static float clampPan(float p) {
		if (p < -1f) return -1f;
		if (p >  1f) return  1f;
		return p;
	}

}