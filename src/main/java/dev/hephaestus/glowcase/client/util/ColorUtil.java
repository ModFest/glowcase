package dev.hephaestus.glowcase.client.util;

import com.mojang.serialization.DataResult;
import net.minecraft.ChatFormatting;
import net.minecraft.util.ARGB;

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

	public static final int RED = 0xFFFF0000;
	public static final int YELLOW = 0xFFFFFF00;
	public static final int GREEN = 0xFF00FF00;
	public static final int CYAN = 0xFF00FFFF;
	public static final int BLUE = 0xFF0000FF;
	public static final int MAGENTA = 0xFFFF00FF;

	public static final int WHITE = ALPHA_MASK | RGB_MASK;
	public static final int BLACK = 0xFF000000;
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

	public static String toHex(int color) {
		return "#" + String.format("%1$06X", color & 0x00FFFFFF);
	}

	private static int upcast(int color) {
		int r = (color & 0x000F) * 0x00000011;
		int g = (color & 0x00F0) * 0x00000110;
		int b = (color & 0x0F00) * 0x00001100;
		int a = (color & 0xF000) * 0x00011000;
		return r | g | b | a;
	}

	/**
	 * Convert an integer color to hue, saturation, brightness/value, and alpha.
	 */
	public static float[] ARGBToHSBA(int color) {
		int red = ARGB.red(color);
		int green = ARGB.green(color);
		int blue = ARGB.blue(color);
		float[] HSBA = new float[4];
		RGBtoHSB(red, green, blue, HSBA);

		HSBA[3] = ARGB.alphaFloat(color);
		return HSBA;
	}

	/**
	 * Converts RGB to HSB (hue, saturation, brightness/value).<br><br>
	 *
	 * Custom method here used in favor of java.awt.Color's to prevent possible crashes on Mac.
	 * @see java.awt.Color#RGBtoHSB(int, int, int, float[])
	 */
	public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
		float hue, saturation, brightness;
		if (hsbvals == null) {
			hsbvals = new float[3];
		}
		int cmax = (r > g) ? r : g;
		if (b > cmax) cmax = b;
		int cmin = (r < g) ? r : g;
		if (b < cmin) cmin = b;

		brightness = ((float) cmax) / 255.0f;
		if (cmax != 0)
			saturation = ((float) (cmax - cmin)) / ((float) cmax);
		else
			saturation = 0;
		if (saturation == 0)
			hue = 0;
		else {
			float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
			float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
			float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
			if (r == cmax)
				hue = bluec - greenc;
			else if (g == cmax)
				hue = 2.0f + redc - bluec;
			else
				hue = 4.0f + greenc - redc;
			hue = hue / 6.0f;
			if (hue < 0)
				hue = hue + 1.0f;
		}
		hsbvals[0] = hue;
		hsbvals[1] = saturation;
		hsbvals[2] = brightness;
		return hsbvals;
	}

	/**
	 * Convert hue, saturation, brightness/value, and alpha to an integer of ARGB.
	 */
	public static int HSBAtoARGB(float hue, float saturation, float brightness, float alpha) {
		return ARGB.color(alpha, HSBtoRGB(hue, saturation,brightness));
	}

	/**
	 * Converts HSB (hue, saturation, brightness/value) to RGB.<br><br>
	 *
	 * Custom method here used in favor of java.awt.Color's to prevent possible crashes on Mac.
	 * @see java.awt.Color#HSBtoRGB(float, float, float)
	 */
	public static int HSBtoRGB(float hue, float saturation, float brightness) {
		int r = 0, g = 0, b = 0;
		if (saturation == 0) {
			r = g = b = (int) (brightness * 255.0f + 0.5f);
		} else {
			float h = (hue - (float)Math.floor(hue)) * 6.0f;
			float f = h - (float)java.lang.Math.floor(h);
			float p = brightness * (1.0f - saturation);
			float q = brightness * (1.0f - saturation * f);
			float t = brightness * (1.0f - (saturation * (1.0f - f)));
			switch ((int) h) {
				case 0:
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (t * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				case 1:
					r = (int) (q * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				case 2:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (t * 255.0f + 0.5f);
					break;
				case 3:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (q * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				case 4:
					r = (int) (t * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				case 5:
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (q * 255.0f + 0.5f);
					break;
			}
		}
		return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
	}
}
