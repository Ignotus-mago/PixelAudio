package net.paulhertz.pixelaudio;

import processing.core.PImage;
import processing.core.PConstants;
import java.util.ArrayList;

/**
 * Implements a combination of color organ and additive audio synth.
 * Note that animation by shifting phase does not necessarily affect the audio: we only hear phase relations when they shift. 
 */
public class WaveSynth {
	// WaveSynth objects
	public PixelAudioMapper mapper;
	public PImage mapImage;
	public int[] colorSignal;
	public float[] audioSignal;
	public ArrayList<WaveData>  waveDataList;
	private int w;
	private int h;
	public int mapSize;
	public int dataLength;
	
	// ------ WaveSynth control variables ----- //
	public float gain = 1.0f;
	public float gamma = 1.0f;
	public int[] gammaTable;
	public boolean useGammaTable = true;
	public boolean isScaleHisto = false;
	public int histoLow = 1;
	public int histoHigh = 254;
	public int animSteps = 720;
	public int step = 0;
	/** comments for JSON file */
	public String comments = "---";
	
	// ----- animation variables ----- //
	/** The sampling frequency, the number of samples read in one second of sound.
	 *  By default, for WaveSynth instances that are intended to be primarily visual,
	 *  mapSize is the sampling frequency. This makes one period of a 1.0 Hz wave fill 
	 *  the entire signal curve. OTOH, if we want the image to represent an audio signal
	 *  that is also produced by additive synthesis, we should set samplingFrequency to
	 *  a standard such as 41500 or 48000. 
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
	
	// ----- may be extraneous ----- //
	public int videoFramerate = 24;
	public String videoFilename = "motion_study.mp4";


	public WaveSynth(PixelAudioMapper mapper, ArrayList<WaveData> wdList) {
		this.setMapper(mapper);
		this.setWaveData(wdList);
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

	public int getVideoFramerate() {
		return videoFramerate;
	}

	public void setVideoFramerate(int videoFramerate) {
		this.videoFramerate = videoFramerate;
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
		myClone.setVideoFramerate(this.videoFramerate);
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
	
	
	// ------------- ANIMATION ------------- //
	
	// set up mapImage for editing, set mapInc
	public void prepareAnimation() {
		this.mapImage.loadPixels();
		this.colorSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
		this.mapInc = PConstants.TWO_PI / this.sampleRate;
	}
	
	// loop to render all the pixels in a frame
	public void renderFrame(int frame) {
		// load variables with prepareAnimation() at start of animation loop
		if (frame == 0)
			prepareAnimation();
		// loop through the image/signal, calculating a value for each pixel/sample
		for (int i = 0; i < this.mapSize; i++) {
			this.colorSignal[i] = this.renderPixel(frame, i);
		}
		// write scanSignal's pixel color values to scanImage pixels
		this.mapper.plantPixels(colorSignal, mapImage.pixels, 0, mapSize);
		// mapImage.pixels = this.colorSignal;
		this.mapImage.updatePixels();
		// set our internal step variable, just a tracker for now
		this.setStep(frame);
	}
	
	// render one pixel, return its RGB value
	public int renderPixel(int frame, int pos) {
		// pos += blockInc;
		// if (pos % 65536 == 0) println("----->>> pos = "+ pos +",
		// blockX, blockY "+ blockX, blockY );
		float freqShift = 1.0f;
		// float freqShift = isRandomFreqShift ? noiseAt(scanIndex % width, floor(scanIndex / width)) : 1;
		for (int j = 0; j < dataLength; j++) {
			WaveData wd = waveDataList.get(j);
			if (wd.isMuted || wd.waveState == WaveData.WaveState.SUSPENDED)
				continue;
			// ::::: sample amplitude = sin(initial phase + phase shift + frequency * i * (TWO_PI/n)) :::::
			// wd.phaseInc = (wd.cycles * TWO_PI)/animSteps; mapInc = TWO_PI / mapSize; 
			// Instead of incrementing phase at each step, we subtract (frame * phase increment)
			// from the initial phase. (instead of adding, we subtract so that animation data files 
			// give the same result in previous implementations. And yes, I have forgotten the original reasons for subtracting.)
			float val = (float) (Math.sin(wd.phaseTwoPi - frame * wd.phaseInc + wd.freq * freqShift * pos * mapInc) + woff)
					* wscale + wd.dc;
			weights[j] = val * wd.amp * this.gain;
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
