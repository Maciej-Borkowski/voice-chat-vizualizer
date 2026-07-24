package com.techwave.svcvisualizer.config;

import net.minecraft.network.chat.Component;

/** Vertical direction the list of speakers grows in. */
public enum StackDirection {
	DOWN("svcvisualizer.enum.stack.down"),
	UP("svcvisualizer.enum.stack.up");

	private final String key;

	StackDirection(String key) {
		this.key = key;
	}

	public Component getDisplayName() {
		return Component.translatable(key);
	}
}
