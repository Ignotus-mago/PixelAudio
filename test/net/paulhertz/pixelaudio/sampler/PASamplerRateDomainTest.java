package net.paulhertz.pixelaudio.sampler;

import ddf.minim.MultiChannelBuffer;

/** Smoke tests for source-buffer and output sample-rate separation. */
public final class PASamplerRateDomainTest {
    private static int assertions = 0;

    public static void main(String[] args) {
        voiceConvertsMusicalPitchOnce();
        instrumentPassesMusicalPitchOnce();
        envelopeUsesOutputClock();
        durationIsReportedInOutputSamples();
        System.out.println("PASamplerRateDomainTest: " + assertions + " assertions passed.");
    }

    private static void instrumentPassesMusicalPitchOnce() {
        CapturingSampler sampler = new CapturingSampler();
        PASamplerInstrument instrument = new PASamplerInstrument(
                new MultiChannelBuffer(16, 1), null, sampler,
                new ADSRParams(1f, 0f, 0f, 1f, 0f), 96000f);
        instrument.setPitchScale(2f);

        instrument.play(0, 16, 1f, null, 1f, 0f);
        assertClose(2f, sampler.lastPitch,
                "instrument applies global musical pitch but not the sample-rate ratio");

        instrument.playSample(0, 16, 1f);
        assertClose(2f, sampler.lastPitch,
                "convenience overload does not apply global pitch twice");
    }

    private static void voiceConvertsMusicalPitchOnce() {
        PASamplerVoice voice = new PASamplerVoice(new float[16], 96000f, 48000f);
        voice.activate(0, 16, 1f, null, 1f, 0f, false);
        assertClose(2f, voice.getSourceStep(), "unity pitch preserves a 96 kHz source at 48 kHz output");

        voice.activate(0, 16, 1f, null, 0.5f, 0f, false);
        assertClose(1f, voice.getSourceStep(), "musical pitch is applied once after rate conversion");
    }

    private static void envelopeUsesOutputClock() {
        float[] buffer = new float[32];
        java.util.Arrays.fill(buffer, 1f);
        PASamplerVoice voice = new PASamplerVoice(buffer, 2000f, 1000f);
        ADSRParams env = new ADSRParams(
                1f, 0.004f, 0.001f, 1f, 0.001f, 1f, 1f, 1f);
        voice.activate(0, 32, 1f, env, 1f, 0f, false);

        voice.nextSample();
        voice.nextSample();
        voice.nextSample();
        float fourthOutputSample = voice.nextSample();
        assertClose(0.75f, fourthOutputSample,
                "four-millisecond attack uses four ticks at a 1 kHz output rate");
    }

    private static void durationIsReportedInOutputSamples() {
        ADSRParams env = new ADSRParams(1f, 0.2f, 0.1f, 0.8f, 0.1f);
        int duration = PlaybackInfo.computeVoiceDuration(
                0, 2000, 4000, 1f, env, false, 2000f, 1000f, false);
        assertEquals(1100, duration, "one-second source window plus output-clock release");

        int octaveUp = PlaybackInfo.computeVoiceDuration(
                0, 2000, 4000, 2f, env, false, 2000f, 1000f, false);
        assertEquals(600, octaveUp, "octave-up pitch halves the note window but not release");
    }

    private static void assertClose(float expected, float actual, String message) {
        assertions++;
        if (Math.abs(expected - actual) > 1e-6f) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        assertions++;
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }

    private static final class CapturingSampler implements PASampler {
        float lastPitch;

        @Override
        public int play(int samplePos, int sampleLen, float amplitude,
                ADSRParams env, float pitch, float pan) {
            lastPitch = pitch;
            return sampleLen;
        }

        @Override
        public void startAtSampleTime(int samplePos, int sampleLen, float amplitude,
                ADSRParams env, float pitch, float pan, long startSample) {
            lastPitch = pitch;
        }

        @Override
        public void startAfterDelaySamples(int samplePos, int sampleLen, float amplitude,
                ADSRParams env, float pitch, float pan, long delaySamples) {
            lastPitch = pitch;
        }

        @Override public long getCurrentSampleTime() { return 0; }
        @Override public void clearScheduled() { }
        @Override public void setWrapAround(boolean wrapAround) { }
        @Override public boolean isWrapAround() { return false; }
        @Override public boolean isLooping() { return false; }
        @Override public void stopAll() { }
        @Override public void releaseAll() { }
        @Override public void setBufferSampleRate(float newRate) { }
        @Override public void setBuffer(float[] buffer) { }
        @Override public void setBuffer(float[] buffer, float bufferSampleRate) { }
    }
}
