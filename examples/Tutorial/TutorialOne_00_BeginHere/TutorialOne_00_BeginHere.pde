/**
 * TutorialOne_00_BeginHere
 * 
 * This tutorial give you the bare bones for a PixelAudio application. It shows up in all 
 * the tutorials that follow. The fundamental concept of PixelAudio is that the pixels in
 * an image and the samples in an audio signal can be put into one-to-one correspondence. 
 * In other words, there are just as many pixels as there are samples, and every pixel has
 * exactly one audio sample that corresponds to it.
 * 
 * Pixels have a standard order that is probably already familiar to you: left-to-right, 
 * top-to-bottom, like reading a book in the English language. Pixel are ordered in
 * two dimensions because images are two-dimensional. Audio signals have one dimension: time. 
 * In PixelAudio, we imagine that a one-to-one correspondence between pixels and samples
 * can be rendered as a path that visits each pixel exactly once. PixelAudio refers to 
 * this path as the "signal path". The signal path might be a zigzag line, from corner 
 * to corner of an image, or a fractal labyrinth like a Hilbert curve, or even a random
 * walk that bumps into every pixel once only. 
 * 
 * PixelAudio provides an abstract class that maps pixels and samples back and forth: 
 * PixelMapGen. With a PixelMapGen (or "gen", for short) you can find the pixel that 
 * corresponds to any sample and the sample that corresponds to any pixel. PixelMapGens
 * can be combined into larger structures, MultiGens. It's all done with arrays, and 
 * for the most part you don't need to be concerned with how it works. See the 
 * LookupTables and MultiGenLookupTables examples for a details.  
 * 
 * The actual work of moving between audio signal and image pixels is done by another 
 * PixelAudio class, PixelAudioMapper. A PixelAudioMapper instance (or "mapper") is 
 * initialized with a PixelMapGen. It can copy values back and forth between audio
 * signals and images. Because samples and pixels are very different numeric formats,
 * PixelAudioMapper also provides a big toolkit for transcoding audio samples, which are
 * floating point numbers between -1.0 and 1.0, and RGB or HSB color values. RGB data is
 * encoded in 24-bit integers, where each channel has 8 bits of data. Another 8 bits are
 * used as an alpha channel, which can be through of as opacity. These features are covered
 * in the TutorialOne sequence and in many of the sample sketches that come with PixelAudio. 
 * 
 */


//Mama's ever-lovin' blue-eyed PixelAudio library
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;

// PixelAudio vars and objects
PixelAudio pixelaudio;     // our shiny new library
MultiGen multigen;         // a PixelMapGen that links together multiple PixelMapGens
int genWidth = 512;        // width of multigen PixelMapGens, must be a power of 2
int genHeight = 512;       // height of  multigen PixelMapGens, must equal genWidth
PixelAudioMapper mapper;   // object for reading, writing, and transcoding audio and image data
int mapSize;               // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
PImage mapImage;           // image for display
// PixelAudioMapper.ChannelNames is an enum whose values can be used to refer to color channels
PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;    
int[] colors;              // array of spectral colors


public void settings() {
  size(3 * genWidth, 2 * genHeight);
}

public void setup() {
  frameRate(30);
  // initialize the PixelAudio library
  pixelaudio = new PixelAudio(this);
  // create PixelMapGen object -- in this case, one made of six Hilbert curves
  multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
  // use the gen to create a PixelAudioMapper instance
  mapper = new PixelAudioMapper(multigen);
  // it's really useful to keep track of the size of the image, which is also the length of the audio signal
  mapSize = mapper.getSize();
  // we generate an array of rainbow colors to fill the image, following the signal path
  colors = getColors(mapSize); 
  // and we prepare to draw to the screen
  initImages();
}

public int[] getColors(int size) {
  int[] colorWheel = new int[size]; // an array for our colors
  pushStyle(); // save styles
  colorMode(HSB, colorWheel.length, 100, 100); // pop over to the HSB color space and give hue a very wide range
  for (int i = 0; i < colorWheel.length; i++) {
    colorWheel[i] = color(i, 50, 70); // fill our array with colors, gradually changing hue
  }
  popStyle(); // restore styles, including the default RGB color space
  return colorWheel;
}

public void initImages() {
  mapImage = createImage(width, height, ARGB); // an image to use with mapper
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
  mapImage.updatePixels();
}

public void draw() {
  image(mapImage, 0, 0);
}

/**
 * The built-in mousePressed handler for Processing
 */
public void mousePressed() {  // println("mousePressed:", mouseX, mouseY);
  int samplePos = mapper.lookupSample(mouseX, mouseY);
  int sample = colors[samplePos];
  // get the color value
  println("-- "+ PixelAudioMapper.colorString(sample) +" at signal path index "+ samplePos 
          +" and image location "+ mouseX +", "+ mouseY);
}

/**
 * The built-in keyPressed handler for Processing
 */
public void keyPressed() {
  switch(key) {
  case 'h': case 'H': // show helpp text and key commands
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" no help ");
}
