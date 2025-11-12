/****** WaveSynth Sequencer Settings ******/

/** for frequency calculations, numbers are piano keys */
public static final double semitoneFac = Math.pow(2, 1/12.0);
int[] dbwfMusic = {
  //9, 9, 9,  9,
  9, 14, 20, 8, 12, 19, 7, 13, 17, 12, 18, 11, 21, 16, 24, 15,
  26, 24, 19, 25, 24, 17, 11, 4, 9, 14, 20, 8, 12, 19, 7, 9,
  4, 9, 15, 8, 14, 19, 16, 23, 21, 26, 19, 12, 20, 16, 14, 10
};

/** for time calculations, 12 = one quarter note */
ArrayList<WaveSynth> waveSynthList;
int[] dbwfTimes = {
  //12, 12, 12, 12,
  12, 12, 12, 12, 8, 4, 12, 24, 12, 12, 18, 6, 4, 4, 16, 24,
  12, 12, 12, 12, 8, 4, 12, 24, 12, 12, 18, 6, 8, 8, 8, 24,
  12, 12, 12, 12, 8, 4, 12, 24, 12, 12, 18, 6, 8, 4, 12, 24
};

/** amplitude values */
float[] dbwfAmps = {
  //0.0f, 0.0f, 0.f, 0.0f,
  0.4f, 0.3f, 0.5f, 0.3f, 0.4f, 0.3f, 0.5f, 0.4f, 0.4f, 0.3f, 0.3f, 0.3f, 0.4f, 0.3f, 0.3f, 0.4f,
  0.4f, 0.3f, 0.5f, 0.3f, 0.4f, 0.3f, 0.3f, 0.5f, 0.4f, 0.3f, 0.5f, 0.3f, 0.4f, 0.3f, 0.3f, 0.5f,
  0.4f, 0.3f, 0.3f, 0.3f, 0.4f, 0.3f, 0.3f, 0.4f, 0.4f, 0.3f, 0.5f, 0.3f, 0.4f, 0.3f, 0.3f, 0.5f
};

// beatSpan is measured in milliseconds, where the entire buffer is 32768 ms, divided into 12 * 4 = 48 beats
// Each beat has 12 subdivisions (ticks), so that dbwfTimes[0], which equals 12, is one beat long.
// If we want to run the music in half time, we can set ticks to 6.

int ticks = 12;
float beatSpan = 32768/(48.0f * ticks);
int wsIndex = 0;
ArrayList<NoteTimedLocation> introMusic;
boolean isPlayIntro = false;

// ------------------------------------------- //
//               WAVESYNTH HANDLERS            //
// ------------------------------------------- //

/**
 * Generates an ArrayList of WaveData objects to be used by a WaveSynth to
 * generate RGB pixel values and (on request) audio signal values.
 * A version of this code is included in the WaveSynthBuilder class in the PixelAudio library,
 * wrapped in a static method called synthTrumpet(float fundamental, int howManyPartials, float pianoKey, int animSteps).
 *
 * @return an ArrayList of WaveData objects
 */
public ArrayList<WaveData> buildWaveDataList(float fundamental, int howManyPartials, float pianoKey) {
  ArrayList<WaveData> list = new ArrayList<WaveData>();
  if (howManyPartials < 1)
    howManyPartials = 1;
  // funda is the fundamental of a musical tone that is somewhat like a trumpet
  // in its frequency spectrum. Vary it to see how the image and sound change.
  float funda = fundamental;
  float frequency = funda;
  float amplitude = 0.55f;
  float phase = 0.766f;
  float dc = 0.0f;
  float cycles = -8.0f;
  int waveColor = color(0, 89, 233);
  waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
  int steps = this.animSteps;
  WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  //
  if (howManyPartials == 1)
    return list;
  frequency = 2 * funda;
  amplitude = 0.52f;
  phase = -0.89f;
  cycles = 8.0f;
  waveColor = color(89, 199, 55);
  waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  //
  if (howManyPartials == 2)
    return list;
  frequency = 3 * funda;
  amplitude = 0.6f;
  phase = -0.486f;
  cycles = 3.0f;
  waveColor = color(254, 89, 110);
  waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  //
  if (howManyPartials == 3)
    return list;
  frequency = 4 * funda;
  amplitude = 0.45f;
  phase = -0.18616974f;
  cycles = -2.0f;
  waveColor = color(89, 110, 233);
  waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  //
  if (howManyPartials == 4)
    return list;
  frequency = 5 * funda;
  amplitude = 0.42f;
  phase = 0.6846085f;
  cycles = -5.0f;
  waveColor = color(233, 34, 21);
  waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  //
  if (howManyPartials == 5)
    return list;
  frequency = 6 * funda;
  amplitude = 0.45f;
  phase = 0.68912f;
  cycles = 13.0f;
  waveColor = color(220, 199, 55);
  waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  //
  if (howManyPartials == 6)
    return list;
  frequency = 7 * funda;
  amplitude = 0.25f;
  phase = 0.68f;
  cycles = 11.0f;
  waveColor = color(159, 190, 255);
  waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  //
  if (howManyPartials == 7)
    return list;
  frequency = 8 * funda;
  amplitude = 0.32f;
  phase = 0.68f;
  cycles = -11.0f;
  waveColor = color(209, 178, 117);
  waveColor = colorShift(waveColor, (pianoKey % 12) / 12.0f);
  wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
  list.add(wd);
  return list;
}

/**
 * Sets gain, gamma, isScaleHisto, animSteps, and sampleRate instance variables
 * of a WaveSynth object and generates its first frame of animation.
 *
 * @param synth    a WaveSynth object whose attributes will be set
 * @return      the WaveSynth object with attributes set
 */
public WaveSynth initWaveSynth(WaveSynth synth) {
  synth.setGain(0.44f);
  synth.setGamma(myGamma);
  synth.setScaleHisto(false);
  synth.setAnimSteps(this.animSteps);
  synth.setSampleRate(sampleRate);
  // synth.setNoiseiness(0.5f);
  // println("--- mapImage size = " + synth.mapImage.pixels.length);
  synth.prepareAnimation();
  // synth.renderFrame(0);
  return synth;
}

/**
 * Initializes the introMusic ArrayList of NoteTimedLocation objects, data for a simple audio sequencer.
 * Uses the dbwfTimes array to set durations, dbwfMusic to set frequencies, and dbwfAmps to set amplitudes.
 */
public void loadMusic() {
  if (introMusic == null)
    introMusic = new ArrayList<NoteTimedLocation>();
  this.introMusic.clear();
  int startTime = millis() + 500;
  int i = 0;
  int stopSum = startTime;
  int circ = 0;
  int signalPos;
  int[] coords;
  int x;
  int y;
  float span;
  ADSRParams adsr = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  for (int dur : this.dbwfTimes) {
    signalPos = (int) random(samplelen, mapSize - samplelen - 1);
    coords = mapper.lookupCoordinate(signalPos);
    x = coords[0];
    y = coords[1];
    circ = color(233, 220, 199, 128);
    adsr = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
    span = dur * this.beatSpan;
    int pianoKey = this.dbwfMusic[i];
    float f = 1.0f * pianoKeyFrequency(pianoKey);
    WaveSynth ws = new WaveSynth(mapper, this.buildWaveDataList(f, 8, pianoKey));
    initWaveSynth(ws);
    float amp = dbwfAmps[i];
    introMusic.add(new NoteTimedLocation(x, y, stopSum, i, pianoKey, span, amp * 1.8f, circ, adsr, ws));
    i++;
    stopSum += (int) (Math.round(span));
    println("--->> note event ", pianoKey, twoPlaces.format(f), twoPlaces.format(span), stopSum);
  }
}

/**
 * @param keyNumber    key number on a piano, where A is key 49
 * @return        frequency of the key (A = 440)
 */
public float pianoKeyFrequency(int keyNumber) {
  return (float) (440 * Math.pow(2, (keyNumber - 49) / 12.0));
}

/**
 * @param funda   the starting frequency
 * @return      a chromatic scale starting with funda
 */
public float[] chromaticScale(float funda) {
  float[] chromaScale = new float[12];
  for (int i = 0; i < chromaScale.length; i++) {
    chromaScale[i] = funda;
    funda *= (float) semitoneFac;
  }
  return chromaScale;
}

/**
 * Initializes a list of WaveSynth objects using pianoKey numbers in dbwfMusic array.
 */
public void initWaveSynthList() {
  this.waveSynthList = new ArrayList<WaveSynth>();
  for (int pianoKey : this.dbwfMusic) {
    float f = pianoKeyFrequency(pianoKey);
    WaveSynth ws = new WaveSynth(mapper, this.buildWaveDataList(f, 8, pianoKey));
    initWaveSynth(ws);
    waveSynthList.add(ws);
  }
}

/**
 * @param c      an RGB color
 * @param shift  the shift [0..1] of the hue in the HSB representation of color c
 * @return the RGB representation of the shifted color
 */
public int colorShift(int c, float shift) {
  float[] hsb = new float[3];
  float h = PixelAudioMapper.hue(c, hsb);
  h = (h + shift);
  return Color.HSBtoRGB(h, hsb[1], hsb[2]);
}
