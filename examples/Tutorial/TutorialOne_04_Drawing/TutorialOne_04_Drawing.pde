/*
 * This example application continues the Tutorial One sequence for the PixelAudio
 * library for Processing. To the previous tools for reading and writing and transcoding
 * audio and image files, triggering audio events, and animating pixels and changing
 * audio sample order, it adds the capability of drawing in the display window to 
 * create brush shapes that can be used to trigger audio events. 
 * 
 * The drawing tools and commands are a substantial addition. To implement them 
 * we call on a whole new package of code, net.paulhert.pixelaudio.curves. We make 
 * numerous edits to the previous tutorial, TutorialOneAnimation:
 *     
 *     -- We add a raft of variables to handle drawing by tracking the mouse and 
 *        converting accumulated points into a reduced point set and then 
 *        into Bezier curve data. Here's an outline of how drawing works in the UI:
 *        
 *          Load an audio or image file to the display.
 *          Drawing starts when isDrawMode is set to true ('d' key command).
 *          Press 'd' to set isDrawMode to true (the console will inform you).
 *          Press and drag the mouse to draw a line, accumulating points into allPoints.
 *          Release the mouse to create a brushstroke -- audio and animation will play.
 *              The accumulated points in allPoints are used to create a PACurveMaker instance.
 *              PACurveMaker uses the RDP algorithm to reduce the number of points. 
 *              PACurveMaker uses the reduced point set to generate a Bezier curve.
 *              The Bezier curve is used to model a brushstroke shape.
 *              All these data elements are available from the PACurveMaker instance.
 *              The PACurveMaker instance is added to a list of PACUrveMakers, brushShapesList.
 *              BrushShapeList handles drawing curves to the screen. 
 *          Draw some more brushstrokes. 
 *          Press 'd' to turn drawing off.
 *          Click on a brushstroke to activate it. 
 *                    
 *          NOTE: Apparent limits on how many audio events can be playing simultaneously suggest that 3 
 *          is the maximum number of actively playing brushstrokes. Otherwise, noise and glitching happen.
 *          I'm working on figuring out how to get around this limit. 
 *          
 *     -- We add several new methods, in the DRAWING METHODS section:
 *     
 *          public void initAllPoints()
 *          public void handleMousePressed()
 *          public void addPoint()
 *          public void playPoints()
 *          public synchronized void storeCurveTL(ListIterator<PVector> iter, int startTime)
 *          public void initCurveMaker()
 *          public int[] reconfigureTimeList(int[] timeList)
 *          public ArrayList<Integer> reconfigureTimeList(ArrayList<Integer> timeList)
 *          public void drawBrushShapes()
 *          public void curveMakerDraw()
 *          public synchronized void runCurveEvents()
 *          public synchronized void runPointEvents()
 *          public boolean mouseInPoly(ArrayList<PVector> poly)
 *          public void reset(boolean isClearCurves)
 *          
 *        See the JavaDocs information for each method for a description of what it does.
 *        Also see the initDrawing() method in the main section. 
 *        
 *        SEE the JavaDoc entries for PACurveMaker, PACurveMakerUtility, PABezShape and the
 *        other classes in the net.paulhertz.pixelaudio.curves package for more information
 *        about curve modeling. 
 *        
 *     -- We reconfigure code for handling mouse events and TimedLocation events. The mouse event
 *        tracking starts in mousePressed(), when isDrawMode == true. While the mouse is down, calls
 *        to handleDrawing() in the draw() method accumulate points to allPoints. When the mouse is 
 *        released, allPoints is used to initialize a PACurveMaker. 
 *        
 *     -- In the draw() method, a call to handleDrawing() draws brush shapes and detects user interaction
 *        with the brush shapes by calling drawBrushShapes(). If a brush shape is flagged as active, a
 *        mousePressed event can trigger its audio and animation events by calling handleMousePressed().
 *        This will generate TimedLocation objects that trigger audio and animation events. 
 *        
 *     -- There are now two lists of TimedLocation events, timeLocsArray for point events, just like in 
 *        earlier versions of this tutorial, and curveTLEvents for brush events. The handleDrawing() method
 *        takes care of both lists with calls to runCurveEvents() and runPointEvents().
 *        
 * 
 * In the last part of this tutorial, we'll get to:
 * -- UDP communication with Max and other media applications
 * 
 * 
 * Here are the key commands for this sketch:
 * 
 * Press ' ' to start or stop animation.
 * Press 'd' to turn drawing on or off.
 * Press 'm' to turn mouse tracking on or off.
 * Press 'c' to apply color from an image file to the display image.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage.
 * Press 'o' or 'O' to open an audio or image file.
 * Press 'h' or 'H' to show help and key commands.
 * Press 'V' to record a video.
 * 
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
int genWidth = 512;        // width of multigen PixelMapGens
int genHeight = 512;       // height of  multigen PixelMapGens
PixelAudioMapper mapper;   // object for reading, writing, and transcoding audio and image data
int mapSize;               // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
PImage mapImage;           // image for display
PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;
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
 * Audio playback support is added with the audio variables and audio methods 
 * (below, in Eclipse, in a tab, in Processing). You will also need the 
 * WFInstrument and TimedLocation classes. In setup(), call initAudio(), then
 * add a mousePressed() method that calls audioMousePressed(mouseX, mouseY)
 * and call runTimeArray() in your draw method. 
 */
/** Minim audio library */
Minim minim;                    // library that handles audio 
AudioOutput audioOut;           // line out to sound hardware
boolean isBufferStale = false;  // flags that audioBuffer needs to be reset
int sampleRate = 48000;         // a critical value for display and audio, see the setup method
float[] audioSignal;            // the audio signal as an array of floats
MultiChannelBuffer playBuffer;  // a buffer for playing the audio signal
int samplePos;                  // an index into the audio signal, selected by a mouse click on the display image
float[] leftSamples;            // audio data for the left channel of a stereo file
float[] rightSamples;           // audio data for the right channel of a stereo file
int audioLength;                // length of the audioSignal, same as the number of pixels in the display image
// SampleInstrument setup
float sampleScale = 4;          // factor in determining audio event duration
int sampleBase = (int) (sampleRate/sampleScale);
int samplelen = (int) (sampleScale * sampleBase);
Sampler audioSampler;           // minim class for sampled sound
WFInstrument instrument;        // local class to wrap audioSampler
// ADSR and its parameters
ADSR adsr;                      // good old attack, decay, sustain, release
float maxAmplitude = 0.7f;
float attackTime = 0.8f;
float decayTime = 0.5f;
float sustainLevel = 0.125f;
float releaseTime = 0.5f;

// interaction variables for audio
int sampleX;                    // keep track of coordinates associated with audio samples
int sampleY;
ArrayList<TimedLocation> timeLocsArray;    // a list of timed events 


/* ------------------------------------------------------------------ */
/*                   ANIMATION AND VIDEO VARIABLES                    */
/* ------------------------------------------------------------------ */

int shift = 4;                  // number of pixels to shift the animation
boolean isAnimating = false;
boolean oldIsAnimating;
boolean isTrackMouse = true;
// animation variables
int animSteps = 720;            // how many steps in an animation loop
boolean isRecordingVideo = false;    // are we recording? (only if we are animating)
int videoFrameRate = 24;        // fps, frames per second
int step;                       // number of current step in animation loop
VideoExport videx;   // hamoid library class for video export (requires ffmpeg)
  
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
public int polySteps = 8;                           // number of steps in polygon representation of a Bezier curve
public PACurveMaker curveMaker;                     // class for tracking and storing drawing data
public ArrayList<PVector> eventPoints;              // list of points stored in or loaded from a PACurveMaker
public ListIterator<PVector> eventPointsIter;       // iterator for eventPoints
int eventStep = 90;                                 // milliseconds between events
public ArrayList<TimedLocation> curveTLEvents;      // a list of TimedLocation instances 
public ArrayList<PACurveMaker> brushShapesList;     // a list of PACurveMaker instances with recorded drawing data
public PACurveMaker activeBrush;                    // the currently active PACurveMaker, collecting points as user drags the mouse
public int activeIndex = 0;                         // index of current brush in brushShapesList, useful for UDP/OSC messages
int newBrushColor = color(144, 34, 42, 233);        // color of the new brushstroke
int polyPointsColor = color(233, 199, 144, 192);    // color for polygon representation of Bezier curve associated with a brushstroke
int activeBrushColor = color(144, 89, 55, 233);     // color for the active brush
int readyBrushColor = color(34, 89, 55, 233);       // color for a brushstroke when ready to be clicked

/* end drawing variables */

// ** YOUR VARIABLES ** Variables for YOUR CLASS may go here **  //

  
public void settings() {
  size(3 * genWidth, 2 * genHeight);
}

public void setup() {
  frameRate(24);
  // initialize our library
  pixelaudio = new PixelAudio(this);
  // create a PixelMapGen subclass such as MultiGen, with dimensions equal to out display window
  // the call to hilbertLoop3x2 produces a MultiGen that is 3 * genWidth x 2 * genHeight, where
  // genWidth == genHeight and genWidth is a power of 2 (a restriction on Hilbert curves)
  multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
  // create a PixelAudioMapper to handle the mapping of pixel colors to audio samples
  mapper = new PixelAudioMapper(multigen);
  // keep track of the area of the PixelAudioMapper
  mapSize = mapper.getSize();
  // create an array of rainbow colors with mapSize elements
  colors = getColors(mapSize);
  initImages();
  initAudio();
  initDrawing();
  showHelp();
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
  image(mapImage, 0, 0);
  // runTimeArray();    // animate audio event markers
  handleDrawing();
  if (isAnimating) {
    animate();
    updateAudio();
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
 * Renders a frame of animation.
 * 
 * @param step   current animation step
 */
public void renderFrame(int step) {
  mapImage.loadPixels();
  int[] rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
  PixelAudioMapper.rotateLeft(rgbSignal, shift);
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, mapSize);
  mapImage.updatePixels();
}
  
/**
 * Transcodes color channel data from mapImage to audio format and writes it to the audio signal and buffer.
 */
public void updateAudio() {
  writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
  // now that the image data has been written to audioSignal, set playBuffer channel 0 to the new audio data
  playBuffer.setChannel(0, audioSignal);
  audioLength = audioSignal.length;
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
      addPoint();
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
 * The built-in mousePressed handler for Processing, but note that it forwards mouse coords to audiMousePressed().
 */
public void mousePressed() {
  // println("mousePressed:", mouseX, mouseY);
  if (this.isDrawMode) {
    initAllPoints();
  } 
  else {
    handleMousePressed();
  }
}

public void mouseDragged() {
    if (isTrackMouse) {
      shift = abs(width/2 - mouseX);
      if (mouseY < height/2) 
        shift = -shift;
    }
}

public void mouseReleased() {
  if (isAnimating && isTrackMouse) {
    println("----- animation shift = "+ shift);
  }
  if (isDrawMode && allPoints != null) {
    if (allPoints.size() > 2) {    // add curve data to the brush list
      initCurveMaker();
    }
    else {              // handle the event as a click
      handleMousePressed();
    }
    allPoints.clear();
  }
}
/**
 * built-in keyPressed handler, forwards events to parseKey
 */
public void keyPressed() {
  parseKey(key, keyCode);    
}
/**
 * Handles key press events passed on by the built-in keyPressed method. 
 * By moving key event handling outside the built-in keyPressed method, 
 * we make it possible to post key commands without an actual key event.
 * Methods and interfaces and even other threads can call parseKey(). 
 * This opens up many possibilities and a some dangers, too.  
 * 
 * @param key
 * @param keyCode
 */
public void parseKey(char key, int keyCode) {
  switch(key) {
  case ' ': //  start or stop animation
    isAnimating = !isAnimating;
    println("-- animation is " + isAnimating);
    break;
  case 'd': // turn drawing on or off
    // turn off mouse tracking that sets shift value for animation
    isTrackMouse = false; 
    // turn off animation (you can try drawing with it on, just press the spacebar)
    isAnimating = false;
    isDrawMode = !isDrawMode;
    println(isDrawMode ? "----- Drawing is turned on" : "----- Drawing is turned off");
    break;
  case 'm': // turn mouse tracking on or off
    isTrackMouse = !isTrackMouse;
    println("-- mouse tracking is " + isTrackMouse);
    break;
  case 'c': // apply color from image file to display image
    chooseColorImage();
    break;
  case 'k': // apply the hue and saturation in the colors array to mapImage 
    mapImage.loadPixels();
    applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
    mapImage.updatePixels();
    break;
  case 'o': case 'O': // open an audio or image file
    chooseFile();
    break;
  case 'h': case 'H':
    showHelp();
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
  println(" * Press ' ' to start or stop animation.");
  println(" * Press 'd' to turn drawing on or off.");
  println(" * Press 'm' to turn mouse tracking on or off.");
  println(" * Press 'c' to apply color from image file to display image.");
  println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage .");
  println(" * Press 'o' or 'O' to open an audio or image file.");
  println(" * Press 'h' or 'H' to show help and key commands.");
  println(" * Press 'V' to record a video.");
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
