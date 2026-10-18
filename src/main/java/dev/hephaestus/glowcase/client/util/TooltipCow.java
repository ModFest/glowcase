package dev.hephaestus.glowcase.client.util;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * @author Ampflower
 **/
public interface TooltipCow {
	Tooltip glowcase$prepend(Component component);

	Tooltip glowcase$prependLine(Component component);

	Tooltip glowcase$append(Component component);

	Tooltip glowcase$appendLine(Component component);
}
