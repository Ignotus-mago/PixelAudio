/**
 * 
 * ArgosyMixer demonstrates how you can use the Argosy class to create and animate patterns 
 * and save them to video. This is still a work in progress, with the new PASamplerInstrument 
 * class and audio events not yet handled in the most efficient way. There will be updates.
 * 
 * The Argosy class turns arrays of integers into color patterns. It steps through the Pattern
 * array to create blocks of pixels and assigns color to the blocks as it steps through the 
 * Colors arrays. The arrays don't have to be the same size--this creates variations in the 
 * pattern color sequences. For example, we might have blocks that are [5, 3, 8] units and 
 * assign them just two colors, say black and white:
 * 
 *    |    5     |  3   |       8        |    5     |  3   |       8        |
 *    |    B     |  W   |       B        |    W     |  B   |       W        |
 * 
 * As you can see, the two patterns together generate a larger repeating unit. If the "Repeat" 
 * filed in the GUI is set to 0, the patterns fill the Argosy array, which is the same size as 
 * the pixels[] array for the display image. A non-zero Repeat value determines how many times  
 * the pattern repeats. The way the Argosy array fills the display image is determined by the 
 * Map parameter. The map is a PixelAudioMapper determined by a PixelMapGen: basically, it 
 * creates a path (the "signal path") that visits every pixel in the display image once. 
 * Experiment with the GUI Map menu to find out more about how paths work. Read PixelAudioMapper 
 * and PixelMapGen documentation if you want detailed information. You can also browse the code
 * for quite a few MultiGen PixelMapGens in this sketch and in the static methods appended to
 * PixelAudio PixelMapGen classes. 
 * 
 * ArgosyMixer provides a GUI for modifying argosy and animation
 * parameters, plus a series of key commands that can shift patterns along
 * the signal path. There are two Argosy patterns involved: the top one, 
 * Argosy 2, is transparent (Opacity = 127, initially). 
 * 
 * In the GUI, the following parameters are exposed for each Argosy pattern:
 * 
 *   Map          -- select the PixelMapGen for each Argosy instance
 *   Colors       -- select a preset palette from a drop down list
 *   Opacity      -- opacity of the colors in the palette, 0-255
 *   Pattern      -- select a preset numeric pattern from a drop down list
 *   Repeat       -- number of times to repeat the pattern; enter 0 for maximum repetitions 
 *   Unit         -- the number of pixels in each unit of the pattern
 *   Gap          -- the number of pixels between each repeated pattern
 *   Gap color    -- select a preset gap color from a drop down list
 *   Gap opacity  -- enter the alpha channel value 0-255 for the gap color
 *   >> ANIMATION <<
 *   Show         -- show or hide Argosy 1 or Argosy 2
 *   Freeze       -- freeze animation of Argosy 1 or Argsoy 2
 *   Step         -- number of pixels to shift on each animation step (negative to shift right)
 *   Open frames  -- number of frames to hold at animation start, applies both Argosy 1 and Argosy 2
 *   Close frames -- number of frames to hold at animation end, applies to both Argsoy 1 and Argosy 2
 *   Run frames   -- number of frames to animate before a hold, sets Argosy 1 and Argosy 2 separately
 *   Hold frames  -- number of frames to hold after a run of frames, sets Argosy 1 and Argosy 2 separately
 *   Duration     -- number of frames in the animation
 *   Record Video -- press to run and record animation from current display 
 *   
 *   
 * I suggest you start by experimenting with the patterns "The One" and "One-one". They create
 * repeating patterns of one or two elements. Setting the Unit value (the number of pixels
 * in each pattern element) to a power of 2 or a sum of powers of 2 is a good place to start, 
 * especially with the Hilbert PixelMapGens in the Map menu. 
 *
 * Click on the image to hear the sounds made by the patterns with sampling rate 48KHz. The patterns
 * produce step or pulse (square) waves, so they are buzzy. Opacity will change how loud the sound is.
 * 
 * You can create stereo drones with the 'e' command, which creates a series of audio events along 
 * the points of an ellipse. 
 * 
 * Press the spacebar to start or stop animation. 
 *   
 *   
 * --------------------------------------------------------------------------------------------
 * ***>> NOTE: Key commands only work when the image display window is the active window. <<***
 * --------------------------------------------------------------------------------------------
 * 
 * Key Commands
 * 
 * Press the UP arrow to increase audio output gain by 3.0 dB.
 * Press the DOWN arrow to decrease audio output gain by 3.0 dB.
 * Press ' ' to toggle animation.
 * Press 'e' to trigger elliptical trail of audio events.
 * Press 'a' to shift left one argosy unit.
 * Press 'A' to shift right one argosy unit.
 * Press 'b' to shift left one argosy length.
 * Press 'B' to shift right one argosy length.
 * Press 'c' to shift left one argosy length + argosy gap.
 * Press 'C' to shift right one argosy length + argosy gap.
 * Press 'd' to advance one animation step.
 * Press 'D' to go back one animation step.
 * Press 'g' or 'G' to set the pixelShift of argosies to zero (reset return point).
 * Press 'l' to shift argosies left one animation step.
 * Press 'L' to shift argosies left one animation step.
 * Press 'r' to shift argosies right one animation step.
 * Press 'R' to shift argosies right one animation step.
 * Press 'p' to shift argosies left one pixel.
 * Press 'P' to shift argosies right one pixel.
 * Press 'f' to freeze changes to argosy 1.
 * Press 'F' to freeze changes to argosy 2.
 * Press 'i' or 'I' to show stats about argosies.
 * Press 'S' to save current display to an PNG file.
 * Press 's' to save current display to an PNG file.
 * Press 'u' or 'U' to reinitialize any argosies that aren't frozen.
 * Press 'v' or 'V' to toggle video recording.
 * Press 'w' to reset animation tracking.
 * Press 'W' to reset animation tracking.
 * Press 'z' to reset argosy 1 to initial position.
 * Press 'Z' to reset argosy 2 to inttial position.
 * Press 'h' or 'H' to show help message in console.
 * 
 * 
 * TODO save two image files, two audio files -- one for each Argosy
 * TODO bug fix, when changing gap color argosy resets with 0 shift
 * TODO reset animation with a key command 
 * 
 */
 
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
