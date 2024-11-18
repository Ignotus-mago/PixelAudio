/*
 * TO GET STARTED
 * Launch the Max patch. 
 * Launch WorkFlowPerformance.
 * Click on image to trigger single voices. Coordinates and sample offset are reported in the Max UDPHandler window.
 * Type 'd' to turn on drawing. Drag the mouse to make "brushstrokes." Sound will play and data will show in 
 * the Max subpatcher network_receiver. Turn off drawing ('d' toggles on and off) and click on brushstrokes to play.
 * In Max, you can trigger this app -- see instructions in Max.
 *
 * 'z' key will reset both the app and the Max patch. See other key commands (incomplete list) below and in console.
 * 
 * Changes: Synchronized signatures on runPointEvents() and runCurveEvents().
 * New synchronized method storeCurveTL() called by playPoints() methods.
 * These steps seem to have eliminated the concurrent modification errors caused by calls from Max.
 * I also changed some params: epsilon = 4 (was 12) makes tighter reduced curve points, 
 * while polySteps = 8 (was 12) reduces number of points in polygonized Bezier curves. 
 * Calling audioSampler.amplitude.setLastValue(0.6f) with 0.6 instead of 0.9 seems to reduce clipping.
 *
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;

import javax.sound.sampled.*;

import ddf.minim.*;
import ddf.minim.ugens.*;
import ddf.minim.analysis.*;
import net.paulhertz.pixelaudio.*;
//import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;
import net.paulhertz.pixelaudio.curves.*;

/**
 * Development version 3 of DeadBodyWorkFlow performance software.
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
PixelAudioMapper.ChannelNames chan = PixelAudioMapper.ChannelNames.ALL;
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
int audioLength;

// SampleInstrument setup
float sampleScale = 4;
int sampleBase = sampleRate / 4;
int samplelen = (int) (sampleScale * sampleBase);
Sampler audioSampler;     // minim class for sampled sound
ArrayList<WFInstrument> octet;
ArrayList<MultiChannelBuffer> voiceList;
WFInstrument instrument;      // local class to wrap audioSampler
int nowPlaying = 0;
int maxPlayers = 8;
// ADSR and params
ADSR adsr;                // good old attack, decay, sustain, release
PAEnvelope longEnv;
PAEnvelope shortEnv;

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
int animSteps = 64;
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
public float epsilon = 4.0f;
public ArrayList<PVector> allPoints = new ArrayList<PVector>();
public ArrayList<Integer> allTimes;
public PVector currentPoint;
public int polySteps = 8;
public PACurveMaker curveMaker;
public ArrayList<PVector> eventPoints;
public ListIterator<PVector> eventPointsIter;
int eventStep = 90;   // milliseconds between events
public ArrayList<TimedLocation> curveTLEvents;
public ArrayList<BrushData> brushShapesList;
public BrushData activeBrush;
public int activeIndex = 0;
// network communications
NetworkDelegate nd;

String dataPath;
String audioStartFile = "workflow_03_mixdown.wav";
String imageStartFile = "workFlowPanel_03.png";


public void settings() {
  size(rows * genWidth, columns * genHeight);
}

public void setup() {
  frameRate(30);
  pixelaudio = new PixelAudio(this);
  minim = new Minim(this);
  sampleRate = 48000; // we've recorded our audio at 48KHz
  sampleBase = sampleRate / 4;
  initAudio();
  initMapper();
  dataPath = dataPath("");
  // println("-- dataPath: "+ dataPath);
  audioStartFile = dataPath +"/"+ audioStartFile;
  imageStartFile = dataPath +"/"+ imageStartFile;
  loadFiles();
  currentPoint = new PVector(-1, -1);
  timeLocsArray = new ArrayList<TimedLocation>();
  octet = new ArrayList<WFInstrument>();
  voiceList = new ArrayList<MultiChannelBuffer>();
  brushShapesList = new ArrayList<BrushData>();
  nd = new NetworkDelegate();
  nd.oscSendClear();
}

public void initAudio() {
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  this.audioBuffer = new MultiChannelBuffer(1024, 1);
  longEnv = new PAEnvelope(0.9f, 0.2f, 0.125f, 0.5f, 0.2f);
  shortEnv = new PAEnvelope(0.8f, 0.05f, 0.0f, 0.8f, 0.05f);
}

public void initMapper() {
  // multigen = loadLoopGen(genWidth, genHeight);
  multigen = loadWordGen(genWidth/4, genHeight/4);
  mapper = new PixelAudioMapper(multigen);
  mapSize = mapper.getSize();
  colors = getColors(mapSize); // create an array of rainbow colors, useful for visualizing the signal path
  mapImage = createImage(width, height, ARGB); // an image to use with mapper
  displayColors();
}

public void loadFiles() {
  boolean oldIsLoadBoth = isLoadBoth;
  isLoadBoth = false;
  loadAudioFile(new File(audioStartFile));
  loadImageFile(new File(imageStartFile));
  isLoadBoth = oldIsLoadBoth;
}


/**
 * Adds PixelMapGen objects to the local variable genList. The genList 
 * initializes a MultiGen, which can be used to map audio and pixel data.
 * This method provides a big looping fractal.
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
 * Adds PixelMapGen objects to the local variable genList. The genList 
 * initializes a MultiGen, which can be used to map audio and pixel data.
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
  image(mapImage, 0, 0);
  if (isAnimating)
    stepAnimation(animSteps);
  if (isDrawMode) {
    if (mousePressed) {
      addPoint();
      // isRefreshBuffer = true;
    }
  } 
  freshDraw();
  if (curveTLEvents != null) runCurveEvents();
  runPointEvents();
}

public void freshDraw() {
  if (this.brushShapesList.size() > 0) {
    int idx = 0;
    for (BrushData bd : brushShapesList) {
      int brushFill = color(34, 89, 55, 233);
      if (mouseInPoly(bd.brush.getPointList(this, polySteps))) {
        brushFill = color(144, 89, 55, 233);
        activeBrush = bd;
        activeIndex = idx;
      }
      bd.brush.setFillColor(brushFill);
      bd.brush.setWeight(2);
      bd.brush.setStrokeColor(brushFill);
      bd.brush.draw(this);
      idx++;
    }
  }
  if (isDrawMode && curveMaker != null) curveMakerDraw();
}

public void curveMakerDraw() {
  if (curveMaker.isReady()) {
    // curveMaker.RDPDraw(this);
    // curveMaker.curveDraw(this, false);
    PABezShape brush = curveMaker.brushShape;
    brush.setFillColor(color(144, 34, 42, 233));
    brush.setWeight(2);
    brush.setStrokeColor(color(144, 34, 42, 233));
    brush.draw(this);
    // curveMaker.brushDraw(this, color(144, 34, 42, 233));
    curveMaker.polyPointsDraw(this, polySteps, color(233, 199, 144, 192), 6);
  }
  else {
    if (curveMaker.allPoints != null && curveMaker.allPoints.size() > 2) 
      curveMaker.allPointsDraw(this);
  }
}

public boolean mouseInPoly(ArrayList<PVector> poly) {
  return PABezShape.pointInPoly(poly, mouseX, mouseY);
}

public void stepAnimation(int animSteps) {
  mapImage.loadPixels();
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
  PixelAudioMapper.rotateLeft(rgbSignal, animSteps);
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, mapSize);
  mapImage.updatePixels();
}

/*
// how we used to do the loop
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
*/

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
  int currentTime = millis();
  curveTLEvents.forEach(tl -> {
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
  curveTLEvents.removeIf(TimedLocation::isStale);
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
    isDrawMode = !isDrawMode;
    if (!isDrawMode) this.curveMaker = null;
    String msg = isDrawMode ? "Screen drawing is on. Drag the mouse to draw to the screen." : "Screen drawing is off.";
    println("-- "+ msg +" --");
    break;
  case 'p': case 'P':
    println("---->>> curveTLEvents size ", this.curveTLEvents.size());
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
  case 'x':
    if (brushShapesList != null) {
      // remove the oldest addition
      if (!brushShapesList.isEmpty()) {
        brushShapesList.remove(0);    // brushShapes array starts at 0
        nd.oscSendDelete(1);       // Mac coll object index starts at 1
      }
    }
    break;
  case 'X':
    if (brushShapesList != null) {
      // remove the most recent addition
      if (!brushShapesList.isEmpty()) {
        int idx = brushShapesList.size();
        brushShapesList.remove(idx - 1);  // brushShapes array starts at 0
        nd.oscSendDelete(idx);        // Mac coll object index starts at 1
      }
    }
    break;
  case 'z':
    reset();
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
  println(" * Press 'W' to write the audio buffer to the image");
  println(" * Press '?' to show this help message.");
}

public void mousePressed() {
  if (this.isDrawMode) {
    allPoints.clear();
    allTimes = new ArrayList<Integer>();
    curveMaker = new PACurveMaker(allPoints);
    curveMaker.setTimeStamp(millis());
    curveMaker.setEpsilon(epsilon);
    addPoint();
  } 
  else {
    if (activeBrush != null) {
      // a brushShape was triggered
      eventPoints = activeBrush.polyPoints;
      playPoints();
      // probably don't want to send the brush, already sent 
      // when it was created (see mouseReleased)
      // nd.oscSendDrawPoints(activeBrush.drawPoints);
      nd.oscSendTrig(activeIndex + 1);
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
      nd.oscSendMousePressed(sampleX, sampleY, samplePos);
    }
  }
}

public void addPoint() {
  if (mouseX != currentPoint.x || mouseY != currentPoint.y) {
    currentPoint = new PVector(mouseX, mouseY);
    allPoints.add(currentPoint);
    
  }
}

public void mouseReleased() {
  if (isDrawMode && allPoints != null && allPoints.size() > 2) {
    calculateDerivedPoints();
    if (curveMaker.isReady()) {
      curveMaker.setTimeOffset(millis() - curveMaker.getTimeStamp());
      PABezShape curve = curveMaker.bezPoints;
      eventPoints = curve.getPointList(this, polySteps);
      playPoints();
      PABezShape brush = curveMaker.brushShape;
      addBrushShape(curve, brush, eventPoints, curveMaker.drawPoints, curveMaker.getTimeStamp(), curveMaker.getTimeOffset());
      // isDrawMode = false;
      nd.oscSendDrawPoints(curveMaker.drawPoints);
      nd.oscSendTimeStamp(curveMaker.timeStamp, curveMaker.timeOffset);
    }
  }
  // isRefreshBuffer = true;
}

public void addBrushShape(PABezShape curve, PABezShape brush, ArrayList<PVector> polyPoints, 
              ArrayList<PVector> drawPoints, int timeStamp, int timeOffset) {
  this.brushShapesList.add(new BrushData(curve, brush, polyPoints, drawPoints, timeStamp, timeOffset));
}

public void calculateDerivedPoints() {
  curveMaker.calculateDerivedPoints();
}  

public void playPoints() {
  if (eventPoints != null) {
    this.timeLocsArray.clear();
    eventPointsIter = eventPoints.listIterator();
    int startTime = millis();
    // println("building pointsTimer: "+ startTime);
    if (curveTLEvents == null) curveTLEvents = new ArrayList<TimedLocation>();
    storeCurveTL(eventPointsIter, startTime);
  }
}

public void playPoints(ArrayList<PVector> pts) {
  if (pts != null) {
    this.timeLocsArray.clear();
    ListIterator<PVector> ptsIter = pts.listIterator();
    int startTime = millis();
    // println("building pointsTimer: "+ startTime);
    if (curveTLEvents == null) curveTLEvents = new ArrayList<TimedLocation>();
    storeCurveTL(ptsIter, startTime);
  }
}

public synchronized void storeCurveTL(ListIterator<PVector> iter, int startTime) {
  startTime += 50;
  int i = 0;
  while (iter.hasNext()) {
    PVector loc = iter.next();
    curveTLEvents.add(new TimedLocation(Math.round(loc.x), Math.round(loc.y), startTime + i++ * eventStep));
  }
  Collections.sort(curveTLEvents);
}

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

public Sampler selectAudioSampler(int samplePos) {
  int sampleOffset = 4096;
  float relTime = isDrawMode ? shortEnv.getRel() : longEnv.getRel();
  audioSampler = new Sampler(audioBuffer, sampleRate, 32); // create a Minim Sampler 
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

public void loadOctet(Sampler sampler, ADSR adsr) {
  for (int i = 0; i < maxPlayers; i++) {
    octet.add(new WFInstrument(audioOut, sampler, adsr));
  }
  nowPlaying = 0;
}

public void loadVoiceList() {
  String[] files = new String[] {"voice_01.wav", "voice_02.wav", "voice_03.wav", 
  "voice_04.wav", "voice_05.wav", "voice_06.wav", "voice_07.wav",  "voice_08.wav"};
  for (int i = 0; i < files.length; i++) {
    
  }
}

public void reset() {
  this.eventPoints.clear();
  this.curveTLEvents.clear();
  this.octet.clear();
  initAudio();
  loadFiles();
  this.curveMaker = null;
  this.brushShapesList.clear();
  this.activeIndex = 0;
  nd.oscSendClear();
  nd.setDrawCount(0);
  println("----->>> RESET <<<------");
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


// ------------------------------------------- //
//          AUDIO and IMAGE FILE I/O           //
// ------------------------------------------- //

// ------------- LOAD AUDIO FILE ------------- //

public void chooseFile() {
  oldIsAnimating = isAnimating;
  isAnimating = false;
  isBufferStale = true;
  selectInput("Choose an audio file or an image file: ", "fileSelected");
}

public void fileSelected(File selectedFile) {
  if (null != selectedFile) {
    String filePath = selectedFile.getAbsolutePath();
    String fileName = selectedFile.getName();
    String fileTag = fileName.substring(fileName.lastIndexOf('.') + 1);
    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
    if (fileTag.equalsIgnoreCase("mp3") || fileTag.equalsIgnoreCase("wav") || fileTag.equalsIgnoreCase("aif")
        || fileTag.equalsIgnoreCase("aiff")) {
      audioFile = selectedFile;
      audioFilePath = filePath;
      audioFileName = fileName;
      audioFileTag = fileTag;
      println("----- Selected file " + fileName + "." + fileTag + " at "
          + filePath.substring(0, filePath.length() - fileName.length()));
      loadAudioFile(audioFile);
      isLoadBoth = false;
    } 
    else if (fileTag.equalsIgnoreCase("png") || fileTag.equalsIgnoreCase("jpg") || fileTag.equalsIgnoreCase("jpeg")) {
        imageFile = selectedFile;
        imageFilePath = filePath;
        imageFileName = fileName;
        imageFileTag = fileTag;
        loadImageFile(imageFile);
        isLoadBoth = true;
    }
    else {
      println("----- File is not a recognized audio format ending with \"mp3\", \"wav\", \"aif\", or \"aiff\".");
    }
  } 
  else {
    println("----- No audio file was selected.");
  }
  isAnimating = oldIsAnimating;
}

public void loadAudioFile(File audFile) {
  // read audio file into our MultiChannelBuffer
  float sampleRate = minim.loadFileIntoBuffer(audFile.getAbsolutePath(), audioBuffer);
  // sampleRate > 0 means we read audio from the file
  if (sampleRate > 0) {
    // read an array of floats from the buffer
    loadAudioSignal();
    // load rgbSignal with rgb gray values corresponding to the audio sample values
    if (isLoadBoth) writeAudioToImage();
  }
}

public void loadAudioSignal() {
  this.audioSignal = audioBuffer.getChannel(0);
  this.audioLength = audioSignal.length;
  if (audioLength < mapSize) {
    audioSignal = Arrays.copyOf(audioSignal, mapSize);
    audioLength = audioSignal.length;
    audioBuffer.setChannel(0, audioSignal);
  }
  if (audioLength > mapSize) {
    audioBuffer.setBufferSize(mapSize);
    audioSignal = audioBuffer.getChannel(0);
    audioLength = audioSignal.length;
  }
}

public void writeAudioToImage() {
  rgbSignal = mapper.pluckSamplesAsRGB(audioSignal, 0, mapSize);
  if (rgbSignal.length < mapSize) {
    // pad rgbSignal with 0's if necessary
    rgbSignal = Arrays.copyOf(rgbSignal, mapSize);
  }
  if (isBlending) {
    int alpha = 128;
    Arrays.setAll(rgbSignal, index -> PixelAudioMapper.setAlpha(rgbSignal[index], alpha));
  }
  mapImage.loadPixels();
  // write the rgbSignal pixels to mapImage, following the signal path
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, rgbSignal.length, chan);
  mapImage.updatePixels();
}


public void loadImageFile(File imgFile) {
  PImage img = loadImage(imgFile.getAbsolutePath());
  loadImagePixels(img);
  if (isLoadBoth) writeImageToAudio();
}

public void loadImagePixels(PImage img) {
  // TODO handle color channel setting for images
  int w = img.width > mapImage.width ? mapImage.width : img.width;
  int h = img.height > mapImage.height ? mapImage.height : img.height;
  if (chan != PixelAudioMapper.ChannelNames.ALL) {
    PImage mixImage = createImage(w, h, ARGB);
    mixImage.copy(mapImage, 0, 0, w, h, 0, 0, w, h);
    img.loadPixels();
    mixImage.loadPixels();
    mixImage.pixels = PixelAudioMapper.pushAudioPixel(img.pixels, mixImage.pixels, chan);
    mixImage.updatePixels();
    // TODO make it work!
    mapImage.copy(mixImage,0, 0, w, h, 0, 0, w, h);
  }
  else {
    mapImage.copy(img,0, 0, w, h, 0, 0, w, h);
  }
}

public void writeImageToAudio() {
  // println("----- writing image to signal ");
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
  audioBuffer.setBufferSize(mapSize);
  mapImage.loadPixels();
  // fetch pixels from mapImage in signal order, put them in rgbSignal
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, rgbSignal.length);    
  // write the Brightness channel of rgbPixels, transcoded to audio range, to audioBuffer
  mapper.plantSamples(rgbSignal, audioBuffer.getChannel(0), 0, mapSize, PixelAudioMapper.ChannelNames.L);
}


// ------------- SAVE AUDIO FILE ------------- //

public void saveToAudio() {
  // File folderToStartFrom = new File(dataPath("") + "/");
  // selectOutput("Select an audio file to write to:", "audioFileSelectedWrite", folderToStartFrom);
  selectOutput("Select an audio file to write to:", "audioFileSelectedWrite");
}

public void audioFileSelectedWrite(File selection) {
  if (selection == null) {
    println("Window was closed or the user hit cancel.");
    return;      
  }
  String fileName = selection.getAbsolutePath();
  if (selection.getName().indexOf(".wav") != selection.getName().length() - 4) {
    fileName += ".wav";
  }
  saveAudioFile(fileName);
}

public void saveAudioFile(String fileName) {
  try {
    saveAudioToFile(audioSignal, sampleRate, fileName);
    println("Saved file to sketch path: "+ fileName);
  } catch (IOException e) {
    println("--->> There was an error outputting the audio file "+ fileName +".\n"+ e.getMessage());
  } catch (UnsupportedAudioFileException e) {
    println("--->> The file format is unsupported." + e.getMessage());
  }
}

/**
 * Saves audio data to 16-bit integer PCM format, which Processing can also
 * open.
 * 
 * @param samples    an array of floats in the audio range (-1.0f, 1.0f)
 * @param sampleRate audio sample rate for the file
 * @param fileName   name of the file to save to
 * @throws IOException                   an Exception you'll need to handle to
 *                                       call this method (see keyPressed entry
 *                                       for 's')
 * @throws UnsupportedAudioFileException another Exception (see keyPressed entry
 *                                       for 's')
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


// ------------- SAVE IMAGE FILE ------------- //

public void saveToImage() {
  // File folderToStartFrom = new File(dataPath(""));
  selectOutput("Select an image file to write to:", "imageFileSelectedWrite");
}

public void imageFileSelectedWrite(File selection) {
  if (selection == null) {
    println("Window was closed or the user hit cancel.");
    return;      
  }
  String fileName = selection.getAbsolutePath();
  if (selection.getName().indexOf(".png") != selection.getName().length() - 4) {
    fileName += ".png";
  }
  // saveImageToFile(mapImage, fileName);
  save(fileName);
}

public void saveImageToFile(PImage img, String fileName) {
  img.save(fileName);
}


public class BrushData {
  PABezShape curve;
  PABezShape brush;
  ArrayList<PVector> allPoints;
  ArrayList<PVector> drawPoints;
  ArrayList<PVector> polyPoints;
  int timeStamp;
  int timeOffset;
  ArrayList<Integer>timeArray;
  
  /**
   * @param curve      a Bezier curve, the skeleton for the brush shape
   * @param brush      a Bezier "brushstroke"
   * @param poly      the Bezier skeleton as a polyline
   * @param drawPoints  the reduced point set from which the Bezier skeleton was derived
   * @param stamp      initial timestamp in milliseconds from application start
   * @param offset    final time offset, milliseconds from initial timestamp
   * 
   * drawPoints UDP format: 
   */
  public BrushData(PABezShape curve, PABezShape brush, ArrayList<PVector> poly, ArrayList<PVector> drawPoints, int stamp, int offset) {
    this.curve = curve;
    this.brush = brush;
    this.drawPoints = drawPoints;
    this.polyPoints = poly;
    this.timeStamp = stamp;
    this.timeOffset = offset;
  }

  public ArrayList<PVector> getAllPoints() {
    return allPoints;
  }

  public void setAllPoints(ArrayList<PVector> allPoints) {
    this.allPoints = allPoints;
  }

  public ArrayList<Integer> getTimeArray() {
    return timeArray;
  }

  public void setTimeArray(ArrayList<Integer> timeArray) {
    this.timeArray = timeArray;
  }
  
}
