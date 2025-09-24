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
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  // set the gain lower to avoid clipping from multiple voices
  audioOut.setGain(-18.0f);
  println("---- audio out gain is "+ audioOut.getGain());
  // create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
  // the buffer will not have any audio data -- you'll need to open a file for that
  this.playBuffer = new MultiChannelBuffer(mapSize, 1);
  this.audioSignal = playBuffer.getChannel(0);
  this.audioLength = audioSignal.length;
  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
  adsr = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  // create a WFSamplerInstrument, though playBuffer is all 0s at the moment
  // adsrParams will be the default ADSR for the synth
  // allocate plenty of voices, the drawing interface generates a lot of audio events
  synth = new WFSamplerInstrument(playBuffer, audioOut.sampleRate(), 48, audioOut, adsr);
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
  // update audioSignal and playBuffer if audioSignal hasn't been initialized or if 
  // playBuffer needs to be refreshed after changes to its data source (isBufferStale == true).
  if (audioSignal == null || isBufferStale) {
    renderSignals();
    isBufferStale = false;
  }
  // use the default envelope
  playSample(samplePos, calcSampleLen(), 0.6f);
}
  
/**
 * Sets variables sampleX, sampleY and samplePos.
 */
public void setSampleVars(int x, int y) {
  sampleX = x;
  sampleY = y;
  samplePos = getSamplePos(sampleX, sampleY);
}

/**
 * Calculate position of the image pixel within the signal path,
 * taking the shifting of pixels and audioSignal into account.
 * See MusicBoxBuffer for use of a windowed buffer in this calculation. 
 */
public int getSamplePos(int x, int y) {
  int pos = mapper.lookupSample(x, y);
  // calculate how much animation has shifted the indices into the buffer
  totalShift = (totalShift + shift % mapSize + mapSize) % mapSize;
  return (pos + totalShift) % mapSize;
}

/**
 * Plays an audio sample with WFSamplerInstrument and custom ADSR.
 * 
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @param adsr         an ADSR envelope for the sample
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env) {
  samplelen = synth.playSample(samplePos, (int) samplelen, amplitude, env);
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
  samplelen = synth.playSample(samplePos, (int) samplelen, amplitude);
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
