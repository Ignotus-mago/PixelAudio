/*
 *  Copyright (c) 2024 - 2025 by Paul Hertz <ignotus@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.paulhertz.pixelaudio;

/**
 * Experimental helper class for PixelAudioMapper and other classes or methods that transcode audio and color values.
 * This is thread-safe but it does have to have all fields initialized in the constructor. 
 * It has not yet been used in any code and may be removed at a future date. 
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

    public int transcodeFloat(float val) {
        val = clamp(val, minAudio, maxAudio);
        float norm = (val - minAudio) / (maxAudio - minAudio); // normalize to [0,1]
        return Math.round(norm * (maxColor - minColor) + minColor);
    }

    public float transcodeInt(int val) {
        val = clamp(val, minColor, maxColor);
        float norm = (val - minColor) / (float)(maxColor - minColor); // normalize to [0,1]
        return norm * (maxAudio - minAudio) + minAudio;
    }

    public float transcodeIntF(float val) {
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

