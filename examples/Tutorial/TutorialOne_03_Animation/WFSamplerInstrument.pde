// ------------------------------------------- //
//          SAMPLER INSTRUMENT CLASS           //
// ------------------------------------------- //

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sampler-based instrument that reuses a single Sampler and
 * creates a fresh ADSR per note using ADSRParams.
 *
 * Notes:
 *  - A Sampler constructed from a MultiChannelBuffer typically copies that buffer,
 *    so only call setBuffer(...) when you truly want to replace the backing audio.
 *  - Call close() when shutting down the application to stop the scheduler.
 */
public class WFSamplerInstrument {
  private final AudioOutput audioOut;     // out to sound hardware
  private final float sampleRate;         // audio output sample rate
  private final int maxVoices;            // polyphonic voices for the Sampler
  private Sampler sampler;                // created once (or when setBuffer is called)
  private int bufferSize;                 // size of the underlying buffer (samples)
  private ADSRParams defaultADSRParams;   // stored default ADSR params

  // Single-threaded scheduler for delayed cleanup tasks (noteOff/unpatch).
  // The lambda here is a ThreadFactory: it takes the scheduled task (Runnable r),
  // wraps it in a new Thread named "WFSamplerInstrument-scheduler", and marks it
  // as a daemon thread so it won't block JVM shutdown.
  // We'll later use scheduler.schedule(...) to handle noteOff/unpatch timing.
  private final ScheduledExecutorService scheduler =
    Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "WFSamplerInstrument-scheduler");
        t.setDaemon(true);
        return t;
    });

  /**
   * Construct the instrument from the MultiChannelBuffer (copies happen inside Sampler).
   *
   * @param buffer          the loaded MultiChannelBuffer (the underlying audio source)
   * @param sampleRate      sample rate used to create the Sampler
   * @param maxVoices       polyphony for the Sampler
   * @param audioOut        Minim AudioOutput to patch into
   * @param defaultParams   default ADSR parameters
   */
  public WFSamplerInstrument(MultiChannelBuffer buffer,
    float sampleRate,
    int maxVoices,
    AudioOutput audioOut,
    ADSRParams defaultParams) {
    this.sampleRate = sampleRate;
    this.maxVoices = maxVoices;
    this.audioOut = audioOut;
    this.defaultADSRParams = defaultParams;
    setBuffer(buffer); // creates the sampler and captures buffer size
  }

  /**
   * Replace the backing buffer (creates a new Sampler). Avoid calling this frequently.
   */
  public synchronized void setBuffer(MultiChannelBuffer buffer) {
    // creating a new Sampler will copy the buffer internally (costly for large files)
    this.sampler = new Sampler(buffer, sampleRate, maxVoices);
    this.bufferSize = buffer.getBufferSize();
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public ADSRParams getDefaultADSRParams() {
    return defaultADSRParams;
  }

  public void setDefaultADSRParams(ADSRParams params) {
    this.defaultADSRParams = params;
  }

  /**
   * Convenience: play using the stored default ADSRParams.
   *
   * @param samplePos start position (samples)
   * @param sampleLen length (samples)
   * @param amplitude amplitude
   * @return actual length used (samples)
   */
  public int playSample(int samplePos, int sampleLen, float amplitude) {
    return playSample(samplePos, sampleLen, amplitude, defaultADSRParams);
  }

  /**
   * Main play method: constructs a fresh Minim ADSR from env,
   * patches Sampler -> ADSR -> AudioOutput, triggers the Sampler,
   * schedules noteOff and then schedules unpatch of the Sampler from the ADSR
   * after the release has completed (to avoid accumulating ADSR references).
   * Note that you can vary the ADSRParams every time you call playSample().
   *
   * @param samplePos   start position (samples)
   * @param sampleLen   length (samples)
   * @param amplitude   amplitude
   * @param env         ADSR parameters for this note (must be non-null)
   * @return actual length used (samples)
   */
  public int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
    if (env == null) throw new IllegalArgumentException("ADSRParams env must not be null");
    if (sampler == null) throw new IllegalStateException("Sampler not initialized (call setBuffer first)");
    // clip sample length so we don't run off the end of the buffer
    if (samplePos < 0) samplePos = 0;
    if (sampleLen < 0) sampleLen = 0;
    if (samplePos + sampleLen > bufferSize) {
      sampleLen = Math.max(0, bufferSize - samplePos);
    }
    // compute end sample index including release so the sampler won't cut the release off
    int releaseSamples = Math.round(env.getRelease() * sampleRate);
    int end = samplePos + sampleLen + releaseSamples;
    if (end >= bufferSize) end = bufferSize - 1;
    // create a fresh Minim ADSR for this note
    final ADSR noteAdsr = env.toADSR();
    // set sampler parameters (these are snapshotted by the Sampler when trigger() runs)
    sampler.amplitude.setLastValue(amplitude);
    sampler.begin.setLastValue(samplePos);
    sampler.end.setLastValue(end);
    // connect and start: Sampler -> ADSR -> Output
    sampler.patch(noteAdsr);
    noteAdsr.patch(audioOut);
    // trigger the voice and start envelope
    sampler.trigger();
    noteAdsr.noteOn();
    // schedule noteOff at the sample duration (if duration > 0)
    if (sampleLen > 0) {
      long durationMillis = Math.round((sampleLen / (double) sampleRate) * 1000.0);
      long releaseMillis = Math.round(env.getRelease() * 1000.0);
      // At durationMillis: call noteOff() to start release and schedule unpatch-after-release for audioOut
      scheduler.schedule(() -> {
        try {
          noteAdsr.noteOff();
          /*
           * Let the ADSR unpatch audioOut after its release completes.
           * Minim provides unpatchAfterRelease on ADSR; calling it ensures
           * audioOut is disconnected after the release phase finishes.
           */
          noteAdsr.unpatchAfterRelease(audioOut);
        }
        catch (Throwable t) {
          // defensive: don't let exceptions kill the scheduler thread
          t.printStackTrace();
        }
      }
      , durationMillis, TimeUnit.MILLISECONDS);
      // After duration + release + small margin, explicitly unpatch sampler -> ADSR to prevent
      // the sampler from holding references to many ADSRs (avoids memory leak).
      long totalDelay = durationMillis + releaseMillis + 50; // extra margin
      scheduler.schedule(() -> {
        try {
          sampler.unpatch(noteAdsr);
        }
        catch (Throwable t) {
          // ignore if unpatch not supported or already unpatched
        }
      }
      , totalDelay, TimeUnit.MILLISECONDS);
    } 
    else {
      // If sampleLen == 0 (sustain indefinitely), we don't schedule noteOff.
      // Caller must later call stopAll or provide another mechanism.
    }
    return sampleLen;
  }

  /**
   * Call when shutting down the app to stop the internal scheduler.
   */
  public void close() {
    scheduler.shutdown();
  }
}
