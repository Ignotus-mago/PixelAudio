/**
 *
 * TransformPImage demonstrates some of the commands available in the BitmapTransform class.
 * The commands rely on PixelAudio's AffineTransformType enum and BitmapTransform class.
 * They are limited to rotation and reflection, but are optimized to do them fast. 
 *
 * The AffineTransformType enum defines the types of affine transformations that
 * can be applied to bitmaps in the PixelAudio library using only rotation and
 * reflection. PixelMapGen uses the AffineTransformType enum to specify the type
 * of transformation to apply to the gen coordinates and LUTs. The BitmapTransform
 * class applies the transformations defined in AffineTransformType to rotate
 * and reflect bitmaps using lookup tables.
 * 
 * Naming follows computer graphics conventions where 0 degrees points right and
 * positive rotation is counterclockwise.
 *
 * Here are AffineTransformType's operations:
 *
 *   NADA     no operation
 *   R270     rotate 90 degrees clockwise
 *   ROT180   rotate 180 degrees
 *   R90      rotate 90 degrees counterclockwise
 *   FLIPX    reflect on y-axis, y coordinates do not change
 *   FX270    reflect on y-axis, then rotate 90 clockwise 
 *                => reflect on the secondary diagonal, upper left to lower right, 
 *                secondary diagonal does not change
 *   FLIPY    reflect on x-axis, x coordinates do not change
 *   FX90     reflect on y-axis, then rotate 90 counterclockwise 
 *                => reflect on the primary diagonal, upper right to lower left, 
 *                primary diagonal does not change
 *
 *
 * Press 'f' to rotate image 90 degrees clockwise (R270).
 * Press 'b' to rotate image 90 degrees counterclockwise (R90).
 * Press 'r' to rotate image 180 degrees (R180).
 * Press 'x' to flip x coordinates = mirror on y-axis (FLIPX).
 * Press 'y' to flip y coordinates = mirror on x-axis (FLIPY).
 * Press '1' to mirror on primary diagonal (FX90).
 * Press '2' to mirror on primary diagonal (FX270).
 * Press 'o' to reload the image.
 * Press 's' to save the image.
 * Press 'm' to print out the affine map of a small square or rectangle.
 * Press 'h' to show this help message.
 *
 */

import net.paulhertz.pixelaudio.*;

PImage img;
// fShapeSquare.png is an opaque square, fShapeRect.png is a rectangle.
String imgFilename = "fShapeRect.png";      
BitmapTransform bTrans;
PixelAudio pixelaudio;

public void setup() {
  size(1024, 1024);
  img = loadImage(imgFilename);
  pixelaudio = new PixelAudio(this);
  showHelp();
}

public void draw() {
  background(255);
  image(img, (width - img.width)/2, (height - img.height)/2);
}

public void showHelp() {
println("\n * Press 'f' to rotate image 90 degrees clockwise (R270).");
println(" * Press 'b' to rotate image 90 degrees counterclockwise (R90).");
println(" * Press 'r' to rotate image 180 degrees (R180).");
println(" * Press 'x' to flip x coordinates = mirror on y-axis (FLIPX).");
println(" * Press 'y' to flip y coordinates = mirror on x-axis (FLIPY).");
println(" * Press '1' to mirror on primary diagonal (FX90).");
println(" * Press '2' to mirror on primary diagonal (FX270).");
println(" * Press 'o' to reload the image.");
println(" * Press 's' to save the image.");
println(" * Press 'm' to print out the affine map of a small square or rectangle.");
println(" * Press 'h' to show this help message.");
}

public void keyPressed() {
  switch (key) {
    case 'f': // rotate image 90 degrees clockwise (R270)
      img = BitmapTransform.imageTransform(img, AffineTransformType.R270);
      break;
    case 'b': // rotate image 90 degrees counterclockwise (R90)
      img = BitmapTransform.imageTransform(img, AffineTransformType.R90);
      break;
    case 'r': // rotate image 180 degrees (R180)
      img = BitmapTransform.imageTransform(img, AffineTransformType.R180);
      break;
    case 'x': // flip x coordinates = mirror on y-axis (FLIPX)
      img = BitmapTransform.imageTransform(img, AffineTransformType.FLIPX);
      break;
    case 'y': // flip y coordinates = mirror on x-axis (FLIPY)
      img = BitmapTransform.imageTransform(img, AffineTransformType.FLIPY);
      break;
    case '1': // mirror on primary diagonal (FX90)
      img = BitmapTransform.imageTransform(img, AffineTransformType.FX90);
      break;
    case '2': // mirror on primary diagonal (FX270)
      img = BitmapTransform.imageTransform(img, AffineTransformType.FX270);
      break;
    case 'o': // reload the image
      img = loadImage(imgFilename);
      break;
    case 's': // save the image
      img.save("transformed_image.png");
      break;
    case 'm': // print out the affine map of a small square or rectangle
        if (imgFilename.equals("fShapeRect.png")) testAffineMap(4, 3);
        else testAffineMap(4, 4);
        break;
    case 'h':
        showHelp();
        break;
    default:
      break;
  }
}

public void testAffineMap(int w, int h) {
  println("\n"+ w +" x "+ h +" bitmap index remapping\n");
  for (AffineTransformType type : AffineTransformType.values()) {
    println("------------- "+ type.name() +" -------------");
    int[] newMap = BitmapTransform.getIndexMap(w, h, type);
    int i = 0;
    for (int n : newMap) {
      if (i < newMap.length - 1) print(n +", ");
      else print(n +"\n ");
      i++;
    }
  }
}
