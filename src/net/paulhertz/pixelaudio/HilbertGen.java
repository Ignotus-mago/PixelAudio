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
			   + "\nWidth and height must be equal powers of 2. You can also call HilbertGen(int depth) and width and height will equal Math.pow(2, depth). ";
	
	
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
		return HilbertGen.prevalidate(width, height);
	}
	
	// static version of validate
	public static boolean prevalidate(int width, int height) {
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

	/**
	 * @param n    size of the array of Hilbert curve coordinates, necessarily a power of 4
	 * @return     an ArrayList of integer pairs {x,y} representing the coordinates of a Hilbert curve
	 */
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
	
	
	//  TODO overload Hilbert Multigens to accept just one dimension argument, hilbertWidth
	
	/* ------------------------------ HILBERT MULTIGEN FACTORIES ------------------------------
	
	 * MultiGens create a PixelMapGen from from a list of PixelMapGen 
	 * objects (genList) and coordinate points (offsetList) where they 
	 * will be displayed. A MultiGen creates a single signal path over all
	 * the PixelMapGen objects. The path may be *continuous*, which is to say that
	 * the path through each PixelMapGen object ("gen" for short) only has to step
	 * one pixel up, down, left, or right to connect to the next gen. It may even
	 * create a loop, where the last pixel in the path is one step away from the
	 * first pixel. This is reflected in the naming conventions. 
	 * 
	 * In the method names, "ortho" refers to gens that are aligned in rows (or
	 * columns) where each new row begins one unit down or over from the previous row,
	 * always adding new gens in the same direction. In the "bou" methods 
	 * (named for boustrophodon, a method of writing text in alternating directions), 
	 * each successive row or column goes in the opposite direction from the previous
	 * one. The bou methods may provide continuous paths, the ortho methods are
	 * inherently discontinuous, like row major bitmaps or video scanlines. 
	 * 
	 * Looping methods are are almost always more complex than bou and necessarily 
	 * more complex than ortho methods. Like the Hilbert curve, they involve
	 * changes in direction reminiscent of folding. Looping methods often have
	 * constraints on the numbers of rows and columns that can produce a loop.
	 * The constraints arise from the connectivity offered by the different
	 * PixelMapGen child classes: Hilbert gens have connections at two adjacent
	 * corners, DiagonalZigzag gens have connections at opposite corners. 
	 * Moore gens are loops to begin with, and have no connections, but are
	 * good for very symmetrical pattern-making.  

	---------------------------------------------------------------------------------------- */
	
	
	// placeholder
	/**
	 * @param columns	number of columns of gens wide
	 * @param rows		number of rows of gens high
	 * @param genEdge	number of pixels for the edge of each Hilbert curve, must be a power of 2
	 * @return			a MultiGen consisting of rows rows and cols columns of Hilbert curves
	 *                  check for null return value
	 */
	public static MultiGen hilbertMultigenLoop(int columns, int rows, int genEdge) {
		// if rows or columns is 1, or if rows and columns are both odd numbers, 
		// double rows and columns and divide genEdge by 2.
		if (columns == 1 || rows == 1) {
			columns *= 2;
			rows *= 2;
			genEdge /= 2;
		}
		if (columns % 2 == 1 && rows % 2 == 1) {
			columns *= 2;
			rows *= 2;
			genEdge /= 2;
		}
		// prevalidate here, maybe throw appropriate error instead of returning null
		if (!HilbertGen.prevalidate(genEdge, genEdge)) {
			return null;
		}
	    // list of PixelMapGens that create a path through an image using PixelAudioMapper
		ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
		// list of x,y coordinates for placing gens from genList
		ArrayList<int[]> offsetList = new ArrayList<int[]>();
		int y = 0;
		int x = 0;
		if (columns % 2 == 0) {
			// even number of columns, number of rows may be odd or even
			// first, for y == 0, the top row
			for (x = 0; x < columns; x++) {
				genList.add(new HilbertGen(genEdge, genEdge, flipy));
				offsetList.add(new int[] { x * genEdge, y * genEdge });
				// System.out.println("--- columns even, first row");
			}
			y++;
			// now all the other rows, which repeat a two column vertical structure
			// except in the special case where there are only two rows (probably don't need special case)
			if (rows == 2) {
				for (x = columns - 1; x >= 0; x--) {
					genList.add(new HilbertGen(genEdge, genEdge, flipx));
					offsetList.add(new int[] { x * genEdge, y * genEdge });					
				}
			}
			else {
				for (x = columns - 1; x >= 0; x--) {
					if (x % 2 == 1) {
						// x is odd, column of gens goes down from second row down (1) to bottom (rows - 1)
						for (y = 1; y < rows - 1; y++) {
							genList.add(new HilbertGen(genEdge, genEdge, r270));
							offsetList.add(new int[] { x * genEdge, y * genEdge });					
							// System.out.println("--- columns even, x odd, x = "+ x +", y = "+ y);
						}
						// now y == rows - 1
						genList.add(new HilbertGen(genEdge, genEdge, flipx));
						offsetList.add(new int[] { x * genEdge, y * genEdge });
						// System.out.println("--> columns even, x odd, x = "+ x +", y = "+ y);
					}
					else {
						// x is even, column of gens goes up from bottom (rows - 1) to second row down (1)
						y = rows - 1;
						genList.add(new HilbertGen(genEdge, genEdge, flipx));
						offsetList.add(new int[] { x * genEdge, y * genEdge });
						// System.out.println("--> columns even, x even, x = "+ x +", y = "+ y);
						for (y = rows - 2; y > 0; y--) {
							//System.out.println("--- giddyup ---");
							genList.add(new HilbertGen(genEdge, genEdge, r90));
							offsetList.add(new int[] { x * genEdge, y * genEdge });					
							// System.out.println("--- columns even, x even, x = "+ x +", y = "+ y);
						}
					}
				}
			}			
		}
		else {
			// odd number of columns, number of rows must be even
			// first, for x == 0, the leftmost column
			for (y = 0; y < rows; y++) {
				genList.add(new HilbertGen(genEdge, genEdge, r270));
				offsetList.add(new int[] { x * genEdge, y * genEdge });					
			}
			x++;
			// now all the other columns, which repeat a two row horizontal structure
			// except in the special case where there are only two columns (probably don't need special case)
			if (columns == 2) {
				for (y = rows - 1; y >= 0; y--) {
					genList.add(new HilbertGen(genEdge, genEdge, flipy));
					offsetList.add(new int[] { x * genEdge, y * genEdge });					
				}
			}
			else {
				for (y = rows - 1; y >= 0; y--) {
					if (y % 2 == 1) {
						// y is odd, row of gens goes right from second column (1) to right edge (columns - 1)
						for (x = 1; x < columns - 1; x++) {
							genList.add(new HilbertGen(genEdge, genEdge, flipy));
							offsetList.add(new int[] { x * genEdge, y * genEdge });					
						}
						// now x == columns - 1
						genList.add(new HilbertGen(genEdge, genEdge, r90));
						offsetList.add(new int[] { x * genEdge, y * genEdge});
					}
					else {
						// y is even, row of gens goes left from right edge (columns - 1) to second column (1)
						x = columns - 1;
						genList.add(new HilbertGen(genEdge, genEdge, r90));
						offsetList.add(new int[] { x * genEdge, y * genEdge });
						for (x = columns - 2; x > 0; x--) {
							genList.add(new HilbertGen(genEdge, genEdge, flipx));
							offsetList.add(new int[] { x * genEdge, y * genEdge });					
						}
					}
				}
			}			
		}
		// System.out.println("--->> complete, width = "+ columns * genEdge +", height = "+ rows * genEdge);
		return new MultiGen(columns * genEdge, rows * genEdge, offsetList, genList);
	}

	/**
	 * Generates a looping fractal signal path consisting of 6 HilbertGens,
	 * arranged 3 wide and 2 tall, to fit a 3 * genW by 2 * genH image. 
	 * This particular MultiGen configuration was used so extensively in
	 * my sample code that I've given it its own method. 
	 * 
	 * Note that genH must equal genW and both must be powers of 2. For the 
	 * image size we're using in this example, genW = image width / 3 and 
	 * genH = image height / 2.
	 * 
	 * @param genW    width of each HilbertGen 
	 * @param genH    height of each HilbertGen
	 * @return a Multigen composed of 6 HilbertGens on a 3 x 2 grid
	 */
	public static MultiGen hilbertLoop3x2(int genW, int genH) {
	    // list of PixelMapGens that create a path through an image using PixelAudioMapper
		ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
		// list of x,y coordinates for placing gens from genList
		ArrayList<int[]> offsetList = new ArrayList<int[]>(); 		
		genList.add(new HilbertGen(genW, genH, fx270));
		offsetList.add(new int[] { 0, 0 });
		genList.add(new HilbertGen(genW, genH, nada));
		offsetList.add(new int[] { genW, 0 });
		genList.add(new HilbertGen(genW, genH, fx90));
		offsetList.add(new int[] { 2 * genW, 0 });
		genList.add(new HilbertGen(genW, genH, fx90));
		offsetList.add(new int[] { 2 * genW, genH });
		genList.add(new HilbertGen(genW, genH, r180));
		offsetList.add(new int[] { genW, genH });
		genList.add(new HilbertGen(genW, genH,fx270));
		offsetList.add(new int[] { 0, genH });
		return new MultiGen(3 * genW, 2 * genH, offsetList, genList);
	}

	/**
	 * This method creates a vertical stacks of rows of HilbertGens. Each row
	 * begins genH pixels down from the previous row, back at the beginning
	 * of the previous row (i.e., in "row major" order, like a bitmap). This 
	 * method pairs nicely with an image with 3 columns of with 8 rows of words,
	 * using the image as a control surface for sampling an audio file with 
	 * words recorded at the appropriate locations to match the screen order. 
	 * I used it for a performance work, DeadBodyWorkFlow, which is included
	 * in the 
	 * The signal path jumps from the end of the last gen in each row to the 
	 * beginning of the first gen int he next row. The path in each row is
	 * continuous, which provides some interesting optical effects. 
	 * 
	 * @param stacks    the number of stacks 
	 * @param rows      the number of rows in each stack
	 * @param units     the number of gens in each row
	 * @param genW      the width of each gen, a power of 2
	 * @param genH      the height of each gen, equal to genW
	 * @return          a Multigen consisting of stacks * rows * units PixelMapGens
	 */
	public static MultiGen hilbertVerticalStackOrtho(int stacks, int rows, int units, int genW, int genH) {
	    // list of PixelMapGens that create a path through an image using PixelAudioMapper
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>(); 	
	    for (int s = 0; s < stacks; s++) {
	        for (int r = 0; r < rows; r++) {
	        	int shift = s * units;
	            for (int u = 0; u < units; u++) {
	                genList.add(new HilbertGen(genW, genH));
	                offsetList.add(new int[] {(u + shift) * genW, r * genH});
	            }
	        }
	    }
	    return new MultiGen(stacks * units * genW, rows * genH, offsetList, genList);
	}

	/**
	 * This method creates a vertical stacks of rows of HilbertGens. Each row
	 * begins genH pixels down from the previous row. Alternating rows add units
	 * in opposite directions. This means path continuity is possible in each 
	 * stack by changing the orientation of the gens; however, it isn't fully 
	 * implemented in this example. Hint: choosing the right orientation for 
	 * each gen will assure path continuity. 
	 * 
	 * @param stacks    the number of stacks 
	 * @param rows      the number of rows in each stack
	 * @param units     the number of gens in each row
	 * @param genW      the width of each gen, a power of 2
	 * @param genH      the height of each gen, equal to genW
	 * @return          a Multigen consisting of stacks * rows * units PixelMapGens
	 */
	public static MultiGen hilbertVerticalStackBou(int stacks, int rows, int units, int genW, int genH) {
	    // list of PixelMapGens that create a path through an image using PixelAudioMapper
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>(); 	
	    for (int s = 0; s < stacks; s++) {
	        for (int r = 0; r < rows; r++) {
	        	int shift = s * units;
	            if (r % 2 == 1) {
	                for (int u = 0; u < units; u++) {
	                    genList.add(new HilbertGen(genW, genH, flipx));
	                    offsetList.add(new int[] {(u + shift) * genW, r * genH});
	                }
	            }
	            else {
	                for (int u = units; u > 0; u--) {
	                    genList.add(new HilbertGen(genW, genH, flipy));
	                    offsetList.add(new int[] {(u + shift - 1) * genW, r * genH});
	                }
	            }
	        }
	    }
	    return new MultiGen(stacks * units * genW, rows * genH, offsetList, genList);
	}

	// public static hilbertVerticalStackPathBou(int stacks, int rows, int units, int genW, int genH);
	
	/**
	 * This method creates rows of HilbertGens, starting each row from the left
	 * and adding gens. The odd rows are flipped vertically and the even rows are
	 * unchanged. The unchanged HilbertGen starts at upper left corner and ends at 
	 * upper right corner, so this provides some possibilities of symmetry between rows.
	 * The path is not continuous. 
	 * 
	 * @param cols    number of columns of gens wide
	 * @param rows    number of rows of gens high
	 * @param genW    width of each gen (same as genH and a power of 2)
	 * @param genH    height of each gen 
	 * @return        a MultiGen composed of cols * rows PixelMapGens
	 */
	public static MultiGen hilbertRowOrtho(int cols, int rows, int genW, int genH) {
	    // list of PixelMapGens
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>();
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				if (y % 2 == 0) {
					genList.add(new HilbertGen(genW, genH, nada));
				}
				else {
					genList.add(new HilbertGen(genW, genH, flipy));
				}
				offsetList.add(new int[] {x * genW, y * genH});
			}
		}
		return new MultiGen(cols * genW, rows * genH, offsetList, genList);
	}

	
}
