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
PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;
int[] colors;              // array of spectral colors


public void settings() {
  size(3 * genWidth, 2 * genHeight);
}

public void setup() {
  frameRate(30);
  pixelaudio = new PixelAudio(this);
  multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
  mapper = new PixelAudioMapper(multigen);
  mapSize = mapper.getSize();
  colors = getColors(mapSize); // create an array of rainbow colors
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
public void mousePressed() {
  println("mousePressed:", mouseX, mouseY);
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
