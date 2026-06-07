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

package net.paulhertz.pixelaudio.curves;

/**
 * PAGesture provides a definitional interface for "gestures":
 * 
 *   1) a list of points and 
 *   2) a list of time offsets where
 *   3) both lists have the same cardinality and 
 *   4) time offsets are monotonically non-dereasing. In addition, 
 *   5) the time list is expected (but not required) to start with a first element 0.  
 *   
 * Absolute time of gesture creation can be returned from in getStartTimeMs(), 
 * possibly as millis from application start.
 * 
 * Gestures are often captured from GUI interaction, is is the case with PACurveMaker. 
 * PAGesture opens the way to use many different sources for generating gestures. 
 * "Gesture" is a core concept in the PixelAudio library, implemented by PACurveMaker, in particular.
 * 
 * @see net.paulhertz.pixelaudio.schedule.GestureSchedule
 * 
 */
public interface PAGesture {
	/** Dense gesture points. */
	java.util.List<processing.core.PVector> getAllPoints();

	/** Time offsets in ms, same length as getAllPoints(), first element typically 0. */
	float[] getTimeOffsetsMs();
	
	/** Absolute start time (e.g. millis() when gesture began) */
	long getStartTimeMs();

	/** Convenience: a schedule over the dense gesture. */
	default net.paulhertz.pixelaudio.schedule.GestureSchedule getAllPointsSchedule() {
		return new net.paulhertz.pixelaudio.schedule.GestureSchedule(getAllPoints(), getTimeOffsetsMs());
	}

	default int size() { return getAllPoints().size(); }
	default boolean isEmpty() { return size() == 0; }
}
