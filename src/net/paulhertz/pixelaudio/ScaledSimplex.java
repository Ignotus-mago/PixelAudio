package net.paulhertz.pixelaudio;

/**
 * A wrapper for OpenSimplex2, adapted from Christian Maher's code at 
 * https://cmaher.github.io/posts/working-with-simplex-noise/
 */
public class ScaledSimplex {
	/* 
	 * if you make OpenSimplex2 a class in a Processing sketch, a .pde instead
	 * of a .java class, you can't use static methods. If you rewrite static
	 * methods as class methods, you can use generator to access OpenSimple2.pde
	 * instead of calling static methods of OpenSimplex2.
	 * In a .java class it doesn't get used, beyond instantiation. 
	 */
	private OpenSimplex2 generator;
	int octaves = 4;
	float detail = 0.5f;
	float inc = 0.00625f;
	float noiz = 0;
	int seed;

	public ScaledSimplex() {
		this((int) System.currentTimeMillis());
	}

	public ScaledSimplex(int seed) {
		generator = new OpenSimplex2();
		this.seed = seed;
	}

	public ScaledSimplex(int octaves, float detail) {
		this((int) System.currentTimeMillis());
		this.octaves = octaves;
		this.detail = detail;
	}

	public ScaledSimplex(int octaves, float detail, int seed) {
		this(seed);
		this.octaves = octaves;
		this.detail = detail;
	}

	public ScaledSimplex(int octaves, float detail, float inc) {
		this((int) System.currentTimeMillis());
		this.octaves = octaves;
		this.detail = detail;
		this.inc = inc;
	}

	public ScaledSimplex(int octaves, float detail, float inc, int seed) {
		this(seed);
		this.octaves = octaves;
		this.detail = detail;
		this.inc = inc;
	}

	public int getSeed() {
		return this.seed;
	}

	public void setSeed(int newSeed) {
		this.seed = newSeed;
	}

	public int getOctaves() {
		return octaves;
	}

	public void setOctaves(int octaves) {
		this.octaves = octaves;
	}

	public float getDetail() {
		return detail;
	}

	public void setDetail(float detail) {
		this.detail = detail;
	}

	public float getInc() {
		return inc;
	}

	public void setInc(float inc) {
		this.inc = inc;
	}

	public float getNoiz() {
		return noiz;
	}

	public void setNoiz(float noiz) {
		this.noiz = noiz;
	}
	
	// ------------- Noise Methods ------------- //

	// See OpenSimplex2.noise2(long seed, double x, double y)
	// Adapted from Christian Maher's code at https://cmaher.github.io/posts/working-with-simplex-noise/
	// note that if we make OpenSimplex2 a Processing class, we'll need to access it through this.generator
	/**
	 * @param iter    number of iterations of scaled noise (same as "octaves")
	 * @param x       x-coordinate
	 * @param y       y-coordinate
	 * @param det     amount of detail retained from each scaled 
	 * 				  iteration, aka "persistence"
	 * @param scale   the effective distance between coordinate points, 
	 * 				  referred to as "inc" in our code
	 * @param low     lower bound, typically for a pixel value
	 * @param high    upper bound, typically for a pixel value
	 * @return		  the noise value at (x, y, z), the value of "noiz"
	 */
	public float noise2(int iter, float x, float y, float det, float scale, float low, float high) {
		float maxAmp = 0;
		float amp = 1;
		float freq = scale;
		noiz = 0;
		// add successively smaller, higher-frequency terms
		for (int i = 0; i < iter; ++i) {
			noiz += OpenSimplex2.noise2(seed, x * freq, y * freq) * amp;
			maxAmp += amp;
			amp *= det;
			freq *= 2;
		}
		// take the average over the iterations
		noiz /= maxAmp;
		// map to the desired range
		noiz = noiz * (high - low) / 2 + (high + low) / 2;
		return noiz;
	}
	
	public float noise1(float x) {
		return noise2(octaves, x, 0, detail, inc, -1, 1);
	}

	// returns a value between -1 and 1
	public float noise2(float x, float y) {
		return noise2(octaves, x, y, detail, inc, -1, 1);
	}

	// returns a value mapped to the interval [low..high]
	public float noise2(float x, float y, float low, float high) {
		return noise2(octaves, x, y, detail, inc, low, high);
	}

	// See OpenSimplex2.noise3_ImproveXY(long seed, double x, double y, double z)
	// Adapted from Christian Maher's code at https://cmaher.github.io/posts/working-with-simplex-noise/
	// note that if we make OpenSimplex2 a Processing class, we'll need to access it through this.generator
	/**
	 * @param iter  	number of iterations of scaled noise (same as "octaves")
	 * @param x     	x-coordinate
	 * @param y     	y-coordinate
	 * @param z     	z-coordinate, incrementing z drives the animation over a 2D
	 *              	bitmap in x and y
	 * @param det   	amount of detail retained from each scaled iteration, 
	 *              	aka "persistence"
	 * @param scale 	the effective distance between coordinate points, referred
	 *              	to as "inc" in our code
	 * @param low   	lower bound, typically for a pixel value
	 * @param high  	upper bound, typically for a pixel value
	 * @return 			the noise value at (x, y, z), the value of "noiz"
	 */
	public float noise3(int iter, float x, float y, float z, float det, float scale, float low, float high) {
		float maxAmp = 0;
		float amp = 1;
		float freq = scale;
		noiz = 0;
		// add successively smaller, higher-frequency terms
		for (int i = 0; i < iter; ++i) {
			noiz += OpenSimplex2.noise3_ImproveXY(seed, x * freq, y * freq, z * freq) * amp;
			maxAmp += amp;
			amp *= det;
			freq *= 2;
		}
		// take the average over the iterations
		noiz /= maxAmp;
		// map to the desired range
		noiz = noiz * (high - low) / 2 + (high + low) / 2;
		return noiz;
	}

	// returns a value between -1 and 1
	public float noise3(float x, float y, float z) {
		return noise3(octaves, x, y, z, detail, inc, -1, 1);
	}

	// returns a value mapped to the interval [low..high]
	public float noise3(float x, float y, float z, float low, float high) {
		return noise3(octaves, x, y, z, detail, inc, low, high);
	}

}