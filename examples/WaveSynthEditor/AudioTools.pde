/*----------------------------------------------------------------*/
/*                                                                */
/*                     BEGIN AUDIO METHODS                        */
/*                                                                */
/*----------------------------------------------------------------*/

/**
 * CALL IN SETUP()
 */
public void initAudio() {
  minim = new Minim(this);
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
  this.setAudioGain(outputGain);
  this.audioBuffer = new MultiChannelBuffer(mapSize, 1);
  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
  adsr = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  this.renderSignal();
  // creae a PASamplerInstrumentPool with 8 instruments, each with a single (monophonic) voice
  pool = new PASamplerInstrumentPool(audioBuffer, sampleRate, 8, 1, audioOut, adsr);
  timeLocsArray = new ArrayList<TimedLocation>();     // initialize mouse event tracking array
}

/**
 * Save audio buffer to a file called "wavesynth_<wsIndex>.wav".
 */
public void saveToAudio() {
  renderSignal();
  try {
    saveAudioToFile(audioSignal, sampleRate, "wavesynth_"+ wsIndex +".wav");
  }
  catch (IOException e) {
    println("--->> There was an error outputting the audio file wavesynth.wav "+ e.getMessage());
  }
  catch (UnsupportedAudioFileException e) {
    println("--->> The file format is unsupported "+ e.getMessage());
  }
}

/**
 * Calls WaveSynth to render a audio sample array derived from the same math that creates the image,
 * then loads the derived audio data to audioBuffer, ready to be played.
 */
public void renderSignal() {
  this.audioSignal = wavesynth.renderAudioRaw(step);           // get the signal "as is" from WaveSynth
  audioSignal = WaveSynth.normalize(audioSignal, 0.9f);        // normalize samples to the range (-0.9f, 0.9f)
  audioLength = audioSignal.length;
  audioBuffer.setBufferSize(audioLength);
  audioBuffer.setChannel(0, audioSignal);                      // copy audioSignal to channel 0 of audioBuffer
  // println("--->> copied audio signal to audio buffer");
}

// Called by mousePressed(), this should be a bottleneck method for all playSample() calls.
public void audioMousePressed(int sampleX, int sampleY) {
  this.sampleX = sampleX;
  this.sampleY = sampleY;
  samplePos = mapper.lookupSample(sampleX, sampleY);
  checkBufferState();
  playSample(samplePos, calcSampleLen(), 0.6f, adsr);
}

public void checkBufferState() {
  if (isBufferStale) {
    println("--->> Stale buffer refreshed ");
    // any changes to image are equivalent changes to audio, so isBufferStale is set often
    renderSignal();
    pool.setBuffer(audioBuffer);
    isBufferStale = false;
  }
}

/**
 * Plays an audio sample with WFSamplerInstrument and custom ADSR.
 *
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @param defaultEnv         an ADSR envelope for the sample
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env) {
  samplelen = pool.playSample(samplePos, (int) samplelen, amplitude, env);
  int durationMS = (int)(samplelen/sampleRate * 1000);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
  // return the length of the sample
  return samplelen;
}

/**
 * Plays an audio sample with WFSamplerInstrument and default ADSR.
 *
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos, int samplelen, float amplitude) {
  samplelen = pool.playSample(samplePos, (int) samplelen, amplitude);
  int durationMS = (int)(samplelen/sampleRate * 1000);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
  // return the length of the sample
  return samplelen;
}

public int calcSampleLen() {
  float vary = 0;
  // skip the fairly rare negative numbers
  while (vary <= 0) {
    vary = (float) PixelAudio.gauss(1.0, 0.0625);
  }
  samplelen = (int)(abs((vary * this.noteDuration) * sampleRate / 1000.0f));
  // println("---- calcSampleLen samplelen = "+ samplelen +" samples at "+ sampleRate +"Hz sample rate");
  return samplelen;
}

/**
 * Run the animation for audio events.
 */
public void runTimeArray() {
  int currentTime = millis();
  timeLocsArray.forEach(tl -> {
    tl.setStale(tl.eventTime() < currentTime);
    if (!tl.isStale()) {
      drawCircle(tl.getX(), tl.getY());
    }
  }
  );
  timeLocsArray.removeIf(TimedLocation::isStale);
}

/**
 * Draws a circle at the location of an audio trigger (mouseDown event).
 * @param x        x coordinate of circle
 * @param y        y coordinate of circle
 */
public void drawCircle(int x, int y) {
  //float size = isRaining? random(10, 30) : 60;
  fill(color(233, 220, 199));
  noStroke();
  circle(x, y, 60);
}
