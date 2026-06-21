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
 * PAGesture provides a interface for "gesture" defined by:
 * <ol>
 *   <li>a list of points and </li> 
 *   <li>a list of time offsets where</li>
 *   <li>both lists have the same cardinality and</li> 
 *   <li>time offsets are monotonically non-decreasing. In addition,</li> 
 *   <li>the time list is expected (but not required) to start with a first element 0.</li>  
 * </ol>  
 * <p>Absolute time of gesture creation can be returned from getStartTimeMs(), 
 * possibly as millis from application start.</p>
 * 
 * <p>Gestures are often captured from GUI interaction, as is the case with PACurveMaker. 
 * PAGesture opens the way to use many different sources for generating gestures. 
 * "Gesture" is a core concept in the PixelAudio library, implemented by PACurveMaker, in particular.</p>
 * 
 * @see net.paulhertz.pixelaudio.schedule.GestureSchedule
 * 
 */
public interface PAGesture {
	/**
	 * Dense gesture points.
	 *
	 * @return gesture points
	 */
	java.util.List<processing.core.PVector> getAllPoints();

	/**
	 * Time offsets in ms, same length as getAllPoints(), first element typically 0.
	 *
	 * @return gesture time offsets in milliseconds
	 */
	float[] getTimeOffsetsMs();
	
	/**
	 * Absolute start time (e.g. millis() when gesture began).
	 *
	 * @return absolute gesture start time in milliseconds
	 */
	long getStartTimeMs();

	/** Convenience: a schedule over the dense gesture. */
	default net.paulhertz.pixelaudio.schedule.GestureSchedule getAllPointsSchedule() {
		return new net.paulhertz.pixelaudio.schedule.GestureSchedule(getAllPoints(), getTimeOffsetsMs());
	}

	/**
	 * Returns the number of gesture points.
	 *
	 * @return gesture point count
	 */
	default int size() { return getAllPoints().size(); }
	/**
	 * Reports whether the gesture contains no points.
	 *
	 * @return true when the gesture has no points
	 */
	default boolean isEmpty() { return size() == 0; }
}
