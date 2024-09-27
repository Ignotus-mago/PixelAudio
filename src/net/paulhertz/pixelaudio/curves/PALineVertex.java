package net.paulhertz.pixelaudio.curves;

import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * Stores a line vertex consisting of a single point.
 * Adapted from IgnoCodeLib Processing library for use with PixelAudio library.
 *
 * IgnoCodeLib is available in the Processing contributed libraries and at
 * https://paulhertz.net/ignocodelib/ and https://github.com/Ignotus-mago/IgnoCodeLib3
 *
 */
public class PALineVertex implements PAVertex2DINF {
  /** x-coordinate of anchor point */
  protected float x;
  /** y-coordinate of anchor point */
  protected float y;
  /** path segment type */
  public final static int segmentType = PABezShape.LINE_SEGMENT;

  public PALineVertex(float x, float y) {
    this.x = x;
    this.y = y;
  }

  public PALineVertex() {
    this(0, 0);
  }

  @Override
  public float x() {
    return x;
  }
  public void setX(float newX) {
    x = newX;
  }

  @Override
  public float y() {
    return y;
  }
  public void setY(float newY) {
    y = newY;
  }

  @Override
  public int segmentType() {
    return PALineVertex.segmentType;
  }

  @Override
  public float[] coords() {
    float[] knots = new float[2];
    knots[0] = x;
    knots[1] = y;
    return knots;
  }

  @Override
  public PALineVertex clone() {
    return new PALineVertex(this.x, this.y);
  }

  @Override
  public void draw(PApplet parent) {
    parent.vertex(x, y);
  }

   @Override
   public void draw(PGraphics pg) {
     pg.vertex(x, y);
  }
  
  public void mark(PApplet parent) {
    int w = 6;
    parent.pushStyle();
    parent.noStroke();
    parent.fill(160);
    parent.square(x - w/2, y - w/2, w);
    parent.popStyle();
  }

  public void mark(PGraphics pg) {
    int w = 6;
    pg.pushStyle();
    pg.noStroke();
    pg.fill(160);
    pg.square(x - w/2, y - w/2, w);
    pg.popStyle();
  }

}