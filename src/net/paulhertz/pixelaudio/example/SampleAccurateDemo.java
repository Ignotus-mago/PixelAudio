package net.paulhertz.pixelaudio.example;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ddf.minim.AudioOutput;
import ddf.minim.Minim;
import ddf.minim.MultiChannelBuffer;
import net.paulhertz.pixelaudio.HilbertGen;
import net.paulhertz.pixelaudio.PixelAudioMapper;
import net.paulhertz.pixelaudio.curves.AudioBrush;
import net.paulhertz.pixelaudio.curves.GranularBrush;
import net.paulhertz.pixelaudio.curves.PABezShape;
import net.paulhertz.pixelaudio.curves.PACurveMaker;
import net.paulhertz.pixelaudio.curves.PACurveUtility;
import net.paulhertz.pixelaudio.curves.SamplerBrush;
import net.paulhertz.pixelaudio.granular.GestureEventParams;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig;
import net.paulhertz.pixelaudio.granular.GestureGranularParams;
import net.paulhertz.pixelaudio.granular.PAGranularInstrument;
import net.paulhertz.pixelaudio.granular.PAGranularInstrumentDirector;
import net.paulhertz.pixelaudio.sampler.ADSRParams;
import net.paulhertz.pixelaudio.sampler.PASamplerInstrumentPool;
import net.paulhertz.pixelaudio.schedule.AudioUtility;
import net.paulhertz.pixelaudio.schedule.GestureSchedule;
import net.paulhertz.pixelaudio.schedule.TimedLocation;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

/**
 * Sample-accurate Sampler/Granular timing demo.
 *
 * <p>
 * Sample accurate timing in PixelAudio in Sampler and Granular synths.
 * 
 * </p>
 */
public class SampleAccurateDemo extends PApplet {

    static final int DISPLAY_W = 1536;
    static final int DISPLAY_H = 1024;
    static final int GEN_W = 512;
    static final int GEN_H = 512;

    static final float REQUIRED_SAMPLE_RATE = 48000.0f;
    static final int AUDIO_BUFFER_SIZE = 1024;
    static final float SAMPLE_RATE_TOLERANCE = 0.5f;

    static final int BEAT_COUNT = 128;
    static final float DEFAULT_SAMPLER_INTERVAL_MS = 117.0f;
    static final float DEFAULT_GRANULAR_INTERVAL_MS = 117.0f;
    static final long SCHEDULE_LEAD_SAMPLES = 2048L;
    static final float BEAT_BRUSH_SIZE = 18.0f;

    static final String DEFAULT_AUDIO_PATH = "examples/examples_data/Bag/bag_1_newSpiral.wav";
    static final String DIST_AUDIO_PATH = "distribution/PixelAudio-1/examples/examples_data/Bag/bag_1_newSpiral.wav";

    Minim minim;
    AudioOutput audioOut;
    PixelAudioMapper mapper;
    PImage mapImage;
    int[] colors;

    MultiChannelBuffer playBuffer;
    float[] audioSignal;
    float bufferSampleRate = REQUIRED_SAMPLE_RATE;

    PASamplerInstrumentPool samplerPool;
    PAGranularInstrument granularSynth;
    PAGranularInstrumentDirector granularDirector;

    ADSRParams samplerEnv;
    ADSRParams granularEnv;
    GestureGranularConfig.Builder samplerBeatConfig;
    GestureGranularConfig.Builder granularBeatConfig;
    GestureGranularParams pointGranularParams;

    SamplerBrush samplerBeatBrush;
    GranularBrush granularBeatBrush;
    BeatBrushData samplerBeatData;
    BeatBrushData granularBeatData;
    final ArrayList<TimedLocation> samplerTimeLocs = new ArrayList<>();
    final ArrayList<TimedLocation> granularTimeLocs = new ArrayList<>();
    final ArrayList<TimedLocation> pointTimeLocs = new ArrayList<>();

    boolean audioReady = false;
    boolean wrapAround = true;
    boolean pointUsesGranular = false;
    boolean shiftIsDown = false;
    BrushKind activeBrushKind = BrushKind.NONE;
    BrushKind hoverBrushKind = BrushKind.NONE;

    float samplerIntervalMs = DEFAULT_SAMPLER_INTERVAL_MS;
    float granularIntervalMs = DEFAULT_GRANULAR_INTERVAL_MS;

    String audioStatus = "";
    String fileStatus = "";
    String actionStatus = "";

    public static void main(String[] args) {
        PApplet.main(new String[] { SampleAccurateDemo.class.getName() });
    }

    @Override
    public void settings() {
        size(DISPLAY_W, DISPLAY_H);
    }

    @Override
    public void setup() {
        surface.setTitle("SampleAccurateDemo");
        initMapping();
        initAudio();
        initSynths();
        rebuildBeatBrushes();
        preloadDefaultAudio();
    }

    void initMapping() {
        mapper = new PixelAudioMapper(HilbertGen.hilbertLoop3x2(GEN_W, GEN_H));
        colors = getColors(mapper.getSize());
        mapImage = createImage(width, height, ARGB);
        mapImage.loadPixels();
        Arrays.fill(mapImage.pixels, color(0));
        mapImage.updatePixels();
    }

    void initAudio() {
        minim = new Minim(this);
        audioOut = minim.getLineOut(Minim.STEREO, AUDIO_BUFFER_SIZE, REQUIRED_SAMPLE_RATE);
        enforceAudioSampleRate(audioOut, REQUIRED_SAMPLE_RATE);
        audioReady = true;
        audioStatus = "Audio: " + nf(audioOut.sampleRate(), 0, 1)
                + " Hz, buffer " + audioOut.bufferSize();
        println("---- " + audioStatus);
    }

    void initSynths() {
        samplerEnv = new ADSRParams(1.0f, 0.005f, 0.02f, 0.85f, 0.035f);
        granularEnv = new ADSRParams(1.0f, 0.005f, 0.02f, 0.875f, 0.025f);

        playBuffer = new MultiChannelBuffer(mapper.getSize(), 1);
        audioSignal = new float[mapper.getSize()];
        playBuffer.setChannel(0, audioSignal);

        samplerPool = new PASamplerInstrumentPool(playBuffer, bufferSampleRate, 16, 4, audioOut, samplerEnv);
        samplerPool.setGain(AudioUtility.dbToLinear(-6.0f));
        samplerPool.setWrapAround(wrapAround);

        granularSynth = new PAGranularInstrument(audioOut, granularEnv, 64);
        granularSynth.setGlobalGainDb(-3.0f);
        granularDirector = new PAGranularInstrumentDirector(granularSynth);

        samplerBeatConfig = new GestureGranularConfig.Builder().gainDb(-12.0f);
        samplerBeatConfig.pathMode = GestureGranularConfig.PathMode.ALL_POINTS;
        samplerBeatConfig.rdpEpsilon = 8.0f;
        samplerBeatConfig.curveSteps = 8;
        samplerBeatConfig.env = samplerEnv;

        granularBeatConfig = new GestureGranularConfig.Builder().gainDb(-9.0f);
        granularBeatConfig.hopLengthSamples = 512;
        granularBeatConfig.grainLengthSamples = 2048;
        granularBeatConfig.burstGrains = 4;
        granularBeatConfig.pathMode = GestureGranularConfig.PathMode.ALL_POINTS;
        granularBeatConfig.rdpEpsilon = 8.0f;
        granularBeatConfig.curveSteps = 8;
        granularBeatConfig.env = granularEnv;
        granularBeatConfig.wrapAround(wrapAround);

        pointGranularParams = granularParamsFromConfig(granularBeatConfig.build());
    }

    void preloadDefaultAudio() {
        File f = resolveDefaultAudioFile();
        if (f != null) {
            loadAudioFile(f);
        } else {
            fileStatus = "Default file not found: " + DEFAULT_AUDIO_PATH;
            println("---- " + fileStatus);
            renderAudioToImage();
        }
    }

    File resolveDefaultAudioFile() {
        File local = new File(DEFAULT_AUDIO_PATH);
        if (local.isFile()) return local;
        File dist = new File(DIST_AUDIO_PATH);
        if (dist.isFile()) return dist;
        File sketchLocal = new File(sketchPath(DEFAULT_AUDIO_PATH));
        if (sketchLocal.isFile()) return sketchLocal;
        return null;
    }
    
    @Override
    public void draw() {
        if (mapImage != null) image(mapImage, 0, 0);
        hoverBrushKind = hitBrush(mouseX, mouseY);
        drawBeatBrushes();
        runTimedLocationFeedback();
        drawStatus();
    }

    void drawBeatBrushes() {
        drawBeatBrush(samplerBeatBrush, color(255, 210, 70), hoverBrushKind == BrushKind.SAMPLER
                || activeBrushKind == BrushKind.SAMPLER);
        drawBeatBrush(granularBeatBrush, color(90, 220, 255), hoverBrushKind == BrushKind.GRANULAR
                || activeBrushKind == BrushKind.GRANULAR);
    }

   @Override
    public void mousePressed() {
        BrushKind hit = hitBrush(mouseX, mouseY);
        if (hit == BrushKind.NONE) {
            activeBrushKind = BrushKind.NONE;
            runPointEvent(mouseX, mouseY);
        } else {
            playBrush(hit);
        }
    }

    @Override
    public void keyPressed() {
        if (key == CODED && keyCode == SHIFT) {
            shiftIsDown = true;
            return;
        }
        if (key != CODED) {
            parseKey(key, keyCode);
        } else {
            switch (keyCode) {
            case UP:
                adjustAudioGain(shiftIsDown ? 3.0f : 1.0f);
                println("---- audio gain is " + nf(audioOut.getGain(), 0, 2) + " dB");
                break;
            case DOWN:
                adjustAudioGain(shiftIsDown ? -3.0f : -1.0f);
                println("---- audio gain is " + nf(audioOut.getGain(), 0, 2) + " dB");
                break;
            case RIGHT:
                BrushKind rightTarget = instrumentGainTarget();
                adjustInstrumentGain(rightTarget, 3.0f);
                println("---- " + rightTarget.label + " instrument gain is "
                        + nf(instrumentGainDb(rightTarget), 0, 2) + " dB");
                break;
            case LEFT:
                BrushKind leftTarget = instrumentGainTarget();
                adjustInstrumentGain(leftTarget, -3.0f);
                println("---- " + leftTarget.label + " instrument gain is "
                        + nf(instrumentGainDb(leftTarget), 0, 2) + " dB");
                break;
            default:
                break;
            }
        }
    }

    public void parseKey(char key, int keyCode) {
        switch (key) {
        case ' ':
            BrushKind target = (hoverBrushKind != BrushKind.NONE) ? hoverBrushKind : activeBrushKind;
            if (target == BrushKind.NONE) runPointEvent(mouseX, mouseY);
            else playBrush(target);
            break;
        case 's': case 'S':
            playSamplerBeatBrush();
            break;
        case 'g': case 'G':
            playGranularBeatBrush();
            break;
        case 'b': case 'B':
            playBothBeatBrushes();
            break;
        case 'd': case 'D':
        	playDoubleSamplerBeatBrush(64);
        	break;
        case 'w': case 'W':
            toggleWrapAround();
            break;
        case 'p': case 'P':
            pointUsesGranular = !pointUsesGranular;
            actionStatus = "Point synth = " + (pointUsesGranular ? "Granular" : "Sampler");
            break;
        case 'o': case 'O':
            chooseAudioFile();
            break;
        case 'h': case 'H':
            showHelp();
            break;
        case '1':
            setBeatRatio(1.0f, 1.0f);
            break;
        case '2':
            setBeatRatio(1.0f, 1.5f);
            break;
        case '3':
            setBeatRatio(1.0f, 2.6f);
            break;
        case '[':
            adjustSamplerInterval(-1.0f);
            break;
        case ']':
            adjustSamplerInterval(1.0f);
            break;
        case '{':
            adjustGranularInterval(-1.0f);
            break;
        case '}':
            adjustGranularInterval(1.0f);
            break;
        default:
            break;
        }
    }

    public void showHelp() {
        println(" * ----- SampleAccurateDemo Help -----");
        println(" * Click a brush to play it; click away from brushes to trigger a point event.");
        println(" * Press SPACE to play the hovered/active brush, or a point event if no brush is selected.");
        println(" * Press 's' or 'S' to play the Sampler beat brush.");
        println(" * Press 'g' or 'G' to play the Granular beat brush.");
        println(" * Press 'b' or 'B' to play both beat brushes together.");
        println(" * Press 'w' or 'W' to toggle wrapAround for Sampler and Granular playback.");
        println(" * Press 'p' or 'P' to toggle point events between Sampler and Granular.");
        println(" * Press 'o' or 'O' to choose an audio file.");
        println(" * Press '1' for unison beat intervals.");
        println(" * Press '2' for a 3:2 beat interval relation.");
        println(" * Press '3' for a 13:5 beat interval relation.");
        println(" * Press '[' or ']' to decrease/increase the Sampler interval by 1 ms.");
        println(" * Press '{' or '}' to decrease/increase the Granular interval by 1 ms.");
        println(" * Press UP/DOWN ARROW to adjust audioOut gain by 1 dB, or 3 dB with Shift.");
        println(" * Press RIGHT/LEFT ARROW to adjust Sampler instrument gain by 3 dB.");
        println(" * Press Shift-RIGHT/LEFT ARROW to adjust Granular instrument gain by 3 dB.");
        println(" * Press 'h' or 'H' to show this help.");
        actionStatus = "Help printed to console.";
    }

    @Override
    public void keyReleased() {
        if (key == CODED && keyCode == SHIFT) {
            shiftIsDown = false;
        }
    }

    public void adjustAudioGain(float gainDbDelta) {
        float nextGain = audioOut.getGain() + gainDbDelta;
        if (nextGain > 12.0f || nextGain < -64.0f) return;
        audioOut.setGain(nextGain);
        actionStatus = "Audio gain = " + nf(audioOut.getGain(), 0, 2) + " dB";
    }

    public void adjustInstrumentGain(float gainDbDelta) {
        adjustInstrumentGain(instrumentGainTarget(), gainDbDelta);
    }

    public void adjustInstrumentGain(BrushKind target, float gainDbDelta) {
        if (target == BrushKind.SAMPLER) {
            adjustSamplerGain(gainDbDelta);
        } else if (target == BrushKind.GRANULAR) {
            adjustGranularGain(gainDbDelta);
        }
    }

    public void adjustSamplerGain(float gainDbDelta) {
        float nextGain = samplerPool.getGainDb() + gainDbDelta;
        if (nextGain > 12.0f || nextGain < -64.0f) return;
        samplerPool.setGainDb(nextGain);
        actionStatus = "Sampler gain = " + nf(samplerPool.getGainDb(), 0, 2) + " dB";
    }

    public void adjustGranularGain(float gainDbDelta) {
        float nextGain = granularSynth.getGlobalGainDb() + gainDbDelta;
        if (nextGain > 12.0f || nextGain < -64.0f) return;
        granularSynth.setGlobalGainDb(nextGain);
        actionStatus = "Granular gain = " + nf(granularSynth.getGlobalGainDb(), 0, 2) + " dB";
    }

    public void chooseAudioFile() {
        selectInput("Choose an audio file:", "fileSelected");
    }

    public void fileSelected(File selectedFile) {
        if (selectedFile == null) {
            actionStatus = "No file selected.";
            return;
        }
        loadAudioFile(selectedFile);
    }

    void loadAudioFile(File audioFile) {
        MultiChannelBuffer loaded = new MultiChannelBuffer(1024, 1);
        float fileRate = minim.loadFileIntoBuffer(audioFile.getAbsolutePath(), loaded);
        if (fileRate <= 0) {
            fileStatus = "Could not load " + audioFile.getName();
            println("---- " + fileStatus);
            return;
        }

        float[] mono;
        if (abs(fileRate - audioOut.sampleRate()) > SAMPLE_RATE_TOLERANCE) {
            mono = AudioUtility.resampleMonoToOutput(loaded.getChannel(0), fileRate, audioOut);
            bufferSampleRate = audioOut.sampleRate();
        } else {
            mono = Arrays.copyOf(loaded.getChannel(0), loaded.getBufferSize());
            bufferSampleRate = fileRate;
        }

        updateAudioChain(mono, bufferSampleRate);
        renderAudioToImage();
        fileStatus = "Loaded " + audioFile.getName()
                + " (" + mono.length + " source samples, " + nf(bufferSampleRate, 0, 1) + " Hz)";
        actionStatus = "Ready.";
        println("---- " + fileStatus);
    }

    void updateAudioChain(float[] sig, float sampleRate) {
        int n = mapper.getSize();
        float[] canonical = new float[n];
        if (sig != null) {
            System.arraycopy(sig, 0, canonical, 0, Math.min(sig.length, n));
        }
        audioSignal = canonical;
        bufferSampleRate = sampleRate;
        if (playBuffer == null || playBuffer.getBufferSize() != n) {
            playBuffer = new MultiChannelBuffer(n, 1);
        }
        playBuffer.setChannel(0, audioSignal);
        if (samplerPool != null) {
            samplerPool.setBuffer(playBuffer, bufferSampleRate);
            samplerPool.setWrapAround(wrapAround);
        }
    }

    void renderAudioToImage() {
        if (mapImage == null || mapper == null || audioSignal == null) return;
        mapImage.loadPixels();
        mapper.mapSigToImg(audioSignal, mapImage.pixels, PixelAudioMapper.ChannelNames.ALL);
        PixelAudioMapper.applyColor(colors, mapImage.pixels, mapper.getImageToSignalLUT());
        mapImage.updatePixels();
    }

    int[] getColors(int size) {
        int[] colorWheel = new int[size];
        pushStyle();
        colorMode(HSB, colorWheel.length, 100, 100);
        for (int i = 0; i < colorWheel.length; i++) {
            colorWheel[i] = color(i, 30, 50);
        }
        popStyle();
        return colorWheel;
    }

    void rebuildBeatBrushes() {
        samplerBeatData = createBeatBrushData(BEAT_COUNT, samplerIntervalMs, false);
        granularBeatData = createBeatBrushData(BEAT_COUNT, granularIntervalMs, true);
        samplerBeatBrush = new SamplerBrush(samplerBeatData.curve, samplerBeatConfig);
        granularBeatBrush = new GranularBrush(granularBeatData.curve, granularBeatConfig);
    }

    BeatBrushData createBeatBrushData(int count, float intervalMs, boolean granular) {
        ArrayList<PVector> points = new ArrayList<>();
        ArrayList<Integer> roundedTimes = new ArrayList<>();
        float[] timesMs = new float[count];

        int xCtr = granular ? width / 4 : width * 3 / 4;
        int yCtr = height / 2;
        float rad = min(width / 4.0f, height / 2.0f) - 72.0f;
        float theta = granular ? PI / 61.0f : PI / 64.0f;
        float dec = (rad * 0.5f) / count;

        for (int i = 0; i < count; i++) {
            PVector p = (i == 0)
                    ? new PVector(xCtr, yCtr + rad)
                    : PACurveUtility.rotateCoordAroundPoint(xCtr, yCtr + rad, xCtr, yCtr, theta * i);
            points.add(p);
            timesMs[i] = i * intervalMs;
            roundedTimes.add(Math.round(timesMs[i]));
            rad -= dec;
        }

        PACurveMaker curve = PACurveMaker.buildCurveMaker(points, roundedTimes, millis());
        curve.setEpsilon(5.0f);
        curve.setCurveSteps(16);
        curve.setPolySteps(12);
        curve.setBrushSize(BEAT_BRUSH_SIZE);
        curve.setBrushColor(granular ? color(90, 220, 255, 96) : color(255, 210, 70, 96));
        curve.setCurveColor(granular ? color(90, 220, 255, 220) : color(255, 210, 70, 220));
        curve.setCurveWeight(2.0f);
        curve.setEventPointsColor(granular ? color(90, 220, 255, 180) : color(255, 210, 70, 180));
        curve.setEventPointsSize(4.0f);
        curve.calculateDerivedPoints();
        GestureSchedule schedule = new GestureSchedule(new ArrayList<>(points), timesMs);
        return new BeatBrushData(curve, schedule);
    }

    void playSamplerBeatBrush() {
        ensureReadyForPlayback();
        activeBrushKind = BrushKind.SAMPLER;
        long startSample = samplerPool.getCurrentSampleTime() + SCHEDULE_LEAD_SAMPLES;
        scheduleSamplerBrush(samplerBeatBrush, samplerBeatData.schedule,
                startSample);
        storeTimedLocations(samplerTimeLocs, samplerBeatData.schedule, visualStartMillis(startSample,
                samplerPool.getCurrentSampleTime(), samplerPool.getSampleRate()));
        actionStatus = "Sampler brush: " + nf(samplerIntervalMs, 0, 3) + " ms";
    }

    void playDoubleSamplerBeatBrush(int offsetSamples) {
        ensureReadyForPlayback();
        activeBrushKind = BrushKind.SAMPLER;
        long startSample = samplerPool.getCurrentSampleTime() + SCHEDULE_LEAD_SAMPLES;
        scheduleSamplerBrush(samplerBeatBrush, samplerBeatData.schedule,
                startSample);
        storeTimedLocations(samplerTimeLocs, samplerBeatData.schedule, visualStartMillis(startSample,
                samplerPool.getCurrentSampleTime(), samplerPool.getSampleRate()));
        startSample += offsetSamples;
        scheduleSamplerBrush(samplerBeatBrush, samplerBeatData.schedule,
                startSample);
        storeTimedLocations(samplerTimeLocs, samplerBeatData.schedule, visualStartMillis(startSample,
                samplerPool.getCurrentSampleTime(), samplerPool.getSampleRate()));
        actionStatus = "Sampler brush: " + nf(samplerIntervalMs, 0, 3) + " ms";
    }
   void playGranularBeatBrush() {
        ensureReadyForPlayback();
        activeBrushKind = BrushKind.GRANULAR;
        long startSample = granularSynth.getCurrentSampleTime() + SCHEDULE_LEAD_SAMPLES;
        scheduleGranularBrush(granularBeatBrush, granularBeatData.schedule,
                startSample);
        storeTimedLocations(granularTimeLocs, granularBeatData.schedule, visualStartMillis(startSample,
                granularSynth.getCurrentSampleTime(), granularSynth.getSampleRate()));
        actionStatus = "Granular brush: " + nf(granularIntervalMs, 0, 3) + " ms";
    }

    void playBothBeatBrushes() {
        ensureReadyForPlayback();
        long samplerStart = samplerPool.getCurrentSampleTime() + SCHEDULE_LEAD_SAMPLES;
        long granularStart = granularSynth.getCurrentSampleTime() + SCHEDULE_LEAD_SAMPLES;
        scheduleSamplerBrush(samplerBeatBrush, samplerBeatData.schedule, samplerStart);
        scheduleGranularBrush(granularBeatBrush, granularBeatData.schedule, granularStart);
        storeTimedLocations(samplerTimeLocs, samplerBeatData.schedule, visualStartMillis(samplerStart,
                samplerPool.getCurrentSampleTime(), samplerPool.getSampleRate()));
        storeTimedLocations(granularTimeLocs, granularBeatData.schedule, visualStartMillis(granularStart,
                granularSynth.getCurrentSampleTime(), granularSynth.getSampleRate()));
        actionStatus = "Both brushes scheduled together.";
    }

    void scheduleSamplerBrush(SamplerBrush brush, GestureSchedule sched, long startSample) {
        if (brush == null || sched == null || sched.isEmpty()) return;
        GestureGranularConfig snap = brush.snapshot();
        float gain = snap.gainLinear();
        float pitch = snap.pitchRatio();
        ADSRParams env = (snap.env != null) ? snap.env : samplerEnv;
        int len = sampleLenForMillis(90.0f);

        for (int i = 0; i < sched.size(); i++) {
            PVector p = sched.points.get(i);
            int x = constrain(Math.round(p.x), 0, width - 1);
            int y = constrain(Math.round(p.y), 0, height - 1);
            int samplePos = mapper.lookupSignalPos(x, y);
            float pan = map(x, 0, width - 1, -0.875f, 0.875f);
            long when = startSample + AudioUtility.millisToSamples(sched.timesMs[i], audioOut.sampleRate());
            samplerPool.startAtSampleTime(samplePos, len, gain, env, pitch, pan, when);
        }
    }

    void scheduleGranularBrush(GranularBrush brush, GestureSchedule sched, long startSample) {
        if (brush == null || sched == null || sched.isEmpty()) return;
        GestureGranularParams params = granularParamsFromConfig(brush.snapshot());
        GestureEventParams eventParams = eventParamsForSchedule(sched);
        granularDirector.playGestureAtSampleTime(audioSignal, sched, params, eventParams, startSample);
    }

    GestureEventParams eventParamsForSchedule(GestureSchedule sched) {
        int n = sched.size();
        int[] startIndices = mapper.lookupSignalPosArray(sched.points, 0, mapper.getSize());
        float[] pan = new float[n];
        for (int i = 0; i < n; i++) {
            PVector p = sched.points.get(i);
            pan[i] = map(p.x, 0, width - 1, -0.875f, 0.875f);
        }
        return GestureEventParams.builder(n).startIndices(startIndices).pan(pan).build();
    }

    GestureGranularParams granularParamsFromConfig(GestureGranularConfig config) {
        return GestureGranularParams.builder()
                .grainLengthSamples(config.grainLengthSamples)
                .hopLengthSamples(config.hopLengthSamples)
                .burstGrains(config.burstGrains)
                .gainLinear(config.gainLinear())
                .pitchRatio(config.pitchRatio())
                .pan(0.0f)
                .env(config.env)
                .looping(false)
                .wrapAround(wrapAround)
                .build();
    }

    void runPointEvent(int x, int y) {
        ensureReadyForPlayback();
        x = constrain(x, 0, width - 1);
        y = constrain(y, 0, height - 1);
        if (pointUsesGranular) {
            runGranularPointEvent(x, y);
        } else {
            runSamplerPointEvent(x, y);
        }
    }

    void runSamplerPointEvent(int x, int y) {
        int samplePos = mapper.lookupSignalPos(x, y);
        float pan = map(x, 0, width - 1, -0.875f, 0.875f);
        int len = sampleLenForMillis(180.0f);
        long when = samplerPool.getCurrentSampleTime() + SCHEDULE_LEAD_SAMPLES;
        samplerPool.startAtSampleTime(samplePos, len, AudioUtility.dbToLinear(-9.0f),
                samplerEnv, 1.0f, pan, when);
        pointTimeLocs.add(new TimedLocation(x, y, visualStartMillis(when,
                samplerPool.getCurrentSampleTime(), samplerPool.getSampleRate()), 180));
        actionStatus = "Sampler point at " + samplePos;
    }

    void runGranularPointEvent(int x, int y) {
        int samplePos = mapper.lookupSignalPos(x, y);
        List<PVector> pts = new ArrayList<>();
        float[] times = new float[8];
        int hop = Math.max(1, granularBeatConfig.hopLengthSamples);
        for (int i = 0; i < times.length; i++) {
            int idx = (samplePos + i * hop) % mapper.getSize();
            int[] xy = mapper.lookupImageCoordShifted(idx, 0);
            pts.add(new PVector(xy[0], xy[1]));
            times[i] = (float) AudioUtility.samplesToMillis((long) i * hop, audioOut.sampleRate());
        }
        GestureSchedule sched = new GestureSchedule(pts, times);
        GestureEventParams eventParams = eventParamsForSchedule(sched);
        long when = granularSynth.getCurrentSampleTime() + SCHEDULE_LEAD_SAMPLES;
        granularDirector.playGestureAtSampleTime(audioSignal, sched, pointGranularParams, eventParams, when);
        pointTimeLocs.add(new TimedLocation(x, y, visualStartMillis(when,
                granularSynth.getCurrentSampleTime(), granularSynth.getSampleRate()), 180));
        actionStatus = "Granular point at " + samplePos;
    }

    void ensureReadyForPlayback() {
        if (!audioReady || audioSignal == null || audioSignal.length == 0) {
            throw new IllegalStateException("Audio is not ready.");
        }
        samplerPool.setWrapAround(wrapAround);
        granularBeatConfig.wrapAround(wrapAround);
        pointGranularParams = granularParamsFromConfig(granularBeatConfig.build());
    }

    int sampleLenForMillis(float ms) {
        return Math.max(1, (int) AudioUtility.millisToSamples(ms, bufferSampleRate));
    }

    void toggleWrapAround() {
        wrapAround = !wrapAround;
        samplerPool.setWrapAround(wrapAround);
        granularBeatConfig.wrapAround(wrapAround);
        pointGranularParams = granularParamsFromConfig(granularBeatConfig.build());
        actionStatus = "wrapAround = " + wrapAround;
    }

    void setBeatRatio(float samplerRatio, float granularRatio) {
        samplerIntervalMs = DEFAULT_SAMPLER_INTERVAL_MS * samplerRatio;
        granularIntervalMs = DEFAULT_GRANULAR_INTERVAL_MS * granularRatio;
        rebuildBeatBrushes();
        actionStatus = "Beat ratio sampler:granular = "
                + nf(samplerRatio, 0, 3) + ":" + nf(granularRatio, 0, 3);
    }

    void adjustSamplerInterval(float deltaMs) {
        samplerIntervalMs = max(1.0f, samplerIntervalMs + deltaMs);
        rebuildBeatBrushes();
        actionStatus = "Sampler interval = " + nf(samplerIntervalMs, 0, 3) + " ms";
    }

    void adjustGranularInterval(float deltaMs) {
        granularIntervalMs = max(1.0f, granularIntervalMs + deltaMs);
        rebuildBeatBrushes();
        actionStatus = "Granular interval = " + nf(granularIntervalMs, 0, 3) + " ms";
    }

    void drawBeatBrush(AudioBrush brush, int brushColor, boolean highlighted) {
        if (brush == null) return;
        PACurveMaker curve = brush.curve();
        int fillColor = highlighted ? color(red(brushColor), green(brushColor), blue(brushColor), 132)
                : color(red(brushColor), green(brushColor), blue(brushColor), 82);
        int strokeColor = highlighted ? color(red(brushColor), green(brushColor), blue(brushColor), 230)
                : color(red(brushColor), green(brushColor), blue(brushColor), 150);
        curve.setCurveSteps(brush.cfg().curveSteps);
        curve.brushDraw(this, fillColor, strokeColor, highlighted ? 2.5f : 1.25f);
        drawBrushPathOverlay(curve, brush.cfg().pathMode, highlighted, strokeColor);
    }

    void drawBrushPathOverlay(PACurveMaker curve, GestureGranularConfig.PathMode pathMode,
            boolean highlighted, int strokeColor) {
        int pointColor = highlighted ? color(255, 255, 255, 210) : color(255, 255, 255, 120);
        switch (pathMode) {
        case REDUCED_POINTS:
            curve.reducedPointsDraw(this, strokeColor, highlighted ? 1.75f : 1.0f);
            break;
        case CURVE_POINTS:
            curve.curveDraw(this);
            curve.eventPointsDraw(this, pointColor, highlighted ? 5.0f : 3.5f);
            break;
        case ALL_POINTS:
        default:
            curve.dragPointsDraw(this, strokeColor, highlighted ? 1.75f : 1.0f);
            curve.eventPointsDraw(this, pointColor, highlighted ? 5.0f : 3.5f);
            break;
        }
    }

    void storeTimedLocations(ArrayList<TimedLocation> events, GestureSchedule sched, int startTimeMs) {
        if (events == null || sched == null || sched.isEmpty()) return;
        int n = Math.min(sched.points.size(), sched.timesMs.length);
        for (int i = 0; i < n; i++) {
            PVector p = sched.points.get(i);
            events.add(new TimedLocation(Math.round(p.x), Math.round(p.y),
                    startTimeMs + Math.round(sched.timesMs[i]), 180));
        }
        Collections.sort(events);
    }

    int visualStartMillis(long startSample, long currentSample, float sampleRate) {
        long delaySamples = Math.max(0L, startSample - currentSample);
        return millis() + (int) Math.round(AudioUtility.samplesToMillis(delaySamples, sampleRate));
    }

    void runTimedLocationFeedback() {
        drawTimedLocationFeedback(granularTimeLocs, color(199, 55, 34), 17.0f);
        drawTimedLocationFeedback(samplerTimeLocs, color(34, 55, 199), 15.0f);
        drawTimedLocationFeedback(pointTimeLocs, pointUsesGranular ? color(90, 220, 255) : color(255, 210, 70), 20.0f);
    }

    void drawTimedLocationFeedback(ArrayList<TimedLocation> events, int eventColor, float maxRadius) {
        if (events == null || events.isEmpty()) return;
        int now = millis();
        pushStyle();
        noFill();
        strokeWeight(2.0f);
        for (Iterator<TimedLocation> it = events.iterator(); it.hasNext();) {
            TimedLocation tl = it.next();
            int low = tl.eventTime();
            int high = low + Math.max(1, tl.getDurationMs());
            if (now >= high) {
                tl.setStale(true);
                it.remove();
            } else if (now >= low) {
                float u = constrain((now - low) / (float) Math.max(1, tl.getDurationMs()), 0.0f, 1.0f);
                float r = 4.0f + maxRadius * u;
                int alpha = Math.round(230.0f * (1.0f - u));
                stroke(eventColor, alpha);
                ellipse(tl.getX(), tl.getY(), r, r);
                fill(eventColor, Math.max(32, alpha / 2));
                ellipse(tl.getX(), tl.getY(), 5.0f, 5.0f);
                noFill();
            } else {
                break;
            }
        }
        popStyle();
    }

    BrushKind hitBrush(int x, int y) {
        if (isNearBrush(samplerBeatData, x, y)) return BrushKind.SAMPLER;
        if (isNearBrush(granularBeatData, x, y)) return BrushKind.GRANULAR;
        return BrushKind.NONE;
    }

    boolean isNearBrush(BeatBrushData data, int x, int y) {
        if (data == null || data.curve == null) return false;
        return PABezShape.pointInPoly(data.curve.getBrushPoly(), x, y);
    }

    void playBrush(BrushKind kind) {
        if (kind == BrushKind.SAMPLER) {
            activeBrushKind = BrushKind.SAMPLER;
            playSamplerBeatBrush();
        } else if (kind == BrushKind.GRANULAR) {
            activeBrushKind = BrushKind.GRANULAR;
            playGranularBeatBrush();
        }
    }


    BrushKind currentInstrumentKind() {
        if (activeBrushKind == BrushKind.SAMPLER || activeBrushKind == BrushKind.GRANULAR) {
            return activeBrushKind;
        }
        return pointUsesGranular ? BrushKind.GRANULAR : BrushKind.SAMPLER;
    }

    float currentInstrumentGainDb() {
        return currentInstrumentKind() == BrushKind.GRANULAR
                ? granularSynth.getGlobalGainDb()
                : samplerPool.getGainDb();
    }

    BrushKind instrumentGainTarget() {
        return shiftIsDown ? BrushKind.GRANULAR : BrushKind.SAMPLER;
    }

    float instrumentGainDb(BrushKind target) {
        return target == BrushKind.GRANULAR
                ? granularSynth.getGlobalGainDb()
                : samplerPool.getGainDb();
    }

    void drawStatus() {
        pushStyle();
        noStroke();
        fill(0, 180);
        rect(16, 16, 560, 202, 4);
        fill(audioReady ? color(220, 245, 230) : color(255, 190, 170));
        textSize(15);
        textLeading(20);
        String status = audioStatus + "\n"
                + fileStatus + "\n"
                + "Granular brush left, Sampler brush right\n"
                + "wrapAround: " + wrapAround
                + "    point synth: " + (pointUsesGranular ? "Granular" : "Sampler") + "\n"
                + "hover: " + hoverBrushKind.label
                + "    active: " + activeBrushKind.label + "\n"
                + "Audio gain: " + nf(audioOut.getGain(), 0, 2) + " dB"
                + "    gain target: " + instrumentGainTarget().label + "\n"
                + "Sampler gain: " + nf(samplerPool.getGainDb(), 0, 2) + " dB"
                + "    Granular gain: " + nf(granularSynth.getGlobalGainDb(), 0, 2) + " dB\n"
                + "Sampler interval: " + nf(samplerIntervalMs, 0, 3) + " ms"
                + "    Granular interval: " + nf(granularIntervalMs, 0, 3) + " ms\n"
                + actionStatus;
        text(status, 28, 40, 532, 170);
        popStyle();
    }

    void enforceAudioSampleRate(AudioOutput out, float requiredRate) {
        if (out == null) {
            throw new IllegalStateException("AudioOutput could not be created.");
        }

        float actualRate = out.sampleRate();
        if (abs(actualRate - requiredRate) <= SAMPLE_RATE_TOLERANCE) {
            return;
        }

        audioReady = false;
        audioStatus = "SampleAccurateDemo requires " + nf(requiredRate, 0, 1)
                + " Hz, but Minim provided " + nf(actualRate, 0, 1)
                + " Hz. Set the audio device to " + nf(requiredRate, 0, 1)
                + " Hz and restart.";
        println("---- " + audioStatus);
        noLoop();
        throw new IllegalStateException(audioStatus);
    }

    @Override
    public void stop() {
        if (samplerPool != null) samplerPool.stopAll();
        if (granularSynth != null) granularSynth.close();
        if (minim != null) minim.stop();
        super.stop();
    }

    static final class BeatBrushData {
        final PACurveMaker curve;
        final GestureSchedule schedule;

        BeatBrushData(PACurveMaker curve, GestureSchedule schedule) {
            this.curve = curve;
            this.schedule = schedule;
        }
    }

    enum BrushKind {
        NONE("none"),
        SAMPLER("Sampler"),
        GRANULAR("Granular");

        final String label;

        BrushKind(String label) {
            this.label = label;
        }
    }
}
