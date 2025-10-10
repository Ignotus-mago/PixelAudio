/*
 * This example application modifies the TutorialOne_04_Drawing code for the PixelAudio
 * library for Processing. Some of the changes:
 * 
 *   -- We use a custom MultiGen, hilbertRowOrtho(). This gen uses rows of Hilbert gens
 *      in row major order (left to right, top to bottom). Its signal path is revealed
 *      by using hues from the "colors" RGB array to color the display image.
 *   -- We load audio and image separately and use key commands 'w' (image to audio) 
 *      and 'W' (audio to image) to exchange data between image and audio.
 *   -- We add a raindrops() method to provide random audio events when the windowed 
 *      buffer is advancing automatically ('t' command). The raindrops cluster along the
 *      top edge and are useful for playing files with a recognizable time structure, like
 *      youthOrchestra.wav, the Spanish national anthem. 
 * 
 * Finally, we add commands to open a selected audio file and load all of it to a buffer. 
 * We use the buffer as a source for "audioSignal", an array of floats that is mapped to the 
 * display using the signal path. You could think of audioSignal as a window that moves 
 * through the buffer, transcoding audio data to brightness in the HSB color space and
 * then writing its values to the PImage "mapImage", which shows in the display window. 
 * The audioSignal array is the same size as mapImage, the display image. AudioSignal 
 * and mapImage use PixelAudioMapper "mapper" to map samples onto pixels. Mapper was
 * initialized with the MultiGen created by hiilbertRowOrtho(). 
 * 
 * In contrast to the AudioCapture example sketch, where we streamed a file from disk,
 * we don't play audio as we advance through the buffer, but we do control how fast we
 * advance. We play audio by drawing on the display image. We step through audio with  
 * WindowedBuffer. It provides better control over animation than the built-in
 * array rotation methods in PixelAudioMapper do, and it's a complete implementation 
 * of a circular buffer. 
 * 
 * The audio loaded to WindowedBuffer windowBuff does not change once it is loaded, 
 * until you load a new audio file. Data from windowBuff gets written to audioSignal
 * and transcoded to mapImage. When you click on the display window, the getSamplePos()
 * method calculates the precise location of a sample in the backing buffer of windowBuff
 * and passes it to the WFSamplerInstrumentPool instance "pool", which plays the audio
 * at the sample position. GetSamplePos() takes animation into account, too, if you are
 * using PixelAudioMapper's shift methods. 
 * 
 * Here are some of the ways I am using WindowedBuffer: 
 *   -- In the loadAudioFile() method, we open a file and load all of it into playBuffer.
 *   -- The audio data is copied to anthemBuffer.
 *   -- anthemBuffer is used to initialize a WindowedBuffer, windowBuff, with a mapSize window.
 *   -- playBuffer is trimmed to mapSize, the size of PixelAudioMapper mapper and the display image.
 *   -- Animation and transcoding image to audio use mapImage.pixels, audioSignal, and playBuffer.
 *   -- Audio events make use of anthemBuffer.
 *   -- You can step through the buffer either with the 't' command or the 'j' and 'J' commands. 
 *      The 't' command automates the window, shifting it on every frame
 *      The 'j' and 'J' commands step forward a half window or a whole window
 *   -- Generally, you can't run animation and windowing at the same time. 
 *   -- Audio events use anthemBuffer as a stable source of audio data, thus avoiding noise
 *      from threaded updates to audio data. See audioMousePressed() for the code. 
 *      The key bit of math is:
 *        position in anthemBuffer = position in playBuffer + this.windowBuff.getIndex();
 *   -- Animation and window shifting are turned off when you open a file, and returned 
 *      to previous settings after the file is loaded. This avoids thread conflicts and 
 *      crashes. Loading files automatically from code, without a dialog, can work around
 *      this restriction, but it's left as an exercise for the diligent coder. 
 *      
 * 
 * Comments on the individual methods indicate where some of the changes were made.
 * 
 *     1. Add hilbertRowOrtho().
 *     2. Modify loadAudioFile() and loadImageFile(), removing the writeAudioToImage()
 *        and writeImageToAudio() calls.
 *     3. Add key commands in parseKey() for writeAudioToImage() ('W')and writeImageToAudio() ('w').
 *     4. Add key commands in parseKey() to save to audio ('S') and save to image ('s'). 
 *     5. Create methods playPoints(int startTime) and playBrushStrokes(int offset) 
 *        to automate playback of brushstrokes. See DrawingTools tab.
 *     6. Add a key command ('p') to parseKey() to call playBrushStrokes(). 
 *     7. Modify the updateAudio() method: comment out the writeAudioToImage() call,
 *        because we will handle audio at full resolution whenever possible. 
 *        Rotate the audioSignal array when isAnimating == true.
 *     8. Add a section of WindowedBuffer variables.
 *     9. Add methods to play, mute and pause audio from WindowedBuffer. 
 *        
 *     
 * To play with the code:
 *     
 *     1. Launch the sketch. 
 *        Rainbow stripes appear in display window.
 *     2. Press 'o' and load an audio file to the audio buffer (saucer_mixdown.wav, in the 
 *        PixelAudio/examples/examples_data/ folder is a good choice).
 *        Display does not change, Click on the display image to test the audio. 
 *     3. Press 't' to step through audio from the selected file and write to the display. WindowBuff
 *        steps through anthemBuffer by windowHopSize samples and returns an array of floats for 
 *        every call to its nextWindow() method. WindowBuff passes the array of floats to audioSignal,  
 *        which gets transcoded to mapImage and the display as animated gray values.
*         See the WindowedBuffer class for more information.  
 *     4. Unlike the AudioCapture example, which can play a file automatically, MusicBoxBuffer
 *        requires mouseClicks or interactive drawing to produce sound.  
 *     5. Click on the image to play samples. Try it while the window is stepping through 
 *        the buffer ('t' key command) and while it is animating (spacebar key command). To avoid
 *        problems, it is generally not possible to animate and step the window at the same time.  
 *     6. Press 'd' to start drawing -- draw a few brushstrokes. Hover on brushstrokes to activate 
 *        them--this will work even when drawing is off. Click on brushstrokes to play audio. 
 *        Audio samples will play, activated by the brushstrokes. 
 *     7. Press 'x' to delete the current active brush shape or the oldest brush shape.
 *        Press 'X' to delete the most recent brush shape.
 *     7. Press 'b' to play the brushstrokes.
 *        Audio corresponding to brushstrokes is played.
 *     8. Press the spacebar to run animation.
 *        The image pixel array and the audio signal array are both rotated and written along
 *        the signal path. 
 *        The audio played by the brushstrokes changes as the animation progresses.
 *     9. Press 't' to start windowed buffer traversal again. Experiment with different audio sources, 
 *        directions of drawing, etc. 
 * 
 * You can change the hilbertRowOrtho() MultiGen method arguments to fit other other audio and image files. 
 * It might be nice to change those circles that show when sound is played to something smaller and transparent...
 * 
 * 
 * Here are the key commands for this sketch:
 * 
 * Press ' ' to  start or stop animation.
 * Press 'd' to turn drawing on or off.
 * Press 'm' to turn mouse tracking on or off.
 * Press 'c' to apply color from image file to display image.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage .
 * Press 'p' to play brushstrokes with a time lapse between each.
 * Press 'o' or 'O' to open an audio or image file.
 * Press 's' to save image to a PNG file.
 * Press 'S' to save audio to a WAV file.
 * Press ']' to jump half window ahead in the buffer.
 * Press '[' to jump half window back in the buffer.
 * Press '}' to jump whole window ahead in the buffer.
 * Press '{' to jump whole window back in the buffer.
 * Press 'r' to rewind the windowed audio buffer.
 * Press UP arrow to increase gain by 3.0 dB.
 * Press DOWN arrow to decrease gain by -3.0 dB.
 * Press 'u' to mute or unmute audio.
 * Press 't' to turn audio stream on or off.
 * Press 'w' to write the image colors to the audio buffer as transcoded values.
 * Press 'W' to write the audio buffer samples to the image as color values.
 * Press 'x' to delete the current active brush shape or the oldest brush shape.
 * Press 'X' to delete the most recent brush shape.
 * Press 'y' to turn rain (random audio events) on and off.
 * Press 'z' to reset brushstrokes and audio buffer (you may need to reload audio).
 * Press 'V' to record a video.
 * Press 'h' or 'H' to show help and key commands. * 
 */

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
/*                        FILE I/O VARIABLES */
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
/*                          AUDIO VARIABLES */
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
WFSamplerInstrument synth;      // class for audio playback, but we use pool in this sketch
// instrument pool, for up to 48 simultaneous voices
WFSamplerInstrumentPool pool;   // a pool of WFSamplerInstruments
int poolSize = 48;              // number of WFSamplerInstruments in the pool
int perInstrumentVoices = 1;    // number of voices for each WFSamplerInstrument
// default ADSR params: maxAmp, attack, decay, sustain, release
ADSRParams adsr = new ADSRParams(
    0.25f,    // maxAmp
    0.8f,     // attack (s)
    0.25f,    // decay (s)
    0.125f,   // sustain level
    0.125f    // release (s)
);
// gain and muting of audioOut
final float defaultGain = -18.0f;
float gain = defaultGain;        // gain for audioOut, decibels
boolean isMuted = false;         // flag for muting audioOut

// interaction variables for audio
int sampleX;                    // keep track of coordinates associated with audio samples
int sampleY;
boolean isIgnoreOutsideBounds = true;      // set to true to ignore points outside bounds when drawing
ArrayList<TimedLocation> timeLocsArray;    // a list of timed events 


/* ------------------------------------------------------------------ */
/*                   ANIMATION AND VIDEO VARIABLES */
/* ------------------------------------------------------------------ */

int shift = -4;                  // number of pixels to shift the animation
/** track the shift in index location when we animate pixels with PixelAudioMapper.rotateLeft() */
int totalShift = 0;              // cumulative shift from array rotation animation
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
/*                         DRAWING VARIABLES */
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
public float epsilon = 8.0f;                        // controls how much reduction is applied to points
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

float blend = 0.5f;                                 // TODO implement blending of images
boolean isUseBlend = true;                          // for future use

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
/** turn random audio events on or off, see raindrops() method in AudioTools tab */
boolean isRaining = false;

// ** YOUR VARIABLES ** //


@Override
public void settings() {
  size(cols * genWidth, rows * genHeight);
}

@Override
public void setup() {
  surface.setTitle("Music Box Buffer Example");
  frameRate(24);
  // initialize our library
  pixelaudio = new PixelAudio(this);
  // we create a PixelMapGen implemented as a MultiGen, with dimensions equal to our display window
  // hilbertRowOrtho() is a very flexible way to create a multigen. 
  // in other sketches we've used hilbertLoop3x2()
  multigen = hilbertRowOrtho(cols, rows, genWidth, genHeight);
  // create a PixelAudioMapper to handle the mapping of pixel colors to audio samples
  mapper = new PixelAudioMapper(multigen);
  // mapSize is the length of mapImage.pixels and audioSignal
  mapSize = mapper.getSize();
  println("---- mapSize == "+ mapSize);
  // create an array of rainbow colors with mapSize elements
  colors = getColors(mapSize);
  initImages();
  initAudio();
  initDrawing();
  showHelp();
  println("--- display window dimensions: ", width, height);
}

// terminate audio threads when the sketch exits
@Override
public void stop() {
  if (synth != null) synth.close();
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
 * This method creates rows of HilbertGens, starting each row from the left
 * and adding gens. The odd rows are flipped vertically and the even rows are
 * unchanged. The unchanged HilbertGen starts at upper left corner and ends at 
 * upper right corner, so this provides some possibilities of symmetry between rows.
 * The path is not continuous. 
 * 
 * @param cols    number of columns of gens wide
 * @param rows    number of rows of gens high
 * @param genW    width of each gen (same as genH and a power of 2)
 * @param genH    height of each gen 
 * @return        a MultiGen composed of cols * rows PixelMapGens
 */
public MultiGen hilbertRowOrtho(int cols, int rows, int genW, int genH) {
  // list of PixelMapGens
  ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
  // list of x,y coordinates for placing gens from genList
  ArrayList<int[]> offsetList = new ArrayList<int[]>();
  for (int y = 0; y < rows; y++) {
    for (int x = 0; x < cols; x++) {
      if (y % 2 == 0) {
        genList.add(new HilbertGen(genW, genH, AffineTransformType.NADA));
      }
      else {
        genList.add(new HilbertGen(genW, genH,  AffineTransformType.NADA));
      }
      offsetList.add(new int[] {x * genW, y * genH});
    }
  }
  return new MultiGen(width, height, offsetList, genList);
}


/**
 * Initializes mapImage with the colors array. MapImage will handle the color data for mapper
 * and also serve as our display image.
 */
public void initImages() {
  mapImage = createImage(width, height, ARGB);
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
  mapImage.updatePixels();
}
  

/**
 * Initializes the line, curve, and brushstroke drawing variables. 
 * Note that timeLocsArray has been initialized by initAudio(), though it
 * will be used for point events in the drawing code, too. 
 */
public void initDrawing() {
  currentPoint = new PVector(-1, -1);
  brushShapesList = new ArrayList<PACurveMaker>();
}


public void draw() {
  image(mapImage, 0, 0);    // draw mapImage to the display window
  handleDrawing();          // handle interactive drawing and audio events created by drawing
  if (isAnimating) {        // animation is different from windowed buffer
    animate();              // rotate mapImage.pixels by shift number of pixels
    updateAudio();          // rotate audioSignal (we could just shift the index...)
  }
  if (isListening) {        // windowed buffer is advancing, but not at the same time animation
    updateAudio();          // update audioSignal with WindowedBuffer.nextWindow()
    drawSignal();           // transcode audioSignal and write it to mapImage.pixels
    if (isRaining && random(10) > 9) raindrops();    // a little rain never hurt anyone
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
  if (step >= animSteps) {
  if (isRecordingVideo) {
    isRecordingVideo = false;
    videx.endMovie();
    isAnimating = oldIsAnimating;
    println("--- Completed video at frame " + animSteps);
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
      println("-- video recording frame " + step + " of " + animSteps);
    }
  }
}


/**
 * Renders a frame of animation: moving along the signal path, copies mapImage pixels into rgbSignal, 
 * rotates them shift elements left, writes them back to mapImage along the signal path.
 * 
 * @param step   current animation step
 */
public void renderFrame(int step) {
  mapImage.loadPixels();
  // get the pixels in the order that the signal path visits them
  int[] rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
  // rotate the pixel array
  PixelAudioMapper.rotateLeft(rgbSignal, shift);
  // keep track of how much the pixel array (and the audio array) are shifted
  totalShift += shift;
  // write the pixels in rgbSignal to mapImage, following the signal path
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, mapSize);
  mapImage.updatePixels();
}


/**
  * Transcode directly from audioSignal to mapImage.pixels, the display image.
  * When we are stepping through an audio buffer with BufferedWindow, audioSignal
  * contains the most recent window, which is exactly the same size as mapImage.pixels.
  * By default, we transcode audio to the Brightness channel of HSB, preserving Hue 
  * and Saturation in the mapImage. 
  */
public void drawSignal() {
  // we transcode directly from audioSignal to mapImage.pixels, with no intermediate steps or copies
  mapImage.loadPixels();
  mapper.mapSigToImg(audioSignal, mapImage.pixels, chan);
  mapImage.updatePixels();
}  

  
/**
 * Updates global variable audioSignal, either by rotating array or getting 
 * a new window of values from the audio buffer.
 */
public void updateAudio() {
  if (isListening) {
    audioSignal = windowBuff.nextWindow(); 
    return;   // don't animate if we're moving through the windowed buffer
  }
  if (isAnimating) {
    PixelAudioMapper.rotateLeft(audioSignal, shift);
  }
}
  
/**
 * Handles user's drawing actions, draws previously recorded brushstrokes, 
 * tracks and generates animation and audio events. 
 */
public void handleDrawing() {  
  // draw current brushShapes 
  drawBrushShapes();
  if (isDrawMode) {
    if (mousePressed) {
      addPoint(mouseX, mouseY);
    }
    if (allPoints != null && allPoints.size() > 2) {
      PACurveUtility.lineDraw(this, allPoints, dragColor, dragWeight);
    }
    if (curveMaker != null) curveMakerDraw();
  } 
  runCurveEvents();
  runPointEvents();
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
  setSampleVars(mouseX, mouseY);      
  if (this.isDrawMode) {
    initAllPoints();
  } 
  else {
    handleMousePressed(mouseX, mouseY);
  }
}


/**
 * Used for interactively setting the amount of pixel array shift when animating.
 * TODO, since this app is a demo for WindowedBuffer, we can probably do without 
 * setting shift interactively. 
 */
public void mouseDragged() {
  if (isTrackMouse) {
    shift = abs(width/2 - mouseX);
    if (mouseY < height/2) 
      shift = -shift;
    writeToScreen("shift = "+ shift, 16, 24, 24, false);
  }
}

public void mouseReleased() {
  setSampleVars(mouseX, mouseY);      
  if (isAnimating && isTrackMouse) {
    // println("----- animation shift = "+ shift);
  }
  if (isDrawMode && allPoints != null) {
    if (allPoints.size() > 2) {    // add curve data to the brush list
      initCurveMaker();
    }
    else {              // handle the event as a click
      handleMousePressed(mouseX, mouseY);
    }
    allPoints.clear();
  }
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
  * This opens up many possibilities and a some dangers, too. 
  * @see TutorialOneUDP.java (Eclipse) or TutorialOne_05_UDP (Processing)
  * for an example of external calls to parseKey().
  * 
  * @param key
  * @param keyCode
  */
public void parseKey(char key, int keyCode) {
  switch(key) {
    case ' ': //  start or stop animation
      if (isListening) {
        println("-- animation is not run when you are capturing an audio stream");      
      }    
    else {
      isAnimating = !isAnimating;
      println("-- animation is " + isAnimating);
    }
    break;
  case 'd': // turn drawing on or off
    // turn off mouse tracking that sets shift value for animation
    isTrackMouse = false; 
    // turn off animation (you can try drawing with it on, just press the spacebar)
    isAnimating = false;
    isDrawMode = !isDrawMode;
    println(isDrawMode ? "----- Drawing is turned on" : "----- Drawing is turned off");
    if (!isDrawMode)
      this.curveMaker = null;
    break;
//    case 'm': // turn mouse tracking on or off, a distraction better omitted in WindowBuffer sketch
//      isTrackMouse = !isTrackMouse;
//      println("-- mouse tracking is " + isTrackMouse);
//      break;
  case 'c': // apply color from image file to display image
    chooseColorImage();
    break;
  case 'k': // apply the hue and saturation in the colors array to mapImage 
    mapImage.loadPixels();
    applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
    mapImage.updatePixels();
    break;
  case 'p': // play brushstrokes with a time lapse between each
    playBrushstrokes(2000);
    break;
  case 'o': case 'O': // open an audio or image file
    chooseFile();
    break;
  case 's': // save image to a PNG file
    saveToImage();
    break;
  case 'S': // save audio to a WAV file
    saveToAudio();
    break;
  case ']': // jump half window ahead in the buffer
    moveAudioWindow(windowBuff.getWindowSize()/2);
    drawSignal();
    break;
  case '[': // jump half window back in the buffer
    moveAudioWindow(-windowBuff.getWindowSize()/2);
    drawSignal();
    break;
  case '}': // jump whole window ahead in the buffer
    moveAudioWindow(windowBuff.getWindowSize());
    drawSignal();
    break;
  case '{': // jump whole window back in the buffer
    moveAudioWindow(-windowBuff.getWindowSize());
    drawSignal();
    break;
  case 'r': // rewind the windowed audio buffer
    resetAudioWindow();
    drawSignal();
    break;
  case 'u': // mute audio
    isMuted = !isMuted;
    if (isMuted) {
      audioOut.mute();
    }
    else {
      audioOut.unmute();
    }
    String msg = isMuted ? "muted" : "unmuted";
    println("---- audio out is "+ msg);
    break;
  case 't': // turn stream capture on or off
    if (anthemSignal != null) {
      isListening = listenToAnthem(!isListening);
      if (isListening) 
        isAnimating = false;    // generally don't want to animate and stream at the same time
      totalShift = 0;
    }
    else {
      println("---- You need to load an audio file before you can window through the buffer.");
    }
    break;
  case 'w': // write the image colors to the audio buffer as transcoded values
    // prepare to copy image data to audio variables
    // resize the buffer to mapSize, if necessary -- signal will not be overwritten
    if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
    audioSignal = playBuffer.getChannel(0);
    writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
    // now that the image data has been written to audioSignal, set playBuffer channel 0 to the new audio data
    playBuffer.setChannel(0, audioSignal);
    audioLength = audioSignal.length;
    println("--->> Wrote image to audio as audio data.");
    break;
  case 'W': // write the audio buffer samples to the image as color values
    writeAudioToImage(audioSignal, mapper, mapImage, chan);
    println("--->> Wrote audio to image as pixel data.");
    break;
  case 'x': // delete the current active brush shape or the oldest brush shape
    if (activeBrush != null) {
      removeActiveBrush();
    }
    else {
      removeOldestBrush();
    }
    break;
  case 'X': // delete the most recent brush shape
    removeNewestBrush();
    break;
  case 'y': // turn rain (random audio events) on and off
    isRaining = !isRaining;
    println("---- isRaining = "+ isRaining);
    break;
  case 'z': // reset brushstrokes and audio buffer (you may need to reload audio)
  isListening = listenToAnthem(false);
  reset(true);
  break;
  case 'V': // record a video
    // records a complete video loop with following actions:
    // Go to frame 0, turn recording on, turn animation on.
    // This will record a complete video loop, from frame 0 to the
    // stop frame value in the GUI control panel.
    step = 0;
    renderFrame(step);
    isRecordingVideo = true;
    oldIsAnimating = isAnimating;
    isAnimating = true;
    break;
  case 'h': case 'H': // show help and key commands
    showHelp();
    break;
  default:
    break;
  }
}

/**
  * Rewinds WindowedBuffer instance windowBuff to the beginning of the audio buffer.
  */
public void resetAudioWindow() {
  windowBuff.reset();
  audioSignal = windowBuff.nextWindow();
  playBuffer.setChannel(0, audioSignal);
  totalShift = 0;
}

/**
  * Moves WindowedBuffer instance windowBuff's window to the index howFar.
  */
public void moveAudioWindow(int howFar) {
  int i = this.windowBuff.getIndex();
  audioSignal = windowBuff.gettWindowAtIndex(i + howFar);
  playBuffer.setChannel(0, audioSignal);
  totalShift = 0;
}

/**
 * Sets audioOut.gain.
 * @param g   gain value for audioOut, in decibels
 */
public void setAudioGain(float g) {
  audioOut.setGain(g);
  gain = audioOut.getGain();
}

/**
 * to generate help output, run RegEx search/replace on parseKey case lines with:
 * // case ('.'): // (.+)  * // println(" * Press $1 to $2.");
 */
public void showHelp() {
  println(" * Press ' ' to  start or stop animation.");
  println(" * Press 'd' to turn drawing on or off.");
  println(" * Press 'c' to apply color from image file to display image.");
  println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage .");
  println(" * Press 'b' to play brushstrokes with a time lapse between each.");
  println(" * Press 'o' or 'O' to open an audio or image file.");
  println(" * Press 's' to save image to a PNG file.");
  println(" * Press 'S' to save audio to a WAV file.");
  println(" * Press ']' to jump half window ahead in the buffer.");
  println(" * Press '[' to jump half window back in the buffer.");
  println(" * Press '}' to jump whole window ahead in the buffer.");
  println(" * Press '{' to jump whole window back in the buffer.");
  println(" * Press 'r' to rewind the windowed audio buffer.");
  println(" * Press UP arrow to increase gain by 3.0 dB.");
  println(" * Press DOWN arrow to decrease gain by 3.0 dB.");
  println(" * Press 'u' to mute audio.");
  println(" * Press 't' to turn stream capture on or off.");
  println(" * Press 'w' to write the image colors to the audio buffer as transcoded values.");
  println(" * Press 'W' to write the audio buffer samples to the image as color values.");
  println(" * Press 'x' to delete the current active brush shape or the oldest brush shape.");
  println(" * Press 'X' to delete the most recent brush shape.");
  println(" * Press 'y' to turn rain (random audio events) on and off.");
  println(" * Press 'z' to reset brushstrokes and audio buffer (you may need to reload audio).");
  println(" * Press 'V' to record a video.");
  println(" * Press 'h' or 'H' to show help and key commands.");
}


/**
 * Utility method for applying hue and saturation values from a source array of RGB values
 * to the brightness values in a target array of RGB values, using a lookup table to redirect indexing.
 * Available as a static method in PixelAudio class PixelAudioMapper.
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


// ------------------------------------------- //
//               UTILITY METHODS               //
// ------------------------------------------- //
