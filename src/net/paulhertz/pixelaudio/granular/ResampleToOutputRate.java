package net.paulhertz.pixelaudio.granular;

import ddf.minim.MultiChannelBuffer;
import ddf.minim.AudioOutput;

/**
 * ResampleToOutputRate
 *
 * Utility functions to resample decoded audio (e.g. from Minim.loadFileIntoBuffer)
 * to match the AudioOutput sample rate.
 *
 * Uses simple linear interpolation. Good enough for most performance uses,
 * and done only once per file load.
 */
public final class ResampleToOutputRate {

    private ResampleToOutputRate() {
        // no instances
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
