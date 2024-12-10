// ------------------------------------------- //
//          AUDIO and IMAGE FILE I/O           //
// ------------------------------------------- //

// ------------- LOAD AUDIO FILE ------------- //

/**
 * Calls up a dialog for choosing a file.
 */
public void chooseFile() {
  oldIsAnimating = isAnimating;
  isAnimating = false;
  isBufferStale = true;
  selectInput("Choose an audio file or an image file: ", "fileSelected");
}

/**
 * Entry point to load a file into audio or image data objects. 
 * Recognizes .mp3, .wav, .aif, .aiff, .png, .jpg, and .jpeg file types. 
 * Calls loadAudioFile() or loadImageFile(). Depending on current settings, 
 * may write audio to image or image to audio. If animation was suspended
 * during a chooseFile() dialog, restarts animation. 
 * 
 * @param selectedFile  the file to load
 */
public void fileSelected(File selectedFile) {
  if (null != selectedFile) {
    String filePath = selectedFile.getAbsolutePath();
    String fileName = selectedFile.getName();
    String fileTag = fileName.substring(fileName.lastIndexOf('.') + 1);
    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
    println("----->>> file selected "+ fileName);
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

/**
 * Attempts to load an audio file to audioBuffer. If successful, sets the current sampleRate to 
 * the file's sampleRate and loads it into audioSignal. If isLoadBoth is true, writes the
 * audio data to mapImage. 
 * 
 * @param audFile  the audio file to load
 */
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

/**
 * Loads channel 0 of audioBuffer to audioSignal, padding with zeros or truncating 
 * to fit mapSize number of values into audioSignal. 
 */
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

/**
 * Transcodes values in audioSignal to RGB data and writes it to mapImage. 
 * If isBlending is true, mixes the new RGB data with the current mapImage.
 */
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


/**
 * Attempts to load image data from a specified file. Stores the
 * data in mapImage by calling loadImagePixels(). If isLoadBoth is true, 
 * transcodes the image data to audio format and writes it to audioBuffer.
 * 
 * @param imgFile  image file to load
 */
public void loadImageFile(File imgFile) {
  PImage img = loadImage(imgFile.getAbsolutePath());
  loadImagePixels(img);
  if (isLoadBoth) writeImageToAudio();
}

/**
 * If global variable chan == ChannelNames.ALL, loads a supplied PImage to mapImage 
 * and applies blending if isBlending == true. If chan != ChannelNames.ALL, loads 
 * the image to channel chan of mapImage. 
 * 
 * @param img  A PImage to load to mapImage
 */
public void loadImagePixels(PImage img) {
  // TODO handle color channel setting for images
  int w = img.width > mapImage.width ? mapImage.width : img.width;
  int h = img.height > mapImage.height ? mapImage.height : img.height;
  if (chan != PixelAudioMapper.ChannelNames.ALL) {
    PImage mixImage = createImage(w, h, ARGB);
    mixImage.copy(mapImage, 0, 0, w, h, 0, 0, w, h);
    img.loadPixels();
    mixImage.loadPixels();
    mixImage.pixels = PixelAudioMapper.pushAudioPixel(img.pixels, mixImage.pixels, chan);
    mixImage.updatePixels();
    // TODO make it work!
    mapImage.copy(mixImage, 0, 0, w, h, 0, 0, w, h);
  } 
  else {
    if (isBlending) {
      PImage dest = mapImage;
      PImage src = img;
      src.loadPixels();
      for (int i = 0; i < src.pixels.length; i++) {
        int pixel = src.pixels[i];
        src.pixels[i] = setAlphaWithBlack(pixel, 32);
      }
      src.updatePixels();
      dest.blend(src, 0, 0, src.width, src.height, 0, 0, dest.width, dest.height, BLEND);
    }
    else {
      mapImage.copy(img, 0, 0, w, h, 0, 0, w, h);
    }
  }
}

/**
 * Writes the RGB data in mapImage as transcoded audio samples to audioBuffer. 
 * Uses the data in the HSB brightness channel. 
 * 
 * TODO use Lab color space Lightness channel instead of HSB brightness. 
 */
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

/**
 * Puts up a dialog for saving an audio file.
 */
public void saveToAudio() {
  // File folderToStartFrom = new File(dataPath("") + "/");
  // selectOutput("Select an audio file to write to:", "audioFileSelectedWrite", folderToStartFrom);
  selectOutput("Select an audio file to write to:", "audioFileSelectedWrite");
}

/**
 * Write the audio data in audioSignal to a specified audio file. 
 * 
 * @param selection    an audio file for writing the audio data in audioSignal
 */
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

/**
 * Saves data in audioSignal to an audio file, typically to the Processing sketch folder.
 * 
 * @param fileName    the name of file to save
 */
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
 * Saves audio data to 16-bit integer PCM format, which Processing can open.
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


// ------------- SAVE IMAGE FILE ------------- //

/**
 * Puts up a file save dialog for selecting a file to write to.
 */
public void saveToImage() {
  // File folderToStartFrom = new File(dataPath("") + "/");
  // selectOutput("Select an image file to write to:", "imageFileSelectedWrite", folderToStartFrom);
  selectOutput("Select an image file to write to:", "imageFileSelectedWrite");
}

/**
 * Saves image to a file using PNG format.
 * 
 * @param selection    the file to save to
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
  // saveImageToFile(mapImage, fileName);
  save(fileName);
}

public void saveImageToFile(PImage img, String fileName) {
  img.save(fileName);
}
