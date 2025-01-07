/* ------------------------------------------------------------- */
/* ----->>>              BEGIN G4P GUI                  <<<----- */
/* ------------------------------------------------------------- */

// G4P GUI 
// Global controls
GWindow controlWindow;
GTextArea commentsField;
GPanel globalPanel;
GTextField blendField; 
GLabel blend_label; 
GTextField gammaField; 
GLabel gamma_label; 
GTextField noiseField;
GLabel noise_label;
GSpinner stepsSpinner;
GLabel steps_label; 
GSpinner stopSpinner;
GLabel stop_label;
GCheckbox histoCheck; 
GTextField histoHighField; 
GLabel histoHigh_label; 
GTextField histoLowField; 
GLabel histoLow_label; 
// GToggleGroup scanTog; 
// GOption hilbertOption; 
// GOption mooreOption; 
GLabel fps_label; 
GDropList fpsMenu; 
GCheckbox recordCheck;
GButton refreshBtn;
GSpinner capSpinner;
GLabel cap_label;
GCheckbox capCheck;
GButton runVideoBtn;
GTextField videoNameField; 
GLabel videoName_label; 
GButton openBtn;
GButton saveBtn; 
GButton saveAsBtn; 

// WaveDataPanel controls
GPanel waveDataPanel; 
GTextField freqField; 
GLabel freq_label; 
GTextField ampField; 
GLabel amp_label; 
GTextField phaseField; 
GLabel phase_label; 
GTextField cyclesField; 
GLabel cycles_label; 
GTextField dcField;
GLabel dc_label;
GButton prevBtn; 
GButton nextBtn; 
GLabel waveDataLabel; 
GCheckbox muteWave; 
GCheckbox soloWave; 
GButton newWaveBtn; 
GButton dupWaveBtn; 
GButton delWaveBtn; 
// Controls used for color chooser dialog GUI 
GButton btnColor;
GView colorView;
GLabel colorTitle;
PGraphics colorPG;
int sel_col = -1;

/* ----->>> initialize GUI and control window <<<----- */
/* ----->>> initialize currentWD and wavesynth.waveDataList before setting up the GUI  <<<----- */

/**
 * Initialize GUI and control window -- initialize wavesynth before calling this method.
 */
public void createGUI() {
  createControlWindow();
  initGlobalPanel();
  createGlobalControls();
  buildGlobalPanel();
  loadGlobalPanelValues();
  initWaveDataPanel();
  createWaveDataControls();
  buildWaveDataPanel();
  loadWaveDataPanelValues(currentWD);
  // get crackin'
  controlWindow.loop();
}

/********************************************************************/
/* ----->>>                CONTROL WINDOW                  <<<----- */
/********************************************************************/

/* ----->>> set up GUI and initialize the control window <<<----- */
public void createControlWindow() {
  G4P.messagesEnabled(false);
  G4P.setGlobalColorScheme(GCScheme.BLUE_SCHEME);
  G4P.setMouseOverEnabled(false);
  surface.setTitle("Animation Window");
  surface.setLocation(60, 20);
  if (isSecondScreen) surface.setLocation(screen2x, screen2y); // on second screen
  controlWindow = GWindow.getWindow(this, "Data Fields", 60, 420, 480, 560, JAVA2D);
  controlWindow.noLoop();
  controlWindow.setActionOnClose(G4P.KEEP_OPEN);
  controlWindow.addDrawHandler(this, "winDraw");
  // ignore mouse and key events in our control panel, they just get in the way of other events
  //controlWindow.addMouseHandler(this, "winMouse");
  //controlWindow.addKeyHandler(this, "winKey");
  // we don't need these events
  //controlWindow.addPreHandler(this, "winPre");
  //controlWindow.addPostHandler(this, "winPost");
  //controlWindow.addOnCloseHandler(this, "winClose");
  createCommentsField();
}

public void createCommentsField() {
  commentsField = new GTextArea(controlWindow, 5, 480, 470, 70, G4P.SCROLLBARS_NONE);
  commentsField.setOpaque(true);
  commentsField.setWrapWidth(460);
  commentsField.addEventHandler(this, "comments_hit");
  commentsField.setText(comments);
}

/********************************************************************/
/* ----->>>                 GLOBAL PANEL                   <<<----- */
/********************************************************************/

/* ----->>> initialize global panel <<<----- */
public void initGlobalPanel() {
  globalPanel = new GPanel(controlWindow, 5, 5, 230, 470, "Globals");
  globalPanel.setCollapsed(false);
  globalPanel.setCollapsible(false);
  globalPanel.setDraggable(false);
  globalPanel.setText("Globals");
  globalPanel.setOpaque(true);
  globalPanel.addEventHandler(this, "globalPanel_hit");
}

/* ----->>> add controls to global panel <<<----- */
public void createGlobalControls() {
  int ypos = 30;
  int yinc = 30;
  /* ----->>>  <<<----- */
  blendField = new GTextField(controlWindow, 10, ypos, 120, 20, G4P.SCROLLBARS_NONE);
  blendField.setNumeric(0.01f, 10.0f, 0.5f);
  blendField.setOpaque(true);
  blendField.addEventHandler(this, "blendField_hit");
  blend_label = new GLabel(controlWindow, 140, ypos, 80, 20);
  blend_label.setText("Blend");
  blend_label.setOpaque(false);
  /* ----->>>  <<<----- */
  ypos += yinc;
  gammaField = new GTextField(controlWindow, 10, ypos, 120, 20, G4P.SCROLLBARS_NONE);
  gammaField.setNumeric(0.1f, 10.0f, 0.5f);
  gammaField.setOpaque(true);
  gammaField.addEventHandler(this, "gammaField_hit");
  gamma_label = new GLabel(controlWindow, 140, ypos, 80, 20);
  gamma_label.setText("Gamma");
  gamma_label.setOpaque(false);
  /* ----->>>  <<<----- */
  ypos += yinc;
  /* space for additional controls */
  /* ----->>>  <<<----- */
  ypos += yinc;
  stepsSpinner = new GSpinner(controlWindow, 10, ypos, 120, 20);
  stepsSpinner.setLimits(240, 8, 24000, 1);
  stepsSpinner.addEventHandler(this, "stepsSpinner_hit");
  steps_label = new GLabel(controlWindow, 140, ypos, 80, 20);
  steps_label.setText("Steps");
  steps_label.setOpaque(false);
  /* ----->>>  <<<----- */
  ypos += yinc;
  stopSpinner = new GSpinner(controlWindow, 10, ypos, 120, 20);
  stopSpinner.setLimits(240, 8, 36000, 1);
  stopSpinner.addEventHandler(this, "stopSpinner_hit");
  stop_label = new GLabel(controlWindow, 140, ypos, 80, 20);
  stop_label.setText("Stop frame");
  stop_label.setOpaque(false);
  /* ----->>>  <<<----- */
  ypos += yinc;
  histoCheck = new GCheckbox(controlWindow, 10, ypos, 120, 20);
  histoCheck.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  histoCheck.setText("Scale histogram");
  histoCheck.setOpaque(false);
  histoCheck.addEventHandler(this, "histoCheck_hit");
  /* ----->>>  <<<----- */
  ypos += yinc;
  histoHighField = new GTextField(controlWindow, 10, ypos, 50, 20, G4P.SCROLLBARS_NONE);
  histoHighField.setNumeric(255, 128, 233);
  histoHighField.setOpaque(true);
  histoHighField.addEventHandler(this, "histoHigh_hit");
  histoHigh_label = new GLabel(controlWindow, 70, ypos, 40, 20);
  histoHigh_label.setText("High");
  histoHigh_label.setOpaque(false);
  /* ----->>>  <<<----- */
  histoLowField = new GTextField(controlWindow, 120, ypos, 50, 20, G4P.SCROLLBARS_NONE);
  histoLowField.setNumeric(128, 0, 8);
  histoLowField.setOpaque(true);
  histoLowField.addEventHandler(this, "histoLow_hit");
  histoLow_label = new GLabel(controlWindow, 180, ypos, 40, 20);
  histoLow_label.setText("Low");
  histoLow_label.setOpaque(false);
  /* ----->>>  <<<----- */
  ypos += yinc;
  /* space for additional controls */
  /* ----->>>  <<<----- */
  ypos += yinc;
  fpsMenu = new GDropList(controlWindow, 10, ypos, 40, 80, 3, 10);
  fpsMenu.setItems(new String[]{"12", "24", "30"}, 1);
  fpsMenu.addEventHandler(this, "fpsMenu_hit");
  fps_label = new GLabel(controlWindow, 55, ypos, 40, 20);
  fps_label.setText("FPS");
  fps_label.setOpaque(false);
  recordCheck = new GCheckbox(controlWindow, 90, ypos, 80, 20);
  recordCheck.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  recordCheck.setText("Record");
  recordCheck.setOpaque(false);
  recordCheck.setSelected(isRecordingVideo);
  recordCheck.addEventHandler(this, "recordCheck_hit");
  /* ----->>>  <<<----- */
  ypos += yinc;
  capSpinner = new GSpinner(controlWindow, 10, ypos, 60, 20);
  capSpinner.setLimits(12, 1, 128, 1);
  capSpinner.addEventHandler(this, "capSpinner_hit");
  cap_label = new GLabel(controlWindow, 75, ypos, 60, 20);
  cap_label.setText("Frames");
  cap_label.setOpaque(false);
  capCheck = new GCheckbox(controlWindow, 125, ypos, 80, 20);
  capCheck.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  capCheck.setText("Capture");
  capCheck.setOpaque(false);
  capCheck.setSelected(isCaptureFrames);
  capCheck.addEventHandler(this, "capCheck_hit");
  /* ----->>>  <<<----- */
  ypos += yinc + yinc/2;
  runVideoBtn = new GButton(controlWindow, 10, ypos, 60, 30);
  runVideoBtn.setText("Run");
  runVideoBtn.addEventHandler(this, "runVideoBtn_hit");
  // Refresh Button
  refreshBtn = new GButton(controlWindow, 160, ypos, 60, 30);
  refreshBtn.setText("Refresh");
  refreshBtn.addEventHandler(this, "refreshBtn_hit");
  /* ----->>>  <<<----- */
  videoName_label = new GLabel(controlWindow, 10, 380, 120, 20);
  videoName_label.setTextAlign(GAlign.LEFT, GAlign.BOTTOM);
  videoName_label.setText("Video file name");
  videoName_label.setOpaque(false);
  videoNameField = new GTextField(controlWindow, 10, 400, 210, 20, G4P.SCROLLBARS_NONE);
  videoNameField.setOpaque(true);
  videoNameField.addEventHandler(this, "videoNameField_hit");
  videoNameField.setText("animation.mp4");
  /* ----->>>  <<<----- */
  openBtn = new GButton(controlWindow, 10, 430, 70, 30);
  openBtn.setText("Open JSON");
  openBtn.addEventHandler(this, "openBtn_hit");
  saveBtn = new GButton(controlWindow, 90, 430, 60, 30);
  saveBtn.setText("Save");
  saveBtn.addEventHandler(this, "saveBtn_hit");
  saveAsBtn = new GButton(controlWindow, 160, 430, 60, 30);
  saveAsBtn.setText("Save As");
  saveAsBtn.addEventHandler(this, "saveAsBtn_hit");
}

/* ----->>> populate the global panel with controls <<<----- */
public void buildGlobalPanel() {
  globalPanel.addControl(blendField);
  globalPanel.addControl(blend_label);
  globalPanel.addControl(gammaField);
  globalPanel.addControl(gamma_label);
  globalPanel.addControl(steps_label);
  globalPanel.addControl(stepsSpinner);
  globalPanel.addControl(stop_label);
  globalPanel.addControl(stopSpinner);
  globalPanel.addControl(histoCheck);
  globalPanel.addControl(histoHighField);
  globalPanel.addControl(histoHigh_label);
  globalPanel.addControl(histoLowField);
  globalPanel.addControl(histoLow_label);
  globalPanel.addControl(fpsMenu);
  globalPanel.addControl(fps_label);
  globalPanel.addControl(recordCheck);
  globalPanel.addControl(refreshBtn);
  globalPanel.addControl(capSpinner);
  globalPanel.addControl(cap_label);
  globalPanel.addControl(capCheck);
  globalPanel.addControl(runVideoBtn);
  globalPanel.addControl(videoNameField);
  globalPanel.addControl(videoName_label);
  globalPanel.addControl(openBtn);
  globalPanel.addControl(saveBtn);
  globalPanel.addControl(saveAsBtn);
  globalPanel.setCollapsed(false);
}

public void loadGlobalPanelValues() {
  blendField.setText(str(wavesynth.gain));
  gammaField.setText(str(wavesynth.gamma));
  histoHighField.setText(str(wavesynth.histoHigh));
  histoLowField.setText(str(wavesynth.histoLow));
  stepsSpinner.setValue(wavesynth.animSteps);
  stopSpinner.setValue(animStop);
  capSpinner.setValue(snapCount);
  histoCheck.setSelected(wavesynth.isScaleHisto);
  // recording and animating are controlled by global variables, not in WaveSynth instance
  recordCheck.setSelected(isRecordingVideo);
  capCheck.setSelected(isCaptureFrames);
  if (wavesynth.videoFrameRate == 30) fpsMenu.setSelected(2);
  else if (wavesynth.videoFrameRate == 24) fpsMenu.setSelected(1);
  else if (wavesynth.videoFrameRate == 12) fpsMenu.setSelected(0);
  else fpsMenu.setSelected(1);
  videoNameField.setText(wavesynth.videoFilename); 
  commentsField.setText(wavesynth.getComments());
}

// called for globals that may be affected by a keypress
public void refreshGlobalPanel() {
  wavesynth. prepareAnimation();
  loadGlobalPanelValues();
  loadWaveDataPanelValues(currentWD);
}

/* ----->>> end of the global panel setup <<<----- */


/********************************************************************/
/* ----->>>               WAVE DATA PANEL                  <<<----- */
/********************************************************************/

/* ----->>> initialize wave data panel <<<----- */
public void initWaveDataPanel() {
  waveDataPanel = new GPanel(controlWindow, 245, 5, 230, 470, "Operators");
  waveDataPanel.setDraggable(false);
  waveDataPanel.setCollapsed(false);
  waveDataPanel.setCollapsible(false);
  waveDataPanel.setText("Operators");
  waveDataPanel.setOpaque(true);
  waveDataPanel.addEventHandler(this, "waveDataPanel_hit");
}

/* ----->>> add controls to the wave data panel <<<----- */
public void createWaveDataControls() {
  /* ----->>>  <<<----- */
  freqField = new GTextField(controlWindow, 10, 30, 120, 20, G4P.SCROLLBARS_NONE);
  // max frequency 131072 = (1024 * 1024) / 8 -- would it be better divided by 4 or 16, get get square resolution?
  freqField.setNumeric(0.01f, 131072.0f, 440.0f);
  freqField.setText("440.0");
  freqField.setOpaque(true);
  freqField.addEventHandler(this, "freqField_hit");
  freq_label = new GLabel(controlWindow, 140, 30, 80, 20);
  freq_label.setText("Frequency");
  freq_label.setOpaque(false);
  /* ----->>>  <<<----- */
  ampField = new GTextField(controlWindow, 10, 60, 120, 20, G4P.SCROLLBARS_NONE);
  ampField.setNumeric(-64.0f, 64.0f, 0.5f);
  ampField.setText("0.5");
  ampField.setOpaque(true);
  ampField.addEventHandler(this, "ampField_hit");
  amp_label = new GLabel(controlWindow, 140, 60, 80, 20);
  amp_label.setText("Amplitude");
  amp_label.setOpaque(false);
  /* ----->>>  <<<----- */
  phaseField = new GTextField(controlWindow, 10, 90, 120, 20, G4P.SCROLLBARS_NONE);
  // phaseField is a decimal fraction of TWO_PI, could range just between -1 and 1, 
  // but we'll give it some leeway 
  // TODO check how we're handling phase calculations
  phaseField.setNumeric(-32.0f, 32.0f, 0.5f);
  phaseField.setText("0.5");
  phaseField.setOpaque(true);
  phaseField.addEventHandler(this, "phaseField_hit");
  phase_label = new GLabel(controlWindow, 140, 90, 80, 20);
  phase_label.setText("Phase");
  phase_label.setOpaque(false);
  /* ----->>>  <<<----- */
  dcField = new GTextField(controlWindow, 10, 120, 120, 20, G4P.SCROLLBARS_NONE);
  dcField.setNumeric(-2.0f, 2.0f, 0.0f);
  dcField.setText("0.0");
  dcField.setOpaque(true);
  dcField.addEventHandler(this, "dc_hit");
  dc_label = new GLabel(controlWindow, 140, 120, 80, 20);
  dc_label.setText("DC Offset");
  dc_label.setOpaque(false);
  /* ----->>>  <<<----- */
  cyclesField = new GTextField(controlWindow, 10, 150, 120, 20, G4P.SCROLLBARS_NONE);
  cyclesField.setNumeric(-1024.0f, 1024.0f, 1.0f);
  cyclesField.setText("1.0");
  cyclesField.setOpaque(true);
  cyclesField.addEventHandler(this, "cycles_hit");
  cycles_label = new GLabel(controlWindow, 140, 150, 80, 20);
  cycles_label.setText("Cycles");
  cycles_label.setOpaque(false);
  /* ----->>>  <<<----- */
  prevBtn = new GButton(controlWindow, 10, 240, 36, 20);
  prevBtn.setText("<");
  prevBtn.addEventHandler(this, "prev_hit");
  waveDataLabel = new GLabel(controlWindow, 50, 240, 96, 20);
  waveDataLabel.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
  waveDataLabel.setText("Op 0 of 0");
  waveDataLabel.setOpaque(true);
  nextBtn = new GButton(controlWindow, 150, 240, 36, 20);
  nextBtn.setText(">");
  nextBtn.addEventHandler(this, "next_hit");
  muteWave = new GCheckbox(controlWindow, 10, 270, 80, 20);
  muteWave.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  muteWave.setText("Mute");
  muteWave.setOpaque(false);
  muteWave.addEventHandler(this, "muteWave_hit");
  soloWave = new GCheckbox(controlWindow, 90, 270, 80, 20);
  soloWave.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
  soloWave.setText("Solo");
  soloWave.setOpaque(false);
  soloWave.addEventHandler(this, "soloWave_hit");
  /* ----->>>  <<<----- */
  createColorChooserGUI(5, 178, 160, 60, 6);
  /* ----->>>  <<<----- */
  newWaveBtn = new GButton(controlWindow, 10, 430, 60, 30);
  newWaveBtn.setText("New");
  newWaveBtn.addEventHandler(this, "newWave_hit");
  dupWaveBtn = new GButton(controlWindow, 80, 430, 70, 30);
  dupWaveBtn.setText("Duplicate");
  dupWaveBtn.addEventHandler(this, "dupWave_hit");
  delWaveBtn = new GButton(controlWindow, 160, 430, 60, 30);
  delWaveBtn.setText("Delete");
  delWaveBtn.addEventHandler(this, "delWave_hit");
}

// Color Chooser GUI
// @TODO dispense with the "Choose" button and just click on the GView to show the color picker
public void createColorChooserGUI(int x, int y, int w, int h, int border) {
  // Store picture frame (not used)
  // rects.add(new Rectangle(x, y, w, h));
  // Set inner frame position
  x += border; 
  y += border;
  w -= 2*border; 
  h -= 2*border;
  colorTitle = new GLabel(controlWindow, x, y, w, 20);
  colorTitle.setText("Color: ", GAlign.LEFT, GAlign.MIDDLE);
  colorTitle.setOpaque(true);
  colorTitle.setTextBold();
  waveDataPanel.addControl(colorTitle);
  btnColor = new GButton(controlWindow, x+70, y+26, 80, 20, "Choose");
  btnColor.addEventHandler(this, "handleColorChooser");
  sel_col = color(127, 127, 127, 255);
  colorTitle.setText("Color: "+ (sel_col >> 16 & 0xFF) +", "+ (sel_col >> 8 & 0xFF) +", "+ (sel_col & 0xFF));
  waveDataPanel.addControl(btnColor);
  colorView = new GView(controlWindow, x, y+26, 60, 20, JAVA2D);
  waveDataPanel.addControl(colorView);
  colorPG = colorView.getGraphics();
  colorPG.beginDraw();
  colorPG.background(sel_col);
  colorPG.endDraw();
}

public void setWaveDataPanelColor(int sel_col) {
  colorTitle.setText("Color: "+ (sel_col >> 16 & 0xFF) +", "+ (sel_col >> 8 & 0xFF) +", "+ (sel_col & 0xFF));
  colorPG.beginDraw();
  colorPG.background(sel_col);
  colorPG.endDraw();
}

/* ----->>> populate the wave data panel with controls <<<----- */
public void buildWaveDataPanel() {
  waveDataPanel.addControl(freqField);
  waveDataPanel.addControl(freq_label);
  waveDataPanel.addControl(ampField);
  waveDataPanel.addControl(amp_label);
  waveDataPanel.addControl(phaseField);
  waveDataPanel.addControl(phase_label);
  waveDataPanel.addControl(cyclesField);
  waveDataPanel.addControl(cycles_label);
  waveDataPanel.addControl(dcField);
  waveDataPanel.addControl(dc_label);
  waveDataPanel.addControl(prevBtn);
  waveDataPanel.addControl(nextBtn);
  waveDataPanel.addControl(waveDataLabel);
  waveDataPanel.addControl(muteWave);
  waveDataPanel.addControl(soloWave);
  waveDataPanel.addControl(newWaveBtn);
  waveDataPanel.addControl(dupWaveBtn);
  waveDataPanel.addControl(delWaveBtn);
}

public void loadWaveDataPanelValues(WaveData wd) {
  freqField.setText(str(wd.freq));
  ampField.setText(str(wd.amp));
  phaseField.setText(str(wd.phase));
  cyclesField.setText(str(wd.phaseCycles));
  dcField.setText(str(wd.dc));
  int dataLen = wavesynth.waveDataList.size();
  waveDataLabel.setText("Op "+ (waveDataIndex + 1) +" of "+ dataLen);
  setWaveDataPanelColor(wd.waveColor);
  if (wd.isMuted) wd.waveState = WaveData.WaveState.MUTE;
  switch (wd.waveState) {
    case ACTIVE: {
      wd.isMuted = false;
      muteWave.setSelected(false);
      soloWave.setSelected(false);
      break;
    }
    case SOLO: {
      wd.isMuted = false;
      muteWave.setSelected(false);
      soloWave.setSelected(true);
      break;
    }
    case MUTE: {
      wd.isMuted = true;
      muteWave.setSelected(true);
      soloWave.setSelected(false);
      break;
    }
    case SUSPENDED: {
      wd.isMuted = true;
      muteWave.setSelected(true);
      soloWave.setSelected(false);
      break;
    }
  }
  // rely on the global blendChannels 
  if (!isAnimating) renderFrame(step);
  
  /*
  muteWave.setText("Mute");
  newWaveBtn.setText("New");
  dupWaveBtn.setText("Dupe");
  delWaveBtn.setText("Delete");
  */
}


/********************************************************************/
/*            Window and Global Control Panel Handlers              */
/********************************************************************/

// our draw method, call each time through the event loop
synchronized public void winDraw(PApplet appc, GWinData data) { 
  appc.background(color(254, 233, 178));
} 

public void globalPanel_hit(GPanel panel, GEvent event) { 
  // nothing doing
}

/*
  Dropped:
  synchronized public void winMouse(PApplet appc, GWinData data, processing.event.MouseEvent mevent)
  synchronized public void winKey(PApplet appc, GWinData data, processing.event.KeyEvent kevent)
  synchronized public void winPre(PApplet appc, GWinData data)
  synchronized public void winPost(PApplet appc, GWinData data) 
  public void winClose(GWindow window)
*/

public void comments_hit(GTextArea source, GEvent event) {
  // println("commentsField - GTextField >> GEvent." + event + " @ " + millis() + " value: " + commentsField.getText());
  wavesynth.setComments(commentsField.getText());
}

public void blendField_hit(GTextField source, GEvent event) { 
  // println("blendField - GTextField >> GEvent." + event + " @ " + millis() + " value: " + blendField.getValueF());
  wavesynth.setGain(blendField.getValueF());
  //if (!isAnimating) blendChannels(step)(step);
} 

public void gammaField_hit(GTextField source, GEvent event) { 
  // println("gammaField - GTextField >> GEvent." + event + " @ " + millis());
  wavesynth.setGamma(gammaField.getValueF());
  //if (!isAnimating) blendChannels(step)(step);
} 

public void stepsSpinner_hit(GSpinner source, GEvent event) {
  // println("stepsSpinner - GSpinner >> GEvent." + event + " @ " + millis());
  // @TODO recalculate global wd settings when animSteps changes
  animSteps = stepsSpinner.getValue();
  wavesynth.setAnimSteps(animSteps);
  for (WaveData wd : wavesynth.waveDataList) {
    wd.phaseInc = (wd.phaseCycles * TWO_PI)/wavesynth.animSteps;
  }
  //if (!isAnimating) blendChannels(step)(step);
}

public void stopSpinner_hit(GSpinner source, GEvent event) {
  // println("stepsSpinner - GSpinner >> GEvent." + event + " @ " + millis());
  animStop = stopSpinner.getValue();
  //if (!isAnimating) blendChannels(step)(step);
}

public void histoCheck_hit(GCheckbox source, GEvent event) { 
  // println("histoCheck - GCheckbox >> GEvent." + event + " @ " + millis());
  wavesynth.setScaleHisto(histoCheck.isSelected());
  //if (!isAnimating) blendChannels(step)(step);
} 

public void histoHigh_hit(GTextField source, GEvent event) { 
  // println("histoHigh - GTextField >> GEvent." + event + " @ " + millis());
  wavesynth.setHistoHigh(histoHighField.getValueI());
  //if (!isAnimating) blendChannels(step)(step);
} 

public void histoLow_hit(GTextField source, GEvent event) { 
  // println("histoLow - GTextField >> GEvent." + event + " @ " + millis());
  wavesynth.setHistoLow(histoLowField.getValueI());
  //if (!isAnimating) blendChannels(step)(step);
} 

public void videoNameField_hit(GTextField source, GEvent event) { 
  // println("videoNameField - GTextField >> GEvent." + event + " @ " + millis());
  wavesynth.setVideoFilename(videoNameField.getText());
  // update global videoFilename
  videoFilename = wavesynth.getVideoFilename();
} 

public void fpsMenu_hit(GDropList source, GEvent event) { 
  // println("fpsMenu - GDropList >> GEvent." + event + " @ " + millis());
  wavesynth.setVideoFrameRate(Integer.valueOf(fpsMenu.getSelectedText()));
  println("----->>> videoFPS = "+ wavesynth.videoFrameRate);  
} 

// this stays global
public void recordCheck_hit(GCheckbox source, GEvent event) { 
  println("recordCheck - GCheckbox >> GEvent." + event + " @ " + millis());
  isRecordingVideo = recordCheck.isSelected();
  if (isRecordingVideo) {
    if (isOversize) {
      println("----->>> Oversize image, setting to full size to record correctly.");
      isFitToScreen = false;
      resizeWindow();
    }
  }
  refreshGlobalPanel();
  println("----->>> isRecordingVideo = "+ isRecordingVideo);
} 

// prob want a refresh call in WaveSynth
public void refreshBtn_hit(GButton source, GEvent event) {
  // println("refreshBtn - GButton >> GEvent." + event + " @ " + millis());
  if (!isDesignMode) {
    
  }
  renderFrame(step);
}

// stays global
public void capSpinner_hit(GSpinner source, GEvent event) {
  // println("capSpinner - GSpinner >> GEvent." + event + " @ " + millis());
  snapCount = capSpinner.getValue();
}

// stays global
public void capCheck_hit(GCheckbox source, GEvent event) {
  // println("capSpinner - GSpinner >> GEvent." + event + " @ " + millis());
  isCaptureFrames = capCheck.isSelected();
  println("----->>> isCaptureFrames = "+ isCaptureFrames);
}

// handled globally with calls on wavesynth or list of WaveAnimators, when relevant
public void runVideoBtn_hit(GButton source, GEvent event) {
  toggleAnimation(); //<>// //<>//
}

public void openBtn_hit(GButton source, GEvent event) { 
  // println("openBtn - GButton >> GEvent." + event + " @ " + millis());
  if (isAnimating) toggleAnimation();
  loadWaveData();
  // isAnimating = oldIsAnimating;
} 

public void saveBtn_hit(GButton source, GEvent event) { 
  // println("saveBtn - GButton >> GEvent." + event + " @ " + millis());
  if (isAnimating) toggleAnimation();
  if ((currentDataFile == null) || (currentDataFile.getAbsolutePath().equals(""))) {
    saveWaveData();
  }
  else {
    fileSelectedWrite(currentDataFile);
  }
} 

public void saveAsBtn_hit(GButton source, GEvent event) { 
  // println("saveAsBtn - GButton >> GEvent." + event + " @ " + millis());
  if (isAnimating) toggleAnimation();
  saveWaveData();
} 


/********************************************************************/
/*                Wave Data Control Panel Handlers                  */
/********************************************************************/

// not active
public void waveDataPanel_hit(GPanel source, GEvent event) { 
  println("waveDataPanel - GPanel >> GEvent." + event + " @ " + millis());
} 

public void freqField_hit(GTextField source, GEvent event) { 
  // println("freqField - GTextField >> GEvent." + event + " @ " + millis());
  float newFreq = freqField.getValueF();
  // currentWD is selected from wavesynth.waveDataList, where wavesynth is the (current) WaveSynth instance
  currentWD.setFreq(newFreq);
} 

public void ampField_hit(GTextField source, GEvent event) { 
  // println("ampField - GTextField >> GEvent." + event + " @ " + millis());
  float newAmp = ampField.getValueF();
  // currentWD is selected from wavesynth.waveDataList, where wavesynth is the (current) WaveSynth instance
  currentWD.setAmp( newAmp);
} 

public void phaseField_hit(GTextField source, GEvent event) { 
  // println("phaseField - GTextField >> GEvent." + event + " @ " + millis());
  float newPhase = phaseField.getValueF();
  // we only need the decimal fraction, which is a portion of TWO_PI
  // next time we load the GUI the newPhase value will appear
  newPhase -= floor(newPhase);
  // currentWD is selected from wavesynth.waveDataList, where wavesynth is the (current) WaveSynth instance
  // TODO verify compatibility with older JSON format
  currentWD.setPhase(newPhase);
  if (!isAnimating) renderFrame(step);
}

public void cycles_hit(GTextField source, GEvent event) { 
  // println("cyclesField - GTextField >> GEvent." + event + " @ " + millis());
  float newCycles = cyclesField.getValueF();
  // currentWD is selected from wavesynth.waveDataList, where wavesynth is the (current) WaveSynth instance
  currentWD.setCycles(newCycles); 
}

public void dc_hit(GTextField source, GEvent event) {
  // println("dcField - GTextField >> GEvent." + event + " @ " + millis());
  float newDc = dcField.getValueF();
  // currentWD is selected from wavesynth.waveDataList, where wavesynth is the (current) WaveSynth instance
  currentWD.setDc(newDc);
}

// G4P code for colour chooser
public void handleColorChooser(GButton button, GEvent event) {
  // println("btnColor - GButton >> GEvent." + event + " @ " + millis());
  sel_col = G4P.selectColor(currentWD.waveColor);
  colorTitle.setText("Color: "+ (sel_col >> 16 & 0xFF) +", "+ (sel_col >> 8 & 0xFF) +", "+ (sel_col & 0xFF));
  colorPG.beginDraw();
  colorPG.background(sel_col);
  colorPG.endDraw();
  currentWD = wavesynth.waveDataList.get(waveDataIndex);
  if (isVerbose) println("==> selected color: "+ PixelAudioMapper.colorString(sel_col));
  // be sure to set color value in wavesynth.waveColors array, otherwise the display won't update
  currentWD.setWaveColor(sel_col);
  wavesynth.waveColors[waveDataIndex] = sel_col;
  if (step != 0) wavesynth.prepareAnimation();
  renderFrame(step);
  mapImage = wavesynth.mapImage;
}

public void prev_hit(GButton source, GEvent event) { 
  //println("prevBtn - GButton >> GEvent." + event + " @ " + millis());
  decWaveData();
} 

public void next_hit(GButton source, GEvent event) { 
  //println("nextBtn - GButton >> GEvent." + event + " @ " + millis());
  incWaveData();
} 

public void newWave_hit(GButton source, GEvent event) { 
  // println("newWaveBtn - GButton >> GEvent." + event + " @ " + millis());
  WaveData wd = new WaveData();
  wavesynth.waveDataList.add(++waveDataIndex, wd);
  currentWD = wavesynth.waveDataList.get(waveDataIndex);
  wavesynth.updateWaveColors();
  loadWaveDataPanelValues(wd);
  refreshGlobalPanel();
} 

public void dupWave_hit(GButton source, GEvent event) { 
  // println("dupWaveBtn - GButton >> GEvent." + event + " @ " + millis());
  WaveData wd = currentWD.clone();
  wavesynth.waveDataList.add(++waveDataIndex, wd);
  currentWD = wavesynth.waveDataList.get(waveDataIndex);
  wavesynth.updateWaveColors();
  loadWaveDataPanelValues(wd);
  refreshGlobalPanel();
} 

public void delWave_hit(GButton source, GEvent event) { 
  // println("delWaveBtn - GButton >> GEvent." + event + " @ " + millis());
  int remIndex = waveDataIndex;
  if (waveDataIndex == wavesynth.waveDataList.size() - 1) waveDataIndex = 0;
  wavesynth.waveDataList.remove(remIndex);
  currentWD = wavesynth.waveDataList.get(waveDataIndex);
  wavesynth.updateWaveColors();
  loadWaveDataPanelValues(currentWD);
  refreshGlobalPanel();
} 

// see enum WaveData.WaveState {ACTIVE, SOLO, MUTE, SUSPENDED};
public void muteWave_hit(GCheckbox source, GEvent event) { 
  //println("muteWave - GCheckbox >> GEvent." + event + " @ " + millis());
  currentWD.isMuted = muteWave.isSelected();
  currentWD.waveState = muteWave.isSelected() ? WaveData.WaveState.MUTE : WaveData.WaveState.ACTIVE;
  loadWaveDataPanelValues(currentWD);
} 

// see enum WaveData.WaveState {ACTIVE, SOLO, MUTE, SUSPENDED};
public void soloWave_hit(GCheckbox source, GEvent event) { 
  //println("soloWave - GCheckbox >> GEvent." + event + " @ " + millis());
  currentWD.waveState = soloWave.isSelected() ? WaveData.WaveState.SOLO : WaveData.WaveState.ACTIVE ;
  if (currentWD.waveState == WaveData.WaveState.SOLO) {
    for (WaveData wd : wavesynth.waveDataList) {
      if (currentWD == wd) continue;
      if (wd.waveState == WaveData.WaveState.ACTIVE) {
        wd.waveState = WaveData.WaveState.SUSPENDED;
      }
    }
  }
  else {
    boolean listHasSolos = false;
    for (WaveData wd : wavesynth.waveDataList) {
      if (wd.waveState == WaveData.WaveState.SOLO) listHasSolos = true;
    }
    if (!listHasSolos) {
      for (WaveData wd : wavesynth.waveDataList) {
        if (wd.waveState == WaveData.WaveState.SUSPENDED) {
          wd.waveState = WaveData.WaveState.ACTIVE;
        }
      }
    }
  }
  loadWaveDataPanelValues(currentWD);
}

/********************************************************************/
/*                 Enable and Disable GUI Elements                  */
/********************************************************************/

public void enableWDListControls(boolean enable) {
  // newWave, dupWave, delWave have to be disabled during animation 
  // because G4P runs separate threads from PApplet
  // println("----->>> enableWDListControls "+ enable);
  newWaveBtn.setEnabled(enable);
  dupWaveBtn.setEnabled(enable);
  delWaveBtn.setEnabled(enable);
  
}

/* ------------------------------------------------------------- */
/*                        END G4P GUI                            */
/* ------------------------------------------------------------- */


// utility methods called through the control panel

/**
 * Comparator class for sorting waveDataList by frequency or phase
 */
public class CompareWaveData implements Comparator <WaveData> {
  boolean isCompareFrequency = true;

  public int compare(WaveData wd1, WaveData wd2) {
    if (isCompareFrequency) {
      if (wd1.freq > wd2.freq) return 1;
      if (wd1.freq < wd2.freq) return -1;
    } 
    else {
      if (wd1.phase > wd2.phase) return 1;
      if (wd1.phase < wd2.phase) return -1;
    }
    return 0;
  }
}

/**
 * Steps through the WaveSynth's list of WaveData, shows the current 
 * WaveData operator in the control panel.
 * @param up   if true, increment waveDataIndex, otherwise, decrement it
 */
public void stepWaveData(boolean up) {
  int dataLen = wavesynth.waveDataList.size();
  if (up) {
    waveDataIndex = (waveDataIndex + 1 >= dataLen) ? 0 : waveDataIndex + 1;
  } 
  else {
    waveDataIndex = (waveDataIndex - 1 >= 0) ? waveDataIndex - 1 : dataLen - 1;
  }
  currentWD = wavesynth.waveDataList.get(waveDataIndex);
  loadWaveDataPanelValues(currentWD);
}

public void incWaveData() {
  stepWaveData(true);
}

public void decWaveData() {
  stepWaveData(false);
}
