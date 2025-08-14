// ------------------------------------------- //
//           STREAM CAPTURE CLASS              //
// ------------------------------------------- //

/**
 * A simple class for obtaining samples from an audio source that is 
 * either a live stream or a stream from a file. Implements the 
 * AudioListener interface from the Minim audio library for Processing. 
 * StreamCapture is designed as a subclass of a parent class that declares:
 * 
 *   float[] audioSignal;
 *   MultiChannelBuffer audioBuffer;
 * 
 * As samples come in, they are written to audioSignal and to 
 * channel 0 in audioBuffer. 
 * 
 * Where you can obtain an audio stream depends on your OS and hardware. 
 * With the Sound library for Processing installed you can get
 * a list of available inputs and outputs. In MacOS, you can use the BlackHole 
 * virtual patch bay to route audio to or from Max and other applications. 
 * In MacOS, use the System Settings Sound tab to control signal routing. 
 * You can do something similar in Windows, but the setup and commands 
 * will be different. 
 * 
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
