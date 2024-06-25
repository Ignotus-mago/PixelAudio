package net.paulhertz.scanner;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Implements a simple Lindenmeyer system (L-system),
 * a so-called DOL-system: deterministic and context-free.
 * Load production strings into transTable with put(), retrieve them with get().
 * Was used by HilbertScanner until I sped it up more optimized methods.
 */
public class Lindenmeyer extends Object {
  // our handy tables
  /** transition table for string production */
  private HashMap<String, String> transTable;
  public boolean verbose = false;


  /**
   * Creates a new Lindenmeyer instance;
   */
  public Lindenmeyer() {
    this.transTable = new HashMap<>();
  }

  /**
   * Gets a value from the transition table corresponding to the supplied key.
   * @param clef   a single-character String
   * @return      value corresponding to the key
   */
  public String get(String clef) {
    if (transTable.containsKey(clef))
      return transTable.get(clef);
    return clef;
  }

  /**
   * Loads a key and its corresponding value into the transition table.
   * @param clef     a single-character String
   * @param value   the String value associated with the key
   */
  public void put(String clef, String value) {
    transTable.put(clef, value);
  }

  public void expandString(ArrayList<String> tokens, int levels, ArrayList<String> sb) {
    //System.out.println("level is "+ levels);
    ArrayList<String> temp = new ArrayList<>();
    for (int i = 0; i < tokens.size(); i++) {
      String ch = tokens.get(i);
      String val = get(ch);
      for (int j = 0; j < val.length(); j++) {
        temp.add("" + val.charAt(j));
      }
    }
    if (levels > 0) {
      expandString(temp, levels - 1, sb);
    }
    else {
      for (int j = 0; j < tokens.size(); j++) {
        sb.add(tokens.get(j));
      }
    }
  }


   /**
    * Encode strings representing a Hilbert curve to supplied depth.
    * To draw the actual curve, ignore the R and L symbols
    *     + : 90 degrees CW
    *     - : 90 degrees CCW
    *     F : forward (n) units
    */
   public ArrayList<String> hilbert(int depth) {
     Lindenmeyer lind = new Lindenmeyer();
     lind.put("L", "+RF-LFL-FR+");
     lind.put("R", "-LF+RFR+FL-");
     ArrayList<String> buf = new ArrayList<>();
     ArrayList<String> seed = new ArrayList<>();
     seed.add("L");
     lind.expandString(seed, depth, buf);
     if (verbose) {
       System.out.println("Hilbert L-system at depth "+ depth +"\n");
       for (String element : buf) {
    	   System.out.print(element);
       }
     }
     return buf;
   }






   public int[] loadAnimSteps(AnimStepper stepper) {
	   for (AnimUnit au : stepper.getList()) {

	   }
	   return null;
	 }



/**
 * Animation frame-counting tool
 */
public class AnimStepper {
	   public ArrayList<AnimUnit> stepList;
	   AnimUnit currentUnit;  // the unit we are currently accessing
	   int u = 0;        // index of the unit in the stepList
	   int t = 0;        // index of the current count over 0..currentUnit.d

	   public AnimStepper() {
	     this.stepList = new ArrayList<>();
	   }

	   public AnimStepper(AnimUnit au) {
	     this();
	     this.addUnit(au);
	   }

	   public void addUnit(AnimUnit au) {
	     stepList.add(au);
	   }

	   public int totalSlide() {
	     int slide = 0;
	     for (AnimUnit element : stepList) {
	       slide += element.slide();
	     }
	     return slide;
	   }

	   public int totalSteps() {
		     int steps = 0;
		     for (AnimUnit element : stepList) {
		       steps += element.getCount();
		     }
		     return steps;
	   }

	   public int totalFrames() {
		     int frames = 0;
		     for (AnimUnit element : stepList) {
		       frames += element.frames() ;
		     }
		     return frames;
	   }

	   public int[] getStepArray() {
		   int[] stepArray = new int[this.totalSteps()];
		   for (AnimUnit au: this.stepList) {

		   }
		   return stepArray;
	   }

	   public void reset() {
	     this.u = 0;
	     this.t = 0;
	   }

	   public ArrayList<AnimUnit> getList() {
	     return this.stepList;
	   }
	 }


/**
 * Data storage for number of steps n each of duration d, with additional scaling factor s.
 *
 */
public class AnimUnit {
	   int d = 1;        // duration of each step
	   int n = 1;        // number of steps
	   float s = 1.0f;   // scaling factor, multiplies the unit size of pixel blocks.

	   public AnimUnit(int duration, int count) {
	     this.d = duration;
	     this.n = count;
	   }

	   public AnimUnit(int duration, int count, float stepSize) {
	     this.d = duration;
	     this.n = count;
	     this.s = stepSize;
	   }

	   public int getDuration() {
	     return d;
	   }
	   public void setDuration(int d) {
	     this.d = d;
	   }

	   public int getCount() {
	     return n;
	   }
	   public void setCount(int n) {
	     this.n = n;
	   }

	   public float getStepSize() {
	     return s;
	   }
	   public void setStepSize(float s) {
	     this.s = s;
	   }

	   public int frames() {
	     return this.d * this.n;
	   }

	   public int slide() {
	     return Math.round(this.s * this.frames());
	   }

	   public int[] toArray() {
		   int[] arr = new int[n];
		   for (int i = 0; i < n; i++) {
			   arr[i] = d;
		   }
		   return arr;
	   }
	 }


}