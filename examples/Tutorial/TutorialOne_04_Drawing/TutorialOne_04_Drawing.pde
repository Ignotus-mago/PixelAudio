/*
 * This example application continues the Tutorial One sequence for the PixelAudio
 * library for Processing. To the previous tools for reading and writing and transcoding
 * audio and image files, triggering audio events, and animating pixels to change
 * audio sample order, it adds the capability of drawing in the display window to 
 * create brush shapes that can be used to trigger audio events. 
 * 
 * The drawing tools and commands are a substantial addition. To implement them 
 * we call on a whole new package of code, net.paulhert.pixelaudio.curves. We make 
 * numerous edits to the previous tutorial, TutorialOneAnimation:
 *     
 *     -- We add a raft of variables to handle drawing.
 *        Here's an outline of how to begin drawing:
 *        
 *     1. Open an audio file with the 'o' key command. The audio file is read into
 *        the audio buffer, playBuffer, It is transcoded as pixel data to the display.
 *        The transcoded data replaces the color pattern that shows the signal path.
 *        Similarly, if you open an image file, it will open in the display and be 
 *        transcoded into the audio buffer, possibly with very noisy results. 
 *        You can turn this simultaneous loading off with the 'L' key and use the
 *        'w' and 'W' keys to write the display to audio or audio to the display.
 *        
 *     2. Press the 'k' key. The color pattern reappears, overlaid on the signal image. 
 *        The overlay works by copying the Hue and Saturation channels of the color image
 *        and combining them with the Brightness channel of the display. See the 
 *        applyColor() and chooseColorImage() methods for the code. You can also load
 *        color data from an image with the 'c' key command.
 *        
 *     3. Click on the display to trigger audio events. They should be fairly quiet: the 
 *        gain for audio output is set to -18.0 dB. The sound will get loud when you draw.
 *        
 *     4. Press 'd' to set isDrawMode to true. Press and drag the mouse to draw a line.
 *        Release the mouse to create a brushstroke -- audio and animation will play.
 *        When you drag the mouse, the sketch accumulates non-repeating points into an 
 *        ArrayList of PVector, allPoints. When you release the mouse, PACurverMaker and 
 *        PACurveMakerUtility process the points:         
 *          - The accumulated points in allPoints are used to create a PACurveMaker object.
 *          - PACurveMaker uses the Ramer-Douglas-Peucker algorithm to reduce the number of points. 
 *          - PACurveMaker uses the reduced point set to generate a Bezier curve.
 *          - The Bezier curve is used to model a brushstroke shape.
 *        All these data elements are available from the PACurveMaker instance. PACurveMaker
 *        also stores time data for drawing, but we don't use it in this sketch. 
 *        The PACurveMaker instance is added to a list of PACUrveMakers, brushShapesList.
 *        BrushShapeList handles drawing curves to the screen. 
 *        
 *     5. Draw some more brushstrokes. Press 'd' to turn drawing off.
 *     
 *     6. Click on a brushstroke to activate it. 
 *     
 *     7. Press the 'x' key while hovering over a brushstroke to delete it. 
 *        Pressing 'x' when you aren't hovering over a brushstroke will delete the oldest 
 *        brushstroke. Pressing 'X' will delete the most recent brushstroke. 
 *     
 *     8. Press'p' to play the brushstrokes one after another, in the order you drew them.
 *          
 *          
 *     -- We added several new methods, in the DRAWING METHODS section.
 *        See the JavaDocs information for each method for a description of what it does.
 *        The initDrawing(), handleDrawing() and handleMousePressed() are new methods that 
 *        are central to interaction and drawing. 
 *        
 *        SEE the JavaDoc entries for PACurveMaker, PACurveMakerUtility, PABezShape and the
 *        other classes in the net.paulhertz.pixelaudio.curves package for more information
 *        about curve modeling. 
 *        
 *     -- We reconfigured code for handling mouse events and TimedLocation events. The mouse event
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
 *     -- Mouse clicks that don't involve drawing are passed to audioMousePressed(), used in previous tutorials. 
 *     
 * AUDIO EVENTS
 *     
 * In this tutorial we introduce a new class for triggering audio events, WFSamplerInstrumentPool.
 * In previous tutorials, we just used WFSamplerInstrument, which can support multiple voices and 
 * works well with events triggered by clicking the mouse. Events triggered by drawing add a new
 * level of complication -- we can automatically trigger dozens of event at once. WFSamplerInstrument
 * and Minim's AudioSampler, which it wraps, appeared to have practical limits to the number of 
 * voices they could support without distortion. WFSamplerInstrumentPool allocates a WFSamplerInstrument
 * from a previously created list or "pool" of instruments to handle each request for an audio event. 
 * So far, this is the best solution I have for playing many voices together.
 * 
 * If you would like to hear the difference between multi-voice WFSamplerIntrument and WFSamplerInstrumentPool,
 * just press the '!' key while one or more brushstrokes are playing.
 * 
 * Remaining in the tutorial are TutorialOne_05_UDP, which implements networked communication with Max 
 * and other media applications, and MusicWindowBox, the final tutorial sketch, which implements a windowed 
 * buffer for traversing an audio file plus various features useful in performance.
 *        
 * 
 * Here are the key commands for this sketch:
 * 
 * Press ' ' to  start or stop animation.
 * Press 'd' to turn drawing on or off.
 * Press 'm' to turn mouse tracking on or off.
 * Press 'c' to apply color from image file to display image.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage .
 * Press 'l' or 'L' to set files to load as both image and audio or load separately.
 * Press 'o' or 'O' to open an audio or image file.
 * Press 'w' to write the image colors to the audio buffer as transcoded values.
 * Press 'W' to write the audio buffer samples to the image as color values.
 * Press 'x' to delete the current active brush shape or the oldest brush shape.
 * Press 'X' to delete the most recent brush shape.
 * Press 'V' to record a video.
 * Press 'h' or 'H' to show help message in the console.
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
 * 
 * Audio playback support is added with the audio variables and audio methods 
 * (below, in Eclipse, in a tab, in Processing). You will also need the 
 * WFInstrument and TimedLocation classes. In setup(), call initAudio(), then
 * add a mousePressed() method that calls audioMousePressed(mouseX, mouseY)
 * and call runTimeArray() in your draw method. 
 * 
 */

/** Minim audio library */
Minim minim;                    // library that handles audio 
AudioOutput audioOut;           // line out to sound hardware
boolean isBufferStale = false;  // flags that audioBuffer needs to be reset
float sampleRate = 48000;       // sample rate for audio output and audio files
float[] audioSignal;            // the audio signal as an array of floats
MultiChannelBuffer playBuffer;  // a buffer for playing the audio signal
int samplePos;                  // an index into the audio signal, selected by a mouse click on the display image
int audioLength;                // length of the audioSignal, same as the number of pixels in the display image

// SampleInstrument setup
int noteDuration = 1000;        // average sample synth note duration, milliseconds
int samplelen;                  // calculated sample synth note length, samples
Sampler audioSampler;           // minim class for sampled sound
WFSamplerInstrument synth;      // local class to wrap audioSampler

// ADSR and its parameters
ADSRParams adsr;                // good old attack, decay, sustain, release
float maxAmplitude = 0.7f;      // 0..1
float attackTime = 0.4f;        // seconds
float decayTime = 0.0f;         // seconds, no decay
float sustainLevel = 0.7f;      // 0..1, same as maxAmplitude
float releaseTime = 0.4f;       // seconds, same as attack

// interaction variables for audio
int sampleX;                    // keep track of coordinates associated with audio samples
int sampleY;
ArrayList<TimedLocation> timeLocsArray;    // a list of timed events 
boolean isLoadToBoth = true;    // if true, load newly opened file both to audio and to video


/* ------------------------------------------------------------------ */
/*                   ANIMATION AND VIDEO VARIABLES                    */
/* ------------------------------------------------------------------ */

int shift = 64;                         // number of pixels to shift the animation
int totalShift = 0;                     // cumulative shift
boolean isAnimating = false;            // do we run animation or not?
boolean oldIsAnimating;                 // keep track of animation state when opening a file
boolean isTrackMouse = false;           // if true, drag the mouse to change shift value
// animation variables
int animSteps = 720;                    // how many steps in an animation loop
boolean isRecordingVideo = false;       // are we recording? (only if we are animating)
int videoFrameRate = 24;                // fps, frames per second
int step;                               // number of current step in animation loop
VideoExport videx;                      // hamoid library class for video export (requires ffmpeg)


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
public boolean isDrawMode = false;                  // is drawing on or not?
public float epsilon = 8.0f;                        // controls how much reduction is applied to points
public ArrayList<PVector> allPoints;                // all the points the user drew, thinnned
public int dragColor = color(233, 199, 89, 128);    // color for initial drawing 
public float dragWeight = 8.0f;                     // weight (brush diameter) of initial line drawing
public int startTime;                               // start time for user drawing event
public ArrayList<Integer> allTimes;                 // list for tracking user drawing times, for future use
public PVector currentPoint;                        // most recent point in user drawing
public int polySteps = 5;                           // number of steps in polygon representation of a Bezier curve
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
boolean isIgnoreOutsideBounds = true;               // when drawing, clip or ignore points outside display bounds

// ** YOUR VARIABLES ** Variables for YOUR CLASS could go here **  //
  
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
 * turn off audio processing when we exit
 */
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
  if (isTrackMouse && mousePressed) {
    writeToScreen("shift = "+ shift, 16, 24, 24, false);
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
 * rotates them shift elements left, writes them back to mapImage.
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
 * Updates audioSignal by rotating it the same amount as mapImage.pixels.
 */
public void updateAudio() {
  PixelAudioMapper.rotateLeft(audioSignal, shift);
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
 * mouse coords to handleMousePressed().
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
    println("----- animation shift = "+ shift);
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
  case 'L': case 'l':
    isLoadToBoth = !isLoadToBoth;
    String msg = isLoadToBoth ? "loads to both audio and image. " : "loads only to selected format. ";
    println("---- isLoadToBoth is "+ isLoadToBoth +", opening a file "+ msg);
    break;
  case 'p': // play brushstrokes with a time lapse between each
    playBrushstrokes(2000);
    break;
  case 'o': case 'O': // open an audio or image file
    chooseFile();
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
  println(" * Press ' ' to  start or stop animation.");
  println(" * Press 'd' to turn drawing on or off.");
  println(" * Press 'm' to turn mouse tracking on or off.");
  println(" * Press 'c' to apply color from image file to display image.");
  println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage .");
  println(" * Press 'l' or 'L' to set files to load as both image and audio or load separately.");
  println(" * Press 'p' to play brushstrokes with a time lapse between each.");
  println(" * Press 'o' or 'O' to open an audio or image file.");
  println(" * Press 'w' to write the image colors to the audio buffer as transcoded values.");
  println(" * Press 'W' to write the audio buffer samples to the image as color values.");
  println(" * Press 'x' to delete the current active brush shape or the oldest brush shape.");
  println(" * Press 'X' to delete the most recent brush shape.");
  println(" * Press 'V' to record a video.");
  println(" * Press 'h' or 'H' to show help message in the console.");
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
