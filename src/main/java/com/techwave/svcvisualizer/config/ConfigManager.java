package com.techwave.svcvisualizer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.techwave.svcvisualizer.SvcVisualizer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Loads and saves {@link VisualizerConfig} to {@code config/svc-visualizer.json}. */
public final class ConfigManager {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH =
			FabricLoader.getInstance().getConfigDir().resolve("svc-visualizer.json");

	private static VisualizerConfig config = new VisualizerConfig();

	private ConfigManager() {
	}

	public static VisualizerConfig get() {
		return config;
	}

	/** Load the config from disk, creating defaults if missing and backing up a corrupt file. */
	public static void load() {
		if (!Files.exists(PATH)) {
			config = new VisualizerConfig();
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
			VisualizerConfig loaded = GSON.fromJson(reader, VisualizerConfig.class);
			if (loaded == null) {
				throw new IOException("Config file was empty or not valid JSON");
			}
			loaded.sanitize();
			config = loaded;
		} catch (Exception e) {
			SvcVisualizer.LOGGER.error("Failed to read config, backing it up and using defaults", e);
			backupCorruptFile();
			config = new VisualizerConfig();
			save();
		}
	}

	/** Persist the current config to disk. */
	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException e) {
			SvcVisualizer.LOGGER.error("Failed to save config", e);
		}
	}

	private static void backupCorruptFile() {
		try {
			Path backup = PATH.resolveSibling("svc-visualizer.json.bak");
			Files.copy(PATH, backup, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			SvcVisualizer.LOGGER.warn("Could not back up corrupt config", ex);
		}
	}
}
