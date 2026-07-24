package com.techwave.svcvisualizer.hud;

import com.techwave.svcvisualizer.config.IndicatorStyle;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws the "is speaking" indicator inside a box. Its animation is driven by wall-clock time and its
 * amplitude by the speaker's loudness ({@code level}), so it visibly reacts to how loud someone talks.
 */
public final class SpeakingIndicator {

	private SpeakingIndicator() {
	}

	public static void render(GuiGraphics g, IndicatorStyle style, int x, int y, int w, int h,
	                          float level, int rgb, float alpha, long now) {
		int color = RenderUtil.argb(rgb, alpha);
		float lv = Math.max(0f, Math.min(1f, level));
		switch (style) {
			case WAVE -> wave(g, x, y, w, h, lv, color, now);
			case BARS -> bars(g, x, y, w, h, lv, color, now);
			case DOT -> dot(g, x, y, w, h, lv, color, now);
		}
	}

	private static void wave(GuiGraphics g, int x, int y, int w, int h, float level, int color, long now) {
		int mid = y + h / 2;
		double amp = (h / 2.0 - 1.0) * (0.20 + 0.80 * level);
		double freq = (2.0 * Math.PI * 2.0) / Math.max(1, w);
		double t = now / 130.0;
		int prevY = mid;
		for (int i = 0; i < w; i++) {
			double yy = mid + amp * Math.sin(t + i * freq);
			int cy = (int) Math.round(yy);
			int top = Math.min(prevY, cy);
			int bot = Math.max(prevY, cy);
			g.fill(x + i, top, x + i + 1, bot + 1, color);
			prevY = cy;
		}
	}

	private static void bars(GuiGraphics g, int x, int y, int w, int h, float level, int color, long now) {
		int count = 4;
		int gap = 1;
		int bw = Math.max(1, (w - (count - 1) * gap) / count);
		for (int i = 0; i < count; i++) {
			double phase = now / 150.0 + i * 0.7;
			double v = level * (0.45 + 0.55 * Math.abs(Math.sin(phase)));
			int bh = Math.max(2, (int) Math.round(h * (0.18 + 0.82 * v)));
			int bx = x + i * (bw + gap);
			g.fill(bx, y + (h - bh), bx + bw, y + h, color);
		}
	}

	private static void dot(GuiGraphics g, int x, int y, int w, int h, float level, int color, long now) {
		double pulse = 0.5 + 0.5 * Math.sin(now / 170.0);
		double s = (0.45 + 0.55 * level) * (0.7 + 0.3 * pulse);
		int size = Math.max(2, (int) Math.round(Math.min(w, h) * s));
		int cx = x + w / 2;
		int cy = y + h / 2;
		g.fill(cx - size / 2, cy - size / 2, cx - size / 2 + size, cy - size / 2 + size, color);
	}
}
