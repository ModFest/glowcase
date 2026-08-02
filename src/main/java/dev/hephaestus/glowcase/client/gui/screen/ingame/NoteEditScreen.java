package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.text.FormattableMultilineTextField;
import dev.hephaestus.glowcase.client.gui.widget.ingame.text.GlowcaseMultilineEditBox;
import dev.hephaestus.glowcase.client.util.NoteTextColorResource;
import dev.hephaestus.glowcase.item.component.NoteComponent;
import dev.hephaestus.glowcase.packet.C2SEditNoteItem;
import eu.pb4.placeholders.api.ParserContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoteEditScreen extends TextEditorScreen {
	private static final Identifier TEXTURE = Glowcase.id("textures/gui/note.png");

	private static final int BG_SIZE = 256;

	private static final int BG_WIDTH = 244;
	private static final int BG_HEIGHT = 117;

	private static final int TXT_OFF_Y = 12;
	private static final int TXT_X_PADDING = 15 * 2;

	private List<Component> lines;
	private String title = "";
	private String author = "";
	private NoteComponent.Alignment textAlignment;

	private boolean signing = false;
	private boolean finalizing = false;

	private List<AbstractWidget> editingWidgets;
	private GlowcaseMultilineEditBox glowcaseEditBox;
	private Button changeAlignment;
	private Button doneAndCancelButton;

	private List<AbstractWidget> signingWidgets;
	private GlowcaseEditBox titleEditBox;
	private GlowcaseEditBox authorEditBox;
	private Button signAndFinalizeButton;

	private ColorPickerWidget colorPickerWidget;

	public NoteEditScreen(ItemStack stack) {
		if (stack.has(Glowcase.NOTE_COMPONENT.get())) {
			// Load data
			NoteComponent note = stack.get(Glowcase.NOTE_COMPONENT.get());
			assert note != null;

			this.lines = new ArrayList<>(
				note.lines().stream()
					.filter(component -> !component.getString().isBlank())
					.toList()
			);
//			lines = new ArrayList<>();
//			lines.addAll(note.lines());
//			for (int i = 0; i < (NoteComponent.LINES_LIMIT - note.lines().size()); i++)
//				lines.add(Component.literal(""));

			textAlignment = note.alignment();
		} else {
			// Default data
			lines = new ArrayList<>();

//			for (int i = 0; i < NoteComponent.LINES_LIMIT; i++)
//				lines.add(Component.literal(""));

			textAlignment = NoteComponent.Alignment.LEFT;
		}
	}

	@Override
	protected void init() {
		super.init();

		// Widgets
		int offset = 7;

		this.glowcaseEditBox = GlowcaseMultilineEditBox.builder(
			this.font, this.lines,
			this.width / 2 - BG_WIDTH / 2 + TXT_X_PADDING / 2, this.height / 2 - BG_HEIGHT / 2 + TXT_OFF_Y,
			BG_WIDTH - TXT_X_PADDING, BG_HEIGHT - TXT_OFF_Y * 2 + 5, // Adding 5 to prevent scroll
			parsedLines -> {
				this.lines = parsedLines;
			})
			.setMaxLines(10)
			.setLineHeight(9)
			.setSideAlignmentPadding(0)
			.setTruncateText(true)
			.setOverflowArrowColor(NoteTextColorResource.TXT_COLOR)
			.setInsetCursorColor(0xCC000000)
			.setAppendCursorColor(NoteTextColorResource.TXT_COLOR)
			.build();
		this.glowcaseEditBox.updateSettings(NoteTextColorResource.TXT_COLOR, false, getTextBlockAlignment(this.textAlignment));
		this.glowcaseEditBox.textField.seekCursor(Whence.ABSOLUTE, 0);

		this.changeAlignment = Button.builder(Component.translatableEscape("gui.glowcase.alignment", textAlignment), action -> {
			switch (textAlignment) {
				case LEFT -> textAlignment = NoteComponent.Alignment.CENTER;
				case CENTER -> textAlignment = NoteComponent.Alignment.RIGHT;
				case RIGHT -> textAlignment = NoteComponent.Alignment.LEFT;
			}

			this.changeAlignment.setMessage(Component.translatableEscape("gui.glowcase.alignment", textAlignment));
			this.glowcaseEditBox.setTextAlignment(getTextBlockAlignment(this.textAlignment));
		}).bounds(width / 2 - BG_WIDTH / 2, height / 2 - BG_HEIGHT / 2 - offset - 20, BG_WIDTH / 12 * 6 - 3 - 7, 20).build();

		this.colorPickerWidget = this.createColorPickerWidget();
		this.initFormattingButtons(width / 2 - BG_WIDTH / 2 + BG_WIDTH / 12 * 6 - 5 - 7, height / 2 - BG_HEIGHT / 2 - offset - 20 - 4, width / 100);

		this.signAndFinalizeButton = Button.builder(Component.translatable("book.signButton"), action -> {
			if (!this.signing) {
				this.switchToSigning();
			} else {
				this.finalizing = true;
				this.onClose();
			}
		}).bounds(width / 2 - BG_WIDTH / 2, height / 2 + BG_HEIGHT / 2 + offset, BG_WIDTH / 2 - 3, 20).build();

		this.doneAndCancelButton = Button.builder(Component.translatable("gui.done"), action -> {
			if (this.signing) {
				this.switchToEditing();
			} else {
				this.onClose();
			}
		}).bounds(width / 2 + BG_WIDTH / 2 - (BG_WIDTH / 2 - 3), height / 2 + BG_HEIGHT / 2 + offset, BG_WIDTH / 2 - 3, 20).build();

		int titleTextWidth = this.font.width(Component.translatable("gui.glowcase.note.title"));
		this.titleEditBox = new GlowcaseEditBox(
			this.font,
			this.glowcaseEditBox.getX() + titleTextWidth,
			this.glowcaseEditBox.getY() + 9 * 6,
			this.glowcaseEditBox.getWidth() - titleTextWidth, 10, Component.empty()
		);
		this.titleEditBox.setResponder(value -> {
			this.title = value;
			this.signAndFinalizeButton.active = !this.title.isBlank();
		});
		this.titleEditBox.setBordered(false);
		this.titleEditBox.setTextColor(NoteTextColorResource.TXT_COLOR);
		this.titleEditBox.setTextShadow(false);

		int authorTextWidth = this.font.width(Component.translatable("gui.glowcase.note.author"));
		this.authorEditBox = new GlowcaseEditBox(
			this.font,
			this.glowcaseEditBox.getX() + authorTextWidth,
			this.glowcaseEditBox.getY() + 9 * 7,
			this.glowcaseEditBox.getWidth() - authorTextWidth, 10, Component.empty()
		);
		this.authorEditBox.setResponder(value -> this.author = value);
		this.authorEditBox.setBordered(false);
		this.authorEditBox.setTextColor(NoteTextColorResource.TXT_COLOR);
		this.authorEditBox.setTextShadow(false);

		this.addRenderableWidget(this.glowcaseEditBox);
		this.addRenderableWidget(this.changeAlignment);
		this.addRenderableWidget(this.doneAndCancelButton);
		this.addRenderableWidget(this.signAndFinalizeButton);
		this.editingWidgets = new ArrayList<>();
		this.editingWidgets.add(this.glowcaseEditBox);
		this.editingWidgets.add(this.changeAlignment);
		this.editingWidgets.addAll(this.formattingButtons);

		this.addRenderableWidget(this.titleEditBox);
		this.addRenderableWidget(this.authorEditBox);
		this.signingWidgets = List.of(this.titleEditBox, this.authorEditBox);

		this.switchToEditing();
	}

	public void switchToSigning() {
		this.signing = true;
		for (AbstractWidget editingWidget : this.editingWidgets) {
			editingWidget.visible = false;
		}
		for (AbstractWidget signingWidget : this.signingWidgets) {
			signingWidget.visible = true;
		}

		this.doneAndCancelButton.setMessage(Component.translatable("gui.cancel"));
		this.signAndFinalizeButton.setMessage(Component.translatable("book.finalizeButton"));
		this.title = "";
		this.author = "";
		this.setFocused(this.titleEditBox);
	}

	public void switchToEditing() {
		this.signing = false;
		for (AbstractWidget signingWidget : this.signingWidgets) {
			signingWidget.visible = false;
		}
		for (AbstractWidget editingWidget : this.editingWidgets) {
			editingWidget.visible = true;
		}

		this.doneAndCancelButton.setMessage(Component.translatable("gui.done"));
		this.signAndFinalizeButton.setMessage(Component.translatable("book.signButton"));
		this.signAndFinalizeButton.active = true;
		this.setFocused(this.glowcaseEditBox);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		if (this.signing) {
			int x = this.glowcaseEditBox.getX();
			int y = this.glowcaseEditBox.getY();
			int width = this.glowcaseEditBox.getWidth();
			int color = NoteTextColorResource.TXT_COLOR;

			graphics.textWithWordWrap(this.font, Component.translatable("gui.glowcase.note.signing"), x, y, width, color, false);
			graphics.textWithWordWrap(this.font, Component.translatable("gui.glowcase.note.warning"), x, y + 18, width, color, false);
			graphics.textWithWordWrap(this.font, Component.translatable("gui.glowcase.note.title"), x, y + 54, width, color, false);
			graphics.textWithWordWrap(this.font, Component.translatable("gui.glowcase.note.author"), x, y + 63, width, color, false);
			graphics.textWithWordWrap(this.font, Component.translatable("gui.glowcase.note.required").withStyle(ChatFormatting.RED), x, y + 81, width, color, false);
		}
		this.extractColorPicker(graphics, mouseX, mouseY, delta);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		this.extractTransparentBackground(context);
		context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, width / 2 - BG_WIDTH / 2, height / 2 - BG_HEIGHT / 2, 0, 0, BG_WIDTH, BG_HEIGHT, BG_SIZE, BG_SIZE);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.keyPressedColorPicker(event)) return true;
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.mouseClickedColorPicker(event, doubleClick)) return true;
		return super.mouseClicked(event, doubleClick);
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

	public static TextBlockEntity.TextAlignment getTextBlockAlignment(NoteComponent.Alignment noteAlignment) {
		return switch (noteAlignment) {
			case LEFT -> TextBlockEntity.TextAlignment.LEFT;
			case RIGHT -> TextBlockEntity.TextAlignment.RIGHT;
			default -> TextBlockEntity.TextAlignment.CENTER;
		};
	}

	@Override
	public CustomPacketPayload getUpdatePayload() {
		if (finalizing) {
			// Remove insertion for optimization as it is not needed anymore
			for (int i = 0; i < lines.size(); i++) {
				String rawLine = this.glowcaseEditBox.formatTextField.getRawLineFromParsed(i);
				Component text = FormattableMultilineTextField.PARSER.parseComponent(rawLine, ParserContext.of());
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
