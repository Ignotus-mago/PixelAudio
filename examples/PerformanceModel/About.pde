import net.paulhertz.pixelaudio.granular.*;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig.*;
import net.paulhertz.pixelaudio.io.*;
import net.paulhertz.pixelaudio.sampler.*;


/**
 * A reusable real-time PixelAudio performance application with drawing, sampler and granular
 * synthesis, brush editing, presets, JSON session I/O, looping, animation, video export, and
 * optional UDP control. It is a neutral reference model derived from Bagatelle: composition-specific
 * presets and cues have been removed, while {@link OK_PerformancePreset} remains as an example and
 * extension point.
 * <p>
 * See {@link Bagatelle} for a detailed description of how PerformanceModel works. Otherwise, I'm
 * leaving this space open for your own performance notes.
 * </p><p>
 * Number keys 1-4 toggle example presets, and 0 clears the preset stack. Use the o key to load
 * audio, image, brush, or session data. The remaining commands are printed by {@link #showHelp()}.
 * Customize {@link OK_PerformancePreset}, {@link #runPerformanceCue(char)},
 * {@link #loadPerformanceGen()}, and {@link #initCustomSettings()} for a particular performance.
 * {@link WindowedBuffer} buffer traversal code is marked with {@code // *** WindowedBuffer support *** //}.
 * </p>
 *
 * @see Bagatelle
 */
