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
import java.util.ArrayList;
import java.util.List;

import net.paulhertz.pixelaudio.curves.PACurveMaker;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig;

/**
 * Read AudioBrush library JSON format.
 * Used in the example sketch {@link net.paulhertz.pixelaudio.example.Bagatelle Bagatelle}.
 * 
 */
public final class AudioBrushLibraryLoader {

    private AudioBrushLibraryLoader() {}

    /** Data loaded for one brush JSON file. */
    public static class BrushData {
        /** Source JSON file. */
        public File sourceFile;
        /** Gesture curve data, when available. */
        public PACurveMaker curve;
        /** Granular configuration builder, when available. */
        public GestureGranularConfig.Builder config;
        /** Instrument type declared by the brush configuration. */
        public GestureGranularConfigIO.InstrumentType instrumentType;
        /** Brush identifier derived from the file or metadata. */
        public String id;
        /** Display name for the brush. */
        public String name;
    }

    /** Result of loading an AudioBrush library folder. */
    public static class LoadResult {
        /** Successfully loaded brush records. */
        public final List<BrushData> brushes = new ArrayList<>();
        /** JSON files skipped because they were not loadable brush gestures. */
        public final List<File> skippedFiles = new ArrayList<>();
        /** Human-readable load messages and errors. */
        public final List<String> messages = new ArrayList<>();
    }

    /**
     * Loads all gesture JSON files in a folder, following linked configuration files when present.
     *
     * @param folder folder containing AudioBrush JSON files
     * @return load result with brushes, skipped files, and messages
     */
    public static LoadResult loadGestureLibrary(File folder) {
        if (folder == null) {
            throw new IllegalArgumentException("folder cannot be null");
        }
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Not a valid folder: " + folder);
        }

        LoadResult result = new LoadResult();
        File[] files = folder.listFiles();
        if (files == null) return result;
        
        // sort the files by name
        java.util.Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        
        for (File file : files) {
            if (file == null || !file.isFile()) continue;
            if (!file.getName().toLowerCase().endsWith(".json")) continue;

            try {
                AudioBrushSessionLoader.JsonFileType type =
                    AudioBrushSessionLoader.detectType(AudioBrushSessionLoader.readRoot(file));

                if (type == AudioBrushSessionLoader.JsonFileType.GESTURE) {
                    AudioBrushSessionLoader.LoadResult load = AudioBrushSessionLoader.load(file);
                    if (load != null && load.brush != null && load.brush.curve != null) {
                        BrushData bd = new BrushData();
                        bd.sourceFile = file;
                        bd.curve = load.brush.curve;
                        bd.config = load.brush.config;
                        bd.instrumentType = load.brush.instrumentType;
                        bd.name = file.getName();
                        bd.id = stripJson(file.getName());
                        result.brushes.add(bd);
                    } else {
                        result.skippedFiles.add(file);
                        result.messages.add("Skipped gesture file with no curve: " + file.getName());
                    }
                } else {
                    result.skippedFiles.add(file);
                }
            }
            catch (Exception e) {
                result.skippedFiles.add(file);
                result.messages.add("Error loading " + file.getName() + ": " + e.getMessage());
            }
        }

        return result;
    }

    private static String stripJson(String name) {
        if (name == null) return null;
        return name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }
}
