/*
 *  Copyright (c) 2024 - 2025 by Paul Hertz <ignotus@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

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
import processing.data.JSONArray;
import processing.data.JSONObject;

/**
 * Session manifest writer/reader for brush sets.
 *
 * Suggested filename:
 *   bagatelle_rehearsalSet_session.json
 *   
 * Used in the example sketch {@link net.paulhertz.pixelaudio.example.Bagatelle Bagatelle}.
 * 
 */
public final class AudioBrushSessionIO {

    private AudioBrushSessionIO() {}

    /** Format identifier written to session manifest headers. */
    public static final String FORMAT = "net.paulhertz.pixelaudio.session";
    /** Session manifest format version. */
    public static final int VERSION = 1;

    /** Adapter that exposes brush data needed for writing a session. */
    public interface BrushAdapter<B> {
        /**
         * @param brush brush instance to inspect
         * @return gesture curve for the brush
         */
        PACurveMaker curveOf(B brush);
        /**
         * @param brush brush instance to inspect
         * @return granular configuration for the brush
         */
        GestureGranularConfig.Builder configOf(B brush);
        /**
         * @param brush brush instance to inspect
         * @return instrument type for the brush
         */
        InstrumentType instrumentTypeOf(B brush);
        /**
         * @param brush brush instance to inspect
         * @return stable brush identifier
         */
        String idOf(B brush);

        /**
         * @param brush brush instance to inspect
         * @return optional display name
         */
        default String nameOf(B brush) { return null; }
        /**
         * @param brush brush instance to inspect
         * @return optional description
         */
        default String descriptionOf(B brush) { return null; }
        /**
         * @param brush brush instance to inspect
         * @return optional notes
         */
        default String notesOf(B brush) { return null; }
        /**
         * @param brush brush instance to inspect
         * @return true to include gesture style fields
         */
        default boolean includeStyle(B brush) { return false; }
    }

    /** Factory that rebuilds brush objects while reading a session. */
    public interface BrushFactory<B> {
        /**
         * Creates a brush from linked gesture/config files and its manifest record.
         *
         * @param curve loaded gesture curve
         * @param builder loaded granular configuration builder
         * @param instrumentType instrument type declared in the manifest
         * @param record manifest record for the brush
         * @return created brush object
         */
        B create(PACurveMaker curve,
                 GestureGranularConfig.Builder builder,
                 InstrumentType instrumentType,
                 BrushRecord record);
    }

    /** Session-level metadata stored in an AudioBrush manifest. */
    public static final class SessionMeta {
        /** Session identifier. */
        public String id;
        /** Session display name. */
        public String name;
        /** Session description. */
        public String description;
        /** Free-form session notes. */
        public String notes;
        /** Path to the source audio file. */
        public String audioFilePath;
        /** Source audio filename. */
        public String audioFileName;
        /** Application-specific source audio tag. */
        public String audioFileTag;
    }

    /** Per-brush manifest record linking gesture and configuration files. */
    public static final class BrushRecord {
        /** Brush identifier. */
        public String id;
        /** Brush display name. */
        public String name;
        /** Brush description. */
        public String description;
        /** Free-form brush notes. */
        public String notes;
        /** Instrument type for the brush. */
        public InstrumentType instrumentType;
        /** Relative path to the gesture JSON file. */
        public String gesturePath;
        /** Relative path to the configuration JSON file. */
        public String configPath;
    }

    /** Loaded session data with rebuilt brush objects and source records. */
    public static final class SessionData<B> {
        /** Session metadata. */
        public final SessionMeta meta;
        /** Brush objects created by the supplied factory. */
        public final List<B> brushes;
        /** Manifest records corresponding to the loaded brushes. */
        public final List<BrushRecord> records;

        /**
         * Creates a session data container.
         *
         * @param meta session metadata
         * @param brushes loaded brush objects
         * @param records source manifest records
         */
        public SessionData(SessionMeta meta, List<B> brushes, List<BrushRecord> records) {
            this.meta = meta;
            this.brushes = brushes;
            this.records = records;
        }
    }

    /**
     * Writes a session manifest and its linked gesture/configuration files.
     *
     * @param <B> brush object type
     * @param sessionFile destination session manifest file
     * @param brushes brushes to serialize
     * @param adapter adapter that exposes data from each brush
     * @param meta optional session metadata
     * @param gestureDirName folder name for linked gesture files
     * @param configDirName folder name for linked configuration files
     * @throws IOException if any file cannot be written
     */
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
    
    /**
     * Reads a session manifest and creates brush objects from linked resources.
     *
     * @param <B> brush object type
     * @param sessionFile session manifest file
     * @param factory factory used to create brush objects
     * @return loaded session data
     * @throws IOException if the manifest or linked files cannot be read
     */
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
