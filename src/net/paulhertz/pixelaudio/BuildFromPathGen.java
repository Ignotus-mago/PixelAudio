/*
 *  Copyright (c) 2024 - 2025 by Paul Hertz <ignotus@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.paulhertz.pixelaudio;

import java.util.ArrayList;

/**
 * A PixelMapGen that loads data from an external pixelMap, such as might be saved in a JSON file.
 * When you instantiate a BuildFromPathGen, set the pixelMap field before you call generate().  
 * PixelMap corresponds to signalToImageLUT in a PixelAudioMapper. See the example sketch 
 * BuildFromPathGenDemo, which shows how to save and load JSON-format data to instantiate 
 * a BuildFromPathGen object. 
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
 * See Processing example sketch BuildFromPathGenDemo.
 * 
 * 
 */
public class BuildFromPathGen extends PixelMapGen {
	public final static String description = "A PixelMapGen that loads data from an external pixelMap, such as might be saved in a JSON file."
			                                 +"\nWhen you instantiate a BuildFromPathGen, set the pixelMap field before you call generate().";

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
