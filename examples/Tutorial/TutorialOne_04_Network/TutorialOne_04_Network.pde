/**
 * NETWORKING WITH UDP
 *
 * TutorialOne_04_Network is a copy of TutorialOne_03_Drawing with networking features added.
 * Refer to the notes for TutorialOne_03_Drawing for information about drawing and audio synthesis
 * in TutorialOne_04_Network. Here, we're going to explain just the networking features.
 *
 * TutorialOne_04_Network is designed to work with a Max patch, simpleAudioIO.maxpat, which is
 * included in the example code for PixelAudio. The simpleAudioIO patch is designed to process
 * audio output from TutorialOne_04_Network and to respond to UDP messages from TutorialOne_04_Network.
 * We'll discuss the audio setup first, for MacOS (something similar can be done in the Windows OS).
 * You can use the audio signal flow without using UDP. It's handy for situations where you want to
 * add a layer of audio processing and it will function with many audio applications besides Max.
 * Next, we'll discuss UDP network messaging. Finally we'll look at the PANetworkClientINF interface
 * and its implementation in NetworkDelegate, an inner class of TutorialOne_04_Network.
 *
 * Audio Setup with Blackhole in MacOS
 * ===================================
 *
 * Using the System Settings for Sound in MacOS and the BlackHole audio routing tool
 * (https://existential.audio/blackhole/), you can set up a wide range of inputs and outputs,
 * depending on your system software and hardware. Here's how to route between this sketch
 * and Max.
 *
 * Open simpleAudioIO.maxpat in Max
 *   1. Open MacOS System Settings control panel and select the Sound tab.
 *   2. In the Input and Output section, select BlackHole 16ch as the Output.
 *   3. In the Max Audio Status control panel, set Input Device to BlackHole 16ch.
 *   4. If you have external audio hardware, set that to your output.
 *   5. Turn on audio processing in Max.
 *   6. Run TutorialOne_04_Network and play some sounds.
 *
 * UDP: User Datagram Protocol
 * ===========================
 *
 * UDP (User Datagram Protocol) is a network communications protocol that is apt for
 * sending relatively short, real-time messages over a network. In Processing, it is implemented
 * with external libraries. I'm using the oscP5 library by Andreas Schlegel, which is included
 * in Processing's Contributed Libraries. You can install it with the Sketch -> Import Library...
 * -> Manage Libraries... menu command in Processing. UDP requires an IP address and a port
 * number to enable communications. In this example, we use 127.0.0.1 as the IP address.
 * The address 127.0.0.1 is the standard address for IPv4 loopback traffic, i.e., for services
 * running on the host computer. If you are communicating with a remote computer, you will need
 * the IP address for that computer and your own, either on the local area network or on the
 * internet, if the addresses are static. We're using ports 7400 and 7401. TutorialOne_04_Network
 * sends messages out on port 7400. The Max application simpleAudioIO receives messages on 7400,
 * using the udpreceive objects. It sends messages to TutorialOne_04_Network via IP address
 * 127.0.0.1 on port 7401 using the udpsend object. TutorialOne_04_Network receives messages
 * over port 7401. See the examples for oscP5 in Processing for more information.
 *
 * PANetworkClientINF and NetworkDelegate
 * ======================================
 *
 * Since UDP communications are independent of the PixelAudio library, they are implemented in the
 * example code, not in the library. The PANetworkClientINF interface is included in the example software.
 * It defines the methods that an implementing class must provide:
 *   1. public PixelAudioMapper getMapper();
 *   2. public int playSample(int samplePos);
 *   3. public void playPoints(ArrayList pts);
 *   4. public void parseKey(char key, int keyCode);
 *   5. public void controlMsg(String control, float val);
 *   6. public PApplet getPApplet();
 *
 * If you are running Processing in an IDE like Eclipse, the PApplet class you create should implement
 * PANetworkClientINF and provide these methods. In Processing, the PApplet host class doesn't provide
 * a way to implement an interface directly, but you can still provide all the required methods and
 * create a network delegate to handle UDP communications. In TutorialOne_04_Network we use the
 * NetworkDelegate subclass. In Eclipse, NetworkDelegate's constructors require a
 * PANetworkClientINF object as their first argument. In Processing, the constructors should require
 * an instance of your PApplet class, which is indicated with the name of your application.
 *
 *   -- Eclipse:    NetworkDelegate(PANetworkClientINF app, String remoteFromAddr, String remoteToAddr, int inPort, int outPort)
 *   -- Processing: NetworkDelegate(TutorialOne_04_Network app, String remoteFromAddr, String remoteToAddr, int inPort, int outPort)
 *
 * Messages and Methods
 * ====================
 *
 * To find the points at which networking code is implemented in methods in this sketch,
 * search for "// *****]]] NETWORKING [[[***** //".
 * The NetworkDelegate class provides a number of messaging methods, each prefaced with "oscSend".
 * For receiving remote messages, it calls oscP5's "plug" method (in initOscPlugs()) to link
 * incoming messages to local methods. In addition, it provides some boolean toggles, such as
 * isNetSendDrawingPoints, to turn features on or off at various points in the
 * TutorialOne_04_Network code--if you aren't going to use data, don't send it.
 * In this sketch, we concentrate on:
 *   -- sending mouse coordinates and audio buffer position to simpleAudioIO
 *   -- turning Max reverb on or off with the ']' or '[' keys (we've left '{' and '}' open in simpleAudioIO)
 *   -- changing Max reverb settings with 'q' or 'Q' keys
 *
 * See NetworkDelegate comments for more information. There is quite a lot more that can be accomplished
 * with UDP communications. In my collaborations with composer Christopher Walczak, it's been very useful
 * in performances where we use mouse position to control objects in Max or trigger brushstrokes to send
 * control values to Max. It's not an integral part of PixelAudio, but I will post useful examples as
 * I develop them.
 *
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
 * Press '.' to increment epsilon value of current brush (reduced points decrease).
 * Press ',' to decrement epsilon value of current brush (reduced points increase).
 * Press '>' to increment curve steps value of current brush.
 * Press '<' to decrement curve steps value of current brush.
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
 * Press ']' to send UDP message to Max (simpleAudioIO.maxpat): reverb ON.
 * Press '[' to send UDP message to Max (simpleAudioIO.maxpat): reverb OFF.
 * Press '}' to send UDP message to Max (simpleAudioIO.maxpat): unused.
 * Press '{' to send UDP message to Max (simpleAudioIO.maxpat): unused.
 * Press 'q' to send UDP message to Max (simpleAudioIO.maxpat): small reverb settings.
 * Press 'Q' to send UDP message to Max (simpleAudioIO.maxpat): big reverb settings.
 * Press 'h' or 'H' to show help message in the console.
 *
 */

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
import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import net.paulhertz.pixelaudio.granular.*;
import net.paulhertz.pixelaudio.sampler.*;

//audio library
import ddf.minim.*;

//video export library
import com.hamoid.*;


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

// system-specific path to example files data
// in Processing, for PixelAudio Tutorial examples, use this in setup():
// daPath = sketchPath("") + "../../examples_data/";
String daPath = "/Users/paulhz/Code/Workspace/PixelAudio/examples/examples_data/";


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

Minim minim;          // library that handles audio
AudioOutput audioOut;      // line out to sound hardware
float outputGain = 0.0f;        // gain setting for audio output, decibels
boolean isBufferStale = false;  // flags that audioBuffer needs to be reset, not used in TutorialOneDrawing
float sampleRate = 48000;       // target audio engine rate used to configure audioOut
float fileSampleRate;           // sample rate of most recently opened file (before resampling)
float bufferSampleRate;         // sample rate of playBuffer: fileSampleRate when not resampled, audioOut.sampleRate() when resampled
boolean doResample = true;      // if true, resample audio from files whose sampling rate != audioOut.sampleRate()
float[] audioSignal;      // the audio signal as an array of floats
MultiChannelBuffer playBuffer;  // a buffer for playing the audio signal
int samplePos;                  // an index into the audio signal, selected by a mouse click on the display image
int audioLength;        // length of the audioSignal, same as the number of pixels in the display image

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
int poolSize = 8;               // number of sampler instruments for polyphony
int samplerMaxVoices = 256;     // number of voices for each sampler instrument

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

int readyBrushColor1 = color(34, 55, 89, 178);    // color for a brushstroke when available to be clicked
int hoverBrushColor1 = color(55, 89, 144, 178);     // color for brushstroke under the cursor
int selectedBrushColor1 = color(199, 123, 55, 233);  // color for the selected (clicked or currently active for editing) brushstroke
int readyBrushColor2 = color(55, 34, 89, 178);    // color for a brushstroke when available to be clicked
int hoverBrushColor2 = color(89, 55, 144, 178);     // color for brushstroke under the cursor
int selectedBrushColor2 = color(123, 199, 55, 233);  // color for the selected (clicked or currently active for editing) brushstroke

int lineColor = color(233, 220, 178);               // brushstroke interior line color
int dimLineColor = color(178, 168, 136);            // brushstroke interior line color, dimmed
int circleColor = color(233, 178, 144);             // brushstroke interior point color
int dimCircleColor = color(178, 168, 136);          // brushstroke interiro point color, dimmed

// TimedLocation events
public ArrayList<SamplerBrushEvent> samplerTimeLocs;    // a list of timed events for Sampler brushes
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

public enum PathMode {
  ALL_POINTS, REDUCED_POINTS, CURVE_POINTS
}    // select model for brush shape in PACurveMaker
public enum BrushOutput {
  SAMPLER, GRANULAR
}                        // select audio synthesis model
public enum HopMode {
  GESTURE, FIXED
}                               // select audio gesture sequence timing model

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
 * A light-weight version of the AudioBrush class for combining gestures and
 * audio synthesis. AudioBrushLite combines PACurveMaker gesture and curve
 * modeling with basic audio synthesis parameters. AudioBrush, introduced in
 * TutorialOne_05_GesturePlayground, exposes most of the parameters used for
 * granular synthesis in PixelAudio, along with a many of the brush and gesture
 * settings available in PACurveMaker.
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
  public PACurveMaker curve() {
    return curve;
  }
  public BrushConfig cfg() {
    return cfg;
  }
  public BrushOutput output() {
    return output;
  }
  public void setOutput(BrushOutput out) {
    this.output = out;
  }
  public float pitchRatio() {
    return pitchRatio;
  }
  public void setPitchRatio(float newRatio) {
    this.pitchRatio = newRatio;
  }
  public HopMode hopMode() {
    return hopMode;
  }
  public void setHopMode(HopMode hop) {
    this.hopMode = hop;
  }
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

// *****]]] NETWORKING [[[***** //
NetworkDelegate nd;
boolean isUseNetworkDelegate = false;
boolean isNetSendDrawingPoints = true;
boolean isNetSendOutsideBrushPoints = true;
boolean isNetSendBrushTriggers = false;
boolean isNetSendFileInfo = false;
boolean isNetSendGestures = false;

// application settings

boolean doPlayOnNewBrush = true;
boolean shiftIsDown = false;


// ---------------- APPLICATION ---------------- //

public void settings() {
  size(3 * genWidth, 2 * genHeight);
}

public void setup() {
  daPath = sketchPath("") + "../../examples_data/";
	// set a standard animation framerate
	frameRate(24);
	// 1. Initialize our library.
	pixelaudio = new PixelAudio(this);
	// 2. create a PixelMapGen object with dimensions equal to the display window.
	//   the call to "hilbertLoop3x2(genWidth, genHeight)" produces a MultiGen that is 3 * genWidth x 2 * genHeight,
	//   where genWidth == genHeight and genWidth is a power of 2 (a restriction on Hilbert curves)
	//
	// multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
	//
	//   Here's an alternative multigen that is good for visualizing audio location within the image
	//   It reads left to right, top to bottom, like a musical score. I've divided it 12 x 8, but 6 x 4 is good, too.
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
	// 7. set up networking
    // *****]]] NETWORKING [[[***** //
	isUseNetworkDelegate = true;
	initNetwork();
	// 8. output a help message to the console
	showHelp();
	// preload files, parameters will vary with the system where we're running
	preloadFiles(daPath, "Saucer_mixdown.wav");    // handy when debugging, too
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
  samplerTimeLocs = new ArrayList<>();   // capture timing data when drawing
}

	/**
	 * Sets up network for UDP communication.
	 */
void initNetwork() {
  // *****]]] NETWORKING [[[***** //
  if (isUseNetworkDelegate) {
    String remoteAddress = "127.0.0.1";
    nd = new NetworkDelegate(this, remoteAddress, remoteAddress, 7401, 7400);
    nd.oscSendClear();
  }
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
  } else {
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
  } else {
    // click outside any brush is handled here
    handleClickOutsideBrush(clipToWidth(mouseX), clipToHeight(mouseY));
  }
  // *****]]] NETWORKING [[[***** //
  sendCoordsUDP(mouseX, mouseY);
}

/**
 * Send coordinates and corresponding sample position through network connection.
 * Coords will be clipped to display and sent with sample index if isNetSendOutsideBrushPoints == true.
 */
public void sendCoordsUDP(int x, int y) {
  x = clipToWidth(x);
  y = clipToHeight(y);
  int pos = getSamplePos(x, y);
  if (nd != null && isNetSendOutsideBrushPoints) nd.oscSendMouseClicked(x, y, pos);
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
  } else {
    if (keyCode == UP) {
      adjustAudioGain(shiftIsDown ? 3.0f : 1.0f);
      println("---- audio gain is "+ nf(audioOut.getGain(), 0, 2) +"dB");
    } else if (keyCode == DOWN) {
      adjustAudioGain(shiftIsDown ? -3.0f : -1.0f);
      println("---- audio gain is "+ nf(audioOut.getGain(), 0, 2) +"dB");
    } else if (keyCode == RIGHT) {
      if (!shiftIsDown) {
        adjustPoolGain(3.0f);
        println("---- pool gain is "+ nf(pool.getGainDb(), 0, 2) +"dB");
      } else {
        adjustGranGain(3.0f);
        println("---- granular gain is "+ nf(gDir.getInstrument().getGlobalGainDb(), 0, 2) +"dB");
      }
    } else if (keyCode == LEFT) {
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
    } else {
      handleClickOutsideBrush(clipToWidth(mouseX), clipToHeight(mouseY));
      // *****]]] NETWORKING [[[***** //
      sendCoordsUDP(mouseX, mouseY);
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
  case '.': // increment epsilon value of current brush (reduced points decrease)
    if (hoverBrush != null) {
      float e = hoverBrush.curve.getEpsilon();
      e = e < 32 ? e + 1 : e;
      setBrushEpsilon(hoverBrush, e);
      println("-- epsilon for current brush is "+ hoverBrush.curve.getEpsilon());
    }
    break;
  case ',': // decrement epsilon value of current brush (reduced points increase)
    if (hoverBrush != null) {
      float e = hoverBrush.curve.getEpsilon();
      e = e > 1 ? e - 1 : e;
      setBrushEpsilon(hoverBrush, e);
      println("-- epsilon for current brush is "+ hoverBrush.curve.getEpsilon());
    }
    break;
  case '>': // increment curve steps value of current brush
    if (hoverBrush != null) {
      int cs = hoverBrush.curve.getCurveSteps();
      cs = cs < 32 ? cs + 1 : cs;
      setBrushCurveSteps(hoverBrush, cs);
      println("-- curveSteps for current brush is "+ hoverBrush.curve.getCurveSteps());
    }
    break;
  case '<': // decrement curve steps value of current brush
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
  case 'L':
  case 'l': // determine whether to load a new file to both image and audio or not
    isLoadToBoth = !isLoadToBoth;
    msg = isLoadToBoth ? "loads to both audio and image. " : "loads only to selected format. ";
    println("---- isLoadToBoth is "+ isLoadToBoth +", opening a file "+ msg);
    break;
  case 'o':
  case 'O': // open an audio or image file and load to image or audio buffer or both
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
    } else {
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
    // *****]]] NETWORKING [[[***** // key commands for networking
  case ']': // send UDP message to Max (simpleAudioIO.maxpat): reverb ON
    if (nd != null) nd.oscSendOnOff(1, true);
    break;
  case '[': // send UDP message to Max (simpleAudioIO.maxpat): reverb OFF
    if (nd != null) nd.oscSendOnOff(1, false);
    break;
  case '}': // send UDP message to Max (simpleAudioIO.maxpat): unused
    if (nd != null) nd.oscSendOnOff(2, true);
    break;
  case '{': // send UDP message to Max (simpleAudioIO.maxpat): unused
    if (nd != null) nd.oscSendOnOff(2, false);
    break;
  case 'q': // send UDP message to Max (simpleAudioIO.maxpat): small reverb settings
    if (nd != null) {
      nd.oscSendSetnum(1, 0.125f);
      nd.oscSendSetnum(2, 2048.0f);
      nd.oscSendSetnum(3, 0.125f);
      nd.oscSendSetnum(4, 2560.0f);
    }
    break;
  case 'Q': // send UDP message to Max (simpleAudioIO.maxpat): big reverb settings
    if (nd != null) {
      nd.oscSendSetnum(1, 0.3f);
      nd.oscSendSetnum(2, 4096.0f);
      nd.oscSendSetnum(3, 0.3f);
      nd.oscSendSetnum(4, 5120.0f);
    }
    break;
  case 'h':
  case 'H': // show help message in the console
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
  println(" * Press '.' to increment epsilon value of current brush (reduced points decrease).");
  println(" * Press ',' to decrement epsilon value of current brush (reduced points increase).");
  println(" * Press '>' to increment curve steps value of current brush.");
  println(" * Press '<' to decrement curve steps value of current brush.");
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
  println(" * Press ']' to send UDP message to Max (simpleAudioIO.maxpat): reverb ON.");
  println(" * Press '[' to send UDP message to Max (simpleAudioIO.maxpat): reverb OFF.");
  println(" * Press '}' to send UDP message to Max (simpleAudioIO.maxpat): unused.");
  println(" * Press '{' to send UDP message to Max (simpleAudioIO.maxpat): unused.");
  println(" * Press 'q' to send UDP message to Max (simpleAudioIO.maxpat): small reverb settings.");
  println(" * Press 'Q' to send UDP message to Max (simpleAudioIO.maxpat): big reverb settings.");
  println(" * Press 'h' or 'H' to show help message in the console.");
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
