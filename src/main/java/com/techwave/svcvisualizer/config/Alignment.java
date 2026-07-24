package com.techwave.svcvisualizer.config;

import net.minecraft.network.chat.Component;

/**
 * Horizontal alignment of the overlay: rows are anchored to their left or right edge
 * and grow away from that edge.
 */
public enum Alignment {
	LEFT("svcvisualizer.enum.alignment.left"),
	RIGHT("svcvisualizer.enum.alignment.right");

	private final String key;

	Alignment(String key) {
		this.key = key;
	}

	public Component getDisplayName() {
		return Component.translatable(key);
	}
}
