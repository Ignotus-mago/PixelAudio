import java.util.Random;
import java.util.Arrays;
import java.io.File;

import net.paulhertz.pixelaudio.*;

import ddf.minim.*;
import ddf.minim.ugens.*;

/** PixelAudio library */
PixelAudio pixelaudio;
HilbertGen hGen;
PixelAudioMapper mapper;
int mapSize;
PImage mapImage;
int[] colors;
PixelAudioMapper.ChannelNames chan;

/** Minim audio library */
Minim minim;
AudioOutput audioOut;
MultiChannelBuffer audioBuffer;
int sampleRate = 44100;
float[] audioSignal;
int[] rgbSignal;
int audioLength;

// SampleInstrument setup
float sampleScale = 2;
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

// audio file
File audioFile;
String audioFilePath;
String audioFileName;
String audioFileTag;

// Java random number generator
Random rando;

// interaction
int pixelPos;
int samplePos;
int blendAlpha = 64;

int histoHigh = 240;
int histoLow = 32;
float gammaUp = 0.9;
float gammaDown = 1.2;
int[] gammaTable;


public void settings() {
  size(1024, 1024);
}

public void setup() {
  initMapper();    // set up mapper and load mapImage with color wheel
  initAudio();     // set up audio
  rando = new Random();
  // load an audio file into the Brightness channel in the HSB color space
  chan = PixelAudioMapper.ChannelNames.L;
  String path = this.dataPath("");
  File audioSource = new File(path +"/youthorchestra.wav");
  // load the file into audio buffer and brightness channel of display image
  fileSelected(audioSource);
}

public void initMapper() {
  pixelaudio = new PixelAudio(this);       // load the PixelAudio library
  hGen = new HilbertGen(width, height);    // create a Hilbert curve that fills our display
  mapper = new PixelAudioMapper(hGen);     // initialize mapper with the HIlbert curve generator
  mapSize = mapper.getSize();              // size of mapper's various arrays and of mapImage
  colors = getColors();                    // create an array of colors
  mapImage = createImage(width, height, ARGB); // an image to use with mapper
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
  mapImage.updatePixels();
}

public void initAudio() {
  this.minim = new Minim(this);
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  this.audioBuffer = new MultiChannelBuffer(1024, 1);
}

public int[] getColors() {
  int[] colorWheel = new int[mapSize];       // an array for our colors
  pushStyle();                   // save styles
  colorMode(HSB, colorWheel.length, 100, 100);   // pop over to the HSB color space and give hue a very wide range
  for (int i = 0; i < colorWheel.length; i++) {
    colorWheel[i] = color(i, 40, 75);       // fill our array with colors, gradually changing hue
  }
  popStyle();                   // restore styles, including the default RGB color space
  return colorWheel;
}

public void draw() {
  image(mapImage, 0, 0);
}

public void keyPressed() {
  switch (key) {
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
  case 'w':
    writeImageToAudio();
    break;
  case 's':
    mapImage.save("pixelAudio.png");
    println("--- saved display image to pixelAudio.png");
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
    // int c = mapImage.get(mouseX, mouseY);
    // String str = PixelAudioMapper.colorString(c);
    // println("--- samplePos:", samplePos, "sampleLength:", sampleLength, "size:", width * height, "end:", samplePos + sampleLength + ", " + str);
  }
}

public void chooseFile() {
  selectInput("Choose an audio file: ", "fileSelected");
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
    } else {
      println("----- File is not a recognized audio format ending with \"mp3\", \"wav\", \"aif\", or \"aiff\".");
    }
  } else {
    println("----- No audio or image file was selected.");
  }
}

public void loadAudioFile(File audioFile) {
  // read audio file into our MultiChannelBuffer
  float sampleRate = minim.loadFileIntoBuffer(audioFile.getAbsolutePath(), audioBuffer);
  if (sampleRate > 0) {                 // sampleRate > 0 means we read audio from the file
    this.audioSignal = audioBuffer.getChannel(0);   // read an array of floats from the buffer
    this.audioLength = audioSignal.length;
    rgbSignal = new int[mapSize];           // create an array the size of mapImage
    rgbSignal = mapper.pluckSamplesAsRGB(audioSignal, 0, mapSize);  // rgbSignal is now an array of rgb gray
    if (rgbSignal.length < mapSize) {
      rgbSignal = Arrays.copyOf(rgbSignal, mapSize); // pad rgbSignal with 0's if necessary
    }
    mapImage.loadPixels();
    mapper.plantPixels(rgbSignal, mapImage.pixels, 0, rgbSignal.length, chan);
    mapImage.updatePixels();
  }
}

public int playSample(int samplePos) {
  if (audioFile == null)
    return 0;
  // create a new ddf.minim.ugens.Sampler from the buffer with 44.1 KHz sampling rate
  // it seems I have to do this every time I want to play a sound, not sure why
  // I can't reuse the Sampler once it's been created -- if I don't the audio 
  // sounds wrong, as if the sampling rate had changed.
  audioSampler = new Sampler(audioBuffer, 44100, 8); 
  // ADSR 
  adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  // println("--- creating sampler ---");
  // set amplitude for the Sampler
  audioSampler.amplitude.setLastValue(0.9f); 
  // set the Sampler to begin playback at samplePos, which corresponds to the place the mouse was clicked
  audioSampler.begin.setLastValue(samplePos); 
  int releaseDuration = (int) (releaseTime * sampleRate); // do some calculation to include the release time.
  // set amplitude for the Sampler using a statistical distribution
  float vary = (float) (gauss(this.sampleScale, this.sampleScale * 0.125f)); 
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
  this.instrument = new SamplerInstrument(audioSampler, adsr);
  // play command takes a duration in seconds
  instrument.play(samplelen / (float) (sampleRate));
  // return the length of the sample
  return samplelen;
}

public void hightlightSample(int pos, int length) {
  shuffle(randColors);
  int highColor = PixelAudioMapper.setAlpha(randColors[0], blendAlpha);
  int[] signalPathPixelSequence = mapper.pluckPixels(mapImage.pixels, pos, samplelen);
  mapImage.loadPixels();
  for (int i = 0; i < length; i++) {
    int newColor = blendColor(mapImage.pixels[pos + i], highColor, BLEND);
    signalPathPixelSequence[i] = newColor;
  }
  mapper.plantPixels(signalPathPixelSequence, mapImage.pixels, pos, length);
  mapImage.updatePixels();
}

public void writeImageToAudio() {
  println("----- writing image to signal ");
  mapImage.loadPixels();
  // fetch pixels from mapImage in signal order, put them in rgbSignal
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, rgbSignal.length);
  // write the Brightness channel of rgbPixels, transcoded to audio range, to audioBuffer
  mapper.plantSamples(rgbSignal, audioBuffer.getChannel(0), 0, mapSize, PixelAudioMapper.ChannelNames.L);
}
