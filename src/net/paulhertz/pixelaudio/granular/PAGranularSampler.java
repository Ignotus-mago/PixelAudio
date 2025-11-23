package net.paulhertz.pixelaudio.granular;

import ddf.minim.UGen;
import ddf.minim.AudioOutput;

import net.paulhertz.pixelaudio.voices.ADSRParams;
import net.paulhertz.pixelaudio.voices.PASource;

import java.util.*;

/**
 * PAGranularSampler
 *
 * UGen-based multi-voice granular sampler.
 *
 * Features:
 *  - Voice pooling (PAGranularVoice instances)
 *  - Per-voice ADSR, gain, and pan
 *  - Optional looping of the grain path
 *  - Thread-safe play() method
 *  - Per-sample mixing (like PASharedBufferSampler)
 */
public class PAGranularSampler extends UGen {

    private final AudioOutput out;

    private final List<PAGranularVoice> voices = new ArrayList<>();
    private int maxVoices = 32;
    private int blockSize;

    private boolean smoothSteal = true;

    public PAGranularSampler(AudioOutput out, int maxVoices) {
        this.out = out;
        this.maxVoices = Math.max(1, maxVoices);
        this.blockSize = out.bufferSize();
        this.patch(out);
    }

    // Convenience
    public PAGranularSampler(AudioOutput out) {
        this(out, 16);
    }

    // ------------------------------------------------------------------------
    // Voice allocation
    // ------------------------------------------------------------------------
    private PAGranularVoice getAvailableVoice(PASource src, ADSRParams env,
                                              float gain, float pan, boolean looping) {
        // 1. find free voice
        for (PAGranularVoice v : voices) {
            if (!v.isActive() && !v.isReleasing()) {
                v.activate(src, env, gain, pan, looping);
                return v;
            }
        }

        // 2. expand pool if allowed
        if (voices.size() < maxVoices) {
            PAGranularVoice v = new PAGranularVoice(src, blockSize, out.sampleRate());
            voices.add(v);
            v.activate(src, env, gain, pan, looping);
            return v;
        }

        // 3. recycle oldest active voice
        PAGranularVoice oldest = null;
        for (PAGranularVoice v : voices) {
            if (v.isActive() && (oldest == null || v.getVoiceId() < oldest.getVoiceId())) {
                oldest = v;
            }
        }
        if (oldest != null) {
            if (smoothSteal) oldest.release();
            else oldest.stop();
            oldest.activate(src, env, gain, pan, looping);
            return oldest;
        }

        return null; // should not occur
    }

    // ------------------------------------------------------------------------
    // Play interface
    // ------------------------------------------------------------------------
    /**
     * Play a granular source as a voice.
     *
     * @param src    PASource (PathGranularSource or BasicIndexGranularSource)
     * @param env    ADSR for the macro envelope
     * @param gain   amplitude
     * @param pan    -1..+1
     * @param looping loop granular path
     * @return voiceId or -1
     */
    public synchronized long play(PASource src,
                                  ADSRParams env,
                                  float gain,
                                  float pan,
                                  boolean looping) {
        if (src == null) return -1;

        PAGranularVoice v = getAvailableVoice(src, env, gain, pan, looping);
        if (v == null) return -1;

        return v.getVoiceId();
    }

    // Overload (no looping)
    public synchronized long play(PASource src,
                                  ADSRParams env,
                                  float gain,
                                  float pan) {
        return play(src, env, gain, pan, false);
    }

    // Convenience: default envelope supplied by instrument
    public synchronized long play(PASource src,
                                  float gain,
                                  float pan,
                                  ADSRParams defaultEnv,
                                  boolean looping) {
        return play(src, defaultEnv, gain, pan, looping);
    }

    // ------------------------------------------------------------------------
    // uGenerate — per-sample mixing
    // ------------------------------------------------------------------------
    @Override
    protected synchronized void uGenerate(float[] channels) {
        Arrays.fill(channels, 0f);

        float[] tmp = new float[2]; // left, right

        for (PAGranularVoice v : voices) {

            v.nextSampleStereo(tmp);
            float left  = tmp[0];
            float right = tmp[1];

            if (v.isActive() || v.isReleasing()) {
                // For now, *ignore voice-level pan* and trust the per-grain pan
                channels[0] += left;
                if (channels.length > 1) {
                    channels[1] += right;
                }
            }

            if (v.isFinished()) {
                // recyclable; nothing else needed
            }
        }
    }

    // ------------------------------------------------------------------------
    // Controls
    // ------------------------------------------------------------------------
    public void stopAll() {
        synchronized (this) {
            for (PAGranularVoice v : voices) v.stop();
        }
    }

    public void setMaxVoices(int maxVoices) {
        this.maxVoices = Math.max(1, maxVoices);
    }

    public int getMaxVoices() {
        return maxVoices;
    }

    public void setSmoothSteal(boolean smoothSteal) {
        this.smoothSteal = smoothSteal;
    }

    public boolean isSmoothSteal() {
        return smoothSteal;
    }

    public List<PAGranularVoice> getVoices() {
        return Collections.unmodifiableList(voices);
    }

    public AudioOutput getAudioOutput() {
        return out;
    }
}
