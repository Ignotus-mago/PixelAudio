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
