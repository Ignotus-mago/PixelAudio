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

import java.util.Arrays;
// import java.util.ArrayList;
import processing.core.PImage;
import processing.core.PConstants;

/**
 * <p>Provides static methods for rotating and reflecting 2D integer arrays using
 * index remapping. For methods that accept an int[] array and width and height parameters, 
 * the arrays may be bitmap pixel arrays, but not  necessarily--they could represent 
 * index values over a large bitmap or integer data of any sort, such as the LUTs generated 
 * by PixelMapGen subclasses. Methods that take a PImage as an argument will use the 
 * <code>PImage.pixels</code> array, and resize the image when necessary. Map methods
 * that accept width and height parameters return arrays of transformed pixel indices.</p>
 * 
 * @see AffineTransformType
 * 
 */
public class BitmapTransform {

	/**
	 * @param img	a PImage to transform
	 * @param type	a geometric transform (rotation, reflection) to perform on img
	 * @return		transformed image
	 */
	public static PImage imageTransform(PImage img, AffineTransformType type) {
		switch (type) {
		case R270: {
			img = rotate270(img);
			break;
		}
		case R90: {
			img = rotate90(img);
			break;
		}
		case R180: {
			img = rotate180(img);
			break;
		}
		case FLIPX: {
			img = flipX(img);
			break;
		}
		case FLIPY: {
			img = flipY(img);
			break;
		}
		case FX270: {
			img = flipX270(img);		// secondary diagonal
			break;
		}
		case FX90: {
			img = flipX90(img);  // primary diagonal
			break;
		}
		case NADA: {
			break;
		}
		default: {

		}
		}
		return img;
	}

	
	/**
	 * Performs a geometric transform (rotation, reflection) of an array of pixels.
	 * 
	 * @param pixels	an array of integers, possibly RGB or ARGB colors or array indices
	 * @param width		width of bitmap for pixel
	 * @param height	height of bitmap for pixel
	 * @param type		type of geometric transform for the bitmap
	 * @return			transformed coordinate pair, as determined by width, height, and transform type
	 */
	public static int[] pixelsTransform(int[] pixels, int width, int height, AffineTransformType type) {
		switch (type) {
		case R270: {
			return BitmapTransform.rotate270(pixels, width, height);
		}
		case R90: {
			return BitmapTransform.rotate90(pixels, width, height);
		}
		case R180: {
			return BitmapTransform.rotate180(pixels, width, height);
		}
		case FLIPX: {
			return BitmapTransform.flipX(pixels, width, height);
		}
		case FLIPY: {
			return BitmapTransform.flipY(pixels, width, height);
		}
		case FX270: {
			return BitmapTransform.flipX270(pixels, width, height);		// secondary diagonal
		}
		case FX90: {
			return BitmapTransform.flipX90(pixels, width, height);  // primary diagonal
		}
		case NADA: {
			return Arrays.copyOf(pixels, pixels.length);
		}
		default: {
			return Arrays.copyOf(pixels, pixels.length);
		}
		}
	}
	
	
	/**
	 * Performs a geometric transform (rotation, reflection) of a single pixel location.
	 * 
	 * @param x			x-coordinate of a pixel
	 * @param y			y-coordinate of a pixel
	 * @param width		width of bitmap for pixel
	 * @param height	height of bitmap for pixel
	 * @param type		type of geometric transform for the bitmap
	 * @return			transformed coordinate pair, as determined by width, height, and transform type
	 */
	public static int[] coordTransform(int x, int y, int width, int height, AffineTransformType type) {
		switch (type) {
		case R270: {
			return BitmapTransform.rotate270Coord(x, y, width, height);
		}
		case R90: {
			return BitmapTransform.rotate90Coord(x, y, width, height);
		}
		case R180: {
			return BitmapTransform.rotate180Coord(x, y, width, height);
		}
		case FLIPX: {
			return BitmapTransform.flipXCoord(x, y, width, height);
		}
		case FLIPY: {
			return BitmapTransform.flipYCoord(x, y, width, height);
		}
		case FX270: {
			return BitmapTransform.flipX270Coord(x, y, width, height);		// secondary diagonal
		}
		case FX90: {
			return BitmapTransform.flipX90Coord(x, y, width, height);       // primary diagonal
		}
		case NADA: {
			return BitmapTransform.nadaCoord(x, y, width, height);
		}
		default: {
			return BitmapTransform.nadaCoord(x, y, width, height);
		}
		}
	}

	
	/**
	 * @param w			bitmap width
	 * @param h			bitmap height
	 * @param type		type of affine transform 
	 * @return			array of bitmap indices reordered in accordance with the desired transform
	 */
	public static int[] getIndexMap(int w, int h, AffineTransformType type) {
		switch (type) {
		case R270: {
			return rotate270Map(w, h);
		}
		case R90: {
			return rotate90Map(w, h);
		}
		case R180: {
			return rotate180Map(w, h);
		}
		case FLIPX: {
			return flipXMap(w, h);
		}
		case FLIPY: {
			return flipYMap(w, h);
		}
		case FX90: {
			return flipX90Map(w, h);
		}
		case FX270: {
			return flipX270Map(w, h);
		}
		case NADA: {
			return nadaMap(w, h);
		}
		default: {
			return nadaMap(w, h);
		}
		}
	}
	

	// ------------- STATIC METHODS FOR ROTATION AND REFLECTION ------------- //
	

	/**
	 * Rotates an array of integers 90 degrees clockwise, as determined by width and height arguments. 
	 * In computer graphics standards, rotation counterclockwise, so this is rotate270 in CG parlance.
	 * 
	 * @param pixels	an array of integer values, possibly RGB or ARGB colors or array indices
	 * @param width		width of the bitmap for the pixels array 
	 * @param height	height of the bitmap for the pixels array 
	 * @return			a transformed copy of the pixels array
	 */
	public static int[] rotate270(int[] pixels, int width, int height) {
		int[] newPixels = new int[pixels.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width; // Index in the original array
				int newX = height - 1 - y; // Calculate rotated x-coordinate
				int newY = x; // and rotated y-coordinate
				int j = newX + newY * height;
				newPixels[j] = pixels[i];
			}
		}
		return newPixels;
	}

	/**
	 * Rotates an array of integers 90 degrees counter-clockwise, as determined by width and height arguments. 
	 * In computer graphices, this is a standard 90 degree rotation. 
	 * 
	 * @param pixels	an array of integer values, possibly RGB or ARGB colors or array indices
	 * @param width		width of the bitmap for the pixels array 
	 * @param height	height of the bitmap for the pixels array 
	 * @return			a transformed copy of the pixels array
	 */
	public static int[] rotate90(int[] pixels, int width, int height) {
		int[] newPixels = new int[pixels.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				int newX = y;
				int newY = width - 1 - x;
				int j = newX + newY * height;
				newPixels[j] = pixels[i];
			}
		}
		return newPixels;
	}

	/**
	 * Rotates an array of integers 180 degrees, as determined by width and height arguments. 
	 * 
	 * @param pixels	an array of integer values, possibly RGB or ARGB colors or array indices
	 * @param width		width of the bitmap for the pixels array 
	 * @param height	height of the bitmap for the pixels array 
	 * @return			a transformed copy of the pixels array
	 */
	public static int[] rotate180(int[] pixels, int width, int height) {
		int[] newPixels = new int[pixels.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				int newX = width - 1 - x;
				int newY = height - 1 - y;
				int j = newX + newY * width;
				newPixels[j] = pixels[i];
			}
		}
		return newPixels;
	}

	/**
	 * Reflects an array of integers on vertical axis, as determined by width and height arguments, flipping x-coordinates. 
	 * 
	 * @param pixels	an array of integer values, possibly RGB or ARGB colors or array indices
	 * @param width		width of the bitmap for the pixels array 
	 * @param height	height of the bitmap for the pixels array 
	 * @return			a transformed copy of the pixels array
	 */
	public static int[] flipX(int[] pixels, int width, int height) {
		int[] newPixels = new int[pixels.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width; // Index in the original array
				int newX = width - 1 - x; // Calculate the reflected x-coordinate
				int newY = y; // y-coordinate is unchanged
				int j = newX + newY * width; // Index in the new array
				newPixels[j] = pixels[i]; // Copy the pixel value to the new array
			}
		}
		return newPixels;
	}

	/**
	 * Reflects an array of integers on the horizontal axis, as determined by width and height arguments, flipping y-coordinates. 
	 * 
	 * @param pixels	an array of integer values, possibly RGB or ARGB colors or array indices
	 * @param width		width of the bitmap for the pixels array 
	 * @param height	height of the bitmap for the pixels array 
	 * @return			a transformed copy of the pixels array
	 */
	public static int[] flipY(int[] pixels, int width, int height) {
		int[] newPixels = new int[pixels.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				int newX = x;
				int newY = height - 1 - y;
				int j = newX + newY * width;
				newPixels[j] = pixels[i];
			}
		}
		return newPixels;
	}

	/**
	 * Reflects an array of integers on the primary diagonal, as determined by width and height arguments.
	 * 
	 * @param pixels	an array of integer values, possibly RGB or ARGB colors or array indices
	 * @param width		width of the bitmap for the pixels array 
	 * @param height	height of the bitmap for the pixels array 
	 * @return			a transformed copy of the pixels array
	 */
	public static int[] flipX90(int[] pixels, int width, int height) {
		int[] newPixels = new int[pixels.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				int newX = y;
				int newY = x;
				int j = newX + newY * height;
				newPixels[j] = pixels[i];
			}
		}
		return newPixels;
	}

	/**
	 * Reflects an array of integers on the secondary diagonal, as determined by width and height arguments.
	 * 
	 * @param pixels	an array of integer values, possibly RGB or ARGB colors or array indices
	 * @param width		width of the bitmap for the pixels array 
	 * @param height	height of the bitmap for the pixels array 
	 * @return			a transformed copy of the pixels array
	 */
	public static int[] flipX270(int[] pixels, int width, int height) {
		int[] newPixels = new int[pixels.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				int newX = height - 1 - y;
				int newY = width - 1 - x;
				int j = newX + newY * height;
				newPixels[j] = pixels[i];
			}
		}
		return newPixels;
	}


	// ------------- STATIC METHODS FOR ROTATING AND REFLECTING A PROCESSING PIMAGE ------------- //
	
	// TODO I wonder if there's a way to swap width and height without creating a new PImage? 
	// Width and height are public fields of PImage, so it can be done. So far, though, I haven't
	// found a way to do this that also displays the bitmap correctly. 
	
	/**
	 * Rotates a PImage 90 degrees clockwise.
	 * 
	 * @param img	a PImage
	 * @return		transformed image
	 */
	public static PImage rotate270(PImage img) {
		img.loadPixels();
		img.pixels = rotate270(img.pixels, img.width, img.height);
		img.updatePixels();
        PImage newImage = PixelAudio.myParent.createImage(img.height, img.width, PConstants.ARGB);
        newImage.pixels = img.pixels;
        img = newImage;
        return img;
	}
	
	/**
	 * Rotates a PImage 90 degrees counter-clockwise.
	 * 
	 * @param img	a PImage
	 * @return		transformed image
	 */
	public static PImage rotate90(PImage img) {
		img.loadPixels();
		img.pixels = rotate90(img.pixels, img.width, img.height);
		img.updatePixels();
        PImage newImage = PixelAudio.myParent.createImage(img.height, img.width, PConstants.ARGB);
        newImage.pixels = img.pixels;
        img = newImage;
        return img;
	}
	
	/**
	 * Rotates a PImage 180 degrees.
	 * 
	 * @param img	a PImage
	 * @return		transformed image
	 */
	public static PImage rotate180(PImage img) {
		img.loadPixels();
		img.pixels = rotate180(img.pixels, img.width, img.height);
		img.updatePixels();
        return img;
	}
	
	/**
	 * Reflects a PImage on the vertical axis, flipping the x-coordinates.
	 * 
	 * @param img	a PImage
	 * @return		transformed image
	 */
	public static PImage flipX(PImage img) {
		img.loadPixels();
		img.pixels = flipX(img.pixels, img.width, img.height);
		img.updatePixels();
        return img;
	}
	
	/**
	 * Reflects a PImage on the horizontal axis, flipping the y-coordinates.
	 * 
	 * @param img	a PImage
	 * @return		transformed image
	 */
	public static PImage flipY(PImage img) {
		img.loadPixels();
		img.pixels = flipY(img.pixels, img.width, img.height);
		img.updatePixels();
        return img;
	}

	/**
	 * Reflects a PImage on the primary diagonal, running from upper left to lower right.
	 * 
	 * @param img	a PImage
	 * @return		transformed image
	 */
	public static PImage flipX90(PImage img) {
		img.loadPixels();
		img.pixels = flipX90(img.pixels, img.width, img.height);
		img.updatePixels();
        PImage newImage = PixelAudio.myParent.createImage(img.height, img.width, PConstants.ARGB);
        newImage.pixels = img.pixels;
        img = newImage;
        return img;
	}
	
	/**
	 * Reflects a PImage on the secondary diagonal, running from lower left to upper right.
	 * 
	 * @param img	a PImage
	 * @return		transformed image
	 */
	public static PImage flipX270(PImage img) {
		img.loadPixels();
		img.pixels = flipX270(img.pixels, img.width, img.height);
		img.updatePixels();
        PImage newImage = PixelAudio.myParent.createImage(img.height, img.width, PConstants.ARGB);
        newImage.pixels = img.pixels;
        img = newImage;
        return img;
	}
	
	
	// ------------- STATIC METHODS FOR SINGLE COORDINATE PAIR TRANSFORM ------------- //
	
	
	/**
	 * @param x		x-coordinate of pixel
	 * @param y		y-coordinate of pixel
	 * @param w		width of bitmap
	 * @param h		height of bitmap
	 * @return		transformed coordinates {x, y}
	 */
	public static int[] rotate270Coord(int x, int y, int w, int h) {
		int newX = h - 1 - y; // Calculate rotated x-coordinate
		int newY = x; // and rotated y-coordinate
		return new int[] { newX, newY };
	}

	/**
	 * @param x		x-coordinate of pixel
	 * @param y		y-coordinate of pixel
	 * @param w		width of bitmap
	 * @param h		height of bitmap
	 * @return		transformed coordinates {x, y}
	 */
	public static int[] rotate90Coord(int x, int y, int w, int h) {
		int newX = y;
		int newY = w - 1 - x;
		return new int[] { newX, newY };
	}

	/**
	 * @param x		x-coordinate of pixel
	 * @param y		y-coordinate of pixel
	 * @param w		width of bitmap
	 * @param h		height of bitmap
	 * @return		transformed coordinates {x, y}
	 */
	public static int[] rotate180Coord(int x, int y, int w, int h) {
		int newX = w - 1 - x;
		int newY = h - 1 - y;
		return new int[] { newX, newY };
	}

	/**
	 * @param x		x-coordinate of pixel
	 * @param y		y-coordinate of pixel
	 * @param w		width of bitmap
	 * @param h		height of bitmap
	 * @return		transformed coordinates {x, y}
	 */
	public static int[] flipXCoord(int x, int y, int w, int h) {
		int newX = w - 1 - x; // Calculate the reflected x-coordinate
		int newY = y; // y-coordinate is unchanged
		return new int[] { newX, newY };
	}

	/**
	 * @param x		x-coordinate of pixel
	 * @param y		y-coordinate of pixel
	 * @param w		width of bitmap
	 * @param h		height of bitmap
	 * @return		transformed coordinates {x, y}
	 */
	public static int[] flipYCoord(int x, int y, int w, int h) {
		int newX = x;
		int newY = h - 1 - y;
		return new int[] { newX, newY };
	}


	/**
	 * Reflect x and y on primary diagonal.
	 * 
	 * @param x		x-coordinate of pixel
	 * @param y		y-coordinate of pixel
	 * @param w		width of bitmap
	 * @param h		height of bitmap
	 * @return		transformed coordinates {x, y}
	 */
	public static int[] flipX90Coord(int x, int y, int w, int h) {
		int newX = y;
		int newY = x;
		return new int[] { newX, newY };
	}

	/**
	 * Reflect x and y on secondary diagonal.
	 * 
	 * @param x		x-coordinate of pixel
	 * @param y		y-coordinate of pixel
	 * @param w		width of bitmap
	 * @param h		height of bitmap
	 * @return		transformed coordinates {x, y}
	 */
	public static int[] flipX270Coord(int x, int y, int w, int h) {
		int newX = h - 1 - y;
		int newY = w - 1 - x;
		return new int[] { newX, newY };
	}

	
	/**
	 * Don't do nothing, no, no, no, just return an array with x and y.
	 * 
	 * @param x		x-coordinate of pixel
	 * @param y		y-coordinate of pixel
	 * @param w		width of bitmap
	 * @param h		height of bitmap
	 * @return		untransformed coordinates {x, y}
	 */
	public static int[] nadaCoord(int x, int y, int w, int h) {
		return new int[] { x, y };
	}
	
	
	// ------------- STATIC METHODS FOR INDEX MAP GENERATION ------------- //
	
	/**
	 * Generates a map to rotate pixels 90 degrees CW. 
	 * 
	 * @param width		width of transformation map
	 * @param height	height of transformation map
	 * @return			look up table for a geometric transform of 90 degrees CW
	 */
	public static int[] rotate270Map(int width, int height) {
		int[] newPixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width; 			// Index in the row major array
				int newX = height - 1 - y; 		// Calculate rotated x-coordinate
				int newY = x; 					// and rotated y-coordinate
				int j = newX + newY * height; 	// new index
				newPixels[j] = i;
			}
		}
		return newPixels;
	}

	/**
	 * Generates a map to rotate pixels 90 degrees CCW. 
	 * 
	 * @param width		width of transformation map
	 * @param height	height of transformation map
	 * @return			look up table for a geometric transform of 90 degrees CCW
	 */
	public static int[] rotate90Map(int width, int height) {
		int[] newPixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				int newX = y;
				int newY = width - 1 - x;
				int j = newX + newY * height;
				newPixels[j] = i;
			}
		}
		return newPixels;
	}

	/**
	 * Generates a map to rotate pixels 180 degrees. 
	 * 
	 * @param width		width of transformation map
	 * @param height	height of transformation map
	 * @return			look up table for a geometric transform rotates 180 degrees
	 */
	public static int[] rotate180Map(int width, int height) {
		int[] newPixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				int newX = width - 1 - x;
				int newY = height - 1 - y;
				int j = newX + newY * width;
				newPixels[j] = i;
			}
		}
		return newPixels;
	}

	/**
	 * Generates a map to reflect pixels on the vertical axis, flipping x-coordinates. 
	 * 
	 * @param width		width of transformation map
	 * @param height	height of transformation map
	 * @return			look up table for a geometric transform that reflects on Y-axis
	 */
	public static int[] flipXMap(int width, int height) {
		int[] newPixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width; 			// Index in the original array
				int newX = width - 1 - x; 		// Calculate the reflected x-coordinate
				int newY = y; 					// y-coordinate is unchanged
				int j = newX + newY * width; 	// Index in the new array
				newPixels[j] = i; 				// Copy the pixel value to the new array
			}
		}
		return newPixels;
	}

	/**
	 * Generates a map to reflect pixels on the horizontal axis, flipping y-coordinates. 
	 * 
	 * @param width		width of transformation map
	 * @param height	height of transformation map
	 * @return			look up table for a geometric transform that reflects on X-axis
	 */
	public static int[] flipYMap(int width, int height) {
		int[] newPixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				int newX = x;
				int newY = height - 1 - y;
				int j = newX + newY * width;
				newPixels[j] = i;
			}
		}
		return newPixels;
	}
	
	/**
	 * Generates a map to reflect pixels on primary diagonal. 
	 * 
	 * @param width		width of transformation map
	 * @param height	height of transformation map
	 * @return			look up table for a geometric transform that reflects on primary diagonal
	 */
	public static int[] flipX90Map(int width, int height) {
		int[] newPixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				int newX = y;
				int newY = x;
				int j = newX + newY * height;
				newPixels[j] = i;
			}
		}
		return newPixels;
	}
	
	/**
	 * Generates a map to reflect pixels on secondary diagonal. 
	 * 
	 * @param width		width of transformation map
	 * @param height	height of transformation map
	 * @return			look up table for a geometric transform that reflects on secondary diagonal
	 */
	public static int[] flipX270Map(int width, int height) {
		int[] newPixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				int newX = height - 1 - y;
				int newY = width - 1 - x;
				int j = newX + newY * height;
				newPixels[j] = i;
			}
		}
		return newPixels;
	}
	
	/**
	 * Generates a map to leave a pixel array unchanged: 
	 * the don't do nothing map, oh no, nada, nothing map.
	 * 
	 * @param width		width of transformation map
	 * @param height	height of transformation map
	 * @return			look up table for a geometric transform that does nothing
	 */
	public static int[] nadaMap(int width, int height) {
		int[] newPixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				newPixels[i] = i;
			}
		}
		return newPixels;
	}

}
