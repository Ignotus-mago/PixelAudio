import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public static final double semitoneFac = Math.pow(2, 1/12.0);
DecimalFormat twoPlaces;
DecimalFormat noPlaces;
DecimalFormat fourFrontPlaces;
DecimalFormat commaTwoPlaces;
DecimalFormat eightPlaces;

public float[] chromaticScale(float funda) {
  float[] chromaScale = new float[12];
  for (int i = 0; i < chromaScale.length; i++) {
    chromaScale[i] = funda;
    funda *= (float) semitoneFac;
  }
  return chromaScale;
}

public float pianoKeyFrequency(float keyNumber) {
  return (float) (440 * Math.pow(2, (keyNumber - 49)/12.0));
}

public float frequencyPianoKey(float freq) {
  return 49 + 12 * (float) (Math.log(freq / 440) / Math.log(2));
}


/**
 * initializes the zero place and two place decimal number formatters
 */
public void initDecimalFormat() {
  // DecimalFormat sets formatting conventions from the local system, unless we tell it otherwise.
  // make sure we use "." for decimal separator, as in US, not a comma, as in many other countries
  Locale loc = Locale.US;
  DecimalFormatSymbols dfSymbols = new DecimalFormatSymbols(loc);
  dfSymbols.setDecimalSeparator('.');
  twoPlaces = new DecimalFormat("0.00", dfSymbols);
  noPlaces = new DecimalFormat("00", dfSymbols);
  fourFrontPlaces = new DecimalFormat("0000", dfSymbols);
  eightPlaces = new DecimalFormat("0.00000000", dfSymbols);
  dfSymbols.setDecimalSeparator(',');
  commaTwoPlaces = new DecimalFormat("0.00", dfSymbols);
}


/**
 * Generates an ArrayList of WaveData objects to be used by a WaveSynth to generate
 * RGB pixel values and (on request) audio signal values.
 *
 * @return an ArrayList of WaveData objects
 */
public ArrayList<WaveData> buildWaveDataList(float fundamental, int howManyPartials) {
  ArrayList<WaveData> list = new ArrayList<WaveData>();
  if (howManyPartials < 1) howManyPartials = 1;
  // funda is the fundamental of a musical tone that is somewhat like a trumpet
  // in its frequency spectrum. Vary it to see how the image and sound change.
  float funda = fundamental;
  float frequency = funda;
  float amplitude = 0.55f;
  float phase = 0.766f;
  float dc = 0.0f;
  float cycles = -8.0f;
  int waveColor = color(0, 89, 233);
  int steps = this.animSteps;
  WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  //
  if (howManyPartials == 1) return list;
  frequency = 2 * funda;
  amplitude = 0.52f;
  phase = -0.89f;
  cycles = 8.0f;
  waveColor = color(89, 199, 55);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  //
  if (howManyPartials == 2) return list;
  frequency = 3 * funda;
  amplitude = 0.6f;
  phase = -0.486f;
  cycles = 3.0f;
  waveColor = color(254, 89, 110);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  //
  if (howManyPartials == 3) return list;
  frequency = 4 * funda;
  amplitude = 0.45f;
  phase = -0.18616974f;
  cycles = -2.0f;
  waveColor = color(89, 110, 233);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  //
  if (howManyPartials == 4) return list;
  frequency = 5 * funda;
  amplitude = 0.42f;
  phase = 0.6846085f;
  cycles = -5.0f;
  waveColor = color(233, 34, 21);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  //
  if (howManyPartials == 5) return list;
  frequency = 6 * funda;
  amplitude = 0.45f;
  phase = 0.68912f;
  cycles = 13.0f;
  waveColor = color(220, 199, 55);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  //
  if (howManyPartials == 6) return list;
  frequency = 7 * funda;
  amplitude = 0.25f;
  phase = 0.68f;
  cycles = 11.0f;
  waveColor = color(159, 190, 255);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  //
  if (howManyPartials == 7) return list;
  frequency = 8 * funda;
  amplitude = 0.32f;
  phase = 0.68f;
  cycles = -11.0f;
  waveColor = color(209, 178, 117);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  return list;
}

/**
 * Sets instance variables for a supplied WaveSynth.
 * @param synth
 * @return
 */
public WaveSynth initWaveSynth(WaveSynth synth) {
  synth.setGain(0.44f);
  synth.setGamma(myGamma);
  synth.setScaleHisto(false);
  synth.setAnimSteps(this.animSteps);
  synth.setSampleRate(sampleRate);
  // synth.setNoiseiness(0.5f);
  println("--- mapImage size = " + synth.mapImage.pixels.length);
  synth.prepareAnimation();
  synth.renderFrame(0);
  return synth;
}
