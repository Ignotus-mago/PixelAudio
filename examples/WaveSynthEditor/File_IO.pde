//-----------------------------------------------------------//
/* ----->>>           BEGIN JSON FILE I/O           <<<----- */
//-----------------------------------------------------------//

// select a file of WaveData objects in JSON format to open
public void loadWaveData() {
    File folderToStartFrom = new File(dataPath("") + jsonFolder + "//*.json");
    selectInput("Select a file to open", "fileSelectedOpen", folderToStartFrom);
}
public void fileSelectedOpen(File selection) {
    if (selection == null) {
        println("Window was closed or the user hit cancel.");
        isAnimating = oldIsAnimating;
        return;
    }
    currentDataFile = selection;
    println("User selected " + selection.getAbsolutePath());
    currentFileName = selection.getAbsolutePath();
    json = loadJSONObject(currentFileName);
    setWaveSynthFromJSON(json, wavesynth);
    surface.setTitle(currentFileName);
    isAnimating = oldIsAnimating;
}
/**
 * Sets the fields of a WaveSynth using values stored in a JSON object. 
 * @param json        a JSON object, typically read in from a file
 * @param synth        a WaveSynth
 */
public void setWaveSynthFromJSON(JSONObject json, WaveSynth synth) {
    // set animation globals and WaveSynth properties
    animSteps = (json.isNull("steps")) ? 240 : json.getInt("steps");
    synth.setAnimSteps(animSteps);
    animStop = (json.isNull("stop")) ? this.animSteps : json.getInt("stop");
    synth.setStop(animStop);
    float myGamma = (json.isNull("gamma")) ? 1.0f : json.getFloat("gamma");
    synth.setGamma(myGamma);
    comments = (json.isNull("comments")) ? "" : json.getString("comments");
    synth.setComments(comments);
    synth.setGain(json.isNull("blendFactor") ? 0.5f : json.getFloat("blendFactor"));
    synth.setVideoFilename((json.isNull("filename")) ? "wavesynth.mp4" : json.getString("filename"));
    this.videoFilename = synth.getVideoFilename();
    synth.setScaleHisto((json.isNull("scaleHisto")) ? false : json.getBoolean("scaleHisto"));
    if (synth.isScaleHisto()) {
        synth.setHistoHigh((json.isNull("histoHigh")) ? 255 : json.getInt("histoHigh"));
        synth.setHistoLow((json.isNull("histoLow")) ? 0 : json.getInt("histoLow"));
    }
    // now load the JSON wavedata into ArrayList<WaveData> waveDataList
    JSONArray waveDataArray = json.getJSONArray("waves");
    int datalen = waveDataArray.size();
    ArrayList<WaveData> waveDataList = new ArrayList<WaveData>(datalen);
    for (int i = 0; i < datalen; i++) {
        // load fields common to both old and new format
        JSONObject waveElement = waveDataArray.getJSONObject(i);
        float f = waveElement.getFloat("freq");
        float a = waveElement.getFloat("amp");
        float p = waveElement.getFloat("phase");
        // float pInc = waveElement.getFloat("phaseInc");
        float dc = 0.0f;
        if (!waveElement.isNull("dc")) {
            dc = waveElement.getFloat("dc");
        }
        JSONObject rgbColor = waveElement.getJSONObject("color");
        int c = color(rgbColor.getInt("r"), rgbColor.getInt("g"), rgbColor.getInt("b"));
        float cycles;
        cycles = waveElement.getFloat("cycles");
        // frequency, amplitude, phase, dc, cycles, color, steps
        WaveData wd = new WaveData(f, a, p, dc, cycles, c, animSteps);
        waveDataList.add(wd);
    }
    synth.setWaveDataList(waveDataList);
    currentWD = wavesynth.waveDataList.get(0);
    waveDataIndex = 0;
    synth.prepareAnimation();
    synth.renderFrame(0);
    loadGlobalPanelValues();
    loadWaveDataPanelValues(currentWD);
    printWaveData(synth);
}
/**
 * Outputs current wavesynth settings and WaveData list.
 */
public void printWaveData(WaveSynth synth) {
    java.nio.file.Path path = java.nio.file.Paths.get(currentFileName);
    String fname = path.getFileName().toString();
    println("\n--------=====>>> Current WaveSynth instance for file " + fname + " <<<=====--------\n");
    println("Animation steps: " + synth.getAnimSteps());
    // println("Stop frame: "+ waveAnimal.getAnimSteps());
    println("gain: " + synth.getGain());
    println("gamma: " + synth.getGamma());
    if (synth.isScaleHisto()) {
        println("scaleHisto: " + synth.isScaleHisto());
        println("histoLow: " + synth.getHistoLow());
        println("histoHigh: " + synth.getHistoHigh());
    }
    println(fname);
    println("video filename: " + synth.getVideoFilename());
    // println("WaveData list for: "+ videoFilename);
    for (int i = 0; i < synth.waveDataList.size(); i++) {
        WaveData wd = synth.waveDataList.get(i);
        println("  " + (i + 1) + ":: " + wd.toString());
    }
    println("comments: " + synth.getComments() +"\n");
}

public void saveWaveData() {
    if ((currentDataFile == null) || (currentDataFile.getAbsolutePath().equals(""))) {
        selectOutput("Select a file to write to:", "fileSelectedWrite");
    }
    else {
        selectOutput("Select a file to write to:", "fileSelectedWrite", currentDataFile);
    }
}
public void fileSelectedWrite(File selection) {
    if (selection == null) {
        println("Window was closed or the user hit cancel.");
        return;
    }
    WaveSynth synth = this.wavesynth;
    println("User selected " + selection.getAbsolutePath());
    // Do we have a .json at the end?
    if (selection.getName().length() < 5
            || selection.getName().indexOf(".json") != selection.getName().length() - 5) {
        // problem missing ".json"
        currentFileName = selection.getAbsolutePath() + ".json"; // very rough approach...
    } else {
        currentFileName = selection.getAbsolutePath();
    }
    // put WaveData objects into an array
    JSONArray waveDataArray = new JSONArray();
    JSONObject waveElement;
    WaveData wd;
    for (int i = 0; i < synth.waveDataList.size(); i++) {
        wd = synth.waveDataList.get(i);
        waveElement = new JSONObject();
        waveElement.setInt("index", i);
        waveElement.setFloat("freq", wd.freq);
        waveElement.setFloat("amp", wd.amp);
        waveElement.setFloat("phase", wd.phase);
        waveElement.setFloat("phaseInc", wd.phaseInc);
        waveElement.setFloat("cycles", wd.phaseCycles);
        waveElement.setFloat("dc", wd.dc);
        // BADSR settings
        int[] rgb = PixelAudioMapper.rgbComponents(wd.waveColor);
        JSONObject rgbColor = new JSONObject();
        rgbColor.setInt("r", rgb[0]);
        rgbColor.setInt("g", rgb[1]);
        rgbColor.setInt("b", rgb[2]);
        waveElement.setJSONObject("color", rgbColor);
        // append wave data to array
        waveDataArray.append(waveElement);
    }
    // put the array into an object that tracks other state variables
    JSONObject stateData = new JSONObject();
    stateData.setInt("steps", synth.animSteps);
    stateData.setInt("stop", animStop);
    stateData.setFloat("blendFactor", synth.gain);
    stateData.setInt("dataFormat", 2);
    stateData.setString("comments", synth.comments);
    // String videoName = selection.getName(); 
    String videoName = synth.videoFilename;
    if (videoName == null || videoName.equals("")) {
        videoName = selection.getName();
        if (videoName.indexOf(".json") != -1) {
            videoName = videoName.substring(0, videoName.indexOf(".json")) + ".mp4";
        } else {
            videoName += ".mp4";
        }
    }
    println("----->>> video name is " + videoName);
    synth.videoFilename = videoName; // ???
    stateData.setString("filename", videoName);
    stateData.setFloat("gamma", synth.gamma);
    stateData.setBoolean("scaleHisto", synth.isScaleHisto);
    stateData.setFloat("histoHigh", synth.histoHigh);
    stateData.setFloat("histoLow", synth.histoLow);
    stateData.setJSONArray("waves", waveDataArray);
    saveJSONObject(stateData, currentFileName);
    currentDataFile = new File(currentFileName);
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
