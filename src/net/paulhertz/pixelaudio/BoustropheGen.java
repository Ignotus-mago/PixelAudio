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
 * BoustropheGen generates a signal path that starts at (0,0) and reads left-to-right to (width-1, 0). 
 * The path steps down to (width-1, 1) and reads right-to-left back to (0, 1). The path alternates 
 * reading direction until it reaches the last pixel at (width-1, height-1) or (0, height-1). The name
 * is derived from the Greek "boustrophedon," a system of writing that makes an "ox turn" from one line
 * to the next. The boustrophedon signal path has better spatial coherence than the row major scanning
 * in general use for bitmaps. 
 * 
 * BoustropheGen is also a good example of how easy it can be to implement a PixelMapGen child class. 
 * 
 */
public class BoustropheGen extends PixelMapGen {
	public final static String description = "BoustropheGen starts at (0,0) and reads left-to-right on even numbered pixel scanlines and right-to-left on odd numbered scanlines.";


	public BoustropheGen(int width, int height) {
		super(width, height);
		this.generate();
	}

	public BoustropheGen(int width, int height, AffineTransformType type) {
		super(width, height, type);
		this.generate();
	}

	@Override
	public String describe() {
		return BoustropheGen.description;
	}

	@Override
	public boolean validate(int width, int height) {
		if (width < 2 || height < 2) {
			System.out.println("Width and height for BoustropheGen must be greater than 1.");
			return false;
		}
		return true;
	}

	/**
	 * Initializes this.coords, this.pixelMap, this.sampleMap: this is handled by 
	 * a call to PixelMapGen's setMapsFromCoords() method. 
	 * @return  this.pixelMap, the value for PixelAudioMapper.signalToImageLUT.
	 */
	@Override
	public int[] generate() {
		this.coords = this.generateCoordinates();
		return this.setMapsFromCoords(this.coords);
	}

	/**
	 * Generically-named method that calls the custom coordinate generation method for a particular 
	 * PixelMapGen child class. Here the method is generateBouCoordinates().
	 * Additional initializations belong here, if required by your coordinate generation method,
	 * rather than in the generate() method.
	 *
	 * @return 	An ArrayList<int[]> of bitmap coordinates in the order the signal mapping would visit them.
	 *
	 */
	private ArrayList<int[]> generateCoordinates() {
		return this.generateBouCoordinates(this.w, this.h);
	}
	
	/**
	 * The specific coordinate generation method for this class. 
	 *
	 * @param   width		width of the 2D bitmap pixel array
	 * @param   height		height of the 2D bitmap pixel array
	 * @return 				an array of coordinate pairs
	 */
	private ArrayList<int[]> generateBouCoordinates(int width, int height) {
		ArrayList<int[]> coordinates = new ArrayList<int[]>(width * height);
		for (int y = 0; y < height; y++) {
			if (y % 2 == 0) {
				for (int x = 0; x < width; x++) {
					coordinates.add(new int[] {x, y});
				}
			}
			else {
				for (int x = width - 1; x >= 0; x--) {
					coordinates.add(new int[] {x, y});
				}
			}
		}
		return coordinates;
	}
	
	
	/* ------------------------- BOUSTROPHEGEN MULTIGEN FACTORIES --------------------------
	
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
	
	/**
	 * Creates a MultiGen with rows * cols BoustropheGens. 
	 * Note that you should set values for such that:
	 * (rows * genW) == width and (cols * genH) == height.
	 * 
	 * @param cols    number of vertical columns, same as number of gens wide
	 * @param rows    number of horizontal rows, same as number of gens high
	 * @param genW    width of an individual PixelMapGen
	 * @param genH    height of an indvidual PixelMapGen
	 * @return        a MultiGen created from rows * cols PixelMapGens
	 */ 
	public static MultiGen boustrophRowRandom(int cols, int rows, int genW, int genH) {
	    // list of PixelMapGens that create an image using mapper
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>();
	    for (int y = 0; y < rows; y++) {
	        for (int x = 0; x < cols; x++) {
	            genList.add(new BoustropheGen(genW, genH, PixelMapGen.randomTransform(PixelAudio.rando())));
	            offsetList.add(new int[] {x * genW, y * genH});
	        }
	    }
	    return new MultiGen(cols * genW, rows * genH, offsetList, genList);
	}

	

}
