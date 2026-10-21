package net.paulhertz.pixelaudio.io;

import net.paulhertz.pixelaudio.granular.GestureGranularConfig;
import processing.data.JSONObject;

/**
 * Smoke tests for brush configuration JSON compatibility.
 *
 * <p>Run with:</p>
 * <pre>
 * javac -cp "libs/*:src" -d test-bin test/net/paulhertz/pixelaudio/io/GestureGranularConfigIOTest.java
 * java -cp "libs/*:src:test-bin" net.paulhertz.pixelaudio.io.GestureGranularConfigIOTest
 * </pre>
 */
public final class GestureGranularConfigIOTest {
    private static int assertions = 0;

    public static void main(String[] args) {
        roundTripPreservesEnvelopeDuration();
        legacyVersionOneUsesEnvelopeDurationDefault();
        System.out.println("GestureGranularConfigIOTest: " + assertions + " assertions passed.");
    }

    private static void roundTripPreservesEnvelopeDuration() {
        GestureGranularConfig.Builder source = new GestureGranularConfig.Builder();
        source.envDurationMs = 2375;

        JSONObject json = GestureGranularConfigIO.toJson(source, null);
        GestureGranularConfig.Builder restored = GestureGranularConfigIO.fromJson(json).builder;

        assertEquals(GestureGranularConfigIO.VERSION,
                json.getJSONObject("header").getInt("version"), "format version");
        assertEquals(2375, json.getJSONObject("config").getInt("envDurationMs"),
                "serialized envelope duration");
        assertEquals(2375, restored.envDurationMs, "round-trip envelope duration");
    }

    private static void legacyVersionOneUsesEnvelopeDurationDefault() {
        GestureGranularConfig.Builder restored = readMinimalVersionOneConfig().builder;

        assertEquals(new GestureGranularConfig.Builder().envDurationMs, restored.envDurationMs,
                "legacy envelope duration default");
    }

    private static GestureGranularConfigIO.Result readMinimalVersionOneConfig() {
        String json = "{\"header\":{\"format\":\"" + GestureGranularConfigIO.FORMAT
                + "\",\"version\":1},\"config\":{}}";
        return GestureGranularConfigIO.fromJson(JSONObject.parse(json));
    }

    private static void assertEquals(int expected, int actual, String message) {
        assertions++;
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }

}
