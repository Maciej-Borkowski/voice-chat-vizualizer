package com.techwave.svcvisualizer.gui;

import com.techwave.svcvisualizer.config.Alignment;
import com.techwave.svcvisualizer.config.ConfigManager;
import com.techwave.svcvisualizer.config.DistanceDisplay;
import com.techwave.svcvisualizer.config.IndicatorStyle;
import com.techwave.svcvisualizer.config.SortMode;
import com.techwave.svcvisualizer.config.StackDirection;
import com.techwave.svcvisualizer.config.VisualizerConfig;
import com.techwave.svcvisualizer.gui.widget.OptionSlider;
import com.techwave.svcvisualizer.hud.RenderUtil;
import com.techwave.svcvisualizer.hud.SpeakerHudRenderer;
import com.techwave.svcvisualizer.speaker.SpeakerTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * The main configuration screen. The settings panel automatically sits on the opposite side of the
 * screen from the overlay preview, so dragging the overlay left moves the panel right (and vice versa)
 * and the panel never hides the thing you are positioning.
 */
public class VisualizerConfigScreen extends Screen {

	private enum Tab {
		POSITION("svcvisualizer.tab.position"),
		APPEARANCE("svcvisualizer.tab.appearance"),
		COLORS("svcvisualizer.tab.colors"),
		BEHAVIOR("svcvisualizer.tab.behavior");

		final String key;

		Tab(String key) {
			this.key = key;
		}
	}

	private final Screen parent;
	private Tab currentTab = Tab.POSITION;

	private int panelW;
	private int panelX;      // left edge of the settings panel (dynamic)
	private int panelMinX;   // left edge of the panel background band
	private int panelMaxX;   // right edge of the panel background band
	private boolean panelOnLeft = true;

	private int viewportTop;
	private int viewportBottom;
	private final int rowH = 24;
	private int visibleRows = 1;
	private int scrollIndex = 0;

	private final List<AbstractWidget> optionWidgets = new ArrayList<>();
	private final Button[] tabButtons = new Button[Tab.values().length];
	private final Button[] bottomButtons = new Button[3];

	private boolean dragging = false;
	private double dragStartMouseX;
	private double dragStartMouseY;
	private double dragStartX;
	private double dragStartY;

	public VisualizerConfigScreen(Screen parent) {
		super(Component.translatable("svcvisualizer.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		SpeakerTracker.INSTANCE.setTestMode(true);

		panelW = Math.min(250, this.width / 2 - 10);
		viewportTop = 52;
		viewportBottom = this.height - 38;
		visibleRows = Math.max(1, (viewportBottom - viewportTop) / rowH);

		Tab[] tabs = Tab.values();
		int tabW = (panelW - 6) / tabs.length;
		for (int i = 0; i < tabs.length; i++) {
			Tab tab = tabs[i];
			tabButtons[i] = Button.builder(Component.translatable(tab.key), b -> setTab(tab))
					.bounds(0, 26, tabW - 2, 20).build();
			addRenderableWidget(tabButtons[i]);
		}

		int bw = (panelW - 12) / 3;
		int by = this.height - 30;
		bottomButtons[0] = Button.builder(Component.translatable("svcvisualizer.button.reset_tab"), b -> resetCurrentTab())
				.bounds(0, by, bw, 20).build();
		bottomButtons[1] = Button.builder(Component.translatable("svcvisualizer.button.reset_all"), b -> resetAll())
				.bounds(0, by, bw, 20).build();
		bottomButtons[2] = Button.builder(Component.translatable("svcvisualizer.button.done"), b -> onClose())
				.bounds(0, by, bw, 20).build();
		for (Button b : bottomButtons) {
			addRenderableWidget(b);
		}

		rebuildOptions();
	}

	private void setTab(Tab tab) {
		currentTab = tab;
		scrollIndex = 0;
		rebuildOptions();
	}

	private void updateTabButtons() {
		Tab[] tabs = Tab.values();
		for (int i = 0; i < tabs.length; i++) {
			tabButtons[i].active = tabs[i] != currentTab;
		}
	}

	private void rebuildOptions() {
		for (AbstractWidget w : optionWidgets) {
			removeWidget(w);
		}
		optionWidgets.clear();
		VisualizerConfig c = ConfigManager.get();
		switch (currentTab) {
			case POSITION -> buildPosition(c);
			case APPEARANCE -> buildAppearance(c);
			case COLORS -> buildColors(c);
			case BEHAVIOR -> buildBehavior(c);
		}
		int maxScroll = Math.max(0, optionWidgets.size() - visibleRows);
		scrollIndex = Math.max(0, Math.min(scrollIndex, maxScroll));
		updateTabButtons();
	}

	// ------------------------------------------------------------------ tab contents

	private void buildPosition(VisualizerConfig c) {
		addSlider("svcvisualizer.option.x", 0, 100, true, () -> c.x * 100.0, v -> c.x = v / 100.0);
		addSlider("svcvisualizer.option.y", 0, 100, true, () -> c.y * 100.0, v -> c.y = v / 100.0);
		addEnum("svcvisualizer.option.alignment", Alignment.values(), c.alignment, v -> c.alignment = v, Alignment::getDisplayName);
		addEnum("svcvisualizer.option.stack", StackDirection.values(), c.stackDirection, v -> c.stackDirection = v, StackDirection::getDisplayName);
		addSlider("svcvisualizer.option.entry_spacing", 0, 40, true, () -> c.entrySpacing, v -> c.entrySpacing = (int) v);
	}

	private void buildAppearance(VisualizerConfig c) {
		addSlider("svcvisualizer.option.icon_size", 4, 48, true, () -> c.iconSize, v -> c.iconSize = (int) v);
		addToggle("svcvisualizer.option.show_face", c.showFace, v -> c.showFace = v);
		addToggle("svcvisualizer.option.show_name", c.showName, v -> c.showName = v);
		addSlider("svcvisualizer.option.name_scale", 50, 200, true, () -> c.nameScale * 100.0, v -> c.nameScale = v / 100.0);
		addSlider("svcvisualizer.option.max_name_width", 20, 260, true, () -> c.maxNameWidth, v -> c.maxNameWidth = (int) v);
		addToggle("svcvisualizer.option.show_icon", c.showSpeakingIcon, v -> c.showSpeakingIcon = v);
		addEnum("svcvisualizer.option.indicator", IndicatorStyle.values(), c.indicatorStyle, v -> c.indicatorStyle = v, IndicatorStyle::getDisplayName);
		addToggle("svcvisualizer.option.show_background", c.showBackground, v -> c.showBackground = v);
		addToggle("svcvisualizer.option.show_border", c.showBorder, v -> c.showBorder = v);
		addSlider("svcvisualizer.option.corner_radius", 0, 8, true, () -> c.cornerRadius, v -> c.cornerRadius = (int) v);
		addSlider("svcvisualizer.option.inner_padding", 0, 16, true, () -> c.innerPadding, v -> c.innerPadding = (int) v);
		addEnum("svcvisualizer.option.distance", DistanceDisplay.values(), c.distanceDisplay, v -> c.distanceDisplay = v, DistanceDisplay::getDisplayName);
	}

	private void buildColors(VisualizerConfig c) {
		addColor("svcvisualizer.option.background_color", () -> c.backgroundColor, v -> c.backgroundColor = v);
		addSlider("svcvisualizer.option.background_opacity", 0, 100, true, () -> c.backgroundOpacity * 100.0, v -> c.backgroundOpacity = v / 100.0);
		addColor("svcvisualizer.option.border_color", () -> c.borderColor, v -> c.borderColor = v);
		addColor("svcvisualizer.option.name_color", () -> c.nameColor, v -> c.nameColor = v);
		addColor("svcvisualizer.option.speaking_color", () -> c.speakingColor, v -> c.speakingColor = v);
		addColor("svcvisualizer.option.whisper_color", () -> c.whisperColor, v -> c.whisperColor = v);
	}

	private void buildBehavior(VisualizerConfig c) {
		addToggle("svcvisualizer.option.enabled", c.enabled, v -> c.enabled = v);
		addToggle("svcvisualizer.option.show_self", c.showSelf, v -> c.showSelf = v);
		addToggle("svcvisualizer.option.whisper_distinct", c.whisperDistinct, v -> c.whisperDistinct = v);
		addEnum("svcvisualizer.option.sort", SortMode.values(), c.sortMode, v -> c.sortMode = v, SortMode::getDisplayName);
		addSlider("svcvisualizer.option.max_entries", 1, 32, true, () -> c.maxEntries, v -> c.maxEntries = (int) v);
		addSlider("svcvisualizer.option.speaking_timeout", 50, 2000, true, () -> c.speakingTimeoutMs, v -> c.speakingTimeoutMs = (int) v);
		addSlider("svcvisualizer.option.hold", 0, 2000, true, () -> c.holdMs, v -> c.holdMs = (int) v);
		addSlider("svcvisualizer.option.fade_in", 0, 2000, true, () -> c.fadeInMs, v -> c.fadeInMs = (int) v);
		addSlider("svcvisualizer.option.fade_out", 0, 4000, true, () -> c.fadeOutMs, v -> c.fadeOutMs = (int) v);
		addToggle("svcvisualizer.option.slide_in", c.slideInAnimation, v -> c.slideInAnimation = v);
		addSlider("svcvisualizer.option.level_sensitivity", 20, 300, true, () -> c.levelSensitivity * 100.0, v -> c.levelSensitivity = v / 100.0);
		addToggle("svcvisualizer.option.render_only_in_game", c.renderOnlyInGame, v -> c.renderOnlyInGame = v);
		addToggle("svcvisualizer.option.respect_hud_hidden", c.respectHudHidden, v -> c.respectHudHidden = v);
		addToggle("svcvisualizer.option.hide_chat", c.hideWhenChatOpen, v -> c.hideWhenChatOpen = v);
	}

	// ------------------------------------------------------------------ widget builders

	private int optionX() {
		return panelX + 4;
	}

	private int optionW() {
		return panelW - 8;
	}

	private void addOption(AbstractWidget w) {
		optionWidgets.add(w);
		addRenderableWidget(w);
	}

	private void addSlider(String key, double min, double max, boolean integer,
	                       java.util.function.DoubleSupplier getter, java.util.function.DoubleConsumer setter) {
		addOption(new OptionSlider(optionX(), 0, optionW(), 20, key, min, max, integer, getter, setter));
	}

	private void addToggle(String key, boolean initial, Consumer<Boolean> setter) {
		CycleButton<Boolean> btn = CycleButton.onOffBuilder(initial)
				.create(optionX(), 0, optionW(), 20, Component.translatable(key), (b, val) -> setter.accept(val));
		addOption(btn);
	}

	private <E extends Enum<E>> void addEnum(String key, E[] values, E initial, Consumer<E> setter, Function<E, Component> nameFn) {
		CycleButton<E> btn = CycleButton.builder(nameFn, initial).withValues(values)
				.create(optionX(), 0, optionW(), 20, Component.translatable(key), (b, val) -> setter.accept(val));
		addOption(btn);
	}

	private void addColor(String key, IntSupplier getter, IntConsumer setter) {
		Component label = Component.translatable(key).append(Component.literal(String.format(": #%06X", getter.getAsInt() & 0xFFFFFF)));
		Button btn = Button.builder(label, b ->
				this.minecraft.setScreenAndShow(new ColorPickerScreen(this, key, getter.getAsInt(), rgb -> {
					setter.accept(rgb & 0xFFFFFF);
					ConfigManager.save();
				}))).bounds(optionX(), 0, optionW(), 20).build();
		addOption(btn);
	}

	// ------------------------------------------------------------------ reset

	private void resetCurrentTab() {
		VisualizerConfig c = ConfigManager.get();
		switch (currentTab) {
			case POSITION -> c.resetPosition();
			case APPEARANCE -> c.resetAppearance();
			case COLORS -> c.resetColors();
			case BEHAVIOR -> c.resetGeneral();
		}
		ConfigManager.save();
		rebuildOptions();
	}

	private void resetAll() {
		VisualizerConfig c = ConfigManager.get();
		c.resetGeneral();
		c.resetPosition();
		c.resetAppearance();
		c.resetColors();
		ConfigManager.save();
		rebuildOptions();
	}

	// ------------------------------------------------------------------ layout

	/** Put the panel on the opposite side of the screen from the overlay. */
	private void computePanelSide() {
		int[] b = SpeakerHudRenderer.lastBounds;
		double overlayCenter = (b != null) ? (b[0] + b[2]) / 2.0 : ConfigManager.get().x * this.width;
		panelOnLeft = overlayCenter >= this.width / 2.0;
		panelX = panelOnLeft ? 8 : (this.width - panelW - 8);
		panelMinX = panelX - 8;
		panelMaxX = panelX + panelW + 8;
	}

	private void layoutChrome() {
		Tab[] tabs = Tab.values();
		int tabW = (panelW - 6) / tabs.length;
		for (int i = 0; i < tabs.length; i++) {
			tabButtons[i].setX(panelX + i * tabW);
		}
		int bw = (panelW - 12) / 3;
		for (int k = 0; k < bottomButtons.length; k++) {
			bottomButtons[k].setX(panelX + k * (bw + 4));
		}
	}

	private void layoutOptions() {
		for (int i = 0; i < optionWidgets.size(); i++) {
			AbstractWidget w = optionWidgets.get(i);
			int wy = viewportTop + (i - scrollIndex) * rowH;
			w.setPosition(optionX(), wy);
			boolean vis = i >= scrollIndex && i < scrollIndex + visibleRows;
			w.visible = vis;
			w.active = vis;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// Dim manually (no vanilla blur – it may only be requested once per frame).
		g.fill(0, 0, this.width, this.height, 0xC0101014);
		long now = System.currentTimeMillis();

		// Full-screen live preview, then decide which side the panel goes on.
		SpeakerHudRenderer.renderOverlay(g, now);
		computePanelSide();
		drawDragHandle(g, now);

		layoutChrome();
		layoutOptions();

		// Settings panel background band.
		int bgL = Math.max(0, panelMinX);
		int bgR = Math.min(this.width, panelMaxX);
		g.fill(bgL, 0, bgR, this.height, 0xC00E0E12);
		if (panelOnLeft) {
			g.fill(bgR - 1, 0, bgR, this.height, 0x80000000);
		} else {
			g.fill(bgL, 0, bgL + 1, this.height, 0x80000000);
		}

		for (GuiEventListener child : this.children()) {
			if (child instanceof Renderable r) {
				r.extractRenderState(g, mouseX, mouseY, delta);
			}
		}

		g.text(this.font, this.title, panelX + 2, 10, 0xFFFFFFFF);
		drawScrollbar(g);
		int hintX = panelOnLeft ? panelMaxX + 6 : 6;
		g.text(this.font, Component.translatable("svcvisualizer.config.drag_hint"), hintX, this.height - 14, 0x80FFFFFF);
	}

	private void drawDragHandle(GuiGraphicsExtractor g, long now) {
		int[] b = SpeakerHudRenderer.lastBounds;
		if (b == null) {
			return;
		}
		float pulse = 0.5f + 0.5f * (float) Math.sin(now / 300.0);
		int color = RenderUtil.argb(0x4FC3F7, 0.5f + 0.4f * pulse);
		RenderUtil.drawBorder(g, b[0] - 2, b[1] - 2, b[2] + 2, b[3] + 2, color);
	}

	private void drawScrollbar(GuiGraphicsExtractor g) {
		int total = optionWidgets.size();
		if (total <= visibleRows) {
			return;
		}
		int trackX = panelX + panelW - 5;
		int trackTop = viewportTop;
		int trackH = viewportBottom - viewportTop;
		g.fill(trackX, trackTop, trackX + 3, trackTop + trackH, 0x40FFFFFF);
		int thumbH = Math.max(12, trackH * visibleRows / total);
		int maxScroll = total - visibleRows;
		int thumbY = trackTop + (maxScroll == 0 ? 0 : (trackH - thumbH) * scrollIndex / maxScroll);
		g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xC0FFFFFF);
	}

	// ------------------------------------------------------------------ input

	private boolean inFreeArea(double mouseX) {
		return mouseX < panelMinX || mouseX > panelMaxX;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		if (event.button() == 0 && inFreeArea(mouseX)) {
			int[] b = SpeakerHudRenderer.lastBounds;
			if (b != null && mouseX >= b[0] - 3 && mouseX <= b[2] + 3 && mouseY >= b[1] - 3 && mouseY <= b[3] + 3) {
				dragging = true;
				dragStartMouseX = mouseX;
				dragStartMouseY = mouseY;
				VisualizerConfig c = ConfigManager.get();
				dragStartX = c.x;
				dragStartY = c.y;
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging) {
			VisualizerConfig c = ConfigManager.get();
			c.x = clamp01(dragStartX + (event.x() - dragStartMouseX) / this.width);
			c.y = clamp01(dragStartY + (event.y() - dragStartMouseY) / this.height);
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging && event.button() == 0) {
			dragging = false;
			ConfigManager.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!inFreeArea(mouseX) && mouseY >= viewportTop && mouseY <= viewportBottom) {
			int maxScroll = Math.max(0, optionWidgets.size() - visibleRows);
			scrollIndex = Math.max(0, Math.min(maxScroll, scrollIndex - (int) Math.signum(scrollY)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void onClose() {
		ConfigManager.save();
		SpeakerTracker.INSTANCE.setTestMode(false);
		this.minecraft.setScreenAndShow(parent);
	}

	@Override
	public void removed() {
		SpeakerTracker.INSTANCE.setTestMode(false);
	}

	private static double clamp01(double v) {
		return v < 0 ? 0 : Math.min(1, v);
	}
}
