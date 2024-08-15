public void build8x8ZZMultiGenList(int w, int h) {
  if (!validate(w, h)) return;
  if ((w % 8) % 2 == 1) {
    // w modulo 8 is an odd number
    
  }
  else if ((h % 8) % 2 == 1) {
    // h modulo 8 is an odd number
    
  }
  else {
    // w modulo 8 and h modulo 8 are both even numbers
    
  }
}

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
