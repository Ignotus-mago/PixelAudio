package net.paulhertz.pixelaudio;

/**
 * Helper class for PixelAudioMapper and other classes or methods that transcode audio and color values.
 * This is thread-safe but it does have to have all fields initialized in the constructor. 
 * 
 * // Standard 8-bit audio → color mapping
 * AudioColorTranscoder defaultTranscoder = new AudioColorTranscoder(-1.0f, 1.0f, 0, 255);
 * 
 * // High-resolution 16-bit color and 5V audio
 * AudioColorTranscoder highResTranscoder = new AudioColorTranscoder(-2.5f, 2.5f, 0, 65535);
 * 
 * 
 */
public class AudioColorTranscoder {
    private final float minAudio;
    private final float maxAudio;
    private final int minColor;
    private final int maxColor;

    // Constructor must set all fields
    public AudioColorTranscoder(float minAudio, float maxAudio, int minColor, int maxColor) {
        this.minAudio = minAudio;
        this.maxAudio = maxAudio;
        this.minColor = minColor;
        this.maxColor = maxColor;
    }

    public int transcode(float val) {
        val = clamp(val, minAudio, maxAudio);
        float norm = (val - minAudio) / (maxAudio - minAudio); // normalize to [0,1]
        return Math.round(norm * (maxColor - minColor) + minColor);
    }

    public float inverseTranscode(int val) {
        val = clamp(val, minColor, maxColor);
        float norm = (val - minColor) / (float)(maxColor - minColor); // normalize to [0,1]
        return norm * (maxAudio - minAudio) + minAudio;
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}

