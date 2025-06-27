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

/**
 * Interface for line and curve vertices. Classes that implement this interface must implement
 * all the methods listed below. 
 * Adapted from IgnoCodeLib Processing library for use with PixelAudio library.
 *
 * IgnoCodeLib is available in the Processing contributed libraries and at
 * https://paulhertz.net/ignocodelib/ and https://github.com/Ignotus-mago/IgnoCodeLib3
 *
 */
public interface PAVertex2DINF {
  /**
   * @return x-coordinate as a float
   */
  public float x();
  
  /**
   * @return y-coordinate as a float
   */
  public float y();
  
  /**
   * @return type of segment, either BezShape.LINE_SEGMENT or BezShape.CURVE_SEGMENT
   */
  public int segmentType();
  
  /**
   * @return coordinates as an array of float
   */
  public float[] coords();
  
  /**
   * @return a deep copy of a Vertex2DINF
   */
  public PAVertex2DINF clone();
  
  /** 
   * Draws a path to the display. It is only valid to call this within a 
   * Processing beginShape/endShape pair where  an initial 
   * vertex has been set with a call to vertex(). 
   * @param parent   the PApplet that handles drawing
   */
  public void draw(PApplet parent);
  
  /**
   * Draws a path to an offscreen buffer. It is only valid to call this within a 
   * Processing beginShape/endShape pair where  an initial 
   * vertex has been set with a call to vertex(). 
   * @param pg   a PGraphics instance
   */
  public void draw(PGraphics pg);
  
  /**
   * Draws marks at vertices and control points.
   */
  public void mark(PApplet parent);
  
  /**
   * Draws marks at vertices and control points to an off-screen PGraphics.
   */
  public void mark(PGraphics pg);
  
}