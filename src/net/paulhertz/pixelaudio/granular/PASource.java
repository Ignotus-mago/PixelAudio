/*
 *  Copyright (c) 2024 - 2025 by Paul Hertz <ignotus@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.paulhertz.pixelaudio.granular;

import ddf.minim.MultiChannelBuffer;
import ddf.minim.analysis.WindowFunction;
import net.paulhertz.pixelaudio.sampler.PitchPolicy;

/**
 * PASource
 *
 * A PAFloatSource with an additional pitch policy hint.
 * 
 * Designed for used with granular synthesis engine, now implemented in the PixelAudio library with 
 * calling chain: PAGranularInstrumentDirector -> PAGranularInstrument -> PAGranularSampler -> PAGranularVoice,  
 * where PABurstGranularSource is PASource that handles the complexities of the granular synthesis sample by sample. 
 * 
 * Implemented by {@link PABurstGranularSource}.
 *
 */
public interface PASource extends PAFloatSource {

    /**
     * Pitch policy hint for the instrument:
     * should the instrument apply its pitch (playback rate) on top of this source?
     */
    default PitchPolicy pitchPolicy() {
        return PitchPolicy.INSTRUMENT_RATE;
    }

    /**
     * Optional seek/rewind hook, mainly for transport or "note-on" start time.
     * Default implementation does nothing.
     *
     * @param absoluteSample absolute sample index to seek to.
     */
    default void seekTo(long absoluteSample) {
        // no-op by default
    }
    
    default void setGrainWindow(WindowFunction wf, int grainLenSamples) { /* no-op */ }

    /**
     * Optional access to an underlying MultiChannelBuffer, if this source is
     * fundamentally buffer-backed (e.g., plain sample playback or a rendered
     * granular "tape").
     *
     * Implementations that are not backed by a fixed buffer (true streams,
     * procedural sources, etc.) should simply return null.
     */
    default MultiChannelBuffer getMultiChannelBuffer() {
        return null;
    }
}
