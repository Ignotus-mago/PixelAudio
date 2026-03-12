package net.paulhertz.pixelaudio.curves;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import net.paulhertz.pixelaudio.PixelAudioMapper;
import processing.core.*;

// TODO time data output
public class PAGestureWriter {
	static final char gPathOps[] = { 'N', 'n', 'S', 's', 'F', 'f', 'B', 'b' };
	/** Binary flag for filled path operators. FILL, STROKE and CLOSE values can be summed to index a path operator tag */
	public final static int  FILL = 4;
	/** Binary flag for stroked path operators. FILL, STROKE and CLOSE values can be summed to index a path operator tag */
	public final static int  STROKE = 2;
	/** Binary flag for closed path operators. FILL, STROKE and CLOSE values can be summed to index a path operator tag */
	public final static int  CLOSE = 1;
	/** Closed, filled, and stroked path operator. */
	public final static char CLOSED_FILLED_STROKED = 'b';
	/** Open, filled, and stroked path operator. */
	public final static char OPEN_FILLED_STROKED = 'B';
	/** Closed and filled path operator. */
	public final static char CLOSED_FILLED = 'f';
	/** Open and filled path operator. */
	public final static char OPEN_FILLED = 'F';
	/** Closed and stroked path operator. */
	public final static char CLOSED_STROKED = 's';
	/** Open and stroked path operator. */
	public final static char OPEN_STROKED = 'S';
	/** Closed and non-printing (invisible) path operator. */
	public final static char NONPRINTING_CLOSED = 'n';
	/** Open and non-printing (invisible) path operator. */
	public final static char NONPRINTING_OPEN = 'N';
	/** Adobe Illustrator default curve recursion is 4 */
	static final int gCurveRecursionDepth = 4;
	/** A number formatter: call fourPlaces.format(Number) to return a String with four decimal places. */
	static public DecimalFormat fourPlaces;
	/** optional transparency flag, for export to AI. Transparency is not part of the AI 7.0 spec, but we try to support it. */
	protected static boolean useTransparency = false;

	static {
		Locale loc = Locale.US;
		DecimalFormatSymbols dfSymbols = new DecimalFormatSymbols(loc);
		dfSymbols.setDecimalSeparator('.');
		fourPlaces = new DecimalFormat("0.0000", dfSymbols);
	}

	public static void writeHeader(PrintWriter pw, String title) {

	}

	/**
	 * Writes RGB fill value and fill operator "Xa" to output.
	 * @param r   red component (0..1)
	 * @param g   green component (0..1)
	 * @param b   blue component (0..1)
	 * @param pw      <code>PrintWriter</code> for file output
	 */
	public static void setRGBFill(double r, double g, double b, PrintWriter pw) {
		String rs = fourPlaces.format(r);
		String gs = fourPlaces.format(g);
		String bs = fourPlaces.format(b);
		pw.println(rs +" "+ gs +" "+ bs +" Xa");
	}
	/**
	 * Writes RGB fill value and fill operator "Xa" to output.
	 * @param shade   an array of four <code>double</code>s in the range (0..1)
	 * @param pw      <code>PrintWriter</code> for file output
	 */
	public static void setRGBFill(double[] shade, PrintWriter pw) {
		setRGBFill(shade[0], shade[1], shade[2], pw);
	}
	/**
	 * Writes RGB fill value and fill operator "Xa" to output.
	 * @param shade   an RGBColor instance
	 * @param pw      <code>PrintWriter</code> for file output
	 */
	public static void setRGBFill(int shade, PrintWriter pw) {
		int[] rgb = PixelAudioMapper.rgbComponents(shade);
		setRGBFill(rgb[0], rgb[1], rgb[2], pw);
	}


	/**
	 * Writes RGB stroke value and fill operator "XA" to output.
	 * @param r   red component (0..1)
	 * @param g   green component (0..1)
	 * @param b   blue component (0..1)
	 * @param pw      <code>PrintWriter</code> for file output
	 */
	public static void setRGBStroke(double r, double g, double b, PrintWriter pw) {
		String rs = fourPlaces.format(r);
		String gs = fourPlaces.format(g);
		String bs = fourPlaces.format(b);
		pw.println( rs +" "+ gs +" "+ bs +" XA");
	}
	/**
	 * Writes RGB stroke value and fill operator "XA" to output.
	 * @param shade   an array of four <code>double</code>s in the range (0..1)
	 * @param pw      <code>PrintWriter</code> for file output
	 */
	public static void setRGBStroke(double[] shade, PrintWriter pw) {
		setRGBStroke(shade[0], shade[1], shade[2], pw);
	}
	/**
	 * Writes RGB stroke value and fill operator "XA" to output.
	 * @param shade   a RGBColor instance
	 * @param pw      <code>PrintWriter</code> for file output
	 */
	public static void setRGBStroke(int shade, PrintWriter pw) {
		int[] rgb = PixelAudioMapper.rgbComponents(shade);
		setRGBStroke(rgb[0], rgb[1], rgb[2], pw);
	}


	/**
	 * Writes weight (in points) and weight operator "w" to output.
	 * @param weight   stroke weight (positive decimal value)
	 * @param pw      <code>PrintWriter</code> for file output
	 */
	public static void setWeight(double weight, PrintWriter pw) {
		pw.println(fourPlaces.format(weight) + " w");
	}
	
	
	/**
	 * @return true if transparency is enabled, false otherwise.
	 */
	public static boolean useTransparency() {
		return PAGestureWriter.useTransparency;
	}
	/**
	 * Pass a value of true to enable transparency when exporting to Adobe Illustrator, default is false. 
	 * Transparency markup is not supported in the AI7.0 specification, but it seems to work.
	 * Note that in Illustrator transparency affects the entire shape, both fill and stroke,
	 * unlike Processing, where fill and stroke can have separate transparency values. This
	 * means for stroked shapes, the stroke transparency will affect the whole shape in AI.
	 * See {@link #setTransparency(double, PrintWriter)}.
	 * @param useTransparency
	 */
	public static void setUseTransparency(boolean useTransparency) {
		PAGestureWriter.useTransparency = useTransparency;
	}
	/**
	 * Writes current opacity to an Illustrator file (not part of the AI7 spec).
	 * This particular operator is pieced together from inspecting AI files
	 * It is not part of the AI7 specification, the last one published by Adobe.
	 * I do not know what each of the arguments to Xy does, but the second one controls opacity.
	 * @param trans   transparency value, in the range 0..1
	 * @param pw      <code>PrintWriter</code> for file output
	 */
	public static void setTransparency(double trans, PrintWriter pw) {
		// 0 0.55 0 0 0 Xy
		pw.println("0 " + fourPlaces.format(trans) + " 0 0 0 Xy");
	}
	/**
	 * If you set transparency, either set it for every object, or reset it to totally opaque by calling noTransparency.
	 * @param pw      <code>PrintWriter</code> for file output
	 */
	public static void noTransparency(PrintWriter pw) {
		pw.println("0 1 0 0 0 Xy");
	}

	/**
	 * Writes current point and "m" operator to output.
	 * @param x    x coordinate
	 * @param y    y coordinate
	 * @param pw   <code>PrintWriter</code> for file output
	 */
	public static void psMoveTo(double x, double y, PrintWriter pw) {
		pw.println( fourPlaces.format(x) +" "+ fourPlaces.format(y) + " m");
	}
	/**
	 * Writes current point and "m" operator to output.
	 * @param pt   array of two double values, x and y coordinates
	 * @param pw   <code>PrintWriter</code> for file output
	 */
	public static void psMoveTo(double[] pt, PrintWriter pw) {
		psMoveTo( pt[0], pt[1], pw);
	}
	/**
	 * Writes current point and "m" operator to output.
	 * @param pt   a Java Point2D instance
	 * @param pw   <code>PrintWriter</code> for file output
	 */
	public static void psMoveTo(PVector pt, PrintWriter pw) {
		psMoveTo(pt.x, pt.y, pw);
	}


	/**	
	 * Writes current point and "L" operator to output.
	 * @param x    x coordinate
	 * @param y    y coordinate
	 * @param pw   <code>PrintWriter</code> for file output
	 */
	public static void psLineTo(double x, double y, PrintWriter pw) {
		pw.println( fourPlaces.format(x) +" "+ fourPlaces.format(y) + " L");
	}


	/**
	 * Writes current point and "c" operator to output.
	 * @param x1   control point 1 x coordinate
	 * @param y1   control point 1 y coordinate
	 * @param x2   control point 2 x coordinate
	 * @param y2   control point 2 y coordinate
	 * @param x3   end point x coordinate
	 * @param y3   end point y coordinate
	 * @param pw   <code>PrintWriter</code> for file output
	 */
	public static void psCurveTo(double x1, double y1, double x2, double y2, double x3, double y3, PrintWriter pw) {
		pw.println(fourPlaces.format(x1) +" "+ fourPlaces.format(y1) +" "+
				   fourPlaces.format(x2) +" "+ fourPlaces.format(y2) +" "+
				   fourPlaces.format(x3) +" "+ fourPlaces.format(y3) +" c");
	}


	/**
	 * Closes a series of path construction operations with the appropriate
	 * operator stored in gPathOps, {'N', 'n', 'S', 's', 'F', 'f', 'B', 'b'}.
	 * Requires an index value for the path operator. It may be simpler to call
	 * the other {@link #paintPath(char, PrintWriter) paintPath} method, that accepts
	 * character constant. 
	 * <pre>
	 *			fill	stroke	close	
	 *		 b	  1		  1		  1		closed filled and stroked path
	 *		 B	  1		  1		  0		open filled and stroked path
	 *		 f	  1		  0		  1		closed filled path
	 *		 F	  1		  0		  0		open filled path
	 *		 s	  0		  1		  1		closed stroked path
	 *		 S	  0		  1		  0		open stroked path
	 *		 n	  0		  0		  1		non-printing closed path
	 *		 N	  0		  0		  0		non-printing open path
	 * </pre>
	 * @param pathIndex   an int in the range (0..7)
	 * @param pw   <code>PrintWriter</code> for file output
	 */
	public static void paintPath(int pathIndex, PrintWriter pw) {
		char pathOp;
		pathOp = gPathOps[pathIndex];
		pw.println(pathOp);
	}	
	/**
	 * Closes a series of path construction operations with the appropriate operator.
	 *
	 * @param pathOp   a <code>char</code> in {'N', 'n', 'S', 's', 'F', 'f', 'B', 'b'}
	 *                 <p> It is simpler just to use one of the supplied constants
	 *                 CLOSED_FILLED_STROKED, OPEN_FILLED_STROKED, CLOSED_FILLED, OPEN_FILLED, 
	 *                 CLOSED_STROKED, OPEN_STROKED, NONPRINTING_CLOSED, NONPRINTING_OPEN</p>
	 * @param pw      <code>PrintWriter</code> for file output
	 */
	public static void paintPath(char pathOp, PrintWriter pw) {
		pw.println(pathOp);
	}
	
}