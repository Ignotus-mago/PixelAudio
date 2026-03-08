/*----------------------------------------------------------------*/
/*                                                                */
/*                    BEGIN DRAWING METHODS                       */
/*                                                                */
/*----------------------------------------------------------------*/

/**
 * Initializes allPoints and adds the current mouse location to it.
 */
public void initAllPoints() {
  allPoints = new ArrayList<>();
  allTimes = new ArrayList<>();
  startTime = millis();
  addPoint(PApplet.constrain(mouseX, 0, width - 1), PApplet.constrain(mouseY, 0, height - 1));
}

/**
 * While user is dragging the mouses and isDrawMode == true, accumulates new points
 * to allPoints and event times to allTimes. Sets sampleX, sampleY and samplePos variables.
 * We constrain points outside the bounds of the display window. An alternative approach
 * is be to ignore them (isIgnoreOutsideBounds == true), which may give a more "natural"
 * appearance for fast drawing.
 */
public void addPoint(int x, int y) {
  if (x != currentPoint.x || y != currentPoint.y) {
    currentPoint = new PVector(clipToWidth(x), clipToHeight(y));
    allPoints.add(currentPoint);
    allTimes.add(millis() - startTime);
  }
}

/**
 * Clips parameter i to the interval (0..width-1)
 * @param i
 * @return
 */
public int clipToWidth(int i) {
  return min(max(0, i), width - 1);
}
/**
 * Clips parameter i to the interval (0..width-1)
 * @param i
 * @return
 */
public int clipToHeight(int i) {
  return min(max(0, i), height - 1);
}

/**
 * @param x              x-coordinate
 * @param y              y-coordinate
 * @param deviationPx    distance deviation from mean
 * @return               a PVector with coordinates shifted by a Gaussing variable
 */
public PVector jitterCoord(int x, int y, int deviationPx) {
  double variance = deviationPx * deviationPx;
  int jx = (int)Math.round(PixelAudio.gauss(0, variance));
  int jy = (int)Math.round(PixelAudio.gauss(0, variance));
  int nx = clipToWidth(x + jx);
  int ny = clipToHeight(y + jy);
  return new PVector(nx, ny);
}


/**
 * Generates an array of Gaussian values for shifting pitch, where 1.0 = no shift.
 * @param length            length of the returned array
 * @param deviationPitch    expected average deviation of the pitch
 * @return                  and array of Gaussian values centered on 1.0
 */
float[] generateJitterPitch(int length, float deviationPitch) {
  float[] pitch = new float[length];
  double variance = deviationPitch * deviationPitch;
  for (int i = 0; i < pitch.length; i++) {
    pitch[i] = (float) PixelAudio.gauss(1, variance);
  }
  return pitch;
}

/**
 * Initializes a PACurveMaker instance with allPoints as an argument to the factory method
 * PACurveMaker.buildCurveMaker() and then fills in PACurveMaker instance variables from
 * variables in the calling class (TutorialOneDrawing, here).
 */
public void initCurveMakerAndAddBrush() {
  curveMaker = PACurveMaker.buildCurveMaker(allPoints, allTimes, startTime);
  // configure from globals
  curveMaker.setEpsilon(epsilon);
  curveMaker.setCurveSteps(curveSteps);
  curveMaker.setTimeOffset(millis() - startTime);
  curveMaker.calculateDerivedPoints();    // load all internal point lists
  // Optional: set PACurveMaker brush colors if you want them retained internally
  curveMaker.setBrushColor(readyBrushColor);
  curveMaker.setActiveBrushColor(selectedBrushColor);
  // Create a forward-compatible config object
  BrushConfig cfg = new BrushConfig();
  cfg.rdpEpsilon = epsilon;
  cfg.curveSteps = curveSteps;
  cfg.pathMode = PathMode.ALL_POINTS; // tutorial default
  // Default output for newly drawn strokes: SAMPLER (tutorial-centric)
  AudioBrushLite b = new AudioBrushLite(curveMaker, cfg, BrushOutput.SAMPLER, HopMode.GESTURE);
  b.cfg.pathMode = defaultPathModeFor(b.output());
  brushes.add(b);
  // Optionally auto-select the new brush
  activeBrush = b;
}

/**
 * Determines the path mode for a particular BrushOutput.
 * @param out    a BrushOutput (SAMPLER of GRANULAR)
 * @return       a PathMode (ALL_POINTS for Sampler instruments, CURVE_POINTS for Granular instruments)
 */
PathMode defaultPathModeFor(BrushOutput out) {
  return (out == BrushOutput.SAMPLER) ? PathMode.ALL_POINTS : PathMode.CURVE_POINTS;
}

/**
 * Enrty point for drawing brushstrokes on the screen.
 */
public void drawBrushShapes() {
  if (brushes == null || brushes.isEmpty()) return;
  drawBrushes(brushes, readyBrushColor1, hoverBrushColor1, selectedBrushColor1);
}

/**
 * Draw brushstrokes on the display image.
 *
 * @param list             a list of all the brushstrokes (AudioBrushLite)
 * @param readyColor       color for a selectable brush
 * @param hoverColor       color for a brush when the mouse hovers over it
 * @param selectedColor    color for a selected brush (click or spacebar selects)
 */
public void drawBrushes(List<AudioBrushLite> list, int readyColor, int hoverColor, int selectedColor) {
  // step through the list of all brushes
  for (int i = 0; i < list.size(); i++) {
    AudioBrushLite b = list.get(i);
    PACurveMaker cm = b.curve();
    boolean isHover = (b == hoverBrush);
    boolean isSelected = (b == activeBrush);
    int fill = readyColor;
    if (isSelected) {
      fill = selectedColor;
      // keep selected brush geometry consistent with cfg
      cm.setEpsilon(b.cfg().rdpEpsilon);
      cm.setCurveSteps(b.cfg().curveSteps);
      cm.calculateDerivedPoints();
    } else if (isHover) {
      fill = hoverColor;
    }
    // brush body
    PACurveUtility.shapeDraw(this, cm.getBrushShape(), fill, fill, 2);
    // overlay points/lines depending on PathMode
    int w = 1, d = 5;
    int lc = isSelected ? lineColor : dimLineColor;
    int cc = isSelected ? circleColor : dimCircleColor;
    // selected the appropriate point set for drawing
    switch (b.cfg().pathMode) {
    case REDUCED_POINTS:
      PACurveUtility.lineDraw(this, cm.getReducedPoints(), lc, w);
      PACurveUtility.pointsDraw(this, cm.getReducedPoints(), cc, d);
      break;

    case CURVE_POINTS:
      PACurveUtility.lineDraw(this, cm.getCurvePoints(), lc, w);
      PACurveUtility.pointsDraw(this, cm.getCurvePoints(), cc, d);
      break;

    case ALL_POINTS:
      PACurveUtility.lineDraw(this, cm.getAllPoints(), lc, w);
      PACurveUtility.pointsDraw(this, cm.getAllPoints(), cc, d);
      break;
    }
  }
}

/**
 * Sets epsilon value for the PACurveMaker associated with an AudioBrushLite instance.
 *
 * @param b    an AudioBrushLite instance
 * @param e    desired epsilon value to control point reduction
 */
public void setBrushEpsilon(AudioBrushLite b, float e) {
  PACurveMaker cm = b.curve();
  BrushConfig cfg = b.cfg();
  cfg.rdpEpsilon = e;
  cm.setEpsilon(e);
  cm.calculateDerivedPoints();
}

/**
 * Get the path points of a brushstroke, with the representation determined by the BrushConfig's path mode.
 *
 * @param b    an AudioBrushLite instance
 * @return     an all points, reduced, or curve representation of the path points of an AudioBrushLite instance
 */
ArrayList<PVector> getPathPoints(AudioBrushLite b) {
  PACurveMaker cm = b.curve();
  switch (b.cfg().pathMode) {
    case ALL_POINTS:    return cm.getAllPoints();
    case REDUCED_POINTS:return cm.getReducedPoints();
    case CURVE_POINTS:  return cm.getCurvePoints();
    default: throw new IllegalStateException();
  }
}

/**
 * Get a GestureSchedule (points + timing) for an AudioBrushLite instance.
 * @param b    an AudioBrushLite instance
 * @return     GestureSchedule for the current pathMode of the brush
 */
public GestureSchedule getScheduleForBrush(AudioBrushLite b) {
  GestureSchedule sched;
  switch (b.cfg().pathMode) {
    case REDUCED_POINTS:
      sched = b.curve.getReducedSchedule(b.cfg.rdpEpsilon);
      break;
    case CURVE_POINTS:
      sched = b.curve.getCurveSchedule(b.cfg.rdpEpsilon, curveSteps, isAnimating);
      break; 
    case ALL_POINTS:
    default:
      sched = b.curve.getAllPointsSchedule();
      break;
  }
  return sched;
}

/**
 * Schedule a Sampler brush audio / animation event.
 *
 * @param b    an AudioBrushLite instance
 */
void scheduleSamplerBrushClick(AudioBrushLite b) {
  if (b == null) return;
  ArrayList<PVector> pts = getPathPoints(b);
  if (pts == null || pts.size() < 2) return;
  GestureSchedule sched = getScheduleForBrush(b);
  storeSamplerCurveTL(sched, millis() + 10);
}

/**
 * Store scheduled sampler synth / animation events for future activation.
 * @param sched        a GestureSchedule (points + timing for a brush)
 * @param startTime    time to start a series of events
 */
public synchronized void storeSamplerCurveTL(GestureSchedule sched, int startTime) {
  if (this.samplerTimeLocs == null) samplerTimeLocs = new ArrayList<>();
  int i = 0;
  startTime = millis() + 5;
  // we store the point and the current time + time offset, where timesMs[0] == 0
  for (PVector loc : sched.points) {
    int x = Math.round(loc.x);
    int y = Math.round(loc.y);
    int t = startTime + Math.round(sched.timesMs[i++]);
    int d = 200;
    this.samplerTimeLocs.add(new TimedLocation(x, y, t, d));
  }
  Collections.sort(samplerTimeLocs);
}

/**
 * Execute audio / animation events for Sampler brushstrokes.
 */
public synchronized void runSamplerBrushEvents() {
  if (samplerTimeLocs == null || samplerTimeLocs.isEmpty()) return;
  int currentTime = millis();
  samplerTimeLocs.forEach(tl -> {
    if (tl.eventTime() < currentTime) {
      int sampleX = PixelAudio.constrain(Math.round(tl.getX()), 0, width - 1);
      int sampleY = PixelAudio.constrain(Math.round(tl.getY()), 0, height - 1);
      float panning = map(sampleX, 0, width, -0.8f, 0.8f);
      int pos = getSamplePos(sampleX, sampleY);
      playSample(pos, calcSampleLen(), synthGain, panning);
      pointTimeLocs.add(new TimedLocation(sampleX, sampleY, tl.getDurationMs() + millis()));
      tl.setStale(true);
    } else {
      return;
    }
  }
  );
  samplerTimeLocs.removeIf(TimedLocation::isStale);
}

/**
 * Schedule a Granular brush audio / animation event.
 *
 * @param b    an AudioBrushLite instance
 */
void scheduleGranularBrushClick(AudioBrushLite b) {
  if (b == null) return;
  ArrayList<PVector> pts = getPathPoints(b);
  if (pts == null || pts.size() < 2) return;
  ensureGranularReady();
  float[] buf = (granSignal != null) ? granSignal : audioSignal;
  boolean isGesture = (b.hopMode() == HopMode.GESTURE);
  GestureGranularParams gParams = isGesture ? gParamsGesture : gParamsFixed;
  GestureSchedule sched = getScheduleForBrush(b);
  playGranularGesture(buf, sched, gParams);
  storeGranularCurveTL(sched, millis() + 10, isGesture);
}

/**
 * Store scheduled granular synth / animation events for future activation.
 * @param sched        a GestureSchedule (points + timing for a brush)
 * @param startTime    time to start a series of events
 * @param isGesture    is the schedule for a GESTURE or FIXED granular event (ignored)
 */
public synchronized void storeGranularCurveTL(GestureSchedule sched, int startTime, boolean isGesture) {
  if (this.grainTimeLocs == null) grainTimeLocs = new ArrayList<>();
  int i = 0;
  int hopMs = (int) Math.round(AudioUtility.samplesToMillis(hopSamples, sampleRate));
  int durMsFixed = (int) Math.round(AudioUtility.samplesToMillis(granSamples, sampleRate)); // or hopMs if you prefer
  // we store the point and the current time + time offset, where timesMs[0] == 0
  for (PVector loc : sched.points) {
    int x = Math.round(loc.x);
    int y = Math.round(loc.y);
    // we can rely on sched for accurate times -- TODO drop in next iteration
    // int t = (isGesture) ? startTime + Math.round(sched.timesMs[i++]) : startTime + i++ * hopMs;
    // int d = (isGesture) ? 200 : durMsFixed;
    int t = startTime + Math.round(sched.timesMs[i++]);
    int d = 200;
    this.grainTimeLocs.add(new TimedLocation(x, y, t, d));
  }
  Collections.sort(grainTimeLocs);
}

/**
 * Tracks and runs TimedLocation events in the grainLocsArray list, which is
 * associated with granular synthesis gestures.
 */
public synchronized void runGrainEvents() {
  if (grainTimeLocs == null || grainTimeLocs.isEmpty()) return;
  int t = millis();
  for (Iterator<TimedLocation> iter = grainTimeLocs.iterator(); iter.hasNext(); ) {
    TimedLocation tl = iter.next();
    int low = tl.eventTime();
    int high = (tl.eventTime() + tl.getDurationMs());
    if (t >= low && t < high) { // event in the interval between low and high
      drawCircle(tl.getX(), tl.getY());
    } else {
      if (t >= high) {        // event in the past
        tl.setStale(true);
        iter.remove();
      }
      if (t < low) {          // event in the future
        break;
      }
    }
  }
  // grainLocsArray.removeIf(TimedLocation::isStale);    // not necessary if we remove in loop
}

/**
 * Tracks and runs TimedLocation events in the timeLocsArray list, which is
 * associated with mouse clicks that trigger audio a the click point.
 */
public synchronized void runPointEvents() {
  int currentTime = millis();
  for (Iterator<TimedLocation> iter = pointTimeLocs.iterator(); iter.hasNext(); ) {
    TimedLocation tl = iter.next();
    tl.setStale(tl.eventTime() < currentTime);
    if (!tl.isStale()) {
      drawCircle(tl.getX(), tl.getY());
    }
  }
  pointTimeLocs.removeIf(TimedLocation::isStale);
}


/**
 * @param poly    a polygon described by an ArrayList of PVector
 * @return        true if the mouse is within the bounds of the polygon, false otherwise
 */
public boolean mouseInPoly(ArrayList<PVector> poly) {
  return PABezShape.pointInPoly(poly, mouseX, mouseY);
}


/**
 * Reinitializes audio and clears event lists.
 * -- TODO drop, this used to be the "emergency off" switch for runaway audio processing
 */
@Deprecated
  public void reset() {
}


/**
 * Removes the current active AudioBrushLite instance.
 */
public void removeHoveredOrOldestBrush() {
  if (brushes == null || brushes.isEmpty()) return;
  if (hoverBrush != null) {
    brushes.remove(hoverBrush);
    if (activeBrush == hoverBrush) activeBrush = null;
    hoverBrush = null;
    hoverIndex = -1;
  } else {
    brushes.remove(0);
  }
}

/**
 * Removes the most recent AudioBrushLite instance.
 */
public void removeNewestBrush() {
  if (brushes == null || brushes.isEmpty()) return;
  AudioBrushLite removed = brushes.remove(brushes.size() - 1);
  if (activeBrush == removed) activeBrush = null;
  if (hoverBrush == removed) {
    hoverBrush = null;
    hoverIndex = -1;
  }
}

/*             END DRAWING METHODS              */
