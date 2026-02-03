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

package net.paulhertz.pixelaudio.curves;

import java.util.ArrayList;
import java.util.List;

import processing.core.PVector;
import processing.core.PApplet;
import processing.core.PGraphics;

import net.paulhertz.pixelaudio.schedule.GestureSchedule;


// This class was designed for lazy initialization, but it has a method for eager intitialization, calculateDerivedPoints(). 
// I would advise anyone using PixelAudio for live performance to initialize all structures up front and then avoid altering them,
// but this class is designed to let you alter settings on the fly, if you want to. Just be *really* careful when it comes
// to altering dragPoints or dragTimes. They must follow the rules set out in PAGesture, repeated below.
//
// TODO = DONE: provide multiple ways that eventPoints and brushPoly can work in PACurveMaker and in apps that use PACurveMaker instances:
// sometimes we'll want dense eventPoints, but leave brushPoly low-res. We'll consider how to vary events over playback.
// we can also consider how the time interval of a PACurveMaker instance could affect its playback,
// DONE partly with classes PAGestureParametric and PAIndexParametric, currently with no sample code. 
// The GesturePlayground demo applet uses GestureScheduleBuilder to modify the number of samples and 
// the duration of gestures modeled by PACurveMaker.
//
// TODO JSON I/O: essential variables are dragPoints, epsilon, dragTimes. It's useful to save bezierBias and eventSteps. 
// One might also save curve data, but that is proper to PABezShape. When planting audio, the data used for the plant is critical. 
// That would seem to include the curve, reconstructed, and the curve points. Finally, one might consider the drawing properties. 


/**
 * <p>PACurveMaker is a utility and storage class for gestures and curve modeling, with
 * point reduction and drawing to on-screen PApplets or off-screen PGraphics. It implements
 * PAGesture and provides a pair of data structures, <code>dragPoints</code> and <code>dragTimes</code>, 
 * that together capture gestures made with a mouse or similar input device.
 * Each point in dragPoints is associated with a corresponding time in dragTimes. 
 * You can use PACurveMaker for data storage in interactive drawing applications, or 
 * implement your own storage with just the features you require. It is a key
 * data structure for playing digital audio instruments using granular synthesis and sampling 
 * in the net.paulhertz.pixelaudio.granular and net.paulhertz.pixelaudio.voices packages. 
 * </p><p>
 * PACUrveMaker makes extensive use of PACurveUtility's static methods and provides
 * a wide range of graphic objects and properties for dense point arrays, 
 * reduced point arrays, derived Bezier curves (weighted and not weighted), 
 * brushstroke Bezier shapes for drawing to the display, and brushstroke polygons
 * for point testing with PABezShape.pointInPoly(). It provides some basic timing
 * data associated with drawing to the screen. You can use PACurveUtility 
 * for drawing all PACurveMaker data formats or you can use the built-in
 * commands if you use PACurveMaker as a storage class.
 * </p><p>
 * PACurveMaker's factory methods take as their principal argument a list of PVectors, dragPoints,
 * a list of time offsets in milliseconds, dragTimes, and an absolute time in milliseconds, startTimeMs.
 * The list of points is typically derived from a line drawn by dragging the mouse across the application
 * window. This line is stored in <code>dragPoints</code> and can be accessed with getDragPoints() or getAllPoints().
 * dragPoints can be mutated with mutateAllPoints, preserving its cardinality.
 * </p><p>
 * The dense point set in dragPoints can be processed by the Ramer-Douglas-Peucker algorithm to derive a 
 * a reduced point set, rdpPoints. The algorithm is implemented in PACUrveUtilities. The reduced points 
 * can be turned into a Bezier path, curveShape, where each point in rdpPoints becomes an anchor point
 * in curveShape. Bezier paths are modeled using PixelAudio's PABezShape class, which includes methods
 * for drawing attributes, affine transforms, and point testing. For display and interaction, a brushstroke 
 * shape, <code>brushShape</code>, can be created from the curve. An array of PVectors, 
 * <code>eventPoints</code>, can be created from curveShape to facilitate timed event-staging along the curve. 
 * Another array of Pvectors, <code>brushPoly</code>, can be used to test points with <code>PABezShape.pointInPoly()</code>.
 * <br><br>
 * ---- TODO continue to revise documentation ----
 * 
 * 
 * </p><p>
 * Each of the geometric data objects--dragPoints, rdpPoints, curveShape, and brushShape--has 
 * method for drawing to the screen in a PApplet or drawing offscreen in a PGraphic. DragPoints and 
 * rdpPoints keep their drawing parameters (fill, stroke, weight) in PACurveMaker variables. PABezShapes
 * like curveShape and brushShape can store drawing parameters internally. 
 * </p><p>
 * There a several properties of PACurveMaker that can affect the appearance of curves. The value of <code>epsilon</code>
 * determines how closely the reduced point set in <code>rdpPoints</code> matches the dense point set in <code>dragPoints</code>.
 * Smaller values, down to 1.0, yield a reduced point set with more points and great precision. Some of the factory methods
 * allow you to supply your own value for epsilon, which defaults to 8.0. Weighted curves and brushstrokes are variations 
 * on the smooth Bezier curves that PACurveMaker generates by default. In the default curves, the rate of change in 
 * curvature is the same on either side of a Bezier anchor point. The weighted curves use a value, bezierBias, to 
 * change the adjust the control points. The default bias value, PABezShape.LAMBDA, may provide a closer approximation
 * to rdpPoints. Other values can create extreme curves. It's worth some experimentation. If you call setDrawWeighted(true)
 * and then call calculateDerivedPoints(), curveShape and brushShape will be generated as weighted Bezier curves. 
 * There are several calls to generate weighted Bezier curves. They do not set local variables, but can be useful
 * for exploring the variations created by different values of bezierBias. PACurveUtility can also create 
 * weighted curves. 
 * </p>
 * 	
 * NOTES 2025.12.14
 * 
 * <code>eventPoints</code> is a <code>List<PVector></code> derived from the gestural points captured during user interaction.
 * <code>dragPoints</code> is reduced to <code>rdpPoints</code>. <code>rdpPoints</code> is used to derive the 
 * Bezier path (PABezShape) <code>curveShape</code>. The polyline representation of curveShape is <code>eventPoints</code>, 
 * where display events tied to the curve can be located. 
 * To reduce confusion, I have created some alias methods in PACurveMaker:
 * 
 *    <pre>
 * 	  // Alias method for getDragPoints().
 * 	  public ArrayList<PVector> getAllPoints() {
 * 	  	return this.getDragPoints();
 * 	  }
 *  
 * 	  // Alias for getRdpPoints(). 
 * 	  public ArrayList<PVector> getReducedPoints() {
 * 	  	return getRdpPoints();
 * 	  }
 *  
 * 	  // Sets epsilon and returns the reduced point set, this.rdpPoints
 * 	  public ArrayList<PVector> getReducedPoints(float epsilon) {
 * 	  	if (this.epsilon != epsilon || rdpPoints.size() < 1) {
 * 	  		this.setEpsilon(epsilon);
 * 	  		calculateDerivedPoints();
 * 	  	}
 * 	  	return rdpPoints;
 * 	  }
 *  
 * 	  // Alias for getCurveShape().
 * 	  public PABezShape getCurve() {
 * 	  	return this.getCurveShape();
 * 	  }
 * 	  
 * 	  // Alias for getEventPoints().
 *    public ArrayList<PVector> getCurvePoints() {
 * 	  	return this.getEventPoints();
 * 	  }
 *  
 * 	  // Alias for getEventPoints(int eventSteps).
 * 	  public ArrayList<PVector> getCurvePoints(int curveSteps) {
 * 	  	return this.getEventPoints(curveSteps);
 * 	  }
 * 
 * 	  // Alias for eventSteps 
 *    public int getCurveSteps() {
 *      return this.eventSteps;
 *    }
 *    </pre>
 * 
 * In addition, I note that the primary point sets in PACUrveMaker are <code>dragPoints</code> (getAllPoints()), 
 * <code>rdpPoints</code> (getCurvePoints()), and <code>eventPoints</code> (getCurvePoints()). The full 
 * gesture is conveyed by <code>dragPoints</code> and <code>dragTimes</code>. The indices of <code>rdpPoints</code> are tracked 
 * and stored in <code>ArrayList<Integer> rdpIndices</code>, so that we can map back to the original gesture points 
 * if needed. The points in <code>rdpPoints</code> are also the anchor points in the Bezier path <code>curveShape</code>,
 * produced from <code>curveShape = PACurveUtility.calculateCurve(rdpPoints);</code>.
 * 
 * 
 * We can also get the reduced time list:
 * 
 *    <pre>
 *    public int[] getReducedTimes() {
 * 	  	int[] timeIndices = getRdpIndicesAsInts();
 * 	  	int[] rdpTimes = new int[timeIndices.length];
 * 	  	// the first value in dragTimes is a time stamp, all following numbers are offsets
 * 	  	// one for each point in dragPoints. We only need the offsets. 
 * 	  	int[] allTimes = this.getDragOffsetsAsInts();
 * 	  	for (int i = 0; i < timeIndices.length; i++) {
 * 	  		rdpTimes[i] = allTimes[timeIndices[i]];
 * 	  	}
 * 	  	return rdpTimes;
 * 	  }
 *    </pre> 
 * 
 * Changing epsilon will change the reduced point set, and thus the Bezier curve and event points. <code>rdpIndices</code> must 
 * be recalculated whenever epsilon is changed. This is done in <code>PACurveMaker.calculateDerivedPoints()</code>, which 
 * is called from <code>setEpsilon()</code> and <code>public ArrayList<PVector> getReducedPoints(float epsilon)</code> The new reduced 
 * point time set can be derived from <code>rdpIndices</code> when needed. 
 * 
 * NOTES 2025.12.23
 * 
 * As much as possible, PACurveMaker now relies on lazy initialization, though end user can initialize all geometry up from with 
 * calculateDerivedPoints(). I have also introduced the interface PAGesture, to clarify how PACurveMaker structures originate
 * in capture gestures from a GUI or other source. A gesture is constructed from 
 * 
 *   1) a list of points and 
 *   2) a list of time offsets where
 *   3) both lists have the same cardinality and 
 *   4) time offsets increase monotonically. In addition, 
 *   5) the time list is expected (but not required) to start with a first element 0.  
 *   
 * Absolute time of gesture creation can be stored in startTime, as millis from application start.
 * 
 * I have also refactored the structure of dragTimes. It no longer has an absolute time stamp as its first element, 
 * thus adhering the the cardinality rule stated above. 
 * GestureSchedule provides some static methods to help enforce the invariants of gestures. 
 * 
 * 
 * 
 */
public class PACurveMaker implements PAGesture {
	/* ---------------------------------------------------------- */
	/*          Data structures and rendering variables           */
	/* ---------------------------------------------------------- */

	/** List of non-repeating points that need to be reduced, typically from a UI gesture 
	 * such as dragging a mouse, optical flow of a video image, flocks of boids, etc. */
	private ArrayList<PVector> dragPoints;
	/** The reduced points returned by the RDP algorithm */
	private ArrayList<PVector> rdpPoints;
	/** list of indices of points in dragPoints captured for rdpPoints */
	private ArrayList<Integer> rdpIndices;
	/** A parameter to control the amount of reduction in the RDP algorithm */
	private float epsilon = 8.0f;
	/** A path composed of Bezier curves and lines, a continuous curved line derived from rdpPoints */
	private PABezShape curveShape;
	/** List of points where events such as audio samples can be triggered, derived from the polygon representation 
	 * of curveShape, accessed with getEventPoints() or getCurvePoints() */
	private ArrayList<PVector> eventPoints;
	/** number of steps along a polygonized curve, used to produce eventPoints from curveShape */
	public int eventSteps = 8;
	/** A simulated brush stroke derived from curveShape, a closed shape rather than a curved line */
	private PABezShape brushShape;
	/** polygon representation of brushShape, for point testing, etc. */
	private ArrayList<PVector> brushPoly;
	/** number of steps along a polygonized brushstroke shape, used to produce brushPoly */
	public int polySteps = 8;
	/** Time when CurveMaker instance was initialized by mousePressed event, 
	 * in milliseconds since application startup */
	public int timeStamp;
	/** Time of mouseReleased event, in milliseconds elapsed since timeStamp */
	public int timeOffset;
	/** List of time data: first element is expected to be 0 and
	 *  the remaining elements are non-decreasing offsets in millis from the first element. */
	private int[] dragTimes;
	/** Cache for float[] version of dragTimes */
	float[] dragTimesFloat;
	/** boolean to determine if bezierBias, "weight", is applied to curve calculations */
	public boolean isDrawWeighted = false;
	/** A weight applied to calculations of the control points for Bezier curves */
	private float bezierBias = PABezShape.LAMBDA;
	/** Color for lines dragged by the mouse, stored in allPoints */

	
	/* ---------------------------------------------------------- */
	/*     Line and fill attributes, drawing style attributes     */
	/* ---------------------------------------------------------- */

	/** The distance in pixels between the edges of the brush stroke, double the distance from the central line */
	public float brushSize = 24.0f;
	/** boolean value that determines whether curve  and brush shapes are weighted */
	public int dragColor = PABezShape.composeColor(233, 199, 89, 128);   // tan
	/** weight of lines drawn between PVectors in allPoints */
	public float dragWeight = 8;
	/** Color for points reduced by RDP algorithm, stored in drawPoints */
	public int rdpColor = PABezShape.composeColor(233, 89, 144);         // red 
	/** weight of lines drawn between PVectors in drawPoints */
	public float rdpWeight = 2;
	/** Color for Bezier curve derived from drawPoints, stored in bezPoints or weightedBezPoints */
	public int curveColor = PABezShape.composeColor(55, 199, 246);       // blue
	/** weight of curved line curveShape */
	public float curveWeight = 4;
	/** Color for simulated brushstroke fill and stroke, stored in brushShape */
	public int brushColor = PABezShape.composeColor(76, 199, 144, 96);   // transparent green
	/** Color for highlighted brush stroke, stored in brushShape */
	public int activeBrushColor = PABezShape.composeColor(199, 89, 55, 192);   // transparent brick red
	/** weight of stroke for brushStroke, set to 0 for no stroke */
	public float brushWeight = 0;
	/** color of eventPoints markers */
	public int eventPointsColor = PABezShape.composeColor(233, 199, 144, 192);	// yellow
	/** size of eventPoints markers */
	public float eventPointsSize = 8;
	/** using a custom brush shape */
	public boolean brushIsCustom = false;
	
	
	/* ---------------------------------------------------------- */
	/*        Dirty flags, to help us track when to rebuild       */
	/* ---------------------------------------------------------- */
	
	// State that we're caching includes:
	//
	//    rdpPoints, rdpIndices (depends on epsilon)
	//	  curveShape (depends on rdpPoints)
	//	  eventPoints (depends on curveShape + curveSteps/eventSteps)
	//	  brushShape (depends on rdpPoints + brush params)
	//	  brushPoly (depends on brushShape + polySteps)

	private boolean dirtyReduced = true;  // epsilon changed OR allPoints changed
	private boolean dirtyCurve   = true;  // reduced changed
	private boolean dirtyEvent   = true;  // curve changed OR curveSteps changed
	private boolean dirtyBrush   = true;  // reduced changed OR brush params changed
	private boolean dirtyBrushPoly = true; // brush changed OR polySteps changed
		
	
	/**
	 * Constructor for PACurveMaker, called by all factory methods. 
	 * 
	 * @param dragPoints     a list of points (PVector) where no two successive points are identical
	 * @param dragTimes      a list of times in milliseconds, successively offset from first entry of 0
	 * @param startTimeMs    an absolute time in milliseconds, determined by the application, for the gesture to begin
	 */
	private PACurveMaker(List<PVector> dragPoints, List<Integer> dragTimes, int startTimeMs) {
	    if (dragPoints == null || dragTimes == null) throw new IllegalArgumentException();
	    if (dragPoints.size() != dragTimes.size()) throw new IllegalArgumentException("points/times mismatch");
	    this.dragPoints = new ArrayList<>(dragPoints);
	    this.dragTimes = normalizeOffsets(dragTimes.stream().mapToInt(Integer::intValue).toArray());
	    this.timeStamp = startTimeMs;
	    this.timeOffset = (this.dragTimes.length == 0) ? 0 : this.dragTimes[this.dragTimes.length - 1];
	    // leave for lazy initialization
	    this.rdpPoints = new ArrayList<>();
	    this.rdpIndices = new ArrayList<>();
	    markDirtyAll(); // or initialize dirty flags true
	}
	
	/**
	 * @param dragPoints     a list of points (PVector) where no two successive points are identical
	 * @param dragTimes      a list of times in milliseconds, successively offset from first entry of 0
	 * @param startTimeMs    an absolute time in milliseconds, determined by the application, for the gesture to begin
	 * @return a PACurveMaker instance, set up for lazy initialization
	 */
	public static PACurveMaker buildCurveMaker(List<PVector> dragPoints, List<Integer> dragTimes, int startTimeMs) {
	    return new PACurveMaker(dragPoints, dragTimes, startTimeMs);
	}
	
	/**
	 * @param dragPoints     a list of points (PVector) where no two successive points are identical
	 * @param dragTimes      a list of times in milliseconds, successively offset from first entry of 0
	 * @param startTimeMs    an absolute time in milliseconds, determined by the application, for the gesture to begin
	 * @param epsilon        the expected distance between reduced points, lower values typically produce more points
	 * @return a PACurveMaker instance, set up for lazy initialization
	 */
	public static PACurveMaker buildCurveMaker(List<PVector> dragPoints, List<Integer> dragTimes, int startTimeMs, float epsilon) {
		PACurveMaker cm = new PACurveMaker(dragPoints, dragTimes, startTimeMs);
		cm.setEpsilon(epsilon);
		return cm;
	}
	
	/**
	 * @param dragPoints     a list of points (PVector) where no two successive points are identical
	 * @param dragTimes      a list of times in milliseconds, successively offset from first entry of 0
	 * @param startTimeMs    an absolute time in milliseconds, determined by the application, for the gesture to begin
	 * @param epsilon        the expected distance between reduced points, lower values typically produce more points
	 * @param brushSize      the width in pixels of a simulated brushstroke created from the Bezier path derived from rdpPoints
	 * @param brushColor     the color assigned to the brushstroke
	 * @return a PACurveMaker instance, set up for lazy initialization
	 */
	public static PACurveMaker buildCurveMaker(List<PVector> dragPoints, 
			List<Integer> dragTimes, int startTimeMs, float epsilon,
			float brushSize, int brushColor) {
		PACurveMaker cm = new PACurveMaker(dragPoints, dragTimes, startTimeMs);
		cm.setEpsilon(epsilon);
		cm.setBrushSize(brushSize);
		cm.setBrushColor(brushColor);   // or setter if you add one
		return cm;
	}

		
	/**
	 * This factory method can be used when you are only using the curve modeling features of PACurveMaker. 
	 * Entries in the time offsets list dragTimes will be set to 0 and startTimesMs will be set to 0. If you 
	 * set values for dragTimes and startTimeMs later, dragTimes and dragPoints must be the same size.
	 * 
	 * @param dragPoints    a list of points (PVector) where no two successive points are identical
	 * @return a PACUrveMaker instance
	 */
	public static PACurveMaker buildCurveMaker(ArrayList<PVector> dragPoints) {
		if (dragPoints == null) throw new IllegalArgumentException();
	    ArrayList<Integer> times = new ArrayList<>(dragPoints.size());
	    for (int i = 0; i < dragPoints.size(); i++) times.add(0);
		return new PACurveMaker(dragPoints, times, 0);
	}
	
	/**
	 * This factory method can be used when you are only using the curve modeling features of PACurveMaker. 
	 * Entries in the time offsets list dragTimes will be set to 0 and startTimesMs will be set to 0. If you 
	 * set values for dragTimes and startTimeMs later, dragTimes and dragPoints must be the same size.
	 * 
	 * @param dragPoints     a list of points (PVector), where no two successive points are identical
	 * @param epsilon        controls amount of thinning of points to derive the reduced point set rdpPoints from dragPoints
	 * @return a PACurveMaker instance
	 */
	public static PACurveMaker buildCurveMaker(ArrayList<PVector> dragPoints, float epsilon) {
		PACurveMaker curveMaker = buildCurveMaker(dragPoints);
		curveMaker.setEpsilon(epsilon);
		return curveMaker;
	}
	
	/**
	 * This factory method can be used when you are only using the curve modeling features of PACurveMaker. 
	 * Entries in the time offsets list dragTimes will be set to 0 and startTimesMs will be set to 0. If you 
	 * set values for dragTimes and startTimeMs later, dragTimes and dragPoints must be the same size.
	 * 
	 * @param dragPoints     a list of points (PVector), where no two successive points are identical
	 * @param epsilon        controls amount of thinning of points to derive the reduced point set rdpPoints from dragPoints
	 * @param brushSize      the width in pixels of a simulated brushstroke created from the Bezier path derived from rdpPoints
	 * @param brushColor     the color assigned to the brushstroke
	 * @return a PACurveMaker instance
	 */
	public static PACurveMaker buildCurveMaker(ArrayList<PVector> points,
			float epsilon,
			float brushSize,
			int brushColor) {
		PACurveMaker curveMaker = buildCurveMaker(points);
		curveMaker.setEpsilon(epsilon);
		curveMaker.setBrushSize(brushSize);
		curveMaker.setBrushColor(brushColor);   // or setter if you add one
		return curveMaker;
	}

	
	/**
	 * Creates a PACurveMaker instance set up for lazy initialization.
	 * Sets various properties used for drawing PACurveMaker graphics. 
	 * 
	 * @param dragPoints
	 * @param dragTimes
	 * @param startTimeMs
	 * @param epsilon
	 * @param dragColor
	 * @param dragWeight
	 * @param rdpColor
	 * @param rdpWeight
	 * @param curveColor
	 * @param curveWeight
	 * @param brushColor
	 * @param brushWeight
	 * @param activeBrushColor
	 * @return
	 */
	public static PACurveMaker buildCurveMaker(List<PVector> dragPoints, List<Integer> dragTimes, int startTimeMs, 
			float epsilon, int dragColor, float dragWeight, int rdpColor, float rdpWeight, 
			int curveColor, float curveWeight, int brushColor, float brushWeight, int activeBrushColor) {
		PACurveMaker curveMaker = new PACurveMaker(dragPoints, dragTimes, startTimeMs);
		curveMaker.setEpsilon(epsilon);
		curveMaker.setDrawingProperties(dragColor, dragWeight, rdpColor, rdpWeight, curveColor, curveWeight, brushColor, brushWeight, activeBrushColor);
		return curveMaker;
	}
	
	/* ------------- thar beez methoddes inna dis madnesse ------------- */
	
	/**
	 * Sets various properties used for drawing PACurveMaker graphics.
	 * 
	 * @param dragColor
	 * @param dragWeight
	 * @param rdpColor
	 * @param rdpWeight
	 * @param curveColor
	 * @param curveWeight
	 * @param brushColor
	 * @param brushWeight
	 * @param activeBrushColor
	 */
	public void setDrawingProperties(int dragColor, float dragWeight, int rdpColor, float rdpWeight, 
			                         int curveColor, float curveWeight, int brushColor, float brushWeight, int activeBrushColor) {
		this.dragColor = dragColor;
		this.dragWeight = dragWeight;
		this.rdpColor = rdpColor;
		this.rdpWeight = rdpWeight;
		this.curveColor = curveColor;
		this.curveWeight = curveWeight;
		this.brushColor = brushColor;
		this.brushWeight = brushWeight;
		this.activeBrushColor = activeBrushColor;
	}

	private static int[] normalizeOffsets(int[] t) {
	    if (t == null) throw new IllegalArgumentException("dragTimes is null");
	    if (t.length == 0) return t;
	    int[] out = t.clone();
	    // shift so start is 0
	    int t0 = out[0];
	    if (t0 != 0) {
	        for (int i = 0; i < out.length; i++) out[i] -= t0;
	    }
	    // enforce non-decreasing
	    for (int i = 1; i < out.length; i++) {
	        if (out[i] < out[i - 1]) out[i] = out[i - 1];
	    }
	    // ensure first is exactly 0
	    out[0] = 0;
	    return out;
	}

	/**
	 * Takes the list of points in allPoints and generates a reduced list in drawPoints.
	 * 
	 * @param epsilon    controls amount of thinning applied to dragPoints to derive rdpPoints
	 */
	private void reducePoints(float epsilon) {
		rdpPoints.clear();
		rdpIndices.clear();
		int total = dragPoints.size();
		PVector start = dragPoints.get(0);
		PVector end = dragPoints.get(total - 1);
		rdpPoints.add(start);
		rdpIndices.add(0);
		// rdp reduces allPoints and puts the result into drawPoints
		PACurveUtility.indexedRDP(0, total - 1, dragPoints, rdpPoints, rdpIndices, epsilon);
		// put in a midpoint when there are only two points in the reduced points
		if (rdpPoints.size() == 1) {
			PVector midPoint = start.copy().add(end).div(2.0f);
			rdpPoints.add(midPoint);
		}
		rdpPoints.add(end);
		rdpIndices.add(total - 1);
	}
	
	/**
	 * We can build all the relevant data structures for geometry here, typically on 
	 * initialization. After that, we can rely on lazy rebuilding, using the "dirty flags".
	 * 
	 * Calculates rdpPoints, curveShape, eventPoints, brushShape and brushPoly.
	 * Sets isReady to true on completion. Call calculateDerivedPoints to refresh the 
	 * drawing objects rdpPoints, curveShape, eventPoints, brushShape, and brushPoly.  
	 */
	public void calculateDerivedPoints() {
	    reducePoints(epsilon);
	    dirtyReduced = false;

	    curveShape = (rdpPoints.size() >= 2)
	        ? PACurveUtility.calculateCurve(rdpPoints)
	        : null;
	    dirtyCurve = false;

	    eventPoints = (curveShape != null)
	        ? curveShape.getPointList(Math.max(1, eventSteps))
	        : new ArrayList<>();
	    dirtyEvent = false;

	    brushShape = (rdpPoints.size() >= 2)
	        ? buildBrushShape(rdpPoints)
	        : null;
	    dirtyBrush = false;

	    brushPoly = (brushShape != null)
	        ? brushShape.getPointList(Math.max(1, polySteps))
	        : new ArrayList<>();
	    dirtyBrushPoly = false;
	}
	
	public boolean isReady() {
		return !(dirtyReduced || dirtyCurve || dirtyEvent || dirtyBrush || dirtyBrushPoly);
	}
	
	
	/* ---------------------------------------------------------- */
	/*                    CACHED STATE METHODS                    */
	/* ---------------------------------------------------------- */
	
	/* ------------- allPoints / dragPoints ------------- */

	public ArrayList<PVector> getDragPoints() {
		return copyAllPoints();
	}
	
	public ArrayList<PVector> copyAllPoints() {
	    ArrayList<PVector> out = new ArrayList<>(dragPoints.size());
	    for (PVector p : dragPoints) out.add(p.copy());
	    return out;
	}
	
	/**
	 * Alias method for getDragPoints().
	 * 
	 * @return ArrayList of PVector
	 */
	public ArrayList<PVector> getAllPoints() {
		return this.getDragPoints();
	}

	
	/**
	 * TODO remove: use mutateAllPoints() to change points.
	 * Changes the value of dragPoints and immediately calls calculateDerivedPoints() 
	 * to refresh all drawing objects. 
	 * 
	 * @param dragPoints	ArrayList<PVector>, a dense set of points for drawing a line
	 */
	@Deprecated
	public void setDragPoints(ArrayList<PVector> dragPoints) {
		this.dragPoints = dragPoints;
		calculateDerivedPoints();
		dragTimesFloat = null;
	}

	// In PACurveMaker.java

	/**
	 * Mutate the live dragPoints list (and/or its PVectors) and automatically
	 * mark cached geometry as dirty afterward.
	 *
	 * Typical use: translating/scaling/warping points for animation.
	 *
	 * IMPORTANT:
	 * - If you change the number/order of points, you must also update dragTimes
	 *   so that (dragTimes.size() == dragPoints.size()).
	 */
	public void mutateAllPoints(PointsMutator mutator) {
	    if (mutator == null) return;
	    mutator.apply(dragPoints);
	    markDirtyAll();
	}

	/** Simple Processing-friendly callback interface.
	 * 
	 *  maker.mutateAllPoints(pts -> {
	 *      for (PVector p : pts) { p.x += 10; p.y  += 10}
	 *  });
	 *
	 */
	public interface PointsMutator {
	    void apply(ArrayList<PVector> pts);
	}

	/** Call after mutating dragPoints contents or order (or changing dragTimes length). */
	public void markDirtyAll() {
	    dirtyReduced = true;
	    dirtyCurve = true;
	    dirtyEvent = true;
	    dirtyBrush = true;
	    dirtyBrushPoly = true;
	}

	/** Call after changing only epsilon-sensitive state (typically point geometry changed). */
	public void markDirtyReduced() {
	    dirtyReduced = true;
	    dirtyCurve = true;
	    dirtyEvent = true;
	    dirtyBrush = true;
	    dirtyBrushPoly = true;
	}

	/** Call after changing only curveSteps/eventSteps. */
	public void markDirtyEventPoints() {
	    dirtyEvent = true;
	}

	/** Call after changing brush size/weighting/bias. */
	public void markDirtyBrush() {
	    dirtyBrush = true;
	    dirtyBrushPoly = true;
	}

	
	/* ------------- reducedPoints / rdpPoints ------------- */
	
	public float getEpsilon() {
		return epsilon;
	}

	/**
	 * Sets value of variable epsilon, used in reduced point set calculations. If the new value
	 * is not equal to the old value, will trigger various "dirty flags" to force rebuild of 
	 * cached values. 
	 * 
	 * @param eps    value for epsilon
	 */
	public void setEpsilon(float eps) {
		if (this.epsilon != eps) {
			this.epsilon = eps;
			dirtyReduced = true;
			dirtyCurve = true;
			dirtyEvent = true;
			dirtyBrush = true;
			dirtyBrushPoly = true;
		}
	}	

	public ArrayList<PVector> getRdpPoints() {
		return getReducedPoints(this.epsilon);
	}
	
	/**
	 * Alias for getReducedPoints(this.epsilon). 
	 * 
	 * @return a list of PVectors, the reduced point set for this PACurveMaker.
	 */
	public ArrayList<PVector> getReducedPoints() {
		return getReducedPoints(this.epsilon);
	}

	/**
	 * Sets epsilon and returns the reduced point set, this.rdpPoints
	 * 
	 * @param epsilon    parameter for RDP algorithm, determines distance/angle for culling points
	 * @return
	 */
	public ArrayList<PVector> getReducedPoints(float epsilon) {
		setEpsilon(epsilon);        // can set various "dirty flags"
		ensureReduced();			// respond to dirty flags
		return rdpPoints;
	}
	
	private void ensureReduced() {
		if (!dirtyReduced) return;
		reducePoints(this.epsilon);     // rebuild rdpPoints + rdpIndices
		dirtyReduced = false;
	}

	/**
	 * Sets rdpPoints, generally not something you want to do: 
	 * let reducePoints() or calculateDerivedPoints() 
	 * derive rdpPoints from dragPoints instead. 
	 * 
	 * @param rdpPoints
	 */
	@Deprecated
	public void setRdpPoints(ArrayList<PVector> rdpPoints) {
		this.rdpPoints = rdpPoints;
	}
	
	public ArrayList<Integer> getRdpIndices() {
		ensureReduced();
		return rdpIndices;
	}
	
	public int[] getRdpIndicesAsInts() {
		ensureReduced();
		return rdpIndices.stream().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * Sets rdpIndices, generally not something you want to do.
	 * Call reducePoints() or calculateDerivedPoints instead
	 * and stay out of trouble. 
	 * 
	 * @param rdpIndices
	 */
	@Deprecated
	public void setRdpIndices(ArrayList<Integer> rdpIndices) {
		this.rdpIndices = rdpIndices;
	}


	/* ------------- eventSteps / curveSteps ------------- */
	
	public int getEventSteps() {
		return eventSteps;
	}

	// Alias for getEventSteps() 
	public int getCurveSteps() {
		return this.eventSteps;
	}
	
	/* Alias for setEeventSteps(int).
	 * 
	 * @param curveSteps
	 */
	public void setCurveSteps(int curveSteps) {
		setEventSteps(curveSteps);
	}

	/**
	 * If eventPoints is null or if eventSteps differs from the old value,
	 * sets a new value for eventSteps and calculates eventPoints.
	 * 
	 * @param steps    a new value for eventSteps
	 */
	public void setEventSteps(int steps) {
		steps = Math.max(1, steps);
		if (this.eventPoints == null || this.eventSteps != steps) {
			this.eventSteps = steps;
			// instead of immediately setting eventPoints, we implement a lazy rebuild strategy
			// this.eventPoints = this.getCurveShape().getPointList(steps);    // rebuild
			dirtyEvent = true;
		}
	}
	
	
	/* ------------- eventPoints / curvePoints ------------- */

	/**
	 * 
	 * @return this.eventPoints, the points along a polyline version of the 
	 *         Bezier curve derived from the reduced point set. 
	 */
	public ArrayList<PVector> getEventPoints() {
		ensureEventPoints();
		return this.eventPoints;
	}

	/**
	 * @param eventSteps    the number of points along each polyline curve in the 
	 *                      PABezShape this.curveShape
	 * 
	 * @return this.eventPoints, the points along a polyline version of the 
	 *         Bezier curve derived from the reduced point set. 
	 */
	public ArrayList<PVector> getEventPoints(int eventSteps) {
		setEventSteps(eventSteps);
		ensureEventPoints();
		return this.eventPoints;
	}
	
	/**
	 * Alias for getEventPoints().
	 * 
	 * @return this.eventPoints, the points along a polyline version of the Bezier curve derived from the reduced point set. 
	 */
	public ArrayList<PVector> getCurvePoints() {
		return this.getEventPoints();
	}

	/**
	 * Alias for getEventPoints(int curveSteps).
	 * 
	 * @param curveSteps
	 * @return points along a polyline version of the Bezier curve derived from the reduced point set. 
	 */
	public ArrayList<PVector> getCurvePoints(int curveSteps) {
		return this.getEventPoints(curveSteps);
	}
	
	private void ensureEventPoints() {
		ensureCurve();
		if (!dirtyEvent) return;
		if (curveShape == null) { eventPoints = new ArrayList<>(); dirtyEvent = false; return; }
		eventPoints = curveShape.getPointList(Math.max(1, eventSteps));
		dirtyEvent = false;
	}

	// NO SETTER FOR eventPoints, handle internally only 
	
	/**
	 * @return PABezShape for the curved line derived from the reduced point set.
	 */
	public PABezShape getCurveShape() {
		ensureCurve();
		return this.curveShape;
	}
	
	/**
	 * Alias for getCurveShape().
	 * 
	 * @return PABezShape for the curved line derived from the reduced point set.
	 */
	public PABezShape getCurve() {
		return getCurveShape();
	}
	
	private void ensureCurve() {
		ensureReduced();
		if (!dirtyCurve) return;
		if (rdpPoints == null || rdpPoints.size() < 2) { curveShape = null; dirtyCurve = false; return; }
		curveShape = PACurveUtility.calculateCurve(rdpPoints);
		dirtyCurve = false;
	}
	
	/**
	 * Calculates a PABezSHape with distances between control points and anchor 
	 * points adjusted by bezierBias. Does not store the returned curve. 
	 * 
	 * @param bias		a parameter to adjust distances between Bezier anchor points and control points.
	 * @return			a Bezier curve shape whose control points are adjusted using bezierBias. 
	 */
	public PABezShape getWeightedCurveShape(float bias) {
		return PACurveUtility.calculateWeightedCurve(this.getRdpPoints(), bias);
	}
	
	public PABezShape getWeightedCurveShape() {
		return PACurveUtility.calculateWeightedCurve(this.getRdpPoints(), this.bezierBias);
	}
	
	
	/* ------------- weighted Bezier settings ------------- */
	
	public boolean isDrawWeighted() {
		return isDrawWeighted;
	}

	/**
	 * Flag whether to draw weighted Bezier curves, displacing control points from 
	 * the anchor by a specified bias, bezierBias. For example, we use the bias value PABezShape.KAPPA, 
	 * to draw circles with four equidistant anchor points. 
	 * 
	 * @param drawBiased
	 */
	public void setDrawWeighted(boolean drawBiased) {
		if (this.isDrawWeighted != drawBiased) {
			this.isDrawWeighted = drawBiased;
			dirtyBrush = true;
			dirtyBrushPoly = true;
		}
	}

	public float getBezierBias() {
		return bezierBias;
	}

	public void setBezierBias(float bias) {
		if (this.bezierBias != bias) {
			this.bezierBias = bias;
			if (isDrawWeighted) {           // bias irrelevant unless weighted
				dirtyBrush = true;
				dirtyBrushPoly = true;
			}
		}
	}

	
	/* ------------- brushSize, brushShape, brushPoly ------------- */
	
	public float getBrushSize() {
		return brushSize;
	}

	/**
	 * Set the distance in pixels between opposite edges of a brushstroke. 
	 * 
	 * @param newSize
	 */
	public void setBrushSize(float newSize) {
		if (this.brushSize != newSize) {
			this.brushSize = newSize;
			dirtyBrush = true;
			dirtyBrushPoly = true;
		}
	}
	
	/**
	 * @return    a PABezShape that looks like a brushstroke, created by expanding curveShape, 
	 *            an open Bezier path, into a closed Bezier path. Handy for interaction with 
	 *            gesture data. 
	 */
	public PABezShape getBrushShape() {
		ensureBrush();
		return brushShape;
	}
	
	private void ensureBrush() {
		ensureReduced();
		// for custom brush, don't rebuild
		if (brushIsCustom) {
			if (brushShape != null) {
				// do we want to set attributes from PACurveMaker local vars?
			}
			dirtyBrush = false;
			return;
		}
		if (!dirtyBrush) return;
		if (rdpPoints == null || rdpPoints.size() < 2) { brushShape = null; dirtyBrush = false; return; }
		brushShape = buildBrushShape(rdpPoints);
		brushShape.setNoStroke();
		brushShape.setFillColor(brushColor);
		dirtyBrush = false;
	}


	/**
	 * @return a PABezShape modifed by the bezierBias value, equal to PABezShape.LAMBDA by default. 
	 *         Experiment to see how values above and below LAMBDA affect the curves. 
	 */
	public PABezShape getWeightedBrushShape() {
		PABezShape weightedBrushShape = PACurveUtility.calculateWeightedCurve(this.getBrushShape(), this.bezierBias);
		return weightedBrushShape;
	}

	/**
	 * @param brushSize
	 * @param bias
	 * @return a PABezShape modifed by the bezierBias value, equal to PABezShape.LAMBDA by default. 
	 *         Experiment to see how values above and below LAMBDA affect the curves. 
	 */
	public PABezShape getWeightedBrushShape(float brushSize, float bias) {
		PABezShape weightedBrushShape = PACurveUtility.calculateWeightedCurve(this.getBrushShape(), bias);
		return weightedBrushShape;
	}
	
	
	/**
	 * @param knotPoints    a list of points that will be used to derive anchor points
	 * @return a closed Bezier path shaped like a stylized brushstroke around the supplied knotPoints
	 */
	public PABezShape buildBrushShape(ArrayList<PVector> knotPoints) {
		// brushSize is the distance in pixels between the edges of the brush stroke 
		// from the central generating curve, knotPoints
		if (this.isDrawWeighted) {
			return PACurveUtility.quickBrushShape(knotPoints, brushSize, true, this.bezierBias);
			//System.out.println("-- weighted brush, bias = "+ this.bezierBias);
		}
		else {
			return PACurveUtility.quickBrushShape(knotPoints, brushSize);
		}
	}

	/**
	 * Replaces the closed PABezShape "brushstroke" with a shape of your own choosing.
	 * Useful to provide a point of interaction. Rely on dragPoints or curveShape
	 * and dragTimes for gestures and timing. 
	 * 
	 * @param customBrushShape
	 */
	public void setCustomBrushShape(PABezShape customBrushShape) {
		this.brushShape = customBrushShape;
		dirtyBrush = false;        // brush has been set
		dirtyBrushPoly = true;     // polygon will be built from brushShape
		brushIsCustom = (customBrushShape != null);
	}

	/**
	 * Sets brushIsCustom to false, prepares rebuild of brush from curveShape
	 */
	public void clearCustomBrushShape() {
		this.brushIsCustom = false;
		dirtyBrush = true;         // rebuild from reduced points
		dirtyBrushPoly = true;
	}

	/**
	 * @return    a list of points that comprise a closed polygonal shape that closely folllows the brushstroke.
	 *            Useful for point in polygon testing for interaction. If you replaced the brush with your own
	 *            PABezShape, you'll get a polygonal representation of your own shape. 
	 */
	public ArrayList<PVector> getBrushPoly() {
		ensureBrushPoly();
		return brushPoly;
	}
	
	private void ensureBrushPoly() {
		ensureBrush();
		if (!dirtyBrushPoly) return;
		if (brushShape == null) { brushPoly = new ArrayList<>(); dirtyBrushPoly = false; return; }
		brushPoly = brushShape.getPointList(Math.max(1, polySteps));
		dirtyBrushPoly = false;
	}
	
	/**
	 * @return    the number of divisions of each Bezier curve in the polygonal 
	 *            representation of brushShape.
	 */
	public int getPolySteps() {
		return polySteps;
	}

	/**
	 * @param steps    the number of divisions of each Bezier curve in the polygonal
	 *                 representation of brushShape. See also setEventSteps().
	 */
	public void setPolySteps(int steps) {
		steps = Math.max(1, steps);
		if (this.polySteps != steps) {
			this.polySteps = steps;
			dirtyBrushPoly = true;
		}
	}

	
	/* ------------- timing accessors ------------- */
	
	public int getTimeStamp() {
		return timeStamp;
	}
	
	public void setTimeStamp(int timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public long getStartTimeMs() {
		return getTimeStamp();
	}

	public int getTimeOffset() {
		return timeOffset;
	}

	public void setTimeOffset(int timeOffset) {
		this.timeOffset = timeOffset;
	}
	
	/**
	 * Returns time offsets as floats. First value should be 0, remaining ones are times
	 * from 0 in milliseconds. 
	 */
	public float[] getTimeOffsetsMs() {
		if (dragTimesFloat == null) 
			dragTimesFloat = PACurveUtility.intsToFloats(getDragTimes());
		return dragTimesFloat;
	}
		
	/**
	 * @return the list of time data for the gesture captured by dragPoints: 
	 *         The first offset is expected to be 0 and the remaining elements
	 *         are offsets in milliseconds. 
	 *         
	 * @return    a list of time offsets, the first one equal to zero
	 */
	public int[] getDragTimes() {
		return dragTimes;
	}
	
	/**
	 * getAllTimes() is a convenient alias for getDragTimes()
	 * 
	 * @return a list of time offsets, the first one equal to zero 
	 */
	public int[] getAllTimes() {
		return getDragTimes();
	}

	/**
	 * @param dragTimes
	 */
	public void setDragTimes(int[] dragTimes) {
		if (dragTimes == null) throw new IllegalArgumentException();
		if (dragPoints.size() != dragTimes.length) throw new IllegalArgumentException("points/times mismatch");
		this.dragTimes = normalizeOffsets(dragTimes);
		dragTimesFloat = null;
	}
	
	public void setDragTimes(List<Integer> dragTimes) {
		if (dragTimes == null) throw new IllegalArgumentException();
		if (dragPoints.size() != dragTimes.size()) throw new IllegalArgumentException("points/times mismatch");
	    this.dragTimes = dragTimes.stream().mapToInt(Integer::intValue).toArray();
		dragTimesFloat = null;
	}

	public int[] getReducedTimes() {
		ensureReduced();
		int[] timeIndices = getRdpIndicesAsInts();
		int[] rdpTimes = new int[timeIndices.length];
		// the first value in dragTimes is a time stamp, all following numbers are offsets
		// one for each point in dragPoints. We only need the offsets. 
		int[] allTimes = this.getDragTimes();
		for (int i = 0; i < timeIndices.length; i++) {
			rdpTimes[i] = allTimes[timeIndices[i]];
		}
		return rdpTimes;
	}

	
	/* ---------------------------------------------------------- */
	/*                    Gesture scheduling                      */
	/* ---------------------------------------------------------- */

	public GestureSchedule getAllPointsSchedule() {
        ArrayList<PVector> pts = getAllPoints(); // alias to getDragPoints()
        float[] times = PACurveUtility.intsToFloats(getDragTimes(), pts.size());
        return new GestureSchedule(pts, times);
    }

    public GestureSchedule getReducedSchedule(float epsilon) {
        ArrayList<PVector> pts = getReducedPoints(epsilon);
        float[] times = PACurveUtility.intsToFloats(getReducedTimes(), pts.size());
        return new GestureSchedule(pts, times);
    }

    public GestureSchedule getCurveSchedule(float epsilon, int curveSteps, boolean arcLengthTime) {
        // Ensure reduced points/times are up-to-date for this epsilon
        ArrayList<PVector> reduced = getReducedPoints(epsilon);
        if (reduced == null || reduced.size() == 0) {
            return new GestureSchedule(new ArrayList<>(), new float[0]);
        }
        float[] reducedTimes = PACurveUtility.intsToFloats(getReducedTimes(), reduced.size());
        PABezShape curve = getCurve();
        if (curve == null) {
        	return new GestureSchedule(reduced, reducedTimes);
        }
        return PACurveUtility.buildScheduleFromBezShape(curve, curveSteps, reducedTimes, arcLengthTime);
    }

    
	/* ---------------------------------------------------------- */
	/*                    DRAWING AND DISPLAY                     */
	/* ---------------------------------------------------------- */
	
	/* ------------- drawing attributes for lines, curves, shapes ------------- */

	public int getDragColor() {
		return dragColor;
	}

	public void setDragColor(int dragColor) {
		this.dragColor = dragColor;
	}

	public float getDragWeight() {
		return dragWeight;
	}

	public void setDragWeight(float dragWeight) {
		this.dragWeight = dragWeight;
	}

	public int getRdpColor() {
		return rdpColor;
	}

	public void setRdpColor(int rdpColor) {
		this.rdpColor = rdpColor;
	}

	public float getRdpWeight() {
		return rdpWeight;
	}

	public void setRdpWeight(float rdpWeight) {
		this.rdpWeight = rdpWeight;
	}

	public int getCurveColor() {
		return curveColor;
	}

	public void setCurveColor(int curveColor) {
		this.curveColor = curveColor;
	}
	
	public float getCurveWeight() {
		return curveWeight;
	}

	public void setCurveWeight(float curveWeight) {
		this.curveWeight = curveWeight;
	}
	
	public int getBrushColor() {
		return brushColor;
	}

	public void setBrushColor(int brushColor) {
		this.brushColor = brushColor;
	}

	public float getBrushWeight() {
		return brushWeight;
	}

	public void setBrushWeight(float brushWeight) {
		this.brushWeight = brushWeight;
	}

	public int getActiveBrushColor() {
		return activeBrushColor;
	}

	public void setActiveBrushColor(int activeBrushColor) {
		this.activeBrushColor = activeBrushColor;
	}

	public int getEventPointsColor() {
		return eventPointsColor;
	}

	public void setEventPointsColor(int eventPointsColor) {
		this.eventPointsColor = eventPointsColor;
	}

	public float getEventPointsSize() {
		return eventPointsSize;
	}

	public void setEventPointsSize(float eventPointsSize) {
		this.eventPointsSize = eventPointsSize;
	}
	

	/************************************************
	 *                                              *
	 * ------------- DRAWING METHODS -------------  *
	 *                                              *
	 ************************************************/
	
	
	// ------------- PApplet drawing methods for allPoints and drawPoints ------------- //

	/**
	 * Draws a line using the PVector data in dragPoints and a supplied color.
	 * 
	 * @param parent		a PApplet where drawing takes place
	 * @param lineColor		color for the line that is drawn
	 */
	public void dragPointsDraw(PApplet parent, int lineColor, float lineWeight) {
		PACurveUtility.lineDraw(parent, this.dragPoints, lineColor, lineWeight);
	}

	/**
	 * Draws a line using the PVector data in allPoints and the current values
	 * of this.dragColor and this.dragWeight.
	 * 
	 * @param parent		a PApplet where drawing takes place
	 */
	public void dragPointsDraw(PApplet parent) {
		PACurveUtility.lineDraw(parent, this.dragPoints, this.dragColor, this.dragWeight);
	}

	/**
	 * Draws a line using the PVector data in drawPoints, 
	 * the reduced point set derived from allPoints.
	 * 
	 * @param parent		a PApplet where drawing takes place
	 * @param lineColor		the color for the line that is drawn
	 * @param lineWeight	the weight of the line that is drawn
	 */
	public void reducedPointsDraw(PApplet parent, int lineColor, float lineWeight) {
		PACurveUtility.lineDraw(parent, this.getRdpPoints(), lineColor, lineWeight);
	}
	
	/**
	 * Draws a line using the PVector data in rdpPoints and the current values 
	 * of this.rdpColor and this.rdpWeight.
	 * 
	 * @param parent		a PApplet where drawing takes place
	 */
	public void reducedPointsDraw(PApplet parent) {
		PACurveUtility.lineDraw(parent, this.getRdpPoints(), this.rdpColor, this.rdpWeight);
	}

	
	// ------------- PGraphics drawing methods for allPoints and drawPoints ------------- //
	
	/**
	 * Draws dragPoints to a PGraphics using supplied color and weight.
	 * 
	 * @param pg			a PGraphics offscreen graphics context
	 * @param lineColor		color for the line through the dense point set
	 * @param lineWeight	weight (in pixels) of the line through the dense point set
	 */
	public void dragPointsDraw(PGraphics pg, int lineColor, float lineWeight) {
		PACurveUtility.lineDraw(pg, this.dragPoints, lineColor, lineWeight);
	}
	
	/**
	 * Draws dragPoints to a PGraphics using local color and weight, this.dragColor and this.dragWeight.
	 * 
	 * @param pg			a PGraphics offscreen graphics context
	 */
	public void dragPointsDraw(PGraphics pg) {
		PACurveUtility.lineDraw(pg, this.dragPoints, this.dragColor, this.dragWeight);
	}

	/**
	 * Draws rdpPoints to a PGraphics using supplied color and weight.
	 * 
	 * @param pg			a PGraphics offscreen graphics context
	 * @param lineColor		color for the line through the reduced point set
	 * @param lineWeight	weight (in pixels) of the line through the reduced point set
	 */
	public void reducedPointsDraw(PGraphics pg, int lineColor, float lineWeight) {
		PACurveUtility.lineDraw(pg, this.getRdpPoints(), lineColor, lineWeight);
	}
	
	/**
	 * Draws drawPoints to a PGraphics using current values of this.rdpColor and this.rdpWeight.
	 * 
	 * @param pg	a PGraphics offscreen graphics context
	 */
	public void reducedPointsDraw(PGraphics pg) {
		PACurveUtility.lineDraw(pg, this.getRdpPoints(), this.rdpColor, this.rdpWeight);
	}
		
	
	// ------------- Drawing methods for curveShape and brushShape ------------- //
	

	/**
	 * Draws a Bezier curve using the 2D Bezier curve data in this.curveShape and the current
	 * values of this.curveColor and this.curveWeight. The resulting curve will have no fill.
	 * 
	 * @param parent			a PApplet instance
	 */
	public void curveDraw(PApplet parent) {
		PACurveUtility.curveDraw(parent, this.getCurveShape(), this.curveColor, this.curveWeight);
	}

	/**
	 * Draws a Bezier curve using local drawing properties of curveShape. 
	 * If the drawing properties use a fill, the resulting curve will have a fill.
	 * 
	 * @param parent			a PApplet instance
	 */
	public void curveDrawDirect(PApplet parent) {
		PACurveUtility.curveDraw(parent, this.getCurveShape());
	}

	/**
	 * Draws a Bezier curve using the 2D Bezier curve data in this.curveShape and the current
	 * values of this.curveColor and this.curveWeight. The resulting curve will have no fill.
	 * 
	 * @param pg				a PGraphics offscreen graphics context
	 */
	public void curveDraw(PGraphics pg) {
		PACurveUtility.curveDraw(pg, this.getCurveShape(), this.curveColor, this.curveWeight);
	}

	/**
	 * Draws a Bezier curve using local drawing properties of this.curveShape as a PABezShape. 
	 * If the drawing properties use a fill, the resulting curve will have a fill.
	 * 
	 * @param pg				a PGraphics offscreen graphics context
	 */
	public void curveDrawDirect(PGraphics pg) {
		PACurveUtility.curveDraw(pg, this.getCurveShape());
	}
	
	/**
	 * Draws the stored brushShape to a PApplet using supplied fill color, stroke color, and weight.
	 * If brushWeight == 0, there is no stroke. 
	 * 
	 * @param parent
	 * @param brushColor
	 * @param strokeColor
	 * @param brushWeight
	 */
	public void brushDraw(PApplet parent, int brushColor, int strokeColor, float brushWeight) {
		PACurveUtility.shapeDraw(parent, this.getBrushShape(), brushColor, strokeColor, brushWeight);
	}

	/**
	 * Draws the stored brushShape to a PApplet using local properties this.brushColor, 
	 * this.brushColor, this.brushWeight.
	 * 
	 * @param parent
	 */
	public void brushDraw(PApplet parent) {
		PACurveUtility.shapeDraw(parent, this.getBrushShape(), this.brushColor, this.brushColor, this.brushWeight);
	}

	/**
	 * Draws the stored brush shape to a PApplet using its properties as a PABezShape.
	 * 
	 * @param parent
	 */
	public void brushDrawDirect(PApplet parent) {
		PACurveUtility.shapeDraw(parent, this.getBrushShape());
	}	

	/**
	 * Draws the stored brushShape to an offscreen PGraphics using supplied fill color, 
	 * stroke color, and weight. If brushWeight == 0, there is no stroke. 
	 * 
	 * @param parent
	 * @param brushColor
	 * @param strokeColor
	 * @param brushWeight
	 */
	public void brushDraw(PGraphics pg, int brushColor, int strokeColor, float brushWeight) {
		PACurveUtility.shapeDraw(pg, this.getBrushShape(), brushColor, strokeColor, brushWeight);
	}
	
	/**
	 * Draws the stored brushShape to an offscreen PGraphics using local properties 
	 * this.brushColor, this.brushColor, this.brushWeight.
	 * 
	 * @param parent
	 */
	public void brushDraw(PGraphics pg) {
		PACurveUtility.shapeDraw(pg, this.getBrushShape(), this.brushColor, this.brushColor, this.brushWeight);
	}
	
	/**
	 * Draws the stored brush shape to an offscreen PGraphics using its properties as a PABezShape.
	 * 
	 * @param parent
	 */
	public void brushDrawDirect(PGraphics pg) {
		PACurveUtility.shapeDraw(pg, this.getBrushShape());
	}	
	

	// ------------- Drawing methods for eventPoints ------------- //
	
	/**
	 * Draws this.eventPoints to a PApplet as circles using local properties 
	 * this.eventPointsColor and this.eventPointsSize.
	 * 
	 * @param parent
	 */
	public void eventPointsDraw(PApplet parent) {
		if (eventPoints == null) this.getEventPoints();
		parent.pushStyle();
		parent.noStroke();
		parent.fill(this.eventPointsColor);
		for (PVector v : this.eventPoints) {
			parent.circle(v.x, v.y, this.eventPointsSize);
		}
		parent.popStyle();
	}
	
	/**
	 * Draws this.eventPoints to a PApplet as circles using supplied properties 
	 * eventPointsColor and eventPointsSize.
	 * 
	 * @param parent
	 * @param eventPointsColor
	 * @param eventPointsSize
	 */
	public void eventPointsDraw(PApplet parent, int eventPointsColor, float eventPointsSize) {
		if (eventPoints == null) this.getEventPoints();
		parent.pushStyle();
		parent.noStroke();
		parent.fill(eventPointsColor);
		for (PVector v : this.eventPoints) {
			parent.circle(v.x, v.y, eventPointsSize);
		}
		parent.popStyle();
	}
	
	/**
	 * Draws this.eventPoints to a PApplet as circles using supplied properties 
	 * eventPointsSteps, eventPointsColor and eventPointsSize. EventPointsSteps 
	 * determines the number of circles. The local value eventPointsSteps is not changed.
	 * 
	 * @param parent
	 * @param eventSteps
	 * @param eventPointsColor
	 * @param eventPointsSize
	 */
	public void eventPointsDraw(PApplet parent, int eventSteps, int eventPointsColor, float eventPointsSize) {
		if (eventPoints == null) this.getEventPoints(eventSteps);
		parent.pushStyle();
		parent.noStroke();
		parent.fill(eventPointsColor);
		for (PVector v : eventPoints) {
			parent.circle(v.x, v.y, eventPointsSize);
		}
		parent.popStyle();
	}
	
	/**
	 * Draws this.eventPoints to an offscreen PGraphics as circles using local properties 
	 * this.eventPointsColor and this.eventPointsSize.
	 * 
	 * @param pg
	 */
	public void eventPointsDraw(PGraphics pg) {
		if (eventPoints == null) this.getEventPoints();
		pg.pushStyle();
		pg.noStroke();
		pg.fill(this.eventPointsColor);
		for (PVector v : eventPoints) {
			pg.circle(v.x, v.y, this.eventPointsSize);
		}
		pg.popStyle();
	}

	/**
	 * Draws this.eventPoints to an offscreen PGraphics as circles using supplied properties 
	 * eventPointsColor and eventPointsSize.
	 * 
	 * @param pg
	 * @param eventPointsColor
	 * @param eventPointsSize
	 */
	public void eventPointsDraw(PGraphics pg, int eventPointsColor, float eventPointsSize) {
		if (eventPoints == null) this.getEventPoints();
		pg.pushStyle();
		pg.noStroke();
		pg.fill(eventPointsColor);
		for (PVector v : eventPoints) {
			pg.circle(v.x, v.y, eventPointsSize);
		}
		pg.popStyle();
	}

	/**
	 * Draws this.eventPoints to an offscreen PGraphics as circles using supplied properties 
	 * eventPointsSteps, eventPointsColor and eventPointsSize. Parameter eventPointsSteps 
	 * determines the number of circles. The local value eventPointsSteps is not changed.
	 * 
	 * @param pg
	 * @param eventSteps
	 * @param eventPointsColor
	 * @param eventPointsSize
	 */
	public void eventPointsDraw(PGraphics pg, int eventSteps, int eventPointsColor, float eventPointsSize) {
		if (eventPoints == null) this.getEventPoints(eventSteps);
		pg.pushStyle();
		pg.noStroke();
		pg.fill(eventPointsColor);
		for (PVector v : eventPoints) {
			pg.circle(v.x, v.y, eventPointsSize);
		}
		pg.popStyle();
	}
		
}
