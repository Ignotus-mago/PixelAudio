import oscP5.*;
import netP5.*;

public class NetworkDelegate {
  private OscP5 osc;
  private int inPort = 7401;
  private int outPort = 7400;
  private String remoteFromAddress = "127.0.0.1";
  private String remoteToAddress = "127.0.0.1";
  // 192.168.1.77
  private NetAddress remoteFrom;
  private NetAddress remoteTo;
  private int drawCount = 0;
  

  public NetworkDelegate() {
    this.osc = new OscP5(this, inPort);
    init();
  }
  
  public NetworkDelegate(String remoteFromAddr, String remoteToAddr) {
    this.osc = new OscP5(this, inPort);
    this.remoteFromAddress = remoteFromAddr;
    this.remoteFromAddress = remoteToAddr;
    init();
  }
  
  public NetworkDelegate(String remoteFromAddr, String remoteToAddr, int inPort, int outPort) {
    this.osc = new OscP5(this, inPort);
    this.remoteFromAddress = remoteFromAddr;
    this.remoteFromAddress = remoteToAddr;
    this.inPort = inPort;
    this.outPort = outPort;
    init();
  }
  
  public void init() {
    this.remoteFrom = new NetAddress(this.remoteFromAddress, this.inPort);
    this.remoteTo = new NetAddress(this.remoteToAddress, this.outPort);
    initOscPlugs();    
  }
  
  
  public int getInPort() {
    return inPort;
  }

  public void setInPort(int inPort) {
    this.inPort = inPort;
  }

  public int getOutPort() {
    return outPort;
  }

  public void setOutPort(int outPort) {
    this.outPort = outPort;
  }

  public NetAddress getRemoteFrom() {
    return remoteFrom;
  }

  public void setRemoteFrom(NetAddress remoteFrom) {
    this.remoteFrom = remoteFrom;
  }

  public NetAddress getRemoteTo() {
    return remoteTo;
  }

  public void setRemoteTo(NetAddress remoteTo) {
    this.remoteTo = remoteTo;
  }

  public int getDrawCount() {
    return drawCount;
  }

  public void setDrawCount(int drawCount) {
    this.drawCount = drawCount;
  }

  /**
   * Call the osc.plug(Object theObject, String the MethodName, String theAddrPattern)
   * or osc.plug(Object theObject, String the MethodName, String theAddrPattern, String the TypeTag)
   * to setup callbacks to methods in the current object ("this") or other object instance. 
   */
  public void initOscPlugs() {
    osc.plug(this, "sampleHit", "/sampleHit");
    osc.plug(this, "drawHit", "/draw");
    osc.plug(this, "multislider", "/multislider");
  }


  /* incoming osc message are forwarded to the oscEvent method. */
  void oscEvent(OscMessage theOscMessage) {
    /* print the address pattern and the typetag of the received OscMessage */
    PApplet.print("### received an osc message.");
    PApplet.print(" addrpattern: "+theOscMessage.addrPattern());
    PApplet.println(" typetag: "+theOscMessage.typetag());
  }

  public void oscSendMousePressed(int sampleX, int sampleY, int sample) {
    OscMessage msg = new OscMessage("/press");
    msg.add(sample);
    msg.add(sampleX);
    msg.add(sampleY);
    osc.send(msg, this.remoteTo);
    PApplet.println("---> msg: "+ msg);
  }

  public void oscSendDrawPoints(ArrayList<PVector> drawPoints) {
    OscMessage msg = new OscMessage("/draw");
    int y0 = (int) drawPoints.get(0).y;
    int sliders = 12;
    int[] multi = new int[sliders];
    int i = 0;
    msg.add(++this.drawCount);
    for (PVector vec : drawPoints) {
    msg.add(i + 1);
    int x = (int) vec.x;
    int y = (int) vec.y;
      msg.add(mapper.lookupSample(x, y));
      msg.add(x);
      msg.add(y);
      //msg.add("\n");
      if (i < sliders) {
        multi[i++] = ((int) PApplet.abs(y0 - vec.y)) % 128;
      }
    }
    osc.send(msg, this.remoteTo);
    // multislider message, a test
    msg = new OscMessage("/multislider");
    for (i = 0; i < multi.length; i++) {
      msg.add(multi[i]);
    }
    osc.send(msg, this.remoteTo);
  }  
  
  public void oscSendTimeStamp(int timeStamp, int timeOffset) {
    OscMessage msg = new OscMessage("/time");
    msg.add(drawCount);
    msg.add(timeStamp);
    msg.add(timeOffset);
    osc.send(msg, this.remoteTo);
  }
      
  public void oscSendTrig(int index) {
    OscMessage msg = new OscMessage("/trig");
    msg.add(index);
    osc.send(msg, this.remoteTo);
  }
  
  public void oscSendDelete(int index) {
    OscMessage msg = new OscMessage("/del");
    msg.add(index);
    osc.send(msg, this.remoteTo);
  }
  
  public void oscSendClear() {
    OscMessage msg = new OscMessage("/clear");
    osc.send(msg, this.remoteTo);
  }
      

  
  public void sampleHit(int sam) {
    int[] xy = mapper.lookupCoordinate(sam);
    PApplet.println("---> sampleHit " + xy[0], xy[1]);
    playSample(sam);
  }

  public void drawHit(int... args) {
    ArrayList<PVector> pts = new ArrayList<PVector>();
    for (int pos : args) {
      int[] xy = mapper.lookupCoordinate(pos);
      PApplet.println("---> drawHit " + xy[0], xy[1], pos);
      pts.add(new PVector(xy[0], xy[1]));
    }
    playPoints(pts);
  }

  public void multislider(int... args) {
    PApplet.print("---> multislider: ");
    for (int pos : args) {
      PApplet.print(pos + ", ");
    }
    PApplet.println();
  }

}
