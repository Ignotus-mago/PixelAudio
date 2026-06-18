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
import java.util.Arrays;
import java.util.Random;

/**
 *
 * <p>Abstract class for handling coordinates and LUT generation for PixelAudioMapper. PixelAudioMapper is designed to be independent 
 * of any specific mapping between its audio and pixel arrays. It uses PixelMapGen classes as plug-ins to obtain values for its LUTs.
 * Keeping the LUT generation class outside PixelAudioMapper removes dependencies on the particular mapping.</p>
 *
 * <p> The PixelAudioMapper class handles the combinatorial math for mapping between two arrays whose elements are in one-to-one correspondence
 * but in different orders. PixelMapGen generates the mapping between the two arrays. PixelAudioMapper, as its name suggests,
 * considers one array to be floating point audio samples and other to be RGBA integer pixel data, but of course the relationship is
 * completely arbitrary as far as the mapping goes. The mapping was given its own class precisely because it is generalizable, though
 * PixelMapGen does assume that the cardinality of its arrays can be factored by width and height.</p>
 * 
 * <p>The following short program shows the typical initialization of PixelAudio classes in Processing:</p>
 *   <pre>
 *   import net.paulhertz.pixelaudio.*;
 *   
 *   PixelAudio pixelaudio;
 *   HilbertGen hGen;
 *   PixelAudioMapper mapper;
 *   PImage mapImage;
 *   int[] colors;
 *   
 *   public void setup() {
 *     size(512, 512);
 *     pixelaudio = new PixelAudio(this);
 *     hGen = new HilbertGen(width, height);
 *     mapper = new PixelAudioMapper(hGen);
 *     mapImage = createImage(width, height, RGB);
 *     mapImage.loadPixels();
 *     mapper.plantPixels(getColors(), mapImage.pixels, 0, 0, mapper.getSize());
 *     mapImage.updatePixels();
 *   }
 *   
 *   public int[] getColors() {
 *     int[] colorWheel = new int[mapper.getSize()];
 *     pushStyle();
 *     colorMode(HSB, colorWheel.length, 100, 100);
 *     int h = 0;
 *     for (int i = 0; i {@code <} colorWheel.length; i++) {
 *       colorWheel[i] = color(h, 66, 66);
 *       h++;
 *     }
 *     popStyle();
 *     return colorWheel;
 *   }
 *   
 *   public void draw() {
 *     image(mapImage, 0, 0);
 *   } 
 *   </pre>
 *   
 * <b>Creating your own PixelMapGen subclass</b>
 * <p>
 * To create your own PixelMapGen subclass, all you need to do is supply a list of 
 * non-repeating coordinate pairs that cover a grid where {@code width >= 2; height >= 2;}.
 * Of course, your subclass may impose other restrictions on width and height, or include
 * constructors that require additional arguments. Your {@code describe()} and {@code validate()} 
 * methods can provide users with information on the requirements of your subclass.
 * PixelMapGen will generate the LUTs for signal and image path from the coordinates.
 * When you initialize a {@code PixelAudioMapper} with a {@code PixelMapGen},  
 * {@code pixelMap} is copied to the {@code PixelAudioMapper.signalToImageLUT} field and
 * {@code sampleMap} is copied to the {@code PixelAudioMapper.imageToSignalLUT} field.
 * {@code pixelMap} and {@code signalToImageLUT} are the "signal map", the pixel indices 
 * of the path the mapping of signal to image follows as it traverses the pixel grid.
 * Whenever possible, please follow the convention of generating the first coordinate at (0,0). 
 * This allows for consistent behavior when coordinates and LUTs apply the transforms 
 * implemented in the BitmapTransform class. Within the body of the subclass, I suggest 
 * the following naming convention for the coordinate generating methods:
 * </p><pre>
 *  {@code @Override}
 *  {@code public int[] generate()} {...}
 *  
 *  {@code private ArrayList<int[]> generateCoordinates()} {...}
 *  
 *  {@code private ArrayList<int[]> generateYourSubclassNameCoordinates(int width, int height)} {...}
 * </pre><p>
 * See {@link DiagonalZigzagGen DiagonalZigzagGen} for an example of how each method functions in context.
 * </p>
 * 
 * 
 *
 *
 */
public abstract class PixelMapGen {
	/** width of the PixelMapGen in pixels, and by extension, width of images to which it is applied */
	public int w;
	/** height of the PixelMapGen in pixels, and by extension, height of images to which it is applied */
	public int h;
	/** the total number of pixels or audio samples handled by the PixelMapGen, == width * height */
	public int size;
	/** signalToImageLUT source for PixelAudioMapper, value at signal index returns index to pixel in bitmap */
	public int[] pixelMap;
	/** imageToSignalLUT source for PixelAudioMapper, value at bitmap index returns index to sample in signal */
	public int[] sampleMap;
	/** the 2D coordinates of the signal path as it traverses the bitmap of dimensions width * height */
	public ArrayList<int[]> coords;
	/** an <code>AffineTransformType</code> applied to the coordinate pairs in <code>coords</code> 
	 * prior to generating <code>pixelMap</code> and <code>sampleMap</code> */
	public AffineTransformType transformType = AffineTransformType.NADA;
	/** a String that summarizes the features of a PixelMapGen class */
	public final static String description = "Declare the description variable in your class and describe your PixelMapGen.";

	// short names for transforms from public enum AffineTransformType 
    /** short name for {@link AffineTransformType#R270} */
	public static AffineTransformType     r270      = AffineTransformType.R270;
    /** short name for {@link AffineTransformType#R90} */
	public static AffineTransformType     r90       = AffineTransformType.R90;
    /** short name for {@link AffineTransformType#R180} */
	public static AffineTransformType     r180      = AffineTransformType.R180;
    /** short name for {@link AffineTransformType#FLIPX} */
	public static AffineTransformType     flipx     = AffineTransformType.FLIPX;
    /** short name for {@link AffineTransformType#FX270} */
	public static AffineTransformType     fx270     = AffineTransformType.FX270;
    /** short name for {@link AffineTransformType#FX90} */
	public static AffineTransformType     fx90      = AffineTransformType.FX90;
    /** short name for {@link AffineTransformType#FLIPY} */
	public static AffineTransformType     flipy     = AffineTransformType.FLIPY;
    /** short name for {@link AffineTransformType#NADA} */
	public static AffineTransformType     nada      = AffineTransformType.NADA;
	/** transArray is useful for random transform selections. Some transforms swap width and height. */
	public static AffineTransformType[]   transArray = {r270, r90, r180, flipx, fx270, fx90, flipy, nada}; 
	/** uniformTransArray has only transforms that do not change dimensions */
	public static AffineTransformType[]   uniformTransArray = {r180, flipx, flipy, nada}; 


	/**
	 * Constructor for classes that extend PixelMapGen. You will need to create you own constructor
	 * for your class, but it can just call super(width, height) if everything it does can be handled
	 * in your generate() method. Note that generate() should be called on the last line of your constructor,
	 * after any additional initializations or calculations required for your class. See {@link DiagonalZigzagGen}
	 * and {@link HilbertGen} for examples of how to organize and initialize your own <code>PixelMapGen</code> class.
	 *
	 * @param width      width of the PixelMapGen in pixels
	 * @param height     height of the PixelMapGen in pixels
	 * @param type       an AffineTransformType to apply to the coordinates of the PixelMapGen prior to creating 
	 *                   the index maps <code>pixelMap</code> and <code>sampleMap</code> 
	 */
	public PixelMapGen(int width, int height, AffineTransformType type) {
		// TODO throw an exception instead? That's not the usual way of handling errors in Processing, AFAIK.
		if (!this.validate(width, height)) {
			System.out.println("Error: Validation failed");
			return;
		}
		this.w = width;
		this.h = height;
		this.size = h * w;
		this.transformType = type;
	}

	/**
	 * Constructor for classes that extend PixelMapGen. You will need to create you own constructor
	 * for your class, but it can just call super(width, height) if everything it does can be handled
	 * in your generate() method. Note that generate() should be called on the last line of your constructor,
	 * after any additional initializations or calculations required for your class. See {@link DiagonalZigzagGen}
	 * and {@link HilbertGen} for examples of how to organize and initialize your own <code>PixelMapGen</code> class.
	 *
	 * @param width
	 * @param height
	 */
	public PixelMapGen(int width, int height) {
		this(width, height, AffineTransformType.NADA);
	}


	/* ---------------- USER MUST SUPPLY THESE METHODS ---------------- */
	/* describe(), validate(width, height), generate() */


	/**
	 * Returns a description of the mapping and initialization requirements of a PixelMapGen class.
	 * @return 	A String describing this class's mapping and initialization requirements
	 */
	public abstract String describe();

	
	/**
	 * Validates parameters to a constructor.
	 * @param 	width
	 * @param 	height
	 * @return	true if the width and height parameters are valid for creating a mapping with this generator,
	 * 			otherwise, false.
	 */
	public abstract boolean validate(int width, int height);

	/**
	 * <p>Initialization method that sets <code>this.coords</code>, and then  <code>this.pixelMap</code> and
	 * <code>this.sampleMap</code>: <code>this.coords</code> is a list of coordinate pairs representing the signal path,
	 * the (x,y) pixel locations along a path that visits every pixel in a bitmap exactly once. Once you have created it,
	 * you can call <code>setMapsFromCoords()</code> to set <code>this.pixelMap</code> and <code>this.sampleMap</code> automatically.</p> 
	 * 
	 * <p><code>generate()</code> must be called from your class, so that you can initialize any local variables before generating 
	 * coordinates and LUTs. The best place to call it is typically on the last line of the constructor for your class, 
	 * after calling super() on the first line and after initializing any local variables needed to generate your coordinates and LUTs.
	 * You must initialize <code>this.coords</code>, <code>this.pixelMap</code>, and <code>this.sampleMap</code> within generate(). 
	 * 
	 * See {@link DiagonalZigzagGen} or {@link HilbertGen} for sample code.
	 * 
	 * @return  this.pixelMap, the value for PixelAudioMapper.signalToImageLUT. 
	 */
	public abstract int[] generate();
	
	
	/**
	 * Sets <code>this.coords</code>, <code>this.pixelMap</code> and <code>this.sampleMap</code> instance variables 
	 * from coordinates ArrayList argument. This method is provided as a convenience: all you have to do in a 
	 * child class is set the coordinates of the signal path as it steps through a bitmap of dimensions this.w * this.h. 
	 * 
	 * @param coordinates	a list of coordinate pairs representing the signal path, the (x,y) pixel locations 
	 *                      along a path that visits every pixel in a bitmap exactly once. This should be 
	 *                      created within your generate() method in your child class that extends PixelMapGen.
	 * @return the <code>pixelMap</code> value, which has already been set in this method and may be ignored
	 */
	public int[] setMapsFromCoords(ArrayList<int[]> coordinates) {
	    if (this.transformType != AffineTransformType.NADA) transformCoords(coordinates, this.transformType);
		loadIndexMaps();
		return this.pixelMap;	// return the pixelMap value, which can be ignored
	}

	/**
	 * Applies an AffineTransformType to a list of 2D coordinates. 
	 * @param coordinates    an array of coordinate pairs
	 * @param type           the AffineTransformType to apply
	 */
	public void transformCoords(ArrayList<int[]> coordinates, AffineTransformType type) {
		ArrayList<int[]> transformedCoords = new ArrayList<int[]>(coordinates.size());
		for (int[] xy : coordinates) {
			int[] newXY = BitmapTransform.coordTransform(xy[0], xy[1], w, h, type);
			// System.out.println("newXY = " + newXY[0] + ", " + newXY[1]);
			transformedCoords.add(newXY);
		}
		this.coords = transformedCoords;
		// some rotations and reflections swap width and height
		if (type == AffineTransformType.R270 || type == AffineTransformType.R90
				|| type == AffineTransformType.FX270
				|| type == AffineTransformType.FX90) {
			int temp = this.w;
			this.w = this.h;
			this.h = temp;
		}
	}

	/**
	 * Creates pixelMap and sampleMap from coords, which must already be initialized.
	 */
	public void loadIndexMaps() {
		int p = 0;										// pixelMap index, for the bitmap coordinates along the signal path
		int i = 0;										// index through the list of coordinate pairs (which we expect to be in signal order)
		this.pixelMap = new int[this.size];				// initialize this.pixelMap
		for (int[] loc : this.coords) {
			p = loc[0] + loc[1] * w;					// fill in pixelMap values using coordinate pairs, which are in signal order
			this.pixelMap[i++] = p;
		}
		this.sampleMap = new int[this.size];            // initialize this.sampleMap
		for (i = 0; i < w * h; i++) {					// set sampleMap values, inverse of pixelMap values
			this.sampleMap[this.pixelMap[i]] = i;
		}
	}


	/* ------------------------------ GETTERS AND NO SETTERS ------------------------------ */
	/* For the most part, we don't want to alter variables once they have been initialized. */

	/**
	 * @return 	Width of the bitmap associated with this PixelMapGen.
	 */
	public int getWidth() {
		return w;
	}

	/**
	 * @return 	Height of the bitmap associated with this PixelMapGen.
	 */
	public int getHeight() {
		return h;
	}

	/**
	 * @return 	Size (width * height) of the bitmap associated with this PixelMapGen.
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Returns pixelMap, the pixel array indices used to initialize PixelAudioMapper.signalToImageLUT.
	 * @return	pixelMap array, which steps through the signal and returns indices
	 *          to each pixel in the corresponding bitmap
	 */
	public int[] getPixelMap() {
		return this.pixelMap;
	}

	/**
	 * Returns a copy of pixelMap.
	 * @return	a copy of the pixelMap array
	 */
	public int[] getPixelMapCopy() {
		return Arrays.copyOf(pixelMap, size);
	}

	/**
	 * Returns sampleMap, the audio signal indices used to initialize PixelAudioMapper.imageToSignalLUT.
	 * 
	 * @return	the sampleMap array, which steps through the bitmap \
	 *          and returns indexes to each sample in the corresponding signal
	 */
	public int[] getSampleMap() {
		return this.sampleMap;
	}

	/**
	 * Returns a copy of {@code sampleMap}.
	 * @return	a copy of sampleMap
	 */
	public int[] getSampleMapCopy() {
		return Arrays.copyOf(sampleMap, size);
	}

	/**
	 * Returns the coordinates of the signal path.
	 * @return	<code>this.coords</code>, the array of coordinate pairs that mark a path 
	 *          (the "signal path") through every pixel in a bitmap. getCoordinatesCopy()
	 *          is the preferred method for obtaining the coordinates.
	 */
	public ArrayList<int[]> getCoordinates() {
		return this.coords;
	}

	/**
	 * Returns a copy of the coordinates of the signal path.
	 * @return	a copy of <code>this.coords</code>
	 */
	public ArrayList<int[]> getCoordinatesCopy() {
		ArrayList<int[]> coordsCopy = new ArrayList<>(size);
		for (int[] coord: this.coords) {
			coordsCopy.add(coord);
		}
		return coordsCopy;
	}
	
	/**
	 * Returns the AffineTransformType associated with this PixelMapGen.
	 * @return	an AffineTransformType 
	 */
	public AffineTransformType getTransformType() {
		return transformType;
	}

	/**
	 * Sets the AffineTransformType associated with this PixelMapGen and 
	 * transforms its coordinates and associated sampleMap and pixelMap fields. 
	 * 
	 * @param transformType		an AffineTransformType 
	 */
	public void setTransformType(AffineTransformType transformType) {
		this.transformType = transformType;
		this.transformCoords(this.coords, this.transformType);
		this.loadIndexMaps();
	}
	

	// ------------- STATIC METHODS FOR POWERS OF TWO ------------- //
	   
	public static boolean isPowerOfTwo(int n) {
		// n must be greater than 0 and n & (n - 1) should be 0
		return n > 0 && (n & (n - 1)) == 0;
	}
	
	public static int findPowerOfTwo(int n) {
        if (n <= 0 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("The number must be a positive power of 2. n = "+ n);
        }
        return Integer.numberOfTrailingZeros(n);
    }

    public static int findNearestPowerOfTwoLessThan(int n) {
        if (n <= 1) {
            throw new IllegalArgumentException("There is no power of 2 less than the given number.");
        }
        int p = 1;			// Start with the highest bit position
        while (p < n) {
            p <<= 1; 		// Shift left to find the next power of 2
        }
        return p >> 1; 		// Shift right to get the previous power of 2
    }
    

    // ------------- UTILITY METHODS FOR PIXELMAPS ------------- //
    
    /**
     * Given a list of coordinate pairs over an image, returns the corresponding bitmap index numbers. 
     * @param coordsList   	a list of coordinate pairs representing the (x,y) pixel locations along a path 
     *                      that visits every pixel in a bitmap
     * @param w				the width of the bitmap
     * @return				the bitmap index numbers of the coordinates
     */
    public static int[] getPixelMapFromCoordinates(ArrayList<int[]> coordsList, int w) {
		if (coordsList == null) {
			throw(new IllegalArgumentException("ERROR: The coordList argument must be non-null."));
		}
		int p = 0;
		int i = 0;
		int[] pixelMap = new int[coordsList.size()];
		for (int[] loc : coordsList) {
			p = loc[0] + loc[1] * w;
			pixelMap[i++] = p;
		}
		return pixelMap;
	}
    
    
    /**
     * Builds the inverse LUT corresponding to the supplied lookup table.
     *
     * @param pixelArr a one-to-one index mapping
     * @return an array such that inverseArr[pixelArr[i]] == i for all valid indices
     */
    public static int[] getInversMapFromPixelArray(int[] pixelArr) {
    	int[] inverseArr = new int[pixelArr.length];
		for (int i = 0; i < pixelArr.length; i++) {
			inverseArr[pixelArr[i]] = i;
		}
    	return inverseArr;
    }

    
    // ------------- RANDOM TRANSFORM ------------- //
    
	/**
	 * Returns a random transform from PixelMapGen.transArray.
	 * @param rand   a Random instance. 
	 * @return       a randomly selected transform from PixelMapGen.transArray
	 */
	public static AffineTransformType randomTransform(Random rand) {
		return PixelMapGen.transArray[rand.nextInt(PixelMapGen.transArray.length)];
	}

	/**
	 * Returns a random transform from PixelMapGen.transArray.
	 * @param rand   a Random instance. 
	 * @return       a randomly selected transform from PixelMapGen.transArray
	 */
	public static AffineTransformType randomUniformTransform(Random rand) {
		return PixelMapGen.uniformTransArray[rand.nextInt(PixelMapGen.uniformTransArray.length)];
	}

 
    
}
