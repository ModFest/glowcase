package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.mojang.datafixers.util.Pair;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.text.GlowcaseMultilineEditBox;
import dev.hephaestus.glowcase.client.util.NoteTextColorResource;
import dev.hephaestus.glowcase.item.component.NoteComponent;
import dev.hephaestus.glowcase.packet.C2SEditNoteItem;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.TagParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//TODO: multi-character selection at some point? it may be a bit complex but it'd be nice
public class NoteEditScreen extends TextEditorScreen {
	private static final Identifier TEXTURE = Glowcase.id("textures/gui/note.png");

	private static final int SCREEN_X1 = 3;
	private static final int SCREEN_Y1 = 5;
	private static final int SCREEN_X2 = -3;
	private static final int SCREEN_Y2 = -5;

	private static final int BG_SIZE = 256;

	private static final int BG_WIDTH = 244;
	private static final int BG_HEIGHT = 117;

	private static final int TXT_OFF_Y = 12;
	private static final int TXT_X_PADDING = 15 * 2;
	private static final Component ARROW_LEFT_SYMBOL = Component.literal("«");
	private static final Component ARROW_RIGHT_SYMBOL = Component.literal("»");

	private int editing_line_offset = 0;

	private final List<Component> lines;
	private String title = "";
	private String author = "";
	private NoteComponent.Alignment textAlignment;

	public static final NodeParser PARSER = TagParser.DEFAULT;
	private TextFieldHelper selectionManager;
	private int currentRow;
	private long ticksSinceOpened = 0;
	private GlowcaseMultilineEditBox glowcaseEditBox;

	private boolean signing = false;
	private boolean finalizing = false;
	private List<FormattedText> signing_text;

	private ColorPickerWidget colorPickerWidget;
	private Button doneButton;
	private Button signButton;
	private Button changeAlignment;

	public NoteEditScreen(ItemStack stack) {
		if (stack.has(Glowcase.NOTE_COMPONENT.get())) {
			// Load data
			NoteComponent note = stack.get(Glowcase.NOTE_COMPONENT.get());
			assert note != null;

			lines = new ArrayList<>();
			lines.addAll(note.lines());
			for (int i = 0; i < (NoteComponent.LINES_LIMIT - note.lines().size()); i++)
				lines.add(Component.literal(""));

			textAlignment = note.alignment();
		} else {
			// Default data
			lines = new ArrayList<>();

			for (int i = 0; i < NoteComponent.LINES_LIMIT; i++)
				lines.add(Component.literal(""));

			textAlignment = NoteComponent.Alignment.LEFT;
		}
	}

	@Override
	protected void init() {
		super.init();

		selectionManager = new TextFieldHelper(
			() -> signing ? (currentRow == 6 ? title : author) : getRawLine(currentRow),
			(string) -> {
				if (signing) {
					if (currentRow == 6)
						title = string;
					else
						author = string;
				} else
					setRawLine(currentRow, string);
			},
			TextFieldHelper.createClipboardGetter(minecraft),
			TextFieldHelper.createClipboardSetter(minecraft),
			(string) -> true);

		// Setup Signing Screen

		signing_text = new ArrayList<>();
		//noinspection unchecked
		Pair<Integer, Component>[] lines = new Pair[]{
			new Pair<>(2, Component.translatable("gui.glowcase.note.signing")),
			new Pair<>(3, Component.translatable("gui.glowcase.note.warning")),
			new Pair<>(1, Component.literal("")),
			new Pair<>(1, Component.translatable("gui.glowcase.note.title")),
			new Pair<>(1, Component.translatable("gui.glowcase.note.author")),
			new Pair<>(1, Component.literal("")),
			new Pair<>(1, Component.translatable("gui.glowcase.note.required").setStyle(Style.EMPTY.withColor(ChatFormatting.RED))),
		};
		for (Pair<Integer, Component> section : lines) {
			int height = section.getFirst();
			List<FormattedText> texts = font.getSplitter().splitLines(section.getSecond(), BG_WIDTH - TXT_X_PADDING, Style.EMPTY);

			for (int i = 0; i < height; i++) {
				if (i + 1 <= texts.size()) {
					FormattedText text = texts.get(i);
					if (i == (height - 1) && texts.size() > height)
						text = ensureBounds(font, text);

					signing_text.add(text);
				} else {
					signing_text.add(Component.empty());
				}
			}
		}

		// Widgets
		int offset = 7;

		this.changeAlignment = Button.builder(Component.translatableEscape("gui.glowcase.alignment", textAlignment), action -> {
			switch (textAlignment) {
				case LEFT -> textAlignment = NoteComponent.Alignment.CENTER;
				case CENTER -> textAlignment = NoteComponent.Alignment.RIGHT;
				case RIGHT -> textAlignment = NoteComponent.Alignment.LEFT;
			}

			this.changeAlignment.setMessage(Component.translatableEscape("gui.glowcase.alignment", textAlignment));
		}).bounds(width / 2 - BG_WIDTH / 2, height / 2 - BG_HEIGHT / 2 - offset - 20, BG_WIDTH / 12 * 6 - 3 - 7, 20).build();

		signButton = Button.builder(Component.translatable("book.signButton"), action -> {
			if (!signing) {
				signing = true;
				doneButton.setMessage(Component.translatable("gui.cancel"));
				signButton.setMessage(Component.translatable("book.finalizeButton"));
				signButton.active = false;
				changeAlignment.active = false;
				this.toggleFormattingButtons(false);

				title = "";
				author = "";
				currentRow = 6;
			} else {
				finalizing = true;
				onClose();
			}
		}).bounds(width / 2 - BG_WIDTH / 2, height / 2 + BG_HEIGHT / 2 + offset, BG_WIDTH / 2 - 3, 20).build();
		doneButton = Button.builder(Component.translatable("gui.done"), action -> {
			if (signing) {
				signing = false;
				doneButton.setMessage(Component.translatable("gui.done"));
				signButton.setMessage(Component.translatable("book.signButton"));
				signButton.active = true;
				changeAlignment.active = true;
				this.toggleFormattingButtons(true);
			} else
				onClose();
		}).bounds(width / 2 + BG_WIDTH / 2 - (BG_WIDTH / 2 - 3), height / 2 + BG_HEIGHT / 2 + offset, BG_WIDTH / 2 - 3, 20).build();


		this.colorPickerWidget = this.createColorPickerWidget();

		addRenderableWidget(changeAlignment);
		addRenderableWidget(doneButton);
		addRenderableWidget(signButton);

		initFormattingButtons(width / 2 - BG_WIDTH / 2 + BG_WIDTH / 12 * 6 - 5 - 7, height / 2 - BG_HEIGHT / 2 - offset - 20 - 4, width / 100);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		List<? extends FormattedText> screen = signing ? signing_text : lines;
		NoteComponent.Alignment alignment = signing ? NoteComponent.Alignment.LEFT : textAlignment;

		// Ensure no overflow is happening
		graphics.enableScissor(
			width / 2 - BG_WIDTH / 2 + SCREEN_X1,
			height / 2 - BG_HEIGHT / 2 + SCREEN_Y1,
			width / 2 + BG_WIDTH / 2 + SCREEN_X2,
			height / 2 + BG_HEIGHT / 2 + SCREEN_Y2
		);

		// Text rendering
		boolean overflow = false;
		for (int i = 0; i < screen.size(); i++) {
			FormattedText text = screen.get(i);
			if (signing && i >= 6 && i <= 7)
				text = FormattedText.composite(text, Component.nullToEmpty((i == 6) ? title : author));

			if (outOfBounds(font, text))
				text = ensureBounds(font, text);

			int line_width = font.width(text);
			float x = 0;

			if (i == currentRow && !signing) {
				text = Component.literal(getRawLine(currentRow));
				line_width = font.width(text);
				if (outOfBounds(font, text)) {
					x += width / 2f + BG_WIDTH / 2f - TXT_X_PADDING / 2f - line_width + editing_line_offset;
					overflow = true;
				}
			}

			if (!overflow || i != currentRow) {
				x += switch (alignment) {
					case LEFT -> width / 2f - BG_WIDTH / 2f + TXT_X_PADDING / 2f;
					case CENTER -> width / 2f - line_width / 2f;
					case RIGHT -> width / 2f + BG_WIDTH / 2f - TXT_X_PADDING / 2f - line_width;
				};
			}

			graphics.text(font, Language.getInstance().getVisualOrder(text), (int) x, (height / 2 - BG_HEIGHT / 2 + TXT_OFF_Y) + (font.lineHeight * i), NoteTextColorResource.TXT_COLOR, false);

			if (overflow && i == currentRow) {
				//RenderSystem.enableBlend();
				for (int j = 0; j < font.lineHeight; j++) {
					graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
						width / 2 - BG_WIDTH / 2 + SCREEN_X1,
						height / 2 - BG_HEIGHT / 2 + TXT_OFF_Y + (font.lineHeight * currentRow) + j,
						0, BG_SIZE - 1, 32, 1, BG_SIZE, BG_SIZE
					);

					graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
						width / 2 + BG_WIDTH / 2 + SCREEN_X2 - 32,
						height / 2 - BG_HEIGHT / 2 + TXT_OFF_Y + (font.lineHeight * currentRow) + j,
						0, BG_SIZE - 2, 32, 1, BG_SIZE, BG_SIZE
					);
				}

				if (x < (width / 2f - BG_WIDTH / 2f + SCREEN_X1)) {
					graphics.text(font, ARROW_LEFT_SYMBOL, width / 2 - BG_WIDTH / 2 + SCREEN_X1 + 1, height / 2 - BG_HEIGHT / 2 + TXT_OFF_Y + (font.lineHeight * currentRow), NoteTextColorResource.TXT_COLOR, false);
				}

				if (editing_line_offset > 0) {
					graphics.text(font, ARROW_RIGHT_SYMBOL, width / 2 + BG_WIDTH / 2 + SCREEN_X2 - font.width(ARROW_RIGHT_SYMBOL) - 1, height / 2 - BG_HEIGHT / 2 + TXT_OFF_Y + (font.lineHeight * currentRow), NoteTextColorResource.TXT_COLOR, false);
				}

				//RenderSystem.disableBlend();
			}
		}

		// Cursor / Selection
		// I literally copied this from TextBlockEditScreen, we might want to abstract this further more down too
		int caretStart = selectionManager.getCursorPos();
		int caretEnd = selectionManager.getSelectionPos();

		if (caretStart >= 0) {
			String line = signing
				? (currentRow == 6 ? title : author)
				: getRawLine(currentRow);

			int selectionStart = Mth.clamp(Math.min(caretStart, caretEnd), 0, line.length());
			int selectionEnd = Mth.clamp(Math.max(caretStart, caretEnd), 0, line.length());

			String preSelection = line.substring(0, Mth.clamp(line.length(), 0, selectionStart));
			int startX = minecraft.font.width(preSelection);
			int caretStartY = (height / 2 - BG_HEIGHT / 2 + TXT_OFF_Y) + (font.lineHeight * currentRow);

			float push = switch (overflow ? NoteComponent.Alignment.RIGHT : alignment) {
				case LEFT -> width / 2f - BG_WIDTH / 2f + TXT_X_PADDING / 2f;
				case CENTER -> width / 2f - font.width(line) / 2f;
				case RIGHT -> width / 2f + BG_WIDTH / 2f - TXT_X_PADDING / 2f - font.width(line);
			};

			startX += (int) push;
			if (signing)
				startX += font.width(screen.get(currentRow));

			if (overflow) {
				int apply = 0;

				while ((startX + editing_line_offset + apply) < (width / 2 - BG_WIDTH / 2 + SCREEN_X1 + 32))
					apply++;
				while ((startX + editing_line_offset + apply) > (width / 2 + BG_WIDTH / 2 + SCREEN_X2 - 32))
					apply--;

				editing_line_offset += apply;
				startX += editing_line_offset;
			}

			int caretLength = 9;
			if (this.ticksSinceOpened / 6 % 2 == 0) {
				if (selectionStart < line.length()) {
					graphics.fill(startX, caretStartY, startX + 1, caretStartY + caretLength, 0xCC000000);
				} else {
					graphics.text(font, "_", startX, caretStartY, NoteTextColorResource.TXT_COLOR, false);
				}
			}

			if (caretStart != caretEnd) {
				int endX = startX + font.width(line.substring(selectionStart, selectionEnd));
				graphics.textHighlight(startX, caretStartY, endX, caretStartY + 9, false);
			}
		}

		graphics.disableScissor();
		this.extractColorPicker(graphics, mouseX, mouseY, delta);
	}

	@Override
	public void tick() {
		++this.ticksSinceOpened;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		this.extractTransparentBackground(context);
		context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, width / 2 - BG_WIDTH / 2, height / 2 - BG_HEIGHT / 2, 0, 0, BG_WIDTH, BG_HEIGHT, BG_SIZE, BG_SIZE);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		boolean result;

		int keyCode = event.key();
		if (keyPressedColorPicker(event)) {
			result = true;
		} else {
			setFocused(null);
			result = true;
			if (keyCode == GLFW.GLFW_KEY_UP || (keyCode == GLFW.GLFW_KEY_LEFT && selectionManager.getCursorPos() <= 0 && currentRow > 0)) {
				// Move cursor up
				currentRow = Math.max(currentRow - 1, signing ? 6 : 0);
				editing_line_offset = 0;
				selectionManager.setCursorToEnd();
			} else if (keyCode == GLFW.GLFW_KEY_DOWN || (keyCode == GLFW.GLFW_KEY_RIGHT && selectionManager.getCursorPos() >= getRawLine(currentRow).length() && currentRow < NoteComponent.LINES_LIMIT - 1)) {
				// Move cursor down
				currentRow = Math.min(currentRow + 1, signing ? 7 : NoteComponent.LINES_LIMIT - 1);
				editing_line_offset = 0;

				if (keyCode == GLFW.GLFW_KEY_DOWN)
					selectionManager.setCursorToEnd();
				else
					selectionManager.setCursorToStart();
			} else if (!signing && (currentRow < NoteComponent.LINES_LIMIT - 1) && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
				// Split lines (enter)
				if (hasSpaceLeft()) {
					int cursor = selectionManager.getCursorPos();
					if (cursor <= 0) {
						lines.add(currentRow, Component.nullToEmpty(""));
						currentRow++;
						selectionManager.setCursorToStart();
					} else if (cursor >= getRawLine(currentRow).length()) {
						lines.add(currentRow + 1, Component.nullToEmpty(""));
						currentRow++;
						selectionManager.setCursorToStart();
					} else {
						String curLine = getRawLine(currentRow);
						String newLine = curLine.substring(cursor);
						curLine = curLine.substring(0, cursor);

						setRawLine(currentRow, curLine);
						lines.add(currentRow + 1, Component.nullToEmpty(""));
						setRawLine(currentRow + 1, newLine);

						currentRow++;
						selectionManager.setCursorToStart();
					}
				}
			} else if (!signing && (currentRow > 0 && selectionManager.getCursorPos() <= 0) && (keyCode == GLFW.GLFW_KEY_BACKSPACE)) {
				// Delete before cursor (backspace)
				String curLine = getRawLine(currentRow);
				String before = getRawLine(currentRow - 1);
				setRawLine(currentRow - 1, before + curLine);

				lines.remove(currentRow);
				lines.add(Component.nullToEmpty(""));

				currentRow--;
				selectionManager.setCursorToStart();
				selectionManager.moveByChars(before.length());
			} else if (!signing && (currentRow < NoteComponent.LINES_LIMIT - 1 && selectionManager.getCursorPos() >= getRawLine(currentRow).length()) && (keyCode == GLFW.GLFW_KEY_DELETE)) {
				// Delete after cursor (delete key)
				String curLine = getRawLine(currentRow);
				String after = getRawLine(currentRow + 1);
				setRawLine(currentRow, curLine + after);

				lines.remove(currentRow + 1);
				lines.add(Component.nullToEmpty(""));
			} else if (signing && keyCode == GLFW.GLFW_KEY_TAB) {
				// Tab
				currentRow = (currentRow == 6 ? 7 : 6);
			} else {
				// Rest
				result = selectionManager.keyPressed(event) || super.keyPressed(event);
			}
		}

		if (signing)
			signButton.active = !title.isEmpty();

		return result;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (!signing || (currentRow == 6 ? title : author).length() < NoteComponent.TITLE_LIMIT) {
			this.selectionManager.charTyped(event);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		if (this.mouseClickedColorPicker(event, doubleClick)) return true;

		boolean withinX = (mouseX >= width / 2f - BG_WIDTH / 2f && mouseX <= width / 2f + BG_WIDTH / 2f);
		boolean withinY = (mouseY >= height / 2f - BG_HEIGHT / 2f && mouseY <= height / 2f + BG_HEIGHT / 2f);

		if (withinX && withinY) {
			this.setFocused(null);

			double linePos = mouseY - (height / 2f - BG_HEIGHT / 2f + TXT_OFF_Y);
			double totalHeight = NoteComponent.LINES_LIMIT * font.lineHeight;

			int clickedLine = Math.clamp(
				(int) (NoteComponent.LINES_LIMIT / totalHeight * linePos),
				0,
				NoteComponent.LINES_LIMIT - 1
			);
			if (signing)
				clickedLine = Math.clamp(clickedLine, 6, 7);

			if (clickedLine == currentRow && !signing) {
				// Click on current line, get more precise in-row positioning
				String line = getRawLine(currentRow);
				int chars = line.length();
				Component text = Component.nullToEmpty(line);
				int length = font.width(text);

				int charPos = (int) mouseX;

				if (outOfBounds(font, text)) {
					// Scrolling line
					charPos -= (int) (width / 2f + BG_WIDTH / 2f - TXT_X_PADDING / 2f - length + editing_line_offset);
				} else {
					// Non-scrolling line
					float offset = switch (textAlignment) {
						case LEFT -> width / 2f - BG_WIDTH / 2f + TXT_X_PADDING / 2f;
						case CENTER -> width / 2f - font.width(line) / 2f;
						case RIGHT -> width / 2f + BG_WIDTH / 2f - TXT_X_PADDING / 2f - font.width(line);
					};
					charPos -= (int) offset;
				}

				// Find spot to move the cursor to

				if (charPos >= length) {
					selectionManager.setCursorToEnd();
				} else if (charPos <= 0) {
					selectionManager.setCursorToStart();
				} else {
					// Clicking mid-text
					for (int i = 1; i < chars; i++) {
						String testContents = line.substring(0, i);
						int sub_width = font.width(testContents);
						if (charPos <= sub_width) {
							selectionManager.setCursorToStart();
							selectionManager.moveByChars(i);
							break;
						}
					}
				}
			} else {
				// Apply new line selection
				currentRow = clickedLine;
				selectionManager.setCursorToEnd();
				editing_line_offset = 0;
			}

			return true;
		} else {
			return super.mouseClicked(event, doubleClick);
		}
	}

	private boolean hasSpaceLeft() {
		Component last = lines.getLast();
		if (last.getString().isEmpty()) {
			lines.removeLast();
			return true;
		}
		return false;
	}

	public String getRawLine(int i) {
		var line = this.lines.get(i);
		return extractRaw(line);
	}

	public void setRawLine(int i, String string) {
		var parsed = PARSER.parseComponent(string, ParserContext.of());

		if (parsed.getString().equals(string)) {
			this.lines.set(i, Component.literal(string));
		} else {
			this.lines.set(i, Component.empty().append(parsed).setStyle(Style.EMPTY.withInsertion(string)));
		}
	}

	public static <T extends FormattedText> boolean outOfBounds(Font textRenderer, T text) {
		int line_width = textRenderer.width(text);
		return (line_width > (BG_WIDTH - TXT_X_PADDING));
	}

	public static FormattedText ensureBounds(Font textRenderer, FormattedText text) {
		FormattedText ellipsis = FormattedText.of("...");
		return FormattedText.composite(
			textRenderer.substrByWidth(text, BG_WIDTH - TXT_X_PADDING - textRenderer.width(ellipsis)),
			ellipsis
		);
	}

	public static String extractRaw(Component text) {
		if (text.getStyle() == null) {
			return text.getString();
		}

		var insert = text.getStyle().getInsertion();

		if (insert == null) {
			return text.getString();
		}
		return insert;
	}

	@Override
	public CustomPacketPayload getUpdatePayload() {
		if (finalizing) {
			// Remove insertion for optimization as it is not needed anymore
			for (int i = 0; i < lines.size(); i++) {
				String rawLine = getRawLine(i);
				Component text = PARSER.parseComponent(rawLine, ParserContext.of());
				lines.set(i, text);
			}
		}

		return new C2SEditNoteItem(new NoteComponent(
			lines,
			textAlignment,
			finalizing ? Optional.of(title) : Optional.empty(),
			(finalizing && !author.isBlank()) ? Optional.of(author) : Optional.empty()
		));
	}

	@Override
	public ColorPickerWidget getColorPickerWidget() {
		return this.colorPickerWidget;
	}

	@Override
	GlowcaseMultilineEditBox getGlowcaseMultilineEditBox() {
		return this.glowcaseEditBox;
	}
}
