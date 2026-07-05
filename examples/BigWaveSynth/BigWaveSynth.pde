/**
 * BigWaveSynth shows how you can load a WaveSynth into the pixel array of a
 * MultiGen. MultiGen is a child class of PixelMapGen that allows you to use
 * multiple PixelMapGens to cover a single image with a single signal
 * path through them. This example also allows you to load JSON files in
 * this example's data folder to reconfigure the WaveSynth. Initially, we
 * call initWaveDataList() to create a WaveData array with two operators.
 *
 * Press ' ' to turn animation on or off.
 * Press 'g' to step through PixelMapGen instances hilb3x2Gen, bGen, and zGen.
 * Press 'o' to open a JSON file that defines a WaveSynth.
 * Press 'O' to reopen JSON file, if one is already open, or open a new JSON file.
 * Press 'j' or 'J' to save WaveSynth data to a JSON file.
 * Press 'f' to print the frameRate to the console.
 * Press 'r' to step through different settings of wavesynth.sampleRate.
 * Press 'R' to reset WaveSynth sample rate to genWidth * genHeight.
 * Press 'i' to reset animation step to 0.
 * Press 'p' to toggle playback rate mode.
 * Press 'S' to save WaveSynth audio to a WAV file.
 * Press 'v' or 'V' to toggle video recording.
 * Press 'h' to show help and key commands.
 *
 * See WaveSynthEditor for the complete set of WaveSynth parameters
 * you can edit in a GUI, load and save to files, and output as video.
 * See also: BigWaveSynthAudio, WaveSynthSequencer.
 *
 */

import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import ddf.minim.AudioOutput;
import ddf.minim.Minim;
import ddf.minim.MultiChannelBuffer;
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.sampler.*;
import net.paulhertz.pixelaudio.schedule.TimedLocation;

import com.hamoid.*;


PixelAudio pixelaudio;
PixelMapGen gen;

MultiGen hilb3x2Gen;
BoustropheGen bGen;
DiagonalZigzagGen zGen;

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
int animSteps = 240;      // number of steps in the animation
int animStop = animSteps;    // The step at which the animation should stop (not used here)
int step = 0;          // the current step in the animation
String comments;        // a JSON field that provides information about the WaveSynth effects it produces

// audio
Minim minim;
AudioOutput audioOut;
MultiChannelBuffer audioBuffer;
PASamplerInstrumentPool pool;
ADSRParams samplerEnv;
float sampleRate = 48000;
float outputGain = -9;
float samplerGain = 0.8f;
float maxAmplitude = 1.0f;
float attackTime = 0.005f;
float decayTime = 0.05f;
float sustainLevel = 0.8f;
float releaseTime = 0.4f;
int poolSize = 8;
int samplerMaxVoices = 4;
int noteDuration = 900;
int samplelen;
float[] audioSignal;
int audioLength;
boolean playAtAssignedFrequency = true;

// audio event markers
int sampleX;
int sampleY;
int samplePos;
ArrayList<TimedLocation> timeLocsArray;
int circleColor = color(233, 220, 199, 160);
int wsIndex = 1;

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
  // display window is scaled by 2 with respect to PixelMapGen instances
  size(2 * rows * genWidth, 2 * columns * genHeight);
}

public void setup() {
  pixelaudio = new PixelAudio(this);
  hilb3x2Gen = hilbertLoop3x2(genWidth, genHeight);
  println("-- hilb3x2Gen dimensions: ", hilb3x2Gen.getWidth(), hilb3x2Gen.getHeight());
  bGen = new BoustropheGen(rows * genWidth, columns * genHeight);
  zGen = new DiagonalZigzagGen(rows * genWidth, columns * genHeight);
  gen = hilb3x2Gen;
  mapper = new PixelAudioMapper(gen);
  wdList = initWaveDataList();
  wavesynth = new WaveSynth(mapper, wdList);
  initWaveSynth(wavesynth);
  synthImage = wavesynth.mapImage;
  initAudio();
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
	 * @param synth		a WaveSynth object whose attributes will be set
	 * @return			the WaveSynth object with attributes set
	 */
public WaveSynth initWaveSynth(WaveSynth synth) {
  synth.setGain(0.8f);
  synth.setGamma(myGamma);
  synth.setScaleHisto(false);
  synth.setAnimSteps(this.animSteps);
  synth.setSampleRate(genWidth * genHeight);
  println("--- mapImage size = " + synth.mapImage.pixels.length);
  synth.prepareAnimation();
  synth.renderFrame(0);
  return synth;
}

public void draw() {
  image(synthImage, 0, 0, width, height);
  if (isAnimating) stepAnimation();
  runTimeArray();
}

/**
 * Sets a new PixelMapGen for a PixelAudioMapper
 * @param gen
 */
public void swapGen(PixelMapGen gen) {
  mapper.setGenerator(gen);
  // if we had a new mapper, we would call wavesynth.setMapper(mapper) and reset
  // synthImage locally.
  // As it is, mapper only changed its variables, so the swap is really simple
}

public void keyPressed() {
  switch (key) {
  case ' ': // turn animation on or off
    isAnimating = !isAnimating;
    println(isAnimating ? "Starting animation at frame " + step + " of " + animSteps
      : "Stopping animation at frame " + step + " of " + animSteps);
    break;
  case 'g': // swap in a new gen to replace the one in use for mapper
    if (gen == zGen) {
      gen = hilb3x2Gen;
      swapGen(gen);
      break;
    }
    if (gen == hilb3x2Gen) {
      gen = bGen;
      swapGen(gen);
      break;
    }
    if (gen == bGen) {
      gen = zGen;
      swapGen(gen);
    }
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
  case 'j':
  case 'J': // save WaveSynth data to a JSON file
    saveWaveData();
    break;
  case 'f': // print the frameRate to the console
    println("--->> frame rate: "+ frameRate);
    break;
  case 'r': // step through different settings of wavesynth.sampleRate
    float rate = wavesynth.getSampleRate();
    if (rate == genWidth * genHeight) {
      rate = rate / 4;
    }
    else if (rate == (genWidth * genHeight) / 4) {
      rate = 48000;
    }
    else if (rate == 48000) {
      rate = 44100;
    }
    else if (rate == 44100) {
      rate = genWidth * genHeight;
    }
    wavesynth.setSampleRate(rate);
    wavesynth.renderFrame(step);
    renderSignal();
    println("----- WaveSynth sample rate is set to "+ wavesynth.getSampleRate());
    break;
  case 'R': // set the internal sample rate of the WaveSynth to genWidth * genHeight
    wavesynth.setSampleRate(genWidth * genHeight);
    wavesynth.renderFrame(step);
    renderSignal();
    println("----- WaveSynth sample rate is set to "+ wavesynth.getSampleRate());
    break;
  case 'i':
  case 'I': // reset animation step to 0
    step = 0;
    wavesynth.renderFrame(step);
    renderSignal();
    break;
  case 'p':
  case 'P': // toggle playback rate interpretation
    playAtAssignedFrequency = !playAtAssignedFrequency;
    renderSignal();
    println(playAtAssignedFrequency
      ? "----- playback uses WaveSynth sample rate: WaveData frequencies are preserved."
      : "----- playback uses audio output sample rate: pitch changes with WaveSynth sample rate.");
    break;
  case 'S': // save WaveSynth audio to a WAV file
    saveToAudio();
    break;
  case 'v':
  case 'V': // toggle video recording
    isRecordingVideo = !isRecordingVideo;
    println("isRecordingVideo is "+ isRecordingVideo);
    if (isRecordingVideo) {
      step = 0;
      wavesynth.renderFrame(step);
      isAnimating = true;
    }
    break;
  case 'h': // show help and key commands
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press ' ' to turn animation on or off.");
  println(" * Press 'g' to step through PixelMapGen instances hilb3x2Gen, bGen, and zGen.");
  println(" * Press 'o' to open a JSON file that defines a WaveSynth.");
  println(" * Press 'O' to reopen JSON file, if one is already open, or open a new JSON file.");
  println(" * Press 'j' or 'J' to save WaveSynth data to a JSON file.");
  println(" * Press 'f' to print the frameRate to the console.");
  println(" * Press 'r' to step through different settings of wavesynth.sampleRate.");
  println(" * Press 'R' to reset WaveSynth sample rate to genWidth * genHeight.");
  println(" * Press 'i' to reset animation step to 0.");
  println(" * Press 'p' to toggle playback rate mode.");
  println(" * Press 'S' to save WaveSynth audio to a WAV file.");
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
  } else {
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

public void initAudio() {
  minim = new Minim(this);
  audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
  audioOut.setGain(outputGain);
  audioBuffer = new MultiChannelBuffer(mapper.getSize(), 1);
  samplerEnv = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  ensureSamplerReady();
  renderSignal();
  timeLocsArray = new ArrayList<TimedLocation>();
}

void ensureSamplerReady() {
  if (pool != null) pool.setBuffer(audioBuffer, currentPlaybackBufferRate());
  else pool = new PASamplerInstrumentPool(audioBuffer, currentPlaybackBufferRate(), poolSize, samplerMaxVoices, audioOut, samplerEnv);
  pool.setGain(samplerGain);
}

float currentPlaybackBufferRate() {
  return playAtAssignedFrequency ? wavesynth.getSampleRate() : audioOut.sampleRate();
}

void updateAudioChain(float[] sig, float bufferSampleRate) {
  int targetSize = mapper.getSize();
  if (targetSize <= 0) return;
  float[] canonical = new float[targetSize];
  if (sig != null) {
    System.arraycopy(sig, 0, canonical, 0, Math.min(sig.length, targetSize));
  }
  audioSignal = canonical;
  audioLength = targetSize;
  if (audioBuffer == null || audioBuffer.getBufferSize() != targetSize) {
    audioBuffer = new MultiChannelBuffer(targetSize, 1);
  }
  audioBuffer.setChannel(0, canonical);
  if (pool != null) {
    pool.setBuffer(audioBuffer, bufferSampleRate);
  }
}

void updateAudioChain(float[] sig) {
  updateAudioChain(sig, currentPlaybackBufferRate());
}

public void renderSignal() {
  if (wavesynth == null || mapper == null) return;
  float[] sig = wavesynth.renderAudioRaw(step);
  sig = WaveSynth.normalize(sig, 0.9f);
  updateAudioChain(sig);
}

public void mouseClicked() {
  sampleX = screenToSampleX(mouseX);
  sampleY = screenToSampleY(mouseY);
  samplePos = mapper.lookupSignalPos(sampleX, sampleY);
  renderSignal();
  float panning = map(mouseX, 0, width, -0.875f, 0.875f);
  playSample(samplePos, calcSampleLen(), 0.8f, samplerEnv, 1.0f, panning);
}

public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pitch, float pan) {
  if (pool == null) return 0;
  samplelen = pool.playSample(samplePos, samplelen, amplitude, env, pitch, pan);
  int durationMS = (int)(samplelen / currentPlaybackBufferRate() * 1000);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
  return samplelen;
}

public int calcSampleLen() {
  float vary = 0;
  while (vary <= 0) {
    vary = (float) PixelAudio.gauss(1.0, 0.0625);
  }
  samplelen = (int)(abs((vary * noteDuration) * currentPlaybackBufferRate() / 1000.0f));
  return samplelen;
}

public void runTimeArray() {
  if (timeLocsArray == null) return;
  int currentTime = millis();
  timeLocsArray.forEach(tl -> {
    tl.setStale(tl.eventTime() < currentTime);
    if (!tl.isStale()) {
      drawCircle(sampleToScreenX(tl.getX()), sampleToScreenY(tl.getY()));
    }
  });
  timeLocsArray.removeIf(TimedLocation::isStale);
}

public void drawCircle(int x, int y) {
  fill(circleColor);
  noStroke();
  circle(x, y, 36);
}

int screenToSampleX(int x) {
  return min(max(0, (int) map(x, 0, width, 0, mapper.getWidth())), mapper.getWidth() - 1);
}

int screenToSampleY(int y) {
  return min(max(0, (int) map(y, 0, height, 0, mapper.getHeight())), mapper.getHeight() - 1);
}

int sampleToScreenX(int x) {
  return (int) map(x, 0, mapper.getWidth(), 0, width);
}

int sampleToScreenY(int y) {
  return (int) map(y, 0, mapper.getHeight(), 0, height);
}

public void saveToAudio() {
  float oldRate = wavesynth.getSampleRate();
  try {
    wavesynth.setSampleRate(sampleRate);
    float[] exportSig = wavesynth.renderAudioRaw(step);
    exportSig = WaveSynth.normalize(exportSig, 0.9f);
    saveAudioToFile(exportSig, sampleRate, "wavesynth_"+ wsIndex +".wav");
    wsIndex++;
  }
  catch (IOException e) {
    println("--->> There was an error outputting the audio file wavesynth.wav "+ e.getMessage());
  }
  catch (UnsupportedAudioFileException e) {
    println("--->> The file format is unsupported "+ e.getMessage());
  }
  finally {
    wavesynth.setSampleRate(oldRate);
    wavesynth.renderFrame(step);
    renderSignal();
  }
}

public void saveAudioToFile(float[] samples, float sampleRate, String fileName)
  throws IOException, UnsupportedAudioFileException {
  byte[] audioBytes = new byte[samples.length * 2];
  int index = 0;
  for (float sample : samples) {
    int intSample = (int) (sample * 32767);
    audioBytes[index++] = (byte) (intSample & 0xFF);
    audioBytes[index++] = (byte) ((intSample >> 8) & 0xFF);
  }
  ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
  AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
  AudioInputStream audioInputStream = new AudioInputStream(byteStream, format, samples.length);
  File outFile = new File(fileName);
  AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outFile);
  println("----- saved WaveSynth data as an audio file: "+ outFile.getAbsolutePath());
}
