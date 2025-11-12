/**
 *
 * LoadAudioToImage shows how you can open audio files and transcode their image data to an image.
 * Once the image is loaded, you can click on it to play back the audio using a PASamplerInstrument.
 * The hightlightSample() method will highlight the pixels that correspond to the audio signal
 * that is played. The highlight will change the pixels and can change the audio, too: just press
 * the 'w' key to transcode the image to an audio signal and write it to the PASamplerInstrument.
 * You can also load audio to individual RGB or HSB Hue and Brightness channels. To hear the
 * results of loading to different channels, write the image to the audio signal ('w' key) and
 * click in the image.
 *
 * This sketch also includes some common methods for adjusting the brightness and contrast of
 * an image: gamma adjustment changes contrast up of down and histogram equalization stretches
 * the range of brightness values in an image. These operations will change the audio, too, if
 * you write the image to the audio signal. The gamma operations both make the audio quieter,
 * while the histogram stretch usually makes the audio louder.
 *
 * Press 'o' or 'O' to open an audio file and load it to the signal and all channels of the image.
 * Press 'r' to open an audio file and load it to the signal and the RED channel of the image.
 * Press 'g' to open an audio file and load it to the signal and the GREEN channel of the image.
 * Press 'b' to open an audio file and load it to the signal and the BLUE channel of the image.
 * Press 'l' to open an audio file and load it to the signal and the HSB Brightness channel of the image.
 * Press 'h' to open an audio file and load it to the signal and the HSB Hue channel of the image.
 * Press 'k' to overlay the rainbow signal path on the image.
 * Press 'm' to equalize the histogram of the image.
 * Press '+' or '=' to apply gamma value  "gammaLighter" to the image.
 * Press '-' or '_' to apply gamma value  "gammaDarker" to the image.
 * Press 'w' to transcode the image HSB Brightness channel and write it to the signal.
 * Press 's' to save the current display image as a PNG file.
 * Press '?' to show Help Message in the console.
 *
 */

import java.util.Random;
import java.util.Arrays;
import java.io.File;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.voices.*;

import ddf.minim.*;

/** PixelAudio library */
PixelAudio pixelaudio;        // PixelAudio library for Processing
HilbertGen hGen;              // PixelMapGen that produces signal paths along a Hilbert curve
PixelAudioMapper mapper;      // a PixelAudioMapper to mediate between image and signal
int mapSize;                  // length of mapImage.pixels[] and audioSignal
PImage mapImage;              // offscreen image to which we write image and transcoded audio data
int[] colors;                 // an array of spectral colors to fill the signal path
PixelAudioMapper.ChannelNames chan;    // a color channel in the RGB or HSB color space

/** Minim audio library */
Minim minim;                  // Minim audio library for Processing
AudioOutput audioOut;         // audio output used by Minim
MultiChannelBuffer playBuffer;    // an audio buffer used to load audio from a file to the PASamplerInstrument synth
float sampleRate = 44100;     // sample rate for audioOut
float[] audioSignal;          // audio data as an array of floats in the range (-1.0f..1.0f)
int[] rgbSignal;              // pixel data ordered along the signal path
int audioLength;              // length of audioSignal (should equal mapSize)

// SampleInstrument setup
int duration = 2000;          // milliseconds duration
PASamplerInstrument synth;    // sampling instrument to play audio

// ADSR and params
ADSRParams adsr;              // an ADSR envelope that wraps Minim's ADSR class
float maxAmplitude = 0.9f;
float attackTime = 0.2f;
float decayTime = 0.125f;
float sustainLevel = 0.5f;
float releaseTime = 0.5f;

// audio file
File audioFile;
String audioFilePath;
String audioFileName;
String audioFileTag;

// Java random number generator
Random rando;
// some colors
int roig = 0xf64c2f;
int groc = 0xf6e959;
int blau = 0x5990e9;
int vert = 0x7bb222;
int taronja = 0xfea537;
int violet = 0xb29de9;
// array of colors, for random selection
public int[] randColors = { blau, groc, roig, vert, violet, taronja };

// interaction variables
int pixelPos;                 // index into the mapImage.pixels[] array
int samplePos;                // index into audioSignal
int blendAlpha = 64;          // value for blending colors

// histogram and gamma adjustments
int histoHigh = 240;          // RGB value, upper bound for histogram stretch
int histoLow = 32;            // RGB value, lower bound for histogram stretch
float gammaLighter = 0.9f;    //
float gammaDarker = 1.2f;
int[] gammaTable;


public void settings() {
  size(1024, 1024);
}

public void setup() {
  initMapper();    // set up mapper and load mapImage with color wheel
  initAudio();    // set up audio
  rando = new Random();
  chan = PixelAudioMapper.ChannelNames.L;
  String path = "/Users/paulhz/Documents/Processing/libraries/PixelAudio/examples/LoadAudioToImage/data";
  File audioSource = new File(path +"/youthorchestra.wav");
  fileSelected(audioSource);
  showHelp();
}

public void initMapper() {
  pixelaudio = new PixelAudio(this);     // load the PixelAudio library
  hGen = new HilbertGen(width, height);   // create a Hilbert curve that fills our display
  mapper = new PixelAudioMapper(hGen);   // initialize mapper with the HIlbert curve generator
  mapSize = mapper.getSize();        // size of mapper's various arrays and of mapImage
  colors = getColors();           // create an array of colors
  mapImage = createImage(width, height, ARGB); // an image to use with mapper
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
  mapImage.updatePixels();
}

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

public int[] getColors() {
  int[] colorWheel = new int[mapSize];       // an array for our colors
  pushStyle();                   // save styles
  colorMode(HSB, colorWheel.length, 100, 100);   // pop over to the HSB color space and give hue a very wide range
  for (int i = 0; i < colorWheel.length; i++) {
    colorWheel[i] = color(i, 40, 75);       // fill our array with colors, gradually changing hue
  }
  popStyle();                   // restore styles, including the default RGB color space
  return colorWheel;
}

public void draw() {
  image(mapImage, 0, 0);
}

public void keyPressed() {
  switch (key) {
  case 'o': // open an audio file and load it to the signal and all channels of the image
  case 'O':
    chan = PixelAudioMapper.ChannelNames.ALL;
    chooseFile();
    break;
  case 'r': // open an audio file and load it to the signal and the RED channel of the image
    chan = PixelAudioMapper.ChannelNames.R;
    chooseFile();
    break;
  case 'g': // open an audio file and load it to the signal and the GREEN channel of the image
    chan = PixelAudioMapper.ChannelNames.G;
    chooseFile();
    break;
  case 'b': // open an audio file and load it to the signal and the BLUE channel of the image
    chan = PixelAudioMapper.ChannelNames.B;
    chooseFile();
    break;
  case 'l': // open an audio file and load it to the signal and the HSB Brightness channel of the image
    chan = PixelAudioMapper.ChannelNames.L;
    chooseFile();
    break;
  case 'h': // open an audio file and load it to the signal and the HSB Hue channel of the image
    chan = PixelAudioMapper.ChannelNames.H;
    chooseFile();
    break;
  case 'k': // overlay the rainbow signal path on the image
    mapImage.loadPixels();
    applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
    mapImage.updatePixels();
    break;
  case 'c':
    break;
  case 'm': // equalize the histogram of the image
    mapImage.loadPixels();
    int[] bounds = getHistoBounds(mapImage.pixels);
    mapImage.pixels = histoStretch(mapImage.pixels, bounds[0], bounds[1]);
    mapImage.updatePixels();
    break;
  case '=': // apply gamma value  \"gammaLighter\" to the image
  case '+':
    setGamma(gammaLighter);
    mapImage.loadPixels();
    mapImage.pixels = adjustGamma(mapImage.pixels);
    mapImage.updatePixels();
    break;
  case '-': // apply gamma value  \"gammaDarker\" to the image
  case '_':
    setGamma(gammaDarker);
    mapImage.loadPixels();
    mapImage.pixels = adjustGamma(mapImage.pixels);
    mapImage.updatePixels();
    break;
  case 'w': // transcode the image HSB Brightness channel and write it to the signal
    writeImageToAudio();
    break;
  case 's': // save the current display image as a PNG file
    mapImage.save("pixelAudio.png");
    println("--- saved display image to pixelAudio.png");
    break;
  case '?': // show Help Message in the console
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press 'o' or 'O' to open an audio file and load it to the signal and all channels of the image.");
  println(" * Press 'r' to open an audio file and load it to the signal and the RED channel of the image.");
  println(" * Press 'g' to open an audio file and load it to the signal and the GREEN channel of the image.");
  println(" * Press 'b' to open an audio file and load it to the signal and the BLUE channel of the image.");
  println(" * Press 'l' to open an audio file and load it to the signal and the HSB Brightness channel of the image.");
  println(" * Press 'h' to open an audio file and load it to the signal and the HSB Hue channel of the image.");
  println(" * Press 'k' to overlay the rainbow signal path on the image.");
  println(" * Press 'm' to equalize the histogram of the image.");
  println(" * Press '+' or '=' to apply gamma value  \"gammaLighter\" to the image.");
  println(" * Press '-' or '_' to apply gamma value  \"gammaDarker\" to the image.");
  println(" * Press 'w' to transcode the image HSB Brightness channel and write it to the signal.");
  println(" * Press 's' to save the current display image as a PNG file.");
  println(" * Press '?' to show Help Message in the console.");
}

public void mousePressed() {
  pixelPos = mouseX + mouseY * width;
  samplePos = mapper.lookupSample(mouseX, mouseY);
  // println("----- sample position for "+ mouseX +", "+ mouseY +" is "+ samplePos);
  int varyDuration = calcSampleLen(duration);
  int sampleLength = playSample(samplePos, varyDuration, 0.9f);
  if (sampleLength > 0) {
    hightlightSample(samplePos, (int)(2 * sampleRate));
    // int c = mapImage.get(mouseX, mouseY);
    // String str = PixelAudioMapper.colorString(c);
    // println("--- samplePos:", samplePos, "sampleLength:", sampleLength, "size:", width * height, "end:", samplePos + sampleLength + ", " + str);
  }
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
  println("---- calcSampleLen result = "+ len +" samples at "+ sampleRate +" Hz sample rate");
  return len;
}

public void hightlightSample(int pos, int length) {
  shuffle(randColors);
  int highColor = PixelAudioMapper.setAlpha(randColors[0], blendAlpha);
  // watch out for the end of the image pixels!
  if (pos + length > mapSize - 1) {
    length = mapSize - pos - 1;
  }
  int[] signalPathPixelSequence = mapper.pluckPixels(mapImage.pixels, pos, length);
  mapImage.loadPixels();
  for (int i = 0; i < length; i++) {
    int newColor = blendColor(mapImage.pixels[pos + i], highColor, BLEND);
    signalPathPixelSequence[i] = newColor;
  }
  mapper.plantPixels(signalPathPixelSequence, mapImage.pixels, pos, length);
  mapImage.updatePixels();
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
