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
 * Still to come, as the tutorial advances:
 * -- drawing to trigger audio events
 * -- UDP communication with Max and other media applications
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
 * WFInstrument and TimedLocation classes. In setup(), call initAudio(), then
 * add a mousePressed() method that calls audioMousePressed(mouseX, mouseY)
 * and call runTimeArray() in your draw method. 
 * 
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
float sampleScale = 4;      // 
int sampleBase = (int) (sampleRate/sampleScale);
int samplelen = (int) (sampleScale * sampleBase);
Sampler audioSampler;           // minim class for sampled sound
WFInstrument instrument;        // local class to wrap audioSampler
// ADSR and its parameters
ADSR adsr;            // good old attack, decay, sustain, release
float maxAmplitude = 0.7f;
float attackTime = 0.8f;
float decayTime = 0.5f;
float sustainLevel = 0.125f;
float releaseTime = 0.5f;

// interaction variables for audio
int sampleX;
int sampleY;
ArrayList<TimedLocation> timeLocsArray;
int count = 0;  
int fileIndex = 0;

// ** LOCAL ** Audio Variables for YOUR Processing app may go here ** //

/* ---------------- end audio variables ---------------- */

/* ------------------------------------------------------------------ */
/*                   ANIMATION AND VIDEO VARIABLES                    */
/* ------------------------------------------------------------------ */
  
int shift = 4;                // number of pixels to shift the animation
boolean isAnimating = false;
boolean oldIsAnimating;
boolean isTrackMouse = true;
boolean isRaining = false;
// animation variables
int animSteps = 720;          // how many steps in an animation loop
boolean isRecordingVideo = false;    // are we recording? (only if we are animating)
int videoFrameRate = 24;      // fps
int step;                     // number of current step in animation loop
VideoExport videx;    // hamoid library class for video export (requires ffmpeg)

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
  
public void renderFrame(int step) {
  mapImage.loadPixels();
  int[] rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
  PixelAudioMapper.rotateLeft(rgbSignal, shift);
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, mapSize);
  mapImage.updatePixels();
}
  
public void updateAudio() {
  writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
  // now that the image data has been written to audioSignal, set playBuffer channel 0 to the new audio data
  playBuffer.setChannel(0, audioSignal);
  audioLength = audioSignal.length;
}
  
  
public void doRain() {
  if (random(20) > 1) return;
  int sampleLength = 256 * 256;
  int samplePos = (int) random(sampleLength, mapSize - sampleLength - 1);
  int[] coords = mapper.lookupCoordinate(samplePos);
  sampleX = coords[0];
  sampleY = coords[1];
  // println("----- Rain samplePos = "+ samplePos);
  playSample(playBuffer, samplePos, calcSampleLen(), 0.4f, new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime));
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
      if (mouseY < height/2) 
        shift = -shift;
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
