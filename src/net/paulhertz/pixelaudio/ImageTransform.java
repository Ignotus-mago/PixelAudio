package net.paulhertz.pixelaudio;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;

//what do we do about the background?
/**
 * @author ignotus
 *
 */
public class ImageTransform {
	PGraphics itGraf;
	int itw;
	int ith;

  /**
   * initializes a PGraphics with width = w and height = h.
   */
  public ImageTransform(int w, int h) {
    this.itw = w;
    this.ith = h;
    this.itGraf = PixelAudio.myParent.createGraphics(itw, ith);
  }

  /**
   * initializes a PGraphics with width = height = 1024.
   */
  public ImageTransform() {
    this(1024, 1024);
  }


	/**
	 * @return itGraf, the PGraphics instance used for transforms
	 */
	public PGraphics getGraphics() {
		return this.itGraf;
	}

	/**
	 * @return the width of the PGraphics instance used for transforms
	 */
	public int getWidth() {
		return this.itw;
	}

	/**
	 * @return the height of the PGraphics instance used for transforms
	 */
	public int getHeight() {
		return this.ith;
	}

	/**
	 * @param img   a PImage whose width and height will be used to set the width and height
	 *              of the PGraphics instance used for transforms
	 */
	public void adjustToImage(PImage img) {
		if (img.width != itw || img.height != ith) {
			itw = img.width;
			ith = img.height;
			itGraf.setSize(itw, ith);
		}
	}

	/**
	 * @param w   the new width of the PGraphics instance used for transforms
	 * @param h   the new height of the PGraphics instance used for transforms
	 */
	public void resizeGraphics(int w, int h) {
		itw = w;
		ith = h;
		itGraf.setSize(itw, ith);
	}

	/**
	 * @param img    a PImage to transform
	 * @param type   the ImageTransformType (ROT90CW, ROT90CCW, ROT180, FLIPX, FLIPY)
	 * @return       a transformed PImage from the PGraphics instance used for transforms
	 */
	public PImage transform(PImage img, ImageTransformType type) {
		// the methods called will set itGraf width and height to image width and height
		switch (type) {
		case ROT90CW: {
			img = rotate90(img);
			break;
		}
		case ROT90CCW: {
			img = rotate90CCW(img);
			break;
		}
		case ROT180: {
			img = rotate180(img);
			break;
		}
		case FLIPX: {
			img = flipX(img);
			break;
		}
		case FLIPX90: {
			img = flipX90(img);
			break;
		}
		case FLIPX90CCW: {
			img = flipX90CCW(img);
			break;
		}
		case FLIPY: {
			img = flipY(img);
			break;
		}
		case NADA: {
			break;
		}
		}
		return img;
	}

	/**
	 * Rotates or reflects a PImage, as specified by the type argument.
	 *
	 * @param img    a PImage to transform
	 * @param type   the ImageTransformType (ROT90CW, ROT90CCW, ROT180, FLIPX, FLIPY)
	 * @param c      the background color for the image (affects the alpha channel of the output)
	 * @return       a transformed PImage from the PGraphics instance used for transforms
	 */
	public PImage transform(PImage img, ImageTransformType type, int c) {
		// the methods called will set itGraf width and height to image width and height
		switch (type) {
		case ROT90CW: {
			img = rotate90(img, c);
			break;
		}
		case ROT90CCW: {
			img = rotate90CCW(img, c);
			break;
		}
		case ROT180: {
			img = rotate180(img, c);
			break;
		}
		case FLIPX: {
			img = flipX(img, c);
			break;
		}
		case FLIPX90: {
			img = flipX90(img, c);
			break;
		}
		case FLIPX90CCW: {
			img = flipX90CCW(img, c);
			break;
		}
		case FLIPY: {
			img = flipY(img, c);
			break;
		}
		case NADA: {
			img = nada(img, c);
			break;
		}
		}
		return img;
	}

	/**
	 * Rotates and scales a PImage around its center.
	 * You may need to set width and height with resizeGraphics before calling this method,
	 * e.g., when rotating by and angle other than a multiple of pi/2 radians.
	 *
	 * @param img    PImage to rotate and scale
	 * @param rads   angle of rotation in radians
	 * @param sx     x-axis scaling
	 * @param sy     y-axis scaling
	 * @return       a PImage obtained from the PGraphics instance used for transforms
	 */
	public PImage rotAndScale(PImage img, float rads, float sx, float sy) {
		itGraf.beginDraw();
		itGraf.pushMatrix();
		// itGraf.background(255);
		itGraf.translate(itw / 2, ith / 2);
		itGraf.scale(sx, sy);
		itGraf.rotate(rads);
		itGraf.translate(-itw / 2, -ith / 2);
		itGraf.image(img, (itw - img.width) / 2, (ith - img.height) / 2);
		itGraf.popMatrix();
		itGraf.endDraw();
		return itGraf.get();
	}

	/**
	 * Rotates and scales a PImage around its center.
	 * You may need to set width and height with resizeGraphics before calling this method,
	 * e.g., when rotating by and angle other than a multiple of pi/2 radians.
	 *
	 * @param img    PImage to rotate and scale
	 * @param rads   angle of rotation in radians
	 * @param sx     x-axis scaling
	 * @param sy     y-axis scaling
	 * @param c      background color (may affect alpha channel of output)
	 * @return       a PImage obtained from the PGraphics instance used for transforms
	 */
	public PImage rotAndScale(PImage img, float rads, float sx, float sy, int c) {
		itGraf.beginDraw();
		itGraf.pushMatrix();
		itGraf.background(c);
		itGraf.translate(itw / 2, ith / 2);
		itGraf.scale(sx, sy);
		itGraf.rotate(rads);
		itGraf.translate(-itw / 2, -ith / 2);
		itGraf.image(img, (itw - img.width) / 2, (ith - img.height) / 2);
		itGraf.popMatrix();
		itGraf.endDraw();
		return itGraf.get();
	}

	/**
	 * Rotates an image 90 degrees clockwise, returns a transformed copy
	 * @param img   PImage to transform
	 * @return      a PImage obtained from the PGraphics instance used for transforms
	 */
	public PImage rotate90(PImage img) {
		adjustToImage(img);
		itGraf.beginDraw();
		itGraf.pushMatrix();
		// itGraf.background(255);
		itGraf.translate(itw / 2, ith / 2);
		itGraf.rotate(PConstants.HALF_PI);
		itGraf.translate(-itw / 2, -ith / 2);
		itGraf.image(img, 0, 0);
		itGraf.popMatrix();
		itGraf.endDraw();
		return itGraf.get();
	}

	/**
	 * Rotates an image 90 degrees clockwise, returns a transformed copy
	 * @param img   PImage to transform
	 * @param c     background color (may affect alpha channel of output)
	 * @return      a PImage obtained from the PGraphics instance used for transforms
	 */
	public PImage rotate90(PImage img, int c) {
		adjustToImage(img);
		itGraf.beginDraw();
		itGraf.pushMatrix();
		itGraf.background(c);
		itGraf.translate(itw / 2, ith / 2);
		itGraf.rotate(PConstants.HALF_PI);
		itGraf.translate(-itw / 2, -ith / 2);
		itGraf.image(img, 0, 0);
		itGraf.popMatrix();
		itGraf.endDraw();
		return itGraf.get();
	}

	/**
	 * Rotates an image 90 degrees counter-clockwise, returns a transformed copy
	 * @param img   PImage to transform
	 * @return      a PImage obtained from the PGraphics instance used for transforms
	 */
	public PImage rotate90CCW(PImage img) {
		adjustToImage(img);
		itGraf.beginDraw();
		itGraf.pushMatrix();
		// itGraf.background(255);
		itGraf.translate(itw / 2, ith / 2);
		itGraf.rotate(-PConstants.HALF_PI);
		itGraf.translate(-itw / 2, -ith / 2);
		itGraf.image(img, 0, 0);
		itGraf.popMatrix();
		itGraf.endDraw();
		return itGraf.get();
	}

	/**
	 * Rotates an image 90 degrees counter-clockwise, returns a transformed copy
	 * @param img   PImage to transform
	 * @param c     background color (may affect alpha channel of output)
	 * @return      a PImage obtained from the PGraphics instance used for transforms
	 */
	public PImage rotate90CCW(PImage img, int c) {
		adjustToImage(img);
		itGraf.beginDraw();
		itGraf.pushMatrix();
		itGraf.background(c);
		itGraf.translate(itw / 2, ith / 2);
		itGraf.rotate(-PConstants.HALF_PI);
		itGraf.translate(-itw / 2, -ith / 2);
		itGraf.image(img, 0, 0);
		itGraf.popMatrix();
		itGraf.endDraw();
		return itGraf.get();
	}

	/**
	 * Rotates an image 180 degrees, returns a transformed copy
	 * @param img   PImage to transform
	 * @return      a PImage obtained from the PGraphics instance used for transforms
	 */
	public PImage rotate180(PImage img) {
		adjustToImage(img);
		itGraf.beginDraw();
		itGraf.pushMatrix();
		// itGraf.background(255);
		itGraf.translate(itw / 2, ith / 2);
		itGraf.rotate(PConstants.PI);
		itGraf.translate(-itw / 2, -ith / 2);
		itGraf.image(img, 0, 0);
		itGraf.popMatrix();
		itGraf.endDraw();
		return itGraf.get();
	}

	/**
	 * Rotates an image 180 degrees, returns a transformed copy
	 * @param img   PImage to transform
	 * @param c     background color (may affect alpha channel of output)
	 * @return      a PImage obtained from the PGraphics instance used for transforms
	 */
	public PImage rotate180(PImage img, int c) {
		adjustToImage(img);
		itGraf.beginDraw();
		itGraf.pushMatrix();
		itGraf.background(c);
		itGraf.translate(itw / 2, ith / 2);
		itGraf.rotate(PConstants.PI);
		itGraf.translate(-itw / 2, -ith / 2);
		itGraf.image(img, 0, 0);
		itGraf.popMatrix();
		itGraf.endDraw();
		return itGraf.get();
	}

	/**
	 * Reflects an image on the y-axis, flipping the x-coordinates, returns a transformed copy
	 * @param img   PImage to transform
	 * @return      a PImage obtained from the PGraphics instance used for transforms
	 */
	public PImage flipX(PImage img) {
		adjustToImage(img);
		itGraf.beginDraw();
		itGraf.pushMatrix();
		// itGraf.background(255);
		itGraf.scale(-1, 1);
		itGraf.image(img, -img.width, 0);
		itGraf.popMatrix();
		itGraf.endDraw();
		return itGraf.get();
	}

	/**
	 * Reflects an image on the y-axis, flipping the x-coordinates, returns a transformed copy
	 * @param img   PImage to transform
	 * @param c     background color (may affect alpha channel of output)
	 * @return      a PImage obtained from the PGraphics instance used for transforms
	 */
	public PImage flipX(PImage img, int c) {
		adjustToImage(img);
		itGraf.beginDraw();
		itGraf.pushMatrix();
		itGraf.background(c);
		itGraf.scale(-1, 1);
		itGraf.image(img, -img.width, 0);
		itGraf.popMatrix();
		itGraf.endDraw();
		return itGraf.get();
	}

  /**
	 * Reflects an image on the y-axis, flipping the x-coordinates, rotates it 90 degrees CW and returns a transformed copy
	 * @param img   PImage to transform
	 * @return      a PImage obtained from the PGraphics instance used for transforms
   */
  public PImage flipX90(PImage img) {
    adjustToImage(img);
    itGraf.beginDraw();
    itGraf.pushMatrix();
    // itGraf.background(255);
    itGraf.translate(itw / 2, ith / 2);
    itGraf.rotate(PConstants.HALF_PI);
    itGraf.translate(-itw / 2, -ith / 2);
    itGraf.scale(-1, 1);
    itGraf.image(img, -img.width, 0);
    itGraf.popMatrix();
    itGraf.endDraw();
    return itGraf.get();
  }

  /**
	 * Reflects an image on the y-axis, flipping the x-coordinates, rotates it 90 degrees CW and returns a transformed copy
	 * @param img   PImage to transform
	 * @param c     background color (may affect alpha channel of output)
	 * @return      a PImage obtained from the PGraphics instance used for transforms
   */
  public PImage flipX90(PImage img, int c) {
    adjustToImage(img);
    itGraf.beginDraw();
    itGraf.pushMatrix();
    itGraf.background(c);
    itGraf.translate(itw / 2, ith / 2);
    itGraf.rotate(PConstants.HALF_PI);
    itGraf.translate(-itw / 2, -ith / 2);
    itGraf.scale(-1, 1);
    itGraf.image(img, -img.width, 0);
    itGraf.popMatrix();
    itGraf.endDraw();
    return itGraf.get();
  }

  /**
	 * Reflects an image on the y-axis, flipping the x-coordinates, rotates it 90 degrees CCW and returns a transformed copy
	 * @param img   PImage to transform
	 * @return      a PImage obtained from the PGraphics instance used for transforms
   */
  public PImage flipX90CCW(PImage img) {
    adjustToImage(img);
    itGraf.beginDraw();
    itGraf.pushMatrix();
    // itGraf.background(255);
    itGraf.translate(itw / 2, ith / 2);
    itGraf.rotate(-PConstants.HALF_PI);
    itGraf.translate(-itw / 2, -ith / 2);
    itGraf.scale(-1, 1);
    itGraf.image(img, -img.width, 0);
    itGraf.popMatrix();
    itGraf.endDraw();
    return itGraf.get();
  }

  /**
	 * Reflects an image on the y-axis, flipping the x-coordinates, rotates it 90 degrees CCW and returns a transformed copy
	 * @param img   PImage to transform
	 * @param c     background color (may affect alpha channel of output)
	 * @return      a PImage obtained from the PGraphics instance used for transforms
   */
  public PImage flipX90CCW(PImage img, int c) {
    adjustToImage(img);
    itGraf.beginDraw();
    itGraf.pushMatrix();
    itGraf.background(c);
    itGraf.translate(itw / 2, ith / 2);
    itGraf.rotate(-PConstants.HALF_PI);
    itGraf.translate(-itw / 2, -ith / 2);
    itGraf.scale(-1, 1);
    itGraf.image(img, -img.width, 0);
    itGraf.popMatrix();
    itGraf.endDraw();
    return itGraf.get();
  }

	/**
	 * Reflects an image on the x-axis, flipping the y-coordinates, returns a transformed copy.
	 * Same as flipX() followed by rotate180().
	 * @param img   PImage to transform
	 * @return      a PImage obtained from the PGraphics instance used for transforms
	 */
	public PImage flipY(PImage img) {
		adjustToImage(img);
		itGraf.beginDraw();
		itGraf.pushMatrix();
		// itGraf.background(255);
		itGraf.scale(1, -1);
		itGraf.image(img, 0, -img.height);
		itGraf.popMatrix();
		itGraf.endDraw();
		return itGraf.get();
	}

	/**
	 * Reflects an image on the x-axis, flipping the y-coordinates, returns a transformed copy
	 * @param img   PImage to transform
	 * @param c     background color (may affect alpha channel of output)
	 * @return      a PImage obtained from the PGraphics instance used for transforms
	 */
	public PImage flipY(PImage img, int c) {
		adjustToImage(img);
		itGraf.beginDraw();
		itGraf.pushMatrix();
		itGraf.background(c);
		itGraf.scale(1, -1);
		itGraf.image(img, 0, -img.height);
		itGraf.popMatrix();
		itGraf.endDraw();
		return itGraf.get();
	}

	/**
	 * Leaves image geometry untransformed but possibly with a different background color (for images with an alpha channel)
	 * @param img   PImage to transform
	 * @param c     background color (may affect alpha channel of output)
	 * @return      a PImage obtained from the PGraphics instance used for transforms
	 */
	public PImage nada(PImage img, int c) {
		adjustToImage(img);
		itGraf.beginDraw();
		itGraf.pushMatrix();
		itGraf.background(c);
		itGraf.image(img, 0, 0);
		itGraf.popMatrix();
		itGraf.endDraw();
		return itGraf.get();
	}


}