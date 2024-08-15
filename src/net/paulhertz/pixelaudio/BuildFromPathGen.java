package net.paulhertz.pixelaudio;

import java.util.ArrayList;

/**
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
		}
		this.loadIndexMaps();
		return this.pixelMap;
	}

	public void setPixelMap(int[] newPixelMap) {
		this.pixelMap = newPixelMap;
	}

}
