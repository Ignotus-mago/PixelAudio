package net.paulhertz.pixelaudio.voices;

/** Utility functions for ADSRParams. */
public final class ADSRUtils {
    private ADSRUtils() {} // no instantiation

    /**
     * Returns a new ADSRParams scaled to fit the specified duration (ms).
     */
    public static ADSRParams fitEnvelopeToDuration(ADSRParams adsr, int duration) {
        if (adsr == null) {
            throw new IllegalArgumentException("ADSRParams must not be null");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        float totalSeconds = duration / 1000f;
        float totalEnv = adsr.getAttack() + adsr.getDecay() + adsr.getRelease();

        if (totalEnv <= 0.0f) {
            return new ADSRParams(adsr.getMaxAmp(), 0, 0, adsr.getSustain(), 0);
        }

        float scale = totalSeconds / totalEnv;
        return new ADSRParams(
            adsr.getMaxAmp(),
            adsr.getAttack()  * scale,
            adsr.getDecay()   * scale,
            adsr.getSustain(),
            adsr.getRelease() * scale
        );
    }
}
