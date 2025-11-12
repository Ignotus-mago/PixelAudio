/*----------------------------------------------------------------*/
/*                                                                */
/*                        AUDIO METHODS                           */
/*                                                                */
/*----------------------------------------------------------------*/

/**
 * CALL THIS METHOD IN SETUP()
 * Initializes Minim audio library and audio variables.
 */
public void initAudio() {
  minim = new Minim(this);
  // Use the getLineOut method of the Minim object to get an AudioOutput object.
  // PixelAudio instruments require a STEREO output. 1024 is a standard number
  // of samples for the output buffer to process at one time. You should usually
  // set the output sampleRate to either 41500 or 48000, standards for digital
  // audio recordings.
  this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
  // reduce the output level by 6.0 dB.
  audioOut.setGain(-6.0f);
  // create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
  // playBuffer will not contain audio data until we load a file
  this.playBuffer = new MultiChannelBuffer(mapSize, 1);
  this.audioSignal = playBuffer.getChannel(0);
  this.audioLength = audioSignal.length;
  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
  defaultEnv = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  initADSRList();
  // create a PASamplerInstrument with 8 voices, adsrParams will be its default envelope
  synth = new PASamplerInstrument(playBuffer, audioOut.sampleRate(), 8, audioOut, defaultEnv);
  // initialize mouse event tracking array
  timeLocsArray = new ArrayList<TimedLocation>();
}

public void initADSRList() {
  adsrList = new ArrayList<ADSRParams>();
  ADSRParams env0 = new ADSRParams(
    0.7f, // maxAmp
    0.05f, // fast attack (s)
    0.1f, // fast decay (s)
    0.35f, // sustain level
    0.5f    // slow release (s)
    );
  ADSRParams env1 = new ADSRParams(
    0.7f, // maxAmp
    0.4f, // slow attack (s)
    0.0f, // no decay (s)
    0.7f, // sustain level
    0.4f    // slow release (s)
    );
  ADSRParams env2 = new ADSRParams(
    0.7f, // maxAmp
    0.75f, // slow attack (s)
    0.1f, // decay (s)
    0.6f, // sustain level
    0.05f    // release (s)
    );
  adsrList.add(env0);
  adsrList.add(env1);
  adsrList.add(env2);
}

/**
 * Prepares audioSignal before it is used as an instrument source.
 * Modify as needed to prepare your audio signal data.
 */
public void renderSignals() {
  writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
  playBuffer.setChannel(0, audioSignal);
  audioLength = audioSignal.length;
}

/**
 * Typically called from mousePressed with mouseX and mouseY, generates audio events.
 *
 * @param x    x-coordinate within a PixelAudioMapper's width
 * @param y    y-coordinate within a PixelAudioMapper's height
 */
public void audioMousePressed(int x, int y) {
  setSampleVars(x, y);    // set sampleX, sampleY, samplePos
  float panning = map(sampleX, 0, width, -0.8f, 0.8f);
  // update audioSignal and playBuffer if audioSignal hasn't been initialized or if
  // playBuffer needs to be refreshed after changes to its data source (isBufferStale == true).
  if (audioSignal == null || isBufferStale) {
    renderSignals();
    isBufferStale = false;
  }
  if (isRandomADSR) {
    ADSRParams env = adsrList.get((int)random(adsrList.size()));
    println("-- "+ env.toString());
    playSample(samplePos, calcSampleLen(), 0.6f, env, panning);
  } else {
    playSample(samplePos, calcSampleLen(), 0.6f, panning);
  }
}

/**
 * Sets variables sampleX, sampleY and samplePos. Arguments x and y may be outside
 * the window bounds, sampleX and sampleY will be constrained to window bounds. As
 * a result, samplePos will be within the bounds of audioSignal.
 *
 * @param x    x coordinate, typically from a mouse event
 * @param y    y coordinate, typically from a mouse event
 * @return     samplePos, the index of of (x, y) along the signal path
 */
public int setSampleVars(int x, int y) {
  sampleX = min(max(0, x), width - 1);
  sampleY = min(max(0, y), height - 1);
  samplePos = getSamplePos(sampleX, sampleY);
  return samplePos;
}

/**
 * Calculates the index of the image pixel within the signal path,
 * taking the shifting of pixels and audioSignal into account.
 * See the MusicWindowBox sketch for use of a windowed buffer in this calculation.
 *
 * @param x    an x coordinate within mapImage and display bounds
 * @param y    a y coordinate within mapImage and display bounds
 * @return     the index of the sample corresponding to (x,y) on the signal path
 */
public int getSamplePos(int x, int y) {
  int pos = mapper.lookupSample(x, y);
  // calculate how much animation has shifted the indices into the buffer
  totalShift = (totalShift + shift % mapSize + mapSize) % mapSize;
  return (pos + totalShift) % mapSize;
}

/**
 * Plays an audio sample with PASamplerInstrument and custom ADSR.
 *
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @param adsr         an ADSR envelope for the sample
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pan) {
  samplelen = synth.playSample(samplePos, (int) samplelen, amplitude, env, pitchScaling, pan);
  int durationMS = (int)(samplelen/sampleRate * 1000);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
  // return the length of the sample
  return samplelen;
}

/**
 * Plays an audio sample with PASamplerInstrument and default ADSR.
 *
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos, int samplelen, float amplitude, float pan) {
  samplelen = synth.playSample(samplePos, (int) samplelen, amplitude, defaultEnv, pitchScaling, pan);
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
 * @param x    x coordinate of circle
 * @param y    y coordinate of circle
 */
public void drawCircle(int x, int y) {
  //float size = isRaining? random(10, 30) : 60;
  fill(color(233, 220, 199));
  noStroke();
  circle(x, y, 60);
}

/*        END AUDIO METHODS                        */
