/**
 * MultiGenDemo shows how to create a MultiGen. 
 * 
 * MultiGen is child class of PixelMapGen that allows you to use multiple PixelMapGens 
 * as if they were a single image with a single signal path through them. There are four  
 * different constructors that you can use. The first two, MultiGen(int width, int height) 
 * and MultiGen(int width, int height, AffineTransformType type), create two DiagonalZigzagGens 
 * to fill whatever width and height you provide. They are present because the abstract class
 * PixelMapGen requires them. Things get interesting with the two custom constructors, 
 * MultiGen(int width, int height, int rows, int columns, ArrayList<PixelMapGen> genList) 
 * and MultiGen(int width, int height, ArrayList<int[]> offsetList, ArrayList<PixelMapGen> genList).
 *
 * Both constructors require an ArrayList of PixelMapGens. The dimensions of the gens should
 * be tailored for the width and height of the application window. 
 * 
 * The first custom constructor uses a count of rows and columns to distribute the gens in 
 * the 2D space of the application window. It's easy to use.
 * 
 * The second custom constructor is the more flexible of the two. With some planning of the
 * spatial location and the AffineTransformType applied to each gen in the genlist, 
 * you can often create a continuous signal path through the final image. This application
 * demonstrates how to do that with HilbertGens. 
 *
 * Press ' ' to toggle animation that shifts pixels along the signal path.
 * 
 */

import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;
HilbertGen hGen;
MultiGen multigen;
ArrayList<PixelMapGen> genList;  // list of "gens" (PixelMapGen child classes)
ArrayList<int[]> offsetList;     // list of pixel offsets for each gen in the final image
int genWidth = 256;              // width of each gen (must be a power of 2 for HilbertGen
int genHeight = 256;             // height of each gen (must equal width of genO
int rows = 3;                    // number of rows of gens
int columns = 2;                 // number of columns of gens
PixelAudioMapper mapper;         // PixelAudioMapper to handle mapping of pixels to image

PImage mapImage;                 // A PImage to display
int[] colors;                    // an array of color values for pixels
int shift = 512;                // amount to displace pixels for animation
boolean isAnimating = false;     // well, are we animating or not?


public void settings() {
  size(rows * genWidth, columns * genHeight);
}

public void setup() {
  pixelaudio = new PixelAudio(this);
  // multigen = new MultiGen(width, height);
  multigen = hilbertLoop3x2(genWidth, genHeight);
  mapper = new PixelAudioMapper(multigen);
  mapImage = createImage(width, height, RGB);
  mapImage.loadPixels();
  colors = getColors();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapper.getSize());
  mapImage.updatePixels();
}

/**
 * Generates a looping fractal signal path consisting of 6 HilbertGens,
 * arranged 3 wide and 2 tall, to fit a 3 * genW by 2 * genH image. 
 * 
 * This method creates a MultiGen instance from a list of PixelMapGen objects 
 * (genList) and a list of  coordinate points (offsetList) where they will
 * be displayed. The MultiGen class creates a single signal path over all
 * its PixelMapGen objects. The path may be *continuous*, which is to say that
 * the path through each PixelMapGen object ("gen" for short) only has to step
 * one pixel up, down, left, or right to connect to the next gen. It may even
 * create a *loop*, where the last pixel in the path is one step away from the
 * first pixel. This method creates a MultiGen that is both continuous and looped. 
 *
 * This method may be called as factory method of the HilbertGen class:
 * public static MultiGen hilbertLoop3x2(int genW, int genH)
 * example: MultiGen multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
 *
 *
 * Note that genH must equal genW and both must be powers of 2. For the 
 * image size we're using in this example, genW = image width / 3 and 
 * genH = image height / 2.
 * 
 * @param genW    width of each HilbertGen 
 * @param genH    height of each HilbertGen
 * @return        a MultiGen consisting of 6 HilbertGens linked together by one signal path
 * 
 */
public MultiGen hilbertLoop3x2(int genWidth, int genHeight) {
  // list of PixelMapGens that create an image using mapper
  ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
  // list of x,y coordinates for placing gens from genList
  ArrayList<int[]> offsetList = new ArrayList<int[]>();     
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FX270));
  offsetList.add(new int[] { 0, 0 });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.NADA));
  offsetList.add(new int[] { genWidth, 0 });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FX90));
  offsetList.add(new int[] { 2 * genWidth, 0 });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FX90));
  offsetList.add(new int[] { 2 * genWidth, genHeight });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.R180));
  offsetList.add(new int[] { genWidth, genHeight });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FX270));
  offsetList.add(new int[] { 0, genHeight });
 return new MultiGen(width, height, offsetList, genList);
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
  image(mapImage, 0, 0);
  if (isAnimating) animate();
}

public void keyPressed() {
  switch(key) {
  case ' ':
    isAnimating = !isAnimating;
    break;
  case 'h':
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
