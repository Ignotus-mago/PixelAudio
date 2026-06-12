package net.paulhertz.pixelaudio.example;

import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.File;
import java.util.ArrayList;
import net.paulhertz.pixelaudio.*;

import com.hamoid.*;

/**
 * BigWaveSynth shows how to load a WaveSynth into the pixel array of a
 * MultiGen. MultiGen is child class of PixelMapGen that allows you to use
 * multiple PixelMapGens to cover a single image with a single signal
 * path through them. This example also allows you to load JSON files in 
 * this example's data folder to reconfigure the WaveSynth. Initially, we 
 * call initWaveDataList() to create a WaveData array with two operators. 
 * <pre>
 * Press ' ' to turn animation on or off.
 * Press 'g' to step through PixelMapGen instances hilb3x2Gen, bGen, and zGen. 
 * Press 'o' to open a JSON file that defines a WaveSynth .
 * Press 'O' to reopen JSON file, if one is already open, or open a new JSON file.
 * Press 'j' or 'J' to save WaveSynth data to a JSON file.
 * Press 'f' to print the frameRate to the console.
 * Press 'r' to animation step to 0.
 * Press 'v' or 'V' to toggle video recording.
 * Press 'h' to show help and key commands.
 * </pre>
 * See WaveSynthEditor for the complete set of WaveSynth parameters 
 * you can edit in a GUI, load and save to files, and output as video. <br>
 * See also: BigWaveSynthAudio, WaveSynthSequencer.
 * 
 */
public class BigWaveSynth extends PApplet {
	PixelAudio pixelaudio;
	PixelMapGen gen;
	
	MultiGen hilb3x2Gen;
	BoustropheGen bGen;
	DiagonalZigzagGen zGen;

	int rows = 3;
	int columns = 2;
	int genWidth = 256;
	int genHeight = 256;
	PixelAudioMapper mapper;
	ArrayList<WaveData> wdList;
	WaveSynth wavesynth;
	PImage synthImage;
	// WaveSynth variables
	float myGamma = 1.0f;			// a non-linear brightness adjustment
	int animSteps = 240;			// number of steps in the animation
	int animStop = animSteps;		// The step at which the animation should stop (not used here)
	int step = 0;					// the current step in the animation
	String comments;				// a JSON field that provides information about the WaveSynth effects it produces
	
	// file i/o
	String jsonFolder = "/JSON_data/";
	File currentDataFile;
	String currentFileName;
	JSONObject json;
	
	// animation
	boolean isAnimating = true;
	boolean oldIsAnimating;
	boolean isLooping = true;
	
	// video export
	boolean isRecordingVideo = false;
	VideoExport videx = null;    // hamoid library class for video export (requires ffmpeg)
	String videoFilename = "pixelAudio_video.mp4";


	
	public static void main(String[] args) {
		PApplet.main(new String[] { BigWaveSynth.class.getName() });
	}

	public void settings() {
		// display window is scaled by 2 with respect to PixelMapGen instances
		size(2 * rows * genWidth, 2 * columns * genHeight);
	}

	public void setup() {
		pixelaudio = new PixelAudio(this);
		hilb3x2Gen = hilbertLoop3x2(genWidth, genHeight);
		println("-- hilb3x2Gen dimensions: ", hilb3x2Gen.getWidth(), hilb3x2Gen.getHeight());
		bGen = new BoustropheGen(rows * genWidth, columns * genHeight);
		zGen = new DiagonalZigzagGen(rows * genWidth, columns * genHeight);
		gen = hilb3x2Gen;
		mapper = new PixelAudioMapper(gen);
		wdList = initWaveDataList();
		wavesynth = new WaveSynth(mapper, wdList);
		initWaveSynth(wavesynth);
		synthImage = wavesynth.mapImage;
		showHelp();
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
	 * @return a 3 x 2 array of Hilbert curves, connected in 
	 *         a loop (3 * genWidth by 2 * genHeight pixels)
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
	 * Initializes a list of WaveData for use by a WaveSynth.
	 * 
	 * @return an ArrayList of WaveData objects
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
	 * Sets gain, gamma, isScaleHisto, animSteps, and sampleRate instance variables 
	 * of a WaveSynth object and generates its first frame of animation.
	 * 
	 * @param synth		a WaveSynth object whose attributes will be set
	 * @return			the WaveSynth object with attributes set
	 */
	public WaveSynth initWaveSynth(WaveSynth synth) {
		synth.setGain(0.8f);
		synth.setGamma(myGamma);
		synth.setScaleHisto(false);
		synth.setAnimSteps(this.animSteps);
		synth.setSampleRate(genWidth * genWidth);
		println("--- mapImage size = " + synth.mapImage.pixels.length);
		synth.prepareAnimation();
		synth.renderFrame(0);
		return synth;
	}

	public void draw() {
		image(synthImage, 0, 0, width, height);
		if (isAnimating) stepAnimation();
	}
	
	/**
	 * Sets a new PixelMapGen for a PixelAudioMapper
	 * @param gen
	 */
	public void swapGen(PixelMapGen gen) {
		mapper.setGenerator(gen);
		// if we had a new mapper, we would call wavesynth.setMapper(mapper) and reset
		// synthImage locally.
		// As it is, mapper only changed its variables, so the swap is really simple
	}

	public void keyPressed() {
		switch (key) {
		case ' ': // turn animation on or off
			isAnimating = !isAnimating;
			println(isAnimating ? "Starting animation at frame " + step + " of " + animSteps
					: "Stopping animation at frame " + step + " of " + animSteps);
			break;
		case 'g': // swap in a new gen to replace the one in use for mapper
			if (gen == zGen) {
				gen = hilb3x2Gen;
				swapGen(gen);
				break;
			}
			if (gen == hilb3x2Gen) {
				gen = bGen;
				swapGen(gen);
				break;
			}
			if (gen == bGen) {
				gen = zGen;
				swapGen(gen);
			}
			break;
		case 'o': // open a JSON file that defines a WaveSynth 
			// turn off animation while reading new settings for wavesynth
			oldIsAnimating = isAnimating;
			isAnimating = false;
			this.loadWaveData();
			break;
		case 'O': // reopen JSON file, if one is already open, or open a new JSON file
			if (currentDataFile == null) {
				loadWaveData();
			} else {
				fileSelectedOpen(currentDataFile);
				println("-------->>>>> Reloaded file");
			}
			break;
		case 'j': case 'J': // save WaveSynth data to a JSON file
			saveWaveData();
			break;
		case 'f': // print the frameRate to the console
			println("--->> frame rate: "+ frameRate);
		case 'r': case 'R': // reset animation step to 0 
			step = 0;
			wavesynth.renderFrame(step);
			break;
		case 'v': case 'V': // toggle video recording
			isRecordingVideo = !isRecordingVideo;
			println("isRecordingVideo is "+ isRecordingVideo);
			if (isRecordingVideo) {
				step = 0;
				wavesynth.renderFrame(step);
				isAnimating = true;
			}
			break;
		case 'h': // show help and key commands
			break;
		default:
			break;
		}
	}
	
	public void showHelp() {
		println(" * Press ' ' to turn animation on or off.");
		println(" * Press 'g' to step through PixelMapGen instances hilb3x2Gen, bGen, and zGen.");
		println(" * Press 'o' to open a JSON file that defines a WaveSynth .");
		println(" * Press 'O' to reopen JSON file, if one is already open, or open a new JSON file.");
		println(" * Press 'j' or 'J' to save WaveSynth data to a JSON file.");
		println(" * Press 'f' to print the frameRate to the console.");
		println(" * Press 'r' to animation step to 0.");
		println(" * Press 'v' or 'V' to toggle video recording.");
		println(" * Press 'h' to show help and key commands.");
	}

	public void stepAnimation() {
		if (!isAnimating) return;
		if (step >= animStop) {
			println("--- Completed video at frame "+ animStop);
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
				println("-- video recording frame "+ step +" of "+ animStop);
			}
		}
		wavesynth.renderFrame(step);
	}
	

	//-------------------------------------------//
	//               JSON FILE I/O               //
	//-------------------------------------------//
	

	// select a file of WaveData objects in JSON format to open
	public void loadWaveData() {
		File folderToStartFrom = new File(dataPath("") + jsonFolder + "//*.json");
		selectInput("Select a file to open", "fileSelectedOpen", folderToStartFrom);
	}

	public void fileSelectedOpen(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
			isAnimating = oldIsAnimating;
			return;
		}
		currentDataFile = selection;
		println("User selected " + selection.getAbsolutePath());
		currentFileName = selection.getAbsolutePath();
		json = loadJSONObject(currentFileName);
		setWaveSynthFromJSON(json, wavesynth);
		surface.setTitle(currentFileName);
		isAnimating = oldIsAnimating;
	}

	public void setWaveSynthFromJSON(JSONObject json, WaveSynth synth) {
		// set animation globals and WaveSynth properties
		animSteps = (json.isNull("steps")) ? 240 : json.getInt("steps");
		synth.setAnimSteps(animSteps);
		animStop = (json.isNull("stop")) ? this.animSteps : json.getInt("stop");
		synth.setStop(animStop);
		myGamma = (json.isNull("gamma")) ? 1.0f : json.getFloat("gamma");
		synth.setGamma(myGamma);
		comments = (json.isNull("comments")) ? "" : json.getString("comments");
		synth.setComments(comments);
		synth.setGain(json.isNull("blendFactor") ? 0.5f : json.getFloat("blendFactor"));
		synth.setVideoFilename((json.isNull("filename")) ? "wavesynth.mp4" : json.getString("filename"));
		synth.setScaleHisto((json.isNull("scaleHisto")) ? false : json.getBoolean("scaleHisto"));
		if (synth.isScaleHisto()) {
			synth.setHistoHigh((json.isNull("histoHigh")) ? 255 : json.getInt("histoHigh"));
			synth.setHistoLow((json.isNull("histoLow")) ? 0 : json.getInt("histoLow"));
		}
		// now load the JSON wavedata into ArrayList<WaveData> waveDataList
		JSONArray waveDataArray = json.getJSONArray("waves");
		int datalen = waveDataArray.size();
		ArrayList<WaveData> waveDataList = new ArrayList<WaveData>(datalen);
		for (int i = 0; i < datalen; i++) {
			// load fields common to both old and new format
			JSONObject waveElement = waveDataArray.getJSONObject(i);
			float f = waveElement.getFloat("freq");
			float a = waveElement.getFloat("amp");
			float p = waveElement.getFloat("phase");
			// float pInc = waveElement.getFloat("phaseInc");
			float dc = 0.0f;
			if (!waveElement.isNull("dc")) {
				dc = waveElement.getFloat("dc");
			}
			JSONObject rgbColor = waveElement.getJSONObject("color");
			int c = color(rgbColor.getInt("r"), rgbColor.getInt("g"), rgbColor.getInt("b"));
			float cycles;
			cycles = waveElement.getFloat("cycles");
			// frequency, amplitude, phase, dc, cycles, color, steps
			WaveData wd = new WaveData(f, a, p, dc, cycles, c, animSteps);
			waveDataList.add(wd);
		}
		synth.setWaveDataList(waveDataList);
		synth.prepareAnimation();
		synth.renderFrame(0);
		printWaveData(synth);
	}

	/**
	 * Outputs fields from current waveAnimal and it waveDataList
	 */
	public void printWaveData(WaveSynth synth) {
		java.nio.file.Path path = java.nio.file.Paths.get(currentFileName);
		String fname = path.getFileName().toString();
		println("\n--------=====>>> Current WaveSynth instance for file " + fname + " <<<=====--------\n");
		println("Animation steps: " + synth.getAnimSteps());
		// println("Stop frame: "+ waveAnimal.getAnimSteps());
		println("gain: " + synth.getGain());
		println("gamma: " + synth.getGamma());
		if (synth.isScaleHisto()) {
			println("scaleHisto: " + synth.isScaleHisto());
			println("histoLow: " + synth.getHistoLow());
			println("histoHigh: " + synth.getHistoHigh());
		}
		println(fname);
		println("video filename: " + synth.getVideoFilename());
		// println("WaveData list for: "+ videoFilename);
		for (int i = 0; i < synth.waveDataList.size(); i++) {
			WaveData wd = synth.waveDataList.get(i);
			println("  " + (i + 1) + ":: " + wd.toString());
		}
		println("comments: " + synth.getComments() +"\n");
	}
	
	public void saveWaveData() {
		selectOutput("Select a file to write to:", "fileSelectedWrite");
	}

	public void fileSelectedWrite(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
			return;
		}
		WaveSynth synth = this.wavesynth;
		println("User selected " + selection.getAbsolutePath());
		// Do we have a .json at the end?
		if (selection.getName().length() < 5
				|| selection.getName().indexOf(".json") != selection.getName().length() - 5) {
			// problem missing ".json"
			currentFileName = selection.getAbsolutePath() + ".json"; // very rough approach...
		} else {
			currentFileName = selection.getAbsolutePath();
		}
		// put WaveData objects into an array
		JSONArray waveDataArray = new JSONArray();
		JSONObject waveElement;
		WaveData wd;
		for (int i = 0; i < synth.waveDataList.size(); i++) {
			wd = synth.waveDataList.get(i);
			waveElement = new JSONObject();
			waveElement.setInt("index", i);
			waveElement.setFloat("freq", wd.freq);
			waveElement.setFloat("amp", wd.amp);
			waveElement.setFloat("phase", wd.phase);
			waveElement.setFloat("phaseInc", wd.phaseInc);
			waveElement.setFloat("cycles", wd.phaseCycles);
			waveElement.setFloat("dc", wd.dc);
			// BADSR settings
			int[] rgb = PixelAudioMapper.rgbComponents(wd.waveColor);
			JSONObject rgbColor = new JSONObject();
			rgbColor.setInt("r", rgb[0]);
			rgbColor.setInt("g", rgb[1]);
			rgbColor.setInt("b", rgb[2]);
			waveElement.setJSONObject("color", rgbColor);
			// append wave data to array
			waveDataArray.append(waveElement);
		}
		// put the array into an object that tracks other state variables
		JSONObject stateData = new JSONObject();
		stateData.setInt("steps", synth.animSteps);
		stateData.setInt("stop", animStop);
		stateData.setFloat("blendFactor", synth.gain);
		stateData.setInt("dataFormat", 2);
		if (!selection.exists())
			stateData.setString("comments", "---");
		else
			stateData.setString("comments", synth.comments);
		// String videoName = selection.getName(); 
		String videoName = synth.videoFilename;
		if (videoName == null || videoName.equals("")) {
			videoName = selection.getName();
			if (videoName.indexOf(".json") != -1) {
				videoName = videoName.substring(0, videoName.indexOf(".json")) + ".mp4";
			} else {
				videoName += ".mp4";
			}
		}
		println("----->>> video name is " + videoName);
		synth.videoFilename = videoName; // ???
		stateData.setString("filename", videoName);
		stateData.setFloat("gamma", synth.gamma);
		stateData.setBoolean("scaleHisto", synth.isScaleHisto);
		stateData.setFloat("histoHigh", synth.histoHigh);
		stateData.setFloat("histoLow", synth.histoLow);
		stateData.setJSONArray("waves", waveDataArray);
		saveJSONObject(stateData, currentFileName);
		currentDataFile = new File(currentFileName);
		surface.setTitle(currentFileName);
	}
	
}
