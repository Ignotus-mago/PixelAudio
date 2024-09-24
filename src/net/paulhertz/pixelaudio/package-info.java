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
sound from images. The audio is likely to be noisy because the audio has only 
8-bits of resolution, like a grayscale representation of an image. 
</p>


*/
package net.paulhertz.pixelaudio;