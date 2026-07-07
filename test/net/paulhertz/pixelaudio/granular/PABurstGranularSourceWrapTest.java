package net.paulhertz.pixelaudio.granular;

/**
 * Smoke tests for finite granular wrap-around reads.
 *
 * <p>Run with:</p>
 * <pre>
 * javac -cp "libs/*:src" -d test-bin test/net/paulhertz/pixelaudio/granular/PABurstGranularSourceWrapTest.java
 * java -cp "libs/*:src:test-bin" net.paulhertz.pixelaudio.granular.PABurstGranularSourceWrapTest
 * </pre>
 */
public final class PABurstGranularSourceWrapTest {
    private static int assertions = 0;

    public static void main(String[] args) {
        finiteGrainWrapsAtBufferEnd();
        System.out.println("PABurstGranularSourceWrapTest: " + assertions + " assertions passed.");
    }

    private static void finiteGrainWrapsAtBufferEnd() {
        float[] source = new float[] { 10f, 20f, 30f };
        PABurstGranularSource burst = new PABurstGranularSource(
                source,
                2,
                3,
                1,
                1,
                0,
                1f,
                true
        );

        float[] out = new float[3];
        burst.seekTo(0);
        burst.renderBlock(0, out.length, out, null);

        assertClose(30f, out[0], "grain starts at requested source index");
        assertClose(10f, out[1], "grain wraps to source start");
        assertClose(20f, out[2], "grain continues after wrap");
    }

    private static void assertClose(float expected, float actual, String message) {
        assertions++;
        if (Math.abs(expected - actual) > 1e-6f) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }
}
