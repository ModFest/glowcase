package dev.hephaestus.glowcase.client.util;

import com.mojang.serialization.DataResult;
import net.minecraft.ChatFormatting;

/**
 * @author Ampflower
 **/
public final class ColorUtil {
	public static final int CHANNEL_BITS = 8;
	public static final int RGB_CHANNELS = 3;
	public static final int CHANNEL_LENGTH = 1 << CHANNEL_BITS;
	public static final int CHANNEL_MASK = CHANNEL_LENGTH - 1;

	public static final int RGB_BITS = CHANNEL_BITS * RGB_CHANNELS;

	public static final int RGB_MASK = (1 << RGB_BITS) - 1;
	public static final int ALPHA_MASK = CHANNEL_MASK << RGB_BITS;
	public static final int WHITE = ALPHA_MASK | RGB_MASK;
	public static final int TRANSPARENT = 0;

	public static int transferAlpha(int oldColor, int newColor) {
		return (oldColor & ALPHA_MASK) | (newColor & RGB_MASK);
	}

	public static int alphaFallback(int color) {
		if ((color & ALPHA_MASK) == TRANSPARENT) {
			return color & RGB_MASK | ALPHA_MASK;
		}

		return color;
	}

	public static int minAlpha(int color, int minAlpha) {
		if ((color >>> RGB_BITS) < minAlpha) {
			return (color & RGB_MASK) | (minAlpha << RGB_CHANNELS);
		}

		return color;
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

			int rgb = formatting.getColor() & RGB_MASK;
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
