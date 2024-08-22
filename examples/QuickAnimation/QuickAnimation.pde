/**
 * This is basically the Starter example with simple animation added. 
 * for more complicated animation, see the various WaveSynth examples. 
 *
 * Press ' ' SPACEBAR to start animation.
 * Drag in the image to change animation speed and direction.
 * Press 'm' to turn off mouse tracking.
 * 
 */

import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;
HilbertGen gen;          // a PixelMapGen generator object
PixelAudioMapper mapper; // a PixelAudioMapper mapping object
int[] colors;            // a bix array of RGB colors
int genWidth = 512;      // for Hilbert Curves, width and height must be powers of 2
int genHeight = 512;
PImage mapImage;         // image to display
int shift = 1024;        // number of pixels to shift the animation
boolean isAnimating = false;
boolean isTrackMouse = true;

public void settings() {
  size(genWidth, genHeight);
}

public void setup() {
  pixelaudio = new PixelAudio(this);
  gen = new HilbertGen(genWidth, genHeight);
  mapper = new PixelAudioMapper(gen);
  colors = getColors();
  mapImage = createImage(genWidth, genHeight, RGB);
  loadImagePixels(colors, mapImage);
  println("-- Press ' ' SPACEBAR to turn animation on and off.");
}

public void draw() {
  image(mapImage, 0, 0);
  if (isAnimating) animate();
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

public void keyPressed() {
  switch(key) {
  case ' ':
    isAnimating = !isAnimating;
    println("-- animation is "+ isAnimating);
    break;
  case 'm':
    isTrackMouse = !isTrackMouse;
    println("-- mouse tracking is "+ isTrackMouse);
    break;
  default:
    break;
  }
}

public void mouseDragged() {
    if (isTrackMouse) {
      shift = abs(width/2 - mouseX) * 16;
      if (mouseY < height/2) shift = -shift;
    }
}


public void loadImagePixels(int[] pixArray, PImage img) {
  if (pixArray.length != img.pixels.length || pixArray.length != mapper.getSize()) {
    throw(new IllegalArgumentException("pixArray length and img.pixels.length must both equal mapper.getSize()."));
  }
  img.loadPixels();      // load img.pixels with a RGB pixel values from pixArray
  // plantPixels() sets the color values following the "signal path", i.e., the Hilbert Curve
  // or whatever curve the PixelMapGen we are using has generated
  mapper.plantPixels(pixArray, img.pixels, 0, mapper.getSize());
  img.updatePixels();
}

public void animate() {
  mapImage.loadPixels();
  // negative values for shift go the other way
  PixelAudioMapper.rotateLeft(colors, shift);
  loadImagePixels(colors, mapImage);
  mapImage.updatePixels();
}
