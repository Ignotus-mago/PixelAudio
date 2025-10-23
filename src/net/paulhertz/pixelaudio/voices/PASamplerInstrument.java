package net.paulhertz.pixelaudio.voices;

import ddf.minim.*;
import ddf.minim.ugens.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Sampler-based instrument that manages a small pool of PASamplerVoice objects.
 * Each voice plays independently, allowing limited polyphony. 
 *
 * Supports pitch scaling, ADSR envelopes, and buffer replacement.
 */
public class PASamplerInstrument implements PASamplerPlayable {
    private final AudioOutput out;                                        // Minim AudioOutput, must be STEREO 
    private final float sampleRate;                                       // sample rate for output
    private final int maxVoices;                                          // number of individual voices in voicePool
    
    private final List<PASamplerVoice> voices = new ArrayList<>();        // voices for this instrument
    private final ADSRParams defaultEnv;                                  // envelope to use with playSample(...) when one is not supplied
    private volatile float pitchScale = 1.0f;                             // Global pitch scaling factor (applied to all play calls)
    private float globalPan = 0.0f;                                       // -1.0 = left, +1.0 = right, 0.0 = center
    
    private MultiChannelBuffer buffer;                                    // audio buffer
    private int bufferSize;                                               // size of the buffer
    private int nextVoice = 0;                                            // index to next voice
    private boolean isClosed = false;                                     // flag set to true on shutdown, closing all active threads 
    private final ScheduledExecutorService scheduler;                     // TODO awaiting future use for timing or cleanup tasks 


    /**
     * Constructs a PASamplerInstrument with multiple voices, default pan (0.0f).
     *
     * @param buffer     The source MultiChannelBuffer
     * @param sampleRate Sample rate of the buffer
     * @param maxVoices  Number of simultaneous playback voices
     * @param out        AudioOutput to patch into, must be STEREO
     * @param env        Default ADSR envelope parameters
     */
    public PASamplerInstrument(MultiChannelBuffer buffer, float sampleRate, int maxVoices, AudioOutput audioOut, ADSRParams env) {
    	this.sampleRate = sampleRate;
    	this.maxVoices = Math.max(1, maxVoices);
    	this.out = audioOut;
    	this.defaultEnv = env;
    	this.buffer = buffer;
    	this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
    		Thread t = new Thread(r, "PASamplerInstrument-scheduler");
    		t.setDaemon(true);
    		return t;
    	});
    	this.bufferSize = buffer.getBufferSize();
    	for (int i = 0; i < maxVoices; i++) {
    	    // Each voice gets its own Sampler, but shares the same MultiChannelBuffer reference
    	    Sampler sampler = new Sampler(buffer, out.sampleRate(), 1);
    	    PASamplerVoice voice = new PASamplerVoice(sampler, bufferSize, sampleRate, out, defaultEnv);
    	    voices.add(voice);
    	}
    }


    // ------------------------------------------------------------------------
    // Core playback
    // ------------------------------------------------------------------------

    /**
     * Trigger playback using per-voice parameters.
     *
     * @param samplePos  start position (samples)
     * @param sampleLen  playback length (samples)
     * @param amplitude  per-voice amplitude
     * @param env        ADSR envelope parameters
     * @param pitch      playback rate (1.0 = normal)
     * @param pan        stereo position (-1.0 = left, +1.0 = right)
     */
    public synchronized int playSample(int samplePos, int sampleLen, float amplitude,
    		ADSRParams env, float pitch, float pan) {
    	if (isClosed) return 0;

    	PASamplerVoice voice = getNextAvailableVoice();
    	if (voice == null) return 0;

    	// Apply global pitch and pan modifiers
    	float actualPitch = pitch * pitchScale;
    	float actualPan = pan + globalPan;
    	actualPan = Math.max(-1.0f, Math.min(1.0f, actualPan)); // clamp

    	int actualLen = voice.play(samplePos, sampleLen, amplitude, env, actualPitch, actualPan);
    	return actualLen;
    }

    
    /**
     * Convenience overload: uses default envelope, default pitch, and center pan.
     */
    public synchronized int playSample(int samplePos, int sampleLen, float amplitude) {
    	return playSample(samplePos, sampleLen, amplitude, defaultEnv, pitchScale, 0.0f);
    }

    /**
     * Convenience overload: uses default envelope, supplied pitch and center pan.
     */
    public synchronized int playSample(int samplePos, int sampleLen, float amplitude, float pitch) {
    	return playSample(samplePos, sampleLen, amplitude, defaultEnv, pitch, 0.0f);
    }

    /**
     * Plays a sample using a supplied envelope with default pitch and default pan.
     */
    @Override
    public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
    	return playSample(samplePos, sampleLen, amplitude, env, pitchScale, globalPan);
    }

	@Override
	public int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen, 
			              float amplitude, ADSRParams env, float pitch) {
		this.setBuffer(buffer);
		return playSample(samplePos, sampleLen, amplitude, env, pitch, globalPan);
	}

	@Override
	public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch) {
		return playSample(samplePos, sampleLen, amplitude, env, pitch, globalPan);
	}
	
	@Override
	public int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
	                      float amplitude, ADSRParams env, float pitch, float pan) {
	    this.setBuffer(buffer);
	    return playSample(samplePos, sampleLen, amplitude, env, pitch, pan);
	}
	

    // ------------------------------------------------------------------------
    // Voice management
    // ------------------------------------------------------------------------

    private PASamplerVoice getNextAvailableVoice() {
        // Round-robin allocation with voice stealing
        for (int i = 0; i < maxVoices; i++) {
            int index = (nextVoice + i) % maxVoices;
            PASamplerVoice v = voices.get(index);
            if (!v.isBusy() && !v.isClosed()) {
                nextVoice = (index + 1) % maxVoices;
                return v;
            }
        }
        // All voices busy — steal next in line
        nextVoice = (nextVoice + 1) % maxVoices;
        return voices.get(nextVoice);
    }

    public boolean isAnyVoiceBusy() {
        for (PASamplerVoice v : voices)
            if (v.isBusy()) return true;
        return false;
    }
    
    public List<PASamplerVoice> getVoices() {
    	return voices;
    }

    
    // ------------------------------------------------------------------------
    // Loop & pitch management
    // ------------------------------------------------------------------------

    public void setIsLooping(boolean looping) {
        for (PASamplerVoice v : voices) v.setIsLooping(looping);
    }

    public void stopAllLoops() {
        for (PASamplerVoice v : voices) {
            if (v.isLooping()) v.stopLoop();
        }
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
    
    /** Set default stereo pan for instrument (-1.0 left to +1.0 right). */
    public void setPan(float pan) {
        if (pan < -1f) pan = -1f;
        if (pan > +1f) pan = +1f;
        this.globalPan = pan;
    }

    /** Get current default pan. */
    public float getPan() {
        return globalPan;
    }    

    
    // ------------------------------------------------------------------------
    // Lazy buffer reload
    // ------------------------------------------------------------------------

    /**
     * Replaces the buffer in the shared sampler and rebuilds voices.
     */
    public synchronized void setBuffer(MultiChannelBuffer buffer) {
        if (isClosed) return;
        this.buffer = buffer;
        int bufferSize = buffer.getBufferSize();
        voices.clear();
    	for (int i = 0; i < maxVoices; i++) {
    	    // Each voice gets its own Sampler, but shares the same MultiChannelBuffer reference
    	    Sampler sampler = new Sampler(buffer, out.sampleRate(), 1);
    	    PASamplerVoice voice = new PASamplerVoice(sampler, bufferSize, sampleRate, out, defaultEnv);
    	    voices.add(voice);
    	}
    }

    /**
     * Get the buffer currently assigned to this instrument.
     */
    public MultiChannelBuffer getBuffer() {
        return buffer;
    }

    
    // ------------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------------

    public synchronized void close() {
        if (isClosed) return;
        isClosed = true;
        scheduler.shutdownNow();
        for (PASamplerVoice v : voices) v.close();
        voices.clear();
    }


    /**
     * Get the buffer currently assigned to this instrument.
     */
    public int getBufferSize() {
        return buffer.getBufferSize();
    }

    /**
     * Indicates whether this instrument has been closed.
     */
    public boolean isClosed() {
        return isClosed;
    }

}
