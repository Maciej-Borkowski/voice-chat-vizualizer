package com.techwave.svcvisualizer.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.world.entity.player.PlayerSkin;

/** Small drawing helpers shared by the overlay and the config preview. */
public final class RenderUtil {

	private RenderUtil() {
	}

	/** Combine an {@code 0xRRGGBB} colour with a 0..1 alpha into an ARGB int. */
	public static int argb(int rgb, float alpha) {
		int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	/** Multiply the alpha channel of an existing ARGB colour by {@code factor}. */
	public static int scaleAlpha(int argb, float factor) {
		int a = (argb >>> 24) & 0xFF;
		a = Math.max(0, Math.min(255, Math.round(a * factor)));
		return (a << 24) | (argb & 0xFFFFFF);
	}

	/** Filled rectangle with (approximately) rounded corners. */
	public static void fillRoundedRect(GuiGraphics g, int x1, int y1, int x2, int y2, int radius, int color) {
		if (radius <= 0) {
			g.fill(x1, y1, x2, y2, color);
			return;
		}
		radius = Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
		// Central cross shape.
		g.fill(x1 + radius, y1, x2 - radius, y2, color);
		g.fill(x1, y1 + radius, x1 + radius, y2 - radius, color);
		g.fill(x2 - radius, y1 + radius, x2, y2 - radius, color);
		// Step the corners for a soft edge.
		for (int i = 0; i < radius; i++) {
			int inset = radius - approxCorner(radius, i);
			g.fill(x1 + inset, y1 + i, x1 + radius, y1 + i + 1, color);           // top-left
			g.fill(x2 - radius, y1 + i, x2 - inset, y1 + i + 1, color);           // top-right
			g.fill(x1 + inset, y2 - i - 1, x1 + radius, y2 - i, color);           // bottom-left
			g.fill(x2 - radius, y2 - i - 1, x2 - inset, y2 - i, color);           // bottom-right
		}
	}

	private static int approxCorner(int radius, int row) {
		// How many pixels to cut from the corner on this row (circle approximation).
		double dy = radius - row - 0.5;
		double dx = Math.sqrt(Math.max(0, radius * radius - dy * dy));
		return (int) Math.round(radius - dx);
	}

	/** 1px border around a rectangle. */
	public static void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2, y1 + 1, color);
		g.fill(x1, y2 - 1, x2, y2, color);
		g.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
		g.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
	}

	/** Draw a player head (base + hat) at the given position, tinted with the given alpha. */
	public static void drawHead(GuiGraphics g, PlayerSkin skin, int x, int y, int size, float alpha) {
		int tint = argb(0xFFFFFF, alpha);
		PlayerFaceRenderer.draw(g, skin, x, y, size, tint);
	}
}
