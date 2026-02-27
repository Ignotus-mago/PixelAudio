/**
<p>Package <code>net.paulhertz.pixelaudio.voices</code> provides a hierarchy of Java classes 
for implementing digital audio sampling synthesis with support from the Minim library for Processing.</p>


<ul>

<li>{@link net.paulhertz.pixelaudio.sampler.PlaybackInfo PlaybackInfo} maintains information about audio playback events, which it generates in response to play() and playSample() methods.</li>

<li>{@link net.paulhertz.pixelaudio.sampler.ADSRUtils ADSRUtils} provides utilities for working with envelopes in ADSRParams format.</li>

<li>{@link net.paulhertz.pixelaudio.sampler.ADSRParams ADSRParams} provides a wrapper for parameters in Minim's ADSR class. Use it to create envelopes.</li>

<li>{@link net.paulhertz.pixelaudio.sampler.SimpleADSR SimpleADSR} provides an executable ADSR envelope for PASamplerVoice.</li>

<li>{@link net.paulhertz.pixelaudio.schedule.TimedLocation TimedLocation} handles event scheduling.</li>

<li>{@link net.paulhertz.pixelaudio.sampler.PAPlayable PAPlayable} is the root interface for things that can play or render audio.</li>

<li>{@link net.paulhertz.pixelaudio.sampler.PASampler PASampler} provides an interface for fundamental sampler methods, including play() and setBuffer().</li>

<li>{@link net.paulhertz.pixelaudio.sampler.PASamplerPlayable PASamplerPlayable} extends PAPlayable for sample-based playback (with parameters like pitch, gain, and envelopes).</li>

<li>{@link net.paulhertz.pixelaudio.sampler.PASamplerVoice PASamplerVoice} handles individual note playback (one per simultaneous note). It's a custom Minim UGen which you don't need to instantiate directly.</li>

<li>{@link net.paulhertz.pixelaudio.sampler.PASharedBufferSampler PASharedBufferSampler} mediates between PASamplerInstrument and PASamplerVoice. Instantiation is controlled through PASamplerInstrument.</li>

<li>{@link net.paulhertz.pixelaudio.sampler.PASamplerInstrument PASamplerInstrument} manages a set of voices for polyphony and envelopes. Instantiate this in your code for basic audio sampler synthesis.</li>

<li>{@link net.paulhertz.pixelaudio.sampler.PASamplerInstrumentPool PASamplerInstrumentPool} manages a collection of PASamplerInstruments, supporting reuse and multi-sample configurations. Think of it as a section of instruments.</li>

<li>{@link net.paulhertz.pixelaudio.sampler.PASamplerInstrumentPoolMulti PASamplerInstrumentPoolMulti} manages a collection of PASamplerInstrumentPools: Think of it as the orchestra.</li>

</ul>



*/
package net.paulhertz.pixelaudio.sampler;


