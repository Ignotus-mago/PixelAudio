package net.paulhertz.pixelaudio.voices;

import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;

/**
 * PASamplerInstrument
 *
 * Represents a playable instrument that uses a PASampler to trigger
 * audio playback from a shared buffer.
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
	private int bufferSize;
	private int maxVoices;

	// Sample rate information
	private float bufferSampleRate;  // sample rate at which buffer was loaded
	private float outputSampleRate;  // sample rate of AudioOutput
	private float sampleRateRatio;   // bufferRate / outputRate, used to correct playback speed

	private ADSRParams defaultEnv;

	// Global modifiers
	private volatile float pitchScale = 1.0f;  // global pitch multiplier
	private float globalPan = 0.0f;            // -1.0 = left, +1.0 = right, 0.0 = center

	// ------------------------------------------------------------------------
	// Constructors
	// ------------------------------------------------------------------------

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
		    this.pitchScale = 1.0f;
		    this.globalPan = 0.0f;
		    this.isClosed = false;
		    this.maxVoices = Math.max(1, maxVoices);
		    // pass through to sampler
		    this.sampler = new PASharedBufferSampler(buffer, sampleRate, audioOut, this.maxVoices);
		}

	/**
	 * Full constructor with explicit buffer sample rate, custom sampler, and envelope.
	 */
	public PASamplerInstrument(AudioOutput out,
			MultiChannelBuffer buffer,
			PASampler sampler,
			ADSRParams defaultEnv,
			float bufferSampleRate) {
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
	public PASamplerInstrument(AudioOutput out, MultiChannelBuffer buffer) {
		this(out,
				buffer,
				new PASharedBufferSampler(buffer, out.sampleRate(), out),
				new ADSRParams(1f, 0.01f, 0.2f, 0.8f, 0.3f),
				out.sampleRate());
	}

	// ------------------------------------------------------------------------
	// Interface Implementations
	// ------------------------------------------------------------------------

	/** Generic play() from PAPlayable. */
	@Override
	public int play(float amplitude, float pitch, float pan)
	{
		if (sampler == null || bufferSize <= 0) return 0;

		float scaledPitch = pitch * pitchScale * sampleRateRatio;
		float finalPan = clampPan(globalPan + pan);

		return sampler.play(0, bufferSize, amplitude, defaultEnv, scaledPitch, finalPan);
	}

	/** Full play() from PASamplerPlayable. */
	@Override
	public int play(int samplePos, int sampleLen, float amplitude,
			ADSRParams env, float pitch, float pan)
	{
		if (sampler == null || bufferSize <= 0) return 0;

		float scaledPitch = pitch * pitchScale * sampleRateRatio;
		float finalPan = clampPan(globalPan + pan);
		ADSRParams useEnv = (env != null) ? env : defaultEnv;

		return sampler.play(samplePos, sampleLen, amplitude, useEnv, scaledPitch, finalPan);
	}

	/** Stop playback (stop all active voices). */
	@Override
	public void stop()
	{
		if (sampler != null) sampler.stopAll();
	}

	// ------------------------------------------------------------------------
	// Core playback - legacy playSample() overloads
	// ------------------------------------------------------------------------

	/**
	 * Trigger playback using per-voice parameters.
	 *
	 * @param samplePos  start position (samples)
	 * @param sampleLen  playback length (samples)
	 * @param amplitude  per-voice amplitude
	 * @param env        ADSR envelope parameters
	 * @param pitch      playback rate (1.0 = normal)
	 * @param pan        stereo position (-1.0 = left, +1.0 = right)
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude,
			ADSRParams env, float pitch, float pan)
	{
		if (sampler == null) return 0;

		float actualPitch = pitch * pitchScale * sampleRateRatio;
		float actualPan = clampPan(pan + globalPan);
		ADSRParams useEnv = (env != null) ? env : defaultEnv;

		return sampler.play(samplePos, sampleLen, amplitude, useEnv, actualPitch, actualPan);
	}

	/**
	 * Convenience overload: uses default envelope, default pitch, and center pan.
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude)
	{
		return playSample(samplePos, sampleLen, amplitude, defaultEnv, pitchScale, 0.0f);
	}

	/**
	 * Convenience overload: uses default envelope, supplied pitch and center pan.
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude, float pitch)
	{
		return playSample(samplePos, sampleLen, amplitude, defaultEnv, pitch, 0.0f);
	}

	/**
	 * Plays a sample using a supplied envelope with default pitch and default pan.
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env)
	{
		return playSample(samplePos, sampleLen, amplitude, env, pitchScale, globalPan);
	}

	/**
	 * Play a buffer directly with envelope, pitch, and pan.
	 * Updates this instrument's buffer reference before playback.
	 */
	public synchronized int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
			float amplitude, ADSRParams env, float pitch, float pan)
	{
		if (buffer != null) this.setBuffer(buffer);
		return playSample(samplePos, sampleLen, amplitude, env, pitch, pan);
	}

	/**
	 * Play a buffer directly with envelope and pitch (uses current pan).
	 */
	public synchronized int playSample(MultiChannelBuffer buffer, int samplePos, int sampleLen,
			float amplitude, ADSRParams env, float pitch)
	{
		if (buffer != null) this.setBuffer(buffer);
		return playSample(samplePos, sampleLen, amplitude, env, pitch, globalPan);
	}

	/**
	 * Plays a sample using a supplied envelope, pitch, and current global pan.
	 */
	public synchronized int playSample(int samplePos, int sampleLen, float amplitude,
			ADSRParams env, float pitch)
	{
		return playSample(samplePos, sampleLen, amplitude, env, pitch, globalPan);
	}

	// ------------------------------------------------------------------------
	// Accessors and utilities
	// ------------------------------------------------------------------------

	public PASampler getSampler() { return sampler; }
	public ADSRParams getDefaultEnv() { return defaultEnv; }
	public void setDefaultEnv(ADSRParams env) { this.defaultEnv = env; }
	
	public MultiChannelBuffer getBuffer() { return buffer; }
	public int getBufferSize() { return bufferSize; }
	
	public synchronized void setBuffer(MultiChannelBuffer newBuffer) {
	    if (newBuffer != null) {
	        this.buffer = newBuffer;
	        // update bufferSize if you want to support swapping:
	        this.bufferSize = newBuffer.getBufferSize();
	    }
	}
	
	public synchronized void setBuffer(MultiChannelBuffer newBuffer, float newSampleRate) {
	    if (newBuffer != null) {
	    	this.buffer = newBuffer;
	    	this.bufferSize = newBuffer.getBufferSize();
	    	setBufferSampleRate(newSampleRate);
	    }
	}


	
	public AudioOutput getAudioOutput() { return out; }

	// --- Sample rate information ---
	public float getBufferSampleRate() { return bufferSampleRate; }
	public float getOutputSampleRate() { return outputSampleRate; }
	public float getSampleRateRatio() { return sampleRateRatio; }
	
	/** Update the buffer's intrinsic sample rate. */
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

	/** Update the output sample rate (e.g. if audio device changes). */
	public synchronized void setOutputSampleRate(float newRate) {
	    if (newRate > 0f) {
	        this.outputSampleRate = newRate;
	        this.sampleRateRatio = bufferSampleRate / outputSampleRate;
	    }
	}
	
	/** Synchronize output sample rate from AudioOutput directly. */
	public synchronized void updateRateFromOutput() {
	    if (out != null) {
	        setOutputSampleRate(out.sampleRate());
	    }
	}


	// --- Global Pitch and Pan Modifiers ---
	public void setPitchScale(float scale) { this.pitchScale = scale; }
	public float getPitchScale() { return pitchScale; }

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
