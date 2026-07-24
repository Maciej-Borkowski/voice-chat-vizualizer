package com.techwave.svcvisualizer.config;

import net.minecraft.network.chat.Component;

/** Order in which speakers are listed in the overlay. */
public enum SortMode {
	RECENT("svcvisualizer.enum.sort.recent"),
	DISTANCE("svcvisualizer.enum.sort.distance"),
	ALPHABETICAL("svcvisualizer.enum.sort.alphabetical");

	private final String key;

	SortMode(String key) {
		this.key = key;
	}

	public Component getDisplayName() {
		return Component.translatable(key);
	}
}
