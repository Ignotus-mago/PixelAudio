/**
 * 
 */
package net.paulhertz.pixelaudio;

import java.awt.Color;
import java.util.ArrayList;

import processing.core.PApplet;

import net.paulhertz.pixelaudio.WaveData.WaveState;

/**
 * 
 */
public class WaveSynthBuilder {
	public static final double semitoneFac = Math.pow(2, 1 / 12.0);

	/**
	 * Generates an ArrayList of WaveData objects to be used by a WaveSynth to
	 * generate RGB pixel values and (on request) audio signal values.
	 *
	 * @return an ArrayList of WaveData objects
	 */
	public static ArrayList<WaveData> synthTrumpet(float fundamental, int howManyPartials, float pianoKey, int animSteps) {
		ArrayList<WaveData> list = new ArrayList<WaveData>();
		if (howManyPartials < 1)
			howManyPartials = 1;
		// funda is the fundamental of a musical tone that is somewhat like a trumpet
		// in its frequency spectrum. Vary it to see how the image and sound change.
		float funda = fundamental;
		float frequency = funda;
		float amplitude = 0.55f;
		float phase = 0.766f;
		float dc = 0.0f;
		float cycles = -8.0f;
		int waveColor = PixelAudioMapper.composeColor(0, 89, 233);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		int steps = animSteps;
		WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 1)
			return list;
		frequency = 2 * funda;
		amplitude = 0.52f;
		phase = -0.89f;
		cycles = 8.0f;
		waveColor = PixelAudioMapper.composeColor(89, 199, 55);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 2)
			return list;
		frequency = 3 * funda;
		amplitude = 0.6f;
		phase = -0.486f;
		cycles = 3.0f;
		waveColor = PixelAudioMapper.composeColor(254, 89, 110);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 3)
			return list;
		frequency = 4 * funda;
		amplitude = 0.45f;
		phase = -0.18616974f;
		cycles = -2.0f;
		waveColor = PixelAudioMapper.composeColor(89, 110, 233);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 4)
			return list;
		frequency = 5 * funda;
		amplitude = 0.42f;
		phase = 0.6846085f;
		cycles = -5.0f;
		waveColor = PixelAudioMapper.composeColor(233, 34, 21);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 5)
			return list;
		frequency = 6 * funda;
		amplitude = 0.45f;
		phase = 0.68912f;
		cycles = 13.0f;
		waveColor = PixelAudioMapper.composeColor(220, 199, 55);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 6)
			return list;
		frequency = 7 * funda;
		amplitude = 0.25f;
		phase = 0.68f;
		cycles = 11.0f;
		waveColor = PixelAudioMapper.composeColor(159, 190, 255);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 7)
			return list;
		frequency = 8 * funda;
		amplitude = 0.32f;
		phase = 0.68f;
		cycles = -11.0f;
		waveColor = PixelAudioMapper.composeColor(209, 178, 117);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		return list;
	}
	
	/**
	 * Generates an ArrayList of WaveData objects to be used by a WaveSynth to
	 * generate RGB pixel values and (on request) audio signal values.
	 *
	 * @return an ArrayList of WaveData objects
	 */
	public static ArrayList<WaveData> synthDoubleStop(float funda1, float funda2, float pianoKey1, float pianoKey2, int animSteps) {
		ArrayList<WaveData> list = new ArrayList<WaveData>();
		float detune = 0.0625f;
		//
		// First Stop
		float funda = funda1;
		float pianoKey = pianoKey1;
		float frequency = funda1;
		float amplitude = 0.6f;
		float phase = 0.75f;
		float dc = 0.0f;
		float cycles = 1.0f;
		int waveColor = PixelAudioMapper.composeColor(0, 89, 233);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		int steps = animSteps;
		WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		frequency = 2 * funda;
		amplitude = 0.4f;
		phase = -0.5f;
		cycles = -2.0f;
		waveColor = PixelAudioMapper.composeColor(89, 199, 55);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		frequency = 4.0f * funda + detune;
		amplitude = 0.3f;
		phase = -0.125f;
		cycles = 3.0f;
		waveColor = PixelAudioMapper.composeColor(89, 110, 233);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		// Second Stop
		funda = funda2;
		pianoKey = pianoKey2;
		frequency = funda2;
		amplitude = 0.6f;
		phase = 0.5f;
		dc = 0.0f;
		cycles = -1.0f;
		waveColor = PixelAudioMapper.composeColor(0, 89, 233);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		//
		frequency = 2 * funda;
		amplitude = 0.4f;
		phase = 0.75f;
		cycles = 2.0f;
		waveColor = PixelAudioMapper.composeColor(89, 199, 55);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		frequency = 4  * funda - detune;
		amplitude = 0.3f;
		phase = 0.125f;
		cycles = -3.0f;
		waveColor = PixelAudioMapper.composeColor(89, 110, 233);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		return list;
	}


	public static float[] chromaticScale(float funda) {
		float[] chromaScale = new float[12];
		for (int i = 0; i < chromaScale.length; i++) {
			chromaScale[i] = funda;
			funda *= (float)(funda * semitoneFac);
		}
		return chromaScale;
	}

	public static float pianoKeyFrequency(float keyNumber) {
		return (float) (440 * Math.pow(2, (keyNumber - 49) / 12.0));
	}

	public static float frequencyPianoKey(float freq) {
		return 49 + 12 * (float) (Math.log(freq / 440) / Math.log(2));
	}
	
	/**
	 * Moves an RGB color into the HSB color space and shifts it by a decimal fraction
	 * in the range (0.0,1.0) that represents a portion of the circle of hue values.
	 * For example, 1.0f/24 is 15 degrees around the 360-degree HSB color circle.
	 * 
	 * @param c			an RGB color 
	 * @param shift		the shift [0..1] of the hue in the HSB representation of color c
	 * @return			the RGB representation of the shifted color
	 */
	public static int colorShift(int c, float shift) {
		float[] hsb = new float[3];
		float h = PixelAudioMapper.hue(c, hsb);
		h = (h + shift);
		return Color.HSBtoRGB(h, hsb[1], hsb[2]);
	}
	
	
	/* ------------------------------------------------ */
	/*               WaveDataList Methods               */
	/* ------------------------------------------------ */
	
	/**
	 * Scales the amplitude of an ArrayList of WaveData objects.
	 * 
	 * @param waveDataList  an ArrayList of WaveData objects
	 * @param scale         the amount to scale the amplitude of each WaveData object
	 * @param isVerbose	    if true, output changes to data to the console
	 */
	public static void scaleAmps(ArrayList<WaveData> waveDataList, float scale, boolean isVerbose) {
		int i = 0;
		for (WaveData wd : waveDataList) {
			if (wd.isMuted) {
				i++;
				continue;
			}
			wd.setAmp(wd.amp * scale);
			if (isVerbose) System.out.println("----- set amplitude " + i + " to " + wd.amp);
		}
	}
	
	/**
	 * Shifts the colors of an ArrayList of WaveData objects.
	 * @param waveDataList	an ArrayList of WaveData objects
	 * @param shift		    the amount shift each color
	 * @param isVerbose	    if true, output changes to data to the console
	 */
	public static void shiftColors(ArrayList<WaveData> waveDataList, float shift, boolean isVerbose) {
		for (WaveData wd : waveDataList) {
			if (wd.isMuted)
				continue;
			wd.setWaveColor(WaveSynthBuilder.colorShift(wd.waveColor, shift));
		}
		if (isVerbose) System.out.println("----->>> shift colors " + shift);
	}
	
	/**
	 * Scales the frequencies of an ArrayList of WaveData objects.
	 * @param waveDataList	an ArrayList of WaveData objects
	 * @param scale		the amount to scale the frequency of each WaveData object
	 * @param isVerbose	   if true, output changes to data to the console
	 */
	public static void scaleFreqs(ArrayList<WaveData> waveDataList, float scale, boolean isVerbose) {
		int i = 0;
		for (WaveData wd : waveDataList) {
			if (wd.isMuted) {
				i++;
				continue;
			}
			wd.setFreq(wd.freq * scale);
			if (isVerbose) System.out.println("----- set frequency " + i + " to " + wd.freq);
		}
	}	
	
	/**
	 * Shifts the phase of an ArrayList of WaveData objects.
	 * @param waveDataList	an ArrayList of WaveData objects
	 * @param shift			amount to shift the phase of each WaveData object
	 * @param isVerbose	   if true, output changes to data to the console
	 */
	public static void shiftPhases(ArrayList<WaveData> waveDataList, float shift, boolean isVerbose) {
		for (WaveData wd : waveDataList) {
			if (wd.isMuted)
				continue;
			// wd.setPhase(wd.phase + shift - floor(wd.phase + shift));
			wd.setPhase(wd.phase + shift);
		}
		if (isVerbose) System.out.println("----->>> shiftPhase " + shift);
	}
	
	/**
	 * Outputs the phase values of a WaveSynth's array of WaveData objects to the console.
	 * @param wavesynth		a WaveSynth whose waveDataList's phase values will be output
	 */
	public static void showPhaseValues(WaveSynth wavesynth) {
		ArrayList<WaveData> waveDataList = wavesynth.getWaveDataList();
		int phaseStep = wavesynth.getStep();
		StringBuffer sb = new StringBuffer("\n----- current phase values scaled over (0, 1) -----\n");
		int i = 1;
		for (WaveData wd : waveDataList) {
			float m = wd.scaledPhaseAtFrame(phaseStep);
			sb.append(i++ +": "+ PApplet.nf(m) + "; ");
		}
		sb.append("\n----- current phase values scaled over (0, TWO_PI) -----\n");
		i = 1;
		for (WaveData wd : waveDataList) {
			float m = wd.phaseAtFrame(phaseStep);
			sb.append(i++ +": "+ PApplet.nf(m) + "; ");
		}
		System.out.println(sb);
	}
	
	/**
	 * Applies the current phase values to the initial values of the WaveSynth, so that
	 * the current state of the image display will appear as the first frame of 
	 * animation. Save the WaveSynth to a JSON file to keep the new phase values. 
	 * 
	 * @param wavesynth		an WaveSynth whose current WaveData phase values will be captured
	 */
	public static void capturePhaseValues(WaveSynth wavesynth) {
		ArrayList<WaveData> waveDataList = wavesynth.getWaveDataList();
		int phaseStep = wavesynth.getStep();
		for (WaveData wd : waveDataList) {
			float currentPhase = wd.scaledPhaseAtFrame(phaseStep);
			wd.setPhase(currentPhase);
		}
	}
	
	/**
	 * Mutes or unmutes a WaveData operator (view in the control panel).
	 * @param wavesynth		a WaveSynth
	 * @param elem			the index number of a WaveData object stored in a WaveSynth's waveDataList field
	 */
	public static void toggleWDMute(WaveSynth wavesynth, int elem) {
		if (wavesynth.waveDataList.size() < elem + 1) return;
		WaveData wd = wavesynth.waveDataList.get(elem);
		wd.isMuted = !wd.isMuted;
		if (wd.isMuted) {
			wd.waveState = WaveState.MUTE;
		}
		else {
			wd.waveState = WaveState.ACTIVE;
		}
	}
	
	public void muteWaveData(WaveSynth wavesynth, int elem) {
		if (wavesynth.waveDataList.size() < elem + 1) return;
		WaveData wd = wavesynth.waveDataList.get(elem);
		wd.waveState = WaveState.MUTE;
	}
	
	public void unmuteWaveData(WaveSynth wavesynth, int elem) {
		if (wavesynth.waveDataList.size() < elem + 1) return;
		WaveData wd = wavesynth.waveDataList.get(elem);
		wd.waveState = WaveState.ACTIVE;
	}
	
	
	/**
	 * Unmutes all the operators in supplied waveDataList.
	 * @param waveDataList	an ArrayList of WaveData objects
	 */
	public static void unmuteAllWD(ArrayList<WaveData> waveDataList) {
		StringBuffer sb = new StringBuffer("Audio operators\n");
		int n = 1;
		for (WaveData wd :waveDataList) {
			wd.setWaveState(WaveState.ACTIVE);
			sb.append("wave "+ (n++) +" "+ wd.waveState.toString() +", ");
		}
		System.out.println(sb.toString());		
	}

	/**
	 * Sets a specified WaveData element in the waveDataList of a supplied WaveSynth
	 * to WaveData.WaveState.SOLO if isSolo is true, otherwise sets it to WaveData.WaveState.ACTIVE.
	 * If isSolo is true, all other WaveData elements taht are ACTIVE will be set to SUSPENDED.
	 * Any elements that are currently muted will remain muted, any elements whose waveState 
	 * is SOLO will remain unchanged. Call unmuteAllWD() to set all elements to ACTIVE.
	 * TODO improve on the logic for WaveState changes. 
	 * 
	 * @param wavesynth		a WaveSynth whose waveDataList will be affected
	 * @param elem			index of an element in the waveDataList
	 * @param isSolo		if true, set WaveData element to WaveData.WaveState.SOLO
	 * 						otherwise, set it to WaveData.WaveState.ACTIVE
	 */
	public void toggleWDSolo(WaveSynth wavesynth, int elem, boolean isSolo) {
		if (wavesynth.waveDataList.size() < elem + 1)
			return;
		WaveData currentWD = wavesynth.waveDataList.get(elem);
		currentWD.waveState = isSolo ? WaveData.WaveState.SOLO : WaveData.WaveState.ACTIVE;
		if (currentWD.waveState == WaveData.WaveState.SOLO) {
			for (WaveData wd : wavesynth.waveDataList) {
				if (wd == currentWD)
					continue;
				if (wd.waveState == WaveData.WaveState.ACTIVE) {
					wd.waveState = WaveData.WaveState.SUSPENDED;
					wd.isMuted = true;
				}
			}
		} 
		else {
			boolean listHasSolos = false;
			for (WaveData wd : wavesynth.waveDataList) {
				if (wd.waveState == WaveData.WaveState.SOLO)
					listHasSolos = true;
			}
			if (!listHasSolos) {
				for (WaveData wd : wavesynth.waveDataList) {
					if (wd.waveState == WaveData.WaveState.SUSPENDED) {
						wd.waveState = WaveData.WaveState.ACTIVE;
						wd.isMuted = false;
					}
				}
			}
		}
	}

	/**
	 * Prints mute/active status of WaveData operators in supplied waveDataList. 
	 * @param waveDataList	an ArrayList of WaveData objects
	 */
	public static void printWDStates(ArrayList<WaveData> waveDataList) {
		StringBuffer sb = new StringBuffer("Audio operators\n");
		int n = 1;
		for (WaveData wd :waveDataList) {
			sb.append("wave "+ (n++) +" "+ wd.waveState.toString() +", ");
		}
		System.out.println(sb.toString());
	}
		

}
