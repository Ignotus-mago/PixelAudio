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

package net.paulhertz.pixelaudio.sampler;

/**
 * Holds information about a triggered playback event.
 * Returned by {@code PASharedBufferSampler.play()}, where it can be used for debugging. 
 */
public class PlaybackInfo {
    /** Unique sampler voice identifier. */
    public final long voiceId;

    /** Duration in output samples (pitch-independent). */
    public final int eventSamples;

    /** Duration in milliseconds. */
    public final float durationMS;

    /** Actual number of buffer samples traversed (pitch-dependent). */
    public final float bufferReadSamples;

    /** Whether this event loops indefinitely. */
    public final boolean looping;

    /** Absolute start time of the event, in samples (optional). */
    public final long startSample;

    /** Absolute stop time of the event, in samples (optional). */
    public final long stopSample;

    /** Absolute start time in milliseconds (optional). */
    public final float startMS;

    /** Absolute stop time in milliseconds (optional). */
    public final float stopMS;

    /**
     * Creates playback information for a triggered voice.
     *
     * @param voiceId             unique sampler voice identifier
     * @param eventSamples        duration in output samples
     * @param bufferReadSamples   number of source buffer samples traversed
     * @param durationMS          duration in milliseconds
     * @param looping             true when the voice loops indefinitely
     * @param startSample         absolute start time in samples
     * @param sampleRate          output sample rate in Hz
     */
    public PlaybackInfo(
            long voiceId,
            int eventSamples,
            float bufferReadSamples,
            float durationMS,
            boolean looping,
            long startSample,
            float sampleRate)
    {
        this.voiceId = voiceId;
        this.eventSamples = eventSamples;
        this.durationMS = durationMS;
        this.bufferReadSamples = bufferReadSamples;
        this.looping = looping;
        this.startSample = startSample;
        this.stopSample = looping ? Long.MAX_VALUE : startSample + eventSamples;
        this.startMS = startSample / sampleRate * 1000f;
        this.stopMS = looping ? Float.POSITIVE_INFINITY : this.stopSample / sampleRate * 1000f;
    }
    
    /**
     * Computes total playback duration (in output samples, pitch-independent).
     * Envelope stage times are assumed to be in seconds.
     *
     * @param samplePos    buffer index to start playback
     * @param sampleLen    requested duration in samples
     * @param bufferLen    source buffer length in samples
     * @param pitch        pitch or playback-rate multiplier
     * @param env          optional ADSR envelope
     * @param looping      true when playback loops indefinitely
     * @param sampleRate   output sample rate in Hz
     * @return duration in output samples (int), or Integer.MAX_VALUE if looping.
     */
    public static int computeVoiceDuration(
    		int samplePos,
    		int sampleLen,
    		int bufferLen,
    		float pitch,
    		ADSRParams env,
    		boolean looping,
    		float sampleRate) {
    	return computeVoiceDuration(samplePos, sampleLen, bufferLen, pitch, env, looping, sampleRate, false);
    }
    
    /**
     * Computes total playback duration (in output samples) with optional source-buffer wrapping.
     *
     * @param samplePos    buffer index to start playback
     * @param sampleLen    requested duration in samples
     * @param bufferLen    source buffer length in samples
     * @param pitch        pitch or playback-rate multiplier
     * @param env          optional ADSR envelope
     * @param looping      true when playback loops indefinitely
     * @param sampleRate   output sample rate in Hz
     * @param wrapAround   true to preserve requested duration across source-buffer boundaries
     * @return duration in output samples (int), or Integer.MAX_VALUE if looping.
     */
    public static int computeVoiceDuration(
    		int samplePos,
    		int sampleLen,
    		int bufferLen,
    		float pitch,
    		ADSRParams env,
    		boolean looping,
    		float sampleRate,
    		boolean wrapAround) {
    	if (looping) return Integer.MAX_VALUE;

    	// Clamp to buffer bounds
    	samplePos = Math.max(0, samplePos);
    	if (sampleLen < 0) sampleLen = 0;
    	if (!wrapAround && samplePos + sampleLen > bufferLen) {
    		sampleLen = Math.max(0, bufferLen - samplePos);
    	}

    	// Base playback time in samples (pitch-independent)
    	float baseSamples = sampleLen;

    	// Envelope duration (attack + decay + release) in seconds → samples
    	float envSamples = 0f;
    	if (env != null) {
    		envSamples = (env.getAttack() + env.getDecay() + env.getRelease()) * sampleRate;
    	}

    	float totalSamples = baseSamples + envSamples;
    	return (int) totalSamples;
    }


    @Override
    public String toString() {
        return String.format(
            "[Voice %d] dur=%d samples (%.2f ms), bufferRead≈%.0f, start=%.2f ms stop=%.2f ms looping=%b",
            voiceId, eventSamples, durationMS, bufferReadSamples, startMS, stopMS, looping
        );
    }
}
