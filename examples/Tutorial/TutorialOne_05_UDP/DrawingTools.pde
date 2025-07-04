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
  addPoint();
  sampleX = mouseX;
  sampleY = mouseY;
  samplePos = mapper.lookupSample(sampleX, sampleY);
  // if (nd != null) nd.oscSendMousePressed(sampleX, sampleY, samplePos);
}

/**
 * Responds to mousePressed events associated with drawing.
 */
public void handleMousePressed() {
  if (activeBrush != null) {
    // a brushShape was triggered
    eventPoints = activeBrush.getEventPoints();
    playPoints();
    // if (nd != null) nd.oscSendTrig(activeIndex + 1);
    activeBrush = null;
  } 
  else {
    sampleX = mouseX;
    sampleY = mouseY;
    samplePos = mapper.lookupSample(sampleX, sampleY);
    // handle audio generation in response to a mouse click
    audioMousePressed(sampleX, sampleY);
    // if (nd != null) nd.oscSendMousePressed(sampleX, sampleY, samplePos);
  }
}

/**
 * While user is dragging the mouses and isDrawMode == true, accumulates new points
 * to allPoints and event times to allTimes.
 */
public void addPoint() {
  // we do some very basic point thinning to eliminate successive duplicate points
  if (mouseX != currentPoint.x || mouseY != currentPoint.y) {
    currentPoint = new PVector(mouseX, mouseY);
    allPoints.add(currentPoint);
    allTimes.add(millis());
    sampleX = mouseX;
    sampleY = mouseY;
    samplePos = mapper.lookupSample(sampleX, sampleY);      
    // if (nd != null) nd.oscSendMousePressed(sampleX, sampleY, samplePos);
  }
}

/**
 * Processes the eventPoints list to create TimedLocation events 
 * and stores them in curveTLEvents. 
 */
public void playPoints() {
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
  playPoints();
  curveMaker.setDragTimes(reconfigureTimeList(allTimes));
  this.brushShapesList.add(curveMaker);
  sampleX = mouseX;
  sampleY = mouseY;
  samplePos = mapper.lookupSample(sampleX, sampleY);
  //if (nd != null) {
  //  nd.oscSendMousePressed(sampleX, sampleY, samplePos);
  //  nd.oscSendDrawPoints(curveMaker.getRdpPoints());
  //  nd.oscSendTimeStamp(curveMaker.timeStamp, curveMaker.timeOffset);
  //}
}

public int[] reconfigureTimeList(int[] timeList) {
  int startTime = timeList[0];
  int[] timeOffsetsList = new int[timeList.length];
  timeOffsetsList[0] = startTime; // first item is the original event time
  for (int i = 1; i < timeList.length; i++) {
    timeOffsetsList[i] = timeList[i] - startTime;
  }
  return timeOffsetsList;
}

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
    for (PACurveMaker bd : brushShapesList) {
      int brushFill = readyBrushColor;
      if (mouseInPoly(bd.getBrushPoly())) {
        brushFill = activeBrushColor;
        activeBrush = bd;
        activeIndex = idx;
      }
      PACurveUtility.shapeDraw(this, bd.getBrushShape(), brushFill, brushFill, 2);
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
        int pos = mapper.lookupSample(sampleX, sampleY);
        playSample(playBuffer, pos, calcSampleLen(), 0.6f, new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime));
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

public boolean mouseInPoly(ArrayList<PVector> poly) {
  return PABezShape.pointInPoly(poly, mouseX, mouseY);
}

public void reset(boolean isClearCurves) {
  initAudio();
  if (this.curveMaker != null) this.curveMaker = null;
  if (this.eventPoints != null) this.eventPoints.clear();
  this.activeIndex = 0;
  if (isClearCurves) {
    if (this.brushShapesList != null) this.brushShapesList.clear();
    if (this.curveTLEvents != null) this.curveTLEvents.clear();
    // if (nd != null) nd.oscSendClear();
    // if (nd != null) nd.setDrawCount(0);
    println("----->>> RESET audio + curves <<<------");
  }
  else {
    println("----->>> RESET audio <<<------");
  }
}

/*             END DRAWING METHODS              */
