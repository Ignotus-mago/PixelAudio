package net.paulhertz.pixelaudio.io;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.paulhertz.pixelaudio.curves.PACurveMaker;
//import net.paulhertz.pixelaudio.curves.PACurveMaker;
//import net.paulhertz.pixelaudio.curves.PACurveMakerIO;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig;
import net.paulhertz.pixelaudio.io.GestureGranularConfigIO.InstrumentType;
import net.paulhertz.pixelaudio.io.PACurveMakerIO.Meta;
import net.paulhertz.pixelaudio.io.PACurveMakerIO.Result;
import processing.data.JSONArray;
import processing.data.JSONObject;

/**
 * Session manifest writer/reader for brush sets.
 *
 * Suggested filename:
 *   deadBodyWorkflow_rehearsalSet_session.json
 */
public final class AudioBrushSessionIO {

    private AudioBrushSessionIO() {}

    public static final String FORMAT = "net.paulhertz.pixelaudio.session";
    public static final int VERSION = 1;

    public interface BrushAdapter<B> {
        PACurveMaker curveOf(B brush);
        GestureGranularConfig.Builder configOf(B brush);
        InstrumentType instrumentTypeOf(B brush);
        String idOf(B brush);

        default String nameOf(B brush) { return null; }
        default String descriptionOf(B brush) { return null; }
        default String notesOf(B brush) { return null; }
        default boolean includeStyle(B brush) { return false; }
    }

    public interface BrushFactory<B> {
        B create(PACurveMaker curve,
                 GestureGranularConfig.Builder builder,
                 InstrumentType instrumentType,
                 BrushRecord record);
    }

    public static final class SessionMeta {
        public String id;
        public String name;
        public String description;
        public String notes;
        public String audioFilePath;
        public String audioFileName;
        public String audioFileTag;
    }

    public static final class BrushRecord {
        public String id;
        public String name;
        public String description;
        public String notes;
        public InstrumentType instrumentType;
        public String gesturePath;
        public String configPath;
    }

    public static final class SessionData<B> {
        public final SessionMeta meta;
        public final List<B> brushes;
        public final List<BrushRecord> records;

        public SessionData(SessionMeta meta, List<B> brushes, List<BrushRecord> records) {
            this.meta = meta;
            this.brushes = brushes;
            this.records = records;
        }
    }

    public static <B> void writeSession(
            File sessionFile,
            List<B> brushes,
            BrushAdapter<B> adapter,
            SessionMeta meta,
            String gestureDirName,
            String configDirName) throws IOException {

        if (sessionFile == null) throw new IllegalArgumentException("sessionFile cannot be null");
        if (brushes == null) throw new IllegalArgumentException("brushes cannot be null");
        if (adapter == null) throw new IllegalArgumentException("adapter cannot be null");
        if (meta == null) meta = new SessionMeta();

        File sessionDir = sessionFile.getAbsoluteFile().getParentFile();
        File gestureDir = new File(sessionDir, nonBlank(gestureDirName, "gestures"));
        File configDir = new File(sessionDir, nonBlank(configDirName, "configs"));
        if (!gestureDir.exists()) gestureDir.mkdirs();
        if (!configDir.exists()) configDir.mkdirs();

        String sessionStem = sessionStem(sessionFile);

        JSONArray arr = new JSONArray();

        int index = 0;
        for (B brush : brushes) {
            String id = safeId(adapter.idOf(brush));
            InstrumentType instrumentType = adapter.instrumentTypeOf(brush);

            String gestureFileName = sessionStem + "_gesture_" + index + ".json";
            String configFileName  = sessionStem + "_config_" + index + ".json";

            File gestureFile = new File(gestureDir, gestureFileName);
            File configFile = new File(configDir, configFileName);

            PACurveMakerIO.Meta gMeta = new PACurveMakerIO.Meta();
            gMeta.id = id;
            gMeta.name = adapter.nameOf(brush);
            gMeta.description = adapter.descriptionOf(brush);
            gMeta.notes = adapter.notesOf(brush);
            gMeta.includeStyle = adapter.includeStyle(brush);
            gMeta.linkedConfigPath = relativize(sessionDir, configFile);

            GestureGranularConfigIO.Meta cMeta = new GestureGranularConfigIO.Meta();
            cMeta.id = id;
            cMeta.name = adapter.nameOf(brush);
            cMeta.description = adapter.descriptionOf(brush);
            cMeta.notes = adapter.notesOf(brush);
            cMeta.instrumentType = instrumentType;
            cMeta.linkedGesturePath = relativize(sessionDir, gestureFile);

            PACurveMakerIO.write(gestureFile, adapter.curveOf(brush), gMeta);
            GestureGranularConfigIO.write(configFile, adapter.configOf(brush), cMeta);

            JSONObject item = new JSONObject();
            item.setString("id", id);
            item.setString("instrumentType", instrumentType.name());
            item.setString("gesturePath", relativize(sessionDir, gestureFile));
            item.setString("configPath", relativize(sessionDir, configFile));
            putIfPresent(item, "name", adapter.nameOf(brush));
            putIfPresent(item, "description", adapter.descriptionOf(brush));
            putIfPresent(item, "notes", adapter.notesOf(brush));
            arr.append(item);

            index++;
        }

        JSONObject root = new JSONObject();

        JSONObject header = new JSONObject();
        header.setString("format", FORMAT);
        header.setInt("version", VERSION);
        header.setString("type", "AudioBrushSession");
        root.setJSONObject("header", header);

        JSONObject session = new JSONObject();
        putIfPresent(session, "id", meta.id);
        putIfPresent(session, "name", meta.name);
        putIfPresent(session, "description", meta.description);
        putIfPresent(session, "notes", meta.notes);
        putIfPresent(session, "audioFilePath", meta.audioFilePath);
        putIfPresent(session, "audioFileName", meta.audioFileName);
        putIfPresent(session, "audioFileTag", meta.audioFileTag);
        root.setJSONObject("session", session);

        root.setJSONArray("brushes", arr);

        Files.writeString(sessionFile.toPath(), root.toString(), StandardCharsets.UTF_8);
    }
    
    public static <B> SessionData<B> readSession(File sessionFile, BrushFactory<B> factory) throws IOException {
        if (sessionFile == null) throw new IllegalArgumentException("sessionFile cannot be null");
        if (factory == null) throw new IllegalArgumentException("factory cannot be null");

        String json = Files.readString(sessionFile.toPath(), StandardCharsets.UTF_8);
        JSONObject root = JSONObject.parse(json);

        JSONObject header = root.getJSONObject("header");
        requireHeader(header, FORMAT, VERSION);

        SessionMeta meta = new SessionMeta();
        JSONObject session = root.getJSONObject("session");
        if (session != null) {
            if (session.hasKey("id")) meta.id = session.getString("id");
            if (session.hasKey("name")) meta.name = session.getString("name");
            if (session.hasKey("description")) meta.description = session.getString("description");
            if (session.hasKey("notes")) meta.notes = session.getString("notes");
            if (session.hasKey("audioFilePath")) meta.audioFilePath = session.getString("audioFilePath");
            if (session.hasKey("audioFileName")) meta.audioFileName = session.getString("audioFileName");
            if (session.hasKey("audioFileTag")) meta.audioFileTag = session.getString("audioFileTag");
        }

        File sessionDir = sessionFile.getAbsoluteFile().getParentFile();

        JSONArray arr = root.getJSONArray("brushes");
        List<B> brushes = new ArrayList<>();
        List<BrushRecord> records = new ArrayList<>();

        if (arr != null) {
            for (int i = 0; i < arr.size(); i++) {
                JSONObject item = arr.getJSONObject(i);

                BrushRecord rec = new BrushRecord();
                rec.id = item.getString("id");
                rec.instrumentType = InstrumentType.valueOf(item.getString("instrumentType"));
                rec.gesturePath = item.getString("gesturePath");
                rec.configPath = item.getString("configPath");
                if (item.hasKey("name")) rec.name = item.getString("name");
                if (item.hasKey("description")) rec.description = item.getString("description");
                if (item.hasKey("notes")) rec.notes = item.getString("notes");
                records.add(rec);

                File gestureFile = new File(sessionDir, rec.gesturePath);
                File configFile = new File(sessionDir, rec.configPath);

                PACurveMakerIO.Result g = PACurveMakerIO.read(gestureFile);
                GestureGranularConfigIO.Result c = GestureGranularConfigIO.read(configFile);

                B brush = factory.create(g.curve, c.builder, rec.instrumentType, rec);
                brushes.add(brush);
            }
        }

        return new SessionData<>(meta, brushes, records);
    }

    private static String sessionStem(File sessionFile) {
        String name = sessionFile.getName();
        if (name.endsWith(".json")) {
            name = name.substring(0, name.length() - 5);
        }
        if (name.endsWith("_session")) {
            name = name.substring(0, name.length() - 8);
        }
        return name;
    }
    
    private static String relativize(File baseDir, File target) {
        return baseDir.toPath().toAbsolutePath().normalize()
                .relativize(target.toPath().toAbsolutePath().normalize())
                .toString()
                .replace('\\', '/');
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

    private static String safeId(String id) {
        if (id == null || id.isBlank()) return "gesture";
        return id.replace('.', '_').replace(' ', '_');
    }

    private static String nonBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}