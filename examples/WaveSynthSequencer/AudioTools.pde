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
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  // set up a audioBuffer
  this.audioBuffer = new MultiChannelBuffer(mapSize, 1);
  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
  adsr = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  // create a pool of eight monophonic instruments
  pool = new PASamplerInstrumentPool(audioBuffer, sampleRate, 8, 1, audioOut, adsr);
  // initialize mouse event tracking array
  timeLocsArray = new ArrayList<TimedLocation>();
}

// Called by mousePressed(), this should be a bottleneck method for all playSample() calls.
public void audioMousePressed(int sampleX, int sampleY) {
  this.sampleX = sampleX;
  this.sampleY = sampleY;
  samplePos = mapper.lookupSample(sampleX, sampleY);
  checkBufferState(isBufferStale);
  playSample(samplePos, calcSampleLen(), 0.6f, adsr);
}

/**
 * Calls WaveSynth to render a audio sample array derived from the same math that creates the image,
 * then loads the derived audio data to audioBuffer, ready to be played.
 */
public void renderSignal() {
  this.audioSignal = wavesynth.renderAudioRaw(step);           // get the signal "as is" from WaveSynth
  audioSignal = WaveSynth.normalize(audioSignal, 0.9f);        // normalize samples to the range (-0.9f, 0.9f)
  audioLength = audioSignal.length;
  isBufferStale = true;
  audioBuffer.setBufferSize(audioLength);
  audioBuffer.setChannel(0, audioSignal);                      // copy audioSignal to channel 0 of audioBuffer
  // println("--->> copied audio signal to audio buffer");
}

public void checkBufferState(boolean isStale) {
  if (isStale) {
    // println("--->> Stale buffer refreshed ");
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
 * Run the WaveSynth Sequencer.
 */
public void runMusicArray() {
  int currentTime = millis();
  introMusic.forEach(tl -> {
    if (tl.eventTime() < currentTime) {
      isWaveSynthAnimating = false;
      wavesynth = tl.getWaveSynth();
      wavesynth.prepareAnimation();
      wavesynth.renderFrame(0);
      synthImage = wavesynth.mapImage;
      renderSignal();
      checkBufferState(isBufferStale);
      int len = (int) ((48 * tl.duration) * 0.4f);
      sampleX = tl.getX();
      sampleY = tl.getY();
      circleColor = tl.getCircleColor();
      // println("---> music ", tl.getX(), tl.getY(), mapper.lookupSample(tl.getX(), tl.getY()));
      // println("---> music ", twoPlaces.format(tl.getAmplitude()));
      playSample(mapper.lookupSample(tl.getX(), tl.getY()), len, tl.getAmplitude(), tl.getAdsr());
      tl.setStale(true);
    } 
    else {
      return;
    }
  }
  );
  introMusic.removeIf(NoteTimedLocation::isStale);
}

/**
 * Trigger a WaveSynth sample at a random location.
 */
public void raindrops() {
  int signalPos = (int) random(samplelen, mapSize - samplelen - 1);
  int[] coords = mapper.lookupCoordinate(signalPos);
  sampleX = coords[0];
  sampleY = coords[1];
  if (audioSignal == null || isBufferStale) {
    renderSignal();
    isBufferStale = false;
  }
  playSample(signalPos, samplelen, 0.15f, new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime));
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
