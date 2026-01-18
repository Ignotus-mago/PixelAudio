package net.paulhertz.pixelaudio.schedule;

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

}
