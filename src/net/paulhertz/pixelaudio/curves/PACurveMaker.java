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

import processing.core.PVector;
import processing.core.PApplet;
import processing.core.PGraphics;

// TODO provide multiple ways that eventPoints and brushPoly can work in PACurveMaker and in apps that use PACurveMaker instances:
// sometimes we'll want dense eventPoints, but leave brushPoly low-res. We'll consider how to vary events over playback.
// we can also consider how the time interval of a PACurveMaker instance could affect its playback.
// TODO JSON I/O: essential variables are dragPoints, epsilon, dragTimes. It's useful to save bezierBias and eventSteps. 
// One might also save curve data, but that is proper to PABezShape. When planting audio, the data used for the plant is critical. 
// That would seem to include the curve, reconstructed, and the curve points. Finally, one might consider the drawing properties. 


/**
 * <p>PACurveMaker is a utility and storage class for interactive drawing and  
 * curve modeling and for drawing to on screen PApplets or off screen PGraphics. 
 * You can use it for data storage in interactive drawing applications, or 
 * implement your own storage with just the features you require. PACUrveMaker
 * makes extensive use of PACurveUtility's static methods and provides
 * a wide range of graphic objects and properties for dense point arrays, 
 * reduced point arrays, derived Bezier curves (weighted and not weighted), 
 * brushstroke Bezier shapes, brushstroke polygons, plus some basic timing data
 * associated with drawing to the screen. You can use PACurveUtility 
 * for drawing all PACurveMaker data formats or you can use the built-in
 * commands if you use PACurveMaker as a storage class.</p>
 * 
 * <p>PACurveMaker's factory methods takes as their principal argument an array of PVectors.
 * The array is typically derived from a line drawn by dragging the mouse across the application
 * window. This line is stored in <code>dragPoints</code>. When <code>reducePoints()</code> or <code>calculateDerivedPoints()</code>
 * is called, <code>dragPoints</code> is reduced using the RDP algorithm and the result is stored in 
 * <code>rdpPoints</code>. <code>CalculateDerivedPoints()</code> passes the reduced points to <code>PACurveUtility.calculateCurve()</code>
 * to create a Bezier path, <code>curveShape</code>, using PixelAudio library's <code>PABezShape</code> class. Finally, a
 * brushstroke shape, <code>brushShape</code>, is created from the curve. An array of PVectors, 
 * <code>eventPoints</code>, is created from curveShape to facilitate timed event-staging along the curve. 
 * Another array of Pvectors, <code>brushPoly</code>, can be used to test points with <code>PABezShape.pointInPoly()</code>.
 * The values of all other fields are up to the caller. In particular, you may want to set <code>timeStamp</code>, 
 * <code>timeOffset</code>, and the <code>dragTimes</code> array, if the timing of drawing events is of interest. 
 * </p>
 * 
 * <p>PACurveMaker works in stages.  If you call any one of the <code>buildCurveMakerComplete()</code>  
 * factory methods, PACurveMaker will call <code>calculateDerivedPoints()</code> to create the objects 
 * listed in the previous paragraph for you. Otherwise, when you call the <code>buildCurveMaker()</code> factory methods, 
 * you will have the opportunity to set drawing properties and other fields yourself and then call <code>calculateDerivedPoints()</code>.
 * After a call to <code>calculateDerivedPoints()</code>, <code>isReady()</code> will return true. You can also
 * use lazy initialization to create PACurveMaker geometry as you need it. The fields for the various point 
 * and curve data objects are private: when you call a getter method such as <code>getCurveShape()</code>, it will 
 * create the curveShape if it has not yet been initialized. The same is true of rdpPoints, brushShape, eventPoints, 
 * curveShape and brushPoly. 
 * </p>
 * 
 * <p>Each of the geometric data objects--dragPoints, rdpPoints, curveShape, and brushShape--has 
 * method for drawing to the screen in a PApplet or drawing offscreen in a PGraphic. DragPoints and 
 * rdpPoints keep their drawing parameters (fill, stroke, weight) in PACurveMaker variables. PABezShapes
 * like curveShape and brushShape can store drawing parameters internally. 
 * </p>
 * 
 * <p>There a several properties of PACurveMaker that can affect the appearance of curves. The value of <code>epsilon</code>
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
 * 
 * 
 * 
 * 
 */
public class PACurveMaker {
	/** List of all the points that need to be reduced */
	public ArrayList<PVector> dragPoints;
	/** The reduced points delivered by the RDP algorithm */
	private ArrayList<PVector> rdpPoints;
	/** list of indices of points in dragPoints captured for rdpPoints */
	private ArrayList<Integer> rdpIndices;
	/** A parameter to control the amount of reduction in the RDP algorithm */
	public float epsilon = 8.0f;
	/** An ArrayList of PABezShapes representing a continuous curved line */
	private PABezShape curveShape;
	/** List of points where events such as audio samples can be triggered, from polygon representation of curveShape */
	private ArrayList<PVector> eventPoints;
	/** number of steps along a polygonized curve, used to produce eventPoints from curveShape */
	public int eventSteps = 8;
	/** A simulated brush stroke derived from curveShape */
	private PABezShape brushShape;
	/** polygon representation of brushShape, for point testing, etc. */
	private ArrayList<PVector> brushPoly;
	/** number of steps along a polygonized curve, used to produce brushPoly */
	public int polySteps = 8;
	/** Time when CurveMaker instance was initialized by mousePressed event, 
	 * in milliseconds since application startup */
	public int timeStamp;
	/** Time of mouseReleased event, in milliseconds elapsed since timeStamp */
	public int timeOffset;
	/** List of time data: first element is time in millis when event occurred, 
	 *  the remaining elements are offsets in millis from the first element  */
	ArrayList<Integer> dragTimes;

	/** The distance in pixels of the edges of the brush stroke from the central generating curve  */
	public float brushSize = 24.0f;
	/** boolean value that determines whether curve  and brush shapes are weighted */
	public boolean isDrawWeighted = false;
	/** A weight applied to calculations of the control points for Bezier curves */
	public float bezierBias = PABezShape.LAMBDA;
	/** Color for lines dragged by the mouse, stored in allPoints */
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
	
	/** Flag to indicate that allPoints, drawPoints, bezPoints, and brushShape have been initialized */
	private boolean isReady = false;
		
	
	/**
	 * Constructor called by all factory methods, initializes dragPoints, rdpPoints and rdpIndices.
	 * 
	 * @param points    the array of points to be reduced, stored in dragPoints
	 */
	private PACurveMaker(ArrayList<PVector> points) {
		this.dragPoints = new ArrayList<PVector>(points);
		this.rdpPoints = new ArrayList<PVector>();
		this.rdpIndices = new ArrayList<Integer>();
	}
	
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
	
	/**
	 * Creates a PACurveMaker from supplied points, caller must complete construction with 
	 * a call to calculateDerivedPoints() to generate rdpPoints and other instance variables. 
	 * 
	 * @param points    a dense point set
	 * @return a partially initialized PACurveMaker instance, requires a later call to calculateDeriverPoints()
	 */
	public static PACurveMaker buildCurveMaker(ArrayList<PVector> points) {
		PACurveMaker curveMaker = new PACurveMaker(points);
		return curveMaker;
	}
	
	/**
	 * @param points     a dense point set
	 * @param epsilon    controls amount of thinning of 0points to derive rdpPoints
	 * @return a partially initialized PACurveMaker instance, requires a later call to calculateDeriverPoints()
	 */
	public static PACurveMaker buildCurveMaker(ArrayList<PVector> points, float epsilon) {
		PACurveMaker curveMaker = new PACurveMaker(points);
		curveMaker.setEpsilon(epsilon);
		return curveMaker;
	}
	
	/**
	 * Creates a PACurveMaker from supplied points, caller must complete construction with 
	 * a call to calculateDerivedPoints() to generate rdpPoints and other instance variables. 
	 * Sets various properties used for drawing PACurveMaker graphics. 
	 * 
	 * @param points          a dense point set
	 * @param epsilon         controls amount of thinning of points to derive rdpPoints
	 * @param dragColor
	 * @param dragWeight
	 * @param rdpColor
	 * @param rdpWeight
	 * @param curveColor
	 * @param curveWeight
	 * @param brushColor
	 * @param brushWeight
	 * @param activeBrushColor
	 * @return a partially initialized PACurveMaker instance, requires a later call to calculateDeriverPoints()
	 */
	public static PACurveMaker buildCurveMaker(ArrayList<PVector> points, float epsilon, int dragColor, float dragWeight, int rdpColor, float rdpWeight, 
            								   int curveColor, float curveWeight, int brushColor, float brushWeight, int activeBrushColor) {
		PACurveMaker curveMaker = new PACurveMaker(points);
		curveMaker.setEpsilon(epsilon);
		curveMaker.setDrawingProperties(dragColor, dragWeight, rdpColor, rdpWeight, curveColor, curveWeight, brushColor, brushWeight, activeBrushColor);
		return curveMaker;
	}
	
	/**
	 * Creates a PACurveMaker from supplied points and calls calculatedDerivedPoints() to initialize 
	 * rdpPoints, curveShape, eventPoints, and  brushPoly and set isReady to true.
	 * 
	 * @param points   a dense point set
	 * @return a fully initialized PACurveMaker instance
	 */
	public static PACurveMaker buildCurveMakerComplete(ArrayList<PVector> points) {
		PACurveMaker curveMaker = new PACurveMaker(points);
		curveMaker.calculateDerivedPoints();
		return curveMaker;
	}

	/**
	 * Creates a PACurveMaker from supplied points and calls calculatedDerivedPoints() to initialize 
	 * rdpPoints, curveShape, eventPoints, and  brushPoly and set isReady to true.
	 * 
	 * @param points     a dense point set
	 * @param epsilon    controls amount of thinning of points to derive rdpPoints
	 * @return a fully initialized PACurveMaker instance
	 */
	public static PACurveMaker buildCurveMakerComplete(ArrayList<PVector> points, float epsilon) {
		PACurveMaker curveMaker = new PACurveMaker(points);
		curveMaker.setEpsilon(epsilon);
		curveMaker.calculateDerivedPoints();
		return curveMaker;
	}
	
	/**
	 * Creates a PACurveMaker from supplied points and calls calculatedDerivedPoints() to initialize 
	 * rdpPoints, curveShape, eventPoints, and  brushPoly and set isReady to true.
	 * Sets various properties used for drawing PACurveMaker graphics. 
	 * 
	 * @param points     a dense point set
	 * @param epsilon    controls amount of thinning of points to derive rdpPoints
	 * @param dragColor
	 * @param dragWeight
	 * @param rdpColor
	 * @param rdpWeight
	 * @param curveColor
	 * @param curveWeight
	 * @param brushColor
	 * @param brushWeight
	 * @param activeBrushColor
	 * @return a fully initialized PACurveMaker instance
	 */
	public static PACurveMaker buildCurveMakerComplete(ArrayList<PVector> points, float epsilon, int dragColor, float dragWeight, int rdpColor, float rdpWeight, 
            								   int curveColor, float curveWeight, int brushColor, float brushWeight, int activeBrushColor) {
		PACurveMaker curveMaker = new PACurveMaker(points);
		curveMaker.setEpsilon(epsilon);
		curveMaker.setDrawingProperties(dragColor, dragWeight, rdpColor, rdpWeight, curveColor, curveWeight, brushColor, brushWeight, activeBrushColor);
		curveMaker.calculateDerivedPoints();
		return curveMaker;
	}
	
	
	/**
	 * Takes the list of points in allPoints and generates a reduced list in drawPoints.
	 * 
	 * @param epsilon    controls amount of thinning applied to dragPoints to derive rdpPoints
	 */
	public void reducePoints(float epsilon) {
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
	 * Calculates rdpPoints, curveShape, eventPoints, brushShape and brushPoly.
	 * Sets isReady to true on completion. If you change epsilon, polySteps, eventSteps
	 * or isUseWeighted, call calculateDerivedPoints to refresh the drawing objects. 
	 */
	public void calculateDerivedPoints() {
		reducePoints(this.epsilon);
		curveShape = this.getCurveShape();
		eventPoints = curveShape.getPointList(polySteps);
		brushShape = this.getBrushShape();
		brushShape.setNoStroke();
		brushShape.setFillColor(brushColor);
		brushPoly = brushShape.getPointList(polySteps);
		isReady = true;
	}
	
	public boolean isReady() {
		return this.isReady;
	}
	
	public boolean isDrawWeighted() {
		return isDrawWeighted;
	}

	public void setDrawWeighted(boolean isDrawWeighted) {
		this.isDrawWeighted = isDrawWeighted;
	}

	public float getBrushSize() {
		return brushSize;
	}

	public void setBrushSize(float brushSize) {
		this.brushSize = brushSize;
	}

	public float getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(float epsilon) {
		this.epsilon = Math.abs(epsilon);
		if (epsilon < 1.0f) epsilon = 1.0f;
		this.epsilon = epsilon;
	}

	public float getBezierBias() {
		return bezierBias;
	}

	public void setBezierBias(float bias) {
		this.bezierBias = bias;
	}

	public ArrayList<PVector> getDragPoints() {
		return this.dragPoints;
	}

	/**
	 * If you change the value of dragPoints, immediately call calculateDerivedPoints() 
	 * to refresh all drawing objects. 
	 * 
	 * @param dragPoints	ArrayList<PVector>, a dense set of points for drawing a line
	 */
	public void setDragPoints(ArrayList<PVector> dragPoints) {
		this.dragPoints = dragPoints;
	}

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

	public ArrayList<PVector> getRdpPoints() {
		if (rdpPoints.size() < 1) {
			this.reducePoints(this.epsilon);
		}
		return rdpPoints;
	}

	/**
	 * Sets rdpPoints, generally not something you want to do: 
	 * let reducePoints() or calculateDerivedPoints() 
	 * derive rdpPoints from dragPoints instead. 
	 * 
	 * @param rdpPoints
	 */
	public void setRdpPoints(ArrayList<PVector> rdpPoints) {
		this.rdpPoints = rdpPoints;
	}
	
	public ArrayList<Integer> getRdpIndices() {
		return rdpIndices;
	}
	
	public int[] getRdpIndicesAsInts() {
		int[] inds = rdpIndices.stream().mapToInt(Integer::intValue).toArray();
		return inds;
	}

	/**
	 * Sets rdpIndices, generally not something you want to do.
	 * Call reducePoints() or calculateDerivedPoints instead
	 * and stay out of trouble. 
	 * 
	 * @param rdpIndices
	 */
	public void setRdpIndices(ArrayList<Integer> rdpIndices) {
		this.rdpIndices = rdpIndices;
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

	public PABezShape getCurveShape() {
		if (this.curveShape == null) {
			if (this.isDrawWeighted) {
				this.curveShape = PACurveUtility.calculateWeightedCurve(this.getRdpPoints(), this.bezierBias);
				//System.out.println("-- weighted curve, bias = "+ this.bezierBias);
			}
			else {
				this.curveShape = PACurveUtility.calculateCurve(this.getRdpPoints());
			}
		}
		return this.curveShape;
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

	public void setCurveShape(PABezShape curveShape) {
		this.curveShape = curveShape;
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

	public PABezShape getBrushShape() {
		if (this.brushShape == null) {
			if (this.isDrawWeighted) {
				brushShape = PACurveUtility.quickBrushShape(this.getRdpPoints(), brushSize, true, this.bezierBias);
				//System.out.println("-- weighted brush, bias = "+ this.bezierBias);
			}
			else {
				brushShape = PACurveUtility.quickBrushShape(this.getRdpPoints(), brushSize);
			}
			brushShape.setNoStroke();
			brushShape.setFillColor(brushColor);
		}
		return brushShape;
	}

	public PABezShape getWeightedBrushShape() {
		PABezShape weightedBrushShape = PACurveUtility.calculateWeightedCurve(this.getBrushShape(), this.bezierBias);
		return weightedBrushShape;
	}

	public PABezShape getWeightedBrushShape(float brushSize, float bias) {
		PABezShape weightedBrushShape = PACurveUtility.calculateWeightedCurve(this.getBrushShape(), bias);
		return weightedBrushShape;
	}

	public void setBrushShape(PABezShape brushShape) {
		this.brushShape = brushShape;
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

	public ArrayList<PVector> getBrushPoly() {
		if (this.brushPoly == null) {
			this.brushPoly = this.getBrushShape().getPointList(polySteps);
		}
		return brushPoly;
	}
	
	public int getPolySteps() {
		return polySteps;
	}

	public void setPolySteps(int polySteps) {
		this.polySteps = polySteps;
	}

	public ArrayList<PVector> getEventPoints() {
		if (this.eventPoints == null) {
			eventPoints = this.getCurveShape().getPointList(this.eventSteps);			
		}
		return eventPoints;
	}

	public ArrayList<PVector> getEventPoints(int eventSteps) {
		return this.getCurveShape().getPointList(eventSteps);
	}

	public void setEventPoints(ArrayList<PVector> eventPoints) {
		this.eventPoints = eventPoints;
	}

	public int getEventSteps() {
		return eventSteps;
	}

	public void setEventSteps(int eventSteps) {
		this.eventSteps = eventSteps;
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

	public int getTimeStamp() {
		return timeStamp;
	}
	
	public void setTimeStamp(int timeStamp) {
		this.timeStamp = timeStamp;
	}

	public int getTimeOffset() {
		return timeOffset;
	}

	public void setTimeOffset(int timeOffset) {
		this.timeOffset = timeOffset;
	}	

	public ArrayList<Integer> getDragTimes() {
		return dragTimes;
	}
	
	public int[] getDragTimesAsInts() {
		int[] times = dragTimes.stream().mapToInt(Integer::intValue).toArray();
		return times;
	}

	public int[] getDragOffsetsAsInts() {
		int[] times = dragTimes.subList(1, dragTimes.size()).stream().mapToInt(Integer::intValue).toArray();
		return times;
	}

	public void setDragTimes(ArrayList<Integer> dragTimes) {
		this.dragTimes = dragTimes;
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
	 * @param dragColor		color for the line that is drawn
	 */
	public void dragPointsDraw(PApplet parent, int dragColor, float dragWeight) {
		PACurveUtility.lineDraw(parent, this.dragPoints, dragColor, dragWeight);
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
	 * @param rdpColor		the color for the line that is drawn
	 * @param drawWeight	the weight of the line that is drawn
	 */
	public void reducedPointsDraw(PApplet parent, int rdpColor, float drawWeight) {
		PACurveUtility.lineDraw(parent, this.getRdpPoints(), rdpColor, drawWeight);
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
	 * @param dragColor		color for the line through the dense point set
	 * @param dragWeight	weight (in pixels) of the line through the dense point set
	 */
	public void dragPointsDraw(PGraphics pg, int dragColor, float dragWeight) {
		PACurveUtility.lineDraw(pg, this.dragPoints, dragColor, dragWeight);
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
	 * @param rdpColor		color for the line through the reduced point set
	 * @param rdpWeight		weight (in pixels) of the line through the reduced point set
	 */
	public void reducedPointsDraw(PGraphics pg, int rdpColor, float rdpWeight) {
		PACurveUtility.lineDraw(pg, this.getRdpPoints(), rdpColor, rdpWeight);
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
