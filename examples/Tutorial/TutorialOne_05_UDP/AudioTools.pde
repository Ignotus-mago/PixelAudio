/*----------------------------------------------------------------*/
/*                                                                */
/*                     BEGIN AUDIO METHODS                        */
/*                                                                */
/*----------------------------------------------------------------*/

/**
 * CALL THIS METHOD IN SETUP()
 * Initializes Minim audio library and audio variables.
 */
public void initAudio() {
  minim = new Minim(this);
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
  // set the gain lower to avoid clipping from multiple voices
  audioOut.setGain(-18.0f);
  println("---- audio out gain is "+ audioOut.getGain());
  // create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
  // the buffer will not have any audio data -- you'll need to open a file for that
  this.playBuffer = new MultiChannelBuffer(mapSize, 1);
  this.audioSignal = playBuffer.getChannel(0);
  this.audioLength = audioSignal.length;
  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
  defaultEnv = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  granularEnv = ADSRUtils.fitEnvelopeToDuration(defaultEnv, grainDuration);
  // create a PASamplerInstrument, though playBuffer is all 0s at the moment
  // adsrParams will be the default ADSR for the synth
  // allocate plenty of voices, the drawing interface generates a lot of audio events
  synth = new PASamplerInstrument(playBuffer, audioOut.sampleRate(), maxVoices, audioOut, defaultEnv);
  // initialize mouse event tracking array
  timeLocsArray = new ArrayList<TimedLocation>();
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
  setSampleVars(x, y);
  float panning = map(sampleX, 0, width, -0.8f, 0.8f);
  // update audioSignal and playBuffer if audioSignal hasn't been initialized or if
  // playBuffer needs to be refreshed after changes to its data source (isBufferStale == true).
  // TODO logic not used in current version, should be deleted?
  if (audioSignal == null || isBufferStale) {
    renderSignals();
    isBufferStale = false;
  }
  if (pool == null || synth == null) {
    println("You need to load an audio file before you can trigger audio events.");
  } else {
    // use the default envelope
    if (!isGranular) {
      int len = calcSampleLen();
      playSample(samplePos, len, defaultGain, defaultEnv, panning);
      println("variable duration "+ len);
    } else {
      int len = (int)(abs((this.grainDuration) * sampleRate / 1000.0f));
      playSample(samplePos, len, defaultGain, granularEnv, panning);
      println("---- grain "+ len);
    }
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
 * See MusicBoxBuffer for use of a windowed buffer in this calculation.
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



/*----------------------------------------------------------------*/
/*      How do I call playSample()? Let me count the ways.        */
/*----------------------------------------------------------------*/

/**
 * Plays an audio sample with the default envelope.
 *
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos, int samplelen, float amplitude) {
  if (pool == null || synth == null) return 0;
  if (isUseSynth) {
    samplelen = synth.playSample(samplePos, samplelen, amplitude);
  } else {
    samplelen = pool.playSample(samplePos, samplelen, amplitude);
  }
  int durationMS = (int)(samplelen/sampleRate * 1000);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
  // return the length of the sample
  return samplelen;
}

/**
 * Plays an audio sample with a custom envelope.
 *
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @param env          an ADSR envelope for the sample
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env) {
  if (pool == null || synth == null) return 0;
  if (isUseSynth) {
    samplelen = synth.playSample(samplePos, samplelen, amplitude, env);
  } else {
    samplelen = pool.playSample(samplePos, samplelen, amplitude, env);
  }
  int durationMS = (int)(samplelen/sampleRate * 1000);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
  // return the length of the sample
  return samplelen;
}


/**
 * Plays an audio sample with default envelope and stereo pan.
 *
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos, int samplelen, float amplitude, float pan) {
  if (synth == null || pool == null) {
    return 0;
  }
  if (isUseSynth) {
    samplelen = synth.playSample(samplePos, samplelen, amplitude, defaultEnv, pitchScaling, pan);
  } else {
    samplelen = pool.playSample(samplePos, samplelen, amplitude, defaultEnv, pitchScaling, pan);
  }
  int durationMS = (int)(samplelen/sampleRate * 1000);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
  // return the length of the sample
  return samplelen;
}

/**
 * Plays an audio sample with a custom envelope and stereo pan.
 *
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @param env          an ADSR envelope for the sample
 * @param pan          position of sound in the stereo audio field (-1.0 = left, 0.0 = center, 1.0 = right)
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pan) {
  if (synth == null || pool == null) {
    return 0;
  }
  if (isUseSynth) {
    samplelen = synth.playSample(samplePos, samplelen, amplitude, env, pitchScaling, pan);
  } else {
    samplelen = pool.playSample(samplePos, samplelen, amplitude, env, pitchScaling, pan);
  }
  int durationMS = (int)(samplelen/sampleRate * 1000);
  // println("---->> adding event to timeLocsArray "+  samplelen, durationMS, millis());
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
  // return the length of the sample
  return samplelen;
}

/**
 * Plays an audio sample with  with a custom envelope, pitch and stereo pan.
 *
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @param env          an ADSR envelope for the sample
 * @param pitch        pitch scaling as deviation from default (1.0), where 0.5 = octave lower, 2.0 = oactave higher
 * @param pan          position of sound in the stereo audio field (-1.0 = left, 0.0 = center, 1.0 = right)
 * @return
 */
public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pitch, float pan) {
  if (synth == null || pool == null) {    println("-- You need to load an audio file before you can activate audio features.");
    return 0;
  }
  if (isUseSynth) {
    samplelen = synth.playSample(samplePos, samplelen, amplitude, env, pitch, pan);
  } else {
    samplelen = pool.playSample(samplePos, samplelen, amplitude, env, pitch, pan);
  }
  int durationMS = (int)(samplelen/sampleRate * 1000);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
  // return the length of the sample
  return samplelen;
}


/**
 * @return a length in samples with some Gaussian variation
 */
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

/*        END AUDIO METHODS                        */
