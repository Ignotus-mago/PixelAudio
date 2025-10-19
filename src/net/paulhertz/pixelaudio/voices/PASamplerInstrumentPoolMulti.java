package net.paulhertz.pixelaudio.voices;

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple WFSamplerInstrumentPool instances, one per buffer.
 * 
 * Supports lazy pool creation, pitch scaling, and optional preallocation of
 * empty buffers for live performance.
 */
public class PASamplerInstrumentPoolMulti implements PASamplerPlayable {

    private final float sampleRate;
    private final int defaultPoolSize;
    private final int perInstrumentVoices;
    private final AudioOutput out;
    private final ADSRParams defaultADSR;
    private final boolean strictMode; // true = throw if buffer unregistered

    private final ConcurrentHashMap<MultiChannelBuffer, PASamplerInstrumentPool> pools = new ConcurrentHashMap<>();
    private final List<MultiChannelBuffer> preallocatedBuffers =
            Collections.synchronizedList(new ArrayList<>());

    private PASamplerInstrumentPool defaultPool;
    private volatile float pitchScale = 1.0f;

    /**
     * Constructor with default buffer and configuration.
     */
    public PASamplerInstrumentPoolMulti(MultiChannelBuffer defaultBuffer,
                                        float sampleRate,
                                        int defaultPoolSize,
                                        int perInstrumentVoices,
                                        AudioOutput out,
                                        ADSRParams defaultADSR,
                                        boolean strictMode) {
        this.sampleRate = sampleRate;
        this.defaultPoolSize = defaultPoolSize;
        this.perInstrumentVoices = perInstrumentVoices;
        this.out = out;
        this.defaultADSR = defaultADSR;
        this.strictMode = strictMode;

        if (defaultBuffer != null) {
            this.defaultPool = new PASamplerInstrumentPool(defaultBuffer, sampleRate,
                    defaultPoolSize, perInstrumentVoices, out, defaultADSR);
            pools.put(defaultBuffer, defaultPool);
        }
    }

    /** Convenience constructor (strictMode = false). */
    public PASamplerInstrumentPoolMulti(MultiChannelBuffer defaultBuffer,
                                        float sampleRate,
                                        int defaultPoolSize,
                                        int perInstrumentVoices,
                                        AudioOutput out,
                                        ADSRParams defaultADSR) {
        this(defaultBuffer, sampleRate, defaultPoolSize, perInstrumentVoices, out, defaultADSR, false);
    }

    // ---------------------------------------------------------------------
    // BUFFER MANAGEMENT
    // ---------------------------------------------------------------------

    /** Register a buffer explicitly with given pool size. */
    public void addBuffer(MultiChannelBuffer buffer, int poolSize) {
        pools.computeIfAbsent(buffer, b ->
                new PASamplerInstrumentPool(b, sampleRate, poolSize,
                        perInstrumentVoices, out, defaultADSR));
    }

    /** Register a buffer using default pool size. */
    public void addBuffer(MultiChannelBuffer buffer) {
        addBuffer(buffer, defaultPoolSize);
    }

    /** Remove and close the sub-pool for a given buffer. */
    public boolean removeBuffer(MultiChannelBuffer buffer) {
        PASamplerInstrumentPool removed = pools.remove(buffer);
        if (removed != null) {
            removed.close();
            return true;
        }
        return false;
    }

    /** List all registered buffers (snapshot). */
    public Set<MultiChannelBuffer> listBuffers() {
        return Collections.unmodifiableSet(new HashSet<>(pools.keySet()));
    }

    // ---------------------------------------------------------------------
    // PREALLOCATION UTILITIES
    // ---------------------------------------------------------------------

    public List<MultiChannelBuffer> preallocateBuffers(int count, int channels, int bufferSize) {
        List<MultiChannelBuffer> created = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            MultiChannelBuffer b = new MultiChannelBuffer(bufferSize, channels);
            preallocatedBuffers.add(b);
            created.add(b);
        }
        return created;
    }

    public MultiChannelBuffer getPreallocatedBuffer(int index) {
        synchronized (preallocatedBuffers) {
            if (index < 0 || index >= preallocatedBuffers.size()) return null;
            return preallocatedBuffers.get(index);
        }
    }

    public void setBufferContent(int index, MultiChannelBuffer source) {
        MultiChannelBuffer target;
        synchronized (preallocatedBuffers) {
            if (index < 0 || index >= preallocatedBuffers.size())
                throw new IndexOutOfBoundsException("Invalid prealloc index");
            target = preallocatedBuffers.get(index);
        }
        target.set(source);
    }

    public MultiChannelBuffer addPreallocatedBufferToPool(int index, int poolSize) {
        MultiChannelBuffer b = getPreallocatedBuffer(index);
        if (b == null) return null;
        addBuffer(b, poolSize);
        return b;
    }

    public MultiChannelBuffer addPreallocatedBufferToPool(int index) {
        return addPreallocatedBufferToPool(index, defaultPoolSize);
    }

    // ---------------------------------------------------------------------
    // PLAY METHODS
    // ---------------------------------------------------------------------

    /** Default buffer, default pitchScale. */
    @Override
    public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
        if (defaultPool == null)
            throw new IllegalStateException("No default buffer configured.");
        return defaultPool.playSample(samplePos, sampleLen, amplitude, pitchScale, env);
    }

    /** Default buffer, explicit pitch. */
    @Override
    public int playSample(int samplePos, int sampleLen, float amplitude, float pitch, ADSRParams env) {
        if (defaultPool == null)
            throw new IllegalStateException("No default buffer configured.");
        return defaultPool.playSample(samplePos, sampleLen, amplitude, pitch, env);
    }

    /** Explicit buffer, default pitchScale. */
    @Override
    public int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
                          float amplitude, ADSRParams env) {
        return playSample(buffer, samplePos, sampleLen, amplitude, pitchScale, env);
    }

    /** Explicit buffer and explicit pitch. */
    @Override
    public int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
                          float amplitude, float pitch, ADSRParams env) {
        PASamplerInstrumentPool sub = pools.get(buffer);
        if (sub == null) {
            if (strictMode) {
                throw new IllegalArgumentException("Buffer not registered (strict mode).");
            } else {
                sub = new PASamplerInstrumentPool(buffer, sampleRate, defaultPoolSize,
                        perInstrumentVoices, out, defaultADSR);
                PASamplerInstrumentPool existing = pools.putIfAbsent(buffer, sub);
                if (existing != null) {
                    sub.close();
                    sub = existing;
                }
            }
        }
        float effectivePitch = pitch * pitchScale;
        return sub.playSample(samplePos, sampleLen, amplitude, effectivePitch, env);
    }

    // ---------------------------------------------------------------------
    // GLOBAL PITCH CONTROL
    // ---------------------------------------------------------------------

    @Override
    public void setPitchScale(float scale) {
        if (scale <= 0) throw new IllegalArgumentException("Pitch scale must be positive.");
        this.pitchScale = scale;
    }

    @Override
    public float getPitchScale() {
        return pitchScale;
    }

    // ---------------------------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------------------------

    @Override
    public void close() {
        for (PASamplerInstrumentPool pool : pools.values()) {
            try { pool.close(); } catch (Throwable ignored) {}
        }
        pools.clear();
        synchronized (preallocatedBuffers) {
            preallocatedBuffers.clear();
        }
    }
}
