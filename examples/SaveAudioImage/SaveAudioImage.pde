import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.*;
import java.util.ArrayList;
import javax.sound.sampled.*;


import ddf.minim.*;
import ddf.minim.ugens.*;
import net.paulhertz.pixelaudio.*;

/**
 * SaveAudioImage builds on BigWaveSynth, which shows how you can load a
 * WaveSynth into the pixel array of a MultiGen. SaveAudioImage uses a
 * WaveSynth but adds some useful features: 1. you can play the image as
 * audio, and 2. you can save the audio to a WAVE file.
 *
 * This example also allows you to load JSON files to reconfigure the WaveSynth.
 * Initially, we call initWaveDataList() to create a WaveData array with eight operators.
 *
 * Note that the sampleRate value influences the appearance of the image, the duration
 * of audio samples and the way data is written to an audio file. See the comments
 * in setup() for details.
 *
 *
 * Press the spacebar to start and stop animation.
 * Press 'o' key to open a JSON file
 * Press 'O' to reload the file, for example after changing it in a text editor.
 * Press the 'j' key to save the current configuration to a JSON file.
 * Press 's' to save wavesynth audio to a .wav audio file
 *
 *
 */
PixelAudio pixelaudio;
HilbertGen hGen;
MultiGen multigen;
ArrayList<PixelMapGen> genList;
ArrayList<int[]> offsetList;
int rows = 3;
int columns = 2;
int genWidth = 512;
int genHeight = 512;
PixelAudioMapper mapper;
int mapSize;
ArrayList<WaveData> wdList;
WaveSynth wavesynth;
PImage synthImage;
// WaveSynth variables
float myGamma = 1.0f;      // a non-linear brightness adjustment
int animSteps = 240;      // number of steps in the animation
int animStop = animSteps;    // The step at which the animation should stop (not used here)
int step = 0;          // the current step in the animation
String comments;        // a JSON field that provides information about the WaveSynth effects it produces

/** Minim audio library */
Minim minim;
AudioOutput audioOut;
MultiChannelBuffer audioBuffer;
boolean isBufferStale = false;
int sampleRate = 41500;
float[] audioSignal;
int[] rgbSignal;
int audioLength;
// SampleInstrument setup
float sampleScale = 4;
int sampleBase = 10250;
int samplelen = (int) (sampleScale * sampleBase);
Sampler audioSampler;
SamplerInstrument instrument;
// ADSR and params
ADSR adsr;
float maxAmplitude = 0.9f;
float attackTime = 0.2f;
float decayTime = 0.125f;
float sustainLevel = 0.5f;
float releaseTime = 0.2f;

// file i/o
String jsonFolder = "/JSON_data/";
File currentDataFile;
String currentFileName;
JSONObject json;
// animation
boolean isAnimating = true;
boolean oldIsAnimating;
boolean isLooping = true;
// interaction
int samplePos;


public void settings() {
  size(rows * genWidth, columns * genHeight);
}

public void setup() {
  println("--->> data path: "+ dataPath(""));
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
  sampleRate = 44100;
  initAudio();
  genList = new ArrayList<PixelMapGen>();
  offsetList = new ArrayList<int[]>();
  loadGenLists();
  multigen = new MultiGen(rows * genWidth, columns * genHeight, offsetList, genList);
  mapper = new PixelAudioMapper(multigen);
  wdList = initWaveDataList();
  wavesynth = new WaveSynth(mapper, wdList);
  initWaveSynth(wavesynth);
  synthImage = wavesynth.mapImage;
  mapSize = mapper.getSize();
}

public void initAudio() {
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  this.audioBuffer = new MultiChannelBuffer(1024, 1);
}

/**
 * Adds PixelMapGen objects to the genList. The genList will be used to initialize a
 * MultiGen, which in turn is passed to a WaveSynth.
 */
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
  println("--- mapImage size = " + synth.mapImage.pixels.length);
  synth.prepareAnimation();
  synth.renderFrame(0);
  return synth;
}

/**
 * Generates an ArrayList of WaveDate objects to be used by a WaveSynth to generate
 * RGB pixel values and (on request) audio signal values.
 *
 * @return an ArrayList of WaveData objects
 */
public ArrayList<WaveData> initWaveDataList() {
  ArrayList<WaveData> list = new ArrayList<WaveData>();
  // funda is the fundamental of a musical tone that is somewhat like a trumpet
  // in its frequency spectrum. Vary it to see how the image and sound change.
  float funda = 128.0f;
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

public void draw() {
  image(synthImage, 0, 0, width, height);
  if (isAnimating) stepAnimation();
}

public void stepAnimation() {
  step += 1;
  step %= animSteps;
  wavesynth.renderFrame(step);
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
  case 'f':
    println("--->> frame rate: "+ frameRate);
  case 'h':
    break;
  default:
    break;
  }
}

/**
 * Calls WaveSynth to render a audio sample array derived from the same math that creates the image.
 * The signal is normalized over the range (-0.95f, 0.95f);
 */
public void renderSignal() {
  this.audioSignal = wavesynth.renderAudio(step);
  audioLength = audioSignal.length;
  audioBuffer.setBufferSize(audioLength);
  audioBuffer.setChannel(0, audioSignal);
  println("--->> set audio buffer");
}

public void mousePressed() {
  samplePos = mapper.lookupSample(mouseX, mouseY);
  if (audioSignal == null || isBufferStale) {
    renderSignal();
    isBufferStale = false;
  }
  playSample(samplePos);
}

public int playSample(int samplePos) {
  audioSampler = new Sampler(audioBuffer, sampleRate, 8); // create a Minim Sampler from the buffer with 44.1 sampling
  // rate, for up to 8 simultaneous outputs
  audioSampler.amplitude.setLastValue(0.9f); // set amplitude for the Sampler
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
  instrument.play(samplelen / (float) (sampleRate));
  // return the length of the sample
  return samplelen;
}



//-------------------------------------------//
//             SAMPLER INSTRUMENT            //
//-------------------------------------------//


// using minim's Instrument interface
class SamplerInstrument implements Instrument {
  Sampler sampler;
  ADSR adsr;

  SamplerInstrument(Sampler sampler, ADSR adsr) {
    this.sampler = sampler;
    this.adsr = adsr;
    sampler.patch(adsr);
  }

  public void play() {
    // Trigger the ADSR envelope by calling noteOn()
    // Duration of 0.0 means the note is sustained indefinitely
    noteOn(0.0f);
  }

  public void play(float duration) {
    // Trigger the ADSR envelope by calling noteOn()
    // Duration of 0.0 means the note is sustained indefinitely
    // Duration should be in seconds
    // println("----->>> SamplerInstrument.play("+ duration +")");
    noteOn(duration);
  }

  @Override
    public void noteOn(float duration) {
    // Trigger the ADSR envelope and sampler
    adsr.noteOn();
    sampler.trigger();
    adsr.patch(audioOut);
    if (duration > 0) {
      // println("----->>> duration > 0");
      int durationMillis = (int) (duration * 1000);
      // schedule noteOff with an anonymous Timer and TimerTask
      new java.util.Timer().schedule(new java.util.TimerTask() {
        public void run() {
          noteOff();
        }
      }
      , durationMillis);
    }
  }

  @Override
    public void noteOff() {
    // println("----->>> noteOff event");
    adsr.unpatchAfterRelease(audioOut);
    adsr.noteOff();
  }

  // Getter for the Sampler instance
  public Sampler getSampler() {
    return sampler;
  }

  // Setter for the Sampler instance
  public void setSampler(Sampler sampler) {
    this.sampler = sampler;
  }

  // Getter for the ADSR instance
  public ADSR getADSR() {
    return adsr;
  }

  // Setter for the ADSR instance
  public void setADSR(ADSR adsr) {
    this.adsr = adsr;
  }
}




//-------------------------------------------//
//               JSON FILE I/O               //
//-------------------------------------------//


/**
 * Show an Open File dialog for JSON files
 */
public void loadWaveData() {
  File folderToStartFrom = new File(dataPath("") + jsonFolder + "//*.json");
  selectInput("Select a file to open", "fileSelectedOpen", folderToStartFrom);
}

/**
 * @param selection   a file selected from an Open File dialog
 */
public void fileSelectedOpen(File selection) {
  if (selection == null) {
    println("Window was closed or the user hit cancel.");
    isAnimating = oldIsAnimating;
    return;
  }
  currentDataFile = selection;
  println("User selected " + selection.getAbsolutePath());
  currentFileName = selection.getAbsolutePath();
  json = loadJSONObject(currentFileName);
  setWaveSynthFromJSON(json, wavesynth);
  surface.setTitle(currentFileName);
  isAnimating = oldIsAnimating;
}

/**
 * Parses a JSONObject to get values for a WaveSynth and load the values to the WaveSynth.
 *
 * @param json    JSONObject data, probably from a file
 * @param synth    a WaveSynth to modify with the JSONObject data values
 */
public void setWaveSynthFromJSON(JSONObject json, WaveSynth synth) {
  // set animation globals and WaveSynth properties
  animSteps = (json.isNull("steps")) ? 240 : json.getInt("steps");
  synth.setAnimSteps(animSteps);
  animStop = (json.isNull("stop")) ? this.animSteps : json.getInt("stop");
  synth.setStop(animStop);
  myGamma = (json.isNull("gamma")) ? 1.0f : json.getFloat("gamma");
  synth.setGamma(myGamma);
  comments = (json.isNull("comments")) ? "" : json.getString("comments");
  synth.setComments(comments);
  synth.setGain(json.isNull("blendFactor") ? 0.5f : json.getFloat("blendFactor"));
  synth.setVideoFilename((json.isNull("filename")) ? "wavesynth.mp4" : json.getString("filename"));
  synth.setScaleHisto((json.isNull("scaleHisto")) ? false : json.getBoolean("scaleHisto"));
  if (synth.isScaleHisto()) {
    synth.setHistoHigh((json.isNull("histoHigh")) ? 255 : json.getInt("histoHigh"));
    synth.setHistoLow((json.isNull("histoLow")) ? 0 : json.getInt("histoLow"));
  }
  // now load the JSON wavedata into ArrayList<WaveData> waveDataList
  JSONArray waveDataArray = json.getJSONArray("waves");
  int datalen = waveDataArray.size();
  ArrayList<WaveData> waveDataList = new ArrayList<WaveData>(datalen);
  for (int i = 0; i < datalen; i++) {
    // load fields common to both old and new format
    JSONObject waveElement = waveDataArray.getJSONObject(i);
    float f = waveElement.getFloat("freq");
    float a = waveElement.getFloat("amp");
    float p = waveElement.getFloat("phase");
    // float pInc = waveElement.getFloat("phaseInc");
    float dc = 0.0f;
    if (!waveElement.isNull("dc")) {
      dc = waveElement.getFloat("dc");
    }
    JSONObject rgbColor = waveElement.getJSONObject("color");
    int c = color(rgbColor.getInt("r"), rgbColor.getInt("g"), rgbColor.getInt("b"));
    float cycles;
    cycles = waveElement.getFloat("cycles");
    // frequency, amplitude, phase, dc, cycles, color, steps
    WaveData wd = new WaveData(f, a, p, dc, cycles, c, animSteps);
    waveDataList.add(wd);
  }
  synth.setWaveDataList(waveDataList);
  synth.prepareAnimation();
  synth.renderFrame(0);
  printWaveData(synth);
}

/**
 * Outputs fields from a WaveSynth to the console
 */
public void printWaveData(WaveSynth synth) {
  java.nio.file.Path path = java.nio.file.Paths.get(currentFileName);
  String fname = path.getFileName().toString();
  println("\n--------=====>>> Current WaveSynth instance for file " + fname + " <<<=====--------\n");
  println("Animation steps: " + synth.getAnimSteps());
  // println("Stop frame: "+ waveAnimal.getAnimSteps());
  println("gain: " + synth.getGain());
  println("gamma: " + synth.getGamma());
  if (synth.isScaleHisto()) {
    println("scaleHisto: " + synth.isScaleHisto());
    println("histoLow: " + synth.getHistoLow());
    println("histoHigh: " + synth.getHistoHigh());
  }
  println(fname);
  println("video filename: " + synth.getVideoFilename());
  // println("WaveData list for: "+ videoFilename);
  for (int i = 0; i < synth.waveDataList.size(); i++) {
    WaveData wd = synth.waveDataList.get(i);
    println("  " + (i + 1) + ":: " + wd.toString());
  }
  println("comments: " + synth.getComments() +"\n");
}

/**
 * Show a Save File dialog
 */
public void saveWaveData() {
  selectOutput("Select a file to write to:", "fileSelectedWrite");
}

public void fileSelectedWrite(File selection) {
  if (selection == null) {
    println("Window was closed or the user hit cancel.");
    return;
  }
  WaveSynth synth = this.wavesynth;
  println("User selected " + selection.getAbsolutePath());
  // Do we have a .json at the end?
  if (selection.getName().length() < 5
    || selection.getName().indexOf(".json") != selection.getName().length() - 5) {
    // problem missing ".json"
    currentFileName = selection.getAbsolutePath() + ".json"; // very rough approach...
  } else {
    currentFileName = selection.getAbsolutePath();
  }
  // put WaveData objects into an array
  JSONArray waveDataArray = new JSONArray();
  JSONObject waveElement;
  WaveData wd;
  for (int i = 0; i < synth.waveDataList.size(); i++) {
    wd = synth.waveDataList.get(i);
    waveElement = new JSONObject();
    waveElement.setInt("index", i);
    waveElement.setFloat("freq", wd.freq);
    waveElement.setFloat("amp", wd.amp);
    waveElement.setFloat("phase", wd.phase);
    waveElement.setFloat("phaseInc", wd.phaseInc);
    waveElement.setFloat("cycles", wd.phaseCycles);
    waveElement.setFloat("dc", wd.dc);
    // BADSR settings
    int[] rgb = PixelAudioMapper.rgbComponents(wd.waveColor);
    JSONObject rgbColor = new JSONObject();
    rgbColor.setInt("r", rgb[0]);
    rgbColor.setInt("g", rgb[1]);
    rgbColor.setInt("b", rgb[2]);
    waveElement.setJSONObject("color", rgbColor);
    // append wave data to array
    waveDataArray.append(waveElement);
  }
  // put the array into an object that tracks other state variables
  JSONObject stateData = new JSONObject();
  stateData.setInt("steps", synth.animSteps);
  stateData.setInt("stop", animStop);
  stateData.setFloat("blendFactor", synth.gain);
  stateData.setInt("dataFormat", 2);
  if (!selection.exists())
    stateData.setString("comments", "---");
  else
    stateData.setString("comments", synth.comments);
  // String videoName = selection.getName();
  String videoName = synth.videoFilename;
  if (videoName == null || videoName.equals("")) {
    videoName = selection.getName();
    if (videoName.indexOf(".json") != -1) {
      videoName = videoName.substring(0, videoName.indexOf(".json")) + ".mp4";
    } else {
      videoName += ".mp4";
    }
  }
  println("----->>> video name is " + videoName);
  synth.videoFilename = videoName; // ???
  stateData.setString("filename", videoName);
  stateData.setFloat("gamma", synth.gamma);
  stateData.setBoolean("scaleHisto", synth.isScaleHisto);
  stateData.setFloat("histoHigh", synth.histoHigh);
  stateData.setFloat("histoLow", synth.histoLow);
  stateData.setJSONArray("waves", waveDataArray);
  saveJSONObject(stateData, currentFileName);
  currentDataFile = new File(currentFileName);
  surface.setTitle(currentFileName);
}



//-------------------------------------------//
//              AUDIO FILE I/O               //
//-------------------------------------------//

public void saveToAudio() {
  renderSignal();
  audioSignal = WaveSynth.normalize(audioSignal, 0.9);
  try {
    saveAudioToFile(audioSignal, sampleRate, sketchPath("") + "/wavesynth.wav");
    println("Saved file to sketch path: "+ sketchPath(""));
  }
  catch (IOException e) {
    println("--->> There was an error outputting the audio file wavesynth.wav."+ e.getMessage());
  }
  catch (UnsupportedAudioFileException e) {
    println("--->> The file format is unsupported."+ e.getMessage());
  }
}

/**
 * Saves to a 32-bit floating point format that has higher resolution than 16-bit integer PCM.
 * The format can't be opened by Processing but can be opened by audio applications.
 *
 * @param samples      an array of floats in the audio range (-1.0f, 1.0f)
 * @param sampleRate    the sample rate for the file
 * @param fileName      name of the file
 * @throws IOException    an Exception you'll need to catch to call this method (see keyPressed entry for 's')
 */
public static void saveAudioTo32BitPCMFile(float[] samples, float sampleRate, String fileName) throws IOException {
  // convert samples to 32-bit PCM float
  byte[] audioBytes = new byte[samples.length * 4];
  int index = 0;
  // convert to IEEE 754 floating-point "single format" bit layout
  for (float sample : samples) {
    int intBits = Float.floatToIntBits(sample);
    audioBytes[index++] = (byte) (intBits & 0xFF);
    audioBytes[index++] = (byte) ((intBits >> 8) & 0xFF);
    audioBytes[index++] = (byte) ((intBits >> 16) & 0xFF);
    audioBytes[index++] = (byte) ((intBits >> 24) & 0xFF);
  }
  ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
  AudioFormat format = new AudioFormat(sampleRate, 32, 1, true, false);
  AudioInputStream audioInputStream = new AudioInputStream(byteStream, format, samples.length);
  File outFile = new File(fileName);
  AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outFile);
}


/**
 * Saves audio data to 16-bit integer PCM format, which Processing can also open.
 *
 * @param samples      an array of floats in the audio range (-1.0f, 1.0f)
 * @param sampleRate    audio sample rate for the file
 * @param fileName      name of the file to save to
 * @throws IOException    an Exception you'll need to catch to call this method (see keyPressed entry for 's')
 * @throws UnsupportedAudioFileException    another Exception (see keyPressed entry for 's')
 */
public static void saveAudioToFile(float[] samples, float sampleRate, String fileName)
  throws IOException, UnsupportedAudioFileException {
  // Convert samples from float to 16-bit PCM
  byte[] audioBytes = new byte[samples.length * 2];
  int index = 0;
  for (float sample : samples) {
    // Scale sample to 16-bit signed integer
    int intSample = (int) (sample * 32767);
    // Convert to bytes
    audioBytes[index++] = (byte) (intSample & 0xFF);
    audioBytes[index++] = (byte) ((intSample >> 8) & 0xFF);
  }
  // Create an AudioInputStream
  ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
  AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
  AudioInputStream audioInputStream = new AudioInputStream(byteStream, format, samples.length);
  // Save the AudioInputStream to a WAV file
  File outFile = new File(fileName);
  AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outFile);
}
