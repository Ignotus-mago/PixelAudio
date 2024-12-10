import java.awt.Toolkit;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;

import javax.sound.sampled.*;

import ddf.minim.*;
import ddf.minim.analysis.*;
import ddf.minim.ugens.*;
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;
import net.paulhertz.pixelaudio.curves.PABezShape;
import net.paulhertz.pixelaudio.curves.PACurveMaker;
import net.paulhertz.pixelaudio.curves.PACurveUtility;


/**
 * 
 * TO GET STARTED
 * Launch the Max patch. 
 * Launch WorkFlowApp3.
 * Click on image to trigger singles voices. Coordinates and sample offset are reported in the Max UDPHandler window.
 * Type 'd' to turn on drawing. Drag the mouse to make "brushstrokes." Sound will play in Processing and data will 
 * show in the Max subpatcher network_receiver.
 * 
 * Changes: Synchronized signatures on runPointEvents() and runCurveEvents().
 * New synchronized method storeCurveTL() called by playPoints() methods.
 * These steps seem to have eliminated the concurrent modification errors caused by calls from Max.
 * I also changed some params: epsilon = 4 (was 12) makes tighter reduced curve points, 
 * while polySteps = 8 (was 12) reduces number of points in polygonized Bezier curves. 
 * Calling audioSampler.amplitude.setLastValue(0.6f) with 0.6 instead of 0.9 seems to reduce clipping.
 * 
 * 
 * 
 * 
 * Development version 3 of Aria / DeadBodyWorkFlow / LoseGainNurtureSustain performance software.
 * Setup for "Aria" only. "// @ //" flags variables that might change for different performances. 
 * 
 * Press ' ' to toggle animation.
 * Press 'a' to set current color channel to all color channels.
 * Press 'r' to set current color channel to the red color channel.
 * Press 'g' to set current color channel to the green color channel.
 * Press 'b' to set current color channel to the blue color channel.
 * Press 'h' to set current color channel to the HSB hue channel.
 * Press 'v' to set current color channel to the HSB saturation (vibrance) channel.
 * Press 'l' to set current color channel to the HSB brightness (lightness) channel.
 * Press 'o' to load a file to only audio or image.
 * Press 'O' to load a file to both image or audio.
 * Press 'm' to apply a contrast enhancement (histogram stretch) to the image.
 * Press '=' or '+' to make the image brighter
 * Press '-' or '_' to make the image darker.
 * Press 'd' to toggle drawing: drag the mouse to create a curve.
 * Press 'p' to trigger audio samples along a curve that you draw.
 * Press 's' to save to an audio file.
 * Press 'S' to save to an image file.
 * Press 'f' to show frameRate in the console.
 * Press 'w' to write the image to the audio buffer (expect noise)
 * Press 'W' to write the audio buffer to the image
 * Press '?' to show this help message.
 * 
 */

// PixelAudio vars and objects
PixelAudio pixelaudio;     // our shiny new library
HilbertGen hGen;           // a PixelMapGen to draw Hilbert curves
MultiGen multigen;         // a PixelMapGen that handles multiple gens
int rows = 3;
int columns = 2;
int genWidth = 512;       // width of PixelMapGen objects, for hGen must be a power of 2
int genHeight = 512;      // height of PixelMapGen objects, for hGen must be equal to width
PixelAudioMapper mapper;  // object for reading, writing, and transcoding audio and image data
int mapSize;              // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
PImage mapImage;          // image for display
PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;
int[] colors;             // array of spectral colors
boolean isBlending = false;

/** Minim audio library */
Minim minim;              // library that handles audio
AudioOutput audioOut;     // line out to sound hardware
AudioInput  audioIn;      // @ //
AudioPlayer anthem;        // @ //
MultiChannelBuffer audioBuffer;    // data structure to hold audio samples
boolean isBufferStale = false;     // do we need to reset the audio buffer?
int sampleRate = 48000;   // ----->> a critical value, see the setup method <<-----
float[] audioSignal;      // the audio signal as an array
int[] rgbSignal;          // the colors in the display image, in the order the signal path visits them
int audioLength;

// SampleInstrument setup
float sampleScale = 4;
int sampleBase = sampleRate / 4;
int samplelen = (int) (sampleScale * sampleBase);
Sampler audioSampler;     // minim class for sampled sound
ArrayList<SamplerInstrument> octet;
ArrayList<MultiChannelBuffer> voiceList;
SamplerInstrument instrument;      // local class to wrap audioSampler
int nowPlaying = 0;
int maxPlayers = 8;
// ADSR and params
ADSR adsr;                // good old attack, decay, sustain, release
PAEnvelope longEnv = new PAEnvelope(0.9f, 0.2f, 0.125f, 0.5f, 0.2f);  // @ //
PAEnvelope shortEnv = new PAEnvelope(0.8f, 0.05f, 0.0f, 0.8f, 0.05f);  // @ //
boolean isDetuned = true;
float detuning = sampleRate/24.0f;

// for capturing an audio stream, only works for a file stream in Eclipse

StreamCapture  streamCap;    // @ //
boolean  isListening = false;  // @ //

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
boolean isLoadBoth = false;

// animation
int animSteps = 64;
boolean isAnimating = false;       // animation status
boolean oldIsAnimating;            // keep old animation status if we suspend animation
boolean isLooping = true;          // looping sample (our instrument ignores this
boolean isAnimateAudio = false;
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
public float epsilon = 4.0f;            // @ //
public ArrayList<PVector> allPoints;
public int dragColor = color(233, 199, 89, 128);  // @ //
public float dragWeight = 8.0f;            // @ //
public int startTime;
public ArrayList<Integer> allTimes;
public PVector currentPoint;
public int polySteps = 8;              // @ //
public PACurveMaker curveMaker;
public ArrayList<PVector> eventPoints;
public ListIterator<PVector> eventPointsIter;
int eventStep = 90;   // milliseconds between events
public ArrayList<TimedLocation> curveTLEvents;
public ArrayList<PACurveMaker> brushShapesList;
public PACurveMaker activeBrush;
public int activeIndex = 0;
int newBrushColor = color(144, 34, 42, 233);    // @ //
int polyPointsColor = color(233, 199, 144, 192);  // @ //
int activeBrushColor = color(144, 89, 55, 233);    // @ //
int readyBrushColor = color(34, 89, 55, 233);    // @ //
boolean isDrawWeighted = false;
float curveWeight = (float) PABezShape.LAMBDA;

// FFT
PAFFTHandler fftHandler;
boolean isUseFFT = false;
ArrayList<PAFFTHandler.Formant> formants;
PAFFTHandler.Formant currentFormant;
int formantIndex = 0;
boolean isRaining = false;
boolean isFormantRain = true;
boolean isSampleRain = false;
boolean isRunFastFFT = true;
// @ //
int[] zoneNums = {2, 4, 8, 10,   1, 10, 4, 10,   14, 11, 10, 11,   10, 12, 0, 11,   0, 15, 10, 11,   1, 8, 11, 11};

// network communications
NetworkDelegate nd;
boolean isUseNetworkDelegate = true;  // @ //


String dataPath;  // @ //
String audioStartFile = dataPath + "Aria_1.wav";    // @ //
String imageStartFile = dataPath + "Aria_color.png";  // @ //

// ------------- APPLICATION SWITCHES ------------- //
// you can create your own switches for other 
// performances or just edit variables in this app

public static final boolean isAria = true;

// ------------------------------------------------ //


public void settings() {
  size(rows * genWidth, columns * genHeight);
}

public void setup() {
  frameRate(30);
  pixelaudio = new PixelAudio(this);
  minim = new Minim(this);
  sampleRate = 48000; // we've recorded our audio at 48KHz
  sampleBase = sampleRate / 4;
  // path to the folder where PixelAudio examples keep their data files 
  // such as image, audio, .json, etc.
  dataPath = sketchPath("") + "../examples_data/";
  println("----->>> dataPath: ", dataPath);
  initAudio();
  initMapper();
  // use an application switch to select method to initialize performance settings
  if (isAria) {
    setAriaVariables();
  }
  // load files needed on startup, for display and audio in particular
  loadFiles();
  currentPoint = new PVector(-1, -1);                // prepare to track drawing
  timeLocsArray = new ArrayList<TimedLocation>();    // event scheduling list
  octet = new ArrayList<SamplerInstrument>();        // eight sampling instruments 
  voiceList = new ArrayList<MultiChannelBuffer>();   // audio buffer
  brushShapesList = new ArrayList<PACurveMaker>();   // list of brush shapes
  fftHandler = new PAFFTHandler(sampleRate, 1024, true);  // FFT handler
  formants = fftHandler.loadVowels();                // formant filters
  currentFormant = formants.get(formantIndex);
  if (nd != null) nd.oscSendClear();                 // notify Max to clear old data
}

public void setAriaVariables() {
  audioStartFile = dataPath + "8vb_01.wav";
  imageStartFile = dataPath + "Aria_color.png";
  isUseNetworkDelegate = false;          // are we using a UDP delegate?
  newBrushColor = color(199, 47, 55, 233);      // @ //
  polyPointsColor = color(233, 55, 55, 192);    // @ //
  activeBrushColor = color(55, 178, 199, 233);  // @ //
  readyBrushColor = color(34, 55, 89, 233);     // @ //
  epsilon = 8;
  polySteps = 5;
  zoneNums = new int[]{2, 4, 8, 10,   1, 10, 4, 10,   14, 11, 10, 11,   10, 12, 0, 11,   0, 15, 10, 11,   1, 8, 11, 11};
  this.loadMediaFiles();
  println();
  if (isUseNetworkDelegate) {
    // In the Max patcher "UDP_Handler.maxpat" you'll need to enter 
    // the IP address of the machine this app is running on. 
    // "127.0.0.1" is the localhost address, used for local testing.
    String remoteAddress = "127.0.0.1";
    nd = new NetworkDelegate(this, remoteAddress, remoteAddress, 7401, 7400);
  }
}
  
/**
 * @param anthemPath  complete path to an audio file for streaming
 */
public void initAudioIn(String anthemPath) {
  this.audioIn = minim.getLineIn(Minim.MONO);    // @ //
  this.streamCap = new StreamCapture();          // @ //
  this.anthem = minim.loadFile(anthemPath);
  anthem.loop();
  audioIn.disableMonitoring();
  anthem.pause();    
}

public void initAudio() {
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  this.audioBuffer = new MultiChannelBuffer(1024, 1);
}

public void initMapper() {
  multigen = loadLoopGen(genWidth, genHeight);    // @ //
  mapper = new PixelAudioMapper(multigen);
  mapSize = mapper.getSize();
  colors = getColors(mapSize); // create an array of rainbow colors, useful for visualizing the signal path
  mapImage = createImage(width, height, ARGB); // an image to use with mapper
  displayColors(false);
}

public void loadFiles() {
  boolean oldIsLoadBoth = isLoadBoth;
  isLoadBoth = false;
  loadAudioFile(new File(audioStartFile));
  loadImageFile(new File(imageStartFile));
  isLoadBoth = oldIsLoadBoth;
}


/**
 * This method provides a big looping fractal consisting of 6 Hilbert curves.
 */
public MultiGen loadLoopGen(int loopGenW, int loopGenH) {
  // list of PixelMapGens that create an image using mapper
  ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
  // list of x,y coordinates for placing gens from genList
  ArrayList<int[]> offsetList = new ArrayList<int[]>();     
  genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.FLIPX90));
  offsetList.add(new int[] { 0, 0 });
  genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.NADA));
  offsetList.add(new int[] { loopGenW, 0 });
  genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.FLIPX90CCW));
  offsetList.add(new int[] { 2 * loopGenW, 0 });
  genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.FLIPX90CCW));
  offsetList.add(new int[] { 2 * loopGenW, loopGenH });
  genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.ROT180));
  offsetList.add(new int[] { loopGenW, loopGenH });
  genList.add(new HilbertGen(loopGenW, loopGenH, AffineTransformType.FLIPX90));
  offsetList.add(new int[] { 0, loopGenH });
  return new MultiGen(width, height, offsetList, genList);
}

/**
 * This method provides a big looping fractal with 24 rows of 4 128 x 128 pixel Hilbert curves
 * arranged in 3 stacks of eight. It was used for Paul Hertz's composition DeadBodyWorkFlow.
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

public int[] getColors(int size) {
  int[] colorWheel = new int[size]; // an array for our colors
  pushStyle(); // save styles
  colorMode(HSB, colorWheel.length, 100, 100); // pop over to the HSB color space and give hue a very wide range
  for (int i = 0; i < colorWheel.length; i++) {
    colorWheel[i] = color(i, 40, 75); // fill our array with colors, gradually changing hue
  }
  popStyle(); // restore styles, including the default RGB color space
  return colorWheel;
}

public void displayColors(boolean isWithAudio) {
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize, chan); // load colors to mapImage following signal path
  mapImage.updatePixels();
  if (isWithAudio) {
    ChannelNames oldChan = chan;
    chan = ChannelNames.L;
    writeAudioToImage();
    chan = oldChan;
  }
}

/**
 * Displays a line of text to the screen, usually in the draw loop. Handy for debugging.
 * typical call: writeToScreen("When does the mind stop and the world begin?", 64, 1000, 24, true);
 * 
 * @param msg    message to write
 * @param x      x coordinate
 * @param y      y coordinate
 * @param weight  font weight
 * @param isWhite  if true, white text with black drop shadow, otherwise, black text with white drop shadow
 */
public void writeToScreen(String msg, int x, int y, int weight, boolean isWhite) {
  int fill1 = isWhite? 0 : 255;
  int fill2 = isWhite? 255 : 0;
  pushStyle();
  fill(fill1);
  textSize(weight);
  text(msg, x, y);
  fill(fill2);
  text(msg, x + 2, y + 1);
  popStyle();
}

public void draw() {
  // background image
  image(mapImage, 0, 0);
  // step the animation, if we're animating
  if (isAnimating) stepAnimation(animSteps);
  // draw current brushShapes 
  drawBrushShapes();
  if (isDrawMode) {
    if (mousePressed) {
      addPoint();
    }
    if (allPoints != null && allPoints.size() > 2) {
      PACurveUtility.lineDraw(this, allPoints, dragColor, dragWeight);
    }
    if (curveMaker != null) curveMakerDraw();
  } 
  runCurveEvents();
  runPointEvents();
  if (isRaining) doRain();
  if (isListening && audioIn != null) {
    drawSignal();
  }
}

public void stepAnimation(int animSteps) {
  mapImage.loadPixels();
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
  PixelAudioMapper.rotateLeft(rgbSignal, animSteps);
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, mapSize);
  mapImage.updatePixels();
  if (isAnimateAudio) {
    audioSignal = audioBuffer.getChannel(0);
    PixelAudioMapper.rotateLeft(audioSignal, animSteps);
    audioBuffer.setChannel(0, audioSignal);
  }
}

public void drawBrushShapes() {
  if (this.brushShapesList.size() > 0) {
    int idx = 0;
    activeBrush = null;
    for (PACurveMaker bd : brushShapesList) {
      int brushFill = readyBrushColor;
      if (mouseInPoly(bd.getBrushPoly())) {
        brushFill = activeBrushColor;
        activeBrush = bd;
        activeIndex = idx;
      }
      PACurveUtility.shapeDraw(this, bd.getBrushShape(), brushFill, brushFill, 2);
      idx++;
    }
  }
}

public void curveMakerDraw() {
  if (curveMaker.isReady()) {
    curveMaker.brushDraw(this, newBrushColor, newBrushColor, 2);
    // curveMaker.brushDrawDirect(this);
    curveMaker.eventPointsDraw(this);
  }
}

public void doRain() {
  int sampleLength = 256 * 256;
  int signalPos = (int) random(sampleLength, mapSize - sampleLength - 1);
  if (isFormantRain) {
    formantRaindrops(sampleLength, true, signalPos);
  }
  if (isSampleRain) {
    playSample(signalPos);
  }
}

public boolean mouseInPoly(ArrayList<PVector> poly) {
  return PABezShape.pointInPoly(poly, mouseX, mouseY);
}

// handle single clicks
public synchronized void runPointEvents() {
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

// handle curves drawn on screen
public synchronized void runCurveEvents() {
  // if the event list is null or empty, skip out
  if (curveTLEvents != null && curveTLEvents.size() > 0) {
    int currentTime = millis();
    curveTLEvents.forEach(tl -> {
      if (tl.stopTime() < currentTime) {
        // the curves may exceed display bounds, so we have to constrain values
        sampleX = PixelAudio.constrain(Math.round(tl.getX()), 0, width - 1);
        sampleY = PixelAudio.constrain(Math.round(tl.getY()), 0, height - 1);
        int pos = mapper.lookupSample(sampleX, sampleY);
        playSample(pos);
        tl.setStale(true);
        // println("----- ");
      } 
      else {
        // pointEventsArray is sorted by time, so ignore events still in the future
        return;
      }
    });
    curveTLEvents.removeIf(TimedLocation::isStale);
  }
}

public void drawCircle(int x, int y) {
  fill(color(233, 220, 199, 128));
  noStroke();
  circle(x, y, 60);
}  

public void keyPressed() {
  parseKey(key, keyCode);
}

/**
 * Detects Caps Lock state. We use Caps Lock state to switch between audio and graphics command sets. 
 * @return true if Caps Lock is down, false otherwise.
 */
public boolean isCapsLockDown() {
  boolean isDown = Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK);
  return isDown;
}

/**
 * Called by keyPressed() to process key presses, can also be called by local methods to 
 * execute a command or called over a UDP connection via NetworkDelegate.
 * 
 * Required by the PANetworkCLientINF interface
 */
public void parseKey(char key, int keyCode) {
  switch (key) {
  case ' ':
    isAnimating = !isAnimating;
    if (isAnimating && isCapsLockDown()) {
      isAnimateAudio = true;
    }
    break;
  case 'a':
    chan = PixelAudioMapper.ChannelNames.ALL;
    println("-- color channel is "+ chan.name());
    //chooseFile();
    break;
  case 'r':
    chan = PixelAudioMapper.ChannelNames.R;
    println("-- color channel is "+ chan.name());
    //chooseFile();
    break;
  case 'g':
    chan = PixelAudioMapper.ChannelNames.G;
    println("-- color channel is "+ chan.name());
    //chooseFile();
    break;
  case 'b':
    chan = PixelAudioMapper.ChannelNames.B;
    println("-- color channel is "+ chan.name());
    //chooseFile();
    break;
  case 'h':
    chan = PixelAudioMapper.ChannelNames.H;
    println("-- color channel is "+ chan.name());
    //chooseFile();
    break;
  case 'v':
    chan = PixelAudioMapper.ChannelNames.S;
    println("-- color channel is "+ chan.name());
    //chooseFile();
    break;
  case 'l':
    chan = PixelAudioMapper.ChannelNames.L;
    println("-- color channel is "+ chan.name());
    //chooseFile();
    break;
  case 'j':
    isBlending = !isBlending;
    println("-- isBlending is "+ isBlending);
    break;
  case 'o':
    isLoadBoth = false;
    chooseFile();
    break;
  case 'O':
    isLoadBoth = true;
    chooseFile();
    break;
  case 'p':
    displayColors(false);
    break;
  case 'P':
    displayColors(true);
    break;
  case 'm':
    mapImage.loadPixels();
    int[] bounds = getHistoBounds(mapImage.pixels);
    mapImage.pixels = stretch(mapImage.pixels, bounds[0], bounds[1]);
    mapImage.updatePixels();
    break;
  case 'n':
    audioSignal = audioBuffer.getChannel(0);
    normalize(audioSignal, 1.0f);
    audioBuffer.setChannel(0, audioSignal);
    break;
  case 'd': case'D':
    isDrawMode = !isDrawMode;
    if (!isDrawMode) {
      // we used to set curveMaker = null. Not a good idea, since it is now a storage option.
      // anyhow, we create a curveMaker on a mousePressed event only when isDrawMode is true
    }
    String msg = isDrawMode ? "Screen drawing is on. Drag the mouse to draw to the screen." : "Screen drawing is off.";
    println("-- "+ msg +" --");
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
    saveToImage();
    break;
  case 'S':
    saveToAudio();
    break;
  case 'f':
    // println("--->> frame rate: " + frameRate);
    if (imageFile != null) loadImageFile(imageFile);
    break;
  case 'F':
    if (audioFile != null) loadAudioFile(audioFile);
    break;
  case 't':
    isUseFFT = !isUseFFT;
    println("--->> isUseFFT: " + isUseFFT);
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
    loadImageFile(new File(imageStartFile));
    break;
  case 'K':
    loadAudioFile(new File(audioStartFile));
    break;
  case 'x':
    if (brushShapesList != null) {
      // remove the oldest addition
      if (!brushShapesList.isEmpty()) {
        brushShapesList.remove(0);    // brushShapes array starts at 0
        if (nd != null) nd.oscSendDelete(1);       // Mac coll object index starts at 1
      }
    }
    break;
  case 'X':
    if (brushShapesList != null) {
      // remove the most recent addition
      if (!brushShapesList.isEmpty()) {
        int idx = brushShapesList.size();
        brushShapesList.remove(idx - 1);  // brushShapes array starts at 0
        if (nd != null) nd.oscSendDelete(idx);        // Mac coll object index starts at 1
      }
    }
    break;
  case ',':
    formantIndex = (formantIndex - 1 <= 0) ? formants.size() - 1 : formantIndex - 1;
    currentFormant = formants.get(formantIndex);
    println("----->>> "+ formantIndex +": "+ currentFormant.toString());
    break;
  case '.':
    formantIndex = (formantIndex + 1 >= formants.size()) ? 0 : formantIndex + 1;
    currentFormant = formants.get(formantIndex);;;
    println("----->>> "+ formantIndex +": "+ currentFormant.toString());
    break;
  case ';':
    isRaining = !isRaining;
    isFormantRain = isRaining;
    println("----->>> formant rain is "+ isRaining);
    break;
  case 'z':
    if (isCapsLockDown()) {
      initAudio();
    }
    else {
      reset(false);
    }
    break;
  case 'Z':
    reset(true);
    break;
  case 'I':
    audioSignal = audioBuffer.getChannel(0);
    PixelAudioMapper.reverseArray(audioSignal, 0, audioSignal.length-1);
    audioBuffer.setChannel(0, audioSignal);
    break;
  case ']':
    if (cosmicList != null) {
      stepFileList(cosmicList, cosmicPtr);
      cosmicPtr = (cosmicPtr + 1) % cosmicList.length;
    }
    break;
  case '[':
    if (cosmicList != null) {
      stepFileList(cosmicList, cosmicPtr);
      cosmicPtr = (cosmicPtr == 0) ? cosmicList.length - 1 : cosmicPtr - 1;
    }
    break;
  case '}':
    if (sonicList != null) {
      stepFileList(sonicList, sonicPtr);
      sonicPtr = (sonicPtr + 1) % sonicList.length;
      if (isAria) displayColors(true);
    }
    break;
  case '{':
    if (sonicList != null) {
      stepFileList(sonicList, sonicPtr);
      sonicPtr = (sonicPtr == 0) ? sonicList.length - 1 : sonicPtr - 1;
      if (isAria) displayColors(true);
    }
    break;
  case '\\':
    if (audioIn != null) {
      if (!isListening) {
        // stepFileList(cosmicList, cosmicPtr);
        // cosmicPtr = (cosmicPtr + 1) % cosmicList.length;
      }
      toggleListening();
      println("----->>> isListening is "+ isListening);
    }
    break;
  case '!':
    if (fotoList != null) {
      stepFileList(fotoList, fotoPtr);
      fotoPtr = (fotoPtr + 1) % fotoList.length;
    }
    break;
  case '/':
    chooseFolder();
    break;
  case '*':
    isDrawWeighted = !isDrawWeighted;
    println("--->> isDrawWeighted is "+ isDrawWeighted);
    break;
  case '?':
    showHelp();
    break;
  default:
    break;
  }
}

// required by the PANetworkCLientINF interface
public void controlMsg(String control, float val) {
  if (control.equals("detune")) {
    isDetuned = !isDetuned;
  }
}


public void showHelp() {
  println(" * Press ' ' to toggle animation.");
  println(" * Press 'a' to set current color channel to all color channels.");
  println(" * Press 'r' to set current color channel to the red color channel.");
  println(" * Press 'g' to set current color channel to the green color channel.");
  println(" * Press 'b' to set current color channel to the blue color channel.");
  println(" * Press 'h' to set current color channel to the HSB hue channel.");
  println(" * Press 'v' to set current color channel to the HSB saturation (vibrance) channel.");
  println(" * Press 'l' to set current color channel to the HSB brightness (lightness) channel.");
  println(" * Press 'o' to load a file to only audio or image.");
  println(" * Press 'O' to load a file to both image or audio.");
  println(" * Press 'm' to apply a contrast enhancement (histogram stretch) to the image.");
  println(" * Press '=' or '+' to make the image brighter");
  println(" * Press '-' or '_' to make the image darker.");
  println(" * Press 'd' to toggle drawing: drag the mouse to create a curve.");
  println(" * Press 'p' to trigger audio samples along a curve that you draw.");
  println(" * Press 's' to save to an audio file.");
  println(" * Press 'S' to save to an image file.");
  println(" * Press 'f' to show frameRate in the console.");
  println(" * Press 'w' to write the image to the audio buffer (expect noise)");
  println(" * Press 'W' to write the audio buffer to the image");
  println(" * Press '?' to show this help message.");
}


/**
 * If isDrawMode is true we call initAllPoints() to start drawing by accumulating 
 * PVectors into allPoints. Otherwise, we call handleMousePressed(). 
 * 
 * Drawing consists of accumulating PVectors representing points to the allPoints list
 * and drawing the points in the list as they accumulate. Drawing begins with a mousePressed event
 * continues while mouseDown is true in the draw() method, and ends with a mouseReleased event.
 */
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
 * If NetworkDelegate nd is not null, we post a UDP message. 
 */
public void initAllPoints() {
  allPoints = new ArrayList<PVector>();
  allTimes = new ArrayList<Integer>();
  startTime = millis();
  allTimes.add(startTime);
  addPoint();
  sampleX = mouseX;
  sampleY = mouseY;
  samplePos = mapper.lookupSample(sampleX, sampleY);      
  if (nd != null) nd.oscSendMousePressed(sampleX, sampleY, samplePos);
}

/**
 * If there is an active brush, we're here because it was triggered, so
 * play the multiple audio samples associated with the brush. Otherwise, 
 * play single audio sample determined by the mouse location. 
 */
public void handleMousePressed() {
  if (activeBrush != null) {
    // a brushShape was triggered
    eventPoints = activeBrush.getEventPoints();
    playPoints(eventPoints);
    if (nd != null) nd.oscSendTrig(activeIndex + 1);
    activeBrush = null;
  } 
  else {
    // a point event was triggered
    sampleX = mouseX;
    sampleY = mouseY;
    samplePos = mapper.lookupSample(sampleX, sampleY);
    if (audioSignal == null || isBufferStale) {
      isBufferStale = false;
    }
    playSample(samplePos);
    if (nd != null) nd.oscSendMousePressed(sampleX, sampleY, samplePos);
  }
}

/**
 * Called by draw() loop when the mouse is down and isDrawMode == true. 
 */
public void addPoint() {
  // we do some very basic point thinning to eliminate successive duplicate points
  if (mouseX != currentPoint.x || mouseY != currentPoint.y) {
    currentPoint = new PVector(mouseX, mouseY);
    allPoints.add(currentPoint);
    allTimes.add(millis());
    sampleX = mouseX;
    sampleY = mouseY;
    samplePos = mapper.lookupSample(sampleX, sampleY);      
    if (nd != null) nd.oscSendMousePressed(sampleX, sampleY, samplePos);
  }
}

/**
 * If isDrawMode == true and allPoints is not empty or trivially small,
 * initialize a new curveMaker (class that handles path, curve, and brush drawing).
 * Otherwise, process the mouseReleased event with handleMousePressed().
 */
public void mouseReleased() {
  if (isDrawMode && allPoints != null) {
    if (allPoints.size() > 2) {    // add curve data to the brush list
      initCurveMaker();
    }
    else {              // handle the event as a click
      handleMousePressed();
    }
    allPoints.clear();
  }
}

/**
 * Once allPoints has been completed, use it to initialize a PACurveMaker, a class
 * for generating and storing drawing data. See PACurveMaker for details. 
 * This method exposes many of the fields that you can set in PACurveMaker, though 
 * you're unlikely to use all of them. The geometric objects in PACurveMaker--
 * dragPoints, rdpPoints, eventPoints, curveShape and brushShape--can only be accessed
 * through getters and setters: this ensures that they are initialized. PACurveMaker
 * uses factory methods to create PACurveMaker objects. See the documentation in
 * PACurveMaker for information on how each factory method works. In PACurveUtility 
 * implements you will find the curve modeling and drawing methods on which
 * PACurveMaker depends. 
 */
public void initCurveMaker() {
  curveMaker = PACurveMaker.buildCurveMaker(this.allPoints);
  curveMaker.setEpsilon(this.epsilon);
  curveMaker.setDrawWeighted(this.isDrawWeighted);
  curveMaker.setBezierBias(this.curveWeight);
  curveMaker.setBrushColor(this.readyBrushColor);
  curveMaker.setActiveBrushColor(this.activeBrushColor);
  curveMaker.setTimeStamp(this.startTime);
  curveMaker.setTimeOffset(millis() - this.startTime);
  curveMaker.setDragTimes(this.allTimes);
  curveMaker.calculateDerivedPoints();
  this.eventPoints = curveMaker.getEventPoints(this.polySteps);
  playPoints(eventPoints);
  this.brushShapesList.add(curveMaker);
  // isDrawMode = false;
  sampleX = mouseX;
  sampleY = mouseY;
  samplePos = mapper.lookupSample(sampleX, sampleY);
  if (nd != null) {
    nd.oscSendMousePressed(sampleX, sampleY, samplePos);
    nd.oscSendDrawPoints(curveMaker.getRdpPoints());
    nd.oscSendTimeStamp(curveMaker.timeStamp, curveMaker.timeOffset);
  }
}
    
/**
 * Returns the host PApplet that runs AriaDemoApp.
 * Required by the PANetworkCLientINF interface.
 */
public PApplet getPApplet() {
  return this;
}


/**
 * Returns the PixelAudioMapper that negotiates between audio and image.
 * Required by the PANetworkCLientINF interface.  
 */
public PixelAudioMapper getMapper() {
  return this.mapper;
}

// obsolete version of playPoints()
/*
 *
public void playPoints() {
  if (eventPoints != null) {
    eventPointsIter = eventPoints.listIterator();
    int startTime = millis();
    // println("building event points schedule: "+ startTime);
    if (curveTLEvents == null) curveTLEvents = new ArrayList<TimedLocation>();
    storeCurveTL(eventPointsIter, startTime);
  }
  else {
    println("--->> NULL eventPoints");
  }
}
*/


/**
 * Takes a list of PVectors representing points along a curve or brushstroke
 * and schedules audio events at the different points. 
 * Required by the PANetworkCLientINF interface.
 */
public void playPoints(ArrayList<PVector> pts) {
  if (pts != null) {
    // this.timeLocsArray.clear();
    ListIterator<PVector> ptsIter = pts.listIterator();
    int startTime = millis();
    // println("building pointsTimer: "+ startTime);
    if (curveTLEvents == null) curveTLEvents = new ArrayList<TimedLocation>();
    storeCurveTL(ptsIter, startTime);
  }
}

/**
 * Generates TimedLocation events and stores them in a sorted collection.
 * @param iter      an iterator over a list of PVectors
 * @param startTime    the time for the first event, stored in a TimedLocation object
 */
public synchronized void storeCurveTL(ListIterator<PVector> iter, int startTime) {
  startTime += 50;
  int i = 0;
  while (iter.hasNext()) {
    PVector loc = iter.next();
    curveTLEvents.add(new TimedLocation(Math.round(loc.x), Math.round(loc.y), startTime + i++ * eventStep));
  }
  Collections.sort(curveTLEvents);
}


/**
 * Plays an audio event that uses audio samples at offset samplePos in the audioBuffer.
 * Required by the PANetworkCLientINF interface.
 */
public int playSample(int samplePos) {
  audioSampler = selectAudioSampler(samplePos);
  // ADSR envelope, we need a new one each time (not sure why)
  adsr = isDrawMode ? shortEnv.clone().getAdsr()
      : longEnv.clone().getAdsr();
  if (octet.size() == 0) {
    loadOctet(audioSampler, adsr);
  }
  instrument = octet.get(nowPlaying);
  // println("--->> nowPlaying instrument "+ nowPlaying);
  instrument.setSampler(audioSampler);
  instrument.setADSR(adsr);
  instrument.sampler.patch(adsr);
  adsr.unpatchAfterRelease(audioSampler);
  nowPlaying = (nowPlaying + 1) % maxPlayers;
  // play command takes a duration in seconds
  float duration = samplelen / (float) (sampleRate);
  instrument.play(duration);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, (int) (duration * 1000) + millis()));
  // return the length of the sample
  return samplelen;
}

/**
 * Creates and initializes an audioSampler. 
 * TODO wrap audioSampler at end of buffer to beginning of buffer.
 * 
 * @param samplePos    offset into the audioBuffer where sample data is located
 * @return        an audioSampler selected among several available in rotation
 */
public Sampler selectAudioSampler(int samplePos) {
  int sampleOffset = 4096;
  float relTime = isDrawMode ? shortEnv.getRel() : longEnv.getRel();
  // detuning makes the pitch of the audio samples vary by a random amount up or down
  // TODO could be refined to represent pitch variations more accurately, i.e. in fractions of an octave
  int detune = (isDetuned) ? (int) ((0.5f - random(1.0f)) * detuning) : 0;
  // println("--- detune "+ detune);
  audioSampler = new Sampler(audioBuffer, sampleRate + detune, 32); // create a Minim Sampler 
  audioSampler.amplitude.setLastValue(0.6f); // set amplitude for the Sampler
  int beginPos = samplePos - sampleOffset < 0 ? 0 : samplePos - 4096;
  audioSampler.begin.setLastValue(beginPos); // set the Sampler to begin playback at samplePos
  int releaseDuration = (int) (relTime * sampleRate); // do some calculation to include the release time.
  float vary = (float) (PixelAudio.gauss(this.sampleScale, this.sampleScale * 0.125f)); // vary duration
  // println("----->>> vary = "+ vary +", sampleScale = "+ sampleScale);
  this.samplelen = (int) (vary * this.sampleBase); // calculate the duration of the sample
  if (isDrawMode) samplelen = samplelen / 4;
  if (samplePos + samplelen >= mapSize) {
    samplelen = mapSize - samplePos; // make sure we don't exceed the mapSize
    // println("----->>> sample length = " + samplelen);
  }
  int durationPlusRelease = this.samplelen + releaseDuration;
  int end = (beginPos + durationPlusRelease >= this.mapSize) ? this.mapSize - 1
      : beginPos + durationPlusRelease;
  // println("----->>> end = " + end);
  audioSampler.end.setLastValue(end);
  // println("-- samplePos = ", samplePos, " -- beginPos = ", beginPos);
  return audioSampler;
}

/**
 * Initializes a list of SamplerInstruments which will be available to play audio samples. 
 * @param sampler
 * @param adsr
 */
public void loadOctet(Sampler sampler, ADSR adsr) {
  for (int i = 0; i < maxPlayers; i++) {
    octet.add(new SamplerInstrument(audioOut, sampler, adsr));
  }
  nowPlaying = 0;
}

/**
 * Applies a filter to a portion of an audio signal using a Fast Fourier Transform.
 * 
 * @param samplePos
 * @param sampleSize
 * @param isWriteToImage
 */
public void randomFormantFilter(int samplePos, int sampleSize, boolean isWriteToImage) {
  // get a section of our audio signal that is padded 
  // samplePos = 131072; // for testing
  int pad = fftHandler.getOverlapSize();
  if (samplePos < pad) pad = samplePos;
  int start = samplePos - pad;
  int end = samplePos + sampleSize + pad;
  // careful with array out of bounds
  if (end > audioSignal.length) {
    end = audioSignal.length - 1;
  }    
  float[] sig = mapper.pluckSamples(audioSignal, start, end - start); 
  fftHandler.setWindow(FourierTransform.HAMMING);
  int pos = (int) random(formants.size());
  println("--- "+ formants.get(pos).toString());
  float[] freqs = formants.get(pos).getFrequencies();
  float[] amps = {1.4f, 1.3f, 1.2f};
  float[] result = fftHandler.processSignalOverlapSave(sig, freqs, amps);
  mapper.plantSamples(result, audioSignal, samplePos, result.length);
  writeAudioToImage();
}

/**
 * Applies a formant filter to a portion of an audio signal using a Fast Fourier Transform.
 * 
 * @param samplePos      location in the audio buffer 
 * @param sampleSize    number of samples to process
 * @param formant      a data object encapsulating the frequencies of a formant filter
 * @param isWriteToImage  if true, write the audio buffer to the display image
 */
public void formantFilter(int samplePos, int sampleSize, PAFFTHandler.Formant formant, boolean isWriteToImage) {
  if (isRunFastFFT) {
    fftHandler.setOverlapSize(0);
  }
  // get a section of our audio signal that is padded 
  int pad = fftHandler.getOverlapSize();
  if (samplePos < pad) pad = samplePos;
  int start = samplePos - pad;
  int end = samplePos + sampleSize + pad;
  // careful with array out of bounds
  if (end > audioSignal.length) {
    end = audioSignal.length - 1;
  }  
  float[] sig = mapper.pluckSamples(audioSignal, start, end - start); 
  fftHandler.setWindow(FourierTransform.HAMMING);
  // println("--- "+ formant.toString());
  float[] freqs = formant.getFrequencies();
  float[] amps = {1.4f, 1.3f, 1.2f};
  float[] result;
  if (isRunFastFFT) {
    // very fast no-window FFT
    result = fftHandler.processSignalFrequenciesFast(sig, freqs, amps, true);    
  }
  else {
    // a slow but accurate 50% overlap windowed FFT
    result = fftHandler.processSignalOverlapSave(sig, freqs, amps);
  }
  mapper.plantSamples(result, audioSignal, start, end - start);
  if (isWriteToImage) {
    writeAudioToImage();
    displayColors(true);
  }
}

/**
 * Applies a formant filter to a portion of an audio signal using FFT. 
 * TODO refactor to allow access to the entire audio buffer
 * 
 * @param samplePos
 * @param sampleSize
 * @param isWriteToImage
 */
public void formantZone(int samplePos, int sampleSize, boolean isWriteToImage) {
  int key = samplePos / 65536;
  int f = zoneNums[key];
  formantFilter(samplePos, sampleSize, formants.get(f), isWriteToImage);
}

/**
 * Trigger formant filters at random locations.
 * 
 * @param sampleLength
 * @param isWriteToImage
 * @param signalPos
 */
public void formantRaindrops(int sampleLength, boolean isWriteToImage, int signalPos) {
  int[] coords = mapper.lookupCoordinate(signalPos);
  sampleX =  coords[0];
  sampleY = coords[1];
  formantFilter(signalPos, sampleLength, currentFormant, isWriteToImage);
}

/**
 * Step through the image applying the formant filters specified by the zoneNums array.
 * 
 * @param isWriteToImage
 */
public void formantSequence(boolean isWriteToImage) {
  int sampleLength = mapSize/24;
  for (int i = 0; i < 24; i++) {
    formantZone(i * sampleLength, sampleLength, isWriteToImage);
  }
}

/**
 * Experimental method using non-real-time windowed FFT.
 * 
 * @param samplePos
 * @param sampleSize
 */
public void freqFilter(int samplePos, int sampleSize) {
  // get a section of our audio signal that is padded 
  // samplePos = 131072; // for testing
  int pad = fftHandler.getOverlapSize();
  if (samplePos < pad) pad = samplePos;
  int start = samplePos - pad;
  int end = samplePos + sampleSize + pad;
  // careful with array out of bounds
  if (end > audioSignal.length) {
    end = audioSignal.length - 1;
  }
  float[] sig = mapper.pluckSamples(audioSignal, start, end - start); 
  fftHandler.setWindow(FourierTransform.HAMMING);
  float[] freqs = {300, 870, 2240}; // formant for "u" as in "boot'
  float[] amps = {1.4f, 1.3f, 1.2f};
  float[] result = fftHandler.processSignalOverlapSave(sig, freqs, amps);
  mapper.plantSamples(result, audioSignal, samplePos, result.length);
  writeAudioToImage();
}

/**
 * Limit the amplitude of a signal to a specified maximum.
 * 
 * @param signal
 * @param limit
 */
public void normalize(float[] signal, float limit) {
    float maxAmplitude = 0;
    for (float sample : signal) {
        maxAmplitude = Math.max(maxAmplitude, Math.abs(sample));
    }
    if (maxAmplitude > limit) {
      float norm = maxAmplitude/limit;
        for (int i = 0; i < signal.length; i++) {
            signal[i] /= norm;  // Normalize the signal
        }
    }
}

/**
 * A reset method that can be called in the middle of a performance when 
 * Mimim's audio processing becomes glitchy. Reinitializes audio. 
 * Minim seems to glitch when too many audio events are playing at once. 
 * It seems that triggering 3 brushstrokes to play audio is about the 
 * maximum that works.
 * 
 * @param isClearCurves    clear the list of brushStrokes and associated events 
 */
public void reset(boolean isClearCurves) {
  initAudio();
  loadFiles();
  if (this.octet != null) this.octet.clear();
  if (this.curveMaker != null) this.curveMaker = null;
  if (this.eventPoints != null) this.eventPoints.clear();
  this.activeIndex = 0;
  if (isClearCurves) {
    if (this.brushShapesList != null) this.brushShapesList.clear();
    if (this.curveTLEvents != null) this.curveTLEvents.clear();
    if (nd != null) nd.oscSendClear();
    if (nd != null) nd.setDrawCount(0);
    println("----->>> RESET audio + curves <<<------");
  }
  else {
    println("----->>> RESET audio <<<------");
  }
}


// ------------- HISTOGRAM AND GAMMA ADJUSTMENTS ------------- // 
  
/**
 * Loops through an array of RGB values to find minimum and maximum
 * values in any color channel. 
 * 
 * @param source
 * @return
 */
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

/**
 * Scales an array of RGB values to fit within lower and upper bounds. 
 * 
 * @param source  an array of RGB values
 * @param low    lower bound to stretch values in each color channel
 * @param high    upper bound to stretch values ineach color channel
 * @return      a new array with the scaled RGB values
 */
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

/**
 * Calculates a gamma table derived from the non-linear gamma scaling function, 
 * used to adjust brightness and contrast in images.
 * 
 * @param gamma  a floating point value typically in the range 0.5 (darker) to 2.2 (lighter)
 */
public void setGamma(float gamma) {
  if (gamma != 1.0) {
    this.gammaTable = new int[256];
    for (int i = 0; i < gammaTable.length; i++) {
      float c = i/(float)(gammaTable.length - 1);
      gammaTable[i] = (int) Math.round(Math.pow(c, gamma) * (gammaTable.length - 1));
    }
  }
}

/**
 * Applies a gamma adjustment to an array of RGB values.
 * 
 * @param source  an array of RGB values
 * @return      a new array of scaled RGB values
 */
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



/**
 * Copies an image and returns the new image.
 * 
 * @param image    PImage to copy
 * @return      a new PImage, copy of the one supplied
 */
public PImage getImageCopy(PImage image) {
  PImage newImage = createImage(image.width, image.height, ARGB);
  newImage.loadPixels();
  System.arraycopy(image.pixels, 0, newImage.pixels, 0, image.pixels.length);
  newImage.updatePixels();
  return newImage;
}

/**
 * @param argb     an RGB color
 * @param alpha    value of alpha channel if argb is not black
 * @return      if color is black, a completely transparent RGB value (alpha channel is 0)
 *           otherwise, the color with alpha channel set to the supplied value
 */
public int setAlphaWithBlack(int argb, int alpha) {
  int[] c = PixelAudioMapper.rgbaComponents(argb);
  if (c[0] == c[1] && c[1] == c[2] && c[2] == 0) {
    alpha = 0;
  }
  return alpha << 24 | c[0] << 16 | c[1] << 8 | c[2];
}

/**
 * Sets the alpha channel of an RGB color to a supplied value.
 * 
 * @param argb    an RGB value
 * @param alpha    value for alpha channel (0..255)
 * @return      the RGB color with its alpha channel set to alpha
 */
public static int setAlpha(int argb, int alpha) {
   return (argb & 0x00FFFFFF) | (alpha << 24);
}



/**
 * For a live stream from a file or the built-in mic (Processing IDE only!),
 * toggle listening / streaming.
 */
public void toggleListening() {
  isListening = !isListening;
  if (!isListening) {
    anthem.removeListener(streamCap);
    anthem.pause();
  }
  else {
    anthem.addListener(streamCap);
    anthem.play();
  }
}

/**
 * Draws audioSignal, transcoded to pixel values,
 * to the Brightness channel of mapImage in the HSB color space.
 * 
 */
public void drawSignal() {
  this.rgbSignal = mapper.pluckSamplesAsRGB(audioSignal, 0, mapSize);
  ChannelNames oldChan = chan;
  chan = ChannelNames.L;
  mapImage.loadPixels();
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, rgbSignal.length, chan);
  mapImage.updatePixels();
  chan = oldChan;
}

/**
 * Capture samples from an audio stream and passes them to audioSignal and audioBuffer. 
 */
public class StreamCapture implements AudioListener {
  private float[] left;
  private float[] right;

  public StreamCapture() {
    left = null;
    right = null;
  }

  @Override
  public synchronized void samples(float[] samp) {
    left = samp;
    fillSignal();
  }

  @Override
  public synchronized void samples(float[] sampL, float[] sampR) {
    left = sampL;
    right = sampR;
    fillSignal();
  }
  
  /**
   * The tricky part is keeping the signal in the right order for playback: write
   * the buffer to the signal first, then rotate the signal left by the length of
   * the buffer.
   */
  public synchronized void fillSignal() {
    if (left != null) {
      for (int i = 0; i < left.length; i++) {
        audioSignal[i] = left[i];
      }
      PixelAudioMapper.rotateLeft(audioSignal, left.length);
      audioBuffer.setChannel(0, audioSignal);
    }
    if (right != null) {
      // not doing anything
    }
  }

}

// ----->>> FILE UTILITIES <<<----- //

// These fields and methods are part of an experiment for furthering 
// real time performance with the AriaDemoApp.
// LoadMediaFiles() is called by setAriaVariables, which is called by setup(). 
// See Javadoc comments for a description of what loadMediaFiles() does. 

File[] fileList;
File[] cosmicList;
File[] fotoList;
File[] sonicList;

String cosmicDir = "_cosmic/";
String fotoDir = "_foto/";
String sonicDir = "_sonic/";
boolean isShuffleCosmic = false;
boolean isShuffleFoto = false;
boolean isShuffleSonic = false;
int filePtr = 0;
int cosmicPtr = 0;
int fotoPtr = 0;
int sonicPtr = 0;

/**
 * present a file selection dialog to open a new file
 */
public void chooseFolder() {
  selectInput("Choose a file in desired folder:", "setFileList");
}

/**
 * Callback method for chooseFolder(), handles user's actions in open file
 * dialog. If user selected a file, it is used to construct the fileList
 * variable. filePtr is reset to 0. Call to getAllFiles outputs first ten files
 * in folder.
 *   ']' and '[' step through cosmicList
 *   '}' and '{' step through sonicList
 *   '!' steps through fotoList
 * 
 * @param selectedFile
 */
public void setFileList(File selectedFile) {
  if (null != selectedFile) {
    noLoop();
    String path = selectedFile.getAbsolutePath();
    String folderPath = path.substring(0, path.lastIndexOf(File.separator)) + File.separator;
    this.getAllFiles(new File(folderPath));
    loop();
  } else {
    println("No file was selected");
  }
}

private void getAllFiles(File dir) {
  fileList = dir.listFiles(new java.io.FilenameFilter() {
    public boolean accept(File dir, String name) {
      return (name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg") 
          || name.toLowerCase().endsWith(".jpeg") || name.toLowerCase().endsWith(".aif")
          || name.toLowerCase().endsWith(".wav") || name.toLowerCase().endsWith(".mp3"));
    }
  });
  if (fileList == null) {
    println("----->>> fileList is null");
    return;
  }
  Arrays.sort(fileList, new java.util.Comparator<File>() {
    public int compare(File f1, File f2) {
      // for descending order
      /* return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified()); */
      // sort by name
      return f1.getName().compareToIgnoreCase(f2.getName());
    }
  });
  int count = 0;
  // print the first 5 file names
  /**/
  for (File f : fileList) {
    if (f.isDirectory()) {
      // skip
    } else if (f.isFile()) {
      println(f.getName());
      if (count++ > 5) return;
    }
  }
  /**/
}


/**
 * In the directory specified by dataPath, looks for folders named by the 
 * String values in fotoDir, cosmicDir, and sonicDir and loads a list of 
 * files in each to fotoList, cosmicList, and sonicList. 
 */
public void loadMediaFiles() {
  println("----- FOTO -----");
  String dir = dataPath + fotoDir;
  // println("***** foto ", dir);
  // chooseFolder();
  getAllFiles(new File(dir));
  if (fileList != null) {
    fotoList = new File[fileList.length];
    System.arraycopy(fileList, 0, fotoList, 0, fileList.length);
    if (isShuffleFoto)
      shuffle(fotoList);
    fileList = null;
  }
  println("----- COSMIC -----");
  dir = dataPath + cosmicDir;
  getAllFiles(new File(dir));
  if (fileList != null) {
    cosmicList = new File[fileList.length];
    System.arraycopy(fileList, 0, cosmicList, 0, fileList.length);
    if (isShuffleCosmic)
      shuffle(cosmicList);
    fileList = null;
  }
  println("----- SONIC -----");
  dir = dataPath + sonicDir;
  getAllFiles(new File(dir));
  if (fileList != null) {
    sonicList = new File[fileList.length];
    System.arraycopy(fileList, 0, sonicList, 0, fileList.length);
    if (isShuffleSonic) {
      shuffle(sonicList);
      println("\n --- _sonic shuffled ---");
      printFileList(sonicList);
    }
  }
}

/**
 * Shuffles an array of objects into random order.
 * 
 * @param objectArray an array of <code>Object</code>s, changed on exit
 */
public void shuffle(Object[] objectArray) {
  for (int lastPlace = objectArray.length - 1; lastPlace > 0; lastPlace--) {
    // Choose a random location from 0..lastPlace
    int randLoc = floor(random(lastPlace + 1));
    // Swap items in locations randLoc and lastPlace
    Object temp = objectArray[randLoc];
    objectArray[randLoc] = objectArray[lastPlace];
    objectArray[lastPlace] = temp;
  }
}

public void printFileList(File[] fileArray) {
  for (File f : fileArray) {
    println(f.getName());
  }
}

public void stepFileList(File[] theList, int pos) {
  pos = pos % theList.length;
  boolean oldIsBlending = isBlending;
  isBlending = true;
  ChannelNames oldChan = chan;
  chan = ChannelNames.ALL;
  fileSelected(theList[pos]);
  isBlending = oldIsBlending;
  chan = oldChan;
}
