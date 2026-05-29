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

package net.paulhertz.pixelaudio;

/**
 * A windowed buffer class to permit loading large audio sources into memory and stepping through them.
 */
public class WindowedBuffer {
    private final float[] buffer;   // circular source
    private final float[] window;   // reusable window array
    private final int windowSize;   // number of samples in a window
    private int hopSize;      		// step between windows
    private int index = 0;          // current start position in buffer

    public WindowedBuffer(float[] buffer, int windowSize, int hopSize) {
        if (buffer.length == 0) {
            throw new IllegalArgumentException("Buffer must not be empty");
        }
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be positive");
        }
        if (hopSize <= 0) {
            throw new IllegalArgumentException("Hop size must be positive");
        }
        this.buffer = buffer;
        this.windowSize = windowSize;
        this.hopSize = hopSize;
        this.window = new float[windowSize];
    }

    /**
     * Returns the next window, advancing the read index by hopSize.
     * Wraps around the buffer as needed.
     */
    public float[] nextWindow() {
        int bufferLen = buffer.length;
        // Copy first chunk
        int firstCopyLen = Math.min(windowSize, bufferLen - index);
        System.arraycopy(buffer, index, window, 0, firstCopyLen);
        // Wrap if needed
        if (firstCopyLen < windowSize) {
            int remaining = windowSize - firstCopyLen;
            System.arraycopy(buffer, 0, window, firstCopyLen, remaining);
        }
        // Advance start index
        index = (index + hopSize) % bufferLen;
        return window;
    }

	/**
     * Returns the window at a supplied index. Wraps around the buffer as needed.
	 * Updates current index and advances it by hopSize.
     */
    public float[] gettWindowAtIndex(int idx) {
    	int len = buffer.length;
    	idx = ((idx % len) + len) % len; // normalize to 0..len-1
    	setIndex(idx);
        return nextWindow();
    }

    /** Reset reader to start of buffer */
    public void reset() {
        index = 0;
    }

    /** Current buffer index */
    public int getIndex() {
        return index;
    }
    /** set current index */
    public void setIndex(int index) {
		this.index = index % buffer.length;
	}

	/** Expose the underlying array size */
    public int getBufferSize() {
        return this.buffer.length;
    }

	/** Expose the reusable window array size */
    public int getWindowSize() {
        return windowSize;
    }

    /** Expose hop size */
    public int getHopSize() {
        return hopSize;
    }

    /** Set the hop size */
	public void setHopSize(int hopSize) {
		this.hopSize = hopSize;
	}

 }	
