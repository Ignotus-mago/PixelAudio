/**
 * Used to schedule or track events that take place at specific coordinate locations.
 * See WorkFlowApp3 and subsequent versions. 
 */
public class TimedLocation implements Comparable<TimedLocation> {
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
    
  public int compareTo(TimedLocation tl) {
    if (stopTime() < tl.stopTime()) return 1;
    else {
      if (stopTime() == tl.stopTime) return 0;
      else return -1;
    }
  }
  
  
}
