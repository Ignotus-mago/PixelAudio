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
  this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
  // set the gain lower to avoid clipping from multiple voices
  setAudioGain(outputGain);
  println("---- audio out gain is "+ audioOut.getGain());
  // create Minim MultiChannelBuffers with one channel, buffer size equal to mapSize
  this.argo1Buffer = new MultiChannelBuffer(mapSize, 1);
  this.argo2Buffer = new MultiChannelBuffer(mapSize, 1);
  // initialize the signals for each buffer
  this.argo1Signal = argo1Buffer.getChannel(0);
  this.argo2Signal = argo2Buffer.getChannel(0);
  // audioLength == mapSize == argo1Signal.length == argo2Signal.length
  this.audioLength = argo1Signal.length;
  // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
  adsr1 = new ADSRParams(0.8f, 0.1f, 0.4f, 0.5f, 0.1f);
  adsr2 = new ADSRParams(0.8f, 0.4f, 0.1f, 0.5f, 0.1f);
  // create two PASamplerInstrument synths, though buffers are all 0s at the moment
  // adsrParams will be the default ADSR for the synth
  argo1Synth = new PASamplerInstrument(argo1Buffer, audioOut.sampleRate(), 32, audioOut, adsr1);
  argo2Synth = new PASamplerInstrument(argo2Buffer, audioOut.sampleRate(), 32, audioOut, adsr2);
  // initialize mouse event tracking array
  timeLocsArray = new ArrayList<TimedLocation>();
}

/**
 * This method writes a color channel from the an image to playBuffer, fulfilling a
 * central concept of the PixelAudio library: image is sound. Calls mapper.mapImgToSig(),
 * which will throw an IllegalArgumentException if img.pixels.length != sig.length or
 * img.width * img.height != mapper.getWidth() * mapper.getHeight().
 *
 * @param img       a PImage, a source of data
 * @param mapper    a PixelAudioMapper, handles mapping between image and audio signal
 * @param sig       an target array of float in audio format
 * @param chan      a color channel
 */
public void writeImageToAudio(PImage img, PixelAudioMapper mapper, float[] sig, PixelAudioMapper.ChannelNames chan) {
  sig = mapper.mapImgToSig(img.pixels, sig, chan);
}

/**
 * Calls Argosy to get a floating point representation of an argosy array.
 * Values in the returned array are scaled by argosy alpha / 255.0f,
 * so that opacity corresponds to audio gain. The arrays from Argosy are
 * loaded into audio buffers so that we can hear the patterns for argo1 and argo2.
 */
public void renderSignals() {
  float[] sig1 = argo1.getArgosySignal(this.argo1Alpha/255.0f);
  float[] sig2 = argo2.getArgosySignal(this.argo2Alpha/255.0f);
  System.arraycopy(sig1, 0, argo1Signal, 0, sig1.length);
  System.arraycopy(sig2, 0, argo2Signal, 0, sig2.length);
  argo1Buffer.setBufferSize(argo1Signal.length);
  argo2Buffer.setBufferSize(argo2Signal.length);
  argo1Buffer.setChannel(0, argo1Signal);
  argo2Buffer.setChannel(0, argo2Signal);
  argo1Synth.setBuffer(argo1Buffer);
  argo2Synth.setBuffer(argo2Buffer);
  // println("--->> generated new audio signals");
}

/**
 * Typically called from mousePressed with mouseX and mouseY, generates audio events.
 * TODO play these argo1 and argo2 signals in a stereo buffer.
 *
 * @param x    x-coordinate within a PixelAudioMapper's width
 * @param y    y-coordinate within a PixelAudioMapper's height
 */
public void audioMousePressed(int x, int y) {
  this.sampleX = x;
  this.sampleY = y;
  argo1SamplePos = getSamplePos(argo1, x, y, argo1Step);
  argo2SamplePos = getSamplePos(argo2, x, y, argo2Step);
  panning = map(sampleX, 0, width, -0.8f, 0.8f);
  if (argo1Signal == null || argo2Signal == null || isBufferStale) {
    renderSignals();
    isBufferStale = false;
  }
  if (this.isShowArgo1)
    playSample(argo1Synth, argo1SamplePos, calcSampleLen(), 0.6f, adsr1, panning);
  if (this.isShowArgo2)
    playSample(argo2Synth, argo2SamplePos, calcSampleLen(), 0.6f, adsr2, panning);
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
public int playSample(PASamplerInstrument synth, int samplePos, int samplelen, float amplitude, ADSRParams env, float pan) {
  synth.playSample(samplePos, samplelen, amplitude, env, pitchScaling, pan);
  int durationMS = (int)(samplelen/sampleRate * 1000);
  // println("---->> adding event to timeLocsArray "+  samplelen, durationMS, millis());
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
 * Calculate position of the image pixel within the signal path,
 * taking the shifting of pixels and audioSignal into account.
 * See MusicBoxBuffer for use of a windowed buffer in this calculation.
 */
public int getSamplePos(Argosy argo, int x, int y, int shift) {
  int pos = argo.getMapper().lookupSample(x, y);
  int totalShift = argo.getArgosyPixelShift();
  // calculate how much animation has shifted the indices into the buffer
  totalShift = (totalShift + shift % mapSize + mapSize) % mapSize;
  return (pos + totalShift) % mapSize;
}

/**
 * Run the animation for audio events.
 */
public void runTimeArray() {
  int currentTime = millis();
  timeLocsArray.forEach(tl -> {
    tl.setStale(tl.eventTime() < currentTime);
    if (!tl.isStale()) {
      drawCircle(tl.getX(), tl.getY());
    }
  }
  );
  timeLocsArray.removeIf(TimedLocation::isStale);
}

public void runAnimationArray() {
  if (animationLocsArray == null || animationLocsArray.size() <= 0) return;
  int currentTime = millis();
  animationLocsArray.forEach(tl -> {
    if (tl.eventTime() < currentTime) {
      sampleX = PixelAudio.constrain(Math.round(tl.getX()), 0, width - 1);
      sampleY = PixelAudio.constrain(Math.round(tl.getY()), 0, height - 1);
      float panning = map(sampleX, 0, width, -0.8f, 0.8f);
      argo1SamplePos = getSamplePos(argo1, sampleX, sampleY, argo1Step);
      argo2SamplePos = getSamplePos(argo2, sampleX, sampleY, argo2Step);
      if (argo1Signal == null || argo2Signal == null || isBufferStale) {
        renderSignals();
        isBufferStale = false;
      }
      if (this.isShowArgo1)
      playSample(argo1Synth, argo1SamplePos, calcSampleLen(), 0.6f, adsr1, panning);
      if (this.isShowArgo2)
      playSample(argo2Synth, argo2SamplePos, calcSampleLen(), 0.6f, adsr2, panning);
      tl.setStale(true);
    }
  }
  );
  animationLocsArray.removeIf(TimedLocation::isStale);
}

/**
 * Draws a circle at the location of an audio trigger (mouseDown event).
 * @param x    x coordinate of circle
 * @param y    y coordinate of circle
 */
public void drawCircle(int x, int y) {
  //float size = isRaining? random(10, 30) : 60;
  fill(color(199, 220, 233, 160));
  noStroke();
  circle(x, y, 40);
}

public ArrayList<PVector> animationPoints(float w, float h, int count) {
  if (aniPoints == null) aniPoints = new ArrayList<>();
  if (animationLocsArray == null) animationLocsArray = new ArrayList<>();
  int start = millis() + 50;
  int interval = 90;
  for (int i = 0; i < count; i++) {
    float x = w/2 * cos(TWO_PI * i/count) + width/2;
    float y = h/2 * sin(TWO_PI * i/count) + height/2;
    aniPoints.add(new PVector(x, y));
    animationLocsArray.add(new TimedLocation(round(x), round(y), start + i * interval));
  }
  return aniPoints;
}
