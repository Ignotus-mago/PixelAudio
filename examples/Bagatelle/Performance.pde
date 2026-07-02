/*                  BEGIN PERFORMANCE METHODS                     */
/*----------------------------------------------------------------*/

/**
 * @param key    key that was pressed (should be a number key)
 */
void numericKey(char key) {
	switch (pMode) {
	case ABSTRACT_JAILBREAK:
		runAJ_PerformancePreset(AJ_PerformancePreset.fromKey(key), key);
		break;
	case DEADBODYWORKFLOW:
		runDBWF_PerformancePreset(DBWF_PerformancePreset.fromKey(key), key);
		break;
	default:
		throw new IllegalStateException("Unhandled performance mode: " + pMode);
	}
}

/**
 * Handles cue and performance preset commands for "Abstract Jailbreak".
 * @param cue    a performance preset
 * @param key    char for the key that triggered the call
 */
public void runAJ_PerformancePreset(AJ_PerformancePreset cue, char key) {
    boolean changed = false;
    if (cue != null) {
        if (aj_presetStack.contains(cue)) {
            aj_presetStack.remove(cue);
        } else {
            aj_presetStack.add(cue);
            runPerformanceCue(key);
        }
        changed = true;
    } else if (key == '0') {
        aj_presetStack.clear();
        changed = true;
    }
    if (changed) println("-- cues: " + aj_presetStack);
}

/**
 * Handles cue and performance preset commands for "DEADBODYWORKFLOW".
 * @param cue    a performance preset
 * @param key    char for the key that triggered the call
 */
public void runDBWF_PerformancePreset(DBWF_PerformancePreset cue, char key) {
    boolean changed = false;
    if (cue != null) {
        if (dbwf_presetStack.contains(cue)) {
            dbwf_presetStack.remove(cue);
        } else {
            dbwf_presetStack.add(cue);
			// execute cues in anticipation of the preset immediately (load files, etc.)
			runPerformanceCue(key);
        }
        changed = true;
    } else if (key == '0') {
        dbwf_presetStack.clear();
        changed = true;
    }
    if (changed) println("-- cues: " + dbwf_presetStack);
}


/**
 * Runs cues for live performance of "Abstract Jailbreak" or "DEADBODYWORKFLOW".
 * @param key   a {@code char} used to trigger performance cues
 */
void runPerformanceCue(char key) {
	switch (pMode) {

	case ABSTRACT_JAILBREAK: {    // Bagatelle "Abstract Jailbreak"
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
		}  // switch (key)
		break;
	}  // case ABSTRACT_JAILBREAK

	case DEADBODYWORKFLOW: {
		switch (key) {
		case '1': // DRONE_RAINDROPS
			isLoadToBoth = true;
			applyColorMapOnLoad = true;
			setAudioGain(-24.0f);
			loadAudioFile(new File(daPath + "D-flat2_bassClar_window.wav"));
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
		}  // switch (key)
		break;
	}  // case DEADBODYWORKFLOW

	default:
		throw new IllegalStateException("Unhandled performance mode: " + pMode);

	}  // switch (pMode)
}

/**
 * Resets variables that may have been altered from expected base states by performance cues and presets.
 */
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
			new float[] {0.0f, 0.5f, 1.0f},      // times
			new float[] {0.1f, 1.0f, 0.1f} );    // values

	isBrushTransformTest = false;      // testing animation feature (key 'y')
	isBrushTransformFrozen = false;    // freeze animation (key 'Y')

	isBrushSelectionModal = false;     // if false, select all brushes, otherwise, select by active mode
}

/**
 * Changes the current performance mode during live performance. Currently there are
 * only two performance modes in Bagatelle, ABSTRACT_JAILBREAK and DEADBODYWORKFLOW.
 * @param newMode            a new performance mode
 * @param recolorAfterLoad   apply signal path spectrum to display image or not
 */
void setPerformanceMode(PerformanceMode newMode, boolean recolorAfterLoad) {
    if (newMode == null || newMode == pMode) return;

    suspendScheduledEvents();
    stopAllLoops();
    if (pool != null) pool.fadeOutAll();
    if (gDir != null) gDir.cancelAndReleaseAll();

    pMode = newMode;

    resetPerformanceState();
    resetConfigForMode();

    surface.setTitle(performanceTitle());

    multigen = loadPerformanceGen();
    mapper = new PixelAudioMapper(multigen);
    mapSize = mapper.getSize();
    boundsPolicy = PABoundsPolicy.fromWidthHeight(mapper.getWidth(), mapper.getHeight(), boundaryMode);
    colors = getColors(mapSize);
    initImages();
    initDrawing();
    initCustomSettings();

    if (recolorAfterLoad) applyColorMap();
}

void clearGranularActivity() {
    if (gDir != null) gDir.cancelAndReleaseAll();
}

void clearSynthEvents() {
    suspendScheduledEvents();
    stopAllLoops();
    if (pool != null) pool.releaseAll();
    if (gDir != null) gDir.cancelAndReleaseAll();
    println("-- released all audio events in queue");
}

/*----------------------------------------------------------------*/
/*----------------------------------------------------------------*/
/*                   END PERFORMANCE METHODS                      */
/*----------------------------------------------------------------*/
