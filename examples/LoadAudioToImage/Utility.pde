int roig = #f64c2f;
int groc = #f6e959;
int blau = #5990e9;
int blau2 = #90b2dc;
int vert = #7bb222;
int taronja = #fea537;
int roigtar = #E9907B;
int violet = #b29de9;
// array of colors, usually for random selection
public int[] randColors = {blau, groc, roig, vert, violet, taronja};

/**
 * Returns a Gaussian variable using a Java library call to <code>Random.nextGaussian</code>.
 * @param mean
 * @param variance
 * @return a Gaussian-distributed random number with mean <code>mean</code> and variance <code>variance</code>
 */
public double gauss(double mean, double variance) {
  return rando.nextGaussian() * Math.sqrt(variance) + mean;
}


/**
 * Shuffles an array of integers into random order.
 * Implements Richard Durstenfeld's version of the Fisher-Yates algorithm, popularized by Donald Knuth.
 * see http://en.wikipedia.org/wiki/Fisher-Yates_shuffle
 * @param intArray an array of <code>int</code>s, changed on exit
 */
public void shuffle(int[] intArray) {
  for (int lastPlace = intArray.length - 1; lastPlace > 0; lastPlace--) {
    // Choose a random location from 0..lastPlace
    int randLoc = int(random(lastPlace + 1));
    // Swap items in locations randLoc and lastPlace
    int temp = intArray[randLoc];
    intArray[randLoc] = intArray[lastPlace];
    intArray[lastPlace] = temp;
  }
}

/**
 * Breaks a Processing color into R, G and B values in an array.
 * @param argb   a Processing color as a 32-bit integer
 * @return       an array of integers in the intRange 0..255 for 3 primary color components: {R, G, B}
 */
public static int[] rgbaComponents(int argb) {
  int[] comp = new int[4];
  comp[0] = (argb >> 16) & 0xFF;  // Faster way of getting red(argb)
  comp[1] = (argb >> 8) & 0xFF;   // Faster way of getting green(argb)
  comp[2] = argb & 0xFF;          // Faster way of getting blue(argb)
  comp[3] = (argb >> 24);         // alpha component
  return comp;
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
 * Breaks a Processing color into R, G and B values in an array.
 * @param argb   a Processing color as a 32-bit integer
 * @return       an array of integers in the range 0..255 for 3 primary color components: {R, G, B}
 */
public int[] rgbComponents(int argb) {
  int[] comp = new int[3];
  comp[0] = (argb >> 16) & 0xFF;  // Faster way of getting red(argb)
  comp[1] = (argb >> 8) & 0xFF;   // Faster way of getting green(argb)
  comp[2] = argb & 0xFF;          // Faster way of getting blue(argb)
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

public int setAlpha(int argb, int alpha) {
  int[] c = rgbComponents(argb);
  return alpha << 24 | c[0] << 16 | c[1] << 8 | c[2];
}


    /* get min and max
    float min = 0;
    float max = 0;
    for (int i = 0; i < rgbSignal.length; i++) {         
      if (audioSignal[i] > max) max = audioSignal[i];
      if (audioSignal[i] < min) min = audioSignal[i];
    }
    println("--- min", min, " max ", max);
    */
