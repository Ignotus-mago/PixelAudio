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

package net.paulhertz.pixelaudio.granular;

import ddf.minim.analysis.WindowFunction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WindowCache
 *
 * Simple cache for windowFunction.generateCurve(length) results.
 * Avoids recomputing window curves for the same (WindowFunction class, length) pairs.
 *
 * Implementation is thread-safe. For strict real-time safety, prewarm curves
 * during setup, not from the audio thread.
 * 
 * Used by the PixelAudio granular synthesis engine.
 * Calling chain: PAGranularInstrumentDirector -> PAGranularInstrument -> PAGranularSampler -> PAGranularVoice,  
 * where PABurstGranularSource handles the complexities of the granular synthesis sample by sample. 
 * 
 */
public final class WindowCache {

    // Singleton instance
    public static final WindowCache INSTANCE = new WindowCache();

    private WindowCache() {
        // private: use INSTANCE
    }

    private static final class Key {
        final Class<? extends WindowFunction> wfClass;
        final int length;

        Key(WindowFunction wf, int length) {
            this.wfClass = wf.getClass();
            this.length = length;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) return false;
            Key other = (Key) o;
            return wfClass == other.wfClass && length == other.length;
        }

        @Override
        public int hashCode() {
            return wfClass.hashCode() * 31 + length;
        }
    }

    private final Map<Key, float[]> windowCurves = new ConcurrentHashMap<>();

    /**
     * Precompute and cache a window curve. Call this during setup, not from the audio thread.
     */
    public float[] prewarm(WindowFunction wf, int length) {
        Key key = new Key(wf, length);
        return windowCurves.computeIfAbsent(key, k -> wf.generateCurve(length));
    }

    /**
     * Get a cached window curve for the given WindowFunction and length.
     * If not cached, this will lazily generate it.
     *
     * For strict real-time safety, prefer calling prewarm(...) ahead of time.
     */
    public float[] getWindowCurve(WindowFunction wf, int length) {
        Key key = new Key(wf, length);
        return windowCurves.computeIfAbsent(key, k -> wf.generateCurve(length));
    }
}
