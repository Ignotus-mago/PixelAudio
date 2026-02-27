package net.paulhertz.pixelaudio.schedule;

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;

/** Utility conversions for audio. */
public final class AudioUtility {
    private AudioUtility() {}

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


}
