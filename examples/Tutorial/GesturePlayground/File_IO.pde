/*----------------------------------------------------------------*/
/*                                                                */
/*                FILE I/O METHODS (AUDIO ONLY)                   */
/*                                                                */
/*----------------------------------------------------------------*/

// ------------- SIMPLIFIED FILE I/O SECTION FOR GranularPlayground -------------
// To keep things simple and focused on our synthesis instruments
// we omit image file opening - we just handle audio

/**
 * Wrapper method for Processing's selectInput command
 */
public void chooseFile() {
  oldIsAnimating = isAnimating;
  isAnimating = false;
  selectInput("Choose an audio file to load to audio buffer and display image: ", "fileSelected");
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
 * Resamples files that are recorded with a different sample rate than the current audio output.
 * If you want to load the image file and audio file separately, comment out writeAudioToImage().
 *
 * @param audFile    an audio file
 */
public void loadAudioFile(File audFile) {
  MultiChannelBuffer buff = new MultiChannelBuffer(1024, 1);
  fileSampleRate =  minim.loadFileIntoBuffer(audFile.getAbsolutePath(), buff);
  if (fileSampleRate > 0) {
    if (fileSampleRate != audioOut.sampleRate()) {
      float[] resampled = AudioUtility.resampleMonoToOutput(buff.getChannel(0), fileSampleRate, audioOut);
      buff.setBufferSize(resampled.length);
      buff.setChannel(0, resampled);
      bufferSampleRate = sampleRate;
    } else {
      bufferSampleRate = fileSampleRate;
    }
    this.audioFileLength = buff.getBufferSize();
    println("---- file sample rate = "+ this.fileSampleRate
      +", buffer sample rate = "+ bufferSampleRate
      +", audio output sample rate = "+ audioOut.sampleRate());
  } else {
    println("-- Unable to load file. File may be empty, wrong format, or damaged.");
    return;
  }
  // adjust buffer size to mapper.getSize()
  if (buff.getBufferSize() != mapper.getSize()) buff.setBufferSize(mapper.getSize());
  playBuffer = buff;
  // ensureSamplerReady will load playBuffer to the Sampler synth "pool"
  ensureSamplerReady();
  // playBuffer is used directly by PASamplerInstrumentPool and should not change, so we copy its signal data
  // TODO consider if PASamplerInstrumentPool should copy the buffer
  float[] newSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
  audioSignal = newSignal;
  granSignal = newSignal;
  audioLength = audioSignal.length;
  if (isLoadToBoth) {
    writeAudioToImage(audioSignal, mapper, mapImage, chan);
    commitMapImageToBaseImage();
  }
  totalShift = 0;    // reset animation shift when audio is reloaded
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
 * Transcodes audio data in sig[] and writes it to color channel chan of mapImage
 * using the lookup tables in mapper to redirect indexing. Calls mapper.mapSigToImg(),
 * which will throw an IllegalArgumentException if sig.length != img.pixels.length
 * or sig.length != mapper.getSize().
 *
 * @param sig         an array of float, should be audio data in the range [-1.0, 1.0]
 * @param mapper      a PixelAudioMapper
 * @param img         a PImage
 * @param chan        a color channel
 */
public void writeAudioToImage(float[] sig, PixelAudioMapper mapper, PImage img, PixelAudioMapper.ChannelNames chan) {
  // If sig.length == mapper.getSize() == mapImage.width * mapImage.height, we can call safely mapper.mapSigToImg()
  img.loadPixels();
  mapper.mapSigToImg(sig, img.pixels, chan);
  img.updatePixels();
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
 * Sets totalShift = 0 on completion: the image and audio are now in sync. TODO
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
