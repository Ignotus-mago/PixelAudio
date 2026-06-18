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

    /** 
     * Create a windowed buffer with the given source, window size, and hop size. 
     * 
	 * @param buffer        the source audio data
	 * @param windowSize    the number of samples in each window
	 * @param hopSize       the number of samples to advance for each window (must be > 0)
	 * @throws IllegalArgumentException if buffer is empty, or if windowSize or hopSize are not positive
	 */
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
	 * 
	 * @return a window of audio samples from the buffer, starting at the current 
	 *         index and wrapping around if necessary
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
	 * 
	 * @param idx    the starting index for the window (can be any integer, will be normalized to buffer length)
	 * @return a window of audio samples from the buffer, starting at the normalized index and wrapping around if necessary
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

    /** Return current buffer index */
    public int getIndex() {
        return index;
    }
    
    /** Set current index. Wraps around the buffer as needed.
	 * @param index the new index to set (can be any integer, will be normalized to buffer length)
	 */
    public void setIndex(int index) {
		this.index = index % buffer.length;
	}

	/** 
	 * Expose the underlying array size.
	 * @return the length of the backing buffer
	 */
    public int getBufferSize() {
        return this.buffer.length;
    }

	/** 
	 * Expose the reusable window array size. 
	 * @return the size of the window array, the number of samples in each window
	 */
    public int getWindowSize() {
        return windowSize;
    }

    /** 
     * Expose hop size, the number of samples to advance for each window.
	 * @return the number of samples to advance for each window, which determines 
	 *         how much the index moves forward after each call to nextWindow()	
	 */
    public int getHopSize() {
        return hopSize;
    }

    /** 
     * Set the hop size, the number of samples to advance for each window. 
	 * @param    hopSize the new hop size to set (must be > 0)
	 * @throws IllegalArgumentException if hopSize is not positive
	*/
	public void setHopSize(int hopSize) {	
		if (hopSize <= 0) {
			throw new IllegalArgumentException("Hop size must be positive");
		}
		this.hopSize = hopSize;
	}

 }	
