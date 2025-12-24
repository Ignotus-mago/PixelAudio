package net.paulhertz.pixelaudio.schedule;

import java.util.List;

import processing.core.PVector;
 

public final class GestureSchedule {
    public final List<PVector> points; // gesture positions
    public final float[] timesMs;      // gesture times in ms, same length as points

    public GestureSchedule(List<PVector> points, float[] timesMs) {
        if (points == null || timesMs == null) {
            throw new IllegalArgumentException("points and times must be non-null");
        }
        if (points.size() != timesMs.length) {
            throw new IllegalArgumentException("points.size() != timesMs.length");
        }
        this.points = points;
        this.timesMs = timesMs;
    }

    public int size() {
        return points.size();
    }

    public float durationMs() {
        return timesMs.length == 0 ? 0 : timesMs[timesMs.length - 1] - timesMs[0];
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }
    
    public static float[] normalizeTimesToStartAtZero(float[] timesMs) {
        float[] out = timesMs.clone();
        if (out.length == 0) return out;
        float t0 = out[0];
        for (int i = 0; i < out.length; i++) out[i] -= t0;
        return out;
    }

    public static void enforceNonDecreasing(float[] t) {
        for (int i = 1; i < t.length; i++) {
            if (t[i] < t[i-1]) t[i] = t[i-1];
        }
    }
    
}
