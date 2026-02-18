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


import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import processing.core.PVector;


/**
 * <p>
 * As of pre-release version 0.9.2-beta, PixelAudioMapper is substantially complete, though
 * there are a number of features that have not been tested or demonstrated with code examples. 
 * </p>
 * 
 * <p>
 * PixelAudioMapper maps between 1D "signal" arrays of audio samples formatted as floating point 
 * values in the range [-1, 1] and 2D "image" arrays formatted as RGBA integer pixel data. 
 * This class is designed to handle one-to-one mappings between signal and image arrays. 
 * The mappings are managed by lookup tables (LUTs) created by a separate mapping generator class, 
 * <code>PixelMapGen</code>. The values in the LUTs are index numbers of pixels in a bitmap or 
 * of samples in a signal. If you think of the signal as a path that visits each pixel in the image, 
 * one lookup table, <code>signalToImageLUT</code>, lists the index numbers of each pixel the path 
 * visits in the image, in the order that it traverses them. There is a similar lookup table 
 * for the image, <code>imageToSignalLUT</code>, that lets you look up the signal value 
 * corresponding to each pixel in the image. For example, when you load audio samples to the 
 * signal array, <code>signalToImageLUT</code> lets you find the corresponding pixels for each sample
 * and update them to visualize the signal as a 2D image. You can save the image to a file and
 * later load it to a bitmap for display. The pixel values can then be written to an audio buffer 
 * using <code>imageToSignalLUT</code>. 
 * </p>
 * <div>
 * Some typical uses for this class include:
 *    <ul>
 * 	  <li> Reading an audio file or audio stream into the signal array and then writing its
 *		   values, transcoded to RGB integer values, to the image array for display as a visualization.</li>
 *	  <li> Using interaction with an image to trigger audio events at precise locations in a signal.</li>
 *    <li> Running audio filters on an image-as-signal and writing the results to the image.</li>
 *    <li> Running image algorithms on a signal-as-image and writing the results back to the signal.</li>
 *    <li> Synthesizing image data and audio and then animating the data while interactively
 *         triggering audio events. </li>
 *    </ul>
 * </div>
 *
 * <h2>DATA REPRESENTATION</h2>
 * <p>
 * PixelAudioMapper requires image arrays to contain standard 24- or 32-bit RGB or RGBA pixel data, in row major order,
 * with (0,0) at upper left corner. It requires signal arrays to contain values in the range [-1.0, 1.0],
 * a standard format for audio samples. 
 * </p><p>
 * For the sake of generality, the enclosing classes for image and audio data remain external to
 * PixelAudioMapper, which just works with the arrays of audio samples or image pixel data that they provide. 
 * In Processing, PImage wraps image data. You could also use Java's BufferedImage class. 
 * I have been using the minim library for audio, (https://code.compartmental.net/minim/).
 * The built-in audio in Processing 4 is definitely also an option. PImage and BufferedImage typically 
 * store color pixel data in an array of RGB or RGBA integer formatted values -- exactly what we need.
 * Audio classes use a variety of formats, particularly when reading from files, and provide methods for
 * setting and changing the format of audio sample data.  
 * </p>
 * 
 * <h3>Image</h3>
 * <pre>
 *	 Width w, Height h
 *	 Index values {0..(w * h - 1)} point into the pixel array.
 *	 Index to coordinate conversion for row major order with index i, width w, height h:
 *		i = x + w * y;
 *		x = i % w; y = i/w   // using integer math;
 *	 Default data format: 24-bit RGB or 32-bit RGBA, for display from a bitmap to a computer monitor.
 *	 RGBA includes an alpha channel A.
 * </pre>
 * <h3>Signal</h3>
 * <pre>
 *	 Array with same cardinality as image data array {0..(w * h - 1)}
 *	 Default data format: floating point values in the range  [-1.0, 1.0]
 * </pre>
 *
 * <h2>LOOKUP TABLES</h2>
 *
 * <p>
 * At their most general, lookup tables or LUTs set up a one-to-one correspondence between two arrays
 * of the same cardinality, independent of the format of their data values. Every element in one array
 * corresponds to exactly one element in the other array. Starting from array A, for an element at index
 * A[i] we find the index of the corresponding element in array B at aToBLUT[i]. An element j in array B
 * has the index of its counterpart in array A at bToALUT[j].
 * </p><p>
 * In PixelAudioMapper, we employ two LUTs, signalToImageLUT and imageToSignalLUT, to map elements in signal
 * or image to the corresponding position in image or signal.</p>
 * <pre>
 *	signalToImageLUT: integer values over {0..(h * w - 1)} map a signal array index to a pixel array index
 *	imageToSignalLUT: integer values over (0..(h * w - 1)} map an image array index to a signal array index
 * </pre><p>
 * In signalToImageLUT, we can get the pixel index in the image for any index in the signal.
 * In imageToSignalLUT, we can get index in the signal for any pixel index in the image.
 * </p>
 * <div>
 * Each array is the inverse of the other: for an array index i:
 * <pre>
 *	signalToImageLUT[imageToSignalLUT[i]] == i;
 *	imageToSignalLUT[signalToImageLUT[i]] == i;
 * </pre>
 * </div><p>
 * Image data is always in row major order for PImage, our image data class. Signal values can be mapped
 * to locations in the image in any arbitrary order, as long their coordinates traverse the entire image.
 * A typical reordering might be a zigzag from upper left to lower right of an image, or a space-filling
 * fractal, or even a randomly shuffled order. The coordinates of each pixel in the image are stored as
 * indices (i = x + w * y) in signalToImageLUT.
 * </p><p>
 * Once we know the “pixel index” for each value in the signal and have initialized signalToImageLUT,
 * we can initialize imageToSignalLUT:</p>
 *	<pre>
 *	for (int i = 0; i < w * h - 1; i++) {
 *		imageToSignalLUT[signalToImageLUT[i]] = i;
 *	}
 *  </pre>
 * <p>
 * The LUTs are generated by a subclass of <code>PixelMapGen</code> that is passed as an argument to the <code>PixelAudioMapper</code> constructor. 
 * Each <code>PixelMapGen</code> subclass generates: 1. a set of coordinates for the path traced by the signal over the image, 
 * 2. <code>pixelMap</code> for mapping from signal to image (<code>signalToImageLUT</code> in <code>PixelAudioMapper</code>), 
 * and 3. <code>sampleMap</code> (<code>imageToSignalLUT</code> in <code>PixelAudioMapper</code>), for mapping from image to signal
 * <code>PixelAudioMapper</code> works with copies of the two LUTs, and can access or obtain a copy of the coordinates if needed. 
 * This strategy allows the generator classes to be compact and reusable, while the host class, PixelAudioMapper, can handle 
 * exchanges between audio and pixel data using its copies of the LUTs. Note the the pixel array and the signal array length 
 * must equal the image size = width * height.  
 * </p><p>
 * To work with PixelAudioMapper, first create a PixMapGen instance with the width and height of the image you are addressing. 
 * The PixMapGen instance will generate the LUTs for its particular mapping for you. You can then pass it to the
 * PixelAudioMapper constructor, which will initialize its variables from copies of the PixMapGen LUTs.
 * Some of the logic behind this process is explained in my notes to the PixMapGen abstract class.
 * </p>
 *
 * <h2>MAPPING AND TRANSCODING</h2>
 * <p>
 * We typically use the LUTs whenever we change the data in the signal or the image and want to write
 * the new values to its counterpart, updating the appearance of the image or the sound of the audio signal.
 * If the values in the arrays are in different formats, we will need to transcode the values from one
 * format to the other. We have two methods, in pseudocode here:</p>
 * <pre>
 *	mapSigToImg		map signal values to the image: img[i] = transcode(sig[imgLUT[i]]);
 *	mapImgToSig		map image values to the signal: sig[i] = transcode(img[sigLUT[i]]);
 * </pre><p>
 * The img variable in the pseudocode corresponds to an array of RGB data from a bitmap class.
 * The sig variable corresponds to an array of floating point samples from an audio class.
 * </p><p>
 * In addition, we can write image or signal values directly, without using the LUTs. This operation transforms
 * the order of the pixel or signal values.</p>
 * <pre>
 * 	writeImgToSig	write image values directly to the signal: sig[i] = transcode(img[i]);
 *	writeSigToImg	write signal values directly to the image: img[i] = transcode(sig[i]);
 * </pre>
 *
 * <h2>READING AND WRITING SUBARRAYS</h2> // TODO rewrite this section
 * <p>
 * When we want to work with subarrays of data from the signal or the image, it can be ordered either
 * by the signal or image array order or by mapping with the corresponding LUT. In the case of images,
 * we also have standard methods of reading and writing rectangular selections. We can define some
 * methods to read and write data either in the order determined by the signal or by rectangular
 * areas in the image. We’ll call the signal order methods pluck (read) and plant (write), and the pixel order
 * methods peel (read) and stamp (write). 
 * </p><p>
 * Arguments to mapping and writing methods are written so that source precedes target. Using this convention,
 * most methods have a unique signature that also indicates how they function. Where there are ambiguities or
 * a need for clarification, I have renamed the function, as in pluckPixelsAsFloat, pluckSamplesAsInt,
 * peelPixelsAsFloat, and peelSamplesAsInt.
 * </p>
 * 
 *  
 * <h2>ARRAY SHIFTING</h2>
 * <p>
 * Standard operations we can perform with the signal array:</p>
 * <pre>
 *   shiftLeft()    an array rotation where array values move left, arr[0] = arr[1], arr[1] = arr[2] ... arr[n-1] = arr[0], etc.
 *   shiftRight()   an array rotation array values move right, arr[0] = arr[n-1], arr[1] = arr[0] ... arr[n-1] = arr[n - 2], etc.
 * </pre><p>
 * Shifting has proved so useful for animation that I am including it in the class. The shift methods also demonstrate
 * how to update the signal and pixel arrays. The <code>WaveSynth</code> class provides other methods for animation.
 * </p>
 *
 * <h2>OTHER OPERATIONS</h2>
 * <p>
 * The following are suggestions for methods that could be implemented using PixelArrayMapper.</p>
 * <ul>
 *	 <li>additive audio synthesis + color organ, implemented with the WaveSynth and WaveData classes</li>
 *	 <li>granular synthesis (AriaDemoApp is not quite GS, but very similar)</li>
 *	 <li>pattern generation (Argosy and Lindenmeyer classes)</li>
 * 	 <li>phase shifting, amplitude modulation, etc.  </li>
 *	 <li>FFT operations on both image and signal data (AriaDemoApp) </li>
 *	 <li>pixel sorting, typically on image data</li>
 *	 <li>blur, sharpen, etc.</li>
 *	 <li>blending images</li>
 *	 <li>mixing signals</li>
 * </ul>
 *
 * <h2>UPDATING AUDIO AND IMAGE</h2>
 * <p>
 * As a rule, operations on the signal should be followed by writing to the image, and operations
 * on the image should be followed by writing to the signal. This will keep the values synchronized,
 * even though they have different numerical formats.
 * </p><p>
 * In most of the examples that accompany this library, audio data uses the Lightness channel of an
 * HSL representation of the image's  RGB data, but this is by no means the only way of doing things.
 * Using the Lightness channel  restricts audio data to 8 bits, apt for glitch esthetics, but noisy.
 * It's also possible to  maintain high resolution data in the signal by processing image and audio
 * data separately, and  writing audio data to the image but not in the other direction.
 * </p><p>
 * Finally, it bears mentioning that the image can be treated as simply an interface into an audio
 * buffer, where events such as mouse clicks or drawing and animation trigger audio events but do not
 * modify the audio buffer. Library examples will provide some suggestions for this strategy.
 * </p>
 *
 * 
 * <h3>Note: Representing ranges</h3>
 * <p>I am following the convention from mathematical notation of representing closed ranges with [] and open ranges with ().
 * I occasionally lapse into the alternate notations {0..(h * w - 1)} or [0, 255]. When values are integers, it should be 
 * understood that the range covers only integer values. Floating point values are continuous, to the limits of digital computation. 
 * </p>
 * 
 * @see PixelMapGen
 * @see WaveSynth
 * 
 * Refactored 15 June 2025
 * Refactored all pluck, plant, peel, stamp, pull and push methods: 
 * 1. wrote loops that call a switch over color channels, a design which JIT can optimize; 
 * 2. included error checking in all pluck, plant, peel, stamp, pull and push methods ; 
 * 3. added helper methods. 
 * Some of the pluck and plant methods throw an error if arrays do not conform to PixelAudioMapper dimensions. 
 * All of the peel and stamp methods must conform. 
 * Pull and push methods only require arrays be of the same size, 
 * and modify the destination array if it is null or the wrong size. 
 * 
 */
public class PixelAudioMapper {
	// necessary instance variables
	/** image width */
	protected int width;
	/** image height */
	protected int height;
	/** pixel array and signal array length, equal to w * h */
	protected int mapSize;
	/** Lookup table to go from the signal to the image: index values over {0..(h * w - 1)}
	 * point to a corresponding index position in the image array img.pixels[] */
	protected int signalToImageLUT[];
	/** Lookup table to go from the image to the signal: index values over {0..(h * w - 1)}
	 * point to a corresponding index position in the signal array sig[] */
	protected int imageToSignalLUT[];
	/** PixelMapGenINF instance to generate LUTs */
	protected PixelMapGen generator;
	/** container for HSB pixel values */
	//private float[] hsbPixel = new float[3];

	/** 
	 * List of available color channels, "L" for lightness, since "B" for brightness is taken.
	 * The expanded enum type allows me to provide some additional information, including extraction
	 * methods for each channel. These all require a float[3] hsbPixel argument. Because of the 
	 * overhead involved in allocation of the hsbPixel array, callers are advised to make it 
	 * reusable for tight loops or to use the other extraction methods in the PixelAudioMapper class.
	 * At the moment, I regard the extraction methods as experimental, but they may be worth 
	 * extending in the future, to have a single entry point for all color channel operations. 
	 */
	public enum ChannelNames {
	    R("Red", 0, (rgb, hsb) -> (float)((rgb >> 16) & 0xFF)),
	    G("Green", 1, (rgb, hsb) -> (float)((rgb >> 8) & 0xFF)),
	    B("Blue", 2, (rgb, hsb) -> (float)(rgb & 0xFF)),
	    H("Hue", 0, (rgb, hsb) -> {
	        java.awt.Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
	        return hsb[0];
	    }),
	    S("Saturation", 1, (rgb, hsb) -> {
	        java.awt.Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
	        return hsb[1];
	    }),
	    L("Brightness", 2, (rgb, hsb) -> {
	        java.awt.Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
	        return hsb[2];
	    }),
	    A("Alpha", 3, (rgb, hsb) -> (float)((rgb >> 24) & 0xFF)),
	    ALL("All channels", -1, (rgb, hsb) -> 0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF));

	    private final String displayName;
	    private final int index;
	    private final ChannelExtractor extractor;

	    @FunctionalInterface
	    public interface ChannelExtractor {
	        float extract(int rgb, float[] hsbPixel);
	    }

	    ChannelNames(String displayName, int index, ChannelExtractor extractor) {
	        this.displayName = displayName;
	        this.index = index;
	        this.extractor = extractor;
	    }

	    public String getDisplayName() {
	        return displayName;
	    }

	    public int getIndex() {
	        return index;
	    }

	    public float extract(int rgb, float[] hsbPixel) {
	        return extractor.extract(rgb, hsbPixel);
	    }
	}


	/**
	 * Basic constructor for PixelAudio, sets up all variables.
	 * @param gen 	A PixelMapGenINF instance -- should be initialized already.
	 */
	public PixelAudioMapper(PixelMapGen gen) {
		this.setGenerator(gen);
	}


	//------------- Dimensions -------------//

	/** @return the width of the image */
	public int getWidth() {
		return this.width;
	}

	/** @return the height of the image */
	public int getHeight() {
		return this.height;
	}

	/** @return the length of the signal array (== length of image pixel array and the LUTs) */
	public int getSize() {
		return this.mapSize;
	}

	/** @return a string representation of our data, possibly partial */
	@Override
	public String toString() {
		return "Parent class for PixelAudioMapper objects, please see the documentation in its comments.";
	}


	//------------- LUTs -------------//

	/** @return a copy of the lookup table that maps an index in the signal to the corresponding pixel index in the image.
	 * TODO possibly change the name of this method to getSignalToImageLUTSnapshot */
	public int[] getSignalToImageLUT() {
		// return this.signalToImageLUT;    // not a copy
		return generator.getPixelMapCopy();
	}

	/**
	 * Sets a new lookup table for mapping signal to image. Probably something to avoid: plug in a new PixelMapGen instead.
	 * Warning: The size of sigLUT must conform to the size the current image and signal arrays.
	 * @param sigLUT
	 */
	protected void setSignalToImageLUT(int[] sigLUT) {
		if (sigLUT.length != this.signalToImageLUT.length) 
			throw(new IllegalArgumentException("The new signalToImageLUT array must be the same size as the old signalToImageLUT array."));
		this.signalToImageLUT = sigLUT;
	}

	/** @return a copy of the lookup table that maps pixel values in the image to the corresponding entry in the signal.
	 * TODO possibly change the name of this method to getImageToSignalLUTSnapshot */
	public int[] getImageToSignalLUT() {
		// return this.imageToSignalLUT;    // not a copy
		return generator.getSampleMapCopy();
	}

	/**
	 * Sets a new lookup table for mapping image to signal. Probably something to avoid: plug in a new PixelMapGen instead.
	 * Warning: the size of imgLUT must conform to the size the current image and signal arrays.
	 * @param imgLUT
	 */
	protected void setImageToSignalLUT(int[] imgLUT) {
		if (imgLUT.length != this.imageToSignalLUT.length) 
			throw(new IllegalArgumentException("The new imageToSignalLUT array must be the same size as the old imageToSignalLUT array."));
		this.imageToSignalLUT = imgLUT;
	}

	/**
	 * @return    a copy of the coordinates of the signal path from the PixelMapGen <code>generator</code>.
	 */
	public ArrayList<int[]> getGeneratorCoordinatesCopy() {
		return this.generator.getCoordinatesCopy();
	}
	
	/**
	 * @return    the descriptive string associated with the PixelMapGen <code>generator</code>
	 */
	public String getGeneratorDescription() {
		return this.generator.describe();
	}

	/**
	 * @return    the PixelMapGen <code>generator</code> for this PixelAudioMapper
	 */
	public PixelMapGen getGenerator() {
		return this.generator;
	}
	
	/**
	 * Sets the PixelMapGen <code>generator</code> for this PixelAudioMapper.
	 * @param newGen    a new PixelMapGen
	 */
	public void setGenerator(PixelMapGen newGen) {
		this.generator = newGen;
		this.regenerate();
	}
	
	/**
	 * Calls PixelMapGen <code>generator</code> to create coordinates and LUTs.
	 */
	public void regenerate() {
		this.width = generator.getWidth();
		this.height = generator.getHeight();
		this.mapSize = generator.getSize();
		this.signalToImageLUT = generator.getPixelMapCopy();
		this.imageToSignalLUT = generator.getSampleMapCopy();
	}

	/**
	 * Call only on arrays containing the values 0..array.length-1, which can be used to 
	 * reorder bitmaps. The array returned will restore the order. 
	 */
	public int[] getInverseArray(int[] src) {
		int[] dest = new int[src.length];
		for (int i = 0; i < width * height - 1; i++) {
			dest[src[i]] = i;
		}
		return dest;
	}
	
	
	//------------- TRANSCODING -------------//
	
	/*
	 * This section contains optimized math for conversion between color and audio formats.
	 * It assumes that RGB values are 8-bit numbers in three or four channels, RGB or RGBA. 
	 * The color channel values handled here are thus in the range [0, 255]. HSB values (brightness, 
	 * hue, saturation) are constrained to [0.0f,1.0f]. Audio values range over [-1.0f, 1.0f]. 
	 * Note that input values are not checked or clamped to the desired interval. That puts the 
	 * burden of normalizing values on the caller before the call. 
	 * 
	 * If you are using a different color depth per channel, such as 16-bit, you can override 
	 * these methods by subclassing PixelAudioMapper. The thread-safe AudioColorTranscoder class 
	 * provides methods that do limit input ranges and are generalized for any ranges. 
	 * AudioColorTranscoder is an experiment that *may* be incorporated here later on. 
	 * 
	 */
	

	/**
	 * Converts a float value in the range [-1.0, 1.0] to an int value in the range
	 * [0, 255]. Using 127.5f and 0.5f as values works around a call to
	 * Math.round(), saving a few cycles.
	 *
	 * @param val    a float value in the range [-1.0, 1.0]
	 * @return       an int mapped to the range [0, 255]
	 */
	public static int audioToRGBChan(float val) {
		return (int) (127.5f * (val + 1.0f) + 0.5f);
	}

	/**
	 * Converts an int value in the range [0..255] to a float value in the range [-1.0, 1.0].
	 *
	 * @param val     an int in the range [0..255]
	 * @return        a float mapped to the range [-1.0, 1.0]
	 */
	public static float rgbChanToAudio(int val) {
		return (val / 127.5f) - 1.0f;
	}

	/**
	 * Converts a float value in the range [0, 255] to a float value in the range [-1.0, 1.0].
	 *
	 * @param val    a float in the range [0.0, 255.0], RGB channel value as a float
	 * @return       a float mapped to the range [-1.0, 1.0]
	 */
	public static float rgbFloatToAudio(float val) {
		return (val / 127.5f) - 1.0f;
	}

	/**
	 * Converts a float value in the range [0, 1] to a float value in the range
	 * [-1.0, 1.0].
	 *
	 * @param val a float in the range [0..1] that represents an HSB component
	 * @return a float mapped to the range [-1.0, 1.0]
	 */
	public static float hsbFloatToAudio(float val) {
		return 2.0f * val - 1.0f;
	}	
	
	/**
	 * Converts a float value in the range [-1, 1] to a float value in the range
	 * [0, 1.0].
	 *
	 * @param val a float in the range [-1..1] that typically represents an audio sample
	 * @return a float mapped to the range [0, 1.0], typically used in the HSB color space
	 */
	public static float audioToHSBFloat(float val) {
		return (val + 1.0f) / 2.0f;
	}	
	

	// ------------------------------------------------------------
	// Utility
	// ------------------------------------------------------------

	/** Wrap idx into [0, n). Handles negative idx too. */
	public static int wrap(int idx, int n) {
	    if (n <= 0) return 0;
	    idx %= n;
	    if (idx < 0) idx += n;
	    return idx;
	}

	// ------------------------------------------------------------
	// Canonical lookup (NO shift)
	// ------------------------------------------------------------

	/**
	 * Given image coordinates (x,y), returns the corresponding signal-path index
	 * using imageToSignalLUT. Returns an index in [0, getSize()).
	 */
	public int lookupSignalPos(int x, int y) {
	    // (Optional safety, if you want)
	    // if (x < 0 || x >= width || y < 0 || y >= height) throw new IndexOutOfBoundsException(...);
	    return this.imageToSignalLUT[x + y * this.width];
	}

	/**
	 * Given imagePos (index into img.pixels), returns the corresponding signal-path index
	 * using imageToSignalLUT. Returns an index in [0, getSize()).
	 */
	public int lookupSignalPos(int imagePos) {
	    return this.imageToSignalLUT[imagePos];
	}

	/**
	 * Given a list of image-space points (x,y), returns corresponding signal-path indices.
	 */
	public int[] lookupSignalPosArray(List<PVector> points) {
	    if (points == null || points.isEmpty())
	        throw new IllegalArgumentException("Points list must be non-null and non-empty");

	    int[] indices = new int[points.size()];
	    int i = 0;
	    for (PVector vec : points) {
	        indices[i++] = lookupSignalPos((int) vec.x, (int) vec.y);
	    }
	    return indices;
	}

	/**
	 * Same as lookupSignalPosArray(points), but also applies a caller-provided offset and
	 * wraps to wrapLen (often wrapLen == getSize()).
	 *
	 * This method is intentionally "application-ish" (you already had it) and does NOT
	 * assume wrapLen == getSize().
	 */
	public int[] lookupSignalPosArray(List<PVector> points, int offset, int wrapLen) {
	    if (points == null || points.isEmpty())
	        throw new IllegalArgumentException("Points list must be non-null and non-empty");
	    if (wrapLen <= 0)
	        throw new IllegalArgumentException("wrapLen must be > 0");

	    int[] indices = new int[points.size()];
	    int i = 0;
	    for (PVector vec : points) {
	        int base = lookupSignalPos((int) vec.x, (int) vec.y) + offset;
	        indices[i++] = wrap(base, wrapLen);
	    }
	    return indices;
	}

	/**
	 * Given a signal-path index (NOT a sample value), returns the corresponding imagePos
	 * using signalToImageLUT. Expects signalPos in [0, getSize()).
	 */
	public int lookupImagePos(int signalPos) {
	    return this.signalToImageLUT[signalPos];
	}

	/**
	 * Given a signal-path index (NOT a sample value), returns image coordinates (x,y).
	 */
	public int[] lookupImageCoord(int signalPos) {
	    int imagePos = this.signalToImageLUT[signalPos];
	    int x = imagePos % this.width;
	    int y = imagePos / this.width;
	    return new int[] { x, y };
	}

	// ------------------------------------------------------------
	// Shifted lookup (signal-path rotation by totalShift)
	// ------------------------------------------------------------

	/**
	 * Display pixel (x,y) -> BUFFER signal index, assuming the display was generated
	 * by reading samples at (pathIndex + totalShift).
	 *
	 * That is: display at path position i shows sample index wrap(i + totalShift, N).
	 *
	 * Therefore click inversion is: bufferIndex = wrap(displayPathIndex + totalShift, N).
	 */
	public int lookupSignalPosShifted(int x, int y, int totalShift) {
	    final int n = getSize();
	    final int dispPathIndex = lookupSignalPos(x, y);            // i
	    return wrap(dispPathIndex + totalShift, n);                 // i + shift
	}

	public int lookupSignalPosShifted(int imagePos, int totalShift) {
	    final int n = getSize();
	    final int dispPathIndex = lookupSignalPos(imagePos);
	    return wrap(dispPathIndex + totalShift, n);
	}

	public int[] lookupSignalPosArrayShifted(List<PVector> points, int totalShift) {
	    int[] disp = lookupSignalPosArray(points);                  // path indices i
	    final int n = getSize();
	    final int s = wrap(totalShift, n);
	    for (int i = 0; i < disp.length; i++) {
	        disp[i] = wrap(disp[i] + s, n);
	    }
	    return disp;
	}

	/**
	 * Shifted version of lookupSignalPosArray(points, offset, wrapLen).
	 * Preserves your existing offset/wrapLen semantics, then applies totalShift
	 * as an additional rotation along the path, wrapping in the same wrapLen.
	 */
	public int[] lookupSignalPosArrayShifted(List<PVector> points, int offset, int wrapLen, int totalShift) {
	    int[] disp = lookupSignalPosArray(points, offset, wrapLen);
	    final int s = wrap(totalShift, wrapLen);
	    for (int i = 0; i < disp.length; i++) {
	        disp[i] = wrap(disp[i] + s, wrapLen);
	    }
	    return disp;
	}

	/**
	 * BUFFER signal index -> display imagePos, inverse of lookupSignalPosShifted(...).
	 *
	 * Since display uses sampleIndex = wrap(pathIndex + shift),
	 * the sample at BUFFER index k appears at pathIndex wrap(k - shift).
	 */
	public int lookupImagePosShifted(int signalPos, int totalShift) {
	    final int n = getSize();
	    final int dispPathIndex = wrap(signalPos - totalShift, n);
	    return lookupImagePos(dispPathIndex);
	}

	public int[] lookupImageCoordShifted(int signalPos, int totalShift) {
	    int imagePos = lookupImagePosShifted(signalPos, totalShift);
	    int x = imagePos % this.width;
	    int y = imagePos / this.width;
	    return new int[] { x, y };
	}



	//------------- MAPPING -------------//
	
	/*
	 * These methods reorder arrays of audio or color values using the LUTs. 
	 */

	/**
	 * Creates an array of int which contains the values in <code>img</code> reordered by the lookup table <code>lut</code>.
	 * The two arrays, img and lut, must be the same size.
	 * 
	 * @param img    an array of int, typically of RGB values
	 * @param lut    a look up table of the same size as <code>img</code>
	 * @return       a new array of int with the values in img reordered by the lookup table
	 * @throw IllegalArgumentException if img.length != lut.length
	 */
	public static int[] remapPixels(int[] img, int[] lut) {
		if (img.length != lut.length) throw new IllegalArgumentException("img and lut arrays must be the same size");
		int[] newPixels = new int[img.length];
		for (int i = 0; i < img.length; i++) {
			newPixels[i] = img[lut[i]];
		}
		return newPixels;
	}

	/**
	 * Creates an array of float which contains the values in <code>sig</code> reordered by the lookup table <code>lut</code>.
	 * The two arrays, sig and lut, must be the same size.
	 * 
	 * @param sig    an array of float, typically audio samples
	 * @param lut	 a lookup table
	 * @return       a new array with the values in sig reordered by the lookup table
	 * @throw IllegalArgumentException if sig.length != lut.length
	 */
	public static float[] remapSamples(float[] sig, int[] lut) {
		if (sig.length != lut.length) throw new IllegalArgumentException("sig and lut arrays must be the same size");
		float[] newSignal = new float[sig.length];
		for (int i = 0; i < sig.length; i++) {
			newSignal[i] = sig[lut[i]];
		}
		return newSignal;
	}
	
	/**
	 * Map signal values to the image using all channels (effectively, grayscale).
	 * On completion, img[] contains new values. The img array and the sig array
	 * must be the same size.
	 *
	 * @param sig    source array of floats in the audio range  [-1.0, 1.0]
	 * @param img    target array of RGB pixel values
	 * @return array of RGB int values derived from sig, loaded to all channels (audio as grayscale)
	 * @throw IllegalArgumentException if sig.length != img.length
	 */
	public int[] mapSigToImg(float[] sig, int[] img) {
		if (sig.length != img.length) throw new IllegalArgumentException("sig and img arrays must be the same size");
		return PixelAudioMapper.pushAudioToChannel(sig, img, this.signalToImageLUT, ChannelNames.ALL);    // audio to grayscale conversion
	}

	/**
	 * Map signal values to a specified channel in the image using imageToSignalLUT.
	 * On completion, img[] contains new values, transcoded from the signal.
	 * The img array and the sig array must be the same size.
	 *
	 * @param sig			an array of floats in the audio range  [-1.0, 1.0]
	 * @param img			an array of RGB pixel values
	 * @param toChannel		the channel to write transcoded values to
	 * @return array of RGB int values derived from sig, loaded to specified channel
	 * @throw IllegalArgumentException if sig.length != img.length
	 */
	public int[] mapSigToImg(float[] sig, int[] img, ChannelNames toChannel) {
	    if (sig == null) throw new IllegalArgumentException("sig cannot be null");
	    if (img == null) throw new IllegalArgumentException("img cannot be null");
	    if (sig.length != img.length)
	        throw new IllegalArgumentException("sig and img arrays must be the same size");
	    if (sig.length != this.getSize())
	        throw new IllegalArgumentException("sig and img array lengths must equal mapper.getSize()");
	    return PixelAudioMapper.pushAudioToChannel(sig, img, this.signalToImageLUT, toChannel);
	}

	/**
	 * Shifted version: rotates along the signal path by totalShift without rotating
	 * the audio buffer or raster pixel array.
	 */
	public int[] mapSigToImgShifted(float[] sig, int[] img, ChannelNames toChannel, int totalShift) {
	    if (sig == null) throw new IllegalArgumentException("sig cannot be null");
	    if (img == null) throw new IllegalArgumentException("img cannot be null");
	    if (sig.length != img.length)
	        throw new IllegalArgumentException("sig and img arrays must be the same size");
	    if (sig.length != this.getSize())
	        throw new IllegalArgumentException("sig and img array lengths must equal mapper.getSize()");
	    return PixelAudioMapper.pushAudioToChannelShifted(sig, img, this.signalToImageLUT, toChannel, totalShift);
	}	
	
	/**
	 * Map current image pixel values to the signal, updating the signal array.
	 * There are several ways to derive an audio value from the image: we use
	 * the brightness channel in the HSB color space. On completion, sig[] contains new values.
	 * The img array and the sig array must be the same size.
	 *
	 * @param sig	an array of floats in the audio range  [-1.0, 1.0]
	 * @param img	an array of RGB pixel values
	 * @return array of audio range float values derived from Brightness channel of color values
	 * @throw IllegalArgumentException if img.length != sig.length
	 */
	public float[] mapImgToSig(int[] img, float[] sig) {
		if (sig.length != img.length) throw new IllegalArgumentException("sig and img arrays must be the same size");
		if (sig.length != this.getSize()) throw new IllegalArgumentException("sig and img array lengths must equal mapper.getSize()");
		float[] hsbPixel = new float[3];
		return PixelAudioMapper.pullPixelAsAudio(img, sig, this.signalToImageLUT, ChannelNames.L, hsbPixel);
	 }

	/**
	 * Map current image pixel values to the signal, updating the signal array, deriving
	 * a value from specified color channel of the image. On completion, sig[] contains new values.
	 * The img array and the sig array must be the same size.
	 *
	 * @param sig			an array of floats in the audio range [-1.0, 1.0]
	 * @param img			an array of RGB pixel values
	 * @param fromChannel	the color channel to get a value from
	 * @param hsbPixel      a float[3] array for use with color channel extraction
	 * @return array of audio range float values derived from specified channel of color values
	 * @throw IllegalArgumentException if img.length != sig.length
	 */
	public float[] mapImgToSig(int[] img, float[] sig, ChannelNames fromChannel) {
		if (img.length != sig.length) throw new IllegalArgumentException("img and sig arrays must be the same size");
		float[] hsbPixel = new float[3];
		return PixelAudioMapper.pullPixelAsAudio(img, sig, this.signalToImageLUT, fromChannel, hsbPixel);
	 }
	
	public float[] mapImgToSigShifted(int[] img, float[] sig, int totalShift) {
	    if (sig.length != img.length) throw new IllegalArgumentException("sig and img arrays must be the same size");
	    if (sig.length != this.getSize()) throw new IllegalArgumentException("sig and img array lengths must equal mapper.getSize()");
	    float[] hsbPixel = new float[3];
	    return PixelAudioMapper.pullPixelAsAudioShifted(img, sig, this.signalToImageLUT, ChannelNames.L, hsbPixel, totalShift);
	}

	public float[] mapImgToSigShifted(int[] img, float[] sig, ChannelNames fromChannel, int totalShift) {
	    if (img.length != sig.length) throw new IllegalArgumentException("img and sig arrays must be the same size");
	    float[] hsbPixel = new float[3];
	    return PixelAudioMapper.pullPixelAsAudioShifted(img, sig, this.signalToImageLUT, fromChannel, hsbPixel, totalShift);
	}	

	/**
	 * Writes transcoded pixel values directly to the signal, without using a LUT to redirect.
	 * Values are calculated with the standard luminosity equation, <code>gray = 0.3 * red + 0.59 * green + 0.11 * blue</code>.
	 *
	 * @param img		source array of RGB pixel values
	 * @param sig		target array of audio samples in the range [-1.0, 1.0]
	 * @throw IllegalArgumentException if img.length != sig.length
	 */
	public void writeImgToSig(int[] img, float[] sig) {
		if (img.length != sig.length) throw new IllegalArgumentException("img and sig arrays must be the same size");
		float[] hsbPixel = new float[3];
		PixelAudioMapper.pullPixelAsAudio(img, sig, ChannelNames.ALL, hsbPixel);
	 }

	/**
	 * @param img			an array of RGB pixel values, source
	 * @param sig			an array of audio samples in the range [-1.0, 1.0], target
	 * @param fromChannel	channel in RGB or HSB color space, from ChannelNames enum
	 * @throw IllegalArgumentException if img.length != sig.length
	 */
	public void writeImgToSig(int[] img, float[] sig, ChannelNames fromChannel) {
		if (img.length != sig.length) throw new IllegalArgumentException("img and sig arrays must be the same size");
		float[] hsbPixel = new float[3];
		PixelAudioMapper.pullPixelAsAudio(img, sig, fromChannel, hsbPixel);
	 }

	/**
	 * @param sig		an array of audio samples in the range [-1.0, 1.0], source
	 * @param img		an array of RGB pixel values, target
	 * @throw IllegalArgumentException if sig.length != img.length
	 */
	public void writeSigToImg(float[] sig, int[] img) {
		if (sig.length != img.length) throw new IllegalArgumentException("sig and img arrays must be the same size");
		PixelAudioMapper.pushAudioToPixel(sig, img, ChannelNames.ALL);
	 }

	 /**
	 * @param sig			an array of audio samples in the range [-1.0, 1.0], source
	 * @param img			an array of RGB pixel values, target
	 * @param toChannel		channel in RGB or HSB color space, from ChannelNames enum
	 * @throw IllegalArgumentException if sig.length != img.length
	 */
	public void writeSigToImg(float[] sig, int[] img, ChannelNames toChannel) {
		if (sig.length != img.length) throw new IllegalArgumentException("sig and img arrays must be the same size");
		 PixelAudioMapper.pushAudioToPixel(sig, img, toChannel);
	 }

		
	/**
	 * Copies pixels from srcPixels to dstPixels along the signal path with a path shift.
	 * At path index i, dst gets src from path index wrap(i + totalShift, N). 
	 * If used as an animation repeatedly adding a short shift > 0, each destination path position i 
	 * reads from a later path position i+shift. Visually, content appears to move “backward” 
	 * along the path (because you’re pulling future content into current positions). 
	 *
	 * srcPixels and dstPixels are raster-order arrays (e.g. PImage.pixels).
	 */
	public void copyPixelsAlongPathShifted(int[] srcPixels, int[] dstPixels, int totalShift) {
	    if (srcPixels == null || dstPixels == null)
	        throw new IllegalArgumentException("srcPixels and dstPixels must be non-null");
	    int n = getSize();
	    if (srcPixels.length != n || dstPixels.length != n)
	        throw new IllegalArgumentException("srcPixels and dstPixels must have length mapper.getSize()");
	    final int shift = wrap(totalShift, n);
	    final int[] lut = this.signalToImageLUT;
	    for (int i = 0; i < n; i++) {
	        int si = i + shift;
	        if (si >= n) si -= n;
	        dstPixels[lut[i]] = srcPixels[lut[si]];
	    }
	}

	
	public void copyPixelsAlongPathShifted(int[] srcPixels, int[] dstPixels, int signalPos, int length, int totalShift) {
	    if (srcPixels == null || dstPixels == null)
	        throw new IllegalArgumentException("srcPixels and dstPixels must be non-null");
	    int n = getSize();
	    if (srcPixels.length != n || dstPixels.length != n)
	        throw new IllegalArgumentException("srcPixels and dstPixels must have length mapper.getSize()");
	    if (signalPos < 0 || signalPos >= n) throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > n) throw new IllegalArgumentException("Invalid length");
	    final int shift = wrap(totalShift, n);
	    final int[] lut = this.signalToImageLUT;
	    for (int j = 0; j < length; j++) {
	        int i = signalPos + j;
	        int si = i + shift;
	        if (si >= n) si -= n;
	        dstPixels[lut[i]] = srcPixels[lut[si]];
	    }
	}

	

	
	//------------- SUBARRAYS -------------//

	/*
	 * In each case, a source subarray is either extracted from or inserted into a larger target array.
	 * When the small array, sprout, is inserted, it is indexed from 0..sprout.length. The larger array,
	 * img or sig, is indexed from read or write point signalPos to signalPos + length.
	 *
	 * All float[] arrays should contain audio range values [-1.0, 1.0].
	 * All int[] arrays should contain RGB pixel values.
	 * Array lengths must equal this.mapSize. 
	 * 
	 * Pluck and Plant methods follow the signal path.
	 * Pluck methods read data from int[] or float[] arrays and return transcoded data.
	 * Plant methods write transcoded data from int[] or float[] arrays to int[] or float arrays[].
	 * Peel and Stamp methods operate on rectangular areas following the image path. 
	 * TODO more better explanations.
	 * 
	 * Where I use signalToImageLUT or imageToSignalLUT to redirect indexing the error checking for 
	 * sig.length and img.length is critical. Elsewhere, it might not matter, nevertheless, 
	 * all arrays need to conform to the dimensions of the PixelAudioMapper instance. 
	 *
	 */

	/*-------------------------- CONFORM ARRAYS --------------------------*/
	
	/**
	 * For use with the audio to color transcoding in PixelAudioMapper, 
	 * Int[] and float[] arrays must be formatted as 24- or 32-bit RGB data.
	 * Float[] arrays must be formatted as audio [-1.0, 1.0] data. 
	 * The arrays must conform to the dimensions of the PixelAudioMapper instance, 
	 * so that array.length == this.mapSize. We do NOT check for null arrays.
	 */
	
	
	public int[] conformArray(int[] source) {
		if (source.length == this.mapSize) return source;
		else // Pad or truncate to match mapSize
			source = Arrays.copyOf(source, this.mapSize);
	    return source;
	}

	public float[] conformArray(float[] source) {
		if (source.length == this.mapSize) return source;
		else // Pad or truncate to match mapSize
			source = Arrays.copyOf(source, this.mapSize);
	    return source;
	}

	public static int[] conformArray(int[] source, int mapSize) {
		if (source.length == mapSize) return source;
		else // Pad or truncate to match mapSize
			source = Arrays.copyOf(source, mapSize);
	    return source;
	}

	public static float[] conformArray(float[] source, int mapSize) {
		if (source.length == mapSize) return source;
		else // Pad or truncate to match mapSize
			source = Arrays.copyOf(source, mapSize);
	    return source;
	}


	/*-------------------------- PLUCK AND PLANT METHODS --------------------------*/


	/**
	 * Starting at signalPos, reads length values from pixel array img
	 * in signal path order using signalToImageLUT to redirect indexing 
	 * and then returns them as an array of RGB pixel values in signal order. 
	 * Note that signalPos = this.imageToSignalLUT[x + y * this.width]. 
	 * The source image data in img must conform to the current PixelAudioMapper
	 * instance's dimensions, otherwise we'll throw an IllegalArgumentException.
	 *
	 * @param img       array of RGB values, the pixels array from a bitmap image
	 *                  with the same width and height as PixelAudioMapper
	 * @param signalPos position in the signal at which to start reading pixel
	 *                  values from the image, following the signal path
	 * @param length    length of the subarray to pluck from img, reading pixel
	 *                  values while following the signal path
	 * @return 			a new array of pixel values in signal order
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if img.length != this.width * this.height
	 */
	public int[] pluckPixels(int[] img, int signalPos, int length) {
	    if (img == null) throw new IllegalArgumentException("Image array cannot be null");
	    if (img.length != this.width * this.height) 
		        throw new IllegalArgumentException("img length does not match PixelAudioMapper dimensions");
	    if (signalPos < 0 || signalPos >= img.length) throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > img.length) throw new IllegalArgumentException("Invalid length");
		int[] pixels = new int[length]; // new array for pixel values
	    for (int i = 0; i < length; i++) {
	        pixels[i] = img[this.signalToImageLUT[signalPos + i]];
	    }
		return pixels; // return the samples
	}

	 
	// It's not clear to me when this signature might be useful. Not yet, anyhow.
	// It's supposed to return one channel of RGB, and that might be useful, after all. Implement it later. TODO
	  
	/**
	 * @param img
	 * @param samplePos
	 * @param length
	 * @param fromChannel
	 * @return
	 */
	  /*
	public int[] pluckPixels(int[] img, int samplePos, int length, ChannelNames fromChannel) {
		int pos = x + y * this.width;
		int[] petal = new int[length];
		for (int i = pos; i < pos + length; i++) {

		}
		return petal;
	}
	*/
	

	/**
     * Starting at <code>signalPos</code>, reads <code>length</code> values from pixel array <code>img</code> in signal order
     * using <code>signalToImageLUT</code> to redirect indexing and then returns them as an array of transcoded float values.
     * Note that <code>signalPos = this.imageToSignalLUT[x + y * this.width]</code> or <code>this.lookupSample(x, y)</code>.
     * 
	 * Starting at image coordinates (x, y), reads values from pixel array img using imageToSignalLUT
	 * to redirect indexing and returns them as an array of transcoded audio values in signal order.
	 *
	 * @param img			source array of RGB pixel values, must conform to PixelAudioMapper dimensions
	 * @param signalPos		position in the signal at which to start reading pixel values from the image, following the signal path
	 * @param length		length of the subarray to pluck from img
	 * @param fromChannel	the color channel from which to read pixel values
	 * @return				a new array of audio values in signal order
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if img.length != this.width * this.height
	 */
	public float[] pluckPixelsAsAudio(int[] img, int signalPos, int length, ChannelNames fromChannel) {
	    if (img == null) throw new IllegalArgumentException("img array cannot be null");
	    if (img.length != this.width * this.height) 
	        throw new IllegalArgumentException("img length does not match PixelAudioMapper dimensions");
	    if (signalPos < 0 || signalPos >= img.length) throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > img.length) throw new IllegalArgumentException("Invalid length: out of bounds");
	    float[] samples = new float[length];							// create an array of samples
	    float[] hsbPixel = new float[3]; 								// Local to avoid thread safety issues
	    for (int j = 0; j < length; j++) {
	        int i = signalPos + j;
	        int rgb = img[this.signalToImageLUT[i]];
	        samples[j] = extractColorAsAudio(rgb, fromChannel, hsbPixel);
	    }
		return samples;
	}

	
	// NO LUT
	/**
     * Starting at signalPos, reads length values from float array sig 
	 * and returns them as a new array of audio values in signal order. 
	 * Really just a standard subarray copy method. 
	 * No lookup tables are used. 
	 * Does not require array lengths to equal this.width * this.height. 
     * 
     * All we're doing is getting a subarray of an array of float. 
     * Since we don't use indirect indexing with LUTs, sig.length is not required to equal this.mapSize.
     * 
	 * @param sig			source array of audio values
	 * @param signalPos		a position in the sig array
	 * @param length		number of values to read from sig array
	 * @return				a new array with the audio values we read
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null 
	 */
	public float[] pluckSamples(float[] sig, int signalPos, int length) {
	    if (sig == null) throw new IllegalArgumentException("sig array cannot be null");
	    if (signalPos < 0 || signalPos >= sig.length) throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > sig.length) throw new IllegalArgumentException("Invalid length: out of bounds");
		float[] samples = new float[mapSize];
		for (int j = 0; j < length; j++) {
			samples[j] = sig[signalPos + j];
		}
		return samples;
	}

	// NO LUT
	/**
     * Starting at <code>signalPos</code>, reads and transcodes <code>length</code> values from float array <code>sig</code> 
     * and returns them as an RGB array in signal order.
	 * No lookup tables are used. 
	 * Does not require array lengths to equal this.width * this.height. 
     * 
     * We're getting a subarray of an array of float and transcoding it to an array of RGB int values.
     * Since we don't use indirect indexing with LUTs, sig.length is not required to equal this.mapSize.
     * 
	 * @param sig			source array of audio values (-1.0f..1.0f)
	 * @param signalPos		entry point in the sig array
	 * @param length		number of values to read from the sig array
	 * @return				an array of RGB values where r == g == b, derived from the sig values
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null
	 */
	public int[] pluckSamplesAsRGB(float[] sig, int signalPos, int length) {
	    if (sig == null) throw new IllegalArgumentException("sig array cannot be null");
	    if (signalPos < 0 || signalPos >= sig.length) throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > sig.length) throw new IllegalArgumentException("Invalid length: out of bounds");
		int[] rgbPixels = new int[length];
		int j = 0;
		for (int i = signalPos; i < signalPos + length; i++) {
			float sample = sig[i];
			// sample = sample > 1.0f ? 1.0f : sample < -1.0f ? -1.0f : sample;		// a precaution, keep values within limits
			int v = audioToRGBChan(sample);                                         // map from [-1.0, 1.0] to [0, 255]
			rgbPixels[j++] = 255 << 24 | v << 16 | v << 8 | v;						// an opaque RGB gray (r == g == b)
		}
		return rgbPixels;
	}


	/**
	 * Starting at <code>signalPos</code>, writes <code>length</code> values from RGB array <code>sprout</code> 
	 * into RGB array <code>img</code>, in signal order. Since we redirect indexing with a lookup table, 
	 * <code>img.length</code> is necessarily equal to mapSize, i.e., this.width * this.height. If not,
	 * we'll throw an IllegalArgumentException.
	 *
	 * @param sprout		source array of RGB values to insert into target array img
	 * @param img			target array of RGB values. in image (row major) order
	 * @param signalPos   	position in the signal at which to start writing pixel values to the image, following the signal path
	 * @param length		number of values from sprout to insert into img array
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if img.length != this.width * this.height
	 */
	public void plantPixels(int[] sprout, int[] img, int signalPos, int length) {
	    if (sprout == null || img == null) 
	    	throw new IllegalArgumentException("Input arrays cannot be null");
	    if (img.length != this.width * this.height)
	        throw new IllegalArgumentException("img length does not match PixelAudioMapper dimensions");
	    if (signalPos < 0 || signalPos >= img.length)
	        throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > img.length || length > sprout.length)
	        throw new IllegalArgumentException("Invalid length: out of bounds");
		int j = 0;														// index for sprout
		for (int i = signalPos; i < signalPos + length; i++) {			// step through signal positions 
			img[signalToImageLUT[i]] = sprout[j++];						// assign values from sprout to img
		}
	}


	/**
	 * Writes values from RGB source array sprout into img at positions mapped 
	 * by the signal path, using the specified channel. Since we redirect indexing with a lookup table, 
	 * <code>img.length</code> is necessarily equal to mapSize, i.e., this.width * this.height. If not,
	 * we'll throw an IllegalArgumentException.
	 * 
	 * @param sprout      source array of RGB values to insert
	 * @param img         target array of RGB values (image, row-major order)
	 * @param signalPos   signal position to start writing
	 * @param length      number of values to write
	 * @param toChannel   channel to write into (R, G, B, L, etc.)
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if img.length != this.width * this.height
	 */
	public void plantPixels(int[] sprout, int[] img, int signalPos, int length, ChannelNames toChannel) {
	    if (sprout == null || img == null) 
	    	throw new IllegalArgumentException("Input arrays cannot be null");
	    if (img.length != this.width * this.height)
	        throw new IllegalArgumentException("img length does not match PixelAudioMapper dimensions");
	    if (signalPos < 0 || signalPos >= img.length)
	        throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > img.length || length > sprout.length)
	        throw new IllegalArgumentException("Invalid length: out of bounds");
	    float[] hsbPixel = new float[3]; // local for thread safety
	    for (int j = 0; j < length; j++) {
	        int imgIdx = this.signalToImageLUT[signalPos + j];
	        img[imgIdx] = applyChannelToColor(sprout[j], img[imgIdx], toChannel, hsbPixel);
	    }
	}
	
	/**
	 * Writes values from audio data array sprout into the specified channel of the img array
	 * at positions mapped by the signal path, starting at signalPos for the given length.
	 * Since we redirect indexing with a lookup table, <code>img.length</code> is necessarily
	 * equal to mapSize, i.e., this.width * this.height. If not, we'll throw an IllegalArgumentException.
  	 *
	 * @param sprout	   source array audio samples ([-1.0, 1.0])
	 * @param img		   target array of RGB values (image, row-major order)
	 * @param signalPos    signal position to start writing 
	 * @param length	   number of values to write
	 * @param toChannel    color channel to write to
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if img.length != this.width * this.height
	 */
	public void plantPixels(float[] sprout, int[] img, int signalPos, int length, ChannelNames toChannel) {
	    if (sprout == null || img == null)
	        throw new IllegalArgumentException("Input arrays cannot be null");
	    if (img.length != this.width * this.height)
	        throw new IllegalArgumentException("img length does not match PixelAudioMapper dimensions");
	    if (signalPos < 0 || signalPos >= img.length)
	        throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > img.length || length > sprout.length)
	        throw new IllegalArgumentException("Invalid length: out of bounds");
	    float[] hsbPixel = new float[3];
	    for (int j = 0; j < length; j++) {
	        int imgIdx = this.signalToImageLUT[signalPos + j];
	        img[imgIdx] = applyAudioToColor(sprout[j], img[imgIdx], toChannel, hsbPixel);
	    }
	}

	// NO LUT
	/**
	 * Starting at signalPos, insert length audio samples from source array sprout 
	 * into target array of audio samples sig. 
	 * In effect, a subarray insertion method. 
	 * No lookup tables are used. 
	 * Does not require array lengths to equal this.width * this.height. 
	 *
	 * @param sprout		source array of audio values (-1.0f..1.0f)
	 * @param sig			target array of signal values, in signal order
	 * @param signalPos		start point in sig array
	 * @param length		number of values to copy from sprout array to sig array
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null
	 */
	public void plantSamples(float[] sprout, float[] sig, int signalPos, int length) {
	    if (sprout == null || sig == null) 
	    	throw new IllegalArgumentException("Input arrays cannot be null");
	    if (signalPos < 0 || signalPos >= sig.length)
	        throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > sig.length || length > sprout.length)
	        throw new IllegalArgumentException("Invalid length: out of bounds");
		int j = 0;
		for (int i = signalPos; i < signalPos + length; i++) {
			sig[i] = sprout[j++];
		}
	}

	// NO LUT
	/**
	 * Starting at <code>signalPos</code>, insert <code>length</code> transcoded RGB samples from source array <code>sprout</code> 
	 * into target array of audio samples <code>sig</code>. 
	 * No lookup tables are used. 
	 * Does not require array lengths to equal this.width * this.height. 
	 *
	 * @param sprout        source array of RGB color values
	 * @param sig           target array of audio values
	 * @param signalPos     insertion point in sig
	 * @param length        number of values to write to sig
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null
	 */
	public void plantSamples(int[] sprout, float[] sig, int signalPos, int length) {
	    if (sprout == null || sig == null) 
	    	throw new IllegalArgumentException("Input arrays cannot be null");
	    if (signalPos < 0 || signalPos >= sig.length)
	        throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > sig.length || length > sprout.length)
	        throw new IllegalArgumentException("Invalid length: out of bounds");
		int j = 0;
		for (int i = signalPos; i < signalPos + length; i++) {
			sig[i] = rgbChanToAudio(PixelAudioMapper.getLuminosity(sprout[j++]));
		}
	}

	// NO LUT
	/**
	 * Writes transcoded values from a specified channel of a color array (sprout) 
	 * into an audio array (sig) starting at signalPos for the given length.
	 * No lookup tables are used. 
	 * Does not require array lengths to equal this.width * this.height. 
	 *
	 * @param sprout      source array of RGB pixel values
	 * @param sig         target array of audio samples (float, [-1.0, 1.0])
	 * @param signalPos   position to start writing into sig
	 * @param length      number of values to write
	 * @param fromChannel channel to extract from sprout values
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null 
	 */
	public void plantSamples(int[] sprout, float[] sig, int signalPos, int length, ChannelNames fromChannel) {
	    if (sprout == null || sig == null)
	        throw new IllegalArgumentException("Input arrays cannot be null");
	    if (signalPos < 0 || signalPos >= sig.length)
	        throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > sig.length || length > sprout.length)
	        throw new IllegalArgumentException("Invalid length: out of bounds");
	    float[] hsbPixel = new float[3]; 		// local for thread safety
	    for (int j = 0; j < length; j++) {
	        sig[signalPos + j] = extractColorAsAudio(sprout[j], fromChannel, hsbPixel);
	    }
	}
	
	
	
	/*-------------------------- PEEL AND STAMP METHODS --------------------------*/
	
	/*
	 * All the "peel" and "stamp" methods depend on the dimensions of the PixelAudioMapper instance.
	 * For that reason, they will throw an IllegalArgumentException if arrays do not conform to 
	 * mapSize, i.e. this.width * this.height.  
	 * 
	 */

	// NO LUT
	/**
	 * Copies a rectangular area of pixels in image (row-major) order and returns it 
	 * as an array of RGB values (a standard operation).
	 * No lookup tables are used. 
	 * Requires img.length to equal this.width * this.height. 
	 * 
	 * @param img       the image pixel array (row-major, length == width * height)
	 * @param x         left edge of rectangle
	 * @param y         top edge of rectangle
	 * @param w         width of rectangle
	 * @param h         height of rectangle
	 * @return array of int (RGB values), length w*h
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null 
	 *         or if img.length != this.width * this.height
	 */
	public int[] peelPixels(int[] img, int x, int y, int w, int h) {
	    if (img == null) throw new IllegalArgumentException("img array cannot be null");
	    if (img.length != this.width * this.height)
	        throw new IllegalArgumentException("img length does not match PixelAudioMapper dimensions");
	    if (w <= 0 || h <= 0) throw new IllegalArgumentException("width and height must be positive");
	    if (x < 0 || y < 0 || x + w > this.width || y + h > this.height)
	        throw new IllegalArgumentException("Requested rectangle is out of image bounds");
	    int[] rgbPixels = new int[w * h];
	    int j = 0;
	    for (int dy = y; dy < y + h; dy++) {
	        int rowStart = dy * this.width;
	        for (int dx = x; dx < x + w; dx++) {
	            rgbPixels[j++] = img[rowStart + dx];
	        }
	    }
	    return rgbPixels;
	}
	
	// NO LUT
	/**
	 * Copies a rectangular area of pixels in image (row-major) order and returns 
	 * it as an array of audio values ([-1.0, 1.0]).
	 * No lookup tables are used. 
	 * Requires img.length to equal this.width * this.height. 
	 * 
	 * @param img the image pixel array (row-major, length == width * height)
	 * @param x   left edge of rectangle
	 * @param y   top edge of rectangle
	 * @param w   width of rectangle
	 * @param h   height of rectangle
	 * @return    array of float (audio values), length w*h
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null 
	 *         or if img.length != this.width * this.height
	 */
	public float[] peelPixelsAsAudio(int[] img, int x, int y, int w, int h) {
	    if (img == null) throw new IllegalArgumentException("img array cannot be null");
	    if (img.length != this.width * this.height)
	        throw new IllegalArgumentException("img length does not match PixelAudioMapper dimensions");
	    if (w <= 0 || h <= 0) throw new IllegalArgumentException("width and height must be positive");
	    if (x < 0 || y < 0 || x + w > this.width || y + h > this.height)
	        throw new IllegalArgumentException("Requested rectangle is out of image bounds");
		float[] samples = new float[w * h];
		int j = 0;
		for (int dy = y; dy < y + h; dy++) {
	        int rowStart = dy * this.width;
			for (int dx = x; dx < x + w; dx++) {
				samples[j++] =  rgbChanToAudio(PixelAudioMapper.getLuminosity(img[rowStart + dx]));
			}
		}
		return samples;
	}

	/**
	 * Copies a rectangular area of audio values from a signal mapped to an image 
	 * using imageToSignalLUT to index values. With the resulting array you could, 
	 * for example, run a 2D filter over selected 1D audio data. 
	 * Requires sig.length to equal this.width * this.height.
	 *
	 * @param sig  a source array of audio samples ([-1.0, 1.0])
	 * @param x    left edge of rectangle
	 * @param y    top edge of rectangle
	 * @param w    width of rectangle
	 * @param h    height of rectangle
	 * @return     array of float (audio values), length w*h
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if sig.length != this.width * this.height
	 */
	public float[] peelSamples(float[] sig, int x, int y, int w, int h) {
	    if (sig == null) throw new IllegalArgumentException("img array cannot be null");
	    if (sig.length != this.width * this.height)
	        throw new IllegalArgumentException("sig length does not match PixelAudioMapper dimensions");
	    if (w <= 0 || h <= 0) throw new IllegalArgumentException("width and height must be positive");
	    if (x < 0 || y < 0 || x + w > this.width || y + h > this.height)
	        throw new IllegalArgumentException("Requested rectangle is out of image bounds");
		float[] samples = new float[w * h];
		int j = 0;
		for (int dy = y; dy < y + h; dy++) {
			int rowStart = dy * this.width;
			for (int dx = x; dx < x + w; dx++) {
				samples[j++] =  sig[this.imageToSignalLUT[rowStart + dx]];
			}
		}
		return samples;
	}

	/**
	 * Copies a rectangular area of audio values from a signal mapped to an image 
	 * using imageToSignalLUT to index values. 
	 * Requires sig.length to equal this.width * this.height.
	 *
	 * @param sig  a source array of audio values ([-1.0, 1.0])
	 * @param x    left edge of rectangle
	 * @param y    top edge of rectangle
	 * @param w    width of rectangle
	 * @param h    height of rectangle
	 * @return     array of float (audio values), length w*h
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if sig.length != this.width * this.height
	 */
	public int[] peelSamplesAsRGB(float[]sig, int x, int y, int w, int h) {
	    if (sig == null) throw new IllegalArgumentException("img array cannot be null");
	    if (sig.length != this.width * this.height)
	        throw new IllegalArgumentException("sig length does not match PixelAudioMapper dimensions");
	    if (w <= 0 || h <= 0) throw new IllegalArgumentException("width and height must be positive");
	    if (x < 0 || y < 0 || x + w > this.width || y + h > this.height)
	        throw new IllegalArgumentException("Requested rectangle is out of image bounds");
		int[] rgbPixels = new int[w * h];
		int j = 0;
		for (int dy = y; dy < y + h; dy++) {
			for (int dx = x; dx < x + w; dx++) {
				int rowStart = dy * this.width;
				rgbPixels[j++] =  Math.round(rgbFloatToAudio(sig[this.imageToSignalLUT[rowStart + dx]]));
			}
		}
		return rgbPixels;
	}

/*
	// I don't think this makes much sense, as far as eventual usefulness
    public int[] peelSamplesAsRGB(float[]sig, int pos, int length, ChannelNames toChannel) {

	}
*/


	// NO LUT
	/**
	 * Pastes a source array of RGB data into a rectangular area of a destination image 
	 * (a standard operation). 
	 * No lookup tables are used. 
	 * Requires img.length to equal this.width * this.height. 
	 * 
	 * @param stamp    a source array of RGB data
	 * @param img      a destination image
	 * @param x        leftmost x-coordinate of a rectangular area in the destination image
	 * @param y        topmost y-coordinate of a rectangular area in the destination image
	 * @param w        width of rectangular area
	 * @param h        height of rectangular area
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null 
	 *         or if img.length != this.width * this.height
	 */
	public void stampPixels(int[] stamp, int[] img, int x, int y, int w, int h) {
	    if (stamp == null) throw new IllegalArgumentException("stamp array cannot be null");
	    if (img.length != this.width * this.height)
	        throw new IllegalArgumentException("img length does not match PixelAudioMapper dimensions");
	    if (w <= 0 || h <= 0) throw new IllegalArgumentException("width and height must be positive");
	    if (x < 0 || y < 0 || x + w > this.width || y + h > this.height)
	        throw new IllegalArgumentException("Requested rectangle is out of image bounds");
		int j = 0;
		for (int dy = y; dy < y + h; dy++) {
			int rowStart = dy * this.width;
			for (int dx = x; dx < x + w; dx++) {
				img[rowStart + dx] = stamp[j++];
			}
		}
	}

	// NO LUT
	/**
	 * Pastes a specified channel of a source array of RGB data into a rectangular area of 
	 * a destination image (a standard operation). 
	 * No lookup tables are used. 
	 * Requires img.length to equal this.width * this.height. 
	 * 
	 * @param stamp        a source array of RGB data
	 * @param img          a destination image
	 * @param x            leftmost x-coordinate of a rectangular area in the destination image
	 * @param y            topmost y-coordinate of a rectangular area in the destination image
	 * @param w            width of rectangular area
	 * @param h            height of rectangular area
	 * @param toChannel    color channel to write to
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null 
	 *         or if img.length != this.width * this.height
	 */ 
	public void stampPixels(int[] stamp, int[] img, int x, int y, int w, int h, ChannelNames toChannel) {
	    if (stamp == null) throw new IllegalArgumentException("stamp array cannot be null");
	    if (img.length != this.width * this.height)
	        throw new IllegalArgumentException("img length does not match PixelAudioMapper dimensions");
	    if (w <= 0 || h <= 0) throw new IllegalArgumentException("width and height must be positive");
	    if (x < 0 || y < 0 || x + w > this.width || y + h > this.height)
	        throw new IllegalArgumentException("Requested rectangle is out of image bounds");
	    float[] hsbPixel = new float[3];
		int j = 0;
		for (int dy = y; dy < y + h; dy++) {
			int rowStart = dy * this.width;
			for (int dx = x; dx < x + w; dx++) {
				img[rowStart + dx] = PixelAudioMapper.applyChannelToColor(stamp[j++], img[rowStart + dx], toChannel, hsbPixel);
			}
		}
	}

	// NO LUT
	/**
	 * Pastes a source array of audio data into a specified color channel of a rectangular
	 * area of a destination image. 
	 * No lookup tables are used. 
	 * Requires img.length to equal this.width * this.height. 
	 * 
	 * @param stamp        a source array of audio data ([-1.0, 1.0])
	 * @param img          a destination image
	 * @param x            leftmost x-coordinate of a rectangular area in the destination image
	 * @param y            topmost y-coordinate of a rectangular area in the destination image
	 * @param w            width of rectangular area
	 * @param h            height of rectangular area
	 * @param toChannel    color channel to write to
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null 
	 *         or if img.length != this.width * this.height
	 */
	public void stampPixels(float[] stamp, int[] img, int x, int y, int w, int h, ChannelNames toChannel) {
	    if (stamp == null) throw new IllegalArgumentException("stamp array cannot be null");
	    if (img.length != this.width * this.height)
	        throw new IllegalArgumentException("img length does not match PixelAudioMapper dimensions");
	    if (w <= 0 || h <= 0) throw new IllegalArgumentException("width and height must be positive");
	    if (x < 0 || y < 0 || x + w > this.width || y + h > this.height)
	        throw new IllegalArgumentException("Requested rectangle is out of image bounds");
		int j = 0;
		float[] hsbPixel = new float[3];
		for (int dy = y; dy < y + h; dy++) {
			int rowStart = dy * this.width;
			for (int dx = x; dx < x + w; dx++) {
				img[rowStart + dx] = PixelAudioMapper.applyAudioToColor(stamp[j++], img[rowStart + dx], toChannel, hsbPixel);
			}
		}
	}

	/**
	 * Pastes a source array of audio data into a destination array of audio data using
	 * imagetoSignalLUT to map data from source to destination. In effect, source and 
	 * destination are treated as 2D rectangular arrays. 
	 * Note that sig.length must equal this.width * this.height.
	 * 
	 * @param stamp    a source array of audio data ([-1.0, 1.0])
	 * @param sig      a destination array of audio data
	 * @param x        leftmost x-coordinate of a rectangular area in the destination image
	 * @param y        topmost y-coordinate of a rectangular area in the destination image
	 * @param w        width of rectangular area
	 * @param h        height of rectangular area
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if sig.length != this.width * this.height
	 */
	public void stampSamples(float[] stamp, float[] sig, int x, int y, int w, int h) {
	    if (stamp == null) throw new IllegalArgumentException("stamp array cannot be null");
	    if (sig.length != this.width * this.height)
	        throw new IllegalArgumentException("sig length does not match PixelAudioMapper dimensions");
	    if (w <= 0 || h <= 0) throw new IllegalArgumentException("width and height must be positive");
	    if (x < 0 || y < 0 || x + w > this.width || y + h > this.height)
	        throw new IllegalArgumentException("Requested rectangle is out of image bounds");
		int j = 0;
		for (int dy = y; dy < y + h; dy++) {
			int rowStart = dy * this.width;
			for (int dx = x; dx < x + w; dx++) {
				sig[this.imageToSignalLUT[rowStart + dx]] = stamp[j++];
			}
		}
	}

	/**
	 * Pastes a source array of audio data into a destination array of RGB data as grayscale
	 * luminosity values. Note that sig.length must equal this.width * this.height.
	 * 
	 * @param stamp
	 * @param sig
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if sig.length != this.width * this.height
	 */
	public void stampSamples(int[] stamp, float[] sig, int x, int y, int w, int h) {
	    if (stamp == null) throw new IllegalArgumentException("stamp array cannot be null");
	    if (sig.length != this.width * this.height)
	        throw new IllegalArgumentException("sig length does not match PixelAudioMapper dimensions");
	    if (w <= 0 || h <= 0) throw new IllegalArgumentException("width and height must be positive");
	    if (x < 0 || y < 0 || x + w > this.width || y + h > this.height)
	        throw new IllegalArgumentException("Requested rectangle is out of image bounds");
		int j = 0;
		for (int dy = y; dy < y + h; dy++) {
			int rowStart = dy * this.width;
			for (int dx = x; dx < x + w; dx++) {
				sig[this.imageToSignalLUT[rowStart + dx]] = rgbChanToAudio(stamp[j++]);
			}
		}
	}
	

	// ------------- AUDIO <---> COLOR ------------- //
	
	/*
	 * This section provides static methods that operate on arrays 
	 * of audio samples and colors, both with and without lookup 
	 * tables for redirecting array indexing. It starts with 
	 * private helper methods that use switch statements, 
	 * extractColorAsAudio, applyAudioToColor, and applyChannelToColor.
	 * 
	 * These are called from public methods that loop over arrays of 
	 * RGB color values (int[]) or audio signal values (float[])
	 * and return modified arrays based on the operation requested.
	 * 
	 */
	
	// --------------- HELPER METHODS -------------- //

	/**
	 * Helper method for color to audio operations, converts a color channel value to an audio value. 
	 * 
	 * @param rgb		an RGB color value
	 * @param chan      the channel to extract for the RGB color value
	 * @param hsbPixel  a local array for HSB values, will contain HSB data on completion of HSB extraction
	 * @return          the extracted channel as an audio float value
	 */
	public static float extractColorAsAudio(int rgb, ChannelNames chan, float[] hsbPixel) {
		switch (chan) {
		case L: return hsbFloatToAudio(brightness(rgb, hsbPixel));
		case H: return hsbFloatToAudio(hue(rgb, hsbPixel));
		case S: return hsbFloatToAudio(saturation(rgb, hsbPixel));
		case R: return rgbChanToAudio(((rgb >> 16) & 0xFF));
		case G: return rgbChanToAudio(((rgb >> 8) & 0xFF));
		case B: return rgbChanToAudio((rgb & 0xFF));
		case A: return rgbChanToAudio(((rgb >> 24) & 0xFF));
		case ALL: return rgbFloatToAudio((0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF)));
		default:  throw new AssertionError("Unknown channel: " + chan);
		}
	}

	/*
	 * Symmetry of method signatures doesn't quite work in this case:
	 * 
	 * Helper method for audio to color operations, converts an audio value to a color channel value. 
     * BUT -- for HSB operations we cannot move a pure H, S, or L channel into an RGB color.
     * That's why we return grayscale in the pluckSamplesAsRGB() method.
     * 
     * float hsbVal = audioToHSBFloat(sample);
     * int rgbVal = applyBrightness(hsbVal, ???, hsbPixel);
	 * 
	 * @param sample	an audio sample value
	 * @param chan      the channel to extract for the RGB color value
	 * @param hsbPixel  a local array for HSB values, will contain HSB data on completion of HSB extraction
	 * @return          an RGB color with the audio sample mapped to the requested channel
	 */
    /* 
	public static int extractAudioAsColor(float sample, ChannelNames chan, float[] hsbPixel) {
		switch (chan) {
		case L: return hsbFloatToAudio(brightness(rgb, hsbPixel));  // ??
		case H: return hsbFloatToAudio(hue(rgb, hsbPixel));         // ??
		case S: return hsbFloatToAudio(saturation(rgb, hsbPixel));  // ??
		case R: return (audioToRGBChan(sample) >> 16) & 0xFF;     // audio sample as red
		case G: return (audioToRGBChan(sample) >> 8) & 0xFF;      // audio sample as green
		case B: return audioToRGBChan(sample) & 0xFF;             // audio sample as blue
		case A: return (audioToRGBChan(sample) >> 24) & 0xFF;     // audio sample as alpha
		case ALL: 
        	int v = audioToRGBChan(sample);                       // map from [-1.0, 1.0] to [0, 255]
			rgbPixels[j++] = 255 << 24 | v << 16 | v << 8 | v;    // an opaque RGB gray (r == g == b)
		default:  throw new AssertionError("Unknown channel: " + chan);
		}
	}
    */
	
	/**
	 * Helper method that applies a float audio sample to a pixel channel, using the channel name.
	 * 
	 * @param sample    audio float source of color value
	 * @param rgb       destination RGB color value
	 * @param chan      the color channel to use
	 * @param hsbPixel  array of 3 floats to maintain HSB color data
	 * @return          RGB color derived from rgb, with color channel changed
	 */
	public static int applyAudioToColor(float sample, int rgb, ChannelNames chan, float[] hsbPixel) {
	    switch (chan) {
	        case L:   return PixelAudioMapper.applyBrightness(sample, rgb, hsbPixel);
	        case H:   return PixelAudioMapper.applyHue(sample, rgb, hsbPixel);
	        case S:   return PixelAudioMapper.applySaturation(sample, rgb, hsbPixel);
	        case R:   return PixelAudioMapper.applyRed(sample, rgb);
	        case G:   return PixelAudioMapper.applyGreen(sample, rgb);
	        case B:   return PixelAudioMapper.applyBlue(sample, rgb);
	        case A:   return PixelAudioMapper.applyAlpha(sample, rgb);
	        case ALL: return PixelAudioMapper.applyAll(sample, rgb);
	        default:  throw new AssertionError("Unknown channel: " + chan);
	    }
	}
	
	/**
	 * Helper method for applying a color channel from a  to an RGB pixel. 
	 * 
	 * @param sample    RGB source of color value
	 * @param rgb       destination color value
	 * @param chan      the color channel to use
	 * @param hsbPixel  array of 3 floats to maintain HSB color data
	 * @return
	 */
	public static int applyChannelToColor(int rgbSource, int rgbTarget, ChannelNames chan, float[] hsbPixel) {
	    switch (chan) {
	        case L:   return PixelAudioMapper.applyBrightness(rgbSource, rgbTarget, hsbPixel);
	        case H:   return PixelAudioMapper.applyHue(rgbSource, rgbTarget, hsbPixel);
	        case S:   return PixelAudioMapper.applySaturation(rgbSource, rgbTarget, hsbPixel);
	        case R:   return PixelAudioMapper.applyRed(rgbSource, rgbTarget);
	        case G:   return PixelAudioMapper.applyGreen(rgbSource, rgbTarget);
	        case B:   return PixelAudioMapper.applyBlue(rgbSource, rgbTarget);
	        case A:   return PixelAudioMapper.applyAlpha(rgbSource, rgbTarget);
	        case ALL: return PixelAudioMapper.applyAll(rgbSource, rgbTarget);
	        default:  throw new AssertionError("Unknown channel: " + chan);
	    }
	}

	// --------------- PULL METHODS, EXTRACT AUDIO VALUES FROM COLOR CHANNELS -------------- //

	
	/**
	 * Converts an array of pixel channel values to an array of audio sample values, mapping sample values
	 * to the interval [-1.0, 1.0], with no remapping of array order. If samples is null 
	 * or samples.length != rgbPixels.length, initializes/adjusts samples.length. 
	 *
	 * @param rgbPixels		an array of RGB pixel values
	 * @param samples		an array of audio samples, which may be null, whose values will be set from rgbPixels
	 * @param chan		    channel to extract from the RGB pixel values
	 * 						Will be initialized and returned if null
	 * @return              a array of floats mapped to the audio range, assigned to samples
	 */
     public static float[] pullPixelAsAudio(int[] rgbPixels, float[] samples, ChannelNames chan, float[] hsbPixel) {
     	if (samples == null || samples.length != rgbPixels.length) {
    		samples = new float[rgbPixels.length];
    	}
        for (int i = 0; i < samples.length; i++) {
        	samples[i] = extractColorAsAudio(rgbPixels[i], chan, hsbPixel);
        }
		return samples;
	}

     
	/**
	 * Converts an array of pixel channel values to an array of audio sample values,
	 * mapping sample values to the interval [-1.0, 1.0], using a supplied lookup
	 * table to change the order of resulting array. If samples is null or
	 * samples.length != rgbPixels.length, initializes/adjusts samples.length.
	 *
	 * @param rgbPixels an array of RGB pixel values
	 * @param samples   an array of audio samples, which may be null, whose values
	 *                  will be set from rgbPixels.
	 * @param lut       a lookup table for redirecting rgbPixels indexing, typically
	 *                  imageToSignalLUT
	 * @param chan      channel to extract from the RGB pixel values Will be
	 *                  initialized and returned if null.
	 * @return a array of floats mapped to the audio range, identical to samples
	 */
	public static float[] pullPixelAsAudio(int[] rgbPixels, float[] samples, int[] lut, ChannelNames chan,
			float[] hsbPixel) {
		if (lut == null || lut.length != rgbPixels.length) {
			throw new IllegalArgumentException(
					"Input array lut cannot be null and must be the same length as rgbPixels");
		}
		if (samples == null || samples.length != rgbPixels.length) {
			samples = new float[rgbPixels.length];
		}
		for (int i = 0; i < samples.length; i++) {
			samples[i] = extractColorAsAudio(rgbPixels[lut[i]], chan, hsbPixel);
		}
		return samples;
	}
		
	public static float[] pullPixelAsAudioShifted(
			int[] rgbPixels, float[] samples, int[] lut, ChannelNames chan,
			float[] hsbPixel, int totalShift) {
		if (lut == null || lut.length != rgbPixels.length) {
			throw new IllegalArgumentException(
					"Input array lut cannot be null and must be the same length as rgbPixels"
					);
		}
		if (samples == null || samples.length != rgbPixels.length) {
			samples = new float[rgbPixels.length];
		}

		final int n = samples.length;
		final int shift = PixelAudioMapper.wrap(totalShift, n);

		for (int i = 0; i < n; i++) {
			int dispPathIndex = i - shift;
			if (dispPathIndex < 0) dispPathIndex += n; // fast wrap; shift in [0,n)
			samples[i] = extractColorAsAudio(rgbPixels[lut[dispPathIndex]], chan, hsbPixel);
		}
		return samples;
	}

	
	public static int[] pullAudioAsColor(float[] samples, int[] rgbPixels, int signalPos, int length) {
		if (samples == null)
			throw new IllegalArgumentException("samples array cannot be null");
		if (rgbPixels == null || rgbPixels.length != samples.length) {
			rgbPixels = new int[samples.length];
		}
		if (signalPos < 0 || signalPos >= samples.length)
			throw new IndexOutOfBoundsException("signalPos out of bounds");
		if (length < 0 || signalPos + length > samples.length)
			throw new IllegalArgumentException("Invalid length: out of bounds");
		int j = 0;
		for (int i = signalPos; i < signalPos + length; i++) {
			float sample = samples[i];
			sample = sample > 1.0f ? 1.0f : sample < -1.0f ? -1.0f : sample; // a precaution, keep values within limits
			int v = audioToRGBChan(sample); // map from [-1.0, 1.0] to [0, 255]
			rgbPixels[j++] = 255 << 24 | v << 16 | v << 8 | v; // an opaque RGB gray (r == g == b)
		}
		return rgbPixels;
	}

	public static int[] pullAudioAsColor(float[] sig) {
		return pullAudioAsColor(sig, null, 0, sig.length);
	}     
     
     /*
      * DELETED: public static float[] pullRawChannel(int[] rgbPixels, ChannelNames chan, float[] hsbPixel)
      * See earlier version (last used in release 0.9) on GitHub if you're curious. 
      */

     
	// ------------- PUSH METHODS, APPLY AUDIO SAMPLES OR COLOR CHANNELS TO COLOR VALUES ------------- //
	
	/**
	 * <p>Replaces a specified channel in an array of pixel values, rgbPixels, with a value derived
	 * from an array of floats, buf, that represent audio samples. Upon completion, the pixel array
	 * rgbPixels contains the new values, always in the RGB color space.</p>
	 *
	 * <p>Both arrays, rgbPixels and buf, must be the same size.</p>
	 *
	 * <p>In the HSB color space, values are assumed to be floats in the range (0..1), so the values
	 * from buf need to be mapped to the correct ranges for HSB or RGB [0, 255]. We do some minimal
	 * limiting of values derived from buf[], but it is the caller's responsibility to constrain them
	 * to the audio range  [-1.0, 1.0].</p>
	 *
	 * @param rgbPixels    a source array of pixel values
	 * @param samples       a target array of floats in the range  (-1.0, 1.0)
	 * @param chan         the channel to replace
	 * @return			   rgbPixels with the selected channel modified by the buf values
	 */
	public static int[] pushAudioToPixel(float[] samples, int[] rgbPixels, ChannelNames chan) {
		// if rgbPixels is null or the wrong size, initialize it and fill it with middle gray color
		if (rgbPixels == null || rgbPixels.length != samples.length) {
			rgbPixels = new int[samples.length];
			Arrays.fill(rgbPixels, PixelAudioMapper.composeColor(127, 127, 127));
		}
		float[] hsbPixel = new float[3];
		for (int i = 0; i < rgbPixels.length; i++) {
			rgbPixels[i] = applyAudioToColor(samples[i], rgbPixels[i], chan, hsbPixel);
		}
		return rgbPixels;
	}

	/**
	 * <p>Replaces a specified channel in an array of pixel values, rgbPixels, with a value derived
	 * from an array of floats, buf, that represent audio samples. The supplied lookup table, lut,
	 * is intended to redirect the indexing of rgbPixels following the signal path. We are stepping
	 * through the buf array (the signal), so rgbPixels employs imageToSignalLUT to find where each
	 * index i into buf is pointing in the image pixels array, which is rgbPixels.  Upon completion,
	 * the pixel array rgbPixels contains the new values, always in the RGB color space.
	 *</p>
	 <p>
	 * All three arrays, rgbPixels, buf, and lut must be the same size.
	 *</p><p>
	 * In the HSB color space, values are assumed to be floats in the range (0..1), so the values
	 * from buf need to be mapped to the correct ranges for HSB or RGB [0, 255]. We do some minimal
	 * limiting of values derived from buf[], but it is the caller's responsibility to constrain them
	 * to the audio range  [-1.0, 1.0].</p>
	 *
	 * @param rgbPixels    an array of pixel values
	 * @param samples       an array of floats in the range  [-1.0, 1.0]
	 * @param lut		   a lookup table to redirect the indexing of the buf, typically imageToPixelsLUT
	 * @param chan         the channel to replace
	 * @return			   rgbPixels with selected channel values modified by the buf values
	 */
	public static int[] pushAudioToChannel(float[] samples, int[] rgbPixels, int[] lut, ChannelNames chan) {
	    if (samples == null) throw new IllegalArgumentException("samples cannot be null");
	    if (lut == null || lut.length != samples.length) {
	        throw new IllegalArgumentException("Input array lut cannot be null and must be the same length as signal.");
	    }

	    final int n = samples.length;

	    // if rgbPixels is null or the wrong size, create an array and fill it with middle gray color
	    if (rgbPixels == null || rgbPixels.length != n) {
	        rgbPixels = new int[n];
	        Arrays.fill(rgbPixels, PixelAudioMapper.composeColor(127, 127, 127));
	    }

	    float[] hsbPixel = new float[3];

	    // Canonical mapping: path position i reads samples[i] and writes to pixel lut[i]
	    for (int i = 0; i < n; i++) {
	        int px = lut[i];
	        rgbPixels[px] = PixelAudioMapper.applyAudioToColor(samples[i], rgbPixels[px], chan, hsbPixel);
	    }
	    return rgbPixels;
	}

	/**
	 * Shifted mapping: path position i reads samples[wrap(i + totalShift, n)]
	 * and writes to pixel lut[i]. This implements "rotation along the signal path"
	 * without rotating arrays.
	 */
	public static int[] pushAudioToChannelShifted(
	    float[] samples, int[] rgbPixels, int[] lut, ChannelNames chan, int totalShift
	) {
	    if (samples == null) throw new IllegalArgumentException("samples cannot be null");
	    if (lut == null || lut.length != samples.length) {
	        throw new IllegalArgumentException("Input array lut cannot be null and must be the same length as signal.");
	    }

	    final int n = samples.length;
	    final int shift = PixelAudioMapper.wrap(totalShift, n);

	    if (rgbPixels == null || rgbPixels.length != n) {
	        rgbPixels = new int[n];
	        Arrays.fill(rgbPixels, PixelAudioMapper.composeColor(127, 127, 127));
	    }

	    float[] hsbPixel = new float[3];

	    // Shifted mapping: samples are read from i+shift; pixels are written to lut[i]
	    for (int i = 0; i < n; i++) {
	        int si = i + shift;
	        if (si >= n) si -= n; // fast wrap for common case; shift already in [0,n)
	        int px = lut[i];
	        rgbPixels[px] = PixelAudioMapper.applyAudioToColor(samples[si], rgbPixels[px], chan, hsbPixel);
	    }
	    return rgbPixels;
	}
	
	/**
	 * <p>Replaces a specified channel in an array of pixel values, rgbPixels, with a value derived
	 * from another array of RGB values, colors. Upon completion, the pixel array
	 * rgbPixels contains the new values, always in the RGB color space.</p>
	 *
	 * <p>Both arrays, rgbPixels and colors, must be the same size.</p>
	 *
	 * <p>For operations in the HSB color space, HSB values will be extracted from the RGB colors
	 * and mapped to the correct ranges for HSB or RGB [0, 255].</p>
	 *
	 * @param colors       an array of RGB colors
	 * @param rgbPixels    an array of pixel values
	 * @param chan         the channel to replace
	 * @return			   rgbPixels with the selected channel modified by the colors array values
	 */
	public static int[] pushChannelToPixel(int[] colors, int[] rgbPixels, ChannelNames chan) {
		// if rgbPixels is null or the wrong size, create an array and fill it with middle gray color
		if (rgbPixels == null || rgbPixels.length != colors.length) {
			rgbPixels = new int[colors.length];
			Arrays.fill(rgbPixels, PixelAudioMapper.composeColor(127, 127, 127));
		}
		float[] hsbPixel = new float[3];
		for (int i = 0; i < rgbPixels.length; i++) {
			rgbPixels[i] = PixelAudioMapper.applyChannelToColor(colors[i], rgbPixels[i], chan, hsbPixel);
		}
		return rgbPixels;
	}

	/**
	 * <p>Replaces a specified channel in an array of pixel values, rgbPixels, with a value derived
	 * from an array of RGB values, colors. The supplied lookup table, lut, is intended
	 * to redirect the indexing of rgbPixels following the signal path. We are stepping through
	 * the color array (the RGB signal), so rgbPixels employs imageToSignalLUT to find where each
	 * index i into colors is pointing in the image pixels array, rgbPixels.  Upon completion,
	 * the pixel array rgbPixels contains the new values, always in the RGB color space.
	 *</p>
	 <p>
	 * All three arrays, rgbPixels, colors, and lut must be the same size.
	 *</p><p>
	 * In the HSB color space, HSB values will be extracted from the RGB colors
	 * and mapped to the correct ranges for HSB or RGB [0, 255].</p>
	 *
	 * @param colors       an array of RGB values
	 * @param rgbPixels    an array of pixel values
	 * @param lut		   a lookup table to redirect indexing, typically imageToPixelsLUT
	 * @param chan         the channel to replace
	 * @return			   rgbPixels with selected channel values modified by the buf values
	 */
	public static int[] pushChannelToPixel(int[] colors, int[] rgbPixels, int[] lut, ChannelNames chan) {
    	if (lut == null || lut.length != colors.length) {
	    	throw new IllegalArgumentException("Input array lut cannot be null and must be the same length as colors");
    	}
		// it's unlikely that this will happen, but if rgbPixels is null, create an array and fill it with middle gray color
		if (rgbPixels == null || rgbPixels.length != colors.length) {
			rgbPixels = new int[colors.length];
			Arrays.fill(rgbPixels, PixelAudioMapper.composeColor(127, 127, 127));
		}
		float[] hsbPixel = new float[3];
		for (int i = 0; i < rgbPixels.length; i++) {
			rgbPixels[lut[i]] = PixelAudioMapper.applyChannelToColor(colors[i], rgbPixels[lut[i]], chan, hsbPixel);
		}
		return rgbPixels;
	}

	
	// ------------- ARRAY ROTATION ------------- //

	/**
	 * Rotates an array of ints left by d values. Uses efficient "Three Reverse" algorithm.
	 *
	 * @param arr array of ints to rotate
	 * @param d   number of elements to shift, positive for shift left, negative for shift right
	 */
	public static final void rotateLeft(int[] arr, int d) {
		int len = arr.length;
		if (d < 0) d = len - (-d % len);
		d = d % len;
		reverseArray(arr, 0, d - 1);
		reverseArray(arr, d, len - 1);
		reverseArray(arr, 0, len - 1);
	}
	public static final void rotateRight(int[] arr, int d) {
		rotateLeft(arr, -d);
	}

	/**
	 * Rotates an array of floats left by d values. Uses efficient "Three Rotation" algorithm.
	 *
	 * @param arr array of floats to rotate
	 * @param d   number of elements to shift, positive for shift left, negative for shift right
	 */
	public static final void rotateLeft(float[] arr, int d) {
		int len = arr.length;
		if (d < 0) d = len - (-d % len);
		d = d % len;
		reverseArray(arr, 0, d - 1);
		reverseArray(arr, d, len - 1);
		reverseArray(arr, 0, len - 1);
	}
	public static final void rotateRight(float[] arr, int d) {
		rotateLeft(arr, -d);
	}

	/**
	 * Reverses an arbitrary subset of an array of ints.
	 *
	 * @param arr array to modify
	 * @param l   left bound of subset to reverse
	 * @param r   right bound of subset to reverse
	 */
	public static final void reverseArray(int[] arr, int l, int r) {
		int temp;
		while (l < r) {
			temp = arr[l];
			arr[l] = arr[r];
			arr[r] = temp;
			l++;
			r--;
		}
	}

	/**
	 * Reverses an arbitrary subset of an array of floats.
	 *
	 * @param arr array to modify
	 * @param l   left bound of subset to reverse
	 * @param r   right bound of subset to reverse
	 */
	public static final void reverseArray(float[] arr, int l, int r) {
		float temp;
		while (l < r) {
			temp = arr[l];
			arr[l] = arr[r];
			arr[r] = temp;
			l++;
			r--;
		}
	}


	//------------- COLOR UTILITIES -------------//

	/**
	 * Breaks a Processing color into R, G and B values in an array.
	 *
	 * @param rgb a Processing color as a 32-bit integer
	 * @return an array of integers in the range [0, 255] for 3 primary color
	 *         components: {R, G, B}
	 */
	public static final int[] rgbComponents(int rgb) {
		int[] comp = new int[3];
	    comp[0] = (rgb >> 16) & 0xFF; // Red component
	    comp[1] = (rgb >> 8) & 0xFF;  // Green component
	    comp[2] = rgb & 0xFF;         // Blue component
		return comp;
	}

	/**
	 * Breaks a Processing color into R, G, B and A values in an array.
	 *
	 * @param 	argb a Processing color as a 32-bit integer
	 * @return 	an array of 4 integers {R, G, B, A} in the range 0..255
	 */
	public static final int[] rgbaComponents(int argb) {
	    int[] comp = new int[4];
	    comp[0] = (argb >> 16) & 0xFF; // Red component
	    comp[1] = (argb >> 8) & 0xFF;  // Green component
	    comp[2] = argb & 0xFF;         // Blue component
	    comp[3] = (argb >> 24) & 0xFF; // Alpha component
	    return comp;
	}


	/**
	 * Breaks a Processing color into A, R, G and B values in an array.
	 * 
	 * @param 	argb a Processing color as a 32-bit integer
	 * @return 	an array of 4 integers {A, R, G, B} in the range 0..255
	 */
	public static final int[] argbComponents(int argb) {
		int[] comp = new int[4];
		comp[0] = (argb >> 24) & 0xFF; // alpha
		comp[1] = (argb >> 16) & 0xFF; // Faster way of getting red(argb)
		comp[2] = (argb >> 8) & 0xFF; // Faster way of getting green(argb)
		comp[3] = argb & 0xFF; // Faster way of getting blue(argb)
		return comp;
	}

	/**
	 * Returns alpha channel value of a color.
	 *
	 * @param argb a Processing color as a 32-bit integer
	 * @return an int for alpha channel
	 */
	public static final int alphaComponent(int argb) {
		return (argb >> 24) & 0xFF;
	}

	/**
	 * Takes the alpha channel from one color, rgba, and applies it to another color, rgb.
	 * @param rgba	The source color from which we get the alpha channel (a mask, for instance)
	 * @param rgb	The target color we want to change
	 * @return		A color with the RGB values from rgb and the A value from rgba
	 */
	public static final int applyAlpha(int rgba, int rgb) {
		return (rgba >> 24) << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | (rgb & 0xFF);
	}
	
	public static final int setAlpha(int argb, int alpha) {
		//int[] c = rgbComponents(argb);
		//return alpha << 24 | c[0] << 16 | c[1] << 8 | c[2];
		 return (argb & 0x00FFFFFF) | (alpha << 24);
	}

	public static final int[] setAlpha(int[] argb, int alpha) {
		for (int i = 0; i < argb.length; i++) {
			argb[i] = (argb[i] & 0x00FFFFFF) | (alpha << 24);
		}
		return argb;
	}
	
	/**
	 * Creates a Processing ARGB color from r, g, b, and alpha channel values. Note the order
	 * of arguments, the same as the Processing color(value1, value2, value3, alpha) method.
	 *
	 * @param r red component [0, 255]
	 * @param g green component [0, 255]
	 * @param b blue component [0, 255]
	 * @param a alpha component [0, 255]
	 * @return a 32-bit integer with bytes in Processing format ARGB.
	 */
	public static final int composeColor(int r, int g, int b, int a) {
		return a << 24 | r << 16 | g << 8 | b;
	}

	/**
	 * Creates an opaque Processing RGB color from r, g, b values. Note the order
	 * of arguments, the same as the Processing color(value1, value2, value3) method.
	 *
	 * @param r red component [0, 255]
	 * @param g green component [0, 255]
	 * @param b blue component [0, 255]
	 * @return a 32-bit integer with bytes in Processing format ARGB.
	 */
	public static final int composeColor(int r, int g, int b) {
		return 255 << 24 | r << 16 | g << 8 | b;
	}

	/**
	 * Creates a Processing ARGB color from r, g, b, values in an array.
	 *
	 * @param comp 	array of 3 integers in range [0, 255], for red, green and blue
	 *             	components of color alpha value is assumed to be 255
	 * @return a 32-bit integer with bytes in Processing format ARGB.
	 */
	public static final int composeColor(int[] comp) {
		return 255 << 24 | comp[0] << 16 | comp[1] << 8 | comp[2];
	}

	/**
	 * @param rgb	an RGB color value
	 * @return		a number in the range [0, 255] equivalent to the luminosity value rgb
	 */
	public static final int getLuminosity(int rgb) {
        //  we'll convert to grayscale using the "luminosity equation."
		float gray = 0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF);
		return Math.round(gray);
	}


	/**
	 * @param argb		an RGB color
	 * @return			a String equivalent to a Processing color(r, g, b, a) call, such as "color(233, 144, 89, 255)"
	 */
	public static String colorString(int argb) {
		int[] comp = rgbaComponents(argb);
		return "color(" + comp[0] + ", " + comp[1] + ", " + comp[2] + ", " + comp[3] + ")";
	}


	// -------- HSB <---> RGB -------- //

	/**
	 * Extracts the hue component from an RGB value. The result is in the range (0, 1).
	 * @param rgb		The RGB color from which we will obtain the hue component in the HSB color model.
	 * @param hsbPixel 	array of float values filled in by this method, caller can provide a reusable array
	 * @return			A floating point number in the range (0..1) that can be multiplied by 360 to get the hue angle.
	 */
	public static float hue(int rgb, float[] hsbPixel) {
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
		return hsbPixel[0];
	}
	/**
	 * Extracts the hue component from an RGB value. The result is in the range (0, 1).
	 * @param rgb	The RGB color from which we will obtain the hue component in the HSB color model.
	 * @return		A floating point number in the range (0..1) that can be multiplied by 360 to get the hue angle.
	 */
	public static float hue(int rgb) {
		float[] hsbTemp = new float[3];
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbTemp);
		return hsbTemp[0];
	}

	/**
	 * Extracts the saturation component from an RGB value. The result is in the range (0, 1).
	 * @param rgb		The RGB color from which we will obtain the hue component in the HSB color model.
	 * @param hsbPixel 	array of float values filled in by this method
	 * @return			A floating point number in the range (0, 1) representing the saturation component of an HSB color.
	 */
	public static float saturation(int rgb, float[] hsbPixel) {
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
		return hsbPixel[1];
	}
	/**
	 * Extracts the saturation component from an RGB value. The result is in the range (0, 1).
	 * @param rgb	The RGB color from which we will obtain the hue component in the HSB color model.
	 * @return		A floating point number in the range (0, 1) representing the saturation component of an HSB color.
	 */
	public static float saturation(int rgb) {
		float[] hsbTemp = new float[3];
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbTemp);
		return hsbTemp[1];
	}

	/**
	 * Extracts the brightness component from an RGB value. The result is in the range (0, 1).
	 * @param rgb		The RGB color from which we will obtain the hue component in the HSB color model.
	 * @param hsbPixel 	array of float values filled in by this method
	 * @return			A floating point number in the range (0, 1) representing the brightness component of an HSB color.
	 */
	public static float brightness(int rgb, float[] hsbPixel) {
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
		return hsbPixel[2];
	}
	/**
	 * Extracts the brightness component from an RGB value. The result is in the range (0, 1).
	 * @param rgb	The RGB color from which we will obtain the hue component in the HSB color model.
	 * @return		A floating point number in the range (0, 1) representing the brightness component of an HSB color.
	 */
	public static float brightness(int rgb) {
		float[] hsbTemp = new float[3];
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbTemp);
		return hsbTemp[2];
	}

	public static int colorShift(int rgbColor, float shift) {
		float[] hsb = new float[3];
		float h = PixelAudioMapper.hue(rgbColor, hsb);
		h = (h + shift);
		return Color.HSBtoRGB(h, hsb[1], hsb[2]);
	}
	
	
	// ------------- APPLY COLOR CHANNEL METHODS ------------- //
	// ---- with floating point (signal) values as source ---- //

	public static int applyBrightness(float sample, int rgb) {
		float[] hsbPixel = new float[3];												// local var so we can make static method
		sample = sample > 1.0f ? 1.0f : sample < -1.0f ? -1.0f : sample;				// a precaution, keep values within limits
		sample = audioToHSBFloat(sample);						                        // map audio sample to (0..1)
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(hsbPixel[0], hsbPixel[1], sample);
	}

	public static int applyBrightness(float sample, int rgb, float[] hsbPixel) {
		sample = sample > 1.0f ? 1.0f : sample < -1.0f ? -1.0f : sample;				// a precaution, keep values within limits
		sample = audioToHSBFloat(sample);						                        // map audio sample to (0..1)
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(hsbPixel[0], hsbPixel[1], sample);
	}

	public static int applyHue(float sample, int rgb) {
		float[] hsbPixel = new float[3];												// local var so we can make static
		sample = sample > 1.0f ? 1.0f : sample < -1.0f ? -1.0f : sample;				// a precaution, keep values within limits
		sample = audioToHSBFloat(sample);						                        // map audio sample to (0..1)
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(sample, hsbPixel[1], hsbPixel[2]);
	}

	public static int applyHue(float sample, int rgb, float[] hsbPixel) {
		sample = sample > 1.0f ? 1.0f : sample < -1.0f ? -1.0f : sample;				// a precaution, keep values within limits
		sample = audioToHSBFloat(sample);						                        // map audio sample to (0..1)
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(sample, hsbPixel[1], hsbPixel[2]);
	}

	public static int applySaturation(float sample, int rgb) {
		float[] hsbPixel = new float[3];												// local var so we can make static
		sample = sample > 1.0f ? 1.0f : sample < -1.0f ? -1.0f : sample;				// a precaution, keep values within limits
		sample = audioToHSBFloat(sample);						                        // map audio sample to (0..1)
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(hsbPixel[0], sample, hsbPixel[2]);
	}

	public static int applySaturation(float sample, int rgb, float[] hsbPixel) {
		sample = sample > 1.0f ? 1.0f : sample < -1.0f ? -1.0f : sample;				// a precaution, keep values within limits
		sample = audioToHSBFloat(sample);						                        // map audio sample to (0..1)
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(hsbPixel[0], sample, hsbPixel[2]);
	}

	public static int applyRed(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < -1.0f ? -1.0f : sample;			// a precaution, keep values within limits
		int r = audioToRGBChan(sample);			                                    // map audio sample to [0, 255]
		return (255 << 24 | r << 16 | ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF);		// apply to red channel
	}

	public static int applyGreen(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < -1.0f ? -1.0f : sample;			// a precaution, keep values within limits
		int g = audioToRGBChan(sample);			                                    // map audio sample to [0, 255]
		return (255 << 24 | ((rgb >> 16) & 0xFF) << 16 | g << 8 | rgb & 0xFF);		// apply to green channel
	}

	public static int applyBlue(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < -1.0f ? -1.0f : sample;			// a precaution, keep values within limits
		int b = audioToRGBChan(sample);						                        // map audio sample to [0, 255]
		return (255 << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | b & 0xFF);	// apply to blue channel
	}

	public static int applyAlpha(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < -1.0f ? -1.0f : sample;			// a precaution, keep values within limits
		int a = audioToRGBChan(sample);						// map audio sample to [0, 255]
		return (a << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF);	// apply to alpha channel
	}
	
	public static int applyAll(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < -1.0f ? -1.0f : sample;			// a precaution, keep values within limits
		int v = audioToRGBChan(sample);			// map audio sample to [0, 255]
		return 255 << 24 | v << 16 | v << 8 | v;									// apply to all channels except alpha
	}

	// ------------- APPLY COLOR CHANNEL METHODS ------------- //
	// ------ with integer (RGB color) values as source ------ //
	
	
	public static int applyBrightness(int rgbSource, int rgb) {
		float[] hsbPixel = new float[3];												// local var so we can make static
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(hsbPixel[0], hsbPixel[1], PixelAudioMapper.brightness(rgbSource));
	}

	public static int applyBrightness(int rgbSource, int rgb, float[] hsbPixel) {
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(hsbPixel[0], hsbPixel[1], PixelAudioMapper.brightness(rgbSource));
	}

	public static int applyHue(int rgbSource, int rgb) {
		float[] hsbPixel = new float[3];												// local var so we can make static
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(PixelAudioMapper.hue(rgbSource), hsbPixel[1], hsbPixel[2]);
	}

	public static int applyHue(int rgbSource, int rgb, float[] hsbPixel) {
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(PixelAudioMapper.hue(rgbSource), hsbPixel[1], hsbPixel[2]);
	}

	public static int applySaturation(int rgbSource, int rgb) {
		float[] hsbPixel = new float[3];												// local var so we can make static
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(hsbPixel[0], PixelAudioMapper.saturation(rgbSource), hsbPixel[2]);
	}

	public static int applySaturation(int rgbSource, int rgb, float[] hsbPixel) {
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(hsbPixel[0], PixelAudioMapper.saturation(rgbSource), hsbPixel[2]);
	}

	public static int applyRed(int rgbSource, int rgb) {
		int r = PixelAudioMapper.rgbComponents(rgbSource)[0];						// get red component
		return (255 << 24 | r << 16 | ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF);		// apply to red channel
	}

	public static int applyGreen(int rgbSource, int rgb) {
		int g = PixelAudioMapper.rgbComponents(rgbSource)[1];				        // get green component
		return (255 << 24 | ((rgb >> 16) & 0xFF) << 16 | g << 8 | rgb & 0xFF);		// apply to green channel
	}

	public static int applyBlue(int rgbSource, int rgb) {
		int b = PixelAudioMapper.rgbComponents(rgbSource)[2];			            // get blue component
		return (255 << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | b & 0xFF);	// apply to blue channel
	}

	public static int applyAll(int rgbSource, int rgb) {
		return rgbSource;											// apply to all channels except alpha
	}

	public static int applyColor(int colorSource, int graySource) {
		float[] hsbPixel = new float[3];
		Color.RGBtoHSB((colorSource >> 16) & 0xff, (colorSource >> 8) & 0xff, colorSource & 0xff, hsbPixel);
		return Color.HSBtoRGB(hsbPixel[0], hsbPixel[1], PixelAudioMapper.brightness(graySource));
	}

	public static int applyColor(int colorSource, int graySource, float[] hsbPixel) {
		Color.RGBtoHSB((colorSource >> 16) & 0xff, (colorSource >> 8) & 0xff, colorSource & 0xff, hsbPixel);
		return Color.HSBtoRGB(hsbPixel[0], hsbPixel[1], PixelAudioMapper.brightness(graySource));
	}

	/**
	 * Utility method for applying hue and saturation values from a source array of RGB values
	 * to the brightness values in a target array of RGB values, using a lookup table to redirect indexing.
	 * 
	 * @param colorSource    a source array of RGB data from which to obtain hue and saturation values
	 * @param graySource     an target array of RGB data from which to obtain brightness values
	 * @param lut            a lookup table, must be the same size as colorSource and graySource
	 * @return the graySource array of RGB values, with hue and saturation values changed
	 * @throws IllegalArgumentException if array arguments are null or if they are not the same length
	 */
	public static int[] applyColor(int[] colorSource, int[] graySource, int[] lut) {
		if (colorSource == null || graySource == null || lut == null) 
			throw new IllegalArgumentException("colorSource, graySource and lut cannot be null.");
		if (colorSource.length != graySource.length || colorSource.length != lut.length) 
			throw new IllegalArgumentException("colorSource, graySource and lut must all have the same length.");
		float[] hsbPixel = new float[3];
		for (int i = 0; i < graySource.length; i++) {
			graySource[i] = PixelAudioMapper.applyColor(colorSource[lut[i]], graySource[i], hsbPixel);
		}
		return graySource;
	}
	

}




