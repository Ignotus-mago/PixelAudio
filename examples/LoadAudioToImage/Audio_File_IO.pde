//-------------------------------------------//
//              AUDIO FILE I/O               //
//-------------------------------------------//

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
