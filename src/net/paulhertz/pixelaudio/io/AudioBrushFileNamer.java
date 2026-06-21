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

/**
 * Generate JSON filenames for AudioBrush "session" format, with gesture and audio synth settings. 
 * Used in the example sketch {@link net.paulhertz.pixelaudio.example.Bagatelle Bagatelle}.
 */
public final class AudioBrushFileNamer {

    private AudioBrushFileNamer() {}

    /** Suffix used for session manifest files. */
    public static final String SESSION_SUFFIX = "_session.json";
    /** Suffix used for gesture data files. */
    public static final String GESTURE_SUFFIX = "_gesture.json";
    /** Suffix used for configuration data files. */
    public static final String CONFIG_SUFFIX  = "_config.json";

    /** Generated file names and relative link paths for an AudioBrush save set. */
    public static class Result {
        /** Session manifest file. */
        public final File sessionFile;
        /** Gesture data file. */
        public final File gestureFile;
        /** Configuration data file. */
        public final File configFile;
        /** Normalized base name shared by generated files. */
        public final String baseName;
        /** Relative path from the session file to the gesture file. */
        public final String linkedGesturePath;
        /** Relative path from the session file to the configuration file. */
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

    /**
     * Builds the related session, gesture, and configuration filenames from one chosen file.
     *
     * @param chosenFile user-selected file whose name and folder provide the base
     * @return generated filename set
     */
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

    /**
     * Removes known JSON suffixes and normalizes spaces and dots for generated filenames.
     *
     * @param name source filename or base name
     * @return normalized base name, or {@code untitled} for blank input
     */
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
