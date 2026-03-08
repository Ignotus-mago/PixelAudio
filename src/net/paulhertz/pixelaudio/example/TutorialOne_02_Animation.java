
package net.paulhertz.pixelaudio.example;


import processing.core.PApplet;
import processing.core.PImage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

//Mama's ever-lovin' blue-eyed PixelAudio library
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;
import net.paulhertz.pixelaudio.sampler.*;
import net.paulhertz.pixelaudio.schedule.AudioUtility;
import net.paulhertz.pixelaudio.schedule.TimedLocation;
//audio library
import ddf.minim.*;

//video export library
import com.hamoid.*;


/**
 * 
 * <p>This example application builds on TutorialOne_01_FileIO, which provided commands to 
 * open and display audio and image files, transcode image pixel data to audio samples,
 * transcode audio samples to image pixel data, and save audio and image files. 
 * TutorialOne_01_FileIO also introduced a Sampler instrument, PASamplerInstrument, which
 * responds to mouse clicks by playing the audio samples corresponding to the click location
 * in the display image. The other audio instrument, the Granular Synthesis instrument, 
 * will be introduced later on.</p> 
 *
 * <p>We add a very simple form of animation in this sketch. We also provide commands
 * for saving an animation to video. The animation consists of shifting pixels along
 * the Signal Path, which is managed by the PixelAudioMapper object <code>mapper</code>.
 * When the image we are animating is a representation of audio data or some other sort
 * of periodic pattern, pixel-shfting can result in hypnotic patterns. This is specifically
 * demonstrated by the the WaveSynth class and the WaveSynthEditor and ArgosyMixer sample sketches. 
 * When the pixels in the display image are shifted, the correspondence between pixel coordinates
 * and audio buffer index changes. This is particularly evident when the image is a visualization
 * of the audio buffer. If you are looking at an image that represents audio data, 
 * such as Saucer_mixdown.wav, you can see how animation changes the apparent position 
 * of the audio data. When animation is running, repeated clicks at the same location 
 * in the window will trigger different audio events. </p> 
 * 
 * <p>We track the shifting with a variable, <code>totalShift</code>, that preserves the 
 * correspondence. The audio buffer itself is never shifted, nor is the canonic image 
 * <code>baseImage</code>. We just use totalShift to determine where to locate pixels or audio samples. 
 * Of course, if you only want to animate the image, you can just ignore the value of totalShift when 
 * accessing the audioBuffer. </p> 
 * 
 * <p>We continue to use Sampler instrument, showing how it can control the pitch and panning 
 * of an audio event. The playSample() methods in this sketch introduce the most complete 
 * audio triggering method available in PASamplerInstrument, one which can set sample start 
 * position, length, amplitude, ADSR-style envelope, pitch scaling and stereo pan location.</p>  
 * <pre>
 *   samplelen = synth.playSample(samplePos, (int) samplelen, amplitude, env, pitchScaling, pan);
 * </pre>
 * <p>Pitch scaling by default is 1.0, which is to say that samples will be played back at 
 * the recorded frequency. Panning, if it is not supplied as an argument to playSample(), is
 * centered in the stereo field. </p> 
 *
 * <div>    
 * Still to come, as the tutorial advances:<br>
 * -- drawing to trigger audio events<br>
 * -- the Granular Synthesis instruments<br>
 * -- UDP communication with Max and other media applications<br>
 * -- Windowed buffer use to load an audio file into memory and advance through it<br>
 * 
 * <p>The key commands:
 * <ul>
 * <li>Press ' ' to spacebar, play sample at current mouse position.</li>
 * <li>Press 'a' to turn animation on or off.</li>
 * <li>Press 'A' to change animation direction.</li>
 * <li> Press 'b' to rotate pixels by shift value.</li>
 * <li>Press 'm' to turn interactive setting of shift value on or off (drag to set).</li>
 * <li>Press 'c' to apply color from image file to display image (mapImage) only.</li>
 * <li>Press 'C' to apply color from image file to base image and map image.</li>
 * <li>Press 'k' to apply the hue and saturation in the colors array to mapImage.</li>
 * <li>Press 'K' to apply hue and saturation in colors to baseImage and mapImage.</li>
 * <li>Press 'o' or 'O' to open an audio or image file.</li>
 * <li>Press 'p' to select low pitch scaling or default pitch scaling.</li>
 * <li>Press 'P' to select high pitch scaling or default pitch scaling.</li>
 * <li>Press 'd' or 'D' to turn rain on and off.</li>
 * <li>Press 'r' or 'R' to set isRandomADSR, to use default envelope or a random choice.</li>
 * <li>Press 'V' to record a video from frame 0 to frame animSteps.</li>
 * <li>Press 'h' or 'H' to show help message.</li>
 * </ul>
 * </p>
 * </div>
 */
public class TutorialOne_02_Animation extends PApplet {
	// PixelAudio vars and objects
	PixelAudio pixelaudio;     // our shiny new library
	MultiGen multigen;         // a PixelMapGen that links together multiple PixelMapGens
	int genWidth = 512;        // width of multigen PixelMapGens
	int genHeight = 512;       // height of  multigen PixelMapGens
	PixelAudioMapper mapper;   // object for reading, writing, and transcoding audio and image data
	int mapSize;               // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
	// baseImage is a reference image that generally should not be changed except when you load a new file
	PImage baseImage;          // unchanging source image
	// mapImage can change, and often does so with reference to the stable baseImage, for example when animating
	PImage mapImage;           // image for display, may be animated
	PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;
	int[] colors;              // array of spectral colors
	
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
	 * 
	 * Audio playback support is added with the audio variables and audio methods 
	 * (below, in Eclipse, in a tab, in Processing). You will also need the 
	 * PASamplerInstrument and TimedLocation classes. In setup(), call initAudio(), then
	 * add a mousePressed() method that calls audioMousePressed(mouseX, mouseY)
	 * and call runTimeArray() in your draw method. 
	 * 
	 */
	 
	/** Minim audio library */
	Minim minim;                    // library that handles audio 
	AudioOutput audioOut;           // output to sound hardware
	boolean isBufferStale = false;  // flags that audioBuffer needs to be reset
	float sampleRate = 44100;       // target audio engine rate used to configure audioOut
	float fileSampleRate;           // sample rate of most recently opened file (before resampling, but may already == sampleRate)
	float bufferSampleRate;         // sample rate of playBuffer, == fileSampleRate when we don't resample, == sampleRate when we do
	float[] audioSignal;            // the audio signal as an array of floats
	MultiChannelBuffer playBuffer;  // a buffer for playing the audio signal
	int samplePos;                  // index into the audio signal, set when an audio event is triggered
	int audioLength;                // length of the audioSignal, same as the number of pixels in the display image
	// SampleInstrument setup
	int noteDuration = 1500;        // average sample synth note duration, milliseconds
	int samplelen;                  // calculated sample synth note length, samples
	PASamplerInstrument synth;      // instance of class that wraps a Minim Sampler and implements an ADSR envelope
	// ADSR and its parameters
	ADSRParams defaultEnv;          // wrapper for ADSR that keeps its values visible
	float maxAmplitude = 0.7f;      // 0..1
	float attackTime = 0.1f;        // seconds
	float decayTime = 0.3f;         // seconds
	float sustainLevel = 0.25f;     // 0..1
	float releaseTime = 0.1f;       // seconds
	ArrayList<ADSRParams> adsrList; // list of ADSR values
	boolean isRandomADSR = false;   // choose a random envelope from adsrList, or not
	float pitchScaling = 1.0f;      // factor for changing pitch
	float defaultPitchScaling = 1.0f;
	float lowPitchScaling = 0.5f;
	float highPitchScaling = 2.0f;

	// interaction variables for audio
	ArrayList<TimedLocation> timeLocsArray;
	int count = 0;  
	int fileIndex = 0;
	
	
	/* ------------------------------------------------------------------ */
	/*                   ANIMATION AND VIDEO VARIABLES                    */
	/* ------------------------------------------------------------------ */
	  
	int shift = 256;                     // number of pixels to shift the animation
	int totalShift = 0;                  // cumulative shift
	boolean isAnimating = false;         // do we run animation or not?
	boolean oldIsAnimating;              // keep track of animation state when opening a file
	boolean isTrackMouse = false;        // if true, drag the mouse to change shift value
	boolean isRaining = false;           // set to true ('r' key) to automate audio events
	// animation variables
	int animSteps = 720;                 // how many steps in an animation loop
	boolean isRecordingVideo = false;    // are we recording? (only if we are animating)
	int videoFrameRate = 24;             // fps
	int step;                            // number of current step in animation loop, used when recording video
	VideoExport videx;                   // hamoid library class for video export (requires ffmpeg)


	// ** YOUR VARIABLES ** Variables for YOUR CLASS may go here **	//

	
	public static void main(String[] args) {
		PApplet.main(new String[] { TutorialOne_02_Animation.class.getName() });
	}

	public void settings() {
		size(3 * genWidth, 2 * genHeight);
	}
	
	public void setup() {
		frameRate(24);
		// initialize our library
		pixelaudio = new PixelAudio(this);
		// create a PixelMapGen subclass such as MultiGen, with dimensions equal to the display window
		// the call to hilbertLoop3x2 produces a MultiGen that is 3 * genWidth x 2 * genHeight, where
		// genWidth == genHeight and genWidth is a power of 2 (a restriction on Hilbert curves)
		multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
		// create a PixelAudioMapper to handle the mapping of pixel colors to audio samples
		mapper = new PixelAudioMapper(multigen);
		// keep track of the area of the PixelAudioMapper
		mapSize = mapper.getSize();
		// create an array of rainbow colors with mapSize elements
		colors = getColors(mapSize);
		initImages();
		initAudio();
		showHelp();
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
	 * Initializes mapImage with the colors array. 
	 * MapImage handles the color data for mapper and also serves as our display image.
	 * BaseImage is intended as a reference image that usually only changes when you open a new image file.
	 */
	public void initImages() {
	    mapImage = createImage(width, height, ARGB);
	    mapImage.loadPixels();
	    mapper.plantPixels(colors, mapImage.pixels, 0, mapSize);
	    mapImage.updatePixels();
	    baseImage = mapImage.copy();
	}
	
	public void draw() {
		image(mapImage, 0, 0);
		if (isRaining) 
			doRain();
		runTimeArray();    // animate audio event markers
		if (isAnimating) {
			animate();
		}
		if (isTrackMouse && mousePressed) {
			writeToScreen("shift = "+ shift, 16, 24, 24, false);
		}
	}
	
	public void animate() {
		stepAnimation();
		renderFrame(step);
	}
	
	/**
	 * Step through the animation, called by the draw() method.
	 * Will also record a frame of video, if we're recording.
	 */
	public void stepAnimation() {
	  if (step >= animSteps) {
	    step = 0;
	    if (isRecordingVideo) {
	      isRecordingVideo = false;
	      videx.endMovie();
	      println("--- Completed video at frame " + animSteps);
	      isAnimating = oldIsAnimating;
	    }
	  } 
	  else {
	    step += 1;
	    if (isRecordingVideo) {
	      if (videx == null) {
	        println("----->>> start video recording ");
	        videx = new VideoExport(this, "TutorialOneVideo.mp4");
	        videx.setFrameRate(videoFrameRate);
	        videx.startMovie();
	      }
	      videx.saveFrame();
	      println("-- video recording frame " + step + " of " + animSteps);
	    }
	  }
	}
	
	/**
	 * Renders a frame of animation: moving along the signal path, copies baseImage pixels to
	 * mapImage pixels, adjusting the index position of the copy using totalShift
	 * i.e. we don't actually rotate the pixels, we just shift the position they're copied to
	 * 
	 * @param step   current animation step
	 */
	public void renderFrame(int step) {
		totalShift = PixelAudioMapper.wrap(totalShift + shift, mapSize);
		mapImage.loadPixels();
		mapper.copyPixelsAlongPathShifted(baseImage.pixels, mapImage.pixels, totalShift);
		mapImage.updatePixels();
	}
		  
	/**
	 * drop some random audio events, like unto the gentle rain
	 */
	public void doRain() {
	  if (random(20) > 1) return;
	  int sampleLength = 256 * 256;
	  int samplePos = (int) random(sampleLength, mapSize - sampleLength - 1);
	  int[] coords = mapper.lookupImageCoord(samplePos);
	  float panning = map(coords[0], 0, width, -0.8f, 0.8f);
	  // println("----- Rain samplePos = "+ samplePos);
	  ADSRParams env = adsrList.get((int)random(3));
	  playSample(samplePos, calcSampleLen(), 0.4f, env, panning);
	}

	/**
	 * Displays a line of text to the screen, usually in the draw loop. Handy for debugging.
	 * typical call: writeToScreen("When does the mind stop and the world begin?", 64, 1000, 24, true);
	 * 
	 * @param msg     message to write
	 * @param x       x coordinate
	 * @param y       y coordinate
	 * @param weight  font weight
	 * @param isWhite if true, white text, otherwise, black text
	 */
	public void writeToScreen(String msg, int x, int y, int weight, boolean isWhite) {
	  int fill1 = isWhite? 0 : 255;
	  int fill2 = isWhite? 255 : 0;
	  pushStyle();
	  textSize(weight);
	  float tw = textWidth(msg);
	  int pad = 4;
	  fill(fill1);
	  rect(x - pad, y - pad - weight, x + tw + pad, y + weight/2 + pad);
	  fill(fill2);
	  text(msg, x, y);
	  popStyle();
	}
	
	/**
	 * The built-in mousePressed handler for Processing, not used yet...
	 */
	public void mousePressed() {
		// println("mousePressed:", mouseX, mouseY);
	}
	
	public void mouseClicked() {
		// handle audio generation in response to a mouse click
		audioMousePressed(clipToWidth(mouseX), clipToHeight(mouseY));
	}

	public void mouseDragged() {
	    if (isTrackMouse) {
	      shift = abs(width/2 - mouseX);
	      if (mouseY < height/2) {
	        shift = -shift;
	      }
	      writeToScreen("shift = "+ shift, 16, 24, 24, false);
	    }
	}

	public void mouseReleased() {
	  if (isAnimating && isTrackMouse) {
	    println("----- animation shift = "+ shift);
	  }
	}

	/**
	 * built-in keyPressed handler, forwards events to parseKey
	 */
	public void keyPressed() {
	  parseKey(key, keyCode);    
	}

	/**
	 * Handles key press events passed on by the built-in keyPressed method. 
	 * By moving key event handling outside the built-in keyPressed method, 
	 * we make it possible to post key commands without an actual key event.
	 * Methods and interfaces and even other threads can call parseKey(). 
	 * This opens up many possibilities and a some dangers, too.  
	 * 
	 * @param key
	 * @param keyCode
	 */
	public void parseKey(char key, int keyCode) {
		switch(key) {
		case ' ': // spacebar, play sample at current mouse position
			audioMousePressed(clipToWidth(mouseX), clipToHeight(mouseY));
			break;
		case 'a': // turn animation on or off
			isAnimating = !isAnimating;
			println("-- animation is " + isAnimating);
			break;
		case 'A': // change animation direction
			shift = -shift;
			println("-- animation is " + isAnimating +", shift = "+ shift);
			break;
		case 'b': // rotate pixels by shift value
			renderFrame(step);
			break;
		case 'm': // turn interactive setting of shift value on or off (drag to set)
			isTrackMouse = !isTrackMouse;
			println("-- mouse tracking is " + isTrackMouse);
			break;
		case 'c': // apply color from image file to display image (mapImage) only
			chooseColorImage();
			break;
		case 'C': // apply color from image file to base image and map image
			chooseColorImageAndStore();
			break;
		case 'k': // apply the hue and saturation in the colors array to mapImage 
			refreshMapImageFromBase();
			mapImage.loadPixels();
			applyColorShifted(colors, mapImage.pixels, mapper.getImageToSignalLUT(), totalShift);
			mapImage.updatePixels();
			break;
		case 'K': // apply hue and saturation in colors to baseImage and mapImage
			baseImage.loadPixels();
			applyColor(colors, baseImage.pixels, mapper.getImageToSignalLUT());
			baseImage.updatePixels();
			refreshMapImageFromBase();
			break;
		case 'o': case 'O': // open an audio or image file
			chooseFile();
			break;
		case 'p': // select low pitch scaling or default pitch scaling
			if (pitchScaling != lowPitchScaling)
				pitchScaling = lowPitchScaling;
			else
				pitchScaling = defaultPitchScaling;
			break;
		case 'P': // select high pitch scaling or default pitch scaling
			if (pitchScaling != highPitchScaling)
				pitchScaling = highPitchScaling;
			else
				pitchScaling = defaultPitchScaling;
			break;
		case 'd': case 'D': // turn rain on and off
			isRaining = !isRaining;
			println(isRaining ? "It's raining" : "It's not raining");
			break;
		case 'r': case'R': // set isRandomADSR, to use default envelope or a random choice
			isRandomADSR = !isRandomADSR;
			String msg = isRandomADSR ? " synth uses a random ADSR" : " synth uses default ADSR";
			println("---- isRandomADSR = "+ isRandomADSR +","+ msg);
			break;
		case 'V': // record a video from frame 0 to frame animSteps
			// records a complete video loop with following actions:
			// Go to frame 0, turn recording on, turn animation on.
			// This will record a complete video loop, from frame 0 to the
			// stop frame value in the GUI control panel.
			step = 0;
			renderFrame(step);
			isRecordingVideo = true;
			oldIsAnimating = isAnimating;
			isAnimating = true;
			break;
		case 'h': case 'H': // show help message
			showHelp();
			break;
		default:
			break;
		}
	}

	/**
	 * to generate help output, run RegEx search/replace on parseKey case lines with:
	 * // case ('.'): // (.+)
	 * // println(" * Press $1 to $2.");
	 */
	public void showHelp() {
		println(" * Press ' ' to spacebar, play sample at current mouse position.");
		println(" * Press 'a' to turn animation on or off.");
		println(" * Press 'A' to change animation direction.");
		println(" * Press 'b' to rotate pixels by shift value.");
		println(" * Press 'm' to turn interactive setting of shift value on or off (drag to set).");
		println(" * Press 'c' to apply color from image file to display image (mapImage) only.");
		println(" * Press 'C' to apply color from image file to base image and map image.");
		println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage .");
		println(" * Press 'K' to apply hue and saturation in colors to baseImage and mapImage.");
		println(" * Press 'o' or 'O' to open an audio or image file.");
		println(" * Press 'p' to select low pitch scaling or default pitch scaling.");
		println(" * Press 'P' to select high pitch scaling or default pitch scaling.");
		println(" * Press 'd' or 'D' to turn rain on and off.");
		println(" * Press 'r' or 'R' to set isRandomADSR, to use default envelope or a random choice.");
		println(" * Press 'V' to record a video from frame 0 to frame animSteps.");
		println(" * Press 'h' or 'H' to show help message.");
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

	public int[] applyColorShifted(int[] colorSource, int[] graySource, int[] lut, int shift) {
	    if (colorSource == null || graySource == null || lut == null)
	        throw new IllegalArgumentException("colorSource, graySource and lut cannot be null.");
	    if (colorSource.length != graySource.length || colorSource.length != lut.length)
	        throw new IllegalArgumentException("colorSource, graySource and lut must all have the same length.");
	    int n = graySource.length;
	    int s = ((shift % n) + n) % n; // wrap + allow negative shifts
	    float[] hsbPixel = new float[3];
	    for (int i = 0; i < n; i++) {
	        int srcIdx = lut[i] + s;
	        if (srcIdx >= n) srcIdx -= n; // faster than % in tight loop
	        graySource[i] = PixelAudioMapper.applyColor(colorSource[srcIdx], graySource[i], hsbPixel);
	    }
	    return graySource;
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

	public void chooseColorImageAndStore() {
		selectInput("Choose an image file to apply color: ", "colorFileSelectedToStore");
	}

	/**
	 * callback method for chooseColorImage() 
	 * @param selectedFile    the File the user selected
	 */
	public void colorFileSelected(File selectedFile) {
		if (selectedFile == null) {
			println("----- No file was selected.");
			return;
		}
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
	
	public void colorFileSelectedToStore(File selectedFile) {
		colorFileSelected(selectedFile);
		baseImage = mapImage.copy();
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
	 * Attempts to load audio data from a selected file into playBuffer, then
	 * calls writeAudioToImage() to transcode audio data and write it to mapImage
	 * 
	 * As in most PixelAudio examples, we provide built-in resampling of audio data
	 * from the file to match the sampling rate of the buffer to audioOut.sampleOut().
	 * 
	 * @param audFile    an audio file
	 */
	public void loadAudioFile(File audFile) {
		MultiChannelBuffer buff = new MultiChannelBuffer(1024, 1);
		fileSampleRate =  minim.loadFileIntoBuffer(audFile.getAbsolutePath(), buff);
        // load the audio file and resample it if necessary
		if (fileSampleRate > 0) {
			if (fileSampleRate != audioOut.sampleRate()) {
				float[] resampled = AudioUtility.resampleMonoToOutput(buff.getChannel(0), fileSampleRate, audioOut);
				buff.setBufferSize(resampled.length);
				buff.setChannel(0, resampled);
				bufferSampleRate = audioOut.sampleRate();
			}
			else {
				bufferSampleRate = fileSampleRate;
			}
			this.audioFileLength = buff.getBufferSize();
			println("---- file sample rate = "+ this.fileSampleRate 
					+", buffer sample rate = "+ bufferSampleRate
					+", audio output sample rate = "+ audioOut.sampleRate());
		}
		else {
			println("-- Unable to load file. File may be empty, wrong format, or damaged.");
			return;
		}
		// adjust buffer size to mapper.getSize()
		if (buff.getBufferSize() != mapper.getSize()) buff.setBufferSize(mapper.getSize());
		playBuffer = buff;
		// read channel 0 the buffer into audioSignal, truncated or padded to fit mapSize
		audioSignal = Arrays.copyOf(playBuffer.getChannel(0), mapSize);
		audioLength = audioSignal.length;
		// load the buffer of our PASamplerInstrument at the sample rate of the buffer
		synth.setBuffer(playBuffer, bufferSampleRate);
        // automatically write the signal to mapImage -- this will change in later tutorials
        renderAudioToMapImage(chan, 0);
        commitMapImageToBaseImage();
	}
	
	/**
	 * Render audio into mapImage at an arbitrary phase offset.
	 * In simple examples, shift will usually be 0. In later examples or 
	 * in your own code, shifted renders may be layered, blended,
	 * or used as temporary performance/display operations without
	 * committing them to baseImage.
	 */
	public void renderAudioToMapImage(PixelAudioMapper.ChannelNames chan, int shift) {
	    // Render current audioSignal into mapImage using current mapper & current totalShift
	    writeAudioToImage(audioSignal, mapper, mapImage, chan, shift);
	}
	
	/**
	 * Transcodes audio data in sig[] and writes it to color channel chan of img 
	 * using the lookup tables in mapper to redirect indexing. Calls mapper.mapSigToImgShifted(), 
	 * which will throw an IllegalArgumentException if sig.length != img.pixels.length
	 * or sig.length != mapper.getSize(). 
	 * 
	 * @param sig         an array of float, should be audio data in the range [-1.0, 1.0]
	 * @param mapper      a PixelAudioMapper
	 * @param img         a PImage
	 * @param chan        a color channel
	 * @param shift       the number of indices to shift when writing audio
	 */
	public void writeAudioToImage(float[] sig, PixelAudioMapper mapper, PImage img, PixelAudioMapper.ChannelNames chan, int shift) {
		img.loadPixels();
	    mapper.mapSigToImgShifted(sig, img.pixels, chan, shift); // commit current phase
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
		// prepare to copy image data to audio variables
		// resize the buffer to mapSize, if necessary -- signal will not be overwritten
		if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
		audioSignal = playBuffer.getChannel(0);
		writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
		totalShift = 0;    // reset animation shift
		// now that the image data has been written to audioSignal, set playBuffer channel 0 to the new audio data
		playBuffer.setChannel(0, audioSignal);
		audioLength = audioSignal.length;
		commitMapImageToBaseImage();
	}

	/**
	 * This method writes a color channel from the an image to playBuffer, fulfilling a 
	 * central concept of the PixelAudio library: image is sound. Calls mapper.mapImgToSig(), 
	 * which will throw an IllegalArgumentException if img.pixels.length != sig.length or 
	 * img.width * img.height != mapper.getWidth() * mapper.getHeight(). 
	 * 
	 * @param img       a PImage, a source of data
	 * @param mapper    a PixelAudioMapper, handles mapping between image and audio signal
	 * @param sig       an target array of float in audio format 
	 * @param chan      a color channel
	 */
	public void writeImageToAudio(PImage img, PixelAudioMapper mapper, float[] sig, PixelAudioMapper.ChannelNames chan) {
		sig = mapper.mapImgToSig(img.pixels, sig, chan);
	}		
	
	public void commitMapImageToBaseImage() {
	    baseImage = mapImage.copy();
	    totalShift = 0;
	}

	public void commitNewBaseImage(PImage img) {
	    baseImage = img.copy();
	    mapImage = img.copy();
	    totalShift = 0;
	}
	
	public void refreshMapImageFromBase() {
	    mapImage.loadPixels();
	    mapper.copyPixelsAlongPathShifted(baseImage.pixels, mapImage.pixels, totalShift);
	    mapImage.updatePixels();
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
			println("--->> There was an error outputting the audio file " + fileName +", "	+ e.getMessage());
		} catch (UnsupportedAudioFileException e) {
			println("--->> The file format is unsupported " + e.getMessage());
		}
	}
	
	/**
	 * Saves audio data to 16-bit integer PCM format, which Processing can also open.
	 * This same method can be called as a static method in PixelAudio.
	 * 
	 * @param samples			an array of floats in the audio range (-1.0f, 1.0f)
	 * @param sampleRate		audio sample rate for the file
	 * @param fileName			name of the file to save to
	 * @throws IOException		an Exception you'll need to handle to call this method
	 * @throws UnsupportedAudioFileException		another Exception
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
		// reduce the output level by 6.0 dB.
		audioOut.setGain(-6.0f);
		// create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
		// playBuffer will not contain audio data until we load a file
		this.playBuffer = new MultiChannelBuffer(mapSize, 1);
		this.audioSignal = playBuffer.getChannel(0);
		this.audioLength = audioSignal.length;
		// ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
		defaultEnv = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
		initADSRList();
		// create a PASamplerInstrument with 8 voices, adsrParams will be its default envelope
		synth = new PASamplerInstrument(playBuffer, audioOut.sampleRate(), 8, audioOut, defaultEnv);
		// initialize mouse event tracking array
		timeLocsArray = new ArrayList<TimedLocation>();
	}

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
	 * Prepares audioSignal before it is used as an instrument source.
	 * Modify as needed to prepare your audio signal data.
	 */
	public void renderSignals() {
	  writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
	  playBuffer.setChannel(0, audioSignal);
	  audioLength = audioSignal.length;
	}

	/**
	 * Typically called from mousePressed with mouseX and mouseY, generates audio events.
	 * 
	 * @param x    x-coordinate within a PixelAudioMapper's width
	 * @param y    y-coordinate within a PixelAudioMapper's height
	 */
	public void audioMousePressed(int x, int y) {
		samplePos = getSamplePos(x,y);
		float panning = map(x, 0, width, -0.8f, 0.8f);
		// update audioSignal and playBuffer if audioSignal hasn't been initialized or if 
		// playBuffer needs to be refreshed after changes to its data source (isBufferStale == true).
		if (audioSignal == null || isBufferStale) {
			renderSignals();
			isBufferStale = false;
		}
		if (isRandomADSR) {
			ADSRParams env = adsrList.get((int)random(adsrList.size()));
			println("-- "+ env.toString());
			playSample(samplePos, calcSampleLen(), 0.6f, env, panning);
		}
		else {
			playSample(samplePos, calcSampleLen(), 0.6f, panning);
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
	 * Calculates the index of the image pixel within the signal path,
	 * taking the shifting of pixels and audioSignal into account.
	 * See the MusicWindowBox sketch for use of a windowed buffer in this calculation. 
	 * 
	 * @param x    an x coordinate within mapImage and display bounds
	 * @param y    a y coordinate within mapImage and display bounds
	 * @return     the index of the sample corresponding to (x,y) on the signal path
	 */
	public int getSamplePos(int x, int y) {
	    return mapper.lookupSignalPosShifted(x, y, totalShift);
	}
	
	/**
	 * Plays an audio sample with PASamplerInstrument and custom ADSR.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param samplelen    length of the sample (will be adjusted)
	 * @param amplitude    amplitude of the sample on playback
	 * @param adsr         an ADSR envelope for the sample
	 * @return the calculated sample length in samples
	 */
	public int playSample(int samplePos, int samplelen, float amplitude, ADSRParams env, float pan) {
	  samplelen = synth.playSample(samplePos, (int) samplelen, amplitude, env, pitchScaling, pan);
	  int durationMS = (int)(samplelen/sampleRate * 1000);
	  int[] coords = mapper.lookupImageCoordShifted(samplePos, totalShift);
	  timeLocsArray.add(new TimedLocation(coords[0], coords[1], durationMS + millis()));
	  // return the length of the sample
	  return samplelen;
	}

	/**
	 * Plays an audio sample with PASamplerInstrument and default ADSR.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param samplelen    length of the sample (will be adjusted)
	 * @param amplitude    amplitude of the sample on playback
	 * @return the calculated sample length in samples
	 */
	public int playSample(int samplePos, int samplelen, float amplitude, float pan) {
	  samplelen = synth.playSample(samplePos, (int) samplelen, amplitude, defaultEnv, pitchScaling, pan);
	  int durationMS = (int)(samplelen/sampleRate * 1000);
	  int[] coords = mapper.lookupImageCoordShifted(samplePos, totalShift);
	  timeLocsArray.add(new TimedLocation(coords[0], coords[1], durationMS + millis()));
	  // return the length of the sample
	  return samplelen;
	}

	public int calcSampleLen() {
	  float vary = 0; 
	  // skip the fairly rare negative numbers
	  while (vary <= 0) {
	    vary = (float) PixelAudio.gauss(1.0, 0.0625);
	  }
	  samplelen = (int)(abs((vary * this.noteDuration) * sampleRate / 1000.0f));
	  // println("---- calcSampleLen samplelen = "+ samplelen +" samples at "+ sampleRate +"Hz sample rate");
	  return samplelen;
	}

	/**
	 * Run the animation for audio events. 
	 */
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