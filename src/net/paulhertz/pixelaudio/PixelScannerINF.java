/**
 * Interface for classes that map values in a one-dimensional array (signal)
 * to a two-dimensional bitmap (image), usually in orders other than
 * the usual left to right and top to bottom (x,y) scan line order.
 * You can think of the signal as a function that traverses every pixel in
 * a bitmap. PixelScannerINF implementations implement the function and its
 * inverse however they see fit. A lookup table will do, and so will a
 * mathematical function. The mapping can be accessed with the lookup()
 * and xcoord() and ycoord() functions. Values can be read from and
 * written to the signal and the bitmap data structures using the plant()
 * and pluck() methods. Again, the implementation is up the the user,
 * though the image data structure is necessarily (for now) a regular
 * old bitmap, typically a Processing PImage, and both signal and map
 * employ 24-bit RGB or 32-bit ARGB values. Backing arrays in other formats
 * can be used in an implementation where a high-resolution audio signa
 * is considered useful.
 *
 * PixelScanner classes generally do not implement the 1-D signal or 2-D image
 * data structures themselves, they provide methods to map points in an image to
 * positions in a signal and vice versa. In the sample classes in this library, the
 * mapping is accomplished with integer arrays. The image and signal data structures
 * that are implemented in external code are typically a 1-D array of floating point or
 * integer values for signals and a standard 2-D bitmap array for images, encoded
 * in horizontal scanline rows and vertical columns.
 *
 */
package net.paulhertz.pixelaudio;

/**
 * @author Ignotus_Mago
 *
 */
public interface PixelScannerINF {

	/** @return the type of this scanner, read-only */
	public PixelScanner.ScannerType getScannerType();

	/** flip the order of the x coordinates */
	abstract void flipX();

	/** flip the order of the y coordinates */
	abstract void flipY();

	/** swap the x and y coordinates in the map */
	abstract void swapXY();

	/** swap all x and y coordinates */
	abstract void swapCoords();

	/** generate coords for indexMap */
	abstract void generateCoords();


	/**
	 * @param pix   an array of pixels
	 * @param w     width of the image represented by the array of pixels
	 * @param h     height of the image represented by the array of pixels
	 * @param x     x-coordinate of the location in the image to scan
	 * @param y     y-coordinate of the location in the image to scan
	 * @return      an array in the order determined by the scan
	 */
	abstract int[] pluck(int[] pix, int w, int h, int x, int y);

	/**
	 * @param pix      an array of pixels
	 * @param sprout   an array of pixels to write to pix
	 * @param w        width of the image represented by the array of pixels
	 * @param h        height of the image represented by the array of pixels
	 * @param x        x-coordinate of the location in the image to write to
	 * @param y        y-coordinate of the location in the image to write to
	 */
	abstract void plant(int[] pix, int[] sprout, int w, int h, int x, int y);

	/** return a string representation of our data, possibly partial */
	@Override
	abstract String toString();

	/** return a number representing recursion depth. If not applicable, return -1 */
	abstract int getDepth();

	/** return the width of the pixel array mapped in this PixelScannerINF instance */
	abstract int getBlockWidth();

	/** return the height of the pixel array mapped in this PixelScannerINF instance */
	abstract int getBlockHeight();

	/** return the number of pixel values mapped in this PixelScannerINF instance */
	abstract int getSize();

	/** given a pixel location, return the index of a point in the mapped representation */
	abstract int lookup(int x, int y);

	/** return the x coordinate at a specified index in the map */
	abstract int xcoord(int pos);

	/** return the y coordinate at a specified index in the map */
	abstract int ycoord(int pos);

}
