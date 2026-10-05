package dev.hephaestus.glowcase.client.util;

import com.mojang.serialization.DataResult;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Range;
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
			return color | ALPHA_MASK;
		}

		return color;
	}

	public static int alphaFallback(int color, @Range(from = 0, to = 255) int alpha) {
		if ((color & ALPHA_MASK) == TRANSPARENT) {
			return color | (alpha << RGB_BITS);
		}

		return color;
	}

	public static int alphaFallback(int color, float alpha) {
		return alphaFallback(color, ARGB.as8BitChannel(alpha));
	}

	public static int minAlpha(int color, int minAlpha) {
		if ((color >>> RGB_BITS) < minAlpha) {
			return transferAlpha(minAlpha << RGB_BITS, color);
		}

		return color;
	}

	public static int withAlpha(int color, @Range(from = 0, to = 255) int alpha) {
		return transferAlpha(alpha << RGB_BITS, color);
	}

	public static int withAlpha(int color, float alpha) {
		return withAlpha(color, ARGB.as8BitChannel(alpha));
	}

	/**
	 * Parses alpha and non-alpha colour values.
	 * <p>
	 * Accepts all the following formats:
	 * <ul>
	 *     <li>{@code #321} - RGB, alpha carried from {@code reference}. Is equivalent to {@code #332211}.</li>
	 *     <li>{@code #332211} - RGB, alpha carried from {@code reference}.</li>
	 *     <li>{@code #4321} - ARGB. {@code reference} is ignored. Is equivalent to {@code #44332211}.</li>
	 *     <li>{@code #44332211} - ARGB. {@code reference} is ignored.</li>
	 *     <li>Named values valid to {@link TextColor#parseColor(String)}.</li>
	 * </ul>
	 *
	 * @param string    The value to parse.
	 * @param reference The original colour to use as a reference.
	 * @return The parsed integer result, if any.
	 */
	public static DataResult<Integer> parse(String string, int reference) {
		// MAINTENANCE NOTE: There is no vanilla equivalent to the hex parser.
		// This accepts #RGB, #ARGB, #RRGGBB and #AARRGGBB along with parsing the name of colours.
		if (string.startsWith("#")) {
			try {
				final int color = Integer.parseUnsignedInt(string, 1, string.length(), 16);

				return switch (string.length()) {
					case 4 -> DataResult.success(transferAlpha(reference, upcast(color)));
					case 5 -> DataResult.success(upcast(color));
					case 7 -> DataResult.success(transferAlpha(reference, color));
					case 9 -> DataResult.success(color);
					default -> DataResult.error(() -> "Unexpected value: " + string);
				};
			} catch (NumberFormatException ignored) {
				return DataResult.error(() -> "Not a number: " + string);
			}
		} else {
			var result = TextColor.parseColor(string);

			return result.map(textColor -> {
				int rgb = textColor.getValue() & RGB_MASK;
				int a = reference & ALPHA_MASK;
				return a | rgb;
			});
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
		return Mth.hsvToArgb(hue, saturation, value, ARGB.as8BitChannel(alpha));
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
