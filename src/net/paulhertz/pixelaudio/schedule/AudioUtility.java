package net.paulhertz.pixelaudio.schedule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;

/** Utility conversions for audio. */
public final class AudioUtility {
    private AudioUtility() {}
    /** Constant: 2^(1/12), the ratio of one semitone. */
    public static final double SEMITONE_FAC = Math.pow(2.0, 1.0 / 12.0);
    /** Precomputed log(2) for efficiency. */
    private static final double LOG2 = Math.log(2.0);


    public static long millisToSamples(double millis, double sampleRate) {
        return (long) Math.floor((millis / 1000.0) * sampleRate + 0.5);
    }

    public static double samplesToMillis(long samples, double sampleRate) {
        return (samples * 1000.0) / sampleRate;
    }

    public static long secsToSamples(double secs, double sampleRate) {
        return (long) Math.floor(secs * sampleRate + 0.5);
    }
    
    /**
     * Converts a gain value in decibels (dB) to a linear amplitude multiplier.
     *
     *  0.0 dB  -> 1.0
     * -6.0 dB  -> ~0.501
     * +6.0 dB  -> ~1.995
     *
     * @param dB gain in decibels
     * @return linear gain multiplier
     */
    public static float dbToLinear(float dB) {
        return (float) Math.pow(10.0, dB / 20.0);
    }
    
    /**
     * @param linear    decimal gain value, for example from a UI slider
     * @return values in decibels for linear
     */
    public static float linearToDb(float linear) {
    	float x = Math.max(linear, 1.0e-12f);
    	return 20.0f * (float)Math.log10(x);
    }
   
    // ------------------------------------------------------------------------
    // Pitch / frequency utilities
    // ------------------------------------------------------------------------

    /**
     * Convert semitone offset to frequency ratio.
     *
     * @param semitones number of semitones (can be fractional)
     * @return frequency ratio (>0)
     *
     * Examples:
     *   12  -> 2.0
     *   0   -> 1.0
     *  -12  -> 0.5
     */
    public static float semitonesToRatio(float semitones) {
    	return (float) Math.pow(2.0, semitones / 12.0);
    }

    /**
     * Convert frequency ratio to semitone offset.
     *
     * @param ratio frequency ratio (>0)
     * @return semitone offset (can be fractional)
     */
    public static float ratioToSemitones(float ratio) {
    	if (ratio <= 0f) {
    		throw new IllegalArgumentException("ratio must be > 0");
    	}
    	return (float) (12.0 * Math.log(ratio) / LOG2);
    }

    /**
     * Convert MIDI-style key number to frequency (A4 = 440 Hz, key 49).
     */
    public static float pianoKeyFrequency(float keyNumber) {
    	return (float) (440.0 * Math.pow(2.0, (keyNumber - 49.0) / 12.0));
    }

    /**
     * Convert frequency to fractional MIDI-style key number.
     */
    public static float frequencyPianoKey(float freq) {
    	if (freq <= 0f) {
    		throw new IllegalArgumentException("frequency must be > 0");
    	}
    	return 49.0f + 12.0f * (float) (Math.log(freq / 440.0) / LOG2);
    }

    /**
     * Apply a semitone offset directly to a frequency.
     */
    public static float transposeFrequency(float freq, float semitones) {
        return freq * semitonesToRatio(semitones);
    }
    
    
    // ------------- SIGNAL LEVELS -------------
    
    /**
     * Computes the peak absolute sample value of a signal.
     *
     * @param signal The audio samples
     * @return Maximum absolute sample value (linear scale)
     */
    public static float computePeak(float[] signal) {
        if (signal == null || signal.length == 0) return 0f;
        float peak = 0f;
        for (float v : signal) {
            float a = Math.abs(v);
            if (a > peak) peak = a;
        }
        return peak;
    }
    
    /**
     * Computes the RMS (root mean square) level of a signal.
     *
     * @param signal The audio samples
     * @return RMS value (linear scale)
     */
    public static float computeRMS(float[] signal) {
        if (signal == null || signal.length == 0) return 0f;
        double sumSq = 0.0;
        for (float v : signal) {
            sumSq += (double) v * v;
        }
        return (float) Math.sqrt(sumSq / signal.length);
    }
    
    
    // ------------- NORMALIZATION -------------
    
    enum NormalizationMode {
    	DB,
        PEAK,
        RMS,
        RMS_WITH_CEILING
    }
    
	/**
	 * Normalizes a single-channel signal array to a target RMS level in dBFS.
	 *
	 * @param signal    The audio samples to normalize (modified in place)
	 * @param targetDB  The peak level in dB
	 *                  (e.g. -3.0f for moderately loud, -12.0f for safe headroom)
	 * @return gain applied to signal
	 */
	public static float normalizeRMS(float[] signal, float targetDB) {
	    if (signal == null || signal.length == 0) return 0f;
	    // --- Step 1: Compute RMS (double precision accumulation) ---
	    double rms = computeRMS(signal);
	    if (rms < 1e-12) return 0f; // silence guard
	    // --- Step 2: Convert target dBFS to linear RMS ---
	    float targetRMS = (float) Math.pow(10.0, targetDB / 20.0);
	    // --- Step 3: Apply gain ---
	    float gain = (float) (targetRMS / rms);
	    for (int i = 0; i < signal.length; i++) {
	        signal[i] *= gain;
	    }
	    return gain;
	}
	
	public static void normalizePeakDb(float[] signal, float targetPeakDB) {
	    if (signal == null || signal.length == 0) return;
	    float peak = computePeak(signal);
	    if (peak < 1e-12f) return; // silence
	    float targetPeak = (float) Math.pow(10.0, targetPeakDB / 20.0);
	    float gain = targetPeak / peak;
	    for (int i = 0; i < signal.length; i++) {
	        signal[i] *= gain;
	    }
	}
	
	public static void normalizePeakLevel(float[] signal, float targetPeakLevel) {
	    if (signal == null || signal.length == 0) return;
	    float peak = computePeak(signal);
	    if (peak < 1e-12f) return; // silence
	    float gain = targetPeakLevel / peak;
	    for (int i = 0; i < signal.length; i++) {
	        signal[i] *= gain;
	    }
	}

	public static void normalizeRmsWithCeiling(float[] signal, float targetRmsDB, float peakCeilingDB) {
	    if (signal == null || signal.length == 0) return;
	    // RMS
	    double sumSq = 0.0;
	    for (float v : signal) sumSq += (double) v * v;
	    double rms = Math.sqrt(sumSq / signal.length);
	    if (rms < 1e-12) return;
	    // Peak
	    float peak = 0f;
	    for (float v : signal) {
	        float a = Math.abs(v);
	        if (a > peak) peak = a;
	    }
	    if (peak < 1e-12f) return;
	    float targetRms = (float) Math.pow(10.0, targetRmsDB / 20.0);
	    float ceiling = (float) Math.pow(10.0, peakCeilingDB / 20.0);
	    float gainRms = (float) (targetRms / rms);
	    float gainPeak = ceiling / peak;
	    float gain = Math.min(gainRms, gainPeak);
	    for (int i = 0; i < signal.length; i++) {
	        signal[i] *= gain;
	    }
	}

    
    // ------------- RESAMPLING -------------
	
	
	public static int fileSamplesRequiredForDisplay(int mapSize, float fileSampleRate, float audioOutRate) {
	    if (mapSize <= 0 || fileSampleRate <= 0f || audioOutRate <= 0f)
	        return 0;
	    double required = mapSize * ((double) fileSampleRate / audioOutRate);
	    return (int) Math.ceil(required);
	}
	
	
    // ------------------------------------------------------------------------
    // Mono resampling: float[] → float[]
    // ------------------------------------------------------------------------

    /**
     * Resamples a mono buffer from sourceRate to targetRate using linear interpolation.
     *
     * @param source     mono samples at sourceRate
     * @param sourceRate sample rate of the source buffer (Hz)
     * @param targetRate desired sample rate (Hz)
     * @return new float[] at targetRate
     */
    public static float[] resampleMono(float[] source,
                                       float sourceRate,
                                       float targetRate) {
        if (source == null) {
            throw new IllegalArgumentException("source buffer must not be null");
        }
        if (sourceRate <= 0 || targetRate <= 0) {
            throw new IllegalArgumentException("sample rates must be > 0");
        }
        if (source.length == 0) {
            return new float[0];
        }

        // If rates match, just clone
        if (Math.abs(sourceRate - targetRate) < 1e-6f) {
            float[] copy = new float[source.length];
            System.arraycopy(source, 0, copy, 0, source.length);
            return copy;
        }

        // duration = N / sourceRate = M / targetRate  => M = N * targetRate / sourceRate
        final int srcLen = source.length;
        final double ratio = targetRate / sourceRate;      // how many target samples per source sample
        final int dstLen = (int) Math.round(srcLen * ratio);

        float[] out = new float[dstLen];

        // For each output sample, pick a position in the source
        // srcPos = i * (sourceRate / targetRate) = i / ratio
        final double invRatio = 1.0 / ratio;

        for (int i = 0; i < dstLen; i++) {
            double srcPos = i * invRatio;
            int i0 = (int) srcPos;
            int i1 = i0 + 1;
            if (i1 >= srcLen) {
                i1 = srcLen - 1;
            }
            float frac = (float) (srcPos - i0);
            float s0 = source[i0];
            float s1 = source[i1];
            out[i] = s0 + (s1 - s0) * frac;  // linear interpolation
        }

        return out;
    }

    /**
     * Convenience: resample mono buffer from sourceRate to match AudioOutput sample rate.
     */
    public static float[] resampleMonoToOutput(float[] source,
                                               float sourceRate,
                                               AudioOutput out) {
        if (out == null) {
            throw new IllegalArgumentException("AudioOutput must not be null");
        }
        return resampleMono(source, sourceRate, out.sampleRate());
    }

    // ------------------------------------------------------------------------
    // MultiChannelBuffer → MultiChannelBuffer (simple stereo support)
    // ------------------------------------------------------------------------

    /**
     * Resamples all channels in a MultiChannelBuffer from sourceRate to targetRate.
     * Produces a new MultiChannelBuffer at targetRate.
     *
     * For PixelAudio you may only need channel 0 (mono); this is available
     * mainly for completeness.
     */
    public static MultiChannelBuffer resampleMCB(MultiChannelBuffer src,
                                                 float sourceRate,
                                                 float targetRate) {
        if (src == null) {
            throw new IllegalArgumentException("source MultiChannelBuffer must not be null");
        }
        if (sourceRate <= 0 || targetRate <= 0) {
            throw new IllegalArgumentException("sample rates must be > 0");
        }

        int channels = src.getChannelCount();
        int srcLen   = src.getBufferSize();

        if (srcLen == 0) {
            return new MultiChannelBuffer(channels, 0);
        }

        if (Math.abs(sourceRate - targetRate) < 1e-6f) {
            // Just clone
            MultiChannelBuffer copy = new MultiChannelBuffer(channels, srcLen);
            copy.set(src);
            return copy;
        }

        double ratio   = targetRate / sourceRate;
        int dstLen     = (int) Math.round(srcLen * ratio);
        MultiChannelBuffer dst = new MultiChannelBuffer(channels, dstLen);

        double invRatio = 1.0 / ratio;

        for (int ch = 0; ch < channels; ch++) {
            float[] srcCh = src.getChannel(ch);
            float[] dstCh = dst.getChannel(ch);

            for (int i = 0; i < dstLen; i++) {
                double srcPos = i * invRatio;
                int i0 = (int) srcPos;
                int i1 = i0 + 1;
                if (i1 >= srcLen) i1 = srcLen - 1;

                float frac = (float) (srcPos - i0);
                float s0 = srcCh[i0];
                float s1 = srcCh[i1];
                dstCh[i] = s0 + (s1 - s0) * frac;
            }
        }

        return dst;
    }

    /**
     * Convenience: resample MultiChannelBuffer from sourceRate to match AudioOutput.
     */
    public static MultiChannelBuffer resampleMCBToOutput(MultiChannelBuffer src,
                                                         float sourceRate,
                                                         AudioOutput out) {
        if (out == null) {
            throw new IllegalArgumentException("AudioOutput must not be null");
        }
        return resampleMCB(src, sourceRate, out.sampleRate());
    }

    
	//-------------------------------------------//
	//              AUDIO FILE I/O               //
	//-------------------------------------------//
	
	
	/**
	 * Saves audio data to 16-bit integer PCM format, which Processing can also open.
	 * 
	 * @param samples			an array of floats in the audio range (-1.0f, 1.0f)
	 * @param sampleRate		audio sample rate for the file
	 * @param fileName			name of the file to save to
	 * @throws IOException		an Exception you'll need to handle to call this method (see keyPressed entry for 's')
	 * @throws UnsupportedAudioFileException		another Exception (see keyPressed entry for 's')
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
	
	/**
	 * Saves audio data to a 32-bit floating point format that has higher resolution than 16-bit integer PCM. 
	 * The format can't be opened by Processing but can be opened by audio applications. 
	 * 
	 * @param samples			an array of floats in the audio range (-1.0f, 1.0f)
	 * @param sampleRate		the sample rate for the file
	 * @param fileName			name of the file
	 * @throws IOException		an Exception you'll need to catch to call this method (see keyPressed entry for 's')
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
	 * Saves stereo audio data to 16-bit integer PCM format, which Processing can also open.
	 * 
	 * @param leftChannel		an array of floats in the audio range (-1.0f, 1.0f)
	 * @param rightChannel		an array of floats in the audio range (-1.0f, 1.0f)
	 * @param sampleRate		audio sample rate for the file
	 * @param fileName			name of the file to save to
	 * @throws IOException		an Exception you'll need to handle to call this method (see keyPressed entry for 's')
	 * @throws UnsupportedAudioFileException		another Exception (see keyPressed entry for 's')
	 */
	public static void saveStereoAudioToFile(float[] leftChannel, float[] rightChannel, float sampleRate, String fileName)
	        throws IOException, UnsupportedAudioFileException {
	    if (leftChannel.length != rightChannel.length) {
	        throw new IllegalArgumentException("Left and right channel sample arrays must have the same length.");
	    }
        int numSamples = leftChannel.length;
	    // Convert samples from float to 16-bit PCM
	    byte[] audioBytes = new byte[leftChannel.length * 2 * 2];
	    int index = 0;
	    for (int i = 0; i < numSamples; i++) {
	    	// sclae the samples to 16-bit integers
	        int left = (int) (leftChannel[i] * 32767);
	        int right = (int) (rightChannel[i] * 32767);
	        // Left channel (little endian)
	        audioBytes[index++] = (byte) (left & 0xFF);
	        audioBytes[index++] = (byte) ((left >> 8) & 0xFF);
	        // Right channel (little endian)
	        audioBytes[index++] = (byte) (right & 0xFF);
	        audioBytes[index++] = (byte) ((right >> 8) & 0xFF);
	    }
	    // Create an AudioInputStream
	    ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
	    AudioFormat format = new AudioFormat(sampleRate, 16, 2, true, false);
	    AudioInputStream audioInputStream = new AudioInputStream(byteStream, format, numSamples);
	    // Save the AudioInputStream to a WAV file
	    File outFile = new File(fileName);
	    AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outFile);
	}
	
	/**
	 * Saves stereo audio data to a 32-bit floating point format that has higher resolution than 16-bit integer PCM. 
	 * The format can't be opened by Processing but can be opened by audio applications. 
	 * 
	 * @param leftChannel		an array of floats in the audio range (-1.0f, 1.0f)
	 * @param rightChannel		an array of floats in the audio range (-1.0f, 1.0f)
	 * @param sampleRate		the sample rate for the file
	 * @param fileName			name of the file
	 * @throws IOException		an Exception you'll need to handle when calling this method
	 */
	public static void saveStereoAudioTo32BitPCMFile(float[] leftChannel, float[] rightChannel, float sampleRate, String fileName) throws IOException {
        int numSamples = leftChannel.length;
		// convert leftChannel to 32-bit PCM float
		byte[] audioBytes = new byte[numSamples * 2 * 4];
		int index = 0;
		// convert to IEEE 754 floating-point "single format" bit layout 
		for (int i = 0; i < numSamples; i++) {
           // Left channel sample
            int intBits = Float.floatToIntBits(leftChannel[i]);
            audioBytes[index++] = (byte) (intBits & 0xFF);
            audioBytes[index++] = (byte) ((intBits >> 8) & 0xFF);
            audioBytes[index++] = (byte) ((intBits >> 16) & 0xFF);
            audioBytes[index++] = (byte) ((intBits >> 24) & 0xFF);
            // Right channel sample
            intBits = Float.floatToIntBits(rightChannel[i]);
            audioBytes[index++] = (byte) (intBits & 0xFF);
            audioBytes[index++] = (byte) ((intBits >> 8) & 0xFF);
            audioBytes[index++] = (byte) ((intBits >> 16) & 0xFF);
            audioBytes[index++] = (byte) ((intBits >> 24) & 0xFF);
		}
        // create an AudioInputStream
		ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
		AudioFormat format = new AudioFormat(sampleRate, 32, 2, true, false);
        AudioInputStream audioInputStream = new AudioInputStream(byteStream, format, numSamples);
        // write the file 
        File outFile = new File(fileName);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outFile);      
	}



}
