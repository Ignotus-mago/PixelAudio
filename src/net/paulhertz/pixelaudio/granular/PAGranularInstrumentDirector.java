
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
    private GestureGranularTexture lastTexture = null;
    private long[] cachedEventOffsetsSamples = new long[0];

    private static final WindowFunction DEFAULT_GRAIN_WINDOW = new HannWindow();

    private static WindowFunction resolveGrainWindow(GestureGranularTexture texture) {
        if (texture == null) return DEFAULT_GRAIN_WINDOW;
        WindowFunction wf = texture.grainWindow;
        return (wf != null) ? wf : DEFAULT_GRAIN_WINDOW;
    }

    public PAGranularInstrumentDirector(PAGranularInstrument instrument) {
        this.instrument = Objects.requireNonNull(instrument, "instrument");
        this.sampleRate = instrument.getSampleRate();
    }

    public void playGestureNow(float[] monoBuf, GestureSchedule schedule, 
		GestureGranularTexture texture, int[] startIndices) {
        long now = instrument.getSampleCursor();
        now += (long)(0.005f * sampleRate); // optional lead-in (once)
        playGestureAtSampleTime(monoBuf, schedule, texture, startIndices, now);
    }

    public void playGestureAtSampleTime(float[] monoBuf, GestureSchedule schedule,
            GestureGranularTexture texture, int[] startIndices, long startSampleTime) {
        if (monoBuf == null || monoBuf.length == 0) return;
        if (schedule == null || texture == null) return;
        if (startIndices == null || startIndices.length == 0) return;

        final WindowFunction grainWf = resolveGrainWindow(texture);

        // 1) Transform schedule times (ms domain) according to texture
        GestureSchedule sched = applyTimeTransform(schedule, texture);
        final int n = sched.size();
        if (n <= 0) return;
        if (startIndices.length < n) return; // or clamp n to min(n, startIndices.length)

        // 2) Cache offsets
        ensureCache(sched, texture);

        // 3) Schedule Model-A events (Director creates sources)
        scheduleEvents(monoBuf, startIndices, sched, texture, grainWf, null, startSampleTime);
    }    
    
    // include a per-grain pan array
    public void playGestureNow(float[] monoBuf, GestureSchedule schedule, 
		GestureGranularTexture texture, int[] startIndices, float[] panValues) {
        long now = instrument.getSampleCursor();
        now += (long)(0.005f * sampleRate); // optional lead-in (once)
        playGestureAtSampleTime(monoBuf, schedule, texture, startIndices, panValues, now);
    }

    // include a per-grain pan array
    public void playGestureAtSampleTime(float[] monoBuf, GestureSchedule schedule,
            GestureGranularTexture texture, int[] startIndices, float[] panValues, long startSampleTime) {
        if (monoBuf == null || monoBuf.length == 0) return;
        if (schedule == null || texture == null) return;
        if (startIndices == null || startIndices.length == 0) return;

        final WindowFunction grainWf = resolveGrainWindow(texture);

        // 1) Transform schedule times (ms domain) according to texture
        GestureSchedule sched = applyTimeTransform(schedule, texture);
        final int n = sched.size();
        if (n <= 0) return;
        if (startIndices.length < n) return; // or clamp n to min(n, startIndices.length)

        // 2) Cache offsets
        ensureCache(sched, texture);

        // 3) Schedule Model-A events (Director creates sources)
        scheduleEvents(monoBuf, startIndices, sched, texture, grainWf, panValues, startSampleTime);
    }

    private void scheduleEvents(float[] monoBuf, int[] startIndices, GestureSchedule sched,
            GestureGranularTexture texture, WindowFunction wf, float[] panValues, long startSampleTime) {
        final int n = sched.size();
        if (n <= 0) return;
        final int grainLen = Math.max(1, texture.grainLengthSamples);
        final int hop = Math.max(1, texture.hopLengthSamples);

        // semantics (Model A):
        // - Each schedule event i becomes one *burst event* starting at:
        //   - GESTURE: event time from sched.timesMs (after timeTransform), converted to samples
        //   - FIXED:   event time = i * hop samples
        // - Within each burst: grains are spaced by hop in time (timeHop) and scanned by hop in index (indexHop).
        final int burstGrains = Math.max(1, texture.burstGrains);
        final int timeHop = hop;
        final int indexHop = hop;
        final float pitchRatio = (texture.pitchRatio > 0f) ? texture.pitchRatio : 1.0f;
		// prewarm window curve (avoid first-hit allocation)
        if (wf != null && grainLen > 1) {
            WindowCache.INSTANCE.prewarm(wf, grainLen);
        }
        final boolean fixedHop = (texture.hopMode == GestureGranularTexture.HopMode.FIXED);

		// step through the schedule, scheduling a burst event for each gesture event time
        for (int i = 0; i < n; i++) {
            final long tEvent = fixedHop ? (long)i * (long)hop : cachedEventOffsetsSamples[i];
            final long when = startSampleTime + tEvent;
            int idx = startIndices[i];
            if (idx < 0) idx = 0;
            if (idx >= monoBuf.length) idx = monoBuf.length - 1;
            float pan = texture.pan;
            if (panValues != null && panValues.length > i) {
                pan = clampPan(panValues[i]);
            }
			// create a burst granular source for this event
            PASource src = new PABurstGranularSource(
                    monoBuf,
                    idx,
                    grainLen,
                    burstGrains,
                    timeHop,
                    indexHop,
                    pitchRatio
            );
			// call PAGranularInstrument to schedule the event at the computed sample time
            instrument.startAtSampleTime(
                    src,
                    texture.gainLinear,
                    pan,
                    texture.env,
                    false,    // Model A: one-shot event/burst
                    when,
                    wf,
                    grainLen
            );
        }
    }

    private void ensureCache(GestureSchedule sched, GestureGranularTexture texture) {
        float endMs = (sched.timesMs.length > 0) ? sched.timesMs[sched.timesMs.length - 1] : 0f;

        boolean same =
                lastScheduleSize == sched.size()
                && Float.compare(lastScheduleEndMs, endMs) == 0
                && lastTexture == texture;

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

        lastScheduleSize = n;
        lastScheduleEndMs = endMs;
        lastTexture = texture;
    }

    // --------------------------
    // Time transform pipeline
    // --------------------------

    private static GestureSchedule applyTimeTransform(GestureSchedule in, GestureGranularTexture tex) {
        if (in == null || in.size() == 0) return in;

        switch (tex.timeTransform) {
            case RAW_GESTURE:
                return in;

            case RESAMPLED_COUNT:
                if (tex.targetCount > 1) {
                    return resampleToCount(in, tex.targetCount);
                }
                return in;

            case DURATION_SCALED:
                if (tex.targetDurationMs > 0f) {
                    return scaleToDuration(in, tex.targetDurationMs);
                }
                return in;

            case WARPED:
                // Common practice: if targetDurationMs is set, scale first; then warp
                GestureSchedule s = in;
                if (tex.targetDurationMs > 0f) {
                    s = scaleToDuration(s, tex.targetDurationMs);
                }
                if (tex.warpShape != null && tex.warpShape != GestureGranularTexture.WarpShape.LINEAR) {
                    s = warpScheduleTimesMs(s, tex.warpShape, tex.warpExponent);
                } else {
                    // LINEAR warp does nothing
                }
                return s;

            default:
                return in;
        }
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
                                                      GestureGranularTexture.WarpShape shape,
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

    private static float warpU(float u, GestureGranularTexture.WarpShape shape, float exponent) {
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