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
PImage synthImage;
// WaveSynth variables
float myGamma = 1.0f;
int animSteps = 240;
int animStop = animSteps;
int step = 0;
String comments;
boolean isAnimating = true;
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
  pixelaudio = new PixelAudio(this);
  hGen = new HilbertGen(1024, 1024);
  mGen = new MooreGen(1024, 1024);
  zGen = new DiagonalZigzagGen(1024, 1024, AffineTransformType.FLIPY);
  gen = hGen;
  mapper = new PixelAudioMapper(gen);
  wdList = initWaveDataList();
  wavesynth = new WaveSynth(mapper, wdList);
  initWaveSynth(wavesynth);
  synthImage = wavesynth.mapImage;
  showHelp();
}

public ArrayList<WaveData> initWaveDataList() {
  ArrayList<WaveData> list = new ArrayList<WaveData>();
  float frequency = 768.0f;
  float amplitude = 0.8f;
  float phase = 0.0f;
  float dc = 0.0f;
  float cycles = 1.0f;
  int waveColor = color(159, 190, 251);
  int steps = this.animSteps;
  WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  frequency = 192.0f;
  phase = 0.0f;
  cycles = 2.0f;
  waveColor = color(209, 178, 117);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  return list;
}

public WaveSynth initWaveSynth(WaveSynth synth) {
  synth.setGain(0.8f);
  synth.setGamma(myGamma);
  synth.setScaleHisto(false);
  synth.setAnimSteps(this.animSteps);
  println("--- mapImage size = " + synth.mapImage.pixels.length);
  synth.prepareAnimation();
  synth.renderFrame(0);
  return synth;
}

public void swapGen(PixelMapGen gen) {
  mapper.setGenerator(gen);
  // if we had a new mapper, we would call wavesynth.setMapper(mapper) and reset
  // synthImage locally.
  // As it is, mapper only changed its variables, so the swap is really simple
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
  println("press 'o' to open a JSON file (animation will stop).");
  println("press 'O' to reload the current JSON file (animation will stop).");
  println("press 'r' or 'R' to reset animation to step 0");
  println("press 'h' to show Help.");
}
