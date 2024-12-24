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
  multigen = loadLoopGen(genWidth, genHeight);
  mapper = new PixelAudioMapper(multigen);
  mapImage = createImage(width, height, RGB);
  mapImage.loadPixels();
  colors = getColors();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapper.getSize());
  mapImage.updatePixels();
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
