package net.paulhertz.pixelaudio;

import java.awt.Color;
import java.util.Arrays;

import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;

/**
 * Lightweight unit tests for PixelAudioMapper audio/color and color utility methods.
 *
 * Run from the project root with:
 * javac -cp "libs/*:src" -d test-bin test/net/paulhertz/pixelaudio/PixelAudioMapperColorTest.java
 * java -cp "libs/*:src:test-bin" net.paulhertz.pixelaudio.PixelAudioMapperColorTest
 */
public final class PixelAudioMapperColorTest {
    private int testsRun = 0;
    private int testsFailed = 0;

    public static void main(String[] args) {
        PixelAudioMapperColorTest suite = new PixelAudioMapperColorTest();
        suite.run();
    }

    private void run() {
        runTest("extractColorAsAudio reads RGB, alpha, HSB, and luminosity", this::testExtractColorAsAudio);
        runTest("applyAudioToColor writes selected channels", this::testApplyAudioToColor);
        runTest("applyChannelToColor copies selected channels", this::testApplyChannelToColor);
        runTest("pullPixelAsAudio extracts arrays with and without LUTs", this::testPullPixelAsAudio);
        runTest("pullPixelAsAudioShifted applies path rotation", this::testPullPixelAsAudioShifted);
        runTest("pullAudioAsColor creates grayscale slices", this::testPullAudioAsColor);
        runTest("pushAudioToPixel writes channels directly", this::testPushAudioToPixel);
        runTest("pushAudioToChannel writes channels through LUTs", this::testPushAudioToChannel);
        runTest("pushAudioToChannelShifted writes shifted channels through LUTs", this::testPushAudioToChannelShifted);
        runTest("pushChannelToPixel copies color channels", this::testPushChannelToPixel);
        runTest("component and alpha utilities preserve channel values", this::testComponentAndAlphaUtilities);
        runTest("HSB extraction matches java.awt.Color", this::testHsbExtraction);
        runTest("float channel application changes the requested channel", this::testFloatChannelApplication);
        runTest("RGB channel application changes the requested channel", this::testRgbChannelApplication);
        runTest("applyColor transfers hue and saturation while preserving brightness", this::testApplyColor);
        runTest("applyColor array helpers use LUTs and shifted LUTs", this::testApplyColorArrayHelpers);
        runTest("precomputeHueSat matches java.awt.Color", this::testPrecomputeHueSat);

        if (testsFailed > 0) {
            throw new AssertionError("PixelAudioMapperColorTest: " + testsFailed + " test groups failed, "
                    + testsRun + " assertions passed.");
        }
        System.out.println("PixelAudioMapperColorTest: " + testsRun + " assertions passed.");
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

    private void testExtractColorAsAudio() {
        int color = PixelAudioMapper.composeColor(32, 64, 128, 200);
        float[] hsb = new float[3];

        assertFloatEquals(PixelAudioMapper.rgbChanToAudio(32),
                PixelAudioMapper.extractColorAsAudio(color, ChannelNames.R, hsb), 0.000001f, "extract R");
        assertFloatEquals(PixelAudioMapper.rgbChanToAudio(64),
                PixelAudioMapper.extractColorAsAudio(color, ChannelNames.G, hsb), 0.000001f, "extract G");
        assertFloatEquals(PixelAudioMapper.rgbChanToAudio(128),
                PixelAudioMapper.extractColorAsAudio(color, ChannelNames.B, hsb), 0.000001f, "extract B");
        assertFloatEquals(PixelAudioMapper.rgbChanToAudio(200),
                PixelAudioMapper.extractColorAsAudio(color, ChannelNames.A, hsb), 0.000001f, "extract A");
        assertFloatEquals(PixelAudioMapper.hsbFloatToAudio(PixelAudioMapper.hue(color)),
                PixelAudioMapper.extractColorAsAudio(color, ChannelNames.H, hsb), 0.000001f, "extract H");
        assertFloatEquals(PixelAudioMapper.hsbFloatToAudio(PixelAudioMapper.saturation(color)),
                PixelAudioMapper.extractColorAsAudio(color, ChannelNames.S, hsb), 0.000001f, "extract S");
        assertFloatEquals(PixelAudioMapper.hsbFloatToAudio(PixelAudioMapper.brightness(color)),
                PixelAudioMapper.extractColorAsAudio(color, ChannelNames.L, hsb), 0.000001f, "extract L");

        float expectedAll = PixelAudioMapper.rgbFloatToAudio(0.3f * 32 + 0.59f * 64 + 0.11f * 128);
        assertFloatEquals(expectedAll,
                PixelAudioMapper.extractColorAsAudio(color, ChannelNames.ALL, hsb), 0.000001f, "extract ALL");
    }

    private void testApplyAudioToColor() {
        float[] hsb = new float[3];
        int target = PixelAudioMapper.composeColor(10, 20, 30, 40);

        assertRgb(128, 20, 30, PixelAudioMapper.applyAudioToColor(0.0f, target, ChannelNames.R, hsb), "apply R");
        assertRgb(10, 128, 30, PixelAudioMapper.applyAudioToColor(0.0f, target, ChannelNames.G, hsb), "apply G");
        assertRgb(10, 20, 128, PixelAudioMapper.applyAudioToColor(0.0f, target, ChannelNames.B, hsb), "apply B");
        assertRgba(10, 20, 30, 128, PixelAudioMapper.applyAudioToColor(0.0f, target, ChannelNames.A, hsb), "apply A");
        assertRgb(128, 128, 128, PixelAudioMapper.applyAudioToColor(0.0f, target, ChannelNames.ALL, hsb), "apply ALL");
    }

    private void testApplyChannelToColor() {
        float[] hsb = new float[3];
        int source = PixelAudioMapper.composeColor(101, 102, 103, 104);
        int target = PixelAudioMapper.composeColor(10, 20, 30, 40);

        assertRgb(101, 20, 30, PixelAudioMapper.applyChannelToColor(source, target, ChannelNames.R, hsb), "copy R");
        assertRgb(10, 102, 30, PixelAudioMapper.applyChannelToColor(source, target, ChannelNames.G, hsb), "copy G");
        assertRgb(10, 20, 103, PixelAudioMapper.applyChannelToColor(source, target, ChannelNames.B, hsb), "copy B");
        assertRgb(101, 102, 103, PixelAudioMapper.applyChannelToColor(source, target, ChannelNames.ALL, hsb), "copy ALL");
    }

    private void testPullPixelAsAudio() {
        int[] pixels = colorsFromRedValues(0, 64, 128, 255);
        float[] hsb = new float[3];

        float[] direct = PixelAudioMapper.pullPixelAsAudio(pixels, null, ChannelNames.R, hsb);
        assertFloatArrayEquals(new float[] {
                PixelAudioMapper.rgbChanToAudio(0),
                PixelAudioMapper.rgbChanToAudio(64),
                PixelAudioMapper.rgbChanToAudio(128),
                PixelAudioMapper.rgbChanToAudio(255)
        }, direct, 0.000001f, "pull direct");

        int[] lut = {2, 0, 3, 1};
        float[] mapped = PixelAudioMapper.pullPixelAsAudio(pixels, null, lut, ChannelNames.R, hsb);
        assertFloatArrayEquals(new float[] {
                PixelAudioMapper.rgbChanToAudio(128),
                PixelAudioMapper.rgbChanToAudio(0),
                PixelAudioMapper.rgbChanToAudio(255),
                PixelAudioMapper.rgbChanToAudio(64)
        }, mapped, 0.000001f, "pull with LUT");
    }

    private void testPullPixelAsAudioShifted() {
        int[] pixels = colorsFromRedValues(0, 64, 128, 255);
        int[] lut = {2, 0, 3, 1};
        float[] hsb = new float[3];

        float[] shifted = PixelAudioMapper.pullPixelAsAudioShifted(pixels, null, lut, ChannelNames.R, hsb, 1);

        assertFloatArrayEquals(new float[] {
                PixelAudioMapper.rgbChanToAudio(64),
                PixelAudioMapper.rgbChanToAudio(128),
                PixelAudioMapper.rgbChanToAudio(0),
                PixelAudioMapper.rgbChanToAudio(255)
        }, shifted, 0.000001f, "pull shifted");
    }

    private void testPullAudioAsColor() {
        float[] samples = {-1.0f, -0.5f, 0.0f, 0.5f, 1.0f};

        int[] colors = PixelAudioMapper.pullAudioAsColor(samples, null, 1, 3);

        assertGrayValues(new int[] {64, 128, 191}, colors, "pullAudioAsColor slice");
    }

    private void testPushAudioToPixel() {
        float[] samples = {-1.0f, 0.0f, 1.0f};
        int[] pixels = filledColorArray(3, PixelAudioMapper.composeColor(10, 20, 30));

        PixelAudioMapper.pushAudioToPixel(samples, pixels, ChannelNames.G);

        assertRgb(10, 0, 30, pixels[0], "push direct 0");
        assertRgb(10, 128, 30, pixels[1], "push direct 1");
        assertRgb(10, 255, 30, pixels[2], "push direct 2");
    }

    private void testPushAudioToChannel() {
        float[] samples = {-1.0f, 0.0f, 1.0f};
        int[] pixels = filledColorArray(3, PixelAudioMapper.composeColor(10, 20, 30));
        int[] lut = {2, 0, 1};

        PixelAudioMapper.pushAudioToChannel(samples, pixels, lut, ChannelNames.B);

        assertRgb(10, 20, 128, pixels[0], "push LUT pixel 0");
        assertRgb(10, 20, 255, pixels[1], "push LUT pixel 1");
        assertRgb(10, 20, 0, pixels[2], "push LUT pixel 2");
    }

    private void testPushAudioToChannelShifted() {
        float[] samples = {-1.0f, 0.0f, 1.0f};
        int[] pixels = filledColorArray(3, PixelAudioMapper.composeColor(10, 20, 30));
        int[] lut = {2, 0, 1};

        PixelAudioMapper.pushAudioToChannelShifted(samples, pixels, lut, ChannelNames.B, 1);

        assertRgb(10, 20, 255, pixels[0], "push shifted pixel 0");
        assertRgb(10, 20, 0, pixels[1], "push shifted pixel 1");
        assertRgb(10, 20, 128, pixels[2], "push shifted pixel 2");
    }

    private void testPushChannelToPixel() {
        int[] colors = {
                PixelAudioMapper.composeColor(101, 0, 0),
                PixelAudioMapper.composeColor(102, 0, 0),
                PixelAudioMapper.composeColor(103, 0, 0)
        };
        int[] pixels = filledColorArray(3, PixelAudioMapper.composeColor(10, 20, 30));

        PixelAudioMapper.pushChannelToPixel(colors, pixels, ChannelNames.R);
        assertRedValues(new int[] {101, 102, 103}, pixels, "pushChannel direct");

        int[] mappedPixels = filledColorArray(3, PixelAudioMapper.composeColor(10, 20, 30));
        PixelAudioMapper.pushChannelToPixel(colors, mappedPixels, new int[] {2, 0, 1}, ChannelNames.R);
        assertRedValues(new int[] {102, 103, 101}, mappedPixels, "pushChannel LUT");
    }

    private void testComponentAndAlphaUtilities() {
        int color = PixelAudioMapper.composeColor(10, 20, 30, 40);

        assertIntArrayEquals(new int[] {10, 20, 30}, PixelAudioMapper.rgbComponents(color), "rgbComponents");
        assertIntArrayEquals(new int[] {10, 20, 30, 40}, PixelAudioMapper.rgbaComponents(color), "rgbaComponents");
        assertIntArrayEquals(new int[] {40, 10, 20, 30}, PixelAudioMapper.argbComponents(color), "argbComponents");
        assertEquals(40, PixelAudioMapper.alphaComponent(color), "alphaComponent");
        assertRgba(10, 20, 30, 99, PixelAudioMapper.setAlpha(color, 99), "setAlpha scalar");

        int[] colors = {PixelAudioMapper.composeColor(1, 2, 3), PixelAudioMapper.composeColor(4, 5, 6)};
        PixelAudioMapper.setAlpha(colors, 77);
        assertRgba(1, 2, 3, 77, colors[0], "setAlpha array 0");
        assertRgba(4, 5, 6, 77, colors[1], "setAlpha array 1");

        int alphaSource = PixelAudioMapper.composeColor(200, 201, 202, 66);
        int rgbTarget = PixelAudioMapper.composeColor(10, 20, 30, 255);
        assertRgba(10, 20, 30, 66, PixelAudioMapper.applyAlpha(alphaSource, rgbTarget), "applyAlpha color");
        assertEquals("color(10, 20, 30, 40)", PixelAudioMapper.colorString(color), "colorString");
        assertEquals(Math.round(0.3f * 10 + 0.59f * 20 + 0.11f * 30), PixelAudioMapper.getLuminosity(color),
                "getLuminosity");
    }

    private void testHsbExtraction() {
        int color = PixelAudioMapper.composeColor(64, 128, 32);
        float[] expected = new float[3];
        Color.RGBtoHSB(64, 128, 32, expected);
        float[] hsb = new float[3];

        assertFloatEquals(expected[0], PixelAudioMapper.hue(color, hsb), 0.000001f, "hue reusable");
        assertFloatEquals(expected[0], PixelAudioMapper.hue(color), 0.000001f, "hue");
        assertFloatEquals(expected[1], PixelAudioMapper.saturation(color, hsb), 0.000001f, "saturation reusable");
        assertFloatEquals(expected[1], PixelAudioMapper.saturation(color), 0.000001f, "saturation");
        assertFloatEquals(expected[2], PixelAudioMapper.brightness(color, hsb), 0.000001f, "brightness reusable");
        assertFloatEquals(expected[2], PixelAudioMapper.brightness(color), 0.000001f, "brightness");
    }

    private void testFloatChannelApplication() {
        int target = PixelAudioMapper.composeColor(10, 20, 30);

        assertRgb(128, 20, 30, PixelAudioMapper.applyRed(0.0f, target), "applyRed float");
        assertRgb(10, 128, 30, PixelAudioMapper.applyGreen(0.0f, target), "applyGreen float");
        assertRgb(10, 20, 128, PixelAudioMapper.applyBlue(0.0f, target), "applyBlue float");
        assertRgba(10, 20, 30, 128, PixelAudioMapper.applyAlpha(0.0f, target), "applyAlpha float");
        assertRgb(128, 128, 128, PixelAudioMapper.applyAll(0.0f, target), "applyAll float");

        int bright = PixelAudioMapper.applyBrightness(0.0f, target);
        assertFloatEquals(0.5f, PixelAudioMapper.brightness(bright), 0.01f, "applyBrightness float");

        int saturated = PixelAudioMapper.applySaturation(0.0f, target);
        assertFloatEquals(0.5f, PixelAudioMapper.saturation(saturated), 0.01f, "applySaturation float");
    }

    private void testRgbChannelApplication() {
        int source = PixelAudioMapper.composeColor(101, 102, 103);
        int target = PixelAudioMapper.composeColor(10, 20, 30);

        assertRgb(101, 20, 30, PixelAudioMapper.applyRed(source, target), "applyRed RGB");
        assertRgb(10, 102, 30, PixelAudioMapper.applyGreen(source, target), "applyGreen RGB");
        assertRgb(10, 20, 103, PixelAudioMapper.applyBlue(source, target), "applyBlue RGB");
        assertRgb(101, 102, 103, PixelAudioMapper.applyAll(source, target), "applyAll RGB");

        int saturationSource = PixelAudioMapper.composeColor(255, 0, 0);
        int saturated = PixelAudioMapper.applySaturation(saturationSource, target);
        assertFloatEquals(PixelAudioMapper.saturation(saturationSource), PixelAudioMapper.saturation(saturated), 0.01f,
                "applySaturation RGB");
    }

    private void testApplyColor() {
        int colorSource = PixelAudioMapper.composeColor(255, 0, 0);
        int graySource = PixelAudioMapper.composeColor(96, 96, 96);
        int applied = PixelAudioMapper.applyColor(colorSource, graySource);

        assertFloatEquals(PixelAudioMapper.hue(colorSource), PixelAudioMapper.hue(applied), 0.01f,
                "applyColor hue");
        assertFloatEquals(PixelAudioMapper.saturation(colorSource), PixelAudioMapper.saturation(applied), 0.01f,
                "applyColor saturation");
        assertFloatEquals(PixelAudioMapper.brightness(graySource), PixelAudioMapper.brightness(applied), 0.01f,
                "applyColor brightness");
    }

    private void testApplyColorArrayHelpers() {
        int[] colorSource = {
                PixelAudioMapper.composeColor(255, 0, 0),
                PixelAudioMapper.composeColor(0, 255, 0),
                PixelAudioMapper.composeColor(0, 0, 255)
        };
        int[] graySource = {
                PixelAudioMapper.composeColor(64, 64, 64),
                PixelAudioMapper.composeColor(128, 128, 128),
                PixelAudioMapper.composeColor(192, 192, 192)
        };
        int[] lut = {2, 0, 1};

        int[] applied = PixelAudioMapper.applyColor(colorSource, Arrays.copyOf(graySource, graySource.length), lut);
        assertColorTransfer(colorSource[2], graySource[0], applied[0], "applyColor array 0");
        assertColorTransfer(colorSource[0], graySource[1], applied[1], "applyColor array 1");
        assertColorTransfer(colorSource[1], graySource[2], applied[2], "applyColor array 2");

        int[] out = new int[3];
        int[] returned = PixelAudioMapper.applyColorInto(colorSource, graySource, lut, out);
        assertSame(out, returned, "applyColorInto returns out");
        assertColorTransfer(colorSource[2], graySource[0], out[0], "applyColorInto 0");

        int[] shiftedOut = new int[3];
        PixelAudioMapper.applyColorShiftedInto(colorSource, graySource, lut, 1, shiftedOut);
        assertColorTransfer(colorSource[0], graySource[0], shiftedOut[0], "applyColorShiftedInto 0");
        assertColorTransfer(colorSource[1], graySource[1], shiftedOut[1], "applyColorShiftedInto 1");
        assertColorTransfer(colorSource[2], graySource[2], shiftedOut[2], "applyColorShiftedInto 2");
    }

    private void testPrecomputeHueSat() {
        int[] colors = {
                PixelAudioMapper.composeColor(255, 0, 0),
                PixelAudioMapper.composeColor(0, 255, 0),
                PixelAudioMapper.composeColor(0, 0, 255)
        };
        float[] hue = new float[colors.length];
        float[] sat = new float[colors.length];

        PixelAudioMapper.precomputeHueSat(colors, hue, sat);

        for (int i = 0; i < colors.length; i++) {
            assertFloatEquals(PixelAudioMapper.hue(colors[i]), hue[i], 0.000001f, "precompute hue " + i);
            assertFloatEquals(PixelAudioMapper.saturation(colors[i]), sat[i], 0.000001f, "precompute sat " + i);
        }
    }

    private int[] filledColorArray(int length, int color) {
        int[] colors = new int[length];
        Arrays.fill(colors, color);
        return colors;
    }

    private int[] colorsFromRedValues(int... redValues) {
        int[] colors = new int[redValues.length];
        for (int i = 0; i < redValues.length; i++) {
            colors[i] = PixelAudioMapper.composeColor(redValues[i], 0, 0);
        }
        return colors;
    }

    private void assertColorTransfer(int expectedHueSatSource, int expectedBrightnessSource, int actual,
            String label) {
        assertFloatEquals(PixelAudioMapper.hue(expectedHueSatSource), PixelAudioMapper.hue(actual), 0.01f,
                label + " hue");
        assertFloatEquals(PixelAudioMapper.saturation(expectedHueSatSource), PixelAudioMapper.saturation(actual),
                0.01f, label + " saturation");
        assertFloatEquals(PixelAudioMapper.brightness(expectedBrightnessSource), PixelAudioMapper.brightness(actual),
                0.01f, label + " brightness");
    }

    private void assertGrayValues(int[] expectedGrayValues, int[] colors, String label) {
        for (int i = 0; i < expectedGrayValues.length; i++) {
            assertRgb(expectedGrayValues[i], expectedGrayValues[i], expectedGrayValues[i], colors[i],
                    label + " gray at " + i);
        }
    }

    private void assertRedValues(int[] expectedRedValues, int[] colors, String label) {
        for (int i = 0; i < expectedRedValues.length; i++) {
            assertEquals(expectedRedValues[i], PixelAudioMapper.rgbComponents(colors[i])[0], label + " red at " + i);
        }
    }

    private void assertRgb(int expectedR, int expectedG, int expectedB, int actual, String label) {
        int[] rgb = PixelAudioMapper.rgbComponents(actual);
        assertEquals(expectedR, rgb[0], label + " red");
        assertEquals(expectedG, rgb[1], label + " green");
        assertEquals(expectedB, rgb[2], label + " blue");
    }

    private void assertRgba(int expectedR, int expectedG, int expectedB, int expectedA, int actual, String label) {
        int[] rgba = PixelAudioMapper.rgbaComponents(actual);
        assertEquals(expectedR, rgba[0], label + " red");
        assertEquals(expectedG, rgba[1], label + " green");
        assertEquals(expectedB, rgba[2], label + " blue");
        assertEquals(expectedA, rgba[3], label + " alpha");
    }

    private void assertIntArrayEquals(int[] expected, int[] actual, String label) {
        testsRun++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(label + "\nexpected " + Arrays.toString(expected)
                    + "\nactual   " + Arrays.toString(actual));
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

    private void assertSame(Object expected, Object actual, String label) {
        testsRun++;
        if (expected != actual) {
            throw new AssertionError(label + ": expected same object");
        }
    }

    private void assertEquals(String expected, String actual, String label) {
        testsRun++;
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
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
}
