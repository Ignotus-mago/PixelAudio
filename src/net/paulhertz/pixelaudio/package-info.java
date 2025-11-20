/**
Package <code>net.paulhertz.pixelaudio</code> blends sounds and images by mapping between 
arrays of audio samples and arrays of pixel values. 1D Audio arrays are mapped onto 
2D image arrays using space-filling curves and patterns. PixelAudio provides a template 
to design your own mappings, and methods to translate values between audio and pixel data. 
Examples provide hooks for animation, audio/image synthesis, and hooks for UDP communication 
with Cycling74's Max application and other audio and video programming environments. 
<p>
In many respects, PixelAudio behaves like an audio visualization widget, with one 
important difference: images can become sound as easily as sound becomes images. 
It can be treated as a basic image synthesis application, using audio signals to 
generate images, or as a somewhat noisy audio synthesis application, generating 
sound from images. 
</p>

<ul>

<li>{@link net.paulhertz.pixelaudio.PixelAudio PixelAudio} provides the Processing library interface for the PixelAudio library.</li>

<li>{@link net.paulhertz.pixelaudio.PixelMapGen PixelMapGen} is an abstract class for handling coordinates and LUT generation for PixelAudioMapper.</li>

<li>{@link net.paulhertz.pixelaudio.DiagonalZigzagGen DiagonalZigzagGen} is a PixelMapGen child class for generating diagonal zigzag paths over an image.</li>

<li>{@link net.paulhertz.pixelaudio.HilbertGen HilbertGen} is a PixelMapGen child class for generating Hilbert curves over an image.</li>

<li>{@link net.paulhertz.pixelaudio.MooreGen MooreGen} is a PixelMapGen child class for generating Moore curves over an image.</li>

<li>{@link net.paulhertz.pixelaudio.BoustropheGen BoustropheGen} is a PixelMapGen child class for generating boustrophedon paths over an image.</li>

<li>{@link net.paulhertz.pixelaudio.BuildFromPathGen BuildFromPathGen} is a PixelMapGen child class for generating from path coordinates stored in a JSON or similar data file.</li>

<li>{@link net.paulhertz.pixelaudio.MultiGen MultiGen} is a PixelMapGen child class for generating a single path over multiple PixelMapGens arranged in a grid.</li>

<li>{@link net.paulhertz.pixelaudio.PixelAudioMapper PixelAudioMapper} maps between 1D "signal" arrays of audio samples formatted as floating point values 
in the range [-1, 1] and 2D "image" arrays formatted as RGBA integer pixel data. It is initialized with a PixelMapGen.</li>

<li>{@link net.paulhertz.pixelaudio.AffineTransformType AffineTransformType} is an enum for labeling basic affine transforms for reflection and 90-degree rotation.</li>

<li>{@link net.paulhertz.pixelaudio.BitmapTransform BitmapTransform} provides static methods for rotating and reflecting 2D integer arrays using index remapping</li>

<li>{@link net.paulhertz.pixelaudio.WaveSynth WaveSynth} implements a combination of color organ and additive audio wave generation for animation and audio synthesis.</li>

<li>{@link net.paulhertz.pixelaudio.WaveData WaveData} provides storage and utility methods for WaveSynth "operators", which are audio signal generators.</li>

<li>{@link net.paulhertz.pixelaudio.WaveSynthBuilder WaveSynthBuilder} provides utilities for generating WaveSynth "instruments" and working with WaveSynth data.</li>

<li>{@link net.paulhertz.pixelaudio.Argosy Argosy} provides tools for shifting pixel patterns along the signal path of an image.</li>

<li>{@link net.paulhertz.pixelaudio.Lindenmayer Lindenmayer} implements a basic L-system for generating patterns for Argosy objects.</li>

<li>{@link net.paulhertz.pixelaudio.ScaledSimplex ScaledSimplex} provides a wrapper for generating simple noise using Open Simplex.</li>

<li>{@link net.paulhertz.pixelaudio.OpenSimplex2 OpenSimplex2} is an open source implementation of simplex noise.</li>

<li>{@link net.paulhertz.pixelaudio.RandomContinousGen RandomContinousGen} is a PixelMapGen child class and a work-in-progress. It will provide random paths over an image.</li>

<li>{@link net.paulhertz.pixelaudio.AudioColorTranscoder AudioColorTranscoder} is an experimental class for extending PixelAudioMapper.ChannelNames functionality.</li>


</ul>


*/
package net.paulhertz.pixelaudio;