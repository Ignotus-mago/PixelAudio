import oscP5.*;
import netP5.*;

/**
 * A class to handle network connections over UDP, for example, with a Max or Pd patch.
 * Used by applications that implement the PANetworkClientINF.
 *
 * @see PANetworkClientINF
 */
public class NetworkDelegate {
  private PApplet parent;
  private TutorialOne_05_UDP app;
  private OscP5 osc;
  private int inPort = 7401;
  private int outPort = 7400;
  private String remoteFromAddress = "127.0.0.1";
  private String remoteToAddress = "127.0.0.1";
  // 192.168.1.77
  private NetAddress remoteFrom;
  private NetAddress remoteTo;
  private int drawCount = 0;


  public NetworkDelegate(TutorialOne_05_UDP app, String remoteFromAddr, String remoteToAddr, int inPort, int outPort) {
    this.app = app;
    this.parent = app.getPApplet();
    this.osc = new OscP5(parent, inPort);
    this.remoteFromAddress = remoteFromAddr;
    this.remoteToAddress = remoteToAddr;
    this.inPort = inPort;
    this.outPort = outPort;
    init();
  }

  public NetworkDelegate(TutorialOne_05_UDP app, String remoteFromAddr, String remoteToAddr) {
    this.app = app;
    this.parent = app.getPApplet();
    this.osc = new OscP5(parent, inPort);
    this.remoteFromAddress = remoteFromAddr;
    this.remoteToAddress = remoteToAddr;
    init();
  }

  public NetworkDelegate(TutorialOne_05_UDP app) {
    this.app = app;
    this.parent = app.getPApplet();
    this.osc = new OscP5(parent, inPort);
    init();
  }

  public void init() {
    this.remoteFrom = new NetAddress(this.remoteFromAddress, this.inPort);
    this.remoteTo = new NetAddress(this.remoteToAddress, this.outPort);
    System.out.println("== remoteFromAddress "+ remoteFromAddress +", in port: "+ inPort);
    System.out.println("== remoteToAddress "+ remoteToAddress +", out port: "+ outPort);
    initOscPlugs();
  }

  /*----------------------------------------------------------------*/
  /*                                                                */
  /*                      GETTERS AND SETTERS                       */
  /*                                                                */
  /*----------------------------------------------------------------*/

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

  /*----------------------------------------------------------------*/
  /*                                                                */
  /*                  OscP5 PLUG & MESSAGE SETUP                    */
  /*                                                                */
  /*----------------------------------------------------------------*/

  /**
   * SET UP RESPONSE TO INCOMING MESSAGES
   * Call the osc.plug(Object theObject, String the MethodName, String theAddrPattern)
   * or osc.plug(Object theObject, String the MethodName, String theAddrPattern, String the TypeTag)
   * to setup callbacks to methods in the current object ("this") or other object instance.
   */
  public void initOscPlugs() {
    osc.plug(this, "sampleHit", "/sampleHit");
    osc.plug(this, "drawHit", "/draw");
    osc.plug(this, "multislider", "/multislider");
    osc.plug(this, "parseKey", "/parseKey");
    osc.plug(this, "controlMsg", "/controlMsg");
  }


  /* incoming osc message are forwarded to the oscEvent method. */
  void oscEvent(OscMessage theOscMessage) {
    /* print the address pattern and the typetag of the received OscMessage */
    PApplet.print("### received an osc message.");
    PApplet.print(" addrpattern: "+theOscMessage.addrPattern());
    PApplet.println(" typetag: "+theOscMessage.typetag());
  }


  /*----------------------------------------------------------------*/
  /*                                                                */
  /*                   OUTGOING MESSAGE METHODS                     */
  /*                                                                */
  /*----------------------------------------------------------------*/

  public void oscSendMousePressed(int sampleX, int sampleY, int sample) {
    OscMessage msg = new OscMessage("/press");
    msg.add(sample);
    msg.add(sampleX);
    msg.add(sampleY);
    osc.send(msg, this.remoteTo);
    // PApplet.println("---> msg: "+ msg);
  }

  public void oscSendMultiSlider(ArrayList<PVector> drawPoints) {
    int y0 = (int) drawPoints.get(0).y;
    int sliders = 12;
    int[] multi = new int[sliders];
    int i = 0;
    for (PVector vec : drawPoints) {
      if (i < sliders) {
        multi[i++] = ((int) PApplet.abs(y0 - vec.y)) % 128;
      }
    }
    OscMessage msg = new OscMessage("/multislider");
    for (i = 0; i < multi.length; i++) {
      msg.add(multi[i]);
    }
    osc.send(msg, this.remoteTo);
  }

  public void oscSendDrawPoints(ArrayList<PVector> drawPoints) {
    OscMessage msg = new OscMessage("/draw");
    int i = 0;
    msg.add(++this.drawCount);
    for (PVector vec : drawPoints) {
      msg.add(i++);
      int x = (int) vec.x;
      int y = (int) vec.y;
      msg.add(this.app.getMapper().lookupSample(x, y));
      msg.add(x);
      msg.add(y);
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

  public void oscSendFileInfo(String path, String name, String tag) {
    OscMessage msg = new OscMessage("/file");
    msg.add(path);
    msg.add(name);
    msg.add(tag);
    osc.send(msg, this.remoteTo);
  }

  /*----------------------------------------------------------------*/
  /*                                                                */
  /*                      OscP5 PLUG METHODS                        */
  /*                                                                */
  /*----------------------------------------------------------------*/


  /*
   * OscP5 plug-in methods, which call methods on the client,
   * are implemented in this section. They take calls from
   * the service (UDP, here) and pass them on to the client.
   * If you want to extend the available calls, modify the
   * PANetworkClientINF interface to make the contracts
   * between client and delegate explicit.
   */

  public void sampleHit(int sam) {
    int[] xy = app.getMapper().lookupCoordinate(sam);
    PApplet.println("---> sampleHit " + xy[0], xy[1]);
    app.playSample(sam);
  }

  public void drawHit(int... args) {
    ArrayList<PVector> pts = new ArrayList<PVector>();
    PApplet.println("---> drawHit "+ args.length);
    for (int pos : args) {
      int[] xy = app.getMapper().lookupCoordinate(pos);
      pts.add(new PVector(xy[0], xy[1]));
      // PApplet.println("  "+ xy[0] +", "+ xy[1]);
    }
    app.playPoints(pts);
  }

  public void multislider(int... args) {
    PApplet.print("---> multislider: ");
    for (int pos : args) {
      PApplet.print(pos + ", ");
    }
    PApplet.println();
  }

  public void parseKey(int arg) {
    char ch = PApplet.parseChar(arg);
    PApplet.println("---> parseKey: "+ ch);
    app.parseKey(ch, 0);
  }

  public void controlMsg(String ctrl, float val) {
    PApplet.println("---> control: "+ ctrl +", value: "+ val);
  }
}
