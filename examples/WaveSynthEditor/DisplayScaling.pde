/********************************************************************/
/* ----->>>             DISPLAY SCALING METHODS            <<<----- */
/********************************************************************/


/**
 * Get a list of available displays and output information about them to the console.
 * Sets screen2x, screen2y, displayWidth and displayHeight from dimensions of a second display.
 */
void listDisplays() {
  // Get the local graphics environment
  GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
  // Get the array of graphics devices (one for each display)
  GraphicsDevice[] devices = ge.getScreenDevices();
  this.isSecondScreen = (devices.length > 1);
  println("Detected displays:");
  for (int i = 0; i < devices.length; i++) {
    GraphicsDevice device = devices[i];
    // Get the display's configuration
    GraphicsConfiguration config = device.getDefaultConfiguration();
    Rectangle bounds = config.getBounds(); // Screen dimensions and position
    println("Display " + (i + 1) + ":");
    println("  Dimensions: " + bounds.width + " x " + bounds.height);
    println("  Position: " + bounds.x + ", " + bounds.y);
    if (i == 1) {
      // second screen details
      this.screen2x = bounds.x + 8;
      this.screen2y = bounds.y + 8;
      this.displayWidth = bounds.width;
      this.displayHeight = bounds.height;
    }
  }
}

/**
 * Calculates window sizes for displaying mapImage at actual size and at full screen.
 * Press the 'r' key to resize the display window.
 * This method will result in display on a second screen, if one is available.
 * If mapImage is smaller than the screen, mapImage is displayed at size on startup
 * and resizing zooms the image.
 * If mapImage is bigger than the display, mapImage is fit to the screen on startup
 * and resizing shows it at full size, partially filling the window.
 *
 */
public void setScaling() {
  // max window width is a little less than the screen width of the screen
  maxWindowWidth = displayWidth - 80;
  // leave window height some room for title bar, etc.
  maxWindowHeight = displayHeight - 80;
  float sc = maxWindowWidth / (float) mapImage.width;
  scaledWindowHeight = Math.round(mapImage.height * sc);
  if (scaledWindowHeight > maxWindowHeight) {
    sc = maxWindowHeight / (float) mapImage.height;
    scaledWindowHeight = Math.round(mapImage.height * sc);
    scaledWindowWidth = Math.round(mapImage.width * sc);
  } 
  else {
    scaledWindowWidth = Math.round(mapImage.width * sc);
  }
  // even width and height allow ffmpeg to save to video
  scaledWindowWidth += (scaledWindowWidth % 2 != 0) ? 1 : 0;
  scaledWindowHeight += (scaledWindowHeight % 2 != 0) ? 1 : 0;
  isOversize = (mapImage.width > scaledWindowWidth || mapImage.height > scaledWindowHeight);
  windowScale = (1.0f * mapImage.width) / scaledWindowWidth;
  println("maxWindowWidth " + maxWindowWidth + ", maxWindowHeight " + maxWindowHeight);
  println("image width " + mapImage.width + ", image height " + mapImage.height);
  println("scaled width " + scaledWindowWidth + ", scaled height " + scaledWindowHeight + ", "
    + "oversize image is " + isOversize);
}

public void resizeWindow() {
  if (isFitToScreen) {
    surface.setSize(scaledWindowWidth, scaledWindowHeight);
  } else {
    surface.setSize(mapImage.width, mapImage.height);
  }
}

// ------------- END DISPLAY SCALING METHODS ------------- //
