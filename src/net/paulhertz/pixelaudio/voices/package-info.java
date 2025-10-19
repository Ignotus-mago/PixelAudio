/**
Package <code>net.paulhertz.pixelaudio.voices</code> implements a hierarchy of Java classes 
implementing a digital audio sampling synth using the Minim library for Processing.
<p>
<ul>
<li>PAPlayable is the root interface for things that can play or render audio;</li>

<li>PASamplerPlayable extends it for sample-based playback (with parameters like pitch, gain, and envelopes);</li>

<li>PASamplerVoice handles individual note playback (one per simultaneous note);</li>

<li>PASamplerInstrument manages a set of voices for polyphony and envelopes;</li>

<li>PASamplerInstrumentPool and PASamplerInstrumentPoolMulti manage collections of instruments, supporting reuse and multi-sample configurations.</li>
</ul>
</p>


*/
package net.paulhertz.pixelaudio.voices;