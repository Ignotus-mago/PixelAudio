/**
 * 
 */
package net.paulhertz.pixelaudio.example;

import processing.core.*;

//File IO support from Java
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

//Audio support from Java
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

//Mama's ever-lovin' blue-eyed PixelAudio library
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.sampler.*;
import net.paulhertz.pixelaudio.schedule.AudioUtility;
import net.paulhertz.pixelaudio.schedule.TimedLocation;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;

//audio library
import ddf.minim.*;


/*
 * If you are just starting to work with PixelAudio coding, I suggest looking through these
 * sketches before doing the TutorialOne sequence:
 * 
 *   LookupTables: an introduction to a core concept in PixelAudio, lookup tables. Start here.
 *   Starter: basics of creating a PixelMapGen instance and plugging it into a PixelAudioMapper.
 *   SimpleAnimation: a simple way to animate a bitmap using PixelAudioMapper.
 *   MultiGenDemo: chain PixelMapGens together to generate a large image.
 *   MultiGenLookupTables: lookup tables in MultiGens, a useful place to test your MultiGenCode. 
 *   TransformPimage (optional): introduces the affine transforms available in the BitmapTransform class.
 * 
 * The opening sequence, above, shows the basics of loading the PixelAudio library and 
 * using it to display a rainbow array of colors along the signal path. It also provides
 * a close look at how to create MultiGen objects, which can use multiple PixelMapGen 
 * objects to create a composite PixelMapGen class. You don't need to create your own
 * custom MultiGen classes. The PixelMapGen subclasses HilbertGen, DiagonalZigzagGen, 
 * and BoustropheGen include various static methods to generate MultiGen objects.
 * 
 * TutorialOne_01_FileIO can open and display audio and image files, transcode RGB pixel 
 * data to audio samples and transcode audio samples to RGB pixel data. It can also save audio
 * and image files. It responds to mouse clicks by playing the audio samples corresponding 
 * to the click location in the display image. To help you visualize the signal path, the 
 * 'k' command key generates a rainbow color array that follows the signal path but keeps
 * the brightness information in the image intact. The image also appears when you launch
 * the sketch.
 *
 *   1. Launch the sketch and then press the 'o' key to open a image or an audio file.
 *      The "Saucer_mixdown.wav" and "snowfence.jpg" files are good for experimenting.
 *      To see the signal path as a color overlay, press the 'k' key. 
 *      
 *   2. Saucer_mixdown.wav is loaded into an audio buffer, playBuffer, and then
 *      transcoded into the PImage mapImage, which is displayed on screen. The audio 
 *      samples are floating point values from -1.0 to +1.0. They are transcoded to RGB
 *      color values in the range (0, 255). The samples follow a path over the image
 *      that visits every pixel, the signal path. In this sketch, the signal path consists
 *      of 6 connected Hilbert curves. The Hilbert curve is a 2D fractal curve which is
 *      often used in scientific visualization to reveal hidden patterns in 1D data. 
 *      In the image created from Saucer_mixdown, high frequency sounds create fine-grained
 *      patterns and low frequency sounds create coarse-grained patterns. 
 *      
 *   3. Snowfence.jpg is loaded into mapImage and then transcoded from to the playBuffer and 
 *      audioSignal variables. If you click in the image, you can hear the sound created 
 *      by reading the brightness levels of pixels along the signal path and changing 
 *      them into audio sample data. The sky, with very little variation in texture, is
 *      relatively quiet. The fence and other areas are noisy. Most images result in
 *      different sorts of noise; however, if you save the image created by loading an 
 *      audio file, it will transcode back into audio. You could send messages this way.
 *      As long as sender and receiver use the same signal path to encode audio, they can
 *      communicate with pictures. Try loading "saucer_image.png" to hear how this works. 
 *      Image data doesn't have as good a resolution as audio data, so there is some 
 *      loss of quality in saving audio as an image and then reloading it as audio. 
 *      
 *      Note: If you press 'k' to load the color overlay, you will not affect audio 
 *      quality. Loading the hue and saturation channels of an image has very little
 *      effect on the brightness levels, which are the source of the audio signal. 
 *      
 *   4. Transcoding is automated in this tutorial, but we'll provide separate loading
 *      of audio and image in later tutorials. 
 *      
 *   5. Experiment with different image and audio files. 
 *      Press 'c' to load only color data (hue and saturation) from an image file. 
 *      Click in the image or hover and press the spacebar to play an audio sample.
 *      Press 'r' to toggle selection of a random ADSR envelope for the audio sample. 
 *
 * Audio events are generated through PASamplerInstrument. PASamplerInstrument lets
 * us add an ADSR (attack, decay, sustain, release) envelope to audio output from 
 * audio samples. It can also shift pitch and set stereo panning. It can support 
 * multiple voices -- in the initAudio() method we set the number to 8. This is a
 * reasonable number of voices for tracking rapid mouse clicks, but you can change it. 
 *
 * In this sketch, audio events are triggered with one of two methods:
 * 
 *  int actualSampleCount = playSample(int samplePos, int sampleCount, float amplitude)
 *  int actualSampleCount = playSample(int samplePos, int sampleCount, float amplitude, ADSRParams env)
 *
 * The first method uses the built-in ADSR supplied on initializing PASamplerInstrument.
 * The second method allows you to supply your own ADSR. Press the 'r' key to have this
 * sketch trigger sounds with a randomly selected envelope from adsrList. 
 * 
 * PASamplerInstrument and the other audio instruments in net.paulhertz.pixelaudio.sampler
 * play an audio event for the requested duration (samplelen) using the attack, decay,
 * and sustain portion of the envelope. When the duration ends, the release portion of the 
 * envelope controls how the audio fades away. Calls to the instruments playSample() methods
 * return the amount of time the envelope will actually take, which is greater than or equal
 * to the requested duration. 
 *
 * Still to come, as the tutorial advances:
 * -- animation and saving to video
 * -- setting pitch and panning with playSample()
 * -- drawing to trigger audio events
 * -- UDP communication with Max and other media applications
 * -- loading a file to memory and traversing it with a windowed buffer
 * 
 * See also: example sketch LoadImageToAudio, with a complete set of commands for loading
 * images and audio to different color channels. 
 * 
 * KEY COMMANDS 
 * 
 * Press 'c' to apply color from image file to display image.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage.
 * Press 'o' or 'O' to open an audio or image file.
 * Press 'r' or 'R' to use the default envelope or a random envelope from a list.
 * Press 'h' or 'H' to show help and key commands in console.
 * 
 * 
 */
 public class TutorialOne_01_FileIO extends PApplet {
	 
	 /* ------------------------------------------------------------------ */
	 /*                       PIXELAUDIO VARIABLES                         */
	 /* ------------------------------------------------------------------ */

	 PixelAudio pixelaudio;     // our shiny new library
	 MultiGen multigen;         // a PixelMapGen that links together multiple PixelMapGens
	 int genWidth = 512;        // width of multigen PixelMapGens
	 int genHeight = 512;       // height of  multigen PixelMapGens
	 PixelAudioMapper mapper;   // object for reading, writing, and transcoding audio and image data
	 int mapSize;               // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
	 PImage mapImage;           // image for display
	 PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;    // color channel that receives pixel data
	 int[] spectrum;            // array of spectral colors

	 /* ------------------------------------------------------------------ */
	 /*                        FILE I/O VARIABLES                          */
	 /* ------------------------------------------------------------------ */

	 // audio file
	 File audioFile;
	 String audioFilePath;
	 String audioFileName;
	 String audioFileTag;
	 int audioFileLength;
	 // image file
	 File imageFile;
	 String imageFilePath;
	 String imageFileName;
	 String imageFileTag;
	 int imageFileWidth;
	 int imageFileHeight;
	 
	 
	 /* ------------------------------------------------------------------ */
	 /*                          AUDIO VARIABLES                           */
	 /* ------------------------------------------------------------------ */

	 /*
	  * Audio playback support is added with the audio variables and audio methods.
	  * Minim audioOut and other globals are initialized in initAudio().
	  */

	 /** Minim audio library */
	 Minim minim;                    // library that handles audio 
	 AudioOutput audioOut;           // output to sound hardware
	 float audioGain = 0.0f;         // audio gain in dB

	 float sampleRate = 44100;       // target audio sampling rate used to configure audioOut
	 float fileSampleRate;           // sample rate of most recently opened file (before resampling, but may already == sampleRate)
	 float bufferSampleRate;         // sample rate of playBuffer, == fileSampleRate when we don't resample, == sampleRate when we do
	 boolean doResample = true;      // if true, resample audio from files whose sampling rate != audioOut.sampleRate()

	 float[] audioSignal;            // the audio signal as an array of floats
	 MultiChannelBuffer playBuffer;  // a buffer for playing the audio signal
	 int audioLength;                // length of the audioSignal, same as the number of pixels in the display image
	 
	 // SamplerInstrument setup
	 int noteDuration = 1500;        // average sample synth note duration, milliseconds
	 PASamplerInstrument synth;      // instance of class that wraps a Minim Sampler and implements an ADSR envelope
	 float samplerGain = -3.0f;      // synth gain in dB
	 
	 // ADSR and its parameters
	 // Typically, we want to set ADSR maxAmplitude to 1.0 and then change sampleGain 
	 // or the gain value passed to playSample() to make a sample louder or softer when it plays
	 ADSRParams defaultEnv;          // ADSRParams is a wrapper for Minim's ADSR that keeps its values visible
	 float maxAmplitude = 1.0f;      // 0..1 linear amplitude
	 float attackTime = 0.1f;        // seconds
	 float decayTime = 0.3f;         // seconds
	 float sustainLevel = 0.5f;      // 0..1
	 float releaseTime = 0.1f;       // seconds
	 ArrayList<ADSRParams> adsrList; // list of ADSR values
	 boolean isRandomADSR = false;   // choose a random envelope from adsrList, or not

	 // interaction variables for audio
	 ArrayList<TimedLocation> timeLocsArray;
	 int count = 0;  
	 int fileIndex = 0;

	 /* ---------------- end audio variables ---------------- */
	 

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(new String[] { TutorialOne_01_FileIO.class.getName() });
	}
	
	public void settings() {
		size(3 * genWidth, 2 * genHeight);
	}

	public void setup() {
		frameRate(24);
		// 1. initialize PixelAudio
		pixelaudio = new PixelAudio(this);         
		// 2. create a PixelMapGen object
		multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);    
		// 3. initialize a PixelAudioMapper object with the gen
		mapper = new PixelAudioMapper(multigen);
		// keep track of the area of the PixelAudioMapper
		mapSize = mapper.getSize();
		// create an array of rainbow colors with mapSize elements
		spectrum = getColors(mapSize);
		// 4. create an image for display
		initImages();
		// 5. set up the audio environment and variables
		initAudio();
		// 6. show key commands in the console
		showHelp();
	}

	// turn off audio processing when we exit
	@Override
	public void stop() {
		if (synth != null) synth.close();
		if (minim != null) minim.stop();
		super.stop();
	}

	/**
	 * Generates an array of rainbow colors using the HSB color space.
	 * @param size    the number of entries in the colors array
	 * @return an array of RGB colors ordered by hue
	 */
	public int[] getColors(int size) {
		int[] colorWheel = new int[size]; // an array for our colors
		pushStyle(); // save styles
		colorMode(HSB, colorWheel.length, 100, 100); // pop over to the HSB color space and give hue a very wide range
		for (int i = 0; i < colorWheel.length; i++) {
			colorWheel[i] = color(i, 50, 70); // fill our array with colors, gradually changing hue
		}
		popStyle(); // restore styles, including the default RGB color space
		return colorWheel;
	}

	/**
	 * Initialize mapImage with the colors array. MapImage will handle the color data for mapper
	 * and also serve as our display image.
	 */
	public void initImages() {
		mapImage = createImage(width, height, ARGB);
		mapImage.loadPixels();
		mapper.plantPixels(spectrum, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
		mapImage.updatePixels();
	}

	public void draw() {
		image(mapImage, 0, 0);
		runTimeArray();    // animate audio event markers
	}

	/**
	 * The built-in mousePressed handler for Processing, not used yet...
	 */
	public void mousePressed() {
		// println("mousePressed:", mouseX, mouseY);
	}
	
	public void mouseClicked() {
		// handle audio generation in response to a mouse click
		audioMouseClick(clipToWidth(mouseX), clipToHeight(mouseY));
	}

	/**
	 * built-in keyPressed handler, forwards events to parseKey
	 */
	public void keyPressed() {
		parseKey(key, keyCode);    
	}

	/**
	 * 
	 * Handles key press events passed on by the built-in keyPressed method. 
	 * By moving key event handling outside the built-in keyPressed method, 
	 * we make it possible to post key commands without an actual key event.
	 * Methods and interfaces and even other threads can call parseKey(). 
	 * This opens up many possibilities and a some risks, too.  
	 * 
	 * @param key
	 * @param keyCode
	 * 
	 */
	public void parseKey(char key, int keyCode) {
		switch(key) {
		case ' ': // spacebar, play sample at current mouse position
			audioMouseClick(clipToWidth(mouseX), clipToHeight(mouseY));
			break;
		case 'c': // apply color from image file to display image
			chooseColorImage();
			break;
		case 'k': // apply the hue and saturation in the colors array to mapImage 
			mapImage.loadPixels();
			applyColor(spectrum, mapImage.pixels, mapper.getImageToSignalLUT());
			mapImage.updatePixels();
			break;
		case 'o': case 'O': // open an audio or image file
			chooseFile();
			break;
		case 'r': case 'R': // set isRandomADSR, to use the default envelope or a random envelope from a list.
			isRandomADSR = !isRandomADSR;
			String msg = isRandomADSR ? " synth uses a random ADSR" : " synth uses default ADSR";
			println("---- isRandomADSR = "+ isRandomADSR +","+ msg);
			break;
		case 'd': // toggle doResample: if true, resample audio when fileSampleRate != audioOut.sampleRate()
			doResample = !doResample;
			println("---- doResample = "+ doResample);
			break;
		case 'h': case 'H': // show help and key commands in console
			showHelp();
			break;
		default:
			break;
		}
	}

	/**
	 * To generate help output, run RegEx search/replace on parseKey case lines with:
	 * find:    case ('.'): // (.+)
	 * replace: println(" * Press $1 to $2.");
	 */
	public void showHelp() {
		println(" * Press ' ' (spacebar) to play sample at current mouse position.");
		println(" * Press 'c' to apply color from image file to display image.");
		println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage.");
		println(" * Press 'o' or 'O' to open an audio or image file.");
		println(" * Press 'r' or 'R' to use the default envelope or a random envelope from a list.");
		println(" * Press 'd' to toggle doResample: if true, resample audio when fileSampleRate != audioOut.sampleRate()");
		println(" * Press 'h' or 'H' to show help and key commands in console.");
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


	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                     FILE I/O METHODS                           */
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
	}
	
	/**
	 * Attempts to load audio data from a selected file into playBuffer, then calls
	 * writeAudioToImage() to transcode audio data and write it to mapImage.
	 * Resamples files that are recorded with a different sample rate than the current audio output.
	 * If you want to load the image file and audio file separately, comment out writeAudioToImage(). 
	 * 
	 * @param audFile    an audio file
	 */
	public void loadAudioFile(File audFile) {
		MultiChannelBuffer buff = new MultiChannelBuffer(1024, 1);
		fileSampleRate =  minim.loadFileIntoBuffer(audFile.getAbsolutePath(), buff);
		if (fileSampleRate <= 0) {
			println("-- Unable to load file. File may be empty, wrong format, or damaged.");
			return;
		}
		float sig[];
		// load audio to sig, resampling it if required
		if (fileSampleRate != audioOut.sampleRate() && doResample) {
			sig = AudioUtility.resampleMonoToOutput(buff.getChannel(0), fileSampleRate, audioOut);
			bufferSampleRate = sampleRate;
		}
		else {
	        sig = Arrays.copyOf(buff.getChannel(0), buff.getBufferSize());
	        bufferSampleRate = fileSampleRate;
		}		
		this.audioFileLength = sig.length;
		println("---- file sample rate = "+ this.fileSampleRate 
				+", buffer sample rate = "+ bufferSampleRate
				+", audio output sample rate = "+ audioOut.sampleRate());
		// make sure the synth is ready (a precaution)
		ensureSamplerReady();
		// update the audio variables that depend on a newly loaded audio file
		updateAudioChain(sig);
		// write the signal to mapImage
		// we do it automatically here, but that will change in later examples
		writeAudioToImage(audioSignal, mapper, mapImage, chan);
		//if (applyColorMapOnLoad) applyColorMapToDisplay(true);
	}

	
	/**
	 * Transcodes audio data in sig[] and writes it to color channel chan of mapImage 
	 * using the lookup tables in mapper to redirect indexing. Calls mapper.mapSigToImg(), 
	 * which will throw an IllegalArgumentException if sig.length != img.pixels.length
	 * or sig.length != mapper.getSize(). We typically use PixelAudioMapper.ChannelNames.ALL 
	 * or PixelAudioMapper.ChannelNames.L as the chan value. Both result in gray values,
	 * with PixelAudioMapper.ChannelNames.L maintaining previous hue and saturation color
	 * values in the image. 
	 * 
	 * @param sig         an array of float, should be audio data in the range [-1.0, 1.0]
	 * @param mapper      a PixelAudioMapper
	 * @param img         a PImage
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
	 * to transcode HSB brightness color data from the image to audio and writes it to playBuffer and audioSignal.
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
		// initialize a new buffer for audioSignal
		float[] sig = new float[mapper.getSize()];
		audioSignal = sig;
		// render the HSB brightness channel (ChannelNames.L) to the audio signal as transcoded samples
		renderMapImageToAudio(PixelAudioMapper.ChannelNames.L);
		// update all audio data structures that depend on audioSignal
		updateAudioChain(sig);
	}

	/**
	 * This method writes a color channel from an image to playBuffer, fulfilling a 
	 * central concept of the PixelAudio library: image is sound. Calls mapper.mapImgToSig(), 
	 * which will throw an IllegalArgumentException if img.pixels.length != sig.length or 
	 * img.width * img.height != mapper.getWidth() * mapper.getHeight(). 
	 * Sets totalShift = 0 on completion: the image and audio are now in sync. TODO
	 * 
	 * @param img       a PImage, a source of data
	 * @param mapper    a PixelAudioMapper, handles mapping between image and audio signal
	 * @param sig       an target array of float in audio format 
	 * @param chan      a color channel
	 */
	public void writeImageToAudio(PImage img, PixelAudioMapper mapper, float[] sig, PixelAudioMapper.ChannelNames chan) {
		// If img is the *display* (shifted) image, commit its phase into audio:
		sig = mapper.mapImgToSig(img.pixels, sig, chan);
	}
	
	/**
	 * Writes a specified channel of mapImage to audioSignal.
	 * 
	 * @param chan    the selected color channel
	 */
	public void renderMapImageToAudio(PixelAudioMapper.ChannelNames chan) {
		writeImageToAudio(mapImage, mapper, audioSignal, chan);
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
	 * @throws IOException  an Exception you'll need to handle to call this method 
	 * @throws UnsupportedAudioFileException    another Exception 
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


	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                        AUDIO METHODS                           */
	/*                                                                */
	/*----------------------------------------------------------------*/

	/**
	 * CALL THIS METHOD IN SETUP()
	 * Initializes Minim audio library and audio variables.
	 */
	public void initAudio() {
		minim = new Minim(this);
		// Use the getLineOut method of the Minim object to get an AudioOutput object.
		// PixelAudio instruments require a STEREO output. 1024 is a standard number
		// of samples for the output buffer to process at one time. You should usually
		// set the output sampleRate to either 41500 or 48000, standards for digital 
		// audio recordings.
		this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
		// set the output level in dB
		audioOut.setGain(audioGain);
		// create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
		// playBuffer will not contain audio data until we load a file
		this.playBuffer = new MultiChannelBuffer(mapSize, 1);
		this.audioSignal = playBuffer.getChannel(0);
		this.audioLength = audioSignal.length;
		// ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
		defaultEnv = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
		initADSRList();
		// create a PASamplerInstrument
		ensureSamplerReady();
		// initialize mouse event tracking array
		timeLocsArray = new ArrayList<TimedLocation>();
	}
	
	/**
	 * Prepares Sampler instruments and assets
	 */
	void ensureSamplerReady() {
		if (synth == null) { 
	    	synth = new PASamplerInstrument(playBuffer, audioOut.sampleRate(), 8, audioOut, defaultEnv); 
	    	println("-- initilialized audio sampler synth");
	    	// set the synth gain with a linear value derived from a dB value
	    	synth.setGain(AudioUtility.dbToLinear(samplerGain));
	    }
	}

	// a list of ADSR envelopes. Feel free to edit and add more.
	public void initADSRList() {
		adsrList = new ArrayList<ADSRParams>();
		ADSRParams env0 = new ADSRParams(
				0.7f,   // maxAmp
				0.05f,  // fast attack (s)
				0.1f,   // fast decay (s)
				0.35f,  // sustain level
				0.5f    // slow release (s)
				);
		ADSRParams env1 = new ADSRParams(
				0.7f,   // maxAmp
				0.4f,   // slow attack (s)
				0.0f,   // no decay (s)
				0.7f,   // sustain level
				0.4f    // slow release (s)
				);
		ADSRParams env2 = new ADSRParams(
				0.7f,   // maxAmp
				0.75f,  // slow attack (s)
				0.1f,   // decay (s)
				0.6f,  // sustain level
				0.05f    // release (s)
				);
		adsrList.add(env0);
		adsrList.add(env1);
		adsrList.add(env2);
	}

	/**
	 * Bottleneck "commit" method for audio state. 
     * 
     * Takes an arbitrary input signal and installs it as the canonical audio signal
     * used by the system. This method:
     *
     *  - Resizes/pads/truncates the input to mapper.getSize()
     *  - Copies the data to ensure no external aliasing
     *  - Updates audioSignal (canonical signal handled by application code)
     *  - Updates playBuffer (audio buffer used by Minim audio library methods)
     *  - Propagates the buffer to active instruments: edit this part for your own code
     * 
     * >>> This is the ONLY method that should mutate the global audio signal state. <<<
     * 
     * In PixelAudio examples, the signal is typically loaded from a file, but
     * it could also be signal cached in memory, a signal generated by code, audio
     * captured live, etc. 
	 * 
	 * @param sig    an audio signal
	 */
	void updateAudioChain(float[] sig) {
	    // Decide target length (make this a single source of truth)
	    int targetSize = mapper.getSize();
	    if (targetSize <= 0) return;
	    // Ensure playBuffer matches target
	    float[] canonical = new float[targetSize];
	    if (sig != null) {
	    	System.arraycopy(sig, 0, canonical, 0, Math.min(sig.length, targetSize));
	    }
	    audioSignal = canonical;
	    audioLength = targetSize;
	    if (playBuffer == null || playBuffer.getBufferSize() != targetSize) {
	        playBuffer = new MultiChannelBuffer(targetSize, 1);
	    }
	    playBuffer.setChannel(0, canonical);
	    // Propagate into audio instruments (adjust to your actual API)
	    if (synth != null) synth.setBuffer(playBuffer);
	}


	/**
	 * Typically called from mouseClicked with mouseX and mouseY, generates audio events.
	 * 
	 * @param x    x-coordinate within a PixelAudioMapper's width
	 * @param y    y-coordinate within a PixelAudioMapper's height
	 */
	public void audioMouseClick(int x, int y) {
		ensureSamplerReady();
		int samplePos = getSamplePos(x, y);
		if (isRandomADSR) {
			ADSRParams env = adsrList.get((int)random(adsrList.size()));
			int len = calcSampleLen();
			print("-- envelope: "+ env.toString());
			println("; pos = "+ samplePos +", length = "+ len);
			playSample(samplePos, len, 0.8f, env);
		}
		else {
			playSample(samplePos, calcSampleLen(), 0.6f);
		}
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

	
	/**
	 * Calculate position of the image pixel within the signal path.
	 *
	 * @param x     x-coordinate, must be clipped to window width
	 * @param y     y-coordinate, must be clipped to window height
	 * @return      index position in audio signal array corresponding to (x, y)
	 */
	public int getSamplePos(int x, int y) {
		return mapper.lookupSignalPos(x, y);
	}



	/**
	 * Plays an audio sample with PASamplerInstrument and custom ADSR.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param samplelen    length of the sample (will be adjusted)
	 * @param amplitude    linear amplitude of the sample on playback
	 * @param adsr         an ADSR envelope for the sample
	 * @return the calculated sample length in samples
	 */
	public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env) {
		// play the sample
		samplelen = synth.playSample(samplePos, samplelen, amplitude, env);
		// get the image coordinates that correspond to samplePos
		int[] coords = mapper.lookupImageCoord(samplePos);
		// samplelen is in samples, translate it to milliseconds
		int durationMS = (int)(samplelen/sampleRate * 1000);
		// prepare a little animation at the image coordinates
		timeLocsArray.add(new TimedLocation(coords[0], coords[1], durationMS + millis()));
		// return the length of the audio event in samples
		return samplelen;
	}

	/**
	 * Plays an audio sample with PASamplerInstrument and default ADSR.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param samplelen    length of the sample (will be adjusted)
	 * @param amplitude    linear amplitude of the sample on playback
	 * @return the calculated sample length in samples
	 */
	public int playSample(int samplePos, int samplelen, float amplitude) {
		// play the sample
		samplelen = synth.playSample(samplePos, samplelen, amplitude);
		// get the image coordinates that correspond to samplePos
		int[] coords = mapper.lookupImageCoord(samplePos);
		// samplelen is in samples, translate it to milliseconds
		int durationMS = (int)(samplelen/sampleRate * 1000);
		// prepare a little animation at the image coordinates
		timeLocsArray.add(new TimedLocation(coords[0], coords[1], durationMS + millis()));
		// return the length of the audio event in samples
		return samplelen;
	}

	/**
	 * Calculates a numerical value using a Gaussian variable, with average value over time equal to noteDuration. 
	 * 
	 * @return a duration value in milliseconds that varies randomly from the global value noteDuration
	 */
	public int calcSampleLen() {
		float vary = 0; 
		// skip the fairly rare negative numbers
		while (vary <= 0) {
			vary = (float) PixelAudio.gauss(1.0, 0.0625);
		}
		int samplelen = (int)(abs((vary * this.noteDuration) * sampleRate / 1000.0f));
		// println("---- calcSampleLen samplelen = "+ samplelen +" samples at "+ sampleRate +"Hz sample rate");
		return samplelen;
	}
	
	/**
	 * Tracks and runs TimedLocation events in the timeLocsArray list, which is 
	 * associated with mouse clicks that trigger audio a the click point.
	 */
	public synchronized void runTimeArray() {
		int currentTime = millis();
		for (Iterator<TimedLocation> iter = timeLocsArray.iterator(); iter.hasNext();) {
			TimedLocation tl = iter.next();
			tl.setStale(tl.eventTime() < currentTime);
			if (!tl.isStale()) {
				drawCircle(tl.getX(), tl.getY());
			}
		}
		timeLocsArray.removeIf(TimedLocation::isStale);		
	}


	/**
	 * Draws a circle at the location of an audio trigger (mouseDown event).
	 * @param x    x coordinate of circle
	 * @param y    y coordinate of circle
	 */
	public void drawCircle(int x, int y) {
		//float size = isRaining? random(10, 30) : 60;
		fill(color(233, 220, 199));
		noStroke();
		circle(x, y, 60);
	}  

	/*        END AUDIO METHODS                        */  		

}
