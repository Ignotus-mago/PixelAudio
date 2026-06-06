/**
 * Package <code>net.paulhertz.pixelaudio.granular</code> implements granular synthesis audio processing
 * for the PixelAudio library.
 *
 * <p>The modern granular synthesis engine uses a calling chain:
 * PAGranularInstrumentDirector → PAGranularInstrument → PAGranularSampler → PAGranularVoice,
 * with PABurstGranularSource handling the complexities of granular synthesis sample-by-sample.</p>
 *
 * <ul>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.PASource PASource} is a PAFloatSource with a pitch policy hint,
 * designed for use with the granular synthesis engine.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.PAFloatSource PAFloatSource} is a minimal, library-agnostic
 * audio source abstraction for PixelAudio voices.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.PABurstGranularSource PABurstGranularSource} provides windowed
 * granular synthesis with normalization, core class in the PAGranularInstrumentDirector granular synthesis
 * processing chain.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.PABufferBackedSource PABufferBackedSource} wraps a Minim 
 * MultiChannelBuffer for buffer-backed sample-based audio sources.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.PAGranularInstrument PAGranularInstrument} is a high-level 
 * granular instrument wrapper that manages PAGranularSampler, voices, pan, gain, and ADSR envelope.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.PAGranularSampler PAGranularSampler} manages a sampler 
 * for granular synthesis with voice pooling and sample-accurate scheduling.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.PAGranularVoice PAGranularVoice} handles individual 
 * granular voice playback with ADSR envelope, amplitude, pan, and optional looping.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.PAGranularInstrumentDirector PAGranularInstrumentDirector} 
 * coordinates the complete granular synthesis engine, managing scheduling and event processing.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.GestureGranularParams GestureGranularParams} provides immutable 
 * core parameters for gesture-driven granular playback.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.GestureGranularConfig GestureGranularConfig} stores core parameters 
 * for both granular and sample synthesis engine events, used extensively in PixelAudio examples.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.GestureEventParams GestureEventParams} stores schedule-aligned 
 * per-event parameters for gesture playback with buffer sample indices and optional pan, gain, and pitch modifiers.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.WindowCache WindowCache} provides a thread-safe cache for 
 * windowFunction.generateCurve(length) results, used by the PixelAudio granular synthesis engine.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.OverlapUtil OverlapUtil} is a small helper for computing overlap 
 * between a span and a block, useful for granular streaming and span-based scheduling at audio rate.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.GranularSettings GranularSettings} provides common configuration 
 * for granular processing. @Deprecated Candidate for removal.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.BasicIndexGranularSource BasicIndexGranularSource} is a simple 
 * granular source that reads from a mono float[] source buffer, uses a list of grain start indices, 
 * plays grains in order with fixed hop and grain length, and sums all active grains. @Deprecated</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.IndexGranularSource IndexGranularSource} is a standard 
 * linear granular source that reads from a mono float[] buffer, starts at a given sample index, uses 
 * fixed grain length and hop, and applies a window and equal-power pan. 
 * @Deprecated Candidate for removal; may still prove useful.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.PathGranularSource PathGranularSource} reads from a 
 * mono float[] source buffer, uses a GranularPath for grain placement, applies per-grain gain and pan, 
 * and supports fixed hop or per-grain timing. @Deprecated Candidate for removal.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.GranularPath GranularPath} is a fixed sequence of GrainSpecs 
 * that describe how to traverse a source buffer for linear or non-linear paths. @Deprecated</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.GranularPaths GranularPaths} provides deprecated utilities for 
 * generating fixed-hop granular paths. @Deprecated</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.GestureGranularTexture GestureGranularTexture} stores essentials 
 * for gesture-driven granular playback with builder pattern support. @Deprecated</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.GestureGranularRenderer GestureGranularRenderer} renders a 
 * GestureSchedule into audio by triggering granular bursts at each scheduled event time. @Deprecated Candidate for removal.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.GranularUGen GranularUGen} is a Minim UGen that uses 
 * BasicIndexGranularSource to generate audio in blocks and feeds it sample-by-sample. @Deprecated</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.granular.LegacyGranularPathBuilder LegacyGranularPathBuilder} builds 
 * GranularPath objects from various input sources and formats.</li>
 *
 * </ul>
 *
 */
package net.paulhertz.pixelaudio.granular;