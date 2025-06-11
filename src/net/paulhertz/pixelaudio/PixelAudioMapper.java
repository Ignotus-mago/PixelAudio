/**
 *
 */
package net.paulhertz.pixelaudio;


import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * <p>
 * As of pre-release version 0.9-beta PixelAudioMapper is reasonably complete. However, 
 * the "peel" and "stamp" methods have not been tested or supplied with example applications.
 * The Javadocs for these methods are also minimal or missing. They will change. You should
 * probably avoid using them for now. 
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
 * <p>
 * Some typical uses for this class include:
 *   <ul>
 * 	  <li> Reading an audio file or audio stream into the signal array and then writing its
 *		   values, transcoded to RGB integer values, to the image array for display as a visualization.</li>
 *	  <li> Using interaction with an image to trigger audio events at precise locations in a signal.</li>
 *    <li> Running audio filters on an image-as-signal and writing the results to the image.</li>
 *    <li> Running image algorithms on a signal-as-image and writing the results back to the signal.</li>
 *    <li> Synthesizing image data and audio and then animating the data while interactively
 *         triggering audio events. </li>
 *  </ul>
 *</p>
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
 * <h2>ARRAY SHIFTING</h2>
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

	/** 
	 * List of available color channels, "L" for lightness, since "B" for brightness is taken.
	 * The expanded enum type allows me to provide some additional information, including extraction
	 * methods for each channel. These all require a float[3] hsbPixel argument. Because of the 
	 * overhead involved in allocation of the hsbPixel array, callers are advised to make it 
	 * reusable for tight loops or to use the other extraction methods in the PixelAudioMapper class.
	 * At the moment, I regard the extraction feature as experimental, but they may be worth 
	 * extending to other extraction methods. 
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
	 * The int values handled here are thus in the range [0, 255]. HSB values (brightness, 
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
	

	// ------------- PIXEL AND SAMPLE INDEX LOOKUP ------------- //

	/* Note that these methods are not returning pixel or sample values, but index values */
	
	/**
	 * Given a coordinate pair (x,y) in an image, returns its index in a signal path
	 * over the image using the lookup table imageToSignalLUT.
	 * 
	 * @param x
	 * @param y
	 * @return     index into signal array
	 */
	public int lookupSample(int x, int y) {
		return this.imageToSignalLUT[x + y * this.width];
	}
	
	/**
	 * Given an index imagePos into the pixel array of an image, returns its index in a signal path
	 * over the image using the lookup table imageToSignalLUT.
	 * 
	 * @param imagePos
	 * @return            index into signal array
	 */
	public int lookupSample(int imagePos) {
		return this.imageToSignalLUT[imagePos];
	}
	
	/**
	 * Given an index into a signal array mapped to an image, returns the corresponding index into 
	 * the image pixel array using the lookup table signalToImageLUT.
	 * 
	 * @param signalPos
	 * @return
	 */
	public int lookupPixel(int signalPos) {
		return this.signalToImageLUT[signalPos];
	}

	/**
	 * Given an index into a signal array mapped to an image, returns the pixel coordinates (x,y)
	 * in the image using the lookup table signalToImageLUT.
	 * 
	 * @param signalPos
	 * @return             an array of two coordinates {x, y}
	 */
	public int[] lookupCoordinate(int signalPos) {
		int imagePos = this.signalToImageLUT[signalPos];
		int x =  imagePos % this.width;
		int y = imagePos / this.width;
		return new int[] {x, y};
	}


	//------------- MAPPING -------------//

	// TODO rewrite method signatures using SOURCE, TARGET, ARGS or SOURCE, LUT, TARGET, ARGS ordering.
	// Names and calls won't change, but change the documentation, too.
	// E.g., mapImgToSig(int[] img, float[] sig);
	// I've been doing this but it should be part of the review before publication.
	
	
	/**
	 * Creates an array of int which contains the values in <code>img</code> reordered by the lookup table <code>lut</code>.
	 * 
	 * @param img    an array of int, typically of RGB values
	 * @param lut    a look up table of the same size as <code>img</code>
	 * @return       a new array of int with the values in img reordered by the lookup table
	 */
	public int[] remapPixels(int[] img, int[] lut) {
		int[] newPixels = new int[img.length];
		for (int i = 0; i < img.length; i++) {
			newPixels[i] = img[lut[i]];
		}
		return newPixels;
	}

	
	/**
	 * Creates an array of float which contains the values in <code>sig</code> reordered by the lookup table <code>lut</code>.
	 * 
	 * @param sig    an array of float, typically audio samples
	 * @param lut	 a lookup table
	 * @return       a new array with the values in sig reordered by the lookup table
	 */
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
		PixelAudioMapper.pushAudioPixel(sig, img, imageToSignalLUT, ChannelNames.ALL);		// calls our utility method's grayscale conversion
	}

	/**
	 * Map signal values to a specified channel in the image using imageToSignalLUT.
	 * On completion, img[] contains new values, transcoded from the signal.
	 * The img array and the sig array must be the same size.
	 *
	 * @param sig			an array of floats in the audio range  [-1.0, 1.0]
	 * @param img			an array of RGB pixel values
	 * @param toChannel		the channel to write transcoded values to
	 */
	public void mapSigToImg(float[] sig, int[] img, ChannelNames toChannel) {
		PixelAudioMapper.pushAudioPixel(sig, img, imageToSignalLUT, toChannel);				// call our utility method with toChannel
	}

	/**
	 * Map current image pixel values to the signal, updating the signal array.
	 * There are several ways to derive an audio value from the image: we use
	 * the brightness channel in the HSB color space. On completion, sig[] contains new values.
	 * The img array and the sig array must be the same size.
	 *
	 * @param sig			an array of floats in the audio range  [-1.0, 1.0]
	 * @param img			an array of RGB pixel values
	 */
	public void mapImgToSig(int[] img, float[] sig) {
		PixelAudioMapper.pullPixelAudio(img, imageToSignalLUT, sig, ChannelNames.L, hsbPixel);
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
	 */
	public void mapImgToSig(int[] img, float[] sig, ChannelNames fromChannel) {
		PixelAudioMapper.pullPixelAudio(img, imageToSignalLUT, sig, fromChannel, hsbPixel);
	 }

	/**
	 * Writes transcoded pixel values directly to the signal, without using a LUT to redirect. V
	 * Values are calculated with the standard luminosity equation, <code>gray = 0.3 * red + 0.59 * green + 0.11 * blue</code>.
	 *
	 * @param img		source array of RGB pixel values
	 * @param sig		target array of audio samples in the range [-1.0, 1.0]
	 * @param hsbPixel  a float[3] array for use with color channel extraction
	 */
	public void writeImgToSig(int[] img, float[] sig) {
		PixelAudioMapper.pullPixelAudio(img, sig, ChannelNames.ALL, hsbPixel);
	 }

	/**
	 * @param img			an array of RGB pixel values, source
	 * @param sig			an array of audio samples in the range [-1.0, 1.0], target
	 * @param fromChannel	channel in RGB or HSB color space, from ChannelNames enum
	 */
	public void writeImgToSig(int[] img, float[] sig, ChannelNames fromChannel) {
		 PixelAudioMapper.pullPixelAudio(img, sig, fromChannel, hsbPixel);
	 }

	/**
	 * @param sig		an array of audio samples in the range [-1.0, 1.0], source
	 * @param img		an array of RGB pixel values, target
	 */
	public void writeSigToImg(float[] sig, int[] img) {
		 PixelAudioMapper.pushAudioPixel(sig, img, ChannelNames.ALL);
	 }

	 /**
	 * @param sig			an array of audio samples in the range [-1.0, 1.0], source
	 * @param img			an array of RGB pixel values, target
	 * @param toChannel		channel in RGB or HSB color space, from ChannelNames enum
	 */
	public void writeSigToImg(float[] sig, int[] img, ChannelNames toChannel) {
		 PixelAudioMapper.pushAudioPixel(sig, img, toChannel);
	 }

	
	//------------- SUBARRAYS -------------//

	/*
	 * In each case, a source subarray is either extracted from or inserted into a larger target array.
	 * When the small array, sprout, is inserted, it is indexed from 0..sprout.length. The larger array,
	 * img or sig, is indexed from read or write point signalPos to signalPos + length.
	 *
	 * All float[] arrays should contain audio range values [-1.0, 1.0].
	 * All int[] arrays should contain RGB pixel values.
	 * Array lengths are checked and adjusted but array values are not. 
	 * 
	 * Pluck and Plant methods follow the signal path.
	 * Peel and Stamp methods follow the row major image path.
	 * TODO more better explanations.
	 *
	 */

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
	// It's supposed to return one channel of RGB, and that might be useful, after all. Implement it. TODO
	  
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
	 * Helper method for color channel operations
	 * 
	 * @param rgb		an RGB color value
	 * @param channel   the channel to extract for the RGB color value
	 * @param hsbPixel  a local array for HSB values
	 * @return          the extracted channel as a float value
	 */
	private float extractChannelAsAudio(int rgb, ChannelNames channel, float[] hsbPixel) {
	    switch (channel) {
	        case L:   return hsbFloatToAudio(brightness(rgb, hsbPixel));
	        case H:   return hsbFloatToAudio(hue(rgb, hsbPixel));
	        case S:   return hsbFloatToAudio(saturation(rgb, hsbPixel));
	        case R:   return rgbChanToAudio((rgb >> 16) & 0xFF);
	        case G:   return rgbChanToAudio((rgb >> 8) & 0xFF);
	        case B:   return rgbChanToAudio(rgb & 0xFF);
	        case A:   return rgbChanToAudio((rgb >> 24) & 0xFF);
	        case ALL: return rgbFloatToAudio(0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF));
	        default:  throw new AssertionError("Unknown channel: " + channel);
	    }
	}


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
	        samples[j] = extractChannelAsAudio(rgb, fromChannel, hsbPixel);
	    }
		return samples;
	}

	
	/**
     * Starting at signalPos, reads length values from float array sig 
	 * and returns a new array of audio values in signal order. No redirection is needed when reading from the signal.
     * Note that <code>signalPos = this.imageToSignalLUT[x + y * this.width]</code> or <code>this.lookupSample(x, y)</code>.
     * Although in this method we don't use indirect indexing with LUTs, sig.length must equal this.width * this.height.
	 *
	 * @param sig			source array of audio values
	 * @param signalPos		a position in the sig array
	 * @param length		number of values to read from sig array
	 * @return				a new array with the audio values we read
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if sig.length != this.width * this.height
	 */
	public float[] pluckSamples(float[] sig, int signalPos, int length) {
	    if (sig == null) throw new IllegalArgumentException("sig array cannot be null");
	    if (sig.length != this.width * this.height) 
	        throw new IllegalArgumentException("sig length does not match PixelAudioMapper dimensions");
	    if (signalPos < 0 || signalPos >= sig.length) throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > sig.length) throw new IllegalArgumentException("Invalid length: out of bounds");
		float[] samples = new float[length];		
		for (int j = 0; j < length; j++) {
			samples[j] = sig[signalPos + j];
		}
		return samples;
	}

	/**
     * Starting at <code>signalPos</code>, reads <code>length</code> values from float array <code>sig</code> 
     * and transcodes and returns them as an RGB array in signal order.
     * Note that <code>signalPos = this.imageToSignalLUT[x + y * this.width]</code> or <code>this.lookupSample(x, y)</code>.
     * Although in this method we don't use indirect indexing with LUTs, sig.length must equal this.width * this.height.
     *
	 * @param sig			source array of audio values (-1.0f..1.0f)
	 * @param signalPos		entry point in the sig array
	 * @param length		number of values to read from the sig array
	 * @return				an array of RGB values where r == g == b, derived from the sig values
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if sig.length != this.width * this.height
	 */
	public int[] pluckSamplesAsRGB(float[] sig, int signalPos, int length) {
	    if (sig == null) throw new IllegalArgumentException("sig array cannot be null");
	    if (sig.length != this.width * this.height) 
	        throw new IllegalArgumentException("sig length does not match PixelAudioMapper dimensions");
	    if (signalPos < 0 || signalPos >= sig.length) throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > sig.length) throw new IllegalArgumentException("Invalid length: out of bounds");
		int[] rgbPixels = new int[length];
		int j = 0;
		for (int i = signalPos; i < signalPos + length; i++) {
			float sample = sig[i];
			sample = sample > 1.0f ? 1.0f : sample < -1.0f ? -1.0f : sample;		// a precaution, keep values within limits
			//int v = Math.round(PixelAudio.map(sample, -1.0f, 1.0f, 0, 255));		
			int v = audioToRGBChan(sample);                                         // map from [-1.0, 1.0] to [0, 255]
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
	 * Helper method for applying a color channel to an RGB pixel. 
	 * 
	 * @param sproutVal    source color vlaue
	 * @param imgVal       destination color value
	 * @param channel      the color channel to use
	 * @param hsbPixel     array of 3 floats to maintain HSB color data
	 * @return
	 */
	private int applyChannelToPixel(int sproutVal, int imgVal, ChannelNames channel, float[] hsbPixel) {
	    switch (channel) {
	        case L: {
	            Color.RGBtoHSB((imgVal >> 16) & 0xff, (imgVal >> 8) & 0xff, imgVal & 0xff, hsbPixel);
	            float br = brightness(sproutVal, hsbPixel);
	            return Color.HSBtoRGB(hsbPixel[0], hsbPixel[1], br);
	        }
	        case H: {
	            Color.RGBtoHSB((imgVal >> 16) & 0xff, (imgVal >> 8) & 0xff, imgVal & 0xff, hsbPixel);
	            float h = hue(sproutVal, hsbPixel);
	            return Color.HSBtoRGB(h, hsbPixel[1], hsbPixel[2]);
	        }
	        case S: {
	            Color.RGBtoHSB((imgVal >> 16) & 0xff, (imgVal >> 8) & 0xff, imgVal & 0xff, hsbPixel);
	            float s = saturation(sproutVal, hsbPixel);
	            return Color.HSBtoRGB(hsbPixel[0], s, hsbPixel[2]);
	        }
	        case R: {
	            int r = (sproutVal >> 16) & 0xFF;
	            return (255 << 24) | (r << 16) | ((imgVal >> 8) & 0xFF) << 8 | (imgVal & 0xFF);
	        }
	        case G: {
	            int g = (sproutVal >> 8) & 0xFF;
	            return (255 << 24) | ((imgVal >> 16) & 0xFF) << 16 | (g << 8) | (imgVal & 0xFF);
	        }
	        case B: {
	            int b = sproutVal & 0xFF;
	            return (255 << 24) | ((imgVal >> 16) & 0xFF) << 16 | ((imgVal >> 8) & 0xFF) << 8 | b;
	        }
	        case A: {
	            int a = (sproutVal >> 24) & 0xFF;
	            return (a << 24) | ((imgVal >> 16) & 0xFF) << 16 | ((imgVal >> 8) & 0xFF) << 8 | imgVal & 0xFF;
	        }
	        case ALL:
	        default:
	            return sproutVal;
	    }
	}

	/**
	 * Writes values from RGB source array sprout into img at positions mapped 
	 * by the signal path, using the specified channel.
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
	        img[imgIdx] = applyChannelToPixel(sprout[j], img[imgIdx], toChannel, hsbPixel);
	    }
	}

	/**
	 * Helper: Applies a float audio sample to a pixel channel, using the channel kind.
	 */
	private int applyAudioToPixelChannel(float sample, int rgb, ChannelNames channel) {
	    switch (channel) {
	        case L:   return PixelAudioMapper.applyBrightness(sample, rgb);
	        case H:   return PixelAudioMapper.applyHue(sample, rgb);
	        case S:   return PixelAudioMapper.applySaturation(sample, rgb);
	        case R:   return PixelAudioMapper.applyRed(sample, rgb);
	        case G:   return PixelAudioMapper.applyGreen(sample, rgb);
	        case B:   return PixelAudioMapper.applyBlue(sample, rgb);
	        case A:   return PixelAudioMapper.applyAlpha(sample, rgb);
	        case ALL: return PixelAudioMapper.applyAll(sample, rgb);
	        default:  throw new AssertionError("Unknown channel: " + channel);
	    }
	}
	
	/**
	 * Writes values from audio data array sprout into the specified channel of the img array
	 * at positions mapped by the signal path, starting at signalPos for the given length.
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
	    for (int j = 0; j < length; j++) {
	        int imgIdx = this.signalToImageLUT[signalPos + j];
	        img[imgIdx] = applyAudioToPixelChannel(sprout[j], img[imgIdx], toChannel);
	    }
	}

	/**
	 * Starting at signalPos, insert length audio samples from source array sprout 
	 * into target array of audio samples sig. No lookup tables are used.
	 *
	 * @param sprout		source array of audio values (-1.0f..1.0f)
	 * @param sig			target array of signal values, in signal order
	 * @param signalPos		start point in sig array
	 * @param length		number of values to copy from sprout array to sig array
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if sig.length != this.width * this.height
	 */
	public void plantSamples(float[] sprout, float[] sig, int signalPos, int length) {
	    if (sprout == null || sig == null) 
	    	throw new IllegalArgumentException("Input arrays cannot be null");
	    if (sig.length != this.width * this.height)
	        throw new IllegalArgumentException("sig length does not match PixelAudioMapper dimensions");
	    if (signalPos < 0 || signalPos >= sig.length)
	        throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > sig.length || length > sprout.length)
	        throw new IllegalArgumentException("Invalid length: out of bounds");
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
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if sig.length != this.width * this.height
	 */
	public void plantSamples(int[] sprout, float[] sig, int signalPos, int length) {
	    if (sprout == null || sig == null) 
	    	throw new IllegalArgumentException("Input arrays cannot be null");
	    if (sig.length != this.width * this.height)
	        throw new IllegalArgumentException("sig length does not match PixelAudioMapper dimensions");
	    if (signalPos < 0 || signalPos >= sig.length)
	        throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > sig.length || length > sprout.length)
	        throw new IllegalArgumentException("Invalid length: out of bounds");
		int j = 0;
		for (int i = signalPos; i < signalPos + length; i++) {
			sig[i] = rgbChanToAudio(PixelAudioMapper.getGrayscale(sprout[j++]));
		}
	}

	/**
	 * Writes values from an int array (sprout) into the specified channel of the sig array
	 * at positions starting at signalPos for the given length, converting pixel color to audio.
	 *
	 * @param sprout      source array of RGB pixel values
	 * @param sig         target array of audio samples (float, [-1.0, 1.0])
	 * @param signalPos   position to start writing into sig
	 * @param length      number of values to write
	 * @param fromChannel channel to extract from sprout values
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if sig.length != this.width * this.height
	 */
	public void plantSamples(int[] sprout, float[] sig, int signalPos, int length, ChannelNames fromChannel) {
	    if (sprout == null || sig == null)
	        throw new IllegalArgumentException("Input arrays cannot be null");
	    if (sig.length != this.width * this.height)
	        throw new IllegalArgumentException("sig length does not match PixelAudioMapper dimensions");
	    if (signalPos < 0 || signalPos >= sig.length)
	        throw new IndexOutOfBoundsException("signalPos out of bounds");
	    if (length < 0 || signalPos + length > sig.length || length > sprout.length)
	        throw new IllegalArgumentException("Invalid length: out of bounds");
	    float[] hsbPixel = new float[3]; 		// local for thread safety
	    for (int j = 0; j < length; j++) {
	        sig[signalPos + j] = extractChannelAsAudio(sprout[j], fromChannel, hsbPixel);
	    }
	}
	
	
	
	/*-------------------------- PEEL AND STAMP METHODS --------------------------*/

	/**
	 * Copies a rectangular area of pixels in image (row-major) order and returns it 
	 * as an array of RGB values (a standard operation) Note that the image 
	 * must conform to the dimensions this.width and this.height. 
	 * 
	 * @param img the image pixel array (row-major, length == width * height)
	 * @param x   left edge of rectangle
	 * @param y   top edge of rectangle
	 * @param w   width of rectangle
	 * @param h   height of rectangle
	 * @return    array of int (RGB values), length w*h
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
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
	
	/**
	 * Copies a rectangular area of pixels in image (row-major) order and returns 
	 * it as an array of audio values ([-1.0, 1.0]).
	 * The array img must conform to the dimensions this.width and this.height. 
	 * 
	 * @param img the image pixel array (row-major, length == width * height)
	 * @param x   left edge of rectangle
	 * @param y   top edge of rectangle
	 * @param w   width of rectangle
	 * @param h   height of rectangle
	 * @return    array of float (audio values), length w*h
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
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
				samples[j++] =  rgbChanToAudio(PixelAudioMapper.getGrayscale(img[rowStart + dx]));
			}
		}
		return samples;
	}

	/**
	 * Copies a rectangular area of audio values from a signal mapped to an image 
	 * using imageToSignalLUT to index values. Note that sig.length must equal
	 * this.width * this.height, i.e., it is a signal conformed to this instance
	 * of a PixelAudioMapper. With the resulting array you could, 
	 * for example, run a 2D filter over selected 1D audio data. 
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
	 * using imageToSignalLUT to index values. Note that sig.length must equal
	 * this.width * this.height.
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


	/**
	 * Pastes a source array of RGB data into a rectangular area of a destination image 
	 * (a standard operation). Image area must equal this.width * this.height.
	 * 
	 * @param stamp    a source array of RGB data
	 * @param img      a destination image
	 * @param x        leftmost x-coordinate of a rectangular area in the destination image
	 * @param y        topmost y-coordinate of a rectangular area in the destination image
	 * @param w        width of rectangular area
	 * @param h        height of rectangular area
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
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

	/**
	 * Pastes a specified channel of a source array of RGB data into a rectangular area of 
	 * a destination image (a standard operation). Image area must equal this.width * this.height.
	 * 
	 * @param stamp        a source array of RGB data
	 * @param img          a destination image
	 * @param x            leftmost x-coordinate of a rectangular area in the destination image
	 * @param y            topmost y-coordinate of a rectangular area in the destination image
	 * @param w            width of rectangular area
	 * @param h            height of rectangular area
	 * @param toChannel    color channel to write to
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
	 *         or if img.length != this.width * this.height
	 */
	public void stampPixels(int[] stamp, int[] img, int x, int y, int w, int h, ChannelNames toChannel) {
	    if (stamp == null) throw new IllegalArgumentException("stamp array cannot be null");
	    if (img.length != this.width * this.height)
	        throw new IllegalArgumentException("img length does not match PixelAudioMapper dimensions");
	    if (w <= 0 || h <= 0) throw new IllegalArgumentException("width and height must be positive");
	    if (x < 0 || y < 0 || x + w > this.width || y + h > this.height)
	        throw new IllegalArgumentException("Requested rectangle is out of image bounds");
		int j = 0;
		switch (toChannel) {
		case L: {
			for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;				
				for (int dx = x; dx < x + w; dx++) {
					int rgb = img[rowStart + dx];
					Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
					rgb = Color.HSBtoRGB(hsbPixel[0], hsbPixel[1], brightness(stamp[j], hsbPixel));
					img[rowStart + dx] = rgb;
					j++;
				}
			}
			break;
		}
		case H: {
			for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
				for (int dx = x; dx < x + w; dx++) {
					int rgb = img[rowStart + dx];
					Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
					rgb = Color.HSBtoRGB(hue(stamp[j], hsbPixel), hsbPixel[1], hsbPixel[2]);
					img[rowStart + dx] = rgb;
					j++;
				}
			}
			break;
		}
		case S: {
			for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
				for (int dx = x; dx < x + w; dx++) {
					int rgb = img[rowStart + dx];
					Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);
					rgb = Color.HSBtoRGB(hsbPixel[0], saturation(stamp[j], hsbPixel), hsbPixel[2]);
					img[rowStart + dx] = rgb;
					j++;
				}
			}
			break;
		}
		case R: {
			for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
				for (int dx = x; dx < x + w; dx++) {
					int rgb = img[rowStart + dx];
					int r = (stamp[j] << 16) & 0xFF;
					img[rowStart + dx] = 255 << 24 | r << 16 | ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF;

					j++;
				}
			}
			break;
		}
		case G: {
			for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
				for (int dx = x; dx < x + w; dx++) {
					int rgb = img[rowStart + dx];
					int g = (stamp[j] << 8) & 0xFF;
					img[rowStart + dx] = 255 << 24 | ((rgb >> 16) & 0xFF) << 16 | g << 8 | rgb & 0xFF;
					j++;
				}
			}
			break;
		}
		case B: {
			for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
				for (int dx = x; dx < x + w; dx++) {
					int rgb = img[rowStart + dx];
					int b = stamp[j] & 0xFF;
					img[rowStart + dx] = 255 << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | b & 0xFF;
					j++;
				}
			}
			break;
		}
		case A: {
			for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
				for (int dx = x; dx < x + w; dx++) {
					int rgb = img[rowStart + dx];
					int a = stamp[j] << 24;
					img[rowStart + dx] = a << 24 | ((rgb >> 16) & 0xFF) << 16 | ((rgb >> 8) & 0xFF) << 8 | rgb & 0xFF;
					j++;
				}
			}
			break;
		}
		case ALL: {
			for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
				for (int dx = x; dx < x + w; dx++) {
					img[rowStart + dx] = stamp[j];
					j++;
				}
			}
			break;
		}
		}
	}

	/**
	 * Pastes a source array of audio data into a specified color channel of a rectangular
	 * area of a destination image. Image area must equal this.width * this.height.
	 * 
	 * @param stamp        a source array of audio data ([-1.0, 1.0])
	 * @param img          a destination image
	 * @param x            leftmost x-coordinate of a rectangular area in the destination image
	 * @param y            topmost y-coordinate of a rectangular area in the destination image
	 * @param w            width of rectangular area
	 * @param h            height of rectangular area
	 * @param toChannel    color channel to write to
	 * @throws IllegalArgumentException if parameters are out of bounds or arrays are null, 
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
		switch (toChannel) {
			case L: {
				for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
					for (int dx = x; dx < x + w; dx++) {
						img[rowStart + dx] = PixelAudioMapper.applyBrightness(stamp[j++], img[rowStart + dx]);
					}
				}
				break;
			}
			case H: {
				for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
					for (int dx = x; dx < x + w; dx++) {
						img[rowStart + dx] = PixelAudioMapper.applyHue(stamp[j++], img[dx + dy * w]);
					}
				}
				break;
			}
			case S: {
				for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
					for (int dx = x; dx < x + w; dx++) {
						img[rowStart + dx] = PixelAudioMapper.applySaturation(stamp[j++], img[rowStart + dx]);
					}
				}
				break;
			}
			case R: {
				for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
					for (int dx = x; dx < x + w; dx++) {
						img[rowStart + dx] = PixelAudioMapper.applyRed(stamp[j++], img[rowStart + dx]);
					}
				}
				break;
			}
			case G: {
				for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
					for (int dx = x; dx < x + w; dx++) {
						img[rowStart + dx] = PixelAudioMapper.applyGreen(stamp[j++], img[rowStart + dx]);
					}
				}
				break;
			}
			case B: {
				for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
					for (int dx = x; dx < x + w; dx++) {
						img[rowStart + dx] = PixelAudioMapper.applyBlue(stamp[j++], img[rowStart + dx]);
					}
				}
				break;
			}
			case A: {
				for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
					for (int dx = x; dx < x + w; dx++) {
						img[rowStart + dx] = PixelAudioMapper.applyAlpha(stamp[j++], img[rowStart + dx]);
					}
				}
				break;
			}
			case ALL: {
				for (int dy = y; dy < y + h; dy++) {
				int rowStart = dy * this.width;
					for (int dx = x; dx < x + w; dx++) {
						img[rowStart + dx] = PixelAudioMapper.applyAll(stamp[j++], img[dx + dy * w]);
					}
				}
				break;
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


	// ------------- ARRAY ROTATION ------------- //

	/**
	 * Rotates an array of ints left by d values. Uses efficient "Three Rotation" algorithm.
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
	public static final int getGrayscale(int rgb) {
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

	public static int applyHue(float sample, int rgb) {
		float[] hsbPixel = new float[3];												// local var so we can make static
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

	public static int applyHue(int rgbSource, int rgb) {
		float[] hsbPixel = new float[3];												// local var so we can make static
		Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, hsbPixel);	// pop over to HSB
		return Color.HSBtoRGB(PixelAudioMapper.hue(rgbSource), hsbPixel[1], hsbPixel[2]);
	}

	public static int applySaturation(int rgbSource, int rgb) {
		float[] hsbPixel = new float[3];												// local var so we can make static
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

	

	// ------------- AUDIO <---> IMAGE ------------- //

	/**
	 * Converts a pixel channel value to an audio sample value, mapping the result to  [-1.0, 1.0].
	 *
	 * @param rgb		an RGB pixel value
	 * @param chan		channel to extract from the RGB pixel value
	 * @return
	 */
	public static float pullPixelAudio(int rgb, ChannelNames chan, float[] hsbPixel) {
		float sample = 0;
		switch (chan) {
		case L: {
			sample = hsbFloatToAudio(brightness(rgb, hsbPixel));
			break;
		}
		case H: {
			sample = hsbFloatToAudio(hue(rgb, hsbPixel));
			break;
		}
		case S: {
			sample = hsbFloatToAudio(saturation(rgb, hsbPixel));
			break;
		}
		case R: {
			sample = rgbChanToAudio(((rgb >> 16) & 0xFF));
			break;
		}
		case G: {
			sample = rgbChanToAudio(((rgb >> 8) & 0xFF));
			break;
		}
		case B: {
			sample = rgbChanToAudio((rgb & 0xFF));
			break;
		}
		case A: {
			sample = rgbChanToAudio(((rgb >> 24) & 0xFF));
			break;
		}
		case ALL: {
            // not a simple case, but we'll convert to grayscale using the "luminosity equation."
			// The brightness value in HSB (case L, above) or the L channel in Lab color spaces could be used, too.
			sample = rgbFloatToAudio((0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF)));
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
	 * @param samples		an array of audio samples, which may be null, whose values will be set from rgbPixels
	 * @param chan		    channel to extract from the RGB pixel values
	 * 						Will be initialized and returned if null
	 * @return              a array of floats mapped to the audio range, assigned to samples
	 */
     public static float[] pullPixelAudio(int[] rgbPixels, float[] samples, ChannelNames chan, float[] hsbPixel) {
    	if (samples == null) {
    		samples = new float[rgbPixels.length];
    	}
		switch (chan) {
		case L: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = hsbFloatToAudio(brightness(rgbPixels[i], hsbPixel));
            }
            break;
		}
		case H: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = hsbFloatToAudio(hue(rgbPixels[i], hsbPixel));
            }
			break;
		}
		case S: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = hsbFloatToAudio(saturation(rgbPixels[i], hsbPixel));
            }
			break;
		}
		case R: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = rgbChanToAudio(((rgbPixels[i] >> 16) & 0xFF));
            }
			break;
		}
		case G: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = rgbChanToAudio(((rgbPixels[i] >> 8) & 0xFF));
            }
			break;
		}
		case B: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = rgbChanToAudio((rgbPixels[i] & 0xFF));
            }
			break;
		}
		case A: {
            for (int i = 0; i < samples.length; i++) {
	    		samples[i] = rgbChanToAudio(((rgbPixels[i] >> 24) & 0xFF));
            }
			break;
		}
		case ALL: {
            // not a simple case, but we'll convert to grayscale using the "luminosity equation."
			// The brightness value in HSB (case L, above) or the L channel in Lab color spaces could be used, too.
             for (int i = 0; i < samples.length; i++) {
                int rgb = rgbPixels[i];
    			samples[i] = rgbFloatToAudio((0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF)));
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
	 * @param samples		an array of audio samples, which may be null, whose values will be set from rgbPixels.
	 * 						Will be initialized and returned if null.
	 * @return              a array of floats mapped to the audio range, identical to samples
	 */
     public static float[] pullPixelAudio(int[] rgbPixels, int[] lut, float[] samples, ChannelNames chan, float[] hsbPixel) {
    	if (samples == null) {
    		samples = new float[rgbPixels.length];
    	}
		switch (chan) {
		case L: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = hsbFloatToAudio(brightness(rgbPixels[lut[i]], hsbPixel));
            }
            break;
		}
		case H: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = hsbFloatToAudio(hue(rgbPixels[lut[i]], hsbPixel));
            }
			break;
		}
		case S: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = hsbFloatToAudio(saturation(rgbPixels[lut[i]], hsbPixel));
            }
			break;
		}
		case R: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = rgbChanToAudio(((rgbPixels[lut[i]] >> 16) & 0xFF));
            }
			break;
		}
		case G: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = rgbChanToAudio(((rgbPixels[lut[i]] >> 8) & 0xFF));
            }
			break;
		}
		case B: {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = rgbChanToAudio((rgbPixels[lut[i]] & 0xFF));
            }
			break;
		}
		case A: {
            for (int i = 0; i < samples.length; i++) {
	    		samples[i] = rgbChanToAudio(((rgbPixels[lut[i]] >> 24) & 0xFF));
            }
			break;
		}
		case ALL: {
            // not a simple case, but we'll convert to grayscale using the "luminosity equation."
			// The brightness value in HSB (case L, above) or the L channel in Lab color spaces could be used, too.
             for (int i = 0; i < samples.length; i++) {
                int rgb = rgbPixels[lut[i]];
    			samples[i] = rgbFloatToAudio((0.3f * ((rgb >> 16) & 0xFF) + 0.59f * ((rgb >> 8) & 0xFF) + 0.11f * (rgb & 0xFF)));
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
	public static float[] pullRawChannel(int[] rgbPixels, ChannelNames chan, float[] hsbPixel) {
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


	// ------------- FLOAT VERSIONS OF PUSH METHODS -------------0 //

	
	/**
	 * Applies an audio value (source) to an RGB color (target) in a selected channel. 
	 * The audio is mapped from the interval (-1.0, 1.0) to (0..255).
	 * 
	 * @param sample    an audio sample
	 * @param rgb       an RGB color
	 * @param chan      a ChannelNames value selecting the channel to use
	 * @return          an RGB color with the selected channel modified by the sample value
	 */
	public static int pushAudioPixel(float sample, int rgb, ChannelNames chan) {
		switch (chan) {
		case L: {
			rgb = PixelAudioMapper.applyBrightness(sample, rgb);
			break;
		}
		case H: {
			rgb = PixelAudioMapper.applyHue(sample, rgb);
			break;
		}
		case S: {
			rgb = PixelAudioMapper.applySaturation(sample, rgb);
			break;
		}
		case R: {
			rgb = PixelAudioMapper.applyRed(sample, rgb);
			break;
		}
		case G: {
			rgb = PixelAudioMapper.applyGreen(sample, rgb);
			break;
		}
		case B: {
			rgb = PixelAudioMapper.applyBlue(sample, rgb);
			break;
		}
		case A: {
			rgb = PixelAudioMapper.applyAlpha(sample, rgb);
			break;
		}
		case ALL: {
			rgb = PixelAudioMapper.applyAll(sample, rgb);
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
	 * @param rgbPixels    an array of pixel values
	 * @param buf          an array of floats in the range  (-1.0, 1.0)
	 * @param chan         the channel to replace
	 * @return			   rgbPixels with the selected channel modified by the buf values
	 */
	public static int[] pushAudioPixel(float[] buf, int[] rgbPixels, ChannelNames chan) {
		// it's unlikely that this will happen, but if rgbPixels is null, create an array and fill it with middle gray color
		if (rgbPixels == null) {
			rgbPixels = new int[buf.length];
			Arrays.fill(rgbPixels, PixelAudioMapper.composeColor(127, 127, 127));
		}
		switch (chan) {
		case L: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = PixelAudioMapper.applyBrightness(buf[i], rgbPixels[i]);
			}
			break;
		}
		case H: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = PixelAudioMapper.applyHue(buf[i], rgbPixels[i]);
			}
			break;
		}
		case S: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = PixelAudioMapper.applySaturation(buf[i], rgbPixels[i]);
			}
			break;
		}
		case R: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = PixelAudioMapper.applyRed(buf[i], rgbPixels[i]);
			}
			break;
		}
		case G: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] =  PixelAudioMapper.applyGreen(buf[i], rgbPixels[i]);
			}
			break;
		}
		case B: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = PixelAudioMapper.applyBlue(buf[i], rgbPixels[i]);
			}
			break;
		}
		case A: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = PixelAudioMapper.applyAlpha(buf[i], rgbPixels[i]);
			}
			break;
		}
		case ALL: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = PixelAudioMapper.applyAll(buf[i], rgbPixels[i]);
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
	 * @param rgbPixels    an array of pixel values
	 * @param buf          an array of floats in the range  [-1.0, 1.0]
	 * @param lut		   a lookup table to redirect the indexing of the buf, typically imageToPixelsLUT
	 * @param chan         the channel to replace
	 * @return			   rgbPixels with selected channel values modified by the buf values
	 */
	public static int[] pushAudioPixel(float[] buf, int[] rgbPixels, int[] lut, ChannelNames chan) {
		// it's unlikely that this will happen, but if rgbPixels is null, create an array and fill it with middle gray color
		if (rgbPixels == null) {
			rgbPixels = new int[buf.length];
			Arrays.fill(rgbPixels, PixelAudioMapper.composeColor(127, 127, 127));
		}
		switch (chan) {
		case L: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = PixelAudioMapper.applyBrightness(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case H: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = PixelAudioMapper.applyHue(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case S: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = PixelAudioMapper.applySaturation(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case R: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = PixelAudioMapper.applyRed(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case G: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] =  PixelAudioMapper.applyGreen(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case B: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = PixelAudioMapper.applyBlue(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case A: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = PixelAudioMapper.applyAlpha(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case ALL: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = PixelAudioMapper.applyAll(buf[i], rgbPixels[lut[i]]);
			}
			break;
		}
		}  // end switch
		return rgbPixels;
	}

	
	// ------------- INTEGER VERSIONS OF PUSH METHODS -------------0 //
	
	/**
	 * Applies an RGB color value (source) to an RGB color (target) in a selected channel. 
	 * 
	 * @param rgbSource    an RGB color sample
	 * @param rgb          an RGB color
	 * @param chan         a ChannelNames value selecting the channel to use
	 * @return             an RGB color with the selected channel modified by the sample value
	 */
	public static int pushAudioPixel(int rgbSource, int rgb, ChannelNames chan) {
		switch (chan) {
		case L: {
			rgb = PixelAudioMapper.applyBrightness(rgbSource, rgb);
			break;
		}
		case H: {
			rgb = PixelAudioMapper.applyHue(rgbSource, rgb);
			break;
		}
		case S: {
			rgb = PixelAudioMapper.applySaturation(rgbSource, rgb);
			break;
		}
		case R: {
			rgb = PixelAudioMapper.applyRed(rgbSource, rgb);
			break;
		}
		case G: {
			rgb = PixelAudioMapper.applyGreen(rgbSource, rgb);
			break;
		}
		case B: {
			rgb = PixelAudioMapper.applyBlue(rgbSource, rgb);
			break;
		}
		case A: {
			rgb = PixelAudioMapper.applyAlpha(rgbSource, rgb);
			break;
		}
		case ALL: {
			rgb = PixelAudioMapper.applyAll(rgbSource, rgb);
			break;
		}
		}
		return rgb;
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
	public static int[] pushAudioPixel(int[] colors, int[] rgbPixels, ChannelNames chan) {
		// it's unlikely that this will happen, but if rgbPixels is null, create an array and fill it with middle gray color
		if (rgbPixels == null) {
			rgbPixels = new int[colors.length];
			Arrays.fill(rgbPixels, PixelAudioMapper.composeColor(127, 127, 127));
		}
		switch (chan) {
		case L: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = PixelAudioMapper.applyBrightness(colors[i], rgbPixels[i]);
			}
			break;
		}
		case H: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = PixelAudioMapper.applyHue(colors[i], rgbPixels[i]);
			}
			break;
		}
		case S: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = PixelAudioMapper.applySaturation(colors[i], rgbPixels[i]);
			}
			break;
		}
		case R: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = PixelAudioMapper.applyRed(colors[i], rgbPixels[i]);
			}
			break;
		}
		case G: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] =  PixelAudioMapper.applyGreen(colors[i], rgbPixels[i]);
			}
			break;
		}
		case B: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = PixelAudioMapper.applyBlue(colors[i], rgbPixels[i]);
			}
			break;
		}
		case A: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = PixelAudioMapper.applyAlpha(colors[i], rgbPixels[i]);
			}
			break;
		}
		case ALL: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[i] = PixelAudioMapper.applyAll(colors[i], rgbPixels[i]);
			}
			break;
		}
		}  // end switch
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
	public static int[] pushAudioPixel(int[] colors, int[] rgbPixels, int[] lut, ChannelNames chan) {
		// it's unlikely that this will happen, but if rgbPixels is null, create an array and fill it with middle gray color
		if (rgbPixels == null) {
			rgbPixels = new int[colors.length];
			Arrays.fill(rgbPixels, PixelAudioMapper.composeColor(127, 127, 127));
		}
		switch (chan) {
		case L: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = PixelAudioMapper.applyBrightness(colors[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case H: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = PixelAudioMapper.applyHue(colors[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case S: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = PixelAudioMapper.applySaturation(colors[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case R: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = PixelAudioMapper.applyRed(colors[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case G: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] =  PixelAudioMapper.applyGreen(colors[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case B: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = PixelAudioMapper.applyBlue(colors[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case A: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = PixelAudioMapper.applyAlpha(colors[i], rgbPixels[lut[i]]);
			}
			break;
		}
		case ALL: {
			for (int i = 0; i < rgbPixels.length; i++) {
				rgbPixels[lut[i]] = PixelAudioMapper.applyAll(colors[i], rgbPixels[lut[i]]);
			}
			break;
		}
		}  // end switch
		return rgbPixels;
	}


}




