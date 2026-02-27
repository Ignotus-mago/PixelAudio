package net.paulhertz.pixelaudio.sampler;

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PASamplerInstrumentPoolMulti
 *
 * Manages multiple PASamplerInstrumentPool instances, each keyed by name.
 * Each sub-pool may represent a distinct sample source or timbral layer.
 *
 * Features:
 *  - Dynamic add/remove of sub-pools
 *  - Shared AudioOutput
 *  - Optional active-key routing (only one pool plays at a time)
 *  - Global stop, buffer, and sample-rate propagation
 *  - Graceful resizing using PASamplerInstrumentPool’s gentle reinit
 *
 * Thread-safe and compatible with both PAPlayable and PASamplerPlayable.
 */
public class PASamplerInstrumentPoolMulti implements PASamplerPlayable, PAPlayable {
	private final AudioOutput out;
	private final Map<String, PASamplerInstrumentPool> pools = new LinkedHashMap<>();
	private String activeKey = null; // default route
	private boolean isClosed = false;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

	public PASamplerInstrumentPoolMulti(AudioOutput out) {
		this.out = out;
	}
	
    /**
     * Convenience constructor for single-pool initialization.
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

	public void addPool(String key, MultiChannelBuffer buffer, float bufferSampleRate, 
			int poolSize, int perInstrumentVoices, AudioOutput out, ADSRParams env) {
		if (key == null || buffer == null) return;
		if (isClosed) return;
		PASamplerInstrumentPool p = new PASamplerInstrumentPool(buffer, bufferSampleRate, poolSize, perInstrumentVoices, out, env);
		pools.put(key, p);
		if (activeKey == null) activeKey = key;
	}
	
	/** Remove a pool by key and close it safely. */
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

    /** Activate the given pool key for subsequent play() calls. */
    public synchronized void setActive(String key) {
        if (pools.containsKey(key)) activeKey = key;
    }

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
	
	// @Override
	public boolean isLooping() {
		for (PASamplerInstrumentPool p : pools.values()) if (p.isLooping()) return true;
		return false;
	}
	
	//  @Override
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

	public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch, float pan) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, env, pitch, pan) : 0;
	}

	public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, env, pitch) : 0;
	}
	
	public int playSample(int samplePos, int sampleLen, float amplitude, float pitch, float pan) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, pitch, pan) : 0;
	}

	public int playSample(int samplePos, int sampleLen, float amplitude) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(samplePos, sampleLen, amplitude) : 0;
	}
	
	public int playSample(int samplePos, int sampleLen, float amplitude, float pitch) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, pitch) : 0;
	}
	
	public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, env) : 0;
		
	}
	
	public int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
			float amplitude, ADSRParams env, float pitch, float pan) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(buffer, samplePos, sampleLen, amplitude, env, pitch, pan) : 0;
	}
	
	public int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
			float amplitude, ADSRParams env, float pitch) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(buffer, samplePos, sampleLen, amplitude, env, pitch) : 0;
	}

    // ------------------------------------------------------------------------
    // TAG-BASED PLAYBACK & CONFIGURATION
    // ------------------------------------------------------------------------

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
    
    public synchronized boolean hasPool(String tag) {
        return tag != null && pools.containsKey(tag);
    }

    public synchronized PASamplerInstrumentPool getPoolByTag(String tag) {
        if (tag == null) return null;
        return pools.get(tag);
    }
	

    // ------------------------------------------------------------------------
    // Buffer & sample-rate propagation
    // ------------------------------------------------------------------------

    /** Propagate new buffer to all sub-pools, keeping sample rates intact. */
    public synchronized void setBuffer(MultiChannelBuffer newBuffer) {
        if (newBuffer == null) return;
        for (PASamplerInstrumentPool p : pools.values()) {
            p.setBuffer(newBuffer);
        }
    }

    /** Propagate new buffer and sample rate to all sub-pools. */
    public synchronized void setBuffer(MultiChannelBuffer newBuffer, float newSampleRate) {
        if (newBuffer == null) return;
        for (PASamplerInstrumentPool p : pools.values()) {
            p.setBuffer(newBuffer, newSampleRate);
        }
    }

    /** Propagate float[] buffer to all sub-pools (mono assumption). */
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

    
    // ------------------------------------------------------------------------
    // Resizing support
    // ------------------------------------------------------------------------

    /** Adjust size or polyphony of all sub-pools gracefully. */
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

	public synchronized boolean isClosed() { return isClosed; }
	
    // ------------------------------------------------------------------------
    // Debug / inspection
    // ------------------------------------------------------------------------

    public synchronized int getPoolCount() { return pools.size(); }
    public synchronized Map<String, PASamplerInstrumentPool> getPools() { return pools; }
    
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
