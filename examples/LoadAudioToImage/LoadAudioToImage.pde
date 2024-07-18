import java.util.Random;

import net.paulhertz.pixelaudio.*;
import ddf.minim.*;
import ddf.minim.ugens.*;


PixelAudio pixelaudio;
HilbertGen hGen;
PixelAudioMapper mapper;
int mapSize;
PImage mapImage;
int[] colors;

PixelAudioMapper.ChannelNames chan = PixelAudioMapper.ChannelNames.L;
File audioFile;
String audioFilePath;
String audioFileName;
String audioFileTag;

/** Minim audio library */
Minim minim;
AudioOutput audioOut;
MultiChannelBuffer buf;
int sampleRate = 44100;
float[] audioSignal;
float[] signalImage;
int audioLength;

// SampleInstrument setup
float sampleScale = 2;
int sampleBase = 10250; 
int samplelen = int(sampleScale * sampleBase);
Sampler sam;
SamplerInstrument instrument;
// ADSR and params
ADSR adsr;
float maxAmplitude = 0.9;        
float attackTime   = 0.2;          
float decayTime    = 0.125;           
float sustainLevel = 0.5;        
float releaseTime  = 0.2;    

// Java random number generator
Random rando;

// interaction
int pixelPos;
int samplePos;
int blendAlpha = 64;

public void setup() {
  size(1024, 1024);            // width and height must be equal powers of 2 for the Hilbert curve
  initMapper();
  initAudio();
  rando = new Random();
}

public void initMapper() {
  pixelaudio = new PixelAudio(this);               // load the PixelAudio library
  hGen = new HilbertGen(width, height);            // create a Hilbert curve that fills our display
  mapper = new PixelAudioMapper(hGen);             // initialize mapper with the HIlbert curve generator
  mapSize = mapper.getSize();                      // size of mapper's various arrays and of mapImage
  colors = getColors();                            // create an array of colors
  mapImage = createImage(width, height, RGB);      // an image to use with mapper
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, 0, mapSize);    // load the colors to mapImage following the Hilbert curve (the "signal path" for hGen)
  mapImage.updatePixels();
}

public void initAudio() {
  this.minim = new Minim(this);
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate); 
  this.buf = new MultiChannelBuffer(1024, 1);
}

public int[] getColors() {
  int[] colorWheel = new int[mapSize];              // an array for our colors
  pushStyle();                                      // save styles
  colorMode(HSB, colorWheel.length, 100, 100);      // pop over to the HSB color space and give hue a very wide range
  for (int i = 0; i < colorWheel.length; i++) {
    colorWheel[i] = color(i, 66, 66);               // fill our array with colors of a gradually changing hue
  }
  popStyle();                                       // restore styles, including the default RGB color space
  return colorWheel;
}

public void draw() {
  image(mapImage, 0, 0);
}

public void keyPressed() {
  switch(key) {
  case 'o':
  case 'O':
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
  case 'l':
    chan = PixelAudioMapper.ChannelNames.L;
    chooseFile();
    break;
  case 'h':
    chan = PixelAudioMapper.ChannelNames.H;
    chooseFile();
    break;
  case 's':
    mapImage.save("pixelAudio.png");
    break;
  case '?':
    // showHelp();
    break;
  default:
    break;
  }
}

public void mousePressed() {
  pixelPos = mouseX + mouseY * width;
  samplePos = mapper.lookupSample(mouseX, mouseY);
  // println("----- sample position for "+ mouseX +", "+ mouseY +" is "+ samplePos);
  int sampleLength = playSample(samplePos);
  if (sampleLength > 0) {
    hightlightSample(samplePos, sampleLength);
    println("--- samplePos:", samplePos, "sampleLength:", sampleLength, "size:", width * height, "end:", samplePos + sampleLength);
  }
}

public void chooseFile() {
  selectInput("Choose an audio file: ", "fileSelected");
}

public void fileSelected(File selectedFile) {
  if (null != selectedFile) {
    String filePath = selectedFile.getAbsolutePath();
    String fileName = selectedFile.getName();
    String fileTag = fileName.substring(fileName.lastIndexOf('.')+1);
    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
    if (fileTag.equalsIgnoreCase("mp3") || fileTag.equalsIgnoreCase("wav") || fileTag.equalsIgnoreCase("aif") || fileTag.equalsIgnoreCase("aiff")) {
      audioFile = selectedFile;
      audioFilePath = filePath;
      audioFileName = fileName;
      audioFileTag = fileTag;
      println("----- Selected file "+ fileName +"."+ fileTag +" at "+ filePath.substring(0, filePath.length() - fileName.length()));
      loadAudioFile(audioFile);
    } 
    else {
      println("----- File is not a recognized audio format ending with \"mp3\", \"wav\", \"aif\", or \"aiff\".");
    }
  } 
  else {
    println("----- No audio or image file was selected.");
  }
}

public void loadAudioFile(File audioFile) {
  float sampleRate = minim.loadFileIntoBuffer(audioFile.getAbsolutePath(), buf);      // read audio file into our MultiChannelBuffer
  if (sampleRate > 0) {                                                               // sampleRate > 0 means we read audio from the file
    this.audioSignal = buf.getChannel(0);                                             // read an array of floats from the buffer
    this.audioLength = audioSignal.length;
    signalImage = new float[mapSize];                                                 // create an array the size of mapImage
    for (int i = 0; i < signalImage.length; i++) {                     // copy audio samples to signalImage, padding with 0 if necessary
      if (i < audioLength) {
        signalImage[i] = audioSignal[i];
      }
      else {
        signalImage[i] = 0;
      }
    }
    mapImage.loadPixels();
    mapper.plantPixels(signalImage, mapImage.pixels, 0, 0, signalImage.length, chan);
    mapImage.updatePixels();
  }
}

public int playSample(int samplePos) {
  if (audioFile == null) return 0;
  sam = new Sampler(buf, 44100, 8);           // create a Minim Sampler from the buffer with 44.1 sampling rate, for up to 8 simultaneous outputs
  sam.amplitude.setLastValue(0.9);            // set amplitude for the Sampler
  sam.begin.setLastValue(samplePos);                // set the Sampler to begin playback at samplePos, which corresponds to the place the mouse was clicked         
  int releaseDuration = int(releaseTime * sampleRate);        // do some calculation to include the release time. There may be better ways to do this.
  float vary = (float)(gauss(this.sampleScale, this.sampleScale * 0.125));        // vary the duration of the signal using a statistical distribution function
  // println("----->>> vary = "+ vary +", sampleScale = "+ sampleScale);
  this.samplelen = int(vary * this.sampleBase);                                    // calculate the duration of the sample
  if (samplePos + samplelen >= mapSize) {
    samplelen = mapSize - samplePos;                   // make sure we don't exceed the mapSize
    println("----->>> sample length = "+ samplelen);
  }
  int durationPlusRelease = this.samplelen + releaseDuration;
  int end = (samplePos + durationPlusRelease >= this.mapSize) ? this.mapSize - 1 : samplePos + durationPlusRelease;
  println("----->>> end = "+ end);
  sam.end.setLastValue(end);
  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
  adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  this.instrument = new SamplerInstrument(sam, adsr);
  // play command takes a duration in seconds
  instrument.play(samplelen/float(sampleRate));
  // return the length of the sample
  return samplelen;
}

public void hightlightSample(int pos, int length) {
  shuffle(randColors);
  int highColor = setAlpha(randColors[0], blendAlpha);
  int[] signalPathPixelSequence = mapper.pluckPixels(mapImage.pixels, pos, samplelen);
  mapImage.loadPixels();
  for (int i = 0; i < length; i++) {
    int newColor = blendColor(mapImage.pixels[pos + i], highColor, BLEND);
    signalPathPixelSequence[i] = newColor;
  }
  mapper.plantPixels(signalPathPixelSequence, mapImage.pixels, pos, length);
  mapImage.updatePixels();
}
