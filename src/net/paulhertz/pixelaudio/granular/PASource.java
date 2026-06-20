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
 * PixelAudio audio source interface used by the granular playback engine.
 *
 * <p>{@code PASource} extends {@link PAFloatSource} with PixelAudio-specific hooks that voices
 * and instruments may use when preparing playback: pitch-policy hints, optional seeking,
 * optional grain-window configuration, and optional access to an underlying Minim
 * {@link MultiChannelBuffer}.</p>
 *
 * <p>The current granular synthesis chain is:</p>
 * <p>{@link PAGranularInstrumentDirector} -&gt; {@link PAGranularInstrument} -&gt;
 * {@link PAGranularSampler} -&gt; {@link PAGranularVoice} -&gt; {@code PASource}</p>
 *
 * <p>{@link PABurstGranularSource} is the primary implementation used by the granular
 * instrument director. Other implementations may represent plain buffers, streams, or
 * procedural sources as long as they satisfy the {@link PAFloatSource} rendering contract.</p>
 *
 * @see PAFloatSource
 * @see PABurstGranularSource
 * @see <a href="https://code.compartmental.net/minim/javadoc/" target="_blank">Minim documentation</a>
 */
public interface PASource extends PAFloatSource {

    /**
     * Returns a hint describing how an instrument should combine its pitch control with this source.
     *
     * <p>The default, {@link PitchPolicy#INSTRUMENT_RATE}, means the instrument may apply its
     * playback-rate pitch control on top of the source. Sources that already control pitch or
     * time internally may override this method.</p>
     *
     * @return pitch policy for this source
     */
    default PitchPolicy pitchPolicy() {
        return PitchPolicy.INSTRUMENT_RATE;
    }

    /**
     * Optional seek or rewind hook.
     *
     * <p>Voices call this method when a source is activated so the source can establish its
     * playback origin. The default implementation does nothing, which is appropriate for
     * sources that do not maintain seekable state.</p>
     *
     * @param absoluteSample absolute sample index to seek to
     */
    default void seekTo(long absoluteSample) {
        // no-op by default
    }
    
    /**
     * Optionally sets a grain amplitude window for sources that render windowed grains.
     *
     * <p>The default implementation does nothing. Implementations that support external grain
     * windows may cache or precompute a window curve for the supplied grain length.
     * The Minim library supplies several windows and a model for creating your own.</p>
     *
     * @param wf window function to apply, or null to keep the source default
     * @param grainLenSamples grain length in samples
     * 
     * @see <a href="https://code.compartmental.net/minim/javadoc/" target="_blank">Minim documentation</a>
     */
    default void setGrainWindow(WindowFunction wf, int grainLenSamples) { /* no-op */ }

    /**
     * Returns the underlying Minim buffer when this source is backed by one.
     *
     * <p>Implementations that are not backed by a fixed {@link MultiChannelBuffer}, such as
     * true streams or procedural sources, should return null.</p>
     *
     * @return the backing buffer, or null if no fixed buffer is available
     */
    default MultiChannelBuffer getMultiChannelBuffer() {
        return null;
    }
}
