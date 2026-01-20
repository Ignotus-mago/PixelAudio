package net.paulhertz.pixelaudio.voices;

/**
 * PASamplerVoice — a single playback "voice" reading from a shared mono buffer.
 *
 * Each voice handles:
 *   - playback position and pitch
 *   - per-voice amplitude and pan
 *   - one independent SimpleADSR envelope (sample-accurate)
 *   - optional zero-crossing start and micro-fade-in
 *
 * Voices can be smoothly released and are recycled once the envelope finishes.
 */
public class PASamplerVoice {

    // ------------------------------------------------------------------------
    // Core state
    // ------------------------------------------------------------------------
    private static long NEXT_VOICE_ID = 0;

    private float[] buffer;
    private float playbackSampleRate;

    private long voiceId;
    private boolean active;
    private boolean released;
    private boolean finished;
    private boolean looping;

    private int start;
    private int end;
    private float position;
    private float rate;
    private float gain;
    private float pan;

    // ------------------------------------------------------------------------
    // Envelope
    // ------------------------------------------------------------------------
    private SimpleADSR envelope;

    // Optional pre-start processing
    private boolean isFindZeroCrossing = false;
    private boolean isMicroFadeIn = false;

    private static final boolean DEBUG = false;
    private int frameCounter = 0;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------
    public PASamplerVoice(float[] buffer, float sampleRate) {
        this.buffer = buffer;
        this.playbackSampleRate = sampleRate;
        this.active = false;
        this.released = false;
        this.finished = false;
    }

    // ------------------------------------------------------------------------
    // Activation
    // ------------------------------------------------------------------------
    public void activate(int start, int length, float gain,
                         ADSRParams envParams, float pitch, float pan, boolean looping) {
        this.active = false;
        this.released = false;
        this.finished = false;
        this.looping = looping;

        this.voiceId = NEXT_VOICE_ID++;
        this.start = Math.max(0, start);
        this.end = Math.min(buffer.length, start + Math.max(0, length));
        this.position = this.start;
        this.rate = pitch;
        this.gain = gain;
        this.pan = Math.max(-1f, Math.min(1f, pan));

        // Optional zero-crossing adjustment
        if (isFindZeroCrossing) {
            this.start = findZeroCrossing(this.start, 1);
            this.position = this.start;
        }

        // Envelope setup
        if (envParams != null) {
            envelope = new SimpleADSR(
                    envParams.getAttack(),
                    envParams.getDecay(),
                    envParams.getSustain(),
                    envParams.getRelease()
            );
            envelope.setSampleRate(playbackSampleRate);
            envelope.noteOn();
        } else {
            envelope = null;
        }

        // Optional micro-fade at buffer start
        if (isMicroFadeIn) applyMicroFadeIn();

        this.active = (this.start < this.end);

        if (DEBUG && frameCounter++ % 2000 == 0) {
            System.out.printf("[Voice %d] Activated: start=%d end=%d gain=%.3f pitch=%.3f pan=%.3f%n",
                    voiceId, start, end, gain, pitch, pan);
        }
    }

    public float nextSample() {
        if (finished || buffer == null) return 0f;

        int idx = (int) position;

        // --- 1. Trigger release once we pass the "note" window ---
        if (idx >= end && !released) {
            released = true;
            if (envelope != null) envelope.noteOff();
        }

        // --- 2. Read sample safely ---
        float base = (idx >= 0 && idx < buffer.length) ? buffer[idx] : 0f;

        // --- 3. Advance ---
        position += rate;

        // --- 4. Envelope always ticks ---
        float envValue = (envelope != null) ? envelope.tick() : 1f;
        float sample = base * gain * envValue;

        // --- 5. Voice finishes when envelope fully decays ---
        if (released && (envelope == null || envelope.isFinished())) {
            active = false;
            finished = true;
        }

        return sample;
    }

    // ------------------------------------------------------------------------
    // Lifecycle control
    // ------------------------------------------------------------------------
    public void release() {
        if (!released) {
            released = true;
            if (envelope != null) envelope.noteOff();
        }
        looping = false;
    }

    public void stop() {
        active = false;
        released = false;
        finished = true;
    }

    public void resetPosition() {
        this.start = 0;
        this.end = (buffer != null ? buffer.length : 0);
        this.position = 0f;
        this.released = false;
        this.active = false;
        this.finished = false;
    }

    // ------------------------------------------------------------------------
    // Buffer management
    // ------------------------------------------------------------------------
    public synchronized void setBuffer(float[] buffer) {
        this.buffer = buffer;
        resetPosition();
    }

    public synchronized void setBuffer(float[] buffer, float playbackSampleRate) {
        this.buffer = buffer;
        this.playbackSampleRate = playbackSampleRate;
        resetPosition();
    }

    // ------------------------------------------------------------------------
    // Accessors and state checks
    // ------------------------------------------------------------------------
    public boolean isActive()     { return active; }
    public boolean isReleasing()  { return released && !finished; }
    public boolean isFinished()   { return finished; }

    public boolean isLooping()    { return looping; }
    public void setLooping(boolean looping) { this.looping = looping; }

    public float getPan()         { return pan; }
    public long getVoiceId()      { return voiceId; }

    // ------------------------------------------------------------------------
    // Optional features
    // ------------------------------------------------------------------------
    
    private void applyMicroFadeIn() {
        int fadeSamples = Math.min(64, end - start);
        float fadeAmp = 0f;
        float fadeStep = 1f / fadeSamples;
        for (int i = 0; i < fadeSamples; i++) {
            buffer[start + i] *= fadeAmp;
            fadeAmp += fadeStep;
        }
    }

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

    public boolean isFindZeroCrossing() { return isFindZeroCrossing; }
    public void setFindZeroCrossing(boolean val) { this.isFindZeroCrossing = val; }

    public boolean isMicroFadeIn() { return isMicroFadeIn; }
    public void setMicroFadeIn(boolean val) { this.isMicroFadeIn = val; }
    
    public void setPlaybackSampleRate(float newRate) {
    	this.playbackSampleRate = newRate;
    }
    
    // TODO consider a utility class for tailoring audio sample arrays with normalization, DC subtraction, etc.
    /*
     *   // worth considering for noise reduction, but the sum call is not supported in Java Arrays
    public float[] subtractDC(float[] buffer) {
    	float mean = Arrays.stream(buffer).sum() / buffer.length;
    	for (int i = 0; i < buffer.length; i++) buffer[i] -= mean;
    	return buffer;
    }
    */
    
}
