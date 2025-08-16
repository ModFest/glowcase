package dev.hephaestus.glowcase.util;

public final class MathUtils {
	public static int clampWrap(int value, int min, int max) {
		if (value < min) {
			return max + value - min + 1;
		} else if (value > max) {
			return min + value - max - 1;
		}

		return value;
	}
}
