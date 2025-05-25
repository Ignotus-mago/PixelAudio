/**
 *
 * TransformPImage demonstrates some of the commands available in the BitmapTransform class.
 * See the keyPressed method for the syntax of the commands.
 *
 * Press 'f' to rotate image clockwise.
 * Press 'b' to rotate image counterclockwise.
 * Press 'r' to rotate image 180 degrees.
 * Press 'x' to flip x coordinates = mirror on y-axis.
 * Press 'y' to flip y coordinates = mirror on x-axis.
 * Press '1' to mirror on primary diagonal.
 * Press '2' to mirror on secondary diagonal.
 * Press 'o' to reload image.
 * Press 's' to save display to a file.
 * Press 'm' to test index remapping.
 * Press 'h' to show this help message in the console.
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
  println("\n * Press 'f' to rotate image clockwise.");
  println(" * Press 'b' to rotate image counterclockwise.");
  println(" * Press 'r' to rotate image 180 degrees.");
  println(" * Press 'x' to flip x coordinates = mirror on y-axis.");
  println(" * Press 'y' to flip y coordinates = mirror on x-axis.");
  println(" * Press '1' to mirror on primary diagonal.");
  println(" * Press '2' to mirror on secondary diagonal.");
  println(" * Press 'o' to reload image.");
  println(" * Press 's' to save display to a file.");
  println(" * Press 'm' to test index remapping.");
  println(" * Press 'h' to show this help message in the console.\n");
}

public void keyPressed() {
  switch (key) {
    case 'f':
      img = BitmapTransform.imageTransform(img, AffineTransformType.R270);
      break;
    case 'b':
      img = BitmapTransform.imageTransform(img, AffineTransformType.R90);
      break;
    case 'r':
      img = BitmapTransform.imageTransform(img, AffineTransformType.R180);
      break;
    case 'x':
      img = BitmapTransform.imageTransform(img, AffineTransformType.FLIPX);
      break;
    case 'y':
      img = BitmapTransform.imageTransform(img, AffineTransformType.FLIPY);
      break;
    case '1':
      img = BitmapTransform.imageTransform(img, AffineTransformType.FX90);
      break;
    case '2':
      img = BitmapTransform.imageTransform(img, AffineTransformType.FX270);
      break;
    case 'o': 
      // reload the image
      img = loadImage(imgFilename);
      break;
    case 's':
      img.save("transformed_image.png");
      break;
    case 'm':
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
