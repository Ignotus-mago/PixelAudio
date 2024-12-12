/**
 * BigWaveSynth shows how you can load a WaveSynth into the pixel array of a
 * MultiGen. MultiGen is child class of PixelMapGen that allows you to use
 * multiple PixelMapGens as if they were a single image with a single signal
 * path through them. Initially, we call initWaveDataList() to create a WaveData 
 * array with two operators. This example also allows you to load JSON files 
 * to reconfigure the WaveSynth.
 * 
 * Press the spacebar to start and stop animation. 
 * Press 'o' to open a JSON file, 
 * Press 'O' to reload the file, for example, after editing it while animation is running.
 * Press 'j' to save the current configuration to a JSON file. 
 * 
 */

import java.io.File;
import java.util.ArrayList;
import com.hamoid.*;
import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;
HilbertGen hGen;
MultiGen multigen;
ArrayList<PixelMapGen> genList;
ArrayList<int[]> offsetList;
int rows = 3;
int columns = 2;
int genWidth = 256;
int genHeight = 256;
PixelAudioMapper mapper;
ArrayList<WaveData> wdList;
WaveSynth wavesynth;
PImage synthImage;
// WaveSynth variables
float myGamma = 1.0f;      // a non-linear brightness adjustment
int animSteps = 240;       // number of steps in the animation
int animStop = animSteps;  // The step at which the animation should stop (not used here)
int step = 0;              // the current step in the animation
String comments;           // a JSON field that provides information about the WaveSynth effects it produces

// file i/o
String jsonFolder;
File currentDataFile;
String currentFileName;
String daPath;
JSONObject json;
// animation
// animation
boolean isAnimating = true;
boolean oldIsAnimating;
// video export
boolean isRecordingVideo = false;
VideoExport videx = null;    // hamoid library class for video export (requires ffmpeg)
String videoFilename = "pixelAudio_video.mp4";


public void settings() {
  size(2 * rows * genWidth, 2 * columns * genHeight);
}

public void setup() {
  pixelaudio = new PixelAudio(this);
  genList = new ArrayList<PixelMapGen>();
  offsetList = new ArrayList<int[]>();
  loadGenLists();
  multigen = new MultiGen(rows * genWidth, columns * genHeight, offsetList, genList);
  mapper = new PixelAudioMapper(multigen);
  wdList = initWaveDataList();
  wavesynth = new WaveSynth(mapper, wdList);
  initWaveSynth(wavesynth);
  synthImage = wavesynth.mapImage;
  // path to the folder where PixelAudio examples keep their data files 
  // such as image, audio, .json, etc.
  daPath = sketchPath("") + "../examples_data/";
  jsonFolder = daPath + "JSON";
}

public void loadGenLists() {
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
  synth.setSampleRate(genWidth * genWidth);
  println("--- mapImage size = " + synth.mapImage.pixels.length);
  synth.prepareAnimation();
  synth.renderFrame(0);
  return synth;
}

public void draw() {
  image(synthImage, 0, 0, width, height);
  if (isAnimating) stepAnimation();
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
        // isAnimating = false;
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
  case 'r':
  case 'R':
    step = 0;
    wavesynth.renderFrame(step);
    break;
  case 'v':
  case 'V':
    isRecordingVideo = !isRecordingVideo;
    println("isRecordingVideo is "+ isRecordingVideo);
    if (isRecordingVideo) {
      step = 0;
      wavesynth.renderFrame(step);
      isAnimating = true;
    }
    break;
  case 'h':
    break;
  default:
    break;
  }
}

public void stepAnimation() {
  if (!isAnimating) return;
  if (step >= animStop) {
    println("--- Completed video at frame "+ animStop);
    step = 0;
    if (isRecordingVideo) {
      isRecordingVideo = false;
      videx.endMovie();
    }
  }
  else {
    step += 1;
    if (isRecordingVideo) {
      if (videx == null) {
        println("----->>> start video recording ");
        videx = new VideoExport(this, videoFilename);
        videx.setFrameRate(wavesynth.videoFrameRate);
        videx.startMovie();
      }
      videx.saveFrame();
      println("-- video recording frame "+ step +" of "+ animStop);
    }
  }
  wavesynth.renderFrame(step);
}
