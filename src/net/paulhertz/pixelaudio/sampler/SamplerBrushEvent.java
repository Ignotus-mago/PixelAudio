package net.paulhertz.pixelaudio.sampler;

import net.paulhertz.pixelaudio.schedule.TimedLocation;

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