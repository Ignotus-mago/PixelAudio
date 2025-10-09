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
  setAudioGain(defaultGain);
  println("---- audio out gain is "+ audioOut.getGain());
  // create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
  this.playBuffer = new MultiChannelBuffer(mapSize, 1);
  this.anthemBuffer = new MultiChannelBuffer(mapSize, 1);
  this.audioSignal = playBuffer.getChannel(0);
  this.audioLength = audioSignal.length;
  this.anthemSignal = new float[audioLength];
  System.arraycopy(audioSignal, 0, anthemSignal, 0, audioLength);
  windowBuff = new WindowedBuffer(anthemSignal, mapSize, 1024);
  // initialize mouse event tracking array
  timeLocsArray = new ArrayList<TimedLocation>();
  // TODO initialize animation buffer    
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
  this.sampleX = x;
  this.sampleY = y;
  samplePos = getSamplePos(x, y);
  // update audioSignal and playBuffer if audioSignal hasn't been initialized or if 
  // playBuffer needs to be refreshed after changes to its data source (isBufferStale == true).
  if (audioSignal == null || isBufferStale) {
    renderSignals();
    isBufferStale = false;
  }
  this.samplelen = calcSampleLen(this.sampleBase, this.sampleScale, this.sampleScale * 0.125f);
  // println("---- playBuffer index is "+ samplePos);
  playSample(samplePos, samplelen, 0.5f, adsr);
}

public int getSamplePos(int x, int y) {
  samplePos = mapper.lookupSample(x, y);
  // calculate how much animation has shifted the indices into the buffer
  animShift = (animShift + shift % mapSize + mapSize) % mapSize;
  samplePos = (samplePos + animShift) % mapSize;
  samplePos += this.windowBuff.getIndex();
  return samplePos % this.windowBuff.getBufferSize();
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
  if (pool == null) {
   println("-->>> You must load a file ('o' key command) before you can play audio samples. <<<--");
   return 0;
  }
  samplelen = pool.playSample(samplePos, (int) samplelen, amplitude, env);
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
  if (pool == null) {
   println("-->>> You must load a file ('o' key command) before you can play audio samples. <<<--");
   return 0;
  }
  samplelen = pool.playSample(samplePos, (int) samplelen, amplitude);
  int durationMS = (int)(samplelen/sampleRate * 1000);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
  // return the length of the sample
  return samplelen;
}

/**
 * Utility method that applies a Gaussian distribution to vary it around its given value.
 * @param base
 * @return
 */
public int calcSampleLen(int base, float mean, float variance) {
  float vary = (float) (PixelAudio.gauss(mean, variance)); // vary the duration of the signal 
  // println("----->>> vary = "+ vary +", sampleScale = "+ sampleScale);
  int len = (int) (vary * base); // calculate the duration of the sample
  return len;
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
  circle(x, y, 24);
}  
/**
 * Draws a circle at the location of an audio trigger (mouseDown event).
 * @param x    x coordinate of circle
 * @param y    y coordinate of circle
 * @param d      diameter of circle
 */
public void drawCircle(int x, int y, float d, int c) {
  //float size = isRaining? random(10, 30) : 60;
  fill(c);
  noStroke();
  circle(x, y, d);
}  

public void raindrops() {
  float x = random(width/4, 3 * width/4);
  float y = random(16, height/5);
//    int signalPos = mapper.lookupSample(x, y);
//    int[] coords = mapper.lookupCoordinate(signalPos);
  audioMousePressed((int) x, (int) y);
}  

/**
 * TODO update for buffer animation
 * @param isStartListening    true if audio stream capture should be initiated, false if it should be ended
 * @return    current value of isStartListening
 */
public boolean listenToAnthem(boolean isStartListening) {
  if (isStartListening) {
    if (isFixedLength) {

    }
    else {

    }
  }
  else {
    if (isFixedLength) {

    }
  }
  return isStartListening;
}
