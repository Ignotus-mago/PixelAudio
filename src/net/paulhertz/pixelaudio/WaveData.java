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

package net.paulhertz.pixelaudio;

import processing.core.PConstants;
import java.util.ArrayList;

/**
 * Data storage for WaveSynth operator data.
 * NEW (as of 
 */
public class WaveData {
	public enum WaveState {
		ACTIVE, SOLO, MUTE, SUSPENDED
	}; // muting and solo states

	/** frequency */
	public float freq;
	/** amplitude */
	public float amp;
	/** fraction of TWO_PI in the range 0..1, */
	public float phase;
	/** TWO_PI * phase, used internally to calculate wave value */
	public float phaseTwoPi;
	/** number of times to cycle through TWO_PI over the duration of the animation */
	public float phaseCycles;
	/** increment to the phase for each step of animation = (cycles * TWO_PI)/(animation steps) 
	 *  Should be set when animation begins, otherwise is set to 0, unchanging.
	 */
	public float phaseInc = 0;
	/** DC offset/bias to add to generated amplitude values, a new setting, default 0 */
	public float dc = 0.0f;
	/** color associated with the wave */
	public int waveColor;
	/** runtime variable for muting */
	public boolean isMuted = false;
	/** tracking variable for mute, solo, etc. */
	public WaveState waveState;
	/** TODO animSteps is an external value from a WaveSynth and application context -- is there a better way to handle it? */
	public int animSteps = WaveData.defaultAnimSteps;
	/** convenience variable, 30 seconds of animation at 24 frames per second, also context dependent */
	public static int defaultAnimSteps = 720; 	//  24 * 30 = 720
	/** support for old JSON format where phase was already scaled by TWO_PI 
	 *  in the new format phase ranges over the open interval (0, 1), so we
	 *  scale it by TWO_PI and store the result in phaseTwoPi, which tracks phase information.
	 */
	public static final boolean phaseScalesTwoPI = true;
	
    // --------------------------------------------------------------------
    // Per-frame oscillator state for fast recurrence
    // --------------------------------------------------------------------
    private float s;   // sin(currentPhase)
    private float c;   // cos(currentPhase)
    private float cs;  // cos(deltaPhasePerPos)
    private float sn;  // sin(deltaPhasePerPos)

    private int preparedFrame = Integer.MIN_VALUE;
    private boolean framePrepared = false;

    // optional: for occasional renormalization to fight drift
    private int stepsSinceRenorm = 0;
    private static final int RENORM_PERIOD = 1024;

	

	public WaveData(float f, float a, float p, float dc, float cycles, int c, int animSteps, boolean phaseScalesTwoPi) {
		this.freq = f;
		this.amp = a;
		this.phase = p;
		if (phaseScalesTwoPi) {
			this.phaseTwoPi = p * PConstants.TWO_PI;
		} else {
			this.phaseTwoPi = p;
		}
		this.setDc(dc);
		this.phaseCycles = cycles;
		this.animSteps = animSteps;
		this.phaseInc = (cycles * PConstants.TWO_PI) / animSteps;
		this.waveColor = c;
		this.waveState = WaveState.ACTIVE;
	}

	/**
	 * Preferred constructor for WaveData
	 * 
	 * @param f			frequency
	 * @param a			amplitude
	 * @param p			phase, decimal fraction of TWO_PI, typically in the range [0..1]
	 * @param dc		DC component added to wave values
	 * @param cycles	number of cyles over one animation period
	 * @param c			color associated with this WaveData object
	 * @param steps		number of frames for animation
	 */
	public WaveData(float f, float a, float p, float dc, float cycles, int c, int steps) {
		this(f, a, p, dc, cycles, c, steps, WaveData.phaseScalesTwoPI);
	}

	/**
	 * Constructor for WaveData when animation steps are not known
	 * 
	 * @param f			frequency
	 * @param a			amplitude
	 * @param p			phase, decimal fraction of TWO_PI, typically in the range [0..1]
	 * @param dc		DC component added to wave values
	 * @param cycles	number of cyles over one animation period
	 * @param c			color associated with this WaveData object
	 */
	public WaveData(float f, float a, float p, float dc, float cycles, int c) {
		this(f, a, p, dc, cycles, c, WaveData.defaultAnimSteps, WaveData.phaseScalesTwoPI);
	}

	public WaveData() {
		this(440.0f, 0.5f, 0.5f, 0.0f, 1.0f, PixelAudioMapper.composeColor(127, 127, 127), WaveData.defaultAnimSteps, WaveData.phaseScalesTwoPI);
	}

	public void updateWaveData(float f, float a, float p, float cycles, int c, int steps) {
		this.freq = f;
		this.amp = a;
		this.phase = p;
		this.animSteps = steps;
		this.phaseTwoPi = p * PConstants.TWO_PI;
		this.phaseCycles = cycles;
		this.phaseInc = (cycles * PConstants.TWO_PI) / animSteps;
		this.waveColor = c;
	}

	public void setFreq(float f) {
		this.freq = f;
	}

	public void setAmp(float a) {
		this.amp = a;
	}

	public void setPhase(float p, boolean phaseScalesTwoPi) {
		this.phase = p;
		if (phaseScalesTwoPi) {
			this.phaseTwoPi = p * PConstants.TWO_PI;
		} else {
			this.phaseTwoPi = p;
		}
	}

	public void setPhase(float p) {
		setPhase(p, WaveData.phaseScalesTwoPI);
	}

	public void setCycles(float cycles, int steps) {
		this.phaseCycles = cycles;
		this.animSteps = steps;
		this.phaseInc = (cycles * PConstants.TWO_PI) / animSteps;
	}
	
	public void setCycles(float cycles) {
		this.phaseCycles = cycles;
		this.phaseInc = (cycles * PConstants.TWO_PI) / this.animSteps;
	}
	
	public void setAnimationSteps(int newSteps) {
		this.animSteps = newSteps;
		this.phaseInc = (this.phaseCycles * PConstants.TWO_PI) / animSteps;
		
	}

	public void setDc(float newDc) {
		this.dc = newDc;
	}

	public void setWaveColor(int c) {
		this.waveColor = c;
	}

	public void setWaveState(WaveState newWaveState) {
		this.waveState = newWaveState;
		if (this.waveState == WaveState.ACTIVE)
			this.isMuted = false;
		else if (this.waveState == WaveState.SOLO)
			this.isMuted = false;
		else
			this.isMuted = true;
	}
	
	// TODO develop this idea: use waveValue() as the signature method of an interface.
	// TODO can we precalculate this.freq * mapInc? mapInc is supposedly a constant.
	// ::::: sample amplitude = sin(initial phase + phase shift + frequency * i * (TWO_PI/n)) :::::
	// wd.phaseInc = (wd.cycles * TWO_PI)/animSteps; mapInc = TWO_PI / mapSize; 
	// Instead of incrementing phase at each step, we subtract (frame * phase increment)
	// from the initial phase, for historic reasons 8^).
	
	/**
	 * Experimenting with noise to change the wave forms. 
	 * 
	 * @param frame			current frame
	 * @param pos			pixel/audio sample index on signal path
	 * @param freqShift		experiment with shifting frequency
	 * @param mapInc		the increment in phase over the image pixels, typically TWO_PI / image size
	 * @return				amplitude value of wave for frame and pos
	 */
	public float waveValue(int frame, int pos, float freqShift, float mapInc) {
		return (float) Math.sin(this.phaseTwoPi - frame * this.phaseInc + this.freq * freqShift * pos * mapInc);
	}
	/**
	 * @param frame			current frame
	 * @param pos			pixel/audio sample index on signal path
	 * @param mapInc		the increment in phase over the image pixels, typically TWO_PI / image size
	 * @return				amplitude value of wave for frame and pos
	 */
	public float waveValue(int frame, int pos, float mapInc) {
		return (float) Math.sin(this.phaseTwoPi - frame * this.phaseInc + this.freq * pos * mapInc);
	}
	
	
	
	
    // --------------------------------------------------------------------
    // NEW: fast oscillator-based API
    // --------------------------------------------------------------------

    /**
     * Prepare oscillator state for this frame, assuming a constant freq
     * and a constant mapInc for pos stepping.
     *
     * Call once per frame (per WaveData) before stepping pos with nextValue().
     */
    public void prepareFrame(int frame, float mapInc) {
        // Optional micro-optimization: if same frame, skip recompute
        if (framePrepared && frame == preparedFrame) return;

        // Initial phase at pos = 0 (your existing formula)
        float phi0 = this.phaseTwoPi - frame * this.phaseInc;
        this.s = (float) Math.sin(phi0);
        this.c = (float) Math.cos(phi0);

        // Phase increment per pos step
        float delta = this.freq * mapInc;
        this.cs = (float) Math.cos(delta);
        this.sn = (float) Math.sin(delta);

        this.preparedFrame = frame;
        this.framePrepared = true;
        this.stepsSinceRenorm = 0;
    }

    /**
     * Variant that includes a per-frame freqShift.
     * Use when you want global detune / modulation per frame, not per pixel.
     */
    public void prepareFrame(int frame, float mapInc, float freqShift) {
        float phi0 = this.phaseTwoPi - frame * this.phaseInc;
        this.s = (float) Math.sin(phi0);
        this.c = (float) Math.cos(phi0);

        float delta = this.freq * freqShift * mapInc;
        this.cs = (float) Math.cos(delta);
        this.sn = (float) Math.sin(delta);

        this.preparedFrame = frame;
        this.framePrepared = true;
        this.stepsSinceRenorm = 0;
    }

    /**
     * Get the next value along the path, using oscillator recurrence.
     * Must be called AFTER prepareFrame(...), and then repeatedly
     * for pos = 0,1,2,... in order.
     */
    public float nextValue() {
        if (!framePrepared) {
            throw new IllegalStateException(
                "WaveData.nextValue() called before prepareFrame()."
            );
        }

        float val = this.s; // current sin(phase)

        // Step to next phase:
        float sNext = this.s * this.cs + this.c * this.sn;
        float cNext = this.c * this.cs - this.s * this.sn;
        this.s = sNext;
        this.c = cNext;

        // Optional renormalization to fight FP drift
        if (++stepsSinceRenorm >= RENORM_PERIOD) {
            float mag2 = this.s * this.s + this.c * this.c;
            if (mag2 != 0f) {
                float invMag = 1.0f / (float) Math.sqrt(mag2);
                this.s *= invMag;
                this.c *= invMag;
            }
            stepsSinceRenorm = 0;
        }

        return val;
    }

    /**
     * If you ever need to force this oscillator to be rebuilt next frame.
     */
    public void invalidateFrameState() {
        this.framePrepared = false;
        this.preparedFrame = Integer.MIN_VALUE;
    }

	
   
    
    
    
	
	public float rawPhaseAtFrame(int frame) {
		return this.phaseTwoPi - frame * this.phaseInc;
	}
	
	public float phaseAtFrame(int frame) {
		return ((rawPhaseAtFrame(frame) % PConstants.TWO_PI) + PConstants.TWO_PI) % PConstants.TWO_PI;
	}
	
	public float scaledPhaseAtFrame(int frame) {
		float range = 1.0f;
		float value = this.phase - (frame * this.phaseCycles / this.animSteps);
		return ((value % range) + range) % range;
	}
	
	/**
	 * Maps a value to a specified range (of a cyclic function)
	 * Explanation:
	 * 1. Subtract a: Translates the interval to [0,b−a)[0,b−a).
	 * 2. value % range: Reduces the value to (−range,range).
	 * 3. Adding range: Ensures the result is positive.
	 * 4. Final % range: Maps the result to [0,range)[0,range).
	 * 5. Adding a: Shifts the result back to the original interval [a,b).
	 * 
	 * @param value		float value to be mapped
	 * @param a			lower bound
	 * @param b			upper bound
	 * @return			value mapped to interval [a,b)
	*/
	public static float mapToPositivePhase(float value, float a, float b) {
	  float range = b - a;
	  return ((value - a) % range + range) % range + a;
	}

	
	public static ArrayList<WaveData> waveDataListCopy(ArrayList<WaveData> wdList) {
		ArrayList<WaveData> wdListCopy = new ArrayList<WaveData>();
		for (WaveData wd : wdList) {
			wdListCopy.add(wd.clone());
		}
		return wdListCopy;
	}

	public WaveData clone() {
		return new WaveData(this.freq, this.amp, this.phase, this.dc, this.phaseCycles, this.waveColor, this.animSteps);
	}

	// output values shown in Operators control panel GUI
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("freq: " + this.freq + ", ");
		sb.append("amp: " + this.amp + ", ");
		sb.append("phase: " + this.phase + ", ");
		// phaseTwoPi is not something the user sets in the GUI, so let's skip it
		// sb.append("phaseTwoPi: "+ this.phaseTwoPi +", ");
		sb.append("dc: " + this.dc + ", ");
		sb.append("cycles: " + this.phaseCycles + ", ");
		// sb.append("phaseInc: "+ this.phaseInc +", ");
		// ignore alpha channel, we aren't using it for weighted color
		// alpha would probably work best as a separately generated mask
		int[] rgb = PixelAudioMapper.rgbComponents(this.waveColor);
		sb.append("color: (" + rgb[0] + ", " + rgb[1] + ", " + rgb[2] + "), ");
		String muted = this.isMuted ? "Y" : "N";
		sb.append("mute: " + muted);
		return sb.toString();
	}
	
	
}
