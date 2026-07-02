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
  // set up the sampler synth
  ensureSamplerReady();
  // set up the granular synth
  ensureGranularReady();
  // start envelope duration at noteDuration
  envDuration = noteDuration;
  // initialize mouse event tracking array
  pointTimeLocs = new ArrayList<TimedLocation>();
}

/**
 * Ensures that all resources and variable necessary for the Sampler synth are ready to go.
 */
void ensureSamplerReady() {
  if (pool != null) pool.setBuffer(playBuffer);
  else pool = new PASamplerInstrumentPool(playBuffer, sampleRate, poolSize, samplerMaxVoices, audioOut, defaultEnv);
  pool.setGain(samplerGain);
}

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

/**
 * @param x
 * @param y
 */
public int handleClickOutsideBrush(int x, int y) {
  samplePos = mapper.lookupSignalPosShifted(x, y, totalShift);
  if (!useGranularSynth) {
    // use Sampler synthesis instrument
    ensureSamplerReady();
    float panning = map(x, 0, width, -0.8f, 0.8f);
    int len = calcSampleLen();
    samplelen = playSample(samplePos, len, samplerPointGain, defaultEnv, panning);
    int durationMS = (int)(samplelen / sampleRate * 1000);
    pointTimeLocs.add(new TimedLocation(x, y, durationMS + millis() + 50));
    return durationMS;
  }
  // use Granular synthesis instrument
  ensureGranularReady();
  float hopMsF = (int)Math.round(AudioUtility.samplesToMillis(hopSamples, sampleRate));
  final int grainCount = useLongBursts ?  2 * (int) Math.round(noteDuration/hopMsF) : (int) Math.round(noteDuration/hopMsF);
  println("-- granular point burst with grainCount = "+ grainCount);
  ArrayList<PVector> path = new ArrayList<>(grainCount);
  int[] timing = new int[grainCount];
  for (int i = 0; i < grainCount; i++) {
    if (useLongBursts) {
      // follow the signal path
      path.add(getCoordFromSignalPos(samplePos + hopSamples * i));
    } else {
      // cluster samples around the sample location
      path.add(this.jitterCoord(x, y, 3));
    }
    timing[i] = Math.round(i * hopMsF);
  }
  int startTime = millis() + 10;
  PACurveMaker curve = PACurveMaker.buildCurveMaker(path);
  curve.setDragTimes(timing);
  curve.setTimeStamp(startTime);
  GestureSchedule sched = curve.getAllPointsSchedule();
  float[] buf = (granSignal != null) ? granSignal : audioSignal;
  playGranularGesture(buf, sched, gParamsFixed, 1.0f);
  storeGranularCurveTL(sched, startTime, false);
  return curve.getTimeOffset();
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
  return mapper.lookupSignalPosShifted(x, y, totalShift);
}

/**
 * Calculates the display image coordinates corresponding to a specified audio sample index.
 * @param pos    an index into an audio signal, must be between 0 and width * height - 1.
 * @return       a PVector with the x and y coordinates
 */
public PVector getCoordFromSignalPos(int pos) {
  int[] xy = this.mapper.lookupImageCoordShifted(pos, totalShift);
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

/**
 * Bottleneck "commit" method for audio state.
 *
 * Takes an arbitrary input signal and installs it as the canonical audio signal
 * used by the application. This method:
 *
 *  - Resizes/pads/truncates the input to mapper.getSize()
 *  - Copies the data to ensure no external aliasing
 *  - Updates audioSignal (canonical signal handled by application code)
 *  - Updates playBuffer (audio buffer used by Minim audio library methods)
 *  - Propagates the buffer to active instruments: edit for your own instruments
 *
 * This is the ONLY method that should mutate the global audio signal state.
 *
 * In PixelAudio examples, the signal is typically loaded from a file, but
 * it could also be signal cached in memory, a signal generated by code, audio
 * captured live, etc.
 *
 * @param sig                 an audio signal
 * @param bufferSampleRate    audio sample rate for sig,
 *                            usually obtained when reading from an audio file
 */
void updateAudioChain(float[] sig, float bufferSampleRate) {
  // 0) Decide target length (make this a single source of truth)
  int targetSize = mapper.getSize();
  if (targetSize <= 0) return;
  // 1) Ensure playBuffer matches target
  float[] canonical = new float[targetSize];
  if (sig != null) {
    System.arraycopy(sig, 0, canonical, 0, Math.min(sig.length, targetSize));
  }
  // 3) Set audioSignal and other audio arrays
  audioSignal = canonical;
  granSignal = audioSignal;    // copy if you want an independent granular source
  audioLength = targetSize;
  // 4) Store signal in playBuffer
  if (playBuffer == null || playBuffer.getBufferSize() != targetSize) {
    playBuffer = new MultiChannelBuffer(targetSize, 1);
  }
  playBuffer.setChannel(0, canonical);
  // 5) Propagate into synths (adjust to your actual API)
  if (pool != null) pool.setBuffer(playBuffer, bufferSampleRate);
}

void updateAudioChain(float[] sig) {
  updateAudioChain(sig, audioOut.sampleRate());
}


/**
 * Calls PAGranularInstrumentDirector gDir to play a granular audio event.
 *
 * @param buf       an audio signal as an array of float
 * @param sched     an GestureSchedule with coordinate and timing information
 * @param params    a bundle of control parameters for granular synthesis
 */
public void playGranularGesture(float buf[], GestureSchedule sched, GestureGranularParams params, float pitchRatio) {
  // call mapper method lookupSignalPosArray to obtain an array of indices into buf, derived from points in sched
  int[] startIndices = mapper.lookupSignalPosArray(sched.points, totalShift, mapper.getSize());
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
  println("-- pitch jitter -- "+ pitch[0]);
  // if we aren't using a pitch array, we can call playGestureNow() with a different signature
  // gDir.playGestureNow(buf, sched, params, startIndices, panPerGrain);
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
