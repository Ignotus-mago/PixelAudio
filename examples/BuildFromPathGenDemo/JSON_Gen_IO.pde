// -------------------------- //
//       JSON FILE I/O        //
// -------------------------- //

public void exportGenData(PixelMapGen gen) {
  currentGen = gen;
  selectOutput("Select a file to write to:", "fileSelectedWrite");
}

public void fileSelectedWrite(File selection) {
  if (selection == null) {
    println("Window was closed or the user hit cancel.");
    return;
  }
  println("User selected " + selection.getAbsolutePath());
  // Do we have a .json at the end?
  if (selection.getName().length() < 5 || selection.getName().indexOf(".json") != selection.getName().length() - 5) {
    // missing ".json"
    currentFileName = selection.getAbsolutePath() + ".json"; 
  } 
  else {
    currentFileName = selection.getAbsolutePath();
  }
  JSONObject genJSON = new JSONObject();
  genJSON.setJSONObject("header", getJSONHeader());
  genJSON.setString("PXAU", "BGEN");
  genJSON.setInt("width", currentGen.getWidth());
  genJSON.setInt("height", currentGen.getHeight());
  JSONArray pixelMapJSON = new JSONArray();
  int[] pixelMap = currentGen.getPixelMap();
  for (int i = 0; i < currentGen.getSize(); i++) {
    pixelMapJSON.append(pixelMap[i]);
  }
  genJSON.setJSONArray("pixelMap", pixelMapJSON);
  saveJSONObject(genJSON, currentFileName);
}

public JSONObject getJSONHeader() {
  // flag this JSON file as WaveSynthEditor data using a "PXAU" key with value "WSYN"
  // add some other pertinent information
  JSONObject header = new JSONObject();
  header.setString("PXAU", "BGEN");
  header.setString("description", "BuildFromPathGen data created with the PixelAudio library by Paul Hertz.");
  header.setString("PixelAudioURL", "https://github.com/Ignotus-mago/PixelAudio");
  return header;
}


public void importGenData() {
  // we only use this data for this example, so it's in the usual "data" folder
  File folderToStartFrom = new File(dataPath("") + "//*.json");
  selectInput("Select a file to open", "fileSelectedOpen", folderToStartFrom);
}

public void fileSelectedOpen(File selection) {
  if (selection == null) {
    println("Window was closed or the user hit cancel.");
    return;
  }
  File currentDataFile = selection;
  println("User selected " + currentDataFile.getAbsolutePath());
  currentFileName = currentDataFile.getAbsolutePath();
  JSONObject json = loadJSONObject(currentFileName);
  boolean goodHeader = checkJSONHeader(json, "PXAU", "BGEN");
  if (goodHeader) {
    println("--->> JSON file contains BuildFromPathGen data. It should load correctly.");
  }
  else {
    println("--->> JSON file apparently does not contain BuildFromPathGen data. Will try to load,anyhow.");
  }
  PixelMapGen myGen = importGenDataJSON(json);
  if (myGen != null) {
    println("----->>> Loading PixelMapGen from JSON file. ");
    currentGen = myGen;
    mapper.setGenerator(currentGen);
    if (mapper.getWidth() != width || mapper.getHeight() != height) {
      windowResize(mapper.getWidth(), mapper.getHeight());
    }
    mapImage = createImage(width, height, RGB);
    mapImage.loadPixels();
    colors = getColors();
    mapper.plantPixels(colors, mapImage.pixels, 0, mapper.getSize());
    mapImage.updatePixels();
  }
}

boolean checkJSONHeader(JSONObject json, String key, String val) {
  JSONObject header = (json.isNull("header") ? null : json.getJSONObject("header"));
  String pxau;
  if (header != null) {
    pxau = (header.isNull(key)) ? "" : header.getString(key);  
  }
  else {
    pxau = (json.isNull(key)) ? "" : json.getString(key);
  }
  if (pxau.equals(val)) {
    return true;
  }
  else {
    return false;
  }
}

public PixelMapGen importGenDataJSON(JSONObject json) {
  int w = (json.isNull("width")) ? 0 : json.getInt("width");
  int h = (json.isNull("height")) ? 0 : json.getInt("height");
  JSONArray map = (json.isNull("pixelMap")) ? null : json.getJSONArray("pixelMap");
  if (map != null && w != 0 && h != 0) {
    BuildFromPathGen myGen = new BuildFromPathGen(w, h);
    // BuildFromPathGen myGen = new BuildFromPathGen(w, h, AffineTransformType.ROT90);
    int[] pixelMap = map.toIntArray();
    myGen.setPixelMap(pixelMap);
    myGen.generate();
    return myGen;
  } 
  else {
    return null;
  }
}
