package com.techwave.svcvisualizer.voicechat;

import com.techwave.svcvisualizer.speaker.AudioLevel;
import com.techwave.svcvisualizer.speaker.SpeakerTracker;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

/**
 * Simple Voice Chat plugin entrypoint. Listens (read-only) to every incoming audio packet and feeds
 * the speaker tracker so the overlay knows who we can currently hear.
 */
public class VisualizerVoicechatPlugin implements VoicechatPlugin {

	@Override
	public String getPluginId() {
		return "svcvisualizer";
	}

	@Override
	public void initialize(de.maxhenkel.voicechat.api.VoicechatApi api) {
		// On the client the API is a VoicechatClientApi - keep it so we can poll the local microphone.
		if (api instanceof de.maxhenkel.voicechat.api.VoicechatClientApi clientApi) {
			SelfSpeaking.setClientApi(clientApi);
		}
	}
	@Override
	public void registerEvents(EventRegistration registration) {
		registration.registerEvent(ClientReceiveSoundEvent.EntitySound.class, this::onEntitySound);
		registration.registerEvent(ClientReceiveSoundEvent.LocationalSound.class, this::onLocationalSound);
		registration.registerEvent(ClientReceiveSoundEvent.StaticSound.class, this::onStaticSound);
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
}
