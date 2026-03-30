package net.paulhertz.pixelaudio.example;

//Java imports
import java.util.ArrayList;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.util.Collections;

import processing.core.*;
import processing.data.*;
import processing.event.KeyEvent;
// Mama's ever-lovin' blue-eyed baby library
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.WaveData.WaveState;
import net.paulhertz.pixelaudio.sampler.*;
import net.paulhertz.pixelaudio.schedule.TimedLocation;
// audio library
import ddf.minim.*;

// video export library
import com.hamoid.*;

// GUI library for Processing
import g4p_controls.*;


/**
 * The PixelAudio demo application WaveSynthEditor makes hypnotic animated patterns 
 * that can be saved to video or played as an additive synthesis audio source. 
 * 
 * Use this application to edit a PixelAudio WaveSynth, including its individual WaveData
 * operators, using a nice GUI made with g4p_controls for Processing. This sketch shows
 * some of what you can do with the HilbertGen for making patterns with the WaveSynth. There
 * are lots of other possibilities. Patterns can be loaded from and saved to JSON files. 
 * 
 * For audio signals, a WaveSynth behaves like an audio synthesizer that adds together 
 * sine waves at different frequencies. The BigWaveSynthAudio and WaveSynthSequencer 
 * example sketches also produce audio with a WaveSynth. This example provides a graphical
 * user interface for editing the colors and other properties of a WaveSynth. 
 * 
 * Click on the WaveSynth image to hear what it sounds like. Note that the appearance of the
 * image is determined by the current sampling frequency, set in the initWaveSynth() method.
 * For a higher sampling rate, there are more samples. One sampling rate we use for the 
 * WaveSynth Editor is the number of pixels in the WaveSynth image, 1024 * 1024 = 1048576.
 * Though it may have more or less samples, the sound of the audio will not vary, as its 
 * frequency is governed by the sampling rate. If you want to save the audio to a file, 
 * you should probably set a standard sampling rate like 48000 in the initWaveSynth() method.
 * 
 * A WaveSynth is organized around attributes, such as gain (i.e. loudness or brightness) 
 * and gamma (a sort of contrast setting), and data objects. The data objects include a 
 * a bitmap, mapImage, that is a Processing PImage instance for the image representation
 * of the WaveSynth, a PixelAudioMapper that allows the WaveSynth to mediate between audio 
 * data and image data using arrays for the audio signal and the image data ordered along the 
 * PixelAudioMapper signal path, and an array of WaveData objects that define the individual
 * sine wave components of the WaveSynth. 
 * 
 * When a WaveSynth is used to produce color patterns, each WaveData object in the waveDataList
 * controls a color. The colors of the various WaveData objects are added together. The 
 * amplitude of the controlling sine wave controls the brightness of each color. The control
 * panel in this example allows to isolate individual WaveData operators to see how they 
 * affect the color patterns. 
 * 
 * SAMPLING RATES FOR AUDIO AND FOR WAVESYNTH IMAGES
 * 
 * We use different sampling rates for audio playback and recording and for WaveSynth's 
 * additive synthesis algorithm. We use the standard sampling rate 48000 Hz for audio
 * output. A 48000 Hz sampling rate fits conveniently with Hilbert curve dimensions. 
 * For the WaveSynth, which in this demo app relies on Hilbert curves to make patterns, 
 * we use (genWidth * genWidth) as the sampling rate for the sine waves that are added 
 * together to produce the WaveSynth image. If genWidth = 512, this value is 262144.
 * You can change the audio sampling rate in the instance variables list or in setup(). 
 * You can change WaveSynth's sampling rate in the initWaveSynth() method. 
 * 
 * 
 * In addition to the GUI commands, there are some useful key commands.
 *
 * ---------------------------------------------------------------------------------------------
 * ***>>  NOTE: Key commands only work when the image display window is the front window.  <<***
 * ---------------------------------------------------------------------------------------------
 * 
 * Key commands will not work when the control panel is the active window.
 * Click on the display window to make it the active window and then try the commands. 
 * See the parseKey() method and the methods it calls for more information about key commands.
 * 
 * The quickest way to record a video, from frame 0 to the stop frame value in the 
 * control panel, is to press the 'V' (capital 'v') key. 
 * 
 * The code in this example is extensively annotated. We the author heartily recommend you 
 * read the notes for the various methods. 
 * 
* Press the UP arrow to increase audio output gain by 3.0 dB.
 * Press the DOWN arrow to decrease audio output gain by 3.0 dB.
 * press ' ' to turn animation on or off.
 * Press 'a' to scale all active WaveSynth amplitudes by ampFac.
 * Press 'A' to scale all active WaveSynth amplitudes by 1/ampFac.
 * Press 'c' to shift all active WaveSynth colors by colorShift * 360 degrees in the HSB color space.
 * Press 'C' to shift all active WaveSynth colors by -colorShift * 360 degrees in the HSB color space.
 * Press 'd' to print animation data to the console.
 * Press 'D' to print WaveSynth data to the console.
 * Press 'f' to scale all active WaveSynth frequencies by freqFac.
 * Press 'F' to scale all active WaveSynth frequencies by 1/freqFac.
 * Press 'p' to shift all active WaveSynth phases by phaseFac.
 * Press 'P' to shift all active WaveSynth phases by -phaseFac.
 * Press 'k' to show all current phase values in the console.
 * Press 'K' to set all phase values so that first frame looks like the current frame, then go to first frame.
 * Press '+' or '=' to make the image brighter.
 * Press '-' or '_' to make the image darker.
// ------------- COMMANDS FOR ANIMATION STEPPING ------------- //
 * Press 'e' to fast forward animation 1/8 of total steps.
 * Press 'E' to rewind animation 1/8 of total steps (loops back from end, if required).
 * Press 'i' to reset current animation step to initial value, 0.
 * Press 'u' to advance animation by 1 step.
 * Press 'U' to advance animation by 10 steps.
 * Press 'y' to rewind animation by 1 step.
 * Press 'Y' to rewind animation by 10 steps.
 * Press 'l' or 'L' to toggle animation looping on or off.
// ------------- MUTING COMMANDS ------------- //
 * press keys 1-8 to mute or unmute first eight wave data operators
 * press 'm' to print current WaveData muting states to console.
 * press 'M' to unmute all current WaveData operators.
// ------------- JSON COMMANDS ------------- //
 * press 'j' or 'J' to save WaveSynth settings to a JSON file.
 * press 'o' to open a new JSON file.
 * press 'O' to reload the current JSON file, if there is one, reverting all edits.
// ------------- MISCELLANEOUS COMMANDS ------------- //
 * Press 'r' to toggles display window to fit screen or display at size.
 * Press 's' to save the current image to a .png file.
 * Press 'S' to save audio from WaveSynth.
 * Press 'v' to toggle video recording.
 * Press 'V' to record a complete video loop from frame 0 to stop frame.
 * Press 't' to sort wave data operators in control panel by frequency (lowest first), useful when saving to JSON.
 * Press 'z' to find nearest zero crossing in the audio signal and play from there.
 * press 'h' or 'H' to show this help message in the console. 
 *  
 * TODO can the audioBuffer be updated for all image and audio amplitude/brightness changes. How?
 * 
 */
public class WaveSynthEditor extends PApplet {
	int bgFillColor;
	/**
	 * Set designMode true to use designWidth and designHeight for display width
	 * and height. Set to false to use renderWidth and renderHeight for display
	 * width and height. Set renderWidth and renderHeight to the dimensions
	 * of the PixelMapGen you want render and animate.
	 */
	boolean isDesignMode = true;
	int genWidth = isDesignMode ? 1024 : 512;         // for Hilbert and Moore curves, genWidth must be a power of 2
	int genHeight = isDesignMode ? 1024 : 512;        // for Hilbert and Moore curves, genHeight must be a power of 2
	int designWidth = genWidth;
	int designHeight = genHeight;
	int renderWidth = 3 * genWidth;
	int renderHeight = 2 * genHeight;

	// display window sizes for resizing images to fit the screen,
	// most are calculated by the setScaling() method
	boolean isOversize = false;            // if false, image is not too big to display
	boolean isFitToScreen = false;         // is the image currently fit to the screen?
	int maxWindowWidth;                    // largest window width
	int maxWindowHeight;                   // largest window height
	int scaledWindowWidth;                 // scaled window width
	int scaledWindowHeight;                // scaled window height
	float windowScale = 1.0f;              // scaling ratio, used to calculate scaled mouse location

	// PixelAudio vars and objects
	PixelAudio pixelaudio;      // our shiny new library
	HilbertGen hGen;            // a PixelMapGen to draw Hilbert curves
	MooreGen mGen;              // a PixelMapGen to draw Moore curves
	DiagonalZigzagGen zGen;     // a PixelMapGen to draw zigzag curves
	MultiGen multigen;          // a PixelMapGen that handles multiple gens
	PixelMapGen gen;            // any PixelMapGen
	PixelAudioMapper mapper;    // object for reading, writing, and transcoding audio and image data
	int mapSize;                // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
	PImage mapImage;            // image for display
	PGraphics offscreen;        // offscreen PGraphics
	int[] rgbSignal;            // the pixel values in mapImage, in the order the signal path visits them

	// WaveSynth vars
	ArrayList<WaveData> wdList;    // list of WaveData objects used by a WaveSynth
	WaveSynth wavesynth;           // a WaveSynth to generate patterns from additive synthesis of sine waves
	ArrayList<WaveSynth> wsCrew;   // a list of WaveSynths
	WaveData currentWD;            // current WaveData object, for editing
	int waveDataIndex;             // index of currentWD in wavesynth.waveDataList

	// file IO for JSON and video output
	File currentDataFile;          // current JSON data file, if one is loaded
	String currentFileName;        // name of the current data file
	String videoFilename = "waveSynth_study.mp4";    // default video name, JSON file can set this
	String comments;               // comments in JSON file
	int snapCount = 13;            // number of snapshots to save while rendering video
	boolean    isCaptureFrames = false;    // set to true to capture snapshots which rendering video
	VideoExport videx;             // hamoid library class for video export (requires ffmpeg)
	JSONObject json;               // JSON data
	String dataPath;               // path to application data
	String videoPath;              // directory for saving video
	String jsonFile = "test327.json";
	String jsonFolder = "JSON";

	/* -------------------------- RENDER SETTINGS -------------------------- */
	/** Sets how much to shift phase with shiftPhases() */
	float phaseShift = (float) (Math.PI * 1.0 / 1536.0);
	/** Sets how much to shift colors with shiftColors()
	 * 1.0f/24 is 15 degrees around the 360-degree HSB color circle,
	 * expressed as a fraction in the range (0..1).
	 */
	float colorShift = 1.0f/24;
	/** Sets how much to scale frequencies with scaleFreqs() */
	float freqFac = (float) (Math.sqrt(2.0));
	/** Sets how much to scale amplitudes with scaleAmps() */
	float ampFac = 0.9375f; // 15/16
	/** Sets the increment to apply to wavesynth gain (brightness) */
	float gainInc = 0.03125f;

	boolean isAnimating = false;
	boolean isLooping = false;
	boolean oldIsAnimating;
	int animSteps = 720;                    // how many steps in an animation loop
	int animStop = animSteps;               // step where animation recording stops
	boolean isRecordingVideo = false;       // are we recording? (only if we are animating)
	int videoFrameRate = 24;                // fps
	int step;                               // number of current step in animation loop
	int startTime;                          // set when animation starts
	int stopTime;                           // used to calculate animation time until finish and duration

	/* ---------------------------------------------------------------------- */
	boolean isVerbose = true;               // if true, post lots of debugging messages to console
	boolean isSecondScreen = false;         // for a two screen display
	int screen2x;                           // second screen x-coord, will be set by setScaling()
	int screen2y;                           // second window y-coord, will be set by setScaling()

	/* ------------------------------------------------------------------ */
	/*                                                                    */
	/*                          AUDIO VARIABLES                           */
	/*                                                                    */
	/* ------------------------------------------------------------------ */
	/** Minim audio library */
	Minim minim;                       // library that handles audio
	AudioOutput audioOut;              // line out to sound hardware
	boolean isBufferStale = false;     // flags that audioBuffer needs to be reset: i.e., after loading JSON data to wavesynth
	float sampleRate = 48000;          // sample rate for audio playback and output to file, see notes above
	float[] audioSignal;               // the audio signal as an array of floats
	MultiChannelBuffer audioBuffer;    // data structure to hold audio samples
	int audioLength;                   // length of the audioSignal, same as the number of pixels in the display image
	float outputGain = -6.0f;          // audio output gain

	// SampleInstrument setup
	int noteDuration = 2000;        // average sample synth note duration, milliseconds
	int samplelen;                  // calculated sample synth note length, samples
	PASamplerInstrumentPool pool;   // pool of instruments

	// ADSR and params
	ADSRParams adsr;                   // good old attack, decay, sustain, release
	float maxAmplitude = 0.9f;
	float attackTime = 0.3f;
	float decayTime = 0.1f;
	float sustainLevel = 0.625f;
	float releaseTime = 0.5f;

	// interaction
	int sampleX;
	int sampleY;
	int samplePos;            // position of a mouse click along the signal path, index into the audio array
	ArrayList<TimedLocation> timeLocsArray;
	int count = 0;
	int wsIndex = 0;
	
	boolean isFindZeroCrossing = false;    // default setting for PASamplerVoice

	/* ---------------- end audio variables ---------------- */
	
	
	/**
	 * Used in Eclipse IDE and other Java environments to launch application. Delete in Processing.
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(new String[] {  WaveSynthEditor.class.getName() });
	}
	
	/**
	 * A call to the settings() method is required when setting size from variables
	 */
	public void settings() {
		if (isDesignMode) {
			size(designWidth, designHeight);
		}
		else {
			size(renderWidth, renderHeight);
		}
	}
	
	/**
	 * Prepare for launch by setting all variables required for the draw() loop.
	 * Initialize the PixelAudio library, then create a PixelMapGen and a PixelAudioMapper
	 * Next set up a WaveSynth and point our global mapImage to the WaveSynth's variable
	 * mapImage. Check for extra displays, decide where to show the application window, and
	 * set the window scaling variables. Finally, build the GUI (using G4P library).
	 */
	public void setup() {
		this.frameRate(24);
		bgFillColor = color(0, 0, 0, 255);
		pixelaudio = new PixelAudio(this);
		gen = isDesignMode ? createHilbertGen(genWidth) : hilbertLoop3x2(genWidth, genHeight);
		mapper = new PixelAudioMapper(gen);
		mapSize = mapper.getSize();                    // size of the image, and of various other entities
		wdList = initWaveDataList();
		wavesynth = new WaveSynth(mapper, wdList);
		initWaveSynth(wavesynth);
		initAudio();
		currentWD = wavesynth.waveDataList.get(0);
		mapImage = wavesynth.mapImage;
		listDisplays();
		setScaling();
		if (isOversize) {
			isFitToScreen = true;
			resizeWindow();
			println("Window is resized");
		}
		createGUI();
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
	 * @param edgeLength	length in pixels of Hilbert curve, must be a power of 2. 
	 * @return				a HilbertGen with its mapping arrays initialized
	 */
	public HilbertGen createHilbertGen(int edgeLength) {
		return new HilbertGen(edgeLength, edgeLength);		
	}
	
	/**
	 * Generates a looping fractal signal path consisting of 6 HilbertGens,
	 * arranged 3 wide and 2 tall, to fit a 3 * genW by 2 * genH image. 
	 * This particular MultiGen configuration is used so extensively in my 
	 * sample code that I've given it a factory method in the HilbertGen class.
	 * It's written out here so you can see how it works. 
	 * 
	 * Note that genH must equal genW and both must be powers of 2. For the 
	 * image size we're using in this example, genW = image width / 3 and 
	 * genH = image height / 2.
	 * 
	 * @param genW    width of each HilbertGen 
	 * @param genH    height of each HilbertGen
	 * @return	            a 3 x 2 array of Hilbert curves, connected in 
	 *                      a loop (3 * genWidth by 2 * genHeight pixels)
	 */
	public MultiGen hilbertLoop3x2(int genW, int genH) {
		// list of PixelMapGens that create an image using mapper
		ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
		// list of x,y coordinates for placing gens from genList
		ArrayList<int[]> offsetList = new ArrayList<int[]>();
		genList.add(new HilbertGen(genW, genH, PixelMapGen.fx270));
		offsetList.add(new int[] { 0, 0 });
		genList.add(new HilbertGen(genW, genH, PixelMapGen.nada));
		offsetList.add(new int[] { genW, 0 });
		genList.add(new HilbertGen(genW, genH, PixelMapGen.fx90));
		offsetList.add(new int[] { 2 * genW, 0 });
		genList.add(new HilbertGen(genW, genH, PixelMapGen.fx90));
		offsetList.add(new int[] { 2 * genW, genH });
		genList.add(new HilbertGen(genW, genH, PixelMapGen.r180));
		offsetList.add(new int[] { genW, genH });
		genList.add(new HilbertGen(genW, genH,PixelMapGen.fx270));
		offsetList.add(new int[] { 0, genH });
		return new MultiGen(3 * genW, 2 * genH, offsetList, genList);
	}
		
	/**
	 * @return an ArrayList of WaveData objects used to initialize a WaveSynth
	 */
	public ArrayList<WaveData> initWaveDataList() {
		ArrayList<WaveData> list = new ArrayList<WaveData>();
		float frequency = 192.0f;
		float amplitude = 0.8f;
		float phase = 0.0f;
		float dc = 0.0f;
		float cycles = 1.0f;
		int waveColor = color(159, 190, 251);
		int steps = this.animSteps;
		WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		frequency = 768.0f;
		phase = 0.0f;
		cycles = 2.0f;
		waveColor = color(209, 178, 117);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		return list;
	}

	/**
	 * Sets the initial values of a WaveSynth instance. Note particularly how varying
	 * sampleRate can change the appearance of the WaveSynth mapImage. SampleRate should 
	 * not change the sound of the audio signal -- only the image changes. Some sampling 
	 * rates are not standard for saving to file, so you may want to use a standard rate
	 * such as 48000Hz if you want to save audio to a file. 
	 * 
	 * @param synth		a WaveSynth instance
	 * @return			the WaveSynth with initial values set
	 */
	public WaveSynth initWaveSynth(WaveSynth synth) {
		synth.setGain(0.8f);
		synth.setGamma(1.0f);
		synth.setScaleHisto(false);
		synth.setAnimSteps(this.animSteps);
		synth.setSampleRate(genWidth * genWidth);
		// some other possible sampling rates
		// synth.setSampleRate(genWidth / 2 * genWidth / 2);
		// synth.setSampleRate(genWidth / 4 * genWidth / 4);
		// synth.setSampleRate(gen.getWidth() * gen.getHeight());
		// synth.setSampleRate(48000);
		println("\n====================================================");
		println("--- mapImage size = " + synth.mapImage.pixels.length);
		println("--- WaveSynth sample rate = " + synth.getSampleRate());
		println("====================================================\n");
		synth.prepareAnimation();
		synth.renderFrame(0);
		return synth;
	}	
	
	public void draw() {
		// draw the image 
		// mapImage points to wavesynth.mapImage, which gets updated by animation, etc.
		image(mapImage, 0, 0, width, height);
		// do one step of animation, if conditions are right
		if (isAnimating) {
			stepAnimation();
		}
		runTimeArray();		// animate audio event markers
	}
	
	/**
	 * Step through the animation, called by the draw() method.
	 * Will also record a frame of video, if we're recording.
	 */
	public void stepAnimation() {
		if (step >= animStop) {
			println("--- Completed video at frame " + animStop);
			if (!isLooping) {
				isAnimating = false;
			}
			step = 0;
			if (isRecordingVideo) {
				isRecordingVideo = false;
				videx.endMovie();
			}
		} 
		else {
			step += 1;
			if (isRecordingVideo) {
				if (videx == null) {
					println("----->>> start video recording ");
					videx = new VideoExport(this, videoFilename);
					videx.setFrameRate(wavesynth.videoFrameRate);
					videx.startMovie();
				}
				videx.saveFrame();
				println("-- video recording frame " + step + " of " + animStop);
			}
		}
		renderFrame(step);
	}
	
	public void renderFrame(int frame) {
		wavesynth.renderFrame(frame);
	}
	
	
	/********************************************************************/
	/* ----->>>             DISPLAY SCALING METHODS            <<<----- */
	/********************************************************************/

	
	/**
	 * Get a list of available displays and output information about them to the console.
	 * Sets screen2x, screen2y, displayWidth and displayHeight from dimensions of a second display.
	 */
	void listDisplays() {
		// Get the local graphics environment
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		// Get the array of graphics devices (one for each display)
		GraphicsDevice[] devices = ge.getScreenDevices();
		this.isSecondScreen = (devices.length > 1);
		println("Detected displays:");
		for (int i = 0; i < devices.length; i++) {
			GraphicsDevice device = devices[i];
			// Get the display's configuration
			GraphicsConfiguration config = device.getDefaultConfiguration();
			Rectangle bounds = config.getBounds(); // Screen dimensions and position
			println("Display " + (i + 1) + ":");
			println("  Dimensions: " + bounds.width + " x " + bounds.height);
			println("  Position: " + bounds.x + ", " + bounds.y);
			if (i == 1) {
				// second screen details
				this.screen2x = bounds.x + 8;
				this.screen2y = bounds.y + 8;
				this.displayWidth = bounds.width;
				this.displayHeight = bounds.height;
			}
		}
	}
		  
	/**
	 * Calculates window sizes for displaying mapImage at actual size and at full screen. 
	 * Press the 'r' key to resize the display window.
	 * This method will result in display on a second screen, if one is available. 
	 * If mapImage is smaller than the screen, mapImage is displayed at size on startup 
	 * and resizing zooms the image. 
	 * If mapImage is bigger than the display, mapImage is fit to the screen on startup
	 * and resizing shows it at full size, partially filling the window. 
	 * 
	 */
	public void setScaling() {
		// max window width is a little less than the screen width of the screen
		maxWindowWidth = displayWidth - 80;
		// leave window height some room for title bar, etc.
		maxWindowHeight = displayHeight - 80;
		float sc = maxWindowWidth / (float) mapImage.width;
		scaledWindowHeight = Math.round(mapImage.height * sc);
		if (scaledWindowHeight > maxWindowHeight) {
			sc = maxWindowHeight / (float) mapImage.height;
			scaledWindowHeight = Math.round(mapImage.height * sc);
			scaledWindowWidth = Math.round(mapImage.width * sc);
		} 
		else {
			scaledWindowWidth = Math.round(mapImage.width * sc);
		}
		// even width and height allow ffmpeg to save to video
		scaledWindowWidth += (scaledWindowWidth % 2 != 0) ? 1 : 0;
		scaledWindowHeight += (scaledWindowHeight % 2 != 0) ? 1 : 0;
		isOversize = (mapImage.width > scaledWindowWidth || mapImage.height > scaledWindowHeight);
		windowScale = (1.0f * mapImage.width) / scaledWindowWidth;
		println("maxWindowWidth " + maxWindowWidth + ", maxWindowHeight " + maxWindowHeight);
		println("image width " + mapImage.width + ", image height " + mapImage.height);
		println("scaled width " + scaledWindowWidth + ", scaled height " + scaledWindowHeight + ", "
				+ "oversize image is " + isOversize);
	}

	public void resizeWindow() {
		if (isFitToScreen) {
			surface.setSize(scaledWindowWidth, scaledWindowHeight);
		} 
		else {
			surface.setSize(mapImage.width, mapImage.height);
		}
	}

	// ------------- END DISPLAY SCALING METHODS ------------- //
	
	
	public void mousePressed() {
		// Demo of how to scale mousePressed events works when window is resized.
		int x = (this.isFitToScreen) ?  (int)(mouseX * windowScale) : mouseX;
		int y = (this.isFitToScreen) ?  (int)(mouseY * windowScale) : mouseY;
		audioMousePressed(constrain(x, 0, width - 1), constrain(y, 0, height - 1));
	}
	
	   /**
     * built-in keyPressed handler, forwards events to parseKey.
     */
    @Override
    public void keyPressed() {
    	if (key != CODED) {
    		parseKey(key, keyCode);
    	}
    	else {
    		float g = audioOut.getGain();
			if (keyCode == UP) {
				setAudioGain(g + 3.0f);
				println("---- audio gain is "+ nf(audioOut.getGain(), 0, 2));
			}
			else if (keyCode == DOWN) {
				setAudioGain(g - 3.0f);
				println("---- audio gain is "+ nf(audioOut.getGain(), 0, 2));
			}
			else if (keyCode == RIGHT) {
				println("-- frame rate = "+ frameRate);
			}
			else if (keyCode == LEFT) {

			}
    	}
    }

	/**
	 * ParseKey handles keyPressed events. Unlike keyPressed, it can be called by other methods.
	 * Note that keyPressed events only work when the image window, not the GUI control panel,
	 * is the active window. Click in the image display window to make it the active window.
	 * @param theKey    char value of the key that was pressed
	 */
	public void parseKey(char theKey, int keyCode) {
	  switch(theKey) {
	  case ' ': // turn animation on or off
	    toggleAnimation();
	    break;
	  case 'a': // scale all active WaveSynth amplitudes by ampFac
	    scaleAmps(wavesynth.getWaveDataList(), ampFac);
	    loadWaveDataPanelValues(currentWD);
	    isBufferStale = true;
	    break;
	  case 'A': // scale all active WaveSynth amplitudes by 1/ampFac
	    scaleAmps(wavesynth.getWaveDataList(), 1/ampFac);
	    loadWaveDataPanelValues(currentWD);
	    isBufferStale = true;
	    break;
	  case 'c': // shift all active WaveSynth colors by colorShift * 360 degrees in the HSB color space
	    shiftColors(wavesynth.getWaveDataList(), colorShift);
	    wavesynth.updateWaveColors();
	    refreshGlobalPanel();
	    break;
	  case 'C': // shift all active WaveSynth colors by -colorShift * 360 degrees in the HSB color space
	    shiftColors(wavesynth.getWaveDataList(), -colorShift);
	    wavesynth.updateWaveColors();
	    refreshGlobalPanel();
	    break;
	  case 'd': // print animation data to the console
	    println(isAnimating ? "-- running animation frame " + step + " of " + animStop : "-- stopped at frame " + step +" of " + animStop);
	    break;
	  case 'D': // print WaveSynth data to the console
	    println(wavesynth.toString());
	    break;
	  case 'f': // scale all active WaveSynth frequencies by freqFac
	    scaleFreqs(wavesynth.getWaveDataList(), freqFac);
	    loadWaveDataPanelValues(currentWD);
	    isBufferStale = true;
	    break;
	  case 'F': // scale all active WaveSynth frequencies by 1/freqFac
	    scaleFreqs(wavesynth.getWaveDataList(), 1/freqFac);
	    loadWaveDataPanelValues(currentWD);
	    isBufferStale = true;
	    break;
	  case 'p': // shift all active WaveSynth phases by phaseFac
	    shiftPhases(wavesynth.getWaveDataList(), phaseShift);
	    loadWaveDataPanelValues(currentWD);
	    isBufferStale = true;
	    break;
	  case 'P': // shift all active WaveSynth phases by -phaseFac
	    shiftPhases(wavesynth.getWaveDataList(), -phaseShift);
	    loadWaveDataPanelValues(currentWD);
	    isBufferStale = true;
	    break;
	  case 'k': // show all current phase values in the console
	    showPhaseValues(wavesynth.getWaveDataList());
	    break;
	  case 'K': // set all phase values so that first frame looks like the current frame, then go to first frame
	    capturePhaseValues(wavesynth.getWaveDataList());
	    step = 0;
	    renderFrame(step);
	    break;
	  case '+': // make the image brighter
	  case '=':
	    wavesynth.setGain(wavesynth.gain + gainInc);
	    refreshGlobalPanel();
	    if (!isAnimating) renderFrame(step);
	    break;
	  case '-': // make the image darker
	  case '_':
	    wavesynth.setGain(wavesynth.gain - gainInc);
	    refreshGlobalPanel();
	    if (!isAnimating) renderFrame(step);
	    break;
	    // ------------- BEGIN COMMANDS FOR ANIMATION STEPPING ------------- //
	  case 'e': // fast forward animation 1/8 of total steps
	    step = (step + animSteps/8) % animSteps;
	    renderFrame(step);
	    println("-- step = "+ step);
	    break;
	  case 'E': // rewind animation 1/8 of total steps (loops back from end, if required)
	    int leap = animSteps/8;
	    step = (step > leap) ? step - leap : animSteps - (leap - step);
	    renderFrame(step);
	    println("-- step = "+ step);
	    break;
	  case 'i': // reset current animation step to initial value, 0
	    step = 0;
	    renderFrame(0);
	    println("-- step = "+ step);
	    break;
	  case 'u': // advance animation by 1 step
	    step = (step + 1) % animSteps;
	    renderFrame(step);
	    println("-- step = "+ step);
	    break;
	  case 'U': // advance animation by 10 steps
	    step = (step + 10) % animSteps;
	    renderFrame(step);
	    println("-- step = "+ step);
	    break;
	  case 'y': // rewind animation by 1 step
	    step = (step > 0) ? (step - 1) : animSteps - 1;
	    renderFrame(step);
	    println("-- step = "+ step);
	    break;
	  case 'Y': // rewind animation by 10 steps
	    step = (step > 10) ? (step - 10) : animSteps - (10 - step);
	    renderFrame(step);
	    println("-- step = "+ step);
	    break;
	  case 'l': // toggle animation looping on or off
	  case 'L':
	    toggleLooping();
	    break;
	    // ------------- END COMMANDS FOR ANIMATION STEPPING ------------- //
	    // ------------- BEGIN MUTING COMMANDS ------------- //
	  case '1':
	  case '2':
	  case '3':
	  case '4':
	  case '5':
	  case '6':
	  case '7':
	  case '8':
	    // keys 1-8 mute or unmute WaveDataList elements 0-7
	    int k = Character.getNumericValue(theKey) - 1;
	    toggleWDMute(k);
	    refreshGlobalPanel();
	    isBufferStale = true;
	    break;
	  case 'm': // print current WaveData states to console
	    printWDStates(wavesynth.getWaveDataList());
	    break;
	  case 'M': // unmute all WaveData operators
		unmuteAllWD(wavesynth.getWaveDataList());
	    refreshGlobalPanel();
	    isBufferStale = true;
	    break;
	    // ------------- END MUTING COMMANDS ------------- //
	  case 'j': // save WaveSynth settings to a JSON file
	  case 'J':
	    saveWaveData();
	    break;
	  case 'o': // open a new JSON file
	    loadWaveData();
	    isBufferStale = true;
	    break;
	  case 'O': // reload the current JSON file, if there is one, reverting all edits
	    if (this.currentDataFile == null) {
	      loadWaveData();
	      isBufferStale = true;
	    } else {
	      fileSelectedOpen(currentDataFile);
	      isBufferStale = true;
	      if (isVerbose) println("--->> reloaded JSON file");
	    }
	    break;
	  case 'r': // toggles display window to fit screen or display at size
	    isFitToScreen = !isFitToScreen;
	    resizeWindow();
	    println("----->>> window width: "+ width +", window height: "+ height);
	    break;
	  case 's': // save the current image to a .png file
	    if (currentDataFile != null) {
	      java.nio.file.Path path = java.nio.file.Paths.get(currentDataFile.getAbsolutePath());
	      String fname = path.getFileName().toString();
	      fname = fname.substring(0, fname.length() - 5);
	      println("----->>> fname = "+ fname);
	      mapImage.save(fname + ".png");
	    } 
	    else {
	      mapImage.save("wavesynth_"+ ".png");
	    }
	    break;
	  case 'S': // save audio from WaveSynth
		  saveToAudio();
		  break;
	  case 'v': // toggle video recording
	    toggleRecording();
	    break;
	  case 'V': // record a complete video loop from frame 0 to stop frame
	    // Go to frame 0, turn recording on, turn animation on.
	    // This will record a complete video loop, from frame 0 to the
	    // stop frame value in the GUI control panel.
	    step = 0;
	    renderFrame(step);
	    isRecordingVideo = true;
	    isAnimating = true;
	  case 't': // sort wave data operators in control panel by frequency (lowest first), useful when saving to JSON
	    Collections.sort(wavesynth.waveDataList, new CompareWaveData());
	    currentWD = wavesynth.waveDataList.get(0);
	    wavesynth.prepareAnimation();
	    refreshGlobalPanel();
	    if (!isAnimating) renderFrame(step);
	    if (isVerbose) {
	      println("--->> Sorted wave data operators by frequency.");
	    }
	    break;
	  case 'z': // find nearest zero crossing in the audio signal and play from there
		  isFindZeroCrossing = !isFindZeroCrossing;
		  println("----- isFindZeroCrossing is "+ isFindZeroCrossing);
		  toggleZeroCrossing(isFindZeroCrossing);
		  break;
	  case 'h': // show Help Message in console
	  case 'H':
	    showHelp();
	    break;
	  default:
	    break;
	  }
	}
	
	public void showHelp() {
		println("\n * Press the UP arrow to increase audio output gain by 3.0 dB.");
		println(" * Press the DOWN arrow to decrease audio output gain by 3.0 dB.");
		println(" * press ' ' to turn animation on or off.");
		println(" * Press 'a' to scale all active WaveSynth amplitudes by ampFac.");
		println(" * Press 'A' to scale all active WaveSynth amplitudes by 1/ampFac.");
		println(" * Press 'c' to shift all active WaveSynth colors by colorShift * 360 degrees in the HSB color space.");
		println(" * Press 'C' to shift all active WaveSynth colors by -colorShift * 360 degrees in the HSB color space.");
		println(" * Press 'd' to print animation data to the console.");
		println(" * Press 'D' to print WaveSynth data to the console.");
		println(" * Press 'f' to scale all active WaveSynth frequencies by freqFac.");
		println(" * Press 'F' to scale all active WaveSynth frequencies by 1/freqFac.");
		println(" * Press 'p' to shift all active WaveSynth phases by phaseFac.");
		println(" * Press 'P' to shift all active WaveSynth phases by -phaseFac.");
		println(" * Press 'k' to show all current phase values in the console.");
		println(" * Press 'K' to set all phase values so that first frame looks like the current frame, then go to first frame.");
		println(" * Press '+' or '=' to make the image brighter.");
		println(" * Press '-' or '_' to make the image darker.");
		println("// ------------- COMMANDS FOR ANIMATION STEPPING ------------- //");
		println(" * Press 'e' to fast forward animation 1/8 of total steps.");
		println(" * Press 'E' to rewind animation 1/8 of total steps (loops back from end, if required).");
		println(" * Press 'i' to reset current animation step to initial value, 0.");
		println(" * Press 'u' to advance animation by 1 step.");
		println(" * Press 'U' to advance animation by 10 steps.");
		println(" * Press 'y' to rewind animation by 1 step.");
		println(" * Press 'Y' to rewind animation by 10 steps.");
		println(" * Press 'l' or 'L' to toggle animation looping on or off.");
		println("// ------------- MUTING COMMANDS ------------- //");
		println(" * press keys 1-8 to mute or unmute first eight wave data operators");
		println(" * press 'm' to print current WaveData muting states to console.");
		println(" * press 'M' to unmute all current WaveData operators.");
		println("// ------------- JSON COMMANDS ------------- //");
		println(" * press 'j' or 'J' to save WaveSynth settings to a JSON file.");
		println(" * press 'o' to open a new JSON file.");
		println(" * press 'O' to reload the current JSON file, if there is one, reverting all edits.");
		println("// ------------- MISCELLANEOUS COMMANDS ------------- //");
		println(" * Press 'r' to toggles display window to fit screen or display at size.");
		println(" * Press 's' to save the current image to a .png file.");
		println(" * Press 'S' to save audio from WaveSynth.");
		println(" * Press 'v' to toggle video recording.");
		println(" * Press 'V' to record a complete video loop from frame 0 to stop frame.");
		println(" * Press 't' to sort wave data operators in control panel by frequency (lowest first), useful when saving to JSON.");
		println(" * Press 'z' to find nearest zero crossing in the audio signal and play from there.");
		println(" * press 'h' or 'H' to show this help message in the console.");
	}

	/**
	 * Sets audioOut.gain.
	 * @param g   gain value for audioOut, in decibels
	 */
	public void setAudioGain(float g) {
		audioOut.setGain(g);
		outputGain = audioOut.getGain();
	}

	
	/**
	 * Turn animation on or off.
	 */
	public void toggleAnimation() {
		isAnimating = !isAnimating;
		// disable or enable some controls
		enableWDListControls(!isAnimating);
		if (isAnimating) {
			// setVideoRecord();
			// prepareAnimation();
			runVideoBtn.setText("Pause");
			startTime = millis();
			println("-----> start time is " + startTime / 1000.0 + " seconds at frame "+ step +" of "+ animSteps);
		} 
		else {
			runVideoBtn.setText("Run");
			stopTime = millis();
			println("-----> stop time is " + stopTime / 1000.0 + " seconds at frame "+ step +" of "+ animSteps);
		}
	}
	
	public void toggleLooping() {
		isLooping = !isLooping;
		println(isLooping ? "-- animation looping is true" : "-- animation looping is false");
	}
	
	/**
	 * Turn video recording on or off. Recording only takes place when animation is also on. 
	 * To record a video loop, from frame 0 to the Stop Frame / Steps value in the GUI control panel,
	 * use this command sequence:
	 * 		Turn animation off if it is currently on.
	 * 		Press 'i' to go to frame 0.
	 * 		Press 'v' to start recording, or check record in the control panel.
	 * 		Press spacebar to start animation and recording.
	 * 
	 * You can also just press the 'V' (capital vee) key to record from frame 0 to the stop frame. 
	 * 
	 */
	public void toggleRecording() {
		isRecordingVideo = !isRecordingVideo;
		println(" Recording video is "+ isRecordingVideo);
		if (isRecordingVideo) {
			if (!isAnimating) {
				println(" Press spacebar to start animation and video recording from frame "+ step);
			}
			else {
				println(" Recording animation from frame "+ step);
			}
		}
		else {
			if (isAnimating) {
				println(" Recording is off. Continuing animation from frame "+ step);
			}
			else {
				println(" Video recording and animation are off at frame "+ step);
			}
		}
		refreshGlobalPanel();
	}
	
	public void toggleZeroCrossing(boolean newFindZero) {
		for (PASamplerInstrument inst: pool.getInstruments() ) {
			for (PASamplerVoice voice : ((PASharedBufferSampler) inst.getSampler()).getVoices()) {
				voice.setFindZeroCrossing(newFindZero);
			}
		}
	}
	
	// ------------- WaveData methods ------------- //

	/**
	 * Scales the amplitude of an ArrayList of WaveData objects.
	 * 
	 * @param waveDataList an ArrayList of WaveData objects
	 * @param scale        the amount to scale the amplitude of each WaveData object
	 */
	public void scaleAmps(ArrayList<WaveData> waveDataList, float scale) {
		int i = 0;
		for (WaveData wd : waveDataList) {
			if (wd.isMuted) {
				i++;
				continue;
			}
			wd.setAmp(wd.amp * scale);
			if (isVerbose) println("----- set amplitude " + i + " to " + wd.amp);
			i++;
		}
	}
	
	/**
	 * Shifts the colors of an ArrayList of WaveData objects.
	 * @param waveDataList	an ArrayList of WaveData objects
	 * @param shift		the amount shift each color
	 */
	public void shiftColors(ArrayList<WaveData> waveDataList, float shift) {
		for (WaveData wd : waveDataList) {
			if (wd.isMuted)
				continue;
			wd.setWaveColor(WaveSynthBuilder.colorShift(wd.waveColor, shift));
		}
		if (isVerbose) println("----->>> shift colors " + shift);
	}
	
	/**
	 * Scales the frequencies of an ArrayList of WaveData objects.
	 * @param waveDataList	an ArrayList of WaveData objects
	 * @param scale		the amount to scale the frequency of each WaveData object
	 */
	public void scaleFreqs(ArrayList<WaveData> waveDataList, float scale) {
		int i = 0;
		for (WaveData wd : waveDataList) {
			if (wd.isMuted) {
				i++;
				continue;
			}
			wd.setFreq(wd.freq * scale);
			if (isVerbose) println("----- set frequency " + i + " to " + wd.freq);
		}
	}	
	
	/**
	 * Shifts the phase of an ArrayList of WaveData objects.
	 * @param waveDataList	an ArrayList of WaveData objects
	 * @param shift			amount to shift the phase of each WaveData object
	 */
	public void shiftPhases(ArrayList<WaveData> waveDataList, float shift) {
		for (WaveData wd : waveDataList) {
			if (wd.isMuted)
				continue;
			// wd.setPhase(wd.phase + shift - floor(wd.phase + shift));
			wd.setPhase(wd.phase + shift);
		}
		if (isVerbose) println("----->>> shiftPhase " + shift);
	}
	
	/**
	 * Prints the phase values of an ArrayList of WaveData objects.
	 * @param waveDataList	an ArrayList of WaveData objects
	 */
	public void showPhaseValues(ArrayList<WaveData> waveDataList) {
		int phaseStep = wavesynth.getStep();
		StringBuffer sb = new StringBuffer("\n----- current phase values scaled over (0, 1) -----\n");
		int i = 1;
		for (WaveData wd : waveDataList) {
			float m = wd.scaledPhaseAtFrame(phaseStep);
			sb.append(i++ +": "+ nf(m) + "; ");
		}
		sb.append("\n----- current phase values scaled over (0, TWO_PI) -----\n");
		i = 1;
		for (WaveData wd : waveDataList) {
			float m = wd.phaseAtFrame(phaseStep);
			sb.append(i++ +": "+ nf(m) + "; ");
		}
		println(sb);
	}
	
	/**
	 * Applies the current phase values to the initial values of the WaveSynth, so that
	 * the current state of the image display will appear as the first frame of 
	 * animation. Save the WaveSynth to a JSON file to keep the new phase values. 
	 * 
	 * @param waveDataList	an ArrayList of WaveData objects
	 */
	public void capturePhaseValues(ArrayList<WaveData> waveDataList) {
		int phaseStep = wavesynth.getStep();
		for (WaveData wd : waveDataList) {
			float currentPhase = wd.scaledPhaseAtFrame(phaseStep);
			wd.setPhase(currentPhase);
		}
	}
	
	/**
	 * Mutes or unmutes a WaveData operator (view in the control panel).
	 * @param elem	the index number of a WaveData object stored in a WaveSynth's waveDataList field
	 */
	public void toggleWDMute(int elem) {
		if (wavesynth.waveDataList.size() < elem + 1) return;
		WaveData wd = wavesynth.waveDataList.get(elem);
		wd.isMuted = !wd.isMuted;
		if (wd.isMuted) {
			wd.waveState = WaveState.MUTE;
		}
		else {
			wd.waveState = WaveState.ACTIVE;
		}
		if (!isAnimating) {
			wavesynth.renderFrame(step);
		}
	}
	
	/**
	 * Prints mute/active status of WaveData operators in supplied waveDataList. 
	 * @param waveDataList	an ArrayList of WaveData objects
	 */
	public void printWDStates(ArrayList<WaveData> waveDataList) {
		StringBuffer sb = new StringBuffer("Audio operators\n");
		int n = 1;
		for (WaveData wd :waveDataList) {
			sb.append("wave "+ (n++) +" "+ wd.waveState.toString() +", ");
		}
		println(sb.toString());
	}
	
	/**
	 * Unmutes all the operators in supplied waveDataList.
	 * @param waveDataList	an ArrayList of WaveData objects
	 */
	public void unmuteAllWD(ArrayList<WaveData> waveDataList) {
		StringBuffer sb = new StringBuffer("Audio operators\n");
		int n = 1;
		for (WaveData wd :waveDataList) {
			wd.setWaveState(WaveState.ACTIVE);
			sb.append("wave "+ (n++) +" "+ wd.waveState.toString() +", ");
		}
		println(sb.toString());		
	}
		
	/**
	 * Comparator class for sorting waveDataList by frequency or phase
	 */
	public class CompareWaveData implements Comparator <WaveData> {
	  boolean isCompareFrequency = true;

	  public int compare(WaveData wd1, WaveData wd2) {
	    if (isCompareFrequency) {
	      if (wd1.freq > wd2.freq) return 1;
	      if (wd1.freq < wd2.freq) return -1;
	    } 
	    else {
	      if (wd1.phase > wd2.phase) return 1;
	      if (wd1.phase < wd2.phase) return -1;
	    }
	    return 0;
	  }
	}
	
	/**
	 * Steps through the WaveSynth's list of WaveData, shows the current 
	 * WaveData operator in the control panel.
	 * @param up 	if true, increment waveDataIndex, otherwise, decrement it
	 */
	public void stepWaveData(boolean up) {
		int dataLen = wavesynth.waveDataList.size();
		if (up) {
			waveDataIndex = (waveDataIndex + 1 >= dataLen) ? 0 : waveDataIndex + 1;
		} 
		else {
			waveDataIndex = (waveDataIndex - 1 >= 0) ? waveDataIndex - 1 : dataLen - 1;
		}
		currentWD = wavesynth.waveDataList.get(waveDataIndex);
		loadWaveDataPanelValues(currentWD);
	}

	public void incWaveData() {
		stepWaveData(true);
	}

	public void decWaveData() {
		stepWaveData(false);
	}
	
	//----- END WAVEDATA METHODS ----- //
	
	
	/* ------------------------------------------------------------- */
	/* ----->>>              BEGIN G4P GUI                  <<<----- */
	/* ------------------------------------------------------------- */

	// G4P GUI 
	// Global controls
	GWindow controlWindow;
	GTextArea commentsField;
	GPanel globalPanel;
	GTextField blendField; 
	GLabel blend_label; 
	GTextField gammaField; 
	GLabel gamma_label; 
	GTextField noiseField;
	GLabel noise_label;
	GSpinner stepsSpinner;
	GLabel steps_label; 
	GSpinner stopSpinner;
	GLabel stop_label;
	GCheckbox histoCheck; 
	GTextField histoHighField; 
	GLabel histoHigh_label; 
	GTextField histoLowField; 
	GLabel histoLow_label; 
	// GToggleGroup scanTog; 
	// GOption hilbertOption; 
	// GOption mooreOption; 
	GLabel fps_label; 
	GDropList fpsMenu; 
	GCheckbox recordCheck;
	GButton refreshBtn;
	GSpinner capSpinner;
	GLabel cap_label;
	GCheckbox capCheck;
	GButton runVideoBtn;
	GTextField videoNameField; 
	GLabel videoName_label; 
	GButton openBtn;
	GButton saveBtn; 
	GButton saveAsBtn; 
	// WaveDataPanel controls
	GPanel waveDataPanel; 
	GTextField freqField; 
	GLabel freq_label; 
	GTextField ampField; 
	GLabel amp_label; 
	GTextField phaseField; 
	GLabel phase_label; 
	GTextField cyclesField; 
	GLabel cycles_label; 
	GTextField dcField;
	GLabel dc_label;
	GButton prevBtn; 
	GButton nextBtn; 
	GLabel waveDataLabel; 
	GCheckbox muteWave; 
	GCheckbox soloWave; 
	GButton newWaveBtn; 
	GButton dupWaveBtn; 
	GButton delWaveBtn; 
	// Controls used for color chooser dialog GUI 
	GButton btnColor;
	GView colorView;
	GLabel colorTitle;
	PGraphics colorPG;
	int sel_col = -1;
	/* ----->>> initialize GUI and control window <<<----- */
	/* ----->>> initialize currentWD and wavesynth.waveDataList before setting up the GUI  <<<----- */

	/**
	 * Initialize GUI and control window -- initialize wavesynth before calling this method.
	 */
	public void createGUI() {
	  createControlWindow();
	  initGlobalPanel();
	  createGlobalControls();
	  buildGlobalPanel();
	  loadGlobalPanelValues();
	  initWaveDataPanel();
	  createWaveDataControls();
	  buildWaveDataPanel();
	  loadWaveDataPanelValues(currentWD);
	  // get crackin'
	  controlWindow.loop();
	}

	/********************************************************************/
	/* ----->>>                CONTROL WINDOW                  <<<----- */
	/********************************************************************/
	/* ----->>> set up GUI and initialize the control window <<<----- */
	public void createControlWindow() {
	  G4P.messagesEnabled(false);
	  G4P.setGlobalColorScheme(GCScheme.BLUE_SCHEME);
	  G4P.setMouseOverEnabled(false);
	  surface.setTitle("Animation Window");
	  surface.setLocation(60, 20);
	  if (isSecondScreen) surface.setLocation(screen2x, screen2y); // on second screen
	  controlWindow = GWindow.getWindow(this, "Data Fields", 60, 420, 480, 560, JAVA2D);
	  controlWindow.noLoop();
	  controlWindow.setActionOnClose(G4P.KEEP_OPEN);
	  controlWindow.addDrawHandler(this, "winDraw");
	  // the key handler under development, don't load
	  // controlWindow.addKeyHandler(this, "winKey");
	  // ignore mouse events in our control panel, they just get in the way of other events
	  //controlWindow.addMouseHandler(this, "winMouse");
	  // we don't need these events
	  //controlWindow.addPreHandler(this, "winPre");
	  //controlWindow.addPostHandler(this, "winPost");
	  //controlWindow.addOnCloseHandler(this, "winClose");
	  createCommentsField();
	}
	public void createCommentsField() {
	  commentsField = new GTextArea(controlWindow, 5, 480, 470, 70, G4P.SCROLLBARS_NONE);
	  commentsField.setOpaque(true);
	  commentsField.setWrapWidth(460);
	  commentsField.addEventHandler(this, "comments_hit");
	  commentsField.setText(comments);
	}
	/********************************************************************/
	/* ----->>>                 GLOBAL PANEL                   <<<----- */
	/********************************************************************/
	/* ----->>> initialize global panel <<<----- */
	public void initGlobalPanel() {
	  globalPanel = new GPanel(controlWindow, 5, 5, 230, 470, "Globals");
	  globalPanel.setCollapsed(false);
	  globalPanel.setCollapsible(false);
	  globalPanel.setDraggable(false);
	  globalPanel.setText("Globals");
	  globalPanel.setOpaque(true);
	  globalPanel.addEventHandler(this, "globalPanel_hit");
	}
	/* ----->>> add controls to global panel <<<----- */
	public void createGlobalControls() {
	    int ypos = 30;
	    int yinc = 30;
	    /* ----->>> <<<----- */
	    blendField = new GTextField(controlWindow, 10, ypos, 120, 20, G4P.SCROLLBARS_NONE);
	    blendField.setNumeric(0.01f, 10.0f, 0.5f);
	    blendField.setOpaque(true);
	    blendField.addEventHandler(this, "blendField_hit");
	    blend_label = new GLabel(controlWindow, 140, ypos, 80, 20);
	    blend_label.setText("Blend");
	    blend_label.setOpaque(false);
	    /* ----->>> <<<----- */
	    ypos += yinc;
	    gammaField = new GTextField(controlWindow, 10, ypos, 120, 20, G4P.SCROLLBARS_NONE);
	    gammaField.setNumeric(0.1f, 10.0f, 0.5f);
	    gammaField.setOpaque(true);
	    gammaField.addEventHandler(this, "gammaField_hit");
	    gamma_label = new GLabel(controlWindow, 140, ypos, 80, 20);
	    gamma_label.setText("Gamma");
	    gamma_label.setOpaque(false);
	    /* ----->>> <<<----- */
	    // ypos += yinc;
	    /* space for additional controls */
	    /* ----->>> <<<----- */
	    ypos += yinc;
	    histoCheck = new GCheckbox(controlWindow, 10, ypos, 120, 20);
	    histoCheck.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
	    histoCheck.setText("Scale histogram");
	    histoCheck.setOpaque(false);
	    histoCheck.addEventHandler(this, "histoCheck_hit");
	    /* ----->>> <<<----- */
	    ypos += yinc;
	    histoHighField = new GTextField(controlWindow, 10, ypos, 50, 20, G4P.SCROLLBARS_NONE);
	    histoHighField.setNumeric(255, 128, 233);
	    histoHighField.setOpaque(true);
	    histoHighField.addEventHandler(this, "histoHigh_hit");
	    histoHigh_label = new GLabel(controlWindow, 70, ypos, 40, 20);
	    histoHigh_label.setText("High");
	    histoHigh_label.setOpaque(false);
	    /* ----->>> <<<----- */
	    histoLowField = new GTextField(controlWindow, 120, ypos, 50, 20, G4P.SCROLLBARS_NONE);
	    histoLowField.setNumeric(128, 0, 8);
	    histoLowField.setOpaque(true);
	    histoLowField.addEventHandler(this, "histoLow_hit");
	    histoLow_label = new GLabel(controlWindow, 180, ypos, 40, 20);
	    histoLow_label.setText("Low");
	    histoLow_label.setOpaque(false);
	    /* ----->>> <<<----- */
	    ypos += yinc;
	    /* space for additional controls */
	    /* ----->>> <<<----- */
	    ypos += yinc;
	    stepsSpinner = new GSpinner(controlWindow, 10, ypos, 120, 20);
	    stepsSpinner.setLimits(240, 8, 24000, 1);
	    stepsSpinner.addEventHandler(this, "stepsSpinner_hit");
	    steps_label = new GLabel(controlWindow, 140, ypos, 80, 20);
	    steps_label.setText("Steps");
	    steps_label.setOpaque(false);
	    /* ----->>> <<<----- */
	    ypos += yinc;
	    stopSpinner = new GSpinner(controlWindow, 10, ypos, 120, 20);
	    stopSpinner.setLimits(240, 8, 36000, 1);
	    stopSpinner.addEventHandler(this, "stopSpinner_hit");
	    stop_label = new GLabel(controlWindow, 140, ypos, 80, 20);
	    stop_label.setText("Stop frame");
	    stop_label.setOpaque(false);
	    /* ----->>> <<<----- */
	    ypos += yinc;
	    fpsMenu = new GDropList(controlWindow, 10, ypos, 40, 80, 3, 10);
	    fpsMenu.setItems(new String[] { "12", "24", "30" }, 1);
	    fpsMenu.addEventHandler(this, "fpsMenu_hit");
	    fps_label = new GLabel(controlWindow, 55, ypos, 40, 20);
	    fps_label.setText("FPS");
	    fps_label.setOpaque(false);
	    recordCheck = new GCheckbox(controlWindow, 90, ypos, 80, 20);
	    recordCheck.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
	    recordCheck.setText("Record");
	    recordCheck.setOpaque(false);
	    recordCheck.setSelected(isRecordingVideo);
	    recordCheck.addEventHandler(this, "recordCheck_hit");
	    /* ----->>> <<<----- */
	    ypos += yinc;
	    capSpinner = new GSpinner(controlWindow, 10, ypos, 60, 20);
	    capSpinner.setLimits(12, 1, 128, 1);
	    capSpinner.addEventHandler(this, "capSpinner_hit");
	    cap_label = new GLabel(controlWindow, 75, ypos, 60, 20);
	    cap_label.setText("Frames");
	    cap_label.setOpaque(false);
	    capCheck = new GCheckbox(controlWindow, 125, ypos, 80, 20);
	    capCheck.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
	    capCheck.setText("Capture");
	    capCheck.setOpaque(false);
	    capCheck.setSelected(isCaptureFrames);
	    capCheck.addEventHandler(this, "capCheck_hit");
	    /* ----->>> <<<----- */
	    ypos += yinc + yinc / 2;
	    runVideoBtn = new GButton(controlWindow, 10, ypos, 60, 30);
	    runVideoBtn.setText("Run");
	    runVideoBtn.addEventHandler(this, "runVideoBtn_hit");
	    // Refresh Button
	    refreshBtn = new GButton(controlWindow, 160, ypos, 60, 30);
	    refreshBtn.setText("Refresh");
	    refreshBtn.addEventHandler(this, "refreshBtn_hit");
	    /* ----->>> <<<----- */
	    videoName_label = new GLabel(controlWindow, 10, 380, 120, 20);
	    videoName_label.setTextAlign(GAlign.LEFT, GAlign.BOTTOM);
	    videoName_label.setText("Video file name");
	    videoName_label.setOpaque(false);
	    videoNameField = new GTextField(controlWindow, 10, 400, 210, 20, G4P.SCROLLBARS_NONE);
	    videoNameField.setOpaque(true);
	    videoNameField.addEventHandler(this, "videoNameField_hit");
	    videoNameField.setText("animation.mp4");
	    /* ----->>> <<<----- */
	    openBtn = new GButton(controlWindow, 10, 430, 70, 30);
	    openBtn.setText("Open JSON");
	    openBtn.addEventHandler(this, "openBtn_hit");
	    saveBtn = new GButton(controlWindow, 90, 430, 60, 30);
	    saveBtn.setText("Save");
	    saveBtn.addEventHandler(this, "saveBtn_hit");
	    saveAsBtn = new GButton(controlWindow, 160, 430, 60, 30);
	    saveAsBtn.setText("Save As");
	    saveAsBtn.addEventHandler(this, "saveAsBtn_hit");
	}
	
	/* ----->>> populate the global panel with controls <<<----- */
	public void buildGlobalPanel() {
	  globalPanel.addControl(blendField);
	  globalPanel.addControl(blend_label);
	  globalPanel.addControl(gammaField);
	  globalPanel.addControl(gamma_label);
	  globalPanel.addControl(steps_label);
	  globalPanel.addControl(stepsSpinner);
	  globalPanel.addControl(stop_label);
	  globalPanel.addControl(stopSpinner);
	  globalPanel.addControl(histoCheck);
	  globalPanel.addControl(histoHighField);
	  globalPanel.addControl(histoHigh_label);
	  globalPanel.addControl(histoLowField);
	  globalPanel.addControl(histoLow_label);
	  globalPanel.addControl(fpsMenu);
	  globalPanel.addControl(fps_label);
	  globalPanel.addControl(recordCheck);
	  globalPanel.addControl(refreshBtn);
	  globalPanel.addControl(capSpinner);
	  globalPanel.addControl(cap_label);
	  globalPanel.addControl(capCheck);
	  globalPanel.addControl(runVideoBtn);
	  globalPanel.addControl(videoNameField);
	  globalPanel.addControl(videoName_label);
	  globalPanel.addControl(openBtn);
	  globalPanel.addControl(saveBtn);
	  globalPanel.addControl(saveAsBtn);
	  globalPanel.setCollapsed(false);
	}
	public void loadGlobalPanelValues() {
	  blendField.setText(str(wavesynth.gain));
	  gammaField.setText(str(wavesynth.gamma));
	  histoHighField.setText(str(wavesynth.histoHigh));
	  histoLowField.setText(str(wavesynth.histoLow));
	  stepsSpinner.setValue(wavesynth.animSteps);
	  stopSpinner.setValue(animStop);
	  capSpinner.setValue(snapCount);
	  histoCheck.setSelected(wavesynth.isScaleHisto);
	  // recording and animating are controlled by global variables, not in WaveSynth instance
	  recordCheck.setSelected(isRecordingVideo);
	  capCheck.setSelected(isCaptureFrames);
	  if (wavesynth.videoFrameRate == 30) fpsMenu.setSelected(2);
	  else if (wavesynth.videoFrameRate == 24) fpsMenu.setSelected(1);
	  else if (wavesynth.videoFrameRate == 12) fpsMenu.setSelected(0);
	  else fpsMenu.setSelected(1);
	  videoNameField.setText(wavesynth.videoFilename); 
	  commentsField.setText(wavesynth.getComments());
	}
	// called for globals that may be affected by a keypress
	public void refreshGlobalPanel() {
	  wavesynth. prepareAnimation();
	  loadGlobalPanelValues();
	  loadWaveDataPanelValues(currentWD);
	}
	/* ----->>> end of the global panel setup <<<----- */
	/********************************************************************/
	/* ----->>>               WAVE DATA PANEL                  <<<----- */
	/********************************************************************/
	/* ----->>> initialize wave data panel <<<----- */
	public void initWaveDataPanel() {
	  waveDataPanel = new GPanel(controlWindow, 245, 5, 230, 470, "Operators");
	  waveDataPanel.setDraggable(false);
	  waveDataPanel.setCollapsed(false);
	  waveDataPanel.setCollapsible(false);
	  waveDataPanel.setText("Operators");
	  waveDataPanel.setOpaque(true);
	  waveDataPanel.addEventHandler(this, "waveDataPanel_hit");
	}
	/* ----->>> add controls to the wave data panel <<<----- */
	public void createWaveDataControls() {
	  /* ----->>>  <<<----- */
	  freqField = new GTextField(controlWindow, 10, 30, 120, 20, G4P.SCROLLBARS_NONE);
	  // max frequency 131072 = (1024 * 1024) / 8 -- would it be better divided by 4 or 16, get get square resolution?
	  freqField.setNumeric(0.01f, 131072.0f, 440.0f);
	  freqField.setText("440.0");
	  freqField.setOpaque(true);
	  freqField.addEventHandler(this, "freqField_hit");
	  freq_label = new GLabel(controlWindow, 140, 30, 80, 20);
	  freq_label.setText("Frequency");
	  freq_label.setOpaque(false);
	  /* ----->>>  <<<----- */
	  ampField = new GTextField(controlWindow, 10, 60, 120, 20, G4P.SCROLLBARS_NONE);
	  ampField.setNumeric(-64.0f, 64.0f, 0.5f);
	  ampField.setText("0.5");
	  ampField.setOpaque(true);
	  ampField.addEventHandler(this, "ampField_hit");
	  amp_label = new GLabel(controlWindow, 140, 60, 80, 20);
	  amp_label.setText("Amplitude");
	  amp_label.setOpaque(false);
	  /* ----->>>  <<<----- */
	  phaseField = new GTextField(controlWindow, 10, 90, 120, 20, G4P.SCROLLBARS_NONE);
	  // phaseField is a decimal fraction of TWO_PI, could range just between -1 and 1, 
	  // but we'll give it some leeway 
	  // TODO check how we're handling phase calculations
	  phaseField.setNumeric(-32.0f, 32.0f, 0.5f);
	  phaseField.setText("0.5");
	  phaseField.setOpaque(true);
	  phaseField.addEventHandler(this, "phaseField_hit");
	  phase_label = new GLabel(controlWindow, 140, 90, 80, 20);
	  phase_label.setText("Phase");
	  phase_label.setOpaque(false);
	  /* ----->>>  <<<----- */
	  dcField = new GTextField(controlWindow, 10, 120, 120, 20, G4P.SCROLLBARS_NONE);
	  dcField.setNumeric(-2.0f, 2.0f, 0.0f);
	  dcField.setText("0.0");
	  dcField.setOpaque(true);
	  dcField.addEventHandler(this, "dc_hit");
	  dc_label = new GLabel(controlWindow, 140, 120, 80, 20);
	  dc_label.setText("DC Offset");
	  dc_label.setOpaque(false);
	  /* ----->>>  <<<----- */
	  cyclesField = new GTextField(controlWindow, 10, 150, 120, 20, G4P.SCROLLBARS_NONE);
	  cyclesField.setNumeric(-1024.0f, 1024.0f, 1.0f);
	  cyclesField.setText("1.0");
	  cyclesField.setOpaque(true);
	  cyclesField.addEventHandler(this, "cycles_hit");
	  cycles_label = new GLabel(controlWindow, 140, 150, 80, 20);
	  cycles_label.setText("Cycles");
	  cycles_label.setOpaque(false);
	  /* ----->>>  <<<----- */
	  prevBtn = new GButton(controlWindow, 10, 240, 36, 20);
	  prevBtn.setText("<");
	  prevBtn.addEventHandler(this, "prev_hit");
	  waveDataLabel = new GLabel(controlWindow, 50, 240, 96, 20);
	  waveDataLabel.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
	  waveDataLabel.setText("Op 0 of 0");
	  waveDataLabel.setOpaque(true);
	  nextBtn = new GButton(controlWindow, 150, 240, 36, 20);
	  nextBtn.setText(">");
	  nextBtn.addEventHandler(this, "next_hit");
	  muteWave = new GCheckbox(controlWindow, 10, 270, 80, 20);
	  muteWave.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
	  muteWave.setText("Mute");
	  muteWave.setOpaque(false);
	  muteWave.addEventHandler(this, "muteWave_hit");
	  soloWave = new GCheckbox(controlWindow, 90, 270, 80, 20);
	  soloWave.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
	  soloWave.setText("Solo");
	  soloWave.setOpaque(false);
	  soloWave.addEventHandler(this, "soloWave_hit");
	  /* ----->>>  <<<----- */
	  createColorChooserGUI(5, 178, 160, 60, 6);
	  /* ----->>>  <<<----- */
	  newWaveBtn = new GButton(controlWindow, 10, 430, 60, 30);
	  newWaveBtn.setText("New");
	  newWaveBtn.addEventHandler(this, "newWave_hit");
	  dupWaveBtn = new GButton(controlWindow, 80, 430, 70, 30);
	  dupWaveBtn.setText("Duplicate");
	  dupWaveBtn.addEventHandler(this, "dupWave_hit");
	  delWaveBtn = new GButton(controlWindow, 160, 430, 60, 30);
	  delWaveBtn.setText("Delete");
	  delWaveBtn.addEventHandler(this, "delWave_hit");
	}
	// Color Chooser GUI
	// @TODO dispense with the "Choose" button and just click on the GView to show the color picker
	public void createColorChooserGUI(int x, int y, int w, int h, int border) {
	  // Store picture frame (not used)
	  // rects.add(new Rectangle(x, y, w, h));
	  // Set inner frame position
	  x += border; 
	  y += border;
	  w -= 2*border; 
	  h -= 2*border;
	  colorTitle = new GLabel(controlWindow, x, y, w, 20);
	  colorTitle.setText("Color: ", GAlign.LEFT, GAlign.MIDDLE);
	  colorTitle.setOpaque(true);
	  colorTitle.setTextBold();
	  waveDataPanel.addControl(colorTitle);
	  btnColor = new GButton(controlWindow, x+70, y+26, 80, 20, "Choose");
	  btnColor.addEventHandler(this, "handleColorChooser");
	  sel_col = color(127, 127, 127, 255);
	  colorTitle.setText("Color: "+ (sel_col >> 16 & 0xFF) +", "+ (sel_col >> 8 & 0xFF) +", "+ (sel_col & 0xFF));
	  waveDataPanel.addControl(btnColor);
	  colorView = new GView(controlWindow, x, y+26, 60, 20, JAVA2D);
	  waveDataPanel.addControl(colorView);
	  colorPG = colorView.getGraphics();
	  colorPG.beginDraw();
	  colorPG.background(sel_col);
	  colorPG.endDraw();
	}
	public void setWaveDataPanelColor(int sel_col) {
	  colorTitle.setText("Color: "+ (sel_col >> 16 & 0xFF) +", "+ (sel_col >> 8 & 0xFF) +", "+ (sel_col & 0xFF));
	  colorPG.beginDraw();
	  colorPG.background(sel_col);
	  colorPG.endDraw();
	}
	/* ----->>> populate the wave data panel with controls <<<----- */
	public void buildWaveDataPanel() {
	  waveDataPanel.addControl(freqField);
	  waveDataPanel.addControl(freq_label);
	  waveDataPanel.addControl(ampField);
	  waveDataPanel.addControl(amp_label);
	  waveDataPanel.addControl(phaseField);
	  waveDataPanel.addControl(phase_label);
	  waveDataPanel.addControl(cyclesField);
	  waveDataPanel.addControl(cycles_label);
	  waveDataPanel.addControl(dcField);
	  waveDataPanel.addControl(dc_label);
	  waveDataPanel.addControl(prevBtn);
	  waveDataPanel.addControl(nextBtn);
	  waveDataPanel.addControl(waveDataLabel);
	  waveDataPanel.addControl(muteWave);
	  waveDataPanel.addControl(soloWave);
	  waveDataPanel.addControl(newWaveBtn);
	  waveDataPanel.addControl(dupWaveBtn);
	  waveDataPanel.addControl(delWaveBtn);
	}
	public void loadWaveDataPanelValues(WaveData wd) {
	  freqField.setText(str(wd.freq));
	  ampField.setText(str(wd.amp));
	  phaseField.setText(str(wd.phase));
	  cyclesField.setText(str(wd.phaseCycles));
	  dcField.setText(str(wd.dc));
	  int dataLen = wavesynth.waveDataList.size();
	  waveDataLabel.setText("Op "+ (waveDataIndex + 1) +" of "+ dataLen);
	  setWaveDataPanelColor(wd.waveColor);
	  if (wd.isMuted) wd.waveState = WaveData.WaveState.MUTE;
	  switch (wd.waveState) {
	    case ACTIVE: {
	      wd.isMuted = false;
	      muteWave.setSelected(false);
	      soloWave.setSelected(false);
	      break;
	    }
	    case SOLO: {
	      wd.isMuted = false;
	      muteWave.setSelected(false);
	      soloWave.setSelected(true);
	      break;
	    }
	    case MUTE: {
	      wd.isMuted = true;
	      muteWave.setSelected(true);
	      soloWave.setSelected(false);
	      break;
	    }
	    case SUSPENDED: {
	      wd.isMuted = true;
	      muteWave.setSelected(true);
	      soloWave.setSelected(false);
	      break;
	    }
	  }
	  // rely on the global blendChannels 
	  if (!isAnimating) renderFrame(step);
	  
	  /*
	  muteWave.setText("Mute");
	  newWaveBtn.setText("New");
	  dupWaveBtn.setText("Dupe");
	  delWaveBtn.setText("Delete");
	  */
	}
	/********************************************************************/
	/*            Window and Global Control Panel Handlers              */
	/********************************************************************/
	// our draw method, call each time through the event loop
	synchronized public void winDraw(PApplet appc, GWinData data) { 
	  appc.background(color(254, 233, 178));
	}
	
	// respond to key in window
	// not loaded
	// under development
	public void winKey(PApplet appc, GWinData data, KeyEvent evt) {
		if (!evt.isControlDown()) {
			println("-- exit winKey");
			return;
		}
		if (evt.getAction() == KeyEvent.RELEASE) {
			println("-- continue winKey");
			parseKey(evt.getKey(), evt.getKeyCode());
		}
		
	}

	public void globalPanel_hit(GPanel panel, GEvent event) { 
	  // nothing doing
	}
	
	/*
	  Dropped:
	  synchronized public void winMouse(PApplet appc, GWinData data, processing.event.MouseEvent mevent)
	  synchronized public void winKey(PApplet appc, GWinData data, processing.event.KeyEvent kevent)
	  synchronized public void winPre(PApplet appc, GWinData data)
	  synchronized public void winPost(PApplet appc, GWinData data) 
	  public void winClose(GWindow window)
	*/

	public void comments_hit(GTextArea source, GEvent event) {
	  // println("commentsField - GTextField >> GEvent." + event + " @ " + millis() + " value: " + commentsField.getText());
	  wavesynth.setComments(commentsField.getText());
	}

	public void blendField_hit(GTextField source, GEvent event) { 
	  // println("blendField - GTextField >> GEvent." + event + " @ " + millis() + " value: " + blendField.getValueF());
	  wavesynth.setGain(blendField.getValueF());
	  //if (!isAnimating) blendChannels(step)(step);
	} 
	
	public void gammaField_hit(GTextField source, GEvent event) { 
	  // println("gammaField - GTextField >> GEvent." + event + " @ " + millis());
	  wavesynth.setGamma(gammaField.getValueF());
	  //if (!isAnimating) blendChannels(step)(step);
	} 
	
	public void stepsSpinner_hit(GSpinner source, GEvent event) {
	  // println("stepsSpinner - GSpinner >> GEvent." + event + " @ " + millis());
	  // @TODO recalculate global wd settings when animSteps changes
	  animSteps = stepsSpinner.getValue();
	  wavesynth.setAnimSteps(animSteps);
	  for (WaveData wd : wavesynth.waveDataList) {
	    wd.phaseInc = (wd.phaseCycles * TWO_PI)/wavesynth.animSteps;
	  }
	  //if (!isAnimating) blendChannels(step)(step);
	}
	
	public void stopSpinner_hit(GSpinner source, GEvent event) {
	  // println("stepsSpinner - GSpinner >> GEvent." + event + " @ " + millis());
	  animStop = stopSpinner.getValue();
	  //if (!isAnimating) blendChannels(step)(step);
	}
	
	public void histoCheck_hit(GCheckbox source, GEvent event) { 
	  // println("histoCheck - GCheckbox >> GEvent." + event + " @ " + millis());
	  wavesynth.setScaleHisto(histoCheck.isSelected());
	  //if (!isAnimating) blendChannels(step)(step);
	} 
	
	public void histoHigh_hit(GTextField source, GEvent event) { 
	  // println("histoHigh - GTextField >> GEvent." + event + " @ " + millis());
	  wavesynth.setHistoHigh(histoHighField.getValueI());
	  //if (!isAnimating) blendChannels(step)(step);
	} 
	
	public void histoLow_hit(GTextField source, GEvent event) { 
	  // println("histoLow - GTextField >> GEvent." + event + " @ " + millis());
	  wavesynth.setHistoLow(histoLowField.getValueI());
	  //if (!isAnimating) blendChannels(step)(step);
	} 

	public void videoNameField_hit(GTextField source, GEvent event) { 
	  // println("videoNameField - GTextField >> GEvent." + event + " @ " + millis());
	  wavesynth.setVideoFilename(videoNameField.getText());
	  // update global videoFilename
	  videoFilename = wavesynth.getVideoFilename();
	} 
	
	public void fpsMenu_hit(GDropList source, GEvent event) { 
	  // println("fpsMenu - GDropList >> GEvent." + event + " @ " + millis());
	  wavesynth.setVideoFrameRate(Integer.valueOf(fpsMenu.getSelectedText()));
	  println("----->>> videoFPS = "+ wavesynth.videoFrameRate);  
	} 
	
	// this stays global
	public void recordCheck_hit(GCheckbox source, GEvent event) { 
	  println("recordCheck - GCheckbox >> GEvent." + event + " @ " + millis());
	  isRecordingVideo = recordCheck.isSelected();
	  if (isRecordingVideo) {
	    if (isOversize) {
	      println("----->>> Oversize image, setting to full size to record correctly.");
	      isFitToScreen = false;
	      resizeWindow();
	    }
	  }
	  refreshGlobalPanel();
	  println("----->>> isRecordingVideo = "+ isRecordingVideo);
	} 

	// prob want a refresh call in WaveSynth
	public void refreshBtn_hit(GButton source, GEvent event) {
	  // println("refreshBtn - GButton >> GEvent." + event + " @ " + millis());
	  if (!isDesignMode) {
	    
	  }
	  renderFrame(step);
	}

	// stays global
	public void capSpinner_hit(GSpinner source, GEvent event) {
	  // println("capSpinner - GSpinner >> GEvent." + event + " @ " + millis());
	  snapCount = capSpinner.getValue();
	}

	// stays global
	public void capCheck_hit(GCheckbox source, GEvent event) {
	  // println("capSpinner - GSpinner >> GEvent." + event + " @ " + millis());
	  isCaptureFrames = capCheck.isSelected();
	  println("----->>> isCaptureFrames = "+ isCaptureFrames);
	}

	// handled globally with calls on wavesynth or list of WaveAnimators, when relevant
	public void runVideoBtn_hit(GButton source, GEvent event) {
	  toggleAnimation(); //<>//
	}

	public void openBtn_hit(GButton source, GEvent event) { 
	  // println("openBtn - GButton >> GEvent." + event + " @ " + millis());
	  if (isAnimating) toggleAnimation();
	  loadWaveData();
	  // isAnimating = oldIsAnimating;
	} 

	public void saveBtn_hit(GButton source, GEvent event) { 
	  // println("saveBtn - GButton >> GEvent." + event + " @ " + millis());
	  if (isAnimating) toggleAnimation();
	  if ((currentDataFile == null) || (currentDataFile.getAbsolutePath().equals(""))) {
	    saveWaveData();
	  }
	  else {
	    fileSelectedWrite(currentDataFile);
	  }
	} 
	
	public void saveAsBtn_hit(GButton source, GEvent event) { 
	  // println("saveAsBtn - GButton >> GEvent." + event + " @ " + millis());
	  if (isAnimating) toggleAnimation();
	  saveWaveData();
	} 
	
	/********************************************************************/
	/*                Wave Data Control Panel Handlers                  */
	/********************************************************************/

	// not active
	public void waveDataPanel_hit(GPanel source, GEvent event) { 
	  println("waveDataPanel - GPanel >> GEvent." + event + " @ " + millis());
	} 

	public void freqField_hit(GTextField source, GEvent event) { 
	  // println("freqField - GTextField >> GEvent." + event + " @ " + millis());
	  float newFreq = freqField.getValueF();
	  // currentWD is selected from wavesynth.waveDataList, where wavesynth is the (current) WaveSynth instance
	  currentWD.setFreq(newFreq);
	} 
	
	public void ampField_hit(GTextField source, GEvent event) { 
	  // println("ampField - GTextField >> GEvent." + event + " @ " + millis());
	  float newAmp = ampField.getValueF();
	  // currentWD is selected from wavesynth.waveDataList, where wavesynth is the (current) WaveSynth instance
	  currentWD.setAmp( newAmp);
	} 
	
	public void phaseField_hit(GTextField source, GEvent event) { 
	  // println("phaseField - GTextField >> GEvent." + event + " @ " + millis());
	  float newPhase = phaseField.getValueF();
	  // we only need the decimal fraction, which is a portion of TWO_PI
	  // next time we load the GUI the newPhase value will appear
	  newPhase -= floor(newPhase);
	  // currentWD is selected from wavesynth.waveDataList, where wavesynth is the (current) WaveSynth instance
	  // TODO verify compatibility with older JSON format
	  currentWD.setPhase(newPhase);
	  if (!isAnimating) renderFrame(step);
	}
	
	public void cycles_hit(GTextField source, GEvent event) { 
	  // println("cyclesField - GTextField >> GEvent." + event + " @ " + millis());
	  float newCycles = cyclesField.getValueF();
	  // currentWD is selected from wavesynth.waveDataList, where wavesynth is the (current) WaveSynth instance
	  currentWD.setCycles(newCycles); 
	}
	
	public void dc_hit(GTextField source, GEvent event) {
	  // println("dcField - GTextField >> GEvent." + event + " @ " + millis());
	  float newDc = dcField.getValueF();
	  // currentWD is selected from wavesynth.waveDataList, where wavesynth is the (current) WaveSynth instance
	  currentWD.setDc(newDc);
	}
	
	// G4P code for colour chooser
	public void handleColorChooser(GButton button, GEvent event) {
	  // println("btnColor - GButton >> GEvent." + event + " @ " + millis());
	  sel_col = G4P.selectColor(currentWD.waveColor);
	  colorTitle.setText("Color: "+ (sel_col >> 16 & 0xFF) +", "+ (sel_col >> 8 & 0xFF) +", "+ (sel_col & 0xFF));
	  colorPG.beginDraw();
	  colorPG.background(sel_col);
	  colorPG.endDraw();
	  currentWD = wavesynth.waveDataList.get(waveDataIndex);
	  if (isVerbose) println("==> selected color: "+ PixelAudioMapper.colorString(sel_col));
	  // be sure to set color value in wavesynth.waveColors array, otherwise the display won't update
	  currentWD.setWaveColor(sel_col);
	  wavesynth.waveColors[waveDataIndex] = sel_col;
	  if (step != 0) wavesynth.prepareAnimation();
	  renderFrame(step);
	  mapImage = wavesynth.mapImage;
	}
	
	public void prev_hit(GButton source, GEvent event) { 
	  //println("prevBtn - GButton >> GEvent." + event + " @ " + millis());
	  decWaveData();
	} 
	
	public void next_hit(GButton source, GEvent event) { 
	  //println("nextBtn - GButton >> GEvent." + event + " @ " + millis());
	  incWaveData();
	} 
	
	public void newWave_hit(GButton source, GEvent event) { 
	  // println("newWaveBtn - GButton >> GEvent." + event + " @ " + millis());
	  WaveData wd = new WaveData();
	  wavesynth.waveDataList.add(++waveDataIndex, wd);
	  currentWD = wavesynth.waveDataList.get(waveDataIndex);
	  wavesynth.updateWaveColors();
	  loadWaveDataPanelValues(wd);
	  refreshGlobalPanel();
	} 
	
	public void dupWave_hit(GButton source, GEvent event) { 
	  // println("dupWaveBtn - GButton >> GEvent." + event + " @ " + millis());
	  WaveData wd = currentWD.clone();
	  wavesynth.waveDataList.add(++waveDataIndex, wd);
	  currentWD = wavesynth.waveDataList.get(waveDataIndex);
	  wavesynth.updateWaveColors();
	  loadWaveDataPanelValues(wd);
	  refreshGlobalPanel();
	} 
	
	public void delWave_hit(GButton source, GEvent event) { 
	  println("delWaveBtn - GButton >> GEvent." + event + " @ " + millis());
	  int remIndex = waveDataIndex;
	  if (waveDataIndex == wavesynth.waveDataList.size() - 1) waveDataIndex = 0;
	  wavesynth.waveDataList.remove(remIndex);
	  currentWD = wavesynth.waveDataList.get(waveDataIndex);
	  wavesynth.updateWaveColors();
	  loadWaveDataPanelValues(currentWD);
	  refreshGlobalPanel();
	} 
	
	// see enum WaveData.WaveState {ACTIVE, SOLO, MUTE, SUSPENDED};
	public void muteWave_hit(GCheckbox source, GEvent event) { 
	  //println("muteWave - GCheckbox >> GEvent." + event + " @ " + millis());
	  currentWD.isMuted = muteWave.isSelected();
	  currentWD.waveState = muteWave.isSelected() ? WaveData.WaveState.MUTE : WaveData.WaveState.ACTIVE;
	  loadWaveDataPanelValues(currentWD);
	} 
	
	// see enum WaveData.WaveState {ACTIVE, SOLO, MUTE, SUSPENDED};
	public void soloWave_hit(GCheckbox source, GEvent event) { 
	  //println("soloWave - GCheckbox >> GEvent." + event + " @ " + millis());
	  // if the soloWave checkbox is selected, set currentWD WaveState to SOLO, otherwise, to ACTIVE
	  currentWD.waveState = soloWave.isSelected() ? WaveData.WaveState.SOLO : WaveData.WaveState.ACTIVE ;
	  if (currentWD.waveState == WaveData.WaveState.SOLO) {
	    for (WaveData wd : wavesynth.waveDataList) {
	      if (currentWD == wd) continue;
	      if (wd.waveState == WaveData.WaveState.ACTIVE) {
	        wd.waveState = WaveData.WaveState.SUSPENDED;
	        wd.isMuted = true;
	      }
	    }
	  }
	  else {
	    boolean listHasSolos = false;
	    for (WaveData wd : wavesynth.waveDataList) {
	      if (wd.waveState == WaveData.WaveState.SOLO) listHasSolos = true;
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
	  loadWaveDataPanelValues(currentWD);
	}

	/********************************************************************/
	/*                 Enable and Disable GUI Elements                  */
	/********************************************************************/

	public void enableWDListControls(boolean enable) {
	  // newWave, dupWave, delWave have to be disabled during animation 
	  // because G4P runs separate threads from PApplet
	  // println("----->>> enableWDListControls "+ enable);
	  newWaveBtn.setEnabled(enable);
	  dupWaveBtn.setEnabled(enable);
	  delWaveBtn.setEnabled(enable);
	  
	}

	/* ------------------------------------------------------------- */
	/*                        END G4P GUI                            */
	/* ------------------------------------------------------------- */
	
	
	
	
	//-----------------------------------------------------------//
	/* ----->>>           BEGIN JSON FILE I/O           <<<----- */
	//-----------------------------------------------------------//
	

	// select a file of WaveData objects in JSON format to open
	public void loadWaveData() {
		File folderToStartFrom = new File(dataPath("") + jsonFolder + "//*.json");
		selectInput("Select a file to open", "fileSelectedOpen", folderToStartFrom);
	}

	public void fileSelectedOpen(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
			isAnimating = oldIsAnimating;
			return;
		}
		currentDataFile = selection;
		println("User selected " + selection.getAbsolutePath());
		currentFileName = selection.getAbsolutePath();
		json = loadJSONObject(currentFileName);
		boolean goodHeader = checkJSONHeader(json, "PXAU", "WSYN");
		if (goodHeader) {
			println("--->> JSON file contains WaveSynthEditor data. It should load correctly.");
		}
		else {
			println("--->> JSON file may not contain WaveSynthEditor data. Will try to load,anyhow.");
		}
		setWaveSynthFromJSON(json, wavesynth);
		surface.setTitle(currentFileName);
		isAnimating = oldIsAnimating;
	}
	
	boolean checkJSONHeader(JSONObject json, String key, String val) {
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
	public void setWaveSynthFromJSON(JSONObject json, WaveSynth synth) {
		// set animation globals and WaveSynth properties
		animSteps = (json.isNull("steps")) ? 240 : json.getInt("steps");
		synth.setAnimSteps(animSteps);
		animStop = (json.isNull("stop")) ? this.animSteps : json.getInt("stop");
		synth.setStop(animStop);
		float myGamma = (json.isNull("gamma")) ? 1.0f : json.getFloat("gamma");
		synth.setGamma(myGamma);
		comments = (json.isNull("comments")) ? "" : json.getString("comments");
		synth.setComments(comments);
		synth.setGain(json.isNull("blendFactor") ? 0.5f : json.getFloat("blendFactor"));
		synth.setVideoFilename((json.isNull("filename")) ? "wavesynth.mp4" : json.getString("filename"));
		this.videoFilename = synth.getVideoFilename();
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
		currentWD = wavesynth.waveDataList.get(0);
		waveDataIndex = 0;
		synth.prepareAnimation();
		synth.renderFrame(0);
		loadGlobalPanelValues();
		loadWaveDataPanelValues(currentWD);
		printWaveData(synth);
	}

	/**
	 * Outputs current wavesynth settings and WaveData list.
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
	
	public void saveWaveData() {
		if ((currentDataFile == null) || (currentDataFile.getAbsolutePath().equals(""))) {
			selectOutput("Select a file to write to:", "fileSelectedWrite");
		}
		else {
			selectOutput("Select a file to write to:", "fileSelectedWrite", currentDataFile);
		}
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
		stateData.setJSONObject("header", getWaveSynthJSONHeader());
		// stateData.setString("PXAU", "WSYN");
		stateData.setInt("steps", synth.animSteps);
		stateData.setInt("stop", animStop);
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
	
	public JSONObject getWaveSynthJSONHeader() {
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
	
	//-----------------------------------------------------------//
	/* ----->>>      BEGIN IMAGE AND AUDIO FILE I/O     <<<----- */
	//-----------------------------------------------------------//


	public void saveToImage() {
	    // File folderToStartFrom = new File(dataPath(""));
	    selectOutput("Select an image file to write to:", "imageFileSelectedWrite");
	}
	public void imageFileSelectedWrite(File selection) {
	    if (selection == null) {
	        println("Window was closed or the user hit cancel.");
	        return;            
	    }
	    String fileName = selection.getAbsolutePath();
	    if (selection.getName().indexOf(".png") != selection.getName().length() - 4) {
	        fileName += ".png";
	    }
	    // saveImageToFile(mapImage, fileName);
	    save(fileName);
	}

	public void saveImageToFile(PImage img, String fileName) {
	    img.save(fileName);
	}

	/**
	 * Saves audio data to 16-bit integer PCM format, which Processing can also open.
	 * 
	 * @param samples            an array of floats in the audio range (-1.0f, 1.0f)
	 * @param sampleRate        audio sample rate for the file
	 * @param fileName            name of the file to save to
	 * @throws IOException        an Exception you'll need to catch to call this method (see keyPressed entry for 's')
	 * @throws UnsupportedAudioFileException        another Exception (see keyPressed entry for 's')
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
	    println("----- saved WaveSynth data as an audio file: "+ outFile.getAbsolutePath());
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
	  this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
	  this.setAudioGain(outputGain);
	  this.audioBuffer = new MultiChannelBuffer(mapSize, 1);
	  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
	  adsr = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
	  this.renderSignal();
	  pool = new PASamplerInstrumentPool(audioBuffer, sampleRate, 8, 1, audioOut, adsr);
	  timeLocsArray = new ArrayList<TimedLocation>();     // initialize mouse event tracking array
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

	/**
	 * Calls WaveSynth to render a audio sample array derived from the same math that creates the image,
	 * then loads the derived audio data to audioBuffer, ready to be played.
	 */
	public void renderSignal() {
	  this.audioSignal = wavesynth.renderAudioRaw(step);           // get the signal "as is" from WaveSynth
	  audioSignal = WaveSynth.normalize(audioSignal, 0.9f);        // normalize samples to the range (-0.9f, 0.9f) 
	  audioLength = audioSignal.length;
	  audioBuffer.setBufferSize(audioLength);
	  audioBuffer.setChannel(0, audioSignal);                      // copy audioSignal to channel 0 of audioBuffer
	  // println("--->> copied audio signal to audio buffer");
	}

	// Called by mousePressed(), this should be a bottleneck method for all playSample() calls.
	public void audioMousePressed(int sampleX, int sampleY) {
	  this.sampleX = sampleX;
	  this.sampleY = sampleY;
	  samplePos = mapper.lookupSignalPos(sampleX, sampleY);
	  checkBufferState();
	  playSample(samplePos, calcSampleLen(), 0.6f, adsr);
	}

	public void checkBufferState() {
	  if (isBufferStale) {
	    println("--->> Stale buffer refreshed ");
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
	        tl.setStale(tl.eventTime() < currentTime);
	        if (!tl.isStale()) {
	            drawCircle(tl.getX(), tl.getY());
	        }
	    });
	    timeLocsArray.removeIf(TimedLocation::isStale);
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

	
}
