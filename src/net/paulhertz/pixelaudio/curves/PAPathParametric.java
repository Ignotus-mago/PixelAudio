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

package net.paulhertz.pixelaudio.curves;

import java.util.List;
import processing.core.PVector;

// TODO evaluate for deletion (legacy class, safe to delete)

/**
 * PAPathParametric
 *
 * Parametric view of a polyline path (e.g., polygonized Bezier).
 *
 * u ∈ [0,1] → (x(u), y(u))
 *
 * For now, u is interpreted over the *index* of the point list:
 *   - u = 0   → first point
 *   - u = 1   → last point
 *   - intermediate u → linear interpolation between neighboring points
 *
 * This preserves any "bunching" of points in high-curvature regions
 * produced by the polygonization.
 */
@Deprecated
public final class PAPathParametric {

    private final List<PVector> points;
    private final int count;

    /**
     * @param points ordered points along the path (e.g., from getEventPoints)
     *               must contain at least 2 points.
     */
    public PAPathParametric(List<PVector> points) {
        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException("PAPathParametric requires at least 2 points.");
        }
        this.points = points;
        this.count = points.size();
    }

    public int getPointCount() {
        return count;
    }

    /**
     * Sample the path at parameter u ∈ [0,1], returning an interpolated point.
     * u is clamped to [0,1].
     */
    public PVector sample(float u) {
        if (u < 0f) u = 0f;
        if (u > 1f) u = 1f;

        if (count == 2) {
            // Simple case: just interpolate between endpoints
            PVector p0 = points.get(0);
            PVector p1 = points.get(1);
            return lerp(p0, p1, u);
        }

        float pos = u * (count - 1);     // 0 .. count-1
        int i = (int) Math.floor(pos);   // base index
        float frac = pos - i;

        if (i >= count - 1) {
            // At or beyond last index; clamp to last point
            return points.get(count - 1).copy();
        }

        PVector p0 = points.get(i);
        PVector p1 = points.get(i + 1);
        return lerp(p0, p1, frac);
    }

    private static PVector lerp(PVector a, PVector b, float u) {
        float x = a.x + (b.x - a.x) * u;
        float y = a.y + (b.y - a.y) * u;
        return new PVector(x, y);
    }
}
