import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;      // our library
HilbertGen hGen;            // a Hilbert curve generator
MooreGen mGen;              // a Moore curve generator 
DiagonalZigzagGen zGen;
PixelMapGen gen;
PixelAudioMapper mapper;
int[] spectrum;
ArrayList<int[]> coords;
int[] imageLUT;
int[] signalLUT;

int imageWidth = 1024;
int imageHeight = 1024;
int genW = 8;
int genH = 8;
int drawingScale = 1;
int offset = 0;
int bigTextSize = 64;
int smallTextSize = 32;


public void settings() {
  size(imageWidth, imageHeight, JAVA2D);
}

public void setup() {
  pixelaudio = new PixelAudio(this);
  hGen = new HilbertGen(genW, genH);
  zGen = new DiagonalZigzagGen(genW, genH);
  mGen = new MooreGen(genW, genH);
  initMapper(hGen);
  // printLUTs();
  spectrum = initColors();
  drawingScale = imageWidth / gen.getWidth();
  offset = drawingScale / 2;
  showHelp();
}

public void initMapper(PixelMapGen gen) {
  this.mapper = new PixelAudioMapper(gen);
  this.coords = gen.getCoordinatesCopy();
  this.imageLUT = mapper.getImageToSignalLUT();     // gen.getSampleMapCopy();
  this.signalLUT = mapper.getSignalToImageLUT();    // gen.getPixelMapCopy();
  this.gen = gen;
}

public void updateMapper(PixelMapGen gen) {
  this.mapper.setGenerator(gen);
  this.coords = gen.getCoordinatesCopy();
  this.imageLUT = mapper.getImageToSignalLUT();     // gen.getSampleMapCopy();
  this.signalLUT = mapper.getSignalToImageLUT();    // gen.getPixelMapCopy();
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
  case 'd':
    println("\n"+ mapper.getGeneratorDescription());
    break;
  case 'g':
    if (gen == zGen) { gen = hGen; }
    else { gen = zGen; }
    initMapper(gen);
    break;
  case 'l': case 'L':
      printLUTs();
      break;
  case 't': case 'T':
      testAffineMap(genW, genH);
      break;
  case 'f':
    gen.setTransformType(AffineTransformType.ROT90);
    updateMapper(gen);
    break;
  case 'b':
    gen.setTransformType(AffineTransformType.ROT90CCW);
    updateMapper(gen);
    break;
  case 'r':
    gen.setTransformType(AffineTransformType.ROT180);
    updateMapper(gen);
    break;
  case 'x':
    gen.setTransformType(AffineTransformType.FLIPX);
    updateMapper(gen);
    break;
  case 'y':
    gen.setTransformType(AffineTransformType.FLIPY);
    updateMapper(gen);
    break;
  case '1':
    gen.setTransformType(AffineTransformType.FLIPX90CCW);
    updateMapper(gen);
    break;
  case '2':
    gen.setTransformType(AffineTransformType.FLIPX90);
    updateMapper(gen);
    break;
  case 'h':
    showHelp();
    break;
  default:
    break;
  }
}

public int[] initColors() {
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

public void printLUTs() {
  println("\n----- imageToSignalLUT -----");
  for (int i = 0; i < imageLUT.length; i++) {
    print(imageLUT[i] +"  ");
  }
  println();
  println("----- signaToImagelLUT -----");
  for (int i = 0; i < signalLUT.length; i++) {
    print(signalLUT[i] +"  ");
  }
  println();
  println("----- Coordinates -----");
  for (int[] xy : this.coords) {
    print("("+ xy[0] +", "+ xy[1] +")  ");
  }
}

public void testAffineMap(int w, int h) {
  println("\n"+ w +" x "+ h +" bitmap index remapping\n");
  for (AffineTransformType type : AffineTransformType.values()) {
    println("------------- "+ type.name() +" -------------");
    int[] newMap = BitmapTransform.getIndexMap(w, h, type);
    int i = 0;
    for (int n : newMap) {
      if (i < newMap.length - 1) print(n +", ");
      else print(n +"\n ");
      i++;
    }
  }
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
  int drop = bigTextSize/4;
  pushStyle();
  for (int[] coordinate : coords) {                      // coords follows the signal path
    x1 = coordinate[0] * drawingScale + offset;          // x-coordinate along the signal path 
    y1 = coordinate[1] * drawingScale + offset + drop;   // y-coordinate along the signal path
    textAlign(CENTER);                                   // text for the center of each square
    textSize(bigTextSize);                               // big font size
    fill(0, 192);                                        // dark color
    text(signalLUT[pos], x1, y1);                        // show the bitmap pixel number in the signalToImageLUT
    textAlign(LEFT);                                     // small white text for the signal path index numbers
    textSize(smallTextSize);                             // which we a flagging with the pos variable
    fill(255, 192);                                      // upper left corner
    text(pos, x1 - offset + smallTextSize/2, y1 - offset + smallTextSize/2);
    pos++;
  }
  popStyle();  
}

public void stepAnimation(int step) {
  PixelAudioMapper.rotateLeft(spectrum, step);
}

public void showHelp() {
  println("\n----- HELP -----");
  println("Signal path index numbers are small white numbers, bitmap index numbers are big black numbers.");
  println("Read the imageToSignalLUT values by following the pixel index order and reading the white numbers.");
  println("Read the signalToImageLUT values by following the signal path order and reading the black numbers.");
  println("Read the imageToSignalLUT values by following the black pixel numbers in order and reading the white numbers.");
  println("Press 'd' to print a description of the current generator to the console.");
  println("Press 'a' or 'A' to rotate the array of colors one step left or right.");
  println("Press 'l' to print the imageToSignalLUT and the signalToImageLUT to the console.");
  println("Press 'h' to show this help text in the console.");
}


public void testRotate90Coords() {
  println("----->>> Rotated Coords, maybe <<<-----");
  for (int[] xy : this.coords) {
    int[] newXY = BitmapTransform.rotate90Coord(xy[0], xy[1], genW, genH);
    print("("+ newXY[0] +", "+ newXY[1] +")  ");
  }
  println("\n");
}
