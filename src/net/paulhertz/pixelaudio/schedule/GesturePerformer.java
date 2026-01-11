package net.paulhertz.pixelaudio.schedule;

import net.paulhertz.pixelaudio.curves.PACurveMaker;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig;

// future GranularPerformer wraps GestureGranularRenderer
// future SamplerPerformer wraps your TimedLocation-based playback

interface GesturePerformer {
		
    void play(PACurveMaker curve, GestureGranularConfig config);
}
