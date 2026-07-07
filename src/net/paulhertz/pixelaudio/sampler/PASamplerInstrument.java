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

import java.util.Arrays;

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;
import net.paulhertz.pixelaudio.schedule.AudioSampleClock;
import net.paulhertz.pixelaudio.schedule.AudioUtility;

/**
 * Represents a playable instrument that uses a PASampler instantiated as a PASharedBufferSampler
 * to trigger audio playback from a shared buffer that is (currently) a Minim MultiChannelBuffer. 
 * <p>
 * TODO decide if MultiChannelBuffer or float[] is the appropriate structure for storing the buffer. 
 *     float[] is preferred for the general style of PixelAudio, but MultiChannelBuffer support 
 *     is a useful consideration for the future. 
 *     DONE We are sticking with MultiChannelBuffer but may provide a future abstraction that 
 *     works with float[] arrays and provide the desired sample format for each synth. 
 * </p>
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>Global pitch scaling.</li>
 *   <li>Global stereo pan.</li>
 *   <li>Default ADSR envelope.</li>
 *   <li>Linear trim (gain) per instrument.</li>
 *   <li>Compensation for differing buffer vs. output sample rates.</li>
 *   <li>Cached buffer size for efficiency.</li>
 * </ul>
 *
 * Implements both PAPlayable and PASamplerPlayable for full compatibility.
 */
public class PASamplerInstrument implements PASamplerPlayable, AudioSampleClock {

	// ------------------------------------------------------------------------
	// Core fields
	// ------------------------------------------------------------------------
	/** Sampler backend used to trigger playback. */
	private final PASampler sampler;
	/** Source buffer for this instrument. */
	private MultiChannelBuffer buffer;
	/** Audio output this instrument is patched to. */
	private final AudioOutput out;
	/** Cached source buffer size in samples. TODO consider replacing with buffer.getBufferSize(). */
	private int bufferSize;
	/** Maximum number of simultaneous voices for the sampler backend. */
	private int maxVoices;

	// Sample rate information
	/** Sample rate of the source from which the buffer was loaded. */
	private float bufferSampleRate;
	/** Sample rate of the AudioOutput. */
	private float outputSampleRate;
	/** Buffer-rate to output-rate ratio, used to correct playback speed. */
	private float sampleRateRatio;

	/** Default ADSR envelope for playback. */
	private ADSRParams defaultEnv;

	// Global modifiers
	/** Global pitch multiplier. */
	private volatile float globalPitch = 1.0f;
	/** Global stereo pan offset, from -1.0 left to 1.0 right. */
	private float globalPan = 0.0f;
	
	/** Per-instrument linear trim. */
	private volatile float gain = 1f;
	/** Parent pool gain, set by PASamplerInstrumentPool. */
	private volatile float parentGain = 1f;


	// ------------------------------------------------------------------------
	// Constructors
	// ------------------------------------------------------------------------

	/**
	 * Primary constructor for backward compatibility.
	 *
	 * @param buffer          the shared MultiChannelBuffer
	 * @param sampleRate      nominal sample rate of the buffer (Hz)
	 * @param maxVoices       number of simultaneous voices (polyphony)
	 * @param audioOut        target AudioOutput
	 * @param env             default ADSR envelope parameters
	 */
	public PASamplerInstrument(MultiChannelBuffer buffer, float sampleRate, int maxVoices,  AudioOutput audioOut, ADSRParams env) {
		this.out = audioOut;
		this.buffer = buffer;
		this.bufferSize = buffer.getBufferSize();
		this.bufferSampleRate = sampleRate;
		this.outputSampleRate = (audioOut != null) ? audioOut.sampleRate() : sampleRate;
		this.sampleRateRatio = (outputSampleRate > 0f) ? bufferSampleRate / outputSampleRate : 1.0f;
		this.defaultEnv = (env != null) ? env : new ADSRParams(1f, 0.01f, 0.2f, 0.8f, 0.3f);
		this.globalPitch = 1.0f;
		this.globalPan = 0.0f;
		this.isClosed = false;
		this.maxVoices = Math.max(1, maxVoices);
		// pass through to sampler
		this.sampler = new PASharedBufferSampler(buffer, sampleRate, audioOut, this.maxVoices);
		syncMasterGain();
	}

	/**
	 * Full constructor with explicit buffer sample rate, custom sampler, and envelope.
	 *
	 * @param buffer             shared MultiChannelBuffer
	 * @param out                target AudioOutput
	 * @param sampler            sampler implementation to use
	 * @param defaultEnv         default ADSR envelope
	 * @param bufferSampleRate   sample rate of the source buffer in Hz
	 */
	public PASamplerInstrument(MultiChannelBuffer buffer, AudioOutput out, PASampler sampler, ADSRParams defaultEnv, float bufferSampleRate) {
		this.out = out;
		this.buffer = buffer;
		this.sampler = sampler;
		syncMasterGain();
		this.defaultEnv = defaultEnv;
		this.bufferSize = (buffer != null) ? buffer.getBufferSize() : 0;
		this.outputSampleRate = (out != null) ? out.sampleRate() : bufferSampleRate;
		this.bufferSampleRate = bufferSampleRate;
		this.sampleRateRatio = (outputSampleRate > 0f) ? bufferSampleRate / outputSampleRate : 1f;
	}

	/**
	 * Convenience constructor assuming buffer and output share the same rate.
	 *
	 * @param buffer   shared MultiChannelBuffer
	 * @param out target AudioOutput
	 */
	public PASamplerInstrument(MultiChannelBuffer buffer, AudioOutput out) {
		this(buffer, 
			 out, 
			 new PASharedBufferSampler(buffer, out.sampleRate(), out),
			 new ADSRParams(1f, 0.01f, 0.2f, 0.8f, 0.3f),
			 out.sampleRate());
	}

	// ------------------------------------------------------------------------
	// Interface Implementations
	// ------------------------------------------------------------------------

	/** 
	 * Generic play() from PAPlayable, will play the entire buffer from the beginning of the buffer
	 * with specified pitch and pan and default envelope. 
	 */
	@Override
	public int play(float amplitude, float pitch, float pan) {
		if (sampler == null || bufferSize <= 0) return 0;
		float scaledPitch = pitch * globalPitch * sampleRateRatio;
		float finalPan = clampPan(globalPan + pan);
		return sampler.play(0, bufferSize, amplitude, defaultEnv, scaledPitch, finalPan);
	}

	/**
	 * The primary play method, called by all playSample() methods, from PASamplerPlayable interface.
	 */
	@Override
	public int play(int samplePos, int sampleLen, float amplitude,
			ADSRParams env, float pitch, float pan) {
		if (sampler == null || bufferSize <= 0) return 0;
		float scaledPitch = pitch * globalPitch * sampleRateRatio;
		float finalPan = clampPan(globalPan + pan);
		ADSRParams useEnv = (env != null) ? env : defaultEnv;
		return sampler.play(samplePos, sampleLen, amplitude, useEnv, scaledPitch, finalPan);
	}

	// ------------------------------------------------------------------------
	// Scheduled Playback API
	// ------------------------------------------------------------------------

	/**
	 * Schedules playback of a buffer region at an absolute sample time on this sampler's clock.
	 *
	 * @param samplePos    buffer index to start playback
	 * @param sampleLen    requested duration in samples
	 * @param amplitude    gain multiplier
	 * @param env          ADSR envelope parameters, or null to use the default
	 * @param pitch        pitch or playback-rate multiplier
	 * @param pan          stereo pan
	 * @param startSample  absolute sample index at which playback should start
	 */
	public synchronized void startAtSampleTime(int samplePos, int sampleLen, float amplitude,
			ADSRParams env, float pitch, float pan, long startSample) {
		if (sampler == null || bufferSize <= 0) return;
		float scaledPitch = pitch * globalPitch * sampleRateRatio;
		float finalPan = clampPan(globalPan + pan);
		ADSRParams useEnv = (env != null) ? env : defaultEnv;
		sampler.startAtSampleTime(samplePos, sampleLen, amplitude, useEnv, scaledPitch, finalPan, startSample);
	}

	/**
	 * Schedules playback after a delay in samples relative to this sampler's current clock.
	 *
	 * @param samplePos      buffer index to start playback
	 * @param sampleLen      requested duration in samples
	 * @param amplitude      gain multiplier
	 * @param env            ADSR envelope parameters, or null to use the default
	 * @param pitch          pitch or playback-rate multiplier
	 * @param pan            stereo pan
	 * @param delaySamples   delay from the current sample time
	 */
	public synchronized void startAfterDelaySamples(int samplePos, int sampleLen, float amplitude,
			ADSRParams env, float pitch, float pan, long delaySamples) {
		if (sampler == null || bufferSize <= 0) return;
		float scaledPitch = pitch * globalPitch * sampleRateRatio;
		float finalPan = clampPan(globalPan + pan);
		ADSRParams useEnv = (env != null) ? env : defaultEnv;
		sampler.startAfterDelaySamples(samplePos, sampleLen, amplitude, useEnv, scaledPitch, finalPan, delaySamples);
	}

	/** Schedules playback at the current sampler clock. */
	public synchronized void startNow(int samplePos, int sampleLen, float amplitude,
			ADSRParams env, float pitch, float pan) {
		startAfterDelaySamples(samplePos, sampleLen, amplitude, env, pitch, pan, 0L);
	}

	/** @return this sampler's current audio-thread sample clock */
	@Override
	public synchronized long getCurrentSampleTime() {
		return (sampler != null) ? sampler.getCurrentSampleTime() : 0L;
	}

	/** Clears scheduled starts that have not fired yet. */
	public synchronized void clearScheduled() {
		if (sampler != null) sampler.clearScheduled();
	}

	/** Stop playback (stop all active voices). */
	@Override
	public void stop() {
		if (sampler != null) sampler.stopAll();
	}
	
	/** 
	 * Smoothly stop all active voices by triggering their envelope release.
	 * This is the preferred performance stop.
	 */
	public void fadeToStop() {
		releaseAllVoices();
	}

	/**
	 * Alias for fadeToStop(), useful when calling from UI or pool code.
	 */
	public void fadeOutAll() {
		fadeToStop();
	}	

	// ------------------------------------------------------------------------
	// Core playback - legacy playSample() overloads
    //
    // Standard argument order: 
    // (int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch, float pan)
    //
	// ------------------------------------------------------------------------

	/**
	 * Trigger playback using all six standard per-voice parameters.
	 *
	 * @param samplePos    start position (samples)
	 * @param sampleLen    playback length (samples)
	 * @param amplitude    per-voice amplitude
	 * @param env          ADSR envelope parameters
	 * @param pitch        playback rate (1.0 = normal)
	 * @param pan          stereo position (-1.0 = left, +1.0 = right)
	 * @return the actual length of the audio event, in samples
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude,
			ADSRParams env, float pitch, float pan) {
		return play(samplePos, sampleLen, amplitude, env, pitch, pan);
	}

	/**
	 * All params except the envelope, so we use the default envelope. 
	 * 
	 * @param samplePos    buffer index to start playback
	 * @param sampleLen    requested duration in samples
	 * @param amplitude    gain multiplier
	 * @param pitch        pitch or playback-rate multiplier
	 * @param pan          stereo pan
	 * @return the actual length of the audio event, in samples
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude, 
			float pitch, float pan) {
		return play(samplePos, sampleLen, amplitude, defaultEnv, pitch, pan);
	}

	/**
	 * Convenience overload: uses default envelope, default pitch, and center pan.
	 *
	 * @param samplePos   buffer index to start playback
	 * @param sampleLen   requested duration in samples
	 * @param amplitude   gain multiplier
	 * @return actual event duration in samples
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude) {
		return play(samplePos, sampleLen, amplitude, defaultEnv, globalPitch, globalPan);
	}

	/**
	 * Convenience overload: uses default envelope, supplied pitch and center pan.
	 *
	 * @param samplePos   buffer index to start playback
	 * @param sampleLen   requested duration in samples
	 * @param amplitude   gain multiplier
	 * @param pitch       pitch or playback-rate multiplier
	 * @return actual event duration in samples
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude, float pitch) {
		return play(samplePos, sampleLen, amplitude, defaultEnv, pitch, globalPan);
	}

	/**
	 * Plays a sample using a supplied envelope with default pitch and default pan.
	 *
	 * @param samplePos   buffer index to start playback
	 * @param sampleLen   requested duration in samples
	 * @param amplitude   gain multiplier
	 * @param env         optional ADSR envelope
	 * @return actual event duration in samples
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
		return play(samplePos, sampleLen, amplitude, env, globalPitch, globalPan);
	}

	/**
	 * Plays a sample using a supplied envelope, pitch, and current global pan.
	 *
	 * @param samplePos   buffer index to start playback
	 * @param sampleLen   requested duration in samples
	 * @param amplitude   gain multiplier
	 * @param env         optional ADSR envelope
	 * @param pitch       pitch or playback-rate multiplier
	 * @return actual event duration in samples
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch) 	{
		return play(samplePos, sampleLen, amplitude, env, pitch, globalPan);
	}

	/**
	 * Sets a new buffer for PASamplerInstrument and plays it with envelope, pitch, and pan.
	 * For large buffers, there may be some latency. 
	 *
	 * @param buffer      source buffer for playback
	 * @param samplePos   buffer index to start playback
	 * @param sampleLen   requested duration in samples
	 * @param amplitude   gain multiplier
	 * @param env         optional ADSR envelope
	 * @param pitch       pitch or playback-rate multiplier
	 * @param pan         stereo pan
	 * @return actual event duration in samples
	 */
	public synchronized int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
			float amplitude, ADSRParams env, float pitch, float pan) {
		if (buffer != null) this.setBuffer(buffer);
		return play(samplePos, sampleLen, amplitude, env, pitch, pan);
	}

	/**
	 * Sets a new buffer for PASamplerInstrument and plays it with envelope, pitch, and default pan.
	 *
	 * @param buffer      source buffer for playback
	 * @param samplePos   buffer index to start playback
	 * @param sampleLen   requested duration in samples
	 * @param amplitude   gain multiplier
	 * @param env         optional ADSR envelope
	 * @param pitch       pitch or playback-rate multiplier
	 * @return actual event duration in samples
	 */
	public synchronized int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
			float amplitude, ADSRParams env, float pitch) {
		if (buffer != null) this.setBuffer(buffer);
		return play(samplePos, sampleLen, amplitude, env, pitch, globalPan);
	}

	// ------------------------------------------------------------------------
	// Accessors and utilities
	// ------------------------------------------------------------------------
	
	/**
	 * Sets per-instrument output gain.
	 *
	 * @param linear   linear gain value
	 */
	public void setGain(float linear) {
	    if (Float.isNaN(linear) || Float.isInfinite(linear)) return;
	    gain = Math.max(0f, linear);
	    syncMasterGain();
	}

	/** @return per-instrument output gain as a linear value */
	public float getGain() { return gain; }

	/**
	 * Sets per-instrument output gain in decibels.
	 *
	 * @param db   gain in decibels
	 */
	public void setGainDb(float db) { setGain(AudioUtility.dbToLinear(db)); }

	/** @return per-instrument output gain in decibels */
	public float getGainDb() { return AudioUtility.linearToDb(gain); }

	// package-private: only the pool should call this
	void setParentGain(float linear) {
	    if (Float.isNaN(linear) || Float.isInfinite(linear)) return;
	    parentGain = Math.max(0f, linear);
	    syncMasterGain();
	}

	float getParentGain() { return parentGain; }

	private void syncMasterGain() {
	    if (sampler instanceof PASharedBufferSampler s) {
	        s.setMasterGain(gain * parentGain);
	    }
	}

	/**
	 * Pass-through to the underlying PASharedBufferSampler mix behavior.
	 * This lets a host application tune density normalization / soft clipping
	 * without changing per-event gain or envelope settings.
	 *
	 * @param profile   a PASharedBufferSampler mix profile
	 */
	public void setMixProfile(PASharedBufferSampler.MixProfile profile) {
	    if (sampler instanceof PASharedBufferSampler sam && profile != null) {
	        sam.setMixProfile(profile);
	    }
	}

	/**
	 * @return the current PASharedBufferSampler mix profile, or BALANCED if the
	 *         sampler implementation is not PASharedBufferSampler.
	 */
	public PASharedBufferSampler.MixProfile getMixProfile() {
	    if (sampler instanceof PASharedBufferSampler sam) {
	        return sam.getMixProfile();
	    }
	    return PASharedBufferSampler.MixProfile.BALANCED;
	}

	/**
	 * Convenience: cycle to the next available sampler mix profile.
	 *
	 * @return the newly selected profile
	 */
	public PASharedBufferSampler.MixProfile cycleMixProfile() {
	    PASharedBufferSampler.MixProfile next = PASharedBufferSampler.MixProfile.BALANCED;
	    if (sampler instanceof PASharedBufferSampler s) {
	        PASharedBufferSampler.MixProfile[] vals = PASharedBufferSampler.MixProfile.values();
	        int i = (s.getMixProfile().ordinal() + 1) % vals.length;
	        next = vals[i];
	        s.setMixProfile(next);
	    }
	    return next;
	}

	/**
	 * Reports whether this instrument has an available voice.
	 *
	 * @return true if at least one voice is available
	 */
	public boolean hasAvailableVoice() {
	    if (sampler == null) return false;
	    for (PASamplerVoice v : ((PASharedBufferSampler) sampler).getVoices()) {
	        if (v.isFinished() || (!v.isActive() && !v.isReleasing())) {
	            return true;
	        }
	    }
	    return false;
	}

	// --- Helpers for pool orchestration (non-invasive; read-only) ---
	/**
	 * Reports whether any voice is active or releasing.
	 *
	 * @return true when one or more voices are active or releasing
	 */
	public boolean hasActiveOrReleasingVoices() {
	    PASharedBufferSampler s = (PASharedBufferSampler) getSampler();
	    for (PASamplerVoice v : s.getVoices()) {
	        if (v.isActive() || v.isReleasing()) return true;
	    }
	    return false;
	}

	/**
	 * Counts active or releasing voices.
	 *
	 * @return active or releasing voice count
	 */
	public int activeOrReleasingVoiceCount() {
	    PASharedBufferSampler s = (PASharedBufferSampler) getSampler();
	    int c = 0;
	    for (PASamplerVoice v : s.getVoices()) {
	        if (v.isActive() || v.isReleasing()) c++;
	    }
	    return c;
	}

	/** Smoothly release all active voices (used only if we must recycle an instrument). */
	public void releaseAllVoices() {
		if (sampler != null) sampler.releaseAll();
	}

	/** @return sampler implementation used by this instrument */
	public PASampler getSampler() { return sampler; }
		
	/** @return current source buffer */
	public MultiChannelBuffer getBuffer() { return buffer; }
	
	/** @return current source buffer size in samples */
	public int getBufferSize() { return bufferSize; }
	
	/**
	 * Copy the data and settings in the provided MultiChannelBuffer to this.buffer; 
	 * bufferSampleRate is unchanged. The sampler and its voices are also refreshed.
	 * Note that bufferSize will change if the buffers are not the same size.
	 * 
	 * @param newBuffer    buffer to replace the current buffer
	 */
	public synchronized void setBuffer(MultiChannelBuffer newBuffer) {
	    if (newBuffer != null) {
	    	// MultiChannelBuffer.set() copies newBuffer data and resizes buffer and channel count, if necessary
	    	// We call it to avoid reallocating memory and copy sample data efficiently.
	        this.buffer.set(newBuffer);
	        this.bufferSize = newBuffer.getBufferSize();
	        float[] sharedBuffer = Arrays.copyOf(newBuffer.getChannel(0), newBuffer.getBufferSize());
	        sampler.setBuffer(sharedBuffer);
	    }
	}
	
	/**
	 * Copy the data and settings of the provided MultiChannelBuffer to this.buffer 
	 * and set bufferSampleRate. The sampler and its voices are also refreshed.
	 * Note that bufferSize will change if the buffers are not the same size.
	 * 
	 * @param newBuffer    buffer to replace the current buffer
	 * @param newSampleRate sample rate of the replacement buffer in Hz
	 */
	public synchronized void setBuffer(MultiChannelBuffer newBuffer, float newSampleRate) {
	    if (newBuffer != null) {
	    	// MultiChannelBuffer.set() copies newBuffer data and resizes buffer and channel count, if necessary
	    	// We call it to avoid reallocating memory and copy sample data efficiently.
	    	this.buffer.set(newBuffer);
	    	this.bufferSize = newBuffer.getBufferSize();
	    	setBufferSampleRate(newSampleRate);
	        float[] sharedBuffer = Arrays.copyOf(newBuffer.getChannel(0), newBuffer.getBufferSize());
	        sampler.setBuffer(sharedBuffer);
	    }
	}

	/**
	 * Replace the instrument's active buffer contents and set a new sample rate. 
	 * Replace with a new signal array using MultiChannelBuffer setChannel(0, newBuffer), 
	 * which creates a copy of the supplied array, and set a new bufferSamplerRate. 
	 * The sampler and its voices are also refreshed. Note that bufferSize will change 
	 * if the this.buffer and newBuffer are not the same size.
	 * <p>
	 * The instrument retains its own MultiChannelBuffer but updates its internal copy.
	 * The sampler and its voices also receive a fresh copy.
	 * </p>
	 * @param newBuffer       replacement mono source buffer
	 * @param newSampleRate   sample rate of the replacement buffer in Hz
	 */
	public synchronized void setBuffer(float[] newBuffer, float newSampleRate) {
	    if (newBuffer == null || newBuffer.length == 0) return;
	    // Ensure the instrument's internal MultiChannelBuffer matches size of newBuffer
	    if (buffer == null || buffer.getBufferSize() != newBuffer.length) {
	        this.buffer.setBufferSize(newBuffer.length);
	    }
	    // Copy signal into the instrument's internal buffer
	    buffer.setChannel(0, newBuffer);
	    bufferSize = newBuffer.length;
	    // Update sample rate references
	    setBufferSampleRate(newSampleRate);
	    // Propagate to sampler and voices — safe internal copy for playback
	    sampler.setBuffer(Arrays.copyOf(newBuffer, newBuffer.length), newSampleRate);
	}

	
	/** @return target AudioOutput */
	public AudioOutput getAudioOutput() { return out; }

	// --- Sample rate information ---
	/** @return source buffer sample rate in Hz */
	public float getBufferSampleRate() { return bufferSampleRate; }
	/** @return output sample rate in Hz */
	public float getOutputSampleRate() { return outputSampleRate; }
	/** @return sample rate used by this instrument's audio clock */
	@Override
	public float getSampleRate() { return outputSampleRate; }
	/** @return buffer-to-output sample-rate ratio */
	public float getSampleRateRatio() { return sampleRateRatio; }
	
	/**
	 * Update the buffer's intrinsic sample rate.
	 * 
	 * @param newRate    new sample rate for the buffer, affects playback rate
	 */
	public synchronized void setBufferSampleRate(float newRate) {
	    if (newRate > 0f) {
	        this.bufferSampleRate = newRate;
	        this.sampleRateRatio = (outputSampleRate > 0f)
	            ? bufferSampleRate / outputSampleRate
	            : 1f;
	        // propagate to sampler if relevant
	        if (sampler != null) sampler.setPlaybackSampleRate(newRate);
	    }
	}

	/**
	 * Update the output sample rate, for example, if audio device changes. 
	 * This is unlikely to happen, but we'll be cautious.
	 * 
	 * @param newRate   new output sample rate in Hz
	 */
	public synchronized void setOutputSampleRate(float newRate) {
	    if (newRate > 0f) {
	        this.outputSampleRate = newRate;
	        this.sampleRateRatio = bufferSampleRate / outputSampleRate;
	    }
	}
	
	/** 
	 * Synchronize output sample rate from AudioOutput directly.
	 */
	public synchronized void updateRateFromOutput() {
	    if (out != null) {
	        setOutputSampleRate(out.sampleRate());
	    }
	}

	/** @return default ADSR envelope */
	public ADSRParams getDefaultEnv() { return defaultEnv; }
	/**
	 * Sets the default ADSR envelope.
	 *
	 * @param env   default ADSR envelope
	 */
	public void setDefaultEnv(ADSRParams env) { this.defaultEnv = env; }
	
	/**
	 * Sets global pitch multiplier.
	 *
	 * @param scale   pitch multiplier
	 */
	public void setPitchScale(float scale) { this.globalPitch = scale; }
	/** @return global pitch multiplier */
	public float getPitchScale() { return globalPitch; }

	/**
	 * Sets global pan.
	 *
	 * @param pan   stereo pan position
	 */
	public void setGlobalPan(float pan) { this.globalPan = clampPan(pan); }
	/** @return global pan position */
	public float getGlobalPan() { return globalPan; }

	// ------------------------------------------------------------------------
	// Helper
	// ------------------------------------------------------------------------

	private static float clampPan(float pan)
	{
		if (pan < -1f) return -1f;
		if (pan > 1f) return 1f;
		return pan;
	}
	
	// ------------------------------------------------------------------------
	// Resource management
	// ------------------------------------------------------------------------

	private boolean isClosed = false;

	/** Stop all voices and disconnect UGens from the output. */
	public synchronized void close() {
	    if (isClosed) return;

	    stop();

	    // Disconnect sampler from output
	    if (sampler != null && out != null) {
	        //sampler.unpatch(out);
	    }

	    // Optional: release buffer reference for GC
	    buffer = null;

	    isClosed = true;
	}

}
