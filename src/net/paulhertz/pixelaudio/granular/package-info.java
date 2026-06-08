/**
 * Granular synthesis classes for PixelAudio.
 *
 * <p>The modern granular engine follows the chain
 * {@code PAGranularInstrumentDirector -> PAGranularInstrument ->
 * PAGranularSampler -> PAGranularVoice}, with {@code PASource}
 * implementations providing sample data and grain behavior. 
 * Example code makes exclusive use of {@code PABurstGranularSource}
 * as the most versatile {@code PASource}.</p>
 *
 * <p><b>Modern granular engine</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.granular.PAGranularInstrumentDirector PAGranularInstrumentDirector}
 *   coordinates gesture playback, scheduling, event parameters, and granular source creation.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.PAGranularInstrument PAGranularInstrument}
 *   provides the high-level instrument API for pan, gain, envelopes, looping, and scheduled starts.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.PAGranularSampler PAGranularSampler}
 *   manages granular voices, voice pooling, and sample-accurate scheduling.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.PAGranularVoice PAGranularVoice}
 *   renders individual granular voices with envelope, pan, gain, and optional looping.</li>
 * </ul>
 *
 * <p><b>Sources and audio data</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.granular.PASource PASource}
 *   extends PAFloatSource with pitch-policy and buffer-access support.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.PAFloatSource PAFloatSource}
 *   defines the minimal audio source abstraction used by granular voices.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.PABurstGranularSource PABurstGranularSource}
 *   provides windowed burst-grain synthesis with overlap-add normalization.</li>
 * </ul>
 *
 * <p><b>Gesture configuration and event data</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.granular.GestureGranularConfig GestureGranularConfig}
 *   stores editable gesture, timing, and synthesis settings, with builder support.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.GestureGranularParams GestureGranularParams}
 *   provides immutable granular playback parameters used by the engine.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.GestureEventParams GestureEventParams}
 *   stores schedule-aligned sample indices and optional per-event pan, gain, and pitch data.</li>
 * </ul>
 *
 * <p><b>Utilities</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.granular.WindowCache WindowCache}
 *   caches generated window curves for granular playback.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.OverlapUtil OverlapUtil}
 *   computes block/span overlaps for sample-accurate audio processing.</li>
 * </ul>
 *
 * <p><b>Legacy and deprecated classes</b></p>
 * <p>The following classes are retained for this release to preserve source
 * compatibility and document earlier granular synthesis experiments. They are
 * deprecated and may be removed in the next release unless they provide reusable
 * legacy code.</p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.granular.PABufferBackedSource PABufferBackedSource}
 *   is a deprecated class retained as an experimental source, not used in library or example code.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.BasicIndexGranularSource BasicIndexGranularSource}
 *   is a deprecated indexed granular source.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.IndexGranularSource IndexGranularSource}
 *   is a deprecated linear granular source.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.PathGranularSource PathGranularSource}
 *   is a deprecated path-based granular source.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.GranularPath GranularPath} and
 *   {@link net.paulhertz.pixelaudio.granular.GranularPaths GranularPaths}
 *   describe and generate older granular paths.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.GestureGranularTexture GestureGranularTexture}
 *   stores older gesture-driven granular texture settings.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.GestureGranularRenderer GestureGranularRenderer}
 *   renders GestureSchedule events using the earlier burst-rendering model.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.GranularUGen GranularUGen}
 *   is a deprecated Minim UGen for block-based granular playback.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.GranularSettings GranularSettings}
 *   stores older granular configuration values.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.LegacyGranularPathBuilder LegacyGranularPathBuilder}
 *   builds older GranularPath objects.</li>
 *   <li>{@link net.paulhertz.pixelaudio.granular.MCBufferSource MCBufferSource}
 *   is a deprecated MultiChannelBuffer-backed PASource candidate for removal.</li>
 * </ul>
 */
package net.paulhertz.pixelaudio.granular;