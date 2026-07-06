package net.paulhertz.pixelaudio;

import java.util.Arrays;

import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;

/**
 * Lightweight unit tests for PixelAudioMapper.
 *
 * Run from the project root with:
 * javac -cp "libs/*:src" -d test-bin test/net/paulhertz/pixelaudio/PixelAudioMapperTest.java
 * java -cp "libs/*:src:test-bin" net.paulhertz.pixelaudio.PixelAudioMapperTest
 */
public final class PixelAudioMapperTest {
    private static final int[] HILBERT_4_SIGNAL_TO_IMAGE = {
        0, 1, 5, 4,
        8, 12, 13, 9,
        10, 14, 15, 11,
        7, 6, 2, 3
    };

    private int testsRun = 0;

    public static void main(String[] args) {
        PixelAudioMapperTest suite = new PixelAudioMapperTest();
        suite.run();
    }

    private void run() {
        testConstructorCopiesHilbertDimensionsAndLuts();
        testLookupTablesAreInverses();
        testReturnedLutsAreCopies();
        testMapSignalToImageFollowsHilbertPath();
        testMapImageToSignalFollowsHilbertPath();
        testShiftedLookupsAreConsistentInverses();
        testArrayLengthMismatchThrows();

        System.out.println("PixelAudioMapperTest: " + testsRun + " tests passed.");
    }

    private void testConstructorCopiesHilbertDimensionsAndLuts() {
        PixelAudioMapper mapper = mapper4x4();

        assertEquals(4, mapper.getWidth(), "width");
        assertEquals(4, mapper.getHeight(), "height");
        assertEquals(16, mapper.getSize(), "size");
        assertArrayEquals(HILBERT_4_SIGNAL_TO_IMAGE, mapper.getSignalToImageLUT(), "signalToImageLUT");
    }

    private void testLookupTablesAreInverses() {
        PixelAudioMapper mapper = mapper4x4();
        int[] signalToImage = mapper.getSignalToImageLUT();
        int[] imageToSignal = mapper.getImageToSignalLUT();

        for (int signalPos = 0; signalPos < mapper.getSize(); signalPos++) {
            int imagePos = signalToImage[signalPos];
            assertEquals(signalPos, imageToSignal[imagePos], "imageToSignal inverse at imagePos " + imagePos);
            assertEquals(imagePos, mapper.lookupImagePos(signalPos), "lookupImagePos at signalPos " + signalPos);
            assertEquals(signalPos, mapper.lookupSignalPos(imagePos), "lookupSignalPos at imagePos " + imagePos);
        }
    }

    private void testReturnedLutsAreCopies() {
        PixelAudioMapper mapper = mapper4x4();
        int[] signalToImage = mapper.getSignalToImageLUT();
        signalToImage[0] = 99;

        assertEquals(HILBERT_4_SIGNAL_TO_IMAGE[0], mapper.getSignalToImageLUT()[0], "signal LUT defensive copy");
    }

    private void testMapSignalToImageFollowsHilbertPath() {
        PixelAudioMapper mapper = mapper4x4();
        float[] signal = evenlySpacedAudio(mapper.getSize());
        int[] image = filledColorArray(mapper.getSize(), PixelAudioMapper.composeColor(0, 0, 0));

        mapper.mapSigToImg(signal, image, ChannelNames.ALL);

        for (int signalPos = 0; signalPos < mapper.getSize(); signalPos++) {
            int imagePos = HILBERT_4_SIGNAL_TO_IMAGE[signalPos];
            int expected = PixelAudioMapper.audioToRGBChan(signal[signalPos]);
            assertRgb(expected, expected, expected, image[imagePos], "mapped gray at signalPos " + signalPos);
        }
    }

    private void testMapImageToSignalFollowsHilbertPath() {
        PixelAudioMapper mapper = mapper4x4();
        int[] image = new int[mapper.getSize()];
        for (int imagePos = 0; imagePos < image.length; imagePos++) {
            image[imagePos] = PixelAudioMapper.composeColor(imagePos, 0, 0);
        }
        float[] signal = new float[mapper.getSize()];

        mapper.mapImgToSig(image, signal, ChannelNames.R);

        for (int signalPos = 0; signalPos < mapper.getSize(); signalPos++) {
            int imagePos = HILBERT_4_SIGNAL_TO_IMAGE[signalPos];
            float expected = PixelAudioMapper.rgbChanToAudio(imagePos);
            assertFloatEquals(expected, signal[signalPos], 0.000001f, "mapped red at signalPos " + signalPos);
        }
    }

    private void testShiftedLookupsAreConsistentInverses() {
        PixelAudioMapper mapper = mapper4x4();
        int shift = 5;

        for (int signalPos = 0; signalPos < mapper.getSize(); signalPos++) {
            int imagePos = mapper.lookupImagePosShifted(signalPos, shift);
            assertEquals(signalPos, mapper.lookupSignalPosShifted(imagePos, shift),
                    "shifted lookup inverse at signalPos " + signalPos);
        }
    }

    private void testArrayLengthMismatchThrows() {
        PixelAudioMapper mapper = mapper4x4();

        expectThrows(IllegalArgumentException.class,
                () -> mapper.mapSigToImg(new float[15], new int[16], ChannelNames.ALL),
                "mapSigToImg rejects different array lengths");
    }

    private PixelAudioMapper mapper4x4() {
        return new PixelAudioMapper(new HilbertGen(4, 4));
    }

    private float[] evenlySpacedAudio(int length) {
        float[] signal = new float[length];
        for (int i = 0; i < length; i++) {
            signal[i] = -1.0f + (2.0f * i / (length - 1));
        }
        return signal;
    }

    private int[] filledColorArray(int length, int color) {
        int[] colors = new int[length];
        Arrays.fill(colors, color);
        return colors;
    }

    private void assertRgb(int expectedR, int expectedG, int expectedB, int actual, String label) {
        int[] rgb = PixelAudioMapper.rgbComponents(actual);
        assertEquals(expectedR, rgb[0], label + " red");
        assertEquals(expectedG, rgb[1], label + " green");
        assertEquals(expectedB, rgb[2], label + " blue");
    }

    private void assertArrayEquals(int[] expected, int[] actual, String label) {
        testsRun++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(label + "\nexpected " + Arrays.toString(expected)
                    + "\nactual   " + Arrays.toString(actual));
        }
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

    private void expectThrows(Class<? extends Throwable> expected, Runnable runnable, String label) {
        testsRun++;
        try {
            runnable.run();
        }
        catch (Throwable throwable) {
            if (expected.isInstance(throwable)) {
                return;
            }
            throw new AssertionError(label + ": expected " + expected.getSimpleName()
                    + ", got " + throwable.getClass().getSimpleName(), throwable);
        }
        throw new AssertionError(label + ": expected " + expected.getSimpleName());
    }
}
