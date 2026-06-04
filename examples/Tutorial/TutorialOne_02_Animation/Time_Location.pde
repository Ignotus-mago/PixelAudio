/*----------------------------------------------------------------*/
/*                                                                */
/*                 TIME/LOCATION/ACTION METHODS                   */
/*                                                                */
/*----------------------------------------------------------------*/

/**
 * Run the animation for audio events.
 */
public void runTimeArray() {
  int currentTime = millis();
  timeLocsArray.forEach(tl -> {
    tl.setStale(tl.eventTime() < currentTime);
    if (!tl.isStale()) {
      drawCircle(tl.getX(), tl.getY());
    }
  }
  );
  timeLocsArray.removeIf(TimedLocation::isStale);
}

/**
 * Draws a circle at the location of an audio trigger (mouseDown event).
 * @param x    x coordinate of circle
 * @param y    y coordinate of circle
 */
public void drawCircle(int x, int y) {
  //float size = isRaining? random(10, 30) : 60;
  fill(color(233, 220, 199));
  noStroke();
  circle(x, y, 60);
}
