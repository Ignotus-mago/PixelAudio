package net.paulhertz.pixelaudio;

import java.util.ArrayList;

public class MooreGen extends PixelMapGen {
	public int depth;
	private boolean doXYSwap;

	public final static String description = "MooreGen generates a Moore curve over a square bitmap starting at (width/2 - 1, 0) and ending at (width/2, 0). "
			   + "Width and height must be equal powers of 2. You can also call MooreGen(int depth) and width and height will equal Math.pow(2, depth). ";


	public MooreGen(int width, int height) {
		super(width, height);
		this.depth = PixelMapGen.findPowerOfTwo(this.w);		// really handy to calculate depth before we generate the Moore curve
		this.doXYSwap = (this.depth % 2 == 1);					// a value to preserve symmetry and orientation when depth is odd
		// System.out.println("> MooreGen "+ width +", "+ height +", depth  = "+ depth + ", swap = "+ doXYSwap);
		this.generate();
	}

	public MooreGen(int depth) {
		this( (int) Math.round(Math.pow(2, depth)), (int) Math.round(Math.pow(2, depth)) );
	}


	@Override
	public String describe() {
		return MooreGen.description;
	}

	@Override
	public boolean validate(int width, int height) {
		if (width < 4) {
			System.out.println("MooreGen Error: 4 is the minimum value for width and height, 2 is the minimum value for depth.");
			return false;
		}
		if (width != height) {
			System.out.println("MooreGen Error: Width and height must be equal.");
			return false;
		}
		if (! PixelMapGen.isPowerOfTwo(width)) {
			System.out.println("MooreGen Error: Width and height must be equal to a power of 2.");
			return false;
		}
		return true;
	}

	@Override
	public int[] generate() {
		int p = 0;
		int i = 0;
		this.pixelMap = new int[this.h * this.w];
		this.sampleMap = new int[this.h * this.w];
		this.coords = this.generateCoordinates();
		for (int[] loc : this.coords) {
			p = loc[0] + loc[1] * w;
			this.pixelMap[i++] = p;
		}
		for (i = 0; i < w * h - 1; i++) {
			this.sampleMap[this.pixelMap[i]] = i;
		}
		return pixelMap;
	}
	
	
	private ArrayList<int[]> generateCoordinates() {
		return this.generateMooreCoordinates(this.getSize());
	}

	/**
	 * 
	 * @param n
	 * @return
	 */
	private ArrayList<int[]> generateMooreCoordinates(int n) {
	    ArrayList<int[]> mooreCoordinates = new ArrayList<>(n);
	    int hilbDepth;
	    if (this.depth > 1) {
	      hilbDepth = this.depth - 1;
	    }
	    else {
	      throw new IllegalArgumentException("Cannot generate Moore curve for depth < 2");
	    }
	    // rewrite the order of Hilbert curve coordinates to get the order for a Moore curve
	    HilbertGen hilb = new HilbertGen(hilbDepth);
	    int[] xcoords = new int[hilb.size];
	    int[] ycoords = new int[hilb.size];
	    int i = 0;
	    for (int[] xy : hilb.getCoordinates()) {
	      xcoords[i] = xy[1];
	      ycoords[i] = xy[0];
	      i++;
	    }
	    int m = hilb.getWidth() - 1;
	    // flip xcoords
	    for (i = 0; i < xcoords.length; i++) {
	      xcoords[i] = m - xcoords[i];
	    }
	    // now we fill mooreCoordinates with transformed copies of the Hilbert curve
	    for (i = 0; i < hilb.size; i++) {
	      mooreCoordinates.add(new int[]{xcoords[i], ycoords[i]});
	    }
	    for (i = 0; i < hilb.size; i++) {
	      mooreCoordinates.add(new int[]{xcoords[i], ycoords[i] + hilb.w});
	    }
	    // flip xcoords
	    for (i = 0; i < xcoords.length; i++) {
	      xcoords[i] = m - xcoords[i];
	    }
	    // flip ycoords
	    for (i = 0; i < ycoords.length; i++) {
	      ycoords[i] = m - ycoords[i];
	    }
	    for (i = 0; i < hilb.size; i++) {
	      mooreCoordinates.add(new int[]{xcoords[i] + hilb.w, ycoords[i] + hilb.w});
	    }
	    for (i = 0; i < hilb.size; i++) {
	      mooreCoordinates.add(new int[]{xcoords[i] + hilb.w, ycoords[i]});
	    }
	    return mooreCoordinates;
	}


}
