import net.paulhertz.pixelaudio.*;

PImage img;
// fShapeSquare.png is an opaque square, fShapeRect.png is a rectangle.
String imgFilename = "fShapeSquare.png";      
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
  println("\nPress 'f' to rotate image clockwise.");
  println("Press 'b' to rotate image counterclockwise.");
  println("Press 'r' to rotate image 180 degrees.");
  println("Press 'x' to flip x coordinates = mirror on y-axis.");
  println("Press 'y' to flip y coordinates = mirror on x-axis.");
  println("Press '1' to mirror on primary diagonal.");
  println("Press '2' to mirror on secondary diagonal.");
  println("Press 'o' to reload image.");
  println("Press 's' to save display to a file.");
  println("Press 'm' to test index remapping.");
  println("Press 'h' to show this help message in the console.\n");
}

public void keyPressed() {
  switch (key) {
    case 'f':
      img = BitmapTransform.imageTransform(img, AffineTransformType.ROT90);
      break;
    case 'b':
      img = BitmapTransform.imageTransform(img, AffineTransformType.ROT90CCW);
      break;
    case 'r':
      img = BitmapTransform.imageTransform(img, AffineTransformType.ROT180);
      break;
    case 'x':
      img = BitmapTransform.imageTransform(img, AffineTransformType.FLIPX);
      break;
    case 'y':
      img = BitmapTransform.imageTransform(img, AffineTransformType.FLIPY);
      break;
    case '1':
      img = BitmapTransform.imageTransform(img, AffineTransformType.FLIPX90CCW);
      break;
    case '2':
      img = BitmapTransform.imageTransform(img, AffineTransformType.FLIPX90);
      break;
    case 'o': 
      // reload the image
      img = loadImage(imgFilename);
      break;
    case 's':
      img.save("transformed_image.png");
      break;
    case 'm':
        testAffineMap(4, 3);
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
