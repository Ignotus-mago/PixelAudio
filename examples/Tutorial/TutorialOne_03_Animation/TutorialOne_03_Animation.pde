/*
 * This example application builds on TutorialOne_01_FileIO, which provided commands to
 * open and display audio and image files, transcode image pixel data to audio samples
 * and transcode audio samples to image pixel data, and save audio and image files. It can
 * respond to mouse clicks by playing the audio samples corresponding to the click location
 * in the display image.
 *
 * We add animation and saving to video in this sketch, which also provides a complete version
 * of the playSample(...) command that can control the pitch and panning of an audio event.
 * Animation is simple: we shift the pixels along the signal path. We keep track of how far
 * the pixels have been shifted and use that information to determine the correct location
 * in the audio buffer to trigger an audio event with playSample(). If you are looking at
 * an image that represents audio data, such as Saucer_mixdown.wav, you can see how animation
 * changes the apparent position of the audio data. When animation is running, repeated clicks
 * at the same location in the window will trigger different audio events.
 *
 * The playSample() methods in this sketch introduce the most complete audio triggering
 * method available in PASamplerInstrument, one which can set sample start position, length,
 * amplitude, ADSR-style envelope, pitch scaling and stereo pan location.
 *
 *   samplelen = synth.playSample(samplePos, (int) samplelen, amplitude, env, pitchScaling, pan);
 *
 * Pitch scaling by default is 1.0, which is to say that samples will be played back at
 * the recorded frequency. The recorded frequency is not necessarily the same as the audio
 * output frequency, which is
 *
 *
 * Here are the primary changes from the FileIO tutorial:
 *
 *     import the video export library, com.hamoid.*, a Processing library
 *     add animation and video variables
 *     add animate() method
 *     modify draw() loop
 *     create mouseDragged() and mouseReleased() methods to handle
 *       interactive setting of animation step size
 *     new key commands, descriptions added to showHelp() method
 *     updateAudio() method tracks number of pixels shifted by animation
 *     add logic in chooseFile() and fileSelected() to pause animation when opening a file
 *     add video variables and key commands
 *     add stepAnimation() and renderFrame() methods, edit animate() method to call these new methods
 *     add doRain method and key command to trigger rain
 *
 * Still to come, as the tutorial advances:
 * -- drawing to trigger audio events
 * -- UDP communication with Max and other media applications
 * -- Windowed buffer use to load an audio file into memory and advance through it
 *
 * Press ' ' to turn animation on or off.
 * Press 'a' to rotate pixels left by shift value.
 * Press 'A' to rotate pixels right by shift value.
 * Press 'm' to turn interactive setting of shift value on or off (drag to set).
 * Press 'c' to apply color from image file to display image.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage .
 * Press 'o' or 'O' to open an audio or image file.
 * Press 'p' to select low pitch scaling or default pitch scaling.
 * Press 'P' to select high pitch scaling or default pitch scaling.
 * Press 'd' or 'D' to turn rain on and off.
 * Press 'r' or 'R' to set isRandomADSR, to use default envelope or a random choice.
 * Press 'V' to record a video from frame 0 to frame animSteps.
 * Press 'h' or 'H' to show help message.
 *
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

//Mama's ever-lovin' blue-eyed PixelAudio library
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.sampler.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;

//audio library
import ddf.minim.*;

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
 * PASamplerInstrument and TimedLocation classes. In setup(), call initAudio(), then
 * add a mousePressed() method that calls audioMousePressed(mouseX, mouseY)
 * and call runTimeArray() in your draw method.
 *
 */

/** Minim audio library */
Minim minim;                    // library that handles audio
AudioOutput audioOut;           // output to sound hardware
boolean isBufferStale = false;  // flags that audioBuffer needs to be reset
float sampleRate = 44100;       // sample rate of audioOut
float fileSampleRate;           // sample rate of most recently opened file
float[] audioSignal;            // the audio signal as an array of floats
MultiChannelBuffer playBuffer;  // a buffer for playing the audio signal
int samplePos;                  // index into the audio signal, set when an audio event is triggered
int audioLength;                // length of the audioSignal, same as the number of pixels in the display image
// SampleInstrument setup
int noteDuration = 1500;        // average sample synth note duration, milliseconds
int samplelen;                  // calculated sample synth note length, samples
PASamplerInstrument synth;      // instance of class that wraps a Minim Sampler and implements an ADSR envelope
// ADSR and its parameters
ADSRParams defaultEnv;          // wrapper for ADSR that keeps its values visible
float maxAmplitude = 0.7f;      // 0..1
float attackTime = 0.1f;        // seconds
float decayTime = 0.3f;         // seconds
float sustainLevel = 0.25f;     // 0..1
float releaseTime = 0.1f;       // seconds
ArrayList<ADSRParams> adsrList; // list of ADSR values
boolean isRandomADSR = false;   // choose a random envelope from adsrList, or not
float pitchScaling = 1.0f;      // factor for changing pitch
float defaultPitchScaling = 1.0f;
float lowPitchScaling = 0.5f;
float highPitchScaling = 2.0f;

// interaction variables for audio
int sampleX;                    // x-coordinate of audio event, set when an audio event is triggered
int sampleY;                    // y-coordinate of audio event, set when an audio event is triggered
ArrayList<TimedLocation> timeLocsArray;
int count = 0;
int fileIndex = 0;


/* ------------------------------------------------------------------ */
/*                   ANIMATION AND VIDEO VARIABLES                    */
/* ------------------------------------------------------------------ */

int shift = 256;                     // number of pixels to shift the animation
int totalShift = 0;                  // cumulative shift
boolean isAnimating = false;         // do we run animation or not?
boolean oldIsAnimating;              // keep track of animation state when opening a file
boolean isTrackMouse = false;        // if true, drag the mouse to change shift value
boolean isRaining = false;           // set to true ('r' key) to automate audio events
// animation variables
int animSteps = 720;                 // how many steps in an animation loop
boolean isRecordingVideo = false;    // are we recording? (only if we are animating)
int videoFrameRate = 24;             // fps
int step;                            // number of current step in animation loop, used when recording video
VideoExport videx;                   // hamoid library class for video export (requires ffmpeg)


public void settings() {
  size(3 * genWidth, 2 * genHeight);
}

public void setup() {
  frameRate(24);
  // initialize our library
  pixelaudio = new PixelAudio(this);
  // create a PixelMapGen subclass such as MultiGen, with dimensions equal to the display window
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
 * Initialize mapImage with the colors array. MapImage will handle the color data for mapper
 * and also serve as our display image.
 */
public void initImages() {
  mapImage = createImage(width, height, ARGB);
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
  mapImage.updatePixels();
}

public void draw() {
  image(mapImage, 0, 0);
  if (isRaining)
    doRain();
  runTimeArray();    // animate audio event markers
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
    step = 0;
    if (isRecordingVideo) {
      isRecordingVideo = false;
      videx.endMovie();
      println("--- Completed video at frame " + animSteps);
      isAnimating = oldIsAnimating;
    }
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
 * drop some random audio events, like unto the gentle rain
 */
public void doRain() {
  if (random(20) > 1) return;
  int sampleLength = 256 * 256;
  int samplePos = (int) random(sampleLength, mapSize - sampleLength - 1);
  int[] coords = mapper.lookupCoordinate(samplePos);
  setSampleVars(coords[0], coords[1]);    // set sampleX, sampleY and samplePos
  float panning = map(sampleX, 0, width, -0.8f, 0.8f);
  // println("----- Rain samplePos = "+ samplePos);
  ADSRParams env = adsrList.get((int)random(3));
  playSample(samplePos, calcSampleLen(), 0.4f, env, panning);
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
 * The built-in mousePressed handler for Processing, but note that it forwards mouse coords to audiMousePressed().
 */
public void mousePressed() {
  // println("mousePressed:", mouseX, mouseY);
  // handle audio generation in response to a mouse click
  audioMousePressed(mouseX, mouseY);
}

public void mouseDragged() {
  if (isTrackMouse) {
    shift = abs(width/2 - mouseX);
    if (mouseY < height/2) {
      shift = -shift;
    }
    writeToScreen("shift = "+ shift, 16, 24, 24, false);
  }
}

public void mouseReleased() {
  if (isAnimating && isTrackMouse) {
    println("----- animation shift = "+ shift);
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
  case ' ': // turn animation on or off
    isAnimating = !isAnimating;
    println("-- animation is " + isAnimating);
    break;
  case 'a': // rotate pixels left by shift value
    renderFrame(step);
    break;
  case 'A': // rotate pixels right by shift value
    shift = -shift;
    renderFrame(step);
    shift = -shift;
    break;
  case 'm': // turn interactive setting of shift value on or off (drag to set)
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
  case 'o':
  case 'O': // open an audio or image file
    chooseFile();
    break;
  case 'p': // select low pitch scaling or default pitch scaling
    if (pitchScaling != lowPitchScaling)
      pitchScaling = lowPitchScaling;
    else
      pitchScaling = defaultPitchScaling;
    break;
  case 'P': // select high pitch scaling or default pitch scaling
    if (pitchScaling != highPitchScaling)
      pitchScaling = highPitchScaling;
    else
      pitchScaling = defaultPitchScaling;
    break;
  case 'd':
  case 'D': // turn rain on and off
    isRaining = !isRaining;
    println(isRaining ? "It's raining" : "It's not raining");
    break;
  case 'r':
  case'R': // set isRandomADSR, to use default envelope or a random choice
    isRandomADSR = !isRandomADSR;
    String msg = isRandomADSR ? " synth uses a random ADSR" : " synth uses default ADSR";
    println("---- isRandomADSR = "+ isRandomADSR +","+ msg);
    break;
  case 'V': // record a video from frame 0 to frame animSteps
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
  case 'h':
  case 'H': // show help message
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press ' ' to turn animation on or off.");
  println(" * Press 'a' to rotate pixels left by shift value.");
  println(" * Press 'A' to rotate pixels right by shift value.");
  println(" * Press 'm' to turn interactive setting of shift value on or off (drag to set).");
  println(" * Press 'c' to apply color from image file to display image.");
  println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage .");
  println(" * Press 'o' or 'O' to open an audio or image file.");
  println(" * Press 'p' to select low pitch scaling or default pitch scaling.");
  println(" * Press 'P' to select high pitch scaling or default pitch scaling.");
  println(" * Press 'd' or 'D' to turn rain on and off.");
  println(" * Press 'r' or 'R' to set isRandomADSR, to use default envelope or a random choice.");
  println(" * Press 'V' to record a video from frame 0 to frame animSteps.");
  println(" * Press 'h' or 'H' to show help message.");
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
