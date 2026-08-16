package net.paulhertz.pixelaudio.granular;

/** Focused checks for granular buffer/output sample-rate handling. */
public final class PABurstGranularRateDomainTest {
    private static int assertions = 0;

    public static void main(String[] args) {
        equalRatesReduceToMusicalPitch();
        bufferToOutputRatioIsAppliedOnce();
        musicalPitchAndRateRatioComposeOnce();
        grainDurationRemainsInOutputFrames();
        legacyAndExplicitHopsHaveClearDomains();
        rejectsInvalidRates();
        System.out.println("PABurstGranularRateDomainTest: " + assertions + " assertions passed.");
    }

    private static void equalRatesReduceToMusicalPitch() {
        PABurstGranularSource burst = burst(2f, 48000f, 48000f, 4);
        assertClose(2f, burst.getSourceStep(), "equal-rate source step");
        assertRenderedRamp(burst, new float[] { 0f, 2f, 4f, 6f });
    }

    private static void bufferToOutputRatioIsAppliedOnce() {
        PABurstGranularSource burst = burst(1f, 48000f, 24000f, 4);
        assertClose(2f, burst.getSourceStep(), "buffer/output ratio");
        assertRenderedRamp(burst, new float[] { 0f, 2f, 4f, 6f });
    }

    private static void musicalPitchAndRateRatioComposeOnce() {
        PABurstGranularSource burst = burst(0.5f, 48000f, 24000f, 4);
        assertClose(1f, burst.getSourceStep(), "musical pitch times rate ratio");
        assertRenderedRamp(burst, new float[] { 0f, 1f, 2f, 3f });
    }

    private static void grainDurationRemainsInOutputFrames() {
        PABurstGranularSource burst = burst(1f, 48000f, 24000f, 5);
        assertEquals(5L, burst.lengthSamples(), "grain duration uses output frames");
    }

    private static void legacyAndExplicitHopsHaveClearDomains() {
        GestureGranularParams legacy = GestureGranularParams.builder()
                .hopLengthSamples(128)
                .build();
        assertEquals(128L, legacy.eventHopOutputFrames, "legacy event hop");
        assertEquals(128L, legacy.burstTimeHopOutputFrames, "legacy burst time hop");
        assertEquals(128L, legacy.burstSourceIndexHopSamples, "legacy source-index hop");

        GestureGranularParams explicit = GestureGranularParams.builder()
                .hopLengthSamples(128)
                .eventHopOutputFrames(64)
                .burstTimeHopOutputFrames(32)
                .burstSourceIndexHopSamples(7)
                .build();
        assertEquals(64L, explicit.eventHopOutputFrames, "explicit event hop");
        assertEquals(32L, explicit.burstTimeHopOutputFrames, "explicit burst time hop");
        assertEquals(7L, explicit.burstSourceIndexHopSamples, "explicit source-index hop");
    }

    private static void rejectsInvalidRates() {
        boolean threw = false;
        try {
            burst(1f, 0f, 48000f, 4);
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        assertTrue(threw, "zero buffer rate is rejected");
    }

    private static PABurstGranularSource burst(
            float musicalPitchRatio, float bufferRate, float outputRate, int grainFrames) {
        float[] ramp = new float[16];
        for (int i = 0; i < ramp.length; i++) ramp[i] = i;
        return new PABurstGranularSource(
                ramp, 0, grainFrames, 1, 1, 0, musicalPitchRatio, false,
                bufferRate, outputRate);
    }

    private static void assertRenderedRamp(PABurstGranularSource burst, float[] expected) {
        float[] actual = new float[expected.length];
        burst.seekTo(0);
        burst.renderBlock(0, actual.length, actual, null);
        for (int i = 0; i < expected.length; i++) {
            assertClose(expected[i], actual[i], "rendered source position " + i);
        }
    }

    private static void assertClose(float expected, float actual, String message) {
        assertions++;
        if (Math.abs(expected - actual) > 1e-6f) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(long expected, long actual, String message) {
        assertions++;
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
