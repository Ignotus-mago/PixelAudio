import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Pool of WFSamplerInstrument objects for polyphonic playback.
 *
 * Features:
 *  - Fixed-size pool with optional voice stealing
 *  - Automatic release and return-to-pool after ADSR completes
 *  - Can reload a new MultiChannelBuffer into all instruments via setBuffer(...)
 */
public class WFSamplerInstrumentPool {
  private final int poolSize;
  private final float sampleRate;
  private final ADSRParams defaultADSR;

  // Keep references for rebuilding instruments
  private final AudioOutput out;
  private final int perInstrumentVoices; // typically 1
  private MultiChannelBuffer buffer;     // current sample buffer

  // track availability of instruments
  private final ArrayDeque<WFSamplerInstrument> available = new ArrayDeque<>();
  private final Set<WFSamplerInstrument> inUse = new HashSet<>();

  // Single-threaded scheduler for delayed cleanup tasks (noteOff/unpatch).
  // The lambda here is a ThreadFactory: it takes the scheduled task (Runnable r),
  // wraps it in a new Thread named "WFSamplerInstrument-scheduler", and marks it
  // as a daemon thread so it won't block JVM shutdown.
  // We'll later use scheduler.schedule(...) to handle noteOff/unpatch timing.
  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(r -> {
          Thread t = new Thread(r, "WFSamplerInstrumentPool-scheduler");
        t.setDaemon(true);
        return t;
      });
  
  // stealing voices is okay by me, or not
  private volatile boolean voiceStealingEnabled = true;

  public WFSamplerInstrumentPool(MultiChannelBuffer buffer,
                           float sampleRate,
                           int poolSize,
                           int perInstrumentVoices,
                           AudioOutput out,
                           ADSRParams defaultADSR) {
    this.buffer = buffer;
    this.sampleRate = sampleRate;
    this.poolSize = poolSize;
    this.perInstrumentVoices = perInstrumentVoices;
    this.out = out;
    this.defaultADSR = defaultADSR;
    initPool(buffer);
  }

  private synchronized void initPool(MultiChannelBuffer buffer) {
    available.clear();
    inUse.clear();
    for (int i = 0; i < poolSize; i++) {
      available.add(new WFSamplerInstrument(buffer, sampleRate, perInstrumentVoices, out, defaultADSR));
    }
  }

  /**
 * Replace the backing buffer in all instruments.
 * All current voices are stopped immediately.
 */
  public synchronized void setBuffer(MultiChannelBuffer newBuffer) {
    this.buffer = newBuffer;
    // Reinitialize all instruments with the new buffer
    initPool(newBuffer);
  }

  public void setVoiceStealingEnabled(boolean enabled) {
    this.voiceStealingEnabled = enabled;
  }

  public boolean isVoiceStealingEnabled() {
    return voiceStealingEnabled;
  }

  /**
 * Play a sample with default ADSR.
 */
  public int playSample(int samplePos, int sampleLen, float amplitude) {
    return playSample(samplePos, sampleLen, amplitude, defaultADSR);
  }

  public synchronized int playSample(int samplePos, int sampleLen, float amplitude, ADSRParams env) {
    WFSamplerInstrument inst = available.poll();
    if (inst == null) {
      if (voiceStealingEnabled && !inUse.isEmpty()) {
        // Steal a voice: pick one arbitrarily
        inst = inUse.iterator().next();
        inUse.remove(inst);
      } 
      else {
        // No voices available
        return 0;
      }
    }
    inUse.add(inst);
    int actualLen = inst.playSample(samplePos, sampleLen, amplitude, env);
    // Schedule return of this instrument after env duration
    float envDurationMs = (env.getAttack() + env.getDecay() + env.getRelease()) * 1000f;
    long durationMillis = Math.round((actualLen / (double) sampleRate) * 1000.0 + envDurationMs);
    final WFSamplerInstrument instFinal = inst; // capture inst in a final var
    scheduler.schedule(() -> {
      synchronized (WFSamplerInstrumentPool.this) {
        inUse.remove(instFinal);
        available.add(instFinal);
      }
    }, durationMillis, TimeUnit.MILLISECONDS);
    return actualLen;
  }

  /**
 * Stop scheduler and release all resources.
 */
  public void close() {
    scheduler.shutdown();
    available.clear();
    inUse.clear();
  }
}
