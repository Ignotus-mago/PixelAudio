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
 * Test class for point reduction and curve modeling. I think PACurveUtility, 
 * with its static methods, is more useful. PACurveMaker may evolve to a storage
 * class, calling PACurveUtility for its modeling tasks.  
 */
public class PACurveMaker {
	public ArrayList<PVector> allPoints;
	public ArrayList<PVector> drawPoints;
	public PABezShape bezPoints;
	public PABezShape weightedBezPoints;
	public PABezShape brushShape;
	public float brushSize = 24.0f;
	public float epsilon = 8.0f;
	public float weight = PABezShape.LAMBDA;
	public boolean isShowBrush = false;
	public int dragColor = PABezShape.composeColor(233, 199, 89, 128);    // tan
	public int rdpColor = PABezShape.composeColor(233, 89, 144);         // red
	public int curveColor = PABezShape.composeColor(55, 199, 246);       // blue
	public int brushColor = PABezShape.composeColor(76, 199, 144, 96);   // transparent green
	private boolean isReady = false;
	public int timeStamp;
	public int timeOffset;


	
	public PACurveMaker() {
		this.allPoints = new ArrayList<PVector>();
		this.drawPoints = new ArrayList<PVector>();
	}
	
	public PACurveMaker(ArrayList<PVector> points) {
		this.allPoints = points;
		this.drawPoints = new ArrayList<PVector>();
	}
	
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
	
	public void calculateDerivedPoints() {
		reducePoints();
		bezPoints = PACurveUtility.calculateCurve(drawPoints);
		weightedBezPoints = PACurveUtility.calculateWeightedCurve(bezPoints, weight);
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

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

	public int getDragColor() {
		return dragColor;
	}

	public void setDragColor(int dragColor) {
		this.dragColor = dragColor;
	}

	public int getRdpColor() {
		return rdpColor;
	}

	public void setRdpColor(int rdpColor) {
		this.rdpColor = rdpColor;
	}

	public int getCurveColor() {
		return curveColor;
	}

	public void setCurveColor(int curveColor) {
		this.curveColor = curveColor;
	}
	
	public void setBrushShape(PABezShape brush) {
		this.brushShape = brush;
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
	
	
	public void allPointsDraw(PApplet parent, int dragColor) {
		if (allPoints.size() > 1) {
			parent.stroke(dragColor);
			parent.strokeWeight(8);
			parent.noFill();
			parent.beginShape();
			for (PVector vec : allPoints) {
				parent.vertex(vec.x, vec.y);
			}
			parent.endShape();
		}
	}

	public void allPointsDraw(PApplet parent) {
		allPointsDraw(parent, dragColor);
	}

	public void reducedPointsDraw(PApplet parent, int rdpColor) {
		if (drawPoints.size() > 1) {
			parent.stroke(rdpColor);
			parent.strokeWeight(1);
			parent.noFill();
			parent.beginShape();
			for (PVector vec : drawPoints) {
				parent.vertex(vec.x, vec.y);
			}
			parent.endShape();
		}
	}
	
	public void reducedPointsDraw(PApplet parent) {
		reducedPointsDraw(parent, rdpColor);
	}

	public void RDPDraw(PApplet parent, int dragColor, int rdpColor) {
		this.allPointsDraw(parent, dragColor);
		this.reducedPointsDraw(parent, rdpColor);
	}
	
	public void RDPDraw(PApplet parent) {
		this.RDPDraw(parent, dragColor, rdpColor);
	}
	
	
	public void allPointsDraw(PGraphics pix, int dragColor) {
		if (allPoints.size() > 1) {
			pix.stroke(dragColor);
			pix.strokeWeight(8);
			pix.noFill();
			pix.beginShape();
			for (PVector vec : allPoints) {
				pix.vertex(vec.x, vec.y);
			}
			pix.endShape();
		}
	}

	public void reducedPointsDraw(PGraphics pix, int rdpColor) {
		if (drawPoints.size() > 1) {
			pix.stroke(rdpColor);
			pix.strokeWeight(1);
			pix.noFill();
			pix.beginShape();
			for (PVector vec : drawPoints) {
				pix.vertex(vec.x, vec.y);
			}
			pix.endShape();
		}
	}
	
	public void RDPDraw(PGraphics pix, int dragColor, int rdpColor) {
		this.allPointsDraw(pix, dragColor);
		this.reducedPointsDraw(pix, rdpColor);
	}

	public void RDPDraw(PGraphics pix) {
		this.RDPDraw(pix, dragColor, rdpColor);
	}
	
	public void curveDraw(PApplet parent, boolean isDrawWeighted) {
		if (null != bezPoints && bezPoints.size() > 0) {
			parent.pushStyle();
			parent.stroke(curveColor);
			parent.strokeWeight(2);
			parent.noFill();
			if (isDrawWeighted) {
				weightedBezPoints.drawQuick(parent);
			} else {
				bezPoints.drawQuick(parent);
			}
			parent.popStyle();
		}
	}

	public void curveDraw(PGraphics pix, boolean isDrawWeighted) {
		if (null != bezPoints && bezPoints.size() > 0) {
			pix.pushStyle();
			pix.stroke(curveColor);
			pix.strokeWeight(2);
			pix.noFill();
			if (isDrawWeighted) {
				weightedBezPoints.drawQuick(pix);
			} else {
				bezPoints.drawQuick(pix);
			}
			pix.popStyle();
		}
	}

	
	public void brushDraw(PApplet parent) {
		if (null != brushShape) {
			brushShape.draw(parent);
		}
	}	

	public void brushDraw(PGraphics pix) {
		if (null != brushShape) {
			brushShape.draw(pix);
		}
	}	

	public void brushDraw(PApplet parent, int brushColor) {
		if (null != brushShape) {
			brushShape.setFillColor(brushColor);
			brushShape.draw(parent);
		}
	}	

	public void brushDraw(PGraphics pix, int brushColor) {
		if (null != brushShape) {
			brushShape.setFillColor(brushColor);
			brushShape.draw(pix);
		}
	}	

	
	public ArrayList<PVector> polyPointsDraw(PApplet parent, int steps, int pointColor, int pointSize) {
		if (bezPoints == null || bezPoints.size() < 2) return null;
		// PApplet.println("poly");
		ArrayList<PVector> poly = bezPoints.getPointList(parent, steps);
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
		if (bezPoints == null || bezPoints.size() < 2) return null;
		// PApplet.println("poly");
		ArrayList<PVector> poly = bezPoints.getPointList(pix, steps);
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
