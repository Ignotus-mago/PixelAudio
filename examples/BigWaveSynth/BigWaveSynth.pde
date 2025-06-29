/**
 * BigWaveSynth shows how you can load a WaveSynth into the pixel array of a
 * MultiGen. MultiGen is child class of PixelMapGen that allows you to use
 * multiple PixelMapGens as if they were a single image with a single signal
 * path through them. This example also allows you to load JSON files in 
 * this example's data folder to reconfigure the WaveSynth. Initially, we 
 * call initWaveDataList() to create a WaveData array with two operators. 
 * 
 * Press ' ' to turn animation on or off.
 * Press 'o' to open a JSON file that defines a WaveSynth .
 * Press 'O' to reopen JSON file, if one is already open, or open a new JSON file.
 * Press 'j' or 'J' to save WaveSynth data to a JSON file.
 * Press 'f' to print the frameRate to the console.
 * Press 'r' to animation step to 0.
 * Press 'v' or 'V' to toggle video recording.
 * Press 'h' to show help and key commands.
 * 
 * See also: BigWaveSynthAudio, WaveSynthSequencer.
 * 
 */

import java.io.File;
import java.util.ArrayList;
import com.hamoid.*;
import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;
HilbertGen hGen;
MultiGen multigen;
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
String jsonFolder = "/JSON_data/";
File currentDataFile;
String currentFileName;
JSONObject json;

// animation
boolean isAnimating = true;
boolean oldIsAnimating;
boolean isLooping = true;

// video export
boolean isRecordingVideo = false;
VideoExport videx = null;    // hamoid library class for video export (requires ffmpeg)
String videoFilename = "pixelAudio_video.mp4";

public void settings() {
  size(2 * rows * genWidth, 2 * columns * genHeight);
}
public void setup() {
  pixelaudio = new PixelAudio(this);
  multigen = hilbertLoop3x2(genWidth, genHeight);
  mapper = new PixelAudioMapper(multigen);
  wdList = initWaveDataList();
  wavesynth = new WaveSynth(mapper, wdList);
  initWaveSynth(wavesynth);
  synthImage = wavesynth.mapImage;
  showHelp();
}

/**
 * Generates a looping fractal signal path consisting of 6 HilbertGens,
 * arranged 3 wide and 2 tall, to fit a 3 * genW by 2 * genH image. 
 * This particular MultiGen configuration is used so extensively in my 
 * sample code that I've given it a factory method in the HilbertGen class.
 * It's written out here so you can see how it works. 
 * 
 * Note that genH must equal genW and both must be powers of 2. For the 
 * image size we're using in this example, genW = image width / 3 and 
 * genH = image height / 2.
 * 
 * @param genW    width of each HilbertGen 
 * @param genH    height of each HilbertGen
 * @return a 3 x 2 array of Hilbert curves, connected in 
 *         a loop (3 * genWidth by 2 * genHeight pixels)
 */
public MultiGen hilbertLoop3x2(int genW, int genH) {
  // list of PixelMapGens that create an image using mapper
  ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
  // list of x,y coordinates for placing gens from genList
  ArrayList<int[]> offsetList = new ArrayList<int[]>();
  genList.add(new HilbertGen(genW, genH, PixelMapGen.fx270));
  offsetList.add(new int[] { 0, 0 });
  genList.add(new HilbertGen(genW, genH, PixelMapGen.nada));
  offsetList.add(new int[] { genW, 0 });
  genList.add(new HilbertGen(genW, genH, PixelMapGen.fx90));
  offsetList.add(new int[] { 2 * genW, 0 });
  genList.add(new HilbertGen(genW, genH, PixelMapGen.fx90));
  offsetList.add(new int[] { 2 * genW, genH });
  genList.add(new HilbertGen(genW, genH, PixelMapGen.r180));
  offsetList.add(new int[] { genW, genH });
  genList.add(new HilbertGen(genW, genH,PixelMapGen.fx270));
  offsetList.add(new int[] { 0, genH });
  return new MultiGen(3 * genW, 2 * genH, offsetList, genList);
}

/**
 * Initializes a list of WaveData for use by a WaveSynth.
 * 
 * @return an ArrayList of WaveData objects
 */
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

/**
 * Sets gain, gamma, isScaleHisto, animSteps, and sampleRate instance variables 
 * of a WaveSynth object and generates its first frame of animation.
 * 
 * @param synth    a WaveSynth object whose attributes will be set
 * @return      the WaveSynth object with attributes set
 */
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
  case ' ': // turn animation on or off
    isAnimating = !isAnimating;
    println(isAnimating ? "Starting animation at frame " + step + " of " + animSteps
        : "Stopping animation at frame " + step + " of " + animSteps);
    break;
  case 'o': // open a JSON file that defines a WaveSynth 
    // turn off animation while reading new settings for wavesynth
    oldIsAnimating = isAnimating;
    isAnimating = false;
    this.loadWaveData();
    break;
  case 'O': // reopen JSON file, if one is already open, or open a new JSON file
    if (currentDataFile == null) {
      loadWaveData();
    } else {
      fileSelectedOpen(currentDataFile);
      println("-------->>>>> Reloaded file");
    }
    break;
  case 'j': case 'J': // save WaveSynth data to a JSON file
    saveWaveData();
    break;
  case 'f': // print the frameRate to the console
    println("--->> frame rate: "+ frameRate);
  case 'r': case 'R': // reset animation step to 0 
    step = 0;
    wavesynth.renderFrame(step);
    break;
  case 'v': case 'V': // toggle video recording
    isRecordingVideo = !isRecordingVideo;
    println("isRecordingVideo is "+ isRecordingVideo);
    if (isRecordingVideo) {
      step = 0;
      wavesynth.renderFrame(step);
      isAnimating = true;
    }
    break;
  case 'h': // show help and key commands
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press ' ' to turn animation on or off.");
  println(" * Press 'o' to open a JSON file that defines a WaveSynth .");
  println(" * Press 'O' to reopen JSON file, if one is already open, or open a new JSON file.");
  println(" * Press 'j' or 'J' to save WaveSynth data to a JSON file.");
  println(" * Press 'f' to print the frameRate to the console.");
  println(" * Press 'r' to animation step to 0.");
  println(" * Press 'v' or 'V' to toggle video recording.");
  println(" * Press 'h' to show help and key commands.");
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
