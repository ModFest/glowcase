package dev.hephaestus.glowcase.client.gui.widget.ingame.text;

import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import dev.hephaestus.glowcase.client.util.GuiGraphicsUtil;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.TagParser;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import eu.pb4.placeholders.api.parsers.tag.TextTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.TextCursorUtils;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
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

// TODO - Allow disable horizontal overflow using the parsed width? (Allow overflow of raw for editing)
// FIXME - Ctrl+right on final word of line moves cursor to beginning of next line, instead of end of that line
public class GlowcaseMultilineEditBox extends MultiLineEditBox {
//	public static final NodeParser PARSER = TagParser.DEFAULT;
	private static final Component ARROW_LEFT_SYMBOL = Component.literal("«");
	private static final Component ARROW_RIGHT_SYMBOL = Component.literal("»");
//	private static final Component ELLIPSIS_SYMBOL = Component.literal("...");

	public final FormattableMultilineTextField formatTextField;

//	public final List<Component> parsedLines;
	public final Consumer<List<Component>> parsedUpdateListener;
	public final Font font;
	public int sideAlignmentPadding;
	public int lineHeight;
	public int maxLines;
	public boolean parsedHorizontalBounds;
	public int overflowArrowColor;

	public int textColor = ColorUtil.WHITE;
	public boolean textShadow = true;
//	public TextBlockEntity.TextAlignment textAlignment = TextBlockEntity.TextAlignment.CENTER;

	public long focusedTime;

	public static Builder builder(Font font, List<Component> lines, int x, int y, int width, int height, Consumer<List<Component>> parsedUpdateListener) {
		return new Builder(font, lines, x, y, width, height, parsedUpdateListener);
	}

	public GlowcaseMultilineEditBox(
		Font font, List<Component> parsedLines,
		int x, int y, int width, int height,
		int sideAlignmentPadding, int lineHeight, int maxLines,
		boolean parsedHorizontalBounds, boolean showBackground, boolean showDecorations,
		int overflowArrowColor,
		Consumer<List<Component>> parsedUpdateListener
	) {
		super(font, x, y, width, height, CommonComponents.EMPTY, CommonComponents.EMPTY, ColorUtil.WHITE, true, -3092272, showBackground, showDecorations);
		this.font = font;
//		this.parsedLines = parsedLines;
		this.sideAlignmentPadding = sideAlignmentPadding;
		this.lineHeight = lineHeight;
		this.maxLines = maxLines;
		this.parsedHorizontalBounds = parsedHorizontalBounds;
		this.parsedUpdateListener = parsedUpdateListener;
		this.overflowArrowColor = overflowArrowColor;

		this.focusedTime = Util.getMillis();

		this.formatTextField = new FormattableMultilineTextField(this.font, parsedLines, x, width, sideAlignmentPadding, parsedUpdateListener);
		this.formatTextField.setLineLimit(this.maxLines);
		this.formatTextField.setCursorListener(this::scrollToCursor);
		this.textField = this.formatTextField;
//		this.textField.setLineLimit(this.maxLines);
//		this.textField.width = 9999; // Hack to make the text field not word wrap (well, at least take longer too)
//		this.setValueFromLines();
//		this.setValueListener(this::parseContents);
	}

	public void updateSettings(int textColor, boolean textShadow, TextBlockEntity.TextAlignment textAlignment) {
		this.textColor = textColor;
		this.textShadow = textShadow;
		this.formatTextField.setTextAlignment(textAlignment);
	}

	public void setTextColor(int textColor) {
		this.textColor = textColor;
	}

	public void setTextShadow(boolean textShadow) {
		this.textShadow = textShadow;
	}

	public void setTextAlignment(TextBlockEntity.TextAlignment textAlignment) {
		this.formatTextField.setTextAlignment(textAlignment);
	}

//	public void parseContents(String value) {
//		this.parsedLines.clear();
//		int lineCount = 0;
//		for (MultilineTextField.StringView lineView : this.textField.iterateLines()) {
//			String line = value.substring(lineView.beginIndex(), lineView.endIndex());
//			this.addParsedLineFromRaw(lineCount, line);
//			lineCount++;
//		}
//		this.parsedUpdateListener.accept(this.parsedLines);
//	}

//	public void setValueFromLines() {
//		StringBuilder valueBuilder = new StringBuilder();
//		for (int i = 0; i < parsedLines.size(); i++) {
//			valueBuilder.append(this.getRawLineFromParsed(i));
//			if (i < parsedLines.size() - 1) valueBuilder.append("\n"); // Append new line to all except last
//		}
//		this.setValue(valueBuilder.toString());
//	}

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
		for (int i = 0; i < this.formatTextField.parsedLines.size(); i++) {
			int lineY = this.getY() + this.innerPadding() + i * this.lineHeight;
			boolean lineWithinVisibleBounds = this.withinContentAreaTopBottom(lineY, lineY + this.lineHeight);
			if (!lineWithinVisibleBounds) continue;

			MultilineTextField.StringView view = this.textField.getLineView(i);
			MultilineTextField.StringView selectedView = this.textField.getSelected();
			boolean isCursorLine = this.formatTextField.isCursorLine(i);
			boolean isSelectedLine = this.formatTextField.isSelectedLine(i);

//			boolean renderRawLine = isCursorLine || isSelectedLine;

			Component renderedLine = this.formatTextField.getLineForRender(i);

//			Component renderedLine = renderRawLine ?
//				Component.literal(this.getRawLineFromParsed(i)) :
//				this.parsedLines.get(i);
			int renderedLineWidth = this.font.width(renderedLine);
			boolean overflows = this.font.width(renderedLine) >= this.getWidth(); // Used for positioning & truncating
			boolean overflowsLeft = false; // Used for rendering overflow arrows
			boolean overflowsRight = false;

			int lineX = this.formatTextField.getLineX(i, renderedLineWidth);

//			int lineX = this.getTextAlignmentX(renderedLineWidth);
//
//			if (overflows) {// Render truncated line if not selected or with cursor, otherwise offset lineX based on cursor
//				if (!isSelectedLine && !isCursorLine) {
//					lineX = Math.max(this.getX() + 1, lineX);
//
//					int ellipsisWidth = this.font.width(ELLIPSIS_SYMBOL);
//					renderedLine = Component.literal(
//						this.font.plainSubstrByWidth(
//							this.getRawLineFromParsed(i),
//							this.getWidth()
//								- (this.textAlignment == TextBlockEntity.TextAlignment.RIGHT || this.textAlignment == TextBlockEntity.TextAlignment.LEFT
//									? this.sideAlignmentPadding : 0)
//								- ellipsisWidth
//						)
//					).append(ELLIPSIS_SYMBOL);
//				}
//				renderedLineWidth = this.font.width(renderedLine);
//			}

			// Set cursor positions
			if (isCursorLine) {
				insetCursor = cursorPos < view.endIndex();

				String beforeCursor = this.textField.value().substring(view.beginIndex(), cursorPos);
				int beforeCursorWidth = this.font.width(beforeCursor);
//				cursorX = this.getTextAlignmentX(renderedLineWidth) + beforeCursorWidth;
				cursorX = lineX + beforeCursorWidth;
				cursorY = lineY;

				if (overflows) {
					overflowsLeft = lineX <= this.getX();
					overflowsRight = lineX + renderedLineWidth >= this.getX() + this.getWidth();
				}

//				if (overflows) {
//					int extendPadding = 32;
//					int cursorDistLeft = cursorX - this.getX();
//					int cursorDistRight = this.getX() + this.getWidth() - cursorX;
//
//					if (cursorDistLeft < extendPadding) {
//						int diff = extendPadding - cursorDistLeft;
//						lineX += diff;
//						cursorX += diff;
//					} else if (cursorDistRight < extendPadding) {
//						int diff = extendPadding - cursorDistRight;
//						lineX -= diff;
//						cursorX -= diff;
//					}
//
//					overflowsLeft = lineX <= this.getX();
//					overflowsRight = lineX + renderedLineWidth >= this.getX() + this.getWidth();
//				}
			}


			// Render overflow indicators & fade text towards overflow
			if (overflows && isCursorLine) {
//				int fadeX = 32;
//				int nonFadedLineX = lineX; // These will change if there is overflow on either side
//				Component nonFadedRenderedLine = renderedLine.copy();
//				int nonFadedRenderedLineWidth = renderedLineWidth;
//
				// Cursed, but basically substrings the rendered line to render individual characters fading to the edges of the screen
//				if (overflowsLeft) {
//					int leftSideWidth = Mth.abs(nonFadedLineX - this.getX() - fadeX);
//					String leftSideText = this.font.plainSubstrByWidth(nonFadedRenderedLine.getString(), leftSideWidth);
//					String fadeOutText = this.font.plainSubstrByWidth(leftSideText, fadeX, true);
//					String removedText = leftSideText.substring(0, leftSideText.length() - fadeOutText.length());
//					nonFadedLineX += this.font.width(removedText); // Add now removed text width
//					for (char fadingChar : fadeOutText.toCharArray()) {
//						float alpha = Math.max((float) nonFadedLineX / (this.getX() + fadeX), 0.1f);
//						int color = ARGB.color(alpha, this.textColor);
//						String fadingStringChar = String.valueOf(fadingChar);
//						graphics.text(this.font, fadingStringChar, nonFadedLineX, lineY, color, this.textShadow);
//						nonFadedLineX += this.font.width(fadingStringChar);
//					}
//					nonFadedRenderedLine = Component.literal(
//						nonFadedRenderedLine.getString().substring(leftSideText.length())
//					);
//					nonFadedRenderedLineWidth = this.font.width(nonFadedRenderedLine);
//				}
//				if (overflowsRight) {
//					int rightSideWidth = nonFadedLineX + nonFadedRenderedLineWidth - this.getX() - this.getWidth() + fadeX;
//					String rightSideText = this.font.plainSubstrByWidth(nonFadedRenderedLine.getString(), rightSideWidth, true);
//					String fadeOutText = this.font.plainSubstrByWidth(rightSideText, fadeX);
//					nonFadedRenderedLine = Component.literal(
//						nonFadedRenderedLine.getString().substring(0, nonFadedRenderedLine.getString().length() - rightSideText.length())
//					);
//					nonFadedRenderedLineWidth = this.font.width(nonFadedRenderedLine);
//					int charX = nonFadedLineX + nonFadedRenderedLineWidth;
//					for (char fadingChar : fadeOutText.toCharArray()) {
//						float alpha = Math.max(1f -
//							(float) (charX - nonFadedLineX - nonFadedRenderedLineWidth) / (this.getX() + this.getWidth() - nonFadedLineX - nonFadedRenderedLineWidth),
//							0.1f
//						);
//						int color = ARGB.color(alpha, this.textColor);
//						String fadingStringChar = String.valueOf(fadingChar);
//						graphics.text(this.font, fadingStringChar, charX, lineY, color, this.textShadow);
//						charX += this.font.width(fadingStringChar);
//					}
//				}
////
//				graphics.text(this.font, nonFadedRenderedLine, nonFadedLineX, lineY, this.textColor, this.textShadow);

				int fadeWidth = 32;
				int nonFadedLineX = lineX;
				String nonFadedText = renderedLine.getString();
				int nonFadedLineWidth = this.font.width(nonFadedText);
				if (overflowsLeft) {
					int leftSideWidth = Mth.abs(nonFadedLineX - this.getX() - fadeWidth);
					String leftSideText = this.font.plainSubstrByWidth(nonFadedText, leftSideWidth);
					String fadeInText = this.font.plainSubstrByWidth(leftSideText, fadeWidth, true);
					String removedText = nonFadedText.substring(0, leftSideText.length() - fadeInText.length());
					int fadeInX = nonFadedLineX + this.font.width(removedText);
					GuiGraphicsUtil.extractFadingText(graphics, this.font, fadeInText, fadeInX, lineY, fadeWidth, this.textColor, this.textShadow, true);

					nonFadedText = nonFadedText.substring(leftSideText.length());
					nonFadedLineWidth = this.font.width(nonFadedText);
					nonFadedLineX += this.font.width(leftSideText);
				}
				if (overflowsRight) {
					int rightSideWidth = nonFadedLineX + nonFadedLineWidth - this.getX() - this.getWidth() + fadeWidth;
					String rightSideText = this.font.plainSubstrByWidth(nonFadedText, rightSideWidth, true);
					String fadeOutText = this.font.plainSubstrByWidth(rightSideText, fadeWidth);
					String removedText = nonFadedText.substring(rightSideText.length());

					nonFadedText = nonFadedText.substring(0, removedText.length());
					nonFadedLineWidth = this.font.width(nonFadedText);
					int fadeOutX = nonFadedLineX + nonFadedLineWidth;
					GuiGraphicsUtil.extractFadingText(graphics, this.font, fadeOutText, fadeOutX, lineY, fadeWidth, this.textColor, this.textShadow, false);
				}

				graphics.text(this.font, nonFadedText, nonFadedLineX, lineY, this.textColor, this.textShadow);
				if (overflowsLeft) graphics.text(this.font, ARROW_LEFT_SYMBOL, this.getX() + 2, lineY, this.overflowArrowColor);
				if (overflowsRight) graphics.text(this.font, ARROW_RIGHT_SYMBOL, this.getX() + this.getWidth() - this.font.width(ARROW_RIGHT_SYMBOL) - 2, lineY, this.overflowArrowColor);
			} else {
				graphics.text(this.font, renderedLine, lineX, lineY, this.textColor, this.textShadow);
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
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		// Allow what is practically a triple click to select the whole line (mirrors modern document editors)
		if (doubleClick && this.formatTextField.hasSelection()) {
			this.formatTextField.selectLineAtCursor();
		} else {
			super.onClick(event, doubleClick);
		}
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		if (Minecraft.getInstance().hasShiftDown()) {
			int cursorLine = this.formatTextField.getLineAtCursor();
			MultilineTextField.StringView lineView = this.formatTextField.getLineView(cursorLine);
			int lineWidth = this.font.width(this.formatTextField.value().substring(lineView.beginIndex(), lineView.endIndex()));
			if (lineWidth >= this.getWidth()) {
				this.formatTextField.cursorOverflowX = Mth.clamp(
					this.formatTextField.cursorOverflowX + Mth.floor(scrollY * 5f),
					-lineWidth + this.getWidth() - 32, 32 // FIXME - This doesn't cap properly on non-centered alignment
				);
				return true;
			}
		}
		return super.mouseScrolled(mx, my, scrollX, scrollY);
	}

	//	@Override
//	public boolean keyPressed(KeyEvent event) {
//		// Check formatting hotkeys first, then key press textField if no hotkeys were pressed
//		int keyCode = event.key();
//		if (event.hasControlDown()) {
//			if (keyCode == GLFW.GLFW_KEY_B) {
//				this.insertTextTag(TagRegistry.SAFE.getTag("bold"));
//				return true;
//			} else if (keyCode == GLFW.GLFW_KEY_I) {
//				this.insertTextTag(TagRegistry.SAFE.getTag("italic"));
//				return true;
//			} else if (keyCode == GLFW.GLFW_KEY_U) {
//				this.insertTextTag(TagRegistry.SAFE.getTag("underline"));
//				return true;
//			} else if (keyCode == GLFW.GLFW_KEY_5 || keyCode == GLFW.GLFW_KEY_S) {
//				// There isn't a commonly agreed upon hotkey for strikethrough unlike the rest above...
//				// Apparently 5 is commonly used for strikethrough ¯\_(ツ)_/¯
//				// Google Docs and Microsoft Word have 5 in their hotkeys, while Discord has S in its hotkey
//				this.insertTextTag(TagRegistry.SAFE.getTag("strikethrough"));
//				return true;
//			} else if (keyCode == GLFW.GLFW_KEY_O) {
//				this.insertTextTag(TagRegistry.SAFE.getTag("obfuscated"));
//				return true;
//			}
//		}
//		return super.keyPressed(event);
//	}

	public void insertText(String text) {
		this.formatTextField.insertText(text);
	}

	public void insertTag(String tagName) {
		this.formatTextField.insertTag(tagName);
//		if (tagName.isBlank()) return;
//
//		String openTag = "<" + tagName + ">";
//		String closeTag = "</" + tagName + ">";
//
//		if (this.textField.hasSelection()) {
//			MultilineTextField.StringView selectedView = this.textField.getSelected();
//			String value = this.textField.value();
//			String beforeSelected = value.substring(0, selectedView.beginIndex());
//			String selected = value.substring(selectedView.beginIndex(), selectedView.endIndex());
//			String afterSelected = value.substring(selectedView.endIndex());
//
//			String newSelected = openTag + selected.replaceAll("\n", closeTag + "\n" + openTag) + closeTag;
//			int addedLength = newSelected.length() - selected.length();
//			int preInsertCursor = this.textField.cursor();
//			this.textField.setValue(beforeSelected + newSelected + afterSelected);
//			this.textField.seekCursor(Whence.ABSOLUTE, preInsertCursor + addedLength - closeTag.length());
//		} else {
//			int preInsertCursor = this.textField.cursor();
//			this.insertText(openTag + closeTag);
//			this.textField.seekCursor(Whence.ABSOLUTE, preInsertCursor + openTag.length());
//		}
	}

	public void insertTextTag(TextTag tag) {
		this.insertTextTag(tag, true);
	}

	public void insertTextTag(TextTag tag, boolean findShortestAlias) {
		this.formatTextField.insertTextTag(tag, findShortestAlias);
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
		int lineIndex = Mth.clamp(top, 0, this.textField.getLineCount() - 1);
		MultilineTextField.StringView clickedLineView = this.textField.getLineView(lineIndex);

		String rawLine = this.textField.value().substring(clickedLineView.beginIndex(), clickedLineView.endIndex());
		int lineX = this.formatTextField.getLineX(lineIndex, this.font.width(rawLine));

		double relativeX = mouseX - lineX + this.getX(); // Adding X because it feels better for some reason

		int left = Mth.floor(relativeX);
		int clickedColumn = this.font.plainSubstrByWidth(this.textField.value().substring(clickedLineView.beginIndex(), clickedLineView.endIndex()), left).length();
		this.textField.seekCursor(Whence.ABSOLUTE, clickedLineView.beginIndex() + clickedColumn);
	}

//	public int getTextAlignmentX(int lineWidth) {
//		return this.getTextAlignmentX(this.textAlignment, lineWidth);
//	}
//
//	public int getTextAlignmentX(TextBlockEntity.TextAlignment alignment, int lineWidth) {
//		int xPush = this.getTextAlignmentPush(alignment);
//		int xLineOffset = switch (alignment) { // Defaults to CENTER (includes CENTER_LEFT & CENTER_RIGHT with it)
//			case LEFT -> xPush;
//			case RIGHT -> xPush - lineWidth;
//			default -> xPush - lineWidth / 2;
//		};
//		return this.getX() + xLineOffset;
//	}
//
//	public int getTextAlignmentPush() {
//		return this.getTextAlignmentPush(this.textAlignment);
//	}
//
//	public int getTextAlignmentPush(TextBlockEntity.TextAlignment alignment) {
//		return switch(alignment) {
//			case LEFT -> this.sideAlignmentPadding;
//			case RIGHT -> this.getWidth() - this.sideAlignmentPadding;
//			default -> this.getWidth() / 2;
//		};
//	}

//	public String getRawLineFromParsed(int i) {
//		Component line = this.parsedLines.get(i);
//
//		//noinspection ConstantValue - IDEA says this is marked as @NonNull, but I don't see where
//		if (line.getStyle() == null) {
//			return line.getString();
//		}
//
//		String insert = line.getStyle().getInsertion();
//		if (insert == null) {
//			return line.getString();
//		}
//
//		return insert;
//	}
//
//	public void addParsedLineFromRaw(int i, String string) {
//		var parsed = PARSER.parseComponent(string, ParserContext.of());
//
//		if (parsed.getString().equals(string)) {
//			this.parsedLines.add(i, Component.literal(string));
//		} else {
//			this.parsedLines.add(i, Component.empty().append(parsed).setStyle(Style.EMPTY.withInsertion(string)));
//		}
//	}
//
//	public void setParsedLineFromRaw(int i, String string) {
//		Component parsed = PARSER.parseComponent(string, ParserContext.of());
//
//		if (parsed.getString().equals(string)) {
//			this.parsedLines.set(i, Component.literal(string));
//		} else {
//			this.parsedLines.set(i, Component.empty().append(parsed).setStyle(Style.EMPTY.withInsertion(string)));
//		}
//	}

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
		private int overflowArrowColor = ColorUtil.WHITE;

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

		public Builder setOverflowArrowColor(int overflowArrowColor) {
			this.overflowArrowColor = overflowArrowColor;
			return this;
		}

		public GlowcaseMultilineEditBox build() {
			return new GlowcaseMultilineEditBox(
				this.font, this.lines,
				this.x, this.y, this.width, this.height,
				this.sideAlignmentPadding, this.lineHeight, this.maxLines, this.parsedHorizontalBounds,
				this.showBackground, this.showDecorations,
				this.overflowArrowColor,
				this.parsedUpdateListener
			);
		}
	}
}
