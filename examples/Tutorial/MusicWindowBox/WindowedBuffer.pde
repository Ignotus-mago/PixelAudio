// ------------------------------------------- //
//            WINDOWED BUFFER CLASS            //
// ------------------------------------------- //

public class WindowedBuffer {
  private final float[] buffer;   // circular source
  private final float[] window;   // reusable window array
  private final int windowSize;   // number of samples in a window
  private int hopSize;            // step between windows
  private int index = 0;          // current start position in buffer

  public WindowedBuffer(float[] buffer, int windowSize, int hopSize) {
    if (buffer.length == 0) {
      throw new IllegalArgumentException("Buffer must not be empty");
    }
    if (windowSize <= 0) {
      throw new IllegalArgumentException("Window size must be positive");
    }
    if (hopSize <= 0) {
      throw new IllegalArgumentException("Hop size must be positive");
    }
    this.buffer = buffer;
    this.windowSize = windowSize;
    this.hopSize = hopSize;
    this.window = new float[windowSize];
  }

/**
 * Returns the next window, advancing the read index by hopSize.
 * Wraps around the buffer as needed.
 */
  public float[] nextWindow() {
    int bufferLen = buffer.length;
    // Copy first chunk
    int firstCopyLen = Math.min(windowSize, bufferLen - index);
    System.arraycopy(buffer, index, window, 0, firstCopyLen);
    // Wrap if needed
    if (firstCopyLen < windowSize) {
      int remaining = windowSize - firstCopyLen;
      System.arraycopy(buffer, 0, window, firstCopyLen, remaining);
    }
    // Advance start index
    index = (index + hopSize) % bufferLen;
    return window;
  }

  /**
 * Returns the window at a supplied index. Wraps around the buffer as needed.
 * Updates current index and advances it by hopSize.
 */
  public float[] gettWindowAtIndex(int idx) {
    int len = buffer.length;
    idx = ((idx % len) + len) % len; // normalize to 0..len-1
    setIndex(idx);
    return nextWindow();
  }

  /** Reset reader to start of buffer */
  public void reset() {
    index = 0;
  }

  /** Current buffer index */
  public int getIndex() {
    return index;
  }

  /** set current index */
  public void setIndex(int index) {
    this.index = index % buffer.length;
  }

  /** Expose the underlying array size */
  public int getBufferSize() {
    return this.buffer.length;
  }

  /** Expose the reusable window array size */
  public int getWindowSize() {
    return windowSize;
  }

  /** Expose hop size */
  public int getHopSize() {
    return hopSize;
  }

  public void setHopSize(int hopSize) {
    this.hopSize = hopSize;
  }

}  
