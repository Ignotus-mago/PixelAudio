/*----------------------------------------------------------------*/
/*                                                                */
/*                    BEGIN DRAWING METHODS                       */
/*                                                                */
/*----------------------------------------------------------------*/

/**
 * Initializes allPoints and adds the current mouse location to it. 
 */
public void initAllPoints() {
  allPoints = new ArrayList<PVector>();
  allTimes = new ArrayList<Integer>();
  startTime = millis();
  allTimes.add(startTime);
  addPoint(mouseX, mouseY);
}

/**
 * Responds to mousePressed events associated with drawing.
 */
public void handleMousePressed(int x, int y) {
  if (activeBrush != null) {
    // a brushShape was triggered
    eventPoints = activeBrush.getEventPoints();
    loadEventPoints();
    activeBrush = null;
  } 
  else {
    // handle audio generation in response to a mouse click
    audioMousePressed(PApplet.constrain(x, 0, width-1), PApplet.constrain(y, 0, height-1));
  }
}

/**
 * While user is dragging the mouses and isDrawMode == true, accumulates new points
 * to allPoints and event times to allTimes.
 */
public void addPoint(int x, int y) {
  // we do some very basic point thinning to eliminate successive duplicate points
  if (x != currentPoint.x || y != currentPoint.y) {
    currentPoint = new PVector(x, y);
    allPoints.add(currentPoint);
    allTimes.add(millis());
    setSampleVars(x, y);
  }
}

/**
 * Processes the eventPoints list to create TimedLocation events 
 * and stores them in curveTLEvents. 
 */
public void loadEventPoints() {
  if (eventPoints != null) {
    eventPointsIter = eventPoints.listIterator();
    int startTime = millis();
    // println("building pointsTimer: "+ startTime);
    if (curveTLEvents == null) curveTLEvents = new ArrayList<TimedLocation>();
    storeCurveTL(eventPointsIter, startTime);
  }
  else {
    println("--->> NULL eventPoints");
  }
}


/**
 * Processes the eventPoints list to create TimedLocation events 
 * and stores them in curveTLEvents. 
 * @param startTime    time in millis (in the future!) when event should begin
 */
public void loadEventPoints(int startTime) {
  if (eventPoints != null) {
    eventPointsIter = eventPoints.listIterator();
    // println("building pointsTimer: "+ startTime);
    if (curveTLEvents == null) curveTLEvents = new ArrayList<TimedLocation>();
    storeCurveTL(eventPointsIter, startTime);
  }
  else {
    println("--->> NULL eventPoints");
  }
}

/**
 * @param iter         a ListIterator over eventPoints
 * @param startTime    a time in millis
 */
public synchronized void storeCurveTL(ListIterator<PVector> iter, int startTime) {
  startTime += 50;
  int i = 0;
  while (iter.hasNext()) {
    PVector loc = iter.next();
    curveTLEvents.add(new TimedLocation(Math.round(loc.x), Math.round(loc.y), startTime + i++ * eventStep));
  }
  Collections.sort(curveTLEvents);
}  

/**
 * Initializes a PACurveMaker instance with allPoints as an argument to the factory method 
 * PACurveMaker.buildCurveMaker() and then fills in PACurveMaker instance variables from 
 * variables in the calling class (TutorialOneDrawing, here). 
 */
public void initCurveMaker() {
  curveMaker = PACurveMaker.buildCurveMaker(allPoints);
  curveMaker.setBrushColor(readyBrushColor);
  curveMaker.setActiveBrushColor(activeBrushColor);
  curveMaker.setEpsilon(epsilon);
  curveMaker.setTimeStamp(startTime);
  curveMaker.setTimeOffset(millis() - startTime);
  curveMaker.calculateDerivedPoints();
  PABezShape curve = curveMaker.getCurveShape();
  eventPoints = curve.getPointList(polySteps);
  loadEventPoints();
  curveMaker.setDragTimes(reconfigureTimeList(allTimes));
  this.brushShapesList.add(curveMaker);
  setSampleVars(mouseX, mouseY);
}

/**
 * @param timeList    an array of ints representing absolute times in millis
 * @return an array of ints with a start time in position 0, followed by offsets
 */
public int[] reconfigureTimeList(int[] timeList) {
  int startTime = timeList[0];
  int[] timeOffsetsList = new int[timeList.length];
  timeOffsetsList[0] = startTime; // first item is the original event time
  for (int i = 1; i < timeList.length; i++) {
    timeOffsetsList[i] = timeList[i] - startTime;
  }
  return timeOffsetsList;
}

/**
 * @param timeList    an ArrayList of ints representing absolute times in millis
 * @return an ArrayList of ints with a start time in position 0, followed by offsets
 */
public ArrayList<Integer> reconfigureTimeList(ArrayList<Integer> timeList) {
  int startTime = timeList.get(0);
  ArrayList<Integer> timeOffsetsList = new ArrayList<>();
  timeOffsetsList.add(startTime); // first item is the original event time
  for (int i = 1; i < timeList.size(); i++) {
    timeOffsetsList.add(timeList.get(i) - startTime);
  }
  return timeOffsetsList;
}


/**
 * Iterates over brushShapesList and draws the brushstrokes stored in 
 * each PACurveMaker in the list. 
 */
public void drawBrushShapes() {
  if (this.brushShapesList.size() > 0) {
    int idx = 0;
    activeBrush = null;
    for (PACurveMaker brush : brushShapesList) {
      int brushFill = readyBrushColor;
      if (mouseInPoly(brush.getBrushPoly())) {
        brushFill = activeBrushColor;
        activeBrush = brush;
        activeIndex = idx;
      }
      PACurveUtility.shapeDraw(this, brush.getBrushShape(), brushFill, brushFill, 2);
      idx++;
    }
  }
}

/**
 * Draws shapes stored in curveMaker, a PACurveMaker instance that stores the most recent drawing data. 
 */
public void curveMakerDraw() {
  if (curveMaker.isReady()) {
    curveMaker.brushDraw(this, newBrushColor, newBrushColor, 2);
    // curveMaker.brushDrawDirect(this);
    curveMaker.eventPointsDraw(this);
  }
}

/**
 * Tracks and runs TimedLocation events in the curveTLEvents list.
 * This method is synchronized with a view to future development where it may be called from different threads.
 */
public synchronized void runCurveEvents() {
  // if the event list is null or empty, skip out
  if (curveTLEvents != null && curveTLEvents.size() > 0) {
    int currentTime = millis();
    curveTLEvents.forEach(tl -> {
      if (tl.stopTime() < currentTime) {
        // the curves may exceed display bounds, so we have to constrain values
        sampleX = PixelAudio.constrain(Math.round(tl.getX()), 0, width - 1);
        sampleY = PixelAudio.constrain(Math.round(tl.getY()), 0, height - 1);
        int pos = getSamplePos(sampleX, sampleY);
        playSample(pos, calcSampleLen(), 0.6f);          
        tl.setStale(true);
        // println("----- ");
      } 
      else {
        // pointEventsArray is sorted by time, so ignore events still in the future
        return;
      }
    });
    curveTLEvents.removeIf(TimedLocation::isStale);
  }
}

/**
 * Tracks and runs TimedLocation events in the timeLocsArray list, which is 
 * associated with mouse clicks that trigger audio a the click point.
 */
public synchronized void runPointEvents() {
  int currentTime = millis();
  for (Iterator<TimedLocation> iter = timeLocsArray.iterator(); iter.hasNext();) {
    TimedLocation tl = iter.next();
    tl.setStale(tl.stopTime() < currentTime);
    if (!tl.isStale()) {
      drawCircle(tl.getX(), tl.getY());
    }
  }
  timeLocsArray.removeIf(TimedLocation::isStale);    
}

/**
 * @param poly    a polygon described by an ArrayList of PVector
 * @return        true if the mouse is within the bounds of the polygon, false otherwise
 */
public boolean mouseInPoly(ArrayList<PVector> poly) {
  return PABezShape.pointInPoly(poly, mouseX, mouseY);
}

/**
 * Reinitializes audio and clears event lists. If isClearCurves is true, clears brushShapesList
 * and curveTLEvents.
 * @param isClearCurves
 */
public void reset(boolean isClearCurves) {
  initAudio();
  if (audioFile != null)
    loadAudioFile(audioFile);
  if (this.curveMaker != null) this.curveMaker = null;
  if (this.eventPoints != null) this.eventPoints.clear();
  this.activeIndex = 0;
  if (isClearCurves) {
    if (this.brushShapesList != null) this.brushShapesList.clear();
    if (this.curveTLEvents != null) this.curveTLEvents.clear();
    println("----->>> RESET audio, event points and curves <<<------");
  }
  else {
    println("----->>> RESET audio and event points <<<------");
  }
}

/**
 * Plays all audio events controlled by PACurveMaker curves in brushShapesList, 
 * spaced out by offset milliseconds.
 * @param offset
 */
public void playBrushstrokes(int offset) {
  int startTime = millis() + 50;
  for (PACurveMaker curve : brushShapesList) {
    if (curve.isReady()) {
      eventPoints = curve.getCurveShape().getPointList(polySteps);
      loadEventPoints(startTime);
      startTime += offset;
    }
  }
}

/**
 * Removes the current active PACurveMaker instance, flagged by a highlighted brush stroke,
 * from brushShapesList, if there is one.
 */
public void removeActiveBrush() {
  if (brushShapesList != null) {
    // remove the active (highlighted) brush
    if (!brushShapesList.isEmpty()) {
      int idx = brushShapesList.indexOf(activeBrush);
      brushShapesList.remove(activeBrush);
      if (brushShapesList.size() == idx)
        curveMaker = null;
      // println("-->> removed activeBrush");
    }
  }
}

/**
 * Removes the newest PACurveMaker instance, shown as a brush stroke
 * in the display, from brushShapesList.
 */
public void removeNewestBrush() {
  if (brushShapesList != null) {
    // remove the most recent addition
    if (!brushShapesList.isEmpty()) {
      int idx = brushShapesList.size();
      brushShapesList.remove(idx - 1);  // brushShapes array starts at 0
      println("-->> removed newest brush");
      curveMaker = null;
    }
  }
}

/**
 * Removes the oldest brush in brushShapesList.
 */
public void removeOldestBrush() {
  if (brushShapesList != null) {
    // remove the oldest addition
    if (!brushShapesList.isEmpty()) {
      brushShapesList.remove(0);    // brushShapes array starts at 0
      if (brushShapesList.isEmpty())
        curveMaker = null;
      }
  }
}
