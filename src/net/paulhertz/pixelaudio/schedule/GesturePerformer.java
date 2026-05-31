package net.paulhertz.pixelaudio.schedule;

import net.paulhertz.pixelaudio.curves.PACurveMaker;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig;

// future GranularPerformer wraps granular gesture parameters
// future SamplerPerformer wraps your TimedLocation-based playback
// TODO delete, unused ... but maybe the concept is valuable?

@Deprecated
interface GesturePerformer {
		
    void play(PACurveMaker curve, GestureGranularConfig config);
}
