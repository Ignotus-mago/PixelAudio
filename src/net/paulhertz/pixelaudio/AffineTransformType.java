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

// TODO consider alternate names for transforms

/**
 * <p>
 * The AffineTransformType enum defines the types of affine transformations that
 * can be applied to bitmaps in the PixelAudio library using only rotation and
 * reflection. PixelMapGen uses the AffineTransformType enum to specify the type
 * of transformation to apply to the gen coordinates and LUTs. PixelMapGen's
 * comments strongly suggest starting gen coordinates at (0,0) in the upper left
 * corner, but this may vary with the signal path you want to create. The Moore
 * curve, for example, begins at the top center of the bitmap. The 
 * {@link BitmapTransform.java BitmapTransform class} applies the transformations 
 * defined in this enum to bitmaps using lookup tables. 
 * </p><p>
 * Naming follows computer graphics conventions where 0 degrees points right and
 * positive rotation is counterclockwise.</p>
 * 
 * 	<pre>
 *   NADA         no operation
 *   R270         rotate 90 degrees clockwise
 *   ROT180       rotate 180 degrees
 *   R90          rotate 90 degrees counterclockwise
 *   FLIPX        reflect on y-axis, y coordinates do not change
 *   FX270        reflect on y-axis, then rotate 90 clockwise = reflect on the secondary diagonal, upper left to lower right, secondary diagonal does not change
 *   FLIPY        reflect on x-axis, x coordinates do not change
 *   FX90         reflect on y-axis, then rotate 90 counterclockwise = reflect on the primary diagonal, upper right to lower left, primary diagonal does not change
 *  </pre>
 *   
 *   @see BitmapTransform
 */
public enum AffineTransformType {NADA, R270, R180, R90, FLIPX, FX270, FLIPY, FX90}
