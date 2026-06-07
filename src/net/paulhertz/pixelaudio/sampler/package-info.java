/**
 * Digital audio sampling synthesis classes for PixelAudio, built on the
 * Minim library for Processing.
 *
 * <p>The package provides shared-buffer sample playback, polyphonic sampler
 * instruments, ADSR envelope support, pitch and playback utilities, and event
 * objects used by gesture-driven sampler workflows.</p>
 *
 * <p><b>Sampler instruments and playback</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASamplerInstrument PASamplerInstrument}
 *   manages sampler playback, global pitch, pan, gain, and default envelopes.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASamplerInstrumentPool PASamplerInstrumentPool}
 *   manages a collection of sampler instruments for polyphony and reuse.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASamplerInstrumentPoolMulti PASamplerInstrumentPoolMulti}
 *   manages multiple sampler pools, useful for multi-buffer or multi-sample playback.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASharedBufferSampler PASharedBufferSampler}
 *   plays multiple sampler voices from a shared mono buffer, with voice pooling,
 *   looping, panning, gain control, and smooth voice stealing.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASamplerVoice PASamplerVoice}
 *   handles one active sampler voice.</li>
 * </ul>
 *
 * <p><b>Interfaces and playback support</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PAPlayable PAPlayable}
 *   is the root interface for playable or renderable audio objects.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASampler PASampler}
 *   defines fundamental sampler methods, including play and buffer access.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASamplerPlayable PASamplerPlayable}
 *   extends PAPlayable for sample-based playback with gain, pitch, pan, and envelope parameters.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PlaybackInfo PlaybackInfo}
 *   stores information about sampler playback events.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PitchPolicy PitchPolicy}
 *   defines pitch behavior for sample playback.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.SamplerBrushEvent SamplerBrushEvent}
 *   stores gesture-derived sampler event data such as location, timing, sample position,
 *   gain, pitch, pan, and envelope.</li>
 * </ul>
 *
 * <p><b>Envelope utilities</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.ADSRParams ADSRParams}
 *   wraps parameters for ADSR envelopes.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.ADSRUtils ADSRUtils}
 *   provides utility methods for ADSRParams.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.SimpleADSR SimpleADSR}
 *   provides an executable ADSR envelope for sampler voices.</li>
 * </ul>
 */
package net.paulhertz.pixelaudio.sampler;