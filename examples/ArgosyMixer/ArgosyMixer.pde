import java.awt.Toolkit;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

//video export library
import com.hamoid.*;
//G4P library for GUI
import g4p_controls.*;
//audio library
import ddf.minim.*;
import ddf.minim.ugens.*;

//Mama's ever-lovin' blue-eyed PixelAudio library
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;

/**
 * 
 * ArgosyMixer demonstrates how to use the Argosy class to create and animate patterns and save them as a video. 
 * 
 * The Argosy class turns arrays of integers into color patterns. It steps through the Pattern
 * array to create blocks of pixels and assigns color to the blocks as it steps through the 
 * Colors arrays. The arrays don't have to be the same size--this creates variations in the 
 * pattern color sequences. The patterns fill the Argosy array, which is the same size as 
 * the pixels[] array for the display image. The Repeat value determines how many times the 
 * pattern repeats. For a Repeat value of 0, it repeats as many times as will fill the Argosy 
 * array. The way the Argosy array fill the display image is determined by the Map parameter.
 * The map is a PixelAudioMapper determined by a PixelMapGen: basically, it creates a path
 * (the "signal path") that visits every pixel in the display image once. Experiment with 
 * the paths to find out more about how they work. Read the PixelAudioMapper and PixelMapGen
 * documentation if you want detailed information about how they work.
 * 
 * ArgosyMixer provides a GUI for modifying argosy and animation
 * parameters, plus a series of key commands that can shift patterns along
 * the signal path. There are two Argosy patterns involved: the top one, 
 * Argosy 2, is transparent (Opacity = 127, initially). 
 * 
 * In the GUI, the following parameters are exposed for each Argosy pattern:
 * 
 *   Map          -- select the PixelMapGen for each Argosy instance
 *   Colors       -- select a preset palette from a drop down list
 *   Opacity      -- opacity of the colors in the palette, 0-255
 *   Pattern      -- select a preset numeric pattern from a drop down list
 *   Repeat       -- number of times to repeat the pattern; enter 0 for maximum repetitions 
 *   Unit         -- the number of pixels in each unit of the pattern
 *   Gap          -- the number of pixels between each repeated pattern
 *   Gap color    -- select a preset gap color from a drop down list
 *   Gap opacity  -- enter the alpha channel value 0-255 for the gap color
 *   >> ANIMATION <<
 *   Show         -- show or hide Argosy 1 or Argosy 2
 *   Freeze       -- freeze animation of Argosy 1 or Argsoy 2
 *   Step         -- number of pixels to shift on each animation step (negative to shift right)
 *   Open frames  -- number of frames to hold at animation start, applies both Argosy 1 and Argosy 2
 *   Close frames -- number of frames to hold at animation end, applies to both Argsoy 1 and Argosy 2
 *   Run frames   -- number of frames to animate before a hold, sets Argosy 1 and Argosy 2 separately
 *   Hold frames  -- number of frames to hold after a run of frames, sets Argosy 1 and Argosy 2 separately
 *   Duration     -- number of frames in the animation
 *   Record Video -- press to run and record animation from current display 
 *   
 *   
 * I suggest you start by experimenting with the patterns "The One" and "One-one". They create
 * create repeating patterns of one or two elements. Setting the Unit value (the number of pixels
 * in each pattern element) to a power of 2 or a sum of powers of 2 is a good place to start, 
 * especially with the Hilbert PixelMapGens in the Map menu. 
 *
 * Click on the image to hear the sounds made by the patterns with sampling rate 48KHz. The patterns
 * produce step or pulse (square) waves, so they are buzzy. Opacity will change how loud the sound is.
 * 
 * Press the spacebar to start or stop animation. 
 *   
 *   
 * --------------------------------------------------------------------------------------------
 * ***>> NOTE: Key commands only work when the image display window is the active window. <<***
 * --------------------------------------------------------------------------------------------
 * 
 * Key Commands
 * 
 * Press ' ' to toggle animation.
 * Press 'a' to shift left one argosy unit.
 * Press 'A' to shift right one argosy unit.
 * Press 'b' to shift left one argosy length.
 * Press 'B' to shift right one argosy length.
 * Press 'c' to shift left one argosy length + argosy gap.
 * Press 'C' to shift right one argosy length + argosy gap.
 * Press 'd' to move animation forward one step.
 * Press 'D' to move animation back one step.
 * Press 'l' or 'L' to shift argosies left one animation step.
 * Press 'r' or 'R' to shift argosies right one animation step.
 * Press 'p' to shift argosies left one pixel.
 * Press 'P' to shift argosies right one pixel.
 * Press 'u' or 'U' to reinitialize argosies.
 * Press 'g' or 'G' to set the pixelShift of argosies to zero (reset return point).
 * Press 'f' to freeze changes to argosy 1.
 * Press 'F' to freeze changes to argosy 2.
 * Press 'i' or 'I' to show stats about argosies.
 * Press 's' or 'S' to save current display to a PNG file.
 * Press 'v' or 'V' to toggle video recording.
 * Press 'w' or 'W' to set animation variables to beginning values.
 * Press 'z' to reset argosy 1 to initial position.
 * Press 'Z' to reset argosy 2 to inttial position.
 * Press 'h' or 'H' to show help message in console.
 * 
 * 
 * TODO save two image files, two audio files -- one for each Argosy
 * TODO bug, when changing gap color argosy resets with 0 shift
 * TODO reset animation with a key command 
 * 
 */
// PApplet settings
int imageWidth = 1536;
int imageHeight = 1024;
int bgColor = color(255, 255, 255);
// PixelAudio variables
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
// some color variables
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
int[] countToFive = {1, 2, 3, 4, 5}; 
int[] sevenFortyNine = {7, 49};
int[] fiboLSystem55 = new int[]{ 2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2,
          1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2,
          1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2 };
int[] fibo13To89 = {13, 21, 55, 89};
int[] lucas18To76 = {18, 29, 47, 76};
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
// animation variables for argo1 and argo2, set in initArgosies()
int argo1Step;
int argo2Step;
int animOpen;
int animClose;
int animRun1;
int animHold1;
int animRun2;
int animHold2;
int animDuration; 
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
/*                                                                    */
/*                          AUDIO VARIABLES                           */
/*                                                                    */
/* ------------------------------------------------------------------ */

/** Minim audio library */
Minim minim;                       // library that handles audio 
AudioOutput audioOut;              // line out to sound hardware
MultiChannelBuffer argo1Buffer;    // data structure to hold audio samples from argo1
MultiChannelBuffer argo2Buffer;    // data structure to hold audio samples from argo2
boolean isBufferStale = false;     // flags that audioBuffer needs to be reset
int sampleRate = 48000;            // a critical value for display and audio, see the setup method
float[] audioSignal;               // the audio signal as an array of floats
float[] argo1Signal;               // audio signal (float array) for argo1
float[] argo2Signal;               // audio signal (float array) for argo2
int audioLength;                   // length of the audioSignal, same as the number of pixels in the display image

// SampleInstrument setup
float sampleScale = 4;             // 
int sampleBase = (int) (sampleRate/sampleScale);
int samplelen = (int) (sampleScale * sampleBase);
Sampler audioSampler;              // minim class for sampled sound
WFInstrument instrument;           // local class to wrap audioSampler

// ADSR and params
ADSR adsr;                         // good old attack, decay, sustain, release
float maxAmplitude = 0.7f;
float attackTime = 0.8f;
float decayTime = 0.5f;
float sustainLevel = 0.125f;
float releaseTime = 0.5f;

// interaction
int sampleX;
int sampleY;
int argo1SamplePos;            // position of a mouse click along the argo1 signal path, index into the argo1 audio array
int argo2SamplePos;            // position of a mouse click along the argo2 signal path, index into the argo2 audio array
ArrayList<TimedLocation> timeLocsArray;
int count = 0;    
int fileIndex = 0;

/* ---------------- end audio variables ---------------- */


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
    bgColor = blanc;
}

// -- SELECTORS FOR MENU ITEMS -- //
// if you want the initial menus to match your settings for argo1 and argo2, 
// set them here or in initArgosies()
int argo1GenSelect;
int argo2GenSelect;
int argo1PaletteSelect;    // "Black, White"
int argo2PaletteSelect;    // "Black, White"
int argo1PatternSelect;
int argo2PatternSelect;

/**
 * Initializes argo1 and argo2 Argosy instances, sets some values for GUI, 
 * sets animation variables: your one-stop setup method for the argosies. 
 *  
 */
public void initArgosies() {
    // first Argosy instance
    argo1GenSelect = 14;          // menu item "Hilbert Random Two"
    argo1Alpha = 255;
    argo1Colors = setArgoColorsAlpha(blackWhite, argo1Alpha);
    argo1PaletteSelect = 1;      // menu item "Black, White"
    argo1Pattern = oneOne;
    argo1PatternSelect = 1;      // menu item "One-one"
    argo1Reps = 0;
    argo1Unit = 256;
    argo1Gap = 256;
    argo1GapColor = white;
    argo1GapColorIndex = 1;
    argo1IsCentered = false;     // not in the GUI
    argo1GapAlpha = 255;
    initArgo1(0);
    // second Argosy instance
    argo2GenSelect = 15;         // menu item "Hilbert Random Two"
    argo2Alpha = 127;
    argo2Colors = setArgoColorsAlpha(whiteBlack, argo2Alpha);
    argo2PaletteSelect = 2;     // menu item "White, Black"
    argo2Pattern = oneOne;
    argo2PatternSelect = 1;      // menu item "One-one"
    argo2Reps = 0;
    argo2Unit = 256;
    argo2Gap = 256;
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
    argo2Step = -1;
    animOpen = 16;
    animClose = 16;
    animRun1 = 36;
    animHold1 = 60;
    animRun2 = 16;
    animHold2 = 32;
    animDuration = 720;   // number of frames
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
        isBufferStale = true;
    }
    runTimeArray();        // animate audio event markers
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
    println("mousePressed:", mouseX, mouseY);
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
    parseKey(key, keyCode);
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
    case 'l': case 'L': { // shift argosies left one animation step
        if (!isArgo1Freeze) argo1.shiftLeft();        // shift pattern left by argosy.argoStep pixels
        if (!isArgo2Freeze) argo2.shiftLeft();        // shift pattern left by argosy.argoStep pixels
        break;
    }
    case 'r': case 'R': { // shift argosies right one animation step
        if (!isArgo1Freeze) argo1.shiftRight();        // shift pattern right by argosy.argoStep pixels
        if (!isArgo2Freeze) argo2.shiftRight();        // shift pattern right by argosy.argoStep pixels
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
        saveToAudio();
        println("Saved audio signals to stereo audio file.");
        break;
    }
    case 's': { // save current display to an PNG file
        saveImage();
        break;
    }
    case 'u': case 'U': {
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
 * Posts some information about the state of argo1 and argo2 to the console.
 */
public void showArgosyStats() {
    int maxreps1 = argo1.getMaxReps();
    int maxreps2 = argo2.getMaxReps();
    int length1 = maxreps1 * argo1PixelCount;
    int length2 = maxreps2 * argo2PixelCount;
    println("--->> Argosy 1: maxReps "+ maxreps1 +", pixel count "+ argo1PixelCount +", total pixels "+ length1 
            +", animation step "+ argo1.getArgosyStep() +", pixelShift "+ argo1.getArgosyPixelShift());
    println("--->> Argosy 2: maxReps "+ maxreps2 +", pixel count "+ argo2PixelCount+", total pixels "+ length2 
            +", animation step "+ argo2.getArgosyStep() +", pixelShift "+ argo2.getArgosyPixelShift());
}

/**
 * Posts key command help to the console.
 */
public void showHelp() {
    println(" * Press ' ' to toggle animation.");
    println(" * Press 'a' to shift left one argosy unit.");
    println(" * Press 'A' to shift right one argosy unit.");
    println(" * Press 'b' to shift left one argosy length.");
    println(" * Press 'B' to shift right one argosy length.");
    println(" * Press 'c' to shift left one argosy length + argosy gap.");
    println(" * Press 'C' to shift right one argosy length + argosy gap.");
    println(" * Press 'd' to move animation forward one step.");
    println(" * Press 'D' to move animation back one step.");
    println(" * Press 'l' or 'L' to shift argosies left one animation step.");
    println(" * Press 'r' or 'R' to shift argosies right one animation step.");
    println(" * Press 'p' to shift argosies left one pixel.");
    println(" * Press 'P' to shift argosies right one pixel.");
    println(" * Press 'u' or 'U' to reinitialize argosies.");
    println(" * Press 'g' or 'G' to set the pixelShift of argosies to zero (reset return point).");
    println(" * Press 'f' to freeze changes to argosy 1.");
    println(" * Press 'F' to freeze changes to argosy 2.");
    println(" * Press 'i' or 'I' to show stats about argosies.");
    println(" * Press 's' or 'S' to save current display to a PNG file.");
    println(" * Press 'v' or 'V' to toggle video recording.");
    println(" * Press 'w' or 'W' to set animation variables to beginning values.");
    println(" * Press 'z' to reset argosy 1 to initial position.");
    println(" * Press 'Z' to reset argosy 2 to inttial position.");
    println(" * Press 'h' or 'H' to show help message in console.");
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
 * Generates a looping fractal signal path consisting of 6 HilbertGens,
 * arranged 3 wide and 2 tall, to fit a 3 * genW by 2 * genH image. 
 * 
 * Like all the methods that follow, this one creates a MultiGen instance
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
 * unchanged. The unchanged HilbertGen starts at upper left corner and end at 
 * upper right corner, so this provides some possibilities of symmetry between rows.
 * The path is not continuous. 
 * 
 * @param rows      the number of rows in each stack
 * @param units     the number of gens in each row
 * @param genW      the width of each gen, a power of 2
 * @param genH      the height of each gen, equal to genW
 * @return          a Multigen consisting of rows * units HilbertGens
 */
public MultiGen hilbertRowOrtho(int rows, int cols, int genW, int genH) {
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
 * @param rows      the number of rows in each stack
 * @param units     the number of gens in each row
 * @param genW      the width of each gen, a power of 2
 * @param genH      the height of each gen, equal to genW
 * @return          a Multigen consisting of rows * units HilbertGens
 */
public MultiGen hilbertColumnOrtho(int rows, int cols, int genW, int genH) {
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
 * @param rows    number of horiaontal rows 
 * @param cols    number of vertical columns
 * @param genW    width of an individual PixelMapGen
 * @param genH    height of an indvidual PixelMapGen
 * @return        a MultiGen created from rows * cols PixelMapGens
 */
public MultiGen zigzagRowOrtho(int rows, int cols, int genW, int genH) {
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
 * @param rows    number of horiaontal rows 
 * @param cols    number of vertical columns
 * @param genW    width of an individual PixelMapGen
 * @param genH    height of an indvidual PixelMapGen
 * @return        a MultiGen created from rows * cols PixelMapGens
 */
public MultiGen zigzagRowAltOrtho(int rows, int cols, int genW, int genH) {
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
 * @param rows    number of horiaontal rows 
 * @param cols    number of vertical columns
 * @param genW    width of an individual PixelMapGen
 * @param genH    height of an indvidual PixelMapGen
 * @return        a MultiGen created from rows * cols PixelMapGens
 */ 
public MultiGen zigzagColumnOrtho(int rows, int cols, int genW, int genH) {
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
 * @param rows    number of horiaontal rows 
 * @param cols    number of vertical columns
 * @param genW    width of an individual PixelMapGen
 * @param genH    height of an indvidual PixelMapGen
 * @return        a MultiGen created from rows * cols PixelMapGens
 */ 
public MultiGen zigzagColumnAltOrtho(int rows, int cols, int genW, int genH) {
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
 * @param rows    number of horiaontal rows 
 * @param cols    number of vertical columns
 * @param genW    width of an individual PixelMapGen
 * @param genH    height of an indvidual PixelMapGen
 * @return        a MultiGen created from rows * cols PixelMapGens
 */ 
public MultiGen zigzagRowRandomFlip(int rows, int cols, int genW, int genH) {
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
 * @param rows    number of horiaontal rows 
 * @param cols    number of vertical columns
 * @param genW    width of an individual PixelMapGen
 * @param genH    height of an indvidual PixelMapGen
 * @return        a MultiGen created from rows * cols PixelMapGens
 */ 
public MultiGen boustrophRowRandom(int rows, int cols, int genW, int genH) {
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
 * @param rows    number of horiaontal rows 
 * @param cols    number of vertical columns
 * @param genW    width of an individual PixelMapGen
 * @param genH    height of an indvidual PixelMapGen
 * @return        a MultiGen created from rows * cols PixelMapGens
 */
public MultiGen hilbertRowRandomFlip(int rows, int cols, int genW, int genH) {
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
        gen = hilbertZigzagLoop6x4(width/6, height/4);
        break;
    case(2):
        gen = hilbertStackOrtho(3, 8, 4, imageWidth/12, imageHeight/8);
        break;
    case(3):
        gen = hilbertStackBou(3, 8, 4, imageWidth/12, imageHeight/8);
        break;
    case(4):
        gen = hilbertRowOrtho(4, 6, height/4, width/6);
        break;
    case(5):
        gen = hilbertColumnOrtho(4, 6, height/4, width/6);
        break;
    case(6):
        gen = zigzagLoop6x4(width/6, height/4);
        break;
    case(7):
        gen = zigzagRowOrtho(4, 6, width/6, height/4);
        break;
    case(8):
        gen = zigzagRowAltOrtho(4, 6, width/6, height/4);
        break;
    case(9): 
        gen = zigzagColumnOrtho(4, 6, width/6, height/4);
        break;
    case(10): 
        gen = zigzagColumnAltOrtho(4, 6, width/6, height/4);
        break;
    case(11): 
        gen = zigzagRowRandomFlip(4, 6, width/6, height/4);
        break;
    case(12): 
        gen = zigzagRowRandomFlip(8, 12, width/12, height/8);
        break;
    case(13): 
        gen = boustrophRowRandom(4, 6, width/6, height/4);
        break;
    case(14): 
        gen = hilbertRowRandomFlip(4, 6, width/6, height/4);
        break;
    case(15): 
        gen = hilbertRowRandomFlip(8, 12, width/12, height/8);
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

/*----------------------------------------------------------------*/
/*                                                                */
/*                     BEGIN AUDIO METHODS                        */
/*                                                                */
/*----------------------------------------------------------------*/

/**
 * CALL THIS METHOD IN SETUP()
 * Initializes Minim audio library and audio variables, including two MultiChannelBuffer 
 * audio buffers, one for each Argosy instance (argo1 and argo2).
 */
public void initAudio() {
    minim = new Minim(this);
    // use the getLineOut method of the Minim object to get an AudioOutput object
    this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
    // create a Minim MultiChannelBuffer with two channels
    this.argo1Buffer = new MultiChannelBuffer(1024, 2);
    this.argo2Buffer = new MultiChannelBuffer(1024, 2);
    // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
    adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
    timeLocsArray = new ArrayList<TimedLocation>();     // initialize mouse event tracking array
}
/**
 * Save audio buffer to a file
 */
public void saveToAudio() {
    renderSignals();
    try {
        saveStereoAudioToFile(argo1Signal, argo2Signal, sampleRate, "argo1+2_"+ fileIndex +".wav");
    }
    catch (IOException e) {
        println("--->> There was an error outputting the audio file wavesynth.wav "+ e.getMessage());
    }
    catch (UnsupportedAudioFileException e) {
        println("--->> The file format is unsupported "+ e.getMessage());
    }
    fileIndex++;
}

/**
 * Calls Argosy to get a floating point representation of an argosy array.
 * Values in the returneda array are scaled by argosy alpha / 255.0f, 
 * so that opacity corresponds to audio gain. The arrays from Argosy are 
 * loaded into audio buffers so that we can hear the patterns for argo1 and argo2.
 */
public void renderSignals() {
    argo1Signal = argo1.getArgosySignal(this.argo1Alpha/255.0f);
    argo2Signal = argo2.getArgosySignal(this.argo2Alpha/255.0f);
    argo1Buffer.setBufferSize(argo1Signal.length);
    argo2Buffer.setBufferSize(argo2Signal.length);
    argo1Buffer.setChannel(0, argo1Signal);                        // copy argo1Signal to channel 0 of audioBuffer
    argo2Buffer.setChannel(0, argo2Signal);                        // copy argo2Signal to channel 1 of audioBuffer
    // println("--->> generated new audio signals");
}

/**
 * Typically called from mousePressed with mouseX and mouseY, generates audio events.
 * 
 * @param x    x-coordinate within a PixelAudioMapper's width
 * @param y    y-coordinate within a PixelAudioMapper's height
 */
public void audioMousePressed(int x, int y) {
    this.sampleX = x;
    this.sampleY = y;
    argo1SamplePos = argo1.getMapper().lookupSample(x, y);
    argo2SamplePos = argo2.getMapper().lookupSample(x, y);
    if (argo1Signal == null || argo2Signal == null|| isBufferStale) {
        renderSignals();
        isBufferStale = false;
    }
    if (this.isShowArgo1) 
        playSample(argo1Buffer, argo1SamplePos, calcSampleLen(), 0.6f, new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime));
    if (this.isShowArgo2) 
        playSample(argo2Buffer, argo2SamplePos, calcSampleLen(), 0.6f, new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime));
}
    
/**
 * Plays an audio sample.
 * 
 * @param samplePos        position of the sample in the audio buffer
 * @param samplelen        length of the sample (will be adjusted)
 * @param amplitude        amplitude of the sample on playback
 * @param adsr            an ADSR envelope for the sample
 * @return                the calculated sample length in samples
 */
public int playSample(MultiChannelBuffer buffer, int samplePos, int samplelen, float amplitude, ADSR adsr) {
    // println("--- play "+ twoPlaces.format(amplitude));
    audioSampler = new Sampler(buffer, sampleRate, 8); // create a Minim Sampler from the buffer sampleRate sampling
                                                            // rate, for up to 8 simultaneous outputs 
    audioSampler.amplitude.setLastValue(amplitude);     // set amplitude for the Sampler
    audioSampler.begin.setLastValue(samplePos); // set the Sampler to begin playback at samplePos, which corresponds
                                                // to the place the mouse was clicked
    int releaseDuration = (int) (releaseTime * sampleRate); // do some calculation to include the release time.
                                                            // There may be better ways to do this.
    if (samplePos + samplelen >= mapSize) {
        samplelen = mapSize - samplePos; // make sure we don't exceed the mapSize
        println("----->>> sample length = " + samplelen);
    }
    int durationPlusRelease = this.samplelen + releaseDuration;
    int end = (samplePos + durationPlusRelease >= this.mapSize) ? this.mapSize - 1
            : samplePos + durationPlusRelease;
    // println("----->>> end = " + end);
    audioSampler.end.setLastValue(end);
    this.instrument = new WFInstrument(audioOut, audioSampler, adsr);
    // play command takes a duration in seconds
    float duration = samplelen / (float) (sampleRate);
    instrument.play(duration);
    timeLocsArray.add(new TimedLocation(sampleX, sampleY, (int) (duration * 1000) + millis()));
    // return the length of the sample
    return samplelen;
}

public int calcSampleLen() {
    float vary = (float) (PixelAudio.gauss(this.sampleScale, this.sampleScale * 0.125f)); // vary the duration of the signal 
    // println("----->>> vary = "+ vary +", sampleScale = "+ sampleScale);
    this.samplelen = (int) (vary * this.sampleBase); // calculate the duration of the sample
    return samplelen;
}

/**
 * Run the animation for audio events. 
 */
public void runTimeArray() {
    int currentTime = millis();
    timeLocsArray.forEach(tl -> {
        tl.setStale(tl.stopTime() < currentTime);
        if (!tl.isStale()) {
            drawCircle(tl.getX(), tl.getY());
        }
    });
    timeLocsArray.removeIf(TimedLocation::isStale);
}

/**
 * Draws a circle at the location of an audio trigger (mouseDown event).
 * @param x        x coordinate of circle
 * @param y        y coordinate of circle
 */
public void drawCircle(int x, int y) {
    //float size = isRaining? random(10, 30) : 60;
    fill(color(233, 220, 199));
    noStroke();
    circle(x, y, 60);
}

/**
 * Saves audio data to 16-bit integer PCM format, which Processing can also open.
 * 
 * @param samples            an array of floats in the audio range (-1.0f, 1.0f)
 * @param sampleRate        audio sample rate for the file
 * @param fileName            name of the file to save to
 * @throws IOException        
 * @throws UnsupportedAudioFileException
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
 * Saves stereo audio data to 16-bit integer PCM WAV format.
 *
 * @param leftSamples   float array for the left channel (-1.0f to 1.0f)
 * @param rightSamples  float array for the right channel (-1.0f to 1.0f)
 * @param sampleRate    sample rate in Hz (e.g., 44100)
 * @param fileName      name of the WAV file to save
 * @throws IOException
 * @throws UnsupportedAudioFileException
 */
public static void saveStereoAudioToFile(float[] leftSamples, float[] rightSamples, float sampleRate, String fileName)
        throws IOException, UnsupportedAudioFileException {
    if (leftSamples.length != rightSamples.length) {
        throw new IllegalArgumentException("Left and right channel sample arrays must have the same length.");
    }
    int totalSamples = leftSamples.length;
    byte[] audioBytes = new byte[totalSamples * 2 * 2]; // 2 bytes per sample, 2 channels
    int index = 0;
    for (int i = 0; i < totalSamples; i++) {
        int left = (int) (leftSamples[i] * 32767);
        int right = (int) (rightSamples[i] * 32767);
        // Left channel (little endian)
        audioBytes[index++] = (byte) (left & 0xFF);
        audioBytes[index++] = (byte) ((left >> 8) & 0xFF);
        // Right channel (little endian)
        audioBytes[index++] = (byte) (right & 0xFF);
        audioBytes[index++] = (byte) ((right >> 8) & 0xFF);
    }
    ByteArrayInputStream byteStream = new ByteArrayInputStream(audioBytes);
    AudioFormat format = new AudioFormat(sampleRate, 16, 2, true, false); // 2 channels for stereo
    AudioInputStream audioInputStream = new AudioInputStream(byteStream, format, totalSamples);
    File outFile = new File(fileName);
    AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outFile);
}
