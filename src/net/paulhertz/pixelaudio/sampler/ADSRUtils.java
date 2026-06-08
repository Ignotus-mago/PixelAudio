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

/** Utility functions for ADSRParams. */
public final class ADSRUtils {
    private ADSRUtils() {} // no instantiation

    // TODO version that does not scale release, or with boolean flag for release scaling
    /**
     * Returns a new ADSRParams scaled to fit the specified duration (ms).
     * 
     * @param adsr        a ADSRParams instance
     * @param duration    duration in milliseconds 
     * @return            a new ADSRParams with attack, decay and release scaled to the requested duration
     */
    public static ADSRParams fitEnvelopeToDuration(ADSRParams adsr, int duration) {
        if (adsr == null) {
            throw new IllegalArgumentException("ADSRParams must not be null");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        float totalSeconds = duration / 1000f;
        float totalEnv = adsr.getAttack() + adsr.getDecay() + adsr.getRelease();

        if (totalEnv <= 0.0f) {
            return new ADSRParams(adsr.getMaxAmp(), 0, 0, adsr.getSustain(), 0);
        }

        float scale = totalSeconds / totalEnv;
        return new ADSRParams(
            adsr.getMaxAmp(),
            adsr.getAttack()  * scale,
            adsr.getDecay()   * scale,
            adsr.getSustain(),
            adsr.getRelease() * scale
        );
    }
}
