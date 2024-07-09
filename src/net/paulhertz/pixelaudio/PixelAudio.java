package net.paulhertz.pixelaudio;


import processing.core.PApplet;

/**
 *
 *
 * (the tag example followed by the name of an example included in folder 'examples' will
 * automatically include the example in the javadoc.)
 *
 * @example Hello
 */

public class PixelAudio {
	// myParent is a reference to the parent sketch
	// we make it static so it's available to other classes
	static PApplet myParent;

	int myVariable = 0;

	public final static String VERSION = "##library.prettyVersion##";


	/**
	 * a Constructor, usually called in the setup() method in your sketch to
	 * initialize and start the Library.
	 *
	 * @example Hello
	 * @param theParent the parent PApplet
	 */
	public PixelAudio(PApplet theParent) {
		myParent = theParent;
		welcome();
	}


	private void welcome() {
		System.out.println("##library.name## ##library.prettyVersion## by ##author##");
	}


	public String sayHello() {
		return "Hello from PixelAudio.";
	}

	/**
	 * return the version of the Library.
	 *
	 * @return String
	 */
	public static String version() {
		return VERSION;
	}

	/**
	 *
	 * @param theA the width of test
	 * @param theB the height of test
	 */
	public void setVariable(int theA, int theB) {
		myVariable = theA + theB;
	}

	/**
	 *
	 * @return int
	 */
	public int getVariable() {
		return myVariable;
	}


	//------------- LERP, MAP, CONSTRAIN -------------//


	// lerp and map

	/**
	 * Processing's PApplet.constrain method, copied for convenience.
	 * @param 	amt
	 * @param 	low
	 * @param 	high
	 * @return 	amt clipped to low and high, closed interval
	 */
	static public final float constrain(float amt, float low, float high) {
		return (amt < low) ? low : ((amt > high) ? high : amt);
	}


	/**
	 * Processing's PApplet.constrain method, copied for convenience.
	 * @param 	amt
	 * @param 	low
	 * @param 	high
	 * @return 	amt clipped to low and high, closed interval
	 */
	static public final int constrain(int amt, int low, int high) {
		return (amt < low) ? low : ((amt > high) ? high : amt);
	}


	/**
	 * Processing's map method, but with no error checking
	 * @param value
	 * @param start1
	 * @param stop1
	 * @param start2
	 * @param stop2
	 * @return
	 */
	static public final float map(float value, float start1, float stop1, float start2, float stop2) {
		return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
	}


	/**
	 * Good old lerp.
	 * @param a		first bound, typically a minimum value
	 * @param b		second bound, typically a maximum value
	 * @param f		scaling value, from 0..1 to interpolate between a and b, but can go over or under
	 * @return		a value between a and b, scaled by f (if 0 <= f >= 1).
	 */
	static public final float lerp(float a, float b, float f) {
	    return a + f * (b - a);
	}




}

