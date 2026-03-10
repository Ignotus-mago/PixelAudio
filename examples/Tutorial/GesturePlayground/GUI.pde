/*----------------------------------------------------------------*/
/*                                                                */
/*                          GUI METHODS                           */
/*                                                                */
/*----------------------------------------------------------------*/

// GUI Variable declarations
GWindow controlWindow;
GPanel controlPanel;
GLabel pathSourceLabel;
GToggleGroup pathSourceGroup;
GOption allOption;
GOption rdpOption;
GOption curveOption;
GSlider rdpEpsilonSlider;
GSlider curvePointsSlider;
GLabel pitchLabel;
GTextField pitchShiftText;
// GLabel timingLabel;
GLabel resampleLabel;
GSlider resampleSlider;
GTextField resampleField;
GLabel durationLabel;
GSlider durationSlider;
GTextField durationField;
GLabel burstLabel;
GSlider burstSlider;
GLabel hopModeLabel;
GToggleGroup hopModeGroup;
GOption gestureOption;
GLabel grainLengthLabel;
GSlider grainLengthSlider;
GLabel hopLengthLabel;
GSlider hopLengthSlider;
GOption fixedOption;
GLabel warpLabel;
GSlider warpSlider;
GLabel epsilonSliderLabel;
GLabel curvePointsLabel;
GLabel gainLabel;
GSlider gainSlider;
GTextField grainLengthField;
GTextField hopLengthField;
GToggleGroup warpGroup;
GOption linearWarpOption;
GOption expWarpOption;
GOption squareRootOption;
GOption customWarpOption;
GOption arcLengthTimeOption;
GLabel envelopeLabel;
GDropList envelopeMenu;


GTextArea commentsField;    // testing

String[] adsrItems = {"Pluck", "Soft", "Percussion", "Fade", "Swell", "Pad"};

/**
 * Create all the GUI controls.
 */
public void createGUI() {
  createControlWindow();
  createControlPanel();
  createControls();
  addControlsToPanel();
}

/**
 * Create a separate window for the GUI control palette.
 */
public void createControlWindow() {
  G4P.messagesEnabled(false);
  G4P.setGlobalColorScheme(GCScheme.BLUE_SCHEME);
  G4P.setMouseOverEnabled(false);
  controlWindow = GWindow.getWindow(this, "Granular Synth", 60, 420, 460, 600, JAVA2D);
  controlWindow.noLoop();
  controlWindow.setActionOnClose(G4P.EXIT_APP);
  controlWindow.addDrawHandler(this, "winDraw");
  controlWindow.addKeyHandler(this, "winKey");
}

/**
 * Create the GUI control palette.
 */
public void createControlPanel() {
  controlPanel = new GPanel(controlWindow, 5, 5, 470, 600, "Settings");
  controlPanel.setCollapsible(false);
  controlPanel.setCollapsed(false);
  controlPanel.setDraggable(false);
  controlPanel.setText("Settings");
  controlPanel.setTextBold();
  // http://lagers.org.uk/g4p/guides/g04-colorschemes.html
  controlPanel.setLocalColor(2, color(254, 246, 233));
  controlPanel.setOpaque(true);
  controlPanel.addEventHandler(this, "controlPanel_hit");
}

// a test
public void createCommentsField() {
  commentsField = new GTextArea(controlWindow, 5, 480, 470, 70, G4P.SCROLLBARS_NONE);
  commentsField.setOpaque(true);
  commentsField.setWrapWidth(460);
  commentsField.addEventHandler(this, "comments_hit");
  commentsField.setText("blah blah blah");
}

/**
 * Create all the controls in the control palette.
 */
public void createControls() {
  int yPos = 30;
  int yInc = 30;
  pathSourceLabel = new GLabel(controlWindow, 10, yPos, 80, 20);
  pathSourceLabel.setText("Path Source");
  pathSourceLabel.setOpaque(true);
  pathSourceGroup = new GToggleGroup();
  allOption = new GOption(controlWindow, 100, yPos, 100, 20);
  allOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  allOption.setText("All Points");
  allOption.setOpaque(false);
  allOption.addEventHandler(this, "allOption_clicked");
  rdpOption = new GOption(controlWindow, 210, yPos, 120, 20);
  rdpOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  rdpOption.setText("Reduced Points");
  rdpOption.setOpaque(false);
  rdpOption.addEventHandler(this, "rdpOption_clicked");
  curveOption = new GOption(controlWindow, 340, yPos, 100, 20);
  curveOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  curveOption.setText("Curve Points");
  curveOption.setOpaque(false);
  curveOption.addEventHandler(this, "curveOption_clicked");
  // epsilon and curvePoints
  yPos += yInc;
  rdpEpsilonSlider = new GSlider(controlWindow, 10, yPos, 200, 40, 10.0f);
  rdpEpsilonSlider.setShowValue(true);
  rdpEpsilonSlider.setShowLimits(true);
  rdpEpsilonSlider.setLimits(2, 1, 50);
  rdpEpsilonSlider.setNbrTicks(8);
  rdpEpsilonSlider.setNumberFormat(G4P.INTEGER, 1);
  rdpEpsilonSlider.setOpaque(false);
  rdpEpsilonSlider.addEventHandler(this, "rdpEpsilonSlider_changed");
  curvePointsSlider = new GSlider(controlWindow, 230, yPos, 200, 40, 10.0f);
  curvePointsSlider.setShowValue(true);
  curvePointsSlider.setShowLimits(true);
  curvePointsSlider.setLimits(2, 2, 128);
  curvePointsSlider.setNumberFormat(G4P.INTEGER, 0);
  curvePointsSlider.setOpaque(false);
  curvePointsSlider.addEventHandler(this, "curvePointsSlider_changed");
  // epsilon and curve labels
  yPos += (yInc + 10);
  epsilonSliderLabel = new GLabel(controlWindow, 10, yPos, 80, 20);
  epsilonSliderLabel.setText("RDP epsilon");
  epsilonSliderLabel.setOpaque(true);
  curvePointsLabel = new GLabel(controlWindow, 230, yPos, 80, 20);
  curvePointsLabel.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
  curvePointsLabel.setText("Curve Points");
  curvePointsLabel.setOpaque(true);
  // pitch
  yPos += yInc + 10;
  pitchLabel = new GLabel(controlWindow, 10, yPos, 140, 20);
  pitchLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
  pitchLabel.setText("Pitch shift (semitones)");
  pitchLabel.setOpaque(true);
  pitchShiftText = new GTextField(controlWindow, 160, yPos, 90, 24, G4P.SCROLLBARS_NONE);
  pitchShiftText.setNumeric(-24.0f, 24.0f, 0.0f);
  pitchShiftText.setText("0.0");
  pitchShiftText.setPromptText("0.0");
  pitchShiftText.setOpaque(true);
  pitchShiftText.addEventHandler(this, "pitchShiftText_changed");
  // gain
  yPos += yInc;
  gainLabel = new GLabel(controlWindow, 10, yPos + 10, 80, 20);
  gainLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
  gainLabel.setText("Gain (dB)");
  gainLabel.setOpaque(true);
  gainSlider = new GSlider(controlWindow, 100, yPos, 210, 40, 10.0f);
  gainSlider.setShowValue(true);
  gainSlider.setShowLimits(true);
  gainSlider.setLimits(1, -60, 6);
  gainSlider.setNbrTicks(24);
  gainSlider.setNumberFormat(G4P.INTEGER, 0);
  gainSlider.setOpaque(false);
  gainSlider.addEventHandler(this, "gainSlider_changed");
  // resampling
  yPos += yInc + 10;
  resampleLabel = new GLabel(controlWindow, 10, yPos + 10, 80, 20);
  resampleLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
  resampleLabel.setText("Resample");
  resampleLabel.setOpaque(true);
  resampleSlider = new GSlider(controlWindow, 100, yPos, 256, 40, 10.0f);
  resampleSlider.setShowValue(false);
  resampleSlider.setLimits(gConfig.resampleCount, 2, 2048);
  resampleSlider.setNumberFormat(G4P.INTEGER, 0);
  resampleSlider.setOpaque(false);
  resampleSlider.addEventHandler(this, "resampleSlider_changed");
  resampleField = new GTextField(controlWindow, 366, yPos + 10, 70, 24, G4P.SCROLLBARS_NONE);
  resampleField.setText("" + gConfig.resampleCount);
  resampleField.setTextEditEnabled(false);
  // duration
  yPos += yInc;
  durationLabel = new GLabel(controlWindow, 10, yPos + 10, 80, 20);
  durationLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
  durationLabel.setText("Duration (ms)");
  durationLabel.setOpaque(true);
  durationSlider = new GSlider(controlWindow, 100, yPos, 256, 40, 10.0f);
  durationSlider.setShowValue(true);
  durationSlider.setLimits(gConfig.targetDurationMs, 50, 16000);
  durationSlider.setNumberFormat(G4P.INTEGER, 0);
  durationSlider.setOpaque(false);
  durationSlider.addEventHandler(this, "durationSlider_changed");
  durationField = new GTextField(controlWindow, 366, yPos + 10, 70, 24, G4P.SCROLLBARS_NONE);
  durationField.setText("" + gConfig.targetDurationMs);
  durationField.setTextEditEnabled(false);
  // hop mode
  yPos += yInc + 20;
  hopModeLabel = new GLabel(controlWindow, 10, yPos, 80, 20);
  hopModeLabel.setText("Hop Mode");
  hopModeLabel.setOpaque(true);
  hopModeGroup = new GToggleGroup();
  gestureOption = new GOption(controlWindow, 100, yPos, 100, 20);
  gestureOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  gestureOption.setText("Gesture");
  gestureOption.setOpaque(false);
  gestureOption.addEventHandler(this, "gestureOption_clicked");
  fixedOption = new GOption(controlWindow, 210, yPos, 100, 20);
  fixedOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  fixedOption.setText("Fixed");
  fixedOption.setOpaque(false);
  fixedOption.addEventHandler(this, "fixedOption_clicked");
  // burst grain count
  yPos += yInc;
  burstLabel = new GLabel(controlWindow, 10, yPos + 10, 80, 20);
  burstLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
  burstLabel.setText("Burst count");
  burstLabel.setOpaque(true);
  burstSlider = new GSlider(controlWindow, 100, yPos, 256, 40, 10.0f);
  burstSlider.setShowValue(true);
  burstSlider.setShowLimits(true);
  burstSlider.setLimits(1, 1, 16);
  burstSlider.setNumberFormat(G4P.INTEGER, 0);
  burstSlider.setOpaque(false);
  //burstSlider.setNbrTicks(16);
  //burstSlider.setShowTicks(true);
  //burstSlider.setStickToTicks(true);
  burstSlider.addEventHandler(this, "burstSlider_changed");
  // grain length
  yPos += yInc + 8;
  grainLengthLabel = new GLabel(controlWindow, 10, yPos + 10, 80, 20);
  grainLengthLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
  grainLengthLabel.setText("Grain length");
  grainLengthLabel.setOpaque(true);
  grainLengthSlider = new GSlider(controlWindow, 100, yPos, 256, 40, 10.0f);
  grainLengthSlider.setShowValue(false);
  grainLengthSlider.setShowLimits(true);
  grainLengthSlider.setLimits(2048, 64, 4096);
  grainLengthSlider.setNumberFormat(G4P.INTEGER, 0);
  grainLengthSlider.setOpaque(false);
  grainLengthSlider.addEventHandler(this, "grainLengthSlider_changed");
  grainLengthField = new GTextField(controlWindow, 366, yPos + 10, 70, 24, G4P.SCROLLBARS_NONE);
  grainLengthField.setText("2048");
  grainLengthField.setOpaque(true);
  grainLengthField.setTextEditEnabled(false);
  grainLengthField.setText(""+ gConfig.grainLengthSamples);
  // hop length
  yPos += yInc;
  hopLengthLabel = new GLabel(controlWindow, 10, yPos + 10, 80, 20);
  hopLengthLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
  hopLengthLabel.setText("Hop length");
  hopLengthLabel.setOpaque(true);
  hopLengthSlider = new GSlider(controlWindow, 100, yPos, 256, 40, 10.0f);
  hopLengthSlider.setShowValue(false);
  hopLengthSlider.setShowLimits(true);
  hopLengthSlider.setLimits(512, 64, 4096);
  hopLengthSlider.setNumberFormat(G4P.INTEGER, 0);
  hopLengthSlider.setOpaque(false);
  hopLengthSlider.addEventHandler(this, "hopLengthSlider_changed");
  hopLengthField = new GTextField(controlWindow, 366, yPos + 10, 70, 24, G4P.SCROLLBARS_NONE);
  hopLengthField.setText("512");
  hopLengthField.setOpaque(true);
  hopLengthField.setTextEditEnabled(false);
  hopLengthField.setText(""+ gConfig.hopLengthSamples);
  // warp
  yPos += yInc + 10;
  warpLabel = new GLabel(controlWindow, 10, yPos + 10, 80, 20);
  warpLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
  warpLabel.setText("Warp");
  warpLabel.setOpaque(true);
  // warp options
  yPos += 10;
  warpGroup = new GToggleGroup();
  linearWarpOption = new GOption(controlWindow, 100, yPos, 80, 20);
  linearWarpOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  linearWarpOption.setText("Linear");
  linearWarpOption.setOpaque(false);
  linearWarpOption.setSelected(true);
  linearWarpOption.addEventHandler(this, "linearWarpOption_clicked");
  expWarpOption = new GOption(controlWindow, 190, yPos, 80, 20);
  expWarpOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  expWarpOption.setText("Exp");
  expWarpOption.setOpaque(false);
  expWarpOption.addEventHandler(this, "expWarpOption_clicked");
  squareRootOption = new GOption(controlWindow, 280, yPos, 80, 20);
  squareRootOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  squareRootOption.setText("Root");
  squareRootOption.setOpaque(false);
  squareRootOption.addEventHandler(this, "squareRootOption_clicked");
  customWarpOption = new GOption(controlWindow, 370, yPos, 80, 20);
  customWarpOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  customWarpOption.setText("Custom");
  customWarpOption.setOpaque(false);
  customWarpOption.addEventHandler(this, "customWarpOption_clicked");
  // warp exponent
  yPos += yInc;
  warpSlider = new GSlider(controlWindow, 100, yPos - 10, 256, 40, 10.0f);
  warpSlider.setShowValue(true);
  warpSlider.setLimits(1f, 0.25f, 4f);
  warpSlider.setNumberFormat(G4P.DECIMAL, 2);
  warpSlider.setOpaque(false);
  warpSlider.addEventHandler(this, "warpSlider_changed");
  warpSlider.setVisible(true);
  warpSlider.setEnabled(false); // only if LINEAR is selected option
  // envelope menu
  yPos += yInc;
  envelopeLabel = new GLabel(controlWindow, 10, yPos, 80, 40);
  envelopeLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
  envelopeLabel.setText("Sampler\nEnvelope");
  envelopeLabel.setOpaque(true);
  envelopeMenu = new GDropList(controlWindow, 100, yPos, 120, 80, 3, 10);
  envelopeMenu.setItems(adsrItems, 0);
  envelopeMenu.addEventHandler(this, "envelopeMenu_clicked");
  // arc length time option3

  arcLengthTimeOption = new GOption(controlWindow, 280, yPos, 120, 20);
  arcLengthTimeOption.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  arcLengthTimeOption.setText("Arc Length Time");
  arcLengthTimeOption.setOpaque(false);
  arcLengthTimeOption.setSelected(false);
  arcLengthTimeOption.addEventHandler(this, "arcLengthTimeOption_clicked");
  // not showing this option, which only applies to curves and has minimal effect
  arcLengthTimeOption.setVisible(false);
}

/**
 * Add the controls to the control palette.
 */
public void addControlsToPanel() {
  controlPanel.setVisible(true);
  controlPanel.addControl(pathSourceLabel);
  // add path source controls
  pathSourceGroup.addControl(allOption);
  allOption.setSelected(true);
  controlPanel.addControl(allOption);
  pathSourceGroup.addControl(rdpOption);
  controlPanel.addControl(rdpOption);
  pathSourceGroup.addControl(curveOption);
  controlPanel.addControl(curveOption);
  // epsilon
  controlPanel.addControl(epsilonSliderLabel);
  controlPanel.addControl(rdpEpsilonSlider);
  controlPanel.addControl(curvePointsLabel);
  controlPanel.addControl(curvePointsSlider);
  // hpp mode
  controlPanel.addControl(hopModeLabel);
  hopModeGroup.addControl(gestureOption);
  gestureOption.setSelected(true);
  controlPanel.addControl(gestureOption);
  hopModeGroup.addControl(fixedOption);
  controlPanel.addControl(fixedOption);
  // burst count
  controlPanel.addControl(burstLabel);
  controlPanel.addControl(burstSlider);
  // grain length
  controlPanel.addControl(grainLengthLabel);
  controlPanel.addControl(grainLengthSlider);
  controlPanel.addControl(grainLengthField);
  // hop length
  controlPanel.addControl(hopLengthLabel);
  controlPanel.addControl(hopLengthSlider);
  controlPanel.addControl(hopLengthField);
  // pitch shift
  controlPanel.addControl(pitchLabel);
  controlPanel.addControl(pitchShiftText);
  // gain
  controlPanel.addControl(gainLabel);
  controlPanel.addControl(gainSlider);
  // resample
  controlPanel.addControl(resampleLabel);
  controlPanel.addControl(resampleSlider);
  controlPanel.addControl(resampleField);
  // change duration
  controlPanel.addControl(durationLabel);
  controlPanel.addControl(durationSlider);
  controlPanel.addControl(durationField);
  // warp
  controlPanel.addControl(warpLabel);
  warpGroup.addControl(linearWarpOption);
  warpGroup.addControl(expWarpOption);
  warpGroup.addControl(squareRootOption);
  warpGroup.addControl(customWarpOption);
  linearWarpOption.setSelected(true);
  controlPanel.addControl(linearWarpOption);
  controlPanel.addControl(expWarpOption);
  controlPanel.addControl(squareRootOption);
  controlPanel.addControl(customWarpOption);
  controlPanel.addControl(warpSlider);
  // envelope
  controlPanel.addControl(envelopeLabel);
  controlPanel.addControl(envelopeMenu);
  controlPanel.setCollapsed(false);
  // arcLengthTime
  controlPanel.addControl(arcLengthTimeOption);
}


// GUI Handlers
/*
   * Conditional enabling of handlers
 *
 * Every handler gets: "if (!isEditable()) return;" -- mode == Mode.PLAY_ONLY
 * Every handler gets: "if (guiSyncing) return;"    -- sync is active
 *
 * Global granular defaults include: "if (mode != Mode.DRAW_EDIT_GRANULAR) return;"
 *   grainLengthSlider_changed, hopLengthSlider_changed
 *   gainSlider_changed
 *   pitchShiftText_changed
 *   linearWarpOption_clicked, expWarpOption_clicked, squareRootOption_clicked, customWarpOption_clicked
 *   warpSlider_changed
 *   envelopeMenu_clicked
 *
 * Handlers for brushstroke parameters include: "if (activeGranularBrush == null) return;"
 *   Path mode options: allOption_clicked, rdpOption_clicked, curveOption_clicked
 *   Path params: rdpEpsilonSlider_changed, curvePointsSlider_changed
 *   Hop mode options: gestureOption_clicked, fixedOption_clicked
 *   Timing overrides: resampleSlider_changed, durationSlider_changed
 *
 */

// our draw method, call each time through the event loop
synchronized public void winDraw(PApplet appc, GWinData data) {
  appc.background(color(212, 220, 228));
}

// respond to key in window
public void winKey(PApplet appc, GWinData data, KeyEvent evt) {
  if (evt.getAction() == KeyEvent.RELEASE) parseKey(evt.getKey(), evt.getKeyCode());
}


public void controlPanel_hit(GPanel source, GEvent event) {
  // println("controlPanel - GPanel >> GEvent." + event + " @ " + millis());
}

public void allOption_clicked(GOption source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  /// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
  if (activeBrush == null) return;
  gConfig.pathMode = GestureGranularConfig.PathMode.ALL_POINTS;
  recomputeUIBaselinesFromActiveBrush();
  syncGuiFromConfig();
  if (isVerbose) println("gConfig.pathMode = "+ gConfig.pathMode.toString());
}

public void rdpOption_clicked(GOption source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  /// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
  if (activeBrush == null) return;
  gConfig.pathMode = GestureGranularConfig.PathMode.REDUCED_POINTS;
  recomputeUIBaselinesFromActiveBrush();
  syncGuiFromConfig();
  if (isVerbose) println("gConfig.pathMode = "+ gConfig.pathMode.toString());
}

public void curveOption_clicked(GOption source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  /// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
  if (activeBrush == null) return;
  gConfig.pathMode = GestureGranularConfig.PathMode.CURVE_POINTS;
  recomputeUIBaselinesFromActiveBrush();
  syncGuiFromConfig();
  if (isVerbose) println("gConfig.pathMode = "+ gConfig.pathMode.toString());
}

public void rdpEpsilonSlider_changed(GSlider source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  /// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
  if (activeBrush == null) return;
  gConfig.rdpEpsilon = source.getValueF();
  if (event == GEvent.RELEASED) {
    recomputeUIBaselinesFromActiveBrush();
    syncGuiFromConfig();
    if (isVerbose) println("gConfig.rdpEpsilon = "+ gConfig.rdpEpsilon);
  }
}

public void curvePointsSlider_changed(GSlider source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  /// if (mode != Mode.DRAW_EDIT_GRANULAR) return;
  if (activeBrush == null) return;
  gConfig.curveSteps = source.getValueI();
  if (event == GEvent.RELEASED) {
    recomputeUIBaselinesFromActiveBrush();
    syncGuiFromConfig();
    if (isVerbose) println("gConfig.curveSteps = "+ gConfig.curveSteps);
  }
}

public void gestureOption_clicked(GOption source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  if (drawingMode != DrawingMode.DRAW_EDIT_GRANULAR) return;
  if (activeBrush == null) return;
  gConfig.hopMode = GestureGranularConfig.HopMode.GESTURE;
  recomputeUIBaselinesFromActiveBrush();
  syncGuiFromConfig();
  if (isVerbose) println("gConfig.hopMode = "+ gConfig.hopMode);
}

public void fixedOption_clicked(GOption source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  if (drawingMode != DrawingMode.DRAW_EDIT_GRANULAR) return;
  if (activeBrush == null) return;
  gConfig.hopMode = GestureGranularConfig.HopMode.FIXED;
  recomputeUIBaselinesFromActiveBrush();
  syncGuiFromConfig();
  if (isVerbose) println("gConfig.hopMode = "+ gConfig.hopMode);
}

public void burstSlider_changed(GSlider source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  if (drawingMode != DrawingMode.DRAW_EDIT_GRANULAR) return;
  if (activeBrush == null) return;
  int v = source.getValueI();
  gConfig.burstGrains = v;
  if (event == GEvent.RELEASED) {
    if (isVerbose) println("gConfig.burstGrains = " + gConfig.burstGrains + " (slider=" + v + ")");
  }
}

public void resampleSlider_changed(GSlider source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  // if (mode != Mode.DRAW_EDIT_GRANULAR) return;
  if (activeBrush == null) return;
  int v = source.getValueI();
  if (v == baselineCount) {
    gConfig.resampleCount = 0;
  } else {
    gConfig.resampleCount = v;
  }
  resampleField.setText(""+ gConfig.resampleCount);
  if (event == GEvent.RELEASED) {
    if (isVerbose) println("gConfig.resampleCount = " + gConfig.resampleCount + " (slider=" + v + ")");
  }
}

public void durationSlider_changed(GSlider source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  // if (mode != Mode.DRAW_EDIT_GRANULAR) return;
  if (activeBrush == null) return;
  int v = source.getValueI();
  if (v == baselineDurationMs) {
    gConfig.targetDurationMs = 0;
  } else {
    gConfig.targetDurationMs = v;
  }
  durationField.setText("" + v);
  if (event == GEvent.RELEASED) {
    if (isVerbose) println("gConfig.targetDurationMs = " + gConfig.targetDurationMs + " (slider=" + v + ")");
  }
}

public void grainLengthSlider_changed(GSlider source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  if (drawingMode != DrawingMode.DRAW_EDIT_GRANULAR) return;
  int raw = source.getValueI();
  int quant = quantizeToStep(raw, 16);
  if (quant != raw) source.setValue(quant);
  gConfig.grainLengthSamples = quant;
  grainLengthField.setText(""+ gConfig.grainLengthSamples);
  if (event == GEvent.RELEASED) if (isVerbose) println("gConfig.grainLengthSamples = "+ gConfig.grainLengthSamples);
}

public void hopLengthSlider_changed(GSlider source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  if (drawingMode != DrawingMode.DRAW_EDIT_GRANULAR) return;
  int raw = source.getValueI();
  int quant = quantizeToStep(raw, 16);
  source.setValue(quant);
  gConfig.hopLengthSamples = quant;
  hopLengthField.setText(""+ gConfig.hopLengthSamples);
  if (event == GEvent.RELEASED) {
    recomputeUIBaselinesFromActiveBrush();
    syncGuiFromConfig();
    if (isVerbose) println("gConfig.hopLengthSamples = "+ gConfig.hopLengthSamples);
  }
}

// this method was not linked to grainLengthField and will not be called
public void grainLengthField_changed(GTextField source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  gConfig.grainLengthSamples = source.getValueI();
  if (isVerbose) println("grainLengthField - GTextField >> GEvent." + event + " @ " + millis());
}

// this method was not linked to hopLengthField and will not be called
public void hopLengthField_changed(GTextField source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  gConfig.hopLengthSamples = source.getValueI();
  if (isVerbose) println("hopLengthField - GTextField >> GEvent." + event + " @ " + millis());
}

public void pitchShiftText_changed(GTextField source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  // if (mode != Mode.DRAW_EDIT_GRANULAR) return;
  float v = source.getValueF();
  if (v != gConfig.pitchSemitones) {
    gConfig.pitchSemitones = source.getValueF();
    if (isVerbose) println("gConfig.pitchSemitones = "+ gConfig.pitchSemitones);
  }
}

public void gainSlider_changed(GSlider source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  // if (mode != Mode.DRAW_EDIT_GRANULAR) return;
  gConfig.gainDb = source.getValueF();
  if (event == GEvent.LOST_FOCUS) if (isVerbose) println("gConfig.gainDb = "+ gConfig.gainDb);
}

public void linearWarpOption_clicked(GOption source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  if (drawingMode != DrawingMode.DRAW_EDIT_GRANULAR) return;
  gConfig.warpShape = GestureGranularConfig.WarpShape.LINEAR;
  warpSlider.setEnabled(gConfig.warpShape != GestureGranularConfig.WarpShape.LINEAR);
  if (isVerbose) println("gConfig.warpShape = "+ gConfig.warpShape.toString());
}

public void expWarpOption_clicked(GOption source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  if (drawingMode != DrawingMode.DRAW_EDIT_GRANULAR) return;
  gConfig.warpShape = GestureGranularConfig.WarpShape.EXP;
  warpSlider.setEnabled(gConfig.warpShape != GestureGranularConfig.WarpShape.LINEAR);
  if (isVerbose) println("gConfig.warpShape = "+ gConfig.warpShape.toString());
}

public void squareRootOption_clicked(GOption source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  if (drawingMode != DrawingMode.DRAW_EDIT_GRANULAR) return;
  gConfig.warpShape = GestureGranularConfig.WarpShape.SQRT;
  warpSlider.setEnabled(gConfig.warpShape != GestureGranularConfig.WarpShape.LINEAR);
  if (isVerbose) println("gConfig.warpShape = "+ gConfig.warpShape.toString());
}

public void customWarpOption_clicked(GOption source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  if (drawingMode != DrawingMode.DRAW_EDIT_GRANULAR) return;
  gConfig.warpShape = GestureGranularConfig.WarpShape.CUSTOM;
  warpSlider.setEnabled(gConfig.warpShape != GestureGranularConfig.WarpShape.LINEAR);
  if (isVerbose) println("gConfig.warpShape = "+ gConfig.warpShape.toString());
}

public void warpSlider_changed(GSlider source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  if (drawingMode != DrawingMode.DRAW_EDIT_GRANULAR) return;
  gConfig.warpExponent = source.getValueF();
  if (event == GEvent.RELEASED) println("gConfig.warpExponent = "+ gConfig.warpExponent);
}

public void arcLengthTimeOption_clicked(GOption source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  gConfig.useArcLengthTime  = source.isSelected();
  // gConfig.useArcLengthTime = ;
}

public void envelopeMenu_clicked(GDropList source, GEvent event) {
  if (!isEditable()) return;
  if (guiSyncing) return;
  if (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR) {
    return;
  }
  // if (mode != Mode.DRAW_EDIT_GRANULAR) return;
  int itemHit = source.getSelectedIndex();
  String itemName = adsrItems[itemHit];
  println("-- envelope "+ itemName +" selected");
  gConfig.env = envPreset(itemName);
  if (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER) samplerEnv = envPreset(itemName);
  // println("envelopeMenu - GDropList >> GEvent." + event + " @ " + millis());
}

public void printGConfigStatus() {
  // GestureGranularConfig has a toString() method, so far GestureGranularConfig.Builder does not
  String status = gConfig.build().toString();
  if (isVerbose) println(status);
}


// GUI Helpers

/**
 * @param name   the name of the ADSRParams envelope to return
 * @return the specified ADSRParams envelope
 */
static ADSRParams envPreset(String name) {
  switch (name) {
  case "Pluck"      :
    return new ADSRParams(1.0f, 0.005f, 0.18f, 0.0f, 0.12f);
  case "Soft"       :
    return new ADSRParams(1.0f, 0.020f, 0.60f, 0.15f, 0.25f); // new
  case "Percussion" :
    return new ADSRParams(1.0f, 0.002f, 0.20f, 0.10f, 0.18f);
  case "Fade"       :
    return new ADSRParams(1.0f, 0.05f, 0.0f, 1.0f, 0.50f);   // your trapezoid
  case "Swell"      :
    return new ADSRParams(1.0f, 0.90f, 0.40f, 0.70f, 0.90f);
  case "Pad"        :
    return new ADSRParams(1.0f, 1.40f, 0.60f, 0.85f, 1.80f);
  default           :
    return new ADSRParams(1.0f, 0.01f, 0.15f, 0.75f, 0.25f);
  }
}

/** Quantize an integer to the nearest multiple of step. */
public static int quantizeToStep(int value, int step) {
  int half = step / 2;
  return ((value + half) / step) * step;
}

/**
 * Synchronize the control palette knobs to the current gConfig, probably
 * because a brush was selected and made active.
 */
void syncGuiFromConfig() {
  guiSyncing = true;
  try {
    boolean hasStroke = (activeBrush != null);
    boolean canEdit = hasStroke && (isEditable());
    setControlsEnabled();

    // PathMode radio
    allOption.setSelected(gConfig.pathMode == GestureGranularConfig.PathMode.ALL_POINTS);
    rdpOption.setSelected(gConfig.pathMode == GestureGranularConfig.PathMode.REDUCED_POINTS);
    curveOption.setSelected(gConfig.pathMode == GestureGranularConfig.PathMode.CURVE_POINTS);

    // epsilon / curveSteps
    rdpEpsilonSlider.setValue(gConfig.rdpEpsilon);
    curvePointsSlider.setValue(gConfig.curveSteps);

    // hop mode radio
    gestureOption.setSelected(gConfig.hopMode == GestureGranularConfig.HopMode.GESTURE);
    fixedOption.setSelected(gConfig.hopMode == GestureGranularConfig.HopMode.FIXED);

    // burst count slider
    burstSlider.setValue(gConfig.burstGrains);

    // grain/hop length sliders + fields
    grainLengthSlider.setValue(gConfig.grainLengthSamples);
    grainLengthField.setText("" + gConfig.grainLengthSamples);

    hopLengthSlider.setValue(gConfig.hopLengthSamples);
    hopLengthField.setText("" + gConfig.hopLengthSamples);

    // gain/pitch
    gainSlider.setValue(gConfig.gainDb);
    pitchShiftText.setText(String.format("%.2f", gConfig.pitchSemitones));

    // ---- Resample & Duration: show baseline unless overridden ----
    int baseCountSafe = Math.max(2, baselineCount);         // slider min is 2
    int shownCount = (gConfig.resampleCount == 0) ? baseCountSafe : gConfig.resampleCount;
    shownCount = clampInt(shownCount, 2, 2048);
    resampleSlider.setLimits(shownCount, 2, 2048);
    resampleField.setText("" + shownCount);

    int baseDurSafe = Math.max(50, baselineDurationMs);     // slider min is 50
    int shownDur = (gConfig.targetDurationMs == 0) ? baseDurSafe : gConfig.targetDurationMs;
    shownDur = clampInt(shownDur, 50, 10000);
    durationSlider.setLimits(shownDur, 50, 16000);
    durationField.setText("" + shownDur);

    // warp radio + exponent
    linearWarpOption.setSelected(gConfig.warpShape == GestureGranularConfig.WarpShape.LINEAR);
    expWarpOption.setSelected(gConfig.warpShape == GestureGranularConfig.WarpShape.EXP);
    squareRootOption.setSelected(gConfig.warpShape == GestureGranularConfig.WarpShape.SQRT);
    customWarpOption.setSelected(gConfig.warpShape == GestureGranularConfig.WarpShape.CUSTOM);

    warpSlider.setValue(gConfig.warpExponent);

    // warp slider only editable when editEnabled AND warp is active
    warpSlider.setEnabled(canEdit && gConfig.warpShape != GestureGranularConfig.WarpShape.LINEAR);

    // (Optional) also disable warp radio buttons outside edit mode:
    linearWarpOption.setEnabled(canEdit);
    expWarpOption.setEnabled(canEdit);
    squareRootOption.setEnabled(canEdit);
    customWarpOption.setEnabled(canEdit);
  }
  finally {
    guiSyncing = false;
  }
}

static int clampInt(int v, int lo, int hi) {
  return (v < lo) ? lo : (v > hi ? hi : v);
}

/**
 * Determine which controls to enable, based on the drawing mode.
 */
void setControlsEnabled() {
  // ---- Mode flags ----
  final boolean inGranularEdit = (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR);
  final boolean inSamplerEdit  = (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER);
  final boolean inPlayOnly     = (drawingMode == DrawingMode.PLAY_ONLY);
  // Editable only in the two DRAW_EDIT modes
  final boolean canEdit = inGranularEdit || inSamplerEdit;
  // Brush selection
  final boolean hasBrush = (activeBrush != null);
  final boolean brushIsGranular = hasBrush && (activeBrush instanceof GranularBrush);
  final boolean brushIsSampler  = hasBrush && (activeBrush instanceof SamplerBrush);
  // PLAY_ONLY visibility rule:
  // - if no brush selected: show everything (disabled)
  // - if brush selected: show only relevant controls (disabled)
  final boolean showGranular = inGranularEdit || (inPlayOnly && (!hasBrush || brushIsGranular));
  final boolean showSampler  = inSamplerEdit  || (inPlayOnly && (!hasBrush || brushIsSampler));
  // Stroke-dependent controls: only enabled when editing AND a brush is selected
  final boolean strokeEditable = canEdit && hasBrush;

  // ---------- Stroke-dependent controls (shared) ----------
  allOption.setEnabled(strokeEditable);
  rdpOption.setEnabled(strokeEditable);
  curveOption.setEnabled(strokeEditable);
  rdpEpsilonSlider.setEnabled(strokeEditable);
  curvePointsSlider.setEnabled(strokeEditable);
  // Gesture timing option is relevant for both brush types, but only editable with a selected brush
  gestureOption.setEnabled(strokeEditable);
  // Fixed timing is Granular-only *global* default (carried to next GranularBrush)
  fixedOption.setVisible(showGranular);
  fixedOption.setEnabled(inGranularEdit);
  // If we're in Sampler context, ensure gesture timing is selected (sampler never uses fixed)
  if (inSamplerEdit || (inPlayOnly && brushIsSampler)) {
    gestureOption.setSelected(true);
  }
  // Schedule controls (shared)
  resampleSlider.setEnabled(strokeEditable);
  durationSlider.setEnabled(strokeEditable);
  // Fields are display-only (non-editable). Keep enabled for legibility/select/select.
  resampleField.setEnabled(true);
  durationField.setEnabled(true);

  // ---------- Granular-only synth controls ----------
  burstSlider.setVisible(showGranular);
  burstSlider.setEnabled(inGranularEdit);
  grainLengthSlider.setVisible(showGranular);
  grainLengthSlider.setEnabled(inGranularEdit);
  hopLengthSlider.setVisible(showGranular);
  hopLengthSlider.setEnabled(inGranularEdit);
  grainLengthField.setVisible(showGranular);
  grainLengthField.setEnabled(true);
  hopLengthField.setVisible(showGranular);
  hopLengthField.setEnabled(true);
  // Warp options
  linearWarpOption.setVisible(showGranular);
  linearWarpOption.setEnabled(inGranularEdit);
  expWarpOption.setVisible(showGranular);
  expWarpOption.setEnabled(inGranularEdit);
  squareRootOption.setVisible(showGranular);
  squareRootOption.setEnabled(inGranularEdit);
  customWarpOption.setVisible(showGranular);
  customWarpOption.setEnabled(inGranularEdit);
  warpSlider.setVisible(showGranular);
  final boolean warpActive =
    (gConfig != null && gConfig.warpShape != GestureGranularConfig.WarpShape.LINEAR);
  warpSlider.setEnabled(inGranularEdit && warpActive);

  // ---------- Sampler-only synth controls ----------
  // Envelope menu is sampler-only in your current UX (visible in sampler edit and play-only sampler context).
  envelopeMenu.setVisible(showSampler);
  envelopeMenu.setEnabled(inSamplerEdit && canEdit);

  // ---------- Global synth controls (shared) ----------
  // Gain/pitch are editable in either edit mode, read-only in play-only.
  gainSlider.setEnabled(canEdit);
  pitchShiftText.setEnabled(canEdit);

  // If you have any other always-visible labels, they are intentionally not handled here.
}

boolean isEditable() {
  return (drawingMode == DrawingMode.DRAW_EDIT_GRANULAR) || (drawingMode == DrawingMode.DRAW_EDIT_SAMPLER);
}

void resetToDefaults() {
  if (!isEditable()) return;
  // Reset global
  gConfig.copyFrom(defaultGranConfig);
  // If a stroke is active, reset its per-stroke cfg too
  if (activeBrush != null) {
    activeBrush.cfg().copyFrom(defaultGranConfig);
    gConfig = activeBrush.cfg();
    recomputeUIBaselinesFromActiveBrush();
  } else {
    // no stroke: baselines don't apply
    baselineCount = 0;
    baselineDurationMs = 0;
  }
  syncGuiFromConfig();
}
