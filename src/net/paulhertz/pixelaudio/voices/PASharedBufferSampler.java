package net.paulhertz.pixelaudio.voices;

import ddf.minim.MultiChannelBuffer;
import ddf.minim.AudioOutput;
import ddf.minim.UGen;

import java.util.*;

/**
 * UGen-based sampler that plays multiple PASamplerVoice instances
 * from a single shared mono buffer (channel 0 of a MultiChannelBuffer).
 *
 * Features:
 *  - Shared buffer (no duplication)
 *  - Polyphony with voice pooling
 *  - Looping (global default + per-voice)
 *  - ADSR per voice via ADSRParams
 *  - Oldest-first voice recycling, optional smooth stealing
 *  - Thread-safe triggering
 *
 * Automatically patches to the provided AudioOutput.
 */
public class PASharedBufferSampler extends UGen implements PASampler
{
    private float[] buffer;     // mono source (channel 0)
    private int bufferLen;
    private float playbackSampleRate;

    private final AudioOutput out;
    private final List<PASamplerVoice> voices = new ArrayList<>();
    private int maxVoices = 32;
    private boolean globalLooping = false;
    private boolean smoothSteal = true;

    /**
     * Construct a sampler over a shared MultiChannelBuffer.
     * Automatically patches to the provided AudioOutput.
     *
     * @param multiBuffer shared source buffer (mono or stereo)
     * @param sampleRate  sample rate of the buffer
     * @param out         target AudioOutput for playback
     */
    public PASharedBufferSampler(MultiChannelBuffer multiBuffer, float sampleRate, AudioOutput out) {
        this.buffer = Arrays.copyOf(multiBuffer.getChannel(0), multiBuffer.getBufferSize());
        this.bufferLen = buffer.length;
        this.playbackSampleRate = sampleRate;
        this.out = out;
        this.patch(out);
    }
    
    public PASharedBufferSampler(MultiChannelBuffer multiBuffer, float playbackSampleRate, AudioOutput out, int maxVoices) {
    	this.buffer = Arrays.copyOf(multiBuffer.getChannel(0), multiBuffer.getBufferSize());
       	this.bufferLen = buffer.length;
    	this.playbackSampleRate = playbackSampleRate;
    	this.out = out;
    	this.maxVoices = Math.max(1, maxVoices);
       	for (int i = 0; i < this.maxVoices; i++) {
    		voices.add(new PASamplerVoice(buffer, playbackSampleRate));
    	}
    	this.patch(out);   	
    }
    

    // NEW: convenience – infer sampleRate from AudioOutput
    public PASharedBufferSampler(MultiChannelBuffer multiBuffer, AudioOutput out) {
        this(multiBuffer, (out != null ? out.sampleRate() : 44100), out);
    }

    @Override
    public synchronized int play(int samplePos, int sampleLen, float amplitude,
                                 ADSRParams env, float pitch, float pan)
    {
        if (sampleLen <= 0 || samplePos >= bufferLen) return 0;
        if (samplePos < 0) samplePos = 0;
        if (samplePos + sampleLen > bufferLen) sampleLen = bufferLen - samplePos;

        PASamplerVoice v = getAvailableVoice();
        if (v == null) return 0;

        v.activate(samplePos, sampleLen, amplitude, env, pitch, pan, globalLooping);
        return 1;
    }

    /**
     * Get a free voice, or recycle the oldest active one if at the polyphony limit.
     */
    private PASamplerVoice getAvailableVoice()
    {
        // 1) Free voice first
        for (PASamplerVoice v : voices)
        {
            if (!v.isActive()) return v;
        }

        // 2) Allocate if under limit
        if (voices.size() < maxVoices)
        {
            PASamplerVoice v = new PASamplerVoice(buffer, playbackSampleRate);
            voices.add(v);
            return v;
        }

        // 3) Recycle oldest
        PASamplerVoice oldest = null;
        for (PASamplerVoice v : voices)
        {
            if (v.isActive() && (oldest == null || v.getVoiceId() < oldest.getVoiceId()))
            {
                oldest = v;
            }
        }
        if (oldest != null)
        {
            if (smoothSteal) oldest.release();
            else oldest.stop();
            return oldest;
        }

        return null; // shouldn't happen
    }

    @Override
    protected void uGenerate(float[] channels)
    {
        float left = 0f;
        float right = 0f;

        synchronized (this)
        {
            for (PASamplerVoice v : voices)
            {
                if (!v.isActive()) continue;
                float s = v.nextSample();
                if (Float.isNaN(s)) continue;

                // Simple linear pan (can upgrade to equal-power if desired)
                float pan = v.getPan();
                float leftGain  = (pan <= 0f) ? 1f : 1f - pan;
                float rightGain = (pan >= 0f) ? 1f : 1f + pan;

                left  += s * leftGain;
                right += s * rightGain;
            }
        }

        channels[0] = left;
        channels[1] = right;
    }

    // PASampler methods

    @Override
    public void stopAll()
    {
        synchronized (this)
        {
            for (PASamplerVoice v : voices) v.stop();
        }
    }

    @Override
    public boolean isLooping()
    {
        for (PASamplerVoice v : voices)
        {
            if (v.isActive() && v.isLooping()) return true;
        }
        return false;
    }

    // Controls / inspection

    /** Default looping for newly triggered voices. */
    public void setGlobalLooping(boolean looping)
    {
        this.globalLooping = looping;
    }

    public boolean isGlobalLooping() { return globalLooping; }

    /** Enable/disable smooth stealing (release envelope) on voice recycle. */
    public void setSmoothSteal(boolean smoothSteal)
    {
        this.smoothSteal = smoothSteal;
    }

    public boolean isSmoothSteal() { return smoothSteal; }

    /** Change maximum polyphony at runtime. */
    public synchronized void setMaxVoices(int maxVoices)
    {
        this.maxVoices = Math.max(1, maxVoices);
    }

    public int getMaxVoices() { return maxVoices; }

    /** Read-only list of voices for GUI or debugging. */
    public List<PASamplerVoice> getVoices()
    {
        return Collections.unmodifiableList(voices);
    }

    // ------------------------------------------------------------------------
    // Sample rate management
    // ------------------------------------------------------------------------

    /** Returns the current sample rate of this sampler. */
    public synchronized float getPlaybackSampleRate() {
    	return playbackSampleRate;
    }

    /**
     * Updates the playback sample rate used for reading from the buffer.
     * Does not affect Minim's UGen sample rate.
     */
    public synchronized void setPlaybackSampleRate(float newRate) {
        if (newRate > 0f && newRate != playbackSampleRate) {
            this.playbackSampleRate = newRate;
            for (PASamplerVoice v : voices) {
                v.setPlaybackSampleRate(newRate);
            }
        }
    }

    /**
     * Convenience: synchronize playback rate with AudioOutput's sample rate.
     * Useful if you want playback speed tied to system rate.
     */
    public synchronized void updatePlaybackRateFromOutput() {
        if (out != null) {
            setPlaybackSampleRate(out.sampleRate());
        }
    }
    
    public int getBufferLength() { return bufferLen; }

    public AudioOutput getAudioOutput() { return out; }
}
