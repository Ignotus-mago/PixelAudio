/**
 * Core classes for PixelAudio, a Processing library that maps between sound
 * and image data.
 *
 * <p>PixelAudio blends sounds and images by mapping one-dimensional signal
 * arrays, typically audio samples, onto two-dimensional image arrays using
 * space-filling curves and related traversal patterns. The same mapping can be
 * used in both directions: sound can be visualized as image data, and image
 * data can be interpreted as sound.</p>
 *
 * <p>The top-level package contains the mapping framework, built-in path
 * generators, bitmap transform utilities, image/audio transcoding tools, and
 * WaveSynth additive synthesis classes. Gesture, sampler, granular, scheduling,
 * I/O, and example code are documented in their own subpackages.</p>
 * 
 * <p>You can read a scholarly introduction to PixelAudio, complete with a brief 
 * review of its historical context in intermedia art, here: 
 * <a href="https://paulhertz.net/docs/intermedia/PaulHertz_PixelAudio_EVA_paper.pdf" target="_blank">
 * PixelAudio: A Toolkit for Intermedia Composition}</a>.</p>
 *
 * <p><b>Library interface and mapping framework</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.PixelAudio PixelAudio} provides the
 *   Processing library interface.</li>
 *   <li>{@link net.paulhertz.pixelaudio.PixelAudioMapper PixelAudioMapper}
 *   maps between 1D signal arrays and 2D image pixel arrays.</li>
 *   <li>{@link net.paulhertz.pixelaudio.PixelMapGen PixelMapGen} is the
 *   abstract base class for signal-path generators.</li>
 * </ul>
 *
 * <p><b>Signal-path generators</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.HilbertGen HilbertGen} generates
 *   Hilbert curve paths.</li>
 *   <li>{@link net.paulhertz.pixelaudio.MooreGen MooreGen} generates Moore
 *   curve paths.</li>
 *   <li>{@link net.paulhertz.pixelaudio.BoustropheGen BoustropheGen} generates
 *   boustrophedon paths.</li>
 *   <li>{@link net.paulhertz.pixelaudio.DiagonalZigzagGen DiagonalZigzagGen}
 *   generates diagonal zigzag paths.</li>
 *   <li>{@link net.paulhertz.pixelaudio.MultiGen MultiGen} combines multiple
 *   PixelMapGens into a single path.</li>
 *   <li>{@link net.paulhertz.pixelaudio.BuildFromPathGen BuildFromPathGen}
 *   builds paths from stored coordinate data.</li>
 *   <li>{@link net.paulhertz.pixelaudio.RandomContinousGen RandomContinousGen}
 *   is an experimental random-path generator.</li>
 * </ul>
 *
 * <p><b>Image transforms, animation, and transcoding</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.AffineTransformType AffineTransformType}
 *   labels reflection and rotation transforms.</li>
 *   <li>{@link net.paulhertz.pixelaudio.BitmapTransform BitmapTransform}
 *   provides bitmap rotation and reflection utilities.</li>
 *   <li>{@link net.paulhertz.pixelaudio.Argosy Argosy} shifts pixel patterns
 *   along an image's signal path.</li>
 *   <li>{@link net.paulhertz.pixelaudio.Lindenmayer Lindenmayer} provides
 *   simple L-system support for Argosy patterns.</li>
 *   <li>{@link net.paulhertz.pixelaudio.AudioColorTranscoder AudioColorTranscoder}
 *   provides experimental audio/color transcoding support.</li>
 * </ul>
 *
 * <p><b>WaveSynth and signal utilities</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.WaveSynth WaveSynth} combines additive
 *   audio synthesis with color-pattern generation.</li>
 *   <li>{@link net.paulhertz.pixelaudio.WaveData WaveData} stores WaveSynth
 *   operator data.</li>
 *   <li>{@link net.paulhertz.pixelaudio.WaveSynthBuilder WaveSynthBuilder}
 *   provides utilities for building WaveSynth instruments and data sets.</li>
 *   <li>{@link net.paulhertz.pixelaudio.WindowedBuffer WindowedBuffer} provides
 *   a moving window over larger audio buffers.</li>
 * </ul>
 *
 * <p><b>Noise utilities</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.ScaledSimplex ScaledSimplex} wraps
 *   OpenSimplex noise with scaling helpers.</li>
 *   <li>{@link net.paulhertz.pixelaudio.OpenSimplex2 OpenSimplex2} implements
 *   OpenSimplex noise.</li>
 * </ul>
 *
 */
package net.paulhertz.pixelaudio;