package net.paulhertz.pixelaudio.example;

import java.io.File;
import java.util.Arrays;

import ddf.minim.MultiChannelBuffer;

import net.paulhertz.pixelaudio.PixelAudioMapper;
import net.paulhertz.pixelaudio.WindowedBuffer;
import net.paulhertz.pixelaudio.curves.PACurveMaker;
import net.paulhertz.pixelaudio.granular.GestureEventParams;
import net.paulhertz.pixelaudio.granular.GestureGranularParams;
import net.paulhertz.pixelaudio.schedule.AudioUtility;
import net.paulhertz.pixelaudio.schedule.GestureSchedule;
import net.paulhertz.pixelaudio.schedule.TimedLocation;
import net.paulhertz.pixelaudio.sampler.ADSRParams;
import net.paulhertz.pixelaudio.sampler.PASamplerInstrumentPool;

import processing.core.PApplet;
import processing.core.PVector;

/**
 * TutorialOne_06_WB_Retrofit
 *
 * A continuation of TutorialOne_03_Drawing that keeps the drawing, brush,
 * sampler, granular, bounds, and envelope code from TutorialOne_03_Drawing,
 * but changes the audio source model:
 *
 *   - anthemSignal / anthemBuffer hold the entire loaded audio file.
 *   - windowBuff is a moving window over anthemSignal.
 *   - audioSignal / playBuffer / mapImage hold only the current visible window.
 *   - Display coordinates map first to the visible window, then to the backing
 *     full-file source by adding windowBuff.getIndex().
 *
 * This file is intentionally small: it is a retrofit layer over the newer
 * TutorialOne_03_Drawing rather than a refactor of the older TutorialOne_06 code.
 * It is not ported to Processing. The programming model in Eclipse provides better
 * support for the Java classes than Processing does. 
 * 
 * See {@link net.paulhertz.pixelaudio.example.TutorialOne_03_Drawing TutorialOne_03_Drawing}
 */
public class TutorialOne_06_WB_Retrofit extends TutorialOne_03_Drawing {

    /** Full-file mono source, padded to at least mapper.getSize(). */
    float[] anthemSignal;
    /** Full-file source buffer used by the Sampler instrument pool. */
    MultiChannelBuffer anthemBuffer;
    /** Moving view onto anthemSignal, with window size == mapper.getSize(). */
    WindowedBuffer windowBuff;

    /** Hop size for automatic window traversal. */
    int windowHopSize = 64;
    /** If true, draw() advances windowBuff each frame and refreshes the display. */
    boolean isWindowing = false;
    /** Save animation state when file loading or windowing temporarily suspends it. */
    boolean oldIsWindowing = false;

    /** Optional random point-event texture while windowing. */
    boolean isRaining = false;
    
    /** WindowBuffer initialized or not?  see initAudio() */
    boolean windowAudioReady = false;
    /** Sampler instruments use anthemBuffer, not the current visible playBuffer. */
    MultiChannelBuffer samplerSourceRef = null;
    /** sampling rate of samplerSourceRef */
    float samplerSourceRateRef = -1;
    
    // variables for our testing and debugging process
    int poolSize = 2;
    int samplerMaxVoices = 64;
    // float samplerGain = 0.25f;

    
	/*----------------------------------------------------------------*/
	/*                         APPLICATION                            */
	/*----------------------------------------------------------------*/
    
    public static void main(String[] args) {
        PApplet.main(new String[] { TutorialOne_06_WB_Retrofit.class.getName() });
    }

    @Override
    public void setup() {
        super.setup();
        surface.setTitle("PixelAudio Tutorial One 06: WB Retrofit");
        // can we load a long file from here?
        // preloadFiles(daPath, "_sonic/FullMoonTonight_22050Hz.mp3");
  }

    /**
     * Initializes audio using the TutorialOne_03_Drawing defaults, then creates
     * an initial full-file backing source from the empty playBuffer.
     */
    @Override
    public void initAudio() {
    	windowAudioReady = false;
        super.initAudio();
        installBackingSource(playBuffer.getChannel(0), audioOut.sampleRate());
        refreshWindowFromBacking(false);
        windowAudioReady = true;
        ensureSamplerReady();
        ensureGranularReady();
    }

    /**
     * Loads an audio file through TutorialOne_03_Drawing's normal path.
     * The override of updateAudioChain(...) below receives the full resampled
     * signal and installs it as the backing source instead of truncating it
     * before instrument playback.
     */
    @Override
    public void loadAudioFile(File audFile) {
        println("-- local loadAudioFile ");

        boolean wasAnimating = isAnimating;
        boolean wasWindowing = isWindowing;
        boolean wasBlending = isBlending;

        isAnimating = false;
        isWindowing = false;
        isBlending = false;

        // hard stop old sampler/granular activity before replacing buffers
        suspendWindowAudio();

        super.loadAudioFile(audFile);

        isBlending = wasBlending;
        isAnimating = wasAnimating;
        isWindowing = wasWindowing;
    }

    void suspendWindowAudio() {
        if (samplerTimeLocs != null) samplerTimeLocs.clear();
        if (pointTimeLocs != null) pointTimeLocs.clear();
        if (grainTimeLocs != null) grainTimeLocs.clear();

        if (pool != null) {
            pool.stopAll();
            pool.close();
            pool = null;
        }

        samplerSourceRef = null;
        samplerSourceRateRef = -1;

        if (gDir != null) gDir.cancelAndReleaseAll();
    }
    /**
     * Commit a new full-file source, then expose only the current mapSize window
     * through audioSignal/playBuffer/mapImage.
     */
    @Override
    void updateAudioChain(float[] sig, float sourceSampleRate) {
        installBackingSource(sig, sourceSampleRate);
        refreshWindowFromBacking(false);
        // critical: rebind instruments after anthemBuffer/anthemSignal change
        ensureSamplerReady();
        ensureGranularReady();
    }
    
    @Override
    void updateAudioChain(float[] sig) {
        float sr = (audioOut != null) ? audioOut.sampleRate() : sampleRate;
        updateAudioChain(sig, sr);
    }

    /**
     * Installs a full-file backing source for WindowedBuffer and instruments.
     * Sets the window size to mapper.getSize() 
     */
    void installBackingSource(float[] sig, float sourceSampleRate) {
        int windowSize = (mapper != null) ? mapper.getSize() : mapSize;
        if (windowSize <= 0) return;

        int backingSize = Math.max(windowSize, (sig != null) ? sig.length : 0);
        float[] backing = new float[backingSize];
        if (sig != null) {
            System.arraycopy(sig, 0, backing, 0, Math.min(sig.length, backing.length));
        }

        anthemSignal = backing;
        anthemBuffer = new MultiChannelBuffer(backing.length, 1);
        anthemBuffer.setChannel(0, anthemSignal);

        bufferSampleRate = sourceSampleRate;
        audioFileLength = backing.length;
        windowHopSize = Math.max(1, Math.round(sourceSampleRate / Math.max(1f, frameRate)));
        windowBuff = new WindowedBuffer(anthemSignal, windowSize, windowHopSize);

        granSignal = anthemSignal;

        println("---- WindowBuffer backing source length = " + anthemSignal.length
                + ", window size = " + windowBuff.getWindowSize()
                + ", hop size = " + windowBuff.getHopSize());
    }

    /**
     * Copies the current window into audioSignal and playBuffer. The Sampler and
     * Granular instruments still read from anthemSignal / anthemBuffer.
     */
    void refreshWindowFromBacking(boolean advance) {
        if (windowBuff == null || anthemSignal == null) return;

        if (advance) {
            windowBuff.nextWindow(); // advances exactly once
        }

        int targetSize = mapper.getSize();
        int start = PixelAudioMapper.wrap(windowBuff.getIndex(), anthemSignal.length);

        if (audioSignal == null || audioSignal.length != targetSize) {
            audioSignal = new float[targetSize];
        }

        for (int i = 0; i < targetSize; i++) {
            audioSignal[i] = anthemSignal[(start + i) % anthemSignal.length];
        }

        audioLength = audioSignal.length;

        if (playBuffer == null || playBuffer.getBufferSize() != targetSize) {
            playBuffer = new MultiChannelBuffer(targetSize, 1);
        }

        playBuffer.setChannel(0, audioSignal);

        totalShift = 0;
        renderAudioToMapImage(chan, 0);
        commitMapImageToBaseImage();
    }
    
 
    @Override
    void ensureSamplerReady() {
        // Prevent super.initAudio() from creating a sampler pool before
        // anthemBuffer has been installed.
        if (!windowAudioReady) return;

        MultiChannelBuffer source = (anthemBuffer != null) ? anthemBuffer : playBuffer;
        float sr = (bufferSampleRate > 0) ? bufferSampleRate : sampleRate;
        if (source == null) return;

        boolean needsRebuild =
                pool == null
                || pool.isClosed()
                || source != samplerSourceRef
                || sr != samplerSourceRateRef;

        if (needsRebuild) {
            if (pool != null && !pool.isClosed()) {
                pool.stopAll();
                pool.close();
            }
            pool = new PASamplerInstrumentPool(
                    source,
                    sr,
                    poolSize,
                    samplerMaxVoices,
                    audioOut,
                    defaultEnv
            );
            samplerSourceRef = source;
            samplerSourceRateRef = sr;
        }
        else {
            // Important: keep the whole pool bound to anthemBuffer.
            // Do not rely on playSample(anthemBuffer, ...) to patch one instrument at a time.
            pool.setBuffer(source, sr);
        }

        pool.setGain(samplerGain);
    }
    
    
    /** Granular gestures use anthemSignal as their source. */
    @Override
    void ensureGranularReady() {
        super.ensureGranularReady();
        if (anthemSignal != null) granSignal = anthemSignal;
    }

    /**
     * Display coordinate -> visible window index -> backing file index.
     */
    @Override
    public int getSamplePos(int x, int y) {
        int local = mapper.lookupSignalPosShifted(x, y, totalShift);
        return backingIndexFromLocal(local);
    }

    int backingIndexFromLocal(int localIndex) {
        if (windowBuff == null || anthemSignal == null || anthemSignal.length == 0) {
            return PixelAudioMapper.wrap(localIndex, Math.max(1, mapper.getSize()));
        }
        int local = PixelAudioMapper.wrap(localIndex, mapper.getSize());
        return PixelAudioMapper.wrap(windowBuff.getIndex() + local, anthemSignal.length);
    }

    /**
     * Backing-file index -> visible display coordinate, relative to the current window.
     */
    @Override
    public PVector getCoordFromSignalPos(int pos) {
        int local = pos;
        if (windowBuff != null && anthemSignal != null && anthemSignal.length > 0) {
            local = PixelAudioMapper.wrap(pos - windowBuff.getIndex(), mapper.getSize());
        }
        int[] xy = mapper.lookupImageCoordShifted(local, totalShift);
        return new PVector(xy[0], xy[1]);
    }

    /**
     * Point clicks are copied from TutorialOne_03_Drawing, but the initial
     * samplePos is the backing source index. Long granular bursts still create
     * display points in the current window; playGranularGesture(...) remaps
     * them to backing indices.
     */
    @Override
    public int handleClickOutsideBrush(int x, int y) {
        int localSamplePos = mapper.lookupSignalPosShifted(x, y, totalShift);
        samplePos = backingIndexFromLocal(localSamplePos);

        if (!useGranularSynth) {
            ensureSamplerReady();
            float panning = map(x, 0, width, -0.8f, 0.8f);
            int len = calcSampleLen();
            samplelen = playSample(samplePos, len, samplerPointGain, defaultEnv, panning);
            int durationMS = (int)(samplelen / sampleRate * 1000);
            pointTimeLocs.add(new TimedLocation(x, y, durationMS + millis() + 50));
            return durationMS;
        }

        ensureGranularReady();
        float hopMsF = (int)Math.round(AudioUtility.samplesToMillis(hopSamples, sampleRate));
        final int grainCount = useLongBursts
                ? 2 * (int)Math.round(noteDuration / hopMsF)
                : (int)Math.round(noteDuration / hopMsF);
        println("-- granular point burst with grainCount = " + grainCount);

        java.util.ArrayList<PVector> path = new java.util.ArrayList<>(grainCount);
        int[] timing = new int[grainCount];
        for (int i = 0; i < grainCount; i++) {
            if (useLongBursts) {
                int local = PixelAudioMapper.wrap(localSamplePos + hopSamples * i, mapper.getSize());
                path.add(super.getCoordFromSignalPos(local));
            }
            else {
                path.add(this.jitterCoord(x, y, 3));
            }
            timing[i] = Math.round(i * hopMsF);
        }

        int startTime = millis() + 10;
        PACurveMaker curve = PACurveMaker.buildCurveMaker(path);
        curve.setDragTimes(timing);
        curve.setTimeStamp(startTime);
        GestureSchedule sched = curve.getAllPointsSchedule();
        playGranularGesture(backingGranularSignal(), sched, gParamsFixed, 1.0f);
        storeGranularCurveTL(sched, startTime, false);
        return curve.getTimeOffset();
    }

    /**
     * Brush granular playback uses the full backing signal and backing indices.
     */
    @Override
    void scheduleGranularBrushClick(AudioBrushLite b) {
        if (b == null) return;
        java.util.ArrayList<PVector> pts = getPathPoints(b);
        if (pts == null || pts.size() < 2) return;
        ensureGranularReady();
        boolean isGesture = (b.hopMode() == HopMode.GESTURE);
        GestureGranularParams gParams = isGesture ? gParamsGesture : gParamsFixed;
        GestureSchedule sched = getPlaybackScheduleForBrush(b);
        playGranularGesture(backingGranularSignal(), sched, gParams, b.pitchRatio());
        storeGranularCurveTL(sched, millis() + 10, isGesture);
    }

    float[] backingGranularSignal() {
        return (anthemSignal != null && anthemSignal.length > 0) ? anthemSignal : audioSignal;
    }

    /**
     * Same as TutorialOne_03_Drawing, but startIndices are offset by the current
     * WindowedBuffer origin so grains read from the full backing file.
     */
    @Override
    public void playGranularGesture(float[] buf, GestureSchedule sched, GestureGranularParams params, float pitchRatio) {
        if (sched == null || sched.isEmpty()) return;
        float[] source = (buf != null) ? buf : backingGranularSignal();
        int sourceLen = (source != null && source.length > 0) ? source.length : mapper.getSize();

        int[] localIndices = mapper.lookupSignalPosArray(sched.points, totalShift, mapper.getSize());
        int[] startIndices = new int[localIndices.length];
        for (int i = 0; i < localIndices.length; i++) {
            startIndices[i] = (windowBuff != null)
                    ? PixelAudioMapper.wrap(windowBuff.getIndex() + localIndices[i], sourceLen)
                    : PixelAudioMapper.wrap(localIndices[i], sourceLen);
        }

        float[] panPerGrain = new float[sched.size()];
        for (int i = 0; i < sched.size(); i++) {
            PVector p = sched.points.get(i);
            panPerGrain[i] = map(p.x, 0, width - 1, -0.875f, 0.875f);
        }

        float jitter = usePitchedGrains ? 0.25f : 0f;
        float[] pitch = generateJitterPitch(sched.size(), jitter, pitchRatio);
        GestureEventParams eventParams = GestureEventParams.builder(sched.size())
                .startIndices(startIndices)
                .pan(panPerGrain)
                .pitchRatio(pitch)
                .build();
        gDir.playGestureNow(source, sched, params, eventParams);
    }
    
    @Override
    public int playSample(int samplePos, int samplelen, float amplitude,
                          ADSRParams env, float pitch, float pan) {
        if (anthemBuffer == null || pool == null) {
            return super.playSample(samplePos, samplelen, amplitude, env, pitch, pan);
        }

        int pos = PixelAudioMapper.wrap(samplePos, anthemBuffer.getBufferSize());
        int len = Math.min(samplelen, anthemBuffer.getBufferSize() - pos);

        println("---- 06 playSample pos=" + pos
            + " len=" + len
            + " anthemBuffer=" + anthemBuffer.getBufferSize()
            + " windowIndex=" + windowBuff.getIndex());

        return pool.playSample(pos, len, amplitude, env, pitch, pan);
    }
    
    /** Advance the visible audio window during draw(). */
    @Override
    public void draw() {
        if (isWindowing && windowBuff != null) {
            refreshWindowFromBacking(true);
            if (isRaining) raindrops(3);
        }
        super.draw();
    }

    /**
     * Add WindowBuffer controls, then pass all other commands to TutorialOne_03_Drawing.
     *
     * T     toggle automatic window traversal
     * { }   jump backward / forward one whole window
     * [ ]   jump backward / forward one half window
     * R     rewind the audio window
     * Y     toggle random point events while windowing
     */
    @Override
    public void parseKey(char key, int keyCode) {
        switch (key) {
        case 'T':
            isWindowing = !isWindowing;
            if (isWindowing) isAnimating = false;
            totalShift = 0;
            println("---- WindowBuffer traversal is " + (isWindowing ? "ON" : "OFF"));
            return;
        case '}':
            moveAudioWindow(windowBuff != null ? windowBuff.getWindowSize() : mapper.getSize());
            return;
        case '{':
            moveAudioWindow(windowBuff != null ? -windowBuff.getWindowSize() : -mapper.getSize());
            return;
        case ')':
            moveAudioWindow(windowBuff != null ? windowBuff.getWindowSize() / 2 : mapper.getSize() / 2);
            return;
        case '(':
            moveAudioWindow(windowBuff != null ? -windowBuff.getWindowSize() / 2 : -mapper.getSize() / 2);
            return;
        case 'R':
            resetAudioWindow();
            return;
        case 'Y':
            isRaining = !isRaining;
            println("---- WindowBuffer raindrops are " + (isRaining ? "ON" : "OFF"));
            return;
        default:
            super.parseKey(key, keyCode);
        }
    }

    public void resetAudioWindow() {
        if (windowBuff == null) return;
        windowBuff.reset();
        refreshWindowFromBacking(false);
    }

    public void moveAudioWindow(int delta) {
        if (windowBuff == null || anthemSignal == null || anthemSignal.length == 0) return;
        int next = PixelAudioMapper.wrap(windowBuff.getIndex() + delta, anthemSignal.length);
        windowBuff.setIndex(next);   // or equivalent setter in promoted WindowedBuffer
        refreshWindowFromBacking(false);
    }
    
    /**
     * Random point events along the upper part of the current window, useful for
     * hearing time-structured files while the window is advancing.
     */
    public void raindrops(int count) {
        for (int i = 0; i < count; i++) {
            if (random(1) < 0.35f) {
                int x = (int)random(width);
                int y = (int)random(Math.max(1, height / 8));
                handleClickOutsideBrush(x, y);
            }
        }
    }

    @Override
    public void showHelp() {
        super.showHelp();
        println("\nWindowBuffer additions:");
        println("Press 'T' to toggle automatic WindowBuffer traversal.");
        println("Press '{' or '}' to jump back/forward one full window.");
        println("Press '(' or ')' to jump back/forward one half window.");
        println("Press 'R' to rewind the WindowBuffer.");
        println("Press 'Y' to toggle raindrop point events while windowing.\n");
    }
}
