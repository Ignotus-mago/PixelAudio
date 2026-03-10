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
 * 
 */

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

Minim minim;          // library that handles audio 
AudioOutput audioOut;      // line out to sound hardware
boolean isBufferStale = false;  // flags that audioBuffer needs to be reset, not used in TutorialOneDrawing
float sampleRate = 44100;       // target audio engine rate used to configure audioOut
float fileSampleRate;           // sample rate of most recently opened file (before resampling)
float bufferSampleRate;         // sample rate of playBuffer, usually == audioOut.sampleRate()
float[] audioSignal;      // the audio signal as an array of floats
MultiChannelBuffer playBuffer;  // a buffer for playing the audio signal
int samplePos;                  // an index into the audio signal, selected by a mouse click on the display image
int audioLength;        // length of the audioSignal, same as the number of pixels in the display image

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
public AudioBrushLite activeBrush = null;           // "selected" brush (optional but useful)

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
public ArrayList<TimedLocation> samplerTimeLocs;    // a list of timed events for Sampler brushes
ArrayList<TimedLocation> pointTimeLocs;             // a list of timed events for mouse clicks
ArrayList<TimedLocation> grainTimeLocs;             // a list of timed events for Granular brushes
int eventStep = 90;                                 // ms spacing for constant interval sampler "path events"
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
    } else {
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
