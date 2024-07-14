import net.paulhertz.pixelaudio.*;

PixelAudio pixelaudio;
HilbertGen hGen;
MooreGen mGen;
PixelAudioMapper mapper;
ArrayList<WaveData> wdList;
WaveSynth wavesynth;
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
  wavesynth = initWaveSynth();
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

public WaveSynth initWaveSynth() {
  WaveSynth synth = new WaveSynth(mapper, wdList);
  synth.setGain(0.8);
  synth.setGamma(myGamma);
  synth.setScaleHisto(false);
  synth.setAnimSteps(this.animSteps);
  synth.prepareAnimation();
  synth.renderFrame(0);
  return synth;
}

public void draw() {
  image(synthImage, 0, 0);
  stepAnimation();
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
    makeGammaTable(myGamma);
    break;
  case 't':
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
  println("press 'g' to print the lookup table for current gamma to console.");
  println("press 't' to change gamma calculation between table lookup and calculation.");
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
