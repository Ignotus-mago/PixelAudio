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

/**
 * SimpleADSR — software envelope generator with optional exponential curves.
 * <p>
 * Features:
 * <ul>
 *  <li>Sample-rate–based time scaling</li>
 *  <li>Per-stage exponential curvature</li>
 *  <li>Sustain-level clamping</li>
 *  <li>`noteOn()`, `noteOff()`, `tick()`, and `isFinished()`</li>
 * </ul>
 * </p>
 */
public class SimpleADSR {

    // --- Time and shape parameters ---
    private float attackTime;   // seconds
    private float decayTime;    // seconds
    private float sustainLevel; // 0–1
    private float releaseTime;  // seconds
    private float sampleRate = 44100f;

    // --- Shape curvature ---
    private float attackCurve = 4.0f;   // >1 = more exponential, 1 = linear
    private float decayCurve = 4.0f;
    private float releaseCurve = 4.0f;

    // --- Internal state ---
    private enum Stage { IDLE, ATTACK, DECAY, SUSTAIN, RELEASE, FINISHED }
    private Stage stage = Stage.IDLE;
    private float value = 0f;
    private float releaseStart = 0f;  
    private int samplesInStage = 0;
    private int stageSamples = 0;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    public SimpleADSR(float attack, float decay, float sustain, float release) {
        this.attackTime = Math.max(attack, 0f);
        this.decayTime = Math.max(decay, 0f);
        this.sustainLevel = Math.max(0f, Math.min(1f, sustain));
        this.releaseTime = Math.max(release, 0f);
    }

    // Optionally allow curvature control
    public SimpleADSR(float attack, float decay, float sustain, float release,
                      float attackCurve, float decayCurve, float releaseCurve) {
        this(attack, decay, sustain, release);
        this.attackCurve = Math.max(0.1f, attackCurve);
        this.decayCurve = Math.max(0.1f, decayCurve);
        this.releaseCurve = Math.max(0.1f, releaseCurve);
    }

    // ------------------------------------------------------------------------
    // Lifecycle control
    // ------------------------------------------------------------------------

    public void setSampleRate(float sr) {
        if (sr > 0) sampleRate = sr;
    }

    public void noteOn() {
        stage = Stage.ATTACK;
        samplesInStage = 0;
        stageSamples = Math.max(1, (int)(attackTime * sampleRate));
    }

    public void noteOff() {
    	releaseStart = value;      // capture current amplitude
        stage = Stage.RELEASE;
        samplesInStage = 0;
        stageSamples = Math.max(1, (int)(releaseTime * sampleRate));
    }

    public boolean isFinished() {
        return stage == Stage.FINISHED || stage == Stage.IDLE;
    }

    // ------------------------------------------------------------------------
    // Tick: advance one sample
    // ------------------------------------------------------------------------

    public float tick() {
        switch (stage) {
            case ATTACK:
                value = exponentialInterp(samplesInStage, stageSamples, 0f, 1f, attackCurve);
                if (++samplesInStage >= stageSamples) {
                    stage = Stage.DECAY;
                    samplesInStage = 0;
                    stageSamples = Math.max(1, (int)(decayTime * sampleRate));
                }
                break;

            case DECAY:
                value = exponentialInterp(samplesInStage, stageSamples, 1f, sustainLevel, decayCurve);
                if (++samplesInStage >= stageSamples) {
                    stage = Stage.SUSTAIN;
                    value = sustainLevel;
                }
                break;

            case SUSTAIN:
                value = sustainLevel;
                break;

            case RELEASE:
                value = exponentialInterp(samplesInStage, stageSamples, releaseStart, 0f, releaseCurve);
                if (++samplesInStage >= stageSamples) {
                    stage = Stage.FINISHED;
                    value = 0f;
                }
                break;

            case FINISHED:
            case IDLE:
            default:
                value = 0f;
                break;
        }
        /*
        if (stage == Stage.RELEASE && samplesInStage % 2000 == 0) {
            System.out.printf("[ADSR release] tick=%d value=%.5f stage=%s%n",
                samplesInStage, value, stage);
        }
		*/
        return value;
    }

    // ------------------------------------------------------------------------
    // Exponential interpolation utility
    // ------------------------------------------------------------------------

    private static float exponentialInterp(int step, int total, float start, float end, float curve) {
        if (total <= 0) return end;
        float t = (float) step / total;
        if (curve == 1.0f) return start + (end - start) * t; // linear
        float shaped = (float) ((Math.pow(curve, t) - 1.0) / (curve - 1.0));
        return start + (end - start) * shaped;
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    public float getValue() { return value; }

    public void setCurves(float attackC, float decayC, float releaseC) {
        attackCurve = Math.max(0.1f, attackC);
        decayCurve = Math.max(0.1f, decayC);
        releaseCurve = Math.max(0.1f, releaseC);
    }

    public void setTimes(float a, float d, float s, float r) {
        attackTime = a;
        decayTime = d;
        sustainLevel = s;
        releaseTime = r;
    }
}
