package com.techwave.svcvisualizer.integration;

import com.techwave.svcvisualizer.gui.VisualizerConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Adds a "Configure" button for this mod in the Mod Menu mod list. */
public class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return VisualizerConfigScreen::new;
	}
}
