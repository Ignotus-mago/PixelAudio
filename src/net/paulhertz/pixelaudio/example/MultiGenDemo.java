package net.paulhertz.pixelaudio.example;

import processing.core.*;
import java.util.ArrayList;
import java.util.Random;

import net.paulhertz.pixelaudio.*;
//import net.paulhertz.pixelaudio.AffineTransformType.*;

/**
 * MultiGenDemo: How to Combine Gens into a MultiGen
 * 
 * MultiGen is child class of PixelMapGen that allows you to combine multiple PixelMapGens 
 * into a single PixelMapGen, with a single signal path through all the gens. There are four  
 * different constructors that you can use. The first two, MultiGen(int width, int height) 
 * and MultiGen(int width, int height, AffineTransformType type), create two DiagonalZigzagGens 
 * to fill whatever width and height you provide. Things get interesting with the two custom constructors, 
 * MultiGen(int width, int height, int rows, int columns, ArrayList<PixelMapGen> genList) 
 * and MultiGen(int width, int height, ArrayList<int[]> offsetList, ArrayList<PixelMapGen> genList).
 * 
 * Both custom constructors require an ArrayList of PixelMapGens. The dimensions 
 * of the gens should be tailored for the width and height of the application window, 
 * so that width == rows * genWidth and height == columns * genHeight. In the first 
 * custom constructor, the number of rows and columns is used to calculate the 
 * dimensions of the gens. It's easy to use.
 * 
 * The second custom constructor is the more flexible of the two. In the second
 * constructor, you can specify the location of the top left corner of each gen
 * in offsetList, an ArrayList of int[] arrays that specify x and y offsets for
 * each gen in the genList. With some planning of the spatial location and the
 * AffineTransformType applied to each gen in the genlist, you can often create
 * a continuous signal path through the final image. This application
 * demonstrates how to do that with HilbertGens. 
 * 
 * The PixelMapGen subclasses HilbertGen, DiagonalZigzagGen, and BoustropheGen
 * include various static methods to generate MultiGen objects. Check them out. 
 *  
 * Animation helps to visualize the orientation of PixelMapGen objects.
 * Press 'a' to toggle animation that shifts pixels along the signal path.
 * 
 * 
 * KEY COMMANDS
 * 
 * Press 'a' to toggle animation.
 * Press 'd' to show default MultiGen.
 * Press 't' to step through transformed MultiGen objects.
 * Press 'r' to show random mix of HilbertGen and DiagonalZigzagGen objects with a discontinuous signal path.
 * Press 'c' to show a MultiGen made of HilbertGen objects with a continuous signal path.
 * Press 'C' to show a MultiGen made with a mix of HilbertGen and DiagonalZigzagGen objects with a continuous path.
 * Press 'h' to show help message in Console.
 * 
 */

public class MultiGenDemo extends PApplet {
	PixelAudio pixelaudio;        // PixelAudio library instance
	HilbertGen hGen;              // a HilbertGen
	MultiGen multigen;            // a MultiGen, combining multiple PixelMapGen objects
	PixelAudioMapper mapper;      // your friendly PixelAudioMapper, with mapping and transcoding services
	PixelMapGen gen;              // variable for current PixelMapGen instance

	int transformIndex = 0;
	AffineTransformType[] fixedDimensionTransforms = {    // transforms that don't change width and height
			AffineTransformType.NADA, 
			AffineTransformType.FLIPX, 
			AffineTransformType.FLIPY, 
			AffineTransformType.R180 };
	
	// renaming shorthand copied from PixelMapGen, useful for creating MultiGen objects 
	// in the style of hilbertZigzagLoop6x4(...) -- see below
	public static AffineTransformType     r270      = AffineTransformType.R270;
	public static AffineTransformType     r90       = AffineTransformType.R90;
	public static AffineTransformType     r180      = AffineTransformType.R180;
	public static AffineTransformType     flipx     = AffineTransformType.FLIPX;
	public static AffineTransformType     fx270     = AffineTransformType.FX270;
	public static AffineTransformType     fx90      = AffineTransformType.FX90;
	public static AffineTransformType     flipy     = AffineTransformType.FLIPY;
	public static AffineTransformType     nada      = AffineTransformType.NADA;

	
	Random rand = new Random();
	
	PImage mapImage;              // a bitmap image for display
	PImage baseImage;             // a reference image, unchanging, used as source for animation
	int[] spectrum;               // an array of colors that will be written along the "signal path" in mapImage
	int shift = 256;              // amount we shift for each animation step
	int totalShift = 0;           // total amount we have shifted the image
	boolean isAnimating = false;  // toggle for animation
	
	int rows = 3;
	int columns = 2;
	int genWidth = 256;
	int genHeight = 256;


	public static void main(String[] args) {
		PApplet.main(new String[] { MultiGenDemo.class.getName() });
	}

	public void settings() {
		size(rows * genWidth, columns * genHeight);
	}

	public void setup() {
		pixelaudio = new PixelAudio(this);          // 1. initialize PixelAudio
		multigen = new MultiGen(width, height);     // 2. create a PixelMapGen object
		mapper = new PixelAudioMapper(multigen);    // 3. initialize a PixelAudioMapper object with the gen
		mapImage = createImage(width, height, RGB); // 4. create an image for display
		spectrum = getColors();                     // get colors for displaying on signal path
		refreshImages();                            // set up images for display and animation
		showHelp();                                 // some things to do with key commands
	}
	
	public MultiGen defaultMultiGen() {
		println("-- creating default MultiGen, width = "+ width +", height = "+ height);
		return new MultiGen(width, height);
	}
	
	public MultiGen transformMultiGen() {
		AffineTransformType t = this.nextTransform();
		println("-- creating transformed MultiGen, width = "+ width +", height = "+ height
				 +", transform = "+ t.name());
		return new MultiGen(width, height, t);
	}

	public MultiGen multiGenFromGenList() {
		ArrayList<PixelMapGen> genList = new ArrayList<>();
		int r = rows * 2;
		int c = columns * 2;
		int genW = width/r;
		int genH = height/c;
		if (genW != genH) {
			throw new IllegalArgumentException("--> genW must equal genH");
		}
		if (!PixelMapGen.isPowerOfTwo(genW)) {
			throw new IllegalArgumentException("--> genW must be a power of 2");
		}
		for (int j = 0; j < c; j++) {
			for (int i = 0; i < r; i++) {
				if (rand.nextBoolean()) 
					genList.add(new HilbertGen(genW, genH, PixelMapGen.randomTransform(rand)));
				else
					genList.add(new DiagonalZigzagGen(genW, genH, PixelMapGen.randomTransform(rand)));
			}
		}
		return new MultiGen(width, height, r, c, genList);
	}
	
	public MultiGen multiGenFromGenAndOffsetLists() {
		if (key == 'c') return hilbertLoop3x2(genWidth, genHeight);
		else return hilbertZigzagLoop6x4(genWidth/2, genHeight/2);
	}
	
	/**
	 * Generates a looping fractal signal path consisting of 6 HilbertGens,
	 * arranged 3 across and 2 down, to fit a 3 * genW by 2 * genH image. 
	 * Shows how to create custom a MultiGen with a genList and an offsetList.
	 * 
	 * This method creates a MultiGen instance from a list of PixelMapGen objects 
	 * (genList) and a list of  coordinate points (offsetList) where they will
	 * be displayed. The MultiGen class creates a single signal path over all
	 * its PixelMapGen objects. The path may be *continuous*, which is to say that
	 * the path through each PixelMapGen object ("gen" for short) only has to step
	 * one pixel up, down, left, or right to connect to the next gen. It may even
	 * create a *loop*, where the last pixel in the path is one step away from the
	 * first pixel. This method creates a MultiGen that is both continuous and looped. 
	 *
	 * This method may be called as factory method of the HilbertGen class:
	 * public static MultiGen hilbertLoop3x2(int genW, int genH)
	 * example: MultiGen multigen = HilbertGen.hilbertLoop3x2(genWidth, genHeight);
	 *
	 *
	 * Note that genH must equal genW and both must be powers of 2. For the 
	 * image size we're using in this example, genW = image width / 3 and 
	 * genH = image height / 2.
	 * 
	 * @param genW    width of each HilbertGen 
	 * @param genH    height of each HilbertGen
	 * @return        a MultiGen consisting of 6 HilbertGens linked together by one signal path
	 * 
	 */
	public MultiGen hilbertLoop3x2(int genW, int genH) {
	  // list of PixelMapGens that create an image using mapper
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
	 return new MultiGen(width, height, offsetList, genList);
	}
		
	/**
	 * Shows how to create a MultiGen object using arrays of locations, transforms, and cues.
	 * The cues are used to select a PixelMapGen subclass. 
	 * 
	 * This method creates a MultiGen consisting of a mix of zigzag and Hilbert curves
	 * in 6 columns and 4 rows arranged to provide a continuous loop. The resulting
	 * MultiGen will be 6 * genW wide by 4 * genH high, where genW == genH and genW 
	 * is a power of 2 (required for Hilbert gens). 
	 * 
	 * Copied from a static method in MultiGen.
	 * 
	 * @param genW    width of each gen, must be a power of 2 and equal to genW
	 * @param genH    height of each gen, must be a power of 2 and equal to genW
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
		return new MultiGen(6 * genW, 4 * genH, offsetList, genList);
	}

	
	public int[] getColors() {
		int[] colorWheel = new int[mapper.getSize()];
		pushStyle();
		colorMode(HSB, colorWheel.length, 100, 100);
		int h = 0;
		for (int i = 0; i < colorWheel.length; i++) {
			colorWheel[i] = color(h, 60, 75);
			h++;
		}
		popStyle();
		return colorWheel;
	}

	public void draw() {
		image(mapImage, 0, 0);
		if (isAnimating)
			animate();
	}

	public void keyPressed() {
		switch (key) {
		case 'a': // toggle animation
			isAnimating = !isAnimating;
			break;
		case 'd': // show default MultiGen
			multigen = defaultMultiGen();
			mapper.setGenerator(multigen);
			refreshImages();
			break;
		case 't': // step through transformed MultiGen objects
			multigen = transformMultiGen();
			mapper.setGenerator(multigen);
			refreshImages();
			break;
		case 'r': // show random mix of HilbertGen and DiagonalZigzagGen objects with a discontinuous signal path
			multigen = this.multiGenFromGenList();
			mapper.setGenerator(multigen);
			refreshImages();
			break;
		case 'c': // show a MultiGen made of HilbertGen objects with a continuous signal path
		case 'C': // show a MultiGen made with a mix of HilbertGen and DiagonalZigzagGen objects with a continuous path
			multigen = multiGenFromGenAndOffsetLists();
			mapper.setGenerator(multigen);
			refreshImages();
			break;
		case 'h': // show help message in Console
			break;
		default:
			break;
		}
	}
	
	public void showHelp() {
		println(" * Press 'a' to toggle animation.");
		println(" * Press 'd' to show default MultiGen.");
		println(" * Press 't' to step through transformed MultiGen objects.");
		println(" * Press 'r' to show random mix of HilbertGen and DiagonalZigzagGen objects with a discontinuous signal path.");
		println(" * Press 'c' to show a MultiGen made of HilbertGen objects with a continuous signal path.");
		println(" * Press 'C' to show a MultiGen made with a mix of HilbertGen and DiagonalZigzagGen objects with a continuous path.");
		println(" * Press 'h' to show help message in Console.");
	}

	public void animate() {
		totalShift += shift;
		mapImage.loadPixels();
		// PixelAudioMapper has the method we need
		mapper.copyPixelsAlongPathShifted(baseImage.pixels, mapImage.pixels, totalShift);
		mapImage.updatePixels();
	}
	
	public void refreshImages() {
		mapImage.loadPixels();
		mapper.plantPixels(spectrum, mapImage.pixels, 0, mapper.getSize());
		mapImage.updatePixels();
		baseImage = mapImage.copy();
		// to reset image to beginning state, set totalShift = 0
		// totalShift = 0;
	}
	
	public AffineTransformType nextTransform() {
		transformIndex = (transformIndex + 1) % fixedDimensionTransforms.length;
		return fixedDimensionTransforms[transformIndex];
	}

}
