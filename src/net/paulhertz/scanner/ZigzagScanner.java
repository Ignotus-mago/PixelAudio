package net.paulhertz.scanner;

// Bizarre error "PixelScannerINF cannot be resolved to a type" does not prevent compilation or library build.
// The same error appears for MooreScanner and HilbertScanner. It may have something to do with package level
// visibility of PixelScannerINF in GlitchSort2?
// Seems to be a common confusion in Eclipse. Clean the Project clears the error.
public class ZigzagScanner implements PixelScannerINF {
	/** zigzag x coord */
	private int zagx;
	/** zigzag y coord */
	private int zagy;
	/** x coordinates */
	int[] xcoords;
	/** y coordinates */
	int[] ycoords;
	/**  */
	private int[] indexMap;
	/** the dimension of an edge of the square block of pixels */
	private int d;
	/** the total number of pixels in the block */
	private int n;
	/** counter variable f = d + d - 1: number of diagonals in zigzag */
	private int f;
	/** the verbose */
	public boolean verbose = false;

	/**
	 * @param order   the number of pixels on an edge of the scan block
	 *                Note that in the Moore and Hilbert scanners the "order" param is called "depth"
	 *                because its value is the depth of recursion of the curve.
	 *                TODO generalize ZigzagScanner to accept any height and width dimensions. At the moment, it's a square.
	 */
	public ZigzagScanner(int order) {
		d = order;
		f = d + d - 1;
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
	public void swapXY() {
		// TODO Auto-generated method stub

	}

	@Override
	public void swapCoords() {
		// TODO Auto-generated method stub

	}

	@Override
	public void generateCoords() {
		int p = 0;
		int n = 0;
		int index = 0;
		for (int t = 0; t < f; t++) {
			if (t < d) {
				n++;
				if (n % 2 == 0) {
					for (int i = 0; i < n; i++) {
						zagx = n - i - 1;
						zagy = i;
						xcoords[p] = zagx;
						ycoords[p] = zagy;
						index = zagx + d * zagy;
						indexMap[index] = p;
						p++;
					}
				} else {
					for (int i = 0; i < n; i++) {
						zagx = i;
						zagy = n - i - 1;
						xcoords[p] = zagx;
						ycoords[p] = zagy;
						index = zagx + d * zagy;
						indexMap[index] = p;
						p++;
					}
				}
			} // end if t < d
			else {
				n--;
				if (n % 2 == 0) {
					for (int i = 0; i < n; i++) {
						zagx = d - i - 1;
						zagy = i + d - n;
						xcoords[p] = zagx;
						ycoords[p] = zagy;
						index = zagx + d * zagy;
						indexMap[index] = p;
						p++;
					}
				} else {
					for (int i = 0; i < n; i++) {
						zagx = i + d - n;
						zagy = d - i - 1;
						xcoords[p] = zagx;
						ycoords[p] = zagy;
						index = zagx + d * zagy;
						indexMap[index] = p;
						p++;
					}
				}
			} // end if t > d
		}
	}

	/**
	 * @param pix an array of pixels
	 * @param w   width of the image represented by the array of pixels
	 * @param h   height of the image represented by the array of pixels
	 * @param x   x-coordinate of the location in the image to scan
	 * @param y   y-coordinate of the location in the image to scan
	 * @return an array in the order determined by the zigzag scan
	 */
	@Override
	public int[] pluck(int[] pix, int w, int h, int x, int y) {
		int len = n;
		int[] out = new int[len];
		for (int i = 0; i < len; i++) {
			int p = (y + ycoords[i]) * w + (x) + xcoords[i];
			if (verbose)
				System.out.println("x = " + x + ", y = " + y + ", i = " + i + ", p = " + p + ", zigzag = (" + xcoords[i]
						+ ", " + ycoords[i] + ")");
			out[i] = pix[p];
		}
		return out;
	}

	/**
	 * @param pix    an array of pixels
	 * @param sprout an array of n = d * d pixels to write to the array of pixels
	 * @param w      width of the image represented by the array of pixels
	 * @param h      height of the image represented by the array of pixels
	 * @param x      x-coordinate of the location in the image to write to
	 * @param y      y-coordinate of the location in the image to write to
	 */
	@Override
	public void plant(int[] pix, int[] sprout, int w, int h, int x, int y) {
		for (int i = 0; i < n; i++) {
			int p = (y + ycoords[i]) * w + (x) + xcoords[i];
			pix[p] = sprout[i];
		}
	}


	/**
	 * @return   returns -1 because the ZigzagScanner does not use recursion
	 */
	@Override
	public int getDepth() {
		return -1;
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
		return n;
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

	/*
	 * (non-Javadoc) returns a list of coordinate points that define a zigzag scan
	 * of order d.
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Zigzag order: " + this.d + "\n  ");
		for (int i = 0; i < xcoords.length; i++) {
			buf.append("(" + xcoords[i] + ", " + ycoords[i] + ") ");
		}
		buf.append("\n");
		return buf.toString();
	}

}
