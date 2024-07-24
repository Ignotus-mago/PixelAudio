// some colors
int roig = 0xf64c2f;
int groc = 0xf6e959;
int blau = 0x5990e9;
int vert = 0x7bb222;
int taronja = 0xfea537;
int violet = 0xb29de9;
// array of colors, for random selection
public int[] randColors = { blau, groc, roig, vert, violet, taronja };

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


public int[] getHistoBounds(int[] source) {
    int min = 255;
    int max = 0;
    for (int i = 0; i < source.length; i++) {
      int[] comp = rgbComponents(source[i]);
      for (int j = 0; j < comp.length; j++) {
        if (comp[j] > max) max = comp[j];
        if (comp[j] < min) min = comp[j];
      }
    }
    println("--- min", min, " max ", max);
    return new int[]{min, max};
}

// histogram stretch -- run getHistoBounds to determine low and high
public int[] stretch(int[] source, int low, int high) {
  int[] out = new int[source.length];
  int r = 0, g = 0, b = 0;
  for (int i = 0; i < out.length; i++) {
    int[] comp = rgbComponents(source[i]);
    r = comp[0];
    g = comp[1];
    b = comp[2];
    r = (int) constrain(map(r, low, high, 1, 254), 0, 255);
    g = (int) constrain(map(g, low, high, 1, 254), 0, 255);
    b = (int) constrain(map(b, low, high, 1, 254), 0, 255);
    out[i] = composeColor(r, g, b, 255);
  }
  return out;
}

public void setGamma(float gamma) {
  this.gamma = gamma;
  if (gamma != 1.0) {
    this.gammaTable = new int[256];
    for (int i = 0; i < gammaTable.length; i++) {
      float c = i/(float)(gammaTable.length - 1);
      gammaTable[i] = (int) Math.round(Math.pow(c, gamma) * (gammaTable.length - 1));
    }
  }
}

public int[] adjustGamma(int[] source) {
  int[] out = new int[source.length];
  int r = 0, g = 0, b = 0;
  for (int i = 0; i < out.length; i++) {
    int[] comp = rgbComponents(source[i]);
    r = comp[0];
    g = comp[1];
    b = comp[2];
    r = gammaTable[r];
    g = gammaTable[g];
    b = gammaTable[b];
    out[i] = composeColor(r, g, b, 255);
  }
  return out;
}
