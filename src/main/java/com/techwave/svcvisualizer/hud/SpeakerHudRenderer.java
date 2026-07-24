package com.techwave.svcvisualizer.hud;

import com.techwave.svcvisualizer.config.Alignment;
import com.techwave.svcvisualizer.config.ConfigManager;
import com.techwave.svcvisualizer.config.DistanceDisplay;
import com.techwave.svcvisualizer.config.IndicatorStyle;
import com.techwave.svcvisualizer.config.StackDirection;
import com.techwave.svcvisualizer.config.VisualizerConfig;
import com.techwave.svcvisualizer.speaker.SpeakerState;
import com.techwave.svcvisualizer.speaker.SpeakerTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the "who is speaking" overlay: a vertical list of rows, each showing a player's face, name and
 * an animated speaking waveform. Layout is fully driven by {@link VisualizerConfig} and is reused by
 * both the in-game HUD and the config screen's live preview.
 */
public final class SpeakerHudRenderer {

	/** Bounds (screen pixels) of the last block drawn – used by the config screen for dragging. */
	public static int[] lastBounds = null;

	private static final int SCREEN_MARGIN = 2;

	private SpeakerHudRenderer() {
	}

	/** Render the overlay using the live config and the current speaker snapshot. */
	public static void renderOverlay(GuiGraphics g, long now) {
		VisualizerConfig c = ConfigManager.get();
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;

		SpeakerTracker.Snapshot snap = SpeakerTracker.INSTANCE.snapshot(now, c);
		if (snap.speakers().isEmpty()) {
			lastBounds = null;
			return;
		}

		LayoutResult layout = computeLayout(c, snap.speakers(), g.guiWidth(), g.guiHeight(), font, now);
		lastBounds = new int[]{layout.blockX1, layout.blockY1, layout.blockX2, layout.blockY2};

		for (RowLayout row : layout.rows) {
			drawRow(g, c, font, mc, row, now);
		}

		if (snap.overflow() > 0) {
			drawOverflow(g, c, font, layout, snap.overflow());
		}
	}

	// ------------------------------------------------------------------ layout

	public static LayoutResult computeLayout(VisualizerConfig c, List<SpeakerState> speakers,
	                                         int screenW, int screenH, Font font, long now) {
		int pad = c.innerPadding;
		int gap = Math.max(2, c.innerPadding);
		int iconSize = c.showFace ? c.iconSize : 0;
		int textHeight = Math.round(font.lineHeight * (float) c.nameScale);
		int rowH = Math.max(iconSize, textHeight) + pad * 2;
		int indW = indicatorWidth(c);
		int indH = indicatorHeight(c);

		List<RowLayout> rows = new ArrayList<>();
		int maxRowW = 0;
		for (SpeakerState s : speakers) {
			String name = buildLabel(c, s);
			String shown = truncate(font, name, c.maxNameWidth);
			int nameW = Math.round(font.width(shown) * (float) c.nameScale);

			int contentW = pad;
			boolean first = true;
			if (c.showFace) {
				contentW += iconSize;
				first = false;
			}
			if (c.showName) {
				contentW += (first ? 0 : gap) + nameW;
				first = false;
			}
			if (c.showSpeakingIcon) {
				contentW += (first ? 0 : gap) + indW;
			}
			contentW += pad;

			RowLayout row = new RowLayout();
			row.state = s;
			row.label = shown;
			row.nameWidth = nameW;
			row.width = contentW;
			row.height = rowH;
			row.iconSize = iconSize;
			row.indicatorW = indW;
			row.indicatorH = indH;
			row.alpha = SpeakerTracker.alpha(s, now, c);
			rows.add(row);
			maxRowW = Math.max(maxRowW, contentW);
		}

		int n = rows.size();
		int totalH = n * rowH + (n - 1) * c.entrySpacing;
		int refX = Math.round((float) c.x * screenW);
		int refY = Math.round((float) c.y * screenH);

		// Vertical + horizontal placement per row.
		for (int i = 0; i < n; i++) {
			RowLayout row = rows.get(i);
			int top;
			if (c.stackDirection == StackDirection.DOWN) {
				top = refY + i * (rowH + c.entrySpacing);
			} else {
				top = refY - rowH - i * (rowH + c.entrySpacing);
			}
			int x1 = (c.alignment == Alignment.LEFT) ? refX : refX - row.width;
			row.x = x1;
			row.y = top;
		}

		// Block bounds.
		int blockX1 = (c.alignment == Alignment.LEFT) ? refX : refX - maxRowW;
		int blockX2 = blockX1 + maxRowW;
		int blockY1 = (c.stackDirection == StackDirection.DOWN) ? refY : refY - totalH;
		int blockY2 = blockY1 + totalH;

		// Keep on screen.
		int dx = 0;
		int dy = 0;
		if (blockX1 < SCREEN_MARGIN) {
			dx = SCREEN_MARGIN - blockX1;
		} else if (blockX2 > screenW - SCREEN_MARGIN) {
			dx = (screenW - SCREEN_MARGIN) - blockX2;
		}
		if (blockY1 < SCREEN_MARGIN) {
			dy = SCREEN_MARGIN - blockY1;
		} else if (blockY2 > screenH - SCREEN_MARGIN) {
			dy = (screenH - SCREEN_MARGIN) - blockY2;
		}
		if (dx != 0 || dy != 0) {
			for (RowLayout row : rows) {
				row.x += dx;
				row.y += dy;
			}
			blockX1 += dx;
			blockX2 += dx;
			blockY1 += dy;
			blockY2 += dy;
		}

		LayoutResult result = new LayoutResult();
		result.rows = rows;
		result.blockX1 = blockX1;
		result.blockY1 = blockY1;
		result.blockX2 = blockX2;
		result.blockY2 = blockY2;
		return result;
	}

	// ------------------------------------------------------------------ drawing

	private static void drawRow(GuiGraphics g, VisualizerConfig c, Font font, Minecraft mc,
	                            RowLayout row, long now) {
		float alpha = row.alpha;
		if (alpha <= 0f) {
			return;
		}
		int pad = c.innerPadding;
		int gap = Math.max(2, c.innerPadding);

		int x1 = row.x;
		int y1 = row.y;

		// Slide-in animation.
		if (c.slideInAnimation && c.fadeInMs > 0) {
			long age = now - row.state.firstPacketMs;
			if (age < c.fadeInMs) {
				float appear = (float) age / (float) c.fadeInMs;
				int offset = Math.round((1f - appear) * 12f);
				x1 += (c.alignment == Alignment.LEFT) ? -offset : offset;
			}
		}

		int x2 = x1 + row.width;
		int y2 = y1 + row.height;

		if (c.showBackground) {
			int bg = RenderUtil.argb(c.backgroundColor, (float) c.backgroundOpacity * alpha);
			RenderUtil.fillRoundedRect(g, x1, y1, x2, y2, c.cornerRadius, bg);
		}
		if (c.showBorder) {
			RenderUtil.drawBorder(g, x1, y1, x2, y2, RenderUtil.argb(c.borderColor, alpha));
		}

		int cx = x1 + pad;
		boolean first = true;

		if (c.showFace) {
			PlayerSkin skin = resolveSkin(mc, row.state);
			int iconY = y1 + (row.height - row.iconSize) / 2;
			RenderUtil.drawHead(g, skin, cx, iconY, row.iconSize, alpha);
			cx += row.iconSize;
			first = false;
		}

		if (c.showName) {
			if (!first) {
				cx += gap;
			}
			int textHeight = Math.round(font.lineHeight * (float) c.nameScale);
			int textY = y1 + (row.height - textHeight) / 2;
			int color = RenderUtil.argb(c.nameColor, alpha);
			if (c.nameScale == 1.0) {
				g.drawString(font, row.label, cx, textY, color, true);
			} else {
				Matrix3x2fStack pose = g.pose();
				pose.pushMatrix();
				pose.translate(cx, textY);
				pose.scale((float) c.nameScale, (float) c.nameScale);
				g.drawString(font, row.label, 0, 0, color, true);
				pose.popMatrix();
			}
			cx += row.nameWidth;
			first = false;
		}

		if (c.showSpeakingIcon) {
			if (!first) {
				cx += gap;
			}
			int indY = y1 + (row.height - row.indicatorH) / 2;
			int rgb = row.state.whispering && c.whisperDistinct ? c.whisperColor : c.speakingColor;
			float indAlpha = row.state.whispering && c.whisperDistinct ? alpha * (float) c.whisperDimFactor : alpha;
			float level = Math.min(1f, row.state.level * (float) c.levelSensitivity);
			SpeakingIndicator.render(g, c.indicatorStyle, cx, indY, row.indicatorW, row.indicatorH,
					level, rgb, indAlpha, now);
		}
	}

	private static void drawOverflow(GuiGraphics g, VisualizerConfig c, Font font,
	                                 LayoutResult layout, int overflow) {
		String text = "+" + overflow;
		int w = font.width(text);
		int x = (c.alignment == Alignment.LEFT) ? layout.blockX1 + c.innerPadding : layout.blockX2 - c.innerPadding - w;
		int y = (c.stackDirection == StackDirection.DOWN) ? layout.blockY2 + 2 : layout.blockY1 - font.lineHeight - 2;
		g.drawString(font, text, x, y, RenderUtil.argb(c.nameColor, 0.85f), true);
	}

	// ------------------------------------------------------------------ helpers

	private static PlayerSkin resolveSkin(Minecraft mc, SpeakerState state) {
		ClientPacketListener connection = mc.getConnection();
		if (connection != null) {
			PlayerInfo info = connection.getPlayerInfo(state.id);
			if (info != null) {
				return info.getSkin();
			}
		}
		return DefaultPlayerSkin.get(state.id);
	}

	private static String buildLabel(VisualizerConfig c, SpeakerState s) {
		String name = SpeakerTracker.resolveName(s);
		if (c.distanceDisplay == DistanceDisplay.BLOCKS && s.distance > 0.01) {
			name = name + " " + Math.round(s.distance) + "m";
		}
		return name;
	}

	private static String truncate(Font font, String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		String ellipsis = "...";
		String sub = font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width(ellipsis)));
		return sub + ellipsis;
	}

	private static int indicatorWidth(VisualizerConfig c) {
		return switch (c.indicatorStyle) {
			case WAVE -> c.iconSize + 6;
			case BARS -> Math.max(6, Math.round(c.iconSize * 0.7f));
			case DOT -> Math.max(6, Math.round(c.iconSize * 0.6f));
		};
	}

	private static int indicatorHeight(VisualizerConfig c) {
		return Math.max(6, Math.round(c.iconSize * 0.62f));
	}

	// ------------------------------------------------------------------ data holders

	public static final class RowLayout {
		public SpeakerState state;
		public String label;
		public int nameWidth;
		public int width;
		public int height;
		public int iconSize;
		public int indicatorW;
		public int indicatorH;
		public int x;
		public int y;
		public float alpha;
	}

	public static final class LayoutResult {
		public List<RowLayout> rows;
		public int blockX1;
		public int blockY1;
		public int blockX2;
		public int blockY2;
	}
}
