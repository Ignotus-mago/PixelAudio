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
import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import net.paulhertz.pixelaudio.granular.*;
import net.paulhertz.pixelaudio.sampler.*;
//audio library
import ddf.minim.*;

//video export library
import com.hamoid.*;


/**
 * <h2>QUICK START</h2>
 * <p>
 * <ol>
 * <li>Launch TutorialOne_03_Drawing. The display window opens with a pre-loaded file, "Saucer_mixdown.wav".
 * The audio data is displayed as grayscale values. A spectrum of rainbow colors overlaid on the 
 * image follows the Signal Path, the mapping of the audio signal to the image pixels created
 * by the PixelMapGen <code>multigen</code> and managed by the PixelAudioMapper <code>mapper</code>. 
 * This particular PixelMapGen reads time from top to bottom, left to right, in eight rows.</li> 
 * <li>Click on the image or press the spacebar with the cursor over the image to play 
 * a Sampler instrument sound.</i> 
 * <li>Press 'd' to enable drawing. Drag the mouse to draw a line. Try varying the speed of your gesture:
 * it will be recorded to the PACurveMaker instance that records your actions. When you release the
 * mouse, a brushstroke appears.</li> 
 * <li>Click on the brushstroke. The Sampler instrument plays new brushstokes, by default, using the 
 * ALL_POINTS representation of the curve. Press the '1', '2' or '3' key to change the representation
 * of the curve. REDUCED_POINTS (2) draws fewer points. CURVE_POINTS (3) draws many more. The Sampler
 * instrument sounds good with all points or reduced points, mostly because it has a fairly long 
 * envelope. It may sound too dense with curve points.</li>  
 * <li>Draw a few more brushstrokes to experiment with drawing and the Sampler instrument. 
 * Draw fast and slow, right to left or left to right (forwards or backwards in time), 
 * vertically, horizontally, or diagonally.</li> 
 * <li>To switch a brushstroke to use the Granular instrument, hover over it and press 't'. 
 * The representation of the curve will change to CURVE_POINTS. Click on the curve to
 * play it. Experiment with the other representations. The curve steps representation
 * of the brushstroke provides enough density to provide a continuous sound for the short
 * envelopes of grains. The reduced points representation may sound too sparse.
 * Press 't' again to switch the curve back to use the Sampler instrument. </li> 
 * </ol>
 * See the various key commands for various ways to alter the sound of the granular synth
 * and other features. Check out the JavaDocs comments code comments on the various methods 
 * for detailed information about the features of TutorialOne_03_Drawing. For an GUI with 
 * greater control over drawing, gesture, and audio synthesis, see the GesturePlayground sketch. 
 * </p>
 * <h2>NEW FEATURES</h2>
 * <p>
 * This sketch continues the Tutorial One sequence for the PixelAudio library
 * for Processing. To the previous tools for reading, writing, and transcoding
 * audio and image files, triggering audio events, and animating pixels to change
 * audio sample order, it adds the capability of drawing in the display window. Lines 
 * drawn on the display are captured as points and times and used to create brushstrokes
 * that can be activated to trigger audio events. In PixelAudio's conceptual framework, 
 * the combination of points and times constitutes a "gesture". Along with mapping of 
 * points to audio samples using PixelAudioMapper, "gesture" is a core concept of the
 * PixelAudio library. Check out {@link net.paulhertz.pixelaudio.curves.PAGesture PAGesture} 
 * for a formal definition of gesture.
 * </p><p>
 * PixelAudio provides two types of audio synthesis: the Sampler intruments, introduced in
 * previous sketches, and the Granular instruments. This sketch introduces PixelAudio's granular 
 * synthesis engine with the <code>PAGranularInstrumentDirector</code> class and provides
 * another class for Sampler instruments, <code>PASamplerInstrumentPool</code>. The Granular
 * instruments and the Sampler instruments both depend on a hierarchy of classes that 
 * implement a signal-processing chain. It's unlikely that you will have to deal with the
 * low-level processing. PAGranularInstrumentDirector and PASamplerInstrumentPool provide
 * the control knobs you would want for a virtual electronic instrument while keeping the
 * math and audio engine hooks in the background. This sketch and GesturePlayground provide
 * a good introduction to the functions available in the granular synth. GesturePlayground
 * goes into more detail than TutorialOne_03_Drawing, and provides a GUI to tweak almost
 * every Granular instrument feature. In TutorialOne_03_Drawing we are principally
 * concerned with showing how to create interactive brushstrokes by drawing on the screen.
 * </p>
 * <div>
 * <h2>Points + Times = Gestures</h2>
 * <p>
  * The drawing tools and commands are a substantial addition. To implement them
 * we call on a whole new package of code, {@link net.paulhert.pixelaudio.curves
 * PixelAudio Curves Package}. To turn gestures into timing information that can
 * be used to schedule audio events, particularly with granular synthesis, we
 * rely on the {@link net.paulhertz.pixelaudio.schedule PixelAudio Schedule Package}. 
 * The workhorse of the Curves package is {@link net.paulhert.pixelaudio.curves.PACurveMaker PACurveMaker}, 
 * which is used to  * capture point and time information when you draw. A PACurveMaker 
 * instance is intialized with a list of unique points and a list of time offsets where 
 * both lists have the same number of elements. Each point is paired to the relative
 * time when it was recorded. Points drawn on the display stored in
 * PACurveMaker's ALL_POINTS representation of the gesture points. PACurveMaker
 * can also reduce the number of points captured, using the
 * Ramer-Douglas-Peucker (RDP) algorithm, to create the REDUCED_POINTS
 * representation of a gesture. RDP controls point reduction with a numerical
 * value, <code>epsilon</code>, which you can vary to control the number of
 * reduced points in a gesture. PACurveMaker can turn the reduced points
 * representation of a drawn line into a Bezier curve, the CURVE_POINTS
 * representation of the gesture. The curve can be divided polygonal segments,
 * with the potential to generate an audio event at each vertex. The number of
 * divisions is controlled by the <code>PACurveMaker.setCurveSteps(int
 * curveSteps)</code> method. In the GesturePlayground sketch you can vary the
 * curve divisions with the GUI.
 * </p><p>
 * The CURVE_POINTS curve is used to create a stylized brushstroke. The
 * brushstroke is an <code>PABezShape</code> object. PABezShape provides a
 * <code>pointInPoly()</code> method that you can use to detect the mouse
 * hovering over or clicking within a brushstroke. TutorialOne_03_Drawing shows
 * how the brushstroke can be activated as an animated UI element and used to
 * trigger audio events. 
 * </p> 
 * <h2>Audio Processing</h2>
 * <p>
 * The <code>PAGranularInstrumentDirector</code> class manages the high level processes for 
 * granular synthesis. Probably all the functionality you will commonly need 
 * is available in its methods. The various <code>playGestureNow(...)</code> methods 
 * allow you to control the timing, panning, pitch, and gain of individual grains, 
 * if you want to. Parameters for grain shaping and are set with the 
 * GestureGranularParams class. The GestureEventParams class supports arrays of 
 * values to set timing, pan, gain, and pitch for individual grains. The timing and 
 * pan settings for individual grains can also be passed as arrays to overloaded
 * <code>playGestureNow(...)</code> methods. 
 * </p><p>
 * Here's an outline of the granular synbthesis chain, which you can feel free to skip:
 * <code>PAGranularInstrumentDirector</code> sets up the <code>PABurstGranularSource</code> 
 * and passes it to the granular synth processing chain, first to <code>PAGranularInstrument</code> 
 * and then to <code>PAGranularSampler</code>, where the <code>PABurstGranularSource</code> is 
 * one of various parameters used to create an <code>AudioScheduler</code> scheduler. In
 * <code>PAGranularSampler.uGenerate()</code>, <code>scheduler.processBlock()</code> is called, 
 * the audio signal <code>leftMix</code> and <code>rightMix</code> variables are initialized 
 * to 0 and used to accumulate the currently active <code>PAGranularVoice</code> instances. 
 * After that, uGenerate applies power normalization and soft clipping to the signal and returns 
 * its value. All of this is set in motion through Minim's UGen interface, 
 * which PAGranularSampler extends.
 * </p>
 * 
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
public class TutorialOne_04_Network extends PApplet implements PANetworkClientINF {
	
	
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
	float sampleRate = 44100;       // target audio engine rate used to configure audioOut
	float fileSampleRate;           // sample rate of most recently opened file (before resampling)
	float bufferSampleRate;         // sample rate of playBuffer, usually == audioOut.sampleRate()
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
    
	// network communications
	NetworkDelegate nd;
	boolean isUseNetworkDelegate = false;
	

    // ---------------- APPLICATION ---------------- //
	
	public static void main(String[] args) {
		PApplet.main(new String[] { TutorialOne_04_Network.class.getName() });
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
		preloadFiles(daPath, "Saucer_mixdown.wav");    // handy when debugging, too
	    // *****>>> NETWORKING <<<***** //
		isUseNetworkDelegate = true;
		if (isUseNetworkDelegate) {
			String remoteAddress = "127.0.0.1";
			nd = new NetworkDelegate(this, remoteAddress, remoteAddress, 7401, 7400);
			nd.oscSendClear();
		}
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
	 * BaseImage is intended as a reference image that usually only changes when you open a new image file.
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
	    brushes = new ArrayList<>();           // keep track of brushstrokes/gestures
	    hoverBrush = null;                     
	    hoverIndex = -1;
	    activeBrush = null;
	    samplerTimeLocs = new ArrayList<>();   // capture timing data when drawing
	}
	
	/**
	 * Preload an audio file using a file path and a filename.
	 * @param path        the fully qualified path to the file's directory, ending with a '/' 
	 * @param filename    the name of the file
	 */
	public void preloadFiles(String path, String filename) {
		// the audio file we want to open on startup
		File audioSource = new File(path + filename);
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
	 * mapImage pixels, adjusting the index position of the copy using totalShift --
	 * i.e. we don't actually rotate the pixels, we just shift the position they're copied to.
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
	
	/**
	 * @return a reference to the brushstroke the mouse is over, or null if there's no brushstroke. 
	 */
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

	/**
	 * Update the hoverBrush and hoverIndex global variables.
	 */
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
	 * The built-in mousePressed handler for Processing, clicks are handled in mouseClicked().
	 */
	public void mousePressed() {
	    if (isDrawMode) {
	        initAllPoints();    // start capturing points and times for a brushstroke
	    }
	}
	
	public void mouseDragged() {
		// we don't do anything here, drawing will be handled by the draw() loop
	}
	
	public void mouseReleased() {
	    if (isDrawMode && allPoints != null) {
	        if (allPoints.size() > 2) {
	            initCurveMakerAndAddBrush();    // finalize curve drawing into a new PACurveMaker instance, add a brush
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
			} 
			else {
				scheduleGranularBrushClick(activeBrush);   // Granular brush clicked event
			}
			return;
		}
		// click outside any brush is handled here
		audioMousePressed(mouseX, mouseY);
	}

    /**
     * Built-in keyPressed handler, forwards events to parseKey.
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
		case 'k': // apply the hue and saturation in the colors array to mapImage (not to baseImage)
			refreshMapImageFromBase();
			mapImage.loadPixels();
			applyColorShifted(colors, mapImage.pixels, mapper.getImageToSignalLUT(), totalShift);
			mapImage.updatePixels();
			break;
		case 'K': // apply hue and saturation in colors to baseImage and mapImage
			baseImage.loadPixels();
			applyColor(colors, baseImage.pixels, mapper.getImageToSignalLUT());
			baseImage.updatePixels();
			refreshMapImageFromBase();
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
		println(" * Press UP ARROW to increase audio gain by 3 dB.");
		println(" * Press DOWN ARROW to decrease audio gain by 3 dB.");
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

	/**
	 * Utility method for applying hue and saturation values from a source array of RGB values
	 * to the brightness values in a target array of RGB values, using a lookup table to redirect indexing,
	 * taking into account any pixels that were shifted.
	 * 
	 * @param colorSource    a source array of RGB data from which to obtain hue and saturation values
	 * @param graySource     an target array of RGB data from which to obtain brightness values
	 * @param lut            a lookup table, must be the same size as colorSource and graySource
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
				// *****>>> NETWORKING <<<***** //
			    if (nd != null) nd.oscSendFileInfo(filePath, fileName, fileTag);
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
			if (fileSampleRate != audioOut.sampleRate()) {
				resampled = AudioUtility.resampleMonoToOutput(buff.getChannel(0), fileSampleRate, audioOut);
				buff.setBufferSize(resampled.length);
				buff.setChannel(0, resampled);
				//if (buff.getBufferSize() != mapSize) buff.setBufferSize(mapSize);
				fileSampleRate = audioOut.sampleRate();
			}
			else {
				bufferSampleRate = fileSampleRate;
			}
			// save the length of the file, possibly resampled, for future use
			this.audioFileLength = buff.getBufferSize();
			println("---- file sample rate = "+ this.fileSampleRate 
					+", buffer sample rate = "+ bufferSampleRate
					+", audio output sample rate = "+ audioOut.sampleRate());
		}
		else {
			println("-- Unable to load file. File may be empty, wrong format, or damaged.");
			return;
		}
		// everything looks good, proceed
		if (isBlending) {
			blendInto(playBuffer, buff, 0.5f, false, -12.0f);    // mix audio sources without normalization
		}
		else {
			// adjust buffer size to mapper.getSize()
			if (buff.getBufferSize() != mapper.getSize()) buff.setBufferSize(mapper.getSize());
			playBuffer = buff;
		}
		// ensureSamplerReady will load playBuffer to the Sampler synth "pool"
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
		
	/**
	 * Normalizes a single-channel signal array to a target RMS level in dBFS (decibels relative to full scale).
	 * 0 is the maximum digital amplitude. -6.0 dB is 50% of the maximum level. 
	 *  
	 * @param signal
	 * @param targetPeakDB
	 */
	public static void normalize(float[] signal, float targetPeakDB) {
		AudioUtility.normalizeRmsWithCeiling(signal, targetPeakDB, -3.0f);
	}
		
	/**
	 * Transcodes audio data in audioSignal and writes it to color channel chan of mapImage.
	 * 
	 * @param chan     A color channel
	 * @param shift    number of index positions to shift the audio signal
	 */
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

	/**
	 * Sets the alpha channel of an RGBA color, conditionally setting alpha = 0 if all other channels = 0.
	 * 
	 * @param argb     an RGBA color value
	 * @param alpha    the desired alpha value to apply to argb
	 * @return         the argb color with changed alpha channel value
	 */
	public int setAlphaWithBlack(int argb, int alpha) {
		int[] c = PixelAudioMapper.rgbaComponents(argb);
		if (c[0] == c[1] && c[1] == c[2] && c[2] == 0) {
			alpha = 0;
		}
		return alpha << 24 | c[0] << 16 | c[1] << 8 | c[2];
	}
	
	/**
	 * Sets the alpha channel of an RGBA color.
	 * 
	 * @param argb     an RGBA color value
	 * @param alpha    the desired alpha value to apply to argb
	 * @return         the argb color with changed alpha channel value
	 */
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
	
	/**
	 * Writes a specified channel of mapImage to audioSignal.
	 * 
	 * @param chan    the selected color channel
	 */
	public void renderMapImageToAudio(PixelAudioMapper.ChannelNames chan) {
		writeImageToAudio(mapImage, mapper, audioSignal, chan, totalShift);
	}
	
	/**
	 * Writes the mapImage, which may change with animation, to the baseImage, a reference image
	 * that usually only changes when a new file is loaded.
	 */
	public void commitMapImageToBaseImage() {
		baseImage = mapImage.copy();
		totalShift = 0;
	}
	
	/**
	 * Copies the supplied PImage to mapImage and baseImage, sets totalShift to 0 (the images are identical).
	 * @param img
	 */
	public void commitNewBaseImage(PImage img) {
		baseImage = img.copy();
		mapImage = img.copy();
		totalShift = 0;
	}
	
	/**
	 * Writes baseImage to mapImage with an index position offset of totalShift.
	 */
	public void refreshMapImageFromBase() {
	    mapImage.loadPixels();
	    mapper.copyPixelsAlongPathShifted(baseImage.pixels, mapImage.pixels, totalShift);
	    mapImage.updatePixels();
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

	/**
	 * @param selection    a File to write as audio
	 */
	public void audioFileSelectedWrite(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
			return;
		}
		String fileName = selection.getAbsolutePath();
		if (!selection.getName().endsWith(".wav")) {
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
	 * This same method can be called as a static method in AudioUtility.
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
	
	/**
	 * Initializes global variables gParamsGesture and gParamsFixed, which provide basic
	 * settings for granular synthesis the follows gesture timing or fixed hop timing 
	 * between grains. 
	 */
	public void initGranularParams() {
		ADSRParams env = this.calculateEnvelopeLinear(granularGain, 1000);
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
		
	/**
	 * Handles mouse clicks that happen outside a brushstroke.
	 * 
	 * @param x    x-coordinate of mouse click
	 * @param y    y-coordinate of mouse click
	 */
	public void audioMousePressed(int x, int y) {
		samplePos = handleClickOutsideBrush(x, y);
		// *****>>> NETWORKING <<<***** //
	    if (nd != null) nd.oscSendMousePressed(x, y, samplePos);
	}

	/**
	 * @param x
	 * @param y
	 */
	public int handleClickOutsideBrush(int x, int y) {
		samplePos = mapper.lookupSignalPosShifted(x, y, totalShift);
	    if (!useGranularSynth) {
	    	// use Sampler synthesis instrument
	    	ensureSamplerReady();
	        float panning = map(x, 0, width, -0.8f, 0.8f);
	        int len = calcSampleLen();
	        samplelen = playSample(samplePos, len, synthPointGain, defaultEnv, panning);
	        int durationMS = (int)(samplelen / sampleRate * 1000);
	        pointTimeLocs.add(new TimedLocation(x, y, durationMS + millis() + 50));
	        return samplePos;
	    }
    	// use Granular synthesis instrument
	    ensureGranularReady();
	    float hopMsF = (int)Math.round(AudioUtility.samplesToMillis(hopSamples, sampleRate));
	    final int grainCount = useLongBursts ?  8 * (int) Math.round(noteDuration/hopMsF) : (int) Math.round(noteDuration/hopMsF);
	    println("-- granular point burst with grainCount = "+ grainCount);
	    ArrayList<PVector> path = new ArrayList<>(grainCount);
	    int[] timing = new int[grainCount];
	    for (int i = 0; i < grainCount; i++) {
	    	if (useLongBursts) {
	    		path.add(getCoordFromSignalPos(samplePos + hopSamples * i));
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
	    return samplePos;
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
	 * Calculates the display image coordinates corresponding to a specified audio sample index.
	 * @param pos    an index into an audio signal, must be between 0 and width * height - 1.
	 * @return       a PVector with the x and y coordinates
	 */
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

	/**
	 * Initializes a new PAGranularSynth instance that you probably would pass to a PAGranularInstrumentDirector.
	 * 
	 * @param out          and AudioOutput, most likely the one used by this sketch
	 * @param env          an ADSRParams envelope
	 * @param numVoices    the number of voices to use for synthesizing simultaneous grains
	 * @return             a PAGranularSynth instance
	 */
	public PAGranularInstrument buildGranSynth(AudioOutput out, ADSRParams env, int numVoices) {
	    return new PAGranularInstrument(out, env, numVoices);
	}
	
	/**
	 * Ensures that all resources and variable necessary for the Sampler synth are ready to go.
	 */
	void ensureSamplerReady() {
	    if (pool != null) pool.setBuffer(playBuffer);
	    else pool = new PASamplerInstrumentPool(playBuffer, sampleRate, 1, samplerMaxVoices, audioOut, defaultEnv);
	}
	
	/**
	 * Ensures that all resources and variable necessary for the Granular synth are ready to go.
	 */
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
	
	/**
	 * Updates resources such as playBuffer and pool with a new signal, typcically when a new file is loaded.
	 * 
	 * @param sig    an audio signal as an array of float
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
	
	/**
	 * Calls PAGranularInstrumentDirector gDir to play a granular audio event.
	 * 
	 * @param buf       an audio signal as an array of flaot
	 * @param sched     an GestureSchedule with coordinate and timing information 
	 * @param params    a bundle of control parameters for granular synthesis
	 */
	public void playGranularGesture(float buf[], GestureSchedule sched, GestureGranularParams params) {
		// call mapper method lookupSignalPosArray to obtain an array of indices into buf, derived from points in sched
		int[] startIndices = mapper.lookupSignalPosArray(sched.points, totalShift, mapper.getSize());
		// println("---> startIndices[0] = "+ startIndices[0] +" for "+ sched.points.get(0).x, sched.points.get(0).y, totalShift, mapper.getSize());
		// calculate the pan for each grain, based on its x-coordinate
		float[] panPerGrain = new float[sched.size()];
		for (int i = 0; i < sched.size(); i++) {
		    PVector p = sched.points.get(i);
		    // example: map x to [-0.8, +0.8]
		    panPerGrain[i] = map(p.x, 0, width-1, -0.875f, 0.875f);
		}
		// debugging
		//println("\n----->>> playGranularGesture()");
		//debugIndexHeadroom(buf, startIndices, tx);
		//debugTimesMs(sched);
		//println("\n");\// end debugging
		// if usePitchedGrains is true, apply a jittery pitch shift to 
		// each grain, then call gDir.playGestureNow(), and return
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

	/**
	 * Calculate an envelope of length totalSamples. 
	 * @param gainDb          desired gain in dB, currently ignored
	 * @param totalSamples    number of samples the envelope should cover
	 * @param sampleRate      sample rate of the audio buffer the envelope is applied to
	 * @return and ADSRParams envelope
	 */
	public ADSRParams calculateEnvelopeDb(float gainDb, int totalSamples, float sampleRate) {
		float linear = AudioUtility.dbToLinear(gainDb);
		return calculateEnvelopeLinear(linear, totalSamples * 1000f / sampleRate);
	}

	/**
	 * Calculate an envelope of length totalSamples. 
	 * @param gainDb     desired gain in dB, currently ignored
	 * @param totalMs    desired duration of the envelope in milliseconds
	 * @return an ADSRParams envelope
	 */
	public ADSRParams calculateEnvelopeLinear(float gainDb, float totalMs) {
	    float attackMS = Math.min(50, totalMs * 0.1f);
	    float releaseMS = Math.min(200, totalMs * 0.3f);
	    float envGain = AudioUtility.dbToLinear(gainDb);
	    envGain = 1.0f;
	    return new ADSRParams(envGain, attackMS / 1000f, 0.01f, 0.8f, releaseMS / 1000f);
	}
	
	// DEBUGGIONG
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

	// DEBUGGIONG
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
	    int x = clipToWidth(mouseX);
	    int y = clipToHeight(mouseY);
	    addPoint(x, y);
		// *****>>> NETWORKING <<<***** //
		if (nd != null) nd.oscSendMousePressed(x, y, samplePos);
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
		
	/**
	 * Clips parameter i to the interval (0..width-1)
	 * @param i
	 * @return
	 */
	public int clipToWidth(int i) {
		return min(max(0, i), width - 1);
	}
	/**
	 * Clips parameter i to the interval (0..width-1)
	 * @param i
	 * @return
	 */
	public int clipToHeight(int i) {
		return min(max(0, i), height - 1);
	}
	
	/**
	 * @param x              x-coordinate
	 * @param y              y-coordinate
	 * @param deviationPx    distance deviation from mean
	 * @return               a PVector with coordinates shifted by a Gaussing variable
	 */
	public PVector jitterCoord(int x, int y, int deviationPx) {
	    double variance = deviationPx * deviationPx;
	    int jx = (int)Math.round(PixelAudio.gauss(0, variance));
	    int jy = (int)Math.round(PixelAudio.gauss(0, variance));
	    int nx = clipToWidth(x + jx);
	    int ny = clipToHeight(y + jy);
	    return new PVector(nx, ny);
	}
	
	
	/**
	 * Generates an array of Gaussian values for shifting pitch, where 1.0 = no shift.
	 * @param length            length of the returned array
	 * @param deviationPitch    expected average deviation of the pitch 
	 * @return                  and array of Gaussian values centered on 1.0
	 */
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
		// *****>>> NETWORKING <<<***** //
		if (nd != null) {
			nd.oscSendMousePressed(clipToWidth(mouseX), clipToHeight(mouseY), samplePos);
			nd.oscSendDrawPoints(curveMaker.getRdpPoints());
			nd.oscSendTimeStamp(curveMaker.timeStamp, curveMaker.timeOffset);
		}
	}

	/**
	 * Determines the path mode for a particular BrushOutput.
	 * @param out    a BrushOutput (SAMPLER of GRANULAR)
	 * @return       a PathMode (ALL_POINTS for Sampler instruments, CURVE_POINTS for Granular instruments)
	 */
	PathMode defaultPathModeFor(BrushOutput out) {
	    return (out == BrushOutput.SAMPLER) ? PathMode.ALL_POINTS : PathMode.CURVE_POINTS;
	}

	/**
	 * Enrty point for drawing brushstrokes on the screen.
	 */
	public void drawBrushShapes() {
		if (brushes == null || brushes.isEmpty()) return;
		drawBrushes(brushes, readyBrushColor1, hoverBrushColor1, selectedBrushColor1);
	}

	/**
	 * Draw brushstrokes on the display image.
	 * 
	 * @param list             a list of all the brushstrokes (AudioBrushLite)
	 * @param readyColor       color for a selectable brush
	 * @param hoverColor       color for a brush when the mouse hovers over it
	 * @param selectedColor    color for a selected brush (click or spacebar selects)
	 */
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
	
	/**
	 * Sets epsilon value for the PACurveMaker associated with an AudioBrushLite instance.
	 * 
	 * @param b    an AudioBrushLite instance
	 * @param e    desired epsilon value to control point reduction
	 */
	public void setBrushEpsilon(AudioBrushLite b, float e) {
		PACurveMaker cm = b.curve();
		BrushConfig cfg = b.cfg();
		cfg.rdpEpsilon = e;
		cm.setEpsilon(e);
		cm.calculateDerivedPoints();
	}

	/**
	 * Get the path points of a brushstroke, with the representation determined by the BrushConfig's path mode.
	 * 
	 * @param b    an AudioBrushLite instance
	 * @return     an all points, reduced, or curve representation of the path points of an AudioBrushLite instance
	 */
	ArrayList<PVector> getPathPoints(AudioBrushLite b) {
		PACurveMaker cm = b.curve();
		return switch (b.cfg().pathMode) {
		case ALL_POINTS -> cm.getAllPoints();
		case REDUCED_POINTS -> cm.getReducedPoints();
		case CURVE_POINTS -> cm.getCurvePoints();
		};
	}

	/**
	 * Get a GestureSchedule (points + timing) for an AudioBrushLite instance.
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
	
	/**
	 * Schedule a Sampler brush audio / animation event.
	 * 
	 * @param b    an AudioBrushLite instance
	 */
	void scheduleSamplerBrushClick(AudioBrushLite b) {
		if (b == null) return;
		ArrayList<PVector> pts = getPathPoints(b);
		if (pts == null || pts.size() < 2) return;
		GestureSchedule sched = getScheduleForBrush(b);
		storeSamplerCurveTL(sched, millis() + 10);
		// *****>>> NETWORKING <<<***** //
		if (nd != null) nd.oscSendTrig(getBrushIndex(b));
	}

	/**
	 * Store scheduled sampler synth / animation events for future activation. 
	 * @param sched        a GestureSchedule (points + timing for a brush)
	 * @param startTime    time to start a series of events
	 */
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

	/**
	 * Execute audio / animation events for Sampler brushstrokes.
	 */
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

	/**
	 * Schedule a Granular brush audio / animation event.
	 * 
	 * @param b    an AudioBrushLite instance
	 */
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

	/**
	 * Store scheduled granular synth / animation events for future activation. 
	 * @param sched        a GestureSchedule (points + timing for a brush)
	 * @param startTime    time to start a series of events
	 * @param isGesture    is the schedule for a GESTURE or FIXED granular event (ignored)
	 */
	public synchronized void storeGranularCurveTL(GestureSchedule sched, int startTime, boolean isGesture) {
		if (this.grainTimeLocs == null) grainTimeLocs = new ArrayList<>();
		int i = 0;
		// we store the point and the current time + time offset, where timesMs[0] == 0
		for (PVector loc : sched.points) {
			int x = Math.round(loc.x);
			int y = Math.round(loc.y);
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
	 * @param poly    a polygon described by an ArrayList of PVector
	 * @return        true if the mouse is within the bounds of the polygon, false otherwise
	 */
	public boolean mouseInPoly(ArrayList<PVector> poly) {
		return PABezShape.pointInPoly(poly, mouseX, mouseY);
	}

	int getBrushIndex(AudioBrushLite brush) {
	    if (brush == null) return -1;
	    return brushes.indexOf(brush);
	}	
	
	/**
	 * Reinitializes audio and clears event lists.   
	 * -- TODO drop, this used to be the "emergency off" switch for runaway audio processing
	 */
	@Deprecated
	public void reset() {
		// if rescued from the deprecated:
		if (nd != null) {
			nd.oscSendMousePressed(clipToWidth(mouseX), clipToHeight(mouseY), samplePos);
			nd.oscSendDrawPoints(curveMaker.getRdpPoints());
			nd.oscSendTimeStamp(curveMaker.timeStamp, curveMaker.timeOffset);
		}
		
	}


	/**
	 * Removes the current active AudioBrushLite instance.
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
	
	/**
	 * Removes the most recent AudioBrushLite instance.
	 */
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
	/*                        NETWORKING                              */
	/*                                                                */
	/*----------------------------------------------------------------*/
	
	
	// required by the PANetworkCLientINF interface
	public PApplet getPApplet() {
		return this;
	}

	// required by the PANetworkCLientINF interface
	public PixelAudioMapper getMapper() {
		return this.mapper;
	}
	
	// required by the PANetworkCLientINF interface
	public void controlMsg(String control, float val) {
		if (control.equals("detune")) {
		    println("--->> controlMsg is \"detune\" = "+ val +"; detune is not implemented.");
		}
	}

	// required by the PANetworkCLientINF interface
	public int playSample(int samplePos) {
		int[] coords = mapper.lookupImageCoord(samplePos);
		int x = coords[0];
		int y = coords[1];
		return handleClickOutsideBrush(x, y);
	}

	// required by the PANetworkCLientINF interface
	public void playPoints(ArrayList<PVector> pts) {
		float[] times = new float[pts.size()];
		int interval = noteDuration / 4;
		for (int i = 0; i < pts.size(); i++) {
			times[i] = i * interval;
		}
		GestureSchedule sched = new GestureSchedule(pts, times);
		storeSamplerCurveTL(sched, millis() + 10);
	}
	
	
	
	
	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                       NETWORK DELEGATE                         */
	/*                                                                */
	/*----------------------------------------------------------------*/

	
	/**
	 * A class to handle network connections over UDP, for example, with a Max or Pd patch.
	 * Used by applications that implement the PANetworkClientINF.
	 * 
	 * @see PANetworkClientINF
	 */
	public class NetworkDelegate {
		private PApplet parent;
		private PANetworkClientINF app;
		private OscP5 osc;
		private int inPort = 7401;
		private int outPort = 7400;
		private String remoteFromAddress = "127.0.0.1";
		private String remoteToAddress = "127.0.0.1";
		// 192.168.1.77
		private NetAddress remoteFrom;
		private NetAddress remoteTo;
		private int drawCount = 0;
		

		public NetworkDelegate(PANetworkClientINF app, String remoteFromAddr, String remoteToAddr, int inPort, int outPort) {
			this.app = app;
			this.parent = app.getPApplet();
			this.osc = new OscP5(parent, inPort);
			this.remoteFromAddress = remoteFromAddr;
			this.remoteToAddress = remoteToAddr;
			this.inPort = inPort;
			this.outPort = outPort;
			init();
		}

		public NetworkDelegate(PANetworkClientINF app, String remoteFromAddr, String remoteToAddr) {
			this.app = app;
			this.parent = app.getPApplet();
			this.osc = new OscP5(parent, inPort);
			this.remoteFromAddress = remoteFromAddr;
			this.remoteToAddress = remoteToAddr;
			init();
		}
		
		public NetworkDelegate(PANetworkClientINF app) {
			this.app = app;
			this.parent = app.getPApplet();
			this.osc = new OscP5(parent, inPort);
			init();
		}
			
		public void init() {
			this.remoteFrom = new NetAddress(this.remoteFromAddress, this.inPort);
			this.remoteTo = new NetAddress(this.remoteToAddress, this.outPort);
			System.out.println("== remoteFromAddress "+ remoteFromAddress +", in port: "+ inPort);
			System.out.println("== remoteToAddress "+ remoteToAddress +", out port: "+ outPort);
			initOscPlugs();		
		}
		
		/*----------------------------------------------------------------*/
		/*                                                                */
		/*                      GETTERS AND SETTERS                       */
		/*                                                                */
		/*----------------------------------------------------------------*/
		
		public int getInPort() {
			return inPort;
		}

		public void setInPort(int inPort) {
			this.inPort = inPort;
		}

		public int getOutPort() {
			return outPort;
		}

		public void setOutPort(int outPort) {
			this.outPort = outPort;
		}

		public NetAddress getRemoteFrom() {
			return remoteFrom;
		}

		public void setRemoteFrom(NetAddress remoteFrom) {
			this.remoteFrom = remoteFrom;
		}

		public NetAddress getRemoteTo() {
			return remoteTo;
		}

		public void setRemoteTo(NetAddress remoteTo) {
			this.remoteTo = remoteTo;
		}

		public int getDrawCount() {
			return drawCount;
		}

		public void setDrawCount(int drawCount) {
			this.drawCount = drawCount;
		}

		/*----------------------------------------------------------------*/
		/*                                                                */
		/*                  OscP5 PLUG & MESSAGE SETUP                    */
		/*                                                                */
		/*----------------------------------------------------------------*/	
		
		/**
		 * SET UP RESPONSE TO INCOMING MESSAGES
		 * Call the osc.plug(Object theObject, String the MethodName, String theAddrPattern)
		 * or osc.plug(Object theObject, String the MethodName, String theAddrPattern, String the TypeTag)
		 * to setup callbacks to methods in the current object ("this") or other object instance. 
		 */
		public void initOscPlugs() {
			osc.plug(this, "sampleHit", "/sampleHit");
			osc.plug(this, "drawHit", "/draw");
			osc.plug(this, "multislider", "/multislider");
			osc.plug(this, "parseKey", "/parseKey");
			osc.plug(this, "controlMsg", "/controlMsg");
		}


		/* incoming osc message are forwarded to the oscEvent method. */
		void oscEvent(OscMessage theOscMessage) {
		  /* print the address pattern and the typetag of the received OscMessage */
		  PApplet.print("### received an osc message.");
		  PApplet.print(" addrpattern: "+theOscMessage.addrPattern());
		  PApplet.println(" typetag: "+theOscMessage.typetag());
		}

		
		/*----------------------------------------------------------------*/
		/*                                                                */
		/*                   OUTGOING MESSAGE METHODS                     */
		/*                                                                */
		/*----------------------------------------------------------------*/
		
		public void oscSendMousePressed(int sampleX, int sampleY, int sample) {
		  OscMessage msg = new OscMessage("/press");
		  msg.add(sample);
		  msg.add(sampleX);
		  msg.add(sampleY);
		  osc.send(msg, this.remoteTo);
		  // PApplet.println("---> msg: "+ msg);
		}

		public void oscSendMultiSlider(ArrayList<PVector> drawPoints) {
		  int y0 = (int) drawPoints.get(0).y;
		  int sliders = 12;
		  int[] multi = new int[sliders];
		  int i = 0;
		  for (PVector vec : drawPoints) {
		    if (i < sliders) {
		      multi[i++] = ((int) PApplet.abs(y0 - vec.y)) % 128;
		    }
		  }
		  OscMessage msg = new OscMessage("/multislider");
		  for (i = 0; i < multi.length; i++) {
		    msg.add(multi[i]);
		  }
		  osc.send(msg, this.remoteTo);
		}  
		
		public void oscSendDrawPoints(ArrayList<PVector> drawPoints) {
			OscMessage msg = new OscMessage("/draw");
			int i = 0;
			msg.add(++this.drawCount);
			for (PVector vec : drawPoints) {
				msg.add(i++);
				int x = (int) vec.x;
				int y = (int) vec.y;
				msg.add(this.app.getMapper().lookupSignalPos(x, y));
				msg.add(x);
				msg.add(y);
			}
			osc.send(msg, this.remoteTo);
		}
		
		public void oscSendTimeStamp(int timeStamp, int timeOffset) {
			OscMessage msg = new OscMessage("/time");
			msg.add(drawCount);
			msg.add(timeStamp);
			msg.add(timeOffset);
			osc.send(msg, this.remoteTo);
		}
		  	
		public void oscSendTrig(int index) {
			OscMessage msg = new OscMessage("/trig");
			msg.add(index);
			osc.send(msg, this.remoteTo);
		}
		
		public void oscSendDelete(int index) {
			OscMessage msg = new OscMessage("/del");
			msg.add(index);
			osc.send(msg, this.remoteTo);
		}
		
		public void oscSendClear() {
			OscMessage msg = new OscMessage("/clear");
			osc.send(msg, this.remoteTo);
		}
		
		public void oscSendFileInfo(String path, String name, String tag) {
			OscMessage msg = new OscMessage("/file");
			msg.add(path);
			msg.add(name);
			msg.add(tag);
			osc.send(msg, this.remoteTo);
		}
		
		/*----------------------------------------------------------------*/
		/*                                                                */
		/*                      OscP5 PLUG METHODS                        */
		/*                                                                */
		/*----------------------------------------------------------------*/
		
		
		/* 
		 * OscP5 plug-in methods, which call methods on the client,
		 * are implemented in this section. They take calls from 
		 * the service (UDP, here) and pass them on to the client.
		 * If you want to extend the available calls, modify the 
		 * PANetworkClientINF interface to make the contracts
		 * between client and delegate explicit. 
		 */
		
		public void sampleHit(int sam) {
			int[] xy = app.getMapper().lookupImageCoord(sam);
			PApplet.println("---> sampleHit " + xy[0], xy[1]);
			app.playSample(sam);
		}

		public void drawHit(int... args) {
			ArrayList<PVector> pts = new ArrayList<PVector>();
			PApplet.println("---> drawHit "+ args.length);
			for (int pos : args) {
				int[] xy = app.getMapper().lookupImageCoord(pos);
				pts.add(new PVector(xy[0], xy[1]));
				// PApplet.println("  "+ xy[0] +", "+ xy[1]);
			}
			app.playPoints(pts);
		}

		public void multislider(int... args) {
			PApplet.print("---> multislider: ");
			for (int pos : args) {
				PApplet.print(pos + ", ");
			}
			PApplet.println();
		}
		
		public void parseKey(int arg) {
			char ch = PApplet.parseChar(arg);
			PApplet.println("---> parseKey: "+ ch);
			app.parseKey(ch, 0);
		}
		
		public void controlMsg(String ctrl, float val) {
			PApplet.println("---> control: "+ ctrl +", value: "+ val);		
		}

	}
	
	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                           UTILITY                              */
	/*                                                                */
	/*----------------------------------------------------------------*/
	

}


