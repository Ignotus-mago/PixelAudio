package net.paulhertz.pixelaudio.voices;

import ddf.minim.ugens.ADSR;
import ddf.minim.ugens.Constant;

/**
 * Represents a single playback "voice" reading from a shared mono buffer.
 * Each voice has its own pitch, gain, pan, looping, and envelope.
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

    private ADSR envelope;
    private Constant envCarrier;  
    private final float[] envFrame = new float[1]; // single-sample output for envelope
    private boolean released = false;
    private int silenceCounter = 0;
    private static final int SILENCE_THRESHOLD = 512; // samples near 0 before deactivate
    
    // debugging
    private static final boolean DEBUG = true;
    int frameCounter = 0;

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

        if (envParams != null) {
            this.envelope = envParams.toADSR();                // e.g., new ADSR(attack, decay, sustain, release)
            this.envelope.setSampleRate(playbackSampleRate);
            this.envCarrier = new Constant(envParams.getMaxAmp());  // steady “carrier” at your peak amplitude
            this.envCarrier.patch(this.envelope);              // <<— this is the key line
            this.envelope.noteOn();
        } 
        else {
            this.envelope = null;
            this.envCarrier = null;
        }
        System.out.printf("Voice %d activated: start=%d end=%d len=%d rate=%.3f gain=%.3f pan=%.3f looping=%b%n",
        		           voiceId, start, end, (end - start), rate, gain, pan, looping);
    }

    public float nextSample() {
        if (!active) return Float.NaN;

        int idx = (int) position;
 
        if (DEBUG && frameCounter++ % 1000 == 0) {
            System.out.printf("[Voice %d] idx=%d pos=%.2f env=%.4f gain=%.3f active=%b looping=%b released=%b%n",
                voiceId, idx, position, envFrame[0], gain, active, looping, released);
        }

        if (idx >= end) {
            if (looping) {
                position = start;
                idx = start;
            } 
            else {
                if (!released) {
                    released = true;
                    if (envelope != null) envelope.noteOff();
                }
                active = false;
                return 0.0f;  // <-- prevents out-of-bounds read
            }
        }

        // Drive envelope
        float envValue = 1.0f;
        if (envelope != null) {
            envelope.tick(envFrame);
            envValue = envFrame[0];
        }

        float sample = buffer[idx] * gain * envValue;
        position += rate;

        // Detect sustained silence for deactivation
        if (released && Math.abs(sample) < 1e-6f) {
            if (++silenceCounter > SILENCE_THRESHOLD) {
                active = false;
            }
        } 
        else {
            silenceCounter = 0;
        }

        return sample;
    }

    /** Begin release phase for smooth voice stealing. */
    public void release() {
        if (!released) {
            released = true;
            if (envelope != null) envelope.noteOff();
        }
        looping = false;
    }

    /** Hard stop (no envelope). */
    public void stop() {
        active = false;
        looping = false;
        released = true;
        // tidy up patch (optional but clean)
        if (envCarrier != null && envelope != null) {
            envCarrier.unpatch(envelope);
        }
    }

    // ---- Getters ----

    public boolean isActive() { return active; }
    public boolean isLooping() { return looping; }
    public float getPosition() { return position; }
    public float getGain() { return gain; }
    public float getPan() { return pan; }
    public long getVoiceId() { return voiceId; }
    public int getStart() { return start; }
    public int getEnd() { return end; }
    
    /** Returns the current sample rate for this voice. */
    public synchronized float getPlaybackSampleRate() { return playbackSampleRate; }
    
    /** Update the sample rate for this voice (used when sampler changes rate). */
    public synchronized void setPlaybackSampleRate(float newRate) {
    	if (newRate > 0f) {
    		this.playbackSampleRate = newRate;
    	}
    }

    public void setLooping(boolean looping) { this.looping = looping; }
}
