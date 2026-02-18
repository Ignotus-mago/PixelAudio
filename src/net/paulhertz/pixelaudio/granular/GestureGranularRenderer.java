package net.paulhertz.pixelaudio.granular;

import net.paulhertz.pixelaudio.schedule.GestureSchedule;

import net.paulhertz.pixelaudio.voices.ADSRParams;
import net.paulhertz.pixelaudio.PixelAudioMapper;
import processing.core.PVector;

import java.util.List;

/**
 * TODO candidate for removal
 * 
 * GestureGranularRenderer
 *
 * Renders a GestureSchedule into audio by triggering a granular "burst"
 * (IndexGranularSource micro-train) at each scheduled event time.
 *
 * Option B renderer: schedule event i -> one burst starting at mapped sampleIndex(p_i),
 * scheduled at schedule.timesMs[i].
 */
public final class GestureGranularRenderer {

	private GestureGranularRenderer() {}

	// --------------------------------------------------------------------
	// Mapping strategy (OOP, Processing-friendly)
	// --------------------------------------------------------------------

	public interface GestureMapping {
		/** Convert a gesture point into a source sample index (0..sourceLen-1). */
		int sampleIndex(PVector p);

		/** Convert a gesture point into stereo pan (-1..+1). */
		float pan(PVector p);
	}

	/**
	 * Default mapping for PixelAudio:
	 *  - sample index from PixelAudioMapper.lookupSample(x,y)
	 *  - optional mapping offset (totalShift) and modulo mapSize
	 *  - soft pan range mapped from x across screenWidth
	 *
	 * Designed to be stateful: totalShift and panRange can change over time
	 * (e.g., sliding window through a larger buffer).
	 */
	public static final class DefaultMapping implements GestureMapping {

		private final PixelAudioMapper mapper;
		private int totalShift;
		private final int mapSize;         // use <=0 to disable modulo
		private int screenWidth;           // must be >= 1
		private float panRange;            // 0..1

		public DefaultMapping(PixelAudioMapper mapper,
				int totalShift,
				int mapSize,
				int screenWidth,
				float panRange) {
			if (mapper == null) throw new IllegalArgumentException("mapper must not be null");
			this.mapper = mapper;
			this.totalShift = totalShift;
			this.mapSize = mapSize;
			this.screenWidth = Math.max(1, screenWidth);
			this.panRange = clamp01(panRange);
		}

		public void setTotalShift(int newShift) { this.totalShift = newShift; }

		public int getTotalShift() { return totalShift; }

		public void setScreenWidth(int w) { this.screenWidth = Math.max(1, w); }

		public void setPanRange(float r) { this.panRange = clamp01(r); }

		public float getPanRange() { return panRange; }

		@Override
		public int sampleIndex(PVector p) {
			int x = Math.round(p.x);
			int y = Math.round(p.y);
			int base = mapper.lookupSignalPos(x, y);
			int idx = base + totalShift;
			if (mapSize > 0) {
				idx %= mapSize;
				if (idx < 0) idx += mapSize;
			}
			return idx;
		}

		@Override
		public float pan(PVector p) {
			// map x in [0..screenWidth-1] -> [-panRange..+panRange]
			if (screenWidth <= 1) return 0f;
			float u = p.x / (screenWidth - 1f); // can exceed [0..1] if point is outside window
			if (u < 0f) u = 0f;
			if (u > 1f) u = 1f;
			float pan = (u * 2f - 1f) * panRange;
			return clampPan(pan);
		}

		private static float clamp01(float v) {
			if (v < 0f) return 0f;
			if (v > 1f) return 1f;
			return v;
		}
	}

	// --------------------------------------------------------------------
	// Main API
	// --------------------------------------------------------------------

	/**
	 * Render a GestureSchedule as fixed-hop granular bursts at each schedule event.
	 *
	 * Implementation notes:
	 *  - Each event i schedules one IndexGranularSource at delaySamples derived from timesMs.
	 *  - The burst uses fixed hop in time (cfg.hopLengthSamples) and fixed hop in source index
	 *    (indexHopSamples == hopSamples), giving a simple forward micro-scan.
	 *  - Per-event pan comes from mapping.pan(p).
	 *  - Stream-level pitch is NOT applied here (IndexGranularSource uses SOURCE_GRANULAR policy).
	 *    If you later want transpose, you can implement it by decoupling timeHop vs indexHop.
	 *
	 * @param buf           mono source buffer
	 * @param schedule      GestureSchedule (points + timesMs; lengths should match)
	 * @param cfg           GestureGranularConfig (grain length, hop length, gain, env...)
	 * @param gSynth        PAGranularInstrument (sample-accurate scheduling)
	 * @param sampleRate    output sample rate (audioOut.sampleRate())
	 * @param mapping       maps points to (sampleIndex, pan)
	 * @param burstGrains   grains per burst (>=1). 1 approximates old “one grain per event.”
	 * @return number of events scheduled
	 */
	public static int playBursts(float[] buf,
			GestureSchedule schedule,
			GestureGranularConfig cfg,
			PAGranularInstrument gSynth,
			float sampleRate,
			GestureMapping mapping) {

		if (buf == null || schedule == null || cfg == null || gSynth == null || mapping == null) return 0;
		if (sampleRate <= 0f) return 0;
		if (schedule.isEmpty()) return 0;

		final List<PVector> pts = schedule.points;
		final float[] timesMs = schedule.timesMs;

		final int n = schedule.size();
		if (n <= 0) return 0;

		final int grainLen = Math.max(1, cfg.grainLengthSamples);
		final int hop = Math.max(1, cfg.hopLengthSamples);
		final int numGrains = Math.max(1, cfg.burstGrains);

		// Settings for IndexGranularSource bursts
		GranularSettings settings = new GranularSettings();
		settings.defaultGrainLength = grainLen;
		settings.hopSamples = hop;
		settings.gain = 1.0f;
		settings.pan = 0.0f;
		// windowFunction remains default Hann unless you add a cfg field later.

		float amp = cfg.gainLinear();
		if (cfg.autoBurstGainComp && cfg.burstGrains > 1) {
		  amp *= 1.0f / (float) Math.sqrt(cfg.burstGrains);
		}
		
		final ADSRParams env = cfg.env;

		// Robust: treat schedule as absolute times; schedule relative to first entry
		final float t0 = timesMs[0];

		int scheduled = 0;

		for (int i = 0; i < n; i++) {
			PVector p = pts.get(i);

			// ms -> delay samples
			float relMs = timesMs[i] - t0;
			if (relMs < 0f) relMs = 0f;

			long delaySamples = (long) Math.round(relMs * 0.001f * sampleRate);
			if (delaySamples < 0L) delaySamples = 0L;

			// map -> index and pan
			int startIndex = mapping.sampleIndex(p);
			if (startIndex < 0) startIndex = 0;
			if (startIndex >= buf.length) startIndex = buf.length - 1;

			float pan = clampPan(mapping.pan(p));
			
			float pitchRatio = cfg.pitchRatio();    // pitch shifting
			// time hop stays as-is (so schedule timing doesn't change)
			int timeHop = hop;                      
			// source hop scaled by pitch ratio (whole-gesture pitch behavior)
			int indexHop = Math.max(1, Math.round(hop * pitchRatio));

			PASource src = new IndexGranularSource(
					buf,
					settings,
					startIndex,
					grainLen,
					timeHop,  // timeHopSamples
					indexHop,  // indexHopSamples (forward scan)
					numGrains,
					pitchRatio
					);

			gSynth.startAfterDelaySamples(src, amp, pan, env, false, delaySamples);

			scheduled++;
			
		}

		return scheduled;
	}

	// --------------------------------------------------------------------
	// Helpers
	// --------------------------------------------------------------------

	private static float clampPan(float p) {
		if (p < -1f) return -1f;
		if (p >  1f) return  1f;
		return p;
	}
}
