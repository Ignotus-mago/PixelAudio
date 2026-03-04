/*
 *  Copyright (c) 2024 - 2025 by Paul Hertz <ignotus@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

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
 * PixelAudio library for Processing.
 * 
 * Typical usage in Processing is to declare a global:
 * 
 *   PixelAudio pixelaudio;
 * 
 * and then initialize it in the setup method:
 * 
 *    public void setup() {
 *      // ... 
 *      pixelaudio = new PixelAudio(this);
 *      // ... more setup code
 *    }
 *  
 * The host PApplet can be obtained from PixelAudio by other classes. A number of useful static 
 * variables and methods are also included. 
 *
 * 
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
	
	public static Random rando(long seed) {
		rando = new Random(seed);
		return rando;
	}
	
	
	

}

