package net.paulhertz.pixelaudio.voices;

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;

import java.util.*;

/**
 * PASamplerInstrumentPool
 *
 * Manages a group of PASamplerInstruments sharing the same source buffer
 * and audio output. Provides polyphony management, buffer swapping, and
 * per-instrument voice recycling. 
 * 
 * -- TODO Uses a Minim MultiChannelBuffer for its audio sample storage. This could change. 
 * -- TODO Duplicates the MultiChannelBuffer. This may be overkill. 
 *
 * Features:
 *  - Configurable pool size (number of instruments)
 *  - Configurable max voices per instrument
 *  - Safe buffer and sample rate propagation
 *  - Voice stealing via release or stop()
 *  - Looping detection
 *  - Thread-safe access
 */
public class PASamplerInstrumentPool implements PASamplerPlayable, PAPlayable {
    // ------------------------------------------------------------------------
    // Core state
    // ------------------------------------------------------------------------
    private final AudioOutput out;
    private MultiChannelBuffer buffer;

    private final List<PASamplerInstrument> pool = new ArrayList<>();
    private int poolSize;
    private int maxVoices = 8;
    
    // Pool-wide defaults / modifiers
    private ADSRParams defaultEnv;
    private float globalPitch = 1.0f;    // pitch multiplier
    private float globalPan = 0.0f;      // -1..+1 left to right panning

    // Output / buffer timing info (useful now; essential for TimedLocation later)
    private int outputBufferSize;        // frames per audio callback
	private float bufferSampleRate;      // sample rate of source from which buffer was loaded
	private float outputSampleRate;      // sample rate of AudioOutput


    // ------------------------------------------------------------------------
    // Constructors
    // TODO is there a standard order for constructor arguments?
    // TODO do we duplicate MultiChannelBuffers supplied to instruments or not? RESOLVED: we do.
    // If we do it for one PASamplerInstrument*, we should do it for all. PASharedBufferSampler
    // and PASamplerVoice are not affected by this choice. 
    //
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
    		ADSRParams defaultEnv) 
    {
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
    		ADSRParams env) 
    {
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
     */
    public PASamplerInstrumentPool(MultiChannelBuffer buffer,
                                   AudioOutput out,
                                   int poolSize)
    {
        this(buffer, out, poolSize, out.sampleRate(), new ADSRParams(0.8f, 0.01f, 0.2f, 0.8f, 0.3f));
    }

    private synchronized void initPool() {
        pool.clear();
        for (int i = 0; i < poolSize; i++) {
            PASamplerInstrument inst = new PASamplerInstrument(buffer, bufferSampleRate, maxVoices, out, defaultEnv);
            inst.setPitchScale(globalPitch);
            inst.setGlobalPan(globalPan);
            pool.add(inst);
        }
    }

    // ------------------------------------------------------------------------
    // Allocation 
    // ------------------------------------------------------------------------

    /**
     * Find a free instrument; otherwise least-busy; otherwise smooth-steal one.
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

    // not an @Override
    public synchronized boolean isLooping() {
        for (PASamplerInstrument inst : pool) {
            if (inst.getSampler().isLooping()) return true;
        }
        return false;
    }

    // @Override
    public synchronized void stopAll() {
        stop();
    }

    // ------------------------------------------------------------------------
    // Backward-compatible playSample(...) overloads
    //
    // Standard argument order: 
    // (int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch, float pan)
    //
    // ------------------------------------------------------------------------

    public synchronized int playSample(int samplePos, int sampleLen, float amplitude,
                                       ADSRParams env, float pitch, float pan) {
        return play(samplePos, sampleLen, amplitude, env, pitch, pan);
    }

	public synchronized int playSample(int samplePos, int sampleLen, float amplitude, 
			float pitch, float pan) {
		return play(samplePos, sampleLen, amplitude, defaultEnv, pitch, pan);
	}

	public synchronized int playSample(int samplePos, int sampleLen, float amplitude) {
        return play(samplePos, sampleLen, amplitude, defaultEnv, globalPitch, globalPan);
    }

    public synchronized int playSample(int samplePos, int sampleLen, float amplitude, float pitch) {
        return play(samplePos, sampleLen, amplitude, defaultEnv, pitch, globalPan);
    }

    public synchronized int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
        return play(samplePos, sampleLen, amplitude, env, globalPitch, globalPan);
    }

    public synchronized int playSample(int samplePos, int sampleLen, float amplitude,
                                       ADSRParams env, float pitch) {
        return play(samplePos, sampleLen, amplitude, env, pitch, globalPan);
    }
    
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
    }

    /**
     * Swap the pool's shared buffer and update its buffer sample rate.
     * 
     * @param newBuffer
     * @param newBufferSampleRate
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
    }
    
    /**
     * Propagate a new float[] buffer to all instruments in this pool.
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
    }

    /** Re-sync instruments to current AudioOutput sample rate (if device changes). */
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

    public synchronized void setGlobalPitch(float pitch) { this.globalPitch = pitch; }
    public synchronized float getGlobalPitch() { return globalPitch; }

    public synchronized void setGlobalPan(float pan) { this.globalPan = clampPan(pan); }
    public synchronized float getGlobalPan() { return globalPan; }

    public synchronized void setDefaultEnv(ADSRParams env) {
        this.defaultEnv = (env != null) ? env : this.defaultEnv;
        for (PASamplerInstrument inst : pool) inst.setDefaultEnv(this.defaultEnv);
    }
    public synchronized ADSRParams getDefaultEnv() { return defaultEnv; }
    
    public int getMaxVoices() { return maxVoices; }
    public synchronized void setMaxVoices(int maxVoices) {
        this.maxVoices = Math.max(1, maxVoices);
        reinitInstruments();
    }
    
    public synchronized int getPoolSize() { return pool.size(); }
    public synchronized void setPoolSize(int newSize) {
        if (newSize != this.poolSize) {
            this.poolSize = Math.max(1, newSize);
            reinitInstruments();
        }
    }

    public synchronized float getOutputSampleRate() { return outputSampleRate; }
    public synchronized int getOutputBufferSize() { return outputBufferSize; }
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
        }
    }

    public List<PASamplerInstrument> getInstruments() {
        return pool;
    }

    // ------------------------------------------------------------------------
    // TimedLocation hooks (stubs)
    // ------------------------------------------------------------------------

 	/** Hook: schedule by (future) wallclock ms — will be wired to TimedLocation later. */
    public synchronized void schedulePlayAtMillis(long triggerTimeMillis,
                                                  int samplePos, int sampleLen,
                                                  float amplitude, ADSRParams env,
                                                  float pitch, float pan) 
    {
        // Stub: immediate play for now. TimedLocation will call into here later.
        playSample(samplePos, sampleLen, amplitude, env, pitch, pan);
    }

    /** Hook: schedule by audio-frame index (frame = one AudioOutput callback). */
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

    /** Check whether the pool has been closed. */
    public synchronized boolean isClosed() { return isClosed; }

}
