package net.paulhertz.pixelaudio;

/**
 * 	<pre>
 *   ROT90        rotate 90 degrees clockwise
 *   ROT90CCW     rotate 90 degrees counterclockwise
 *   ROT180       rotate 180 degrees
 *   FLIPX        reflect vertically, y coordinates do not change
 *   FLIPX90      reflect vertically, then rotate 90 clockwise = reflect on the secondary diagonal, upper left to lower right, secondary diagonal does not change
 *   FLIPX90CCW   reflect vertically, then rotate 90 counterclockwise = reflect on the primary diagonal, upper right to lower left, primary diagonal does not change
 *   FLIPY        reflect horizontally, x coordinates do not change
 *   NADA         no operation
 *  </pre>
 *   
 *   @see BitmapTransform
 */
public enum AffineTransformType {ROT90, ROT90CCW, ROT180, FLIPX, FLIPX90, FLIPX90CCW, FLIPY, NADA}
