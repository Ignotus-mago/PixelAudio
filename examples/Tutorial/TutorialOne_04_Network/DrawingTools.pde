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
  int x = clipToWidth(mouseX);
  int y = clipToHeight(mouseY);
  addDrawingPoint(x, y);
}

/**
 * While user is dragging the mouses and isDrawMode == true, accumulates new points
 * to allPoints and event times to allTimes. Sets sampleX, sampleY and samplePos variables.
 * We constrain points outside the bounds of the display window. An alternative approach
 * is be to ignore them, which may give a more "natural" appearance for fast drawing.
 *
 * @param x    x-coordinate of point to add to allPoints
 * @param y    y-coordinate of point to add to allPoints
 */
public void addDrawingPoint(int x, int y) {
  if (x != currentPoint.x || y != currentPoint.y) {
    x = clipToWidth(x);
    y = clipToHeight(y);
    currentPoint = new PVector(x, y);
    allPoints.add(currentPoint);
    allTimes.add(millis() - startTime);
    // *****]]] NETWORKING [[[***** //
    if (nd != null && isNetSendDrawingPoints) nd.oscSendMouseClicked(x, y, getSamplePos(x, y));
  }
}

/**
 * Clips parameter i to the interval (0..width-1)
 * @param i    integer to clip to width
 * @return     value within the range 0..width-1
 */
public int clipToWidth(int i) {
  return min(max(0, i), width - 1);
}
/**
 * Clips parameter i to the interval (0..width-1)
 * @param i    integer to clip to height
 * @return     value within the range 0..height-1
 */
public int clipToHeight(int i) {
  return min(max(0, i), height - 1);
}

	/**
	 * Displaces a supplied point by a random Gaussian variable.
	 * @param x              x-coordinate
	 * @param y              y-coordinate
	 * @param deviationPx    average deviation, in pixels
	 * @return a displaced coordinate point as a PVector
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
 * @return                  an array of Gaussian values centered on 1.0
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
 * Generates an array of Gaussian values for shifting pitch, where 1.0 = no shift.
 * @param length            length of the returned array
 * @param deviationPitch    expected average deviation of the pitch around center pitch ratio
 * @param centerPitch       ratio of center pitch, 1.0 => no change in source
 * @return                  an array of Gaussian values centered on 1.0
 */
float[] generateJitterPitch(int length, float deviationPitch, float centerPitch) {
  float[] pitch = new float[length];
  double variance = deviationPitch * deviationPitch;
  for (int i = 0; i < pitch.length; i++) {
    pitch[i] = (float) PixelAudio.gauss(centerPitch, variance);
  }
  return pitch;
}

/**
 * Initializes a PACurveMaker instance with allPoints as an argument to the factory method
 * PACurveMaker.buildCurveMaker() and then fills in PACurveMaker instance variables from
 * variables in the calling class (TutorialOneDrawing, here).
 */
public AudioBrushLite initCurveMakerAndAddBrush() {
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
  cfg.pathMode = PathMode.REDUCED_POINTS; // tutorial default
  // Default output for newly drawn strokes: SAMPLER (tutorial-centric)
  AudioBrushLite b = new AudioBrushLite(curveMaker, cfg, BrushOutput.SAMPLER, HopMode.GESTURE);
  b.cfg.pathMode = defaultPathModeFor(b.output());
  brushes.add(b);
  // Optionally auto-select the new brush
  activeBrush = b;
  if (this.doPlayOnNewBrush) {
    if (b.output() == BrushOutput.GRANULAR) {
      scheduleGranularBrushClick(b);
    } else {
      scheduleSamplerBrushClick(b);
    }
  }
  // *****]]] NETWORKING [[[***** //
  if (nd != null && isNetSendGestures) {
    int x = clipToWidth(mouseX);
    int y = clipToHeight(mouseY);
    nd.oscSendMouseClicked(x, y, getSamplePos(x, y));
    nd.oscSendDrawPoints(curveMaker.getRdpPoints());
    nd.oscSendTimeStamp(curveMaker.timeStamp, curveMaker.timeOffset);
  }
  return b;
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
 * Entry point for drawing brushstrokes on the screen.
 */
public void drawBrushShapes() {
  if (brushes == null || brushes.isEmpty()) return;
  drawBrushes(brushes);
}

/**
 * Draw brushstrokes on the display image.
 *
 * @param list    a list of all the brushstrokes (AudioBrushLite)
 */
public void drawBrushes(List<AudioBrushLite> list) {
  // step through the list of all brushes
  int readyColor;
  int hoverColor;
  int selectedColor;
  for (int i = 0; i < list.size(); i++) {
    AudioBrushLite b = list.get(i);
    if (b.output == BrushOutput.GRANULAR) {
      readyColor = readyBrushColor1;
      hoverColor = hoverBrushColor1;
      selectedColor = selectedBrushColor1;
    } else {
      readyColor = readyBrushColor2;
      hoverColor = hoverBrushColor2;
      selectedColor = selectedBrushColor2;
    }
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
      case REDUCED_POINTS: {
        PACurveUtility.lineDraw(this, cm.getReducedPoints(), lc, w);
        PACurveUtility.pointsDraw(this, cm.getReducedPoints(), cc, d);
        break;
      }
      case CURVE_POINTS: {
        PACurveUtility.lineDraw(this, cm.getCurvePoints(), lc, w);
        PACurveUtility.pointsDraw(this, cm.getCurvePoints(), cc, d);
        break;
      }
      case ALL_POINTS: {
        PACurveUtility.lineDraw(this, cm.getAllPoints(), lc, w);
        PACurveUtility.pointsDraw(this, cm.getAllPoints(), cc, d);
        break;
      }
      default: {
        PACurveUtility.lineDraw(this, cm.getAllPoints(), lc, w);
        PACurveUtility.pointsDraw(this, cm.getAllPoints(), cc, d);
        break;
      }
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
 * Sets epsilon value for the PACurveMaker associated with an AudioBrushLite instance.
 *
 * @param b    an AudioBrushLite instance
 * @param cs    desired epsilon value to control point reduction
 */
public void setBrushCurveSteps(AudioBrushLite b, int cs) {
  PACurveMaker cm = b.curve();
  BrushConfig cfg = b.cfg();
  cfg.curveSteps = cs;
  cm.setCurveSteps(cs);
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
    case ALL_POINTS: return cm.getAllPoints();
    case REDUCED_POINTS: return cm.getReducedPoints();
    case CURVE_POINTS: return cm.getCurvePoints();
    default:  return cm.getAllPoints();
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
    case REDUCED_POINTS: {
      sched = b.curve.getReducedSchedule(b.cfg.rdpEpsilon);
      break;
    }
    case CURVE_POINTS: {
      sched = b.curve.getCurveSchedule(b.cfg.rdpEpsilon, b.cfg.curveSteps, isAnimating);
      break;
    }
    case ALL_POINTS: {
      sched = b.curve.getAllPointsSchedule();
      break;
    }
    default: {
      sched = b.curve.getAllPointsSchedule();
      break;
    }
  }
  return sched;
}

/**
 * @param b    an AudioBrushLIte instance
 * @return     a GestureSchedule filtered by boundsPolicy to provide only in-bounds points
 */
GestureSchedule getPlaybackScheduleForBrush(AudioBrushLite b) {
  GestureSchedule sched = getScheduleForBrush(b);
  if (b.output == BrushOutput.SAMPLER) {
    // divide gesture duration by number of gesture intervals and multiply result a fixed value
    envDuration = isAdjustEnvelope ? computeEnvDurationMs(sched, defaultEnv.toString(), noteDuration ) : noteDuration;
    println("-- envelope duration = "+ envDuration);
  }
  return boundsPolicy.applySchedule(sched);
}


	/**
	 * @param sched         a {@code GestureSchedule} to access for calculating an envelope duration
	 * @param envName       name of an envelope preset
	 * @param fallbackMs    default duration in milliseconds
	 * @return calculated sample length in samples of an envelope
	 */
int computeEnvDurationMs(GestureSchedule sched, String envName, int fallbackMs) {
  int n = sched.points.size();
  if (n < 2) return fallbackMs;
  float avgStepMs = sched.durationMs() / (float)(n - 1);
  float factor;
  switch (envName) {
  case "Pluck":
  case "Percussion":
    factor = 4.0f;
    break;
  case "Soft":
  case "Fade":
    factor = 3.0f;
    break;
  case "Swell":
  case "Pad":
    factor = 2.0f;
    break;
  default:
    factor = 3.0f;
  }
  int minEnvMs = envMinDurationMs;
  int maxEnvMs = envMaxDurationMs;
  return PApplet.constrain(Math.round(avgStepMs * factor), minEnvMs, maxEnvMs);
}

/**
 * Schedule a Sampler brush audio / animation event.
 *
 * @param sb    an AudioBrushLite instance
 */
void scheduleSamplerBrushClick(AudioBrushLite sb) {
  if (sb == null) return;
  ArrayList<PVector> pts = getPathPoints(sb);
  if (pts == null || pts.size() < 2) return;
  GestureSchedule sched = getPlaybackScheduleForBrush(sb);
  storeSamplerCurveTL(sb, sched, millis() + 10);
  // *****]]] NETWORKING [[[***** //
  if (nd != null && isNetSendBrushTriggers) nd.oscSendTrig(brushes.indexOf(sb));
}

/**
 * Store scheduled sampler synth / animation events for future activation.
 * @param sched        a GestureSchedule (points + timing for a brush)
 * @param startTime    time to start a series of events
 */
public synchronized void storeSamplerCurveTL(AudioBrushLite b, GestureSchedule sched, int startTime) {
  if (this.samplerTimeLocs == null) samplerTimeLocs = new ArrayList<>();
  int i = 0;
  startTime = millis() + 5;
  // we store the point and the current time + time offset, where timesMs[0] == 0
  for (PVector loc : sched.points) {
    int x = Math.round(loc.x);
    int y = Math.round(loc.y);
    int t = startTime + Math.round(sched.timesMs[i++]);
    int pos = getSamplePos(x, y);
    int len = calcSampleLen();
    int d = 200;
    float gain = samplerGain;
    float pitch = b.pitchRatio;
    ADSRParams env = defaultEnv.copy();
    float pan = map(x, 0, width - 1, -0.875f, 0.875f);
    this.samplerTimeLocs.add(new SamplerBrushEvent(x, y, t, pos, len, gain, pitch, env, pan));
  }
  Collections.sort(samplerTimeLocs);
}

/**
 * Execute audio / animation events for Sampler brushstrokes.
 */
	public synchronized void runSamplerBrushEvents() {
	    if (samplerTimeLocs == null || samplerTimeLocs.isEmpty()) return;
	    int currentTime = millis();
	    int durationMs = 200;
	    samplerTimeLocs.forEach(stl -> {
	        if (stl.eventTimeMs() < currentTime) {
		// sched points from storeSamplerCurveTL are already in bounds
	            int sampleX = Math.round(stl.getX());
	            int sampleY = Math.round(stl.getY());
	            // playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pitch, float pan)
                playSample(stl.samplePos, stl.durationSamples, stl.gain, stl.env, stl.pitchRatio, stl.pan);
		pointTimeLocs.add(new TimedLocation(sampleX, sampleY, durationMs + millis()));
	            stl.setStale(true);
	        }
	        else {
	            return;
	        }
	    });
	    samplerTimeLocs.removeIf(SamplerBrushEvent::isStale);
	}

/**
 * Schedule a Granular brush audio / animation event.
 *
 * @param gb    an AudioBrushLite instance
 */
void scheduleGranularBrushClick(AudioBrushLite gb) {
  if (gb == null) return;
  ArrayList<PVector> pts = getPathPoints(gb);
  if (pts == null || pts.size() < 2) return;
  ensureGranularReady();
  float[] buf = (granSignal != null) ? granSignal : audioSignal;
  boolean isGesture = (gb.hopMode() == HopMode.GESTURE);
  GestureGranularParams gParams = isGesture ? gParamsGesture : gParamsFixed;
  GestureSchedule sched = getPlaybackScheduleForBrush(gb);
  playGranularGesture(buf, sched, gParams, gb.pitchRatio);
  storeGranularCurveTL(sched, millis() + 10, isGesture);
  // *****]]] NETWORKING [[[***** //
  if (nd != null && isNetSendBrushTriggers) nd.oscSendTrig(brushes.indexOf(gb));
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
	for (Iterator<TimedLocation> iter = grainTimeLocs.iterator(); iter.hasNext();) {
		TimedLocation tl = iter.next();
		int low = tl.eventTime();
		int high = (tl.eventTime() + tl.getDurationMs());
		if (t >= low && t < high) { // event in the interval between low and high
			drawCircle(tl.getX(), tl.getY());
		}
		else {
			if (t >= high) {        // event in the past
				tl.setStale(true);
				iter.remove();
			}
			if (t < low) {          // event in the future
				break;
			}
		}
	}
	// grainLocsArray.removeIf(TimedLocation::isStale);		// not necessary if we remove in loop
}

/**
 * Tracks and runs TimedLocation events in the timeLocsArray list, which is
 * associated with mouse clicks that trigger audio a the click point.
 */
public synchronized void runPointEvents() {
	int currentTime = millis();
	for (Iterator<TimedLocation> iter = pointTimeLocs.iterator(); iter.hasNext();) {
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
