/**
 * 
 */
package net.paulhertz.pixelaudio.curves;

import java.util.ArrayList;
import java.util.ListIterator;

import processing.core.PVector;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * Storage and utility class for point reduction, curve modeling and drawing.
 * Makes extensive use of PACurveUtility's static methods.
 * 
 */
public class PACurveMaker {
	/** List of all the points that need to be reduced */
	public ArrayList<PVector> allPoints;
	/** The reduced points delivered by the RDP algoritnm */
	public ArrayList<PVector> drawPoints;
	/** An ArrayList of PABezShapes representing a continuous curved line */
	public PABezShape curveShape;
	/** A different version of the bezPoints, where control points are adjusted by a weighting parameter */
	public PABezShape weightedCurveShape;
	/** A simulated brush stroke */
	public PABezShape brushShape;
	/** The distance in pixels of the edges of the brush stroke from the central generating curve  */
	public float brushSize = 24.0f;
	/** A parameter to control the amount of reduction in the RDP algorithm */
	public float epsilon = 8.0f;
	/** A weight applied to calculations of the control points for Bezier curves */
	public float bezWeight = PABezShape.LAMBDA;
	/** Do the brush be visible? */
	public boolean isShowBrush = false;
	/** Color for lines dragged by the mouse, stored in allPoints */
	public int dragColor = PABezShape.composeColor(233, 199, 89, 128);   // tan
	public float dragWeight = 8;
	/** Color for points reduced by RDP algorithm, stored in drawPoints */
	public int rdpColor = PABezShape.composeColor(233, 89, 144);         // red 
	public float rdpWeight = 2;
	/** Color for Bezier curve derived from drawPoints, stored in bezPoints or weightedBezPoints */
	public int curveColor = PABezShape.composeColor(55, 199, 246);       // blue
	public float curveWeight = 4;
	/** Color for simulated brush stroke, stored in brushShape */
	public int brushColor = PABezShape.composeColor(76, 199, 144, 96);   // transparent green
	/** Color for highlighted brush stroke, stored in brushShape */
	public int activeBrushColor = PABezShape.composeColor(199, 89, 55, 192);   // transparent brick red
	public float brushWeight = 0;
	/** Flag to indicate that allPoints, drawPoints, bezPoints, and brushShape have been initialized */
	private boolean isReady = false;
	/** Time when CurveMaker instance was initialized by mousePressed event, 
	 * in milliseconds since application startup */
	public int timeStamp;
	/** Time of mouseReleased event, in milliseconds elapsed since timeStamp */
	public int timeOffset;


		
	private PACurveMaker(ArrayList<PVector> points) {
		this.allPoints = points;
		this.drawPoints = new ArrayList<PVector>();
	}
	
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
	
	public static PACurveMaker buildCurveMaker(ArrayList<PVector> points) {
		PACurveMaker curveMaker = new PACurveMaker(points);
		curveMaker.calculateDerivedPoints();
		return curveMaker;
	}
	
	public static PACurveMaker buildCurveMaker(ArrayList<PVector> points, int dragColor, float dragWeight, int rdpColor, float rdpWeight, 
            								   int curveColor, float curveWeight, int brushColor, float brushWeight, int activeBrushColor) {
		PACurveMaker curveMaker = new PACurveMaker(points);
		curveMaker.setDrawingProperties(dragColor, dragWeight, rdpColor, rdpWeight, curveColor, curveWeight, brushColor, brushWeight, activeBrushColor);
		curveMaker.calculateDerivedPoints();
		return curveMaker;
	}
	
	/**
	 * Takes the list of points in allPoints and generates a reduced list in drawPoints.
	 */
	public void reducePoints() {
		drawPoints.clear();
		int total = allPoints.size();
		PVector start = allPoints.get(0);
		PVector end = allPoints.get(total - 1);
		drawPoints.add(start);
		// rdp reduces allPoints and puts the result into drawPoints
		PACurveUtility.rdp(0, total - 1, allPoints, drawPoints, epsilon);
		// put in a midpoint when there are only two points in the reduced points
		if (drawPoints.size() == 1) {
			PVector midPoint = start.copy().add(end).div(2.0f);
			drawPoints.add(midPoint);
		}
		drawPoints.add(end);
	}
	
	/**
	 * Calculates drawPoints, bezPoints, weightedBezPoints 
	 * and brushShape and then sets isReady to true.
	 */
	public void calculateDerivedPoints() {
		reducePoints();
		curveShape = PACurveUtility.calculateCurve(drawPoints);
		weightedCurveShape = PACurveUtility.calculateWeightedCurve(curveShape, bezWeight);
		brushShape = PACurveUtility.quickBrushShape(drawPoints, brushSize);
		brushShape.setNoStroke();
		brushShape.setFillColor(brushColor);
		isReady = true;
	}
	
	public boolean isReady() {
		return this.isReady;
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
		this.epsilon = epsilon;
	}

	public float getBezWeight() {
		return bezWeight;
	}

	public void setBezWeight(float weight) {
		this.bezWeight = weight;
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

	public PABezShape getBrushShape() {
		return brushShape;
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

	/************************************************
	 *                                              *
	 * ------------- DRAWING METHODS -------------  *
	 *                                              *
	 ************************************************/
	
	
	// ------------- PApplet drawing methods for allPoints and drawPoints ------------- //

	/**
	 * Draws a line using the PVector data in allPoints and a supplied color.
	 * 
	 * @param parent		a PApplet where drawing takes place
	 * @param dragColor		color for the line that is drawn
	 */
	public void allPointsDraw(PApplet parent, int dragColor, float dragWeight) {
		PACurveUtility.pointsDraw(parent, this.allPoints, dragColor, dragWeight);
	}

	/**
	 * Draws a line using the PVector data in allPoints and the current values
	 * of this.dragColor and this.dragWeight.
	 * 
	 * @param parent		a PApplet where drawing takes place
	 */
	public void allPointsDraw(PApplet parent) {
		allPointsDraw(parent, this.dragColor, this.dragWeight);
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
		PACurveUtility.pointsDraw(parent, drawPoints, rdpColor, drawWeight);
	}
	
	/**
	 * Draws a line using the PVector data in drawPoints and the current values 
	 * of this.rdpColor and this.rdpWeight.
	 * 
	 * @param parent		a PApplet where drawing takes place
	 */
	public void reducedPointsDraw(PApplet parent) {
		reducedPointsDraw(parent, this.rdpColor, this.rdpWeight);
	}

	/**
	 * Draws both the dense and reduced point sets, allPoints and drawPoints
	 * as lines using supplied colors.
	 * 
	 * @param parent		a PApplet where drawing takes place
	 * @param dragColor		color for the line through the dense point set
	 * @param dragWeight	weight (in pixels) of the line through the dense point set
	 * @param rdpColor		color for the line through the reduced point set
	 * @param rdpWeight		weight (in pixels) of the line through the reduced point set
	 */
	public void RDPDraw(PApplet parent, int dragColor, float dragWeight, int rdpColor, float rdpWeight) {
		this.allPointsDraw(parent, dragColor, dragWeight);
		this.reducedPointsDraw(parent, rdpColor, rdpWeight);
	}
	
	/**
	 * Draws both the dense and reduced point sets, allPoints and drawPoints
	 * as lines of different colors using local properties this.dragColor, 
	 * this.dragWeight, this.rdpColor and this.rdpWeight.
	 * 
	 * @param parent		a PApplet where drawing takes place
	 */
	public void RDPDraw(PApplet parent) {
		this.allPointsDraw(parent);
		this.RDPDraw(parent);
	}
	
	// ------------- PGraphics drawing methods for allPoints and drawPoints ------------- //
	
	/**
	 * Draws allPoints to a PGraphics using supplied color and weight.
	 * 
	 * @param pix			a PGraphics instance
	 * @param dragColor		color for the line through the dense point set
	 * @param dragWeight	weight (in pixels) of the line through the dense point set
	 */
	public void allPointsDraw(PGraphics pix, int dragColor, float dragWeight) {
		PACurveUtility.pointsDraw(pix, this.allPoints, dragColor, dragWeight);
	}
	
	/**
	 * Draws allPoints to a PGraphics using local color and weight, this.dragColor and this.dragWeight.
	 * 
	 * @param pix			a PGraphics instance
	 */
	public void allPointsDraw(PGraphics pix) {
		PACurveUtility.pointsDraw(pix, this.allPoints, this.dragColor, this.dragWeight);
	}

	/**
	 * Draws drawPoints to a PGraphics using supplied color and weight.
	 * 
	 * @param pix			a PGraphics instance
	 * @param rdpColor		color for the line through the reduced point set
	 * @param rdpWeight		weight (in pixels) of the line through the reduced point set
	 */
	public void reducedPointsDraw(PGraphics pix, int rdpColor, float rdpWeight) {
		PACurveUtility.pointsDraw(pix, this.drawPoints, rdpColor, rdpWeight);
	}
	
	/**
	 * Draws drawPoints to a PGraphics using current values of this.rdpColor and this.rdpWeight.
	 * 
	 * @param pix	a PGraphics instance
	 */
	public void reducedPointsDraw(PGraphics pix) {
		PACurveUtility.pointsDraw(pix, this.drawPoints, this.rdpColor, this.rdpWeight);
	}

	
	/**
	 * Draws allPoints and rdpPoints using values of supplied parameters.
	 * 
	 * @param pix
	 * @param dragColor		color for the line through the dense point set
	 * @param dragWeight	weight (in pixels) of the line through the dense point set
	 * @param rdpColor		color for the line through the reduced point set
	 * @param rdpWeight		weight (in pixels) of the line through the reduced point set
	 */
	public void RDPDraw(PGraphics pix, int dragColor, float dragWeight, int rdpColor, float rdpWeight) {
		this.allPointsDraw(pix, dragColor, dragWeight);
		this.reducedPointsDraw(pix, rdpColor, rdpWeight);
	}

	/**
	 * Draws allPoints and rdpPoints using current values of this.dragColor, 
	 * this.dragWeight, this.rdpColor and this.rdpWeight.
	 * 
	 * @param pix	a PGraphics instance
	 */
	public void RDPDraw(PGraphics pix) {
		this.allPointsDraw(pix);
		this.reducedPointsDraw(pix);
	}
		
	
	// ------------- Drawing methods for curveShape and brushShape ------------- //
	

	/**
	 * Draws a Bezier curve using the 2D Bezier curve data in this.curveShape and the current
	 * values of this.curveColor and this.curveWeight. The resulting curve will have no fill.
	 * 
	 * @param parent			a PApplet instance
	 * @param isDrawWeighted	if true, use the version of the curve with 
	 *                          control points adjusted using this.bezWeight.
	 */
	public void curveDraw(PApplet parent, boolean isDrawWeighted) {
		if (isDrawWeighted) {
			PACurveUtility.curveDraw(parent, this.curveShape, this.curveColor, this.curveWeight);
		}
		else {
			PACurveUtility.curveDraw(parent, this.weightedCurveShape, this.curveColor, this.curveWeight);
		}
	}

	/**
	 * Draws a Bezier curve using local drawing properties of this.curveShape. 
	 * If the drawing properties use a fill, the resulting curve will have a fill.
	 * 
	 * @param parent			a PApplet instance
	 * @param isDrawWeighted	if true, use the version of the curve with 
	 *                          control points adjusted using this.bezWeight.
	 */
	public void curveDrawDirect(PApplet parent, boolean isDrawWeighted) {
		if (isDrawWeighted) {
			PACurveUtility.curveDraw(parent, this.curveShape);
		}
		else {
			PACurveUtility.curveDraw(parent, this.weightedCurveShape);
		}
	}

	/**
	 * Draws a Bezier curve using the 2D Bezier curve data in this.curveShape and the current
	 * values of this.curveColor and this.curveWeight. The resulting curve will have no fill.
	 * 
	 * @param pix				a PGraphics instance
	 * @param isDrawWeighted	if true, use the version of the curve with 
	 *                          control points adjusted using this.bezWeight.
	 */
	public void curveDraw(PGraphics pix, boolean isDrawWeighted) {
		if (isDrawWeighted) {
			PACurveUtility.curveDraw(pix, this.weightedCurveShape, this.curveColor, this.curveWeight);
		}
		else {
			PACurveUtility.curveDraw(pix, this.curveShape, this.curveColor, this.curveWeight);
		}
	}

	/**
	 * Draws a Bezier curve using local drawing properties of this.curveShape. 
	 * If the drawing properties use a fill, the resulting curve will have a fill.
	 * 
	 * @param pix				a PGraphics instance
	 * @param isDrawWeighted	if true, use the version of the curve with 
	 *                          control points adjusted using this.bezWeight.
	 */
	public void curveDrawDirect(PGraphics pix, boolean isDrawWeighted) {
		if (isDrawWeighted) {
			PACurveUtility.curveDraw(pix, this.weightedCurveShape);
		}
		else {
			PACurveUtility.curveDraw(pix, this.curveShape);
		}
	}
	
	public void brushDraw(PApplet parent, int brushColor, int strokeColor, float brushWeight) {
		PACurveUtility.shapeDraw(parent, this.brushShape, brushColor, strokeColor, brushWeight);
	}

	public void brushDraw(PApplet parent) {
		PACurveUtility.shapeDraw(parent, this.brushShape, this.brushColor, this.brushColor, this.brushWeight);
	}

	public void brushDrawDirect(PApplet parent) {
		PACurveUtility.shapeDraw(parent, this.brushShape);
	}	

	public void brushDraw(PGraphics pix, int brushColor, int strokeColor, float brushWeight) {
		PACurveUtility.shapeDraw(pix, this.brushShape, brushColor, strokeColor, brushWeight);
	}
	
	public void brushDraw(PGraphics pix) {
		PACurveUtility.shapeDraw(pix, this.brushShape, this.brushColor, this.brushColor, this.brushWeight);
	}
	
	public void brushDrawDirect(PGraphics pix) {
		PACurveUtility.shapeDraw(pix, this.brushShape);
	}	
	

	
	// ------------- Drawing methods for points in polygon representation of curveShape ------------- //
	
	public ArrayList<PVector> polyPointsDraw(PApplet parent, int steps, int pointColor, int pointSize) {
		if (curveShape == null || curveShape.size() < 2) return null;
		// PApplet.println("poly");
		ArrayList<PVector> poly = curveShape.getPointList(parent, steps);
		parent.pushStyle();
		parent.noStroke();
		parent.fill(pointColor);
		for (PVector v : poly) {
			parent.circle(v.x, v.y, pointSize);
		}
		parent.popStyle();
		return poly;
	}

	public ArrayList<PVector> polyPointsDraw(PGraphics pix, int steps, int pointColor, int pointSize) {
		if (curveShape == null || curveShape.size() < 2) return null;
		// PApplet.println("poly");
		ArrayList<PVector> poly = curveShape.getPointList(pix, steps);
		pix.pushStyle();
		pix.noStroke();
		pix.fill(pointColor);
		for (PVector v : poly) {
			pix.circle(v.x, v.y, pointSize);
		}
		pix.popStyle();
		return poly;
	}
	
	
	
}
