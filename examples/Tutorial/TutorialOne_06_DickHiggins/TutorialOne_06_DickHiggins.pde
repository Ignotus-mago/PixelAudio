/*
 * This example application modifies the TutorialOne_04_Drawing code for the PixelAudio
 * library for Processing. We change the sketch so that we have a custom MultiGen, hilbertRowOrtho()
 * created just to handle a 512 x 640 image and an audio file just long enough to fit the size of
 * the image. We modify the code further to make audio and image files load separately, then add 
 * some key commands to write data between image and audio and to save files. Finally, we add
 * some methods to automate the drawing of brushstrokes. Comments on the methods indicate where
 * changes were made.
 * 
 *     1. Add hilbertRowOrtho().
 *     2. Modify loadAudioFile() and loadImageFile(), removing the writeAudioToImage()
 *        and writeImageToAudio() calls.
 *     3. Add key commands in parseKey() for writeAudioToImage() ('W')and writeImageToAudio() ('w').
 *     4. Add key commands in parseKey() to save to audio ('S') and save to image ('s'). 
 *     5. Create methods playPoints(int startTime) and playBrushStrokes(int offset) 
 *        to automate playback of brushstrokes. See DrawingTools tab.
 *     6. Add a key command ('p') to parseKey() to call playBrushStrokes(). 
 *     7. Modify the updateAudio() method: comment out the writeAudioToImage(0 call. 
 *        Rotate the audio signal array when isAnimating == true.
 *        
 *     
 * To play with the code:
 *     
 *     1. Launch the sketch. 
 *        Rainbow stripes appear in display window.
 *     2. Press 'o' and load the file DickHiggins.wav to the audio buffer.
 *        Display does not change, Click on the display image to test the audio. 
 *     3. Press 'W' to write the audio buffer to the display image.
 *        The audio signal is written to mapImage and the display, as gray values
 *     4. Press 'k' to color the image along the signal path. 
 *        Hue and saturation values from the colors array blend with the brightness channel of mapImage. 
 *        The rainbow stripes reappear, coloring the gray image.
 *     5. Press 'd' to start drawing -- draw a few brushstrokes. The press 'd' to turn off drawing.
 *        Pale brushstrokes float above the display image.
 *     6. Press 'o' to open the file DH_30173855_0004.png (in the data folder).
 *        A glitched iamge of Dick Higgins appears. The brushstrokes are still there.
 *     7. Press 'p' to play the brushstrokes.
 *        Audio corresponding to brushstrokes is played.
 *     8. Press the spacebar to run animation.
 *        The image pixels and the audio samples are both rotated. 
 *        The audio played by the brushstrokes change.
 *     9. Adjust the speed and direction of animation. Press 'm' to turn mouse tracking on or off.
 *        When isTrackMouse and isAnimating are true, dragging the mouse will change the animation 
 *        rate and direction.
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
 * Press 'O' or 'o' to open an audio or image file.
 * Press 's' to save image to a PNG file.
 * Press 'S' to save audio to a WAV file.
 * Press 'w' to write the image colors to the audio buffer as transcoded values.
 * Press 'W' to write the audio buffer samples to the image as color values.
 * Press 'z' to reset brushstrokes and audio buffer.
 * Press 'V' to record a video.
 * Press 'H' or 'h' to show help and key commands.
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
int genWidth = 64;         // width of multigen PixelMapGens
int genHeight = 64;        // height of  multigen PixelMapGens
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
float sampleRate = 48000;       // sample rate for audio playback, set when you load an audio file
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
boolean isIgnoreOutsideBounds = true;      // set to true to ignore points outside bounds when drawing
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
boolean isCopyBuffer = true;      // flag for noise reduction during animation 
  
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

float blend = 0.5;
boolean isUseBlend = true;

/* end drawing variables */

// ** YOUR VARIABLES ** Variables for YOUR CLASS may go here **  //

  
public void settings() {
  size(8 * genWidth, 10 * genHeight);
}

public void setup() {
  frameRate(24);
  // initialize our library
  pixelaudio = new PixelAudio(this);
  // create a PixelMapGen subclass such as MultiGen, with dimensions equal to out display window
  // the call to hilbertLoop3x2 produces a MultiGen that is 3 * genWidth x 2 * genHeight, where
  // genWidth == genHeight and genWidth is a power of 2 (a restriction on Hilbert curves)
  // multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
  // hilbertRowOrtho(columns, rows, genWidth, genHeight) creates rows of HilbertGens
  multigen = hilbertRowOrtho(8, 10, genWidth, genHeight);
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
 * In earlier versions, transcodes color channel data from mapImage to audio format 
 * and writes it to the audio signal and buffer. Here, writeImageToAudio() is commented
 * out, but can be called with the key command 'w'. Instead of writing the image to the 
 * audio buffer, we rotate the audio array exactly as we rotated the image array and then
 * update the audio buffer.
 */
public void updateAudio() {
  // writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
  // now that the image data has been written to audioSignal, set playBuffer channel 0 to the new audio data
  if (isAnimating) {
    PixelAudioMapper.rotateLeft(audioSignal, shift);
    playBuffer.setChannel(0, audioSignal);
    audioLength = audioSignal.length;
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
 * The built-in mousePressed handler for Processing, but note that it forwards mouse coords to handleMousePressed().
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
  case 'z': // reset brushstrokes and audio buffer
    reset(true);
    break;
  case 'h': case 'H': // show help and key commands
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
