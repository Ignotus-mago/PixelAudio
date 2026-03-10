/*----------------------------------------------------------------*/
/*                                                                */
/*                        DRAWING METHODS                         */
/*                                                                */
/*----------------------------------------------------------------*/

/**
 * Utility method for applying hue and saturation values from a source array of RGB values
 * to the brightness values in a target array of RGB values, using a lookup table to redirect indexing.
 *
 * @param colorSource    a source array of RGB data from which to obtain hue and saturation values
 * @param graySource     an target array of RGB data from which to obtain brightness values
 * @param lut            a lookup table, must be the same size as colorSource and graySource
 * @return the graySource array of RGB values, with hue and saturation values changed
 * @throws IllegalArgumentException if array arguments are null or if they are not the same length
 */
public int[] applyColor(int[] colorSource, int[] graySource, int[] lut) {
  if (colorSource == null || graySource == null || lut == null)
    throw new IllegalArgumentException("colorSource, graySource and lut cannot be null.");
  if (colorSource.length != graySource.length || colorSource.length != lut.length)
    throw new IllegalArgumentException("colorSource, graySource and lut must all have the same length.");
  // initialize a reusable array for HSB color data -- this is a way to speed up the applyColor() method
  float[] hsbPixel = new float[3];
  for (int i = 0; i < graySource.length; i++) {
    graySource[i] = PixelAudioMapper.applyColor(colorSource[lut[i]], graySource[i], hsbPixel);
  }
  return graySource;
}

/**
 * @param colorSource    a source array of RGB data from which to obtain hue and saturation values
 * @param graySource     an target array of RGB data from which to obtain brightness values
 * @param lut            a lookup table, must be the same size as colorSource and graySource
 * @param shift          pixel shift from array rotation, windowed buffer, etc.
 * @return the graySource array of RGB values, with hue and saturation values changed
 * @throws IllegalArgumentException if array arguments are null or if they are not the same length
 */
public int[] applyColorShifted(int[] colorSource, int[] graySource, int[] lut, int shift) {
  if (colorSource == null || graySource == null || lut == null)
    throw new IllegalArgumentException("colorSource, graySource and lut cannot be null.");
  if (colorSource.length != graySource.length || colorSource.length != lut.length)
    throw new IllegalArgumentException("colorSource, graySource and lut must all have the same length.");
  int n = graySource.length;
  int s = ((shift % n) + n) % n; // wrap + allow negative shifts
  float[] hsbPixel = new float[3];
  for (int i = 0; i < n; i++) {
    int srcIdx = lut[i] + s;
    if (srcIdx >= n) srcIdx -= n; // faster than % in tight loop
    graySource[i] = PixelAudioMapper.applyColor(colorSource[srcIdx], graySource[i], hsbPixel);
  }
  return graySource;
}

/**
 * Applies the Hue and Saturation of pixel values in the colors[] array to mapImage and baseImage.
 *
 */
public void applyColorMap() {
  mapImage.loadPixels();
  applyColorShifted(colors, mapImage.pixels, mapper.getImageToSignalLUT(), totalShift);
  mapImage.updatePixels();
  baseImage.loadPixels();
  applyColor(colors, baseImage.pixels, mapper.getImageToSignalLUT());
  baseImage.updatePixels();
}

/**
 * Initializes allPoints and adds the current mouse location to it.
 */
public void initAllPoints() {
  allPoints = new ArrayList<PVector>();
  allTimes = new ArrayList<Integer>();
  startTime = millis();
  addPoint(clipToWidth(mouseX), clipToHeight(mouseY));
}

/**
 * Respond to mousePressed events, usually by triggering an event
 */
public void handleClickOutsideBrush(int x, int y) {
  if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) {
    runSamplerPointEvent(x, y);
    return;
  } else if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR) {
    runGranularPointEvent(x, y);
    return;
  } else if (drawingMode == DrawingMode.PLAY_ONLY) {
    // In play-only, play what user clicked/hovered, otherwise fall back to selected
    if (pointEventUseSampler) {
      runSamplerPointEvent(x, y);
      return;
    } else {
      runGranularPointEvent(x, y);
      return;
    }
  }
}

// NOT USED
boolean isOverAnyBrush(int x, int y) {
  // optionally gate by mode; or check both lists always
  for (int i = granularBrushes.size() - 1; i >= 0; i--) {
    AudioBrush b = granularBrushes.get(i);
    if (pointInPoly(b.curve().getBrushPoly(), x, y)) return true;
  }
  for (int i = samplerBrushes.size() - 1; i >= 0; i--) {
    AudioBrush b = samplerBrushes.get(i);
    if (pointInPoly(b.curve().getBrushPoly(), x, y)) return true;
  }
  return false;
}

/**
 * While user is dragging the mouse and mode == Mode.DRAW_EDIT_GRANULAR or DRAW_EDIT_SAMPLER,
 * accumulates new points to allPoints and event times to allTimes. Coordinates should be
 * constrained to display window bounds.
 */
public void addPoint(int x, int y) {
  // we do some very basic point thinning to eliminate successive duplicate points
  if (x != currentPoint.x || y != currentPoint.y) {
    currentPoint = new PVector(x, y);
    allPoints.add(currentPoint);
    allTimes.add(millis() - startTime);   // store time offset, not absolute time, in allTimes
  }
}

/**
 * @param x    a value to constrain to the current window width
 * @return the constrained value
 */
public int clipToWidth(int x) {
  return min(max(0, x), width - 1);
}
/**
 * @param y    a value to constrain to the current window height
 * @return the constrained value
 */
public int clipToHeight(int y) {
  return min(max(0, y), height - 1);
}

public PVector jitterCoord(int x, int y, int deviationPx) {
  double variance = deviationPx * deviationPx;
  int jx = (int)Math.round(PixelAudio.gauss(0, variance));
  int jy = (int)Math.round(PixelAudio.gauss(0, variance));
  int nx = clipToWidth(x + jx);
  int ny = clipToHeight(y + jy);
  return new PVector(nx, ny);
}

float[] generateJitterPitch(int length, float deviationPitch) {
  float[] pitch = new float[length];
  double variance = deviationPitch * deviationPitch;
  for (int i = 0; i < pitch.length; i++) {
    pitch[i] = (float) PixelAudio.gauss(1, variance);
  }
  return pitch;
}

public GestureSchedule loadGestureSchedule(PACurveMaker brush, GestureGranularConfig snap) {
  if (brush == null || snap == null) return null;
  GestureSchedule schedule = scheduleBuilder.build(brush, snap, audioOut.sampleRate());
  if (schedule == null || schedule.isEmpty()) return null;
  // we could set a global variable, this.currentSchedule
  return schedule;
}

/**
 * Initializes a PACurveMaker instance with allPoints as an argument to the factory method
 * PACurveMaker.buildCurveMaker() and then fills in PACurveMaker instance variables.
 */
public AudioBrush initCurveMakerAndAddBrush() {
  if (gConfig == null) {
    throw new IllegalStateException("gConfig is null: you probably need to initialize it.");
  }
  curveMaker = PACurveMaker.buildCurveMaker(allPoints, allTimes, startTime);
  curveMaker.setBrushColor(readyBrushColor1);
  curveMaker.setActiveBrushColor(hoverBrushColor1);
  curveMaker.setEpsilon(epsilon);                   // control resolution of reduced points
  curveMaker.setTimeOffset(millis() - startTime);   // time between first point and last point
  curveMaker.calculateDerivedPoints();              // initialize all the useful structures up front
  // reset some fields in gConfig
  gConfig.resampleCount = 0;
  gConfig.targetDurationMs = 0;
  gConfig.pitchSemitones = 0.0f;
  if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) {             // TODO implement complete logic for mode, if mode is PLAY we should be here...
    SamplerBrush sb = new SamplerBrush(curveMaker, gConfig.copy());
    this.samplerBrushes.add(sb);                   // add new brush to sampler brush list
    setActiveBrush(sb, samplerBrushes.size() - 1);
    if (doPlayOnDraw) {
      GestureGranularConfig snap = gConfig.build();
      loadGestureSchedule(sb.curve(), snap);
    }
    return sb;
  } else {
    gConfig.warpShape = GestureGranularConfig.WarpShape.LINEAR;
    GestureGranularConfig.Builder cfg = gConfig.copy();
    cfg.curveSteps = Math.min(cfg.curveSteps, 32);     // simplest way to set curveSteps
    cfg.env = envPreset("Fade");    // TODO calculate optimal envelope
    GranularBrush gb = new GranularBrush(curveMaker, cfg);
    granularBrushes.add(gb);                           // add new brush to granular brush list
    setActiveBrush(gb, granularBrushes.size() - 1);
    if (doPlayOnDraw) {
      GestureGranularConfig snap = gConfig.build();
      loadGestureSchedule(gb.curve(), snap);
    }
    if (isVerbose) println("----- new granular brush created");
    return gb;
  }
}

boolean isBrushInteractable(AudioBrush b) {
  switch (drawingMode) {
  case DRAW_EDIT_SAMPLER:
    return b instanceof SamplerBrush;
  case DRAW_EDIT_GRANULAR:
    return b instanceof GranularBrush;
  case PLAY_ONLY:
    return true; // both playable
  default:
    return false;
  }
}

void setActiveBrush(AudioBrush brush) {
  setActiveBrush(brush, hoverIndex);
}

void setActiveBrush(AudioBrush brush, int idx) {
  if (brush == null) return;
  if (brush instanceof GranularBrush) {
    GranularBrush gb = (GranularBrush) brush;
    activeBrush = gb;
    activeGranularBrush = gb;
    activeGranularIndex = idx;
    activeSamplerBrush = null;
    activeSamplerIndex = -1;
  } 
  else if (brush instanceof SamplerBrush) {
    SamplerBrush sb = (SamplerBrush) brush;
    activeBrush = sb;
    activeSamplerBrush = sb;
    activeSamplerIndex = idx;
    activeGranularBrush = null;
    activeGranularIndex = -1;
  }
  gConfig = brush.cfg();
  recomputeUIBaselinesFromActiveBrush();
  syncGuiFromConfig();
}

void recomputeUIBaselinesFromActiveBrush() {
  if (activeBrush == null) {
    baselineCount = 0;
    baselineDurationMs = 0;
    return;
  }
  // 1) Count: based on the point set implied by current PathMode
  PACurveMaker curve = activeBrush.curve();
  List<PVector> pts;
  switch (gConfig.pathMode) {
  case REDUCED_POINTS:
    pts = curve.getReducedPoints();
    break;
  case CURVE_POINTS:
    pts = curve.getCurvePoints();
    break;
  case ALL_POINTS:
  default:
    pts = curve.getAllPoints();
    break;
  }
  baselineCount = (pts == null) ? 0 : pts.size();
  // 2) Duration: depends on HopMode (baseline has resample/duration/warp off)
  if (baselineCount <= 1) {
    baselineDurationMs = 0;
    return;
  }
  if (gConfig.hopMode == GestureGranularConfig.HopMode.FIXED) {
    // Baseline FIXED duration = hop * (n - 1), excludes last grain length by design
    int hop = Math.max(1, gConfig.hopLengthSamples);
    double hopMs = 1000.0 * hop / audioOut.sampleRate();
    baselineDurationMs = (int) Math.round((baselineCount - 1) * hopMs);
  } else {
    // Gesture-timed baseline duration: use the curve's own recorded timing if available.
    // PACurveMaker already tracks "timeOffset" between first and last point.
    baselineDurationMs = Math.max(0, curve.getTimeOffset()); // int ms (adjust if getter differs)
  }
  if (isVerbose) {
    println("-- baseLineCount = "+ baselineCount +"; baseLineDurationMs = "+ baselineDurationMs);
  }
}


/**
 * Iterates over brushShapesList and draws the brushstrokes stored in
 * each PACurveMaker in the list.
 */
public void drawBrushShapes() {
  drawBrushes(granularBrushes, readyBrushColor1, hoverBrushColor1, selectedBrushColor1);
  drawBrushes(samplerBrushes, readyBrushColor2, hoverBrushColor2, selectedBrushColor2);
}

public void drawBrushes(List<? extends AudioBrush> brushes, int readyColor, int hoverColor, int selectedColor) {
  if (brushes.isEmpty()) return;
  for (int i = 0; i < brushes.size(); i++) {
    AudioBrush b = brushes.get(i);
    PACurveMaker brush = b.curve();
    boolean isHover = (b == hoverBrush);
    boolean isSelected = (b == activeBrush);
    int fill = readyColor;
    if (isSelected) {
      fill = selectedColor;
      brush.setEpsilon(b.cfg().rdpEpsilon);
      brush.setCurveSteps(b.cfg().curveSteps);
    } else if (isHover) {
      fill = hoverColor;
    }
    GestureGranularConfig.PathMode pm = b.cfg().pathMode;
    PACurveUtility.shapeDraw(this, brush.getBrushShape(), fill, fill, 2);
    int w = 1, d = 5;
    int lc = isSelected ? lineColor : dimLineColor;
    int cc = isSelected ? circleColor : dimCircleColor;
    switch (pm) {
      case REDUCED_POINTS:
        PACurveUtility.lineDraw(this, brush.getReducedPoints(), lc, w);
      PACurveUtility.pointsDraw(this, brush.getReducedPoints(), cc, d);
      break;
    case CURVE_POINTS:
      PACurveUtility.lineDraw(this, brush.getCurvePoints(), lc, w);
      PACurveUtility.pointsDraw(this, brush.getCurvePoints(), cc, d);
      break;
    case ALL_POINTS:
      PACurveUtility.lineDraw(this, brush.getAllPoints(), lc, w);
      PACurveUtility.pointsDraw(this, brush.getAllPoints(), cc, d);
      break;
    default:
      {
        PACurveUtility.lineDraw(this, brush.getAllPoints(), lc, w);
        PACurveUtility.pointsDraw(this, brush.getAllPoints(), cc, d);
      }
    }
  }
  if (activeBrush == null) {
    currentGranStatus = "Draw or select a brushstroke.";
  }
}

ArrayList<PVector> getPathPoints(AudioBrush b) {
  PACurveMaker cm = b.curve();
  switch (b.cfg().pathMode) {
  case ALL_POINTS:
    return cm.getAllPoints();
  case REDUCED_POINTS:
    return cm.getReducedPoints();
  case CURVE_POINTS:
    return cm.getCurvePoints();
  default:
    return cm.getAllPoints();
  }
}

/**
 * @param b    an AudioBrushLite instance
 * @return     GestureSchedule for the current pathMode of the brush
 */
public GestureSchedule getScheduleForBrush(AudioBrush b) {
  switch (b.cfg().pathMode) {
  case REDUCED_POINTS:
    return b.curve().getReducedSchedule(b.cfg().rdpEpsilon);
  case CURVE_POINTS:
    return b.curve().getCurveSchedule(b.cfg().rdpEpsilon, curveSteps, isAnimating);
  case ALL_POINTS:
  default:
    return b.curve().getAllPointsSchedule();
  }
}

/*             END DRAWING METHODS              */

/**
 * @param poly    a polygon described by an ArrayList of PVector
 * @return        true if the mouse is within the bounds of the polygon, false otherwise
 */
public boolean mouseInPoly(ArrayList<PVector> poly) {
  return pointInPoly(poly, mouseX, mouseY);
}

/**
 * @param poly    a polygon described by an ArrayList of PVector
 * @param x       x-coordinate
 * @param y       y-coordinate
 * @return        true if the mouse is within the bounds of the polygon, false otherwise
 */
public boolean pointInPoly(ArrayList<PVector> poly, int x, int y) {
  return PABezShape.pointInPoly(poly, x, y);
}

/**
 * Reinitializes audio and clears event lists. If isClearCurves is true, clears brushShapesList.
 * @param isClearCurves
 */
public void reset(boolean isClearCurves) {
  // note that initAudio also clears TimedLocation event lists
  initAudio();
  if (audioFile != null)
    loadAudioFile(audioFile);
  if (this.curveMaker != null) this.curveMaker = null;
  this.activeSamplerIndex = 0;
  if (isClearCurves) {
    if (this.samplerBrushes != null) this.samplerBrushes.clear();
    if (this.granularBrushes != null) this.granularBrushes.clear();
    println("----->>> RESET audio, event points and brushes <<<------");
  } else {
    println("----->>> RESET audio and event points <<<------");
  }
}
