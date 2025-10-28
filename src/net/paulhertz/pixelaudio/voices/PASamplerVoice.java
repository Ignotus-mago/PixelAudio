package net.paulhertz.pixelaudio.voices;

import ddf.minim.ugens.ADSR;
import ddf.minim.ugens.Constant;

/**
 * Represents a single playback "voice" reading from a shared mono buffer.
 * Each voice has its own pitch, gain, pan, looping, and envelope.
 * 
 * Hybrid envelope system:
 *  - Uses Minim's ADSR if patched in a live UGen graph.
 *  - Otherwise, falls back to a manual "SimpleADSR" tick inside nextSample().
 */
public class PASamplerVoice {
    private static long NEXT_VOICE_ID = 0;

    private float[] buffer;
    private float playbackSampleRate;

    private long voiceId;
    private boolean active;
    private boolean looping;

    private int start;
    private int end;
    private float position;
    private float rate;
    private float gain;
    private float pan;

    // --- Envelope handling ---
    private ADSR envelope;         // Minim envelope (UGen)
    private Constant envCarrier;   // Input UGen for ADSR (only used if patched)
    private SimpleADSR fallbackEnv; // Fallback software envelope
    private final float[] envFrame = new float[1];

    private boolean released = false;
    private int silenceCounter = 0;
    private static final int SILENCE_THRESHOLD = 512;
    
    private static final boolean DEBUG = true;
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
        this.voiceId = NEXT_VOICE_ID++;
        this.start = Math.max(0, start);
        this.end = Math.min(buffer.length, start + Math.max(0, length));
        this.position = this.start;
        this.rate = pitch;
        this.gain = gain;
        this.pan = Math.max(-1f, Math.min(1f, pan));
        this.looping = looping;
        this.active = (this.start < this.end);
        this.released = false;
        this.silenceCounter = 0;

        // --- Envelope setup ---
        if (envParams != null) {
            try {
                // Try Minim ADSR
                this.envelope = envParams.toADSR();
                this.envelope.setSampleRate(playbackSampleRate);
                this.envCarrier = new Constant(envParams.getMaxAmp());
                this.envCarrier.patch(this.envelope);
                this.envelope.noteOn();

                // Initialize fallback envelope (used if Minim ADSR isn't producing)
                this.fallbackEnv = new SimpleADSR(envParams.getAttack(), envParams.getDecay(),
                                                  envParams.getSustain(), envParams.getRelease());
                this.fallbackEnv.noteOn();
            } 
            catch (Exception e) {
                // fallback only
                this.envelope = null;
                this.envCarrier = null;
                this.fallbackEnv = new SimpleADSR(envParams.getAttack(), envParams.getDecay(),
                                                  envParams.getSustain(), envParams.getRelease());
                this.fallbackEnv.noteOn();
            }
        } 
        else {
            this.envelope = null;
            this.envCarrier = null;
            this.fallbackEnv = null;
        }
        
        /*
        if (DEBUG) {
            System.out.printf("[Voice %d] activated start=%d end=%d len=%d rate=%.3f gain=%.3f pan=%.3f looping=%b%n",
                              voiceId, start, end, (end - start), rate, gain, pan, looping);
        }
        */
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
        if (!active) return Float.NaN;
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
            } else {
                if (!released) {
                    released = true;
                    if (envelope != null) envelope.noteOff();
                    if (fallbackEnv != null) fallbackEnv.noteOff();
                }
                active = false;
                return 0.0f;
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
        } else if (fallbackEnv != null) {
            envValue = fallbackEnv.tick();
        }

        float sample = buffer[idx] * gain * envValue;
        position += rate;

        // Silence auto-deactivation
        if (released && Math.abs(sample) < 1e-6f) {
            if (++silenceCounter > SILENCE_THRESHOLD) active = false;
        } else {
            silenceCounter = 0;
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
    
    public void setBuffer(float[] buffer) {
    	this.buffer = buffer;
    }
    
    public void setBuffer(float[] buffer, float playbackSampleRate) {
    	this.buffer = buffer;
    	this.playbackSampleRate = playbackSampleRate;
    }
    
    public boolean isActive() { return active; }
    public boolean isLooping() { return looping; }
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
    public void setLooping(boolean looping) { this.looping = looping; }
    

    // -------------------------------------------------------------------------
    // Internal fallback ADSR class
    // -------------------------------------------------------------------------
    private static class SimpleADSR {
        private final float attack, decay, sustain, release;
        private float value = 0f;
        private boolean attackPhase, decayPhase, sustainPhase, releasePhase;

        public SimpleADSR(float attack, float decay, float sustain, float release) {
            this.attack = attack;
            this.decay = decay;
            this.sustain = sustain;
            this.release = release;
        }

        public void noteOn() {
            attackPhase = true;
            decayPhase = false;
            sustainPhase = false;
            releasePhase = false;
        }

        public void noteOff() {
            releasePhase = true;
            attackPhase = false;
            decayPhase = false;
        }

        public float tick() {
            if (attackPhase) {
                value += attack;
                if (value >= 1.0f) {
                    value = 1.0f;
                    attackPhase = false;
                    decayPhase = true;
                }
            } else if (decayPhase) {
                value -= decay;
                if (value <= sustain) {
                    value = sustain;
                    decayPhase = false;
                    sustainPhase = true;
                }
            } else if (releasePhase) {
                value -= release;
                if (value <= 0f) {
                    value = 0f;
                    releasePhase = false;
                }
            }
            return Math.max(0f, Math.min(1f, value));
        }
        
        public float getValue() { return this.value; }
    }
}
