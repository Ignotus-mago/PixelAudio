/**
 * A wrapper to improve data access for Minim's ADSR class, which is opaque
 * in that we can't access the ADSR component values. 
 * We also can't set any of the ADSR fields, that would require a new ADSR.
 * We aren't covering all the ADSR constructors, just the one that is useful 
 * for our application.
 */
public class PAEnvelope {
  private float max;
  private float att;
  private float dec;
  private float sus;
  private float rel;
  public ADSR adsr;
  
  /**
   * @param maxAmplitude
   * @param attackTime
   * @param decayTime
   * @param sustainLevel
   * @param releaseTime
   * @return  a new PAEnvelope instance
   */
  public PAEnvelope createPAEnvelope(float maxAmplitude, float attackTime, float decayTime, float sustainLevel,
      float releaseTime) {
    return new PAEnvelope(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  }

  /**
   * @param maxAmplitude
   * @param attackTime
   * @param decayTime
   * @param sustainLevel
   * @param releaseTime
   */
  public PAEnvelope(float maxAmplitude, float attackTime, float decayTime, float sustainLevel, float releaseTime) {
    this.max = maxAmplitude;
    this.att = attackTime;
    this.dec = decayTime;
    this.sus = sustainLevel;
    this.rel = releaseTime;
    this.adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
  }

  public float getMax() {
    return max;
  }

  public float getAtt() {
    return att;
  }

  public float getDec() {
    return dec;
  }

  public float getSus() {
    return sus;
  }

  public float getRel() {
    return rel;
  }

  public ADSR getAdsr() {
    return adsr;
  }
  
  public PAEnvelope clone() {
    return new PAEnvelope(max, att, dec, sus, rel);
  }
  
  public ADSR modADSRAmplitude(float newMax, float newSus) {
    return new PAEnvelope(newMax, att, dec, newSus, rel).adsr;
  }
  
}
