package net.paulhertz.pixelaudio.granular;

import ddf.minim.MultiChannelBuffer;
import ddf.minim.analysis.WindowFunction;
import net.paulhertz.pixelaudio.voices.PitchPolicy;

/**
 * MCBufferSource
 *
 * A simple PASource that wraps a Minim MultiChannelBuffer for linear playback.
 * This is a natural building block for PASamplerInstrument when you want
 * to work directly with buffer-backed sample sources.
 *
 * NOTE:
 *  - This class does not manage playback position on its own; typically a
 *    PASamplerVoice will determine which segment to read and call
 *    renderBlock(...) accordingly.
 *  - For mono buffers, MultiChannelBuffer should have 1 channel.
 *  - For stereo or multi-channel, the mixing logic belongs in renderBlock().
 */
public class MCBufferSource implements PASource {

    private final MultiChannelBuffer buffer;
    private final long lengthSamples;
    private WindowFunction grainWindow = null;
    private int grainLenSamples = 1024;

    public MCBufferSource(MultiChannelBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }
        this.buffer = buffer;
        this.lengthSamples = buffer.getBufferSize();
    }

    @Override
    public void renderBlock(long blockStart,
                            int blockSize,
                            float[] outL,
                            float[] outR) {
        // TODO (implementation when ready):
        //
        // - Interpret blockStart as the sample index (in buffer space)
        //   that this source should read from.
        // - If used inside PASamplerVoice, blockStart is typically computed
        //   from the voice's playback position, pitch, and envelope state.
        //
        // Pseudocode:
        //
        // float[] left  = buffer.getChannel(0);
        // float[] right = (buffer.getChannelCount() > 1) ? buffer.getChannel(1) : left;
        //
        // for (int i = 0; i < blockSize; i++) {
        //     int idx = (int) (blockStart + i);
        //     if (idx < 0 || idx >= lengthSamples) break;
        //
        //     float l = left[idx];
        //     float r = right[idx];
        //
        //     outL[i] += l;
        //     if (outR != outL) {
        //         outR[i] += r;
        //     }
        // }
    }

    @Override
    public long lengthSamples() {
        return lengthSamples;
    }

    @Override
    public MultiChannelBuffer getMultiChannelBuffer() {
        return buffer;
    }

    @Override
    public PitchPolicy pitchPolicy() {
        // Classic sample playback: instrument-level pitch applies.
        return PitchPolicy.INSTRUMENT_RATE;
    }
    
    @Override
    public void setGrainWindow(WindowFunction wf, int grainLenSamples) {
        this.grainWindow = wf;
        this.grainLenSamples = Math.max(1, grainLenSamples);

        // Prewarm here if this is called from UI/scheduler thread (safe).
        if (wf != null) {
            WindowCache.INSTANCE.prewarm(wf, this.grainLenSamples);
        }
    }

}
