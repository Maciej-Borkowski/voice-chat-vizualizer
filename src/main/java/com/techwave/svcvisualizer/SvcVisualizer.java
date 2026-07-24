package com.techwave.svcvisualizer;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared constants and logger for the mod. */
public final class SvcVisualizer {

	public static final String MOD_ID = "svcvisualizer";
	public static final String MOD_NAME = "Voice Chat Visualizer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	private SvcVisualizer() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
