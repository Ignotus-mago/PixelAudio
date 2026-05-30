package net.paulhertz.pixelaudio.curves;

/**
 * Minimal interface for interpolated curves.
 */
public interface PAControlCurve {
    float sample(float u);   // canonical form
}