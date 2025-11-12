// ------------- WaveData methods ------------- //

/**
 * Scales the amplitude of an ArrayList of WaveData objects.
 *
 * @param waveDataList an ArrayList of WaveData objects
 * @param scale        the amount to scale the amplitude of each WaveData object
 */
public void scaleAmps(ArrayList<WaveData> waveDataList, float scale) {
  int i = 0;
  for (WaveData wd : waveDataList) {
    if (wd.isMuted) {
      i++;
      continue;
    }
    wd.setAmp(wd.amp * scale);
    if (isVerbose) println("----- set amplitude " + i + " to " + wd.amp);
    i++;
  }
}

/**
 * Shifts the colors of an ArrayList of WaveData objects.
 * @param waveDataList  an ArrayList of WaveData objects
 * @param shift    the amount shift each color
 */
public void shiftColors(ArrayList<WaveData> waveDataList, float shift) {
  for (WaveData wd : waveDataList) {
    if (wd.isMuted)
      continue;
    wd.setWaveColor(WaveSynthBuilder.colorShift(wd.waveColor, shift));
  }
  if (isVerbose) println("----->>> shift colors " + shift);
}

/**
 * Scales the frequencies of an ArrayList of WaveData objects.
 * @param waveDataList  an ArrayList of WaveData objects
 * @param scale    the amount to scale the frequency of each WaveData object
 */
public void scaleFreqs(ArrayList<WaveData> waveDataList, float scale) {
  int i = 0;
  for (WaveData wd : waveDataList) {
    if (wd.isMuted) {
      i++;
      continue;
    }
    wd.setFreq(wd.freq * scale);
    if (isVerbose) println("----- set frequency " + i + " to " + wd.freq);
  }
}

/**
 * Shifts the phase of an ArrayList of WaveData objects.
 * @param waveDataList  an ArrayList of WaveData objects
 * @param shift      amount to shift the phase of each WaveData object
 */
public void shiftPhases(ArrayList<WaveData> waveDataList, float shift) {
  for (WaveData wd : waveDataList) {
    if (wd.isMuted)
      continue;
    // wd.setPhase(wd.phase + shift - floor(wd.phase + shift));
    wd.setPhase(wd.phase + shift);
  }
  if (isVerbose) println("----->>> shiftPhase " + shift);
}

/**
 * Prints the phase values of an ArrayList of WaveData objects.
 * @param waveDataList  an ArrayList of WaveData objects
 */
public void showPhaseValues(ArrayList<WaveData> waveDataList) {
  int phaseStep = wavesynth.getStep();
  StringBuffer sb = new StringBuffer("\n----- current phase values scaled over (0, 1) -----\n");
  int i = 1;
  for (WaveData wd : waveDataList) {
    float m = wd.scaledPhaseAtFrame(phaseStep);
    sb.append(i++ +": "+ nf(m) + "; ");
  }
  sb.append("\n----- current phase values scaled over (0, TWO_PI) -----\n");
  i = 1;
  for (WaveData wd : waveDataList) {
    float m = wd.phaseAtFrame(phaseStep);
    sb.append(i++ +": "+ nf(m) + "; ");
  }
  println(sb);
}

/**
 * Applies the current phase values to the initial values of the WaveSynth, so that
 * the current state of the image display will appear as the first frame of
 * animation. Save the WaveSynth to a JSON file to keep the new phase values.
 *
 * @param waveDataList  an ArrayList of WaveData objects
 */
public void capturePhaseValues(ArrayList<WaveData> waveDataList) {
  int phaseStep = wavesynth.getStep();
  for (WaveData wd : waveDataList) {
    float currentPhase = wd.scaledPhaseAtFrame(phaseStep);
    wd.setPhase(currentPhase);
  }
}

/**
 * Mutes or unmutes a WaveData operator (view in the control panel).
 * @param elem  the index number of a WaveData object stored in a WaveSynth's waveDataList field
 */
public void toggleWDMute(int elem) {
  if (wavesynth.waveDataList.size() < elem + 1) return;
  WaveData wd = wavesynth.waveDataList.get(elem);
  wd.isMuted = !wd.isMuted;
  if (wd.isMuted) {
    wd.waveState = WaveState.MUTE;
  } 
  else {
    wd.waveState = WaveState.ACTIVE;
  }
  if (!isAnimating) {
    wavesynth.renderFrame(step);
  }
}

/**
 * Prints mute/active status of WaveData operators in supplied waveDataList.
 * @param waveDataList  an ArrayList of WaveData objects
 */
public void printWDStates(ArrayList<WaveData> waveDataList) {
  StringBuffer sb = new StringBuffer("Audio operators\n");
  int n = 1;
  for (WaveData wd : waveDataList) {
    sb.append("wave "+ (n++) +" "+ wd.waveState.toString() +", ");
  }
  println(sb.toString());
}

/**
 * Unmutes all the operators in supplied waveDataList.
 * @param waveDataList  an ArrayList of WaveData objects
 */
public void unmuteAllWD(ArrayList<WaveData> waveDataList) {
  StringBuffer sb = new StringBuffer("Audio operators\n");
  int n = 1;
  for (WaveData wd : waveDataList) {
    wd.setWaveState(WaveState.ACTIVE);
    sb.append("wave "+ (n++) +" "+ wd.waveState.toString() +", ");
  }
  println(sb.toString());
}

/**
 * Comparator class for sorting waveDataList by frequency or phase
 */
public class CompareWaveData implements Comparator <WaveData> {
  boolean isCompareFrequency = true;

  public int compare(WaveData wd1, WaveData wd2) {
    if (isCompareFrequency) {
      if (wd1.freq > wd2.freq) return 1;
      if (wd1.freq < wd2.freq) return -1;
    } else {
      if (wd1.phase > wd2.phase) return 1;
      if (wd1.phase < wd2.phase) return -1;
    }
    return 0;
  }
}

/**
 * Steps through the WaveSynth's list of WaveData, shows the current
 * WaveData operator in the control panel.
 * @param up   if true, increment waveDataIndex, otherwise, decrement it
 */
public void stepWaveData(boolean up) {
  int dataLen = wavesynth.waveDataList.size();
  if (up) {
    waveDataIndex = (waveDataIndex + 1 >= dataLen) ? 0 : waveDataIndex + 1;
  } else {
    waveDataIndex = (waveDataIndex - 1 >= 0) ? waveDataIndex - 1 : dataLen - 1;
  }
  currentWD = wavesynth.waveDataList.get(waveDataIndex);
  loadWaveDataPanelValues(currentWD);
}

public void incWaveData() {
  stepWaveData(true);
}

public void decWaveData() {
  stepWaveData(false);
}

//----- END WAVEDATA METHODS ----- //
