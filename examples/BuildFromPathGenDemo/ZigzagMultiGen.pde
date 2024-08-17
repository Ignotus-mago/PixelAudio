public class ZigzagMultiGen extends PixelMapGen {
  public final static String description = "A PixelMapGen made of multiple zigzag parts.";

  public ZigzagMultiGen(int width, int height, AffineTransformType type) {
    super(width, height, type);
    // TODO Auto-generated constructor stub
  }

  public ZigzagMultiGen(int width, int height) {
    super(width, height);
    // TODO Auto-generated constructor stub
  }

  @Override
  public String describe() {
    return ZigzagMultiGen.description;
  }

  @Override
public boolean validate(int w, int h) {
  if(h < 8 || w < 8) {
    println("MultiZigzagGen width and height must be > 8.");
    return false;
  }
  if (w % 8 != 0 || h % 8 != 0) {
    println("MultiZigzagGen width and height must both be evenly divisible by 8.");
    return false;
  }
  return true;
}

  @Override
  public int[] generate() {
    if (this.pixelMap == null && this.coords == null) {
      System.out.println("BuildFromPathGen: You need to call setPixelMap(int[] newPixelMap) or "
                   + "setCoords(ArrayList<int[]> newCoords) before calling generate().");  
      return null;
    }
    if (this.pixelMap != null && this.coords == null) {
      this.coords = new ArrayList<int[]>(pixelMap.length);
      for (int i = 0; i < pixelMap.length; i++) {
        int pos = pixelMap[i];
        int[] xy = new int[] {pos % this.w, pos / this.w};
        coords.add(xy);
      }
    }
    this.loadIndexMaps();
    return this.pixelMap;
  }

  public void setPixelMap(int[] newPixelMap) {
    this.pixelMap = newPixelMap;
  }

}
