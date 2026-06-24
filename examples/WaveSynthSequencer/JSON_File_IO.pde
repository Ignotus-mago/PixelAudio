//-------------------------------------------//
//               JSON FILE I/O               //
//-------------------------------------------//


/**
 * Show an Open File dialog for JSON files
 */
public void loadWaveData() {
    File folderToStartFrom = new File(dataPath("") + jsonFolder + "//*.json");
    selectInput("Select a file to open", "fileSelectedOpen", folderToStartFrom);
}

/**
 * @param selection   a file selected from an Open File dialog
 */
public void fileSelectedOpen(File selection) {
    boolean success = WaveSynthBuilder.getJSONFromFile(selection, wavesynth);
    if (success) {
        currentDataFile = selection;
        currentFileName = selection.getAbsolutePath();
        println("User selected " + currentFileName);
        syncAfterWaveSynthLoad();
        renderSignal();
        surface.setTitle(currentFileName);
    }
    isWaveSynthAnimating = oldIsAnimating;
}

public void syncAfterWaveSynthLoad() {
    animSteps = wavesynth.getAnimSteps();
    animStop = wavesynth.getStop();
    myGamma = wavesynth.getGamma();
    comments = wavesynth.getComments();
    synthImage = wavesynth.mapImage;
    wavesynth.prepareAnimation();
    wavesynth.renderFrame(0);
    printWaveData(wavesynth);
}

/**
 * Outputs fields from a WaveSynth to the console
 */
public void printWaveData(WaveSynth synth) {
    String wavesynthDescription = WaveSynthBuilder.waveSynthAsString(synth);
    println(wavesynthDescription +"\n");
}

/**
 * Show a Save File dialog
 */
public void saveWaveData() {
    selectOutput("Select a file to write to:", "fileSelectedWrite");
}

public void fileSelectedWrite(File selection) {
    String jsonFileName = WaveSynthBuilder.saveWaveSynthJSON(this, selection, wavesynth);
    if (jsonFileName == null || jsonFileName.isEmpty()) return;
    currentDataFile = new File(jsonFileName);
    currentFileName = currentDataFile.getAbsolutePath();
    surface.setTitle(currentFileName);
}
