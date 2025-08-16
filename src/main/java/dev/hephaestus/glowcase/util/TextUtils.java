package dev.hephaestus.glowcase.util;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TextUtils {
	public static final Style PLACEHOLDER_STYLE = Style.EMPTY.withItalic(true).withColor(Formatting.GRAY);

	public static Text placeholder(String key) {
		return Text.translatable(key).setStyle(PLACEHOLDER_STYLE);
	}
}
