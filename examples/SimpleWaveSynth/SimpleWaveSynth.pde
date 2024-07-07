import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;
HilbertGen hGen;
MooreGen mGen;
PixelAudioMapper mapper;
ArrayList<WaveData> wdList;
WaveSynth synth;
int imageWidth = 1024;
int imageHeight = 1024;
PImage synthImage;
float myGamma = 1.0;
int animSteps = 240;
int step = 0;


public void settings() {
  size(imageWidth, imageHeight, JAVA2D);
}

public void setup() {
  pixelaudio = new PixelAudio(this);
  hGen = new HilbertGen(1024, 1024);
  mGen = new MooreGen(1024, 1024);
  mapper = new PixelAudioMapper(mGen);
  wdList = initWaveDataList();
  synth = new WaveSynth(mapper, wdList);
  synth.setGain(0.8);
  synth.setGamma(myGamma);
  synth.setScaleHisto(false);
  synth.setAnimSteps(this.animSteps);
  synth.prepareAnimation();
  synth.renderFrame(0);
  synthImage = synth.mapImage;
  // showHelp();
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

public void draw() {
  image(synthImage, 0, 0);
  stepAnimation();
}

public void stepAnimation() {
  step += 1;
  step %= animSteps;
  synth.renderFrame(step);
}

public void keyPressed() {
  switch (key) {
  case '1':
    myGamma = 1.0;
    synth.setGamma(myGamma);
    break;
  case '2':
    myGamma = 1.4;
    synth.setGamma(myGamma);
    break;
  case '3':
    myGamma = 1.8;
    synth.setGamma(myGamma);
    break;
  case '4':
    myGamma = 0.5;
    synth.setGamma(myGamma);
    break;
  case 'g':
    makeGammaTable(myGamma);
    break;
  case 't':
    synth.useGammaTable = !synth.useGammaTable;
    if (synth.useGammaTable) println("-- using gamma table");
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
}

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
