package net.paulhertz.pixelaudio.sampler;

/**
 * Smoke tests for complete ADSR parameter descriptions.
 *
 * <p>Run with:</p>
 * <pre>
 * javac -cp "libs/*:src" -d test-bin test/net/paulhertz/pixelaudio/sampler/ADSRParamsTest.java
 * java -cp "libs/*:src:test-bin" net.paulhertz.pixelaudio.sampler.ADSRParamsTest
 * </pre>
 */
public final class ADSRParamsTest {
    private static int assertions = 0;

    public static void main(String[] args) {
        legacyConstructorUsesSimpleAdsrDefaults();
        copyPreservesCurves();
        durationScalingPreservesCurves();
        simpleAdsrUsesConfiguredCurves();
        System.out.println("ADSRParamsTest: " + assertions + " assertions passed.");
    }

    private static void legacyConstructorUsesSimpleAdsrDefaults() {
        ADSRParams params = new ADSRParams(1f, 0.1f, 0.2f, 0.5f, 0.3f);

        assertClose(ADSRParams.DEFAULT_CURVE, params.getAttackCurve(), "default attack curve");
        assertClose(ADSRParams.DEFAULT_CURVE, params.getDecayCurve(), "default decay curve");
        assertClose(ADSRParams.DEFAULT_CURVE, params.getReleaseCurve(), "default release curve");
    }

    private static void copyPreservesCurves() {
        ADSRParams original = new ADSRParams(0.8f, 0.1f, 0.2f, 0.5f, 0.3f,
                1f, 2f, 8f);
        ADSRParams copy = original.copy();

        assertClose(1f, copy.getAttackCurve(), "copy attack curve");
        assertClose(2f, copy.getDecayCurve(), "copy decay curve");
        assertClose(8f, copy.getReleaseCurve(), "copy release curve");
    }

    private static void durationScalingPreservesCurves() {
        ADSRParams original = new ADSRParams(0.8f, 0.1f, 0.2f, 0.5f, 0.3f,
                1f, 2f, 8f);
        ADSRParams scaled = ADSRUtils.fitEnvelopeToDuration(original, 1200);

        assertClose(1f, scaled.getAttackCurve(), "scaled attack curve");
        assertClose(2f, scaled.getDecayCurve(), "scaled decay curve");
        assertClose(8f, scaled.getReleaseCurve(), "scaled release curve");
    }

    private static void simpleAdsrUsesConfiguredCurves() {
        ADSRParams linear = new ADSRParams(1f, 1f, 0f, 1f, 0f,
                1f, 1f, 1f);
        ADSRParams curved = linear.withCurves(4f, 1f, 1f);

        SimpleADSR linearEnvelope = linear.toSimpleADSR(4f);
        SimpleADSR curvedEnvelope = curved.toSimpleADSR(4f);
        linearEnvelope.noteOn();
        curvedEnvelope.noteOn();

        linearEnvelope.tick();
        curvedEnvelope.tick();
        float linearSecondTick = linearEnvelope.tick();
        float curvedSecondTick = curvedEnvelope.tick();

        assertClose(0.25f, linearSecondTick, "linear attack interpolation");
        assertTrue(curvedSecondTick < linearSecondTick, "curved attack starts more slowly");
    }

    private static void assertClose(float expected, float actual, String message) {
        assertions++;
        if (Math.abs(expected - actual) > 1e-6f) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
