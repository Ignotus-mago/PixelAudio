// ------------------------------------------- //
//               TIMED LOCATION CLASS          //
// ------------------------------------------- //

public class TimedLocation {
  private int x;
  private int y;
  private int stopTime;
  private boolean isStale;
  
  public TimedLocation(int x, int y, int stop) {
    this.x = x;
    this.y = y;
    this.stopTime = stop;
    this.isStale = false;
  }
  
  public int getX() {
    return this.x;
  }
  
  public int getY() {
    return this.y;
  }
  
  public int stopTime() {
    return this.stopTime;
  }
  
  public boolean isStale() {
    return this.isStale;
  }
  public void setStale(boolean stale) {
    this.isStale = stale;
  }
}

// ------------------------------------------- //
//            NOTE TIMED LOCATION CLASS        //
// ------------------------------------------- //

public class NoteTimedLocation extends TimedLocation {
  private int pos;
  private int note;
  private float duration;
  private float amplitude;
  private ADSRParams adsr;
  private WaveSynth wavesynth;
  private int circleColor;
  public NoteTimedLocation(int x, int y, int stop, int pos, int note, float duration, float amplitude, int circ, ADSRParams adsr, WaveSynth ws) {
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
