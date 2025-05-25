package net.paulhertz.pixelaudio;


import processing.core.PApplet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * @author Paul Hertz
 *
 * (the tag example followed by the name of an example included in folder 'examples' will
 * automatically include the example in the javadoc.)
 *
 * @example Hello
 */

public class PixelAudio {
	/** myParent is a reference to the parent sketch, we make it static so it's available to other classes */
	public static PApplet myParent;
	/** Java Random */
	private static Random rando;
	/** SHould be set by Ant script (?), but that is not happening */
	public final static String VERSION = "##library.prettyVersion##";

	// audio sampling rates
	public static final int SR_96k = 96000;
	public static final int SR_48k = 48000;
	public static final int SR_44dot1k = 44100;
	public static final int SR_256x256 = 65536;
	public static final int SR_512x512 = 262144;
	public static final int SR_1024x1024 = 1048576;


	/**
	 * a Constructor, usually called in the setup() method in your sketch to
	 * initialize and start the Library.
	 *
	 * @example Hello
	 * @param theParent the parent PApplet
	 */
	public PixelAudio(PApplet theParent) {
		myParent = theParent;
		welcome();
	}


	private void welcome() {
		System.out.println("##library.name## ##library.prettyVersion## by ##author##");
	}


	public String sayHello() {
		return "Hello from PixelAudio.";
	}

	/**
	 * return the version of the Library.
	 *
	 * @return String
	 */
	public static String version() {
		return VERSION;
	}


	
	//-------------------------------------------//
	//           LERP, MAP, CONSTRAIN            //
	//-------------------------------------------//


	/**
	 * Processing's PApplet.constrain method, copied for convenience.
	 * @param 	amt
	 * @param 	low
	 * @param 	high
	 * @return 	amt clipped to low and high, closed interval
	 */
	static public final float constrain(float amt, float low, float high) {
		return (amt < low) ? low : ((amt > high) ? high : amt);
	}


	/**
	 * Processing's PApplet.constrain method, copied for convenience.
	 * @param 	amt
	 * @param 	low
	 * @param 	high
	 * @return 	amt clipped to low and high, closed interval
	 */
	static public final int constrain(int amt, int low, int high) {
		return (amt < low) ? low : ((amt > high) ? high : amt);
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


	
	//-------------------------------------------//
	//                RANDOM STUFF               //
	//-------------------------------------------//
	
	
	/**
	 * Returns a Gaussian variable using a Java library call to
	 * <code>Random.nextGaussian</code>.
	 * 
	 * @param mean
	 * @param variance
	 * @return a Gaussian-distributed random number with mean <code>mean</code> and
	 *         variance <code>variance</code>
	 */
	public static double gauss(double mean, double variance) {
		return rando().nextGaussian() * Math.sqrt(variance) + mean;
	}

	public static Random rando() {
		if (rando == null) {
			rando = new Random();
		}
		return rando;
	}
	
	
	
	//-------------------------------------------//
	//              AUDIO FILE I/O               //
	//-------------------------------------------//
	
	
	/**
	 * Saves audio data to 16-bit integer PCM format, which Processing can also open.
	 * 
	 * @param samples			an array of floats in the audio range (-1.0f, 1.0f)
	 * @param sampleRate		audio sample rate for the file
	 * @param fileName			name of the file to save to
	 * @throws IOException		an Exception you'll need to handle to call this method (see keyPressed entry for 's')
	 * @throws UnsupportedAudioFileException		another Exception (see keyPressed entry for 's')
	 */
	public static void saveAudioToFile(float[] samples, float sampleRate, String fileName)
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
	
	/**
	 * Saves audio data to a 32-bit floating point format that has higher resolution than 16-bit integer PCM. 
	 * The format can't be opened by Processing but can be opened by audio applications. 
	 * 
	 * @param samples			an array of floats in the audio range (-1.0f, 1.0f)
	 * @param sampleRate		the sample rate for the file
	 * @param fileName			name of the file
	 * @throws IOException		an Exception you'll need to catch to call this method (see keyPressed entry for 's')
	 */
	public static void saveAudioTo32BitPCMFile(float[] samples, float sampleRate, String fileName) throws IOException {
		// convert samples to 32-bit PCM float
		byte[] audioBytes = new byte[samples.length * 4];
		int index = 0;
		// convert to IEEE 754 floating-point "single format" bit layout 
		for (float sample : samples) {
			int intBits = Float.floatToIntBits(sample);
			audioBytes[index++] = (byte) (intBits & 0xFF);
			audioBytes[index++] = (byte) ((intBits >> 8) & 0xFF);
			audioBytes[index++] = (byte) ((intBits >> 16) & 0xFF);
			audioBytes[index++] = (byte) ((intBits >> 24) & 0xFF);
		}
		ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
		AudioFormat format = new AudioFormat(sampleRate, 32, 1, true, false);
        AudioInputStream audioInputStream = new AudioInputStream(byteStream, format, samples.length);
        File outFile = new File(fileName);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outFile);      
	}

	/**
	 * Saves stereo audio data to 16-bit integer PCM format, which Processing can also open.
	 * 
	 * @param leftChannel		an array of floats in the audio range (-1.0f, 1.0f)
	 * @param rightChannel		an array of floats in the audio range (-1.0f, 1.0f)
	 * @param sampleRate		audio sample rate for the file
	 * @param fileName			name of the file to save to
	 * @throws IOException		an Exception you'll need to handle to call this method (see keyPressed entry for 's')
	 * @throws UnsupportedAudioFileException		another Exception (see keyPressed entry for 's')
	 */
	public static void saveStereoAudioToFile(float[] leftChannel, float[] rightChannel, float sampleRate, String fileName)
	        throws IOException, UnsupportedAudioFileException {
        int numSamples = leftChannel.length;
	    // Convert samples from float to 16-bit PCM
	    byte[] audioBytes = new byte[leftChannel.length * 2 * 2];
	    int index = 0;
	    for (int i = 0; i < numSamples; i++) {
	        // Scale leftChannel sample to 16-bit signed integer
	        int intSample = (int) (leftChannel[i] * 32767);
	        // Convert to bytes
	        audioBytes[index++] = (byte) (intSample & 0xFF);
	        audioBytes[index++] = (byte) ((intSample >> 8) & 0xFF);
	        // Scale rightChannel sample to 16-bit signed integer
	        intSample = (int) (rightChannel[i] * 32767);
	        // Convert to bytes
	        audioBytes[index++] = (byte) (intSample & 0xFF);
	        audioBytes[index++] = (byte) ((intSample >> 8) & 0xFF);
	        
	    }
	    // Create an AudioInputStream
	    ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
	    AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
	    AudioInputStream audioInputStream = new AudioInputStream(byteStream, format, numSamples);
	    // Save the AudioInputStream to a WAV file
	    File outFile = new File(fileName);
	    AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outFile);
	}
	
	/**
	 * Saves stereo audio data to a 32-bit floating point format that has higher resolution than 16-bit integer PCM. 
	 * The format can't be opened by Processing but can be opened by audio applications. 
	 * 
	 * @param leftChannel		an array of floats in the audio range (-1.0f, 1.0f)
	 * @param rightChannel		an array of floats in the audio range (-1.0f, 1.0f)
	 * @param sampleRate		the sample rate for the file
	 * @param fileName			name of the file
	 * @throws IOException		an Exception you'll need to handle when calling this method
	 */
	public static void saveStereoAudioTo32BitPCMFile(float[] leftChannel, float[] rightChannel, float sampleRate, String fileName) throws IOException {
        int numSamples = leftChannel.length;
		// convert leftChannel to 32-bit PCM float
		byte[] audioBytes = new byte[numSamples * 2 * 4];
		int index = 0;
		// convert to IEEE 754 floating-point "single format" bit layout 
		for (int i = 0; i < numSamples; i++) {
           // Left channel sample
            int intBits = Float.floatToIntBits(leftChannel[i]);
            audioBytes[index++] = (byte) (intBits & 0xFF);
            audioBytes[index++] = (byte) ((intBits >> 8) & 0xFF);
            audioBytes[index++] = (byte) ((intBits >> 16) & 0xFF);
            audioBytes[index++] = (byte) ((intBits >> 24) & 0xFF);
            // Right channel sample
            intBits = Float.floatToIntBits(rightChannel[i]);
            audioBytes[index++] = (byte) (intBits & 0xFF);
            audioBytes[index++] = (byte) ((intBits >> 8) & 0xFF);
            audioBytes[index++] = (byte) ((intBits >> 16) & 0xFF);
            audioBytes[index++] = (byte) ((intBits >> 24) & 0xFF);
		}
        // create an AudioInputStream
		ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
		AudioFormat format = new AudioFormat(sampleRate, 32, 2, true, false);
        AudioInputStream audioInputStream = new AudioInputStream(byteStream, format, numSamples);
        // write the file 
        File outFile = new File(fileName);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outFile);      
	}


}

