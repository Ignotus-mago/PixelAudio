/**
 * Returns a Gaussian variable using a Java library call to
 * <code>Random.nextGaussian</code>.
 *
 * @param mean
 * @param variance
 * @return a Gaussian-distributed random number with mean <code>mean</code> and
 *         variance <code>variance</code>
 */
public double gauss(double mean, double variance) {
  return rando.nextGaussian() * Math.sqrt(variance) + mean;
}

/**
 * Shuffles an array of integers into random order. Implements Richard
 * Durstenfeld's version of the Fisher-Yates algorithm, popularized by Donald
 * Knuth. see http://en.wikipedia.org/wiki/Fisher-Yates_shuffle
 *
 * @param intArray an array of <code>int</code>s, changed on exit
 */
public void shuffle(int[] intArray) {
  for (int lastPlace = intArray.length - 1; lastPlace > 0; lastPlace--) {
    // Choose a random location from 0..lastPlace
    int randLoc = (int) (random(lastPlace + 1));
    // Swap items in locations randLoc and lastPlace
    int temp = intArray[randLoc];
    intArray[randLoc] = intArray[lastPlace];
    intArray[lastPlace] = temp;
  }
}

public int[] getHistoBounds(int[] source) {
  int min = 255;
  int max = 0;
  for (int i = 0; i < source.length; i++) {
    int[] comp = PixelAudioMapper.rgbComponents(source[i]);
    for (int j = 0; j < comp.length; j++) {
      if (comp[j] > max) max = comp[j];
      if (comp[j] < min) min = comp[j];
    }
  }
  println("--- min", min, " max ", max);
  return new int[]{min, max};
}

// histogram stretch -- run getHistoBounds to determine low and high
public int[] histoStretch(int[] source, int low, int high) {
  int[] out = new int[source.length];
  int r = 0, g = 0, b = 0;
  for (int i = 0; i < out.length; i++) {
    int[] comp = PixelAudioMapper.rgbComponents(source[i]);
    r = comp[0];
    g = comp[1];
    b = comp[2];
    r = (int) constrain(map(r, low, high, 1, 254), 0, 255);
    g = (int) constrain(map(g, low, high, 1, 254), 0, 255);
    b = (int) constrain(map(b, low, high, 1, 254), 0, 255);
    out[i] = PixelAudioMapper.composeColor(r, g, b, 255);
  }
  return out;
}

public void setGamma(float gamma) {
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
    int[] comp = PixelAudioMapper.rgbComponents(source[i]);
    r = comp[0];
    g = comp[1];
    b = comp[2];
    r = gammaTable[r];
    g = gammaTable[g];
    b = gammaTable[b];
    out[i] = PixelAudioMapper.composeColor(r, g, b, 255);
  }
  return out;
}
