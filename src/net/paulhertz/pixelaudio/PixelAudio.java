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




}

