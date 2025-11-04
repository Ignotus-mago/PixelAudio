package net.paulhertz.pixelaudio.voices;

import ddf.minim.ugens.ADSR;
import ddf.minim.ugens.Constant;
import net.paulhertz.pixelaudio.voices.SimpleADSR;


/**
 * Represents a single playback "voice" reading from a shared mono buffer.
 * Each voice has its own pitch, gain, pan, looping, and envelope.
 * 
 * Hybrid envelope system:
 *  - Uses Minim's ADSR if patched in a live UGen graph.
 *  - Otherwise, falls back to a manual "SimpleADSR" tick inside nextSample().
 */
public class PASamplerVoice {
	private boolean isFindZeroCrossing = false;    // if true, shift start to a zero crossing (does not change the buffer, change initial sample position)
	private boolean isMicroFadeIn = false;         // if true, impose a mini-envolope on buffer values at start (changes the buffer)
	// buffer
    private float[] buffer;
    private float playbackSampleRate;
    // voices
	private static long NEXT_VOICE_ID = 0;
    private long voiceId;
    private boolean active;
    private boolean looping;
    // sample indexing
    private int start;
    private int end;
    private float position;
    private float rate;
    private float gain;
    private float pan;
    // --- Envelope handling ---
    private ADSR envelope;                         // Minim envelope (UGen), commented out
    private Constant envCarrier;                   // Input UGen for ADSR (only used if patched)
    
    private SimpleADSR fallbackEnv;                // Fallback software envelope, the one we will use
    private final float[] envFrame = new float[1]; // frame for tick function
    private boolean released = false;              // is the voice released
    private boolean finished = false;
        
    private static final boolean DEBUG = false;
    int frameCounter = 0;

    
    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------
    public PASamplerVoice(float[] buffer, float sampleRate) {
        this.buffer = buffer;
        this.playbackSampleRate = sampleRate;
    }

    /**
     * Activates this voice for playback.
     */
    public void activate(int start, int length, float gain,
    		ADSRParams envParams, float pitch, float pan, boolean looping) {
    	// --- full state reset ---
    	this.active = false;       // prevent mid-update reuse
    	this.released = false;
    	this.finished = false;
    	this.looping = false;
    	this.envelope = null;      // (if you ever reinstate Minim)
    	this.envCarrier = null;
    	this.fallbackEnv = null;   // guarantee fresh envelope each trigger
    	// --- voice settings ---
    	this.voiceId = NEXT_VOICE_ID++;
    	this.start = Math.max(0, start);
    	this.end = Math.min(buffer.length, start + Math.max(0, length));
    	this.active = (this.start < this.end);
    	this.position = this.start;
    	this.rate = pitch;
    	this.gain = gain;
    	this.pan = Math.max(-1f, Math.min(1f, pan));
    	this.looping = looping;
    	this.released = false;

    	// --- 1. optional zero-crossing adjustment ---
    	if (isFindZeroCrossing) {
    		this.start = findZeroCrossing(this.start, 1);
    		this.position = this.start;
    	}

    	// --- 2. Envelope setup (fallback only) ---
    	if (envParams != null) {
    		this.fallbackEnv = new SimpleADSR(
				envParams.getAttack(),
				envParams.getDecay(),
				envParams.getSustain(),
				envParams.getRelease());
    		this.fallbackEnv.setSampleRate(playbackSampleRate);
    		this.fallbackEnv.noteOn();

    		// Optional: "warm up" a few ticks
    		for (int i = 0; i < 5; i++) this.fallbackEnv.tick();
    	} 
    	else {
    		this.fallbackEnv = null;
    	}

    	// --- 4. Apply micro-fade-in to first few samples (1–2 ms) ---

    	if (isMicroFadeIn) {
    		int fadeSamples = Math.min(64, end - start);
    		float fadeAmp = 0f;
    		float fadeStep = 1f / fadeSamples;
    		for (int i = 0; i < fadeSamples; i++) {
    			buffer[start + i] *= fadeAmp;
    			fadeAmp += fadeStep;
    		}
    	}

    	if (DEBUG && frameCounter++ % 2000 == 0) {
    		float debugEnv = (fallbackEnv != null) ? fallbackEnv.getValue() : envFrame[0];
    		System.out.printf("[Voice %d] idx=%d pos=%.2f env=%.4f gain=%.3f active=%b looping=%b released=%b%n",
    				voiceId, getCurrentIndex(), position, debugEnv, gain, active, looping, released);
    	}
    }

    
    public int getCurrentIndex() {
        return (int) position;
    }


    /**
     * Returns next sample (with envelope applied).
     */
    public float nextSample() {
        if (!active) return 0.0f;
        int idx = (int) position;
        
        // Debug output
        if (DEBUG && frameCounter++ % 2000 == 0) {
            System.out.printf("[Voice %d] idx=%d pos=%.2f env=%.4f gain=%.3f active=%b looping=%b released=%b%n",
                              voiceId, idx, position, envFrame[0], gain, active, looping, released);
        }
        
        // End of buffer handling
        if (idx >= end) {
            if (looping) {
                position = start;
                idx = start;
            }
            else {
                if (!released) {
                    released = true;
                    if (envelope != null) envelope.noteOff();
                    if (fallbackEnv != null) fallbackEnv.noteOff();
                }
                idx = end - 1;
            }
        }

        // Envelope value
        float envValue = 1.0f;
        if (envelope != null) {
            envelope.tick(envFrame);
            envValue = envFrame[0];
            if (envValue == 0 && fallbackEnv != null) {
                // fallback if Minim ADSR produces silence
                envValue = fallbackEnv.tick();
            }
        } 
        else if (fallbackEnv != null) {
            envValue = fallbackEnv.tick();
        }

        float sample = buffer[idx] * gain * envValue;    // we have a sample
        position += rate;

        // Envelope-driven deactivation (replaces silenceCounter logic)
        if (released) {
            boolean envDone = false;
            if (fallbackEnv != null) {
                // If fallback envelope exists, check if it's finished
                if (fallbackEnv.isFinished()) envDone = true;
            }
            else if (envelope != null) {
                // Minim ADSR: consider finished when envelope output nearly zero
                if (envFrame[0] <= 1e-6f) envDone = true;
            }
            if (envDone) {
                active = false;
                released = false;
                finished = true;
            }
        }
        return sample;
    }

    /** Begin release phase for smooth voice stealing. */
    public void release() {
        if (!released) {
            released = true;
            if (envelope != null) envelope.noteOff();
            if (fallbackEnv != null) fallbackEnv.noteOff();
        }
        looping = false;
    }

    /** Hard stop (no envelope). */
    public void stop() {
        active = false;
        looping = false;
        released = true;
        if (envCarrier != null && envelope != null) envCarrier.unpatch(envelope);
    }

    // --- Accessors ---
    
    public synchronized void setBuffer(float[] buffer) {
    	this.buffer = buffer;
    }
    
    public synchronized void setBuffer(float[] buffer, float playbackSampleRate) {
    	this.buffer = buffer;
    	this.playbackSampleRate = playbackSampleRate;
    }
    
    public boolean isActive() { return active; }
    public boolean isReleasing() { return released && active; }
    public boolean isFinished() {
        // A voice is finished when inactive and envelope fully completed.
        return !active && (fallbackEnv == null || fallbackEnv.isFinished());
    }
    
    public boolean isLooping() { return looping; }
    public void setLooping(boolean looping) { this.looping = looping; }

    public float getPosition() { return position; }
    public float getGain() { return gain; }
    public float getPan() { return pan; }
    public long getVoiceId() { return voiceId; }
    public int getStart() { return start; }
    public int getEnd() { return end; }

    public synchronized float getPlaybackSampleRate() { return playbackSampleRate; }
    public synchronized void setPlaybackSampleRate(float newRate) {
        if (newRate > 0f) this.playbackSampleRate = newRate;
    }

    
    
    public boolean isFindZeroCrossing() {
		return isFindZeroCrossing;
	}
	public void setFindZeroCrossing(boolean isFindZeroCrossing) {
		this.isFindZeroCrossing = isFindZeroCrossing;
	}

	public boolean isMicroFadeIn() {
		return isMicroFadeIn;
	}
	public void setMicroFadeIn(boolean isMicroFadeIn) {
		this.isMicroFadeIn = isMicroFadeIn;
	}

	public void resetPosition() {
        this.start = 0;
        this.end = (buffer != null ? buffer.length : 0);
        this.position = 0f;
        this.released = false;
        this.active = false;
        this.finished = false;
    }

    /**
     * Finds a nearby zero crossing starting at index in given direction (±1).
     * Returns the adjusted index or the original if none found within 256 samples.
     */
    private int findZeroCrossing(int index, int direction) {
        int limit = Math.min(buffer.length - 2, Math.max(1, index));
        int step = (direction >= 0) ? 1 : -1;
        float prev = buffer[limit];
        for (int i = 0; i < 256 && limit + i * step > 1 && limit + i * step < buffer.length - 1; i++) {
            int pos = limit + i * step;
            float next = buffer[pos];
            if ((prev <= 0 && next > 0) || (prev >= 0 && next < 0)) return pos;
            prev = next;
        }
        return index;
    }

    
}
