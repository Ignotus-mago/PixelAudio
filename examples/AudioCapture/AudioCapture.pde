import java.util.Arrays;

import net.paulhertz.pixelaudio.*;
import ddf.minim.*;
import ddf.minim.ugens.*;

/* PixelAudio library */
public PixelAudio pixelaudio;
HilbertGen hGen;
PixelAudioMapper mapper;
PixelAudioMapper.ChannelNames chan;

/* image for display, audio signal, RGB array */
PImage mapImage;
int mapSize;
int[] colors;
float[] audioSignal;
int audioLength;
int[] rgbSignal;

/* Minim audio library */
Minim minim;
AudioInput audioIn;
AudioOutput audioOut;
MultiChannelBuffer audioBuffer;
Sampler audioSampler;
SamplerInstrument instrument;
AudioPlayer anthem;
StreamCapture streamCap;
float sampleScale = 4;
int sampleBase = 10250;
int sampleLength = (int) (sampleScale * sampleBase);

/* ADSR and parameters */
ADSR adsr;
float maxAmplitude = 0.9f;
float attackTime = 0.2f;
float decayTime = 0.125f;
float sustainLevel = 0.5f;
float releaseTime = 0.2f;

/* audio variables */
int sampleRate = 44100;
boolean listening = false;
boolean listenLive = true;

/* interaction */
int pixelPos;
int samplePos;

public void settings() {
  size(512, 512);
}

public void setup() {
  initMapper();
  mapSize = mapper.getSize();
  initAudio();
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
  pixelaudio = new PixelAudio(this);
  hGen = new HilbertGen(width, height);
  mapper = new PixelAudioMapper(hGen);
  chan = PixelAudioMapper.ChannelNames.L;
  mapImage = createImage(width, height, RGB);
  mapImage.loadPixels();
  mapper.plantPixels(getColors(), mapImage.pixels, 0, mapper.getSize());
  mapImage.updatePixels();
}

public void initAudio() {
  this.minim = new Minim(this);
  this.audioIn = minim.getLineIn(Minim.MONO);
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  this.audioBuffer = new MultiChannelBuffer(mapSize, 1);
  this.audioSignal = new float[mapSize];
  this.rgbSignal = new int[mapSize];
  Arrays.fill(audioSignal, 0.0f);
  this.audioLength = audioSignal.length;
  this.audioBuffer.setChannel(0, audioSignal);
  streamCap = new StreamCapture();
  // String path = "/Users/paulhz/Documents/Processing/libraries/PixelAudio/examples/LoadAudioToImage/data";
  String path = this.dataPath("");
  anthem = minim.loadFile(path +"/youthorchestra.wav");
  anthem.loop();
  anthem.pause();
  //createSampler();
}

public void createSampler() {
  // create a Minim Sampler from the buffer with 44.1 sampling
  audioBuffer.setChannel(0, audioSignal);
  audioSampler = new Sampler(audioBuffer, sampleRate, 8); 
  audioSampler.amplitude.setLastValue(0.9f); 
  audioSampler.begin.setLastValue(0);
  adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
}

public void draw() {
  image(mapImage, 0, 0);
  textSize(18);
  text( "Listening is currently " + listening + ".", 6, 18 );
  if (listening) drawSignal();
  drawInput();
}

public void keyPressed() {
  switch (key) {
  case 'm':
  case'M':
    if (audioIn.isMonitoring()) audioIn.disableMonitoring();
    else audioIn.enableMonitoring();
    break;
  case ' ':
    toggleListening();
    break;
  case 'p':
  case 'P':
    // change the audio source, but only if we are not listening
    if (!listening) {
      listenLive = !listenLive;
      println("-- listenLive = "+ listenLive);
      if (listenLive) anthem.pause();
      else anthem.play();
    }
    break;
  case 'h':
    break;
  default:
    break;
  }
}

public void toggleListening() {
    if (listening) {
      if (listenLive) audioIn.removeListener(streamCap);
      else anthem.removeListener(streamCap);
      listening = false;
    } 
    else {
      if (listenLive) audioIn.addListener(streamCap);
      else anthem.addListener(streamCap);
      listening = true;
    }
}

public void mousePressed() {
  if (listening) {
    toggleListening();
    if (anthem.isPlaying()) anthem.pause(); 
  }
  pixelPos = mouseX + mouseY * width;
  samplePos = mapper.lookupSample(mouseX, mouseY);
  playSample(samplePos);
}

public int playSample(int samplePos) {
  audioSampler = new Sampler(audioBuffer, sampleRate, 8); 
  // ADSR 
  adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  // set amplitude for the Sampler
  audioSampler.amplitude.setLastValue(0.9f); 
  // set the Sampler to begin playback at samplePos, which corresponds to the place the mouse was clicked
  audioSampler.begin.setLastValue(samplePos); 
  int releaseDuration = (int) (releaseTime * sampleRate); // do some calculation to include the release time.
  int duration = sampleLength;
  if (samplePos + sampleLength >= mapSize) {
    duration = mapSize - samplePos; // make sure we don't exceed the mapSize
    // println("----->>> duraton = " + duration);
  }
  int durationPlusRelease = duration + releaseDuration;
  int end = (samplePos + durationPlusRelease >= this.mapSize) 
            ? this.mapSize - 1 : samplePos + durationPlusRelease;
  // println("----->>> end = " + end);
  audioSampler.end.setLastValue(end);
  // println("----->>> audioBuffer size = "+ audioBuffer.getBufferSize());
  this.instrument = new SamplerInstrument(audioSampler, adsr);
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
  rgbSignal = mapper.pluckSamplesAsRGB(audioSignal, 0, mapSize);  // rgbSignal is now an array of rgb gray
  mapImage.loadPixels();
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, rgbSignal.length, chan);
  mapImage.updatePixels();
}
