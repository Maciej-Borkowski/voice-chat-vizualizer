package com.techwave.svcvisualizer.config;

import net.minecraft.network.chat.Component;

/** Visual style of the "is speaking" indicator that reacts to voice loudness. */
public enum IndicatorStyle {
	/** Animated sine wave next to the name (LabyMod-like "someone is talking" waveform). */
	WAVE("svcvisualizer.enum.indicator.wave"),
	BARS("svcvisualizer.enum.indicator.bars"),
	DOT("svcvisualizer.enum.indicator.dot");

	private final String key;

	IndicatorStyle(String key) {
		this.key = key;
	}

	public Component getDisplayName() {
		return Component.translatable(key);
	}
}
