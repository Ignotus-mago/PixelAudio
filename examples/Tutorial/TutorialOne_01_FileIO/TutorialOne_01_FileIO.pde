/**
 * TutorialOne_00_BeginHere showed the basics of loading the PixelAudio library and 
 * using it to display a rainbow array of colors along the signal path. It also provided
 * a basic response to mousePressed events. Tutorial_01_FileIO adds basic input and
 * output of audio and image files to the previous materials.  
 * 
 *   1. Launch the sketch. A rainbow of colors appears in the display window, arrayed
 *      along a signal path created by six connected Hilbert curves. 
 *
 *   2. Press 'o' to get an Open File dialog. Open an audio file or an image file.
 *      If you choose an audio file, a representation of its sample data is written 
 *      to mapImage and appears in the display window. 
 *********>>>      Press the 'k' key to show the spectrum colors again. 
 *      If you choose an image file, a representation of its pixel data is written
 *      to audioSignal, though we won't have tools to play the sound until
 *      the next tutorial. 
 *
 *   3. Press 'w' to write the image colors to the audio buffer as transcoded values.
 *      Press 'W' to write the audio buffer samples to the image as color values.
 *      These two methods triggered by these key commands, writeAudioToImage() and 
 *      writeAudioToImage()(), are run automatically when you open an image or audio
 *      file. In later tutorials, we won't run them automatically, so that we can
 *      work with audio samples and image pixels separately.
 *
 *   4. Click on the image to get information about coordinates in the image and 
 *      the corresponding audio sample index in audioSignal. 
 *
 *   5. Press 's' to save image to a PNG file.
 *      Press 'S' to save audio to a WAV file.
 *      Try opening the file "garden.jpg" and save it as an audio file. Then open 
 *      and play the audio file. 
 *
 *   6. Check out the code for the basic steps of opening, transcoding, and saving
 *      audio and image files. Note how writeAudioToImage() and writeAudioToImage()
 *      make use of the PixelAudioMapper mapper. 
 *
 *          mapper.mapSigToImg(sig, img.pixels, chan);
 *          mapper.mapImgToSig(img.pixels, sig, chan);
 *
 *      These methods work because mapper can use its PixelMapGen to find corresponding
 *      pixels and samples. PixelAudioMapper also has methods to transcode audio samples, 
 *      in the range -1.0 to 1.0, to RGB pixel data. While it has methods for all 
 *      available color channels in the RGB and HSB color spaces, we typically transcode
 *      audio to the brightness channel ("L") in HSB or turn it into grayscale values ("ALL"). 
 *      In the LoadImageToAudio and LoadAudioToImage example sketches, you can see how to 
 *      write to all of the available color channels. 
 *      
 *      Note that mapSigToImg() and mapImgToSig() require sig and img.pixels to be
 *      _exactly_ the same size, mapSize. If this isn't the case, they throw an error.
 *      PixelAudioMapper has additional methods for use when the signal and pixel arrays
 *      are of different sizes or when you want to transcode only part of an array.
 *      The mapSigToImg and mapImgToSig are the ones we'll use most of the time in 
 *      tutorials and examples. 
 *
 * KEY COMMANDS
 *      
 * Press 'o' or 'O' to open an audio or image file.
 * Press 's' to save image to a PNG file.
 * Press 'S' to save audio to a WAV file.
 * Press 'w' to write the image colors to the audio buffer as transcoded values.
 * Press 'W' to write the audio buffer samples to the image as color values.
 * 
 */

// File IO support from Java
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

// Audio support from Java
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

//Mama's ever-lovin' blue-eyed PixelAudio library
import net.paulhertz.pixelaudio.*;
import net.paulhertz.pixelaudio.PixelAudioMapper.ChannelNames;

//audio library
import ddf.minim.*;
import ddf.minim.ugens.*;

//video export library
import com.hamoid.*;


// PixelAudio variables and objects
PixelAudio pixelaudio;     // our shiny new library
MultiGen multigen;         // a PixelMapGen that links together multiple PixelMapGens
int genWidth = 512;        // width of multigen PixelMapGens, must be a power of 2
int genHeight = 512;       // height of  multigen PixelMapGens, must equal genWidth
PixelAudioMapper mapper;   // object for reading, writing, and transcoding audio and image data
int mapSize;               // size of the display bitmap, audio signal, wavesynth pixel array, mapper arrays, etc.
PImage mapImage;           // image for display
PixelAudioMapper.ChannelNames chan = ChannelNames.ALL;    // color channel(s) to use in transcoding audio to image
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

/* ------------------------------------------------------------------ */
/*                          AUDIO VARIABLES                           */
/* ------------------------------------------------------------------ */

Minim minim;               // library that handles audio 
AudioOutput audioOut;      // for future use
float[] audioSignal;       // the audio signal as an array of floats
int audioLength;           // length of audio signal, audio buffer
MultiChannelBuffer playBuffer;  // a buffer for playing the audio signal
int sampleRate = 48000;         // a critical value for display and audio



public void settings() {
  size(3 * genWidth, 2 * genHeight);
}

public void setup() {
  frameRate(30);
  pixelaudio = new PixelAudio(this);
  multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
  mapper = new PixelAudioMapper(multigen);
  mapSize = mapper.getSize();
  colors = getColors(mapSize); // create an array of rainbow colors
  initImages();
  initAudio();
  showHelp();
}

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

public void initImages() {
  mapImage = createImage(width, height, ARGB); // an image to use with mapper
  mapImage.loadPixels();
  mapper.plantPixels(colors, mapImage.pixels, 0, mapSize); // load colors to mapImage following signal path
  mapImage.updatePixels();
}

public void initAudio() {
  minim = new Minim(this);
  // use the getLineOut method of the Minim object to get an AudioOutput object
  this.audioOut = minim.getLineOut(Minim.MONO, 1024, sampleRate);
  // create a Minim MultiChannelBuffer with one channel
  this.playBuffer = new MultiChannelBuffer(1024, 1);
}

public void draw() {
  image(mapImage, 0, 0);
}

/**
 * The built-in mousePressed handler for Processing
 */
public void mousePressed() {
  int samplePos = mapper.lookupSample(mouseX, mouseY);
  println("mousePressed:", mouseX, mouseY, "signal path index", samplePos);
}

/**
 * The built-in keyPressed handler for Processing
 */
public void keyPressed() {
  switch(key) {
  case 'o': case 'O': // open an audio or image file
    chooseFile();
    break;
  case 's': // save image to a PNG file
    saveToImage();
    break;
  case 'S': // save audio to a WAV file
    saveToAudio();
    break;
  case 'w': // write the image colors to the audio buffer as transcoded values
    writeImageToAudio(mapImage, mapper, audioSignal, PixelAudioMapper.ChannelNames.L);
    println("--->> Wrote image to audio as audio data.");
    break;
  case 'W': // write the audio buffer samples to the image as color values
    writeAudioToImage(audioSignal, mapper, mapImage, chan);
    println("--->> Wrote audio to image as pixel data.");
    break;
  case 'h': case 'H': // show help text and key commands
    showHelp();
    break;
  default:
    break;
  }
}

public void showHelp() {
  println(" * Press 'o' or 'O' to open an audio or image file.");
  println(" * Press 's' to save image to a PNG file.");
  println(" * Press 'S' to save audio to a WAV file.");
  println(" * Press 'w' to write the image colors to the audio buffer as transcoded values.");
  println(" * Press 'W' to write the audio buffer samples to the image as color values.");
  println(" * Press 'h' or 'H' to show help text and key commands.");
}
