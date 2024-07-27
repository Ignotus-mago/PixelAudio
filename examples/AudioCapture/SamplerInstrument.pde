// using minim's Instrument interface
class SamplerInstrument implements Instrument {
  Sampler sampler;
  ADSR adsr;

  SamplerInstrument(Sampler sampler, ADSR adsr) {
    this.sampler = sampler;
    this.adsr = adsr;
    sampler.patch(adsr);
  }

  public void play() {
    // Trigger the ADSR envelope by calling noteOn()
    // Duration of 0.0 means the note is sustained indefinitely
    noteOn(0.0f);
  }

  public void play(float duration) {
    // Trigger the ADSR envelope by calling noteOn()
    // Duration of 0.0 means the note is sustained indefinitely
    // Duration should be in seconds
    // println("----->>> SamplerInstrument.play("+ duration +")");
    noteOn(duration);
  }

  public void noteOn(float duration) {
    // Trigger the ADSR envelope and sampler
    adsr.noteOn();
    sampler.trigger();
    adsr.patch(audioOut);
    if (duration > 0) {
      // println("----->>> duration > 0");
      int durationMillis = (int) (duration * 1000);
      // schedule noteOff with an anonymous Timer and TimerTask
      new java.util.Timer().schedule(new java.util.TimerTask() {
        public void run() {
          noteOff();
        }
      }
      , durationMillis);
    }
  }

  public void noteOff() {
    // println("----->>> noteOff event");
    adsr.unpatchAfterRelease(audioOut);
    adsr.noteOff();
  }

  // Getter for the Sampler instance
  public Sampler getSampler() {
    return sampler;
  }

  // Setter for the Sampler instance
  public void setSampler(Sampler sampler) {
    this.sampler = sampler;
  }

  // Getter for the ADSR instance
  public ADSR getADSR() {
    return adsr;
  }

  // Setter for the ADSR instance
  public void setADSR(ADSR adsr) {
    this.adsr = adsr;
  }
}
