package net.paulhertz.pixelaudio.voices;

import ddf.minim.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sampler-based instrument that manages a small pool of WFSamplerVoice objects.
 * Each voice plays independently, allowing limited polyphony.
 *
 * Supports pitch scaling, ADSR envelopes, and buffer replacement.
 */
public class PASamplerInstrument implements PASamplerPlayable {
    private final AudioOutput out;
    private final float sampleRate;
    private final int voices;
    private final ADSRParams defaultEnv;

    private MultiChannelBuffer buffer;
    private PASamplerVoice[] voicePool;
    private final AtomicInteger nextVoiceIndex = new AtomicInteger(0);
    private boolean isClosed = false;

    // Global pitch scaling factor (applied to all play calls)
    private volatile float pitchScale = 1.0f;

    /**
     * Constructs a WFSamplerInstrument with multiple voices.
     *
     * @param buffer     The source MultiChannelBuffer.
     * @param sampleRate Sample rate of the buffer.
     * @param voices     Number of simultaneous playback voices.
     * @param out        AudioOutput to patch into.
     * @param env        Default ADSR envelope parameters.
     */
    public PASamplerInstrument(MultiChannelBuffer buffer, float sampleRate, int voices, AudioOutput out, ADSRParams env) {
        this.buffer = buffer;
        this.sampleRate = sampleRate;
        this.voices = voices;
        this.out = out;
        this.defaultEnv = env;
        this.voicePool = new PASamplerVoice[voices];
        for (int i = 0; i < voices; i++) {
            voicePool[i] = new PASamplerVoice(buffer, sampleRate, out, env);
        }
    }

    /**
     * Plays a sample using the default buffer, ADSR, and global pitch scale.
     */
    @Override
    public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
        return playSample(buffer, samplePos, sampleLen, amplitude, pitchScale, env);
    }

    /**
     * Plays a sample from the given buffer using the specified ADSR and global pitch scale.
     */
    @Override
    public int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen, float amplitude, ADSRParams env) {
        return playSample(buffer, samplePos, sampleLen, amplitude, pitchScale, env);
    }

    /**
     * Plays a sample from the default buffer with an explicit pitch factor.
     */
    @Override
    public int playSample(int samplePos, int sampleLen, float amplitude, float pitch, ADSRParams env) {
        return playSample(buffer, samplePos, sampleLen, amplitude, pitch, env);
    }

    /**
     * Plays a sample from the default buffer with an explicit pitch factor.
     */
    public int playSample(int samplePos, int sampleLen, float amplitude, float pitch) {
        return playSample(buffer, samplePos, sampleLen, amplitude, pitch, defaultEnv);
    }

    /**
     * Plays a sample from the default buffer with an explicit pitch factor.
     */
    public int playSample(int samplePos, int sampleLen, float amplitude) {
        return playSample(buffer, samplePos, sampleLen, amplitude, pitchScale, defaultEnv);
    }

    /**
     * Plays a sample from the specified buffer with explicit pitch and ADSR.
     */
    @Override
    public int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen, float amplitude, float pitch, ADSRParams env) {
        if (isClosed) return -1;

        PASamplerVoice voice = getNextVoice();
        if (voice == null) return -1;

        // Combine global pitch scale with per-call pitch factor
        float effectivePitch = pitch * pitchScale;

        voice.play(buffer, samplePos, sampleLen, amplitude, effectivePitch, env);
        return nextVoiceIndex.get();
    }

    /**
     * Returns the next available voice in a round-robin fashion.
     */
    private PASamplerVoice getNextVoice() {
        int idx = nextVoiceIndex.getAndUpdate(i -> (i + 1) % voices);
        return voicePool[idx];
    }

    /**
     * Set the global pitch scaling factor applied to all playback.
     *
     * @param scale The global pitch scale (1.0 = normal speed).
     */
    @Override
    public void setPitchScale(float scale) {
        if (scale <= 0) throw new IllegalArgumentException("Pitch scale must be positive.");
        this.pitchScale = scale;
    }

    /**
     * Get the current global pitch scaling factor.
     */
    @Override
    public float getPitchScale() {
        return pitchScale;
    }

    /**
     * Replace the buffer used by all voices.
     * Creates a new Sampler per voice.
     */
    public synchronized void setBuffer(MultiChannelBuffer buffer) {
        this.buffer = buffer;
        for (PASamplerVoice voice : voicePool) {
            voice.setBuffer(buffer);
        }
    }

    /**
     * Get the buffer currently assigned to this instrument.
     */
    public MultiChannelBuffer getBuffer() {
        return buffer;
    }

    /**
     * Get the buffer currently assigned to this instrument.
     */
    public int getBufferSize() {
        return buffer.getBufferSize();
    }

    /**
     * Cleanly shuts down all voices and releases resources.
     */
    @Override
    public void close() {
        if (!isClosed) {
            for (PASamplerVoice voice : voicePool) {
                voice.close();
            }
            isClosed = true;
        }
    }

    /**
     * Indicates whether this instrument has been closed.
     */
    public boolean isClosed() {
        return isClosed;
    }
}
