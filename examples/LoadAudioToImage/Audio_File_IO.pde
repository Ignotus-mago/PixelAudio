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
      loadAudioFile(audioFile);
    } else {
      println("----- File is not a recognized audio format ending with \"mp3\", \"wav\", \"aif\", or \"aiff\".");
    }
  } else {
    println("----- No audio file was selected.");
  }
}

public void loadAudioFile(File audioFile) {
  // read audio file into our MultiChannelBuffer
  float sampleRate = minim.loadFileIntoBuffer(audioFile.getAbsolutePath(), audioBuffer);
  // sampleRate > 0 means we read audio from the file
  if (sampleRate > 0) {
    // read an array of floats from the buffer
    this.audioSignal = audioBuffer.getChannel(0);
    this.audioLength = audioSignal.length;
    // load rgbSignal with rgb gray values corresponding to the audio sample values
    rgbSignal = mapper.pluckSamplesAsRGB(audioSignal, 0, mapSize);  
    if (rgbSignal.length < mapSize) {
      // pad rgbSignal with 0's if necessary
      rgbSignal = Arrays.copyOf(rgbSignal, mapSize); 
    }
    mapImage.loadPixels();
    // write the rgbSignal pixels to mapImage, following the signal path
    mapper.plantPixels(rgbSignal, mapImage.pixels, 0, rgbSignal.length, chan);
    mapImage.updatePixels();
  }
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
