/*----------------------------------------------------------------*/
/*                                                                */
/*                        NETWORKING                              */
/*                                                                */
/*----------------------------------------------------------------*/


// required by the PANetworkCLientINF interface
public PApplet getPApplet() {
  return this;
}

// required by the PANetworkCLientINF interface
public PixelAudioMapper getMapper() {
  return this.mapper;
}

// required by the PANetworkCLientINF interface
public void controlMsg(String control, float val) {
  if (control.equals("detune")) {
    println("--->> controlMsg is \"detune\" = "+ val +"; detune is not implemented.");
  }
}

// required by the PANetworkCLientINF interface
public int playSample(int samplePos) {
  int[] coords = mapper.lookupCoordinate(samplePos);
  sampleX = coords[0];
  sampleY = coords[1];
  if (audioSignal == null || isBufferStale) {
    renderSignals();
    isBufferStale = false;
  }
  return playSample(samplePos, calcSampleLen(), 0.6f, new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime));
}

// required by the PANetworkCLientINF interface
public void playPoints(ArrayList<PVector> pts) {
  eventPoints = pts;
  playPoints();
}
