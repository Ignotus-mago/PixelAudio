import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;
HilbertGen gen;
PixelAudioMapper mapper;
int[] colors;
int genWidth = 512;
int genHeight = 512;
PImage mapImage;
int shift = 1024;
boolean isAnimating = false;

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
  println("-- Press 'a' to tuen animation on and off.");
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
  case 'a':
    isAnimating = !isAnimating;
    println("-- animation is "+ isAnimating);
    break;
  case 'i':
    shift = - shift;
    break;
  default:
    break;
  }
}

public void loadImagePixels(int[] pixArray, PImage img) {
  if (pixArray.length != img.pixels.length || pixArray.length != mapper.getSize()) {
    throw(new IllegalArgumentException("pixArray length and img.pixels.length must both equal mapper.getSize()."));
  }
  img.loadPixels();
  mapper.plantPixels(pixArray, img.pixels, 0, 0, mapper.getSize());
  img.updatePixels();
}

public void animate() {
  mapImage.loadPixels();
  PixelAudioMapper.rotateLeft(colors, shift);
  loadImagePixels(colors, mapImage);
  mapImage.updatePixels();
}
