package net.paulhertz.pixelaudio.example;

import java.util.ArrayList;
import processing.core.*;
import net.paulhertz.pixelaudio.*;

/**
 * SimpleWaveSynth demonstrates the basics of setting up a {@code WaveSynth} as an animated 
 * visual display. We'll work with WaveSynth audio in other sketches. 
 * <p>
 * A WaveSynth uses additive audio synthesis of sine wave "operators" to generate both an audio
 * signal array and an RGB "color signal" array. The color signal serves as the pixel array of a
 * PImage instance variable of the WaveSynth, {@code WaveSynth.mapImage}. In this sketch, the
 * dimensions of the image are the same as the display image, {@code this.mapImage}. 
 * </p><p>
 * The appearance of a WaveSynth is governed by the PixelMapGen we use and by the internal
 * sampling rate of the WaveSynth. By default, {@code WaveSynth.sampleRate = WaveSynth.mapSize},
 * the dimensions of the PImage the WaveSynth uses for its derived image. The sine wave
 * operators are stored as {@code WaveData} objects. Each WaveData object has fields for
 * frequency, amplitude, phase, number of cycles per animation sequence, etc. When the internal
 * sampling rate of the WaveSynth equals the width * height of its mapImage, this means that one
 * cycle of a 1.0 Hz sine wave will fill its signal array. Similarly, the color controlled by
 * that WaveData object in the color array will change its brightness over one cycle of a sine
 * wave. When a WaveSynth is animated, it sums the operator sine waves to create an audio signal
 * and it sums the operator colors to create a color array that it writes to its mapImage,
 * moving through the operator values frame by frame. For more details, see the WaveSynth code
 * and javadoc.
 * </p>
 * <pre>
 * Press '\t' (tab) to toggle animation.
 * Press '1' to set WaveSynth gamma to 1.0.
 * Press '2' to set WaveSynth gamma to 1.4.
 * Press '3' to set WaveSynth gamma to 1.8.
 * Press '4' to set WaveSynth gamma to 0.5.
 * Press 'g' to swap in a different PixelMapGen for the WaveSynth.
 * Press 'f' to set the WaveSynth to output an image rotated 90 degrees clockwise.
 * Press 'r' to step though different values for the internal sample rate of the WaveSynth.
 * Press 'R' to set the internal sample rate of the WaveSynth to mapSize.
 * Press 't' to output the gamma table for the current gamma value myGamma.
 * Press 'i' to output display frame rate to the console.
 * Press 'h' to show the help message in the console.
 * </pre>
 */
public class SimpleWaveSynth extends PApplet {
	PixelAudio pixelaudio;
	HilbertGen hGen;
	MooreGen mGen;
	DiagonalZigzagGen zGen;
	BoustropheGen bGenHz;
	PixelMapGen gen;
	PixelAudioMapper mapper;
	ArrayList<WaveData> wdList;
	WaveSynth wavesynth;
	int imageWidth = 1024;
	int imageHeight = 1024;
	PImage synthImage;
	float myGamma = 1.0f;
	int animSteps = 240;
	int step = 0;
	int start = 0;
	int timespan = 0;
	boolean isTrackTime = false;   // set to true to output the frame count every 24 frames
	boolean isAnimating = true;

	public static void main(String args[]) {
		PApplet.main(new String[] { SimpleWaveSynth.class.getName() });
	}

	public void settings() {
		size(imageWidth, imageHeight, JAVA2D);
	}

	public void setup() {
		frameRate(30);
		pixelaudio = new PixelAudio(this);
		hGen = new HilbertGen(1024, 1024);
		mGen = new MooreGen(1024, 1024);
		zGen = new DiagonalZigzagGen(1024, 1024, AffineTransformType.FLIPY);
		bGenHz = new BoustropheGen(height, width, PixelMapGen.r90);
		gen = hGen;
		mapper = new PixelAudioMapper(gen);
		wdList = initWaveDataList();
		wavesynth = new WaveSynth(mapper, wdList);
		initWaveSynth(wavesynth);
		synthImage = wavesynth.mapImage;
		showHelp();
	}

	/**
	 * Generates a list of WaveData for a WaveSynth.
	 * @return an ArrayList of WaveData
	 */
	public ArrayList<WaveData> initWaveDataList() {
		ArrayList<WaveData> list = new ArrayList<WaveData>();
		float frequency = 768.0f;
		float amplitude = 0.8f;
		float phase = 0.0f;
		float dc = 0.0f;
		float cycles = 1.0f;
		int waveColor = color(159, 190, 251);
		int steps = this.animSteps;
		WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		frequency = 192.0f;
		phase = 0.0f;
		cycles = 2.0f;
		waveColor = color(209, 178, 117);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		return list;
	}

	/**
	 * Sets some of a WaveSynth's instance variables, such gain, gamma, animation steps.
	 * We do not set the WaveSynth's sample rate, which defaults to the size of the WaveSynth's
	 * pixel map and audio signal array. 
	 * 
	 * @param synth   the WaveSynth to use
	 * @return the WaveSynth with instance variables set
	 */
	public WaveSynth initWaveSynth(WaveSynth synth) {
		synth.setGain(0.8f);
		synth.setGamma(myGamma);
		synth.setScaleHisto(false);
		synth.setAnimSteps(this.animSteps);
		println("--- synth mapImage size = " + synth.mapImage.pixels.length);
		println("--- synth sampleRate = " + synth.getSampleRate());
		synth.prepareAnimation();
		synth.renderFrame(0);
		return synth;
	}

	/**
	 * Set the PixelMapGen used by {@code htis.mapper}, changing the signal path in the display image.
	 * @param gen   PixelMapGen to provide to {@code mapper}
	 */
	public void swapGen(PixelMapGen gen) {
		mapper.setGenerator(gen);
		// if we had a new mapper, we would call wavesynth.setMapper(mapper) and reset
		// synthImage locally.
		// As it is, mapper only changed its variables, so the swap is really simple
	}

	public void draw() {
		image(synthImage, 0, 0);
		if (isTrackTime) {
			trackTime();
		}
		if (isAnimating) stepAnimation();
	}

	public void trackTime() {
		if (step == 0) {
			start = millis();
			println("----- timer started -----");
		}
		if (step % 24 == 0) {
			println("-- step " + step);
		}
		if (step == animSteps - 1) {
			timespan = (millis() - start);
			int xms = timespan % 1000;
			int secs = (timespan - xms) / 1000;
			int xsecs = secs % 60;
			int mins = secs / 60;
			println("--->> elapsed time: " + mins + ":" + xsecs + ":" + xms);
		}
	}
	
	public void stepAnimation() {
		step += 1;
		step %= animSteps;
		wavesynth.renderFrame(step);
	}

	public void keyPressed() {
		switch (key) {
		case '\t': // toggle animation
			isAnimating = !isAnimating;
			break;
		case '1': // set WaveSynth gamma to 1.0
			myGamma = 1.0f;
			wavesynth.setGamma(myGamma);
			println("----- WaveSynth.gamma is set to "+ wavesynth.getGamma());
			break;
		case '2': // set WaveSynth gamma to 1.4
			myGamma = 1.4f;
			wavesynth.setGamma(myGamma);
			println("----- WaveSynth.gamma is set to "+ wavesynth.getGamma());
			break;
		case '3': // set WaveSynth gamma to 1.8
			myGamma = 1.8f;
			wavesynth.setGamma(myGamma);
			println("----- WaveSynth.gamma is set to "+ wavesynth.getGamma());
			break;
		case '4': // set WaveSynth gamma to 0.5
			myGamma = 0.5f;
			wavesynth.setGamma(myGamma);
			println("----- WaveSynth.gamma is set to "+ wavesynth.getGamma());
			break;
		case 'g': // swap in a different PixelMapGen for the WaveSynth
			// change the gen
			if (gen == zGen) {
				gen = hGen;
				swapGen(gen);
				println("----- using Hilbert curve generator: " + gen.describe());
				break;
			}
			if (gen == hGen) {
				gen = mGen;
				swapGen(gen);
				println("----- using Moore curve generator: " + gen.describe());
				break;
			}
			if (gen == mGen) {
				gen = bGenHz;
				swapGen(gen);
				println("----- using BoustropheGen generator: " + gen.describe());
				break;
			}
			if (gen == bGenHz) {
				gen = zGen;
				swapGen(gen);
				println("----- using diagonal zigzag generator: " + gen.describe());
				break;
			}
		case 'f': // set the WaveSynth to output an image rotated 90 degrees clockwise
			// rotate gen 90 degrees clockwise
			gen.setTransformType(AffineTransformType.R270);
			swapGen(gen);
			println("----- Generator was rotated 90 degrees clockwise");
			break;
		case 'i': // output framerate info
			println(" ----- frame rate is "+ frameRate);
			break;
		case 'r': // set the internal sample rate of the WaveSynth to 44100
			float rate = wavesynth.getSampleRate();
			if (rate == wavesynth.getWidth() * wavesynth.getHeight()) {
				rate = rate / 4;
			}
			else if (rate == (wavesynth.getWidth() * wavesynth.getHeight()) / 4) {
				rate = 48000;
			}
			else if (rate == 48000) {
				rate = 44100;
			}
			else if (rate == 44100) {
				rate = wavesynth.getWidth() * wavesynth.getHeight();
			}
			wavesynth.setSampleRate(rate);
			println("----- WaveSynth sample rate is set to "+ wavesynth.getSampleRate());
			break;
		case 'R': // set the internal sample rate of the WaveSynth to mapSize
			wavesynth.setSampleRate(wavesynth.mapSize);
			println("----- WaveSynth sample rate is set to "+ wavesynth.getSampleRate());
			break;
		case 't': // output the gamma table for the current gamma value myGamma
			makeGammaTable(myGamma);
			break;
		case 'h': // show the help message in the console
			showHelp();
			break;
		default:
			break;
		}
	}

	public void showHelp() {
		println(" * Press '\t' (tab) to toggle animation.");
		println(" * Press '1' to set WaveSynth gamma to 1.0.");
		println(" * Press '2' to set WaveSynth gamma to 1.4.");
		println(" * Press '3' to set WaveSynth gamma to 1.8.");
		println(" * Press '4' to set WaveSynth gamma to 0.5.");
		println(" * Press 'g' to swap in a different PixelMapGen for the WaveSynth.");
		println(" * Press 'f' to set the WaveSynth to output an image rotated 90 degrees clockwise.");
		println(" * Press 'r' to step though different values for the internal sample rate of the WaveSynth.");
		println(" * Press 'R' to set the internal sample rate of the WaveSynth to mapSize.");
		println(" * Press 't' to output the gamma table for the current gamma value myGamma.");
		println(" * Press 'i' to output display frame rate to the console.");
		println(" * Press 'h' to show the help message in the console.");
	}

	/**
	 * Generates a gamma table, a lookup table for a non-linear adjustment of brightness.
	 * 
	 * @param gamma   the gamma value for generating the table
	 */
	public void makeGammaTable(float gamma) {
		int[] gammaTable = new int[256];
		println("----- GAMMA TABLE, gamma = " + gamma + " -----");
		println("-- index  - value  - RGB -");		
		for (int i = 0; i < gammaTable.length; i++) {
			float c = i / (float) (gammaTable.length - 1);
			c = (float) (Math.pow(c, gamma) * (gammaTable.length - 1));
			gammaTable[i] = (int) c;
			println("-- " + i + "  " + nf(c, 0, 4) + "  " + gammaTable[i]);
		}
	}

}
