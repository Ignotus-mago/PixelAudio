/**
 * 
 */

package net.paulhertz.pixelaudio.example;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;


import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.event.KeyEvent;

// video export
import com.hamoid.VideoExport;

// Minim audio library
import ddf.minim.AudioOutput;
import ddf.minim.Minim;
import ddf.minim.MultiChannelBuffer;
import ddf.minim.AudioListener;

//GUI library for Processing
import g4p_controls.*;

// our red leaf lettuce of a library, so crisp and delicious
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;
import net.paulhertz.pixelaudio.curves.*;
import net.paulhertz.pixelaudio.example.TutorialOne_03_Drawing.BrushOutput;
import net.paulhertz.pixelaudio.schedule.*;
import net.paulhertz.pixelaudio.granular.*;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig.HopMode;
import net.paulhertz.pixelaudio.sampler.*;


/* 
 * TODO
 */

/**
 * 
 * <DIV>
 * <h2>QUICK START</h2>
 * <ol>
 * <li>Launch the sketch. A display window and a palette of Graphical User Interface (GUI) controls appears.
 * The display window has an audio file preloaded. The grayscale values in the image are transcoded audio
 * samples. An overlaid rainbow spectrum traces the Signal Path, the mapping of the audio signal to the image
 * pixels created by the PixelMapGen <code>multigen</code> and managed by the PixelAudioMapper <code>mapper</code>.
 * The Signal Path starts in the upper left corner and ends in the lower right corner.</li> 
 * 
 * <li>Drawing is already turned on, so go ahead and drag the mouse to draw a line. As in TutorialOne_03_Drawing, 
 * a brushstroke appears when you release the mouse. TutorialOne_03_Drawing gave you limited control over
 * the attributes of the brushstroke and its associated audio parameters. In GesturePlayground, you can
 * control nearly all the available parameters with the control palette.</li> 
 * 
 * <li>At the top of the control palette, you'll find Path Source radio buttons and sliders for setting 
 * the geometry of the brush curve. When the curve is set to Reduced Points or Curve Points, the epsilon 
 * slider will allow you to visualize changes in the curve. For the curve points representation of the 
 * curve, theCurve Points slider will add or subtract points.</li>
 * 
 * <li>The control palette displays knobs for the type of audio synthesis instrument you have selected.
 * Press the 't' key to change the instrument. The control palette will reflect the changes. The 
 * control palette provides three play modes: one for editing granular synthesis parameters, another
 * for the sampler synthesizer, and a "play only" mode where you can play both instruments but
 * don't have editing enabled.</li>
 * 
 * <li>The controls for the Sampler are fairly simple. You can change the number of points in the curve
 * with the geometry controls. You can also change the duration of the gesture and the number of 
 * points in it with the Resample and Duration sliders. Finally, there's a Sampler Envelope menu
 * that will change the ADSR envelope of each sampler event point.</li> 
 * 
 * <li>The Granular Synth has all the controls of the Sampler synth except for the envelopes, plus 
 * many controls for granular synthesis:</li>
 *   <ol>
 *   <li>The Hop Mode radio buttons determine if the duration of the granular event is determined 
 *   by the gesture timing data in the brushstroke's PACurveMaker instance, or by the Grain
 *   Length and Hop Length sliders.</li> 
 *   <li>Burst Count sets the number of linear grains at each event point. Its effect is to expand
 *   the sound of the grain.</li> 
 *   <li>Grain Length and Hop Length sliders control the spacing of the grains. Hop Length is only 
 *   used for Fixed Hop Mode. Grain and Hop durations are in milliseconds.</li> 
 *   <li>The Warp radio buttons and slider control non-linear timing changes to the gesture.</li> 
 *   </ol>
 * <li>There are many key commands too, including command to load new audio files. Two commands are 
 * particularly useful with granular synthesis: the 'q' command key will calculate the optimal
 * number of grains in a gesture (usually in GESTURE Path Mode) and update the control palette. 
 * This can provide smooth granular synthesis even as it preserves the timing characteristic 
 * of the gesture. The 'c' command will print configuration data to the console.</li>
 * 
 * </ol>
 * <p>
 * <b>GesturePlayground</b> uses a GUI to provide a tour of the usage and properties of the <code>AudioBrush</code> 
 * subclasses <code>GranularBrush</code> and <code>SamplerBrush</code> and the <code>GestureSchedule</code> class.
 * An AudioBrush combines a <code>PACurveMaker</code> and a <code>GestureGranularConfig.Builder</code>. PACurveMaker
 * models <i>gestures</i>, one of the core concepts of PixelAudio. In its simplest encoded form, the <code>PAGesture</code> interface, 
 * a gesture consists of an array of points and an array of times. The times array and the points array must be the same size, because
 * the times array records the times when something as-yet-unspecified will happen at the corresponding point in the 
 * points array. In my demos for PixelAudio, what happens at a point is typically an audio event and an animation event. 
 * The sound happens at the point because points in PixelAudio map onto locations in the sound buffer. Mapping of bitmap locations 
 * onto audio buffer indices is another core concept of PixelAudio. Gestures over the 2D space of an image become 
 * paths through audio buffers. The audio buffer is traversed either by a granular synthesis engine or by a sampling synthesizer.
 * For the granular synth, a gesture corresponds to a non-linear traversal of an audio buffer, potentially as a continuous sequence 
 * of overlapping grains with a single envelope. The sampling synthesizer treats each point as a discrete event with its own 
 * envelope. Depending on how gestures and schedules are structured, the two synthesizers can sound very similar, but there 
 * are possibilities in each that the other cannot realize. As you might expect, GranularBrush implements granular synth
 * events and SamplerBrush implements sampler synth events. Bother rely on PACUrveMaker which, in addition to capturing
 * the raw gesture of drawing a line, provides methods to reduce points / times and create Bezier paths. PACurveMaker 
 * data can also be modified by changing duration, interpolating samples, or non-linear time warping. <code>PAGestureParametric</code>
 * provides a basic structure for implementing these modifications. GesturePlayground uses <code>GestureScheduleBuilder</code> to
 * interpolate and warp time and point lists, with basically the same math. 
 * </p>
 * <p>The parameters for gesture modeling, granular and sampling synthesis, time and sample interpolation, and audio events are
 * modeled in the GUI, which uses <code>GestureGranularConfig.Builder gConfig</code> to track its current state. A GestureGranularConfig 
 * instance is associated with each AudioBrush. When you click on an AudioBrush and activate it, its configuration data is 
 * loaded to the GUI and you can edit it. It will be saved to the brush when you select another brush or change the edit mode. When 
 * a brush is activated with a click, the schedule is built from its PACurveMaker and estureGranularConfig.Builder 
 * instance variables:
 *     <pre>GestureSchedule schedule = scheduleBuilder.build(gb.curve(), cfg.build(), audioOut.sampleRate());</pre>
 * </p>
 * 
 * <p>Part of the calling chain for a GranularBrush:<br>
 * <code>mouseClicked()</code> calls <code>scheduleGranularBrushClick(gb, x, y);</code>.<br>
 * In <code>scheduleGranularBrushClick(...)</code> we use <code>gb.snapshot()</code> to build a <code>GestureSchedule</code> Our schedule is loaded outside<br> 
 * a loop by calling <code>audioSched.schedulePoint(startSample, new GranularBrushHappening(x0, y0, gb, schedule, snap))</code>. <br>
 * We use a loop to schedule animation, dot appearing at the scheduled event points at the right event times. <br>
 * Each <code>GranularBrushHappening</code> in audioSched will be handled in the <code>AudioListener.processAudioBlock(...)</code> method: <br>
 * <code>else (h instanceof GranularBrushHappening gbh) {playGranularBrush(gbh.brush, gbh.schedule, gbh.snap, granularMapping);</code> </p> 
 * 
 * <p>Part of the calling chain for a SamplerBrush:<br>
 * <code>mouseClicked()</code> calls <code>scheduleSamplerBrushClick(sb, x, y)</code>.<br>
 * In <code>scheduleSamplerBrushClick()</code> we use <code>sb.snapshot()</code> to build a <code>GestureSchedule</code> and loop through the schedule,<br> 
 * calling <code>audioSched.schedulePoint(startSample + dt, new SamplerPointHappening(x, y, samplePos, len, pan));</code>. <br>
 * The <code>SamplerPointHappening</code> in audioSched will be handled in the <code>AudioListener.processAudioBlock(...)</code> method: <br>
 * <code>else if (h instanceof SamplerPointHappening sph) { playSample(sph.samplePos, sph.len, synthGain, samplerEnv, sph.pan); }</code> </p> 
 * 
 * <p>
 * 
 * </p>
 * 
 * </DIV>
 * 
 * 
 */
public class GesturePlayground extends PApplet {

	/* ------------------------------------------------------------------ */
	/*                       PIXELAUDIO VARIABLES                         */
	/* ------------------------------------------------------------------ */
	
	PixelAudio pixelaudio;     // our shiny new library
	MultiGen multigen;         // a PixelMapGen that links together multiple PixelMapGens
	int genWidth = 512;        // width of multigen PixelMapGens
	int genHeight = 512;       // height of  multigen PixelMapGens
	PixelAudioMapper mapper;   // object for reading, writing, and transcoding audio and image data
	int mapSize;               // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
	// baseImage is a reference image that generally should not be changed except when you load a new file
	PImage baseImage;          // unchanging source image
	// mapImage can change, and often does so with reference to the stable baseImage, for example when animating
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

	String daPath = "/Users/paulhz/Code/Workspace/PixelAudio/examples/examples_data/";   // system-specific path to example files data
	String daFilename = "audioBlend.wav";

	/* ------------------------------------------------------------------ */
	/*                          AUDIO VARIABLES                           */
	/* ------------------------------------------------------------------ */
	
	/** Minim audio library */
	Minim minim;					// library that handles audio 
	AudioOutput audioOut;			// line out to sound hardware
	boolean isBufferStale = false;	// flags that audioBuffer needs to be reset
	float sampleRate = 44100;       // target audio engine rate used to configure audioOut
	float fileSampleRate;           // sample rate of most recently opened file (before resampling)
	float bufferSampleRate;         // sample rate of playBuffer, usually == audioOut.sampleRate()
	float[] audioSignal;			// the audio signal as an array of floats
	MultiChannelBuffer playBuffer;	// a buffer for playing the audio signal
	int samplePos;                  // an index into the audio signal, selected by a mouse click on the display image
	int audioLength;				// length of the audioSignal, same as the number of pixels in the display image

	// ADSR and its parameters
	ADSRParams samplerEnv;			// good old attack, decay, sustain, release for Sampler instrument
	ADSRParams granularEnv;         // envelope for a granular-style series of samples
	float maxAmplitude = 0.75f;     // 0..1
	float attackTime = 0.4f;        // seconds
	float decayTime = 0.0f;         // seconds, no decay
	float sustainLevel = 0.75f;     // 0..1, same as maxAmplitude
	float releaseTime = 0.5f;       // seconds, same as attack
	float pitchScaling = 1.0f;      // factor for changing pitch
	float defaultPitchScaling = 1.0f;
	float lowPitchScaling = 0.5f;
	float highPitchScaling = 2.0f;
		
	boolean envIsGranular = false;  // if true, sets all envelopes to the same size, with 50% overlap when playing
	int grainDuration = 120;        // granular envelope duration in milliseconds
	
	// ====== Sampler Synth ====== //

	// SampleInstrument setup
	int noteDuration = 1000;        // average sample synth note duration, milliseconds
	int samplelen;                  // calculated sample synth note length, samples
	float samplerGain = 0.75f;       // gain setting for audio playback instrument, decimal value 0..1
	float samplerPointGain = 0.75f;   // gain for Sampler instrument point events
	float outputGain = -6.0f;       // gain setting for audio output, decibels
	boolean isMuted = false;
	PASamplerInstrumentPool pool;   // an allocation pool of PASamplerInstruments
	int sMaxVoices = 64;            // number of voices to allocate to pool or synth

    // ====== Granular Synth ====== //

	public float[] granSignal;
	public PAGranularInstrument gSynth;
	public int curveSteps = 16;
	public int granLength = 4096;
	public int granHop = 1024;
	public int gMaxVoices = 256;
	public ADSRParams granEnvelope = new ADSRParams(1.0f, 0.02f, 0.06f, 0.9f, 0.10f);
	
	String currentGranStatus = "";
	
	// TODO -- ADD FOR REFACTOR  
    public PAGranularInstrumentDirector gDir;   // director of granular events
    public float granularGain = 1.0f;           // gain for a granular gesture event
    public float granularPointGain = 0.9f;      // gain for a granular point event ("granular burst")
    // parameters for granular synthesis
    boolean useShortGrain = true;               // default to short grains, if true
    int longSample = 4096;                      // number of samples for a moderately long grain
    int shortSample = 512;                      // number of samples for a relatively short grain
    int granSamples = useShortGrain ? shortSample : longSample;    // number of samples in a grain window
    int hopSamples = granSamples/4;             // number of samples between each grain in a granular burst
    GestureGranularParams gParamsFixed;         // granular parameters when hopMode == HopMode.FIXED, for granular point event
    boolean useLongBursts = false;              // controls the number of burst grain in a point event or gesture event
    int maxBurstGrains = 4;
    int burstGrains = 1;
    boolean usePitchedGrains = false;

	
	/* ------------------------------------------------------------------ */
	/*                   ANIMATION AND VIDEO VARIABLES                    */
	/* ------------------------------------------------------------------ */
	
    // This example concentrates on audio synthesis, we don't do pixel-shift 
    // animation, but the hooks for handling it are all here
    
    int shift = 1024;                    // number of pixels to shift the animation
    int totalShift = 0;                  // cumulative shift
    boolean isAnimating = false;         // do we run animation or not?
    boolean oldIsAnimating;              // keep track of animation state when opening a file
    boolean isTrackMouse = false;        // if true, drag the mouse to change shift value
    // animation variables
    int animSteps = 720;                 // how many steps in an animation loop
    boolean isRecordingVideo = false;    // are we recording? (only if we are animating)
    int videoFrameRate = 24;             // fps, frames per second
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
	 * 
	 */
	
	// curve drawing and interaction
	public PACurveMaker curveMaker;                     // class for tracking and storing drawing data
	public boolean isDrawMode = false;                  // is drawing on or not?
	public float epsilon = 4.0f;						// controls how much reduction is applied to points
	
    public PVector currentPoint = new PVector(-1, -1);  // point to add to brushstroke points
    public ArrayList<PVector> allPoints;                // in-progress brushstroke points
    public ArrayList<Integer> allTimes;                 // in-progress brushstroke times
    public int startTime;                               // in-progress brushstroke start time
	
	public int dragColor = color(233, 199, 89, 128);	// color for initial drawing 
	public float dragWeight = 8.0f;						// weight (brush diameter) of initial line drawing
	
	public int polySteps = 5;							// number of steps in polygon representation of a Bezier curve	
	
	// ====== Visual styling for brushstrokes ======
	
	// the next series of colors seems to be all we need
	int readyBrushColor1 = color(34, 55, 89, 178);		// color for a brushstroke when available to be clicked
	int hoverBrushColor1 = color(55, 89, 144, 178);     // color for brushstroke under the cursor
	int selectedBrushColor1 = color(199, 123, 55, 233);	// color for the selected (clicked or currently active for editing) brushstroke
	int readyBrushColor2 = color(55, 34, 89, 178);		// color for a brushstroke when available to be clicked
	int hoverBrushColor2 = color(89, 55, 144, 178);     // color for brushstroke under the cursor
	int selectedBrushColor2 = color(123, 199, 55, 233);	// color for the selected (clicked or currently active for editing) brushstroke

	int dimmedBrushColor = color(55, 55, 55, 178);      // color for a brush that cannot be selected
	
	int circleColor = color(233, 178, 144);             // interior circles in brushstrokes
	int dimCircleColor = color(178, 168, 136);
	int lineColor = color(233, 220, 178);               // interior lines in brushStrokes
	int dimLineColor = color(178, 168, 136);
    int animatedCircleColor = color(233, 220, 199);     // color for animated circles when playing audio   

	boolean isIgnoreOutsideBounds = true;               // when drawing, clip or ignore points outside display bounds
	
	
	/* ------------------------------------------------------------------ */
	/*                        INTERACTION VARIABLES                       */
	/* ------------------------------------------------------------------ */
	
	/**
	 * Simple container for a brush hit-test result.
	 * Holds the brush that was hit and its index in the corresponding list.
	 */
	static class BrushHit {
	    final AudioBrush brush;
	    final int index;

	    BrushHit(AudioBrush brush, int index) {
	        this.brush = brush;
	        this.index = index;
	    }
	}
	
	/** AudioBrush wraps a PACurveMaker (gesture) and a GestureGranularConfig.Builder (granular synthesis parameters) */
	private AudioBrush hoverBrush;         // the brush the mouse is over, if there is one
	private int hoverIndex;                // index of hoverBrush in a brush list
	private AudioBrush activeBrush;        // brush slotted for selection/highlight/editing in GUI

	// AudioBrushes for use with PAGranularInstrument
	ArrayList<GranularBrush> granularBrushes;
	GranularBrush activeGranularBrush = null;
	int activeGranularIndex = -1; // index of activeStroke in strokes, optional convenience

	// AudioBrushes for use with PASamplerInstrument 
	ArrayList<SamplerBrush> samplerBrushes;      
	SamplerBrush activeSamplerBrush;             // the currently active PACurveMaker, collecting points as user drags the mouse
	int activeSamplerIndex = 0;                  // index of current brush in brushShapesList, useful for UDP/OSC messages, replaced
	
    // TimedLocation events
	ArrayList<TimedLocation> pointTimeLocs;      // a list of timed events for mouse clicks
    ArrayList<TimedLocation> samplerTimeLocs;    // a list of timed events for Sampler brushes
	ArrayList<TimedLocation> grainTimeLocs;      // a list of timed events for Granular brushes
	
	boolean pointEventUseSampler = true;
	
	/* ------------------------------------------------------------------ */
	/*                  GRAPHIC USER INTERFACE VARIABLES                  */
	/* ------------------------------------------------------------------ */
		
	// configuration for the GUI to control, also part of the audio synthesis chain
	GestureGranularConfig.Builder gConfig = new GestureGranularConfig.Builder();
	// for reset to default configuration, optionally set to non-default values
	final GestureGranularConfig.Builder defaultGranConfig = new GestureGranularConfig.Builder().gainDb(0.0f); 
	final GestureGranularConfig.Builder defaultSampConfig  = new GestureGranularConfig.Builder().gainDb(-6.0f);
	
	boolean guiSyncing = false;   // prevents event feedback loops
	int baselineCount = 0;
	int baselineDurationMs = 0;
	GestureScheduleBuilder scheduleBuilder;
	GestureGranularRenderer.DefaultMapping granularMapping;

	enum Mode { DRAW_EDIT_SAMPLER, DRAW_EDIT_GRANULAR, PLAY_ONLY }
	Mode mode = Mode.DRAW_EDIT_GRANULAR;

	static final int CURVE_STEPS_HARD_MAX = 128;   // Curve steps GUI slider max
	static final int CURVE_STEPS_SAFE_MAX = 32;    // Curve steps preferred max threshold
	static final int CURVE_STEPS_FLOOR = 4;        // don’t go too low

	boolean doPlayOnDraw = false;                  // play audio when a curve is drawn
	
	/* ------------------------------------------------------------------ */
	/*                    TIME/LOCATION/ACTION VARIABLES                  */
	/* ------------------------------------------------------------------ */

	/**
	 * Barebones interface for "something that happens at a certain point": 
	 * in AudioScheduler the time-when-something-happens gets connected to
	 * the-room-where-it-happens and the entire cast of Hamilton steps in, if you let them. 
	 */
	interface Happening { int x(); int y(); }
		
	AudioScheduler<Happening> audioSched = new AudioScheduler<>();	

	// Tracks current absolute sample position (block start) on the audio thread
	private final AtomicLong audioBlockStartSample = new AtomicLong(0);

	// Optional: if you want to schedule “now”, this is a safe estimate of the next block start.
	private final AtomicLong audioNextBlockStartSample = new AtomicLong(0);
		
	// grain density mod
	float hopScale = 1.0f;
	// optimal number of grains for specified time
	int optGrainCount = 2;

	int eventStep = 90;                                 // milliseconds between events
	
	/* ------------------------------------------------------------------ */
	/*                         DEBUGGING AND FEEDBACK                     */
	/* ------------------------------------------------------------------ */
	
	boolean isVerbose = true;
	boolean isDebugging = false;

	
	//------------- APPLICATION CODE -------------//
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(new String[] { GesturePlayground.class.getName() });
	}
	
	public void settings() {
		size(3 * genWidth, 2 * genHeight);
	}
	
	public void setup() {
		// set a standard animation framerate
		frameRate(24);
		surface.setTitle("Granular Playground");
		surface.setLocation(60, 20);
		// 1) initialize our library
		pixelaudio = new PixelAudio(this);
		// 2) create a PixelMapGen instance with dimensions equal to the display window.
		multigen = HilbertGen.hilbertRowOrtho(6, 4, width/6, height/4);
		// 3) Create a PixelAudioMapper to handle the mapping of pixel colors to audio samples.
		mapper = new PixelAudioMapper(multigen);
		mapSize = mapper.getSize();
		scheduleBuilder = new GestureScheduleBuilder();
		colors = getColors(mapSize);    // create an array of rainbow colors with mapSize elements
		initImages();                   // load baseImage and mapImage
		initAudio();                    // set up Minima and our granular and sampling synths
		// initListener();              // PLACEHOLDER: sample-accurate audio timer -- TODO future implementation
		initConfig();                   // set up configuration for granular and sampling instruments
		initDrawing();                  // set up drawing variables
		initGUI();                      // set up the G4P control window and widgets
		resetConfigForMode();           // determine which GestureGranularConfig to use first and load it
		preloadFiles(daPath, daFilename);    // load files - BEWARE system dependent file references!
		applyColorMap();                // apply spectrum to mapImage and baseImage
		showHelp();                     // print key commands to console
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
			colorWheel[i] = color(i, 30, 50); // fill our array with colors, gradually changing hue
		}
		popStyle(); // restore styles, including the default RGB color space
		return colorWheel;
	}

	/**
	 * Initializes mapImage with the colors array. 
	 * mapImage handles the color data for mapper and also serves as our display image.
	 */
	public void initImages() {
		mapImage = createImage(width, height, ARGB);
		mapImage.loadPixels();
		mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
		mapImage.updatePixels();
		baseImage = mapImage.copy();
	}
	
	/**
	 * Initializes drawing and drawing interaction variables. 
	 */
	public void initDrawing() {
	    currentPoint = new PVector(-1, -1);    // used for drawing to the display
	    granularBrushes = new ArrayList<>();
	    samplerBrushes = new ArrayList<>();
	    hoverBrush = null;                     
	    hoverIndex = -1;
	    activeBrush = null;
	}
	
	public void initConfig() {
		defaultGranConfig.grainLengthSamples = granLength;
		defaultGranConfig.hopLengthSamples = granHop;
		defaultGranConfig.curveSteps = curveSteps;
		defaultGranConfig.env = envPreset("Triangle");
		defaultSampConfig.env = envPreset("Pluck");
	}
	
	public void initGUI() {
		createGUI();
		controlWindow.loop();
	}
	
	// Processing provides access to local data folder, in Eclipse we use full paths
	public void preloadFiles(String path, String fileName) {
		// the audio file we want to open on startup
		File audioSource = new File(path + fileName);
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
		image(mapImage, 0, 0);
		handleDrawing();          // handle interactive drawing and audio events created by drawing
	}
	
	// OMITTING ANIMATION METHODS FOR THE MOMENT
	
	/**
	 * Handles user's drawing actions, draws previously recorded brushstrokes, 
	 * tracks and generates animation and audio events. 
	 */
	public void handleDrawing() {	
	    // 1) draw existing brushes
		drawBrushShapes();
	    // 2) update hover state (pure state update, no action)
		updateHover();
	    // 3) if in the process of drawing, accumulate points while mouse is held down
		if (isEditable()) {
			if (mousePressed) {
	            addPoint(clipToWidth(mouseX), clipToHeight(mouseY));
			}
			if (allPoints != null && allPoints.size() > 2) {
				PACurveUtility.lineDraw(this, allPoints, dragColor, dragWeight);
			}
		}
		// 4) depending on your event dispatching model, run scheduled events
	    runSamplerBrushEvents();
	    runPointEvents();
	    runGrainEvents();
	}

	BrushHit findHoverHit() {
		// Decide z-order: check the list you draw last *first*,
		// so topmost brushes win.
		// granular on top here, descending for loop means most recent brushes are on top
		if (mode == Mode.DRAW_EDIT_GRANULAR || mode == Mode.PLAY_ONLY) {
			for (int i = granularBrushes.size() - 1; i >= 0; i--) {
				GranularBrush b = granularBrushes.get(i);
				if (mouseInPoly(b.curve().getBrushPoly())) {
					return new BrushHit(b, i);
				}
			}
		}
		if (mode == Mode.DRAW_EDIT_SAMPLER || mode == Mode.PLAY_ONLY) {
			for (int i = samplerBrushes.size() - 1; i >= 0; i--) {
				SamplerBrush b = samplerBrushes.get(i);
				if (mouseInPoly(b.curve().getBrushPoly())) {
					return new BrushHit(b, i);
				}
			}
		}
		return null;
	}

	void updateHover() {
	    BrushHit hit = findHoverHit();
	    if (hit != null) {
	        hoverBrush = hit.brush;
	        hoverIndex = hit.index;
	    } 
	    else {
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
	 * The built-in mousePressed handler for Processing, but note that it forwards 
	 * mouse coords to handleMousePressed(). If isDrawMode is true, we start accumulating
	 * points to allPoints: initAllPoints() adds the current mouseX and mouseY. After that, 
	 * the draw loop calls handleDrawing() to add points. Drawing ends on mouseReleased(). 
	 */
	public void mousePressed() {
		if (isEditable()) {
			initAllPoints();
		} 
		// expand here to call a handler for mousePressed when editing is not on (mode == PLAY_ONLY)
	}
	
	public void mouseDragged() {
		// we don't need to handle dragging -- the draw() loop takes care of drawing to the display
	}
	
	public void mouseReleased() {
		// if (!(isEditable() && allPoints != null)) return; // EDIT to go ahead in all modes
		if (allPoints.size() > 2) {	
			initCurveMakerAndAddBrush();    // create a new brush
			// possible preview action, play on draw
		}
		else {	// handle the event as a click in mouseClicked()
			// nothing
		}
		allPoints.clear();
	}
	
	public void mouseClicked() {
		int x = this.clipToWidth(mouseX);
		int y = this.clipToHeight(mouseY);
		BrushHit hit = findHoverHit();
		if (hit != null) {
			setActiveBrush(hit.brush);      // flag the brush as the activeBrush
			if (hit.brush instanceof SamplerBrush sb) {
				scheduleSamplerBrushClick(sb, x, y);
			}
			else if (hit.brush instanceof GranularBrush gb) {
				scheduleGranularBrushClick(gb, x, y);
			}
			return;
		}
		// where formerly we called audioMousePressed(mouseX, mouseY);
		if (isVerbose) println(" in mouseClicked, calling handleClickOutsideBrush, mode is "+ mode.toString());
		handleClickOutsideBrush(x, y);
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

	public void parseKey(char key, int keyCode) {
		switch(key) {
		case ' ': // spacebar triggers a brush if we're hovering over a brush, otherwise it triggers a point event
			if (hoverBrush != null) {
				if (hoverBrush instanceof SamplerBrush sb) {
					scheduleSamplerBrushClick(sb, clipToWidth(mouseX), clipToHeight(mouseY));
				}
				else if (hoverBrush instanceof GranularBrush gb) {
					scheduleGranularBrushClick(gb, clipToWidth(mouseX), clipToHeight(mouseY));
				}
			}
			else {
				handleClickOutsideBrush(clipToWidth(mouseX), clipToHeight(mouseY));
			}
			break;
		case 'c': case 'C': // print the current configuration status to the console
			printGConfigStatus();
			break;
		case 't': // switch between Granular and Sampler editing and playing
			if (mode == Mode.DRAW_EDIT_GRANULAR) {
				setMode(Mode.DRAW_EDIT_SAMPLER);
				controlWindow.setTitle("Sampler Synth");				
			}
			else if (mode == Mode.DRAW_EDIT_SAMPLER) {
				setMode(Mode.PLAY_ONLY);
				controlWindow.setTitle("Play Only: Both Synths");
			}
			else if (mode == Mode.PLAY_ONLY) {
				setMode(Mode.DRAW_EDIT_GRANULAR);
				controlWindow.setTitle("Granular Synth");
			}
			println("---> mode is "+ mode.toString());			
			break;
		case 'f': case'F': // toggle verbose output to the console
			isVerbose = !isVerbose;
			println("-- isVerbose == "+ isVerbose);
			break;
		case 'r': case 'R': // reset synths to defaults -- TODO may be dropped
			resetToDefaults(); 
			break;
		case 'q': // automatically set an active GRANULAR brush to have an optimized number of samples
			if (activeBrush instanceof SamplerBrush) {
				println("-- please choose a Granular Brush to adjust resampling and duration values.");
				return;
			}
			printGOptHints(hopScale);
			if (activeBrush != null) {
				activeBrush.cfg().resampleCount = optGrainCount;
				syncGuiFromConfig();
			}
			break;
		case 'x': // delete the current active brush shape or the oldest brush shape
			if (hoverBrush != null) {
				removeActiveBrush();
			}
			else {
				removeOldestBrush();
			}
			break;
		case 'X': // delete the most recent brush shape
			removeNewestBrush();
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
		println(" * Press UP ARROW to increase audio gain by 3 dB.");
		println(" * Press DOWN ARROW to decrease audio gain by 3 dB.");
		println(" * Press 'c' or 'C' to print the current configuration status to the console.");
		println(" * Press 't' to switch between GRANULAR and SAMPLER editing and playing.");
		println(" * Press 'T' to set mode to PLAY ONLY (no editing).");
		println(" * Press 'f' to toggle verbose output to the console.");
		println(" * Press 'r' or 'R' to reset synths to defaults -- TODO may be dropped.");
		println(" * Press 'q' to automatically set an active GRANULAR brush to have an optimized number of samples.");
		println(" * Press 'x' to delete the current active brush shape or the oldest brush shape.");
		println(" * Press 'X' to delete the most recent brush shape.");
	}

	/**
	 * Sets audioOut.gain.
	 * @param g  gain value for audioOut, in decibels
	 */
	public void setAudioGain(float g) {
		audioOut.setGain(g);
		outputGain = audioOut.getGain();
	}

	void setMode(Mode newMode) {
		if (newMode == mode) return;
		mode = newMode;
		// Clear hover because the hover rules changed
		hoverBrush = null;
		hoverIndex = -1;
		// Choose what should be "active" in the new mode
		AudioBrush nextActive = null;
		switch (mode) {
		case DRAW_EDIT_GRANULAR: 
			nextActive = activeGranularBrush;
			gConfig.env = granEnvelope;
			break;
		case DRAW_EDIT_SAMPLER:  
			nextActive = activeSamplerBrush;
			break;
		case PLAY_ONLY:
			nextActive = (activeBrush != null) ? activeBrush
					: (activeGranularBrush != null ? activeGranularBrush : activeSamplerBrush);
			break;
		default: {}
		}
		if (nextActive != null) {
			// Keep indices consistent: use stored index for the corresponding type
			int idx = (nextActive instanceof GranularBrush) ? activeGranularIndex : activeSamplerIndex;
			setActiveBrush(nextActive, idx);     // this also syncs GUI
		} 
		else {
			// No selection for this mode
			activeBrush = null;
			resetConfigForMode();
		}
		setControlsEnabled();
	}

	// Reset tool config to defaults (copy, so default config never mutates)
	void resetConfigForMode() {
		if (mode == Mode.DRAW_EDIT_GRANULAR) {
			gConfig = defaultGranConfig.copy();
		} else if (mode == Mode.DRAW_EDIT_SAMPLER) {
			gConfig = defaultSampConfig.copy();
		} else {
			// PLAY_ONLY: choose what you want the GUI to show
			// Option: keep last gConfig; or keep a neutral preset:
			// gConfig = defaultGranularConfig.copy();
		}
		syncGuiFromConfig();
	}

	
	
	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                FILE I/O METHODS (AUDIO ONLY)                   */
	/*                                                                */
	/*----------------------------------------------------------------*/
	
	// ------------- SIMPLIFIED FILE I/O SECTION FOR GranularPlayground ------------- 
	// To keep things simple and focused on our synthesis instruments
	// we omit image file opening - we just handle audio

	/**
	 * Wrapper method for Processing's selectInput command
	 */
	public void chooseFile() {
		oldIsAnimating = isAnimating;
		isAnimating = false;
		selectInput("Choose an audio file to load to audio buffer and display image: ", "fileSelected");
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
	 * Resamples files that are recorded with a different sample rate than the current audio output.
	 * If you want to load the image file and audio file separately, comment out writeAudioToImage(). 
	 * 
	 * @param audFile    an audio file
	 */
	public void loadAudioFile(File audFile) {
		MultiChannelBuffer buff = new MultiChannelBuffer(1024, 1);
		fileSampleRate =  minim.loadFileIntoBuffer(audFile.getAbsolutePath(), buff);
		float[] resampled;
		if (fileSampleRate > 0) {
			println("---- file sample rate is "+ this.fileSampleRate);
			if (fileSampleRate != audioOut.sampleRate()) {
				resampled = AudioUtility.resampleMonoToOutput(buff.getChannel(0), fileSampleRate, audioOut);
				buff.setBufferSize(resampled.length);
				buff.setChannel(0, resampled);
				bufferSampleRate = sampleRate;
			}
			// save the length of the file, possibly resampled, for future use
			this.audioFileLength = buff.getBufferSize();
		}
		else {
			println("-- Unable to load file. File may be empty, wrong format, or damaged.");
			return;
		}
		// adjust buffer size to mapper.getSize()
		if (buff.getBufferSize() != mapper.getSize()) buff.setBufferSize(mapper.getSize());
		playBuffer = buff;
		// ensureSamplerReady will load playBuffer to the Sampler synth "pool"
		ensureSamplerReady();
		// playBuffer is used directly by PASamplerInstrumentPool and should not change, so we copy its signal data
		// TODO consider if PASamplerInstrumentPool should copy the buffer
		float[] newSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
		audioSignal = newSignal;
		granSignal = newSignal;
		audioLength = audioSignal.length;
		if (isLoadToBoth) {
			writeAudioToImage(audioSignal, mapper, mapImage, chan);
			commitMapImageToBaseImage();
		}
		totalShift = 0;    // reset animation shift when audio is reloaded
	}


	/**
	 * Transcodes audio data in sig[] and writes it to color channel chan of mapImage 
	 * using the lookup tables in mapper to redirect indexing. Calls mapper.mapSigToImg(), 
	 * which will throw an IllegalArgumentException if sig.length != img.pixels.length
	 * or sig.length != mapper.getSize(). 
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
		
	public void commitMapImageToBaseImage() {
		baseImage = mapImage.copy();
		totalShift = 0;
	}
	
	public void commitNewBaseImage(PImage img) {
		baseImage = img.copy();
		mapImage = img.copy();
		totalShift = 0;
	}
	
	
	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                        AUDIO METHODS                           */
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
		println("---- audio out gain is "+ nf(audioOut.getGain(), 0, 2) +", sample rate is "+ sampleRate);
		// create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
		// the buffer will not have any audio data -- you'll need to open a file for that
		this.playBuffer = new MultiChannelBuffer(mapSize, 1);
		audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
		granSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
		this.audioLength = audioSignal.length;
		// ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
		samplerEnv = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
		granularEnv = ADSRUtils.fitEnvelopeToDuration(samplerEnv, grainDuration);
		// initialize event animation tracking arrays
		initTimedEventLists();   
	}

	/**
	 * initialize lists of TimedLocation objects, used for animated response to mouse clicks
	 */
	public void initTimedEventLists() {
		pointTimeLocs = new ArrayList<TimedLocation>();
	    samplerTimeLocs = new ArrayList<TimedLocation>();   
		grainTimeLocs = new ArrayList<TimedLocation>();
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
	  int pos = mapper.lookupSignalPos(x, y);
	  // calculate how much animation has shifted the indices into the buffer
	  totalShift = (totalShift + shift % mapSize + mapSize) % mapSize;
	  return (pos + totalShift) % mapSize;
	}

	/**
	 * @param pos    an index into the audio signal
	 * @return a PVector representing the image pixel mapped to pos
	 */
	public PVector getCoordFromSignalPos(int pos) {
		int[] xy = this.mapper.lookupImageCoordShifted(pos, totalShift);
		return new PVector(xy[0], xy[1]);
	}
	
	
	
	// -------------- METHODS TO PLAY SAMPLING OR GRANULAR SYNTH ------------- //
		
	/**
	 * Plays a sample and animates a point
	 * @param x    x-coordinate of event
	 * @param y    y-coordinate of event
	 */
	void runSamplerPointEvent(int x, int y) {
		ensureSamplerReady();
		int signalPos = mapper.lookupSignalPosShifted(x, y, totalShift);
		float panning = map(x, 0, width, -0.8f, 0.8f);
		int len = calcSampleLen(noteDuration, 1.0f, 0.0625f);
		samplelen = playSample(signalPos, len, samplerPointGain, samplerEnv, panning);
		int durationMS = (int)(samplelen / sampleRate * 1000);
		pointTimeLocs.add(new TimedLocation(x, y, durationMS + millis() + 50));		
		if (isVerbose) println("----- sampler audio event + edit enabled "+ hoverIndex);
	}

	/**
	 * Plays a granular burst and animates a point
	 * @param x    x-coordinate of event
	 * @param y    y-coordinate of event
	 */
	void runGranularPointEvent(int x, int y) {
		ensureGranularReady();
		float hopMsF = Math.round(AudioUtility.samplesToMillis(hopSamples, sampleRate));
		final int grainCount = useLongBursts ?  8 * (int) Math.round(noteDuration/hopMsF) : (int) Math.round(noteDuration/hopMsF);
		if (isVerbose) println("-- granular point burst with grainCount = "+ grainCount);
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
		if (isVerbose) println("----- granular audio event + edit enabled ----- " + hoverIndex);
		return;
	}

	// ----- old code deleted ----- //
	
	public void playGranularGesture(float buf[], GestureSchedule sched, GestureGranularParams params) {
		int[] startIndices = mapper.lookupSignalPosArray(sched.points, totalShift, mapper.getSize());
		// println("---> startIndices[0] = "+ startIndices[0] +" for "+ sched.points.get(0).x, sched.points.get(0).y, totalShift, mapper.getSize());
		float[] panPerGrain = new float[sched.size()];
		for (int i = 0; i < sched.size(); i++) {
		    PVector p = sched.points.get(i);
		    // example: map x to [-0.8, +0.8]
		    panPerGrain[i] = map(p.x, 0, width-1, -0.875f, 0.875f);
		}
		println("\n----->>> playGranularGesture()");
//		debugIndexHeadroom(buf, startIndices, params);
//		debugTimesMs(sched);
//		println("\n");
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
	
	
	// debugging -- TODO drop in release
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

	// debugging -- TODO drop in release
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
	
	
	public ADSRParams calculateEnvelopeDb(float gainDb, int totalSamples, float sampleRate) {
		float linear = AudioUtility.dbToLinear(gainDb);
		return calculateEnvelopeLinear(linear, totalSamples * 1000f / sampleRate);
	}

	public ADSRParams calculateEnvelopeLinear(float linear, float totalMs) {
		float attackMS = Math.min(50, totalMs * 0.1f);
		float releaseMS = Math.min(200, totalMs * 0.3f);
		float envGain = linear;    // or = 1.0f;
		ADSRParams env = new ADSRParams(envGain, attackMS / 1000f, 0.01f, 0.8f, releaseMS / 1000f);
		return env;
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
		return playSample(samplePos, samplelen, amplitude, samplerEnv, 1.0f, pan);
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
	public int calcSampleLen(int dur, float mean, float variance) {
		float vary = 0; 
		// skip the fairly rare negative numbers
		while (vary <= 0) {
			vary = (float) PixelAudio.gauss(mean, variance);
		}
		samplelen = (int)(abs((vary * dur) * sampleRate / 1000.0f));
		// if (isVerbose) println("---- calcSampleLen samplelen = "+ samplelen +" samples at "+ sampleRate +"Hz sample rate");
		return samplelen;
	}
	
	public int calcSampleLen() {
		return calcSampleLen(noteDuration, 1.0f, 0.0625f);
	}

	/**
	 * Prepares Sampler instruments and assets
	 */
	void ensureSamplerReady() {
		if (playBuffer == null) return;
	    if (pool != null) pool.setBuffer(playBuffer);
	    else pool = new PASamplerInstrumentPool(playBuffer, sampleRate, 1, sMaxVoices, audioOut, samplerEnv); 
    	pool.setGain(samplerGain);
	}
	
	/**
	 * Prepares Granular instruments and assets
	 */
	void ensureGranularReady() {
	    if (gSynth == null) {
	    	ADSRParams granEnv = new ADSRParams(1.0f, 0.005f, 0.02f, 0.875f, 0.025f);
	    	gSynth = buildGranSynth(audioOut, granEnv, gMaxVoices);
	    }
	    if (granSignal == null) {
	        granSignal = (audioSignal != null) ? audioSignal : playBuffer.getChannel(0);
	    }
	    if (gParamsFixed == null) {
	    	initGranularParams();
	    }
	    if (gDir == null) {
	    	gDir = new PAGranularInstrumentDirector(gSynth);
	    }
	}
	
	/**
	 * Initializes a PAGranularInstrument.
	 * @param out          AudioOutput for this application
	 * @param env          an ADSRParams envelope
	 * @param numVoices    number of voices for the synth
	 * @return a PAGranularInstrument
	 */
	public PAGranularInstrument buildGranSynth(AudioOutput out, ADSRParams env, int numVoices) {
		PAGranularInstrument inst = new PAGranularInstrument(out, env, numVoices);
		return inst;
	}

	/**
	 * Initializes gParamsFixed, a GestureGranularParams instances used for granular point events.
	 */
	public void initGranularParams() {
		ADSRParams env = this.calculateEnvelopeLinear(granularGain, 1000);
		gParamsFixed = GestureGranularParams.builder()
				.grainLengthSamples(granSamples)
				.hopLengthSamples(hopSamples)
				.gainLinear(granularPointGain)
				.looping(false)
				.env(env)
				.hopMode(GestureGranularParams.HopMode.FIXED)
				.burstGrains(burstGrains)
				.build();
	}

		
	// ---- NEW METHOD FROM TutorialOneDrawing ---- //
	
	/**
	 * Updates the various audio buffers when we load a new signal, typically from a file. 
	 * @param sig    an audio signal
	 */
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
	

	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                        DRAWING METHODS                         */
	/*                                                                */
	/*----------------------------------------------------------------*/
	
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

	/**
	 * @param colorSource    a source array of RGB data from which to obtain hue and saturation values
	 * @param graySource     an target array of RGB data from which to obtain brightness values
	 * @param lut            a lookup table, must be the same size as colorSource and graySource
	 * @param shift          pixel shift from array rotation, windowed buffer, etc.
	 * @return the graySource array of RGB values, with hue and saturation values changed
	 * @throws IllegalArgumentException if array arguments are null or if they are not the same length
	 */
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

	/**
	 * Applies the Hue and Saturation of pixel values in the colors[] array to mapImage and baseImage.
	 * 
	 */
	public void applyColorMap() {
		mapImage.loadPixels();
		applyColorShifted(colors, mapImage.pixels, mapper.getImageToSignalLUT(), totalShift);
		mapImage.updatePixels();
		baseImage.loadPixels();
		applyColor(colors, baseImage.pixels, mapper.getImageToSignalLUT());
		baseImage.updatePixels();
	}
	
	/**
	 * Initializes allPoints and adds the current mouse location to it. 
	 */
	public void initAllPoints() {
		allPoints = new ArrayList<PVector>();
		allTimes = new ArrayList<Integer>();
		startTime = millis();
		addPoint(clipToWidth(mouseX), clipToHeight(mouseY));
	}
	
	/**
	 * Respond to mousePressed events, usually by triggering an event
	 */
	public void handleClickOutsideBrush(int x, int y) {
		if (mode == Mode.DRAW_EDIT_SAMPLER) {
			runSamplerPointEvent(x, y);
			return;
		}
		else if (mode == Mode.DRAW_EDIT_GRANULAR) {
			runGranularPointEvent(x, y);
			return;
		}
		else if (mode == Mode.PLAY_ONLY) {
			// In play-only, play what user clicked/hovered, otherwise fall back to selected
			if (pointEventUseSampler) {
				runSamplerPointEvent(x, y);
				return;
			}
			else {
				runGranularPointEvent(x, y);
				return;
			}
		}		
	}
	
	// NOT USED
	boolean isOverAnyBrush(int x, int y) {
	    // optionally gate by mode; or check both lists always
	    for (int i = granularBrushes.size() - 1; i >= 0; i--) {
	        AudioBrush b = granularBrushes.get(i);
	        if (pointInPoly(b.curve().getBrushPoly(), x, y)) return true;
	    }
	    for (int i = samplerBrushes.size() - 1; i >= 0; i--) {
	        AudioBrush b = samplerBrushes.get(i);
	        if (pointInPoly(b.curve().getBrushPoly(), x, y)) return true;
	    }
	    return false;
	}

	/**
	 * While user is dragging the mouse and mode == Mode.DRAW_EDIT_GRANULAR or DRAW_EDIT_SAMPLER, 
	 * accumulates new points to allPoints and event times to allTimes. Coordinates should be 
	 * constrained to display window bounds. 
	 */
	public void addPoint(int x, int y) {
		// we do some very basic point thinning to eliminate successive duplicate points
		if (x != currentPoint.x || y != currentPoint.y) {
			currentPoint = new PVector(x, y);
			allPoints.add(currentPoint);
			allTimes.add(millis() - startTime);   // store time offset, not absolute time, in allTimes
		}
	}
	
	/**
	 * @param x    a value to constrain to the current window width
	 * @return the constrained value
	 */
	public int clipToWidth(int x) {
		return min(max(0, x), width - 1);
	}
	/**
	 * @param y    a value to constrain to the current window height
	 * @return the constrained value
	 */
	public int clipToHeight(int y) {
		return min(max(0, y), height - 1);
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

	public GestureSchedule loadGestureSchedule(PACurveMaker brush, GestureGranularConfig snap) {
	    if (brush == null || snap == null) return null;
	    GestureSchedule schedule = scheduleBuilder.build(brush, snap, audioOut.sampleRate());
	    if (schedule == null || schedule.isEmpty()) return null;
	    // we could set a global variable, this.currentSchedule
	    return schedule;
	}

	/**
	 * Initializes a PACurveMaker instance with allPoints as an argument to the factory method 
	 * PACurveMaker.buildCurveMaker() and then fills in PACurveMaker instance variables. 
	 */
	public AudioBrush initCurveMakerAndAddBrush() {
		if (gConfig == null) {
		    throw new IllegalStateException("gConfig is null: you probably need to initialize it.");
		}
		curveMaker = PACurveMaker.buildCurveMaker(allPoints, allTimes, startTime);
		curveMaker.setBrushColor(readyBrushColor1);
		curveMaker.setActiveBrushColor(hoverBrushColor1);
		curveMaker.setEpsilon(epsilon);                   // control resolution of reduced points
		curveMaker.setTimeOffset(millis() - startTime);   // time between first point and last point
		curveMaker.calculateDerivedPoints();              // initialize all the useful structures up front
		// reset some fields in gConfig
		gConfig.resampleCount = 0;
		gConfig.targetDurationMs = 0;
		gConfig.pitchSemitones = 0.0f;
		if (mode == Mode.DRAW_EDIT_SAMPLER) {             // TODO implement complete logic for mode, if mode is PLAY we should be here...
			SamplerBrush sb = new SamplerBrush(curveMaker, gConfig.copy());
			this.samplerBrushes.add(sb);                   // add new brush to sampler brush list
			setActiveBrush(sb, samplerBrushes.size() - 1);
			if (doPlayOnDraw) {
				GestureGranularConfig snap = gConfig.build();
				loadGestureSchedule(sb.curve(), snap);
			}
			return sb;
		}
		else {
			gConfig.warpShape = GestureGranularConfig.WarpShape.LINEAR;
			GestureGranularConfig.Builder cfg = gConfig.copy();
			cfg.curveSteps = Math.min(cfg.curveSteps, 32);     // simplest way to set curveSteps
			cfg.env = envPreset("Fade");    // TODO calculate optimal envelope
			GranularBrush gb = new GranularBrush(curveMaker, cfg);
			granularBrushes.add(gb);                           // add new brush to granular brush list
			setActiveBrush(gb, granularBrushes.size() - 1);
			if (doPlayOnDraw) {
				GestureGranularConfig snap = gConfig.build();
				loadGestureSchedule(gb.curve(), snap);
			}
			if (isVerbose) println("----- new granular brush created");
			return gb;
		}
	}
	
	boolean isBrushInteractable(AudioBrush b) {
	    switch (mode) {
	        case DRAW_EDIT_SAMPLER: return b instanceof SamplerBrush;
	        case DRAW_EDIT_GRANULAR: return b instanceof GranularBrush;
	        case PLAY_ONLY: return true; // both playable
	        default: return false;
	    }
	}

	void setActiveBrush(AudioBrush brush) {
		setActiveBrush(brush, hoverIndex);
	}

	void setActiveBrush(AudioBrush brush, int idx) {
		if (brush == null) return;
		if (brush instanceof GranularBrush gb) {
			activeBrush = gb;
			activeGranularBrush = gb;
			activeGranularIndex = idx;
			activeSamplerBrush = null;
			activeSamplerIndex = -1;
		}
		else if (brush instanceof SamplerBrush sb) {
			activeBrush = sb;
			activeSamplerBrush = sb;
			activeSamplerIndex = idx;
			activeGranularBrush = null;
			activeGranularIndex = -1;
		}
		gConfig = brush.cfg();
		recomputeUIBaselinesFromActiveBrush();
		syncGuiFromConfig();
	}
	
	void recomputeUIBaselinesFromActiveBrush() {
		if (activeBrush == null) {
			baselineCount = 0;
			baselineDurationMs = 0;
			return;
		}
		// 1) Count: based on the point set implied by current PathMode
		PACurveMaker curve = activeBrush.curve();
		List<PVector> pts;
		switch (gConfig.pathMode) {
		  case REDUCED_POINTS:
		    pts = curve.getReducedPoints();
		    break;
		  case CURVE_POINTS:
		    pts = curve.getCurvePoints();
		    break;
		  case ALL_POINTS:
		  default:
		    pts = curve.getAllPoints();
		    break;
		}
		baselineCount = (pts == null) ? 0 : pts.size();
		// 2) Duration: depends on HopMode (baseline has resample/duration/warp off)
		if (baselineCount <= 1) {
			baselineDurationMs = 0;
			return;
		}
		if (gConfig.hopMode == GestureGranularConfig.HopMode.FIXED) {
			// Baseline FIXED duration = hop * (n - 1), excludes last grain length by design
			int hop = Math.max(1, gConfig.hopLengthSamples);
			double hopMs = 1000.0 * hop / audioOut.sampleRate();
			baselineDurationMs = (int) Math.round((baselineCount - 1) * hopMs);
		} 
		else {
			// Gesture-timed baseline duration: use the curve's own recorded timing if available.
			// PACurveMaker already tracks "timeOffset" between first and last point.
			baselineDurationMs = Math.max(0, curve.getTimeOffset()); // int ms (adjust if getter differs)
		}
		if (isVerbose) {
			println("-- baseLineCount = "+ baselineCount +"; baseLineDurationMs = "+ baselineDurationMs);
		}
	}

	
	/**
	 * Iterates over brushShapesList and draws the brushstrokes stored in 
	 * each PACurveMaker in the list. 
	 */
	public void drawBrushShapes() {
		drawBrushes(granularBrushes, readyBrushColor1, hoverBrushColor1, selectedBrushColor1);
		drawBrushes(samplerBrushes, readyBrushColor2, hoverBrushColor2, selectedBrushColor2);
	}
	
	public void drawBrushes(List<? extends AudioBrush> brushes, int readyColor, int hoverColor, int selectedColor) {
		if (brushes.isEmpty()) return;
		for (int i = 0; i < brushes.size(); i++) {
			AudioBrush b = brushes.get(i);
			PACurveMaker brush = b.curve();
			boolean isHover = (b == hoverBrush);
			boolean isSelected = (b == activeBrush);
			int fill = readyColor;
			if (isSelected) {
				fill = selectedColor;
				brush.setEpsilon(b.cfg().rdpEpsilon);
				brush.setCurveSteps(b.cfg().curveSteps);
			} 
			else if (isHover) {
				fill = hoverColor;
			}
			GestureGranularConfig.PathMode pm = b.cfg().pathMode;
			PACurveUtility.shapeDraw(this, brush.getBrushShape(), fill, fill, 2);
			int w = 1, d = 5;
			int lc = isSelected ? lineColor : dimLineColor;
			int cc = isSelected ? circleColor : dimCircleColor;
			switch (pm) {
			case REDUCED_POINTS:
				PACurveUtility.lineDraw(this, brush.getReducedPoints(), lc, w);
				PACurveUtility.pointsDraw(this, brush.getReducedPoints(), cc, d);
				break;
			case CURVE_POINTS:
				PACurveUtility.lineDraw(this, brush.getCurvePoints(), lc, w);
				PACurveUtility.pointsDraw(this, brush.getCurvePoints(), cc, d);
				break;
			case ALL_POINTS:
				PACurveUtility.lineDraw(this, brush.getAllPoints(), lc, w);
				PACurveUtility.pointsDraw(this, brush.getAllPoints(), cc, d);
				break;
			default: {
				PACurveUtility.lineDraw(this, brush.getAllPoints(), lc, w);
				PACurveUtility.pointsDraw(this, brush.getAllPoints(), cc, d);
			}
			}
		}
		if (activeBrush == null) {
			currentGranStatus = "Draw or select a brushstroke.";
		}
	}
	
	ArrayList<PVector> getPathPoints(AudioBrush b) {
		PACurveMaker cm = b.curve();
		switch (b.cfg().pathMode) {
			case ALL_POINTS: return cm.getAllPoints();
			case REDUCED_POINTS: return cm.getReducedPoints();
			case CURVE_POINTS: return cm.getCurvePoints();
			default: return cm.getAllPoints();
		}
	}

	/**
	 * @param b    an AudioBrushLite instance
	 * @return     GestureSchedule for the current pathMode of the brush
	 */
	public GestureSchedule getScheduleForBrush(AudioBrush b) {
		switch (b.cfg().pathMode) {
			case REDUCED_POINTS: return b.curve().getReducedSchedule(b.cfg().rdpEpsilon);
			case CURVE_POINTS: return b.curve().getCurveSchedule(b.cfg().rdpEpsilon, curveSteps, isAnimating);
			case ALL_POINTS: default: return b.curve().getAllPointsSchedule();
		}
	}


	/*             END DRAWING METHODS              */
	
	/**
	 * @param poly    a polygon described by an ArrayList of PVector
	 * @return        true if the mouse is within the bounds of the polygon, false otherwise
	 */
	public boolean mouseInPoly(ArrayList<PVector> poly) {
		return pointInPoly(poly, mouseX, mouseY);
	}

	/**
	 * @param poly    a polygon described by an ArrayList of PVector
	 * @param x       x-coordinate
	 * @param y       y-coordinate
	 * @return        true if the mouse is within the bounds of the polygon, false otherwise
	 */
	public boolean pointInPoly(ArrayList<PVector> poly, int x, int y) {
		return PABezShape.pointInPoly(poly, x, y);
	}

	/**
	 * Reinitializes audio and clears event lists. If isClearCurves is true, clears brushShapesList.
	 * @param isClearCurves
	 */
	public void reset(boolean isClearCurves) {
		// note that initAudio also clears TimedLocation event lists
		initAudio();
		if (audioFile != null)
			loadAudioFile(audioFile);
		if (this.curveMaker != null) this.curveMaker = null;
		this.activeSamplerIndex = 0;
		if (isClearCurves) {
			if (this.samplerBrushes != null) this.samplerBrushes.clear();
			if (this.granularBrushes != null) this.granularBrushes.clear();
			println("----->>> RESET audio, event points and brushes <<<------");
		}
		else {
			println("----->>> RESET audio and event points <<<------");
		}
	}
	
	//------------- REMOVE BRUSHES -------------//

	// TODO verify correct action -- active brush is not always getting set
	
	/**
	 * Removes the current active PACurveMaker instance, flagged by a highlighted brush stroke,
	 * from brushShapesList, if there is one.
	 */
	public void removeActiveBrush() {
		if (mode == Mode.DRAW_EDIT_SAMPLER) {
			if (samplerBrushes != null && !samplerBrushes.isEmpty()) {
				// remove the active (highlighted) brush
				int idx = samplerBrushes.indexOf(activeSamplerBrush);
				samplerBrushes.remove(activeSamplerBrush);
				if (samplerBrushes.size() == idx)
					curveMaker = null;
				// if (isVerbose) println("-->> removed activeBrush");
			}
		}
		else if (mode == Mode.DRAW_EDIT_GRANULAR) {
			if (activeGranularBrush != null && !granularBrushes.isEmpty()) {
				int idx = granularBrushes.indexOf(hoverBrush);
				granularBrushes.remove(hoverBrush);
				if (granularBrushes.size() == idx) {
					curveMaker = null;
				}
			}
		}
		else {
			// play mode, no edting
		}
	}
	
	/**
	 * Removes the newest PACurveMaker instance, shown as a brush stroke
	 * in the display, from brushShapesList.
	 */
	public void removeNewestBrush() {
		if (mode == Mode.DRAW_EDIT_SAMPLER) {
			if (samplerBrushes != null && !samplerBrushes.isEmpty()) {
				int idx = samplerBrushes.size();
				samplerBrushes.remove(idx - 1);	// brushShapes array starts at 0
				if (isVerbose) println("-->> removed newest brush");
				curveMaker = null;
			}
		}
		else if (mode == Mode.DRAW_EDIT_GRANULAR) {
			if (granularBrushes != null && !granularBrushes.isEmpty()) {
				int idx = granularBrushes.size();
				granularBrushes.remove(idx - 1);
				if (isVerbose) println("-->> removed newest brush");
				curveMaker = null;
			}
		}
		else {

		}
	}

	/**
	 * Removes the oldest brush in brushShapesList.
	 */
	public void removeOldestBrush() {
		if (mode == Mode.DRAW_EDIT_SAMPLER) {
			if (samplerBrushes != null && !samplerBrushes.isEmpty()) {
				samplerBrushes.remove(0);	// brushShapes array starts at 0
				if (isVerbose) println("-->> removed oldest brush");
				if (samplerBrushes.isEmpty()) curveMaker = null;
			}
		}
		else if (mode == Mode.DRAW_EDIT_GRANULAR) {
			if (granularBrushes != null && !granularBrushes.isEmpty()) {
				granularBrushes.remove(0);
				if (isVerbose) println("-->> removed oldest brush");
				if (granularBrushes.isEmpty()) curveMaker = null;
			}
		}
		else {

		}
	}
	
	
	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                 TIME/LOCATION/ACTION METHODS                   */
	/*                                                                */
	/*----------------------------------------------------------------*/
	
	// ------------- STANDARD SAMPLER AND GRANULAR BRUSH SCHEDULING ------------- //

	void scheduleSamplerBrushClick(SamplerBrush sb, int clickX, int clickY) {
		if (sb == null) return;
		ArrayList<PVector> pts = getPathPoints(sb);
		if (pts == null || pts.size() < 2) return;
		ensureSamplerReady();
		GestureGranularConfig snap = sb.snapshot();
		GestureSchedule sched = scheduleBuilder.build(sb.curve(), snap, audioOut.sampleRate());
		// GestureSchedule sched = getScheduleForBrush(sb);  // just the brush settings here
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
                playSample(pos, calcSampleLen(), samplerGain, panning);
        		pointTimeLocs.add(new TimedLocation(sampleX, sampleY, tl.getDurationMs() + millis()));
	            tl.setStale(true);
	        } 
	        else {
	            return;
	        }
	    });
	    samplerTimeLocs.removeIf(TimedLocation::isStale);
	}
		
	void scheduleGranularBrushClick(GranularBrush gb, int clickX, int clickY) {
	    if (gb == null) return;
	    ArrayList<PVector> pts = getPathPoints(gb);
	    if (pts == null || pts.size() < 2) return;
	    ensureGranularReady();
	    float[] buf = (granSignal != null) ? granSignal : audioSignal;	    
	    // Snapshot config ONCE so schedule + params are consistent
	    GestureGranularConfig snap = gb.snapshot();
		// TODO:
		// Define explicit semantics for cfg.resampleCount <= 0 and cfg.targetDurationMs <= 0.
		// Current assumption:
		//   resampleCount <= 0 → keep original gesture timing
		//   targetDurationMs <= 0 → keep natural duration
	    // apply resample/duration/warp via scheduleBuilder
	    GestureSchedule sched = scheduleBuilder.build(gb.curve(), snap, audioOut.sampleRate());
	    if (sched == null || sched.isEmpty()) return;
	    if (isVerbose) {
		    println("sched.size=" + sched.size()
		    + " durationMs=" + sched.durationMs()
		    + " cfg.resampleCount=" + snap.resampleCount
		    + " cfg.targetDurationMs=" + snap.targetDurationMs
		    + " cfg.pathMode=" + snap.pathMode
		    + " warp=" + snap.warpShape);
	    }
	    
	    boolean isGesture = gb.cfg().hopMode == HopMode.GESTURE;
		GestureGranularParams gParams = gb.cfg().build().toParams();
		// GestureSchedule sched = getScheduleForBrush(gb); 
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
			// we can rely on sched for accurate times -- TODO drop in next iteration
			// int t = (isGesture) ? startTime + Math.round(sched.timesMs[i++]) : startTime + i++ * hopMs;
			// int d = (isGesture) ? 200 : durMsFixed;
			int t = startTime + Math.round(sched.timesMs[i++]);
			int d = 200;
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
	 * Draws a circle at the location of an audio trigger (mouseDown event).
	 * @param x		x coordinate of circle
	 * @param y		y coordinate of circle
	 */
	public void drawCircle(int x, int y) {
		//float size = isRaining? random(10, 30) : 60;
		fill(circleColor);
		noStroke();
		circle(x, y, 18);
	}

	
	

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

	// ----- BEGIN OLD LISTENER-STYLE CODE ----- //	
	
	// TODO revise
	/**
	 * Sets up sample-accurate AudioListener called from the Minim audio processing loop.
	 * We use the samples() methods in the AudioListener interface to call our processAudioBlock()
	 * method. The audio samples don't concern us, just the regular interval over which they are processed:
	 * essentially, we have a timer that wakes up at a regular interval. 
	 * 
	 */
	public void initListener() {
		// define the AudioListener class instance inline
		audioOut.addListener(new AudioListener() {

			/**
			 * we use the samples() methods  in the AudioListener interface to call processAudioBlock()
			 * the audio samples don't concern us, just the regular interval over which they are processed
			 */
			@Override
			public void samples(float[] samp) {
				// mono callback (blockSize == samp.length)
				processAudioBlock(samp.length);
			}

			@Override
			public void samples(float[] left, float[] right) {
				// stereo callback (blockSize == left.length)
				processAudioBlock(left.length);
			}

			private void processAudioBlock(int blockSize) {
				long blockStart = audioBlockStartSample.getAndAdd(blockSize);
				long nextBlockStart = blockStart + blockSize;
				audioNextBlockStartSample.set(nextBlockStart);

				audioSched.processBlock(
						blockStart,
						blockSize,
						(Happening h, int offsetInBlock) -> {
							if (h instanceof SamplerPointHappening sph) {
								playSample(sph.samplePos, sph.len, sph.gain, sph.env, sph.pitchRatio, sph.pan);
							}
							// We add an event for the draw() loop
							// dotInbox.add(new ActiveDot(h.x(), h.y(), System.currentTimeMillis() + dotLifeMs));
						},
						null // span handler later
						);
			}
		});
		audioSched.setLatePolicy(AudioScheduler.LatePolicy.DROP);
	}

	// TODO revise or drop
	static final class ActiveDot {
		final int x, y;
		final long expireMs;

		ActiveDot(int x, int y, long expireMs) {
			this.x = x;
			this.y = y;
			this.expireMs = expireMs;
		}
	}


	// TODO revise or drop
	// Happening at a point with PASamplerInstrument use
	static final class SamplerPointHappening implements Happening {
		final int x, y;
		final int samplePos, len;
		final float gain, pan;
		final float pitchRatio;     // pitch scaling as rate multiplier
		final ADSRParams env;
		final float noteMs;

		SamplerPointHappening(int x, int y,
				int samplePos, int len,
				float gain, float pan,
				float pitch,
				ADSRParams env, int noteMs) {
			this.x = x; this.y = y;
			this.samplePos = samplePos;
			this.len = len;
			this.gain = gain;
			this.pan = pan;
			this.pitchRatio = pitch;
			this.env = env;
			this.noteMs = noteMs;
		}

		@Override public int x() { return x; }
		@Override public int y() { return y; }
	}

		
	// ----- END OLD LISTENER-STYLE CODE ----- //
	
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------


	
	
	
	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                          GUI METHODS                           */
	/*                                                                */
	/*----------------------------------------------------------------*/

	// GUI Variable declarations 
	GWindow controlWindow;
	GPanel controlPanel; 
	GLabel pathSourceLabel; 
	GToggleGroup pathSourceGroup; 
	GOption allOption; 
	GOption rdpOption; 
	GOption curveOption; 
	GSlider rdpEpsilonSlider; 
	GSlider curvePointsSlider; 
	GLabel pitchLabel; 
	GTextField pitchShiftText; 
	// GLabel timingLabel; 
	GLabel resampleLabel;
	GSlider resampleSlider; 
	GTextField resampleField; 
	GLabel durationLabel;
	GSlider durationSlider; 
	GTextField durationField; 
	GLabel burstLabel;
	GSlider burstSlider;
	GLabel hopModeLabel; 
	GToggleGroup hopModeGroup; 
	GOption gestureOption; 
	GLabel grainLengthLabel; 
	GSlider grainLengthSlider; 
	GLabel hopLengthLabel; 
	GSlider hopLengthSlider; 
	GOption fixedOption; 
	GLabel warpLabel;
	GSlider warpSlider; 
	GLabel epsilonSliderLabel; 
	GLabel curvePointsLabel; 
	GLabel gainLabel; 
	GSlider gainSlider; 
	GTextField grainLengthField; 
	GTextField hopLengthField; 
	GToggleGroup warpGroup;
	GOption linearWarpOption;
	GOption expWarpOption;
	GOption squareRootOption;
	GOption customWarpOption;
	GOption arcLengthTimeOption;
	GLabel envelopeLabel; 
	GDropList envelopeMenu; 

	
	GTextArea commentsField;    // testing

	String[] adsrItems = {"Pluck", "Soft", "Percussion", "Fade", "Swell", "Pad"};

	// Create all the GUI controls. 
	public void createGUI(){
	  createControlWindow();
	  createControlPanel();
	  createControls();
	  addControlsToPanel();
	}

	/**
	 * 
	 */
	public void createControlWindow() {
		G4P.messagesEnabled(false);
		G4P.setGlobalColorScheme(GCScheme.BLUE_SCHEME);
		G4P.setMouseOverEnabled(false);
		controlWindow = GWindow.getWindow(this, "Granular Synth", 60, 420, 460, 600, JAVA2D);
		controlWindow.noLoop();
		controlWindow.setActionOnClose(G4P.EXIT_APP);
		controlWindow.addDrawHandler(this, "winDraw");
		controlWindow.addKeyHandler(this, "winKey");
	}

	/**
	 * 
	 */
	public void createControlPanel() {
		controlPanel = new GPanel(controlWindow, 5, 5, 470, 600, "Settings");
		controlPanel.setCollapsible(false);
		controlPanel.setCollapsed(false);
		controlPanel.setDraggable(false);
		controlPanel.setText("Settings");
		controlPanel.setTextBold();
		// http://lagers.org.uk/g4p/guides/g04-colorschemes.html
		controlPanel.setLocalColor(2, color(254, 246, 233));
		controlPanel.setOpaque(true);
		controlPanel.addEventHandler(this, "controlPanel_hit");
	}

	// a test
	public void createCommentsField() {
		commentsField = new GTextArea(controlWindow, 5, 480, 470, 70, G4P.SCROLLBARS_NONE);
		commentsField.setOpaque(true);
		commentsField.setWrapWidth(460);
		commentsField.addEventHandler(this, "comments_hit");
		commentsField.setText("blah blah blah");
	}
	
	/**
	 * 
	 */
	public void createControls() {
		int yPos = 30;
		int yInc = 30;
		pathSourceLabel = new GLabel(controlWindow, 10, yPos, 80, 20);
		pathSourceLabel.setText("Path Source");
		pathSourceLabel.setOpaque(true);
		pathSourceGroup = new GToggleGroup();
		allOption = new GOption(controlWindow, 100, yPos, 100, 20);
		allOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
		allOption.setText("All Points");
		allOption.setOpaque(false);
		allOption.addEventHandler(this, "allOption_clicked");
		rdpOption = new GOption(controlWindow, 210, yPos, 120, 20);
		rdpOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
		rdpOption.setText("Reduced Points");
		rdpOption.setOpaque(false);
		rdpOption.addEventHandler(this, "rdpOption_clicked");
		curveOption = new GOption(controlWindow, 340, yPos, 100, 20);
		curveOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
		curveOption.setText("Curve Points");
		curveOption.setOpaque(false);
		curveOption.addEventHandler(this, "curveOption_clicked");
		// epsilon and curvePoints
		yPos += yInc;
		rdpEpsilonSlider = new GSlider(controlWindow, 10, yPos, 200, 40, 10.0f);
		rdpEpsilonSlider.setShowValue(true);
		rdpEpsilonSlider.setShowLimits(true);
		rdpEpsilonSlider.setLimits(2, 1, 50);
		rdpEpsilonSlider.setNbrTicks(8);
		rdpEpsilonSlider.setNumberFormat(G4P.INTEGER, 1);
		rdpEpsilonSlider.setOpaque(false);
		rdpEpsilonSlider.addEventHandler(this, "rdpEpsilonSlider_changed");
		curvePointsSlider = new GSlider(controlWindow, 230, yPos, 200, 40, 10.0f);
		curvePointsSlider.setShowValue(true);
		curvePointsSlider.setShowLimits(true);
		curvePointsSlider.setLimits(2, 2, 128);
		curvePointsSlider.setNumberFormat(G4P.INTEGER, 0);
		curvePointsSlider.setOpaque(false);
		curvePointsSlider.addEventHandler(this, "curvePointsSlider_changed");
		// epsilon and curve labels
		yPos += (yInc + 10);
		epsilonSliderLabel = new GLabel(controlWindow, 10, yPos, 80, 20);
		epsilonSliderLabel.setText("RDP epsilon");
		epsilonSliderLabel.setOpaque(true);
		curvePointsLabel = new GLabel(controlWindow, 230, yPos, 80, 20);
		curvePointsLabel.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
		curvePointsLabel.setText("Curve Points");
		curvePointsLabel.setOpaque(true);
		// pitch
		yPos += yInc + 10;
		pitchLabel = new GLabel(controlWindow, 10, yPos, 140, 20);
		pitchLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
		pitchLabel.setText("Pitch shift (semitones)");
		pitchLabel.setOpaque(true);
		pitchShiftText = new GTextField(controlWindow, 160, yPos, 90, 24, G4P.SCROLLBARS_NONE);
		pitchShiftText.setNumeric(-24.0f, 24.0f, 0.0f);
		pitchShiftText.setText("0.0");
		pitchShiftText.setPromptText("0.0");
		pitchShiftText.setOpaque(true);
		pitchShiftText.addEventHandler(this, "pitchShiftText_changed");
		// gain
		yPos += yInc;
		gainLabel = new GLabel(controlWindow, 10, yPos + 10, 80, 20);
		gainLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
		gainLabel.setText("Gain (dB)");
		gainLabel.setOpaque(true);
		gainSlider = new GSlider(controlWindow, 100, yPos, 210, 40, 10.0f);
		gainSlider.setShowValue(true);
		gainSlider.setShowLimits(true);
		gainSlider.setLimits(1, -60, 6);
		gainSlider.setNbrTicks(24);
		gainSlider.setNumberFormat(G4P.INTEGER, 0);
		gainSlider.setOpaque(false);
		gainSlider.addEventHandler(this, "gainSlider_changed");
		// resampling
		yPos += yInc + 10;
		resampleLabel = new GLabel(controlWindow, 10, yPos + 10, 80, 20);
		resampleLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
		resampleLabel.setText("Resample");
		resampleLabel.setOpaque(true);
		resampleSlider = new GSlider(controlWindow, 100, yPos, 256, 40, 10.0f);
		resampleSlider.setShowValue(false);
		resampleSlider.setLimits(gConfig.resampleCount, 2, 2048); 
		resampleSlider.setNumberFormat(G4P.INTEGER, 0);
		resampleSlider.setOpaque(false);
		resampleSlider.addEventHandler(this, "resampleSlider_changed");
		resampleField = new GTextField(controlWindow, 366, yPos + 10, 70, 24, G4P.SCROLLBARS_NONE);
		resampleField.setText("" + gConfig.resampleCount);
		resampleField.setTextEditEnabled(false);
		// duration
		yPos += yInc;
		durationLabel = new GLabel(controlWindow, 10, yPos + 10, 80, 20);
		durationLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
		durationLabel.setText("Duration (ms)");
		durationLabel.setOpaque(true);
		durationSlider = new GSlider(controlWindow, 100, yPos, 256, 40, 10.0f);
		durationSlider.setShowValue(true);
		durationSlider.setLimits(gConfig.targetDurationMs, 50, 16000);
		durationSlider.setNumberFormat(G4P.INTEGER, 0);
		durationSlider.setOpaque(false);
		durationSlider.addEventHandler(this, "durationSlider_changed");
		durationField = new GTextField(controlWindow, 366, yPos + 10, 70, 24, G4P.SCROLLBARS_NONE);
		durationField.setText("" + gConfig.targetDurationMs);
		durationField.setTextEditEnabled(false);
		// hop mode
		yPos += yInc + 20;
		hopModeLabel = new GLabel(controlWindow, 10, yPos, 80, 20);
		hopModeLabel.setText("Hop Mode");
		hopModeLabel.setOpaque(true);
		hopModeGroup = new GToggleGroup();
		gestureOption = new GOption(controlWindow, 100, yPos, 100, 20);
		gestureOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
		gestureOption.setText("Gesture");
		gestureOption.setOpaque(false);
		gestureOption.addEventHandler(this, "gestureOption_clicked");
		fixedOption = new GOption(controlWindow, 210, yPos, 100, 20);
		fixedOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
		fixedOption.setText("Fixed");
		fixedOption.setOpaque(false);
		fixedOption.addEventHandler(this, "fixedOption_clicked");
		// burst grain count	
		yPos += yInc;
		burstLabel = new GLabel(controlWindow, 10, yPos + 10, 80, 20);
		burstLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
		burstLabel.setText("Burst count");
		burstLabel.setOpaque(true);
		burstSlider = new GSlider(controlWindow, 100, yPos, 256, 40, 10.0f);
		burstSlider.setShowValue(true);
		burstSlider.setShowLimits(true);
		burstSlider.setLimits(1, 1, 16);
		burstSlider.setNumberFormat(G4P.INTEGER, 0);
		burstSlider.setOpaque(false);
		//burstSlider.setNbrTicks(16);
		//burstSlider.setShowTicks(true);
		//burstSlider.setStickToTicks(true);
		burstSlider.addEventHandler(this, "burstSlider_changed");
		// grain length
		yPos += yInc + 8;
		grainLengthLabel = new GLabel(controlWindow, 10, yPos + 10, 80, 20);
		grainLengthLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
		grainLengthLabel.setText("Grain length");
		grainLengthLabel.setOpaque(true);
		grainLengthSlider = new GSlider(controlWindow, 100, yPos, 256, 40, 10.0f);
		grainLengthSlider.setShowValue(false);
		grainLengthSlider.setShowLimits(true);
		grainLengthSlider.setLimits(2048, 64, 4096);
		grainLengthSlider.setNumberFormat(G4P.INTEGER, 0);
		grainLengthSlider.setOpaque(false);
		grainLengthSlider.addEventHandler(this, "grainLengthSlider_changed");
		grainLengthField = new GTextField(controlWindow, 366, yPos + 10, 70, 24, G4P.SCROLLBARS_NONE);
		grainLengthField.setText("2048");
		grainLengthField.setOpaque(true);
		grainLengthField.setTextEditEnabled(false);
		grainLengthField.setText(""+ gConfig.grainLengthSamples);
		// hop length
		yPos += yInc;
		hopLengthLabel = new GLabel(controlWindow, 10, yPos + 10, 80, 20);
		hopLengthLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
		hopLengthLabel.setText("Hop length");
		hopLengthLabel.setOpaque(true);
		hopLengthSlider = new GSlider(controlWindow, 100, yPos, 256, 40, 10.0f);
		hopLengthSlider.setShowValue(false);
		hopLengthSlider.setShowLimits(true);
		hopLengthSlider.setLimits(512, 64, 4096);
		hopLengthSlider.setNumberFormat(G4P.INTEGER, 0);
		hopLengthSlider.setOpaque(false);
		hopLengthSlider.addEventHandler(this, "hopLengthSlider_changed");
		hopLengthField = new GTextField(controlWindow, 366, yPos + 10, 70, 24, G4P.SCROLLBARS_NONE);
		hopLengthField.setText("512");
		hopLengthField.setOpaque(true);
		hopLengthField.setTextEditEnabled(false);
		hopLengthField.setText(""+ gConfig.hopLengthSamples);
		// warp
		yPos += yInc + 10;
		warpLabel = new GLabel(controlWindow, 10, yPos + 10, 80, 20);
		warpLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
		warpLabel.setText("Warp");
		warpLabel.setOpaque(true);
		// warp options
		yPos += 10;
		warpGroup = new GToggleGroup();
		linearWarpOption = new GOption(controlWindow, 100, yPos, 80, 20);
		linearWarpOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
		linearWarpOption.setText("Linear");
		linearWarpOption.setOpaque(false);
		linearWarpOption.setSelected(true);
		linearWarpOption.addEventHandler(this, "linearWarpOption_clicked");
		expWarpOption = new GOption(controlWindow, 190, yPos, 80, 20);
		expWarpOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
		expWarpOption.setText("Exp");
		expWarpOption.setOpaque(false);
		expWarpOption.addEventHandler(this, "expWarpOption_clicked");
		squareRootOption = new GOption(controlWindow, 280, yPos, 80, 20);
		squareRootOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
		squareRootOption.setText("Root");
		squareRootOption.setOpaque(false);
		squareRootOption.addEventHandler(this, "squareRootOption_clicked");	
		customWarpOption = new GOption(controlWindow, 370, yPos, 80, 20);
		customWarpOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
		customWarpOption.setText("Custom");
		customWarpOption.setOpaque(false);
		customWarpOption.addEventHandler(this, "customWarpOption_clicked");
		// warp exponent
		yPos += yInc;
		warpSlider = new GSlider(controlWindow, 100, yPos - 10, 256, 40, 10.0f);
		warpSlider.setShowValue(true);
		warpSlider.setLimits(1f, 0.25f, 4f);
		warpSlider.setNumberFormat(G4P.DECIMAL, 2);
		warpSlider.setOpaque(false);
		warpSlider.addEventHandler(this, "warpSlider_changed");
		warpSlider.setVisible(true);
		warpSlider.setEnabled(false); // only if LINEAR is selected option
		// envelope menu
		yPos += yInc;
		envelopeLabel = new GLabel(controlWindow, 10, yPos, 80, 40);
		envelopeLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
		envelopeLabel.setText("Sampler\nEnvelope");
		envelopeLabel.setOpaque(true);
		envelopeMenu = new GDropList(controlWindow, 100, yPos, 120, 80, 3, 10);
		envelopeMenu.setItems(adsrItems, 0);
		envelopeMenu.addEventHandler(this, "envelopeMenu_clicked");
		// arc length time option3
		
		arcLengthTimeOption = new GOption(controlWindow, 280, yPos, 120, 20);
		arcLengthTimeOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
		arcLengthTimeOption.setText("Arc Length Time");
		arcLengthTimeOption.setOpaque(false);
		arcLengthTimeOption.setSelected(false);
		arcLengthTimeOption.addEventHandler(this, "arcLengthTimeOption_clicked");
		// not showing this option, which only applies to curves and has minimal effect
		arcLengthTimeOption.setVisible(false);
	}

	/**
	 * 
	 */
	public void addControlsToPanel() {
		controlPanel.setVisible(true);
		controlPanel.addControl(pathSourceLabel);
		// add path source controls
		pathSourceGroup.addControl(allOption);
		allOption.setSelected(true);
		controlPanel.addControl(allOption);
		pathSourceGroup.addControl(rdpOption);
		controlPanel.addControl(rdpOption);
		pathSourceGroup.addControl(curveOption);
		controlPanel.addControl(curveOption);	
		// epsilon
		controlPanel.addControl(epsilonSliderLabel);
		controlPanel.addControl(rdpEpsilonSlider);
		controlPanel.addControl(curvePointsLabel);
		controlPanel.addControl(curvePointsSlider);
		// hpp mode
		controlPanel.addControl(hopModeLabel);
		hopModeGroup.addControl(gestureOption);
		gestureOption.setSelected(true);
		controlPanel.addControl(gestureOption);
		hopModeGroup.addControl(fixedOption);
		controlPanel.addControl(fixedOption);
		// burst count
		controlPanel.addControl(burstLabel);
		controlPanel.addControl(burstSlider);
		// grain length
		controlPanel.addControl(grainLengthLabel);
		controlPanel.addControl(grainLengthSlider);
		controlPanel.addControl(grainLengthField);
		// hop length
		controlPanel.addControl(hopLengthLabel);
		controlPanel.addControl(hopLengthSlider);
		controlPanel.addControl(hopLengthField);
		// pitch shift
		controlPanel.addControl(pitchLabel);
		controlPanel.addControl(pitchShiftText);
		// gain
		controlPanel.addControl(gainLabel);
		controlPanel.addControl(gainSlider);
		// resample
		controlPanel.addControl(resampleLabel);
		controlPanel.addControl(resampleSlider);
		controlPanel.addControl(resampleField);
		// change duration
		controlPanel.addControl(durationLabel);
		controlPanel.addControl(durationSlider);
		controlPanel.addControl(durationField);
		// warp
		controlPanel.addControl(warpLabel);
		warpGroup.addControl(linearWarpOption);
		warpGroup.addControl(expWarpOption);
		warpGroup.addControl(squareRootOption);
		warpGroup.addControl(customWarpOption);
		linearWarpOption.setSelected(true);
		controlPanel.addControl(linearWarpOption);
		controlPanel.addControl(expWarpOption);
		controlPanel.addControl(squareRootOption);
		controlPanel.addControl(customWarpOption);
		controlPanel.addControl(warpSlider);
		// envelope
		controlPanel.addControl(envelopeLabel);
		controlPanel.addControl(envelopeMenu);
		controlPanel.setCollapsed(false);
		// arcLengthTime
		controlPanel.addControl(arcLengthTimeOption);
	}


	// GUI Handlers
	/*
	 * Conditional enabling of handlers
	 * 
	 * Every handler gets: "if (!isEditable()) return;" -- mode == Mode.PLAY_ONLY
	 * Every handler gets: "if (guiSyncing) return;"    -- sync is active
	 * 
	 * Global granular defaults include: "if (mode != Mode.DRAW_EDIT_GRANULAR) return;"
	 *   grainLengthSlider_changed, hopLengthSlider_changed
	 *   gainSlider_changed
	 *   pitchShiftText_changed
	 *   linearWarpOption_clicked, expWarpOption_clicked, squareRootOption_clicked, customWarpOption_clicked
	 *   warpSlider_changed
	 *   envelopeMenu_clicked
	 * 
	 * Handlers for brushstroke parameters include: "if (activeGranularBrush == null) return;"
	 *   Path mode options: allOption_clicked, rdpOption_clicked, curveOption_clicked
	 *   Path params: rdpEpsilonSlider_changed, curvePointsSlider_changed
	 *   Hop mode options: gestureOption_clicked, fixedOption_clicked
	 *   Timing overrides: resampleSlider_changed, durationSlider_changed
	 *  
	 */
	
	// our draw method, call each time through the event loop
	synchronized public void winDraw(PApplet appc, GWinData data) {
	  appc.background(color(212, 220, 228));
	}
	
	// respond to key in window
	public void winKey(PApplet appc, GWinData data, KeyEvent evt) {
		if (evt.getAction() == KeyEvent.RELEASE) parseKey(evt.getKey(), evt.getKeyCode());
	}
	

	public void controlPanel_hit(GPanel source, GEvent event) { 
		// println("controlPanel - GPanel >> GEvent." + event + " @ " + millis());
	} 

	public void allOption_clicked(GOption source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		/// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		if (activeBrush == null) return;
		gConfig.pathMode = GestureGranularConfig.PathMode.ALL_POINTS;
		recomputeUIBaselinesFromActiveBrush();
		syncGuiFromConfig();
		if (isVerbose) println("gConfig.pathMode = "+ gConfig.pathMode.toString());
	} 

	public void rdpOption_clicked(GOption source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		/// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		if (activeBrush == null) return;
		gConfig.pathMode = GestureGranularConfig.PathMode.REDUCED_POINTS;
		recomputeUIBaselinesFromActiveBrush();
		syncGuiFromConfig();
		if (isVerbose) println("gConfig.pathMode = "+ gConfig.pathMode.toString());
	} 

	public void curveOption_clicked(GOption source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		/// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		if (activeBrush == null) return;
		gConfig.pathMode = GestureGranularConfig.PathMode.CURVE_POINTS;
		recomputeUIBaselinesFromActiveBrush();
		syncGuiFromConfig();
		if (isVerbose) println("gConfig.pathMode = "+ gConfig.pathMode.toString());
	} 

	public void rdpEpsilonSlider_changed(GSlider source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		/// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		if (activeBrush == null) return;
		gConfig.rdpEpsilon = source.getValueF();
		if (event == GEvent.RELEASED) {
			recomputeUIBaselinesFromActiveBrush();
			syncGuiFromConfig();
			if (isVerbose) println("gConfig.rdpEpsilon = "+ gConfig.rdpEpsilon);
		}
	} 

	public void curvePointsSlider_changed(GSlider source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		/// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		if (activeBrush == null) return;
		gConfig.curveSteps = source.getValueI();
		if (event == GEvent.RELEASED) {
			recomputeUIBaselinesFromActiveBrush();
			syncGuiFromConfig();
			if (isVerbose) println("gConfig.curveSteps = "+ gConfig.curveSteps);
		}
	} 

	public void gestureOption_clicked(GOption source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		if (activeBrush == null) return;
		gConfig.hopMode = GestureGranularConfig.HopMode.GESTURE;
		recomputeUIBaselinesFromActiveBrush();
		syncGuiFromConfig();
		if (isVerbose) println("gConfig.hopMode = "+ gConfig.hopMode);
	} 

	public void fixedOption_clicked(GOption source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		if (activeBrush == null) return;
		gConfig.hopMode = GestureGranularConfig.HopMode.FIXED;
		recomputeUIBaselinesFromActiveBrush();
		syncGuiFromConfig();
		if (isVerbose) println("gConfig.hopMode = "+ gConfig.hopMode);
	} 
	
	public void burstSlider_changed(GSlider source, GEvent event) {
		if (!isEditable()) return;
		if (guiSyncing) return;
		if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		if (activeBrush == null) return;
		int v = source.getValueI();
		gConfig.burstGrains = v;
		if (event == GEvent.RELEASED) {
			if (isVerbose) println("gConfig.burstGrains = " + gConfig.burstGrains + " (slider=" + v + ")");
		}
		
	}

	public void resampleSlider_changed(GSlider source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		if (activeBrush == null) return;
		int v = source.getValueI();
		if (v == baselineCount) {
			gConfig.resampleCount = 0;
		} else {
			gConfig.resampleCount = v;
		}
		resampleField.setText(""+ gConfig.resampleCount);
		if (event == GEvent.RELEASED) {
			if (isVerbose) println("gConfig.resampleCount = " + gConfig.resampleCount + " (slider=" + v + ")");
		}
	} 

	public void durationSlider_changed(GSlider source, GEvent event) {
		if (!isEditable()) return;
		if (guiSyncing) return;
		// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		if (activeBrush == null) return;
		int v = source.getValueI();
		if (v == baselineDurationMs) {
			gConfig.targetDurationMs = 0;
		} else {
			gConfig.targetDurationMs = v;
		}
		durationField.setText("" + v);
		if (event == GEvent.RELEASED) {
			if (isVerbose) println("gConfig.targetDurationMs = " + gConfig.targetDurationMs + " (slider=" + v + ")");
		}
	}

	public void grainLengthSlider_changed(GSlider source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		int raw = source.getValueI();
		int quant = quantizeToStep(raw, 16);
		if (quant != raw) source.setValue(quant);
		gConfig.grainLengthSamples = quant;
		grainLengthField.setText(""+ gConfig.grainLengthSamples);
		if (event == GEvent.RELEASED) if (isVerbose) println("gConfig.grainLengthSamples = "+ gConfig.grainLengthSamples);
	}
	
	public void hopLengthSlider_changed(GSlider source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		int raw = source.getValueI();
		int quant = quantizeToStep(raw, 16);
		source.setValue(quant);
		gConfig.hopLengthSamples = quant;
		hopLengthField.setText(""+ gConfig.hopLengthSamples);
		if (event == GEvent.RELEASED) {
			recomputeUIBaselinesFromActiveBrush();
			syncGuiFromConfig();
			if (isVerbose) println("gConfig.hopLengthSamples = "+ gConfig.hopLengthSamples);
		}
	} 

	// this method was not linked to grainLengthField and will not be called
	public void grainLengthField_changed(GTextField source, GEvent event) {
		if (!isEditable()) return;
		if (guiSyncing) return;
		gConfig.grainLengthSamples = source.getValueI();
		if (isVerbose) println("grainLengthField - GTextField >> GEvent." + event + " @ " + millis());
	}

	// this method was not linked to hopLengthField and will not be called
	public void hopLengthField_changed(GTextField source, GEvent event) {
		if (!isEditable()) return;
		if (guiSyncing) return;
		gConfig.hopLengthSamples = source.getValueI();
		if (isVerbose) println("hopLengthField - GTextField >> GEvent." + event + " @ " + millis());
	}

	public void pitchShiftText_changed(GTextField source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		float v = source.getValueF();
		if (v != gConfig.pitchSemitones) {
			gConfig.pitchSemitones = source.getValueF();
			if (isVerbose) println("gConfig.pitchSemitones = "+ gConfig.pitchSemitones);
		}
	} 

	public void gainSlider_changed(GSlider source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		gConfig.gainDb = source.getValueF();
		if (event == GEvent.LOST_FOCUS) if (isVerbose) println("gConfig.gainDb = "+ gConfig.gainDb);
	} 
	
	public void linearWarpOption_clicked(GOption source, GEvent event) {
		if (!isEditable()) return;
		if (guiSyncing) return;
		if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		gConfig.warpShape = GestureGranularConfig.WarpShape.LINEAR;
		warpSlider.setEnabled(gConfig.warpShape != GestureGranularConfig.WarpShape.LINEAR);
		if (isVerbose) println("gConfig.warpShape = "+ gConfig.warpShape.toString());
	}
	
	public void expWarpOption_clicked(GOption source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		gConfig.warpShape = GestureGranularConfig.WarpShape.EXP;
		warpSlider.setEnabled(gConfig.warpShape != GestureGranularConfig.WarpShape.LINEAR);
		if (isVerbose) println("gConfig.warpShape = "+ gConfig.warpShape.toString());
	} 

	public void squareRootOption_clicked(GOption source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		gConfig.warpShape = GestureGranularConfig.WarpShape.SQRT;
		warpSlider.setEnabled(gConfig.warpShape != GestureGranularConfig.WarpShape.LINEAR);
		if (isVerbose) println("gConfig.warpShape = "+ gConfig.warpShape.toString());
	} 

	public void customWarpOption_clicked(GOption source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		gConfig.warpShape = GestureGranularConfig.WarpShape.CUSTOM;
		warpSlider.setEnabled(gConfig.warpShape != GestureGranularConfig.WarpShape.LINEAR);
		if (isVerbose) println("gConfig.warpShape = "+ gConfig.warpShape.toString());
	} 

	public void warpSlider_changed(GSlider source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		gConfig.warpExponent = source.getValueF();
		if (event == GEvent.RELEASED) println("gConfig.warpExponent = "+ gConfig.warpExponent);
	} 

	public void arcLengthTimeOption_clicked(GOption source, GEvent event) {
		if (!isEditable()) return;
		if (guiSyncing) return;
		gConfig.useArcLengthTime  = source.isSelected();
		// gConfig.useArcLengthTime = ;
		
	}
	
	public void envelopeMenu_clicked(GDropList source, GEvent event) { 
		if (!isEditable()) return;
		if (guiSyncing) return;
		if (mode == Mode.DRAW_EDIT_GRANULAR) {
			return;
		}
		// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
		int itemHit = source.getSelectedIndex();
		String itemName = adsrItems[itemHit];
		println("-- envelope "+ itemName +" selected");
		gConfig.env = envPreset(itemName);
		if (mode == Mode.DRAW_EDIT_SAMPLER) samplerEnv = envPreset(itemName);
		// println("envelopeMenu - GDropList >> GEvent." + event + " @ " + millis());
	} 
	
	public void printGConfigStatus() {
		// GestureGranularConfig has a toString() method, so far GestureGranularConfig.Builder does not
		String status = gConfig.build().toString();
		if (isVerbose) println(status);
	}

	
	// GUI Helpers
	
	// Envelopes
	static ADSRParams envPreset(String name) {
		switch (name) {
		case "Pluck"      : return new ADSRParams(0.9f, 0.005f, 0.18f, 0.0f, 0.12f);
		case "Soft"       : return new ADSRParams(0.9f, 0.020f, 0.60f, 0.15f, 0.25f); // new
		case "Percussion" : return new ADSRParams(0.9f, 0.002f, 0.20f, 0.10f, 0.18f);
		case "Fade"       : return new ADSRParams(0.9f, 0.50f, 0.0f, 1.0f, 0.50f);   // your trapezoid
		case "Swell"      : return new ADSRParams(0.9f, 0.90f, 0.40f, 0.70f, 0.90f);
		case "Pad"        : return new ADSRParams(0.9f, 1.40f, 0.60f, 0.85f, 1.80f);
		default           : return new ADSRParams(0.9f, 0.01f, 0.15f, 0.75f, 0.25f);
		}
	}

	/** Quantize an integer to the nearest multiple of step. */
	public static int quantizeToStep(int value, int step) {
	    int half = step / 2;
	    return ((value + half) / step) * step;
	}

	void syncGuiFromConfig() {
		guiSyncing = true;
		try {
			boolean hasStroke = (activeBrush != null);
			boolean canEdit = hasStroke && (isEditable());
			setControlsEnabled();

			// PathMode radio
			allOption.setSelected(gConfig.pathMode == GestureGranularConfig.PathMode.ALL_POINTS);
			rdpOption.setSelected(gConfig.pathMode == GestureGranularConfig.PathMode.REDUCED_POINTS);
			curveOption.setSelected(gConfig.pathMode == GestureGranularConfig.PathMode.CURVE_POINTS);

			// epsilon / curveSteps
			rdpEpsilonSlider.setValue(gConfig.rdpEpsilon);
			curvePointsSlider.setValue(gConfig.curveSteps);

			// hop mode radio
			gestureOption.setSelected(gConfig.hopMode == GestureGranularConfig.HopMode.GESTURE);
			fixedOption.setSelected(gConfig.hopMode == GestureGranularConfig.HopMode.FIXED);
			
			// burst count slider
			burstSlider.setValue(gConfig.burstGrains);

			// grain/hop length sliders + fields
			grainLengthSlider.setValue(gConfig.grainLengthSamples);
			grainLengthField.setText("" + gConfig.grainLengthSamples);

			hopLengthSlider.setValue(gConfig.hopLengthSamples);
			hopLengthField.setText("" + gConfig.hopLengthSamples);

			// gain/pitch
			gainSlider.setValue(gConfig.gainDb);
			pitchShiftText.setText(String.format("%.2f", gConfig.pitchSemitones));

			// ---- Resample & Duration: show baseline unless overridden ----
			int baseCountSafe = Math.max(2, baselineCount);         // slider min is 2
			int shownCount = (gConfig.resampleCount == 0) ? baseCountSafe : gConfig.resampleCount;
			shownCount = clampInt(shownCount, 2, 2048);
			resampleSlider.setLimits(shownCount, 2, 2048);
			resampleField.setText("" + shownCount);

			int baseDurSafe = Math.max(50, baselineDurationMs);     // slider min is 50
			int shownDur = (gConfig.targetDurationMs == 0) ? baseDurSafe : gConfig.targetDurationMs;
			shownDur = clampInt(shownDur, 50, 10000);
			durationSlider.setLimits(shownDur, 50, 16000);
			durationField.setText("" + shownDur);

			// warp radio + exponent
			linearWarpOption.setSelected(gConfig.warpShape == GestureGranularConfig.WarpShape.LINEAR);
			expWarpOption.setSelected(gConfig.warpShape == GestureGranularConfig.WarpShape.EXP);
			squareRootOption.setSelected(gConfig.warpShape == GestureGranularConfig.WarpShape.SQRT);
			customWarpOption.setSelected(gConfig.warpShape == GestureGranularConfig.WarpShape.CUSTOM);

			warpSlider.setValue(gConfig.warpExponent);

			// warp slider only editable when editEnabled AND warp is active
			warpSlider.setEnabled(canEdit && gConfig.warpShape != GestureGranularConfig.WarpShape.LINEAR);

			// (Optional) also disable warp radio buttons outside edit mode:
			linearWarpOption.setEnabled(canEdit);
			expWarpOption.setEnabled(canEdit);
			squareRootOption.setEnabled(canEdit);
			customWarpOption.setEnabled(canEdit);

		}
		finally {
			guiSyncing = false;
		}
	}

	static int clampInt(int v, int lo, int hi) {
		return (v < lo) ? lo : (v > hi ? hi : v);
	}

	void setControlsEnabled() {
		// ---- Mode flags ----
		final boolean inGranularEdit = (mode == Mode.DRAW_EDIT_GRANULAR);
		final boolean inSamplerEdit  = (mode == Mode.DRAW_EDIT_SAMPLER);
		final boolean inPlayOnly     = (mode == Mode.PLAY_ONLY);
		// Editable only in the two DRAW_EDIT modes
		final boolean canEdit = inGranularEdit || inSamplerEdit;
		// Brush selection
		final boolean hasBrush = (activeBrush != null);
		final boolean brushIsGranular = hasBrush && (activeBrush instanceof GranularBrush);
		final boolean brushIsSampler  = hasBrush && (activeBrush instanceof SamplerBrush);
		// PLAY_ONLY visibility rule:
		// - if no brush selected: show everything (disabled)
		// - if brush selected: show only relevant controls (disabled)
		final boolean showGranular = inGranularEdit || (inPlayOnly && (!hasBrush || brushIsGranular));
		final boolean showSampler  = inSamplerEdit  || (inPlayOnly && (!hasBrush || brushIsSampler));
		// Stroke-dependent controls: only enabled when editing AND a brush is selected
		final boolean strokeEditable = canEdit && hasBrush;

		// ---------- Stroke-dependent controls (shared) ----------
		allOption.setEnabled(strokeEditable);
		rdpOption.setEnabled(strokeEditable);
		curveOption.setEnabled(strokeEditable);
		rdpEpsilonSlider.setEnabled(strokeEditable);
		curvePointsSlider.setEnabled(strokeEditable);
		// Gesture timing option is relevant for both brush types, but only editable with a selected brush
		gestureOption.setEnabled(strokeEditable);
		// Fixed timing is Granular-only *global* default (carried to next GranularBrush)
		fixedOption.setVisible(showGranular);
		fixedOption.setEnabled(inGranularEdit);
		// If we're in Sampler context, ensure gesture timing is selected (sampler never uses fixed)
		if (inSamplerEdit || (inPlayOnly && brushIsSampler)) {
			gestureOption.setSelected(true);
		}
		// Schedule controls (shared)
		resampleSlider.setEnabled(strokeEditable);
		durationSlider.setEnabled(strokeEditable);
		// Fields are display-only (non-editable). Keep enabled for legibility/select/select.
		resampleField.setEnabled(true);
		durationField.setEnabled(true);

		// ---------- Granular-only synth controls ----------
		burstSlider.setVisible(showGranular);
		burstSlider.setEnabled(inGranularEdit);
		grainLengthSlider.setVisible(showGranular);
		grainLengthSlider.setEnabled(inGranularEdit);
		hopLengthSlider.setVisible(showGranular);
		hopLengthSlider.setEnabled(inGranularEdit);
		grainLengthField.setVisible(showGranular);
		grainLengthField.setEnabled(true);
		hopLengthField.setVisible(showGranular);
		hopLengthField.setEnabled(true);
		// Warp options
		linearWarpOption.setVisible(showGranular);
		linearWarpOption.setEnabled(inGranularEdit);
		expWarpOption.setVisible(showGranular);
		expWarpOption.setEnabled(inGranularEdit);
		squareRootOption.setVisible(showGranular);
		squareRootOption.setEnabled(inGranularEdit);
		customWarpOption.setVisible(showGranular);
		customWarpOption.setEnabled(inGranularEdit);
		warpSlider.setVisible(showGranular);
		final boolean warpActive =
				(gConfig != null && gConfig.warpShape != GestureGranularConfig.WarpShape.LINEAR);
		warpSlider.setEnabled(inGranularEdit && warpActive);

		// ---------- Sampler-only synth controls ----------
		// Envelope menu is sampler-only in your current UX (visible in sampler edit and play-only sampler context).
		envelopeMenu.setVisible(showSampler);
		envelopeMenu.setEnabled(inSamplerEdit && canEdit);

		// ---------- Global synth controls (shared) ----------
		// Gain/pitch are editable in either edit mode, read-only in play-only.
		gainSlider.setEnabled(canEdit);
		pitchShiftText.setEnabled(canEdit);

		// If you have any other always-visible labels, they are intentionally not handled here.
	}
		
	boolean isEditable() {
	    return (mode == Mode.DRAW_EDIT_GRANULAR) || (mode == Mode.DRAW_EDIT_SAMPLER);
	}
	
	void resetToDefaults() {
		if (!isEditable()) return;
		// Reset global
		gConfig.copyFrom(defaultGranConfig);
		// If a stroke is active, reset its per-stroke cfg too
		if (activeBrush != null) {
			activeBrush.cfg().copyFrom(defaultGranConfig);
			gConfig = activeBrush.cfg();
			recomputeUIBaselinesFromActiveBrush();
		} 
		else {
			// no stroke: baselines don't apply
			baselineCount = 0;
			baselineDurationMs = 0;
		}
		syncGuiFromConfig();
	}
	
	
	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                          EXPERIMENTS                           */
	/*                                                                */
	/*----------------------------------------------------------------*/
	
	
	void printGOptHints(float alpha) {
		if (activeBrush == null) return;

		GestureGranularConfig snap = activeBrush.snapshot();
		GestureSchedule schedule = scheduleBuilder.build(activeBrush.curve(), snap, audioOut.sampleRate());
		if (schedule == null || schedule.isEmpty()) {
			System.out.println("---- Granular OptHints [activeBrush] ----");
			System.out.println("No schedule / empty schedule.");
			System.out.println("----------------------------------------");
			return;
		}

		// Use the ACTUAL N and T produced by the builder
		int Nactual = schedule.points.size();

		// Duration from schedule times (robust to whether builder used drag time, target time, clamping, etc.)
		float[] tMs = schedule.timesMs;
		float t0 = tMs[0];
		float t1 = tMs[tMs.length - 1];
		float TactualMs = Math.max(0f, t1 - t0);

		// If you normally normalize/enforce for playback, do the same here so the debug matches playback.
		// (Optional but recommended)
		float[] norm = GestureSchedule.normalizeTimesToStartAtZero(tMs);
		GestureSchedule.enforceNonDecreasing(norm);
		TactualMs = norm[norm.length - 1] - norm[0];
		
		float S = PACurveUtility.pathLength(schedule.points);
		float Tsec = schedule.durationMs() / 1000f;
		float hsec = snap.hopLengthSamples / audioOut.sampleRate();
		float dHop = (S / Tsec) * hsec;   // px per hop
		float targetSpacingPx = dHop * alpha;     // or 0.75*dHop, etc.
		if (isVerbose) {
			println("-- path length px = "+ S);
			println("-- dHop = "+ dHop +", alpha = "+ alpha);
			println("-- targetSpacingPx = "+ targetSpacingPx);
		}
		
		StringBuffer info = new StringBuffer();
		this.optGrainCount = calcGranularOptHints(
				"activeBrush",
				Nactual,
				TactualMs,
				snap.hopLengthSamples,
				snap.grainLengthSamples,
				audioOut.sampleRate(),
				(ArrayList<PVector>) schedule.points,
				targetSpacingPx,   // d0
				1.0f,   // wt
				0.25f,   // ws
				info
				);
		if (isVerbose) println(info.toString());
	}

	public int calcGranularOptHints(
			String tag,
			int N, float Tms,
			int hopSamples, int grainLenSamples,
			float sr,
			List<PVector> scheduledPoints,   // the list you will actually schedule
			float targetSpacingPx, float wt, float ws,
			StringBuffer sb
			) {
		// ---- GUI ranges ----
		final int   RESAMPLE_MIN = 2,    RESAMPLE_MAX = 2048;
		final float DUR_MIN_MS   = 50f,  DUR_MAX_MS   = 16000f; // bump from 10000 -> 16000 if desired

		// Path length in px
		float S = PACurveUtility.pathLength(scheduledPoints);

		// Guard rails
		N = Math.max(RESAMPLE_MIN, Math.min(RESAMPLE_MAX, N));
		Tms = Math.max(DUR_MIN_MS, Math.min(DUR_MAX_MS, Tms));

		float T = Math.max(0.001f, Tms / 1000f);
		float h = hopSamples / sr;        // seconds
		float hopMs = 1000f * h;

		int Nm1 = Math.max(1, N - 1);
		float dtMs = Tms / Nm1;
		float r = dtMs / hopMs;           // dt/hop ratio
		float dsPx = (S > 0 ? S / Nm1 : Float.NaN);

		// Time-opt formulas
		float ToptRawMs = 1000f * (Nm1 * h);
		int NoptRaw = 1 + Math.max(1, Math.round((T * sr) / hopSamples));

		// Clamp to widget ranges
		float ToptMs = Math.max(DUR_MIN_MS, Math.min(DUR_MAX_MS, ToptRawMs));
		int Nopt = Math.max(RESAMPLE_MIN, Math.min(RESAMPLE_MAX, NoptRaw));

		// Compromise N* (time+space)
		int NstarRaw = -1, Nstar = -1;
		if (S > 0 && targetSpacingPx > 0 && (wt > 0 || ws > 0)) {
			float numer = wt * T * T + ws * S * S;
			float denom = wt * T * h + ws * S * targetSpacingPx;
			if (denom > 1e-9f) {
				float xStar = numer / denom; // x = N-1
				NstarRaw = 1 + Math.max(1, Math.round(xStar));
				Nstar = Math.max(RESAMPLE_MIN, Math.min(RESAMPLE_MAX, NstarRaw));
			}
		}

		float overlapPct = Float.NaN;
		if (grainLenSamples > 0) overlapPct = 100f * (1f - hopSamples / (float)grainLenSamples);

		String densityWord = (r > 1.25f) ? "SPARSE" : (r < 0.80f ? "DENSE" : "OK");
		
		// construct informative StringBuffer
		sb.append("\n---- Granular OptHints" + (tag == null ? "" : " [" + tag + "]") + " ----\n");
		sb.append("Current: N=" + N + "  Tms=" + fmt(Tms, 2) +
				"  hop=" + hopSamples + " samp (" + fmt(hopMs, 3) + " ms)" +
				"  grain=" + grainLenSamples + " samp  sr=" + fmt(sr, 1) +"\n");
		sb.append("Timing:  avg dt≈" + fmt(dtMs, 3) + " ms  dt/hop≈" + fmt(r, 2) + "  => " + densityWord +"\n");
		if (!Float.isNaN(dsPx)) sb.append("Space:   pathLen≈" + fmt(S, 2) + " px  avg ds≈" + fmt(dsPx, 3) + " px");
		if (!Float.isNaN(overlapPct)) sb.append("Overlap: ≈" + fmt(overlapPct, 1) + "% (1 - H/L)\n");
		sb.append("If keep N:  Topt≈" + fmt(ToptRawMs, 2) + " ms" +
				(ToptMs != ToptRawMs ? "  (clamped→" + fmt(ToptMs, 2) + ")" : ""));
		sb.append("\n");
		sb.append("If keep T:  Nopt≈" + NoptRaw +
				(Nopt != NoptRaw ? "  (clamped→" + Nopt + ")" : ""));
		sb.append("\n");
		if (Nstar > 0) {
			sb.append("Compromise (wt=" + fmt(wt,3) + ", ws=" + fmt(ws,3) + ", d0=" + fmt(targetSpacingPx,2) + "px): N*≈" +
					NstarRaw + (Nstar != NstarRaw ? " (clamped→" + Nstar + ")" : ""));
			sb.append("\n");
		}
		// Extra guidance when clamped
		if (NoptRaw > RESAMPLE_MAX && r > 1.25f) {
			sb.append("Note: Nopt exceeds max; likely sparse even at max resample. Consider CURVE_POINTS, shorter T, or smaller hop.\n");
		}
		if (ToptRawMs > DUR_MAX_MS && r > 1.25f) {
			sb.append("Note: Topt exceeds max; consider raising Duration max (you mentioned 16000ms), or increase N / use CURVE_POINTS.\n");
		}
		sb.append("-------------------------------------------");
		
		// optimal number of samples if time is kept as is
		return Nstar;
	}

	static String fmt(float v, int decimals) {
		return String.format("%." + decimals + "f", v);
	}



}
