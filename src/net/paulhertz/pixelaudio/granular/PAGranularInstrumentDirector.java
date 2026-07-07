/*
 *  Copyright (c) 2024 - 2025 by Paul Hertz <ignotus@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.paulhertz.pixelaudio.granular;

import java.util.Arrays;
import java.util.Objects;

import ddf.minim.analysis.WindowFunction;
import ddf.minim.analysis.HannWindow;

import net.paulhertz.pixelaudio.schedule.GestureSchedule;
import processing.core.PVector;

/**
 * Top-level director for gesture-driven granular synthesis in PixelAudio.
 *
 * <p>{@code PAGranularInstrumentDirector} is the high-level entry point most callers use
 * after they have a mono source buffer, a {@link GestureSchedule}, runtime synthesis settings,
 * and per-event buffer indices. The director prepares the schedule, creates one
 * {@link PABurstGranularSource} for each scheduled event, and delegates playback to a
 * {@link PAGranularInstrument}, which further delegates audio generation.</p>
 *
 * <p><b>Granular playback chain:</b></p>
 * <ol>
 *   <li>{@code PAGranularInstrumentDirector} receives a mono source buffer, gesture schedule,
 *   runtime synthesis parameters, and per-event parameters. It prepares event timing, resolves
 *   defaults, creates one {@link PABurstGranularSource} per gesture event, and schedules each
 *   source at an absolute sample time.</li>
 *   <li>{@link PAGranularInstrument} applies instrument-level gain, pan, and envelope defaults,
 *   then forwards immediate or scheduled playback requests to the sampler.</li>
 *   <li>{@link PAGranularSampler} owns the voice pool and sample-accurate scheduler. It starts
 *   voices immediately or when scheduled sample times arrive.</li>
 *   <li>{@link PAGranularVoice} applies the macro envelope, voice gain, pan, and lifecycle state
 *   while asking its {@link PASource} to render audio blocks.</li>
 *   <li>{@link PABurstGranularSource} renders the actual burst grains sample by sample, including
 *   source-buffer lookup, pitch-ratio playback, windowing, and overlap-add normalization.</li>
 * </ol>
 *
 * <p>This class owns the gesture-level decisions: when events occur, where each event reads
 * from the source buffer, which per-event pan/gain/pitch overrides apply, and which grain
 * window is used. {@link PABurstGranularSource} owns sample-level burst rendering for each
 * event.</p>
 *
 * @see GestureGranularParams
 * @see GestureEventParams
 * @see PABurstGranularSource
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
    
    // Performance fade state

    private static final WindowFunction DEFAULT_GRAIN_WINDOW = new HannWindow();

    private static WindowFunction resolveGrainWindow(GestureGranularParams params) {
        if (params == null) return DEFAULT_GRAIN_WINDOW;
        WindowFunction wf = params.grainWindow;
        return (wf != null) ? wf : DEFAULT_GRAIN_WINDOW;
    }

    /**
     * Creates a director for the supplied granular instrument.
     *
     * @param instrument    instrument used to schedule and render granular voices
     * @throws NullPointerException if {@code instrument} is null
     */
    public PAGranularInstrumentDirector(PAGranularInstrument instrument) {
        this.instrument = Objects.requireNonNull(instrument, "instrument");
        this.sampleRate = instrument.getSampleRate();
    }

    /**
     * Plays a gesture immediately using per-event source-buffer indices.
     *
     * <p>The start time is the instrument's current sample cursor plus a short lead-in.</p>
     *
     * @param monoBuf         mono source buffer containing audio samples
     * @param schedule        gesture schedule whose size must match {@code startIndices.length}
     * @param params          runtime granular playback parameters
     * @param startIndices    per-event start indices into {@code monoBuf}
     */
    public void playGestureNow(float[] monoBuf, GestureSchedule schedule, 
		GestureGranularParams params, int[] startIndices) {
        long now = instrument.getSampleCursor();
        now += (long)(0.005f * sampleRate); // optional lead-in (once)
        playGestureAtSampleTime(monoBuf, schedule, params, startIndices, now);
    }
    
    /**
     * Plays a gesture immediately using per-event source-buffer indices and pan overrides.
     *
     * @param monoBuf         mono source buffer containing audio samples
     * @param schedule        gesture schedule whose size must match the per-event arrays
     * @param params          runtime granular playback parameters
     * @param startIndices    per-event start indices into {@code monoBuf}
     * @param panValues       optional per-event pan values
     */
    public void playGestureNow(float[] monoBuf, GestureSchedule schedule, 
		GestureGranularParams params, int[] startIndices, float[] panValues) {
        long now = instrument.getSampleCursor();
        now += (long)(0.005f * sampleRate); // optional lead-in (once)
        playGestureAtSampleTime(monoBuf, schedule, params, startIndices, panValues, now);
    }

    /**
     * Plays a gesture immediately using a complete per-event parameter object.
     *
     * @param monoBuf     mono source buffer containing audio samples
     * @param schedule    gesture schedule whose size must match {@code evtParams.n}
     * @param params      runtime granular playback parameters
     * @param evtParams   per-event source indices and optional pan, gain, and pitch overrides
     */
    public void playGestureNow(float[] monoBuf,
            GestureSchedule schedule,
            GestureGranularParams params,
            GestureEventParams evtParams) {
        long now = instrument.getSampleCursor();
        now += (long)(0.005f * sampleRate); // optional lead-in (once)
        playGestureAtSampleTime(monoBuf, schedule, params, evtParams, now);
    }

    /**
     * Schedules a gesture at an absolute sample time using per-event source-buffer indices.
     *
     * @param monoBuf         mono source buffer containing audio samples
     * @param schedule        gesture schedule whose size must match {@code startIndices.length}
     * @param params          runtime granular playback parameters
     * @param startIndices    per-event start indices into {@code monoBuf}
     * @param startSampleTime absolute sample time for the first gesture event
     */
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

    /**
     * Schedules a gesture at an absolute sample time using source-buffer indices and pan overrides.
     *
     * @param monoBuf          mono source buffer containing audio samples
     * @param schedule         gesture schedule whose size must match the per-event arrays
     * @param params           runtime granular playback parameters
     * @param startIndices     per-event start indices into {@code monoBuf}
     * @param panValues        optional per-event pan values
     * @param startSampleTime  absolute sample time for the first gesture event
     */
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

    /**
     * Schedules a gesture at an absolute sample time using complete per-event parameters.
     *
     * <p>This method prepares the schedule according to {@code params}, validates the per-event
     * parameter count, caches event offsets in samples, and schedules one
     * {@link PABurstGranularSource} for each event.</p>
     *
     * @param monoBuf             mono source buffer containing audio samples
     * @param schedule            raw gesture schedule in milliseconds
     * @param params              runtime granular playback parameters
     * @param evtParams           per-event source indices and optional pan, gain, and pitch overrides
     * @param startSampleTime     absolute sample time for the first gesture event
     */
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
    
    /**
     * Schedules a gesture whose timing has already been transformed.
     *
     * <p>Use this overload when schedule preparation has been performed elsewhere and
     * {@code transformedSchedule.timesMs} already contains the desired event offsets.</p>
     *
     * @param monoBuf                mono source buffer containing audio samples
     * @param transformedSchedule    prepared schedule in milliseconds
     * @param params                 runtime granular playback parameters
     * @param evtParams              per-event source indices and optional pan, gain, and pitch overrides
     * @param startSampleTime        absolute sample time for the first gesture event
     */
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
    
    /**
     * Creates and schedules one burst source for each gesture event.
     *
     * @param monoBuf            mono source buffer containing audio samples
     * @param sched              prepared gesture schedule in milliseconds
     * @param params             runtime granular playback parameters
     * @param evtParams          per-event source indices and optional pan, gain, and pitch overrides
     * @param wf                 grain window function resolved from {@code params}, or the default window
     * @param startSampleTime    absolute sample time for the first gesture event
     */
    private void scheduleEvents(float[] monoBuf,
            GestureSchedule sched,
            GestureGranularParams params,
            GestureEventParams evtParams,
            WindowFunction wf,
            long startSampleTime) {

        final int n = sched.size();
        if (n <= 0) return;

        final int grainLen = Math.max(1, params.grainLengthSamples);
        final int hop      = Math.max(1, params.hopLengthSamples);

        // early model semantics (unchanged)
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

            int idx = evtParams.startIndices[i];
            if (idx < 0) idx = 0;
            if (idx >= monoBuf.length) idx = monoBuf.length - 1;

            final float pan = (evtParams.pan != null) ? clampPan(evtParams.pan[i]) : defaultPan;
            final float dynamics = (evtParams.gain != null) ? Math.max(0f, evtParams.gain[i]) : 1.0f;
            final float gain = defaultGain * dynamics;
            final float pitchRatio = (evtParams.pitchRatio != null)
                    ? Math.max(1e-6f, evtParams.pitchRatio[i])
                    : defaultPitch;

            PASource src = new PABurstGranularSource(
                    monoBuf,
                    idx,
                    grainLen,
                    burstGrains,
                    timeHop,
                    indexHop,
                    pitchRatio,
                    params.wrapAround
            );

            instrument.startAtSampleTime(
                    src,
                    gain,
                    pan,
                    params.env,
                    false,    // one-shot burst
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
     * Applies the director's schedule-time transform pipeline.
     *
     * <p>The pipeline applies, in order, optional resampling, optional duration scaling, and
     * optional timing warp. Callers normally reach this method through
     * {@link #prepareSchedule(GestureSchedule, GestureGranularParams)}.</p>
     *
     * @param rawSchedule    original gesture schedule
     * @param ggParams       runtime granular playback parameters
     * @return transformed schedule, or the input schedule when no transform applies
     */
    private static GestureSchedule applyTimeTransform(GestureSchedule rawSchedule, GestureGranularParams ggParams) {
        if (rawSchedule == null || rawSchedule.size() == 0 || ggParams == null) return rawSchedule;

        GestureSchedule schedule = rawSchedule;

        // ---- 1) Resample (if requested -- *typically* the schedule generated by PACurveMaker is already the right size)
        final int targetCount = ggParams.targetCount;
        final boolean doResample =
                targetCount > 1
                && targetCount != schedule.size();
        if (doResample) {
            schedule = resampleToCount(schedule, targetCount);
        }

        // ---- 2) Duration scale (if requested, which may be the case and is not handled by PACurveMaker)
        final float targetDur = ggParams.targetDurationMs;
        final boolean doScale =
                targetDur > 0f
                && schedule.size() > 1
                && schedule.timesMs[schedule.size() - 1] > 0f;
        if (doScale) {
            schedule = scaleToDuration(schedule, targetDur);
        }

        // ---- 3) Warp (if requested, definitely not handled by PACurveMaker)
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

    
    /**
     * Returns a schedule prepared for playback by this director.
     *
     * <p>If {@code params.timeTransform} is {@link GestureGranularParams.TimeTransform#RAW_GESTURE},
     * the raw schedule is returned unchanged. Otherwise this method applies the director's
     * transform pipeline and caches the result by raw schedule and parameter object identity.</p>
     *
     * @param rawSchedule    original gesture schedule
     * @param params         runtime granular playback parameters
     * @return prepared schedule, or {@code rawSchedule} when no transform is needed
     */
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
	
	// ------------------------------------------------------------------------
	// Access to PAGranularInstrument
    // ------------------------------------------------------------------------

    /**
     * Returns the underlying granular instrument.
     *
     * @return granular instrument used by this director
     */
	public PAGranularInstrument getInstrument() {
		return this.instrument;
	}
 
    // ------------------------------------------------------------------------
    // Performance stop and release methods
    // ------------------------------------------------------------------------
	
    /**
     * Clears pending scheduled granular events without stopping voices already playing.
     *
     * <p>For a musical stop, call this first and then call {@link #releaseAll()}.</p>
     */
    public synchronized void clearScheduled() {
        if (instrument != null) instrument.clearScheduled();
    }

    /**
     * Releases all currently playing granular voices through their envelopes.
     *
     * <p>A release is generally less abrupt than a stop, because each active voice is allowed
     * to pass through its envelope release stage.</p>
     */
    public synchronized void releaseAll() {
        if (instrument != null) instrument.releaseAll();
    }

    /**
     * Cancels pending events and stops all granular voices immediately.
     *
     * <p>A stop halts voice processing directly and is therefore more abrupt than a release.</p>
     */
    public synchronized void cancelAndStopAll() {
        if (instrument != null) instrument.cancelAndStopAll();
    }

    /**
     * Cancels pending events and releases all granular voices through their envelopes.
     *
     * <p>This is the less abrupt counterpart to {@link #cancelAndStopAll()} and is generally
     * better suited to musical fade-outs.</p>
     */
    public synchronized void cancelAndReleaseAll() {
        if (instrument != null) instrument.cancelAndReleaseAll();
    }
    
    /**
     * Counts active or releasing granular voices in the underlying instrument.
     *
     * @return active or releasing voice count
     */
    public synchronized int activeOrReleasingVoiceCount() {
        return (instrument != null) ? instrument.activeOrReleasingVoiceCount() : 0;
    }
    
}
