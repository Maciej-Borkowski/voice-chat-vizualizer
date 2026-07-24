package com.techwave.svcvisualizer.speaker;

import java.util.UUID;

/**
 * Live state for one speaker. Written from the voice-chat audio thread and read from the render
 * thread, so mutable fields are {@code volatile}.
 */
public class SpeakerState {

	public final UUID id;
	/** Optional display name override (used by the preview/test mode); {@code null} for real players. */
	public final String nameOverride;

	public volatile long lastPacketMs;
	public volatile long firstPacketMs;
	public volatile boolean whispering;
	public volatile double distance;
	/** Smoothed loudness 0..1. */
	public volatile float level;

	public SpeakerState(UUID id) {
		this(id, null);
	}

	public SpeakerState(UUID id, String nameOverride) {
		this.id = id;
		this.nameOverride = nameOverride;
	}

	/** Update from an incoming audio packet. */
	public void update(boolean whispering, double distance, float rawLevel, long now) {
		if (now - lastPacketMs > 500L) {
			// New talk burst.
			firstPacketMs = now;
			level = rawLevel;
		} else {
			// Fast attack, slower release for a natural "breathing" indicator.
			float target = rawLevel;
			float current = level;
			float smoothing = target > current ? 0.5f : 0.2f;
			level = current + (target - current) * smoothing;
		}
		this.whispering = whispering;
		this.distance = distance;
		this.lastPacketMs = now;
	}
}
