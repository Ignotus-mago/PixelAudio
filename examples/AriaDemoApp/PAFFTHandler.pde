/**
 * Handles windowed FFT on image and audio data using the Minim library.
 * Image data has to be translated into audio format. Audio data is expected 
 * to be floating point values in the range (0..1).
 * 
 * 
 * When handling image data from the Hilbert or Moore curves, it may be useful
 * to use a timeSize of 4096, corresponding to a 64 x 64 pixel square block if
 * you are stepping through a complete curve used as the signal path for an 
 * image. 
 */
public class PAFFTHandler {
  private FFT fft;
  private int frameSize;  // number of samples per frame, must be a power of 2
  private int overlapSize;
  private int inputFrameSize;
  private int sampleRate; 
  private boolean isUseWindow = true;
  private WindowFunction window = FFT.HAMMING;
  
  
  public PAFFTHandler(int sampleRate, int frameSize, boolean isUseWindow) {
    this.sampleRate = sampleRate;
    this.frameSize = frameSize;
    this.overlapSize = frameSize / 2;  // for a 50% overlap
    this.inputFrameSize = frameSize + overlapSize;
    this.isUseWindow = isUseWindow;
    this.fft = new FFT(frameSize, sampleRate);
    fft.window(window);
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
    fft.window(window);
  }


  public int getFrameSize() {
    return frameSize;
  }

  

  public int getOverlapSize() {
    return overlapSize;
  }

  public void setOverlapSize(int overlapSize) {
    this.overlapSize = overlapSize;
  }

  public int getInputFrameSize() {
    return inputFrameSize;
  }

  public void setInputFrameSize(int inputFrameSize) {
    this.inputFrameSize = inputFrameSize;
  }

  public int getSampleRate() {
    return sampleRate;
  }


  public FFT getFft() {
    return fft;
  }
  
  /**
   * @param inputSignal
   * @param overlapFactor    0 for no overlap
   * @param freqs
   * @param amps
   * @return
   */
  public float[] processSignalFrequencies(float[] inputSignal, float overlapFactor, float[] freqs, float amps[]) {
    if (overlapFactor >= 1.0f) {
      throw(new IllegalArgumentException("ERROR: The overlapFactor argument must be between 0 and 1."));
    }
    boolean isDebug = false;
    if (isDebug) {
      System.out.println("--- processSignalFrequencies: ");
      for (int i = 0; i < freqs.length; i++) {
        System.out.println(freqs[i] +", "+ amps[i]);
      }
    }
    // number of samples between frames, i.e., 512 if timeSize = 1024 and overlapFactor = 0.5
    int hopSize = (int)(this.frameSize * (1 - overlapFactor));
    // total number of frames in the signal
    // int numFrames = (inputSignal.length - this.frameSize) / hopSize + 1;
    // System.out.println("--- numFrames = "+ numFrames);
    int numFrames = (int) Math.ceil((double)(inputSignal.length - frameSize) / hopSize) + 1;
    // System.out.println("--- also numFrames = "+ numFrames);
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
      // Apply FFT (windowing is automatically applied 
      // if you set the window with fft.window(WindowFunction))
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
    // smooth the end
    for (int i = outputSignal.length - frameSize / 2; i < outputSignal.length; i++) {
        outputSignal[i] *= 0.5f * (1 + Math.cos(Math.PI * (i - (outputSignal.length - frameSize)) / frameSize));
    }
    return outputSignal;
  }
  
    /**
   * @param inputSignal
   * @param freqs
   * @param amps
   * @return
   */
  public float[] processSignalFrequenciesFast(float[] inputSignal, float[] freqs, float amps[], boolean isNormalize) {
    // no window
    fft.window(FFT.NONE);
    // number of samples between frames
    int hopSize = frameSize;
    // total number of frames in the signal
    int numFrames = (int) Math.ceil((double) (inputSignal.length - frameSize) / hopSize) + 1;
    // an array to hold the resynthesized results
    float[] outputSignal = new float[inputSignal.length];
    for (int i = 0; i < numFrames; i++) {
      int startIndex = i * hopSize;
      float[] frame = new float[frameSize];
      System.arraycopy(inputSignal, startIndex, frame, 0, frameSize);
      // Apply FFT (windowing is automatically applied
      // if you set the window with fft.window(WindowFunction))
      this.fft.forward(frame);
      for (int j = 0; j < freqs.length; j++) {
        this.fft.scaleFreq(freqs[j], amps[j]);
      }
      this.fft.inverse(frame);
      for (int j = 0; j < frameSize; j++) {
        outputSignal[startIndex + j] += frame[j];
      }
    }
    // smooth the end
    if (isNormalize) {
      float maxAmplitude = 0;
      for (float sample : outputSignal) {
        maxAmplitude = Math.max(maxAmplitude, Math.abs(sample));
      }
      if (maxAmplitude > 1.0f) {
        for (int i = 0; i < outputSignal.length; i++) {
          outputSignal[i] /= maxAmplitude; // Normalize the signal
        }
      }
    }
    return outputSignal;
  }

  public float[] processSignalBins(float[] inputSignal, float overlapFactor, int[] bins, float amps[]) {
      if (overlapFactor >= 1.0f) {
      throw(new IllegalArgumentException("ERROR: The overlapFactor argument must be between 0 and 1."));
    }
      float normScale = 0.9f; 
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
      // Apply FFT (windowing is automatically applied 
          // if you set the window with fft.window(WindowFunction))
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
            float sample = outputSignal[i];
            outputSignal[i] = (sample * normScale)/ windowSum[i];
              // outputSignal[i] /= windowSum[i];
          }
      }
      return outputSignal;
  }
  
  public float[] processWithFormant(float inputSignal[], float overlapFactor, Formant formant, float[] amps) {
    float[] freqs = formant.getFrequencies();
    return this.processSignalFrequencies(inputSignal, overlapFactor, freqs, amps);
  }

  public float[] processSignalOverlapSave(float[] inputSignal, float[] freqs, float[] amps) {
    int numFrames = (inputSignal.length - overlapSize) / overlapSize;
    float[] outputSignal = new float[inputSignal.length];
    float[] windowSum = new float[outputSignal.length];
    int fftFrameSize = this.getFrameSize();
    for (int i = 0; i < numFrames; i++) {
      // Extract input frame with overlap
      int startIdx = i * overlapSize;
      float[] frame = new float[fftFrameSize];
          // Ensure we do not exceed inputSignal length
          int copyLength = Math.min(fftFrameSize, inputSignal.length - startIdx);
          System.arraycopy(inputSignal, startIdx, frame, 0, copyLength);
          // Apply FFT on the frame
          fft.forward(frame);
          // Modify frequency bins based on specified freqs and amps
          for (int f = 0; f < freqs.length; f++) {
              fft.scaleFreq(freqs[f], amps[f]);
          }
          // Inverse FFT to get back to time domain
          fft.inverse(frame); // This should match the original FFT frame size
          // Overlap-save: only save the central portion of the frame
          for (int j = 0; j < fftFrameSize; j++) {
              if (startIdx + j < outputSignal.length) {
                  outputSignal[startIdx + j] += frame[j]; // Update output signal
                  windowSum[startIdx + j] += window.generateCurve(fftFrameSize)[j]; // Update window sum
              }
          }
    }
      // Normalize based on the sum of window functions
      for (int i = 0; i < outputSignal.length; i++) {
          if (windowSum[i] > 0) {
              outputSignal[i] /= windowSum[i]; // Normalize the signal
          }
      }
    // smooth the end
    for (int i = outputSignal.length - frameSize / 2; i < outputSignal.length; i++) {
        outputSignal[i] *= 0.5f * (1 + Math.cos(Math.PI * (i - (outputSignal.length - frameSize)) / frameSize));
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
  
  // ------------->>> BEGIN FORMANT LIST METHODS <<<------------- //

  /*
   * some useful information
   * 
   * FORMANTS
   *    0  i beet 270 2290 3010 
   *    1  I bit 390 1990 2550
   *    2  e bet 530 1840 2480
   *    3  ae bat 660 1720 2410
   *    4  a father 730 1090 2440
   *    5  U book 440 1020 2240
   *    6  u boot 300 870 2240
   *    7  L but 640 1190 2390
   *    8  r bird 490 1350 1690
   *    9  aw bought 570 840 2410
   *   10  o coat 490 910 2450
   *   11  A cake 390 2300 (2500-3380)
   *   12  a1 pain 500 1500 2500
   *   13  a2 pain 300 2100 2650
   *   14  ai fire 300 2500 3300
   *   15  i1 time 750 1200 2400
   *   16  i2 time 350 2300 2500
   *   
   *
   * only the first two formants figure in many analyses of vowel sounds they
   * correspond, roughly, to tongue position: the closer F1 and F2 are to each
   * other, the further back the tongue. The more front the vowel, the higher F2.
   * F3 tends to vary less. supplement: cake: 390 2300 (2500-3380)
   */

  // A storage class for formants, effectively 3-pole bandpass filters
  public class Formant {
    public float f0;
    public float f1;
    public float f2;
    public String example;
    public String symbol;

    public Formant(float f0, float f1, float f2, String sym, String ex) {
      this.f0 = f0;
      this.f1 = f1;
      this.f2 = f2;
      this.symbol = sym;
      this.example = ex;
    }
    
    public float[] getFrequencies() {
      float[] freqs = new float[3];
      freqs[0] = this.f0;
      freqs[1] = this.f1;
      freqs[2] = this.f2;
      return freqs;
    }

    public String toString() {
      return "formant: " + f0 + ", " + f1 + ", " + f2 + " -- " + symbol + ": " + example;
    }
  }  
  
  public ArrayList<Formant> loadVowels() {
    ArrayList<Formant> formantList = new ArrayList<Formant>();
    formantList.add(new Formant(270, 2290, 3010, "i", "beet"));    //   0
    formantList.add(new Formant(390, 1990, 2550, "I", "bit"));    //   1
    formantList.add(new Formant(530, 1840, 2480, "e", "bet"));    //   2
    formantList.add(new Formant(660, 1720, 2410, "ae", "bat"));    //   3
    formantList.add(new Formant(730, 1090, 2440, "a", "father"));  //   4
    formantList.add(new Formant(440, 1020, 2240, "U", "book"));    //   5
    formantList.add(new Formant(300, 870, 2240, "u", "boot"));    //   6
    formantList.add(new Formant(640, 1190, 2390, "L", "but"));    //   7
    formantList.add(new Formant(490, 1350, 1690, "r", "bird"));    //   8
    formantList.add(new Formant(570, 840, 2410, "aw", "bought"));  //   9
    formantList.add(new Formant(490, 910, 2450, "o", "coat"));    // 10
    formantList.add(new Formant(390, 2300, 2640, "A", "cake"));    // 11
    formantList.add(new Formant(500, 1500, 2500, "a1", "pain"));  // 12
    formantList.add(new Formant(300, 2100, 2700, "a2", "pain"));  // 13
    formantList.add(new Formant(300, 2500, 3300, "ai", "fire"));  // 14
    formantList.add(new Formant(750, 1200, 2400, "i1", "time"));  // 15
    formantList.add(new Formant(350, 2300, 2500, "i2", "time"));  // 16
    return formantList;
  }
  
  /**
   * @param f0  a frequency value, maybe A = 440
   * @param f1  another frequency, maybe f0 * Math.pow(2, 5/12.0), perfect 4th up
   * @param f2  yet another frequency, could it be f0 * Math.pow(2, 10/12.0), minor 7th up
   * @return    a list of formants, a chromatic scale of band-pass filters
   */
  public ArrayList<Formant> loadChromatic(float f0, float f1, float f2) {
      ArrayList<Formant> formantList = new ArrayList<Formant>();
      float fac = (float) Math.pow(2, 1/12.0); // one semitone
      for (int i = 0; i < 12; i++) {
        formantList.add(new Formant(f0, f1, f2, "chromatic"+i, "--scale--"));
        f0 *= fac;
        f1 *= fac;
        f2 *= fac;
      }
      return formantList;
    }
      
}
