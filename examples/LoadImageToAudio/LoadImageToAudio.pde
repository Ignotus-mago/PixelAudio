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
HilbertGen hGen;           // a PixelMapGen to draw Hilbert curves
MultiGen multigen;         // a PixelMapGen that handles multiple gens
ArrayList<PixelMapGen> genList;    // list of PixelMapGens that create an image using mapper
ArrayList<int[]> offsetList;       // list of x,y coordinates for placing gens from genList
int rows = 3;
int columns = 2;
int genWidth = 512;       // width of PixelMapGen objects, for hGen must be a power of 2
int genHeight = 512;      // height of PixelMapGen objects, for hGen must be equal to width
PixelAudioMapper mapper;  // object for reading, writing, and transcoding audio and image data
int mapSize;              // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
PImage mapImage;          // image for display
PixelAudioMapper.ChannelNames chan;
int[] colors;             // array of spectral colors

/** Minim audio library */
Minim minim;              // library that handles audio
AudioOutput audioOut;     // line out to sound hardware
MultiChannelBuffer audioBuffer;    // data structure to hold audio samples
boolean isBufferStale = false;     // do we need to reset the audio buffer?
int sampleRate = 44100;   // ----->> a critical value, see the setup method <<-----
float[] audioSignal;      // the audio signal as an array
int[] rgbSignal;          // the colors in the display image, in the order the signal path visits them
int audioLength;

// SampleInstrument setup
float sampleScale = 4;
int sampleBase = sampleRate / 4;
int samplelen = (int) (sampleScale * sampleBase);
Sampler audioSampler;     // minim class for sampled sound
SamplerInstrument instrument;      // local class to wrap audioSampler
// ADSR and params
ADSR adsr;                // good old attack, decay, sustain, release
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

String daPath;                     // path to media files
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
  size(rows * genWidth, columns * genHeight);
}

public void setup() {
  frameRate(30);
  pixelaudio = new PixelAudio(this);
  chan = PixelAudioMapper.ChannelNames.ALL;
  minim = new Minim(this);
  // sampleRate affects image display and audio sample calculation.
  // For compatibility with other applications, including Processing, it's a good
  // idea to use a standard sampling rate, like 44100. However, you can experiment
  // with other sampling rates and probably can play audio and and save files.
  // Their behavior in Processing when you try to open them can be unpredictable.
  sampleRate = 44100; // = genWidth * genHeight;
  sampleBase = sampleRate / 4;
  initAudio();
  genList = new ArrayList<PixelMapGen>();
  offsetList = new ArrayList<int[]>();
  loadGenLists();
  multigen = new MultiGen(rows * genWidth, columns * genHeight, offsetList, genList);
  mapper = new PixelAudioMapper(multigen);
  mapSize = mapper.getSize();
  colors = getColors(); // create an array of rainbow colors
  mapImage = createImage(width, height, ARGB); // an image to use with mapper
  timeLocsArray = new ArrayList<TimedLocation>();
  // path to the folder where PixelAudio examples keep their data files 
  // such as image, audio, .json, etc.
  daPath = sketchPath("") + "../examples_data/";
  // saucer_image.png is an image generated from an audio file
  imageFile = new File(daPath + "saucer_image.png");
  loadColors();
  showHelp();
}

public void initAudio() {
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  this.audioBuffer = new MultiChannelBuffer(1024, 1);
}

/**
 * Adds PixelMapGen objects to the genList. The genList will be used to
 * initialize a MultiGen, which in turn is passed to a WaveSynth.
 */
public void loadGenLists() {
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FX270));
  offsetList.add(new int[] { 0, 0 });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.NADA));
  offsetList.add(new int[] { genWidth, 0 });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FX90));
  offsetList.add(new int[] { 2 * genWidth, 0 });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FX90));
  offsetList.add(new int[] { 2 * genWidth, genHeight });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.R180));
  offsetList.add(new int[] { genWidth, genHeight });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FX270));
  offsetList.add(new int[] { 0, genHeight });
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

public void loadColors() {
  PixelAudioMapper.ChannelNames oldChan = chan;
  chan = PixelAudioMapper.ChannelNames.ALL;
  mapImage.loadPixels();
  // load the color spectrum in "colors" to mapImage
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize, chan); // load colors to mapImage following signal path
  mapImage.updatePixels();
  if (imageFile != null) {
    // use the L = brightness in the HSB color space as the channel. Color information will remain intact.
    chan = PixelAudioMapper.ChannelNames.L;
    loadImageFile(imageFile);
  }
  chan = oldChan;
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
  case 'O':
    if (audioFile == null && imageFile == null) {
      chooseFile();
    } else {
      if (isLoadFromImage) {
        // reload image
        loadImageFile(imageFile);
        println("-------->>>>> Reloaded image file");
      } else {
        // reload image
        loadAudioFile(audioFile);
        println("-------->>>>> Reloaded audio file");
      }
    }
    break;
  case 'k': case 'K':
    loadColors();
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
  case '1':
    sampleRate = 44100;
    break;
  case '2':
    sampleRate = 22050;
    break;
  case '3':
    sampleRate = 11025;
    break;
  case '4':
    sampleRate = genWidth * genHeight;
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
  println(" * Press 'O' to reload the most recent audio or image file.");
  println(" * Press 'k' or 'K' to load the color spectrum along the signal path over the most recent image.");
  println(" * Press 'm' to apply a contrast enhancement (histogram stretch) to the image.");
  println(" * Press '=' or '+' to make the image brighter");
  println(" * Press '-' or '_' to make the image darker.");
  println(" * Press 's' to save to an audio file.");
  println(" * Press 'S' to save to an image file.");
  println(" * Press 'f' to show frameRate in the console.");
  println(" * Press 'w' to write the image to the audio buffer (expect noise)");
  println(" * Press '1' to set sampleRate to 44100Hz");
  println(" * Press '2' to set sampleRate to 22050Hz");
  println(" * Press '3' to set sampleRate to 11025Hz");
  println(" * Press '4' to set sampleRate to genWidth * genHeight = 262144Hz");
  println(" * Press '?' to show this help message.");
  println(" * ");
  println(" * PLEASE NOTE: Hue (H) and Saturation (V) may have no effect on gray pixels.");
  println(" * ALSO: Image brightness determines image audio. Images with uniform brightness will be silent.");

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
  audioSampler = new Sampler(audioBuffer, sampleRate, 8); // create a Minim Sampler from the buffer sampleRate sampling rate, for up to 8 simultaneous outputs
  audioSampler.amplitude.setLastValue(0.9f);              // set amplitude for the Sampler
  audioSampler.begin.setLastValue(samplePos);             // set the Sampler to begin playback at samplePos, which corresponds to the place the mouse was clicked
  int releaseDuration = (int) (releaseTime * sampleRate); // do some calculation to include the release time.
  float vary = (float) (PixelAudio.gauss(this.sampleScale, this.sampleScale * 0.125f)); // vary the duration of the signal
  // println("----->>> vary = "+ vary +", sampleScale = "+ sampleScale);
  this.samplelen = (int) (vary * this.sampleBase);        // calculate the duration of the sample
  if (samplePos + samplelen >= mapSize) {
    samplelen = mapSize - samplePos;                      // make sure we don't exceed the mapSize
    println("----->>> sample length = " + samplelen);
  }
  int durationPlusRelease = this.samplelen + releaseDuration;
  int end = (samplePos + durationPlusRelease >= this.mapSize) ? this.mapSize - 1
      : samplePos + durationPlusRelease;
  // println("----->>> end = " + end);
  audioSampler.end.setLastValue(end);
  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
  adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  this.instrument = new SamplerInstrument(audioOut, audioSampler, adsr);
  // play command takes a duration in seconds
  float duration = samplelen / (float) (sampleRate);
  instrument.play(duration);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, (int) (duration * 1000) + millis()));
  // return the length of the sample
  return samplelen;
}


public void writeImageToAudio() {
  println("----- writing image to signal ");
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
  audioBuffer.setBufferSize(mapSize);
  mapImage.loadPixels();
  // fetch pixels from mapImage in signal order, put them in rgbSignal
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, rgbSignal.length);
  // write the Brightness channel of rgbPixels, transcoded to audio range, to audioBuffer
  mapper.plantSamples(rgbSignal, audioBuffer.getChannel(0), 0, mapSize, PixelAudioMapper.ChannelNames.L);
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
