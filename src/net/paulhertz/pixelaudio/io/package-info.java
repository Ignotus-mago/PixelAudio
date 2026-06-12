/**
 * JSON-based persistence for PixelAudio data structures supported by file i/o classes.
 *
 * <p><b>Overview</b></p>
 * <p>This package implements serialization and deserialization of core PixelAudio objects to JSON format,
 * enabling saving and loading of gestures, granular synthesis configurations, and complete audio brush sessions.
 * The format is designed for the Bagatelle example sketch and supports the AudioBrush session paradigm.</p>
 *
 * <ul>
 *
 * <li>{@link net.paulhertz.pixelaudio.io.PACurveMakerIO PACurveMakerIO} provides JSON read/write support for PACurveMaker
 * gesture data with optional metadata (id, name, description, notes, style, and linked config path).</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.io.GestureGranularConfigIO GestureGranularConfigIO} provides JSON read/write support
 * for GestureGranularConfig.Builder with full parameter serialization including timing, hopping, warping, grain, and ADSR parameters.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.io.AudioBrushSessionIO AudioBrushSessionIO} implements a session manifest writer/reader
 * for brush sets using generic adapter/factory patterns, enabling flexible brush collection management with support for
 * multiple instrument types and linked gesture/config files.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.io.AudioBrushSessionLoader AudioBrushSessionLoader} reads AudioBrush session data from JSON files,
 * with automatic file type detection (gesture, config, or session) and support for loading single files or complete sessions
 * with linked resources.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.io.AudioBrushLibraryLoader AudioBrushLibraryLoader} reads AudioBrush library JSON format
 * and loads directories of gesture/config files into a collection of brush objects with error tracking.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.io.AudioBrushFileNamer AudioBrushFileNamer} generates JSON filenames for AudioBrush
 * "session" format with gesture and audio synth settings, supporting normalized base names and linked file references.</li>
 *
 * </ul>
 *
 * <p><b>JSON Format Conventions</b></p>
 * <p>Each JSON file includes a header with format identifier, version, and type. Files follow these naming patterns:</p>
 * <ul>
 *   <li><code>gesture_001_gesture.json</code> - gesture data (PACurveMaker)</li>
 *   <li><code>gesture_001_config.json</code> - configuration data (GestureGranularConfig.Builder)</li>
 *   <li><code>rehearsalSet_session.json</code> - session manifest with brush collection</li>
 * </ul>
 */
package net.paulhertz.pixelaudio.io;
