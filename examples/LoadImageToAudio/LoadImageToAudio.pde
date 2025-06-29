/**
 * LoadImageToAudio shows how to load an image and turn it into an audio file
 * that can be played by clicking on the image. It will probably be noisy, both because
 * it's an image and because its resolution is only 8 bits. When you load an audio, it
 * will exist both as a floating point audio signal and as an image. When you
 * click in the image, you will be playing a sample from the signal. You can write
 * the image to the audio signal ('w' key command).
 *
 * An audio signal or image can be loaded to various channels of the image: Red,
 * Green, Blue or all channels in the RGB color space or Hue or Brightness in the
 * HSB color space (We ignore Saturation for now). 
 *
 * You can enhance image contrast by stretching its histogram ('m' key).
 * You can make the image brighter ('=' and '+' keys) or darker ('-' or '_' key)
 * using a gamma function, a non-linear adjustment.
 *
 * Press ' ' to toggle animation.
 * Press 'o' to load an image or audio file to all color channels.
 * Press 'r' to load an image or audio file to the red color channel.
 * Press 'g' to load an image or audio file to the green color channel.
 * Press 'b' to load an image or audio file to the blue color channel.
 * Press 'h' to load an image or audio file to the HSB hue channel.
 * Press 'v' to load an image or audio file to the HSB saturation (vibrance) channel.
 * Press 'l' to load an image or audio file to the HSB brightness (lightness) channel.
 * Press 'O' to reload the most recent audio or image file.
 * Press 'k' or 'K' to load the color spectrum along the signal path over the most recent image.
 * Press 'm' to apply a contrast enhancement (histogram stretch) to the image.
 * Press '=' or '+' to make the image brighter
 * Press '-' or '_' to make the image darker.
 * Press 's' to save to an audio file.
 * Press 'S' to save to an image file.
 * Press 'f' to show frameRate in the console.
 * Press 'w' to write the image to the audio buffer (expect noise)
 * Press '1' to set sampleRate to 44100Hz
 * Press '2' to set sampleRate to 22050Hz
 * Press '3' to set sampleRate to 11025Hz
 * Press '4' to set sampleRate to genWidth * genHeight = 262144Hz
 * Press '?' to show this help message.
 *
 * PLEASE NOTE: Hue (H) and Saturation (V) operations may have no effect on gray pixels.
 * ALSO: Image brightness determines image audio. Images with uniform brightness will be silent.
 *
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import javax.sound.sampled.*;
import java.util.Timer;
import java.util.TimerTask;

import ddf.minim.*;
import ddf.minim.ugens.*;

import net.paulhertz.pixelaudio.*;
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
boolean isBlending = false;

/** Minim audio library */
Minim minim;              // library that handles audio
AudioOutput audioOut;     // line out to sound hardware
MultiChannelBuffer audioBuffer;    // data structure to hold audio samples
boolean isBufferStale = false;     // do we need to reset the audio buffer?
int sampleRate = 41500;   // ----->> a critical value, see the setup method <<-----
float[] audioSignal;      // the audio signal as an array
int[] rgbSignal;          // the colors in the display image, in the order the signal path visits them
int audioLength;
int audioFileLength;

// SampleInstrument setup
float sampleScale = 4;
int sampleBase = sampleRate / 4;
int samplelen = (int) (sampleScale * sampleBase);
Sampler audioSampler;     // minim class for sampled sound
WFInstrument instrument;      // local class to wrap audioSampler
// ADSR and params
ADSR adsr;                // good old attack, decay, sustain, release
float maxAmplitude = 0.9f;
float attackTime = 0.2f;
float decayTime = 0.125f;
float sustainLevel = 0.5f;
float releaseTime = 0.2f;

// audio file
String daPath;
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
boolean isLooping = true;          // looping sample (our instrument ignores this
// interaction
int sampleX;
int sampleY;
int samplePos;            // position of a mouse click along the signal path, index into the audio array
ArrayList<TimedLocation> timeLocsArray;
int count = 0;
// histogram and gamma adjustments
int histoHigh = 240;
int histoLow = 32;
float gammaUp = 0.9f;
float gammaDown = 1.2f;
int[] gammaTable;


public void settings() {
  size(3 * genWidth, 2 * genHeight);
}

public void setup() {
  frameRate(30);
  pixelaudio = new PixelAudio(this);
  minim = new Minim(this);
  // sampleRate affects image display and audio sample calculation.
  // For compatibility with other applications, including Processing, it's a good
  // idea to use a standard sampling rate, like 44100 or 48000 However, you can 
  // experiment with other sampling rates and probably can play audio and save files.
  // The behavior of files with non-standard sampling rates when open them in 
  // Processing can be unpredictable.
  //
  // Setting sampleRate = genWidth * genHeight provides interesting symmetries in
  // the image and will play audio and save to file -- but it's not a standard sample rate
  // and though Processing may open files saved with non-standard sampling rates, it
  // usually shifts the frequency according the sampleRate you have set.
  sampleRate = 44100; // = genWidth * genHeight;
  sampleBase = sampleRate / 4;
  initAudio();
  genList = new ArrayList<PixelMapGen>();
  multigen = hilbertLoop3x2(genWidth, genHeight);
  mapper = new PixelAudioMapper(multigen);
  mapSize = mapper.getSize();
  colors = getColors(); // create an array of rainbow colors
  // instantiate the display image
  mapImage = createImage(width, height, ARGB); // an image to use with mapper
  // path to the folder where PixelAudio examples keep their data files
  daPath = sketchPath("") + "../examples_data/";
  // the audio file we want to open on startup
  File audioSource = new File(daPath +"Saucer_mixdown.wav");
  // load the file into audio buffer and Brightness channel of display image (mapImage)
  fileSelected(audioSource);
  // overlay colors on mapImage
  mapImage.loadPixels();
  applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
  mapImage.updatePixels();
  // instantiate timeLocsArray
  timeLocsArray = new ArrayList<TimedLocation>();
}

public void initAudio() {
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  this.audioBuffer = new MultiChannelBuffer(1024, 1);
}

/**
 * Generates a looping fractal signal path consisting of 6 HilbertGens,
 * arranged 3 wide and 2 tall, to fit a 3 * genW by 2 * genH image. 
 * This particular MultiGen configuration was used so extensively in
 * my sample code that I've given it its own static method in HilbertGen.
 * 
 * Note that genW must be a power of 2 and genH == genW. For the 
 * image size we're using in this example, image width = 3 * genW
 * and image height = 2 * genH.
 * 
 * @param genW    width of each HilbertGen 
 * @param genH    height of each HilbertGen
 * @return
 */
public MultiGen hilbertLoop3x2(int genW, int genH) {
    // list of PixelMapGens that create a path through an image using PixelAudioMapper
  ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
  // list of x,y coordinates for placing gens from genList
  ArrayList<int[]> offsetList = new ArrayList<int[]>();     
  genList.add(new HilbertGen(genW, genH, AffineTransformType.FX270));
  offsetList.add(new int[] { 0, 0 });
  genList.add(new HilbertGen(genW, genH, AffineTransformType.NADA));
  offsetList.add(new int[] { genW, 0 });
  genList.add(new HilbertGen(genW, genH, AffineTransformType.FX90));
  offsetList.add(new int[] { 2 * genW, 0 });
  genList.add(new HilbertGen(genW, genH, AffineTransformType.FX90));
  offsetList.add(new int[] { 2 * genW, genH });
  genList.add(new HilbertGen(genW, genH, AffineTransformType.R180));
  offsetList.add(new int[] { genW, genH });
  genList.add(new HilbertGen(genW, genH,AffineTransformType.FX270));
  offsetList.add(new int[] { 0, genH });
  return new MultiGen(3 * genW, 2 * genH, offsetList, genList);
}

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
  PixelAudioMapper.rotateLeft(rgbSignal, 16);
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, mapSize);
  mapImage.updatePixels();
}

public void runTimeArray() {
  int currentTime = millis();
  timeLocsArray.forEach(tl -> {
    tl.setStale(tl.stopTime() < currentTime);
    if (!tl.isStale()) {
      drawCircle(tl.getX(), tl.getY());
    }
  });
  timeLocsArray.removeIf(TimedLocation::isStale);
}

public void drawCircle(int x, int y) {
  fill(color(233, 220, 199, 128));
  noStroke();
  circle(x, y, 60);
}  

public void keyPressed() {
  switch (key) {
  case ' ':
    isAnimating = !isAnimating;
    break;
  case 'o':
    chan = PixelAudioMapper.ChannelNames.ALL;
    chooseFile();
    break;
  case 'r':
    chan = PixelAudioMapper.ChannelNames.R;
    chooseFile();
    break;
  case 'g':
    chan = PixelAudioMapper.ChannelNames.G;
    chooseFile();
    break;
  case 'b':
    chan = PixelAudioMapper.ChannelNames.B;
    chooseFile();
    break;
  case 'h':
    chan = PixelAudioMapper.ChannelNames.H;
    chooseFile();
    break;
  case 'v':
    chan = PixelAudioMapper.ChannelNames.S;
    chooseFile();
    break;
  case 'l':
    chan = PixelAudioMapper.ChannelNames.L;
    chooseFile();
    break;
  case 'a':
    isBlending = !isBlending;
    println("-- isBlending is "+ isBlending);
    break;
  case 'O':
    if (isLoadFromImage) {
      if (imageFile == null) {
        chooseFile();
      } else {
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
  case 'c': // apply color from image file to display image
    chooseColorImage();
    break;
  case 'k': // apply the hue and saturation in the colors array to mapImage 
    mapImage.loadPixels();
    applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
    mapImage.updatePixels();
    break;
  case 'm':
    mapImage.loadPixels();
    int[] bounds = getHistoBounds(mapImage.pixels);
    mapImage.pixels = stretch(mapImage.pixels, bounds[0], bounds[1]);
    mapImage.updatePixels();
    break;
  case '=':
  case '+':
    setGamma(gammaUp);
    mapImage.loadPixels();
    mapImage.pixels = adjustGamma(mapImage.pixels);
    mapImage.updatePixels();
    break;
  case '-':
  case '_':
    setGamma(gammaDown);
    mapImage.loadPixels();
    mapImage.pixels = adjustGamma(mapImage.pixels);
    mapImage.updatePixels();
    break;
  case 's':
    saveToAudio();
    break;
  case 'S':
    saveToImage();
    break;
  case 'f':
    println("--->> frame rate: " + frameRate);
    break;
  case 'w':
    writeImageToAudio();
    println("--->> Wrote image to audio as audio data.");
    break;
  case '?':
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press ' ' to toggle animation.");
  println(" * Press 'o' to load an image or audio file to all color channels.");
  println(" * Press 'r' to load an image or audio file to the red color channel.");
  println(" * Press 'g' to load an image or audio file to the green color channel.");
  println(" * Press 'b' to load an image or audio file to the blue color channel.");
  println(" * Press 'h' to load an image or audio file to the HSB hue channel.");
  println(" * Press 'v' to load an image or audio file to the HSB saturation (vibrance) channel.");
  println(" * Press 'l' to load an image or audio file to the HSB brightness (lightness) channel.");
  println(" * Press 'c' to open an image and apply its hue and saturation to the display image brightness channel.");
  println(" * Press 'O' to reload the most recent audio or image file.");
  println(" * Press 'm' to apply a contrast enhancement (histogram stretch) to the image.");
  println(" * Press '=' or '+' to make the image brighter");
  println(" * Press '-' or '_' to make the image darker.");
  println(" * Press 's' to save to an audio file.");
  println(" * Press 'S' to save to an image file.");
  println(" * Press 'f' to show frameRate in the console.");
  println(" * Press 'w' to write the image to the audio buffer (expect noise)");
  println(" * Press '?' to show this help message.");    
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
  samplePos = mapper.lookupSample(sampleX, sampleY);
  if (audioSignal == null || isBufferStale) {
    isBufferStale = false;
  }
  playSample(samplePos);
}

public int playSample(int samplePos) {
  // create a Minim Sampler from audioBuffer with sample rate sampleRate and up to 8 voices
  audioSampler = new Sampler(audioBuffer, sampleRate, 8); 
  audioSampler.amplitude.setLastValue(0.9f);  // set amplitude for the Sampler
  audioSampler.begin.setLastValue(samplePos); // set the Sampler to begin playback at samplePos
  int releaseDuration = (int) (releaseTime * sampleRate); // do some calculation to include the release time.
  float vary = (float) (PixelAudio.gauss(this.sampleScale, this.sampleScale * 0.125f)); // vary duration
  // println("----->>> vary = "+ vary +", sampleScale = "+ sampleScale);
  this.samplelen = (int) (vary * this.sampleBase); // calculate the duration of the sample
  if (samplePos + samplelen >= mapSize) {
    samplelen = mapSize - samplePos; // make sure we don't exceed the mapSize
    println("----->>> sample length = " + samplelen);
  }
  int durationPlusRelease = this.samplelen + releaseDuration;
  int end = (samplePos + durationPlusRelease >= this.mapSize) ? this.mapSize - 1
      : samplePos + durationPlusRelease;
  // println("----->>> end = " + end);
  audioSampler.end.setLastValue(end);
  // ADSR envelope with max amplitude, attack Time, decay time, sustain level, release time
  adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  this.instrument = new WFInstrument(audioOut, audioSampler, adsr);
  // play command takes a duration in seconds
  float duration = samplelen / (float) (sampleRate);
  instrument.play(duration);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, (int) (duration * 1000) + millis()));
  // return the length of the sample
  return samplelen;
}


public void writeImageToAudio() {
  println("----- writing image to signal ");
  mapImage.loadPixels();
  audioSignal = new float[mapSize];
  mapper.mapImgToSig(mapImage.pixels, audioSignal);
  audioBuffer.setBufferSize(mapSize);
  audioBuffer.setChannel(0, audioSignal);
  if (audioBuffer != null) println("--->> audioBuffer length channel 0 = "+ audioBuffer.getChannel(0).length);
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

 
