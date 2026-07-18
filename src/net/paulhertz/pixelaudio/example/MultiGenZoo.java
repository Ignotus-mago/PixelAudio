package net.paulhertz.pixelaudio.example;

import java.util.ArrayList;
import java.util.Random;

import net.paulhertz.pixelaudio.AffineTransformType;
import net.paulhertz.pixelaudio.BoustropheGen;
import net.paulhertz.pixelaudio.DiagonalZigzagGen;
import net.paulhertz.pixelaudio.HilbertGen;
import net.paulhertz.pixelaudio.MultiGen;
import net.paulhertz.pixelaudio.PixelAudio;
import net.paulhertz.pixelaudio.PixelAudioMapper;
import net.paulhertz.pixelaudio.PixelMapGen;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * MultiGenZoo: a small sketch showing different ways to create MultiGens, copied and condensed from 
 * {@link ArgosyMixer}.
 * <figure>
 * <img src="doc-files/multigenzoo.png" alt="MultiGenZoo Screen" width="768" height="526"/>
 * <figcaption>MultiGen composed of row-wise connected HilbertGen objects.</figcaption>
 * </figure>
 * <p>
 * The display framework borrows the useful parts of MultiGenLookupTables: each
 * MultiGen is drawn as a colored signal path, with optional path lines, so the
 * orientation of the component PixelMapGens is easy to inspect.
 * </p><p>
 * <b>NAMING CONVENTIONS</b>
 * </p><p>
 * As all the methods that follow below, hilbertLoop3x2() creates a MultiGen instance
 * from a list of PixelMapGen objects (genList) and coordinate points (offsetList)
 * where they will be displayed. MultiGen creates a single signal path over all
 * the PixelMapGen objects. The path may be *continuous*, which is to say that
 * the path through each PixelMapGen object ("gen" for short) only has to step
 * one pixel up, down, left, or right to connect to the next gen. It may even
 * create a loop, where the last pixel in the path is one step away from the
 * first pixel. This is reflected in the naming conventions.
 * </p><p>
 * In the method names, "ortho" refers to gens that are aligned in rows (or
 * columns) where each new row begins one unit down from the previous row,
 * always adding new gens in the same direction. In the "bou" methods
 * (named for boustrophodon, a method of writing text in alternating directions),
 * each successive row or column goes in the opposite direction from the previous
 * one. The bou methods may provide continuous paths, the ortho methods are
 * inherently discontinous, like row major bitmaps or video scanlines.
 * </p><p>
 * Looping methods are are almost always more complex than bou and necessarily
 * more complex than ortho methods. Like the Hilbert curve, they involve
 * changes in direction reminiscent of folding. Looping methods often have
 * constraints on the numbers of rows and columns that can produce a loop.
 * The constraints arise from the connectivity offered by the different
 * PixelMapGen child classes: Hilbert gens have connections at two adjacent
 * corners, DiagonalZigzag gens have connections at opposite corners.
 * Moore gens are loops to begin with, and have no connections, but are
 * good for very symmetrical pattern-making.
 * </p>
 * <pre>
 * KEY COMMANDS
 *
 * Press 'g' or RIGHT to show the next MultiGen.
 * Press 'G' or LEFT to show the previous MultiGen.
 * Press 'a' to toggle animation along the signal path.
 * Press 'A' to step animation backward.
 * Press 'n' to hide or show numbers.
 * Press 'l' to hide or show path lines.
 * Press 'o' to hide or show outlines around the component gens.
 * Press 't' to hide or show the title text.
 * Press 'd' to print a description of the current MultiGen.
 * Press 'h' to show this help text.
 * </pre>
 */
public class MultiGenZoo extends PApplet {
    PixelAudio pixelaudio;
    PixelAudioMapper mapper;
    PixelMapGen gen;
    PGraphics offscreen;
    ArrayList<int[]> coords;
    int[] imageLUT;
    int[] signalLUT;
    int[] spectrum;

    int imageWidth = 1536;
    int imageHeight = 1024;
    int genW = 4;
    int genH = genW;
    int drawingScale = 2;
    int offset = 0;
    int bigTextSize = 64;
    int smallTextSize = 32;
    int componentWidth = genW;
    int componentHeight = genH;
    int patternIndex = 0;
    boolean isAnimating = false;
    boolean isHideNumbers = true;
    boolean isHideLines = false;
    boolean isHideOutlines = false;
    boolean isHideTitle = false;
    Random rand = new Random();

    String[] patternNames = {
        "Hilbert loop 3x2",
        "Hilbert loop 6x4",
        "Hilbert + Zigzag loop 6x4",
        "Hilbert stack ortho",
        "Hilbert stack bou",
        "Hilbert rows ortho",
        "Hilbert columns ortho",
        "Zigzag loop 6x4",
        "Zigzag rows ortho",
        "Zigzag rows alt ortho",
        "Zigzag columns ortho",
        "Zigzag columns alt ortho",
        "Random zigzag 6x4",
        "Random zigzag 12x8",
        "Random boustrophe 6x4",
        "Random Hilbert 6x4",
        "Random Hilbert 12x8",
        "Random Hilbert 24x16"
    };

    public static AffineTransformType r270 = AffineTransformType.R270;
    public static AffineTransformType r90 = AffineTransformType.R90;
    public static AffineTransformType r180 = AffineTransformType.R180;
    public static AffineTransformType flipx = AffineTransformType.FLIPX;
    public static AffineTransformType fx270 = AffineTransformType.FX270;
    public static AffineTransformType fx90 = AffineTransformType.FX90;
    public static AffineTransformType flipy = AffineTransformType.FLIPY;
    public static AffineTransformType nada = AffineTransformType.NADA;

    public static void main(String[] args) {
        PApplet.main(new String[] { MultiGenZoo.class.getName() });
    }

    public void settings() {
        size(imageWidth, imageHeight, JAVA2D);
    }

    public void setup() {
        windowResizable(true);
        pixelaudio = new PixelAudio(this);
        offscreen = createGraphics(width, height);
        loadPattern(0);
        showHelp();
    }

    public void draw() {
        if (isAnimating) {
            PixelAudioMapper.rotateLeft(spectrum, max(1, mapper.getSize() / 512));
        }
        drawPattern();
        image(offscreen, 0, 0, width, height);
        drawTitle();
    }

    public void keyPressed() {
        if (key == 'g' || keyCode == RIGHT) {
            loadPattern(patternIndex + 1);
        } else if (key == 'G' || keyCode == LEFT) {
            loadPattern(patternIndex - 1);
        } else if (key == 'a') {
            isAnimating = !isAnimating;
        } else if (key == 'A') {
            stepAnimation(-1);
        } else if (key == 'n') {
            isHideNumbers = !isHideNumbers;
        } else if (key == 'l') {
            isHideLines = !isHideLines;
        } else if (key == 'o') {
            isHideOutlines = !isHideOutlines;
        } else if (key == 't') {
            isHideTitle = !isHideTitle;
        } else if (key == 'd') {
            println("\n" + mapper.getGeneratorDescription());
            println("Dimensions: " + mapper.getWidth() + " x " + mapper.getHeight());
            println("Size: " + mapper.getSize() + " pixels");
        } else if (key == 'h') {
            showHelp();
        }
    }

    public void loadPattern(int nextIndex) {
        patternIndex = floorMod(nextIndex, patternNames.length);
        setComponentSize(patternIndex);
        gen = createPattern(patternIndex);
        mapper = new PixelAudioMapper(gen);
        coords = gen.getCoordinatesCopy();
        imageLUT = mapper.getImageToSignalLUT();
        signalLUT = mapper.getSignalToImageLUT();
        spectrum = getColors(mapper.getSize());
        setDrawingVars();
        int newWidth = mapper.getWidth() * drawingScale;
        int newHeight = mapper.getHeight() * drawingScale;
        if (newWidth != width || newHeight != height) {
            windowResize(newWidth, newHeight);
        }
        offscreen = createGraphics(newWidth, newHeight);
        println("\n" + (patternIndex + 1) + ". " + patternNames[patternIndex]);
    }

    public PixelMapGen createPattern(int selector) {
        switch (selector) {
            case 0:
                return hilbertLoop3x2(genW, genH);
            case 1:
                return hilbertLoop6x4(genW);
            case 2:
                return hilbertZigzagLoop6x4(genW, genH);
            case 3:
                return hilbertStackOrtho(3, 8, 4, genW, genH);
            case 4:
                return hilbertStackBou(3, 8, 4, genW, genH);
            case 5:
                return hilbertRowOrtho(6, 4, genW, genH);
            case 6:
                return hilbertColumnOrtho(6, 4, genW, genH);
            case 7:
                return zigzagLoop6x4(genW, genH);
            case 8:
                return zigzagRowOrtho(6, 4, genW, genH);
            case 9:
                return zigzagRowAltOrtho(6, 4, genW, genH);
            case 10:
                return zigzagColumnOrtho(6, 4, genW, genH);
            case 11:
                return zigzagColumnAltOrtho(6, 4, genW, genH);
            case 12:
                return zigzagRowRandomFlip(6, 4, genW, genH);
            case 13:
                return zigzagRowRandomFlip(12, 8, genW, genH);
            case 14:
                return boustrophRowRandom(6, 4, genW, genH);
            case 15:
                return hilbertRowRandomFlip(6, 4, genW, genH);
            case 16:
                return hilbertRowRandomFlip(12, 8, genW, genH);
            case 17:
                return hilbertRowRandomFlip(24, 16, genW, genH);
            default:
                return hilbertLoop3x2(genW, genH);
        }
    }

    public void setComponentSize(int selector) {
        componentWidth = genW;
        componentHeight = genH;
    }

    public void setDrawingVars() {
        drawingScale = max(1, min(imageWidth / mapper.getWidth(), imageHeight / mapper.getHeight()));
        offset = drawingScale / 2;
        bigTextSize = max(12, drawingScale);
        smallTextSize = max(8, drawingScale / 2);
        isHideLines = genW > 128;
    }


    /**
     * hilbertLoop3x2() returns a looping fractal signal path consisting of
     * 6 Hilbert gens, 3 wide by 2 tall, to fit a 3 * genW by 2 * genH image.
     * This method is available as a static method of HilbertGen.
     *
     * Note that genH must equal genW and both must be powers of 2.
     *
     * @param genW    width of each HilbertGen
     * @param genH    height of each HilbertGen
     * @return        a MultiGen consisting of 6 HilbertGens linked together by one signal path
     */
    public MultiGen hilbertLoop3x2(int genW, int genH) {
        ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
        ArrayList<int[]> offsetList = new ArrayList<int[]>();
        genList.add(new HilbertGen(genW, genH, fx270));
        offsetList.add(new int[] {0, 0});
        genList.add(new HilbertGen(genW, genH, nada));
        offsetList.add(new int[] {genW, 0});
        genList.add(new HilbertGen(genW, genH, fx90));
        offsetList.add(new int[] {2 * genW, 0});
        genList.add(new HilbertGen(genW, genH, fx90));
        offsetList.add(new int[] {2 * genW, genH});
        genList.add(new HilbertGen(genW, genH, r180));
        offsetList.add(new int[] {genW, genH});
        genList.add(new HilbertGen(genW, genH, fx270));
        offsetList.add(new int[] {0, genH});
        return new MultiGen(3 * genW, 2 * genH, offsetList, genList);
    }

    /**
     * Generates a MultiGen with 6 * 4 HilbertGen components and a continuous signal path.
     * @param genW   width of a single PixelMapGen
     * @return a MultiGen with 6 * 4 HilbertGen components
     */
    public MultiGen hilbertLoop6x4(int genW) {
        return HilbertGen.hilbertMultigenLoop(6, 4, genW);
    }

    /**
     * This method creates a MultiGen consisting of a mix of zigzag and Hilbert curves
     * in 6 columns and 4 rows arranged to provide a continuous loop.
     *
     * @param genW   width of a single PixelMapGen composing the MultiGen
     * @param genH   height of a single PixelMapGen composing the MultiGen
     * @return a MultiGen with a signal path composed of HilbertGens and DiagonalZigzagGens.
     */
    public MultiGen hilbertZigzagLoop6x4(int genW, int genH) {
        ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
        ArrayList<int[]> offsetList = new ArrayList<int[]>();
        int[][] locs = {{0, 0}, {0, 1}, {0, 2}, {0, 3}, {1, 3}, {1, 2}, {2, 2}, {2, 3},
                {3, 3}, {3, 2}, {4, 2}, {4, 3}, {5, 3}, {5, 2}, {5, 1}, {5, 0},
                {4, 0}, {4, 1}, {3, 1}, {3, 0}, {2, 0}, {2, 1}, {1, 1}, {1, 0}};
        AffineTransformType[] trans = {r270, r270, nada, r270, r90, fx270, nada, r270,
                r90, r90, fx90, nada, r90, r90, r180, r90,
                r270, fx90, r180, r90, r270, r270, fx270, r180};
        char[] cues = {'H', 'D', 'D', 'H', 'D', 'H', 'D', 'H',
                'H', 'D', 'H', 'D', 'H', 'D', 'D', 'H',
                'D', 'H', 'D', 'H', 'H', 'D', 'H', 'D'};
        for (int i = 0; i < trans.length; i++) {
            offsetList.add(new int[] {locs[i][0] * genW, locs[i][1] * genH});
            if (cues[i] == 'H') {
                genList.add(new HilbertGen(genW, genH, trans[i]));
            } else {
                genList.add(new DiagonalZigzagGen(genW, genH, trans[i]));
            }
        }
        return new MultiGen(6 * genW, 4 * genH, offsetList, genList);
    }

    /**
     * This method creates a vertical stacks of rows of HilbertGens. Each row
     * begins genH pixels down from the previous row, back at the beginning
     * of the previous row (i.e., in "row major" order, like a bitmap). This
     * method pairs nicely with an image with 3 columns of with 8 rows of words,
     * using the image as a control surface for sampling an audio file with
     * words recorded at the appropriate locations to match the screen order.
     * The signal path jumps from the end of the last gen in each row to the
     * beginning of the first gen in the next row. The path in each row is
     * continuous, which provides some interesting optical effects.
     *
     * @param stacks    the number of stacks
     * @param rows      the number of rows in each stack
     * @param units     the number of gens in each row
     * @param genW      the width of each gen, a power of 2
     * @param genH      the height of each gen, equal to genW
     * @return          a MultiGen consisting of stacks * rows * units PixelMapGens
     */
    public MultiGen hilbertStackOrtho(int stacks, int rows, int units, int genW, int genH) {
        ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
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
        return new MultiGen(stacks * units * genW, rows * genH, offsetList, genList);
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
     * @return          a MultiGen consisting of stacks * rows * units PixelMapGens
     */
    public MultiGen hilbertStackBou(int stacks, int rows, int units, int genW, int genH) {
        ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
        ArrayList<int[]> offsetList = new ArrayList<int[]>();
        for (int s = 0; s < stacks; s++) {
            for (int r = 0; r < rows; r++) {
                int shift = s * units;
                if (r % 2 == 1) {
                    for (int u = 0; u < units; u++) {
                        genList.add(new HilbertGen(genW, genH, flipx));
                        offsetList.add(new int[] {(u + shift) * genW, r * genH});
                    }
                } else {
                    for (int u = units; u > 0; u--) {
                        genList.add(new HilbertGen(genW, genH, flipy));
                        offsetList.add(new int[] {(u + shift - 1) * genW, r * genH});
                    }
                }
            }
        }
        return new MultiGen(stacks * units * genW, rows * genH, offsetList, genList);
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
        ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
        ArrayList<int[]> offsetList = new ArrayList<int[]>();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                genList.add(new HilbertGen(genW, genH, y % 2 == 0 ? nada : flipy));
                offsetList.add(new int[] {x * genW, y * genH});
            }
        }
        return new MultiGen(cols * genW, rows * genH, offsetList, genList);
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
        ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
        ArrayList<int[]> offsetList = new ArrayList<int[]>();
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                genList.add(new HilbertGen(genW, genH, x % 2 == 0 ? r270 : r90));
                offsetList.add(new int[] {x * genW, y * genH});
            }
        }
        return new MultiGen(cols * genW, rows * genH, offsetList, genList);
    }

    /**
     * @param genW    width of each zigzag gen
     * @param genH    height of each zigzag gen
     * @return        a looping MultiGen with 6 rows x 4 columns of DiagonalZigzagGen instances
     */
    public MultiGen zigzagLoop6x4(int genW, int genH) {
        ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
        ArrayList<int[]> offsetList = new ArrayList<int[]>();
        int[][] locs = {{0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0},
                {5, 1}, {5, 2}, {5, 3}, {4, 3}, {4, 2}, {4, 1},
                {3, 1}, {3, 2}, {3, 3}, {2, 3}, {2, 2}, {2, 1},
                {1, 1}, {1, 2}, {1, 3}, {0, 3}, {0, 2}, {0, 1}};
        AffineTransformType[] trans = {r90, fx90, r90, fx90, r90, fx90,
                r270, fx90, r270, fx270, r90, fx270,
                r270, fx90, r270, fx270, r90, fx270,
                r270, fx90, r270, fx270, r90, fx270};
        for (int i = 0; i < trans.length; i++) {
            offsetList.add(new int[] {locs[i][0] * genW, locs[i][1] * genH});
            genList.add(new DiagonalZigzagGen(genW, genH, trans[i]));
        }
        return new MultiGen(6 * genW, 4 * genH, offsetList, genList);
    }

    /**
     * Creates a MultiGen with rows * cols DiagonalZigzagGens.
     * Note that you should set values for such that:
     * (rows * genW) == width and (cols * genH) == height.
     * "Ortho" implies that each row starts at the left edge.
     * The orientation of the the diagonals changes for alternate rows.
     *
     * @param cols    number of vertical columns
     * @param rows    number of horizontal rows
     * @param genW    width of an individual PixelMapGen
     * @param genH    height of an individual PixelMapGen
     * @return        a MultiGen created from rows * cols PixelMapGens
     */
    public MultiGen zigzagRowOrtho(int cols, int rows, int genW, int genH) {
        ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
        ArrayList<int[]> offsetList = new ArrayList<int[]>();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                genList.add(new DiagonalZigzagGen(genW, genH, y % 2 == 0 ? flipy : nada));
                offsetList.add(new int[] {x * genW, y * genH});
            }
        }
        return new MultiGen(cols * genW, rows * genH, offsetList, genList);
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
     * @param rows    number of horizontal rows
     * @param genW    width of an individual PixelMapGen
     * @param genH    height of an individual PixelMapGen
     * @return        a MultiGen created from rows * cols PixelMapGens
     */
    public MultiGen zigzagRowAltOrtho(int cols, int rows, int genW, int genH) {
        ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
        ArrayList<int[]> offsetList = new ArrayList<int[]>();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                boolean flip = (x + y) % 2 == 0;
                genList.add(new DiagonalZigzagGen(genW, genH, flip ? flipy : nada));
                offsetList.add(new int[] {x * genW, y * genH});
            }
        }
        return new MultiGen(cols * genW, rows * genH, offsetList, genList);
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
     * @param rows    number of horizontal rows
     * @param genW    width of an individual PixelMapGen
     * @param genH    height of an individual PixelMapGen
     * @return        a MultiGen created from rows * cols PixelMapGens
     */
    public MultiGen zigzagColumnOrtho(int cols, int rows, int genW, int genH) {
        ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
        ArrayList<int[]> offsetList = new ArrayList<int[]>();
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                genList.add(new DiagonalZigzagGen(genW, genH, x % 2 == 0 ? fx270 : r90));
                offsetList.add(new int[] {x * genW, y * genH});
            }
        }
        return new MultiGen(cols * genW, rows * genH, offsetList, genList);
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
     * @param rows    number of horizontal rows high
     * @param genW    width of an individual PixelMapGen
     * @param genH    height of an individual PixelMapGen
     * @return        a MultiGen created from rows * cols PixelMapGens
     */
    public MultiGen zigzagColumnAltOrtho(int cols, int rows, int genW, int genH) {
        ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
        ArrayList<int[]> offsetList = new ArrayList<int[]>();
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                boolean useFx270 = x % 2 == y % 2;
                genList.add(new DiagonalZigzagGen(genW, genH, useFx270 ? fx270 : r90));
                offsetList.add(new int[] {x * genW, y * genH});
            }
        }
        return new MultiGen(cols * genW, rows * genH, offsetList, genList);
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
     * @param rows    number of horizontal rows
     * @param genW    width of an individual PixelMapGen
     * @param genH    height of an individual PixelMapGen
     * @return        a MultiGen created from rows * cols PixelMapGens
     */
    public MultiGen zigzagRowRandomFlip(int cols, int rows, int genW, int genH) {
        ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
        ArrayList<int[]> offsetList = new ArrayList<int[]>();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                genList.add(new DiagonalZigzagGen(genW, genH, PixelMapGen.randomTransform(rand)));
                offsetList.add(new int[] {x * genW, y * genH});
            }
        }
        return new MultiGen(cols * genW, rows * genH, offsetList, genList);
    }

    /**
     * Creates a MultiGen with rows * cols BoustropheGens.
     * Note that you should set values for such that:
     * (rows * genW) == width and (cols * genH) == height.
     *
     * @param cols    number of vertical columns wide
     * @param rows    number of horizontal rows high
     * @param genW    width of an individual PixelMapGen
     * @param genH    height of an individual PixelMapGen
     * @return        a MultiGen created from rows * cols PixelMapGens
     */
    public MultiGen boustrophRowRandom(int cols, int rows, int genW, int genH) {
        ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
        ArrayList<int[]> offsetList = new ArrayList<int[]>();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                genList.add(new BoustropheGen(genW, genH, PixelMapGen.randomTransform(rand)));
                offsetList.add(new int[] {x * genW, y * genH});
            }
        }
        return new MultiGen(cols * genW, rows * genH, offsetList, genList);
    }

    /**
     * Creates a MultiGen with rows * cols HilbertGens.
     * Note that you should set values for such that:
     * (rows * genW) == width and (cols * genH) == height
     * and that genH == genW and both are powers of 2.
     * The orientation of the HilbertGens is randomized.
     *
     * @param cols    number of vertical columns wide
     * @param rows    number of horizontal rows high
     * @param genW    width of an individual PixelMapGen
     * @param genH    height of an individual PixelMapGen
     * @return        a MultiGen created from rows * cols PixelMapGens
     */
    public MultiGen hilbertRowRandomFlip(int cols, int rows, int genW, int genH) {
        ArrayList<PixelMapGen> genList = new ArrayList<PixelMapGen>();
        ArrayList<int[]> offsetList = new ArrayList<int[]>();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                genList.add(new HilbertGen(genW, genH, PixelMapGen.randomTransform(rand)));
                offsetList.add(new int[] {x * genW, y * genH});
            }
        }
        return new MultiGen(cols * genW, rows * genH, offsetList, genList);
    }

    public void drawPattern() {
        offscreen.beginDraw();
        offscreen.background(255);
        offscreen.endDraw();
        drawSquares();
        if (!isHideOutlines) {
            drawComponentOutlines();
        }
        if (!isHideLines) {
            drawLines();
        }
        if (!isHideNumbers) {
            drawNumbers();
        }
    }

    public void drawSquares() {
        int pos = 0;
        offscreen.beginDraw();
        offscreen.pushStyle();
        offscreen.noStroke();
        for (int[] coordinate : coords) {
            offscreen.fill(spectrum[pos]);
            offscreen.square(coordinate[0] * drawingScale, coordinate[1] * drawingScale, drawingScale);
            pos++;
        }
        offscreen.popStyle();
        offscreen.endDraw();
    }

    public void drawLines() {
        int x1 = 0;
        int y1 = 0;
        int x2 = 0;
        int y2 = 0;
        int pos = 0;
        offscreen.beginDraw();
        offscreen.pushStyle();
        offscreen.strokeWeight(2.5f);
        offscreen.stroke(255, 160);
        for (int[] coordinate : coords) {
            if (pos == 0) {
                x1 = coordinate[0] * drawingScale + offset;
                y1 = coordinate[1] * drawingScale + offset;
            } else {
                x2 = coordinate[0] * drawingScale + offset;
                y2 = coordinate[1] * drawingScale + offset;
                offscreen.line(x1, y1, x2, y2);
                x1 = x2;
                y1 = y2;
            }
            pos++;
        }
        offscreen.popStyle();
        offscreen.endDraw();
    }

    public void drawNumbers() {
        int x1 = 0;
        int y1 = 0;
        int pos = 0;
        int drop = bigTextSize / 4;
        offscreen.beginDraw();
        offscreen.pushStyle();
        for (int[] coordinate : coords) {
            x1 = coordinate[0] * drawingScale + offset;
            y1 = coordinate[1] * drawingScale + offset + drop;
            offscreen.textAlign(CENTER);
            offscreen.textSize(bigTextSize * 0.5f);
            offscreen.fill(0, 192);
            offscreen.text(signalLUT[pos], x1, y1);
            offscreen.textAlign(LEFT);
            offscreen.textSize(smallTextSize * 0.75f);
            offscreen.fill(255, 192);
            offscreen.text(pos, x1 - offset + smallTextSize / 2, y1 - offset + smallTextSize / 2);
            pos++;
        }
        offscreen.popStyle();
        offscreen.endDraw();
    }

    public void drawComponentOutlines() {
        int cellW = componentWidth * drawingScale;
        int cellH = componentHeight * drawingScale;
        offscreen.beginDraw();
        offscreen.pushStyle();
        offscreen.noFill();
        offscreen.stroke(0, 170);
        offscreen.strokeWeight(2);
        for (int y = 0; y < offscreen.height; y += cellH) {
            for (int x = 0; x < offscreen.width; x += cellW) {
                offscreen.rect(x, y, cellW, cellH);
            }
        }
        offscreen.stroke(255, 120);
        offscreen.strokeWeight(1);
        for (int y = 0; y < offscreen.height; y += cellH) {
            for (int x = 0; x < offscreen.width; x += cellW) {
                offscreen.rect(x + 1, y + 1, cellW - 2, cellH - 2);
            }
        }
        offscreen.popStyle();
        offscreen.endDraw();
    }

    public void drawTitle() {
        if (isHideTitle) {
            return;
        }
        pushStyle();
        fill(0, 180);
        noStroke();
        rect(0, 0, width, 34);
        fill(255);
        textAlign(LEFT, CENTER);
        textSize(16);
        text((patternIndex + 1) + "/" + patternNames.length + "  " + patternNames[patternIndex], 12, 17);
        popStyle();
    }

    public int[] getColors(int size) {
        int[] colorWheel = new int[size];
        pushStyle();
        colorMode(HSB, colorWheel.length, 100, 100);
        for (int i = 0; i < colorWheel.length; i++) {
            colorWheel[i] = color(i, 66, 66);
        }
        popStyle();
        return colorWheel;
    }

    public void stepAnimation(int step) {
        PixelAudioMapper.rotateLeft(spectrum, step);
    }

    public int floorMod(int x, int y) {
        int mod = x % y;
        return mod < 0 ? mod + y : mod;
    }

    public void showHelp() {
        println("\n----- MultiGenZoo Help -----\n");
        println(" * Press 'g' or RIGHT to show the next MultiGen.");
        println(" * Press 'G' or LEFT to show the previous MultiGen.");
        println(" * Press 'a' to toggle animation along the signal path.");
        println(" * Press 'A' to step animation backward.");
        println(" * Press 'n' to hide or show numbers.");
        println(" * Press 'l' to hide or show path lines.");
        println(" * Press 'o' to hide or show outlines around the component gens.");
        println(" * Press 't' to hide or show the title text.");
        println(" * Press 'd' to print a description of the current MultiGen.");
        println(" * Press 'h' to show this help text.");
    }
}
