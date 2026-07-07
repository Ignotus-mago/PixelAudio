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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PASamplerInstrumentPoolMulti
 *
 * Manages multiple PASamplerInstrumentPool instances, each keyed by name.
 * Each sub-pool may represent a distinct sample source or timbral layer.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Dynamic add/remove of sub-pools.</li>
 *   <li>Shared AudioOutput.</li>
 *   <li>Optional active-key routing, where only one pool plays at a time.</li>
 *   <li>Global stop, buffer, and sample-rate propagation.</li>
 *   <li>Graceful resizing using {@link PASamplerInstrumentPool} gentle reinitialization.</li>
 * </ul>
 *
 * Thread-safe and compatible with both PAPlayable and PASamplerPlayable.
 * <br>
 * TODO example sketches.
 */
public class PASamplerInstrumentPoolMulti implements PASamplerPlayable, PAPlayable, AudioSampleClock {
	private final AudioOutput out;
	private final Map<String, PASamplerInstrumentPool> pools = new LinkedHashMap<>();
	private String activeKey = null; // default route
	private boolean isClosed = false;
	
	private volatile float masterGain = 1f;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

	/**
	 * Constructs an empty multi-pool manager.
	 *
	 * @param out   shared AudioOutput for sub-pools
	 */
	public PASamplerInstrumentPoolMulti(AudioOutput out) {
		this.out = out;
	}
	
    /**
     * Convenience constructor for single-pool initialization.
     *
     * @param key                   key for the initial pool
     * @param buffer                source buffer for the initial pool
     * @param sampleRate            sample rate of the source buffer in Hz
     * @param poolSize              number of instruments in the initial pool
     * @param perInstrumentVoices   voices per instrument
     * @param out                   shared AudioOutput
     * @param adsr                  default ADSR envelope
     */
	public PASamplerInstrumentPoolMulti(String key, 
			MultiChannelBuffer buffer,
			float sampleRate,
			int poolSize,
			int perInstrumentVoices,
			AudioOutput out,
			ADSRParams adsr) {
		this(out);
		this.addPool(key, buffer, sampleRate, poolSize, perInstrumentVoices, out, adsr);
	}


    // ------------------------------------------------------------------------
    // Pool management
    // ------------------------------------------------------------------------

	/**
	 * Adds a named sampler pool.
	 *
	 * @param key pool key
	 * @param buffer                source buffer for the pool
	 * @param bufferSampleRate      sample rate of the source buffer in Hz
	 * @param poolSize              number of instruments in the pool
	 * @param perInstrumentVoices   voices per instrument
	 * @param out                   AudioOutput for the pool
	 * @param env                   default ADSR envelope
	 */
	public void addPool(String key, MultiChannelBuffer buffer, float bufferSampleRate, 
			int poolSize, int perInstrumentVoices, AudioOutput out, ADSRParams env) {
		if (key == null || buffer == null) return;
		if (isClosed) return;
		PASamplerInstrumentPool p = new PASamplerInstrumentPool(buffer, bufferSampleRate, poolSize, perInstrumentVoices, out, env);
		pools.put(key, p);
		if (activeKey == null) activeKey = key;
	}
	
	/**
	 * Remove a pool by key and close it safely.
	 *
	 * @param key   pool key to remove
	 */
	public synchronized void removePool(String key) {
		PASamplerInstrumentPool p = pools.remove(key);
		if (p != null) {
			p.stopAll();
			p.close();
		}
		if (key.equals(activeKey)) activeKey = pools.isEmpty() ? null : pools.keySet().iterator().next();
	}
	

    /** Returns the currently active sub-pool (may be null). */
    private synchronized PASamplerInstrumentPool current() {
        return (activeKey != null) ? pools.get(activeKey) : null;
    }

    /**
     * Activate the given pool key for subsequent play() calls.
     *
     * @param key   pool key to activate
     */
    public synchronized void setActive(String key) {
        if (pools.containsKey(key)) activeKey = key;
    }

    /** @return currently active pool key */
    public synchronized String getActiveKey() { return activeKey; }

    
    // ------------------------------------------------------------------------
    // ---- PAPlayable ----
    // ------------------------------------------------------------------------

    /**
     * Plays the instrument's entire buffer, from PAPlayable interface.
     */
    @Override
	public int play(float amplitude, float pitch, float pan) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.play(amplitude, pitch, pan) : 0;
	}
    
	@Override
	/** Stops playback on every sub-pool. */
	public void stop() {
		for (PASamplerInstrumentPool p : pools.values()) p.stop();
	}

    // ------------------------------------------------------------------------
	// ---- PASamplerPlayable ----
    // ------------------------------------------------------------------------
	
	/**
	 * PASamplerPlayable's play method, all six standard parameters in standard order,
	 * called by other methods. 
	 */
	@Override
	public int play(int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch, float pan) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.play(samplePos, sampleLen, amplitude, env, pitch, pan) : 0;
	}

    // ------------------------------------------------------------------------
    // Scheduled Playback API
    // ------------------------------------------------------------------------

	/**
	 * Schedules playback on the active pool at an absolute sample time.
	 *
	 * @param samplePos    buffer index to start playback
	 * @param sampleLen    requested duration in samples
	 * @param amplitude    gain multiplier
	 * @param env          ADSR envelope parameters, or null to use the default
	 * @param pitch        pitch or playback-rate multiplier
	 * @param pan          stereo pan
	 * @param startSample  absolute sample index at which playback should start
	 */
	public void startAtSampleTime(int samplePos, int sampleLen, float amplitude,
			ADSRParams env, float pitch, float pan, long startSample) {
		PASamplerInstrumentPool p = current();
		if (p != null) p.startAtSampleTime(samplePos, sampleLen, amplitude, env, pitch, pan, startSample);
	}

	/**
	 * Schedules playback on the active pool after a delay in samples.
	 *
	 * @param samplePos      buffer index to start playback
	 * @param sampleLen      requested duration in samples
	 * @param amplitude      gain multiplier
	 * @param env            ADSR envelope parameters, or null to use the default
	 * @param pitch          pitch or playback-rate multiplier
	 * @param pan            stereo pan
	 * @param delaySamples   delay from the current sample time
	 */
	public void startAfterDelaySamples(int samplePos, int sampleLen, float amplitude,
			ADSRParams env, float pitch, float pan, long delaySamples) {
		PASamplerInstrumentPool p = current();
		if (p != null) p.startAfterDelaySamples(samplePos, sampleLen, amplitude, env, pitch, pan, delaySamples);
	}

	/** Schedules playback on the active pool at its current sampler clock. */
	public void startNow(int samplePos, int sampleLen, float amplitude,
			ADSRParams env, float pitch, float pan) {
		startAfterDelaySamples(samplePos, sampleLen, amplitude, env, pitch, pan, 0L);
	}

	/**
	 * Returns a representative sample clock from the active pool.
	 *
	 * @return current sample clock, or 0 if no active pool is available
	 */
	@Override
	public long getCurrentSampleTime() {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.getCurrentSampleTime() : 0L;
	}

	/** @return sample rate used by the active pool's representative audio clock */
	@Override
	public float getSampleRate() {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.getSampleRate() : (out != null ? out.sampleRate() : 0f);
	}

	/** Clears scheduled starts across all sub-pools. */
	public void clearScheduled() {
		for (PASamplerInstrumentPool p : pools.values()) p.clearScheduled();
	}
	
	// @Override
	/** @return true if any sub-pool has looping voices */
	public boolean isLooping() {
		for (PASamplerInstrumentPool p : pools.values()) if (p.isLooping()) return true;
		return false;
	}
	
	//  @Override
	/** Stops all voices in every sub-pool. */
	public void stopAll() {
		for (PASamplerInstrumentPool p : pools.values()) p.stopAll();
	}

    // ------------------------------------------------------------------------
    // Backward-compatible playSample(...) overloads
    //
    // Standard argument order: 
    // (int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch, float pan)
    //
    // ------------------------------------------------------------------------

	/** {@inheritDoc} */
	public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch, float pan) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, env, pitch, pan) : 0;
	}

	/**
	 * Plays a sample range with envelope and pitch on the active pool.
	 *
	 * @param samplePos   buffer index to start playback
	 * @param sampleLen   requested duration in samples
	 * @param amplitude   gain multiplier
	 * @param env         optional ADSR envelope
	 * @param pitch       pitch or playback-rate multiplier
	 * @return actual event duration in samples
	 */
	public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, env, pitch) : 0;
	}
	
	/**
	 * Plays a sample range with pitch and pan on the active pool.
	 *
	 * @param samplePos   buffer index to start playback
	 * @param sampleLen   requested duration in samples
	 * @param amplitude   gain multiplier
	 * @param pitch       pitch or playback-rate multiplier
	 * @param pan         stereo pan
	 * @return actual event duration in samples
	 */
	public int playSample(int samplePos, int sampleLen, float amplitude, float pitch, float pan) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, pitch, pan) : 0;
	}

	/** {@inheritDoc} */
	public int playSample(int samplePos, int sampleLen, float amplitude) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(samplePos, sampleLen, amplitude) : 0;
	}
	
	/** {@inheritDoc} */
	public int playSample(int samplePos, int sampleLen, float amplitude, float pitch) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, pitch) : 0;
	}
	
	/** {@inheritDoc} */
	public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, env) : 0;
		
	}
	
	/**
	 * Plays a range from a temporary buffer on the active pool.
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
	public int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
			float amplitude, ADSRParams env, float pitch, float pan) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(buffer, samplePos, sampleLen, amplitude, env, pitch, pan) : 0;
	}
	
	/**
	 * Plays a range from a temporary buffer on the active pool.
	 *
	 * @param buffer      source buffer for playback
	 * @param samplePos   buffer index to start playback
	 * @param sampleLen   requested duration in samples
	 * @param amplitude   gain multiplier
	 * @param env         optional ADSR envelope
	 * @param pitch       pitch or playback-rate multiplier
	 * @return actual event duration in samples
	 */
	public int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
			float amplitude, ADSRParams env, float pitch) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(buffer, samplePos, sampleLen, amplitude, env, pitch) : 0;
	}

    // ------------------------------------------------------------------------
    // TAG-BASED PLAYBACK & CONFIGURATION
    // ------------------------------------------------------------------------

    /**
     * Plays a sample range on a named pool.
     *
     * @param tag         pool key
     * @param samplePos   buffer index to start playback
     * @param sampleLen   requested duration in samples
     * @param amplitude   gain multiplier
     * @param env         optional ADSR envelope
     * @param pitch       pitch or playback-rate multiplier
     * @param pan         stereo pan
     * @return actual event duration in samples
     */
    public synchronized int playSample(String tag,
                                       int samplePos,
                                       int sampleLen,
                                       float amplitude,
                                       ADSRParams env,
                                       float pitch,
                                       float pan) {
    	PASamplerInstrumentPool pool = getPoolByTag(tag);
        if (pool == null)
            throw new IllegalArgumentException("No buffer found for tag: " + tag);
        return pool.playSample(samplePos, sampleLen, amplitude, env, pitch, pan);
    }
    
    /**
     * Reports whether a named pool exists.
     *
     * @param tag   pool key
     * @return true when the pool exists
     */
    public synchronized boolean hasPool(String tag) {
        return tag != null && pools.containsKey(tag);
    }

    /**
     * Returns a named pool.
     *
     * @param tag   pool key
     * @return matching pool, or null
     */
    public synchronized PASamplerInstrumentPool getPoolByTag(String tag) {
        if (tag == null) return null;
        return pools.get(tag);
    }
	

    // ------------------------------------------------------------------------
    // Buffer & sample-rate propagation
    // ------------------------------------------------------------------------

    /**
     * Propagate new buffer to all sub-pools, keeping sample rates intact.
     *
     * @param newBuffer   replacement source buffer
     */
    public synchronized void setBuffer(MultiChannelBuffer newBuffer) {
        if (newBuffer == null) return;
        for (PASamplerInstrumentPool p : pools.values()) {
            p.setBuffer(newBuffer);
        }
    }

    /**
     * Propagate new buffer and sample rate to all sub-pools.
     *
     * @param newBuffer       replacement source buffer
     * @param newSampleRate   sample rate of the replacement buffer in Hz
     */
    public synchronized void setBuffer(MultiChannelBuffer newBuffer, float newSampleRate) {
        if (newBuffer == null) return;
        for (PASamplerInstrumentPool p : pools.values()) {
            p.setBuffer(newBuffer, newSampleRate);
        }
    }

    /**
     * Propagate float[] buffer to all sub-pools (mono assumption).
     *
     * @param newBuffer       replacement mono source buffer
     * @param newSampleRate   sample rate of the replacement buffer in Hz
     */
    public synchronized void setBuffer(float[] newBuffer, float newSampleRate) {
        if (newBuffer == null || newBuffer.length == 0) return;
        for (PASamplerInstrumentPool p : pools.values()) {
            p.setBuffer(newBuffer, newSampleRate);
        }
    }

    /** Update sample rates of all sub-pools from AudioOutput. */
    public synchronized void updateRateFromOutput() {
        for (PASamplerInstrumentPool p : pools.values()) {
            p.updateRateFromOutput();
        }
    }

    /**
     * Sets master gain on every sub-pool.
     *
     * @param linear   linear gain value
     */
    public void setMasterGain(float linear) {
        masterGain = Math.max(0f, linear);
        for (PASamplerInstrumentPool p : pools.values()) {
            p.setGain(masterGain); // if you want this to override per-pool gain
        }
    }
    
    // ------------------------------------------------------------------------
    // Resizing support
    // ------------------------------------------------------------------------

    /**
     * Adjust size or polyphony of all sub-pools gracefully.
     *
     * @param newPoolSize              number of instruments per pool
     * @param newVoicesPerInstrument   voices per instrument
     */
    public synchronized void resizeAllPools(int newPoolSize, int newVoicesPerInstrument) {
        for (PASamplerInstrumentPool p : pools.values()) {
            p.setPoolSize(newPoolSize);
            p.setMaxVoices(newVoicesPerInstrument);
        }
    }

    
    // TimedLocation hooks can simply forward to the selected pool when we’re ready

	// ------------------------------------------------------------------------
	// Resource management
	// ------------------------------------------------------------------------

	/** Close all sub-pools and release shared resources. */
	public synchronized void close() {
		if (isClosed) return;
		for (PASamplerInstrumentPool pool : pools.values()) {
			pool.close();
		}
		pools.clear();
		activeKey = null;
		isClosed = true;
	}

	/** @return true after this multi-pool manager has been closed */
	public synchronized boolean isClosed() { return isClosed; }
	
    // ------------------------------------------------------------------------
    // Debug / inspection
    // ------------------------------------------------------------------------

    /** @return number of managed pools */
    public synchronized int getPoolCount() { return pools.size(); }
    /** @return map of pool keys to pools */
    public synchronized Map<String, PASamplerInstrumentPool> getPools() { return pools; }
    
    /** Prints current pool state to standard output. */
    public synchronized void debugPrintState() {
        System.out.printf("PASamplerInstrumentPoolMulti: %d pools active%n", pools.size());
        for (Map.Entry<String, PASamplerInstrumentPool> e : pools.entrySet()) {
            System.out.printf("  • %s : %d instruments, %.1f Hz buffer rate%n",
                e.getKey(),
                e.getValue().getPoolSize(),
                e.getValue().getBufferSampleRate());
        }
    }

}
