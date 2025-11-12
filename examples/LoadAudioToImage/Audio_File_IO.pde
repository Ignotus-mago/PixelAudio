//-------------------------------------------//
//              AUDIO FILE I/O               //
//-------------------------------------------//

public void chooseFile() {
  selectInput("Choose an audio file: ", "fileSelected");
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
    } 
    else {
      println("----- File is not a recognized audio format ending with \"mp3\", \"wav\", \"aif\", or \"aiff\".");
    }
  } 
  else {
    println("----- No audio file was selected.");
  }
}

/**
 * Attempts to load audio data from a selected file into playBuffer, then calls
 * writeAudioToImage() to transcode audio data and write it to mapImage
 *
 * @param audioFile    an audio file
 */
public void loadAudioFile(File audioFile) {
  // read audio file into our MultiChannelBuffer
  float fileSampleRate = minim.loadFileIntoBuffer(audioFile.getAbsolutePath(), playBuffer);
  // sampleRate > 0 means we read audio from the file
  if (fileSampleRate > 0) {
    // resize the buffer to mapSize, if necessary -- signal will not be overwritten
    if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
    // read a copy of channel 0 of the buffer into audioSignal, truncated or padded to fit mapSize
    audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
    synth.setBuffer(audioSignal, fileSampleRate);
    audioLength = audioSignal.length;
    writeAudioToImage(audioSignal, mapper, mapImage, chan);
  }
}

/**
 * Transcodes audio data in sig[] and writes it to color channel chan of mapImage
 * using the lookup tables in mapper to redirect indexing. Calls mapper.mapSigToImg(),
 * which will throw an IllegalArgumentException if sig.length != img.pixels.length
 * or sig.length != mapper.getSize().
 *
 * @param sig         an source array of float, should be audio data in the range [-1.0, 1.0]
 * @param mapper      a PixelAudioMapper
 * @param img         a target PImage, modified by audio data in sig
 * @param chan        a color channel
 */
public void writeAudioToImage(float[] sig, PixelAudioMapper mapper, PImage img, PixelAudioMapper.ChannelNames chan) {
  // If sig.length == mapper.getSize() == mapImage.width * mapImage.height, we can call safely mapper.mapSigToImg()
  img.loadPixels();
  mapper.mapSigToImg(sig, img.pixels, chan);
  img.updatePixels();
}

/**
 * Plays an audio sample with PASamplerInstrument and default ADSR.
 *
 * @param samplePos    position of the sample in the audio buffer
 * @param sampleCount      number of samples to play
 * @param amplitude    amplitude of the samples on playback
 * @return the calculated sample length in samples
 */
public int playSample(int samplePos, int sampleCount, float amplitude) {
  sampleCount = synth.playSample(samplePos, sampleCount, amplitude);
  int durationMS = (int)(sampleCount/sampleRate * 1000);
  println("----- audio event duration = "+ durationMS +" millisconds");
  // return the length of the sample
  return sampleCount;
}

public void writeImageToAudio() {
  println("----- writing image to signal ");
  mapImage.loadPixels();
  audioSignal = new float[mapSize];
  mapper.mapImgToSig(mapImage.pixels, audioSignal);
  playBuffer.setBufferSize(mapSize);
  playBuffer.setChannel(0, audioSignal);
  if (playBuffer != null) println("--->> audioBuffer length channel 0 = "+ playBuffer.getChannel(0).length);
  synth.setBuffer(playBuffer);
}
