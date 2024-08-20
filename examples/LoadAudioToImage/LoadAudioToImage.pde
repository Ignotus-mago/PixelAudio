/**
 * LoadAudioToImage shows how you can open audio files and transcode their image data to an image.
 * Once the image is loaded, you can click on it to play back the audio, which is stored in a 
 * MultiChannelBuffer. The <code>hightlightSample</code> method will highlight the pixels 
 * that correspond to the audio signal that is playes. The highlight will change the pixels, 
 * and can change the audio, too: just press the 'w' key to write the image, transcoded as an
 * audio signal, to the audio buffer. 
 * 
 * You can also load audio to individual RGB or HSB Hue and Brightness channels. To hear the 
 * results of loading to different channels, write the image to the buffer ('w' key) and 
 * click in the image. 
 * 
 * 
 * Press 'o'or 'O' to open an audio file.
 * Press 'r' to open an audio file and load it to the RED channel.
 * Press 'g' to open an audio file and load it to the GREEN channel.
 * Press 'b' to open an audio file and load it to the BLUE channel.
 * Press 'l' to open an audio file and load it to the HSB BRIGHTNESS channel.
 * Press 'h' to open an audio file and load it to the HSB HUE channel.
 * Press 'm' to apply histogram stretch to the image.
 * Press '=' or '+' to adjust gamma upwards.
 * Press '-' or '_' to adjust gamma downwards.
 * Press 'w' to write the current pixel values to the audio buffer.
 * Press 's' to to save the current image and audio buffer.
 * Press '?' to print the key commands to the console.
 * 
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

import ddf.minim.*;
import ddf.minim.ugens.*;

/** PixelAudio library */
PixelAudio pixelaudio;
HilbertGen hGen;
PixelAudioMapper mapper;
int mapSize;
PImage mapImage;
int[] colors;
PixelAudioMapper.ChannelNames chan;

/** Minim audio library */
Minim minim;
AudioOutput audioOut;
MultiChannelBuffer audioBuffer;
int sampleRate = 44100;
float[] audioSignal;
int[] rgbSignal;
int audioLength;

// SampleInstrument setup
float sampleScale = 2;
int sampleBase = 10250;
int sampleLength = (int) (sampleScale * sampleBase);
Sampler audioSampler;
SamplerInstrument instrument;

// ADSR and params
ADSR adsr;
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

// Java random number generator
Random rando;

// interaction variables
int pixelPos;
int samplePos;
int blendAlpha = 64;

// histograma and gamma adjustments
int histoHigh = 240;
int histoLow = 32;
float gammaUp = 0.9;
float gammaDown = 1.2;
int[] gammaTable;


public void settings() {
  size(1024, 1024);
}

public void setup() {
  // set up mapper and load mapImage with color wheel
  initMapper();
  initAudio();
  rando = new Random();
  // load transcoded data from an audio file into the 
  // Brightness channel (ChannelNames.L) in the HSB color space
  chan = PixelAudioMapper.ChannelNames.L;
  String path = this.dataPath("");
  File audioSource = new File(path +"/youthorchestra.wav");
  // load the file into audio buffer and Brightness channel of display image
  fileSelected(audioSource);
}

public void initMapper() {
  pixelaudio = new PixelAudio(this);       // load the PixelAudio library
  hGen = new HilbertGen(width, height);    // create a Hilbert curve that fills our display
  mapper = new PixelAudioMapper(hGen);     // initialize mapper with the Hilbert curve generator
  mapSize = mapper.getSize();              // size of mapper's various arrays and of mapImage
  colors = getColors();                    // create an array of rainbow colors
  mapImage = createImage(width, height, ARGB); // an image to use with mapper
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
  mapImage.updatePixels();
}

public void initAudio() {
  this.minim = new Minim(this);
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  this.audioBuffer = new MultiChannelBuffer(1024, 1);
}

public int[] getColors() {
  int[] colorWheel = new int[mapSize];       // an array for our colors
  pushStyle();                               // save styles
  colorMode(HSB, colorWheel.length, 100, 100);   // pop over to the HSB color space and give hue a very wide range
  for (int i = 0; i < colorWheel.length; i++) {
    colorWheel[i] = color(i, 40, 75);       // fill our array with colors, gradually changing hue
  }
  popStyle();                               // restore styles, including the default RGB color space
  return colorWheel;
}

public void draw() {
  image(mapImage, 0, 0);
}

public void keyPressed() {
  switch (key) {
  case 'o':
  case 'O':
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
  case 'l':
    chan = PixelAudioMapper.ChannelNames.L;
    chooseFile();
    break;
  case 'h':
    chan = PixelAudioMapper.ChannelNames.H;
    chooseFile();
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
  case 'w':
    writeImageToAudio();
    break;
  case 's':
    mapImage.save("pixelAudio.png");
    println("--- saved display image to pixelAudio.png");
    saveToAudio("pixelAudio.wav");
    println("--- saved audio to pixelAudio.wav");
    break;
  case '?':
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println("Press 'o'or 'O' to open an audio file.");
  println("Press 'r' to open an audio file and load it to the RED channel.");
  println("Press 'g' to open an audio file and load it to the GREEN channel.");
  println("Press 'b' to open an audio file and load it to the BLUE channel.");
  println("Press 'l' to open an audio file and load it to the HSB BRIGHTNESS channel.");
  println("Press 'h' to open an audio file and load it to the HSB HUE channel.");
  println("Press 'm' to apply histogram stretch to the image.");
  println("Press '=' or '+' to adjust gamma upwards.");
  println("Press '-' or '_' to adjust gamma downwards.");
  println("Press 'w' to write the current pixel values to the audio buffer.");
  println("Press 's' to to save the current image and audio buffer.");
  println("Press '?' to print the key commands to the console.");  
}

public void mousePressed() {
  pixelPos = mouseX + mouseY * width;
  samplePos = mapper.lookupSample(mouseX, mouseY);
  // println("----- sample position for "+ mouseX +", "+ mouseY +" is "+ samplePos);
  int sampleLength = playSample(samplePos);
  if (sampleLength > 0) {
    hightlightSample(samplePos, sampleLength);
    // int c = mapImage.get(mouseX, mouseY);
    // String str = PixelAudioMapper.colorString(c);
    // println("--- samplePos:", samplePos, "sampleLength:", sampleLength, "size:", width * height, "end:", samplePos + sampleLength + ", " + str);
  }
}

public int playSample(int samplePos) {
  if (audioFile == null)
    return 0;
  // create a new ddf.minim.ugens.Sampler from the buffer with 44.1 KHz sampling rate
  // it seems I have to do this every time I want to play a sound, not sure why
  // I can't reuse the Sampler once it's been created -- if I don't the audio 
  // sounds wrong, as if the sampling rate had changed.
  audioSampler = new Sampler(audioBuffer, sampleRate, 8); 
  // ADSR 
  adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  // println("--- creating sampler ---");
  // set amplitude for the Sampler
  audioSampler.amplitude.setLastValue(0.9f); 
  // set the Sampler to begin playback at samplePos, which corresponds to the place the mouse was clicked
  audioSampler.begin.setLastValue(samplePos); 
  int releaseDuration = (int) (releaseTime * sampleRate); // do some calculation to include the release time.
  // set amplitude for the Sampler using a statistical distribution
  float vary = (float) (gauss(this.sampleScale, this.sampleScale * 0.125f)); 
  // println("----->>> vary = "+ vary +", sampleScale = "+ sampleScale);
  this.sampleLength = (int) (vary * this.sampleBase); // calculate the duration of the sample
  if (samplePos + sampleLength >= mapSize) {
    sampleLength = mapSize - samplePos; // make sure we don't exceed the mapSize
    println("----->>> sample length = " + sampleLength);
  }
  int durationPlusRelease = this.sampleLength + releaseDuration;
  int end = (samplePos + durationPlusRelease >= this.mapSize) ? this.mapSize - 1
    : samplePos + durationPlusRelease;
  // println("----->>> end = " + end);
  audioSampler.end.setLastValue(end);
  this.instrument = new SamplerInstrument(audioSampler, adsr);
  // play command takes a duration in seconds
  instrument.play(sampleLength / (float) (sampleRate));
  // return the length of the sample
  return sampleLength;
}

public void hightlightSample(int pos, int length) {
  shuffle(randColors);
  int highColor = PixelAudioMapper.setAlpha(randColors[0], blendAlpha);
  int[] signalPathPixelSequence = mapper.pluckPixels(mapImage.pixels, pos, sampleLength);
  mapImage.loadPixels();
  for (int i = 0; i < length; i++) {
    int newColor = blendColor(mapImage.pixels[pos + i], highColor, BLEND);
    signalPathPixelSequence[i] = newColor;
  }
  mapper.plantPixels(signalPathPixelSequence, mapImage.pixels, pos, length);
  mapImage.updatePixels();
}

public void writeImageToAudio() {
  println("----- writing image to signal ");
  mapImage.loadPixels();
  // fetch pixels from mapImage in signal order, put them in rgbSignal
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, rgbSignal.length);
  // write the Brightness channel of rgbPixels, transcoded to audio range, to audioBuffer
  mapper.plantSamples(rgbSignal, audioBuffer.getChannel(0), 0, mapSize, PixelAudioMapper.ChannelNames.L);
}
