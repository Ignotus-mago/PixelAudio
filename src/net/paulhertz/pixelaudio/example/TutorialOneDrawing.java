/*
 * This example application continues the Tutorial One sequence for the PixelAudio
 * library for Processing. To the previous tools for reading and writing and transcoding
 * audio and image files, triggering audio events, and animating pixels to change
 * audio sample order, it adds the capability of drawing in the display window to 
 * create brush shapes that can be used to trigger audio events. 
 * 
 * The drawing tools and commands are a substantial addition. To implement them 
 * we call on a whole new package of code, net.paulhert.pixelaudio.curves. We make 
 * numerous edits to the previous tutorial, TutorialOneAnimation:
 *     
 *     -- We add a raft of variables to handle drawing.
 *        Here's an outline of how to begin drawing:
 *        
 *     1. Open an audio file with the 'o' key command. The audio file is read into
 *        the audio buffer, playBuffer, It is transcoded as pixel data to the display.
 *        The transcoded data replaces the color pattern that shows the signal path.
 *        Similarly, if you open an image file, it will open in the display and be 
 *        transcoded into the audio buffer, possibly with very noisy results. 
 *        You can turn this simultaneous loading off with the 'L' key and use the
 *        'w' and 'W' keys to write the display to audio or audio to the display.
 *        
 *     2. Press the 'k' key. The color pattern reappears, overlaid on the signal image. 
 *        The overlay works by copying the Hue and Saturation channels of the color image
 *        and combining them with the Brightness channel of the display. See the 
 *        applyColor() and chooseColorImage() methods for the code. You can also load
 *        color data from an image with the 'c' key command.
 *        
 *     3. Click on the display to trigger audio events. They should be fairly quiet: the 
 *        gain for audio output is set to -18.0 dB. The sound will get loud when you draw.
 *        
 *     4. Press 'd' to set isDrawMode to true. Press and drag the mouse to draw a line.
 *        Release the mouse to create a brushstroke -- audio and animation will play.
 *        When you drag the mouse, the sketch accumulates non-repeating points into an 
 *        ArrayList of PVector, allPoints. When you release the mouse, PACurverMaker and 
 *        PACurveMakerUtility process the points:         
 *          - The accumulated points in allPoints are used to create a PACurveMaker object.
 *          - PACurveMaker uses the Ramer-Douglas-Peucker algorithm to reduce the number of points. 
 *          - PACurveMaker uses the reduced point set to generate a Bezier curve.
 *          - The Bezier curve is used to model a brushstroke shape.
 *        All these data elements are available from the PACurveMaker instance. PACurveMaker
 *        also stores time data for drawing, but we don't use it in this sketch. 
 *        The PACurveMaker instance is added to a list of PACUrveMakers, brushShapesList.
 *        BrushShapeList handles drawing curves to the screen. 
 *        
 *     5. Draw some more brushstrokes. Press 'd' to turn drawing off.
 *     
 *     6. Click on a brushstroke to activate it. 
 *     
 *     7. Press the 'x' key while hovering over a brushstroke to delete it. 
 *        Pressing 'x' when you aren't hovering over a brushstroke will delete the oldest 
 *        brushstroke. Pressing 'X' will delete the most recent brushstroke. 
 *     
 *     8. Press'p' to play the brushstrokes one after another, in the order you drew them.
 *          
 *          
 *     -- We added several new methods, in the DRAWING METHODS section.
 *        See the JavaDocs information for each method for a description of what it does.
 *        The initDrawing(), handleDrawing() and handleMousePressed() are new methods that 
 *        are central to interaction and drawing. 
 *        
 *        SEE the JavaDoc entries for PACurveMaker, PACurveMakerUtility, PABezShape and the
 *        other classes in the net.paulhertz.pixelaudio.curves package for more information
 *        about curve modeling. 
 *        
 *     -- We reconfigured code for handling mouse events and TimedLocation events. The mouse event
 *        tracking starts in mousePressed(), when isDrawMode == true. While the mouse is down, calls
 *        to handleDrawing() in the draw() method accumulate points to allPoints. When the mouse is 
 *        released, allPoints is used to initialize a PACurveMaker. 
 *                
 *     -- In the draw() method, a call to handleDrawing() draws brush shapes and detects user interaction
 *        with the brush shapes by calling drawBrushShapes(). If a brush shape is flagged as active, a
 *        mousePressed event can trigger its audio and animation events by calling handleMousePressed().
 *        This will generate TimedLocation objects that trigger audio and animation events. 
 *        
 *     -- There are now two lists of TimedLocation events, timeLocsArray for point events, just like in 
 *        earlier versions of this tutorial, and curveTLEvents for brush events. The handleDrawing() method
 *        takes care of both lists with calls to runCurveEvents() and runPointEvents().
 *        
 *     -- Mouse clicks that don't involve drawing are passed to audioMousePressed(), used in previous tutorials. 
 *     
 *     >> NOTE: TimedLocation is slated for further development and will probably become part of the PixelAudio library.
 *        
 *     -- We added blending/mixing features that can operate when you open audio or image files.
 *        Turn the feature on by pressing the 'j' key. It's off by default. See the loadAudioFile() and 
 *        loadImageFile() methods for the code. 
 *        
 *     
 * AUDIO EVENTS
 *     
 * In this tutorial we introduce a new class for triggering audio events, PASamplerInstrumentPool.
 * In previous tutorials, we just used PASamplerInstrument, which can support multiple voices and 
 * works well with events triggered by clicking the mouse. Events triggered by drawing add a new
 * level of complication -- we can automatically trigger dozens of event at once. PASamplerInstrument
 * and Minim's AudioSampler, which it wraps, appeared to have practical limits to the number of 
 * voices they could support without distortion. PASamplerInstrumentPool allocates a PASamplerInstrument
 * from a previously created list or "pool" of instruments to handle each request for an audio event. 
 * So far, this is the best solution I have for playing many voices together.
 * 
 * If you would like to hear the difference between multi-voice PASamplerIntrument and PASamplerInstrumentPool,
 * just press the '!' key while one or more brushstrokes are playing.
 * 
 * Remaining in the tutorial are TutorialOne_05_UDP, which implements networked communication with Max 
 * and other media applications, and MusicWindowBox, a sketch that implements a windowed buffer for 
 * traversing an audio file plus various features useful in performance.
 *        
 * 
 * Here are the key commands for this sketch:
 * 
 * Press the UP arrow to increase audio output gain by 3.0 dB.
 * Press the DOWN arrow to decrease audio output gain by 3.0 dB.
 * Press ' ' to toggle animation.
 * Press 'o' to open an audio or image file in all RGB channels.
 * Press 'r' to open an audio or image file in the RED channel of the image.
 * Press 'g' to open an audio or image file in the GREEN channel of the image.
 * Press 'b' to open an audio or image file in the BLUE channel of the image.
 * Press 'h' to open an audio or image file in the HSB Hue channel of the image.
 * Press 'v' to open an audio or image file in the HSB Saturation channel of the image.
 * Press 'l' to open an audio or image file in the HSB Brightness channel of the image.
 * Press 'a' to toggle blending on or off.
 * Press 'O' to reload the most recent image or audio file or show an Open File dialog.
 * Press 'c' to apply color from an image file to the display image.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage .
 * Press 'm' to remap the histogram of the image.
 * Press '=' to use a gamma function to make the image lighter.
 * Press '-' to use a gamma function to make the image darker.
 * Press 'S' to save the audio signal to an audio file.
 * Press 's' to save the image to a PNG file.
 * Press 'f' to print the current frame rate to the console.
 * Press 'w' to transcode the image and write it to the audio signal.
 * Press 'W' to transcode the audio signal and write it to the image.
 * Press '?' to show the Help Message in the console.
 * 
 * 
 * REVISIONS
 * 
 * Rendering rule: mapper.mapSigToImgShifted(audioSignal, mapImage.pixels, chan, totalShift);
 * CLick rule: int signalPos = mapper.lookupSignalPosShifted(x, y, totalShift);
 * 
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
import java.util.List;
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
import net.paulhertz.pixelaudio.schedule.*;
import net.paulhertz.pixelaudio.voices.*;
import net.paulhertz.pixelaudio.granular.*;

//audio library
import ddf.minim.*;

//video export library
import com.hamoid.*;


public class TutorialOneDrawing extends PApplet {
	
	/* ------------------------------------------------------------------ */
	/*                       PIXELAUDIO VARIABLES                         */
	/* ------------------------------------------------------------------ */
	
	PixelAudio pixelaudio;     // our shiny new library
	MultiGen multigen;         // a PixelMapGen that links together multiple PixelMapGens
	int genWidth = 512;        // width of multigen PixelMapGens
	int genHeight = 512;       // height of  multigen PixelMapGens
	PixelAudioMapper mapper;   // object for reading, writing, and transcoding audio and image data
	int mapSize;               // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
	PImage baseImage;          // unchanging source image
	PImage mapImage;           // image for display, may be animated
	PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;    // or ChannelNames.L (HSB brightness channel)
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
	
	boolean isLoadToBoth = true;    // if true, load newly opened file both to audio and to video	
    boolean isBlending = false;     // flags blending of newly opened audio or image file with display/buffer 

    String daPath = "/Users/paulhz/Code/Workspace/PixelAudio/examples/examples_data/";    // system-specific path to example files data


    /* ------------------------------------------------------------------ */
	/*                          AUDIO VARIABLES                           */
	/* ------------------------------------------------------------------ */
	
	/*
	 * Audio playback support is added with the audio variables and audio methods. 
	 * You will also need the ADSRParams, PASamplerInstrument, PASamplerInstrumentPool
	 * and TimedLocation classes. In setup(), call initAudio(), then add
	 * a mousePressed() method that calls audioMousePressed(mouseX, mouseY)
	 * and call runTimeArray() in your draw method. 
	 */

	// ====== Minim audio library ====== //
    
	Minim minim;					// library that handles audio 
	AudioOutput audioOut;			// line out to sound hardware
	boolean isBufferStale = false;	// flags that audioBuffer needs to be reset, not used in TutorialOneDrawing
	float sampleRate = 44100;       // sample rate of audioOut
	float fileSampleRate;           // sample rate of most recently opened file
	float[] audioSignal;			// the audio signal as an array of floats
	MultiChannelBuffer playBuffer;	// a buffer for playing the audio signal
	int samplePos;                  // an index into the audio signal, selected by a mouse click on the display image
	int audioLength;				// length of the audioSignal, same as the number of pixels in the display image

	// ====== Sampler Synth ====== //

	// SampleInstrument setup
	int noteDuration = 1000;        // average sample synth note duration, milliseconds
	int samplelen;                  // calculated sample synth note length, samples
	float synthGain = 0.875f;       // gain setting for audio playback instrument, decimal value 0..1
	float outputGain = -6.0f;       // gain setting for audio output, decibels
	boolean isMuted = false;
	PASamplerInstrument synth;      // local class to wrap audioSampler
	PASamplerInstrumentPool pool;   // an allocation pool of PASamplerInstruments
	int maxVoices = 32;
	boolean isUseSynth = false;     // switch between pool and synth, pool is preferred

	// ADSR and its parameters
	ADSRParams defaultEnv;			// good old attack, decay, sustain, release
	ADSRParams granularEnv;         // envelope for a granular-style series of samples
	float maxAmplitude = 0.75f;     // 0..1
	float attackTime = 0.2f;        // seconds
	float decayTime = 0.0f;         // seconds
	float sustainLevel = 0.75f;     // 0..1, same as maxAmplitude
	float releaseTime = 0.5f;       // seconds, same as attack
	float pitchScaling = 1.0f;      // factor for changing pitch
	float defaultPitchScale = 1.0f; // pitch scaling factor
	float lowPitchScaling = 0.5f;   // low pitch scaling, 0.5 => one octave down
	float highPitchScaling = 2.0f;  // high pitch scaling, 2.0 => one octave up
	
    // toggle for sampler events with a "granular" envelope
    boolean useGranularEnv = false; // sampler mode uses granularEnv + grainDuration spacing
    int granEnvDuration = 120;      // envelope duration in ms

    // ====== Granular Synth ====== //
    
    public float[] granSignal;                  // buffer source for granular (defaults to audioSignal)
    public PAGranularInstrument gSynth;         // granular synthesis instrument
    public PAGranularInstrumentDirector gDir;   // director of granular events
    // parameters for granular synthesis
    GestureGranularParams gParams = GestureGranularParams.builder()
            .grainLengthSamples(4096)
            .hopLengthSamples(1024)
            .gainLinear(0.9f)
            .looping(false)
            .env(null)
            .hopMode(GestureGranularParams.HopMode.GESTURE)
            .build();

    public int curveSteps = 16;                 // number of steps in curve representation of a brushstroke path
    public int granLength = 4096;               // length of grain, in samples
    public int granHop = 1024;                  // distance between FIXED mode grains, in samples
    public int gMaxVoices = 64;                 // number of voices managed by a PAGranularInstrument (gSynth)


    /* ------------------------------------------------------------------ */
	/*                   ANIMATION AND VIDEO VARIABLES                    */
	/* ------------------------------------------------------------------ */
	
    int shift = 1024;                    // number of pixels to shift the animation
    int totalShift = 0;                  // cumulative shift
    int totalShiftCache;                 // tracking variable for change in totalShift
    boolean isAnimating = false;         // do we run animation or not?
    boolean oldIsAnimating;              // keep track of animation state when opening a file
    // animation variables for video recording
    int videoSteps = 720;                // how many steps in an animation loop
    boolean isRecordingVideo = false;    // are we recording? (only if we are animating)
    int videoFrameRate = 24;             // frames per second
    int step;                            // number of current step in animation loop
    VideoExport videx;                   // hamoid library class for video export (requires ffmpeg)
    

    /* ------------------------------------------------------------------ */
	/*                         DRAWING VARIABLES                          */
	/* ------------------------------------------------------------------ */
	
	/*
	 * Drawing uses classes in the package net.paulhertz.pixelaudio.curves to store drawing data
	 * and draw Bezier curves and lines. It also provides very basic brushstroke modeling code. 
	 * Unlike most of the code in PixelAudio, which avoids dependencies on Processing, the 
	 * curves.* classes interface with Processing to draw to PApplets and PGraphics instances. 
	 * See the CurveMaker class for details of how drawing works. 
	 */
	    
	int sampleX;                         // x-coordinate associated with mouse click and audio samples
	int sampleY;                         // y-coordinate associated with mouse click and audio samples
	
    PACurveMaker curveMaker;             // a PACurveMaker instance for use when drawing a new brushstroke
    public boolean isDrawMode = false;   // flag that drawing is enabled
    public float epsilon = 4.0f;         // variable that controls point reduction in PACurveMaker
    
    public PVector currentPoint = new PVector(-1, -1);  // point to add to brushstroke points
    public ArrayList<PVector> allPoints; // in-progress brushstroke points
    public ArrayList<Integer> allTimes;  // in-progress brushstroke times
    public int startTime;                // in-progress brushstroke start time
    
    public int dragColor = color(233, 199, 89, 128);    // color of line while dragging the mouse
    public float dragWeight = 8.0f;                     // line weight

    public int polySteps = 5;                           // polygon steps for brush, used by PACurveMaker

    /** AudioBrushLite class wraps around a PACurveMaker and a BrushConfig */   
    public ArrayList<AudioBrushLite> brushes;
    // Hover + selection
    public AudioBrushLite hoverBrush = null;            // brush the mouse is over, if there is one
    public int hoverIndex = -1;                         // index of the brush in the brushes list
    public AudioBrushLite activeBrush = null;           // “selected” brush (optional but useful)

    // ====== Visual styling for brushstrokes ====== //
    
    int readyBrushColor = color(34, 89, 55, 233);       // color for a brushstroke when available to be clicked
    int hoverBrushColor = color(144, 89, 55, 233);      // color for brushstroke under the cursor
    int selectedBrushColor = color(233, 199, 144, 233); // color for the selected (clicked or currently active for editing) brushstroke
    
	int readyBrushColor1 = color(34, 55, 89, 178);		// color for a brushstroke when available to be clicked
	int hoverBrushColor1 = color(55, 89, 144, 178);     // color for brushstroke under the cursor
	int selectedBrushColor1 = color(199, 123, 55, 233);	// color for the selected (clicked or currently active for editing) brushstroke
	int readyBrushColor2 = color(55, 34, 89, 178);		// color for a brushstroke when available to be clicked
	int hoverBrushColor2 = color(89, 55, 144, 178);     // color for brushstroke under the cursor
	int selectedBrushColor2 = color(123, 199, 55, 233);	// color for the selected (clicked or currently active for editing) brushstroke

	int lineColor = color(233, 220, 178);               // brushstroke interior line color
	int dimLineColor = color(178, 168, 136);            // brushstroke interior line color, dimmed
	int circleColor = color(233, 178, 144);             // brushstroke interior point color
	int dimCircleColor = color(178, 168, 136);          // brushstroke interiro point color, dimmed

    // TimedLocation events
    public ArrayList<TimedLocation> samplerTimeLocs;    // a list of timed events for Sampler brushes
	ArrayList<TimedLocation> pointTimeLocs;             // a list of timed events for mouse clicks
	ArrayList<TimedLocation> grainTimeLocs;             // a list of timed events for Granular brushes
    int eventStep = 90;                                 // ms spacing for constant interval sampler “path events”
    int animatedCircleColor = color(233, 220, 199);     // color for animated circles when playing audio   

    boolean isIgnoreOutsideBounds = true;               // not used in TutorialOneDrawing, regulates brushstroke drawing
        

    /* ------------------------------------------------------------------ */
	/*                       INTERACTION VARIABLES                        */
	/* ------------------------------------------------------------------ */

    // ====== Minimal forward-compatible config model ====== //
    // GesturePlayground example provides a complete configuration model with the GestureGranularConfig class
    
    public enum PathMode { ALL_POINTS, REDUCED_POINTS, CURVE_POINTS }    // select model for brush shape in PACurveMaker
    public enum BrushOutput { SAMPLER, GRANULAR }                        // select audio synthesis model
    public enum HopMode { GESTURE, FIXED }                               // select audio gesture sequence timing model

    /**
     * Defines the curve drawing model for a brush using PACurveMaker. 
     */
    public static final class BrushConfig {
        public PathMode pathMode = PathMode.ALL_POINTS;
        public float rdpEpsilon = 4.0f;
        public int curveSteps = 16;
        // leave room for future compatibility (gainDb etc.)
    }

    /**
     * A light-weight version of the AudioBrush class for combining gestures and audio synthesis.
     * PACurveMaker stores gesture points and times provides various curve and polygon models for 
     * drawing brushes and outputting GestureSchedule objects for audio events. AudioBrushLite
     * combines PACurveMaker brush modeling with basic audio synthesis parameters. 
     * AudioBrush combines PACurveMaker with GestureGranularConfig, to provide just about every parameter
     * you could ever need for sampler or granular synthesis. See GesturePlayground for an example. 
     * 
     */
    public static final class AudioBrushLite {
        private final PACurveMaker curve;
        private final BrushConfig cfg;
        private BrushOutput output;

        public AudioBrushLite(PACurveMaker curve, BrushConfig cfg, BrushOutput output) {
            this.curve = curve;
            this.cfg = cfg;
            this.output = output;
        }
        public PACurveMaker curve() { return curve; }
        public BrushConfig cfg() { return cfg; }
        public BrushOutput output() { return output; }
        public void setOutput(BrushOutput out) { this.output = out; }
    }

    // Hit-test result, matching GesturePlayground’s style
    public static final class BrushHit {
        public final AudioBrushLite brush;
        public final int index;
        public BrushHit(AudioBrushLite brush, int index) {
            this.brush = brush;
            this.index = index;
        }
    }
    	
	

    // ---------------- APPLICATION ---------------- //
	
	public static void main(String[] args) {
		PApplet.main(new String[] { TutorialOneDrawing.class.getName() });
	}

	public void settings() {
		size(3 * genWidth, 2 * genHeight);
	}
	
	public void setup() {
		// a standard animation framerate
		frameRate(24);
		// initialize our library
		pixelaudio = new PixelAudio(this);
		// create a PixelMapGen subclass such as MultiGen, with dimensions equal to the display window
		// the call to hilbertLoop3x2 produces a MultiGen that is 3 * genWidth x 2 * genHeight, where
		// genWidth == genHeight and genWidth is a power of 2 (a restriction on Hilbert curves)
		// multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
		// Here's an alternative multigen that is good for visualizing audio location within the image
		multigen = HilbertGen.hilbertRowOrtho(6, 4, width/6, height/4);
		// create a PixelAudioMapper to handle the mapping of pixel colors to audio samples
		mapper = new PixelAudioMapper(multigen);
		// area of the PixelAudioMapper == number of pixels in display == number of samples in audio buffer
		mapSize = mapper.getSize();
		// create an array of rainbow colors with mapSize elements
		colors = getColors(mapSize);
		// initialize various aspects of this application
		initImages();
		initAudio();
		initDrawing();
		showHelp();
		preloadFiles(daPath);    // handy when debugging
	}

	/**
	 * turn off audio processing when we exit
	 */
	public void stop() {
		if (synth != null) synth.close();
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
	 * Initializes mapImage with the colors array. 
	 * MapImage handles the color data for mapper and also serves as our display image.
	 */
	public void initImages() {
		baseImage = createImage(width, height, ARGB);
		mapImage = createImage(width, height, ARGB);
		mapImage.loadPixels();
		mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
		mapImage.updatePixels();
	}
	
	/**
	 * Initializes drawing and drawing interaction variables. 
	 */
	public void initDrawing() {
	    currentPoint = new PVector(-1, -1);
	    brushes = new ArrayList<>();
	    hoverBrush = null;
	    hoverIndex = -1;
	    activeBrush = null;
	    samplerTimeLocs = new ArrayList<>();
	}
	
	// Processing provides access to local data folder, in Eclipse we use full paths TODO
	public void preloadFiles(String path) {
		// the audio file we want to open on startup
		File audioSource = new File(path +"Saucer_mixdown.wav");
		// load the file into audio buffer and Brightness channel of display image (mapImage)
		// if audio is also loaded to the image, will set baseImage to the new image 
		fileSelected(audioSource);
		// overlay colors on mapImage
		mapImage.loadPixels();
		applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
		mapImage.updatePixels();
		commitMapImageToBaseImage();
	}

	public void draw() {
		image(mapImage, 0, 0);    // draw mapImage to the display window
		handleDrawing();          // handle interactive drawing and audio events created by drawing
		if (isAnimating) {        // animation is different from windowed buffer
			animate();            // rotate mapImage.pixels by shift number of pixels
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
		if (step >= videoSteps) {
			if (isRecordingVideo) {
				isRecordingVideo = false;
				videx.endMovie();
				isAnimating = oldIsAnimating;
				println("--- Completed video at frame " + videoSteps);
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
				println("-- video recording frame " + step + " of " + videoSteps);
			}
		}
	}
	
	/**
	 * Renders a frame of animation: moving along the signal path, copies baseImage pixels into rgbSignal 
	 * and writes them back to mapImage along the signal path using incremented totalShift to do animation.
	 * 
	 * @param step   current animation step
	 */
	public void renderFrame(int step) {
		// keep track of how much the pixel array is shifted
		totalShift = PixelAudioMapper.wrap(totalShift + shift, mapSize);
		// write the pixels in rgbSignal to mapImage, following the signal path
		mapImage.loadPixels();
		mapper.copyPixelsAlongPathShifted(baseImage.pixels, mapImage.pixels, totalShift);
		mapImage.updatePixels();
	}

	/**
	 * Handles user's drawing actions, draws previously recorded brushstrokes, 
	 * tracks and generates animation and audio events. 
	 */
	public void handleDrawing() {
	    // 1) draw existing brushes
	    drawBrushShapes();
	    // 2) update hover state (pure state update)
	    updateHover();
	    // 3) if in the process of drawing, accumulate points while mouse is held down
	    if (isDrawMode) {
	        if (mousePressed) {
	            addPoint(clipToBounds(mouseX), clipToBounds(mouseY));
	        }
	        if (allPoints != null && allPoints.size() > 2) {
	            PACurveUtility.lineDraw(this, allPoints, dragColor, dragWeight);
	        }
	    }
	    // 4) run scheduled events
	    runSamplerBrushEvents();
	    runPointEvents();
	    runGrainEvents();
	}
	
	BrushHit findHoverHit() {
	    if (brushes == null || brushes.isEmpty()) return null;    // no brushes
	    // check from topmost (end) to bottom (start)
	    for (int i = brushes.size() - 1; i >= 0; i--) {
	        AudioBrushLite b = brushes.get(i);
	        if (mouseInPoly(b.curve().getBrushPoly())) {
	            return new BrushHit(b, i);
	        }
	    }
	    return null;
	}

	void updateHover() {
	    BrushHit hit = findHoverHit();
	    if (hit != null) {
	        hoverBrush = hit.brush;
	        hoverIndex = hit.index;
	    } else {
	        hoverBrush = null;
	        hoverIndex = -1;
	    }
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
	 * The built-in mousePressed handler for Processing, clicks are handled in mouseClicked().
	 */
	public void mousePressed() {
	    if (isDrawMode) {
	        initAllPoints();
	    }
	}
	
	public void mouseDragged() {
		// drawing will be handled by the draw() loop
	}
	
	public void mouseReleased() {
	    if (isDrawMode && allPoints != null) {
	        if (allPoints.size() > 2) {
	            // finalize stroke into a new brush
	            initCurveMakerAndAddBrush();
	        }
	        allPoints.clear();
	        // mouseClicked() handles actual clicks
	    }
	}
	
	public void mouseClicked() {
		BrushHit hit = findHoverHit();
		if (hit != null) {
			activeBrush = hit.brush; // select
			if (activeBrush.output() == BrushOutput.SAMPLER) {
				scheduleSamplerBrushClick(activeBrush);    // Sampler brush clicked event
			} else {
				scheduleGranularBrushClick(activeBrush);   // Granular brush clicked event
			}
			return;
		}
		// click outside any brush
		audioMousePressed(mouseX, mouseY);
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
	 * This opens up many possibilities and a some risks, too.  
	 * 
	 * @param key
	 * @param keyCode
	 */
	public void parseKey(char key, int keyCode) {
		switch(key) {
		case '1': // PathMode ALL_POINTS
		    // if (activeBrush != null) activeBrush.cfg().pathMode = PathMode.ALL_POINTS;
		    if (hoverBrush != null) hoverBrush.cfg().pathMode = PathMode.ALL_POINTS;
	        println("-- hover brush path mode is now " + PathMode.ALL_POINTS);		    
		    break;
		case '2': // PathMode REDUCED_POINTS
		    if (hoverBrush != null) hoverBrush.cfg().pathMode = PathMode.REDUCED_POINTS;
	        println("-- hover brush path mode is now " + PathMode.REDUCED_POINTS);		    
		    break;
		case '3': // PathMode CURVE_POINTS
		    if (hoverBrush != null) hoverBrush.cfg().pathMode = PathMode.CURVE_POINTS;
	        println("-- hover brush path mode is now " + PathMode.CURVE_POINTS);		    
		    break;
		case 't': // toggle brush output sampler/granular
		    if (hoverBrush != null) {
		    	hoverBrush.setOutput(hoverBrush.output() == BrushOutput.SAMPLER ? BrushOutput.GRANULAR : BrushOutput.SAMPLER);
			    hoverBrush.cfg.pathMode = defaultPathModeFor(hoverBrush.output());
		        println("-- hover brush output is now " + hoverBrush.output());
		    }
		    break;
		case 'f':
			if (hoverBrush != null) ;
			break;
		case ' ': //  start or stop animation
			isAnimating = !isAnimating;
			println("-- animation is " + isAnimating);
			break;
		case 'd': // turn drawing on or off
			// turn off animation (if you insist, you can try drawing with it on, just press the spacebar)
			isAnimating = false;
			isDrawMode = !isDrawMode;
			println(isDrawMode ? "----- Drawing is turned on" : "----- Drawing is turned off");
			break;
		case 'c': // apply color from image file to display image
			chooseColorImage();
			break;
		case 'k': // apply the hue and saturation in the colors array to mapImage 
			mapImage.loadPixels();
			applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
			mapImage.updatePixels();
			break;
		case 'j': // turn audio and image blending on or off
			isBlending = !isBlending;
			println("-- isBlending is "+ isBlending);
			break;
		case 'g': // turn "granular" envelope and timing (for drawing) on or off
			useGranularEnv = !useGranularEnv;
			println("-- useGranularEnv is "+ useGranularEnv);
			break;
		case 'n': // normalize the audio buffer to -6.0dB
			audioSignal = playBuffer.getChannel(0);
			normalize(audioSignal, -6.0f);
		    synth.setBuffer(audioSignal, sampleRate);
		    if (pool != null) pool.setBuffer(audioSignal, sampleRate);
		    else pool = new PASamplerInstrumentPool(playBuffer, sampleRate, maxVoices, 1, audioOut, defaultEnv);		    
		    println("---- normalized");
			break;
		case 'L': case 'l': // determine whether to load a new file to both image and audio or not
			isLoadToBoth = !isLoadToBoth;
			String msg = isLoadToBoth ? "loads to both audio and image. " : "loads only to selected format. ";
			println("---- isLoadToBoth is "+ isLoadToBoth +", opening a file "+ msg);
			break;
		case 'o': case 'O': // open an audio or image file
			chooseFile();
			break;
		case 'w': // write the image colors to the audio buffer as transcoded values
			// TODO refactor with loadImageFile() and loadAudioFile() code
			// prepare to copy image data to audio variables
			// resize the buffer to mapSize, if necessary -- signal will not be overwritten
			if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
			audioSignal = playBuffer.getChannel(0);
			// transcode mapImage brightness channel in HSB color space to audio sample values TODO totalShift = 0
			renderMapImageToAudio(PixelAudioMapper.ChannelNames.L);
			commitMapImageToBaseImage();
			// now that the image data has been written to audioSignal, set playBuffer channel 0 to the new audio data
			playBuffer.setChannel(0, audioSignal);
			audioLength = audioSignal.length;
			println("--->> Wrote image to audio as audio data.");
		    // load the buffer of our PASamplerInstrument (it will use a copy)
		    synth.setBuffer(playBuffer);
		    if (pool != null) pool.setBuffer(playBuffer);
		    else pool = new PASamplerInstrumentPool(playBuffer, sampleRate, maxVoices, 1, audioOut, defaultEnv);
		    // because playBuffer is used by synth and pool and should not change, while audioSignal changes
		    // when the image animates, we don't want playBuffer and audioSignal to point to the same array
		    // copy channel 0 of the buffer into audioSignal, truncated or padded to fit mapSize
		    audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
		    granSignal = audioSignal;
		    audioLength = audioSignal.length;
			break;
		case 'W': // write the audio buffer samples to the image as color values
			renderAudioToMapImage(chan);
			println("--->> Wrote audio to image as pixel data.");
			break;
		case 'x': // delete the current active brush shape or the oldest brush shape
			if (activeBrush != null) {
				removeHoveredOrOldestBrush();
			}
			break;
		case 'X': // delete the most recent brush shape
			removeNewestBrush();
			break;
		case '!': // switch pool and synth, secret toggle, not for the help message
			isUseSynth = !isUseSynth;
			msg = isUseSynth ? ", sound will play using synth." : ", sound will play using pool.";
			println("---- isUseSynth = "+ isUseSynth + msg);
			break;
		case 'u': // mute audio
			isMuted = !isMuted;
			if (isMuted) {
				audioOut.mute();
			}
			else {
				audioOut.unmute();
			}
			msg = isMuted ? "muted" : "unmuted";
			println("---- audio out is "+ msg);
			break;
		case 'V': // record a video
			// records a complete video loop with following actions:
			// Go to frame 0, turn recording on, turn animation on,
			// record animSteps number of frames. 
		    // not of much use in this sketch, but here to keep code complete
			step = 0;
			renderFrame(step);
			isRecordingVideo = true;
			oldIsAnimating = isAnimating;
			isAnimating = true;
			break;
		case 'h': case 'H': // show help message in the console
			showHelp();
			break;
		default:
			break;
		}
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
	 * to generate help output, run RegEx search/replace on parseKey case lines with:
	 * // case ('.'): // (.+)
	 * // println(" * Press $1 to $2.");
	 */
	public void showHelp() {
		println(" * Press the UP arrow to increase audio output gain by 3.0 dB.");
		println(" * Press the DOWN arrow to decrease audio output gain by 3.0 dB.");
		println(" * Press ' ' to  start or stop animation.");
		println(" * Press 'd' to turn drawing on or off.");
		println(" * Press 'c' to apply color from image file to display image.");
		println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage .");
		println(" * Press 'j' to turn audio and image blending on or off.");
		println(" * Press 'f' to turn \"granular\" envelope and timing (for drawing) on or off.");
		println(" * Press 'n' to normalize the audio buffer to -6.0dB.");
		println(" * Press 'l' or 'L' to determine whether to load a new file to both image and audio or not.");
		println(" * Press 'p' to play brushstrokes with a time lapse between each.");
		println(" * Press 'o' or 'O' to open an audio or image file.");
		println(" * Press 'w' to write the image colors to the audio buffer as transcoded values.");
		println(" * Press 'W' to write the audio buffer samples to the image as color values.");
		println(" * Press 'x' to delete the current active brush shape or the oldest brush shape.");
		println(" * Press 'X' to delete the most recent brush shape.");
		println(" * Press 'u' to mute audio.");
		println(" * Press 'V' to record a video.");
		println(" * Press 'h' or 'H' to show help message in the console.");
	}

	/**
	 * Utility method for applying hue and saturation values from a source array of RGB values
	 * to the brightness values in a target array of RGB values, using a lookup table to redirect indexing.
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
	 * from an image you supply, such as display image. 
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
				applyImageColor(imageFile, mapImage);
			} else {
				println("----- File is not a recognized image format ending with \"png\", \"jpg\", or \"jpeg\".");
			}
		} else {
			println("----- No file was selected.");
		}
	}
	
    /**
     * Apply the hue and saturation of a chosen image file to the brightness channel of the display image.
     * @param imgFile        selected image file, source of hue and saturation values
     * @param targetImage    target image where brightness will remain unchanged
     */
    public void applyImageColor(File imgFile, PImage targetImage) {
		PImage colorImage = loadImage(imgFile.getAbsolutePath());
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
				println("----- Selected file " + fileName + "." + fileTag + " at "
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
				println("----- File is not a recognized audio format ending with \"mp3\", \"wav\", \"aif\", or \"aiff\".");
			}
		} 
		else {
			println("----- No audio or image file was selected.");
		}
		isAnimating = oldIsAnimating;
	}

	/**
	 * Attempts to load audio data from a selected file into playBuffer, then calls
	 * writeAudioToImage() to transcode audio data and write it to mapImage.
	 * If you want to load the image file and audio file separately, comment out writeAudioToImage(). 
	 * 
	 * @param audFile    an audio file
	 */
	public void loadAudioFile(File audFile) {
		if (isBlending) {
			MultiChannelBuffer buff = new MultiChannelBuffer(1024, 1);
			fileSampleRate =  minim.loadFileIntoBuffer(audioFile.getAbsolutePath(), buff);
			if (fileSampleRate > 0) {
				println("---- file sample rate is "+ this.fileSampleRate);
				// TODO we're ignoring possibly different sampling rates in the playBuffer and buff, does it matter?
				blendInto(playBuffer, buff, 0.5f, false, -12.0f);    // mix audio sources without normalization
			}
		}
		else {
			// read audio file into our MultiChannelBuffer, buffer size will be adjusted to match the file
			fileSampleRate = minim.loadFileIntoBuffer(audFile.getAbsolutePath(), playBuffer);
			// sampleRate > 0 means we read audio from the file
			if (fileSampleRate > 0) {
				println("---- file sample rate is "+ this.fileSampleRate);
				// save the length of the buffer as read from the file, for future use
				this.audioFileLength = playBuffer.getBufferSize();
				// resize the buffer to mapSize, if necessary -- signal will not be overwritten
				if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
				// load the buffer of our PASamplerInstrument (created in initAudio(), on starting the sketch)
			}
		}
		synth.setBuffer(playBuffer, fileSampleRate);
		if (pool != null) pool.setBuffer(playBuffer, fileSampleRate);
		else pool = new PASamplerInstrumentPool(playBuffer, fileSampleRate, maxVoices, 1, audioOut, defaultEnv);
		// because playBuffer is used by synth and pool and should not change, while audioSignal changes
		// when the image animates, we don't want playBuffer and audioSignal to point to the same array
		// so we copy channel 0 of the buffer into audioSignal, truncated or padded to fit mapSize
		audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
		granSignal = audioSignal;
		audioLength = audioSignal.length;
		if (isLoadToBoth) {
			renderAudioToMapImage(chan);
			commitMapImageToBaseImage();
		}
		totalShift = 0;    // reset animation shift when audio is reloaded
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
	
	public void renderAudioToMapImage(PixelAudioMapper.ChannelNames chan) {
	    // Render current audioSignal into mapImage using current mapper & current totalShift
	    writeAudioToImage(audioSignal, mapper, mapImage, chan, totalShift);
	}
	
	/**
	 * Transcodes audio data in sig[] and writes it to color channel chan of img 
	 * using the lookup tables in mapper to redirect indexing. Calls mapper.mapSigToImgShifted(), 
	 * which will throw an IllegalArgumentException if sig.length != img.pixels.length
	 * or sig.length != mapper.getSize(). 
	 * 
	 * @param sig         an array of float, should be audio data in the range [-1.0, 1.0]
	 * @param mapper      a PixelAudioMapper
	 * @param img         a PImage
	 * @param chan        a color channel
	 * @param shift       the number of indices to shift when writing audio
	 */
	public void writeAudioToImage(float[] sig, PixelAudioMapper mapper, PImage img, PixelAudioMapper.ChannelNames chan, int shift) {
		img.loadPixels();
	    mapper.mapSigToImgShifted(sig, img.pixels, chan, shift); // commit current phase
	    img.updatePixels();
	}

	/**
	 * Attempts to load image data from a selected file into mapImage, then calls writeImageToAudio() 
	 * to transcode HSB brightness channel to audio and writes it to playBuffer and audioSignal.
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
		    renderMapImageToAudio(PixelAudioMapper.ChannelNames.L);
		    // now that the image data has been written to audioSignal, set playBuffer channel 0 to the new audio data
		    playBuffer.setChannel(0, audioSignal);
		    audioLength = audioSignal.length;
		    // load the buffer of our PASamplerInstrument (created in initAudio() on starting the sketch)
		    synth.setBuffer(playBuffer);
		    if (pool != null) pool.setBuffer(playBuffer);
		    else pool = new PASamplerInstrumentPool(playBuffer, sampleRate, maxVoices, 1, audioOut, defaultEnv);
		    // because playBuffer is used by synth and pool and should not change, while audioSignal changes
		    // when the image animates, we don't want playBuffer and audioSignal to point to the same array
		    // copy channel 0 of the buffer into audioSignal, truncated or padded to fit mapSize
		    audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
		    granSignal = audioSignal;
		    audioLength = audioSignal.length;
		}
		commitMapImageToBaseImage();
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
	 * This method writes a color channel from an image to playBuffer, fulfilling a 
	 * central concept of the PixelAudio library: image is sound. Calls mapper.mapImgToSig(), 
	 * which will throw an IllegalArgumentException if img.pixels.length != sig.length or 
	 * img.width * img.height != mapper.getWidth() * mapper.getHeight(). 
	 * Sets totalShift = 0 on completion: the image and audio are now in sync. 
	 * 
	 * @param img       a PImage, a source of data
	 * @param mapper    a PixelAudioMapper, handles mapping between image and audio signal
	 * @param sig       an target array of float in audio format 
	 * @param chan      a color channel
	 * @param shift     number of indices to shift 
	 */
	public void writeImageToAudio(PImage img, PixelAudioMapper mapper, float[] sig, PixelAudioMapper.ChannelNames chan, int shift) {
		// If img is the *display* (shifted) image, commit its phase into audio:
		sig = mapper.mapImgToSigShifted(img.pixels, sig, chan, shift);
	}
	
	public void renderMapImageToAudio(PixelAudioMapper.ChannelNames chan) {
		writeImageToAudio(mapImage, mapper, audioSignal, chan, totalShift);
	}
	
	public void commitMapImageToBaseImage() {
		baseImage = mapImage.copy();
		totalShift = 0;
	}
	
	public void commitNewBaseImage(PImage img) {
		baseImage = img.copy();
		mapImage = img.copy();
		totalShift = 0;
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
	 * @throws IOException		an Exception you'll need to handle to call this method
	 * @throws UnsupportedAudioFileException		another Exception
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
		// initialize the MInim library
		minim = new Minim(this);
		// use the getLineOut method of the Minim object to get an AudioOutput object
		this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
		// set the gain lower to avoid clipping from multiple voices (UP and DOWN arrow keys adjust)
		audioOut.setGain(outputGain); 
		println("---- audio out gain is "+ nf(audioOut.getGain(), 0, 2));
		// create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
		// the buffer will not have any audio data -- you'll need to open a file for that
		this.playBuffer = new MultiChannelBuffer(mapSize, 1);
		this.audioSignal = playBuffer.getChannel(0);
		this.granSignal = audioSignal;
		this.audioLength = audioSignal.length;
		// ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
		defaultEnv = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
		granularEnv = new ADSRParams(maxAmplitude, 0.125f, 0.125f, 0, 0);
		// create a PASamplerInstrument, though playBuffer is all 0s at the moment
		// allocate plenty of voices, the drawing interface generates a lot of audio events
		synth = new PASamplerInstrument(playBuffer, audioOut.sampleRate(), maxVoices, audioOut, defaultEnv);
		// initialize mouse event tracking array
		pointTimeLocs = new ArrayList<TimedLocation>();
	}
	
	/**
	 * Typically called from mousePressed with mouseX and mouseY, generates audio events.
	 * At the moment, plays the Sampler synth. TODO granular burst for point event. 
	 * 
	 * @param x    x-coordinate within a PixelAudioMapper's width
	 * @param y    y-coordinate within a PixelAudioMapper's height
	 */
	public void audioMousePressed(int x, int y) {
		int signalPos = mapper.lookupSignalPosShifted(x, y, totalShift);
		// println("-- signalPos = "+ signalPos);
		float panning = map(x, 0, width, -0.8f, 0.8f);		
		if (pool == null || synth == null) {
			println("You need to load an audio file before you can trigger audio events.");
		}
		else {
			if (!useGranularEnv) {
				int len = calcSampleLen();
				samplelen = playSample(signalPos, len, synthGain, defaultEnv, panning);
			}
			else {
				int len = (int)(abs((this.granEnvDuration) * sampleRate / 1000.0f));
				samplelen = playSample(signalPos, len, synthGain, granularEnv, panning);
			}
		}
		int durationMS = (int)(samplelen/sampleRate * 1000);
		pointTimeLocs.add(new TimedLocation(x, y, durationMS + millis() + 50));
	}
			
	/**
	 * Calculates the index of the image pixel within the signal path,
	 * taking the shifting of pixels and audioSignal into account.
	 * See MusicBoxBuffer for use of a windowed buffer in this calculation. 
	 * 
	 * @param x    an x coordinate within mapImage and display bounds
	 * @param y    a y coordinate within mapImage and display bounds
	 * @return     the index of the sample corresponding to (x,y) on the signal path
	 */
	public int getSamplePos(int x, int y) {
	  int pos =  mapper.lookupSignalPosShifted(x, y, totalShift);
	  return pos;
	}

	/**
	 * Draws a circle at the location of an audio trigger (mouseDown event).
	 * @param x		x coordinate of circle
	 * @param y		y coordinate of circle
	 */
	public void drawCircle(int x, int y) {
		//float size = isRaining? random(10, 30) : 60;
		fill(animatedCircleColor);
		noStroke();
		circle(x, y, 16);
	}
	
	
	/*----------------------------------------------------------------*/
	/*       See PASamplerInstrument and PASamplerInstrumentPool      */
	/*       for all the ways you can play a Sampler instrument.      */
	/*       The ones listed here are the most useful of them.        */
	/*----------------------------------------------------------------*/

	/**
	 * Plays an audio sample with default envelope and stereo pan.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param samplelen    length of the sample (will be adjusted)
	 * @param amplitude    amplitude of the sample on playback
	 * @return the calculated sample length in samples
	 */
	public int playSample(int samplePos, int samplelen, float amplitude, float pan) {
		return playSample(samplePos, samplelen, amplitude, defaultEnv, pitchScaling, pan);
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
		return synth.playSample(samplePos, samplelen, amplitude, env, pitchScaling, pan);
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
	  if (isUseSynth) {
		  return synth.playSample(samplePos, samplelen, amplitude, env, pitch, pan);
	  }
	  else {
		  return pool.playSample(samplePos, samplelen, amplitude, env, pitch, pan);
	  }
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

	public PAGranularInstrument buildGranSynth(AudioOutput out, ADSRParams env, int numVoices) {
	    return new PAGranularInstrument(out, env, numVoices);
	}

	void ensureGranularReady() {
		if (gParams == null) {
			ADSRParams granEnv = new ADSRParams(1.0f, 0.005f, 0.02f, 0.75f, 0.025f);
		}
	    if (gSynth == null) {
	        gSynth = buildGranSynth(audioOut, granularEnv, gMaxVoices);
	    }
	    if (granSignal == null) {
	        granSignal = (audioSignal != null) ? audioSignal : playBuffer.getChannel(0);
	    }
	    if (gDir == null) {
	    	gDir = new PAGranularInstrumentDirector(gSynth);
	    }
	}

	public GranularSettings buildGranSettings(int len, int hop, GranularSettings.WindowPreset win) {
	    GranularSettings settings = new GranularSettings();
	    settings.defaultGrainLength = len;
	    settings.hopSamples = hop;
	    settings.selectWindowPreset(win);
	    return settings;
	}

	public PathGranularSource buildPathGranSource(float[] buf, GranularPath camino, GranularSettings settings) {
	    return new PathGranularSource(buf, camino, settings, audioOut.sampleRate());
	}

	//  
	public void playGranular(float[] buf, GranularPath camino, GranularSettings settings, boolean isBuildADSR) {
	    ensureGranularReady();
	    PathGranularSource granSource = buildPathGranSource(buf, camino, settings);
	    if (isBuildADSR) {
	        int steps = Math.max(1, camino.size() - 1);
	        int totalSamples = steps * settings.hopSamples + settings.defaultGrainLength;
	        ADSRParams env = calculateEnvelope(0.0f, totalSamples, audioOut.sampleRate()); // gainDb=0 for tutorial
	        gSynth.play(granSource, 1.0f, 0.0f, env, false);
	    } else {
	        gSynth.play(granSource, 1.0f, 0.0f, null, false);
	    }
	}
	
	public void playGranularGesture(float buf[], GestureSchedule sched, GestureGranularParams params) {
		int[] startIndices = mapper.lookupSignalPosArray(sched.points, totalShift, mapper.getSize());
		println("---> startIndices[0] = "+ startIndices[0] +" for "+ sched.points.get(0).x, sched.points.get(0).y, totalShift, mapper.getSize());
		float[] panPerGrain = new float[sched.size()];
		for (int i = 0; i < sched.size(); i++) {
		    PVector p = sched.points.get(i);
		    // example: map x to [-0.8, +0.8]
		    panPerGrain[i] = map(p.x, 0, width-1, -0.875f, 0.875f);
		}
		//println("\n----->>> playGranularGesture()");
		//debugIndexHeadroom(buf, startIndices, tx);
		//debugTimesMs(sched);
		//println("\n");
		gDir.playGestureNow(buf, sched, params, startIndices, panPerGrain);
	}

	public ADSRParams calculateEnvelope(float gainDb, int totalSamples, float sampleRate) {
	    return calculateEnvelope(gainDb, totalSamples * 1000f / sampleRate);
	}

	public ADSRParams calculateEnvelope(float gainDb, float totalMs) {
	    float attackMS = Math.min(50, totalMs * 0.1f);
	    float releaseMS = Math.min(200, totalMs * 0.3f);
	    float envGain = AudioUtility.dbToLinear(gainDb);
	    return new ADSRParams(envGain, attackMS / 1000f, 0.01f, 0.8f, releaseMS / 1000f);
	}
	
	static void debugIndexHeadroom(float[] buf, int[] startIndices, GestureGranularParams ggp) {
	    int bufLen = buf.length;
	    int grainLen = Math.max(1, ggp.grainLengthSamples);
	    int hop = Math.max(1, ggp.hopLengthSamples);
	    int burst = Math.max(1, ggp.burstGrains);
	    float pitch = (ggp.pitchRatio > 0f) ? ggp.pitchRatio : 1.0f;

	    int indexHop = hop; // your current semantics
	    int need = (int)Math.ceil((grainLen - 1) * pitch) + (burst - 1) * indexHop;

	    int maxStart = bufLen - 2 - need;
	    if (maxStart < 0) maxStart = 0;

	    int over = 0;
	    int maxIdx = Integer.MIN_VALUE;
	    int minIdx = Integer.MAX_VALUE;

	    for (int idx : startIndices) {
	        if (idx > maxStart) over++;
	        if (idx > maxIdx) maxIdx = idx;
	        if (idx < minIdx) minIdx = idx;
	    }

	    System.out.println("-- bufLen=" + bufLen
	        + " grainLen=" + grainLen
	        + " burst=" + burst
	        + " hop=" + hop
	        + " pitch=" + pitch
	        + " need=" + need
	        + " maxStart=" + maxStart);
	    System.out.println("-- startIndices: min=" + minIdx + " max=" + maxIdx
	        + " overMaxStart=" + over + "/" + startIndices.length);
	}

	static void debugTimesMs(GestureSchedule s) {
	    int n = s.size();
	    if (n <= 1 || s.timesMs == null) return;

	    float[] t = s.timesMs;
	    float t0 = t[0];
	    float tLast = t[n - 1];

	    float minDt = Float.POSITIVE_INFINITY;
	    float maxDt = Float.NEGATIVE_INFINITY;
	    int nonInc = 0;
	    int tiny = 0;

	    for (int i = 1; i < n; i++) {
	        float dt = t[i] - t[i - 1];
	        if (dt <= 0f) nonInc++;
	        if (dt >= 0f && dt < 0.1f) tiny++; // <0.1ms buckets into same ~4 samples at 44.1k
	        if (dt < minDt) minDt = dt;
	        if (dt > maxDt) maxDt = dt;
	    }

	    System.out.println("-- sched n=" + n
	        + " spanMs=" + (tLast - t0)
	        + " t0=" + t0 + " tLast=" + tLast);
	    System.out.println("-- dtMs min=" + minDt
	        + " max=" + maxDt
	        + " nonInc=" + nonInc
	        + " tiny(<0.1ms)=" + tiny);
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
	    allPoints = new ArrayList<>();
	    allTimes = new ArrayList<>();
	    startTime = millis();
	    addPoint(PApplet.constrain(mouseX, 0, width - 1), PApplet.constrain(mouseY, 0, height - 1));
	}
		
	/**
	 * While user is dragging the mouses and isDrawMode == true, accumulates new points
	 * to allPoints and event times to allTimes. Sets sampleX, sampleY and samplePos variables.
	 * We constrain points outside the bounds of the display window. An alternative approach 
	 * is be to ignore them (isIgnoreOutsideBounds == true), which may give a more "natural" 
	 * appearance for fast drawing. 
	 */
	public void addPoint(int x, int y) {
	    if (x != currentPoint.x || y != currentPoint.y) {
	        currentPoint = new PVector(clipToBounds(x), clipToBounds(y));
	        allPoints.add(currentPoint);
	        allTimes.add(millis() - startTime);
	    }
	}
		
	public int clipToBounds(int i) {
		return min(max(0, i), width - 1);
	}	
	
	/**
	 * Initializes a PACurveMaker instance with allPoints as an argument to the factory method 
	 * PACurveMaker.buildCurveMaker() and then fills in PACurveMaker instance variables from 
	 * variables in the calling class (TutorialOneDrawing, here). 
	 */
	public void initCurveMakerAndAddBrush() {
	    curveMaker = PACurveMaker.buildCurveMaker(allPoints, allTimes, startTime);
	    // configure from globals
	    curveMaker.setEpsilon(epsilon);
	    curveMaker.setCurveSteps(curveSteps);
	    curveMaker.setTimeOffset(millis() - startTime);
	    curveMaker.calculateDerivedPoints();    // load all internal point lists
	    // Optional: set PACurveMaker brush colors if you want them retained internally
	    curveMaker.setBrushColor(readyBrushColor);
	    curveMaker.setActiveBrushColor(selectedBrushColor);
	    // Create a forward-compatible config object
	    BrushConfig cfg = new BrushConfig();
	    cfg.rdpEpsilon = epsilon;
	    cfg.curveSteps = curveSteps;
	    cfg.pathMode = PathMode.ALL_POINTS; // tutorial default
	    // Default output for newly drawn strokes: SAMPLER (tutorial-centric)
	    AudioBrushLite b = new AudioBrushLite(curveMaker, cfg, BrushOutput.SAMPLER);
	    b.cfg.pathMode = defaultPathModeFor(b.output());
	    brushes.add(b);
	    // Optionally auto-select the new brush
	    activeBrush = b;
	}

	PathMode defaultPathModeFor(BrushOutput out) {
	    return (out == BrushOutput.SAMPLER) ? PathMode.ALL_POINTS : PathMode.CURVE_POINTS;
	}

	public void drawBrushShapes() {
		if (brushes == null || brushes.isEmpty()) return;
		drawBrushes(brushes, readyBrushColor1, hoverBrushColor1, selectedBrushColor1);
	}

	public void drawBrushes(List<AudioBrushLite> list, int readyColor, int hoverColor, int selectedColor) {
		// step through the list of all brushes
		for (int i = 0; i < list.size(); i++) {
			AudioBrushLite b = list.get(i);
			PACurveMaker cm = b.curve();
			boolean isHover = (b == hoverBrush);
			boolean isSelected = (b == activeBrush);
			int fill = readyColor;
			if (isSelected) {
				fill = selectedColor;
				// keep selected brush geometry consistent with cfg
				cm.setEpsilon(b.cfg().rdpEpsilon);
				cm.setCurveSteps(b.cfg().curveSteps);
				cm.calculateDerivedPoints();
			} 
			else if (isHover) {
				fill = hoverColor;
			}
			// brush body
			PACurveUtility.shapeDraw(this, cm.getBrushShape(), fill, fill, 2);
			// overlay points/lines depending on PathMode
			int w = 1, d = 5;
			int lc = isSelected ? lineColor : dimLineColor;
			int cc = isSelected ? circleColor : dimCircleColor;
			// selected the appropriate point set for drawing
			switch (b.cfg().pathMode) {
			case REDUCED_POINTS -> {
				PACurveUtility.lineDraw(this, cm.getReducedPoints(), lc, w);
				PACurveUtility.pointsDraw(this, cm.getReducedPoints(), cc, d);
			}
			case CURVE_POINTS -> {
				PACurveUtility.lineDraw(this, cm.getCurvePoints(), lc, w);
				PACurveUtility.pointsDraw(this, cm.getCurvePoints(), cc, d);
			}
			case ALL_POINTS -> {
				PACurveUtility.lineDraw(this, cm.getAllPoints(), lc, w);
				PACurveUtility.pointsDraw(this, cm.getAllPoints(), cc, d);
			}
			}
		}
	}

	ArrayList<PVector> getPathPoints(AudioBrushLite b) {
		PACurveMaker cm = b.curve();
		return switch (b.cfg().pathMode) {
		case ALL_POINTS -> cm.getAllPoints();
		case REDUCED_POINTS -> cm.getReducedPoints();
		case CURVE_POINTS -> cm.getCurvePoints();
		};
	}

	/**
	 * @param b    an AudioBrushLite instance
	 * @return     GestureSchedule for the current pathMode of the brush
	 */
	public GestureSchedule getScheduleForBrush(AudioBrushLite b) {
		GestureSchedule sched;
		switch (b.cfg().pathMode) {
		case REDUCED_POINTS -> {
			sched = b.curve.getReducedSchedule(epsilon);
		}
		case CURVE_POINTS -> {
			sched = b.curve.getCurveSchedule(epsilon, curveSteps, isAnimating);
		}
		case ALL_POINTS -> {
			sched = b.curve.getAllPointsSchedule();
		}
		default -> {
			sched = b.curve.getAllPointsSchedule();
		}
		}
		return sched;
	}
	
	void scheduleSamplerBrushClick(AudioBrushLite b) {
		if (b == null) return;
		ArrayList<PVector> pts = getPathPoints(b);
		if (pts == null || pts.size() < 2) return;
		GestureSchedule sched = getScheduleForBrush(b);
		storeSamplerCurveTL(sched, millis() + 10);
	}

	public synchronized void storeSamplerCurveTL(GestureSchedule sched, int startTime) {
		if (this.samplerTimeLocs == null) samplerTimeLocs = new ArrayList<>();
		int i = 0;
		// we store the point and the current time + time offset, where timesMs[0] == 0
		for (PVector loc : sched.points) {
			this.samplerTimeLocs.add(new TimedLocation(Math.round(loc.x), Math.round(loc.y), startTime + Math.round(sched.timesMs[i++]), 200));
		}
		Collections.sort(samplerTimeLocs);
	}

	public synchronized void runSamplerBrushEvents() {
	    if (samplerTimeLocs == null || samplerTimeLocs.isEmpty()) return;
	    int currentTime = millis();
	    samplerTimeLocs.forEach(tl -> {
	        if (tl.eventTime() < currentTime) {
	            sampleX = PixelAudio.constrain(Math.round(tl.getX()), 0, width - 1);
	            sampleY = PixelAudio.constrain(Math.round(tl.getY()), 0, height - 1);
	            float panning = map(sampleX, 0, width, -0.8f, 0.8f);
	            int pos = getSamplePos(sampleX, sampleY);
                playSample(pos, calcSampleLen(), synthGain, panning);
	            tl.setStale(true);
	        } 
	        else {
	            return;
	        }
	    });
	    samplerTimeLocs.removeIf(TimedLocation::isStale);
	}

	void scheduleGranularBrushClick(AudioBrushLite b) {
	    if (b == null) return;
	    ArrayList<PVector> pts = getPathPoints(b);
	    if (pts == null || pts.size() < 2) return;
	    ensureGranularReady();
	    float[] buf = (granSignal != null) ? granSignal : audioSignal;
		GestureSchedule sched = getScheduleForBrush(b);
	    playGranularGesture(buf, sched, gParams);
		storeGranularCurveTL(sched, millis() + 10);
	}	

	public synchronized void storeGranularCurveTL(GestureSchedule sched, int startTime) {
		if (this.grainTimeLocs == null) grainTimeLocs = new ArrayList<>();
		int i = 0;
		// we store the point and the current time + time offset, where timesMs[0] == 0
		for (PVector loc : sched.points) {
			this.grainTimeLocs.add(new TimedLocation(Math.round(loc.x), Math.round(loc.y), startTime + Math.round(sched.timesMs[i++]), 200));
		}
		Collections.sort(grainTimeLocs);
	}
		
	/**
	 * Tracks and runs TimedLocation events in the grainLocsArray list, which is 
	 * associated with granular synthesis gestures.
	 */
	public synchronized void runGrainEvents() {
	    if (grainTimeLocs == null || grainTimeLocs.isEmpty()) return;	
	    int t = millis();
		for (Iterator<TimedLocation> iter = grainTimeLocs.iterator(); iter.hasNext();) {
			TimedLocation tl = iter.next();
			int low = tl.eventTime();
			int high = (tl.eventTime() + tl.getDurationMs());
			if (t >= low && t < high) { // event in the interval between low and high
				drawCircle(tl.getX(), tl.getY());
			}
			else {
				if (t >= high) {        // event in the past
					tl.setStale(true);
					iter.remove();
				}
				if (t < low) {          // event in the future
					break;
				}
			}
		}
		// grainLocsArray.removeIf(TimedLocation::isStale);		// not necessary if we remove in loop
	}

	/**
	 * Tracks and runs TimedLocation events in the timeLocsArray list, which is 
	 * associated with mouse clicks that trigger audio a the click point.
	 */
	public synchronized void runPointEvents() {
		int currentTime = millis();
		for (Iterator<TimedLocation> iter = pointTimeLocs.iterator(); iter.hasNext();) {
			TimedLocation tl = iter.next();
			tl.setStale(tl.eventTime() < currentTime);
			if (!tl.isStale()) {
				drawCircle(tl.getX(), tl.getY());
			}
		}
		pointTimeLocs.removeIf(TimedLocation::isStale);		
	}
	

	/**
	 * @param poly    a polygon described by an ArrayList of PVector
	 * @return        true if the mouse is within the bounds of the polygon, false otherwise
	 */
	public boolean mouseInPoly(ArrayList<PVector> poly) {
		return PABezShape.pointInPoly(poly, mouseX, mouseY);
	}

	
	/**
	 * Reinitializes audio and clears event lists. 
	 * @param isClearCurves
	 */
	public void resets() {

	}


	/**
	 * Removes the current active PACurveMaker instance, flagged by a highlighted brush stroke,
	 * from brushShapesList, if there is one.
	 */
	public void removeHoveredOrOldestBrush() {
	    if (brushes == null || brushes.isEmpty()) return;
	    if (hoverBrush != null) {
	        brushes.remove(hoverBrush);
	        if (activeBrush == hoverBrush) activeBrush = null;
	        hoverBrush = null;
	        hoverIndex = -1;
	    } else {
	        brushes.remove(0);
	    }
	}
	
	public void removeNewestBrush() {
	    if (brushes == null || brushes.isEmpty()) return;
	    AudioBrushLite removed = brushes.remove(brushes.size() - 1);
	    if (activeBrush == removed) activeBrush = null;
	    if (hoverBrush == removed) {
	        hoverBrush = null;
	        hoverIndex = -1;
	    }
	}

	/*             END DRAWING METHODS              */
	
	
	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                           UTILITY                              */
	/*                                                                */
	/*----------------------------------------------------------------*/
	

}


