package net.paulhertz.pixelaudio.voices;

import ddf.minim.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PASamplerInstrumentPoolMulti
 *
 * Manages multiple PASamplerInstrumentPool instances, one per MultiChannelBuffer.
 * Supports:
 *  - Lazy or explicit creation of per-buffer subpools
 *  - Per-buffer stereo positioning and looping
 *  - Optional strict mode: only preallocated buffers may be added
 *  - Preallocation utilities for performance-critical workflows
 *  - Tag-based lookup, playback, and control for easier live use
 *  
 * This refactored version removes unnecessary synchronization on playback, 
 * uses concurrent maps for safe parallel access, and relies on the improved
 * PASamplerInstrumentPool (map-based pan and stable scheduling).
 * 
 * https://chatgpt.com/c/68fe47f3-8800-832d-873f-b8f4e8c0a294
 *  
 */
public class PASamplerInstrumentPoolMulti implements PASamplerPlayable {

    // ------------------------------------------------------------------------
    // Shared configuration
    // ------------------------------------------------------------------------

    private final AudioOutput out;
    private final float sampleRate;
    private final ADSRParams defaultEnv;

    // Use concurrent maps for safe, lock-free access
    private final Map<MultiChannelBuffer, PASamplerInstrumentPool> pools = new ConcurrentHashMap<>();
    private final Map<MultiChannelBuffer, PoolConfig> poolConfigs = new ConcurrentHashMap<>();

    private final List<MultiChannelBuffer> preallocatedBuffers = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, MultiChannelBuffer> tagMap = new ConcurrentHashMap<>();

    private volatile float pitchScale = 1.0f;
    private volatile boolean isClosed = false;
    private final boolean isStrict;

    private int defaultPoolSize = 8;
    private int defaultVoicesPerInstrument = 1;

    // ------------------------------------------------------------------------
    // PoolConfig inner class (unchanged)
    // ------------------------------------------------------------------------
    private static class PoolConfig {
        int poolSize;
        int voicesPerInstrument;
        float basePan;

        PoolConfig(int poolSize, int voicesPerInstrument, float basePan) {
            this.poolSize = poolSize;
            this.voicesPerInstrument = voicesPerInstrument;
            this.basePan = basePan;
        }
    }

    // ------------------------------------------------------------------------
    // Constructors (unchanged)
    // ------------------------------------------------------------------------

    public PASamplerInstrumentPoolMulti(float sampleRate, AudioOutput out, ADSRParams env) {
        this(sampleRate, out, env, false);
    }

    public PASamplerInstrumentPoolMulti(float sampleRate, AudioOutput out, ADSRParams env, boolean isStrict) {
        this.sampleRate = sampleRate;
        this.out = out;
        this.defaultEnv = env;
        this.isStrict = isStrict;
    }

    public PASamplerInstrumentPoolMulti(MultiChannelBuffer buffer1,
                                        float sampleRate,
                                        AudioOutput out,
                                        int poolSize,
                                        int perInstrumentVoices,
                                        ADSRParams adsr,
                                        boolean isStrict) {
        this(sampleRate, out, adsr, isStrict);
        addPool(buffer1, poolSize, perInstrumentVoices, 0.0f);
    }

    // ------------------------------------------------------------------------
    // Pool management (minor concurrency-safe tweaks)
    // ------------------------------------------------------------------------

    public void addPool(MultiChannelBuffer buffer, int poolSize, int voices, float basePan) {
        if (isClosed) return;

        if (isStrict && !preallocatedBuffers.contains(buffer)) {
            throw new IllegalStateException("Strict mode: buffer must be preallocated before adding to pool.");
        }

        PASamplerInstrumentPool existing = pools.remove(buffer);
        if (existing != null) existing.close();

        PASamplerInstrumentPool pool =
            new PASamplerInstrumentPool(buffer, sampleRate, poolSize, voices, out, defaultEnv);
        pool.setPitchScale(pitchScale);

        // Apply base pan directly — safe with new map-based pan handling
        for (int i = 0; i < poolSize; i++) {
            pool.setPanForInstrument(i, basePan);
        }

        pools.put(buffer, pool);
        poolConfigs.put(buffer, new PoolConfig(poolSize, voices, basePan));
    }

    private PASamplerInstrumentPool getOrCreatePool(MultiChannelBuffer buffer) {
        if (isClosed) return null;

        PASamplerInstrumentPool pool = pools.get(buffer);
        if (pool == null) {
            if (isStrict) {
                throw new IllegalStateException("Strict mode: cannot create new pool for unallocated buffer.");
            }
            PoolConfig cfg = poolConfigs.computeIfAbsent(buffer,
                b -> new PoolConfig(defaultPoolSize, defaultVoicesPerInstrument, 0.0f));

            pool = new PASamplerInstrumentPool(buffer, sampleRate, cfg.poolSize, cfg.voicesPerInstrument, out, defaultEnv);
            pool.setPitchScale(pitchScale);

            for (int i = 0; i < cfg.poolSize; i++) {
                pool.setPanForInstrument(i, cfg.basePan);
            }

            pools.put(buffer, pool);
        }
        return pool;
    }

    // ------------------------------------------------------------------------
    // Playback delegation (no global synchronization)
    // ------------------------------------------------------------------------

    @Override
    public int playSample(MultiChannelBuffer buffer,
                          int samplePos,
                          int sampleLen,
                          float amplitude,
                          ADSRParams env,
                          float pitch,
                          float pan) {
        if (isClosed || buffer == null) return 0;
        PASamplerInstrumentPool pool = getOrCreatePool(buffer);
        return (pool != null)
                ? pool.playSample(samplePos, sampleLen, amplitude, env, pitch, pan)
                : 0;
    }

    @Override
    public int playSample(MultiChannelBuffer buffer,
                          int samplePos,
                          int sampleLen,
                          float amplitude,
                          ADSRParams env,
                          float pitch) {
        return playSample(buffer, samplePos, sampleLen, amplitude, env, pitch, 0.0f);
    }

    @Override
    public int playSample(int samplePos,
                          int sampleLen,
                          float amplitude,
                          ADSRParams env,
                          float pitch,
                          float pan) {
        if (isClosed) return 0;
        if (pools.size() == 1) {
            PASamplerInstrumentPool only = pools.values().iterator().next();
            return only.playSample(samplePos, sampleLen, amplitude, env, pitch, pan);
        }
        throw new UnsupportedOperationException("Must supply a MultiChannelBuffer when multiple pools exist.");
    }

    @Override
    public int playSample(int samplePos,
                          int sampleLen,
                          float amplitude,
                          ADSRParams env,
                          float pitch) {
        return playSample(samplePos, sampleLen, amplitude, env, pitch, 0.0f);
    }

    @Override
    public int playSample(int samplePos,
                          int sampleLen,
                          float amplitude,
                          ADSRParams env) {
        return playSample(samplePos, sampleLen, amplitude, env, pitchScale, 0.0f);
    }

    public int playSample(int samplePos,
                          int sampleLen,
                          float amplitude) {
        return playSample(samplePos, sampleLen, amplitude, defaultEnv, pitchScale, 0.0f);
    }
    // ------------------------------------------------------------------------
    // TAGGING UTILITIES
    // ------------------------------------------------------------------------

    public synchronized void tagBuffer(String tag, MultiChannelBuffer buffer) {
        if (tag == null || buffer == null) return;
        tagMap.put(tag.toLowerCase(), buffer);
    }

    public synchronized void untagBuffer(String tag) {
        if (tag == null) return;
        tagMap.remove(tag.toLowerCase());
    }

    public synchronized MultiChannelBuffer getBufferByTag(String tag) {
        if (tag == null) return null;
        return tagMap.get(tag.toLowerCase());
    }

    public synchronized Set<String> getAllTags() {
        return new HashSet<>(tagMap.keySet());
    }

    public synchronized void renameTag(String oldTag, String newTag) {
        if (oldTag == null || newTag == null) return;
        MultiChannelBuffer buffer = tagMap.remove(oldTag.toLowerCase());
        if (buffer != null) tagMap.put(newTag.toLowerCase(), buffer);
    }

    // ------------------------------------------------------------------------
    // TAG QUERY HELPERS
    // ------------------------------------------------------------------------

    public synchronized boolean hasTag(String tag) {
        if (tag == null) return false;
        return tagMap.containsKey(tag.toLowerCase());
    }

    public synchronized boolean isTagLooping(String tag) {
        MultiChannelBuffer buffer = getBufferByTag(tag);
        if (buffer == null) return false;
        PASamplerInstrumentPool pool = pools.get(buffer);
        if (pool == null) return false;
        return pool.isAnyInstrumentLooping();
    }
    
    public synchronized String describeTag(String tag) {
        MultiChannelBuffer buffer = getBufferByTag(tag);
        if (buffer == null) return "Tag '" + tag + "' not found.";
        PoolConfig cfg = poolConfigs.get(buffer);
        if (cfg == null) return "No pool config for tag '" + tag + "'.";
        return String.format(
            "Tag: %s | Pool Size: %d | Voices: %d | Pan: %.2f | Looping: %b | Pitch: %.2f",
            tag, cfg.poolSize, cfg.voicesPerInstrument, cfg.basePan,
            isTagLooping(tag), getPitchScale(tag)
        );
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
        MultiChannelBuffer buffer = getBufferByTag(tag);
        if (buffer == null)
            throw new IllegalArgumentException("No buffer found for tag: " + tag);
        return playSample(buffer, samplePos, sampleLen, amplitude, env, pitch, pan);
    }

    public synchronized int playSample(String tag,
                                       int samplePos,
                                       int sampleLen,
                                       float amplitude,
                                       ADSRParams env) {
        return playSample(tag, samplePos, sampleLen, amplitude, env, pitchScale, 0.0f);
    }

    public synchronized int playSample(String tag,
                                       int samplePos,
                                       int sampleLen,
                                       float amplitude) {
        return playSample(tag, samplePos, sampleLen, amplitude, defaultEnv, pitchScale, 0.0f);
    }

    public synchronized void setBasePan(String tag, float pan) {
        MultiChannelBuffer buffer = getBufferByTag(tag);
        if (buffer == null) return;
        setBasePan(buffer, pan);
    }

    public synchronized void setIsLooping(String tag, boolean looping) {
        MultiChannelBuffer buffer = getBufferByTag(tag);
        if (buffer == null) return;
        PASamplerInstrumentPool pool = pools.get(buffer);
        if (pool != null) pool.setIsLooping(looping);
    }

    public synchronized void stopLoops(String tag) {
        MultiChannelBuffer buffer = getBufferByTag(tag);
        if (buffer == null) return;
        PASamplerInstrumentPool pool = pools.get(buffer);
        if (pool != null) pool.stopAllLoops();
    }

    public synchronized void setPitchScale(String tag, float scale) {
        MultiChannelBuffer buffer = getBufferByTag(tag);
        if (buffer == null) return;
        PASamplerInstrumentPool pool = pools.get(buffer);
        if (pool != null) pool.setPitchScale(scale);
    }

    public synchronized float getPitchScale(String tag) {
        MultiChannelBuffer buffer = getBufferByTag(tag);
        if (buffer == null) return pitchScale;
        PASamplerInstrumentPool pool = pools.get(buffer);
        return (pool != null) ? pool.getPitchScale() : pitchScale;
    }

    // ------------------------------------------------------------------------
    // GLOBAL CONTROLS
    // ------------------------------------------------------------------------

    @Override
    public synchronized void setPitchScale(float scale) {
        if (scale <= 0) throw new IllegalArgumentException("Pitch scale must be positive.");
        this.pitchScale = scale;
        for (PASamplerInstrumentPool pool : pools.values()) pool.setPitchScale(scale);
    }

    @Override
    public synchronized float getPitchScale() {
        return pitchScale;
    }

    public synchronized void setIsLooping(boolean looping) {
        for (PASamplerInstrumentPool pool : pools.values()) pool.setIsLooping(looping);
    }

    public synchronized void stopAllLoops() {
        for (PASamplerInstrumentPool pool : pools.values()) pool.stopAllLoops();
    }

    public synchronized void setBasePan(MultiChannelBuffer buffer, float pan) {
        PASamplerInstrumentPool pool = pools.get(buffer);
        if (pool != null) {
            PoolConfig cfg = poolConfigs.get(buffer);
            if (cfg != null) cfg.basePan = clamp(pan);
            for (int i = 0; i < cfg.poolSize; i++) pool.setPanForInstrument(i, clamp(pan));
        }
    }

    private static float clamp(float v) {
        return Math.max(-1.0f, Math.min(1.0f, v));
    }

    // ------------------------------------------------------------------------
    // PREALLOCATION UTILITIES
    // ------------------------------------------------------------------------

    public List<MultiChannelBuffer> preallocateBuffers(int count, int channels, int bufferSize) {
        List<MultiChannelBuffer> created = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            MultiChannelBuffer b = new MultiChannelBuffer(bufferSize, channels);
            synchronized (preallocatedBuffers) {
                preallocatedBuffers.add(b);
            }
            tagMap.put("prealloc_" + i, b);
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
        addPool(b, poolSize, defaultVoicesPerInstrument, 0.0f);
        return b;
    }

    public MultiChannelBuffer addPreallocatedBufferToPool(int index) {
        return addPreallocatedBufferToPool(index, defaultPoolSize);
    }

    // ------------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------------

    public synchronized void close() {
        if (isClosed) return;
        isClosed = true;
        for (PASamplerInstrumentPool pool : pools.values()) pool.close();
        pools.clear();
        poolConfigs.clear();
        preallocatedBuffers.clear();
        tagMap.clear();
    }

    public boolean isClosed() {
        return isClosed;
    }

    // ------------------------------------------------------------------------
    // Diagnostics / utilities
    // ------------------------------------------------------------------------

    public synchronized Set<MultiChannelBuffer> getLoadedBuffers() {
        return new HashSet<>(pools.keySet());
    }

    public synchronized PASamplerInstrumentPool getPool(MultiChannelBuffer buffer) {
        return pools.get(buffer);
    }

    public synchronized void removePool(MultiChannelBuffer buffer) {
        PASamplerInstrumentPool pool = pools.remove(buffer);
        if (pool != null) pool.close();
        poolConfigs.remove(buffer);
    }

    public synchronized void setDefaultPoolConfig(int poolSize, int voicesPerInstrument) {
        this.defaultPoolSize = poolSize;
        this.defaultVoicesPerInstrument = voicesPerInstrument;
    }
    
    public void debugPools() {
        pools.forEach((buffer, pool) -> {
            System.out.printf("Buffer %s → Available=%d, InUse=%d, Looping=%b%n",
                buffer.hashCode(),
                pool.getAvailableCount(),
                pool.getInUseCount(),
                pool.isAnyInstrumentLooping());
        });
    }
    
}
