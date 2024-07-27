public class StreamCapture implements AudioListener {
  private float[] left;
  private float[] right;
 
  public StreamCapture() {
    left = null;
    right = null;
  }

  @Override
  public synchronized void samples(float[] samp) {
    left = samp;
    fillSignal();
  }

  @Override
  public synchronized void samples(float[] sampL, float[] sampR) {
    left = sampL;
    right = sampR;
    fillSignal();
  }

  /**
   * The tricky part is keeping the signal in the right order for playback: write the 
   * buffer to the signal first, then rotate the signal left by the length of the buffer.
   */
  public synchronized void fillSignal() {
    if (left != null) {
      for (int i = 0; i < left.length; i++) {
        audioSignal[i] = left[i];
      }
      PixelAudioMapper.rotateLeft(audioSignal, left.length);
      audioBuffer.setChannel(0, audioSignal);
    }
    if (right != null) {
      // not doing anything
    }
  }
}
