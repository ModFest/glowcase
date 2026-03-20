package dev.hephaestus.glowcase.client.util;

import com.mojang.serialization.DataResult;
import net.minecraft.ChatFormatting;

/**
 * @author Ampflower
 **/
public final class ColorUtil {
	public static final int WHITE = 0xFFFFFFFF;
	public static final int TRANSPARENT = 0x00000000;
	public static final int ALPHA_MASK = 0xFF000000;
	public static final int COLOR_MASK = 0x00FFFFFF;

	public static int transferAlpha(int oldColor, int newColor) {
		return (oldColor & ALPHA_MASK) | (newColor & COLOR_MASK);
	}

	public static DataResult<Integer> parse(String string, int reference) {
		if (string.startsWith("#")) {
			try {
				final int color = Integer.parseUnsignedInt(string, 1, string.length(), 16);

				return switch (string.length()) {
					case 4 -> {
						int rgb = upcast(color);
						int a = reference & ALPHA_MASK;

						yield DataResult.success(a | rgb);
					}
					case 5 -> DataResult.success(upcast(color));
					case 7 -> {
						int a = reference & ALPHA_MASK;

						yield DataResult.success(a | color);
					}
					case 9 -> DataResult.success(color);
					default -> DataResult.error(() -> "Unexpected value: " + string);
				};
			} catch (NumberFormatException ignored) {
				return DataResult.error(() -> "Not a number: " + string);
			}
		} else {
			final ChatFormatting formatting = ChatFormatting.getByName(string);
			if (formatting == null || !formatting.isColor()) {
				return DataResult.error(() -> "Unknown color: " + string);
			}

			int rgb = formatting.getColor() & COLOR_MASK;
			int a = reference & ALPHA_MASK;

			return DataResult.success(a | rgb);
		}
	}

	public static String toAlphaHex(int color) {
		return String.format("#%1$08X", color);
	}

	private static int upcast(int color) {
		int r = (color & 0x000F) * 0x00000011;
		int g = (color & 0x00F0) * 0x00000110;
		int b = (color & 0x0F00) * 0x00001100;
		int a = (color & 0xF000) * 0x00011000;
		return r | g | b | a;
	}
}
