package com.techwave.svcvisualizer.voicechat;

import com.techwave.svcvisualizer.speaker.SpeakerTracker;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;

/**
 * Makes the "show me when I am speaking" option work. Simple Voice Chat never sends our own voice back
 * to us as a {@code ClientReceiveSoundEvent}, so the local player is never picked up by the normal
 * path. Instead we poll {@link VoicechatClientApi#isTalking()} once per client tick and, while we are
 * transmitting, mark ourselves in the speaker tracker (the {@code showSelf} option then decides whether
 * to actually draw us).
 *
 * <p>{@code isTalking()} / {@code isWhispering()} only exist since Simple Voice Chat 2.6.0, so they are
 * invoked reflectively — the mod still compiles and runs against the older 2.5.x API, where the local
 * indicator simply stays inactive (that API exposes no way to detect the local microphone).
 */
public final class SelfSpeaking {

	private static volatile VoicechatClientApi api;
	private static Method isTalking;
	private static Method isWhispering;

	private SelfSpeaking() {
	}

	/** Called from the plugin's {@code initialize} with the client-side voice chat API. */
	public static void setClientApi(VoicechatClientApi clientApi) {
		api = clientApi;
		isTalking = lookup(clientApi, "isTalking");
		isWhispering = lookup(clientApi, "isWhispering");
		com.techwave.svcvisualizer.SvcVisualizer.LOGGER.info("Local speaking detection {}",
				isTalking != null ? "enabled" : "unavailable (Simple Voice Chat < 2.6.0)");
	}

	/** Poll once per client tick; marks the local player as speaking while the microphone is live. */
	public static void poll() {
		VoicechatClientApi a = api;
		if (a == null || isTalking == null) {
			return; // No API yet, or Simple Voice Chat < 2.6.0 (no local microphone state).
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		try {
			if (Boolean.TRUE.equals(isTalking.invoke(a))) {
				boolean whisper = isWhispering != null && Boolean.TRUE.equals(isWhispering.invoke(a));
				SpeakerTracker.INSTANCE.mark(mc.player.getUUID(), whisper, 0.0, 0.7f);
			}
		} catch (ReflectiveOperationException ignored) {
			// Leave the local indicator inactive if the call is unavailable.
		}
	}

	private static Method lookup(Object target, String name) {
		try {
			return target.getClass().getMethod(name);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}
}
