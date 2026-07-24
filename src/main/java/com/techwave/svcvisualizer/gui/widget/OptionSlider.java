package com.techwave.svcvisualizer.gui.widget;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** A labelled slider mapping a config value in {@code [min, max]} to a getter/setter. */
public class OptionSlider extends AbstractSliderButton {

	private final String labelKey;
	private final double min;
	private final double max;
	private final boolean integer;
	private final DoubleConsumer setter;

	public OptionSlider(int x, int y, int width, int height, String labelKey,
	                    double min, double max, boolean integer,
	                    DoubleSupplier getter, DoubleConsumer setter) {
		super(x, y, width, height, Component.empty(), toSlider(getter.getAsDouble(), min, max));
		this.labelKey = labelKey;
		this.min = min;
		this.max = max;
		this.integer = integer;
		this.setter = setter;
		updateMessage();
	}

	private static double toSlider(double actual, double min, double max) {
		if (max <= min) {
			return 0.0;
		}
		return Math.max(0.0, Math.min(1.0, (actual - min) / (max - min)));
	}

	private double actual() {
		double v = min + value * (max - min);
		return integer ? Math.round(v) : v;
	}

	/** Set the slider from an actual value (used when another control changes the same field). */
	public void setActual(double actual) {
		this.value = toSlider(actual, min, max);
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		double v = actual();
		String shown = integer ? String.valueOf((long) v) : String.format(java.util.Locale.ROOT, "%.2f", v);
		setMessage(Component.translatable(labelKey).append(Component.literal(": " + shown)));
	}

	@Override
	protected void applyValue() {
		setter.accept(actual());
	}
}
