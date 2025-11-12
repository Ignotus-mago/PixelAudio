/*----------------------------------------------------------------*/
/*                                                                */
/*                     FILE I/O METHODS                           */
/*                                                                */
/*----------------------------------------------------------------*/

// -------- BEGIN FILE I/O FOR APPLYING COLOR --------- //

/* 
 * Here is a special section of code for TutorialOne and other applications that
 * color a grayscale image with color data from a file. The color and saturation
 * come from the selected file, the brightness (gray values, more or less) come
 * from an image you supply, such as display image. 
 */


/**
 * Call to initiate process of opening an image file to get its color data.
 */
public void chooseColorImage() {
  selectInput("Choose an image file to apply color: ", "colorFileSelected");
}

/**
 * callback method for chooseColorImage() 
 * @param selectedFile    the File the user selected
 */
public void colorFileSelected(File selectedFile) {
  if (null != selectedFile) {
    String filePath = selectedFile.getAbsolutePath();
    String fileName = selectedFile.getName();
    String fileTag = fileName.substring(fileName.lastIndexOf('.') + 1);
    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
    if (fileTag.equalsIgnoreCase("png") || fileTag.equalsIgnoreCase("jpg") || fileTag.equalsIgnoreCase("jpeg")) {
      imageFile = selectedFile;
      imageFilePath = filePath;
      imageFileName = fileName;
      imageFileTag = fileTag;
      println("--- Selected color file "+ fileName +"."+ fileTag);
      // apply the color data (hue, saturation) in the selected image to our display image, mapImage
      applyImageColor(imageFile, mapImage);
    } 
    else {
      println("----- File is not a recognized image format ending with \"png\", \"jpg\", or \"jpeg\".");
    }
  } 
  else {
    println("----- No file was selected.");
  }
}

/**
 * Apply the hue and saturation of a chosen image file to the brightness channel of the display image.
 * @param imgFile        selected image file, source of hue and saturation values
 * @param targetImage    target image where brightness will remain unchanged
 */
public void applyImageColor(File imgFile, PImage targetImage) {
  PImage colorImage = loadImage(imgFile.getAbsolutePath());
  int w = colorImage.width > mapImage.width ? targetImage.width : colorImage.width;
  int h = colorImage.height > targetImage.height ? targetImage.height : colorImage.height;
  float[] hsbPixel = new float[3];
  colorImage.loadPixels();
  int[] colorSource = colorImage.pixels;
  targetImage.loadPixels();
  int[] graySource = targetImage.pixels;
  for (int y = 0; y < h; y++) {
    int rowStart = y * w;
    for (int x = 0; x < w; x++) {
      int cPos = rowStart + x;
      int gPos = y * targetImage.width + x;
      graySource[gPos] = PixelAudioMapper.applyColor(colorSource[cPos], graySource[gPos], hsbPixel);
    }
  }
  targetImage.updatePixels();
}

// -------- END FILE I/O FOR APPLYING COLOR --------- //

/*
 * Here is a section of "regular" file i/o methods for audio and image files.
 */


/**
 * Wrapper method for Processing's selectInput command
 */
public void chooseFile() {
  selectInput("Choose an audio file or an image file: ", "fileSelected");
}

/**
 * callback method for chooseFile(), handles standard audio and image formats for Processing.
 * If a file has been successfully selected, continues with a call to loadAudioFile() or loadImageFile().
 * 
 * @param selectedFile    the File the user selected
 */
public void fileSelected(File selectedFile) {
  if (null != selectedFile) {
    String filePath = selectedFile.getAbsolutePath();
    String fileName = selectedFile.getName();
    String fileTag = fileName.substring(fileName.lastIndexOf('.') + 1);
    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
    if (fileTag.equalsIgnoreCase("mp3") || fileTag.equalsIgnoreCase("wav") || fileTag.equalsIgnoreCase("aif")
        || fileTag.equalsIgnoreCase("aiff")) {
      // we chose an audio file
      audioFile = selectedFile;
      audioFilePath = filePath;
      audioFileName = fileName;
      audioFileTag = fileTag;
      println("----- Selected file " + fileName + "." + fileTag + " at "
          + filePath.substring(0, filePath.length() - fileName.length()));
      loadAudioFile(audioFile);
    } 
    else if (fileTag.equalsIgnoreCase("png") || fileTag.equalsIgnoreCase("jpg")
        || fileTag.equalsIgnoreCase("jpeg")) {
      // we chose an image file
      imageFile = selectedFile;
      imageFilePath = filePath;
      imageFileName = fileName;
      imageFileTag = fileTag;
      loadImageFile(imageFile);
    } 
    else {
      println("----- File is not a recognized audio format ending with \"mp3\", \"wav\", \"aif\", or \"aiff\".");
    }
  } 
  else {
    println("----- No audio or image file was selected.");
  }
}

/**
 * Attempts to load audio data from a selected file into playBuffer, then calls
 * writeAudioToImage() to transcode audio data and write it to mapImage
 * 
 * @param audFile    an audio file
 */
public void loadAudioFile(File audFile) {
  // read audio file into our MultiChannelBuffer, buffer size will be adjusted to match the file
  float fileSampleRate = minim.loadFileIntoBuffer(audFile.getAbsolutePath(), playBuffer);
  println("----- file sample rate is "+ fileSampleRate);
  // sampleRate > 0 means we read audio from the file
  if (fileSampleRate > 0) {
    // save the length of the buffer as read from the file, for future use
    this.audioFileLength = playBuffer.getBufferSize();
    // resize the buffer to mapSize, if necessary -- audio data will not be overwritten
    if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
    // read channel 0 the buffer into audioSignal, truncated or padded to fit mapSize
    audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
    audioLength = audioSignal.length;
    // load the buffer of our PASamplerInstrument
    synth.setBuffer(playBuffer, fileSampleRate);
    // automatically write the signal to mapImage --- this will change in later tutorials
    writeAudioToImage(audioSignal, mapper, mapImage, chan);
  }
}

/**
 * Transcodes audio data in sig[] and writes it to color channel chan of mapImage 
 * using the lookup tables in mapper to redirect indexing. Calls mapper.mapSigToImg(), 
 * which will throw an IllegalArgumentException if sig.length != img.pixels.length
 * or sig.length != mapper.getSize(). 
 * 
 * @param sig         an array of float, should be audio data in the range [-1.0, 1.0]
 * @param mapper      a PixelAudioMapper
 * @param img    a PImage
 * @param chan        a color channel
 */
public void writeAudioToImage(float[] sig, PixelAudioMapper mapper, PImage img, PixelAudioMapper.ChannelNames chan) {
  // If sig.length == mapper.getSize() == mapImage.width * mapImage.height, we can call safely mapper.mapSigToImg()  
  img.loadPixels();
  mapper.mapSigToImg(sig, img.pixels, chan);
  img.updatePixels();
}

/**
 * Attempts to load image data from a selected file into mapImage, then calls writeImageToAudio() 
 * to transcode HSB brightness color data from the image to audio and writes it to playBuffer and audioSignal.
 * 
 * @param imgFile    an image file
 */
public void loadImageFile(File imgFile) {
  PImage img = loadImage(imgFile.getAbsolutePath());
  // stash information about the image in imgFileWidth, imageFileHeight for future use
  imageFileWidth = img.width;
  imageFileHeight = img.height;
  // calculate w and h for copying image to display (mapImage)
  int w = img.width > mapImage.width ? mapImage.width : img.width;
  int h = img.height > mapImage.height ? mapImage.height : img.height;
  if (chan == PixelAudioMapper.ChannelNames.ALL) {
    // copy the image directly using Processing copy command
    mapImage.copy(img, 0, 0, w, h, 0, 0, w, h);
  } 
  else {
    // copy only specified channels of the new image
    PImage mixImage = createImage(w, h, RGB);
    mixImage.copy(mapImage, 0, 0, w, h, 0, 0, w, h);
    img.loadPixels();
    mixImage.loadPixels();
    mixImage.pixels = PixelAudioMapper.pushChannelToPixel(img.pixels, mixImage.pixels, chan);
    mixImage.updatePixels();
    mapImage.copy(mixImage, 0, 0, w, h, 0, 0, w, h);
  }
  // prepare to copy image data to audio variables
  // resize the buffer to mapSize, if necessary -- signal will not be overwritten
  if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
  audioSignal = new float[mapSize];
  writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
  // now that the image data has been written to audioSignal, set playBuffer channel 0 to the new audio data
  playBuffer.setChannel(0, audioSignal);
  synth.setBuffer(playBuffer);
  audioLength = audioSignal.length;
}

/**
 * This method writes a color channel from the an image to playBuffer, fulfilling a 
 * central concept of the PixelAudio library: image is sound. Calls mapper.mapImgToSig(), 
 * which will throw an IllegalArgumentException if img.pixels.length != sig.length or 
 * img.width * img.height != mapper.getWidth() * mapper.getHeight(). 
 * 
 * @param img       a PImage, a source of data
 * @param mapper    a PixelAudioMapper, handles mapping between image and audio signal
 * @param sig       an target array of float in audio format 
 * @param chan      a color channel
 */
public void writeImageToAudio(PImage img, PixelAudioMapper mapper, float[] sig, PixelAudioMapper.ChannelNames chan) {
  sig = mapper.mapImgToSig(img.pixels, sig, chan);
}    


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
  try {
    saveAudioToFile(audioSignal, sampleRate, fileName);
  } catch (IOException e) {
    println("--->> There was an error outputting the audio file " + fileName +", "  + e.getMessage());
  } catch (UnsupportedAudioFileException e) {
    println("--->> The file format is unsupported " + e.getMessage());
  }
}

/**
 * Saves audio data to 16-bit integer PCM format, which Processing can also open.
 * This same method can be called as a static method in PixelAudio.
 * 
 * @param samples       an array of floats in the audio range (-1.0f, 1.0f)
 * @param sampleRate    audio sample rate for the file
 * @param fileName      name of the file to save to
 * @throws IOException  an Exception you'll need to handle to call this method 
 * @throws UnsupportedAudioFileException    another Exception 
 */
public void saveAudioToFile(float[] samples, float sampleRate, String fileName)
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
  saveImageToFile(mapImage, fileName);
}

public void saveImageToFile(PImage img, String fileName) {
  img.save(fileName);
}
