//-------------------------------------------//
//              AUDIO FILE I/O               //
//-------------------------------------------//

// ------------- LOAD AUDIO FILE ------------- //

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
      // good to go, let's load the audio file
      loadAudioFile(audioFile);
    } else {
      println("----- File is not a recognized audio format ending with \"mp3\", \"wav\", \"aif\", or \"aiff\".");
    }
  } else {
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
  float sampleRate = minim.loadFileIntoBuffer(audioFile.getAbsolutePath(), audioBuffer);
  // sampleRate > 0 means we read audio from the file
  if (sampleRate > 0) {
    // resize the buffer to mapSize, if necessary -- signal will not be overwritten
    if (audioBuffer.getBufferSize() != mapper.getSize()) audioBuffer.setBufferSize(mapper.getSize());
    // read a copy of channel 0 of the buffer into audioSignal, truncated or padded to fit mapSize
    audioSignal = Arrays.copyOf(audioBuffer.getChannel(0), mapSize);
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

public void writeImageToAudio() {
  println("----- writing image to signal ");
  mapImage.loadPixels();
  audioSignal = new float[mapSize];
  mapper.mapImgToSig(mapImage.pixels, audioSignal);
  audioBuffer.setBufferSize(mapSize);
  audioBuffer.setChannel(0, audioSignal);
  if (audioBuffer != null) println("--->> audioBuffer length channel 0 = "+ audioBuffer.getChannel(0).length);
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


// ------------- SAVE AUDIO FILE ------------- //

public void saveToAudio(String fileName) {
  try {
    saveAudioToFile(audioSignal, sampleRate, sketchPath("") +"/"+ fileName);
    println("Saved file to sketch path: "+ sketchPath(""));
  }
  catch (IOException e) {
    println("--->> There was an error outputting the audio file wavesynth.wav."+ e.getMessage());
  }
  catch (UnsupportedAudioFileException e) {
    println("--->> The file format is unsupported."+ e.getMessage());
  }
}

/**
 * Saves audio data to 16-bit integer PCM format, which Processing can also open.
 * 
 * @param samples      an array of floats in the audio range (-1.0f, 1.0f)
 * @param sampleRate    audio sample rate for the file
 * @param fileName      name of the file to save to
 * @throws IOException    an Exception you'll need to handle to call this method (see keyPressed entry for 's')
 * @throws UnsupportedAudioFileException    another Exception (see keyPressed entry for 's')
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
