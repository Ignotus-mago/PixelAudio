/**
 * Example Processing sketches and demonstrations of PixelAudio library functionality. The sketches 
 * are available as .java files in the PixelAudio library {@code net.paulhertz.pixelaudio.example} package and as Processing 
 * sketches in the library release. Both formats share data in the {@code /PixelAudio/examples/examples_data} 
 * directory. If PixelAudio is new to you, I recommend starting with the <b>Basic PixelAudio Features</b>,
 * followed by the <b>Intermediate to Advanced: Tutorial Series</b>. After that, feel free
 * to check out the other examples and the <b>Performance and Production Examples</b>. 
 *
 * <p><b>Basic PixelAudio Features</b></p>
 *
 * <ul>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.LookupTables LookupTables} demonstrates the creation and visualization
 * of lookup tables for spatial indexing and mapping.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.Starter Starter} is a minimal example that demonstrates
 * initializing PixelAudio with a PixelMapGen, mapping colors to the signal path, and displaying the result.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.MultiGenDemo MultiGenDemo} demonstrates switching between multiple
 * PixelMapGen implementations (Hilbert, Moore, DiagonalZigzag) to explore different spatial patterns.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.MultiGenLookupTables MultiGenLookupTables} generates and displays lookup
 * tables for different curve generators, useful for understanding spatial mapping.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.SimpleWaveSynth SimpleWaveSynth} demonstrates setting up a WaveSynth
 * as an animated visual display with gamma adjustment and multiple curve generators (Hilbert, Moore, DiagonalZigzag).</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.SimpleAnimation SimpleAnimation} demonstrates basic animation
 * of a spectral display mapped to a geometric pattern.</li>
 *
 * </ul>
 *
 * <p><b>Intermediate to Advanced: Tutorial Series</b></p>
 *
 * <ul>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.TutorialOne_01_FileIO TutorialOne_01_FileIO} covers file I/O operations
 * including loading and saving audio and image files with PixelAudio.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.TutorialOne_02_Animation TutorialOne_02_Animation} demonstrates
 * pixel-shifting animation within a standard framework for pixel and audio indexing. Introduces
 * the Sampler audio synthesis instrument.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.TutorialOne_03_Drawing TutorialOne_03_Drawing} enables interactive gesture
 * drawing and real-time synthesis, with curve modeling and display control. Introduces the Granular synthesis instrument.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.TutorialOne_04_Network TutorialOne_04_Network} demonstrates network
 * communication capabilities using UDP for remote control and data exchange with external applications.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.TutorialOne_05_GesturePlayground TutorialOne_05_GesturePlayground} provides
 * an interactive playground for gesture-driven sampler and granular synthesis with full parameter control through
 * a GUI.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.TutorialOne_06_WindowBuffer TutorialOne_06_WindowBuffer} demonstrates
 * window buffer management for streaming audio processing and sample-accurate control.</li>
 *
 * </ul>
 *
 * <p><b>Audio and Image I/O</b></p>
 *
 * <ul>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.LoadImageToAudio LoadImageToAudio} loads an image file and converts it
 * to audio using a PixelAudioMapper, demonstrating image-to-signal conversion with playback: an early but
 * still useful example.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.LoadAudioToImage LoadAudioToImage} loads an audio file and converts it
 * to a visual representation using a PixelAudioMapper: an early but still useful example.</li>
 *
 * </ul>
 *
 * <p><b>Introduction to WaveSynth Series</b></p>
 *
 * <ul>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.BigWaveSynth BigWaveSynth} demonstrates basic features of a color organ 
 * that uses additive sine wave synthesis.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.BigWaveSynthAudio BigWaveSynthAudio} extends BigWaveSynth with audio output
 * capabilities, enabling real-time sonification of visual patterns.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.JSONWaveSynth JSONWaveSynth} demonstrates JSON-based serialization and
 * loading of waveform parameters for configuration management.</li>
 *
 * </ul>
 *
 * <p><b>Performance and Production Examples</b></p>
 *
 * <ul>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.Bagatelle Bagatelle} is a comprehensive interactive audio-visual performance
 * tool supporting brush management, session saving/loading, gesture recording, and real-time granular synthesis with
 * extensive preset library support and a graphical user interface.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.WaveSynthEditor WaveSynthEditor} provides a GUI for editing WaveSynth 
 * color organ parameters. It can construct complex spectral and visual compositions that can be output to video. 
 * WaveSynth parameters can be saved to or retrieved from a JSON file.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.WaveSynthSequencer WaveSynthSequencer} demonstrates sequencing with the 
 * WaveSynth color organ, with frequency, rhythmic timing, and image generation.</li>
 * 
 * <li>{@link net.paulhertz.pixelaudio.example.ArgosyMixer ArgosyMixer} is a specialized mixing and synthesis tool
 * for rhythmic pattern making in images and audio. It also provides a GUI with 18 different MultiGen instances 
 * to try out, a menu of patterns, animation controls, and image, audio, and video export.
 * </li>
 *
 * </ul>
 *
 * <p><b>Utility and Demonstration Examples</b></p>
 *
 * <ul>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.TransformPImage TransformPImage} shows image transformation and manipulation
 * techniques compatible with PixelAudio processing.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.TutorialOne_06_WB_Retrofit TutorialOne_06_WB_Retrofit} demonstrates retrofitting
 * existing code to use the WindowBuffer pattern for improved audio handling. Available only in Eclipse version.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.example.PANetworkClientINF PANetworkClientINF} provides a network interface for
 * remote communication with PixelAudio processes.</li>
 *
 * </ul>
 *
 * <p><b>Companion Files</b></p>
 *
 * <p>Some examples include Max/MSP support files for network integration:</p>
 * <ul>
 *   <li>{@code UDP_Handler.maxpat} - Max/MSP patch for UDP message handling</li>
 *   <li>{@code simpleAudioIO.maxpat} - Max/MSP patch for basic audio I/O</li>
 * </ul>
 *
 * <p><b>Development Notes</b></p>
 * <p>Most examples extend Processing's PApplet and follow the standard setup/draw pattern.
 * Many support interactive keyboard control for real-time parameter manipulation.
 * The tutorial series (TutorialOne_*) provides progressive lessons in increasing complexity.</p>
 */
package net.paulhertz.pixelaudio.example;
