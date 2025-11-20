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

    private final Map<Key, float[]> curves = new ConcurrentHashMap<>();

    /**
     * Precompute and cache a window curve. Call this during setup, not from the audio thread.
     */
    public float[] prewarm(WindowFunction wf, int length) {
        Key key = new Key(wf, length);
        return curves.computeIfAbsent(key, k -> wf.generateCurve(length));
    }

    /**
     * Get a cached window curve for the given WindowFunction and length.
     * If not cached, this will lazily generate it.
     *
     * For strict real-time safety, prefer calling prewarm(...) ahead of time.
     */
    public float[] getWindowCurve(WindowFunction wf, int length) {
        Key key = new Key(wf, length);
        return curves.computeIfAbsent(key, k -> wf.generateCurve(length));
    }
}
