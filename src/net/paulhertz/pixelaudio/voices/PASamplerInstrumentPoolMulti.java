package net.paulhertz.pixelaudio.voices;

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;

import java.util.LinkedHashMap;
import java.util.Map;

public class PASamplerInstrumentPoolMulti implements PASamplerPlayable, PAPlayable {

    private final AudioOutput out;
    private final Map<String, PASamplerInstrumentPool> pools = new LinkedHashMap<>();
    private String activeKey = null; // default route

    public PASamplerInstrumentPoolMulti(AudioOutput out) {
        this.out = out;
    }

    public void addPool(String key, MultiChannelBuffer buffer, int poolSize, float bufferSampleRate, ADSRParams env) {
        PASamplerInstrumentPool p = new PASamplerInstrumentPool(out, buffer, poolSize, bufferSampleRate, env);
        pools.put(key, p);
        if (activeKey == null) activeKey = key;
    }

    public void setActive(String key) {
        if (pools.containsKey(key)) activeKey = key;
    }

    private PASamplerInstrumentPool current() {
        return (activeKey != null) ? pools.get(activeKey) : null;
    }

    // --- PAPlayable ---
    @Override
    public int play(float amplitude, float pitch, float pan) {
        PASamplerInstrumentPool p = current();
        return (p != null) ? p.play(amplitude, pitch, pan) : 0;
    }
    @Override
    public void stop() {
        for (PASamplerInstrumentPool p : pools.values()) p.stop();
    }

    // --- PASamplerPlayable ---
    @Override
    public int play(int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch, float pan) {
        PASamplerInstrumentPool p = current();
        return (p != null) ? p.play(samplePos, sampleLen, amplitude, env, pitch, pan) : 0;
    }
    // @Override
    public boolean isLooping() {
        for (PASamplerInstrumentPool p : pools.values()) if (p.isLooping()) return true;
        return false;
    }
   //  @Override
    public void stopAll() {
        for (PASamplerInstrumentPool p : pools.values()) p.stopAll();
    }

    // --- Back-compat playSample(...) shims ---
    public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch, float pan) {
        PASamplerInstrumentPool p = current();
        return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, env, pitch, pan) : 0;
    }
    public int playSample(int samplePos, int sampleLen, float amplitude) {
        PASamplerInstrumentPool p = current();
        return (p != null) ? p.playSample(samplePos, sampleLen, amplitude) : 0;
    }
    public int playSample(int samplePos, int sampleLen, float amplitude, float pitch) {
        PASamplerInstrumentPool p = current();
        return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, pitch) : 0;
    }
    public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
        PASamplerInstrumentPool p = current();
        return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, env) : 0;
    }
    public int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
                          float amplitude, ADSRParams env, float pitch, float pan) {
        PASamplerInstrumentPool p = current();
        return (p != null) ? p.playSample(buffer, samplePos, sampleLen, amplitude, env, pitch, pan) : 0;
    }
    public int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
                          float amplitude, ADSRParams env, float pitch) {
        PASamplerInstrumentPool p = current();
        return (p != null) ? p.playSample(buffer, samplePos, sampleLen, amplitude, env, pitch) : 0;
    }
    public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch) {
        PASamplerInstrumentPool p = current();
        return (p != null) ? p.playSample(samplePos, sampleLen, amplitude, env, pitch) : 0;
    }

    // TimedLocation hooks can simply forward to the selected pool when we’re ready
    
    // ------------------------------------------------------------------------
    // Resource management
    // ------------------------------------------------------------------------

    private boolean isClosed = false;

    /** Close all sub-pools and release shared resources. */
    public synchronized void close() {
    	if (isClosed) return;

    	for (PASamplerInstrumentPool pool : pools.values()) {
    		pool.close();
    	}

    	pools.clear();
    	activeKey = null;

    	isClosed = true;
    }

    public synchronized boolean isClosed() { return isClosed; }

}
