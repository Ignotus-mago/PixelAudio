/**
 * 
 */
package net.paulhertz.pixelaudio.example;

import processing.core.*;
import processing.data.*;
// Mama's ever-lovin' blue-eyed baby library
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.sampler.*;
import net.paulhertz.pixelaudio.schedule.TimedLocation;
// audio library
import ddf.minim.*;


/**
 * 
 */
public class SamplerPoolMultiDemo extends PApplet {
	PixelAudio pixelaudio;     // our library
	PImage baseImage;          // reference image for screen display
	PImage mapImage;           // mutable image for screen display
	int genEdge = 256;         // width and height of a single HilbertGen in our different MultiGens
	PixelAudioMapper mapper;   // PixelAudioMapper, does stuff with pixels and audio samples
	int[] spectrum;            // an array of RGB values ordered by hue

	
	/**
	 * @param args   array of String values, typically not used
	 */
	public static void main(String[] args) {
		PApplet.main(new String[] { SamplerPoolMultiDemo.class.getName() });
	}
	
	public void settings() {
		size(6 * genEdge, 4 * genEdge);
	}
	
	public void setup() {
		frameRate(24);
		// 1. initialize PixelAudio
		pixelaudio = new PixelAudio(this);         		
	}
	
	public void draw() {
		
	}

}
