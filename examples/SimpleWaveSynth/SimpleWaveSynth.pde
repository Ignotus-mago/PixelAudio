/**
 * SimpleWaveSynth demonstrates the basics of setting up a WaveSynth as an animated visual display.
 *
 * Press ' ' (spacebar) to toggle animation.
 * Press '1' to set WaveSynth gamma to 1.0.
 * Press '2' to set WaveSynth gamma to 1.4.
 * Press '3' to set WaveSynth gamma to 1.8.
 * Press '4' to set WaveSynth gamma to 0.5.
 * Press 'g' to swap in a different PixelMapGen for the WaveSynth.
 * Press 'f' to set the WaveSynth to output an image rotated 90 degrees clockwise.
 * Press 'r' to set the internal sample rate of the WaveSynth to 44100.
 * Press 'R' to set the internal sample rate of the WaveSynth to mapSize.
 * Press 't' to output the gamma table for the current gamma value myGamma.
 * Press 'i' to output display frame rate to the console.
 * Press 'h' to show the help message in the console.
 *
 */

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
int imageWidth = 1024;
int imageHeight = 1024;
PImage synthImage;
float myGamma = 1.0f;
int animSteps = 240;
int step = 0;
int start = 0;
int timespan = 0;
boolean isTrackTime = false;
boolean isAnimating = true;

public void settings() {
  size(imageWidth, imageHeight, JAVA2D);
}

public void setup() {
  frameRate(30);
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
  if (isTrackTime) {
    trackTime();
  }
  if (isAnimating) stepAnimation();
}

public void trackTime() {
  if (step == 0) {
    start = millis();
    println("----- timer started -----");
  }
  if (step % 24 == 0) {
    println("-- step " + step);
  }
  if (step == animSteps - 1) {
    timespan = (millis() - start);
    int xms = timespan % 1000;
    int secs = (timespan - xms) / 1000;
    int xsecs = secs % 60;
    int mins = secs / 60;
    println("--->> elapsed time: " + mins + ":" + xsecs + ":" + xms);
  }
}

public void stepAnimation() {
  step += 1;
  step %= animSteps;
  wavesynth.renderFrame(step);
}

public void keyPressed() {
  switch (key) {
  case ' ': // toggle animation
    isAnimating = !isAnimating;
    break;
  case '1': // set WaveSynth gamma to 1.0
    myGamma = 1.0f;
    wavesynth.setGamma(myGamma);
    println("----- WaveSynth.gamma is set to "+ wavesynth.getGamma());
    break;
  case '2': // set WaveSynth gamma to 1.4
    myGamma = 1.4f;
    wavesynth.setGamma(myGamma);
    println("----- WaveSynth.gamma is set to "+ wavesynth.getGamma());
    break;
  case '3': // set WaveSynth gamma to 1.8
    myGamma = 1.8f;
    wavesynth.setGamma(myGamma);
    println("----- WaveSynth.gamma is set to "+ wavesynth.getGamma());
    break;
  case '4': // set WaveSynth gamma to 0.5
    myGamma = 0.5f;
    wavesynth.setGamma(myGamma);
    println("----- WaveSynth.gamma is set to "+ wavesynth.getGamma());
    break;
  case 'g': // swap in a different PixelMapGen for the WaveSynth
    // change the gen
    if (gen == zGen) {
      gen = hGen;
      swapGen(gen);
      println("----- using Hilbert curve generator: " + gen.describe());
      break;
    }
    if (gen == hGen) {
      gen = mGen;
      swapGen(gen);
      println("----- using Moore curve generator: " + gen.describe());
      break;
    }
    if (gen == mGen) {
      gen = zGen;
      swapGen(gen);
      println("----- using diagonal zigzag generator: " + gen.describe());
    }
    break;
  case 'f': // set the WaveSynth to output an image rotated 90 degrees clockwise
    // rotate gen 90 degrees clockwise
    gen.setTransformType(AffineTransformType.R270);
    swapGen(gen);
    println("----- Generator was rotated 90 degrees clockwise");
    break;
  case 'i': // output framerate info
    println(" ----- frame rate is "+ frameRate);
    break;
  case 'r': // set the internal sample rate of the WaveSynth to 44100
    wavesynth.setSampleRate(44100);
    println("----- WaveSynth sample rate is set to "+ wavesynth.getSampleRate());
    break;
  case 'R': // set the internal sample rate of the WaveSynth to mapSize
    wavesynth.setSampleRate(wavesynth.mapSize);
    println("----- WaveSynth sample rate is set to "+ wavesynth.getSampleRate());
    break;
  case 't': // output the gamma table for the current gamma value myGamma
    makeGammaTable(myGamma);
    break;
  case 'h': // show the help message in the console
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press ' ' (spacebar) to toggle animation.");
  println(" * Press '1' to set WaveSynth gamma to 1.0.");
  println(" * Press '2' to set WaveSynth gamma to 1.4.");
  println(" * Press '3' to set WaveSynth gamma to 1.8.");
  println(" * Press '4' to set WaveSynth gamma to 0.5.");
  println(" * Press 'g' to swap in a different PixelMapGen for the WaveSynth.");
  println(" * Press 'f' to set the WaveSynth to output an image rotated 90 degrees clockwise.");
  println(" * Press 'r' to set the internal sample rate of the WaveSynth to 44100.");
  println(" * Press 'R' to set the internal sample rate of the WaveSynth to mapSize.");
  println(" * Press 't' to output the gamma table for the current gamma value myGamma.");
  println(" * Press 'i' to output display frame rate to the console.");
  println(" * Press 'h' to show the help message in the console.");
}

// code for generating a gamma table, a non-linear adjustment of brightness
public void makeGammaTable(float gamma) {
  int[] gammaTable = new int[256];
  println("----- GAMMA TABLE, gamma = " + gamma + " -----");
  println("-- index  - value  - RGB -");
  for (int i = 0; i < gammaTable.length; i++) {
    float c = i / (float) (gammaTable.length - 1);
    c = (float) (Math.pow(c, gamma) * (gammaTable.length - 1));
    gammaTable[i] = (int) c;
    println("-- " + i + "  " + nf(c, 0, 4) + "  " + gammaTable[i]);
  }
}
