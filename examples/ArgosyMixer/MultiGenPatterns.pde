/*----------------------------------------------------------------*/
/*                                                                */
/*                 BEGIN PATTERN MAKING METHODS                   */
/*                                                                */
/*      General versions of some of these methods are available   */
/*      in the PixelAudio library. Use these as examples          */
/*      of how to roll your own MultiGens.                        */
/*                                                                */
/*----------------------------------------------------------------*/
  
/**
 * hilbertLoop3x2() returns a looping fractal signal path consisting of 
 * 6 Hilbert gens, 3 wide by 2 tall, to fit a 3 * genW by 2 * genH image. 
 * 
 * Like all the methods that follow, this one creates a MultiGen instance
 * from a list of PixelMapGen objects (genList) and coordinate points (offsetList)
 * where they will be displayed. MultiGen creates a single signal path over all
 * the PixelMapGen objects. The path may be *continuous*, which is to say that
 * the path through each PixelMapGen object ("gen" for short) only has to step
 * one pixel up, down, left, or right to connect to the next gen. It may even
 * create a loop, where the last pixel in the path is one step away from the
 * first pixel. This is reflected in the naming conventions. 
 * 
 * In the method names, "ortho" refers to gens that are aligned in rows (or
 * columns) where each new row begins one unit down from the previous row,
 * always adding new gens in the same direction. In the "bou" methods 
 * (named for boustrophodon, a method of writing text in alternating directions), 
 * each successive row or column goes in the opposite direction from the previous
 * one. The bou methods may provide continuous paths, the ortho methods are
 * inherently discontinous, like row major bitmaps or video scanlines. 
 * 
 * Looping methods are are almost always more complex than bou and necessarily 
 * more complex than ortho methods. Like the Hilbert curve, they involve
 * changes in direction reminiscent of folding. Looping methods often have
 * constraints on the numbers of rows and columns that can produce a loop.
 * The constraints arise from the connectivity offered by the different
 * PixelMapGen child classes: Hilbert gens have connections at two adjacent
 * corners, DiagonalZigzag gens have connections at opposite corners. 
 * Moore gens are loops to begin with, and have no connections, but are
 * good for very symmetrical pattern-making.  
 * 
 * 
 * Note that genH must equal genW and both must be powers of 2. For the 
 * image size we're using in this example, genW = image width / 3 and 
 * genH = image height / 2.
 * 
 * @param genW    width of each HilbertGen 
 * @param genH    height of each HilbertGen
 * @return        a MultiGen consisting of 6 HilbertGens linked together by one signal path
 */
public MultiGen hilbertLoop3x2(int genW, int genH) {
    // list of PixelMapGens that create a path through an image using PixelAudioMapper
  ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
  // list of x,y coordinates for placing gens from genList
  ArrayList<int[]> offsetList = new ArrayList<int[]>();     
  genList.add(new HilbertGen(genW, genH, fx270));
  offsetList.add(new int[] { 0, 0 });
  genList.add(new HilbertGen(genW, genH, nada));
  offsetList.add(new int[] { genW, 0 });
  genList.add(new HilbertGen(genW, genH, fx90));
  offsetList.add(new int[] { 2 * genW, 0 });
  genList.add(new HilbertGen(genW, genH, fx90));
  offsetList.add(new int[] { 2 * genW, genH });
  genList.add(new HilbertGen(genW, genH, r180));
  offsetList.add(new int[] { genW, genH });
  genList.add(new HilbertGen(genW, genH,fx270));
  offsetList.add(new int[] { 0, genH });
  return new MultiGen(width, height, offsetList, genList);
}

/**
 * This method creates a MultiGen consisting of a mix of zigzag and Hilbert curves
 * in 6 columns and 4 rows arranged to provide a continuous loop.
 * 
 * @param genW
 * @param genH
 * @return
 */
public MultiGen hilbertZigzagLoop6x4(int genW, int genH) {
    // list of PixelMapGens that create a path through an image using PixelAudioMapper
  ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
  // list of x,y coordinates for placing gens from genList
  ArrayList<int[]> offsetList = new ArrayList<int[]>();     
  int[][] locs = {{0,0}, {0,1}, {0,2}, {0,3}, {1,3}, {1,2}, {2,2}, {2,3}, 
          {3,3}, {3,2}, {4,2}, {4,3}, {5,3}, {5,2}, {5,1}, {5,0},
          {4,0}, {4,1}, {3,1}, {3,0}, {2,0}, {2,1}, {1,1}, {1,0}};
  AffineTransformType[] trans = {r270, r270, nada, r270, r90, fx270, nada, r270, 
                             r90, r90, fx90, nada, r90, r90, r180, r90, 
                             r270, fx90, r180, r90, r270, r270, fx270, r180};
  char[] cues = {'H','D','D','H','D','H','D','H', 
             'H','D','H','D','H','D','D','H',
             'D','H','D','H','H','D','H','D'}; 
  int i = 0;
  for (AffineTransformType att: trans) {
    int x = locs[i][0] * genW;
    int y = locs[i][1] * genH;
    offsetList.add(new int[] {x,y});
    // println("locs: ", locs[i][0], locs[i][1]);
    if (cues[i] == 'H') {
      genList.add(new HilbertGen(genW, genH, att));    
    }
    else {
      genList.add(new DiagonalZigzagGen(genW, genH, att));    
    }
    i++;
  }
  return new MultiGen(width, height, offsetList, genList);
}
  
/**
 * This method creates a vertical stacks of rows of HilbertGens. Each row
 * begins genH pixels down from the previous row, back at the beginning
 * of the previous row (i.e., in "row major" order, like a bitmap). This 
 * method pairs nicely with an image with 3 columns of with 8 rows of words,
 * using the image as a control surface for sampling an audio file with 
 * words recorded at the appropriate locations to match the screen order. 
 * I used it for a performance work, DeadBodyWorkFlow, which is included
 * in the 
 * The signal path jumps from the end of the last gen in each row to the 
 * beginning of the first gen int he next row. The path in each row is
 * continuous, which provides some interesting optical effects. 
 * 
 * @param stacks    the number of stacks 
 * @param rows      the number of rows in each stack
 * @param units     the number of gens in each row
 * @param genW      the width of each gen, a power of 2
 * @param genH      the height of each gen, equal to genW
 * @return          a Multigen consisting of stacks * rows * units PixelMapGens
 */
public MultiGen hilbertStackOrtho(int stacks, int rows, int units, int genW, int genH) {
    // list of PixelMapGens that create a path through an image using PixelAudioMapper
    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
    // list of x,y coordinates for placing gens from genList
    ArrayList<int[]> offsetList = new ArrayList<int[]>();   
    for (int s = 0; s < stacks; s++) {
        for (int r = 0; r < rows; r++) {
          int shift = s * units;
            for (int u = 0; u < units; u++) {
                genList.add(new HilbertGen(genW, genH));
                offsetList.add(new int[] {(u + shift) * genW, r * genH});
            }
        }
    }
    return new MultiGen(width, height, offsetList, genList);
}
/**
 * This method creates a vertical stacks of rows of HilbertGens. Each row
 * begins genH pixels down from the previous row. Alternating rows add units
 * in opposite directions. This means path continuity is possible in each 
 * stack by changing the orientation of the gens; however, it isn't fully 
 * implemented in this example. Hint: choosing the right orientation for 
 * each gen will assure path continuity. 
 * 
 * @param stacks    the number of stacks 
 * @param rows      the number of rows in each stack
 * @param units     the number of gens in each row
 * @param genW      the width of each gen, a power of 2
 * @param genH      the height of each gen, equal to genW
 * @return          a Multigen consisting of stacks * rows * units PixelMapGens
 */
public MultiGen hilbertStackBou(int stacks, int rows, int units, int genW, int genH) {
    // list of PixelMapGens that create a path through an image using PixelAudioMapper
    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
    // list of x,y coordinates for placing gens from genList
    ArrayList<int[]> offsetList = new ArrayList<int[]>();   
    for (int s = 0; s < stacks; s++) {
        for (int r = 0; r < rows; r++) {
          int shift = s * units;
            if (r % 2 == 1) {
                for (int u = 0; u < units; u++) {
                    genList.add(new HilbertGen(genW, genH, flipx));
                    offsetList.add(new int[] {(u + shift) * genW, r * genH});
                }
            }
            else {
                for (int u = units; u > 0; u--) {
                    genList.add(new HilbertGen(genW, genH, flipy));
                    offsetList.add(new int[] {(u + shift - 1) * genW, r * genH});
                }
            }
        }
    }
    return new MultiGen(width, height, offsetList, genList);
}

/**
 * This method creates rows of HilbertGens, starting each row from the left
 * and adding gens. The odd rows are flipped vertically and the even rows are
 * unchanged. The unchanged HilbertGen starts at upper left corner and ends at 
 * upper right corner, so this provides some possibilities of symmetry between rows.
 * The path is not continuous. 
 * 
 * @param cols    number of columns of gens wide
 * @param rows    number of rows of gens high
 * @param genW    width of each gen (same as genH and a power of 2)
 * @param genH    height of each gen 
 * @return        a MultiGen composed of cols * rows PixelMapGens
 */
public MultiGen hilbertRowOrtho(int cols, int rows, int genW, int genH) {
    // list of PixelMapGens
    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
    // list of x,y coordinates for placing gens from genList
    ArrayList<int[]> offsetList = new ArrayList<int[]>();
  for (int y = 0; y < rows; y++) {
    for (int x = 0; x < cols; x++) {
      if (y % 2 == 0) {
        genList.add(new HilbertGen(genW, genH, nada));
      }
      else {
        genList.add(new HilbertGen(genW, genH, flipy));
      }
      offsetList.add(new int[] {x * genW, y * genH});
    }
  }
  return new MultiGen(width, height, offsetList, genList);
}

/**
 * This variation on hilbertRowOrtho arranges the gens vertically, in columns. 
 * 
 * @param cols    number of columns of gens wide
 * @param rows    number of rows of gens high
 * @param genW    width of each gen (same as genH and a power of 2)
 * @param genH    height of each gen 
 * @return        a MultiGen composed of cols * rows PixelMapGens
 */
public MultiGen hilbertColumnOrtho(int cols, int rows, int genW, int genH) {
    // list of PixelMapGens that create a path through an image using PixelAudioMapper
    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
    // list of x,y coordinates for placing gens from genList
    ArrayList<int[]> offsetList = new ArrayList<int[]>();
  for (int x = 0; x < cols; x++) {
    for (int y = 0; y < rows; y++) {
      if (x % 2 == 0) {
        genList.add(new HilbertGen(genW, genH, r270));
      }
      else {
        genList.add(new HilbertGen(genW, genH, r90));
      }
      offsetList.add(new int[] {x * genW, y * genH});
    }
  }
  return new MultiGen(width, height, offsetList, genList);
}

/**
 * @param genW    width of each zigzag gen
 * @param genH    height of each zigzag gen
 * @return        a looping MultiGen with 6 rows x 4 columns of DiagonalZigzagGen instances
 */
public MultiGen zigzagLoop6x4(int genW, int genH) {
    // list of PixelMapGens that create a path through an image using PixelAudioMapper
    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
    // list of x,y coordinates for placing gens from genList
    ArrayList<int[]> offsetList = new ArrayList<int[]>();
    int[][] locs = {{0,0}, {1,0}, {2,0}, {3,0}, {4,0}, {5,0}, 
        {5,1}, {5,2}, {5,3}, {4,3}, {4,2}, {4,1},
        {3,1}, {3,2}, {3,3}, {2,3}, {2,2}, {2,1},
        {1,1}, {1,2}, {1,3}, {0,3}, {0,2}, {0,1}};
  AffineTransformType[] trans = {r90, fx90, r90, fx90, r90, fx90, 
                             r270, fx90, r270, fx270, r90, fx270, 
                             r270, fx90, r270, fx270, r90, fx270, 
                             r270, fx90, r270, fx270, r90, fx270};
  int i = 0;
  for (AffineTransformType att: trans) {
      int x = locs[i][0] * genW;
      int y = locs[i][1] * genH;
      offsetList.add(new int[] {x,y});
      // println("locs: ", locs[i][0], locs[i][1]);
      genList.add(new DiagonalZigzagGen(genW, genH, att));
      i++;
  }
  return new MultiGen(width, height, offsetList, genList);
}

/**
 * Creates a MultiGen with rows * cols DiagonalZigzagGens. 
 * Note that you should set values for such that:
 * (rows * genW) == width and (cols * genH) == height.
 * "Ortho" implies that each row starts at the left edge.
 * The orientation of the the diagonals changes for alternate rows.
 * 
 * @param cols    number of vertical columns
 * @param rows    number of horiaontal rows 
 * @param genW    width of an individual PixelMapGen
 * @param genH    height of an indvidual PixelMapGen
 * @return        a MultiGen created from rows * cols PixelMapGens
 */
public MultiGen zigzagRowOrtho(int cols, int rows, int genW, int genH) {
    // list of PixelMapGens that create an image using mapper
    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
    // list of x,y coordinates for placing gens from genList
    ArrayList<int[]> offsetList = new ArrayList<int[]>();
  for (int y = 0; y < rows; y++) {
    for (int x = 0; x < cols; x++) {
      if (y % 2 == 0) {
        genList.add(new DiagonalZigzagGen(genW, genH, flipy));
      }
      else {
        genList.add(new DiagonalZigzagGen(genW, genH, nada));
      }
      offsetList.add(new int[] {x * genW, y * genH});
    }
  }
  return new MultiGen(width, height, offsetList, genList);
}

/**
 * Creates a MultiGen with rows * cols DiagonalZigzagGens. 
 * Note that you should set values for such that:
 * (rows * genW) == width and (cols * genH) == height.
 * "Ortho" implies that each row starts at the left edge.
 * The orientation of the the diagonals is more complex than 
 * zigzagRowOrtho(), hence the "Alt" in the method name.
 * 
 * @param cols    number of vertical columns
 * @param rows    number of horiaontal rows 
 * @param genW    width of an individual PixelMapGen
 * @param genH    height of an indvidual PixelMapGen
 * @return        a MultiGen created from rows * cols PixelMapGens
 */
public MultiGen zigzagRowAltOrtho(int cols, int rows, int genW, int genH) {
  // list of PixelMapGens that create an image using mapper
  ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
  // list of x,y coordinates for placing gens from genList
  ArrayList<int[]> offsetList = new ArrayList<int[]>();
  for (int y = 0; y < rows; y++) {
    for (int x = 0; x < cols; x++) {
      if (y % 2 == 0) {
        if (x % 2 == 0) {
          genList.add(new DiagonalZigzagGen(genW, genH, flipy));
        }
        else {
          genList.add(new DiagonalZigzagGen(genW, genH, nada));
        }
        offsetList.add(new int[] { x * genW, y * genH });
      } 
      else {
        if (x % 2 == 0) {
          genList.add(new DiagonalZigzagGen(genW, genH, nada));
        } 
        else {
          genList.add(new DiagonalZigzagGen(genW, genH, flipy));
        }
        offsetList.add(new int[] { x * genW, y * genH });
      }
    }
  }
  return new MultiGen(width, height, offsetList, genList);
}

/**
 * Creates a MultiGen with rows * cols DiagonalZigzagGens. 
 * Note that you should set values for such that:
 * (rows * genW) == width and (cols * genH) == height.
 * The MultiGen steps along columns rather than rows. 
 * "Ortho" implies that each column starts at the top edge.
 * The orientation of the diagonals alternates one column to the next.
 * 
 * @param cols    number of vertical columns
 * @param rows    number of horiaontal rows 
 * @param genW    width of an individual PixelMapGen
 * @param genH    height of an indvidual PixelMapGen
 * @return        a MultiGen created from rows * cols PixelMapGens
 */ 
public MultiGen zigzagColumnOrtho(int cols, int rows, int genW, int genH) {
  // list of PixelMapGens that create a path through an image using
  // PixelAudioMapper
  ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
  // list of x,y coordinates for placing gens from genList
  ArrayList<int[]> offsetList = new ArrayList<int[]>();
  for (int x = 0; x < cols; x++) {
    for (int y = 0; y < rows; y++) {
      if (x % 2 == 0) {
        genList.add(new DiagonalZigzagGen(genW, genH, fx270));
      } 
      else {
        genList.add(new DiagonalZigzagGen(genW, genH, r90));
      }
      offsetList.add(new int[] { x * genW, y * genH });
    }
  }
  return new MultiGen(width, height, offsetList, genList);
}

/**
 * Creates a MultiGen with rows * cols DiagonalZigzagGens. 
 * Note that you should set values for such that:
 * (rows * genW) == width and (cols * genH) == height.
 * The MultiGen steps along columns rather than rows. 
 * "Ortho" implies that each column starts at the top edge.
 * The orientation of the diagonals is more complex than zigzagColumnOrtho(),
 * hence "Alt" in the method name.
 * 
 * @param cols    number of vertical columns wide
 * @param rows    number of horiaontal rows high
 * @param genW    width of an individual PixelMapGen
 * @param genH    height of an indvidual PixelMapGen
 * @return        a MultiGen created from rows * cols PixelMapGens
 */ 
public MultiGen zigzagColumnAltOrtho(int cols, int rows, int genW, int genH) {
    // list of PixelMapGens that create a path through an image using PixelAudioMapper
    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
    // list of x,y coordinates for placing gens from genList
    ArrayList<int[]> offsetList = new ArrayList<int[]>();
  for (int x = 0; x < cols; x++) {
    for (int y = 0; y < rows; y++) {
      if (y % 2 == 0) {
        if (x % 2 == 0) {
          genList.add(new DiagonalZigzagGen(genW, genH, fx270));
        }
        else {
          genList.add(new DiagonalZigzagGen(genW, genH, r90));
        }          
      }
      else {
        if (x % 2 == 0) {
          genList.add(new DiagonalZigzagGen(genW, genH, r90));
        }
        else {
          genList.add(new DiagonalZigzagGen(genW, genH, fx270));
        }          
      }
      offsetList.add(new int[] {x * genW, y * genH});
    }
  }
  return new MultiGen(width, height, offsetList, genList);
}

/**
 * Creates a MultiGen with rows * cols DiagonalZigzagGens. 
 * Note that you should set values for such that:
 * (rows * genW) == width and (cols * genH) == height.
 * The MultiGen steps along rows. 
 * "Ortho" implies that each row starts at the left edge.
 * The orientation of the diagonals is randomized.
 * 
 * @param cols    number of vertical columns
 * @param rows    number of horiaontal rows 
 * @param genW    width of an individual PixelMapGen
 * @param genH    height of an indvidual PixelMapGen
 * @return        a MultiGen created from rows * cols PixelMapGens
 */ 
public MultiGen zigzagRowRandomFlip(int cols, int rows, int genW, int genH) {
    // list of PixelMapGens that create an image using mapper
    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
    // list of x,y coordinates for placing gens from genList
    ArrayList<int[]> offsetList = new ArrayList<int[]>();
    for (int y = 0; y < rows; y++) {
        for (int x = 0; x < cols; x++) {
            genList.add(new DiagonalZigzagGen(genW, genH, randomTransform()));
            offsetList.add(new int[] {x * genW, y * genH});
        }
    }
    return new MultiGen(width, height, offsetList, genList);
}

/**
 * Creates a MultiGen with rows * cols BoustropheGens. 
 * Note that you should set values for such that:
 * (rows * genW) == width and (cols * genH) == height.
 * 
 * @param cols    number of vertical columns wide
 * @param rows    number of horiaontal rows high
 * @param genW    width of an individual PixelMapGen
 * @param genH    height of an indvidual PixelMapGen
 * @return        a MultiGen created from rows * cols PixelMapGens
 */ 
public MultiGen boustrophRowRandom(int cols, int rows, int genW, int genH) {
    // list of PixelMapGens that create an image using mapper
    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
    // list of x,y coordinates for placing gens from genList
    ArrayList<int[]> offsetList = new ArrayList<int[]>();
    for (int y = 0; y < rows; y++) {
        for (int x = 0; x < cols; x++) {
            genList.add(new BoustropheGen(genW, genH, randomTransform()));
            offsetList.add(new int[] {x * genW, y * genH});
        }
    }
    return new MultiGen(width, height, offsetList, genList);
}

/**
 * Creates a MultiGen with rows * cols HilbertGens.
 * Note that you should set values for such that:
 * (rows * genW) == width and (cols * genH) == height
 * and that genH == genW and both are powers of 2.
 * The orientation of the HilbertGens is randomized.
 * 
 * @param cols    number of vertical columns wide
 * @param rows    number of horiaontal rows high
 * @param genW    width of an individual PixelMapGen
 * @param genH    height of an indvidual PixelMapGen
 * @return        a MultiGen created from rows * cols PixelMapGens
 */
public MultiGen hilbertRowRandomFlip(int cols, int rows, int genW, int genH) {
    // list of PixelMapGens that create an image using mapper
    ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>(); 
    // list of x,y coordinates for placing gens from genList
    ArrayList<int[]> offsetList = new ArrayList<int[]>();
    for (int y = 0; y < rows; y++) {
        for (int x = 0; x < cols; x++) {
            genList.add(new HilbertGen(genW, genH, randomTransform()));
            offsetList.add(new int[] {x * genW, y * genH});
        }
    }
    return new MultiGen(width, height, offsetList, genList);
}

/**
 * Returns a randomly selected AffineTransformType. @see PixelMapGen.
 * @return    a random element from transArray
 */
public AffineTransformType randomTransform() {
  return this.transArray[rand.nextInt(this.transArray.length)];
}

/**
 * A method for creating a PixelMapGen (MultiGen) to initialize and return a PixelAudioMapper, 
 * called by the GUI methods genMenu1_hit() and genMenu2_hit().
 * 
 * @param selector    number to select a particular method to create MultiGen, keyed to GUI.
 * @param gen         a PixelMapGen object that will contain the new PixelMapGen on completion
 * @return            a PixelAudioMapper created with the new PixelMapGen
 */
public PixelAudioMapper selectMapper(int selector, PixelMapGen gen) {
    switch (selector) {
    case(0):
        gen = hilbertLoop3x2(width/3, height/2);
        break;
    case(1):
        gen = hilbertZigzagLoop6x4(width/6, height/4);
        break;
    case(2):
        gen = hilbertStackOrtho(3, 8, 4, imageWidth/12, imageHeight/8);
        break;
    case(3):
        gen = hilbertStackBou(3, 8, 4, imageWidth/12, imageHeight/8);
        break;
    case(4):
        gen = hilbertRowOrtho(6, 4, height/4, width/6);
        break;
    case(5):
        gen = hilbertColumnOrtho(6, 4, height/4, width/6);
        break;
    case(6):
        gen = zigzagLoop6x4(width/6, height/4);
        break;
    case(7):
        gen = zigzagRowOrtho(6, 4, width/6, height/4);
        break;
    case(8):
        gen = zigzagRowAltOrtho(6, 4, width/6, height/4);
        break;
    case(9): 
        gen = zigzagColumnOrtho(6, 4, width/6, height/4);
        break;
    case(10): 
        gen = zigzagColumnAltOrtho(6, 4, width/6, height/4);
        break;
    case(11): 
        gen = zigzagRowRandomFlip(6, 4, width/6, height/4);
        break;
    case(12): 
        gen = zigzagRowRandomFlip(12, 8, width/12, height/8);
        break;
    case(13): 
        gen = boustrophRowRandom(6, 4, width/6, height/4);
        break;
    case(14): 
        gen = hilbertRowRandomFlip(6, 4, width/6, height/4);
        break;
    case(15): 
        gen = hilbertRowRandomFlip(12, 8, width/12, height/8);
        break;
    default: 
        gen = hilbertLoop3x2(width/3, height/2);
        break;
    }
    return new PixelAudioMapper(gen);
}

/*------------------------------------------------------------------*/
/*                                                                  */
/*                    END PATTERN MAKING METHODS                    */
/*                                                                  */
/*------------------------------------------------------------------*/
