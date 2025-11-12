// ------------- SAVE IMAGE FILE ------------- //

/**
 * Starts the image saving event chain.
 */
public void saveImage() {
  // File folderToStartFrom = new File(dataPath(""));
  selectOutput("Select an image file to write to:", "imageFileSelectedWrite");
}

/**
 * Handles image file output once an output file is selected.
 *
 * @param selection    an output file for the image, forwarded from saveImage()
 */
public void imageFileSelectedWrite(File selection) {
  if (selection == null) {
    println("Window was closed or the user hit cancel.");
    return;
  }
  String fileName = selection.getAbsolutePath();
  if (selection.getName().indexOf(".png") != selection.getName().length() - 4) {
    fileName += ".png";
  }
  saveImageToFile(drawOffscreen(), fileName);
}

/**
 * Saves display image to a specified file.
 *
 * @param img         image to save, reference to a PImage
 * @param fileName    name of the file to save, typically a fully qualified file path + file name
 */
public void saveImageToFile(PImage img, String fileName) {
  img.save(fileName);
}

/**
 * @return    a PImage generated in an offscreen PGraphics buffer.
 */
public PImage drawOffscreen() {
  PGraphics offscreen = createGraphics(width, height);
  offscreen.beginDraw();
  offscreen.background(bgColor);
  updateArgosies();
  if (isShowArgo1) offscreen.image(argo1Image, 0, 0);
  if (isShowArgo2) offscreen.image(argo2Image, 0, 0);
  offscreen.endDraw();
  return offscreen.get();
}

/**
 * Save audio buffer to a file
 */
public void saveToAudio(boolean isStereo) {
  renderSignals();
  if (!isStereo) {
    try {
      PixelAudio.saveAudioToFile(argo1Signal, sampleRate, "argo1_" + fileIndex + ".wav");
    }
    catch (IOException e) {
      println("--->> There was an error outputting the audio file argo1_"+ fileIndex +".wav"+ e.getMessage());
    }
    catch (UnsupportedAudioFileException e) {
      println("--->> The file format is unsupported " + e.getMessage());
    }
    try {
      PixelAudio.saveAudioToFile(argo2Signal, sampleRate, "argo2_" + fileIndex + ".wav");
    }
    catch (IOException e) {
      println("--->> There was an error outputting the audio file argo2_"+ fileIndex +".wav"+ e.getMessage());
    }
    catch (UnsupportedAudioFileException e) {
      println("--->> The file format is unsupported " + e.getMessage());
    }
    fileIndex++;
  } 
  else {
    try {
      PixelAudio.saveStereoAudioToFile(argo1Signal, argo2Signal, sampleRate, "argo1+2_" + fileIndex + ".wav");
    }
    catch (IOException e) {
      println("--->> There was an error outputting the audio file argo1+2"+ fileIndex +".wav"+ e.getMessage());
    }
    catch (UnsupportedAudioFileException e) {
      println("--->> The file format is unsupported " + e.getMessage());
    }
    fileIndex++;
  }
}
