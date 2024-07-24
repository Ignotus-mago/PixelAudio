/**
 *
 */
package net.paulhertz.pixelaudio;


import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;


/**
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
 * <p>
 * Some typical uses for this class include:
 * <ul>
 * 	  <li> Reading an audio file or audio stream into the signal array and then writing its
 *		   values, transcoded to RGB integer values, to the image array for display as a visualization.</li>
 *	  <li> Using interaction with an image to trigger audio events at precise locations in a signal.</li>
 *    <li> Running audio filters on an image-as-signal and writing the results to the image.</li>
 *    <li> Running image algorithms on a signal-as-image and writing the results back to the signal.</li>
 *    <li> Synthesizing image data and audio and then animating the data while interactively
 *         triggering audio events. </li>
 *  </p>
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
 * <p>
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
 * or image to the corresponding position in image or signal.
 * <pre>
 *	signalToImageLUT: integer values over {0..(h * w - 1)} map a signal array index to a pixel array index
 *	imageToSignalLUT: integer values over (0..(h * w - 1)} map an image array index to a signal array index
 * </pre>
 * In signalToImageLUT, we can get the pixel index in the image for any index in the signal.
 * In imageToSignalLUT, we can get index in the signal for any pixel index in the image.
 * </p><p>
 * Each array is the inverse of the other: for an array index i:
 * <pre>
 *	signalToImageLUT[imageToSignalLUT[i]] == i;
 *	imageToSignalLUT[signalToImageLUT[i]] == i;
 * </pre>
 * </p><p>
 * Image data is always in row major order for PImage, our image data class. Signal values can be mapped
 * to locations in the image in any arbitrary order, as long their coordinates traverse the entire image.
 * A typical reordering might be a zigzag from upper left to lower right of an image, or a space-filling
 * fractal, or even a randomly shuffled order. The coordinates of each pixel in the image are stored as
 * indices (i = x + w * y) in signalToImageLUT.
 * </p><p>
 * Once we know the “pixel index” for each value in the signal and have initialized signalToImageLUT,
 * we can initialize imageToSignalLUT:
 *	<pre>
 *	for (int i = 0; i < w * h - 1; i++) {
 *		imageToSignalLUT[signalToImageLUT[i]] = i;
 *	}
 *  </pre>
 * </p><p>
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
 * format to the other. We have two methods, in pseudocode here:
 * <pre>
 *	mapSigToImg		map signal values to the image: img[i] = transcode(sig[imgLUT[i]]);
 *	mapImgToSig		map image values to the signal: sig[i] = transcode(img[sigLUT[i]]);
 * </pre>
 * The img variable in the pseudocode corresponds to an array of RGB data from a bitmap class.
 * The sig variable corresponds to an array of floating point samples from an audio class.
 * </p><p>
 * In addition, we can write image or signal values directly, without using the LUTs. This operation transforms
 * the order of the pixel or signal values.
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
 * <h2>ARRAY SHIFTING<?h2>
 * <p>
 * Standard operations we can perform with the signal array:
 * <pre>
 *   shiftLeft()		an array rotation where index values decrease and wrap around at the beginning
 *   shiftRight()		an array rotation where index values increase and wrap around at the end
 * </pre>
 * Shifting has proved so useful for animation that I am including it in the class. The shift methods also demonstrate
 * how to update the signal and pixel arrays. The <code>WaveSynth</code> class provides other methods for animation.
 * </p>
 *
 * <h2>OTHER OPERATIONS</h2>
 * <p>
 * The following are suggestions for methods that could be implemented using PixelArrayMapper.
 * <ul>
 *	 <li>audio synthesis (the WaveSynth algorithm used in the animation for Campos | Temporales)</li>
 *	 <li>pattern generation (the Argosy pattern algorithm for Campos | Temporales, https://vimeo.com/856300250)</li>
 * 	 <li>phase shifting, amplitude modulation, etc. </li>
 *	 <li>FFT operations on both image and signal data</li>
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
 * <p>
 * <h3>Note: Representing ranges</h3>
 * I am following the convention from mathematical notation of representing closed ranges with [] and open ranges with ().
 * I occasionally lapse into the alternate notations {0..(h * w - 1)} or [0, 255]. When values are integers, it should be 
 * understood that the range covers only integer values. Floating point values are continuous, to the limits of digital computation. 
 * </p>
 * 
 * @see PixelMapGen
 * @see WaveSynth
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
	private float[] hsbPixel = new float[3];

	/** List of available color channels, "L" for lightness, since "B" for brightness is taken */
	public static enum ChannelNames {
		R, G, B, H, S, L, A, ALL;
	}



	/**
	 * Basic constructor for PixelAudio, sets up all variables.
	 * @param gen 	A PixelMapGenINF instance -- should be initialized already.
	 */
	public PixelAudioMapper(PixelMapGen gen) {
		this.generator = gen;
		this.width = gen.getWidth();
		this.height = gen.getHeight();
		this.mapSize = gen.getSize();
		this.signalToImageLUT = gen.getPixelMapCopy();		// value at signal index returns index to pixel in bitmap
		this.imageToSignalLUT = gen.getSampleMapCopy();		// 
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
		return "Parent class for PixelAudioMapper objects, with documentation in its comments.";
	}


	//------------- LUTs -------------//


	/** @return the lookup table that maps an index in the signal to the corresponding pixel index in the image. */
	public int[] getSignalToImageLUT() {
		return this.signalToImageLUT;
	}

	/**
	 * Sets a new lookup table for mapping signal to image.
	 * Warning: The size of sigLUT must conform to the size the current image and signal arrays.
	 * @param sigLUT
	 */
	protected void setSignalToImageLUT(int[] sigLUT) {
		if (sigLUT.length != this.signalToImageLUT.length) 
			throw(new IllegalArgumentException("The new signalToImageLUT array must be the same size as the old signalToImageLUT array."));
		this.signalToImageLUT = sigLUT;
	}

	/** @return the lookup table that maps pixel values in the image to the corresponding entry in the signal. */
	public int[] getImageToSignalLUT() {
		return this.imageToSignalLUT;
	}

	/**
	 * Sets a new lookup table for mapping image to signal.
	 * Warning: the size of imgLUT must conform to the size the current image and signal arrays.
	 * @param imgLUT
	 */
	protected void setImageToSignalLUT(int[] imgLUT) {
		if (imgLUT.length != this.imageToSignalLUT.length) 
			throw(new IllegalArgumentException("The new imageToSignalLUT array must be the same size as the old imageToSignalLUT array."));
		this.imageToSignalLUT = imgLUT;
	}

	public ArrayList<int[]> getGeneratorCoordinatesCopy() {
		return this.generator.getCoordinatesCopy();
	}
	
	public String getGeneratorDescription() {
		return this.generator.describe();
	}

	public PixelMapGen getGenerator() {
		return this.generator;
	}
	
	public void setGenerator(PixelMapGen newGen) {
		this.generator = newGen;
		this.regenerate();
	}
	
	/**
	 * Calls generator.generate() to recreate coordinates and LUTs.
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
	
	
	//------------- MAPPING -------------//

	// TODO rewrite method signatures using SOURCE, TARGET, ARGS or SOURCE, LUT, TARGET, ARGS ordering.
	// Names and calls won't change, but change the documentation, too.
	// E.g., mapImgToSig(int[] img, float[] sig);
	// I've been doing this but it should be part of the review before publication.
	
	
	public int[] remapPixels(int[] img, int[] lut) {
		int[] newPixels = new int[img.length];
		for (int i = 0; i < img.length; i++) {
			newPixels[i] = img[lut[i]];
		}
		return newPixels;
	}

	
	public float[] remapSamples(float[] sig, int[] lut) {
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
	 * @param sig source array of floats in the audio range  [-1.0, 1.0]
	 * @param img target array of RGB pixel values
	 */
	public void mapSigToImg(float[] sig, int[] img) {
		this.pushAudioPixel(sig, img, imageToSignalLUT, ChannelNames.ALL);		// calls our utility method's grayscale conversion
	}

	/**
	 * Map signal values to a specified channel in the image.
	 * On completion, img[] contains new values.
	 * The img array and the sig array must be the same size.
	 *
	 * @param sig			an array of floats in the audio range  [-1.0, 1.0]
	 * @param img			an array of RGB pixel values
	 * @param toChannel		the channel to write transcoded values to
	 */
	public void mapSigToImg(float[] sig, int[] img, ChannelNames toChannel) {
		this.pushAudioPixel(sig, img, imageToSignalLUT, toChannel);				// call our utility method with toChannel
	}

	/**
	 * Map current image pixel values to the signal, updating the signal array.
	 * There are several ways to do this derive a value we want from the image: we use
	 * the brightness channel in the HSB color space. On completion, sig[] contains new values.
	 * The img array and the sig array must be the same size.
	 *
	 * @param sig			an array of floats in the audio range  [-1.0, 1.0]
	 * @param img			an array of RGB pixel values
	 */
	public void mapImgToSig(int[] img, float[] sig) {
		this.pullPixelAudio(img, imageToSignalLUT, sig, ChannelNames.ALL);
	 }

	/**
	 * Map current image pixel values to the signal, updating the signal array, deriving
	 * a value from specified color channel of the image. On completion, sig[] contains new values.
	 * The img array and the sig array must be the same size.
	 *
	 * @param sig			an array of floats in the audio range [-1.0, 1.0]
	 * @param img			an array of RGB pixel values
	 * @param fromChannel	the color channel to get a value from
	 */
	public void mapImgToSig(int[] img, float[] sig, ChannelNames fromChannel) {
		this.pullPixelAudio(img, imageToSignalLUT, sig, fromChannel);
	 }

	/**
	 * Writes transcoded pixel values directly to the signal, without using a LUT to redirect. V
	 * Values are calculated with the standard luminosity equation, <code>gray = 0.3 * red + 0.59 * green + 0.11 * blue</code>.
	 *
	 * @param img		source array of RGB pixel values
	 * @param sig		target array of audio samples in the range [-1.0, 1.0]
	 */
	public void writeImgToSig(int[] img, float[] sig) {
		this.pullPixelAudio(img, sig, ChannelNames.ALL);
	 }

	/**
	 * @param img			an array of RGB pixel values, source
	 * @param sig			an array of audio samples in the range [-1.0, 1.0], target
	 * @param fromChannel	channel in RGB or HSB color space, from ChannelNames enum
	 */
	public void writeImgToSig(int[] img, float[] sig, ChannelNames fromChannel) {
		 this.pullPixelAudio(img, sig, fromChannel);
	 }

	/**
	 * @param sig		an array of audio samples in the range [-1.0, 1.0], source
	 * @param img		an array of RGB pixel values, target
	 */
	public void writeSigToImg(float[] sig, int[] img) {
		 this.pushAudioPixel(sig, img, ChannelNames.ALL);
	 }

	 /**
	 * @param sig			an array of audio samples in the range [-1.0, 1.0], source
	 * @param img			an array of RGB pixel values, target
	 * @param toChannel		channel in RGB or HSB color space, from ChannelNames enum
	 */
	public void writeSigToImg(float[] sig, int[] img, ChannelNames toChannel) {
		 this.pushAudioPixel(sig, img, toChannel);
	 }



	//------------- TRANSCODING -------------//

	/* TODO do we replace all identical calls to map() in transcode with calls to transcode?
	 * This would seem to be a Good Idea, because then we can override transcode in child classes
	 * to get new behavior across all methods that involve transcoding.
	 */

	 /**
	 * Converts a float value in the range  [-1.0, 1.0] to an int value in the range [0..255].
	 *
	 * @param val	a float value in the range  [-1.0, 1.0]
	 * @return		an int mapped to the range [0..255]
	 */
	public int transcode(float val) {
		 float vout = PixelAudio.map(val, -1.0f, 1.0f, 0, 255);
		 return Math.round(vout);
	 }

	 /**
	 * Converts an int value in the range [0..255] to a float value in the range [-1.0, 1.0].
	 *
	 * @param val	an int int he range [0..255]
	 * @return		a float mapped to the range [-1.0, 1.0]
	 */
	public float transcode(int val) {
		 float vout = PixelAudio.map(val, 0, 255, -1.0f, 1.0f);
		 return vout;
	 }

	
	// ------------- PIXEL AND SAMPLE LOOKUP ------------- //
	
	public int lookupSample(int x, int y) {
		return this.imageToSignalLUT[x + y * this.width];
	}
	
	public int lookupSample(int imagePos) {
		return this.imageToSignalLUT[imagePos];
	}
	
	public int lookupPixel(int signalPos) {
		return this.signalToImageLUT[signalPos];
	}

	public int[] lookupCoordinate(int signalPos) {
		signalPos = this.signalToImageLUT[signalPos];
		int x =  signalPos % this.width;
		int y = signalPos / this.width;
		return new int[] {x, y};
	}

	//------------- SUBARRAYS -------------//

	/*
	 * In each case, a source subarray is either extracted from or inserted into a target larger array.
	 * When the small array, sprout, is inserted, it is indexed from 0..sprout.length. The larger array,
	 * img or sig, is indexed from read or write point signalPos to signalPos + length.
	 *
	 * All float[] arrays should contain audio range values [-1.0, 1.0].
	 * All int[] arrays should contain RGB pixel values.
	 *
	 */


	  /**
	   * Starting at <code>signalPos</code>, reads <code>length</code> values from pixel array <code>img</code> in signal order
	   * using <code>signalToImageLUT</code> to redirect indexing and then returns them as an array of RGB pixel values in signal order.
	   * Note that <code>signalPos = this.imageToSignalLUT[x + y * this.width]</code> or 
	   *
	   * @param img      	array of RGB values, the pixels array from a bitmap image with the same width and height as PixelAudioMapper
	   * @param signalPos   position in the signal at which to start reading pixel values from the image, following the signal path
	   * @param length    	length of the subarray to pluck from img, reading pixel values while following the signal path
	   * @return        	a new array of pixel values in signal order
	   */
	  public int[] pluckPixels(int[] img, int signalPos, int length) {
		// We can have an error condition if signalPos + length exceeds img.length! 
		// If signalPos exceeds length, we return null. Let the caller take note and remedy the problem.
		if (signalPos + length > img.length) {
			length = img.length - signalPos;
			System.out.println("WARNING! signalPos + length exceeded img array length. Length was trimmed to "+ length);
		}
		if (signalPos >= img.length) {
			System.out.println("WARNING! signalPos "+ signalPos +" exceeded img length "+ img.length +". Returning null.");
			return null;
		}
	    int[] pixels = new int[length];         // new array for pixel values
	    int j = 0;                    			// index for pixels array 
	    for (int i = signalPos; i < signalPos + length; i++) {  // step through the signal with i as index    
	      int rgb = img[this.signalToImageLUT[i]];  			// get an rgb value from img at position signalToImageLUT[i]
	      pixels[j++] = rgb;              		// accumulate values in pixels array
	    }
	    return pixels;                  		// return the samples
	  }


	  /*
	// It's not clear to me when this signature might be useful. Not yet, anyhow.
	 * It's supposed to return one channel of RGB, and that might be useful, after all. Implement it. TODO
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
	 * @param img			source array of RGB pixel values, typically from the bitmap image you are using with PixelAudioMapper
	 * @param signalPos		position in the signal at which to start reading pixel values from the image, following the signal path
	 * @param length		length of the subarray to pluck from img
	 * @param fromChannel	the color channel from which to read pixel values
	 * @return				a new array of audio values in signal order
	 */
	public float[] pluckPixelsAsAudio(int[] img, int signalPos, int length, ChannelNames fromChannel) {
		// We can have an error condition if signalPos + length exceeds img.length! 
		// If signalPos exceeds length, we return null. Let the caller take note and remedy the problem.
		if (signalPos + length > img.length) {
			length = img.length - signalPos;
			System.out.println("WARNING! signalPos + length exceeded img array length. Length was trimmed to "+ length);
		}
		if (signalPos >= img.length) {
			System.out.println("WARNING! signalPos "+ signalPos +" exceeded img length "+ img.length +". Returning null.");
			return null;
		}
		float[] samples = new float[length];							// create an array of samples
		int j = 0;														// TODO we can have an error condition if signalPos + length exceeds img.length!
		switch (fromChannel) {
		case L: {
			for (int i = signalPos; i < signalPos + length; i++) {
				int rgb = img[this.signalToImageLUT[i]];						// get the RGB value from the pixel associated with the signal position
				samples[j++] = PixelAudio.map(brightness(rgb, hsbPixel), 0, 1, -1.0f, 1.0f);	// extract brightness and map it to the audio range
			}																	
			break;
		}
		case H: {
			for (int i = signalPos; i < signalPos + length; i++) {
				int rgb = img[this.signalToImageLUT[i]];						// get the RGB value from the pixel associated with the signal position
				samples[j++] = PixelAudio.map(hue(rgb, hsbPixel), 0, 1, -1.0f, 1.0f);	// extract hue and map it to the audio range
			}
			break;
		}
		case S: {
			for (int i = signalPos; i < signalPos + length; i++) {
				int rgb = img[this.signalToImageLUT[i]];				// get the RGB value from the pixel associated with the signal position
				samples[j++] = PixelAudio.map(saturation(rgb, hsbPixel), 0, 1, -1.0f, 1.0f);	// extract saturation and map it to the audio range
			}
			break;
		}
		case R: {
			for (int i = signalPos; i < signalPos + length; i++) {
				int rgb = img[this.signalToImageLUT[i]];				// get the RGB value from the pixel associated with the signal position
				samples[j++] = PixelAudio.map(((rgb >> 16) & 0xFF), 0, 255, -1.0f, 1.0f);	// extract red component and map it to the audio range
			}
			break;
		}
		case G: {
			for (int i = signalPos; i < signalPos + length; i++) {
				int rgb = img[this.signalToImageLUT[i]];				// get the RGB value from the pixel associated with the signal position
				samples[j++] = PixelAudio.map(((rgb >> 8) & 0xFF), 0, 255, -1.0f, 1.0f);	// extract green component and map it to the audio range
			}
			break;
		}
		case B: {
			for (int i = signalPos; i < signalPos + length; i++) {
				int rgb = img[this.signalToImageLUT[i]];				// get the RGB value from the pixel associated with the signal position
				samples[j++] = PixelAudio.map((rgb & 0xFF), 0, 255, -1.0f, 1.0f);	// extract blue component and map it to the audio range
			}
			break;
		}
		case A: {
			for (int i = signalPos; i < signalPos + length; i++) {
				int rgb = img[this.signalToImageLUT[i]];				// get the RGB value from the pixel associated with the signal position
				samples[j++] = PixelAudio.map(((rgb >> 24) & 0xFF), 0, 255, -1.0f, 1.0f);    // extract alpha component and map it to the audio range
			}
			break;
		}
		case ALL: {
            // not a simple case, but we'll convert to grayscale using the "luminosity equation."
			// The brightness value in HSB (case L, above) or the L channel in Lab color spaces could be used, too.
			for (int i = signalPos; i < signalPos + length; i++) {
				int rgb = img[this.signalToImageLUT[i]];				// get the RGB value from the pixel associated with the signal position
				samples[j++] = PixelAudio.map((0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF)), 0, 255, -1.0f, 1.0f);
			}
			break;
		}
		}
		return samples;
	}

	/**
     * Starting at <code>signalPos</code>, reads <code>length</code> values from float array <code>sig</code> 
	 * and returns a new array of audio values in signal order. No redirection is needed when reading from the signal.
     * Note that <code>signalPos = this.imageToSignalLUT[x + y * this.width]</code> or <code>this.lookupSample(x, y)</code>.
	 *
	 * @param sig			source array of audio values
	 * @param signalPos		a position in the sig array
	 * @param length		number of values to read from sig array
	 * @return				a new array with the audio values we read
	 */
	public float[] pluckSamples(float[] sig, int signalPos, int length) {
		// We can have an error condition if signalPos + length exceeds sig.length! 
		// If signalPos exceeds length, we return null. Let the caller take note and remedy the problem.
		if (signalPos + length > sig.length) {
			length = sig.length - signalPos;
			System.out.println("WARNING! signalPos + length exceeded sig array length. Length was trimmed to "+ length);
		}
		if (signalPos >= sig.length) {
			System.out.println("WARNING! signalPos "+ signalPos +" exceeded img length "+ sig.length +". Returning null.");
			return null;
		}
		float[] samples = new float[length];
		int j = 0;
		for (int i = signalPos; i < signalPos + length; i++) {
			samples[j++] = sig[i];
		}
		return samples;
	}

	/**
     * Starting at <code>signalPos</code>, reads <code>length</code> values from float array <code>sig</code> 
     * and transcodes and returns them as an RGB array in signal order.
     * Note that <code>signalPos = this.imageToSignalLUT[x + y * this.width]</code> or <code>this.lookupSample(x, y)</code>.
	 *
	 * @param sig			source array of audio values (-1.0f..1.0f)
	 * @param signalPos		entry point in the sig array
	 * @param length		number of values to read from the sig array
	 * @return				an array of RGB values where r == g == b, derived from the sig values
	 */
	public int[] pluckSamplesAsRGB(float[] sig, int signalPos, int length) {
		// We can have an error condition if signalPos + length exceeds sig.length! 
		// If signalPos exceeds length, we return null. Let the caller take note and remedy the problem.
		if (signalPos + length > sig.length) {
			length = sig.length - signalPos;
			System.out.println("WARNING! signalPos + length exceeded sig array length. Length was trimmed to "+ length);
		}
		if (signalPos >= sig.length) {
			System.out.println("WARNING! signalPos "+ signalPos +" exceeded img length "+ sig.length +". Returning null.");
			return null;
		}
		int[] rgbPixels = new int[length];
		int j = 0;
		for (int i = signalPos; i < signalPos + length; i++) {
			float sample = sig[i];
			sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;				// a precaution, keep values within limits
			int v = Math.round(PixelAudio.map(sample, -1.0f, 1.0f, 0, 255));		// map from [-1.0, 1.0] to [0, 255]
			rgbPixels[j++] = 255 << 24 | v << 16 | v << 8 | v;						// an opaque RGB gray (r == g == b)
		}
		return rgbPixels;
	}


	/**
	 * Starting at <code>signalPos</code>, writes <code>length</code> values from RGB array <code>sprout</code> 
	 * into RGB array <code>img</code>, in signal order. <code>img</code> is typically the pixel array from a PImage 
	 * with the same width and height as PixemAudioMapper. 
     * Note that <code>signalPos = this.imageToSignalLUT[x + y * this.width]</code> or <code>this.lookupSample(x, y)</code>.
	 *
	 * @param sprout		source array of RGB values to insert into target array img, in signal order
	 * @param img			target array of RGB values. in image (row major) order
	 * @param signalPos   	position in the signal at which to start writing pixel values to the image, following the signal path
	 * @param length		number of values from sprout to insert into img array
	 */
	public void plantPixels(int[] sprout, int[] img, int signalPos, int length) {
		int j = 0;														// index for sprout
		for (int i = signalPos; i < signalPos + length; i++) {			// step through signal positions    TODO if signalPos + length > img.length we're in trouble
			img[signalToImageLUT[i]] = sprout[j++];						// assign values from sprout to img
		}
	}


	/**
	 * Starting at <code>signalPos</code>, writes <code>length</code> values from RGB array <code>sprout</code> 
	 * into a specified channel of RGB array <code>img</code>, in signal order.
     * Note that <code>signalPos = this.imageToSignalLUT[x + y * this.width]</code> or <code>this.lookupSample(x, y)</code>.
	 *
	 * @param sprout	source array of RGB values to insert into target array img, in signal order
	 * @param img		target array of RGB values, in image order
	 * @param x			x coordinate in image from which img pixel array was derived
	 * @param y			y coordinate in image from which img pixel array was derived
	 * @param length	number of values from sprout to insert into img array
	 */
	public void plantPixels(int[] sprout, int[] img, int signalPos, int length, ChannelNames toChannel) {  
		// We can have an error condition if signalPos + length exceeds img.length! 
		// If signalPos exceeds length, we return null. Let the caller take note and remedy the problem.
		if (signalPos + length > img.length) {
			length = img.length - signalPos;
			System.out.println("WARNING! signalPos + length exceeded img array length. Length was trimmed to "+ length);
		}
		if (signalPos >= img.length) {
			System.out.println("WARNING! signalPos "+ signalPos +" exceeded img length "+ img.length +". Returning null.");

		}
		int j = 0;		// index for sprout
		switch (toChannel) {
		case L: {
			for (int i = signalPos; i < signalPos + length; i++) {		
				int rgb = img[this.signalToImageLUT[i]];											// get the RGB value in img that is mapped to signal position i
				Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	    // encode RGB value as HSB in hsbPixel array
				rgb = Color.HSBtoRGB(hsbPixel[0], hsbPixel[1], brightness(sprout[j]));				// encode HSB back to RGB, with brightness value from sprout
				img[this.signalToImageLUT[i]] = rgb;												// write the new value to the img array
				j++;
			}
			break;
		}
		case H: {
			for (int i = signalPos; i < signalPos + length; i++) {
				int rgb = img[this.signalToImageLUT[i]];			// get the RGB value in img that is mapped to signal position i
				Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
				rgb = Color.HSBtoRGB(hue(sprout[j]), hsbPixel[1], hsbPixel[2]);			// encode HSB back to RGB, with hue value from sprout
				img[this.signalToImageLUT[i]] = rgb;
				j++;
			}
			break;
		}
		case S: {
			for (int i = signalPos; i < signalPos + length; i++) {
				int rgb = img[this.signalToImageLUT[i]];			// get the RGB value in img that is mapped to signal position i
				Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
				rgb = Color.HSBtoRGB(hsbPixel[0], saturation(sprout[j]), hsbPixel[2]);	// encode HSB back to RGB, with saturation value from sprout
				img[this.signalToImageLUT[i]] = rgb;
				j++;
			}
			break;
		}
		case R: {
			for (int i = signalPos; i < signalPos + length; i++) {
				int rgb = img[this.signalToImageLUT[i]];			// get the RGB value in img that is mapped to signal position i
				int r = (sprout[j] >> 16) & 0xFF;					// get red value from sprout
				img[this.signalToImageLUT[i]] = 255 << 24 | r << 16 | ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF;	// replace red channel
				j++;
			}
			break;
		}
		case G: {
				for (int i = signalPos; i < signalPos + length; i++) {
				int rgb = img[this.signalToImageLUT[i]];			// get the RGB value in img that is mapped to signal position i
				int g = (sprout[j] >> 8) & 0xFF;					// get the green value from sprout
				img[this.signalToImageLUT[i]] = 255 << 24 | ((rgb >> 16) & 0xFF) << 16 | g << 8 | rgb & 0xFF;	// replace green channel
				j++;
			}
			break;
		}
		case B: {
			for (int i = signalPos; i < signalPos + length; i++) {
				int rgb = img[this.signalToImageLUT[i]];			// get the RGB value in img that is mapped to signal position i
				int b = sprout[j] & 0xFF;							// get blue value from sprout
				img[this.signalToImageLUT[i]] = 255 << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | b & 0xFF;		// replace blue channel
				j++;
			}
			break;
		}
		case A: {
			for (int i = signalPos; i < signalPos + length; i++) {
				int rgb = img[this.signalToImageLUT[i]];			// get the RGB value in img that is mapped to signal position i
				int a = (rgb >> 24) & 0xFF;							// get alpha value from sprout
				img[this.signalToImageLUT[i]] = a << 24 | ((rgb >> 16) & 0xFF) << 16| ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF;		// replace alpha channel
				j++;
			}
			break;
		}
		case ALL: {
			for (int i = signalPos; i < signalPos + length; i++) {
				img[signalToImageLUT[i]] = sprout[j++];				// replace RGB value in img with RGB value in sprout
			}
			break;
		}
		} // end switch
	}


	/**
	 * Starting at <code>signalPos</code>, writes <code>length</code> transcoded values from audio sample array <code>sprout</code> 
	 * into a specified channel of RGB array <code>img</code>, in signal order.
     * Note that <code>signalPos = this.imageToSignalLUT[x + y * this.width]</code> or <code>this.lookupSample(x, y)</code>.
	 *
	 * Inserts elements from a source array of audio values (-1.0f..1.0f) into a specified color channel
	 * in a target array of RGB pixel values following the signal path.
	 *
	 * @param sprout	source array of RGB values to insert into target array img, in signal order
	 * @param img		target array of RGB values, in image order
	 * @param x			x coordinate in image from which img pixel array was derived
	 * @param y			y coordinate in image from which img pixel array was derived
	 * @param length	number of values from sprout to insert into img array
	 */
	public void plantPixels(float[] sprout, int[] img, int signalPos, int length, ChannelNames toChannel) {
		int j = 0;
		switch (toChannel) {
		case L: {
			for (int i = signalPos; i < signalPos + length; i++) {
				img[this.signalToImageLUT[i]] = this.applyBrightness(sprout[j++], img[this.signalToImageLUT[i]]);
			}
			break;
		}
		case H: {
			for (int i = signalPos; i < signalPos + length; i++) {
				img[this.signalToImageLUT[i]] = this.applyHue(sprout[j++], img[this.signalToImageLUT[i]]);
			}
			break;
		}
		case S: {
			for (int i = signalPos; i < signalPos + length; i++) {
				img[this.signalToImageLUT[i]] = this.applySaturation(sprout[j++], img[this.signalToImageLUT[i]]);
			}
			break;
		}
		case R: {
			for (int i = signalPos; i < signalPos + length; i++) {
				img[this.signalToImageLUT[i]] = this.applyRed(sprout[j++], img[this.signalToImageLUT[i]]);
			}
			break;
		}
		case G: {
			for (int i = signalPos; i < signalPos + length; i++) {
				img[this.signalToImageLUT[i]] = this.applyGreen(sprout[j++], img[this.signalToImageLUT[i]]);
			}
			break;
		}
		case B: {
			for (int i = signalPos; i < signalPos + length; i++) {
				img[this.signalToImageLUT[i]] = this.applyBlue(sprout[j++], img[this.signalToImageLUT[i]]);
			}
			break;
		}
		case A: {
			for (int i = signalPos; i < signalPos + length; i++) {
				img[this.signalToImageLUT[i]] = this.applyAlpha(sprout[j++], img[this.signalToImageLUT[i]]);
			}
			break;
		}
		case ALL: {
			for (int i = signalPos; i < signalPos + length; i++) {
				img[this.signalToImageLUT[i]] = this.applyAll(sprout[j++], img[this.signalToImageLUT[i]]);
			}
			break;
		}
		} // end switch
	}

	/**
	 * Starting at <code>signalPos</code>, insert <code>length</code> audio samples from source array <code>sprout</code> 
	 * into target array of audio samples <code>sig</code>. No redirection by lookup tables is used.
	 *
	 * @param sprout		source array of audio values (-1.0f..1.0f)
	 * @param sig			target array of signal values, in signal order
	 * @param signalPos		start point in sig array
	 * @param length		number of values to copy from sprout array to sig array
	 */
	public void plantSamples(float[] sprout, float[] sig, int signalPos, int length) {
		int j = 0;
		for (int i = signalPos; i < signalPos + length; i++) {
			sig[i] = sprout[j++];
		}
	}

	/**
	 * Starting at <code>signalPos</code>, insert <code>length</code> transcoded RGB samples from source array <code>sprout</code> 
	 * into target array of audio samples <code>sig</code>. No redirection by lookup tables is is used.
	 *
	 * @param sprout
	 * @param sig
	 * @param signalPos
	 * @param length
	 */
	public void plantSamples(int[] sprout, float[] sig, int signalPos, int length) {
		int j = 0;
		for (int i = signalPos; i < signalPos + length; i++) {
			sig[i] = PixelAudio.map(PixelAudioMapper.getGrayscale(sprout[j++]), 0, 255, -1.0f, 1.0f);
		}
	}

	/**
	 * Starting at <code>signalPos</code>, insert <code>length</code> transcoded RGB samples from channel <code>fromChannel</code> 
	 * of source array <code>sprout</code> into target array of audio samples <code>sig</code>. 
	 * No redirection by lookup tables is is used.
	 *
	 * @param sprout
	 * @param sig
	 * @param signalPos
	 * @param length
	 * @param fromChannel
	 */
	public void plantSamples(int[] sprout, float[] sig, int signalPos, int length, ChannelNames fromChannel) {
		int j = 0;
		switch (fromChannel) {
		case L: {
			for (int i = signalPos; i < signalPos + length; i++) {
				sig[i] = PixelAudio.map(brightness(sprout[j++]), 0, 1, -1.0f, 1.0f);
			}
			break;
		}
		case H: {
			for (int i = signalPos; i < signalPos + length; i++) {
				sig[i] = PixelAudio.map(hue(sprout[j++]), 0, 1, -1.0f, 1.0f);
			}
			break;
		}
		case S: {
			for (int i = signalPos; i < signalPos + length; i++) {
				sig[i] = PixelAudio.map(saturation(sprout[j++]), 0, 1, -1.0f, 1.0f);
			}
			break;
		}
		case R: {
			for (int i = signalPos; i < signalPos + length; i++) {
				sig[i] = PixelAudio.map(((sprout[j++] >> 16) & 0xFF), 0, 255, -1.0f, 1.0f);
			}
			break;
		}
		case G: {
			for (int i = signalPos; i < signalPos + length; i++) {
				sig[i] = PixelAudio.map(((sprout[j++] >> 8) & 0xFF), 0, 255, -1.0f, 1.0f);
			}
			break;
		}
		case B: {
			for (int i = signalPos; i < signalPos + length; i++) {
				sig[i] = PixelAudio.map((sprout[j++] & 0xFF), 0, 255, -1.0f, 1.0f);
			}
			break;
		}
		case A: {
			for (int i = signalPos; i < signalPos + length; i++) {
				sig[i] = PixelAudio.map(((sprout[j++] >> 24) & 0xFF), 0, 255, -1.0f, 1.0f);
			}
			break;
		}
		case ALL: {
			for (int i = signalPos; i < signalPos + length; i++) {
				// convert to grayscale using the "luminosity equation."
				sig[i] = PixelAudio.map((0.3f * ((sprout[i] >> 16) & 0xFF)
						+ 0.59f * ((sprout[i] >> 8) & 0xFF)
						+ 0.11f * (sprout[i] & 0xFF)), 0, 255, -1.0f, 1.0f);
				j++;
			}
			break;
		}
		}

	}

	/**
	 * Copy a rectangular area of pixels in image (row major) order and return it as an array of RGB values.
	 * This is a standard image method, for example, public PImage get(int x, int y, int w, int h) in Processing.
	 * TODO How much error checking do we want in the pluck/plant/peel/stamp methods?
	 * If we check, do we fall through or post an error message?
	 *
	 * @param img
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @return
	 */
	public int[] peelPixels(int[] img, int x, int y, int w, int h) {
		int len = w * h;
		int[] rgbPixels = new int[len];
		int j = 0;
		for (int dy = y; dy < dy + h; dy++) {
			for (int dx = x; dx < x + w; dx++) {
				rgbPixels[j++] = img[dx + dy * w];
			}
		}
		return rgbPixels;
	}

	/**
	 * Copy a rectangular area of pixels in image (row major) order and return it as an array of audio values (-1.0f..1.0f).
	 *
	 * @param img
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @return
	 */
	public float[] peelPixelsAsAudio(int[] img, int x, int y, int w, int h) {
		int len = w * h;
		float[] samples = new float[len];
		int j = 0;
		for (int dy = y; dy < dy + h; dy++) {
			for (int dx = x; dx < x + w; dx++) {
				samples[j++] =  PixelAudio.map(PixelAudioMapper.getGrayscale(img[dx + dy * w]), 0, 255, -1.0f, 1.0f);
			}
		}
		return samples;
	}

	/**
	 * Follow the coordinates of rectangle defined by x, y, w, h and return the corresponding signal values.
	 *
	 * @param sig
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @return
	 */
	public float[] peelSamples(float[] sig, int x, int y, int w, int h) {
		int len = w * h;
		float[] samples = new float[len];
		int j = 0;
		for (int dy = y; dy < dy + h; dy++) {
			for (int dx = x; dx < x + w; dx++) {
				samples[j++] =  sig[this.imageToSignalLUT[dx + dy * w]];
			}
		}
		return samples;
	}

	public int[] peelSamplesAsRGB(float[]sig, int x, int y, int w, int h) {
		int len = w * h;
		int[] rgbPixels = new int[len];
		int j = 0;
		for (int dy = y; dy < dy + h; dy++) {
			for (int dx = x; dx < x + w; dx++) {
				rgbPixels[j++] =  Math.round(PixelAudio.map(sig[this.imageToSignalLUT[dx + dy * w]], 0, 255, -1.0f, 1.0f));
			}
		}
		return rgbPixels;
	}

/*
	// I don't think this makes much sense, as far as eventual usefulness
    public int[] peelSamplesAsRGB(float[]sig, int pos, int length, ChannelNames toChannel) {

	}
*/


	public void stampPixels(int[] stamp, int[] img, int x, int y, int w, int h) {
		int j = 0;
		for (int dy = y; dy < dy + h; dy++) {
			for (int dx = x; dx < x + w; dx++) {
				img[dx + dy * w] = stamp[j++];
			}
		}
	}

	public void stampPixels(int[] stamp, int[] img, int x, int y, int w, int h, ChannelNames toChannel) {
		int j = 0;
		switch (toChannel) {
		case L: {
			for (int dy = y; dy < dy + h; dy++) {
				for (int dx = x; dx < x + w; dx++) {
					int rgb = img[dx + dy * w];
					Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
					rgb = Color.HSBtoRGB(hsbPixel[0], hsbPixel[1], brightness(stamp[j], hsbPixel));
					img[dx + dy * w] = rgb;
					j++;
				}
			}
			break;
		}
		case H: {
			for (int dy = y; dy < dy + h; dy++) {
				for (int dx = x; dx < x + w; dx++) {
					int rgb = img[dx + dy * w];
					Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
					rgb = Color.HSBtoRGB(hue(stamp[j], hsbPixel), hsbPixel[1], hsbPixel[2]);
					img[dx + dy * w] = rgb;
					j++;
				}
			}
			break;
		}
		case S: {
			for (int dy = y; dy < dy + h; dy++) {
				for (int dx = x; dx < x + w; dx++) {
					int rgb = img[dx + dy * w];
					Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
					rgb = Color.HSBtoRGB(hsbPixel[0], saturation(stamp[j], hsbPixel), hsbPixel[2]);
					img[dx + dy * w] = rgb;
					j++;
				}
			}
			break;
		}
		case R: {
			for (int dy = y; dy < dy + h; dy++) {
				for (int dx = x; dx < x + w; dx++) {
					int rgb = img[dx + dy * w];
					int r = (stamp[j] << 16) & 0xFF;
					img[dx + dy * w] = 255 << 24 | r << 16 | ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF;

					j++;
				}
			}
			break;
		}
		case G: {
			for (int dy = y; dy < dy + h; dy++) {
				for (int dx = x; dx < x + w; dx++) {
					int rgb = img[dx + dy * w];
					int g = (stamp[j] << 8) & 0xFF;
					img[dx + dy * w] = 255 << 24 | ((rgb >> 16) & 0xFF) << 16 | g << 8 | rgb & 0xFF;
					j++;
				}
			}
			break;
		}
		case B: {
			for (int dy = y; dy < dy + h; dy++) {
				for (int dx = x; dx < x + w; dx++) {
					int rgb = img[dx + dy * w];
					int b = stamp[j] & 0xFF;
					img[dx + dy * w] = 255 << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | b & 0xFF;
					j++;
				}
			}
			break;
		}
		case A: {
			for (int dy = y; dy < dy + h; dy++) {
				for (int dx = x; dx < x + w; dx++) {
					int rgb = img[dx + dy * w];
					int a = stamp[j] << 24;
					img[dx + dy * w] = a << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF;
					j++;
				}
			}
			break;
		}
		case ALL: {
			for (int dy = y; dy < dy + h; dy++) {
				for (int dx = x; dx < x + w; dx++) {
					img[dx + dy * w] = stamp[j];
					j++;
				}
			}
			break;
		}
		}
	}

	public void stampPixels(float[] stamp, int[] img, int x, int y, int w, int h, ChannelNames toChannel) {
		int j = 0;
		switch (toChannel) {
			case L: {
				for (int dy = y; dy < dy + h; dy++) {
					for (int dx = x; dx < x + w; dx++) {
						img[dx + dy * w] = this.applyBrightness(stamp[j++], img[dx + dy * w]);
					}
				}
				break;
			}
			case H: {
				for (int dy = y; dy < dy + h; dy++) {
					for (int dx = x; dx < x + w; dx++) {
						img[dx + dy * w] = this.applyHue(stamp[j++], img[dx + dy * w]);
					}
				}
				break;
			}
			case S: {
				for (int dy = y; dy < dy + h; dy++) {
					for (int dx = x; dx < x + w; dx++) {
						img[dx + dy * w] = this.applySaturation(stamp[j++], img[dx + dy * w]);
					}
				}
				break;
			}
			case R: {
				for (int dy = y; dy < dy + h; dy++) {
					for (int dx = x; dx < x + w; dx++) {
						img[dx + dy * w] = this.applyRed(stamp[j++], img[dx + dy * w]);
					}
				}
				break;
			}
			case G: {
				for (int dy = y; dy < dy + h; dy++) {
					for (int dx = x; dx < x + w; dx++) {
						img[dx + dy * w] = this.applyGreen(stamp[j++], img[dx + dy * w]);
					}
				}
				break;
			}
			case B: {
				for (int dy = y; dy < dy + h; dy++) {
					for (int dx = x; dx < x + w; dx++) {
						img[dx + dy * w] = this.applyBlue(stamp[j++], img[dx + dy * w]);
					}
				}
				break;
			}
			case A: {
				for (int dy = y; dy < dy + h; dy++) {
					for (int dx = x; dx < x + w; dx++) {
						img[dx + dy * w] = this.applyAlpha(stamp[j++], img[dx + dy * w]);
					}
				}
				break;
			}
			case ALL: {
				for (int dy = y; dy < dy + h; dy++) {
					for (int dx = x; dx < x + w; dx++) {
						img[dx + dy * w] = this.applyAll(stamp[j++], img[dx + dy * w]);
					}
				}
				break;
			}
			}
		}

	public void stampSamples(float[] stamp, float[] sig, int x, int y, int w, int h) {
		int j = 0;
		for (int dy = y; dy < dy + h; dy++) {
			for (int dx = x; dx < x + w; dx++) {
				sig[this.imageToSignalLUT[dx + dy * w]] = stamp[j++];
			}
		}
	}

	public void stampSamples(int[] stamp, float[] sig, int x, int y, int w, int h) {
		int j = 0;
		for (int dy = y; dy < dy + h; dy++) {
			for (int dx = x; dx < x + w; dx++) {
				sig[this.imageToSignalLUT[dx + dy * w]] = transcode(stamp[j++]);
			}
		}
	}


	// ------------- ARRAY ROTATION ------------- //

	/**
	 * Rotates an array of ints left by d values. Uses efficient "Three Rotation" algorithm.
	 *
	 * @param arr array of ints to rotate
	 * @param d   number of elements to shift
	 */
	public static final void rotateLeft(int[] arr, int d) {
		int len = arr.length;
		if (d < 0) d = len - (-d % len);
		d = d % len;
		reverseArray(arr, 0, d - 1);
		reverseArray(arr, d, len - 1);
		reverseArray(arr, 0, len - 1);
	}

	/**
	 * Rotates an array of floats left by d values. Uses efficient "Three Rotation" algorithm.
	 *
	 * @param arr array of floats to rotate
	 * @param d   number of elements to shift
	 */
	public static final void rotateLeft(float[] arr, int d) {
		int len = arr.length;
		if (d < 0) d = len - (-d % len);
		d = d % len;
		reverseArray(arr, 0, d - 1);
		reverseArray(arr, d, len - 1);
		reverseArray(arr, 0, len - 1);
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
		comp[0] = (rgb >> 16) & 0xFF; // Faster way of getting red(rgb)
		comp[1] = (rgb >> 8) & 0xFF; // Faster way of getting green(rgb)
		comp[2] = rgb & 0xFF; // Faster way of getting blue(rgb)
		return comp;
	}

	/**
	 * Breaks a Processing color into R, G, B and A values in an array.
	 *
	 * @param argb a Processing color as a 32-bit integer
	 * @return an array of integers in the intRange [0, 255] for 3 primary color
	 *         components: {R, G, B} plus alpha
	 */
	public static final int[] rgbaComponents(int argb) {
		int[] comp = new int[4];
		comp[0] = (argb >> 16) & 0xFF; // Faster way of getting red(argb)
		comp[1] = (argb >> 8) & 0xFF; // Faster way of getting green(argb)
		comp[2] = argb & 0xFF; // Faster way of getting blue(argb)
		comp[3] = argb >> 24 & 0xFF; // alpha component
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
	 * @param rgb	The color we want to change
	 * @param rgba	The color from which we get the alpha channel (a mask, for instance)
	 * @return		A color with the RGB values from rgb and the A value from rgba
	 */
	public static final int applyAlpha(int rgb, int rgba) {
		return (rgba >> 24) << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | (rgb & 0xFF);
	}
	
	public static int setAlpha(int argb, int alpha) {
		int[] c = rgbComponents(argb);
		return alpha << 24 | c[0] << 16 | c[1] << 8 | c[2];
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
	public static final int getGrayscale(int rgb) {
        //  we'll convert to grayscale using the "luminosity equation."
		float gray = 0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF);
		return Math.round(gray);
	}


	/**
	 * @param argb		an RGB color
	 * @return			a String equivalent to a Processing color(r, g, b, a) call, such as "color(233, 144, 89, 255)"
	 */
	public static final String colorString(int argb) {
		int[] comp = rgbaComponents(argb);
		return "color(" + comp[0] + ", " + comp[1] + ", " + comp[2] + ", " + comp[3] + ")";
	}


	// -------- HSB <---> RGB -------- //

	/**
	 * Extracts the hue component from an RGB value. The result is in the range (0, 1).
	 * @param rgb		The RGB color from which we will obtain the hue component in the HSB color model.
	 * @param hsbPixel 	array of float values filled in by this method
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

	// ------------- APPLY COLOR CHANNEL METHODS ------------- //

	public int applyBrightness(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		sample = PixelAudio.map(sample, -1.0f, 1.0f, 0.0f, 1.0f);						// map audio sample to (0..1)
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(hsbPixel[0], hsbPixel[1], sample);
	}

	public int applyHue(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		sample = PixelAudio.map(sample, -1.0f, 1.0f, 0.0f, 1.0f);						// map audio sample to (0..1)
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(sample, hsbPixel[1], hsbPixel[2]);
	}

	public int applySaturation(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		sample = PixelAudio.map(sample, -1.0f, 1.0f, 0.0f, 1.0f);						// map audio sample to (0..1)
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(hsbPixel[0], sample, hsbPixel[2]);
	}

	public int applyRed(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		int r = Math.round(PixelAudio.map(sample, -1.0f, 1.0f, 0, 255));			// map audio sample to [0, 255]
		return (255 << 24 | r << 16 | ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF);		// apply to red channel
	}

	public int applyGreen(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		int g = Math.round(PixelAudio.map(sample, -1.0f, 1.0f, 0, 255));			// map audio sample to [0, 255]
		return (255 << 24 | ((rgb >> 16) & 0xFF) << 16 | g << 8 | rgb & 0xFF);		// apply to green channel
	}

	public int applyBlue(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		int b = Math.round(PixelAudio.map(sample, -1.0f, 1.0f, 0, 255));						// map audio sample to [0, 255]
		return (255 << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | b & 0xFF);	// apply to blue channel
	}

	public int applyAlpha(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		int a = Math.round(PixelAudio.map(sample, -1.0f, 1.0f, 0, 255));						// map audio sample to [0, 255]
		return (a << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF);	// apply to alpha channel
	}

	public int applyAll(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		int v = Math.round(PixelAudio.map(sample, -1.0f, 1.0f, 0, 255));			// map audio sample to [0, 255]
		return 255 << 24 | v << 16 | v << 8 | v;									// apply to all channels except alpha
	}


	// ------------- AUDIO <---> IMAGE ------------- //

	/**
	 * Converts a pixel channel value to an audio sample value, mapping the result to  [-1.0, 1.0].
	 *
	 * @param rgb		an RGB pixel value
	 * @param chan		channel to extract from the RGB pixel value
	 * @return
	 */
	public float pullPixelAudio(int rgb, ChannelNames chan) {
		float sample = 0;
		switch (chan) {
		case L: {
			sample = PixelAudio.map(brightness(rgb, hsbPixel), 0, 1, -1.0f, 1.0f);
			break;
		}
		case H: {
			sample = PixelAudio.map(hue(rgb, hsbPixel), 0, 1, -1.0f, 1.0f);
			break;
		}
		case S: {
			sample = PixelAudio.map(saturation(rgb, hsbPixel), 0, 1, -1.0f, 1.0f);
			break;
		}
		case R: {
			sample = PixelAudio.map(((rgb >> 16) & 0xFF), 0, 255, -1.0f, 1.0f);
			break;
		}
		case G: {
			sample = PixelAudio.map(((rgb >> 8) & 0xFF), 0, 255, -1.0f, 1.0f);
			break;
		}
		case B: {
			sample = PixelAudio.map((rgb & 0xFF), 0, 255, -1.0f, 1.0f);
			break;
		}
		case A: {
			sample = PixelAudio.map(((rgb >> 24) & 0xFF), 0, 255, -1.0f, 1.0f);
			break;
		}
		case ALL: {
            // not a simple case, but we'll convert to grayscale using the "luminosity equation."
			// The brightness value in HSB (case L, above) or the L channel in Lab color spaces could be used, too.
			sample = PixelAudio.map((0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF)), 0, 255, -1.0f, 1.0f);
			break;
		}
		}
		return sample;
	}


	/**
	 * Converts an array of pixel channel values to an array of audio sample values, mapping sample values
	 * to the interval [-1.0, 1.0], with no remapping of array order.
	 *
	 * @param rgbPixels		an array of RGB pixel values
	 * @param samples		an array of audio samples whose values will be set from rgbPixels, which may be null.
	 * @param chan		    channel to extract from the RGB pixel values
	 * 						Will be initialized and returned if null.
	 * @return              a array of floats mapped to the audio range, assigned to samples
	 */
     public float[] pullPixelAudio(int[] rgbPixels, float[] samples, ChannelNames chan) {
    	if (samples == hsbPixel) {
    		samples = new float[rgbPixels.length];
    	}
		switch (chan) {
		case L: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = PixelAudio.map(brightness(rgbPixels[i], hsbPixel), 0, 1, -1.0f, 1.0f);
            }
            break;
		}
		case H: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = PixelAudio.map(hue(rgbPixels[i], hsbPixel), 0, 1, -1.0f, 1.0f);
            }
			break;
		}
		case S: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = PixelAudio.map(saturation(rgbPixels[i], hsbPixel), 0, 1, -1.0f, 1.0f);
            }
			break;
		}
		case R: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = PixelAudio.map(((rgbPixels[i] >> 16) & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case G: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = PixelAudio.map(((rgbPixels[i] >> 8) & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case B: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = PixelAudio.map((rgbPixels[i] & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case A: {
            for (int i = 0; i < samples.length; i++) {
	    		samples[i] = PixelAudio.map(((rgbPixels[i] >> 24) & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case ALL: {
            // not a simple case, but we'll convert to grayscale using the "luminosity equation."
			// The brightness value in HSB (case L, above) or the L channel in Lab color spaces could be used, too.
             for (int i = 0; i < samples.length; i++) {
                int rgb = rgbPixels[i];
    			samples[i] = PixelAudio.map((0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF)), 0, 255, -1.0f, 1.0f);
             }
			break;
		}
		}
		return samples;
	}

     /**
	 * Converts an array of pixel channel values to an array of audio sample values, mapping sample values
	 * to the interval [-1.0, 1.0], using a lookup table to change the order of resulting array. 
	 *
	 * @param rgbPixels		an array of RGB pixel values
	 * @param lut			a lookup table for redirecting rgbPixels indexing, typically imageToSignalLUT
	 * @param chan		    channel to extract from the RGB pixel values
	 * @param samples		an array of audio samples whose values will be set from rgbPixels, which may be null.
	 * 						Will be initialized and returned if null.
	 * @return              a array of floats mapped to the audio range, identical to samples
	 */
     public float[] pullPixelAudio(int[] rgbPixels, int[] lut, float[] samples, ChannelNames chan) {
    	if (samples == null) {
    		samples = new float[rgbPixels.length];
    	}
		switch (chan) {
		case L: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = PixelAudio.map(brightness(rgbPixels[lut[i]], hsbPixel), 0, 1, -1.0f, 1.0f);
            }
            break;
		}
		case H: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = PixelAudio.map(hue(rgbPixels[lut[i]], hsbPixel), 0, 1, -1.0f, 1.0f);
            }
			break;
		}
		case S: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = PixelAudio.map(saturation(rgbPixels[lut[i]], hsbPixel), 0, 1, -1.0f, 1.0f);
            }
			break;
		}
		case R: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = PixelAudio.map(((rgbPixels[lut[i]] >> 16) & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case G: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = PixelAudio.map(((rgbPixels[lut[i]] >> 8) & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case B: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = PixelAudio.map((rgbPixels[lut[i]] & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case A: {
            for (int i = 0; i < samples.length; i++) {
	    		samples[i] = PixelAudio.map(((rgbPixels[lut[i]] >> 24) & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case ALL: {
            // not a simple case, but we'll convert to grayscale using the "luminosity equation."
			// The brightness value in HSB (case L, above) or the L channel in Lab color spaces could be used, too.
             for (int i = 0; i < samples.length; i++) {
                int rgb = rgbPixels[lut[i]];
    			samples[i] = PixelAudio.map((0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF)), 0, 255, -1.0f, 1.0f);
             }
			break;
		}
		}
		return samples;
	}


	/**
	 * Extracts a selected channel from an array of rgb values.
	 *
	 * The values returned are within the ranges expected for the channel requested: (0..1) for HSB and [0, 255] for RGB.
	 * If you want to use RGB channels as signal values, you'll need to map their range to  [-1.0, 1.0]. If you want to 
	 * use them as integer values, you'll need to typecast them, for examnple: <code>int redValue = (int) floatArray[i];</code>
	 * 
	 * @see https://docs.oracle.com/javase/8/docs/api/
	 * 
	 * <p>From java.awt.Color, entry for getHSBColor(): "The s and b components should be floating-point values between zero and one (numbers in the range 0.0-1.0).
	 * The h component can be any floating-point number. The floor of this number is subtracted from it to create
	 * a fraction between 0 and 1. This fractional number is then multiplied by 360 to produce the hue angle in
	 * the HSB color model."</p>
	 *
	 * @param rgbPixels rgb values in an array of int
	 * @param chan      the channel to extract, a value from the ChannelNames enum
	 * @return          the extracted channel values as an array of floats
	 */
	public float[] pullRawChannel(int[] rgbPixels, ChannelNames chan) {
	  // convert sample channel to float array buf
	  float[] buf = new float[rgbPixels.length];
	  int i = 0;
	  switch (chan) {
	  case L: {
	      for (int rgb : rgbPixels) buf[i++] = brightness(rgb, hsbPixel);
	      break;
	    }
	  case H: {
	      for (int rgb : rgbPixels) buf[i++] = hue(rgb, hsbPixel);
	      break;
	    }
	  case S: {
	      for (int rgb : rgbPixels) buf[i++] = saturation(rgb, hsbPixel);
	      break;
	    }
	  case R: {
	      for (int rgb : rgbPixels)  buf[i++] = (rgb >> 16) & 0xFF;
	      break;
	    }
	  case G: {
	      for (int rgb : rgbPixels) buf[i++] = (rgb >> 8) & 0xFF;
	      break;
	    }
	  case B: {
	      for (int rgb : rgbPixels) buf[i++] = rgb & 0xFF;
	      break;
	    }
	  case A: {
	      for (int rgb : rgbPixels) buf[i++] = (rgb >> 24) & 0xFF;
	      break;
	    }
	  case ALL: {
	      for (int rgb : rgbPixels) buf[i++] = rgb;
	      break;
	    }
	  }
	  return buf;
	}


	public int pushAudioPixel(float sample, int rgb, ChannelNames chan) {
		switch (chan) {
		case L: {
			rgb = this.applyBrightness(sample, rgb);
			break;
		}
		case H: {
			rgb = this.applyHue(sample, rgb);
			break;
		}
		case S: {
			rgb = this.applySaturation(sample, rgb);
			break;
		}
		case R: {
			rgb = this.applyRed(sample, rgb);
			break;
		}
		case G: {
			rgb = this.applyGreen(sample, rgb);
			break;
		}
		case B: {
			rgb = this.applyBlue(sample, rgb);
			break;
		}
		case A: {
			rgb = this.applyAlpha(sample, rgb);
			break;
		}
		case ALL: {
			rgb = this.applyAll(sample, rgb);
			break;
		}
		}
		return rgb;
	}

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
	 * @param rgbPixels an array of pixel values
	 * @param buf       an array of floats in the range  [-1.0, 1.0]
	 * @param chan      the channel to replace
	 * @return			rgbPixels with new values
	 */
	public int[] pushAudioPixel(float buf[], int[] rgbPixels, ChannelNames chan) {
		// it's unlikely that this will happen, but if rgbPixels is null, create an array and fill it with middle gray color
		if (rgbPixels == null) {
			rgbPixels = new int[buf.length];
			Arrays.fill(rgbPixels, PixelAudioMapper.composeColor(127, 127, 127));
		}
		switch (chan) {
		case L: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = this.applyBrightness(buf[i], rgbPixels[i]);
			}
			break;
		}
		case H: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = this.applyHue(buf[i], rgbPixels[i]);
			}
			break;
		}
		case S: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = this.applySaturation(buf[i], rgbPixels[i]);
			}
			break;
		}
		case R: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = this.applyRed(buf[i], rgbPixels[i]);
			}
			break;
		}
		case G: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] =  this.applyGreen(buf[i], rgbPixels[i]);
			}
			break;
		}
		case B: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = this.applyBlue(buf[i], rgbPixels[i]);
			}
			break;
		}
		case A: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = this.applyAlpha(buf[i], rgbPixels[i]);
			}
			break;
		}
		case ALL: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = this.applyAll(buf[i], rgbPixels[i]);
			}
			break;
		}
		}  // end switch
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
	 * @param rgbPixels an array of pixel values
	 * @param buf       an array of floats in the range  [-1.0, 1.0]
	 * @param lut		a lookup table to redirect the indexing of the buf, typically imageToPixelsLUT
	 * @param chan      the channel to replace
	 * @return			rgbPixels with new values
	 */
	public int[] pushAudioPixel(float buf[], int[] rgbPixels, int[] lut, ChannelNames chan) {
		// it's unlikely that this will happen, but if rgbPixels is null, create an array and fill it with middle gray color
		if (rgbPixels == null) {
			rgbPixels = new int[buf.length];
			Arrays.fill(rgbPixels, PixelAudioMapper.composeColor(127, 127, 127));
		}
		switch (chan) {
		case L: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = this.applyBrightness(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case H: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = this.applyHue(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case S: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = this.applySaturation(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case R: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = this.applyRed(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case G: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] =  this.applyGreen(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case B: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = this.applyBlue(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case A: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = this.applyAlpha(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case ALL: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = this.applyAll(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		}  // end switch
		return rgbPixels;
	}



}




