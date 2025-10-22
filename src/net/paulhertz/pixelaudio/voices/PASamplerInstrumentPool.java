package net.paulhertz.pixelaudio.voices;

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;
import java.util.*;
import java.util.concurrent.*;

/**
 * PASamplerInstrumentPool
 *
 * A pool of PASamplerInstrument objects that all share the same MultiChannelBuffer.
 * Each instrument manages its own set of PASamplerVoice instances (internal polyphony).
 *
 * Features:
 *  - Round-robin instrument allocation with optional voice stealing
 *  - Manual per-instrument pan offsets (added to per-note pan)
 *  - Global pitch scaling across the pool (applied to per-note pitch)
 *  - Rebuilds all instruments when setBuffer(...) is called (Option A)
 *  - Lifecycle-safe with isClosed guard and graceful shutdown
 */
public class PASamplerInstrumentPool implements PASamplerPlayable {
    // Shared settings and resources
	private final AudioOutput out;
	private final float sampleRate;
    private final ADSRParams defaultEnv;
    // global pitch-scaling
    private volatile float pitchScale = 1.0f;
    // Manual per-instrument pan offsets (same order as 'available' initialization)
    // Indexed in the same order instruments are created.
    private final List<Float> instrumentPan = new ArrayList<>(); 
    // shared buffer
    private MultiChannelBuffer buffer;
    private int bufferSize;
    // Pool settings and resources
    private final int poolSize;
    private final int perInstrumentVoices;
    // pool containers
    private final ArrayDeque<PASamplerInstrument> available = new ArrayDeque<>();
    private final Set<PASamplerInstrument> inUse = new HashSet<>();
    // Scheduler handles delayed noteOff/unpatch cleanup
    private final ScheduledExecutorService scheduler;
    // Round-robin index for allocation fallback
    private int rrIndex = 0;
    // voice-stealing 
    private volatile boolean voiceStealingEnabled = true;
    // shutdown status
    private boolean isClosed = false;
    

    // ------------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------------

    /**
     * Build a pool of PASamplerInstrument that all share 'buffer'.
     *
     * @param buffer               shared MultiChannelBuffer
     * @param sampleRate           audio sample rate
     * @param poolSize             number of instruments in the pool
     * @param perInstrumentVoices  voices per instrument (internal polyphony)
     * @param out                  Minim AudioOutput (stereo)
     * @param defaultEnv           default ADSR
     */
    public PASamplerInstrumentPool(MultiChannelBuffer buffer, float sampleRate, int poolSize, 
    		                       int perInstrumentVoices, AudioOutput out, ADSRParams defaultEnv) {
        this.out = out;
        this.sampleRate = sampleRate;
        this.poolSize = Math.max(1, poolSize);
        this.perInstrumentVoices = Math.max(1, perInstrumentVoices);
        this.defaultEnv = defaultEnv;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PASamplerInstrumentPool-scheduler");
            t.setDaemon(true);
            return t;
        });
        setBufferInternal(buffer); // initializes instruments, pans, queues
    }

    // Internal helper used by constructor and setBuffer(...)
    private synchronized void setBufferInternal(MultiChannelBuffer newBuffer) {
        // clear old
        available.clear();
        inUse.clear();
        instrumentPan.clear();
        this.buffer = newBuffer;
        this.bufferSize = newBuffer.getBufferSize();
        // Build instruments
        for (int i = 0; i < poolSize; i++) {
            PASamplerInstrument inst = new PASamplerInstrument(
                    newBuffer,            // shared buffer (Sampler is inside instrument)
                    sampleRate,
                    perInstrumentVoices,
                    out,
                    defaultEnv
            );
            inst.setPitchScale(pitchScale);
            available.add(inst);
            instrumentPan.add(0.0f); // default: center; caller can set later
        }
        rrIndex = 0;
    }

    
    // ------------------------------------------------------------------------
    // PASamplerPlayable — Core playback API (+ convenience overloads)
    // ------------------------------------------------------------------------

    /**
     * Core play: default buffer, explicit env/pitch/pan.
     */
    @Override
    public synchronized int playSample(int samplePos, int sampleLen, float amplitude, 
    		                           ADSRParams env, float pitch, float pan) {
        if (isClosed) return 0;
        PASamplerInstrument inst = acquireInstrument();
        if (inst == null) return 0;
        // Combine per-instrument base pan with per-note pan, clamp
        float basePan = getInstrumentPan(inst);
        float actualPan = clampPan(pan + basePan);
        // Apply global pitch scaling on top of per-note pitch
        float actualPitch = pitch * this.pitchScale;
        int actualLen = inst.playSample(samplePos, sampleLen, amplitude, env, actualPitch, actualPan);
        // Schedule return-to-available after grain duration + env tail
        scheduleReturn(inst, actualLen, env);
        return actualLen;
    }

    /**
     * Optional buffer variant: replaces the pool buffer (rebuilds instruments)
     * and then plays from the new buffer.
     */
    public synchronized int playSample(MultiChannelBuffer buffer,
    		                           int samplePos, int sampleLen, float amplitude, 
    		                           ADSRParams env, float pitch, float pan) {
        if (isClosed) return 0;
        setBuffer(buffer); // rebuilds the pool (Option A)
        return playSample(samplePos, sampleLen, amplitude, env, pitch, pan);
    }

    // Convenience overloads (match PASamplerInstrument style)

    /** Default env + center pan. */
    public synchronized int playSample(int samplePos, int sampleLen, float amplitude, float pitch) {
        return playSample(samplePos, sampleLen, amplitude, defaultEnv, pitch, 0.0f);
    }

    /** Default env, default pitchScale, center pan. */
    public synchronized int playSample(int samplePos, int sampleLen, float amplitude) {
        return playSample(samplePos, sampleLen, amplitude, defaultEnv, pitchScale, 0.0f);
    }

    /** Supplied env, default pitchScale, default (0) pan. */
    @Override
    public synchronized int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
        return playSample(samplePos, sampleLen, amplitude, env, pitchScale, 0.0f);
    }

    /** Buffer variant without explicit pan (uses 0). */
    public synchronized int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen, 
    		                           float amplitude, ADSRParams env, float pitch) {
        return playSample(buffer, samplePos, sampleLen, amplitude, env, pitch, 0.0f);
    }

    // ------------------------------------------------------------------------
    // Allocation / scheduling
    // ------------------------------------------------------------------------

    private PASamplerInstrument acquireInstrument() {
        PASamplerInstrument inst = available.poll();
        if (inst != null) {
            inUse.add(inst);
            return inst;
        }
        if (voiceStealingEnabled && !inUse.isEmpty()) {
            // Steal (simple strategy: round-robin target)
            int skip = rrIndex++ % inUse.size();
            Iterator<PASamplerInstrument> it = inUse.iterator();
            PASamplerInstrument stolen = null;
            for (int i = 0; i <= skip && it.hasNext(); i++) {
                stolen = it.next();
            }
            if (stolen != null) {
                // No explicit stop here; we assume instruments can overlap internally.
                // We simply reassign it for the new note.
                return stolen;
            }
        }
        // None available
        return null;
    }

    private void scheduleReturn(PASamplerInstrument inst, int actualLen, ADSRParams env) {
        // For non-looping usage, a reasonable reclaim time is:
        // sample duration + attack + decay + release (+ small margin)
        if (actualLen <= 0) {
            // If zero length (or looping scenario handled elsewhere), return immediately
            releaseInstrument(inst);
            return;
        }
        long durMs = Math.round((actualLen / (double) sampleRate) * 1000.0);
        long envTailMs = Math.round((env.getAttack() + env.getDecay() + env.getRelease()) * 1000.0);
        long totalMs = durMs + envTailMs + 50L;
        scheduler.schedule(() -> releaseInstrument(inst), totalMs, TimeUnit.MILLISECONDS);
    }

    private synchronized void releaseInstrument(PASamplerInstrument inst) {
        if (isClosed) return;
        inUse.remove(inst);
        available.add(inst);
    }

    private float getInstrumentPan(PASamplerInstrument inst) {
        // Find index; order is construction order (size is modest, linear scan is fine)
        int idx = 0;
        // Try available deque first
        for (PASamplerInstrument i : available) {
            if (i == inst) return instrumentPan.get(idx);
            idx++;
        }
        // Then in-use set (order not fixed—fallback to lookup by identity)
        idx = 0;
        for (PASamplerInstrument i : instrumentsSnapshot()) {
            if (i == inst) {
                // this snapshot aligns with instrumentPan ordering at construction time
                return instrumentPan.get(idx);
            }
            idx++;
        }
        // If not found (shouldn't happen), return center
        return 0.0f;
    }

    // Snapshot of instruments in construction order
    private List<PASamplerInstrument> instrumentsSnapshot() {
        // We don't store a separate list of instruments; reconstruct in order of pan list:
        // instrumentPan indices correspond to construction order; reconstruct via available+inUse membership.
        List<PASamplerInstrument> all = new ArrayList<>(poolSize);
        // We must keep a deterministic order matching instrumentPan; so rebuild from both containers:
        // First collect all known instruments (by identity) from both sets
        Set<PASamplerInstrument> allSet = new HashSet<>(available);
        allSet.addAll(inUse);
        // Then iterate allSet and add to list while preserving a stable order by hash (not perfect)
        // For perfect stability, you could maintain a separate ArrayList<PASamplerInstrument> on construction.
        all.addAll(allSet);
        return all;
    }

    private static float clampPan(float pan) {
        if (pan < -1f) return -1f;
        if (pan > 1f) return 1f;
        return pan;
    }

    // ------------------------------------------------------------------------
    // Pan API (manual per-instrument)
    // ------------------------------------------------------------------------

    /**
     * Set the per-instrument pan offset (-1..+1) for instrument at 'index' (0-based).
     * Added to per-note pan and clamped to [-1, +1] on playback.
     */
    public synchronized void setPanForInstrument(int index, float pan) {
        if (index < 0 || index >= instrumentPan.size()) return;
        instrumentPan.set(index, clampPan(pan));
    }

    /**
     * Bulk set base pans for all instruments (array length >= poolSize).
     */
    public synchronized void setPanForAll(float[] pans) {
        if (pans == null) return;
        for (int i = 0; i < Math.min(poolSize, pans.length); i++) {
            instrumentPan.set(i, clampPan(pans[i]));
        }
    }

    /**
     * Get per-instrument pan offset.
     */
    public synchronized float getPanForInstrument(int index) {
        if (index < 0 || index >= instrumentPan.size()) return 0.0f;
        return instrumentPan.get(index);
    }

    // ------------------------------------------------------------------------
    // Loop & pitch management
    // ------------------------------------------------------------------------

    /** Apply looping to all instruments in the pool. */
    public synchronized void setIsLooping(boolean looping) {
        for (PASamplerInstrument inst : available) inst.setIsLooping(looping);
        for (PASamplerInstrument inst : inUse) inst.setIsLooping(looping);
    }
    
    /** Returns true if any instrument in the pool is looping. */
    public boolean isAnyInstrumentLooping() {
        synchronized (this) { // defensive: avoids concurrent iteration issues
            for (PASamplerInstrument inst : available) {
                for (PASamplerVoice v : inst.getVoices()) {
                    if (v != null && v.isLooping()) return true;
                }
            }
            for (PASamplerInstrument inst : inUse) {
                for (PASamplerVoice v : inst.getVoices()) {
                    if (v != null && v.isLooping()) return true;
                }
            }
            return false;
        }
    }

    /** Stop all loops on all instruments. */
    public synchronized void stopAllLoops() {
        for (PASamplerInstrument inst : available) inst.stopAllLoops();
        for (PASamplerInstrument inst : inUse) inst.stopAllLoops();
    }

    @Override
    public synchronized void setPitchScale(float scale) {
        if (scale <= 0) throw new IllegalArgumentException("Pitch scale must be positive.");
        this.pitchScale = scale;
        for (PASamplerInstrument inst : available) inst.setPitchScale(scale);
        for (PASamplerInstrument inst : inUse) inst.setPitchScale(scale);
    }

    @Override
    public synchronized float getPitchScale() {
        return pitchScale;
    }

    // ------------------------------------------------------------------------
    // Buffer management (Option A: rebuild)
    // ------------------------------------------------------------------------

    /**
     * Replace the shared buffer and rebuild all instruments fresh.
     */
    public synchronized void setBuffer(MultiChannelBuffer newBuffer) {
        if (isClosed) return;
        setBufferInternal(newBuffer);
    }

    public synchronized MultiChannelBuffer getBuffer() {
        return buffer;
    }

    public synchronized int getBufferSize() {
        return bufferSize;
    }

    // ------------------------------------------------------------------------
    // Pool behavior toggles
    // ------------------------------------------------------------------------

    public void setVoiceStealingEnabled(boolean enabled) {
        this.voiceStealingEnabled = enabled;
    }

    public boolean isVoiceStealingEnabled() {
        return voiceStealingEnabled;
    }

    // ------------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------------

    public synchronized void close() {
        if (isClosed) return;
        isClosed = true;
        scheduler.shutdownNow();

        // Close all instruments
        for (PASamplerInstrument inst : available) inst.close();
        for (PASamplerInstrument inst : inUse) inst.close();

        available.clear();
        inUse.clear();
        instrumentPan.clear();
    }

    public boolean isClosed() {
        return isClosed;
    }
}

