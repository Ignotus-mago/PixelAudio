package net.paulhertz.pixelaudio.granular;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.paulhertz.pixelaudio.sampler.ADSRParams;
import processing.data.JSONObject;

/**
 * JSON read/write support for GestureGranularConfig.Builder.
 *
 * Filename convention suggestion:
 *   gesture_001_softGranular_config.json
 */
public final class GestureGranularConfigIO {

    private GestureGranularConfigIO() {}

    public static final String FORMAT = "net.paulhertz.pixelaudio.brushconfig";
    public static final int VERSION = 1;

    public enum InstrumentType {
        GRANULAR,
        SAMPLER
    }

    public static final class Meta {
        public String id;
        public String name;
        public String description;
        public String notes;
        public InstrumentType instrumentType = InstrumentType.GRANULAR;
        public String linkedGesturePath;
    }

    public static final class Result {
        public final GestureGranularConfig.Builder builder;
        public final String id;
        public final String name;
        public final String description;
        public final String notes;
        public final InstrumentType instrumentType;
        public final String linkedGesturePath;

        public Result(GestureGranularConfig.Builder builder,
                      String id,
                      String name,
                      String description,
                      String notes,
                      InstrumentType instrumentType,
                      String linkedGesturePath) {
            this.builder = builder;
            this.id = id;
            this.name = name;
            this.description = description;
            this.notes = notes;
            this.instrumentType = instrumentType;
            this.linkedGesturePath = linkedGesturePath;
        }
    }

    public static JSONObject toJson(GestureGranularConfig.Builder b, Meta meta) {
        if (b == null) throw new IllegalArgumentException("builder cannot be null");
        if (meta == null) meta = new Meta();

        JSONObject root = new JSONObject();

        JSONObject header = new JSONObject();
        header.setString("format", FORMAT);
        header.setInt("version", VERSION);
        header.setString("type", "GestureGranularConfig.Builder");
        root.setJSONObject("header", header);

        JSONObject cfg = new JSONObject();
        cfg.setString("id", nonNull(meta.id, "config"));
        putIfPresent(cfg, "name", meta.name);
        putIfPresent(cfg, "description", meta.description);
        putIfPresent(cfg, "notes", meta.notes);
        cfg.setString("instrumentType", meta.instrumentType.name());

        cfg.setString("pathMode", b.pathMode.name());
        cfg.setFloat("rdpEpsilon", b.rdpEpsilon);
        cfg.setInt("curveSteps", b.curveSteps);
        cfg.setFloat("curveBias", b.curveBias);

        cfg.setString("hopMode", b.hopMode.name());
        cfg.setInt("hopLengthSamples", b.hopLengthSamples);

        cfg.setString("timingMode", b.timingMode.name());
        cfg.setInt("basePointCount", b.basePointCount);
        cfg.setInt("baseDurationMs", b.baseDurationMs);
        cfg.setInt("resampleCount", b.resampleCount);
        cfg.setInt("targetDurationMs", b.targetDurationMs);
        cfg.setString("warpShape", b.warpShape.name());
        cfg.setFloat("warpExponent", b.warpExponent);

        cfg.setInt("grainLengthSamples", b.grainLengthSamples);
        cfg.setJSONObject("env", adsrToJson(b.env));
        cfg.setFloat("gainDb", b.gainDb);
        cfg.setFloat("pitchSemitones", b.pitchSemitones);

        cfg.setInt("burstGrains", b.burstGrains);
        cfg.setBoolean("autoBurstGainComp", b.autoBurstGainComp);
        cfg.setBoolean("useArcLengthTime", b.useArcLengthTime);

        if (meta.linkedGesturePath != null && !meta.linkedGesturePath.isBlank()) {
            JSONObject linked = new JSONObject();
            linked.setString("path", meta.linkedGesturePath);
            cfg.setJSONObject("linkedGesture", linked);
        }

        root.setJSONObject("config", cfg);
        return root;
    }

    public static void write(File file, GestureGranularConfig.Builder builder, Meta meta) throws IOException {
        if (file == null) throw new IllegalArgumentException("file cannot be null");
        Files.writeString(file.toPath(), toJson(builder, meta).toString(), StandardCharsets.UTF_8);
    }

    public static Result read(File file) throws IOException {
        if (file == null) throw new IllegalArgumentException("file cannot be null");
        String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        return fromJson(JSONObject.parse(json));
    }

    public static Result fromJson(JSONObject root) {
        if (root == null) throw new IllegalArgumentException("root cannot be null");

        JSONObject header = root.getJSONObject("header");
        requireHeader(header, FORMAT, VERSION);

        JSONObject cfg = root.getJSONObject("config");
        if (cfg == null) throw new IllegalArgumentException("Missing config object");

        GestureGranularConfig.Builder b = new GestureGranularConfig.Builder();

        if (cfg.hasKey("pathMode")) b.pathMode = GestureGranularConfig.PathMode.valueOf(cfg.getString("pathMode"));
        if (cfg.hasKey("rdpEpsilon")) b.rdpEpsilon = cfg.getFloat("rdpEpsilon");
        if (cfg.hasKey("curveSteps")) b.curveSteps = cfg.getInt("curveSteps");
        if (cfg.hasKey("curveBias")) b.curveBias = cfg.getFloat("curveBias");

        if (cfg.hasKey("hopMode")) b.hopMode = GestureGranularConfig.HopMode.valueOf(cfg.getString("hopMode"));
        if (cfg.hasKey("hopLengthSamples")) b.hopLengthSamples = cfg.getInt("hopLengthSamples");

        if (cfg.hasKey("timingMode")) b.timingMode = GestureGranularConfig.TimeTransform.valueOf(cfg.getString("timingMode"));
        if (cfg.hasKey("basePointCount")) b.basePointCount = cfg.getInt("basePointCount");
        if (cfg.hasKey("baseDurationMs")) b.baseDurationMs = cfg.getInt("baseDurationMs");
        if (cfg.hasKey("resampleCount")) b.resampleCount = cfg.getInt("resampleCount");
        if (cfg.hasKey("targetDurationMs")) b.targetDurationMs = cfg.getInt("targetDurationMs");
        if (cfg.hasKey("warpShape")) b.warpShape = GestureGranularConfig.WarpShape.valueOf(cfg.getString("warpShape"));
        if (cfg.hasKey("warpExponent")) b.warpExponent = cfg.getFloat("warpExponent");

        if (cfg.hasKey("grainLengthSamples")) b.grainLengthSamples = cfg.getInt("grainLengthSamples");
        if (cfg.hasKey("env")) b.env = adsrFromJson(cfg.getJSONObject("env"));
        if (cfg.hasKey("gainDb")) b.gainDb = cfg.getFloat("gainDb");
        if (cfg.hasKey("pitchSemitones")) b.pitchSemitones = cfg.getFloat("pitchSemitones");

        if (cfg.hasKey("burstGrains")) b.burstGrains = cfg.getInt("burstGrains");
        if (cfg.hasKey("autoBurstGainComp")) b.autoBurstGainComp = cfg.getBoolean("autoBurstGainComp");
        if (cfg.hasKey("useArcLengthTime")) b.useArcLengthTime = cfg.getBoolean("useArcLengthTime");

        b.validate();

        String id = cfg.hasKey("id") ? cfg.getString("id") : null;
        String name = cfg.hasKey("name") ? cfg.getString("name") : null;
        String description = cfg.hasKey("description") ? cfg.getString("description") : null;
        String notes = cfg.hasKey("notes") ? cfg.getString("notes") : null;

        InstrumentType instrumentType = InstrumentType.GRANULAR;
        if (cfg.hasKey("instrumentType")) {
            instrumentType = InstrumentType.valueOf(cfg.getString("instrumentType"));
        }

        String linkedGesturePath = null;
        if (cfg.hasKey("linkedGesture")) {
            JSONObject linked = cfg.getJSONObject("linkedGesture");
            if (linked != null && linked.hasKey("path")) {
                linkedGesturePath = linked.getString("path");
            }
        }

        return new Result(b, id, name, description, notes, instrumentType, linkedGesturePath);
    }

    private static JSONObject adsrToJson(ADSRParams env) {
        JSONObject obj = new JSONObject();
        if (env == null) {
            obj.setFloat("amplitude", 1.0f);
            obj.setFloat("attack", 0.02f);
            obj.setFloat("decay", 0.06f);
            obj.setFloat("sustain", 0.9f);
            obj.setFloat("release", 0.10f);
            return obj;
        }

        obj.setFloat("amplitude", reflectFloat(env, "maxAmp"));
        obj.setFloat("attack", reflectFloat(env, "attack"));
        obj.setFloat("decay", reflectFloat(env, "decay"));
        obj.setFloat("sustain", reflectFloat(env, "sustain"));
        obj.setFloat("release", reflectFloat(env, "release"));
        return obj;
    }
    
    private static ADSRParams adsrFromJson(JSONObject obj) {
        float amplitude = getFloatOr(obj, "amplitude", 1.0f);
        float attack = getFloatOr(obj, "attack", 0.02f);
        float decay = getFloatOr(obj, "decay", 0.06f);
        float sustain = getFloatOr(obj, "sustain", 0.9f);
        float release = getFloatOr(obj, "release", 0.10f);
        return new ADSRParams(amplitude, attack, decay, sustain, release);
    }

    private static float reflectFloat(Object target, String... candidates) {
        for (String name : candidates) {
            try {
                Field f = target.getClass().getDeclaredField(name);
                f.setAccessible(true);
                Object v = f.get(target);
                if (v instanceof Number n) return n.floatValue();
            } catch (ReflectiveOperationException ignored) {
            }
        }
        throw new IllegalStateException("Could not read ADSRParams field via reflection");
    }

    private static float getFloatOr(JSONObject obj, String key, float fallback) {
        return (obj != null && obj.hasKey(key)) ? obj.getFloat(key) : fallback;
    }

    private static void requireHeader(JSONObject header, String expectedFormat, int expectedVersion) {
        if (header == null) throw new IllegalArgumentException("Missing header");
        String format = header.getString("format");
        int version = header.getInt("version");
        if (!expectedFormat.equals(format)) {
            throw new IllegalArgumentException("Unexpected format: " + format);
        }
        if (version != expectedVersion) {
            throw new IllegalArgumentException("Unsupported version: " + version);
        }
    }

    private static void putIfPresent(JSONObject obj, String key, String value) {
        if (value != null && !value.isBlank()) obj.setString(key, value);
    }

    private static String nonNull(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}