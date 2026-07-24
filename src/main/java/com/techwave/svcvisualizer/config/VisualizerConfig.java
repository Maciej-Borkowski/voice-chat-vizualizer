package com.techwave.svcvisualizer.config;

/**
 * All persisted settings for the overlay. Plain POJO serialised to JSON by {@link ConfigManager}.
 * Colours are stored as {@code 0xRRGGBB} integers; opacity/alpha is stored separately where relevant.
 */
public class VisualizerConfig {

	/** Bump when the schema changes so {@link ConfigManager} can migrate old files. */
	public int configVersion = 1;

	// --- General / behaviour ---
	public boolean enabled = true;
	public boolean showSelf = true;
	public boolean whisperDistinct = true;
	public boolean renderOnlyInGame = true;
	public boolean respectHudHidden = true;
	public boolean hideWhenChatOpen = false;
	public SortMode sortMode = SortMode.RECENT;
	public double whisperDimFactor = 0.55;
	public int maxEntries = 8;
	public int speakingTimeoutMs = 300;
	public int holdMs = 150;
	public int fadeInMs = 120;
	public int fadeOutMs = 400;
	public boolean slideInAnimation = true;
	public double levelSensitivity = 1.0;

	// --- Position / layout ---
	/** Reference point as a fraction of the screen (0..1). */
	public double x = 0.99;
	public double y = 0.06;
	public Alignment alignment = Alignment.RIGHT;
	public StackDirection stackDirection = StackDirection.DOWN;
	public int entrySpacing = 0;

	// --- Appearance ---
	public int iconSize = 8;
	public boolean showFace = true;
	public boolean showName = true;
	public double nameScale = 1.0;
	public int maxNameWidth = 120;
	public boolean showSpeakingIcon = true;
	// Face -> name -> animated sine wave (the "someone is talking" waveform).
	public IndicatorStyle indicatorStyle = IndicatorStyle.WAVE;
	public boolean showBackground = false;
	public boolean showBorder = false;
	public int cornerRadius = 3;
	public int innerPadding = 4;
	public DistanceDisplay distanceDisplay = DistanceDisplay.OFF;

	// --- Colours (0xRRGGBB) ---
	public int backgroundColor = 0x000000;
	public double backgroundOpacity = 0.45;
	public int borderColor = 0xFFFFFF;
	public int nameColor = 0xFFFFFF;
	public int speakingColor = 0x55FF55;
	public int whisperColor = 0xAAAAAA;

	/** Clamp every numeric field into a sane range. Called after loading from disk. */
	public void sanitize() {
		VisualizerConfig d = new VisualizerConfig();
		if (sortMode == null) sortMode = d.sortMode;
		if (alignment == null) alignment = d.alignment;
		if (stackDirection == null) stackDirection = d.stackDirection;
		if (indicatorStyle == null) indicatorStyle = d.indicatorStyle;
		if (distanceDisplay == null) distanceDisplay = d.distanceDisplay;

		maxEntries = clampI(maxEntries, 1, 32);
		speakingTimeoutMs = clampI(speakingTimeoutMs, 50, 2000);
		holdMs = clampI(holdMs, 0, 2000);
		fadeInMs = clampI(fadeInMs, 0, 2000);
		fadeOutMs = clampI(fadeOutMs, 0, 4000);
		levelSensitivity = clampD(levelSensitivity, 0.2, 3.0);
		whisperDimFactor = clampD(whisperDimFactor, 0.1, 1.0);

		x = clampD(x, 0.0, 1.0);
		y = clampD(y, 0.0, 1.0);
		entrySpacing = clampI(entrySpacing, 0, 40);

		iconSize = clampI(iconSize, 4, 48);
		nameScale = clampD(nameScale, 0.5, 2.0);
		maxNameWidth = clampI(maxNameWidth, 20, 260);
		cornerRadius = clampI(cornerRadius, 0, 8);
		innerPadding = clampI(innerPadding, 0, 16);

		backgroundOpacity = clampD(backgroundOpacity, 0.0, 1.0);

		backgroundColor &= 0xFFFFFF;
		borderColor &= 0xFFFFFF;
		nameColor &= 0xFFFFFF;
		speakingColor &= 0xFFFFFF;
		whisperColor &= 0xFFFFFF;
	}

	// --- Section resets (used by the config screen) ---
	public void resetGeneral() {
		VisualizerConfig d = new VisualizerConfig();
		enabled = d.enabled; showSelf = d.showSelf; whisperDistinct = d.whisperDistinct;
		renderOnlyInGame = d.renderOnlyInGame; respectHudHidden = d.respectHudHidden;
		hideWhenChatOpen = d.hideWhenChatOpen; sortMode = d.sortMode; maxEntries = d.maxEntries;
		speakingTimeoutMs = d.speakingTimeoutMs; holdMs = d.holdMs; fadeInMs = d.fadeInMs;
		fadeOutMs = d.fadeOutMs; slideInAnimation = d.slideInAnimation; levelSensitivity = d.levelSensitivity;
		whisperDimFactor = d.whisperDimFactor;
	}

	public void resetPosition() {
		VisualizerConfig d = new VisualizerConfig();
		x = d.x; y = d.y; alignment = d.alignment; stackDirection = d.stackDirection; entrySpacing = d.entrySpacing;
	}

	public void resetAppearance() {
		VisualizerConfig d = new VisualizerConfig();
		iconSize = d.iconSize; showFace = d.showFace; showName = d.showName; nameScale = d.nameScale;
		maxNameWidth = d.maxNameWidth; showSpeakingIcon = d.showSpeakingIcon; indicatorStyle = d.indicatorStyle;
		showBackground = d.showBackground; showBorder = d.showBorder; cornerRadius = d.cornerRadius;
		innerPadding = d.innerPadding; distanceDisplay = d.distanceDisplay;
	}

	public void resetColors() {
		VisualizerConfig d = new VisualizerConfig();
		backgroundColor = d.backgroundColor; backgroundOpacity = d.backgroundOpacity; borderColor = d.borderColor;
		nameColor = d.nameColor; speakingColor = d.speakingColor; whisperColor = d.whisperColor;
	}

	private static int clampI(int v, int min, int max) {
		return v < min ? min : Math.min(v, max);
	}

	private static double clampD(double v, double min, double max) {
		return v < min ? min : Math.min(v, max);
	}
}
