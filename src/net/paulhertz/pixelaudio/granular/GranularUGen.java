package net.paulhertz.pixelaudio.granular;

import ddf.minim.UGen;
import java.util.Arrays;

// ...

/**
 * GranularUGen
 *
 * A Minim UGen that uses BasicIndexGranularSource to generate audio
 * in blocks, but feeds it sample-by-sample via uGenerate().
 */
public class GranularUGen extends UGen {

    private final BasicIndexGranularSource src;
    private final float[] blockL;
    private final float[] blockR;
    private final int blockSize;

    private long absSample = 0;
    private int cursor = 0; // position inside current block

    public GranularUGen(BasicIndexGranularSource src, int blockSize) {
        this.src = src;
        this.blockSize = blockSize;
        this.blockL = new float[blockSize];
        this.blockR = new float[blockSize];
    }

    private void refillBlock() {
        // Clear the block
        Arrays.fill(blockL, 0f);
        Arrays.fill(blockR, 0f);

        // Ask the granular source to render the next block
        src.renderBlock(absSample, blockSize, blockL, blockR);
        absSample += blockSize;
        cursor = 0;

        // Debug if needed:
        // System.out.println("Refilled block, blockL[0]=" + blockL[0]);
    }
    
    public void reset() {
    	this.absSample = 0;
    	this.cursor = 0;
    }

    @Override
    protected void uGenerate(float[] channels) {
        // channels.length is the number of output channels (1 = mono, 2 = stereo, etc.)
        if (cursor >= blockSize) {
            refillBlock();
        }

        float l = blockL[cursor];
        float r = blockR[cursor];
        cursor++;

        // Write into channels
        channels[0] = l;              // left
        if (channels.length > 1) {
            channels[1] = r;          // right
        }
        // If more channels existed, you could fan out here.
    }
}
