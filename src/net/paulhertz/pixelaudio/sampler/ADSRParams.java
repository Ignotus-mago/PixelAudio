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
 * Value object for Minim ADSR parameters. Minim's implementation of a typical
 * ADSR (attack, decay, sustain, release) envelope doesn't allow us to retrieve
 * the values of its parameters after it's been created. This wrapper for ADSR
 * makes those values available. 
 */
public class ADSRParams {
    private final float maxAmp;   // maximum amplitude
    private final float attack;   // attack time (sec)
    private final float decay;    // decay time (sec)
    private final float sustain;  // sustain level (0..1)
    private final float release;  // release time (sec)

    /**
     * Construct an ADSRParams object - note that duration of the envelope is 
     * calculated up to but not including release time.
     * 
     * @param maxAmp      maximum amplitude (0..1)
     * @param attack      attack time in seconds
     * @param decay       decay time in seconds
     * @param sustain     sustain amplitude 90..1)
     * @param release     release time in seconds
     */
    public ADSRParams(float maxAmp, float attack, float decay, float sustain, float release) {
        this.maxAmp = maxAmp;
        this.attack = attack;
        this.decay = decay;
        this.sustain = sustain;
        this.release = release;
    }

    public float getMaxAmp() { return maxAmp; }
    public float getAttack() { return attack; }
    public float getDecay()  { return decay; }
    public float getSustain(){ return sustain; }
    public float getRelease(){ return release; }

    /**
     * @return a copy of this ADSRParams object
     */
    public ADSRParams copy() {
        return new ADSRParams(maxAmp, attack, decay, sustain, release);
    }

    /** 
     * Build a fresh Minim ADSR from these parameters. 
     * @return a Minim ADSR constructed with the instance variable stored in ADSRParams
     */
    public ADSR toADSR() {
        return new ADSR(maxAmp, attack, decay, sustain, release);
    }
    
    /**
     * @return a String representation of the ADSRParams data
     */
    public String toString() {
    	return ("maxAmp: "+ maxAmp +", ADSR: "+ attack +", "+ decay +", "+ sustain +", "+ release);
    }
    
}
