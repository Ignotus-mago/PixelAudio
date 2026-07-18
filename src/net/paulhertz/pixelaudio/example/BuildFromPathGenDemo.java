package net.paulhertz.pixelaudio.example;

import processing.core.*;
import processing.data.*;

import java.io.File;
import java.util.ArrayList;

import net.paulhertz.pixelaudio.*;


/**
 * BuildFromPathGenDemo shows how PixelMapGen data can be stored in and
 * loaded from a JSON file.
 *
 * There are three JSON files available in the data folder of this sketch:
 * vertical.json, bigDiagonal.json, multigen.json.
 * This sketch provides various methods for reading and writing JSON files.
 * The JSON file should contain a "header" with fields "PXAU", "Description" and "PixelAudioURL".
 * The value of the PXAU field is set to "BGEN" to identify the JSON data as encoding a PixelMapGen.
 * The PXAU field is repeated in the body of the JSON file, which must contain the following fields:
 *
 *   "PXAU": "BGEN"
 *   "width": <PixelMapGen width>
 *   "height": <PixelMapGen height>
 *   "pixelMap": [...]
 *
 * "pixelMap" flags an array of integers that are the values of the indices of the signal path,
 * signalToImageLUT, from a PixelMapGen. The signalToImageLUT values can be decoded to (x, y)
 * coordinates and used to initialize the remaining fields of a BuildFromPathGen. Most of the
 * work gets done by calling importGenDataJSON(JSONObject json) with the data loaded from the
 * JSON file. The type of PixelMapGen created in importGenDataJSON() is a BuildFromPathGen.
 * The BuildFromPathGen constructor requires width and height. To complete initialization,
 * you must call BuildFromPathGen.setPixelMap() with the integer array derived from the JSON
 * data. Then call BuildFromPathGen.generate() to initialize all remaining variables.
 *
 *     BuildFromPathGen myGen = new BuildFromPathGen(w, h);
 *     int[] pixelMap = map.toIntArray();
 *     // always call BuildFromPathGen.setPixelMap() before you call BuildFromPathGen.generate()
 *     myGen.setPixelMap(pixelMap);
 *     myGen.generate();
 *
 * For most purposes, the BuildFromPathGen can be handled as PixelMapGen. Note that only the
 * signalToImageLUT data is saved to the JSON. Color data and audio samples are external to the
 * PixelMapGen. You can save them to image or audio files and reload them later.
 *
 */
public class BuildFromPathGenDemo extends PApplet {

	PixelAudio pixelaudio;
	HilbertGen hGen;
	MultiGen multigen;
	int genWidth = 256;          // width of each gen (must be a power of 2 for HilbertGen
	int genHeight = 256;         // height of each gen (must equal width of genO
	int rows = 3;                // number of rows of gens
	int columns = 2;             // number of columns of gens
	PixelAudioMapper mapper;     // PixelAudioMapper to handle mapping of pixels to image

	PImage mapImage;             // A PImage to display
	PImage baseImage;            // A PImage for reference
	int[] colors;                // an array of color values for pixels
	int shift = 512;             // amount to displace pixels for animation
	int totalShift = 0;          // accumulated shift while animating
	boolean isAnimating = false;    // well, are we animating or not?
	boolean oldIsAnimating;

	// for JSON file I/O
	String currentFileName;
	PixelMapGen currentGen;

	public static void main(String[] args) {
		PApplet.main(new String[] { BuildFromPathGenDemo.class.getName() });
	}

	public void settings() {
		size(rows * genWidth, columns * genHeight);
	}

	public void setup() {
		windowResizable(true);
		// 1. initialize PixelAudio
		pixelaudio = new PixelAudio(this);
		// 2. create a multigen for immediate display
		multigen = hilbertLoop3x2(genWidth, genHeight);
		currentGen = multigen;
		// 3. initialize a PixelAudioMapper object with the gen
		mapper = new PixelAudioMapper(currentGen);
		// 4. create an image for display
		mapImage = createImage(width, height, RGB);
		// 5. get colors to display on the signal path of mapImage
		initImages();
	}

	/**
	 * Generates a looping fractal signal path consisting of 6 HilbertGens,
	 * arranged 3 wide and 2 tall, to fit a 3 * genW by 2 * genH image.
	 * This particular MultiGen configuration was used so extensively in
	 * my sample code that I've given it its own static method in HilbertGen.
	 *
	 * Note that genW must be a power of 2 and genH == genW. For the
	 * image size we're using in this example, image width = 3 * genW
	 * and image height = 2 * genH.
	 *
	 * @param genW    width of each HilbertGen
	 * @param genH    height of each HilbertGen
	 * @return
	 */
	public MultiGen hilbertLoop3x2(int genW, int genH) {
		// list of PixelMapGens that create a path through an image using PixelAudioMapper
		ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
		// list of x,y coordinates for placing gens from genList
		ArrayList<int[]> offsetList = new ArrayList<int[]>();
		genList.add(new HilbertGen(genW, genH, AffineTransformType.FX270));
		offsetList.add(new int[] { 0, 0 });
		genList.add(new HilbertGen(genW, genH, AffineTransformType.NADA));
		offsetList.add(new int[] { genW, 0 });
		genList.add(new HilbertGen(genW, genH, AffineTransformType.FX90));
		offsetList.add(new int[] { 2 * genW, 0 });
		genList.add(new HilbertGen(genW, genH, AffineTransformType.FX90));
		offsetList.add(new int[] { 2 * genW, genH });
		genList.add(new HilbertGen(genW, genH, AffineTransformType.R180));
		offsetList.add(new int[] { genW, genH });
		genList.add(new HilbertGen(genW, genH, AffineTransformType.FX270));
		offsetList.add(new int[] { 0, genH });
		return new MultiGen(3 * genW, 2 * genH, offsetList, genList);
	}

	public int[] getColors() {
		int[] colorWheel = new int[mapper.getSize()];
		pushStyle();
		colorMode(HSB, colorWheel.length, 100, 100);
		int h = 0;
		for (int i = 0; i < colorWheel.length; i++) {
			colorWheel[i] = color(h, 66, 66);
			h++;
		}
		popStyle();
		return colorWheel;
	}

	/**
	 * Initializes mapImage with the colors array, copies mapImage to baseImage.
	 * MapImage handles the color data for mapper and also serves as our display image.
	 * BaseImage is intended as a reference image that typically only changes when you load a new image.
	 */
	public void initImages() {
		mapImage = createImage(width, height, ARGB);
		mapImage.loadPixels();
		colors = getColors();
		mapper.plantPixels(colors, mapImage.pixels, 0, mapper.getSize());
		mapImage.updatePixels();
		baseImage = mapImage.copy();
		totalShift = 0;
	}

	public void draw() {
		image(mapImage, 0, 0, width, height);
		if (isAnimating)
			animate();
	}

	public void keyPressed() {
		switch (key) {
		case ' ': // toggle animation
			isAnimating = !isAnimating;
			break;
		case 'j': // export signal path to a JSON file
			exportGenData(currentGen);
			break;
		case 'o': // import signal path from a JSON file
			importGenData();
			break;
		case 'h': // show help message in the console
			showHelp();
			break;
		default:
			break;
		}
	}

	public void showHelp() {
		println(" * Press ' ' to toggle animation.");
		println(" * Press 'j' to export signal path to a JSON file.");
		println(" * Press 'o' to import signal path from a JSON file.");
		println(" * Press 'h' to show help message in the console.");
	}

	public void animate() {
		totalShift += shift;
		mapImage.loadPixels();
		// PixelAudioMapper has the method we need
		mapper.copyPixelsAlongPathShifted(baseImage.pixels, mapImage.pixels, totalShift);
		mapImage.updatePixels();
	}	

	// -------------------------- //
	//      JSON FILE I/O        //
	//-------------------------- //

	public void exportGenData(PixelMapGen gen) {
		currentGen = gen;
		selectOutput("Select a file to write to:", "fileSelectedWrite");
	}

	public void fileSelectedWrite(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
			return;
		}
		println("User selected " + selection.getAbsolutePath());
		// Do we have a .json at the end?
		if (selection.getName().length() < 5 || selection.getName().indexOf(".json") != selection.getName().length() - 5) {
			// missing ".json"
			currentFileName = selection.getAbsolutePath() + ".json";
		} 
		else {
			currentFileName = selection.getAbsolutePath();
		}
		JSONObject genJSON = new JSONObject();
		genJSON.setJSONObject("header", getJSONHeader());
		genJSON.setString("PXAU", "BGEN");
		genJSON.setInt("width", currentGen.getWidth());
		genJSON.setInt("height", currentGen.getHeight());
		JSONArray pixelMapJSON = new JSONArray();
		int[] pixelMap = currentGen.getPixelMap();
		for (int i = 0; i < currentGen.getSize(); i++) {
			pixelMapJSON.append(pixelMap[i]);
		}
		genJSON.setJSONArray("pixelMap", pixelMapJSON);
		saveJSONObject(genJSON, currentFileName);
	}

	public JSONObject getJSONHeader() {
		// flag this JSON file as WaveSynthEditor data using a "PXAU" key with value "WSYN"
		// add some other pertinent information
		JSONObject header = new JSONObject();
		header.setString("PXAU", "BGEN");
		header.setString("description", "BuildFromPathGen data created with the PixelAudio library by Paul Hertz.");
		header.setString("PixelAudioURL", "https://github.com/Ignotus-mago/PixelAudio");
		return header;
	}


	public void importGenData() {
		oldIsAnimating = isAnimating;
		isAnimating = false;
		selectInput("Select a file to open", "fileSelectedOpen");
	}

	public void fileSelectedOpen(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
			return;
		}
		File currentDataFile = selection;
		println("User selected " + currentDataFile.getAbsolutePath());
		currentFileName = currentDataFile.getAbsolutePath();
		JSONObject json = loadJSONObject(currentFileName);
		boolean goodHeader = checkJSONHeader(json, "PXAU", "BGEN");
		if (goodHeader) {
			println("--->> JSON file contains BuildFromPathGen data. It should load correctly.");
		} 
		else {
			println("--->> JSON file apparently does not contain BuildFromPathGen data. Will try to load,anyhow.");
		}
		PixelMapGen myGen = importGenDataJSON(json);
		if (myGen != null) {
			println("----->>> Loading PixelMapGen from JSON file. ");
			currentGen = myGen;
			mapper.setGenerator(currentGen);
			if (mapper.getWidth() != width || mapper.getHeight() != height) {
				windowResize(mapper.getWidth(), mapper.getHeight());
			}
			initImages();
		}
		isAnimating = oldIsAnimating;
	}

	boolean checkJSONHeader(JSONObject json, String key, String val) {
		JSONObject header = (json.isNull("header") ? null : json.getJSONObject("header"));
		String pxau;
		if (header != null) {
			pxau = (header.isNull(key)) ? "" : header.getString(key);
		} 
		else {
			pxau = (json.isNull(key)) ? "" : json.getString(key);
		}
		if (pxau.equals(val)) {
			return true;
		} 
		else {
			return false;
		}
	}

	public PixelMapGen importGenDataJSON(JSONObject json) {
		int w = (json.isNull("width")) ? 0 : json.getInt("width");
		int h = (json.isNull("height")) ? 0 : json.getInt("height");
		JSONArray map = (json.isNull("pixelMap")) ? null : json.getJSONArray("pixelMap");
		if (map != null && w != 0 && h != 0) {
			BuildFromPathGen myGen = new BuildFromPathGen(w, h);
			// BuildFromPathGen myGen = new BuildFromPathGen(w, h, AffineTransformType.ROT90);
			int[] pixelMap = map.toIntArray();
			// always call BuildFromPathGen.setPixelMap() before you call BuildFromPathGen.generate()
			myGen.setPixelMap(pixelMap);
			myGen.generate();
			return myGen;
		} else {
			return null;
		}
	}

}
