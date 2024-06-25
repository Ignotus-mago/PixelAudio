package net.paulhertz.pixelaudio;

/**
 * Provides static methods for rotating and reflecting bitmap pixel arrays using index remapping. The "pixel" arrays
 * do not necessarily contain color values--they can contain any integer value, and it will not be changed by the operations.
 * 
 */
public class BitmapTransformations {
	
	
	// ------------- STATIC METHODS FOR ROTATION AND REFLECTION ------------- //
	
	
	public static int[] rotate90Clockwise(int[] pixels, int width, int height) {
		int[] newPixels = new int[pixels.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width; 					// Index in the original array
				int newX = height - 1 - y;				// Calculate rotated x-coordinate
				int newY = x;							// sw
				int j = newX + newY * height;
				newPixels[j] = pixels[i];
			}
		}
		return newPixels;
	}

	public static int[] rotate90Counterclockwise(int[] pixels, int width, int height) {
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

	public static int[] reflecVertically(int[] pixels, int width, int height) {
		int[] newPixels = new int[pixels.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width; 					// Index in the original array
				int newX = width - 1 - x; 				// Calculate the reflected x-coordinate
				int newY = y;							// y-coordinate is unchanged
				int j = newX + newY * width; 			// Index in the new array
				newPixels[j] = pixels[i]; 				// Copy the pixel value to the new array
			}
		}
		return newPixels;
	}

	public static int[] reflectHorizontally(int[] pixels, int width, int height) {
		int[] newPixels = new int[pixels.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				int newX = width - 1 - x;
				int newY = y;
				int j = newX + newY * width;
				newPixels[j] = pixels[i];
			}
		}
		return newPixels;
	}

	public static int[] reflectDiagonallyPrimary(int[] pixels, int width, int height) {
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

	public static int[] reflectDiagonallySecondary(int[] pixels, int width, int height) {
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

}
