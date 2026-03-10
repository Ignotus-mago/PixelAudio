  // required by the PANetworkCLientINF interface
  public PApplet getPApplet() {
    return this;
  }

  // required by the PANetworkCLientINF interface
  public PixelAudioMapper getMapper() {
    return this.mapper;
  }
  
  // required by the PANetworkCLientINF interface
  public void controlMsg(String control, float val) {
    if (control.equals("detune")) {
        println("--->> controlMsg is \"detune\" = "+ val +"; detune is not implemented.");
    }
  }

  // required by the PANetworkCLientINF interface
  public int playSample(int samplePos) {
    int[] coords = mapper.lookupImageCoord(samplePos);
    int x = coords[0];
    int y = coords[1];
    return handleClickOutsideBrush(x, y);
  }

  // required by the PANetworkCLientINF interface
  public void playPoints(ArrayList<PVector> pts) {
    float[] times = new float[pts.size()];
    int interval = noteDuration / 4;
    for (int i = 0; i < pts.size(); i++) {
      times[i] = i * interval;
    }
    GestureSchedule sched = new GestureSchedule(pts, times);
    storeSamplerCurveTL(sched, millis() + 10);
  }
  
  
