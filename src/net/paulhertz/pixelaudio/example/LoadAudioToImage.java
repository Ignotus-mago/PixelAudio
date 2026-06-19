package net.paulhertz.pixelaudio.example;

import processing.core.*;
import java.util.Random;
import java.util.Arrays;
import java.io.File;

import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.sampler.*;
import ddf.minim.*;

/**
 * 
 * LoadAudioToImage shows how you can open audio files and transcode their image data to an image.
 * Once the image is loaded, you can click on it to play back the audio using a PASamplerInstrument. 
 * Hovering over the image and pressing the spacebar will also trigger an audio event.
 * The hightlightSample() method highlights the pixels that correspond to the audio signal 
 * that is played. The highlight changes the pixels and can change the audio, too: just press 
 * the 'w' key to transcode the image to an audio signal and write it to the PASamplerInstrument.
 * You can also load audio to individual RGB or HSB Hue and Brightness channels. To hear the 
 * results of loading to different channels, write the image to the audio signal ('w' key) and 
 * click in the image. 
 * 
 * This sketch also includes some common methods for adjusting the brightness and contrast of 
 * an image: gamma adjustment changes contrast up of down and histogram equalization stretches
 * the range of brightness values in an image. These operations will change the audio, too, if 
 * you write the image to the audio signal. The gamma operations both make the audio quieter, 
 * while the histogram stretch usually makes the audio louder.
 * 
 * 
 * Press 'o' or 'O' to open an audio file and load it to the signal and all channels of the image.
 * Press 'r' to open an audio file and load it to the signal and the RED channel of the image.
 * Press 'g' to open an audio file and load it to the signal and the GREEN channel of the image.
 * Press 'b' to open an audio file and load it to the signal and the BLUE channel of the image.
 * Press 'l' to open an audio file and load it to the signal and the HSB Brightness channel of the image.
 * Press 'h' to open an audio file and load it to the signal and the HSB Hue channel of the image.
 * Press 'k' to overlay the rainbow signal path on the image.
 * Press 'm' to equalize the histogram of the image.
 * Press '+' or '=' to apply gamma value  "gammaLighter" to the image.
 * Press '-' or '_' to apply gamma value  "gammaDarker" to the image.
 * Press 'w' to transcode the image HSB Brightness channel and write it to the signal.
 * Press 's' to save the current display image as a PNG file.
 * Press '?' to show Help Message in the console.
 * 
 */
public class LoadAudioToImage extends PApplet {
	/** PixelAudio library */
	PixelAudio pixelaudio;        // PixelAudio library for Processing
	HilbertGen hGen;              // PixelMapGen that produces signal paths along a Hilbert curve
	PixelAudioMapper mapper;      // a PixelAudioMapper to mediate between image and signal
	int mapSize;                  // length of mapImage.pixels[] and audioSignal
	PImage mapImage;              // offscreen image to which we write image and transcoded audio data
	int[] colors;                 // an array of spectral colors to fill the signal path 
	PixelAudioMapper.ChannelNames chan;    // a color channel in the RGB or HSB color space

	/** Minim audio library */
	Minim minim;                  // Minim audio library for Processing
	AudioOutput audioOut;         // audio output used by Minim
	MultiChannelBuffer playBuffer;    // an audio buffer used to load audio from a file to the PASamplerInstrument synth
	float sampleRate = 44100;     // sample rate for audioOut
	float[] audioSignal;          // audio data as an array of floats in the range (-1.0f..1.0f)
	int[] rgbSignal;              // pixel data ordered along the signal path
	int audioLength;              // length of audioSignal (should equal mapSize)

	// SampleInstrument setup
	int duration = 2000;          // milliseconds duration
	PASamplerInstrument synth;    // sampling instrument to play audio

	// ADSR and params
	ADSRParams adsr;              // an ADSR envelope that wraps Minim's ADSR class
	float maxAmplitude = 0.9f;
	float attackTime = 0.2f;
	float decayTime = 0.125f;
	float sustainLevel = 0.5f;
	float releaseTime = 0.5f;

	// audio file
	File audioFile;
	String audioFilePath;
	String audioFileName;
	String audioFileTag;

	// Java random number generator
	Random rando;
	// some colors
	int roig = 0xf64c2f;
	int groc = 0xf6e959;
	int blau = 0x5990e9;
	int vert = 0x7bb222;
	int taronja = 0xfea537;
	int violet = 0xb29de9;
	// array of colors, for random selection
	public int[] randColors = { blau, groc, roig, vert, violet, taronja };

	// interaction variables
	int pixelPos;                 // index into the mapImage.pixels[] array
	int samplePos;                // index into audioSignal
	int blendAlpha = 64;          // value for blending colors
	
	// histogram and gamma adjustments
	int histoHigh = 240;          // RGB value, upper bound for histogram stretch
	int histoLow = 32;            // RGB value, lower bound for histogram stretch
	float gammaLighter = 0.9f;    // 
	float gammaDarker = 1.2f;
	int[] gammaTable;


	public static void main(String args[]) {
		PApplet.main(new String[]{LoadAudioToImage.class.getName()});
	}

	public void settings() {
		size(1024, 1024);
	}

	public void setup() {
		initMapper();		// set up mapper and load mapImage with color wheel
		initAudio();		// set up audio 
		rando = new Random();
		chan = PixelAudioMapper.ChannelNames.L;
		String path = "/Users/paulhz/Documents/Processing/libraries/PixelAudio/examples/LoadAudioToImage/data";
		File audioSource = new File(path +"/youthorchestra.wav");
		fileSelected(audioSource);
		showHelp();
	}

	public void initMapper() {
		pixelaudio = new PixelAudio(this); 		// load the PixelAudio library
		hGen = new HilbertGen(width, height); 	// create a Hilbert curve that fills our display
		mapper = new PixelAudioMapper(hGen); 	// initialize mapper with the HIlbert curve generator
		mapSize = mapper.getSize();				// size of mapper's various arrays and of mapImage
		colors = getColors(); 					// create an array of colors
		mapImage = createImage(width, height, ARGB); // an image to use with mapper
		mapImage.loadPixels();
		mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
		mapImage.updatePixels();
	}

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

	public int[] getColors() {
		int[] colorWheel = new int[mapSize]; 			// an array for our colors
		pushStyle(); 									// save styles
		colorMode(HSB, colorWheel.length, 100, 100); 	// pop over to the HSB color space and give hue a very wide range
		for (int i = 0; i < colorWheel.length; i++) {
			colorWheel[i] = color(i, 40, 75); 			// fill our array with colors, gradually changing hue
		}
		popStyle(); 									// restore styles, including the default RGB color space
		return colorWheel;
	}

	public void draw() {
		image(mapImage, 0, 0);
	}

	public void keyPressed() {
		switch (key) {
		case ' ': // play audio for the point the mouse is currently over
			audioMouseClick(clipToWidth(mouseX), clipToHeight(mouseY));
			break;
		case 'o': // open an audio file and load it to the signal and all channels of the image
		case 'O':
			chan = PixelAudioMapper.ChannelNames.ALL;
			chooseFile();
			break;
		case 'r': // open an audio file and load it to the signal and the RED channel of the image
			chan = PixelAudioMapper.ChannelNames.R;
			chooseFile();
			break;
		case 'g': // open an audio file and load it to the signal and the GREEN channel of the image
			chan = PixelAudioMapper.ChannelNames.G;
			chooseFile();
			break;
		case 'b': // open an audio file and load it to the signal and the BLUE channel of the image
			chan = PixelAudioMapper.ChannelNames.B;
			chooseFile();
			break;
		case 'l': // open an audio file and load it to the signal and the HSB Brightness channel of the image
			chan = PixelAudioMapper.ChannelNames.L;
			chooseFile();
			break;
		case 'h': // open an audio file and load it to the signal and the HSB Hue channel of the image
			chan = PixelAudioMapper.ChannelNames.H;
			chooseFile();
			break;
		case 'k': // overlay the rainbow signal path on the image
			mapImage.loadPixels();
			applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
			mapImage.updatePixels();
			break;
		case 'c':
			break;
		case 'm': // equalize the histogram of the image
			mapImage.loadPixels();
			int[] bounds = getHistoBounds(mapImage.pixels);
			mapImage.pixels = histoStretch(mapImage.pixels, bounds[0], bounds[1]);
			mapImage.updatePixels();
			break;
		case '=': // apply gamma value  \"gammaLighter\" to the image
		case '+':
			setGamma(gammaLighter);
			mapImage.loadPixels();
			mapImage.pixels = adjustGamma(mapImage.pixels);
			mapImage.updatePixels();
			break;
		case '-': // apply gamma value  \"gammaDarker\" to the image
		case '_':
			setGamma(gammaDarker);
			mapImage.loadPixels();
			mapImage.pixels = adjustGamma(mapImage.pixels);
			mapImage.updatePixels();
			break;
		case 'w': // transcode the image HSB Brightness channel and write it to the signal
			writeImageToAudio();
			break;
		case 's': // save the current display image as a PNG file
			mapImage.save("pixelAudio.png");
			println("--- saved display image to pixelAudio.png");
			break;
		case '?': // show Help Message in the console
			showHelp();
			break;
		default:
			break;
		}
	}
	
	public void showHelp() {
		println(" * Press 'o' or 'O' to open an audio file and load it to the signal and all channels of the image.");
		println(" * Press 'r' to open an audio file and load it to the signal and the RED channel of the image.");
		println(" * Press 'g' to open an audio file and load it to the signal and the GREEN channel of the image.");
		println(" * Press 'b' to open an audio file and load it to the signal and the BLUE channel of the image.");
		println(" * Press 'l' to open an audio file and load it to the signal and the HSB Brightness channel of the image.");
		println(" * Press 'h' to open an audio file and load it to the signal and the HSB Hue channel of the image.");
		println(" * Press 'k' to overlay the rainbow signal path on the image.");
		println(" * Press 'm' to equalize the histogram of the image.");
		println(" * Press '+' or '=' to apply gamma value  \"gammaLighter\" to the image.");
		println(" * Press '-' or '_' to apply gamma value  \"gammaDarker\" to the image.");
		println(" * Press 'w' to transcode the image HSB Brightness channel and write it to the signal.");
		println(" * Press 's' to save the current display image as a PNG file.");
		println(" * Press '?' to show Help Message in the console.");
	}

	public void applyColor(int[] colorSource, int[] graySource, int[] lut) {
		float[] hsbPixel = new float[3];
		for (int i = 0; i < mapImage.pixels.length; i++) {
			graySource[i] = PixelAudioMapper.applyColor(colorSource[lut[i]], graySource[i], hsbPixel);
		}
	}

	public void mouseClicked() {
		int x = clipToWidth(mouseX);
		int y = clipToHeight(mouseY);
		audioMouseClick(x, y);
	}

	/**
	 * Respond to a mouse click with an audio event.
	 */
	public void audioMouseClick(int x, int y) {
		pixelPos = x + y * width;
		samplePos = mapper.lookupSignalPos(x, y);
		// println("----- sample position for "+ mouseX +", "+ mouseY +" is "+ samplePos);
		int varyDuration = calcSampleLen(duration);
		int sampleLength = playSample(samplePos, varyDuration, 0.9f);
		if (sampleLength > 0) {
			hightlightSample(samplePos, (int)(2 * sampleRate));
			int c = mapImage.get(x, y);
			String str = PixelAudioMapper.colorString(c);
			println("--- samplePos:", samplePos, "sampleLength:", sampleLength, "size:", width * height, "end:", samplePos + sampleLength + ", " + str);
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
	 * @return a length in samples with some Gaussian variation
	 */
	public int calcSampleLen(int durationMS) {
		float vary = 0; 
		// skip the fairly rare negative numbers
		while (vary <= 0) {
			vary = (float) gauss(1.0, 0.0625);
		}
		int len = (int)(abs((vary * durationMS) * sampleRate / 1000.0f));
		println("---- calcSampleLen result = "+ len +" samples at "+ sampleRate +" Hz sample rate");
		return len;
	}


	public void chooseFile() {
		selectInput("Choose an audio file: ", "fileSelected");
	}

	public void fileSelected(File selectedFile) {
		if (null != selectedFile) {
			String filePath = selectedFile.getAbsolutePath();
			String fileName = selectedFile.getName();
			String fileTag = fileName.substring(fileName.lastIndexOf('.') + 1);
			fileName = fileName.substring(0, fileName.lastIndexOf('.'));
			if (fileTag.equalsIgnoreCase("mp3") || fileTag.equalsIgnoreCase("wav") || fileTag.equalsIgnoreCase("aif")
					|| fileTag.equalsIgnoreCase("aiff")) {
				audioFile = selectedFile;
				audioFilePath = filePath;
				audioFileName = fileName;
				audioFileTag = fileTag;
				println("----- Selected file " + fileName + "." + fileTag + " at "
						+ filePath.substring(0, filePath.length() - fileName.length()));
				loadAudioFile(audioFile);
			} else {
				println("----- File is not a recognized audio format ending with \"mp3\", \"wav\", \"aif\", or \"aiff\".");
			}
		} else {
			println("----- No audio file was selected.");
		}
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
	  }
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
		// return the length of the sample
		return sampleCount;
	}

	public void hightlightSample(int pos, int length) {
		shuffle(randColors);
		int highColor = PixelAudioMapper.setAlpha(randColors[0], blendAlpha);
		// watch out for the end of the image pixels!
		if (pos + length > mapSize - 1) {
			length = mapSize - pos - 1;
		}
		int[] signalPathPixelSequence = mapper.pluckPixels(mapImage.pixels, pos, length);
		mapImage.loadPixels();
		for (int i = 0; i < length; i++) {
			int newColor = blendColor(mapImage.pixels[pos + i], highColor, BLEND);
			signalPathPixelSequence[i] = newColor;
		}
		mapper.plantPixels(signalPathPixelSequence, mapImage.pixels, pos, length);
		mapImage.updatePixels();
	}

	public void writeImageToAudio() {
		println("----- writing image to signal ");
		mapImage.loadPixels();
		audioSignal = new float[mapSize];
		mapper.mapImgToSig(mapImage.pixels, audioSignal);
		playBuffer.setBufferSize(mapSize);
		playBuffer.setChannel(0, audioSignal);
		if (playBuffer != null) println("--->> audioBuffer length channel 0 = "+ playBuffer.getChannel(0).length);
		synth.setBuffer(playBuffer);
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
	 * Shuffles an array of integers into random order. Implements Richard
	 * Durstenfeld's version of the Fisher-Yates algorithm, popularized by Donald
	 * Knuth. see http://en.wikipedia.org/wiki/Fisher-Yates_shuffle
	 * 
	 * @param intArray an array of {@code int}s, changed on exit
	 */
	public void shuffle(int[] intArray) {
		for (int lastPlace = intArray.length - 1; lastPlace > 0; lastPlace--) {
			// Choose a random location from 0..lastPlace
			int randLoc = (int) (random(lastPlace + 1));
			// Swap items in locations randLoc and lastPlace
			int temp = intArray[randLoc];
			intArray[randLoc] = intArray[lastPlace];
			intArray[lastPlace] = temp;
		}
	}
	
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
	public int[] histoStretch(int[] source, int low, int high) {
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
	
}
