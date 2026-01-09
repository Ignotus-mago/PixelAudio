package net.paulhertz.pixelaudio.granular;

import ddf.minim.analysis.WindowFunction;
import net.paulhertz.pixelaudio.voices.PASource;
import net.paulhertz.pixelaudio.voices.PitchPolicy;

/**
 * IndexGranularSource
 *
 * "Standard" linear granular source that:
 *  - reads from a mono float[] source buffer,
 *  - starts at a given source sample index,
 *  - uses a fixed grain length and hop in samples,
 *  - plays either:
 *      * a fixed number of grains, or
 *      * as many grains as needed to fill a target duration,
 *  - applies a window and equal-power pan.
 *
 * This is the classic time-domain granulator:
 *   sourceIndex(g, k) = startSampleIndex + g * indexHopSamples + k
 *   time(g)           = noteStartSample + g * timeHopSamples
 *
 * For now, indexHopSamples == timeHopSamples gives 1x pitch.
 * Later you can decouple them for time-stretch/pitch-shift.
 * 
 * TODO remove pan from IndexGranularSource entirely and make it mono.
 */
public class IndexGranularSource implements PASource {

    private final float[] source;
    private final GranularSettings settings;
    private final WindowCache windowCache = WindowCache.INSTANCE;

    // Core granular params
    private final int startSampleIndex;
    private final int grainLength;
    private final int timeHopSamples;
    private final int indexHopSamples;
    private final int numGrains;
    private final float pitchRatio;    // new

    private final WindowFunction windowFunction;

    // Derived length
    private final long totalLengthSamples;

    // Note-level state
    private long noteStartSample = Long.MIN_VALUE;
    private boolean noteStarted = false;

    
    /**
     * Construct by explicit grain count.
     *
     * @param source           mono source buffer
     * @param settings         granular settings (window, default grain length, hop, etc.)
     * @param startSampleIndex first source sample for grain 0
     * @param grainLength      grain length in samples (or <=0 → settings.defaultGrainLength)
     * @param timeHopSamples   hop in output time (samples between grain starts)
     * @param indexHopSamples  hop in source index (samples between grain starts in source)
     * @param numGrains        number of grains to play (>= 1)
     * @param pitchRatio       proportional change in pitch
     */
    public IndexGranularSource(float[] source,
    		GranularSettings settings,
    		int startSampleIndex,
    		int grainLength,
    		int timeHopSamples,
    		int indexHopSamples,
    		int numGrains,
    		float pitchRatio) {
    	if (source == null) throw new IllegalArgumentException("source must not be null");
    	if (settings == null) throw new IllegalArgumentException("settings must not be null");
    	if (numGrains <= 0) throw new IllegalArgumentException("numGrains must be >= 1");

    	this.source = source;
    	this.settings = settings;
    	this.startSampleIndex = Math.max(0, startSampleIndex);
    	this.grainLength = (grainLength > 0) ? grainLength : settings.defaultGrainLength;
    	this.timeHopSamples = (timeHopSamples > 0) ? timeHopSamples : settings.hopSamples;

    	this.indexHopSamples = (indexHopSamples > 0) ? indexHopSamples : this.timeHopSamples;
    	this.numGrains = numGrains;

    	this.pitchRatio = (pitchRatio > 0f) ? pitchRatio : 1.0f;

    	this.windowFunction = settings.windowFunction;

    	this.totalLengthSamples =
    			(long) (numGrains - 1) * (long) this.timeHopSamples + (long) this.grainLength;
    }

    public IndexGranularSource(float[] source,
                                    GranularSettings settings,
                                    int startSampleIndex,
                                    int grainLength,
                                    int timeHopSamples,
                                    int indexHopSamples,
                                    int numGrains) {
        this(source, settings, startSampleIndex, grainLength, timeHopSamples, indexHopSamples, numGrains, 1.0f);
    }

    /**
     * Convenience constructor: no pitch-shift yet, use timeHop for source hop.
     */
    public IndexGranularSource(float[] source,
                                    GranularSettings settings,
                                    int startSampleIndex,
                                    int grainLength,
                                    int hopSamples,
                                    int numGrains) {
        this(source, settings, startSampleIndex, grainLength,
             hopSamples, hopSamples, numGrains);    // indexHop == timeHop
    }

    /**
     * Factory: build by duration in milliseconds.
     *
     * @param source           mono source buffer
     * @param settings         granular settings
     * @param startSampleIndex first source sample
     * @param grainLength      grain length in samples (<=0 → default)
     * @param hopSamples       time hop between grains (samples)
     * @param durationMs       total output duration in ms
     * @param sampleRate       playback sample rate (e.g., 44100)
     */
    public static IndexGranularSource fromDurationMs(float[] source,
                                                          GranularSettings settings,
                                                          int startSampleIndex,
                                                          int grainLength,
                                                          int hopSamples,
                                                          float durationMs,
                                                          float sampleRate) {
        if (durationMs <= 0f) {
            throw new IllegalArgumentException("durationMs must be > 0");
        }
        if (sampleRate <= 0f) {
            throw new IllegalArgumentException("sampleRate must be > 0");
        }

        int effectiveGrainLen = (grainLength > 0)
                ? grainLength
                : settings.defaultGrainLength;

        int effectiveHop = (hopSamples > 0)
                ? hopSamples
                : settings.hopSamples;

        long totalOutSamples = Math.round(durationMs * 0.001f * sampleRate);

        if (totalOutSamples <= effectiveGrainLen) {
            // Single grain fits the whole duration
            return new IndexGranularSource(
                    source,
                    settings,
                    startSampleIndex,
                    effectiveGrainLen,
                    effectiveHop,
                    1
            );
        }

        // g-th grain start = g * hop
        // last grain start must be < totalOutSamples
        // last grain end = g * hop + grainLen <= totalOutSamples
        long maxStart = totalOutSamples - effectiveGrainLen;
        int numGrains = (int) (maxStart / effectiveHop) + 1;
        if (numGrains < 1) numGrains = 1;

        return new IndexGranularSource(
                source,
                settings,
                startSampleIndex,
                effectiveGrainLen,
                effectiveHop,
                numGrains
        );
    }

    /**
     * Factory: build by duration in seconds.
     */
    public static IndexGranularSource fromDurationSeconds(float[] source,
                                                               GranularSettings settings,
                                                               int startSampleIndex,
                                                               int grainLength,
                                                               int hopSamples,
                                                               float durationSeconds,
                                                               float sampleRate) {
        return fromDurationMs(
                source,
                settings,
                startSampleIndex,
                grainLength,
                hopSamples,
                durationSeconds * 1000f,
                sampleRate
        );
    }

    // ------------------------------------------------------------------------
    // PASource implementation
    // ------------------------------------------------------------------------

    @Override
    public void seekTo(long absoluteSample) {
        this.noteStartSample = absoluteSample;
        this.noteStarted = true;
    }

    @Override
    public void renderBlock(long blockStart,
                            int blockSize,
                            float[] outL,
                            float[] outR) {

        if (!noteStarted || numGrains == 0) {
            return;
        }
        if (outL == null || blockSize <= 0) {
            return;
        }

        long blockEnd = blockStart + blockSize;

        for (int g = 0; g < numGrains; g++) {
            long grainStartAbs = noteStartSample + (long) g * (long) timeHopSamples;
            long grainEndAbs   = grainStartAbs + (long) grainLength;

            // Skip if no overlap
            if (grainEndAbs <= blockStart || grainStartAbs >= blockEnd) {
                continue;
            }

            OverlapUtil.Slice slice = OverlapUtil.computeBlockSlice(
                    blockStart, blockSize, grainStartAbs, grainEndAbs);
            if (!slice.hasOverlap) {
                continue;
            }

            int i0 = slice.startIndex;
            int i1 = slice.endIndex;

            // Window curve for this grain length
            float[] window = windowCache.getWindowCurve(windowFunction, grainLength);

            // Per-grain gain & pan: for now use GranularSettings global gain/pan,
            // or you could add per-grain modulation later.
            float gain = settings.gain;
            float pan = settings.pan; // assume [-1..+1]

            // Equal-power pan
            float panAngle = (pan + 1.0f) * 0.25f * (float) Math.PI; // -1..+1 → 0..π/2
            float panL = (float) Math.cos(panAngle);
            float panR = (float) Math.sin(panAngle);

            long grainSourceStart = (long) startSampleIndex + (long) g * (long) indexHopSamples;

            for (int i = i0; i < i1; i++) {
            	long globalSample = blockStart + i;
            	int offsetInGrain = (int) (globalSample - grainStartAbs);
            	if (offsetInGrain < 0 || offsetInGrain >= grainLength) {
            		continue;
            	}

            	float srcPos = (float) grainSourceStart + (float) offsetInGrain * pitchRatio;

            	// Need i0 and i0+1 valid for interpolation:
            	if (srcPos < 0f || srcPos >= (source.length - 1)) {
            		continue;
            	}

            	float w = window[offsetInGrain];
            	float s = readLinear(source, srcPos) * w * gain;
            	float sL = s * panL;
            	float sR = s * panR;

            	outL[i] += sL;
            	if (outR != null && outR != outL) {
            		outR[i] += sR;
            	}
            }
        }
    }

    @Override
    public long lengthSamples() {
        return totalLengthSamples;
    }

    @Override
    public PitchPolicy pitchPolicy() {
        // Time/pitch are determined by the granular config; instrument should not re-pitch.
        return PitchPolicy.SOURCE_GRANULAR;
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    public float[] getSource() {
        return source;
    }

    public GranularSettings getSettings() {
        return settings;
    }

    public int getStartSampleIndex() {
        return startSampleIndex;
    }

    public int getGrainLength() {
        return grainLength;
    }

    public int getTimeHopSamples() {
        return timeHopSamples;
    }

    public int getIndexHopSamples() {
        return indexHopSamples;
    }

    public int getNumGrains() {
        return numGrains;
    }
    
    
    // ------------------------------------------------------------------------
    // Utilities
    // ------------------------------------------------------------------------

    private static float readLinear(float[] buf, float pos) {
        int i0 = (int) pos;
        float frac = pos - i0;

        if (i0 < 0) return buf[0];
        if (i0 >= buf.length - 1) return buf[buf.length - 1];

        float a = buf[i0];
        float b = buf[i0 + 1];
        return a + frac * (b - a);
    }

}
