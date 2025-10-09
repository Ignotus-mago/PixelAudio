import ddf.minim.ugens.ADSR;

// ------------------------------------------- //
//                 ADSR WRAPPER                //
// ------------------------------------------- //

/**
 * Value object for Minim ADSR parameters. Minim's implementation of a typical
 * ADSR (attack, decay, sustain, release) envelope doesn't allow us to retrieve
 * the values of its parameters after it's been created. This wrapper for ADSR
 * makes those values available. 
 */
public class ADSRParams {
  private final float maxAmp;   // maximum amplitude (0..1)
  private final float attack;   // attack time (sec)
  private final float decay;    // decay time (sec)
  private final float sustain;  // sustain level (0..1)
  private final float release;  // release time (sec)

  public ADSRParams(float maxAmp, float attack, float decay, float sustain, float release) {
    this.maxAmp = maxAmp;
    this.attack = attack;
    this.decay = decay;
    this.sustain = sustain;
    this.release = release;
  }

  public float getMaxAmp() {
    return maxAmp;
  }
  public float getAttack() {
    return attack;
  }
  public float getDecay() {
    return decay;
  }
  public float getSustain() {
    return sustain;
  }
  public float getRelease() {
    return release;
  }

  /** Build a fresh Minim ADSR from these parameters. */
  public ADSR toADSR() {
    return new ADSR(maxAmp, attack, decay, sustain, release);
  }
  
  public String toString() {
    return ("maxAmp, ADSR: "+ maxAmp +", "+ attack +", "+ decay +", "+ sustain +", "+ release);
  }
}
