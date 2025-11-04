package net.paulhertz.pixelaudio.voices;

/**
 * A lightweight, sample-rate–aware ADSR envelope generator for offline or manual sample processing.
 *
 * All times (attack, decay, release) are in seconds.
 * Sustain is a level between 0 and 1.
 *
 * Typical use:
 *   SimpleADSR env = new SimpleADSR(0.1f, 0.2f, 0.7f, 0.5f);
 *   env.setSampleRate(44100);
 *   env.noteOn();
 *   while (!env.isFinished()) {
 *       float gain = env.tick();
 *       // multiply your sample by gain
 *   }
 */
public class SimpleADSR {
    private float attackTime, decayTime, sustainLevel, releaseTime;
    private float value = 0f;
    private boolean attackPhase, decayPhase, sustainPhase, releasePhase;
    private float attackInc, decayDec, releaseDec;
    private float sampleRate = 44100f;

    public SimpleADSR(float attack, float decay, float sustain, float release) {
        this.attackTime = Math.max(attack, 0.0001f);
        this.decayTime = Math.max(decay, 0.0001f);
        this.sustainLevel = Math.max(0f, Math.min(1f, sustain));
        this.releaseTime = Math.max(release, 0.0001f);
        updateIncrements();
    }

    /** Must be called after constructing or when sample rate changes. */
    public void setSampleRate(float sampleRate) {
        this.sampleRate = Math.max(1f, sampleRate);
        updateIncrements();
    }

    private void updateIncrements() {
        attackInc  = 1.0f / (attackTime  * sampleRate);
        decayDec   = (1.0f - sustainLevel) / (decayTime * sampleRate);
        releaseDec = sustainLevel / (releaseTime * sampleRate);
    }

    public void noteOn() {
        attackPhase = true;
        decayPhase = sustainPhase = releasePhase = false;
    }

    public void noteOff() {
        releasePhase = true;
        attackPhase = decayPhase = sustainPhase = false;
    }

    /** Advance envelope one sample. Returns gain [0..1]. */
    public float tick() {
        if (attackPhase) {
            value += attackInc;
            if (value >= 1.0f) {
                value = 1.0f;
                attackPhase = false;
                decayPhase = true;
            }
        }
        else if (decayPhase) {
            value -= decayDec;
            if (value <= sustainLevel) {
                value = sustainLevel;
                decayPhase = false;
                sustainPhase = true;
            }
        }
        else if (releasePhase) {
            value -= releaseDec;
            if (value <= 0f) {
                value = 0f;
                releasePhase = false;
            }
        }
        return value;
    }

    public float getValue() { return value; }
    
    public boolean isReleasing() { return releasePhase; }

    /** True when envelope is idle (at zero). */
    public boolean isFinished() {
        return !(attackPhase || decayPhase || sustainPhase || releasePhase) && value <= 0f;
    }
    
    @Override
    public String toString() {
        return String.format("SimpleADSR[a=%.3fs, d=%.3fs, s=%.3f, r=%.3fs, value=%.4f]",
                attackTime, decayTime, sustainLevel, releaseTime, value);
    }
}
