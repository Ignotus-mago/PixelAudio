import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

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
import net.paulhertz.pixelaudio.schedule.*;
import net.paulhertz.pixelaudio.granular.*;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig.*;
import net.paulhertz.pixelaudio.io.*;
import net.paulhertz.pixelaudio.sampler.*;

// UDP / OSC network communications
import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;


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
Minim minim;            // library that handles audio
AudioOutput audioOut;        // line out to sound hardware
float sampleRate = 48000;         // target audio engine rate used to configure audioOut
float fileSampleRate;             // sample rate of most recently opened file (before resampling)
float bufferSampleRate;           // sample rate of playBuffer, usually == audioOut.sampleRate()
boolean doResample = true;        // if true, resample audio from files whose sampling rate != audioOut.sampleRate()
float[] audioSignal;        // the audio signal as an array of floats
MultiChannelBuffer playBuffer;    // a buffer for playing the audio signal
int samplePos;                    // an index into the audio signal, selected by a mouse click on the display image
int audioLength;          // length of the audioSignal, same as the number of pixels in the display image

// ====== Envelopes ====== //

ADSRParams samplerEnv;        // good old attack, decay, sustain, release for individual Sampler events
ADSRParams granularEnv;           // envelope for a sequence of Granular events
final int noteDuration = 1000;    // average sample synth note duration, milliseconds, for reference
int envDuration = noteDuration;   // current envelope duration
boolean isAdjustEnvelope = true;  // if true, adjust envelope duration based on gesture duration
float overlapFactor = 3.0f;       // envelope overlap
int envMinDurationMs = 40;        // min envelope duration, milliseconds
int envMaxDurationMs = 1280;      // max envelope duration, milliseconds

// ====== Sampler Synth ====== //

int samplelen;                    // calculated sample synth note length, samples
float samplerGain = 0.5f;         // linear gain setting for Sampler instrument
float samplerPointGain = 0.75f;   // linear gain for Sampler instrument point events
float outputGain = 0.0f;          // gain setting for audio output, decibels
boolean isMuted = false;          // global muting
PASamplerInstrumentPool pool;     // an allocation pool of PASamplerInstruments
int poolSize = 16;                 // number of sampler instruments for polyphony
int sMaxVoices = 256;             // number of voices to allocate to pool or synth

// ====== Granular Synth ====== //

public float[] granSignal;
public PAGranularInstrument gSynth;
public int curveSteps = 16;
public int granLength = 4096;
public int granHop = 1024;
public int gMaxVoices = 512;

String currentGranStatus = "";

public PAGranularInstrumentDirector gDir;   // director of granular events
public float granularGain = 1.0f;           // linear gain for a granular gesture event
public float granularPointGain = 0.9f;      // linear gain for a granular point event ("granular burst")
// parameters for granular synthesis
boolean useShortGrain = true;               // default to short grains, if true
int longSample = 4096;                      // number of samples for a moderately long grain
int shortSample = 512;                      // number of samples for a relatively short grain
int granSamples = useShortGrain ? shortSample : longSample;    // number of samples in a grain window
int hopSamples = granSamples/4;             // number of samples between each grain in a granular burst
GestureGranularParams gParamsFixed;         // granular parameters when hopMode == HopMode.FIXED, for granular point event
GestureGranularParams gParamsDraw;          // granular parameters when drawing
boolean useLongBursts = false;              // controls the number of burst grain in a point event or gesture event
int maxBurstGrains = 4;
int burstGrains = 1;


/* ------------------------------------------------------------------ */
/*                   ANIMATION AND VIDEO VARIABLES                    */
/* ------------------------------------------------------------------ */

// This example concentrates on audio synthesis, we don't do pixel-shift
// animation, but the hooks for handling it are all here

int shift = 1;                       // number of pixels to shift the animation
int leap = 65536;                    // a big shift
int totalShift = 0;                  // cumulative shift
boolean isAnimating = false;         // do we run animation or not?
boolean oldIsAnimating;              // keep track of animation state when opening a file
boolean isTrackMouse = false;        // if true, drag the mouse to change shift value
// animation variables
int animSteps = 720;                 // how many steps in an animation loop
boolean isRecordingVideo = false;    // are we recording? (only if we are animating)
int videoFrameRate = 120;            // fps, frames per second
int videoSteps = 720;                // TODO seems redundant
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
public float epsilon = 4.0f;                        // controls how much reduction is applied to points

public PVector currentPoint = new PVector(-1, -1);  // point to add to brushstroke points
public ArrayList<PVector> allPoints;                // in-progress brushstroke points
public ArrayList<Integer> allTimes;                 // in-progress brushstroke times
public int startTime;                               // in-progress brushstroke start time

public int dragColor = color(220, 178, 152, 128);   // color for initial drawing
public float dragWeight = 8.0f;                     // weight (brush diameter) of initial line drawing

public int polySteps = 5;                           // number of steps in polygon representation of a Bezier curve

// ====== Visual styling for brushstrokes ======

// the next series of colors seems to be all we need
int readyGranColor = color(34, 55, 89, 178);        // color for a brushstroke when available to be clicked
int hoverGranColor = color(55, 89, 144, 178);       // color for brushstroke under the cursor
int activeGranColor = color(199, 123, 55, 233);     // color for the selected (clicked or currently active for editing) brushstroke
int readySamplerColor = color(55, 34, 89, 178);     // color for a brushstroke when available to be clicked
int hoverSamplerColor = color(89, 55, 144, 178);    // color for brushstroke under the cursor
int activeSamplerColor = color(123, 199, 55, 233);  // color for the selected (clicked or currently active for editing) brushstroke

int dimmedBrushColor = color(55, 55, 55, 178);      // color for a brush that cannot be selected

int circleColor = color(233, 178, 144);             // interior circles in brushstrokes
int dimCircleColor = color(178, 168, 136);
int lineColor = color(233, 220, 178);               // interior lines in brushStrokes
int dimLineColor = color(178, 168, 136);
int animatedCircleColor = color(233, 220, 199);     // color for animated circles when playing audio

boolean isIgnoreOutsideBounds = true;               // when drawing, clip or ignore points outside display bounds


/* ------------------------------------------------------------------ */
/*                  INTERACTION CLASSES AND VARIABLES                 */
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
private AudioBrush hoverBrush;               // the brush the mouse is over, if there is one
private int hoverIndex;                      // index of hoverBrush in a brush list
private AudioBrush activeBrush;              // brush slotted for selection/highlight/editing in GUI

// AudioBrushes for use with PAGranularInstrument
ArrayList<GranularBrush> granularBrushes;
GranularBrush activeGranularBrush = null;
int activeGranularIndex = -1;                // index of active brush in granularBrushes

// AudioBrushes for use with PASamplerInstrument
ArrayList<SamplerBrush> samplerBrushes;
SamplerBrush activeSamplerBrush;             // the currently active PACurveMaker, collecting points as user drags the mouse
int activeSamplerIndex = 0;                  // index of active brush in samplerBrushes

// TimedLocation events
ArrayList<TimedLocation> pointTimeLocs;             // a list of timed events for mouse clicks
ArrayList<SamplerBrushEvent> samplerBrushEvents;    // a list of sampler brush events
ArrayList<TimedLocation> grainTimeLocs;             // a list of timed events for Granular brushes
final Object pointTimeLocsLock   = new Object();    // dedicated lock for pointTimeLocs access
final Object samplerBrushEventsLock = new Object(); // dedicated lock for samplerBrushEvents
final Object grainTimeLocsLock   = new Object();    // dedicated lock for grainTimeLocsLock access

boolean pointEventUseSampler = true;
boolean runningFadeOut = true;

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

enum DrawingMode {
  DRAW_EDIT_SAMPLER, DRAW_EDIT_GRANULAR, PLAY_ONLY
}

DrawingMode drawingMode = DrawingMode.DRAW_EDIT_GRANULAR;

static final int CURVE_STEPS_HARD_MAX = 128;   // Curve steps GUI slider max
static final int CURVE_STEPS_SAFE_MAX = 32;    // Curve steps preferred max threshold
static final int CURVE_STEPS_FLOOR = 4;        // don't go too low


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

// Optional, Future development: if you want to schedule "now", this is a safe estimate of the next block start.
private final AtomicLong audioNextBlockStartSample = new AtomicLong(0);

// grain density mod
float hopScale = 1.0f;
// optimal number of grains for specified time
int optGrainCount = 2;

int eventStep = 90;    // milliseconds between events, formerly used by Sampler instrument

/* ------------------------------------------------------------------ */
/*                              JSON FILE IO                          */
/* ------------------------------------------------------------------ */

boolean isSaveConfig = true;


/* ------------------------------------------------------------------ */
/*                           PLAY WHILE DRAWING                       */
/* ------------------------------------------------------------------ */

// ----- lightweight audio buffer "tasting" while drawing -----

int drawTasteIntervalMs = 30;      // minimum time between tasting triggers
float drawTasteMinDist = 6.0f;     // minimum mouse movement before retrigger
int drawTasteDurationMs = 96;      // short taste burst
int drawTasteMaxGrains = 12;       // hard cap on grains per taste
int drawTasteBurstGrains = 1;      // keep taste small and cheap

int lastDrawTasteMs = -100000;        // milliseconds since last draw taste
PVector lastDrawTastePoint = null;    // last point where we tasted a sample


/* ------------------------------------------------------------------ */
/*                                LOOPING                             */
/* ------------------------------------------------------------------ */

static class InstrumentLoop {
  final AudioBrush brush;     // optional owner, useful later for hover/cancel
  final Runnable playAction;
  final Runnable stopAction;

  final int durationMs;       // one cycle duration
  final int gapMs;            // silence between cycles
  int repeatsRemaining;       // -1 = infinite
  long nextStartMillis;       // next trigger time
  boolean active = true;

  InstrumentLoop(AudioBrush brush,
    Runnable playAction,
    Runnable stopAction,
    int durationMs,
    int gapMs,
    int repeatsRemaining,
    long nextStartMillis) {
    this.brush = brush;
    this.playAction = playAction;
    this.stopAction = stopAction;
    this.durationMs = Math.max(1, durationMs);
    this.gapMs = Math.max(0, gapMs);
    this.repeatsRemaining = repeatsRemaining;
    this.nextStartMillis = nextStartMillis;
  }

  void stop() {
    active = false;
    if (stopAction != null) stopAction.run();
  }
}

final ArrayList<InstrumentLoop> activeLoops = new ArrayList<>();

/* ------------------------------------------------------------------ */
/*                              PRESET LIST                           */
/* ------------------------------------------------------------------ */

/* Presets for Bagatelle "Abstract Jailbreak"
 *    1. measure 5, first gesture file, bag_1_gest_1_tail.wav, 5 seconds long, with swell and fall-off in dynamics, begin drawing at m3
 *    2. measure 7 begin drawing at trill, release at m11 downbeat, second file, bag_1_gest_2_tail.wav, how long? any dynamics?
 *    3. from measure 12 to measure 19 first beat, third file, bag_1_crackle.wav, glitches, wisps, crackles -- new file?
 *    4. begin drawing at m37, release at m43, 8 seconds 16th notes until m58 1st beat — which file?
 *    5. m66, Macabre, effects control via UDP
 */
/**
 * Presets are applied to each new brush at the moment drawing is completed by
 * releasing the mouse button and calling makeBrush(), the bottleneck method for
 * all brush creation. Presets are best used just for brush modifications. If you
 * want to change application settings, use runPerformanceCue() with your own custom
 * code. You can address the host application with the <code>app</app> parameter,
 * but keep in mind that is is called on every brush.
 */
enum PerformancePreset {
  DURATION_5SEC_SWELL('1') {
    @Override
      CueResult apply(GestureGranularConfig.Builder cfg, PACurveMaker curve, Bagatelle app) {
      int newDurationMs = 5000;
      cfg.targetDurationMs = newDurationMs;
      cfg.rdpEpsilon = 2;
      cfg.pathMode = GestureGranularConfig.PathMode.ALL_POINTS;
      cfg.hopMode = GestureGranularConfig.HopMode.GESTURE;
      cfg.burstGrains = 8;
      cfg.grainLengthSamples = 2560;
      cfg.hopLengthSamples = 640;
      return new CueResult(curve, cfg);
    }
  }
  ,
    LONG_ECHO('2') {
    @Override
      CueResult apply(GestureGranularConfig.Builder cfg, PACurveMaker curve, Bagatelle app) {
      cfg.grainLengthSamples = 1024;
      cfg.hopLengthSamples = 256;
      cfg.targetDurationMs = curve.getTimeOffset() * 2;
      return new CueResult(curve, cfg);
    }
  }
  ,
    GLITCH_CORTO('3') {
    @Override
      CueResult apply(GestureGranularConfig.Builder cfg, PACurveMaker curve, Bagatelle app) {
      curve.setBezierBias(2.0f);
      curve.setDrawWeighted(true);
      cfg.rdpEpsilon = 8.0f;
      cfg.pathMode = GestureGranularConfig.PathMode.REDUCED_POINTS;
      cfg.hopLengthSamples = 256;
      cfg.grainLengthSamples = 1024;
      cfg.burstGrains = 4;
      return new CueResult(curve, cfg);
    }
  }
  ,
    GLITCH_LARGO('4') {
    @Override
      CueResult apply(GestureGranularConfig.Builder cfg, PACurveMaker curve, Bagatelle app) {
      curve.setBezierBias(0.5f);
      curve.setDrawWeighted(true);
      cfg.rdpEpsilon = 40.0f;
      cfg.pathMode = GestureGranularConfig.PathMode.REDUCED_POINTS;
      cfg.hopLengthSamples = 128;
      cfg.grainLengthSamples = 512;
      cfg.burstGrains = 16;
      return new CueResult(curve, cfg);
    }
  }
  ,
    SIXTEENTHS('5') {
    @Override
      CueResult apply(GestureGranularConfig.Builder cfg, PACurveMaker curve, Bagatelle app) {
      // app.resetPerformanceState();
      int bpm = 128;
      int stepMs = Math.round(60000f / bpm / 4f);   // 117 ms
      // we can calculate new points and times using the total gesture time
      int totalMs = Math.max(stepMs, curve.getTimeOffset());
      int count = 1 + Math.max(1, Math.round(totalMs / (float) stepMs));
      PAGestureParametric tweener = new PAGestureParametric(curve.getAllPoints(), curve.getAllTimes());
      ArrayList<PVector> newPoints = new ArrayList<>(count);
      ArrayList<Integer> newTimes = new ArrayList<>(count);
      //
      for (int i = 0; i < count; i++) {
        float u = (count <= 1) ? 0f : i / (float)(count - 1);
        PAGestureParametric.Sample s = tweener.sample(u);
        newPoints.add(new PVector(s.x, s.y));
        newTimes.add(i * stepMs);
      }
      // create a new curve, don't try to edit the original
      PACurveMaker newCurve = PACurveMaker.buildCurveMaker(newPoints, newTimes, curve.timeStamp);
      // preserve visual / geometric settings you care about
      newCurve.setEpsilon(curve.getEpsilon());
      newCurve.setCurveSteps(curve.getCurveSteps());
      newCurve.setBrushSize(curve.getBrushSize());
      newCurve.setBrushColor(curve.getBrushColor());
      newCurve.setActiveBrushColor(curve.getActiveBrushColor());
      newCurve.calculateDerivedPoints();
      // config adjustments, may override curve settings
      // cfg.targetDurationMs = newTimes.get(newTimes.size() - 1);
      cfg.hopLengthSamples = 512;
      cfg.grainLengthSamples = 2048;
      cfg.burstGrains = 4;
      cfg.pathMode = GestureGranularConfig.PathMode.ALL_POINTS;
      cfg.rdpEpsilon = 2.0f;
      cfg.curveSteps = 8;
      cfg.env = app.granularEnv;
      return new CueResult(newCurve, cfg);
    }
  }
  ,
    DYNAMICS_1('6') {
    @Override
      CueResult apply(GestureGranularConfig.Builder cfg, PACurveMaker curve, Bagatelle app) {
      cfg.rdpEpsilon = 2;
      cfg.pathMode = GestureGranularConfig.PathMode.ALL_POINTS;
      cfg.hopMode = GestureGranularConfig.HopMode.GESTURE;
      cfg.burstGrains = 8;
      cfg.grainLengthSamples = 2560;
      cfg.hopLengthSamples = 640;
      float[] timesMs = new float[] {0, 500, 2500, 5000};
      float dur = timesMs[timesMs.length - 1];
      float[] values = new float[] {0.1f, 1.0f, 1.0f, 0.1f};
      float[] times = new float[timesMs.length];
      for (int i = 0; i < times.length; i++) {
        times[i] =  map(timesMs[i], 0f, dur, 0f, 1f);
      }
      app.dynamics = new PAKeyframeControlCurve (times, values);
      return new CueResult(curve, cfg);
    }
  };

  final char key;

  PerformancePreset(char key) {
    this.key = key;
  }

  static PerformancePreset fromKey(char k) {
    for (PerformancePreset c : values()) {
      if (c.key == k) return c;
    }
    return null;
  }

  /**
   * Abstract method for concrete apply() methods implemented by each enum constant. In the preset
   * logic, each constant is in effect its own function, which can be invoked by reference.
   *
   * @param cfg      configuration parameters for GestureGranularConfig, used to modify audio synthesis
   * @param curve    a PACurveMaker gesture, which can be modifed by the concrete apply() method
   * @param app      a reference to the host application, use with caution
   * @return         a reference to the concrete apply() method for an enum constant
   */
  abstract CueResult apply(GestureGranularConfig.Builder cfg, PACurveMaker curve, Bagatelle app);
}

ArrayList<PerformancePreset> presetStack = new ArrayList<>();

static class CueResult {
  final PACurveMaker curve;
  final GestureGranularConfig.Builder cfg;
  CueResult(PACurveMaker curve, GestureGranularConfig.Builder cfg) {
    this.curve = curve;
    this.cfg = cfg;
  }
}

/* ------------------------------------------------------------------ */
/*                    DEBUGGING + PERFORMANCE SETTINGS                */
/* ------------------------------------------------------------------ */

boolean isVerbose = true;
boolean isDebugging = false;

boolean shiftIsDown = false;         // flag for shift key down

// performance state

boolean isRunWordGame = false;       // presets and files: if true, run DeadBodyWorkFlow; if false, run Bagatelle 1
boolean doPlayOnNewBrush = false;    // play audio when a curve is drawn
boolean doPlayWhileDrawing = false;  // play audio events while drawing, or not
boolean isAutoOptimize = false;      // optimize the freshly drawn curve before playing it
boolean doMagicClick = false;        // find a brush that starts in a selected rectangle and play it
boolean usePitchedGrains = false;    // jitter granular pitch if true
float pitchJitter = 0.0167f;         // amount of jitter
boolean applyColorMapOnLoad = true;  // if true, apply the color map when a file is loaded

boolean isReplaceBrushes = true;     // if true, replace current brushes when we load a session or library folder
boolean isSaveSession = true;        // save the session brushes ('J' key commend)

boolean isRaining = false;           // if true, run random audio/animation point events

boolean isAddDynamics = false;       // add dynamics curve to gestures
// dynamics for sampler and granular gestures
PAControlCurve dynamics = new PAKeyframeControlCurve (
  new float[] {0.0f, 0.5f, 1.0f}, // times
  new float[] {0.1f, 1.0f, 0.1f} );    // values

boolean isBrushTransformTest = false;      // testing animation feature (key 'y')
boolean isBrushTransformFrozen = false;    // freeze animation (key 'Y')

boolean isBrushSelectionModal = false;     // if false, select all brushes, otherwise, select by active mode

PABoundsPolicy.PABoundaryMode boundaryMode = PABoundsPolicy.PABoundaryMode.CLIP;
PABoundsPolicy boundsPolicy;

// *****>>> NETWORKING <<<***** //
NetworkDelegate nd;
boolean isUseNetworkDelegate = false;
boolean isNetSendDrawingPoints = true;
boolean isNetSendOutsideBrushPoints = true;
boolean isNetSendBrushTriggers = false;
boolean isNetSendFileInfo = false;
boolean isNetSendGestures = false;

// in Processing, for PixelAudio Tutorial examples, use this in setup(): daPath = sketchPath("") + "../examples_data/";
String daPath = "/Users/paulhz/Code/Workspace/PixelAudio/examples/examples_data/";   // system-specific path to example files data
String daFilename = "audioBlend.wav";    // "audioBlend.wav";
ArrayList<String> daFilelist = new ArrayList<>();



//------------- APPLICATION CODE -------------//

public void settings() {
  size(3 * genWidth, 2 * genHeight);
}

public void setup() {
  daPath = sketchPath("") + "../examples_data/";    // PROCESSING ONLY, in Eclipse use system-specific path
  // set a standard animation framerate -- in most example sketches we use 44100
  // but in performance sketches like DeadBodyWorkFlow we use 48000
  sampleRate = 48000;    // could be a little redundant, but I'm too lazy to scroll up to the top
  frameRate(120);
  surface.setTitle(isRunWordGame ? "DeadBodyWorkFlow" : "Abstract Jailbreak");
  // we can put application and control panel always on top, see createControlWindow()
  surface.setAlwaysOnTop(true);
  // 1) initialize our library
  pixelaudio = new PixelAudio(this);
  // 2) create a PixelMapGen instance with dimensions equal to the display window.
  if (isRunWordGame) {
    multigen = loadWordGen(genWidth/4, genHeight/4);
  } else {
    multigen = HilbertGen.hilbertRowOrtho(6, 4, width/6, height/4);
  }
  // 3) Create a PixelAudioMapper to handle the mapping of pixel colors to audio samples.
  mapper = new PixelAudioMapper(multigen);
  mapSize = mapper.getSize();
  scheduleBuilder = new GestureScheduleBuilder();
  // initialize the boundary policy for keeping points and indices in bounds
  boundsPolicy = PABoundsPolicy.fromWidthHeight(mapper.getWidth(), mapper.getHeight(), boundaryMode);
  colors = getColors(mapSize);    // create an array of rainbow colors with mapSize elements
  initImages();                   // load baseImage and mapImage
  initAudio();                    // set up Minima and our granular and sampling synths
  // initListener();              // PLACEHOLDER: sample-accurate audio timer -- TODO future implementation
  initConfig();                   // set up configuration for granular and sampling instruments
  initDrawing();                  // set up drawing variables
  initGUI();                      // set up the G4P control window and widgets
  resetConfigForMode();           // determine which GestureGranularConfig to use first and load it
  // *****>>> NETWORKING <<<***** //
  isUseNetworkDelegate = true;
  initNetwork();
  // customize environment
  initCustomSettings();
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
 * Adds PixelMapGen objects to the local variable genList. The genList
 * initializes a MultiGen, which can be used to map audio and pixel data.
 * This method follows the words in the workFlowPanel.png graphic.
 */
public MultiGen loadWordGen(int wordGenW, int wordGenH) {
  // list of PixelMapGens that create an image using mapper
  ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
  // list of x,y coordinates for placing gens from genList
  ArrayList<int[]> offsetList = new ArrayList<int[]>();
  for (int y = 0; y < 4; y++) {
    for (int x = 0; x < 4; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 4; y < 8; y++) {
    for (int x = 0; x < 4; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 0; y < 4; y++) {
    for (int x = 4; x < 8; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 4; y < 8; y++) {
    for (int x = 4; x < 8; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 0; y < 4; y++) {
    for (int x = 8; x < 12; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 4; y < 8; y++) {
    for (int x = 8; x < 12; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  return new MultiGen(width, height, offsetList, genList);
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
 * MapImage handles the color data for mapper and also serves as our display image.
 * BaseImage is intended as a reference image that usually only changes when you open a new image file.
 */
public void initImages() {
  mapImage = createImage(width, height, ARGB);
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapper.getSize()); // load colors to mapImage following signal path
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

/**
 * Initializes default settings for granular synthesis, defaultGranConfig,
 * and for sampler synthesis, defaultSampConfig.
 */
public void initConfig() {
  granularEnv = new ADSRParams(1.0f, 0.005f, 0.02f, 0.975f, 0.025f);
  defaultGranConfig.grainLengthSamples = granLength;
  defaultGranConfig.hopLengthSamples = granHop;
  defaultGranConfig.curveSteps = curveSteps;
  defaultGranConfig.granularEnvelope(granularEnv);
  samplerEnv = envPreset("Soft");
  defaultSampConfig.samplerEnvelope(samplerEnv);
  defaultSampConfig.rdpEpsilon = 8.0f;
  defaultSampConfig.pathMode = GestureGranularConfig.PathMode.REDUCED_POINTS;
}

/**
 * Initializes the control palette.
 */
public void initGUI() {
  createGUI();
  controlWindow.loop();
}

void initCustomSettings() {
  if (isRunWordGame) {
    isLoadToBoth = false;
    daFilename = "workflow_48Khz.wav";
    daPath = daPath + "Body/";
    loadAudioFile(new File(daPath + daFilename));
    daFilename = "workFlowPanel.png";
    preloadFiles(daPath, daFilename);
  } else {
    daPath = daPath + "Bag/";
    daFilename = "bag_1_gest_1_tail.wav";
    preloadFiles(daPath, daFilename);
  }
}

void initNetwork() {
  // *****>>> NETWORKING <<<***** //
  if (isUseNetworkDelegate) {
    String remoteAddress = "127.0.0.1";
    nd = new NetworkDelegate(this, remoteAddress, remoteAddress, 7401, 7400);
    nd.oscSendClear();
  }
}

/**
 * Preload an audio file using a file path and a filename.
 * @param path        the fully qualified path to the file's directory, ending with a '/'
 * @param fileName    the name of the file
 */
public void preloadFiles(String path, String fileName) {
  // the audio file we want to open on startup
  File sourceFile = new File(path + fileName);
  // load the file into audio buffer and Brightness channel of display image (mapImage)
  // if audio is also loaded to the image, will set baseImage to the new image
  fileSelected(sourceFile);
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

// ANIMATION METHODS //

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
 * Handles user's drawing actions, draws previously recorded brushstrokes,
 * tracks and generates animation and audio events.
 */
public void handleDrawing() {
  // 0) any animation of brushes goes first
  updateAnimatedBrushes();
  // 1) draw existing brushes
  drawBrushShapes();
  // 2) update hover state (pure state update, no action)
  updateHover();
  // 3) if in the process of drawing, accumulate points while mouse is held down
  if (isEditable()) {
    if (mousePressed) {
      addDrawingPoint(clipToWidth(mouseX), clipToHeight(mouseY));
    }
    if (allPoints != null && allPoints.size() > 2) {
      PACurveUtility.lineDraw(this, allPoints, dragColor, dragWeight);
    }
  }
  // 4) loooooooping of various sorts
  updateInstrumentLoops();
  if (isRaining) {
    // animation slows the frame rate, so we change the threshold when animating
    float thresh = 0.05f;
    if (random(0, 1) < thresh) {
      raindrops();              // trigger random audio events
    }
  }
  if (isAnimating) animate();
  // 5) depending on your event dispatching model, run scheduled events
  runSamplerBrushEvents();
  runPointEvents();
  runGrainEvents();
}

/**
 * @return a reference to the brushstroke the mouse is over, or null if there's no brushstroke.
 */
BrushHit findHoverHit() {
  // Decide z-order: check the list you draw last *first*,
  // so topmost brushes win.
  // granular on top here, descending for loop means most recent brushes are on top
  /* MODAL SELECTION */
  if (isBrushSelectionModal) {
    if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER || drawingMode == DrawingMode.PLAY_ONLY) {
      for (int i = samplerBrushes.size() - 1; i >= 0; i--) {
        SamplerBrush b = samplerBrushes.get(i);
        if (mouseInPoly(b.curve().getBrushPoly())) {
          return new BrushHit(b, i);
        }
      }
    }
    if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR || drawingMode == DrawingMode.PLAY_ONLY) {
      for (int i = granularBrushes.size() - 1; i >= 0; i--) {
        GranularBrush b = granularBrushes.get(i);
        if (mouseInPoly(b.curve().getBrushPoly())) {
          return new BrushHit(b, i);
        }
      }
    }
  } else {
    for (int i = samplerBrushes.size() - 1; i >= 0; i--) {
      SamplerBrush b = samplerBrushes.get(i);
      if (mouseInPoly(b.curve().getBrushPoly())) {
        return new BrushHit(b, i);
      }
    }
    for (int i = granularBrushes.size() - 1; i >= 0; i--) {
      GranularBrush b = granularBrushes.get(i);
      if (mouseInPoly(b.curve().getBrushPoly())) {
        return new BrushHit(b, i);
      }
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

BrushHit getBrushInRect(int x, int y, int wStep, int hStep) {
  float[] r = new float[4];
  int x1 = (x / wStep) * wStep;
  int y1 = (y / hStep) * hStep;
  int x2 = x1 + wStep;
  int y2 = y1 + hStep;
  r[0] = x1;
  r[1] = y1;
  r[2] = x2;
  r[3] = y2;
  // println("-- [", x1, y1, x2, y2, "] contains ", x, y, " == ", containsPoint(x, y, r));
  if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR || drawingMode == DrawingMode.PLAY_ONLY) {
    for (int i = granularBrushes.size() - 1; i >= 0; i--) {
      GranularBrush b = granularBrushes.get(i);
      PVector v = b.curve().getDragPoints().get(0);
      if (containsPoint(v.x, v.y, r)) {
        return new BrushHit(b, i);
      }
    }
  }
  if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER || drawingMode == DrawingMode.PLAY_ONLY) {
    for (int i = samplerBrushes.size() - 1; i >= 0; i--) {
      SamplerBrush b = samplerBrushes.get(i);
      PVector v = b.curve().getDragPoints().get(0);
      if (containsPoint(v.x, v.y, r)) {
        return new BrushHit(b, i);
      }
    }
  }
  return null;
}

/**
 * @param x   x-coordinate of point to test
 * @param y   y-coordinate of point to test
 * @param r   rectangle to test
 * @return    {@code true} if r contains the specified point
 */
public static boolean containsPoint(float x, float y, float[] r) {
  return (    x >= r[0] && x <= r[2]
    && y >= r[1] && y <= r[3]);
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
 * The built-in mousePressed handler for Processing, used to begin drawing.
 */
public void mousePressed() {
  if (isEditable()) {
    initAllPoints();
    lastDrawTasteMs = -100000;
    lastDrawTastePoint = null;
  }
  // expand here to call a handler for mousePressed when editing is not on (mode == PLAY_ONLY)
}

public void mouseDragged() {
  // we don't need to handle dragging -- the draw() loop takes care of drawing to the display
}

public void mouseReleased() {
  // if (!(isEditable() && allPoints != null)) return; // EDIT to go ahead in all modes
  if (allPoints != null && allPoints.size() > 2) {
    initCurveMakerAndAddBrush();    // create a new brush
    allPoints.clear();
    // possible preview action, play on draw
  } else {  // handle the event as a click in mouseClicked()
    if (allPoints != null) allPoints.clear();
  }
}

public void mouseClicked() {
  int x = this.clipToWidth(mouseX);
  int y = this.clipToHeight(mouseY);
  BrushHit hit = findHoverHit();
  if (hit != null) {
    openBrushEditor(hit.brush);      // flag the hit brush as the activeBrush
    PAControlCurve gainCurve = isAddDynamics ? dynamics : null;
    if (hit.brush instanceof SamplerBrush) {
      SamplerBrush sb = (SamplerBrush) hit.brush;
      scheduleSamplerBrushClick(sb, x, y, gainCurve);
    } else if (hit.brush instanceof GranularBrush) {
      GranularBrush gb = (GranularBrush) hit.brush;
      scheduleGranularBrushClick(gb, x, y, gainCurve);
    }
    return;
  }
  // in other sketches we called audioMousePressed(), handleClickOutsideBrush() does the same sort of things
  if (isDebugging) println(" in mouseClicked, calling handleClickOutsideBrush, drawing mode is "+ drawingMode.toString());
  handleClickOutsideBrush(x, y);
}


/**
 * built-in keyPressed handler, forwards events to parseKey.
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
      adjustInstrumentGain(3.0f);
      float newGain = (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) ?
        pool.getGainDb() : gDir.getInstrument().getGlobalGainDb();
      println("---- configuration gain is "+ nf(newGain, 0, 2) +"dB");
    } else if (keyCode == LEFT) {
      adjustInstrumentGain(-3.0f);
      float newGain = (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) ?
        pool.getGainDb() : gDir.getInstrument().getGlobalGainDb();
      println("---- configuration gain is "+ nf(newGain, 0, 2) +"dB");
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
 * @param key
 * @param keyCode
 */
public void parseKey(char key, int keyCode) { // TODO create auto-adjust Sampler note duration toggle
  String msg;
  if ("0123456789".contains("" + key)) {
    numericKey(key);
  }
  switch(key) {
  case TAB: // set brush to active, if cursor is over a brush
    if (isEditable() && hoverBrush !=  null) openBrushEditor(hoverBrush);
    break;
  case ' ': // (spacebar) trigger a brush if we're hovering over a brush, otherwise trigger a point event
    if (doMagicClick) {
      BrushHit hit = getBrushInRect(clipToWidth(mouseX), clipToHeight(mouseY), genWidth, genHeight/4);
      if (hit != null) {
        hoverBrush = hit.brush;
        hoverIndex = hit.index;
        setActiveBrush(hoverBrush);
      }
    }
    if (hoverBrush != null) {
      PAControlCurve gainCurve = isAddDynamics ? dynamics : null;
      if (hoverBrush instanceof SamplerBrush) {
        SamplerBrush sb = (SamplerBrush) hoverBrush;
        scheduleSamplerBrushClick(sb, clipToWidth(mouseX), clipToHeight(mouseY), gainCurve);
      } else if (hoverBrush instanceof GranularBrush) {
        GranularBrush gb = (GranularBrush) hoverBrush;
        scheduleGranularBrushClick(gb, clipToWidth(mouseX), clipToHeight(mouseY), gainCurve);
      }
    } else {
      handleClickOutsideBrush(clipToWidth(mouseX), clipToHeight(mouseY));
    }
    break;
  case 'a': // toggle animation
    isAnimating = !isAnimating;
    println(isAnimating ? "-- animation is ON" : "-- animation is OFF");
    break;
  case 'c':
  case 'C': // print the current configuration status to the console
    printGConfigStatus();
    if (activeBrush != null) {
      println("-- "+ activeBrush.curve().curveInfo());
    }
    break;
  case 't': // switch between Granular, Sampler, and Play Only modes
    if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR) {
      setMode(DrawingMode.DRAW_EDIT_SAMPLER);
      controlWindow.setTitle("Sampler Synth");
    } else if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) {
      setMode(DrawingMode.PLAY_ONLY);
      controlWindow.setTitle("Play Only: Both Synths");
    } else if (drawingMode == DrawingMode.PLAY_ONLY) {
      setMode(DrawingMode.DRAW_EDIT_GRANULAR);
      controlWindow.setTitle("Granular Synth");
    }
    println("---> mode is "+ drawingMode.toString());
    break;
  case 'z': // change the drawing mode of the hover brush
    AudioBrush changed = null;
    if (hoverBrush != null) {
      changed = toggleHoveredBrushType();
      syncDrawingModeToBrush(changed);
    } else if (activeBrush != null) {
      changed = toggleActiveBrushType();
      syncDrawingModeToBrush(changed);
    } else {
      // handleClickOutsideBrush(clipToWidth(mouseX), clipToHeight(mouseY));
    }
    break;
  case 'd': // toggle doPlayOnNewBrush: if true, audio plays when a new brush is created
    doPlayOnNewBrush = !doPlayOnNewBrush;
    println("-- play on new brush is "+ doPlayOnNewBrush);
    break;
  case 'D': // toggle doPlayOnDraw: if true, drawing triggers audio while you drag the mouse
    doPlayWhileDrawing = !doPlayWhileDrawing;
    println("-- play while drawing is "+ doPlayWhileDrawing);
    break;
  case 'p': // jitter the pitch of granular gestures
    usePitchedGrains = !usePitchedGrains;
    pitchJitter = 0.0167f;
    msg = (usePitchedGrains) ? " jitter granular pitch." : " steady granular pitch.";
    println("-- Play granular synth with "+ msg);
    break;
  case 'k': // apply the hue and saturation in the colors array to mapImage (not to baseImage)
    applyColorMapToDisplay(false);
    break;
  case 'K': // apply hue and saturation in colors to baseImage and mapImage
    applyColorMapToDisplay(true);
    break;
  case 'b':
  case 'B': // toggle loading data to both image and audio buffers when you open either an image or an audio file
    isLoadToBoth = !isLoadToBoth;
    println(isLoadToBoth ? "-- load to both image and audio" : "-- load only to image or audio");
    break;
  case 'f':
  case 'F': // open a folder with JSON brush data and load all files
    chooseGestureLibraryFolder();
    break;
  case 'j': // save the active brush curve and config to JSON files
    isSaveSession = false;
    this.saveGestureJSON(isSaveSession);
    break;
  case 'J': // save all brushes curve and config to JSON Session file
    isSaveSession = true;
    this.saveGestureJSON(isSaveSession);
    break;
  case 'o': // open an audio file, image file, or JSON file
    chooseFile();
    break;
  case 'w': // write the map image to the audio buffer
    renderMapImageToAudio(PixelAudioMapper.ChannelNames.L);
    updateAudioChain(audioSignal);
    break;
  case 'W': // write the audio buffer to the display image
    writeAudioToImage(audioSignal, mapper, mapImage, PixelAudioMapper.ChannelNames.L);
    commitMapImageToBaseImage();
    if (applyColorMapOnLoad) applyColorMapToDisplay(true);
    break;
  case 'm': // toggle doMagicClick, play brushstroke in same rectangle as mouse on click or spacebar
    doMagicClick = !doMagicClick;
    println(doMagicClick ? "-- doMagicClick is true" : "-- doMagicClick is false");
    break;
  case 'n': // set noise reduction policy for Sampler instrument audio mix
    pool.cycleMixProfile();
    println("-- mix profile is "+ pool.getMixProfile().name());
    break;
  case 'R': // reset transform of active brush if it has a transform TODO clarify
    if (activeBrush != null && activeBrush.hasTransform()) {
      activeBrush.restoreTransform();
      activeBrush.transform().resetTransform();
      println("-- restored hover brush transform");
    } else {
      resetConfigToDefaults();
    }
    break;
  case 'r': // reset instrument configuration to defaults in GUI
    resetConfigToDefaults();
    break;
  case 'q': // automatically set an active GRANULAR brush to have an optimized number of samples
    if (activeBrush instanceof SamplerBrush) {
      println("-- please choose a Granular Brush to adjust resampling and duration values.");
      return;
    }
    optimizeActiveBrush();
    break;
  case 'u': // toggle granular sample optimization: same as the 'q' command, applied on brushstroke creation
    isAutoOptimize = !isAutoOptimize;
    println("-- isAutoOptimize is "+ isAutoOptimize);
    break;
  case 'E': // toggle whether we adjust envelope duration in relation to gesture duration
    isAdjustEnvelope = !isAdjustEnvelope;
    println("-- isAdjustEnvelope is "+ isAdjustEnvelope);
    break;
  case 'g': // toggle use of dynamics in gainCurve with gesture
    isAddDynamics = !isAddDynamics;
    println("-- isAddDynamics = "+ isAddDynamics);
    break;
    // LOOPING
  case 'G': // create a beatBrush
    if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR) {
      GranularBrush gBeatBrush = generateGranularBeatBrush(256, 117);
      this.granularBrushes.add(gBeatBrush);
      setActiveBrush(gBeatBrush);
    } else if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) {
      SamplerBrush sBeatBrush = generateSamplerBeatBrush(256, 117);
      this.samplerBrushes.add(sBeatBrush);
      setActiveBrush(sBeatBrush);
    } // for PLAY_ONLY, do nothing
    break;
  case 'l': // loop hovered brush 4 times
    int loops = 4;
    if (hoverBrush instanceof GranularBrush) {
      GranularBrush gb = (GranularBrush) hoverBrush;
      loopGranularBrush(gb, loops, 150);
      println("-- started granular loop x4");
    } else if (hoverBrush instanceof SamplerBrush) {
      SamplerBrush sb = (SamplerBrush) hoverBrush;
      loopSamplerBrush(sb, loops, 150);
      println("-- started sampler loop x4");
    } else if (activeBrush instanceof GranularBrush) {
      GranularBrush gb = (GranularBrush) activeBrush;
      loopGranularBrush(gb, loops, 150);
      println("-- started granular loop x4");
    } else if (activeBrush instanceof SamplerBrush) {
      SamplerBrush sb = (SamplerBrush) activeBrush;
      loopSamplerBrush(sb, loops, 150);
      println("-- started sampler loop x4");
    }
    break;
  case 'L': // run an infinite loop on hovered brush
    if (hoverBrush instanceof GranularBrush) {
      GranularBrush gb = (GranularBrush) hoverBrush;
      loopGranularBrush(gb, -1, 150);
      println("-- started infinite granular loop");
    } else if (hoverBrush instanceof SamplerBrush) {
      SamplerBrush sb = (SamplerBrush) hoverBrush;
      loopSamplerBrush(sb, -1, 150);
      println("-- started infinite sampler loop");
    } else if (activeBrush instanceof GranularBrush) {
      GranularBrush gb = (GranularBrush) activeBrush;
      loopGranularBrush(gb, -1, 150);
      println("-- started infinite granular loop");
    } else if (activeBrush instanceof SamplerBrush) {
      SamplerBrush sb = (SamplerBrush) activeBrush;
      loopSamplerBrush(sb, -1, 150);
      println("-- started infinite sampler loop");
    }
    break;
  case ';': // stop loop for hovered brush
    if (hoverBrush != null) {
      stopLoopsForBrush(hoverBrush);
      println("-- stopped loops for hover brush");
    } else if (activeBrush != null) {
      stopLoopsForBrush(activeBrush);
      println("-- stopped loops for active brush");
    }
    break;
  case ':': // stop all loops
    stopAllLoops();
    println("-- stopped all loops");
    break;
  case 'y': // toggle transform animation test
    isBrushTransformTest = !isBrushTransformTest;
    if (isBrushTransformTest) {
      if (activeBrush != null) {
        initBrushTransform(activeBrush);
      }
      println("-- brush transform test ON");
    } else {
      if (activeBrush != null && activeBrush.hasTransform()) {
        activeBrush.restoreTransform();
      }
      println("-- brush transform test OFF");
    }
    break;
    // BRUSH ANIMATON
  case 'Y': // freeze / unfreeze brush geometric transform animation
    isBrushTransformFrozen = !isBrushTransformFrozen;
    println("-- brush transform frozen = " + isBrushTransformFrozen);
    break;
  case 'x': // delete the current active brush shape or the oldest brush shape
    if (hoverBrush != null) {
      removeHoverBrush();
    } else {
      removeOldestBrush();
    }
    break;
  case 'X': // delete the most recent brush shape
    removeNewestBrush();
    break;
  case '.': // turn random raindrops audio events on or off
    isRaining = !isRaining;
    println(isRaining ? "-- rain, rain" : "-- not a drop");
    break;
  case '≈': // option-x on MacOS keyboard, clear all brushes
    granularBrushes.clear();
    samplerBrushes.clear();
    break;
  case '`': // fade out all instruments
    suspendScheduledEvents();
    if (pool != null) pool.fadeOutAll();
    if (gDir != null) gDir.cancelAndReleaseAll();
    println("-- fade out all");
    break;
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
  case 'v': // send UDP message to Max (simpleAudioIO.maxpat): small reverb settings
    if (nd != null) {
      nd.oscSendSetnum(1, 0.125f);
      nd.oscSendSetnum(2, 2048.0f);
      nd.oscSendSetnum(3, 0.125f);
      nd.oscSendSetnum(4, 2560.0f);
    }
    break;
  case 'V': // send UDP message to Max (simpleAudioIO.maxpat): big reverb settings
    if (nd != null) {
      nd.oscSendSetnum(1, 0.3f);
      nd.oscSendSetnum(2, 4096.0f);
      nd.oscSendSetnum(3, 0.3f);
      nd.oscSendSetnum(4, 5120.0f);
    }
    break;
  case 'h':
  case 'H': // show help message
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
  println(" * Press UP ARROW to increase audio output volume by 1.0 or 3.0 dB (+shift).");
  println(" * Press DOWN ARROW to decrease audio output volume by 1.0 or 3.0 dB (+shift).");
  println(" * Press RIGHT ARROW to increase current instrument gain by 3.0 dB.");
  println(" * Press LEFT ARROW to decrease current instrument gain by 3.0 dB.");
  println(" * Keys 1 through 9 are reserved for triggering Performance Presets 1-9, '0' will clear all presets.");
  println(" * Press TAB to set brush to active, if cursor is over a brush.");
  println(" * Press ' ' to (spacebar) trigger a brush if we're hovering over a brush, otherwise trigger a point event.");
  println(" * Press 'a' to toggle animation.");
  println(" * Press 'c' or 'C' to print the current configuration status to the console.");
  println(" * Press 't' to switch between Granular, Sampler, and Play Only modes.");
  println(" * Press 'z' to change the drawing mode of the hover brush.");
  println(" * Press 'd' to toggle doPlayOnNewBrush: if true, audio plays when a new brush is created.");
  println(" * Press 'D' to toggle doPlayOnDraw: if true, drawing triggers audio while you drag the mouse.");
  println(" * Press 'p' to jitter the pitch of granular gestures.");
  println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage (not to baseImage).");
  println(" * Press 'K' to apply hue and saturation in colors to baseImage and mapImage.");
  println(" * Press 'b' or 'B' to toggle loading data to both image and audio buffers when you open either an image or an audio file.");
  println(" * Press 'f' or 'F' to open a folder with JSON brush data and load all files.");
  println(" * Press 'j' to save the active brush curve and config to JSON files.");
  println(" * Press 'J' to save all brushes curve and config to JSON Session file.");
  println(" * Press 'o' to open an audio file, image file, or JSON file.");
  println(" * Press 'w' to write the map image to the audio buffer.");
  println(" * Press 'W' to write the audio buffer to the display image.");
  println(" * Press 'm' to toggle doMagicClick, play brushstroke in same rectangle as mouse on click or spacebar.");
  println(" * Press 'n' to set noise reduction policy for Sampler instrument audio mix.");
  println(" * Press 'R' to reset transform of active brush if it has a transform TODO clarify.");
  println(" * Press 'r' to reset instrument configuration to defaults in GUI.");
  println(" * Press 'q' to automatically set an active GRANULAR brush to have an optimized number of samples.");
  println(" * Press 'u' to toggle granular sample optimization: same as the 'q' command, applied on brushstroke creation.");
  println(" * Press 'E' to toggle whether we adjust envelope duration in relation to gesture duration.");
  println(" * Press 'g' to toggle use of dynamics in gainCurve with gesture .");
  println(" * Press 'G' to create a beatBrush.");
  println(" * Press 'l' to loop hovered brush 4 times.");
  println(" * Press 'L' to run an infinite loop on hovered brush.");
  println(" * Press ';' to stop loop for hovered brush.");
  println(" * Press ':' to stop all loops.");
  println(" * Press 'y' to toggle transform animation test.");
  println(" * Press 'Y' to freeze / unfreeze brush geometric transform animation.");
  println(" * Press 'x' to delete the current active brush shape or the oldest brush shape.");
  println(" * Press 'X' to delete the most recent brush shape.");
  println(" * Press '.' to turn random raindrops audio events on or off.");
  println(" * Press '≈' to option-x on MacOS keyboard, clear all brushes.");
  println(" * Press '`' to fade out all instruments.");
  println(" * Press ']' to send UDP message to Max (simpleAudioIO.maxpat): reverb ON.");
  println(" * Press '[' to send UDP message to Max (simpleAudioIO.maxpat): reverb OFF.");
  println(" * Press '}' to send UDP message to Max (simpleAudioIO.maxpat): unused.");
  println(" * Press '{' to send UDP message to Max (simpleAudioIO.maxpat): unused.");
  println(" * Press 'v' to send UDP message to Max (simpleAudioIO.maxpat): small reverb settings.");
  println(" * Press 'V' to send UDP message to Max (simpleAudioIO.maxpat): big reverb settings.");
  println(" * Press 'h' or 'H' to show help message.");
}


/**
 * Apply color map hue and saturation to mapImage or baseImage.
 */
public void applyColorMapToDisplay(boolean updateBaseImage) {
  if (updateBaseImage) {
    baseImage.loadPixels();
    applyColor(colors, baseImage.pixels, mapper.getImageToSignalLUT());
    baseImage.updatePixels();
    refreshMapImageFromBase();
  } else {
    refreshMapImageFromBase();
    mapImage.loadPixels();
    applyColorShifted(colors, mapImage.pixels, mapper.getImageToSignalLUT(), totalShift);
    mapImage.updatePixels();
  }
}

/**
 *
 */
public void openBrushEditor(AudioBrush brush) {
  setActiveBrush(brush);
  syncDrawingModeToBrush(activeBrush);
}

/**
 * Sets audioOut.gain.
 * @param g  gain value for audioOut, in decibels
 */
public void setAudioGain(float g) {
  audioOut.setGain(g);
  outputGain = audioOut.getGain();
}

/**
 * Sets audioOut.gain.
 * @param g   gain value for audioOut, in decibels
 */
public void adjustAudioGain(float g) {
  float ag = audioOut.getGain();
  ag += g;
  if (ag > 12.0f || ag < -64.0f) return;
  audioOut.setGain(ag);
  outputGain = audioOut.getGain();
}

/**
 * Sets Sampler instrument <code>pool</code> gain in dB.
 * @param g   gain increment or decrement, in decibels
 */
public void adjustInstrumentGain(float g) {
  if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) {
    adjustSamplerGain(g);
  } else if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR) {
    adjustGranGain(g);
  } else {
    return;
  }
}

/**
 * Sets Sampler instrument <code>pool</code> gain in dB.
 * @param g   gain increment or decrement, in decibels
 */
public void adjustSamplerGain(float g) {
  float pg = pool.getGainDb();
  pg += g;
  if (pg > 12.0f || pg < -64.0f) return;
  pool.setGainDb(pg);
  gainSlider.setValue(pg);
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
  gainSlider.setValue(gg);
}


/**
 * Sets the drawing mode.
 * @param newMode
 */
void setMode(DrawingMode newMode) {
  if (newMode == drawingMode) return;
  drawingMode = newMode;
  // Clear hover because the hover rules changed
  hoverBrush = null;
  hoverIndex = -1;
  // Choose what should be "active" in the new mode
  AudioBrush nextActive = null;
  switch (drawingMode) {
  case DRAW_EDIT_GRANULAR:
    nextActive = activeGranularBrush;
    break;
  case DRAW_EDIT_SAMPLER:
    nextActive = activeSamplerBrush;
    break;
  case PLAY_ONLY:
    nextActive = (activeBrush != null) ? activeBrush
      : (activeGranularBrush != null ? activeGranularBrush : activeSamplerBrush);
    break;
  default:
    {
    }
  }
  if (nextActive != null) {
    // Keep indices consistent: use stored index for the corresponding type
    int idx = (nextActive instanceof GranularBrush) ? activeGranularIndex : activeSamplerIndex;
    setActiveBrush(nextActive, idx);     // this also syncs GUI
  } else {
    // No selection for this mode
    activeBrush = null;
    resetConfigForMode();    // reset the GUI control palette
  }
  setControlsEnabled();        // enable or disable controls, depending on the drawing mode
}

/**
 * optimize grainCount for the activeBrush, most useful for granular brushes
 */
public void optimizeActiveBrush() {
  printGOptHints(hopScale);
  if (activeBrush != null) {
    activeBrush.cfg().resampleCount = optGrainCount;
    syncGuiFromConfig();
  }
}

/**
 * Reset tool config to defaults (copy, so default config never mutates).
 */
void resetConfigForMode() {
  if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR) {
    gConfig = defaultGranConfig.copy();
  } else if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) {
    gConfig = defaultSampConfig.copy();
  } else {
    // PLAY_ONLY: choose what you want the GUI to show
    // Option: keep last gConfig; or keep a neutral preset:
    // gConfig = defaultGranularConfig.copy();
  }
  syncGuiFromConfig();    // enable or disable controls, depending on the drawing mode
}

public synchronized void fadeOutGranularNow() {
  if (gDir != null) gDir.cancelAndReleaseAll();
}

public synchronized void stopGranularNow() {
  if (gDir != null) gDir.cancelAndStopAll();
}

public synchronized void suspendScheduledEvents() {
  synchronized (pointTimeLocsLock) {
    for (TimedLocation tl : this.pointTimeLocs) {
      tl.setStale(true);
    }
  }
  synchronized (samplerBrushEventsLock) {
    samplerBrushEvents.clear();
  }
  synchronized (grainTimeLocsLock) {
    for (TimedLocation tl : this.grainTimeLocs) {
      tl.setStale(true);
    }
    grainTimeLocs.removeIf(TimedLocation::isStale);    // necessary on fade-out
  }
}


public SamplerBrush generateSamplerBeatBrush(int count, int intervalMs) {
  ArrayList<Integer> times = new ArrayList<>();
  ArrayList<PVector> points = new ArrayList<>();
  int xCtr = width/2;
  int yCtr = height/2;
  int rad = height/2 - 32;
  float theta = PI/64.0f;
  float dec = (rad * 0.5f)/count;
  PVector p0 = new PVector(xCtr, yCtr + rad);
  points.add(p0);
  times.add(0);
  for (int i = 1; i < count; i++) {
    times.add(i * intervalMs);
    PVector nextPoint = PACurveUtility.rotateCoordAroundPoint(xCtr, yCtr + rad, xCtr, yCtr, theta * i);
    points.add(nextPoint);
    rad -= dec;
  }
  PACurveMaker curve = PACurveMaker.buildCurveMaker(points, times, millis());
  GestureGranularConfig.Builder cfg = new GestureGranularConfig.Builder().gainDb(-12.0f);
  cfg.pathMode = GestureGranularConfig.PathMode.ALL_POINTS;
  cfg.rdpEpsilon = 8.0f;
  cfg.curveSteps = 8;
  ADSRParams env = new ADSRParams(1.0f, 0.025f, 0.02f, 0.025f, 0.05f);
  cfg.env = env;
  SamplerBrush beatBrush = new SamplerBrush(curve, cfg);
  return beatBrush;
}

public GranularBrush generateGranularBeatBrush(int count, int intervalMs) {
  ArrayList<Integer> times = new ArrayList<>();
  ArrayList<PVector> points = new ArrayList<>();
  int xCtr = width/2;
  int yCtr = height/2;
  int rad = height/2 - 32;
  float theta = PI/64.0f;
  float dec = (rad * 0.5f)/count;
  PVector p0 = new PVector(xCtr, yCtr + rad);
  points.add(p0);
  times.add(0);
  for (int i = 1; i < count; i++) {
    times.add(i * intervalMs);
    PVector nextPoint = PACurveUtility.rotateCoordAroundPoint(xCtr, yCtr + rad, xCtr, yCtr, theta * i);
    points.add(nextPoint);
    rad -= dec;
  }
  PACurveMaker curve = PACurveMaker.buildCurveMaker(points, times, millis());
  GestureGranularConfig.Builder cfg = new GestureGranularConfig.Builder().gainDb(-3.0f);
  cfg.hopLengthSamples = 512;
  cfg.grainLengthSamples = 2048;
  cfg.burstGrains = 4;
  cfg.pathMode = GestureGranularConfig.PathMode.ALL_POINTS;
  cfg.rdpEpsilon = 8.0f;
  cfg.curveSteps = 8;
  // we can use the default envelope,
  // granularEnv: ADSRParams(1.0f, 0.005f, 0.02f, 0.9f, 0.025f);
  cfg.env = granularEnv;
  // or not
  // ADSRParams env = new ADSRParams(1.0f, 0.025f, 0.25f, 1.0f, 0.25f);
  // cfg.env = env;
  GranularBrush beatBrush = new GranularBrush(curve, cfg);
  return beatBrush;
}

/**
 * Trigger a sample at a random location.
 */
public void raindrops() {
  int signalPos = (int) random(samplelen, mapSize - samplelen - 1);
  int[] coords = mapper.lookupImageCoordShifted(signalPos, totalShift);
  this.runRaindropPointEvent(coords[0], coords[1]);
}

/**
 * Plays a sample and animates a point
 * @param x    x-coordinate of event
 * @param y    y-coordinate of event
 */
void runRaindropPointEvent(int x, int y) {
  if (random(1.0f) > 0.5f) {
    ensureGranularReady();
    runGranularPointEvent(x, y);
    return;
  }
  ensureSamplerReady();
  int signalPos = mapper.lookupSignalPosShifted(x, y, totalShift);
  float panning = map(x, 0, width, -0.875f, 0.875f);
  int len = calcSampleLen(noteDuration, 1.0f, 0.0625f);
  // playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pitch, float pan)
  // TODO vary gain and pitch, combine with granular jitter point events (eerie)
  float pitchRatio = AudioUtility.semitonesToRatio(-9.0f);
  samplelen = playSample(signalPos, len, samplerPointGain, envPreset("Pluck"), pitchRatio, panning);
  int durationMS = (int)(samplelen / sampleRate * 1000);
  pointTimeLocsAddPoint(new TimedLocation(x, y, durationMS + millis() + 50));
  // if (isVerbose) println("----- sampler point event, signalPos = "+ signalPos);
}


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
 * There's no key command to trigger this, yet. TODO decide if you want a key command.
 *
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
  } else {
    println("----->>> RESET audio and event points <<<------");
  }
}




/*----------------------------------------------------------------*/
/*                                                                */
/*                          EXPERIMENTS                           */
/*                                                                */
/*----------------------------------------------------------------*/


/**
 * Print suggested values for optimizing grain overlap for a brush.
 *
 * @param alpha
 */
void printGOptHints(float alpha) {
  if (activeBrush == null) return;

  GestureGranularConfig snap = activeBrush.snapshot();
  GestureSchedule schedule = scheduleBuilder.build(activeBrush.curve(), snap, audioOut.sampleRate());
  if (schedule == null || schedule.isEmpty()) {
    //System.out.println("---- Granular OptHints [activeBrush] ----");
    System.out.println("No schedule / empty schedule.");
    //System.out.println("----------------------------------------");
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
  //if (isVerbose) {
  //  println("-- path length px = "+ S);
  //  println("-- dHop = "+ dHop +", alpha = "+ alpha);
  //  println("-- targetSpacingPx = "+ targetSpacingPx);
  //}

  StringBuffer info = new StringBuffer();
  this.optGrainCount = calcGranularOptHints(
    "activeBrush",
    Nactual,
    TactualMs,
    snap.hopLengthSamples,
    snap.grainLengthSamples,
    audioOut.sampleRate(),
    (ArrayList<PVector>) schedule.points,
    targetSpacingPx, // d0
    1.0f, // wt
    0.25f, // ws
    info
    );
  //if (isVerbose) println(info.toString());
}

/**
 * Calculate optimal configuration settings for a granular brush.
 *
 * @return optimal number of samples if time duration is kept as is
 */
public int calcGranularOptHints(
  String tag,
  int N, float Tms,
  int hopSamples, int grainLenSamples,
  float sr,
  List<PVector> scheduledPoints, // the list you will actually schedule
  float targetSpacingPx, float wt, float ws,
  StringBuffer sb
  ) {
  // ---- GUI ranges ----
  final int   RESAMPLE_MIN = 2, RESAMPLE_MAX = 2048;
  final float DUR_MIN_MS   = 50f, DUR_MAX_MS   = 16000f; // bump from 10000 -> 16000 if desired

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
    sb.append("Compromise (wt=" + fmt(wt, 3) + ", ws=" + fmt(ws, 3) + ", d0=" + fmt(targetSpacingPx, 2) + "px): N*≈" +
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
