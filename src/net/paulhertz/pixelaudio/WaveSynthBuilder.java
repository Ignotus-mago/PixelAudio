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

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;
import net.paulhertz.pixelaudio.WaveData.WaveState;

/**
 * Utilities for generating WaveSynth "instruments" and working with WaveSynth data. 
 */
public class WaveSynthBuilder {
	public static final double semitoneFac = Math.pow(2, 1 / 12.0);
	public static int animSteps = 720;

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
	
	public static void muteWaveData(WaveSynth wavesynth, int elem) {
		if (wavesynth.waveDataList.size() < elem + 1) return;
		WaveData wd = wavesynth.waveDataList.get(elem);
		wd.waveState = WaveState.MUTE;
	}
	
	public static void unmuteWaveData(WaveSynth wavesynth, int elem) {
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
	public static void toggleWDSolo(WaveSynth wavesynth, int elem, boolean isSolo) {
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
	
	
	// static methods for JSON IO

	//-----------------------------------------------------------//
	/* ----->>>           BEGIN JSON FILE I/O           <<<----- */
	//-----------------------------------------------------------//
	
/*
	// select a file of WaveData objects in JSON format to open
	public void loadWaveData() {
		File folderToStartFrom = new File(dataPath("") + jsonFolder + "//*.json");
		selectInput("Select a file to open", "fileSelectedOpen", folderToStartFrom);
	}
*/
	
	/**
	 * @param selection    a file containing JSON data
	 * @param json         a JSONObject that will be initialized with data from the file
	 * @param synth        a WaveSynth object whose fields will be filled out from the JSON data
	 * @param isStrict     if true, return false if no WaveSynth header is found in the JSON file
	 * @return             true if the File 
	 */
	public static boolean getJSONFromFile(File selection, JSONObject json, WaveSynth synth, boolean isStrict) {
		if (selection == null) {
			System.out.println("Window was closed or the user hit cancel.");
			return false;
		}
		// println("User selected " + selection.getAbsolutePath());
		json = PApplet.loadJSONObject(selection);
		boolean goodHeader = checkJSONHeader(json, "PXAU", "WSYN");
		if (goodHeader) {
			System.out.println("--->> JSON file contains WaveSynthEditor data. It should load correctly.");
		}
		else {
			if (isStrict) {
				System.out.println("--->> JSON file may not contain WaveSynthEditor data. WaveSynth was not initialized.");
				return false;
			}
			else {
				System.out.println("--->> JSON file may not contain WaveSynthEditor data. Will try to initialize WaveSynth,anyhow.");
			}
		}
		setWaveSynthFromJSON(json, synth);
		return true;
	}
	
	public static boolean getJSONFromFile(File selection, JSONObject json, WaveSynth synth) {
		return getJSONFromFile(selection, json, synth, false);
	}

	
	public static boolean checkJSONHeader(JSONObject json, String key, String val) {
		JSONObject header = (json.isNull("header") ? null : json.getJSONObject("header"));
		String pxau;
		if (header != null) {
			pxau = (header.isNull(key)) ? "" : header.getString(key);	
		}
		else {
			pxau = (json.isNull(key)) ? "" : json.getString(key);
		}
		if (pxau.equals(val)) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Sets the fields of a WaveSynth using values stored in a JSON object. 
	 * @param json		a JSON object, typically read in from a file
	 * @param synth		a WaveSynth
	 */
	public static void setWaveSynthFromJSON(JSONObject json, WaveSynth synth) {
		// set WaveSynth properties
		int animSteps = (json.isNull("steps")) ? 240 : json.getInt("steps");
		synth.setAnimSteps(animSteps);
		int animStop = (json.isNull("stop")) ? WaveSynthBuilder.animSteps : json.getInt("stop");
		synth.setStop(animStop);
		float myGamma = (json.isNull("gamma")) ? 1.0f : json.getFloat("gamma");
		synth.setGamma(myGamma);
		String comments = (json.isNull("comments")) ? "" : json.getString("comments");
		synth.setComments(comments);
		synth.setGain(json.isNull("blendFactor") ? 0.5f : json.getFloat("blendFactor"));
		synth.setVideoFilename((json.isNull("filename")) ? "wavesynth.mp4" : json.getString("filename"));
		synth.setScaleHisto((json.isNull("scaleHisto")) ? false : json.getBoolean("scaleHisto"));
		if (synth.isScaleHisto()) {
			synth.setHistoHigh((json.isNull("histoHigh")) ? 255 : json.getInt("histoHigh"));
			synth.setHistoLow((json.isNull("histoLow")) ? 0 : json.getInt("histoLow"));
		}
		// now load the JSON wavedata into ArrayList<WaveData> waveDataList
		JSONArray waveDataArray = json.getJSONArray("waves");
		int datalen = waveDataArray.size();
		ArrayList<WaveData> waveDataList = new ArrayList<WaveData>(datalen);
		for (int i = 0; i < datalen; i++) {
			// load fields common to both old and new format
			JSONObject waveElement = waveDataArray.getJSONObject(i);
			float f = waveElement.getFloat("freq");
			float a = waveElement.getFloat("amp");
			float p = waveElement.getFloat("phase");
			// float pInc = waveElement.getFloat("phaseInc");
			float dc = 0.0f;
			if (!waveElement.isNull("dc")) {
				dc = waveElement.getFloat("dc");
			}
			JSONObject rgbColor = waveElement.getJSONObject("color");
			int c = PixelAudioMapper.composeColor(rgbColor.getInt("r"), rgbColor.getInt("g"), rgbColor.getInt("b"));
			float cycles;
			cycles = waveElement.getFloat("cycles");
			// frequency, amplitude, phase, dc, cycles, color, steps
			WaveData wd = new WaveData(f, a, p, dc, cycles, c, animSteps);
			waveDataList.add(wd);
		}
		synth.setWaveDataList(waveDataList);
		synth.prepareAnimation();
		synth.renderFrame(0);
	}

	/**
	 * Returns supplied WaveSynth settings and WaveData list as a formated String.
	 */
	public static String waveSynthAsString(WaveSynth synth) {
		java.nio.file.Path path = java.nio.file.Paths.get(synth.getVideoFilename());
		StringBuffer sb = new StringBuffer();
		String fname = path.getFileName().toString();
		sb.append("\n--------=====>>> Current WaveSynth instance for file " + fname + " <<<=====--------\n");
		sb.append("Animation steps: " + synth.getAnimSteps());
		// println("Stop frame: "+ waveAnimal.getAnimSteps());
		sb.append("gain: " + synth.getGain());
		sb.append("gamma: " + synth.getGamma());
		if (synth.isScaleHisto()) {
			sb.append("scaleHisto: " + synth.isScaleHisto());
			sb.append("histoLow: " + synth.getHistoLow());
			sb.append("histoHigh: " + synth.getHistoHigh());
		}
		sb.append(fname);
		sb.append("video filename: " + synth.getVideoFilename());
		// println("WaveData list for: "+ videoFilename);
		for (int i = 0; i < synth.waveDataList.size(); i++) {
			WaveData wd = synth.waveDataList.get(i);
			sb.append("  " + (i + 1) + ":: " + wd.toString());
		}
		sb.append("comments: " + synth.getComments() +"\n");
		return sb.toString();
	}
	
	public static void saveWaveSynthJSON(PApplet app, File selection, WaveSynth synth) {
		if (selection == null) {
			System.out.println("Window was closed or the user hit cancel.");
			return;
		}
		System.out.println("User selected " + selection.getAbsolutePath());
		String currentFileName;
		// Do we have a .json at the end?
		if (selection.getName().length() < 5
				|| selection.getName().indexOf(".json") != selection.getName().length() - 5) {
			// problem missing ".json"
			currentFileName = selection.getAbsolutePath() + ".json"; // very rough approach...
		} else {
			currentFileName = selection.getAbsolutePath();
		}
		// put WaveData objects into an array
		JSONArray waveDataArray = new JSONArray();
		JSONObject waveElement;
		WaveData wd;
		for (int i = 0; i < synth.waveDataList.size(); i++) {
			wd = synth.waveDataList.get(i);
			waveElement = new JSONObject();
			waveElement.setInt("index", i);
			waveElement.setFloat("freq", wd.freq);
			waveElement.setFloat("amp", wd.amp);
			waveElement.setFloat("phase", wd.phase);
			waveElement.setFloat("phaseInc", wd.phaseInc);
			waveElement.setFloat("cycles", wd.phaseCycles);
			waveElement.setFloat("dc", wd.dc);
			// BADSR settings
			int[] rgb = PixelAudioMapper.rgbComponents(wd.waveColor);
			JSONObject rgbColor = new JSONObject();
			rgbColor.setInt("r", rgb[0]);
			rgbColor.setInt("g", rgb[1]);
			rgbColor.setInt("b", rgb[2]);
			waveElement.setJSONObject("color", rgbColor);
			// append wave data to array
			waveDataArray.append(waveElement);
		}
		// put the array into an object that tracks other state variables
		JSONObject stateData = new JSONObject();
		stateData.setJSONObject("header", getWaveSynthJSONHeader());
		// stateData.setString("PXAU", "WSYN");
		stateData.setInt("steps", synth.animSteps);
		stateData.setInt("stop", synth.getStop());
		stateData.setFloat("blendFactor", synth.gain);
		stateData.setInt("dataFormat", 2);
		stateData.setString("comments", synth.comments);
		// String videoName = selection.getName(); 
		String videoName = synth.videoFilename;
		if (videoName == null || videoName.equals("")) {
			videoName = selection.getName();
			if (videoName.indexOf(".json") != -1) {
				videoName = videoName.substring(0, videoName.indexOf(".json")) + ".mp4";
			} else {
				videoName += ".mp4";
			}
		}
		System.out.println("----->>> video name is " + videoName);
		synth.videoFilename = videoName; // ???
		stateData.setString("filename", videoName);
		stateData.setFloat("gamma", synth.gamma);
		stateData.setBoolean("scaleHisto", synth.isScaleHisto);
		stateData.setFloat("histoHigh", synth.histoHigh);
		stateData.setFloat("histoLow", synth.histoLow);
		stateData.setJSONArray("waves", waveDataArray);
		app.saveJSONObject(stateData, currentFileName);
	}
	
	public static JSONObject getWaveSynthJSONHeader() {
		// flag this JSON file as WaveSynthEditor data using a "PXAU" key with value "WSYN"
		// add some other pertinent information
		JSONObject header = new JSONObject();
		header.setString("PXAU", "WSYN");
		header.setString("description", "WaveSynthEditor data created with the PixelAudio library by Paul Hertz.");
		header.setString("PixelAudioURL", "https://github.com/Ignotus-mago/PixelAudio");
		return header;
	}

	//-------------------------------------------//
	//             END JSON FILE I/O             //
	//-------------------------------------------//

		

}
