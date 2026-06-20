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
 * Thread-safe cache for generated Minim window curves. PixelAudio uses 
 * a Hann window by default. Minim provides additional WindowFunction classes 
 * and a model for creating your own. 
 *
 * <p>{@code WindowCache} stores the arrays returned by
 * {@link WindowFunction#generateCurve(int)} so repeated granular voices can reuse the same
 * window data instead of regenerating it for every grain or playback event.</p>
 *
 * <p>Cache entries are keyed by the concrete {@link WindowFunction} class and the requested
 * length. Two different instances of the same window-function class therefore share a cached
 * curve when the requested length is the same.</p>
 *
 * <p>The cache is safe to access from multiple threads. For strict real-time audio behavior,
 * call {@link #prewarm(WindowFunction, int)} during setup or scheduling so the audio thread
 * can later call {@link #getWindowCurve(WindowFunction, int)} without triggering curve
 * generation.</p>
 *
 * <p>The returned arrays are cached and shared. Callers should treat them as read-only.</p>
 *
 * @see PABurstGranularSource
 * @see PAGranularInstrumentDirector
 * @see <a href="https://code.compartmental.net/minim/javadoc/" target="_blank">Minim documentation</a>
 * 
 */
public final class WindowCache {

    /** Shared cache instance used by the granular synthesis engine. */
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
     * Precomputes and caches a window curve.
     *
     * <p>Use this method during setup or scheduling to avoid first-use curve generation on the
     * audio thread. If the curve is already cached, this method returns the existing shared
     * array.</p>
     *
     * @param wf window function used to generate the curve
     * @param length number of samples in the generated curve
     * @return the cached window curve; callers should treat the returned array as read-only
     * @throws NullPointerException if {@code wf} is null
     */
    public float[] prewarm(WindowFunction wf, int length) {
        Key key = new Key(wf, length);
        return windowCurves.computeIfAbsent(key, k -> wf.generateCurve(length));
    }

    /**
     * Returns a cached window curve for the given window function and length.
     *
     * <p>If no matching curve has been cached, this method generates one lazily and stores it.
     * For strict real-time safety, prefer calling {@link #prewarm(WindowFunction, int)} ahead
     * of time.</p>
     *
     * @param wf window function used to generate the curve
     * @param length number of samples in the generated curve
     * @return the cached window curve; callers should treat the returned array as read-only
     * @throws NullPointerException if {@code wf} is null
     */
    public float[] getWindowCurve(WindowFunction wf, int length) {
        Key key = new Key(wf, length);
        return windowCurves.computeIfAbsent(key, k -> wf.generateCurve(length));
    }
}
