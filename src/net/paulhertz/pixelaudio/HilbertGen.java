package net.paulhertz.pixelaudio;

import java.util.ArrayList;

/**
 * Generates coordinates and LUTs for a Hilbert curve over a square bitmap starting at (0,0) and ending at (width-1, 0).
 * Width and height must be equal powers of 2. You can also call HilbertGen(int depth) and width and height will equal Math.pow(2, depth). 
 * See abstract class {@link PixelMapGen} for instance variables shared by all child classes. 
 */
public class HilbertGen extends PixelMapGen {
	/** recursion depth */
	public int depth;
	private boolean doXYSwap;

	public final static String description = "HilbertGen generates a Hilbert curve over a square bitmap starting at (0,0) and ending at (width-1, 0). "
			   + "Width and height must be equal powers of 2. You can also call HilbertGen(int depth) and width and height will equal Math.pow(2, depth). ";

	public HilbertGen(int width, int height, AffineTransformType type) {
		super(width, height, type);								// necessary first call
		this.depth = PixelMapGen.findPowerOfTwo(this.w);		// calculate depth before we generate the Hilbert curve
		this.doXYSwap = (this.depth % 2 == 1);					// a value to preserve symmetry and orientation when depth is odd
		// System.out.println("> HilbertGen "+ width +", "+ height +", depth  = "+ depth + ", swap = "+ doXYSwap +", transform = "+ type.name());
		this.generate();										// last of all, once all parameters are set, go ahead and generate coordinates and LUTs.
	}

	public HilbertGen(int width, int height) {
		super(width, height);									// necessary first call
		this.depth = PixelMapGen.findPowerOfTwo(this.w);		// calculate depth before we generate the Hilbert curve
		this.doXYSwap = (this.depth % 2 == 1);					// a value to preserve symmetry and orientation when depth is odd
		// System.out.println("> HilbertGen "+ width +", "+ height +", depth  = "+ depth + ", swap = "+ doXYSwap);
		this.generate();										// last of all, once all parameters are set, go ahead and generate coordinates and LUTs.
	}

	public HilbertGen(int depth) {
		this( (int) Math.round(Math.pow(2, depth)), (int) Math.round(Math.pow(2, depth)) );
	}
	
	public HilbertGen(int depth, AffineTransformType type) {
		this( (int) Math.round(Math.pow(2, depth)), (int) Math.round(Math.pow(2, depth)), type);
	}


	@Override
	public String describe() {
		return HilbertGen.description;
	}

	@Override
	public boolean validate(int width, int height) {
		if (width < 2) {
			System.out.println("HilbertGen Error: 2 is the minimum value for width and height, 1 is the minimum value for depth.");
			return false;
		}
		if (width != height) {
			System.out.println("HilbertGen Error: Width and height must be equal.");
			return false;
		}
		if (! PixelMapGen.isPowerOfTwo(width)) {
			System.out.println("HilbertGen Error: Width and height must be equal to a power of 2.");
			return false;
		}
		return true;
	}
	
	@Override
	public int[] generate() {
		this.coords = this.generateCoordinates();
		return this.setMapsFromCoords(this.coords);
	}
	
	
	private ArrayList<int[]> generateCoordinates() {
		return this.generateHilbertCoordinates(this.getSize());
	}

	private ArrayList<int[]> generateHilbertCoordinates(int n) {
		ArrayList<int[]> coordinates = new ArrayList<>(n);
		if (n == 4) {
			coordinates.add(new int[] { 0, 0 });
			coordinates.add(new int[] { 0, 1 });
			coordinates.add(new int[] { 1, 1 });
			coordinates.add(new int[] { 1, 0 });
			// System.out.println("-- Hilbert n == 4");
		} 
		else {
			for (int i = 0; i < n; i++) {
				int[] xy = d2xy(n, i);
				coordinates.add(xy);
			}
		}
		return coordinates;
	}
	
	private int[] d2xy(int n, int pos) {
		int rx = 0;
		int ry = 0;
		int s = 0;
		int t = pos;
		int bertx = 0;
		int berty = 0;
		for (s = 1; s < n; s *= 2) {	// raise s to next power of 2 until it exceeds n, which is w * h
			rx = 1 & (t / 2); 			// bitwise AND (integer division is truncated)
			ry = 1 & (t ^ rx); 			// bitwise AND (^ is exclusive OR operation)
			if (ry == 0) {
				if (rx == 1) {			// when rx == 1, rotate coordinates
					bertx = s - 1 - bertx;
					berty = s - 1 - berty;
				}
				int temp = berty;		// when ry == 0, swap bertx and berty
				berty = bertx;
				bertx = temp;
			}
			bertx += s * rx;
			berty += s * ry;
			t /= 4;
		}
		if (this.doXYSwap) {
			int temp = berty;			// when recursion depth is odd, swap maintains expected orientation
			berty = bertx;
			bertx = temp;
			
		}
		return new int[]{bertx, berty};
	}

	

	/* ------------------------------ GETTERS AND NO SETTERS ------------------------------ */
	/*                                                                                      */
	/*                  See abstract class PixMapGen for additional methods                 */
	/*                                                                                      */
	/* These include: getWidth(), getHeight(), getSize(), getPixelMap(), getPixelMapCopy(), */
	/* getSampleMap(), get SampleMapCopy(), getCoordinates(), getCoordinatesCopy().         */
	/*                                                                                      */
	/* ------------------------------------------------------------------------------------ */
	
	/* ------------------------------ HILBERT MULTIGEN BUILDER ------------------------------ */
	
	// placeholder
	/**
	 * @param genEdge	number of pixels for the edge of each Hilbert curve, must be a power of 2
	 * @param rows		number of rows of curves
	 * @param cols		number of columns of curves
	 * @return			a MultiGen consisting of rows rows and cols columns of Hilbert curves
	 */
	public static MultiGen buildHilbertMultigen(int genEdge, int rows, int cols) {
		/*
		 * rows even, cols even
		 * rows odd, cols even
		 * rows even, cols odd
		 * rows odd, cols odd
		 * symmetrical or not
		 * 
		 */
		if (rows % 2 == 0) {
			if (cols % 2 == 0) {
				// even number of rows, even number of columns
			}
			else {
				// even number of rows, odd number of columns
			}
		}
		else {
			if (cols % 2 == 0) {
				// odd number of rows, even number of columns
			}
			else {
				// odd number of rows, odd number of columns
			}
		}
		return null;
	}



}
