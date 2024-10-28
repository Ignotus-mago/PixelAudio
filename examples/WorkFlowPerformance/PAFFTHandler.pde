/**
 * Handles windowed FFT on image and audio data using the Minim library.
 * Image data has to be translated into audio format. Audio data is expected 
 * to be floating point values in the range (0..1).
 * 
 * 
 * 
 * When handling image data from the Hilbert or Moore curves, it may be useful
 * to use a timeSize of 4096, corresponding to a 64 x 64 pixel square block if
 * you are stepping through a complete curve used as the signal path for an 
 * image. 
 */
public class PAFFTHandler {
  private FFT fft;
  private boolean isUseWindow = false;
  private WindowFunction window = FFT.HAMMING;
  private int frameSize;  // number of samples per frame, must be a power of 2
  private int sampleRate; 
  
  
  public PAFFTHandler(int sampleRate, int frameSize, boolean isUseWindow) {
    this.sampleRate = sampleRate;
    this.frameSize = frameSize;
    this.isUseWindow = isUseWindow;
    this.fft = new FFT(frameSize, sampleRate);
  }  

  public boolean isUseWindow() {
    return isUseWindow;
  }


  public void setUseWindow(boolean isUseWindow) {
    this.isUseWindow = isUseWindow;
  }


  public WindowFunction getWindow() {
    return window;
  }


  public void setWindow(WindowFunction window) {
    this.window = window;
  }


  public int getFrameSize() {
    return frameSize;
  }


  public int getSampleRate() {
    return sampleRate;
  }


  public FFT getFft() {
    return fft;
  }
  
  public float[] processSignalFrequencies(float[] inputSignal, float overlapFactor, float[] freqs, float amps[]) {
    // number of samples between frames, i.e., 512 if timeSize = 1024 and overlapFactor = 0.5
    int hopSize = (int)(this.frameSize * (1 - overlapFactor));
    // total number of frames in the signal
    int numFrames = (inputSignal.length - this.frameSize) / hopSize + 1;
    // an array to hold the reynthesized results
    float[] outputSignal = new float[inputSignal.length];
    // Generate the window curve once for the frame size
      float[] windowCurve = window.generateCurve(frameSize);
      // keep track of the scaling contributions from the window
    float[] windowSum = new float[inputSignal.length];
    for (int i = 0; i < numFrames; i++) {
      int startIndex = i * hopSize;
      float[] frame = new float[frameSize];
      System.arraycopy(inputSignal, startIndex, frame, 0, frameSize);
      this.fft.forward(frame);
      for (int j = 0; j < freqs.length; j++) {
        this.fft.scaleFreq(freqs[j], amps[j]);
      }
      this.fft.inverse(frame);
      for (int j = 0; j < frameSize; j++) {
        outputSignal[startIndex + j] += frame[j];
        windowSum[startIndex + j] += windowCurve[j];
      }      
    }
    // Normalize the output signal by the sum of window functions
    for (int i = 0; i < outputSignal.length; i++) {
      if (windowSum[i] > 0) {
        outputSignal[i] /= windowSum[i];
      }
    }
    return outputSignal;
  }
  
  public float[] processSignalBins(float[] inputSignal, float overlapFactor, int[] bins, float amps[]) {
      // number of samples between frames, i.e., 512 if timeSize = 1024 and overlapFactor = 0.5
      int hopSize = (int)(this.frameSize * (1 - overlapFactor));
      // total number of frames in the signal
      int numFrames = (inputSignal.length - this.frameSize) / hopSize + 1;
      // an array to hold the reynthesized results
      float[] outputSignal = new float[inputSignal.length];
      // Generate the window curve once for the frame size
      float[] windowCurve = window.generateCurve(frameSize);
      // keep track of the scaling contributions from the window
      float[] windowSum = new float[inputSignal.length];
      for (int i = 0; i < numFrames; i++) {
          int startIndex = i * hopSize;
          float[] frame = new float[frameSize];
          System.arraycopy(inputSignal, startIndex, frame, 0, frameSize);
          this.fft.forward(frame);
          for (int j = 0; j < bins.length; j++) {
              this.fft.scaleBand(bins[j], amps[j]);
          }
          this.fft.inverse(frame);
          for (int j = 0; j < frameSize; j++) {
              outputSignal[startIndex + j] += frame[j];
              windowSum[startIndex + j] += windowCurve[j];
          }      
      }
      // Normalize the output signal by the sum of window functions
      for (int i = 0; i < outputSignal.length; i++) {
          if (windowSum[i] > 0) {
              outputSignal[i] /= windowSum[i];
          }
      }
      return outputSignal;
  }

  
  /**
   * Scales a frequency by a factor.
   * 
   * @param freq
   * @param fac
   */
  public void fftScaleFreq(float freq, float fac) {
    fft.scaleFreq(freq, fac);
  }

  /**
   * Scales an array of frequencies by an array of factors.
   * 
   * @param freqs
   * @param facs
   */
  public void fftScaleFreq(float[] freqs, float[] facs) {
    for (int i = 0; i < freqs.length; i++) {
      fft.scaleFreq(freqs[i], facs[i]);
    }
  }

  /**
   * Scales a single frequency bin (index number) by a factor.
   * 
   * @param bin
   * @param fac
   */
  public void fftScaleBin(int bin, float fac) {
    fft.scaleBand(bin, fac);
  }

  /**
   * Scales an array of frequency bins (index numbers) by an array of factors.
   * 
   * @param bins
   * @param facs
   */
  public void fftScaleBin(int[] bins, float[] facs) {
    for (int i = 0; i < bins.length; i++) {
      fft.scaleBand(bins[i], facs[i]);
    }
  }  
  
}
