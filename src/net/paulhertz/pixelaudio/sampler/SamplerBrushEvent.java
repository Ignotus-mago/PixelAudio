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

import net.paulhertz.pixelaudio.schedule.TimedLocation;

/**
 * Stores values used for SamplerBrush event staging, used in tutorial examples
 * such as {@link TutorialOne_03_Drawing}. SamplerBrush objects implement AudioBrush
 * and address the Sampler synthesis engine. A SamplerBrushEvent is a discrete 
 * audio synthesis process, complete with ADSR envelope. 
 * <p>
 * There is no corresponding "GranularBrushEvent". Grains, which allow similar 
 * parametric control with the grain window equivalent to the ADSR envelope, 
 * are embedded in the audio synthesis process and hence are not discrete events. 
 * </p>
 */
public class SamplerBrushEvent implements Comparable<SamplerBrushEvent> {
    // spatial (optional but useful for mapping / visualization)
	/** x-coordinate associated with SamplerBrushEvent */
    public final int x;
	/** y-coordinate associated with SamplerBrushEvent */
    public final int y;

    // timing
	/** time of event, typically a milliseconds offset from a trigger event */
    public final int eventTimeMs;        // UI-time (for now)
    // later: public final long eventSampleTime;

    // playback
    /** audio buffer index, typically derived from event coordinates */
    public final int samplePos;
    /** event duration */
    public final int durationMs;

    public final float gain;
    public final float pitchRatio;
    public final float pan;

    public final ADSRParams env;
    
    // state 
    public boolean isStale;

    public SamplerBrushEvent(int x, int y, int eventTimeMs, int samplePos, int sampleLen, 
                      float gain, float pitchRatio, ADSRParams env, float pan) {
        this.x = x;
        this.y = y;
        this.eventTimeMs = eventTimeMs;
        this.samplePos = samplePos;
        this.durationMs = sampleLen;
        this.gain = gain;
        this.pitchRatio = pitchRatio;
        this.env = env;
        this.pan = pan;
        this.isStale = false;
    } 
    
	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public int eventTimeMs() {
		return eventTimeMs;
	}

	public boolean isStale() {
		return this.isStale;
	}

	public void setStale(boolean stale) {
		this.isStale = stale;
	}
		
	public int getDurationMs() {
		return durationMs;
	}


	public int compareTo(SamplerBrushEvent tl) {
		if (this.eventTimeMs() > tl.eventTimeMs()) return 1;
		else {
			if (this.eventTimeMs() == tl.eventTimeMs()) return 0;
			else return -1;
		}
	}

}