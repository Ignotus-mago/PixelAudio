/*----------------------------------------------------------------*/
/*                                                                */
/*                        AUDIO METHODS                           */
/*                                                                */
/*----------------------------------------------------------------*/

// *** WindowedBuffer support *** //
/**
 * CALL THIS METHOD IN SETUP()
 * Initializes Minim audio library and audio variables.
 */
public void initAudio() {
  windowAudioReady = false;    // suspend operations that use the backing buffer
  // initialize the Minim library
  minim = new Minim(this);
  // Use the getLineOut method of the Minim object to get an AudioOutput object.
  // PixelAudio instruments require a STEREO output. 1024 is a standard number of
  // samples for the output buffer to process at one time. You should usually set
  // the output sampleRate to either 41500 or 48000, standards for digital audio.
  this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
  // set the gain (UP and DOWN arrow keys adjust)
  audioOut.setGain(outputGain);
  println("---- audio out gain is "+ nf(audioOut.getGain(), 0, 2));
  // create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
  // the buffer will not have any audio data -- you'll need to open an audio file
  this.playBuffer = new MultiChannelBuffer(mapSize, 1);
  this.audioSignal = playBuffer.getChannel(0);
  this.granSignal = audioSignal;
  this.audioLength = audioSignal.length;
  // start envelope duration at noteDuration
  envDuration = noteDuration;
  // setup the backing buffer
  installBackingSource(playBuffer.getChannel(0), audioOut.sampleRate());
  // refresh variables that depend on the backing buffer
  refreshWindowFromBacking(false);
  windowAudioReady = true;
  // set up the sampler synth, taking the backing buffer into account
  ensureSamplerReady();
  // set up the granular synth, taking the backing buffer into account
  ensureGranularReady();
  // initialize audio event animation tracking arrays
  initTimedEventLists();
}

// *** WindowedBuffer support *** //
/**
 * Installs a full-file backing source for WindowedBuffer and instruments.
 * Sets the window size to mapper.getSize()
 */
void installBackingSource(float[] sig, float sourceSampleRate) {
  int windowSize = (mapper != null) ? mapper.getSize() : mapSize;
  if (windowSize <= 0) return;

  int backingSize = Math.max(windowSize, (sig != null) ? sig.length : 0);
  float[] backing = new float[backingSize];
  if (sig != null) {
    System.arraycopy(sig, 0, backing, 0, Math.min(sig.length, backing.length));
  }

  anthemSignal = backing;
  anthemBuffer = new MultiChannelBuffer(backing.length, 1);
  anthemBuffer.setChannel(0, anthemSignal);

  bufferSampleRate = sourceSampleRate;
  audioFileLength = backing.length;
  windowHopSize = Math.max(1, Math.round(sourceSampleRate / Math.max(1f, frameRate)));
  windowBuff = new WindowedBuffer(anthemSignal, windowSize, windowHopSize);

  granSignal = anthemSignal;

  println("---- WindowBuffer backing source length = " + anthemSignal.length
    + ", window size = " + windowBuff.getWindowSize()
    + ", hop size = " + windowBuff.getHopSize());
}

// *** WindowedBuffer support *** //
/**
 * Copies the current window into audioSignal and playBuffer. The Sampler and
 * Granular instruments still read from anthemSignal / anthemBuffer.
 */
void refreshWindowFromBacking(boolean advance) {
  if (windowBuff == null || anthemSignal == null) return;

  if (advance) {
    windowBuff.nextWindow(); // advances exactly once
  }

  int targetSize = mapper.getSize();
  int start = PixelAudioMapper.wrap(windowBuff.getIndex(), anthemSignal.length);

  if (audioSignal == null || audioSignal.length != targetSize) {
    audioSignal = new float[targetSize];
  }

  for (int i = 0; i < targetSize; i++) {
    audioSignal[i] = anthemSignal[(start + i) % anthemSignal.length];
  }

  audioLength = audioSignal.length;

  if (playBuffer == null || playBuffer.getBufferSize() != targetSize) {
    playBuffer = new MultiChannelBuffer(targetSize, 1);
  }

  playBuffer.setChannel(0, audioSignal);

  totalShift = 0;
  renderAudioToMapImage(chan, 0);
  commitMapImageToBaseImage();
}

// *** WindowedBuffer support *** //
/**
 * Ensures that all resources and variable necessary for the Sampler synth are ready to go.
 */
void ensureSamplerReady() {
  // Prevent super.initAudio() from creating a sampler pool before
  // anthemBuffer has been installed.
  if (!windowAudioReady) return;
  // Prefer anthemBuffer as our source
  MultiChannelBuffer source = (anthemBuffer != null) ? anthemBuffer : playBuffer;
  if (isDebugging) {
    if (source == anthemBuffer) println("-- sampler source is anthemBuffer");
    if (source == playBuffer) println("-- sampler source is playBuffer");
    if (source == null) println("-- sampler source is null");
  }
  float sr = (bufferSampleRate > 0) ? bufferSampleRate : sampleRate;
  if (source == null) return;
  // Rebuild the Sampler instrument, pool, when required
  // Loading a new file to the backing buffer should trigger this
  boolean needsRebuild =
    pool == null
    || pool.isClosed()
    || source != samplerSourceRef
    || sr != samplerSourceRateRef;

  if (needsRebuild) {
    if (pool != null && !pool.isClosed()) {
      pool.stopAll();
      pool.close();
    }
    pool = new PASamplerInstrumentPool(
      source,
      sr,
      poolSize,
      samplerMaxVoices,
      audioOut,
      defaultEnv
      );
    samplerSourceRef = source;
    samplerSourceRateRef = sr;
  } else {
    // Important: keep the whole pool bound to anthemBuffer.
    // Do not rely on playSample(anthemBuffer, ...) to patch one instrument at a time.
    pool.setBuffer(source, sr);
  }

  pool.setGain(samplerGain);
}

// *** WindowedBuffer support *** //
/**
 * Ensures that all resources and variable necessary for the Granular synth are ready to go.
 */
void ensureGranularReady() {
  if (gParamsGesture == null || gParamsFixed == null) {
    initGranularParams();
  }
  if (gSynth == null) {
    ADSRParams granEnv = new ADSRParams(1.0f, 0.005f, 0.02f, 0.875f, 0.025f);
    gSynth = buildGranSynth(audioOut, granEnv, gMaxVoices);
    gSynth.setGlobalGain(granularGain);
  }
  if (granSignal == null) {
    granSignal = (audioSignal != null) ? audioSignal : playBuffer.getChannel(0);
  }
  if (gDir == null) {
    gDir = new PAGranularInstrumentDirector(gSynth);
  }
  // when we use a backing buffer, point granSignal at anthemSignal
  if (anthemSignal != null) granSignal = anthemSignal;
}

void initTimedEventLists() {
  // initialize mouse event tracking array
  pointTimeLocs = new ArrayList<TimedLocation>();
  samplerTimeLocs = new ArrayList<>();   // capture timing data when drawing
  grainTimeLocs = new ArrayList<>();
}

/**
 * Initializes global variables gParamsGesture and gParamsFixed, which provide basic
 * settings for granular synthesis the follows gesture timing or fixed hop timing
 * between grains.
 */
public void initGranularParams() {
  ADSRParams env = this.calculateEnvelopeLinear(granularGain, 1000);
  gParamsGesture = GestureGranularParams.builder()
    .grainLengthSamples(granSamples)
    .hopLengthSamples(hopSamples)
    .gainLinear(granularGain)
    .looping(false)
    .env(env)
    .hopMode(GestureGranularParams.HopMode.GESTURE)
    .burstGrains(burstGrains)
    .build();
  gParamsFixed = GestureGranularParams.builder()
    .grainLengthSamples(granSamples)
    .hopLengthSamples(hopSamples)
    .gainLinear(granularGain)
    .looping(false)
    .env(env)
    .hopMode(GestureGranularParams.HopMode.FIXED)
    .burstGrains(burstGrains)
    .build();
}

// *** WindowedBuffer support *** //
/**
 * Point clicks are mouse location, but the initial samplePos is the
 * backing source index. Long granular bursts still create
 * display points in the current window; playGranularGesture(...) remaps
 * them to backing indices.
 *
 * @param x
 * @param y
 */
public int handleClickOutsideBrush(int x, int y) {
  int localSamplePos = mapper.lookupSignalPosShifted(x, y, totalShift);
  samplePos = backingIndexFromLocal(localSamplePos);

  if (!useGranularSynth) {
    ensureSamplerReady();
    float panning = map(x, 0, width, -0.8f, 0.8f);
    int len = calcSampleLen();
    samplelen = playSample(samplePos, len, samplerPointGain, defaultEnv, panning);
    int durationMS = (int)(samplelen / sampleRate * 1000);
    pointTimeLocs.add(new TimedLocation(x, y, durationMS + millis() + 50));
    return durationMS;
  }

  ensureGranularReady();
  float hopMsF = (int)Math.round(AudioUtility.samplesToMillis(hopSamples, sampleRate));
  final int grainCount = useLongBursts
    ? 2 * (int)Math.round(noteDuration / hopMsF)
    : (int)Math.round(noteDuration / hopMsF);
  println("-- granular point burst with grainCount = " + grainCount);

  java.util.ArrayList<PVector> path = new java.util.ArrayList<>(grainCount);
  int[] timing = new int[grainCount];
  for (int i = 0; i < grainCount; i++) {
    if (useLongBursts) {
      int local = PixelAudioMapper.wrap(localSamplePos + hopSamples * i, mapper.getSize());
      path.add(getCoordFromSignalPosLocal(local));
    } else {
      path.add(this.jitterCoord(x, y, 3));
    }
    timing[i] = Math.round(i * hopMsF);
  }

  int startTime = millis() + 10;
  PACurveMaker curve = PACurveMaker.buildCurveMaker(path);
  curve.setDragTimes(timing);
  curve.setTimeStamp(startTime);
  GestureSchedule sched = curve.getAllPointsSchedule();
  playGranularGesture(backingGranularSignal(), sched, gParamsFixed, 1.0f);
  storeGranularCurveTL(sched, startTime, false);
  return curve.getTimeOffset();
}

// *** WindowedBuffer support *** //
float[] backingGranularSignal() {
  return (anthemSignal != null && anthemSignal.length > 0) ? anthemSignal : audioSignal;
}


// *** WindowedBuffer support *** //
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
  int local = mapper.lookupSignalPosShifted(x, y, totalShift);
  return backingIndexFromLocal(local);
}

// *** WindowedBuffer support *** //
int backingIndexFromLocal(int localIndex) {
  if (windowBuff == null || anthemSignal == null || anthemSignal.length == 0) {
    return PixelAudioMapper.wrap(localIndex, Math.max(1, mapper.getSize()));
  }
  int local = PixelAudioMapper.wrap(localIndex, mapper.getSize());
  return PixelAudioMapper.wrap(windowBuff.getIndex() + local, anthemSignal.length);
}

/**
 * Calculates the display image coordinates corresponding to a specified audio sample index,
 * use this version when the position is used to calculate local display coordinates.
 * @param pos    an index into an audio signal, must be between 0 and width * height - 1.
 * @return       a PVector with the x and y coordinates
 */
public PVector getCoordFromSignalPosLocal(int pos) {
  int[] xy = this.mapper.lookupImageCoordShifted(pos, totalShift);
  return new PVector(xy[0], xy[1]);
}

// *** WindowedBuffer support *** //
/**
 * Calculates the display image coordinates corresponding to a specified audio sample index,
 * use when the position is relative to the backing buffer.
 * @param pos    an index into an audio signal, must be between 0 and width * height - 1.
 * @return       a PVector with the x and y coordinates
 *
 * Backing-file index -> visible display coordinate, relative to the current window.
 */
public PVector getCoordFromSignalPos(int pos) {
  int local = pos;
  if (windowBuff != null && anthemSignal != null && anthemSignal.length > 0) {
    local = PixelAudioMapper.wrap(pos - windowBuff.getIndex(), mapper.getSize());
  }
  int[] xy = mapper.lookupImageCoordShifted(local, totalShift);
  return new PVector(xy[0], xy[1]);
}

	/**
	 * Draws a circle at the location of an audio trigger (mouseDown event).
	 * @param x		x coordinate of circle
	 * @param y		y coordinate of circle
	 */
public void drawCircle(int x, int y) {
  //float size = isRaining? random(10, 30) : 60;
  fill(animatedCircleColor);
  noStroke();
  circle(x, y, 16);
}


/*----------------------------------------------------------------*/
/*       See PASamplerInstrument and PASamplerInstrumentPool      */
/*       for all the ways you can play a Sampler instrument.      */
/*       The ones listed here are the most useful of them.        */
/*----------------------------------------------------------------*/

/**
 * Plays an audio sample with default envelope and stereo pan.
 *
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos, int samplelen, float amplitude, float pan) {
  return playSample(samplePos, samplelen, amplitude, defaultEnv, 1.0f, pan);
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
  return playSample(samplePos, samplelen, amplitude, env, 1.0f, pan);
}

// *** WindowedBuffer support *** //
/**
 * Plays an audio sample with  with a custom envelope, pitch and stereo pan.
 *
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @param env          an ADSR envelope for the sample
 * @param pitch        pitch scaling as deviation from default (1.0), where 0.5 = octave lower, 2.0 = octave higher
 * @param pan          position of sound in the stereo audio field (-1.0 = left, 0.0 = center, 1.0 = right)
 * @return
 */
public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pitch, float pan) {
  if (anthemBuffer == null || pool == null) {
    return 0;
    // return playSample(samplePos, samplelen, amplitude, env, pitch, pan);
  }
  // debugging
  // println("-- retrofit playSample() call ");
  int pos = PixelAudioMapper.wrap(samplePos, anthemBuffer.getBufferSize());
  int len = Math.min(samplelen, anthemBuffer.getBufferSize() - pos);
  // debugging information for windowed buffer
  // println("---- 06 playSample pos=" + pos + " len=" + len
  //    + " anthemBuffer=" + anthemBuffer.getBufferSize()
  //    + " windowIndex=" + windowBuff.getIndex());
  return pool.playSample(samplePos, samplelen, amplitude, env, pitch, pan);
}


/**
 * @return a length in samples with some Gaussian variation
 */
public int calcSampleLen(int durMs, double mean, double variance) {
  float vary = 0;
  // skip the fairly rare negative numbers
  while (vary <= 0) {
    vary = (float) PixelAudio.gauss(mean, variance);
  }
  samplelen = (int)(abs((vary * durMs) * sampleRate / 1000.0f));
  // println("---- calcSampleLen samplelen = "+ samplelen +" samples at "+ sampleRate +"Hz sample rate");
  return samplelen;
}

/**
 * @return a length in samples with some Gaussian variation
 */
public int calcSampleLen() {
  return calcSampleLen(envDuration, 1.0, 0.0625);
}

/**
 * Initializes a new PAGranularSynth instance that you probably would pass to a PAGranularInstrumentDirector.
 *
 * @param out          and AudioOutput, most likely the one used by this sketch
 * @param env          an ADSRParams envelope
 * @param numVoices    the number of voices to use for synthesizing simultaneous grains
 * @return             a PAGranularSynth instance
 */
public PAGranularInstrument buildGranSynth(AudioOutput out, ADSRParams env, int numVoices) {
  return new PAGranularInstrument(out, env, numVoices);
}

/**
 * @param name   the name of the ADSRParams envelope to return
 * @return the specified ADSRParams envelope
 */
static ADSRParams envPreset(String name) {
  switch (name) {
    // medium fast attack, medium decay to 0.15, short tail
  case "Soft"       :
    return new ADSRParams(1.0f, 0.02f, 0.80f, 0.7f, 0.2f);
    // sharp attack, fast decay to 0.1, short tail
  case "Percussion" :
    return new ADSRParams(1.0f, 0.005f, 0.04f, 0.20f, 0.06f);
    // long attack, slight decay, long tail
  case "Pad"        :
    return new ADSRParams(1.0f, 0.50f, 0.25f, 0.75f, 1.0f);
    // fast attack, fast decay to 0.75, medium tail
  default           :
    return new ADSRParams(1.0f, 0.01f, 0.15f, 0.75f, 0.25f);
  }
}

// *** WindowedBuffer support *** //
/**
 * Suspend audio instruments and events when a new backing buffer is in the process of loading.
 */
void suspendWindowAudio() {
  // clear audio/animation TimedLocation events
  if (samplerTimeLocs != null) samplerTimeLocs.clear();
  if (pointTimeLocs != null) pointTimeLocs.clear();
  if (grainTimeLocs != null) grainTimeLocs.clear();
  // shut down Sampler instrument, pool
  if (pool != null) {
    pool.stopAll();
    pool.close();
    pool = null;
  }
  samplerSourceRef = null;
  samplerSourceRateRef = -1;
  // shut down Granular instrument, gDir
  if (gDir != null) gDir.cancelAndReleaseAll();
}


// *** WindowedBuffer support *** //
/**
 * Bottleneck "commit" method for audio state.
 *
 * This is the ONLY method that should mutate the global audio signal state.
 *
 * Modified for use with WindowedBuffer and a backing buffer, installBackingSource()
 * and refreshWindowFromBacking() set the various buffers. The ensureSamplerReady()
 * and ensureGranularReady() reset instruments.
 *
 * In PixelAudio examples, the signal is typically loaded from a file, but
 * it could also be signal cached in memory, a signal generated by code, audio
 * captured live, etc.
 *
 * @param sig                 an audio signal
 * @param sourceSampleRate    audio sample rate for sig,
 *                            usually obtained when reading from an audio file
 */
void updateAudioChain(float[] sig, float sourceSampleRate) {
  installBackingSource(sig, sourceSampleRate);
  refreshWindowFromBacking(false);
  // critical: rebind instruments after anthemBuffer/anthemSignal change
  ensureSamplerReady();
  ensureGranularReady();
}

// *** WindowedBuffer support *** //
void updateAudioChain(float[] sig) {
  float sr = (audioOut != null) ? audioOut.sampleRate() : sampleRate;
  updateAudioChain(sig, sr);
}


// *** WindowedBuffer support *** //
/**
 * Calls PAGranularInstrumentDirector gDir to play a granular audio event.
 *
 * @param buf       an audio signal as an array of float (null, if we are using the backing buffer)
 * @param sched     an GestureSchedule with coordinate and timing information
 * @param params    a bundle of control parameters for granular synthesis
 */
public void playGranularGesture(float buf[], GestureSchedule sched, GestureGranularParams params, float pitchRatio) {
  if (sched == null || sched.isEmpty()) return;
  float[] source = (buf != null) ? buf : backingGranularSignal();
  int sourceLen = (source != null && source.length > 0) ? source.length : mapper.getSize();

  int[] localIndices = mapper.lookupSignalPosArray(sched.points, totalShift, mapper.getSize());
  int[] startIndices = new int[localIndices.length];
  for (int i = 0; i < localIndices.length; i++) {
    startIndices[i] = (windowBuff != null)
      ? PixelAudioMapper.wrap(windowBuff.getIndex() + localIndices[i], sourceLen)
      : PixelAudioMapper.wrap(localIndices[i], sourceLen);
  }

  // calculate the pan for each grain, based on its x-coordinate
  float[] panPerGrain = new float[sched.size()];
  for (int i = 0; i < sched.size(); i++) {
    PVector p = sched.points.get(i);
    // example: map x to [-0.8, +0.8]
    panPerGrain[i] = map(p.x, 0, width-1, -0.875f, 0.875f);
  }

  float jitter = usePitchedGrains ? 0.25f : 0f;
  float[] pitch = generateJitterPitch(sched.size(), jitter, pitchRatio);
  // assemble all the grain-level attributes into a GestureEventParams object
  GestureEventParams eventParams = GestureEventParams.builder(sched.size())
    .startIndices(startIndices)
    .pan(panPerGrain)
    .pitchRatio(pitch)
    .build();
  // call playGestureNow() with eventParams and return
  gDir.playGestureNow(buf, sched, params, eventParams);
}

/**
 * Calculate an envelope of length totalSamples.
 * @param gainDb          desired gain in dB, currently ignored
 * @param totalSamples    number of samples the envelope should cover
 * @param sampleRate      sample rate of the audio buffer the envelope is applied to
 * @return and ADSRParams envelope
 */
public ADSRParams calculateEnvelopeDb(float gainDb, int totalSamples, float sampleRate) {
  float linear = AudioUtility.dbToLinear(gainDb);
  return calculateEnvelopeLinear(linear, totalSamples * 1000f / sampleRate);
}

/**
 * Calculate an envelope of length totalSamples.
 * @param gainDb     desired gain in dB, currently ignored
 * @param totalMs    desired duration of the envelope in milliseconds
 * @return an ADSRParams envelope
 */
public ADSRParams calculateEnvelopeLinear(float gainDb, float totalMs) {
  float attackMS = Math.min(50, totalMs * 0.1f);
  float releaseMS = Math.min(200, totalMs * 0.3f);
  float envGain = AudioUtility.dbToLinear(gainDb);
  envGain = 1.0f;
  return new ADSRParams(envGain, attackMS / 1000f, 0.01f, 0.8f, releaseMS / 1000f);
}


/*        END AUDIO METHODS                        */
