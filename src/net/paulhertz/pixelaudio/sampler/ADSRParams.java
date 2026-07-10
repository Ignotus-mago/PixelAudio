/*
 *  Copyright (c) 2024 - 2025 by Paul Hertz <ignotus@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.paulhertz.pixelaudio.sampler;

import ddf.minim.ugens.ADSR;

// ------------------------------------------- //
//                 ADSR WRAPPER                //
// ------------------------------------------- //

/**
 * Value object describing an ADSR envelope. Minim's implementation of a typical
 * ADSR (attack, decay, sustain, release) envelope doesn't allow us to retrieve
 * the values of its parameters after it's been created. This class makes those
 * values available and also stores the per-stage curves used by
 * {@link SimpleADSR}.
 */
public class ADSRParams {
    /** Default curve used by {@link SimpleADSR}; {@code 1.0f} is linear. */
    public static final float DEFAULT_CURVE = 4.0f;

    private final float maxAmp;   // maximum amplitude
    private final float attack;   // attack time (sec)
    private final float decay;    // decay time (sec)
    private final float sustain;  // sustain level (0..1)
    private final float release;  // release time (sec)
    private final float attackCurve;
    private final float decayCurve;
    private final float releaseCurve;

    /**
     * Construct an ADSRParams object - note that duration of the envelope is 
     * calculated up to but not including release time.
     * 
     * @param maxAmp      maximum amplitude (0..1)
     * @param attack      attack time in seconds
     * @param decay       decay time in seconds
     * @param sustain     sustain amplitude (0..1)
     * @param release     release time in seconds
     */
    public ADSRParams(float maxAmp, float attack, float decay, float sustain, float release) {
        this(maxAmp, attack, decay, sustain, release,
                DEFAULT_CURVE, DEFAULT_CURVE, DEFAULT_CURVE);
    }

    /**
     * Constructs an ADSRParams object with explicit stage curves.
     *
     * <p>A curve of {@code 1.0f} is linear. Values greater than {@code 1.0f}
     * rise slowly at first and more quickly near the end; values between
     * {@code 0.0f} and {@code 1.0f} do the reverse. Curve values are clamped
     * to the minimum supported by {@link SimpleADSR}.</p>
     *
     * @param maxAmp        maximum amplitude (0..1)
     * @param attack        attack time in seconds
     * @param decay         decay time in seconds
     * @param sustain       sustain amplitude (0..1)
     * @param release       release time in seconds
     * @param attackCurve   attack-stage curve
     * @param decayCurve    decay-stage curve
     * @param releaseCurve  release-stage curve
     */
    public ADSRParams(float maxAmp, float attack, float decay, float sustain, float release,
                      float attackCurve, float decayCurve, float releaseCurve) {
        this.maxAmp = maxAmp;
        this.attack = attack;
        this.decay = decay;
        this.sustain = sustain;
        this.release = release;
        this.attackCurve = clampCurve(attackCurve);
        this.decayCurve = clampCurve(decayCurve);
        this.releaseCurve = clampCurve(releaseCurve);
    }

    /** @return maximum amplitude */
    public float getMaxAmp() { return maxAmp; }
    /** @return attack time in seconds */
    public float getAttack() { return attack; }
    /** @return decay time in seconds */
    public float getDecay()  { return decay; }
    /** @return sustain level in the range 0..1 */
    public float getSustain(){ return sustain; }
    /** @return release time in seconds */
    public float getRelease(){ return release; }
    /** @return attack-stage curve; {@code 1.0f} is linear */
    public float getAttackCurve() { return attackCurve; }
    /** @return decay-stage curve; {@code 1.0f} is linear */
    public float getDecayCurve() { return decayCurve; }
    /** @return release-stage curve; {@code 1.0f} is linear */
    public float getReleaseCurve() { return releaseCurve; }

    /**
     * @return a copy of this ADSRParams object
     */
    public ADSRParams copy() {
        return new ADSRParams(maxAmp, attack, decay, sustain, release,
                attackCurve, decayCurve, releaseCurve);
    }

    /**
     * Returns a copy with new attack, decay, and release curves.
     *
     * @param attackCurve   attack-stage curve
     * @param decayCurve    decay-stage curve
     * @param releaseCurve  release-stage curve
     * @return a new ADSRParams with the requested curves
     */
    public ADSRParams withCurves(float attackCurve, float decayCurve, float releaseCurve) {
        return new ADSRParams(maxAmp, attack, decay, sustain, release,
                attackCurve, decayCurve, releaseCurve);
    }

    /** 
     * Build a fresh Minim ADSR from these parameters. 
     * @return a Minim ADSR constructed with the instance variables stored in ADSRParams
     */
    public ADSR toADSR() {
        return new ADSR(maxAmp, attack, decay, sustain, release);
    }

    /**
     * Builds a fresh SimpleADSR from this complete envelope description.
     *
     * <p>The returned envelope has not been triggered; call
     * {@link SimpleADSR#noteOn()} when it should begin.</p>
     *
     * @param sampleRate sample rate in Hz
     * @return a SimpleADSR configured with this instance's times, level, and curves
     */
    public SimpleADSR toSimpleADSR(float sampleRate) {
        SimpleADSR envelope = new SimpleADSR(
                attack, decay, sustain, release,
                attackCurve, decayCurve, releaseCurve);
        envelope.setSampleRate(sampleRate);
        return envelope;
    }
    
    /**
     * @return a String representation of the ADSRParams data
     */
    @Override
    public String toString() {
        return ("maxAmp: " + maxAmp + ", ADSR: " + attack + ", " + decay + ", "
                + sustain + ", " + release + ", curves: " + attackCurve + ", "
                + decayCurve + ", " + releaseCurve);
    }

    private static float clampCurve(float curve) {
        return Math.max(0.1f, curve);
    }
}
