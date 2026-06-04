package net.paulhertz.pixelaudio.example;

import java.io.File;
import java.util.ArrayList;
import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;
import net.paulhertz.pixelaudio.*;
//video export library
import com.hamoid.*;

/**
 * Demo of how to set and save WaveSynth parameters from JSON files.
 * See WaveSynthEditor for the complete set of WaveSynth parameters 
 * you can edit in a GUI, load and save to files, and output as video. 
 * <pre>
 * Press ' ' (spacebar) to toggle animation.
 * Press 'o' to open a new JSON file containing WaveSynth data.
 * Press 'O' to reload a JSON file, or open a JSON file is none has yet been loaded.
 * Press 'j' or 'J' to write WaveSynth data to a JSON file.
 * Press 'g' to swap in a new gen to replace the one in use for mapper.
 * Press 'f' to rotate gen 90 degrees clockwise.
 * Press 'r' or 'R' to reset animation to step 0.
 * Press 'v' or 'V' to record a video .
 * Press 'h' to show Help Message in the console.
 * </pre>
 */
public class JSONWaveSynth extends PApplet {
	PixelAudio pixelaudio;
	HilbertGen hGen;
	BoustropheGen bGen;
	DiagonalZigzagGen zGen;
	PixelMapGen gen;
	PixelAudioMapper mapper;
	ArrayList<WaveData> wdList;
	WaveSynth wavesynth;
	/*
	 * display dimensions are used for PixelMapGen instances, PixelAudioMapper, and
	 * WaveSynth. For HilbertGen and MooreGen, dimensions must be equal powers of 2.
	 */
	int imageWidth = 1024;
	int imageHeight = 1024;	
	// the PImage used by our WaveSynth 
	PImage synthImage;
	// WaveSynth variables
	float myGamma = 1.0f;			// a non-linear brightness adjustment
	int animSteps = 240;			// number of steps in the animation
	int animStop = animSteps;		// The step at which the animation should stop (not used here)
	int step = 0;					// the current step in the animation
	String comments;				// JSON field that provides information about its WaveSynth settings
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
	VideoExport videx = null;		// hamoid library class for video export (requires ffmpeg)
	String videoFilename = "pixelAudio_video.mp4";


	public static void main(String[] args) {
		PApplet.main(new String[] { JSONWaveSynth.class.getName() });
	}

	public void settings() {
		size(imageWidth, imageHeight, JAVA2D);
	}
	
	public void setup() {
		pixelaudio = new PixelAudio(this);
		hGen = new HilbertGen(1024, 1024);
		bGen = new BoustropheGen(1024, 1024);
		zGen = new DiagonalZigzagGen(1024, 1024, AffineTransformType.FLIPY);
		gen = hGen;
		mapper = new PixelAudioMapper(gen);
		wdList = initWaveDataList();
		wavesynth = new WaveSynth(mapper, wdList);
		initWaveSynth(wavesynth);
		synthImage = wavesynth.mapImage;
		// path settings
		showHelp();
	}

	/**
	 * Creates a WaveData list with two entries, use to initialize a WaveSynth
	 * @return an ArrayList of WaveData
	 */
	public ArrayList<WaveData> initWaveDataList() {
		ArrayList<WaveData> list = new ArrayList<WaveData>();
		float frequency = 768.0f;				// frequency of the sine wave
		float amplitude = 0.8f;					// amplitude of the sine wave
		float phase = 0.0f;						// phase of the sine wave: the animation works by incrementing phase
		float dc = 0.0f;						// dc component of signal, increase or decrease brightness
		float cycles = 1.0f;					// number of times to cycle through phase (from 0 to TWO_PI)
		int waveColor = color(159, 190, 251);	// color controlled by a WaveData instance
		int steps = this.animSteps;				// number of steps in animation (application ignores this value)
		// now create a WaveData instance and add it to the list
		WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		// lest's create another WaveData object
		frequency = 192.0f;						// 1/4 of previous frequency
		phase = 0.0f;							// same phase as previous
		cycles = 2.0f;							// cycling twice as fast the previous one
		waveColor = color(209, 178, 117);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		return list;
	}

	/**
	 * Sets up a WaveSynth's variables.
	 * 
	 * @param synth    a WaveSynth instances
	 * @return
	 */
	public WaveSynth initWaveSynth(WaveSynth synth) {
		synth.setGain(0.8f);
		synth.setGamma(myGamma);
		synth.setScaleHisto(false);
		synth.setAnimSteps(this.animSteps);
		synth.setVideoFilename(videoFilename);
		println("--- mapImage size = " + synth.mapImage.pixels.length);
		synth.prepareAnimation();
		synth.renderFrame(0);
		return synth;
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

	public void draw() {
		image(synthImage, 0, 0);
		stepAnimation();
	}

	/**
	 * Advance animation by one step, handle video recording of animation frames
	 */
	public void stepAnimation() {
		if (!isAnimating) return;
		if (step >= animStop) {
			// if (isAnimating) println("--- Completed video at frame "+ animStop);
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
				println("-- video recording frame "+ step +" of "+ animStop);
			}
		}
		wavesynth.renderFrame(step);
	}

	public void keyPressed() {
		switch (key) {
		case ' ': // toggle animation
			isAnimating = !isAnimating;
			println(isAnimating ? "Starting animation at frame " + step + " of " + animSteps
					: "Stopping animation at frame " + step + " of " + animSteps);
			break;
		case 'o': // open a new JSON file containing WaveSynth data
			  // turn off animation while reading new settings for wavesynth
		      oldIsAnimating = isAnimating;
		      isAnimating = false;
		      this.loadWaveData();
		      break;
		case 'O': // reload a JSON file, or open a JSON file is none has yet been loaded
			if (currentDataFile == null) {
				loadWaveData();
			} 
			else {
				fileSelectedOpen(currentDataFile);
				println("-------->>>>> Reloaded file");
			}
			break;
		case 'j': // write WaveSynth data to a JSON file
		case 'J':
			saveWaveData();
			break;
		case 'g': // swap in a new gen to replace the one in use for mapper
			if (gen == zGen) {
				gen = hGen;
				swapGen(gen);
				break;
			}
			if (gen == hGen) {
				gen = bGen;
				swapGen(gen);
				break;
			}
			if (gen == bGen) {
				gen = zGen;
				swapGen(gen);
			}
			break;
		case 'f': // rotate gen 90 degrees clockwise
			gen.setTransformType(AffineTransformType.R270);
			swapGen(gen);
			break;
		case 'r': // reset animation to step 0
		case 'R':
			step = 0;
			break;
		case 'v': // record a video 
		case 'V':
			isRecordingVideo = !isRecordingVideo;
			println("isRecordingVideo is "+ isRecordingVideo);
			if (isRecordingVideo) {
				step = 0;
				wavesynth.renderFrame(step);
				isAnimating = true;
			}
			break;
		case 'h': // show Help Message in the console
			showHelp();
			break;
		default:
			break;
		}
	}

	public void showHelp() {
		println(" * Press ' ' (spacebar) to toggle animation.");
		println(" * Press 'o' to open a new JSON file containing WaveSynth data.");
		println(" * Press 'O' to reload a JSON file, or open a JSON file is none has yet been loaded.");
		println(" * Press 'j' or 'J' to write WaveSynth data to a JSON file.");
		println(" * Press 'g' to swap in a new gen to replace the one in use for mapper.");
		println(" * Press 'f' to rotate gen 90 degrees clockwise.");
		println(" * Press 'r' or 'R' to reset animation to step 0.");
		println(" * Press 'v' or 'V' to record a video .");
		println(" * Press 'h' to show Help Message in the console.");
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
	 * Outputs current wavesynth settings and WaveData list.
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
