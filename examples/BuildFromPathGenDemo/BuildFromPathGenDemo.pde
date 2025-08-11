/**
 * BuildFromPathGenDemo shows how to read and write data from a PixelMapGen in JSON format.
 *
 * We use a MultiGen to get things rolling. See the MultiGenDemo example for more information.
 * A MultiGen is constructed of two or more PixelMapGens that are merged into a single gen.
 *
 * 
 *
 * Press ' ' to toggle animation that shifts pixels along the signal path.
 * Press 'j' to save currentGen data to a .json file.
 * Press 'o' to open and load data from a .json file.
 *
 */

import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;
HilbertGen hGen;
MultiGen multigen;
int genWidth = 256;              // width of each gen (must be a power of 2 for HilbertGen
int genHeight = 256;             // height of each gen (must equal width of genO
int rows = 3;                    // number of rows of gens
int columns = 2;                 // number of columns of gens
PixelAudioMapper mapper;         // PixelAudioMapper to handle mapping of pixels to image

PImage mapImage;                 // A PImage to display
int[] colors;                    // an array of color values for pixels
int shift = 512;                 // amount to displace pixels for animation
boolean isAnimating = false;     // well, are we animating or not?
boolean oldIsAnimating = isAnimating;          // save animation state

String currentFileName;
PixelMapGen currentGen;

public void settings() {
  size(rows * genWidth, columns * genHeight);
}

public void setup() {
  windowResizable(true);
  pixelaudio = new PixelAudio(this);
  /* try the other versions of MultiGen */
  // multigen = new MultiGen(width, height);
  /* 
   * The first custom constructor uses a count of rows and columns to distribute
   * gens in the 2D space of the application window. It's easy to use.
   */
  // multigen = new MultiGen(width, height, rows, columns, genList);
  /*
   * The second custom constructor is the more flexible of the two. With some planning of the
   * spatial location and the AffineTransformType applied to each gen in the genlist,
   * you can often create a continuous signal path through the final image. This application
   * demonstrates how to do that with HilbertGens in the hilbertLoop3x2 method.
   */
  multigen = hilbertLoop3x2(genWidth, genHeight);
  currentGen = multigen;
  mapper = new PixelAudioMapper(currentGen);
  mapImage = createImage(width, height, RGB);
  mapImage.loadPixels();
  colors = getColors();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapper.getSize());
  mapImage.updatePixels();
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
  genList.add(new HilbertGen(genW, genH,AffineTransformType.FX270));
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

public void draw() {
  image(mapImage, 0, 0, width, height);
  if (isAnimating) animate();
}

public void keyPressed() {
  switch(key) {
  case ' ':
    isAnimating = !isAnimating;
    break;
  case 'j':
    exportGenData(currentGen);
    break;
  case 'o':
    oldIsAnimating = isAnimating;
    isAnimating = false;
    importGenData();
    break;
  default:
    break;
  }
}

public void animate() {
  mapImage.loadPixels();
  PixelAudioMapper.rotateLeft(colors, shift);
  mapper.plantPixels(colors, mapImage.pixels, 0, mapper.getSize());
  mapImage.updatePixels();
}
