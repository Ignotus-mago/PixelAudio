/* ---------------------------------------------------------------- */
/*                                                                  */
/*                      GUI Interface Code                          */
/*                       Uses G4P library                           */    
/*                                                                  */
/* ---------------------------------------------------------------- */
// Variable declarations 
GWindow controlWindow;
GLabel argo1Label; 
GLabel genLabel1;
GDropList genMenu1;
GLabel colorsLabel1; 
GDropList colorMenu1; 
GLabel alpha1Label; 
GTextField alpha1Text;
GLabel patternLabel1; 
GDropList patternMenu1; 
GLabel repLabel1; 
GTextField repText1; 
GLabel unitLabel1; 
GTextField unitSize1; 
GLabel gapLabel1; 
GTextField gap1Text; 
GLabel gapColorLabel1;
GDropList gapColorMenu1;
GLabel gapAlpha1Label; 
GTextField gapAlpha1Text;
GCheckbox argo1Show;
GCheckbox argo1Freeze;
GLabel argoStepLabel1;
GTextField argoStepText1;
//
GLabel argo2Label; 
GLabel genLabel2;
GDropList genMenu2;
GLabel colorsLabel2; 
GDropList colorMenu2; 
GLabel alpha2Label; 
GTextField alpha2Text;
GLabel patternLabel2; 
GDropList patternMenu2; 
GLabel repLabel2; 
GTextField repText2; 
GLabel unitLabel2; 
GTextField unitSize2; 
GLabel gapLabel2; 
GTextField gap2Text; 
GLabel gapColorLabel2;
GDropList gapColorMenu2;
GLabel gapAlpha2Label; 
GTextField gapAlpha2Text;
GCheckbox argo2Show;
GCheckbox argo2Freeze;
GLabel argoStepLabel2;
GTextField argoStepText2;
// 
GLabel animationLabel;
GLabel animOpenLabel;
GTextField animOpenText;
GLabel animRunLabel1;
GTextField animRunText1;
GLabel animHoldLabel1;
GTextField animHoldText1;
GLabel animRunLabel2;
GTextField animRunText2;
GLabel animHoldLabel2;
GTextField animHoldText2;
GLabel animCloseLabel;
GTextField animCloseText;
GLabel animDurationLabel;
GTextField animDurationText;
GButton recordButton;
// 
// menu items
String[] genItems = {"Hilbert Loop 3x2", "Hilbert ZZ Loop", "Hilbert Stack Ortho", "Hilbert Stack Bou", "Hilbert Row Ortho", 
         "Hilbert Column Ortho", "ZZ Loop 6x4", "ZZ Row Ortho","ZZ Row Alt Ortho", "ZZ Column Ortho", 
         "ZZ Column Alt Ortho", "ZZ Row Random One", "ZZ Row Random Two", "Boustroph Row Random", "Hilbert Random One", "Hilbert Random Two"};
String[] colorItems = {"Black Alone", "Black, White", "White, Black", "Black, Gray, White", "Gray Ramp", "Gray Triangle", "Multicolor", "Spectrum 8", "Spectrum 6", "Blue Cream", "Cream Blue", "Four Color"};
int[][] colorVars = {blackAlone, blackWhite, whiteBlack, blackGrayWhite, grayRamp, grayTriangle, multicolor, espectroOcho, espectroSeis, blueCream, creamBlue, fourColor};
String[] patternItems = {"The One", "One-one", "Count to Five", "Seven Forty-nine", "Fibo 55", "Fibonacci", "Lucas"};
int[][] patternVars = {theOne, oneOne, countToFive, sevenFortyNine, fiboLSystem55, fibo13To89, lucas18To76};
int[] gapColorVars = {black, white, gray, roig, roigtar, taronja, groc, vert, blau, 
                      blau2, violet, grana, blanc, gris, negre};
String[] gapColorItems = {"black", "white", "gray", "red", "red orange", "orange", "yellow", "green", 
                          "blue 1", "blue 2", "violet", "red violet", "bone", "blue gray", "midnight"};

// spacing variables
int ypos = 10;
int inc = 28;
// Create all the GUI controls. 
public void createGUI(){
    G4P.messagesEnabled(false);
    G4P.setGlobalColorScheme(GCScheme.BLUE_SCHEME);
    G4P.setMouseOverEnabled(false);
    surface.setTitle("Argosy Window");
    /* ----->>> floating control window <<<----- */  
    controlWindow = GWindow.getWindow(this, "Argosy Settings", 0, 0, 480, 540, JAVA2D);
    controlWindow.noLoop();
    controlWindow.setActionOnClose(G4P.KEEP_OPEN);
    controlWindow.addDrawHandler(this, "drawControlWindow");
    /* ----->>> argosy01 controls <<<----- */
    ypos = 10;
    argo1Label = new GLabel(controlWindow, 10, ypos, 220, 20);
    argo1Label.setText("Argosy 1");
    argo1Label.setTextBold();
    argo1Label.setOpaque(true);
    //
    ypos += inc;
    genLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
    genLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    genLabel1.setText("Map: ");
    genLabel1.setOpaque(false);
    genMenu1 = new GDropList(controlWindow, 100, ypos, 130, 120, 5, 10);
    genMenu1.setItems(genItems, 0);
    genMenu1.addEventHandler(this, "genMenu1_hit");
    genMenu1.setSelected(argo1GenSelect);
    //
    ypos += inc;
    colorsLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
    colorsLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    colorsLabel1.setText("Colors: ");
    colorsLabel1.setOpaque(false);
    colorMenu1 = new GDropList(controlWindow, 100, ypos, 130, 120, 5, 10);
    colorMenu1.setItems(colorItems, 0);
    colorMenu1.addEventHandler(this, "colorMenu1_hit");
    colorMenu1.setSelected(argo1PaletteSelect);
    //
    ypos += inc;
    alpha1Label = new GLabel(controlWindow, 10, ypos, 80, 20);
    alpha1Label.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    alpha1Label.setText("Opacity: ");
    alpha1Label.setOpaque(false);
    alpha1Text = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);
    alpha1Text.setText(String.valueOf(argo1Alpha));
    alpha1Text.setOpaque(true);
    alpha1Text.setNumeric(0, 255, argo1Alpha);
    alpha1Text.addEventHandler(this, "alphaText1_change");
    //
    ypos += inc;
    patternLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
    patternLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    patternLabel1.setText("Pattern: ");
    patternLabel1.setOpaque(false);
    patternMenu1 = new GDropList(controlWindow, 100, ypos, 130, 120, 5, 10);
    patternMenu1.setItems(patternItems, 0);
    patternMenu1.addEventHandler(this, "patternMenu1_hit");
    patternMenu1.setSelected(argo1PatternSelect);
    //
    ypos += inc;
    repLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
    repLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    repLabel1.setText("Repeat: ");
    repLabel1.setOpaque(false);
    repText1 = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);
    repText1.setText("0");
    repText1.setOpaque(true);
    repText1.setNumeric(0, 16384, 0);
    repText1.addEventHandler(this, "repText1_change");
    //
    ypos += inc;
    unitLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
    unitLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    unitLabel1.setText("Unit: ");
    unitLabel1.setOpaque(false);
    unitSize1 = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);
    unitSize1.setText(String.valueOf(argo1Unit));
    unitSize1.setOpaque(true);
    unitSize1.setNumeric(1, 65536, 1);
    unitSize1.addEventHandler(this, "unitSize1_change");
    //
    ypos += inc;
    gapLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
    gapLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    gapLabel1.setText("Gap: ");
    gapLabel1.setOpaque(false);
    gap1Text = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);
    gap1Text.setText(String.valueOf(argo1Gap));
    gap1Text.setOpaque(true);
    gap1Text.setNumeric(0, 1048576, 1);
    gap1Text.addEventHandler(this, "gap1_change");
    //
    ypos += inc;
    gapColorLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
    gapColorLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    gapColorLabel1.setText("Gap color: ");
    gapColorLabel1.setOpaque(false);
    gapColorMenu1 = new GDropList(controlWindow, 100, ypos, 130, 120, 5, 10);
    gapColorMenu1.setItems(gapColorItems, 0);
    gapColorMenu1.addEventHandler(this, "gapColorMenu1_hit");
    gapColorMenu1.setSelected(argo1GapColorIndex);
    //
    ypos += inc;
    gapAlpha1Label = new GLabel(controlWindow, 10, ypos, 80, 20);
    gapAlpha1Label.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    gapAlpha1Label.setText("Gap opacity: ");
    gapAlpha1Label.setOpaque(false);
    gapAlpha1Text = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
    gapAlpha1Text.setText(String.valueOf(argo1GapAlpha));
    gapAlpha1Text.setOpaque(true);
    gapAlpha1Text.setNumeric(0, 255, 255);
    gapAlpha1Text.addEventHandler(this, "gapOpacity1_change");
      //
      ypos += inc;
      // animation label -- print ypos to get coordinate 
    animationLabel = new GLabel(controlWindow, 10, ypos, 460, 20);
    animationLabel.setText("Animation Settings");
    animationLabel.setTextBold();
    animationLabel.setOpaque(true);
    //
    ypos += inc;
    argo1Show = new GCheckbox(controlWindow, 100, ypos, 128, 20);
    argo1Show.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
    argo1Show.setText("Show Argosy One");
    argo1Show.setOpaque(false);
    argo1Show.setSelected(isShowArgo1);
    argo1Show.addEventHandler(this, "argo1ShowCheck_hit");
    //
    ypos += 20;
    argo1Freeze = new GCheckbox(controlWindow, 100, ypos, 128, 20);
    argo1Freeze.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
    argo1Freeze.setText("Freeze");
    argo1Freeze.setOpaque(false);
    argo1Freeze.setSelected(isArgo1Freeze);
    argo1Freeze.addEventHandler(this, "argo1FreezeCheck_hit");
    //
    /* ----->>> argosy02 controls <<<----- */
    //
    ypos = 10;
    argo2Label = new GLabel(controlWindow, 250, ypos, 220, 20);
    argo2Label.setText("Argosy 2");
    argo2Label.setTextBold();
    argo2Label.setOpaque(true);
    //
    ypos += inc;
    genLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
    genLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    genLabel2.setText("Map: ");
    genLabel2.setOpaque(false);
    genMenu2 = new GDropList(controlWindow, 340, ypos, 130, 120, 5, 10);
    genMenu2.setItems(genItems, 0);
    genMenu2.addEventHandler(this, "genMenu2_hit");
    genMenu2.setSelected(argo2GenSelect);
    //
    ypos += inc;
    colorsLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
    colorsLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    colorsLabel2.setText("Colors: ");
    colorsLabel2.setOpaque(false);
    colorMenu2 = new GDropList(controlWindow, 340, ypos, 130, 120, 5, 10);
    colorMenu2.setItems(colorItems, 0);
    colorMenu2.addEventHandler(this, "colorMenu2_hit");
    colorMenu2.setSelected(argo2PaletteSelect);
    //
    ypos += inc;
    alpha2Label = new GLabel(controlWindow, 250, ypos, 80, 20);
    alpha2Label.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    alpha2Label.setText("Opacity: ");
    alpha2Label.setOpaque(false);
    alpha2Text = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);
    alpha2Text.setText(String.valueOf(argo2Alpha));
    alpha2Text.setOpaque(true);
    alpha2Text.setNumeric(0, 255, argo2Alpha);
    alpha2Text.addEventHandler(this, "alphaText2_change");
    //
    ypos += inc;
    patternLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
    patternLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    patternLabel2.setText("Pattern: ");
    patternLabel2.setOpaque(false);
    patternMenu2 = new GDropList(controlWindow, 340, ypos, 130, 120, 5, 10);
    patternMenu2.setItems(patternItems, 0);
    patternMenu2.addEventHandler(this, "patternMenu2_hit");
    patternMenu2.setSelected(argo2PatternSelect);
    //
    ypos += inc;
    repLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
    repLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    repLabel2.setText("Repeat: ");
    repLabel2.setOpaque(false);
    repText2 = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);
    repText2.setText("0");
    repText2.setOpaque(true);
    repText2.setNumeric(0, 16384, 0);
    repText2.addEventHandler(this, "repText2_change");
    //
    ypos += inc;
    unitLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
    unitLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    unitLabel2.setText("Unit: ");
    unitLabel2.setOpaque(false);
    unitSize2 = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);
    unitSize2.setText(String.valueOf(argo2Unit));
    unitSize2.setOpaque(true);
    unitSize2.setNumeric(1, 65536, 1);
    unitSize2.addEventHandler(this, "unitSize2_change");
    //
    ypos += inc;
    gapLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
    gapLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    gapLabel2.setText("Gap: ");
    gapLabel2.setOpaque(false);
    gap2Text = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);
    gap2Text.setText(String.valueOf(argo2Gap));
    gap2Text.setOpaque(true);
    gap2Text.setNumeric(0, 1048576, 1);
    gap2Text.addEventHandler(this, "gap2_change");
    //
    ypos += inc;
    gapColorLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
    gapColorLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    gapColorLabel2.setText("Gap color: ");
    gapColorLabel2.setOpaque(false);
    gapColorMenu2 = new GDropList(controlWindow, 340, ypos, 130, 120, 5, 10);
    gapColorMenu2.setItems(gapColorItems, 0);
    gapColorMenu2.addEventHandler(this, "gapColorMenu2_hit");
    gapColorMenu2.setSelected(argo2GapColorIndex);
    //
    ypos += inc;
    gapAlpha2Label = new GLabel(controlWindow, 250, ypos, 80, 20);
    gapAlpha2Label.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    gapAlpha2Label.setText("Gap opacity: ");
    gapAlpha2Label.setOpaque(false);
    gapAlpha2Text = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
    gapAlpha2Text.setText(String.valueOf(argo2GapAlpha));
    gapAlpha2Text.setOpaque(true);
    gapAlpha2Text.setNumeric(0, 255, 255);
    gapAlpha2Text.addEventHandler(this, "gapOpacity2_change");
      //
      ypos += inc;
      // space for animation label
    //
    ypos += inc;
    argo2Show = new GCheckbox(controlWindow, 340, ypos, 128, 20);
    argo2Show.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
    argo2Show.setText("Show Argosy Two");
    argo2Show.setOpaque(false);
    argo2Show.setSelected(isShowArgo2);
    argo2Show.addEventHandler(this, "argo2ShowCheck_hit");
    //
    ypos += 20;
    argo2Freeze = new GCheckbox(controlWindow, 340, ypos, 128, 20);
    argo2Freeze.setIconAlign(GAlign.LEFT, GAlign.MIDDLE);
    argo2Freeze.setText("Freeze");
    argo2Freeze.setOpaque(false);
    argo2Freeze.setSelected(isArgo2Freeze);
    argo2Freeze.addEventHandler(this, "argo2FreezeCheck_hit");
    //
    /* ----->>> Animation controls <<<----- */
    //
    ypos += inc;
    argoStepLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
    argoStepLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    argoStepLabel1.setText("Step: ");
    argoStepLabel1.setOpaque(false);
    argoStepText1 = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
    argoStepText1.setText(String.valueOf(argo1Step));
    argoStepText1.setOpaque(true);
    argoStepText1.setNumericType(G4P.INTEGER);
    argoStepText1.addEventHandler(this, "argoStep1_change");
    //
    argoStepLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
      argoStepLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
      argoStepLabel2.setText("Step: ");
      argoStepLabel2.setOpaque(false);
      argoStepText2 = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
      argoStepText2.setText(String.valueOf(argo2Step));
      argoStepText2.setOpaque(true);
      argoStepText2.setNumericType(G4P.INTEGER);
      argoStepText2.addEventHandler(this, "argoStep2_change");
    //
    ypos += inc;
    animOpenLabel = new GLabel(controlWindow, 10, ypos, 80, 20);
    animOpenLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    animOpenLabel.setText("Open frames: ");
    animOpenLabel.setOpaque(false);
    animOpenText = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
    animOpenText.setText(String.valueOf(animOpen));
    animOpenText.setOpaque(true);
    animOpenText.setNumeric(0, 65536, 0);
    animOpenText.addEventHandler(this, "animOpen_change");
    // 
    animCloseLabel = new GLabel(controlWindow, 250, ypos, 80, 20);
    animCloseLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    animCloseLabel.setText("Close frames: ");
    animCloseLabel.setOpaque(false);
    animCloseText = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
    animCloseText.setText(String.valueOf(animClose));
    animCloseText.setOpaque(true);
    animCloseText.setNumeric(0, 65536, 0);
    animCloseText.addEventHandler(this, "animClose_change");
    //
    ypos += inc;
    animRunLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
    animRunLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    animRunLabel1.setText("Run 1: ");
    animRunLabel1.setOpaque(false);
    animRunText1 = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
    animRunText1.setText(String.valueOf(animRun1));
    animRunText1.setOpaque(true);
    animRunText1.setNumeric(0, 65536, 0);
    animRunText1.addEventHandler(this, "animRun1_change");
    //
    animRunLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
    animRunLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    animRunLabel2.setText("Run 2: ");
    animRunLabel2.setOpaque(false);
    animRunText2 = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
    animRunText2.setText(String.valueOf(animRun2));
    animRunText2.setOpaque(true);
    animRunText2.setNumeric(0, 65536, 0);
    animRunText2.addEventHandler(this, "animRun2_change");
    //
    ypos += inc;
    animHoldLabel1 = new GLabel(controlWindow, 10, ypos, 80, 20);
    animHoldLabel1.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    animHoldLabel1.setText("Hold 1: ");
    animHoldLabel1.setOpaque(false);
    animHoldText1 = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
    animHoldText1.setText(String.valueOf(animHold1));
    animHoldText1.setOpaque(true);
    animHoldText1.setNumeric(0, 65536, 0);
    animHoldText1.addEventHandler(this, "animHold1_change");
    //
    animHoldLabel2 = new GLabel(controlWindow, 250, ypos, 80, 20);
    animHoldLabel2.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    animHoldLabel2.setText("Hold 2: ");
    animHoldLabel2.setOpaque(false);
    animHoldText2 = new GTextField(controlWindow, 340, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
    animHoldText2.setText(String.valueOf(animHold2));
    animHoldText2.setOpaque(true);
    animHoldText2.setNumeric(0, 65536, 0);
    animHoldText2.addEventHandler(this, "animHold2_change");
    //
    ypos += inc;
    animDurationLabel = new GLabel(controlWindow, 10, ypos, 80, 20);
    animDurationLabel.setTextAlign(GAlign.RIGHT, GAlign.MIDDLE);
    animDurationLabel.setText("Duration: ");
    animDurationLabel.setOpaque(false);
    animDurationText = new GTextField(controlWindow, 100, ypos, 130, 20, G4P.SCROLLBARS_NONE);;
    animDurationText.setText(String.valueOf(animDuration));
    animDurationText.setOpaque(true);
    animDurationText.setNumeric(0, 65536, 0);
    animDurationText.addEventHandler(this, "animDuration_change");      
    //
    recordButton = new GButton(controlWindow, 270, ypos, 160, 20);
    recordButton.setText("Record Video");
    recordButton.addEventHandler(this, "recordButton_hit");
    //
    controlWindow.loop();
}
/* ----->>> EVENT HANDLERS <<<----- */  
/* ----->>>   ARGOSY ONE   <<<----- */  
synchronized public void drawControlWindow(PApplet appc, GWinData data) {
    appc.background(color(204, 199, 212));
    appc.stroke(gris);
    appc.strokeWeight(2);
    appc.line(10, 308, 470, 308);
}
public void genMenu1_hit(GDropList source, GEvent event) {
    argo1GenSelect = source.getSelectedIndex();
    int shift = argo1.getArgosyPixelShift();
    argo1Mapper = selectMapper(argo1GenSelect, argo1Gen);
    this.initArgo1(shift);
    isBufferStale = true;
}

public void colorMenu1_hit(GDropList source, GEvent event) {
    argo1PaletteSelect = source.getSelectedIndex();
    argo1Colors = setArgoColorsAlpha(colorVars[argo1PaletteSelect], argo1Alpha);
    int shift = argo1.getArgosyPixelShift();
    argo1.setArgosyColors(argo1Colors);
    if (shift != 0) {
        argo1.shift(-shift, true);
    }        
    isBufferStale = true;
}
public void alphaText1_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (argo1Alpha != val) {
        argo1Alpha = val;
        argo1Colors = setArgoColorsAlpha(argo1.getArgosyColors(), argo1Alpha);
        int shift = argo1.getArgosyPixelShift();
        argo1.setArgosyColors(argo1Colors);
        if (shift != 0) {
            argo1.shift(-shift, true);
        }
        isBufferStale = true;
    }
}

public void patternMenu1_hit(GDropList source, GEvent event) {
    argo1PatternSelect = source.getSelectedIndex();
    argo1Pattern = patternVars[argo1PatternSelect];
    int shift = argo1.getArgosyPixelShift();
    argo1.setArgosyPattern(argo1Pattern);
    if (shift != 0) {
        argo1.shift(-shift, true);
    }        
    isBufferStale = true;
}
public void repText1_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (argo1Reps != val) {
        argo1Reps = val;
        int shift = argo1.getArgosyPixelShift();
        argo1.setArgosyReps(argo1Reps);
        if (shift != 0) {
            argo1.shift(-shift, true);
        }
        isBufferStale = true;
    }
}

public void unitSize1_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (argo1Unit != val) {
        argo1Unit = val;
        int shift = argo1.getArgosyPixelShift();
        argo1.setUnitSize(argo1Unit);
        if (shift != 0) {
            argo1.shift(-shift, true);
        }
        isBufferStale = true;
    }
}
public void gap1_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (argo1Gap != val) {
        argo1Gap = val;
        //println("--->> new argo1 gap = "+ argo1Gap);
        int shift = argo1.getArgosyPixelShift();
        argo1.setArgosyGap(argo1Gap);
        if (shift != 0) {
            argo1.shift(-shift, true);
        }
        isBufferStale = true;
    }
}
public void gapColorMenu1_hit(GDropList source, GEvent event) {
    int index = source.getSelectedIndex();
    argo1GapColor = gapColorVars[index];
    gapColorMenu1.setSelected(index);
    int shift = argo1.getArgosyPixelShift();
    argo1.setArgosyGapColor(PixelAudioMapper.setAlpha(argo1GapColor, argo1GapAlpha));
    if (shift != 0) {
        argo1.shift(-shift, true);
    }        
    isBufferStale = true;
}

public void gapOpacity1_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (argo1GapAlpha != val) {
        argo1GapAlpha = val;
        argo1GapColor = PixelAudioMapper.setAlpha(argo1GapColor, argo1GapAlpha);
        int shift = argo1.getArgosyPixelShift();
        argo1.setArgosyGapColor(argo1GapColor);
        if (shift != 0) {
            argo1.shift(-shift, true);
        }
        isBufferStale = true;
    }
}

public void argoStep1_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (argo1Step != val) {
        argo1Step = val;
    }
}

public void argo1ShowCheck_hit(GCheckbox source, GEvent event) {
    isShowArgo1 = source.isSelected();
}

public void argo1FreezeCheck_hit(GCheckbox source, GEvent event) {
    this.isArgo1Freeze = source.isSelected();
    println("isArgo1Freeze is "+ isArgo1Freeze);
}

public void argo1InitAndShift() {
    int shift = argo1.getArgosyPixelShift();
    this.initArgo1(shift);
    isBufferStale = true;
}

/* ----->>>   ARGOSY TWO   <<<----- */

public void genMenu2_hit(GDropList source, GEvent event) {
    argo2GenSelect = source.getSelectedIndex();
    int shift = argo2.getArgosyPixelShift();
    argo2Mapper = selectMapper(argo2GenSelect, argo2Gen);
    this.initArgo2(shift);
    isBufferStale = true;
}
public void colorMenu2_hit(GDropList source, GEvent event) {
    argo2PaletteSelect = source.getSelectedIndex();
    argo2Colors = setArgoColorsAlpha(colorVars[argo2PaletteSelect], argo2Alpha);
    int shift = argo2.getArgosyPixelShift();
    argo2.setArgosyColors(argo2Colors);
    if (shift != 0) {
        argo2.shift(-shift, true);
    }        
    isBufferStale = true;
}
public void alphaText2_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (argo2Alpha != val) {
        argo2Alpha = val;
        argo2Colors = setArgoColorsAlpha(argo2.getArgosyColors(), argo2Alpha);
        int shift = argo2.getArgosyPixelShift();
        argo2.setArgosyColors(argo2Colors);
        if (shift != 0) {
            argo2.shift(-shift, true);
        }
        isBufferStale = true;
    }        
}

public void patternMenu2_hit(GDropList source, GEvent event) {
    argo2PatternSelect = source.getSelectedIndex();
    argo2Pattern = patternVars[argo2PatternSelect];
    int shift = argo2.getArgosyPixelShift();
    argo2.setArgosyPattern(argo2Pattern);
    if (shift != 0) {
        argo2.shift(-shift, true);
    }        
    isBufferStale = true;
}

public void repText2_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (argo2Reps != val) {
        argo2Reps = val;
        int shift = argo2.getArgosyPixelShift();
        argo2.setArgosyReps(argo2Reps);
        if (shift != 0) {
            argo2.shift(-shift, true);
        }    
        isBufferStale = true;
    }    
}

public void unitSize2_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (argo2Unit != val) {
        argo2Unit = val;
        int shift = argo2.getArgosyPixelShift();
        argo2.setUnitSize(argo2Unit);
        if (shift != 0) {
            argo2.shift(-shift, true);
        }
        isBufferStale = true;
    }
}

public void gap2_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (argo2Gap != val) {
        argo2Gap = val;
        //println("--->> new argo2 gap = "+ argo2Gap);
        int shift = argo2.getArgosyPixelShift();
        argo2.setArgosyGap(argo2Gap);
        if (shift != 0) {
            argo2.shift(-shift, true);
        }
        isBufferStale = true;
    }
}

public void gapColorMenu2_hit(GDropList source, GEvent event) {
    int index = source.getSelectedIndex();
    argo2GapColor = gapColorVars[index];
    int shift = argo2.getArgosyPixelShift();
    argo2.setArgosyGapColor(PixelAudioMapper.setAlpha(argo2GapColor, argo2GapAlpha));
    if (shift != 0) {
        argo2.shift(-shift, true);
    }        
    isBufferStale = true;
}

public void gapOpacity2_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (argo2GapAlpha != val) {
        argo2GapAlpha = val;
        argo2GapColor = PixelAudioMapper.setAlpha(argo2GapColor, argo2GapAlpha);
        int shift = argo2.getArgosyPixelShift();
        argo2.setArgosyGapColor(argo2GapColor);
        if (shift != 0) {
            argo2.shift(-shift, true);
        }
        isBufferStale = true;
    }
}

public void argoStep2_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (argo2Step != val) {
        argo2Step = val;
    }
}

public void argo2ShowCheck_hit(GCheckbox source, GEvent event) {
    isShowArgo2 = source.isSelected();
}

public void argo2FreezeCheck_hit(GCheckbox source, GEvent event) {
    this.isArgo2Freeze = source.isSelected();
    println("isArgo2Freeze is "+ isArgo2Freeze);
}

public void argo2InitAndShift() {
    int shift = argo1.getArgosyPixelShift();
    this.initArgo2(shift);
    isBufferStale = true;
}

public void animOpen_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (animOpen != val) {
        animOpen = val;
    }
}

public void animClose_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (animClose != val) {
        animClose = val;
    }
}

public void animRun1_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (animRun1 != val) {
        animRun1 = val;
        //println("--->> animRun1 = "+ animRun1);
    }
}

public void animHold1_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (animHold1 != val) {
        animHold1 = val;
        //println("--->> animHold1 = "+ animHold1);
    }
}

public void animRun2_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (animRun2 != val) {
        animRun2 = val;
        //println("--->> animRun2 = "+ animRun2);
    }
}

public void animHold2_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (animHold2 != val) {
        animHold2 = val;
        //println("--->> animHold2 = "+ animHold2);
    }
}

public void animDuration_change(GTextField source, GEvent event) {
    int val = source.getValueI();
    if (animDuration != val) {
        animDuration = val;
    }
}

public void recordButton_hit(GButton source, GEvent event ) {
    this.isAnimating = true;
    this.isRecordingVideo = true;
    initAnimation();
}

/* ---------------------------------------------------------------- */
/*                                                                  */
/*                    END GUI Interface Code                        */
/*                                                                  */
/* ---------------------------------------------------------------- */
