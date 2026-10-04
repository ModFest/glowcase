package dev.hephaestus.glowcase.client.util;

import com.mojang.serialization.DataResult;
import net.minecraft.ChatFormatting;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

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
	 * Convert an integer color to hue, saturation, value, and alpha.
	 * @see ColorUtil#RGBtoHSV(int, float[])
	 */
	public static float[] ARGBToHSVA(int color) {
		float[] HSVA = new float[4];
		RGBtoHSV(color, HSVA);
		HSVA[3] = ARGB.alphaFloat(color);
		return HSVA;
	}

	/**
	 * Convert an integer color to hue, saturation, and value.
	 *
	 * @param color The integer color to get the RGB from.
	 * @param HSV The HSV float array to apply the gathered HSV values if desired.
	 *
	 * @apiNote The {@link ARGB#setBrightness(int, float)} method was used as reference for getting the hue and saturation, while the Wikipedia source was used as reference for getting the value ("With maximum component (i.e. value)").
	 *
	 * @see ColorUtil#ARGBToHSVA(int)
	 * @see ARGB#setBrightness(int, float)
	 * @see <a href="https://en.wikipedia.org/wiki/HSL_and_HSV#From_RGB">Wikipedia source for RGB to HSV Formula</a>
	 */
	public static float[] RGBtoHSV(int color, float @Nullable [] HSV) {
		if (HSV == null) HSV = new float[3];

		int red = ARGB.red(color);
		int green = ARGB.green(color);
		int blue = ARGB.blue(color);

		int rgbMax = Math.max(Math.max(red, green), blue);
		int rgbMin = Math.min(Math.min(red, green), blue);
		float rgbConstantRange = rgbMax - rgbMin;

		float saturation;
		if (rgbMax != 0) {
			saturation = rgbConstantRange / rgbMax;
		} else {
			saturation = 0f;
		}

		float hue;
		if (saturation == 0f) {
			hue = 0f;
		} else {
			float constantRed = (rgbMax - red) / rgbConstantRange;
			float constantGreen = (rgbMax - green) / rgbConstantRange;
			float constantBlue = (rgbMax - blue) / rgbConstantRange;

			if (red == rgbMax) hue = constantBlue - constantGreen;
			else if (green == rgbMax) hue = 2f + constantRed - constantBlue;
			else hue = 4f + constantGreen - constantRed;

			hue /= 6f;
			if (hue < 0f) hue++;
		}

		float value = rgbMax / 255f;

		HSV[0] = hue;
		HSV[1] = saturation;
		HSV[2] = value;
		return HSV;
	}

	/**
	 * Convert hue, saturation, value, and alpha to an integer of ARGB.
	 * @apiNote If hue == 1f, it is passed as 0f because of a bug with Vanilla's hsv to rgb conversion.
	 * @see ColorUtil#HSVtoRGB(float, float, float)
	 */
	public static int HSVAtoARGB(float hue, float saturation, float value, float alpha) {
		if (hue == 1f) hue = 0f;
		return Mth.hsvToArgb(hue, saturation, value, Mth.floor(alpha * 255f));
	}

	/**
	 * Convert hue, saturation, and value to an integer of RGB.
	 * @apiNote If hue == 1f, it is passed as 0f because of a bug with Vanilla's hsv to rgb conversion.
	 * @see ColorUtil#HSVAtoARGB(float, float, float, float)
	 */
	public static int HSVtoRGB(float hue, float saturation, float value) {
		if (hue == 1f) hue = 0f;
		return ARGB.opaque(Mth.hsvToRgb(hue, saturation, value));
	}
}
