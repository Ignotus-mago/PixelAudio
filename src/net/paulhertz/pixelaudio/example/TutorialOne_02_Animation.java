
package net.paulhertz.pixelaudio.example;


import processing.core.PApplet;
import processing.core.PImage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

//Audio support from Java
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
 * <p>
 * This example sketch builds on TutorialOne_01_FileIO, which provided commands
 * to open and display audio and image files, transcode image pixel data to
 * audio samples, transcode audio samples to image pixel data, and save audio
 * and image files. TutorialOne_01_FileIO also introduced a digital audio
 * instrument, PASamplerInstrument, and provided an interface which responds to
 * mouse clicks by playing the audio samples corresponding to the click location
 * in the display image. In this sketch, we add panning and pitch control to
 * PASamplerInstrument <code>playSample(...)</code> methods. The other audio
 * instrument, the Granular Synthesis instrument, will be introduced later on.
 * This sketch introduces the animation feature built-in to PixelAudioMapper,
 * pixel-shifting along the Signal Path, which shifts pixels in the display 
 * and changes the correspondence between screen coordinates and audio signal
 * index position. The animation provides an analog to visual "audio streaming"
 * and can be used to create a sort of "music box" audio player. 
 * </p>
 * <pre>
 *   QUICK START
 * 
 *   1. Launch the sketch and then press the 'o' key to open an image or audio
 *      file. The "audioBlend.wav" file is good for experimenting. The audio
 *      data in it will be transcoded to gray scale pixel data and displayed in
 *      the window. Click on the display image to trigger audio events. To see
 *      the signal path as a color overlay, press the 'K' key. 
 *   
 *   2. Press 'm' to run the "Music Box" routine, which triggers an audio event
 *      every frame in the same location while animating the display image with
 *      pixel shifting. Use the UP and DOWN arrow keys to change the amount of
 *      pixel shifting, and the 'A' key to reverse the direction of shifting.
 *      Use the 'p' and 'P' keys to change the pitch of the audio. Press 'r'
 *      to turn random ADSR envelope selection on and off.
 *      See below for more information.
 * </pre>
 * <p>
 * We add a simple form of animation in this sketch, which consists of shifting
 * the pixels in the display image along the Signal Path. This is a simple way
 * to create an animation that is directly tied to the audio data. When the
 * image we are animating is a representation of audio data or some other sort
 * of periodic pattern, pixel-shfting can result in hypnotic animated patterns.
 * This is demonstrated by the the WaveSynth class and the WaveSynthEditor and
 * ArgosyMixer sample sketches. When the pixels in the display image are
 * shifted, the correspondence between pixel coordinates and audio buffer index
 * changes. This is particularly evident when the image is a visualization of
 * the audio buffer. If you are looking at an image that represents audio data,
 * such as Saucer_mixdown.wav, you can see how animation changes the apparent
 * position of the audio data. When animation is running, repeated clicks at the
 * same location in the window will trigger different audio events.
 * </p><p>
 * Pixel shifting animation works by moving the pixels in the display image
 * <code>mapImage</code> once every frame by the number of pixels specified by
 * the variable <code>shift</code>, and tracking the accummulated shifting with
 * another variable, <code>totalShift</code>. The audio buffer itself is never
 * shifted, nor is the canonic image <code>baseImage</code>. We just use
 * totalShift to determine where to locate pixels or audio samples. The shifting
 * is managed by the PixelAudioMapper object <code>mapper</code>, which provides
 * methods for accessing pixel and audio data with shifting taken into account.
 * Of course, if you only want to animate the image, you can just ignore the
 * value of totalShift when accessing the audioBuffer.
 * </p><p>
 * The shift variable can be tied to the audio sample rate and the video frame
 * rate in such a way that the animation is synchronized with the audio when we
 * trigger an audio event once every frame. If the audio sample rate is 44100
 * and the video frame rate is 24, as it is in this example, then we can set
 * shift to 44100/24, which is about 1838 ('m' key). If we then trigger an audio
 * event once every frame in the same location in the window, the audio events
 * will advance through the buffer in sync with the animation, and the audio
 * will play more or less as it would if it were streaming from a file. If you
 * double the shift or cut it in half (UP ARROW or DOWN ARROW), the audio events
 * will happen at double or half speed. If you reverse the sign of shift ('A'
 * key), the animation will run backwards, and the audio events will also happen
 * in reverse order. 
 * </p><p>
 * We also provide commands for saving an animation to video ('V' key) and saving
 * the display image and audio buffer to files. To visualize the Signal Path, you
 * can use the 'k' or 'K' keys to overlay a spectrum of color on the display
 * image. The 'K' key will write the spectrum to <code>baseImage</code>, so that
 * it will persist when running animation. The 'k' key will write color data to
 * <code>mapImage</code> only.
 * </p><p>
 * We continue to use the Sampler instrument for audio events, showing how it
 * can control the pitch and panning of an audio event. The playSample() methods
 * in this sketch introduce the most complete audio triggering method available
 * in PASamplerInstrument, one which can set sample start position, length,
 * amplitude, ADSR-style envelope, pitch scaling and stereo pan location.
 * </p>
 * <pre>
 *   samplelen = synth.playSample(samplePos, (int) samplelen, amplitude, env, pitchScaling, pan);
 * </pre>
 * <p>
 * Pitch scaling by default is 1.0, which is to say that samples will be played
 * back at the recorded frequency. Panning, if it is not supplied as an argument
 * to playSample(), is centered in the stereo field. We calculate the panning
 * value by mapping the x-coordinate of the mouse position to the stereo pan
 * value.
 * </p> * <div>    
 * Still to come, as the tutorial advances:<br>
 * -- drawing to trigger audio events<br>
 * -- the Granular Synthesis instruments<br>
 * -- UDP communication with Max and other media applications<br>
 * 
 * <p>The key commands:
 * <ul>
 * <li>Press UP ARROW to increment pixel shift.</li>
 * <li>Press DOWN ARROW to decrement pixel shift.</li>
 * <li>Press ' ' (spacebar) to play sample at current mouse position.</li>
 * <li>Press 'a' to turn animation on or off.</li>
 * <li>Press 'A' to change animation direction.</li>
 * <li>Press 'f' to rotate pixels by shift value .</li>
 * <li>Press 'F' to rotate pixels by shift value .</li>
 * <li>Press 'm' to play audio events synced to audio sample rate and video frame rate.</li>
 * <li>Press 'c' to apply color from image file to display image (mapImage) only.</li>
 * <li>Press 'C' to apply color from image file to base image and map image.</li>
 * <li>Press 'k' to apply hue and saturation in the spectrum array to mapImage .</li>
 * <li>Press 'K' to apply hue and saturation in the spectrum array to baseImage.</li>
 * <li>Press 'o' or 'O' to open an audio or image file.</li>
 * <li>Press 'p' to select low pitch scaling or default pitch scaling.</li>
 * <li>Press 'P' to select high pitch scaling or default pitch scaling.</li>
 * <li>Press 'd' or 'D' to turn rain on and off.</li>
 * <li>Press 'r' or 'R' to toggle isRandomADSR, to use default envelope or a random choice.</li>
 * <li>Press 's' to save display image to a PNG file.</li>
 * <li>Press 'S' to save audio buffer to a .wav file.</li>
 * <li>Press 'V' to record a video from frame 0 to frame animSteps.</li>
 * <li>Press 'w' to show data values in display.</li>
 * <li>Press 'h' or 'H' to show help message.</li>
 * </ul>
 * </p>
 * </div>
 */
public class TutorialOne_02_Animation extends PApplet {

	/* ------------------------------------------------------------------ */
	/*                       PIXELAUDIO VARIABLES                         */
	/* ------------------------------------------------------------------ */

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
	PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;    // color channel that receives pixel data
	int[] spectrum;              // array of spectral colors
	
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
	int shiftInc = 16;                   // increment/decrement value for shift
	int totalShift = 0;                  // cumulative shift
	boolean isAnimating = false;         // do we run animation or not?
	boolean oldIsAnimating;              // keep track of animation state when opening a file
	// animation variables
	int animSteps = 768;                 // how many steps in an animation loop
	boolean isRecordingVideo = false;    // are we recording? (only if we are animating)
	int videoFrameRate = 24;             // fps
	int step;                            // number of current step in animation loop, used when recording video
	VideoExport videx;                   // hamoid library class for video export (requires ffmpeg)
	
	// application settings

	boolean isRaining = false;           // toggle to true to run random "raindrop" audio events ('d' key)
	boolean isShowOverlay = false;       // toggle for overlay of text in display ('w' key)
	boolean isPlayMusicBox = false;      // toggle to show how audio events can be tied to frame rate ('m' key)


	
	public static void main(String[] args) {
		PApplet.main(new String[] { TutorialOne_02_Animation.class.getName() });
	}

	public void settings() {
		size(3 * genWidth, 2 * genHeight);
	}
	
	public void setup() {
		frameRate(videoFrameRate);
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

	/**
	 * turn off audio processing when we exit
	 */
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
		colorMode(HSB, colorWheel.length, 100, 100); // create colors in the HSB color space, giving hue a very wide range
		for (int i = 0; i < colorWheel.length; i++) {
			colorWheel[i] = color(i, 50, 70); // fill our array with colors, gradually changing hue
		}
		popStyle(); // restore styles, including the default RGB color space
		return colorWheel;
	}
	
	/**
	 * Initializes mapImage with the colors array, copies mapImage to baseImage.
	 * MapImage handles the color data for mapper and also serves as our display image.
	 * BaseImage is intended as a reference image that typically only changes when you load a new image.
	 */
	public void initImages() {
	    mapImage = createImage(width, height, ARGB);
	    mapImage.loadPixels();
	    mapper.plantPixels(spectrum, mapImage.pixels, 0, mapSize);
	    mapImage.updatePixels();
	    baseImage = mapImage.copy();
	}
	
	public void draw() {
		image(mapImage, 0, 0);
		if (isRaining) 
			doRain();
		runTimeArray();    // animate audio event markers
		if (isAnimating) {
			if (isPlayMusicBox) audioMouseClick(width/2 - 32, height/2 - 32);
			animate();
		}
		if (isShowOverlay) {
			writeToScreen("shift = "+ shift, 64, 1000, 24, true);
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
	 * @param isInverseShift   boolean, if true shift is negative
	 */
	public void renderFrame(int step, boolean isInverseShift) {
		int signedShift = isInverseShift ? - shift : shift;
		totalShift = PixelAudioMapper.wrap(totalShift + signedShift, mapSize);
		mapImage.loadPixels();
		mapper.copyPixelsAlongPathShifted(baseImage.pixels, mapImage.pixels, totalShift);
		mapImage.updatePixels();
	}
	
	public void renderFrame(int step) {
		if (step < 0) renderFrame(step, true); 
		else renderFrame(step, false);
	}
		  
	/**
	 * drop some random audio events, like unto the gentle rain
	 */
	public void doRain() {
	  if (random(20) > 1) return;
	  int sampleLength = 256 * 256;
	  int samplePos = (int) random(sampleLength, mapSize - sampleLength - 1);
	  int[] coords = mapper.lookupImageCoord(samplePos);
	  int len = calcSampleLen();
	  float rainGain = 0.4f;
	  ADSRParams env = adsrList.get((int)random(3));
	  float panning = map(coords[0], 0, width, -0.875f, 0.875f);
	  playSample(samplePos, len, rainGain, env, panning);
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
		audioMouseClick(clipToWidth(mouseX), clipToHeight(mouseY));
	}

	public void mouseDragged() {
		// mouseDragged events
	}

	public void mouseReleased() {
		// mouseReleased events
	}

	/**
     * built-in keyPressed handler, forwards events to parseKey.
     */
    @Override
    public void keyPressed() {
    	if (key != CODED) {
    		parseKey(key, keyCode);
    	}
    	else {
    		int maxShift = 16384;
    		int minShift = shiftInc;
			if (keyCode == UP) { // increment shift value by shiftInc
				shift = shift < maxShift ? shift + shiftInc : shift;
			}
			else if (keyCode == DOWN) { // decrement shift value by shiftInc
				shift = shift > minShift ? shift - shiftInc : shift;
			}
			else if (keyCode == RIGHT) {

			}
			else if (keyCode == LEFT) {

			}
    	}
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
			audioMouseClick(clipToWidth(mouseX), clipToHeight(mouseY));
			break;
		case 'a': // turn animation on or off
			isAnimating = !isAnimating;
			println("-- animation is " + isAnimating);
			break;
		case 'A': // change animation direction
			shift = -shift;
			println("-- animation is " + isAnimating +", shift = "+ shift);
			break;
		case 'f': // rotate pixels by shift value 
			renderFrame(step);
			break;
		case 'F': // rotate pixels by shift value 
			renderFrame(-step);
			break;
		case 'm': // play audio events synced to audio sample rate and video frame rate
			isPlayMusicBox = !isPlayMusicBox;
			if (isPlayMusicBox) {
				isShowOverlay = true;
				float rate = this.frameRate;    // system frame rate, we could use videoFrameRate instead
				shift = Math.round(audioOut.sampleRate() / rate);
				println("-- starting Music Box with shift = "+ shift +" at frame rate "+ rate);
				isAnimating = true;
			}
			break;
		case 'c': // apply color from image file to display image (mapImage) only
			chooseColorImage();
			break;
		case 'C': // apply color from image file to base image and map image
			chooseColorImageAndStore();
			break;
		case 'k': // apply hue and saturation in the spectrum array to mapImage 
			applyColorMapToDisplay(false);
			break;
		case 'K': // apply hue and saturation in the spectrum array to baseImage
			applyColorMapToDisplay(true);
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
		case 'r': case 'R': // set isRandomADSR, to use default envelope or a random choice
			isRandomADSR = !isRandomADSR;
			String msg = isRandomADSR ? " synth uses a random ADSR" : " synth uses default ADSR";
			println("---- isRandomADSR = "+ isRandomADSR +","+ msg);
			break;
		case 's': // save display image to a PNG file
			saveToImage();
			break;
		case 'S': // save audio buffer to a .wav file
			saveToAudio();
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
		case 'w': // show data values in display
			isShowOverlay = !isShowOverlay;
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
		println(" * Press UP ARROW to increment pixel shift.");
		println(" * Press DOWN ARROW to decrement pixel shift.");
		println(" * Press ' ' (spacebar) to play sample at current mouse position.");
		println(" * Press 'a' to turn animation on or off.");
		println(" * Press 'A' to change animation direction.");
		println(" * Press 'f' to rotate pixels by shift value .");
		println(" * Press 'F' to rotate pixels by shift value .");
		println(" * Press 'm' to play audio events synced to audio sample rate and video frame rate.");
		println(" * Press 'c' to apply color from image file to display image (mapImage) only.");
		println(" * Press 'C' to apply color from image file to base image and map image.");
		println(" * Press 'k' to apply hue and saturation in the spectrum array to mapImage .");
		println(" * Press 'K' to apply hue and saturation in the spectrum array to baseImage.");
		println(" * Press 'o' or 'O' to open an audio or image file.");
		println(" * Press 'p' to select low pitch scaling or default pitch scaling.");
		println(" * Press 'P' to select high pitch scaling or default pitch scaling.");
		println(" * Press 'd' or 'D' to turn rain on and off.");
		println(" * Press 'r' or 'R' to toggle isRandomADSR, to use default envelope or a random choice.");
		println(" * Press 's' to save display image to a PNG file.");
		println(" * Press 'S' to save audio buffer to a .wav file.");
		println(" * Press 'V' to record a video from frame 0 to frame animSteps.");
		println(" * Press 'w' to show data values in display.");
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
		return applyColorShifted(colorSource, graySource, lut, 0);
	}

	/**
	 * Utility method for applying hue and saturation values from a source array of RGB values
	 * to the brightness values in a target array of RGB values, using a lookup table to redirect indexing.
	 * 
	 * @param colorSource    a source array of RGB data from which to obtain hue and saturation values
	 * @param graySource     an target array of RGB data from which to obtain brightness values
	 * @param lut            a lookup table, must be the same size as colorSource and graySource
	 * @param pixelShift     total amount of pixel shifting in the image
	 * @return the graySource array of RGB values, with hue and saturation values changed
	 * @throws IllegalArgumentException if array arguments are null or if they are not the same length
	 */
	public int[] applyColorShifted(int[] colorSource, int[] graySource, int[] lut, int pixelShift) {
	    if (colorSource == null || graySource == null || lut == null)
	        throw new IllegalArgumentException("colorSource, graySource and lut cannot be null.");
	    if (colorSource.length != graySource.length || colorSource.length != lut.length)
	        throw new IllegalArgumentException("colorSource, graySource and lut must all have the same length.");
	    int n = graySource.length;
	    int s = ((pixelShift % n) + n) % n; // wrap + allow negative shifts
	    float[] hsbPixel = new float[3];
	    for (int i = 0; i < n; i++) {
	        int srcIdx = lut[i] + s;
	        if (srcIdx >= n) srcIdx -= n; // faster than % in tight loop
	        graySource[i] = PixelAudioMapper.applyColor(colorSource[srcIdx], graySource[i], hsbPixel);
	    }
	    return graySource;
	}	

	/**
	 * Apply color map hue and saturation to mapImage or baseImage.
	 *
	 * @param updateBaseImage    if true, update baseImage, otherwise just update mapImage
	 */
	public void applyColorMapToDisplay(boolean updateBaseImage) {
		if (updateBaseImage) {
			baseImage.loadPixels();
			applyColor(spectrum, baseImage.pixels, mapper.getImageToSignalLUT());
			baseImage.updatePixels();
			refreshMapImageFromBase();
		} else {
			refreshMapImageFromBase();
			mapImage.loadPixels();
			applyColorShifted(spectrum, mapImage.pixels, mapper.getImageToSignalLUT(), totalShift);
			mapImage.updatePixels();
		}
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
		// update dependent audio sources
		updateAudioChain(playBuffer.getChannel(0), fileSampleRate);
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
		// create a new array for the audio signal
		float[] sig = new float[mapper.getSize()];
		audioSignal = sig;
		// write transcoded pixel data from HSB Brightness channel to audioSignal
		renderMapImageToAudio(PixelAudioMapper.ChannelNames.L);
		// update all dependent audio data
		updateAudioChain(sig);
		// update baseImage from mapImage
		commitMapImageToBaseImage();
		// uncomment next line to get automatic color overlay update on image file load
		// applyColorMapToDisplay(true);
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
	 * @param shift     number of indices to shift 
	 */
	public void writeImageToAudio(PImage img, PixelAudioMapper mapper, float[] sig, PixelAudioMapper.ChannelNames chan, int shift) {
		// If img is the *display* (shifted) image, commit its phase into audio:
		sig = mapper.mapImgToSigShifted(img.pixels, sig, chan, shift);
	}
	
	/**
	 * Writes a specified channel of mapImage to audioSignal.
	 * 
	 * @param chan    the selected color channel
	 */
	public void renderMapImageToAudio(PixelAudioMapper.ChannelNames chan) {
		writeImageToAudio(mapImage, mapper, audioSignal, chan, totalShift);
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
		
	
	// -------- AUDIO AND IMAGE FILE OUTPUT --------- //

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
		// PixelAudio instruments require a STEREO output. 1024 is a standard number of
		// samples for the output buffer to process at one time. You should usually set
		// the output sampleRate to either 41500 or 48000, standards for digital audio.
		this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
		// reduce the output level by 6.0 dB.
		audioOut.setGain(audioGain);
		// create a Minim MultiChannelBuffer with one channel, buffer size equal to mapSize
		// playBuffer will not contain audio data until we load a file
		this.playBuffer = new MultiChannelBuffer(mapSize, 1);
		this.audioSignal = playBuffer.getChannel(0);
		this.audioLength = audioSignal.length;
		// ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
		defaultEnv = new ADSRParams(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
		initADSRList();
		// create a PASamplerInstrument with 8 voices, adsrParams will be its default envelope
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
	 * Typically called from mousePressed with mouseX and mouseY, generates audio events.
	 * 
	 * @param x    x-coordinate within a PixelAudioMapper's width
	 * @param y    y-coordinate within a PixelAudioMapper's height
	 */
	public void audioMouseClick(int x, int y) {
		ensureSamplerReady();
		int samplePos = getSamplePos(x,y);
		float panning = map(x, 0, width, -0.875f, 0.875f);
		if (isRandomADSR) {
			ADSRParams env = adsrList.get((int)random(adsrList.size()));
			int len = calcSampleLen();
			// don't output envelope information for automated events
			if (!isPlayMusicBox || isRaining) {
				print("-- envelope: "+ env.toString());
				println("; pos = "+ samplePos +", length = "+ len);
			}
			playSample(samplePos, len, 0.8f, env, panning);
		}
		else {
			playSample(samplePos, calcSampleLen(), 0.8f, panning);
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
	  int samplelen = (int)(abs((vary * this.noteDuration) * sampleRate / 1000.0f));
	  // println("---- calcSampleLen samplelen = "+ samplelen +" samples at "+ sampleRate +"Hz sample rate");
	  return samplelen;
	}
	
	/**
	 * Bottleneck "commit" method for audio state. 
     * 
     * Takes an arbitrary input signal and installs it as the canonical audio signal
     * used by the application. This method:
     *
     *  - Resizes/pads/truncates the input to mapper.getSize()
     *  - Copies the data to ensure no external aliasing
     *  - Updates audioSignal (canonical signal handled by application code)
     *  - Updates playBuffer (audio buffer used by Minim audio library methods)
     *  - Propagates the buffer to active instruments: edit for your own instruments
     * 
     * This is the ONLY method that should mutate the global audio signal state.
     * 
     * In PixelAudio examples, the signal is typically loaded from a file, but
     * it could also be signal cached in memory, a signal generated by code, audio
     * captured live, etc. 
	 * 
	 * @param sig                 an audio signal
	 * @param bufferSampleRate    audio sample rate for sig, 
	 *                            usually obtained when reading an audio file
	 */
	void updateAudioChain(float[] sig, float bufferSampleRate) {
	    // 0) Decide target length (make this a single source of truth)
	    int targetSize = mapper.getSize();
	    if (targetSize <= 0) return;
	    // 1) Ensure playBuffer matches target
	    float[] canonical = new float[targetSize];
	    if (sig != null) {
	    	System.arraycopy(sig, 0, canonical, 0, Math.min(sig.length, targetSize));
	    }
	    // 3) Set audioSignal and other audio arrays
	    audioSignal = canonical;
	    audioLength = targetSize;
	    if (playBuffer == null || playBuffer.getBufferSize() != targetSize) {
	        playBuffer = new MultiChannelBuffer(targetSize, 1);
	    }
	    // 4) Set playBuffer
	    playBuffer.setChannel(0, canonical);
	    // 5) Propagate into synths (adjust to your actual API)
	    if (synth != null) synth.setBuffer(playBuffer, bufferSampleRate);
	}
	
	void updateAudioChain(float[] sig) {
		updateAudioChain(sig, audioOut.sampleRate());
	}


	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                 TIME/LOCATION/ACTION METHODS                   */
	/*                                                                */
	/*----------------------------------------------------------------*/

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


}