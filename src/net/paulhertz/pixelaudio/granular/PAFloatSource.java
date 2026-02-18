package net.paulhertz.pixelaudio.granular;

/**
 * PAFloatSource
 *
 * Minimal, library-agnostic "audio source" abstraction for PixelAudio voices.
 * It deals only with float[] buffers and sample indices, no Minim types.
 *
 * Implementations are expected to:
 *  - be called on the audio thread,
 *  - be allocation-free inside renderBlock().
 */
public interface PAFloatSource {

    /**
     * Render audio into the given block buffers.
     *
     * Implementations should:
     *  - Assume outL/outR length >= blockSize.
     *  - Mix into outL/outR (add), not clear them.
     *  - Avoid allocation on the audio thread.
     *
     * @param blockStart the absolute sample index in the source’s own sample domain (e.g., buffer index space).
     * @param blockSize  number of samples in this block.
     * @param outL       left channel buffer to mix into.
     * @param outR       right channel buffer to mix into (may be same as outL for mono).
     */
    void renderBlock(long blockStart,
                     int blockSize,
                     float[] outL,
                     float[] outR);

    /**
     * Duration in samples, or Long.MAX_VALUE if effectively infinite/streaming.
     */
    long lengthSamples();
}
