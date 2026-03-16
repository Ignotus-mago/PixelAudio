package net.paulhertz.pixelaudio.io;

import java.io.File;

public final class AudioBrushFileNamer {

    private AudioBrushFileNamer() {}

    public static final String SESSION_SUFFIX = "_session.json";
    public static final String GESTURE_SUFFIX = "_gesture.json";
    public static final String CONFIG_SUFFIX  = "_config.json";

    public static class Result {
        public final File sessionFile;
        public final File gestureFile;
        public final File configFile;
        public final String baseName;
        public final String linkedGesturePath;
        public final String linkedConfigPath;

        Result(File sessionFile, File gestureFile, File configFile, String baseName) {
            this.sessionFile = sessionFile;
            this.gestureFile = gestureFile;
            this.configFile = configFile;
            this.baseName = baseName;
            this.linkedGesturePath = gestureFile.getName();
            this.linkedConfigPath = configFile.getName();
        }
    }

    public static Result build(File chosenFile) {
        if (chosenFile == null) {
            throw new IllegalArgumentException("chosenFile cannot be null");
        }

        File parent = chosenFile.getAbsoluteFile().getParentFile();
        String baseName = normalizeBaseName(chosenFile.getName());

        File sessionFile = new File(parent, baseName + SESSION_SUFFIX);
        File gestureFile = new File(parent, baseName + GESTURE_SUFFIX);
        File configFile  = new File(parent, baseName + CONFIG_SUFFIX);

        return new Result(sessionFile, gestureFile, configFile, baseName);
    }

    public static String normalizeBaseName(String name) {
        if (name == null || name.isBlank()) {
            return "untitled";
        }

        String base = name;

        if (base.endsWith(".json")) {
            base = base.substring(0, base.length() - 5);
        }

        if (base.endsWith("_session")) {
            base = base.substring(0, base.length() - 8);
        }
        else if (base.endsWith("_gesture")) {
            base = base.substring(0, base.length() - 8);
        }
        else if (base.endsWith("_config")) {
            base = base.substring(0, base.length() - 7);
        }

        base = base.trim().replace(' ', '_').replace('.', '_');

        if (base.isEmpty()) {
            base = "untitled";
        }

        return base;
    }
}