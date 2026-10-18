package dev.hephaestus.glowcase.client.util;

import com.mojang.serialization.DataResult;
import dev.hephaestus.glowcase.util.Components;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Pattern;

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

	/**
	 * Intentionally copy the internal map.
	 * <p>
	 * Provides a space/underscore removed lowercase map that allows looking up colors by name.
	 * <p>
	 * Includes transparency.
	 */
	private static final Map<String, Container> NAMED_COLORS = Map.ofEntries(
		Map.entry("black", Container.tri(0x000000)),
		Map.entry("darkblue", Container.tri(0x0000aa)),
		Map.entry("darkgreen", Container.tri(0x00aa00)),
		Map.entry("darkaqua", Container.tri(0x00aaaa)),
		Map.entry("darkred", Container.tri(0xaa0000)),
		Map.entry("darkpurple", Container.tri(0xaa00aa)),
		Map.entry("gold", Container.tri(0xffaa00)),
		Map.entry("gray", Container.tri(0xaaaaaa)),
		Map.entry("darkgray", Container.tri(0x555555)),
		Map.entry("blue", Container.tri(0x5555ff)),
		Map.entry("green", Container.tri(0x55ff55)),
		Map.entry("aqua", Container.tri(0x55ffff)),
		Map.entry("red", Container.tri(0xff5555)),
		Map.entry("lightpurple", Container.tri(0xff55ff)),
		Map.entry("yellow", Container.tri(0xffff55)),
		Map.entry("white", Container.tri(0xffffff)),
		Map.entry("transparent", Container.quad(0x00000000))
	);
	private static final Pattern SPACE_REMOVER = Pattern.compile("[\\s_]");

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
	@Deprecated(forRemoval = true)
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

	/**
	 * Parses the name of colours into their respective value.
	 * <p>
	 * Always yields ARGB format.
	 *
	 * @implNote TextColor's parser tries to parse #\d{1,6}, which is, inappropriate.
	 */
	private static Container parseName(String string) {
		return NAMED_COLORS.get(SPACE_REMOVER.matcher(string.toLowerCase(Locale.ROOT)).replaceAll(""));
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
	 * </ul>
	 *
	 * @param string The value to parse.
	 * @return The parsed integer result, if any.
	 */
	private static Container parseHex(String string) {
		if (!string.startsWith("#")) {
			return null;
		}

		try {
			final int color = Integer.parseUnsignedInt(string, 1, string.length(), 16);

			return switch (string.length()) {
				case 4 -> new Container(upcast(color), 3);
				case 5 -> new Container(upcast(color), 4);
				case 7 -> new Container(color, 3);
				case 9 -> new Container(color, 4);
				default -> null;
			};
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	public static String toAlphaHex(int color) {
		return String.format(Locale.ROOT, "#%1$08X", color);
	}

	public static String toHex(int color) {
		return String.format(Locale.ROOT, "#%1$06X", color & RGB_MASK);
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
	 *
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
	 * @param HSV   The HSV float array to apply the gathered HSV values if desired.
	 * @apiNote The {@link ARGB#setBrightness(int, float)} method was used as reference for getting the hue and saturation, while the Wikipedia source was used as reference for getting the value ("With maximum component (i.e. value)").
	 * @see ColorUtil#ARGBToHSVA(int)
	 * @see ARGB#setBrightness(int, float)
	 * @see <a href="https://en.wikipedia.org/wiki/HSL_and_HSV#From_RGB">Wikipedia source for RGB to HSV Formula</a>
	 */
	public static float[] RGBtoHSV(int color, float @Nullable [] HSV) {
		if (HSV == null) {
			HSV = new float[3];
		}

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

			if (red == rgbMax) {
				hue = constantBlue - constantGreen;
			} else if (green == rgbMax) {
				hue = 2f + constantRed - constantBlue;
			} else {
				hue = 4f + constantGreen - constantRed;
			}

			hue /= 6f;
			if (hue < 0f) {
				hue++;
			}
		}

		float value = rgbMax / 255f;

		HSV[0] = hue;
		HSV[1] = saturation;
		HSV[2] = value;
		return HSV;
	}

	/**
	 * Convert hue, saturation, value, and alpha to an integer of ARGB.
	 *
	 * @apiNote If hue == 1f, it is passed as 0f because of a bug with Vanilla's hsv to rgb conversion.
	 * @see ColorUtil#HSVtoRGB(float, float, float)
	 */
	public static int HSVAtoARGB(float hue, float saturation, float value, float alpha) {
		if (hue == 1f) {
			hue = 0f;
		}
		return Mth.hsvToArgb(hue, saturation, value, ARGB.as8BitChannel(alpha));
	}

	/**
	 * Convert hue, saturation, and value to an integer of RGB.
	 *
	 * @apiNote If hue == 1f, it is passed as 0f because of a bug with Vanilla's hsv to rgb conversion.
	 * @see ColorUtil#HSVAtoARGB(float, float, float, float)
	 */
	public static int HSVtoRGB(float hue, float saturation, float value) {
		if (hue == 1f) {
			hue = 0f;
		}
		return ARGB.opaque(Mth.hsvToRgb(hue, saturation, value));
	}

	/**
	 * Gets the active presentation for the environment.
	 * <p>
	 * In the future, this could be driven by a config, but because Glowcase has no existing config,
	 * the complexity of writing a config system is currently outweighed by the lack of demand and UX.
	 *
	 * @param withAlpha Whether it is the alpha version.
	 * @implNote In the future: alpha presentations should be used to get the reference non-alpha version.
	 * 	Non-alpha should never be presented in the config, as this can lead to odd behaviour.
	 */
	public static Presentation getPresentation(boolean withAlpha) {
		return withAlpha ? Presentation.RGBA : Presentation.RGB;
	}

	/**
	 * Handles transforming parsed and raw values for use with front-ends.
	 */
	public enum Presentation {
		RGB(Components.Color.rgbTooltip, Components.Color.doubleRgb) {
			@Override
			public int toArgb(final int value) {
				return value | ALPHA_MASK;
			}

			@Override
			public int fromArgb(final int value) {
				return value & RGB_MASK;
			}

			@Override
			public String present(final int argb) {
				return toHex(argb);
			}
		},
		ARGB(Components.Color.argbTooltip, Components.Color.doubleArgb) {
			@Override
			public int toArgb(final int value) {
				return value;
			}

			@Override
			public int fromArgb(final int value) {
				return value;
			}

			@Override
			public String present(final int argb) {
				return toAlphaHex(argb);
			}
		},
		RGBA(Components.Color.rgbaTooltip, Components.Color.doubleRgba) {
			@Override
			public int toArgb(final int value) {
				return Integer.rotateRight(value, CHANNEL_BITS);
			}

			@Override
			public int fromArgb(final int value) {
				return Integer.rotateLeft(value, CHANNEL_BITS);
			}

			@Override
			public String present(final int argb) {
				return toAlphaHex(fromArgb(argb));
			}
		},
		;

		public final Component component, hint;
		public final Tooltip tooltip;

		Presentation(Component component, Component hint) {
			this.component = component;
			this.hint = hint;
			this.tooltip = Tooltip.create(component);
		}

		public boolean hasAlpha() {
			return this != RGB;
		}

		public boolean hasTrailingAlpha() {
			return this == RGBA;
		}

		public abstract int toArgb(int value);

		public abstract int fromArgb(int value);

		public abstract String present(int argb);

		/**
		 * Parses the given input string.
		 *
		 * @param value The string to parse.
		 * @return The result in the ARGB format.
		 */
		public OptionalInt parse(final String value) {
			final Container container = parseHex(value);

			if (container == null) {
				final Container fallback = parseName(value);

				if (fallback == null) {
					return OptionalInt.empty();
				}

				return OptionalInt.of(fallback.value(false));
			}

			return OptionalInt.of(toArgb(container.value(hasTrailingAlpha())));
		}

		/**
		 * Parses the given input string.
		 *
		 * @param value         The string to parse.
		 * @param originalColor The original color to copy the alpha from.
		 * @return The result in the ARGB format.
		 */
		public OptionalInt parse(final String value, final int originalColor) {
			final Container container = parseHex(value);

			if (container == null) {
				final Container fallback = parseName(value);

				if (fallback == null) {
					return OptionalInt.empty();
				}

				return OptionalInt.of(fallback.value(originalColor));
			}

			int argb = toArgb(container.value(hasTrailingAlpha()));

			if (container.channels == 3) {
				argb = transferAlpha(originalColor, argb);
			}

			return OptionalInt.of(argb);
		}
	}

	private record Container(int value, int channels) {
		public static Container tri(final int value) {
			return new Container(value, 3);
		}

		public static Container quad(final int value) {
			return new Container(value, 4);
		}

		int value(boolean requireTrailingAlpha) {
			return switch (channels) {
				case 3 -> requireTrailingAlpha ? value << 8 | CHANNEL_MASK : value | ALPHA_MASK;
				case 4 -> value;
				default -> throw new IllegalStateException("invalid channel count: " + channels);
			};
		}

		// Safe for ARGB only.
		int value(int original) {
			return switch (channels) {
				case 3 -> transferAlpha(original, value);
				case 4 -> value;
				default -> throw new IllegalStateException("invalid channel count: " + channels);
			};
		}
	}
}
