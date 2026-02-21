package dev.hephaestus.glowcase.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class TextUtils {
	public static final Style PLACEHOLDER_STYLE = Style.EMPTY.withItalic(true).withColor(ChatFormatting.GRAY);

	public static Component placeholder(String key) {
		return Component.translatable(key).setStyle(PLACEHOLDER_STYLE);
	}
}
