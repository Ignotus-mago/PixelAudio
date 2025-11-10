/**
<p>Package <code>net.paulhertz.pixelaudio.voices</code> implements a hierarchy of Java classes 
for implementing a digital audio sampling synth with support from the Minim library for Processing.</p>
The 
<p>
<ul>
<li>PAPlayable is the root interface for things that can play or render audio;</li>

<li>PASampler provides an interface for fundamental sampler methods, including play() and setBuffer();</li>

<li>PASamplerPlayable extends PAPlayable for sample-based playback (with parameters like pitch, gain, and envelopes);</li>

<li>PASamplerVoice handles individual note playback (one per simultaneous note);</li>

<li>PASamplerInstrument manages a set of voices for polyphony and envelopes;</li>

<li>PASamplerInstrumentPool and PASamplerInstrumentPoolMulti manage collections of instruments, supporting reuse and multi-sample configurations.</li>
</ul>
</p>


*/
package net.paulhertz.pixelaudio.voices;