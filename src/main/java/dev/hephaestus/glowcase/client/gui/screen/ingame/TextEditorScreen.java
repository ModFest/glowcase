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

	abstract GlowcaseMultilineEditBox getGlowcaseMultilineEditBox();

	public void focusEditBox() {
		this.setFocused(this.getGlowcaseMultilineEditBox());
	}

	@Override
	protected void setInitialFocus() { // The edit box is the most likely thing to be used first, so focus it first
		// FIXME - Uncomment this once note screen is ready, it crashes for now lol
//		this.setInitialFocus(this.getGlowcaseMultilineEditBox());
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
				// FIXME - This doesn't work because #clickedColorPicker() focuses the color picker right after #mouseClicked(),
				//  and attempting to move it beforehand breaks *a lot* of stuff
				//  Will probably need a skipFocus thing in the color picker
				this.focusEditBox();
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
	public void insertTag(String tagName) {
		this.getGlowcaseMultilineEditBox().insertTag(tagName);
	}
}
