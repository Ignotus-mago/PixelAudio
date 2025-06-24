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
  // create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
  this.playBuffer = new MultiChannelBuffer(mapSize, 1);
  this.audioSignal = playBuffer.getChannel(0);
  this.audioLength = audioSignal.length;
  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
  adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
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
  this.sampleX = x;
  this.sampleY = y;
  samplePos = mapper.lookupSample(x, y);
  // update audioSignal and playBuffer if audioSignal hasn't been initialized or if 
  // playBuffer needs to be refreshed after changes to its data source (isBufferStale == true).
  if (audioSignal == null || isBufferStale) {
    renderSignals();
    isBufferStale = false;
  }
  playSample(playBuffer, samplePos, calcSampleLen(), 0.6f, new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime));
}
  
/**
 * Plays an audio sample.
 * 
 * @param samplePos    position of the sample in the audio buffer
 * @param samplelen    length of the sample (will be adjusted)
 * @param amplitude    amplitude of the sample on playback
 * @param adsr         an ADSR envelope for the sample
 * @return the calculated sample length in samples
 */
public int playSample(MultiChannelBuffer buffer, int samplePos, int samplelen, float amplitude, ADSR adsr) {
  // println("--- play "+ twoPlaces.format(amplitude));
  audioSampler = new Sampler(buffer, sampleRate, 8); // create a Minim Sampler from the buffer sampleRate sampling
                             // rate, for up to 8 simultaneous outputs 
  audioSampler.amplitude.setLastValue(amplitude);    // set amplitude for the Sampler
  audioSampler.begin.setLastValue(samplePos);        // set the Sampler to begin playback at samplePos, which 
                               // corresponds to the place the mouse was clicked
  int releaseDuration = (int) (releaseTime * sampleRate); // calculate the release time.
  if (samplePos + samplelen >= mapSize) {
    samplelen = mapSize - samplePos; // make sure we don't exceed the mapSize
    println("----->>> sample length = " + samplelen);
  }
  int durationPlusRelease = this.samplelen + releaseDuration;
  int end = (samplePos + durationPlusRelease >= this.mapSize) ? this.mapSize - 1
      : samplePos + durationPlusRelease;
  // println("----->>> end = " + end);
  audioSampler.end.setLastValue(end);
  this.instrument = new WFInstrument(audioOut, audioSampler, adsr);
  // play command takes a duration in seconds
  float duration = samplelen / (float) (sampleRate);
  instrument.play(duration);
  timeLocsArray.add(new TimedLocation(sampleX, sampleY, (int) (duration * 1000) + millis()));
  // return the length of the sample
  return samplelen;
}

public int calcSampleLen() {
  float vary = (float) (PixelAudio.gauss(this.sampleScale, this.sampleScale * 0.125f)); // vary the duration of the signal 
  // println("----->>> vary = "+ vary +", sampleScale = "+ sampleScale);
  this.samplelen = (int) (vary * this.sampleBase); // calculate the duration of the sample
  return samplelen;
}

/**
 * Run the animation for audio events. 
 */
public void runTimeArray() {
  int currentTime = millis();
  timeLocsArray.forEach(tl -> {
    tl.setStale(tl.stopTime() < currentTime);
    if (!tl.isStale()) {
      drawCircle(tl.getX(), tl.getY());
    }
  });
  timeLocsArray.removeIf(TimedLocation::isStale);
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

/*        END AUDIO METHODS                        */
