/*
 * This example application completes the first part of Tutorial One for the PixelAudio 
 * library for Processing. It provides methods for generating a rainbow color array that
 * can reveal the structure of an audio path, a curve that maps audio signals onto an image.
 * It can open and display audio and image files, transcode image pixel data to audio samples
 * and transcode audio samples to image pixel data, and save audio and image files. It can 
 * respond to mouse clicks by playing the audio samples corresponding to the click location
 * in the display image. It is designed to be a good starting place for your own coding. 
 *
 * >>>>>  We add animation and saving to video <<<<< in this very sketch
 * 
 *     import the video export library, com.hamoid.*, a Processing library
 *     add animation and video variables
 *     add animate() method
 *     modify draw() loop
 *     create mouseDragged() and mouseReleased() methods
 *     add key commands, edit showHelp() and test
 *     add updateAudio() method, test
 *     add logic in chooseFile() and fileSelected() to pause animation when opening a file
 *     add video variables and key commands
 *     add stepAnimation() and renderFrame() methods, edit animate() method to call these new methods
 *     add doRain method and key command to trigger rain
 *
 *   1. Launch the sketch and press the 'o' key to open an audio or image file.
 *   2. As in the previous tutorial, the file loads to both audio buffer and display image. 
 *   3. Click on the image to trigger audio events. 
 *   4. Press the space bar to start or stop animation. Animation consists of rotating the image
 *      pixels along the signal path in the renderFrame() methdo. The audio buffer is also rotated, 
 *      in the updateAudio() method. By rotating both the image and the audio buffer, we keep
 *      pixels and audio samples aligned. 
 *   5. Press the 'r' key to turn random audio events ("it's raining") on and off. 
 *   6. If you press 'o' to open a file while animation is running, animation will stop. 
 *      This is a precaution, followed in later example sketches. The sketch, audio events, and
 *      file input all run in separate threads. We want to avoid having two different threads
 *      attempt to write to the audio buffer at the same time.  
 *
 * Still to come, as the tutorial advances:
 * -- drawing to trigger audio events
 * -- UDP communication with Max and other media applications
 * -- loading a file to memory and traversing it with a windowed buffer
 * 
 */

import java.awt.Color;
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
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;

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
 * WFSamplerInstrument and TimedLocation classes. In setup(), call initAudio(), then
 * add a mousePressed() method that calls audioMousePressed(mouseX, mouseY)
 * and call runTimeArray() in your draw method. 
 * 
 */
 
/** Minim audio library */
Minim minim;                    // library that handles audio 
AudioOutput audioOut;           // line out to sound hardware
boolean isBufferStale = false;  // flags that audioBuffer needs to be reset
float sampleRate = 48000;       // a critical value for display and audio, see the setup method
float[] audioSignal;            // the audio signal as an array of floats
MultiChannelBuffer playBuffer;  // a buffer for playing the audio signal
int samplePos;                  // an index into the audio signal, set when audio events are triggered
int audioLength;                // length of the audioSignal, same as the number of pixels in the display image
// SampleInstrument setup
// SampleInstrument setup
int noteDuration = 1500;        // average sample synth note duration, milliseconds
int samplelen;                  // calculated sample synth note length, samples
Sampler audioSampler;           // minim class for sampled sound
WFSamplerInstrument synth;      // instance of a local class to wrap audioSampler
// ADSR and its parameters
ADSRParams adsrParams;          // good old attack, decay, sustain, release
float maxAmplitude = 0.7f;      // 0..1
float attackTime = 0.1f;        // seconds
float decayTime = 0.3f;         // seconds
float sustainLevel = 0.25f;     // 0..1
float releaseTime = 0.1f;       // seconds
ArrayList<ADSRParams> adsrList; // list of ADSR values

// interaction variables for audio
int sampleX;                    // x-coordinate of audio event, set when an audio event is triggered
int sampleY;                    // y-coordinate of audio event, set when an audio event is triggered
ArrayList<TimedLocation> timeLocsArray;
int count = 0;  
int fileIndex = 0;

// ** LOCAL ** Audio Variables for YOUR Processing app may go here ** //

/* ---------------- end audio variables ---------------- */

/* ------------------------------------------------------------------ */
/*                   ANIMATION AND VIDEO VARIABLES                    */
/* ------------------------------------------------------------------ */
  
int shift = 64;                      // number of pixels to shift the animation
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

// ** YOUR VARIABLES ** Variables for YOUR CLASS may go here **  //

  
public void settings() {
  size(3 * genWidth, 2 * genHeight);
}

public void setup() {
  frameRate(24);
  // initialize our library
  pixelaudio = new PixelAudio(this);
  // create a PixelMapGen subclass such as MultiGen, with dimensions equal to our display window
  // the call to hilbertLoop3x2() produces a MultiGen that is 3 * genWidth x 2 * genHeight, where
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

// turn off audio processing when we exit
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
  stepAnimation();      // handle animation step count and video recording
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
  // println("----- Rain samplePos = "+ samplePos);
  adsrParams = adsrList.get((int)random(3));
  playSample(samplePos, calcSampleLen(), 0.4f, adsrParams);
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
  println("mousePressed:", mouseX, mouseY);
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
  case ' ':
    isAnimating = !isAnimating;
    println("-- animation is " + isAnimating);
    break;
  case 'm':
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
  case 'r': case 'R': // turn rain on and off
    isRaining = !isRaining;
    println(isRaining ? "It's raining" : "It's not raining");
    break;
  case 'V':
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
  case 'h': case 'H':
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press ' ' to turn animation on or off.");
  println(" * Press 'm' to turn mouse tracking on or off (controls animation speed).");
  println(" * Press 'c' to apply color from image file to display image.");
  println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage .");
  println(" * Press 'o' or 'O' to open an audio or image file.");
  println(" * Press 'r' or 'R' to turn rain on and off.");
  println(" * Press 'V' to record a video sequence.");
  println(" * Press 'h' or 'H' to show help and key commands in the console.");
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
