package net.paulhertz.pixelaudio.io;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.paulhertz.pixelaudio.curves.PACurveMaker;
import processing.core.PVector;
import processing.data.JSONArray;
import processing.data.JSONObject;

/**
 * JSON read/write support for PACurveMaker.
 *
 * Filename convention suggestion:
 *   gesture_001_gesture.json
 */
public final class PACurveMakerIO {

    private PACurveMakerIO() {}

    public static final String FORMAT = "net.paulhertz.pixelaudio.gesture";
    public static final int VERSION = 1;

    public static final class Meta {
        public String id;
        public String name;
        public String description;
        public String notes;
        public String linkedConfigPath;
        public boolean includeStyle = false;

        public Meta() {}
    }

    public static final class Result {
        public final PACurveMaker curve;
        public final String id;
        public final String name;
        public final String description;
        public final String notes;
        public final String linkedConfigPath;

        public Result(PACurveMaker curve,
                      String id,
                      String name,
                      String description,
                      String notes,
                      String linkedConfigPath) {
            this.curve = curve;
            this.id = id;
            this.name = name;
            this.description = description;
            this.notes = notes;
            this.linkedConfigPath = linkedConfigPath;
        }
    }

    public static JSONObject toJson(PACurveMaker curve, Meta meta) {
        if (curve == null) throw new IllegalArgumentException("curve cannot be null");
        if (meta == null) meta = new Meta();

        JSONObject root = new JSONObject();

        JSONObject header = new JSONObject();
        header.setString("format", FORMAT);
        header.setInt("version", VERSION);
        header.setString("type", "PACurveMaker");
        root.setJSONObject("header", header);

        JSONObject gesture = new JSONObject();
        gesture.setString("id", nonNull(meta.id, "gesture"));
        putIfPresent(gesture, "name", meta.name);
        putIfPresent(gesture, "description", meta.description);
        putIfPresent(gesture, "notes", meta.notes);

        gesture.setJSONArray("dragPoints", pointsToJson(curve.getAllPoints()));
        gesture.setJSONArray("dragTimes", intsToJson(curve.getDragTimes()));
        gesture.setInt("startTime", curve.getTimeStamp());
        gesture.setInt("timeOffset", curve.getTimeOffset());

        gesture.setFloat("epsilon", curve.getEpsilon());
        gesture.setInt("curveSteps", curve.getCurveSteps());
        gesture.setInt("polySteps", curve.getPolySteps());
        gesture.setFloat("bezierBias", curve.getBezierBias());
        gesture.setFloat("brushSize", curve.getBrushSize());
        gesture.setBoolean("drawWeighted", curve.isDrawWeighted());
        gesture.setBoolean("brushIsCustom", curve.brushIsCustom);

        if (meta.includeStyle) {
            JSONObject style = new JSONObject();
            style.setInt("dragColor", curve.getDragColor());
            style.setFloat("dragWeight", curve.getDragWeight());
            style.setInt("rdpColor", curve.getRdpColor());
            style.setFloat("rdpWeight", curve.getRdpWeight());
            style.setInt("curveColor", curve.getCurveColor());
            style.setFloat("curveWeight", curve.getCurveWeight());
            style.setInt("brushColor", curve.getBrushColor());
            style.setFloat("brushWeight", curve.getBrushWeight());
            style.setInt("activeBrushColor", curve.getActiveBrushColor());
            style.setInt("eventPointsColor", curve.getEventPointsColor());
            style.setFloat("eventPointsSize", curve.getEventPointsSize());
            gesture.setJSONObject("style", style);
        }

        if (meta.linkedConfigPath != null && !meta.linkedConfigPath.isBlank()) {
            JSONObject linked = new JSONObject();
            linked.setString("path", meta.linkedConfigPath);
            gesture.setJSONObject("linkedConfig", linked);
        }

        root.setJSONObject("gesture", gesture);
        return root;
    }

    public static void write(File file, PACurveMaker curve, Meta meta) throws IOException {
        if (file == null) throw new IllegalArgumentException("file cannot be null");
        String json = toJson(curve, meta).toString();
        Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
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

        JSONObject gesture = root.getJSONObject("gesture");
        if (gesture == null) throw new IllegalArgumentException("Missing gesture object");

        ArrayList<PVector> points = jsonToPoints(gesture.getJSONArray("dragPoints"));
        ArrayList<Integer> times = jsonToIntsList(gesture.getJSONArray("dragTimes"));
        if (points.size() != times.size()) {
            throw new IllegalArgumentException("dragPoints / dragTimes size mismatch");
        }

        int startTime = gesture.getInt("startTime");
        PACurveMaker curve = PACurveMaker.buildCurveMaker(points, times, startTime);

        if (gesture.hasKey("timeOffset")) curve.setTimeOffset(gesture.getInt("timeOffset"));
        if (gesture.hasKey("epsilon")) curve.setEpsilon(gesture.getFloat("epsilon"));
        if (gesture.hasKey("curveSteps")) curve.setEventSteps(gesture.getInt("curveSteps"));
        else if (gesture.hasKey("eventSteps")) curve.setEventSteps(gesture.getInt("eventSteps"));
        if (gesture.hasKey("polySteps")) curve.setPolySteps(gesture.getInt("polySteps"));
        if (gesture.hasKey("bezierBias")) curve.setBezierBias(gesture.getFloat("bezierBias"));
        if (gesture.hasKey("brushSize")) curve.setBrushSize(gesture.getFloat("brushSize"));
        if (gesture.hasKey("drawWeighted")) curve.setDrawWeighted(gesture.getBoolean("drawWeighted"));
        if (gesture.hasKey("brushIsCustom")) curve.brushIsCustom = gesture.getBoolean("brushIsCustom");

        if (gesture.hasKey("style")) {
            JSONObject style = gesture.getJSONObject("style");
            if (style.hasKey("dragColor")) curve.setDragColor(style.getInt("dragColor"));
            if (style.hasKey("dragWeight")) curve.setDragWeight(style.getFloat("dragWeight"));
            if (style.hasKey("rdpColor")) curve.setRdpColor(style.getInt("rdpColor"));
            if (style.hasKey("rdpWeight")) curve.setRdpWeight(style.getFloat("rdpWeight"));
            if (style.hasKey("curveColor")) curve.setCurveColor(style.getInt("curveColor"));
            if (style.hasKey("curveWeight")) curve.setCurveWeight(style.getFloat("curveWeight"));
            if (style.hasKey("brushColor")) curve.setBrushColor(style.getInt("brushColor"));
            if (style.hasKey("brushWeight")) curve.setBrushWeight(style.getFloat("brushWeight"));
            if (style.hasKey("activeBrushColor")) curve.setActiveBrushColor(style.getInt("activeBrushColor"));
            if (style.hasKey("eventPointsColor")) curve.setEventPointsColor(style.getInt("eventPointsColor"));
            if (style.hasKey("eventPointsSize")) curve.setEventPointsSize(style.getFloat("eventPointsSize"));
        }

        curve.calculateDerivedPoints();

        String id = gesture.hasKey("id") ? gesture.getString("id") : null;
        String name = gesture.hasKey("name") ? gesture.getString("name") : null;
        String description = gesture.hasKey("description") ? gesture.getString("description") : null;
        String notes = gesture.hasKey("notes") ? gesture.getString("notes") : null;

        String linkedConfigPath = null;
        if (gesture.hasKey("linkedConfig")) {
            JSONObject linked = gesture.getJSONObject("linkedConfig");
            if (linked != null && linked.hasKey("path")) {
                linkedConfigPath = linked.getString("path");
            }
        }

        return new Result(curve, id, name, description, notes, linkedConfigPath);
    }

    private static JSONArray pointsToJson(List<PVector> points) {
        JSONArray arr = new JSONArray();
        if (points == null) return arr;
        for (PVector p : points) {
            JSONObject obj = new JSONObject();
            obj.setFloat("x", p.x);
            obj.setFloat("y", p.y);
            arr.append(obj);
        }
        return arr;
    }

    private static ArrayList<PVector> jsonToPoints(JSONArray arr) {
        ArrayList<PVector> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.size(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            out.add(new PVector(obj.getFloat("x"), obj.getFloat("y")));
        }
        return out;
    }

    private static JSONArray intsToJson(int[] values) {
        JSONArray arr = new JSONArray();
        if (values == null) return arr;
        for (int v : values) arr.append(v);
        return arr;
    }

    private static ArrayList<Integer> jsonToIntsList(JSONArray arr) {
        ArrayList<Integer> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.size(); i++) out.add(arr.getInt(i));
        return out;
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