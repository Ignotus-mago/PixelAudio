/**
 * 
 */
package net.paulhertz.pixelaudio.curves;

import java.util.ArrayList;
import java.util.ListIterator;

import net.paulhertz.pixelaudio.PixelAudio;
import processing.core.PVector;
import processing.core.PApplet;

/**
 *  Static versions of point reduction and curve-modeling methods in PACurveMaker.
 */
public class PACurveUtility {
	/**
	 * KAPPA = (distance between Bezier anchor and its associated control point) /
	 * (circle radius) when a circle is divided into 4 sectors of 90 degrees. kappa
	 * = 4 * (√2 - 1) / 3 see <a href=
	 * "http://www.whizkidtech.redprince.net/bezier/circle/kappa/">http://www.whizkidtech.redprince.net/bezier/circle/kappa/</a>
	 */
	public final static double KAPPA = 0.5522847498;
	/**
	 * LAMBDA = KAPPA/√2, a value for weighting Bezier splines based on the length
	 * of line segments between anchor points derived from the ratio of the chord of
	 * a quarter circle to KAPPA, LAMBDA = KAPPA * (1/√2)
	 *
	 */
	public final static float LAMBDA = 0.39052429175f;

	
	/* ------------- BEGIN CODE FROM CODING TRAIN ------------- */
	/* see Coding Challenge on RDP Line Simplification at 
	 * https://thecodingtrain.com/CodingChallenges/152-rdp-algorithm.html 
	 */

	/**
	 * Ramer-Douglas-Peucker point reduction algorithm (RDP), reduces points in allPoints and 
	 * returns the result in rdpPoints.
	 * 
	 * @param startIndex	start index in allPoints (usually 0 to begin with)
	 * @param endIndex		end index in allPoints (usually allPoints.size()-1 to begin with)
	 * @param allPoints		ArrayList of dense points, for example a hand-drawn line
	 * @param rdpPoints		an empty ArrayList for accumulating reduced points
	 * @param epsilon       the expected distance between reduced points
	 */
	public static void rdp(int startIndex, int endIndex, ArrayList<PVector> allPoints, ArrayList<PVector> rdpPoints, float epsilon) {
	  int nextIndex = findFurthest(allPoints, startIndex, endIndex, epsilon);
	  if (nextIndex > 0) {
	    if (startIndex != nextIndex) {
	      rdp(startIndex, nextIndex, allPoints, rdpPoints, epsilon);
	    }
	    rdpPoints.add(allPoints.get(nextIndex));
	    if (endIndex != nextIndex) {
	      rdp(nextIndex, endIndex, allPoints, rdpPoints, epsilon);
	    }
	  }
	}

	static int findFurthest(ArrayList<PVector> points, int a, int b, float epsilon) {
	  float recordDistance = -1;
	  PVector start = points.get(a);
	  PVector end = points.get(b);
	  int furthestIndex = -1;
	  for (int i = a+1; i < b; i++) {
	    PVector currentPoint = points.get(i);
	    float d = lineDist(currentPoint, start, end);
	    if (d > recordDistance) {
	      recordDistance = d;
	      furthestIndex = i;
	    }
	  }
	  if (recordDistance > epsilon) {
	    return furthestIndex;
	  } else {
	    return -1;
	  }
	}

	static float lineDist(PVector c, PVector a, PVector b) {
	  PVector norm = scalarProjection(c, a, b);
	  return PVector.dist(c, norm);
	}

	static PVector scalarProjection(PVector p, PVector a, PVector b) {
	  PVector ap = PVector.sub(p, a);
	  PVector ab = PVector.sub(b, a);
	  ab.normalize(); // Normalize the line
	  ab.mult(ap.dot(ab));
	  PVector normalPoint = PVector.add(a, ab);
	  return normalPoint;
	}

	/* ------------- END CODE FROM CODING TRAIN ------------- */
	

	/* ------------- SOME CODE PORTED FROM http://www.particleincell.com/2012/bezier-splines/ ------------- */
	/* 
	 * There's a handy mathematical explanation of the creation of a Bezier spline out at 
	 * http://www.particleincell.com/2012/bezier-splines/ 
	 * with some details at https://en.wikipedia.org/wiki/Tridiagonal_matrix_algorithm
	 *
	 */

	public static PABezShape calculateCurve(ArrayList<PVector> framePoints) {
	  int n = framePoints.size();
	  float[] xCoords = new float[n];
	  float[] yCoords = new float[n];
	  int i = 0;
	  for (PVector vec : framePoints) {
	      xCoords[i] = vec.x;
	      yCoords[i] = vec.y;
	      i++;
	  }
	  float[] xp1 = new float[n-1];
	  float[] xp2 = new float[n-1];
	  computeControlPoints(xCoords, xp1, xp2);
	  float[] yp1 = new float[n-1];
	  float[] yp2 = new float[n-1];
	  computeControlPoints(yCoords, yp1, yp2);
	  PABezShape bez = new PABezShape(framePoints.get(0).x, framePoints.get(0).y, false);
	  for (int k = 0; k < n - 1; k++) {
	    bez.append(xp1[k], yp1[k], xp2[k], yp2[k], framePoints.get(k+1).x, framePoints.get(k+1).y);
	  }
	  return bez;
	}

	static void computeControlPoints(float[] K, float[] p1, float[] p2) {
	  int n = K.length - 1;
	  if (n <= 0) return;

	  /* rhs vector */
	  float[] a = new float[n];
	  float[] b = new float[n];
	  float[] c = new float[n];
	  float[] r = new float[n];
	  
	  /* leftmost segment */
	  a[0] = 0;
	  b[0] = 2;
	  c[0] = 1;
	  r[0] = K[0] + 2 * K[1];
	  
	  /* internal segments */
	  for (int i = 1; i < n - 1; i++) {
	    a[i] = 1;
	    b[i] = 4;
	    c[i] = 1;
	    r[i] = 4 * K[i] + 2 * K[i+1];
	  }
	      
	  /* rightmost segment */
	  a[n-1] = 2;
	  b[n-1] = 7;
	  c[n-1] = 0;
	  r[n-1] = 8 * K[n-1] + K[n];
	  
	  /* solves Ax = b with the Thomas algorithm, details at https://en.wikipedia.org/wiki/Tridiagonal_matrix_algorithm */
	  for (int i = 1; i < n; i++) {
	    float m = a[i] / b[i-1];
	    b[i] = b[i] - m * c[i - 1];
	    r[i] = r[i] - m * r[i-1];
	  }
	 
	  p1[n-1] = r[n-1] / b[n-1];
	  for (int i = n - 2; i >= 0; --i) {
	    p1[i] = (r[i] - c[i] * p1[i+1]) / b[i];
	  }
	    
	  /* we have p1, now compute p2 */
	  for (int i = 0;i  <n-1; i++) {
	    p2[i] = 2 * K[i+1] - p1[i+1];
	  }
	  
	  p2[n-1] = 0.5f * (K[n]+p1[n-1]);
	}

	/* ------------- END CODE FROM http://www.particleincell.com/2012/bezier-splines/ ------------- */
	
	/* ------------- code for weighted Bezier path ------------- */


	/**
	 * Scales the position of the curve control points on a Bezier curve by a factor
	 * determined by the length of the line between the two anchor points and a weight
	 * value, such as PACurveUtility.LAMBDA.
	 * It's most useful when a short control line segment follows a long segment. 
	 * 
	 * @param weightedBezPoints
	 * @param weight
	 */
	public static PABezShape calculateWeightedCurve(PABezShape bezPoints, float weight) {
	  PABezShape weightedBezPoints = bezPoints.clone();
	  ListIterator<PAVertex2DINF> it = weightedBezPoints.curveIterator();
	  float x1 = weightedBezPoints.startVertex().x();
	  float y1 = weightedBezPoints.startVertex().y();
	  PABezVertex bz;
	  while (it.hasNext()) {
	    PAVertex2DINF bez = it.next();
	    if (bez.segmentType() == PABezShape.CURVE_SEGMENT) {
	      bz = (PABezVertex) bez;
	      // lines from anchors to control point:
	      // (x1, y1), (bz.cx1(), bz.cy1())
	      // (bz.x(), bz.y()), (bz.cx2(), bz.cy2())
	      // distance between anchor points
	      float d = PApplet.dist(x1, y1, bz.x(), bz.y());
	      PVector cxy1 = weightedControlVec(x1, y1, bz.cx1(), bz.cy1(), weight, d);
	      bz.setCx1(cxy1.x);
	      bz.setCy1(cxy1.y);
	      PVector cxy2 = weightedControlVec(bz.x(), bz.y(), bz.cx2(), bz.cy2(), weight, d);
	      bz.setCx2(cxy2.x);
	      bz.setCy2(cxy2.y);
	      // store the first anchor point for the next iteration
	      x1 = bz.x();
	      y1 = bz.y();
	    }
	    else if (bez.segmentType() == PABezShape.LINE_SEGMENT) {
	      x1 = bez.x();
	      y1 = bez.y();
	    }
	    else {
	      // error! should never arrive here
	    }
	  }
	  return weightedBezPoints;
	}
	
	/**
	 * Scales the position of the curve control points on a Bezier curve by a factor
	 * determined by the length of the line between the two anchor points and a weight
	 * value, such as PACurveUtility.LAMBDA (the default, in this method).
	 * It's most useful when a short control line segment follows a long segment. 
	 * 
	 * @param weightedBezPoints
	 */
	public static void calculateWeightedCurve(PABezShape weightedBezPoints) {
		calculateWeightedCurve(weightedBezPoints, PACurveUtility.LAMBDA);
	}


	public static PVector weightedControlVec(float ax, float ay, float cx, float cy, float w, float d) {
	  // divide the weighted distance between anchor points by the distance from anchor point to control point
	  float t = w * d * 1/(PApplet.dist(ax, ay, cx, cy));
	  // plug into parametric line equation
	  float x = ax + (cx - ax) * t;
	  float y = ay + (cy - ay) * t;
	  return new PVector(x, y);
	}

	// ------------- CODE FOR BRUSH SHAPE ------------- //

	// simulate a brushstroke as a vector shape
	public static PABezShape quickBrushShape(ArrayList<PVector> points, float brushWidth, boolean isDrawWeighted, float weight) {
	  ArrayList<PVector> pointsLeft = new ArrayList<PVector>();
	  ArrayList<PVector> pointsRight = new ArrayList<PVector>();
	  if (!(points.size() > 0)) return null;
	  // handle the first point
	  PVector v1 = points.get(0);
	  pointsLeft.add(v1.copy());
	  pointsRight.add(v1.copy());
	  for (int i = 1; i < points.size() - 1; i++) {
	    PVector v2 = points.get(i);
	    PVector v3 = points.get(i+1);
	    // get the normals to the lines (v1, v2) and (v2, v3) at the point v2
	    PVector norm1 = normalAtPoint(v1, v2, 1, 1);
	    PVector norm2 = normalAtPoint(v2, v3, 0, 1);
	    // add the normals together and take the average
	    norm1.add(norm2).mult(0.5f);
	    // normalize (probably not necessary, eh?)
	    norm1.sub(v2).normalize();
	    // add points on either side of v2 at distance brushWidth/2
	    pointsLeft.add(scaledNormalAtPoint(v2, norm1, -brushWidth/2));
	    pointsRight.add(scaledNormalAtPoint(v2, norm1, brushWidth/2));
	    v1 = v2;    // if v2 is the last point, v1 will store it when we exit the loop
	  }
	  // handle the last point
	  v1 = points.get(points.size() -1);
	  pointsLeft.add(v1.copy());
	  pointsRight.add(v1.copy());
	  // reverse one of the arrays
	  reverseArray(pointsLeft, 0, pointsLeft.size() - 1);
	  // generate two Bezier splines
	  PABezShape bezLeft = calculateCurve(pointsLeft);
	  PABezShape bezRight = calculateCurve(pointsRight);
	  if (isDrawWeighted ) {
	    calculateWeightedCurve(bezLeft);
	    calculateWeightedCurve(bezRight);
	  }
	  // append points in bezLeft to bezRight
	  ListIterator<PAVertex2DINF> it = bezLeft.curveIterator();
	  while (it.hasNext()) {
	    bezRight.append(it.next());
	  }
	  bezRight.setIsClosed(true);
	  // return the brushstroke shape in bezRight
	  return bezRight;
	}
	
	public static PABezShape quickBrushShape(ArrayList<PVector> points, float brushWidth) {
		return quickBrushShape(points, brushWidth, false, 0);
	}
	
	public static PABezShape quickBrushShape(ArrayList<PVector> points, float brushWidth, float weight) {
		return quickBrushShape(points, brushWidth, true, weight);
	}
	
	
	// returns a normal to a line at a point on the line at parametric distance u, normalized if d = 1
	public static PVector normalAtPoint(PVector a1, PVector a2, float u, float d) {
	  float ax1 = a1.x;
	  float ay1 = a1.y;
	  float ax2 = a2.x;
	  float ay2 = a2.y;
	  // determine the proportions of change on x and y axes for the line segment 
	  float f = (ax2 - ax1);
	  float g = (ay2 - ay1);
	  // get the point on the line segment at u
	  float pux = ax1 + f * u;
	  float puy = ay1 + g * u;
	  // prepare to calculate normalized parametric equations
	  float root = PApplet.sqrt(f*f + g*g);
	  float inv = 1/root;
	  // plug distance d into normalized parametric equations for normal to line segment
	  float x = pux - g * inv * d;
	  float y = puy + f * inv * d;
	  return new PVector(x, y);
	}

	// we're using this to scale normals at a determined point, but the "normal" could be any vector
	// PVector anchor   the point from which the normal extends
	// PVector normal   a normalized vector
	// float   d        distance from anchor to the returned vector
	public static PVector scaledNormalAtPoint(PVector anchor, PVector normal, float d) {
	  return new PVector(anchor.x + normal.x * d, anchor.y + normal.y * d);
	}

	public static void reverseArray(ArrayList<PVector> arr, int l, int r) {
	  PVector temp;
	  while (l < r) {
	    temp = arr.get(l);
	    arr.set(l, arr.get(r));
	    arr.set(r, temp);
	    l++;
	    r--;
	  }
	}	
	
	
	
}
