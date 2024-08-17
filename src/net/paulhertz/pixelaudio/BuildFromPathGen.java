package net.paulhertz.pixelaudio;

import java.util.ArrayList;

/**
 * A PixelMapGen that loads data from an external pixelMap, such as might be saved in a JSON file.
 * When you instantiate a BuildFromPathGen, set the pixelMap field before you call generate().  
 * 
 * <pre>
 *	BuildFromPathGen myGen = new BuildFromPathGen(w, h);
 *	myGen.setPixelMap(newPixelMap);
 *	myGen.generate();
 * </pre>
 * 
 * The int[] array that you supply to setPixelMap must have length = w * h and contain the
 * ordinal numbers from 0..(w * h - 1) in any order. Its values are the index numbers in
 * a w * h bitmap in signal path order. BuildFromPathGen makes it easy to create arbitrary
 * signal paths that do not depend on numerical calculations. 
 * 
 * @example BuildFromPathGenDemo
 * 
 * 
 */
public class BuildFromPathGen extends PixelMapGen {
	public final static String description = "A PixelMapGen that loads data from an external pixelMap, such as might be saved in a JSON file.";

	public BuildFromPathGen(int width, int height, AffineTransformType type) {
		super(width, height, type);
	}

	public BuildFromPathGen(int width, int height) {
		super(width, height);
	}

	@Override
	public String describe() {
		return BuildFromPathGen.description;
	}

	@Override
	public boolean validate(int width, int height) {
		if (width < 1 || height < 1) {
			System.out.println("AssembleFromPathGen: width and height must be greater than 0.");
			return false;
		}
		return true;
	}

	@Override
	public int[] generate() {
		if (this.pixelMap == null && this.coords == null) {
			System.out.println("AssembleFromPathGen: You need to call setPixelMap(int[] newPixelMap) or "
					+ "setCoords(ArrayList<int[]> newCoords) before calling generate().");
			return null;
		}
		if (this.pixelMap != null && this.coords == null) {
			this.coords = new ArrayList<int[]>(pixelMap.length);
			for (int i = 0; i < pixelMap.length; i++) {
				int pos = pixelMap[i];
				int[] xy = new int[] { pos % this.w, pos / this.w };
				coords.add(xy);
			}
			if (this.transformType != AffineTransformType.NADA) transformCoords(coords, this.transformType);
		}
		this.loadIndexMaps();
		return this.pixelMap;
	}

	/**
	 * Sets the value of the pixelMap field. PixelMap corresponds to signalToImageLUT in a PixelAudioMapper, 
	 * an int[] array where the value at each index is the unique index of a pixel in a bitmap. 
	 * The array that you supply to setPixelMap must have length = w * h and contain the ordinal numbers 
	 * from 0..(w * h - 1) in any order. 
	 * 
	 * @param newPixelMap
	 */
	public void setPixelMap(int[] newPixelMap) {
		this.pixelMap = newPixelMap;
	}

}
