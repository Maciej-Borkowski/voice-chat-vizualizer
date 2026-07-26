package com.techwave.svcvisualizer.speaker;

import com.techwave.svcvisualizer.config.SortMode;
import com.techwave.svcvisualizer.config.VisualizerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry of who is currently speaking. {@link #mark} is called from the Simple Voice Chat
 * audio thread; {@link #snapshot} is called from the render thread.
 */
public final class SpeakerTracker {

	public static final SpeakerTracker INSTANCE = new SpeakerTracker();

	private final ConcurrentHashMap<UUID, SpeakerState> speakers = new ConcurrentHashMap<>();

	private volatile boolean testMode = false;
	private final List<SpeakerState> testStates = new ArrayList<>();

	private SpeakerTracker() {
	}

	/** Record an incoming audio packet for the given speaker. */
	public void mark(UUID id, boolean whispering, double distance, float level) {
		if (id == null) {
			return;
		}
		long now = System.currentTimeMillis();
		speakers.computeIfAbsent(id, SpeakerState::new).update(whispering, distance, level, now);
	}

	public void clear() {
		speakers.clear();
	}

	public boolean isTestMode() {
		return testMode;
	}

	public void setTestMode(boolean on) {
		this.testMode = on;
		if (on) {
			buildTestSpeakers();
		} else {
			testStates.clear();
		}
	}

	/** Compute the ordered, filtered list of speakers to render right now. */
	public Snapshot snapshot(long now, VisualizerConfig config) {
		List<SpeakerState> candidates = new ArrayList<>();

		if (testMode) {
			animateTestSpeakers(now);
			candidates.addAll(testStates);
		} else {
			UUID self = selfId();
			ClientPacketListener connection = Minecraft.getInstance().getConnection();
			speakers.forEach((id, state) -> {
				float a = alpha(state, now, config);
				if (a <= 0f) {
					// Expired – drop it to keep the map small.
					if (now - state.lastPacketMs > (long) config.speakingTimeoutMs + config.holdMs + config.fadeOutMs + 250L) {
						speakers.remove(id);
					}
					return;
				}
				// Ghost guard: only surface sources that map to a real tab-list player. Simple Voice Chat
				// also delivers plugin locational/static channels (radios, music, TTS, other mods routing
				// audio through SVC, or non-player entities) whose id is an arbitrary random UUID; without
				// this check they showed up as hex-like names with default skins.
				if (connection == null || connection.getPlayerInfo(id) == null) {
					return;
				}
				if (!config.showSelf && id.equals(self)) {
					return;
				}
				candidates.add(state);
			});
		}

		sort(candidates, config.sortMode);

		int overflow = Math.max(0, candidates.size() - config.maxEntries);
		List<SpeakerState> visible = candidates.size() > config.maxEntries
				? new ArrayList<>(candidates.subList(0, config.maxEntries))
				: candidates;
		return new Snapshot(visible, overflow);
	}

	private void sort(List<SpeakerState> list, SortMode mode) {
		switch (mode) {
			case DISTANCE -> list.sort(Comparator.comparingDouble(s -> s.distance));
			case ALPHABETICAL -> list.sort(Comparator.comparing(s -> resolveName(s).toLowerCase()));
			case RECENT -> list.sort(Comparator.comparingLong((SpeakerState s) -> s.firstPacketMs));
			default -> {
			}
		}
	}

	/** Combined fade-in / hold / fade-out alpha for a speaker, 0..1. */
	public static float alpha(SpeakerState s, long now, VisualizerConfig c) {
		long elapsed = now - s.lastPacketMs;
		long solid = (long) c.speakingTimeoutMs + c.holdMs;
		float fadeOut;
		if (elapsed <= solid) {
			fadeOut = 1f;
		} else if (elapsed >= solid + c.fadeOutMs) {
			fadeOut = 0f;
		} else {
			fadeOut = 1f - (float) (elapsed - solid) / (float) Math.max(1, c.fadeOutMs);
		}
		float fadeIn;
		long age = now - s.firstPacketMs;
		if (c.fadeInMs <= 0 || age >= c.fadeInMs) {
			fadeIn = 1f;
		} else {
			fadeIn = (float) age / (float) c.fadeInMs;
		}
		return Math.max(0f, Math.min(fadeIn, fadeOut));
	}

	/** Whether the speaker received audio recently enough to be considered actively talking. */
	public static boolean isActivelySpeaking(SpeakerState s, long now, VisualizerConfig c) {
		return now - s.lastPacketMs <= c.speakingTimeoutMs;
	}

	/** Resolve a display name from the tab list, falling back to an override or short UUID. */
	public static String resolveName(SpeakerState s) {
		if (s.nameOverride != null) {
			return s.nameOverride;
		}
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection != null) {
			PlayerInfo info = connection.getPlayerInfo(s.id);
			if (info != null && info.getProfile() != null && info.getProfile().getName() != null) {
				return info.getProfile().getName();
			}
		}
		String u = s.id.toString();
		return u.substring(0, Math.min(8, u.length()));
	}

	private static UUID selfId() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null ? mc.player.getUUID() : null;
	}

	private void buildTestSpeakers() {
		testStates.clear();
		long now = System.currentTimeMillis();
		List<UUID> real = new ArrayList<>();
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection != null) {
			for (PlayerInfo info : connection.getOnlinePlayers()) {
				real.add(info.getProfile().getId());
				if (real.size() >= 4) {
					break;
				}
			}
		}
		String[] fakeNames = {"Steve", "Alex", "Notch", "Whisperer"};
		for (int i = 0; i < 4; i++) {
			SpeakerState s;
			if (i < real.size()) {
				s = new SpeakerState(real.get(i));
			} else {
				s = new SpeakerState(UUID.nameUUIDFromBytes(("svcviz-test-" + i).getBytes()), fakeNames[i]);
			}
			s.firstPacketMs = now;
			s.lastPacketMs = now;
			s.whispering = (i == 3);
			s.distance = 4 + i * 3;
			s.level = 0.5f;
			testStates.add(s);
		}
	}

	private void animateTestSpeakers(long now) {
		for (int i = 0; i < testStates.size(); i++) {
			SpeakerState s = testStates.get(i);
			s.lastPacketMs = now;
			double phase = now / 260.0 + i * 1.3;
			s.level = (float) Math.max(0.05, 0.4 + 0.45 * Math.sin(phase));
		}
	}

	/** Immutable result of {@link #snapshot}. */
	public record Snapshot(List<SpeakerState> speakers, int overflow) {
	}
}
