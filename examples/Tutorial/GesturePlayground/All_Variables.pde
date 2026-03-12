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
boolean isBlending = false;


/* ------------------------------------------------------------------ */
/*                          AUDIO VARIABLES                           */
/* ------------------------------------------------------------------ */

/** Minim audio library */
Minim minim;          // library that handles audio
AudioOutput audioOut;      // line out to sound hardware
boolean isBufferStale = false;  // flags that audioBuffer needs to be reset
float sampleRate = 44100;       // target audio engine rate used to configure audioOut
float fileSampleRate;           // sample rate of most recently opened file (before resampling)
float bufferSampleRate;         // sample rate of playBuffer, usually == audioOut.sampleRate()
float[] audioSignal;      // the audio signal as an array of floats
MultiChannelBuffer playBuffer;  // a buffer for playing the audio signal
int samplePos;                  // an index into the audio signal, selected by a mouse click on the display image
int audioLength;        // length of the audioSignal, same as the number of pixels in the display image

// ADSR and its parameters
ADSRParams samplerEnv;      // good old attack, decay, sustain, release for Sampler instrument
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
float samplerGain = 0.95f;      // linear gain setting for Sampler instrument
float samplerPointGain = 0.75f;   // linear gain for Sampler instrument point events
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
public float epsilon = 4.0f;            // controls how much reduction is applied to points

public PVector currentPoint = new PVector(-1, -1);  // point to add to brushstroke points
public ArrayList<PVector> allPoints;                // in-progress brushstroke points
public ArrayList<Integer> allTimes;                 // in-progress brushstroke times
public int startTime;                               // in-progress brushstroke start time

public int dragColor = color(233, 199, 89, 128);  // color for initial drawing
public float dragWeight = 8.0f;            // weight (brush diameter) of initial line drawing

public int polySteps = 5;              // number of steps in polygon representation of a Bezier curve

// ====== Visual styling for brushstrokes ======

// the next series of colors seems to be all we need
int readyBrushColor1 = color(34, 55, 89, 178);    // color for a brushstroke when available to be clicked
int hoverBrushColor1 = color(55, 89, 144, 178);     // color for brushstroke under the cursor
int selectedBrushColor1 = color(199, 123, 55, 233);  // color for the selected (clicked or currently active for editing) brushstroke
int readyBrushColor2 = color(55, 34, 89, 178);    // color for a brushstroke when available to be clicked
int hoverBrushColor2 = color(89, 55, 144, 178);     // color for brushstroke under the cursor
int selectedBrushColor2 = color(123, 199, 55, 233);  // color for the selected (clicked or currently active for editing) brushstroke

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

enum DrawingMode {
  DRAW_EDIT_SAMPLER, DRAW_EDIT_GRANULAR, PLAY_ONLY
}
DrawingMode drawingMode = DrawingMode.DRAW_EDIT_GRANULAR;

static final int CURVE_STEPS_HARD_MAX = 128;   // Curve steps GUI slider max
static final int CURVE_STEPS_SAFE_MAX = 32;    // Curve steps preferred max threshold
static final int CURVE_STEPS_FLOOR = 4;        // don’t go too low

/* ------------------------------------------------------------------ */
/*                    TIME/LOCATION/ACTION VARIABLES                  */
/* ------------------------------------------------------------------ */

/**
 * Future Development:
 * Barebones interface for "something that happens at a certain point",
 * and then in AudioScheduler the time-when-something-happens gets connected to
 * the-room-where-it-happens and the entire cast of Hamilton steps in, if you let them.
 */
interface Happening {
  int x();
  int y();
}

AudioScheduler<Happening> audioSched = new AudioScheduler<>();

// Future development: Tracks current absolute sample position (block start) on the audio thread
private final AtomicLong audioBlockStartSample = new AtomicLong(0);

// Optional, Future development: if you want to schedule “now”, this is a safe estimate of the next block start.
private final AtomicLong audioNextBlockStartSample = new AtomicLong(0);

// grain density mod
float hopScale = 1.0f;
// optimal number of grains for specified time
int optGrainCount = 2;

int eventStep = 90;    // milliseconds between events, formerly used by Sampler instrument

/* ------------------------------------------------------------------ */
/*                       DEBUGGING & LOCAL SETTINGS                   */
/* ------------------------------------------------------------------ */

boolean isVerbose = true;
boolean isDebugging = false;

// system-specific path to example files data, in Processing this should work for PixelAudio Tutorial files
// daPath = sketchPath("") + "../../examples_data/";
String daPath;    
String daFilename = "audioBlend.wav";

boolean isRunWordGame = false;    // load DeadBodyWorkFlow audio at 48KHz sampleRate
boolean doPlayOnDraw = false;                  // play audio when a curve is drawn
