/**
 * LoadImageToAudio shows how to load an image and turn it into an audio file
 * that can be played by clicking on the image. You can also load an audio file
 * and turn it into an image.
 *
 * You can write the current image to the audio signal with the 'w' key command.
 * The sound of an image will probably be noisy since it is not designed with cyclic
 * functions along the arbitrary signal path we impose on it with a PixelMapGen.
 * RGB values range in integer steps from 0 to 255. When we transcode them to
 * audio values in the range (-1.0, 1.0), we have less resolution than the full
 * range of floating point values. There's always some noise in values transcoded
 * from images. In most of the examples for the PixelAudio library when you load
 * an audio file and it gets transcoded into an image, we still use the audio
 * signal with all its resolution to play sounds. When you click in the image,
 * you will be playing a sample from the signal.
 *
 * You can write the audio signal to the image with the 'W' key command. This will
 * convert the audioSignal into HSB Brightness values and write them to mapImage.
 * If you open this image in all color channels or in the HSB Brightness channel
 * and then write it to the audio channel, you will get a reasonably good recreation
 * of the audio, at 8-bit resolution.
 *
 * An audio signal or image can be loaded to various channels of the image: Red,
 * Green, Blue or all channels in the RGB color space or Hue, Brightness, or
 * Saturation in the HSB color space. HSB Hue operations on grayscale images may
 * result in no change to the image. Grayscale images have no saturation or hue,
 * only brightness, when they are represented in the HSB color space. Loading the
 * saturation of a grayscale image or a transcoded audio file to a color image
 * will turn it gray. To work more effectively with HSB, we can load both hue and
 * saturation from a color image to another color image or to a grayscale image,
 * maintaining the brightness channel of the target image, with the 'c' command key.
 *
 * You can enhance image contrast by stretching its histogram ('m' key).
 * You can make the image brighter ('=' and '+' keys) or darker ('-' or '_' key)
 * using a gamma function, a non-linear adjustment.
 *
 * Press ' ' to toggle animation.
 * Press 'o' to open an audio or image file in all RGB channels.
 * Press 'r' to open an audio or image file in the RED channel of the image.
 * Press 'g' to open an audio or image file in the GREEN channel of the image.
 * Press 'b' to open an audio or image file in the BLUE channel of the image.
 * Press 'h' to open an audio or image file in the HSB Hue channel of the image.
 * Press 'v' to open an audio or image file in the HSB Saturation channel of the image.
 * Press 'l' to open an audio or image file in the HSB Brightness channel of the image.
 * Press 'c' to apply color from an image file to the display image.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage .
 * Press 'O' to reload the most recent image or audio file or show an Open File dialog.
 * Press 'm' to remap the histogram of the image.
 * Press '=' to use a gamma function to make the image lighter.
 * Press '-' to use a gamma function to make the image darker.
 * Press 'S' to save the audio signal to an audio file.
 * Press 's' to save the image to a PNG file.
 * Press 'f' to print the current frame rate to the console.
 * Press 'w' to transcode the image and write it to the audio signal.
 * Press 'W' to transcode the audio signal and write it to the image.
 * Press '?' to show the Help Message in the console.
 *
 * PLEASE NOTE: Hue (H) and Saturation (V) operations may have no effect on gray pixels.
 * ALSO: Image brightness determines image audio. Images with uniform brightness will be silent.
 *
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import javax.sound.sampled.*;

import ddf.minim.*;

import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.voices.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;


// PixelAudio vars and objects
PixelAudio pixelaudio;     // our shiny new library
MultiGen multigen;         // a PixelMapGen that handles multiple gens
ArrayList<PixelMapGen> genList;    // list of PixelMapGens that create an image using mapper
ArrayList<int[]> offsetList;       // list of x,y coordinates for placing gens from genList
int genWidth = 512;       // width of HilbertGen used in multiGen, must be a power of 2
int genHeight = 512;      // height of HilbertGen used in multigen, for hGen must be equal to width
PixelAudioMapper mapper;  // object for reading, writing, and transcoding audio and image data
int mapSize;              // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
PImage mapImage;          // image for display
PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;
int[] colors;             // array of spectral colors

// Java random number generator
Random rando;

/** Minim audio library */
Minim minim;              // library that handles audio
AudioOutput audioOut;     // line out to sound hardware
MultiChannelBuffer playBuffer;     // data structure to hold audio samples
boolean isBufferStale = false;     // do we need to reset the audio buffer?
float sampleRate = 44100;   // audioOut sample rate
float[] audioSignal;      // the audio signal as an array
int[] rgbSignal;          // the colors in the display image, in the order the signal path visits them
int audioLength;
int audioFileLength;

// SampleInstrument setup
int sampleLen;
int durationMS = 2000;
PASamplerInstrument synth;      // instrument to generate audio events
// ADSR and params
ADSRParams adsr;                // good old attack, decay, sustain, release
float maxAmplitude = 0.9f;
float attackTime = 0.2f;
float decayTime = 0.125f;
float sustainLevel = 0.5f;
float releaseTime = 0.2f;

// audio file
File audioFile;
String audioFilePath;
String audioFileName;
String audioFileTag;

// image file
File imageFile;
String imageFilePath;
String imageFileName;
String imageFileTag;
int imageFileWidth;
int imageFileHeight;

boolean isLoadFromImage = false;

// animation
boolean isAnimating = false;       // animation status
boolean oldIsAnimating;            // keep old animation status if we suspend animation
int shift = 1024;                  // number of pixels to shift the animation
int totalShift = 0;                // cumulative shift
boolean isLooping = true;          // looping sample (our instrument ignores this)
// interaction
int sampleX;      // x-coordinate of a sample point on the image
int sampleY;      // y-coordinate of a sample point on the image
int samplePos;    // position of a mouse click along the signal path, index into the audio array
ArrayList<TimedLocation> timeLocsArray;
int count = 0;
// histogram and gamma adjustments
int histoHigh = 240;
int histoLow = 32;
float gammaLighter = 0.9f;
float gammaDarker = 1.2f;
int[] gammaTable;


public void settings() {
  size(3 * genWidth, 2 * genHeight);
}

public void setup() {
  frameRate(30);
  pixelaudio = new PixelAudio(this);
  sampleRate = 44100;
  rando = new Random();
  genList = new ArrayList<PixelMapGen>();
  multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
  mapper = new PixelAudioMapper(multigen);
  mapSize = mapper.getSize();
  initAudio();
  initImage();
  timeLocsArray = new ArrayList<TimedLocation>();
  showHelp();
}

/**
 * Initialize audio variables
 */
public void initAudio() {
  this.minim = new Minim(this);
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  // create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
  // playBuffer will not contain audio data until we load a file
  this.playBuffer = new MultiChannelBuffer(mapSize, 1);
  this.audioSignal = playBuffer.getChannel(0);
  this.audioLength = audioSignal.length;
  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
  adsr = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  // create a PASamplerInstrument with 8 voices, adsrParams will be its default envelope
  synth = new PASamplerInstrument(playBuffer, audioOut.sampleRate(), 8, audioOut, adsr);
}

/**
 * Initialize mapImage and associated variables
 */
public void initImage() {
  colors = getColors(); // create an array of rainbow colors
  mapImage = createImage(width, height, ARGB); // an image to use with mapper
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
  mapImage.updatePixels();
}

/**
 * @return an array of RGB colors that cover a full rainbow spectrum
 */
public int[] getColors() {
  int[] colorWheel = new int[mapSize]; // an array for our colors
  pushStyle(); // save styles
  colorMode(HSB, colorWheel.length, 100, 100); // pop over to the HSB color space and give hue a very wide range
  for (int i = 0; i < colorWheel.length; i++) {
    colorWheel[i] = color(i, 40, 75); // fill our array with colors, gradually changing hue
  }
  popStyle(); // restore styles, including the default RGB color space
  return colorWheel;
}

public void draw() {
  image(mapImage, 0, 0);
  if (isAnimating)
    stepAnimation();
  runTimeArray();
}

public void stepAnimation() {
  mapImage.loadPixels();
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
  PixelAudioMapper.rotateLeft(rgbSignal, shift);
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, mapSize);
  mapImage.updatePixels();
  totalShift += shift;
}

public void runTimeArray() {
  int currentTime = millis();
  timeLocsArray.forEach(tl -> {
    tl.setStale(tl.eventTime() < currentTime);
    if (!tl.isStale()) {
      drawCircle(tl.getX(), tl.getY());
    }
  }
  );
  timeLocsArray.removeIf(TimedLocation::isStale);
}

public void drawCircle(int x, int y) {
  fill(color(233, 220, 199, 128));
  noStroke();
  circle(x, y, 60);
}

public void keyPressed() {
  switch (key) {
  case ' ': // toggle animation
    isAnimating = !isAnimating;
    break;
  case 'o': // open an audio or image file in all RGB channels
    chan = PixelAudioMapper.ChannelNames.ALL;
    chooseFile();
    break;
  case 'r': // open an audio or image file in the RED channel of the image
    chan = PixelAudioMapper.ChannelNames.R;
    chooseFile();
    break;
  case 'g': // open an audio or image file in the GREEN channel of the image
    chan = PixelAudioMapper.ChannelNames.G;
    chooseFile();
    break;
  case 'b': // open an audio or image file in the BLUE channel of the image
    chan = PixelAudioMapper.ChannelNames.B;
    chooseFile();
    break;
  case 'h': // open an audio or image file in the HSB Hue channel of the image
    chan = PixelAudioMapper.ChannelNames.H;
    chooseFile();
    break;
  case 'v': // open an audio or image file in the HSB Saturation channel of the image
    chan = PixelAudioMapper.ChannelNames.S;
    chooseFile();
    break;
  case 'l': // open an audio or image file in the HSB Brightness channel of the image
    chan = PixelAudioMapper.ChannelNames.L;
    chooseFile();
    break;
  case 'c': // apply color from an image file to the display image
    chooseColorImage();
    break;
  case 'k': // apply the hue and saturation in the colors array to mapImage
    mapImage.loadPixels();
    applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
    mapImage.updatePixels();
    break;
  case 'O': // reload the most recent image or audio file or show an Open File dialog
    if (isLoadFromImage) {
      if (imageFile == null) {
        chooseFile();
      } 
      else {
        // reload image
        loadImageFile(imageFile);
        println("-------->>>>> Reloaded image file");
      }
    } 
    else {
      if (audioFile == null) {
        chooseFile();
      } else {
        // reload audio
        loadAudioFile(audioFile);
        println("-------->>>>> Reloaded audio file");
      }
    }
    break;
  case 'm': // remap the histogram of the image
    mapImage.loadPixels();
    int[] bounds = getHistoBounds(mapImage.pixels);
    mapImage.pixels = stretch(mapImage.pixels, bounds[0], bounds[1]);
    mapImage.updatePixels();
    break;
  case '=': // use a gamma function to make the image lighter
  case '+':
    setGamma(gammaLighter);
    mapImage.loadPixels();
    mapImage.pixels = adjustGamma(mapImage.pixels);
    mapImage.updatePixels();
    break;
  case '-': // use a gamma function to make the image darker
  case '_':
    setGamma(gammaDarker);
    mapImage.loadPixels();
    mapImage.pixels = adjustGamma(mapImage.pixels);
    mapImage.updatePixels();
    break;
  case 'S': // save the audio signal to an audio file
    saveToAudio();
    break;
  case 's': // save the image to a PNG file
    saveToImage();
    break;
  case 'f': // print the current frame rate to the console
    println("--->> frame rate: " + frameRate);
    break;
  case 'w': // transcode the image and write it to the audio signal
    writeImageToAudio();
    synth.setBuffer(audioSignal, sampleRate);
    println("--->> Wrote image to audio as audio data.");
    break;
  case 'W': // transcode the audio signal and write it to the image
    writeAudioToImage();
    println("--->> Wrote audio to image as audio data.");
    break;
  case '?': // show the Help Message in the console
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press ' ' to toggle animation.");
  println(" * Press 'o' to open an audio or image file in all RGB channels.");
  println(" * Press 'r' to open an audio or image file in the RED channel of the image.");
  println(" * Press 'g' to open an audio or image file in the GREEN channel of the image.");
  println(" * Press 'b' to open an audio or image file in the BLUE channel of the image.");
  println(" * Press 'h' to open an audio or image file in the HSB Hue channel of the image.");
  println(" * Press 'v' to open an audio or image file in the HSB Saturation channel of the image.");
  println(" * Press 'l' to open an audio or image file in the HSB Brightness channel of the image.");
  println(" * Press 'c' to apply color from an image file to the display image.");
  println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage .");
  println(" * Press 'O' to reload the most recent image or audio file or show an Open File dialog.");
  println(" * Press 'm' to remap the histogram of the image.");
  println(" * Press '=' to use a gamma function to make the image lighter.");
  println(" * Press '-' to use a gamma function to make the image darker.");
  println(" * Press 'S' to save the audio signal to an audio file.");
  println(" * Press 's' to save the image to a PNG file.");
  println(" * Press 'f' to print the current frame rate to the console.");
  println(" * Press 'w' to transcode the image and write it to the audio signal.");
  println(" * Press 'W' to transcode the audio signal and write it to the image.");
  println(" * Press '?' to show the Help Message in the console.");
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

public void mousePressed() {
  sampleX = mouseX;
  sampleY = mouseY;
  samplePos = getSamplePos(sampleX, sampleY);
  int varyDuration = calcSampleLen(durationMS);
  int sampleLength = playSample(samplePos, varyDuration, 0.9f);
}

/**
 * Calculates the index of the image pixel within the signal path,
 * taking the shifting of pixels and audioSignal into account.
 * See the MusicWindowBox sketch for use of a windowed buffer in this calculation.
 *
 * @param x    an x coordinate within mapImage and display bounds
 * @param y    a y coordinate within mapImage and display bounds
 * @return     the index of the sample corresponding to (x,y) on the signal path
 */
public int getSamplePos(int x, int y) {
  int pos = mapper.lookupSample(x, y);
  // calculate how much animation has shifted the indices into the buffer
  totalShift = (totalShift + shift % mapSize + mapSize) % mapSize;
  return (pos + totalShift) % mapSize;
}


/**
 * @return a length in samples with some Gaussian variation
 */
public int calcSampleLen(int durationMS) {
  float vary = 0;
  // skip the fairly rare negative numbers
  while (vary <= 0) {
    vary = (float) gauss(1.0, 0.0625);
  }
  int len = (int)(abs((vary * durationMS) * sampleRate / 1000.0f));
  int actualLength = (int)((len / sampleRate) * 1000);
  println("---- calcSampleLen result = "+ len +" samples at "+ sampleRate +" Hz sample rate, "+ actualLength +" milliseconds");
  return len;
}

/**
 * Returns a Gaussian variable using a Java library call to
 * <code>Random.nextGaussian</code>.
 *
 * @param mean
 * @param variance
 * @return a Gaussian-distributed random number with mean <code>mean</code> and
 *         variance <code>variance</code>
 */
public double gauss(double mean, double variance) {
  return rando.nextGaussian() * Math.sqrt(variance) + mean;
}

/**
 * Plays an audio sample with PASamplerInstrument and default ADSR.
 *
 * @param samplePos    position of the sample in the audio buffer
 * @param sampleCount      number of samples to play
 * @param amplitude    amplitude of the samples on playback
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos, int sampleCount, float amplitude) {
  sampleCount = synth.playSample(samplePos, sampleCount, amplitude);
  int durationMS = (int)(sampleCount/sampleRate * 1000);
  println("----- audio event duration = "+ durationMS +" millisconds");
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
  // return the length of the sample
  return sampleCount;
}


public void writeImageToAudio() {
  println("----- writing image to signal ");
  mapImage.loadPixels();
  audioSignal = new float[mapSize];
  mapper.mapImgToSig(mapImage.pixels, audioSignal);
  playBuffer.setBufferSize(mapSize);
  playBuffer.setChannel(0, audioSignal);
  if (playBuffer != null) println("--->> audioBuffer length channel 0 = "+ playBuffer.getChannel(0).length);
}

// ------------- HISTOGRAM AND GAMMA ADJUSTMENTS ------------- //

public int[] getHistoBounds(int[] source) {
  int min = 255;
  int max = 0;
  for (int i = 0; i < source.length; i++) {
    int[] comp = PixelAudioMapper.rgbComponents(source[i]);
    for (int j = 0; j < comp.length; j++) {
      if (comp[j] > max) max = comp[j];
      if (comp[j] < min) min = comp[j];
    }
  }
  println("--- min", min, " max ", max);
  return new int[]{min, max};
}

// histogram stretch -- run getHistoBounds to determine low and high
public int[] stretch(int[] source, int low, int high) {
  int[] out = new int[source.length];
  int r = 0, g = 0, b = 0;
  for (int i = 0; i < out.length; i++) {
    int[] comp = PixelAudioMapper.rgbComponents(source[i]);
    r = comp[0];
    g = comp[1];
    b = comp[2];
    r = (int) constrain(map(r, low, high, 1, 254), 0, 255);
    g = (int) constrain(map(g, low, high, 1, 254), 0, 255);
    b = (int) constrain(map(b, low, high, 1, 254), 0, 255);
    out[i] = PixelAudioMapper.composeColor(r, g, b, 255);
  }
  return out;
}

public void setGamma(float gamma) {
  if (gamma != 1.0) {
    this.gammaTable = new int[256];
    for (int i = 0; i < gammaTable.length; i++) {
      float c = i/(float)(gammaTable.length - 1);
      gammaTable[i] = (int) Math.round(Math.pow(c, gamma) * (gammaTable.length - 1));
    }
  }
}

public int[] adjustGamma(int[] source) {
  int[] out = new int[source.length];
  int r = 0, g = 0, b = 0;
  for (int i = 0; i < out.length; i++) {
    int[] comp = PixelAudioMapper.rgbComponents(source[i]);
    r = comp[0];
    g = comp[1];
    b = comp[2];
    r = gammaTable[r];
    g = gammaTable[g];
    b = gammaTable[b];
    out[i] = PixelAudioMapper.composeColor(r, g, b, 255);
  }
  return out;
}
