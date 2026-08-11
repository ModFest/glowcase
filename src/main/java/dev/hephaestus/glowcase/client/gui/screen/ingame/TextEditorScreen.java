package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.gui.widget.ingame.IconButtonWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.text.GlowcaseMultilineEditBox;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import eu.pb4.placeholders.api.parsers.tag.TextTag;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public abstract class TextEditorScreen extends EditorScreen implements ColorPickerIncludedScreen, TagFormatIncludedScreen {
	public static final Identifier COLOR_TEXT_ICON = Glowcase.id("color_text");

	protected Button colorTextButton;
	protected List<Button> formattingButtons;
	protected boolean queuedEditBoxFocus = false;

	abstract GlowcaseMultilineEditBox getGlowcaseMultilineEditBox();

	public void focusEditBox() {
		this.setFocused(this.getGlowcaseMultilineEditBox());
	}

	// Queue the GlowcaseEditBox to be focused, intended for other widgets to call from the mouseClicked method.
	// Widgets are focused after they return true in mouseClicked, making it impossible for them to focus the edit box.
	// The queue is checked for on mouseRelease
	public void queueFocusEditBox() {
		this.queuedEditBoxFocus = true;
	}

	@Override
	protected void setInitialFocus() {
		// The edit box is the most likely thing to be used first, so focus it first
		this.setInitialFocus(this.getGlowcaseMultilineEditBox());
	}

	protected void initFormattingButtons(int x, int y, int innerPadding) {
		this.initFormattingButtons(x, y, innerPadding, 20, 2);
	}

	protected void initFormattingButtons(int x, int y, int innerPadding, int buttonSize, int buttonPadding) {
		int buttonX = x + innerPadding * 2;
		int buttonY = y + innerPadding;
		int shiftX = buttonSize + buttonPadding;

		Button boldText = Button.builder(Component.literal("B").withStyle(ChatFormatting.BOLD), button -> this.insertBoldTag())
			.bounds(buttonX, buttonY, buttonSize, buttonSize)
			.tooltip(Tooltip.create(Component.translatable("gui.glowcase.bold")))
			.build();

		Button italicText = Button.builder(Component.literal("I").withStyle(ChatFormatting.ITALIC), button -> this.insertItalicTag())
			.bounds(buttonX + shiftX, buttonY, buttonSize, buttonSize)
			.tooltip(Tooltip.create(Component.translatable("gui.glowcase.italic")))
			.build();

		Button strikeText = Button.builder(Component.literal("S").withStyle(ChatFormatting.STRIKETHROUGH), button -> this.insertStrikethroughTag())
			.bounds(buttonX + shiftX * 2, buttonY, buttonSize, buttonSize)
			.tooltip(Tooltip.create(Component.translatable("gui.glowcase.strikethrough")))
			.build();

		Button underlineText = Button.builder(Component.literal("U").withStyle(ChatFormatting.UNDERLINE), button -> this.insertUnderlineTag())
			.bounds(buttonX + shiftX * 3, buttonY, buttonSize, buttonSize)
			.tooltip(Tooltip.create(Component.translatable("gui.glowcase.underline")))
			.build();

		// Don't use actual obfuscated text as its movement is distracting
		Button obfuscateText = Button.builder(Component.literal("@"), button -> this.insertObfuscatedTag())
			.bounds(buttonX + shiftX * 4, buttonY, buttonSize, buttonSize)
			.tooltip(Tooltip.create(Component.translatable("gui.glowcase.obfuscate")))
			.build();

		this.colorTextButton = IconButtonWidget.builder(COLOR_TEXT_ICON, button -> {
			ColorPickerWidget colorPickerWidget = this.getColorPickerWidget();
			colorPickerWidget.target(this.colorTextButton, ColorUtil.RED, false, true, pickedColor -> {});
			colorPickerWidget.setConfirmListener(this::insertColorHexTag);
			colorPickerWidget.setPresetListener(preset -> {
				if (preset.getPresetFormatting() != null) this.insertFormattingTag(preset.getPresetFormatting());
				colorPickerWidget.hide();
			});
		}).dimensions(buttonX + shiftX * 5, buttonY, buttonSize, buttonSize, 10, 10).build();
		this.colorTextButton.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.color_text")));

		this.formattingButtons = List.of(boldText, italicText, strikeText, underlineText, obfuscateText, this.colorTextButton);
		for (Button formattingButton : formattingButtons) {
			this.addRenderableWidget(formattingButton);
		}
	}

	protected void toggleFormattingButtons(boolean active) {
		for (Button formattingButton : formattingButtons) {
			formattingButton.active = active;
		}
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean released = super.mouseReleased(event);
		// Do after current focused widget mouseReleased to prevent drag desync issues
		// Also doing it on mouseReleased is convenient since extending classes usually don't override this
		this.checkQueuedEditBoxFocus();
		return released;
	}

	public void checkQueuedEditBoxFocus() {
		if (this.queuedEditBoxFocus) {
			if (this.getFocused() != null) this.focusEditBox();
			this.queuedEditBoxFocus = false;
		}
	}

	@Override
	public void insertTag(String tagName) {
		this.getGlowcaseMultilineEditBox().insertTag(tagName);
		// The edit box is almost certainly going to want to be focused after a tag insert, so may as well queue it here
		// Keyboard shortcuts aren't a worry here as they are handled within the edit box
		this.queueFocusEditBox();
	}
}
