/**
 *
 */
package net.paulhertz.pixelaudio;


import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * <p>
 * PixelAudioMapper is the parent class for child classes that map a 1D "signal" array of
 * floating point numbers to a 2D "image" array of integers. It makes assumptions about the
 * range of values in these arrays, detailed below. This class specifically handles the one-to-one
 * mapping between the signal and the image arrays. The mapping is handled by lookup tables
 * created by a separate mapping generator class. If you think of the signal as a space-filling
 * curve that visits each pixel in the image, one lookup table is a list of index numbers of
 * each pixel it visits, in the order it traverses them. It uses the table to "look up" the
 * pixels in the image array. There is a similar lookup table for the image that allows you
 * to look up the corresponding value in the signal array.
 * </p>
 * <p>
 * Some typical uses for this class include:
 * <ul>
 * 	  <li> Reading an audio file or audio stream into the signal array and then writing its
 *		transcoded values to the image array for display as a visualization.</li>
 *	  <li> Using interaction with an image to trigger audio events at precise locations in a signal.</li>
 *    <li> Running audio filters on an image-as-signal and writing the results to the image.</li>
 *    <li> Running image algorithms on a signal-as-image and writing the results back to the signal.</li>
 *    <li> Synthesizing image data and audio and then animating the data while interactively
 *      triggering audio events. </li>
 *  </p>
 *
 * <h2>DATA REPRESENTATION</h2>
 * <p>
 * For the sake of generality, the enclosing types for image and audio data are implemented outside
 * this class. In Processing, PImage wraps image data. I have been using the minim library for audio,
 * (https://code.compartmental.net/minim/) but the built-in audio in Processing 4 is definitely an option.
 * </p><p>
 * Within this class, image pixels are represented as an array of ints, and audio samples are represented
 * as an array of floats. Processing.core.PImage.pixels can provide the int array. Whatever audio class
 * you use can provide the float array.
 * </p><p>
 * The image array contains standard 24- or 32-bit RGB or RGBA pixel data, in row major order,
 * with (0,0) at upper left corner. The signal array contains values in the range [-1.0,1.0],
 * a standard format for audio values. It is up to the implementation to devise methods to convert
 * values from signal to image and vice versa: unless my default methods suit your purpose,
 * you should override the transcode() methods.
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
 *	 Default data format: floating point values in the range {-1.0..1.0}
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
 * Accordingly, we set up the PixelAudioMapper constructor to generate signalToImageLUT first and then generate
 * imageToSignalLUT from signalToImageLUT. It uses a subsidiary class to do this.
 * </p><p>
 * Note that the same LUT could be used by multiple instances of PixelAudioMapper, as long as it is in effect
 * a static variable. The LUT for a 512 x 512 pixel Hilbert curve can be calculated once and copied to
 * or called by every PixelAudio instance that maps a signal to a 512 x 512 bitmap using a Hilbert curve.
 * A call to the PixelMapGen.generate() method will provide a signalToImageLUT which can be used by
 * all PixelAudioMapper instances with the same width and height.
 * </p><p>
 * Currently, I am using a PixelMapGen instance as an argument to the PixelAudioMapper constructor.
 * First create a PixMapGen instance with the width and height of the image you are addressing. The PixMapGen
 * instance will generate the LUTs for its particular mapping for you. You can then pass it to the
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
 * Implementation of the transcode() method is left to child instances of PixelArrayMapper.
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
 * areas in the image. We’ll call the former methods pluck (read) and plant (write), and the latter
 * peel (read) and stamp (write). The method signatures may be sufficient to determine the order of values
 * when reading or writing, but it may clarify things to modify the name.
 * </p><p>
 * When data to be written is not in the format of the target array, it will have to be transcoded first.
 * In the following, intArray contains pixel data and floatArray contains signal data.
 * Pos is an index into the signal, x and y are locations in the image. The length parameter is associated
 * with the array order of samples or pixels. The w and h parameters work with rectangular areas mapped
 * from the image. When we call peel or stamp on signal data, length should be equal to w * h for
 * a given rectangular area of the image.
 * </p><p>
 * Arguments to mapping and writing methods are written so that source precedes target. Using this convention,
 * most methods have a unique signature that also indicates how they function. Where there are ambiguities or
 * a need for clrification, I have renamed the function, as in pluckPixelsAsFloat, pluckSamplesAsInt,
 * peelPixelsAsFloat, and peelSamplesAsInt.
 *
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
 * how to update the signal and pixel arrays.
 * </p>
 *
 * <h2>OTHER OPERATIONS</h2>
 * <p>
 * The following are suggestions for methods that could be implemented in children of PixelArrayMapper.
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
 */
public class PixelAudioMapper {
	// necessary instance variables
	/** image width */
	protected int width;
	/** image height */
	protected int height;
	/** pixel array and signal array length, equal to w * h */
	protected int len;
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
		this.len = gen.getSize();
		this.signalToImageLUT = gen.getPixelMapCopy();
		this.imageToSignalLUT = gen.getSampleMapCopy();
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
		return this.len;
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
		this.imageToSignalLUT = imgLUT;
	}

	/**
	 * Generate signalToImageLUT.
	 */
	public int[] generateSignalToImageLUT() {
		return this.generator.generate();
	}

	/**
	 * Called when signalToImageLUT has been initialized to generate imageToSignalLUT
	 */
	public void generateImageToSignalLUT(int[] sigLUT) {
		for (int i = 0; i < width * height - 1; i++) {
			this.imageToSignalLUT[sigLUT[i]] = i;
		}
	}
	
	public PixelMapGen getGenerator() {
		return this.generator;
	}
	
	public ArrayList<int[]> getGeneratorCoordinatesCopy() {
		return this.generator.getCoordinatesCopy();
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
	 * @param sig an array of floats in the audio range (-1..1)
	 * @param img an array of RGB pixel values
	 */
	public void mapSigToImg(float[] sig, int[] img) {
		this.pushAudioPixel(sig, img, imageToSignalLUT, ChannelNames.ALL);		// calls our utility method's grayscale conversion
	}

	/**
	 * Map signal values to a specified channel in the image.
	 * On completion, img[] contains new values.
	 * The img array and the sig array must be the same size.
	 *
	 * @param sig			an array of floats in the audio range (-1..1)
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
	 * @param sig			an array of floats in the audio range (-1..1)
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
	 * @param sig			an array of floats in the audio range (-1..1)
	 * @param img			an array of RGB pixel values
	 * @param fromChannel	the color channel to get a value from
	 */
	public void mapImgToSig(int[] img, float[] sig, ChannelNames fromChannel) {
		this.pullPixelAudio(img, imageToSignalLUT, sig, fromChannel);
	 }

	/**
	 * Writes transcoded pixel values directly to the signal, without using a LUT to redirect. V
	 * Values are calculated with the standard luminosity equation, gray = 0.3 * red + 0.59 * green + 0.11 * blue.
	 *
	 * @param img		an array of RGB pixel values, source
	 * @param sig		an array of audio samples in the range (-1.0..1.0), target
	 */
	public void writeImgToSig(int[] img, float[] sig) {
		this.pullPixelAudio(img, sig, ChannelNames.ALL);
	 }

	/**
	 * @param img			an array of RGB pixel values, source
	 * @param sig			an array of audio samples in the range (-1.0..1.0), target
	 * @param fromChannel	channel in RGB or HSB color space, from ChannelNames enum
	 */
	public void writeImgToSig(int[] img, float[] sig, ChannelNames fromChannel) {
		 this.pullPixelAudio(img, sig, fromChannel);
	 }

	/**
	 * @param sig		an array of audio samples in the range (-1.0..1.0), source
	 * @param img		an array of RGB pixel values, target
	 */
	public void writeSigToImg(float[] sig, int[] img) {
		 this.pushAudioPixel(sig, img, ChannelNames.ALL);
	 }

	 /**
	 * @param sig			an array of audio samples in the range (-1.0..1.0), source
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
	 * Converts a float value in the range (-1.0, 1.0) to an int value in the range [0..255].
	 *
	 * @param val	a float value in the range (-1.0, 1.0)
	 * @return		an int mapped to the range [0..255]
	 */
	public int transcode(float val) {
		 float vout = map(val, -1.0f, 1.0f, 0, 255);
		 return Math.round(vout);
	 }

	 /**
	 * Converts an int value in the range [0..255] to a float value in the range (-1.0, 1.0).
	 *
	 * @param val	an int int he range [0..255]
	 * @return		a float mapped to the range (-1.0, 1.0)
	 */
	public float transcode(int val) {
		 float vout = map(val, 0, 255, -1.0f, 1.0f);
		 return vout;
	 }


	//------------- SUBARRAYS -------------//

	/*
	 * In each case, a source subarray is either extracted from or inserted into a target larger array.
	 * When the small array, sprout, is inserted, it is indexed from 0..sprout.length. The larger array,
	 * img or sig, is indexed from read or write point pos to pos + length.
	 *
	 * All float[] arrays should contain audio range values (-1.0f..1.0f).
	 * All int[] arrays should contain RGB pixel values.
	 *
	 */


	/**
	 * Starting at image coordinates (x, y), reads values from pixel array img using imageToSignalLUT
	 * to redirect indexing and returns them as an array of RGB pixel values in signal order.
	 *
	 * @param img			an array of RGB pixel values, typically from the bitmap image you are using with PixelAudioMapper
	 * @param x				x coordinate of a point in the bitmap image from which img is derived
	 * @param y				y coordinate of a point in the bitmap image from which img is derived
	 * @param length		length of the subarray to pluck from img
	 * @return				a new array of pixel values in signal order
	 */
	public int[] pluckPixels(int[] img, int x, int y, int length) {
		int pos = x + y * this.width;
		int[] petal = new int[length];
		for (int i = pos; i < pos + length; i++) {
			int rgb = img[this.imageToSignalLUT[i]];
			petal[i] = rgb;
		}
		return petal;
	}

	/*
	// It's not clear to me when this signature might be useful. Not yet, anyhow.
	public int[] pluckPixels(int[] img, int x, int y, int length, ChannelNames fromChannel) {
		int pos = x + y * this.width;
		int[] petal = new int[length];
		for (int i = pos; i < pos + length; i++) {

		}
		return petal;
	}
	*/


	/**
	 * Starting at image coordinates (x, y), reads values from pixel array img using imageToSignalLUT
	 * to redirect indexing and returns them as an array of transcoded audio values in signal order.
	 *
	 * @param img			source array of RGB pixel values, typically from the bitmap image you are using with PixelAudioMapper
	 * @param x				x coordinate of a point in the bitmap image from which img is derived
	 * @param y				y coordinate of a point in the bitmap image from which img is derived
	 * @param length		length of the subarray to pluck from img
	 * @param fromChannel	the color channel from which to read pixel values
	 * @return				a new array of audio values in signal order
	 */
	public float[] pluckPixelsAsAudio(int[] img, int x, int y, int length, ChannelNames fromChannel) {
		int pos = x + y * this.width;
		float[] samples = new float[length];
		int j = 0;
		switch (fromChannel) {
		case L: {
			for (int i = pos; i < pos + length; i++) {
				int rgb = img[this.imageToSignalLUT[i]];						// TODO is it worth lowering legibility and debugging clarity to
				samples[j++] = map(brightness(rgb), 0, 1, -1.0f, 1.0f);			// replace rgb with its value expression img[this.imageToSignalLUT[i]]
			}																	// in hopes of gaining some speed? Probably not.
			break;
		}
		case H: {
			for (int i = pos; i < pos + length; i++) {
				int rgb = img[this.imageToSignalLUT[i]];
				samples[j++] = map(hue(rgb), 0, 1, -1.0f, 1.0f);
			}
			break;
		}
		case S: {
			for (int i = pos; i < pos + length; i++) {
				int rgb = img[this.imageToSignalLUT[i]];
				samples[j++] = map(saturation(rgb), 0, 1, -1.0f, 1.0f);
			}
			break;
		}
		case R: {
			for (int i = pos; i < pos + length; i++) {
				int rgb = img[this.imageToSignalLUT[i]];
				samples[j++] = map(((rgb >> 16) & 0xFF), 0, 255, -1.0f, 1.0f);
			}
			break;
		}
		case G: {
			for (int i = pos; i < pos + length; i++) {
				int rgb = img[this.imageToSignalLUT[i]];
				samples[j++] = map(((rgb >> 8) & 0xFF), 0, 255, -1.0f, 1.0f);
			}
			break;
		}
		case B: {
			for (int i = pos; i < pos + length; i++) {
				int rgb = img[this.imageToSignalLUT[i]];
				samples[j++] = map((rgb & 0xFF), 0, 255, -1.0f, 1.0f);
			}
			break;
		}
		case A: {
			for (int i = pos; i < pos + length; i++) {
				int rgb = img[this.imageToSignalLUT[i]];
				samples[j++] = map(((rgb >> 24) & 0xFF), 0, 255, -1.0f, 1.0f);
			}
			break;
		}
		case ALL: {
            // not a simple case, but we'll convert to grayscale using the "luminosity equation."
			// The brightness value in HSB (case L, above) or the L channel in Lab color spaces could be used, too.
			for (int i = pos; i < pos + length; i++) {
				int rgb = img[this.imageToSignalLUT[i]];
				samples[j++] = map((0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF)), 0, 255, -1.0f, 1.0f);
			}
			break;
		}
		}
		return samples;
	}

	/**
	 * Starting at image coordinates (x, y), reads audio values from signal array sig starting at pos for length values
	 * and returns a new array of audio values in signal order. No redirection is needed when reading from the signal.
	 *
	 * @param sig		source array of audio values
	 * @param pos		a position in the sig array
	 * @param length	number of values to read from sig array
	 * @return			a new array with the audio values we read
	 */
	public float[] pluckSamples(float[] sig, int pos, int length) {
		float[] samples = new float[length];
		int j = 0;
		for (int i = pos; i < pos + length; i++) {
			samples[j++] = sig[i];
		}
		return samples;
	}

	/**
	 * Starting at pos in the sig[] array, read values and transcode them into RGB data.
	 *
	 * @param sig		source array of audio values (-1.0f..1.0f)
	 * @param pos		entry point in the sig array
	 * @param length	number of values to read from the sig array
	 * @return			an array of RGB values where r == g == b, derived from the sig values
	 */
	public int[] pluckSamplesAsRGB(float[] sig, int pos, int length) {
		int[] rgbPixels = new int[length];
		int j = 0;
		for (int i = pos; i < pos + length; i++) {
			float sample = sig[i];
			sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
			int v = Math.round(map(sample, -1.0f, 1.0f, 0, 255));
			rgbPixels[j++] = 255 << 24 | v << 16 | v << 8 | v;
		}
		return rgbPixels;
	}


	/**
	 * Inserts elements from a source array of RGB pixel values into a target array of RGB pixel values
	 * following the signal path.
	 *
	 * @param sprout	source array of RGB values to insert into target array img
	 * @param img		target array of RGB values
	 * @param x			x coordinate in image from which img pixel array was derived
	 * @param y			y coordinate in image from which img pixel array was derived
	 * @param length	number of values from sprout to insert into img array
	 */
	public void plantPixels(int[] sprout, int[] img, int x, int y, int length) {
		int pos = x + y * this.width;
		int j = 0;
		for (int i = pos; i < pos + length; i++) {
			img[imageToSignalLUT[i]] = sprout[j++];
		}
	}


	/**
	 * Inserts elements from a source array of RGB pixel values into a specified color channel
	 * in a target array of RGB pixel values following the signal path.
	 *
	 * @param sprout	source array of RGB values to insert into target array img
	 * @param img		target array of RGB values
	 * @param x			x coordinate in image from which img pixel array was derived
	 * @param y			y coordinate in image from which img pixel array was derived
	 * @param length	number of values from sprout to insert into img array
	 */
	public void plantPixels(int[] sprout, int[] img, int x, int y, int length, ChannelNames toChannel) {
		int pos = x + y * this.width;
		int j = 0;
		switch (toChannel) {
		case L: {
			for (int i = pos; i < pos + length; i++) {
				int rgb = img[this.imageToSignalLUT[i]];
				Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
				rgb = Color.HSBtoRGB(hsbPixel[0], hsbPixel[1], brightness(sprout[j]));
				img[this.imageToSignalLUT[i]] = rgb;
				j++;
			}
			break;
		}
		case H: {
			for (int i = pos; i < pos + length; i++) {
				int rgb = img[this.imageToSignalLUT[i]];
				Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
				rgb = Color.HSBtoRGB(hue(sprout[j]), hsbPixel[1], hsbPixel[2]);
				img[this.imageToSignalLUT[i]] = rgb;
				j++;
			}
			break;
		}
		case S: {
			for (int i = pos; i < pos + length; i++) {
				int rgb = img[this.imageToSignalLUT[i]];
				Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
				rgb = Color.HSBtoRGB(hsbPixel[0], saturation(sprout[j]), hsbPixel[2]);
				img[this.imageToSignalLUT[i]] = rgb;
				j++;
			}
			break;
		}
		case R: {
			for (int i = pos; i < pos + length; i++) {
				int r = (sprout[j] << 16) & 0xFF;
				int rgb = img[this.imageToSignalLUT[i]];
				img[this.imageToSignalLUT[i]] = 255 << 24 | r << 16 | ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF;
				j++;
			}
			break;
		}
		case G: {
			for (int i = pos; i < pos + length; i++) {
				int g = (sprout[j] << 8) & 0xFF;
				int rgb = img[this.imageToSignalLUT[i]];
				img[this.imageToSignalLUT[i]] = 255 << 24 | ((rgb >> 16) & 0xFF) << 16 | g << 8 | rgb & 0xFF;
				j++;
			}
			break;
		}
		case B: {
			for (int i = pos; i < pos + length; i++) {
				int b = sprout[j] & 0xFF;
				int rgb = img[this.imageToSignalLUT[i]];
				img[this.imageToSignalLUT[i]] = 255 << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | b & 0xFF;		// set new RGB value, change blue channel
				j++;
			}
			break;
		}
		case A: {
			for (int i = pos; i < pos + length; i++) {
				int a = sprout[j] << 24;
				int rgb = img[this.imageToSignalLUT[i]];
				img[this.imageToSignalLUT[i]] = a << 24 | ((rgb >> 16) & 0xFF) << 16| ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF;
				j++;
			}
			break;
		}
		case ALL: {
			for (int i = pos; i < pos + length; i++) {
				img[imageToSignalLUT[i]] = sprout[j++];
			}
			break;
		}
		} // end switch
	}


	/**
	 * Inserts elements from a source array of audio values (-1.0f..1.0f) into a specified color channel
	 * in a target array of RGB pixel values following the signal path.
	 *
	 * @param sprout	source array of RGB values to insert into target array img
	 * @param img		target array of RGB values
	 * @param x			x coordinate in image from which img pixel array was derived
	 * @param y			y coordinate in image from which img pixel array was derived
	 * @param length	number of values from sprout to insert into img array
	 */
	public void plantPixels(float[] sprout, int[] img, int x, int y, int length, ChannelNames toChannel) {
		int pos = x + y * this.width;
		int j = 0;
		switch (toChannel) {
		case L: {
			for (int i = pos; i < pos + length; i++) {
				img[this.imageToSignalLUT[i]] = this.applyBrightness(sprout[j++], img[this.imageToSignalLUT[i]]);
			}
			break;
		}
		case H: {
			for (int i = pos; i < pos + length; i++) {
				img[this.imageToSignalLUT[i]] = this.applyHue(sprout[j++], img[this.imageToSignalLUT[i]]);
			}
			break;
		}
		case S: {
			for (int i = pos; i < pos + length; i++) {
				img[this.imageToSignalLUT[i]] = this.applySaturation(sprout[j++], img[this.imageToSignalLUT[i]]);
			}
			break;
		}
		case R: {
			for (int i = pos; i < pos + length; i++) {
				img[this.imageToSignalLUT[i]] = this.applyRed(sprout[j++], img[this.imageToSignalLUT[i]]);
			}
			break;
		}
		case G: {
			for (int i = pos; i < pos + length; i++) {
				img[this.imageToSignalLUT[i]] = this.applyGreen(sprout[j++], img[this.imageToSignalLUT[i]]);
			}
			break;
		}
		case B: {
			for (int i = pos; i < pos + length; i++) {
				img[this.imageToSignalLUT[i]] = this.applyBlue(sprout[j++], img[this.imageToSignalLUT[i]]);
			}
			break;
		}
		case A: {
			for (int i = pos; i < pos + length; i++) {
				img[this.imageToSignalLUT[i]] = this.applyAlpha(sprout[j++], img[this.imageToSignalLUT[i]]);
			}
			break;
		}
		case ALL: {
			for (int i = pos; i < pos + length; i++) {
				img[this.imageToSignalLUT[i]] = this.applyAll(sprout[j++], img[this.imageToSignalLUT[i]]);
			}
			break;
		}
		} // end switch
	}

	/**
	 * Insert audio samples from source array sprout into target array of audio samples sig. No redirection needed,
	 * array values are already in signal order.
	 *
	 * @param sprout	source array of audio values (-1.0f..1.0f)
	 * @param sig		target array of signal values
	 * @param pos		start point in sig array
	 * @param length	number of values to copy from sprout array to sig array
	 */
	public void plantSamples(float[] sprout, float[] sig, int pos, int length) {
		int j = 0;
		for (int i = pos; i < pos + length; i++) {
			sig[i] = sprout[j++];
		}
	}

	/**
	 * Insert values from source array of RGB values sprout into target array sig of audio values (-1.0f..1.0f),
	 * transcoding from RGB to audio.
	 *
	 * @param sprout
	 * @param sig
	 * @param pos
	 * @param length
	 */
	public void plantSamples(int[] sprout, float[] sig, int pos, int length) {
		int j = 0;
		for (int i = pos; i < pos + length; i++) {
			sig[i] = map(PixelAudioMapper.getGrayscale(sprout[j++]), 0, 255, -1.0f, 1.0f);
		}
	}

	public void plantSamples(int[] sprout, float[] sig, int pos, int length, ChannelNames fromChannel) {
		int j = 0;
		switch (fromChannel) {
		case L: {
			for (int i = pos; i < pos + length; i++) {
				sig[i] = map(brightness(sprout[j++]), 0, 1, -1.0f, 1.0f);
			}
			break;
		}
		case H: {
			for (int i = pos; i < pos + length; i++) {
				sig[i] = map(hue(sprout[j++]), 0, 1, -1.0f, 1.0f);
			}
			break;
		}
		case S: {
			for (int i = pos; i < pos + length; i++) {
				sig[i] = map(saturation(sprout[j++]), 0, 1, -1.0f, 1.0f);
			}
			break;
		}
		case R: {
			for (int i = pos; i < pos + length; i++) {
				sig[i] = map(((sprout[j++] >> 16) & 0xFF), 0, 255, -1.0f, 1.0f);
			}
			break;
		}
		case G: {
			for (int i = pos; i < pos + length; i++) {
				sig[i] = map(((sprout[j++] >> 8) & 0xFF), 0, 255, -1.0f, 1.0f);
			}
			break;
		}
		case B: {
			for (int i = pos; i < pos + length; i++) {
				sig[i] = map((sprout[j++] & 0xFF), 0, 255, -1.0f, 1.0f);
			}
			break;
		}
		case A: {
			for (int i = pos; i < pos + length; i++) {
				sig[i] = map(((sprout[j++] >> 24) & 0xFF), 0, 255, -1.0f, 1.0f);
			}
			break;
		}
		case ALL: {
			for (int i = pos; i < pos + length; i++) {
				// convert to grayscale using the "luminosity equation."
				sig[i] = map((0.3f * ((sprout[i] >> 16) & 0xFF)
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
				samples[j++] =  map(PixelAudioMapper.getGrayscale(img[dx + dy * w]), 0, 255, -1.0f, 1.0f);
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
				rgbPixels[j++] =  Math.round(map(sig[this.imageToSignalLUT[dx + dy * w]], 0, 255, -1.0f, 1.0f));
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
					rgb = Color.HSBtoRGB(hsbPixel[0], hsbPixel[1], brightness(stamp[j]));
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
					rgb = Color.HSBtoRGB(hue(stamp[j]), hsbPixel[1], hsbPixel[2]);
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
					rgb = Color.HSBtoRGB(hsbPixel[0], saturation(stamp[j]), hsbPixel[2]);
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


	//------------- UTILITY -------------//


	// lerp and map

	/**
	 * Good old lerp.
	 * @param a		first bound, typically a minimum value
	 * @param b		second bound, typically a maximum value
	 * @param f		scaling value, from 0..1 to interpolate between a and b, but can go over or under
	 * @return		a value between a and b, scaled by f (if 0 <= f >= 1).
	 */
	static public final float lerp(float a, float b, float f) {
	    return a + f * (b - a);
	}

	/**
	 * Processing's map method, but with no error checking
	 * @param value
	 * @param start1
	 * @param stop1
	 * @param start2
	 * @param stop2
	 * @return
	 */
	static public final float map(float value, float start1, float stop1, float start2, float stop2) {
		return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
	}


	// array rotation

	/**
	 * Rotates an array of ints left by d values. Uses efficient "Three Rotation" algorithm.
	 *
	 * @param arr array of ints to rotate
	 * @param d   number of elements to shift
	 */
	static public final void rotateLeft(int[] arr, int d) {
		d = d % arr.length;
		reverseArray(arr, 0, d - 1);
		reverseArray(arr, d, arr.length - 1);
		reverseArray(arr, 0, arr.length - 1);
	}

	/**
	 * Rotates an array of floats left by d values. Uses efficient "Three Rotation" algorithm.
	 *
	 * @param arr array of floats to rotate
	 * @param d   number of elements to shift
	 */
	static public final void rotateLeft(float[] arr, int d) {
		d = d % arr.length;
		reverseArray(arr, 0, d - 1);
		reverseArray(arr, d, arr.length - 1);
		reverseArray(arr, 0, arr.length - 1);
	}

	/**
	 * Reverses an arbitrary subset of an array of ints.
	 *
	 * @param arr array to modify
	 * @param l   left bound of subset to reverse
	 * @param r   right bound of subset to reverse
	 */
	static public final void reverseArray(int[] arr, int l, int r) {
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
	static public final void reverseArray(float[] arr, int l, int r) {
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
	 * @return an array of integers in the intRange 0..255 for 3 primary color
	 *         components: {R, G, B}
	 */
	static public final int[] rgbComponents(int rgb) {
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
	 * @return an array of integers in the intRange 0..255 for 3 primary color
	 *         components: {R, G, B} plus alpha
	 */
	static public final int[] rgbaComponents(int argb) {
		int[] comp = new int[4];
		comp[0] = (argb >> 16) & 0xFF; // Faster way of getting red(argb)
		comp[1] = (argb >> 8) & 0xFF; // Faster way of getting green(argb)
		comp[2] = argb & 0xFF; // Faster way of getting blue(argb)
		comp[3] = argb >> 24; // alpha component
		return comp;
	}

	/**
	 * Returns alpha channel value of a color.
	 *
	 * @param argb a Processing color as a 32-bit integer
	 * @return an int for alpha channel
	 */
	static public final int alphaComponent(int argb) {
		return (argb >> 24);
	}

	/**
	 * Takes the alpha channel from one color, rgba, and applies it to another color, rgb.
	 * @param rgb	The color we want to change
	 * @param rgba	The color from which we get the alpha channel (a mask, for instance)
	 * @return		A color with the RGB values from rgb and the A value from rgba
	 */
	static public final int applyAlpha(int rgb, int rgba) {
		return (rgba >> 24) << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | (rgb & 0xFF);
	}

	/**
	 * Creates a Processing ARGB color from r, g, b, and alpha channel values. Note the order
	 * of arguments, the same as the Processing color(value1, value2, value3, alpha) method.
	 *
	 * @param r red component 0..255
	 * @param g green component 0..255
	 * @param b blue component 0..255
	 * @param a alpha component 0..255
	 * @return a 32-bit integer with bytes in Processing format ARGB.
	 */
	static public final int composeColor(int r, int g, int b, int a) {
		return a << 24 | r << 16 | g << 8 | b;
	}

	/**
	 * Creates an opaque Processing RGB color from r, g, b values. Note the order
	 * of arguments, the same as the Processing color(value1, value2, value3) method.
	 *
	 * @param r red component 0..255
	 * @param g green component 0..255
	 * @param b blue component 0..255
	 * @return a 32-bit integer with bytes in Processing format ARGB.
	 */
	static public final int composeColor(int r, int g, int b) {
		return 255 << 24 | r << 16 | g << 8 | b;
	}

	/**
	 * Creates a Processing ARGB color from r, g, b, values in an array.
	 *
	 * @param comp 	array of 3 integers in range 0..255, for red, green and blue
	 *             	components of color alpha value is assumed to be 255
	 * @return a 32-bit integer with bytes in Processing format ARGB.
	 */
	static public final int composeColor(int[] comp) {
		return 255 << 24 | comp[0] << 16 | comp[1] << 8 | comp[2];
	}

	/**
	 * @param rgb	an RGB color value
	 * @return		a number in the range [0, 255] equivalent to the luminosity value rgb
	 */
	static public final int getGrayscale(int rgb) {
        //  we'll convert to grayscale using the "luminosity equation."
		float gray = 0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF);
		return Math.round(gray);
	}


	/**
	 * @param argb		an RGB color
	 * @return			a String equivalent to a Processing color(r, g, b, a) call, such as "color(233, 144, 89, 255)"
	 */
	static public final String colorString(int argb) {
		int[] comp = rgbaComponents(argb);
		return "color(" + comp[0] + ", " + comp[1] + ", " + comp[2] + ", " + comp[3] + ")";
	}


	// -------- HSB <---> RGB -------- //

	/**
	 * @param rgb	The RGB color from which we will obtain the hue component in the HSB color model.
	 * @return		A floating point number in the range (0..1) that can be multiplied by 360 to get the hue angle.
	 */
	public float hue(int rgb) {
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
		return hsbPixel[0];
	}

	/**
	 * @param rgb	The RGB color from which we will obtain the hue component in the HSB color model.
	 * @return		A floating point number in the range (0..1) representing the saturation component of an HSB color.
	 */
	public float saturation(int rgb) {
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
		return hsbPixel[1];
	}

	/**
	 * @param rgb	The RGB color from which we will obtain the hue component in the HSB color model.
	 * @return		A floating point number in the range (0..1) representing the brightness component of an HSB color.
	 */
	public float brightness(int rgb) {
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
		return hsbPixel[2];
	}


	// ------------- APPLY COLOR CHANNEL METHODS ------------- //

	public int applyBrightness(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		sample = map(sample, -1.0f, 1.0f, 0.0f, 1.0f);								// map audio sample to (0..1)
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(hsbPixel[0], hsbPixel[1], sample);
	}

	public int applyHue(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		sample = map(sample, -1.0f, 1.0f, 0.0f, 1.0f);								// map audio sample to (0..1)
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(sample, hsbPixel[1], hsbPixel[2]);
	}

	public int applySaturation(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		sample = map(sample, -1.0f, 1.0f, 0.0f, 1.0f);								// map audio sample to (0..1)
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(hsbPixel[0], sample, hsbPixel[2]);
	}

	public int applyRed(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		int r = Math.round(map(sample, -1.0f, 1.0f, 0, 255));						// map audio sample to [0, 255]
		return (255 << 24 | r << 16 | ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF);		// apply to red channel
	}

	public int applyGreen(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		int g = Math.round(map(sample, -1.0f, 1.0f, 0, 255));						// map audio sample to [0, 255]
		return (255 << 24 | ((rgb >> 16) & 0xFF) << 16 | g << 8 | rgb & 0xFF);		// apply to green channel
	}

	public int applyBlue(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		int b = Math.round(map(sample, -1.0f, 1.0f, 0, 255));						// map audio sample to [0, 255]
		return (255 << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | b & 0xFF);	// apply to blue channel
	}

	public int applyAlpha(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		int a = Math.round(map(sample, -1.0f, 1.0f, 0, 255));						// map audio sample to [0, 255]
		return (a<< 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF);	// apply to alpha channel
	}

	public int applyAll(float sample, int rgb) {
		sample = sample > 1.0f ? 1.0f : sample < 0 ? 0 : sample;					// a precaution, keep values within limits
		int v = Math.round(map(sample, -1.0f, 1.0f, 0, 255));						// map audio sample to [0, 255]
		return 255 << 24 | v << 16 | v << 8 | v;									// apply to all channels except alpha
	}


	// ------------- AUDIO <---> IMAGE ------------- //

	/**
	 * Converts a pixel channel value to an audio sample value, mapping the result to (-1.0..1.0).
	 *
	 * @param rgb		an RGB pixel value
	 * @param chan		channel to extract from the RGB pixel value
	 * @return
	 */
	public float pullPixelAudio(int rgb, ChannelNames chan) {
		float sample = 0;
		switch (chan) {
		case L: {
			sample = map(brightness(rgb), 0, 1, -1.0f, 1.0f);
			break;
		}
		case H: {
			sample = map(hue(rgb), 0, 1, -1.0f, 1.0f);
			break;
		}
		case S: {
			sample = map(saturation(rgb), 0, 1, -1.0f, 1.0f);
			break;
		}
		case R: {
			sample = map(((rgb >> 16) & 0xFF), 0, 255, -1.0f, 1.0f);
			break;
		}
		case G: {
			sample = map(((rgb >> 8) & 0xFF), 0, 255, -1.0f, 1.0f);
			break;
		}
		case B: {
			sample = map((rgb & 0xFF), 0, 255, -1.0f, 1.0f);
			break;
		}
		case A: {
			sample = map(((rgb >> 24) & 0xFF), 0, 255, -1.0f, 1.0f);
			break;
		}
		case ALL: {
            // not a simple case, but we'll convert to grayscale using the "luminosity equation."
			// The brightness value in HSB (case L, above) or the L channel in Lab color spaces could be used, too.
			sample = map((0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF)), 0, 255, -1.0f, 1.0f);
			break;
		}
		}
		return sample;
	}


	/**
	 * Converts a pixel channel value to an audio sample value, mapping the result to (-1.0..1.0).
	 *
	 * @param rgbPixels		an array of RGB pixel values
	 * @param samples		an array of audio samples whose values will be set from rgbPixels, which may be null.
	 * @param chan		    channel to extract from the RGB pixel values
	 * 						Will be initialized and returned if null.
	 * @return              a array of floats mapped to the audio range, identical to samples
	 */
     public float[] pullPixelAudio(int[] rgbPixels, float[] samples, ChannelNames chan) {
    	if (samples == null) {
    		samples = new float[rgbPixels.length];
    	}
		switch (chan) {
		case L: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = map(brightness(rgbPixels[i]), 0, 1, -1.0f, 1.0f);
            }
            break;
		}
		case H: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = map(hue(rgbPixels[i]), 0, 1, -1.0f, 1.0f);
            }
			break;
		}
		case S: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = map(saturation(rgbPixels[i]), 0, 1, -1.0f, 1.0f);
            }
			break;
		}
		case R: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = map(((rgbPixels[i] >> 16) & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case G: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = map(((rgbPixels[i] >> 8) & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case B: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = map((rgbPixels[i] & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case A: {
            for (int i = 0; i < samples.length; i++) {
	    		samples[i] = map(((rgbPixels[i] >> 24) & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case ALL: {
            // not a simple case, but we'll convert to grayscale using the "luminosity equation."
			// The brightness value in HSB (case L, above) or the L channel in Lab color spaces could be used, too.
             for (int i = 0; i < samples.length; i++) {
                int rgb = rgbPixels[i];
    			samples[i] = map((0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF)), 0, 255, -1.0f, 1.0f);
             }
			break;
		}
		}
		return samples;
	}

     /**
	 * Converts a pixel channel value to an audio sample value, mapping the result to (-1.0..1.0).
	 * A lookup table is used to redirect the indexing of the pixel values.
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
                samples[i] = map(brightness(rgbPixels[lut[i]]), 0, 1, -1.0f, 1.0f);
            }
            break;
		}
		case H: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = map(hue(rgbPixels[lut[i]]), 0, 1, -1.0f, 1.0f);
            }
			break;
		}
		case S: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = map(saturation(rgbPixels[lut[i]]), 0, 1, -1.0f, 1.0f);
            }
			break;
		}
		case R: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = map(((rgbPixels[lut[i]] >> 16) & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case G: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = map(((rgbPixels[lut[i]] >> 8) & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case B: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = map((rgbPixels[lut[i]] & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case A: {
            for (int i = 0; i < samples.length; i++) {
	    		samples[i] = map(((rgbPixels[lut[i]] >> 24) & 0xFF), 0, 255, -1.0f, 1.0f);
            }
			break;
		}
		case ALL: {
            // not a simple case, but we'll convert to grayscale using the "luminosity equation."
			// The brightness value in HSB (case L, above) or the L channel in Lab color spaces could be used, too.
             for (int i = 0; i < samples.length; i++) {
                int rgb = rgbPixels[lut[i]];
    			samples[i] = map((0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF)), 0, 255, -1.0f, 1.0f);
             }
			break;
		}
		}
		return samples;
	}


	/**
	 * Extracts a selected channel from an array of rgb values.
	 *
	 * From https://docs.oracle.com/javase/8/docs/api/, java.awt.Color, entry for getHSBColor():
	 *
	 * The s and b components should be floating-point values between zero and one (numbers in the range 0.0-1.0).
	 * The h component can be any floating-point number. The floor of this number is subtracted from it to create
	 * a fraction between 0 and 1. This fractional number is then multiplied by 360 to produce the hue angle in
	 * the HSB color model.
	 *
	 * The values returned are within the ranges expected for the channel requested: (0..1) for HSB and [0, 255] for RGB.
	 * If you want to use RGB channels as signal values, you'll need to map their range to (-1.0..1.0).
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
	      for (int rgb : rgbPixels) buf[i++] = brightness(rgb);
	      break;
	    }
	  case H: {
	      for (int rgb : rgbPixels) buf[i++] = hue(rgb);
	      break;
	    }
	  case S: {
	      for (int rgb : rgbPixels) buf[i++] = saturation(rgb);
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
	 * Replaces a specified channel in an array of pixel values, rgbPixels, with a value derived
	 * from an array of floats, buf, that represent audio samples. Upon completion, the pixel array
	 * rgbPixels contains the new values, always in the RGB color space.
	 *
	 * Both arrays, rgbPixels and buf, must be the same size.
	 *
	 * In the HSB color space, values are assumed to be floats in the range (0..1), so the values
	 * from buf need to be mapped to the correct ranges for HSB or RGB [0, 255]. We do some minimal
	 * limiting of values derived from buf[], but it is the caller's responsibility to constrain them
	 * to the audio range (-1..1).
	 *
	 * @param rgbPixels an array of pixel values
	 * @param buf       an array of floats in the range (-1..1)
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
	 * Replaces a specified channel in an array of pixel values, rgbPixels, with a value derived
	 * from an array of floats, buf, that represent audio samples. The supplied lookup table, lut,
	 * is intended to redirect the indexing of rgbPixels following the signal path. We are stepping
	 * through the buf array (the signal), so rgbPixels employs imageToSignalLUT to find where each
	 * index i into buf is pointing in the image pixels array, which is rgbPixels.  Upon completion,
	 * the pixel array rgbPixels contains the new values, always in the RGB color space.
	 *
	 * All three arrays, rgbPixels, buf, and lut must be the same size.
	 *
	 * In the HSB color space, values are assumed to be floats in the range (0..1), so the values
	 * from buf need to be mapped to the correct ranges for HSB or RGB [0, 255]. We do some minimal
	 * limiting of values derived from buf[], but it is the caller's responsibility to constrain them
	 * to the audio range (-1..1).
	 *
	 * @param rgbPixels an array of pixel values
	 * @param buf       an array of floats in the range (-1..1)
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




