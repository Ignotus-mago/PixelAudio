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
  // initialize the MInim library
  minim = new Minim(this);
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
  // set the gain lower to avoid clipping from multiple voices (UP and DOWN arrow keys adjust)
  audioOut.setGain(outputGain); 
  println("---- audio out gain is "+ nf(audioOut.getGain(), 0, 2));
  // create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
  // the buffer will not have any audio data -- you'll need to open a file for that
  this.playBuffer = new MultiChannelBuffer(mapSize, 1);
  this.audioSignal = playBuffer.getChannel(0);
  this.granSignal = audioSignal;
  this.audioLength = audioSignal.length;
  // initialize mouse event tracking array
  pointTimeLocs = new ArrayList<TimedLocation>();
  initGranularParams();
}

/**
 * Initializes global variables gParamsGesture and gParamsFixed, which provide basic
 * settings for granular synthesis the follows gesture timing or fixed hop timing 
 * between grains. 
 */
public void initGranularParams() {
  ADSRParams env = this.calculateEnvelope(granularGain, 1000);
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
 * Handles mouse clicks that happen outside a brushstroke.
 * 
 * @param x    x-coordinate of mouse click
 * @param y    y-coordinate of mouse click
 */
public void audioMousePressed(int x, int y) {
  if (!useGranularSynth) {
    // use Sampler synthesis instrument
    ensureSamplerReady();
    int signalPos = mapper.lookupSignalPosShifted(x, y, totalShift);
    float panning = map(x, 0, width, -0.8f, 0.8f);
    int len = calcSampleLen();
    samplelen = playSample(signalPos, len, synthPointGain, defaultEnv, panning);
    int durationMS = (int)(samplelen / sampleRate * 1000);
    pointTimeLocs.add(new TimedLocation(x, y, durationMS + millis() + 50));
    return;
  }
  // use Granular synthesis instrument
  ensureGranularReady();
  float hopMsF = (int)Math.round(AudioUtility.samplesToMillis(hopSamples, sampleRate));
  final int grainCount = useLongBursts ?  8 * (int) Math.round(noteDuration/hopMsF) : (int) Math.round(noteDuration/hopMsF);
  println("-- granular point burst with grainCount = "+ grainCount);
  ArrayList<PVector> path = new ArrayList<>(grainCount);
  int[] timing = new int[grainCount];
  int sigIndex = mapper.lookupSignalPosShifted(x, y, totalShift);
  for (int i = 0; i < grainCount; i++) {
    if (useLongBursts) {
      path.add(getCoordFromSignalPos(sigIndex + hopSamples * i));
    }
    else {
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
  int pos =  mapper.lookupSignalPosShifted(x, y, totalShift);
  return pos;
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
 * @param x    x coordinate of circle
 * @param y    y coordinate of circle
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
 * Ensures that all resources and variable necessary for the Sampler synth are ready to go.
 */
void ensureSamplerReady() {
  if (pool != null) pool.setBuffer(playBuffer);
  else pool = new PASamplerInstrumentPool(playBuffer, sampleRate, 1, samplerMaxVoices, audioOut, defaultEnv);
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
  }
  if (granSignal == null) {
    granSignal = (audioSignal != null) ? audioSignal : playBuffer.getChannel(0);
  }
  if (gDir == null) {
    gDir = new PAGranularInstrumentDirector(gSynth);
  }
}

/**
 * Updates resources such as playBuffer and pool with a new signal, typcically when a new file is loaded.
 * 
 * @param sig    an audio signal as an array of float
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

/**
 * Calls PAGranularInstrumentDirector gDir to play a granular audio event.
 * 
 * @param buf       an audio signal as an array of flaot
 * @param sched     an GestureSchedule with coordinate and timing information 
 * @param params    a bundle of control parameters for granular synthesis
 */
public void playGranularGesture(float buf[], GestureSchedule sched, GestureGranularParams params) {
  // call mapper method lookupSignalPosArray to obtain an array of indices into buf, derived from points in sched
  int[] startIndices = mapper.lookupSignalPosArray(sched.points, totalShift, mapper.getSize());
  // println("---> startIndices[0] = "+ startIndices[0] +" for "+ sched.points.get(0).x, sched.points.get(0).y, totalShift, mapper.getSize());
  // calculate the pan for each grain, based on its x-coordinate
  float[] panPerGrain = new float[sched.size()];
  for (int i = 0; i < sched.size(); i++) {
    PVector p = sched.points.get(i);
    // example: map x to [-0.8, +0.8]
    panPerGrain[i] = map(p.x, 0, width-1, -0.875f, 0.875f);
  }
  // debugging
  //println("\n----->>> playGranularGesture()");
  //debugIndexHeadroom(buf, startIndices, tx);
  //debugTimesMs(sched);
  //println("\n");\// end debugging
  // if usePitchedGrains is true, apply a jittery pitch shift to 
  // each grain, then call gDir.playGestureNow(), and return
  if (usePitchedGrains) {
    float[] pitch = generateJitterPitch(sched.size(), 0.25f);
    GestureEventParams eventParams = GestureEventParams.builder(sched.size())
      .startIndices(startIndices)
      .pan(panPerGrain)
      .pitchRatio(pitch)
      .build();
    gDir.playGestureNow(buf, sched, params, eventParams);
    println("-- pitch jitter -- "+ pitch[0]);
    return;
  }
  gDir.playGestureNow(buf, sched, params, startIndices, panPerGrain);
}

/**
 * Calculate an envelope of length totalSamples. 
 * @param gainDb          desired gain in dB, currently ignored
 * @param totalSamples    number of samples the envelope should cover
 * @param sampleRate      sample rate of the audio buffer the envelope is applied to
 * @return and ADSRParams envelope
 */
public ADSRParams calculateEnvelope(float gainDb, int totalSamples, float sampleRate) {
  return calculateEnvelope(gainDb, totalSamples * 1000f / sampleRate);
}

/**
 * Calculate an envelope of length totalSamples. 
 * @param gainDb     desired gain in dB, currently ignored
 * @param totalMs    desired duration of the envelope in milliseconds
 * @return an ADSRParams envelope
 */
public ADSRParams calculateEnvelope(float gainDb, float totalMs) {
  float attackMS = Math.min(50, totalMs * 0.1f);
  float releaseMS = Math.min(200, totalMs * 0.3f);
  float envGain = AudioUtility.dbToLinear(gainDb);
  envGain = 1.0f;
  return new ADSRParams(envGain, attackMS / 1000f, 0.01f, 0.8f, releaseMS / 1000f);
}

// DEBUGGIONG
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

// DEBUGGIONG
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


/*        END AUDIO METHODS                        */
