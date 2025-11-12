/**
 * A TimedLocation child class that can schedule musical note events. 
 */
public class NoteTimedLocation extends TimedLocation {
    private int pos;
    private int note;
    private float duration;
    private float amplitude;
    private ADSRParams adsr;
    private WaveSynth wavesynth;
    private int circleColor;

    public NoteTimedLocation(int x, int y, int stop, int pos, int note, float duration, 
           float amplitude, int circ, ADSRParams adsr, WaveSynth ws) {
        super(x, y, stop);
        this.pos = pos;
        this.note = note;
        this.duration = duration;
        this.amplitude = amplitude;
        this.circleColor = circ;
        this.adsr = adsr;
        this.wavesynth = ws;
    }
    
    public int getNote() {
        return this.note;
    }
    
    public int getPos() {
        return this.pos;
    }
    
    public float getDuration() {
        return this.duration;
    }
    
    public float getAmplitude() {
        return this.amplitude;
    }
    
    public int getCircleColor() {
        return this.circleColor;
    }
    
    public ADSRParams getAdsr() {
        return this.adsr;
    }
    
    public WaveSynth getWaveSynth() {
        return this.wavesynth;
    }
    
}
