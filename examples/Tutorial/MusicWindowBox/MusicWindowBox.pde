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

//audio library
import ddf.minim.*;
import ddf.minim.ugens.*;

//video export library
import com.hamoid.*;

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
float sampleRate = 48000;       // a critical value for audio playback and file i/o, see the setup method
float[] audioSignal;            // the audio signal as an array of floats
MultiChannelBuffer playBuffer;  // a data structure allocated for storing and playing the audio signal
int samplePos;                  // an index into the audio signal, can be set by a mouse click on the display image
int audioLength;                // length of the audioSignal, same as the number of pixels in the display image
// SampleInstrument setup
// TODO find a less clunky way of setting durations, only of use in calcSampleLen()
float sampleScale = 4;          // factor in determining audio event duration 
int sampleBase = (int) (sampleRate/sampleScale);     // 1/4 of a second
int samplelen = (int) (sampleScale * sampleBase);    // one second
Sampler audioSampler;           // minim class for sampled sound
WFSamplerInstrument synth;      // class for audio playback, not use at the moment
// instrument pool, 16 WFSamplerInstruments with 4 voices each, for up to 64 simultaneous voices
  WFSamplerInstrumentPool pool;   // a pool of WFSamplerInstruments
  int poolSize = 16;              // number of WFSamplerInstruments in the pool
  int perInstrumentVoices = 4;    // number of voices for each WFSamplerInstrument
  // default ADSR params: maxAmp, attack, decay, sustain, release
  ADSRParams adsr = new ADSRParams(
      0.25f,    // maxAmp
      0.8f,     // attack (s)
      0.25f,    // decay (s)
      0.125f,   // sustain level
      0.125f    // release (s)
  );
  final float defaultGain = -18.0f;
  float gain = -18.0f;             // gain for audioOut, decibels
  boolean isMuted = false;         // flag for muting audioOut

// interaction variables for audio
int sampleX;                    // keep track of coordinates associated with audio samples
int sampleY;
boolean isIgnoreOutsideBounds = true;      // set to true to ignore points outside bounds when drawing
ArrayList<TimedLocation> timeLocsArray;    // a list of timed events 


/* ------------------------------------------------------------------ */
/*                   ANIMATION AND VIDEO VARIABLES                    */
/* ------------------------------------------------------------------ */

int shift = -4;                  // number of pixels to shift the animation
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

float blend = 0.5f;
boolean isUseBlend = true;

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
/** track the shift in index location when we animate pixels with PixelAudioMapper.rotateLeft() */
int animShift = 0;
/**  */
boolean isRaining = false;

// ** YOUR VARIABLES ** Variables for YOUR CLASS may go here **  //



/// lots to fill in here



// ------------------------------------------- //
//            WINDOWED BUFFER CLASS            //
// ------------------------------------------- //

public class WindowedBuffer {
  private final float[] buffer;   // circular source
  private final float[] window;   // reusable window array
  private final int windowSize;   // number of samples in a window
  private int hopSize;            // step between windows
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
