/**
 * 
 */
package net.paulhertz.pixelaudio;

import processing.core.PConstants;
import java.util.ArrayList;

/**
 * 
 */
public class WaveData {
	enum WaveState {
		ACTIVE, SOLO, MUTE, SUSPENDED
	}; // muting and solo states

	/** frequency */
	public float freq;
	/** amplitude */
	public float amp;
	/** fraction of TWO_PI in the range 0..1, */
	public float phase;
	/** TWO_PI * phase */
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
	/** animation steps */
	public int animSteps = WaveData.defaultAnimSteps;
	
	/** convenience variable, 30 seconds of animation at 24 frames per second */
	public static int defaultAnimSteps = 720; 	//  24 * 30 = 720
	/** support for old JSON format where phase was already scaled by TWO_PI */
	public static final boolean phaseScalesTwoPI = true;
	

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
	 * @param steps		number of frames for animation
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
	
	// TODO develop this idea: waveValue() is the signature method of an interface.
	// TODO can we precalculate this.freq * mapInc? mapInc is supposedly a constant.
	// ::::: sample amplitude = sin(initial phase + phase shift + frequency * i * (TWO_PI/n)) :::::
	// wd.phaseInc = (wd.cycles * TWO_PI)/animSteps; mapInc = TWO_PI / mapSize; 
	// Instead of incrementing phase at each step, we subtract (frame * phase increment)
	// from the initial phase, for historic reasons 8^).
	public float waveValue(int frame, int pos, float freqShift, float mapInc) {
		return (float) Math.sin(this.phaseTwoPi - frame * this.phaseInc + this.freq * freqShift * pos * mapInc);
	}
	public float waveValue(int frame, int pos, float mapInc) {
		return (float) Math.sin(this.phaseTwoPi - frame * this.phaseInc + this.freq * pos * mapInc);
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
