/**
 * Package <code>net.paulhertz.pixelaudio.schedule</code> provides sample-accurate event scheduling,
 * gesture-based time management, and audio utility functions for the PixelAudio library.
 *
 * <p><b>Core Scheduling</b></p>
 * <p>The AudioScheduler is a generic event scheduler supporting both point events (one-shot at a sample time)
 * and span events (active over an interval). It provides sample-accurate callback delivery with thread-safe
 * enqueuing and block-based processing on the audio thread.</p>
 *
 * <ul>
 *
 * <li>{@link net.paulhertz.pixelaudio.schedule.AudioScheduler AudioScheduler} provides a sample-accurate scheduler for one-shot
 * point events and duration-based span events, with support for late event policies, block-wise processing, and thread-safe scheduling.</li>
 *
 * </ul>
 *
 * <p><b>Gesture and Time Management</b></p>
 *
 * <ul>
 *
 * <li>{@link net.paulhertz.pixelaudio.schedule.GestureSchedule GestureSchedule} is a data container for a PAGesture with immutable points,
 * times in milliseconds, and optional start time, serving as the base structure for gesture-driven playback.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.schedule.GestureScheduleBuilder GestureScheduleBuilder} builds schedules for classes that implement
 * PAGesture or use PAGesture implementers, supporting resampling, duration scaling, time warping, and hop mode conversion for flexible
 * gesture playback control.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.schedule.TimedLocation TimedLocation} is used to schedule or track events that take place at specific
 * coordinate locations, with support for duration and staleness tracking.</li>
 *
 * <li>{@link net.paulhertz.pixelaudio.schedule.GesturePerformer GesturePerformer} (deprecated) is a placeholder interface for future
 * gesture performance abstractions.</li>
 *
 * </ul>
 *
 * <p><b>Audio Utilities</b></p>
 * <p>AudioUtility provides a comprehensive collection of audio processing functions including time/frequency conversions,
 * gain/decibel conversions, signal level computation, normalization, resampling, and file I/O operations.</p>
 *
 * <ul>
 *
 * <li>{@link net.paulhertz.pixelaudio.schedule.AudioUtility AudioUtility} provides utility conversions for audio including:
 * <ul>
 *   <li>Time conversions: milliseconds ↔ samples, seconds ↔ samples</li>
 *   <li>Gain conversions: dB ↔ linear amplitude</li>
 *   <li>Pitch/frequency: semitones ↔ frequency ratio, MIDI key number ↔ frequency</li>
 *   <li>Signal analysis: peak, RMS, and normalization (by peak, by RMS, or with ceiling)</li>
 *   <li>Resampling: mono and multi-channel buffer resampling with linear interpolation</li>
 *   <li>File I/O: save audio to 16-bit or 32-bit PCM WAV format (mono or stereo)</li>
 * </ul>
 * </li>
 *
 * </ul>
 */
package net.paulhertz.pixelaudio.schedule;
