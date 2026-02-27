package net.paulhertz.pixelaudio.granular;

import java.util.Objects;

import ddf.minim.MultiChannelBuffer;
import ddf.minim.analysis.WindowFunction;
import net.paulhertz.pixelaudio.sampler.PitchPolicy;

public final class PABufferBackedSource implements PASource {

    private final String name;
    private final float sampleRate;
    private final MultiChannelBuffer mcb;
    private final PitchPolicy pitchPolicy;
    private WindowFunction grainWindow = null;
    private int grainLenSamples = 1024;

    /**
     * Base offset applied to blockStart in {@link #renderBlock}.
     *
     * Semantics: after {@link #seekTo(long)} with value T, a call to
     * renderBlock(T, ...) begins reading from buffer index 0.
     */
    private volatile long baseOffset = 0L;

    // ---- Factories ----

    /** Copies {@code mono} into an internal 1-channel MultiChannelBuffer. */
    public static PABufferBackedSource fromMono(String name, float[] mono, float sampleRate) {
        Objects.requireNonNull(mono, "mono");
        MultiChannelBuffer mcb = new MultiChannelBuffer(mono.length, 1);
        System.arraycopy(mono, 0, mcb.getChannel(0), 0, mono.length);
        return new PABufferBackedSource(name, sampleRate, mcb, PitchPolicy.INSTRUMENT_RATE);
    }

    /** Copies {@code left/right} into an internal 2-channel MultiChannelBuffer. */
    public static PABufferBackedSource fromStereo(String name, float[] left, float[] right, float sampleRate) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length != right.length) {
            throw new IllegalArgumentException("left/right must have same length");
        }
        MultiChannelBuffer mcb = new MultiChannelBuffer(left.length, 2);
        System.arraycopy(left, 0, mcb.getChannel(0), 0, left.length);
        System.arraycopy(right, 0, mcb.getChannel(1), 0, right.length);
        return new PABufferBackedSource(name, sampleRate, mcb, PitchPolicy.INSTRUMENT_RATE);
    }

    /**
     * Copies per-channel arrays into an internal MultiChannelBuffer.
     * All channels must have the same length (frames).
     */
    public static PABufferBackedSource fromChannels(String name, float sampleRate, float[]... channels) {
        Objects.requireNonNull(channels, "channels");
        if (channels.length == 0) throw new IllegalArgumentException("channels must be non-empty");
        int frames = channels[0].length;
        for (int c = 1; c < channels.length; c++) {
            if (channels[c].length != frames) {
                throw new IllegalArgumentException("all channels must have same length");
            }
        }
        MultiChannelBuffer mcb = new MultiChannelBuffer(channels.length, frames);
        for (int c = 0; c < channels.length; c++) {
            System.arraycopy(channels[c], 0, mcb.getChannel(c), 0, frames);
        }
        return new PABufferBackedSource(name, sampleRate, mcb, PitchPolicy.INSTRUMENT_RATE);
    }

    // ---- Ctor ----

    public PABufferBackedSource(String name, float sampleRate, MultiChannelBuffer mcb, PitchPolicy pitchPolicy) {
        this.name = (name != null) ? name : "buffer";
        this.sampleRate = sampleRate;
        this.mcb = Objects.requireNonNull(mcb, "mcb");
        this.pitchPolicy = (pitchPolicy != null) ? pitchPolicy : PitchPolicy.INSTRUMENT_RATE;
    }

    // ---- PASource ----

    public String name() { return name; }

    /** Convenience (not required by PASource). */
    public float getSampleRate() { return sampleRate; }

    @Override
    public PitchPolicy pitchPolicy() {
        return pitchPolicy;
    }

    @Override
    public void seekTo(long absoluteSample) {
        // Base-offset model (see field docs).
        this.baseOffset = Math.max(0L, absoluteSample);
    }

    @Override
    public MultiChannelBuffer getMultiChannelBuffer() {
        return mcb;
    }

    // ---- PAFloatSource ----

    @Override
    public long lengthSamples() {
        // MultiChannelBuffer stores frames-per-channel.
        return mcb.getBufferSize();
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


    @Override
    public void renderBlock(long blockStart, int blockSize, float[] outL, float[] outR) {
    	if (blockSize <= 0) return;

    	final int frames = mcb.getBufferSize();
    	final int chCount = mcb.getChannelCount();
    	if (chCount <= 0) return;

    	// blockStart is interpreted in *source* sample space.
    	long start = blockStart - baseOffset;

    	// Clamp negative starts to 0.
    	if (start < 0L) {
    		start = 0L;
    	}

    	// If we're already beyond the buffer, nothing to add.
    	if (start >= frames) return;

    	final int iStart = (int) start;
    	final int n = Math.min(blockSize, frames - iStart);

    	// Optional grain windowing (taper) — this is NOT per-grain overlap-add,
    	// but it correctly applies a WindowFunction in voice time.
    	final WindowFunction wf = this.grainWindow;          // assume you added these fields
    	final int wLen = this.grainLenSamples;               // >= 1 when enabled
    	final float[] wCurve;
    	final int phase0;

    	if (wf != null && wLen > 1) {
    		wCurve = WindowCache.INSTANCE.getWindowCurve(wf, wLen);
    		// Phase in VOICE time (blockStart is the voice's absolute sample cursor).
    		// Ensure non-negative modulo.
    		long m = blockStart % (long) wLen;
    		if (m < 0) m += wLen;
    		phase0 = (int) m;
    	} else {
    		wCurve = null;
    		phase0 = 0;
    	}

    	if (chCount == 1) {
    		// Mono: copy to L and R, but avoid double-add if outL == outR.
    		final boolean same = (outL == outR);
    		final float[] s0 = mcb.getChannel(0);

    		if (wCurve == null) {
    			for (int i = 0; i < n; i++) {
    				float v = s0[iStart + i];
    				outL[i] += v;
    				if (!same) outR[i] += v;
    			}
    		} else {
    			for (int i = 0; i < n; i++) {
    				int wi = phase0 + i;
    				if (wi >= wLen) wi -= (wi / wLen) * wLen; // fast-ish wrap without %
    				float v = s0[iStart + i] * wCurve[wi];
    				outL[i] += v;
    				if (!same) outR[i] += v;
    			}
    		}

    	} 
    	else {
    		// Policy: first two channels are treated as L/R; extra channels are ignored.
    		final float[] sL = mcb.getChannel(0);
    		final float[] sR = mcb.getChannel(1);

    		if (wCurve == null) {
    			for (int i = 0; i < n; i++) {
    				outL[i] += sL[iStart + i];
    				outR[i] += sR[iStart + i];
    			}
    		} else {
    			for (int i = 0; i < n; i++) {
    				int wi = phase0 + i;
    				if (wi >= wLen) wi -= (wi / wLen) * wLen;
    				float w = wCurve[wi];
    				outL[i] += sL[iStart + i] * w;
    				outR[i] += sR[iStart + i] * w;
    			}
    		}
    	}
    }

    @Override
    public String toString() {
        return "PABufferBackedSource{name=" + name +
               ", frames=" + mcb.getBufferSize() +
               ", chans=" + mcb.getChannelCount() + "}";
    }
}
