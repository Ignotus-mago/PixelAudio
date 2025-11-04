package net.paulhertz.pixelaudio.voices;

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PASamplerInstrumentPoolMulti
 *
 * Manages multiple PASamplerInstrumentPool instances, each typically tied to a
 * different source buffer or timbre.
 *
 * Supports:
 *  - Lazy or explicit creation of per-buffer subpools
 *  - Dynamic switching between active pools by key
 *  - Global stopAll() and close() propagation
 *  - Safe buffer and sample rate propagation using float[] updates
 */
public class PASamplerInstrumentPoolMulti implements PASamplerPlayable, PAPlayable {
	private final AudioOutput out;
	private final Map<String, PASamplerInstrumentPool> pools = new LinkedHashMap<>();
	private String activeKey = null; // default route
	private boolean isClosed = false;

	public PASamplerInstrumentPoolMulti(AudioOutput out) {
		this.out = out;
	}
	
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


	public void addPool(String key, MultiChannelBuffer buffer, float bufferSampleRate, int poolSize, int maxVoices, AudioOutput out, ADSRParams env) {
		PASamplerInstrumentPool p = new PASamplerInstrumentPool(buffer, out, poolSize, bufferSampleRate, env);
		pools.put(key, p);
		if (activeKey == null) activeKey = key;
	}

    public synchronized void setActive(String key) {
        if (pools.containsKey(key)) activeKey = key;
    }

    public synchronized PASamplerInstrumentPool getActivePool() {
        return (activeKey != null) ? pools.get(activeKey) : null;
    }

    private PASamplerInstrumentPool current() {
        return getActivePool();
    }

    // ------------------------------------------------------------------------
    // Playback (PAPlayable, PASamplerPlayable)
    // ------------------------------------------------------------------------

    @Override
	public int play(float amplitude, float pitch, float pan) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.play(amplitude, pitch, pan) : 0;
	}
	@Override
	public void stop() {
		for (PASamplerInstrumentPool p : pools.values()) p.stop();
	}

	// --- PASamplerPlayable ---
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

	// --- Back-compat playSample(...) shims ---
	public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch, float pan) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, env, pitch, pan) : 0;
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
	public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch) {
		PASamplerInstrumentPool p = current();
		return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, env, pitch) : 0;
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

    public synchronized PASamplerInstrumentPool getPoolByTag(String tag) {
        if (tag == null) return null;
        return pools.get(tag);
    }

	

    // ------------------------------------------------------------------------
    // Buffer propagation (new)
    // ------------------------------------------------------------------------

    /**
     * Propagate a new float[] buffer and sample rate to all sub-pools.
     * Used when the source signal (e.g. audioSignal) has changed.
     */
    public synchronized void setBuffer(float[] newBuffer, float newSampleRate) {
        if (newBuffer == null || newBuffer.length == 0) return;
        for (PASamplerInstrumentPool pool : pools.values()) {
            pool.setBuffer(newBuffer, newSampleRate);
        }
    }

    /**
     * Replace sub-pool buffers from a MultiChannelBuffer source.
     */
    public synchronized void setBuffer(MultiChannelBuffer newBuffer, float newSampleRate) {
        if (newBuffer == null) return;
        float[] channel0 = newBuffer.getChannel(0);
        setBuffer(channel0, newSampleRate);
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

}
