package net.paulhertz.pixelaudio.voices;

import ddf.minim.MultiChannelBuffer;
import ddf.minim.AudioOutput;
import ddf.minim.UGen;

import java.util.*;

/**
 * UGen-based sampler that plays multiple PASamplerVoice instances
 * from a single shared mono buffer (channel 0 of a MultiChannelBuffer)
 * which is an array of floating point samples over (-1.0..1.0).
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
 * 
 */
public class PASharedBufferSampler extends UGen implements PASampler {
    private float[] buffer;     // mono source (channel 0)
    private int bufferLen;
    private float playbackSampleRate;

    private final AudioOutput out;
    private final List<PASamplerVoice> voices = new ArrayList<>();
    private int maxVoices = 32;
    private boolean globalLooping = false;
    private boolean smoothSteal = true;
    
    protected boolean DEBUG = false;

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

    /**
     * Play command with all the useful arguments in standard order, overrides PASampler.play().
     * TODO We plan to make this the only play() method PASharedBufferSampler. 
     */
     @Override
    public synchronized int play(int samplePos, int sampleLen, float amplitude,
                                 ADSRParams env, float pitch, float pan) {
        if (sampleLen <= 0 || samplePos >= bufferLen) return 0;
        if (samplePos < 0) samplePos = 0;
        if (samplePos + sampleLen > bufferLen) sampleLen = bufferLen - samplePos;
        PASamplerVoice v = getAvailableVoice();
        if (v == null) return 0;
        v.activate(samplePos, sampleLen, amplitude, env, pitch, pan, globalLooping);
        int eventSamples = PlaybackInfo.computeVoiceDuration(
                samplePos,
                sampleLen,
                bufferLen,
                pitch,
                env,
                globalLooping,
                playbackSampleRate
            );
        float bufferReadSamples = sampleLen * Math.abs(pitch);
        float durationMS = eventSamples / playbackSampleRate * 1000f;
        long startSample = 0; // later replace with an actual sample clock
        // information to be shared later
        PlaybackInfo info = new PlaybackInfo(
            v.getVoiceId(),
            eventSamples,
            bufferReadSamples,
            durationMS,
            globalLooping,
            startSample,
            playbackSampleRate
        );
        // debugging
        if (DEBUG) 
            System.out.printf("[Voice %d] eventDuration=%d samples (%.2f ms)%n",
            v.getVoiceId(), eventSamples,
            eventSamples / playbackSampleRate * 1000f);        
        return eventSamples;
    }

    /**
     * Get a free voice, or recycle the oldest active one if at the polyphony limit.
     */
    private PASamplerVoice getAvailableVoice() {
        // 1) Free voice first
        for (PASamplerVoice v : voices) {
            if (!v.isActive() && !v.isReleasing()) {
                return v;
            }
        }
        // 2) Allocate if under limit
        if (voices.size() < maxVoices) {
            PASamplerVoice v = new PASamplerVoice(buffer, playbackSampleRate);
            voices.add(v);
            return v;
        }
        // 3) Recycle oldest
        PASamplerVoice oldest = null;
        for (PASamplerVoice v : voices) {
            if (v.isActive() && (oldest == null || v.getVoiceId() < oldest.getVoiceId())) {
                oldest = v;
            }
        }
        if (oldest != null) {
            if (smoothSteal) oldest.release();
            else oldest.stop();
            return oldest;
        }
        return null; // shouldn't happen
    }

    @Override
    protected synchronized void uGenerate(float[] channels) {
    	// clear mix buffer for this frame
    	Arrays.fill(channels, 0f);
    	Iterator<PASamplerVoice> it = voices.iterator();
    	while (it.hasNext()) {
    		PASamplerVoice v = it.next();
    		// advance voice
    		float sample = 0f;
    		try {
    			sample = v.nextSample();
    		} 
    		catch (ArrayIndexOutOfBoundsException e) {
    			// hard stop if buffer index ever slips past the end
    			v.stop();
    			continue;
    		}
            // Mix only active or releasing voices
            if (v.isActive() || v.isReleasing()) {
                float pan = v.getPan();
                float leftGain = (pan <= 0) ? 1f : 1f - pan;
                float rightGain = (pan >= 0) ? 1f : 1f + pan;
                channels[0] += sample * leftGain;
                if (channels.length > 1) channels[1] += sample * rightGain;
            }
            // Recycle finished voices
            if (v.isFinished()) {
                v.resetPosition();  // resets active=false, released=false
            }
         }
    }

    // PASampler methods

    @Override
    public void stopAll() {
        synchronized (this) {
            for (PASamplerVoice v : voices) v.stop();
        }
    }

    @Override
    public boolean isLooping() {
        for (PASamplerVoice v : voices) {
            if (v.isActive() && v.isLooping()) return true;
        }
        return false;
    }

    // Controls / inspection

    /** Default looping for newly triggered voices. */
    public void setGlobalLooping(boolean looping) {
        this.globalLooping = looping;
    }

    public boolean isGlobalLooping() { return globalLooping; }

    /** Enable/disable smooth stealing (release envelope) on voice recycle. */
    public void setSmoothSteal(boolean smoothSteal) {
        this.smoothSteal = smoothSteal;
    }

    public boolean isSmoothSteal() { return smoothSteal; }

    /** Change maximum polyphony at runtime. */
    public synchronized void setMaxVoices(int maxVoices) {
        this.maxVoices = Math.max(1, maxVoices);
    }

    public int getMaxVoices() { return maxVoices; }

    /** Read-only list of voices for GUI or debugging. */
    public List<PASamplerVoice> getVoices() {
        return Collections.unmodifiableList(voices);
    }
    
    // ----- Accessors ----- //
    
    public synchronized void setBuffer(float[] buffer) {
    	this.buffer = buffer;
    	for (PASamplerVoice v : this.voices) {
    		v.stop();
    		v.setBuffer(buffer);
    		v.resetPosition();
    	}
    }
 
    public synchronized void setBuffer(float[] buffer, float playbackSampleRate) {
    	this.buffer = buffer;
    	this.playbackSampleRate = playbackSampleRate;
    	for (PASamplerVoice v : this.voices) {
    		v.stop();
    		v.setBuffer(buffer);
    		v.resetPosition();
    	}
    }
        
    public int countAvailableVoices() {
        int n = 0;
        for (var v : voices) if (!v.isActive() && !v.isReleasing()) n++;
        return n;
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
