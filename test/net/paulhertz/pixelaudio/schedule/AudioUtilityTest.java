package net.paulhertz.pixelaudio.schedule;

import ddf.minim.MultiChannelBuffer;

/**
 * Lightweight unit tests for AudioUtility mix protection helpers.
 *
 * Run from the project root with:
 * javac -cp "libs/*:src" -d test-bin test/net/paulhertz/pixelaudio/schedule/AudioUtilityTest.java
 * java -cp "libs/*:src:test-bin" net.paulhertz.pixelaudio.schedule.AudioUtilityTest
 */
public final class AudioUtilityTest {
    private int testsRun = 0;
    private int testsFailed = 0;

    public static void main(String[] args) {
        AudioUtilityTest suite = new AudioUtilityTest();
        suite.run();
    }

    private void run() {
        runTest("removeDCOffset recenters signal", this::testRemoveDCOffsetRecentersSignal);
        runTest("hardClip reports clipped samples", this::testHardClipReportsClippedSamples);
        runTest("protectPeak only attenuates overloads", this::testProtectPeakOnlyAttenuatesOverloads);
        runTest("softLimitSoftsign follows expected curve", this::testSoftLimitSoftsignFollowsExpectedCurve);
        runTest("MultiChannelBuffer helpers protect all channels", this::testMultiChannelBufferHelpers);
        runTest("conditionForOutput removes DC and enforces ceiling", this::testConditionForOutput);

        if (testsFailed > 0) {
            throw new AssertionError("AudioUtilityTest: " + testsFailed + " test groups failed, "
                    + testsRun + " assertions passed.");
        }
        System.out.println("AudioUtilityTest: " + testsRun + " assertions passed.");
    }

    private void runTest(String name, Runnable test) {
        try {
            test.run();
        }
        catch (Throwable throwable) {
            testsFailed++;
            System.err.println("FAIL " + name + ": " + throwable.getMessage());
        }
    }

    private void testRemoveDCOffsetRecentersSignal() {
        float[] signal = {0.25f, 0.75f, 1.25f};

        float offset = AudioUtility.removeDCOffset(signal);

        assertFloatEquals(0.75f, offset, 0.000001f, "removed offset");
        assertFloatEquals(0f, AudioUtility.computeDCOffset(signal), 0.000001f, "recentered signal");
        assertFloatEquals(0.5f, AudioUtility.computePeak(signal), 0.000001f, "peak after recentering");
    }

    private void testHardClipReportsClippedSamples() {
        float[] signal = {-1.25f, -0.25f, 0.2f, 1.4f};

        int clipped = AudioUtility.hardClip(signal, 0.8f);

        assertEquals(2, clipped, "clipped sample count");
        assertFloatEquals(-0.8f, signal[0], 0.000001f, "negative clip");
        assertFloatEquals(0.8f, signal[3], 0.000001f, "positive clip");
        assertEquals(0, AudioUtility.countOverloads(signal, 0.8f), "no remaining overloads");
    }

    private void testProtectPeakOnlyAttenuatesOverloads() {
        float[] signal = {-0.5f, 0.75f, 1.5f};

        float gain = AudioUtility.protectPeak(signal, 0.75f);

        assertFloatEquals(0.5f, gain, 0.000001f, "gain");
        assertFloatEquals(0.75f, AudioUtility.computePeak(signal), 0.000001f, "protected peak");

        float[] quiet = {-0.25f, 0.5f};
        float quietGain = AudioUtility.protectPeak(quiet, 0.75f);
        assertFloatEquals(1f, quietGain, 0.000001f, "quiet gain unchanged");
        assertFloatEquals(0.5f, quiet[1], 0.000001f, "quiet sample unchanged");
    }

    private void testSoftLimitSoftsignFollowsExpectedCurve() {
        float[] signal = {-1f, 0f, 1f};

        AudioUtility.softLimitSoftsign(signal, 2f);

        assertFloatEquals(-0.6666667f, signal[0], 0.000001f, "negative soft limit");
        assertFloatEquals(0f, signal[1], 0.000001f, "zero soft limit");
        assertFloatEquals(0.6666667f, signal[2], 0.000001f, "positive soft limit");

        float unchanged = AudioUtility.softClipSoftsign(0.75f, 0f);
        assertFloatEquals(0.75f, unchanged, 0.000001f, "zero drive leaves sample unchanged");
    }

    private void testMultiChannelBufferHelpers() {
        MultiChannelBuffer buffer = new MultiChannelBuffer(3, 2);
        buffer.setChannel(0, new float[] {-0.5f, 0.5f, 1.5f});
        buffer.setChannel(1, new float[] {-2.0f, 0.25f, 0.5f});

        assertFloatEquals(2.0f, AudioUtility.computePeak(buffer), 0.000001f, "multi-channel peak");
        assertEquals(2, AudioUtility.countOverloads(buffer, 1.0f), "multi-channel overload count");

        float gain = AudioUtility.protectPeak(buffer, 1.0f);

        assertFloatEquals(0.5f, gain, 0.000001f, "multi-channel gain");
        assertFloatEquals(1.0f, AudioUtility.computePeak(buffer), 0.000001f, "multi-channel protected peak");
        assertFloatEquals(0.75f, buffer.getChannel(0)[2], 0.000001f, "channel 0 scaled");
        assertFloatEquals(-1.0f, buffer.getChannel(1)[0], 0.000001f, "channel 1 scaled");
    }

    private void testConditionForOutput() {
        float[] signal = {0f, 1f, 2f};

        float gain = AudioUtility.conditionForOutput(signal, -6.0f, 0f, true);

        assertTrue(gain < 1f, "conditioning attenuates overload");
        assertFloatEquals(0f, AudioUtility.computeDCOffset(signal), 0.000001f, "conditioning removes DC");
        assertTrue(AudioUtility.computePeak(signal) <= AudioUtility.dbToLinear(-6.0f) + 0.000001f,
                "conditioning enforces ceiling");
    }

    private void assertEquals(int expected, int actual, String label) {
        testsRun++;
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private void assertTrue(boolean condition, String label) {
        testsRun++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private void assertFloatEquals(float expected, float actual, float tolerance, String label) {
        testsRun++;
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
