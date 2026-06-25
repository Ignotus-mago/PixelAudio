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
  int x = clipToWidth(mouseX);
  int y = clipToHeight(mouseY);
  addDrawingPoint(x, y);
}

/**
 * Respond to mousePressed events, usually by triggering an event
 */
public int handleClickOutsideBrush(int x, int y) {
  int pos = getSamplePos(x, y);
  if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) {
    runSamplerPointEvent(x, y);
  } else if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR) {
    runGranularPointEvent(x, y);
  } else if (drawingMode == DrawingMode.PLAY_ONLY) {
    // In play-only, play what user clicked/hovered, otherwise fall back to selected
    if (pointEventUseSampler) {
      runSamplerPointEvent(x, y);
    } else {
      runGranularPointEvent(x, y);
    }
  } else {
  }
  // *****]]] NETWORKING [[[***** //
  if (nd != null && isNetSendOutsideBrushPoints) nd.oscSendMouseClicked(x, y, pos);
  return pos;
}

/**
 * Dispatches lightweight "taste" audio while drawing.
 * Call this only after a new drawing point has been accepted.
 */
void handlePlayOnDraw(int x, int y) {
  if (!doPlayWhileDrawing) return;

  int nowMs = millis();
  if (!shouldTriggerDrawTaste(x, y, nowMs)) return;

  switch (drawingMode) {
  case DRAW_EDIT_SAMPLER:
    runSamplerDrawTaste(x, y);
    break;
  case DRAW_EDIT_GRANULAR:
    runGranularDrawTaste(x, y);
    break;
  case PLAY_ONLY:
  default:
    break;
  }
}

/**
 * Shared thinning logic for draw-time tasting.
 * Requires both:
 *   1) enough time since the last taste
 *   2) enough mouse movement since the last taste point
 */
boolean shouldTriggerDrawTaste(int x, int y, int nowMs) {
  if (nowMs - lastDrawTasteMs < drawTasteIntervalMs) return false;

  if (lastDrawTastePoint != null) {
    float dx = x - lastDrawTastePoint.x;
    float dy = y - lastDrawTastePoint.y;
    float distSq = dx * dx + dy * dy;
    float minDistSq = drawTasteMinDist * drawTasteMinDist;
    if (distSq < minDistSq) return false;
  }

  lastDrawTasteMs = nowMs;
  if (lastDrawTastePoint == null) {
    lastDrawTastePoint = new PVector(x, y);
  } else {
    lastDrawTastePoint.set(x, y);
  }
  return true;
}

/**
 * Lightweight Sampler "taste" while drawing.
 * Short, percussive, cheap to trigger.
 */
void runSamplerDrawTaste(int x, int y) {
  ensureSamplerReady();

  int signalPos = mapper.lookupSignalPosShifted(x, y, totalShift);
  float pan = map(x, 0, width, -0.875f, 0.875f);

  // Keep the taste short and light.
  int len = calcSampleLen(envDuration, 1.0f, 0.0625f) / 4;
  ADSRParams env = envPreset("Percussion");

  samplelen = playSample(signalPos, len, 0.125f, env, pan);

  int durationMs = (int) (samplelen / sampleRate * 1000.0f);
  pointTimeLocsAddPoint(new TimedLocation(x, y, millis() + durationMs + 50));

  // if (isVerbose) println("----- sampler draw taste, signalPos = " + signalPos);
}

/**
 * Lightweight Granular "taste" while drawing.
 * Uses a very small fixed-hop burst gesture so the result is textured
 * but inexpensive compared to full gesture playback.
 */
void runGranularDrawTaste(int x, int y) {
  ensureGranularReady();
  if (granSignal == null || gDir == null) return;

  // Build a tiny local gesture around the current point.
  ArrayList<PVector> pts = new ArrayList<>();
  ArrayList<Integer> times = new ArrayList<>();

  // Two-point micro gesture: same point, short duration.
  // This gives the director a valid schedule without pulling in full current-brush logic.
  pts.add(new PVector(x, y));
  pts.add(new PVector(x, y));
  times.add(0);
  times.add(drawTasteDurationMs);

  PACurveMaker cm = PACurveMaker.buildCurveMaker(pts, times, millis());
  GestureSchedule sched = scheduleBuilder.build(cm, defaultGranConfig.build(), audioOut.sampleRate());
  if (sched == null || sched.isEmpty()) return;

  // Limit cost aggressively.
  int n = Math.min(sched.size(), drawTasteMaxGrains);
  if (n <= 0) return;

  // If the schedule builder returned more than we want, trim it.
  if (sched.size() > n) {
    ArrayList<PVector> shortPts = new ArrayList<>(n);
    float[] shortTimes = new float[n];
    for (int i = 0; i < n; i++) {
      shortPts.add(sched.points.get(i));
      shortTimes[i] = sched.timesMs[i];
    }
    sched = new GestureSchedule(shortPts, shortTimes);
  }

  float pan = map(x, 0, width, -0.875f, 0.875f);
  GestureGranularParams params = GestureGranularParams.builder()
    .grainLengthSamples(granSamples)
    .hopLengthSamples(hopSamples)
    .burstGrains(drawTasteBurstGrains)
    .gainLinear(granularPointGain)
    .pan(pan)
    .env(granularEnv)
    .hopMode(GestureGranularParams.HopMode.FIXED)
    .timeTransform(GestureGranularParams.TimeTransform.RAW_GESTURE)
    .build();

  int[] startIndices = mapper.lookupSignalPosArray(sched.points, totalShift, mapper.getSize());

  gDir.playGestureNow(granSignal, sched, params, startIndices);

  // Simple UI dot, parallel to sampler taste.
  pointTimeLocsAddPoint(new TimedLocation(x, y, millis() + drawTasteDurationMs + 50));

  // if (isVerbose) println("----- granular draw taste, grains = " + n + ", x = " + x + ", y = " + y);
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
public void addDrawingPoint(int x, int y) {
  if (allPoints == null || allTimes == null) return;
  // basic point thinning: skip duplicate successive points
  if (x == (int)currentPoint.x && y == (int)currentPoint.y) return;
  currentPoint = new PVector(x, y);
  allPoints.add(currentPoint);
  allTimes.add(millis() - startTime);   // store time offset, not absolute time

  // *****]]] NETWORKING [[[***** //
  if (nd != null && isNetSendDrawingPoints) nd.oscSendMouseClicked(x, y, getSamplePos(x, y));

  if (!doPlayWhileDrawing) return;
  // preview only while drawing in editable synth modes
  if (!(drawingMode == DrawingMode.DRAW_EDIT_GRANULAR
    || drawingMode == DrawingMode.DRAW_EDIT_SAMPLER)) {
    return;
  }
  handlePlayOnDraw(x, y);
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
  curveMaker.setBrushColor(readyGranColor);
  curveMaker.setActiveBrushColor(hoverGranColor);
  curveMaker.setEpsilon(epsilon);                   // control resolution of reduced points
  curveMaker.setTimeOffset(millis() - startTime);   // time between first point and last point
  curveMaker.calculateDerivedPoints();              // initialize all the useful structures up front
  AudioBrush brush = makeBrushFromCurveMaker();
  PAControlCurve gainCurve = isAddDynamics ? dynamics : null;
  if (doPlayOnNewBrush && brush instanceof SamplerBrush) {
    SamplerBrush sb = (SamplerBrush) brush;
    scheduleSamplerBrushClick(sb, clipToWidth(mouseX), clipToHeight(mouseY), gainCurve);
  }
  if (doPlayOnNewBrush && brush instanceof GranularBrush) {
    GranularBrush gb = (GranularBrush) brush;
    scheduleGranularBrushClick(gb, clipToWidth(mouseX), clipToHeight(mouseY), gainCurve);
  }
  // *****]]] NETWORKING [[[***** //
  if (nd != null && isNetSendGestures) {
    int x = clipToWidth(mouseX);
    int y = clipToHeight(mouseY);
    nd.oscSendMouseClicked(x, y, getSamplePos(x, y));
    nd.oscSendDrawPoints(curveMaker.getRdpPoints());
    nd.oscSendTimeStamp(curveMaker.timeStamp, curveMaker.timeOffset);
  }
  return brush;
}

/**
 * @return
 */
public AudioBrush makeBrushFromCurveMaker() {
  // reset some fields in gConfig
  GestureGranularConfig.Builder cfg = gConfig.copy();
  cfg.resampleCount = 0;
  cfg.targetDurationMs = 0;
  cfg.pitchSemitones = 0.0f;
  return makeBrush(curveMaker, cfg);
}

public AudioBrush makeBrush(PACurveMaker curve, GestureGranularConfig.Builder config) {
  if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) {
    return makeBrush(curve, config, GestureGranularConfigIO.InstrumentType.SAMPLER);
  } else {
    return makeBrush(curve, config, GestureGranularConfigIO.InstrumentType.GRANULAR);
  }
}

/**
 * End point for all makeBrush(...) calls, applies cues and special fx.
 *
 * @param curve
 * @param config
 * @param instrumentType
 * @return
 */
public AudioBrush makeBrush(PACurveMaker curve, GestureGranularConfig.Builder config,
  GestureGranularConfigIO.InstrumentType instrumentType) {
  // prepare
  GestureGranularConfig.Builder cfg = config.copy();
  PACurveMaker useCurve = curve;
  // handle cues
  CueResult r = applyPresets(cfg, curve);
  cfg = r.cfg;
  useCurve = r.curve;
  // now make brushes
  if (instrumentType == GestureGranularConfigIO.InstrumentType.SAMPLER) {
    SamplerBrush sb = new SamplerBrush(useCurve, cfg);
    samplerBrushes.add(sb);
    setActiveBrush(sb, samplerBrushes.size() - 1);
    if (this.isAutoOptimize) optimizeActiveBrush();
    syncEnvelopeMenu(cfg.env);
    if (isVerbose) println("----- new sampler brush created");
    return sb;
  } else {
    cfg.curveSteps = Math.min(cfg.curveSteps, 32);
    // Granular events use a stable envelope profile.
    // We do not fit ADSR to gesture duration; onset shape matters more than total span.
    cfg.env = granularEnv;
    GranularBrush gb = new GranularBrush(useCurve, cfg);
    granularBrushes.add(gb);
    setActiveBrush(gb, granularBrushes.size() - 1);
    if (this.isAutoOptimize) optimizeActiveBrush();
    if (isVerbose) println("----- new granular brush created, framerate = "+ frameRate);
    return gb;
  }
}

//  public void applyCue(GestureGranularConfig.Builder cfg, PACurveMaker curve) {
//      if (dbCue != null) dbCue.apply(cfg, curve, this);
//  }

public CueResult applyPresets(GestureGranularConfig.Builder cfg, PACurveMaker curve) {
  if (presetStack == null || presetStack.isEmpty()) {
    return new CueResult(curve, cfg);
  }
  PACurveMaker curCurve = curve;
  GestureGranularConfig.Builder curCfg = cfg;
  for (PerformancePreset cue : presetStack) {
    CueResult r = cue.apply(curCfg, curCurve, this);
    if (r != null) {
      if (r.cfg != null) curCfg = r.cfg;
      if (r.curve != null) curCurve = r.curve;
    }
  }
  return new CueResult(curCurve, curCfg);
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
  activeBrush = brush;
  gConfig = brush.cfg();
  if (brush instanceof GranularBrush) {
    GranularBrush gb = (GranularBrush) brush;
    activeGranularBrush = gb;
    activeGranularIndex = idx;
    activeSamplerBrush = null;
    activeSamplerIndex = -1;
  } else if (brush instanceof SamplerBrush) {
    SamplerBrush sb = (SamplerBrush) brush;
    activeSamplerBrush = sb;
    activeSamplerIndex = idx;
    // samplerEnv = (gConfig.env != null) ? gConfig.env : defaultSampConfig.env;
    samplerEnv = (gConfig.env != null) ? gConfig.env : envPreset("Soft");
    activeGranularBrush = null;
    activeGranularIndex = -1;
  }
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
  drawBrushes(granularBrushes, readyGranColor, hoverGranColor, activeGranColor);
  drawBrushes(samplerBrushes, readySamplerColor, hoverSamplerColor, activeSamplerColor);
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
 * @param b    an AudioBrush instance
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

/**
 * @param b    an AudioBrushLIte instance
 * @return     a GestureSchedule filtered by boundsPolicy to provide only in-bounds points
 */
GestureSchedule getPlaybackScheduleForBrush(AudioBrush b) {
  GestureSchedule sched = getScheduleForBrush(b);
  return boundsPolicy.applySchedule(sched);
}


// ------------- TRANSFORMS ------------- //

void initBrushTransform(AudioBrush b) {
  if (b == null) return;
  GestureTransformState st = b.ensureTransform();
  st.enabled = true;
  st.captureRestPoints(b.curve().copyAllPoints());
  st.resetTransform();
}

void updateAnimatedBrushes() {
  if (!isBrushTransformTest) return;
  if (isBrushTransformFrozen) return;
  if (activeBrush == null) return;

  GestureTransformState st = activeBrush.ensureTransform();
  if (!st.hasRestPoints()) {
    st.captureRestPoints(activeBrush.curve().copyAllPoints());
  }
  st.enabled = true;

  float t = millis() * 0.001f;

  // --- simple first test: pulse + spin + small orbit ---
  st.rotation = 0.6f * t;

  float pulse = 1.0f + 0.18f * sin(2.0f * t);
  st.scaleX = pulse;
  st.scaleY = pulse;

  st.translateX = 28.0f * cos(0.8f * t);
  st.translateY = 18.0f * sin(1.1f * t);

  // Optional reflection test every few seconds
  //st.flipHorizontal = ((int)(t * 0.25f) % 2) == 1;
  //st.flipVertical = false;

  activeBrush.applyTransform();
}

/*             END DRAWING METHODS              */
