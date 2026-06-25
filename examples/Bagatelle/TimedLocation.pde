/*----------------------------------------------------------------*/
/*                                                                */
/*                 TIME/LOCATION/ACTION METHODS                   */
/*                                                                */
/*----------------------------------------------------------------*/

// ------------- STANDARD SAMPLER AND GRANULAR BRUSH SCHEDULING ------------- //

void scheduleSamplerBrushClick(SamplerBrush sb, int clickX, int clickY) {
  scheduleSamplerBrushClick(sb, clickX, clickY, null);
}

void scheduleSamplerBrushClick(SamplerBrush sb, int clickX, int clickY, PAControlCurve gainCurve) {
  if (sb == null) return;
  ArrayList<PVector> pts = getPathPoints(sb);
  if (pts == null || pts.size() < 2) return;
  ensureSamplerReady();
  GestureGranularConfig snap = sb.snapshot();
  GestureSchedule sched = scheduleBuilder.build(sb.curve(), snap, audioOut.sampleRate());
  // ***** LIMIT SCHED TO IN-BOUNDS POINTS ***** TODO cache GestureSchedule
  sched = boundsPolicy.applySchedule(sched);
  if (sched == null || sched.isEmpty()) return;
  storeSamplerBrushEvents(sched, snap, millis() + 10, gainCurve);
  // *****]]] NETWORKING [[[***** //
  if (nd != null && isNetSendBrushTriggers) nd.oscSendTrig(samplerBrushes.indexOf(sb));
  if (isDebugging) {
    PVector startPoint = sched.points.get(0);
    int clickPos = mapper.lookupSignalPos(clickX, clickY);
    int signalPos = mapper.lookupSignalPos((int)startPoint.x, (int)startPoint.y);
    println("-- sampler brush event, signalPos = "+ signalPos +", clickPos = "+ clickPos);
  }
}

void debugSched(GestureSchedule sched) {
  println("-- schedule number of points= "+ sched.points.size());
}

public void storeSamplerBrushEvents(GestureSchedule sched, GestureGranularConfig snap, int startTime, PAControlCurve gainCurve) {
  if (sched == null || sched.points == null || sched.timesMs == null) return;
  if (samplerBrushEvents == null) samplerBrushEvents = new ArrayList<>();
  // check sizes
  final int pointCount = sched.points.size();
  final int timeCount = sched.timesMs.length;
  final int n = Math.min(pointCount, timeCount);
  if (n == 0) return;
  if (pointCount != timeCount) {
    System.err.println("storeSamplerBrushEvents(): point/time mismatch: points="
      + pointCount + ", times=" + timeCount + ", using n=" + n);
  }
  // good to go
  final float baseGain = snap.gainLinear();
  final float[] gainPerEvent =
    (gainCurve != null) ? PAKeyframeControlCurve.expandToSchedule(gainCurve, sched) : null;
  final float pitch = snap.pitchRatio();
  final ADSRParams baseEnv = (snap.env != null) ? snap.env.copy() : samplerEnv;
  synchronized (samplerBrushEventsLock) {
    for (int i = 0; i < n; i++) {
      PVector loc = sched.points.get(i);
      int x = Math.round(loc.x);
      int y = Math.round(loc.y);
      int pos = mapper.lookupSignalPos(x, y);
      envDuration = isAdjustEnvelope ? computeEnvDurationMs(sched, baseEnv.toString(), noteDuration ) : noteDuration;
      int len = calcSampleLen();
      int t = startTime + Math.round(sched.timesMs[i]);
      float pan = map(x, 0, width - 1, -0.875f, 0.875f);
      float gain = baseGain;
      if (gainPerEvent != null && i < gainPerEvent.length) {
        gain *= gainPerEvent[i];
      }
      samplerBrushEvents.add(
        new SamplerBrushEvent(x, y, t, pos, len, gain, pitch, baseEnv.copy(), pan)
        );
      if (isDebugging) {
        println("-- sampler evt i=" + i
          + " dt=" + sched.timesMs[i]
          + " eventTime=" + t
          + " pos=" + pos
          + " len=" + len
          + " gain=" + gain
          + " pitch=" + pitch
          + " pan=" + pan);
      }
    }
    samplerBrushEvents.sort((a, b) -> Integer.compare(a.eventTimeMs, b.eventTimeMs));
  }
}

/**
 * Runs sampler brush events in the samplerTimeLocs list.
 * TODO move audio events into a sample-accurate (or at least block-accurate) schedule
 */
public void runSamplerBrushEvents() {
  if (samplerBrushEvents == null || samplerBrushEvents.isEmpty()) return;
  int currentTime = millis();
  synchronized (samplerBrushEventsLock) {
    Iterator<SamplerBrushEvent> it = samplerBrushEvents.iterator();
    while (it.hasNext()) {
      SamplerBrushEvent evt = it.next();
      if (evt.eventTimeMs > currentTime) {
        // list is sorted, so we can stop early
        break;
      }
      playSample(evt.samplePos, evt.durationMs, evt.gain, evt.env, evt.pitchRatio, evt.pan);
      int sampleX = PixelAudio.constrain(evt.x, 0, width - 1);
      int sampleY = PixelAudio.constrain(evt.y, 0, height - 1);
      pointTimeLocsAddPoint(new TimedLocation(sampleX, sampleY, 200 + millis()));
      it.remove();
    }
  }
}

public void scheduleGranularBrushClick(GranularBrush gb, int clickX, int clickY) {
  scheduleGranularBrushClick(gb, clickX, clickY, null);
}


/**
 * Schedules a response to a mouse click or hover + spacebar on a granular brush.
 *
 * @param gb        a GranularBrush
 * @param clickX    x-coordinate of point of activation
 * @param clickY    y-coordinate of point of activation
 */
public void scheduleGranularBrushClick(GranularBrush gb, int clickX, int clickY, PAControlCurve gainCurve) {
  if (gb == null) return;
  ArrayList<PVector> pts = getPathPoints(gb);
  if (pts == null || pts.size() < 2) return;
  ensureGranularReady();
  float[] buf = (granSignal != null) ? granSignal : audioSignal;
  // Snapshot config ONCE so schedule + params are consistent
  GestureGranularConfig snap = gb.snapshot();
  // TODO:
  // Define explicit semantics for cfg.resampleCount <= 0 and cfg.targetDurationMs <= 0.
  // Current assumption:
  //   resampleCount <= 0 → keep original gesture timing
  //   targetDurationMs <= 0 → keep natural duration
  // apply resample/duration/warp via scheduleBuilder
  GestureSchedule sched = scheduleBuilder.build(gb.curve(), snap, audioOut.sampleRate());
  // ***** LIMIT SCHED TO IN-BOUNDS POINTS ***** TODO cache GestureSchedule
  sched = boundsPolicy.applySchedule(sched);
  if (sched == null || sched.isEmpty()) return;
  if (isDebugging) {
    println("sched.size=" + sched.size()
      + " durationMs=" + sched.durationMs()
      + " cfg.resampleCount=" + snap.resampleCount
      + " cfg.targetDurationMs=" + snap.targetDurationMs
      + " cfg.pathMode=" + snap.pathMode
      + " warp=" + snap.warpShape);
  }
  boolean isGesture = gb.cfg().hopMode == HopMode.GESTURE;
  GestureGranularParams gParams = gb.cfg().build().toParams();
  GestureEventParams eventParams = prepareGranularGesture(buf, sched, gParams, gainCurve);
  playGranularGesture(buf, sched, gParams, eventParams);
  storeGranularCurveTL(sched, millis() + 10, isGesture);
  // *****]]] NETWORKING [[[***** //
  if (nd != null && isNetSendBrushTriggers) nd.oscSendTrig(granularBrushes.indexOf(gb));
  if (isDebugging) {
    PVector startPoint = sched.points.get(0);
    int clickPos = mapper.lookupSignalPos(clickX, clickY);
    int signalPos = mapper.lookupSignalPos((int)startPoint.x, (int)startPoint.y);
    println("-- granular brush event, signalPos = "+ signalPos +", clickPos = "+ clickPos);
  }
}

/**
 * Stores granular gesture time/location events in grainTimeLocs.
 *
 * @param sched        a GestureSchedule, the times when things happen and where they happen
 * @param startTime    time when a gesture starts
 * @param isGesture    is the timing gesture-based or fixed? ignored, for now
 */
public void storeGranularCurveTL(GestureSchedule sched, int startTime, boolean isGesture) {
  if (sched == null || sched.points == null || sched.timesMs == null) return;
  if (this.grainTimeLocs == null) grainTimeLocs = new ArrayList<>();
  int pointCount = sched.points.size();
  int timeCount = sched.timesMs.length;
  int n = Math.min(pointCount, timeCount);
  if (n == 0) return;
  if (pointCount != timeCount) {
    System.err.println("storeGranularCurveTL(): point/time mismatch: points="
      + pointCount + ", times=" + timeCount + ", using n=" + n);
  }
  synchronized (grainTimeLocsLock) {
    for (int i = 0; i < n; i++) {
      PVector loc = sched.points.get(i);
      int x = Math.round(loc.x);
      int y = Math.round(loc.y);
      int t = startTime + Math.round(sched.timesMs[i]);
      int d = 200;
      this.grainTimeLocs.add(new TimedLocation(x, y, t, d));
    }
    Collections.sort(grainTimeLocs);
  }
}

/**
 * Tracks and runs TimedLocation events in the grainLocsArray list, which is
 * associated with granular synthesis gestures.
 */
public void runGrainEvents() {
  if (grainTimeLocs == null || grainTimeLocs.isEmpty()) return;
  int t = millis();
  synchronized (grainTimeLocsLock) {
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
  }
  // grainTimeLocs.removeIf(TimedLocation::isStale);    // necessary on fade-out, otherwise iteration does pruning
}

/**
 * Tracks and runs TimedLocation events in the timeLocsArray list, which is
 * associated with mouse clicks that trigger audio a the click point.
 * TODO java.util.ConcurrentModificationException BUG
 */
public void runPointEvents() {
  int currentTime = millis();
  synchronized (pointTimeLocsLock) {
    for (Iterator<TimedLocation> iter = pointTimeLocs.iterator(); iter.hasNext(); ) {
      TimedLocation tl = iter.next();
      tl.setStale(tl.eventTime() < currentTime);
      if (!tl.isStale()) {
        drawCircle(tl.getX(), tl.getY());
      }
    }
    pointTimeLocs.removeIf(TimedLocation::isStale);
  }
}

/**
 * Adds a TimedLocation to the pointTimeLocs list.
 * @param tl    a TimedLocation
 */
public void pointTimeLocsAddPoint(TimedLocation tl) {
  synchronized (pointTimeLocsLock) {
    if (pointTimeLocs == null) pointTimeLocs = new ArrayList<>();
    pointTimeLocs.add(tl);
  }
}

/**
 * Draws a circle at the location of an audio trigger (mouseDown event).
 * @param x    x coordinate of circle
 * @param y    y coordinate of circle
 */
public void drawCircle(int x, int y) {
  //float size = isRaining? random(10, 30) : 60;
  fill(circleColor);
  noStroke();
  circle(x, y, 18);
}
