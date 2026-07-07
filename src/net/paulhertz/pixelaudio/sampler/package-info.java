/**
 * Digital audio sampling synthesis classes for PixelAudio.
 *
 * <p>PixelAudio's sampler engine follows the chain
 * {@code PASamplerInstrumentPoolMulti -> PASamplerInstrumentPool ->
 * PASamplerInstrument -> PASharedBufferSampler -> PASamplerVoice}. Small sketches may
 * use {@link net.paulhertz.pixelaudio.sampler.PASamplerInstrument PASamplerInstrument}
 * directly, while larger gesture-driven sketches can route playback through one or more
 * instrument pools.</p>
 *
 * <p>The package provides shared-buffer sample playback, polyphonic sampler instruments,
 * ADSR envelope support, pitch and pan controls, gain staging, and event objects used by
 * gesture-driven sampler workflows. It is built on the Minim library for Processing.</p>
 *
 * <p>Immediate playback and scheduled playback both follow the same hierarchy. Calls made
 * on {@code PASamplerInstrumentPoolMulti}, {@code PASamplerInstrumentPool}, or
 * {@code PASamplerInstrument} are forwarded to the level below, ending in
 * {@link net.paulhertz.pixelaudio.sampler.PASharedBufferSampler PASharedBufferSampler},
 * where voices are allocated and mixed. The scheduled playback API mirrors the granular
 * synthesis API with {@code startAtSampleTime(...)} and {@code startAfterDelaySamples(...)}
 * methods. These methods schedule against the sampler's local audio-thread sample clock,
 * exposed by {@code getCurrentSampleTime()}.</p>
 *
 * <p>For cross-engine alignment, such as playing sampler and granular instruments from the
 * same generated beat brush, application code should choose a shared transport clock and
 * convert gesture or beat times to absolute sample times before calling either synth. The
 * sampler-local clock is the timing mechanism for one sampler backend; it is not, by itself,
 * a global application transport.</p>
 *
 * <p><b>Sampler playback processing chain</b></p>
 * <ol>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASamplerInstrumentPoolMulti PASamplerInstrumentPoolMulti}
 *   manages named sampler pools and routes playback to the active pool or a tagged pool.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASamplerInstrumentPool PASamplerInstrumentPool}
 *   owns a reusable group of sampler instruments, selects an available instrument for each
 *   event, and propagates buffer, gain, pan, pitch, and envelope defaults.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASamplerInstrument PASamplerInstrument}
 *   applies instrument-level gain, pitch, pan, envelope defaults, and sample-rate correction,
 *   then forwards playback requests to the sampler backend.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASharedBufferSampler PASharedBufferSampler}
 *   owns the voice pool for one shared mono buffer, starts voices, mixes active voices into
 *   Minim's audio callback, and applies mix normalization and soft clipping.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASamplerVoice PASamplerVoice}
 *   reads samples from the source buffer, applies per-voice gain, pan, pitch, envelope state,
 *   looping state, and release/finish lifecycle handling.</li>
 * </ol>
 *
 * <p><b>Sampler synthesis engine</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASamplerInstrument PASamplerInstrument}
 *   provides the high-level instrument API for pan, gain, pitch scaling, envelopes,
 *   buffer replacement, and sampler backend access.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASamplerInstrumentPool PASamplerInstrumentPool}
 *   manages a collection of sampler instruments for polyphony, reuse, and group gain.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASamplerInstrumentPoolMulti PASamplerInstrumentPoolMulti}
 *   manages multiple sampler pools for multi-buffer or multi-sample playback.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASharedBufferSampler PASharedBufferSampler}
 *   manages sampler voices, voice pooling, looping defaults, panning, gain, smooth voice
 *   stealing, and mix profiles.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASamplerVoice PASamplerVoice}
 *   renders individual sampler voices with gain, pitch, pan, and optional ADSR envelope.</li>
 * </ul>
 *
 * <p><b>Interfaces and event data</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PAPlayable PAPlayable}
 *   defines the minimal playable audio API used by sampler instruments and pools.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASampler PASampler}
 *   defines the backend sampler contract, including immediate playback, scheduled playback,
 *   looping, stop, source-buffer playback sample rate, and buffer update methods.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PASamplerPlayable PASamplerPlayable}
 *   extends PAPlayable for sample-based playback with buffer position, duration, gain, pitch,
 *   pan, and envelope parameters.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PlaybackInfo PlaybackInfo}
 *   stores computed duration and timing information for triggered sampler events.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.PitchPolicy PitchPolicy}
 *   defines pitch behavior for sample playback.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.SamplerBrushEvent SamplerBrushEvent}
 *   stores gesture-derived sampler event data such as location, timing, sample position,
 *   gain, pitch, pan, and envelope.</li>
 * </ul>
 *
 * <p><b>Envelope support</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.ADSRParams ADSRParams}
 *   stores parameters for ADSR envelopes and can create Minim ADSR instances.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.ADSRUtils ADSRUtils}
 *   provides utility methods for fitting envelope times to event durations.</li>
 *   <li>{@link net.paulhertz.pixelaudio.sampler.SimpleADSR SimpleADSR}
 *   provides a sample-by-sample ADSR envelope used by sampler voices.</li>
 * </ul>
 */
package net.paulhertz.pixelaudio.sampler;
