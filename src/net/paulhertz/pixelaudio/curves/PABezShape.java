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
  /** PApplet for callbacks to Processing drawing environment, etc. Used by constructors */
  PApplet parent;
  /** initial x-coordinate */
  float x;
  /** initial y-coordinate */
  float y;
  /** list of bezier vertices */
  private ArrayList<PAVertex2DINF> curves;
  /** flags if shape is closed or not */
  boolean isClosed = false;
  /** flags we shoud draw control points and vertices */
  boolean isMarked = false;
  /** flag for line segment type, associated with LineVertex */
  public final static int LINE_SEGMENT = 1;
  /** flag for curve segment type, associated with PauBezVertex */
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
  
  
  /** 
   *  KAPPA = (distance between Bezier anchor and its associated control point) / (circle radius)
   *  when a circle is divided into 4 sectors of 90 degrees.
   *  kappa = 4 * (√2 - 1) / 3
   *  see <a href="http://www.whizkidtech.redprince.net/bezier/circle/kappa/">http://www.whizkidtech.redprince.net/bezier/circle/kappa/</a>
   */
  public final static double KAPPA = 0.5522847498;
  /**
   * LAMBDA = KAPPA/√2, a value for weighting Bezier splines based on the length of line segments between anchor points
   * derived from the ratio of the chord of a quarter circle to KAPPA, LAMBDA = KAPPA * (1/√2)
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
   * @param parent   PApplet used for calls to the Processing environment, notably for drawing
   * @param x    x-coordinate of initial point
   * @param y    y-coordinate of initial point
   * @param isClosed   true if shape is closed, false if it is open
   */
  public PABezShape(PApplet parent, float x, float y, boolean isClosed) {
    this.parent = parent;
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
   * @return   a PauLineVertex with start point coordinates of this shape 
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

  /**
   * Breaks a Processing color into A, R, G and B values in an array.
   * @param argb   a Processing color as a 32-bit integer 
   * @return       an array of integers in the range 0..255 for each color component: {A, R, G, B}
   */
  public int[] argbComponents(int argb) {
    int[] comp = new int[4];
    comp[0] = (argb >> 24) & 0xFF;  // alpha
    comp[1] = (argb >> 16) & 0xFF;  // Faster way of getting red(argb)
    comp[2] = (argb >> 8) & 0xFF;   // Faster way of getting green(argb)
    comp[3] = argb & 0xFF;          // Faster way of getting blue(argb)
    return comp;
  }

  /**
   * Creates a Processing ARGB color from r, g, b, and alpha channel values. Note the order
   * of arguments, the same as the Processing color(value1, value2, value3, alpha) method. 
   * @param r   red component 0..255
   * @param g   green component 0..255
   * @param b   blue component 0..255
   * @param a   alpha component 0..255
   * @return    a 32-bit integer with bytes in Processing format ARGB.
   */
  public int composeColor(int r, int g, int b, int a) {
    return a << 24 | r << 16 | g << 8 | b;
  }
  /**
   * Creates a Processing ARGB color from a grayscale value. Alpha will be set to 255.
   * @param gray   a grayscale value 0..255
   * @return       an int compatible with a Processing color
   */
  public int composeColor(int gray) {
    return 255 << 24 | gray << 16 | gray << 8 | gray;
  }


  /*-------------------------------------------------------------------------------------------*/
  /*                                                                                           */
  /* METHODS TO APPEND POINTS AND ITERATE THROUGH THIS SHAPE                                   */ 
  /*                                                                                           */
  /*-------------------------------------------------------------------------------------------*/

  
  /**
   * Appends a Vertex2DINF to this BezShape
   * @param vt   a PauVertex2DINF (line segment or curve segment)
   */
  public void append(PAVertex2DINF vt) {
    curves.add(vt);
  }

  /**
   * Appends a PauBezVertex (cubic Bezier segment) to this BezShape.
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
   * @return an iterator over the PauVertex2DINF segments that comprise the geometry of this shape
   */
  public ListIterator <PAVertex2DINF> curveIterator() {
    return curves.listIterator();
  }
  
  /**
   * Returns size of number of vertices (PauBezVertex and LineVertex) in curves.
   * @return size of curves ArrayList.
   */
  public int size() {
    return curves.size();
  }
  
  /**
   * Returns number of points (anchor points and control points) in curves.
   * Dosn't count the start point.
   * @return total numbr of points in curves ArrayList data.
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
   * Creates a deep copy of this BezShape.
   * @see java.lang.Object#clone
   */
  public PABezShape clone() {
    PABezShape copyThis = new PABezShape(parent, this.x, this.y, false);
    copyThis.setIsClosed(this.isClosed());
    ListIterator<PAVertex2DINF> it = curveIterator();
    while (it.hasNext()) {
      PAVertex2DINF bez = it.next();
      copyThis.append(bez.clone());
    }
    return copyThis;
  }


  /*-------------------------------------------------------------------------------------------*/
  /*                                                                                           */
  /* METHODS TO DRAW TO DISPLAY                                                                */ 
  /*                                                                                           */
  /*-------------------------------------------------------------------------------------------*/


  /** 
   * Draws this shape to the display. Calls beginShape and endShape on its own.
   * Uses current fill, stroke and weight from Processing environment.
   */
  public void drawQuick() {
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
  public void draw() {
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
   * Extracts an approximated polygon from path data, returning it as an array of floats.
   * Rebuilds the {@code xcoords} and {@code ycoords} arrays. Polygon data is not cached, but the
   * {@code xcoords} and {@code ycoords} arrays are. You can use them to construct a polygon once 
   * they have been initialized. If, against our good advice, you munge around with
   * shape geometry, you can reset {@code xcoords} and {@code ycoords} with a call to 
   * this method, which always recalculates {@code xcoords} and {@code ycoords} and {@code boundsRect}
   * @param steps    number of straight line segments to divide Bezier curves into
   * @return         ArrayList of PVector, coordinates for a polygon approximation of this shape.
   */
  public ArrayList<PVector> getPointList(int steps) {
    ArrayList<PVector> curvePoints = new ArrayList<PVector>();

    ListIterator<PAVertex2DINF> it = curves.listIterator();
    // calculate the total number of points in the result array
    // start counting points at 1, since start point (at indices 0 and 1) will begin the array
    int ct = 1;
    while (it.hasNext()) {
      PAVertex2DINF vt = it.next();
      int segType = vt.segmentType();
      if (CURVE_SEGMENT == segType) {
        // divide the curve into steps lines
        ct += steps;
      }
      else if (LINE_SEGMENT == segType) {
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
        float ax  = knots[4];
        float ay  = knots[5];
        for (int j = 1; j <= steps; j++) {
          float t = j / (float) steps;
          float segx = parent.bezierPoint(currentX, cx1, cx2, ax, t);
          float segy = parent.bezierPoint(currentY, cy1, cy2, ay, t);
          points[i++] = segx;
          points[i++] = segy;
        }
      }
      else if (LINE_SEGMENT == segType) {
        points[i++] = vt.x();
        points[i++] = vt.y();
      }
      currentX = points[i - 2];
      currentY = points[i - 1];
    }
    for (int j = 0; j < i; ) {
      curvePoints.add(new PVector(points[j++], points[j++]));
    }
    return curvePoints;
  }

}