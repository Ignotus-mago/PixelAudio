/**
 * Interface for applications extending PApplet that use NetworkDelegate to send and receive UDP messages.
 * @see NetworkDelegate
 */
public interface PANetworkClientINF {

  public PixelAudioMapper getMapper();

  public int playSample(int samplePos);

  public void playPoints(ArrayList<PVector> pts);

  public void parseKey(char key, int keyCode);

  public void controlMsg(String control, float val);

  public PApplet getPApplet();

  // whatever else we need can be added
}
