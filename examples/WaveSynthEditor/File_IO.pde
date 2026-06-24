//-----------------------------------------------------------//
/* ----->>>           BEGIN JSON FILE I/O           <<<----- */
//-----------------------------------------------------------//


// select a file of WaveData objects in JSON format to open
public void loadWaveData() {
  if (isAnimating) {
    toggleAnimation();    // avoid concurrent modification of WaveData
  }
  File folderToStartFrom = new File(dataPath("") + jsonFolder + "//*.json");
  selectInput("Select a file to open", "fileSelectedOpen", folderToStartFrom);
}

public void fileSelectedOpen(File selection) {
  boolean success = WaveSynthBuilder.getJSONFromFile(selection, wavesynth);
  if (success) {
    currentDataFile = selection;
    currentFileName = selection.getAbsolutePath();
    println("User selected " + currentFileName);
    surface.setTitle(currentFileName);
    syncEditorAfterWaveSynthLoad();
    loadGlobalPanelValues();
    loadWaveDataPanelValues(currentWD);
    printWaveData(wavesynth);
    markWaveSynthAudioDirty();
  }
  isAnimating = oldIsAnimating;
}

void syncEditorAfterWaveSynthLoad() {
  animSteps = wavesynth.getAnimSteps();
  animStop = wavesynth.getStop();
  comments = wavesynth.getComments();
  videoFilename = wavesynth.getVideoFilename();
  waveDataIndex = 0;
  currentWD = wavesynth.waveDataList.isEmpty() ? null : wavesynth.waveDataList.get(0);
  wavesynth.prepareAnimation();
  wavesynth.renderFrame(0);
}

/**
 * Outputs current wavesynth settings and WaveData list.
 */
public void printWaveData(WaveSynth synth) {
  String wavesynthDescription = WaveSynthBuilder.waveSynthAsString(synth);
  println(wavesynthDescription +"\n");
}

public void saveWaveData() {
  if ((currentDataFile == null) || (currentDataFile.getAbsolutePath().equals(""))) {
    selectOutput("Select a file to write to:", "fileSelectedWrite");
  } else {
    selectOutput("Select a file to write to:", "fileSelectedWrite", currentDataFile);
  }
}

public void fileSelectedWrite(File selection) {
  applyPendingWaveEdits();
  String jsonFileName = WaveSynthBuilder.saveWaveSynthJSON(this, selection, wavesynth);
  if (jsonFileName == null || jsonFileName.isEmpty()) return;
  currentDataFile = new File(jsonFileName);
  currentFileName = currentDataFile.getAbsolutePath();
  surface.setTitle(currentFileName);
}

//-------------------------------------------//
//             END JSON FILE I/O             //
//-------------------------------------------//

//-----------------------------------------------------------//
/* ----->>>      BEGIN IMAGE AND AUDIO FILE I/O     <<<----- */
//-----------------------------------------------------------//

public void saveToImage() {
  // File folderToStartFrom = new File(dataPath(""));
  selectOutput("Select an image file to write to:", "imageFileSelectedWrite");
}

public void imageFileSelectedWrite(File selection) {
  if (selection == null) {
    println("Window was closed or the user hit cancel.");
    return;
  }
  String fileName = selection.getAbsolutePath();
  if (selection.getName().indexOf(".png") != selection.getName().length() - 4) {
    fileName += ".png";
  }
  // saveImageToFile(mapImage, fileName);
  save(fileName);
}

public void saveImageToFile(PImage img, String fileName) {
  img.save(fileName);
}

/**
 * Saves audio data to 16-bit integer PCM format, which Processing can also open.
 *
 * @param samples            an array of floats in the audio range (-1.0f, 1.0f)
 * @param sampleRate        audio sample rate for the file
 * @param fileName            name of the file to save to
 * @throws IOException        an Exception you'll need to catch to call this method (see keyPressed entry for 's')
 * @throws UnsupportedAudioFileException        another Exception (see keyPressed entry for 's')
 */
public static void saveAudioToFile(float[] samples, float sampleRate, String fileName)
  throws IOException, UnsupportedAudioFileException {
  // Convert samples from float to 16-bit PCM
  byte[] audioBytes = new byte[samples.length * 2];
  int index = 0;
  for (float sample : samples) {
    // Scale sample to 16-bit signed integer
    int intSample = (int) (sample * 32767);
    // Convert to bytes
    audioBytes[index++] = (byte) (intSample & 0xFF);
    audioBytes[index++] = (byte) ((intSample >> 8) & 0xFF);
  }
  // Create an AudioInputStream
  ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
  AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
  AudioInputStream audioInputStream = new AudioInputStream(byteStream, format, samples.length);
  // Save the AudioInputStream to a WAV file
  File outFile = new File(fileName);
  AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outFile);
  println("----- saved WaveSynth data as an audio file: "+ outFile.getAbsolutePath());
}
