package net.paulhertz.pixelaudio;

import java.util.ArrayList;


/**
 * @author Paul Hertz
 * 
 * ArgosyArray provides tools for shifting pixel patterns along the signal path
 * of an image. It is currently awaiting further development and some sample code.
 * One way to generate the pixel patterns is with the Lindenmeyer class. Demos 
 * will appear eventually, probably after the early beta release of the PixelAudio 
 * library at Experimental Sound Studio.  
 *
 */
public class ArgosyArray {
	/** Array of color values used for animation of PixelScannerINF instances */
	int[] bigArray;
	/** the number of pixels in an argosy unit */
	int unitSize;
	/** number of pixels in a shiftLeft animation Step */
	int animStep;
	/** colors for argosy units */
	int[] argosyColors = {PixelAudio.myParent.color(255, 255), PixelAudio.myParent.color(0, 255) };
	/** scaling for number of units in gap between argosies */
	float argosyGapScale = 55.0f;
	/** number of pixels in the gap between argosy patterns */
	int argosyGap;
	/** color of pixels in the gap */
	int argosyGapColor = PixelAudio.myParent.color(127, 255);
	/** how many times to repeat the pattern */
	int argosyReps = 0;
	/** margin on either side of the argosy patterns */
	int argosyMargin;
	/** current pattern to fill bigArray  */
	int[] argosyPattern;
	/** sum of values in argosy pattern */
	int argosySize;
	/** center the argosy patterns in the big array */
	boolean isCentered = true;
	/** array of number of pixels in each element of the expanded argosy pattern */
	int[] argosyIntervals;
	/** the number of pixels the array has shifted from its initial state */
	int argosyPixelShift = 0;
	/** do we count the shift or not?  */
	boolean isCountShift = true;
    /** count the number of unit shifts */
    int argosyShiftStep = 0;

  // an argosy pattern with 55 elements, 89 = (34 * 2 + 21) units long, derived from a Fibonacci L-system
	public static int[] argosy55 = new int[]{2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2,
                                           1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2,
                                           1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2};


	/**
	 * @param bigSize       size of the array
	 * @param unitSize      size of a unit of the argosy
	 * @param reps          number of repetitions of the argosy pattern, pass in 0 for maximum that fit
	 * @param isCentered    true if argosy array should be centered in bigArray
	 */
	public ArgosyArray(int bigSize, int unitSize, int reps, boolean isCentered) {
		this.bigArray = new int[bigSize];
		this.unitSize = unitSize;
		argosyPattern = new int[argosy55.length];
		for (int i = 0; i < argosy55.length; i++) {
			argosyPattern[i] = argosy55[i];
		}
		this.animStep = Math.round(this.unitSize / 16.0f);
		this.argosyGap = Math.round((this.argosyGapScale * this.unitSize));
		this.argosyReps = reps;
		this.isCentered = isCentered;
		this.bigArrayFill();
	}

	/**
	 * @param bigSize       size of the array
	 * @param unitSize      size of a unit of the argosy
	 * @param reps          number of repetitions of the argosy pattern, pass in 0 for maximum that fit
	 * @param isCentered    true if argosy array should be centered in bigArray
	 * @param colors        an array of colors for the argosy patterns
	 * @param gapColor      a color for the spaces between argosy patterns
	 * @param gapScale      scaling for number of units in gap between argosies
   * @param argosy        a pattern of numbers, will be copied to argosyPattern
	 */
	public ArgosyArray(int bigSize, int unitSize, int reps, boolean isCentered,
			               int[]colors, int gapColor, float gapScale, int[] argosy) {
		this.bigArray = new int[bigSize];
		this.unitSize = unitSize;
    this.argosyPattern = new int[argosy.length];
    for (int i = 0; i < argosy.length; i++) {
      this.argosyPattern[i] = argosy[i];
    }
		this.animStep = Math.round(this.unitSize / 16.0f);
		this.argosyGap = Math.round((this.argosyGapScale * this.unitSize));
		this.argosyReps = reps;
		this.isCentered = isCentered;
		this.argosyColors = colors;
		this.argosyGapColor = gapColor;
		this.argosyGapScale = gapScale;
		this.bigArrayFill();
	}

	/* --------------------------------------------------------------------------- */
	/*                                                                             */
	/*    Initialization: call bigArrayFill(() when you change other values that   */
	/*    affect the ordering of patterns and colors (just about everything).      */
	/*                                                                             */
	/* --------------------------------------------------------------------------- */


	/**
	 * Sets up the big array and fills it with colors using the argosy pattern.
	 */
	public void bigArrayFill() {
		int bigSize = this.bigArray.length;
		this.argosySize = 0;
		for (int element : argosyPattern) {
			argosySize += element;
		}
		int maxReps = Math.round(bigSize / (argosySize * unitSize + argosyGap));
		if (argosyReps > maxReps || argosyReps == 0) argosyReps = maxReps;
		if (isCentered) {
			// calculate how many repetitions of argosy + argosy gap fit into the array,
			// minding that there is one less gap than the number of argosies
			argosyMargin = bigSize - (argosyReps * (argosySize * unitSize + argosyGap) - argosyGap);
			// margin on either side, to center the argosies in the array
			// TODO review, revise argosyMargin calculations
			argosyMargin /= 2;
		}
		else argosyMargin = 0;
    argosyIntervals = new int[argosyPattern.length];
	  for (int i = 0; i < argosyPattern.length; i++) {
	    argosyIntervals[i] = argosyPattern[i] * unitSize;
	  }
	  this.argosyPixelShift = 0;
	  argosyFill();
	}

	/**
	 * Fill the big array with the argosy pattern, called by bigArrayFill.
	 */
	public void argosyFill() {
		int bigSize = this.bigArray.length;
		int reps = 0;
    int vi = 0;    // argosyIntervals index
    int ci = 0;    // argosyColors index
    int si = 0;    // bigArray index
    int i = 0;     // local index
    while (si < bigSize) {
      for (i = si; i < si + argosyIntervals[vi]; i++) {
        if (i >= bigSize) break;
        bigArray[i] = argosyColors[ci];
      }
      si = i;
      ci = (ci + 1) % argosyColors.length;
      vi = (vi + 1) % argosyIntervals.length;
      if (vi == 0) {
        reps++;
        for (i = si; i < si + argosyGap; i++) {
          if (i >= bigSize) break;
          bigArray[i] = argosyGapColor;
        }
        si = i;
      }
      if (reps == argosyReps) break;
    }
	}


	/* ----->>> ANIMATION <<<----- */

	/**
	 * Rotates bigArray left by d values. Uses efficient "Three Rotation" algorithm.
	 * @param d     number of elements to shift
	 */
	public void rotateLeft(int d) {
		int[] arr = this.bigArray;
	  if (d < 0) {
	    d = arr.length - (-d % arr.length);
	  }
	  else {
	    d = d % arr.length;
	  }
	  reverseArray(arr, 0, d - 1);
	  reverseArray(arr, d, arr.length - 1);
	  reverseArray(arr, 0, arr.length - 1);
	  if (isCountShift) argosyPixelShift += d;
	}


	/**
	 * Reverses an arbitrary subset of an array.
	 * @param arr   array to modify
	 * @param l     left bound of subset to reverse
	 * @param r     right bound of subset to reverse
	 */
	private void reverseArray(int[] arr, int l, int r) {
	  int temp;
	  while (l < r) {
	    temp = arr[l];
	    arr[l] = arr[r];
	    arr[r] = temp;
	    l++;
	    r--;
	  }
	}

	/**
	 * basic animation, rotate right by animStep pixels, decrement the step counter argosyShiftStep
	 */
	public void shiftRight() {
		rotateLeft(-this.animStep);
		argosyShiftStep--;
	}
	/**
	 * basic animation, rotate left by animStep pixels, inccrement the step counter argosyShiftStep
	 */
	public void shiftLeft() {
		rotateLeft(this.animStep);
		argosyShiftStep++;
	}



	/**
	 * @return the argosyPixelShift, save this if you want to reshift
	 */
	public int getArgosyPixelShift() {
		return argosyPixelShift;
	}

	/**
	 * Shifts left by a specified number of pixels, summing them to argosyPixelShift
	 * if isCounted is true.
	 * @param pixelShift
	 * @param isCounted
	 */
	public void shift(int pixelShift, boolean isCounted) {
		boolean oldCountShift = isCountShift;
		isCountShift = isCounted;
		rotateLeft(pixelShift);
		isCountShift = oldCountShift;
	}


	/* ----->>> GETTERS AND SETTERS <<<----- */
	/*
	 * These may have consequences, but implementation will have to wait.
	 * For the moment, we just set up the array and animate.
	 * Calling bigArryFill() should reset everything after a change.
	 */

	/**
	 * @return bigArray, but you really should get a copy if you want to do anything with it
	 */
	public int[] getBigArray() {
		return bigArray;
	}
	/**
	 * @return a copy of bigArray
	 */
	public int[] getBigArrayCopy() {
		int[] copy = new int[bigArray.length];
		for (int i = 0; i < bigArray.length; i++) {
			copy[i] = bigArray[i];
		}
		return copy;
	}

  /**
   * @param bigArray  the big array to set, must be same length as this.bigArray
   *                  use at your own risk, for example to load a very big image into the big array
   */
  public void setBigArray(int[] bigArray) {
    if (bigArray.length != this.bigArray.length) {
      System.out.println("----->>> ERROR : big arrays must both be the same size!");
      return;
    }
   for (int i = 0; i < bigArray.length; i++) {
     this.bigArray[i] = bigArray[i];
   }
  }

	/**
	 * @return the unitSize
	 */
	public int getUnitSize() {
		return unitSize;
	}
	/**
	 * Sets unitSize and triggers a call to bigArrayFill() to reset the pattern in bigArray.
	 * @param unitSize the new unitSize
	 */
	public void setUnitSize(int unitSize) {
		this.unitSize = unitSize;
		bigArrayFill();
	}


	/**
	 * @return the animStep, number of pixels to shift in an animation
	 */
	public int getAnimStep() {
		return animStep;
	}
	/**
	 * Set the animStep, with no side effects (but animation calls will use the new value)
	 * @param animStep
	 */
	public void setAnimStep(int animStep) {
		this.animStep = animStep;
	}


	/**
	 * @return the array of colors for the argosy pattern elements
	 */
	public int[] getArgosyColors() {
		return argosyColors;
	}
	/**
	 * Sets new argosyColors and triggers a call to bigArrayFill() to reset the pattern in bigArray.
	 * @param argosyColors
	 */
	public void setArgosyColors(int[] argosyColors) {
		this.argosyColors = argosyColors;
		bigArrayFill();
	}


	/**
	 * @return the argosyGapScale
	 */
	public float getArgosyGapScale() {
		return argosyGapScale;
	}
	/**
	 * Sets argosyGapScale and triggers a call to bigArrayFill() to reset the pattern in bigArray.
	 * @param argosyGapScale
	 */
	public void setArgosyGapScale(float argosyGapScale) {
		this.argosyGapScale = argosyGapScale;
		bigArrayFill();
	}


	/**
	 * @return the argosyGap, number of pixels between iterations of the argosy pattern
	 */
	public int getArgosyGap() {
		return argosyGap;
	}
	/**
	 * Sets argosyGap and triggers a call to bigArrayFill() to reset the pattern in bigArray.
	 * Usually it's better to set the argosyGapScale, but if you want a gap that isn't a
	 * multiple of unitSize, this is the way to do it.
	 * @param argosyGap
	 */
	public void setArgosyGap(int argosyGap) {
		this.argosyGap = argosyGap;
		bigArrayFill();
	}


	/**
	 * @return the argosyGapColor
	 */
	public int getArgosyGapColor() {
		return argosyGapColor;
	}
	/**
	 * Sets argosyGapColor and triggers a call to bigArrayFill() to reset the pattern in bigArray.
	 * @param argosyGapColor
	 */
	public void setArgosyGapColor(int argosyGapColor) {
		this.argosyGapColor = argosyGapColor;
		bigArrayFill();
	}


	/**
	 * @return argosyReps, the number of repetitions of the argosy pattern in the array,
	 * set by bigArrayFill().
	 */
	public int getArgosyReps() {
		return argosyReps;
	}


	/**
	 * @return argosyMargin, the left and right margin to argosy patterns in the array,
	 * set by bigArrayFill().
	 */
	public int getArgosyMargin() {
		return argosyMargin;
	}


	/**
	 * @return the argosyPattern, an array with a numeric pattern.
	 */
	public int[] getArgosyPattern() {
		return argosyPattern;
	}
	/**
	 * Sets a new argosy pattern and triggers a call to bigArrayFill() to reset the pattern in bigArray.
	 * @param argosyPattern
	 */
	public void setArgosyPattern(int[] argosyPattern) {
		this.argosyPattern = argosyPattern;
		bigArrayFill();
	}


	/**
	 * Sets new argosy colors and argosy gap color, triggers a call to bigArrayFill().
	 * @param argosyColors
	 * @param argosyGapColor
	 */
	public void setNewColors(int[] argosyColors, int argosyGapColor) {
		this.argosyColors = argosyColors;
		this.argosyGapColor = argosyGapColor;
		bigArrayFill();
	}


	/* --------------------------------------------------------------------------- */
	/*                                                                             */
	/*             Two utility methods for generating argosy patterns.             */
	/*                                                                             */
	/* --------------------------------------------------------------------------- */


	/**
	 * An L-System generator for Fibonacci trees represented as a sequence of 0s and 1s.
	 *
	 * @param depth     depth of iteration of the L-System. A depth of 8 gets you an ArrayList with 34 elements.
	 * @param verbose   Keep me informed. Or not.
	 * @return          an ArrayList of String values "1" and "0".
	 */
	public ArrayList<String> fibo(int depth, boolean verbose) {
		Lindenmayer lind = new Lindenmayer();
		lind.put("0", "1");
		lind.put("1", "01");
		ArrayList<String> buf = new ArrayList<>();
		ArrayList<String> seed = new ArrayList<>();
		seed.add("0");
		lind.expandString(seed, depth, buf);
		if (verbose) {
			System.out.println("Fibonacci L-system at depth "+ depth +"\n");
			for (String element : buf) {
				System.out.print(element);
			}
		}
		return buf;
	}

	/**
	 * Generates an argosy pattern based on a Fibonacci tree. Depth 8 gets you a 34 element
	 * sequence, and so Fibonacci forth. For example:
	 *     int[] testPattern = argosyGen(8, 5, 8, true);
	 *
	 * @param depth     depth of iteration of the L-System. A depth of 8 gets you an array with 34 elements.
	 * @param v1        value to substitute for a "0" in the ArrayList returned by fibo()
	 * @param v2        value to substitute for a "1" in the ArrayList return by fibo()
	 * @param verbose   if true, tells the console what's up
	 * @return          an array of ints determined by a Fibonacci tree generator and your inputs v1 and v2
	 */
	public int[] argosyGen(int depth, int v1, int v2, boolean verbose) {
		ArrayList<String> buf = fibo(depth, verbose);
		int[] argo = new int[buf.size()];
		for (int i = 0; i < argo.length; i++) {
			int gen = Integer.valueOf(buf.get(i));
			argo[i] = (gen == 0) ? v1 : v2;
		}
		if (verbose) {
			System.out.println("\n----- argosy pattern: ");
			int i;
			for (i = 0; i < argo.length - 1; i++) {
				System.out.print(argo[i] +", ");
			}
			System.out.println(argo[i]);
		}
		return argo;
	}




}
