import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;
HilbertGen hGen;
MooreGen mGen;
DiagonalZigzagGen zGen;
PixelMapGen gen;
PixelAudioMapper mapper;
ArrayList<WaveData> wdList;
WaveSynth wavesynth;
int imageWidth = 1024;
int imageHeight = 1024;
PImage synthImage;
float myGamma = 1.0;
int animSteps = 240;
int step = 0;
int start = 0;
int timespan = 0;
boolean isTrackTime = false;


public void settings() {
  size(imageWidth, imageHeight, JAVA2D);
}

public void setup() {
  pixelaudio = new PixelAudio(this);
  hGen = new HilbertGen(1024, 1024);
  mGen = new MooreGen(1024, 1024);
  zGen = new DiagonalZigzagGen(1024, 1024, AffineTransformType.FLIPY);
  gen = hGen;
  mapper = new PixelAudioMapper(gen);
  wdList = initWaveDataList();
  wavesynth = new WaveSynth(mapper, wdList);
  initWaveSynth(wavesynth);
  synthImage = wavesynth.mapImage;
  showHelp();
}

public ArrayList<WaveData> initWaveDataList() {
  ArrayList<WaveData> list = new ArrayList<WaveData>();
  float frequency = 768.0;
  float amplitude = 0.8;
  float phase = 0.0;
  float dc = 0.0;
  float cycles = 1.0;
  int waveColor = color(159, 190, 251);
  int steps = this.animSteps;
  WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  frequency = 192.0;
  phase = 0.0;
  cycles = 2.0;
  waveColor = color(209, 178, 117);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  return list;
}

public WaveSynth initWaveSynth(WaveSynth synth) {
  synth.setGain(0.8);
  synth.setGamma(myGamma);
  synth.setScaleHisto(false);
  synth.setAnimSteps(this.animSteps);
  println("--- mapImage size = "+ synth.mapImage.pixels.length);
  synth.prepareAnimation();
  synth.renderFrame(0);
  return synth;
}

public void swapGen(PixelMapGen gen) {
  mapper.setGenerator(gen);
  // if we had a new mapper, we would call wavesynth.setMapper(mapper) and reset synthImage locally. 
  // As it is, mapper only changed its variables, so the swap is really simple
}

  public void draw() {
    image(synthImage, 0, 0);
    if (isTrackTime) {
      trackTime();
    }
    stepAnimation();
  }

  public void trackTime() {
    if (step == 0) {
      start = millis();
      println("----- timer started -----");
    }
    if (step % 24 == 0) {
      println("-- step " + step);
    }
    if (step == animSteps - 1) {
      timespan = (millis() - start);
      int xms = timespan % 1000;
      int secs = (timespan - xms) / 1000;
      int xsecs = secs % 60;
      int mins = secs / 60;
      println("--->> elapsed time: " + mins + ":" + xsecs + ":" + xms);
    }
  }

public void stepAnimation() {
  step += 1;
  step %= animSteps;
  wavesynth.renderFrame(step);
}

public void keyPressed() {
  switch (key) {
  case '1':
    myGamma = 1.0;
    wavesynth.setGamma(myGamma);
    break;
  case '2':
    myGamma = 1.4;
    wavesynth.setGamma(myGamma);
    break;
  case '3':
    myGamma = 1.8;
    wavesynth.setGamma(myGamma);
    break;
  case '4':
    myGamma = 0.5;
    wavesynth.setGamma(myGamma);
    break;
  case 'g':
    // change the gen
    if (gen == zGen) { gen = hGen; swapGen(gen); break;}
    if (gen == hGen) {gen = mGen; swapGen(gen); break;}
    if (gen == mGen) {gen = zGen; swapGen(gen);}
    break;
  case 'f':
    // rotate gen 90 degrees clockwise
    gen.setTransformType(AffineTransformType.ROT90);
    swapGen(gen);
    break;
  case 'e':
    wavesynth.setSampleRate(wavesynth.mapSize/2);
    break;
  case 'E':
    wavesynth.setSampleRate(wavesynth.mapSize/4);
    break;
  case 'r':
    wavesynth.setSampleRate(41500);
    break;
  case 'R':
    wavesynth.setSampleRate(wavesynth.mapSize);
    break;
  case 't':
    makeGammaTable(myGamma);
    break;
  case 'u':
    wavesynth.useGammaTable = !wavesynth.useGammaTable;
    if (wavesynth.useGammaTable) println("-- using gamma table");
    else println("-- calculating gamma ");
    break;
  case 'h':
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println("press '1' to set gamma to 1.0.");
  println("press '2' to set gamma to 1.4.");
  println("press '3' to set gamma to 1.8.");
  println("press '4' to set gamma to 0.5.");
  println("press 'e' to set sample rate to (width * height)/2");
  println("press 'e' to set sample rate to (width * height)/4");
  println("press 'r' to set sample rate to 41500");
  println("press 'R' to set sample rate to image size (width * height)");
  println("press 't' to print the lookup table for current gamma to console.");
  println("press 'u' to change gamma calculation between table lookup and calculation.");
  println("press 'h' to show Help.");
}

// code for generating a gamma table, a non-linear adjustment of brightness
public void makeGammaTable(float gamma) {
  int[] gammaTable = new int[256];
  println("----- GAMMA TABLE, gamma = "+ gamma +" -----");
  for (int i = 0; i < gammaTable.length; i++) {
    float c = i/(float)(gammaTable.length - 1);
    c = (float) (Math.pow(c, gamma) * (gammaTable.length - 1));
    gammaTable[i] = (int) c;
    println("-- "+ i +"  "+ nf(c, 0, 4) +"  "+    gammaTable[i]);
  }
}
