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

package net.paulhertz.pixelaudio.schedule;

// TODO extended classes for different time-based events. 

/**
 * Used to schedule or track events that take place at specific coordinate locations.
 * Used in multiple example sketches. 
 */
public class TimedLocation implements Comparable<TimedLocation> {
	private int x;
	private int y;
	private int eventTime;
	private boolean isStale;
	private int durationMs = 0;

	public TimedLocation(int x, int y, int eventTime, int durationMs) {
		this.x = x;
		this.y = y;
		this.eventTime = eventTime;
		this.durationMs = durationMs;
		this.isStale = false;
	}

	public TimedLocation(int x, int y, int eventTime) {
		this(x, y, eventTime, 0);
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public int eventTime() {
		return this.eventTime;
	}

	public boolean isStale() {
		return this.isStale;
	}

	public void setStale(boolean stale) {
		this.isStale = stale;
	}
		
	public int getDurationMs() {
		return durationMs;
	}

	public void setDurationMs(int durationMs) {
		this.durationMs = durationMs;
	}

	public int compareTo(TimedLocation tl) {
		if (eventTime() > tl.eventTime()) return 1;
		else {
			if (eventTime() == tl.eventTime) return 0;
			else return -1;
		}
	}
	
	
}
