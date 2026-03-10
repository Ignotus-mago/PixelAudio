/*----------------------------------------------------------------*/
/*                                                                */
/*                          EXPERIMENTS                           */
/*                                                                */
/*----------------------------------------------------------------*/


/**
 * Print suggested values for optimizing grain overlap for a brush.
 *
 * @param alpha
 */
void printGOptHints(float alpha) {
  if (activeBrush == null) return;

  GestureGranularConfig snap = activeBrush.snapshot();
  GestureSchedule schedule = scheduleBuilder.build(activeBrush.curve(), snap, audioOut.sampleRate());
  if (schedule == null || schedule.isEmpty()) {
    System.out.println("---- Granular OptHints [activeBrush] ----");
    System.out.println("No schedule / empty schedule.");
    System.out.println("----------------------------------------");
    return;
  }

  // Use the ACTUAL N and T produced by the builder
  int Nactual = schedule.points.size();

  // Duration from schedule times (robust to whether builder used drag time, target time, clamping, etc.)
  float[] tMs = schedule.timesMs;
  float t0 = tMs[0];
  float t1 = tMs[tMs.length - 1];
  float TactualMs = Math.max(0f, t1 - t0);

  // If you normally normalize/enforce for playback, do the same here so the debug matches playback.
  // (Optional but recommended)
  float[] norm = GestureSchedule.normalizeTimesToStartAtZero(tMs);
  GestureSchedule.enforceNonDecreasing(norm);
  TactualMs = norm[norm.length - 1] - norm[0];

  float S = PACurveUtility.pathLength(schedule.points);
  float Tsec = schedule.durationMs() / 1000f;
  float hsec = snap.hopLengthSamples / audioOut.sampleRate();
  float dHop = (S / Tsec) * hsec;   // px per hop
  float targetSpacingPx = dHop * alpha;     // or 0.75*dHop, etc.
  if (isVerbose) {
    println("-- path length px = "+ S);
    println("-- dHop = "+ dHop +", alpha = "+ alpha);
    println("-- targetSpacingPx = "+ targetSpacingPx);
  }

  StringBuffer info = new StringBuffer();
  this.optGrainCount = calcGranularOptHints(
    "activeBrush",
    Nactual,
    TactualMs,
    snap.hopLengthSamples,
    snap.grainLengthSamples,
    audioOut.sampleRate(),
    (ArrayList<PVector>) schedule.points,
    targetSpacingPx, // d0
    1.0f, // wt
    0.25f, // ws
    info
    );
  if (isVerbose) println(info.toString());
}

/**
 * Calculate optimal configuration settings for a granular brush.
 *
 * @return optimal number of samples if time duration is kept as is
 */
public int calcGranularOptHints(
  String tag,
  int N, float Tms,
  int hopSamples, int grainLenSamples,
  float sr,
  List<PVector> scheduledPoints, // the list you will actually schedule
  float targetSpacingPx, float wt, float ws,
  StringBuffer sb
  ) {
  // ---- GUI ranges ----
  final int   RESAMPLE_MIN = 2, RESAMPLE_MAX = 2048;
  final float DUR_MIN_MS   = 50f, DUR_MAX_MS   = 16000f; // bump from 10000 -> 16000 if desired

  // Path length in px
  float S = PACurveUtility.pathLength(scheduledPoints);

  // Guard rails
  N = Math.max(RESAMPLE_MIN, Math.min(RESAMPLE_MAX, N));
  Tms = Math.max(DUR_MIN_MS, Math.min(DUR_MAX_MS, Tms));

  float T = Math.max(0.001f, Tms / 1000f);
  float h = hopSamples / sr;        // seconds
  float hopMs = 1000f * h;

  int Nm1 = Math.max(1, N - 1);
  float dtMs = Tms / Nm1;
  float r = dtMs / hopMs;           // dt/hop ratio
  float dsPx = (S > 0 ? S / Nm1 : Float.NaN);

  // Time-opt formulas
  float ToptRawMs = 1000f * (Nm1 * h);
  int NoptRaw = 1 + Math.max(1, Math.round((T * sr) / hopSamples));

  // Clamp to widget ranges
  float ToptMs = Math.max(DUR_MIN_MS, Math.min(DUR_MAX_MS, ToptRawMs));
  int Nopt = Math.max(RESAMPLE_MIN, Math.min(RESAMPLE_MAX, NoptRaw));

  // Compromise N* (time+space)
  int NstarRaw = -1, Nstar = -1;
  if (S > 0 && targetSpacingPx > 0 && (wt > 0 || ws > 0)) {
    float numer = wt * T * T + ws * S * S;
    float denom = wt * T * h + ws * S * targetSpacingPx;
    if (denom > 1e-9f) {
      float xStar = numer / denom; // x = N-1
      NstarRaw = 1 + Math.max(1, Math.round(xStar));
      Nstar = Math.max(RESAMPLE_MIN, Math.min(RESAMPLE_MAX, NstarRaw));
    }
  }

  float overlapPct = Float.NaN;
  if (grainLenSamples > 0) overlapPct = 100f * (1f - hopSamples / (float)grainLenSamples);

  String densityWord = (r > 1.25f) ? "SPARSE" : (r < 0.80f ? "DENSE" : "OK");

  // construct informative StringBuffer
  sb.append("\n---- Granular OptHints" + (tag == null ? "" : " [" + tag + "]") + " ----\n");
  sb.append("Current: N=" + N + "  Tms=" + fmt(Tms, 2) +
    "  hop=" + hopSamples + " samp (" + fmt(hopMs, 3) + " ms)" +
    "  grain=" + grainLenSamples + " samp  sr=" + fmt(sr, 1) +"\n");
  sb.append("Timing:  avg dt≈" + fmt(dtMs, 3) + " ms  dt/hop≈" + fmt(r, 2) + "  => " + densityWord +"\n");
  if (!Float.isNaN(dsPx)) sb.append("Space:   pathLen≈" + fmt(S, 2) + " px  avg ds≈" + fmt(dsPx, 3) + " px");
  if (!Float.isNaN(overlapPct)) sb.append("Overlap: ≈" + fmt(overlapPct, 1) + "% (1 - H/L)\n");
  sb.append("If keep N:  Topt≈" + fmt(ToptRawMs, 2) + " ms" +
    (ToptMs != ToptRawMs ? "  (clamped→" + fmt(ToptMs, 2) + ")" : ""));
  sb.append("\n");
  sb.append("If keep T:  Nopt≈" + NoptRaw +
    (Nopt != NoptRaw ? "  (clamped→" + Nopt + ")" : ""));
  sb.append("\n");
  if (Nstar > 0) {
    sb.append("Compromise (wt=" + fmt(wt, 3) + ", ws=" + fmt(ws, 3) + ", d0=" + fmt(targetSpacingPx, 2) + "px): N*≈" +
      NstarRaw + (Nstar != NstarRaw ? " (clamped→" + Nstar + ")" : ""));
    sb.append("\n");
  }
  // Extra guidance when clamped
  if (NoptRaw > RESAMPLE_MAX && r > 1.25f) {
    sb.append("Note: Nopt exceeds max; likely sparse even at max resample. Consider CURVE_POINTS, shorter T, or smaller hop.\n");
  }
  if (ToptRawMs > DUR_MAX_MS && r > 1.25f) {
    sb.append("Note: Topt exceeds max; consider raising Duration max (you mentioned 16000ms), or increase N / use CURVE_POINTS.\n");
  }
  sb.append("-------------------------------------------");

  // optimal number of samples if time is kept as is
  return Nstar;
}

static String fmt(float v, int decimals) {
  return String.format("%." + decimals + "f", v);
}
