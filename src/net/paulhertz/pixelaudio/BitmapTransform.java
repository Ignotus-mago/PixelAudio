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
 * <code>PImage.pixels</code> array, and resize the image when necessary. *Map methods
 * that accept width and height parameters return arrays of transformed pixel indices.</p>
 * 
 * <p>BitmapTransform's methods are faster and more generalized than those in
 * ImageTransform, but they do not provide scaling or rotation by angles other
 * than 90 degrees.</p>
 * 
 * @see AffineTransformType
 * @see ImageTransform
 * 
 */
public class BitmapTransform {

	public static PImage imageTransform(PImage img, AffineTransformType type) {
		switch (type) {
		case ROT90: {
			img = BitmapTransform.rotate90(img);
			break;
		}
		case ROT90CCW: {
			img = rotate90CCW(img);
			break;
		}
		case ROT180: {
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
		case FLIPX90: {
			img = flipX90(img);		// secondary diagonal
			break;
		}
		case FLIPX90CCW: {
			img = flipX90CCW(img);  // primary diagonal
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

	
	public static int[] pixelsTransform(int[] pixels, int width, int height, AffineTransformType type) {
		switch (type) {
		case ROT90: {
			return BitmapTransform.rotate90(pixels, width, height);
		}
		case ROT90CCW: {
			return BitmapTransform.rotate90CCW(pixels, width, height);
		}
		case ROT180: {
			return BitmapTransform.rotate180(pixels, width, height);
		}
		case FLIPX: {
			return BitmapTransform.flipX(pixels, width, height);
		}
		case FLIPY: {
			return BitmapTransform.flipY(pixels, width, height);
		}
		case FLIPX90: {
			return BitmapTransform.flipX90(pixels, width, height);		// secondary diagonal
		}
		case FLIPX90CCW: {
			return BitmapTransform.flipX90CCW(pixels, width, height);  // primary diagonal
		}
		case NADA: {
			return Arrays.copyOf(pixels, pixels.length);
		}
		default: {
			return Arrays.copyOf(pixels, pixels.length);
		}
		}
	}
	
	public static int[] coordTransform(int x, int y, int width, int height, AffineTransformType type) {
		switch (type) {
		case ROT90: {
			return BitmapTransform.rotate90Coord(x, y, width, height);
		}
		case ROT90CCW: {
			return BitmapTransform.rotate90CCWCoord(x, y, width, height);
		}
		case ROT180: {
			return BitmapTransform.rotate180Coord(x, y, width, height);
		}
		case FLIPX: {
			return BitmapTransform.flipXCoord(x, y, width, height);
		}
		case FLIPY: {
			return BitmapTransform.flipYCoord(x, y, width, height);
		}
		case FLIPX90: {
			return BitmapTransform.flipX90Coord(x, y, width, height);		// secondary diagonal
		}
		case FLIPX90CCW: {
			return BitmapTransform.flipX90CCWCoord(x, y, width, height);  // primary diagonal
		}
		case NADA: {
			return BitmapTransform.nadaCoord(x, y, width, height);
		}
		default: {
			return BitmapTransform.nadaCoord(x, y, width, height);
		}
		}
	}

	
	public static int[] getIndexMap(int w, int h, AffineTransformType type) {
		switch (type) {
		case ROT90: {
			return rotate90Map(w, h);
		}
		case ROT90CCW: {
			return rotate90CCWMap(w, h);
		}
		case ROT180: {
			return rotate180Map(w, h);
		}
		case FLIPX: {
			return flipXMap(w, h);
		}
		case FLIPY: {
			return flipYMap(w, h);
		}
		case FLIPX90CCW: {
			return flipX90CCWMap(w, h);
		}
		case FLIPX90: {
			return flipX90Map(w, h);
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
	

	public static int[] rotate90(int[] pixels, int width, int height) {
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

	public static int[] rotate90CCW(int[] pixels, int width, int height) {
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

	// primary diagonal
	public static int[] flipX90CCW(int[] pixels, int width, int height) {
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

	// secondary diagonal
	public static int[] flipX90(int[] pixels, int width, int height) {
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
	

	public static PImage rotate90(PImage img) {
		img.loadPixels();
		img.pixels = rotate90(img.pixels, img.width, img.height);
		img.updatePixels();
        PImage newImage = PixelAudio.myParent.createImage(img.height, img.width, PConstants.ARGB);
        newImage.pixels = img.pixels;
        img = newImage;
        return img;
	}
	
	public static PImage rotate90CCW(PImage img) {
		img.loadPixels();
		img.pixels = rotate90CCW(img.pixels, img.width, img.height);
		img.updatePixels();
        PImage newImage = PixelAudio.myParent.createImage(img.height, img.width, PConstants.ARGB);
        newImage.pixels = img.pixels;
        img = newImage;
        return img;
	}
	
	public static PImage rotate180(PImage img) {
		img.loadPixels();
		img.pixels = rotate180(img.pixels, img.width, img.height);
		img.updatePixels();
        return img;
	}
	
	public static PImage flipX(PImage img) {
		img.loadPixels();
		img.pixels = flipX(img.pixels, img.width, img.height);
		img.updatePixels();
        return img;
	}
	
	public static PImage flipY(PImage img) {
		img.loadPixels();
		img.pixels = flipY(img.pixels, img.width, img.height);
		img.updatePixels();
        return img;
	}

	// primary diagonal
	public static PImage flipX90CCW(PImage img) {
		img.loadPixels();
		img.pixels = flipX90CCW(img.pixels, img.width, img.height);
		img.updatePixels();
        PImage newImage = PixelAudio.myParent.createImage(img.height, img.width, PConstants.ARGB);
        newImage.pixels = img.pixels;
        img = newImage;
        return img;
	}
	
	// secondary diagonal
	public static PImage flipX90(PImage img) {
		img.loadPixels();
		img.pixels = flipX90(img.pixels, img.width, img.height);
		img.updatePixels();
        PImage newImage = PixelAudio.myParent.createImage(img.height, img.width, PConstants.ARGB);
        newImage.pixels = img.pixels;
        img = newImage;
        return img;
	}
	
	
	// ------------- STATIC METHODS FOR SINGLE COORDINATE PAIR TRANSFORM ------------- //
	
	public static int[] rotate90Coord(int x, int y, int w, int h) {
		int newX = h - 1 - y; // Calculate rotated x-coordinate
		int newY = x; // and rotated y-coordinate
		return new int[] { newX, newY };
	}

	public static int[] rotate90CCWCoord(int x, int y, int w, int h) {
		int newX = y;
		int newY = w - 1 - x;
		return new int[] { newX, newY };
	}

	public static int[] rotate180Coord(int x, int y, int w, int h) {
		int newX = w - 1 - x;
		int newY = h - 1 - y;
		return new int[] { newX, newY };
	}

	public static int[] flipXCoord(int x, int y, int w, int h) {
		int newX = w - 1 - x; // Calculate the reflected x-coordinate
		int newY = y; // y-coordinate is unchanged
		return new int[] { newX, newY };
	}

	public static int[] flipYCoord(int x, int y, int w, int h) {
		int newX = x;
		int newY = h - 1 - y;
		return new int[] { newX, newY };
	}

	// reflect on primary diagonal
	public static int[] flipX90CCWCoord(int x, int y, int w, int h) {
		int newX = y;
		int newY = x;
		return new int[] { newX, newY };
	}

	// reflect on secondary diagonal
	public static int[] flipX90Coord(int x, int y, int w, int h) {
		int newX = h - 1 - y;
		int newY = w - 1 - x;
		return new int[] { newX, newY };
	}

	// don't do nothing, no, no, no
	public static int[] nadaCoord(int x, int y, int w, int h) {
		return new int[] { x, y };
	}
	
	
	// ------------- STATIC METHODS FOR INDEX MAP GENERATION ------------- //
	
	public static int[] rotate90Map(int width, int height) {
		int[] newPixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width; // Index in the row major array
				int newX = height - 1 - y; // Calculate rotated x-coordinate
				int newY = x; // and rotated y-coordinate
				int j = newX + newY * height; // new index
				newPixels[j] = i;
			}
		}
		return newPixels;
	}

	public static int[] rotate90CCWMap(int width, int height) {
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

	public static int[] flipXMap(int width, int height) {
		int[] newPixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width; // Index in the original array
				int newX = width - 1 - x; // Calculate the reflected x-coordinate
				int newY = y; // y-coordinate is unchanged
				int j = newX + newY * width; // Index in the new array
				newPixels[j] = i; // Copy the pixel value to the new array
			}
		}
		return newPixels;
	}

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
	
	// reflect on primary diagonal
	public static int[] flipX90CCWMap(int width, int height) {
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
	
	// reflect on secondary diagonal
	public static int[] flipX90Map(int width, int height) {
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
	
	// don't do nothing, no, no, no
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
