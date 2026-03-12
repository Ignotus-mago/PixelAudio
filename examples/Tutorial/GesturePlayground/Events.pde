/*----------------------------------------------------------------*/
/*                                                                */
/*                 TIME/LOCATION/ACTION METHODS                   */
/*                                                                */
/*----------------------------------------------------------------*/

// ------------- STANDARD SAMPLER AND GRANULAR BRUSH SCHEDULING ------------- //

void scheduleSamplerBrushClick(SamplerBrush sb, int clickX, int clickY) {
  if (sb == null) return;
  ArrayList<PVector> pts = getPathPoints(sb);
  if (pts == null || pts.size() < 2) return;
  ensureSamplerReady();
  GestureGranularConfig snap = sb.snapshot();
  GestureSchedule sched = scheduleBuilder.build(sb.curve(), snap, audioOut.sampleRate());
  // GestureSchedule sched = getScheduleForBrush(sb);  // just the brush settings here
  storeSamplerCurveTL(sched, millis() + 10);
  PVector startPoint = sched.points.get(0);
  int clickPos = mapper.lookupSignalPos(clickX, clickY);
  int signalPos = mapper.lookupSignalPos((int)startPoint.x, (int)startPoint.y);
  if (isVerbose) println("-- sampler brush event, signalPos = "+ signalPos +", clickPos = "+ clickPos);
}

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

public synchronized void runSamplerBrushEvents() {
  if (samplerTimeLocs == null || samplerTimeLocs.isEmpty()) return;
  int currentTime = millis();
  samplerTimeLocs.forEach(tl -> {
    if (tl.eventTime() < currentTime) {
      int sampleX = PixelAudio.constrain(Math.round(tl.getX()), 0, width - 1);
      int sampleY = PixelAudio.constrain(Math.round(tl.getY()), 0, height - 1);
      float panning = map(sampleX, 0, width, -0.8f, 0.8f);
      int pos = getSamplePos(sampleX, sampleY);
      playSample(pos, calcSampleLen(), samplerGain, panning);
      pointTimeLocs.add(new TimedLocation(sampleX, sampleY, tl.getDurationMs() + millis()));
      tl.setStale(true);
    } else {
      return;
    }
  }
  );
  samplerTimeLocs.removeIf(TimedLocation::isStale);
}

void scheduleGranularBrushClick(GranularBrush gb, int clickX, int clickY) {
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
  if (sched == null || sched.isEmpty()) return;
  if (isVerbose) {
    println("sched.size=" + sched.size()
      + " durationMs=" + sched.durationMs()
      + " cfg.resampleCount=" + snap.resampleCount
      + " cfg.targetDurationMs=" + snap.targetDurationMs
      + " cfg.pathMode=" + snap.pathMode
      + " warp=" + snap.warpShape);
  }

  boolean isGesture = gb.cfg().hopMode == HopMode.GESTURE;
  GestureGranularParams gParams = gb.cfg().build().toParams();
  // GestureSchedule sched = getScheduleForBrush(gb);
  playGranularGesture(buf, sched, gParams);
  storeGranularCurveTL(sched, millis() + 10, isGesture);
  PVector startPoint = sched.points.get(0);
  int clickPos = mapper.lookupSignalPos(clickX, clickY);
  int signalPos = mapper.lookupSignalPos((int)startPoint.x, (int)startPoint.y);
  if (isVerbose) println("-- granular brush event "+ signalPos +", clickPos = "+ clickPos);
}

public synchronized void storeGranularCurveTL(GestureSchedule sched, int startTime, boolean isGesture) {
  if (this.grainTimeLocs == null) grainTimeLocs = new ArrayList<>();
  int i = 0;
  //int hopMs = (int) Math.round(AudioUtility.samplesToMillis(hopSamples, sampleRate));
  //int durMsFixed = (int) Math.round(AudioUtility.samplesToMillis(granSamples, sampleRate)); // or hopMs if you prefer
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
