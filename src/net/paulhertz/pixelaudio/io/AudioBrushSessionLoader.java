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
import net.paulhertz.pixelaudio.granular.GestureGranularConfig;
import processing.data.JSONArray;
import processing.data.JSONObject;

/**
 * Read AudioBrush session data from JSON files.
 * 
 * Used in the example sketch {@link net.paulhertz.pixelaudio.example.Bagatelle Bagatelle}.
 * 
 */
public final class AudioBrushSessionLoader {

	private AudioBrushSessionLoader() {}

	/** Supported AudioBrush JSON file categories. */
	public enum JsonFileType {
		/** PACurveMaker gesture JSON. */
		GESTURE,
		/** GestureGranularConfig JSON. */
		CONFIG,
		/** AudioBrush session manifest JSON. */
		SESSION,
		/** Unknown or unsupported JSON format. */
		UNKNOWN
	}

	/** Data loaded for a single brush. */
	public static class BrushData {
		/** Gesture curve data, when available. */
		public PACurveMaker curve;
		/** Granular configuration builder, when available. */
		public GestureGranularConfig.Builder config;
		/** Instrument type declared by linked configuration data. */
		public GestureGranularConfigIO.InstrumentType instrumentType;
	}

	/** Result of loading a gesture, configuration, or session file. */
	public static class LoadResult {
		/** Detected file type. */
		public JsonFileType type = JsonFileType.UNKNOWN;

		/** Brush loaded from a single gesture or configuration file. */
		public BrushData brush;

		/** Session metadata for session manifest loads. */
		public AudioBrushSessionIO.SessionMeta sessionMeta;
		/** Brushes loaded from a session manifest. */
		public List<BrushData> brushes;
	}

	/**
	 * Loads a gesture, configuration, or session JSON file.
	 *
	 * @param chosenFile JSON file to load
	 * @return load result for the detected file type
	 * @throws IOException if the file cannot be read or its type is unsupported
	 */
	public static LoadResult load(File chosenFile) throws IOException {
		if (chosenFile == null) {
			throw new IllegalArgumentException("chosenFile cannot be null");
		}

		JSONObject root = readRoot(chosenFile);
		JsonFileType type = detectType(root);

		return switch (type) {
		case GESTURE -> loadSingleFromGesture(chosenFile, root);
		case CONFIG  -> loadSingleFromConfig(chosenFile, root);
		case SESSION -> loadSession(chosenFile, root);
		case UNKNOWN -> throw new IOException("Unknown or unsupported JSON file type: " + chosenFile.getName());
		};
	}

	/**
	 * Reads a JSON file into a Processing {@link JSONObject}.
	 *
	 * @param file JSON file to read
	 * @return parsed root object
	 * @throws IOException if the file cannot be read
	 */
	public static JSONObject readRoot(File file) throws IOException {
		if (file == null) {
			throw new IllegalArgumentException("file cannot be null");
		}
		String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
		return JSONObject.parse(json);
	}

	/**
	 * Detects the AudioBrush JSON file type from its header format field.
	 *
	 * @param root parsed JSON root object
	 * @return detected file type, or {@link JsonFileType#UNKNOWN}
	 */
	public static JsonFileType detectType(JSONObject root) {
		if (root == null || !root.hasKey("header")) return JsonFileType.UNKNOWN;

		JSONObject header = root.getJSONObject("header");
		if (header == null || !header.hasKey("format")) return JsonFileType.UNKNOWN;

		String format = header.getString("format");
		if (format == null) return JsonFileType.UNKNOWN;

		return switch (format) {
		case PACurveMakerIO.FORMAT -> JsonFileType.GESTURE;
		case GestureGranularConfigIO.FORMAT -> JsonFileType.CONFIG;
		case AudioBrushSessionIO.FORMAT -> JsonFileType.SESSION;
		default -> JsonFileType.UNKNOWN;
		};
	}

	private static LoadResult loadSingleFromGesture(File gestureFile, JSONObject root) throws IOException {
		PACurveMakerIO.Result gestureResult = PACurveMakerIO.fromJson(root);

		BrushData bd = new BrushData();
		bd.curve = gestureResult.curve;
		bd.config = null;
		bd.instrumentType = null;

		String linkedConfig = gestureResult.linkedConfigPath;
		if (linkedConfig != null && !linkedConfig.isBlank()) {
			File configFile = new File(gestureFile.getParentFile(), linkedConfig);
			if (configFile.exists()) {
				GestureGranularConfigIO.Result configResult = GestureGranularConfigIO.read(configFile);
				bd.config = configResult.builder;
				bd.instrumentType = configResult.instrumentType;
			}
		}

		LoadResult out = new LoadResult();
		out.type = JsonFileType.GESTURE;
		out.brush = bd;
		return out;
	}

	private static LoadResult loadSingleFromConfig(File configFile, JSONObject root) throws IOException {
		GestureGranularConfigIO.Result configResult = GestureGranularConfigIO.fromJson(root);

		BrushData bd = new BrushData();
		bd.curve = null;
		bd.config = configResult.builder;
		bd.instrumentType = configResult.instrumentType;

		String linkedGesture = configResult.linkedGesturePath;
		if (linkedGesture != null && !linkedGesture.isBlank()) {
			File gestureFile = new File(configFile.getParentFile(), linkedGesture);
			if (gestureFile.exists()) {
				PACurveMakerIO.Result gestureResult = PACurveMakerIO.read(gestureFile);
				bd.curve = gestureResult.curve;
			}
		}

		LoadResult out = new LoadResult();
		out.type = JsonFileType.CONFIG;
		out.brush = bd;
		return out;
	}

	private static LoadResult loadSession(File sessionFile, JSONObject root) throws IOException {
		AudioBrushSessionIO.SessionMeta meta = new AudioBrushSessionIO.SessionMeta();

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

		List<BrushData> brushes = new ArrayList<>();
		File sessionDir = sessionFile.getParentFile();

		JSONArray arr = root.getJSONArray("brushes");
		if (arr != null) {
			for (int i = 0; i < arr.size(); i++) {
				JSONObject item = arr.getJSONObject(i);
				if (item == null) continue;

				String gesturePath = item.hasKey("gesturePath") ? item.getString("gesturePath") : null;
				String configPath = item.hasKey("configPath") ? item.getString("configPath") : null;
				GestureGranularConfigIO.InstrumentType type =
						item.hasKey("instrumentType")
						? GestureGranularConfigIO.InstrumentType.valueOf(item.getString("instrumentType"))
								: null;

				PACurveMaker curve = null;
				GestureGranularConfig.Builder cfg = null;

				if (gesturePath != null && !gesturePath.isBlank()) {
					File gestureFile = new File(sessionDir, gesturePath);
					if (gestureFile.exists()) {
						PACurveMakerIO.Result g = PACurveMakerIO.read(gestureFile);
						curve = g.curve;
					}
				}

				if (configPath != null && !configPath.isBlank()) {
					File configFile = new File(sessionDir, configPath);
					if (configFile.exists()) {
						GestureGranularConfigIO.Result c = GestureGranularConfigIO.read(configFile);
						cfg = c.builder;
						if (type == null) type = c.instrumentType;
					}
				}

				BrushData bd = new BrushData();
				bd.curve = curve;
				bd.config = cfg;
				bd.instrumentType = type;
				brushes.add(bd);
			}
		}

		LoadResult out = new LoadResult();
		out.type = JsonFileType.SESSION;
		out.sessionMeta = meta;
		out.brushes = brushes;
		return out;
	}
}
