package net.paulhertz.pixelaudio.granular;

import java.util.Arrays;
import java.util.Objects;

import ddf.minim.analysis.WindowFunction;
import ddf.minim.analysis.HannWindow;

import net.paulhertz.pixelaudio.curves.GestureMapping;
import net.paulhertz.pixelaudio.schedule.GestureSchedule;
import processing.core.PVector;

/**
 * High-level facade: play a gesture schedule using a {@link PAGranularInstrument}.
 *
 * Provides:
 *   playGestureAtSampleTime(schedule, texture, mappingSnapshot, startSampleTime)
 *   playGestureNow(schedule, texture, mappingSnapshot)
 *
 * Caches derived arrays for fast retriggering.
 */
public final class PAGranularInstrumentDirector {

    /**
     * Optional hook: apply mappingSnapshot to the underlying audio source/signal.
     * For example: granSignal.setMappingSnapshot(mappingSnapshot)
     */
    @FunctionalInterface
    public interface MappingConsumer {
        void accept(int[] mappingSnapshot);
    }

    private final PAGranularInstrument instrument;   
    private final float sampleRate;
    private final GestureMapping mapping;
    private final MappingConsumer mappingConsumer;
    

    // --- cache
    private int lastScheduleSize = -1;
    private float lastScheduleEndMs = Float.NaN;
    private GestureGranularTexture lastTexture = null;
    private int lastMappingIdentity = 0; // identityHashCode of mapping snapshot

    private long[] cachedEventOffsetsSamples = new long[0];
    private int[] cachedSourceIndices = new int[0]; // optional: per-event source index (if you need it)
    
    // Default grain window (grain-level envelope).
    // Static is fine; Minim window functions are stateless in typical usage.
    private static final WindowFunction DEFAULT_GRAIN_WINDOW = new HannWindow();

    private static WindowFunction resolveGrainWindow(GestureGranularTexture texture) {
        if (texture == null) return DEFAULT_GRAIN_WINDOW;
        WindowFunction wf = texture.grainWindow; // or texture.grainWindow() if you add accessor
        return (wf != null) ? wf : DEFAULT_GRAIN_WINDOW;
    }


    /**
     * @param instrument already constructed and patched (or patch outside)
     * @param mapping point->snapshot index mapping
     * @param mappingConsumer optional hook to push mappingSnapshot into your gran signal/source
     */
    public PAGranularInstrumentDirector(PAGranularInstrument instrument,
                                       GestureMapping mapping,
                                       MappingConsumer mappingConsumer) {
        this.instrument = Objects.requireNonNull(instrument, "instrument");
        this.sampleRate = instrument.getSampleRate(); // get sample rate from instrument's reference to AudioOutput
        this.mapping = Objects.requireNonNull(mapping, "mapping");
        this.mappingConsumer = mappingConsumer;
    }

    /** Convenience: schedule relative to the instrument's current sample time. */
    public void playGestureNow(PASource src,
    		GestureSchedule schedule,
    		GestureGranularTexture texture,
    		int[] mappingSnapshot) {
    	// long now = instrument.getSampleCursor(); // or instrument.nowSampleTime()
    	long now = 0L;
    	// Optional lead-in if you want safety from UI jitter:
    	now += Math.max(0, (long)(0.005f * sampleRate)); // 5ms
    	playGestureAtSampleTime(src, schedule, texture, mappingSnapshot, now);
    }

    public void playGestureAtSampleTime(PASource src,
    		GestureSchedule schedule,
    		GestureGranularTexture texture,
    		int[] mappingSnapshot,
    		long startSampleTime) {
    	if (schedule == null || texture == null) return;
    	if (mappingSnapshot == null || mappingSnapshot.length == 0) return;
    	
        // Resolve grain window once (don’t do it per grain)
        final WindowFunction grainWf = resolveGrainWindow(texture);

    	// 0) Push mapping snapshot into the source/signal if needed (no copies)
    	if (mappingConsumer != null) {
    		mappingConsumer.accept(mappingSnapshot);
    	}

        // 1) Transform schedule times (ms domain) according to texture
        GestureSchedule sched = applyTimeTransform(schedule, texture);

        // 2) Build/reuse cached event sample offsets and optionally indices
        ensureCache(sched, texture, mappingSnapshot);

        // 3) Schedule events
        scheduleEvents(src, sched, texture, grainWf, startSampleTime);
    }

    private void scheduleEvents(PASource src,
    		GestureSchedule sched,
    		GestureGranularTexture texture,
    		WindowFunction wf,
    		long startSampleTime) {
        final int n = sched.size();
        if (n <= 0) return;

        final int grainLen = Math.max(1, texture.grainLengthSamples);
        WindowCache.INSTANCE.prewarm(wf, grainLen);
        
        // Choose cadence rule
        final boolean fixedHop = (texture.hopMode == GestureGranularTexture.HopMode.FIXED);

        for (int i = 0; i < n; i++) {
            long tEvent = fixedHop
                    ? (long)i * (long)Math.max(1, texture.hopLengthSamples)
                    : cachedEventOffsetsSamples[i];

            // burst grains: 1 == one grain per event
            for (int g = 0; g < texture.burstGrains; g++) {
                long intra = (texture.burstGrains <= 1) ? 0L : (long)g * (long)Math.max(1, texture.hopLengthSamples);
                long when = startSampleTime + tEvent + intra;

                // NOTE: this currently schedules "start a voice" only.
                // If/when you extend ScheduledPlay to include per-grain start index, you can pass it here.
                instrument.startAtSampleTime(
                        // /*src*/ instrument.getDefaultSource(), // TODO: adapt to your PASource usage
                		src,
                        /*amp*/ texture.gainLinear,
                        /*pan*/ texture.pan,
                        /*env*/ texture.env,
                        /*looping*/ texture.looping,
                        /*startSample*/ when,
                        /*window function*/ wf,
                        /*number of samples in one grain*/ grainLen
                );
            }
        }
    }

    /**
     * Ensures cachedEventOffsetsSamples[] is computed for the schedule+texture+mappingSnapshot combination.
     * Offsets are relative to the gesture start (0 at first event time).
     */
    private void ensureCache(GestureSchedule sched,
                             GestureGranularTexture texture,
                             int[] mappingSnapshot) {

        int ident = System.identityHashCode(mappingSnapshot);
        float endMs = (sched.timesMs.length > 0) ? sched.timesMs[sched.timesMs.length - 1] : 0f;

        boolean same =
                lastScheduleSize == sched.size()
                && Float.compare(lastScheduleEndMs, endMs) == 0
                && lastTexture == texture // simple identity compare; you can replace with a hash/signature
                && lastMappingIdentity == ident;

        if (same) return;

        // recompute offsets in samples
        int n = sched.size();
        if (cachedEventOffsetsSamples.length != n) {
            cachedEventOffsetsSamples = new long[n];
            cachedSourceIndices = new int[n];
        }

        float t0 = (n > 0) ? sched.timesMs[0] : 0f;
        for (int i = 0; i < n; i++) {
            float relMs = sched.timesMs[i] - t0;
            if (relMs < 0f) relMs = 0f;
            cachedEventOffsetsSamples[i] = msToSamples(relMs, sampleRate);

            // Optional: precompute a per-event mapping index (if useful later)
            PVector p = sched.points.get(i);
            int snapIdx = mapping.pointToSnapshotIndex(p, mappingSnapshot);
            cachedSourceIndices[i] = snapIdx; // NOTE: this is snapshot-index, not sample index value
        }

        lastScheduleSize = n;
        lastScheduleEndMs = endMs;
        lastTexture = texture;
        lastMappingIdentity = ident;
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
}
