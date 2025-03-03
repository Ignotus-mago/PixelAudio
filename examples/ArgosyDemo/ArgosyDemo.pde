
/**
 * ArgosyDemo shows some features of the Argosy class.
 * The Argosy class create pixel patterns ordered by the signal path of a PixelAudioMapper.
 * Argosy patterns consist of an array of numbers, an array of colors, a gap between patterns
 * and a color for the gap. Each number in the pattern array determines the length of a run
 * of pixels in a color specified by the color array. The pattern numbers are scaled by the 
 * argosyGapScale value and the argosyUnitSize value. Experiment with changing them to see 
 * what happens. 
 * 
 * Press 'l' to shift pattern left by argosy.animStep pixels.
 * Press 'r' to shift pattern right by argosy.animStep pixels.
 * Press 'j' to shift pattern left the length of one argosy pattern .
 * Press 'J' to shift pattern right the length of one argosy pattern .
 * Press 'k' to shift pattern left the length of one argosy pattern + gap.
 * Press 'K' to shift pattern right the length of one argosy pattern + gap.
 * Press 'u' to shift pattern left one argosy unit.
 * Press 'U' to shift pattern right one argosy unit.
 * Press 'z' to shift pattern right accumulated pixel count (reset to original state).
 * Press 's' to save current display image to a file as a PNG.
 * Press keys 1, 2, 3 to change colors.
 * Press keys 4, 5, 6 to change patterns.
 * Press keys 7, 8, 9 to change space between patterns.
 * Press key 0 to change background.
 * Press 'h' to show key press help in the console.
 * 
 */

import java.io.File;
// import java.util.ArrayList;

import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;


// PixelAudio vars and objects
PixelAudio pixelaudio;    // our shiny new library
MultiGen multigen;        // a PixelMapGen that handles multiple gens
int genWidth = 256;       // width of PixelMapGen objects, for Hilbert curves must be a power of 2
int genHeight = 256;      // height of PixelMapGen objects, for  Hilbert curves must be equal to genWidth
int imageWidth = 3 * genWidth;    // scale for the multigen created by loadLoopGen()
int imageHeight = 2 * genHeight;  // scale for the multigen created by loadLoopGen()
PixelAudioMapper mapper;  // object for reading, writing, and transcoding audio and image data
int mapSize;              // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
PImage mapImage;          // image for display
PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;
int[] colors;             // array of spectral colors
// colors
int roig = 0xfff64c2f; int groc = 0xfff6e959; int blau = 0xff5990e9; int blau2 = 0xff90b2dc; int blanc = 0xfffef6e9;
int vert = 0xff7bb222; int taronja = 0xfffea537; int roigtar = 0xffE9907B; int violet = 0xffb29de9; int negre = 0xff080d15;
// Argosy variants
int[] pattern01 = {1, 2, 3, 4, 5};
int[] pattern02 = {2, 4, 8};
int[] pattern03 = {7, 11, 5, 3};
int[] colors01  = {color(246, 246, 233, 255), color(34, 55, 89, 255)};
int[] colors02  = {color(246, 246, 233, 192), color(34, 55, 89, 192), color(123, 131, 144, 255)};
int[] colors03  = {violet, blau, vert, roig, taronja, groc};
boolean isBlackBackground = false;
// Argosy vars
Argosy argosy;
int argosyUnitSize = 256;    // values divisible by two bring symmetry, prime numbers create asymmetry
int argosyReps = 0;
boolean isCentered = false;
int[] argosyColors = colors01;
int argosyGapColor = color(21, 21, 21, 16);
float argosyGapScale = 2;    // change the size of the gap (argosyUnitSize * argosyGapScale) 
int[] argosyPattern = pattern01;
int argosyPixelCount;
PImage argosyImage;


public void settings() {
  size(imageWidth, imageHeight);
}

public void setup() {
  pixelaudio = new PixelAudio(this);
  // hGen = new HilbertGen(genWidth, genHeight);
  multigen = loadLoopGen(genWidth, genHeight);
  mapper = new PixelAudioMapper(multigen);
  mapSize = mapper.getSize();
  mapImage = createImage(width, height, ARGB);
  argosyImage = createImage(width, height, ARGB);
  colors = getColors(mapSize); // create an array of rainbow colors, useful for visualizing the signal path
  displayColors();
  argosy = getArgosy();
  argosyPixelCount =  argosy.getArgosySize() * argosy.getUnitSize();
  println("argosyMargin = "+ argosy.getArgosyMargin());
  println("argosySize = "+ argosy.getArgosySize());
  println("argosy pixel count = "+ argosyPixelCount);
  displayArgosy();
  showHelp();
}

/**
 * Adds PixelMapGen objects to the local variable genList. The genList 
 * initializes a MultiGen, which can be used to map audio and pixel data.
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
public int[] getColors(int size) {
  int[] colorWheel = new int[size]; // an array for our colors
  pushStyle(); // save styles
  colorMode(HSB, colorWheel.length, 100, 100); // pop over to the HSB color space and give hue a very wide range
  for (int i = 0; i < colorWheel.length; i++) {
    colorWheel[i] = color(i, 60, 75); // fill our array with colors, gradually changing hue
  }
  popStyle(); // restore styles, including the default RGB color space
  return colorWheel;
}

public void displayColors() {
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize, chan); // load colors to mapImage following signal path
  mapImage.updatePixels();
}

public Argosy getArgosy() {
  return new Argosy(mapper, argosyUnitSize, argosyReps, isCentered, argosyColors, argosyGapColor, argosyGapScale, argosyPattern);
}

public void displayArgosy() {
  argosyImage.loadPixels();
  mapper.plantPixels(argosy.getArgosyArray(), argosyImage.pixels, 0, mapSize, chan);
  argosyImage.updatePixels();
}
public void draw() {
  background(127);
  image(mapImage, 0, 0);
  if (isBlackBackground) background(0);
  // The call to displayArgosy could be deleted here and called only
  // after changes to argosy, i.e., after most of the calls in keyPressed(). 
  // That would be more efficient, but require more attention to detail. 
  displayArgosy();
  image(argosyImage, 0, 0);
}

public void keyPressed() {
  switch (key) {
  case 'l': {
    argosy.shiftLeft();    // shift pattern left by argosy.animStep pixels
    break;
  }
  case 'r': {
    argosy.shiftRight();  // shift pattern right by argosy.animStep pixels
    break;
  }
  case 'j': {
    argosy.shift(argosyPixelCount, true);  // shift pattern left the length of one argosy pattern 
    break;
  }
  case 'J': {
    argosy.shift(-argosyPixelCount, true);  // shift pattern right the length of one argosy pattern 
    break;
  }
  case 'k': {
    argosy.shift(argosyPixelCount + argosy.getArgosyGap(), true);  // shift pattern left the length of one argosy pattern + gap
    break;
  }
  case 'K': {
    argosy.shift(-(argosyPixelCount + argosy.getArgosyGap()), true);  // shift pattern right the length of one argosy pattern + gap
    break;
  }
  case 'u': {
    argosy.shift(argosyUnitSize, true);    // shift pattern left one argosy unit
    break;
  }
  case 'U': {
    argosy.shift(-argosyUnitSize, true);  // shift pattern right one argosy unit
    break;
  }
  case 'z': {
    int shift = argosy.getArgosyPixelShift();  // shift pattern right accumulated pixel count (reset to original state)
    argosy.shift(-shift, true);
    break;
  }
  case 's': {
    saveImage();  // save current display image to a file as a PNG
    break;
  }
  case '1': {
    argosyColors = colors01;
    argosy = getArgosy();
    break;
  }
  case '2': {
    argosyColors = colors02;
    argosy = getArgosy();
    break;
  }
  case '3': {
    argosyColors = colors03;
    argosy = getArgosy();
    break;
  }
  case '4': {
    argosyPattern = pattern01;
    argosy = getArgosy();
    break;
  }
  case '5': {
    argosyPattern = pattern02;
    argosy = getArgosy();
    break;
  }
  case '6': {
    argosyPattern = pattern03;
    argosy = getArgosy();
    break;
  }
  case '7': {
    argosyGapScale = 1;
    argosy = getArgosy();
    break;
  }
  case '8': {
    argosyGapScale = 17;
    argosy = getArgosy();
    break;
  }
  case '9': {
    argosyGapScale = 4.5f;
    argosy = getArgosy();
    break;
  }
  case '0': {
    isBlackBackground = !isBlackBackground;
    break;
  }
  case 'h': {
    showHelp();  // save current display image to a file as a PNG
    break;
  }
  default: {
    break;
  }
  }
}

public void showHelp() {
  println();
  println(" * Press 'l' to shift pattern left by argosy.animStep pixels.");
  println(" * Press 'r' to shift pattern right by argosy.animStep pixels.");
  println(" * Press 'j' to shift pattern left the length of one argosy pattern .");
  println(" * Press 'J' to shift pattern right the length of one argosy pattern .");
  println(" * Press 'k' to shift pattern left the length of one argosy pattern + gap.");
  println(" * Press 'K' to shift pattern right the length of one argosy pattern + gap.");
  println(" * Press 'u' to shift pattern left one argosy unit.");
  println(" * Press 'U' to shift pattern right one argosy unit.");
  println(" * Press 'z' to shift pattern right accumulated pixel count (reset to original state).");
  println(" * Press 's' to save current display image to a file as a PNG.");
  println(" * Press keys 1, 2, 3 to change colors.");
  println(" * Press keys 4, 5, 6 to change patterns.");
  println(" * Press keys 7, 8, 9 to change space between patterns.");
  println(" * Press key 0 to change background.");
  println(" * Press 'h' to show key press help in the console.");
}

// ------------- SAVE IMAGE FILE ------------- //

public void saveImage() {
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
  // saveImageToFile(mapImage, fileName);      // save mapImage
  // saveImageToFile(argosyImage, fileName);    // save argosyImage
  // save(fileName);                // save display
  saveImageToFile(drawOffscreen(), fileName);
}

public void saveImageToFile(PImage img, String fileName) {
  img.save(fileName);
}

public PImage drawOffscreen() {
  displayColors();
  displayArgosy();
  PGraphics offscreen = createGraphics(width, height);
  offscreen.beginDraw();
  offscreen.background(127);
  offscreen.image(mapImage, 0, 0);
  offscreen.image(argosyImage, 0, 0);
  offscreen.endDraw();
  return offscreen.get();
}
