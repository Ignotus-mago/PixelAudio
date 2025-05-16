package net.paulhertz.pixelaudio;

import java.util.ArrayList;

/**
 * BoustropheGen generates a signal path that starts at (0,0) and reads left-to-right to (width-1, 0). 
 * The path steps down to (width-1, 1) and reads right-to-left back to (0, 1). The path alternates 
 * reading direction until it reaches the last pixel at (width-1, height-1) or (0, height-1). The name
 * is derived from the Greek "boustrophedon," a system of writing that makes an "ox turn" from one line
 * to the next. The boustrophedon signal path has better spatial coherence than the row major scanning
 * in general use for bitmaps. 
 * 
 * BoustropheGen is also a good example of how easy it can be to implement a PixelMapGen child class. 
 * 
 */
public class BoustropheGen extends PixelMapGen {
	public final static String description = "BoustropheGen starts at (0,0) and reads left-to-right on even numbered pixel scanlines and right-to-left on odd numbered scanlines.";


	public BoustropheGen(int width, int height) {
		super(width, height);
		this.generate();
	}

	public BoustropheGen(int width, int height, AffineTransformType type) {
		super(width, height, type);
		this.generate();
	}

	@Override
	public String describe() {
		return BoustropheGen.description;
	}

	@Override
	public boolean validate(int width, int height) {
		if (width < 2 || height < 2) {
			System.out.println("Width and height for BoustropheGen must be greater than 1.");
			return false;
		}
		return true;
	}

	/**
	 * Initializes this.coords, this.pixelMap, this.sampleMap: this is handled by 
	 * a call to PixelMapGen's setMapsFromCoords() method. 
	 * @return  this.pixelMap, the value for PixelAudioMapper.signalToImageLUT.
	 */
	@Override
	public int[] generate() {
		this.coords = this.generateCoordinates();
		return this.setMapsFromCoords(this.coords);
	}

	/**
	 * Generically-named method that calls the custom coordinate generation method for a particular 
	 * PixelMapGen child class. Here the method is generateBouCoordinates().
	 * Additional initializations belong here, if required by your coordinate generation method,
	 * rather than in the generate() method.
	 *
	 * @return 	An ArrayList<int[]> of bitmap coordinates in the order the signal mapping would visit them.
	 *
	 */
	private ArrayList<int[]> generateCoordinates() {
		return this.generateBouCoordinates(this.w, this.h);
	}
	
	/**
	 * The specific coordinate generation method for this class. 
	 *
	 * @param   width		width of the 2D bitmap pixel array
	 * @param   height		height of the 2D bitmap pixel array
	 * @return 				an array of coordinate pairs
	 */
	private ArrayList<int[]> generateBouCoordinates(int width, int height) {
		ArrayList<int[]> coordinates = new ArrayList<int[]>(width * height);
		for (int y = 0; y < height; y++) {
			if (y % 2 == 0) {
				for (int x = 0; x < width; x++) {
					coordinates.add(new int[] {x, y});
				}
			}
			else {
				for (int x = width - 1; x >= 0; x--) {
					coordinates.add(new int[] {x, y});
				}
			}
		}
		return coordinates;
	}

}
