/**
 * Used to schedule or track events that take place at specific coordinate locations.
 * See WorkFlowApp3 and subsequent versions. 
 */
public class TimedLocation implements Comparable<TimedLocation> {
  private int x;
  private int y;
  private int eventTime;
  private boolean isStale;

  public TimedLocation(int x, int y, int stop) {
    this.x = x;
    this.y = y;
    this.eventTime = stop;
    this.isStale = false;
  }

  public int getX() {
    return this.x;
  }

  public int getY() {
    return this.y;
  }

  public int eventTime() {
    return this.eventTime;
  }

  public boolean isStale() {
    return this.isStale;
  }

  public void setStale(boolean stale) {
    this.isStale = stale;
  }
    
  public int compareTo(TimedLocation tl) {
    if (eventTime() < tl.eventTime()) return 1;
    else {
      if (eventTime() == tl.eventTime) return 0;
      else return -1;
    }
  }
  
}
