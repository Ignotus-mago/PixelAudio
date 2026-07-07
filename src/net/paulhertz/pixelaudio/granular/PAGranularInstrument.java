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

import ddf.minim.AudioOutput;
import ddf.minim.analysis.WindowFunction;

import net.paulhertz.pixelaudio.sampler.ADSRParams;
import net.paulhertz.pixelaudio.schedule.AudioSampleClock;
import net.paulhertz.pixelaudio.schedule.AudioUtility;

/**
 * High-level granular instrument wrapper for {@link PASource} playback.
 *
 * <p>{@code PAGranularInstrument} is the instrument-level control surface in the granular
 * synthesis chain. It applies global pan, global gain, and default envelope settings, then
 * delegates voice allocation and sample-accurate scheduling to {@link PAGranularSampler}.</p>
 *
 * <p>Most gesture-driven granular synthesis code should enter the chain through
 * {@link PAGranularInstrumentDirector}, which creates the {@link PABurstGranularSource}
 * instances and calls this class. Direct use of {@code PAGranularInstrument} is useful when
 * callers already have a {@link PASource} and want immediate or scheduled playback.</p>
 *
 * <p>This class does not manage Minim {@code MultiChannelBuffer} data. A {@link PASource}
 * encapsulates its own audio data or rendering behavior.</p>
 *
 * @see PAGranularInstrumentDirector
 * @see PAGranularSampler
 * @see PAGranularVoice
 * @see PASource
 */
public class PAGranularInstrument implements AudioSampleClock {

    // ------------------------------------------------------------------------
    // Core components
    // ------------------------------------------------------------------------
    private final PAGranularSampler sampler;
    private final AudioOutput out;

    private ADSRParams defaultEnv;

    // Global modifiers
    private float globalPan  = 0f;   // -1..+1
    private float globalGain = 1f;   // overall amplitude multiplier (linear)

    private boolean isClosed = false;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    /**
     * Creates a granular instrument.
     *
     * @param out Minim audio output used by the sampler
     * @param defaultEnv default macro envelope for voices when a playback call supplies null
     * @param maxVoices maximum number of simultaneous granular voices
     */
    public PAGranularInstrument(AudioOutput out,
                                ADSRParams defaultEnv,
                                int maxVoices) {
        this.out = out;
        this.sampler = new PAGranularSampler(out, maxVoices);
        this.defaultEnv = (defaultEnv != null)
                ? defaultEnv
                : new ADSRParams(1f, 0.01f, 0.2f, 0.8f, 0.3f);
    }

    /**
     * Creates a granular instrument with a default envelope and 16 voices.
     *
     * @param out Minim audio output used by the sampler
     */
    public PAGranularInstrument(AudioOutput out) {
        this(out, new ADSRParams(1f, 0.01f, 0.2f, 0.8f, 0.3f), 16);
    }

    // ------------------------------------------------------------------------
    // Immediate Playback API
    // ------------------------------------------------------------------------

    /**
     * Plays a granular source immediately.
     *
     * <p>The current gesture-driven granular engine normally supplies a
     * {@link PABurstGranularSource} created by {@link PAGranularInstrumentDirector}. Other
     * {@link PASource} implementations can be played directly if they satisfy the source
     * rendering contract.</p>
     *
     * @param src source to render
     * @param amp linear amplitude multiplier before global gain is applied
     * @param pan stereo pan offset in the range [-1, 1]
     * @param env envelope to use, or null to use the instrument default
     * @param looping true to loop the source path where supported
     * @return voice id, or -1 if playback could not start
     */
    public synchronized long play(PASource src,
                                  float amp,
                                  float pan,
                                  ADSRParams env,
                                  boolean looping)
    {
        if (src == null || sampler == null || isClosed) return -1;

        float finalGain = amp * globalGain;
        float finalPan = clampPan(globalPan + pan);

        ADSRParams useEnv = (env != null) ? env : defaultEnv;

        return sampler.play(src, useEnv, finalGain, finalPan, looping);
    }

    /** Convenience overload that uses the default envelope and disables looping. */
    public synchronized long play(PASource src, float amp, float pan) {
        return play(src, amp, pan, defaultEnv, false);
    }

    /** Convenience overload that uses center pan and the default envelope. */
    public synchronized long play(PASource src, float amp) {
        return play(src, amp, 0f, defaultEnv, false);
    }

    /** Convenience overload that uses unity gain, global pan, and the default envelope. */
    public synchronized long play(PASource src) {
        return play(src, 1f, globalPan, defaultEnv, false);
    }

    /** Convenience overload that enables looping with the default envelope. */
    public synchronized long playLooping(PASource src, float amp, float pan) {
        return play(src, amp, pan, defaultEnv, true);
    }

    // ------------------------------------------------------------------------
    // Scheduled Playback API
    // ------------------------------------------------------------------------

    /**
     * Schedules playback of a source at an absolute sample time.
     *
     * @param src           source to render
     * @param amp           linear amplitude multiplier before global gain is applied
     * @param pan           stereo pan offset in the range [-1, 1]
     * @param env           envelope to use, or null to use the instrument default
     * @param looping       true to loop the source path where supported
     * @param startSample   absolute sample index at which playback should start
     */
    public synchronized void startAtSampleTime(PASource src,
    		float amp,
    		float pan,
    		ADSRParams env,
    		boolean looping,
    		long startSample) {
    	if (src == null || sampler == null || isClosed) return;

    	float finalGain = amp * globalGain;
    	float finalPan = clampPan(globalPan + pan);
    	ADSRParams useEnv = (env != null) ? env : defaultEnv;

    	sampler.startAtSampleTime(src, useEnv, finalGain, finalPan, looping, startSample);
    }
    
    /**
     * Schedules playback of a source at an absolute sample time with grain-window settings.
     *
     * <p>This overload is used by {@link PAGranularInstrumentDirector} so the resolved grain
     * window and grain length can travel with a scheduled event into the sampler and voice.</p>
     *
     * @param src               source to render
     * @param amp               linear amplitude multiplier before global gain is applied
     * @param pan               stereo pan offset in the range [-1, 1]
     * @param env               envelope to use, or null to use the instrument default
     * @param looping           true to loop the source path where supported
     * @param startSample       absolute sample index at which playback should start
     * @param grainWindow       window function for shaping grain amplitude, or null for source default
     * @param grainLenSamples   number of samples in one grain
     */
    public synchronized void startAtSampleTime(PASource src,
            float amp,
            float pan,
            ADSRParams env,
            boolean looping,
            long startSample,
            WindowFunction grainWindow,
            int grainLenSamples) {

        if (src == null || sampler == null || isClosed) return;

        float finalGain = amp * globalGain;
        float finalPan  = clampPan(globalPan + pan);
        ADSRParams useEnv = (env != null) ? env : defaultEnv;

        // Delegate — sampler must carry these into the scheduled play / voice params.
        sampler.startAtSampleTime(
                src,
                useEnv,
                finalGain,
                finalPan,
                looping,
                startSample,
                grainWindow,
                Math.max(1, grainLenSamples)
        );
    }    

    /**
     * Schedules playback after a delay in samples relative to the current sampler time.
     *
     * @param src            source to render
     * @param amp            linear amplitude multiplier before global gain is applied
     * @param pan            stereo pan offset in the range [-1, 1]
     * @param env            envelope to use, or null to use the instrument default
     * @param looping        true to loop the source path where supported
     * @param delaySamples   delay from the current sample time
     */
    public synchronized void startAfterDelaySamples(PASource src,
    		float amp,
    		float pan,
    		ADSRParams env,
    		boolean looping,
    		long delaySamples) {
    	if (src == null || sampler == null || isClosed) return;

    	float finalGain = amp * globalGain;
    	float finalPan = clampPan(globalPan + pan);
    	ADSRParams useEnv = (env != null) ? env : defaultEnv;

    	sampler.startAfterDelaySamples(src, useEnv, finalGain, finalPan, looping, delaySamples);
    }

    /**
     * Schedules playback at the current instrument cursor.
     *
     * @param src       source to render
     * @param amp       linear amplitude multiplier before global gain is applied
     * @param pan       stereo pan offset in the range [-1, 1]
     * @param env       envelope to use, or null to use the instrument default
     * @param looping   true to loop the source path where supported
     */
    public synchronized void startNow(PASource src,
    		float amp,
    		float pan,
    		ADSRParams env,
    		boolean looping) {
    	startAfterDelaySamples(src, amp, pan, env, looping, 0L);
    }

    // ------------------------------------------------------------------------
    // Controls
    // ------------------------------------------------------------------------

    public void stopAll() {
        if (sampler != null) sampler.stopAll();
    }

    public void setDefaultEnvelope(ADSRParams env) {
        if (env != null) this.defaultEnv = env;
    }

    public ADSRParams getDefaultEnvelope() { return defaultEnv; }

    public void setGlobalPan(float pan) { this.globalPan = clampPan(pan); }

    public float getGlobalPan() { return globalPan; }

    public void setGlobalGain(float gLinear) { this.globalGain = gLinear; }

    public float getGlobalGain() { return globalGain; }
    
    public void setGlobalGainDb(float gDb) { this.globalGain = AudioUtility.dbToLinear(gDb); }

    public float getGlobalGainDb() { return AudioUtility.linearToDb(globalGain); }
    
    @Override
    public float getSampleRate() { return this.out.sampleRate(); }

    public PAGranularSampler getSampler() { return sampler; }
    
    @Override
    public long getCurrentSampleTime() {
        return sampler.getCurrentSampleTime();
    }

    public long getSampleCursor() {
        return getCurrentSampleTime();
    }

    // ------------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------------

    /**
     * Closes this instrument and stops all voices.
     */
    public synchronized void close() {
        if (isClosed) return;
        isClosed = true;
        stopAll();
        // sampler is a UGen patched to out — optionally leave patch on
    }
    
    // stop or release
    
    /**
     * Clears playback events that have been scheduled but have not started yet.
     *
     * <p>Voices that are already active continue playing. For a musical stop, call this first
     * and then call {@link #releaseAll()}.</p>
     */
    public void clearScheduled() {
        if (sampler != null) sampler.clearScheduled();
    }

    /**
     * Releases all active or already-releasing voices through their envelopes.
     *
     * <p>A release is generally less abrupt than a stop, because each active voice is allowed
     * to pass through its envelope release stage. Scheduled events are not cleared by this
     * method.</p>
     */
    public void releaseAll() {
        if (sampler != null) sampler.releaseAll();
    }

    /**
     * Clears pending scheduled events and immediately stops all active voices.
     *
     * <p>A stop halts voice processing directly and is therefore more abrupt than a release.</p>
     */
    public void cancelAndStopAll() {
        if (sampler != null) sampler.cancelAndStopAll();
    }

    /**
     * Clears pending scheduled events and releases all active voices through their envelopes.
     *
     * <p>This is the less abrupt counterpart to {@link #cancelAndStopAll()} and is generally
     * better suited to musical fade-outs.</p>
     */
    public void cancelAndReleaseAll() {
        if (sampler != null) sampler.cancelAndReleaseAll();
    }
    
    /**
     * Counts active or releasing granular voices.
     *
     * @return active or releasing voice count
     */
    public int activeOrReleasingVoiceCount() {
        return (sampler != null) ? sampler.activeOrReleasingVoiceCount() : 0;
    }

    
    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    private static float clampPan(float p) {
        if (p < -1f) return -1f;
        if (p > 1f) return 1f;
        return p;
    }
    
}
