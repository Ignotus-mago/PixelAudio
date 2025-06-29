import processing.core.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Timer;

import javax.sound.sampled.*;

import ddf.minim.*;
import ddf.minim.ugens.*;
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.curves.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;

/**
 *
 * PRESS 'd' TO START OR STOP DRAWING.
 * PRESS 'p' TO TRIGGER AUDIO FROM THE DRAWING. 
 * 
 * See TutorialOne_04_Drawing for a more recent version of code for drawing.
 * This version only permits a single brushstroke, TutorialOne_04_Drawing 
 * lets you create multiple interactive brushstrokes. 
 *
 * DrawAudioDemo shows how to load an audio file and an image separately and then use
 * the image as a control surface for the audio. Press 'o' to load either an audio or
 * an image file to both the audio buffer and the display image. Press 'O' to load either
 * type of file to just the display image or just the audio buffer.
 * 
 * DrawAudioDemo's image and audio files are part of a new media performance created by 
 * Paul Hertz, "DeadBodyWorkFlow." The typeface used in the image, IgnoBlox, is intentionally
 * difficult to read. The text and audio match closely -- when drawing is off, a click on a 
 * the first letter of a word will play that word. There are 24 words arranged in 3 columns
 * of 4 letters each. The text and audio match because of the way the signal path through 
 * the image is generated, using the loadWordGen method. Press 'k' to show a spectral color 
 * image of the signal path. Press 'K' to reload the image and audio files. 
 * 
 * An audio signal or image can be loaded to various channels of the image: Red, 
 * Green, Blue or all channels in the RGB color space or Hue or Brightness in the 
 * HSB color space (We ignore Saturation for now). 
 * 
 * You can enhance image contrast by stretching its histogram ('m' key).
 * You can make the image brighter ('=' and '+' keys) or darker ('-' or '_' key)
 * using a gamma function, a non-linear adjustment. 
 * 
 * Press ' ' to toggle animation.
 * Press 'a' to load an image or audio file to all color channels.
 * Press 'r' to load an image or audio file to the red color channel.
 * Press 'g' to load an image or audio file to the green color channel.
 * Press 'b' to load an image or audio file to the blue color channel.
 * Press 'h' to load an image or audio file to the HSB hue channel.
 * Press 'v' to load an image or audio file to the HSB saturation (vibrance) channel.
 * Press 'l' to load an image or audio file to the HSB brightness (lightness) channel.
 * Press 'o' to load a file to both audio or image.
 * Press 'O' to load a file to only image or audio.
 * Press 'm' to apply a contrast enhancement (histogram stretch) to the image.
 * Press '=' or '+' to make the image brighter
 * Press '-' or '_' to make the image darker.
 * Press 'd' to toggle drawing: drag the mouse to create a curve.
 * Press 'p' to trigger audio samples along a curve that you draw.
 * Press 's' to save to an audio file.
 * Press 'S' to save to an image file.
 * Press 'f' to show frameRate in the console.
 * Press 'w' to write the image to the audio buffer (expect noise)
 * Press '?' to show this help message.
 * 
 */

// PixelAudio vars and objects
PixelAudio pixelaudio;     // our shiny new library
HilbertGen hGen;           // a PixelMapGen to draw Hilbert curves
MultiGen multigen;         // a PixelMapGen that handles multiple gens strung together
int rows = 3;
int columns = 2;
int genWidth = 512;       // width of PixelMapGen objects: for hGen must be a power of 2
int genHeight = 512;      // height of PixelMapGen objects: for hGen must be equal to width
PixelAudioMapper mapper;  // object for reading, writing, and transcoding audio and image data
int mapSize;              // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
PImage mapImage;          // image for display
PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;
int[] colors;             // array of spectral colors
boolean isBlending = false;

/** Minim audio library */
Minim minim;              // library that handles audio
AudioOutput audioOut;     // line out to sound hardware
MultiChannelBuffer audioBuffer;    // data structure to hold audio samples
boolean isBufferStale = false;     // do we need to reset the audio buffer?
int sampleRate = 41500;   // ----->> a critical value, see the setup method <<-----
float[] audioSignal;      // the audio signal as an array
int[] rgbSignal;          // the colors in the display image, in the order the signal path visits them
int audioLength;      // length of audio buffer, audio signal, usually 32.768 seconds

// SampleInstrument setup
float sampleScale = 4;
int sampleBase;
int samplelen;
Sampler audioSampler;     // minim class for sampled sound
ArrayList<WFInstrument> octet;
WFInstrument instrument;      // local class to wrap audioSampler
int nowPlaying = 0;
int maxPlayers = 8;
// ADSR and params
ADSR adsr;                // good old attack, decay, sustain, release
float maxAmplitude1 = 0.9f;
float attackTime1 = 0.2f;
float decayTime1 = 0.125f;
float sustainLevel1 = 0.5f;
float releaseTime1 = 0.2f;
float maxAmplitude2 = 0.8f;
float attackTime2 = 0.05f;
float decayTime2 = 0.0f;
float sustainLevel2 = 0.8f;
float releaseTime2 = 0.05f;

// audio file
File audioFile;
String audioFilePath;
String audioFileName;
String audioFileTag;

// image file
File imageFile;
String imageFilePath;
String imageFileName;
String imageFileTag;

boolean isLoadImage = true;
boolean isLoadBoth = true;

// animation
boolean isAnimating = false;       // animation status
boolean oldIsAnimating;            // keep old animation status if we suspend animation
boolean isLooping = true;          // looping sample (our instrument ignores this
// interaction
int sampleX;
int sampleY;
int samplePos;            // position of a mouse click along the signal path, index into the audio array
ArrayList<TimedLocation> timeLocsArray;
int count = 0;
// histogram and gamma adjustments
int histoHigh = 240;
int histoLow = 32;
float gammaUp = 0.9f;
float gammaDown = 1.2f;
int[] gammaTable;

// curve drawing and interaction
public boolean isDrawMode = false;
public float epsilon = 12.0f;
public ArrayList<PVector> allPoints;
public int allPointsColor = color(220, 199, 212, 192);
public float allPointsWeight = 4;
public PVector currentPoint;
public int polySteps = 12;
public PACurveMaker curveMaker;
public ArrayList<PVector> eventPoints;
public ListIterator<PVector> eventPointsIter;
int eventStep = 67;   // milliseconds between events
public ArrayList<TimedLocation> pointEventsArray;

String dataPath = "/Users/paulhz/Code/Workspace/TestProcessing/src/net/paulhertz/testpixelaudio/data/";
String audioStartFile = dataPath + "workflow_t01_mix.wav";       // or workflow_t01_mix_01.mp3, smaller file
String imageStartFile = dataPath + "workFlowPanel_03.png";

boolean isWriteToScreen = true;


public static void main(String[] args) {
  PApplet.main(new String[] { DrawAudioDemo.class.getName() });
}

public void settings() {
  size(rows * genWidth, columns * genHeight);
}

public void setup() {
  frameRate(30);
  pixelaudio = new PixelAudio(this);
  minim = new Minim(this);
  // For this sketch, audio was recorded at 48KHz. 
  sampleRate = 48000; // = genWidth * genHeight;
  sampleBase = (int) (sampleRate / sampleScale);
  initAudio();
  // multigen = loadLoopGen(genWidth, genHeight);
  multigen = loadWordGen(genWidth/4, genHeight/4);
  mapper = new PixelAudioMapper(multigen);
  mapSize = mapper.getSize();
  colors = getColors(); // create an array of rainbow colors
  mapImage = createImage(width, height, ARGB); // an image to use with mapper
     currentPoint = new PVector(-1, -1);
  timeLocsArray = new ArrayList<TimedLocation>();
  octet = new ArrayList<WFInstrument>();
  loadFiles();
  showHelp();
}

public void loadFiles() {
  boolean oldIsLoadBoth = isLoadBoth;
  isLoadBoth = false;
  loadAudioFile(new File(audioStartFile));
  loadImageFile(new File(imageStartFile));
  isLoadBoth = oldIsLoadBoth;
}

public void initAudio() {
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  this.audioBuffer = new MultiChannelBuffer(1024, 1);
}


/**
 * Adds PixelMapGen objects to the genList. The genList will be used to
 * initialize a MultiGen, which in turn is passed to a WaveSynth.
 * This method provides a big looping fractal.
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
  return new MultiGen(width, height, offsetList, genList);
}

/**
 * Adds PixelMapGen objects to the genList. The genList will be used to
 * initialize a MultiGen, which in turn is passed to a WaveSynth.
 * This method follows the words in the workFlowPanel.png graphic.
 */
public MultiGen loadWordGen(int wordGenW, int wordGenH) {
  // list of PixelMapGens that create an image using mapper
  ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
  // list of x,y coordinates for placing gens from genList
  ArrayList<int[]> offsetList = new ArrayList<int[]>();     
  for (int y = 0; y < 4; y++) {
    for (int x = 0; x < 4; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 4; y < 8; y++) {
    for (int x = 0; x < 4; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 0; y < 4; y++) {
    for (int x = 4; x < 8; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 4; y < 8; y++) {
    for (int x = 4; x < 8; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 0; y < 4; y++) {
    for (int x = 8; x < 12; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 4; y < 8; y++) {
    for (int x = 8; x < 12; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  return new MultiGen(width, height, offsetList, genList);
}

public int[] getColors() {
  int[] colorWheel = new int[mapSize]; // an array for our colors
  pushStyle(); // save styles
  colorMode(HSB, colorWheel.length, 100, 100); // pop over to the HSB color space and give hue a very wide range
  for (int i = 0; i < colorWheel.length; i++) {
    colorWheel[i] = color(i, 40, 75); // fill our array with colors, gradually changing hue
  }
  popStyle(); // restore styles, including the default RGB color space
  return colorWheel;
}

public void displayColors() {
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
  mapImage.updatePixels();
}

public void writeToScreen(String msg) {
  pushStyle();
  fill(0);
  textSize(24);
  text(msg, 64, 1000);
  fill(255);
  text(msg, 66, 1001);
  popStyle();
}

public void draw() {
  // background image, erases previous display
  image(mapImage, 0, 0);
  // step the animation
  if (isAnimating)
    stepAnimation();
  // draw brush shapes
  if (isDrawMode) {
    if (mousePressed) {
      addPoint();
    }
    if (allPoints != null && allPoints.size() > 2) {
      // draw the line we're making while we drag the mouse
      PACurveUtility.lineDraw(this, allPoints, allPointsColor, allPointsWeight);
    }
    if (curveMaker != null) {
      freshDraw();
    }
    if (pointEventsArray != null) runPointEventsArray();
  } 
  runTimeArray();
  if (isWriteToScreen) writeToScreen("Press 'd' to turn drawing on (or off). Draw something. Then press 'p' to play your drawing.");
}

public void freshDraw() {
  if (curveMaker.isReady()) {
    // curveMaker.RDPDraw(this);
    // curveMaker.curveDraw(this, false);
    PABezShape brush = curveMaker.getBrushShape();
    brush.setFillColor(color(144, 34, 42, 233));
    brush.setWeight(2);
    brush.setStrokeColor(color(144, 34, 42, 233));
    brush.draw(this);
    // curveMaker.brushDraw(this, color(144, 34, 42, 233));
    curveMaker.eventPointsDraw(this, polySteps, color(233, 199, 144, 192), 6);
  }
  else {
    if (curveMaker.dragPoints != null && curveMaker.dragPoints.size() > 2) 
      curveMaker.dragPointsDraw(this);
  }
}

public void stepAnimation() {
  mapImage.loadPixels();
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
  PixelAudioMapper.rotateLeft(rgbSignal, 16);
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, mapSize);
  mapImage.updatePixels();
}

// the modern way to loop (as of Java 8)
public void runTimeArrayBack() {
  int currentTime = millis();
  timeLocsArray.forEach(tl -> {
    tl.setStale(tl.stopTime() < currentTime);
    if (!tl.isStale()) {
      drawCircle(tl.getX(), tl.getY());
    }
  });
  timeLocsArray.removeIf(TimedLocation::isStale);
}

// a more thread safe way to loop (but for now this application is single-threaded)
public void runTimeArray() {
  int currentTime = millis();
  for (Iterator<TimedLocation> iter = timeLocsArray.iterator(); iter.hasNext();) {
    TimedLocation tl = iter.next();
    tl.setStale(tl.stopTime() < currentTime);
    if (!tl.isStale()) {
      drawCircle(tl.getX(), tl.getY());
    }
  }
  timeLocsArray.removeIf(TimedLocation::isStale);    
}

public void runPointEventsArray() {
  int currentTime = millis();
  pointEventsArray.forEach(tl -> {
    if (tl.stopTime() < currentTime) {
      // the curves may exceed display bounds, so we have to constrain values
      sampleX = PixelAudio.constrain(Math.round(tl.getX()), 0, width-1);
      sampleY = PixelAudio.constrain(Math.round(tl.getY()), 0, height-1);
      int pos = mapper.lookupSample(sampleX, sampleY);
      playSample(pos);
      tl.setStale(true);
    }
    else {
      // pointEventsArray is sorted by time and 
      // we can ignore events set in the future
      return;
    }
  });
  pointEventsArray.removeIf(TimedLocation::isStale);
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
    break;
  case 'a':
    chan = PixelAudioMapper.ChannelNames.ALL;
    chooseFile();
    break;
  case 'r':
    chan = PixelAudioMapper.ChannelNames.R;
    chooseFile();
    break;
  case 'g':
    chan = PixelAudioMapper.ChannelNames.G;
    chooseFile();
    break;
  case 'b':
    chan = PixelAudioMapper.ChannelNames.B;
    chooseFile();
    break;
  case 'h':
    chan = PixelAudioMapper.ChannelNames.H;
    chooseFile();
    break;
  case 'v':
    chan = PixelAudioMapper.ChannelNames.S;
    chooseFile();
    break;
  case 'l':
    chan = PixelAudioMapper.ChannelNames.L;
    chooseFile();
    break;
  case 'j':
    isBlending = !isBlending;
    println("-- isBlending is "+ isBlending);
    break;
  case 'O':
    isLoadBoth = false;
    chooseFile();
    break;
  case 'o':
    isLoadBoth = true;
    chooseFile();
    break;
  case 'm':
    mapImage.loadPixels();
    int[] bounds = getHistoBounds(mapImage.pixels);
    mapImage.pixels = stretch(mapImage.pixels, bounds[0], bounds[1]);
    mapImage.updatePixels();
    break;
  case 'd': case'D':
    isWriteToScreen = false;
    isDrawMode = !isDrawMode;
    String msg = isDrawMode ? "Screen drawing is on. Drag the mouse to draw to the screen." : "Screen drawing is off.";
    println("-- "+ msg +" --");
    break;
  case 'p': case 'P':
    if (curveMaker.isReady()) {
      eventPoints = curveMaker.getCurveShape().getPointList(polySteps);
      playPoints();
    }
  break;
  case '=':
  case '+':
    setGamma(gammaUp);
    mapImage.loadPixels();
    mapImage.pixels = adjustGamma(mapImage.pixels);
    mapImage.updatePixels();
    break;
  case '-':
  case '_':
    setGamma(gammaDown);
    mapImage.loadPixels();
    mapImage.pixels = adjustGamma(mapImage.pixels);
    mapImage.updatePixels();
    break;
  case 's':
    saveToAudio();
    break;
  case 'S':
    saveToImage();
    break;
  case 'f':
    println("--->> frame rate: " + frameRate);
    break;
  case 'w':
    writeImageToAudio();
    println("--->> Wrote image to audio as audio data.");
    break;
  case 'W':
    writeAudioToImage();
    println("--->> Wrote audio to image as pixel data.");
    break;
  case 'k':
    displayColors();
    break;
  case 'K':
    loadFiles();
    break;
  case '?':
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press ' ' to toggle animation.");
  println(" * Press 'a' to load an image or audio file to all color channels.");
  println(" * Press 'r' to load an image or audio file to the red color channel.");
  println(" * Press 'g' to load an image or audio file to the green color channel.");
  println(" * Press 'b' to load an image or audio file to the blue color channel.");
  println(" * Press 'h' to load an image or audio file to the HSB hue channel.");
  println(" * Press 'v' to load an image or audio file to the HSB saturation (vibrance) channel.");
  println(" * Press 'l' to load an image or audio file to the HSB brightness (lightness) channel.");
  println(" * Press 'o' to load a file to both audio or image.");
  println(" * Press 'O' to load a file to only image or audio.");
  println(" * Press 'm' to apply a contrast enhancement (histogram stretch) to the image.");
  println(" * Press '=' or '+' to make the image brighter");
  println(" * Press '-' or '_' to make the image darker.");
  println(" * Press 'd' to toggle drawing: drag the mouse to create a curve.");
  println(" * Press 'p' to trigger audio samples along a curve that you draw.");
  println(" * Press 's' to save to an audio file.");
  println(" * Press 'S' to save to an image file.");
  println(" * Press 'f' to show frameRate in the console.");
  println(" * Press 'w' to write the image to the audio buffer (expect noise)");
  println(" * Press 'k' to display a color spectrum along the signal path.");
  println(" * Press 'K' to load audioStartFile and imageStartFile.");
  println(" * Press '?' to show this help message.");    
}

public void mousePressed() {
  if (this.isDrawMode) {
    initAllPoints();
  } 
  else {
    handleMousePressed();
  }
}

/**
 * Initializes allPoints and adds the current mouse location to it. 
 */
public void initAllPoints() {
  allPoints = new ArrayList<PVector>();
  addPoint();
  sampleX = mouseX;
  sampleY = mouseY;
  samplePos = mapper.lookupSample(sampleX, sampleY);      
}

public void handleMousePressed() {
  // a point event was triggered
  sampleX = mouseX;
  sampleY = mouseY;
  samplePos = mapper.lookupSample(sampleX, sampleY);
  if (audioSignal == null || isBufferStale) {
    isBufferStale = false;
  }
  playSample(samplePos);
}

public void addPoint() {
  if (mouseX != currentPoint.x || mouseY != currentPoint.y) {
    currentPoint = new PVector(mouseX, mouseY);
    allPoints.add(currentPoint);
  }
}

public void mouseReleased() {
  if (allPoints != null) {
    if (isDrawMode && allPoints.size() > 2) {
      initCurveMaker();
    } 
    else {
      handleMousePressed();
    }
    allPoints.clear();
  }
}

public void initCurveMaker() {
  curveMaker = PACurveMaker.buildCurveMakerComplete(allPoints, epsilon);
}

public void playPoints() {
  if (eventPoints != null) {
    this.timeLocsArray.clear();
    eventPointsIter = eventPoints.listIterator();
    int startTime = millis();
    println("building pointsTimer: "+ startTime);
    pointEventsArray = new ArrayList<TimedLocation>();
    startTime += 50;
    int i = 0;
    while(eventPointsIter.hasNext()) {
      PVector loc = eventPointsIter.next();
      pointEventsArray.add(new TimedLocation(Math.round(loc.x), Math.round(loc.y), startTime + i++ * eventStep));
    }
  }
}

public int playSample(int samplePos) {
  float relTime = isDrawMode ? releaseTime2 : releaseTime1;
  audioSampler = new Sampler(audioBuffer, sampleRate, 8); // create a Minim Sampler 
  audioSampler.amplitude.setLastValue(0.9f); // set amplitude for the Sampler
  audioSampler.begin.setLastValue(samplePos); // set the Sampler to begin playback at samplePos
  int releaseDuration = (int) (relTime * sampleRate); // do some calculation to include the release time.
  float vary = (float) (PixelAudio.gauss(this.sampleScale, this.sampleScale * 0.125f)); // vary duration
  // println("----->>> vary = "+ vary +", sampleScale = "+ sampleScale);
  this.samplelen = (int) (vary * this.sampleBase); // calculate the duration of the sample
  if (isDrawMode) samplelen = samplelen / 4;
  if (samplePos + samplelen >= mapSize) {
    samplelen = mapSize - samplePos; // make sure we don't exceed the mapSize
    println("----->>> sample length = " + samplelen);
  }
  int durationPlusRelease = this.samplelen + releaseDuration;
  int end = (samplePos + durationPlusRelease >= this.mapSize) ? this.mapSize - 1
      : samplePos + durationPlusRelease;
  // println("----->>> end = " + end);
  audioSampler.end.setLastValue(end);
  // ADSR envelope 
  adsr = isDrawMode ? new ADSR(maxAmplitude2, attackTime2, decayTime2, sustainLevel2, releaseTime2) 
      : new ADSR(maxAmplitude1, attackTime1, decayTime1, sustainLevel1, releaseTime1);
  if (octet.size() == 0) {
    loadOctet(audioSampler, adsr);
  }
  instrument = octet.get(nowPlaying);
  // println("--->> nowPlaying instrument "+ nowPlaying);
  instrument.setSampler(audioSampler);
  instrument.setADSR(adsr);
  instrument.getSampler().patch(adsr);
  nowPlaying = (nowPlaying + 1) % maxPlayers;
  // play command takes a duration in seconds
  float duration = samplelen / (float) (sampleRate);
  instrument.play(duration);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, (int) (duration * 1000) + millis()));
  // return the length of the sample
  return samplelen;
}

public void loadOctet(Sampler sampler, ADSR adsr) {
  for (int i = 0; i < maxPlayers; i++) {
    octet.add(new WFInstrument(audioOut, sampler, adsr));
  }
  nowPlaying = 0;
}


// ------------- HISTOGRAM AND GAMMA ADJUSTMENTS ------------- // 
  
public int[] getHistoBounds(int[] source) {
    int min = 255;
    int max = 0;
    for (int i = 0; i < source.length; i++) {
      int[] comp = PixelAudioMapper.rgbComponents(source[i]);
      for (int j = 0; j < comp.length; j++) {
        if (comp[j] > max) max = comp[j];
        if (comp[j] < min) min = comp[j];
      }
    }
    println("--- min", min, " max ", max);
    return new int[]{min, max};
}

// histogram stretch -- run getHistoBounds to determine low and high
public int[] stretch(int[] source, int low, int high) {
  int[] out = new int[source.length];
  int r = 0, g = 0, b = 0;
  for (int i = 0; i < out.length; i++) {
    int[] comp = PixelAudioMapper.rgbComponents(source[i]);
    r = comp[0];
    g = comp[1];
    b = comp[2];
    r = (int) constrain(map(r, low, high, 1, 254), 0, 255);
    g = (int) constrain(map(g, low, high, 1, 254), 0, 255);
    b = (int) constrain(map(b, low, high, 1, 254), 0, 255);
    out[i] = PixelAudioMapper.composeColor(r, g, b, 255);
  }
  return out;
}

public void setGamma(float gamma) {
  if (gamma != 1.0) {
    this.gammaTable = new int[256];
    for (int i = 0; i < gammaTable.length; i++) {
      float c = i/(float)(gammaTable.length - 1);
      gammaTable[i] = (int) Math.round(Math.pow(c, gamma) * (gammaTable.length - 1));
    }
  }
}

public int[] adjustGamma(int[] source) {
  int[] out = new int[source.length];
  int r = 0, g = 0, b = 0;
  for (int i = 0; i < out.length; i++) {
    int[] comp = PixelAudioMapper.rgbComponents(source[i]);
    r = comp[0];
    g = comp[1];
    b = comp[2];
    r = gammaTable[r];
    g = gammaTable[g];
    b = gammaTable[b];
    out[i] = PixelAudioMapper.composeColor(r, g, b, 255);
  }
  return out;
}  
  
