  /*----------------------------------------------------------------*/
  /*                                                                */
  /*                     WINDOW BUFFER METHODS                      */
  /*                                                                */
  /*----------------------------------------------------------------*/

	// *** WindowedBuffer support *** //
	/** @return the active window size, or the mapper size before initialization */
	int windowSizeOrMapSize() {
		return (windowBuff != null) ? windowBuff.getWindowSize() : mapper.getSize();
	}

	// *** WindowedBuffer support *** //
	/** Rewinds the visible audio window to the beginning of the backing source. */
	public void resetAudioWindow() {
		if (windowBuff == null) return;
		windowBuff.reset();
		refreshWindowFromBacking(false);
		if (applyColorMapOnLoad) applyColorMapToDisplay(true);
	}

	// *** WindowedBuffer support *** //
	/** @param delta signed number of backing-source samples to move the window */
	public void moveAudioWindow(int delta) {
		if (windowBuff == null || backingSignal == null || backingSignal.length == 0) return;
		int next = PixelAudioMapper.wrap(windowBuff.getIndex() + delta, backingSignal.length);
		windowBuff.setIndex(next);
		refreshWindowFromBacking(false);
		if (applyColorMapOnLoad) applyColorMapToDisplay(true);
	}

	// *** WindowedBuffer support *** //
	/** Installs a full-file backing source and a mapper-sized moving window. */
	void installBackingSource(float[] sig, float sourceSampleRate) {
		int windowSize = (mapper != null) ? mapper.getSize() : mapSize;
		if (windowSize <= 0) return;
		int backingSize = Math.max(windowSize, (sig != null) ? sig.length : 0);
		backingSignal = new float[backingSize];
		if (sig != null) {
			System.arraycopy(sig, 0, backingSignal, 0, Math.min(sig.length, backingSignal.length));
		}
		backingBuffer = new MultiChannelBuffer(backingSignal.length, 1);
		backingBuffer.setChannel(0, backingSignal);
		bufferSampleRate = sourceSampleRate;
		audioFileLength = backingSignal.length;
		windowBuff = new WindowedBuffer(backingSignal, windowSize, windowHopSize);
		granSignal = backingSignal;
		println("---- WindowBuffer backing source length = " + backingSignal.length
				+ ", window size = " + windowBuff.getWindowSize()
				+ ", hop size = " + windowBuff.getHopSize());
	}

	// *** WindowedBuffer support *** //
	/** Copies the current backing-source window into the display and visible audio buffer. */
	void refreshWindowFromBacking(boolean advance) {
		if (windowBuff == null || backingSignal == null || backingSignal.length == 0) return;
		if (advance) windowBuff.nextWindow();
		int targetSize = mapper.getSize();
		int start = PixelAudioMapper.wrap(windowBuff.getIndex(), backingSignal.length);
		if (audioSignal == null || audioSignal.length != targetSize) audioSignal = new float[targetSize];
		for (int i = 0; i < targetSize; i++) {
			audioSignal[i] = backingSignal[(start + i) % backingSignal.length];
		}
		audioLength = audioSignal.length;
		if (playBuffer == null || playBuffer.getBufferSize() != targetSize) {
			playBuffer = new MultiChannelBuffer(targetSize, 1);
		}
		playBuffer.setChannel(0, audioSignal);
		totalShift = 0;
		writeAudioToImage(audioSignal, mapper, mapImage, chan);
		commitMapImageToBaseImage();
	}

	// *** WindowedBuffer support *** //
	/** Stops events and releases source bindings before installing a new backing buffer. */
	void suspendWindowAudio() {
		windowAudioReady = false;
		suspendScheduledEvents();
		stopAllLoops();
		if (pool != null && !pool.isClosed()) {
			pool.stopAll();
			pool.close();
		}
		pool = null;
		samplerSourceRef = null;
		samplerSourceRateRef = -1;
		if (gDir != null) gDir.cancelAndReleaseAll();
	}

	/**
	 * Calculates the index of the image pixel within the signal path,
	 * taking the shifting of pixels and audioSignal into account.
	 * See MusicBoxBuffer for use of a windowed buffer in this calculation.
	 *
	 * @param x    an x coordinate within mapImage and display bounds
	 * @param y    a y coordinate within mapImage and display bounds
	 * @return     the index of the sample corresponding to (x,y) on the signal path
	 */
	public int getSamplePos(int x, int y) {
		int local = mapper.lookupSignalPosShifted(x, y, totalShift);
		return backingIndexFromLocal(local);
	}

	/**
	 * Calculates the display image coordinates corresponding to a specified audio sample index.
	 * @param pos    an index into an audio signal, must be between 0 and width * height - 1.
	 * @return       a PVector with the x and y coordinates
	 */
	public PVector getCoordFromSignalPos(int pos) {
		int[] xy = this.mapper.lookupImageCoordShifted(pos, totalShift);
		return new PVector(xy[0], xy[1]);
	}

	// *** WindowedBuffer support *** //
	int backingIndexFromLocal(int localIndex) {
		if (windowBuff == null || backingSignal == null || backingSignal.length == 0) {
			return PixelAudioMapper.wrap(localIndex, Math.max(1, mapper.getSize()));
		}
		int local = PixelAudioMapper.wrap(localIndex, mapper.getSize());
		return PixelAudioMapper.wrap(windowBuff.getIndex() + local, backingSignal.length);
	}

	// *** WindowedBuffer support *** //
	float[] backingGranularSignal() {
		return (backingSignal != null && backingSignal.length > 0) ? backingSignal : audioSignal;
	}
