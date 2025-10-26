package net.paulhertz.pixelaudio.voices;

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;
import java.util.*;
import java.util.concurrent.*;

/**
 * PASamplerInstrumentPool (refactored)
 *
 * Same API and behavior as original.
 * Fixes IndexOutOfBoundsException by replacing index-based pan lookup
 * with identity-based mapping.
 */
public class PASamplerInstrumentPool implements PASamplerPlayable {
    // Shared settings and resources
    private final AudioOutput out;
    private final float sampleRate;
    private final ADSRParams defaultEnv;

    // global pitch-scaling
    private volatile float pitchScale = 1.0f;

    // Manual per-instrument pan offsets, identity-based
    private final Map<PASamplerInstrument, Float> instrumentPan = new HashMap<>();

    // shared buffer
    private MultiChannelBuffer buffer;
    private int bufferSize;

    // Pool settings
    private final int poolSize;
    private final int perInstrumentVoices;

    // pool containers
    private final ArrayDeque<PASamplerInstrument> available = new ArrayDeque<>();
    private final Set<PASamplerInstrument> inUse = new HashSet<>();

    // Scheduler handles delayed noteOff/unpatch cleanup
    private final ScheduledExecutorService scheduler;

    // Round-robin index
    private int rrIndex = 0;

    // voice-stealing toggle
    private volatile boolean voiceStealingEnabled = true;

    // shutdown status
    private boolean isClosed = false;

    // ------------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------------

    public PASamplerInstrumentPool(MultiChannelBuffer buffer,
                                   float sampleRate,
                                   int poolSize,
                                   int perInstrumentVoices,
                                   AudioOutput out,
                                   ADSRParams defaultEnv)
    {
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
        setBufferInternal(buffer);
    }

    private synchronized void setBufferInternal(MultiChannelBuffer newBuffer) {
        available.clear();
        inUse.clear();
        instrumentPan.clear();

        this.buffer = newBuffer;
        this.bufferSize = newBuffer.getBufferSize();

        for (int i = 0; i < poolSize; i++) {
            PASamplerInstrument inst = new PASamplerInstrument(
                    newBuffer,
                    sampleRate,
                    perInstrumentVoices,
                    out,
                    defaultEnv
            );
            inst.setPitchScale(pitchScale);
            available.add(inst);
            instrumentPan.put(inst, 0.0f); // default pan center
        }
        rrIndex = 0;
    }

    // ------------------------------------------------------------------------
    // PASamplerPlayable — Core playback API (+ convenience overloads)
    // ------------------------------------------------------------------------

    @Override
    public synchronized int playSample(int samplePos, int sampleLen, float amplitude,
                                       ADSRParams env, float pitch, float pan)
    {
        if (isClosed) return 0;
        PASamplerInstrument inst = acquireInstrument();
        if (inst == null) return 0;

        float basePan = getInstrumentPan(inst);
        float actualPan = clampPan(pan + basePan);
        float actualPitch = pitch * this.pitchScale;

        int actualLen = inst.playSample(samplePos, sampleLen, amplitude, env, actualPitch, actualPan);
        scheduleReturn(inst, actualLen, env);
        return actualLen;
    }

    public synchronized int playSample(MultiChannelBuffer buffer,
                                       int samplePos, int sampleLen, float amplitude,
                                       ADSRParams env, float pitch, float pan)
    {
        if (isClosed) return 0;
        setBuffer(buffer);
        return playSample(samplePos, sampleLen, amplitude, env, pitch, pan);
    }

    public synchronized int playSample(int samplePos, int sampleLen, float amplitude, float pitch) {
        return playSample(samplePos, sampleLen, amplitude, defaultEnv, pitch, 0.0f);
    }

    public synchronized int playSample(int samplePos, int sampleLen, float amplitude) {
        return playSample(samplePos, sampleLen, amplitude, defaultEnv, pitchScale, 0.0f);
    }

    @Override
    public synchronized int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
        return playSample(samplePos, sampleLen, amplitude, env, pitchScale, 0.0f);
    }

    public synchronized int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
                                       float amplitude, ADSRParams env, float pitch)
    {
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
            int skip = rrIndex++ % inUse.size();
            Iterator<PASamplerInstrument> it = inUse.iterator();
            PASamplerInstrument stolen = null;
            for (int i = 0; i <= skip && it.hasNext(); i++) {
                stolen = it.next();
            }
            return stolen;
        }
        return null;
    }

    private void scheduleReturn(PASamplerInstrument inst, int actualLen, ADSRParams env) {
        if (actualLen <= 0) {
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

    // ------------------------------------------------------------------------
    // Fixed pan lookup
    // ------------------------------------------------------------------------

    private float getInstrumentPan(PASamplerInstrument inst) {
        // Identity-based lookup — no index math, no out-of-bounds
        Float pan = instrumentPan.get(inst);
        return (pan != null) ? pan : 0.0f;
    }

    private static float clampPan(float pan) {
        return (pan < -1f) ? -1f : (pan > 1f ? 1f : pan);
    }

    // ------------------------------------------------------------------------
    // Pan API
    // ------------------------------------------------------------------------

    public synchronized void setPanForInstrument(int index, float pan) {
        if (index < 0 || index >= available.size() + inUse.size()) return;
        PASamplerInstrument inst = getInstrumentByIndex(index);
        if (inst != null) instrumentPan.put(inst, clampPan(pan));
    }

    public synchronized void setPanForAll(float[] pans) {
        if (pans == null) return;
        int i = 0;
        for (PASamplerInstrument inst : getAllInConstructionOrder()) {
            if (i >= pans.length) break;
            instrumentPan.put(inst, clampPan(pans[i]));
            i++;
        }
    }

    public synchronized float getPanForInstrument(int index) {
        PASamplerInstrument inst = getInstrumentByIndex(index);
        if (inst == null) return 0.0f;
        return instrumentPan.getOrDefault(inst, 0.0f);
    }

    private PASamplerInstrument getInstrumentByIndex(int index) {
        List<PASamplerInstrument> ordered = getAllInConstructionOrder();
        if (index < 0 || index >= ordered.size()) return null;
        return ordered.get(index);
    }

    private List<PASamplerInstrument> getAllInConstructionOrder() {
        List<PASamplerInstrument> all = new ArrayList<>(poolSize);
        all.addAll(available);
        all.addAll(inUse);
        return all;
    }

    // ------------------------------------------------------------------------
    // Loop & pitch management (unchanged)
    // ------------------------------------------------------------------------

    public synchronized void setIsLooping(boolean looping) {
        for (PASamplerInstrument inst : available) inst.setIsLooping(looping);
        for (PASamplerInstrument inst : inUse) inst.setIsLooping(looping);
    }

    public boolean isAnyInstrumentLooping() {
        synchronized (this) {
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
    // Buffer management
    // ------------------------------------------------------------------------

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
    // Pool behavior & lifecycle
    // ------------------------------------------------------------------------

    public void setVoiceStealingEnabled(boolean enabled) {
        this.voiceStealingEnabled = enabled;
    }

    public boolean isVoiceStealingEnabled() {
        return voiceStealingEnabled;
    }

    public synchronized void close() {
        if (isClosed) return;
        isClosed = true;
        scheduler.shutdownNow();
        for (PASamplerInstrument inst : available) inst.close();
        for (PASamplerInstrument inst : inUse) inst.close();
        available.clear();
        inUse.clear();
        instrumentPan.clear();
    }

    public boolean isClosed() {
        return isClosed;
    }

    // ------------------------------------------------------------------------
    // Diagnostics / Pool State
    // ------------------------------------------------------------------------

    /**
     * Returns the number of instruments currently available for reuse.
     * Useful for debugging pool pressure and voice allocation.
     */
    public synchronized int getAvailableCount() {
    	return available.size();
    }

    /**
     * Returns the number of instruments currently in use (playing or reserved).
     */
    public synchronized int getInUseCount() {
    	return inUse.size();
    }


}
