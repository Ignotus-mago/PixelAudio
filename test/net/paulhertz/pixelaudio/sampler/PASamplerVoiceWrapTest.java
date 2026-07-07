package net.paulhertz.pixelaudio.sampler;

/**
 * Smoke tests for finite sampler wrap-around reads.
 *
 * <p>Run with:</p>
 * <pre>
 * javac -cp "libs/*:src" -d test-bin test/net/paulhertz/pixelaudio/sampler/PASamplerVoiceWrapTest.java
 * java -cp "libs/*:src:test-bin" net.paulhertz.pixelaudio.sampler.PASamplerVoiceWrapTest
 * </pre>
 */
public final class PASamplerVoiceWrapTest {
    private static int assertions = 0;

    public static void main(String[] args) {
        finiteVoiceWrapsAtBufferEnd();
        System.out.println("PASamplerVoiceWrapTest: " + assertions + " assertions passed.");
    }

    private static void finiteVoiceWrapsAtBufferEnd() {
        float[] buffer = new float[] { 1f, 2f, 3f };
        PASamplerVoice voice = new PASamplerVoice(buffer, 48000f);

        voice.activate(2, 4, 1f, null, 1f, 0f, false, true);

        assertClose(3f, voice.nextSample(), "first sample starts at requested position");
        assertClose(1f, voice.nextSample(), "second sample wraps to buffer start");
        assertClose(2f, voice.nextSample(), "third sample continues after wrap");
        assertClose(3f, voice.nextSample(), "fourth sample preserves requested finite duration");
    }

    private static void assertClose(float expected, float actual, String message) {
        assertions++;
        if (Math.abs(expected - actual) > 1e-6f) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }
}
