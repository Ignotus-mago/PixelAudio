/**
 * LoadImageToAudio shows how to load an image and turn it into an audio file
 * that can be played by clicking on the image. You can also load an image file
 * and turn it into an audio signal. It will probably be noisy, both because it's
 * an image and because its resolution is only 8 bits. When you load an audio, it
 * will exist both as a floating point audio signal and as an image. When you
 * click in the image, you will be playing a sample from the signal. You can write
 * the image to the audio signal ('w' key command).
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
 * Press 'o' to load an image or audio file to all color channels.
 * Press 'r' to load an image or audio file to the red color channel.
 * Press 'g' to load an image or audio file to the green color channel.
 * Press 'b' to load an image or audio file to the blue color channel.
 * Press 'h' to load an image or audio file to the HSB hue channel.
 * Press 'v' to load an image or audio file to the HSB saturation (vibrance) channel.
 * Press 'l' to load an image or audio file to the HSB brightness (lightness) channel.
 * Press 'O' to reload the most recent audio or image file.
 * Press 'm' to apply a contrast enhancement (histogram stretch) to the image.
 * Press '=' or '+' to make the image brighter
 * Press '-' or '_' to make the image darker.
 * Press 's' to save to an audio file.
 * Press 'S' to save to an image file.
 * Press 'f' to show frameRate in the console.
 * Press 'w' to write the image to the audio buffer (expect noise)
 * Press '?' to show this help message.
 *
 * PLEASE NOTE: Hue (H) and Saturation (V) operations may have no effect on gray pixels.
 * ALSO: Image brightness determines image audio. Images with uniform brightness will be silent.
 *
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import javax.sound.sampled.*;
import java.util.Timer;
import java.util.TimerTask;

import ddf.minim.*;
import ddf.minim.ugens.*;

import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;

// oscP5
import oscP5.*;
import netP5.*;

// PixelAudio vars and objects
PixelAudio pixelaudio;     // our shiny new library
HilbertGen hGen;           // a PixelMapGen to draw Hilbert curves
MultiGen multigen;         // a PixelMapGen that handles multiple gens
ArrayList<PixelMapGen> genList;    // list of PixelMapGens that create an image using mapper
ArrayList<int[]> offsetList;       // list of x,y coordinates for placing gens from genList
int rows = 3;
int columns = 2;
int genWidth = 512;       // width of PixelMapGen objects, for hGen must be a power of 2
int genHeight = 512;      // height of PixelMapGen objects, for hGen must be equal to width
PixelAudioMapper mapper;  // object for reading, writing, and transcoding audio and image data
int mapSize;              // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
PImage mapImage;          // image for display
PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;
int[] colors;             // array of spectral colors

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
SamplerInstrument instrument;      // local class to wrap audioSampler
// ADSR and params
ADSR adsr;                // good old attack, decay, sustain, release
float maxAmplitude = 0.9f;
float attackTime = 0.2f;
float decayTime = 0.125f;
float sustainLevel = 0.5f;
float releaseTime = 0.2f;

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

boolean isLoadFromImage = false;

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

// simple RDP drawing 
public ArrayList<PVector> allPoints = new ArrayList<PVector>();
public ArrayList<PVector> drawPoints = new ArrayList<PVector>();
float epsilon = 0;
PVector currentPoint;

// OSC protocol
OscP5 osc;
int inPort = 7401;
int outPort = 7400;
NetAddress remoteFrom;
NetAddress remoteTo;


public void settings() {
  size(rows * genWidth, columns * genHeight);
}

public void setup() {
  frameRate(30);
  pixelaudio = new PixelAudio(this);
  minim = new Minim(this);
  // sampleRate affects image display and audio sample calculation.
  // For compatibility with other applications, including Processing, it's a good
  // idea to use a standard sampling rate, like 44100. However, you can experiment
  // with other sampling rates and probably can play audio and and save files.
  // Their behavior in Processing when you try to open them can be unpredictable.
  //
  // Setting sampleRate = genWidth * genHeight provides interesting symmetries in
  // the image
  // and will play audio and save to file -- but it's not a standard sample rate
  // and
  // though Processing may open files saved with non-standard sampling rates, it
  // usually shifts the frequency according the sampleRate you have set.
  sampleRate = 44100; // = genWidth * genHeight;
  sampleBase = sampleRate / 4;
  initAudio();
  genList = new ArrayList<PixelMapGen>();
  offsetList = new ArrayList<int[]>();
  loadGenLists();
  multigen = new MultiGen(rows * genWidth, columns * genHeight, offsetList, genList);
  mapper = new PixelAudioMapper(multigen);
  mapSize = mapper.getSize();
  colors = getColors(); // create an array of rainbow colors
  mapImage = createImage(width, height, ARGB); // an image to use with mapper
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
  mapImage.updatePixels();
  timeLocsArray = new ArrayList<TimedLocation>();
  // simple drawing init
  epsilon = 2.0f;
  currentPoint = new PVector(-1, -1);
  // OSC init
  osc = new OscP5(this, inPort);
  remoteFrom = new NetAddress("127.0.0.1", inPort);
  remoteTo = new NetAddress("127.0.0.1", outPort);
  initOSCPlugs();
  showHelp();
}

public void initAudio() {
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  this.audioBuffer = new MultiChannelBuffer(1024, 1);
}

/**
 * Adds PixelMapGen objects to the genList. The genList will be used to
 * initialize a MultiGen, which in turn is passed to a WaveSynth.
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

public void initOSCPlugs() {
  osc.plug(this, "sampleHit", "/sampleHit");
}

public void draw() {
  image(mapImage, 0, 0);
  if (isAnimating)
    stepAnimation();
  drawPoints();
  runTimeArray();
}

public void stepAnimation() {
  mapImage.loadPixels();
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
  PixelAudioMapper.rotateLeft(rgbSignal, 16);
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, mapSize);
  mapImage.updatePixels();
}

public void drawPoints() {
  if (allPoints.size() > 0) {
    stroke(233, 144, 89, 127);
    strokeWeight(5);
    noFill();
    beginShape();
    for (PVector vec : allPoints) {
      vertex(vec.x, vec.y);
    }
    endShape();
  }
  if (drawPoints.size() > 0) {
    stroke(144, 89, 233);
    strokeWeight(2);
    noFill();
    beginShape();
    for (PVector vec : drawPoints) {
      vertex(vec.x, vec.y);
    }
    endShape();
  }
}

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
  case 'o':
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
  case 'O':
    if (audioFile == null && imageFile == null) {
      chooseFile();
    } else {
      if (isLoadFromImage) {
        // reload image
        loadImageFile(imageFile);
        println("-------->>>>> Reloaded image file");
      } else {
        // reload image
        loadAudioFile(audioFile);
        println("-------->>>>> Reloaded audio file");
      }
    }
    break;
  case 'm':
    mapImage.loadPixels();
    int[] bounds = getHistoBounds(mapImage.pixels);
    mapImage.pixels = stretch(mapImage.pixels, bounds[0], bounds[1]);
    mapImage.updatePixels();
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
  case '?':
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press ' ' to toggle animation.");
  println(" * Press 'o' to load an image or audio file to all color channels.");
  println(" * Press 'r' to load an image or audio file to the red color channel.");
  println(" * Press 'g' to load an image or audio file to the green color channel.");
  println(" * Press 'b' to load an image or audio file to the blue color channel.");
  println(" * Press 'h' to load an image or audio file to the HSB hue channel.");
  println(" * Press 'v' to load an image or audio file to the HSB saturation (vibrance) channel.");
  println(" * Press 'l' to load an image or audio file to the HSB brightness (lightness) channel.");
  println(" * Press 'O' to reload the most recent audio or image file.");
  println(" * Press 'm' to apply a contrast enhancement (histogram stretch) to the image.");
  println(" * Press '=' or '+' to make the image brighter");
  println(" * Press '-' or '_' to make the image darker.");
  println(" * Press 's' to save to an audio file.");
  println(" * Press 'S' to save to an image file.");
  println(" * Press 'f' to show frameRate in the console.");
  println(" * Press 'w' to write the image to the audio buffer (expect noise)");
  println(" * Press '?' to show this help message.");
  println(" * ");
  println(" * PLEASE NOTE: Hue (H) and Saturation (V) may have no effect on gray pixels.");
  println(" * ALSO: Image brightness determines image audio. Images with uniform brightness will be silent.");

}

public void mousePressed() {
  sampleX = mouseX;
  sampleY = mouseY;
  samplePos = mapper.lookupSample(sampleX, sampleY);
  if (audioSignal == null || isBufferStale) {
    isBufferStale = false;
  }
  playSample(samplePos);
  allPoints.clear();
  addPoint();
  // osc.send(new OscMessage("/press").add(mapper.lookupSample(sampleX, sampleY)), remoteTo);
  oscSendMousePressed(remoteTo);
}

public void mouseDragged() {
  addPoint();
}

public void mouseReleased() {
  reducePoints();
  oscSendDrawPoints(remoteTo);
  printSizes();
}

/* incoming osc message are forwarded to the oscEvent method. */
void oscEvent(OscMessage theOscMessage) {
  /* print the address pattern and the typetag of the received OscMessage */
  print("### received an osc message.");
  print(" addrpattern: "+theOscMessage.addrPattern());
  println(" typetag: "+theOscMessage.typetag());
}

public void oscSendMousePressed(NetAddress dest) {
  OscMessage msg = new OscMessage("/press");
  msg.add(mapper.lookupSample(sampleX, sampleY));
  msg.add(sampleX);
  msg.add(sampleY);
  osc.send(msg, dest);
}

public void oscSendDrawPoints(NetAddress dest) {
  OscMessage msg = new OscMessage("/draw");
  for (PVector vec : drawPoints) {
    msg.add(mapper.lookupSample((int)vec.x, (int)vec.y));
  }
  osc.send(msg, dest);
}  
  
public void sampleHit(int sam) {
  int[] xy = mapper.lookupCoordinate(sam);
  println("---> sampleHit "+ xy[0], xy[1]);
  playSample(sam);
}
  


// ************* POINT AND MOUSE TRACKING ************* //

public void addPoint() {
  if (mouseX != currentPoint.x && mouseY != currentPoint.y) {
    currentPoint = new PVector(mouseX, mouseY);
    allPoints.add(currentPoint);
  }
}

public void printSizes() {
  int allSize = allPoints.size();
  int drawSize = drawPoints.size();
  float percent = (drawSize * 100.0f) / allSize;
  println("For epsilon of " + nf(epsilon, 0, 2) + ": all points: " + allSize + ", reduced points: " + drawSize
      + ", " + nf(percent, 0, 2) + "% reduction.");
}

public void reducePoints() {
  drawPoints.clear();
  int total = allPoints.size();
  PVector start = allPoints.get(0);
  PVector end = allPoints.get(total - 1);
  drawPoints.add(start);
  rdp(0, total - 1, allPoints, drawPoints);
  drawPoints.add(end);
}

/* ------------- BEGIN CODE FROM CODING TRAIN ------------- */

void rdp(int startIndex, int endIndex, ArrayList<PVector> allPoints, ArrayList<PVector> rdpPoints) {
  int nextIndex = findFurthest(allPoints, startIndex, endIndex);
  if (nextIndex > 0) {
    if (startIndex != nextIndex) {
      rdp(startIndex, nextIndex, allPoints, rdpPoints);
    }
    rdpPoints.add(allPoints.get(nextIndex));
    if (endIndex != nextIndex) {
      rdp(nextIndex, endIndex, allPoints, rdpPoints);
    }
  }
}

int findFurthest(ArrayList<PVector> points, int a, int b) {
  float recordDistance = -1;
  PVector start = points.get(a);
  PVector end = points.get(b);
  int furthestIndex = -1;
  for (int i = a + 1; i < b; i++) {
    PVector currentPoint = points.get(i);
    float d = lineDist(currentPoint, start, end);
    if (d > recordDistance) {
      recordDistance = d;
      furthestIndex = i;
    }
  }
  if (recordDistance > epsilon) {
    return furthestIndex;
  } else {
    return -1;
  }
}

float lineDist(PVector c, PVector a, PVector b) {
  PVector norm = scalarProjection(c, a, b);
  return PVector.dist(c, norm);
}

PVector scalarProjection(PVector p, PVector a, PVector b) {
  PVector ap = PVector.sub(p, a);
  PVector ab = PVector.sub(b, a);
  ab.normalize(); // Normalize the line
  ab.mult(ap.dot(ab));
  PVector normalPoint = PVector.add(a, ab);
  return normalPoint;
}

/* ------------- END CODE FROM CODING TRAIN ------------- */


public int playSample(int samplePos) {
  audioSampler = new Sampler(audioBuffer, sampleRate, 8); // create a Minim Sampler from the buffer sampleRate sampling rate, for up to 8 simultaneous outputs
  audioSampler.amplitude.setLastValue(0.9f);              // set amplitude for the Sampler
  audioSampler.begin.setLastValue(samplePos);             // set the Sampler to begin playback at samplePos, which corresponds to the place the mouse was clicked
  int releaseDuration = (int) (releaseTime * sampleRate); // do some calculation to include the release time.
  float vary = (float) (PixelAudio.gauss(this.sampleScale, this.sampleScale * 0.125f)); // vary the duration of the signal
  // println("----->>> vary = "+ vary +", sampleScale = "+ sampleScale);
  this.samplelen = (int) (vary * this.sampleBase);        // calculate the duration of the sample
  if (samplePos + samplelen >= mapSize) {
    samplelen = mapSize - samplePos;                      // make sure we don't exceed the mapSize
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
  float duration = samplelen / (float) (sampleRate);
  instrument.play(duration);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, (int) (duration * 1000) + millis()));
  // return the length of the sample
  return samplelen;
}


public void writeImageToAudio() {
  println("----- writing image to signal ");
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
  audioBuffer.setBufferSize(mapSize);
  mapImage.loadPixels();
  // fetch pixels from mapImage in signal order, put them in rgbSignal
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, rgbSignal.length);
  // write the Brightness channel of rgbPixels, transcoded to audio range, to audioBuffer
  mapper.plantSamples(rgbSignal, audioBuffer.getChannel(0), 0, mapSize, PixelAudioMapper.ChannelNames.L);
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
//             TIMED LOCATION CLASS       //
// ------------------------------------------- //

public class TimedLocation {
  private int x;
  private int y;
  private int stopTime;
  private boolean isStale;

  public TimedLocation(int x, int y, int stop) {
    this.x = x;
    this.y = y;
    this.stopTime = stop;
    this.isStale = false;
  }

  public int getX() {
    return this.x;
  }

  public int getY() {
    return this.y;
  }

  public int stopTime() {
    return this.stopTime;
  }

  public boolean isStale() {
    return this.isStale;
  }

  public void setStale(boolean stale) {
    this.isStale = stale;
  }
}


// ------------------------------------------- //
//       SAMPLER INSTRUMENT CLASS       //
// ------------------------------------------- //

// using minim's Instrument interface
public class SamplerInstrument implements Instrument {
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
      }, durationMillis);
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
      isLoadFromImage = false;
    } 
    else if (fileTag.equalsIgnoreCase("png") || fileTag.equalsIgnoreCase("jpg") || fileTag.equalsIgnoreCase("jpeg")) {
      imageFile = selectedFile;
      imageFilePath = filePath;
      imageFileName = fileName;
      imageFileTag = fileTag;
      loadImageFile(imageFile);
      isLoadFromImage = true;
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
    // load rgbSignal with rgb gray values corresponding to the audio sample values
    rgbSignal = mapper.pluckSamplesAsRGB(audioSignal, 0, mapSize);
    if (rgbSignal.length < mapSize) {
      // pad rgbSignal with 0's if necessary
      rgbSignal = Arrays.copyOf(rgbSignal, mapSize);
    }
    mapImage.loadPixels();
    // write the rgbSignal pixels to mapImage, following the signal path
    mapper.plantPixels(rgbSignal, mapImage.pixels, 0, rgbSignal.length, chan);
    mapImage.updatePixels();
  }
}

public void loadImageFile(File imgFile) {
  PImage img = loadImage(imgFile.getAbsolutePath());
  // TODO handle color channel setting for images
  int w = img.width > mapImage.width ? mapImage.width : img.width;
  int h = img.height > mapImage.height ? mapImage.height : img.height;
  if (chan != PixelAudioMapper.ChannelNames.ALL) {
    PImage mixImage = createImage(w, h, RGB);
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
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
  audioBuffer.setBufferSize(mapSize);
  writeImageToAudio();
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

// ------------- IMAGES ------------- //

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
  saveImageToFile(mapImage, fileName);
}

public void saveImageToFile(PImage img, String fileName) {
  img.save(fileName);
}
