/*
 * This example sketch continues the Tutorial One sequence for the PixelAudio
 * library for Processing. The previous examples included reading and writing and 
 * transcoding audio and image files, triggering audio events, animating pixels 
 * to change audio sample order, and drawing in the display window to 
 * create brush shapes that can be used to trigger audio events. 
 * 
 * In this part of the tutorials, we'll get to UDP communication with Max 
 * and other media applications. This material should be regarded as 
 * @Experimental, and likely to change in future releases. 
 * 
 * This particular example builds on TutorialOne_04_Drawing. For notes on the 
 * drawing tools and everything else not concerned with UDP communications, see
 * that tutorial. 
 * 
 * We use the oscP5 Processing library to handle UDP communications. UDP, User Datagram
 * Protocol, is a standard for internet communications introduced in 1980 and still going
 * strong. OSC, Open Sound Control, is a protocol for networking synthesizers, computers, 
 * etc, that can be used over UDP, but it is not necessary for our implementation.
 * 
 * The sample files that come with oscP5 provide you with the basics for communicating over 
 * a network. You'll see from them that there are two import statement to add to your code:
 * 
 *     import oscP5.*;
 *     import netP5.*;
 * 
 * Because our tasks go beyond the basics and we also don't want to clutter our sketch 
 * with more code, we're going to use a simple Design Pattern called Delegation. In delegation, 
 * an object handles a task by passing it on to another object, a Delegate. The delegate 
 * maintains a reference to the calling object, and--depending on the implementation--can 
 * access the original, calling object. In this way, the heavy duty code for a task can be 
 * isolated in the delegate. When the delegate needs to communicate with the caller, it 
 * should have a defined way to do this, typically an Interface, that indicates which 
 * methods the caller must implement to receive communications from the delegate. Any 
 * caller that implements the interface can be a client of the delegate, with full access
 * to its functionality.
 * 
 * The interface tells us what methods the client of the delegate needs to implement.
 * Our interface is called PANetworkClientINF.java and our delegate is NetworkDelegate.java.
 * In the Processing IDE, we add these files as tabs in our sketch. In Java, we would add a 
 * some text to our main class declaration: 
 * 
 *     public class TutorialOneUDP extends PApplet implements PANetworkClientINF
 * 
 * >>>>> In Processing, NetworkDelegate is an internal class of our main class, TutorialOne_05_UDP, <<<<<
 * >>>>> in the main tab, and we can call it without having to declare the interface.               <<<<<
 * 
 * Also in out main class we will declare two variables:
 * 
 *     // network communications
 *     NetworkDelegate nd;
 *     boolean isUseNetworkDelegate = false;
 * 
 * The in the last lines of setup, we add:
 * 
 *     isUseNetworkDelegate = true;
 *     if (isUseNetworkDelegate) {
 *       String remoteAddress = "127.0.0.1";
 *       nd = new NetworkDelegate(this, remoteAddress, remoteAddress, 7401, 7400);
 *       nd.oscSendClear();
 *     }
 * 
 * The variable "nd" is our NetworkDelegate instance. We instantiated with this constructor:
 * 
 *   NetworkDelegate(PANetworkClientINF app, String remoteFromAddr, String remoteToAddr, int inPort, int outPort)
 * 
 * >>>>> However, if you are using Processing, you'll need to edit the constructors for NetworkDelegate. <<<<<
 * 
 * Wherever NetworkDelegate refers to "PANetworkClientINF" argument, you need to put the name of your sketch. For example:
 * 
 *   NetworkDelegate(TutorialOne_05_UDP app, String remoteFromAddr, String remoteToAddr, int inPort, int outPort)
 * 
 * This is because behind the scenes, Processing makes the type of your sketch whatever the main tab is called,
 * creating it as a subclass of PApplet. Through this modified constructor, the delegate will have access to 
 * the host class ("this"). It expects to receive messages over remotedFromAddr and send messages with 
 * remoteToAddr, using inPort and outPort. 
 * 
 * Once we have all those steps done, it should be possible to launch the app. Before we do that, it 
 * would be good to have a Max patcher or some other UDP-enabled media app that we can use. "UDP_Handler" 
 * is the (catchy) name for the one that accompanies this sketch. Go ahead and launch it, and place it 
 * somewhere on the screen where you can see both it and your Processing sketch. Then launch the sketch. 
 * 
 * Open a sound file to have something to play, and click on "most recent curve" message in the Max 
 * "network_receiver" subpatcher. You should get sound and animation in Processing. This happens because  
 * Max sends a "/draw" token followed by the numbers in the message box to the NetworkDelegate. The 
 * delegate has a method, drawHit(), for handling "/draw" messages: it was set up in initOscPlugs(),
 * when the NetworkDelegate was initialized. 
 * 
 * 
 * There's more to do, of course. We want to set up some messaging from Processing, for example, for
 * mouse clicks and drawing. Whenever we use the delegate nd, we'll wrap the command in a conditional:
 * 
 *   if (nd != null) nd.oscSendMousePressed(sampleX, sampleY, samplePos);
 * 
 * This is the command for clicks -- but where do we put it? In this case, since we are passing mousePressed
 * events on to other handlers, we can put it in the drawing section, handleMousePressed(); It's already 
 * there, we just need to uncomment it. 
 * 
 * We can proceed to provide more communication between Max and Processing in a similar way. You can 
 * activate additional communications by uncommenting lines with "if (nd != null)". We proceed in this 
 * way, with a conditional, so that the sketch can run with or without Processing. 
 * 
 * NOTES ON NEW RELEASE 0.9.2-beta AND LATER
 * 
 * I have not commented out the lines that call the NetworkDelegate. If nd is instantiated, they will
 * execute and the communicate with UDPHandler.maxpat, if it's available. By default, isUseNetworkDelegate = false, 
 * in the setup() method. Set it to true to enable UDP communications. 
 * 
 * Also note that the points and time stamps sent to UDPHandler by initCurveMaker(), in the Drawing Tools 
 * tab in Processing, are not the same as the Bezier curve points used to draw and trigger events
 * in Processing. I'm sending the reduced point set from which the Bezier curves are derived.
 * This was a choice based on Max's apparent issues with sending large data bundles to Processing.
 * It would receive the plentiful Bezier points, but not send them all back beyond about 512 points. 
 * I've been looking into this issue. If you draw a really long curve, you'll encounter it when you
 * try to send the curve data back to Processing, even though it was all captured.  
 * 
 * Meanwhile, here are the calls in intCurveMaker():
 * 
 *   if (nd != null) {
 *     nd.oscSendMousePressed(sampleX, sampleY, samplePos);
 *     nd.oscSendDrawPoints(curveMaker.getRdpPoints());
 *     nd.oscSendTimeStamp(curveMaker.timeStamp, curveMaker.timeOffset);
 *   }
 *   
 * Their behavior can be changed by editing the methods in NetworkDelegate, particularly 
 * oscSentDrawPoints(). See PACurveMaker for information on the various data arrays it 
 * has available. 
 * 
 * >>>>> SOUND THE DANGER MUSIC <<<<< 
 * 
 * As was apparent in an earlier stage of this sketch, TutorialOne_03_Animation, when we introduce
 * another thread, such as an "Open File" command, we have to take care that it doesn't cause 
 * cause problems by, for example, changing resources that the sketch thread also wants to change. 
 * In TutorialOne_03_Animation, the animation was changing the audio buffer and the Open File
 * command did so, too. Our solution there was to pause animation while opening a file. It was not
 * the only solution, but it's simple, and it only applied for as long as it took to choose a file
 * 
 * In TutorialOne_05_UDP, we have two independent threads running simultaneously. Both of them can 
 * give commands to play sounds to Processing. The sounds to be play are queued in two lists:
 * TimeLocsArray, for events triggered by a mouse click, and curveTLEvents, for series of events 
 * associated with a brushstroke. If the Processing thread and the Max thread both try to alter
 * one of the lists are the same time, the Java Runtime Environment may throw a 
 * ConcurrentModificationException. Or worse yet, the event lists may be altered and events
 * deleted before they can be played. To prevent these sort of things from happening, the
 * methods that run the event lists have been declared with the keyword "synchronized":
 * 
 *     public synchronized void runCurveEvents()
 *     public synchronized void runPointEvents()
 * 
 * This means that the method must complete before it can be called from another thread. 
 * Since the methods execute very quickly, this is a viable solution. Running multiple 
 * threads is a complex issue, with a ample suite of tools in Java, but it's far beyond 
 * the aims of this tutorial. Suffice it to say, if two or more threads use the same resources,
 * you will need to handle multithreading. In this tutorial, we've exposed all the key
 * commands in parseKey() to potential messaging from external threads, such as our
 * Max patcher. Remote control of a PixelAudio application can provide interesting
 * results, but it comes with risks that you'll have to deal with, particularly in
 * a live performance situation. 
 * 
 * 
 * Here are the key commands for this sketch:
 * 
 * Press ' ' to  start or stop animation.
 * Press 'd' to turn drawing on or off.
 * Press 'm' to turn mouse tracking on or off.
 * Press 'c' to apply color from image file to display image.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage .
 * Press 'l' or 'L' to set files to load as both image and audio or load separately.
 * Press 'o' or 'O' to open an audio or image file.
 * Press 'w' to write the image colors to the audio buffer as transcoded values.
 * Press 'W' to write the audio buffer samples to the image as color values.
 * Press 'x' to delete the current active brush shape or the oldest brush shape.
 * Press 'X' to delete the most recent brush shape.
 * Press 'V' to record a video.
 * Press 'h' or 'H' to show help message in the console.
 * 
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

//Mama's ever-lovin' blue-eyed PixelAudio library
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;
import net.paulhertz.pixelaudio.curves.*;

//audio library
import ddf.minim.*;
import ddf.minim.ugens.*;

//video export library
import com.hamoid.*;


// PixelAudio vars and objects
PixelAudio pixelaudio;     // our shiny new library
MultiGen multigen;         // a PixelMapGen that links together multiple PixelMapGens
int genWidth = 512;        // width of multigen PixelMapGens
int genHeight = 512;       // height of  multigen PixelMapGens
PixelAudioMapper mapper;   // object for reading, writing, and transcoding audio and image data
int mapSize;               // size of the display bitmap, audio signal, mapper LUTs, etc.
PImage mapImage;           // image for display
PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;
int[] colors;              // array of spectral colors

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

String daPath;

/* ------------------------------------------------------------------ */
/*                          AUDIO VARIABLES                           */
/* ------------------------------------------------------------------ */

/*
 * Audio playback support is added with the audio variables and audio methods 
 * (below, in Eclipse, in a tab, in Processing). You will also need the 
 * WFInstrument and TimedLocation classes. In setup(), call initAudio(), then
 * add a mousePressed() method that calls audioMousePressed(mouseX, mouseY)
 * and call runTimeArray() in your draw method. 
 */
 
/** Minim audio library */
Minim minim;               // library that handles audio 
AudioOutput audioOut;      // line out to sound hardware
boolean isBufferStale = false;  // flags that audioBuffer needs to be reset
float sampleRate = 48000;  // sample rate for audio output and audio files
float[] audioSignal;       // the audio signal as an array of floats
MultiChannelBuffer playBuffer;  // a buffer for playing the audio signal
int samplePos;             // an index into the audio signal, selected by a mouse click on the display image
int audioLength;           // length of the audioSignal, same as the number of pixels in the display image

// SampleInstrument setup
int noteDuration = 1000;   // average sample synth note duration, milliseconds
int samplelen;             // calculated sample synth note length, samples
WFSamplerInstrument synth;      // local class to wrap audioSampler
WFSamplerInstrumentPool pool;   // an allocation pool of WFSamplerInstruments
boolean isUseSynth = false;     // switch between pool and synth

// ADSR and its parameters
ADSRParams adsr;          // good old attack, decay, sustain, release
float maxAmplitude = 0.7f;      // 0..1
float attackTime = 0.4f;        // seconds
float decayTime = 0.0f;         // seconds, no decay
float sustainLevel = 0.7f;      // 0..1, same as maxAmplitude
float releaseTime = 0.4f;       // seconds, same as attack

// interaction variables for audio
int sampleX;                    // keep track of coordinates associated with audio samples
int sampleY;
ArrayList<TimedLocation> timeLocsArray;    // a list of timed events 
boolean isLoadToBoth = true;    // if true, load newly opened file both to audio and to video


/* ------------------------------------------------------------------ */
/*                   ANIMATION AND VIDEO VARIABLES                    */
/* ------------------------------------------------------------------ */

int shift = 64;                         // number of pixels to shift the animation
int totalShift = 0;                     // cumulative shift
boolean isAnimating = false;            // do we run animation or not?
boolean oldIsAnimating;                 // keep track of animation state when opening a file
boolean isTrackMouse = false;           // if true, drag the mouse to change shift value
// animation variables
int animSteps = 720;          // how many steps in an animation loop
boolean isRecordingVideo = false;    // are we recording? (only if we are animating)
int videoFrameRate = 24;        // fps, frames per second
int step;                // number of current step in animation loop
VideoExport videx;                      // hamoid library class for video export (requires ffmpeg)
    
/* ------------------------------------------------------------------ */
/*                         DRAWING VARIABLES                          */
/* ------------------------------------------------------------------ */

/*
 * Drawing uses classes in the package net.paulhertz.pixelaudio.curves to store drawing data
 * and draw Bezier curves and lines. It also provides very basic brushstroke modeling code. 
 * Unlike most of the code in PixelAudio, which avoids dependencies on Processing, the 
 * curves.* classes interface with Processing to draw to PApplets and PGraphics instances. 
 * See the PACurveMaker class for details of how drawing works. 
 * 
 */

// curve drawing and interaction
public boolean isDrawMode = false;                  // is drawing on or not?
public float epsilon = 4.0f;                        // controls how much reduction is applied to points
public ArrayList<PVector> allPoints;                // all the points the user drew, thinnned
public int dragColor = color(233, 199, 89, 128);    // color for initial drawing 
public float dragWeight = 8.0f;                     // weight (brush diameter) of initial line drawing
public int startTime;                               // start time for user drawing event
public ArrayList<Integer> allTimes;                 // list for tracking user drawing times, for future use
public PVector currentPoint;                        // most recent point in user drawing
public int polySteps = 5;                           // number of steps in polygon representation of a Bezier curve
public PACurveMaker curveMaker;                     // class for tracking and storing drawing data
public ArrayList<PVector> eventPoints;              // list of points stored in or loaded from a PACurveMaker
public ListIterator<PVector> eventPointsIter;       // iterator for eventPoints
int eventStep = 90;                                 // milliseconds between events
public ArrayList<TimedLocation> curveTLEvents;      // a list of TimedLocation instances 
public ArrayList<PACurveMaker> brushShapesList;     // a list of PACurveMaker instances with recorded drawing data
public PACurveMaker activeBrush;                    // the currently active PACurveMaker, collecting points as user drags the mouse
public int activeIndex = 0;                         // index of current brush in brushShapesList, useful for UDP/OSC messages
int newBrushColor = color(144, 34, 42, 233);        // color of the new brushstroke
int polyPointsColor = color(233, 199, 144, 192);    // color for polygon representation of Bezier curve associated with a brushstroke
int activeBrushColor = color(144, 89, 55, 233);     // color for the active brush
int readyBrushColor = color(34, 89, 55, 233);       // color for a brushstroke when ready to be clicked
boolean isIgnoreOutsideBounds = true;               // when drawing, clip or ignore points outside display bounds

/* ------------- end drawing variables ------------- */
  
// network communications
NetworkDelegate nd;
boolean isUseNetworkDelegate = false;

// ** YOUR VARIABLES ** Variables for YOUR CLASS may go here **  //

  
public void settings() {
  size(3 * genWidth, 2 * genHeight);
}

public void setup() {
  frameRate(24);
  // initialize our library
  pixelaudio = new PixelAudio(this);
  // create a PixelMapGen subclass such as MultiGen, with dimensions equal to out display window
  // the call to hilbertLoop3x2 produces a MultiGen that is 3 * genWidth x 2 * genHeight, where
  // genWidth == genHeight and genWidth is a power of 2 (a restriction on Hilbert curves)
  multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
  // create a PixelAudioMapper to handle the mapping of pixel colors to audio samples
  mapper = new PixelAudioMapper(multigen);
  // keep track of the area of the PixelAudioMapper
  mapSize = mapper.getSize();
  // create an array of rainbow colors with mapSize elements
  colors = getColors(mapSize);
  initImages();
  initAudio();
  initDrawing();
  isUseNetworkDelegate = true;
  if (isUseNetworkDelegate) {
    String remoteAddress = "127.0.0.1";
    nd = new NetworkDelegate(this, remoteAddress, remoteAddress, 7401, 7400);
    nd.oscSendClear();
  }
  // in Processing (but not so easily in Eclipse) we can load an audio or image file
  // from the path to the folder where PixelAudio examples keep their data files
  // If you modify this sketch, you may need to change the path.
  preloadFiles(sketchPath("") + "../../examples_data/");
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
  colorMode(HSB, colorWheel.length, 100, 100); // pop over to the HSB color space and give hue a very wide range
  for (int i = 0; i < colorWheel.length; i++) {
    colorWheel[i] = color(i, 50, 70); // fill our array with colors, gradually changing hue
  }
  popStyle(); // restore styles, including the default RGB color space
  return colorWheel;
}

/**
 * Initializes mapImage with the colors array. MapImage will handle the color data for mapper
 * and also serve as our display image.
 */
public void initImages() {
  mapImage = createImage(width, height, ARGB);
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
  mapImage.updatePixels();
}

/**
 * Initializes the line, curve, and brushstroke drawing variables. 
 * Note that timeLocsArray has been initialized by initAudio(), though it
 * will be used for point events in the drawing code, too. 
 */
public void initDrawing() {
  currentPoint = new PVector(-1, -1);
  brushShapesList = new ArrayList<PACurveMaker>();
}

public void preloadFiles(String path) {
  daPath = path;
  // the audio file we want to open on startup
  File audioSource = new File(daPath +"Saucer_mixdown.wav");
  // load the file into audio buffer and Brightness channel of display image (mapImage)
  fileSelected(audioSource);
  // overlay colors on mapImage
  mapImage.loadPixels();
  applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
  mapImage.updatePixels();  
}
  
public void draw() {
  image(mapImage, 0, 0);
  // handleDrawing() handles circle and brushstroke drawing and audio events
  handleDrawing();
  if (isAnimating) {
    animate();
    updateAudio();
  }
  if (isTrackMouse && mousePressed) {
    writeToScreen("shift = "+ shift, 16, 24, 24, false);
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
    if (isRecordingVideo) {
      isRecordingVideo = false;
      videx.endMovie();
      isAnimating = oldIsAnimating;
      println("--- Completed video at frame " + animSteps);
    }
    step = 0;
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
 * Renders a frame of animation: moving along the signal path, copies mapImage pixels into rgbSignal, 
 * rotates them shift elements left, writes them back to mapImage.
 * 
 * @param step   current animation step
 */
public void renderFrame(int step) {
  mapImage.loadPixels();
  // get the pixels in the order that the signal path visits them
  int[] rgbSignal = mapper.pluckPixels(mapImage.pixels, 0, mapSize);
  // rotate the pixel array
  PixelAudioMapper.rotateLeft(rgbSignal, shift);
  // keep track of how much the pixel array (and the audio array) are shifted
  totalShift += shift;
  // write the pixels in rgbSignal to mapImage, following the signal path
  mapper.plantPixels(rgbSignal, mapImage.pixels, 0, mapSize);
  mapImage.updatePixels();
}

/**
 * When animating, updates audioSignal by rotating it the same amount as mapImage.pixels.
 */
public void updateAudio() {
  PixelAudioMapper.rotateLeft(audioSignal, shift);
}

/**
 * Handles user's drawing actions, draws previously recorded brushstrokes, 
 * tracks and generates animation and audio events. 
 */
public void handleDrawing() {  
  // draw current brushShapes 
  drawBrushShapes();
  if (isDrawMode) {
    if (mousePressed) {
      addPoint(mouseX, mouseY);
    }
    if (allPoints != null && allPoints.size() > 2) {
      PACurveUtility.lineDraw(this, allPoints, dragColor, dragWeight);
    }
    if (curveMaker != null) curveMakerDraw();
  } 
  runCurveEvents();
  runPointEvents();
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
 * The built-in mousePressed handler for Processing, but note that it forwards 
 * mouse coords to handleMousePressed(). If isDrawMode is true, we start accumulating
 * points to allPoints: initAllPoints() adds the current mouseX and mouseY. After that, 
 * the draw loop calls handleDrawing() to add points. Drawing ends on mouseReleased(). 
 */
public void mousePressed() {
  setSampleVars(mouseX, mouseY);      
  if (this.isDrawMode) {
    initAllPoints();
  } 
  else {
    handleMousePressed(mouseX, mouseY);
  }
}

public void mouseDragged() {
  if (isTrackMouse) {
    shift = abs(width/2 - mouseX);
    if (mouseY < height/2) 
      shift = -shift;
    writeToScreen("shift = "+ shift, 16, 24, 24, false);
  }
}

public void mouseReleased() {
  setSampleVars(mouseX, mouseY);      
  if (isAnimating && isTrackMouse) {
    // println("----- animation shift = "+ shift);
  }
  if (isDrawMode && allPoints != null) {
    if (allPoints.size() > 2) {    // add curve data to the brush list
      initCurveMaker();
    }
    else {              // handle the event as a click
      handleMousePressed(mouseX, mouseY);
    }
    allPoints.clear();
  }
}

/**
 * built-in keyPressed handler, forwards events to parseKey
 */
public void keyPressed() {
  parseKey(key, keyCode);    
}

/**
 * Required by the PANetworkClientINF interface.
 *
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
  case ' ': //  start or stop animation
    isAnimating = !isAnimating;
    println("-- animation is " + isAnimating);
    break;
  case 'd': // turn drawing on or off
    // turn off mouse tracking that sets shift value for animation
    isTrackMouse = false; 
    // turn off animation (you can try drawing with it on, just press the spacebar)
    isAnimating = false;
    isDrawMode = !isDrawMode;
    println(isDrawMode ? "----- Drawing is turned on" : "----- Drawing is turned off");
    break;
  case 'm': // turn mouse tracking on or off
    isTrackMouse = !isTrackMouse;
    println("-- mouse tracking is " + isTrackMouse);
    break;
  case 'c': // apply color from image file to display image
    chooseColorImage();
    break;
  case 'k': // apply the hue and saturation in the colors array to mapImage 
    mapImage.loadPixels();
    applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
    mapImage.updatePixels();
    break;
  case 'L': case 'l':
    isLoadToBoth = !isLoadToBoth;
    String msg = isLoadToBoth ? "loads to both audio and image. " : "loads only to selected format. ";
    println("---- isLoadToBoth is "+ isLoadToBoth +", opening a file "+ msg);
    break;
  case 'p': // play brushstrokes with a time lapse between each
    playBrushstrokes(2000);
    break;
  case 'o': case 'O': // open an audio or image file
    chooseFile();
    break;
  case 'w': // write the image colors to the audio buffer as transcoded values
    // TODO refactor with loadImageFile() and loadAudioFile() code
    // prepare to copy image data to audio variables
    // resize the buffer to mapSize, if necessary -- signal will not be overwritten
    if (playBuffer.getBufferSize() != mapper.getSize()) playBuffer.setBufferSize(mapper.getSize());
    audioSignal = playBuffer.getChannel(0);
    writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
    // now that the image data has been written to audioSignal, set playBuffer channel 0 to the new audio data
    playBuffer.setChannel(0, audioSignal);
    audioLength = audioSignal.length;
    println("--->> Wrote image to audio as audio data.");
    break;
  case 'W': // write the audio buffer samples to the image as color values
    writeAudioToImage(audioSignal, mapper, mapImage, chan);
    println("--->> Wrote audio to image as pixel data.");
    break;
  case 'x': // delete the current active brush shape or the oldest brush shape
    if (activeBrush != null) {
      removeActiveBrush();
    }
    else {
      removeOldestBrush();
    }
    break;
  case 'X': // delete the most recent brush shape
    removeNewestBrush();
    break;
  case 'V': // record a video
    // records a complete video loop with following actions:
    // Go to frame 0, turn recording on, turn animation on,
    // record animSteps number of frames. 
    // not of much use in this sketch, but here to keep code complete
    step = 0;
    renderFrame(step);
    isRecordingVideo = true;
    oldIsAnimating = isAnimating;
    isAnimating = true;
    break;
  case 'h': case 'H':
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press ' ' to  start or stop animation.");
  println(" * Press 'd' to turn drawing on or off.");
  println(" * Press 'm' to turn mouse tracking on or off.");
  println(" * Press 'c' to apply color from image file to display image.");
  println(" * Press 'k' to apply the hue and saturation in the colors array to mapImage .");
  println(" * Press 'l' or 'L' to set files to load as both image and audio or load separately.");
  println(" * Press 'p' to play brushstrokes with a time lapse between each.");
  println(" * Press 'o' or 'O' to open an audio or image file.");
  println(" * Press 'w' to write the image colors to the audio buffer as transcoded values.");
  println(" * Press 'W' to write the audio buffer samples to the image as color values.");
  println(" * Press 'x' to delete the current active brush shape or the oldest brush shape.");
  println(" * Press 'X' to delete the most recent brush shape.");
  println(" * Press 'V' to record a video.");
  println(" * Press 'h' or 'H' to show help message in the console.");
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
  if (colorSource == null || graySource == null || lut == null) 
    throw new IllegalArgumentException("colorSource, graySource and lut cannot be null.");
  if (colorSource.length != graySource.length || colorSource.length != lut.length) 
    throw new IllegalArgumentException("colorSource, graySource and lut must all have the same length.");
  // initialize a reusable array for HSB color data -- this is a way to speed up the applyColor() method
  float[] hsbPixel = new float[3];
  for (int i = 0; i < graySource.length; i++) {
    graySource[i] = PixelAudioMapper.applyColor(colorSource[lut[i]], graySource[i], hsbPixel);
  }
  return graySource;
}
