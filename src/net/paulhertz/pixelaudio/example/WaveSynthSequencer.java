package net.paulhertz.pixelaudio.example;

import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.awt.Color;
import java.io.*;
import java.util.ArrayList;
import javax.sound.sampled.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import ddf.minim.*;
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.sampler.*;

/**
 * 
 * INSTRUCTIONS FOR PERFORMANCE
 * 
 * Launch and let the raindrops play. 
 * The tempo for performance is quarter note = 88 bpm, flexible for humans, inflexible for computer.
 * Percussion cues computer instrumentalist -- type '\' to start music, speak words. (Engineer: mic up)
 * At end, escape key closes app and WorkFlowPerformance is uncovered.
 * 
 * WaveSynthSequencer builds on BigWaveSynth, which shows how you can load
 * WaveSynth pixel data into the pixel array of a MultiGen. WaveSynthSequencer 
 * adds some useful features: 1. you can play the image as audio, and
 * 2. you can save the audio to a WAVE file.
 * 
 * This example also allows you to load JSON files to reconfigure the WaveSynth. 
 * Initially, we call buildWaveDataList() to create a WaveData array with eight operators. 
 * This is passed to a WaveSynth which is further configured by initWaveSynth(). 
 * To load JSON data, press the 'o' key and go to the data folder of this sketch.
 * 
 * Note that the sampleRate value influences the appearance of the image, the duration
 * of audio samples and the way data is written to an audio file. See the comments
 * in setup() for details. 
 * 
 * 
 * Press ' ' to start and stop WaveSynth animation.
 * Press 'o' to open a JSON WaveSynth configuration file.
 * Press 'O' to reload the most recent JSON WaveSynth configuration file.
 * Press 'j' or 'J' to save current WaveSynth configuration to a JSON file.
 * Press 's' to save the display to an image file named wavesynth_<wsIndex>.png.
 * Press 'S' to save WaveSynth audio to an audio file named wavesynth_<wsIndex>.wave.
 * Press 'f' to display the current frameRate. 
 * Press 'w' to step the WaveSynth Sequencer without playing a musical note.
 * Press 'W' to step the WaveSynth Sequencer and play the corresponding a musical note.
 * Press 'd' to turn raindrops on or off (raindrops trigger audio events). 
 * Press '\' to run the WaveSynth Sequencer 
 * Press 'c' to reset the image and sound to the opening state. 
 * Press 'h' to show this help message in the console. 
 * 
 * See also: BigWaveSynth, BigWaveSynthAudio.
 * 
 */
public class WaveSynthSequencer extends PApplet {
	PixelAudio pixelaudio;			// our shiny new library
	HilbertGen hGen;				// a PixelMapGen to draw Hilbert curves
	MultiGen multigen;				// a PixelMapGen that handles multiple gens
	int rows = 3;					// rows
	int columns = 2;				// columns
	int genWidth = 512;				// width of PixelMapGen objects, for hGen must be a power of 2
	int genHeight = 512;			// height of PixelMapGen objects, for hGen must be equal to width
	PixelAudioMapper mapper;		// instance of a class for reading, writing, and transcoding audio and image data 
	int mapSize;					// size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc. 
	ArrayList<WaveData> wdList;		// a list of audio "operators" that drive a WaveSynth
	WaveSynth wavesynth;			// a sort of additive audio synthesis color organ that also produces sound
	PImage synthImage;				// local variable that points to wavesynth's PImage instance variable

	// WaveSynth variables
	float myGamma = 1.0f;			// a non-linear brightness adjustment
	int animSteps = 240;			// number of steps in the animation
	int animStop = animSteps;		// The step at which the animation should stop (not used here)
	int step = 0;					// the current step in the animation
	String comments;				// a JSON field that provides information about the WaveSynth effects it produces
	
	/** Minim audio library */
	Minim minim;					// library that handles audio 
	AudioOutput audioOut;			// line out to sound hardware
	MultiChannelBuffer audioBuffer;	// data structure to hold audio samples
	boolean isBufferStale = true;	// flags that audioBuffer needs to be reset: i.e., after loading JSON data to wavesynth
	int sampleRate = 48000;			// a critical value for display and audio, see the setup method
	float[] audioSignal;			// the audio signal as an array of floats
	int[] rgbSignal;				// the colors in the display image, in the order the signal path visits them
	int audioLength;				// length of the audioSignal, same as the number of pixels in the display image

	// SampleInstrument setup
	int noteDuration = 1000;        // average sample synth note duration, milliseconds
	int samplelen;                  // calculated sample synth note length, samples
	PASamplerInstrumentPool pool;   // pool of instruments

	// ADSR and params
	ADSRParams adsr;                // good old attack, decay, sustain, release
	float maxAmplitude = 0.7f;
	float attackTime = 0.8f;
	float decayTime = 0.5f;
	float sustainLevel = 0.125f;
	float releaseTime = 0.5f;

	// file i/o from JSON
	String jsonFolder = "/JSON_data/";
	File currentDataFile;
	String currentFileName;
	JSONObject json;

	// animation
	boolean isWaveSynthAnimating = true;		// animation status
	boolean oldIsAnimating;			// keep old animation status if we suspend animation
	boolean isLooping = true;		// looping sample (our instrument ignores this)
	boolean isRaining = false;
	int circleColor = color(233, 220, 199, 128);

	// interaction
	int sampleX;
	int sampleY;
	int samplePos;            // position of a mouse click along the signal path, index into the audio array
	ArrayList<TimedLocation> timeLocsArray;
	int count = 0;	

	/****** WaveSynth Sequencer Settings ******/
	/** for frequency calculations, numbers are piano keys */
	public static final double semitoneFac = Math.pow(2, 1/12.0);
	int[] dbwfMusic = { 
		//9, 9, 9,  9,
		9, 14, 20,  8,    12, 19,  7, 13,    17, 12, 18, 11,    21, 16, 24, 15,
		26, 24, 19, 25,    24, 17, 11,  4,     9, 14, 20,  8,    12, 19,  7,  9, 
		 4,  9, 15,  8,    14, 19, 16, 23,    21, 26, 19, 12,    20, 16, 14, 10
	};
	/** for time calculations, 12 = one quarter note */
	ArrayList<WaveSynth> waveSynthList;
	int[] dbwfTimes = {
		//12, 12, 12, 12, 
		12, 12, 12, 12,    8, 4, 12, 24,     12, 12, 18, 6,   4, 4, 16, 24,
		12, 12, 12, 12,    8, 4, 12, 24,     12, 12, 18, 6,   8, 8,  8, 24,
		12, 12, 12, 12,    8, 4, 12, 24,     12, 12, 18, 6,   8, 4, 12, 24
	};
	/** amplitude values */
	float[] dbwfAmps = {
		//0.0f, 0.0f, 0.f, 0.0f,
		0.4f, 0.3f, 0.5f, 0.3f,    0.4f, 0.3f, 0.5f, 0.4f,     0.4f, 0.3f, 0.3f, 0.3f,   0.4f, 0.3f, 0.3f, 0.4f,
		0.4f, 0.3f, 0.5f, 0.3f,    0.4f, 0.3f, 0.3f, 0.5f,     0.4f, 0.3f, 0.5f, 0.3f,   0.4f, 0.3f,  0.3f, 0.5f,
		0.4f, 0.3f, 0.3f, 0.3f,    0.4f, 0.3f, 0.3f, 0.4f,     0.4f, 0.3f, 0.5f, 0.3f,   0.4f, 0.3f, 0.3f, 0.5f
	};
	// beatSpan is measured in milliseconds, where the entire buffer is 32768 ms, divided into 12 * 4 = 48 beats
	// Each beat has 12 subdivisions (ticks), so that dbwfTimes[0], which equals 12, is one beat long.
	// If we want to run the music in half time, we can set ticks to 6. 
	int ticks = 12;
	float beatSpan = 32768/(48.0f * ticks); 
	int wsIndex = 0;
	ArrayList<NoteTimedLocation> introMusic;
	boolean isPlayIntro = false;
	
	// number formatting, useful for output to console
	DecimalFormat twoPlaces;
	DecimalFormat noPlaces;
	DecimalFormat fourFrontPlaces;
	DecimalFormat commaTwoPlaces;
	DecimalFormat eightPlaces;

	
	public static void main(String[] args) {
		// PApplet.main(new String[] { "--display=1",  "--present", WaveSynthSequencer.class.getName() });
		PApplet.main(new String[] { WaveSynthSequencer.class.getName() });
	}

	public void settings() {
		size(rows * genWidth, columns * genHeight);
	}

	public void setup() {
		frameRate(30);
		pixelaudio = new PixelAudio(this);
		minim = new Minim(this);
		multigen = loadLoopGen(genWidth, genHeight);      // See the MultiGenDemo example for details on how MultiGen works
		mapper = new PixelAudioMapper(multigen);	// initialize a PixelAudioMapper
		float drone = sampleRate/1024.0f;			// a frequency that generates visual symmetries
		wdList = buildWaveDataList(drone, 8, 10);	// generate a WaveData list for the WaveSynth
		wavesynth = new WaveSynth(mapper, wdList);	// initialize a WaveSynth object
		initWaveSynth(wavesynth);					// fill in some parameters of the WaveSynth
		synthImage = wavesynth.mapImage;			// point synthImage at the WaveSynth's PImage field
		mapSize = mapper.getSize();					// size of the image, and of various other entities
		initAudio();								// set up audio output and an audio buffer
		timeLocsArray = new ArrayList<TimedLocation>();   // initialize mouse event tracking array
		initDecimalFormats();						// initializes some utility functions for formatting numbers
		initWaveSynthList();						// sets up a sequencer using dbwfMusic, dbwfTimes, and dbwfAmps arrays
		showHelp();
	}
	
	/**
	 * turn off audio processing when we exit
	 */
	public void stop() {
	  if (pool != null) pool.close();
	  if (minim != null) minim.stop();
	  super.stop();
	}

	/**
	 * Adds PixelMapGen objects to the local variable genList, puts the coords
	 * of their upper left corner in offsetList. The two lists are used to  
	 * initialize a MultiGen, which can be used to map audio and pixel data.
	 * This method provides a big looping fractal consisting of 6 Hilbert curves.
	 * This method was an early effort at creating continuous paths over multiple
	 * PixelMapGens. It is now available as a static method in the PixelAudio library,
	 * HilbertGen. hilbertLoop3x2(int genW, int genH).
	 */
	public MultiGen loadLoopGen(int loopGenW, int loopGenH) {
		// list of PixelMapGens that create an image using mapper
		ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
		// list of x,y coordinates for placing gens from genList
		ArrayList<int[]> offsetList = new ArrayList<int[]>(); 		
		genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.FX270));
		offsetList.add(new int[] { 0, 0 });
		genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.NADA));
		offsetList.add(new int[] { loopGenW, 0 });
		genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.FX90));
		offsetList.add(new int[] { 2 * loopGenW, 0 });
		genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.FX90));
		offsetList.add(new int[] { 2 * loopGenW, loopGenH });
		genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.R180));
		offsetList.add(new int[] { loopGenW, loopGenH });
		genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.FX270));
		offsetList.add(new int[] { 0, loopGenH });
		MultiGen gen = new MultiGen(width, height, offsetList, genList);
		return gen;
	}

	/**
	 * Initializes number formatters, handy for display. Processing's nf() method will also do. 
	 */
	public void initDecimalFormats() {
	  // DecimalFormat sets formatting conventions from the local system, unless we tell it otherwise.
	  // make sure we use "." for decimal separator, as in US, not a comma, as in many other countries
	  Locale loc = Locale.US;
	  DecimalFormatSymbols dfSymbols = new DecimalFormatSymbols(loc);
	  dfSymbols.setDecimalSeparator('.');
	  twoPlaces = new DecimalFormat("0.00", dfSymbols);
	  noPlaces = new DecimalFormat("00", dfSymbols);
	  fourFrontPlaces = new DecimalFormat("0000", dfSymbols);
	  eightPlaces = new DecimalFormat("0.00000000", dfSymbols);
	  dfSymbols.setDecimalSeparator(',');
	  commaTwoPlaces = new DecimalFormat("0.00", dfSymbols);
	}

	
	/**
	 * The main loop for drawing to the screen.
	 */
	public void draw() {
		image(synthImage, 0, 0, width, height);		// draw the synth image
		if (isWaveSynthAnimating) stepAnimation();	// animate the image if requested
		runTimeArray();								// animate audio event markers
		if (isPlayIntro && introMusic.size() > 0) {
			runMusicArray();						// play the WaveSynth Sequencer
		}
		if (isRaining) {
			// animation slows the frame rate, so we change the threshold when animating
		    float thresh = (isWaveSynthAnimating) ? 0.25f : 0.05f;
		    if (random(0,1) < thresh) {
		      raindrops();							// trigger random audio events
		    }
		}
	}

	/**
	 * Animates the WaveSynth referenced by variable wavesynth.
	 * The animation is controlled by the WaveSynth phase and cycle attributes.
	 */
	public void stepAnimation() {
		step += 1;
		step %= animSteps;
		wavesynth.renderFrame(step);
	}

	public void keyPressed() {
		switch (key) {
		case ' ':
			isWaveSynthAnimating = !isWaveSynthAnimating;
			println(isWaveSynthAnimating ? "Starting animation at frame " + step + " of " + animSteps
					: "Stopping animation at frame " + step + " of " + animSteps);
			break;
		case 'o':
		      // turn off animation while reading new settings for wavesynth
		      oldIsAnimating = isWaveSynthAnimating;
		      isWaveSynthAnimating = false;
		      this.loadWaveData();
		      isBufferStale = true;
		      break;
		case 'O':
			if (currentDataFile == null) {
				loadWaveData();
			} 
			else {
				fileSelectedOpen(currentDataFile);
				renderSignal();
				println("-------->>>>> Reloaded file");
			}
			break;
		case 'j':
		case 'J':
			saveWaveData();
			break;
		case 's': 
			synthImage.save("wavesynth_"+ wsIndex +".png");
			break;
		case 'S': 
			saveToAudio();
			break;
		case 'f':
			println("--->> frame rate: "+ frameRate);
			break;
		case 'w':
			stepWaveSynth();
			break;
		case 'W':
			stepWaveSynth();
			playSample(mapper.lookupSignalPos(width/2, height/2), calcSampleLen(), 0.3f, new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime));
			break;
		case 'd':
			isRaining = !isRaining;
			println("----->>> isRaining ", isRaining);
			break;
		case '\\':
			isRaining = false;
			loadMusic();
			isPlayIntro = true;
			break;
		case 'c':
			wavesynth = new WaveSynth(mapper, wdList);
			initWaveSynth(wavesynth);
			wavesynth.prepareAnimation();
			wavesynth.renderFrame(0);
			this.synthImage = wavesynth.mapImage;
			checkBufferState(true);
			isWaveSynthAnimating = true;
			break;
		case 'h':
			showHelp();
			break;
		default:
			break;
		}
	}
	
	
	/**
	 * Advance the WaveSynth Sequencer to its next state. 
	 */
	public void stepWaveSynth() {
		wavesynth = this.waveSynthList.get(wsIndex);
		wavesynth.prepareAnimation();
		wavesynth.renderFrame(0);
		synthImage = wavesynth.mapImage;	// point synthImage at the WaveSynth's PImage field
		wsIndex++;
		if (wsIndex > waveSynthList.size() - 1) wsIndex = 0;
		checkBufferState(true);
	}
	
	public void showHelp() {
		println(" * Press ' ' to start and stop WaveSynth animation.");
		println(" * Press 'o' to open a JSON WaveSynth configuration file.");
		println(" * Press 'O' to reload the most recent JSON WaveSynth configuration file.");
		println(" * Press 'j' or 'J' to save current WaveSynth configuration to a JSON file.");
		println(" * Press 's' to save the display to an image file named wavesynth_<wsIndex>.png."); 
		println(" * Press 'S' to save WaveSynth audio to an audio file named wavesynth_<wsIndex>.wav."); 
		println(" * Press 'f' to display the current frameRate. ");
		println(" * Press 'w' to step the WaveSynth Sequencer without playing a musical note.");
		println(" * Press 'W' to step the WaveSynth Sequencer and play the corresponding a musical note.");
		println(" * Press 'd' to turn raindrops on or off (raindrops trigger audio events). ");
		println(" * Press '\\' to run the WaveSynth Sequencer ");
		println(" * Press 'c' to reset the image and sound to the opening state. ");
		println(" * Press 'h' to show this help message in the console. ");
	}

	/**
	 * Save audio buffer to a file called "wavesynth_<wsIndex>.wav".
	 */
	public void saveToAudio() {
		renderSignal();
		try {
			saveAudioToFile(audioSignal, sampleRate, "wavesynth_"+ wsIndex +".wav");
		}
		catch (IOException e) {
			println("--->> There was an error outputting the audio file wavesynth.wav "+ e.getMessage());
		}
		catch (UnsupportedAudioFileException e) {
			println("--->> The file format is unsupported "+ e.getMessage());
		}
	}

	public void mousePressed() {
		audioMousePressed(constrain(mouseX, 0, width - 1), constrain(mouseY, 0, height - 1));
	}
		
	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                     BEGIN AUDIO METHODS                        */
	/*                                                                */
	/*----------------------------------------------------------------*/

	/**
	 * CALL IN SETUP()
	 */
	public void initAudio() {
	  minim = new Minim(this);
	  // use the getLineOut method of the Minim object to get an AudioOutput object
	  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
	  // set up a audioBuffer
	  this.audioBuffer = new MultiChannelBuffer(mapSize, 1);
	  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
	  adsr = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
	  // create a pool of eight monophonic instruments
	  pool = new PASamplerInstrumentPool(audioBuffer, sampleRate, 8, 1, audioOut, adsr);
	  // initialize mouse event tracking array
	  timeLocsArray = new ArrayList<TimedLocation>();     
	}

	// Called by mousePressed(), this should be a bottleneck method for all playSample() calls.
	public void audioMousePressed(int sampleX, int sampleY) {
	  this.sampleX = sampleX;
	  this.sampleY = sampleY;
	  samplePos = mapper.lookupSignalPos(sampleX, sampleY);
	  checkBufferState(isBufferStale);
	  playSample(samplePos, calcSampleLen(), 0.6f, adsr);
	}

	/**
	 * Calls WaveSynth to render a audio sample array derived from the same math that creates the image,
	 * then loads the derived audio data to audioBuffer, ready to be played.
	 */
	public void renderSignal() {
	  this.audioSignal = wavesynth.renderAudioRaw(step);           // get the signal "as is" from WaveSynth
	  audioSignal = WaveSynth.normalize(audioSignal, 0.9f);        // normalize samples to the range (-0.9f, 0.9f) 
	  audioLength = audioSignal.length;
	  isBufferStale = true;
	  audioBuffer.setBufferSize(audioLength);
	  audioBuffer.setChannel(0, audioSignal);                      // copy audioSignal to channel 0 of audioBuffer
	  // println("--->> copied audio signal to audio buffer");
	}

	public void checkBufferState(boolean isStale) {
		if (isStale) {
			// println("--->> Stale buffer refreshed ");
			// any changes to image are equivalent changes to audio, so isBufferStale is set often
			renderSignal();
			pool.setBuffer(audioBuffer);
			isBufferStale = false;
		}
	}
	    
	/**
	 * Plays an audio sample with WFSamplerInstrument and custom ADSR.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param samplelen    length of the sample (will be adjusted)
	 * @param amplitude    amplitude of the sample on playback
	 * @param defaultEnv         an ADSR envelope for the sample
	 * @return the calculated sample length in samples
	 */
	public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env) {
	  samplelen = pool.playSample(samplePos, (int) samplelen, amplitude, env);
	  int durationMS = (int)(samplelen/sampleRate * 1000);
	  timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
	  // return the length of the sample
	  return samplelen;
	}

	/**
	 * Plays an audio sample with WFSamplerInstrument and default ADSR.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param samplelen    length of the sample (will be adjusted)
	 * @param amplitude    amplitude of the sample on playback
	 * @return the calculated sample length in samples
	 */
	public int playSample(int samplePos, int samplelen, float amplitude) {
	  samplelen = pool.playSample(samplePos, (int) samplelen, amplitude);
	  int durationMS = (int)(samplelen/sampleRate * 1000);
	  timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
	  // return the length of the sample
	  return samplelen;
	}

	public int calcSampleLen() {
	  float vary = 0; 
	  // skip the fairly rare negative numbers
	  while (vary <= 0) {
	    vary = (float) PixelAudio.gauss(1.0, 0.0625);
	  }
	  samplelen = (int)(abs((vary * this.noteDuration) * sampleRate / 1000.0f));
	  // println("---- calcSampleLen samplelen = "+ samplelen +" samples at "+ sampleRate +"Hz sample rate");
	  return samplelen;
	}

	/**
	 * Run the animation for audio events. 
	 */
	public void runTimeArray() {
	    int currentTime = millis();
	    timeLocsArray.forEach(tl -> {
	        tl.setStale(tl.stopTime() < currentTime);
	        if (!tl.isStale()) {
	            drawCircle(tl.getX(), tl.getY());
	        }
	    });
	    timeLocsArray.removeIf(TimedLocation::isStale);
	}

	/**
	 * Run the WaveSynth Sequencer.
	 */
	public void runMusicArray() {
	  int currentTime = millis();
	  introMusic.forEach(tl -> {
	    if (tl.stopTime() < currentTime) {
	      isWaveSynthAnimating = false;
	      wavesynth = tl.getWaveSynth();
	      wavesynth.prepareAnimation();
	      wavesynth.renderFrame(0);
	      synthImage = wavesynth.mapImage;
	      renderSignal();
	      checkBufferState(isBufferStale);
	      int len = (int) ((48 * tl.duration) * 0.4f);
	      sampleX = tl.getX();
	      sampleY = tl.getY();
	      circleColor = tl.getCircleColor();
	      // println("---> music ", tl.getX(), tl.getY(), mapper.lookupSample(tl.getX(), tl.getY()));
	      // println("---> music ", twoPlaces.format(tl.getAmplitude()));
	      playSample(mapper.lookupSignalPos(tl.getX(), tl.getY()), len, tl.getAmplitude(), tl.getAdsr());
	      tl.setStale(true);
	    }
	    else {
	      return;
	    }
	  });
	  introMusic.removeIf(NoteTimedLocation::isStale);
	}

	/**
	 * Trigger a WaveSynth sample at a random location.
	 */
	public void raindrops() {
	  int signalPos = (int) random(samplelen, mapSize - samplelen - 1);
	  int[] coords = mapper.lookupImageCoord(signalPos);
	  sampleX = coords[0];
	  sampleY = coords[1];
	  if (audioSignal == null || isBufferStale) {
	    renderSignal();
	    isBufferStale = false;
	  }
	  playSample(signalPos, samplelen, 0.15f, new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime));  
	}

	/**
	 * Draws a circle at the location of an audio trigger (mouseDown event).
	 * @param x        x coordinate of circle
	 * @param y        y coordinate of circle
	 */
	public void drawCircle(int x, int y) {
	    //float size = isRaining? random(10, 30) : 60;
	    fill(color(233, 220, 199));
	    noStroke();
	    circle(x, y, 60);
	}    
	
	// ------------------------------------------- //
	// 	        	FREQUENCY CALCULATIONS		   //
	// ------------------------------------------- //

	/**
	 * Generates an ArrayList of WaveData objects to be used by a WaveSynth to
	 * generate RGB pixel values and (on request) audio signal values.
	 * A version of this code is included in the WaveSynthBuilder class in the PixelAudio library, 
	 * wrapped in a static method called synthTrumpet(float fundamental, int howManyPartials, float pianoKey, int animSteps). 
	 *
	 * @return an ArrayList of WaveData objects
	 */
	public ArrayList<WaveData> buildWaveDataList(float fundamental, int howManyPartials, float pianoKey) {
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
		int waveColor = color(0, 89, 233);
		waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
		int steps = this.animSteps;
		WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 1)
			return list;
		frequency = 2 * funda;
		amplitude = 0.52f;
		phase = -0.89f;
		cycles = 8.0f;
		waveColor = color(89, 199, 55);
		waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 2)
			return list;
		frequency = 3 * funda;
		amplitude = 0.6f;
		phase = -0.486f;
		cycles = 3.0f;
		waveColor = color(254, 89, 110);
		waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 3)
			return list;
		frequency = 4 * funda;
		amplitude = 0.45f;
		phase = -0.18616974f;
		cycles = -2.0f;
		waveColor = color(89, 110, 233);
		waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 4)
			return list;
		frequency = 5 * funda;
		amplitude = 0.42f;
		phase = 0.6846085f;
		cycles = -5.0f;
		waveColor = color(233, 34, 21);
		waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 5)
			return list;
		frequency = 6 * funda;
		amplitude = 0.45f;
		phase = 0.68912f;
		cycles = 13.0f;
		waveColor = color(220, 199, 55);
		waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 6)
			return list;
		frequency = 7 * funda;
		amplitude = 0.25f;
		phase = 0.68f;
		cycles = 11.0f;
		waveColor = color(159, 190, 255);
		waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 7)
			return list;
		frequency = 8 * funda;
		amplitude = 0.32f;
		phase = 0.68f;
		cycles = -11.0f;
		waveColor = color(209, 178, 117);
		waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		return list;
	}
	
	/**
	 * @param c			an RGB color 
	 * @param shift		the shift [0..1] of the hue in the HSB representation of color c
	 * @return			the RGB representation of the shifted color
	 */
	public int colorShift(int c, float shift) {
		float[] hsb = new float[3];
		float h = PixelAudioMapper.hue(c, hsb);
		h = (h + shift);
		return Color.HSBtoRGB(h, hsb[1], hsb[2]);
	}

	/**
	 * Sets gain, gamma, isScaleHisto, animSteps, and sampleRate instance variables 
	 * of a WaveSynth object and generates its first frame of animation.
	 * 
	 * @param synth		a WaveSynth object whose attributes will be set
	 * @return			the WaveSynth object with attributes set
	 */
	public WaveSynth initWaveSynth(WaveSynth synth) {
		synth.setGain(0.44f);
		synth.setGamma(myGamma);
		synth.setScaleHisto(false);
		synth.setAnimSteps(this.animSteps);
		synth.setSampleRate(sampleRate);
		// synth.setNoiseiness(0.5f);
		// println("--- mapImage size = " + synth.mapImage.pixels.length);
		synth.prepareAnimation();
		// synth.renderFrame(0);
		return synth;
	}

	/**
	 * @param keyNumber		key number on a piano, where A is key 49
	 * @return				frequency of the key (A = 440)
	 */
	public float pianoKeyFrequency(int keyNumber) {
		return (float) (440 * Math.pow(2, (keyNumber - 49) / 12.0));
	}
	
	/**
	 * @param funda 	the starting frequency
	 * @return			a chromatic scale starting with funda
	 */
	public float[] chromaticScale(float funda) {
		float[] chromaScale = new float[12];
		for (int i = 0; i < chromaScale.length; i++) {
			chromaScale[i] = funda;
			funda *= (float) semitoneFac;
		}
		return chromaScale;
	}
	
	/**
	 * Initializes a list of WaveSynth objects using pianoKey numbers in dbwfMusic array.
	 */
	public void initWaveSynthList() {
		this.waveSynthList = new ArrayList<WaveSynth>();
		for (int pianoKey : this.dbwfMusic) {
			float f = pianoKeyFrequency(pianoKey);
			WaveSynth ws = new WaveSynth(mapper, this.buildWaveDataList(f, 8, pianoKey));
			initWaveSynth(ws);
			waveSynthList.add(ws);			
		}
	}
	
	
	/**
	 * Initializes the introMusic ArrayList of NoteTimedLocation objects, data for a simple audio sequencer. 
	 * Uses the dbwfTimes array to set durations, dbwfMusic to set frequencies, and dbwfAmps to set amplitudes.
	 */
	public void loadMusic() {
		if (introMusic == null)
			introMusic = new ArrayList<NoteTimedLocation>();
		this.introMusic.clear();
		int startTime = millis() + 500;
		int i = 0;
		int stopSum = startTime;
		int circ = 0;
		int signalPos;
		int[] coords;
		int x;
		int y;
		float span;
		ADSRParams adsr = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
		for (int dur : this.dbwfTimes) {
			signalPos = (int) random(samplelen, mapSize - samplelen - 1);
			coords = mapper.lookupImageCoord(signalPos);
			x = coords[0];
			y = coords[1];
			circ = color(233, 220, 199, 128);
			adsr = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
			span = dur * this.beatSpan;
			int pianoKey = this.dbwfMusic[i];
			float f = 1.0f * pianoKeyFrequency(pianoKey);
			WaveSynth ws = new WaveSynth(mapper, this.buildWaveDataList(f, 8, pianoKey));
			initWaveSynth(ws);
			float amp = dbwfAmps[i];
			introMusic.add(new NoteTimedLocation(x, y, stopSum, i, pianoKey, span, amp * 1.8f, circ, adsr, ws));
			i++;
			stopSum += (int) (Math.round(span));
			println("--->> note event ", pianoKey, twoPlaces.format(f), twoPlaces.format(span), stopSum);
		}
	}	
	

	// ------------------------------------------- //
	// 	        	TIMED LOCATION CLASS		   //
	// ------------------------------------------- //
	
	public class TimedLocation {
		private int x;
		private int y;
		private int stopTime;
		private boolean isStale;

		public TimedLocation(int x, int y, int stop) {
			this.x = x;
			this.y = y;
			this.stopTime = stop;
			this.isStale = false;
		}

		public int getX() {
			return this.x;
		}

		public int getY() {
			return this.y;
		}

		public int stopTime() {
			return this.stopTime;
		}

		public boolean isStale() {
			return this.isStale;
		}

		public void setStale(boolean stale) {
			this.isStale = stale;
		}
	}
	
	
	public class NoteTimedLocation extends TimedLocation {
		private int pos;
		private int note;
		private float duration;
		private float amplitude;
		private ADSRParams adsr;
		private WaveSynth wavesynth;
		private int circleColor;

		public NoteTimedLocation(int x, int y, int stop, int pos, int note, float duration, float amplitude, int circ, ADSRParams adsr, WaveSynth ws) {
			super(x, y, stop);
			this.pos = pos;
			this.note = note;
			this.duration = duration;
			this.amplitude = amplitude;
			this.circleColor = circ;
			this.adsr = adsr;
			this.wavesynth = ws;
		}
		
		public int getNote() {
			return this.note;
		}
		
		public int getPos() {
			return this.pos;
		}
		
		public float getDuration() {
			return this.duration;
		}
		
		public float getAmplitude() {
			return this.amplitude;
		}
		
		public int getCircleColor() {
			return this.circleColor;
		}
		
		public ADSRParams getAdsr() {
			return this.adsr;
		}
		
		public WaveSynth getWaveSynth() {
			return this.wavesynth;
		}
		
	}
	
	
	//-------------------------------------------//
	//               JSON FILE I/O               //
	//-------------------------------------------//
	

	/**
	 * Show an Open File dialog for JSON files
	 */
	public void loadWaveData() {
		File folderToStartFrom = new File(dataPath("") + jsonFolder + "//*.json");
		selectInput("Select a file to open", "fileSelectedOpen", folderToStartFrom);
	}

	/**
	 * @param selection   a file selected from an Open File dialog
	 */
	public void fileSelectedOpen(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
			isWaveSynthAnimating = oldIsAnimating;
			return;
		}
		currentDataFile = selection;
		println("User selected " + selection.getAbsolutePath());
		currentFileName = selection.getAbsolutePath();
		json = loadJSONObject(currentFileName);
		setWaveSynthFromJSON(json, wavesynth);
		renderSignal(); 
		surface.setTitle(currentFileName);
		isWaveSynthAnimating = oldIsAnimating;
	}

	/**
	 * Parses a JSONObject to get values for a WaveSynth and load the values to the WaveSynth.
	 * 
	 * @param json		JSONObject data, probably from a file
	 * @param synth		a WaveSynth to modify with the JSONObject data values
	 */
	public void setWaveSynthFromJSON(JSONObject json, WaveSynth synth) {
		// set animation globals and WaveSynth properties
		animSteps = (json.isNull("steps")) ? 240 : json.getInt("steps");
		synth.setAnimSteps(animSteps);
		animStop = (json.isNull("stop")) ? this.animSteps : json.getInt("stop");
		synth.setStop(animStop);
		myGamma = (json.isNull("gamma")) ? 1.0f : json.getFloat("gamma");
		synth.setGamma(myGamma);
		comments = (json.isNull("comments")) ? "" : json.getString("comments");
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
			int c = color(rgbColor.getInt("r"), rgbColor.getInt("g"), rgbColor.getInt("b"));
			float cycles;
			cycles = waveElement.getFloat("cycles");
			// frequency, amplitude, phase, dc, cycles, color, steps
			WaveData wd = new WaveData(f, a, p, dc, cycles, c, animSteps);
			waveDataList.add(wd);
		}
		synth.setWaveDataList(waveDataList);
		synth.prepareAnimation();
		synth.renderFrame(0);
		printWaveData(synth);
	}

	/**
	 * Outputs fields from a WaveSynth to the console
	 */
	public void printWaveData(WaveSynth synth) {
		java.nio.file.Path path = java.nio.file.Paths.get(currentFileName);
		String fname = path.getFileName().toString();
		println("\n--------=====>>> Current WaveSynth instance for file " + fname + " <<<=====--------\n");
		println("Animation steps: " + synth.getAnimSteps());
		// println("Stop frame: "+ waveAnimal.getAnimSteps());
		println("gain: " + synth.getGain());
		println("gamma: " + synth.getGamma());
		if (synth.isScaleHisto()) {
			println("scaleHisto: " + synth.isScaleHisto());
			println("histoLow: " + synth.getHistoLow());
			println("histoHigh: " + synth.getHistoHigh());
		}
		println(fname);
		println("video filename: " + synth.getVideoFilename());
		// println("WaveData list for: "+ videoFilename);
		for (int i = 0; i < synth.waveDataList.size(); i++) {
			WaveData wd = synth.waveDataList.get(i);
			println("  " + (i + 1) + ":: " + wd.toString());
		}
		println("comments: " + synth.getComments() +"\n");
	}
	
	/**
	 * Show a Save File dialog
	 */
	public void saveWaveData() {
		selectOutput("Select a file to write to:", "fileSelectedWrite");
	}

	public void fileSelectedWrite(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
			return;
		}
		WaveSynth synth = this.wavesynth;
		println("User selected " + selection.getAbsolutePath());
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
		stateData.setInt("steps", synth.animSteps);
		stateData.setInt("stop", animStop);
		stateData.setFloat("blendFactor", synth.gain);
		stateData.setInt("dataFormat", 2);
		if (!selection.exists())
			stateData.setString("comments", "---");
		else
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
		println("----->>> video name is " + videoName);
		synth.videoFilename = videoName; // ???
		stateData.setString("filename", videoName);
		stateData.setFloat("gamma", synth.gamma);
		stateData.setBoolean("scaleHisto", synth.isScaleHisto);
		stateData.setFloat("histoHigh", synth.histoHigh);
		stateData.setFloat("histoLow", synth.histoLow);
		stateData.setJSONArray("waves", waveDataArray);
		saveJSONObject(stateData, currentFileName);
		currentDataFile = new File(currentFileName);
		surface.setTitle(currentFileName);
	}
	
	
	
	//-------------------------------------------//
	//              AUDIO FILE I/O               //
	//-------------------------------------------//
	

	
	/**
	 * Saves to a 32-bit floating point format that has higher resolution than 16-bit integer PCM. 
	 * The format can't be opened by Processing but can be opened by audio applications. 
	 * 
	 * @param samples			an array of floats in the audio range (-1.0f, 1.0f)
	 * @param sampleRate		the sample rate for the file
	 * @param fileName			name of the file
	 * @throws IOException		an Exception you'll need to catch to call this method (see keyPressed entry for 's')
	 */
	public static void saveAudioTo32BitPCMFile(float[] samples, float sampleRate, String fileName) throws IOException {
		// convert samples to 32-bit PCM float
		byte[] audioBytes = new byte[samples.length * 4];
		int index = 0;
		// convert to IEEE 754 floating-point "single format" bit layout 
		for (float sample : samples) {
			int intBits = Float.floatToIntBits(sample);
			audioBytes[index++] = (byte) (intBits & 0xFF);
			audioBytes[index++] = (byte) ((intBits >> 8) & 0xFF);
			audioBytes[index++] = (byte) ((intBits >> 16) & 0xFF);
			audioBytes[index++] = (byte) ((intBits >> 24) & 0xFF);
		}
		ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
		AudioFormat format = new AudioFormat(sampleRate, 32, 1, true, false);
        AudioInputStream audioInputStream = new AudioInputStream(byteStream, format, samples.length);
        File outFile = new File(fileName);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outFile);      
	}


	/**
	 * Saves audio data to 16-bit integer PCM format, which Processing can also open.
	 * 
	 * @param samples			an array of floats in the audio range (-1.0f, 1.0f)
	 * @param sampleRate		audio sample rate for the file
	 * @param fileName			name of the file to save to
	 * @throws IOException		an Exception you'll need to catch to call this method (see keyPressed entry for 's')
	 * @throws UnsupportedAudioFileException		another Exception (see keyPressed entry for 's')
	 */
	public static void saveAudioToFile(float[] samples, float sampleRate, String fileName)
	        throws IOException, UnsupportedAudioFileException {
	    // Convert samples from float to 16-bit PCM
	    byte[] audioBytes = new byte[samples.length * 2];
	    int index = 0;
	    for (float sample : samples) {
	        // Scale sample to 16-bit signed integer
	        int intSample = (int) (sample * 32767);
	        // Convert to bytes
	        audioBytes[index++] = (byte) (intSample & 0xFF);
	        audioBytes[index++] = (byte) ((intSample >> 8) & 0xFF);
	    }
	    // Create an AudioInputStream
	    ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
	    AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
	    AudioInputStream audioInputStream = new AudioInputStream(byteStream, format, samples.length);
	    // Save the AudioInputStream to a WAV file
	    File outFile = new File(fileName);
	    AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outFile);
	}
	
	
}
