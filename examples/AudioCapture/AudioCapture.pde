import processing.sound.*;

/**
 * AudioCapture shows how you can capture streaming audio from live input or from a file.
 * The live input in the Processing IDE seems to be limited to the built-in microphone. 
 * Live input is not available for Processing running in the Eclipse IDE. 
 * I'm sure there are some Java workarounds for both situations. 
 * 
 * Press the 'p' key to toggle between live streaming from the built-in microphone and streaming from a file. 
 * Press the spacebar to record audio from the current stream into the audio buffer and write it to the screen.
 * Click on the image to play a sample. Clicking turns off recording. 
 */

import java.util.Arrays;

import net.paulhertz.pixelaudio.*;
import processing.core.*;
import ddf.minim.*;
import ddf.minim.ugens.*;


/** PixelAudio library */
public PixelAudio pixelaudio;
/** A PixelMapGen to generate the signal path */
HilbertGen hGen;
/** A PixelAudioMapper to negotiate between image and audio */
PixelAudioMapper mapper;
/** the color channel we will draw to */
PixelAudioMapper.ChannelNames chan;

/** image for display */
PImage mapImage;
/** the number of pixels in the image, the number of samples in the audio signal, etc. */
int mapSize;
/** an array of colors through the RGB spectrum, handy for showing the shape of the signal path */
int[] colors;

/** Minim audio library */
Minim minim;
AudioInput audioIn;
AudioOutput audioOut;
/** A buffer for audio that we stream */
MultiChannelBuffer audioBuffer;
/** A Minim audio sampler to construct a sampling instrument */
Sampler audioSampler;
/** An audio recorder for capturing input from the microphone */
AudioRecorder recorder;
/** Our homemade audio sampling instrument */
WFInstrument instrument;
/** A source for streaming audio from a file */
AudioPlayer anthem;
/** The class that captures audio for us */
StreamCapture streamCap;
/** audio signal as an array of floats */
float[] audioSignal;
/** audio signal transcoded to RGB data */
int[] rgbSignal;
int audioLength;
/** audio sampling rate */
int sampleRate = 44100;
/** duration of a sample played by the WFInstrument, in seconds */
float sampleLength = 1.0f;
/** ADSR and parameters */
ADSR adsr;
float maxAmplitude = 0.9f;
float attackTime = 0.2f;
float decayTime = 0.125f;
float sustainLevel = 0.5f;
float releaseTime = 0.2f;
/** audio variables */
boolean listening = false;
boolean listenLive = true;


/**
 *  For the HilbertGen we require width == height == a power of 2. 
 */
public void settings() {
  size(512, 512);
}

public void setup() {
  pixelaudio = new PixelAudio(this);
  initMapper();
  mapSize = mapper.getSize();
  Sound.list();
  Sound.inputDevice(6);
  Sound.outputDevice(6);
  minim = new Minim(this);
  initAudio();
  showHelp();
}

public int[] getColors() {
  int[] colorWheel = new int[mapper.getSize()];
  pushStyle();
  colorMode(HSB, colorWheel.length, 100, 100);
  int h = 0;
  for (int i = 0; i < colorWheel.length; i++) {
    colorWheel[i] = color(h, 15, 80);
    h++;
  }
  popStyle();
  return colorWheel;
}

public void initMapper() {
  hGen = new HilbertGen(width, height);
  mapper = new PixelAudioMapper(hGen);
  chan = PixelAudioMapper.ChannelNames.L;
  mapImage = createImage(width, height, RGB);
  mapImage.loadPixels();
  mapper.plantPixels(getColors(), mapImage.pixels, 0, mapper.getSize());
  mapImage.updatePixels();
}

public void initAudio() {
  this.audioIn = minim.getLineIn(Minim.MONO);
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  this.audioBuffer = new MultiChannelBuffer(mapSize, 1);
  this.audioSignal = new float[mapSize];
  this.rgbSignal = new int[mapSize];
  Arrays.fill(audioSignal, 0.0f);
  this.audioLength = audioSignal.length;
  this.audioBuffer.setChannel(0, audioSignal);
  streamCap = new StreamCapture();
  // path to the folder where PixelAudio examples keep their data files 
  // such as image, audio, .json, etc.
  String daPath = sketchPath("") + "../examples_data/";
  println("daPath: ", daPath);
  anthem = minim.loadFile(daPath + "youthorchestra.wav");
}  

public void draw() {
  image(mapImage, 0, 0);
  showAudioStatus();
  if (listening) {
    drawSignal();
  }
  drawInput();
}

public void rewindAudioPlayer(AudioPlayer player, boolean playAgain) {
  player.cue(0);
  if (playAgain) player.play();
}

public void showAudioStatus() {
  textSize(18);
  text("Listening is " + listening + "; playing is " + anthem.isPlaying() +" at "+ anthem.position() 
        +" of "+ anthem.length(), 6, 18);    
}

public void keyPressed() {
  switch (key) {
  case ' ':
    toggleListening();
    break;
  case 'p':
  case 'P':
    // change the audio source, but only if we are not listening
    if (!listening) {
      listenLive = !listenLive;
      println("-- listenLive = " + listenLive);
      if (listenLive)
        anthem.pause();
      else
        anthem.loop();
    }
    break;
  case 'm':
    if (anthem.isMuted()) {
      anthem.unmute();
    }
    else {
      anthem.mute();
    }
    println("==== v ===");
    break;
  case 'h':
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press the 'p' key to toggle between live streaming from the built-in microphone and streaming from a file.");
  println(" * Press the spacebar to record audio from the current stream into the audio buffer and write it to the screen.");
  println(" * Click on the image to play a sample. Clicking turns off recording.");
}

public void toggleListening() {
  if (listening) {
    if (listenLive)
      audioIn.removeListener(streamCap);
    else
      anthem.removeListener(streamCap);
    listening = false;
  } 
  else {
    if (listenLive)
      audioIn.addListener(streamCap);
    else
      anthem.addListener(streamCap);
    listening = true;
  }
}

public void mousePressed() {
  if (listening) {
    //toggleListening();
    //if (anthem.isPlaying())
    //  anthem.pause();
  }
  // get the position in the audio buffer that corresponds to the pixel location in the image
  int samplePos = mapper.lookupSample(mouseX, mouseY);
  playSample(samplePos);
}

public int playSample(int samplePos) {
  audioSampler = new Sampler(audioBuffer, sampleRate, 8);
  // ADSR
  adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  // set amplitude for the Sampler
  audioSampler.amplitude.setLastValue(0.9f);
  // set the Sampler to begin playback at samplePos, which corresponds to the
  // place the mouse was clicked
  audioSampler.begin.setLastValue(samplePos);
  int releaseDuration = (int) (releaseTime * sampleRate); // do some calculation to include the release time.
  int duration = (int) (sampleLength * sampleRate);
  if (samplePos + sampleLength >= mapSize) {
    duration = mapSize - samplePos; // make sure we don't exceed the mapSize
    // println("----->>> duraton = " + duration);
  }
  int durationPlusRelease = duration + releaseDuration;
  int end = (samplePos + durationPlusRelease >= this.mapSize) ? this.mapSize - 1
      : samplePos + durationPlusRelease;
  // println("----->>> end = " + end);
  audioSampler.end.setLastValue(end);
  // println("----->>> audioBuffer size = "+ audioBuffer.getBufferSize());
  this.instrument = new WFInstrument(audioOut, audioSampler, adsr);
  // play command takes a duration in seconds
  float dur = duration / (float) (sampleRate);
  instrument.play(dur);
  // println("----->>> duration = "+ dur);
  // return the length of the sample
  return duration;
}

public void drawInput() {
  if (listenLive) {
    for (int i = 0; i < audioIn.bufferSize() - 1; i++) {
      line(i, 50 + audioIn.left.get(i) * 50, i + 1, 50 + audioIn.left.get(i + 1) * 50);
      line(i, 150 + audioIn.right.get(i) * 50, i + 1, 150 + audioIn.right.get(i + 1) * 50);
    }
  } else {
    for (int i = 0; i < anthem.bufferSize() - 1; i++) {
      line(i, 50 + anthem.left.get(i) * 50, i + 1, 50 + anthem.left.get(i + 1) * 50);
      line(i, 150 + anthem.right.get(i) * 50, i + 1, 150 + anthem.right.get(i + 1) * 50);
    }
  }
}

public void drawSignal() {
  rgbSignal = mapper.pluckSamplesAsRGB(audioSignal, 0, mapSize); // rgbSignal is now an array of rgb gray
  mapImage.loadPixels();
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, rgbSignal.length, chan);
  mapImage.updatePixels();
}  
