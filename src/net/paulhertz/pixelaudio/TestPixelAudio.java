package net.paulhertz.pixelaudio;

import processing.core.*;
import java.util.Random;
import java.util.Arrays;
import java.io.File;

import ddf.minim.*;
import ddf.minim.ugens.*;

public class TestPixelAudio extends PApplet {
	/** PixelAudio library */
	PixelAudio pixelaudio;
	HilbertGen hGen;
	PixelAudioMapper mapper;
	int mapSize;
	PImage mapImage;
	int[] colors;
	PixelAudioMapper.ChannelNames chan = PixelAudioMapper.ChannelNames.L;

	/** Minim audio library */
	Minim minim;
	AudioOutput audioOut;
	MultiChannelBuffer audioBuffer;
	int sampleRate = 44100;
	float[] audioSignal;
	int[] rgbSignal;
	int audioLength;

	// SampleInstrument setup
	float sampleScale = 2;
	int sampleBase = 10250;
	int samplelen = (int) (sampleScale * sampleBase);
	Sampler audioSampler;
	SamplerInstrument instrument;

	// ADSR and params
	ADSR adsr;
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

	// Java random number generator
	Random rando;
	int roig = 0xf64c2f;
	int groc = 0xf6e959;
	int blau = 0x5990e9;
	int blau2 = 0x90b2dc;
	int vert = 0x7bb222;
	int taronja = 0xfea537;
	int roigtar = 0xE9907B;
	int violet = 0xb29de9;
	// array of colors, usually for random selection
	public int[] randColors = { blau, groc, roig, vert, violet, taronja };

	// interaction
	int pixelPos;
	int samplePos;
	int blendAlpha = 64;

	public static void main(String args[]) {
		PApplet.main(new String[] { "net.paulhertz.pixelaudio.TestPixelAudio" });
	}

	public void settings() {
		size(1024, 1024);
	}

	public void setup() {
		initMapper();
		initAudio();
		rando = new Random();
	}

	public void initMapper() {
		pixelaudio = new PixelAudio(this); // load the PixelAudio library
		hGen = new HilbertGen(width, height); // create a Hilbert curve that fills our display
		mapper = new PixelAudioMapper(hGen); // initialize mapper with the HIlbert curve generator
		mapSize = mapper.getSize(); // size of mapper's various arrays and of mapImage
		colors = getColors(); // create an array of colors
		mapImage = createImage(width, height, ARGB); // an image to use with mapper
		mapImage.loadPixels();
		mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load the colors to mapImage following the Hilbert
																	// curve (the "signal path" for hGen)
		mapImage.updatePixels();
	}

	public void initAudio() {
		this.minim = new Minim(this);
		// use the getLineOut method of the Minim object to get an AudioOutput object
		this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
		this.audioBuffer = new MultiChannelBuffer(1024, 1);
	}

	public int[] getColors() {
		int[] colorWheel = new int[mapSize]; // an array for our colors
		pushStyle(); // save styles
		colorMode(HSB, colorWheel.length, 100, 100); // pop over to the HSB color space and give hue a very wide range
		for (int i = 0; i < colorWheel.length; i++) {
			colorWheel[i] = color(i, 66, 66); // fill our array with colors of a gradually changing hue
		}
		popStyle(); // restore styles, including the default RGB color space
		return colorWheel;
	}

	public void draw() {
		image(mapImage, 0, 0);
	}

	public void keyPressed() {
		switch (key) {
		case 'o':
		case 'O':
			chan = PixelAudioMapper.ChannelNames.ALL;
			chooseFile();
			break;
		case 'r':
			chan = PixelAudioMapper.ChannelNames.R;
			chooseFile();
			break;
		case 'g':
			chan = PixelAudioMapper.ChannelNames.G;
			chooseFile();
			break;
		case 'b':
			chan = PixelAudioMapper.ChannelNames.B;
			chooseFile();
			break;
		case 'l':
			chan = PixelAudioMapper.ChannelNames.L;
			chooseFile();
			break;
		case 'h':
			chan = PixelAudioMapper.ChannelNames.H;
			chooseFile();
			break;
		case 'w':
			// writeImageToAudio();
			break;
		case 's':
			mapImage.save("pixelAudio.png");
			println("--- saved image pixelAudio.png");
			break;
		case '?':
			// showHelp();
			break;
		default:
			break;
		}
	}

	public void mousePressed() {
		pixelPos = mouseX + mouseY * width;
		samplePos = mapper.lookupSample(mouseX, mouseY);
		int c = mapImage.get(mouseX, mouseY);
		String str = PixelAudioMapper.colorString(c);
		// println("----- sample position for "+ mouseX +", "+ mouseY +" is "+
		// samplePos);
		int sampleLength = playSample(samplePos);
		if (sampleLength > 0) {
			hightlightSample(samplePos, sampleLength);
			println("--- samplePos:", samplePos, "sampleLength:", sampleLength, "size:", width * height, "end:",
					samplePos + sampleLength + ", " + str);
		}
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
			println("----- No audio or image file was selected.");
		}
	}

	public void loadAudioFile(File audioFile) {
		float sampleRate = minim.loadFileIntoBuffer(audioFile.getAbsolutePath(), audioBuffer); // read audio file into
																								// our
																								// MultiChannelBuffer
		if (sampleRate > 0) { // sampleRate > 0 means we read audio from the file
			this.audioSignal = audioBuffer.getChannel(0); // read an array of floats from the buffer
			this.audioLength = audioSignal.length; //
			rgbSignal = new int[mapSize]; // create an array the size of mapImage
			rgbSignal = mapper.pluckSamplesAsRGB(audioSignal, 0, mapSize);
			if (rgbSignal.length < mapSize) {
				rgbSignal = Arrays.copyOf(rgbSignal, mapSize); // pad rgbSignal with 0's
			}
			mapImage.loadPixels();
			mapper.plantPixels(rgbSignal, mapImage.pixels, 0, rgbSignal.length, chan);
			mapImage.updatePixels();
		}
	}

	public int playSample(int samplePos) {
		if (audioFile == null)
			return 0;
		audioSampler = new Sampler(audioBuffer, 44100, 8); // create a Minim Sampler from the buffer with 44.1 sampling
															// rate, for up to 8 simultaneous outputs
		audioSampler.amplitude.setLastValue(0.9f); // set amplitude for the Sampler
		audioSampler.begin.setLastValue(samplePos); // set the Sampler to begin playback at samplePos, which corresponds
													// to the place the mouse was clicked
		int releaseDuration = (int) (releaseTime * sampleRate); // do some calculation to include the release time.
																// There may be better ways to do this.
		float vary = (float) (gauss(this.sampleScale, this.sampleScale * 0.125f)); // vary the duration of the signal
																					// using a statistical distribution
																					// function
		// println("----->>> vary = "+ vary +", sampleScale = "+ sampleScale);
		this.samplelen = (int) (vary * this.sampleBase); // calculate the duration of the sample
		if (samplePos + samplelen >= mapSize) {
			samplelen = mapSize - samplePos; // make sure we don't exceed the mapSize
			println("----->>> sample length = " + samplelen);
		}
		int durationPlusRelease = this.samplelen + releaseDuration;
		int end = (samplePos + durationPlusRelease >= this.mapSize) ? this.mapSize - 1
				: samplePos + durationPlusRelease;
		println("----->>> end = " + end);
		audioSampler.end.setLastValue(end);
		// ADSR envelope with maximum amplitude, attack Time, decay time, sustain level,
		// and release time
		adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
		this.instrument = new SamplerInstrument(audioSampler, adsr);
		// play command takes a duration in seconds
		instrument.play(samplelen / (float) (sampleRate));
		// return the length of the sample
		return samplelen;
	}

	public void hightlightSample(int pos, int length) {
		shuffle(randColors);
		int highColor = PixelAudioMapper.setAlpha(randColors[0], blendAlpha);
		int[] signalPathPixelSequence = mapper.pluckPixels(mapImage.pixels, pos, samplelen);
		mapImage.loadPixels();
		for (int i = 0; i < length; i++) {
			int newColor = blendColor(mapImage.pixels[pos + i], highColor, BLEND);
			signalPathPixelSequence[i] = newColor;
		}
		mapper.plantPixels(signalPathPixelSequence, mapImage.pixels, pos, length);
		mapImage.updatePixels();
	}

	/**
	 * Returns a Gaussian variable using a Java library call to
	 * <code>Random.nextGaussian</code>.
	 * 
	 * @param mean
	 * @param variance
	 * @return a Gaussian-distributed random number with mean <code>mean</code> and
	 *         variance <code>variance</code>
	 */
	public double gauss(double mean, double variance) {
		return rando.nextGaussian() * Math.sqrt(variance) + mean;
	}

	/**
	 * Shuffles an array of integers into random order. Implements Richard
	 * Durstenfeld's version of the Fisher-Yates algorithm, popularized by Donald
	 * Knuth. see http://en.wikipedia.org/wiki/Fisher-Yates_shuffle
	 * 
	 * @param intArray an array of <code>int</code>s, changed on exit
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

	// using minim's Instrument interface
	class SamplerInstrument implements Instrument {
		Sampler sampler;
		ADSR adsr;

		SamplerInstrument(Sampler sampler, ADSR adsr) {
			this.sampler = sampler;
			this.adsr = adsr;
			sampler.patch(adsr);
		}

		public void play() {
			// Trigger the ADSR envelope by calling noteOn()
			// Duration of 0.0 means the note is sustained indefinitely
			noteOn(0.0f);
		}

		public void play(float duration) {
			// Trigger the ADSR envelope by calling noteOn()
			// Duration of 0.0 means the note is sustained indefinitely
			// Duration should be in seconds
			// println("----->>> SamplerInstrument.play("+ duration +")");
			noteOn(duration);
		}

		@Override
		public void noteOn(float duration) {
			// Trigger the ADSR envelope and sampler
			adsr.noteOn();
			sampler.trigger();
			adsr.patch(audioOut);
			if (duration > 0) {
				// println("----->>> duration > 0");
				int durationMillis = (int) (duration * 1000);
				// schedule noteOff with an anonymous Timer and TimerTask
				new java.util.Timer().schedule(new java.util.TimerTask() {
					public void run() {
						noteOff();
					}
				}, durationMillis);
			}
		}

		@Override
		public void noteOff() {
			// println("----->>> noteOff event");
			adsr.unpatchAfterRelease(audioOut);
			adsr.noteOff();
		}

		// Getter for the Sampler instance
		public Sampler getSampler() {
			return sampler;
		}

		// Setter for the Sampler instance
		public void setSampler(Sampler sampler) {
			this.sampler = sampler;
		}

		// Getter for the ADSR instance
		public ADSR getADSR() {
			return adsr;
		}

		// Setter for the ADSR instance
		public void setADSR(ADSR adsr) {
			this.adsr = adsr;
		}
	}

}
