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
  float[] resampled;
  if (fileSampleRate > 0) {
    println("---- file sample rate is "+ this.fileSampleRate);
    if (fileSampleRate != audioOut.sampleRate()) {
      resampled = AudioUtility.resampleMonoToOutput(buff.getChannel(0), fileSampleRate, audioOut);
      buff.setBufferSize(resampled.length);
      buff.setChannel(0, resampled);
      bufferSampleRate = sampleRate;
    }
    // save the length of the file, possibly resampled, for future use
    this.audioFileLength = buff.getBufferSize();
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
