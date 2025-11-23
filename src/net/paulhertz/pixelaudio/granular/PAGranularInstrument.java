package net.paulhertz.pixelaudio.granular;

import ddf.minim.AudioOutput;

import net.paulhertz.pixelaudio.voices.ADSRParams;
import net.paulhertz.pixelaudio.voices.PASource;

/**
 * PAGranularInstrument
 *
 * High-level granular instrument wrapper.
 *
 * Mirrors PASamplerInstrument but uses:
 *   PAGranularSampler → PAGranularVoice → PASource.
 *
 * Provides:
 *   - Global pan
 *   - Global gain scaling
 *   - Default ADSR envelope
 *   - Looping option for granular paths
 *   - Thread-safe play() methods
 *
 * Does NOT manage MultiChannelBuffer. PASource encapsulates its data.
 */
public class PAGranularInstrument {

    // ------------------------------------------------------------------------
    // Core components
    // ------------------------------------------------------------------------
    private final PAGranularSampler sampler;
    private final AudioOutput out;

    private ADSRParams defaultEnv;

    // Global modifiers
    private float globalPan  = 0f;   // -1..+1
    private float globalGain = 1f;   // overall amplitude multiplier

    private boolean isClosed = false;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    public PAGranularInstrument(AudioOutput out,
                                ADSRParams defaultEnv,
                                int maxVoices) {
        this.out = out;
        this.sampler = new PAGranularSampler(out, maxVoices);
        this.defaultEnv = (defaultEnv != null)
                ? defaultEnv
                : new ADSRParams(1f, 0.01f, 0.2f, 0.8f, 0.3f);
    }

    public PAGranularInstrument(AudioOutput out) {
        this(out, new ADSRParams(1f, 0.01f, 0.2f, 0.8f, 0.3f), 16);
    }

    // ------------------------------------------------------------------------
    // Playback API
    // ------------------------------------------------------------------------

    /**
     * Main granular play() method.
     *
     * @param src     PASource (BasicIndexGranularSource, PathGranularSource, ...)
     * @param amp     amplitude
     * @param pan     stereo pan (-1..+1)
     * @param env     envelope (or null → default)
     * @param looping loop the grain path
     * @return voiceId or -1
     */
    public synchronized long play(PASource src,
                                  float amp,
                                  float pan,
                                  ADSRParams env,
                                  boolean looping)
    {
        if (src == null || sampler == null || isClosed) return -1;

        float finalGain = amp * globalGain;
        float finalPan = clampPan(globalPan + pan);

        ADSRParams useEnv = (env != null) ? env : defaultEnv;

        return sampler.play(src, useEnv, finalGain, finalPan, looping);
    }

    /** Convenience: uses default envelope, no looping. */
    public synchronized long play(PASource src, float amp, float pan) {
        return play(src, amp, pan, defaultEnv, false);
    }

    /** Convenience: default pan, default envelope. */
    public synchronized long play(PASource src, float amp) {
        return play(src, amp, 0f, defaultEnv, false);
    }

    /** Convenience: default env and global pan. */
    public synchronized long play(PASource src) {
        return play(src, 1f, globalPan, defaultEnv, false);
    }

    /** Convenience: looping version. */
    public synchronized long playLooping(PASource src, float amp, float pan) {
        return play(src, amp, pan, defaultEnv, true);
    }

    // ------------------------------------------------------------------------
    // Controls
    // ------------------------------------------------------------------------

    public void stopAll() {
        if (sampler != null) sampler.stopAll();
    }

    public void setDefaultEnvelope(ADSRParams env) {
        if (env != null) this.defaultEnv = env;
    }

    public ADSRParams getDefaultEnvelope() { return defaultEnv; }

    public void setGlobalPan(float pan) { this.globalPan = clampPan(pan); }

    public float getGlobalPan() { return globalPan; }

    public void setGlobalGain(float g) { this.globalGain = g; }

    public float getGlobalGain() { return globalGain; }

    public PAGranularSampler getSampler() { return sampler; }

    // ------------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------------

    public synchronized void close() {
        if (isClosed) return;
        isClosed = true;
        stopAll();
        // sampler is a UGen patched to out — optionally leave patch on
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    private static float clampPan(float p) {
        if (p < -1f) return -1f;
        if (p > 1f) return 1f;
        return p;
    }
    
}
