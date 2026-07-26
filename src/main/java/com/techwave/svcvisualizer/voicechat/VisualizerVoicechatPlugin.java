package com.techwave.svcvisualizer.voicechat;

import com.techwave.svcvisualizer.speaker.AudioLevel;
import com.techwave.svcvisualizer.speaker.SpeakerTracker;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.minecraft.client.Minecraft;

/**
 * Simple Voice Chat plugin entrypoint. Listens (read-only) to every incoming audio packet so the
 * overlay knows who we can hear, plus our own outgoing microphone ({@link ClientSoundEvent}) so
 * "show me when I am speaking" works – scaled to the real input volume, like everyone else.
 */
public class VisualizerVoicechatPlugin implements VoicechatPlugin {

	@Override
	public String getPluginId() {
		return "svcvisualizer";
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		registration.registerEvent(ClientReceiveSoundEvent.EntitySound.class, this::onEntitySound);
		registration.registerEvent(ClientReceiveSoundEvent.LocationalSound.class, this::onLocationalSound);
		registration.registerEvent(ClientReceiveSoundEvent.StaticSound.class, this::onStaticSound);
		registration.registerEvent(ClientSoundEvent.class, this::onClientSound);
	}

	/** Proximity voice tied to a player entity – the common case. */
	private void onEntitySound(ClientReceiveSoundEvent.EntitySound event) {
		SpeakerTracker.INSTANCE.mark(
				event.getEntityId(),
				event.isWhispering(),
				event.getDistance(),
				AudioLevel.rms(event.getRawAudio()));
	}

	/** Locational audio not bound to an entity (some group / plugin channels). */
	private void onLocationalSound(ClientReceiveSoundEvent.LocationalSound event) {
		SpeakerTracker.INSTANCE.mark(
				event.getId(),
				false,
				event.getDistance(),
				AudioLevel.rms(event.getRawAudio()));
	}

	/** Non-positional audio (e.g. group chat). */
	private void onStaticSound(ClientReceiveSoundEvent.StaticSound event) {
		SpeakerTracker.INSTANCE.mark(
				event.getId(),
				false,
				0.0,
				AudioLevel.rms(event.getRawAudio()));
	}

	/** Our own microphone – shows us speaking, with the wave scaled to the real input volume. */
	private void onClientSound(ClientSoundEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			SpeakerTracker.INSTANCE.mark(
					mc.player.getUUID(),
					event.isWhispering(),
					0.0,
					AudioLevel.rms(event.getRawAudio()));
		}
	}
}
