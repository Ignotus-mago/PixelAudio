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
  // PixelAudio instruments require a STEREO output. 1024 is a standard number of
  // samples for the output buffer to process at one time. You should usually set
  // the output sampleRate to either 44100 or 48000, standards for digital audio.
  this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
  // reduce the output level by 6.0 dB.
  audioOut.setGain(audioGain);
  // create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
  // playBuffer will not contain audio data until we load a file
  this.playBuffer = new MultiChannelBuffer(mapSize, 1);
  this.audioSignal = playBuffer.getChannel(0);
  this.audioLength = audioSignal.length;
  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
  defaultEnv = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  initADSRList();
  // create a PASamplerInstrument with 8 voices, adsrParams will be its default envelope
  ensureSamplerReady();
  // initialize mouse event tracking array
  timeLocsArray = new ArrayList<TimedLocation>();
}

/**
 * Prepares Sampler instruments and assets
 */
void ensureSamplerReady() {
  if (synth == null) {
    synth = new PASamplerInstrument(playBuffer, audioOut.sampleRate(), 8, audioOut, defaultEnv);
    println("-- initialized audio sampler synth");
    // set the synth gain with a linear value derived from a dB value
    synth.setGain(AudioUtility.dbToLinear(samplerGain));
  }
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
 * Typically called from mousePressed with mouseX and mouseY, generates audio events.
 *
 * @param x    x-coordinate within a PixelAudioMapper's width
 * @param y    y-coordinate within a PixelAudioMapper's height
 */
public void audioMouseClick(int x, int y) {
	ensureSamplerReady();
	int samplePos = getSamplePos(x,y);
	float panning = map(x, 0, width, -0.875f, 0.875f);
	if (isRandomADSR) {
		ADSRParams env = adsrList.get((int)random(adsrList.size()));
		int len = calcSampleLen();
		// don't output envelope information for automated events
		if (!isPlayMusicBox || isRaining) {
			print("-- envelope: "+ env.toString());
			println("; pos = "+ samplePos +", length = "+ len);
		}
		playSample(samplePos, len, 0.8f, env, panning);
	}
	else {
		playSample(samplePos, calcSampleLen(), 0.8f, panning);
	}
}

/**
 * @param x    a value to constrain to the current window width
 * @return the constrained value
 */
public int clipToWidth(int x) {
  return min(max(0, x), width - 1);
}

/**
 * @param y    a value to constrain to the current window height
 * @return the constrained value
 */
public int clipToHeight(int y) {
  return min(max(0, y), height - 1);
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
  return mapper.lookupSignalPosShifted(x, y, totalShift);
}

/**
 * Plays an audio sample with PASamplerInstrument and custom ADSR.
 *
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @param env          an ADSRParams envelope for the sample
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pan) {
  samplelen = synth.playSample(samplePos, (int) samplelen, amplitude, env, pitchScaling, pan);
  int durationMS = (int)(samplelen/sampleRate * 1000);
  int[] coords = mapper.lookupImageCoordShifted(samplePos, totalShift);
  timeLocsArray.add(new TimedLocation(coords[0], coords[1], durationMS + millis()));
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
  int[] coords = mapper.lookupImageCoordShifted(samplePos, totalShift);
  timeLocsArray.add(new TimedLocation(coords[0], coords[1], durationMS + millis()));
  // return the length of the sample
  return samplelen;
}

public int calcSampleLen() {
  float vary = 0;
  // skip the fairly rare negative numbers
  while (vary <= 0) {
    vary = (float) PixelAudio.gauss(1.0, 0.0625);
  }
  int samplelen = (int)(abs((vary * this.noteDuration) * sampleRate / 1000.0f));
  // println("---- calcSampleLen samplelen = "+ samplelen +" samples at "+ sampleRate +"Hz sample rate");
  return samplelen;
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
 *                            usually obtained when reading an audio file
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
  audioLength = targetSize;
  if (playBuffer == null || playBuffer.getBufferSize() != targetSize) {
    playBuffer = new MultiChannelBuffer(targetSize, 1);
  }
  // 4) Set playBuffer
  playBuffer.setChannel(0, canonical);
  // 5) Propagate into synths (adjust to your actual API)
  if (synth != null) synth.setBuffer(playBuffer, bufferSampleRate);
}

void updateAudioChain(float[] sig) {
  updateAudioChain(sig, audioOut.sampleRate());
}
