
 // ------------------------------------------- //
 //          AUDIO and IMAGE FILE I/O           //
 // ------------------------------------------- //

 // ------------- LOAD AUDIO FILE ------------- //

public void chooseFile() {
  oldIsAnimating = isAnimating;
  isAnimating = false;
  isBufferStale = true;
  selectInput("Choose an audio file or an image file: ", "fileSelected");
}

public void fileSelected(File selectedFile) {
  if (null != selectedFile) {
    String filePath = selectedFile.getAbsolutePath();
    String fileName = selectedFile.getName();
    String fileTag = fileName.substring(fileName.lastIndexOf('.') + 1);
    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
    if (fileTag.equalsIgnoreCase("mp3") || fileTag.equalsIgnoreCase("wav") || fileTag.equalsIgnoreCase("aif")
        || fileTag.equalsIgnoreCase("aiff")) {
      audioFile = selectedFile;
      audioFilePath = filePath;
      audioFileName = fileName;
      audioFileTag = fileTag;
      println("----- Selected file " + fileName + "." + fileTag + " at "
          + filePath.substring(0, filePath.length() - fileName.length()));
      loadAudioFile(audioFile);
      isLoadBoth = false;
    } 
    else if (fileTag.equalsIgnoreCase("png") || fileTag.equalsIgnoreCase("jpg") || fileTag.equalsIgnoreCase("jpeg")) {
          imageFile = selectedFile;
          imageFilePath = filePath;
          imageFileName = fileName;
          imageFileTag = fileTag;
          loadImageFile(imageFile);
          isLoadBoth = true;
    }
    else {
      println("----- File is not a recognized audio format ending with \"mp3\", \"wav\", \"aif\", or \"aiff\".");
    }
  } 
  else {
    println("----- No audio file was selected.");
  }
  isAnimating = oldIsAnimating;
}

public void loadAudioFile(File audFile) {
  // read audio file into our MultiChannelBuffer
  float sampleRate = minim.loadFileIntoBuffer(audFile.getAbsolutePath(), audioBuffer);
  // sampleRate > 0 means we read audio from the file
  if (sampleRate > 0) {
    // read an array of floats from the buffer
    loadAudioSignal();
    // load rgbSignal with rgb gray values corresponding to the audio sample values
    if (isLoadBoth) writeAudioToImage();
  }
}

public void loadAudioSignal() {
  this.audioSignal = audioBuffer.getChannel(0);
  this.audioLength = audioSignal.length;
  if (audioLength < mapSize) {
    audioSignal = Arrays.copyOf(audioSignal, mapSize);
    audioLength = audioSignal.length;
    audioBuffer.setChannel(0, audioSignal);
  }
  if (audioLength > mapSize) {
    audioBuffer.setBufferSize(mapSize);
    audioSignal = audioBuffer.getChannel(0);
    audioLength = audioSignal.length;
  }
}

public void writeAudioToImage() {
  rgbSignal = mapper.pluckSamplesAsRGB(audioSignal, 0, mapSize);
  if (rgbSignal.length < mapSize) {
    // pad rgbSignal with 0's if necessary
    rgbSignal = Arrays.copyOf(rgbSignal, mapSize);
  }
  if (isBlending) {
    int alpha = 128;
    Arrays.setAll(rgbSignal, index -> PixelAudioMapper.setAlpha(rgbSignal[index], alpha));
  }
  mapImage.loadPixels();
  // write the rgbSignal pixels to mapImage, following the signal path
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, rgbSignal.length, chan);
  mapImage.updatePixels();
}


public void loadImageFile(File imgFile) {
  PImage img = loadImage(imgFile.getAbsolutePath());
  loadImagePixels(img);
  if (isLoadBoth) writeImageToAudio();
}

public void loadImagePixels(PImage img) {
  // TODO handle color channel setting for images
  int w = img.width > mapImage.width ? mapImage.width : img.width;
  int h = img.height > mapImage.height ? mapImage.height : img.height;
  if (chan != PixelAudioMapper.ChannelNames.ALL) {
    PImage mixImage = createImage(w, h, ARGB);
    mixImage.copy(mapImage, 0, 0, w, h, 0, 0, w, h);
    img.loadPixels();
    mixImage.loadPixels();
    mixImage.pixels = PixelAudioMapper.pushChannelToPixel(img.pixels, mixImage.pixels, chan);
    mixImage.updatePixels();
    // TODO make it work!
    mapImage.copy(mixImage,0, 0, w, h, 0, 0, w, h);
  }
  else {
    mapImage.copy(img,0, 0, w, h, 0, 0, w, h);
  }
}

public void writeImageToAudio() {
  // println("----- writing image to signal ");
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
  audioBuffer.setBufferSize(mapSize);
  mapImage.loadPixels();
  // fetch pixels from mapImage in signal order, put them in rgbSignal
  rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, rgbSignal.length);    
  // write the Brightness channel of rgbPixels, transcoded to audio range, to audioBuffer
  mapper.plantSamples(rgbSignal, audioBuffer.getChannel(0), 0, mapSize, PixelAudioMapper.ChannelNames.L);
}


// ------------- SAVE AUDIO FILE ------------- //

public void saveToAudio() {
  // File folderToStartFrom = new File(dataPath("") + "/");
  // selectOutput("Select an audio file to write to:", "audioFileSelectedWrite", folderToStartFrom);
  selectOutput("Select an audio file to write to:", "audioFileSelectedWrite");
}

public void audioFileSelectedWrite(File selection) {
  if (selection == null) {
      println("Window was closed or the user hit cancel.");
      return;      
  }
  String fileName = selection.getAbsolutePath();
  if (selection.getName().indexOf(".wav") != selection.getName().length() - 4) {
    fileName += ".wav";
  }
  saveAudioFile(fileName);
}

public void saveAudioFile(String fileName) {
  try {
    saveAudioToFile(audioSignal, sampleRate, fileName);
    println("Saved file to sketch path: "+ fileName);
  } catch (IOException e) {
    println("--->> There was an error outputting the audio file "+ fileName +".\n"+ e.getMessage());
  } catch (UnsupportedAudioFileException e) {
    println("--->> The file format is unsupported." + e.getMessage());
  }
}

/**
 * Saves audio data to 16-bit integer PCM format, which Processing can also
 * open.
 * 
 * @param samples    an array of floats in the audio range (-1.0f, 1.0f)
 * @param sampleRate audio sample rate for the file
 * @param fileName   name of the file to save to
 * @throws IOException                   an Exception you'll need to handle to
 *                                       call this method (see keyPressed entry
 *                                       for 's')
 * @throws UnsupportedAudioFileException another Exception (see keyPressed entry
 *                                       for 's')
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
}

// ------------- IMAGES ------------- //

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
