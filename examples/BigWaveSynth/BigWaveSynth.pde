import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;
HilbertGen hGen;
MultiGen multigen;
ArrayList<PixelMapGen> genList;
ArrayList<int[]> offsetList;
int rows = 3;
int columns = 2;
int genWidth = 256;
int genHeight = 256;
PixelAudioMapper mapper;

PImage mapImage;
int[] colors;


public void settings() {
  size(rows * genWidth, columns * genHeight);
}

public void setup() {
  pixelaudio = new PixelAudio(this);
  genList = new ArrayList<PixelMapGen>();
  offsetList = new ArrayList<int[]>();
  loadGenLists();
  // multigen = new MultiGen(width, height);
  // multigen = new MultiGen(width, height, rows, columns, genList);
  multigen = new MultiGen(width, height, offsetList, genList);
  mapper = new PixelAudioMapper(multigen);
  mapImage = createImage(width, height, RGB);
  mapImage.loadPixels();
  mapper.plantPixels(getColors(), mapImage.pixels, 0, mapper.getSize());
  mapImage.updatePixels();
}

public void loadGenLists() {
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FLIPX90));
  offsetList.add(new int[] { 0, 0 });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.NADA));
  offsetList.add(new int[] { genWidth, 0 });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FLIPX90CCW));
  offsetList.add(new int[] { 2 * genWidth, 0 });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FLIPX90CCW));
  offsetList.add(new int[] { 2 * genWidth, genHeight });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.ROT180));
  offsetList.add(new int[] { genWidth, genHeight });
  genList.add(new HilbertGen(genWidth, genHeight, AffineTransformType.FLIPX90));
  offsetList.add(new int[] { 0, genHeight });
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
