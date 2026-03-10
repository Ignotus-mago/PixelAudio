/*
 * This example application modifies the TutorialOne_04_Drawing code for the PixelAudio
 * library for Processing. Some of the changes:
 * 
 *   -- We use a windowed buffer to traverse all of the data in an audio file. The data
 *      is stored in its own buffer, anthemBuffer, distinct from playBuffer, which contains
 *      only the audio data corresponding to the display image.
 *   -- We use a custom MultiGen, hilbertRowOrtho(). This gen uses rows of Hilbert gens
 *      in row major order (left to right, top to bottom). Its signal path is revealed
 *      by using hues from the "colors" RGB array to color the display image. This gen
 *      is useful for revealing the order of data in a file, particularly if we stream
 *      it in at audio rate. 
 *      If you draw a line along one of the rows in the image dragging the mouse from
 *      left to right, you will sample the audio in "chronological" order, going 
 *      forwards through the file. If you draw a line along a row from right to left, 
 *      you are in effect playing audio back in "reverse chronological" order, goinf
 *      backwards through the file. You can only play as much of the file as appears,
 *      transcoded to pixels, in the display window. Bigger windows will expose more of
 *      the file. Smaller windows will refresh the diaplay faster when you are stepping
 *      through the audio buffer with the WindowedBuffer window. 
 *   -- We load audio and image separately. You can use key commands 'w' (image to audio) 
 *      and 'W' (audio to image) to exchange data between image and audio, but they only
 *      apply to the data corresponding to the current image and its corresponding
 *      audio buffer. 
 *   -- We add a raindrops() method to provide random audio events when the windowed 
 *      buffer is advancing automatically ('t' command). The raindrops cluster along the
 *      top edge and are useful for playing files with a recognizable time structure, like
 *      youthOrchestra.wav, the Spanish national anthem. 
 * 
 * Finally, we add methods to open a selected audio file and load all of it to a buffer. 
 * We use the buffer as a source for "audioSignal", an array of floats that is mapped to the 
 * display using the signal path. You could think of audioSignal as a window that moves 
 * through the buffer, transcoding audio data to brightness in the HSB color space and
 * then writing its values to the PImage "mapImage", which shows in the display window. 
 * The audioSignal array is the same size as mapImage, which is the display image. Signal 
 * and image use the PixelAudioMapper "mapper" to map samples onto pixels. Mapper was
 * initialized with the MultiGen created by hiilbertRowOrtho(). 
 * 
 * Note that we aren't streaming a file from disk and playing it. We're moving through 
 * a buffer in memory. We've loaded the entire file into the buffer and then assigned
 * the buffer to instances of our audio sampling instrument PASamplerInstrumentPool. 
 * We play audio by drawing on the display image. We step through audio with 
 * WindowedBuffer. It provides better control over animation than the built-in
 * array rotation methods in PixelAudioMapper do, and it's a complete implementation 
 * of a circular buffer. 
 * 
 * The audio loaded to WindowedBuffer windowBuff does not change once it is loaded, 
 * until you load a new audio file. Data from the windowed buffer gets written to audioSignal
 * and transcoded to mapImage. When you click on the display window, the getSamplePos()
 * method calculates the precise index of a sample in the backing buffer of the 
 * windowed buffer, which contains the entire audio file that you loaded. The index
 * gets passed to the WFSamplerInstrumentPool instance "pool", which plays the audio
 * at the sample position. GetSamplePos() takes animation into account, too, if you are
 * using PixelAudioMapper's shift methods. 
 * 
 * The current iteration of the instruments in net.paulhertz.pixelaudio.sampler can, 
 * in theory, play a great many samples from a single instance of PASamplerInstrumentPool
 * of PASamplerInstrument. I'm still experimenting with how many duplicated buffers are
 * optimal for real-time performance. The number of duplicate buffers is set with:
 * 
 *     int poolSize = 16;              // number of WFSamplerInstruments in the pool
 *     int perInstrumentVoices = 4;    // number of voices for each WFSamplerInstrument
 *     
 * Vary these numbers to experiment. In theory, poolSize = 1 and perInstrumentVoices = 64
 * should work as well as the current settings. It certainly works. The output signal
 * may be more susceptible to clipping and phasing distortion -- I'm still trying to 
 * determine that myself. 
 * 
 * 
 * Here are some of the ways I am using WindowedBuffer: 
 *   -- In the loadAudioFile() method, we open a file and load all of it into playBuffer.
 *   -- The audio data is copied to anthemBuffer.
 *   -- anthemBuffer is used to initialize a WindowedBuffer, windowBuff, with a mapSize window.
 *   -- playBuffer is trimmed to mapSize, the size of PixelAudioMapper mapper and the display image.
 *   -- Animation and transcoding image to audio use mapImage.pixels, audioSignal, and playBuffer.
 *   -- Audio events make use of anthemBuffer.
 *   -- You can step through the buffer either with the 't' command or the 'j' and 'J' commands. 
 *      The 't' command automates the window, shifting it on every frame
 *      The 'j' and 'J' commands step forward a half window or a whole window
 *   -- Generally, you can't run animation and windowing at the same time. 
 *   -- Audio events use anthemBuffer as a stable source of audio data, thus avoiding noise
 *      from threaded updates to audio data. See audioMousePressed() for the code. 
 *      The key bit of math is:
 *        position in anthemBuffer = position in playBuffer + this.windowBuff.getIndex();
 *   -- Animation and window shifting are turned off when you open a file, and returned 
 *      to previous settings after the file is loaded. This avoids thread conflicts and 
 *      crashes. Loading files automatically from code, without a dialog, can work around
 *      this restriction, but it's left as an exercise for the diligent coder. 
 *      
 * 
 * Comments on the individual methods indicate where some of the changes were made.
 * 
 *     1. Add hilbertRowOrtho().
 *     2. Modify loadAudioFile() and loadImageFile(), removing the writeAudioToImage()
 *        and writeImageToAudio() calls.
 *     3. Add key commands in parseKey() for writeAudioToImage() ('W')and writeImageToAudio() ('w').
 *     4. Add key commands in parseKey() to save to audio ('S') and save to image ('s'). 
 *     5. Create methods playPoints(int startTime) and playBrushStrokes(int offset) 
 *        to automate playback of brushstrokes. See DrawingTools tab.
 *     6. Add a key command ('p') to parseKey() to call playBrushStrokes(). 
 *     7. Modify the updateAudio() method: comment out the writeAudioToImage() call,
 *        because we will handle audio at full resolution whenever possible. 
 *        Rotate the audioSignal array when isAnimating == true.
 *     8. Add a section of WindowedBuffer variables.
 *     9. Add methods to play, mute and pause audio from WindowedBuffer. 
 *        
 *     
 * To play with the code:
 *     
 *     1. Launch the sketch. 
 *        Rainbow stripes appear in display window.
 *     2. Press 'o' and load an audio file to the audio buffer (saucer_mixdown.wav, in the 
 *        PixelAudio/examples/examples_data/ folder is a good choice).
 *        Display does not change, Click on the display image to test the audio. 
 *     3. Press 't' to step through audio from the selected file and write to the display. WindowBuff
 *        steps through anthemBuffer by windowHopSize samples and returns an array of floats for 
 *        every call to its nextWindow() method. WindowBuff passes the array of floats to audioSignal,  
 *        which gets transcoded to mapImage and the display as animated gray values.
*         See the WindowedBuffer class for more information.  
 *     4. Unlike the AudioCapture example, which can play a file automatically, MusicBoxBuffer
 *        requires mouseClicks or interactive drawing to produce sound.  
 *     5. Click on the image to play samples. Try it while the window is stepping through 
 *        the buffer ('t' key command) and while it is animating (spacebar key command). To avoid
 *        problems, it is generally not possible to animate and step the window at the same time.  
 *     6. Press 'd' to start drawing -- draw a few brushstrokes. Hover on brushstrokes to activate 
 *        them--this will work even when drawing is off. Click on brushstrokes to play audio. 
 *        Audio samples will play, activated by the brushstrokes. 
 *     7. Press 'x' to delete the current active brush shape or the oldest brush shape.
 *        Press 'X' to delete the most recent brush shape.
 *     7. Press 'b' to play the brushstrokes.
 *        Audio corresponding to brushstrokes is played.
 *     8. Press the spacebar to run animation.
 *        The image pixel array and the audio signal array are both rotated and written along
 *        the signal path. 
 *        The audio played by the brushstrokes changes as the animation progresses.
 *     9. Press 't' to start windowed buffer traversal again. Experiment with different audio sources, 
 *        directions of drawing, etc. 
 * 
 * You can change the hilbertRowOrtho() MultiGen method arguments to fit other other audio and image files. 
 * 
 * 
 * Here are the key commands for this sketch:
 * 
 * Press ' ' to  start or stop animation.
 * Press 'd' to turn drawing on or off.
 * Press 'c' to apply color from image file to display image.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage .
 * Press 'p' to play brushstrokes with a time lapse between each.
 * Press 'f' to toggle granular envelope and timing.
 * Press 'o' or 'O' to open an audio or image file.
 * Press 's' to save image to a PNG file.
 * Press 'S' to save audio to a WAV file.
 * Press ']' to jump half window ahead in the buffer.
 * Press '[' to jump half window back in the buffer.
 * Press '}' to jump whole window ahead in the buffer.
 * Press '{' to jump whole window back in the buffer.
 * Press 'r' to rewind the windowed audio buffer.
 * Press UP arrow to increase gain by 3.0 dB.
 * Press DOWN arrow to decrease gain by 3.0 dB.
 * Press 'u' to mute audio.
 * Press 't' to turn stream capture on or off.
 * Press 'w' to write the image colors to the audio buffer as transcoded values.
 * Press 'W' to write the audio buffer samples to the image as color values.
 * Press 'x' to delete the current active brush shape or the oldest brush shape.
 * Press 'X' to delete the most recent brush shape.
 * Press 'y' to turn rain (random audio events) on and off.
 * Press 'z' to reset brushstrokes and audio buffer (you may need to reload audio).
 * Press 'V' to record a video.
 * Press 'h' or 'H' to show help and key commands.
 * 
 */


package net.paulhertz.pixelaudio.example;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

//Mama's ever-lovin' blue-eyed PixelAudio library
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;
import net.paulhertz.pixelaudio.curves.*;
import net.paulhertz.pixelaudio.sampler.*;
import net.paulhertz.pixelaudio.schedule.*;
//audio library
import ddf.minim.*;
import ddf.minim.ugens.*;

//video export library
import com.hamoid.*;


public class TutorialOne_06_WindowBuffer extends PApplet {
	// PixelAudio vars and objects
	PixelAudio pixelaudio;     // our shiny new library
	MultiGen multigen;         // a PixelMapGen that links together multiple PixelMapGens
	int genWidth = 64;         // width of multigen PixelMapGens
	int genHeight = 64;        // height of  multigen PixelMapGens
	int rows = 8;
	int cols = 12;
	PixelAudioMapper mapper;   // object for reading, writing, and transcoding audio and image data
	int mapSize;               // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
	PImage mapImage;           // image for display
	PixelAudioMapper.ChannelNames chan = ChannelNames.L;
	int[] colors;              // array of spectral colors

	/* ------------------------------------------------------------------ */
	/*                        FILE I/O VARIABLES                          */
	/* ------------------------------------------------------------------ */

	// audio file
	File audioFile;
	String audioFilePath;
	String audioFileName;
	String audioFileTag;
	int audioFileLength;
	// image file
	File imageFile;
	String imageFilePath;
	String imageFileName;
	String imageFileTag;
	int imageFileWidth;
	int imageFileHeight;
	
	String daPath;                  // system-specific path to example files data


	/* ------------------------------------------------------------------ */
	/*                          AUDIO VARIABLES                           */
	/* ------------------------------------------------------------------ */

	/*
	 * Audio playback support is added with the audio variables and audio methods. 
	 * You will also need the WFSamplerInstrument and TimedLocation classes. In setup(), 
	 * call initAudio(), then add a mousePressed() method that calls 
	 * audioMousePressed(mouseX, mouseY). AudioMousePressed() posts timed audio 
	 * events to an event list. Call handleDrawing() in the draw() loop to run events. 
	 * It calls runPointEvents() and runCurveEvents(), which traverse event lists for 
	 * mouse clicks and for curve drawing. 
	 */
	/** Minim audio library */
	Minim minim;                    // library that handles audio 
	AudioOutput audioOut;           // line out to sound hardware
	boolean isBufferStale = false;  // flags that audioBuffer needs to be refreshed or reset
	float sampleRate = 48000;       // sample rate of audioOut
	float fileSampleRate;           // sample rate of most recently opened file
	float[] audioSignal;            // the audio signal as an array of floats
	MultiChannelBuffer playBuffer;  // a data structure allocated for storing and playing the audio signal
	int samplePos;                  // an index into the audio signal, can be set by a mouse click on the display image
	int audioLength;                // length of the audioSignal, same as the number of pixels in the display image

	// SampleInstrument setup
	int noteDuration = 1000;        // average sample synth note duration, milliseconds
	int samplelen;                  // calculated sample synth note length, samples
	float defaultGain = 0.8f;
	float gain = defaultGain;
	// instrument pool, 16 WFSamplerInstruments with 4 voices each, for up to 64 simultaneous voices
    PASamplerInstrumentPool pool;   // a pool of WFSamplerInstruments
    int poolSize = 8;              // number of WFSamplerInstruments in the pool
    int perInstrumentVoices = 8;    // number of voices for each WFSamplerInstrument
	// ADSR and its parameters
	ADSRParams defaultEnv;			// good old attack, decay, sustain, release
	ADSRParams granularEnv;         // envelope for a granular-style series of samples
	float maxAmplitude = 0.75f;     // 0..1
	float attackTime = 0.4f;        // seconds
	float decayTime = 0.0f;         // seconds, no decay
	float sustainLevel = 0.75f;     // 0..1, same as maxAmplitude
	float releaseTime = 0.4f;       // seconds, same as attack
	float pitchScaling = 1.0f;      // factor for changing pitch
	float defaultPitchScaling = 1.0f;
	float lowPitchScaling = 0.5f;
	float highPitchScaling = 2.0f;
    boolean isMuted = false;         // flag for muting audioOut

	// interaction variables for audio
	int sampleX;                    // keep track of coordinates associated with audio samples
	int sampleY;
	boolean isIgnoreOutsideBounds = true;      // set to true to ignore points outside bounds when drawing
	ArrayList<TimedLocation> timeLocsArray;    // a list of timed events 

	boolean isLoadToBoth = true;    // if true, load newly opened file both to audio and to video
	boolean isGranular = false;     // if true, sets all envelopes to the same size, with 50% overlap when playing
	int grainDuration = 120;        // granular envelope duration in milliseconds


	/* ------------------------------------------------------------------ */
	/*                   ANIMATION AND VIDEO VARIABLES                    */
	/* ------------------------------------------------------------------ */

	int shift = -4;                  // number of pixels to shift the animation
	int totalShift = 0;              // cumulative shift
	boolean isAnimating = false;     // are we animating or not?
	boolean oldIsAnimating;          // keep track of state when we suspend animation
	boolean isTrackMouse = false;    // if true, dragging the mouse sets animation speed
	// animation variables
	int animSteps = 720;             // how many steps in an animation loop
	boolean isRecordingVideo = false;    // are we recording? (only if we are animating)
	int videoFrameRate = 24;         // fps, frames per second
	int step;                        // number of current step in animation loop
	VideoExport videx;               // hamoid library class for video export (requires ffmpeg)
	  
	/* ------------------------------------------------------------------ */
	/*                         DRAWING VARIABLES                          */
	/* ------------------------------------------------------------------ */

	/*
	 * Drawing uses classes in the package net.paulhertz.pixelaudio.curves to store drawing data
	 * and draw Bezier curves and lines. It also provides very basic brushstroke modeling code. 
	 * Unlike most of the code in PixelAudio, which avoids dependencies on Processing, the 
	 * curves.* classes interface with Processing to draw to PApplets and PGraphics instances. 
	 * See the PACurveMaker class for details of how drawing works. 
	 * 
	 */

	// curve drawing and interaction
	public boolean isDrawMode = false;                  // is drawing on or not?
	public float epsilon = 4.0f;                        // controls how much reduction is applied to points
	public ArrayList<PVector> allPoints;                // all the points the user drew, thinnned
	public int dragColor = color(233, 199, 89, 128);    // color for initial drawing 
	public float dragWeight = 8.0f;                     // weight (brush diameter) of initial line drawing
	public int startTime;                               // start time for user drawing event
	public ArrayList<Integer> allTimes;                 // list for tracking user drawing times, for future use
	public PVector currentPoint;                        // most recent point in user drawing
	public int polySteps = 4;                           // number of steps in polygon representation of a Bezier curve
	public int defaultPolySteps = polySteps;
	public int granPolySteps = 16;
	public PACurveMaker curveMaker;                     // class for tracking and storing drawing data
	public ArrayList<PVector> eventPoints;              // list of points stored in or loaded from a PACurveMaker
	public ListIterator<PVector> eventPointsIter;       // iterator for eventPoints
	int eventStep = 90;                                 // milliseconds between events
	public ArrayList<TimedLocation> curveTLEvents;      // a list of TimedLocation instances 
	public ArrayList<PACurveMaker> brushShapesList;     // a list of PACurveMaker instances with recorded drawing data
	public PACurveMaker activeBrush;                    // the currently active PACurveMaker, collecting points as user drags the mouse
	public int activeIndex = 0;                         // index of current brush in brushShapesList, useful for UDP/OSC messages
	int newBrushColor = color(144, 34, 42, 128);        // color of the new brushstroke
	int polyPointsColor = color(233, 199, 144, 192);    // color for polygon representation of Bezier curve associated with a brushstroke
	int activeBrushColor = color(144, 89, 55, 128);     // color for the active brush
	int readyBrushColor = color(34, 89, 55, 96);        // color for a brushstroke when ready to be clicked

	float blend = 0.5f;                                 // TODO implement blending of images
	boolean isBlending = true;                          // for future use

	/* end drawing variables */
	
	/* ------------------------------------------------------------------ */
	/*                     WINDOWED BUFFER VARIABLES                      */
	/* ------------------------------------------------------------------ */
	
	/** keep track of the frame we should be rendering */
	int frame = 0;
	/** A windowed buffer for anthem */
	WindowedBuffer windowBuff;
	/** how far to step the window on each frame */
	int windowHopSize = 64;
	/** buffer for complete file */
	MultiChannelBuffer anthemBuffer;
	/** A source for streaming audio from a file */
	float[] anthemSignal;
	/** boolean to flag audio capture */
	boolean isListening = false;
	/** save isListening state */
	boolean oldIsListening = isListening;
	/** boolean to flag fixed length capture */
	boolean isFixedLength = false;
	/**  */
	boolean isRaining = false;

	// ** YOUR VARIABLES ** Variables for YOUR CLASS may go here **  //
	

	 
	/**
	 * Entry point for pure Java implementation (in Eclipse). Omit in Processsing. 
	 * @param args    not used
	 */
	public static void main(String[] args) {
		PApplet.main(new String[] { TutorialOne_06_WindowBuffer.class.getName() });
	}

    @Override
	public void settings() {
	  size(cols * genWidth, rows * genHeight);
	}

    @Override
	public void setup() {
	  surface.setTitle("Music Box Buffer Example");
	  frameRate(24);
	  // initialize our library
	  pixelaudio = new PixelAudio(this);
	  // we create a PixelMapGen implemented as a MultiGen, with dimensions equal to our display window
	  // hilbertRowOrtho() is a very flexible way to create a multigen. 
	  // hilbertLoop3x2() produces a MultiGen that is 3 * genWidth x 2 * genHeight, where
	  // genWidth == genHeight and genWidth is a power of 2 (a restriction on Hilbert curves)
	  //
	  // multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);  // for example
	  // 
	  multigen = hilbertRowOrtho(cols, rows, genWidth, genHeight);
	  // create a PixelAudioMapper to handle the mapping of pixel colors to audio samples
	  mapper = new PixelAudioMapper(multigen);
	  // keep track of the area of the PixelAudioMapper
	  mapSize = mapper.getSize();
	  println("---- mapSize == "+ mapSize);
	  // create an array of rainbow colors with mapSize elements
	  colors = getColors(mapSize);
	  initImages();
	  initAudio();
	  initDrawing();
	  showHelp();
	  println("--- dimensions: ", width, height);
	}

    @Override
    public void stop() {
        if (pool != null) pool.close();
        if (minim != null) minim.stop();
        super.stop();
    }


	/**
	 * Generates an array of rainbow colors using the HSB color space.
	 * @param size    the number of entries in the colors array
	 * @return an array of RGB colors ordered by hue
	 */
	public int[] getColors(int size) {
	  int[] colorWheel = new int[size]; // an array for our colors
	  pushStyle(); // save styles
	  colorMode(HSB, colorWheel.length, 100, 100); // pop over to the HSB color space and give hue a very wide range
	  for (int i = 0; i < colorWheel.length; i++) {
	    colorWheel[i] = color(i, 50, 70); // fill our array with colors, gradually changing hue
	  }
	  popStyle(); // restore styles, including the default RGB color space
	  return colorWheel;
	}

	
	/**
	 * This method creates rows of HilbertGens, starting each row from the left
	 * and adding gens. The odd rows are flipped vertically and the even rows are
	 * unchanged. The unchanged HilbertGen starts at upper left corner and ends at 
	 * upper right corner, so this provides some possibilities of symmetry between rows.
	 * The path is not continuous. 
	 * 
	 * @param cols    number of columns of gens wide
	 * @param rows    number of rows of gens high
	 * @param genW    width of each gen (same as genH and a power of 2)
	 * @param genH    height of each gen 
	 * @return        a MultiGen composed of cols * rows PixelMapGens
	 */
	public MultiGen hilbertRowOrtho(int cols, int rows, int genW, int genH) {
	    // list of PixelMapGens
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>();
	  for (int y = 0; y < rows; y++) {
	    for (int x = 0; x < cols; x++) {
	      if (y % 2 == 0) {
	        genList.add(new HilbertGen(genW, genH, AffineTransformType.NADA));
	      }
	      else {
	        genList.add(new HilbertGen(genW, genH,  AffineTransformType.NADA));
	      }
	      offsetList.add(new int[] {x * genW, y * genH});
	    }
	  }
	  return new MultiGen(width, height, offsetList, genList);
	}


	/**
	 * Initializes mapImage with the colors array. MapImage will handle the color data for mapper
	 * and also serve as our display image.
	 */
	public void initImages() {
	  mapImage = createImage(width, height, ARGB);
	  mapImage.loadPixels();
	  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
	  mapImage.updatePixels();
	}	  
	
	/**
	 * Initializes the line, curve, and brushstroke drawing variables. 
	 * Note that timeLocsArray has been initialized by initAudio(), though it
	 * will be used for point events in the drawing code, too. 
	 */
	public void initDrawing() {
	  currentPoint = new PVector(-1, -1);
	  brushShapesList = new ArrayList<PACurveMaker>();
	}
	
	// ONLY IN PROCESSING, though it's possible in Eclipse
	public void preloadFiles(String path) {
		daPath = path;
		// the audio file we want to open on startup
		File audioSource = new File(daPath +"Saucer_mixdown.wav");
		// load the file into audio buffer and Brightness channel of display image (mapImage)
		fileSelected(audioSource);
		// overlay colors on mapImage
		mapImage.loadPixels();
		applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
		mapImage.updatePixels();  
	}
	  
	public void draw() {
		image(mapImage, 0, 0);    // draw mapImage to the display window
		handleDrawing();          // handle interactive drawing and audio events created by drawing
		if (isAnimating) {        // animation is different from windowed buffer
			animate();              // rotate mapImage.pixels by shift number of pixels
			updateAudio();          // rotate audioSignal (we could just shift the index...)
		}
		if (isListening) {        // windowed buffer, never at the same time as isAnimating
			updateAudio();        // update audioSignal with WindowedBuffer.nextWindow()
			drawSignal();         // transcode audioSignal and write it to mapImage.pixels
			if (isRaining && random(10) > 9) raindrops();    // a little rain never hurt anyone
		}
	}
	
	  
	 public void animate() {
	   stepAnimation();
	   renderFrame(step);
	 }
	 
	  
	/**
	 * Step through the animation, called by the draw() method.
	 * Will also record a frame of video, if we're recording.
	 */
	public void stepAnimation() {
	  if (step >= animSteps) {
	    if (isRecordingVideo) {
	      isRecordingVideo = false;
	      videx.endMovie();
	      isAnimating = oldIsAnimating;
	      println("--- Completed video at frame " + animSteps);
	    }
	    step = 0;
	  } 
	  else {
	    step += 1;
	    if (isRecordingVideo) {
	      if (videx == null) {
	        println("----->>> start video recording ");
	        videx = new VideoExport(this, "TutorialOneVideo.mp4");
	        videx.setFrameRate(videoFrameRate);
	        videx.startMovie();
	      }
	      videx.saveFrame();
	      println("-- video recording frame " + step + " of " + animSteps);
	    }
	  }
	}
	

	/**
	 * Renders a frame of animation: moving along the signal path, copies mapImage pixels into rgbSignal, 
	 * rotates them shift elements left, writes them back to mapImage along the signal path.
	 * 
	 * @param step   current animation step
	 */
	public void renderFrame(int step) {
		mapImage.loadPixels();
		// get the pixels in the order that the signal path visits them
		int[] rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
		// rotate the pixel array
		PixelAudioMapper.rotateLeft(rgbSignal, shift);
		// keep track of how much the pixel array (and the audio array) are shifted
		totalShift += shift;
		// write the pixels in rgbSignal to mapImage, following the signal path
		mapper.plantPixels(rgbSignal, mapImage.pixels, 0, mapSize);
		mapImage.updatePixels();
	}
	
	
	/**
	 * Transcode directly from audioSignal to mapImage.pixels, the display image.
	 * When we are stepping through an audio buffer with BufferedWindow, audioSignal
	 * contains the most recent window, which is exactly the same size as mapImage.pixels.
	 * By default, we transcode audio to the Brightness channel of HSB, preserving Hue 
	 * and Saturation in the mapImage. 
	 */
	public void drawSignal() {
		// we transcode directly from audioSignal to mapImage.pixels, with no intermediate steps or copies
		mapImage.loadPixels();
		mapper.mapSigToImg(audioSignal, mapImage.pixels, chan);
		mapImage.updatePixels();
	}  

	  
	/**
	 * Updates global variable audioSignal, either by rotating array or getting 
	 * a new window of values from the audio buffer.
	 */
	public void updateAudio() {
		if (isListening) {
			audioSignal = windowBuff.nextWindow();
			return;    // don't animate if we're moving through the windowed buffer
		}
		if (isAnimating) {
			PixelAudioMapper.rotateLeft(audioSignal, shift);
		}
	}
	  
	
	/**
	 * Handles user's drawing actions, draws previously recorded brushstrokes, 
	 * tracks and generates animation and audio events. 
	 */
	public void handleDrawing() {	
		// draw current brushShapes 
		drawBrushShapes();
		if (isDrawMode) {
			if (mousePressed) {
				addPoint(mouseX, mouseY);
			}
			if (allPoints != null && allPoints.size() > 2) {
				PACurveUtility.lineDraw(this, allPoints, dragColor, dragWeight);
			}
			if (curveMaker != null) curveMakerDraw();
		} 
		runCurveEvents();
		runPointEvents();
	}
	

	/**
	 * Displays a line of text to the screen, usually in the draw loop. Handy for debugging.
	 * typical call: writeToScreen("When does the mind stop and the world begin?", 64, 1000, 24, true);
	 * 
	 * @param msg     message to write
	 * @param x       x coordinate
	 * @param y       y coordinate
	 * @param weight  font weight
	 * @param isWhite if true, white text, otherwise, black text
	 */
	public void writeToScreen(String msg, int x, int y, int weight, boolean isWhite) {
		int fill1 = isWhite? 0 : 255;
		int fill2 = isWhite? 255 : 0;
		pushStyle();
		textSize(weight);
		float tw = textWidth(msg);
		int pad = 4;
		fill(fill1);
		rect(x - pad, y - pad - weight, x + tw + pad, y + weight/2 + pad);
		fill(fill2);
		text(msg, x, y);
		popStyle();
	}

	/**
	 * The built-in mousePressed handler for Processing, but note that it forwards 
	 * mouse coords to handleMousePressed(). If isDrawMode is true, we start accumulating
	 * points to allPoints: initAllPoints() adds the current mouseX and mouseY. After that, 
	 * the draw loop calls handleDrawing() to add points. Drawing ends on mouseReleased(). 
	 */
	public void mousePressed() {
		setSampleVars(mouseX, mouseY);			
		if (this.isDrawMode) {
			initAllPoints();
		} 
		else {
			handleMousePressed(mouseX, mouseY);
		}
	}
	
    /**
     * Used for interactively setting the amount of pixel array shift when animating.
     * TODO, since this app is a demo for WindowedBuffer, we can probably do without 
     * setting shift interactively. 
     */
    @Override
	public void mouseDragged() {
		if (isTrackMouse) {
			shift = abs(width/2 - mouseX);
			if (mouseY < height/2) 
				shift = -shift;
			writeToScreen("shift = "+ shift, 16, 24, 24, false);
		}
	}
	
    public void mouseReleased() {
    	setSampleVars(mouseX, mouseY);			
    	if (isAnimating && isTrackMouse) {
    		// ignore isTrackMouse in this sketch
    		// println("----- animation shift = "+ shift);
    	}
    	if (isDrawMode && allPoints != null) {
    		if (allPoints.size() > 2) {		// add curve data to the brush list
    			initCurveMaker();
    		}
    		else {							// handle the event as a click
    			handleMousePressed(mouseX, mouseY);
    		}
    		allPoints.clear();
    	}
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

			}
			else if (keyCode == LEFT) {

			}
    	}
    }

	
    /**
     * Handles key press events passed on by the built-in keyPressed method. 
     * By moving key event handling outside the built-in keyPressed method, 
     * we make it possible to post key commands without an actual key event.
     * Methods and interfaces and even other threads can call parseKey(). 
     * This opens up many possibilities and a some dangers, too. 
     * @see TutorialOne_04_UDP.java (Eclipse) or TutorialOne_05_UDP (Processing)
     * for an example of external calls to parseKey().
     * 
     * @param key
     * @param keyCode
     */
    public void parseKey(char key, int keyCode) {
    	switch(key) {
    	case ' ': //  start or stop animation
    		if (isListening) {
    			println("-- animation is not run when you moving through the windowed buffer");			
    		}
    		else {
    			isAnimating = !isAnimating;
    			println("-- animation is " + isAnimating);
    		}
    		break;
    	case 'd': // turn drawing on or off
    		// turn off mouse tracking that sets shift value for animation
    		isTrackMouse = false; 
    		// turn off animation (you can try drawing with it on, just press the spacebar)
    		isAnimating = false;
    		isDrawMode = !isDrawMode;
    		println(isDrawMode ? "----- Drawing is turned on" : "----- Drawing is turned off");
    		if (!isDrawMode)
    			this.curveMaker = null;
    		break;
    		//	  case 'm': // turn mouse tracking on or off, a distraction better omitted
    		//	    isTrackMouse = !isTrackMouse;
    		//	    println("-- mouse tracking is " + isTrackMouse);
    		//	    break;
    	case 'c': // apply color from image file to display image
    		chooseColorImage();
    		break;
    	case 'k': // apply the hue and saturation in the colors array to mapImage 
    		mapImage.loadPixels();
    		applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
    		mapImage.updatePixels();
    		break;
    	case 'p': // play brushstrokes with a time lapse between each
    		playBrushstrokes(2000);
    		break;
    	case 'f': // toggle granular envelope and timing
    		isGranular = !isGranular;
    		println("-- isGranular is "+ isGranular);
    		break;
    	case 'o': case 'O': // open an audio or image file
    		chooseFile();
    		break;
    	case 's': // save image to a PNG file
    		saveToImage();
    		break;
    	case 'S': // save audio to a WAV file
    		saveToAudio();
    		break;
    	case ']': // jump half window ahead in the buffer
    		moveAudioWindow(windowBuff.getWindowSize()/2);
    		drawSignal();
    		break;
    	case '[': // jump half window back in the buffer
    		moveAudioWindow(-windowBuff.getWindowSize()/2);
    		drawSignal();
    		break;
    	case '}': // jump whole window ahead in the buffer
    		moveAudioWindow(windowBuff.getWindowSize());
    		drawSignal();
    		break;
    	case '{': // jump whole window back in the buffer
    		moveAudioWindow(-windowBuff.getWindowSize());
    		drawSignal();
    		break;
    	case 'r': // rewind the windowed audio buffer
    		resetAudioWindow();
    		drawSignal();
    		break;
    	case 'u': // mute audio
    		isMuted = !isMuted;
    		if (isMuted) {
    			audioOut.mute();
    		}
    		else {
    			audioOut.unmute();
    		}
    		String msg = isMuted ? "muted" : "unmuted";
    		println("---- audio out is "+ msg);
    		break;
    	case 't': // turn stream capture on or off
    		if (anthemSignal != null) {
    			isListening = listenToAnthem(!isListening);
    			if (isListening) 
    				isAnimating = false;		// generally don't want to animate and stream at the same time
    			totalShift = 0;
    		}
    		else {
    			println("---- You need to load an audio file before you can window through the buffer.");
    		}
    		break;
    	case 'w': // write the image colors to the audio buffer as transcoded values
    		// prepare to copy image data to audio variables
    		// resize the buffer to mapSize, if necessary -- signal will not be overwritten
    		if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
    		audioSignal = playBuffer.getChannel(0);
    		writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
    		// now that the image data has been written to audioSignal, set playBuffer channel 0 to the new audio data
    		playBuffer.setChannel(0, audioSignal);
    		audioLength = audioSignal.length;
    		println("--->> Wrote image to audio as audio data.");
    		break;
    	case 'W': // write the audio buffer samples to the image as color values
    		writeAudioToImage(audioSignal, mapper, mapImage, chan);
    		println("--->> Wrote audio to image as pixel data.");
    		break;
    	case 'x': // delete the current active brush shape or the oldest brush shape
    		if (activeBrush != null) {
    			removeActiveBrush();
    		}
    		else {
    			removeOldestBrush();
    		}
    		break;
    	case 'X': // delete the most recent brush shape
    		removeNewestBrush();
    		break;
    	case 'y': // turn rain (random audio events) on and off
    		isRaining = !isRaining;
    		println("---- isRaining = "+ isRaining);
    		break;
    	case 'z': // reset brushstrokes and audio buffer (you may need to reload audio)
    		isListening = listenToAnthem(false);
    		reset(true);
    		break;
    	case 'V': // record a video
    		// records a complete video loop with following actions:
    		// Go to frame 0, turn recording on, turn animation on.
    		// This will record a complete video loop, from frame 0 to the
    		// stop frame value in the GUI control panel.
    		step = 0;
    		renderFrame(step);
    		isRecordingVideo = true;
    		oldIsAnimating = isAnimating;
    		isAnimating = true;
    	case 'h': case 'H': // show help and key commands
    		showHelp();
    		break;
    	default:
    		break;
    	}
    }

	/**
	 * Rewinds WindowedBuffer instance windowBuff to the beginning of the audio buffer.
	 */
	public void resetAudioWindow() {
		windowBuff.reset();
		audioSignal = windowBuff.nextWindow();
		playBuffer.setChannel(0, audioSignal);
		totalShift = 0;
	}

	/**
	 * Moves WindowedBuffer instance windowBuff's window to the index howFar.
	 */
	public void moveAudioWindow(int howFar) {
		int i = this.windowBuff.getIndex();
		audioSignal = windowBuff.gettWindowAtIndex(i + howFar);
		playBuffer.setChannel(0, audioSignal);
		// println("---->> audioSignal[0] = "+ audioSignal[0]);
		totalShift = 0;
	}

	/**
	 * Sets audioOut.gain.
	 * @param g   gain value for audioOut, in decibels
	 */
	public void setAudioGain(float g) {
		audioOut.setGain(g);
		gain = audioOut.getGain();
	}
	
	/**
	 * to generate help output, run RegEx search/replace on parseKey case lines with:
	 * // case ('.'): // (.+)
	 * // println(" * Press $1 to $2.");
	 */
	public void showHelp() {
		println(" * Press ' ' to  start or stop animation.");
		println(" * Press 'd' to turn drawing on or off.");
		println(" * Press 'c' to apply color from image file to display image.");
		println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage .");
		println(" * Press 'p' to play brushstrokes with a time lapse between each.");
		println(" * Press 'f' to toggle granular envelope and timing.");
		println(" * Press 'o' or 'O' to open an audio or image file.");
		println(" * Press 's' to save image to a PNG file.");
		println(" * Press 'S' to save audio to a WAV file.");
		println(" * Press ']' to jump half window ahead in the buffer.");
		println(" * Press '[' to jump half window back in the buffer.");
		println(" * Press '}' to jump whole window ahead in the buffer.");
		println(" * Press '{' to jump whole window back in the buffer.");
		println(" * Press 'r' to rewind the windowed audio buffer.");
		println(" * Press UP arrow to increase gain by 3.0 dB.");
		println(" * Press DOWN arrow to decrease gain by 3.0 dB.");
		println(" * Press 'u' to mute audio.");
		println(" * Press 't' to turn stream capture on or off.");
		println(" * Press 'w' to write the image colors to the audio buffer as transcoded values.");
		println(" * Press 'W' to write the audio buffer samples to the image as color values.");
		println(" * Press 'x' to delete the current active brush shape or the oldest brush shape.");
		println(" * Press 'X' to delete the most recent brush shape.");
		println(" * Press 'y' to turn rain (random audio events) on and off.");
		println(" * Press 'z' to reset brushstrokes and audio buffer (you may need to reload audio).");
		println(" * Press 'V' to record a video.");
		println(" * Press 'h' or 'H' to show help and key commands.");
	}
	

	/**
	 * Utility method for applying hue and saturation values from a source array of RGB values
	 * to the brightness values in a target array of RGB values, using a lookup table to redirect indexing.
	 * Available as a static method in PixelAudio class PixelAudioMapper.
	 * 
	 * @param colorSource    a source array of RGB data from which to obtain hue and saturation values
	 * @param graySource     an target array of RGB data from which to obtain brightness values
	 * @param lut            a lookup table, must be the same size as colorSource and graySource
	 * @return the graySource array of RGB values, with hue and saturation values changed
	 * @throws IllegalArgumentException if array arguments are null or if they are not the same length
	 */
	public int[] applyColor(int[] colorSource, int[] graySource, int[] lut) {
	  if (colorSource == null || graySource == null || lut == null) 
	    throw new IllegalArgumentException("colorSource, graySource and lut cannot be null.");
	  if (colorSource.length != graySource.length || colorSource.length != lut.length) 
	    throw new IllegalArgumentException("colorSource, graySource and lut must all have the same length.");
	  // initialize a reusable array for HSB color data -- this is a way to speed up the applyColor() method
	  float[] hsbPixel = new float[3];
	  for (int i = 0; i < graySource.length; i++) {
	    graySource[i] = PixelAudioMapper.applyColor(colorSource[lut[i]], graySource[i], hsbPixel);
	  }
	  return graySource;
	}
	
	
	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                   BEGIN FILE I/O METHODS                       */
	/*                                                                */
	/*----------------------------------------------------------------*/
	
	// -------- BEGIN FILE I/O FOR APPLYING COLOR --------- //
	
	/* 
	 * Here is a special section of code for TutorialOne and other applications that
	 * color a grayscale image with color data from a file. The color and saturation
	 * come from the selected file, the brightness (gray values, more or less) come
	 * from an image you supply. 
	 */

	/**
	 * Call to initiate process of opening an image file to get its color data.
	 */
	public void chooseColorImage() {
		selectInput("Choose an image file to apply color: ", "colorFileSelected");
	}
	

	/**
	 * callback method for chooseColorImage() 
	 * @param selectedFile    the File the user selected
	 */
	public void colorFileSelected(File selectedFile) {
		if (null != selectedFile) {
			String filePath = selectedFile.getAbsolutePath();
			String fileName = selectedFile.getName();
			String fileTag = fileName.substring(fileName.lastIndexOf('.') + 1);
			fileName = fileName.substring(0, fileName.lastIndexOf('.'));
			if (fileTag.equalsIgnoreCase("png") || fileTag.equalsIgnoreCase("jpg") || fileTag.equalsIgnoreCase("jpeg")) {
				imageFile = selectedFile;
				imageFilePath = filePath;
				imageFileName = fileName;
				imageFileTag = fileTag;
				println("--- Selected color file "+ fileName +"."+ fileTag);
				// apply the color data (hue, saturation) in the selected image to our display image, mapImage
				applyImageColorToMapImage(imageFile);
			} else {
				println("----- File is not a recognized image format ending with \"png\", \"jpg\", or \"jpeg\".");
			}
		} else {
			println("----- No file was selected.");
		}
	}
	
	
    /**
     * Apply the hue and saturation channels of one image to another image, 
     * leaving its brightness channel unchanged.
     * @param colorImage     image that is the source of hue and saturation values
     * @param targetImage    target image where brightness will remain unchanged
     */
    public void applyImageColor(PImage colorImage, PImage targetImage) {
		int w = colorImage.width > mapImage.width ? targetImage.width : colorImage.width;
		int h = colorImage.height > targetImage.height ? targetImage.height : colorImage.height;
        float[] hsbPixel = new float[3];
        colorImage.loadPixels();
        int[] colorSource = colorImage.pixels;
        targetImage.loadPixels();
        int[] graySource = targetImage.pixels;
        for (int y = 0; y < h; y++) {
        	int rowStart = y * w;
        	for (int x = 0; x < w; x++) {
        		int cPos = rowStart + x;
        		int gPos = y * targetImage.width + x;
        		graySource[gPos] = PixelAudioMapper.applyColor(colorSource[cPos], graySource[gPos], hsbPixel);
        	}
        }
        targetImage.updatePixels();
	}
    
    
    public void applyImageColorToMapImage(File imgFile) {
    	PImage colorImage = loadImage(imgFile.getAbsolutePath());
    	applyImageColor(colorImage, mapImage);
    }
	
	// -------- END FILE I/O FOR APPLYING COLOR --------- //
    
    
    /*
     * Here is a section of "regular" file i/o methods for audio and image files.
     */
	
	/**
	 * Wrapper method for Processing's selectInput command
	 */
	public void chooseFile() {
		oldIsAnimating = isAnimating;
		isAnimating = false;
		oldIsListening = isListening;
		isListening = listenToAnthem(false);
		selectInput("Choose an audio file or an image file: ", "fileSelected");
	}
	
	
	/**
	 * callback method for chooseFile(), handles standard audio and image formats for Processing.
	 * If a file has been successfully selected, continues with a call to loadAudioFile() or loadImageFile().
	 * 
	 * @param selectedFile    the File the user selected
	 */
	public void fileSelected(File selectedFile) {
		if (null != selectedFile) {
			String filePath = selectedFile.getAbsolutePath();
			String fileName = selectedFile.getName();
			String fileTag = fileName.substring(fileName.lastIndexOf('.') + 1);
			fileName = fileName.substring(0, fileName.lastIndexOf('.'));
			if (fileTag.equalsIgnoreCase("mp3") || fileTag.equalsIgnoreCase("wav") || fileTag.equalsIgnoreCase("aif")
					|| fileTag.equalsIgnoreCase("aiff")) {
				// we chose an audio file
				audioFile = selectedFile;
				audioFilePath = filePath;
				audioFileName = fileName;
				audioFileTag = fileTag;
				println("---- Selected file " + fileName + "." + fileTag + " at "
						+ filePath.substring(0, filePath.length() - fileName.length()));
				loadAudioFile(audioFile);
			} 
			else if (fileTag.equalsIgnoreCase("png") || fileTag.equalsIgnoreCase("jpg")
					|| fileTag.equalsIgnoreCase("jpeg")) {
				// we chose an image file
				imageFile = selectedFile;
				imageFilePath = filePath;
				imageFileName = fileName;
				imageFileTag = fileTag;
				loadImageFile(imageFile);
			} 
			else {
				println("---- File is not a recognized audio format ending with \"mp3\", \"wav\", \"aif\", or \"aiff\".");
			}
		} 
		else {
			println("---- No audio or image file was selected.");
		}
		isAnimating = oldIsAnimating;
		isListening = listenToAnthem(oldIsListening);
	}

	
	/**
	 * LoadAudioFile attempts to load audio data from a selected file into playBuffer and anthemBuffer.
	 * If audio loads, resizes playBuffer to mapSize and loads the entire file into anthemBuffer, 
	 * which is passed to the PASamplerInstrumentPool audio sampling instrument "pool". 
	 * In the context of a windowed buffer as audio source, as in TutorialOneWindowBuffer,
	 * blending is not applied to the WindowedBuffer instance, but only to playBuffer. I have
	 * ignored it in this example code. It might make sense to use it to blend images during 
	 * a performance, but I leave that to future development by others or by myself.
	 * 
	 * @param audFile    an audio file
	 */
	public void loadAudioFile(File audFile) {
		boolean isLoaded = false;
		if (isBlending) {
			MultiChannelBuffer buff = new MultiChannelBuffer(1024, 1);
			fileSampleRate =  minim.loadFileIntoBuffer(audFile.getAbsolutePath(), buff);
			if (buff.getBufferSize() != mapper.getSize()) buff.setBufferSize(mapper.getSize());
			// fileSampleRate > 0 means we read audio from the file
			isLoaded = (fileSampleRate > 0);
			if (isLoaded) {
				println("---- file sample rate is "+ fileSampleRate);
				// TODO we're ignoring possibly different sampling rates in the playBuffer and buff, does it matter?
				blendInto(playBuffer, buff, 0.5f, false, -12.0f);    // mix audio sources without normalization
			}
		}
		else {
			// read audio file into our MultiChannelBuffer, buffer size will be adjusted to match the file
			fileSampleRate = minim.loadFileIntoBuffer(audFile.getAbsolutePath(), playBuffer);
			// sampleRate > 0 means we read audio from the file
			isLoaded = (fileSampleRate > 0);
			if (isLoaded) {
				println("---- file sample rate is "+ fileSampleRate);
				// save the length of the buffer as read from the file, for future use
				this.audioFileLength = playBuffer.getBufferSize();
				// resize the buffer to mapSize, if necessary -- signal will not be overwritten
				if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
			}
		}
		if (isLoaded) {
			loadAnthem(audFile);    // load the complete file into windowed buffer
			// because playBuffer is used by synth and pool and should not change, while audioSignal changes
			// when the image animates, we don't want playBuffer and audioSignal to point to the same array
			// so we copy channel 0 of the buffer into audioSignal, truncated or padded to fit mapSize
			audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
			audioLength = audioSignal.length;
			if (isLoadToBoth) writeAudioToImage(audioSignal, mapper, mapImage, chan);
			totalShift = 0;    // reset animation shift when audio is reloaded
		}
		else {
			println("---->> File did not load or was empty.");
		}
	}

	
	public float loadAnthem(File audFile) {
		anthemBuffer = new MultiChannelBuffer(1, 1); // dummy buffer
		float fileSampleRate = minim.loadFileIntoBuffer(audFile.getAbsolutePath(), anthemBuffer);
		if (fileSampleRate == 0) {
			return -1;
		}
		else {
			// if anthemBuffer size is smaller than mapper size, padd it to fit
			if (anthemBuffer.getBufferSize() < mapper.getSize()) anthemBuffer.setBufferSize(mapper.getSize());
			// save the length of the buffer as read from the file, for future use
			this.audioFileLength = anthemBuffer.getBufferSize();
			this.windowHopSize = (int) (fileSampleRate/this.frameRate);
			anthemSignal = anthemBuffer.getChannel(0);
			windowBuff = new WindowedBuffer(anthemSignal, mapSize, windowHopSize);
			// this.instrument = new WFSamplerInstrument(anthemBuffer, audioOut.sampleRate(), 32, audioOut, env); 
			this.pool = new PASamplerInstrumentPool(anthemBuffer, fileSampleRate, 
					    poolSize, perInstrumentVoices, audioOut, defaultEnv);
			println("---- initialized WindowBuffer windowBuff with signal length "+ windowBuff.buffer.length 
					+", window size "+ windowBuff.getWindowSize() +", hop size "+ windowBuff.getHopSize());
			println("---- file sample rate = "+ fileSampleRate +", file duration = "+ windowBuff.buffer.length / fileSampleRate 
					+", window time span = "+ windowBuff.getWindowSize() / fileSampleRate);
			return fileSampleRate;
		}
	}


	/**
	 * Blends audio data from buffer "src" into buffer "dest" in place.
	 *
	 * The formula per sample is:
	 *    dest[i] = weight * src[i] + (1 - weight) * dest[i]
	 *
	 * @param dest   Destination buffer (will be modified)
	 * @param src    Source buffer to blend into dest
	 * @param weight Blend ratio (0.0 = keep dest, 1.0 = replace with src)
	 */
	public static void blendInto(MultiChannelBuffer dest, MultiChannelBuffer src, float weight, boolean normalize, float targetDB) {
		// Clamp blend ratio to [0, 1]
		weight = Math.max(0f, Math.min(1f, weight));
		float invWeight = 1f - weight;
		// Match dimensions safely
		int channels = Math.min(dest.getChannelCount(), src.getChannelCount());
		int frames = Math.min(dest.getBufferSize(), src.getBufferSize());
		// Perform blending directly on dest channels
		for (int c = 0; c < channels; c++) {
			float[] d = dest.getChannel(c);
			float[] s = src.getChannel(c);
			for (int i = 0; i < frames; i++) {
				d[i] = weight * s[i] + invWeight * d[i];
			}
		}
		if (normalize) {
		    for (int c = 0; c < channels; c++) {
		        float[] d = dest.getChannel(c);
		        normalize(d, targetDB);
		    }

		}
	}
	
	/**
	 * Normalizes a single-channel signal array to a target RMS level in dBFS.
	 *
	 * @param signal    The audio samples to normalize (modified in place)
	 * @param targetDB  The target RMS level in decibels relative to full scale
	 *                  (e.g. -3.0f for moderately loud, -12.0f for safe headroom)
	 */
	public static void normalize(float[] signal, float targetDB) {
	    if (signal == null || signal.length == 0) return;
	    // --- Step 1: Compute RMS of the signal ---
	    float sumSq = 0f;
	    for (float v : signal) {
	        sumSq += v * v;
	    }
	    float rms = (float)Math.sqrt(sumSq / signal.length);
	    // --- Step 2: Convert target dBFS to linear RMS value ---
	    float targetRMS = (float)Math.pow(10.0, targetDB / 20.0);
	    // --- Step 3: Compute and apply gain ---
	    if (rms > 1e-6f) {
	        float gain = targetRMS / rms;
	        if (gain > 100.0f) gain = 100.0f; // safety limit
	        for (int i = 0; i < signal.length; i++) {
	            signal[i] *= gain;
	        }
	    }
	}

	/**
	 * Transcodes audio data in sig[] and writes it to color channel chan of mapImage 
	 * using the lookup tables in mapper to redirect indexing. Calls PixelAudioMapper 
	 * method mapSigToImg(), which will throw an IllegalArgumentException if 
	 * sig.length != img.pixels.length or sig.length != mapper.getSize(). 
	 * 
	 * @param sig         an array of float, should be audio data in the range [-1.0, 1.0]
	 * @param mapper      a PixelAudioMapper
	 * @param img         a PImage
	 * @param chan        a color channel
	 */
	public void writeAudioToImage(float[] sig, PixelAudioMapper mapper, PImage img, PixelAudioMapper.ChannelNames chan) {
		// If sig.length == mapper.getSize() == mapImage.width * mapImage.height, we can call safely mapper.mapSigToImg()	
		img.loadPixels();
		mapper.mapSigToImg(sig, img.pixels, chan);
		img.updatePixels();
	}


	/**
	 * Attempts to load image data from a selected file into mapImage, then calls writeImageToAudio() 
	 * to transcode HSB brightness channel to audio and writes it to playBuffer and audioSignal.
	 * If you want to load the image file and audio file separately, comment out writeImageToAudio(). 
	 * 
	 * @param imgFile    an image file
	 */
	public void loadImageFile(File imgFile) {
		PImage img = loadImage(imgFile.getAbsolutePath());
		// stash information about the image in imgFileWidth, imageFileHeight for future use
		imageFileWidth = img.width;
		imageFileHeight = img.height;
		// calculate w and h for copying image to display (mapImage)
		int w = img.width > mapImage.width ? mapImage.width : img.width;
		int h = img.height > mapImage.height ? mapImage.height : img.height;
		if (chan == PixelAudioMapper.ChannelNames.ALL) {
			if (isBlending) {
				PImage dest = mapImage;
				PImage src = img;
				src.loadPixels();
				for (int i = 0; i < src.pixels.length; i++) {
					int pixel = src.pixels[i];
					src.pixels[i] = setAlphaWithBlack(pixel, 96);
				}
				src.updatePixels();
				dest.blend(src, 0, 0, src.width, src.height, 0, 0, dest.width, dest.height, BLEND);
			}
			else {
				// copy the image directly using Processing copy command
				mapImage.copy(img, 0, 0, w, h, 0, 0, w, h);
			}
		} 
		else {
			// copy only specified channels of the new image
			PImage mixImage = createImage(w, h, RGB);
			mixImage.copy(mapImage, 0, 0, w, h, 0, 0, w, h);
			img.loadPixels();
			mixImage.loadPixels();
			mixImage.pixels = PixelAudioMapper.pushChannelToPixel(img.pixels, mixImage.pixels, chan);
			mixImage.updatePixels();
			mapImage.copy(mixImage, 0, 0, w, h, 0, 0, w, h);
		}
		if (isLoadToBoth) {
		    // prepare to copy image data to audio variables
		    // resize the buffer to mapSize, if necessary -- signal will not be overwritten
		    if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
		    audioSignal = playBuffer.getChannel(0);
		    writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
		    // now that the image data has been written to audioSignal, set playBuffer channel 0 to the new audio data
		    playBuffer.setChannel(0, audioSignal);
		    audioLength = audioSignal.length;
		    if (pool != null) pool.setBuffer(playBuffer);
		    else pool = new PASamplerInstrumentPool(playBuffer, sampleRate, poolSize, 1, audioOut, defaultEnv);
		    // because playBuffer is used by synth and pool and should not change, while audioSignal changes
		    // when the image animates, we don't want playBuffer and audioSignal to point to the same array
		    // copy channel 0 of the buffer into audioSignal, truncated or padded to fit mapSize
		    audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
		    audioLength = audioSignal.length;
			totalShift = 0;    // reset animation shift when audio is reloaded
		}
	}

	public int setAlphaWithBlack(int argb, int alpha) {
		int[] c = PixelAudioMapper.rgbaComponents(argb);
		if (c[0] == c[1] && c[1] == c[2] && c[2] == 0) {
			alpha = 0;
		}
		return alpha << 24 | c[0] << 16 | c[1] << 8 | c[2];
	}
	
	public static int setAlpha(int argb, int alpha) {
		 return (argb & 0x00FFFFFF) | (alpha << 24);
	}
	

	/**
	 * This method writes a color channel from the an image to playBuffer, fulfilling a 
	 * central concept of the PixelAudio library: image is sound. Calls mapper.mapImgToSig(), 
	 * which will throw an IllegalArgumentException if img.pixels.length != sig.length or 
	 * img.width * img.height != mapper.getWidth() * mapper.getHeight(). 
	 * 
	 * @param img       a PImage, a source of data
	 * @param mapper    a PixelAudioMapper, handles mapping between image and audio signal
	 * @param sig       an target array of float in audio format 
	 * @param chan      a color channel
	 */
	public void writeImageToAudio(PImage img, PixelAudioMapper mapper, float[] sig, PixelAudioMapper.ChannelNames chan) {
		sig = mapper.mapImgToSig(img.pixels, sig, chan);
	}		
	
	
	/**
	 * Calls Processing's selectOutput method to start the process of saving 
	 * the current audio signal to a .wav file. 
	 */
	public void saveToAudio() {
		// File folderToStartFrom = new File(dataPath("") + "/");
		// selectOutput("Select an audio file to write to:", "audioFileSelectedWrite", folderToStartFrom);
		selectOutput("Select an audio file to write to:", "audioFileSelectedWrite");
	}

	
	public void audioFileSelectedWrite(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
			return;
		}
		String fileName = selection.getAbsolutePath();
		if (selection.getName().indexOf(".wav") != selection.getName().length() - 4) {
			fileName += ".wav";
		}
		try {
			saveAudioToFile(audioSignal, sampleRate, fileName);
		} catch (IOException e) {
			println("--->> There was an error outputting the audio file " + fileName +", "	+ e.getMessage());
		} catch (UnsupportedAudioFileException e) {
			println("--->> The file format is unsupported " + e.getMessage());
		}
	}
	
	
	/**
	 * Saves audio data to 16-bit integer PCM format, which Processing can also open.
	 * This same method can be called as a static method in PixelAudio.
	 * 
	 * @param samples			an array of floats in the audio range (-1.0f, 1.0f)
	 * @param sampleRate		audio sample rate for the file
	 * @param fileName			name of the file to save to
	 * @throws IOException		an Exception you'll need to handle to call this method (see keyPressed entry for 's')
	 * @throws UnsupportedAudioFileException		another Exception (see keyPressed entry for 's')
	 */
	public void saveAudioToFile(float[] samples, float sampleRate, String fileName)
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

	
	/**
	 * Calls Processing's selectOutput method to start the process of saving 
	 * the mapImage (the offscreen copy of the display image) to a .png file. 
	 */
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
		saveImageToFile(mapImage, fileName);
	}

	public void saveImageToFile(PImage img, String fileName) {
		img.save(fileName);
	}
	
	
	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                     BEGIN AUDIO METHODS                        */
	/*                                                                */
	/*----------------------------------------------------------------*/
	
	/**
	 * CALL THIS METHOD IN SETUP()
	 * Initializes Minim audio library and audio variables.
	 */
	public void initAudio() { 
		minim = new Minim(this);
		// use the getLineOut method of the Minim object to get an AudioOutput object
		this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
		audioOut.setGain(defaultGain);
		println("---- audio out gain is "+ audioOut.getGain());
		// create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
		this.playBuffer = new MultiChannelBuffer(mapSize, 1);
		this.anthemBuffer = new MultiChannelBuffer(mapSize, 1);
		this.audioSignal = playBuffer.getChannel(0);
		this.audioLength = audioSignal.length;
		this.anthemSignal = new float[audioLength];
		System.arraycopy(audioSignal, 0, anthemSignal, 0, audioLength);
		windowBuff = new WindowedBuffer(anthemSignal, mapSize, 1024);
		// ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
		defaultEnv = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
		granularEnv = ADSRUtils.fitEnvelopeToDuration(defaultEnv, grainDuration);
		// initialize mouse event tracking array
		timeLocsArray = new ArrayList<TimedLocation>();
	}
	
	
	/**
	 * Prepares audioSignal before it is used as an instrument source.
	 * Modify as needed to prepare your audio signal data. 
	 * TODO Of limited use. Discard?
	 */
	public void renderSignals() {
		writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
		playBuffer.setChannel(0, audioSignal);
		audioLength = audioSignal.length;
	}
	

	/**
	 * Typically called from mousePressed with mouseX and mouseY, generates audio events.
	 * 
	 * @param x    x-coordinate within a PixelAudioMapper's width
	 * @param y    y-coordinate within a PixelAudioMapper's height
	 */
	public void audioMousePressed(int x, int y) {
		setSampleVars(x, y);
		float panning = map(sampleX, 0, width, -0.8f, 0.8f);
		// update audioSignal and playBuffer if audioSignal hasn't been initialized or if 
		// playBuffer needs to be refreshed after changes to its data source (isBufferStale == true).
		// TODO logic not used in current version, should be deleted?
		if (audioSignal == null || isBufferStale) {
			renderSignals();
			isBufferStale = false;
		}
		if (pool == null) {
			println("---- You need to load a audio file before you can trigger audio events.");
		}
		else {
			// use the default envelope
			if (!isGranular) {
				int len = calcSampleLen();
				playSample(samplePos, len, defaultGain, defaultEnv, panning);
				//println("variable duration "+ len);
			}
			else {
				int len = (int)(abs((this.grainDuration) * sampleRate / 1000.0f));
				playSample(samplePos, len, defaultGain, granularEnv, panning);
				//println("---- grain "+ len);
			}
		}
	}
	
	/**
	 * Sets variables sampleX, sampleY and samplePos. Arguments x and y may be outside
	 * the window bounds, sampleX and sampleY will be constrained to window bounds. As
	 * a result, samplePos will be within the bounds of audioSignal.
	 * 
	 * @param x    x coordinate, typically from a mouse event
	 * @param y    y coordinate, typically from a mouse event
	 * @return     samplePos, the index of of (x, y) along the signal path
	 */
	public int setSampleVars(int x, int y) {
		// (PApplet.constrain(x, 0, width-1), PApplet.constrain(y, 0, height-1));
		sampleX = min(max(0, x), width - 1);
		sampleY = min(max(0, y), height - 1);
		samplePos = getSamplePos(sampleX, sampleY);
		return samplePos;
	}

	/**
	 * Calculates the index of the image pixel within the signal path,
	 * taking the current window position and animated shifting of pixels 
	 * and audioSignal into account.
	 * 
	 * @param x    an x coordinate within mapImage and display bounds
	 * @param y    a y coordinate within mapImage and display bounds
	 * @return     the index of the sample corresponding to (x,y) on the signal path
	 */
	public int getSamplePos(int x, int y) {
		samplePos = mapper.lookupSignalPos(x, y);
		// calculate how much animation has shifted the indices into the buffer
		totalShift = (totalShift + shift % mapSize + mapSize) % mapSize;
		samplePos = (samplePos + totalShift) % mapSize;
		samplePos += this.windowBuff.getIndex();
		return samplePos % this.windowBuff.getBufferSize();
	}
		
	
	/**
	 * Draws a circle at the location of an audio trigger (mouseDown event).
	 * @param x		x coordinate of circle
	 * @param y		y coordinate of circle
	 */
	public void drawCircle(int x, int y) {
		//float size = isRaining? random(10, 30) : 60;
		fill(color(233, 220, 199));
		noStroke();
		circle(x, y, 24);
	}	
	/**
	 * Draws a circle at the location of an audio trigger (mouseDown event).
	 * @param x		x coordinate of circle
	 * @param y		y coordinate of circle
	 * @param d	    diameter of circle
	 */
	public void drawCircle(int x, int y, float d, int c) {
		//float size = isRaining? random(10, 30) : 60;
		fill(c);
		noStroke();
		circle(x, y, d);
	}	
	
	public void raindrops() {
		float x = random(width/4, 3 * width/4);
		float y = random(16, height/5);
//		int signalPos = mapper.lookupSample(x, y);
//		int[] coords = mapper.lookupCoordinate(signalPos);
		audioMousePressed((int) x, (int) y);
	}	

	
	/**
	 * TODO update for buffer animation
	 * @param isStartListening    true if audio stream capture should be initiated, false if it should be ended
	 * @return    current value of isStartListening
	 */
	public boolean listenToAnthem(boolean isStartListening) {
		if (isStartListening) {
			if (isFixedLength) {

			}
			else {

			}
//			anthemBuffer.loop();
		}
		else {
//			anthemBuffer.pause();
			if (isFixedLength) {

			}
		}
		return isStartListening;
	}
	
	/*----------------------------------------------------------------*/
	/*      How do I call playSample()? Let me count the ways.        */
	/*----------------------------------------------------------------*/

	/**
	 * Plays an audio sample with the default envelope.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param samplelen    length of the sample (will be adjusted)
	 * @param amplitude    amplitude of the sample on playback
	 * @return the calculated sample length in samples
	 */
	public int playSample(int samplePos, int samplelen, float amplitude) {
		if (pool == null) return 0;
		samplelen = pool.playSample(samplePos, samplelen, amplitude);
		int durationMS = (int)(samplelen/sampleRate * 1000);
		timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
		// return the length of the sample
		return samplelen;
	}

	/**
	 * Plays an audio sample with a custom envelope.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param samplelen    length of the sample (will be adjusted)
	 * @param amplitude    amplitude of the sample on playback
	 * @param env          an ADSR envelope for the sample
	 * @return the calculated sample length in samples
	 */
	public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env) {
		if (pool == null) return 0;
		samplelen = pool.playSample(samplePos, samplelen, amplitude, env);
		int durationMS = (int)(samplelen/sampleRate * 1000);
		timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
		// return the length of the sample
		return samplelen;
	}

	
	/**
	 * Plays an audio sample with default envelope and stereo pan.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param samplelen    length of the sample (will be adjusted)
	 * @param amplitude    amplitude of the sample on playback
	 * @return the calculated sample length in samples
	 */
	public int playSample(int samplePos, int samplelen, float amplitude, float pan) {
		if (pool == null) return 0;
		samplelen = pool.playSample(samplePos, samplelen, amplitude, defaultEnv, pitchScaling, pan);
		int durationMS = (int)(samplelen/sampleRate * 1000);
		timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
		// return the length of the sample
		return samplelen;
	}

	/**
	 * Plays an audio sample with a custom envelope and stereo pan.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param samplelen    length of the sample (will be adjusted)
	 * @param amplitude    amplitude of the sample on playback
	 * @param env          an ADSR envelope for the sample
	 * @param pan          position of sound in the stereo audio field (-1.0 = left, 0.0 = center, 1.0 = right)
	 * @return the calculated sample length in samples
	 */
	public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pan) {
		if (pool == null) return 0;
		samplelen = pool.playSample(samplePos, samplelen, amplitude, env, pitchScaling, pan);
		int durationMS = (int)(samplelen/sampleRate * 1000);
		// println("---->> adding event to timeLocsArray "+  samplelen, durationMS, millis());
		timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
		// return the length of the sample
		return samplelen;
	}

	/**
	 * Plays an audio sample with  with a custom envelope, pitch and stereo pan.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param samplelen    length of the sample (will be adjusted)
	 * @param amplitude    amplitude of the sample on playback
	 * @param env          an ADSR envelope for the sample
	 * @param pitch        pitch scaling as deviation from default (1.0), where 0.5 = octave lower, 2.0 = oactave higher 
	 * @param pan          position of sound in the stereo audio field (-1.0 = left, 0.0 = center, 1.0 = right)
	 * @return
	 */
	public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pitch, float pan) {
		if (pool == null) return 0;
		samplelen = pool.playSample(samplePos, samplelen, amplitude, env, pitch, pan);
		int durationMS = (int)(samplelen/sampleRate * 1000);
		timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
		// return the length of the sample
		return samplelen;
	}

	
	/**
	 * @return a length in samples with some Gaussian variation
	 */
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

	/*				END AUDIO METHODS                        */
	

	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                    BEGIN DRAWING METHODS                       */
	/*                                                                */
	/*----------------------------------------------------------------*/
	
	/**
	 * Initializes allPoints and adds the current mouse location to it. 
	 */
	public void initAllPoints() {
		allPoints = new ArrayList<PVector>();
		allTimes = new ArrayList<Integer>();
		startTime = millis();
		addPoint(PApplet.constrain(mouseX, 0, width-1), PApplet.constrain(mouseY, 0, height-1));
	}
	
	
	/**
	 * Responds to mousePressed events associated with drawing.
	 */
	public void handleMousePressed(int x, int y) {
		if (activeBrush != null) {
			// a brushShape was triggered
			eventPoints = activeBrush.getEventPoints();
			loadEventPoints();
			activeBrush = null;
		} 
		else {
			// handle audio generation in response to a mouse click
			audioMousePressed(PApplet.constrain(x, 0, width-1), PApplet.constrain(y, 0, height-1));
		}
	}
	
	
	/**
	 * While user is dragging the mouses and isDrawMode == true, accumulates new points
	 * to allPoints and event times to allTimes. Sets sampleX, sampleY and samplePos variables.
	 * We constrain points outside the bounds of the display window. An alternative approach 
	 * is be to ignore them (isIgnoreOutsideBounds == true), which may give a more "natural" 
	 * appearance for fast drawing. 
	 */
	public void addPoint(int x, int y) {
		// we do some very basic point thinning to eliminate successive duplicate points
		if (x != currentPoint.x || y != currentPoint.y) {
			if (isIgnoreOutsideBounds && (x < 0 || x >= width || y < 0 || y >= height)) return;
			currentPoint = new PVector(x, y);
			allPoints.add(currentPoint);
			allTimes.add(millis() - startTime);
			setSampleVars(x, y);
		}
	}
	
	
	/**
	 * Processes the eventPoints list to create TimedLocation events 
	 * and stores them in curveTLEvents. 
	 */
	public void loadEventPoints() {
		if (eventPoints != null) {
			eventPointsIter = eventPoints.listIterator();
			int startTime = millis();
			// println("building pointsTimer: "+ startTime);
			if (curveTLEvents == null) curveTLEvents = new ArrayList<TimedLocation>();
			if (!isGranular) {				
				storeCurveTL(eventPointsIter, startTime);
			}
			else {
				storeGranularCurveTL(eventPointsIter, startTime, grainDuration);
			}
		}
		else {
			println("--->> NULL eventPoints");
		}
	}
	
	
	/**
	 * Processes the eventPoints list to create TimedLocation events 
	 * and stores them in curveTLEvents. 
	 * @param startTime    time in millis (in the future!) when event should begin
	 */
	public void loadEventPoints(int startTime) {
	  if (eventPoints != null) {
	    eventPointsIter = eventPoints.listIterator();
	    // println("building pointsTimer: "+ startTime);
	    if (curveTLEvents == null) curveTLEvents = new ArrayList<TimedLocation>();
		if (!isGranular) {				
			storeCurveTL(eventPointsIter, startTime);
		}
		else {
			storeGranularCurveTL(eventPointsIter, startTime, grainDuration);
		}
	  }
	  else {
	    println("--->> NULL eventPoints");
	  }
	}

	/**
	 * @param iter         a ListIterator over eventPoints
	 * @param startTime    a time in millis  
	 * @param grainSize    a duration in milliseconds
	 */
	public synchronized void storeGranularCurveTL(ListIterator<PVector> iter, int startTime, int grainSize) {
		startTime += 50;
		int grainOffset = grainSize / 4;
		int i = 0;
		while (iter.hasNext()) {
			PVector loc = iter.next();
			curveTLEvents.add(new TimedLocation(Math.round(loc.x), Math.round(loc.y), startTime + i++ * grainOffset));
		}
		Collections.sort(curveTLEvents);
	}	
	
	/**
	 * @param iter         a ListIterator over eventPoints
	 * @param startTime    a time in millis
	 */
	public synchronized void storeCurveTL(ListIterator<PVector> iter, int startTime) {
		startTime += 50;
		int i = 0;
		while (iter.hasNext()) {
			PVector loc = iter.next();
			curveTLEvents.add(new TimedLocation(Math.round(loc.x), Math.round(loc.y), startTime + i++ * eventStep));
		}
		Collections.sort(curveTLEvents);
	}	
	
	
	/**
	 * Initializes a PACurveMaker instance with allPoints as an argument to the factory method 
	 * PACurveMaker.buildCurveMaker() and then fills in PACurveMaker instance variables from 
	 * global variables in the host class. 
	 */
	public void initCurveMaker() {
		curveMaker = PACurveMaker.buildCurveMaker(allPoints, allTimes, startTime);
		curveMaker.setBrushColor(readyBrushColor);
		curveMaker.setActiveBrushColor(activeBrushColor);
		curveMaker.setEpsilon(epsilon);
		curveMaker.setTimeOffset(millis() - startTime);
		curveMaker.calculateDerivedPoints();
		PABezShape curve = curveMaker.getCurveShape();
		eventPoints = curve.getPointList(polySteps);
		loadEventPoints();
		this.brushShapesList.add(curveMaker);
		setSampleVars(mouseX, mouseY);
		// testing
		/*
		println("----- RDP Indices: ");
		int[] ndx = curveMaker.getRdpIndicesAsInts();
		for (int n : ndx) {
			println(n);
		}
		*/
	}
	
	/**
	 * Iterates over brushShapesList and draws the brushstrokes stored in 
	 * each PACurveMaker in the list. Detects if mouse is over a brush 
	 * and points activeBrush to it if that is the case.
	 */
	public void drawBrushShapes() {
		if (this.brushShapesList.size() > 0) {
			int idx = 0;
			activeBrush = null;
			for (PACurveMaker bd : brushShapesList) {
				int brushFill = readyBrushColor;
				if (mouseInPoly(bd.getBrushPoly())) {
					brushFill = activeBrushColor;
					activeBrush = bd;
					activeIndex = idx;
				}
				PACurveUtility.shapeDraw(this, bd.getBrushShape(), brushFill, brushFill, 2);
				idx++;
			}
		}
	}
	

	/**
	 * Draws shapes stored in curveMaker, a PACurveMaker instance that stores the most recent drawing data. 
	 */
	public void curveMakerDraw() {
		if (curveMaker.isReady()) {
			curveMaker.brushDraw(this, newBrushColor, newBrushColor, 2);
			// curveMaker.brushDrawDirect(this);
			curveMaker.eventPointsDraw(this);
		}
	}

	
	/**
	 * Tracks and runs TimedLocation events in the curveTLEvents list.
	 * This method is synchronized with a view to future development where it may be called from different threads.
	 */
	public synchronized void runCurveEvents() {
		// if the event list is null or empty, skip out
		if (curveTLEvents != null && curveTLEvents.size() > 0) {
			int currentTime = millis();
			curveTLEvents.forEach(tl -> {
				if (tl.eventTime() < currentTime) {
					// the curves may exceed display bounds, so we have to constrain values
					sampleX = PixelAudio.constrain(Math.round(tl.getX()), 0, width - 1);
					sampleY = PixelAudio.constrain(Math.round(tl.getY()), 0, height - 1);
					float panning = map(sampleX, 0, width, -0.8f, 0.8f);
					int pos = getSamplePos(sampleX, sampleY);
					if (!isGranular) {
						playSample(pos, calcSampleLen(), defaultGain, panning);
					}
					else {
						int len = (int)(abs((this.grainDuration) * sampleRate / 1000.0f));
						playSample(pos, len, defaultGain, granularEnv, panning);
					}
					tl.setStale(true);
					// println("----- ");
				} 
				else {
					// pointEventsArray is sorted by time, so ignore events still in the future
					return;
				}
			});
			curveTLEvents.removeIf(TimedLocation::isStale);
		}
	}
	

	/**
	 * Tracks and runs TimedLocation events in the timeLocsArray list.
	 * This method is synchronized with a view to future development where it may be called from different threads.
	 */
	public synchronized void runPointEvents() {
		int currentTime = millis();
		for (Iterator<TimedLocation> iter = timeLocsArray.iterator(); iter.hasNext();) {
			TimedLocation tl = iter.next();
			tl.setStale(tl.eventTime() < currentTime);
			if (!tl.isStale()) {
				drawCircle(tl.getX(), tl.getY(), 20, color(233, 89, 110, 199));
			}
		}
		timeLocsArray.removeIf(TimedLocation::isStale);		
	}
	

	/**
	 * Detects if the mouse is within a selected polygon. 
	 * @param poly    a polygon as an ArrayList of PVectors
	 * @return        true if mouse is within poly, false otherwise
	 */
	public boolean mouseInPoly(ArrayList<PVector> poly) {
		return PABezShape.pointInPoly(poly, mouseX, mouseY);
	}

	
	/**
	 * Reinitializes audio and clears event lists. If isClearCurves is true, clears brushShapesList
	 * and curveTLEvents. 
	 * @param isClearCurves
	 */
	public void reset(boolean isClearCurves) {
		initAudio();
		if (audioFile != null)
			loadAudioFile(audioFile);
		if (this.curveMaker != null) this.curveMaker = null;
		if (this.eventPoints != null) this.eventPoints.clear();
		this.activeIndex = 0;
		if (isClearCurves) {
			if (this.brushShapesList != null) this.brushShapesList.clear();
			if (this.curveTLEvents != null) this.curveTLEvents.clear();
			println("----->>> RESET audio, event points and curves <<<------");
		}
		else {
			println("----->>> RESET audio and event points <<<------");
		}
	}

	/**
	 * Plays all audio events controlled by PACurveMaker curves in brushShapesList, 
	 * spaced out by offset milliseconds.
	 * @param offset
	 */
	public void playBrushstrokes(int offset) {
		  int startTime = millis() + 50;
		  for (PACurveMaker curve : brushShapesList) {
		    if (curve.isReady()) {
		      eventPoints = curve.getCurveShape().getPointList(polySteps);
		      loadEventPoints(startTime);
		      startTime += offset;
		    }
		  }
		}

	/**
	 * Removes the current active PACurveMaker instance, flagged by a highlighted brush stroke,
	 * from brushShapesList, if there is one.
	 */
	public void removeActiveBrush() {
		if (brushShapesList != null) {
			// remove the active (highlighted) brush
			if (!brushShapesList.isEmpty()) {
				int idx = brushShapesList.indexOf(activeBrush);
				brushShapesList.remove(activeBrush);
				if (brushShapesList.size() == idx)
					curveMaker = null;
				// println("-->> removed activeBrush");
			}
		}
	}
	

	/**
	 * Removes the newest PACurveMaker instance, shown as a brush stroke
	 * in the display, from brushShapesList.
	 */
	public void removeNewestBrush() {
		if (brushShapesList != null) {
			// remove the most recent addition
			if (!brushShapesList.isEmpty()) {
				int idx = brushShapesList.size();
				brushShapesList.remove(idx - 1);	// brushShapes array starts at 0
				println("-->> removed newest brush");
				curveMaker = null;
			}
		}
	}

	
	/**
	 * Removes the oldest brush in brushShapesList.
	 */
	public void removeOldestBrush() {
		if (brushShapesList != null) {
			// remove the oldest addition
			if (!brushShapesList.isEmpty()) {
				brushShapesList.remove(0);		// brushShapes array starts at 0
				if (brushShapesList.isEmpty())
					curveMaker = null;
			}
		}
	}
	

	/*             END DRAWING METHODS              */
	
	
	// ------------------------------------------- //
	//            WINDOWED BUFFER CLASS            //
	// ------------------------------------------- //

	public class WindowedBuffer {
	    private final float[] buffer;   // circular source
	    private final float[] window;   // reusable window array
	    private final int windowSize;   // number of samples in a window
	    private int hopSize;      		// step between windows
	    private int index = 0;          // current start position in buffer

	    public WindowedBuffer(float[] buffer, int windowSize, int hopSize) {
	        if (buffer.length == 0) {
	            throw new IllegalArgumentException("Buffer must not be empty");
	        }
	        if (windowSize <= 0) {
	            throw new IllegalArgumentException("Window size must be positive");
	        }
	        if (hopSize <= 0) {
	            throw new IllegalArgumentException("Hop size must be positive");
	        }
	        this.buffer = buffer;
	        this.windowSize = windowSize;
	        this.hopSize = hopSize;
	        this.window = new float[windowSize];
	    }

	    /**
	     * Returns the next window, advancing the read index by hopSize.
	     * Wraps around the buffer as needed.
	     */
	    public float[] nextWindow() {
	        int bufferLen = buffer.length;
	        // Copy first chunk
	        int firstCopyLen = Math.min(windowSize, bufferLen - index);
	        System.arraycopy(buffer, index, window, 0, firstCopyLen);
	        // Wrap if needed
	        if (firstCopyLen < windowSize) {
	            int remaining = windowSize - firstCopyLen;
	            System.arraycopy(buffer, 0, window, firstCopyLen, remaining);
	        }
	        // Advance start index
	        index = (index + hopSize) % bufferLen;
	        return window;
	    }

 	    /**
	     * Returns the window at a supplied index. Wraps around the buffer as needed.
		 * Updates current index and advances it by hopSize.
	     */
	    public float[] gettWindowAtIndex(int idx) {
	    	int len = buffer.length;
	    	idx = ((idx % len) + len) % len; // normalize to 0..len-1
	    	setIndex(idx);
	        return nextWindow();
	    }

	    /** Reset reader to start of buffer */
	    public void reset() {
	        index = 0;
	    }

	    /** Current buffer index */
	    public int getIndex() {
	        return index;
	    }
	    /** set current index */
	    public void setIndex(int index) {
			this.index = index % buffer.length;
		}

		/** Expose the underlying array size */
	    public int getBufferSize() {
	        return this.buffer.length;
	    }

		/** Expose the reusable window array size */
	    public int getWindowSize() {
	        return windowSize;
	    }

	    /** Expose hop size */
	    public int getHopSize() {
	        return hopSize;
	    }

		public void setHopSize(int hopSize) {
			this.hopSize = hopSize;
		}

	 }	
		
	
	// ------------------------------------------- //
	//               UTILITY METHODS               //
	// ------------------------------------------- //
	

}


