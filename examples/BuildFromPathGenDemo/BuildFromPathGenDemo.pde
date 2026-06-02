/**
 * BuildFromPathGenDemo shows how PixelMapGen data can be stored in and
 * loaded from a JSON file.
 *
 * There are three JSON files available in the data folder of this sketch:
 * vertical.json, bigDiagonal.json, multigen.json.
 * This sketch provides various methods for reading and writing JSON files.
 * The JSON file should contain a "header" with fields "PXAU", "Description" and "PixelAudioURL".
 * The value of the PXAU field is set to "BGEN" to identify the JSON data as encoding a PixelMapGen.
 * The PXAU field is repeated in the body of the JSON file, which must contain the following fields:
 *
 *   "PXAU": "BGEN"
 *   "width": <PixelMapGen width>
 *   "height": <PixelMapGen height>
 *   "pixelMap": [...]
 *
 * "pixelMap" flags an array of integers that are the values of the indices of the signal path,
 * signalToImageLUT, from a PixelMapGen. The signalToImageLUT values can be decoded to (x, y)
 * coordinates and used to initialize the remaining fields of a BuildFromPathGen. Most of the
 * work gets done by calling importGenDataJSON(JSONObject json) with the data loaded from the
 * JSON file. The type of PixelMapGen created in importGenDataJSON() is a BuildFromPathGen.
 * The BuildFromPathGen constructor requires width and height. To complete initialization,
 * you must call BuildFromPathGen.setPixelMap() with the integer array derived from the JOSN
 * data. Then call BuildFromPathGen.generate() to initialize all remaining variables.
 *
 *     BuildFromPathGen myGen = new BuildFromPathGen(w, h);
 *     int[] pixelMap = map.toIntArray();
 *     // always call BuildFromPathGen.setPixelMap() before you call BuildFromPathGen.generate()
 *     myGen.setPixelMap(pixelMap);
 *     myGen.generate();
 *
 * For most purposes, the BuildFromPathGen can be handled as PixelMapGen. Note that only the
 * signalToImageLUT data is saved to the JSON. Color data and audio samples are external to the
 * PixelMapGen. You can save them to image or audio files and reload them later.
 *
 */

import java.io.File;
import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;
HilbertGen hGen;
MultiGen multigen;
int genWidth = 256;         // width of each gen (must be a power of 2 for HilbertGen
int genHeight = 256;         // height of each gen (must equal width of genO
int rows = 3;             // number of rows of gens
int columns = 2;           // number of columns of gens
PixelAudioMapper mapper;       // PixelAudioMapper to handle mapping of pixels to image

PImage mapImage;           // A PImage to display
PImage baseImage;                   // A PImage for reference
int[] colors;             // an array of color values for pixels
int shift = 512;           // amount to displace pixels for animation
int totalShift = 0;                 // accumulated shift while animating
boolean isAnimating = false;     // well, are we animating or not?
boolean oldIsAnimating;

// for JSON file I/O
String currentFileName;
PixelMapGen currentGen;
String dataFolder = "/Users/paulhz/Code/Workspace/TestProcessing/src/net/paulhertz/testprocessing/data";

public static void main(String[] args) {
  PApplet.main(new String[] { BuildFromPathGenDemo.class.getName() });
}

public void settings() {
  size(rows * genWidth, columns * genHeight);
}

public void setup() {
  windowResizable(true);
  // 1. initialize PixelAudio
  pixelaudio = new PixelAudio(this);
  // 2. create a multigen for immediate display
  multigen = hilbertLoop3x2(genWidth, genHeight);
  currentGen = multigen;
  // 3. initialize a PixelAudioMapper object with the gen
  mapper = new PixelAudioMapper(currentGen);
  // 4. create an image for display
  mapImage = createImage(width, height, RGB);
  // 5. get colors to display on the signal path of mapImage
  initImages();
}

/**
 * Generates a looping fractal signal path consisting of 6 HilbertGens,
 * arranged 3 wide and 2 tall, to fit a 3 * genW by 2 * genH image.
 * This particular MultiGen configuration was used so extensively in
 * my sample code that I've given it its own static method in HilbertGen.
 *
 * Note that genW must be a power of 2 and genH == genW. For the
 * image size we're using in this example, image width = 3 * genW
 * and image height = 2 * genH.
 *
 * @param genW    width of each HilbertGen
 * @param genH    height of each HilbertGen
 * @return
 */
public MultiGen hilbertLoop3x2(int genW, int genH) {
  // list of PixelMapGens that create a path through an image using PixelAudioMapper
  ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
  // list of x,y coordinates for placing gens from genList
  ArrayList<int[]> offsetList = new ArrayList<int[]>();
  genList.add(new HilbertGen(genW, genH, AffineTransformType.FX270));
  offsetList.add(new int[] { 0, 0 });
  genList.add(new HilbertGen(genW, genH, AffineTransformType.NADA));
  offsetList.add(new int[] { genW, 0 });
  genList.add(new HilbertGen(genW, genH, AffineTransformType.FX90));
  offsetList.add(new int[] { 2 * genW, 0 });
  genList.add(new HilbertGen(genW, genH, AffineTransformType.FX90));
  offsetList.add(new int[] { 2 * genW, genH });
  genList.add(new HilbertGen(genW, genH, AffineTransformType.R180));
  offsetList.add(new int[] { genW, genH });
  genList.add(new HilbertGen(genW, genH, AffineTransformType.FX270));
  offsetList.add(new int[] { 0, genH });
  return new MultiGen(3 * genW, 2 * genH, offsetList, genList);
}

public int[] getColors() {
  int[] colorWheel = new int[mapper.getSize()];
  pushStyle();
  colorMode(HSB, colorWheel.length, 100, 100);
  int h = 0;
  for (int i = 0; i < colorWheel.length; i++) {
    colorWheel[i] = color(h, 66, 66);
    h++;
  }
  popStyle();
  return colorWheel;
}

/**
 * Initializes mapImage with the colors array, copies mapImage to baseImage.
 * MapImage handles the color data for mapper and also serves as our display image.
 * BaseImage is intended as a reference image that typically only changes when you load a new image.
 */
public void initImages() {
  mapImage = createImage(width, height, ARGB);
  mapImage.loadPixels();
  colors = getColors();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapper.getSize());
  mapImage.updatePixels();
  baseImage = mapImage.copy();
  totalShift = 0;
}

public void draw() {
  image(mapImage, 0, 0, width, height);
  if (isAnimating)
    animate();
}

public void keyPressed() {
  switch (key) {
  case ' ': // toggle animation
    isAnimating = !isAnimating;
    break;
  case 'j': // export signal path to a JSON file
    exportGenData(currentGen);
    break;
  case 'o': // import signal path from a JSON file
    importGenData();
    break;
  case 'h': // show help message in the console
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press ' ' to toggle animation.");
  println(" * Press 'j' to export signal path to a JSON file.");
  println(" * Press 'o' to import signal path from a JSON file.");
  println(" * Press 'h' to show help message in the console.");
}

public void animate() {
  totalShift += shift;
  mapImage.loadPixels();
  // PixelAudioMapper has the method we need
  mapper.copyPixelsAlongPathShifted(baseImage.pixels, mapImage.pixels, totalShift);
  mapImage.updatePixels();
}
