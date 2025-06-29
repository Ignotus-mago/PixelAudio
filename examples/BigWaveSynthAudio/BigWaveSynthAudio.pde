import java.io.*;
import java.util.ArrayList;
import javax.sound.sampled.*;


import ddf.minim.AudioOutput;
import ddf.minim.Minim;
import ddf.minim.MultiChannelBuffer;
import ddf.minim.ugens.ADSR;
import ddf.minim.ugens.Instrument;
import ddf.minim.ugens.Sampler;
import net.paulhertz.pixelaudio.*;

/**
 * BigWaveSynthAudio shows how you can load a WaveSynth into the pixel array of a
 * MultiGen and then play audio using the audio signal corresponding to the WaveSynth.
 * MultiGen is child class of PixelMapGen that allows you to use
 * multiple PixelMapGens as if they were a single image with a single signal
 * path through them. This example also allows you to load JSON files in 
 * this example's data folder to reconfigure the WaveSynth. Initially, we 
 * call initWaveDataList() to create a WaveData array with two operators. 
 * 
 * Note the role that sampleRate plays in generating a frequency from the WaveSynth.
 * There are two sampleRate values that influence the frequency, one global and one
 * local to the WaveSynth object. The global sampleRate affects the frequency of 
 * audio samples in the audio buffer in the playSample() method:
 * 
 *   audioSampler = new Sampler(audioBuffer, sampleRate, 8);
 *
 * The sampleRate local to the waveSynth affects the pattern in the display image. 
 * It's set in the initWaveSynth() method:
 * 
 *   synth.setSampleRate(genWidth * genWidth);
 * 
 * In effect, the frequencies set in the WaveSynth WaveData objects fill the entire image.
 * A frequency of 768 will place 768 copies of a sine wave along the signal path over the
 * entire bitmap of the MultiGen whose mapImage is displayed on the screen. The MultiGen 
 * for this example is composed of six Hilbert curves (HilbertGen objects). Try varying
 * the settings in initWaveSynth to get an idea of how the diplay is affected. Keys
 * 1 through 7 will do this for you.
 *
 * Try varying the global sampleRate to hear how playback of the audio frequency is affected.
 * 22050 is a good value to try. Check out WaveSynthSequencer for an example where the global 
 * sampleRate and the WaveSynth.sampleRate are the same: this makes the frequency generated 
 * from the WaveSynth precisely the one requested for playing the synth as a musical instrument.
 * 
 * Press ' ' to turn animation on and off.
 * Press 'd' to turn raindrops on and off
 * Press 'o' to open a JSON file with WaveSynth data.
 * Press 'O' to reload the most recent JSON file.
 * Press 'j' 0r 'J' to save WaveSynth data to a JSON file
 * Press 'f' to show the frame rate in the console
 * Press '1' to call wavesynth.setSampleRate(genWidth * genHeight) 
 * Press '2' to call wavesynth.setSampleRate(genWidth * genHeight / 2) 
 * Press '3' to call wavesynth.setSampleRate(genWidth * genHeight / 3) 
 * Press '4' to call wavesynth.setSampleRate(genWidth * genHeight / 4) 
 * Press '5' to call wavesynth.setSampleRate(genWidth * genHeight / 5) 
 * Press '6' to call wavesynth.setSampleRate(genWidth * genHeight / 6) 
 * Press '7' to call wavesynth.setSampleRate(this.sampleRate) 
 * Press 's' to save display to a PNG file
 * Press 'S' to save audio to a WAV file
 * Press 'h' to show this help message in the console
 * 
 * See also: BigWaveSynth, WaveSynthSequencer
 * 
 */

PixelAudio pixelaudio;      // our shiny new library
HilbertGen hGen;            // a PixelMapGen to draw Hilbert curves
MultiGen multigen;          // a PixelMapGen that handles multiple gens
int rows = 3;               // rows
int columns = 2;            // columns
int genWidth = 512;         // width of PixelMapGen objects, for hGen must be a power of 2
int genHeight = 512;        // height of PixelMapGen objects, for hGen must be equal to width
PixelAudioMapper mapper;    // instance of a class for reading, writing, and transcoding audio and image data 
int mapSize;                // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc. 
ArrayList<WaveData> wdList; // a list of audio "operators" that drive a WaveSynth
WaveSynth wavesynth;        // a sort of additive audio synthesis color organ that also produces sound
int imgIndex = 1;
int audIndex = 1;
PImage synthImage;          // local variable that points to wavesynth's PImage instance variable

// WaveSynth variables
float myGamma = 1.0f;        // a non-linear brightness adjustment
int animSteps = 240;         // number of steps in the animation
int animStop = animSteps;    // The step at which the animation should stop (not used here)
int step = 0;                // the current step in the animation
String comments;             // a JSON field that provides information about the WaveSynth effects it produces

/** Minim audio library */
Minim minim;                 // library that handles audio 
AudioOutput audioOut;        // line out to sound hardware
MultiChannelBuffer audioBuffer;   // data structure to hold audio samples
boolean isBufferStale = false;    // flags that audioBuffer needs to be reset: i.e., after loading JSON data to wavesynth
int sampleRate = 48000;      // a critical value for display and audio, see the setup method
float[] audioSignal;         // the audio signal as an array of floats
int[] rgbSignal;             // the colors in the display image, in the order the signal path visits them
int audioLength;             // length of the audioSignal, same as the number of pixels in the display image

// SampleInstrument setup
float sampleScale = 4;         // == number of divisions of one second, used to vary duration
int sampleBase = (int) (sampleRate/(float) sampleScale);  
int samplelen = sampleRate;    // one second
Sampler audioSampler;          // minim class for sampled sound
WFInstrument instrument;       // local class to wrap audioSampler

// ADSR and params
ADSR adsr;            // good old attack, decay, sustain, release
float maxAmplitude = 0.7f;
float attackTime = 0.8f;
float decayTime = 0.5f;
float sustainLevel = 0.125f;
float releaseTime = 0.5f;

// file i/o
String daPath;
String jsonFolder;
File currentDataFile;
String currentFileName;
JSONObject json;

// animation
boolean isAnimating = true;
boolean oldIsAnimating;
boolean isLooping = true;
int circleColor = color(233, 220, 199, 128);
boolean isRaining = false;

// interaction
int sampleX;
int sampleY;
int samplePos;            // position of a mouse click along the signal path, index into the audio array
ArrayList<TimedLocation> timeLocsArray;
int count = 0;  

public void settings() {
  size(rows * genWidth, columns * genHeight);
}

public void setup() {
  pixelaudio = new PixelAudio(this);
  minim = new Minim(this);
  initAudio();
  initAudio();                           // set up audio output and an audio buffer
  multigen = hilbertLoop3x2(genWidth, genHeight);  // See the MultiGenDemo example for details on how MultiGen works
  mapper = new PixelAudioMapper(multigen);         // initialize a PixelAudioMapper
  mapSize = mapper.getSize();            // set mapSize
  wdList = initWaveDataList();           // generate a list of WaveData objects
  wavesynth = new WaveSynth(mapper, wdList);       // create a WaveSynth object
  initWaveSynth(wavesynth);              // set wavesynth fields
  synthImage = wavesynth.mapImage;       // set the image to display
  timeLocsArray = new ArrayList<TimedLocation>();  // initialize mouse event tracking array
  // path to the folder where PixelAudio examples keep their data files 
  // such as image, audio, .json, etc.
  daPath = sketchPath("") + "../examples_data/";
  jsonFolder = daPath + "JSON/";
  showHelp();
}

public void initAudio() {
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  // create an audioBuffer for our audio data
  this.audioBuffer = new MultiChannelBuffer(1024, 1);
  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
  adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
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

public ArrayList<WaveData> initWaveDataList() {
  ArrayList<WaveData> list = new ArrayList<WaveData>();
  float frequency = 768;     // sampleRate/62.5f;
  float amplitude = 0.8f;
  float phase = 0.0f;
  float dc = 0.0f;
  float cycles = 1.0f;
  int waveColor = color(159, 190, 251);
  int steps = this.animSteps;
  WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  frequency = 192;       // sampleRate/250.0f;
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
  runTimeArray();                // animate audio event markers
  if (isRaining) {
    // animation slows the frame rate, so we change the threshold when animating
    float thresh = (isAnimating) ? 0.25f : 0.05f;
    if (random(0,1) < thresh) {
      raindrops();              // trigger random audio events
    }
  }
}

/**
 * Run the animation for audio events. 
 */
public void runTimeArray() {
  int currentTime = millis();
  timeLocsArray.forEach(tl -> {
    tl.setStale(tl.stopTime() < currentTime);
    if (!tl.isStale()) {
      drawCircle(tl.getX(), tl.getY());
    }
  });
  timeLocsArray.removeIf(TimedLocation::isStale);
}

/**
 * Draws a circle at the location of an audio trigger (mouseDown event).
 * @param x    x coordinate of circle
 * @param y    y coordinate of circle
 */
public void drawCircle(int x, int y) {
  float size = isRaining? random(10, 30) : 60;
  fill(circleColor);
  noStroke();
  circle(x, y, size);
}  

/**
 * Trigger a WaveSynth sample at a random location.
 */
public void raindrops() {
  int signalPos = (int) random(samplelen, mapSize - samplelen - 1);
  int[] coords = mapper.lookupCoordinate(signalPos);
  sampleX = coords[0];
  sampleY = coords[1];
  if (audioSignal == null || isBufferStale) {
    renderSignal();
    isBufferStale = false;
  }
  playSample(audioBuffer, signalPos, samplelen, 0.15f, 
             new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime));
}

public void keyPressed() {
  int newSampleRate;
  switch (key) {
  case ' ':
    isAnimating = !isAnimating;
    println(isAnimating ? "Starting animation at frame " + step + " of " + animSteps
        : "Stopping animation at frame " + step + " of " + animSteps);
    break;
  case 'd':
    isRaining = !isRaining;
    println("--- isRaining = " + isRaining);
    break;
  case 'o':
    // turn off animation while reading new settings for wavesynth
    oldIsAnimating = isAnimating;
    isAnimating = false;
    this.loadWaveData();
    // the audio buffer will need to be refreshed
    isBufferStale = true;
    break;
  case 'O':
    if (currentDataFile == null) {
      loadWaveData();
    } 
    else {
      fileSelectedOpen(currentDataFile);
      println("-------->>>>> Reloaded file");
    }
    break;
  case 'j':
  case 'J':
    saveWaveData();
    break;
  case 'f':
    println("--->> frame rate: " + frameRate);
  case '1':
    newSampleRate = genWidth * genWidth;
    println("--->> new sample rate for WaveSynth = genWidth * genWidth = "+ newSampleRate);
    wavesynth.setSampleRate(newSampleRate);
    isBufferStale = true;
    break;
  case '2':
    newSampleRate = genWidth * genWidth / 2;
    println("--->> new sample reate for WaveSynth = genWidth * genWidth / 2 = "+ newSampleRate);
    wavesynth.setSampleRate(newSampleRate);
    isBufferStale = true;
    break;
  case '3':
    newSampleRate = genWidth * genWidth / 3;
    println("--->> new sample reate for WaveSynth = genWidth * genWidth / 3 = "+ newSampleRate);
    wavesynth.setSampleRate(newSampleRate);
    isBufferStale = true;
    break;
  case '4':
    newSampleRate = genWidth * genWidth / 4;
    println("--->> new sample reate for WaveSynth = genWidth * genWidth / 4 = "+ newSampleRate);
    wavesynth.setSampleRate(newSampleRate);
    isBufferStale = true;
    break;
  case '5':
    newSampleRate = genWidth * genWidth / 5;
    println("--->> new sample reate for WaveSynth = genWidth * genWidth / 5 = "+ newSampleRate);
    wavesynth.setSampleRate(newSampleRate);
    isBufferStale = true;
    break;
  case '6':
    newSampleRate = genWidth * genWidth / 6;
    println("--->> new sample reate for WaveSynth = genWidth * genWidth / 6 = "+ newSampleRate);
    wavesynth.setSampleRate(newSampleRate);
    isBufferStale = true;
    break;
  case '7':
    newSampleRate = this.sampleRate;
    println("--->> new sample reate for WaveSynth = this.sampleRate = "+ newSampleRate);
    wavesynth.setSampleRate(newSampleRate);
    isBufferStale = true;
    break;
  case 's': 
    synthImage.save(sketchPath() + "/wavesynth_"+ imgIndex +".png");
    println("-- saved to image file");
    imgIndex++;
    break;
  case 'S': 
    saveToAudio();
    println("-- saved to audio file");
    audIndex++;
    break;
  case 'h':
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press ' ' to turn animation on and off.");
  println(" * Press 'd' to turn raindrops on and off");
  println(" * Press 'o' to open a JSON file with WaveSynth data.");
  println(" * Press 'O' to reload the most recent JSON file.");
  println(" * Press 'j' 0r 'J' to save WaveSynth data to a JSON file");
  println(" * Press 'f' to show the frame rate in the console");
  println(" * Press '1' to call wavesynth.setSampleRate(genWidth * genHeight) ");
  println(" * Press '2' to call wavesynth.setSampleRate(genWidth * genHeight / 2) ");
  println(" * Press '3' to call wavesynth.setSampleRate(genWidth * genHeight / 3) ");
  println(" * Press '4' to call wavesynth.setSampleRate(genWidth * genHeight / 4) ");
  println(" * Press '5' to call wavesynth.setSampleRate(genWidth * genHeight / 5) ");
  println(" * Press '6' to call wavesynth.setSampleRate(genWidth * genHeight / 6) ");
  println(" * Press '7' to call wavesynth.setSampleRate(this.sampleRate) ");
  println(" * Press 's' to save display to a PNG file");
  println(" * Press 'S' to save audio to a WAV file");
  println(" * Press 'h' to show this help message in the console");
}

public void stepAnimation() {
  step += 1;
  step %= animSteps;
  wavesynth.renderFrame(step);
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
  // println("--->> copied audio signal to audio buffer");
}

public void mousePressed() {
  sampleX = mouseX;
  sampleY = mouseY;
  samplePos = mapper.lookupSample(sampleX, sampleY);
  if (audioSignal == null || isBufferStale) {
    renderSignal();
    isBufferStale = false;
  }
  playSample(audioBuffer, samplePos, calcSampleLen(), 0.3f, new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime));
}
  
/**
 * Plays an audio sample.
 * 
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @param adsr         an ADSR envelope for the sample
 * @return the calculated sample length in samples
 */
public int playSample(MultiChannelBuffer buffer, int samplePos, int samplelen, float amplitude, ADSR adsr) {
  // println("--- play "+ twoPlaces.format(amplitude));
  audioSampler = new Sampler(buffer, sampleRate, 8); // create a Minim Sampler from the buffer sampleRate sampling
                             // rate, for up to 8 simultaneous outputs 
  audioSampler.amplitude.setLastValue(amplitude);    // set amplitude for the Sampler
  audioSampler.begin.setLastValue(samplePos);        // set the Sampler to begin playback at samplePos, which 
                               // corresponds to the place the mouse was clicked
  int releaseDuration = (int) (releaseTime * sampleRate); // calculate the release time.
  if (samplePos + samplelen >= mapSize) {
    samplelen = mapSize - samplePos; // make sure we don't exceed the mapSize
    println("----->>> sample length = " + samplelen);
  }
  int durationPlusRelease = this.samplelen + releaseDuration;
  int end = (samplePos + durationPlusRelease >= this.mapSize) ? this.mapSize - 1
      : samplePos + durationPlusRelease;
  // println("----->>> end = " + end);
  audioSampler.end.setLastValue(end);
  this.instrument = new WFInstrument(audioOut, audioSampler, adsr);
  // play command takes a duration in seconds
  float duration = samplelen / (float) (sampleRate);
  instrument.play(duration);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, (int) (duration * 1000) + millis()));
  // return the length of the sample
  return samplelen;
}

public int calcSampleLen() {
  float vary = (float) (PixelAudio.gauss(this.sampleScale, this.sampleScale * 0.125f)); // vary the duration of the signal 
  // println("----->>> vary = "+ vary +", sampleScale = "+ sampleScale);
  this.samplelen = (int) (vary * this.sampleBase); // calculate the duration of the sample
  return samplelen;
}
