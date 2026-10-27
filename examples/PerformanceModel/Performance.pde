	/*----------------------------------------------------------------*/
	/*                  BEGIN PERFORMANCE METHODS                     */
	/*----------------------------------------------------------------*/

	/**
	 * @param key    key that was pressed (should be a number key)
	 */
	void numericKey(char key) {
		runOK_PerformancePreset(OK_PerformancePreset.fromKey(key), key);
	}

	/** Toggle a user preset, or clear all presets when {@code key} is {@code '0'}. */
	public void runOK_PerformancePreset(OK_PerformancePreset cue, char key) {
		boolean changed = false;
		if (cue != null) {
			if (ok_presetStack.contains(cue)) {
				ok_presetStack.remove(cue);
			} else {
				ok_presetStack.add(cue);
				runPerformanceCue(key);
			}
			changed = true;
		} else if (key == '0') {
	        ok_presetStack.clear();
	        changed = true;
		}
	    if (changed) println("-- cues: " + ok_presetStack);
	}


	/** Runs the application-level cue associated with a preset key. */
	void runPerformanceCue(char key) {
		switch(key) {
			case '1':   // NEW_DURATION
				isLoadToBoth = true;
				applyColorMapOnLoad = true;
				doPlayOnNewBrush = true;
				break;
			case '2': // WEIRD
				break;
			case '3': // RANDOM_DYNAMICS
				isAddDynamics = true;
				break;
			case '4': // BLANK
				// an odd way to give a command, but possible
				this.playBrushstrokes(BrushPlaybackMode.BOTH, 10, true);
				break;
		}
	}

	/**
	 *
	 */
	public void setPoolAttributes(int size, int maxVoices) {
		pool.setPoolSize(size);           // testing
		pool.setMaxVoices(maxVoices);     // testing
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
	/*                   END PREFORMANCE METHODS                      */
	/*----------------------------------------------------------------*/
