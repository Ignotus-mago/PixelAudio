package net.paulhertz.pixelaudio.curves;

import java.util.ArrayList;

import processing.core.PVector;

/**
 * Stores optional affine-transform state for a gesture/curve.
 *
 * Transform order:
 *   1) flip/scale about pivot
 *   2) rotate about pivot
 *   3) translate
 */
public class GestureTransformState {

    public boolean enabled = false;

    /** Translation in display units (pixels). */
    public float translateX = 0f;
    public float translateY = 0f;

    /** Rotation in radians. */
    public float rotation = 0f;

    /** Scale factors before rotation. */
    public float scaleX = 1f;
    public float scaleY = 1f;

    /** Mirror left/right around pivot (negates x scale). */
    public boolean flipHorizontal = false;

    /** Mirror up/down around pivot (negates y scale). */
    public boolean flipVertical = false;

    /**
     * Optional pivot for scale/flip/rotation.
     * If null, utilities may use the bounds center of restPoints.
     */
    public PVector pivot = null;

    /**
     * Optional rest pose to avoid cumulative drift during animation.
     */
    public ArrayList<PVector> restPoints = null;

    public GestureTransformState() {
    }

    public GestureTransformState copy() {
        GestureTransformState out = new GestureTransformState();
        out.enabled = this.enabled;
        out.translateX = this.translateX;
        out.translateY = this.translateY;
        out.rotation = this.rotation;
        out.scaleX = this.scaleX;
        out.scaleY = this.scaleY;
        out.flipHorizontal = this.flipHorizontal;
        out.flipVertical = this.flipVertical;
        out.pivot = (this.pivot == null) ? null : this.pivot.copy();
        out.restPoints = copyPoints(this.restPoints);
        return out;
    }

    public void resetTransform() {
        translateX = 0f;
        translateY = 0f;
        rotation = 0f;
        scaleX = 1f;
        scaleY = 1f;
        flipHorizontal = false;
        flipVertical = false;
    }

    public void captureRestPoints(ArrayList<PVector> points) {
        this.restPoints = copyPoints(points);
        if (this.pivot == null && this.restPoints != null && !this.restPoints.isEmpty()) {
            this.pivot = PACurveUtility.getBoundsCenter(this.restPoints);
        }
    }

    public boolean hasRestPoints() {
        return restPoints != null && !restPoints.isEmpty();
    }

    public float effectiveScaleX() {
        return flipHorizontal ? -scaleX : scaleX;
    }

    public float effectiveScaleY() {
        return flipVertical ? -scaleY : scaleY;
    }

    public static ArrayList<PVector> copyPoints(ArrayList<PVector> points) {
        if (points == null) return null;
        ArrayList<PVector> out = new ArrayList<>(points.size());
        for (PVector p : points) {
            out.add((p == null) ? null : p.copy());
        }
        return out;
    }
}