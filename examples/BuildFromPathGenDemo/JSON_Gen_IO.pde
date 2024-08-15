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
  if (selection.getName().length() < 5
    || selection.getName().indexOf(".json") != selection.getName().length() - 5) {
    // problem missing ".json"
    currentFileName = selection.getAbsolutePath() + ".json"; // very rough approach...
  } else {
    currentFileName = selection.getAbsolutePath();
  }
  JSONObject genJSON = new JSONObject();
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

public void importGenData() {
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
  PixelMapGen myGen = importGenDataJSON(json);
  if (myGen != null) {
    println("----->>> SUCCESS! ");
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

public PixelMapGen importGenDataJSON(JSONObject json) {
  int w = (json.isNull("width")) ? 0 : json.getInt("width");
  int h = (json.isNull("height")) ? 0 : json.getInt("height");
  JSONArray map = (json.isNull("pixelMap")) ? null : json.getJSONArray("pixelMap");
  if (map != null && w != 0 && h != 0) {
    BuildFromPathGen myGen = new BuildFromPathGen(w, h);
    int[] pixelMap = map.toIntArray();
    myGen.setPixelMap(pixelMap);
    myGen.generate();
    return myGen;
  }
  else {
    return null;
  } 
}
