package dev.hephaestus.glowcase.util;

public class ParseUtil {
	public static boolean canParseInt(String string) {
		if (string.isEmpty() || string.equals("-")) {
            return true;
        }

		try {
			Integer.valueOf(string);
		} catch (NumberFormatException e) {
			return false;
		}

		return true;
	}

	public static int parseOrDefault(String string, int value) {
		if (string.isEmpty() || string.equals("-") || !canParseInt(string)) {
			return value;
		}
		
		return Integer.parseInt(string);
	}

	public static boolean canParseDouble(String string) {
		if (string.isEmpty() || string.equals("-")) {
            return true;
        }

		try {
			Double.valueOf(string);
		} catch (NumberFormatException e) {
			return false;
		}

		return true;
	}

	public static double parseOrDefault(String string, double value) {
		if (string.isEmpty() || string.equals("-") || !canParseDouble(string)) {
			return value;
		}

		return Double.parseDouble(string);
	}
}
