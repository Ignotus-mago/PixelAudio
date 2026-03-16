package net.paulhertz.pixelaudio.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.paulhertz.pixelaudio.curves.PACurveMaker;
import net.paulhertz.pixelaudio.granular.GestureGranularConfig;

public final class AudioBrushLibraryLoader {

    private AudioBrushLibraryLoader() {}

    public static class BrushData {
        public File sourceFile;
        public PACurveMaker curve;
        public GestureGranularConfig.Builder config;
        public GestureGranularConfigIO.InstrumentType instrumentType;
        public String id;
        public String name;
    }

    public static class LoadResult {
        public final List<BrushData> brushes = new ArrayList<>();
        public final List<File> skippedFiles = new ArrayList<>();
        public final List<String> messages = new ArrayList<>();
    }

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