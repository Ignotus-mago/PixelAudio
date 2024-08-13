//-------------------------------------------//
//              AUDIO FILE I/O               //
//-------------------------------------------//

public void saveToAudio() {
  renderSignal();
  audioSignal = WaveSynth.normalize(audioSignal, 0.9);
  try {
    saveAudioToFile(audioSignal, sampleRate, sketchPath("") + "/wavesynth.wav");
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
 * Saves to a 32-bit floating point format that has higher resolution than 16-bit integer PCM.
 * The format can't be opened by Processing but can be opened by audio applications.
 *
 * @param samples        an array of floats in the audio range (-1.0f, 1.0f)
 * @param sampleRate     the sample rate for the file
 * @param fileName       name of the file
 * @throws IOException   an Exception you'll need to catch to call this method (see saveToAudio(), above)
 */
public static void saveAudioTo32BitPCMFile(float[] samples, float sampleRate, String fileName) throws IOException {
  // convert samples to 32-bit PCM float
  byte[] audioBytes = new byte[samples.length * 4];
  int index = 0;
  // convert to IEEE 754 floating-point "single format" bit layout
  for (float sample : samples) {
    int intBits = Float.floatToIntBits(sample);
    audioBytes[index++] = (byte) (intBits & 0xFF);
    audioBytes[index++] = (byte) ((intBits >> 8) & 0xFF);
    audioBytes[index++] = (byte) ((intBits >> 16) & 0xFF);
    audioBytes[index++] = (byte) ((intBits >> 24) & 0xFF);
  }
  ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
  AudioFormat format = new AudioFormat(sampleRate, 32, 1, true, false);
  AudioInputStream audioInputStream = new AudioInputStream(byteStream, format, samples.length);
  File outFile = new File(fileName);
  AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outFile);
}


/**
 * Saves audio data to 16-bit integer PCM format, which Processing can also open.
 *
 * @param samples       an array of floats in the audio range (-1.0f, 1.0f)
 * @param sampleRate    audio sample rate for the file
 * @param fileName      name of the file to save to
 * @throws IOException  an Exception you'll need to catch to call this method (see saveToAudio(), above)
 * @throws UnsupportedAudioFileException    another possible Exception
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
