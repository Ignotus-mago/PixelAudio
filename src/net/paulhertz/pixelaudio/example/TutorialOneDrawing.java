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



/**
 * <p>
 * This example application continues the Tutorial One sequence for the PixelAudio
 * library for Processing. To the previous tools for reading and writing and transcoding
 * audio and image files, triggering audio events, and animating pixels to change
 * audio sample order, it adds the capability of drawing in the display window to 
 * capture points used to create brush shapes and to capture timing information used
 * for audio events. In PixelAudio's conceptual framework, the combination of points
 * and times constitutes a "gesture". Along with mapping of points to audio samples
 * using PixelAudioMapper, "gesture" is a core concept of the PixelAudio library. 
 * </p>
 * Check out {@link net.paulhertz.pixelaudio.curves.PAGesture PAGesture} for a formal definition of gesture.
 * <div>
 * <h2>Points + Times = Gestures</h2>
 * <p>
 * The drawing tools and commands are a substantial addition. To implement them 
 * we call on a whole new package of code, {@link net.paulhert.pixelaudio.curves PixelAudio Curves Package}. To turn
 * gestures into timing information that can be used to schedule audio events, particularly
 * with granular sysnthesis, we rely on the {@link net.paulhertz.pixelaudio.schedule PixelAudio Schedule Package}. 
 * The workhorse of the Curves package is {@link net.paulhert.pixelaudio.curves.PACurveMaker PACurveMaker}, which 
 * is used to capture point and time information when you draw. 
 * The unique points that you draw when dragging A PACurveMaker instance is intialized with a list of points and
 * a list of time offsets where both lists have the same number of elements: each point corresponds a unique time,
 * ascending from a start time, 
 * the mouse are stored in PACurveMaker's ALL_POINTS representation of the gesture points. PACurveMaker can also 
 * reduce the number of points captured, using the Ramer-Douglas-Peucker (RDP) algorithm, to create the REDUCED_POINTS
 * representation of a gesture. RDP controls point reduction with a numerical value, <code>epsilon</code>, 
 * which you can vary to control the number of reduced points in a gesture. PACurveMaker can turn the reduced points 
 * representation of a drawn line into a Bezier curve, the CURVE_POINTS representation of the gesture. 
 * </p><p>
 * A PACurveMaker 
 * </p> 
 * <h2>Audio Processing</h2>
 * PAGranularInstrumentDirector sets up the PABurstGranularSource and passes it down the granular synth chain, first 
 * to PAGranularInstrument and then to PAGranularSampler, where the PABurstGranularSource is one of various parameters 
 * to create an AudioScheduler scheduler. In PAGranularSampler.uGenerate(), scheduler.processBlock is called, leftMix 
 * and rightMix are initialized to 0 and the currently active PAGranularVoice instances are summed. After that, uGenerate 
 * does apply power normalization and soft clipping. It look to me that PABurstGranularSource.processBlock handles 
 * the first filling in of arrays (through the available voices) before any normalization or soft clipping.
 * <pre>
 * Here are the key commands for this sketch:
 * 
 * Press ' ' to spacebar triggers a brush if we're hovering over a brush, otherwise it triggers a point event.
 * Press '1' to set brushstroke under cursor to PathMode ALL_POINTS.
 * Press '2' to set brushstroke under cursor to PathMode REDUCED_POINTS.
 * Press '3' to set brushstroke under cursor to PathMode CURVE_POINTS.
 * Press 't' to set brushstroke under cursor to use Sampler or Granular synth.
 * Press 'f' to set brushstroke under cursor to FIXED hop mode for granular events.
 * Press 'g' to set brushstroke under cursor to GESTURE hop mode for granular events.
 * Press 'e' to select granular or sampler synth for click response.
 * Press 'r' to select long or short grain duration .
 * Press 'p' to jitter the pitch of granular gestures.
 * Press 'a' to  start or stop animation (bitmap rotation along the signal path).
 * Press 'd' to turn drawing on or off.
 * Press 'c' to apply color from image file to display image.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage (shifted when animating).
 * Press 'K' to color display with spectrum and write to base image.
 * Press 'j' to turn audio and image blending on or off.
 * Press 'n' to normalize the audio buffer to -6.0dB.
 * Press 'l' or 'L' to determine whether to load a new file to both image and audio or not.
 * Press 'o' or 'O' to open an audio or image file and load to image or audio buffer or both.
 * Press 'w' to write the image HSB Brightness channel to the audio buffer as transcoded sample values .
 * Press 'W' to write the audio buffer samples to the image as color values.
 * Press 'x' to delete the current active brush shape or the oldest brush shape.
 * Press 'X' to delete the most recent brush shape.
 * Press 'u' to mute audio.
 * Press 'V' to record a video.
 * Press 'h' or 'H' to show help message in the console.
 * </pre>
 * </div>
 * 
 * REVISIONS
 * 
 * Rendering rule: mapper.mapSigToImgShifted(audioSignal, mapImage.pixels, chan, totalShift);
 * Click rule: int signalPos = mapper.lookupSignalPosShifted(x, y, totalShift);
 * 
 * 
 */
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
	PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;    // also try ChannelNames.L (HSB brightness channel)
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
	 * You will also need the ADSRParams, PASamplerInstrument, PASamplerInstrumentPool5	 * and TimedLocation classes. In setup(), call initAudio(), then add
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
	float synthGain = 0.75f;         // gain setting for gesture events with Sampler instrument, decimal value 0..1
	float synthPointGain = 0.75f;   // gain for point events with the Sampler instrument
	float outputGain = -6.0f;       // gain setting for audio output, decibels
	boolean isMuted = false;
	PASamplerInstrumentPool pool;   // an allocation pool of PASamplerInstruments
	int samplerMaxVoices = 64;

	// ADSR and its parameters
	float maxAmplitude = 0.875f;         // in the range 0..1, maximum amplitude of envelope for sampler instruments
	// ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
	ADSRParams defaultEnv = new ADSRParams(maxAmplitude, 0.2f, 0.1f, maxAmplitude * 0.75f, 0.5f);
	// ADSRParams granularEnv = new ADSRParams(maxAmplitude, 0.125f, 0.125f, 0, 0);
	float pitchScaling = 1.0f;          // factor for changing pitch
	float defaultPitchScale = 1.0f;     // pitch scaling factor
	float lowPitchScaling = 0.5f;       // low pitch scaling, 0.5 => one octave down
	float highPitchScaling = 2.0f;      // high pitch scaling, 2.0 => one octave up
	boolean usePitchedGrains = false;   // detune a granular gesture, if true
	
    // toggle for sampler events with a "granular" envelope
    boolean useGranularSynth = false;   // which synth do we use for point events (mouse clicks and the like)
    int granEnvDuration = 120;          // envelope duration in ms

    // ====== Granular Synth ====== //
    
    public float[] granSignal;                  // buffer source for granular (defaults to audioSignal)
    public PAGranularInstrument gSynth;         // granular synthesis instrument
    public PAGranularInstrumentDirector gDir;   // director of granular events
    public float granularGain = 0.9f;           // gain for a granular gesture event
    public float granularPointGain = 1.0f;      // gain for a granular point event ("granular burst")
    // parameters for granular synthesis
    boolean useShortGrain = true;               // default to short grains, if true
    int longSample = 4096;                      // number of samples for a moderately long grain
    int shortSample = 512;                     // number of samples for a relatively short grain
    int granSamples = useShortGrain ? shortSample : longSample;    // number of samples in a grain window
    int hopSamples = granSamples/4;             // number of samples between each grain in a granular burst
    GestureGranularParams gParamsGesture;       // granular parameters when hopMode == HopMode.GESTURE
    GestureGranularParams gParamsFixed;         // granular parameters when hopMode == HopMode.FIXED
    public int curveSteps = 16;                 // number of steps in curve representation of a brushstroke path
    public int gMaxVoices = 256;                 // number of voices managed by a PAGranularInstrument (gSynth)
    boolean useLongBursts = false;              // controls the number of burst grain in a point event or gesture event
    int maxBurstGrains = 4;
    int burstGrains = 1;


    /* ------------------------------------------------------------------ */
	/*                   ANIMATION AND VIDEO VARIABLES                    */
	/* ------------------------------------------------------------------ */
    
    // animation consists of rotating pixels / audio samples along the mapper's "signal path"
    // it's an effect that looks glitchy or hypnotic, depending on your source material
    // you an also use it to shift audio samples with respect to brushstrokes
	
    int shift = 1024;                    // number of pixels to shift the animation
    int totalShift = 0;                  // cumulative shift
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
	 * Drawing uses classes in the package net.paulhertz.pixelaudio.curves to store gesture data
	 * and draw Bezier curves and lines. It also provides very basic brushstroke modeling code. 
	 * Unlike most of the code in PixelAudio, which avoids dependencies on Processing, the 
	 * curves.* classes interface with Processing to draw to PApplets and PGraphics instances. 
	 * See the CurveMaker class for details of how drawing works. Because CurveMaker also stores
	 * timing data for each point it stores, it can reproduce gestures as audio events. 
	 */
	    
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

    public ArrayList<AudioBrushLite> brushes;           // a list of AudioBrushLite, a class that wraps a PACurveMaker and a BrushConfig
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

    // ====== Minimal gesture + audio synthesis configuration ====== //
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
     */
    public static final class AudioBrushLite {
        private final PACurveMaker curve;
        private final BrushConfig cfg;
        private BrushOutput output;
        private HopMode hopMode;

        public AudioBrushLite(PACurveMaker curve, BrushConfig cfg, BrushOutput output, HopMode hopMode) {
            this.curve = curve;
            this.cfg = cfg;
            this.output = output;
            this.hopMode = hopMode;
        }
        public PACurveMaker curve() { return curve; }
        public BrushConfig cfg() { return cfg; }
        public BrushOutput output() { return output; }
        public void setOutput(BrushOutput out) { this.output = out; }
        public HopMode hopMode() { return hopMode; }
        public void setHopMode(HopMode hop) { this.hopMode = hop; }
    }

    // A class to store a hit-test result
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
		// set a standard animation framerate
		frameRate(24);
		// 1) Initialize our library.
		pixelaudio = new PixelAudio(this);
		// 2) create a PixelMapGen subclass with dimensions equal to the display window.
		//   the call to "hilbertLoop3x2(genWidth, genHeight)" produces a MultiGen that is 3 * genWidth x 2 * genHeight, 
		//   where genWidth == genHeight and genWidth is a power of 2 (a restriction on Hilbert curves)
		// multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
		//   Here's an alternative multigen that is good for visualizing audio location within the image
		//   It reads left to right, top to bottom, like a musical score. I've divided it 12 x 8, but 6 x 4 is good, too.
		multigen = HilbertGen.hilbertRowOrtho(12, 8, width/12, height/8);
		// 3) Create a PixelAudioMapper to handle the mapping of pixel colors to audio samples.
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
			colorWheel[i] = color(i, 40, 60); // fill our array with colors, gradually changing hue
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
	    currentPoint = new PVector(-1, -1);    // used for drawing to the display
	    brushes = new ArrayList<>();           // keep track of brushstrokes/gestures
	    hoverBrush = null;                     
	    hoverIndex = -1;
	    activeBrush = null;
	    samplerTimeLocs = new ArrayList<>();   // capture timing data when drawing
	}
	
	// Processing provides access to local data folder, in Eclipse we use full paths
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
		// write mapImage to baseImage
		commitMapImageToBaseImage();
	}

	public void draw() {
		image(mapImage, 0, 0);    // draw mapImage to the display window
		handleDrawing();          // handle interactive drawing and audio events created by drawing
		if (isAnimating) {        // animation is different from windowed buffer
			animate();            // do some animation, here it's just to rotate mapImage.pixels by shift number of pixels
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
	 * Renders a frame of animation: moving along the signal path, copies baseImage pixels to
	 * mapImage pixels, adjusting the index position of the copy using totalShift
	 * i.e. we don't actually rotate the pixels, we just shift the position they're copied to
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
	 * Handles user's drawing actions: draws previously recorded brushstrokes, 
	 * tracks and generates interactive animation and audio events. 
	 */
	public void handleDrawing() {
	    // 1) draw existing brushes
	    drawBrushShapes();
	    // 2) update hover state (pure state update, no action)
	    updateHover();
	    // 3) if in the process of drawing, accumulate points while mouse is held down
	    if (isDrawMode) {
	        if (mousePressed) {
	            addPoint(clipToWidth(mouseX), clipToHeight(mouseY));
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
	        initAllPoints();    // start capturing points and times
	    }
	}
	
	public void mouseDragged() {
		// we don't do anything here, drawing will be handled by the draw() loop
	}
	
	public void mouseReleased() {
	    if (isDrawMode && allPoints != null) {
	        if (allPoints.size() > 2) {
	            initCurveMakerAndAddBrush();    // finalize stroke into a new brush
	        }
	        allPoints.clear();                  // clear points accumulated while dragging the mouse
	        // mouseClicked() handles actual clicks
	    }
	}
	
	public void mouseClicked() {
		BrushHit hit = findHoverHit();    // are we over a brush?
		if (hit != null) {                // yes, let's figure out what to do
			activeBrush = hit.brush;      // flag the brush as the activeBrush
			if (activeBrush.output() == BrushOutput.SAMPLER) {
				scheduleSamplerBrushClick(activeBrush);    // Sampler brush clicked event
			} else {
				scheduleGranularBrushClick(activeBrush);   // Granular brush clicked event
			}
			return;
		}
		// click outside any brush is handled here
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
				println("---- audio gain is "+ nf(audioOut.getGain(), 0, 2) +"dB");
			}
			else if (keyCode == DOWN) {
				setAudioGain(g - 3.0f);
				println("---- audio gain is "+ nf(audioOut.getGain(), 0, 2) +"dB");
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
		String msg;
		switch(key) {
		case ' ': // spacebar triggers a brush if we're hovering over a brush, otherwise it triggers a point event
			if (hoverBrush != null) {
				if (hoverBrush.output() == BrushOutput.SAMPLER) {
					scheduleSamplerBrushClick(hoverBrush);    // Sampler brush clicked event
				} else {
					scheduleGranularBrushClick(hoverBrush);   // Granular brush clicked event
				}
			}
			else {
				audioMousePressed(mouseX, mouseY);
			}
			break;
		case '1': // set brushstroke under cursor to PathMode ALL_POINTS
		    // if (activeBrush != null) activeBrush.cfg().pathMode = PathMode.ALL_POINTS;
		    if (hoverBrush != null) hoverBrush.cfg().pathMode = PathMode.ALL_POINTS;
	        println("-- hover brush path mode is now " + PathMode.ALL_POINTS);		    
		    break;
		case '2': // set brushstroke under cursor to PathMode REDUCED_POINTS
		    if (hoverBrush != null) hoverBrush.cfg().pathMode = PathMode.REDUCED_POINTS;
	        println("-- hover brush path mode is now " + PathMode.REDUCED_POINTS);		    
		    break;
		case '3': // set brushstroke under cursor to PathMode CURVE_POINTS
		    if (hoverBrush != null) hoverBrush.cfg().pathMode = PathMode.CURVE_POINTS;
	        println("-- hover brush path mode is now " + PathMode.CURVE_POINTS);		    
		    break;
		case 't': // set brushstroke under cursor to use Sampler or Granular synth
		    if (hoverBrush != null) {
		    	hoverBrush.setOutput(hoverBrush.output() == BrushOutput.SAMPLER ? BrushOutput.GRANULAR : BrushOutput.SAMPLER);
			    hoverBrush.cfg.pathMode = defaultPathModeFor(hoverBrush.output());
		        println("-- hover brush output is now " + hoverBrush.output());
		    }
		    break;
		case 'f': // set brushstroke under cursor to FIXED hop mode for granular events
			if (hoverBrush != null) {
				hoverBrush.hopMode = HopMode.FIXED;
				println("-- hover brush hop mode is now " + hoverBrush.hopMode());
			}
			break;
		case 'g': // set brushstroke under cursor to GESTURE hop mode for granular events
			if (hoverBrush != null) {
				hoverBrush.hopMode = HopMode.GESTURE;
				println("-- hover brush hop mode is now " + hoverBrush.hopMode());
			}
			break;
		case 'e': // select granular or sampler synth for click response
			useGranularSynth = !useGranularSynth;
			String synth = (useGranularSynth) ? "granular." : "sampler.";
			println("-- default synth for click events is "+ synth);
			break;
		case 'r': // select long or short grain duration 
			useShortGrain = !useShortGrain;
			granSamples = useShortGrain ? shortSample : longSample;
			hopSamples = granSamples/4;
			initGranularParams();
			println("-- Granular synth uses grain length = "+ granSamples +" samples with "+ hopSamples +" between each grain");
			break;
		case 'b':
			useLongBursts = !useLongBursts;
			burstGrains = useLongBursts ? maxBurstGrains : 1;
			initGranularParams();
			println(useLongBursts ? "-- using long bursts" : "using a single grain");
			break;
		case 'p': // jitter the pitch of granular gestures
			usePitchedGrains = !usePitchedGrains;
			msg = (usePitchedGrains) ? " jitter granular pitch." : " steady granular pitch.";
			println("-- Play granular synth with "+ msg);
			break;
		case '>': case '.': // increment epsilon value of current brush (reduced points decrease)
			if (hoverBrush != null) {
				float e = hoverBrush.curve.getEpsilon();
				e = e < 32 ? e + 1 : e;
				setBrushEpsilon(hoverBrush, e);
				println("-- epsilon for current brush is "+ hoverBrush.curve.getEpsilon());
			}
			break;
		case '<': case ',': // decrement epsilon value of current brush (reduced points increase)
			if (hoverBrush != null) {
				float e = hoverBrush.curve.getEpsilon();
				e = e > 1 ? e - 1 : e;
				setBrushEpsilon(hoverBrush, e);
				println("-- epsilon for current brush is "+ hoverBrush.curve.getEpsilon());
			}
			break;
		case 'a': //  start or stop animation (bitmap rotation along the signal path)
			isAnimating = !isAnimating;
			println("-- animation is " + isAnimating);
			break;
		case 'd': // turn drawing on or off
			// turn off animation (if you insist, you can try drawing with it on, just press the 'a' key)
			isAnimating = false;
			isDrawMode = !isDrawMode;
			println(isDrawMode ? "----- Drawing is turned on" : "----- Drawing is turned off");
			break;
		case 'c': // apply color from image file to display image
			chooseColorImage();
			break;
		case 'k': // apply the hue and saturation in the colors array to mapImage (shifted when animating)
			mapImage.loadPixels();
			applyColorShifted(colors, mapImage.pixels, mapper.getImageToSignalLUT(), totalShift);
			mapImage.updatePixels();
			break;
		case 'K': // color display with spectrum and write to base image
			// color the base image
			baseImage.loadPixels();
			applyColor(colors, baseImage.pixels, mapper.getImageToSignalLUT());
			baseImage.updatePixels();
			// copy baseImage to the possibly shifted pixels of mapImage
			mapImage.loadPixels();
			mapper.copyPixelsAlongPathShifted(baseImage.pixels, mapImage.pixels, totalShift);
			mapImage.updatePixels();
			break;
		case 'j': // turn audio and image blending on or off
			isBlending = !isBlending;
			println("-- isBlending is "+ isBlending);
			break;
		case 'n': // normalize the audio buffer to -6.0dB
			audioSignal = playBuffer.getChannel(0);
			normalize(audioSignal, -6.0f);
			updateAudioChain(audioSignal);
			ensureSamplerReady();
			ensureGranularReady();
		    println("---- normalized");
			break;
		case 'L': case 'l': // determine whether to load a new file to both image and audio or not
			isLoadToBoth = !isLoadToBoth;
			msg = isLoadToBoth ? "loads to both audio and image. " : "loads only to selected format. ";
			println("---- isLoadToBoth is "+ isLoadToBoth +", opening a file "+ msg);
			break;
		case 'o': case 'O': // open an audio or image file and load to image or audio buffer or both
			chooseFile();
			break;
		case 's': // save display image to a PNG file
			saveToImage();
			break;
		case 'S': // save audio buffer to a .wav file
			saveToAudio();
			break;
		case 'm': // copy the current display image to the baseImage
			commitMapImageToBaseImage();
			break;
		case 'w': // write the image HSB Brightness channel to the audio buffer as transcoded sample values 
			// TODO refactor with loadImageFile() and loadAudioFile() code
			// prepare to copy image data to audio variables
			// resize the buffer to mapSize, if necessary -- signal will not be overwritten
			if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
			audioSignal = playBuffer.getChannel(0);
			// transcode mapImage brightness channel in HSB color space to audio sample values
			renderMapImageToAudio(PixelAudioMapper.ChannelNames.L);
			commitMapImageToBaseImage();
			// now that the image data has been written to audioSignal, set playBuffer channel 0 to the new audio data
			updateAudioChain(audioSignal);
			println("--->> Wrote image to audio as audio data.");
		    // load the buffer of our PASamplerInstrument (it will use a copy)
			ensureSamplerReady();
			ensureGranularReady();
			break;
		case 'W': // write the audio buffer samples to the image as color values
			renderAudioToMapImage(chan, totalShift);
			println("--->> Wrote audio to image as pixel data.");
			break;
		case 'x': // delete the current active brush shape or the oldest brush shape
			if (activeBrush != null) removeHoveredOrOldestBrush();
			break;
		case 'X': // delete the most recent brush shape
			removeNewestBrush();
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
	 * to generate help output, run RegEx search/replace on parseKey case lines with:
	 * // case ('.'): // (.+)
	 * // println(" * Press $1 to $2.");
	 */
	public void showHelp() {
		println(" * Press ' ' to spacebar triggers a brush if we're hovering over a brush, otherwise it triggers a point event.");
		println(" * Press '1' to set brushstroke under cursor to PathMode ALL_POINTS.");
		println(" * Press '2' to set brushstroke under cursor to PathMode REDUCED_POINTS.");
		println(" * Press '3' to set brushstroke under cursor to PathMode CURVE_POINTS.");
		println(" * Press 't' to set brushstroke under cursor to use Sampler or Granular synth.");
		println(" * Press 'f' to set brushstroke under cursor to FIXED hop mode for granular events.");
		println(" * Press 'g' to set brushstroke under cursor to GESTURE hop mode for granular events.");
		println(" * Press 'e' to select granular or sampler synth for click response.");
		println(" * Press 'r' to select long or short grain duration .");
		println(" * Press 'p' to jitter the pitch of granular gestures.");
		println(" * Press 'a' to  start or stop animation (bitmap rotation along the signal path).");
		println(" * Press 'd' to turn drawing on or off.");
		println(" * Press 'c' to apply color from image file to display image.");
		println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage (shifted when animating).");
		println(" * Press 'K' to color display with spectrum and write to base image.");
		println(" * Press 'j' to turn audio and image blending on or off.");
		println(" * Press 'n' to normalize the audio buffer to -6.0dB.");
		println(" * Press 'l' or 'L' to determine whether to load a new file to both image and audio or not.");
		println(" * Press 'o' or 'O' to open an audio or image file and load to image or audio buffer or both.");
		println(" * Press 'w' to write the image HSB Brightness channel to the audio buffer as transcoded sample values .");
		println(" * Press 'W' to write the audio buffer samples to the image as color values.");
		println(" * Press 'x' to delete the current active brush shape or the oldest brush shape.");
		println(" * Press 'X' to delete the most recent brush shape.");
		println(" * Press 'u' to mute audio.");
		println(" * Press 'V' to record a video.");
		println(" * Press 'h' or 'H' to show help message in the console.");
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

	public int[] applyColorShifted(int[] colorSource, int[] graySource, int[] lut, int shift) {
	    if (colorSource == null || graySource == null || lut == null)
	        throw new IllegalArgumentException("colorSource, graySource and lut cannot be null.");
	    if (colorSource.length != graySource.length || colorSource.length != lut.length)
	        throw new IllegalArgumentException("colorSource, graySource and lut must all have the same length.");
	    int n = graySource.length;
	    int s = ((shift % n) + n) % n; // wrap + allow negative shifts
	    float[] hsbPixel = new float[3];
	    for (int i = 0; i < n; i++) {
	        int srcIdx = lut[i] + s;
	        if (srcIdx >= n) srcIdx -= n; // faster than % in tight loop
	        graySource[i] = PixelAudioMapper.applyColor(colorSource[srcIdx], graySource[i], hsbPixel);
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
		float[] resampled;
		MultiChannelBuffer buff = new MultiChannelBuffer(1024, 1);
		fileSampleRate =  minim.loadFileIntoBuffer(audioFile.getAbsolutePath(), buff);
		if (fileSampleRate > 0) {
			println("---- file sample rate is "+ this.fileSampleRate);
			if (fileSampleRate != audioOut.sampleRate()) {
				resampled = AudioUtility.resampleMonoToOutput(buff.getChannel(0), fileSampleRate, audioOut);
				buff.setBufferSize(resampled.length);
				buff.setChannel(0, resampled);
				//if (buff.getBufferSize() != mapSize) buff.setBufferSize(mapSize);
				fileSampleRate = audioOut.sampleRate();
			}
		}
		if (isBlending) {
			blendInto(playBuffer, buff, 0.5f, false, -12.0f);    // mix audio sources without normalization
		}
		else {
			// read audio file into our MultiChannelBuffer, buffer size will be adjusted to match the file
			playBuffer = buff;
			// save the length of the buffer as read from the file, for future use
			this.audioFileLength = playBuffer.getBufferSize();
			// resize the buffer to mapSize, if necessary -- signal will not be overwritten
			if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
			// load the buffer of our PASamplerInstrument (created in initAudio(), on starting the sketch)
		}
		ensureSamplerReady();
		// because playBuffer is used by synth and pool and should not change, while audioSignal changes
		// when the image animates, we don't want playBuffer and audioSignal to point to the same array
		// so we copy channel 0 of the buffer into audioSignal, truncated or padded to fit mapSize
		audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
		granSignal = audioSignal;
		audioLength = audioSignal.length;
		if (isLoadToBoth) {
			renderAudioToMapImage(chan, 0);
			commitMapImageToBaseImage();
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
		
	public static void normalize(float[] signal, float targetPeakDB) {
		AudioUtility.normalizeRmsWithCeiling(signal, targetPeakDB, -3.0f);
	}
		
	public void renderAudioToMapImage(PixelAudioMapper.ChannelNames chan, int shift) {
	    // Render current audioSignal into mapImage using current mapper & current totalShift
	    writeAudioToImage(audioSignal, mapper, mapImage, chan, shift);
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
		    ensureSamplerReady();
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
		// initialize mouse event tracking array
		pointTimeLocs = new ArrayList<TimedLocation>();
		initGranularParams();
	}
	
	public void initGranularParams() {
		ADSRParams env = this.calculateEnvelope(granularGain, 1000);
	    gParamsGesture = GestureGranularParams.builder()
	            .grainLengthSamples(granSamples)
	            .hopLengthSamples(hopSamples)
	            .gainLinear(granularGain)
	            .looping(false)
	            .env(env)
	            .hopMode(GestureGranularParams.HopMode.GESTURE)
	            .burstGrains(burstGrains)
	            .build();
	   gParamsFixed = GestureGranularParams.builder()
	            .grainLengthSamples(granSamples)
	            .hopLengthSamples(hopSamples)
	            .gainLinear(granularGain)
	            .looping(false)
	            .env(env)
	            .hopMode(GestureGranularParams.HopMode.FIXED)
	            .burstGrains(burstGrains)
	            .build();
	}
		
	public void audioMousePressed(int x, int y) {
	    if (!useGranularSynth) {
	    	// use Sampler synthesis instrument
	    	ensureSamplerReady();
	        int signalPos = mapper.lookupSignalPosShifted(x, y, totalShift);
	        float panning = map(x, 0, width, -0.8f, 0.8f);
	        int len = calcSampleLen();
	        samplelen = playSample(signalPos, len, synthPointGain, defaultEnv, panning);
	        int durationMS = (int)(samplelen / sampleRate * 1000);
	        pointTimeLocs.add(new TimedLocation(x, y, durationMS + millis() + 50));
	        return;
	    }
    	// use Granular synthesis instrument
	    ensureGranularReady();
	    float hopMsF = (int)Math.round(AudioUtility.samplesToMillis(hopSamples, sampleRate));
	    final int grainCount = useLongBursts ?  8 * (int) Math.round(noteDuration/hopMsF) : (int) Math.round(noteDuration/hopMsF);
	    println("-- granular point burst with grainCount = "+ grainCount);
	    ArrayList<PVector> path = new ArrayList<>(grainCount);
	    int[] timing = new int[grainCount];
		int sigIndex = mapper.lookupSignalPosShifted(x, y, totalShift);
	    for (int i = 0; i < grainCount; i++) {
	    	if (useLongBursts) {
	    		path.add(getCoordFromSignalPos(sigIndex + hopSamples * i));
	    	}
	    	else {
	    		path.add(this.jitterCoord(x, y, 3));
	    	}
	        timing[i] = Math.round(i * hopMsF);
	    }
	    int startTime = millis() + 10;
	    PACurveMaker curve = PACurveMaker.buildCurveMaker(path);
	    curve.setDragTimes(timing);
	    curve.setTimeStamp(startTime);
	    GestureSchedule sched = curve.getAllPointsSchedule();
	    float[] buf = (granSignal != null) ? granSignal : audioSignal;
	    playGranularGesture(buf, sched, gParamsFixed);
	    storeGranularCurveTL(sched, startTime, false);
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

	public PVector getCoordFromSignalPos(int pos) {
		int[] xy = this.mapper.lookupImageCoordShifted(pos, totalShift);
		return new PVector(xy[0], xy[1]);
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
		return playSample(samplePos, samplelen, amplitude, defaultEnv, 1.0f, pan);
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
		return playSample(samplePos, samplelen, amplitude, env, 1.0f, pan);
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
		return pool.playSample(samplePos, samplelen, amplitude, env, pitch, pan);
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
	
	void ensureSamplerReady() {
	    if (pool != null) pool.setBuffer(playBuffer);
	    else pool = new PASamplerInstrumentPool(playBuffer, sampleRate, 1, samplerMaxVoices, audioOut, defaultEnv);
	}
	
	void ensureGranularReady() {
		if (gParamsGesture == null || gParamsFixed == null) {
			initGranularParams();
		}
	    if (gSynth == null) {
	    	ADSRParams granEnv = new ADSRParams(1.0f, 0.005f, 0.02f, 0.875f, 0.025f);
	    	gSynth = buildGranSynth(audioOut, granEnv, gMaxVoices);
	    }
	    if (granSignal == null) {
	        granSignal = (audioSignal != null) ? audioSignal : playBuffer.getChannel(0);
	    }
	    if (gDir == null) {
	    	gDir = new PAGranularInstrumentDirector(gSynth);
	    }
	}
	
	void updateAudioChain(float[] sig) {
	    // 0) Decide target length (make this a single source of truth)
	    int targetSize = mapper.getSize();          // or mapSize, but pick one canonical TODO
	    if (targetSize <= 0) return;
	    // 1) Ensure playBuffer matches target
	    if (playBuffer.getBufferSize() != targetSize) {
	        playBuffer.setBufferSize(targetSize);
	    }
	    // 2) Copy sig into a temp array of exactly targetSize (pad/truncate deterministically)
	    float[] tmp = new float[targetSize];
	    if (sig != null) {
	        System.arraycopy(sig, 0, tmp, 0, Math.min(sig.length, targetSize));
	    }
	    // 3) Store into playBuffer
	    playBuffer.setChannel(0, tmp);
	    // 4) Snapshot arrays used elsewhere
	    audioSignal = tmp;                 // already correct size
	    granSignal = audioSignal;          // alias intentionally (or copy if you want independent)
	    audioLength = targetSize;
	    // 5) Propagate into synths (examples — adjust to your actual API)
	    pool.setBuffer(playBuffer);
	    // granularDirector doesn't track an audio buffer with a field
	}
	
	public void playGranularGesture(float buf[], GestureSchedule sched, GestureGranularParams params) {
		int[] startIndices = mapper.lookupSignalPosArray(sched.points, totalShift, mapper.getSize());
		// println("---> startIndices[0] = "+ startIndices[0] +" for "+ sched.points.get(0).x, sched.points.get(0).y, totalShift, mapper.getSize());
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
		if (usePitchedGrains) {
			float[] pitch = generateJitterPitch(sched.size(), 0.25f);
            GestureEventParams eventParams = GestureEventParams.builder(sched.size())
                .startIndices(startIndices)
                .pan(panPerGrain)
                .pitchRatio(pitch)
                .build();
			gDir.playGestureNow(buf, sched, params, eventParams);
			println("-- pitch jitter -- "+ pitch[0]);
			return;
		}
		gDir.playGestureNow(buf, sched, params, startIndices, panPerGrain);
	}

	public ADSRParams calculateEnvelope(float gainDb, int totalSamples, float sampleRate) {
	    return calculateEnvelope(gainDb, totalSamples * 1000f / sampleRate);
	}

	public ADSRParams calculateEnvelope(float gainDb, float totalMs) {
	    float attackMS = Math.min(50, totalMs * 0.1f);
	    float releaseMS = Math.min(200, totalMs * 0.3f);
	    float envGain = AudioUtility.dbToLinear(gainDb);
	    envGain = 1.0f;
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
	        currentPoint = new PVector(clipToWidth(x), clipToHeight(y));
	        allPoints.add(currentPoint);
	        allTimes.add(millis() - startTime);
	    }
	}
		
	public int clipToWidth(int i) {
		return min(max(0, i), width - 1);
	}
	public int clipToHeight(int i) {
		return min(max(0, i), height - 1);
	}
	
	public PVector jitterCoord(int x, int y, int deviationPx) {
	    double variance = deviationPx * deviationPx;
	    int jx = (int)Math.round(PixelAudio.gauss(0, variance));
	    int jy = (int)Math.round(PixelAudio.gauss(0, variance));
	    int nx = clipToWidth(x + jx);
	    int ny = clipToHeight(y + jy);
	    return new PVector(nx, ny);
	}
	
	
	float[] generateJitterPitch(int length, float deviationPitch) {
		float[] pitch = new float[length];
		double variance = deviationPitch * deviationPitch;
		for (int i = 0; i < pitch.length; i++) {
			pitch[i] = (float) PixelAudio.gauss(1, variance);
		}
		return pitch;
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
	    AudioBrushLite b = new AudioBrushLite(curveMaker, cfg, BrushOutput.SAMPLER, HopMode.GESTURE);
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
	
	public void setBrushEpsilon(AudioBrushLite b, float e) {
		PACurveMaker cm = b.curve();
		BrushConfig cfg = b.cfg();
		cfg.rdpEpsilon = e;
		cm.setEpsilon(e);
		cm.calculateDerivedPoints();
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
			sched = b.curve.getReducedSchedule(b.cfg.rdpEpsilon);
		}
		case CURVE_POINTS -> {
			sched = b.curve.getCurveSchedule(b.cfg.rdpEpsilon, curveSteps, isAnimating);
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
		startTime = millis() + 5;
		// we store the point and the current time + time offset, where timesMs[0] == 0
		for (PVector loc : sched.points) {
			int x = Math.round(loc.x);
			int y = Math.round(loc.y);
			int t = startTime + Math.round(sched.timesMs[i++]);
			int d = 200;
			this.samplerTimeLocs.add(new TimedLocation(x, y, t, d));
		}
		Collections.sort(samplerTimeLocs);
	}

	public synchronized void runSamplerBrushEvents() {
	    if (samplerTimeLocs == null || samplerTimeLocs.isEmpty()) return;
	    int currentTime = millis();
	    samplerTimeLocs.forEach(tl -> {
	        if (tl.eventTime() < currentTime) {
	            int sampleX = PixelAudio.constrain(Math.round(tl.getX()), 0, width - 1);
	            int sampleY = PixelAudio.constrain(Math.round(tl.getY()), 0, height - 1);
	            float panning = map(sampleX, 0, width, -0.8f, 0.8f);
	            int pos = getSamplePos(sampleX, sampleY);
                playSample(pos, calcSampleLen(), synthGain, panning);
        		pointTimeLocs.add(new TimedLocation(sampleX, sampleY, tl.getDurationMs() + millis()));
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
	    boolean isGesture = (b.hopMode() == HopMode.GESTURE);
		GestureGranularParams gParams = isGesture ? gParamsGesture : gParamsFixed;
		GestureSchedule sched = getScheduleForBrush(b); 
	    playGranularGesture(buf, sched, gParams);
		storeGranularCurveTL(sched, millis() + 10, isGesture);
	}	

	public synchronized void storeGranularCurveTL(GestureSchedule sched, int startTime, boolean isGesture) {
		if (this.grainTimeLocs == null) grainTimeLocs = new ArrayList<>();
		int i = 0;
		int hopMs = (int) Math.round(AudioUtility.samplesToMillis(hopSamples, sampleRate));
		int durMsFixed = (int) Math.round(AudioUtility.samplesToMillis(granSamples, sampleRate)); // or hopMs if you prefer
		// we store the point and the current time + time offset, where timesMs[0] == 0
		for (PVector loc : sched.points) {
			int x = Math.round(loc.x);
			int y = Math.round(loc.y);
			int t = (isGesture) ? startTime + Math.round(sched.timesMs[i++]) : startTime + i++ * hopMs;
			int d = (isGesture) ? 200 : durMsFixed;
			this.grainTimeLocs.add(new TimedLocation(x, y, t, d));
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


