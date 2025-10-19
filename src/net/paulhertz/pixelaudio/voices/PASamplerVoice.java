package net.paulhertz.pixelaudio.voices;

import ddf.minim.*;
import ddf.minim.ugens.*;
import java.util.concurrent.*;

/**
 * Represents a single playback voice for WFSamplerInstrument.
 *
 * Each voice holds its own Sampler, ADSR, and Panner.
 * Supports pitch scaling, looping, and per-note panning.
 * 
 */
public class PASamplerVoice {
    private final AudioOutput out;
    private final float sampleRate;
    private final ScheduledExecutorService scheduler;
    private final ADSRParams defaultEnv;
    private int bufferSize = 0;
    // Minim objects
    private Sampler sampler;
    private Pan panner;
    // state variables
    private boolean isBusy = false;
    private boolean isClosed = false;
    private boolean isLooping = false;
    // Minim loops over the complete buffer, we may roll our own start and end points later on
    // private int loopStart = 0;
    // private int loopEnd = 0;


    /**
     * Construct a voice with its own Sampler, Pan, and scheduler.
     *
     * @param buffer      buffer assigned to this voice
     * @param sampleRate  sample rate
     * @param out         AudioOutput for patching
     * @param env         default ADSR parameters
     */
    public PASamplerVoice(MultiChannelBuffer buffer, float sampleRate, AudioOutput out, ADSRParams env) {
    	this.sampleRate = sampleRate;
    	this.out = out;
    	this.defaultEnv = env;
    	this.sampler = new Sampler(buffer, sampleRate, 1);
    	this.panner = new Pan(0.0f);
    	this.bufferSize = buffer.getBufferSize();
    	this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
    		Thread t = new Thread(r, "WFSamplerVoice-scheduler");
    		t.setDaemon(true);
    		return t;
    	});
    }

    /**
     * Update this voice’s buffer (rebuilds its internal Sampler).
     */
    public synchronized void setBuffer(MultiChannelBuffer buffer) {
        if (isClosed) return;
        this.sampler = new Sampler(buffer, sampleRate, 1);
        this.bufferSize = buffer.getBufferSize();
    }

    /**
     * Plays this voice with explicit pitch scaling and ADSR.
     *
     * @param buffer     the buffer to play
     * @param samplePos  start position (samples)
     * @param sampleLen  length (samples)
     * @param amplitude  amplitude multiplier
     * @param pitch      playback rate (1.0 = normal speed)
     * @param env        ADSR envelope parameters
     */
    public synchronized void play(MultiChannelBuffer buffer,
                                  int samplePos,
                                  int sampleLen,
                                  float amplitude,
                                  float pitch,
                                  ADSRParams env) {
        if (isClosed || isBusy) return;
        isBusy = true;
        // Clip range
        if (samplePos < 0) samplePos = 0;
        if (sampleLen < 0) sampleLen = 0;
        if (!isLooping) {
        	if (samplePos + sampleLen > bufferSize) {
        		sampleLen = Math.max(0, bufferSize - samplePos);
        	}
        }
        int releaseSamples = Math.round(env.getRelease() * sampleRate);
        int end = samplePos + sampleLen + releaseSamples;
        if (end >= bufferSize) end = bufferSize - 1;
        // fresh ADSR per note
        ADSR noteAdsr = env.toADSR();
        // configure sampler
        sampler.amplitude.setLastValue(amplitude);
        sampler.begin.setLastValue(samplePos);
        sampler.end.setLastValue(end);
        sampler.rate.setLastValue(pitch);
        // Looping support
        sampler.looping = isLooping;
        // Patch: Sampler → ADSR → Panner → Output
        sampler.patch(noteAdsr);
        noteAdsr.patch(panner);
        panner.patch(out);
        // Trigger voice
        sampler.trigger();
        noteAdsr.noteOn();
        // Schedule cleanup
        if (!isLooping && sampleLen > 0) {
            long durationMillis = Math.round((sampleLen / (double) sampleRate) * 1000.0);
            long releaseMillis = Math.round(env.getRelease() * 1000.0);
            scheduler.schedule(() -> {
                try {
                    noteAdsr.noteOff();
                    noteAdsr.unpatchAfterRelease(panner);
                } 
                catch (Exception e) {
                    e.printStackTrace();
                }
            }, durationMillis, TimeUnit.MILLISECONDS);
            // Unpatch chain after envelope finishes
            long totalDelay = durationMillis + releaseMillis + 50;
            scheduler.schedule(() -> {
                try {
                    sampler.unpatch(noteAdsr);
                    noteAdsr.unpatch(panner);
                    panner.unpatch(out);
                } 
                catch (Exception ignored) {}
                isBusy = false;
            }, totalDelay, TimeUnit.MILLISECONDS);
        } 
        else {
            // If looping, let caller handle stop
            isBusy = false;
        }
    }
    
    public void play(MultiChannelBuffer buffer, int samplePos, int sampleLen, float amplitude, float pitch) {
        play(buffer, samplePos, sampleLen, amplitude, pitch, defaultEnv);
    }
    

    // ------------------------------------------------------------------------
    // PAN & LOOP CONTROL
    // ------------------------------------------------------------------------

    /** Set stereo pan position (-1 = left, 0 = center, +1 = right). */
    public void setPan(float pan) {
        if (panner != null) {
            panner.pan.setLastValue(pan);
        }
    }

    /** Get current pan position. */
    public float getPan() {
        return panner != null ? panner.pan.getLastValue() : 0.0f;
    }

    /** Enable or disable looping. */
    public void setIsLooping(boolean looping) {
        this.isLooping = looping;
        if (sampler != null) {
            sampler.looping = looping;
        }
    }
    
    // TODO add manual region looping:
    // - use scheduler to retrigger the Sampler at the region’s end
    // - optionally crossfade for clickless transitions

    /** Get current looping state. */
    public boolean isLooping() {
        return isLooping;
    }

    // ------------------------------------------------------------------------
    // CLEANUP
    // ------------------------------------------------------------------------

    /** Stop playback if looping. */
    public void stop() {
        try {
            sampler.stop();
        } catch (Exception ignored) {}
    }

    public void close() {
        if (isClosed) return;
        scheduler.shutdownNow();
        try {
            // Defensive unpatching: remove all outgoing and incoming connections
            // Minim’s Sampler class supports unpatch(target), not unpatchAll().
            sampler.unpatch(out);
        } 
        catch (Exception ignored) {}
        isClosed = true;
    }

    public boolean isBusy() {
        return isBusy;
    }

    public boolean isClosed() {
        return isClosed;
    }
}
