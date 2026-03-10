/*----------------------------------------------------------------*/
/*                                                                */
/*                   BRUSH INTERACTION METHODS                    */
/*                                                                */
/*----------------------------------------------------------------*/

//------------- REMOVE BRUSHES -------------//

// TODO verify correct action -- active brush is not always getting set

/**
 * Removes the current active PACurveMaker instance, flagged by a highlighted brush stroke,
 * from brushShapesList, if there is one.
 */
public void removeActiveBrush() {
  if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) {
    if (activeBrush instanceof SamplerBrush) removeSamplerBrush(activeBrush, activeSamplerIndex);
  } else if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR) {
    if (activeBrush instanceof GranularBrush) removeGranularBrush(activeBrush, activeGranularIndex);
  } else {
    // play mode, no edting
  }
}

/**
 * Removes the current active PACurveMaker instance, flagged by a highlighted brush stroke,
 * from brushShapesList, if there is one.
 */
public void removeHoverBrush() {
  if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) {
    if (hoverBrush instanceof SamplerBrush) removeSamplerBrush(hoverBrush, activeSamplerIndex);
  } else if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR) {
    if (hoverBrush instanceof GranularBrush) removeGranularBrush(hoverBrush, activeGranularIndex);
  } else {
    // play mode, no edting
  }
}

/**
 * Removes the newest PACurveMaker instance, shown as a brush stroke
 * in the display, from brushShapesList.
 */
public void removeNewestBrush() {
  if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) {
    if (samplerBrushes != null && !samplerBrushes.isEmpty()) {
      int idx = samplerBrushes.size();
      samplerBrushes.remove(idx - 1);  // brushShapes array starts at 0
      if (isVerbose) println("-->> removed newest brush");
      curveMaker = null;
    }
  } else if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR) {
    if (granularBrushes != null && !granularBrushes.isEmpty()) {
      int idx = granularBrushes.size();
      granularBrushes.remove(idx - 1);
      if (isVerbose) println("-->> removed newest brush");
      curveMaker = null;
    }
  } else {
  }
}

/**
 * Removes the oldest brush in brushShapesList.
 */
public void removeOldestBrush() {
  if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) {
    if (samplerBrushes != null && !samplerBrushes.isEmpty()) {
      samplerBrushes.remove(0);  // brushShapes array starts at 0
      if (isVerbose) println("-->> removed oldest brush");
      if (samplerBrushes.isEmpty()) curveMaker = null;
    }
  } else if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR) {
    if (granularBrushes != null && !granularBrushes.isEmpty()) {
      granularBrushes.remove(0);
      if (isVerbose) println("-->> removed oldest brush");
      if (granularBrushes.isEmpty()) curveMaker = null;
    }
  } else {
  }
}


// -------------- CONVERT BRUSH TYPES ------------- //

/**
 * Convert a brush to the opposite type, reusing the same PACurveMaker
 * and the same GestureGranularConfig.Builder instance.
 *
 * This is a replacement operation: the old brush should be removed
 * from its list immediately after conversion.
 *
 * @param brush    AudioBrush to convert, GranularBrush <-> SamplerBrush
 */
AudioBrush toggleBrushType(AudioBrush brush) {
  if (brush == null) return null;

  PACurveMaker curve = brush.curve();
  GestureGranularConfig.Builder cfg = brush.cfg();   // reuse same builder intentionally

  if (brush instanceof SamplerBrush) {
    normalizeConfigForGranular(cfg);
    return new GranularBrush(curve, cfg);
  } else if (brush instanceof GranularBrush) {
    normalizeConfigForSampler(cfg);
    return new SamplerBrush(curve, cfg);
  }

  return brush;
}

/**
 * Convert a brush explicitly to SamplerBrush.
 */
SamplerBrush toSamplerBrush(AudioBrush brush) {
  if (brush == null) return null;
  if (brush instanceof SamplerBrush) return (SamplerBrush) brush;
  PACurveMaker curve = brush.curve();
  GestureGranularConfig.Builder cfg = brush.cfg();   // reuse same builder intentionally
  normalizeConfigForSampler(cfg);
  return new SamplerBrush(curve, cfg);
}

/**
 * Convert a brush explicitly to GranularBrush.
 */
GranularBrush toGranularBrush(AudioBrush brush) {
  if (brush == null) return null;
  if (brush instanceof GranularBrush) return (GranularBrush) brush;
  PACurveMaker curve = brush.curve();
  GestureGranularConfig.Builder cfg = brush.cfg();   // reuse same builder intentionally
  normalizeConfigForGranular(cfg);
  return new GranularBrush(curve, cfg);
}

/**
 * Remove oldBrush from its current typed list, insert newBrush into
 * the opposite typed list, and preserve hover/active state.
 *
 * oldIndex should be the index in the old brush's own list.
 */
void replaceBrush(AudioBrush oldBrush, AudioBrush newBrush, int oldIndex) {
  if (oldBrush == null || newBrush == null || oldBrush == newBrush) return;
  boolean wasHover  = (hoverBrush == oldBrush);
  boolean wasActive = (activeBrush == oldBrush);
  // 1) Remove from old typed list
  if (oldBrush instanceof GranularBrush) {
    GranularBrush gbOld = (GranularBrush) oldBrush;
    removeGranularBrush(gbOld, oldIndex);
  } else if (oldBrush instanceof SamplerBrush) {
    SamplerBrush sbOld = (SamplerBrush) oldBrush;
    removeSamplerBrush(sbOld, oldIndex);
  }
  // 2) Add to new typed list
  int newIndex = -1;
  if (newBrush instanceof GranularBrush) {
    GranularBrush gbNew = (GranularBrush) newBrush;
    newIndex = appendGranularBrush(gbNew);
  } else if (newBrush instanceof SamplerBrush) {
    SamplerBrush sbNew = (SamplerBrush) newBrush;
    newIndex = appendSamplerBrush(sbNew);
  }
  // 3) Restore hover state
  if (wasHover) {
    hoverBrush = newBrush;
    hoverIndex = newIndex;
  }
  // 4) Restore active state
  if (wasActive) {
    setActiveBrush(newBrush, newIndex);
  }
  if (isVerbose) {
    println("-- converted "
      + oldBrush.getClass().getSimpleName()
      + " -> "
      + newBrush.getClass().getSimpleName()
      + " at index " + newIndex);
  }
}

/**
 * Append a granular brush and return its new index.
 */
int appendGranularBrush(GranularBrush gb) {
  if (gb == null) return -1;
  granularBrushes.add(gb);
  return granularBrushes.size() - 1;
}

/**
 * Append a sampler brush and return its new index.
 */
int appendSamplerBrush(SamplerBrush sb) {
  if (sb == null) return -1;
  samplerBrushes.add(sb);
  return samplerBrushes.size() - 1;
}

/**
 * Remove a granular brush using index when reliable, else by object.
 */
void removeGranularBrush(AudioBrush gb, int idx) {
  if (gb instanceof SamplerBrush) return;
  if (gb == null || granularBrushes == null || granularBrushes.isEmpty()) return;
  if (idx >= 0 && idx < granularBrushes.size() && granularBrushes.get(idx) == gb) {
    granularBrushes.remove(idx);
  } else {
    granularBrushes.remove(gb);
  }
  if (activeGranularBrush == gb) {
    activeGranularBrush = null;
    activeGranularIndex = -1;
  }
}

/**
 * Remove a sampler brush using index when reliable, else by object.
 */
void removeSamplerBrush(AudioBrush sb, int idx) {
  if (sb instanceof GranularBrush) return;
  if (sb == null || samplerBrushes == null || samplerBrushes.isEmpty()) return;
  if (idx >= 0 && idx < samplerBrushes.size() && samplerBrushes.get(idx) == sb) {
    samplerBrushes.remove(idx);
  } else {
    samplerBrushes.remove(sb);
  }
  if (activeSamplerBrush == sb) {
    activeSamplerBrush = null;
    activeSamplerIndex = -1;
  }
}

/**
 * Normalize config values when converting to SamplerBrush.
 * Keep most gesture/path information intact.
 */
void normalizeConfigForSampler(GestureGranularConfig.Builder cfg) {
  if (cfg == null) return;
  // Sampler is point-oriented; FIXED hop is not very meaningful there.
  cfg.hopMode = GestureGranularConfig.HopMode.GESTURE;
  // Keep curve/path settings as-is.
  // Keep resampleCount / targetDurationMs / pitchSemitones
  // Envelope: supply a default if missing.
  if (cfg.env == null) {
    cfg.env = envPreset("Pluck");
  }
}

/**
 * Normalize config values when converting to GranularBrush.
 * Keep most gesture/path information intact, but make a few granular-friendly adjustments.
 */
void normalizeConfigForGranular(GestureGranularConfig.Builder cfg) {
  if (cfg == null) return;
  // Granular can use either hop mode, so keep current hopMode.
  // But ensure warp shape has a valid default.
  if (cfg.warpShape == null) {
    cfg.warpShape = GestureGranularConfig.WarpShape.LINEAR;
  }
  // Granular rendering gets unwieldy with huge curveSteps in your sketch;
  // your init path already clamps this for newly-created granular brushes.
  cfg.curveSteps = Math.min(cfg.curveSteps, 32);
  // Envelope: supply a default if missing.
  if (cfg.env == null) {
    cfg.env = envPreset("Fade");
  }
}

/**
 * Toggle the currently hovered brush between SamplerBrush and GranularBrush.
 */
AudioBrush toggleHoveredBrushType() {
  if (hoverBrush == null) return null;
  AudioBrush oldBrush = hoverBrush;
  AudioBrush newBrush = toggleBrushType(oldBrush);
  replaceBrush(oldBrush, newBrush, hoverIndex);
  return newBrush;
}

/**
 * Toggle the currently active brush between SamplerBrush and GranularBrush.
 */
AudioBrush toggleActiveBrushType() {
  if (activeBrush == null) return null;
  AudioBrush oldBrush = activeBrush;
  int oldIndex = (oldBrush instanceof GranularBrush) ? activeGranularIndex : activeSamplerIndex;
  AudioBrush newBrush = toggleBrushType(oldBrush);
  replaceBrush(oldBrush, newBrush, oldIndex);
  return newBrush;
}

/**
 * Change the current DrawingMode to suit the brush passed as an argument.
 *
 * @param brush    an AudioBrush instance
 */
void syncDrawingModeToBrush(AudioBrush brush) {
  if (brush instanceof GranularBrush) {
    drawingMode = DrawingMode.DRAW_EDIT_GRANULAR;
    controlWindow.setTitle("Granular Synth");
  } else if (brush instanceof SamplerBrush) {
    drawingMode = DrawingMode.DRAW_EDIT_SAMPLER;
    controlWindow.setTitle("Sampler Synth");
  }
}
