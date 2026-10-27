import net.paulhertz.pixelaudio.granular.*;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig.*;
import net.paulhertz.pixelaudio.io.*;
import net.paulhertz.pixelaudio.sampler.*;


/*
 * DONE a release version where two different preset lists and performance cues can be swapped.
 * TODO playback with specified gap DONE
 */

/**
 * Experimental real time performance application, with an editing GUI, presets, and JSON-format
 * files for persistent brushstroke and audio configuration data. Used in performance at the
 * Outside the Box New Music Festival at Southern Illinois University, Carbondale, March 2026.
 * We played Christopher Walczak's composition "Abstract Jailbreak", one of a series of
 * "Bagatelles" we are collaborating on, and my composition "DEADBODYWORKFLOW".
 * <p>
 * The presets and performance cues in this version of the Bagatelle sketch are set up for
 * "Abstract Jailbreak". If you change {@code pMode} to {@code PerformanceMode.DEADBODYWORKFLOW},
 * this sketch will run the presets and cues for "DEADBODYWORKFLOW". You can toggle {@code pMode}
 * at runtime with the '%' key--this is new feature, YMMV.
 * </p>
 * <figure>
 * <img src="doc-files/bagatelle.png" alt="DeadBodyWorkFlow Screen" width="768" height="526"/>
 * <figcaption>Bagatelle screen for DEADBODYWORKFLOW performance, with brush presets loaded.</figcaption>
 * </figure>
 * <p>
 * Bagatelle is an experiment. I have tried to document it reasonably well, but some features are bound to
 * appear opaque or mysterious. I think most of the features will reveal themselves with experimentation.
 * You may even find that you can write your own presets and use them for your performances. That
 * would be an ideal outcome, to my mind.
 * </p>
 * <DIV>
 * <h2>QUICK START</h2>
 * <ol>
 * <li>Launch the sketch. A display window and a palette of Graphical User Interface (GUI) controls appears.
 * The display window has an audio file preloaded. The grayscale values in the image are transcoded audio
 * samples. An overlaid rainbow spectrum traces the Signal Path, the mapping of the audio signal to the image
 * pixels created by the PixelMapGen {@code multigen} and managed by the PixelAudioMapper {@code mapper}.
 * The Signal Path starts in the upper left corner and ends in the lower right corner.
 * <br><br>
 * Bagatelle is set up for a performance of "Abstract Jailbreak," a musical work by Christopher Walczak.
 * Abstract Jailbreak makes use of the Performance Presets built in to the application. The presets
 * can be triggered with the number keys. At this point you could press '1' to load the first preset.
 * For more information, see the "PRESET LIST" in the variables section of the Bagatelle tab, and the
 * various methods in the Performance tab. There are five presets for Abstract Jailbreak. They control
 * audio synthesis parameters and brush drawing style. More than one can be loaded at one time. To clear
 * presets from the preset stack, press '0'. <br><br></li>
 *
 * <li>Drawing is already turned on, so go ahead and drag the mouse to draw a line. As in TutorialOne_03_Drawing,
 * a brushstroke appears when you release the mouse. TutorialOne_03_Drawing gave you limited control over
 * the attributes of a brushstroke and its associated audio parameters. In GesturePlayground, you can
 * control nearly all the available parameters with the control palette. Building on GesturePlayground,
 * Bagatelle provides a framework for live performance. <br><br></li>
 *
 * <li>You can probably use Bagatelle "right out of the box" by just supplying your own audio and image files
 * and open your files with the 'o' key command. If you want to use your own files with Bagatelle's
 * PerformancePreset feature, I suggest you look closely at the existing presets (for Abstract Jailbreak)
 * and at the {@code runPerformanceCue(...)} method in the Performance Methods section of this app.
 * There are three things you need to modify to use your own performance presets:<br>
 *   <ol>
 *   <li>Create your own PerformancePreset list by editing the existing one.</li>
 *   <li>Set the variable {@code performanceBasePath} to point to the directory where you store your performance files</li>
 *   <li>Edit the entries in {@code runPerformanceCue(...)} to access your own files</li>
 *   </ol>
 * <br></li>
 * <li>At the top of the control palette, you'll find Path Source radio buttons and sliders for setting
 * the geometry of the brush curve. When the curve is set to Reduced Points or Curve Points, the epsilon
 * slider will allow you to visualize changes in the curve. For the curve points representation of the
 * curve, theCurve Points slider will add or subtract points.<br><br></li>
 *
 * <li>The control palette displays knobs for the type of audio synthesis instrument you have selected.
 * Press the 't' key to change the instrument. The control palette will reflect the changes. The
 * control palette provides three play modes: one for editing granular synthesis parameters, another
 * for the sampler synthesizer, and a "play only" mode where you can play both instruments but
 * don't have editing enabled.<br><br></li>
 *
 * <li>The controls for the Sampler are fairly simple. You can change the number of points in the curve
 * with the geometry controls. You can also change the duration of the gesture and the number of
 * points in it with the Resample and Duration sliders. Resampled points are not drawn on-screen;
 * they appear when the brush is activated. Finally, there's a Sampler Envelope menu that will change
 * the ADSR envelope of each sampler event point. The Duration (ms) field below it sets the duration
 * of a single click event. When {@code isAdjustEnvelope} is true (the default, set with the
 * 'E' key command), brush event durations are calculated to fit the curve duration. When it is
 * false, the Duration (ms) field controls both brush and click event durations. <br><br></li>
 *
 * <li>The Granular Synth has all the controls of the Sampler synth except for the envelopes, plus
 * many controls for granular synthesis:
 *   <ol>
 *   <li>The Hop Mode radio buttons determine if the duration of the granular event is determined
 *   by the gesture timing data in the brushstroke's PACurveMaker instance, or by the Grain
 *   Length and Hop Length sliders.</li>
 *   <li>Burst Count sets the number of linear grains at each event point. Its effect is to expand
 *   the sound of the grain.</li>
 *   <li>Grain Length and Hop Length sliders control the spacing of the grains. Hop Length is only
 *   used for Fixed Hop Mode. Grain and Hop durations are in milliseconds.</li>
 *   <li>The Warp radio buttons and slider control non-linear timing changes to the gesture.</li>
 *   </ol><br><br></li>
 * <li>There are many key commands too, including the 'o' command to load a new audio files. Some
 * commands are particularly useful with granular synthesis:
 *   <ol>
 *   <li>The 'q' command key will calculate the optimal number of grains in a gesture (usually in
 *   GESTURE Path Mode) and update the control palette. This can provide smooth granular synthesis
 *   even as it preserves the timing characteristic of the gesture. </li>
 *   <li>The 'u' command will apply grain optimization to every new granular brush.</li>
 *   <li>the 'd' command will toggle playing a new brush immediately upon mouse release.</li>
 *   <li>The 'D' command will toggle playing audio while you draw.</li>
 *   <li>The 'c' command key will print configuration data to the console.</li>
 *   <li>The 'x' command key deletes the brush you are hovering over, if it is editable.</li>
 *   <li>The 'z' command key swaps the instrument type of the brush you are hovering over and changes
 *   edit mode to match.</li>
 *   </ol><br><br></li>
 * </ol>
 *
 * <h2>About Bagatelle</h2>
 * <b>Bagatelle</b> uses a GUI to view and manage the properties of the {@code AudioBrush} subclasses
 * {@code GranularBrush} and {@code SamplerBrush}, the {@code GestureSchedule} class,
 * and the Sampler and Granular audio synthesis instruments {@code PASamplerInstrumentPool} and
 * {@code PAGranularInstrumentDirector}.
 * <p>An AudioBrush combines a {@code PACurveMaker} and a {@code GestureGranularConfig.Builder}.
 * PACurveMaker models <i>gestures</i>, one of the core concepts of PixelAudio. In its simplest encoded
 * form, the {@code PAGesture} interface, a gesture consists of an array of points and an array of
 * times. The times array and the points array must be the same size, because the times array records the
 * times when something as-yet-unspecified will happen at the corresponding point in the points array. In my
 * demos for PixelAudio, what happens at a point is typically an audio event and an animation event. The
 * sound happens at the point because points in PixelAudio map onto locations in the sound buffer. Mapping
 * of bitmap locations onto audio buffer indices is another core concept of PixelAudio. Gestures over the 2D
 * space of an image become paths through audio buffers.
 * </p><p>The audio buffer is traversed either by a granular
 * synthesis engine or by a sampling synthesizer. For the granular synth, a gesture corresponds to a
 * non-linear traversal of an audio buffer, potentially as a continuous sequence of overlapping grains with
 * a single envelope. The sampling synthesizer treats each point as a discrete event with its own envelope.
 * Depending on how gestures and schedules are structured, the two synthesizers can sound very similar, but
 * there are possibilities in each that the other cannot realize. As you might expect, {@code GranularBrush}
 * implements granular synth events and {@code SamplerBrush} implements sampler synth events. Both rely on
 * {@code PACUrveMaker} which, in addition to capturing the raw gesture of drawing a line, provides methods to
 * reduce points / times and create Bezier paths. {@code PACurveMaker} data can also be modified by changing
 * duration, interpolating samples, or non-linear time warping. DeadBodyWorkFlow uses
 * {@code GestureScheduleBuilder} to interpolate and warp time and point lists.
 * </p><p>
 * The parameters for gesture modeling, granular and sampling synthesis, time and sample interpolation, and
 * audio events are modeled in the GUI, which uses {@code GestureGranularConfig.Builder gConfig} to
 * track its current state. A GestureGranularConfig instance is associated with each AudioBrush. When you
 * click on an AudioBrush and activate it, its configuration data is loaded to the GUI and you can edit it.
 * It will be saved to the brush when you select another brush or change the edit mode. When a brush is
 * activated with a click, the schedule is built from its PACurveMaker and GestureGranularConfig.Builder
 * instance variables: </p>
 *     <pre>GestureSchedule schedule = scheduleBuilder.build(gb.curve(), cfg.build(), audioOut.sampleRate());</pre>
 *
 * <p>The calling chain for a GranularBrush:<br>
 * {@code mouseClicked()} calls {@code scheduleGranularBrushClick(gb, x, y);}.<br>
 *
 * In {@code scheduleGranularBrushClick(...)} we get a reference to the audio buffer {@code buf}
 * and then use the PACurveMaker object {@code gb.curve()} and {@code gb.snapshot()} to build a
 * {@code GestureSchedule}, {@code sched}. <br>
 *
 * {@code sched} gets timing and location information for the gesture from {@code gb.curve()} and
 * modifies it with the settings from the control palette which are stored {@code gb.snapshot()}. <br>
 *
 * We port the granular synthesis parameters from the brush to a {@code GestureGranularParams} object, and then call
 * {@code playGranularGesture(buf, sched, gParams)} to play the granular synth. We also call
 * {@code storeGranularCurveTL(...)}, which sets up UI animation events to track the grains.<br>
 *
 * Parameter {@code buf} is the audio signal that is the source of our grains, parameter {@code sched} provides
 * the points and times for grains and parameter {@code params} provides the core parameters for granular synthesis.<br>
 *
 * {@code playGranularGesture()} builds arrays for buffer position and pan for each individual grain and then calls
 * {@code gDir.playGestureNow(buf, sched, params, startIndices, panPerGrain)} to play the PAGranularInstrumentDirector
 * granular synth. The 'p' command key can toggle per-grain pitch jitter, which calls {@code playGestureNow()}in a slightly
 * different way. See {@code playGranularGesture()} for details.<br>
 *
 * {@code PAGranularInstrumentDirector} its own calling chain that goes all the way down to the individual sample level
 * using the Minim library's UGen interface. If you just want to play music, you'll probably never have to deal with the
 * hierarchy of classes directly, but comments {@code PAGranularInstrumentDirector} may be useful. <br>
 * </p>
 *
 * <p>Part of the calling chain for a SamplerBrush:<br>
 * {@code mouseClicked()} calls {@code scheduleSamplerBrushClick(sb, x, y)}.<br>
 *
 * In {@code scheduleSamplerBrushClick()} we get array of points on the curve with {@code getPathPoints(sb)} and then
 * use {@code sb.snapshot()} and {@code scheduleBuilder.build()} to build a {@code GestureSchedule} <br>
 *
 * Finally, we pass the schedule, snapshot and start time to {@code storeSamplerBrushEvents()}, an array of
 * {@code SamplerBrushEvent} objects that is checked at every pass through the {@code draw()} loop and posts
 * both Sampler instrument triggers and animation events. Unlike the Granular instrument, which requires very accurate
 * timing, the Sampler synth requires less precision, so we can handle it through the UI frames. Sample-accurate
 * timing is a topic for another as-yet-unreleased example sketch. <br>
 *
 * The {@code runSamplerBrushEvents()} method executes the UI brushstroke animation and the Sampler audio events.
 * Sampler events all pass through {@code pool.playSample(samplePos, samplelen, amplitude, env, pitch, pan)}.</p>
 *
 * <p>See Issue #45 <a href="https://github.com/Ignotus-mago/PixelAudio/issues/45">Noise in Sampler Instruments</a>
 * for information about suggested {@code poolSize} and {@code maxVoices} usage with Sampler synth.</p>
 *
 * <pre>{@code
 * ----- Audio Gain -----
 * Press UP ARROW to increase audio output volume by 1.0 or 3.0 dB (+shift).
 * Press DOWN ARROW to decrease audio output volume by 1.0 or 3.0 dB (+shift).
 * Press RIGHT ARROW to increase current instrument gain by 3.0 dB.
 * Press LEFT ARROW to decrease current instrument gain by 3.0 dB.
 * Press '`' to fade out all instruments.
 * ----- Presets and Cues -----
 * Keys 1 through 9 are reserved for triggering Performance Presets 1-9, '0' will clear all presets.
 * ----- Drawing, Audio Settings, Playback -----
 * Press TAB to set brush to active, if cursor is over a brush.
 * Press ' ' to (spacebar) trigger a brush if we're hovering over a brush, otherwise trigger a point event.
 * Press 'm' to toggle doMagicClick, play brushstroke in same rectangle as mouse on click or spacebar.
 * Press 'a' to toggle animation.
 * Press 't' to switch between Granular, Sampler, and Play Only modes.
 * Press 'z' to change the drawing mode of the hover brush.
 * Press 'q' to automatically set an active GRANULAR brush to have an optimized number of samples.
 * Press 'u' to toggle granular sample optimization: same as the 'q' command, applied on brushstroke creation.
 * Press 'd' to toggle doPlayOnNewBrush: if true, audio plays when a new brush is created.
 * Press 'D' to toggle doPlayOnDraw: if true, drawing triggers audio while you drag the mouse.
 * Press 'p' to jitter the pitch of granular gestures and point events.
 * Press 'P' to toggle useLinearPointTraversal (granular click event behavior).
 * Press 'k' to apply the hue and saturation in the colors array to mapImage (not to baseImage).
 * Press 'K' to apply hue and saturation in colors to baseImage and mapImage.
 * Press '*' to turn automatic audio path coloring on file load on and off.
 * Press 'r' to reset instrument configuration to defaults in GUI.
 * Press 'c' or 'C' to print the current configuration status to the console.
 * ----- File IO and Mapping -----
 * Press 'f' or 'F' to open a folder with JSON brush data and load all files.
 * Press 'j' to save the active brush curve and config to JSON files.
 * Press 'J' to save all brushes curve and config to JSON Session file.
 * Press 'o' to open an audio file, image file, or JSON file.
 * Press 'b' or 'B' to toggle loading data to both image and audio buffers when you open a file.
 * Press 'w' to write the display image to the audio buffer.
 * Press 'W' to write the audio buffer to the display image.
 * ----- Audio Mix Dynamics -----
 * Press 'n' to set noise reduction policy for Sampler instrument audio mix.
 * Press 'E' to toggle calculating envelope duration from brush points or setting it to envDurationMs.
 * Press 'g' to toggle use of dynamics in gainCurve with gesture .
 * ----- Special FX -----
 * Press 'l' to loop the hovered brush 4 times.
 * Press 'L' to run an infinite loop on the hovered brush.
 * Press ';' to stop loop for the hovered brush.
 * Press ':' to stop all loops.
 * Press 'y' to toggle transform animation test.
 * Press 'Y' to freeze / unfreeze brush geometric transform animation.
 * Press 'R' to reset transform of active brush if it has a transform.
 * Press 'G' to create a beatBrush.
 * Press '.' to turn random raindrops audio events on or off.
 * Press '`' to fade out all instruments.
 * Press '%' to switch performance presets and cue handlers.
 * Press '&' to clear events in granular and sample synths.
 * ----- Brush Deletion -----
 * Press 'x' to delete the current active brush shape or the oldest brush shape.
 * Press 'X' to delete the most recent brush shape.
 * Press '≈' to option-x on MacOS keyboard, clear all brushes.
 * ----- Network -----
 * Press '=' to send UDP message to Max (simpleAudioIO.maxpat): reverb ON.
 * Press '-' to send UDP message to Max (simpleAudioIO.maxpat): reverb OFF.
 * Press '+' to send UDP message to Max (simpleAudioIO.maxpat): unused.
 * Press '_' to send UDP message to Max (simpleAudioIO.maxpat): unused.
 * Press 'v' to send UDP message to Max (simpleAudioIO.maxpat): small reverb settings.
 * Press 'V' to send UDP message to Max (simpleAudioIO.maxpat): big reverb settings.
 * ----- Help -----
 * Press 'h' or 'H' to show help message.
 * }</pre>
 *
 *
 * <h2>MacOS AUDIO TO MAX SETUP</h2>
 *
 * In MacOS:<br>
 *   Ignore Sound.inputDevice() and Sound.outputDevice(), use the System Settings instead.<br>
 *   Set Output to BlackHole 16ch<br>
 *   Set Input to your external audio hardware, for an external mic: mine is Volt2<br>
 *   <br>
 * Then in Max Audio Status control panel:<br>
 *   Set Input Device to BlackHole 16ch<br>
 *   Set Output Device to your external audio hardware, e.g. Volt2<br>
 *   Create a patcher that gets signals from an adc~, route the adc~ to some sort of effects
 *   and out to a dac~.<br>
 *
 * </DIV>
 *
 */
