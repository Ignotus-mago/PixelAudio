/**
 *
 * INSTRUCTIONS FOR PERFORMANCE
 *
 * Launch and let the raindrops play.
 * The tempo for performance is quarter note = 88 bpm, flexible for humans, inflexible for computer.
 * Percussion cues computer instrumentalist -- type '\' to start music, speak words. (Engineer: mic up)
 * At end, escape key closes app and WorkFlowPerformance is uncovered.
 *
 * WaveSynthSequencer builds on BigWaveSynth, which shows how you can load
 * WaveSynth pixel data into the pixel array of a MultiGen. WaveSynthSequencer
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
 * Press ' ' to start and stop WaveSynth animation.
 * Press 'o' to open a JSON WaveSynth configuration file.
 * Press 'O' to reload the most recent JSON WaveSynth configuration file.
 * Press 'j' or 'J' to save current WaveSynth configuration to a JSON file.
 * Press 's' to save the display to an image file named wavesynth_<wsIndex>.png.
 * Press 'S' to save WaveSynth audio to an audio file named wavesynth_<wsIndex>.wave.
 * Press 'f' to display the current frameRate.
 * Press 'w' to step the WaveSynth Sequencer without playing a musical note.
 * Press 'W' to step the WaveSynth Sequencer and play the corresponding a musical note.
 * Press 'd' to turn raindrops on or off (raindrops trigger audio events).
 * Press '\' to run the WaveSynth Sequencer
 * Press 'c' to reset the image and sound to the opening state.
 * Press 'h' to show this help message in the console.
 *
 * See also: BigWaveSynth, BigWaveSynthAudio, WaveSynthEditor.
 *
 */

import java.awt.Color;
import java.io.*;
import java.util.ArrayList;
import javax.sound.sampled.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import ddf.minim.*;
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.sampler.*;

PixelAudio pixelaudio;      // our shiny new library
HilbertGen hGen;            // a PixelMapGen to draw Hilbert curves
MultiGen multigen;          // a PixelMapGen that handles multiple gens
int rows = 3;               // rows
int columns = 2;            // columns
int genWidth = 512;         // width of PixelMapGen objects, for hGen must be a power of 2
int genHeight = 512;        // height of PixelMapGen objects, for hGen must be equal to width
PixelAudioMapper mapper;    // instance of a class for reading, writing, and transcoding audio and image data
int mapSize;          // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
ArrayList<WaveData> wdList; // a list of audio "operators" that drive a WaveSynth
WaveSynth wavesynth;        // a sort of additive audio synthesis color organ that also produces sound
PImage synthImage;          // local variable that points to wavesynth's PImage instance variable

// WaveSynth variables
float myGamma = 1.0f;       // a non-linear brightness adjustment
int animSteps = 240;        // number of steps in the animation
int animStop = animSteps;   // The step at which the animation should stop (not used here)
int step = 0;               // the current step in the animation
String comments;            // a JSON field that provides information about the WaveSynth effects it produces

/** Minim audio library */
Minim minim;                // library that handles audio
AudioOutput audioOut;       // line out to sound hardware
MultiChannelBuffer audioBuffer;  // data structure to hold audio samples
boolean isBufferStale = true;    // flags that audioBuffer needs to be reset: i.e., after loading JSON data to wavesynth
int sampleRate = 48000;     // a critical value for display and audio, see the setup method
float[] audioSignal;        // the audio signal as an array of floats
int[] rgbSignal;            // the colors in the display image, in the order the signal path visits them
int audioLength;            // length of the audioSignal, same as the number of pixels in the display image

// SampleInstrument setup
int noteDuration = 1000;    // average sample synth note duration, milliseconds
int samplelen;              // calculated sample synth note length, samples
PASamplerInstrumentPool pool;   // pool of PASamplerInstruments to play our music

// ADSR and params
ADSRParams adsr;            // good old attack, decay, sustain, release
float maxAmplitude = 0.7f;
float attackTime = 0.8f;
float decayTime = 0.5f;
float sustainLevel = 0.125f;
float releaseTime = 0.5f;

// file i/o from JSON
String jsonFolder = "/JSON_data/";
File currentDataFile;
String currentFileName;
JSONObject json;

// animation
boolean isWaveSynthAnimating = true;    // animation status
boolean oldIsAnimating;      // keep old animation status if we suspend animation
boolean isLooping = true;    // looping sample (our instrument ignores this)
boolean isRaining = false;
int circleColor = color(233, 220, 199, 128);

// interaction
int sampleX;
int sampleY;
int samplePos;    // position of a mouse click along the signal path, index into the audio array
ArrayList<TimedLocation> timeLocsArray;
int count = 0;

// number formatting, useful for output to console
DecimalFormat twoPlaces;
DecimalFormat noPlaces;
DecimalFormat fourFrontPlaces;
DecimalFormat commaTwoPlaces;
DecimalFormat eightPlaces;


public void settings() {
  size(rows * genWidth, columns * genHeight);
}

public void setup() {
  frameRate(30);
  pixelaudio = new PixelAudio(this);
  minim = new Minim(this);
  multigen = loadLoopGen(genWidth, genHeight);      // See the MultiGenDemo example for details on how MultiGen works
  mapper = new PixelAudioMapper(multigen);  // initialize a PixelAudioMapper
  float drone = sampleRate/1024.0f;      // a frequency that generates visual symmetries
  wdList = buildWaveDataList(drone, 8, 10);  // generate a WaveData list for the WaveSynth
  wavesynth = new WaveSynth(mapper, wdList);  // initialize a WaveSynth object
  initWaveSynth(wavesynth);          // fill in some parameters of the WaveSynth
  synthImage = wavesynth.mapImage;      // point synthImage at the WaveSynth's PImage field
  mapSize = mapper.getSize();          // size of the image, and of various other entities
  initAudio();                // set up audio output and an audio buffer
  timeLocsArray = new ArrayList<TimedLocation>();   // initialize mouse event tracking array
  initDecimalFormats();            // initializes some utility functions for formatting numbers
  initWaveSynthList();            // sets up a sequencer using dbwfMusic, dbwfTimes, and dbwfAmps arrays
  showHelp();
}

/**
 * turn off audio processing when we exit
 */
public void stop() {
  if (pool != null) pool.close();
  if (minim != null) minim.stop();
  super.stop();
}

/**
 * Adds PixelMapGen objects to the local variable genList, puts the coords
 * of their upper left corner in offsetList. The two lists are used to
 * initialize a MultiGen, which can be used to map audio and pixel data.
 * This method provides a big looping fractal consisting of 6 Hilbert curves.
 * This method was an early effort at creating continuous paths over multiple
 * PixelMapGens. It is now available as a static method in the PixelAudio library,
 * HilbertGen. hilbertLoop3x2(int genW, int genH).
 */
public MultiGen loadLoopGen(int loopGenW, int loopGenH) {
  // list of PixelMapGens that create an image using mapper
  ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
  // list of x,y coordinates for placing gens from genList
  ArrayList<int[]> offsetList = new ArrayList<int[]>();
  genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.FX270));
  offsetList.add(new int[] { 0, 0 });
  genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.NADA));
  offsetList.add(new int[] { loopGenW, 0 });
  genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.FX90));
  offsetList.add(new int[] { 2 * loopGenW, 0 });
  genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.FX90));
  offsetList.add(new int[] { 2 * loopGenW, loopGenH });
  genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.R180));
  offsetList.add(new int[] { loopGenW, loopGenH });
  genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.FX270));
  offsetList.add(new int[] { 0, loopGenH });
  MultiGen gen = new MultiGen(width, height, offsetList, genList);
  return gen;
}

/**
 * Initializes number formatters, handy for display. Processing's nf() method will also do.
 */
public void initDecimalFormats() {
  // DecimalFormat sets formatting conventions from the local system, unless we tell it otherwise.
  // make sure we use "." for decimal separator, as in US, not a comma, as in many other countries
  Locale loc = Locale.US;
  DecimalFormatSymbols dfSymbols = new DecimalFormatSymbols(loc);
  dfSymbols.setDecimalSeparator('.');
  twoPlaces = new DecimalFormat("0.00", dfSymbols);
  noPlaces = new DecimalFormat("00", dfSymbols);
  fourFrontPlaces = new DecimalFormat("0000", dfSymbols);
  eightPlaces = new DecimalFormat("0.00000000", dfSymbols);
  dfSymbols.setDecimalSeparator(',');
  commaTwoPlaces = new DecimalFormat("0.00", dfSymbols);
}


/**
 * The main loop for drawing to the screen.
 */
public void draw() {
  image(synthImage, 0, 0, width, height);    // draw the synth image
  if (isWaveSynthAnimating) stepAnimation();  // animate the image if requested
  runTimeArray();                // animate audio event markers
  if (isPlayIntro && introMusic.size() > 0) {
    runMusicArray();            // play the WaveSynth Sequencer
  }
  if (isRaining) {
    // animation slows the frame rate, so we change the threshold when animating
    float thresh = (isWaveSynthAnimating) ? 0.25f : 0.05f;
    if (random(0, 1) < thresh) {
      raindrops();              // trigger random audio events
    }
  }
}

/**
 * Animates the WaveSynth referenced by variable wavesynth.
 * The animation is controlled by the WaveSynth phase and cycle attributes.
 */
public void stepAnimation() {
  step += 1;
  step %= animSteps;
  wavesynth.renderFrame(step);
}

public void keyPressed() {
  switch (key) {
  case ' ':
    isWaveSynthAnimating = !isWaveSynthAnimating;
    println(isWaveSynthAnimating ? "Starting animation at frame " + step + " of " + animSteps
      : "Stopping animation at frame " + step + " of " + animSteps);
    break;
  case 'o':
    // turn off animation while reading new settings for wavesynth
    oldIsAnimating = isWaveSynthAnimating;
    isWaveSynthAnimating = false;
    this.loadWaveData();
    isBufferStale = true;
    break;
  case 'O':
    if (currentDataFile == null) {
      loadWaveData();
    } else {
      fileSelectedOpen(currentDataFile);
      renderSignal();
      println("-------->>>>> Reloaded file");
    }
    break;
  case 'j':
  case 'J':
    saveWaveData();
    break;
  case 's':
    synthImage.save("wavesynth_"+ wsIndex +".png");
    break;
  case 'S':
    saveToAudio();
    break;
  case 'f':
    println("--->> frame rate: "+ frameRate);
    break;
  case 'w':
    stepWaveSynth();
    break;
  case 'W':
    stepWaveSynth();
    playSample(mapper.lookupSample(width/2, height/2), calcSampleLen(), 0.3f, new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime));
    break;
  case 'd':
    isRaining = !isRaining;
    println("----->>> isRaining ", isRaining);
    break;
  case '\\':
    isRaining = false;
    loadMusic();
    isPlayIntro = true;
    break;
  case 'c':
    wavesynth = new WaveSynth(mapper, wdList);
    initWaveSynth(wavesynth);
    wavesynth.prepareAnimation();
    wavesynth.renderFrame(0);
    this.synthImage = wavesynth.mapImage;
    checkBufferState(true);
    isWaveSynthAnimating = true;
    break;
  case 'h':
    showHelp();
    break;
  default:
    break;
  }
}


/**
 * Advance the WaveSynth Sequencer to its next state.
 */
public void stepWaveSynth() {
  wavesynth = this.waveSynthList.get(wsIndex);
  wavesynth.prepareAnimation();
  wavesynth.renderFrame(0);
  synthImage = wavesynth.mapImage;  // point synthImage at the WaveSynth's PImage field
  wsIndex++;
  if (wsIndex > waveSynthList.size() - 1) wsIndex = 0;
  checkBufferState(true);
}

public void showHelp() {
  println(" * Press ' ' to start and stop WaveSynth animation.");
  println(" * Press 'o' to open a JSON WaveSynth configuration file.");
  println(" * Press 'O' to reload the most recent JSON WaveSynth configuration file.");
  println(" * Press 'j' or 'J' to save current WaveSynth configuration to a JSON file.");
  println(" * Press 's' to save the display to an image file named wavesynth_<wsIndex>.png.");
  println(" * Press 'S' to save WaveSynth audio to an audio file named wavesynth_<wsIndex>.wav.");
  println(" * Press 'f' to display the current frameRate. ");
  println(" * Press 'w' to step the WaveSynth Sequencer without playing a musical note.");
  println(" * Press 'W' to step the WaveSynth Sequencer and play the corresponding a musical note.");
  println(" * Press 'd' to turn raindrops on or off (raindrops trigger audio events). ");
  println(" * Press '\\' to run the WaveSynth Sequencer ");
  println(" * Press 'c' to reset the image and sound to the opening state. ");
  println(" * Press 'h' to show this help message in the console. ");
}

/**
 * Save audio buffer to a file called "wavesynth_<wsIndex>.wav".
 */
public void saveToAudio() {
  renderSignal();
  try {
    saveAudioToFile(audioSignal, sampleRate, "wavesynth_"+ wsIndex +".wav");
  }
  catch (IOException e) {
    println("--->> There was an error outputting the audio file wavesynth.wav "+ e.getMessage());
  }
  catch (UnsupportedAudioFileException e) {
    println("--->> The file format is unsupported "+ e.getMessage());
  }
}

public void mousePressed() {
  audioMousePressed(constrain(mouseX, 0, width - 1), constrain(mouseY, 0, height - 1));
}
