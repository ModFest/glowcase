package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.client.util.ColorUtil;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import eu.pb4.placeholders.api.parsers.tag.TextTag;
import net.minecraft.ChatFormatting;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Main interface for any Screen wishing for niceties related to QuickText tag formatting.<br><br>
 * Includes some default methods for various tag insertions, and also prevents the narrator from being enabled when pressing "ctrl+b"
 * @see TextEditorScreen
 * @see dev.hephaestus.glowcase.client.gui.widget.ingame.text.GlowcaseMultilineEditBox#insertTag(String)
 * @see dev.hephaestus.glowcase.mixin.client.KeyboardHandlerMixin
 * @author Superkat32
 */
public interface TagFormatIncludedScreen {

	void insertTag(String tagName);

	default void insertTextTag(TextTag tag) {
		this.insertTextTag(tag, true);
	}

	default void insertTextTag(TextTag tag, boolean findShortestAlias) {
		if (tag == null) return;

		String tagName = tag.name();
		if (findShortestAlias && tag.aliases().length > 1) { // Find an alias with the least amount of characters
			tagName = Arrays.stream(tag.aliases()).min(Comparator.comparing(String::length)).get();
		}
		this.insertTag(tagName);
	}

	default void insertFormattingTag(ChatFormatting formatting) {
		this.insertTextTag(TagRegistry.SAFE.getTag(formatting.getName()), false);
	}

	default void insertColorHexTag(int color) {
		String hex = ColorUtil.toHex(color);
		this.insertTag(hex);
	}

	default void insertBoldTag() {
		this.insertTextTag(TagRegistry.SAFE.getTag("bold"));
	}

	default void insertItalicTag() {
		this.insertTextTag(TagRegistry.SAFE.getTag("italic"));
	}

	default void insertStrikethroughTag() {
		this.insertTextTag(TagRegistry.SAFE.getTag("strikethrough"));
	}

	default void insertUnderlineTag() {
		this.insertTextTag(TagRegistry.SAFE.getTag("underline"));
	}

	default void insertObfuscatedTag() {
		this.insertTextTag(TagRegistry.SAFE.getTag("obfuscated"));
	}
}
