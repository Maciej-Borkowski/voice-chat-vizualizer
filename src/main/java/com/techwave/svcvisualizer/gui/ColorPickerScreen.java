package com.techwave.svcvisualizer.gui;

import com.techwave.svcvisualizer.gui.widget.OptionSlider;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/** A small HSV + hex colour picker. Returns the chosen {@code 0xRRGGBB} colour via a callback. */
public class ColorPickerScreen extends Screen {

	private final Screen parent;
	private final IntConsumer onDone;

	private float hue;        // 0..360
	private float sat;        // 0..1
	private float val;        // 0..1
	private int currentRgb;
	private boolean syncing = false;

	private OptionSlider hueSlider;
	private OptionSlider satSlider;
	private OptionSlider valSlider;
	private EditBox hexBox;

	public ColorPickerScreen(Screen parent, String titleKey, int initialRgb, IntConsumer onDone) {
		super(Component.translatable(titleKey));
		this.parent = parent;
		this.onDone = onDone;
		this.currentRgb = initialRgb & 0xFFFFFF;
		float[] hsv = rgbToHsv(this.currentRgb);
		this.hue = hsv[0];
		this.sat = hsv[1];
		this.val = hsv[2];
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int w = 200;
		int x = cx - w / 2;
		int y = this.height / 2 - 60;

		hueSlider = new OptionSlider(x, y, w, 20, "svcvisualizer.color.hue", 0, 360, true,
				() -> hue, v -> { hue = (float) v; onHsvChanged(); });
		satSlider = new OptionSlider(x, y + 24, w, 20, "svcvisualizer.color.saturation", 0, 100, true,
				() -> sat * 100.0, v -> { sat = (float) (v / 100.0); onHsvChanged(); });
		valSlider = new OptionSlider(x, y + 48, w, 20, "svcvisualizer.color.value", 0, 100, true,
				() -> val * 100.0, v -> { val = (float) (v / 100.0); onHsvChanged(); });
		addRenderableWidget(hueSlider);
		addRenderableWidget(satSlider);
		addRenderableWidget(valSlider);

		hexBox = new EditBox(this.font, x + 40, y + 76, w - 40, 20, Component.translatable("svcvisualizer.color.hex"));
		hexBox.setMaxLength(7);
		hexBox.setValue(String.format("#%06X", currentRgb));
		hexBox.setResponder(this::onHexTyped);
		addRenderableWidget(hexBox);

		addRenderableWidget(Button.builder(Component.translatable("svcvisualizer.button.done"), b -> {
			onDone.accept(currentRgb);
			onClose();
		}).bounds(cx - 100, y + 108, 96, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("svcvisualizer.button.cancel"), b -> onClose())
				.bounds(cx + 4, y + 108, 96, 20).build());
	}

	private void onHsvChanged() {
		if (syncing) {
			return;
		}
		currentRgb = hsvToRgb(hue, sat, val);
		syncing = true;
		hexBox.setValue(String.format("#%06X", currentRgb));
		syncing = false;
	}

	private void onHexTyped(String text) {
		if (syncing) {
			return;
		}
		String clean = text.startsWith("#") ? text.substring(1) : text;
		if (clean.length() != 6) {
			return;
		}
		try {
			int rgb = Integer.parseInt(clean, 16) & 0xFFFFFF;
			currentRgb = rgb;
			float[] hsv = rgbToHsv(rgb);
			hue = hsv[0];
			sat = hsv[1];
			val = hsv[2];
			syncing = true;
			hueSlider.setActual(hue);
			satSlider.setActual(sat * 100.0);
			valSlider.setActual(val * 100.0);
			syncing = false;
		} catch (NumberFormatException ignored) {
			// leave values unchanged for partial input
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// Manual dim (no blur – see VisualizerConfigScreen) then widgets, then labels on top.
		g.fill(0, 0, this.width, this.height, 0xC0101014);
		for (GuiEventListener child : this.children()) {
			if (child instanceof Renderable r) {
				r.extractRenderState(g, mouseX, mouseY, delta);
			}
		}
		int cx = this.width / 2;
		int y = this.height / 2 - 60;
		g.centeredText(this.font, this.title, cx, y - 40, 0xFFFFFFFF);

		// Preview swatch.
		int sw = 30;
		int px = cx - 100;
		g.fill(px, y + 76, px + sw, y + 96, 0xFF000000 | currentRgb);
		g.fill(px, y + 76, px + sw, y + 77, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(parent);
	}

	// ---- colour maths ----

	private static int hsvToRgb(float h, float s, float v) {
		float c = v * s;
		float hp = (h % 360f) / 60f;
		float xx = c * (1f - Math.abs(hp % 2f - 1f));
		float r = 0, g = 0, b = 0;
		if (hp < 1) { r = c; g = xx; }
		else if (hp < 2) { r = xx; g = c; }
		else if (hp < 3) { g = c; b = xx; }
		else if (hp < 4) { g = xx; b = c; }
		else if (hp < 5) { r = xx; b = c; }
		else { r = c; b = xx; }
		float m = v - c;
		int ri = Math.round((r + m) * 255f);
		int gi = Math.round((g + m) * 255f);
		int bi = Math.round((b + m) * 255f);
		return (ri << 16) | (gi << 8) | bi;
	}

	private static float[] rgbToHsv(int rgb) {
		float r = ((rgb >> 16) & 0xFF) / 255f;
		float g = ((rgb >> 8) & 0xFF) / 255f;
		float b = (rgb & 0xFF) / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float d = max - min;
		float h = 0;
		if (d != 0) {
			if (max == r) {
				h = ((g - b) / d) % 6f;
			} else if (max == g) {
				h = (b - r) / d + 2f;
			} else {
				h = (r - g) / d + 4f;
			}
			h *= 60f;
			if (h < 0) {
				h += 360f;
			}
		}
		float s = max == 0 ? 0 : d / max;
		return new float[]{h, s, max};
	}
}
