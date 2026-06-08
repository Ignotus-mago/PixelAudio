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

package net.paulhertz.pixelaudio.sampler;

/**
 * PitchPolicy determines how PASamplerInstrument / PASamplerVoice should interpret
 * their pitch parameter in combination with a given PASource.
 */
public enum PitchPolicy {

    /**
     * Instrument pitch maps to playback rate (classic sample playback).
     * Source is considered time-neutral.
     */
    INSTRUMENT_RATE,

    /**
     * Source (e.g., granular engine) controls time/pitch.
     * Instrument pitch should be ignored for this source.
     */
    SOURCE_GRANULAR,

    /**
     * Both instrument and source contribute to pitch/time.
     * Advanced / experimental; can create complex artifacts.
     */
    COMBINED
}
