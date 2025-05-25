//Java imports
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.util.Comparator;
import java.util.Collections;
import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

// Mama's ever-lovin' blue-eyed baby library
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.WaveData.WaveState;

// audio library
import ddf.minim.*;
import ddf.minim.ugens.*;
// video export library
import com.hamoid.*;
// GUI library for Processing
import g4p_controls.*;

/**
 * PixelAudio demo application WaveSynthAnimation created by Paul Hertz,  
 * makes hypnotic animated patterns and outputs them to video. 
 * 
 * Use this application to edit a PixelAudio WaveSynth, including its individual WaveData
 * operators, using a nice GUI (g4p_controls) for Processing. This should provide some idea 
 * of what you can do with the HilbertGen for making patterns with the WaveSynth. There
 * are lots of other possibilities. Patterns can be loaded from and saved to JSON files. 
 * 
 * For audio signals, a WaveSynth behaves like an audio synthesizer that adds together 
 * sine waves at different frequencies. The BigWaveSynthAudio and WaveSynthSequencer 
 * examples show how to produce audio with a WaveSynth. This example provides a graphical
 * user interface for editing the colors and other properties of a WaveSynth. 
 * 
 * Click on the WaveSynth image to hear what it sounds like. Note that the appearance of the
 * image is determined by the current sampling frequency, set in the initWaveSynth() method.
 * For a higher sampling rate, there are more samples. One sampling rate we use for the 
 * WaveSynth Editor is the number of pixels in the WaveSynth image, 1024 * 1024 = 1048576.
 * Though it may have more or less samples, the sound of the audio will not vary, as its 
 * frequency is governed by the sampling rate. If you want to save the audio to a file, 
 * you should probably set a standard sampling rate like 48000 in the initWaveSynth() method.
 * 
 * A WaveSynth is organized around attributes, such as gain (i.e. loudness or brightness) 
 * and gamma (a sort of contrast setting), and data objects. The data objects include a 
 * a bitmap, mapImage, that is a Processing PImage instance for the image representation
 * of the WaveSynth, a PixelAudioMapper that allows the WaveSynth to mediate between audio 
 * data and image data using arrays for the audio signal and the image data ordered along the 
 * PixelAudioMapper signal path, and an array of WaveData objects that define the individual
 * sine wave components of the WaveSynth. 
 * 
 * When a WaveSynth is used to produce color patterns, each WaveData object in the waveDataList
 * controls a color. The colors of the various WaveData objects are added together. The 
 * amplitude of the controlling sine wave controls the brightness of each color. The control
 * panel in this example allows to isolate individual WaveData operators to see how they 
 * affect the color patterns. 
 * 
 * SAMPLING RATES FOR AUDIO AND FOR WAVESYNTH IMAGES
 * 
 * We use different sampling rates for audio playback and recording and for WaveSynth's 
 * additive synthesis algorithm. We use the standard sampling rate 48000 for audio. 
 * For the WaveSynth, which in this demo app relies on Hilbert curves to make patterns, 
 * we use (genWidth * genWidth) as the sampling rate for the sine waves that are added 
 * together to produce the WaveSynth image. If genWidth = 512, this value is 262144.
 * You can change the audio sampling rate in the instance variables list or in setup(). 
 * You can change WaveSynth's sampling rate in the initWaveSynth() method. 
 * 
 * 
 * In addition to the GUI commands, there are some useful key commands.
 *
 * --------------------------------------------------------------------------------------------
 * ***>> NOTE: Key commands only work when the image display window is the active window. <<***
 * --------------------------------------------------------------------------------------------
 * 
 * Key commands will not work when the control panel is the active window.
 * Click on the display window to make it the active window and then try the commands. 
 * See the parseKey() method and the methods it calls for more information about key commands.
 * 
 * The quickest way to record a video, from frame 0 to the stop frame value in the 
 * control panel, is to press the 'V' (capital 'v') key. 
 * 
 * The code in this example is extensively annotated. We the author heartily recommend you 
 * read the notes for the various methods. 
 * 
 * press ' ' to turn animation on or off.
 * press 'a' to scale all active WaveSynth amplitudes by ampFac.
 * press 'A' to scale all active WaveSynth amplitudes by 1/ampFac.
 * press 'c' to shift all active WaveSynth colors by colorShift * 360 degrees in the HSB color space.
 * press 'C' to shift all active WaveSynth colors by -colorShift * 360 degrees in the HSB color space.
 * press 'd' to print animation data to the console.
 * press 'D' to print WaveSynth data to the console.
 * press 'f' to scale all active WaveSynth frequencies by freqFac.
 * press 'F' to scale all active WaveSynth frequencies by 1/freqFac.
 * press 'p' to shift all active WaveSynth phases by phaseFac.
 * press 'P' to shift all active WaveSynth phases by -phaseFac.
 * press 'k' to show all current phase values in the console.
 * press 'K' to set all phase values so that first frame looks like the current frame, then go to first frame.
 * press '=' or '+' to make the image brighter.
 * press '-' or '_' to make the image darker.
// ------------- COMMANDS FOR ANIMATION STEPPING ------------- //
 * press 'e' to fast forward animation 1/8 of total steps.
 * press 'E' to rewind animation 1/8 of total steps (loops back from end, if required).
 * press 'i' to reset current animation step to initial value, 0.
 * press 'u' to advance animation by 1 step.
 * press 'U' to advance animation by 10 steps.
 * press 'y' to rewind animation by 1 step.
 * press 'Y' to rewind animation by 10 steps.
 * press 'l' or 'L' to toggle animation looping.
// ------------- MUTING COMMANDS ------------- //
 * press keys 1-8 to mute or unmute first eight wave data operators
 * press 'm' to print current WaveData states to console.
 * press 'M' to unmute all current WaveData operators.
// ------------- JSON COMMANDS ------------- //
 * press 'j' or 'J' to save WaveSynth settings to a JSON file.
 * press 'o' to open a new JSON file.
 * press 'O' to reload the current JSON file, if there is one, reverting all edits.
// ------------- MISCELLANEOUS COMMANDS ------------- //
 * press 'r' to toggles display window to fit screen or display at size.
 * press 's' to save the current image to a .png file.
 * press 'v' to toggle video recording.
 * press 'V' to record a complete video loop from frame 0 to stop frame.
 * press 't' to sort wave data operators in control panel by frequency (lowest first), useful when saving to JSON
 * press 'h' or 'H' to show this help message in the console.
 * 
 */

int bgFillColor;
/** 
 * Set designMode true to use designWidth and designHeight for display width
 * and height. Set to false to use renderWidth and renderHeight for display
 * width and height. Set renderWidth and renderHeight to the dimensions 
 * of the PixelMapGen you want render and animate.
 */
boolean isDesignMode = true;
int genWidth = isDesignMode ? 1024 : 512;         // for Hilbert and Moore curves, genWidth must be a power of 2
int genHeight = isDesignMode ? 1024 : 512;        // for Hilbert and Moore curves, genHeight must be a power of 2
int designWidth = genWidth;
int designHeight = genHeight;
int renderWidth = 3 * genWidth;
int renderHeight = 2 * genHeight;

// display window sizes for resizing images to fit the screen, 
// most are calculated by the setScaling() method
boolean isOversize = false;            // if false, image is not too big to display
boolean isFitToScreen = false;         // is the image currently fit to the screen? 
int maxWindowWidth;                    // largest window width
int maxWindowHeight;                   // largest window height
int scaledWindowWidth;                 // scaled window width
int scaledWindowHeight;                // scaled window height
float windowScale = 1.0f;              // scaling ratio, used to calculate scaled mouse location

// PixelAudio vars and objects
PixelAudio pixelaudio;      // our shiny new library
HilbertGen hGen;            // a PixelMapGen to draw Hilbert curves
MooreGen mGen;              // a PixelMapGen to draw Moore curves
DiagonalZigzagGen zGen;     // a PixelMapGen to draw zigzag curves
MultiGen multigen;          // a PixelMapGen that handles multiple gens
PixelMapGen gen;            // any PixelMapGen
PixelAudioMapper mapper;    // object for reading, writing, and transcoding audio and image data
int mapSize;                // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
PImage mapImage;            // image for display
PGraphics offscreen;        // offscreen PGraphics
int[] rgbSignal;            // the pixel values in mapImage, in the order the signal path visits them

// WaveSynth vars
ArrayList<WaveData> wdList;    // list of WaveData objects used by a WaveSynth
WaveSynth wavesynth;           // a WaveSynth to generate patterns from additive synthesis of sine waves
ArrayList<WaveSynth> wsCrew;   // a list of WaveSynths
WaveData currentWD;            // current WaveData object, for editing
int waveDataIndex;             // index of currentWD in wavesynth.waveDataList

// file IO for JSON and video output
File currentDataFile;          // current JSON data file, if one is loaded
String currentFileName;        // name of the current data file
String videoFilename = "waveSynth_study.mp4";    // default video name, JSON file can set this
String comments;               // comments in JSON file
int snapCount = 13;            // number of snapshots to save while rendering video
boolean    isCaptureFrames = false;    // set to true to capture snapshots which rendering video
VideoExport videx;             // hamoid library class for video export (requires ffmpeg)
JSONObject json;               // JSON data
String dataPath;               // path to application data
String videoPath;              // directory for saving video
String jsonFile = "test327.json";
String jsonFolder = "JSON";

/* -------------------------- RENDER SETTINGS -------------------------- */
/** Sets how much to shift phase with shiftPhases() */
float phaseShift = (float) (Math.PI * 1.0 / 1536.0);
/** Sets how much to shift colors with shiftColors() 
 * 1.0f/24 is 15 degrees around the 360-degree HSB color circle, 
 * expressed as a fraction in the range (0..1).
 */
float colorShift = 1.0f/24;
/** Sets how much to scale frequencies with scaleFreqs() */
float freqFac = (float) (Math.sqrt(2.0));
/** Sets how much to scale amplitudes with scaleAmps() */
float ampFac = 0.9375f; // 15/16
/** Sets the increment to apply to wavesynth gain (brightness) */
float gainInc = 0.03125f;

boolean isAnimating = false;          
boolean isLooping = false;
boolean oldIsAnimating;
int animSteps = 720;                    // how many steps in an animation loop
int animStop = animSteps;               // step where animation recording stops
boolean isRecordingVideo = false;       // are we recording? (only if we are animating)
int videoFrameRate = 24;                // fps
int step;                               // number of current step in animation loop
int startTime;                          // set when animation starts
int stopTime;                           // used to calculate animation time until finish and duration

/* ---------------------------------------------------------------------- */
boolean isVerbose = true;               // if true, post lots of debugging messages to console
boolean isSecondScreen = false;         // for a two screen display
int screen2x;                           // second screen x-coord, will be set by setScaling()
int screen2y;                           // second window y-coord, will be set by setScaling()

/* ------------------------------------------------------------------ */
/*                                                                    */
/*                          AUDIO VARIABLES                           */
/*                                                                    */
/* ------------------------------------------------------------------ */
/** Minim audio library */
Minim minim;                       // library that handles audio 
AudioOutput audioOut;              // line out to sound hardware
MultiChannelBuffer audioBuffer;    // data structure to hold audio samples
boolean isBufferStale = false;     // flags that audioBuffer needs to be reset: i.e., after loading JSON data to wavesynth
int sampleRate = 48000;            // sample rate for audio playback and output to file, see notes above
float[] audioSignal;               // the audio signal as an array of floats
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
int samplePos;            // position of a mouse click along the signal path, index into the audio array
ArrayList<TimedLocation> timeLocsArray;
int count = 0;    
int wsIndex = 0;

/* ---------------- end audio variables ---------------- */


/**
 * A call to the settings() method is required when setting size from variables
 */
public void settings() {
    if (isDesignMode) {
        size(designWidth, designHeight);
    }
    else {
        size(renderWidth, renderHeight);
    }
}

/**
 * Prepare for launch by setting all variables required for the draw() loop.
 * Initialize the PixelAudio library, then create a PixelMapGen and a PixelAudioMapper
 * Next set up a WaveSynth and point our global mapImage to the WaveSynth's variable 
 * mapImage. Check for extra displays, decide where to show the application window, and 
 * set the window scaling variables. Finally, build the GUI (using G4P library).
 */
public void setup() {
    bgFillColor = color(0, 0, 0, 255);
    pixelaudio = new PixelAudio(this);
    initAudio();
    gen = isDesignMode ? createHilbertGen(genWidth) : hilbertLoop3x2(genWidth, genHeight);
    mapper = new PixelAudioMapper(gen);
    mapSize = mapper.getSize();                    // size of the image, and of various other entities
    wdList = initWaveDataList();
    wavesynth = new WaveSynth(mapper, wdList);
    initWaveSynth(wavesynth);
    currentWD = wavesynth.waveDataList.get(0);
    mapImage = wavesynth.mapImage;
    listDisplays();
    setScaling();
    if (isOversize) {
        isFitToScreen = true;
        resizeWindow();
        println("Window is resized");
    }
    createGUI();
    showHelp();
}

/**
 * @param edgeLength    length in pixels of Hilbert curve, must be a power of 2. 
 * @return              a HilbertGen with its mapping arrays initialized
 */
public HilbertGen createHilbertGen(int edgeLength) {
    return new HilbertGen(edgeLength, edgeLength);        
}

/**
 * Generates a looping fractal signal path consisting of 6 HilbertGens,
 * arranged 3 wide and 2 tall, to fit a 3 * genW by 2 * genH image. 
 * This particular MultiGen configuration is used so extensively in my 
 * sample code that I've given it a factory method in the HilbertGen class.
 * It's written out here so you can see how it works. 
 * 
 * Note that genH must equal genW and both must be powers of 2. For the 
 * image size we're using in this example, genW = image width / 3 and 
 * genH = image height / 2.
 * 
 * @param genW    width of each HilbertGen 
 * @param genH    height of each HilbertGen
 * @return              a 3 x 2 array of Hilbert curves, connected in 
 *                      a loop (3 * genWidth by 2 * genHeight pixels)
 */
public MultiGen hilbertLoop3x2(int genW, int genH) {
    // list of PixelMapGens that create an image using mapper
    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
    // list of x,y coordinates for placing gens from genList
    ArrayList<int[]> offsetList = new ArrayList<int[]>();
    genList.add(new HilbertGen(genW, genH, PixelMapGen.fx270));
    offsetList.add(new int[] { 0, 0 });
    genList.add(new HilbertGen(genW, genH, PixelMapGen.nada));
    offsetList.add(new int[] { genW, 0 });
    genList.add(new HilbertGen(genW, genH, PixelMapGen.fx90));
    offsetList.add(new int[] { 2 * genW, 0 });
    genList.add(new HilbertGen(genW, genH, PixelMapGen.fx90));
    offsetList.add(new int[] { 2 * genW, genH });
    genList.add(new HilbertGen(genW, genH, PixelMapGen.r180));
    offsetList.add(new int[] { genW, genH });
    genList.add(new HilbertGen(genW, genH,PixelMapGen.fx270));
    offsetList.add(new int[] { 0, genH });
    return new MultiGen(3 * genW, 2 * genH, offsetList, genList);
}

/**
 * @return an ArrayList of WaveData objects used to initialize a WaveSynth
 */
public ArrayList<WaveData> initWaveDataList() {
    ArrayList<WaveData> list = new ArrayList<WaveData>();
    float frequency = 768.0f;
    float amplitude = 0.8f;
    float phase = 0.0f;
    float dc = 0.0f;
    float cycles = 1.0f;
    int waveColor = color(159, 190, 251);
    int steps = this.animSteps;
    WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
    list.add(wd);
    frequency = 192.0f;
    phase = 0.0f;
    cycles = 2.0f;
    waveColor = color(209, 178, 117);
    wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
    list.add(wd);
    return list;
}

/**
 * Sets the initial values of a WaveSynth instance. Note particularly how varying
 * sampleRate can change the appearance of the WaveSynth mapImage. SampleRate should 
 * not change the sound of the audio signal -- only the image changes. Some sampling 
 * rates are not standard for saving to file, so you may want to use a standard rate
 * such as 48000Hz if you want to save audio to a file. 
 * 
 * @param synth    a WaveSynth instance
 * @return         the WaveSynth with initial values set
 */
public WaveSynth initWaveSynth(WaveSynth synth) {
    synth.setGain(0.8f);
    synth.setGamma(1.0f);
    synth.setScaleHisto(false);
    synth.setAnimSteps(this.animSteps);
    synth.setSampleRate(genWidth * genWidth);
    // some other possible sampling rates
    // synth.setSampleRate(genWidth / 2 * genWidth / 2);
    // synth.setSampleRate(genWidth / 4 * genWidth / 4);
    // synth.setSampleRate(gen.getWidth() * gen.getHeight());
    // synth.setSampleRate(48000);
    // See the PixelAudio class for some public static final values for sampleRate
    println("\n====================================================");
    println("--- mapImage size = " + synth.mapImage.pixels.length);
    println("--- WaveSynth sample rate = " + synth.getSampleRate());
    println("====================================================\n");
    synth.prepareAnimation();
    synth.renderFrame(0);
    return synth;
}    

public void draw() {
    // draw the image 
    // mapImage points to wavesynth.mapImage, which gets updated by animation, etc.
    image(mapImage,0, 0, width, height);
    // do one step of animation, if conditions are right
    if (isAnimating) {
        stepAnimation();
    }
    runTimeArray();        // animate audio event markers
}

/**
 * Step through the animation, called by the draw() method.
 * Will also record a frame of video, if we're recording.
 */
public void stepAnimation() {
    if (step >= animStop) {
        println("--- Completed video at frame " + animStop);
        if (!isLooping) {
            isAnimating = false;
        }
        step = 0;
        if (isRecordingVideo) {
            isRecordingVideo = false;
            videx.endMovie();
        }
    } 
    else {
        step += 1;
        if (isRecordingVideo) {
            if (videx == null) {
                println("----->>> start video recording ");
                videx = new VideoExport(this, videoFilename);
                videx.setFrameRate(wavesynth.videoFrameRate);
                videx.startMovie();
            }
            videx.saveFrame();
            println("-- video recording frame " + step + " of " + animStop);
        }
    }
    renderFrame(step);
}

public void renderFrame(int frame) {
    wavesynth.renderFrame(frame);
}


/********************************************************************/
/* ----->>>             DISPLAY SCALING METHODS            <<<----- */
/********************************************************************/

/**
 * Get a list of available displays and output information about them to the console.
 * Sets screen2x, screen2y, displayWidth and displayHeight from dimensions of a second display.
 */
void listDisplays() {
    // Get the local graphics environment
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    // Get the array of graphics devices (one for each display)
    GraphicsDevice[] devices = ge.getScreenDevices();
    this.isSecondScreen = (devices.length > 1);
    println("Detected displays:");
    for (int i = 0; i < devices.length; i++) {
        GraphicsDevice device = devices[i];
        // Get the display's configuration
        GraphicsConfiguration config = device.getDefaultConfiguration();
        Rectangle bounds = config.getBounds(); // Screen dimensions and position
        println("Display " + (i + 1) + ":");
        println("  Dimensions: " + bounds.width + " x " + bounds.height);
        println("  Position: " + bounds.x + ", " + bounds.y);
        if (i == 1) {
            // second screen details
            this.screen2x = bounds.x + 8;
            this.screen2y = bounds.y + 8;
            this.displayWidth = bounds.width;
            this.displayHeight = bounds.height;
        }
    }
}
      
/**
 * Calculates window sizes for displaying mapImage at actual size and at full screen. 
 * Press the 'r' key to resize the display window.
 * This method will result in display on a second screen, if one is available. 
 * If mapImage is smaller than the screen, mapImage is displayed at size on startup 
 * and resizing zooms the image. 
 * If mapImage is bigger than the display, mapImage is fit to the screen on startup
 * and resizing shows it at full size, partially filling the window. 
 * 
 */
public void setScaling() {
    // max window width is a little less than the screen width of the screen
    maxWindowWidth = displayWidth - 80;
    // leave window height some room for title bar, etc.
    maxWindowHeight = displayHeight - 80;
    float sc = maxWindowWidth / (float) mapImage.width;
    scaledWindowHeight = Math.round(mapImage.height * sc);
    if (scaledWindowHeight > maxWindowHeight) {
        sc = maxWindowHeight / (float) mapImage.height;
        scaledWindowHeight = Math.round(mapImage.height * sc);
        scaledWindowWidth = Math.round(mapImage.width * sc);
    } 
    else {
        scaledWindowWidth = Math.round(mapImage.width * sc);
    }
    // even width and height allow ffmpeg to save to video
    scaledWindowWidth += (scaledWindowWidth % 2 != 0) ? 1 : 0;
    scaledWindowHeight += (scaledWindowHeight % 2 != 0) ? 1 : 0;
    isOversize = (mapImage.width > scaledWindowWidth || mapImage.height > scaledWindowHeight);
    windowScale = (1.0f * mapImage.width) / scaledWindowWidth;
    println("maxWindowWidth " + maxWindowWidth + ", maxWindowHeight " + maxWindowHeight);
    println("image width " + mapImage.width + ", image height " + mapImage.height);
    println("scaled width " + scaledWindowWidth + ", scaled height " + scaledWindowHeight + ", "
            + "oversize image is " + isOversize);
}
public void resizeWindow() {
    if (isFitToScreen) {
        surface.setSize(scaledWindowWidth, scaledWindowHeight);
    } 
    else {
        surface.setSize(mapImage.width, mapImage.height);
    }
}
// ------------- END DISPLAY SCALING METHODS ------------- //


public void mousePressed() {
    // Demo of how to scale mousePressed events works when window is resized.
    println("mousePressed:", mouseX, mouseY);
    if (this.isFitToScreen) {
        // window is resized
        println("-- scaled coords:", (int)(mouseX * windowScale), (int)(mouseY * windowScale));
    }
    audioMousePressed(mouseX, mouseY);
}

public void keyPressed() {
    parseKey(key);
}

/**
 * ParseKey handles keyPressed events. Unlike keyPressed, it can be called by other methods.
 * Note that keyPressed events only work when the image window, not the GUI control panel, 
 * is the active window. Click in the image display window to make it the active window.
 * @param theKey    char value of the key that was pressed
 */
public void parseKey(char theKey) {
    switch(theKey) {
    case ' ':
        // turn animation on or off
        toggleAnimation();
        break;
    case 'a':
        // scale all active WaveSynth amplitudes by ampFac
        scaleAmps(wavesynth.getWaveDataList(), ampFac);
        loadWaveDataPanelValues(currentWD);
        isBufferStale = true;
        break;
    case 'A':
        // scale all active WaveSynth amplitudes by 1/ampFac
        scaleAmps(wavesynth.getWaveDataList(), 1/ampFac);
        loadWaveDataPanelValues(currentWD);
        isBufferStale = true;
        break;
    case 'c':
        // shift all active WaveSynth colors by colorShift * 360 degrees in the HSB color space
        shiftColors(wavesynth.getWaveDataList(), colorShift);
        wavesynth.updateWaveColors();
        refreshGlobalPanel();
        break;
    case 'C':
        // shift all active WaveSynth colors by -colorShift * 360 degrees in the HSB color space
        shiftColors(wavesynth.getWaveDataList(), -colorShift);
        wavesynth.updateWaveColors();
        refreshGlobalPanel();
        break;
    case 'd':
        // print animation data to the console
        println(isAnimating ? "-- running animation frame " + step + " of " + animStop : "-- stopped at frame " + step +" of " + animStop);
        break;
    case 'D':
        // print WaveSynth data to the console
        println(wavesynth.toString());
        break;
    case 'f':
        // scale all active WaveSynth frequencies by freqFac
        scaleFreqs(wavesynth.getWaveDataList(), freqFac);
        loadWaveDataPanelValues(currentWD);
        isBufferStale = true;
        break;
    case 'F':
        // scale all active WaveSynth frequencies by 1/freqFac
        scaleFreqs(wavesynth.getWaveDataList(), 1/freqFac);
        loadWaveDataPanelValues(currentWD);
        isBufferStale = true;
        break;
    case 'p':
        // shift all active WaveSynth phases by phaseFac
        shiftPhases(wavesynth.getWaveDataList(), phaseShift);
        loadWaveDataPanelValues(currentWD);
        isBufferStale = true;
        break;
    case 'P':
        // shift all active WaveSynth phases by -phaseFac
        shiftPhases(wavesynth.getWaveDataList(), -phaseShift);
        loadWaveDataPanelValues(currentWD);
        isBufferStale = true;
        break;
    case 'k':
        // show all current phase values in the console
        showPhaseValues(wavesynth.getWaveDataList());
        break;
    case 'K':
        // set all phase values so that first frame looks like the current frame, then go to first frame
        capturePhaseValues(wavesynth.getWaveDataList());
        step = 0;
        renderFrame(step);
        break;
    case '+': case '=':
        // make the image brighter
        wavesynth.setGain(wavesynth.gain + gainInc);
        refreshGlobalPanel();
        if (!isAnimating) renderFrame(step);
        break;
    case '-': case '_':
        // make the image darker
        wavesynth.setGain(wavesynth.gain - gainInc);
        refreshGlobalPanel();
        if (!isAnimating) renderFrame(step);
        break;
    // ------------- BEGIN COMMANDS FOR ANIMATION STEPPING ------------- //
    case 'e':
        // fast forward animation 1/8 of total steps
        step = (step + animSteps/8) % animSteps;
        renderFrame(step);
        println("-- step = "+ step);
        break;
    case 'E':
        // rewind animation 1/8 of total steps (loops back from end, if required)
        int leap = animSteps/8;
        step = (step > leap) ? step - leap : animSteps - (leap - step);
        renderFrame(step);
        println("-- step = "+ step);
        break;
    case 'i':
        // reset current animation step to initial value, 0
        step = 0;
        renderFrame(0);
        println("-- step = "+ step);
        break;
    case 'u':
        // advance animation by 1 step
        step = (step + 1) % animSteps;
        renderFrame(step);
        println("-- step = "+ step);
        break;
    case 'U':
        // advance animation by 10 steps
        step = (step + 10) % animSteps;            
        renderFrame(step);
        println("-- step = "+ step);
        break;
    case 'y':
        // rewind animation by 1 step
        step = (step > 0) ? (step - 1) : animSteps - 1;
        renderFrame(step);
        println("-- step = "+ step);
        break;
    case 'Y':
        // rewind animation by 10 steps
        step = (step > 10) ? (step - 10) : animSteps - (10 - step);
        renderFrame(step);
        println("-- step = "+ step);
        break;
    case 'l': case 'L':
        // toggle animation looping on or off
        toggleLooping();
        break;
    // ------------- END COMMANDS FOR ANIMATION STEPPING ------------- //
    // ------------- BEGIN MUTING COMMANDS ------------- //
    case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': 
        // keys 1-8 mute or unmute WaveDataList elements 0-7
        int k = Character.getNumericValue(theKey) - 1;
        toggleWDMute(k);
        refreshGlobalPanel();
        isBufferStale = true;
        break;
    case 'm':
        // print current WaveData states to console
        printWDStates(wavesynth.getWaveDataList());
        break;
    case 'M':
        unmuteAllWD(wavesynth.getWaveDataList());
        refreshGlobalPanel();
        isBufferStale = true;
        break;
    // ------------- END MUTING COMMANDS ------------- //
    case 'j': case 'J':
        // save WaveSynth settings to a JSON file
        saveWaveData();
        break;
    case 'o':
        // open a new JSON file
        loadWaveData();
        isBufferStale = true;
        break;
    case 'O':
        // reload the current JSON file, if there is one, reverting all edits
        if (this.currentDataFile == null) {
            loadWaveData();
            isBufferStale = true;
        }
        else {
            fileSelectedOpen(currentDataFile);
            isBufferStale = true;
            if (isVerbose) println("--->> reloaded JSON file");
        }
        break;
    case 'r':
        // toggles display window to fit screen or display at size
        isFitToScreen = !isFitToScreen;
        resizeWindow();
        println("----->>> window width: "+ width +", window height: "+ height);
        break;
    case 's':
        // save the current image to a .png file
        if (currentDataFile != null) {
            java.nio.file.Path path = java.nio.file.Paths.get(currentDataFile.getAbsolutePath());
            String fname = path.getFileName().toString();
            fname = fname.substring(0, fname.length() - 5);
            println("----->>> fname = "+ fname);
            mapImage.save(fname + ".png");
          }
          else {
            mapImage.save("wavesynth_"+ ".png");
          }
        
        break;
    case 'v':
        // toggle video recording
        toggleRecording();
        break;
    case 'V':
        // records a complete video loop with following actions:
        // Go to frame 0, turn recording on, turn animation on.
        // This will record a complete video loop, from frame 0 to the
        // stop frame value in the GUI control panel.
        step = 0;
        renderFrame(step);
        isRecordingVideo = true;
        isAnimating = true;
    case 't':
        // sort wave data operators in control panel by frequency (lowest first), useful when saving to JSON
        Collections.sort(wavesynth.waveDataList, new CompareWaveData());
        currentWD = wavesynth.waveDataList.get(0);
        wavesynth.prepareAnimation();
        refreshGlobalPanel();
        if (!isAnimating) renderFrame(step);
        if (isVerbose) {
            println("--->> Sorted wave data operators by frequency.");
        }
        break;
    case 'h': case 'H':
        showHelp();
        break;
    default:
        break;
    }
    
}
public void showHelp() {
    println("\n * press ' ' to turn animation on or off.");
    println(" * press 'a' to scale all active WaveSynth amplitudes by ampFac.");
    println(" * press 'A' to scale all active WaveSynth amplitudes by 1/ampFac.");
    println(" * press 'c' to shift all active WaveSynth colors by colorShift * 360 degrees in the HSB color space.");
    println(" * press 'C' to shift all active WaveSynth colors by -colorShift * 360 degrees in the HSB color space.");
    println(" * press 'd' to print animation data to the console.");
    println(" * press 'D' to print WaveSynth data to the console.");
    println(" * press 'f' to scale all active WaveSynth frequencies by freqFac.");
    println(" * press 'F' to scale all active WaveSynth frequencies by 1/freqFac.");
    println(" * press 'p' to shift all active WaveSynth phases by phaseFac.");
    println(" * press 'P' to shift all active WaveSynth phases by -phaseFac.");
    println(" * press 'k' to show all current phase values in the console.");
    println(" * press 'K' to set all phase values so that first frame looks like the current frame, then go to first frame.");
    println(" * press '=' or '+' to make the image brighter.");
    println(" * press '-' or '_' to make the image darker.");
    println("// ------------- COMMANDS FOR ANIMATION STEPPING ------------- //");
    println(" * press 'e' to fast forward animation 1/8 of total steps.");
    println(" * press 'E' to rewind animation 1/8 of total steps (loops back from end, if required).");
    println(" * press 'i' to reset current animation step to initial value, 0.");
    println(" * press 'u' to advance animation by 1 step.");
    println(" * press 'U' to advance animation by 10 steps.");
    println(" * press 'y' to rewind animation by 1 step.");
    println(" * press 'Y' to rewind animation by 10 steps.");
    println(" * press 'l' or 'L' to toggle animation looping.");
    println("// ------------- MUTING COMMANDS ------------- //");
    println(" * press keys 1-8 to mute or unmute first eight wave data operators");
    println(" * press 'm' to print current WaveData states to console.");
    println(" * press 'M' to unmute all current WaveData operators.");
    println("// ------------- JSON COMMANDS ------------- //");
    println(" * press 'j' or 'J' to save WaveSynth settings to a JSON file.");
    println(" * press 'o' to open a new JSON file.");
    println(" * press 'O' to reload the current JSON file, if there is one, reverting all edits.");
    println("// ------------- MISCELLANEOUS COMMANDS ------------- //");
    println(" * press 'r' to toggles display window to fit screen or display at size.");
    println(" * press 's' to save the current image to a .png file.");
    println(" * press 'v' to toggle video recording.");
    println(" * press 'V' to record a complete video loop from frame 0 to stop frame.");
    println(" * press 't' to sort wave data operators in control panel by frequency (lowest first), useful when saving to JSON");
    println(" * press 'h' or 'H' to show this help message in the console.");
}

/**
 * Turn animation on or off.
 */
public void toggleAnimation() {
    isAnimating = !isAnimating;
    // disable or enable some controls
    enableWDListControls(!isAnimating);
    if (isAnimating) {
        // setVideoRecord();
        // prepareAnimation();
        runVideoBtn.setText("Pause");
        startTime = millis();
        println("-----> start time is " + startTime / 1000.0 + " seconds at frame "+ step +" of "+ animSteps);
    } 
    else {
        runVideoBtn.setText("Run");
        stopTime = millis();
        println("-----> stop time is " + stopTime / 1000.0 + " seconds at frame "+ step +" of "+ animSteps);
    }
}

public void toggleLooping() {
    isLooping = !isLooping;
    println(isLooping ? "-- animation looping is true" : "-- animation looping is false");
}

/**
 * Turn video recording on or off. Recording only takes place when animation is also on. 
 * To record a video loop, from frame 0 to the Stop Frame / Steps value in the GUI control panel,
 * use this command sequence:
 *         Turn animation off if it is currently on.
 *         Press 'i' to go to frame 0.
 *         Press 'v' to start recording, or check record in the control panel.
 *         Press spacebar to start animation and recording.
 * 
 * You can also just press the 'V' (capital vee) key to record from frame 0 to the stop frame. 
 * 
 */
public void toggleRecording() {
    isRecordingVideo = !isRecordingVideo;
    println(" Recording video is "+ isRecordingVideo);
    if (isRecordingVideo) {
        if (!isAnimating) {
            println(" Press spacebar to start animation and video recording from frame "+ step);
        }
        else {
            println(" Recording animation from frame "+ step);
        }
    }
    else {
        if (isAnimating) {
            println(" Recording is off. Continuing animation from frame "+ step);
        }
        else {
            println(" Video recording and animation are off at frame "+ step);
        }
    }
    refreshGlobalPanel();
}

// ------------- WaveData methods ------------- //

/**
 * Scales the amplitude of an ArrayList of WaveData objects.
 * 
 * @param waveDataList an ArrayList of WaveData objects
 * @param scale        the amount to scale the amplitude of each WaveData object
 */
public void scaleAmps(ArrayList<WaveData> waveDataList, float scale) {
    int i = 0;
    for (WaveData wd : waveDataList) {
        if (wd.isMuted) {
            i++;
            continue;
        }
        wd.setAmp(wd.amp * scale);
        if (isVerbose) println("----- set amplitude " + i + " to " + wd.amp);
    }
}

/**
 * Shifts the colors of an ArrayList of WaveData objects.
 * @param waveDataList    an ArrayList of WaveData objects
 * @param shift        the amount shift each color
 */
public void shiftColors(ArrayList<WaveData> waveDataList, float shift) {
    for (WaveData wd : waveDataList) {
        if (wd.isMuted)
            continue;
        wd.setWaveColor(WaveSynthBuilder.colorShift(wd.waveColor, shift));
    }
    if (isVerbose) println("----->>> shift colors " + shift);
}

/**
 * Scales the frequencies of an ArrayList of WaveData objects.
 * @param waveDataList    an ArrayList of WaveData objects
 * @param scale        the amount to scale the frequency of each WaveData object
 */
public void scaleFreqs(ArrayList<WaveData> waveDataList, float scale) {
    int i = 0;
    for (WaveData wd : waveDataList) {
        if (wd.isMuted) {
            i++;
            continue;
        }
        wd.setFreq(wd.freq * scale);
        if (isVerbose) println("----- set frequency " + i + " to " + wd.freq);
    }
}    

/**
 * Shifts the phase of an ArrayList of WaveData objects.
 * @param waveDataList    an ArrayList of WaveData objects
 * @param shift            amount to shift the phase of each WaveData object
 */
public void shiftPhases(ArrayList<WaveData> waveDataList, float shift) {
    for (WaveData wd : waveDataList) {
        if (wd.isMuted)
            continue;
        // wd.setPhase(wd.phase + shift - floor(wd.phase + shift));
        wd.setPhase(wd.phase + shift);
    }
    if (isVerbose) println("----->>> shiftPhase " + shift);
}

/**
 * Prints the phase values of an ArrayList of WaveData objects.
 * @param waveDataList    an ArrayList of WaveData objects
 */
public void showPhaseValues(ArrayList<WaveData> waveDataList) {
    int phaseStep = wavesynth.getStep();
    StringBuffer sb = new StringBuffer("\n----- current phase values scaled over (0, 1) -----\n");
    int i = 1;
    for (WaveData wd : waveDataList) {
        float m = wd.scaledPhaseAtFrame(phaseStep);
        sb.append(i++ +": "+ nf(m) + "; ");
    }
    sb.append("\n----- current phase values scaled over (0, TWO_PI) -----\n");
    i = 1;
    for (WaveData wd : waveDataList) {
        float m = wd.phaseAtFrame(phaseStep);
        sb.append(i++ +": "+ nf(m) + "; ");
    }
    println(sb);
}

/**
 * Applies the current phase values to the initial values of the WaveSynth, so that
 * the current state of the image display will appear as the first frame of 
 * animation. Save the WaveSynth to a JSON file to keep the new phase values. 
 * 
 * @param waveDataList    an ArrayList of WaveData objects
 */
public void capturePhaseValues(ArrayList<WaveData> waveDataList) {
    int phaseStep = wavesynth.getStep();
    for (WaveData wd : waveDataList) {
        float currentPhase = wd.scaledPhaseAtFrame(phaseStep);
        wd.setPhase(currentPhase);
    }
}

/**
 * Mutes or unmutes a WaveData operator (view in the control panel).
 * @param elem    the index number of a WaveData object stored in a WaveSynth's waveDataList field
 */
public void toggleWDMute(int elem) {
    if (wavesynth.waveDataList.size() < elem + 1) return;
    WaveData wd = wavesynth.waveDataList.get(elem);
    wd.isMuted = !wd.isMuted;
    if (wd.isMuted) {
        wd.waveState = WaveState.MUTE;
    }
    else {
        wd.waveState = WaveState.ACTIVE;
    }
    if (!isAnimating) {
        wavesynth.renderFrame(step);
    }
}

/**
 * Prints mute/active status of WaveData operators in supplied waveDataList. 
 * @param waveDataList    an ArrayList of WaveData objects
 */
public void printWDStates(ArrayList<WaveData> waveDataList) {
    StringBuffer sb = new StringBuffer("Audio operators\n");
    int n = 1;
    for (WaveData wd :waveDataList) {
        sb.append("wave "+ (n++) +" "+ wd.waveState.toString() +", ");
    }
    println(sb.toString());
}

/**
 * Unmutes all the operators in supplied waveDataList.
 * @param waveDataList    an ArrayList of WaveData objects
 */
public void unmuteAllWD(ArrayList<WaveData> waveDataList) {
    StringBuffer sb = new StringBuffer("Audio operators\n");
    int n = 1;
    for (WaveData wd :waveDataList) {
        wd.setWaveState(WaveState.ACTIVE);
        sb.append("wave "+ (n++) +" "+ wd.waveState.toString() +", ");
    }
    println(sb.toString());        
}

/**
 * Comparator class for sorting waveDataList by frequency or phase
 */
public class CompareWaveData implements Comparator <WaveData> {
  boolean isCompareFrequency = true;
  public int compare(WaveData wd1, WaveData wd2) {
    if (isCompareFrequency) {
      if (wd1.freq > wd2.freq) return 1;
      if (wd1.freq < wd2.freq) return -1;
    } 
    else {
      if (wd1.phase > wd2.phase) return 1;
      if (wd1.phase < wd2.phase) return -1;
    }
    return 0;
  }
}

/**
 * Steps through the WaveSynth's list of WaveData, shows the current 
 * WaveData operator in the control panel.
 * @param up     if true, increment waveDataIndex, otherwise, decrement it
 */
public void stepWaveData(boolean up) {
    int dataLen = wavesynth.waveDataList.size();
    if (up) {
        waveDataIndex = (waveDataIndex + 1 >= dataLen) ? 0 : waveDataIndex + 1;
    } 
    else {
        waveDataIndex = (waveDataIndex - 1 >= 0) ? waveDataIndex - 1 : dataLen - 1;
    }
    currentWD = wavesynth.waveDataList.get(waveDataIndex);
    loadWaveDataPanelValues(currentWD);
}

public void incWaveData() {
    stepWaveData(true);
}

public void decWaveData() {
    stepWaveData(false);
}


/*----------------------------------------------------------------*/
/*                                                                */
/*                     BEGIN AUDIO METHODS                        */
/*                                                                */
/*----------------------------------------------------------------*/

/**
 * CALL IN SETUP()
 */
public void initAudio() {
    minim = new Minim(this);
    // use the getLineOut method of the Minim object to get an AudioOutput object
    this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
    this.audioBuffer = new MultiChannelBuffer(1024, 1);
    // ADSR envelope with maximum amplitude, attack Time, decay time, sustain level, and release time
    adsr = new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime);
    timeLocsArray = new ArrayList<TimedLocation>();     // initialize mouse event tracking array
}
/**
 * Save audio buffer to a file called "wavesynth_<wsIndex>.wav".
 */
public void saveToAudio() {
    renderSignal();
    try {
        saveAudioToFile(audioSignal, sampleRate, "wavesynth_"+ wsIndex +".wav");
    }
    catch (IOException e) {
        println("--->> There was an error outputting the audio file wavesynth.wav "+ e.getMessage());
    }
    catch (UnsupportedAudioFileException e) {
        println("--->> The file format is unsupported "+ e.getMessage());
    }
}
/**
 * Calls WaveSynth to render a audio sample array derived from the same math that creates the image.
 */
public void renderSignal() {
    this.audioSignal = wavesynth.renderAudioRaw(step);            // get the signal "as is" from WaveSynth
    audioSignal = WaveSynth.normalize(audioSignal, 0.9f);        // normalize samples to the range (-0.9f, 0.9f) 
    audioLength = audioSignal.length;
    audioBuffer.setBufferSize(audioLength);
    audioBuffer.setChannel(0, audioSignal);                        // copy audioSignal to channel 0 of audioBuffer
    // println("--->> copied audio signal to audio buffer");
}
public void audioMousePressed(int sampleX, int sampleY) {
    this.sampleX = sampleX;
    this.sampleY = sampleY;
    samplePos = mapper.lookupSample(sampleX, sampleY);
    if (audioSignal == null || isBufferStale) {
        renderSignal();
        isBufferStale = false;
    }
    playSample(samplePos, calcSampleLen(), 0.6f, new ADSR(maxAmplitude, attackTime, decayTime, sustainLevel, releaseTime));
}
    
/**
 * @param samplePos        position of the sample in the audio buffer
 * @param samplelen        length of the sample (will be adjusted)
 * @param amplitude        amplitude of the sample on playback
 * @param adsr            an ADSR envelope for the sample
 * @return                the calculated sample length in samples
 */
public int playSample(int samplePos, int samplelen, float amplitude, ADSR adsr) {
    // println("--- play "+ twoPlaces.format(amplitude));
    audioSampler = new Sampler(audioBuffer, sampleRate, 8); // create a Minim Sampler from the buffer sampleRate sampling
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
 * @throws IOException        an Exception you'll need to catch to call this method (see keyPressed entry for 's')
 * @throws UnsupportedAudioFileException        another Exception (see keyPressed entry for 's')
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
