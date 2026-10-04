package dev.hephaestus.glowcase.util;

public final class MathUtils {

	public static boolean equals(final double a, final double b, final double epsilon) {
		return Math.abs(b - a) < epsilon;
	}

	public static boolean equals(final float a, final float b, final float epsilon) {
		return Math.abs(b - a) < epsilon;
	}

	public static int clampWrap(int value, int min, int max) {
		if (value < min) {
			return max + value - min + 1;
		} else if (value > max) {
			return min + value - max - 1;
		}

		return value;
	}
}
