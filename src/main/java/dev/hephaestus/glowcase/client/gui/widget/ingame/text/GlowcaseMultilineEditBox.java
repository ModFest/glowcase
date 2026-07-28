package dev.hephaestus.glowcase.client.gui.widget.ingame.text;

import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.TagParser;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import eu.pb4.placeholders.api.parsers.tag.TextTag;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.TextCursorUtils;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

// GOALS:
// - Change text alignment (LEFT, CENTER, RIGHT)
// - Change text color, toggle text shadow
// - Update listener instead of only value listener
// - Be able to resize and update position
// - Render QuickText on non-selected lines
// - View-only boolean? (e.g. PopupBlockViewScreen)

// CHANGES FROM EXTENDED:
// - Account for text alignment (rendering & clicking)
// - QuickText rendering
// - Account for changeable text color & shadow
// - Update listener
// - LINE HEIGHT IS 12, NOT 9!!!
// - View-only mode for Popup Block?

// TODO - Allow disable vertical overflow
// TODO - Allow disable horizontal overflow using the parsed width? (Allow overflow of raw for editing)
// FIXME - Ctrl+right on final word of line moves cursor to beginning of next line, instead of end of that line
public class GlowcaseMultilineEditBox extends MultiLineEditBox {
	public static final NodeParser PARSER = TagParser.DEFAULT;

	public final List<Component> parsedLines;
	public final Consumer<List<Component>> parsedUpdateListener;
	public final Font font;
	public int sideAlignmentPadding;
	public int lineHeight;
	public int maxLines;
	public boolean parsedHorizontalBounds;

	public int textColor = ColorUtil.WHITE;
	public boolean textShadow = true;
	public TextBlockEntity.TextAlignment textAlignment = TextBlockEntity.TextAlignment.CENTER;

	public long focusedTime;

	public static Builder builder(Font font, List<Component> lines, int x, int y, int width, int height, Consumer<List<Component>> parsedUpdateListener) {
		return new Builder(font, lines, x, y, width, height, parsedUpdateListener);
	}

	public GlowcaseMultilineEditBox(Font font, List<Component> parsedLines, int x, int y, int width, int height, int sideAlignmentPadding, int lineHeight, int maxLines, boolean parsedHorizontalBounds, boolean showBackground, boolean showDecorations, Consumer<List<Component>> parsedUpdateListener) {
		super(font, x, y, width, height, CommonComponents.EMPTY, CommonComponents.EMPTY, ColorUtil.WHITE, true, -3092272, showBackground, showDecorations);
		this.font = font;
		this.parsedLines = parsedLines;
		this.sideAlignmentPadding = sideAlignmentPadding;
		this.lineHeight = lineHeight;
		this.maxLines = maxLines;
		this.parsedHorizontalBounds = parsedHorizontalBounds;
		this.parsedUpdateListener = parsedUpdateListener;

		this.focusedTime = Util.getMillis();

		this.textField.setLineLimit(this.maxLines);
		this.setValueFromLines();
		this.setValueListener(this::parseContents);
	}

	public void updateSettings(int textColor, boolean textShadow, TextBlockEntity.TextAlignment textAlignment) {
		this.textColor = textColor;
		this.textShadow = textShadow;
		this.textAlignment = textAlignment;
	}

	public void setTextColor(int textColor) {
		this.textColor = textColor;
	}

	public void setTextShadow(boolean textShadow) {
		this.textShadow = textShadow;
	}

	public void setTextAlignment(TextBlockEntity.TextAlignment textAlignment) {
		this.textAlignment = textAlignment;
	}

	public void parseContents(String value) {
		this.parsedLines.clear();
		int lineCount = 0;
		for (MultilineTextField.StringView lineView : this.textField.iterateLines()) {
			String line = value.substring(lineView.beginIndex(), lineView.endIndex());
			this.addParsedLineFromRaw(lineCount, line);
			lineCount++;
		}
		this.parsedUpdateListener.accept(this.parsedLines);
	}

	public void setValueFromLines() {
		StringBuilder valueBuilder = new StringBuilder();
		for (int i = 0; i < parsedLines.size(); i++) {
			valueBuilder.append(this.getRawLineFromParsed(i));
			if (i < parsedLines.size() - 1) valueBuilder.append("\n"); // Append new line to all except last
		}
		this.setValue(valueBuilder.toString());
	}

	// TODO - Beyond edges indicators (from note screen)
	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		// Debug - View edit regions depending on text alignment (red is center, green is left & right alignments)
//		graphics.outline(this.getX() + 1, this.getY() + 1, this.getWidth() - 2, this.getHeight() - 2, ColorUtil.RED);
//		graphics.outline(this.getX() + this.sideAlignmentPadding, this.getY() + 2, this.getWidth() - (this.sideAlignmentPadding * 2), this.getHeight() - 4, ColorUtil.GREEN);

		// Render text lines
		boolean insetCursor = false;
		int cursorPos = this.textField.cursor();
		int cursorX = 0;
		int cursorY = 0;
		for (int i = 0; i < this.parsedLines.size(); i++) {
			int lineY = this.getY() + this.innerPadding() + i * this.lineHeight;
			boolean lineWithinVisibleBounds = this.withinContentAreaTopBottom(lineY, lineY + this.lineHeight);
			if (!lineWithinVisibleBounds) continue;

			MultilineTextField.StringView view = this.textField.getLineView(i);
			MultilineTextField.StringView selectedView = this.textField.getSelected();
			boolean isCursorLine = i == this.textField.getLineAtCursor();
			boolean isSelectedLine = this.textField.hasSelection()
				&& (selectedView.endIndex() > view.beginIndex() && selectedView.beginIndex() < view.endIndex());
			boolean renderRawLine = isCursorLine || isSelectedLine;

			Component renderedLine = renderRawLine ?
				Component.literal(this.getRawLineFromParsed(i)) :
				this.parsedLines.get(i);
			int renderedLineWidth = this.font.width(renderedLine);
			int lineX = this.getTextAlignmentX(renderedLineWidth);
			graphics.text(this.font, renderedLine, lineX, lineY, this.textColor, this.textShadow);

			// Set cursor positions
			if (isCursorLine) {
				insetCursor = cursorPos < view.endIndex();

				String beforeCursor = this.textField.value().substring(view.beginIndex(), cursorPos);
				int beforeCursorWidth = this.font.width(beforeCursor);
				cursorX = this.getTextAlignmentX(renderedLineWidth) + beforeCursorWidth;
				cursorY = lineY;
			}

			// Render selection highlight
			if (isSelectedLine) {
				String startSelectWithinLine = this.textField.value().substring(view.beginIndex(), Math.max(selectedView.beginIndex(), view.beginIndex()));
				String endSelectWithinLine = this.textField.value().substring(view.beginIndex(), Math.min(view.endIndex(), selectedView.endIndex()));
				int highlightStartX = lineX + this.font.width(startSelectWithinLine);
				int highlightEndX = lineX + this.font.width(endSelectWithinLine);
				graphics.textHighlight(highlightStartX, lineY, highlightEndX, lineY + 9, true);
			}
		}

		// Render cursor
		boolean showCursor = this.isFocused() && TextCursorUtils.isCursorVisible(Util.getMillis() - this.focusedTime);
		if (showCursor) {
			if (insetCursor) TextCursorUtils.extractInsertCursor(graphics, cursorX, cursorY, 0xCCFFFFFF, 10);
			else TextCursorUtils.extractAppendCursor(graphics, this.font, cursorX, cursorY, 0xFFFFFFFF, true);
		}
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

	public void insertText(String text) {
		this.textField.insertText(text);
	}

	public void insertTag(String tagName) {
		if (tagName.isBlank()) return;

		String openTag = "<" + tagName + ">";
		String closeTag = "</" + tagName + ">";

		if (this.textField.hasSelection()) {
			MultilineTextField.StringView selectedView = this.textField.getSelected();
			String value = this.textField.value();
			String beforeSelected = value.substring(0, selectedView.beginIndex());
			String selected = value.substring(selectedView.beginIndex(), selectedView.endIndex());
			String afterSelected = value.substring(selectedView.endIndex());

			String newSelected = openTag + selected.replaceAll("\n", closeTag + "\n" + openTag) + closeTag;
			int addedLength = newSelected.length() - selected.length();
			int preInsertCursor = this.textField.cursor();
			this.textField.setValue(beforeSelected + newSelected + afterSelected);
			this.textField.seekCursor(Whence.ABSOLUTE, preInsertCursor + addedLength - closeTag.length());
		} else {
			int preInsertCursor = this.textField.cursor();
			this.insertText(openTag + closeTag);
			this.textField.seekCursor(Whence.ABSOLUTE, preInsertCursor + openTag.length());
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
	protected void scrollToCursor() {
		double scrollAmount = this.scrollAmount();
		MultilineTextField.StringView firstFullyVisibleLine = this.textField.getLineView((int)(scrollAmount / this.lineHeight));
		if (this.textField.cursor() <= firstFullyVisibleLine.beginIndex()) {
			scrollAmount = this.textField.getLineAtCursor() * this.lineHeight;
		} else {
			MultilineTextField.StringView lastFullyVisibleLine = this.textField.getLineView((int)((scrollAmount + this.height) / this.lineHeight) - 1);
			if (this.textField.cursor() > lastFullyVisibleLine.endIndex()) {
				scrollAmount = this.textField.getLineAtCursor() * this.lineHeight - this.height + this.lineHeight + this.totalInnerPadding();
			}
		}

		this.setScrollAmount(scrollAmount);
	}

	@Override
	protected void seekCursorScreen(double mouseX, double mouseY) {
		double relativeY = mouseY - this.getY() - (this.innerPadding() / 2.0) + this.scrollAmount();
		int top = Mth.floor(relativeY / this.lineHeight);
		MultilineTextField.StringView clickedLineView = this.textField.getLineView(Mth.clamp(top, 0, this.textField.getLineCount() - 1));

		String rawLine = this.textField.value().substring(clickedLineView.beginIndex(), clickedLineView.endIndex());
		double relativeX = mouseX - this.getTextAlignmentX(this.font.width(rawLine)) + this.getX(); // Adding X because it feels better for some reason
		int left = Mth.floor(relativeX);
		int clickedColumn = this.font.plainSubstrByWidth(this.textField.value().substring(clickedLineView.beginIndex(), clickedLineView.endIndex()), left).length();
		this.textField.seekCursor(Whence.ABSOLUTE, clickedLineView.beginIndex() + clickedColumn);
	}

	public int getTextAlignmentX(int lineWidth) {
		return this.getTextAlignmentX(this.textAlignment, lineWidth);
	}

	public int getTextAlignmentX(TextBlockEntity.TextAlignment alignment, int lineWidth) {
		int xPush = this.getTextAlignmentPush(alignment);
		int xLineOffset = switch (alignment) { // Defaults to CENTER (includes CENTER_LEFT & CENTER_RIGHT with it)
			case LEFT -> xPush;
			case RIGHT -> xPush - lineWidth;
			default -> xPush - lineWidth / 2;
		};
		return this.getX() + xLineOffset;
	}

	public int getTextAlignmentPush() {
		return this.getTextAlignmentPush(this.textAlignment);
	}

	public int getTextAlignmentPush(TextBlockEntity.TextAlignment alignment) {
		return switch(alignment) {
			case LEFT -> this.sideAlignmentPadding;
			case RIGHT -> this.getWidth() - this.sideAlignmentPadding;
			default -> this.getWidth() / 2;
		};
	}

	public String getRawLineFromParsed(int i) {
		Component line = this.parsedLines.get(i);

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

	public void addParsedLineFromRaw(int i, String string) {
		var parsed = PARSER.parseComponent(string, ParserContext.of());

		if (parsed.getString().equals(string)) {
			this.parsedLines.add(i, Component.literal(string));
		} else {
			this.parsedLines.add(i, Component.empty().append(parsed).setStyle(Style.EMPTY.withInsertion(string)));
		}
	}

	public void setParsedLineFromRaw(int i, String string) {
		Component parsed = PARSER.parseComponent(string, ParserContext.of());

		if (parsed.getString().equals(string)) {
			this.parsedLines.set(i, Component.literal(string));
		} else {
			this.parsedLines.set(i, Component.empty().append(parsed).setStyle(Style.EMPTY.withInsertion(string)));
		}
	}

	@Override
	public int getInnerHeight() {
		return this.textField.getLineCount() * this.lineHeight;
	}

	public static class Builder {
		private final Font font;
		private final List<Component> lines;
		private final int x, y, width, height;
		private final Consumer<List<Component>> parsedUpdateListener;

		private int sideAlignmentPadding;
		private int lineHeight = 12;
		private int maxLines = Integer.MAX_VALUE;
		private boolean parsedHorizontalBounds = false;
		private boolean showBackground = false;
		private boolean showDecorations = true;

		public Builder(Font font, List<Component> lines, int x, int y, int width, int height, Consumer<List<Component>> parsedUpdateListener) {
			this.font = font;
			this.lines = lines;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.parsedUpdateListener = parsedUpdateListener;

			this.sideAlignmentPadding = this.width / 10;
		}

		public Builder setSideAlignmentPadding(int sideAlignmentPadding) {
			this.sideAlignmentPadding = sideAlignmentPadding;
			return this;
		}

		public Builder setLineHeight(int lineHeight) {
			this.lineHeight = lineHeight;
			return this;
		}

		public Builder setMaxLines(int maxLines) {
			this.maxLines = maxLines;
			return this;
		}

		public Builder parsedHorizontalBounds(boolean parsedHorizontalBounds) {
			this.parsedHorizontalBounds = parsedHorizontalBounds;
			return this;
		}

		public Builder showBackground(boolean showBackground) {
			this.showBackground = showBackground;
			return this;
		}

		public Builder showDecorations(boolean showDecorations) {
			this.showDecorations = showDecorations;
			return this;
		}

		public GlowcaseMultilineEditBox build() {
			return new GlowcaseMultilineEditBox(
				this.font, this.lines,
				this.x, this.y, this.width, this.height,
				this.sideAlignmentPadding, this.lineHeight, this.maxLines, this.parsedHorizontalBounds,
				this.showBackground, this.showDecorations,
				this.parsedUpdateListener
			);
		}
	}
}
