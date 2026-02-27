package net.paulhertz.pixelaudio.sampler;

/**
 * PitchPolicy
 *
 * Determines how PASamplerInstrument / PASamplerVoice should interpret
 * their pitch parameter in combination with a given PASource.
 */
public enum PitchPolicy {

    /**
     * Instrument pitch maps to playback rate (classic sample playback).
     * Source is considered time-neutral.
     */
    INSTRUMENT_RATE,

    /**
     * Source (e.g., granular engine) controls time/pitch.
     * Instrument pitch should be ignored for this source.
     */
    SOURCE_GRANULAR,

    /**
     * Both instrument and source contribute to pitch/time.
     * Advanced / experimental; can create complex artifacts.
     */
    COMBINED
}
