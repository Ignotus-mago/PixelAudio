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


import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Class to store a path composed of lines and Bezier curves, along with fill, stroke, weight and opacity values.
 * Adapted from IgnoCodeLib Processing library for use with PixelAudio library.
 *
 * IgnoCodeLib is available in the Processing contributed libraries and at
 * https://paulhertz.net/ignocodelib/ and https://github.com/Ignotus-mago/IgnoCodeLib3
 * 
 * TODO add write methods to save curves to a (JSON) file, create (JSON) file reader.
 *
 */
public class PABezShape {
  /** initial x-coordinate */
  float x;
  /** initial y-coordinate */
  float y;
  /** list of bezier vertices */
  private ArrayList<PAVertex2DINF> curves;
  /** flags if shape is closed or not */
  boolean isClosed = false;
  /** flags we should draw control points and vertices */
  boolean isMarked = false;
  /** flag for line segment type, associated with LineVertex */
  public final static int LINE_SEGMENT = 1;
  /** flag for curve segment type, associated with PABezVertex */
  public final static int CURVE_SEGMENT = 2;
  /* ----- COLOR FILL AND STROKE PROPERTIES ----- */
  /** flags if shape is filled or not */
  boolean hasFill;
  /** flags if shape has a stroke or not */
  boolean hasStroke;
  /** fill color for shape */
  int fillColor;
  /** stroke color for shape */
  int strokeColor;
  /** stroke weight for shape */
  float weight;
  /** left, top, right, bottom bounding rectangle */
  float[] bounds;
  
  
	/**
	 * <p>
	 * In the approximation of a circle by Bezier curves, <br>
	 * KAPPA = (distance between Bezier anchor and its associated control point) / (circle radius),<br> 
	 * when a circle is divided into 4 sectors of 90 degrees.<br>
	 * KAPPA = 4 * (√2 - 1) / 3 = 0.5522847498</p>
	 * @see <a href="http://www.whizkidtech.redprince.net/bezier/circle/kappa/">http://www.whizkidtech.redprince.net/bezier/circle/kappa/</a>
	 */
	public final static double KAPPA = 0.5522847498;
	/**
	 * A value for weighting Bezier splines based on the length of line segments between 
	 * anchor points, derived from the ratio of the chord of a quarter circle to KAPPA. </br>
	 * LAMBDA = KAPPA * (1/√2) = 0.39052429175f
	 *
	 */
	public final static float LAMBDA = 0.39052429175f;



  /**
   * Creates a BezShape with initial point x,y, closed or open according to the value of isClosed.
   * Fill, stroke, and weight of shapes are set from their values in the Processing environment.
   * The Processing transformation matrix (set by calls to rotate, translate and scale) is saved
   * to the instance variable <code>ctm</code>, but no transform is performed.  Note that drawing
   * is affected by the current Processing transform.
   *  
   * @param x    x-coordinate of initial point
   * @param y    y-coordinate of initial point
   * @param isClosed   true if shape is closed, false if it is open
   */
  public PABezShape(float x, float y, boolean isClosed) {
    this.setStartPoint(x, y);
    this.curves = new ArrayList<PAVertex2DINF>();
    this.isClosed = isClosed;
  }
  
  
  /**
   * Returns the x-coordinate of the initial point of the geometry of this shape.
   * @return   x-coordinate of initial vertex
   */
  public float x() {
    return x;
  }
  /**
   * Sets the x-coordinate of the initial point of the geometry of this shape.
   * @param newX   new x-coordinate of initial vertex
   */
  public void setX(float newX) {
    x = newX;
  }


  /**
   * Returns the y-coordinate of the initial point of the geometry of this shape.
   * @return   y-coordinate of initial vertex
   */
  public float y() {
    return y;
  }
  /**
   * Sets the y-coordinate of the initial point of the geometry of this shape.
   * @param newY   new y-coordinate of initial vertex
   */
  public void setY(float newY) {
    y = newY;
  }


  /**
   * @return   a PALineVertex with start point coordinates of this shape 
   */
  public PALineVertex startVertex() {
    return new PALineVertex(this.x, this.y);
  }
  
  /**
   * Sets a new initial vertex for this BezShape.
   * @param newX
   * @param newY
   */
  public void setStartPoint(float newX, float newY) {
    this.setX(newX); 
    this.setY(newY); 
  }


  /**
   * @return  {@code true} if this shape is closed, {@code false} otherwise.
   */
  public boolean isClosed() {
    return isClosed;
  }
  /**
   * @param newIsClosed   {@code true} if this shape is closed, {@code false} otherwise
   */
  public void setIsClosed(boolean newIsClosed) {
    isClosed = newIsClosed;
  }
  
  /**
   * @return  {@code true} if this shape is marked with vertices and control points, {@code false} otherwise.
   */
  public boolean isMarked() {
    return isMarked;
  }
  /**
   * @param newIsMarked   {@code true} if this shape is marked with vertices and control points, {@code false} otherwise
   */
  public void setIsMarked(boolean newIsMarked) {
    isMarked = newIsMarked;
  }

  /*-------------------------------------------------------------------------------------------*/
  /*                                                                                           */
  /* METHODS TO SET COLOR FILL, STROKE, AND WEIGHT                                             */ 
  /*                                                                                           */
  /*-------------------------------------------------------------------------------------------*/

  /**
   * @return   true if this component is filled, false otherwise
   */
  public boolean hasFill() {
    return hasFill;
  }
  /**
   * @param newHasFill   pass true if this shape has a fill, false otherwise. Note that
   * the current fillColor will not be discarded by setting hasFill to false: the shape
   * simply won't display or save to file with a fill. 
   */
  public void setHasFill(boolean newHasFill) {
    hasFill = newHasFill;
  }
  /**
   * Marks this component as having no fill.
   * Equivalent to setHasFill(false), if the implementor provides a setHasFill method
   */
  public void setNoFill() {
    setHasFill(false);
  }

  /**
   * @return   true if this shape is stroked, false otherwise
   */
  public boolean hasStroke() {
    return hasStroke;
  }
  /**
   * @param newHasStroke   pass true if this shape has a stroke, false otherwise. Note that
   * the current strokeColor will not be discarded by setting hasStroke to false: the shape
   * simply won't display or save to file with a stroke.
   */
  public void setHasStroke(boolean newHasStroke) {
    hasStroke = newHasStroke;
  }
  /**
   * Marks this component as having no stroke
   * Equivalent to setHasStroke(false), if the implementor provides a setHasStroke method
   */
  public void setNoStroke() {
    setHasStroke(false);
  }
  
  /**
   * @return the current fill color
   */
  public int fillColor() {
    return fillColor;
  }
  /**
   * @param newFillColor   a Processing color (32-bit int with ARGB bytes).
   */
  public void setFillColor(int newFillColor) {
    fillColor = newFillColor;
    setHasFill(true);
  }

  /**
   * @return the current stroke color
   */
  public int strokeColor() {
    return this.strokeColor;
  }
  /**
   * @param newStrokeColor   a Processing color (32-bit int with ARGB bytes).
   */
  public void setStrokeColor(int newStrokeColor) {
    this.strokeColor = newStrokeColor;
    this.setHasStroke(true);
  }

  /**
   * Sets opacity of current fill color.
   * @param opacity   a number in the range 0..255. Value is not checked!
   */
  public void setFillOpacity(int opacity) {
    int[] argb = argbComponents(this.fillColor);
    this.setFillColor(composeColor(argb[1], argb[2], argb[3], opacity));
  }
  /**
   * @return   the opacity value of the current fill color
   */
  public int fillOpacity() {
    int[] argb = argbComponents(this.fillColor);
    return argb[0];
  }
  
  /**
   * Sets opacity of current stroke color.
   * @param opacity   a number in the range 0..255. Value is not checked!
   */
  public void setStrokeOpacity(int opacity) {
    int[] argb = argbComponents(this.strokeColor);
    this.setStrokeColor(composeColor(argb[1], argb[2], argb[3], opacity));
  }
  /**
   * @return   the opacity value of the current stroke color
   */
  public int strokeOpacity() {
    int[] argb = argbComponents(this.strokeColor);
    return argb[0];
  }
  
  /**
   * Returns the current weight (in points) of stroked lines.
   * @return the current weight (in points) of stroked lines. One point = one pixel on screen.
   */
  public float weight() {
    return weight;
  }
  /**
   * @param newWeight the new weight of stroked lines.
   */
  public void setWeight(float newWeight) {
    weight = newWeight;
  }

  /*-------------------------------------------------------------------*/
  /*                                                                   */
  /*                          COLOR UTILITIES                          */ 
  /*                                                                   */
  /*-------------------------------------------------------------------*/
  
  // TODO These color utilities are duplicated in PixelAudioMapper. 
  // They are useful here if we want to pull out the pixelaudio.curves package
  // for use in other contexts. 
  
  
  /**
   * Breaks a Processing color into R, G and B values in an array.
   * @param rgb    a Processing color as a 32-bit integer 
   * @return       an array of 3 integers in the intRange 0..255 for 3 primary color components: {R, G, B}
   */
  public static int[] rgbComponents(int rgb) {
    int[] comp = new int[3];
    comp[0] = (rgb >> 16) & 0xFF;  // Faster way of getting red(argb)
    comp[1] = (rgb >> 8) & 0xFF;   // Faster way of getting green(argb)
    comp[2] = rgb & 0xFF;          // Faster way of getting blue(argb)
    return comp;
  }

	/**
	 * Breaks a Processing color into A, R, G and B values in an array.
	 * 
	 * @param argb a Processing color as a 32-bit integer
	 * @return an array of 4 integers in the range 0..255 for each color component:
	 *         {A, R, G, B}
	 */
	public static int[] argbComponents(int argb) {
		int[] comp = new int[4];
		comp[0] = (argb >> 24) & 0xFF; // alpha
		comp[1] = (argb >> 16) & 0xFF; // Faster way of getting red(argb)
		comp[2] = (argb >> 8) & 0xFF; // Faster way of getting green(argb)
		comp[3] = argb & 0xFF; // Faster way of getting blue(argb)
		return comp;
	}

	/**
	 * Breaks a Processing color into R, G, B and A values in an array.
	 *
	 * @param argb a Processing color as a 32-bit integer
	 * @return an array of integers in the intRange [0, 255] for 3 primary color
	 *         components: {R, G, B} plus alpha
	 */
	public static int[] rgbaComponents(int argb) {
		int[] comp = new int[4];
		comp[0] = (argb >> 16) & 0xFF; // Faster way of getting red(argb)
		comp[1] = (argb >> 8) & 0xFF; // Faster way of getting green(argb)
		comp[2] = argb & 0xFF; // Faster way of getting blue(argb)
		comp[3] = argb >> 24 & 0xFF; // alpha component
		return comp;
	}

	/**
	 * Creates an opaque Processing RGB color from r, g, b values. Note the order
	 * of arguments, the same as the Processing color(value1, value2, value3) method.
	 *
	 * @param r red component [0, 255]
	 * @param g green component [0, 255]
	 * @param b blue component [0, 255]
	 * @return a 32-bit integer with bytes in Processing format ARGB.
	 */
	public static int composeColor(int r, int g, int b) {
		return 255 << 24 | r << 16 | g << 8 | b;
	}

	/**
	 * Creates a Processing ARGB color from r, g, b, and alpha channel values. Note the order
	 * of arguments, the same as the Processing color(value1, value2, value3, alpha) method.
	 *
	 * @param r red component [0, 255]
	 * @param g green component [0, 255]
	 * @param b blue component [0, 255]
	 * @param a alpha component [0, 255]
	 * @return a 32-bit integer with bytes in Processing format ARGB.
	 */
	public static final int composeColor(int r, int g, int b, int a) {
		return a << 24 | r << 16 | g << 8 | b;
	}

	/**
	 * Creates a Processing ARGB color from r, g, b, values in an array.
	 *
	 * @param comp 	array of 3 integers in range [0, 255], for red, green and blue
	 *             	components of color alpha value is assumed to be 255
	 * @return a 32-bit integer with bytes in Processing format ARGB.
	 */
	public static int composeColor(int[] comp) {
		return 255 << 24 | comp[0] << 16 | comp[1] << 8 | comp[2];
	}

	/**
	 * Creates a Processing ARGB color from a grayscale value. Alpha is set to 255.
	 * 
	 * @param gray a grayscale value 0..255
	 * @return an int compatible with a Processing color
	 */
	public static int composeColor(int gray) {
		return 255 << 24 | gray << 16 | gray << 8 | gray;
	}

  /*-------------------------------------------------------------------------*/
  /*                                                                         */
  /*          METHODS TO APPEND, ACCESS AND ITERATE THROUGH CURVES           */ 
  /*                                                                         */
  /*-------------------------------------------------------------------------*/

  /**
   * Appends a Vertex2DINF to this BezShape
   * @param vt   a PauVertex2DINF (line segment or curve segment)
   */
  public void append(PAVertex2DINF vt) {
    curves.add(vt);
  }

  /**
   * Appends a PABezVertex (cubic Bezier segment) to this BezShape.
   * @param cx1   x-coordinate of first control point 
   * @param cy1   y-coordinate of first control point
   * @param cx2   x-coordinate of second control point
   * @param cy2   y-coordinate of second control point
   * @param x     x-coordinate of terminal anchor point
   * @param y     y-coordinate of terminal anchor point
   */
  public void append(float cx1, float cy1, float cx2, float cy2, float x, float y) {
    this.append(new PABezVertex(cx1, cy1, cx2, cy2, x, y));
  }


  /**
   * Appends a LineVertex (line segment) to this BezShape.
   * @param x
   * @param y
   */
  public void append(float x, float y) {
    this.append(new PALineVertex(x, y));
  }
  
  /**
   * Returns an iterator over the geometry of this shape. Preferred method for accessing geometry.
   * Does not include the initial point, call x() and y() or  startVertex() for that. 
   * @return an iterator over the PAVertex2DINF segments that comprise the geometry of this shape
   */
  public ListIterator <PAVertex2DINF> curveIterator() {
    return curves.listIterator();
  }
  
  /**
   * Returns size of number of vertices (PABezVertex and LineVertex) in curves.
   * @return size of curves ArrayList.
   */
  public int size() {
    return curves.size();
  }
  
  /**
   * Returns number of points (anchor points and control points) in curves.
   * Doesn't count the start point.
   * @return total number of points in curves ArrayList data.
   */
  public int pointCount() {
    int count = 0;
    ListIterator<PAVertex2DINF> it = curveIterator();
    while (it.hasNext()) {
      PAVertex2DINF bez = it.next();
      if (bez.segmentType() == CURVE_SEGMENT) {
        count += 3;
      }
      else if (bez.segmentType() == LINE_SEGMENT) {
        count += 1;
      }
      else {
        // error! should never arrive here
      }
    }
    return count;
  }
  
  
  /**
   * Provides access to the list of vertices maintained by PABezShape.
   * 
   * @return the list of vertices maintained by PABezShape
   */
  public ArrayList<PAVertex2DINF> getCurves() {
	  return this.curves;
  }

  /**
   * Creates a deep copy of this BezShape.
   * @see java.lang.Object#clone
   */
  public PABezShape clone() {
    PABezShape cloneShape = new PABezShape(this.x, this.y, false);
    cloneShape.setIsClosed(this.isClosed());
    ListIterator<PAVertex2DINF> it = curveIterator();
    while (it.hasNext()) {
      PAVertex2DINF bez = it.next();
      cloneShape.append(bez.clone());
    }
    return cloneShape;
  }


  /*-------------------------------------------------------------------------------------------*/
  /*                                                                                           */
  /*                               METHODS TO DRAW TO DISPLAY                                  */ 
  /*                                                                                           */
  /*-------------------------------------------------------------------------------------------*/


  /** 
   * Draws this shape to the display. Calls beginShape and endShape on its own.
   * Uses current fill, stroke and weight from Processing environment.
   */
  public void drawQuick(PApplet parent) {
    parent.beginShape();
    // equivalent to startPoint.draw(this.parent);
    parent.vertex(this.x, this.y);
    ListIterator<PAVertex2DINF> it = curveIterator();
    int i = 0;
    while (it.hasNext()) {
      PAVertex2DINF bez = it.next();
      bez.draw(parent); 
      if (isMarked) {
         if (bez.segmentType() == CURVE_SEGMENT) {
          parent.pushStyle();
          parent.noFill();
          parent.stroke(192);
          parent.strokeWeight(1);
          PABezVertex bz = (PABezVertex)bez;
          if (i > 0) {
        	  parent.line(curves.get(i-1).x(), curves.get(i-1).y(), bz.cx1(), bz.cy1());
        	  parent.line(bz.x(), bz.y(), bz.cx2(), bz.cy2());
          }
          else {
            int w = 6;
            parent.pushStyle();
            parent.noStroke();
            parent.fill(160);
            parent.square(x - w/2, y - w/2, w);
            parent.popStyle();
            parent.line(x, y, bz.cx1(), bz.cy1());
            parent.line(bz.x(), bz.y(), bz.cx2(), bz.cy2());
          }
          parent.popStyle();
        }
        bez.mark(parent);
      }
      i++;
    }
    if (isClosed()) {
      parent.endShape(PApplet.CLOSE);
    }
    else {
      parent.endShape();
    }
  }
  
  /** 
   * Draws this shape to a PGraphics passed as an argument. Calls beginShape and endShape on its own.
   * Uses current fill, stroke and weight from Processing environment.
   */
  public void drawQuick(PGraphics pg) {
    pg.beginShape();
    // equivalent to startPoint.draw(this.parent);
    pg.vertex(this.x, this.y);
    ListIterator<PAVertex2DINF> it = curveIterator();
    int i = 0;
    while (it.hasNext()) {
      PAVertex2DINF bez = it.next();
      bez.draw(pg); 
      if (isMarked) {
         if (bez.segmentType() == CURVE_SEGMENT) {
          pg.pushStyle();
          pg.noFill();
          pg.stroke(192);
          pg.strokeWeight(1);
          PABezVertex bz = (PABezVertex)bez;
          if (i > 0) {
            pg.line(curves.get(i-1).x(), curves.get(i-1).y(), bz.cx1(), bz.cy1());
            pg.line(bz.x(), bz.y(), bz.cx2(), bz.cy2());
          }
          else {
            int w = 6;
            pg.pushStyle();
            pg.noStroke();
            pg.fill(160);
            pg.square(x - w/2, y - w/2, w);
            pg.popStyle();
            pg.line(x, y, bz.cx1(), bz.cy1());
            pg.line(bz.x(), bz.y(), bz.cx2(), bz.cy2());
          }
          pg.popStyle();
        }
        bez.mark(pg);
      }
      i++;
    }
    if (isClosed()) {
      pg.endShape(PApplet.CLOSE);
    }
    else {
      pg.endShape();
    }
  }

  /** 
   * Draws this shape to the display. Calls beginShape and endShape on its own.
   * If isMarked is true, will mark anchor and control points.
   */
  public void draw(PApplet parent) {
    parent.beginShape();
    if (hasFill()) {
      parent.fill(fillColor);
    }
    else {
      parent.noFill();
    }
    if (hasStroke()) {
      parent.stroke(strokeColor);
    }
    else {
      parent.noStroke();
    }
    parent.strokeWeight(weight);
    parent.vertex(this.x, this.y);
    ListIterator<PAVertex2DINF> it = curveIterator();
    int i = 0;
    while (it.hasNext()) {
      PAVertex2DINF bez = it.next();
      bez.draw(parent);
      if (isMarked) {
      if (bez.segmentType() == CURVE_SEGMENT) {
       parent.pushStyle();
       parent.noFill();
       parent.stroke(192);
       parent.strokeWeight(1);
       PABezVertex bz = (PABezVertex)bez;
       if (i > 0) {
         parent.line(curves.get(i-1).x(), curves.get(i-1).y(), bz.cx1(), bz.cy1());
         parent.line(bz.x(), bz.y(), bz.cx2(), bz.cy2());
       }
       else {
         int w = 6;
         parent.pushStyle();
         parent.noStroke();
         parent.fill(160);
         // parent.square(x - w/2, y - w/2, w);
         parent.rect(x - w/2, y - w/2, w, w);
         parent.popStyle();
         parent.line(x, y, bz.cx1(), bz.cy1());
         parent.line(bz.x(), bz.y(), bz.cx2(), bz.cy2());
       }
       parent.popStyle();
       }
       bez.mark(parent);
      }
      i++;
      }
      if (isClosed()) {
        parent.endShape(PApplet.CLOSE);
      }
      else {
        parent.endShape();
      }
  }

  /** 
   * Draws this shape to an offscreen PGraphics. Calls beginShape and endShape on its own. 
   * It's up to the user to call beginDraw() and endDraw() on the PGraphics instance.
   * If isMarked is true, draws marks for anchor and control points.
   * @param pg   a PGraphics instance
   */
  public void draw(PGraphics pg) {
     pg.beginShape();
    if (hasFill()) {
      pg.fill(fillColor);
    }
    else {
      pg.noFill();
    }
    if (hasStroke()) {
      pg.stroke(strokeColor);
    }
    else {
      pg.noStroke();
    }
    pg.strokeWeight(weight);
    // equivalent to startPoint.draw(this.parent);
    pg.vertex(this.x, this.y);
    ListIterator<PAVertex2DINF> it = curveIterator();
    int i = 0;
    while (it.hasNext()) {
      PAVertex2DINF bez = it.next();
      bez.draw(pg);
      if (isMarked) {
        if (bez.segmentType() == CURVE_SEGMENT) {
         pg.pushStyle();
         pg.noFill();
         pg.stroke(192);
         pg.strokeWeight(1);
         PABezVertex bz = (PABezVertex)bez;
         if (i > 0) {
           pg.line(curves.get(i-1).x(), curves.get(i-1).y(), bz.cx1(), bz.cy1());
           pg.line(bz.x(), bz.y(), bz.cx2(), bz.cy2());
         }
         else {
           int w = 6;
           pg.pushStyle();
           pg.noStroke();
           pg.fill(160);
           // pg.square(x - w/2, y - w/2, w);
           pg.rect(x - w/2, y - w/2, w, w);
           pg.popStyle();
           pg.line(x, y, bz.cx1(), bz.cy1());
           pg.line(bz.x(), bz.y(), bz.cx2(), bz.cy2());
         }
         pg.popStyle();
       }
       bez.mark(pg);
      }
      i++;
    }
    if (isClosed()) {
      pg.endShape(PApplet.CLOSE);
    }
    else {
      pg.endShape();
    }
  }


	/**
	 * Extracts an approximated polygon from path data, returning it as an array of PVector.
	 * We no longer require a PApplet instance to calculate the bezierPoint.
	 * 
	 * @param parent a PApplet instance
	 * @param steps  number of straight line segments to divide Bezier curves into
	 * @return ArrayList of PVector, coordinates for a polygon approximation of this shape.
	 */
  	@Deprecated
	public ArrayList<PVector> getPointList(PApplet parent, int steps) {
		return getPointList(steps);
	}

	/**
	 * Extracts an approximated polygon from path data, returning it as an array of PVector.
	 * 
	 * @param steps  number of straight line segments to divide Bezier curves into
	 * @return ArrayList of PVector, coordinates for a polygon approximation of this shape.
	 */
	public ArrayList<PVector> getPointList(int steps) {
		ArrayList<PVector> curvePoints = new ArrayList<PVector>();
		ListIterator<PAVertex2DINF> it = curves.listIterator();
		// calculate the total number of points in the result array
		// start counting points at 1, since start point (at indices 0 and 1) will begin
		// the array
		int ct = 1;
		while (it.hasNext()) {
			PAVertex2DINF vt = it.next();
			int segType = vt.segmentType();
			if (CURVE_SEGMENT == segType) {
				// divide the curve into steps lines
				ct += steps;
			} else if (LINE_SEGMENT == segType) {
				ct += 1;
			}
		}
		// each point is comprised of 2 floats
		float[] points = new float[ct * 2];
		// add the start point to the array
		int i = 0;
		points[i++] = this.x;
		points[i++] = this.y;
		// retrieve x and y values
		float currentX = points[i - 2];
		float currentY = points[i - 1];
		// reset the iterator to the beginning
		it = curves.listIterator();
		while (it.hasNext()) {
			PAVertex2DINF vt = it.next();
			int segType = vt.segmentType();
			if (CURVE_SEGMENT == segType) {
				float[] knots = vt.coords();
				float cx1 = knots[0];
				float cy1 = knots[1];
				float cx2 = knots[2];
				float cy2 = knots[3];
				float ax = knots[4];
				float ay = knots[5];
				for (int j = 1; j <= steps; j++) {
					float t = j / (float) steps;
					float segx = bezierPoint(currentX, cx1, cx2, ax, t);
					float segy = bezierPoint(currentY, cy1, cy2, ay, t);
					points[i++] = segx;
					points[i++] = segy;
				}
			} else if (LINE_SEGMENT == segType) {
				points[i++] = vt.x();
				points[i++] = vt.y();
			}
			currentX = points[i - 2];
			currentY = points[i - 1];
		}
		for (int j = 0; j < i;) {
			curvePoints.add(new PVector(points[j++], points[j++]));
		}
		return curvePoints;
	}
 
	/**
	 * Extracts an approximated polygon from path data, returning it as an array of PVector.
	 * We no longer require a PGraphics instance to calculate the bezierPoint.
	 * 
	 * @param pg 		a PGraphics instance
	 * @param steps     number of straight line segments to divide Bezier curves
	 * @return 			ArrayList of PVector, coordinates for a polygon approximation of this shape.
	 */
	@Deprecated
	public ArrayList<PVector> getPointList(PGraphics pg, int steps) {
		return getPointList(steps);
	}

  
	/**
	 * <p>Evaluates the Bezier at point t for points a, b, c, d. The parameter t varies
	 * between 0 and 1, a and d are points on the curve, and b and c are the control
	 * points. This can be done once with the x coordinates and a second time with
	 * the y coordinates to get the location of a Bézier curve at t. Evaluation takes
	 * multiple calls, once each for x and y coordinates, in two dimensions.</p>
	 * 
	 * @param a		coordinate of first point on the curve
	 * @param b 	coordinate of first control point
	 * @param c 	coordinate of second control point
	 * @param d 	coordinate of second point on the curve
	 * @param t 	value between 0 and 1
	 * @return		single coordinate value of a point on the curve
	 * 
	 * @see <a href="https://github.com/benfry/processing4/blob/main/core/src/processing/core/PGraphics.java#L3369">
	 *      https://github.com/benfry/processing4/blob/main/core/src/processing/core/PGraphics.java#L3369</a>
	 */
	public float bezierPoint(float a, float b, float c, float d, float t) {
		float t1 = 1.0f - t;
		return (a * t1 + 3 * b * t) * t1 * t1 + (3 * c * t1 + d * t) * t * t;
	}
	
	/**
	 * <p>Calculates the tangent of a point on a Bézier curve. There is a good definition of
	 * <a href="http://en.wikipedia.org/wiki/Tangent" target="new"><em>tangent</em> on Wikipedia</a>. 
	 * Evaluation takes multiple calls, once each for x and y coordinates, in two dimensions.</p>
	 *
	 * <h3>From Processing GitHub repo:</h3> <p>Code submitted by Dave Bollinger (davbol) for release 0136.</p>
	 *
	 * @param a 	coordinate of first point on the curve
	 * @param b 	coordinate of first control point
	 * @param c 	coordinate of second control point
	 * @param d 	coordinate of second point on the curve
	 * @param t 	value between 0 and 1
	 * @return		a single coordinate value of endpoint of the tangent
	 * 
	 * @see <a href="https://github.com/benfry/processing4/blob/main/core/src/processing/core/PGraphics.java#L3360">
	 * https://github.com/benfry/processing4/blob/main/core/src/processing/core/PGraphics.java#L3360</a>
	 */
	public float bezierTangent(float a, float b, float c, float d, float t) {
		return (3 * t * t * (-a + 3 * b - 3 * c + d) + 6 * t * (a - 2 * b + c) + 3 * (-a + b));
	}

	/**
	 * Calculates the boundary rectangle of this shape and returns it as an array of floats.
	 * @return	[xMin, yMin, xMax, yMax]
	 */
	protected float[] bounds() {
		ArrayList<PVector> points = this.getPointList(16);
		PVector vec = points.get(0);
		float xMin = vec.x;
		float yMin = vec.y;
		float xMax = xMin;
		float yMax = yMin;
		for (int i = 1; i < points.size(); i++) {
			vec = points.get(i);
			float x = vec.x;
			float y = vec.y;
			if (x < xMin) xMin = x;
			if (y < yMin) yMin = y;
			if (x > xMax) xMax = x;
			if (y > yMax) yMax = y;
		}
		float[] result = new float[4];
		result[0] = xMin;
		result[1] = yMin;
		result[2] = xMax;
		result[3] = yMax;
		return result;
	} 
	
	/**
	 * @return bounding rectangle of this shape as an array of floats, [xMin, yMin, xMax, yMax]
	 */
	public float[] getBounds() {
		if (bounds == null) {
			bounds = bounds();
		}
		return bounds;
	}
	
	public void setBounds(float[] newBounds) {
		this.bounds = newBounds;
	}

	public PVector getBoundsCenter() {
		float[] bounds = this.getBounds();
		float left = bounds[0];
		float top = bounds[1];
		float right = bounds[2];
		float bottom = bounds[3];
		return new PVector((right + left)/2.0f, (top + bottom)/2.0f);
	}
	

	// ------------- Some basic geometric transforms for our shape. ------------- //

	/**
	 * Translates this shape in x and y directions. 
	 * 
	 * @param xTrans
	 * @param yTrans
	 */
	public void translateShape(float xTrans, float yTrans) {
		this.setStartPoint(this.x + xTrans, this.y + yTrans);
		ListIterator<PAVertex2DINF> it = this.curveIterator();
		while (it.hasNext()) {
			PAVertex2DINF vt = it.next();
			int segType = vt.segmentType();
			if (CURVE_SEGMENT == segType) {
				PABezVertex bv = (PABezVertex) vt;
				float[] coords = bv.coords();
				coords[0] += xTrans;
				coords[1] += yTrans;
				coords[2] += xTrans;
				coords[3] += yTrans;
				coords[4] += xTrans;
				coords[5] += yTrans;
				bv.setCx1(coords[0]);
				bv.setCy1(coords[1]);
				bv.setCx2(coords[2]);
				bv.setCy2(coords[3]);
				bv.setX(coords[4]);
				bv.setY(coords[5]);
			}
			else if (LINE_SEGMENT == segType) {
				PALineVertex lv = (PALineVertex) vt;
				lv.setX(lv.x + xTrans);
				lv.setY(lv.y + yTrans);
			}
		}
		this.bounds = null;
	}
	
	/**
	 * Scales this shape around a given point. 
	 * Sets xcoords and ycoords arrays to null: they will have to be recalculated after a transform,
	 * which will be done through lazy initialization when {@code xcoords()} or {@code ycoords()} are called.
	 * 
	 * @param xScale   scaling on x-axis
	 * @param yScale   scaling on y-axis
	 */
	public void scaleShape(float xScale, float yScale, float x0, float y0) {
		this.x = (x0 + (this.x - x0) * xScale);
		this.y = (y0 + (this.y - y0) * yScale);
		ListIterator<PAVertex2DINF> it = this.curveIterator();
		while (it.hasNext()) {
			PAVertex2DINF vt = it.next();
			int segType = vt.segmentType();
			float[] coords = vt.coords();
			PVector pt;
			if (CURVE_SEGMENT == segType) {
				PABezVertex bv = (PABezVertex) vt;
				pt = scaleCoordAroundPoint(coords[0], coords[1], xScale, yScale, x0, y0);
				bv.setCx1(pt.x);
				bv.setCy1(pt.y);
				pt = scaleCoordAroundPoint(coords[2], coords[3], xScale, yScale, x0, y0);
				bv.setCx2(pt.x);
				bv.setCy2(pt.y);
				pt = scaleCoordAroundPoint(coords[4], coords[5], xScale, yScale, x0, y0);
				bv.setX(pt.x);
				bv.setY(pt.y);
			}
			else if (LINE_SEGMENT == segType) {
				PALineVertex lv = (PALineVertex) vt;
				pt = scaleCoordAroundPoint(coords[0], coords[1], xScale, yScale, x0, y0);
				lv.setX(pt.x);
				lv.setY(pt.y);
			}
		}
		this.bounds = null;
	}
	
	/**
	 * Rotates this shape around a supplied center point.
	 * 
	 * @param theta    degrees to rotate (in radians)
	 * TODO for theta very near PI, 0, or TWO_PI, insure correct rotation.  
	 */
	public void rotateShape(float xctr, float yctr, float theta) {
		PVector pt = rotateCoordAroundPoint(this.x(), this.y(), xctr, yctr, theta);
		this.setX(pt.x);
		this.setY(pt.y);
		ListIterator<PAVertex2DINF> it = this.curveIterator();
		while (it.hasNext()) {
			PAVertex2DINF vt = it.next();
			int segType = vt.segmentType();
			float[] coords = vt.coords();
			if (CURVE_SEGMENT == segType) {
				PABezVertex bv = (PABezVertex) vt;
				pt = rotateCoordAroundPoint(coords[0], coords[1], xctr, yctr, theta);
				bv.setCx1(pt.x);
				bv.setCy1(pt.y);
				pt = rotateCoordAroundPoint(coords[2], coords[3], xctr, yctr, theta);
				bv.setCx2(pt.x);
				bv.setCy2(pt.y);
				pt = rotateCoordAroundPoint(coords[4], coords[5], xctr, yctr, theta);
				bv.setX(pt.x);
				bv.setY(pt.y);
			}
			else if (LINE_SEGMENT == segType) {
				PALineVertex lv = (PALineVertex) vt;
				pt = rotateCoordAroundPoint(coords[0], coords[1], xctr, yctr, theta);
				lv.setX(pt.x);
				lv.setY(pt.y);
			}
		}
		this.bounds = null;;
	}	
	
	/*
	 * Because the points in our shapes may need to interact with specific pixel locations,
	 * we want to transform the shape itself and not just shift the local frame where it is
	 * drawn, as happens with Processing's transform commands. These global transform methods 
	 * assist us in doing that. There are better ways of doing these transforms, with matrices
	 * and perhaps with quaternions, but these methods have the benefit of clarity. 
	 *  
	 */
	
	/**
	 * Translates a point by xOffset and yOffset, returns a new point.
	 * 
	 * @param x         x coordinate of point
	 * @param y         y coordinate of point
	 * @param xOffset   distance to translate on x-xis
	 * @param yOffset   distance to translate on y-axis
	 * @return          a new translated point
	 */
	public static PVector translateCoord(float x, float y, float xOffset, float yOffset) {
		return new PVector(x + xOffset, y + yOffset);
	}

	/**
	 * Scales a point by xScale and yScale around a point (xctr, yctr), returns a new point.
	 * 
	 * @param x        x coordinate of point
	 * @param y        y coordinate of point
	 * @param xScale   scaling on x-axis
	 * @param yScale   scaling on y-axis
	 * @param xctr     x coordinate of center of transformation
	 * @param yctr     y coordinate of center of transformation
	 * @return         a new scaled point as a PVector
	 */
	public static PVector scaleCoordAroundPoint(float x, float y, float xScale, float yScale, float xctr, float yctr) {
		float xout = xctr + (x - xctr) * xScale;
		float yout = yctr + (y - yctr) * yScale;
		return new PVector(xout, yout);
	}
	
	/**
	 * Rotates a point theta radians around a point (xctr, yctr), returns a new point.
	 * rotation is counterclockwise for positive theta in Cartesian system, 
	 * clockwise in screen display coordinate system
	 * 
	 * @param x       x coordinate of point
	 * @param y       y coordinate of point
	 * @param xctr    x coordinate of center of rotation
	 * @param yctr    y coordinate of center of rotation
	 * @param theta   angle to rotate, in radians
	 * @return        a new rotated point
	 */
	public static PVector rotateCoordAroundPoint(float x, float y, float xctr, float yctr, float theta) {
		double sintheta = Math.sin(theta);
		double costheta = Math.cos(theta);
		PVector pt = translateCoord(x, y, -xctr, -yctr);
		pt.set((float)(pt.x * costheta - pt.y * sintheta), (float)(pt.x * sintheta + pt.y * costheta));
		return translateCoord(pt.x, pt.y, xctr, yctr);
	}

	/**
	 * Decides if a point is inside a polygon, returns true if it is.
	 * 
     * @param poly   an array of PVectors representing a polygon
	 * @param x      x-coordinate of point
	 * @param y      y-coordinate of point
	 * @return       true if point is in polygon, false otherwise
	 */
	public static boolean pointInPoly(ArrayList<PVector> poly, float x, float y) {
		int npol = poly.size();
		int i, j = 0;
		boolean inside = false;
		for (i = 0, j = npol-1; i < npol; j = i++) {
            PVector iVec = poly.get(i);
            PVector jVec = poly.get(j);
			if ( (((iVec.y <= y) && (y < jVec.y)) || ((jVec.y <= y) && (y < iVec.y))) &&
				 (x < (jVec.x - iVec.x) * (y - iVec.y) / (jVec.y - iVec.y) + iVec.x) ) {
                inside = !inside;
            }
		}
		return inside;
	}

}


