package net.paulhertz.pixelaudio;

import java.util.ArrayList;

public class MultiGen extends PixelMapGen {
	public int rows = 0;
	public int columns = 0;
	public ArrayList<PixelMapGen> genList;
	public ArrayList<int[]> offsetList = null;
	public final static String description = "A PixelMapGen that creates a single signal path over multiple PixelMapGens.";

	
	// we'll create two DiagonalZigzagGens by default.
	/**
	 * @param width
	 * @param height
	 * @param transform
	 */
	public MultiGen(int width, int height, AffineTransformType transform) {
		super(width, height, transform);
		genList = new ArrayList<PixelMapGen>();
		offsetList = new ArrayList<int[]>();
		int halfway = 0;
		boolean isTall = (width < height);
		if (isTall) {
			halfway = height / 2;
			genList.add(new DiagonalZigzagGen(width, halfway));
			offsetList.add(new int[]{0, 0});
			genList.add(new DiagonalZigzagGen(width, height - halfway));
			offsetList.add(new int[]{0, halfway});
		}
		else {
			halfway = width / 2;
			genList.add(new DiagonalZigzagGen(halfway, height));
			offsetList.add(new int[]{0, 0});
			genList.add(new DiagonalZigzagGen(width - halfway, height));
			offsetList.add(new int[]{halfway, 0});
		}
		this.generate();
	}

	
	public MultiGen(int width, int height) {
		this(width, height, AffineTransformType.NADA);
	}
	
	public MultiGen(int width, int height, int rows, int columns, ArrayList<PixelMapGen> genList) {
		super(width, height);
		this.rows = rows;
		this.columns = columns;
		this.genList = genList;
		this.generate();
	}

	public MultiGen(int width, int height, ArrayList<int[]> offsetList, ArrayList<PixelMapGen> genList) {
		super(width, height);
		this.genList = genList;
		this.offsetList = offsetList;
		this.generate();
	}

	@Override
	public String describe() {
		return MultiGen.description;
	}

	@Override
	public boolean validate(int width, int height) {
		if (width >= 4  && height >= 4) return true;
		System.out.println("Width and height of MultiGen must equal to or greater than 4.");
		return false;
	}

	/**
	 * Initialize this.coords, this.pixelMap, this.sampleMap.
	 * @return  this.pixelMap, the value for PixelAudioMapper.signalToImageLUT.
	 */
	@Override
	public int[] generate() {
		this.coords = this.generateCoordinates();
		return this.setMapsFromCoords(this.coords);
	}

	/**
	 * Generically-named method that calls the custom coordinate generation method (here, generateMultiCoordinates).
	 * Consider putting additional initializations here, if required by your coordinate generation method,
	 * rather than in the generate() method, which will then only handle coords initialization and the
	 * built-in pixelMap and sampleMap initializations.
	 *
	 * @return 	An ArrayList<int[]> of bitmap coordinates in the order the signal mapping would visit them.
	 *
	 */
	private ArrayList<int[]> generateCoordinates() {
		return this.generateMultiCoordinates(this.w, this.h);
	}

	/**
	 * The coordinate generation method for this class. Both lookup tables are derived from the coordinate list created
	 * by this method.
	 *
	 * @param   width		width of the 2D bitmap pixel array
	 * @param   height		height of the 2D bitmap pixel array
	 * @return 				an array of coordinate pairs
	 */
	private ArrayList<int[]> generateMultiCoordinates(int width, int height) {
		ArrayList<int[]> coordinates = new ArrayList<int[]>(width * height);
		if (offsetList != null) {
			int i = 0;
			for (int[] xy : offsetList) {
				int tx = xy[0];
				int ty = xy[1];
				ArrayList<int[]> genCoords = this.genList.get(i++).getCoordinates();
				genCoords = translateCoords(genCoords, tx, ty);
				coordinates.addAll(genCoords);				
			}
		} 
		else {
			int tx = width / this.rows;
			int ty = height / this.columns;
			int i = 0;
			for (int c = 0; c < this.columns; c++) {
				for (int r = 0; r < this.rows; r++) {
					ArrayList<int[]> genCoords = this.genList.get(i++).getCoordinates();
					genCoords = translateCoords(genCoords, r * tx, c * ty);
					coordinates.addAll(genCoords);
				}
			}
		}
		return coordinates;
	}

	public ArrayList<int[]> translateCoords(ArrayList<int[]> coordList, int tx, int ty ) {
		ArrayList<int[]> newList = new ArrayList<int[]>(coordList.size());
		for (int[] xy : coordList) {
			newList.add(new int[] {xy[0] + tx, xy[1] + ty});
		}		
		return newList;
	}
	
	
	

	/* ------------------------------ GETTERS AND NO SETTERS ------------------------------ */
	/*                                                                                      */
	/*                  See abstract class PixMapGen for additional methods                 */
	/*                                                                                      */
	/* These include: getWidth(), getHeight(), getSize(), getPixelMap(), getPixelMapCopy(), */
	/* getSampleMap(), get SampleMapCopy(), getCoordinates(), getCoordinatesCopy().         */
	/*                                                                                      */
	/* ------------------------------------------------------------------------------------ */
	

	public int getRows() {
		return rows;
	}


	public int getColumns() {
		return columns;
	}


	public ArrayList<PixelMapGen> getGenList() {
		return genList;
	}


	public ArrayList<int[]> getOffsetList() {
		return offsetList;
	}
	
	
	/**
	 * This method creates a MultiGen consisting of a mix of zigzag and Hilbert curves
	 * in 6 columns and 4 rows arranged to provide a continuous loop.
	 * 
	 * @param genW
	 * @param genH
	 * @return
	 */
	public static MultiGen hilbertZigzagLoop6x4(int genW, int genH) {
	    // list of PixelMapGens that create a path through an image using PixelAudioMapper
		ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
		// list of x,y coordinates for placing gens from genList
		ArrayList<int[]> offsetList = new ArrayList<int[]>(); 		
		int[][] locs = {{0,0}, {0,1}, {0,2}, {0,3}, {1,3}, {1,2}, {2,2}, {2,3}, 
						{3,3}, {3,2}, {4,2}, {4,3}, {5,3}, {5,2}, {5,1}, {5,0},
						{4,0}, {4,1}, {3,1}, {3,0}, {2,0}, {2,1}, {1,1}, {1,0}};
		AffineTransformType[] trans = {r90cw, r90cw, nada, r90cw, r90ccw, fxr90cw, nada, r90cw, 
				                       r90ccw, r90ccw, fxr90ccw, nada, r90ccw, r90ccw, r180, r90ccw, 
				                       r90cw, fxr90ccw, r180, r90ccw, r90cw, r90cw, fxr90cw, r180};
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


}
