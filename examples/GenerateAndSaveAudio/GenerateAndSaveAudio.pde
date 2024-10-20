/**
 * GenerateAndSaveAudio builds on BigWaveSynth, which shows how you can load
 * WaveSynth pixel data into the pixel array of a MultiGen. GenerateAndSaveAudio 
 * adds some useful features: 1. you can play the image as audio, and
 * 2. you can save the audio to a WAVE file.
 * 
 * This example also allows you to load JSON files to reconfigure the WaveSynth. 
 * Initially, we call buildWaveDataList() to create a WaveData array with eight operators. 
 * This is passed to a WaveSynth which is further configured by initWaveSynth(). 
 * To load JSON data, press the 'o' key and go to the data folder of this sketch.
 * 
 * Note that the sampleRate value influences the appearance of the image, the duration
 * of audio samples and the way data is written to an audio file. See the comments
 * in setup() for details. 
 * 
 * 
 * Press the spacebar to start and stop animation. 
 * Press 'o' key to open a JSON file
 * Press 'O' to reload the file, for example after changing it in a text editor. 
 * Press the 'j' or 'J' key to save the current configuration to a JSON file. 
 * Press 's' to save wavesynth audio to a .wav audio file.
 * Press 'i' to save the display immage as "wavesynth.png" to the sketch folder.
 * Press 'f' to check the frame rate.
 * Press 't' to trigger the audio (for testing some audio weirdness).
 * Press 'h' to print key commands to the console.
 * 
 */

import java.io.*;
import javax.sound.sampled.*;
import java.util.Timer;
import java.util.TimerTask;


import ddf.minim.*;
import ddf.minim.ugens.*;
import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;        // our shiny new library
HilbertGen hGen;              // a PixelMapGen to draw Hilbert curves
MultiGen multigen;            // a PixelMapGen that handles multiple gens
ArrayList<PixelMapGen> genList;  // list of PixelMapGens that create an image using mapper
ArrayList<int[]> offsetList;  // list of x,y coordinates for placing gens from genList
int rows = 3;
int columns = 2;
int genWidth = 512;           // width of PixelMapGen objects, for hGen must be a power of 2
int genHeight = 512;          // height of PixelMapGen objects, for hGen must be equal to width
PixelAudioMapper mapper;      // instance of a class for reading, writing, and transcoding audio and image data
int mapSize;          // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
ArrayList<WaveData> wdList;   // a list of audio "operators" that drive a WaveSynth
WaveSynth wavesynth;          // a sort of additive audio synthesis color organ that also produces sound
PImage synthImage;            // local variable that points to wavesynth's PImage instance variable
// WaveSynth variables
float myGamma = 1.0f;         // a non-linear brightness adjustment
int animSteps = 240;          // number of steps in the animation
int animStop = animSteps;     // The step at which the animation should stop (not used here)
int step = 0;                 // the current step in the animation
String comments;              // a JSON field that provides information about the WaveSynth effects it produces

/** Minim audio library */
Minim minim;                  // library that handles audio
AudioOutput audioOut;         // line out to sound hardware
MultiChannelBuffer audioBuffer;  // data structure to hold audio samples
boolean isBufferStale = false;  // after loading JSON data to wavesynth, we need to reset the audio buffer
int sampleRate = 48000;       // a critical value, see the setup method
float[] audioSignal;          // the audio signal as an array
int[] rgbSignal;              // the colors in the display image, in the order the signal path visits them
int audioLength;
// SampleInstrument setup
float sampleScale = 4;
int sampleBase = sampleRate/4;
int samplelen = (int) (sampleScale * sampleBase);
Sampler audioSampler;         // minim class for sampled sound
SamplerInstrument instrument; // local class to wrap audioSampler
// ADSR and params
ADSR adsr;                    // good old attack, decay, sustain, release
float maxAmplitude = 0.9f;
float attackTime = 0.2f;
float decayTime = 0.125f;
float sustainLevel = 0.5f;
float releaseTime = 0.2f;

// file i/o from JSON
String jsonFolder = "/JSON_data/";
File currentDataFile;
String currentFileName;
JSONObject json;
// animation
boolean isAnimating = true;   // animation status
boolean oldIsAnimating;       // keep old animation status if we suspend animation
boolean isLooping = true;     // looping sample (our instrument ignores this
// interaction
int sampleX;
int sampleY;
int samplePos;                // position of a mouse click along the signal path, index into the audio array
ArrayList<TimedLocation> timeLocsArray;
int count = 0;

boolean isRaining = false;
boolean oldIsRaining = false;

public void settings() {
  size(rows * genWidth, columns * genHeight);
}

public void setup() {
  frameRate(30);
  pixelaudio = new PixelAudio(this);
  minim = new Minim(this);
  // sampleRate affects image display and audio sample calculation.
  // For compatibility with other applications, including Processing, it's a good
  // idea to use a standard sampling rate, like 44100. However, you can experiment
  // with other sampling rates and probably can play audio and and save files.
  // Their behavior in Processing when you try to open them can be unpredictable.
  //
  // Setting sampleRate = genWidth * genHeight provides interesting symmetries in the image
  // and will play audio and save to file -- but it's not a standard sample rate and
  // though Processing may open files saved with non-standard sampling rates, it
  // usually shifts the frequency according the sampleRate you have set.
  sampleRate = 48000;             // = genWidth * genHeight; // another value for sampleRate
  sampleBase = sampleRate / 4;        // a quarter of a second
  initAudio();                // set up audio output and an audio buffer
  genList = new ArrayList<PixelMapGen>();     // prepare data for the MultiGen
  offsetList = new ArrayList<int[]>();
  initMultiGenLists();
  // See the MultiGenDemo example for details on how MultiGen works
  multigen = new MultiGen(rows * genWidth, columns * genHeight, offsetList, genList);
  mapper = new PixelAudioMapper(multigen);  // initialize a PixelAudioMapper
  wdList = buildWaveDataList();        // generate a WaveData list for the WaveSynth
  wavesynth = new WaveSynth(mapper, wdList);  // initialize a WaveSynth object
  initWaveSynth(wavesynth);          // fill in some parameters of the WaveSynth
  synthImage = wavesynth.mapImage;      // point synthImage at the WaveSynth's PImage field
  mapSize = mapper.getSize();          // size of the image, and of various other entities
  timeLocsArray = new ArrayList<TimedLocation>();     // initialize mouse event tracking array
  showHelp();
}

public void initAudio() {
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  this.audioBuffer = new MultiChannelBuffer(1024, 1);
}

/**
 * Adds PixelMapGen objects to the genList. The genList will be used to initialize a
 * MultiGen, which in turn is passed to a WaveSynth. The different AffineTransformType values
 * are used to align the Hilbert curves in a continuous path.
 */
public void initMultiGenLists() {
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FLIPX90));
  offsetList.add(new int[] { 0, 0 });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.NADA));
  offsetList.add(new int[] { genWidth, 0 });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FLIPX90CCW));
  offsetList.add(new int[] { 2 * genWidth, 0 });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FLIPX90CCW));
  offsetList.add(new int[] { 2 * genWidth, genHeight });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.ROT180));
  offsetList.add(new int[] { genWidth, genHeight });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FLIPX90));
  offsetList.add(new int[] { 0, genHeight });
}

/**
 * Generates an ArrayList of WaveDate objects to be used by a WaveSynth to generate
 * RGB pixel values and (on request) audio signal values.
 *
 * @return an ArrayList of WaveData objects
 */
public ArrayList<WaveData> buildWaveDataList() {
  ArrayList<WaveData> list = new ArrayList<WaveData>();
  // funda is the fundamental of a musical tone that is somewhat like a trumpet
  // in its frequency spectrum. Vary it to see how the image and sound change.
  float funda = 46.875f;
  float frequency = funda;
  float amplitude = 0.55f;
  float phase = 0.766f;
  float dc = 0.0f;
  float cycles = -8.0f;
  int waveColor = color(0, 89, 233);
  int steps = this.animSteps;
  WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  frequency = 2 * funda;
  amplitude = 0.52f;
  phase = -0.89f;
  cycles = 8.0f;
  waveColor = color(89, 199, 55);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  frequency = 3 * funda;
  amplitude = 0.6f;
  phase = -0.486f;
  cycles = 3.0f;
  waveColor = color(254, 89, 110);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  frequency = 4 * funda;
  amplitude = 0.45f;
  phase = -0.18616974f;
  cycles = -2.0f;
  waveColor = color(89, 110, 233);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  frequency = 5 * funda;
  amplitude = 0.42f;
  phase = 0.6846085f;
  cycles = -5.0f;
  waveColor = color(233, 34, 21);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  frequency = 6 * funda;
  amplitude = 0.45f;
  phase = 0.68912f;
  cycles = 13.0f;
  waveColor = color(220, 199, 55);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  frequency = 7 * funda;
  amplitude = 0.25f;
  phase = 0.68f;
  cycles = 11.0f;
  waveColor = color(159, 190, 255);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  frequency = 8 * funda;
  amplitude = 0.32f;
  phase = 0.68f;
  cycles = -11.0f;
  waveColor = color(209, 178, 117);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  return list;
}

/**
 * Sets instance variables for a supplied WaveSynth.
 * @param synth
 * @return
 */
public WaveSynth initWaveSynth(WaveSynth synth) {
  synth.setGain(0.44f);
  synth.setGamma(myGamma);
  synth.setScaleHisto(false);
  synth.setAnimSteps(this.animSteps);
  synth.setSampleRate(sampleRate);
  // synth.setNoiseiness(0.5f);
  println("--- mapImage size = " + synth.mapImage.pixels.length);
  synth.prepareAnimation();
  synth.renderFrame(0);
  return synth;
}

public void draw() {
  image(synthImage, 0, 0, width, height);
  if (isAnimating) stepAnimation();
  runTimeArray();
  if (isRaining) {
    float thresh = (isAnimating) ? 0.5 : 0.1;
    if (random(0,1) < thresh) {
      raindrops();
    }
  }
}

public void stepAnimation() {
  step += 1;
  step %= animSteps;
  wavesynth.renderFrame(step);
}

public void runTimeArray() {
  int currentTime = millis();
  timeLocsArray.forEach(tl -> {
    tl.setStale(tl.stopTime() < currentTime);
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
  case ' ':
    isAnimating = !isAnimating;
    println(isAnimating ? "Starting animation at frame " + step + " of " + animSteps
      : "Stopping animation at frame " + step + " of " + animSteps);
    break;
  case 'o':
    // turn off animation while reading new settings for wavesynth
    oldIsAnimating = isAnimating;
    isAnimating = false;
    oldIsRaining = isRaining;
    isRaining = false;
    this.loadWaveData();
    isBufferStale = true;
    break;
  case 'O':
    if (currentDataFile == null) {
      loadWaveData();
    } else {
      fileSelectedOpen(currentDataFile);
      println("-------->>>>> Reloaded file");
    }
    break;
  case 'j':
  case 'J':
    saveWaveData();
    break;
  case 's':
    saveToAudio();
    break;
  case 'i':
    synthImage.save("wavesynth.png");
    break;
  case 'f':
    println("--->> frame rate: "+ frameRate);
    break;
  case 't':
    // there are some weird things going on with repeated triggers of the instrument
    if (instrument != null) {
      instrument.play(samplelen / (float) (sampleRate));
    }
    break;
  case 'r':
    isRaining = !isRaining;
    break;
  case 'h':
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println("  * Press the spacebar to start and stop animation. ");
  println("  * Press 'o' key to open a JSON file.");
  println("  * Press 'O' to reload the file, for example after changing it in a text editor. ");
  println("  * Press the 'j' or 'J' key to save the current configuration to a JSON file. ");
  println("  * Press 's' to save wavesynth audio to a .wav audio file.");
  println("  * Press 'i' to save the display immage as \"wavesynth.png\" to the sketch folder.");
  println("  * Press 'f' to check the frame rate.");
  println("  * Press 't' to trigger the audio (for testing some audio weirdness).");
  println("  * Press 'r' to trigger raindrops.");
  println("  * Press 'h' to print key commands to the console.");
}

/**
 * Calls WaveSynth to render a audio sample array derived from the same math that creates the image.
 */
public void renderSignal() {
  this.audioSignal = wavesynth.renderAudioRaw(step);      // get the signal "as is" from WaveSynth
  audioSignal = WaveSynth.normalize(audioSignal, 0.9f);    // normalize samples to the range (-0.9f, 0.9f)
  audioLength = audioSignal.length;
  audioBuffer.setBufferSize(audioLength);
  audioBuffer.setChannel(0, audioSignal);            // copy audioSignal to channel 0 of audioBuffer
  println("--->> copied audio signal to audio buffer");
}

public void mousePressed() {
  sampleX = mouseX;
  sampleY = mouseY;
  samplePos = mapper.lookupSample(sampleX, sampleY);
  if (audioSignal == null || isBufferStale) {
    renderSignal();
    isBufferStale = false;
  }
  playSample(samplePos);
}

public int playSample(int samplePos) {
  audioSampler = new Sampler(audioBuffer, sampleRate, 8); // create a Minim Sampler from the buffer sampleRate sampling
  // rate, for up to 8 simultaneous outputs
  audioSampler.amplitude.setLastValue(0.9f);   // set amplitude for the Sampler
  audioSampler.begin.setLastValue(samplePos); // set the Sampler to begin playback at samplePos, which corresponds
  // to the place the mouse was clicked
  int releaseDuration = (int) (releaseTime * sampleRate); // do some calculation to include the release time.
  // There may be better ways to do this.
  float vary = (float) (PixelAudio.gauss(this.sampleScale, this.sampleScale * 0.125f)); // vary the duration of the signal
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
  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
  adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  this.instrument = new SamplerInstrument(audioSampler, adsr);
  // play command takes a duration in seconds
  float duration = samplelen / (float) (sampleRate);
  instrument.play(duration);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, (int) (duration * 1000) + millis()));
  // return the length of the sample
  return samplelen;
}

public void raindrops() {
  int signalPos = (int) random(samplelen, mapSize - samplelen - 1);
  int[] coords = mapper.lookupCoordinate(signalPos);
  sampleX = coords[0];
  sampleY = coords[1];
  if (audioSignal == null || isBufferStale) {
    renderSignal();
    isBufferStale = false;
  }
  playSample(samplePos);  
}
