/**
 * Demo of how to set and save WaveSynth parameters from JSON files.
 * JSON files are stored in this sketches "data" folder.
 */

import java.io.File;
import java.util.ArrayList;
import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;
HilbertGen hGen;
MooreGen mGen;
DiagonalZigzagGen zGen;
PixelMapGen gen;
PixelAudioMapper mapper;
ArrayList<WaveData> wdList;
WaveSynth wavesynth;
/*
 * display dimensions are used for PixelMapGen instances, PixelAudioMapper, and
 * WaveSynth. For HilbertGen and MooreGen, dimensions must be equal powers of 2.
 */
int imageWidth = 1024;
int imageHeight = 1024;  
// the PImage used by our WaveSynth 
PImage synthImage;
// WaveSynth variables
float myGamma = 1.0f;        // a non-linear brightness adjustment
int animSteps = 240;         // number of steps in the animation
int animStop = animSteps;    // The step at which the animation should stop (not used here)
int step = 0;                // the current step in the animation
String comments;             // a JSON field that provides information about the WaveSynth effects it produces
boolean isAnimating = true;
boolean oldIsAnimating;
boolean isLooping = true;
// file i/o
String jsonFolder = "/JSON_data/";
File currentDataFile;
String currentFileName;
JSONObject json;

public void settings() {
  size(imageWidth, imageHeight, JAVA2D);
}

public void setup() {
  println("--->> data path: "+ dataPath(""));
  pixelaudio = new PixelAudio(this);
  hGen = new HilbertGen(1024, 1024);
  mGen = new MooreGen(1024, 1024);
  zGen = new DiagonalZigzagGen(1024, 1024, AffineTransformType.FLIPY);
  gen = hGen;
  mapper = new PixelAudioMapper(gen);
  wdList = initWaveDataList();
  wavesynth = new WaveSynth(mapper, wdList);
  initWaveSynth(wavesynth);
  synthImage = wavesynth.mapImage;  // point synthImage to wavesynth's calculated image
  showHelp();
}

public ArrayList<WaveData> initWaveDataList() {
  ArrayList<WaveData> list = new ArrayList<WaveData>();
  float frequency = 768.0f;        // frequency of the sine wave
  float amplitude = 0.8f;          // amplitude of the sine wave
  float phase = 0.0f;              // phase of the sine wave: the animation works by incrementing phase
  float dc = 0.0f;                 // dc component of signal, increase or decrease brightness
  float cycles = 1.0f;             // number of times to cycle through phase (from 0 to TWO_PI)
  int waveColor = color(159, 190, 251);  // the color controlled by this WaveData instance
  int steps = this.animSteps;            // number of steps in animation (application ignores this value)
  // now create a WaveData instance and add it to the list
  WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  // lest's create another WaveData object
  frequency = 192.0f;              // 1/4 of previous frequency
  phase = 0.0f;                    // same phase as previous
  cycles = 2.0f;                   // cycling twice as fast the previous one
  waveColor = color(209, 178, 117);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  return list;
}

public WaveSynth initWaveSynth(WaveSynth synth) {
  synth.setGain(0.8f);            // overall brightness of the image
  synth.setGamma(myGamma);        // non-linear brightness adjustment
  synth.setScaleHisto(false);     // do a linear stretch of brightness if true
  synth.setAnimSteps(this.animSteps);    // number of steps in the animation (used by application)
  println("--- mapImage size = " + synth.mapImage.pixels.length);
  synth.prepareAnimation();       // tell the synth to load some internal variables
  synth.renderFrame(0);           // render frame 0
  return synth;
}

public void swapGen(PixelMapGen gen) {
  mapper.setGenerator(gen);
}

public void draw() {
  image(synthImage, 0, 0);
  stepAnimation();
}

public void stepAnimation() {
  if (!isAnimating) return;
  step += 1;
  if (step >= animSteps) {
    println("--- Completed "+ animSteps +" frames of animation.");
    if (!isLooping) {
      isAnimating = false;
    } else {
      step = 0;
    }
  }
  // step through the wavesynth animation rendering
  wavesynth.renderFrame(step);
}

public void keyPressed() {
  switch (key) {
  case ' ':
    isAnimating = !isAnimating;
    println (isAnimating ? "Starting animation at frame "+ step +" of "+ animSteps
      : "Stopping animation at frame "+ step +" of "+ animSteps);
    break;
  case 'o':
      // turn off animation while reading new settings for wavesynth
      oldIsAnimating = isAnimating;
      isAnimating = false;
      this.loadWaveData();
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
  case 'g':
    // change the gen
    if (gen == zGen) {
      gen = hGen;
      swapGen(gen);
      break;
    }
    if (gen == hGen) {
      gen = mGen;
      swapGen(gen);
      break;
    }
    if (gen == mGen) {
      gen = zGen;
      swapGen(gen);
    }
    break;
  case 'f':
    // rotate gen 90 degrees clockwise
    gen.setTransformType(AffineTransformType.ROT90);
    swapGen(gen);
    break;
  case 'r':
  case 'R':
    step = 0;
    break;
  case 'h':
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println("press SPACEBAR to start or stop animation.");
  println("press 'o' to open a JSON file.");
  println("press 'O' to reload the current JSON file.");
  println("press 'r' or 'R' to reset animation to step 0");
  println("press 'h' to show Help.");
}
