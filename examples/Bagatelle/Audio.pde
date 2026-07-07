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
  // initialize the MInim library
  minim = new Minim(this);
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
  // set the gain lower to avoid clipping from multiple voices (UP and DOWN arrow keys adjust)
  audioOut.setGain(outputGain);
  println("---- audio out gain is "+ nf(audioOut.getGain(), 0, 2) +", sample rate is "+ sampleRate);
  // create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
  // the buffer will not have any audio data -- you'll need to open a file for that
  mapSize = mapper.getSize();
  this.playBuffer = new MultiChannelBuffer(mapSize, 1);
  audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
  granSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
  this.audioLength = audioSignal.length;
  // initialize event animation tracking arrays
  initTimedEventLists();
  ensureGranularReady();
  ensureSamplerReady();
}

/**
 * Initialize lists of TimedLocation objects, used for animated response to mouse clicks
 * on brushstrokes and outside brushstrokes.
 */
public void initTimedEventLists() {
  pointTimeLocs = new ArrayList<TimedLocation>();      // events outside a brush
  samplerBrushEvents = new ArrayList<SamplerBrushEvent>();    // events in a Sampler brush
  grainTimeLocs = new ArrayList<TimedLocation>();      // events in a Granular brush
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
  int pos = mapper.lookupSignalPosShifted(x, y, totalShift);
  // * BUG * do not add any shift !!!
  // calculate how much animation has shifted the indices into the buffer
  // totalShift = (totalShift + shift % mapSize + mapSize) % mapSize;
  // return (pos + totalShift) % mapSize;
  return pos;    // just return pos if we don't use pixel shift animation in this sketch
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

// -------------- METHODS TO PLAY SAMPLING OR GRANULAR SYNTH ------------- //

/**
 * Plays a sample and animates a point
 * @param x    x-coordinate of event
 * @param y    y-coordinate of event
 */
void runSamplerPointEvent(int x, int y) {
  ensureSamplerReady();
  int signalPos = mapper.lookupSignalPosShifted(x, y, totalShift);
  float panning = map(x, 0, width, -0.875f, 0.875f);
  int len = calcSampleLen(envDuration, 1.0f, 0.0625f);
  samplelen = playSample(signalPos, len, samplerPointGain, samplerEnv, panning);
  int durationMS = (int)(samplelen / sampleRate * 1000);
  pointTimeLocsAddPoint(new TimedLocation(x, y, durationMS + millis() + 50));
  // if (isVerbose) println("----- sampler point event, signalPos = "+ signalPos);
}

/**
 * Plays a sample and animates a point
 * @param x    x-coordinate of event
 * @param y    y-coordinate of event
 */
void runSamplerDrawEvent(int x, int y) {
  ensureSamplerReady();
  int signalPos = mapper.lookupSignalPosShifted(x, y, totalShift);
  float panning = map(x, 0, width, -0.875f, 0.875f);
  int len = calcSampleLen(envDuration, 1.0f, 0.0625f)/4;
  ADSRParams env = envPreset("Pluck");
  samplelen = playSample(signalPos, len, 0.5f, env, panning);
  int durationMS = (int)(samplelen / sampleRate * 1000);
  pointTimeLocsAddPoint(new TimedLocation(x, y, durationMS + millis() + 50));
  // if (isVerbose) println("----- sampler point event, signalPos = "+ signalPos);
}


/**
 * Plays a granular burst and animates a point
 * @param x    x-coordinate of event
 * @param y    y-coordinate of event
 */
void runGranularPointEvent(int x, int y) {
  ensureGranularReady();
  float hopMsF = (float)(AudioUtility.samplesToMillis(hopSamples, sampleRate));
  int dur = envDuration;
  final int grainCount = useLongBursts ?  8 * (int) Math.round(dur/hopMsF) : (int) Math.round(dur/hopMsF);
  // if (isVerbose) println("-- granular burst with grainCount = "+ grainCount +", hopMsF = "+ hopMsF);
  ArrayList<PVector> path = new ArrayList<>(grainCount);
  int[] timing = new int[grainCount];
  int signalPos = mapper.lookupSignalPosShifted(x, y, totalShift);
  for (int i = 0; i < grainCount; i++) {
    if (useLongBursts) {
      path.add(getCoordFromSignalPos(signalPos + hopSamples * i));
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
  float[] buf = (granSignal != null) ? granSignal : audioSignal;
  playGranularGesture(buf, sched, gParamsFixed);
  storeGranularCurveTL(sched, startTime, false);
  if (isDebugging) println("-- point event: "
    + " hopSamples = "+ hopSamples
    + ", hopMsF = "+ hopMsF
    + ", grainCount = "+ grainCount
    + ", duration = " + sched.durationMs()
    + ", eventTime = " + startTime
    + ", buffer index = "+ signalPos);
}


/**
 * Primary method for playing a granular synthesis audio event.
 *
 * @param buf       an audio signal as a array of float
 * @param sched     GestureSchedule (points + times) for grains
 * @param params    core parameters for granular synthesis
 */
public void playGranularGesture(float buf[], GestureSchedule sched, GestureGranularParams params) {
  GestureEventParams eventParams = prepareGranularGesture(buf, sched, params);
  playGranularGesture(buf, sched, params, eventParams);
}

/**
 * Primary method for playing a granular synthesis audio event.
 *
 * @param buf            an audio signal as a array of float
 * @param sched          GestureSchedule (points + times) for grains
 * @param params         core parameters for granular synthesis
 * @param eventParams    event parameters for granular synthesis
 */
public void playGranularGesture(float buf[], GestureSchedule sched, GestureGranularParams params, GestureEventParams eventParams) {
  gDir.playGestureNow(buf, sched, params, eventParams);
}

public GestureEventParams prepareGranularGesture(float buf[], GestureSchedule sched, GestureGranularParams params) {
  return prepareGranularGesture(buf, sched, params, null);
}

public GestureEventParams prepareGranularGesture(float buf[], GestureSchedule sched,
  GestureGranularParams params, PAControlCurve gainCurve) {
  GestureEventParams eventParams;
  // map gesture points to array of indices into the audio buffer
  int[] startIndices = mapper.lookupSignalPosArray(sched.points, totalShift, mapper.getSize());
  float[] panPerGrain = new float[sched.size()];
  for (int i = 0; i < sched.size(); i++) {
    PVector p = sched.points.get(i);
    // use the x-coordinate for left to right stereo field mapping
    panPerGrain[i] = map(p.x, 0, width-1, -0.875f, 0.875f);
  }
  // optional per-grain dynamics
  float[] gainPerGrain = null;
  if (gainCurve != null) {
    gainPerGrain = PAKeyframeControlCurve.expandToSchedule(gainCurve, sched);
  }
  GestureEventParams.Builder builder = GestureEventParams.builder(sched.size())
    .startIndices(startIndices)
    .pan(panPerGrain);
  if (gainPerGrain != null) {
    builder.gain(gainPerGrain);
  }
  if (usePitchedGrains) {
    float[] pitch = generateJitterPitch(sched.size(), params.pitchRatio, pitchJitter);
    builder.pitchRatio(pitch);
  }
  eventParams = builder.build();
  return eventParams;
}

float[] generateJitterPitch(int length, float basePitch, float deviationPitch) {
  float[] pitch = new float[length];
  double variance = deviationPitch * deviationPitch;
  for (int i = 0; i < pitch.length; i++) {
    pitch[i] = basePitch * (float) PixelAudio.gauss(1, variance);
  }
  return pitch;
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
 * @param linear     desired gain as a linear ratio, currently ignored
 * @param totalMs    desired duration of the envelope in milliseconds
 * @return an ADSRParams envelope
 */
public ADSRParams calculateEnvelopeLinear(float linear, float totalMs) {
  float attackMS = Math.min(50, totalMs * 0.1f);
  float releaseMS = Math.min(200, totalMs * 0.3f);
  float envGain = 1.0f;    // or = linear;
  ADSRParams env = new ADSRParams(envGain, attackMS / 1000f, 0.01f, 0.8f, releaseMS / 1000f);
  return env;
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
	 * @param pan          stereo pan [-1.0, 1.0] for sample
	 * @return the calculated sample length in samples
	 */
public int playSample(int samplePos, int samplelen, float amplitude, float pan) {
  return playSample(samplePos, samplelen, amplitude, samplerEnv, 1.0f, pan);
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
 * @param pitch        pitch scaling as deviation from default (1.0), where 0.5 = octave lower, 2.0 = oactave higher
 * @param pan          position of sound in the stereo audio field (-1.0 = left, 0.0 = center, 1.0 = right)
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pitch, float pan) {
  int beforeCount = 0;
  int afterCount = 0;
  int voiceCount = 0;
  // tracking Sampler voice count for possible problems, which we discovered and fixed
  if (isTrackSamplerVoices) {
    voiceCount = gDir.activeOrReleasingVoiceCount();
    println("-- gDir voice count = "+ voiceCount);
    // println("-- playSample: gain = "+ amplitude +", length = "+ samplelen +", time = "+ millis());
    beforeCount = pool.samplerActiveVoiceCount();
    // println("-- sampler voices BEFORE: "+ beforeCount);
  }
  int sampleLen = pool.playSample(samplePos, samplelen, amplitude, env, pitch, pan);
  if (isTrackSamplerVoices) {
    afterCount = pool.samplerActiveVoiceCount();
    if (afterCount - beforeCount > 1) println("-- sampler voice jump BEFORE "+ beforeCount +" AFTER "+ afterCount);
    if (afterCount > 256) println("-- high sampler voice count "+ afterCount);
    // println("-- sampler voices AFTER: "+ afterCount);
  }
  return sampleLen;
}

	/**
	 * @param dur         sample duration in milliseconds
	 * @param mean        multiplier for duration, 1.0 leaves duration as mean value
	 * @param variance    variance from mean value
	 * @return a length in samples with some Gaussian variation
	 */
public int calcSampleLen(int dur, float mean, float variance) {
  float vary = 0;
  // skip the fairly rare negative numbers
  while (vary <= 0) {
    vary = (float) PixelAudio.gauss(mean, variance);
  }
  samplelen = (int)(abs((vary * dur) * sampleRate / 1000.0f));
  // if (isVerbose) println("---- calcSampleLen samplelen = "+ samplelen +" samples at "+ sampleRate +"Hz sample rate");
  return samplelen;
}

	/**
	 * Convenience method for calcSampleLen(envDuration, 1.0f, 0.0625f).
	 * @return calculated sample length in samples of an envelope
	 */
public int calcSampleLen() {
  return calcSampleLen(envDuration, 1.0f, 0.0625f);
}

	/**
	 * @param sched         a {@code GestureSchedule} to access for calculating an envelope duration
	 * @param envName       name of an envelope preset
	 * @param fallbackMs    default duration in milliseconds
	 * @return calculated sample length in samples of an envelope
	 */
int computeEnvDurationMs(GestureSchedule sched, String envName, int fallbackMs) {
  int n = sched.points.size();
  if (n < 2) return fallbackMs;
  float avgStepMs = sched.durationMs() / (float)(n - 1);
  float factor;
  switch (envName) {
  case "Pluck":
  case "Percussion":
    factor = 4.0f;
    break;
  case "Soft":
  case "Fade":
    factor = 3.0f;
    break;
  case "Swell":
  case "Pad":
    factor = 2.0f;
    break;
  default:
    factor = 3.0f;
  }
  int minEnvMs = envMinDurationMs;
  int maxEnvMs = envMaxDurationMs;
  return PApplet.constrain(Math.round(avgStepMs * factor), minEnvMs, maxEnvMs);
}

/**
 * Prepares Sampler instruments and assets
 */
void ensureSamplerReady() {
  if (pool == null) {
    pool = new PASamplerInstrumentPool(playBuffer, sampleRate, poolSize, sMaxVoices, audioOut, samplerEnv);
    pool.setWrapAround(true);
    println("-- initilialized pool sampler synth");
    pool.setGain(samplerGain);
  }
}

/**
 * Prepares Granular instruments and assets
 */
void ensureGranularReady() {
  if (gSynth == null) {
    ADSRParams granEnv = new ADSRParams(1.0f, 0.005f, 0.02f, 0.875f, 0.025f);
    gSynth = buildGranSynth(audioOut, granEnv, gMaxVoices);
  }
  if (granSignal == null) {
    granSignal = (audioSignal != null) ? audioSignal : playBuffer.getChannel(0);
  }
  if (gParamsFixed == null) {
    initGranularParams();
  }
  if (gDir == null) {
    gDir = new PAGranularInstrumentDirector(gSynth);
  }
}

/**
 * Initializes a PAGranularInstrument.
 * @param out          AudioOutput for this application
 * @param env          an ADSRParams envelope
 * @param numVoices    number of voices for the synth
 * @return a PAGranularInstrument
 */
public PAGranularInstrument buildGranSynth(AudioOutput out, ADSRParams env, int numVoices) {
  PAGranularInstrument inst = new PAGranularInstrument(out, env, numVoices);
  return inst;
}

/**
 * Initializes gParamsFixed, a GestureGranularParams instances used for granular point events.
 */
public void initGranularParams() {
  ADSRParams env = this.calculateEnvelopeLinear(granularGain, 1000);
  gParamsFixed = GestureGranularParams.builder()
    .grainLengthSamples(granSamples)
    .hopLengthSamples(hopSamples)
    .gainLinear(granularPointGain)
    .looping(false)
    .env(env)
    .hopMode(GestureGranularParams.HopMode.FIXED)
    .burstGrains(burstGrains)
    .build();
  gParamsDraw = GestureGranularParams.builder()
    .grainLengthSamples(512)
    .hopLengthSamples(128)
    .gainLinear(0.02f)
    .looping(false)
    .env(granularEnv)
    .hopMode(GestureGranularParams.HopMode.FIXED)
    .burstGrains(4)
    .build();
}


/**
 * Bottleneck "commit" method for audio state.
 *
 * Takes an arbitrary input signal and installs it as the canonical audio signal
 * used by the system. This method:
 *
 *  - Resizes/pads/truncates the input to mapper.getSize()
 *  - Copies the data to ensure no external aliasing
 *  - Updates audioSignal (canonical signal handled by application code)
 *  - Updates playBuffer (audio buffer used by Minim audio library methods)
 *  - Propagates the buffer to active instruments: edit this part for your own code
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
  audioSignal = canonical;
  granSignal = audioSignal;
  audioLength = targetSize;
  if (playBuffer == null || playBuffer.getBufferSize() != targetSize) {
    playBuffer = new MultiChannelBuffer(targetSize, 1);
    // println("**** playBuffer initialized, length = "+ playBuffer.getBufferSize());
  }
  playBuffer.setChannel(0, canonical);
  // Propagate into synths (adjust to your actual API)
  if (pool != null) {
    pool.setBuffer(playBuffer, bufferSampleRate);
    // println("**** playBuffer updated, length = "+ playBuffer.getBufferSize());
  }
}

void updateAudioChain(float[] sig) {
  updateAudioChain(sig, audioOut.sampleRate());
}


// ------------- LOOPING ------------- //

	/**
	 * Updates looping instruments.
	 */
public synchronized void updateInstrumentLoops() {
  if (activeLoops.isEmpty()) return;
  long now = millis();
  for (InstrumentLoop loop : activeLoops) {
    if (!loop.active) continue;
    if (now >= loop.nextStartMillis) {
      loop.playAction.run();
      if (loop.repeatsRemaining > 0) {
        loop.repeatsRemaining--;
        if (loop.repeatsRemaining == 0) {
          loop.active = false;
          continue;
        }
      }
      loop.nextStartMillis = now + loop.durationMs + loop.gapMs;
    }
  }
  activeLoops.removeIf(loop -> !loop.active);
}

	/**
	 * Stops all loops.
	 */
public synchronized void stopAllLoops() {
  for (InstrumentLoop loop : activeLoops) {
    loop.stop();
  }
  activeLoops.clear();
}

	/**
	 * Stop looping for a specified brush
	 * @param brush    AudioBrush whose looping will end
	 */
public synchronized void stopLoopsForBrush(AudioBrush brush) {
  if (brush == null || activeLoops.isEmpty()) return;
  for (InstrumentLoop loop : activeLoops) {
    if (loop.brush == brush) {
      loop.stop();
    }
  }
  activeLoops.removeIf(loop -> !loop.active);
}

public synchronized boolean hasLoopForBrush(AudioBrush brush) {
  if (brush == null) return false;
  for (InstrumentLoop loop : activeLoops) {
    if (loop.active && loop.brush == brush) return true;
  }
  return false;
}

	/**
	 * Estimate loop duration for a Granular instrument.
	 * @param sched     a GestureSchedule associated with a brush
	 * @param params    a GestureGranularParams object
	 * @return expected duration of a loop
	 */
public int estimateLoopDurationMs(GestureSchedule sched, GestureGranularParams params) {
  if (sched == null || sched.size() == 0) return 1;
  int schedMs = Math.max(1, Math.round(sched.durationMs()));
  int grainTailMs = 0;
  if (params != null) {
    grainTailMs = (int) Math.max(0, Math.round(AudioUtility.samplesToMillis(params.grainLengthSamples, sampleRate)));
  }
  return schedMs + grainTailMs;
}

	/**
	 * Estimate loop duration for a Sampler instrument.
	 * @param sched             a GestureSchedule associated with a brush
	 * @param env               an ADSRParams envelope
	 * @param noteLenSamples    number of samples in audio event ("note")
	 * @return expected duration of a loop
	 */
public int estimateLoopDurationMs(GestureSchedule sched, ADSRParams env, int noteLenSamples) {
  if (sched == null || sched.size() == 0) return 1;
  int schedMs = Math.max(1, Math.round(sched.durationMs()));
  int tailMs = 0;
  if (noteLenSamples > 0) {
    tailMs = (int) Math.max(tailMs, Math.round(AudioUtility.samplesToMillis(noteLenSamples, sampleRate)));
  }
  if (env != null) {
    int envMs = Math.round((env.getAttack() + env.getDecay() + env.getRelease()) * 1000f);
    tailMs = Math.max(tailMs, envMs);
  }
  return schedMs + tailMs;
}

	/**
	 * @param gb            a GranularBrush to loop
	 * @param buf           buffer to play from
	 * @param sched         a GestureSchedule
	 * @param params        runtime parameters for gesture playback
	 * @param eventParams   optional pan, gain, and pitch modifiers per grain
	 * @param repeats       number of times to loop
	 * @param gapMs         time between loop events
	 * @param animate       use TimedLocation animation (not used in method, true by default, TODO clarify)
	 * @return              an InstrumentLoop object
	 */
public synchronized InstrumentLoop startGranularLoop(GranularBrush gb,
  float[] buf,
  GestureSchedule sched,
  GestureGranularParams params,
  GestureEventParams eventParams,
  int repeats,
  int gapMs,
  boolean animate) {
  if (gb == null || buf == null || sched == null || params == null || eventParams == null) return null;
  int durationMs = estimateLoopDurationMs(sched, params);
  long startMs = millis();

  Runnable playAction = () -> {
    playGranularGesture(buf, sched, params, eventParams);
    if (animate) {
      storeGranularCurveTL(sched, millis() + 10, false);
    }
  };

  Runnable stopAction = () -> {
    if (grainTimeLocs != null) {
      for (TimedLocation tl : grainTimeLocs) {
        tl.setStale(true);
      }
      grainTimeLocs.removeIf(TimedLocation::isStale);
    }
    if (gDir != null) {
      gDir.cancelAndReleaseAll();
    }
  };

  InstrumentLoop loop = new InstrumentLoop(gb, playAction, stopAction,
    durationMs, gapMs, repeats, startMs);
  activeLoops.add(loop);
  return loop;
}

public InstrumentLoop loopGranularBrush(GranularBrush gb, int repeats, int gapMs) {
    if (gb == null) return null;
    ensureGranularReady();
    GestureGranularConfig.Builder cfg = gb.cfg().copy();
    CueResult result = applyPerformancePresets(cfg, gb.curve());
    PACurveMaker curve = result.curve;
    GestureGranularConfig snap = result.cfg.build();
    GestureSchedule sched = scheduleBuilder.build(curve, snap, audioOut.sampleRate());
    if (sched == null || sched.size() == 0) return null;
    GestureGranularParams params = snap.toParams();
    float[] buf = (granSignal != null) ? granSignal : audioSignal;
    GestureEventParams eventParams = prepareGranularGesture(buf, sched, params);
    return startGranularLoop(gb, buf, sched, params, eventParams, repeats, gapMs, true);
}

	/**
	 * @param sb               a SamplerBrush
	 * @param sched            GestureSchedule for brush events
	 * @param env              ADSRParams envelope for each Sampler event in schedule
	 * @param noteLenSamples   length of a note in samples
	 * @param repeats          number of times to repeat loop
	 * @param gapMs            time between loop repetitions, ms
	 * @param animate          use TimedLocation animation (not used in method, true by default, TODO clarify)
	 * @return                 an InstrumentLoop object
	 */
public synchronized InstrumentLoop startSamplerLoop(SamplerBrush sb, GestureSchedule sched,
  ADSRParams env, int noteLenSamples, int repeats, int gapMs, boolean animate) {
  if (sb == null || sched == null || sched.size() == 0) return null;
  int durationMs = estimateLoopDurationMs(sched, env, noteLenSamples);
  durationMs = (int)(durationMs * 0.8f);
  long startMs = millis();
  PAControlCurve gainCurve = isAddDynamics ? dynamics : null;

  Runnable playAction = () -> {
    storeSamplerBrushEvents(sched, sb.snapshot(), millis() + 10, gainCurve);
  };

  Runnable stopAction = () -> {
    synchronized (samplerBrushEventsLock) {
      if (samplerBrushEvents != null) {
        samplerBrushEvents.clear();
      }
    }
    synchronized (pointTimeLocsLock) {
      if (pointTimeLocs != null) {
        for (TimedLocation tl : pointTimeLocs) {
          tl.setStale(true);
        }
        pointTimeLocs.removeIf(TimedLocation::isStale);
      }
    }
    if (pool != null) {
      pool.fadeOutAll();
    }
  };

  InstrumentLoop loop = new InstrumentLoop(sb, playAction, stopAction, durationMs, gapMs, repeats, startMs);
  activeLoops.add(loop);
  return loop;
}

	/**
	 * @param sb        a SamplerBrush instance
	 * @param repeats   number of times to repeat
	 * @param gapMs     time between loop repetitions, ms
	 * @return
	 */
public InstrumentLoop loopSamplerBrush(SamplerBrush sb, int repeats, int gapMs) {
  if (sb == null) return null;
  ensureSamplerReady();

  GestureGranularConfig snap = sb.snapshot();
  GestureSchedule sched = scheduleBuilder.build(sb.curve(), snap, audioOut.sampleRate());
  if (sched == null || sched.size() == 0) return null;

  ADSRParams env = (snap.env != null) ? snap.env : samplerEnv;
  int noteLenSamples = calcSampleLen();

  return startSamplerLoop(sb, sched, env, noteLenSamples, repeats, gapMs, true);
}
