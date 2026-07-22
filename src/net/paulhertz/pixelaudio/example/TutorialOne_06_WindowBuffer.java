package net.paulhertz.pixelaudio.example;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import net.paulhertz.pixelaudio.granular.*;
import net.paulhertz.pixelaudio.sampler.*;
//audio library
import ddf.minim.*;

//video export library
import com.hamoid.*;

// TODO optimize granular brush 'q' command (may not be necessary for a demo

/**
 * 
 * Changes the audio source model of TutorialOne_03_Drawing to support large files
 * with windowed buffering. Keeps the drawing, brush, sampler, granular, bounds, and envelope code.
 * <ul>
 *   <li>anthemSignal / anthemBuffer hold the entire loaded audio file,</li>
 *   <li>windowBuff is a moving window over anthemSignal,</li>
 *   <li>audioSignal / playBuffer / mapImage hold only the current visible window,</li>
 *   <li>Display coordinates map first to the visible window, then to the backing
 *     full-file source by adding windowBuff.getIndex().</li>
 * </ul>
 * <p>
 * The new features are fit on top of Tutorial_03_Drawing variables and methods.
 * Support for WindowedBuffer is flagged with "// *** WindowedBuffer support *** //".
 * Search for the flag to see how TutorialOne_03_Drawing was adapted. 
 * See Tutorial_03_Drawing for information about drawing, audio synthesis,
 * audio events and animation. Here we'll document just the new features.
 * </p><p>
 * See {@link net.paulhertz.pixelaudio.example.TutorialOne_03_Drawing TutorialOne_03_Drawing}
 * </p>
 * <h2>NEW FEATURES</h2>
 * <p>
 * This sketch adds a windowed audio buffer to the drawing, interaction and
 * audio synthesis tools of {@link TutorialOne_03_Drawing}. A windowed audio buffer 
 * combines an audio buffer with a method that positions a "window" within a larger
 * source or "backing" buffer and reads data from it to a separate data structure.
 * TutorialOne_06_WindowBuffer uses PixelAudio's {@link net.paulhertz.pixelaudio.WindowedBuffer} class to store a
 * backing buffer read from an audio file. It uses the backing buffer as the source
 * array for the smaller audio buffers {@code audioSignal} and {@code playBuffer}. We
 * can use the window function to select which portion of the backing buffer we want to
 * see and interact with. Data in audioSignal is transcoded as pixel values in {@code
 * mapImage}, an image used in this sketch to provide a visual representation of the
 * data in the audio buffer. A PixelAudioMapper instance, {@code mapper}, handles the
 * mapping of data between image and audio structures, and also maps mouse interactions
 * on the display image to sample indices in the audio buffer.
 * </p><p>
 * The Granular and Sampler instruments both use the backing audio buffer directly
 * as their audio source, though they can also use the smaller audio buffers derived
 * from the window function. The window that moves through the backing buffer is
 * exactly the same size as the display image and the smaller audio buffers. It can
 * be positioned anywhere within the backing buffer. It also can be automatically
 * moved across the backing buffer at a specified rate, animating the display image. 
 * </p><p>
 * Interaction with the sketch using the mouse and keyboard takes into account the
 * indexing of the windowed audio buffer. A mouse click on the display image will
 * trigger a sound from the sampler or granular instrument by requesting a sample
 * index from mapper and adding the current window index in the backing buffer to
 * it, thus obtaining the correct sample index in the backing buffer to forward to
 * the instrument. It is possible to animate mapImage by pixel-shifting, which
 * shifts the audio index with respect to the number of pixels shifted. Probably you
 * won't want to run pixel-shifting and automatic window advancing together, but is
 * is possible. As with the window index, the pixel shift index will be used to
 * determine the audio sample index.
 * </p>
 * <h2>QUICK START</h2>
 * 
 * <ol>
 *    <li>Run the sketch. It will load the audio file specified in daPath and daFile, and write 
 *    it to the display image as colors overlaid on a visualization of the audio values along
 *    the signal path. You can change the file with the 'o' key command.</li> 
 *    
 *    <li>The drawing and audio synthesis features are the same as in TutorialOne_03_Drawing, 
 *    but now mouse interactions will trigger audio events based on the current position of 
 *    the windowed buffer within the backing buffer. You can trigger audio events at different 
 *    locations in the audio file by moving the window and clicking on the display image.</li>
 *    
 *    <li>Press spacebar or click to trigger audio events at audio index corresponding to the
 *    mouse location. Draw brushstrokes for both Sampler and Granular events.</li> 
 *    
 *    <li>Press 'T' to toggle automatic window advancement. Press '{' or '}' to jump back/forward 
 *    one full window. Press '(' or ')' to jump back/forward one half window. Press 'R' to 
 *    rewind the WindowBuffer.</li>
 *    
 *    <li>Press 'Y' to toggle raindrop point events while windowing.</li>
 *    
 *    <li>Experiment with drawing brushstrokes and triggering audio events while moving through 
 *    the buffer.</li>
 * </ol>
 * <pre>
 * Here are the key commands for this sketch:
 * 
 * Press UP ARROW to increase audio gain by 3 dB.
 * Press DOWN ARROW to decrease audio gain by 3 dB.
 * Press RIGHT ARROW to increase Sampler audio gain by 3 dB.
 * Press LEFT ARROW to decrease Sampler audio gain by 3 dB.
 * Press SHIFT-RIGHT ARROW to increase Granular audio gain by 3 dB.
 * Press SHIFT-LEFT ARROW to decrease Granular audio gain by 3 dB.
 * Press ' ' (spacebar) to trigger a brush if we're hovering over a brush, otherwise trigger a point event.
 * Press '1' to set brushstroke under cursor to PathMode ALL_POINTS.
 * Press '2' to set brushstroke under cursor to PathMode REDUCED_POINTS.
 * Press '3' to set brushstroke under cursor to PathMode CURVE_POINTS.
 * Press 't' to set brushstroke under cursor to use Sampler or Granular synth.
 * Press 'f' to set brushstroke under cursor to FIXED hop mode for granular events.
 * Press 'g' to set brushstroke under cursor to GESTURE hop mode for granular events.
 * Press 'y' to select granular or sampler synth for click response.
 * Press 'r' to select long or short grain duration .
 * Press 'b' to toggle between long burst grains and short burst grains.
 * Press 'p' to jitter the pitch of granular gestures.
 * Press 'P' to adjust pitch of current brushstroke.
 * Press '>' or '.' to increment epsilon value of current brush (reduced points decrease).
 * Press '<' or ',' to decrement epsilon value of current brush (reduced points increase).
 * Press ']' to increment curve steps value of current brush.
 * Press '[' to decrement curve steps value of current brush.
 * Press 'm' to change the Sampler noise reduction profile.
 * Press 'e' to change the envelope we're using .
 * Press 'E' to toggle whether we adjust envelope duration in relation to gesture duration.
 * Press 'a' to  start or stop animation (bitmap rotation along the signal path).
 * Press 'd' to toggle play audio on new brush .
 * Press 'c' to apply color from image file to display image.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage (not to baseImage).
 * Press 'K' to apply hue and saturation in colors to baseImage and mapImage.
 * Press 'j' to turn audio and image blending on or off.
 * Press 'n' to normalize the audio buffer to -6.0dB.
 * Press 'L' or 'l' to determine whether to load a new file to both image and audio or not.
 * Press 'o' or 'O' to open an audio or image file and load to image or audio buffer or both.
 * Press 's' to save display image to a PNG file.
 * Press 'S' to save audio buffer to a .wav file.
 * Press 'w' to write the image HSB Brightness channel to the audio buffer as transcoded sample values .
 * Press 'W' to write the audio buffer samples to the image as color values.
 * Press 'x' to delete the current active brush shape or the oldest brush shape.
 * Press 'X' to delete the most recent brush shape.
 * Press 'u' to mute audio.
 * Press 'V' to record a video.
 * Press 'h' or 'H' to show help message in the console.
 *
 * WindowBuffer additions:
 * Press 'T' to toggle automatic WindowBuffer traversal.
 * Press '{' or '}' to jump back/forward one full window.
 * Press '(' or ')' to jump back/forward one half window.
 * Press 'R' to rewind the WindowBuffer.
 * Press 'Y' to toggle raindrop point events while windowing.
 * </pre>
 * 
 * 
 * 
 */
public class TutorialOne_06_WindowBuffer extends PApplet {
	
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
	int[] spectrum;              // array of spectral colors
	
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
	float outputGain = 0.0f;        // gain setting for audio output, decibels
	boolean isBufferStale = false;	// flags that audioBuffer needs to be reset, not used in TutorialOneDrawing
	float sampleRate = 48000;       // target audio engine rate used to configure audioOut
	float fileSampleRate;           // sample rate of most recently opened file (before resampling)
	float bufferSampleRate;         // sample rate of playBuffer: fileSampleRate when not resampled, audioOut.sampleRate() when resampled
	boolean doResample = true;      // if true, resample audio from files whose sampling rate != audioOut.sampleRate()
	float[] audioSignal;			// the audio signal as an array of floats
	MultiChannelBuffer playBuffer;	// a buffer for playing the audio signal
	int samplePos;                  // an index into the audio signal, selected by a mouse click on the display image
	int audioLength;				// length of the audioSignal, same as the number of pixels in the display image

	// ====== Sampler Synth ====== //

	// Sampler Instrument duration and envelopes
	final int noteDuration = 1000;  // average sample synth note duration, milliseconds, for reference
	int envDuration;                // current envelope duration
	boolean isAdjustEnvelope = true;    // if true, adjust envelope duration based on gesture duration
    String[] envPresets = {"Soft", "Percussion", "Pad", "default"};    // envelope presets
    int presetIndex = envPresets.length - 1;    // current preset index
    float overlapFactor = 3.0f;     // envelope overlap
    int envMinDurationMs = 40;        // min envelope duration, milliseconds
    int envMaxDurationMs = 1280;      // max envelope duration, milliseconds
    
    // Sampler Instrument setup
	int samplelen;                  // calculated sample synth note length, samples
	float samplerGain = AudioUtility.dbToLinear(-3.0f);    // linear gain for Sampler gesture event
	float samplerPointGain = 0.75f; // linear gain for point events with the Sampler instrument
	boolean isMuted = false;
	PASamplerInstrumentPool pool;   // an allocation pool of PASamplerInstruments
	int poolSize = 2;               // number of sampler instruments for polyphony
	int samplerMaxVoices = 64;      // number of voices for each sampler instrument

	// ADSR and its parameters
	float maxAmplitude = 1.0f;          // set envelopes to 1.0f amplitude, then scale later with audio instrument gain
	// ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
	ADSRParams defaultEnv = new ADSRParams(maxAmplitude, 0.2f, 0.1f, maxAmplitude * 0.75f, 0.5f);
	// ADSRParams granularEnv = new ADSRParams(maxAmplitude, 0.125f, 0.125f, 0, 0);
	float pitchScaling = 1.0f;          // factor for changing pitch
	float defaultPitchScaling = 1.0f;   // pitch scaling factor
	float lowPitchScaling = 0.5f;       // low pitch scaling, 0.5 => one octave down
	float highPitchScaling = 2.0f;      // high pitch scaling, 2.0 => one octave up
	
    // toggle for sampler events with a "granular" envelope
    boolean useGranularSynth = false;   // which synth do we use for point events (mouse clicks and the like)
    int granEnvDuration = 120;          // envelope duration in ms

    // ====== Granular Synth ====== //
    
    public float[] granSignal;                  // buffer source for granular (defaults to audioSignal)
    public PAGranularInstrument gSynth;         // granular synthesis instrument
    public PAGranularInstrumentDirector gDir;   // director of granular events
    public float granularGain = AudioUtility.dbToLinear(-3.0f);    // linear gain for a granular gesture event
    public float granularPointGain = 1.0f;      // linear gain for a granular point event
    // parameters for granular synthesis
    boolean useShortGrain = false;              // default to short grains, if true
    int longSample = 4096;                      // number of samples for a moderately long grain
    int shortSample = 512;                      // number of samples for a relatively short grain
    int granSamples = useShortGrain ? shortSample : longSample;    // number of samples in a grain window
    int hopSamples = granSamples/4;             // number of samples between each grain in a granular burst
    GestureGranularParams gParamsGesture;       // granular parameters when hopMode == HopMode.GESTURE
    GestureGranularParams gParamsFixed;         // granular parameters when hopMode == HopMode.FIXED
    public int curveSteps = 8;                  // number of steps in curve representation of a brushstroke path
    public int gMaxVoices = 256;                // number of voices managed by a PAGranularInstrument (gSynth)
    boolean useLongBursts = true;               // controls the number of burst grains in a point event or gesture event
    int maxBurstGrains = 8;
    int minBurstGrains = 1;
    int burstGrains = useLongBursts ? maxBurstGrains : minBurstGrains;
	boolean usePitchedGrains = false;           // detune a granular gesture, if true


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
    int videoSteps = 768;                // how many steps in an animation loop
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
    ArrayList<SamplerBrushEvent> samplerTimeLocs;       // a list of timed events for Sampler brushes
	ArrayList<TimedLocation> pointTimeLocs;             // a list of timed events for mouse clicks
	ArrayList<TimedLocation> grainTimeLocs;             // a list of timed events for Granular brushes
    int animatedCircleColor = color(233, 220, 199);     // color for animated circles when playing audio   

    PABoundsPolicy.PABoundaryMode boundaryMode = PABoundsPolicy.PABoundaryMode.CLIP;
    PABoundsPolicy boundsPolicy;

    /* ------------------------------------------------------------------ */
	/*                       INTERACTION VARIABLES                        */
	/* ------------------------------------------------------------------ */

    // ====== Minimal gesture + audio synthesis configuration ====== //
    // GesturePlayground example provides a complete configuration model with the GestureGranularConfig class
    
    /** Select PACurveMaker gesture points model. */
    public enum PathMode { ALL_POINTS, REDUCED_POINTS, CURVE_POINTS }    
    /** Select audio synthesis model */
    public enum BrushOutput { SAMPLER, GRANULAR } 
    /** Select audio gesture sequence timing model. */
    public enum HopMode { GESTURE, FIXED }

    /**
     * Defines the curve drawing model for a brush using PACurveMaker. 
     */
    public static final class BrushConfig {
    	/** PathMode for drawing */
        public PathMode pathMode = PathMode.ALL_POINTS;
    	/** Point reduction parameter */
        public float rdpEpsilon = 4.0f;
    	/** Number of polygon segments in a curve */
        public int curveSteps = 8;
        // leave room for future compatibility
    }

    /**
     * A light-weight version of the {@link net.paulhertz.pixelaudio.curves.AudioBrush AudioBrush} 
     * class for combining gestures and audio synthesis. 
     * AudioBrushLite combines PACurveMaker gesture and curve modeling with basic 
     * audio synthesis parameters. AudioBrush, introduced in TutorialOne_05_GesturePlayground, 
     * exposes most of the parameters used for granular synthesis in PixelAudio, along with 
     * many of the brush and gesture settings available in PACurveMaker. 
     */
    public static final class AudioBrushLite {
    	/** PACurveMaker instance stores point and time data, provides drawing and scheduling methods  */
    	private final PACurveMaker curve;
    	/** Configuration variables for drawing */
        private final BrushConfig cfg;
        /** Audio output variable */
        private BrushOutput output;
    	/** For granular instrument, timing model */
        private HopMode hopMode;
        /** Pitch scaling */
        private float pitchRatio = 1.0f;

        public AudioBrushLite(PACurveMaker curve, BrushConfig cfg, BrushOutput output, HopMode hopMode) {
            this.curve = curve;
            this.cfg = cfg;
            this.output = output;
            this.hopMode = hopMode;
            this.pitchRatio = 1.0f;
        }
        
        public PACurveMaker curve() { return curve; }
        public BrushConfig cfg() { return cfg; }
        public BrushOutput output() { return output; }
        public void setOutput(BrushOutput out) { this.output = out; }
        public float pitchRatio() { return pitchRatio; }
        public void setPitchRatio(float newRatio) { this.pitchRatio = newRatio; }
        public HopMode hopMode() { return hopMode; }
        public void setHopMode(HopMode hop) { this.hopMode = hop; }
        
    }

    /**
     *  A class to store a hit-test result, recording an AudioBrushLite instance and its index in brush list.
     */
    public static final class BrushHit {
        public final AudioBrushLite brush;
        public final int index;
        public BrushHit(AudioBrushLite brush, int index) {
            this.brush = brush;
            this.index = index;
        }
    }
    
    boolean doPlayOnNewBrush = true;    // play audio immediately on creating a new brush
	boolean shiftIsDown = false;        // shift key state
	

	/* ------------------------------------------------------------------ */
	/*                         WINDOWED BUFFER                            */
	/* ------------------------------------------------------------------ */

    /** Full-file mono source, padded to at least mapper.getSize(). */
    float[] anthemSignal;
    /** Full-file source buffer used by the Sampler instrument pool. */
    MultiChannelBuffer anthemBuffer;
    /** Moving view onto anthemSignal, with window size == mapper.getSize(). */
    WindowedBuffer windowBuff;

    /** Hop size for automatic window traversal. */
    int windowHopSize = 64;
    /** If true, draw() advances windowBuff each frame and refreshes the display. */
    boolean isWindowing = false;
    /** Save animation state when file loading or windowing temporarily suspends it. */
    boolean oldIsWindowing = false;

    /** Optional random point-event texture while windowing. */
    boolean isRaining = false;
    
    /** WindowBuffer initialized or not?  see initAudio() */
    boolean windowAudioReady = false;
    /** Sampler instruments use anthemBuffer, not the current visible playBuffer. */
    MultiChannelBuffer samplerSourceRef = null;
    /** sampling rate of samplerSourceRef */
    float samplerSourceRateRef = -1;

    // NOTE: I use poolSize = 2 and samplerMaxVoices = 64 in this sketch.
    // Using samplerMaxVoices = 256 resulted in noise artifacts whose
    // origin I have not tracked down yet. You can probably set poolSize = 8
    // and samplerMaxVoices = 128 without difficulty. I tested these values and 
    // didn't have any problems. TODO resolve the issue with noise when 
    // samplerMaxVoices is comparatively large. I regard the issue as minor,
    // as 128 voices should be entirely adequate for most uses of the Sampler. 
	
    /* ------------------------------------------------------------------ */
	/*                 APPLICATION SPECIFIC VARIABLES                     */
	/* ------------------------------------------------------------------ */
    
	// system-specific path to example files data
    
	// system-specific path to example files data
	String daPath = "/Users/paulhz/Code/Workspace/PixelAudio/examples/examples_data/";    // Eclipse
	// String daPath = sketchPath("") + "../../examples_data/";                           // Processing    
    String daFile = "_sonic/FullMoonTonight_22050Hz.mp3";    // _sonic/FullMoonTonight_22050Hz.mp3, Saucer_mixdown.wav
    
    boolean isDebugging = false;


    // ---------------- APPLICATION ---------------- //
	
	public static void main(String[] args) {
		PApplet.main(new String[] { TutorialOne_06_WindowBuffer.class.getName() });
	}

	public void settings() {
		size(3 * genWidth, 2 * genHeight);
	}
	
	/// Processing `setup()` method.
	public void setup() {
        surface.setTitle("PixelAudio Tutorial One 06: WindowBuffer");
		// set a standard animation framerate
		frameRate(24);
		// 1. Initialize our library.
		pixelaudio = new PixelAudio(this);
		// 2. create a PixelMapGen object with dimensions equal to the display window.
		//    the call to "hilbertLoop3x2(genWidth, genHeight)" produces a MultiGen that is 3 * genWidth x 2 * genHeight, 
		//    where genWidth == genHeight and genWidth is a power of 2 (a restriction on Hilbert curves)
		//
		// multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
		//
		//    Here's an alternative multigen that is good for visualizing audio location within the image
		//    It reads left to right, top to bottom, like a musical score. I've divided it 12 x 8, but 6 x 4 is good, too.
		//
		multigen = HilbertGen.hilbertRowOrtho(12, 8, width/12, height/8);
		// 3. Initialize a PixelAudioMapper with multigen
		mapper = new PixelAudioMapper(multigen);
		// area of the PixelAudioMapper == number of pixels in display == number of samples in audio buffer
		mapSize = mapper.getSize();
		// initialize the boundary policy for keeping points and indices in bounds
		boundsPolicy = PABoundsPolicy.fromWidthHeight(mapper.getWidth(), mapper.getHeight(), boundaryMode);
		// create an array of rainbow colors with mapSize elements
		spectrum = getColors(mapSize);
		// 4. create image for display
		initImages();
		// 5. set up the audio environment and variables
		initAudio();
		// 6. set up the drawing environment and variables
		initDrawing();
		// 7. output a help message to the console
		showHelp();
		// preload files, parameters will vary with the system where we're running
		preloadFiles(daPath, daFile);    // handy when debugging, too
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
		colorMode(HSB, colorWheel.length, 100, 100); // create colors in the HSB color space, giving hue a very wide range
		for (int i = 0; i < colorWheel.length; i++) {
			colorWheel[i] = color(i, 50, 70); // fill our array with colors, gradually changing hue
		}
		popStyle(); // restore styles, including the default RGB color space
		return colorWheel;
	}
	
	/**
	 * Initializes mapImage with the colors array. 
	 * MapImage handles the color data for mapper and also serves as our display image.
	 * BaseImage is intended as a reference image that typically only changes when you load a new image.
	 */
	public void initImages() {
		mapImage = createImage(width, height, ARGB);
		mapImage.loadPixels();
		mapper.plantPixels(spectrum, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
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
		applyColor(spectrum, mapImage.pixels, mapper.getImageToSignalLUT());
		mapImage.updatePixels();
		// write mapImage to baseImage
		commitMapImageToBaseImage();
	}

	public void draw() {
        if (isWindowing && windowBuff != null) {
            refreshWindowFromBacking(true);
    		mapImage.loadPixels();
    		applyColor(spectrum, mapImage.pixels, mapper.getImageToSignalLUT());
    		mapImage.updatePixels();           
            if (isRaining) raindrops(3);
        }
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
	 * Renders a frame of pixel-shifting animation: moving along the signal path, copies baseImage pixels to
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
	            addDrawingPoint(clipToWidth(mouseX), clipToHeight(mouseY));
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
     * Random point events along the upper part of the current window, useful for
     * hearing time-structured files while the window is advancing.
     */
    public void raindrops(int count) {
        for (int i = 0; i < count; i++) {
            if (random(1) < 0.35f) {
                int x = (int)random(width);
                int y = (int)random(Math.max(1, height / 8));
                handleClickOutsideBrush(x, y);
            }
        }
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
			} else {
				scheduleGranularBrushClick(activeBrush);   // Granular brush clicked event
			}
			return;
		}
		// click outside any brush is handled here
		handleClickOutsideBrush(clipToWidth(mouseX), clipToHeight(mouseY));
	}

		
	/**
     * Built-in keyPressed handler, forwards events to parseKey.
     */
    @Override
    public void keyPressed() {
        if (key == CODED && keyCode == SHIFT) {
            shiftIsDown = true;
            // println("-->> shiftIsDown " + shiftIsDown);
            return;
        }
    	if (key != CODED) {
    		parseKey(key, keyCode);
    	}
    	else {
			if (keyCode == UP) {
				   adjustAudioGain(shiftIsDown ? 3.0f : 1.0f);
				println("---- audio gain is "+ nf(audioOut.getGain(), 0, 2) +"dB");
			}
			else if (keyCode == DOWN) {
				adjustAudioGain(shiftIsDown ? -3.0f : -1.0f);
				println("---- audio gain is "+ nf(audioOut.getGain(), 0, 2) +"dB");
			}
			else if (keyCode == RIGHT) {				
				if (!shiftIsDown) {
					adjustPoolGain(3.0f);
					println("---- pool gain is "+ nf(pool.getGainDb(), 0, 2) +"dB");					
				} else {
					adjustGranGain(3.0f);
					println("---- granular gain is "+ nf(gDir.getInstrument().getGlobalGainDb(), 0, 2) +"dB");					
				}
			}
			else if (keyCode == LEFT) {
				if (!shiftIsDown) {
					adjustPoolGain(-3.0f);					
					println("---- pool gain is "+ nf(pool.getGainDb(), 0, 2) +"dB");					
				} else {
					adjustGranGain(-3.0f);
					println("---- granular gain is "+ nf(gDir.getInstrument().getGlobalGainDb(), 0, 2) +"dB");					
				}
			}
    	}
    }

    @Override
    public void keyReleased() {
        if (key == CODED && keyCode == SHIFT) {
            shiftIsDown = false;
            // println("-->> shiftIsDown " + shiftIsDown);
        }
    }
    
	/**
	 * Handles key press events passed on by the built-in keyPressed method. 
	 * By moving key event handling outside the built-in keyPressed method, 
	 * we make it possible to post key commands without an actual key event.
	 * Methods and interfaces and even other threads can call parseKey(). 
	 * This opens up many possibilities and a some risks, too.  
	 * 
	 * @param key        the key that was pressed, as a char
	 * @param keyCode    keyCode for the key that was pressed
	 */
	public void parseKey(char key, int keyCode) {
		String msg;
		switch(key) {
		case ' ': // (spacebar) trigger a brush if we're hovering over a brush, otherwise trigger a point event
			if (hoverBrush != null) {
				if (hoverBrush.output() == BrushOutput.SAMPLER) {
					scheduleSamplerBrushClick(hoverBrush);    // Sampler brush clicked event
				} else {
					scheduleGranularBrushClick(hoverBrush);   // Granular brush clicked event
				}
			}
			else {
				handleClickOutsideBrush(clipToWidth(mouseX), clipToHeight(mouseY));
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
		case 'y': // select granular or sampler synth for click response
			useGranularSynth = !useGranularSynth;
			String synth = (useGranularSynth) ? "granular." : "sampler.";
			println("-- default synth for click events is "+ synth);
			break;
		case 'r': // select long or short grain duration 
			useShortGrain = !useShortGrain;
			granSamples = useShortGrain ? shortSample : longSample;
			hopSamples = granSamples/4;
			initGranularParams();
			println("-- Granular synth grain length = "+ granSamples +" samples with "+ hopSamples +" samples hop length");
			break;
		case 'b': // toggle between long burst grains and short burst grains
			useLongBursts = !useLongBursts;
			burstGrains = useLongBursts ? maxBurstGrains : 1;
			initGranularParams();
			msg = useLongBursts ? "long bursts of "+ maxBurstGrains : "short bursts of "+ minBurstGrains ;
			println("-- Granular synth uses "+ msg +" grains.");
			break;
		case 'p': // jitter the pitch of granular gestures
			usePitchedGrains = !usePitchedGrains;
			msg = (usePitchedGrains) ? " jitter granular pitch." : " steady granular pitch.";
			println("-- Play granular synth with "+ msg);
			break;
		case 'P': // adjust pitch of current brushstroke
			if (hoverBrush == null) return;
			pitchScaling = hoverBrush.pitchRatio();
			if (pitchScaling == defaultPitchScaling) {
				pitchScaling = lowPitchScaling;
			} else if (pitchScaling == lowPitchScaling) {
				pitchScaling = highPitchScaling;
			} else if (pitchScaling == highPitchScaling) {
				pitchScaling = defaultPitchScaling;
			} else {
				pitchScaling = defaultPitchScaling;
			}
			hoverBrush.setPitchRatio(pitchScaling);
			msg = "hover brush pitch scaling set to "+ hoverBrush.pitchRatio();
			println("-- "+ msg);
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
		case ']': // increment curve steps value of current brush
			if (hoverBrush != null) {
				int cs = hoverBrush.curve.getCurveSteps();
				cs = cs < 32 ? cs + 1 : cs;
				setBrushCurveSteps(hoverBrush, cs);
				println("-- curveSteps for current brush is "+ hoverBrush.curve.getCurveSteps());
			}
			break;
		case '[': // decrement curve steps value of current brush
			if (hoverBrush != null) {
				int cs = hoverBrush.curve.getCurveSteps();
				cs = cs > 1 ? cs - 1 : cs;
				setBrushCurveSteps(hoverBrush, cs);
				println("-- curveSteps for current brush is "+ hoverBrush.curve.getCurveSteps());
			}
			break;
		case 'm': // change the Sampler noise reduction profile
			pool.cycleMixProfile();
			println("-- mix profile is "+ pool.getMixProfile().name());
			break;
		case 'e': // change the envelope we're using 
			presetIndex = (presetIndex + 1) % envPresets.length;
			defaultEnv = envPreset(envPresets[presetIndex]);
			println("-- default envelope is "+ envPresets[presetIndex] +", "+ defaultEnv.toString());
			break;
		case 'E': // toggle whether we adjust envelope duration in relation to gesture duration
			isAdjustEnvelope = !isAdjustEnvelope;
			println("-- isAdjustEnvelope is "+ isAdjustEnvelope);
			break;
		case 'a': //  start or stop animation (bitmap rotation along the signal path)
			isAnimating = !isAnimating;
			println("-- animation is " + isAnimating);
			break;
		case '/': // turn drawing on or off
			// turn off animation (if you insist, you can try drawing with it on, just press the 'a' key)
			isAnimating = false;
			isDrawMode = !isDrawMode;
			println(isDrawMode ? "----- Drawing is turned on" : "----- Drawing is turned off");
			break;
		case 'd': // toggle play audio on new brush 
			doPlayOnNewBrush = !doPlayOnNewBrush;
			println("-- play audio when creating a new brush is "+ (doPlayOnNewBrush ? "ON" : "OFF"));
			break;
		case 'c': // apply color from image file to display image
			chooseColorImage();
			break;
		case 'k': // apply the hue and saturation in the colors array to mapImage (not to baseImage)
			applyColorMapToDisplay(false);
			break;
		case 'K': // apply hue and saturation in colors to baseImage and mapImage
			applyColorMapToDisplay(true);
			break;
		case 'j': // turn audio and image blending on or off
			isBlending = !isBlending;
			println("-- isBlending is "+ isBlending +" (images blend only if chan = ChannelNames.ALL)");
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
		case 'w': // write the image HSB Brightness channel to the audio buffer as transcoded sample values 
			commitMapImageToAudio();
			println("--->> Wrote image to audio as audio data.");
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
			parseKeyWB(key, keyCode);
		}
	}
	
	public void parseKeyWB(char key, int keyCode) {
		switch(key) {
		case 'T':
			isWindowing = !isWindowing;
			if (isWindowing) isAnimating = false;
			totalShift = 0;
			println("---- WindowBuffer traversal is " + (isWindowing ? "ON" : "OFF"));
			break;
		case '}':
			moveAudioWindow(windowBuff != null ? windowBuff.getWindowSize() : mapper.getSize());
			break;
		case '{':
			moveAudioWindow(windowBuff != null ? -windowBuff.getWindowSize() : -mapper.getSize());
			break;
		case ')':
			moveAudioWindow(windowBuff != null ? windowBuff.getWindowSize() / 2 : mapper.getSize() / 2);
			break;
		case '(':
			moveAudioWindow(windowBuff != null ? -windowBuff.getWindowSize() / 2 : -mapper.getSize() / 2);
			break;
		case 'R':
			resetAudioWindow();
			break;
		case 'Y':
			isRaining = !isRaining;
			println("---- WindowBuffer raindrops are " + (isRaining ? "ON" : "OFF"));
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
		println(" * Press RIGHT ARROW to increase Sampler audio gain by 3 dB.");
		println(" * Press LEFT ARROW to decrease Sampler audio gain by 3 dB.");
		println(" * Press SHIFT-RIGHT ARROW to increase Granular audio gain by 3 dB.");
		println(" * Press SHIFT-LEFT ARROW to decrease Granular audio gain by 3 dB.");
		println(" * Press ' ' (spacebar) to trigger a brush if we're hovering over a brush, otherwise trigger a point event.");
		println(" * Press '1' to set brushstroke under cursor to PathMode ALL_POINTS.");
		println(" * Press '2' to set brushstroke under cursor to PathMode REDUCED_POINTS.");
		println(" * Press '3' to set brushstroke under cursor to PathMode CURVE_POINTS.");
		println(" * Press 't' to set brushstroke under cursor to use Sampler or Granular synth.");
		println(" * Press 'f' to set brushstroke under cursor to FIXED hop mode for granular events.");
		println(" * Press 'g' to set brushstroke under cursor to GESTURE hop mode for granular events.");
		println(" * Press 'y' to select granular or sampler synth for click response.");
		println(" * Press 'r' to select long or short grain duration .");
		println(" * Press 'b' to toggle between long burst grains and short burst grains.");
		println(" * Press 'p' to jitter the pitch of granular gestures.");
		println(" * Press 'P' to adjust pitch of current brushstroke.");
		println(" * Press '>' or '.' to increment epsilon value of current brush (reduced points decrease).");
		println(" * Press '<' or ',' to decrement epsilon value of current brush (reduced points increase).");
		println(" * Press ']' to increment curve steps value of current brush.");
		println(" * Press '[' to decrement curve steps value of current brush.");
		println(" * Press 'm' to change the Sampler noise reduction profile.");
		println(" * Press 'e' to change the envelope we're using .");
		println(" * Press 'E' to toggle whether we adjust envelope duration in relation to gesture duration.");
		println(" * Press 'a' to  start or stop animation (bitmap rotation along the signal path).");
		println(" * Press 'd' to toggle play audio on new brush .");
		println(" * Press 'c' to apply color from image file to display image.");
		println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage (not to baseImage).");
		println(" * Press 'K' to apply hue and saturation in colors to baseImage and mapImage.");
		println(" * Press 'j' to turn audio and image blending on or off.");
		println(" * Press 'n' to normalize the audio buffer to -6.0dB.");
		println(" * Press 'L' or 'l' to determine whether to load a new file to both image and audio or not.");
		println(" * Press 'o' or 'O' to open an audio or image file and load to image or audio buffer or both.");
		println(" * Press 's' to save display image to a PNG file.");
		println(" * Press 'S' to save audio buffer to a .wav file.");
		println(" * Press 'w' to write the image HSB Brightness channel to the audio buffer as transcoded sample values .");
		println(" * Press 'W' to write the audio buffer samples to the image as color values.");
		println(" * Press 'x' to delete the current active brush shape or the oldest brush shape.");
		println(" * Press 'X' to delete the most recent brush shape.");
		println(" * Press 'u' to mute audio.");
		println(" * Press 'V' to record a video.");
		println(" * Press 'h' or 'H' to show help message in the console.");
        println("\n * WindowBuffer additions:");
        println(" * Press 'T' to toggle automatic WindowBuffer traversal.");
        println(" * Press '{' or '}' to jump back/forward one full window.");
        println(" * Press '(' or ')' to jump back/forward one half window.");
        println(" * Press 'R' to rewind the WindowBuffer.");
        println(" * Press 'Y' to toggle raindrop point events while windowing.\n");
	}

	/**
	 * Adjusts audioOut.gain.
	 * @param g   value to add to audioOut.gain, in decibels
	 */
	public void adjustAudioGain(float g) {
		float ag = audioOut.getGain();
		ag += g;
		if (ag > 12.0f || ag < -64.0f) return;
		audioOut.setGain(ag);
		outputGain = audioOut.getGain();
	}

	/**
	 * Sets Sampler instrument {@code pool} gain in dB.
	 * @param g   gain increment or decrement, in decibels
	 */
	public void adjustPoolGain(float g) {
		float pg = pool.getGainDb();
		pg += g;
		if (pg > 12.0f || pg < -64.0f) return;
		pool.setGainDb(pg);
	}
	
	/**
	 * Sets Granular instrument gain in dB.
	 * @param g   gain value for audioOut, in decibels
	 */
	public void adjustGranGain(float g) {		
		float gg = gDir.getInstrument().getGlobalGainDb();
		gg += g;
		if (gg > 12.0f || gg < -64.0f) return;
		gDir.getInstrument().setGlobalGainDb(gg);
	}
	
	
	/**
	 * Utility method for applying hue and saturation values from a source array of RGB values
	 * to the brightness values in a target array of RGB values, using a lookup table to redirect indexing.
	 * 
	 * @param colorSource    a source array of RGB data from which to obtain hue and saturation values
	 * @param graySource     a target array of RGB data from which to obtain brightness values
	 * @param lut            a lookup table, must be the same size as colorSource and graySource
	 * @return the graySource array of RGB values, with hue and saturation values changed
	 * @throws IllegalArgumentException if array arguments are null or if they are not the same length
	 */
	public int[] applyColor(int[] colorSource, int[] graySource, int[] lut) {
		return applyColorShifted(colorSource, graySource, lut, 0);
	}

	/**
	 * Utility method for applying hue and saturation values from a source array of RGB values
	 * to the brightness values in a target array of RGB values, using a lookup table to redirect indexing,
	 * taking into account any pixels that were shifted.
	 * 
	 * @param colorSource    a source array of RGB data from which to obtain hue and saturation values
	 * @param graySource     a target array of RGB data from which to obtain brightness values
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
	 * Apply color map hue and saturation to mapImage or baseImage.
	 *
	 * @param updateBaseImage    if true, update baseImage, otherwise just update mapImage
	 */
	public void applyColorMapToDisplay(boolean updateBaseImage) {
		if (updateBaseImage) {
			baseImage.loadPixels();
			applyColor(spectrum, baseImage.pixels, mapper.getImageToSignalLUT());
			baseImage.updatePixels();
			refreshMapImageFromBase();
		} else {
			refreshMapImageFromBase();
			mapImage.loadPixels();
			applyColorShifted(spectrum, mapImage.pixels, mapper.getImageToSignalLUT(), totalShift);
			mapImage.updatePixels();
		}
	}

	// *** WindowedBuffer support *** //	
	public void resetAudioWindow() {
		if (windowBuff == null) return;
		windowBuff.reset();
		refreshWindowFromBacking(false);
	}

	// *** WindowedBuffer support *** //
	public void moveAudioWindow(int delta) {
		if (windowBuff == null || anthemSignal == null || anthemSignal.length == 0) return;
		int next = PixelAudioMapper.wrap(windowBuff.getIndex() + delta, anthemSignal.length);
		windowBuff.setIndex(next);   // or equivalent setter in promoted WindowedBuffer
		refreshWindowFromBacking(false);
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
				println("----- File is not a recognized audio or image format ending with "
						+ "\"mp3\", \"wav\", \"aif\", \"aiff\", \"png\", \"jpg\" or \"jpeg\" .");
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
        println("-- local loadAudioFile ");
        boolean wasAnimating = isAnimating;
        boolean wasWindowing = isWindowing;
        boolean wasBlending = isBlending;
        isAnimating = false;
        isWindowing = false;
        isBlending = false;
        // hard stop old sampler/granular activity before replacing buffers
        suspendWindowAudio();
        // now the standard calls for loading audio from a file
		MultiChannelBuffer buff = new MultiChannelBuffer(1024, 1);
		fileSampleRate =  minim.loadFileIntoBuffer(audFile.getAbsolutePath(), buff);
        // load the audio file and resample it if necessary
		if (fileSampleRate > 0) {
			if (fileSampleRate != audioOut.sampleRate() && doResample) {
				float[] resampled = AudioUtility.resampleMonoToOutput(buff.getChannel(0), fileSampleRate, audioOut);
				buff.setBufferSize(resampled.length);
				buff.setChannel(0, resampled);
				bufferSampleRate = audioOut.sampleRate();
			}
			else {
				bufferSampleRate = fileSampleRate;
			}
			this.audioFileLength = buff.getBufferSize();
			println("---- file sample rate = "+ this.fileSampleRate 
					+", buffer sample rate = "+ bufferSampleRate
					+", audio output sample rate = "+ audioOut.sampleRate());
		}
		else {
			println("-- Unable to load file. File may be empty, wrong format, or damaged.");
			return;
		}
		// everything looks good so far, proceed (but blending is turned off) 
		// TODO omit blending with window buffer
		if (isBlending) {
		    blendInto(playBuffer, buff, 0.5f, false, -12.0f);
		    updateAudioChain(playBuffer.getChannel(0), bufferSampleRate);
		}
		else {
		    updateAudioChain(buff.getChannel(0), bufferSampleRate);
		}
		// update dependent audio sources
		if (isLoadToBoth) {
		    println("-- loading transcoded audio to display image\n");
		    renderAudioToMapImage(chan, 0);   // or writeAudioToImage(audioSignal, mapper, mapImage, chan)
		    commitMapImageToBaseImage();
		}
		// restore state
        isBlending = wasBlending;
        isAnimating = wasAnimating;
        isWindowing = wasWindowing;		
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
			commitMapImageToAudio();
		}
		else {
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
	 * @param sig       a target array of float in audio format
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
	
	public void commitMapImageToAudio() {
		float[] sig = new float[mapper.getSize()];
		audioSignal = sig;
		renderMapImageToAudio(PixelAudioMapper.ChannelNames.L);
		updateAudioChain(sig);
		commitMapImageToBaseImage();
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
		if (selection.getName().indexOf(".wav") != selection.getName().length() - 4) {
			fileName += ".wav";
		}
		try {
		    // Save at the current audio output rate. This favors performance use, but the rate
		    // may differ from the originally loaded file if resampling was disabled or applied.
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
	/*                        AUDIO METHODS                           */
	/*                                                                */
	/*----------------------------------------------------------------*/
	
	// *** WindowedBuffer support *** //
	/**
	 * CALL THIS METHOD IN SETUP() 
	 * Initializes Minim audio library and audio variables.
	 */
	public void initAudio() {
    	windowAudioReady = false;    // suspend operations that use the backing buffer
		// initialize the Minim library
		minim = new Minim(this);
		// Use the getLineOut method of the Minim object to get an AudioOutput object.
		// PixelAudio instruments require a STEREO output. 1024 is a standard number of
		// samples for the output buffer to process at one time. You should usually set
		// the output sampleRate to either 44100 or 48000, standards for digital audio.
		this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
		// set the gain (UP and DOWN arrow keys adjust)
		audioOut.setGain(outputGain); 
		println("---- audio out gain is "+ nf(audioOut.getGain(), 0, 2));
		// create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
		// the buffer will not have any audio data -- you'll need to open an audio file
		this.playBuffer = new MultiChannelBuffer(mapSize, 1);
		this.audioSignal = playBuffer.getChannel(0);
		this.granSignal = audioSignal;
		this.audioLength = audioSignal.length;
		// start envelope duration at noteDuration
		envDuration = noteDuration;
		// setup the backing buffer
		installBackingSource(playBuffer.getChannel(0), audioOut.sampleRate());
		// refresh variables that depend on the backing buffer
		refreshWindowFromBacking(false);
		windowAudioReady = true;
		// set up the sampler synth, taking the backing buffer into account
		ensureSamplerReady();
		// set up the granular synth, taking the backing buffer into account
		ensureGranularReady();
		// initialize audio event animation tracking arrays
		initTimedEventLists();
	}
	
	// *** WindowedBuffer support *** //
    /**
     * Installs a full-file backing source for WindowedBuffer and instruments.
     * Sets the window size to mapper.getSize() 
     */
    void installBackingSource(float[] sig, float sourceSampleRate) {
        int windowSize = (mapper != null) ? mapper.getSize() : mapSize;
        if (windowSize <= 0) return;

        int backingSize = Math.max(windowSize, (sig != null) ? sig.length : 0);
        float[] backing = new float[backingSize];
        if (sig != null) {
            System.arraycopy(sig, 0, backing, 0, Math.min(sig.length, backing.length));
        }

        anthemSignal = backing;
        anthemBuffer = new MultiChannelBuffer(backing.length, 1);
        anthemBuffer.setChannel(0, anthemSignal);

        bufferSampleRate = sourceSampleRate;
        audioFileLength = backing.length;
        windowHopSize = Math.max(1, Math.round(sourceSampleRate / Math.max(1f, frameRate)));
        windowBuff = new WindowedBuffer(anthemSignal, windowSize, windowHopSize);

        granSignal = anthemSignal;

        println("---- WindowBuffer backing source length = " + anthemSignal.length
                + ", window size = " + windowBuff.getWindowSize()
                + ", hop size = " + windowBuff.getHopSize());
    }

    // *** WindowedBuffer support *** //
    /**
     * Copies the current window into audioSignal and playBuffer. The Sampler and
     * Granular instruments still read from anthemSignal / anthemBuffer.
     */
    void refreshWindowFromBacking(boolean advance) {
        if (windowBuff == null || anthemSignal == null) return;

        if (advance) {
            windowBuff.nextWindow(); // advances exactly once
        }

        int targetSize = mapper.getSize();
        int start = PixelAudioMapper.wrap(windowBuff.getIndex(), anthemSignal.length);

        if (audioSignal == null || audioSignal.length != targetSize) {
            audioSignal = new float[targetSize];
        }

        for (int i = 0; i < targetSize; i++) {
            audioSignal[i] = anthemSignal[(start + i) % anthemSignal.length];
        }

        audioLength = audioSignal.length;

        if (playBuffer == null || playBuffer.getBufferSize() != targetSize) {
            playBuffer = new MultiChannelBuffer(targetSize, 1);
        }

        playBuffer.setChannel(0, audioSignal);

        totalShift = 0;
        renderAudioToMapImage(chan, 0);
        commitMapImageToBaseImage();
    }

    // *** WindowedBuffer support *** //
	/**
	 * Ensures that all resources and variable necessary for the Sampler synth are ready to go.
	 */
	void ensureSamplerReady() {
        // Prevent super.initAudio() from creating a sampler pool before
        // anthemBuffer has been installed.
        if (!windowAudioReady) return;
        // Prefer anthemBuffer as our source
        MultiChannelBuffer source = (anthemBuffer != null) ? anthemBuffer : playBuffer;
        if (isDebugging) {
        	if (source == anthemBuffer) println("-- sampler source is anthemBuffer");
        	if (source == playBuffer) println("-- sampler source is playBuffer");
        	if (source == null) println("-- sampler source is null");
        }
        float sr = (bufferSampleRate > 0) ? bufferSampleRate : sampleRate;
        if (source == null) return;
        // Rebuild the Sampler instrument, pool, when required
        // Loading a new file to the backing buffer should trigger this
        boolean needsRebuild =
                pool == null
                || pool.isClosed()
                || source != samplerSourceRef
                || sr != samplerSourceRateRef;

        if (needsRebuild) {
            if (pool != null && !pool.isClosed()) {
                pool.stopAll();
                pool.close();
            }
            pool = new PASamplerInstrumentPool(
                    source,
                    sr,
                    poolSize,
                    samplerMaxVoices,
                    audioOut,
                    defaultEnv
            );
            samplerSourceRef = source;
            samplerSourceRateRef = sr;
        }
        else {
            // Important: keep the whole pool bound to anthemBuffer.
            // Do not rely on playSample(anthemBuffer, ...) to patch one instrument at a time.
            pool.setBuffer(source, sr);
        }

        pool.setGain(samplerGain);
	}
	
    // *** WindowedBuffer support *** //
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
	    	gSynth.setGlobalGain(granularGain);
	    }
	    if (granSignal == null) {
	        granSignal = (audioSignal != null) ? audioSignal : playBuffer.getChannel(0);
	    }
	    if (gDir == null) {
	    	gDir = new PAGranularInstrumentDirector(gSynth);
	    }
	    // when we use a backing buffer, point granSignal at anthemSignal
	    if (anthemSignal != null) granSignal = anthemSignal;
	}
	
	void initTimedEventLists() {
		// initialize mouse event tracking array
		pointTimeLocs = new ArrayList<TimedLocation>();
		samplerTimeLocs = new ArrayList<>();   // capture timing data when drawing
		grainTimeLocs = new ArrayList<>();
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
		
    // *** WindowedBuffer support *** //
	/**
     * Point clicks are mouse location, but the initial samplePos is the 
     * backing source index. Long granular bursts still create
     * display points in the current window; playGranularGesture(...) remaps
     * them to backing indices.
     * 
	 * @param x
	 * @param y
	 */
    public int handleClickOutsideBrush(int x, int y) {
        int localSamplePos = mapper.lookupSignalPosShifted(x, y, totalShift);
        samplePos = backingIndexFromLocal(localSamplePos);

        if (!useGranularSynth) {
            ensureSamplerReady();
            float panning = map(x, 0, width, -0.8f, 0.8f);
            int len = calcSampleLen();
            samplelen = playSample(samplePos, len, samplerPointGain, defaultEnv, panning);
            int durationMS = (int)(samplelen / sampleRate * 1000);
            pointTimeLocs.add(new TimedLocation(x, y, durationMS + millis() + 50));
            return durationMS;
        }

        ensureGranularReady();
        float hopMsF = (int)Math.round(AudioUtility.samplesToMillis(hopSamples, sampleRate));
        final int grainCount = useLongBursts
                ? 2 * (int)Math.round(noteDuration / hopMsF)
                : (int)Math.round(noteDuration / hopMsF);
        println("-- granular point burst with grainCount = " + grainCount);

        java.util.ArrayList<PVector> path = new java.util.ArrayList<>(grainCount);
        int[] timing = new int[grainCount];
        for (int i = 0; i < grainCount; i++) {
            if (useLongBursts) {
                int local = PixelAudioMapper.wrap(localSamplePos + hopSamples * i, mapper.getSize());
                path.add(getCoordFromSignalPosLocal(local));
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
        playGranularGesture(backingGranularSignal(), sched, gParamsFixed, 1.0f);
        storeGranularCurveTL(sched, startTime, false);
        return curve.getTimeOffset();
    }
    
    // *** WindowedBuffer support *** //
    float[] backingGranularSignal() {
    	return (anthemSignal != null && anthemSignal.length > 0) ? anthemSignal : audioSignal;
    }


    // *** WindowedBuffer support *** //
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
        int local = mapper.lookupSignalPosShifted(x, y, totalShift);
        return backingIndexFromLocal(local);
    }

    // *** WindowedBuffer support *** //
    int backingIndexFromLocal(int localIndex) {
        if (windowBuff == null || anthemSignal == null || anthemSignal.length == 0) {
            return PixelAudioMapper.wrap(localIndex, Math.max(1, mapper.getSize()));
        }
        int local = PixelAudioMapper.wrap(localIndex, mapper.getSize());
        return PixelAudioMapper.wrap(windowBuff.getIndex() + local, anthemSignal.length);
    }

	/**
	 * Calculates the display image coordinates corresponding to a specified audio sample index,
	 * use this version when the position is used to calculate local display coordinates. 
	 * @param pos    an index into an audio signal, must be between 0 and width * height - 1.
	 * @return       a PVector with the x and y coordinates
	 */
	public PVector getCoordFromSignalPosLocal(int pos) {
		int[] xy = this.mapper.lookupImageCoordShifted(pos, totalShift);
		return new PVector(xy[0], xy[1]);
	}

	// *** WindowedBuffer support *** //
    /**
	 * Calculates the display image coordinates corresponding to a specified audio sample index,
	 * use when the position is relative to the backing buffer. 
	 * @param pos    an index into an audio signal, must be between 0 and width * height - 1.
	 * @return       a PVector with the x and y coordinates
	 * 
     * Backing-file index -> visible display coordinate, relative to the current window.
     */
    public PVector getCoordFromSignalPos(int pos) {
        int local = pos;
        if (windowBuff != null && anthemSignal != null && anthemSignal.length > 0) {
            local = PixelAudioMapper.wrap(pos - windowBuff.getIndex(), mapper.getSize());
        }
        int[] xy = mapper.lookupImageCoordShifted(local, totalShift);
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

	// *** WindowedBuffer support *** //
	/**
	 * Plays an audio sample with  with a custom envelope, pitch and stereo pan.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param samplelen    length of the sample (will be adjusted)
	 * @param amplitude    amplitude of the sample on playback
	 * @param env          an ADSR envelope for the sample
	 * @param pitch        pitch scaling as deviation from default (1.0), where 0.5 = octave lower, 2.0 = octave higher 
	 * @param pan          position of sound in the stereo audio field (-1.0 = left, 0.0 = center, 1.0 = right)
	 * @return
	 */
	public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pitch, float pan) {
        if (anthemBuffer == null || pool == null) {
        	return 0;
            // return playSample(samplePos, samplelen, amplitude, env, pitch, pan);
        }
        // debugging
        // println("-- retrofit playSample() call ");
        int pos = PixelAudioMapper.wrap(samplePos, anthemBuffer.getBufferSize());
        int len = Math.min(samplelen, anthemBuffer.getBufferSize() - pos);
        // debugging information for windowed buffer
        // println("---- 06 playSample pos=" + pos + " len=" + len
        //    + " anthemBuffer=" + anthemBuffer.getBufferSize() 
        //    + " windowIndex=" + windowBuff.getIndex());
		return pool.playSample(samplePos, samplelen, amplitude, env, pitch, pan);
	}
	
	
    /**
	 * @return a length in samples with some Gaussian variation
	 */
	public int calcSampleLen(int durMs, double mean, double variance) {
		float vary = 0; 
		// skip the fairly rare negative numbers
		while (vary <= 0) {
			vary = (float) PixelAudio.gauss(mean, variance);
		}
		samplelen = (int)(abs((vary * durMs) * sampleRate / 1000.0f));
		// println("---- calcSampleLen samplelen = "+ samplelen +" samples at "+ sampleRate +"Hz sample rate");
		return samplelen;
	}

	/**
	 * @return a length in samples with some Gaussian variation
	 */
	public int calcSampleLen() {
		return calcSampleLen(envDuration, 1.0, 0.0625);
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
	 * @param name   the name of the ADSRParams envelope to return
	 * @return the specified ADSRParams envelope
	 */
	static ADSRParams envPreset(String name) {
		switch (name) {
        // medium fast attack, medium decay to 0.15, short tail 
		case "Soft"       : return new ADSRParams(1.0f, 0.02f, 0.80f, 0.7f, 0.2f);   
		// sharp attack, fast decay to 0.1, short tail
        case "Percussion" : return new ADSRParams(1.0f, 0.005f, 0.04f, 0.20f, 0.06f);   
        // long attack, slight decay, long tail
		case "Pad"        : return new ADSRParams(1.0f, 0.50f, 0.25f, 0.75f, 1.0f);    
        // fast attack, fast decay to 0.75, medium tail
		default           : return new ADSRParams(1.0f, 0.01f, 0.15f, 0.75f, 0.25f);    
		}
	}
	
    // *** WindowedBuffer support *** //	
    /**
     * Suspend audio instruments and events when a new backing buffer is in the process of loading.
     */
    void suspendWindowAudio() {
    	// clear audio/animation TimedLocation events
        if (samplerTimeLocs != null) samplerTimeLocs.clear();
        if (pointTimeLocs != null) pointTimeLocs.clear();
        if (grainTimeLocs != null) grainTimeLocs.clear();
        // shut down Sampler instrument, pool
        if (pool != null) {
            pool.stopAll();
            pool.close();
            pool = null;
        }
        samplerSourceRef = null;
        samplerSourceRateRef = -1;
        // shut down Granular instrument, gDir
        if (gDir != null) gDir.cancelAndReleaseAll();
    }
 
	
    // *** WindowedBuffer support *** //	
	/**
	 * Bottleneck "commit" method for audio state. 
	 * 
     * This is the ONLY method that should mutate the global audio signal state.
     * 
     * Modified for use with WindowedBuffer and a backing buffer, installBackingSource()
     * and refreshWindowFromBacking() set the various buffers. The ensureSamplerReady()
     * and ensureGranularReady() reset instruments. 
     * 
     * In PixelAudio examples, the signal is typically loaded from a file, but
     * it could also be signal cached in memory, a signal generated by code, audio
     * captured live, etc. 
	 * 
	 * @param sig                 an audio signal
	 * @param sourceSampleRate    audio sample rate for sig, 
	 *                            usually obtained when reading from an audio file
	 */
	void updateAudioChain(float[] sig, float sourceSampleRate) {
        installBackingSource(sig, sourceSampleRate);
        refreshWindowFromBacking(false);
        // critical: rebind instruments after anthemBuffer/anthemSignal change
        ensureSamplerReady();
        ensureGranularReady();
	}
	
    // *** WindowedBuffer support *** //	
	void updateAudioChain(float[] sig) {
        float sr = (audioOut != null) ? audioOut.sampleRate() : sampleRate;
		updateAudioChain(sig, sr);
	}

	
    // *** WindowedBuffer support *** //	
	/**
	 * Calls PAGranularInstrumentDirector gDir to play a granular audio event.
	 * 
	 * @param buf       an audio signal as an array of float (null, if we are using the backing buffer)
	 * @param sched     an GestureSchedule with coordinate and timing information 
	 * @param params    a bundle of control parameters for granular synthesis
	 */
	public void playGranularGesture(float buf[], GestureSchedule sched, GestureGranularParams params, float pitchRatio) {
        if (sched == null || sched.isEmpty()) return;
        float[] source = (buf != null) ? buf : backingGranularSignal();
        int sourceLen = (source != null && source.length > 0) ? source.length : mapper.getSize();

        int[] localIndices = mapper.lookupSignalPosArray(sched.points, totalShift, mapper.getSize());
        int[] startIndices = new int[localIndices.length];
        for (int i = 0; i < localIndices.length; i++) {
            startIndices[i] = (windowBuff != null)
                    ? PixelAudioMapper.wrap(windowBuff.getIndex() + localIndices[i], sourceLen)
                    : PixelAudioMapper.wrap(localIndices[i], sourceLen);
        }
		
		// calculate the pan for each grain, based on its x-coordinate
		float[] panPerGrain = new float[sched.size()];
		for (int i = 0; i < sched.size(); i++) {
		    PVector p = sched.points.get(i);
		    // example: map x to [-0.8, +0.8]
		    panPerGrain[i] = map(p.x, 0, width-1, -0.875f, 0.875f);
		}
		
		float jitter = usePitchedGrains ? 0.25f : 0f;
		float[] pitch = generateJitterPitch(sched.size(), jitter, pitchRatio);
		// assemble all the grain-level attributes into a GestureEventParams object
		GestureEventParams eventParams = GestureEventParams.builder(sched.size())
				.startIndices(startIndices)
				.pan(panPerGrain)
				.pitchRatio(pitch)
				.build();
		// call playGestureNow() with eventParams and return
		gDir.playGestureNow(buf, sched, params, eventParams);
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
	    addDrawingPoint(x, y);
	}

	/**
	 * While user is dragging the mouses and isDrawMode == true, accumulates new points
	 * to allPoints and event times to allTimes. Sets sampleX, sampleY and samplePos variables.
	 * We constrain points outside the bounds of the display window. An alternative approach 
	 * is be to ignore them, which may give a more "natural" appearance for fast drawing. 
	 *
	 * @param x    x-coordinate of point to add to allPoints
	 * @param y    y-coordinate of point to add to allPoints
	 */
	public void addDrawingPoint(int x, int y) {
	    if (x != currentPoint.x || y != currentPoint.y) {
	        currentPoint = new PVector(clipToWidth(x), clipToHeight(y));
	        allPoints.add(currentPoint);
	        allTimes.add(millis() - startTime);
	    }
	}
	    
	/**
	 * Clips parameter i to the interval (0..width-1)
	 * @param i    integer to clip to width
	 * @return     value within the range 0..width-1
	 */
	public int clipToWidth(int i) {
	    return min(max(0, i), width - 1);
	}
	/**
	 * Clips parameter i to the interval (0..width-1)
	 * @param i    integer to clip to height
	 * @return     value within the range 0..height-1
	 */
	public int clipToHeight(int i) {
	    return min(max(0, i), height - 1);
	}

	/**
	 * Displaces a supplied point by a random Gaussian variable.
	 * @param x              x-coordinate
	 * @param y              y-coordinate
	 * @param deviationPx    average deviation, in pixels
	 * @return a displaced coordinate point as a PVector
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
	 * @return                  an array of Gaussian values centered on 1.0
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
	 * Generates an array of Gaussian values for shifting pitch, where 1.0 = no shift.
	 * @param length            length of the returned array
	 * @param deviationPitch    expected average deviation of the pitch around center pitch ratio
	 * @param centerPitch       ratio of center pitch, 1.0 => no change in source
	 * @return                  an array of Gaussian values centered on 1.0
	 */
	float[] generateJitterPitch(int length, float deviationPitch, float centerPitch) {
	    float[] pitch = new float[length];
	    double variance = deviationPitch * deviationPitch;
	    for (int i = 0; i < pitch.length; i++) {
	        pitch[i] = (float) PixelAudio.gauss(centerPitch, variance);
	    }
	    return pitch;
	}

	/**
	 * Initializes a PACurveMaker instance with allPoints as an argument to the factory method 
	 * PACurveMaker.buildCurveMaker() and then fills in PACurveMaker instance variables from 
	 * variables in the calling class (TutorialOneDrawing, here). 
	 */
	public AudioBrushLite initCurveMakerAndAddBrush() {
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
	    cfg.pathMode = PathMode.REDUCED_POINTS; // tutorial default
	    // Default output for newly drawn strokes: SAMPLER (tutorial-centric)
	    AudioBrushLite b = new AudioBrushLite(curveMaker, cfg, BrushOutput.SAMPLER, HopMode.GESTURE);
	    b.cfg.pathMode = defaultPathModeFor(b.output());
	    brushes.add(b);
	    // Optionally auto-select the new brush
	    activeBrush = b;
	    if (this.doPlayOnNewBrush) {
	        if (b.output() == BrushOutput.GRANULAR) {
	            scheduleGranularBrushClick(b);
	        } else {
	            scheduleSamplerBrushClick(b);
	        }
	    }
	    return b;
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
	 * Entry point for drawing brushstrokes on the screen.
	 * TODO distinguish brush types by color.
	 */
	public void drawBrushShapes() {
	    if (brushes == null || brushes.isEmpty()) return;
	    drawBrushes(brushes);
	}

	/**
	 * Draw brushstrokes on the display image.
	 * 
	 * @param list    a list of all the brushstrokes (AudioBrushLite)
	 */
	public void drawBrushes(List<AudioBrushLite> list) {
	    // step through the list of all brushes
	    int readyColor; 
	    int hoverColor; 
	    int selectedColor;
	    for (int i = 0; i < list.size(); i++) {
	        AudioBrushLite b = list.get(i);
	        if (b.output == BrushOutput.GRANULAR) {
	            readyColor = readyBrushColor1;
	            hoverColor = hoverBrushColor1;
	            selectedColor = selectedBrushColor1;
	        } else {
	            readyColor = readyBrushColor2;
	            hoverColor = hoverBrushColor2;
	            selectedColor = selectedBrushColor2;
	        }
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
	        case REDUCED_POINTS: {
	            PACurveUtility.lineDraw(this, cm.getReducedPoints(), lc, w);
	            PACurveUtility.pointsDraw(this, cm.getReducedPoints(), cc, d);
	            break;
	        }
	        case CURVE_POINTS: {
	            PACurveUtility.lineDraw(this, cm.getCurvePoints(), lc, w);
	            PACurveUtility.pointsDraw(this, cm.getCurvePoints(), cc, d);
	            break;
	        }
	        case ALL_POINTS: {
	            PACurveUtility.lineDraw(this, cm.getAllPoints(), lc, w);
	            PACurveUtility.pointsDraw(this, cm.getAllPoints(), cc, d);
	            break;
	        }
	        default: {
	            PACurveUtility.lineDraw(this, cm.getAllPoints(), lc, w);
	            PACurveUtility.pointsDraw(this, cm.getAllPoints(), cc, d);
	            break;
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
	 * Sets epsilon value for the PACurveMaker associated with an AudioBrushLite instance.
	 * 
	 * @param b    an AudioBrushLite instance
	 * @param cs    desired epsilon value to control point reduction
	 */
	public void setBrushCurveSteps(AudioBrushLite b, int cs) {
	    PACurveMaker cm = b.curve();
	    BrushConfig cfg = b.cfg();
	    cfg.curveSteps = cs;
	    cm.setCurveSteps(cs);
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
	    switch (b.cfg().pathMode) {
	    case REDUCED_POINTS:
	      return cm.getReducedPoints();
	    case CURVE_POINTS:
	      return cm.getCurvePoints();
	    case ALL_POINTS: default:
	      return cm.getAllPoints();
	    }
	}

	/**
	 * Get a GestureSchedule (points + timing) for an AudioBrushLite instance.
	 * @param b    an AudioBrushLite instance
	 * @return     GestureSchedule for the current pathMode of the brush
	 */
	public GestureSchedule getScheduleForBrush(AudioBrushLite b) {
	    GestureSchedule sched;
	    switch (b.cfg().pathMode) {
	    case REDUCED_POINTS: {
	        sched = b.curve.getReducedSchedule(b.cfg.rdpEpsilon);
	        break;
	    }
	    case CURVE_POINTS: {
	        sched = b.curve.getCurveSchedule(b.cfg.rdpEpsilon, b.cfg.curveSteps, isAnimating);
	        break;
	    }
	    case ALL_POINTS: default: {
	        sched = b.curve.getAllPointsSchedule();
	        break;
	    }
	    }
	    return sched;
	}

	/**
	 * @param b    an AudioBrushLIte instance
	 * @return     a GestureSchedule filtered by boundsPolicy to provide only in-bounds points
	 */
	GestureSchedule getPlaybackScheduleForBrush(AudioBrushLite b) {
	    GestureSchedule sched = getScheduleForBrush(b);
	    if (b.output == BrushOutput.SAMPLER) {
	        // divide gesture duration by number of gesture intervals and multiply result a fixed value
	        envDuration = isAdjustEnvelope ? computeEnvDurationMs(sched, defaultEnv.toString(), noteDuration ) : noteDuration;
	        println("-- envelope duration = "+ envDuration);
	    }
	    return boundsPolicy.applySchedule(sched);
	}


	/**
	 * @param sched         a {@code GestureSchedule} to access for calculating an envelope duration
	 * @param envName       name of an envelope preset
	 * @param fallbackMs    default duration in milliseconds
	 * @return calculated sample length in samples of an envelope
	 */
	int computeEnvDurationMs(GestureSchedule sched, String envName, int fallbackMs) {
	    int n = sched.points.size();
	    if (n < 2) return fallbackMs;
	    float avgStepMs = sched.durationMs() / (float)(n - 1);
	    float factor;
	    switch (envName) {
	        case "Pluck":
	        case "Percussion":
	            factor = 4.0f;
	            break;
	        case "Soft":
	        case "Fade":
	            factor = 3.0f;
	            break;
	        case "Swell":
	        case "Pad":
	            factor = 2.0f;
	            break;
	        default:
	            factor = 3.0f;
	    }
	    int minEnvMs = envMinDurationMs;
	    int maxEnvMs = envMaxDurationMs;
	    return PApplet.constrain(Math.round(avgStepMs * factor), minEnvMs, maxEnvMs);
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
	    GestureSchedule sched = getPlaybackScheduleForBrush(b);
	    storeSamplerCurveTL(b, sched, millis() + 10);
	}

	/**
	 * Store scheduled sampler synth / animation events for future activation. 
	 * @param sched        a GestureSchedule (points + timing for a brush)
	 * @param startTime    time to start a series of events
	 */
	public synchronized void storeSamplerCurveTL(AudioBrushLite b, GestureSchedule sched, int startTime) {
	    int i = 0;
	    startTime = millis() + 5;
	    // we store the point and the current time + time offset, where timesMs[0] == 0
	    for (PVector loc : sched.points) {
	        int x = Math.round(loc.x);
	        int y = Math.round(loc.y);
	        int t = startTime + Math.round(sched.timesMs[i++]);
	        int pos = getSamplePos(x, y);
	        int len = calcSampleLen();
	        int d = 200;
	        float gain = samplerGain;
	        float pitch = b.pitchRatio;
	        ADSRParams env = defaultEnv.copy();
	        float pan = map(x, 0, width - 1, -0.875f, 0.875f);
	        this.samplerTimeLocs.add(new SamplerBrushEvent(x, y, t, pos, len, gain, pitch, env, pan));
	    }
	    Collections.sort(samplerTimeLocs);
	}

	/**
	 * Execute audio / animation events for Sampler brushstrokes.
	 */
	public synchronized void runSamplerBrushEvents() {
	    if (samplerTimeLocs == null || samplerTimeLocs.isEmpty()) return;
	    int currentTime = millis();
	    int durationMs = 200;
	    samplerTimeLocs.forEach(stl -> {
	        if (stl.eventTimeMs() < currentTime) {
	            // sched points from storeSamplerCurveTL are already in bounds
	            int sampleX = Math.round(stl.getX());
	            int sampleY = Math.round(stl.getY());
	            // playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pitch, float pan)
	            playSample(stl.samplePos, stl.durationSamples, stl.gain, stl.env, stl.pitchRatio, stl.pan);
	            pointTimeLocs.add(new TimedLocation(sampleX, sampleY, durationMs + millis()));
	            stl.setStale(true);
	        } 
	        else {
	            return;
	        }
	    });
	    samplerTimeLocs.removeIf(SamplerBrushEvent::isStale);
	}

	// *** WindowedBuffer support *** //  
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
	    boolean isGesture = (b.hopMode() == HopMode.GESTURE);
	    GestureGranularParams gParams = isGesture ? gParamsGesture : gParamsFixed;
	    GestureSchedule sched = getPlaybackScheduleForBrush(b); 
	    playGranularGesture(backingGranularSignal(), sched, gParams, b.pitchRatio());
	    storeGranularCurveTL(sched, millis() + 10, isGesture);
	}  

	/**
	 * Store scheduled granular synth / animation events for future activation. 
	 * @param sched        a GestureSchedule (points + timing for a brush)
	 * @param startTime    time to start a series of events
	 * @param isGesture    is the schedule for a GESTURE or FIXED granular event (ignored)
	 */
	public synchronized void storeGranularCurveTL(GestureSchedule sched, int startTime, boolean isGesture) {
	    int i = 0;
	    int hopMs = (int) Math.round(AudioUtility.samplesToMillis(hopSamples, sampleRate));
	    int durMsFixed = (int) Math.round(AudioUtility.samplesToMillis(granSamples, sampleRate)); // or hopMs if you prefer
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
	    // grainLocsArray.removeIf(TimedLocation::isStale);    // not necessary if we remove in loop
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
	 * -- TODO drop, this used to be the "emergency off" switch for runaway audio processing
	 */
	@Deprecated
	public void reset() {

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
	/*                           UTILITY                              */
	/*                                                                */
	/*----------------------------------------------------------------*/
	

}
