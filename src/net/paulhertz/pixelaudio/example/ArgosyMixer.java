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

package net.paulhertz.pixelaudio.example;

import processing.core.*;

//G4P library for GUI
import g4p_controls.*;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.sound.sampled.UnsupportedAudioFileException;

//video export library
import com.hamoid.*;

//Mama's ever-lovin' blue-eyed PixelAudio library
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;
import net.paulhertz.pixelaudio.sampler.*;
import net.paulhertz.pixelaudio.schedule.AudioUtility;
import net.paulhertz.pixelaudio.schedule.TimedLocation;

//audio library
import ddf.minim.*;

/**
 * 
 * ArgosyMixer demonstrates use of the Argosy class to create and animate patterns 
 * and save them to video. This is still a work in progress, with the new PASamplerInstrument 
 * class and audio events not yet handled in the most efficient way. There will be updates.
 * <P>
 * The Argosy class turns arrays of integers into color patterns. It steps through the Pattern
 * array to create blocks of pixels and assigns color to the blocks as it steps through the 
 * Colors arrays. The arrays don't have to be the same size--this creates variations in the 
 * pattern color sequences. For example, we might have blocks that are [5, 3, 8] units and 
 * assign them just two colors, say black and white:
 * </p>
 * <pre>
 *    |    5     |  3   |       8        |    5     |  3   |       8        |
 *    |    B     |  W   |       B        |    W     |  B   |       W        |
 * </pre>
 * <p>
 * As you can see, the two patterns together generate a larger repeating unit. If the "Repeat" 
 * filed in the GUI is set to 0, the patterns fill the Argosy array, which is the same size as 
 * the pixels[] array for the display image. A non-zero Repeat value determines how many times  
 * the pattern repeats. The way the Argosy array fills the display image is determined by the 
 * Map parameter. The map is a PixelAudioMapper determined by a PixelMapGen: basically, it 
 * creates a path (the "signal path") that visits every pixel in the display image once. 
 * Experiment with the GUI Map menu to find out more about how paths work. Read PixelAudioMapper 
 * and PixelMapGen documentation if you want detailed information. You can also browse the code
 * for quite a few MultiGen PixelMapGens in this sketch and in the static methods appended to
 * PixelAudio PixelMapGen classes. 
 * </p><p>
 * ArgosyMixer provides a GUI for modifying argosy and animation
 * parameters, plus a series of key commands that can shift patterns along
 * the signal path. There are two Argosy patterns involved: the top one, 
 * Argosy 2, is transparent (Opacity = 127, initially). 
 * </p><p>
 * In the GUI, the following parameters are exposed for each Argosy pattern:
 * </p>
 * <pre>
 *   Map          -- select the PixelMapGen for each Argosy instance
 *   Colors       -- select a preset palette from a drop down list
 *   Opacity      -- opacity of the colors in the palette, 0-255
 *   Pattern      -- select a preset numeric pattern from a drop down list
 *   Repeat       -- number of times to repeat the pattern; enter 0 for maximum repetitions 
 *   Unit         -- the number of pixels in each unit of the pattern
 *   Gap          -- the number of pixels between each repeated pattern
 *   Gap color    -- select a preset gap color from a drop down list
 *   Gap opacity  -- enter the alpha channel value 0-255 for the gap color
 *   == ANIMATION ==
 *   Show         -- show or hide Argosy 1 or Argosy 2
 *   Freeze       -- freeze animation of Argosy 1 or Argsoy 2
 *   Step         -- number of pixels to shift on each animation step (negative to shift right)
 *   Open frames  -- number of frames to hold at animation start, applies both Argosy 1 and Argosy 2
 *   Close frames -- number of frames to hold at animation end, applies to both Argsoy 1 and Argosy 2
 *   Run frames   -- number of frames to animate before a hold, sets Argosy 1 and Argosy 2 separately
 *   Hold frames  -- number of frames to hold after a run of frames, sets Argosy 1 and Argosy 2 separately
 *   Duration     -- number of frames in the animation
 *   Record Video -- press to run and record animation from current display 
 * </pre>  
 * <p>  
 * I suggest you start by experimenting with the patterns "The One" and "One-one". They create
 * repeating patterns of one or two elements. Setting the Unit value (the number of pixels
 * in each pattern element) to a power of 2 or a sum of powers of 2 is a good place to start, 
 * especially with the Hilbert PixelMapGens in the Map menu. 
 * </p><p>
 * Click on the image to hear the sounds made by the patterns with sampling rate 48KHz. The patterns
 * produce step or pulse (square) waves, so they are buzzy. Opacity will change how loud the sound is.
 * </p><p>
 * You can create stereo drones with the 'e' command, which creates a series of audio events along 
 * the points of an ellipse. 
 * </p><p>
 * Press the spacebar to start or stop animation. 
 *   
 * <pre>  
 * --------------------------------------------------------------------------------------------
 * ***++ NOTE: Key commands only work when the image display window is the active window. ++***
 * --------------------------------------------------------------------------------------------
 * 
 * Key Commands
 * 
 * Press ' ' to toggle animation.
 * Press 'e' to trigger elliptical trail of audio events.
 * Press 'a' to shift left one argosy unit.
 * Press 'A' to shift right one argosy unit.
 * Press 'b' to shift left one argosy length.
 * Press 'B' to shift right one argosy length.
 * Press 'c' to shift left one argosy length + argosy gap.
 * Press 'C' to shift right one argosy length + argosy gap.
 * Press 'd' to advance one animation step.
 * Press 'D' to go back one animation step.
 * Press 'g' or 'G' to set the pixelShift of argosies to zero (reset return point).
 * Press 'l' to shift argosies left one animation step.
 * Press 'L' to shift argosies left one animation step.
 * Press 'r' to shift argosies right one animation step.
 * Press 'R' to shift argosies right one animation step.
 * Press 'p' to shift argosies left one pixel.
 * Press 'P' to shift argosies right one pixel.
 * Press 'f' to freeze changes to argosy 1.
 * Press 'F' to freeze changes to argosy 2.
 * Press 'i' or 'I' to show stats about argosies.
 * Press 'S' to save current display to an PNG file.
 * Press 's' to save current display to an PNG file.
 * Press 'u' or 'U' to reinitialize any argosies that aren't frozen.
 * Press 'v' or 'V' to toggle video recording.
 * Press 'w' to reset animation tracking.
 * Press 'W' to reset animation tracking.
 * Press 'z' to reset argosy 1 to initial position.
 * Press 'Z' to reset argosy 2 to inttial position.
 * Press 'h' or 'H' to show help message in console.
 * 
 * </pre>
 * 
 * TODO save two image files, two audio files -- one for each Argosy<br>
 * TODO bug fix, when changing gap color argosy resets with 0 shift<br>
 * TODO reset animation with a key command 
 * 
 */
public class ArgosyMixer extends PApplet {
	/* ------------------------------------------------------------------ */
	/*                          PAPPLET SETTINGS                          */
	/* ------------------------------------------------------------------ */
	int imageWidth = 1536;
	int imageHeight = 1024;
	int bgColor = color(0, 0, 0);
	
	/* ------------------------------------------------------------------ */
	/*                       PIXELAUDIO VARIABLES                         */
	/* ------------------------------------------------------------------ */
	PixelAudio pixelaudio;
	int mapSize;
	PImage mapImage;
	PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;
	// short names for transforms from
	// public enum AffineTransformType {ROT90CW, ROT90CCW, ROT180, FLIPX, FLIPX90, FLIPX90CCW, FLIPY, NADA};
	// also used in PixelMapGen
	AffineTransformType     r270      = AffineTransformType.R270;
	AffineTransformType     r90       = AffineTransformType.R90;
	AffineTransformType     r180      = AffineTransformType.R180;
	AffineTransformType     flipx     = AffineTransformType.FLIPX;
	AffineTransformType     fx270     = AffineTransformType.FX270;
	AffineTransformType     fx90      = AffineTransformType.FX90;
	AffineTransformType     flipy     = AffineTransformType.FLIPY;
	AffineTransformType     nada      = AffineTransformType.NADA;
	// transArray is useful for random selections
	AffineTransformType[]   transArray = {r270, r90, r180, flipx, fx270, fx90, flipy, nada}; 
	Random rand;
	
	/* ------------------------------------------------------------------ */
	/*                         ARGOSY VARIABLES                           */
	/* ------------------------------------------------------------------ */
	int roig = 0xfff64c2f; int groc = 0xfff6e959; int blau = 0xff5990e9; int blau2 = 0xff90b2dc; 
	int blanc = 0xfffef6e9; int gris = 0xffa09faa; int negre = 0xff080d15; int grana = 0xffe56ad8;
	int vert = 0xff7bb222; int taronja = 0xfffea537; int roigtar = 0xffE9907B; int violet = 0xffb29de9;
	// Standard black, gray, white colors without transparency
	int black = color(0, 0, 0);
	int white = color(255, 255, 255);
	int gray = color(128, 128, 128);
	// Argosy pattern variables
	int[] theOne = new int[]{1};
	int[] oneOne = new int[]{1,1};
	int[] countToFive = {1, 2, 3, 4, 5}; 	// use unit = gap = 64: (1 + 2 + 3 + 4 + 5) * 64 + 64 = 1024
	int[] oddOneToSeven = {1, 3, 5, 7}; 	// first four odd numbers sum to 16
	int[] fourPower = {16, 64, 256}; 	    // use unit = 16: (16 + 64 + 256) = 336; 336 + 48 = 384; gap = 16 * 48 = 768
	int[] sevenFortyNine = {7, 49};         // a gap of 8 * unit will repeat patterns in Hilbert gens
	int[] fiboLSystem55 = new int[]{ 2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2,
	          1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2,
	          1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2 };
			  // use unit = 64, gap = 448: sum(fiboLSystem55) = 89; 89 * 64 + 448 = 6144 = 6 * 1024
	int[] fibonacciNums = {21, 34, 55, 13};     // sum to 123; 128 - 123 = 5; 5 * 64 = 320;
	int[] lucasNums = {18, 29, 47, 29};         // sum TO 123; 128 - 123 = 5; 5 * 64 = 320;
	// Argosy color palette variables
	int[] blackAlone = {black};
	int[] blackWhite = {black, white};
	int[] whiteBlack = {white, black};
	int[] blackGrayWhite = {black, gray, white};
	int[] grayRamp = {color(16), color(48), color(80), color(112), color(144), color(176), color(208), color(240)};
	int[] grayTriangle = {color(12), color(41), color(70), color(99), color(128), color(157), color(186), color(215), 
			              color(244), color(215), color(186), color(157), color(128), color(99), color(70), color(41)};
	int[] espectroOcho = {roig, roigtar, taronja, groc, vert, blau, violet, grana};
	int[] espectroSeis = {roig, taronja, groc, vert, blau, violet};
	int[] totsElsColors = {roig, roigtar, taronja, groc, vert, blau, blau2, violet, grana, blanc, gris, negre};
	int[] multicolor = {roig, blau, groc, vert, violet}; 
	int[] blueCream = {color(144, 157, 186), color(233, 220, 199)};
	int[] creamBlue = {color(233, 220, 199), color(144, 157, 186)};
	int[] fourColor = {color(204, 212, 164), color(81, 121, 127), color(210, 202, 250), color(136, 97, 90)};
	int[] fiveColor = {color(206, 166, 237), color(224, 99, 128), color(161, 156, 6), color(60, 186, 81), color(150, 199, 227)};
	// variables for first Argosy instance
	// set argo1, argo2 and animation values in initArgosies()
	Argosy argo1;
	PixelMapGen argo1Gen;
	PixelAudioMapper argo1Mapper;
	int[] argo1Pattern;
	int[] argo1Colors;
	int argo1Alpha;
	int argo1Reps;
	int argo1Unit;
	int argo1Gap;
	int argo1GapColor;
	int argo1GapColorIndex = 0;
	int argo1GapAlpha;
	boolean argo1IsCentered;
	PImage argo1Image;
	boolean isShowArgo1 = true;
	Argosy argo2;
	// variables for second Argosy instance
	PixelMapGen argo2Gen;
	PixelAudioMapper argo2Mapper;
	int[] argo2Pattern;
	int[] argo2Colors;
	int argo2Alpha;
	int argo2Reps;
	int argo2Unit;
	int argo2Gap;
	int argo2GapColor;
	int argo2GapColorIndex = 0;
	int argo2GapAlpha;
	boolean argo2IsCentered;
	PImage argo2Image;
	boolean isShowArgo2 = true;
	
	/* ------------------------------------------------------------------ */
	/*                     ARGOSY ANIMATION VARIABLES                     */
	/* ------------------------------------------------------------------ */
	
	int argo1Step;
	int argo2Step;
	int animOpen;
	int animClose;
	int animRun1;
	int animHold1;
	int animRun2;
	int animHold2;
	int animDuration; 
	// animation shift left or right, 'l' and 'r' key commands, usually a power of 4
	int argoStep = 64;
	// animation tracking variables, set here
	boolean isAnimating = false;     // start with animation off
	boolean isLooping = false;       // currently not used
	int animstep1 = 1;               // count steps of animation for argo1
	int runCount1 = 0;               // track animRun1 
	int holdCount1 = 0;              // track animHold1
	int animstep2 = 1;               // count steps of animation for argo2
	int runCount2 = 0;               // track animRun2
	int holdCount2 = 0;              // track animHold2
	boolean isArgo1Freeze = false;   // ready to animate argo1
	boolean isArgo2Freeze = false;   // ready to animate argo2
	int argo1PixelCount;             // keep track of pixels shifted by animation of argo1
	int argo2PixelCount;             // keep track of pixels shifted by animation of argo2
	// video output variables
	// video export 
	VideoExport videx;         // hamoid library class for video export (requires ffmpeg)
	String videoPath;          // directory for saving video
	String videoFilename = "argosy_demo.mp4";  // default video name
	int videoFrameRate = 24;   // frame rate for output video
	int currentFrame = 0;
	boolean isRecordingVideo = false;
	// image export
	boolean isSavePatterns = false;   // for future use

	/* ------------------------------------------------------------------ */
	/*                           AUDIO VARIABLES                          */
	/* ------------------------------------------------------------------ */
	Minim minim;					// library that handles audio 
	AudioOutput audioOut;			// line out to sound hardware
	boolean isBufferStale = false;	// flags that audioBuffer needs to be reset
	float sampleRate = 48000;       // a critical value for display and audio, see the setup method
	int audioLength;				// length of the audioSignal == number of pixels in the display image == mapSize
	int noteDuration = 2000;        // average sample synth note duration, milliseconds
	int samplelen;                  // calculated sample synth note length, samples
	float pitchScaling = 1.0f;      // scaling factor for pitch
	float panning = 0.0f;           // location within stereo field, left = -1.0, center = 0.0, right = 1.0
	float outputGain = -12.0f;      // audioOutput gain, decibels - set to -12.0dB because argosy patterns are noisy

	// ADSR and its parameters
	ADSRParams adsr1;               // envelope for argo1Synth, set in initAudio()
	ADSRParams adsr2;               // envelope for argo2Synth, set in initAudio()
	
	// interaction variables for audio
	int sampleX;
	int sampleY;
	ArrayList<TimedLocation> timeLocsArray;
	ArrayList<TimedLocation> animationLocsArray;
	ArrayList<PVector> aniPoints;
	int count = 0;	
	int fileIndex = 0;
	
	// ** LOCAL AUDIO VARIABLES ** Audio variables for ArgosyMixer class
	MultiChannelBuffer argo1Buffer;	// data structure to hold audio samples from argo1
	MultiChannelBuffer argo2Buffer;	// data structure to hold audio samples from argo2
	PASamplerInstrument argo1Synth;	// class to wrap a Minim audioSampler
	PASamplerInstrument argo2Synth;	// class to wrap a Minim audioSampler
	float[] argo1Signal;            // audio signal (float array) for argo1
	float[] argo2Signal;            // audio signal (float array) for argo2
	int argo1SamplePos;             // position of a mouse click along the argo1 signal path, index into the argo1 audio array
	int argo2SamplePos;             // position of a mouse click along the argo2 signal path, index into the argo2 audio array

	
	/* ---------------- end audio variables ---------------- */
	
	

	public static void main(String[] args) {
		// PApplet.main(new String[] { "--display=1", "--present", ArgosyMixer.class.getName() });
		PApplet.main(new String[] {  ArgosyMixer.class.getName() });
	}
	
	public void settings() {
		size(imageWidth, imageHeight, JAVA2D);
	}
	
	public void setup() {
		frameRate(videoFrameRate);
		pixelaudio = new PixelAudio(this);
		rand = new Random();
		argo1Mapper = selectMapper(argo1GenSelect, argo1Gen);
		argo2Mapper = selectMapper(argo2GenSelect, argo2Gen);
		mapSize = argo1Mapper.getSize();
		mapImage = createImage(width, height, ARGB);
		argo1Image = createImage(width, height, ARGB);
		argo2Image = createImage(width, height, ARGB);
		initArgosies();
		createGUI();
		initAudio();
		showHelp();
		bgColor = gray;
		isBufferStale = true;
	}
	
	// -- SELECTORS FOR MENU ITEMS -- //
	// if you want the initial menus to match your settings for argo1 and argo2, 
	// set them here or in initArgosies()
	int argo1GenSelect;
	int argo2GenSelect;
	int argo1PaletteSelect;
	int argo2PaletteSelect;
	int argo1PatternSelect;
	int argo2PatternSelect;

	/**
	 * Initializes argo1 and argo2 Argosy instances, sets some values for GUI, 
	 * sets animation variables: your one-stop setup method for the argosies. 
	 *  
	 */
	public void initArgosies() {
	    // first Argosy instance
	    argo1GenSelect = 17;          // menu item "Hilbert Random three"
	    argo1Alpha = 255;
	    argo1Colors = setArgoColorsAlpha(blackWhite, argo1Alpha);
	    argo1PaletteSelect = 1;      // menu item "Black, White"
	    argo1Pattern = countToFive;
	    argo1PatternSelect = 2;      // menu item "Count to Five"
	    argo1Reps = 0;
	    argo1Unit = 64;
	    argo1Gap = 64;
	    argo1GapColor = white;
	    argo1GapColorIndex = 1;
	    argo1IsCentered = false;     // not in the GUI
	    argo1GapAlpha = 255;
	    initArgo1(0);
	    // second Argosy instance
	    argo2GenSelect = 15;         // menu item "Hilbert Random One"
	    argo2Alpha = 127;
	    argo2Colors = setArgoColorsAlpha(whiteBlack, argo2Alpha);
	    argo2PaletteSelect = 2;     // menu item "White, Black"
	    argo2Pattern = countToFive;
	    argo2PatternSelect = 2;      // menu item "Count to five"
	    argo2Reps = 0;
	    argo2Unit = 64;
	    argo2Gap = 64;
	    argo2GapColor = black;
	    argo2GapColorIndex = 0;
	    argo2IsCentered = false;     // not in the GUI
	    argo2GapAlpha = 255;
	    initArgo2(0);
	    // visibility and frozen state
	    isShowArgo1 = true;
	    isShowArgo2 = true;
	    isArgo1Freeze = false;
	    isArgo2Freeze = false;
	    // animation
	    argo1Step = 4;
	    argo2Step = -4;
	    animOpen = 16;
	    animClose = 16;
	    animRun1 = 32;
	    animHold1 = 48;
	    animRun2 = 16;
	    animHold2 = 36;
	    animDuration = 768;   // number of frames
	}
	
	/**
	 * Applies alpha channel value <code>alpha</code> to all the RGB colors in <code>colorArray</code> 
	 * and returns a new array with the modified colors.
	 * 
	 * @param colorArray    an array of RGB colors
	 * @param alpha         alpha channel value to apply to RGB colors
	 * @return              a new array with the modified colors.
	 */
	public int[] setArgoColorsAlpha(int[] colorArray, int alpha) {
		int[] arr = new int[colorArray.length];
		int i = 0;
		for (int c : colorArray) {
			arr[i++] = PixelAudioMapper.setAlpha(c, alpha);
		}
		return arr;
	}
	
    /**
     * Initializes Argosy instance <code>argo1</code> and then shifts its pixels by <code>shift</code> pixels.
     * 
     * @param shift    number of pixels to rotate left argo1.argosyArray
     */
    public void initArgo1(int shift) {
		argo1Mapper = selectMapper(argo1GenSelect, argo1Gen);
		argo1GapColor = PixelAudioMapper.setAlpha(argo1GapColor, argo1GapAlpha);
		argo1 = getArgosy(argo1Mapper, argo1Pattern, argo1Unit, argo1Reps, argo1IsCentered, argo1Colors, argo1Gap, argo1GapColor, argo1Step);
		if (shift != 0) {
			argo1.shift(-shift, true);
		}
		argo1Image.loadPixels();
		argo1.getMapper().plantPixels(argo1.getArgosyArray(), argo1Image.pixels, 0, mapSize, chan);
		argo1Image.updatePixels();
	}
	
    /**
     * Initializes Argosy instance <code>argo2</code> and then shifts its pixels by <code>shift</code> pixels.
     * 
     * @param shift    number of pixels to rotate left argo2.argosyArray
     */
	public void initArgo2(int shift) {
		argo2Mapper = selectMapper(argo2GenSelect, argo2Gen);
		argo2GapColor = PixelAudioMapper.setAlpha(argo2GapColor, argo2GapAlpha);
		argo2 = getArgosy(argo2Mapper, argo2Pattern, argo2Unit, argo2Reps, argo2IsCentered, argo2Colors, argo2Gap, argo2GapColor, argo2Step);
		if (shift > 0) {
			argo2.shift(-shift, true);
		}
		argo2Image.loadPixels();
		argo2.getMapper().plantPixels(argo2.getArgosyArray(), argo2Image.pixels, 0, mapSize, chan);
		argo2Image.updatePixels();
	}
	
	/**
	 * Creates a new Argosy instance using supplied arguments. Display of the Argosy array of color values 
	 * is managed through the supplied PixelAudioMapper argument. The array is created by stepping through 
	 * the pattern and color values, creating blocks of color of argosyUnitSize, separated by argosyGap 
	 * pixels. 
	 * 
	 * @param mapper            a PixelAudioMapper instance, with width and height appropriate for display window
	 * @param argosyPattern     a pattern of ints to step through in creating the argosy array
	 * @param argosyUnitSize    number of pixels in each block of same-colored pixels in the Argosy.argosyArray
	 * @param argosyReps        number of times to repeat the pattern, 0 for as many as will fit
	 * @param isCentered        true if argosy pixel array should be centered
	 * @param argosyColors      array of colors to step through
	 * @param argosyGap         number of pixels between repeated patterns
	 * @param argosyGapColor    color to apply to the argosy gap
	 * @param argoStep          the number of pixels to rotate when animating, mostly ignored in this app
	 * @return
	 */
	public Argosy getArgosy(PixelAudioMapper mapper, int[] argosyPattern, int argosyUnitSize, int argosyReps, boolean isCentered, 
			                int[] argosyColors, int argosyGap, int argosyGapColor, int argoStep) {
		return new Argosy(mapper, argosyPattern, argosyUnitSize, argosyReps, isCentered, argosyColors, argosyGap, argosyGapColor, argoStep);
	}
	
	// TODO set alpha in the the Argosy color palette, but don't change the gap color alpha -- part done
	/**
	 * Updates argo1 and argo2 to reflect animation and other changes.
	 */
	public void updateArgosies() {
		argo1Image.loadPixels();
		argo1.getMapper().plantPixels(argo1.getArgosyArray(), argo1Image.pixels, 0, mapSize, chan);
		argo1Image.updatePixels();
		argo2Image.loadPixels();
		argo2.getMapper().plantPixels(argo2.getArgosyArray(), argo2Image.pixels, 0, mapSize, chan);
		argo2Image.updatePixels();
	}
	
	/**
	 * Initializes some animation variables.
	 */
	public void initAnimation() {
		if (animOpen > 0) {
			holdCount1 = animOpen;
			holdCount2 = animOpen;
		}
		animstep1 = 1;
		animstep2 = 1;
	}
	
	public void draw() {
		background(bgColor);
		updateArgosies();
		if (isShowArgo1) image(argo1Image, 0, 0);
		if (isShowArgo2) image(argo2Image, 0, 0);
		if (isAnimating) {
			animate();
			// TODO set isBufferStale when Argosy changes, not on every animation frame
			// mostly done, but double check
			isBufferStale = true;
		}
		runTimeArray();		// animate audio event markers
		runAnimationArray();
	}
	
	/**
	 * Drives animation and video recording.
	 */
	public void animate() {
		if (isRecordingVideo) {
			recordVideo();
		}
		animateArgosies();
	}

	/**
	 * Animates argo1 and argo2 argosy arrays. Note that we are supplying our own
	 * animation step values by calling the Argosy.shift() method.
	 * The total shift is stored in Argosy.argosyPixelShift if isCounted is true
	 * (last argument to argo1.shift() and argo2.shift()).
	 */
	public void animateArgosies() {
	    if (animstep1 % animRun1 == 0) {
	        holdCount1 = animHold1;
	        if (!isArgo1Freeze) argo1.shift(argo1Step, true);
	        animstep1++;
	    }
	    if (holdCount1 > 0) {
	        holdCount1--;
	    }
	    else {
	        if (!isArgo1Freeze) argo1.shift(argo1Step, true);
	        animstep1++;
	    }
	    if (animstep2 % animRun2 == 0) {
	        holdCount2 = animHold2;
	        if (!isArgo2Freeze) argo2.shift(argo2Step, true);
	        animstep2++;
	    }
	    if (holdCount2 > 0) {
	        holdCount2--;
	    }
	    else {
	        if (!isArgo2Freeze) argo2.shift(argo2Step, true);
	        animstep2++;
	    }
	}
	
	
	
	/**
	 * record a frame of video
	 */
	public void recordVideo() {
		if (currentFrame >= animDuration) {
			println("--- Completed video at frame " + currentFrame);
			if (!isLooping) {
				isAnimating = false;
			}
			currentFrame = 0;
			if (isRecordingVideo) {
				isRecordingVideo = false;
				videx.endMovie();
				videx = null;
			}
		}
		else {
			if (videx == null) {
				println("----->>> start video recording at "+ frameRate +" frames per second");
				videx = new VideoExport(this, videoFilename);
				videx.setFrameRate(frameRate);
				videx.startMovie();
				initAnimation();
			}
			videx.saveFrame();
			currentFrame++;
			if (currentFrame > animDuration - animClose) {
				holdCount1 = animClose;
				holdCount2 = animClose;
			}
			println("-- video recording frame " + currentFrame + " of " + animDuration);
		}
	}
	
	/**
	 * The built-in mousePressed handler for Processing, but note that it forwards mouse coords to audiMousePressed().
	 */
	public void mousePressed() {
		// println("mousePressed:", mouseX, mouseY);
		// handle audio generation in response to a mouse click
		audioMousePressed(mouseX, mouseY);
	}
	
	/**
	 * Detects Caps Lock state. We use Caps Lock state to switch between audio and graphics command sets. 
	 * @return true if Caps Lock is down, false otherwise.
	 */
	public boolean isCapsLockDown() {
	  return Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK);
	}
	
	/**
	 * The built-in keyPressed handler for Processing, forwards key and keyCode to parseKey().
	 */
	public void keyPressed() {
	   	if (key != CODED) {
    		parseKey(key, keyCode);
    	}
    	else {
    		float g = audioOut.getGain();
			if (keyCode == UP) {
				setAudioGain(g + 3.0f);
				println("---- audio gain is "+ nf(audioOut.getGain(), 0, 2));
			}
			else if (keyCode == DOWN) {
				setAudioGain(g - 3.0f);
				println("---- audio gain is "+ nf(audioOut.getGain(), 0, 2));
			}
			else if (keyCode == RIGHT) {

			}
			else if (keyCode == LEFT) {

			}
    	}
	}
	
	/**
	 * Handles key press events passed on by the built-in keyPressed method. 
	 * 
	 * @param key
	 * @param keyCode
	 */
	public void parseKey(char key, int keyCode) {
		argo1PixelCount =  argo1.getArgosySize() * argo1.getUnitSize();
		argo2PixelCount =  argo2.getArgosySize() * argo2.getUnitSize();
		switch(key) {
		case ' ': { // toggle animation
			isAnimating = ! isAnimating;
			break;
		}
		case 'e': { // trigger elliptical trail of audio events
			animationPoints(width - 40, height - 40, 120);
			break;
		}
		case 'a': { // shift left one argosy unit
			if (!isArgo1Freeze) argo1.shift(argo1.getUnitSize(), true);
			if (!isArgo2Freeze) argo2.shift(argo2.getUnitSize(), true);
			break;
		}
		case 'A': { // shift right one argosy unit
			if (!isArgo1Freeze) argo1.shift(-argo1.getUnitSize(), true);
			if (!isArgo2Freeze) argo2.shift(-argo2.getUnitSize(), true);
			break;
		}
		case 'b': { // shift left one argosy length
			if (!isArgo1Freeze) argo1.shift(argo1PixelCount, true);
			if (!isArgo2Freeze) argo2.shift(argo2PixelCount, true);
			break;
		}
		case 'B': { // shift right one argosy length
			if (!isArgo1Freeze) argo1.shift(-argo1PixelCount, true);
			if (!isArgo2Freeze) argo2.shift(-argo2PixelCount, true);
			break;
		}
		case 'c': { // shift left one argosy length + argosy gap
			if (!isArgo1Freeze) argo1.shift(argo1PixelCount + argo1.getArgosyGap(), true);
			if (!isArgo2Freeze) argo2.shift(argo2PixelCount + argo2.getArgosyGap(), true);
		    break;
		}
		case 'C': { // shift right one argosy length + argosy gap
			if (!isArgo1Freeze) argo1.shift(-argo1PixelCount - argo1.getArgosyGap(), true);
			if (!isArgo2Freeze) argo2.shift(-argo2PixelCount - argo2.getArgosyGap(), true);
		    break;
		}
	    case 'd': { // advance one animation step
	        if (!isArgo1Freeze) argo1.shift(argo1Step, true);
	        if (!isArgo2Freeze) argo2.shift(argo2Step, true);
	        count++;
	        break;
	    }
	    case 'D': { // go back one animation step
	        if (!isArgo1Freeze) argo1.shift(-argo1Step, true);
	        if (!isArgo2Freeze) argo2.shift(-argo2Step, true);
	        count = 0;
	        break;
	    }
		case 'g': case 'G': { // set the pixelShift of argosies to zero (reset return point)
			if (!isArgo1Freeze) {
				argo1.zeroArgosyPixelShift();
				println("--->> argosy 1 pixel shift set to 0");
			}
			if (!isArgo2Freeze) {
				argo2.zeroArgosyPixelShift();
				println("--->> argosy 2 pixel shift set to 0");
			}
			break;
		}
		case 'l': { // shift argosies left one animation step
			if (!isArgo1Freeze) argo1.shiftLeft();		// shift pattern left by argo1.argoStep pixels
			if (!isArgo2Freeze) argo2.shiftLeft();		// shift pattern left by argo2.argoStep pixels
			break;
		}
		case 'L': { // shift argosies left one animation step
			if (!isArgo1Freeze) argo1.shift(argoStep, true);		// shift pattern left by this.argoStep pixels
			if (!isArgo2Freeze) argo2.shift(argoStep, true);		// shift pattern left by this.argoStep pixels
			break;
		}
		case 'r': { // shift argosies right one animation step
			if (!isArgo1Freeze) argo1.shiftRight();		// shift pattern right by argo1.argoStep pixels
			if (!isArgo2Freeze) argo2.shiftRight();		// shift pattern right by argo2.argoStep pixels
			break;
		}
		case 'R': { // shift argosies right one animation step
			if (!isArgo1Freeze) argo1.shift(-argoStep, true);		// shift pattern right by this.argoStep pixels
			if (!isArgo2Freeze) argo2.shift(-argoStep, true);		// shift pattern right by this.argoStep pixels
			break;
		}
		case 'p': { // shift argosies left one pixel
		    if (!isArgo1Freeze) argo1.shift(1, true);
		    if (!isArgo2Freeze) argo2.shift(1, true);
		    break;
		}
		case 'P': { // shift argosies right one pixel
		    if (!isArgo1Freeze) argo1.shift(-1, true);
		    if (!isArgo2Freeze) argo2.shift(-1, true);
		    break;
		}
		case 'f': { // freeze changes to argosy 1
			isArgo1Freeze = !isArgo1Freeze;
			argo1Freeze.setSelected(isArgo1Freeze);
			println("isArgo1Freeze is "+ isArgo1Freeze);
			break;
		}
		case 'F': { // freeze changes to argosy 2
			isArgo2Freeze = !isArgo2Freeze;
			argo2Freeze.setSelected(isArgo2Freeze);
			println("isArgo2Freeze is "+ isArgo2Freeze);
			break;
		}
		case 'i': case 'I': { // show stats about argosies
			showArgosyStats();
			break;
		}
		case 'S': { // save current display to an PNG file
			saveToAudio(true);
			println("Saved audio signals to stereo audio file.");
			break;
		}
		case 's': { // save current display to an PNG file
			saveImage();
			break;
		}
		case 'u': case 'U': { // reinitialize any argosies that aren't frozen
		    if (!isArgo1Freeze) {
		    	initArgo1(0);
		    	println("--->> reinitialized argosy 1");
		    }
		    if (!isArgo2Freeze) {
		    	initArgo2(0);
		    	println("--->> reinitialized argosy 2");
		    }
			break;
		}
		case 'v': case 'V': { // toggle video recording
			isRecordingVideo = !isRecordingVideo;
			if (!isRecordingVideo) {
				println("-- video recording is off");
				if (videx != null) {
					videx.endMovie();
				}
			}
			else {
				println("-- video recording is on, press spacebar to toggle animation");
				initAnimation();
			}
			break;
		}
		case 'w': {    // reset animation tracking
			initAnimation();
			println("-- reset animation variables");
			break;
		}
		case 'W': {    // reset animation tracking
			initAnimation();
			println("-- reset animation variables");
			break;
		}
		case 'z': { // reset argosy 1 to initial position
			argo1.shift(-argo1.getArgosyPixelShift(), true);
			break;
		}
		case 'Z': { // reset argosy 2 to inttial position
			argo2.shift(-argo2.getArgosyPixelShift(), true);
			break;
		}
		case 'h': case 'H': { // show help message in console
			showHelp();
			break;
		}
		default: {
			break;
		}
		}
		// many commands change the argosy array, so we'll just set the stale flag for all of them
		isBufferStale = true;
	}
	
	/**
	 * Posts key command help to the console.
	 */
	public void showHelp() {
		println(" * Press ' ' to toggle animation.");
		println(" * Press 'e' to trigger elliptical trail of audio events.");
		println(" * Press 'a' to shift left one argosy unit.");
		println(" * Press 'A' to shift right one argosy unit.");
		println(" * Press 'b' to shift left one argosy length.");
		println(" * Press 'B' to shift right one argosy length.");
		println(" * Press 'c' to shift left one argosy length + argosy gap.");
		println(" * Press 'C' to shift right one argosy length + argosy gap.");
		println(" * Press 'd' to advance one animation step.");
		println(" * Press 'D' to go back one animation step.");
		println(" * Press 'g' or 'G' to set the pixelShift of argosies to zero (reset return point).");
		println(" * Press 'l' to shift argosies left one animation step.");
		println(" * Press 'L' to shift argosies left one animation step.");
		println(" * Press 'r' to shift argosies right one animation step.");
		println(" * Press 'R' to shift argosies right one animation step.");
		println(" * Press 'p' to shift argosies left one pixel.");
		println(" * Press 'P' to shift argosies right one pixel.");
		println(" * Press 'f' to freeze changes to argosy 1.");
		println(" * Press 'F' to freeze changes to argosy 2.");
		println(" * Press 'i' or 'I' to show stats about argosies.");
		println(" * Press 'S' to save current display to an PNG file.");
		println(" * Press 's' to save current display to an PNG file.");
		println(" * Press 'u' or 'U' to reinitialize any argosies that aren't frozen.");
		println(" * Press 'v' or 'V' to toggle video recording.");
		println(" * Press 'w' to reset animation tracking.");
		println(" * Press 'W' to reset animation tracking.");
		println(" * Press 'z' to reset argosy 1 to initial position.");
		println(" * Press 'Z' to reset argosy 2 to inttial position.");
		println(" * Press 'h' or 'H' to show help message in console.");
	}
	
	/**
	 * Sets audioOut.gain.
	 * @param g   gain value for audioOut, in decibels
	 */
	public void setAudioGain(float g) {
		audioOut.setGain(g);
		outputGain = audioOut.getGain();
	}

	/**
	 * Posts some information about the state of argo1 and argo2 to the console.
	 */
	public void showArgosyStats() {
		int maxreps1 = argo1.getMaxReps();
		int maxreps2 = argo2.getMaxReps();
		argo1PixelCount =  argo1.getArgosySize() * argo1.getUnitSize();
		argo2PixelCount =  argo2.getArgosySize() * argo2.getUnitSize();
		int gap1 = argo1.getArgosyGap();
		int gap2 = argo2.getArgosyGap();
		int length1 = maxreps1 * argo1PixelCount;
		int length2 = maxreps2 * argo2PixelCount;
		println("--->> Argosy 1: maxReps "+ maxreps1 +", pixel count "+ argo1PixelCount +", gap "+ gap1 
				+", total pixels "+ length1 +", animation step "+ argo1.getArgosyStep() +", pixelShift "+ argo1.getArgosyPixelShift());
		println("--->> Argosy 2: maxReps "+ maxreps2 +", pixel count "+ argo2PixelCount +", gap "+ gap2 
				+", total pixels "+ length2 +", animation step "+ argo2.getArgosyStep() +", pixelShift "+ argo2.getArgosyPixelShift());
	}

	
	// ------------- SAVE IMAGE FILE ------------- //
	
	/**
	 * Starts the image saving event chain. 
	 */
	public void saveImage() {
		// File folderToStartFrom = new File(dataPath(""));
		selectOutput("Select an image file to write to:", "imageFileSelectedWrite");
	}

	/**
	 * Handles image file output once an output file is selected.
	 * 
	 * @param selection    an output file for the image, forwarded from saveImage()
	 */
	public void imageFileSelectedWrite(File selection) {
		if (selection == null) {
		    println("Window was closed or the user hit cancel.");
		    return;			
		}
		String fileName = selection.getAbsolutePath();
		if (selection.getName().indexOf(".png") != selection.getName().length() - 4) {
			fileName += ".png";
		}
		saveImageToFile(drawOffscreen(), fileName);
	}
	
	/**
	 * Saves display image to a specified file.
	 * 
	 * @param img         image to save, reference to a PImage
	 * @param fileName    name of the file to save, typically a fully qualified file path + file name
	 */
	public void saveImageToFile(PImage img, String fileName) {
		img.save(fileName);
	}
	
	/**
	 * @return    a PImage generated in an offscreen PGraphics buffer.
	 */
	public PImage drawOffscreen() {
		PGraphics offscreen = createGraphics(width, height);
		offscreen.beginDraw();
		offscreen.background(bgColor);
		updateArgosies();
		if (isShowArgo1) offscreen.image(argo1Image, 0, 0);
		if (isShowArgo2) offscreen.image(argo2Image, 0, 0);
		offscreen.endDraw();
		return offscreen.get();
	}

	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                 BEGIN PATTERN MAKING METHODS                   */
	/*                                                                */
	/*      Generalized version of these methods are available        */
	/*      in the PixelAudio library. Use these as examples          */
	/*      of how to roll your own MultiGens.                        */
	/*                                                                */
	/*----------------------------------------------------------------*/
		
	/**
	 * 
	 * NAMING CONVENTIONS
	 * 
	 * Like all the methods that follow, hilbertLoop3x2() creates a MultiGen instance
	 * from a list of PixelMapGen objects (genList) and coordinate points (offsetList)
	 * where they will be displayed. MultiGen creates a single signal path over all
	 * the PixelMapGen objects. The path may be *continuous*, which is to say that
	 * the path through each PixelMapGen object ("gen" for short) only has to step
	 * one pixel up, down, left, or right to connect to the next gen. It may even
	 * create a loop, where the last pixel in the path is one step away from the
	 * first pixel. This is reflected in the naming conventions. 
	 * 
	 * In the method names, "ortho" refers to gens that are aligned in rows (or
	 * columns) where each new row begins one unit down from the previous row,
	 * always adding new gens in the same direction. In the "bou" methods 
	 * (named for boustrophodon, a method of writing text in alternating directions), 
	 * each successive row or column goes in the opposite direction from the previous
	 * one. The bou methods may provide continuous paths, the ortho methods are
	 * inherently discontinous, like row major bitmaps or video scanlines. 
	 * 
	 * Looping methods are are almost always more complex than bou and necessarily 
	 * more complex than ortho methods. Like the Hilbert curve, they involve
	 * changes in direction reminiscent of folding. Looping methods often have
	 * constraints on the numbers of rows and columns that can produce a loop.
	 * The constraints arise from the connectivity offered by the different
	 * PixelMapGen child classes: Hilbert gens have connections at two adjacent
	 * corners, DiagonalZigzag gens have connections at opposite corners. 
	 * Moore gens are loops to begin with, and have no connections, but are
	 * good for very symmetrical pattern-making.  
	 * 
	 */
	
	/** 
	 * hilbertLoop3x2() returns a looping fractal signal path consisting of 
	 * 6 Hilbert gens, 3 wide by 2 tall, to fit a 3 * genW by 2 * genH image. 
	 * 
	 * Note that genH must equal genW and both must be powers of 2. For the 
	 * image size we're using in this example, genW = image width / 3 and 
	 * genH = image height / 2.
	 * 
	 * @param genW    width of each HilbertGen 
	 * @param genH    height of each HilbertGen
	 * @return        a MultiGen consisting of 6 HilbertGens linked together by one signal path
	 */
	public MultiGen hilbertLoop3x2(int genW, int genH) {
	    // list of PixelMapGens that create a path through an image using PixelAudioMapper
		ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
		// list of x,y coordinates for placing gens from genList
		ArrayList<int[]> offsetList = new ArrayList<int[]>(); 		
		genList.add(new HilbertGen(genW, genH, fx270));
		offsetList.add(new int[] { 0, 0 });
		genList.add(new HilbertGen(genW, genH, nada));
		offsetList.add(new int[] { genW, 0 });
		genList.add(new HilbertGen(genW, genH, fx90));
		offsetList.add(new int[] { 2 * genW, 0 });
		genList.add(new HilbertGen(genW, genH, fx90));
		offsetList.add(new int[] { 2 * genW, genH });
		genList.add(new HilbertGen(genW, genH, r180));
		offsetList.add(new int[] { genW, genH });
		genList.add(new HilbertGen(genW, genH,fx270));
		offsetList.add(new int[] { 0, genH });
		return new MultiGen(width, height, offsetList, genList);
	}
	
	public MultiGen hilbertLoop6x4(int genW) {
		// get a HIlbert curve generator
		return HilbertGen.hilbertMultigenLoop(6, 4, genW);
	}
	
	/**
	 * This method creates a MultiGen consisting of a mix of zigzag and Hilbert curves
	 * in 6 columns and 4 rows arranged to provide a continuous loop.
	 * 
	 * @param genW
	 * @param genH
	 * @return
	 */
	public MultiGen hilbertZigzagLoop6x4(int genW, int genH) {
	    // list of PixelMapGens that create a path through an image using PixelAudioMapper
		ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
		// list of x,y coordinates for placing gens from genList
		ArrayList<int[]> offsetList = new ArrayList<int[]>(); 		
		int[][] locs = {{0,0}, {0,1}, {0,2}, {0,3}, {1,3}, {1,2}, {2,2}, {2,3}, 
						{3,3}, {3,2}, {4,2}, {4,3}, {5,3}, {5,2}, {5,1}, {5,0},
						{4,0}, {4,1}, {3,1}, {3,0}, {2,0}, {2,1}, {1,1}, {1,0}};
		AffineTransformType[] trans = {r270, r270, nada, r270, r90, fx270, nada, r270, 
				                       r90, r90, fx90, nada, r90, r90, r180, r90, 
				                       r270, fx90, r180, r90, r270, r270, fx270, r180};
		char[] cues = {'H','D','D','H','D','H','D','H', 
				       'H','D','H','D','H','D','D','H',
				       'D','H','D','H','H','D','H','D'}; 
		int i = 0;
		for (AffineTransformType att: trans) {
			int x = locs[i][0] * genW;
			int y = locs[i][1] * genH;
			offsetList.add(new int[] {x,y});
			// println("locs: ", locs[i][0], locs[i][1]);
			if (cues[i] == 'H') {
				genList.add(new HilbertGen(genW, genH, att));		
			}
			else {
				genList.add(new DiagonalZigzagGen(genW, genH, att));		
			}
			i++;
		}
		return new MultiGen(width, height, offsetList, genList);
	}
		
	/**
	 * This method creates a vertical stacks of rows of HilbertGens. Each row
	 * begins genH pixels down from the previous row, back at the beginning
	 * of the previous row (i.e., in "row major" order, like a bitmap). This 
	 * method pairs nicely with an image with 3 columns of with 8 rows of words,
	 * using the image as a control surface for sampling an audio file with 
	 * words recorded at the appropriate locations to match the screen order. 
	 * I used it for a performance work, DeadBodyWorkFlow, which is included
	 * in the 
	 * The signal path jumps from the end of the last gen in each row to the 
	 * beginning of the first gen int he next row. The path in each row is
	 * continuous, which provides some interesting optical effects. 
	 * 
	 * @param stacks    the number of stacks 
	 * @param rows      the number of rows in each stack
	 * @param units     the number of gens in each row
	 * @param genW      the width of each gen, a power of 2
	 * @param genH      the height of each gen, equal to genW
	 * @return          a Multigen consisting of stacks * rows * units PixelMapGens
	 */
	public MultiGen hilbertStackOrtho(int stacks, int rows, int units, int genW, int genH) {
	    // list of PixelMapGens that create a path through an image using PixelAudioMapper
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>(); 	
	    for (int s = 0; s < stacks; s++) {
	        for (int r = 0; r < rows; r++) {
	        	int shift = s * units;
	            for (int u = 0; u < units; u++) {
	                genList.add(new HilbertGen(genW, genH));
	                offsetList.add(new int[] {(u + shift) * genW, r * genH});
	            }
	        }
	    }
	    return new MultiGen(width, height, offsetList, genList);
	}

	/**
	 * This method creates a vertical stacks of rows of HilbertGens. Each row
	 * begins genH pixels down from the previous row. Alternating rows add units
	 * in opposite directions. This means path continuity is possible in each 
	 * stack by changing the orientation of the gens; however, it isn't fully 
	 * implemented in this example. Hint: choosing the right orientation for 
	 * each gen will assure path continuity. 
	 * 
	 * @param stacks    the number of stacks 
	 * @param rows      the number of rows in each stack
	 * @param units     the number of gens in each row
	 * @param genW      the width of each gen, a power of 2
	 * @param genH      the height of each gen, equal to genW
	 * @return          a Multigen consisting of stacks * rows * units PixelMapGens
	 */
	public MultiGen hilbertStackBou(int stacks, int rows, int units, int genW, int genH) {
	    // list of PixelMapGens that create a path through an image using PixelAudioMapper
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>(); 	
	    for (int s = 0; s < stacks; s++) {
	        for (int r = 0; r < rows; r++) {
	        	int shift = s * units;
	            if (r % 2 == 1) {
	                for (int u = 0; u < units; u++) {
	                    genList.add(new HilbertGen(genW, genH, flipx));
	                    offsetList.add(new int[] {(u + shift) * genW, r * genH});
	                }
	            }
	            else {
	                for (int u = units; u > 0; u--) {
	                    genList.add(new HilbertGen(genW, genH, flipy));
	                    offsetList.add(new int[] {(u + shift - 1) * genW, r * genH});
	                }
	            }
	        }
	    }
	    return new MultiGen(width, height, offsetList, genList);
	}
	
	/**
	 * This method creates rows of HilbertGens, starting each row from the left
	 * and adding gens. The odd rows are flipped vertically and the even rows are
	 * unchanged. The unchanged HilbertGen starts at upper left corner and ends at 
	 * upper right corner, so this provides some possibilities of symmetry between rows.
	 * The path is not continuous. 
	 * 
	 * @param cols    number of columns of gens wide
	 * @param rows    number of rows of gens high
	 * @param genW    width of each gen (same as genH and a power of 2)
	 * @param genH    height of each gen 
	 * @return        a MultiGen composed of cols * rows PixelMapGens
	 */
	public MultiGen hilbertRowOrtho(int cols, int rows, int genW, int genH) {
	    // list of PixelMapGens
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>();
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				if (y % 2 == 0) {
					genList.add(new HilbertGen(genW, genH, nada));
				}
				else {
					genList.add(new HilbertGen(genW, genH, flipy));
				}
				offsetList.add(new int[] {x * genW, y * genH});
			}
		}
		return new MultiGen(width, height, offsetList, genList);
	}
	
	/**
	 * This variation on hilbertRowOrtho arranges the gens vertically, in columns. 
	 * 
	 * @param cols    number of columns of gens wide
	 * @param rows    number of rows of gens high
	 * @param genW    width of each gen (same as genH and a power of 2)
	 * @param genH    height of each gen 
	 * @return        a MultiGen composed of cols * rows PixelMapGens
	 */
	public MultiGen hilbertColumnOrtho(int cols, int rows, int genW, int genH) {
	    // list of PixelMapGens that create a path through an image using PixelAudioMapper
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>();
		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				if (x % 2 == 0) {
					genList.add(new HilbertGen(genW, genH, r270));
				}
				else {
					genList.add(new HilbertGen(genW, genH, r90));
				}
				offsetList.add(new int[] {x * genW, y * genH});
			}
		}
		return new MultiGen(width, height, offsetList, genList);
	}
	
	/**
	 * @param genW    width of each zigzag gen
	 * @param genH    height of each zigzag gen
	 * @return        a looping MultiGen with 6 rows x 4 columns of DiagonalZigzagGen instances
	 */
	public MultiGen zigzagLoop6x4(int genW, int genH) {
	    // list of PixelMapGens that create a path through an image using PixelAudioMapper
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>();
	    int[][] locs = {{0,0}, {1,0}, {2,0}, {3,0}, {4,0}, {5,0}, 
	    		{5,1}, {5,2}, {5,3}, {4,3}, {4,2}, {4,1},
	    		{3,1}, {3,2}, {3,3}, {2,3}, {2,2}, {2,1},
	    		{1,1}, {1,2}, {1,3}, {0,3}, {0,2}, {0,1}};
		AffineTransformType[] trans = {r90, fx90, r90, fx90, r90, fx90, 
				                       r270, fx90, r270, fx270, r90, fx270, 
				                       r270, fx90, r270, fx270, r90, fx270, 
				                       r270, fx90, r270, fx270, r90, fx270};
		int i = 0;
		for (AffineTransformType att: trans) {
		    int x = locs[i][0] * genW;
		    int y = locs[i][1] * genH;
		    offsetList.add(new int[] {x,y});
		    // println("locs: ", locs[i][0], locs[i][1]);
		    genList.add(new DiagonalZigzagGen(genW, genH, att));
		    i++;
		}
		return new MultiGen(width, height, offsetList, genList);
	}
	
	/**
	 * Creates a MultiGen with rows * cols DiagonalZigzagGens. 
	 * Note that you should set values for such that:
	 * (rows * genW) == width and (cols * genH) == height.
	 * "Ortho" implies that each row starts at the left edge.
	 * The orientation of the the diagonals changes for alternate rows.
	 * 
	 * @param cols    number of vertical columns
	 * @param rows    number of horiaontal rows 
	 * @param genW    width of an individual PixelMapGen
	 * @param genH    height of an indvidual PixelMapGen
	 * @return        a MultiGen created from rows * cols PixelMapGens
	 */
	public MultiGen zigzagRowOrtho(int cols, int rows, int genW, int genH) {
	    // list of PixelMapGens that create an image using mapper
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>();
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				if (y % 2 == 0) {
					genList.add(new DiagonalZigzagGen(genW, genH, flipy));
				}
				else {
					genList.add(new DiagonalZigzagGen(genW, genH, nada));
				}
				offsetList.add(new int[] {x * genW, y * genH});
			}
		}
		return new MultiGen(width, height, offsetList, genList);
	}
	
	/**
	 * Creates a MultiGen with rows * cols DiagonalZigzagGens. 
	 * Note that you should set values for such that:
	 * (rows * genW) == width and (cols * genH) == height.
	 * "Ortho" implies that each row starts at the left edge.
	 * The orientation of the the diagonals is more complex than 
	 * zigzagRowOrtho(), hence the "Alt" in the method name.
	 * 
	 * @param cols    number of vertical columns
	 * @param rows    number of horiaontal rows 
	 * @param genW    width of an individual PixelMapGen
	 * @param genH    height of an indvidual PixelMapGen
	 * @return        a MultiGen created from rows * cols PixelMapGens
	 */
	public MultiGen zigzagRowAltOrtho(int cols, int rows, int genW, int genH) {
		// list of PixelMapGens that create an image using mapper
		ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
		// list of x,y coordinates for placing gens from genList
		ArrayList<int[]> offsetList = new ArrayList<int[]>();
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				if (y % 2 == 0) {
					if (x % 2 == 0) {
						genList.add(new DiagonalZigzagGen(genW, genH, flipy));
					}
					else {
						genList.add(new DiagonalZigzagGen(genW, genH, nada));
					}
					offsetList.add(new int[] { x * genW, y * genH });
				} 
				else {
					if (x % 2 == 0) {
						genList.add(new DiagonalZigzagGen(genW, genH, nada));
					} 
					else {
						genList.add(new DiagonalZigzagGen(genW, genH, flipy));
					}
					offsetList.add(new int[] { x * genW, y * genH });
				}
			}
		}
		return new MultiGen(width, height, offsetList, genList);
	}
	
	/**
	 * Creates a MultiGen with rows * cols DiagonalZigzagGens. 
	 * Note that you should set values for such that:
	 * (rows * genW) == width and (cols * genH) == height.
	 * The MultiGen steps along columns rather than rows. 
	 * "Ortho" implies that each column starts at the top edge.
	 * The orientation of the diagonals alternates one column to the next.
	 * 
	 * @param cols    number of vertical columns
	 * @param rows    number of horiaontal rows 
	 * @param genW    width of an individual PixelMapGen
	 * @param genH    height of an indvidual PixelMapGen
	 * @return        a MultiGen created from rows * cols PixelMapGens
	 */ 
	public MultiGen zigzagColumnOrtho(int cols, int rows, int genW, int genH) {
		// list of PixelMapGens that create a path through an image using
		// PixelAudioMapper
		ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
		// list of x,y coordinates for placing gens from genList
		ArrayList<int[]> offsetList = new ArrayList<int[]>();
		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				if (x % 2 == 0) {
					genList.add(new DiagonalZigzagGen(genW, genH, fx270));
				} 
				else {
					genList.add(new DiagonalZigzagGen(genW, genH, r90));
				}
				offsetList.add(new int[] { x * genW, y * genH });
			}
		}
		return new MultiGen(width, height, offsetList, genList);
	}

	/**
	 * Creates a MultiGen with rows * cols DiagonalZigzagGens. 
	 * Note that you should set values for such that:
	 * (rows * genW) == width and (cols * genH) == height.
	 * The MultiGen steps along columns rather than rows. 
	 * "Ortho" implies that each column starts at the top edge.
	 * The orientation of the diagonals is more complex than zigzagColumnOrtho(),
	 * hence "Alt" in the method name.
	 * 
	 * @param cols    number of vertical columns wide
	 * @param rows    number of horiaontal rows high
	 * @param genW    width of an individual PixelMapGen
	 * @param genH    height of an indvidual PixelMapGen
	 * @return        a MultiGen created from rows * cols PixelMapGens
	 */ 
	public MultiGen zigzagColumnAltOrtho(int cols, int rows, int genW, int genH) {
	    // list of PixelMapGens that create a path through an image using PixelAudioMapper
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>();
		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				if (y % 2 == 0) {
					if (x % 2 == 0) {
						genList.add(new DiagonalZigzagGen(genW, genH, fx270));
					}
					else {
						genList.add(new DiagonalZigzagGen(genW, genH, r90));
					}					
				}
				else {
					if (x % 2 == 0) {
						genList.add(new DiagonalZigzagGen(genW, genH, r90));
					}
					else {
						genList.add(new DiagonalZigzagGen(genW, genH, fx270));
					}					
				}
				offsetList.add(new int[] {x * genW, y * genH});
			}
		}
		return new MultiGen(width, height, offsetList, genList);
	}
	
	/**
	 * Creates a MultiGen with rows * cols DiagonalZigzagGens. 
	 * Note that you should set values for such that:
	 * (rows * genW) == width and (cols * genH) == height.
	 * The MultiGen steps along rows. 
	 * "Ortho" implies that each row starts at the left edge.
	 * The orientation of the diagonals is randomized.
	 * 
	 * @param cols    number of vertical columns
	 * @param rows    number of horiaontal rows 
	 * @param genW    width of an individual PixelMapGen
	 * @param genH    height of an indvidual PixelMapGen
	 * @return        a MultiGen created from rows * cols PixelMapGens
	 */ 
	public MultiGen zigzagRowRandomFlip(int cols, int rows, int genW, int genH) {
	    // list of PixelMapGens that create an image using mapper
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>();
	    for (int y = 0; y < rows; y++) {
	        for (int x = 0; x < cols; x++) {
	            genList.add(new DiagonalZigzagGen(genW, genH, randomTransform()));
	            offsetList.add(new int[] {x * genW, y * genH});
	        }
	    }
	    return new MultiGen(width, height, offsetList, genList);
	}

	/**
	 * Creates a MultiGen with rows * cols BoustropheGens. 
	 * Note that you should set values for such that:
	 * (rows * genW) == width and (cols * genH) == height.
	 * 
	 * @param cols    number of vertical columns wide
	 * @param rows    number of horiaontal rows high
	 * @param genW    width of an individual PixelMapGen
	 * @param genH    height of an indvidual PixelMapGen
	 * @return        a MultiGen created from rows * cols PixelMapGens
	 */ 
	public MultiGen boustrophRowRandom(int cols, int rows, int genW, int genH) {
	    // list of PixelMapGens that create an image using mapper
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>();
	    for (int y = 0; y < rows; y++) {
	        for (int x = 0; x < cols; x++) {
	            genList.add(new BoustropheGen(genW, genH, randomTransform()));
	            offsetList.add(new int[] {x * genW, y * genH});
	        }
	    }
	    return new MultiGen(width, height, offsetList, genList);
	}

	/**
	 * Creates a MultiGen with rows * cols HilbertGens.
	 * Note that you should set values for such that:
	 * (rows * genW) == width and (cols * genH) == height
	 * and that genH == genW and both are powers of 2.
	 * The orientation of the HilbertGens is randomized.
	 * 
	 * @param cols    number of vertical columns wide
	 * @param rows    number of horiaontal rows high
	 * @param genW    width of an individual PixelMapGen
	 * @param genH    height of an indvidual PixelMapGen
	 * @return        a MultiGen created from rows * cols PixelMapGens
	 */
	public MultiGen hilbertRowRandomFlip(int cols, int rows, int genW, int genH) {
	    // list of PixelMapGens that create an image using mapper
	    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
	    // list of x,y coordinates for placing gens from genList
	    ArrayList<int[]> offsetList = new ArrayList<int[]>();
	    for (int y = 0; y < rows; y++) {
	        for (int x = 0; x < cols; x++) {
	            genList.add(new HilbertGen(genW, genH, randomTransform()));
	            offsetList.add(new int[] {x * genW, y * genH});
	        }
	    }
	    return new MultiGen(width, height, offsetList, genList);
	}
	
	/**
	 * Returns a randomly selected AffineTransformType. @see PixelMapGen.
	 * @return    a random element from transArray
	 */
	public AffineTransformType randomTransform() {
		return this.transArray[rand.nextInt(this.transArray.length)];
	}

	
	/**
	 * A method for creating a PixelMapGen (MultiGen) to initialize and return a PixelAudioMapper, 
	 * called by the GUI methods genMenu1_hit() and genMenu2_hit().
	 * 
	 * @param selector    number to select a particular method to create MultiGen, keyed to GUI.
	 * @param gen         a PixelMapGen object that will contain the new PixelMapGen on completion
	 * @return            a PixelAudioMapper created with the new PixelMapGen
	 */
	public PixelAudioMapper selectMapper(int selector, PixelMapGen gen) {
	    switch (selector) {
	    case(0):
	        gen = hilbertLoop3x2(width/3, height/2);
	        break;
	    case(1):
	        gen = hilbertLoop6x4(width/6);
	        break;
	    case(2):
	        gen = hilbertZigzagLoop6x4(width/6, height/4);
	        break;
	    case(3):
	        gen = hilbertStackOrtho(3, 8, 4, imageWidth/12, imageHeight/8);
	        break;
	    case(4):
	        gen = hilbertStackBou(3, 8, 4, imageWidth/12, imageHeight/8);
	        break;
	    case(5):
	        gen = hilbertRowOrtho(6, 4, height/4, width/6);
	        break;
	    case(6):
	        gen = hilbertColumnOrtho(6, 4, height/4, width/6);
	        break;
	    case(7):
	        gen = zigzagLoop6x4(width/6, height/4);
	        break;
	    case(8):
	        gen = zigzagRowOrtho(6, 4, width/6, height/4);
	        break;
	    case(9):
	        gen = zigzagRowAltOrtho(6, 4, width/6, height/4);
	        break;
	    case(10): 
	        gen = zigzagColumnOrtho(6, 4, width/6, height/4);
	        break;
	    case(11): 
	        gen = zigzagColumnAltOrtho(6, 4, width/6, height/4);
	        break;
	    case(12): 
	        gen = zigzagRowRandomFlip(6, 4, width/6, height/4);
	        break;
	    case(13): 
	        gen = zigzagRowRandomFlip(12, 8, width/12, height/8);
	        break;
	    case(14): 
	        gen = boustrophRowRandom(6, 4, width/6, height/4);
	        break;
	    case(15): 
	        gen = hilbertRowRandomFlip(6, 4, width/6, height/4);
	        break;
	    case(16): 
	        gen = hilbertRowRandomFlip(12, 8, width/12, height/8);
	        break;
	    case(17): 
	        gen = hilbertRowRandomFlip(24, 16, width/24, height/16);
	        break;
	    default: 
	        gen = hilbertLoop3x2(width/3, height/2);
	        break;
	    }
	    return new PixelAudioMapper(gen);
	}
	
	/*------------------------------------------------------------------*/
	/*                                                                  */
	/*                    END PATTERN MAKING METHODS                    */
	/*                                                                  */
	/*------------------------------------------------------------------*/

	
	/* ---------------------------------------------------------------- */
	/*                                                                  */
	/*                      GUI Interface Code                          */
	/*                       Uses G4P library                           */	
	/*                                                                  */
	/* ---------------------------------------------------------------- */


	// Variable declarations 
	GWindow controlWindow;
	GLabel argo1Label; 
	GLabel genLabel1;
	GDropList genMenu1;
	GLabel colorsLabel1; 
	GDropList colorMenu1; 
	GLabel alpha1Label; 
	GTextField alpha1Text;
	GLabel patternLabel1; 
	GDropList patternMenu1; 
	GLabel repLabel1; 
	GTextField repText1; 
	GLabel unitLabel1; 
	GTextField unitSize1; 
	GLabel gapLabel1; 
	GTextField gap1Text; 
	GLabel gapColorLabel1;
	GDropList gapColorMenu1;
	GLabel gapAlpha1Label; 
	GTextField gapAlpha1Text;
	GCheckbox argo1Show;
	GCheckbox argo1Freeze;
	GLabel argoStepLabel1;
	GTextField argoStepText1;
	//
	GLabel argo2Label; 
	GLabel genLabel2;
	GDropList genMenu2;
	GLabel colorsLabel2; 
	GDropList colorMenu2; 
	GLabel alpha2Label; 
	GTextField alpha2Text;
	GLabel patternLabel2; 
	GDropList patternMenu2; 
	GLabel repLabel2; 
	GTextField repText2; 
	GLabel unitLabel2; 
	GTextField unitSize2; 
	GLabel gapLabel2; 
	GTextField gap2Text; 
	GLabel gapColorLabel2;
	GDropList gapColorMenu2;
	GLabel gapAlpha2Label; 
	GTextField gapAlpha2Text;
	GCheckbox argo2Show;
	GCheckbox argo2Freeze;
	GLabel argoStepLabel2;
	GTextField argoStepText2;
	// 
	GLabel animationLabel;
	GLabel animOpenLabel;
	GTextField animOpenText;
	GLabel animRunLabel1;
	GTextField animRunText1;
	GLabel animHoldLabel1;
	GTextField animHoldText1;
	GLabel animRunLabel2;
	GTextField animRunText2;
	GLabel animHoldLabel2;
	GTextField animHoldText2;
	GLabel animCloseLabel;
	GTextField animCloseText;
	GLabel animDurationLabel;
	GTextField animDurationText;
	GButton recordButton;

	// 
	// menu items
	String[] genItems = {"Hilbert Loop 3x2", "Hilbert Loop 6x4", "Hilbert ZZ Loop", "Hilbert Stack Ortho", "Hilbert Stack Bou", "Hilbert Row Ortho", 
			 "Hilbert Column Ortho", "ZZ Loop 6x4", "ZZ Row Ortho","ZZ Row Alt Ortho", "ZZ Column Ortho", 
			 "ZZ Column Alt Ortho", "ZZ Row Random One", "ZZ Row Random Two", "Boustroph Row Random", "Hilbert Random One", "Hilbert Random Two", "Hilbert Random Three"};
	String[] colorItems = {"Black Alone", "Black, White", "White, Black", "Black, Gray, White", "Gray Ramp", "Gray Triangle", "Multicolor", "Spectrum 8", 
			               "Spectrum 6", "Blue Cream", "Cream Blue", "Four Color", "Five Color"};
	int[][] colorVars = {blackAlone, blackWhite, whiteBlack, blackGrayWhite, grayRamp, grayTriangle, multicolor, espectroOcho, 
			               espectroSeis, blueCream, creamBlue, fourColor, fiveColor};
	String[] patternItems = {"The One", "One-one", "Count to Five", "Odd One to Seven", "Four Power", "Seven Forty-nine", "Fibo 55", "Fibonacci", "Lucas"};
	int[][] patternVars = {theOne, oneOne, countToFive, oddOneToSeven, fourPower, sevenFortyNine, fiboLSystem55, fibonacciNums, lucasNums};
	int[] gapColorVars = {black, white, gray, roig, roigtar, taronja, groc, vert, blau, blau2, violet, grana, blanc, gris, negre};
	String[] gapColorItems = {"black", "white", "gray", "red", "red orange", "orange", "yellow", "green", 
			                  "blue 1", "blue 2", "violet", "red violet", "bone", "blue gray", "midnight"};
	
	// spacing variables
	int ypos = 10;
	int inc = 28;


	// Create all the GUI controls. 
	public void createGUI(){
	  G4P.messagesEnabled(false);
	  G4P.setGlobalColorScheme(GCScheme.BLUE_SCHEME);
	  G4P.setMouseOverEnabled(false);
	  surface.setTitle("Argosy Window");
	  /* ----->>> floating control window <<<----- */  
	  controlWindow = GWindow.getWindow(this, "Argosy Settings", 0, 0, 480, 540, JAVA2D);
	  controlWindow.noLoop();
	  controlWindow.setActionOnClose(G4P.KEEP_OPEN);
	  controlWindow.addDrawHandler(this, "drawControlWindow");
	  /* ----->>> argosy01 controls <<<----- */
      ypos = 10;
	  argo1Label = new GLabel(controlWindow, 10, ypos, 220, 20);
	  argo1Label.setText("Argosy 1");
	  argo1Label.setTextBold();
	  argo1Label.setOpaque(true);
	  //
	  ypos += inc;
	  genLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
	  genLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  genLabel1.setText("Map: ");
	  genLabel1.setOpaque(false);
	  genMenu1 = new GDropList(controlWindow, 100, ypos, 130, 120, 5, 10);
	  genMenu1.setItems(genItems, 0);
	  genMenu1.addEventHandler(this, "genMenu1_hit");
	  genMenu1.setSelected(argo1GenSelect);
	  //
	  ypos += inc;
	  colorsLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
	  colorsLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  colorsLabel1.setText("Colors: ");
	  colorsLabel1.setOpaque(false);
	  colorMenu1 = new GDropList(controlWindow, 100, ypos, 130, 120, 5, 10);
	  colorMenu1.setItems(colorItems, 0);
	  colorMenu1.addEventHandler(this, "colorMenu1_hit");
	  colorMenu1.setSelected(argo1PaletteSelect);
	  //
	  ypos += inc;
	  alpha1Label = new GLabel(controlWindow, 10, ypos, 80, 20);
	  alpha1Label.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  alpha1Label.setText("Opacity: ");
	  alpha1Label.setOpaque(false);
	  alpha1Text = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);
	  alpha1Text.setText(String.valueOf(argo1Alpha));
	  alpha1Text.setOpaque(true);
	  alpha1Text.setNumeric(0, 255, argo1Alpha);
	  alpha1Text.addEventHandler(this, "alphaText1_change");
	  //
	  ypos += inc;
	  patternLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
	  patternLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  patternLabel1.setText("Pattern: ");
	  patternLabel1.setOpaque(false);
	  patternMenu1 = new GDropList(controlWindow, 100, ypos, 130, 120, 5, 10);
	  patternMenu1.setItems(patternItems, 0);
	  patternMenu1.addEventHandler(this, "patternMenu1_hit");
	  patternMenu1.setSelected(argo1PatternSelect);
	  //
	  ypos += inc;
	  repLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
	  repLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  repLabel1.setText("Repeat: ");
	  repLabel1.setOpaque(false);
	  repText1 = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);
	  repText1.setText("0");
	  repText1.setOpaque(true);
	  repText1.setNumeric(0, 16384, 0);
	  repText1.addEventHandler(this, "repText1_change");
	  //
	  ypos += inc;
	  unitLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
	  unitLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  unitLabel1.setText("Unit: ");
	  unitLabel1.setOpaque(false);
	  unitSize1 = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);
	  unitSize1.setText(String.valueOf(argo1Unit));
	  unitSize1.setOpaque(true);
	  unitSize1.setNumeric(1, 65536, 1);
	  unitSize1.addEventHandler(this, "unitSize1_change");
	  //
	  ypos += inc;
	  gapLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
	  gapLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  gapLabel1.setText("Gap: ");
	  gapLabel1.setOpaque(false);
	  gap1Text = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);
	  gap1Text.setText(String.valueOf(argo1Gap));
	  gap1Text.setOpaque(true);
	  gap1Text.setNumeric(0, 1048576, 1);
	  gap1Text.addEventHandler(this, "gap1_change");
	  //
	  ypos += inc;
	  gapColorLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
	  gapColorLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  gapColorLabel1.setText("Gap color: ");
	  gapColorLabel1.setOpaque(false);
	  gapColorMenu1 = new GDropList(controlWindow, 100, ypos, 130, 120, 5, 10);
	  gapColorMenu1.setItems(gapColorItems, 0);
	  gapColorMenu1.addEventHandler(this, "gapColorMenu1_hit");
	  gapColorMenu1.setSelected(argo1GapColorIndex);
	  //
	  ypos += inc;
	  gapAlpha1Label = new GLabel(controlWindow, 10, ypos, 80, 20);
	  gapAlpha1Label.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  gapAlpha1Label.setText("Gap opacity: ");
	  gapAlpha1Label.setOpaque(false);
	  gapAlpha1Text = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
	  gapAlpha1Text.setText(String.valueOf(argo1GapAlpha));
	  gapAlpha1Text.setOpaque(true);
	  gapAlpha1Text.setNumeric(0, 255, 255);
	  gapAlpha1Text.addEventHandler(this, "gapOpacity1_change");
  	  //
  	  ypos += inc;
  	  // animation label -- print ypos to get coordinate 
	  animationLabel = new GLabel(controlWindow, 10, ypos, 460, 20);
	  animationLabel.setText("Animation Settings");
	  animationLabel.setTextBold();
	  animationLabel.setOpaque(true);
	  //
	  ypos += inc;
	  argo1Show = new GCheckbox(controlWindow, 100, ypos, 128, 20);
	  argo1Show.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
	  argo1Show.setText("Show Argosy One");
	  argo1Show.setOpaque(false);
	  argo1Show.setSelected(isShowArgo1);
	  argo1Show.addEventHandler(this, "argo1ShowCheck_hit");
	  //
	  ypos += 20;
	  argo1Freeze = new GCheckbox(controlWindow, 100, ypos, 128, 20);
	  argo1Freeze.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
	  argo1Freeze.setText("Freeze");
	  argo1Freeze.setOpaque(false);
	  argo1Freeze.setSelected(isArgo1Freeze);
	  argo1Freeze.addEventHandler(this, "argo1FreezeCheck_hit");
	  //
	  /* ----->>> argosy02 controls <<<----- */
	  //
      ypos = 10;
	  argo2Label = new GLabel(controlWindow, 250, ypos, 220, 20);
	  argo2Label.setText("Argosy 2");
	  argo2Label.setTextBold();
	  argo2Label.setOpaque(true);
	  //
	  ypos += inc;
	  genLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
	  genLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  genLabel2.setText("Map: ");
	  genLabel2.setOpaque(false);
	  genMenu2 = new GDropList(controlWindow, 340, ypos, 130, 120, 5, 10);
	  genMenu2.setItems(genItems, 0);
	  genMenu2.addEventHandler(this, "genMenu2_hit");
	  genMenu2.setSelected(argo2GenSelect);
	  //
	  ypos += inc;
	  colorsLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
	  colorsLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  colorsLabel2.setText("Colors: ");
	  colorsLabel2.setOpaque(false);
	  colorMenu2 = new GDropList(controlWindow, 340, ypos, 130, 120, 5, 10);
	  colorMenu2.setItems(colorItems, 0);
	  colorMenu2.addEventHandler(this, "colorMenu2_hit");
	  colorMenu2.setSelected(argo2PaletteSelect);
	  //
	  ypos += inc;
	  alpha2Label = new GLabel(controlWindow, 250, ypos, 80, 20);
	  alpha2Label.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  alpha2Label.setText("Opacity: ");
	  alpha2Label.setOpaque(false);
	  alpha2Text = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);
	  alpha2Text.setText(String.valueOf(argo2Alpha));
	  alpha2Text.setOpaque(true);
	  alpha2Text.setNumeric(0, 255, argo2Alpha);
	  alpha2Text.addEventHandler(this, "alphaText2_change");
	  //
	  ypos += inc;
	  patternLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
	  patternLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  patternLabel2.setText("Pattern: ");
	  patternLabel2.setOpaque(false);
	  patternMenu2 = new GDropList(controlWindow, 340, ypos, 130, 120, 5, 10);
	  patternMenu2.setItems(patternItems, 0);
	  patternMenu2.addEventHandler(this, "patternMenu2_hit");
	  patternMenu2.setSelected(argo2PatternSelect);
	  //
	  ypos += inc;
	  repLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
	  repLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  repLabel2.setText("Repeat: ");
	  repLabel2.setOpaque(false);
	  repText2 = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);
	  repText2.setText("0");
	  repText2.setOpaque(true);
	  repText2.setNumeric(0, 16384, 0);
	  repText2.addEventHandler(this, "repText2_change");
	  //
	  ypos += inc;
	  unitLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
	  unitLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  unitLabel2.setText("Unit: ");
	  unitLabel2.setOpaque(false);
	  unitSize2 = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);
	  unitSize2.setText(String.valueOf(argo2Unit));
	  unitSize2.setOpaque(true);
	  unitSize2.setNumeric(1, 65536, 1);
	  unitSize2.addEventHandler(this, "unitSize2_change");
	  //
	  ypos += inc;
	  gapLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
	  gapLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  gapLabel2.setText("Gap: ");
	  gapLabel2.setOpaque(false);
	  gap2Text = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);
	  gap2Text.setText(String.valueOf(argo2Gap));
	  gap2Text.setOpaque(true);
	  gap2Text.setNumeric(0, 1048576, 1);
	  gap2Text.addEventHandler(this, "gap2_change");
	  //
	  ypos += inc;
      gapColorLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
	  gapColorLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  gapColorLabel2.setText("Gap color: ");
	  gapColorLabel2.setOpaque(false);
	  gapColorMenu2 = new GDropList(controlWindow, 340, ypos, 130, 120, 5, 10);
	  gapColorMenu2.setItems(gapColorItems, 0);
	  gapColorMenu2.addEventHandler(this, "gapColorMenu2_hit");
	  gapColorMenu2.setSelected(argo2GapColorIndex);
	  //
	  ypos += inc;
	  gapAlpha2Label = new GLabel(controlWindow, 250, ypos, 80, 20);
	  gapAlpha2Label.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  gapAlpha2Label.setText("Gap opacity: ");
	  gapAlpha2Label.setOpaque(false);
	  gapAlpha2Text = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
	  gapAlpha2Text.setText(String.valueOf(argo2GapAlpha));
	  gapAlpha2Text.setOpaque(true);
	  gapAlpha2Text.setNumeric(0, 255, 255);
	  gapAlpha2Text.addEventHandler(this, "gapOpacity2_change");
  	  //
  	  ypos += inc;
  	  // space for animation label
	  //
	  ypos += inc;
	  argo2Show = new GCheckbox(controlWindow, 340, ypos, 128, 20);
	  argo2Show.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
	  argo2Show.setText("Show Argosy Two");
	  argo2Show.setOpaque(false);
	  argo2Show.setSelected(isShowArgo2);
	  argo2Show.addEventHandler(this, "argo2ShowCheck_hit");
	  //
	  ypos += 20;
	  argo2Freeze = new GCheckbox(controlWindow, 340, ypos, 128, 20);
	  argo2Freeze.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
	  argo2Freeze.setText("Freeze");
	  argo2Freeze.setOpaque(false);
	  argo2Freeze.setSelected(isArgo2Freeze);
	  argo2Freeze.addEventHandler(this, "argo2FreezeCheck_hit");
	  //
	  /* ----->>> Animation controls <<<----- */
	  //
	  ypos += inc;
	  argoStepLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
	  argoStepLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  argoStepLabel1.setText("Step: ");
	  argoStepLabel1.setOpaque(false);
	  argoStepText1 = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
	  argoStepText1.setText(String.valueOf(argo1Step));
	  argoStepText1.setOpaque(true);
	  argoStepText1.setNumericType(G4P.INTEGER);
	  argoStepText1.addEventHandler(this, "argoStep1_change");
	  //
	  argoStepLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
  	  argoStepLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
  	  argoStepLabel2.setText("Step: ");
  	  argoStepLabel2.setOpaque(false);
  	  argoStepText2 = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
  	  argoStepText2.setText(String.valueOf(argo2Step));
  	  argoStepText2.setOpaque(true);
  	  argoStepText2.setNumericType(G4P.INTEGER);
  	  argoStepText2.addEventHandler(this, "argoStep2_change");
	  //
	  ypos += inc;
	  animOpenLabel = new GLabel(controlWindow, 10, ypos, 80, 20);
	  animOpenLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  animOpenLabel.setText("Open frames: ");
	  animOpenLabel.setOpaque(false);
	  animOpenText = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
	  animOpenText.setText(String.valueOf(animOpen));
	  animOpenText.setOpaque(true);
	  animOpenText.setNumeric(0, 65536, 0);
	  animOpenText.addEventHandler(this, "animOpen_change");
	  // 
	  animCloseLabel = new GLabel(controlWindow, 250, ypos, 80, 20);
	  animCloseLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  animCloseLabel.setText("Close frames: ");
	  animCloseLabel.setOpaque(false);
	  animCloseText = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
	  animCloseText.setText(String.valueOf(animClose));
	  animCloseText.setOpaque(true);
	  animCloseText.setNumeric(0, 65536, 0);
	  animCloseText.addEventHandler(this, "animClose_change");
	  //
	  ypos += inc;
	  animRunLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
	  animRunLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  animRunLabel1.setText("Run 1: ");
	  animRunLabel1.setOpaque(false);
	  animRunText1 = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
	  animRunText1.setText(String.valueOf(animRun1));
	  animRunText1.setOpaque(true);
	  animRunText1.setNumeric(0, 65536, 0);
	  animRunText1.addEventHandler(this, "animRun1_change");
	  //
	  animRunLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
	  animRunLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  animRunLabel2.setText("Run 2: ");
	  animRunLabel2.setOpaque(false);
	  animRunText2 = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
	  animRunText2.setText(String.valueOf(animRun2));
	  animRunText2.setOpaque(true);
	  animRunText2.setNumeric(0, 65536, 0);
	  animRunText2.addEventHandler(this, "animRun2_change");
	  //
	  ypos += inc;
	  animHoldLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
	  animHoldLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  animHoldLabel1.setText("Hold 1: ");
	  animHoldLabel1.setOpaque(false);
	  animHoldText1 = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
	  animHoldText1.setText(String.valueOf(animHold1));
	  animHoldText1.setOpaque(true);
	  animHoldText1.setNumeric(0, 65536, 0);
	  animHoldText1.addEventHandler(this, "animHold1_change");
	  //
	  animHoldLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
	  animHoldLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  animHoldLabel2.setText("Hold 2: ");
	  animHoldLabel2.setOpaque(false);
	  animHoldText2 = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
	  animHoldText2.setText(String.valueOf(animHold2));
	  animHoldText2.setOpaque(true);
	  animHoldText2.setNumeric(0, 65536, 0);
	  animHoldText2.addEventHandler(this, "animHold2_change");
	  //
	  ypos += inc;
	  animDurationLabel = new GLabel(controlWindow, 10, ypos, 80, 20);
	  animDurationLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
	  animDurationLabel.setText("Duration: ");
	  animDurationLabel.setOpaque(false);
	  animDurationText = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
	  animDurationText.setText(String.valueOf(animDuration));
	  animDurationText.setOpaque(true);
	  animDurationText.setNumeric(0, 65536, 0);
	  animDurationText.addEventHandler(this, "animDuration_change");	  
	  //
	  recordButton = new GButton(controlWindow, 270, ypos, 160, 20);
	  recordButton.setText("Record Video");
	  recordButton.addEventHandler(this, "recordButton_hit");
	  //
	  controlWindow.loop();
	}


	/* ----->>> EVENT HANDLERS <<<----- */  
	/* ----->>>   ARGOSY ONE   <<<----- */  


	synchronized public void drawControlWindow(PApplet appc, GWinData data) {
		appc.background(color(204, 199, 212));
		appc.stroke(gris);
		appc.strokeWeight(2);
		appc.line(10, 308, 470, 308);
	}

	public void genMenu1_hit(GDropList source, GEvent event) {
		argo1GenSelect = source.getSelectedIndex();
		int shift = argo1.getArgosyPixelShift();
		argo1Mapper = selectMapper(argo1GenSelect, argo1Gen);
		this.initArgo1(shift);
		isBufferStale = true;
	}
	
	public void colorMenu1_hit(GDropList source, GEvent event) {
		argo1PaletteSelect = source.getSelectedIndex();
		argo1Colors = setArgoColorsAlpha(colorVars[argo1PaletteSelect], argo1Alpha);
		int shift = argo1.getArgosyPixelShift();
		argo1.setArgosyColors(argo1Colors);
		if (shift != 0) {
			argo1.shift(-shift, true);
		}		
		isBufferStale = true;
	}

	public void alphaText1_change(GTextField source, GEvent event) {
		int val = source.getValueI();
		if (argo1Alpha != val) {
			argo1Alpha = val;
			argo1Colors = setArgoColorsAlpha(argo1.getArgosyColors(), argo1Alpha);
			int shift = argo1.getArgosyPixelShift();
			argo1.setArgosyColors(argo1Colors);
			if (shift != 0) {
				argo1.shift(-shift, true);
			}
			isBufferStale = true;
		}
	}
	
	public void patternMenu1_hit(GDropList source, GEvent event) {
		argo1PatternSelect = source.getSelectedIndex();
		argo1Pattern = patternVars[argo1PatternSelect];
		int shift = argo1.getArgosyPixelShift();
		argo1.setArgosyPattern(argo1Pattern);
		if (shift != 0) {
			argo1.shift(-shift, true);
		}		
		isBufferStale = true;
	}

	public void repText1_change(GTextField source, GEvent event) {
		int val = source.getValueI();
		if (argo1Reps != val) {
			argo1Reps = val;
			int shift = argo1.getArgosyPixelShift();
			argo1.setArgosyReps(argo1Reps);
			if (shift != 0) {
				argo1.shift(-shift, true);
			}
			isBufferStale = true;
		}
	}
	
	public void unitSize1_change(GTextField source, GEvent event) {
		int val = source.getValueI();
		if (argo1Unit != val) {
			argo1Unit = val;
			int shift = argo1.getArgosyPixelShift();
			argo1.setUnitSize(argo1Unit);
			if (shift != 0) {
				argo1.shift(-shift, true);
			}
			isBufferStale = true;
		}
	}

	public void gap1_change(GTextField source, GEvent event) {
		int val = source.getValueI();
		if (argo1Gap != val) {
			argo1Gap = val;
			//println("--->> new argo1 gap = "+ argo1Gap);
			int shift = argo1.getArgosyPixelShift();
			argo1.setArgosyGap(argo1Gap);
			if (shift != 0) {
				argo1.shift(-shift, true);
			}
			isBufferStale = true;
		}
	}

	public void gapColorMenu1_hit(GDropList source, GEvent event) {
		int index = source.getSelectedIndex();
		argo1GapColor = gapColorVars[index];
		gapColorMenu1.setSelected(index);
		int shift = argo1.getArgosyPixelShift();
		argo1.setArgosyGapColor(PixelAudioMapper.setAlpha(argo1GapColor, argo1GapAlpha));
		if (shift != 0) {
			argo1.shift(-shift, true);
		}		
		isBufferStale = true;
	}

	public void gapOpacity1_change(GTextField source, GEvent event) {
		int val = source.getValueI();
		if (argo1GapAlpha != val) {
			argo1GapAlpha = val;
			argo1GapColor = PixelAudioMapper.setAlpha(argo1GapColor, argo1GapAlpha);
			int shift = argo1.getArgosyPixelShift();
			argo1.setArgosyGapColor(argo1GapColor);
			if (shift != 0) {
				argo1.shift(-shift, true);
			}
			isBufferStale = true;
		}
	}
	
	public void argoStep1_change(GTextField source, GEvent event) {
	    int val = source.getValueI();
	    if (argo1Step != val) {
	        argo1Step = val;
	    }
	}

	public void argo1ShowCheck_hit(GCheckbox source, GEvent event) {
		isShowArgo1 = source.isSelected();
	}
	
	public void argo1FreezeCheck_hit(GCheckbox source, GEvent event) {
		this.isArgo1Freeze = source.isSelected();
		println("isArgo1Freeze is "+ isArgo1Freeze);
	}
	
	public void argo1InitAndShift() {
		int shift = argo1.getArgosyPixelShift();
		this.initArgo1(shift);
		isBufferStale = true;
	}

	/* ----->>>   ARGOSY TWO   <<<----- */  

	public void genMenu2_hit(GDropList source, GEvent event) {
		argo2GenSelect = source.getSelectedIndex();
		int shift = argo2.getArgosyPixelShift();
		argo2Mapper = selectMapper(argo2GenSelect, argo2Gen);
		this.initArgo2(shift);
		isBufferStale = true;
	}

	public void colorMenu2_hit(GDropList source, GEvent event) {
	    argo2PaletteSelect = source.getSelectedIndex();
	    argo2Colors = setArgoColorsAlpha(colorVars[argo2PaletteSelect], argo2Alpha);
	    int shift = argo2.getArgosyPixelShift();
	    argo2.setArgosyColors(argo2Colors);
	    if (shift != 0) {
	        argo2.shift(-shift, true);
	    }		
		isBufferStale = true;
	}

	public void alphaText2_change(GTextField source, GEvent event) {
		int val = source.getValueI();
		if (argo2Alpha != val) {
			argo2Alpha = val;
			argo2Colors = setArgoColorsAlpha(argo2.getArgosyColors(), argo2Alpha);
			int shift = argo2.getArgosyPixelShift();
			argo2.setArgosyColors(argo2Colors);
			if (shift != 0) {
				argo2.shift(-shift, true);
			}
			isBufferStale = true;
		}		
	}

	public void patternMenu2_hit(GDropList source, GEvent event) {
	    argo2PatternSelect = source.getSelectedIndex();
	    argo2Pattern = patternVars[argo2PatternSelect];
	    int shift = argo2.getArgosyPixelShift();
	    argo2.setArgosyPattern(argo2Pattern);
	    if (shift != 0) {
	        argo2.shift(-shift, true);
	    }		
		isBufferStale = true;
	}

	public void repText2_change(GTextField source, GEvent event) {
		int val = source.getValueI();
		if (argo2Reps != val) {
			argo2Reps = val;
			int shift = argo2.getArgosyPixelShift();
			argo2.setArgosyReps(argo2Reps);
			if (shift != 0) {
				argo2.shift(-shift, true);
			}	
			isBufferStale = true;
		}	
	}

	public void unitSize2_change(GTextField source, GEvent event) {
		int val = source.getValueI();
		if (argo2Unit != val) {
			argo2Unit = val;
			int shift = argo2.getArgosyPixelShift();
			argo2.setUnitSize(argo2Unit);
			if (shift != 0) {
				argo2.shift(-shift, true);
			}
			isBufferStale = true;
		}
	}

	public void gap2_change(GTextField source, GEvent event) {
		int val = source.getValueI();
		if (argo2Gap != val) {
			argo2Gap = val;
			//println("--->> new argo2 gap = "+ argo2Gap);
			int shift = argo2.getArgosyPixelShift();
			argo2.setArgosyGap(argo2Gap);
			if (shift != 0) {
				argo2.shift(-shift, true);
			}
			isBufferStale = true;
		}
	}

	public void gapColorMenu2_hit(GDropList source, GEvent event) {
	    int index = source.getSelectedIndex();
	    argo2GapColor = gapColorVars[index];
	    int shift = argo2.getArgosyPixelShift();
	    argo2.setArgosyGapColor(PixelAudioMapper.setAlpha(argo2GapColor, argo2GapAlpha));
	    if (shift != 0) {
	        argo2.shift(-shift, true);
	    }		
		isBufferStale = true;
	}

	public void gapOpacity2_change(GTextField source, GEvent event) {
		int val = source.getValueI();
		if (argo2GapAlpha != val) {
			argo2GapAlpha = val;
			argo2GapColor = PixelAudioMapper.setAlpha(argo2GapColor, argo2GapAlpha);
			int shift = argo2.getArgosyPixelShift();
			argo2.setArgosyGapColor(argo2GapColor);
			if (shift != 0) {
				argo2.shift(-shift, true);
			}
			isBufferStale = true;
		}
	}

	public void argoStep2_change(GTextField source, GEvent event) {
	    int val = source.getValueI();
	    if (argo2Step != val) {
	        argo2Step = val;
	    }
	}

	public void argo2ShowCheck_hit(GCheckbox source, GEvent event) {
		isShowArgo2 = source.isSelected();
	}
	
	public void argo2FreezeCheck_hit(GCheckbox source, GEvent event) {
		this.isArgo2Freeze = source.isSelected();
		println("isArgo2Freeze is "+ isArgo2Freeze);
	}

	public void argo2InitAndShift() {
		int shift = argo1.getArgosyPixelShift();
		this.initArgo2(shift);
		isBufferStale = true;
	}

	public void animOpen_change(GTextField source, GEvent event) {
	    int val = source.getValueI();
	    if (animOpen != val) {
	        animOpen = val;
	    }
	}
	
	public void animClose_change(GTextField source, GEvent event) {
	    int val = source.getValueI();
	    if (animClose != val) {
	        animClose = val;
	    }
	}
	
	public void animRun1_change(GTextField source, GEvent event) {
	    int val = source.getValueI();
	    if (animRun1 != val) {
	        animRun1 = val;
	        //println("--->> animRun1 = "+ animRun1);
	    }
	}
	
	public void animHold1_change(GTextField source, GEvent event) {
	    int val = source.getValueI();
	    if (animHold1 != val) {
	        animHold1 = val;
	        //println("--->> animHold1 = "+ animHold1);
	    }
	}
	
	public void animRun2_change(GTextField source, GEvent event) {
	    int val = source.getValueI();
	    if (animRun2 != val) {
	        animRun2 = val;
	        //println("--->> animRun2 = "+ animRun2);
	    }
	}
	
	public void animHold2_change(GTextField source, GEvent event) {
	    int val = source.getValueI();
	    if (animHold2 != val) {
	        animHold2 = val;
	        //println("--->> animHold2 = "+ animHold2);
	    }
	}

	public void animDuration_change(GTextField source, GEvent event) {
	    int val = source.getValueI();
	    if (animDuration != val) {
	        animDuration = val;
	    }
	}
	
	public void recordButton_hit(GButton source, GEvent event ) {
		this.isAnimating = true;
		this.isRecordingVideo = true;
		initAnimation();
	}

	/* ---------------------------------------------------------------- */
	/*                                                                  */
	/*                    END GUI Interface Code                        */
	/*                                                                  */
	/* ---------------------------------------------------------------- */


	/*----------------------------------------------------------------*/
	/*                                                                */
	/*                     BEGIN AUDIO METHODS                        */
	/*                                                                */
	/*----------------------------------------------------------------*/
	
	/**
	 * CALL THIS METHOD IN SETUP()
	 * Initializes Minim audio library and audio variables.
	 */
	public void initAudio() {
		minim = new Minim(this);
		// use the getLineOut method of the Minim object to get an AudioOutput object
		this.audioOut = minim.getLineOut(Minim.STEREO, 1024, sampleRate);
		// set the gain lower to avoid clipping from multiple voices
		setAudioGain(outputGain);
		println("---- audio out gain is "+ audioOut.getGain());
		// create Minim MultiChannelBuffers with one channel, buffer size equal to mapSize
		this.argo1Buffer = new MultiChannelBuffer(mapSize, 1);
		this.argo2Buffer = new MultiChannelBuffer(mapSize, 1);
		// initialize the signals for each buffer
		this.argo1Signal = argo1Buffer.getChannel(0);
		this.argo2Signal = argo2Buffer.getChannel(0);
		// audioLength == mapSize == argo1Signal.length == argo2Signal.length
		this.audioLength = argo1Signal.length;
		// ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
		adsr1 = new ADSRParams(0.8f, 0.1f, 0.4f, 0.5f, 0.1f);
		adsr2 = new ADSRParams(0.8f, 0.4f, 0.1f, 0.5f, 0.1f);
		// create two PASamplerInstrument synths, though buffers are all 0s at the moment
		// adsrParams will be the default ADSR for the synth
		argo1Synth = new PASamplerInstrument(argo1Buffer, audioOut.sampleRate(), 32, audioOut, adsr1);
		argo2Synth = new PASamplerInstrument(argo2Buffer, audioOut.sampleRate(), 32, audioOut, adsr2);
		// initialize mouse event tracking array
		timeLocsArray = new ArrayList<TimedLocation>();
	}

	/**
	 * Save audio buffer to a file
	 */
	public void saveToAudio(boolean isStereo) {
		renderSignals();
		if (!isStereo) {
			try {		
				AudioUtility.saveAudioToFile(argo1Signal, sampleRate, "argo1_" + fileIndex + ".wav");
			} catch (IOException e) {
				println("--->> There was an error outputting the audio file argo1_"+ fileIndex +".wav"+ e.getMessage());
			} catch (UnsupportedAudioFileException e) {
				println("--->> The file format is unsupported " + e.getMessage());
			}
			try {
				AudioUtility.saveAudioToFile(argo2Signal, sampleRate, "argo2_" + fileIndex + ".wav");
			} catch (IOException e) {
				println("--->> There was an error outputting the audio file argo2_"+ fileIndex +".wav"+ e.getMessage());
			} catch (UnsupportedAudioFileException e) {
				println("--->> The file format is unsupported " + e.getMessage());
			}
			fileIndex++;
		} 
		else {
			try {
				AudioUtility.saveStereoAudioToFile(argo1Signal, argo2Signal, sampleRate, "argo1+2_" + fileIndex + ".wav");
			} catch (IOException e) {
				println("--->> There was an error outputting the audio file argo1+2"+ fileIndex +".wav"+ e.getMessage());
			} catch (UnsupportedAudioFileException e) {
				println("--->> The file format is unsupported " + e.getMessage());
			}
			fileIndex++;
		}
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

	/**
	 * Calls Argosy to get a floating point representation of an argosy array.
	 * Values in the returned array are scaled by argosy alpha / 255.0f, 
	 * so that opacity corresponds to audio gain. The arrays from Argosy are 
	 * loaded into audio buffers so that we can hear the patterns for argo1 and argo2.
	 */
	public void renderSignals() {
		float[] sig1 = argo1.getArgosySignal(this.argo1Alpha/255.0f);
		float[] sig2 = argo2.getArgosySignal(this.argo2Alpha/255.0f);
		System.arraycopy(sig1, 0, argo1Signal, 0, sig1.length);
		System.arraycopy(sig2, 0, argo2Signal, 0, sig2.length);
        argo1Buffer.setBufferSize(argo1Signal.length);
        argo2Buffer.setBufferSize(argo2Signal.length);
		argo1Buffer.setChannel(0, argo1Signal);						
		argo2Buffer.setChannel(0, argo2Signal);						
		argo1Synth.setBuffer(argo1Buffer);
		argo2Synth.setBuffer(argo2Buffer);
		// println("--->> generated new audio signals");
	}

	/**
	 * Typically called from mousePressed with mouseX and mouseY, generates audio events.
	 * TODO play these argo1 and argo2 signals in a stereo buffer.
	 * 
	 * @param x    x-coordinate within a PixelAudioMapper's width
	 * @param y    y-coordinate within a PixelAudioMapper's height
	 */
	public void audioMousePressed(int x, int y) {
		this.sampleX = x;
		this.sampleY = y;
		argo1SamplePos = getSamplePos(argo1, x, y, argo1Step);
		argo2SamplePos = getSamplePos(argo2, x, y, argo2Step);
		panning = map(sampleX, 0, width, -0.8f, 0.8f);
		if (argo1Signal == null || argo2Signal == null || isBufferStale) {
			renderSignals();
			isBufferStale = false;
		}
		if (this.isShowArgo1) 
			playSample(argo1Synth, argo1SamplePos, calcSampleLen(), 0.6f, adsr1, panning);
		if (this.isShowArgo2) 
			playSample(argo2Synth, argo2SamplePos, calcSampleLen(), 0.6f, adsr2, panning);
	}
			
	/**
	 * Plays an audio sample with a custom envelope and stereo pan.
	 * 
	 * @param samplePos    position of the sample in the audio buffer
	 * @param samplelen    length of the sample (will be adjusted)
	 * @param amplitude    amplitude of the sample on playback
	 * @param env          an ADSR envelope for the sample
	 * @param pan          position of sound in the stereo audio field (-1.0 = left, 0.0 = center, 1.0 = right)
	 * @return the calculated sample length in samples
	 */
	public int playSample(PASamplerInstrument synth, int samplePos, int samplelen, float amplitude, ADSRParams env, float pan) {
		synth.playSample(samplePos, samplelen, amplitude, env, pitchScaling, pan);
		int durationMS = (int)(samplelen/sampleRate * 1000);
		// println("---->> adding event to timeLocsArray "+  samplelen, durationMS, millis());
		timeLocsArray.add(new TimedLocation(sampleX, sampleY, durationMS + millis()));
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
	 * Calculate position of the image pixel within the signal path,
	 * taking the shifting of pixels and audioSignal into account.
	 * See MusicBoxBuffer for use of a windowed buffer in this calculation. 
	 */
	public int getSamplePos(Argosy argo, int x, int y, int shift) {
		int pos = argo.getMapper().lookupSignalPos(x, y);
		int totalShift = argo.getArgosyPixelShift();
		// calculate how much animation has shifted the indices into the buffer
		totalShift = (totalShift + shift % mapSize + mapSize) % mapSize;
		return (pos + totalShift) % mapSize;
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
	
	public void runAnimationArray() {
		if (animationLocsArray == null || animationLocsArray.size() <= 0) return;
		int currentTime = millis();
		animationLocsArray.forEach(tl -> {
			if (tl.eventTime() < currentTime) {
				sampleX = PixelAudio.constrain(Math.round(tl.getX()), 0, width - 1);
				sampleY = PixelAudio.constrain(Math.round(tl.getY()), 0, height - 1);
				float panning = map(sampleX, 0, width, -0.8f, 0.8f);
				argo1SamplePos = getSamplePos(argo1, sampleX, sampleY, argo1Step);
				argo2SamplePos = getSamplePos(argo2, sampleX, sampleY, argo2Step);
				if (argo1Signal == null || argo2Signal == null || isBufferStale) {
					renderSignals();
					isBufferStale = false;
				}
				if (this.isShowArgo1) 
					playSample(argo1Synth, argo1SamplePos, calcSampleLen(), 0.6f, adsr1, panning);
				if (this.isShowArgo2) 
					playSample(argo2Synth, argo2SamplePos, calcSampleLen(), 0.6f, adsr2, panning);
				tl.setStale(true);
			}
		});
		animationLocsArray.removeIf(TimedLocation::isStale);
	}

	/**
	 * Draws a circle at the location of an audio trigger (mouseDown event).
	 * @param x		x coordinate of circle
	 * @param y		y coordinate of circle
	 */
	public void drawCircle(int x, int y) {
		//float size = isRaining? random(10, 30) : 60;
		fill(color(199, 220, 233, 160));
		noStroke();
		circle(x, y, 40);
	}
	
	public ArrayList<PVector> animationPoints(float w, float h, int count) {
		if (aniPoints == null) aniPoints = new ArrayList<>();
		if (animationLocsArray == null) animationLocsArray = new ArrayList<>();
		int start = millis() + 50;
		int interval = 90;
		for (int i = 0; i < count; i++) {
			float x = w/2 * cos(TWO_PI * i/count) + width/2;
			float y = h/2 * sin(TWO_PI * i/count) + height/2;
			aniPoints.add(new PVector(x, y));
			animationLocsArray.add(new TimedLocation(round(x), round(y), start + i * interval));
		}
		return aniPoints;
	}
	
	
	
}
