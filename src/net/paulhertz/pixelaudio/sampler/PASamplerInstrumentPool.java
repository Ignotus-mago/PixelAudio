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

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;
import net.paulhertz.pixelaudio.schedule.AudioSampleClock;
import net.paulhertz.pixelaudio.schedule.AudioUtility;

import java.util.*;

/**
 * Manages a group of PASamplerInstruments sharing the same source buffer
 * and audio output. Provides polyphony management, buffer swapping, and
 * per-instrument voice recycling. 
 *
 * <p>
 * Features:
 * </p>
 * <ul>
 *  <li>Sample-based instruments with ADSR envelopes</li>
 *  <li>Configurable pool size (number of instruments)</li>
 *  <li>Configurable max voices per instrument</li>
 *  <li>Safe buffer and sample rate propagation</li>
 *  <li>Voice stealing via release or stop()</li>
 *  <li>Looping detection</li>
 *  <li>Thread-safe access</li>
 * </ul>
 */
public class PASamplerInstrumentPool implements PASamplerPlayable, PAPlayable, AudioSampleClock {
    // ------------------------------------------------------------------------
    // Core state
    // ------------------------------------------------------------------------
	
    /** Audio output shared by all instruments in this pool. */
    private final AudioOutput out;
    /** Shared source buffer for pooled instruments. */
    private MultiChannelBuffer buffer;

    /** Instruments managed by this pool. */
    private final List<PASamplerInstrument> pool = new ArrayList<>();
    /** Requested number of instruments in the pool. */
    private int poolSize;
    /** Maximum voices allocated per instrument. */
    private int maxVoices = 8;
    
    // Pool-wide defaults / modifiers
    /** Default ADSR envelope passed to pooled instruments. */
    private ADSRParams defaultEnv;
    /** Pool-wide pitch multiplier. */
    private float globalPitch = 1.0f;
    /** Pool-wide stereo pan offset, from -1.0 left to 1.0 right. */
    private float globalPan = 0.0f;

    // Output / buffer timing info (useful now; essential for TimedLocation later)
    /** Output buffer size in frames per audio callback. */
    private int outputBufferSize;
	/** Sample rate of the source from which the buffer was loaded. */
	private float bufferSampleRate;
	/** Sample rate of the AudioOutput. */
	private float outputSampleRate;
	
	/** Pool-wide linear gain scalar. */
	private volatile float poolGain = 1f;

	/** Pool-wide sampler mix profile propagated to pooled instruments. */
	private volatile PASharedBufferSampler.MixProfile mixProfile =
	        PASharedBufferSampler.MixProfile.BALANCED;
	
	/** True to wrap finite sampler events across the source-buffer boundary. */
	private volatile boolean wrapAround = false;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Full backward-compatible constructor with a MultiChannelBuffer argument. Envelope is
     * supplied, pitch and pan default to globalPitch and globalPan. Note that the MultiChannelBuffer
     * we supply is not copied, we use it directly for storage. Calling applications should not 
     * modify the buffer. 
     *
     * @param buffer               shared MultiChannelBuffer
     * @param sampleRate           nominal sample rate of the buffer (Hz)
     * @param poolSize             number of instruments in the pool
     * @param perInstrumentVoices  number of voices per instrument
     * @param out                  AudioOutput to patch instruments into
     * @param defaultEnv           default ADSR envelope for all instruments
     */
    public PASamplerInstrumentPool(MultiChannelBuffer buffer,
    		float sampleRate,
    		int poolSize,
    		int perInstrumentVoices,
    		AudioOutput out,
    		ADSRParams defaultEnv) {
    	this.out = out;
    	this.buffer = buffer;
    	this.poolSize = Math.max(1, poolSize);
    	this.maxVoices = perInstrumentVoices;
    	this.bufferSampleRate = sampleRate;
    	this.outputSampleRate = (out != null) ? out.sampleRate() : sampleRate;
    	this.outputBufferSize = (out != null) ? out.bufferSize() : 1024;
    	this.defaultEnv = (defaultEnv != null) ? defaultEnv : new ADSRParams(0.8f, 0.01f, 0.2f, 0.8f, 0.3f);
    	this.globalPitch = 1.0f;
    	this.globalPan = 0.0f;
    	// Initialize instrument pool
    	initPool();
    }

    /**
     * Full constructor.
     * @param buffer            shared MultiChannelBuffer
     * @param out               target AudioOutput
     * @param poolSize          number of instruments to preallocate
     * @param bufferSampleRate  nominal sample rate of buffer (Hz)
     * @param env               default ADSR (nullable)
     */
    public PASamplerInstrumentPool(MultiChannelBuffer buffer, 
    		AudioOutput out, 
    		int poolSize,
    		float bufferSampleRate,
    		ADSRParams env) {
        this.out = out;
        this.buffer = buffer;
        this.poolSize = Math.max(1, poolSize);
        this.bufferSampleRate = bufferSampleRate;
        this.defaultEnv = (env != null) ? env : new ADSRParams(1f, 0.01f, 0.2f, 0.8f, 0.3f);
        this.outputSampleRate = (out != null) ? out.sampleRate() : bufferSampleRate;
        this.outputBufferSize = (out != null) ? out.bufferSize() : 1024;
        initPool();
    }

    /**
     * Convenience constructor: default env, bufferSampleRate = out.sampleRate().
     *
     * @param buffer     shared MultiChannelBuffer
     * @param out        target AudioOutput
     * @param poolSize   number of instruments to preallocate
     */
    public PASamplerInstrumentPool(MultiChannelBuffer buffer,
                                   AudioOutput out,
                                   int poolSize) {
        this(buffer, out, poolSize, out.sampleRate(), new ADSRParams(0.8f, 0.01f, 0.2f, 0.8f, 0.3f));
    }

    private synchronized void initPool() {
        pool.clear();
        for (int i = 0; i < poolSize; i++) {
            PASamplerInstrument inst = new PASamplerInstrument(buffer, bufferSampleRate, maxVoices, out, defaultEnv);
            inst.setPitchScale(globalPitch);
            inst.setGlobalPan(globalPan);
            inst.setParentGain(poolGain);
            inst.setMixProfile(mixProfile);
            inst.setWrapAround(wrapAround);
            pool.add(inst);
        }
    }

    // ------------------------------------------------------------------------
    // Allocation 
    // ------------------------------------------------------------------------

    /**
     * Find a free instrument; if no free instrument find least-busy; otherwise smooth-steal one.
     */
    private synchronized PASamplerInstrument getAvailableInstrument() {
    	PASamplerInstrument free = null;
    	PASamplerInstrument leastBusy = null;
    	int leastCount = Integer.MAX_VALUE;
    	for (PASamplerInstrument inst : pool) {
    		if (!inst.hasActiveOrReleasingVoices()) {
    			free = inst;
    			break;
    		}
    		// track least busy in case none are free
    		int c = inst.activeOrReleasingVoiceCount();
    		if (c < leastCount) {
    			leastCount = c;
    			leastBusy = inst;
    		}
    	}
    	if (free != null) return free;
    	// If we get here, all instruments are busy.
    	// Prefer the least-busy instrument (lets its tails finish if possible).
    	if (leastBusy != null) return leastBusy;
    	// Fallback: smooth-steal the first instrument (should be rare)
    	// System.out.println("-- sampler pool fallback: releasing all voices on victim instrument");
    	PASamplerInstrument victim = pool.get(0);
    	victim.releaseAllVoices(); // smooth release instead of hard stop
    	return victim;
    }

    // ------------------------------------------------------------------------
    // PAPlayable implementation
    // ------------------------------------------------------------------------


    /**
     * The most basic play command, plays the entire buffer.
     */
    @Override
    public synchronized int play(float amplitude, float pitch, float pan) {
        PASamplerInstrument inst = getAvailableInstrument();
        if (inst == null) return 0;
        return inst.play(amplitude, pitch, pan);
    }

    @Override
    public synchronized void stop() {
        for (PASamplerInstrument inst : pool) {
            inst.stop();
        }
    }

    // ------------------------------------------------------------------------
    // PASamplerPlayable implementation
    // ------------------------------------------------------------------------

    /**
     * The primary play command, with all common arguments in standard order. 
     * The playSample(...) methods provide the greatest flexibility in method signatures
     * and will call this method. 
     */
    @Override
    public synchronized int play(int samplePos, int sampleLen, float amplitude,
                                 ADSRParams env, float pitch, float pan) {
        PASamplerInstrument inst = getAvailableInstrument();
        if (inst == null) return 0;
        return inst.play(samplePos, sampleLen, amplitude, env, pitch, pan);
    }

    // ------------------------------------------------------------------------
    // Scheduled Playback API
    // ------------------------------------------------------------------------

    /**
     * Schedules playback on an available pooled instrument at an absolute sample time.
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
        PASamplerInstrument inst = getAvailableInstrument();
        if (inst == null) return;
        inst.startAtSampleTime(samplePos, sampleLen, amplitude, env, pitch, pan, startSample);
    }

    /**
     * Schedules playback after a delay in samples relative to the selected instrument clock.
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
        PASamplerInstrument inst = getAvailableInstrument();
        if (inst == null) return;
        inst.startAfterDelaySamples(samplePos, sampleLen, amplitude, env, pitch, pan, delaySamples);
    }

    /** Schedules playback at the selected instrument's current sampler clock. */
    public synchronized void startNow(int samplePos, int sampleLen, float amplitude,
            ADSRParams env, float pitch, float pan) {
        startAfterDelaySamples(samplePos, sampleLen, amplitude, env, pitch, pan, 0L);
    }

    /**
     * Returns a representative sample clock from the first pooled instrument.
     *
     * <p>All instruments are patched to the same output when the pool is constructed, but
     * callers that need cross-engine alignment should still choose one transport clock and
     * pass absolute sample times to every instrument.</p>
     *
     * @return current sample clock, or 0 if the pool is empty
     */
    @Override
    public synchronized long getCurrentSampleTime() {
        return pool.isEmpty() ? 0L : pool.get(0).getCurrentSampleTime();
    }

    /** Clears scheduled starts across all pooled instruments. */
    public synchronized void clearScheduled() {
        for (PASamplerInstrument inst : pool) {
            if (inst != null) inst.clearScheduled();
        }
    }

    /**
     * Sets whether finite sampler events wrap source-buffer reads at the buffer end.
     *
     * @param wrapAround true to wrap source-buffer reads for newly triggered events
     */
    public synchronized void setWrapAround(boolean wrapAround) {
        this.wrapAround = wrapAround;
        for (PASamplerInstrument inst : pool) {
            if (inst != null) inst.setWrapAround(wrapAround);
        }
    }

    /** @return true when finite sampler events wrap source-buffer reads */
    public synchronized boolean isWrapAround() {
        return wrapAround;
    }

    /** @return true if any pooled instrument has a looping sampler */
    // not an @Override
    public synchronized boolean isLooping() {
        for (PASamplerInstrument inst : pool) {
            if (inst.getSampler().isLooping()) return true;
        }
        return false;
    }
    
    /**
     * Counts all active or releasing sampler voices across the instrument pool.
     *
     * @return total active or releasing voice count
     */
    public synchronized int samplerActiveVoiceCount() {
    	int count = 0;
    	for (PASamplerInstrument inst : pool) {
    		if (inst != null) count += inst.activeOrReleasingVoiceCount();
    	}
    	return count;
    }

    /** Stops playback on all pooled instruments. */
    // @Override
    public synchronized void stopAll() {
        stop();
    }
    
    /**
     * Smoothly release all active voices on all instruments.
     * Preferred for live transitions and endings.
     */
    public synchronized void fadeOutAll() {
        for (PASamplerInstrument inst : pool) {
            if (inst != null) inst.fadeToStop();
        }
    }

    /**
     * Alias for fadeOutAll(), if you prefer the same naming style as instruments.
     */
    public synchronized void releaseAll() {
        fadeOutAll();
    }   

    // ------------------------------------------------------------------------
    // Backward-compatible playSample(...) overloads
    //
    // Standard argument order: 
    // (int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch, float pan)
    //
    // ------------------------------------------------------------------------

    /** {@inheritDoc} */
    public synchronized int playSample(int samplePos, int sampleLen, float amplitude,
                                       ADSRParams env, float pitch, float pan) {
        return play(samplePos, sampleLen, amplitude, env, pitch, pan);
    }

	/**
	 * Plays a sample range with pitch and pan.
	 *
	 * @param samplePos buffer index to start playback
	 * @param sampleLen requested duration in samples
	 * @param amplitude gain multiplier
	 * @param pitch pitch or playback-rate multiplier
	 * @param pan stereo pan
	 * @return actual event duration in samples
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude, 
			float pitch, float pan) {
		return play(samplePos, sampleLen, amplitude, defaultEnv, pitch, pan);
	}

	/** {@inheritDoc} */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude) {
        return play(samplePos, sampleLen, amplitude, defaultEnv, globalPitch, globalPan);
    }

    /** {@inheritDoc} */
    public synchronized int playSample(int samplePos, int sampleLen, float amplitude, float pitch) {
        return play(samplePos, sampleLen, amplitude, defaultEnv, pitch, globalPan);
    }

    /** {@inheritDoc} */
    public synchronized int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
        return play(samplePos, sampleLen, amplitude, env, globalPitch, globalPan);
    }

    /**
     * Plays a sample range with envelope and pitch.
     *
     * @param samplePos   buffer index to start playback
     * @param sampleLen   requested duration in samples
     * @param amplitude   gain multiplier
     * @param env         optional ADSR envelope
     * @param pitch       pitch or playback-rate multiplier
     * @return actual event duration in samples
     */
    public synchronized int playSample(int samplePos, int sampleLen, float amplitude,
                                       ADSRParams env, float pitch) {
        return play(samplePos, sampleLen, amplitude, env, pitch, globalPan);
    }
    
    /**
     * Plays a range from a temporary buffer.
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
    	// Use this call to temporarily switch the instrument's buffer for the event.
    	PASamplerInstrument inst = getAvailableInstrument();
    	if (inst == null) return 0;
    	inst.setBuffer(buffer, bufferSampleRate);    // assumes same nominal bufferSampleRate; call setBufferRate(...) first if needed
    	// ADSRParams useEnv = (env != null) ? env : defaultEnv;
    	// return inst.playSample(samplePos, sampleLen, amplitude, useEnv, scaledPitch, finalPan);
    	return inst.play(samplePos, sampleLen, amplitude, env, pitch, pan);
    }

    /**
     * Plays a range from a temporary buffer.
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
    	return playSample(buffer, samplePos, sampleLen, amplitude, env, pitch, globalPan);
    }

    // ------------------------------------------------------------------------
    // Buffer & sample-rate propagation
    // ------------------------------------------------------------------------
    
    /**
     * Swap the pool's shared buffer; keeps existing bufferSampleRate.
     * 
     * @param newBuffer    a new MultiChannelBuffer as audio source for this instrument
     * 
     */
    public synchronized void setBuffer(MultiChannelBuffer newBuffer) {
        if (newBuffer == null) return;       
        // Ensure channel count matches to avoid hidden exceptions in Minim
        if (this.buffer.getChannelCount() != newBuffer.getChannelCount() ||
            this.buffer.getBufferSize()  != newBuffer.getBufferSize()) {
            // Different shape: replace instead of set()
        	this.buffer = new MultiChannelBuffer(newBuffer.getBufferSize(), newBuffer.getChannelCount());
            this.buffer.set(newBuffer);
        } 
        else {
            this.buffer.set(newBuffer); // fast path
        }
        for (PASamplerInstrument inst : pool) {
            inst.setBuffer(newBuffer);
        }
        propagateParentGain();
    }

    /**
     * Swap the pool's shared buffer and update its buffer sample rate.
     * 
     * @param newBuffer   a new MultiChannelBuffer as audio source for this instrument
     * @param newBufferSampleRate sample rate of the replacement buffer in Hz
     */
    public synchronized void setBuffer(MultiChannelBuffer newBuffer, float newBufferSampleRate) {
        if (newBuffer == null) return;
        // Ensure channel count matches to avoid hidden exceptions in Minim
        if (this.buffer.getChannelCount() != newBuffer.getChannelCount() ||
            this.buffer.getBufferSize()  != newBuffer.getBufferSize()) {
            // Different shape: replace instead of set()
            this.buffer = new MultiChannelBuffer(newBuffer.getBufferSize(), newBuffer.getChannelCount());
            this.buffer.set(newBuffer);
        } 
        else {
            this.buffer.set(newBuffer); // fast path
        }
        this.bufferSampleRate = newBufferSampleRate;
        for (PASamplerInstrument inst : pool) {
            inst.setBuffer(newBuffer, newBufferSampleRate);
        }
        propagateParentGain();
    }
    
    /**
     * Propagate a new float[] buffer to all instruments in this pool.
     *
     * @param newBuffer   replacement mono source buffer
     * @param newBufferSampleRate sample rate of the replacement buffer in Hz
     */
    public synchronized void setBuffer(float[] newBuffer, float newBufferSampleRate) {
    	if (newBuffer == null || newBuffer.length == 0) return;
    	if (newBuffer.length != this.buffer.getBufferSize()) {
    		this.buffer = new MultiChannelBuffer(newBuffer.length, 1);
    	}
    	else {
    		this.buffer.setChannel(0, newBuffer);
    	}
    	this.bufferSampleRate = newBufferSampleRate;
    	for (PASamplerInstrument inst : pool) {
    		inst.setBuffer(newBuffer, newBufferSampleRate);
    	}
    	propagateParentGain();
    }

    /** Re-sync instruments to current AudioOutput sample rate (if output device changes). */
    public synchronized void updateRateFromOutput() {
        if (out == null) return;
        this.outputSampleRate = out.sampleRate();
        this.outputBufferSize = out.bufferSize();
        for (PASamplerInstrument inst : pool) {
            inst.setOutputSampleRate(outputSampleRate);
        }
    }

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------
    
    /**
     * Sets pool output gain.
     *
     * @param linear   linear gain value
     */
    public void setGain(float linear) {
        if (Float.isNaN(linear) || Float.isInfinite(linear)) return;
        poolGain = Math.max(0f, linear);
        propagateParentGain();
    }

    /** @return pool output gain as a linear value */
    public float getGain() { return poolGain; }

    /**
     * Sets pool output gain in decibels.
     *
     * @param db gain in decibels
     */
    public void setGainDb(float db) { setGain(AudioUtility.dbToLinear(db)); }

    /** @return pool output gain in decibels */
    public float getGainDb() { return AudioUtility.linearToDb(poolGain); }

    private void propagateParentGain() {
        for (PASamplerInstrument inst : pool) {
            inst.setParentGain(poolGain); // package-private method in same package
        }
    }
    
    /**
     * Set the sampler bus mix behavior for every instrument in the pool.
     *
     * @param profile desired PASharedBufferSampler mix profile
     */
    public synchronized void setMixProfile(PASharedBufferSampler.MixProfile profile) {
        if (profile == null) return;
        this.mixProfile = profile;
        for (PASamplerInstrument inst : pool) {
            inst.setMixProfile(profile);
        }
    }

    /**
     * @return the pool-wide sampler mix profile
     */
    public synchronized PASharedBufferSampler.MixProfile getMixProfile() {
        return mixProfile;
    }

    /**
     * Convenience: advance to the next mix profile and apply it pool-wide.
     *
     * @return the newly selected profile
     */
    public synchronized PASharedBufferSampler.MixProfile cycleMixProfile() {
        PASharedBufferSampler.MixProfile[] vals = PASharedBufferSampler.MixProfile.values();
        int i = (mixProfile.ordinal() + 1) % vals.length;
        setMixProfile(vals[i]);
        return mixProfile;
    }

    /**
     * Sets global pitch multiplier applied to subsequent playback.
     *
     * @param pitch   pitch multiplier
     */
    public synchronized void setGlobalPitch(float pitch) { this.globalPitch = pitch; }
    /** @return global pitch multiplier */
    public synchronized float getGlobalPitch() { return globalPitch; }

    /**
     * Sets global pan used by overloads without explicit pan.
     *
     * @param pan   stereo pan position
     */
    public synchronized void setGlobalPan(float pan) { this.globalPan = clampPan(pan); }
    /** @return global pan position */
    public synchronized float getGlobalPan() { return globalPan; }

    /**
     * Sets the default ADSR envelope for pooled instruments.
     *
     * @param env   default ADSR envelope
     */
    public synchronized void setDefaultEnv(ADSRParams env) {
        this.defaultEnv = (env != null) ? env : this.defaultEnv;
        for (PASamplerInstrument inst : pool) inst.setDefaultEnv(this.defaultEnv);
    }
    /** @return default ADSR envelope */
    public synchronized ADSRParams getDefaultEnv() { return defaultEnv; }
    
    /** @return maximum voices per instrument */
    public int getMaxVoices() { return maxVoices; }
    /**
     * Sets maximum voices per instrument and reapplies pool configuration.
     *
     * @param maxVoices   maximum voices per instrument
     */
    public synchronized void setMaxVoices(int maxVoices) {
        this.maxVoices = Math.max(1, maxVoices);
        reinitInstruments();
    }
    
    /** @return number of instruments in the pool */
    public synchronized int getPoolSize() { return pool.size(); }
    /**
     * Sets the number of instruments in the pool.
     *
     * @param newSize   desired pool size
     */
    public synchronized void setPoolSize(int newSize) {
        if (newSize != this.poolSize) {
            this.poolSize = Math.max(1, newSize);
            reinitInstruments();
        }
    }

    /** @return output sample rate in Hz */
    public synchronized float getOutputSampleRate() { return outputSampleRate; }
    /** @return sample rate used by this pool's representative audio clock */
    @Override
    public synchronized float getSampleRate() { return outputSampleRate; }
    /** @return output buffer size in samples */
    public synchronized int getOutputBufferSize() { return outputBufferSize; }
    /** @return source buffer sample rate in Hz */
    public synchronized float getBufferSampleRate() { return bufferSampleRate; }
    
    /** 
     * Resize the pool gracefully without disrupting existing instruments unnecessarily.
     * Active instruments are preserved whenever possible.
     */
    private synchronized void reinitInstruments() {
        int currentSize = pool.size();
        // --- Grow the pool ---
        if (poolSize > currentSize) {
            for (int i = currentSize; i < poolSize; i++) {
                PASamplerInstrument inst = new PASamplerInstrument(buffer, bufferSampleRate, maxVoices, out, defaultEnv);
                inst.setPitchScale(globalPitch);
                inst.setGlobalPan(globalPan);
                inst.setParentGain(poolGain);
                inst.setMixProfile(mixProfile);
                inst.setWrapAround(wrapAround);
                pool.add(inst);
            }
        }
        // --- Shrink the pool ---
        else if (poolSize < currentSize) {
            // Stop any extra instruments before removing them
            for (int i = poolSize; i < currentSize; i++) {
                PASamplerInstrument inst = pool.get(i);
                inst.stop();
                inst.close();
            }
            // Trim the list
            while (pool.size() > poolSize) {
                pool.remove(pool.size() - 1);
            }
        }
        // Update shared parameters on all instruments
        for (PASamplerInstrument inst : pool) {
            inst.setDefaultEnv(defaultEnv);
            inst.setPitchScale(globalPitch);
            inst.setGlobalPan(globalPan);
            inst.setParentGain(poolGain);
            inst.setMixProfile(mixProfile);
            inst.setWrapAround(wrapAround);
        }
    }

    /** @return mutable list of pooled instruments */
    public List<PASamplerInstrument> getInstruments() {
        return pool;
    }

    // ------------------------------------------------------------------------
    // TimedLocation hooks (stubs)
    // ------------------------------------------------------------------------

	/**
	 * Hook: schedule by future wallclock milliseconds; will be wired to TimedLocation later.
	 *
	 * @param triggerTimeMillis   future trigger time in milliseconds
	 * @param samplePos           buffer index to start playback
	 * @param sampleLen           requested duration in samples
	 * @param amplitude           gain multiplier
	 * @param env                 optional ADSR envelope
	 * @param pitch               pitch or playback-rate multiplier
	 * @param pan                 stereo pan
	 */
    public synchronized void schedulePlayAtMillis(long triggerTimeMillis,
                                                  int samplePos, int sampleLen,
                                                  float amplitude, ADSRParams env,
                                                  float pitch, float pan) 
    {
        // Stub: immediate play for now. TimedLocation will call into here later.
        playSample(samplePos, sampleLen, amplitude, env, pitch, pan);
    }

    /**
     * Hook: schedule by audio-frame index (frame = one AudioOutput callback).
     *
     * @param frameIndex  future audio callback frame index
     * @param samplePos   buffer index to start playback
     * @param sampleLen   requested duration in samples
     * @param amplitude   gain multiplier
     * @param env         optional ADSR envelope
     * @param pitch       pitch or playback-rate multiplier
     * @param pan         stereo pan
     */
    public synchronized void schedulePlayAtFrame(long frameIndex,
                                                 int samplePos, int sampleLen,
                                                 float amplitude, ADSRParams env,
                                                 float pitch, float pan)
    {
        // Stub: immediate play for now. TimedLocation will call into here later.
        playSample(samplePos, sampleLen, amplitude, env, pitch, pan);
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private static float clampPan(float pan) {
        return (pan < -1f) ? -1f : (pan > 1f ? 1f : pan);
    }

    // ------------------------------------------------------------------------
    // Resource management
    // ------------------------------------------------------------------------

    private boolean isClosed = false;

    /** Close all instruments and free audio resources. */
    public synchronized void close() {
    	if (isClosed) return;
    	stopAll();
    	for (PASamplerInstrument inst : pool) {
    		inst.close();
    	}
    	pool.clear();
    	buffer = null;    // necessary?
    	isClosed = true;
    }

    /**
     * Check whether the pool has been closed.
     *
     * @return true after the pool has been closed
     */
    public synchronized boolean isClosed() { return isClosed; }

}
