/**
 *
 */
package net.paulhertz.scanner;

import java.util.Arrays;

/**
 * @author ignotus
 *
 */
public class HilbertScanner implements PixelScannerINF {
	/** Hilbert x coord used internally */
	private int bertx;
	/** Hilbert y coord, used internally */
	private int berty;
	/** x coordinates */
	int[] xcoords;
	/** y coordinates */
	int[] ycoords;
	/** flipped x coordinates */
	int[] flipXcoords;
	/** flipped y coordinates */
	int[] flipYcoords;
	/** index map */
	private int[] indexMap;
	/** the depth of recursion of the Hilbert curve */
	int depth = 1;
	/** the dimension of an edge of the square block of pixels */
	int d;
	/** the total number of points in the curve */
	int n;
	/** the verbose */
	boolean verbose = false;
	/** if depth is an odd number we need to correct the distance and coordinate calculations */
	boolean doXYSwap = false;

	/**
	 * @param depth   the depth of recursion that determines the number of pixels on an edge of the scan block
	 *                Depth can be calculated from the image dimensions as (int) ((log(height)/log(2)))
	 */
	public HilbertScanner(int depth) {
		this.depth = depth;
		d = (int) Math.round(Math.pow(2, depth));
		n = d * d;
		xcoords = new int[n];
		ycoords = new int[n];
		indexMap = new int[n];
		doXYSwap = (depth % 2 == 1);
		generateCoords();
	}

	@Override
	public PixelScanner.ScannerType getScannerType() {
		return PixelScanner.ScannerType.HILBERT;
	}

	/** needed to make corrections when width is an odd power of 2 */
	// convert (x,y) to d, the distance (index value) into the curve
	// n = number of points in curve, a power of 4
	private int xy2d (int n) {
		// correct before function body
		if (doXYSwap) {
			swapXY();
		}
		int rx, ry, s, d = 0;
		for (s = n/2; s > 0; s /= 2) {
			rx = ((bertx & s) > 0) ? 1 : 0;
			ry = ((berty & s) > 0) ? 1 : 0;
			d += s * s * ((3 * rx) ^ ry);
			rot(s, rx, ry);
		}
		return d;
	}

	// convert curve index d to display coordinates (x,y), returning the result in bertx and berty
	// n = number of points in curve, a power of 4 = (2^k * 2^k)
	private void d2xy(int n, int d) {
		int rx, ry, s, t = d;
		bertx = berty = 0;
		for (s = 1; s < n; s *= 2) {
			rx = 1 & (t / 2);			// bitwise AND (truncated int division)
			ry = 1 & (t ^ rx);			// bitwise AND (^ is exclusive OR operation)
			rot(s, rx, ry);
			bertx += s * rx;
			berty += s * ry;
			t /= 4;
		}
		// correct after function body
		if (doXYSwap) {
			swapXY();
		}
	}

	// rotate/flip a quadrant appropriately
	void rot(int n, int rx, int ry) {
		if (ry == 0) {
			if (rx == 1) {
				bertx = n - 1 - bertx;
				berty = n - 1 - berty;
			}
			// Swap x and y
			int t  = bertx;
			bertx = berty;
			berty = t;
		}
	}

	@Override
	public void swapXY() {
		int temp = berty;
		berty = bertx;
		bertx = temp;
	}

	/**
	 * Generates coordinates of a block of pixels of specified dimensions, offset from (0,0).
	 */
	@Override
	public void generateCoords() {
		int index = 0;
		for (int i = 0; i < n; i++) {
			d2xy(n, i);
			xcoords[i] = bertx;
			ycoords[i] = berty;
			index = bertx + d * berty;
			indexMap[index] = i;
		}
	}


	@Override
	public void flipX() {
		int m = d - 1;
		for (int i = 0; i < xcoords.length; i++) {
			xcoords[i] = m - xcoords[i];
		}
	}

	@Override
	public void flipY() {
		int m = d - 1;
		for (int i = 0; i < ycoords.length; i++) {
			ycoords[i] = m - ycoords[i];
		}
	}


	@Override
	public void swapCoords() {
		int[] temp = Arrays.copyOf(xcoords, xcoords.length);
		xcoords = ycoords;
		ycoords = temp;
	}


	/**
	 * @param pix   an array of pixels
	 * @param w     width of the image represented by the array of pixels
	 * @param h     height of the image represented by the array of pixels
	 * @param x     x-coordinate of the location in the image to scan
	 * @param y     y-coordinate of the location in the image to scan
	 * @return      an array in the order determined by the Hilbert scan
	 */
	@Override
	public int[] pluck(int[] pix, int w, int h, int x, int y) {
		int len = d * d;
		int[] out = new int[len];
		for (int i = 0; i < len; i++) {
			int p = (y + ycoords[i]) * w + (x) + xcoords[i];
			if (verbose) {
				System.out.println("x = "+ x +", y = "+ y +", i = "+ i +", p = "+ p +", hilbert = ("+ xcoords[i] +", "+ ycoords[i] +")");
			}
			out[i] = pix[p];
		}
		return out;
	}

	/**
	 * @param pix      an array of pixels
	 * @param sprout   an array of d * d pixels to write to the array of pixels
	 * @param w        width of the image represented by the array of pixels
	 * @param h        height of the image represented by the array of pixels
	 * @param x        x-coordinate of the location in the image to write to
	 * @param y        y-coordinate of the location in the image to write to
	 */
	@Override
	public void plant(int[] pix, int[] sprout, int w, int h, int x, int y) {
		for (int i = 0; i < d * d; i++) {
			int p = (y + ycoords[i]) * w + (x) + xcoords[i];
			pix[p] = sprout[i];
		}
	}

	/* (non-Javadoc)
	 * returns a list of coordinate points that define a scan of order d.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String buf = new String();
		buf += ("Hilbert order: "+ this.d +"\n  ");
		for (int i = 0; i < xcoords.length; i++) {
			buf += ("("+ xcoords[i] +", "+ ycoords[i] +") ");
		}
		buf += ("\n");
		return buf;
	}


	@Override
	public int getDepth() {
		return depth;
	}

	@Override
	public int getBlockWidth() {
		return d;
	}

	@Override
	public int getBlockHeight() {
		return d;
	}

	@Override
	public int getSize() {
		return d * d;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public int[] getIndexMap() {
		return this.indexMap;
	}

	@Override
	public int lookup(int x, int y) {
		return indexMap[x + d * y];
	}

	@Override
	public int xcoord(int pos) {
		return xcoords[pos];
	}

	@Override
	public int ycoord(int pos) {
		return ycoords[pos];
	}

	/**
	 * Rotates an array of ints left by d values. Uses efficient "Three Rotation" algorithm.
	 * @param arr   array of ints to rotate
	 * @param d     number of elements to shift
	 */
	public void rotateLeft(int[] arr, int d) {
		d = d % arr.length;
		reverseArray(arr, 0, d - 1);
		reverseArray(arr, d, arr.length - 1);
		reverseArray(arr, 0, arr.length - 1);
	}

	/**
	 * Reverses an arbitrary subset of an array.
	 * @param arr   array to modify
	 * @param l     left bound of subset to reverse
	 * @param r     right bound of subset to reverse
	 */
	public void reverseArray(int[] arr, int l, int r) {
		int temp;
		while (l < r) {
			temp = arr[l];
			arr[l] = arr[r];
			arr[r] = temp;
			l++;
			r--;
		}
	}

	public void rotateXLeft(int offset) {
		rotateLeft(xcoords, offset);
	}

	public void rotateYLeft(int offset) {
		rotateLeft(ycoords, offset);
	}
}
