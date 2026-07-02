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

package net.paulhertz.pixelaudio.granular;

import ddf.minim.UGen;
import ddf.minim.analysis.WindowFunction;
import net.paulhertz.pixelaudio.sampler.ADSRParams;
import net.paulhertz.pixelaudio.sampler.SimpleADSR;


/**
 * Single voice for rendering one granular {@link PASource}.
 *
 * <p>{@code PAGranularVoice} is the voice layer in PixelAudio's granular synthesis chain.
 * A {@link PAGranularSampler} activates voices with a source, macro envelope, gain, pan,
 * looping flag, and optional grain-window settings. The voice then pulls audio from its
 * {@link PASource}, applies the macro envelope and voice-level pan/gain, and returns one
 * sample frame at a time to the sampler.</p>
 *
 * <p>The voice renders internally in blocks. When the sampler asks for the next stereo sample,
 * the voice reads from cached block buffers; when those buffers are exhausted, it asks the
 * source to render the next block. This keeps source rendering block-oriented while allowing
 * the sampler to mix voices one frame at a time inside Minim's {@code UGen} callback.</p>
 *
 * <p>The envelope used here is a macro envelope over the whole voice. Individual grains may
 * still have their own source-level windows, supplied through {@link WindowFunction} and handled
 * by the source.</p>
 *
 * @see PAGranularSampler
 * @see PASource
 * @see PABurstGranularSource
 */
public class PAGranularVoice {

    // ------------------------------------------------------------------------
    // Core state
    // ------------------------------------------------------------------------
    private static long NEXT_VOICE_ID = 0;

    /** Granular source: a {@link PABurstGranularSource}, in the current design using @link PAGranularInstrumentDirector} */
    private PASource source;
    private final int blockSize;

    private long voiceId;
    private boolean active;
    private boolean released;
    private boolean finished;

    private float gain = 1f;
    private float pan = 0f;           // -1..+1 stereo pan
    private boolean looping = false;  // loop entire path

    private float playbackSampleRate;

    // Block buffers
    private final float[] blockL;
    private final float[] blockR;
    private int cursor = 0;
    private long absSample = 0;

    // Envelope
    private SimpleADSR envelope;
    ADSRParams defaultEnvParams = new ADSRParams(1.0f, 0.01f, 0.02f, 0.9375f, 0.125f);
    
    // pan gain
    private float panGainL = 1f;
    private float panGainR = 1f;
    
    /** granular window function, may be null, in which case defaults apply */
    private WindowFunction grainWindow;
    private int grainLenSamples = 1024;

    private boolean endTriggered = false;


    /**
     * Creates an inactive granular voice.
     *
     * @param source               initial source for the voice; may be replaced during activation
     * @param blockSize            internal render-block size
     * @param playbackSampleRate   sample rate used by the voice envelope
     */
    public PAGranularVoice(PASource source, int blockSize, float playbackSampleRate) {
        this.source = source;
        this.blockSize = blockSize;
        this.playbackSampleRate = playbackSampleRate;
        this.blockL = new float[blockSize];
        this.blockR = new float[blockSize];
        this.active = false;
    }
    
    public void setSource(PASource source) {
    	this.source = source;
    }

    // ------------------------------------------------------------------------
    // Activation
    // ------------------------------------------------------------------------
    
    /**
     * Activates this voice with no external grain-window override.
     *
     * @param source      source to render
     * @param envParams   macro envelope parameters, or null to use the voice default
     * @param gain        voice gain
     * @param pan         stereo pan in the range [-1, 1]
     * @param looping     true to loop the source path when the source length is reached
     */
    public void activate(PASource source,
    		ADSRParams envParams,
    		float gain,
    		float pan,
    		boolean looping) {
    	activate(source, envParams, gain, pan, looping, null, 1024);
    }

    /**
     * Activates this voice with a source and playback settings.
     *
     * <p>Activation resets voice state, assigns a new voice id, initializes pan gains,
     * rewinds the source with {@link PASource#seekTo(long)}, optionally pushes grain-window
     * settings into the source, and starts a fresh macro envelope.</p>
     *
     * @param source            source to render
     * @param envParams         macro envelope parameters, or null to use the voice default
     * @param gain              voice gain
     * @param pan               stereo pan in the range [-1, 1]
     * @param looping           true to loop the source path when the source length is reached
     * @param grainWindow       optional grain window to pass to the source
     * @param grainLenSamples   grain length associated with {@code grainWindow}
     */
    public void activate(PASource source,
    		ADSRParams envParams,
    		float gain,
    		float pan,
    		boolean looping,
    		WindowFunction grainWindow,
    		int grainLenSamples) {

    	this.source = source;

    	this.voiceId = NEXT_VOICE_ID++;
    	this.gain = gain;
    	this.pan = clampPan(pan);
    	updatePanGains();
    	this.looping = looping;

    	this.grainWindow = grainWindow;
    	this.grainLenSamples = Math.max(1, grainLenSamples);

    	this.released = false;
    	this.finished = false;
    	this.active = true;
    	this.endTriggered = false;    // bug fix, 1 July 2026

    	// Reset block state
    	this.cursor = 0;
    	this.absSample = 0;

    	// Start granular engine at absSample = 0
    	// this is the critical first step to open the PASource for audio processing
    	if (this.source != null) {
    		this.source.seekTo(0);
    		// Only push grain window config when explicitly provided.
    		// This preserves legacy sources' internally-configured grainLength.
    		if (grainWindow != null) {
    			this.source.setGrainWindow(grainWindow, this.grainLenSamples);
    		}    	
    	}

    	// Envelope setup (macro envelope over a gesture): always have an envelope
    	ADSRParams useEnv = (envParams != null) ? envParams : defaultEnvParams;

    	// Envelope setup (macro envelope over a gesture)
    	envelope = new SimpleADSR(
    			useEnv.getAttack(),
    			useEnv.getDecay(),
    			useEnv.getSustain(),
    			useEnv.getRelease()
    			);
    	envelope.setSampleRate(playbackSampleRate);
    	envelope.noteOn();
    }

    // ------------------------------------------------------------------------
    // fetch next sample (stereo, default method)
    // ------------------------------------------------------------------------
    
    /**
     * Writes the next stereo sample frame from this voice.
     *
     * <p>If the voice is inactive or finished, both output samples are set to 0. Otherwise
     * this method refills the internal block when needed, reads the next cached source sample,
     * advances the macro envelope, applies voice gain and equal-power pan, and writes left and
     * right samples into {@code outLR}.</p>
     *
     * @param outLR two-element array receiving left and right sample values
     */
    public void nextSampleStereo(float[] outLR) {
        if (!active || finished) {
            outLR[0] = 0f;
            outLR[1] = 0f;
            return;
        }

        if (cursor >= blockSize) {
            refillBlock();
        }

        float l = blockL[cursor];
        float r = blockR[cursor];
        cursor++;

        float envValue = (envelope != null ? envelope.tick() : 1f);
        float amp = gain * envValue;

        // Apply cached voice pan gains (equal-power)
        l *= amp * panGainL;
        r *= amp * panGainR;

        outLR[0] = l;
        outLR[1] = r;

        if (released && (envelope == null || envelope.isFinished())) {
            active = false;
            finished = true;
        }
    }

    // ------------------------------------------------------------------------
    // fetch next sample (mono, for future option)
    // ------------------------------------------------------------------------
    
    /**
     * Returns the next mono sample from this voice.
     *
     * <p>This method mirrors {@link #nextSampleStereo(float[])} but folds the panned stereo
     * block sample to mono. It is retained for future mono-output use.</p>
     *
     * @return next mono sample, or 0 if the voice is inactive or finished
     */
    public float nextSample() {
        if (!active || finished) return 0f;

        if (cursor >= blockSize) {
            refillBlock();
        }

        float l = blockL[cursor] * panGainL;
        float r = blockR[cursor] * panGainR;
        cursor++; // advance exactly once

        float envValue = (envelope != null ? envelope.tick() : 1f);
        float amp = gain * envValue;

        float mono = 0.5f * (l + r) * amp;

        if (released && (envelope == null || envelope.isFinished())) {
            active = false;
            finished = true;
        }

        return mono;
    }

    // ------------------------------------------------------------------------
    // Block refill
    // ------------------------------------------------------------------------
    /**
     * Refills the internal stereo block buffers from the source.
     *
     * <p>The voice asks its {@link PASource} to render the block beginning at
     * {@code absSample}, then advances {@code absSample} by {@code blockSize}. If looping is
     * enabled and the source span has ended, the voice rewinds the source and starts again.
     * If looping is disabled and the source span has ended, the voice triggers envelope
     * release or stops immediately when no envelope is available.</p>
     */
    private void refillBlock() {
        // Zero out block
        for (int i = 0; i < blockSize; i++) {
            blockL[i] = 0f;
            blockR[i] = 0f;
        }

        source.renderBlock(absSample, blockSize, blockL, blockR);
        absSample += blockSize;
        cursor = 0;

        // Handle looping (restart granular path)
        long sourceLen = source.lengthSamples();
        if (looping && absSample >= sourceLen) {
            absSample = 0;
            endTriggered = false;
            source.seekTo(0);
            return;
        }
        
        // non-looping end of source => release/stop the voice
        if (!looping && absSample >= sourceLen) {
            if (!endTriggered) {
                endTriggered = true;
                // If we have an ADSR, let it release naturally.
                // If no ADSR, stop immediately.
                if (envelope != null) {
                    release();            // calls noteOff()
                } else {
                    stop();
                }
            }
        }
    }
    
    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------
    
     private void updatePanGains() {
    	float p = clampPan(pan);
    	float angle = (p + 1.0f) * 0.25f * (float) Math.PI;
    	panGainL = (float) Math.cos(angle);
    	panGainR = (float) Math.sin(angle);
    }


    // ------------------------------------------------------------------------
    // Lifecycle control
    // ------------------------------------------------------------------------
    /**
     * Starts the macro envelope release stage for this voice.
     *
     * <p>Release is less abrupt than {@link #stop()} because the voice remains active until
     * its envelope finishes.</p>
     */
    public void release() {
        if (!released) {
            released = true;
            if (envelope != null) envelope.noteOff();
        }
    }

    /**
     * Immediately marks this voice inactive and finished.
     *
     * <p>Unlike {@link #release()}, this bypasses the envelope release stage.</p>
     */
    public void stop() {
        active = false;
        finished = true;
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------
    public boolean isActive() { return active; }
    public boolean isReleasing() { return released && !finished; }
    public boolean isFinished() { return finished; }

    public long getVoiceId() { return voiceId; }

    public float getPan() { return pan; }

    private static float clampPan(float p) {
        if (p < -1f) return -1f;
        if (p > 1f) return 1f;
        return p;
    }
}
