import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.UnsupportedAudioFileException;

//Mama's ever-lovin' blue-eyed PixelAudio library
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;
import net.paulhertz.pixelaudio.voices.*;

//video export library
import com.hamoid.*;

//G4P library for GUI
import g4p_controls.*;

// Minim audio library
import ddf.minim.*;

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

int roig = 0xfff64c2f;
int groc = 0xfff6e959;
int blau = 0xff5990e9;
int blau2 = 0xff90b2dc;
int blanc = 0xfffef6e9;
int gris = 0xffa09faa;
int negre = 0xff080d15;
int grana = 0xffe56ad8;
int vert = 0xff7bb222;
int taronja = 0xfffea537;
int roigtar = 0xffE9907B;
int violet = 0xffb29de9;
// Standard black, gray, white colors without transparency
int black = color(0, 0, 0);
int white = color(255, 255, 255);
int gray = color(128, 128, 128);
// Argosy pattern variables
int[] theOne = new int[]{1};
int[] oneOne = new int[]{1, 1};
int[] countToFive = {1, 2, 3, 4, 5};   // use unit = gap = 64: (1 + 2 + 3 + 4 + 5) * 64 + 64 = 1024
int[] oddOneToSeven = {1, 3, 5, 7};    // first four odd numbers sum to 16
int[] fourPower = {16, 64, 256};       // use unit = 16: (16 + 64 + 256) = 336; 336 + 48 = 384; gap = 16 * 48 = 768
int[] sevenFortyNine = {7, 49};        // a gap of 8 * unit will repeat patterns in Hilbert gens
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
Argosy argo1;                // Argosy object 1, bottom layer
PixelMapGen argo1Gen;        // Gen for mapping argo1
PixelAudioMapper argo1Mapper;    // PixelAudioMapper for argo1
int[] argo1Pattern;          // array for argo1 pattern
int[] argo1Colors;           // argo1 colors
int argo1Alpha;              // alpha channel value for argo1
int argo1Reps;               // number of repetitions of argo1 pattern (0 --> fill argo1Image)
int argo1Unit;               // number of pixels in a unit for argo1
int argo1Gap;                // number of pixels in argo1 gap, between patterns
int argo1GapColor;           // color for pixels in the gap
int argo1GapColorIndex = 0;  // menu item index for argo1 gap color
int argo1GapAlpha;           // alpha channel value for argo1 gap color
boolean argo1IsCentered;     // center argosy pixels in the signal path
PImage argo1Image;           // image derived from argo1 (like mapImage in other sketches)
boolean isShowArgo1 = true;  // visibility of argo1
Argosy argo2;
// variables for second Argosy instance, just like the first Argosy instance
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

int argo1Step;                   // number of pixels to advance argo1 with each animation step
int argo2Step;                   // number of pixels to advance argo2 with each animation step
int animOpen;                    // number of frames to pause at the beginning of a video recording
int animClose;                   // number of frames to pause at the end of a video recording
int animRun1;                    // number of frames to run argo1 before holding
int animHold1;                   // number of frames to hold argo1
int animRun2;                    // number of frames to run argo2 before holding
int animHold2;                   // number of frames to hold argo2
int animDuration;                // total number of frames in the animation
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

Minim minim;                    // library that handles audio
AudioOutput audioOut;           // line out to sound hardware
boolean isBufferStale = false;  // flags that audioBuffer needs to be reset
float sampleRate = 48000;       // a critical value for display and audio, see the setup method
int audioLength;        // length of the audioSignal == number of pixels in the display image == mapSize
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
MultiChannelBuffer argo1Buffer;  // data structure to hold audio samples from argo1
MultiChannelBuffer argo2Buffer;  // data structure to hold audio samples from argo2
PASamplerInstrument argo1Synth;  // class to wrap a Minim audioSampler
PASamplerInstrument argo2Synth;  // class to wrap a Minim audioSampler
float[] argo1Signal;             // audio signal (float array) for argo1
float[] argo2Signal;             // audio signal (float array) for argo2
int argo1SamplePos;              // position of a mouse click along the argo1 signal path, index into the argo1 audio array
int argo2SamplePos;              // position of a mouse click along the argo2 signal path, index into the argo2 audio array

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
  runTimeArray();    // animate audio event markers
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
  } else {
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
  } else {
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
    } else if (keyCode == DOWN) {
      setAudioGain(g - 3.0f);
      println("---- audio gain is "+ nf(audioOut.getGain(), 0, 2));
    } 
    else if (keyCode == RIGHT) {} 
    else if (keyCode == LEFT) {}
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
  case ' ':
    { // toggle animation
      isAnimating = ! isAnimating;
      break;
    }
  case 'e':
    { // trigger elliptical trail of audio events
      animationPoints(width - 40, height - 40, 120);
      break;
    }
  case 'a':
    { // shift left one argosy unit
      if (!isArgo1Freeze) argo1.shift(argo1.getUnitSize(), true);
      if (!isArgo2Freeze) argo2.shift(argo2.getUnitSize(), true);
      break;
    }
  case 'A':
    { // shift right one argosy unit
      if (!isArgo1Freeze) argo1.shift(-argo1.getUnitSize(), true);
      if (!isArgo2Freeze) argo2.shift(-argo2.getUnitSize(), true);
      break;
    }
  case 'b':
    { // shift left one argosy length
      if (!isArgo1Freeze) argo1.shift(argo1PixelCount, true);
      if (!isArgo2Freeze) argo2.shift(argo2PixelCount, true);
      break;
    }
  case 'B':
    { // shift right one argosy length
      if (!isArgo1Freeze) argo1.shift(-argo1PixelCount, true);
      if (!isArgo2Freeze) argo2.shift(-argo2PixelCount, true);
      break;
    }
  case 'c':
    { // shift left one argosy length + argosy gap
      if (!isArgo1Freeze) argo1.shift(argo1PixelCount + argo1.getArgosyGap(), true);
      if (!isArgo2Freeze) argo2.shift(argo2PixelCount + argo2.getArgosyGap(), true);
      break;
    }
  case 'C':
    { // shift right one argosy length + argosy gap
      if (!isArgo1Freeze) argo1.shift(-argo1PixelCount - argo1.getArgosyGap(), true);
      if (!isArgo2Freeze) argo2.shift(-argo2PixelCount - argo2.getArgosyGap(), true);
      break;
    }
  case 'd':
    { // advance one animation step
      if (!isArgo1Freeze) argo1.shift(argo1Step, true);
      if (!isArgo2Freeze) argo2.shift(argo2Step, true);
      count++;
      break;
    }
  case 'D':
    { // go back one animation step
      if (!isArgo1Freeze) argo1.shift(-argo1Step, true);
      if (!isArgo2Freeze) argo2.shift(-argo2Step, true);
      count = 0;
      break;
    }
  case 'g':
  case 'G':
    { // set the pixelShift of argosies to zero (reset return point)
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
  case 'l':
    { // shift argosies left one animation step
      if (!isArgo1Freeze) argo1.shiftLeft();    // shift pattern left by argo1.argoStep pixels
      if (!isArgo2Freeze) argo2.shiftLeft();    // shift pattern left by argo2.argoStep pixels
      break;
    }
  case 'L':
    { // shift argosies left one animation step
      if (!isArgo1Freeze) argo1.shift(argoStep, true);    // shift pattern left by this.argoStep pixels
      if (!isArgo2Freeze) argo2.shift(argoStep, true);    // shift pattern left by this.argoStep pixels
      break;
    }
  case 'r':
    { // shift argosies right one animation step
      if (!isArgo1Freeze) argo1.shiftRight();    // shift pattern right by argo1.argoStep pixels
      if (!isArgo2Freeze) argo2.shiftRight();    // shift pattern right by argo2.argoStep pixels
      break;
    }
  case 'R':
    { // shift argosies right one animation step
      if (!isArgo1Freeze) argo1.shift(-argoStep, true);    // shift pattern right by this.argoStep pixels
      if (!isArgo2Freeze) argo2.shift(-argoStep, true);    // shift pattern right by this.argoStep pixels
      break;
    }
  case 'p':
    { // shift argosies left one pixel
      if (!isArgo1Freeze) argo1.shift(1, true);
      if (!isArgo2Freeze) argo2.shift(1, true);
      break;
    }
  case 'P':
    { // shift argosies right one pixel
      if (!isArgo1Freeze) argo1.shift(-1, true);
      if (!isArgo2Freeze) argo2.shift(-1, true);
      break;
    }
  case 'f':
    { // freeze changes to argosy 1
      isArgo1Freeze = !isArgo1Freeze;
      argo1Freeze.setSelected(isArgo1Freeze);
      println("isArgo1Freeze is "+ isArgo1Freeze);
      break;
    }
  case 'F':
    { // freeze changes to argosy 2
      isArgo2Freeze = !isArgo2Freeze;
      argo2Freeze.setSelected(isArgo2Freeze);
      println("isArgo2Freeze is "+ isArgo2Freeze);
      break;
    }
  case 'i':
  case 'I':
    { // show stats about argosies
      showArgosyStats();
      break;
    }
  case 'S':
    { // save current display to an PNG file
      saveToAudio(true);
      println("Saved audio signals to stereo audio file.");
      break;
    }
  case 's':
    { // save current display to an PNG file
      saveImage();
      break;
    }
  case 'u':
  case 'U':
    { // reinitialize any argosies that aren't frozen
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
  case 'v':
  case 'V':
    { // toggle video recording
      isRecordingVideo = !isRecordingVideo;
      if (!isRecordingVideo) {
        println("-- video recording is off");
        if (videx != null) {
          videx.endMovie();
        }
      } else {
        println("-- video recording is on, press spacebar to toggle animation");
        initAnimation();
      }
      break;
    }
  case 'w':
    {    // reset animation tracking
      initAnimation();
      println("-- reset animation variables");
      break;
    }
  case 'W':
    {    // reset animation tracking
      initAnimation();
      println("-- reset animation variables");
      break;
    }
  case 'z':
    { // reset argosy 1 to initial position
      argo1.shift(-argo1.getArgosyPixelShift(), true);
      break;
    }
  case 'Z':
    { // reset argosy 2 to inttial position
      argo2.shift(-argo2.getArgosyPixelShift(), true);
      break;
    }
  case 'h':
  case 'H':
    { // show help message in console
      showHelp();
      break;
    }
  default:
    {
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
  println(" * Press the UP arrow to increase audio output gain by 3.0 dB.");
  println(" * Press the DOWN arrow to decrease audio output gain by 3.0 dB.");
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
