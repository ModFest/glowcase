package dev.hephaestus.glowcase.client.gui.widget.ingame.text;

import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.TagParser;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import eu.pb4.placeholders.api.parsers.tag.TextTag;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * A MultilineTextField with QuickText, Text Alignment, and overflow text editing support.<br><br>
 *
 * This class handles the text logic behind the {@link GlowcaseMultilineEditBox} Widget.
 *
 * @apiNote The MultilineTextField stores its text in one String, while this class stores parsed Components in a List of lines. {@link FormattableMultilineTextField#parseLinesFromValue()} converts the String value into the List of Components, while {@link FormattableMultilineTextField#setValueFromParsedLines()} does the opposite.
 * @see GlowcaseMultilineEditBox
 * @author Superkat32
 */
public class FormattableMultilineTextField extends MultilineTextField {
	public static final NodeParser PARSER = TagParser.DEFAULT;
	private static final Component ELLIPSIS_SYMBOL = Component.literal("...");
	public final Font font;
	public final List<Component> parsedLines;
	public final Consumer<List<Component>> parsedUpdateListener;

	public int x;
	public int sideAlignmentPadding;
	public int lineHeight = 12;
	public boolean wordWrap = false;
	public boolean truncateText = false;
	public int cursorOverflowX = 0;
	public int selectCursorOverflowX = 0;
	public TextBlockEntity.TextAlignment textAlignment = TextBlockEntity.TextAlignment.CENTER;

	public FormattableMultilineTextField(Font font, List<Component> parsedLines, int x, int width, int sideAlignmentPadding, Consumer<List<Component>> parsedUpdateListener) {
		super(font, width);
		this.font = font;
		this.parsedLines = parsedLines;
		this.x = x;
		this.sideAlignmentPadding = sideAlignmentPadding;
		this.parsedUpdateListener = parsedUpdateListener;
		this.setValueFromParsedLines();
	}

	public void parseLinesFromValue() {
		if (this.parsedLines == null) return; // Can equal null due to super constructor calling this.setValue("")
		this.parsedLines.clear();

		int lineCount = 0;
		for (StringView lineView : this.iterateLines()) {
			String rawLine = this.value().substring(lineView.beginIndex(), lineView.endIndex());
			this.addParsedLineFromRaw(lineCount, rawLine);
			lineCount++;
		}
		this.parsedUpdateListener.accept(this.parsedLines);
	}

	public void setValueFromParsedLines() {
		StringBuilder valueBuilder = new StringBuilder();
		for (int i = 0; i < this.parsedLines.size(); i++) {
			valueBuilder.append(this.getRawLineFromParsed(i));
			if (i < parsedLines.size() - 1) valueBuilder.append("\n");
		}
		this.setValue(valueBuilder.toString());
	}

	@Override
	public void onValueChange() {
		super.onValueChange();
		this.parseLinesFromValue();
	}

	public void insertTag(String tagName) {
		if (tagName.isBlank()) return;

		String openTag = "<" + tagName + ">";
		String closeTag = "</" + tagName + ">";

		if (this.hasSelection()) {
			StringView selectedView = this.getSelected();
			String beforeSelected = this.value().substring(0, selectedView.beginIndex());
			String selected = this.value().substring(selectedView.beginIndex(), selectedView.endIndex());
			String afterSelected = this.value().substring(selectedView.endIndex());

			// End each line with a closed tag, and begin each line with an opened tag
			String newSelected = openTag + selected.replaceAll("\n", closeTag + "\n" + openTag) + closeTag;
			int addedLength = newSelected.length() - selected.length();
			int preInsertCursorPos = this.cursor();
			int preInsertSelectCursorPos = this.selectCursor;
			this.setValue(beforeSelected + newSelected + afterSelected);
			this.seekCursor(Whence.ABSOLUTE, preInsertCursorPos + addedLength - closeTag.length());
			this.selectCursor = preInsertSelectCursorPos + openTag.length();
		} else {
			int preInsertCursorPos = this.cursor();
			this.insertText(openTag + closeTag);
			this.seekCursor(Whence.ABSOLUTE, preInsertCursorPos + openTag.length());
		}
	}

	public void insertTextTag(TextTag tag) {
		this.insertTextTag(tag, true);
	}

	public void insertTextTag(TextTag tag, boolean findShortestAlias) {
		if (tag == null) return;

		String tagName = tag.name();
		if (findShortestAlias && tag.aliases().length > 1) { // Find an alias with the least amount of characters
			tagName = Arrays.stream(tag.aliases()).min(Comparator.comparing(String::length)).get();
		}
		this.insertTag(tagName);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// Check formatting hotkeys first, then key press textField if no hotkeys were pressed
		int keyCode = event.key();
		if (event.hasControlDown()) {
			if (keyCode == GLFW.GLFW_KEY_B) {
				this.insertTextTag(TagRegistry.SAFE.getTag("bold"));
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_I) {
				this.insertTextTag(TagRegistry.SAFE.getTag("italic"));
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_U) {
				this.insertTextTag(TagRegistry.SAFE.getTag("underline"));
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_5 || keyCode == GLFW.GLFW_KEY_S) {
				// There isn't a commonly agreed upon hotkey for strikethrough unlike the rest above...
				// Apparently 5 is commonly used for strikethrough ¯\_(ツ)_/¯
				// Google Docs and Microsoft Word have 5 in their hotkeys, while Discord has S in its hotkey
				this.insertTextTag(TagRegistry.SAFE.getTag("strikethrough"));
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_O) {
				this.insertTextTag(TagRegistry.SAFE.getTag("obfuscated"));
				return true;
			}
		}
		return super.keyPressed(event);
	}

	public void selectLineAtCursor() {
		StringView lineView = this.getLineView(this.getLineAtCursor());
		this.setSelecting(false);
		this.seekCursor(Whence.ABSOLUTE, lineView.beginIndex());
		this.setSelecting(true);
		this.seekCursor(Whence.ABSOLUTE, lineView.endIndex());
	}

	@Override
	public void seekCursor(@NonNull Whence whence, int cursor) {
		super.seekCursor(whence, cursor);

		int cursorLineIndex = this.getLineAtCursor();
		StringView cursorLineView = this.getLineView(cursorLineIndex);
		int cursorLineWidth = this.font.width(this.value().substring(cursorLineView.beginIndex(), cursorLineView.endIndex()));;

		if (cursorLineWidth >= this.width) {
			String beforeCursor = this.value().substring(cursorLineView.beginIndex(), this.cursor());
			int cursorX = this.getLineX(cursorLineIndex, cursorLineWidth) + this.font.width(beforeCursor);
			this.cursorOverflowX += this.getCursorOverflowOffset(cursorX, 32);
		} else {
			this.cursorOverflowX = 0;
		}
	}

	public void setCursorOverflowX(int cursorOverflowX) {
		StringView cursorLineView = this.getLineView(this.getLineAtCursor());
		String cursorLine = this.value().substring(cursorLineView.beginIndex(), cursorLineView.endIndex());
		int cursorLineWidth = this.font.width(cursorLine);
		// Very strange (& mildly brute-forced) on the caps but it works
		// FIXME - This could still be better, especially for note screen
		int safeZoneWidth = this.sideAlignmentPadding == 0 ? 32 : this.sideAlignmentPadding;
		boolean leftAligned = this.textAlignment == TextBlockEntity.TextAlignment.LEFT && this.sideAlignmentPadding > 0;
		int min = -cursorLineWidth + this.width
			- safeZoneWidth * (leftAligned ? 2 : 1);
		int max = leftAligned ? 0 : safeZoneWidth;
		this.cursorOverflowX = Mth.clamp(
			cursorOverflowX, min, max
		);
	}

	private int getCursorOverflowOffset(int cursorX, int safeZoneWidth) {
		int distLeft = cursorX - this.x;
		int distRight = this.x + this.width - cursorX;
		if (distLeft <= safeZoneWidth) {
			return safeZoneWidth - distLeft;
		} else if (distRight <= safeZoneWidth) {
			return -(safeZoneWidth - distRight);
		}
		return 0;
	}

	public boolean isCursorLine(int lineIndex) {
		return lineIndex == this.getLineAtCursor();
	}

	public boolean isSelectedLine(int lineIndex) {
		if (!this.hasSelection()) return false;

		StringView view = this.getLineView(lineIndex);
		StringView selectedView = this.getSelected();
		return (selectedView.endIndex() > view.beginIndex() && selectedView.beginIndex() < view.endIndex());
	}

	public boolean isEndOfSelectionLine(int lineIndex) {
		if (this.isCursorLine(lineIndex) || !this.isSelectedLine(lineIndex)) return false;

		StringView view = this.getLineView(lineIndex);
		StringView selectedView = this.getSelected();
		return (selectedView.beginIndex() > view.beginIndex() || selectedView.endIndex() < view.endIndex());
	}

	/**
	 * @return The Component to render for a given line index. Returns raw text if line is selected or has the cursor, otherwise returns the parsed text.
	 */
	public FormattedText getLineForRender(int lineIndex) {
		boolean isCursorLine = this.isCursorLine(lineIndex);
		boolean isSelectedLine = this.isSelectedLine(lineIndex);

		if (isCursorLine || isSelectedLine) return Component.literal(this.getRawLineFromParsed(lineIndex));
		Component parsedLine = this.parsedLines.get(lineIndex);
		if (this.truncateText && this.font.width(parsedLine) >= this.width) {
			int ellipsisWidth = this.font.width(ELLIPSIS_SYMBOL);
			int alignmentWidth = this.textAlignment == TextBlockEntity.TextAlignment.LEFT || this.textAlignment == TextBlockEntity.TextAlignment.RIGHT
				? this.sideAlignmentPadding : 0;

			return FormattedText.composite(
				this.font.substrByWidth(
				parsedLine, this.width - alignmentWidth - ellipsisWidth
			), ELLIPSIS_SYMBOL);
		}
		return parsedLine;
	}

	/**
	 * @return The beginning x placement of a line based on the text alignment, whether the line overflows, or is selected.
	 */
	public int getLineX(int lineIndex, int lineWidth) {
		int lineX = this.getAlignmentX(lineWidth);

		// Ensure beginning of text is at the edge if overflowing and not cursor or selected line
		if (lineWidth >= this.width) { // If overflow
			lineX = Math.max(this.x + 1, lineX);
			if (this.isCursorLine(lineIndex)) {
				lineX += this.cursorOverflowX;
			} else if (this.isEndOfSelectionLine(lineX)) {
				lineX += this.selectCursorOverflowX;
			}
		}

		return lineX;
	}

	public int getAlignmentX(int lineWidth) {
		int xOffset = switch(this.textAlignment) {
			case LEFT -> this.sideAlignmentPadding;
			case RIGHT -> this.width - this.sideAlignmentPadding - lineWidth;
			default -> this.width / 2 - lineWidth / 2;
		};
		return this.x + xOffset;
	}

	@Override
	public @NonNull StringView getNextWord() {
		if (this.value().isEmpty()) {
			return new StringView(0, 0);
		} else {
			int startPosition = Mth.clamp(this.cursor(), 0, this.value().length() - 1);
			boolean isTag = this.value().charAt(startPosition) == '<';

			while (startPosition < this.value().length() && !Character.isWhitespace(this.value().charAt(startPosition))) {
				char charAt = this.value().charAt(startPosition);
				if (!isTag && charAt == '<') break;
				startPosition++;
				if (charAt == '>') break;
				// If tag verification is wanted, SimpleTagRegistry.DEFAULT.getTag(String name) exists for name check
			}

			while (startPosition < this.value().length() && Character.isWhitespace(this.value().charAt(startPosition)) && !isTag) {
				startPosition++;
			}

			return new MultilineTextField.StringView(startPosition, startPosition); // End position isn't used from here
		}
	}

	@Override
	public @NonNull StringView getPreviousWord() {
		if (this.value().isEmpty()) {
			return new StringView(0, 0);
		} else {
			int startPosition = Mth.clamp(this.cursor(), 0, this.value().length() - 1);
			boolean isTag = this.value().charAt(Math.max(startPosition - 1, 0)) == '>';

			while (startPosition > 0 && Character.isWhitespace(this.value().charAt(startPosition - 1))) {
				startPosition--;
			}

			while (startPosition > 0 && !Character.isWhitespace(this.value().charAt(startPosition - 1))) {
				char charAt = this.value().charAt(startPosition - 1);
				if (!isTag && charAt == '>') break;
				startPosition--;
				if (charAt == '<') break;
			}

			return new MultilineTextField.StringView(startPosition, this.getWordEndPosition(startPosition));
		}
	}

	private int getWordEndPosition(final int from) {
		int end = from;
		boolean isTag = this.value().charAt(from) == '<';

		while (end < this.value().length() && !Character.isWhitespace(this.value().charAt(end))) {
			end++;
			if (isTag && this.value().charAt(end - 1) == '>') break;
			if (end < this.value().length() && this.value().charAt(end) == '<') break;
		}

		return end;
	}

	@Override
	public boolean overflowsLineLimit(@NonNull String newValue) {
		if (this.wordWrap) return super.overflowsLineLimit(newValue);

		int actualWidth = this.width;
		this.width = Integer.MAX_VALUE;
		boolean doesOverflow = super.overflowsLineLimit(newValue);
		this.width = actualWidth;
		return doesOverflow;
	}

	@Override
	public void reflowDisplayLines() {
		if (this.wordWrap) {
			super.reflowDisplayLines();
			return;
		}

		int actualWidth = this.width;
		this.width = Integer.MAX_VALUE; // Somewhat reasonable prevent word wrapping without much actual effort here
		super.reflowDisplayLines();
		this.width = actualWidth;
	}

	public void addParsedLineFromRaw(int lineIndex, String rawLine) {
		Component parsed = PARSER.parseComponent(rawLine, ParserContext.of());

		if (parsed.getString().equals(rawLine)) {
			this.parsedLines.add(lineIndex, Component.literal(rawLine));
		} else {
			this.parsedLines.add(lineIndex, Component.empty().append(parsed).setStyle(Style.EMPTY.withInsertion(rawLine)));
		}
	}

	public String getRawLineFromParsed(int lineIndex) {
		Component line = this.parsedLines.get(lineIndex);

		//noinspection ConstantValue - IDEA says this is marked as @NonNull, but I don't see where
		if (line.getStyle() == null) {
			return line.getString();
		}

		String insert = line.getStyle().getInsertion();
		if (insert == null) {
			return line.getString();
		}

		return insert;
	}

	public void setTextAlignment(TextBlockEntity.TextAlignment textAlignment) {
		this.textAlignment = textAlignment;
	}
}
