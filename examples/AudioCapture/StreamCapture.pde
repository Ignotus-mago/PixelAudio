// ------------------------------------------- //
//           STREAM CAPTURE CLASS              //
// ------------------------------------------- //

/**
 * A simple class for obtaining samples from an audio source that is 
 * either a live stream or a stream from a file. In Processing, the 
 * live stream is pretty much confined to the built-in mic in MacOS, 
 * and works only in the Processing IDE--not in Eclipse. Implements the 
 * AudioListener interface from the Minim audio library for Processing. 
 */
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
   * The tricky part is keeping the signal in the right order for playback: write
   * the buffer to the signal first, then rotate the signal left by the length of
   * the buffer.
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
