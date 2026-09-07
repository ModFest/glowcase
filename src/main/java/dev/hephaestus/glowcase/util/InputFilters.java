package dev.hephaestus.glowcase.util;

public class InputFilters {
	/// Asserts that the given character is only input as a prefix, and prevents other inputs from breaking this rule. \
	/// This only allows one instance of the character, and does not require it to be present.
	///
	/// @param prefix       The character to check for
	/// @param currentValue The current text from the input source
	/// @param newChar      The new character to check if is allowed
	/// @param cursorPos    The current position of the text cursor
	/// @return If the new character is allowed
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean assertOptionalPrefix(char prefix, String currentValue, final int newChar, final int cursorPos, final int highlightedPos) {
		// If the position isn't 0 and not all text is selected, it is allowed as long as it isn't the prefix character
		boolean allTextHighlighted = (currentValue.length() == cursorPos && highlightedPos == 0) || (currentValue.length() == highlightedPos && cursorPos == 0);
		if (cursorPos != 0 && !allTextHighlighted) return newChar != prefix;

		// If we have the prefix and not all text is selected, no character is allowed at position 0
		// If all text is selected, we allow return true because the newChar will replace any and all existing characters
		return currentValue.indexOf(prefix) == -1 || allTextHighlighted;
	}

	/// An input filter for checking if the input is valid for building a natural number. \
	/// This does not verify if it is within bounds or if it is fully parseable!
	///
	/// @param currentValue The current text for the number being built
	/// @param newChar      The new character to check if is allowed
	/// @param cursorPos    The current position of the text cursor
	/// @return If the new character is valid for building a natural number
	public static boolean naturalNumber(String currentValue, final int newChar, final int cursorPos, final int highlightedPos) {
		return newChar >= '0' && newChar <= '9';
	}

	/// An input filter for checking if the input is valid for building an integer number. \
	/// This does not verify if it is within bounds or if it is fully parseable!
	///
	/// @param currentValue The current text for the number being built
	/// @param newChar      The new character to check if is allowed
	/// @param cursorPos    The current position of the text cursor
	/// @return If the new character is valid for building an integer number
	// This does technically allow -0, but it's parseable so why bother
	public static boolean integerNumber(String currentValue, final int newChar, final int cursorPos, final int highlightedPos) {
		// Allow - at the start of the text
		boolean allTextHighlighted = (currentValue.length() == cursorPos && highlightedPos == 0) || (currentValue.length() == highlightedPos && cursorPos == 0);
		if (!assertOptionalPrefix('-', currentValue, newChar, cursorPos, highlightedPos)) return false;
		if (newChar == '-' && (cursorPos == 0 || allTextHighlighted)) return true;

		return naturalNumber(currentValue, newChar, cursorPos, highlightedPos);
	}

	/// An input filter for checking if the input is valid for building a real number. \
	/// This does not verify if it is within bounds or if it is fully parseable!
	///
	/// @param currentValue The current text for the number being built
	/// @param newChar      The new character to check if is allowed
	/// @param cursorPos    The current position of the text cursor
	/// @return If the new character is valid for building a real number
	public static boolean realNumber(String currentValue, final int newChar, final int cursorPos, final int highlightedPos) {
		// Only allow up to 1 . in the value
		if (newChar == '.' && currentValue.indexOf('.') != -1) return false;

		return integerNumber(currentValue, newChar, cursorPos, highlightedPos) || newChar == '.';
	}
}
