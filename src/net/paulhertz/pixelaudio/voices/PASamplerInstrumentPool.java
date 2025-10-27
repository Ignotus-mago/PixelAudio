package net.paulhertz.pixelaudio.voices;

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;

import java.util.*;

/**
 * PASamplerInstrumentPool
 *
 * Manages a pool of PASamplerInstrument instances for polyphonic playback.
 * - Backward-compatible playSample(...) overloads
 * - Deterministic round-robin allocation
 * - Pool-level global pitch/pan
 * - Buffer + sample-rate propagation
 * - "TimedLocation hooks" for future scheduling integration
 */
public class PASamplerInstrumentPool implements PASamplerPlayable, PAPlayable {

    // ------------------------------------------------------------------------
    // Core state
    // ------------------------------------------------------------------------
    private final AudioOutput out;
    private MultiChannelBuffer buffer;

    private final List<PASamplerInstrument> pool = new ArrayList<>();
    private int poolSize;
    private int rrIndex = 0; // round-robin pointer

    // Pool-wide defaults / modifiers
    private ADSRParams defaultEnv;
    private float globalPitch = 1.0f; // multiplier
    private float globalPan = 0.0f;   // -1..+1

    // Output / buffer timing info (useful now; essential for TimedLocation later)
    private float outputSampleRate;
    private int outputBufferSize;     // frames per audio callback
    private float bufferSampleRate;   // nominal rate of the currently assigned buffer

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

 // ------------------------------------------------------------------------
 // Constructors
 // ------------------------------------------------------------------------

    /**
     * Full backward-compatible constructor.
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
    	this.bufferSampleRate = sampleRate;
    	this.outputSampleRate = (out != null) ? out.sampleRate() : sampleRate;
    	this.outputBufferSize = (out != null) ? out.bufferSize() : 1024;
    	this.defaultEnv = (defaultEnv != null)
    			? defaultEnv
    					: new ADSRParams(1f, 0.01f, 0.2f, 0.8f, 0.3f);
    	this.globalPitch = 1.0f;
    	this.globalPan = 0.0f;

    	// Initialize instruments
    	pool.clear();
    	for (int i = 0; i < this.poolSize; i++) {
    		PASamplerInstrument inst = new PASamplerInstrument(
    				buffer,
    				sampleRate,
    				perInstrumentVoices,
    				out,
    				this.defaultEnv
    				);
    		pool.add(inst);
    	}

    	rrIndex = 0;
    }

 /**
     * Full constructor.
     *
     * @param out               target AudioOutput
     * @param buffer            shared MultiChannelBuffer
     * @param poolSize          number of instruments to preallocate
     * @param bufferSampleRate  nominal sample rate of buffer (Hz)
     * @param env               default ADSR (nullable)
     */
    public PASamplerInstrumentPool(AudioOutput out,
                                   MultiChannelBuffer buffer,
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

        initializePool();
    }

    /**
     * Convenience constructor: default env, bufferSampleRate = out.sampleRate().
     */
    public PASamplerInstrumentPool(AudioOutput out,
                                   MultiChannelBuffer buffer,
                                   int poolSize)
    {
        this(out, buffer, poolSize, out.sampleRate(),
             new ADSRParams(1f, 0.01f, 0.2f, 0.8f, 0.3f));
    }

    private void initializePool() {
        pool.clear();
        for (int i = 0; i < poolSize; i++) {
            // Create an instrument and force it to this pool's buffer + nominal rate
            PASamplerInstrument inst = new PASamplerInstrument(out, buffer);
            inst.setDefaultEnv(defaultEnv);
            inst.setPitchScale(1.0f);               // instrument-local baseline
            inst.setGlobalPan(0.0f);                 // instrument-local baseline
            inst.setBuffer(buffer, bufferSampleRate);
            pool.add(inst);
        }
        rrIndex = 0;
    }

    // ------------------------------------------------------------------------
    // Allocation (round-robin)
    // ------------------------------------------------------------------------

    /** Deterministic round-robin selection. Instruments can be re-triggered polyphonically. */
    protected synchronized PASamplerInstrument nextInstrument() {
        if (pool.isEmpty()) return null;
        PASamplerInstrument inst = pool.get(rrIndex);
        rrIndex = (rrIndex + 1) % pool.size();
        return inst;
    }

    // ------------------------------------------------------------------------
    // PAPlayable implementation (simple play)
    // ------------------------------------------------------------------------

    @Override
    public synchronized int play(float amplitude, float pitch, float pan) {
        PASamplerInstrument inst = nextInstrument();
        if (inst == null || buffer == null) return 0;

        int sampleLen = buffer.getBufferSize();
        float scaledPitch = pitch * globalPitch;
        float finalPan = clampPan(globalPan + pan);

        return inst.playSample(0, sampleLen, amplitude, defaultEnv, scaledPitch, finalPan);
    }

    @Override
    public synchronized void stop() {
        for (PASamplerInstrument inst : pool) {
            inst.stop();
        }
    }

    // ------------------------------------------------------------------------
    // PASamplerPlayable implementation (detailed play)
    // ------------------------------------------------------------------------

    @Override
    public synchronized int play(int samplePos, int sampleLen, float amplitude,
                                 ADSRParams env, float pitch, float pan) {
        PASamplerInstrument inst = nextInstrument();
        if (inst == null) return 0;

        float scaledPitch = pitch * globalPitch;
        float finalPan = clampPan(globalPan + pan);
        ADSRParams useEnv = (env != null) ? env : defaultEnv;

        return inst.playSample(samplePos, sampleLen, amplitude, useEnv, scaledPitch, finalPan);
    }

    // @Override
    public synchronized boolean isLooping() {
        // Pool-level loop status: true if any sampler reports looping
        for (PASamplerInstrument inst : pool) {
            PASampler s = inst.getSampler();
            if (s != null && s.isLooping()) return true;
        }
        return false;
    }

    // @Override
    public synchronized void stopAll() {
        stop();
    }

    // ------------------------------------------------------------------------
    // Backward-compatible playSample(...) overloads
    // ------------------------------------------------------------------------

    public synchronized int playSample(int samplePos, int sampleLen, float amplitude,
                                       ADSRParams env, float pitch, float pan) {
        return play(samplePos, sampleLen, amplitude, env, pitch, pan);
    }

    public synchronized int playSample(int samplePos, int sampleLen, float amplitude) {
        return play(samplePos, sampleLen, amplitude, defaultEnv, globalPitch, 0.0f);
    }

    public synchronized int playSample(int samplePos, int sampleLen, float amplitude, float pitch) {
        return play(samplePos, sampleLen, amplitude, defaultEnv, pitch, 0.0f);
    }

    public synchronized int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
        return play(samplePos, sampleLen, amplitude, env, globalPitch, globalPan);
    }

    public synchronized int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
                                       float amplitude, ADSRParams env, float pitch, float pan) {
        // Use this call to temporarily switch the instrument's buffer for the event.
        PASamplerInstrument inst = nextInstrument();
        if (inst == null) return 0;

        inst.setBuffer(buffer, bufferSampleRate); // assumes same nominal rate; call setBufferRate(...) first if needed
        float scaledPitch = pitch * globalPitch;
        float finalPan = clampPan(globalPan + pan);
        ADSRParams useEnv = (env != null) ? env : defaultEnv;

        return inst.playSample(samplePos, sampleLen, amplitude, useEnv, scaledPitch, finalPan);
    }

    public synchronized int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
                                       float amplitude, ADSRParams env, float pitch) {
        return playSample(buffer, samplePos, sampleLen, amplitude, env, pitch, globalPan);
    }

    public synchronized int playSample(int samplePos, int sampleLen, float amplitude,
                                       ADSRParams env, float pitch) {
        return playSample(samplePos, sampleLen, amplitude, env, pitch, globalPan);
    }

    // ------------------------------------------------------------------------
    // Buffer & sample-rate propagation
    // ------------------------------------------------------------------------

    /** Swap the pool's shared buffer; keeps existing bufferSampleRate. */
    public synchronized void setBuffer(MultiChannelBuffer newBuffer) {
        if (newBuffer == null) return;
        this.buffer = newBuffer;
        for (PASamplerInstrument inst : pool) {
            inst.setBuffer(newBuffer);
        }
    }

    /** Swap the pool's shared buffer and update its nominal sample rate. */
    public synchronized void setBuffer(MultiChannelBuffer newBuffer, float newBufferSampleRate) {
        if (newBuffer == null) return;
        this.buffer = newBuffer;
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
    // Global modifiers
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
    // Diagnostics
    // ------------------------------------------------------------------------

    public synchronized int getPoolSize() { return pool.size(); }
    public synchronized float getOutputSampleRate() { return outputSampleRate; }
    public synchronized int getOutputBufferSize() { return outputBufferSize; }
    public synchronized float getBufferSampleRate() { return bufferSampleRate; }

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

    	for (PASamplerInstrument inst : pool) {
    		inst.close();
    	}

    	pool.clear();
    	buffer = null;

    	isClosed = true;
    }

    /** Check whether the pool has been closed. */
    public synchronized boolean isClosed() { return isClosed; }

}
