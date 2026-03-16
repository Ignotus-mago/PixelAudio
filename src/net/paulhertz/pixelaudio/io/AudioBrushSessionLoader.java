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

public final class AudioBrushSessionLoader {

	private AudioBrushSessionLoader() {}

	public enum JsonFileType {
		GESTURE,
		CONFIG,
		SESSION,
		UNKNOWN
	}

	public static class BrushData {
		public PACurveMaker curve;
		public GestureGranularConfig.Builder config;
		public GestureGranularConfigIO.InstrumentType instrumentType;
	}

	public static class LoadResult {
		public JsonFileType type = JsonFileType.UNKNOWN;

		// for single gesture/config loads
		public BrushData brush;

				// for session loads
		public AudioBrushSessionIO.SessionMeta sessionMeta;
		public List<BrushData> brushes;
	}

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

	public static JSONObject readRoot(File file) throws IOException {
		if (file == null) {
			throw new IllegalArgumentException("file cannot be null");
		}
		String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
		return JSONObject.parse(json);
	}

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