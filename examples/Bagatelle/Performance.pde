/*----------------------------------------------------------------*/
/*                  BEGIN PERFORMANCE METHODS                     */
/*----------------------------------------------------------------*/

/**
 * @param key
 */
void numericKey(char key) {
  // load a function that modifies the next brush when it is created by makeBrush()
  runPerformancePreset(PerformancePreset.fromKey(key), key);
  // execute cues in anticipation of the preset immediately (load files, etc.)
  runPerformanceCue(key);
}

/**
 * @param key
 */
public void runPerformancePreset(PerformancePreset cue, char key) {
  if (cue != null) {
    if (presetStack.contains(cue)) {
      presetStack.remove(cue);
    } else {
      presetStack.add(cue);
    }
  } else if (key == '0') {
    presetStack.clear();
  }
  println("-- cues: " + presetStack);
}

/**
 * @param key
 */
void runPerformanceCue(char key) {
  if (isRunWordGame) {
    switch (key) {
    case '1': // DRONE_RAINDROPS
      isLoadToBoth = true;
      applyColorMapOnLoad = true;
      setAudioGain(-24.0f);
      loadAudioFile(new File(daPath + "D-flat2_bassClar_window..wav"));
      break;
    case '2': // VOICE_AND_MELODY
      setMode(DrawingMode.DRAW_EDIT_SAMPLER);
      controlWindow.setTitle("Sampler Synth");
      envDuration = 432;
      samplerEnv = envPreset("Percussion");
      isLoadToBoth = false;
      isAnimating = false;
      setAudioGain(-6.0f);
      daFilename = "workflow_48Khz.wav";
      loadAudioFile(new File(daPath + daFilename));
      daFilename = "workFlowPanel.png";
      preloadFiles(daPath, daFilename);
      break;
    case '3': // GLITCH_CORTO('3')
      setMode(DrawingMode.DRAW_EDIT_GRANULAR);
      this.doPlayOnNewBrush = true;
      this.doPlayWhileDrawing = true;
      this.usePitchedGrains = true;
      this.pitchJitter = 0.1f;
      break;
    case '4': // GLITCH_LARGO('4')
      break;
    case '5': // REPRISE
      this.doPlayOnNewBrush = true;
      this.doPlayWhileDrawing = false;
      this.usePitchedGrains = false;
      this.pitchJitter = 0.1f;
      fileSelected(new File(daPath + "session_02/dbwf_02_session.json"));
      resetConfigToDefaults();
      break;
    case '6': // CLOSE
      fileSelected(new File(daPath + "session_02/dbwf_02_session.json"));
      resetConfigToDefaults();
      break;
    }
  } else {    // Bagatelle "Abstract Jailbreak"
    applyColorMapOnLoad = true;
    isLoadToBoth = true;
    switch (key) {
    case '1': // preset = DURATION_5SEC_SWELL('1')
      if (nd != null) {
        this.nd.oscSendOnOff(1, true);
        println("-- trig 1 -- reverb ON");
      }
      loadAudioFile(new File(daPath + "bag_1_gest_1_tail.wav"));
      this.doPlayOnNewBrush = true;
      this.doPlayWhileDrawing = false;
      this.isAutoOptimize = true;
      this.isAddDynamics = true;
      float[] timesMs = new float[] {0, 500, 2500, 5000};
      float dur = timesMs[timesMs.length - 1];
      float[] values = new float[] {0.1f, 1.0f, 1.0f, 0.1f};
      float[] times = new float[timesMs.length];
      for (int i = 0; i < times.length; i++) {
        times[i] =  map(timesMs[i], 0f, dur, 0f, 1f);
      }
      this.dynamics = new PAKeyframeControlCurve (times, values);
      break;
    case '2': // preset = LONG_ECHO('2')
      this.doPlayOnNewBrush = true;
      this.doPlayWhileDrawing = false;
      this.isAutoOptimize = true;
      this.isAddDynamics = true;
      loadAudioFile(new File(daPath + "bag_1_gest_2_tail.wav"));
      break;
    case '3': // preset = GLITCH_CORTO('3')
      if (nd != null) {
        this.nd.oscSendTrig(0);
        println("-- trig 0 -- reverb OFF");
      }
      this.doPlayOnNewBrush = false;
      this.doPlayWhileDrawing = true;
      this.usePitchedGrains = true;
      this.pitchJitter = 0.1f;
      loadAudioFile(new File(daPath + "bag_1_crackle.wav"));
      //doPlayWhileDrawing = true;
      break;
    case '4': // preset = GLITCH_LARGO('4')
      this.doPlayOnNewBrush = true;
      this.doPlayWhileDrawing = true;
      this.usePitchedGrains = true;
      this.pitchJitter = 0.5f;
      break;
    case '5': // preset = SIXTEENTHS('5')
      this.doPlayOnNewBrush = false;
      this.doPlayWhileDrawing = false;
      this.isAutoOptimize = false;
      this.usePitchedGrains = false;
      this.isAddDynamics = true; // ???
      loadAudioFile(new File(daPath + "bag_1_newSpiral.wav"));
      break;
    case '6': // preset = DYNAMICS_1('6')
      this.doPlayOnNewBrush = true;
      this.doPlayWhileDrawing = false;
      this.isAutoOptimize = true;
      this.usePitchedGrains = false;
      loadAudioFile(new File(daPath + "High_Swells.wav"));
      break;
    default:
    }
  }
}

void resetPerformanceState() {
  doPlayOnNewBrush = false;    // play audio when a curve is drawn
  doPlayWhileDrawing = false;  // play audio events while drawing, or not
  isAutoOptimize = false;      // optimize the freshly drawn curve before playing it
  doMagicClick = false;        // find a brush that starts in a selected rectangle and play it
  usePitchedGrains = false;    // jitter granular pitch if true
  pitchJitter = 0.0167f;       // amount of jitter
  applyColorMapOnLoad = true;  // if true, apply the color map when a file is loaded
  isReplaceBrushes = true;     // if true, replace current brushes when we load a session or library folder
  isSaveSession = true;        // save the session brushes ('J' key commend)
  isRaining = false;           // if true, run random audio/animation point events

  isAddDynamics = false;       // add dynamics curve to gestures
  // dynamics for sampler and granular gestures
  dynamics = new PAKeyframeControlCurve (
    new float[] {0.0f, 0.5f, 1.0f}, // times
    new float[] {0.1f, 1.0f, 0.1f} );    // values

  isBrushTransformTest = false;      // testing animation feature (key 'y')
  isBrushTransformFrozen = false;    // freeze animation (key 'Y')

  isBrushSelectionModal = false;     // if false, select all brushes, otherwise, select by active mode
}

/*----------------------------------------------------------------*/
/*                   END PERFORMANCE METHODS                      */
/*----------------------------------------------------------------*/
