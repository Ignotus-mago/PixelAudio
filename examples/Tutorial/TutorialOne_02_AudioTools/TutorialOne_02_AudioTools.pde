/*
 * This example application completes the first part of Tutorial One for the PixelAudio 
 * library for Processing. It provides methods for generating a rainbow color array that
 * can reveal the structure of an signal path, a curve that maps audio signals onto an image.
 * It can open and display audio and image files, transcode image pixel data to audio samples
 * and transcode audio samples to image pixel data, and save audio and image files. It can 
 * respond to mouse clicks by playing the audio samples corresponding to the click location
 * in the display image. It is designed to be a good starting place for your own coding. 
 *
 * Still to come, as the tutorial advances:
 * -- animation and saving to video
 * -- drawing to trigger audio events
 * -- UDP communication with Max and other media applications
 * 
 * See also: example sketch LoadImageToAudio, with a complete set of commands for loading
 * images and audio to different color channels. 
 /*
 * This example application completes the first part of Tutorial One for the PixelAudio 
 * library for Processing. It provides methods for generating a rainbow color array that
 * can reveal the structure of an audio path, a curve that maps audio signals onto an image.
 * It can open and display audio and image files, transcode image pixel data to audio samples
 * and transcode audio samples to image pixel data, and save audio and image files. It can 
 * respond to mouse clicks by playing the audio samples corresponding to the click location
 * in the display image. It is designed to be a good starting place for your own coding. 
 *
 * Still to come, as the tutorial advances:
 * -- animation and saving to video
 * -- drawing to trigger audio events
 * -- UDP communication with Max and other media applications
 * 
 * See also: example sketch LoadImageToAudio, with a complete set of commands for loading
 * images and audio to different color channels. 
 * 
 * KEY COMMANDS 
 * 
 * Press 'c' to apply color from image file to display image.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage.
 * Press 'o' or 'O' to open an audio or image file.
 * Press 'h' or 'H' to show help and key commands in console.
 * 
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
AudioOutput audioOut;           // output to sound hardware
boolean isBufferStale = false;  // flags that audioBuffer needs to be reset
float sampleRate = 48000;       // sample rate of audioOut, sample rate of most recently opened audio file
float[] audioSignal;            // the audio signal as an array of floats
MultiChannelBuffer playBuffer;  // a buffer for playing the audio signal
int samplePos;                  // index into the audio signal, set when an audio event is triggered
int audioLength;                // length of the audioSignal, same as the number of pixels in the display image
// SampleInstrument setup
int noteDuration = 1500;        // average sample synth note duration, milliseconds
int samplelen;                  // calculated sample synth note length, samples
Sampler audioSampler;           // minim class for sampled sound
WFSamplerInstrument synth;      // instance of class that wraps a Minim Sampler and implements an ADSR envelope
// ADSR and its parameters
ADSRParams adsrParams;          // wrapper for ADSR that keeps its values visible
float maxAmplitude = 0.7f;      // 0..1
float attackTime = 0.1f;        // seconds
float decayTime = 0.3f;         // seconds
float sustainLevel = 0.25f;     // 0..1
float releaseTime = 0.1f;       // seconds
ArrayList<ADSRParams> adsrList; // list of ADSR values
boolean isRandomADSR = false;   // choose a random envelope from adsrList, or not

// interaction variables for audio
int sampleX;                    // x-coordinate of audio event, set when an audio event is triggered
int sampleY;                    // y-coordinate of audio event, set when an audio event is triggered
ArrayList<TimedLocation> timeLocsArray;
int count = 0;  
int fileIndex = 0;

// ** LOCAL ** Audio Variables for YOUR Processing app may go here ** //

/* ---------------- end audio variables ---------------- */

  
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
  runTimeArray();    // animate audio event markers
}

/**
 * The built-in mousePressed handler for Processing, but note that it forwards mouse coords to audiMousePressed().
 */
public void mousePressed() {
  println("mousePressed:", mouseX, mouseY);
  // handle audio generation in response to a mouse click
  audioMousePressed(mouseX, mouseY);
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
 * This opens up many possibilities and a some risks, too.  
 * 
 * @param key
 * @param keyCode
 */
public void parseKey(char key, int keyCode) {
  switch(key) {
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
  case 'r': case'R': // set isRandomADSR, to use default envelope or a random choice
    isRandomADSR = !isRandomADSR;
    String msg = isRandomADSR ? " synth uses a random ADSR" : " synth uses default ADSR";
    println("---- isRandomADSR = "+ isRandomADSR +","+ msg);
    break;
  case 'h': case 'H': // show help and key commands in console
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press 'c' to apply color from image file to display image.");
  println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage.");
  println(" * Press 'o' or 'O' to open an audio or image file.");
  println(" * Press 'r' or 'R' to tell synth to use default envelope or a random choice.");
  println(" * Press 'h' or 'H' to show help and key commands in console.");
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
