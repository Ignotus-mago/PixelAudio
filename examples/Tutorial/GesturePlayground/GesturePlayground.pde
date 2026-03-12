import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

// video export
import com.hamoid.VideoExport;
// Minim audio library
import ddf.minim.AudioOutput;
import ddf.minim.Minim;
import ddf.minim.MultiChannelBuffer;
import ddf.minim.AudioListener;

//GUI library for Processing
import g4p_controls.*;

// our red leaf lettuce of a library, so crisp and delicious
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig.HopMode;
import net.paulhertz.pixelaudio.curves.*;
import net.paulhertz.pixelaudio.schedule.*;
import net.paulhertz.pixelaudio.granular.*;
import net.paulhertz.pixelaudio.sampler.*;

/**
 *
 * 
 * QUICK START
 * 
 * 1. Launch the sketch. A display window and a palette of Graphical User Interface (GUI) controls appears.
 * The display window has an audio file preloaded. The grayscale values in the image are transcoded audio
 * samples. An overlaid rainbow spectrum traces the Signal Path, the mapping of the audio signal to the image
 * pixels created by the PixelMapGen multigen and managed by the PixelAudioMapper mapper.
 * The Signal Path starts in the upper left corner and ends in the lower right corner.
 *
 * 2. Drawing is already turned on, so go ahead and drag the mouse to draw a line. As in TutorialOne_03_Drawing,
 * a brushstroke appears when you release the mouse. TutorialOne_03_Drawing gave you limited control over
 * the attributes of the brushstroke and its associated audio parameters. In GesturePlayground, you can
 * control nearly all the available parameters with the control palette.
 *
 * 3. At the top of the control palette, you'll find Path Source radio buttons and sliders for setting
 * the geometry of the brush curve. When the curve is set to Reduced Points or Curve Points, the epsilon
 * slider will allow you to visualize changes in the curve. For the curve points representation of the
 * curve, theCurve Points slider will add or subtract points.
 *
 * 4. The control palette displays knobs for the type of audio synthesis instrument you have selected.
 * Press the 't' key to change the instrument. The control palette will reflect the changes. The
 * control palette provides three play modes: one for editing granular synthesis parameters, another
 * for the sampler synthesizer, and a "play only" mode where you can play both instruments but
 * don't have editing enabled.
 *
 * 5. The controls for the Sampler are fairly simple. You can change the number of points in the curve
 * with the geometry controls. You can also change the duration of the gesture and the number of
 * points in it with the Resample and Duration sliders. Finally, there's a Sampler Envelope menu
 * that will change the ADSR envelope of each sampler event point.
 *
 * 6. The Granular Synth has all the controls of the Sampler synth except for the envelopes, plus
 * many controls for granular synthesis:
 *   
 *   - The Hop Mode radio buttons determine if the duration of the granular event is determined
 *     by the gesture timing data in the brushstroke's PACurveMaker instance, or by the Grain
 *     Length and Hop Length sliders.
 *   - Burst Count sets the number of linear grains at each event point. Its effect is to expand
 *     the sound of the grain.
 *   - Grain Length and Hop Length sliders control the spacing of the grains. Hop Length is only
 *     used for Fixed Hop Mode. Grain and Hop durations are in milliseconds.
 *   - The Warp radio buttons and slider control non-linear timing changes to the gesture.
 *   
 * 7. There are many key commands too, including the 'o' command to load a new audio files. Some
 * commands are particularly useful with granular synthesis:
 *   
 *   - The 'q' command key will calculate the optimal number of grains in a gesture (usually in
 *     GESTURE Path Mode) and update the control palette. This can provide smooth granular synthesis
 *     even as it preserves the timing characteristic of the gesture.
 *   - The 'c' command key will print configuration data to the console.
 *   - The 'x' command key deletes the brush you are hovering over, if it is editable.
 *   - The 'w' command key swaps the instrument type of the brush you are hovering over and changes
 *     edit mode to match.
 *   
 * About GesturePlayground
 * 
 * GesturePlayground uses a GUI to provide a tour of the usage and properties of the
 * AudioBrush subclasses GranularBrush and SamplerBrush, the GestureSchedule class, and
 * theSampler and Granular audio synthesis instruments PASamplerInstrumentPool and
 * PAGranularInstrumentDirector. 
 *
 * An AudioBrush combines a PACurveMaker and a GestureGranularConfig.Builder. 
 * PACurveMakermodels gestures, one of the core concepts of PixelAudio. In its simplest 
 * encoded form, the PAGesture interface,a gesture consists of an array of points and 
 * an array of times. The times array and the points array must be the same size, because
 * the times array records the times when something as-yet-unspecified will happen at 
 * the corresponding point in the points array. In my demos for PixelAudio, what happens 
 * at a point is typically an audio event and an animation event.The sound happens at 
 * the point because points in PixelAudio map onto locations in the sound buffer. 
 *
 * Mapping of bitmap locations onto audio buffer indices is another core concept of PixelAudio. 
 * Gestures over the 2D space of an image become paths through audio buffers. The audio buffer 
 * is traversed either by a granular synthesis engine or by a sampling synthesizer. For the 
 * granular synth, a gesture corresponds to a non-linear traversal of an audio buffer, 
 * potentially as a continuous sequence of overlapping grains with a single envelope. The 
 * sampling synthesizer treats each point as a discrete event with its own envelope. Depending 
 * on how gestures and schedules are structured, the two synthesizers can sound very similar, 
 * but there are possibilities in each that the other cannot realize. 
 *
 * As you might expect, GranularBrush implements granular synth events and SamplerBrush implements 
 * sampler synth events. Both rely on PACUrveMaker which, in addition to capturingthe raw gesture 
 * of drawing a line, provides methods to reduce points / times and create Bezier paths.  
 * PACurveMakerdata can also be modified by changing duration, interpolating samples, or non-linear  
 * time warping. GesturePlayground usesGestureScheduleBuilder to interpolate and warp time and point lists.
 * 
 * The parameters for gesture modeling, granular and sampling synthesis, time and sample interpolation, 
 * and audio events are modeled in the GUI, which uses GestureGranularConfig.Builder gConfig to track  
 * its current state. A GestureGranularConfig instance is associated with each AudioBrush. When you click 
 * on an AudioBrush and activate it, its configuration data is loaded to the GUI and you can edit it.  
 * It will be saved to the brush when you select another brush or change the edit mode. When a brush  
 * is activated with a click, the schedule is built from its PACurveMaker and GestureGranularConfig.Builder
 * instance variables:
 *
 *     GestureSchedule schedule = scheduleBuilder.build(gb.curve(), cfg.build(), audioOut.sampleRate());
 * 
 * ++++++++++++++++++++++++++++++++++++++
 * PROGRAMMING DETAILS, FEEL FREE TO SKIP
 * ++++++++++++++++++++++++++++++++++++++
 *
 * The calling chain for a GranularBrush:
 *
 * mouseClicked() calls scheduleGranularBrushClick(gb, x, y);.
 * In scheduleGranularBrushClick(...) we get a reference to the audio buffer buf and then
 * use the PACurveMaker object gb.curve() and gb.snapshot() to build a GestureSchedule, sched. 
 * sched gets timing and location information for the gesture from gb.curve() and
 * modifies it with the settings from the control palette which are stored gb.snapshot(). 
 *
 * We port the granular synthesis parameters from the brush to a GestureGranularParams object,  
 * and then call playGranularGesture(buf, sched, gParams) to play the granular synth. We also call
 * storeGranularCurveTL(...), which sets up UI animation events to track the grains.
 *
 * Parameter buf is the audio signal that is the source of our grains, parameter sched provides
 * the points and times for grains and parameter params provides the core parameters for granular synthesis.
 *
 * playGranularGesture() builds arrays for buffer position and pan for each individual grain and then calls
 * gDir.playGestureNow(buf, sched, params, startIndices, panPerGrain) to play the PAGranularInstrumentDirector
 * granular synth. The 'p' command key can toggle per-grain pitch jitter, which calls playGestureNow()in a slightly
 * different way. See playGranularGesture() for details.
 *
 * PAGranularInstrumentDirector its own calling chain that goes all the way down to the individual sample level
 * using the Minim library's UGen interface. If you just want to play music, you'll probably never have to deal with the
 * hierarchy of classes directly, but comments PAGranularInstrumentDirector may be useful. 
 * 
 *
 * Part of the calling chain for a SamplerBrush:
 
 * mouseClicked() calls scheduleSamplerBrushClick(sb, x, y).
 *
 * In scheduleSamplerBrushClick() we get array of points on the curve with getPathPoints(sb) and then
 * use sb.snapshot() and scheduleBuilder.build() to build a GestureSchedule 
 *
 * Finally, we pass the schedule and a small time offset to storeSamplerCurveTL(), an array of
 * TimedLocation objects that is checked at every pass through the draw() loop and posts
 * both Sampler instrument triggers and animation events. Unlike the Granular instrument, which requires very accurate
 * timing, the Sampler synth requires less precision, so we can handle it through the UI frames. Sample-accurate
 * timing is a topic for another as-yet-unreleased example sketch. 
 *
 * The runSamplerBrushEvents() method executes the UI brushstroke animation and the Sampler audio events.
 * Sampler events all pass through pool.playSample(samplePos, samplelen, amplitude, env, pitch, pan).
 *
 *
 * Press ' ' to spacebar triggers a brush if we're hovering over a brush, otherwise it triggers a point event.
 * Press 'c' or 'C' to print the current configuration status to the console.
 * Press 't' to switch between Granular and Sampler editing and playing.
 * Press 'z' to change the drawing mode of the hover brush.
 * Press 'd' to toggle doPlayOnDraw to play when a drawing gesture ends or not.
 * Press 'p' to jitter the pitch of granular gestures.
 * Press 'k' to apply the hue and saturation in the colors array to mapImage (not to baseImage).
 * Press 'K' to apply hue and saturation in colors to baseImage and mapImage.
 * Press 'l' or 'L' to toggle loading data to both image and audio buffers when you open either an image or an audio file.
 * Press 'f' or 'F' to toggle verbose output to the console.
 * Press 'o' to open an audio file.
 * Press 'r' or 'R' to reset synths to defaults -- TODO may be dropped.
 * Press 'q' to automatically set an active GRANULAR brush to have an optimized number of samples.
 * Press 'x' to delete the current active brush shape or the oldest brush shape.
 * Press 'X' to delete the most recent brush shape.
 * Press 'h' or 'H' to print help.
 *
 */

//------------- APPLICATION CODE -------------//

public void settings() {
  size(3 * genWidth, 2 * genHeight);
}

public void setup() {
  // set a standard animation framerate
  frameRate(24);
  surface.setTitle("Granular Playground");
  surface.setLocation(60, 20);
  // 1) initialize our library
  pixelaudio = new PixelAudio(this);
  // 2) create a PixelMapGen instance with dimensions equal to the display window.
  if (isRunWordGame) {
    sampleRate = 48000;
    multigen = loadWordGen(genWidth/4, genHeight/4);
    daFilename = "workflow_48Khz.wav";
  } else {
    multigen = HilbertGen.hilbertRowOrtho(6, 4, width/6, height/4);
  }
  // 3) Create a PixelAudioMapper to handle the mapping of pixel colors to audio samples.
  mapper = new PixelAudioMapper(multigen);
  mapSize = mapper.getSize();
  scheduleBuilder = new GestureScheduleBuilder();
  colors = getColors(mapSize);    // create an array of rainbow colors with mapSize elements
  initImages();                   // load baseImage and mapImage
  initAudio();                    // set up Minima and our granular and sampling synths
  // initListener();              // PLACEHOLDER: sample-accurate audio timer -- TODO future implementation
  initConfig();                   // set up configuration for granular and sampling instruments
  initDrawing();                  // set up drawing variables
  initGUI();                      // set up the G4P control window and widgets
  resetConfigForMode();           // determine which GestureGranularConfig to use first and load it
  // path to the folder where PixelAudio examples keep their data files 
  // such as image, audio, .json, etc.
  daPath = sketchPath("") + "../../examples_data/";
  preloadFiles(daPath, daFilename);    // load files - BEWARE system dependent file references!
  applyColorMap();                // apply spectrum to mapImage and baseImage
  showHelp();                     // print key commands to console
}

/**
 * turn off audio processing when we exit
 */
public void stop() {
  if (pool != null) pool.close();
  if (minim != null) minim.stop();
  super.stop();
}

/**
 * Adds PixelMapGen objects to the local variable genList. The genList
 * initializes a MultiGen, which can be used to map audio and pixel data.
 * This method follows the words in the workFlowPanel.png graphic.
 */
public MultiGen loadWordGen(int wordGenW, int wordGenH) {
  // list of PixelMapGens that create an image using mapper
  ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
  // list of x,y coordinates for placing gens from genList
  ArrayList<int[]> offsetList = new ArrayList<int[]>();
  for (int y = 0; y < 4; y++) {
    for (int x = 0; x < 4; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 4; y < 8; y++) {
    for (int x = 0; x < 4; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 0; y < 4; y++) {
    for (int x = 4; x < 8; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 4; y < 8; y++) {
    for (int x = 4; x < 8; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 0; y < 4; y++) {
    for (int x = 8; x < 12; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  for (int y = 4; y < 8; y++) {
    for (int x = 8; x < 12; x++) {
      genList.add(new HilbertGen(wordGenW, wordGenH));
      offsetList.add(new int[] {x * wordGenW, y * wordGenH});
    }
  }
  return new MultiGen(width, height, offsetList, genList);
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
    colorWheel[i] = color(i, 30, 50); // fill our array with colors, gradually changing hue
  }
  popStyle(); // restore styles, including the default RGB color space
  return colorWheel;
}

/**
 * Initializes mapImage with the colors array.
 * MapImage handles the color data for mapper and also serves as our display image.
 * BaseImage is intended as a reference image that usually only changes when you open a new image file.
 */
public void initImages() {
  mapImage = createImage(width, height, ARGB);
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
  mapImage.updatePixels();
  baseImage = mapImage.copy();
}

/**
 * Initializes drawing and drawing interaction variables.
 */
public void initDrawing() {
  currentPoint = new PVector(-1, -1);    // used for drawing to the display
  granularBrushes = new ArrayList<>();
  samplerBrushes = new ArrayList<>();
  hoverBrush = null;
  hoverIndex = -1;
  activeBrush = null;
}

/**
 * Initializes default settings for granular synthesis, defaultGranConfig,
 * and for sampler synthesis, defaultSampConfig.
 */
public void initConfig() {
  defaultGranConfig.grainLengthSamples = granLength;
  defaultGranConfig.hopLengthSamples = granHop;
  defaultGranConfig.curveSteps = curveSteps;
  defaultGranConfig.env = envPreset("Triangle");
  defaultSampConfig.env = envPreset("Pluck");
}

/**
 * Initializes the control palette.
 */
public void initGUI() {
  createGUI();
  controlWindow.loop();
}

/**
 * Preload an audio file using a file path and a filename.
 * @param path        the fully qualified path to the file's directory, ending with a '/'
 * @param filename    the name of the file
 */
public void preloadFiles(String path, String fileName) {
  // the audio file we want to open on startup
  File audioSource = new File(path + fileName);
  // load the file into audio buffer and Brightness channel of display image (mapImage)
  // if audio is also loaded to the image, will set baseImage to the new image
  fileSelected(audioSource);
  // overlay colors on mapImage
  mapImage.loadPixels();
  applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
  mapImage.updatePixels();
  // write mapImage to baseImage
  commitMapImageToBaseImage();
}


public void draw() {
  image(mapImage, 0, 0);
  handleDrawing();          // handle interactive drawing and audio events created by drawing
}

// WE ARE OMITTING ANIMATION METHODS FOR THE MOMENT //

/**
 * Handles user's drawing actions, draws previously recorded brushstrokes,
 * tracks and generates animation and audio events.
 */
public void handleDrawing() {
  // 1) draw existing brushes
  drawBrushShapes();
  // 2) update hover state (pure state update, no action)
  updateHover();
  // 3) if in the process of drawing, accumulate points while mouse is held down
  if (isEditable()) {
    if (mousePressed) {
      addPoint(clipToWidth(mouseX), clipToHeight(mouseY));
    }
    if (allPoints != null && allPoints.size() > 2) {
      PACurveUtility.lineDraw(this, allPoints, dragColor, dragWeight);
    }
  }
  // 4) depending on your event dispatching model, run scheduled events
  runSamplerBrushEvents();
  runPointEvents();
  runGrainEvents();
}

/**
 * @return a reference to the brushstroke the mouse is over, or null if there's no brushstroke.
 */
BrushHit findHoverHit() {
  // Decide z-order: check the list you draw last *first*,
  // so topmost brushes win.
  // granular on top here, descending for loop means most recent brushes are on top
  if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR || drawingMode == DrawingMode.PLAY_ONLY) {
    for (int i = granularBrushes.size() - 1; i >= 0; i--) {
      GranularBrush b = granularBrushes.get(i);
      if (mouseInPoly(b.curve().getBrushPoly())) {
        return new BrushHit(b, i);
      }
    }
  }
  if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER || drawingMode == DrawingMode.PLAY_ONLY) {
    for (int i = samplerBrushes.size() - 1; i >= 0; i--) {
      SamplerBrush b = samplerBrushes.get(i);
      if (mouseInPoly(b.curve().getBrushPoly())) {
        return new BrushHit(b, i);
      }
    }
  }
  return null;
}

/**
 * Update the hoverBrush and hoverIndex global variables.
 */
void updateHover() {
  BrushHit hit = findHoverHit();
  if (hit != null) {
    hoverBrush = hit.brush;
    hoverIndex = hit.index;
  } else {
    hoverBrush = null;
    hoverIndex = -1;
  }
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
 * The built-in mousePressed handler for Processing, used to begin drawing.
 */
public void mousePressed() {
  if (isEditable()) {
    initAllPoints();
  }
  // expand here to call a handler for mousePressed when editing is not on (mode == PLAY_ONLY)
}

public void mouseDragged() {
  // we don't need to handle dragging -- the draw() loop takes care of drawing to the display
}

public void mouseReleased() {
  // if (!(isEditable() && allPoints != null)) return; // EDIT to go ahead in all modes
  if (allPoints.size() > 2) {
    initCurveMakerAndAddBrush();    // create a new brush
    // possible preview action, play on draw
  } else {  // handle the event as a click in mouseClicked()
    // nothing
  }
  allPoints.clear();
}

public void mouseClicked() {
  int x = this.clipToWidth(mouseX);
  int y = this.clipToHeight(mouseY);
  BrushHit hit = findHoverHit();
  if (hit != null) {
    setActiveBrush(hit.brush);      // flag the hit brush as the activeBrush
    if (hit.brush instanceof SamplerBrush) {
      SamplerBrush sb = (SamplerBrush) hit.brush;
      scheduleSamplerBrushClick(sb, x, y);
    } else if (hit.brush instanceof GranularBrush) {
      GranularBrush gb = (GranularBrush) hit.brush;
      scheduleGranularBrushClick(gb, x, y);
    }
    return;
  }
  // in other sketches we called audioMousePressed(), handleClickOutsideBrush() does the same sort of things
  if (isVerbose) println(" in mouseClicked, calling handleClickOutsideBrush, drawing mode is "+ drawingMode.toString());
  handleClickOutsideBrush(x, y);
}

/**
 * built-in keyPressed handler, forwards events to parseKey.
 */
@Override
  public void keyPressed() {
  if (key != CODED) {
    parseKey(key, keyCode);
  } else {
    float g = audioOut.getGain();
    if (keyCode == UP) {
      setAudioGain(g + 3.0f);
      println("---- audio gain is "+ nf(audioOut.getGain(), 0, 2));
    } else if (keyCode == DOWN) {
      setAudioGain(g - 3.0f);
      println("---- audio gain is "+ nf(audioOut.getGain(), 0, 2));
    } else if (keyCode == RIGHT) {
    } else if (keyCode == LEFT) {
    }
  }
}

/**
 * Handles key press events passed on by the built-in keyPressed method.
 * By moving key event handling outside the built-in keyPressed method,
 * we make it possible to post key commands without an actual key event.
 * Methods and interfaces and even other threads can call parseKey().
 * This opens up many possibilities and a some risks, too.
 *
 * @param key
 * @param keyCode
 */
public void parseKey(char key, int keyCode) {
  String msg;
  switch(key) {
  case ' ': // spacebar triggers a brush if we're hovering over a brush, otherwise it triggers a point event
    if (hoverBrush != null) {
      if (hoverBrush instanceof SamplerBrush) {
        SamplerBrush sb = (SamplerBrush) hoverBrush;
        scheduleSamplerBrushClick(sb, clipToWidth(mouseX), clipToHeight(mouseY));
      } else if (hoverBrush instanceof GranularBrush) {
        GranularBrush gb = (GranularBrush) hoverBrush;
        scheduleGranularBrushClick(gb, clipToWidth(mouseX), clipToHeight(mouseY));
      }
    } else {
      handleClickOutsideBrush(clipToWidth(mouseX), clipToHeight(mouseY));
    }
    break;
  case 'c':
  case 'C': // print the current configuration status to the console
    printGConfigStatus();
    break;
  case 't': // switch between Granular and Sampler editing and playing
    if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR) {
      setMode(DrawingMode.DRAW_EDIT_SAMPLER);
      controlWindow.setTitle("Sampler Synth");
    } else if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) {
      setMode(DrawingMode.PLAY_ONLY);
      controlWindow.setTitle("Play Only: Both Synths");
    } else if (drawingMode == DrawingMode.PLAY_ONLY) {
      setMode(DrawingMode.DRAW_EDIT_GRANULAR);
      controlWindow.setTitle("Granular Synth");
    }
    println("---> mode is "+ drawingMode.toString());
    break;
  case 'w': // change the drawing mode of the hover brush
    AudioBrush changed = null;
    if (hoverBrush != null) {
      changed = toggleHoveredBrushType();
    } else if (activeBrush != null) {
      changed = toggleActiveBrushType();
    } else {
      handleClickOutsideBrush(clipToWidth(mouseX), clipToHeight(mouseY));
    }
    syncDrawingModeToBrush(changed);
    break;
  case 'd':
    doPlayOnDraw = !doPlayOnDraw;
    println("-- play on draw is "+ doPlayOnDraw);
    break;
  case 'p': // jitter the pitch of granular gestures
    usePitchedGrains = !usePitchedGrains;
    msg = (usePitchedGrains) ? " jitter granular pitch." : " steady granular pitch.";
    println("-- Play granular synth with "+ msg);
    break;
  case 'k': // apply the hue and saturation in the colors array to mapImage (not to baseImage)
    refreshMapImageFromBase();
    mapImage.loadPixels();
    applyColorShifted(colors, mapImage.pixels, mapper.getImageToSignalLUT(), totalShift);
    mapImage.updatePixels();
    break;
  case 'K': // apply hue and saturation in colors to baseImage and mapImage
    baseImage.loadPixels();
    applyColor(colors, baseImage.pixels, mapper.getImageToSignalLUT());
    baseImage.updatePixels();
    refreshMapImageFromBase();
    break;
  case 'l':
  case 'L':
    isLoadToBoth = !isLoadToBoth;
    println(isLoadToBoth ? "-- load to both image and audio" : "-- load only to image or audio");
    break;
  case 'f':
  case'F': // toggle verbose output to the console
    isVerbose = !isVerbose;
    println("-- isVerbose == "+ isVerbose);
    break;
  case 'o': // open an audio file
    chooseFile();
    break;
  case 'r':
  case 'R': // reset synths to defaults -- TODO may be dropped
    resetToDefaults();
    break;
  case 'q': // automatically set an active GRANULAR brush to have an optimized number of samples
    if (activeBrush instanceof SamplerBrush) {
      println("-- please choose a Granular Brush to adjust resampling and duration values.");
      return;
    }
    printGOptHints(hopScale);
    if (activeBrush != null) {
      activeBrush.cfg().resampleCount = optGrainCount;
      syncGuiFromConfig();
    }
    break;
  case 'x': // delete the current active brush shape or the oldest brush shape
    if (hoverBrush != null) {
      removeHoverBrush();
    } else {
      removeOldestBrush();
    }
    break;
  case 'X': // delete the most recent brush shape
    removeNewestBrush();
    break;
  case 'h':
  case 'H':
    showHelp();
    break;
  default:
    break;
  }
}

/**
 * to generate help output, run RegEx search/replace on parseKey case lines with:
 * // case ('.'): // (.+)
 * // println(" * Press $1 to $2.");
 */
public void showHelp() {
  println(" * Press UP ARROW to increase audio gain by 3 dB.");
  println(" * Press DOWN ARROW to decrease audio gain by 3 dB.");
  println(" * Press 'c' or 'C' to print the current configuration status to the console.");
  println(" * Press 't' to switch between GRANULAR and SAMPLER editing and playing.");
  println(" * Press 'T' to set mode to PLAY ONLY (no editing).");
  println(" * Press 'f' to toggle verbose output to the console.");
  println(" * Press 'r' or 'R' to reset synths to defaults -- TODO may be dropped.");
  println(" * Press 'q' to automatically set an active GRANULAR brush to have an optimized number of samples.");
  println(" * Press 'x' to delete the current active brush shape or the oldest brush shape.");
  println(" * Press 'X' to delete the most recent brush shape.");
}

/**
 * Sets audioOut.gain.
 * @param g  gain value for audioOut, in decibels
 */
public void setAudioGain(float g) {
  audioOut.setGain(g);
  outputGain = audioOut.getGain();
}

/**
 * Sets the drawing mode.
 * @param newMode
 */
void setMode(DrawingMode newMode) {
  if (newMode == drawingMode) return;
  drawingMode = newMode;
  // Clear hover because the hover rules changed
  hoverBrush = null;
  hoverIndex = -1;
  // Choose what should be "active" in the new mode
  AudioBrush nextActive = null;
  switch (drawingMode) {
  case DRAW_EDIT_GRANULAR:
    nextActive = activeGranularBrush;
    gConfig.env = granEnvelope;
    break;
  case DRAW_EDIT_SAMPLER:
    nextActive = activeSamplerBrush;
    break;
  case PLAY_ONLY:
    nextActive = (activeBrush != null) ? activeBrush
      : (activeGranularBrush != null ? activeGranularBrush : activeSamplerBrush);
    break;
  default:
    {
    }
  }
  if (nextActive != null) {
    // Keep indices consistent: use stored index for the corresponding type
    int idx = (nextActive instanceof GranularBrush) ? activeGranularIndex : activeSamplerIndex;
    setActiveBrush(nextActive, idx);     // this also syncs GUI
  } else {
    // No selection for this mode
    activeBrush = null;
    resetConfigForMode();    // reset the GUI control palette
  }
  setControlsEnabled();        // enable or disable controls, depending on the drawing mode
}

/**
 * Reset tool config to defaults (copy, so default config never mutates).
 */
void resetConfigForMode() {
  if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR) {
    gConfig = defaultGranConfig.copy();
  } else if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) {
    gConfig = defaultSampConfig.copy();
  } else {
    // PLAY_ONLY: choose what you want the GUI to show
    // Option: keep last gConfig; or keep a neutral preset:
    // gConfig = defaultGranularConfig.copy();
  }
  syncGuiFromConfig();    // enable or disable controls, depending on the drawing mode
}
