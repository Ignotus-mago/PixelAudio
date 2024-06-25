/**
 *
 */
package net.paulhertz.scanner;

import java.util.Arrays;

/**
 * @author ignotus
 *
 */
public class MooreScanner implements PixelScannerINF {

	/** x coord used internally */
	private int moorex;
	/** Hilbert y coord, ised internally */
	private int moorey;
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

	/**
	 * @param depth   the depth of recursion that determines the number of pixels on an edge of the scan block
	 *                Depth can be calculated from the image dimensions as (int) ((log(height)/log(2))).
	 */
	public MooreScanner(int depth) {
		this.depth = depth;
		d = (int) Math.round(Math.pow(2, depth));
		n = d * d;
		xcoords = new int[n];
		ycoords = new int[n];
		indexMap = new int[n];
		generateCoords();
	}

	@Override
	public PixelScanner.ScannerType getScannerType() {
		return PixelScanner.ScannerType.MOORE;
	}


	/**
	 * Generates coordinates of a block of pixels of specified dimensions, offset from (0,0).
	 */
	@Override
	public void generateCoords() {
		int index = 0;
		int hilbDepth;
		if (depth > 1) {
			hilbDepth = depth - 1;
		}
		else {
			System.out.println("----->>> Cannot generate Moore curve for depth < 2");
			return;
		}
		// rewrite the order of Hilbert curve coordinates to get the order for a Moore curve
		HilbertScanner hilb = new HilbertScanner(hilbDepth);
		hilb.swapCoords();
		hilb.flipX();
		for (int i = 0; i < hilb.getSize(); i++) {
			xcoords[index] = hilb.xcoord(i);
			ycoords[index] = hilb.ycoord(i);
			index++;
		}
		// shift y coordinate by Hilbert scanner's width
		int hilbWidth = hilb.getBlockWidth();
		for (int i = 0; i < hilb.getSize(); i++) {
			xcoords[index] = hilb.xcoord(i);
			ycoords[index] = hilb.ycoord(i) + hilbWidth;
			index++;
		}
		hilb.flipX();
		hilb.flipY();
		// shift x and y coordinates by Hilbert scanner's width
		for (int i = 0; i < hilb.getSize(); i++) {
			xcoords[index] = hilb.xcoord(i) + hilbWidth;
			ycoords[index] = hilb.ycoord(i) + hilbWidth;
			index++;
		}
		// shift x coordinate by Hilbert scanner's width
		for (int i = 0; i < hilb.getSize(); i++) {
			xcoords[index] = hilb.xcoord(i) + hilbWidth;
			ycoords[index] = hilb.ycoord(i);
			index++;
		}
		// now set up indexing
		index = 0;
		for (int i = 0; i < n; i++) {
			moorex = xcoords[i];
			moorey = ycoords[i];
			index = moorex + d * moorey;
			indexMap[index] = i;
		}
	}


	@Override
	public void swapXY() {
		int temp = moorey;
		moorey = moorex;
		moorex = temp;
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
		buf += ("Moore curve order: "+ this.d +"\n  ");
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

	// get the index in the Moore curve of a specified Cartesian point
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
