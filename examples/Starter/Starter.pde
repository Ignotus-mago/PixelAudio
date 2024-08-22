import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;
HilbertGen hGen;
PixelAudioMapper mapper;
PImage mapImage;
int[] colors;

public void setup() {
  size(512, 512);                          // Hilbert curves require dimensions that are powers of 2. 
  pixelaudio = new PixelAudio(this);       // initialize PixelAudio library
  hGen = new HilbertGen(width, height);    // create a Hilbert curve generator
  mapper = new PixelAudioMapper(hGen);     // plug it into a PixelAudioMapper object
  mapImage = createImage(width, height, RGB);  // create an image for display
  mapImage.loadPixels();                   // load the image with a spectrum of colors
  // plantPixels lays the colors along the "signal path", i.e., the Hilbert Curve, through mapImage
  mapper.plantPixels(getColors(), mapImage.pixels, 0, mapper.getSize());
  mapImage.updatePixels();
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
}

public void keyPressed() {
  switch(key) {
  case 'h':
    break;
  default:
    break;
  }
}
