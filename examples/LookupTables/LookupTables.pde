import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;
HilbertGen hGen;
DiagonalZigzagGen zGen;
PixelMapGen gen;
PixelAudioMapper mapper;
int imageWidth = 1024;
int imageHeight = 1024;
int[] spectrum;
int[] imageLUT;
int[] signalLUT;
int drawingScale = 1;
int offset = 0;
ArrayList<int[]> coords;


public void settings() {
  size(imageWidth, imageHeight, JAVA2D);
}

public void setup() {
  pixelaudio = new PixelAudio(this);
  hGen = new HilbertGen(4, 4);
  zGen = new DiagonalZigzagGen(4, 4);
  initMapper(zGen);
  printLUTs();
  spectrum = initColors();
  drawingScale = imageWidth / gen.getWidth();
  offset = drawingScale / 2;
}

public void initMapper(PixelMapGen gen) {
  this.mapper = new PixelAudioMapper(gen);
  this.coords = gen.getCoordinatesCopy();
  this.imageLUT = gen.getPixelMapCopy();
  this.signalLUT = gen.getSampleMapCopy();
  this.gen = gen;
}

public void draw() {
  drawSquares();
  drawLines();
  drawNumbers();
}

public void keyPressed() {
  switch(key) {
  case 'a':
    stepAnimation(1);
    break;
  case 'A':
    stepAnimation(-1);
    break;
  case 'g':
    if (gen == zGen) { gen = hGen; }
    else { gen = zGen; }
    initMapper(gen);
    break;
  case 'l': case 'L':
      printLUTs();
      break;
  case 'h':
    break;
  default:
    break;
  }
}

public int[] initColors() {
  int[] misColores = new int[mapper.getSize()];
  pushStyle();
  colorMode(HSB, misColores.length, 100, 100);
  int h = 0;
  for (int i = 0; i < misColores.length; i++) {
      misColores[i] = color(h, 66, 66);
      h++;
  }
  popStyle();
  return misColores;
}

public void printLUTs() {
  println("\n----- imageLUT -----");
  for (int i = 0; i < imageLUT.length; i++) {
    print(imageLUT[i] +"  ");
  }
  println();
  println("----- signalLUT -----");
  for (int i = 0; i < signalLUT.length; i++) {
    print(signalLUT[i] +"  ");
  }
  println();
}

public void drawSquares() {
  int x1 = 0;
  int y1 = 0;
  int x2 = 0;
  int y2 = 0;
  int pos = 0;
  pushStyle();
  for (int[] coordinate : coords) {
    fill(spectrum[pos]);
    if (pos == 0) {
      x1 = coordinate[0] * drawingScale;
      y1 = coordinate[1] * drawingScale;
    } 
    else {
      x2 = coordinate[0] * drawingScale;
      y2 = coordinate[1] * drawingScale;
      x1 = x2;
      y1 = y2;
    }
    noStroke();
    square(x1, y1, drawingScale);
    pos++;
  }
  noStroke();
  square(x2, y2, drawingScale);
  popStyle();
}

public void drawLines() {
  int x1 = 0;
  int y1 = 0;
  int x2 = 0;
  int y2 = 0;
  int pos = 0;
  pushStyle();
  strokeWeight(1);
  stroke(255, 216);
  for (int[] coordinate : coords) {
    if (pos == 0) {
      x1 = coordinate[0] * drawingScale + offset;
      y1 = coordinate[1] * drawingScale + offset;
    } 
    else {
      x2 = coordinate[0] * drawingScale + offset;
      y2 = coordinate[1] * drawingScale + offset;
      line(x1, y1, x2, y2);
      x1 = x2;
      y1 = y2;
    }
    pos++;
  }
  //line(x1, y1, x2, y2);
  popStyle();  
}

public void drawNumbers() {
  int x1 = 0;
  int y1 = 0;
  int pos = 0;
  int bigTextSize = 72;
  int smallTextSize = 36;
  int drop = bigTextSize/4;
  pushStyle();
  for (int[] coordinate : coords) {
    x1 = coordinate[0] * drawingScale + offset;
    y1 = coordinate[1] * drawingScale + offset + drop;
    textAlign(CENTER);
    textSize(bigTextSize);
    fill(0, 192);
    text(pos, x1, y1);
    textAlign(LEFT);
    textSize(smallTextSize);
    fill(255, 192);
    text(imageLUT[pos], x1 - offset + smallTextSize/2, y1 - offset + smallTextSize/2);
    pos++;
  }
  popStyle();  
}

public void stepAnimation(int step) {
  PixelAudioMapper.rotateLeft(spectrum, step);
}
