package dev.hephaestus.glowcase.client.util;

import com.mojang.serialization.DataResult;
import net.minecraft.network.chat.TextColor;

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
		var result = TextColor.parseColor(string);

		return result.map(textColor -> {
			int rgb = textColor.getValue() & RGB_MASK;
			int a = reference & ALPHA_MASK;
			return a | rgb;
		});
	}

	public static String toAlphaHex(int color) {
		return String.format("#%1$08X", color);
	}
}
