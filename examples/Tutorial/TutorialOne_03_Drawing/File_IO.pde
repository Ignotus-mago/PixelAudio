/*----------------------------------------------------------------*/
/*                                                                */
/*                   BEGIN FILE I/O METHODS                       */
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
    } else {
      println("----- File is not a recognized image format ending with \"png\", \"jpg\", or \"jpeg\".");
    }
  } else {
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
  oldIsAnimating = isAnimating;
  isAnimating = false;
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
  isAnimating = oldIsAnimating;
}

/**
 * Attempts to load audio data from a selected file into playBuffer, then calls
 * writeAudioToImage() to transcode audio data and write it to mapImage.
 * If you want to load the image file and audio file separately, comment out writeAudioToImage(). 
 * 
 * @param audFile    an audio file
 */
public void loadAudioFile(File audFile) {
  float[] resampled;
  MultiChannelBuffer buff = new MultiChannelBuffer(1024, 1);
  fileSampleRate =  minim.loadFileIntoBuffer(audioFile.getAbsolutePath(), buff);
  if (fileSampleRate > 0) {
    println("---- file sample rate is "+ this.fileSampleRate);
    if (fileSampleRate != audioOut.sampleRate()) {
      resampled = AudioUtility.resampleMonoToOutput(buff.getChannel(0), fileSampleRate, audioOut);
      buff.setBufferSize(resampled.length);
      buff.setChannel(0, resampled);
      //if (buff.getBufferSize() != mapSize) buff.setBufferSize(mapSize);
      fileSampleRate = audioOut.sampleRate();
    }
    // save the length of the file, possibly resampled, for future use
    this.audioFileLength = buff.getBufferSize();
  }
  else {
    println("-- Unable to load file. File may be empty, wrong format, or damaged.");
    return;
  }
  // everything looks good, proceed
  if (isBlending) {
    blendInto(playBuffer, buff, 0.5f, false, -12.0f);    // mix audio sources without normalization
  }
  else {
    // adjust buffer size to mapper.getSize()
    if (buff.getBufferSize() != mapper.getSize()) buff.setBufferSize(mapper.getSize());
    playBuffer = buff;
  }
  // ensureSamplerReady will load playBuffer to the Sampler synth "pool"
  ensureSamplerReady();
  // because playBuffer is used by synth and pool and should not change, while audioSignal changes
  // when the image animates, we don't want playBuffer and audioSignal to point to the same array
  // so we copy channel 0 of the buffer into audioSignal, truncated or padded to fit mapSize
  audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
  granSignal = audioSignal;
  audioLength = audioSignal.length;
  if (isLoadToBoth) {
    renderAudioToMapImage(chan, 0);
    commitMapImageToBaseImage();
  }
}

/**
 * Blends audio data from buffer "src" into buffer "dest" in place.
 *
 * The formula per sample is:
 *    dest[i] = weight * src[i] + (1 - weight) * dest[i]
 *
 * @param dest   Destination buffer (will be modified)
 * @param src    Source buffer to blend into dest
 * @param weight Blend ratio (0.0 = keep dest, 1.0 = replace with src)
 */
public static void blendInto(MultiChannelBuffer dest, MultiChannelBuffer src, float weight, boolean normalize, float targetDB) {
  // Clamp blend ratio to [0, 1]
  weight = Math.max(0f, Math.min(1f, weight));
  float invWeight = 1f - weight;
  // Match dimensions safely
  int channels = Math.min(dest.getChannelCount(), src.getChannelCount());
  int frames = Math.min(dest.getBufferSize(), src.getBufferSize());
  // Perform blending directly on dest channels
  for (int c = 0; c < channels; c++) {
    float[] d = dest.getChannel(c);
    float[] s = src.getChannel(c);
    for (int i = 0; i < frames; i++) {
      d[i] = weight * s[i] + invWeight * d[i];
    }
  }
  if (normalize) {
    for (int c = 0; c < channels; c++) {
      float[] d = dest.getChannel(c);
      normalize(d, targetDB);
    }

  }
}
  
/**
 * Normalizes a single-channel signal array to a target RMS level in dBFS (decibels relative to full scale).
 * 0 is the maximum digital amplitude. -6.0 dB is 50% of the maximum level. 
 *  
 * @param signal
 * @param targetPeakDB
 */
public static void normalize(float[] signal, float targetPeakDB) {
  AudioUtility.normalizeRmsWithCeiling(signal, targetPeakDB, -3.0f);
}
  
/**
 * Transcodes audio data in audioSignal and writes it to color channel chan of mapImage.
 * 
 * @param chan     A color channel
 * @param shift    number of index positions to shift the audio signal
 */
public void renderAudioToMapImage(PixelAudioMapper.ChannelNames chan, int shift) {
  // Render current audioSignal into mapImage using current mapper & current totalShift
  writeAudioToImage(audioSignal, mapper, mapImage, chan, shift);
}

/**
 * Transcodes audio data in sig[] and writes it to color channel chan of img 
 * using the lookup tables in mapper to redirect indexing. Calls mapper.mapSigToImgShifted(), 
 * which will throw an IllegalArgumentException if sig.length != img.pixels.length
 * or sig.length != mapper.getSize(). 
 * 
 * @param sig         an array of float, should be audio data in the range [-1.0, 1.0]
 * @param mapper      a PixelAudioMapper
 * @param img         a PImage
 * @param chan        a color channel
 * @param shift       the number of indices to shift when writing audio
 */
public void writeAudioToImage(float[] sig, PixelAudioMapper mapper, PImage img, PixelAudioMapper.ChannelNames chan, int shift) {
  img.loadPixels();
  mapper.mapSigToImgShifted(sig, img.pixels, chan, shift); // commit current phase
  img.updatePixels();
}

/**
 * Attempts to load image data from a selected file into mapImage, then calls writeImageToAudio() 
 * to transcode HSB brightness channel to audio and writes it to playBuffer and audioSignal.
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
    if (isBlending) {
      PImage dest = mapImage;
      PImage src = img;
      src.loadPixels();
      for (int i = 0; i < src.pixels.length; i++) {
        int pixel = src.pixels[i];
        src.pixels[i] = setAlphaWithBlack(pixel, 96);
      }
      src.updatePixels();
      dest.blend(src, 0, 0, src.width, src.height, 0, 0, dest.width, dest.height, BLEND);
    }
    else {
      // copy the image directly using Processing copy command
      mapImage.copy(img, 0, 0, w, h, 0, 0, w, h);
    }
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
  if (isLoadToBoth) {
    // prepare to copy image data to audio variables
    // resize the buffer to mapSize, if necessary -- signal will not be overwritten
    if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
    audioSignal = playBuffer.getChannel(0);
    renderMapImageToAudio(PixelAudioMapper.ChannelNames.L);
    // now that the image data has been written to audioSignal, set playBuffer channel 0 to the new audio data
    playBuffer.setChannel(0, audioSignal);
    audioLength = audioSignal.length;
    // load the buffer of our PASamplerInstrument (created in initAudio() on starting the sketch)
    ensureSamplerReady();
    // because playBuffer is used by synth and pool and should not change, while audioSignal changes
    // when the image animates, we don't want playBuffer and audioSignal to point to the same array
    // copy channel 0 of the buffer into audioSignal, truncated or padded to fit mapSize
    audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
    granSignal = audioSignal;
    audioLength = audioSignal.length;
  }
  commitMapImageToBaseImage();
}

/**
 * Sets the alpha channel of an RGBA color, conditionally setting alpha = 0 if all other channels = 0.
 * 
 * @param argb     an RGBA color value
 * @param alpha    the desired alpha value to apply to argb
 * @return         the argb color with changed alpha channel value
 */
public int setAlphaWithBlack(int argb, int alpha) {
  int[] c = PixelAudioMapper.rgbaComponents(argb);
  if (c[0] == c[1] && c[1] == c[2] && c[2] == 0) {
    alpha = 0;
  }
  return alpha << 24 | c[0] << 16 | c[1] << 8 | c[2];
}

/**
 * Sets the alpha channel of an RGBA color.
 * 
 * @param argb     an RGBA color value
 * @param alpha    the desired alpha value to apply to argb
 * @return         the argb color with changed alpha channel value
 */
public static int setAlpha(int argb, int alpha) {
    return (argb & 0x00FFFFFF) | (alpha << 24);
}

/**
 * This method writes a color channel from an image to playBuffer, fulfilling a 
 * central concept of the PixelAudio library: image is sound. Calls mapper.mapImgToSig(), 
 * which will throw an IllegalArgumentException if img.pixels.length != sig.length or 
 * img.width * img.height != mapper.getWidth() * mapper.getHeight(). 
 * Sets totalShift = 0 on completion: the image and audio are now in sync. 
 * 
 * @param img       a PImage, a source of data
 * @param mapper    a PixelAudioMapper, handles mapping between image and audio signal
 * @param sig       an target array of float in audio format 
 * @param chan      a color channel
 * @param shift     number of indices to shift 
 */
public void writeImageToAudio(PImage img, PixelAudioMapper mapper, float[] sig, PixelAudioMapper.ChannelNames chan, int shift) {
  // If img is the *display* (shifted) image, commit its phase into audio:
  sig = mapper.mapImgToSigShifted(img.pixels, sig, chan, shift);
}

/**
 * Writes a specified channel of mapImage to audioSignal.
 * 
 * @param chan    the selected color channel
 */
public void renderMapImageToAudio(PixelAudioMapper.ChannelNames chan) {
  writeImageToAudio(mapImage, mapper, audioSignal, chan, totalShift);
}

/**
 * Writes the mapImage, which may change with animation, to the baseImage, a reference image
 * that usually only changes when a new file is loaded.
 */
public void commitMapImageToBaseImage() {
  baseImage = mapImage.copy();
  totalShift = 0;
}

/**
 * Copies the supplied PImage to mapImage and baseImage, sets totalShift to 0 (the images are identical).
 * @param img
 */
public void commitNewBaseImage(PImage img) {
  baseImage = img.copy();
  mapImage = img.copy();
  totalShift = 0;
}

/**
 * Writes baseImage to mapImage with an index position offset of totalShift.
 */
public void refreshMapImageFromBase() {
  mapImage.loadPixels();
  mapper.copyPixelsAlongPathShifted(baseImage.pixels, mapImage.pixels, totalShift);
  mapImage.updatePixels();
}

/**
 * Calls Processing's selectOutput method to start the process of saving 
 * the current audio signal to a .wav file. 
 */
public void saveToAudio() {
  // File folderToStartFrom = new File(dataPath("") + "/");
  // selectOutput("Select an audio file to write to:", "audioFileSelectedWrite", folderToStartFrom);
  selectOutput("Select an audio file to write to:", "audioFileSelectedWrite");
}

/**
 * @param selection    a File to write as audio
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
 * This same method can be called as a static method in AudioUtility.
 * 
 * @param samples      an array of floats in the audio range (-1.0f, 1.0f)
 * @param sampleRate    audio sample rate for the file
 * @param fileName      name of the file to save to
 * @throws IOException    an Exception you'll need to handle to call this method
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

/**
 * Calls Processing's selectOutput method to start the process of saving 
 * the mapImage (the offscreen copy of the display image) to a .png file. 
 */
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
