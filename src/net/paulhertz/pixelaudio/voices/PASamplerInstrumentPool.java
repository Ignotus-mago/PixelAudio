package net.paulhertz.pixelaudio.voices;

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;

import java.util.*;
import java.util.concurrent.*;

/**
 * Pool of WFSamplerInstrument objects for polyphonic playback.
 *
 * Features:
 *  - Fixed-size pool with optional voice stealing
 *  - Automatic return of instruments to pool after envelope finishes
 *  - Optional per-note pitch scaling
 *  - Backward-compatible with earlier WFSamplerInstrumentPool versions
 */
public class PASamplerInstrumentPool implements PASamplerPlayable {
    private final int poolSize;
    private final float sampleRate;
    private final ADSRParams defaultADSR;
    private final AudioOutput out;
    private final int perInstrumentVoices;

    private MultiChannelBuffer buffer;

    private final ArrayDeque<PASamplerInstrument> available = new ArrayDeque<>();
    private final Set<PASamplerInstrument> inUse = new HashSet<>();

    // Scheduler handles delayed noteOff/unpatch cleanup
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "WFSamplerInstrumentPool-scheduler");
                t.setDaemon(true);
                return t;
            });

    private volatile boolean voiceStealingEnabled = true;
    private volatile float pitchScale = 1.0f;

    /**
     * Create a pool of WFSamplerInstrument objects.
     *
     * @param buffer              the sample buffer used by all instruments
     * @param sampleRate          sample rate
     * @param poolSize            number of instruments in pool
     * @param perInstrumentVoices number of voices per instrument (typically 1)
     * @param out                 AudioOutput
     * @param defaultADSR         default ADSR envelope
     */
    public PASamplerInstrumentPool(MultiChannelBuffer buffer,
                                   float sampleRate,
                                   int poolSize,
                                   int perInstrumentVoices,
                                   AudioOutput out,
                                   ADSRParams defaultADSR) {
        this.buffer = buffer;
        this.sampleRate = sampleRate;
        this.poolSize = poolSize;
        this.perInstrumentVoices = perInstrumentVoices;
        this.out = out;
        this.defaultADSR = defaultADSR;
        initPool(buffer);
    }

    /**
     * Initializes or rebuilds the pool with a specific buffer.
     */
    private synchronized void initPool(MultiChannelBuffer buffer) {
        available.clear();
        inUse.clear();
        for (int i = 0; i < poolSize; i++) {
            available.add(new PASamplerInstrument(buffer, sampleRate, perInstrumentVoices, out, defaultADSR));
        }
    }

    /**
     * Replace the backing buffer for all instruments in the pool.
     */
    public synchronized void setBuffer(MultiChannelBuffer newBuffer) {
        this.buffer = newBuffer;
        initPool(newBuffer);
    }

    public MultiChannelBuffer getBuffer() {
        return buffer;
    }

    public void setVoiceStealingEnabled(boolean enabled) {
        this.voiceStealingEnabled = enabled;
    }

    public boolean isVoiceStealingEnabled() {
        return voiceStealingEnabled;
    }

    // =====================================================================
    // PLAY METHODS — All compatible with WFSamplerPlayable
    // =====================================================================

    /** Legacy, default-pitch version */
    @Override
    public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
        return playSample(samplePos, sampleLen, amplitude, pitchScale, env);
    }

    /** Legacy, default ADSR version */
    public int playSample(int samplePos, int sampleLen, float amplitude) {
        return playSample(samplePos, sampleLen, amplitude, pitchScale, defaultADSR);
    }

    /** Play from buffer explicitly (uses current pitchScale) */
    @Override
    public int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen, float amplitude, ADSRParams env) {
        return playSample(buffer, samplePos, sampleLen, amplitude, pitchScale, env);
    }

    /** Play from buffer with explicit pitch factor */
    @Override
    public int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
                          float amplitude, float pitch, ADSRParams env) {
        PASamplerInstrument inst = acquireInstrument();
        if (inst == null) return 0;

        int actualLen = inst.playSample(buffer, samplePos, sampleLen, amplitude, pitch, env);
        scheduleReturn(inst, actualLen, env);
        return actualLen;
    }

    /** Play from default buffer with explicit pitch */
    @Override
    public int playSample(int samplePos, int sampleLen, float amplitude, float pitch, ADSRParams env) {
        PASamplerInstrument inst = acquireInstrument();
        if (inst == null) return 0;

        int actualLen = inst.playSample(buffer, samplePos, sampleLen, amplitude, pitch, env);
        scheduleReturn(inst, actualLen, env);
        return actualLen;
    }

    // =====================================================================
    // INTERNAL HELPERS
    // =====================================================================

    private synchronized PASamplerInstrument acquireInstrument() {
        PASamplerInstrument inst = available.poll();
        if (inst == null) {
            if (voiceStealingEnabled && !inUse.isEmpty()) {
                inst = inUse.iterator().next();
                inUse.remove(inst);
            } else {
                return null;
            }
        }
        inUse.add(inst);
        return inst;
    }

    /**
     * Schedule return of instrument after note finishes.
     */
    private void scheduleReturn(PASamplerInstrument inst, int actualLen, ADSRParams env) {
        float envDurationMs = (env.getAttack() + env.getDecay() + env.getRelease()) * 1000f;
        long durationMillis = Math.round((actualLen / (double) sampleRate) * 1000.0 + envDurationMs);

        scheduler.schedule(() -> {
            synchronized (PASamplerInstrumentPool.this) {
                inUse.remove(inst);
                available.add(inst);
            }
        }, durationMillis, TimeUnit.MILLISECONDS);
    }

    // =====================================================================
    // PITCH SCALE & SHUTDOWN
    // =====================================================================

    @Override
    public void setPitchScale(float scale) {
        if (scale <= 0) throw new IllegalArgumentException("Pitch scale must be positive.");
        this.pitchScale = scale;
    }

    @Override
    public float getPitchScale() {
        return pitchScale;
    }

    @Override
    public void close() {
        scheduler.shutdown();
        synchronized (this) {
            for (PASamplerInstrument inst : available) {
                inst.close();
            }
            for (PASamplerInstrument inst : inUse) {
                inst.close();
            }
            available.clear();
            inUse.clear();
        }
    }
}
