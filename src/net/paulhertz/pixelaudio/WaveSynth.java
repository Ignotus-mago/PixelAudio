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

import processing.core.PImage;
import processing.core.PConstants;
import java.util.ArrayList;


/**
 * Implements a combination of color organ and additive audio synth.
 * Animates pixels using phase shifting of audio generators in <code>waveDataList</code>. 
 * 
 * WaveSynth is organized around properties, such as gain (i.e. loudness or brightness) 
 * and gamma (a sort of contrast setting), and data objects. The data objects include a 
 * a bitmap, mapImage, that is a Processing PImage instance for the image representation
 * of the WaveSynth, a PixelAudioMapper that allows the WaveSynth to mediate between audio 
 * data and image data, arrays for the WaveSynth's audio signal and the image data ordered 
 * by the PixelAudioMapper signal path, and an array of WaveData objects, waveDataList,
 * that holds the individual sine wave components of the WaveSynth with their frequency,
 * amplitude, phase, and other properties. There are also a series of properties concerned 
 * with animation and video output. 
 * 
 * When a WaveSynth is used to produce color patterns, each WaveData object in the waveDataList
 * controls a color. The colors of the various WaveData objects are added together, much 
 * as the audio sine waves are to produce audio, with the brightness of each color determined
 * by the amplitude of the controlling sine wave. The WaveSynthAnimation example code 
 * provides a graphical user interface for editing the WaveSynth properties and the individual
 * WaveData objects. Experimenting with it to get an idea of what WaveSynth can do to 
 * produce patterns. 
 * 
 * 
 */
public class WaveSynth {
	// WaveSynth data objects
	public PixelAudioMapper mapper;
	public PImage mapImage;
	public int[] colorSignal;
	public float[] audioSignal;
	public float[] renderSignal;
	public ArrayList<WaveData>  waveDataList;

	// ------ Properties derived from data objects -------- //
	boolean isRenderAudio = false;
	private int w;
	private int h;
	public int mapSize;
	public int dataLength;

	// ------ WaveSynth control variable attributes ----- //
	public float gain = 1.0f;
	public float gamma = 1.0f;
	public int[] gammaTable;
	public boolean useGammaTable = true;
	public boolean isScaleHisto = false;
	public int histoLow = 1;
	public int histoHigh = 254;
	public int animSteps = 720;
	public int step = 0;
	public int stop = 0;
	public float noisiness = 0.0f;		// for future development
	/** comments for JSON file */
	public String comments = "---";
	
	// ----- animation variables ----- //
	/** 
	 *  The sampling frequency, the number of samples read in one second of sound.
	 *  By default, for WaveSynth instances that are intended to be primarily visual,
	 *  mapSize is the sampling frequency. This makes one period of a 1.0 Hz wave fill 
	 *  the entire signal curve. OTOH, if we want the image to represent an audio signal
	 *  that is also produced by additive synthesis, we should set samplingFrequency to
	 *  a standard such as 44100 or 48000. 
	 */
	public int sampleRate;
	/** the increment in phase over the image pixels, typically TWO_PI / image size */
	public float mapInc;
	/** array of amplitudes associated with the WaveData operators */
	public float[] weights;
	/** array of colors associated with the WaveData operators */
	public int[] waveColors;
	/** array of color values for mask, especially useful when it is constant */
	public int[] maskScan;
	/** offset for normailzing signal, see renderFrame method */
	public float woff = 1.0f;
	/** scaling facotr for normalizing signal, see renderFrame method */
	public float wscale = 0.5f;
	
	// ----- may be extraneous, but it appears in JSON data ----- //
	public int videoFrameRate = 24;
	public String videoFilename = "motion_study.mp4";


	public WaveSynth(PixelAudioMapper mapper, ArrayList<WaveData> wdList) {
		this.setMapper(mapper);
		this.setWaveData(wdList);
	}
	
	public WaveSynth(PixelAudioMapper mapper) {
		this(mapper, quickWaveDataList());
	}
	
	public void setMapper(PixelAudioMapper mapper) {
		this.mapper = mapper;
		this.w = mapper.getWidth();
		this.h = mapper.getHeight();
		this.mapSize = w * h;
		this.sampleRate = mapSize;
		this.mapImage = PixelAudio.myParent.createImage(w, h, PConstants.RGB);
		this.colorSignal = new int[mapSize];
		this.audioSignal = new float[mapSize];
	}
	
	public void setWaveData(ArrayList<WaveData> wdList) {
		this.waveDataList = wdList;
		this.dataLength = wdList.size();
		this.waveColors = new int[dataLength];
		for (int j = 0; j < dataLength; j++) {
			waveColors[j] = waveDataList.get(j).waveColor;
		}
		this.weights = new float[dataLength];
	}
	
	public void updateWaveColors() {
		this.dataLength = this.waveDataList.size();
		if (this.waveColors == null || this.waveColors.length != this.dataLength) {
			this.waveColors = new int[dataLength];
			this.weights = new float[dataLength];
		}
		for (int j = 0; j < dataLength; j++) {
			waveColors[j] = waveDataList.get(j).waveColor;
		}
	}
	
	/**
	 * Initializes a list of WaveData for use by a WaveSynth.
	 * 
	 * @return an ArrayList of WaveData objects
	 */
	public static ArrayList<WaveData> quickWaveDataList() {
		ArrayList<WaveData> list = new ArrayList<WaveData>();
		float frequency = 768.0f;
		float amplitude = 0.8f;
		float phase = 0.0f;
		float dc = 0.0f;
		float cycles = 1.0f;
		int waveColor = PixelAudioMapper.composeColor(159, 190, 251);
		int steps = 240;
		WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		frequency = 192.0f;
		phase = 0.0f;
		cycles = 2.0f;
		waveColor = PixelAudioMapper.composeColor(209, 178, 117);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		return list;
	}

	
	
	// ------------- GETTERS AND SETTERS ------------- //
	
	// ------------- Values for JSON and GUI ------------- //

	public ArrayList<WaveData> getWaveDataList() {
		return waveDataList;
	}

	public void setWaveDataList(ArrayList<WaveData> waveDataList) {
		this.setWaveData(waveDataList);
	}	

	public float getGain() {
		return gain;
	}

	public void setGain(float gain) {
		this.gain = gain;
	}

	public float getGamma() {
		return gamma;
	}

	public void setGamma(float gamma) {
		this.gamma = gamma;
		if (gamma != 1.0) {
			this.gammaTable = new int[256];
			for (int i = 0; i < gammaTable.length; i++) {
				float c = i/(float)(gammaTable.length - 1);
				gammaTable[i] = (int) Math.round(Math.pow(c, gamma) * (gammaTable.length - 1));
			}
		}
	}

	public boolean isScaleHisto() {
		return isScaleHisto;
	}

	public void setScaleHisto(boolean isScaleHisto) {
		this.isScaleHisto = isScaleHisto;
	}

	public int getHistoLow() {
		return histoLow;
	}

	public void setHistoLow(int histoLow) {
		this.histoLow = histoLow;
	}

	public int getHistoHigh() {
		return histoHigh;
	}

	public void setHistoHigh(int histoHigh) {
		this.histoHigh = histoHigh;
	}

	public float getNoiseiness() {
		return noisiness;
	}

	public void setNoiseiness(float noiseiness) {
		this.noisiness = noiseiness;
	}

	public int getAnimSteps() {
		return animSteps;
	}

	public void setAnimSteps(int animSteps) {
		this.animSteps = animSteps;
		if (this.waveDataList != null) {
			for (WaveData wd : this.waveDataList) {
				wd.phaseInc = (wd.phaseCycles * PConstants.TWO_PI) / this.animSteps;
			}
		}
	}

	public int getStop() {
		return stop;
	}

	public void setStop(int stop) {
		this.stop = stop;
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public int getVideoFrameRate() {
		return videoFrameRate;
	}

	public void setVideoFrameRate(int videoFrameRate) {
		this.videoFrameRate = videoFrameRate;
	}

	public String getVideoFilename() {
		return videoFilename;
	}

	public void setVideoFilename(String videoFilename) {
		this.videoFilename = videoFilename;
	}
	

	// ------------- MAPPER AND ASSOCIATED PROPERTIES ------------- //
	
	public PixelAudioMapper getMapper() {
		return mapper;
	}
	
	public int getWidth() {
		return this.w;
	}
	
	public int getHeight() {
		return this.h;
	}
	
	public int getSampleRate() {
		return this.sampleRate;
	}
	
	public void setSampleRate(int newSampleRate) {
		this.sampleRate = newSampleRate;
		this.mapInc = PConstants.TWO_PI / this.sampleRate;
	}
	

	public WaveSynth clone() {
		WaveSynth myClone = new WaveSynth(this.mapper, this.waveDataList);
		// instantiation sets scanner, scanType, depth, scanlen, scanSignal and
		// scanImage
		// set the "globals"
		myClone.setGain(this.gain);
		myClone.setGamma(this.gamma);
		myClone.setScaleHisto(this.isScaleHisto);
		myClone.setHistoLow(this.histoLow);
		myClone.setHistoHigh(this.histoHigh);
		myClone.setVideoFrameRate(this.videoFrameRate);
		myClone.setVideoFilename(this.videoFilename);
		myClone.setComments(this.comments);
		// set waveDataList
		myClone.setWaveDataList(WaveData.waveDataListCopy(this.waveDataList));
		// have to set this after we have a wave data list
		myClone.setAnimSteps(this.animSteps);
		myClone.setStep(this.step);
		myClone.setSampleRate(sampleRate);
		// println("----->>> CLONE \n" + this.toString() +"\n");
		return myClone;
	}
		

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Animation steps: " + this.getAnimSteps() + "\n");
		sb.append("blendFactor: " + this.getGain() + "\n");
		sb.append("gamma: " + this.getGamma() + "\n");
		if (this.isScaleHisto()) {
			sb.append("scaleHisto: " + this.isScaleHisto() + "\n");
			sb.append("histoLow: " + this.getHistoLow() + "\n");
			sb.append("histoHigh: " + this.getHistoHigh() + "\n");
		}
		sb.append("comments: " + this.getComments() + "\n");
		sb.append("video filename: " + this.getVideoFilename() + "\n");
		sb.append("sampling frequency: " + this.getSampleRate() + "\n");
		sb.append("WaveData list: ");
		for (int i = 0; i < this.waveDataList.size(); i++) {
			WaveData wd = this.waveDataList.get(i);
			sb.append("  " + (i + 1) + ":: " + wd.toString() + "\n");
		}
		return sb.toString();
	}
	
	
	// ------------- ANIMATION AND AUDIO ------------- //
	
	public boolean isRenderAudio() {
		return isRenderAudio;
	}

	public void setRenderAudio(boolean isRenderAudio) {
		if (isRenderAudio) {
			this.renderSignal = new float[audioSignal.length];
		}
		this.isRenderAudio = isRenderAudio;
	}

	// set up mapImage for editing, set mapInc
	public void prepareAnimation() {
		this.mapImage.loadPixels();
		this.colorSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
		this.mapInc = PConstants.TWO_PI / this.sampleRate;
		/*
		this.editModeWDList = new ArrayList<WaveData>();
		for (int j = 0; j < dataLength; j++) {
			WaveData wd = waveDataList.get(j);
			if (wd.isMuted || wd.waveState == WaveData.WaveState.SUSPENDED) {
				setEditMode(true);
				continue;
			}
			editModeWDList.add(wd);
		}
		*/
	}
	
	// loop to render all the pixels in a frame
	// We want it to complete a frame before any changes to the WaveSynth, so it's synchronized.
	public synchronized void renderFrame(int frame) {
		// load variables with prepareAnimation() at start of animation loop
		if (frame == 0) {
			prepareAnimation();
		}
		for (int i = 0; i < this.mapSize; i++) {
			this.colorSignal[i] = this.renderPixel(frame, i, this.waveDataList);
		}
		// write scanSignal's pixel color values to scanImage pixels
		this.mapper.plantPixels(colorSignal, mapImage.pixels, 0, mapSize);
		this.mapImage.updatePixels();
		if (isRenderAudio) {
			audioSignal = renderSignal;
		}
		// set our internal step variable, just a tracker for now
		this.setStep(frame);
	}
	
	/**
	 * 	Render one pixel, return its RGB value.
	 * 
	 *  NOTES
	 *  Our basic equation:
	 *  ::::: sample amplitude = sin(initial phase + phase shift + frequency * i * (TWO_PI/n)) :::::
	 *  Restated, in two parts:
	 *  wd.phaseInc = (wd.cycles * TWO_PI)/animSteps; mapInc = TWO_PI / mapSize;
	 *  float val = (float) (Math.sin(wd.phaseTwoPi - frame * wd.phaseInc + wd.freq * freqShift * pos * mapInc) + woff) * wscale + wd.dc;
	 *  Instead of incrementing phase at each step, we subtract (frame * phase increment)
	 *  from the initial phase: instead of adding, we subtract so that animation data files 
	 *  give the same result in previous implementations. And yes, I have forgotten the original reasons for subtracting.
	 *  For the latest version:
	 *  We now let the WaveData object calculate the signal: this is much more flexible and barely affects the time
	 *   
	 * @param frame
	 * @param pos
	 * @param wdList
	 * @return
	 */
	public int renderPixel(int frame, int pos, ArrayList<WaveData> wdList) {
		// noisiness is an experiment, left for future development
		// float freqShift = noisiness != 0.0f ? noiseAt(pos % mapper.width, (int) Math.floor(pos / mapper.width)) : 1;
	    // if (pos == 0) System.out.println("==> freqShift = "+ freqShift);
		for (int j = 0; j < dataLength; j++) {
			WaveData wd = waveDataList.get(j);
			// TODO this logic has a performance hit. Move it out of here.
			if (wd.isMuted || wd.waveState == WaveData.WaveState.SUSPENDED) continue;
			// float val = (wd.waveValue(frame, pos, freqShift, mapInc) + woff) * wscale + wd.dc;
			float val = (wd.waveValue(frame, pos, mapInc) + woff) * wscale + wd.dc;
			weights[j] = val * wd.amp * this.gain;
		}
		if (isRenderAudio) {
			float weightSum = 0.0f;
			for (int i = 0; i < weights.length; i++) {
				weightSum += weights[i];
			}
			this.renderSignal[pos] = weightSum;
		}
		return this.weightedColor(waveColors, weights);
	}
	
	public int weightedColor(int[] colors, float[] weights) {
		float r = 0, g = 0, b = 0;
		for (int i = 0; i < colors.length; i++) {
			int[] comp = PixelAudioMapper.rgbComponents(colors[i]);
			r += comp[0] * weights[i];
			g += comp[1] * weights[i];
			b += comp[2] * weights[i];
		}
		// gamma correction
		if (this.gamma != 1.0) {
			if (useGammaTable) {
				r = PixelAudio.constrain(r, 0, 255);
				r = gammaTable[(int)r];
				g = PixelAudio.constrain(g, 0, 255);
				g = gammaTable[(int)g];
				b = PixelAudio.constrain(b, 0, 255);
				b = gammaTable[(int)b];
			}
			else {
				r = (float) Math.pow((r / 255.0), this.gamma) * 255;
				g = (float) Math.pow((g / 255.0), this.gamma) * 255;
				b = (float) Math.pow((b / 255.0), this.gamma) * 255;
			}
		}
		// linear stretch, if you want it, adjust hi and lo to suit
		if (this.isScaleHisto) {
			r = PixelAudio.constrain(PixelAudio.map(r, this.histoLow, this.histoHigh, 1, 254), 0, 255);
			g = PixelAudio.constrain(PixelAudio.map(g, this.histoLow, this.histoHigh, 1, 254), 0, 255);
			b = PixelAudio.constrain(PixelAudio.map(b, this.histoLow, this.histoHigh, 1, 254), 0, 255);
		}
		return PixelAudioMapper.composeColor((int) r, (int) g, (int) b, 255);
	}	
	
	public float[] renderAudio(int frame) {
		return this.renderAudio(frame, 0.95f);
	}
	
	public float[] renderAudioRaw(int frame) {
		for (int pos = 0; pos < this.mapSize; pos++) {
			for (int j = 0; j < dataLength; j++) {
				WaveData wd = waveDataList.get(j);
				if (wd.isMuted || wd.waveState == WaveData.WaveState.SUSPENDED)
					continue;
				float val = (wd.waveValue(frame, pos, 1.0f, mapInc) + woff) * wscale + wd.dc;
				weights[j] = val * wd.amp * this.gain;
			}
			float weightSum = 0.0f;
			for (int i = 0; i < weights.length; i++) {
				weightSum += weights[i];
			}
			this.audioSignal[pos] = weightSum;
		}
		return audioSignal;
	}
	
	public float[] renderAudio(int frame, float limit) {
		for (int pos = 0; pos < this.mapSize; pos++) {
			for (int j = 0; j < dataLength; j++) {
				WaveData wd = waveDataList.get(j);
				if (wd.isMuted || wd.waveState == WaveData.WaveState.SUSPENDED)
					continue;
				float val = (wd.waveValue(frame, pos, 1.0f, mapInc) + woff) * wscale + wd.dc;
				weights[j] = val * wd.amp * this.gain;
			}
			float weightSum = 0.0f;
			for (int i = 0; i < weights.length; i++) {
				weightSum += weights[i];
			}
			this.audioSignal[pos] = weightSum;
		}
		return WaveSynth.normalize(audioSignal, limit);
	}
	
	public float noiseAt(int x, int y) {
		float scale = 0.001f;
		float detail = 0.4f;
		PixelAudio.myParent.noiseDetail(6, detail);
		float px = (x) * scale;
		float py = (y) * scale;
		float g = PixelAudio.myParent.noise(px, py); 
		g = PixelAudio.map(g, 0.0f, 1.0f, -0.5f, 0.5f) * noisiness;
		return 1.0f + g;
	}
	
	// ------------- STATIC METHODS ------------- //


	public static float[] normalize(float[] sig, float limit) {
		float min = 0;
		float max = 0;
		for (int i = 0; i < sig.length; i++) {
			if (sig[i] < min) min = sig[i];
			if (sig[i] > max) max = sig [i];
		}
		for (int i = 0; i < sig.length; i++) {
			sig[i] = PixelAudio.map(sig[i], min, max, -limit, limit);
		}
		return sig;
	}

	public static float[] normalize(float[] sig) {
		return normalize(sig, 1.0f);
	}

	public static int[] getHistoBounds(int[] source) {
	    int min = 255;
	    int max = 0;
	    for (int i = 0; i < source.length; i++) {
	      int[] comp = PixelAudioMapper.rgbComponents(source[i]);
	      for (int j = 0; j < comp.length; j++) {
	        if (comp[j] > max) max = comp[j];
	        if (comp[j] < min) min = comp[j];
	      }
	    }
	    return new int[]{min, max};
	}

	// histogram stretch -- run getHistoBounds to determine low and high
	public static int[] stretch(int[] source, int low, int high) {
	  int[] out = new int[source.length];
	  int r = 0, g = 0, b = 0;
	  for (int i = 0; i < out.length; i++) {
	    int[] comp = PixelAudioMapper.rgbComponents(source[i]);
	    r = comp[0];
	    g = comp[1];
	    b = comp[2];
	    r = (int) PixelAudio.constrain(PixelAudio.map(r, low, high, 0, 255), 0, 255);
	    g = (int) PixelAudio.constrain(PixelAudio.map(g, low, high, 0, 255), 0, 255);
	    b = (int) PixelAudio.constrain(PixelAudio.map(b, low, high, 0, 255), 0, 255);
	    out[i] = PixelAudioMapper.composeColor(r, g, b, 255);
	  }
	  return out;
	}

	
}
