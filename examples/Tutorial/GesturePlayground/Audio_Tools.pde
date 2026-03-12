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
  this.playBuffer = new MultiChannelBuffer(mapSize, 1);
  audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
  granSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
  this.audioLength = audioSignal.length;
  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
  samplerEnv = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  granularEnv = ADSRUtils.fitEnvelopeToDuration(samplerEnv, grainDuration);
  // initialize event animation tracking arrays
  initTimedEventLists();
}

/**
 * Initialize lists of TimedLocation objects, used for animated response to mouse clicks
 * on brushstrokes and outside brushstrokes.
 */
public void initTimedEventLists() {
  pointTimeLocs = new ArrayList<TimedLocation>();      // events outside a brush
  samplerTimeLocs = new ArrayList<TimedLocation>();    // events in a Sampler brush
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
  return pos;
}

/**
 * @param pos    an index into the audio signal
 * @return a PVector representing the image pixel mapped to pos
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
  float panning = map(x, 0, width, -0.8f, 0.8f);
  int len = calcSampleLen(noteDuration, 1.0f, 0.0625f);
  samplelen = playSample(signalPos, len, samplerPointGain, samplerEnv, panning);
  int durationMS = (int)(samplelen / sampleRate * 1000);
  pointTimeLocs.add(new TimedLocation(x, y, durationMS + millis() + 50));
  if (isVerbose) println("----- sampler point event, signalPos = "+ signalPos);
}

/**
 * Plays a granular burst and animates a point
 * @param x    x-coordinate of event
 * @param y    y-coordinate of event
 */
void runGranularPointEvent(int x, int y) {
  ensureGranularReady();
  float hopMsF = Math.round(AudioUtility.samplesToMillis(hopSamples, sampleRate));
  final int grainCount = useLongBursts ?  8 * (int) Math.round(noteDuration/hopMsF) : (int) Math.round(noteDuration/hopMsF);
  if (isVerbose) println("-- granular point burst with grainCount = "+ grainCount);
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
  if (isVerbose) println("----- granular point event, signalPos = " + signalPos);
  return;
}


/**
 * Primary method for playing a granular synthesis audio event.
 *
 * @param buf       an audio signal as a array of float
 * @param sched     GestureSchedule (points + times) for grains
 * @param params    core parameters for granular synthesis
 */
public void playGranularGesture(float buf[], GestureSchedule sched, GestureGranularParams params) {
  // get the position of each grain we're going to play as an array of indices into the audio buffer
  int[] startIndices = mapper.lookupSignalPosArray(sched.points, totalShift, mapper.getSize());
  //println("---> startIndices[0] = "+ startIndices[0] +" for "+ sched.points.get(0).x, sched.points.get(0).y, totalShift, mapper.getSize());
  // calculate the panning for each grain and save it to an array of float with range (-1.0, 1.0)
  float[] panPerGrain = new float[sched.size()];
  for (int i = 0; i < sched.size(); i++) {
    PVector p = sched.points.get(i);
    // use the x-coordinate for left to right stereo field mapping
    panPerGrain[i] = map(p.x, 0, width-1, -0.875f, 0.875f);
  }
  println("\n----->>> playGranularGesture()");
  //debugIndexHeadroom(buf, startIndices, params);
  //debugTimesMs(sched);
  //println("\n");
  // we can can set start pitch and gain for each grain, but we need to create a GestureEventParams
  // object to do that. We don't both with a gain array here.
  if (usePitchedGrains) {
    float[] pitch = generateJitterPitch(sched.size(), 0.0167f);
    GestureEventParams eventParams = GestureEventParams.builder(sched.size())
      .startIndices(startIndices)
      .pan(panPerGrain)
      .pitchRatio(pitch)
      .build();
    // this is the playGestureNow() command with maximum control over individual grains
    gDir.playGestureNow(buf, sched, params, eventParams);
    println("-- pitch jitter -- "+ pitch[0]);
    return;
  }
  // there's a version of playGestureNow that accepts a array of buffer indices and and array panning values
  gDir.playGestureNow(buf, sched, params, startIndices, panPerGrain);
}

// debugging -- TODO drop in release
static void debugIndexHeadroom(float[] buf, int[] startIndices, GestureGranularParams ggp) {
  int bufLen = buf.length;
  int grainLen = Math.max(1, ggp.grainLengthSamples);
  int hop = Math.max(1, ggp.hopLengthSamples);
  int burst = Math.max(1, ggp.burstGrains);
  float pitch = (ggp.pitchRatio > 0f) ? ggp.pitchRatio : 1.0f;

  int indexHop = hop; // your current semantics
  int need = (int)Math.ceil((grainLen - 1) * pitch) + (burst - 1) * indexHop;

  int maxStart = bufLen - 2 - need;
  if (maxStart < 0) maxStart = 0;

  int over = 0;
  int maxIdx = Integer.MIN_VALUE;
  int minIdx = Integer.MAX_VALUE;

  for (int idx : startIndices) {
    if (idx > maxStart) over++;
    if (idx > maxIdx) maxIdx = idx;
    if (idx < minIdx) minIdx = idx;
  }

  System.out.println("-- bufLen=" + bufLen
    + " grainLen=" + grainLen
    + " burst=" + burst
    + " hop=" + hop
    + " pitch=" + pitch
    + " need=" + need
    + " maxStart=" + maxStart);
  System.out.println("-- startIndices: min=" + minIdx + " max=" + maxIdx
    + " overMaxStart=" + over + "/" + startIndices.length);
}

// debugging -- TODO drop in release
static void debugTimesMs(GestureSchedule s) {
  int n = s.size();
  if (n <= 1 || s.timesMs == null) return;

  float[] t = s.timesMs;
  float t0 = t[0];
  float tLast = t[n - 1];

  float minDt = Float.POSITIVE_INFINITY;
  float maxDt = Float.NEGATIVE_INFINITY;
  int nonInc = 0;
  int tiny = 0;

  for (int i = 1; i < n; i++) {
    float dt = t[i] - t[i - 1];
    if (dt <= 0f) nonInc++;
    if (dt >= 0f && dt < 0.1f) tiny++; // <0.1ms buckets into same ~4 samples at 44.1k
    if (dt < minDt) minDt = dt;
    if (dt > maxDt) maxDt = dt;
  }

  System.out.println("-- sched n=" + n
    + " spanMs=" + (tLast - t0)
    + " t0=" + t0 + " tLast=" + tLast);
  System.out.println("-- dtMs min=" + minDt
    + " max=" + maxDt
    + " nonInc=" + nonInc
    + " tiny(<0.1ms)=" + tiny);
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
 * @return
 */
public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pitch, float pan) {
  return pool.playSample(samplePos, samplelen, amplitude, env, pitch, pan);
}

/**
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

public int calcSampleLen() {
  return calcSampleLen(noteDuration, 1.0f, 0.0625f);
}

/**
 * Prepares Sampler instruments and assets
 */
void ensureSamplerReady() {
  if (playBuffer == null) return;
  if (pool != null) pool.setBuffer(playBuffer);
  else pool = new PASamplerInstrumentPool(playBuffer, sampleRate, 1, sMaxVoices, audioOut, samplerEnv);
  pool.setGain(samplerGain);
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
}


// ---- NEW METHOD FROM TutorialOneDrawing ---- //

/**
 * Updates the various audio buffers when we load a new signal, typically from a file.
 * @param sig    an audio signal
 */
void updateAudioChain(float[] sig) {
  // 0) Decide target length (make this a single source of truth)
  int targetSize = mapper.getSize();          // or mapSize, but pick one canonical TODO
  if (targetSize <= 0) return;
  // 1) Ensure playBuffer matches target
  if (playBuffer.getBufferSize() != targetSize) {
    playBuffer.setBufferSize(targetSize);
  }
  // 2) Copy sig into a temp array of exactly targetSize (pad/truncate deterministically)
  float[] tmp = new float[targetSize];
  if (sig != null) {
    System.arraycopy(sig, 0, tmp, 0, Math.min(sig.length, targetSize));
  }
  // 3) Store into playBuffer
  playBuffer.setChannel(0, tmp);
  // 4) Snapshot arrays used elsewhere
  audioSignal = tmp;                 // already correct size
  granSignal = audioSignal;          // alias intentionally (or copy if you want independent)
  audioLength = targetSize;
  // 5) Propagate into synths (examples — adjust to your actual API)
  pool.setBuffer(playBuffer);
  // granularDirector doesn't track an audio buffer with a field
}
