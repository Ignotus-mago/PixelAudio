public void initAudio() {
  this.audioIn = minim.getLineIn(Minim.MONO);
  this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
  this.audioBuffer = new MultiChannelBuffer(mapSize, 1);
  this.audioSignal = new float[mapSize];
  this.rgbSignal = new int[mapSize];
  Arrays.fill(audioSignal, 0.0f);
  this.audioLength = audioSignal.length;
  this.audioBuffer.setChannel(0, audioSignal);
  streamCap = new StreamCapture();
  // path to the folder where PixelAudio examples keep their data files 
  // such as image, audio, .json, etc.
  String daPath = sketchPath("") + "../examples_data/";
  if (isVerbose) println("daPath: ", daPath);
  anthem = minim.loadFile(daPath + "youthorchestra.wav");
}  

public void rewindAudioPlayer(AudioPlayer player, boolean playAgain) {
  player.cue(0);
  if (playAgain) player.play();
}

public void showAudioStatus() {
  textSize(18);
  StringBuffer sb = new StringBuffer();
  sb.append("Listening is "+ listening +"; ");
  if (!listenLive) {
    if (anthem.isPlaying()) {
      sb.append("source is file, playing at "+ anthem.position() +"/"+ anthem.length() +"; ");
      sb.append("muting is "+ anthem.isMuted());
    }
    else {
      sb.append("source is file, paused at "+ anthem.position() +"/"+ anthem.length() +"; ");
      sb.append("muting is "+ anthem.isMuted());
    }
  }
  else {
    sb.append("source is current device "); 
  }
  text(sb.toString(), 6, 18);
}

public void toggleListening() {
  if (listening) {
    if (listenLive) {
      audioIn.removeListener(streamCap);
    }
    else {
      anthem.removeListener(streamCap);
    }
    listening = false;
  }
  else {
    if (listenLive) {
      audioIn.addListener(streamCap);
    }
    else {
      anthem.addListener(streamCap);
    }
    listening = true;
  }
}

public void toggleAudioSource() {
  listenLive = !listenLive;
  if (listening) {
    if (listenLive) listenToDevice();
    else listenToFile();
  }
}

public void listenToDevice() {
  listening = true;
  anthem.removeListener(streamCap);
  anthem.pause();
  audioIn.addListener(streamCap);
  listenLive = true;
}

public void listenToFile() {
  listening = true;
  audioIn.removeListener(streamCap);
  anthem.addListener(streamCap);
  anthem.loop();
  listenLive = false;
}

/**
 * Plays a sample from the streaming input source, which our StreamCapture object streamCap is
 * continually writing to audioBuffer, while streaming is on. 
 * 
 * Because audioBuffer is continually refreshed by streamCap, we need to create a new Sampler
   * every time we play a sample from the stream. (Okay, we could be more sophisticated, but this 
   * is sample code.) This is a somewhat expensive way to do things, but not a big stretch for
   * current computational resources. Depending on the situation -- for example, if we want to
   * capture a buffer of sound to use for instruments over a period of time -- we can improve
   * the efficiency of our code. However, if we just want to create real-time events on a stream, 
   * we do need to keep creating instruments each time an audio event is triggered. 
 * 
 * @param samplePos
 * @return
 */
/**
 * Plays an audio sample with PASamplerInstrument and custom ADSR.
 * TODO we're churning more memory than necessary, creating a MultiChannelBuffer every time.
 * If the audio signal has not changed, we don't have to reinitialize the PASamplerInstrument.
 * 
 * @param samplePos    position of the sample in the audio buffer
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos) {
  adsr = generateADSR();
  samplelen = calcSampleLen();
  float[] samples = new float[audioSignal.length];
  System.arraycopy(audioSignal, 0, samples, 0, samples.length);
  MultiChannelBuffer buf = new MultiChannelBuffer(samples.length, 1);
  buf.setChannel(0, samples);
  instrument = new PASamplerInstrument(buf, sampleRate, 1, audioOut, adsr);
  int sampleCount = instrument.playSample(samplePos, (int) samplelen, 0.9f, adsr);
  int durationMS = (int) (sampleCount * 1000.0f/sampleRate);
  if (isVerbose) println("---- ADSR "+ adsr.toString() +"; duration: "+ durationMS +" ms");
  // return the length of the sample
  return samplelen;
}

public int calcSampleLen() {
  float vary = 0; 
  // skip the fairly rare negative numbers
  while (vary <= 0) {
    vary = (float) PixelAudio.gauss(1.0, 0.0625);
  }
  samplelen = (int)(abs((vary * this.noteDuration) * sampleRate));
  // if (isVerbose) println("---- calcSampleLen samplelen = "+ samplelen +" samples at "+ sampleRate +"Hz sample rate");
  return samplelen;
}

ADSRParams generateADSR() {
  float amp = random(amplitudeRange[0], amplitudeRange[1]);
  float att = random(attackRange[0], attackRange[1]);
  float dec = random(decayRange[0], decayRange[1]);
  float sus = random(sustainRange[0], sustainRange[1]);
  float rel = random(releaseRange[0], releaseRange[1]);
  return new ADSRParams(amp, att, dec, sus, rel);
}
