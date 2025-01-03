package net.paulhertz.pixelaudio;

import java.util.ArrayList;

public class MultiGen extends PixelMapGen {
	public int rows = 0;
	public int columns = 0;
	public ArrayList<PixelMapGen> genList;
	public ArrayList<int[]> offsetList = null;
	public final static String description = "A PixelMapGen that creates a continuous signal over multiple PixelMapGens.";

	
	// we'll create two DiagonalZigzagGens by default.
	public MultiGen(int width, int height, AffineTransformType type) {
		super(width, height, type);
		genList = new ArrayList<PixelMapGen>();
		offsetList = new ArrayList<int[]>();
		int halfway = 0;
		boolean tall = (width < height);
		if (tall) {
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
	 * Generically-named method that calls the custom coordinate generation method (here, generateZigzagDiagonalCoordinates).
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

	/* ------------------------------ HILBERT MULTIGEN BUILDER ------------------------------ */
	
	public static MultiGen buildHilbertMultigen(int genEdge, int rows, int cols) {
		/*
		 * rows even, cols even
		 * rows odd, cols even
		 * rows even, cols odd
		 * rows odd, cols odd
		 * 
		 * 
		 */
		return null;
	}


}
