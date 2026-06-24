/*
 *  Copyright (c) 2024 - 2025 by Paul Hertz <ignotus@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.paulhertz.pixelaudio.sampler;

/**
 * PASamplerVoice - a single playback "voice" reading from a shared mono buffer.
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
    
    /**
     * Constructs an inactive sampler voice.
     *
     * @param buffer shared mono source buffer
     * @param sampleRate playback sample rate in Hz
     */
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
    
    /**
     * Activates the voice over a buffer region.
     *
     * @param start buffer index to start playback
     * @param length playback length in samples
     * @param gain linear gain multiplier
     * @param envParams optional ADSR envelope parameters
     * @param pitch pitch or playback-rate multiplier
     * @param pan stereo pan position
     * @param looping true to loop this voice
     */
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

        // intentionally dead code, can be reactivated for debugging
        if (DEBUG && frameCounter++ % 2000 == 0) {
            System.out.printf("[Voice %d] Activated: start=%d end=%d gain=%.3f pitch=%.3f pan=%.3f%n",
                    voiceId, start, end, gain, pitch, pan);
        }
    }

    /**
     * Generates the next mono sample for this voice.
     *
     * @return next sample value, or 0 when inactive/finished
     */
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
    
    /** Starts the release stage and disables looping. */
    public void release() {
        if (!released) {
            released = true;
            if (envelope != null) envelope.noteOff();
        }
        looping = false;
    }

    /** Stops this voice immediately. */
    public void stop() {
        active = false;
        released = false;
        finished = true;
    }

    /** Resets this voice to the beginning of its current buffer and marks it inactive. */
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
    
    /**
     * Replaces the source buffer and resets the voice.
     *
     * @param buffer shared mono source buffer
     */
    public synchronized void setBuffer(float[] buffer) {
        this.buffer = buffer;
        resetPosition();
    }

    /**
     * Replaces the source buffer and playback sample rate, then resets the voice.
     *
     * @param buffer shared mono source buffer
     * @param playbackSampleRate playback sample rate in Hz
     */
    public synchronized void setBuffer(float[] buffer, float playbackSampleRate) {
        this.buffer = buffer;
        this.playbackSampleRate = playbackSampleRate;
        resetPosition();
    }
    

    // ------------------------------------------------------------------------
    // Accessors and state checks
    // ------------------------------------------------------------------------
    
    /** @return true while the voice is in its active playback window */
    public boolean isActive()     { return active; }
    /** @return true while the voice is releasing but not finished */
    public boolean isReleasing()  { return released && !finished; }
    /** @return true when the voice has finished playback */
    public boolean isFinished()   { return finished; }

    /** @return true when this voice is configured to loop */
    public boolean isLooping()    { return looping; }
    /** @param looping true to loop this voice */
    public void setLooping(boolean looping) { this.looping = looping; }

    /** @return stereo pan position */
    public float getPan()         { return pan; }
    /** @return unique voice identifier */
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

    /** @return true when activation searches for a nearby zero crossing */
    public boolean isFindZeroCrossing() { return isFindZeroCrossing; }
    /** @param val true to search for a nearby zero crossing on activation */
    public void setFindZeroCrossing(boolean val) { this.isFindZeroCrossing = val; }

    /** @return true when activation applies a short fade-in */
    public boolean isMicroFadeIn() { return isMicroFadeIn; }
    /** @param val true to apply a short fade-in on activation */
    public void setMicroFadeIn(boolean val) { this.isMicroFadeIn = val; }
    
    /**
     * Sets the playback sample rate.
     *
     * @param newRate playback sample rate in Hz
     */
    public void setPlaybackSampleRate(float newRate) {
    	this.playbackSampleRate = newRate;
    }
        
}
