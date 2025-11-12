/*
 * TutorialOne_00_BeginHere showed the basics of loading the PixelAudio library and 
 * using it to display a rainbow array of colors along the signal path. It also provided
 * a basic response to mousePressed events. Tutorial_01_FileIO adds basic input and
 * output of audio and image files to the previous materials.
 * 
 * TutorialOne_01_FileIO can open and display audio and image files, transcode RGB pixel 
 * data to audio samples and transcode audio samples to RGB pixel data. It can also save audio
 * and image files. It responds to mouse clicks by playing the audio samples corresponding 
 * to the click location in the display image. To help you visualize the signal path, the 
 * 'k' command key generates a rainbow color array that follows the signal path but keeps
 * the brightness information in the image intact. The image also appears when you launch
 * the sketch.
 *
 *   1. Launch the sketch and then press the 'o' key to open a image or an audio file.
 *      The "Saucer_mixdown.wav" and "snowfence.jpg" files are good for experimenting.
 *      To see the signal path as a color overlay, press the 'k' key. 
 *      
 *   2. Saucer_mixdown.wav is loaded into an audio buffer, playBuffer, and then
 *      transcoded into the PImage mapImage, which is displayed on screen. The audio 
 *      samples are floating point values from -1.0 to +1.0. They are transcoded to RGB
 *      color values in the range (0, 255). The samples follow a path over the image
 *      that visits every pixel, the signal path. In this sketch, the signal path consists
 *      of 6 connected Hilbert curves. The Hilbert curve is a 2D fractal curve which is
 *      often used in scientific visualization to reveal hidden patterns in 1D data. 
 *      In the image created from Saucer_mixdown, high frequency sounds create fine-grained
 *      patterns and low frequency sounds create coarse-grained patterns. 
 *      
 *   3. Snowfence is loaded into mapImage and then transcoded from to the playBuffer and 
 *      audioSignal variables. If you click in the image, you can hear the sound created 
 *      by reading the brightness levels of pixels along the signal path and changing 
 *      them into audio sample data. The sky, with very little variation in texture, is
 *      relatively quiet. The fence and other areas are noisy. Most images result in
 *      different sorts of noise; however, if you save the image created by loading an 
 *      audio file, it will transcode back into audio. You could send messages this way.
 *      As long as sender and receiver use the same signal path to encode audio, they can
 *      communicate with pictures. Try loading "saucer_image.png" to hear how this works. 
 *      Image data doesn't have as good a resolution as audio data, so there is some 
 *      loss of quality in saving audio as an image and then reloading it as audio. 
 *      
 *      Note: If you press 'k' to load the color overlay, you will not affect audio 
 *      quality. Loading the hue and saturation channels of an image has very little
 *      effect on the brightness levels, which are the source of the audio signal. 
 *      
 *   4. Transcoding is automated in this tutorial, but we'll provide separate loading
 *      of audio and image in later tutorials. 
 *      
 *      Experiment with different image and audio files. Press 'r' turn selection of a  
 *      random ADSR envelope from adsrList on and off. Press 'c' to load only color 
 *      data (hue and saturation) from an image file. 
 *
 * Audio events are generated through PASamplerInstrument. PASamplerInstrument lets
 * us add an ADSR (attack, decay, sustain, release) envelope to audio output from 
 * audio samples. It can also shift pitch and set stereo panning. It can support 
 * multiple voices -- in the initAudio() method we set the number to 8. This is a
 * reasonable number of voices for tracking rapid mouse clicks, but you can change it. 
 *
 * In this sketch, audio events are triggered with one of two methods:
 * 
 *  int actualSampleCount = playSample(int samplePos, int sampleCount, float amplitude)
 *  int actualSampleCount = playSample(int samplePos, int sampleCount, float amplitude, ADSRParams env)
 *
 * The first method uses the built-in ADSR supplied on initializing PASamplerInstrument.
 * The second method allows you to supply your own ADSR. Press the 'r' key to have this
 * sketch trigger sounds with a randomly selected envelope from adsrList. 
 * 
 * PASamplerInstrument and the other audio instruments in net.paulhertz.pixelaudio.voices
 * play an audio event with for the requested duration (samplelen) using the attack, decay,
 * and sustain portion of the envelope. When the duration ends, the release portion of the 
 * envelope controls how the audio fades away. Calls to the instruments playSample() methods
 * return the amount of time the envelope will actually take, which is greater than or equal
 * to the requested duration. 
 *
 * Still to come, as the tutorial advances:
 * -- animation and saving to video
 * -- setting pitch and panning with playSample()
 * -- drawing to trigger audio events
 * -- UDP communication with Max and other media applications
 * -- loading a file to memory and traversing it with a windowed buffer
 * 
 * See also: example sketch LoadImageToAudio, with a complete set of commands for loading
 * images and audio to different color channels. 
 * 
 * KEY COMMANDS 
 * 
 * Press 'c' to apply color from image file to display image.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage.
 * Press 'o' or 'O' to open an audio or image file.
 * Press 'r' or 'R' to use the default envelope or a random envelope from a list.
 * Press 'h' or 'H' to show help and key commands in console.
 * 
 * 
 */
 
// File IO support from Java
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

// Audio support from Java
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

//Mama's ever-lovin' blue-eyed PixelAudio library
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.voices.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;

//audio library
import ddf.minim.*;


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
 * Audio playback support is added with the audio variables and audio methods. 
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
ADSRParams defaultEnv;          // ADSRParams is a wrapper for Minim's ADSR that keeps its values visible
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
  // println("mousePressed:", mouseX, mouseY);
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
 * 
 * Handles key press events passed on by the built-in keyPressed method. 
 * By moving key event handling outside the built-in keyPressed method, 
 * we make it possible to post key commands without an actual key event.
 * Methods and interfaces and even other threads can call parseKey(). 
 * This opens up many possibilities and a some risks, too.  
 * 
 * @param key
 * @param keyCode
 * 
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
  case 'r': case'R': // set isRandomADSR, to use the default envelope or a random envelope from a list.
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
  println(" * Press 'r' or 'R' to use the default envelope or a random envelope from a list.");
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
