package net.paulhertz.pixelaudio.voices;

import java.util.Arrays;

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;

/**
 * PASamplerInstrument
 *
 * Represents a playable instrument that uses a PASampler instantiated as a PASharedBufferSampler
 * to trigger audio playback from a shared buffer that is (currently) a Minim MultiChannelBuffer. 
 * TODO decide if MultiChannelBuffer or float[] is the appropriate structure for storing the buffer. 
 *     float[] is preferred for the general style of PixelAudio, but MultiChannelBuffer support 
 *     is a useful consideration for the future. 
 * TODO sample accurate start for sampler events to unify timing API with PAGranularInstrument.
 * Currently sampler play() is block-time accurate only.
 * In the future, add sample-time scheduling (startAtSampleTime / startAfterDelaySamples)
 * using AudioScheduler so sampler and granular share a transport model.
 *
 * Supports:
 *  - Global pitch scaling
 *  - Global stereo pan
 *  - Default ADSR envelope
 *  - Compensation for differing buffer vs. output sample rates
 *  - Cached buffer size for efficiency
 *
 * Implements both PAPlayable and PASamplerPlayable for full compatibility.
 */
public class PASamplerInstrument implements PASamplerPlayable {

	// ------------------------------------------------------------------------
	// Core fields
	// ------------------------------------------------------------------------
	private final PASampler sampler;
	private MultiChannelBuffer buffer;
	private final AudioOutput out;
	private int bufferSize;    // TODO consider replacing this instance variable with a call to buffer.getBufferSize()
	private int maxVoices;

	// Sample rate information
	private float bufferSampleRate;  // sample rate of source from which buffer was loaded
	private float outputSampleRate;  // sample rate of AudioOutput
	private float sampleRateRatio;   // bufferRate / outputRate, used to correct playback speed

	private ADSRParams defaultEnv;

	// Global modifiers
	private volatile float globalPitch = 1.0f;  // global pitch multiplier
	private float globalPan = 0.0f;             // -1.0 = left, +1.0 = right, 0.0 = center


	// ------------------------------------------------------------------------
	// Constructors
	// ------------------------------------------------------------------------

	/**
	 * Primary constructor for backward compatibility.
	 *
	 * @param buffer          the shared MultiChannelBuffer
	 * @param sampleRate      nominal sample rate of the buffer (Hz)
	 * @param maxVoices       number of simultaneous voices (polyphony)
	 * @param audioOut        target AudioOutput
	 * @param env             default ADSR envelope parameters
	 */
	public PASamplerInstrument(MultiChannelBuffer buffer, float sampleRate, int maxVoices,  AudioOutput audioOut, ADSRParams env) {
		this.out = audioOut;
		this.buffer = buffer;
		this.bufferSize = buffer.getBufferSize();
		this.bufferSampleRate = sampleRate;
		this.outputSampleRate = (audioOut != null) ? audioOut.sampleRate() : sampleRate;
		this.sampleRateRatio = (outputSampleRate > 0f) ? bufferSampleRate / outputSampleRate : 1.0f;
		this.defaultEnv = (env != null) ? env : new ADSRParams(1f, 0.01f, 0.2f, 0.8f, 0.3f);
		this.globalPitch = 1.0f;
		this.globalPan = 0.0f;
		this.isClosed = false;
		this.maxVoices = Math.max(1, maxVoices);
		// pass through to sampler
		this.sampler = new PASharedBufferSampler(buffer, sampleRate, audioOut, this.maxVoices);
	}

	/**
	 * Full constructor with explicit buffer sample rate, custom sampler, and envelope.
	 */
	public PASamplerInstrument(MultiChannelBuffer buffer, AudioOutput out, PASampler sampler, ADSRParams defaultEnv, float bufferSampleRate) {
		this.out = out;
		this.buffer = buffer;
		this.sampler = sampler;
		this.defaultEnv = defaultEnv;

		this.bufferSize = (buffer != null) ? buffer.getBufferSize() : 0;
		this.outputSampleRate = (out != null) ? out.sampleRate() : bufferSampleRate;
		this.bufferSampleRate = bufferSampleRate;
		this.sampleRateRatio = (outputSampleRate > 0f) ? bufferSampleRate / outputSampleRate : 1f;
	}

	/**
	 * Convenience constructor assuming buffer and output share the same rate.
	 */
	public PASamplerInstrument(MultiChannelBuffer buffer, AudioOutput out) {
		this(buffer, 
			 out, 
			 new PASharedBufferSampler(buffer, out.sampleRate(), out),
			 new ADSRParams(1f, 0.01f, 0.2f, 0.8f, 0.3f),
			 out.sampleRate());
	}

	// ------------------------------------------------------------------------
	// Interface Implementations
	// ------------------------------------------------------------------------

	/** 
	 * Generic play() from PAPlayable, will play the entire buffer from the beginning of the buffer
	 * with specified pitch and pan and default envelope. 
	 */
	@Override
	public int play(float amplitude, float pitch, float pan) {
		if (sampler == null || bufferSize <= 0) return 0;
		float scaledPitch = pitch * globalPitch * sampleRateRatio;
		float finalPan = clampPan(globalPan + pan);
		return sampler.play(0, bufferSize, amplitude, defaultEnv, scaledPitch, finalPan);
	}

	/**
	 * The primary play method, called by all playSample() methods, from PASamplerPlayable interface.
	 */
	@Override
	public int play(int samplePos, int sampleLen, float amplitude,
			ADSRParams env, float pitch, float pan) {
		if (sampler == null || bufferSize <= 0) return 0;
		float scaledPitch = pitch * globalPitch * sampleRateRatio;
		float finalPan = clampPan(globalPan + pan);
		ADSRParams useEnv = (env != null) ? env : defaultEnv;
		return sampler.play(samplePos, sampleLen, amplitude, useEnv, scaledPitch, finalPan);
	}

	/** Stop playback (stop all active voices). */
	@Override
	public void stop() {
		if (sampler != null) sampler.stopAll();
	}

	// ------------------------------------------------------------------------
	// Core playback - legacy playSample() overloads
    //
    // Standard argument order: 
    // (int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch, float pan)
    //
	// ------------------------------------------------------------------------

	/**
	 * Trigger playback using all six standard per-voice parameters.
	 *
	 * @param samplePos  start position (samples)
	 * @param sampleLen  playback length (samples)
	 * @param amplitude  per-voice amplitude
	 * @param env        ADSR envelope parameters
	 * @param pitch      playback rate (1.0 = normal)
	 * @param pan        stereo position (-1.0 = left, +1.0 = right)
	 * @return the actual length of the audio event, in samples
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude,
			ADSRParams env, float pitch, float pan) {
		return play(samplePos, sampleLen, amplitude, env, pitch, pan);
	}

	/**
	 * All params except the envelope, so we use the default envelope. 
	 * 
	 * @param samplePos
	 * @param sampleLen
	 * @param amplitude
	 * @param pitch
	 * @param pan
	 * @return the actual length of the audio event, in samples
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude, 
			float pitch, float pan) {
		return play(samplePos, sampleLen, amplitude, defaultEnv, pitch, pan);
	}

	/**
	 * Convenience overload: uses default envelope, default pitch, and center pan.
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude) {
		return play(samplePos, sampleLen, amplitude, defaultEnv, globalPitch, globalPan);
	}

	/**
	 * Convenience overload: uses default envelope, supplied pitch and center pan.
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude, float pitch) {
		return play(samplePos, sampleLen, amplitude, defaultEnv, pitch, globalPan);
	}

	/**
	 * Plays a sample using a supplied envelope with default pitch and default pan.
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
		return play(samplePos, sampleLen, amplitude, env, globalPitch, globalPan);
	}

	/**
	 * Plays a sample using a supplied envelope, pitch, and current global pan.
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env, float pitch) 	{
		return play(samplePos, sampleLen, amplitude, env, pitch, globalPan);
	}

	/**
	 * Sets a new buffer for PASamplerInstrument and plays it with envelope, pitch, and pan.
	 * For large buffers, there may be some latency. 
	 */
	public synchronized int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
			float amplitude, ADSRParams env, float pitch, float pan) {
		if (buffer != null) this.setBuffer(buffer);
		return play(samplePos, sampleLen, amplitude, env, pitch, pan);
	}

	/**
	 * Sets a new buffer for PASamplerInstrument and plays it with envelope, pitch, and default pan.
	 */
	public synchronized int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
			float amplitude, ADSRParams env, float pitch) {
		if (buffer != null) this.setBuffer(buffer);
		return play(samplePos, sampleLen, amplitude, env, pitch, globalPan);
	}

	// ------------------------------------------------------------------------
	// Accessors and utilities
	// ------------------------------------------------------------------------

	public boolean hasAvailableVoice() {
	    if (sampler == null) return false;
	    for (PASamplerVoice v : ((PASharedBufferSampler) sampler).getVoices()) {
	        if (v.isFinished() || (!v.isActive() && !v.isReleasing())) {
	            return true;
	        }
	    }
	    return false;
	}

	// --- Helpers for pool orchestration (non-invasive; read-only) ---
	public boolean hasActiveOrReleasingVoices() {
	    PASharedBufferSampler s = (PASharedBufferSampler) getSampler();
	    for (PASamplerVoice v : s.getVoices()) {
	        if (v.isActive() || v.isReleasing()) return true;
	    }
	    return false;
	}

	public int activeOrReleasingVoiceCount() {
	    PASharedBufferSampler s = (PASharedBufferSampler) getSampler();
	    int c = 0;
	    for (PASamplerVoice v : s.getVoices()) {
	        if (v.isActive() || v.isReleasing()) c++;
	    }
	    return c;
	}

	/** Smoothly release all active voices (used only if we must recycle an instrument). */
	public void releaseAllVoices() {
	    PASharedBufferSampler s = (PASharedBufferSampler) getSampler();
	    for (PASamplerVoice v : s.getVoices()) {
	        if (v.isActive() || v.isReleasing()) v.release();
	    }
	}

	public PASampler getSampler() { return sampler; }
	
	public MultiChannelBuffer getBuffer() { return buffer; }
	public int getBufferSize() { return bufferSize; }
	
	/**
	 * Copy the data and settings in the provided MultiChannelBuffer to this.buffer; 
	 * bufferSampleRate is unchanged. The sampler and its voices are also refreshed.
	 * Note that bufferSize will change if the buffers are not the same size.
	 * 
	 * @param newBuffer    buffer to replace the current buffer
	 */
	public synchronized void setBuffer(MultiChannelBuffer newBuffer) {
	    if (newBuffer != null) {
	    	// use MultiChannelBuffer.set() to avoid reallocating memory and copy sample data efficiently
	        this.buffer.set(newBuffer);
	        this.bufferSize = newBuffer.getBufferSize();
	        float[] sharedBuffer = Arrays.copyOf(newBuffer.getChannel(0), newBuffer.getBufferSize());
	        sampler.setBuffer(sharedBuffer);
	    }
	}
	
	/**
	 * Copy the data and settings of the provided MultiChannelBuffer to this.buffer 
	 * and set bufferSampleRate. The sampler and its voices are also refreshed.
	 * Note that bufferSize will change if the buffers are not the same size.
	 * 
	 * @param newBuffer    buffer to replace the current buffer
	 * @param newSampleRate
	 */
	public synchronized void setBuffer(MultiChannelBuffer newBuffer, float newSampleRate) {
	    if (newBuffer != null) {
	    	// use MultiChannelBuffer.set() to avoid reallocating memory and copy sample data efficiently
	    	this.buffer.set(newBuffer);
	    	this.bufferSize = newBuffer.getBufferSize();
	    	setBufferSampleRate(newSampleRate);
	        float[] sharedBuffer = Arrays.copyOf(newBuffer.getChannel(0), newBuffer.getBufferSize());
	        sampler.setBuffer(sharedBuffer);
	    }
	}

	/**
	 * Replace the instrument's active buffer contents with a new signal array using
	 * MultiChannelBuffer.setChannel(0, newBuffer), which creates a copy of the supplied array,
	 * and set a new bufferSamplerRate. The sampler and its voices are also refreshed.
	 * Note that bufferSize will change if the this.buffer and newBuffer are not the same size.
	 * 
	 * The instrument retains its own MultiChannelBuffer but updates its internal copy.
	 * The sampler and its voices also receive a fresh copy.
	 */
	public synchronized void setBuffer(float[] newBuffer, float newSampleRate) {
	    if (newBuffer == null || newBuffer.length == 0) return;
	    // Ensure the instrument's internal MultiChannelBuffer matches size of newBuffer
	    if (buffer == null || buffer.getBufferSize() != newBuffer.length) {
	        this.buffer.setBufferSize(newBuffer.length);
	    }
	    // Copy signal into the instrument's internal buffer
	    buffer.setChannel(0, newBuffer);
	    bufferSize = newBuffer.length;
	    // Update sample rate references
	    setBufferSampleRate(newSampleRate);
	    // Propagate to sampler and voices — safe internal copy for playback
	    sampler.setBuffer(Arrays.copyOf(newBuffer, newBuffer.length), newSampleRate);
	}

	
	public AudioOutput getAudioOutput() { return out; }

	// --- Sample rate information ---
	public float getBufferSampleRate() { return bufferSampleRate; }
	public float getOutputSampleRate() { return outputSampleRate; }
	public float getSampleRateRatio() { return sampleRateRatio; }
	
	/**
	 * Update the buffer's intrinsic sample rate.
	 * 
	 * @param newRate    new sample rate for the buffer, affects playback rate
	 */
	public synchronized void setBufferSampleRate(float newRate) {
	    if (newRate > 0f) {
	        this.bufferSampleRate = newRate;
	        this.sampleRateRatio = (outputSampleRate > 0f)
	            ? bufferSampleRate / outputSampleRate
	            : 1f;
	        // propagate to sampler if relevant
	        if (sampler != null) sampler.setSampleRate(newRate);
	    }
	}

	/**
	 * Update the output sample rate, for example, if audio device changes. 
	 * This is unlikely to happen, but we'll be cautious.
	 * 
	 * @param newRate
	 */
	public synchronized void setOutputSampleRate(float newRate) {
	    if (newRate > 0f) {
	        this.outputSampleRate = newRate;
	        this.sampleRateRatio = bufferSampleRate / outputSampleRate;
	    }
	}
	
	/** 
	 * Synchronize output sample rate from AudioOutput directly.
	 */
	public synchronized void updateRateFromOutput() {
	    if (out != null) {
	        setOutputSampleRate(out.sampleRate());
	    }
	}

	public ADSRParams getDefaultEnv() { return defaultEnv; }
	public void setDefaultEnv(ADSRParams env) { this.defaultEnv = env; }
	
	public void setPitchScale(float scale) { this.globalPitch = scale; }
	public float getPitchScale() { return globalPitch; }

	public void setGlobalPan(float pan) { this.globalPan = clampPan(pan); }
	public float getGlobalPan() { return globalPan; }

	// ------------------------------------------------------------------------
	// Helper
	// ------------------------------------------------------------------------

	private static float clampPan(float pan)
	{
		if (pan < -1f) return -1f;
		if (pan > 1f) return 1f;
		return pan;
	}
	
	// ------------------------------------------------------------------------
	// Resource management
	// ------------------------------------------------------------------------

	private boolean isClosed = false;

	/** Stop all voices and disconnect UGens from the output. */
	public synchronized void close() {
	    if (isClosed) return;

	    stop();

	    // Disconnect sampler from output
	    if (sampler != null && out != null) {
	        //sampler.unpatch(out);
	    }

	    // Optional: release buffer reference for GC
	    buffer = null;

	    isClosed = true;
	}

}
