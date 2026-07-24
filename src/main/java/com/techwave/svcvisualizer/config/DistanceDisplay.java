package com.techwave.svcvisualizer.config;

import net.minecraft.network.chat.Component;

/** Whether (and how) to show the distance to the speaker. */
public enum DistanceDisplay {
	OFF("svcvisualizer.enum.distance.off"),
	BLOCKS("svcvisualizer.enum.distance.blocks");

	private final String key;

	DistanceDisplay(String key) {
		this.key = key;
	}

	public Component getDisplayName() {
		return Component.translatable(key);
	}
}
