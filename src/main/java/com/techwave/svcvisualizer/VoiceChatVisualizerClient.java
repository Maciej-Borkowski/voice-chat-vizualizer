package com.techwave.svcvisualizer;

import com.mojang.blaze3d.platform.InputConstants;
import com.techwave.svcvisualizer.config.ConfigManager;
import com.techwave.svcvisualizer.config.VisualizerConfig;
import com.techwave.svcvisualizer.gui.VisualizerConfigScreen;
import com.techwave.svcvisualizer.hud.SpeakerHudRenderer;
import com.techwave.svcvisualizer.speaker.SpeakerTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;

/** Client entrypoint: loads config, registers the overlay HUD element, keybinds and lifecycle hooks. */
public class VoiceChatVisualizerClient implements ClientModInitializer {

	private static KeyMapping openConfigKey;
	private static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		ConfigManager.load();

		String category = "key.categories.svcvisualizer.general";
		openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.svcvisualizer.open_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, category));
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.svcvisualizer.toggle", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category));

		HudElementRegistry.addLast(SvcVisualizer.id("overlay"), (guiGraphics, deltaTracker) -> {
			if (shouldRenderHud()) {
				SpeakerHudRenderer.renderOverlay(guiGraphics, System.currentTimeMillis());
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Detect our own microphone so "show me when I am speaking" works (SVC never echoes it back).
			com.techwave.svcvisualizer.voicechat.SelfSpeaking.poll();
			while (openConfigKey.consumeClick()) {
				client.setScreen(new VisualizerConfigScreen(client.screen));
			}
			while (toggleKey.consumeClick()) {
				VisualizerConfig config = ConfigManager.get();
				config.enabled = !config.enabled;
				ConfigManager.save();
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SpeakerTracker.INSTANCE.clear());

		SvcVisualizer.LOGGER.info("{} initialized", SvcVisualizer.MOD_NAME);
	}

	private static boolean shouldRenderHud() {
		VisualizerConfig config = ConfigManager.get();
		if (!config.enabled) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		// The config screen paints its own live preview – don't double-render behind it.
		if (mc.screen instanceof VisualizerConfigScreen) {
			return false;
		}
		if (config.respectHudHidden && mc.options.hideGui) {
			return false;
		}
		if (config.renderOnlyInGame && mc.level == null) {
			return false;
		}
		if (config.hideWhenChatOpen && mc.screen instanceof ChatScreen) {
			return false;
		}
		return true;
	}
}
