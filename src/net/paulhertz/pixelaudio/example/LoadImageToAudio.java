package net.paulhertz.pixelaudio.example;

import processing.core.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.sound.sampled.*;

import ddf.minim.*;
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;
import net.paulhertz.pixelaudio.sampler.*;
import net.paulhertz.pixelaudio.schedule.TimedLocation;

/**
 * LoadImageToAudio shows how to load an image and turn it into an audio file
 * that can be played by clicking on the image. You can also load an audio file
 * and turn it into an image. 
 * <h2>QUICK START</h2>
 * <ol> 
 *   <li>Run LoadImageToAudio. The sketch shows a rainbow-colored image generated 
 *   by the getColors() method.</li>
 *   <li>Press 'o' to open an image file. Image and audio files for the PixelAudio
 *   example sketches can be found in the PixelAudio library "examples/example_data/" 
 *   directory. Your selected image appears in the display and is transcoded to audio.</li>
 *   <li>Press the spacebar while the mouse is over the image to play its transcoded
 *   audio signal.</li>
 *   <li>Experiment with loading images and audio files to the various color 
 *   channels. Press '?' to see the available key commands ('h' is reserved for
 *   the Hue channel).</li>
 * </ol>
 * <p>
 * You can write the current image to the audio signal with the 'w' key command. 
 * The sound of an image will probably be noisy since it is not designed with cyclic 
 * functions along the arbitrary signal path we impose on it with a PixelMapGen.
 * RGB values range in integer steps from 0 to 255. When we transcode them to 
 * audio values in the range (-1.0, 1.0), we have less resolution than the full 
 * range of floating point values. There's always some noise in values transcoded 
 * from images. In most of the examples for the PixelAudio library, when you load 
 * an audio file and it gets transcoded into an image we still use the audio 
 * signal with all its resolution to play sounds. When you click in the image, 
 * you will be playing a sample from the signal. 
 * </p><p>
 * You can write the audio signal to the image with the 'W' key command. This will 
 * convert the audioSignal into HSB Brightness values and write them to mapImage.
 * If you open this image in all color channels or in the HSB Brightness channel 
 * and then write it to the audio channel, you will get a reasonably good recreation
 * of the audio, at 8-bit resolution.
 * </p><p>
 * An audio signal or image can be loaded to various channels of the image: Red, 
 * Green, Blue or all channels in the RGB color space or Hue, Brightness, or
 * Saturation in the HSB color space. HSB Hue operations on grayscale images may 
 * result in no change to the image. Grayscale images have no saturation or hue,
 * only brightness, when they are represented in the HSB color space. Loading the
 * saturation of a grayscale image or a transcoded audio file to a color image
 * will turn it gray. To work more effectively with HSB, we can load both hue and
 * saturation from a color image to another color image or to a grayscale image, 
 * maintaining the brightness channel of the target image, with the 'c' command key.
 * </p><p>
 * You can enhance image contrast by stretching its histogram ('m' key).
 * You can make the image brighter ('=' and '+' keys) or darker ('-' or '_' key)
 * using a gamma function, a non-linear adjustment. 
 * </p>
 * <pre>
 * Press ' ' to play audio for the point the mouse is currently over.
 * Press TAB to toggle animation.
 * Press 'o' to open an audio or image file in all RGB channels.
 * Press 'r' to open an audio or image file in the RED channel of the image.
 * Press 'g' to open an audio or image file in the GREEN channel of the image.
 * Press 'b' to open an audio or image file in the BLUE channel of the image.
 * Press 'h' to open an audio or image file in the HSB Hue channel of the image.
 * Press 'v' to open an audio or image file in the HSB Saturation channel of the image.
 * Press 'l' to open an audio or image file in the HSB Brightness channel of the image.
 * Press 'c' to apply color from an image file to the display image.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage.
 * Press 'O' to reload the most recent image or audio file or show an Open File dialog.
 * Press 'm' to remap the histogram of the image.
 * Press '=' to use a gamma function to make the image lighter.
 * Press '-' to use a gamma function to make the image darker.
 * Press 'S' to save the audio signal to an audio file.
 * Press 's' to save the image to a PNG file.
 * Press 'f' to print the current frame rate to the console.
 * Press 'w' to transcode the image and write it to the audio signal.
 * Press 'W' to transcode the audio signal and write it to the image.
 * Press '?' to show the Help Message in the console.
 * </pre>
 * 
 * PLEASE NOTE: Hue (H) and Saturation (V) operations may have no effect on gray pixels.<br>
 * ALSO: Image brightness determines image audio. Images with uniform brightness will be silent.<br>
 * 
 */
public class LoadImageToAudio extends PApplet {
	// PixelAudio vars and objects
	PixelAudio pixelaudio;     // our shiny new library
	MultiGen multigen;         // a PixelMapGen that handles multiple gens
	ArrayList<PixelMapGen> genList;    // list of PixelMapGens that create an image using mapper
	ArrayList<int[]> offsetList;       // list of x,y coordinates for placing gens from genList
	int genWidth = 512;       // width of HilbertGen used in multiGen, must be a power of 2
	int genHeight = 512;      // height of HilbertGen used in multigen, for hGen must be equal to width
	PixelAudioMapper mapper;  // object for reading, writing, and transcoding audio and image data
	int mapSize;              // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
	PImage mapImage;          // image for display
	PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;
	int[] colors;             // array of spectral colors

	// Java random number generator
	Random rando;

	/** Minim audio library */
	Minim minim;              // library that handles audio
	AudioOutput audioOut;     // line out to sound hardware
	MultiChannelBuffer playBuffer;     // data structure to hold audio samples
	boolean isBufferStale = false;     // do we need to reset the audio buffer?
	float sampleRate = 44100;   // audioOut sample rate
	float[] audioSignal;      // the audio signal as an array
	int[] rgbSignal;          // the colors in the display image, in the order the signal path visits them
	int audioLength;
	int audioFileLength;

	// SampleInstrument setup
	int sampleLen;
	int durationMS = 2000;
	PASamplerInstrument synth;      // instrument to generate audio events
	// ADSR and params
	ADSRParams adsr;                // good old attack, decay, sustain, release
	float maxAmplitude = 0.9f;
	float attackTime = 0.2f;
	float decayTime = 0.125f;
	float sustainLevel = 0.5f;
	float releaseTime = 0.2f;

	// audio file
	File audioFile;
	String audioFilePath;
	String audioFileName;
	String audioFileTag;

	// image file
	File imageFile;
	String imageFilePath;
	String imageFileName;
	String imageFileTag;
	int imageFileWidth;
	int imageFileHeight;
	
	boolean isLoadFromImage = false;

	// animation
	boolean isAnimating = false;       // animation status
	boolean oldIsAnimating;            // keep old animation status if we suspend animation
	int shift = 1024;                  // number of pixels to shift the animation
	int totalShift = 0;                // cumulative shift
	boolean isLooping = true;          // looping sample (our instrument ignores this)
	// interaction
	int sampleX;      // x-coordinate of a sample point on the image
	int sampleY;      // y-coordinate of a sample point on the image
	int samplePos;    // position of a mouse click along the signal path, index into the audio array
	ArrayList<TimedLocation> timeLocsArray;
	int count = 0;
	// histogram and gamma adjustments
	int histoHigh = 240;
	int histoLow = 32;
	float gammaLighter = 0.9f;
	float gammaDarker = 1.2f;
	int[] gammaTable;


	public static void main(String[] args) {
		PApplet.main(new String[] { LoadImageToAudio.class.getName() });
	}

	public void settings() {
		size(3 * genWidth, 2 * genHeight);
	}

	public void setup() {
		frameRate(30);
		pixelaudio = new PixelAudio(this);
		sampleRate = 44100;
		rando = new Random();
		genList = new ArrayList<PixelMapGen>();
		multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
		mapper = new PixelAudioMapper(multigen);
		mapSize = mapper.getSize();
		initAudio();
		initImage();
		timeLocsArray = new ArrayList<TimedLocation>();
		showHelp();
	}

	/**
	 * Initialize audio variables
	 */
	public void initAudio() {
		this.minim = new Minim(this);
		// use the getLineOut method of the Minim object to get an AudioOutput object
		this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
		// create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
		// playBuffer will not contain audio data until we load a file
		this.playBuffer = new MultiChannelBuffer(mapSize, 1);
		this.audioSignal = playBuffer.getChannel(0);
		this.audioLength = audioSignal.length;
		// ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
		adsr = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
		// create a PASamplerInstrument with 8 voices, adsrParams will be its default envelope
		synth = new PASamplerInstrument(playBuffer, audioOut.sampleRate(), 8, audioOut, adsr);		
	}

	/**
	 * Initialize mapImage and associated variables
	 */
	public void initImage() {
		colors = getColors(); // create an array of rainbow colors
		mapImage = createImage(width, height, ARGB); // an image to use with mapper
		mapImage.loadPixels();
		mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
		mapImage.updatePixels();
	}

	/**
	 * @return an array of RGB colors that cover a full rainbow spectrum
	 */
	public int[] getColors() {
		int[] colorWheel = new int[mapSize]; // an array for our colors
		pushStyle(); // save styles
		colorMode(HSB, colorWheel.length, 100, 100); // pop over to the HSB color space and give hue a very wide range
		for (int i = 0; i < colorWheel.length; i++) {
			colorWheel[i] = color(i, 40, 75); // fill our array with colors, gradually changing hue
		}
		popStyle(); // restore styles, including the default RGB color space
		return colorWheel;
	}

	public void draw() {
		image(mapImage, 0, 0);
		if (isAnimating)
			stepAnimation();
		runTimeArray();
	}

	public void stepAnimation() {
		mapImage.loadPixels();
		rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
		PixelAudioMapper.rotateLeft(rgbSignal, shift);
		mapper.plantPixels(rgbSignal, mapImage.pixels, 0, mapSize);
		mapImage.updatePixels();
		totalShift += shift;
	}

	public void runTimeArray() {
		int currentTime = millis();
		timeLocsArray.forEach(tl -> {
			tl.setStale(tl.eventTime() < currentTime);
			if (!tl.isStale()) {
				drawCircle(tl.getX(), tl.getY());
			}
		});
		timeLocsArray.removeIf(TimedLocation::isStale);
	}

	public void drawCircle(int x, int y) {
		fill(color(233, 220, 199, 128));
		noStroke();
		circle(x, y, 60);
	}	
	
	public void keyPressed() {
		switch (key) {
		case ' ': // play audio for the point the mouse is currently over
			audioMouseClick(mouseX, mouseY);
			break;
		case TAB: // toggle animation
			isAnimating = !isAnimating;
			break;
		case 'o': // open an audio or image file in all RGB channels
			chan = PixelAudioMapper.ChannelNames.ALL;
			chooseFile();
			break;
		case 'r': // open an audio or image file in the RED channel of the image
			chan = PixelAudioMapper.ChannelNames.R;
			chooseFile();
			break;
		case 'g': // open an audio or image file in the GREEN channel of the image
			chan = PixelAudioMapper.ChannelNames.G;
			chooseFile();
			break;
		case 'b': // open an audio or image file in the BLUE channel of the image
			chan = PixelAudioMapper.ChannelNames.B;
			chooseFile();
			break;
		case 'h': // open an audio or image file in the HSB Hue channel of the image
			chan = PixelAudioMapper.ChannelNames.H;
			chooseFile();
			break;
		case 'v': // open an audio or image file in the HSB Saturation channel of the image
			chan = PixelAudioMapper.ChannelNames.S;
			chooseFile();
			break;
		case 'l': // open an audio or image file in the HSB Brightness channel of the image
			chan = PixelAudioMapper.ChannelNames.L;
			chooseFile();
			break;
		case 'c': // apply color from an image file to the display image
			chooseColorImage();
			break;
		case 'k': // apply the hue and saturation in the colors array to mapImage 
			mapImage.loadPixels();
			applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
			mapImage.updatePixels();
			break;
		case 'O': // reload the most recent image or audio file or show an Open File dialog
			if (isLoadFromImage) {
				if (imageFile == null) {
					chooseFile();
				} else {
					// reload image
					loadImageFile(imageFile);
					println("-------->>>>> Reloaded image file");
				}
			} 
			else {
				if (audioFile == null) {
					chooseFile();
				} else {
					// reload audio
					loadAudioFile(audioFile);
					println("-------->>>>> Reloaded audio file");
				}
			}
			break;
		case 'm': // remap the histogram of the image
			mapImage.loadPixels();
			int[] bounds = getHistoBounds(mapImage.pixels);
			mapImage.pixels = stretch(mapImage.pixels, bounds[0], bounds[1]);
			mapImage.updatePixels();
			break;
		case '=': // use a gamma function to make the image lighter
		case '+':
			setGamma(gammaLighter);
			mapImage.loadPixels();
			mapImage.pixels = adjustGamma(mapImage.pixels);
			mapImage.updatePixels();
			break;
		case '-': // use a gamma function to make the image darker
		case '_':
			setGamma(gammaDarker);
			mapImage.loadPixels();
			mapImage.pixels = adjustGamma(mapImage.pixels);
			mapImage.updatePixels();
			break;
		case 'S': // save the audio signal to an audio file
			saveToAudio(); 
			break;
		case 's': // save the image to a PNG file
			saveToImage();
			break;
		case 'f': // print the current frame rate to the console
			println("--->> frame rate: " + frameRate);
			break;
		case 'w': // transcode the image and write it to the audio signal
			writeImageToAudio();
			synth.setBuffer(audioSignal, sampleRate);
			println("--->> Wrote image to audio as audio data.");
			break;
		case 'W': // transcode the audio signal and write it to the image
			writeAudioToImage();
			println("--->> Wrote audio to image as audio data.");
			break;
		case '?': // show the Help Message in the console
			showHelp();
			break;
		default:
			break;
		}
	}
	
	public void showHelp() {
		println(" * Press ' ' to play audio for the point the mouse is currently over.");
		println(" * Press TAB to toggle animation.");
		println(" * Press 'o' to open an audio or image file in all RGB channels.");
		println(" * Press 'r' to open an audio or image file in the RED channel of the image.");
		println(" * Press 'g' to open an audio or image file in the GREEN channel of the image.");
		println(" * Press 'b' to open an audio or image file in the BLUE channel of the image.");
		println(" * Press 'h' to open an audio or image file in the HSB Hue channel of the image.");
		println(" * Press 'v' to open an audio or image file in the HSB Saturation channel of the image.");
		println(" * Press 'l' to open an audio or image file in the HSB Brightness channel of the image.");
		println(" * Press 'c' to apply color from an image file to the display image.");
		println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage.");
		println(" * Press 'O' to reload the most recent image or audio file or show an Open File dialog.");
		println(" * Press 'm' to remap the histogram of the image.");
		println(" * Press '=' to use a gamma function to make the image lighter.");
		println(" * Press '-' to use a gamma function to make the image darker.");
		println(" * Press 'S' to save the audio signal to an audio file.");
		println(" * Press 's' to save the image to a PNG file.");
		println(" * Press 'f' to print the current frame rate to the console.");
		println(" * Press 'w' to transcode the image and write it to the audio signal.");
		println(" * Press 'W' to transcode the audio signal and write it to the image.");
		println(" * Press '?' to show the Help Message in the console.");
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
	public int[] applyColor(int[] colorSource, int[] graySource, int[] lut) {
	  if (colorSource == null || graySource == null || lut == null) 
	    throw new IllegalArgumentException("colorSource, graySource and lut cannot be null.");
	  if (colorSource.length != graySource.length || colorSource.length != lut.length) 
	    throw new IllegalArgumentException("colorSource, graySource and lut must all have the same length.");
	  // initialize a reusable array for HSB color data -- this is a way to speed up the applyColor() method
	  float[] hsbPixel = new float[3];
	  for (int i = 0; i < graySource.length; i++) {
	    graySource[i] = PixelAudioMapper.applyColor(colorSource[lut[i]], graySource[i], hsbPixel);
	  }
	  return graySource;
	}

	public void mouseClicked() {
		audioMouseClick(mouseX, mouseY);
	}

	/**
	 * @param x    a value to constrain to the current window width
	 * @return the constrained value
	 */
	public int clipToWidth(int x) {
		return min(max(0, x), width - 1);
	}
	/**
	 * @param y    a value to constrain to the current window height
	 * @return the constrained value
	 */
	public int clipToHeight(int y) {
		return min(max(0, y), height - 1);
	}
	
	public int audioMouseClick(int x, int y) {
		sampleX = clipToWidth(x);
		sampleY = clipToHeight(y);
		samplePos = getSamplePos(x, y);
		int varyDuration = calcSampleLen(durationMS);
		return playSample(samplePos, varyDuration, 0.9f);
	}

	/**
	 * Calculates the index of the image pixel within the signal path,
	 * taking the shifting of pixels and audioSignal into account.
	 * See the MusicWindowBox sketch for use of a windowed buffer in this calculation. 
	 * 
	 * @param x    an x coordinate within mapImage and display bounds
	 * @param y    a y coordinate within mapImage and display bounds
	 * @return     the index of the sample corresponding to (x,y) on the signal path
	 */
	public int getSamplePos(int x, int y) {
	  int pos = mapper.lookupSignalPos(x, y);
	  // calculate how much animation has shifted the indices into the buffer
	  totalShift = (totalShift + shift % mapSize + mapSize) % mapSize;
	  return (pos + totalShift) % mapSize;
	}

	
	/**
	 * @return a length in samples with some Gaussian variation
	 */
	public int calcSampleLen(int durationMS) {
		float vary = 0; 
		// skip the fairly rare negative numbers
		while (vary <= 0) {
			vary = (float) gauss(1.0, 0.0625);
		}
		int len = (int)(abs((vary * durationMS) * sampleRate / 1000.0f));
		int actualLength = (int)((len / sampleRate) * 1000);
		println("---- calcSampleLen result = "+ len +" samples at "+ sampleRate +" Hz sample rate, "+ actualLength +" milliseconds");
		return len;
	}

	/**
	 * Returns a Gaussian variable using a Java library call to
	 * {@code Random.nextGaussian}.
	 * 
	 * @param mean
	 * @param variance
	 * @return a Gaussian-distributed random number with mean {@code mean} and
	 *         variance {@code variance}
	 */
	public double gauss(double mean, double variance) {
		return rando.nextGaussian() * Math.sqrt(variance) + mean;
	}

	/**
	 * Plays an audio sample with PASamplerInstrument and default ADSR.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param sampleCount      number of samples to play
	 * @param amplitude    amplitude of the samples on playback
	 * @return the calculated sample length in samples
	 */
	public int playSample(int samplePos, int sampleCount, float amplitude) {
		sampleCount = synth.playSample(samplePos, sampleCount, amplitude);
		int durationMS = (int)(sampleCount/sampleRate * 1000);
		println("----- audio event duration = "+ durationMS +" millisconds");
		timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
		// return the length of the sample
		return sampleCount;
	}

	
	public void writeImageToAudio() {
		println("----- writing image to signal ");
		mapImage.loadPixels();
		audioSignal = new float[mapSize];
		mapper.mapImgToSig(mapImage.pixels, audioSignal);
		playBuffer.setBufferSize(mapSize);
		playBuffer.setChannel(0, audioSignal);
		if (playBuffer != null) println("--->> audioBuffer length channel 0 = "+ playBuffer.getChannel(0).length);
	}

	
	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                       UTILITY METHODS                          */
	/*                                                                */
	/*----------------------------------------------------------------*/

	// ------------- HISTOGRAM AND GAMMA ADJUSTMENTS ------------- // 
		
	public int[] getHistoBounds(int[] source) {
	    int min = 255;
	    int max = 0;
	    for (int i = 0; i < source.length; i++) {
	      int[] comp = PixelAudioMapper.rgbComponents(source[i]);
	      for (int j = 0; j < comp.length; j++) {
	        if (comp[j] > max) max = comp[j];
	        if (comp[j] < min) min = comp[j];
	      }
	    }
	    println("--- min", min, " max ", max);
	    return new int[]{min, max};
	}

	// histogram stretch -- run getHistoBounds to determine low and high
	public int[] stretch(int[] source, int low, int high) {
	  int[] out = new int[source.length];
	  int r = 0, g = 0, b = 0;
	  for (int i = 0; i < out.length; i++) {
	    int[] comp = PixelAudioMapper.rgbComponents(source[i]);
	    r = comp[0];
	    g = comp[1];
	    b = comp[2];
	    r = (int) constrain(map(r, low, high, 1, 254), 0, 255);
	    g = (int) constrain(map(g, low, high, 1, 254), 0, 255);
	    b = (int) constrain(map(b, low, high, 1, 254), 0, 255);
	    out[i] = PixelAudioMapper.composeColor(r, g, b, 255);
	  }
	  return out;
	}

	public void setGamma(float gamma) {
	  if (gamma != 1.0) {
	    this.gammaTable = new int[256];
	    for (int i = 0; i < gammaTable.length; i++) {
	      float c = i/(float)(gammaTable.length - 1);
	      gammaTable[i] = (int) Math.round(Math.pow(c, gamma) * (gammaTable.length - 1));
	    }
	  }
	}

	public int[] adjustGamma(int[] source) {
	  int[] out = new int[source.length];
	  int r = 0, g = 0, b = 0;
	  for (int i = 0; i < out.length; i++) {
	    int[] comp = PixelAudioMapper.rgbComponents(source[i]);
	    r = comp[0];
	    g = comp[1];
	    b = comp[2];
	    r = gammaTable[r];
	    g = gammaTable[g];
	    b = gammaTable[b];
	    out[i] = PixelAudioMapper.composeColor(r, g, b, 255);
	  }
	  return out;
	}	
	
	
	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                   BEGIN FILE I/O METHODS                       */
	/*                                                                */
	/*----------------------------------------------------------------*/

	// -------- BEGIN FILE I/O FOR APPLYING COLOR --------- //

	/* 
	 * Here is a special section of code for TutorialOne and other applications that
	 * color a grayscale image with color data from a file. The color and saturation
	 * come from the selected file, the brightness (gray values, more or less) come
	 * from an image you supply, such as display image. 
	 */


	/**
	 * Call to initiate process of opening an image file to get its color data.
	 */
	public void chooseColorImage() {
	  selectInput("Choose an image file to apply color: ", "colorFileSelected");
	}

	/**
	 * callback method for chooseColorImage() 
	 * @param selectedFile    the File the user selected
	 */
	public void colorFileSelected(File selectedFile) {
	  if (null != selectedFile) {
	    String filePath = selectedFile.getAbsolutePath();
	    String fileName = selectedFile.getName();
	    String fileTag = fileName.substring(fileName.lastIndexOf('.') + 1);
	    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
	    if (fileTag.equalsIgnoreCase("png") || fileTag.equalsIgnoreCase("jpg") || fileTag.equalsIgnoreCase("jpeg")) {
	      imageFile = selectedFile;
	      imageFilePath = filePath;
	      imageFileName = fileName;
	      imageFileTag = fileTag;
	      println("--- Selected color file "+ fileName +"."+ fileTag);
	      // apply the color data (hue, saturation) in the selected image to our display image, mapImage
	      applyImageColor(imageFile, mapImage);
	    } 
	    else {
	      println("----- File is not a recognized image format ending with \"png\", \"jpg\", or \"jpeg\".");
	    }
	  } 
	  else {
	    println("----- No file was selected.");
	  }
	}

	/**
	 * Apply the hue and saturation of a chosen image file to the brightness channel of the display image.
	 * @param imgFile        selected image file, source of hue and saturation values
	 * @param targetImage    target image where brightness will remain unchanged
	 */
	public void applyImageColor(File imgFile, PImage targetImage) {
	  PImage colorImage = loadImage(imgFile.getAbsolutePath());
	  int w = colorImage.width > mapImage.width ? targetImage.width : colorImage.width;
	  int h = colorImage.height > targetImage.height ? targetImage.height : colorImage.height;
	  float[] hsbPixel = new float[3];
	  colorImage.loadPixels();
	  int[] colorSource = colorImage.pixels;
	  targetImage.loadPixels();
	  int[] graySource = targetImage.pixels;
	  for (int y = 0; y < h; y++) {
	    int rowStart = y * w;
	    for (int x = 0; x < w; x++) {
	      int cPos = rowStart + x;
	      int gPos = y * targetImage.width + x;
	      graySource[gPos] = PixelAudioMapper.applyColor(colorSource[cPos], graySource[gPos], hsbPixel);
	    }
	  }
	  targetImage.updatePixels();
	}

	// -------- END FILE I/O FOR APPLYING COLOR --------- //
	  
	/*
	 * Here is a section of "regular" file i/o methods for audio and image files.
	 */


	/**
	 * Wrapper method for Processing's selectInput command
	 */
	public void chooseFile() {
	  oldIsAnimating = isAnimating;
	  isAnimating = false;
	  selectInput("Choose an audio file or an image file: ", "fileSelected");
	}

	/**
	 * callback method for chooseFile(), handles standard audio and image formats for Processing.
	 * If a file has been successfully selected, continues with a call to loadAudioFile() or loadImageFile().
	 * 
	 * @param selectedFile    the File the user selected
	 */
	public void fileSelected(File selectedFile) {
	  if (null != selectedFile) {
	    String filePath = selectedFile.getAbsolutePath();
	    String fileName = selectedFile.getName();
	    String fileTag = fileName.substring(fileName.lastIndexOf('.') + 1);
	    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
	    if (fileTag.equalsIgnoreCase("mp3") || fileTag.equalsIgnoreCase("wav") || fileTag.equalsIgnoreCase("aif")
	        || fileTag.equalsIgnoreCase("aiff")) {
	      // we chose an audio file
	      audioFile = selectedFile;
	      audioFilePath = filePath;
	      audioFileName = fileName;
	      audioFileTag = fileTag;
	      println("----- Selected file " + fileName + "." + fileTag + " at "
	          + filePath.substring(0, filePath.length() - fileName.length()));
	      // if (nd != null) nd.oscSendFileInfo(filePath, fileName, fileTag);
	      loadAudioFile(audioFile);
	    } 
	    else if (fileTag.equalsIgnoreCase("png") || fileTag.equalsIgnoreCase("jpg")
	        || fileTag.equalsIgnoreCase("jpeg")) {
	      // we chose an image file
	      imageFile = selectedFile;
	      imageFilePath = filePath;
	      imageFileName = fileName;
	      imageFileTag = fileTag;
	      loadImageFile(imageFile);
	    } 
	    else {
	      println("----- File is not a recognized audio format ending with \"mp3\", \"wav\", \"aif\", or \"aiff\".");
	    }
	  } 
	  else {
	    println("----- No audio or image file was selected.");
	  }
	  isAnimating = oldIsAnimating;
	}

	/**
	 * Attempts to load audio data from a selected file into playBuffer, then calls
	 * writeAudioToImage() to transcode audio data and write it to mapImage
	 * 
	 * @param audioFile    an audio file
	 */
	public void loadAudioFile(File audioFile) {
		// read audio file into our MultiChannelBuffer
		float fileSampleRate = minim.loadFileIntoBuffer(audioFile.getAbsolutePath(), playBuffer);
		// sampleRate > 0 means we read audio from the file
		if (fileSampleRate > 0) {
			// resize the buffer to mapSize, if necessary -- signal will not be overwritten
			if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
			// read a copy of channel 0 of the buffer into audioSignal, truncated or padded to fit mapSize
			audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
			synth.setBuffer(audioSignal, fileSampleRate);
			audioLength = audioSignal.length;
			writeAudioToImage(audioSignal, mapper, mapImage, chan);
			totalShift = 0;    // reset animation shift
		}
	}

	
	/**
	 * Convenience method to call writeAudioToImage(float[], PixelAudioMapper, PImage, PixelAudioMapper.ChannelNames)
	 * with audioSignal, mapper, and mapImage and ChannelNames.L as arguments.
	 */
	public void writeAudioToImage() {
		writeAudioToImage(audioSignal, mapper, mapImage, ChannelNames.L);
	}
	
	/**
	 * Transcodes audio data in sig[] and writes it to color channel chan of mapImage 
	 * using the lookup tables in mapper to redirect indexing. Calls mapper.mapSigToImg(), 
	 * which will throw an IllegalArgumentException if sig.length != img.pixels.length
	 * or sig.length != mapper.getSize(). 
	 * 
	 * @param sig         an source array of float, should be audio data in the range [-1.0, 1.0]
	 * @param mapper      a PixelAudioMapper
	 * @param img         a target PImage, modified by audio data in sig
	 * @param chan        a color channel
	 */
	public void writeAudioToImage(float[] sig, PixelAudioMapper mapper, PImage img, PixelAudioMapper.ChannelNames chan) {
	  // If sig.length == mapper.getSize() == mapImage.width * mapImage.height, we can call safely mapper.mapSigToImg()  
	  img.loadPixels();
	  mapper.mapSigToImg(sig, img.pixels, chan);
	  img.updatePixels();
	}

	/**
	 * Attempts to load image data from a selected file into mapImage, then calls writeImageToAudio() 
	 * to transcode HSB brightness color data from the image to audio and writes it to audioBuffer and audioSignal.
	 * 
	 * @param imgFile    an image file
	 */
	public void loadImageFile(File imgFile) {
	  PImage img = loadImage(imgFile.getAbsolutePath());
	  // stash information about the image in imgFileWidth, imageFileHeight for future use
	  imageFileWidth = img.width;
	  imageFileHeight = img.height;
	  // calculate w and h for copying image to display (mapImage)
	  int w = img.width > mapImage.width ? mapImage.width : img.width;
	  int h = img.height > mapImage.height ? mapImage.height : img.height;
	  if (chan == PixelAudioMapper.ChannelNames.ALL) {
	    // copy the image directly using Processing copy command
	    mapImage.copy(img, 0, 0, w, h, 0, 0, w, h);
	  } 
	  else {
	    // copy only specified channels of the new image
	    PImage mixImage = createImage(w, h, RGB);
	    mixImage.copy(mapImage, 0, 0, w, h, 0, 0, w, h);
	    img.loadPixels();
	    mixImage.loadPixels();
	    mixImage.pixels = PixelAudioMapper.pushChannelToPixel(img.pixels, mixImage.pixels, chan);
	    mixImage.updatePixels();
	    mapImage.copy(mixImage, 0, 0, w, h, 0, 0, w, h);
	  }
	  // prepare to copy image data to audio variables
	  // resize the buffer to mapSize, if necessary -- signal will not be overwritten
	  if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
	  audioSignal = playBuffer.getChannel(0);
	  writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
	  // now that the image data has been written to audioSignal, set audioBuffer channel 0 to the new audio data
	  playBuffer.setChannel(0, audioSignal);
	  audioLength = audioSignal.length;
	  // pass the updated signal to our sampling synth 
	  synth.setBuffer(audioSignal, sampleRate);
	  totalShift = 0;    // reset animation shift
	}

	/**
	 * This method writes a color channel from the an image to an audio signal, fulfilling a 
	 * central concept of the PixelAudio library: image is sound. Calls mapper.mapImgToSig(), 
	 * which will throw an IllegalArgumentException if img.pixels.length != sig.length or 
	 * img.width * img.height != mapper.getWidth() * mapper.getHeight(). 
	 * 
	 * @param img       a PImage, a source of data
	 * @param mapper    a PixelAudioMapper, handles mapping between image and audio signal
	 * @param sig       an target array of float in audio format, rewritten by this method
	 * @param chan      a color channel
	 */
	public void writeImageToAudio(PImage img, PixelAudioMapper mapper, float[] sig, PixelAudioMapper.ChannelNames chan) {
	  sig = mapper.mapImgToSig(img.pixels, sig, chan);
	}    


	public void saveToAudio() {
	  // File folderToStartFrom = new File(dataPath("") + "/");
	  // selectOutput("Select an audio file to write to:", "audioFileSelectedWrite", folderToStartFrom);
	  selectOutput("Select an audio file to write to:", "audioFileSelectedWrite");
	}

	public void audioFileSelectedWrite(File selection) {
	  if (selection == null) {
	    println("Window was closed or the user hit cancel.");
	    return;
	  }
	  String fileName = selection.getAbsolutePath();
	  if (selection.getName().indexOf(".wav") != selection.getName().length() - 4) {
	    fileName += ".wav";
	  }
	  try {
	    saveAudioToFile(audioSignal, sampleRate, fileName);
	  } catch (IOException e) {
	    println("--->> There was an error outputting the audio file " + fileName +", "  + e.getMessage());
	  } catch (UnsupportedAudioFileException e) {
	    println("--->> The file format is unsupported " + e.getMessage());
	  }
	}

	/**
	 * Saves audio data to 16-bit integer PCM format, which Processing can also open.
	 * This same method can be called as a static method in PixelAudio.
	 * 
	 * @param samples       an array of floats in the audio range (-1.0f, 1.0f)
	 * @param sampleRate    audio sample rate for the file
	 * @param fileName      name of the file to save to
	 * @throws IOException  an Exception you'll need to handle to call this method (see keyPressed entry for 's')
	 * @throws UnsupportedAudioFileException    another Exception (see keyPressed entry for 's')
	 */
	public void saveAudioToFile(float[] samples, float sampleRate, String fileName)
	        throws IOException, UnsupportedAudioFileException {
	  // Convert samples from float to 16-bit PCM
	  byte[] audioBytes = new byte[samples.length * 2];
	  int index = 0;
	  for (float sample : samples) {
	    // Scale sample to 16-bit signed integer
	    int intSample = (int) (sample * 32767);
	    // Convert to bytes
	    audioBytes[index++] = (byte) (intSample & 0xFF);
	    audioBytes[index++] = (byte) ((intSample >> 8) & 0xFF);
	  }
	  // Create an AudioInputStream
	  ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
	  AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
	  AudioInputStream audioInputStream = new AudioInputStream(byteStream, format, samples.length);
	  // Save the AudioInputStream to a WAV file
	  File outFile = new File(fileName);
	  AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outFile);
	}

	public void saveToImage() {
	  // File folderToStartFrom = new File(dataPath(""));
	  selectOutput("Select an image file to write to:", "imageFileSelectedWrite");
	}

	public void imageFileSelectedWrite(File selection) {
	  if (selection == null) {
	    println("Window was closed or the user hit cancel.");
	    return;
	  }
	  String fileName = selection.getAbsolutePath();
	  if (selection.getName().indexOf(".png") != selection.getName().length() - 4) {
	    fileName += ".png";
	  }
	  saveImageToFile(mapImage, fileName);
	}

	public void saveImageToFile(PImage img, String fileName) {
	  img.save(fileName);
	}	
	
}
