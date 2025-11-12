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
      // *****>>> NETWORKING <<<***** //
      if (nd != null) nd.oscSendFileInfo(filePath, fileName, fileTag);
    } else if (fileTag.equalsIgnoreCase("png") || fileTag.equalsIgnoreCase("jpg")
      || fileTag.equalsIgnoreCase("jpeg")) {
      // we chose an image file
      imageFile = selectedFile;
      imageFilePath = filePath;
      imageFileName = fileName;
      imageFileTag = fileTag;
      loadImageFile(imageFile);
    } else {
      println("----- File is not a recognized audio format ending with \"mp3\", \"wav\", \"aif\", or \"aiff\".");
    }
  } else {
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
  if (isBlending) {
    MultiChannelBuffer buff = new MultiChannelBuffer(1024, 1);
    fileSampleRate =  minim.loadFileIntoBuffer(audioFile.getAbsolutePath(), buff);
    if (fileSampleRate > 0) {
      println("---- file sample rate is "+ this.fileSampleRate);
      // TODO we're ignoring possibly different sampling rates in the playBuffer and buff, does it matter?
      blendInto(playBuffer, buff, 0.5f, false, -12.0f);    // mix audio sources without normalization
    }
  } else {
    // read audio file into our MultiChannelBuffer, buffer size will be adjusted to match the file
    fileSampleRate = minim.loadFileIntoBuffer(audFile.getAbsolutePath(), playBuffer);
    // sampleRate > 0 means we read audio from the file
    if (fileSampleRate > 0) {
      println("---- file sample rate is "+ this.fileSampleRate);
      // save the length of the buffer as read from the file, for future use
      this.audioFileLength = playBuffer.getBufferSize();
      // resize the buffer to mapSize, if necessary -- signal will not be overwritten
      if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
      // load the buffer of our PASamplerInstrument (created in initAudio(), on starting the sketch)
    }
  }
  synth.setBuffer(playBuffer, fileSampleRate);
  if (pool != null) pool.setBuffer(playBuffer, fileSampleRate);
  else pool = new PASamplerInstrumentPool(playBuffer, fileSampleRate, maxVoices, 1, audioOut, defaultEnv);
  // because playBuffer is used by synth and pool and should not change, while audioSignal changes
  // when the image animates, we don't want playBuffer and audioSignal to point to the same array
  // so we copy channel 0 of the buffer into audioSignal, truncated or padded to fit mapSize
  audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
  audioLength = audioSignal.length;
  if (isLoadToBoth) {
    writeAudioToImage(audioSignal, mapper, mapImage, chan);
  }
  totalShift = 0;    // reset animation shift when audio is reloaded
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
 * Normalizes a single-channel signal array to a target RMS level in dBFS.
 *
 * @param signal    The audio samples to normalize (modified in place)
 * @param targetDB  The target RMS level in decibels relative to full scale
 *                  (e.g. -3.0f for moderately loud, -12.0f for safe headroom)
 */
public static void normalize(float[] signal, float targetDB) {
  if (signal == null || signal.length == 0) return;
  // --- Step 1: Compute RMS of the signal ---
  float sumSq = 0f;
  for (float v : signal) {
    sumSq += v * v;
  }
  float rms = (float)Math.sqrt(sumSq / signal.length);
  // --- Step 2: Convert target dBFS to linear RMS value ---
  float targetRMS = (float)Math.pow(10.0, targetDB / 20.0);
  // --- Step 3: Compute and apply gain ---
  if (rms > 1e-6f) {
    float gain = targetRMS / rms;
    if (gain > 100.0f) gain = 100.0f; // safety limit
    for (int i = 0; i < signal.length; i++) {
      signal[i] *= gain;
    }
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
 * to transcode HSB brightness channel to audio and writes it to playBuffer and audioSignal.
 * If you want to load the image file and audio file separately, comment out writeImageToAudio().
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
    } else {
      // copy the image directly using Processing copy command
      mapImage.copy(img, 0, 0, w, h, 0, 0, w, h);
    }
  } else {
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
    writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
    // now that the image data has been written to audioSignal, set playBuffer channel 0 to the new audio data
    playBuffer.setChannel(0, audioSignal);
    audioLength = audioSignal.length;
    // load the buffer of our PASamplerInstrument (created in initAudio() on starting the sketch)
    synth.setBuffer(playBuffer);
    if (pool != null) pool.setBuffer(playBuffer);
    else pool = new PASamplerInstrumentPool(playBuffer, sampleRate, maxVoices, 1, audioOut, defaultEnv);
    // because playBuffer is used by synth and pool and should not change, while audioSignal changes
    // when the image animates, we don't want playBuffer and audioSignal to point to the same array
    // copy channel 0 of the buffer into audioSignal, truncated or padded to fit mapSize
    audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
    audioLength = audioSignal.length;
    totalShift = 0;    // reset animation shift when audio is reloaded
  }
}

public int setAlphaWithBlack(int argb, int alpha) {
  int[] c = PixelAudioMapper.rgbaComponents(argb);
  if (c[0] == c[1] && c[1] == c[2] && c[2] == 0) {
    alpha = 0;
  }
  return alpha << 24 | c[0] << 16 | c[1] << 8 | c[2];
}

public static int setAlpha(int argb, int alpha) {
  return (argb & 0x00FFFFFF) | (alpha << 24);
}

/**
 * This method writes a color channel from an image to playBuffer, fulfilling a
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
  }
  catch (IOException e) {
    println("--->> There was an error outputting the audio file " + fileName +", "  + e.getMessage());
  }
  catch (UnsupportedAudioFileException e) {
    println("--->> The file format is unsupported " + e.getMessage());
  }
}

/**
 * Saves audio data to 16-bit integer PCM format, which Processing can also open.
 * This same method can be called as a static method in PixelAudio.
 *
 * @param samples      an array of floats in the audio range (-1.0f, 1.0f)
 * @param sampleRate    audio sample rate for the file
 * @param fileName      name of the file to save to
 * @throws IOException    an Exception you'll need to handle to call this method (see keyPressed entry for 's')
 * @throws UnsupportedAudioFileException    another Exception (see keyPressed entry for 's')
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
