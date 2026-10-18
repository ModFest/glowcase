package dev.hephaestus.glowcase.client.util;

import dev.hephaestus.glowcase.util.Components;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * @author Ampflower
 **/
public final class Tooltips {
	public static final List<Tooltip> AXES = of(Components.AXES);

	public static final List<Tooltip> SCALE_AXES = of(Components.SCALE_AXES);

	public static final List<Tooltip> OFFSET_AXES = of(Components.OFFSET_AXES);

	public static final List<Tooltip> ROTATIONS = of(Components.ROTATIONS);

	public static List<Tooltip> of(final List<Component> components) {
		if (components.isEmpty()) {
			// Short circuit
			return List.of();
		}

		final var array = new Tooltip[components.size()];

		for (int i = 0; i < components.size(); i++) {
			array[i] = Tooltip.create(components.get(i));
		}

		return List.of(array);
	}
}
