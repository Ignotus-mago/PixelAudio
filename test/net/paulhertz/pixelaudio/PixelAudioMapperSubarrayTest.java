package net.paulhertz.pixelaudio;

import java.util.Arrays;

import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;

/**
 * Lightweight unit tests for PixelAudioMapper pluck, plant, peel, and stamp methods.
 *
 * Run from the project root with:
 * javac -cp "libs/*:src" -d test-bin test/net/paulhertz/pixelaudio/PixelAudioMapperSubarrayTest.java
 * java -cp "libs/*:src:test-bin" net.paulhertz.pixelaudio.PixelAudioMapperSubarrayTest
 */
public final class PixelAudioMapperSubarrayTest {
    private static final int[] HILBERT_4_SIGNAL_TO_IMAGE = {
        0, 1, 5, 4,
        8, 12, 13, 9,
        10, 14, 15, 11,
        7, 6, 2, 3
    };

    private int testsRun = 0;
    private int testsFailed = 0;

    public static void main(String[] args) {
        PixelAudioMapperSubarrayTest suite = new PixelAudioMapperSubarrayTest();
        suite.run();
    }

    private void run() {
        runTest("pluckPixels follows Hilbert path", this::testPluckPixelsFollowsHilbertPath);
        runTest("pluckPixelsAsAudio transcodes selected channel", this::testPluckPixelsAsAudioTranscodesSelectedChannel);
        runTest("pluckSamples returns requested subarray", this::testPluckSamplesReturnsRequestedSubarray);
        runTest("pluckSamplesAsRGB transcodes to gray", this::testPluckSamplesAsRGBTranscodesToGray);
        runTest("plantPixels follows Hilbert path", this::testPlantPixelsFollowsHilbertPath);
        runTest("plantPixels writes selected channel along Hilbert path", this::testPlantPixelsWritesSelectedChannelAlongHilbertPath);
        runTest("plant audio writes selected channel along Hilbert path", this::testPlantAudioWritesSelectedChannelAlongHilbertPath);
        runTest("plantSamples copies audio subarray", this::testPlantSamplesCopiesAudioSubarray);
        runTest("plantSamples transcodes RGB to audio", this::testPlantSamplesTranscodesRgbToAudio);
        runTest("plantSamples transcodes selected channel", this::testPlantSamplesTranscodesSelectedChannel);
        runTest("peelPixels reads rectangle in image order", this::testPeelPixelsReadsRectangleInImageOrder);
        runTest("peelPixelsAsAudio transcodes rectangle", this::testPeelPixelsAsAudioTranscodesRectangle);
        runTest("peelSamples reads mapped rectangle", this::testPeelSamplesReadsMappedRectangle);
        runTest("peelSamplesAsRGB transcodes mapped rectangle", this::testPeelSamplesAsRGBTranscodesMappedRectangle);
        runTest("stampPixels writes rectangle in image order", this::testStampPixelsWritesRectangleInImageOrder);
        runTest("stampPixels writes selected channel in rectangle", this::testStampPixelsWritesSelectedChannelInRectangle);
        runTest("stamp audio writes selected channel in rectangle", this::testStampAudioWritesSelectedChannelInRectangle);
        runTest("stampSamples writes mapped rectangle", this::testStampSamplesWritesMappedRectangle);
        runTest("stampSamples transcodes RGB to mapped audio", this::testStampSamplesTranscodesRgbToMappedAudio);

        if (testsFailed > 0) {
            throw new AssertionError("PixelAudioMapperSubarrayTest: " + testsFailed + " test groups failed, "
                    + testsRun + " assertions passed.");
        }
        System.out.println("PixelAudioMapperSubarrayTest: " + testsRun + " assertions passed.");
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

    private void testPluckPixelsFollowsHilbertPath() {
        PixelAudioMapper mapper = mapper4x4();
        int[] image = imageWithRedEqualToImagePos(mapper.getSize());

        int[] plucked = mapper.pluckPixels(image, 2, 4);

        assertRedValues(new int[] {5, 4, 8, 12}, plucked, "pluckPixels");
    }

    private void testPluckPixelsAsAudioTranscodesSelectedChannel() {
        PixelAudioMapper mapper = mapper4x4();
        int[] image = imageWithRedEqualToImagePos(mapper.getSize());

        float[] plucked = mapper.pluckPixelsAsAudio(image, 2, 4, ChannelNames.R);

        assertFloatArrayEquals(new float[] {
                PixelAudioMapper.rgbChanToAudio(5),
                PixelAudioMapper.rgbChanToAudio(4),
                PixelAudioMapper.rgbChanToAudio(8),
                PixelAudioMapper.rgbChanToAudio(12)
        }, plucked, 0.000001f, "pluckPixelsAsAudio");
    }

    private void testPluckSamplesReturnsRequestedSubarray() {
        PixelAudioMapper mapper = mapper4x4();
        float[] signal = signalWithTenths(mapper.getSize());

        float[] plucked = mapper.pluckSamples(signal, 2, 4);

        assertFloatArrayEquals(new float[] {0.2f, 0.3f, 0.4f, 0.5f}, plucked, 0.000001f, "pluckSamples");
    }

    private void testPluckSamplesAsRGBTranscodesToGray() {
        PixelAudioMapper mapper = mapper4x4();
        float[] signal = {-1.0f, -0.5f, 0.0f, 0.5f, 1.0f};

        int[] plucked = mapper.pluckSamplesAsRGB(signal, 1, 4);

        assertGrayValues(new int[] {64, 128, 191, 255}, plucked, "pluckSamplesAsRGB");
    }

    private void testPlantPixelsFollowsHilbertPath() {
        PixelAudioMapper mapper = mapper4x4();
        int[] image = filledColorArray(mapper.getSize(), PixelAudioMapper.composeColor(0, 0, 0));
        int[] sprout = colorsFromRedValues(101, 102, 103, 104);

        mapper.plantPixels(sprout, image, 2, 4);

        assertImageRedAtSignalPositions(new int[] {2, 3, 4, 5}, new int[] {101, 102, 103, 104}, image,
                "plantPixels");
    }

    private void testPlantPixelsWritesSelectedChannelAlongHilbertPath() {
        PixelAudioMapper mapper = mapper4x4();
        int[] image = filledColorArray(mapper.getSize(), PixelAudioMapper.composeColor(10, 20, 30));
        int[] sprout = colorsFromRedValues(101, 102, 103, 104);

        mapper.plantPixels(sprout, image, 2, 4, ChannelNames.R);

        for (int signalPos = 2; signalPos < 6; signalPos++) {
            int imagePos = HILBERT_4_SIGNAL_TO_IMAGE[signalPos];
            int expectedRed = 99 + signalPos;
            assertRgb(expectedRed, 20, 30, image[imagePos], "plantPixels selected channel at " + signalPos);
        }
    }

    private void testPlantAudioWritesSelectedChannelAlongHilbertPath() {
        PixelAudioMapper mapper = mapper4x4();
        int[] image = filledColorArray(mapper.getSize(), PixelAudioMapper.composeColor(10, 20, 30));
        float[] sprout = {-1.0f, -0.5f, 0.0f, 1.0f};

        mapper.plantPixels(sprout, image, 2, 4, ChannelNames.G);

        int[] expectedGreen = {0, 64, 128, 255};
        for (int j = 0; j < expectedGreen.length; j++) {
            int imagePos = HILBERT_4_SIGNAL_TO_IMAGE[2 + j];
            assertRgb(10, expectedGreen[j], 30, image[imagePos], "plant audio selected channel at " + j);
        }
    }

    private void testPlantSamplesCopiesAudioSubarray() {
        PixelAudioMapper mapper = mapper4x4();
        float[] signal = new float[mapper.getSize()];
        float[] sprout = {0.11f, 0.22f, 0.33f, 0.44f};

        mapper.plantSamples(sprout, signal, 2, 4);

        assertFloatArrayEquals(sprout, Arrays.copyOfRange(signal, 2, 6), 0.000001f, "plantSamples audio");
    }

    private void testPlantSamplesTranscodesRgbToAudio() {
        PixelAudioMapper mapper = mapper4x4();
        float[] signal = new float[mapper.getSize()];
        int[] sprout = colorsFromRedValues(0, 64, 128, 255);

        mapper.plantSamples(sprout, signal, 2, 4);

        for (int j = 0; j < sprout.length; j++) {
            float expected = PixelAudioMapper.rgbChanToAudio(PixelAudioMapper.getLuminosity(sprout[j]));
            assertFloatEquals(expected, signal[2 + j], 0.000001f, "plantSamples RGB at " + j);
        }
    }

    private void testPlantSamplesTranscodesSelectedChannel() {
        PixelAudioMapper mapper = mapper4x4();
        float[] signal = new float[mapper.getSize()];
        int[] sprout = {
                PixelAudioMapper.composeColor(0, 10, 0),
                PixelAudioMapper.composeColor(0, 20, 0),
                PixelAudioMapper.composeColor(0, 30, 0),
                PixelAudioMapper.composeColor(0, 40, 0)
        };

        mapper.plantSamples(sprout, signal, 2, 4, ChannelNames.G);

        assertFloatArrayEquals(new float[] {
                PixelAudioMapper.rgbChanToAudio(10),
                PixelAudioMapper.rgbChanToAudio(20),
                PixelAudioMapper.rgbChanToAudio(30),
                PixelAudioMapper.rgbChanToAudio(40)
        }, Arrays.copyOfRange(signal, 2, 6), 0.000001f, "plantSamples selected channel");
    }

    private void testPeelPixelsReadsRectangleInImageOrder() {
        PixelAudioMapper mapper = mapper4x4();
        int[] image = imageWithRedEqualToImagePos(mapper.getSize());

        int[] peeled = mapper.peelPixels(image, 1, 1, 2, 2);

        assertRedValues(new int[] {5, 6, 9, 10}, peeled, "peelPixels");
    }

    private void testPeelPixelsAsAudioTranscodesRectangle() {
        PixelAudioMapper mapper = mapper4x4();
        int[] image = imageWithRedEqualToImagePos(mapper.getSize());

        float[] peeled = mapper.peelPixelsAsAudio(image, 1, 1, 2, 2);

        assertFloatArrayEquals(new float[] {
                PixelAudioMapper.rgbChanToAudio(PixelAudioMapper.getLuminosity(image[5])),
                PixelAudioMapper.rgbChanToAudio(PixelAudioMapper.getLuminosity(image[6])),
                PixelAudioMapper.rgbChanToAudio(PixelAudioMapper.getLuminosity(image[9])),
                PixelAudioMapper.rgbChanToAudio(PixelAudioMapper.getLuminosity(image[10]))
        }, peeled, 0.000001f, "peelPixelsAsAudio");
    }

    private void testPeelSamplesReadsMappedRectangle() {
        PixelAudioMapper mapper = mapper4x4();
        float[] signal = signalWithTenths(mapper.getSize());

        float[] peeled = mapper.peelSamples(signal, 1, 1, 2, 2);

        assertFloatArrayEquals(new float[] {0.2f, 1.3f, 0.7f, 0.8f}, peeled, 0.000001f, "peelSamples");
    }

    private void testPeelSamplesAsRGBTranscodesMappedRectangle() {
        PixelAudioMapper mapper = mapper4x4();
        float[] signal = signalFromRgbValues(mapper.getSize());

        int[] peeled = mapper.peelSamplesAsRGB(signal, 1, 1, 2, 2);

        assertGrayValues(new int[] {2, 13, 7, 8}, peeled, "peelSamplesAsRGB");
    }

    private void testStampPixelsWritesRectangleInImageOrder() {
        PixelAudioMapper mapper = mapper4x4();
        int[] image = filledColorArray(mapper.getSize(), PixelAudioMapper.composeColor(0, 0, 0));
        int[] stamp = colorsFromRedValues(101, 102, 103, 104);

        mapper.stampPixels(stamp, image, 1, 1, 2, 2);

        assertRedValuesAtImagePositions(new int[] {5, 6, 9, 10}, new int[] {101, 102, 103, 104}, image,
                "stampPixels");
    }

    private void testStampPixelsWritesSelectedChannelInRectangle() {
        PixelAudioMapper mapper = mapper4x4();
        int[] image = filledColorArray(mapper.getSize(), PixelAudioMapper.composeColor(10, 20, 30));
        int[] stamp = colorsFromRedValues(101, 102, 103, 104);

        mapper.stampPixels(stamp, image, 1, 1, 2, 2, ChannelNames.R);

        int[] imagePositions = {5, 6, 9, 10};
        for (int j = 0; j < imagePositions.length; j++) {
            assertRgb(101 + j, 20, 30, image[imagePositions[j]], "stampPixels selected channel at " + j);
        }
    }

    private void testStampAudioWritesSelectedChannelInRectangle() {
        PixelAudioMapper mapper = mapper4x4();
        int[] image = filledColorArray(mapper.getSize(), PixelAudioMapper.composeColor(10, 20, 30));
        float[] stamp = {-1.0f, -0.5f, 0.0f, 1.0f};

        mapper.stampPixels(stamp, image, 1, 1, 2, 2, ChannelNames.B);

        int[] imagePositions = {5, 6, 9, 10};
        int[] expectedBlue = {0, 64, 128, 255};
        for (int j = 0; j < imagePositions.length; j++) {
            assertRgb(10, 20, expectedBlue[j], image[imagePositions[j]], "stamp audio selected channel at " + j);
        }
    }

    private void testStampSamplesWritesMappedRectangle() {
        PixelAudioMapper mapper = mapper4x4();
        float[] signal = new float[mapper.getSize()];
        float[] stamp = {0.11f, 0.22f, 0.33f, 0.44f};

        mapper.stampSamples(stamp, signal, 1, 1, 2, 2);

        assertFloatEquals(0.11f, signal[2], 0.000001f, "stampSamples imagePos 5");
        assertFloatEquals(0.22f, signal[13], 0.000001f, "stampSamples imagePos 6");
        assertFloatEquals(0.33f, signal[7], 0.000001f, "stampSamples imagePos 9");
        assertFloatEquals(0.44f, signal[8], 0.000001f, "stampSamples imagePos 10");
    }

    private void testStampSamplesTranscodesRgbToMappedAudio() {
        PixelAudioMapper mapper = mapper4x4();
        float[] signal = new float[mapper.getSize()];
        int[] stamp = colorsFromRedValues(0, 64, 128, 255);

        mapper.stampSamples(stamp, signal, 1, 1, 2, 2);

        int[] signalPositions = {2, 13, 7, 8};
        for (int j = 0; j < stamp.length; j++) {
            float expected = PixelAudioMapper.rgbChanToAudio(PixelAudioMapper.getLuminosity(stamp[j]));
            assertFloatEquals(expected, signal[signalPositions[j]], 0.000001f, "stampSamples RGB at " + j);
        }
    }

    private PixelAudioMapper mapper4x4() {
        return new PixelAudioMapper(new HilbertGen(4, 4));
    }

    private int[] filledColorArray(int length, int color) {
        int[] colors = new int[length];
        Arrays.fill(colors, color);
        return colors;
    }

    private int[] imageWithRedEqualToImagePos(int length) {
        int[] image = new int[length];
        for (int imagePos = 0; imagePos < image.length; imagePos++) {
            image[imagePos] = PixelAudioMapper.composeColor(imagePos, 0, 0);
        }
        return image;
    }

    private float[] signalWithTenths(int length) {
        float[] signal = new float[length];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i / 10.0f;
        }
        return signal;
    }

    private float[] signalFromRgbValues(int length) {
        float[] signal = new float[length];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = PixelAudioMapper.rgbChanToAudio(i);
        }
        return signal;
    }

    private int[] colorsFromRedValues(int... redValues) {
        int[] colors = new int[redValues.length];
        for (int i = 0; i < redValues.length; i++) {
            colors[i] = PixelAudioMapper.composeColor(redValues[i], 0, 0);
        }
        return colors;
    }

    private void assertImageRedAtSignalPositions(int[] signalPositions, int[] expectedRedValues, int[] image,
            String label) {
        for (int i = 0; i < signalPositions.length; i++) {
            int imagePos = HILBERT_4_SIGNAL_TO_IMAGE[signalPositions[i]];
            assertEquals(expectedRedValues[i], PixelAudioMapper.rgbComponents(image[imagePos])[0],
                    label + " at signalPos " + signalPositions[i]);
        }
    }

    private void assertRedValuesAtImagePositions(int[] imagePositions, int[] expectedRedValues, int[] image,
            String label) {
        for (int i = 0; i < imagePositions.length; i++) {
            assertEquals(expectedRedValues[i], PixelAudioMapper.rgbComponents(image[imagePositions[i]])[0],
                    label + " at imagePos " + imagePositions[i]);
        }
    }

    private void assertRedValues(int[] expectedRedValues, int[] colors, String label) {
        for (int i = 0; i < expectedRedValues.length; i++) {
            assertEquals(expectedRedValues[i], PixelAudioMapper.rgbComponents(colors[i])[0], label + " red at " + i);
        }
    }

    private void assertGrayValues(int[] expectedGrayValues, int[] colors, String label) {
        for (int i = 0; i < expectedGrayValues.length; i++) {
            assertRgb(expectedGrayValues[i], expectedGrayValues[i], expectedGrayValues[i], colors[i],
                    label + " gray at " + i);
        }
    }

    private void assertRgb(int expectedR, int expectedG, int expectedB, int actual, String label) {
        int[] rgb = PixelAudioMapper.rgbComponents(actual);
        assertEquals(expectedR, rgb[0], label + " red");
        assertEquals(expectedG, rgb[1], label + " green");
        assertEquals(expectedB, rgb[2], label + " blue");
    }

    private void assertEquals(int expected, int actual, String label) {
        testsRun++;
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private void assertFloatEquals(float expected, float actual, float tolerance, String label) {
        testsRun++;
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private void assertFloatArrayEquals(float[] expected, float[] actual, float tolerance, String label) {
        testsRun++;
        if (expected.length != actual.length) {
            throw new AssertionError(label + ": expected length " + expected.length + ", got " + actual.length);
        }
        for (int i = 0; i < expected.length; i++) {
            if (Math.abs(expected[i] - actual[i]) > tolerance) {
                throw new AssertionError(label + " at " + i + ": expected " + expected[i] + ", got " + actual[i]);
            }
        }
    }
}
