package net.paulhertz.pixelaudio.sampler;

public class SamplerBrushEvent {
    // spatial (optional but useful for mapping / visualization)
    public final int x;
    public final int y;

    // timing
    public final int eventTimeMs;        // UI-time (for now)
    // later: public final long eventSampleTime;

    // playback
    public final int samplePos;
    public final int sampleLen;

    public final float gain;
    public final float pitchRatio;
    public final float pan;

    public final ADSRParams env;

    public SamplerBrushEvent(int x, int y, int eventTimeMs, int samplePos, int sampleLen, 
                      float gain, float pitchRatio, ADSRParams env, float pan) {
        this.x = x;
        this.y = y;
        this.eventTimeMs = eventTimeMs;
        this.samplePos = samplePos;
        this.sampleLen = sampleLen;
        this.gain = gain;
        this.pitchRatio = pitchRatio;
        this.env = env;
        this.pan = pan;
    }
}