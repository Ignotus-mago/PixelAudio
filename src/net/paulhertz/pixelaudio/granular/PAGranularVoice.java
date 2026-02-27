package net.paulhertz.pixelaudio.granular;

import ddf.minim.UGen;
import ddf.minim.analysis.WindowFunction;
import net.paulhertz.pixelaudio.sampler.ADSRParams;
import net.paulhertz.pixelaudio.sampler.SimpleADSR;


/**
 * PAGranularVoice
 *
 * A single granular "voice" driven by a PASource.
 *
 * Provides:
 *  - nextSample() API so it can be mixed like PASamplerVoice
 *  - voice-level ADSR envelope (macro envelope)
 *  - per-voice amplitude + pan
 *  - optional looping of entire grain path
 *
 * Internally uses:
 *  - Block buffer (blockL / blockR)
 *  - Absolute sample counter (absSample)
 *  - Cursor into current block
 */
public class PAGranularVoice {

    // ------------------------------------------------------------------------
    // Core state
    // ------------------------------------------------------------------------
    private static long NEXT_VOICE_ID = 0;

    private PASource source;       // Granular source (PathGranularSource, BasicIndexGranularSource, etc.)
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
    
    // granular window function
    private WindowFunction grainWindow; // may be null; source/director defaults
    private int grainLenSamples = 1024;

    private boolean endTriggered = false;


    // Constructor
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
    
    public void activate(PASource source,
    		ADSRParams envParams,
    		float gain,
    		float pan,
    		boolean looping) {
    	activate(source, envParams, gain, pan, looping, null, 1024);
    }

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
    public void release() {
        if (!released) {
            released = true;
            if (envelope != null) envelope.noteOff();
        }
    }

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
