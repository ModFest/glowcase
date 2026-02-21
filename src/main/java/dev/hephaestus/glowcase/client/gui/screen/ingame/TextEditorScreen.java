package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.hephaestus.glowcase.client.gui.widget.ingame.ColorPickerWidget;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import eu.pb4.placeholders.api.parsers.tag.TextTag;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.network.chat.Component;

public abstract class TextEditorScreen extends GlowcaseScreen implements ColorPickerIncludedScreen {
	private Button colorText;
	private Button[] widgets = new Button[0];

	abstract TextFieldHelper getSelectionManager();

	protected void addFormattingButtons(int x, int y, int innerPadding, int buttonSize, int buttonPadding) {
		int buttonX = x + innerPadding * 2; //adding numbers to this variable because I personally find that more readable, that's all
		int buttonY = y + innerPadding; //reduce the times this is calculated
		Button boldText = Button.builder(Component.literal("B").withStyle(ChatFormatting.BOLD), action -> {
			insertTag(TagRegistry.SAFE.getTag("bold"), true);
		}).bounds(buttonX, buttonY, buttonSize, buttonSize).build();

		buttonX += buttonSize + buttonPadding;
		Button italicizeText = Button.builder(Component.literal("I").withStyle(ChatFormatting.ITALIC), action -> {
			insertTag(TagRegistry.SAFE.getTag("italic"), true);
		}).bounds(buttonX, buttonY, buttonSize, buttonSize).build();

		buttonX += buttonSize + buttonPadding;
		Button strikeText = Button.builder(Component.literal("S").withStyle(ChatFormatting.STRIKETHROUGH), action -> {
			insertTag(TagRegistry.SAFE.getTag("strikethrough"), true);
		}).bounds(buttonX, buttonY, buttonSize, buttonSize).build();

		buttonX += buttonSize + buttonPadding;
		Button underlineText = Button.builder(Component.literal("U").withStyle(ChatFormatting.UNDERLINE), action -> {
			insertTag(TagRegistry.SAFE.getTag("underline"), true);
		}).bounds(buttonX, buttonY, buttonSize, buttonSize).build();

		buttonX += buttonSize + buttonPadding;
		//not using the actual obfuscated formatting here because the movement can be annoying
		Button obfuscateText = Button.builder(Component.literal("@"), action -> {
			insertTag(TagRegistry.SAFE.getTag("obfuscated"), true);
		}).bounds(buttonX, buttonY, buttonSize, buttonSize).build();

		buttonX += buttonSize + buttonPadding; // + 4? (only works on padding of 2)
		this.colorText = Button.builder(Component.literal("\uD83D\uDD8C"), action -> {
			ColorPickerWidget colorPickerWidget = colorPickerWidget();
			colorPickerWidget.setPosition(216, 10);
			colorPickerWidget.setTargetElement(this.colorText);
			colorPickerWidget.setOnAccept(picker -> {
				picker.insertColor(picker.color);
				picker.toggle(false);
			});
			colorPickerWidget.setOnCancel(picker -> picker.toggle(false));
			colorPickerWidget.setPresetListener((color, formatting) -> {
				if(formatting != null) {
					insertFormattingTag(formatting);
				} else {
					insertHexTag(ColorPickerWidget.getHexCode(color));
				}
				this.toggleColorPicker(false);
			});
			colorPickerWidget.setChangeListener(null);
			toggleColorPicker(!colorPickerWidget.active);
		}).bounds(buttonX, buttonY, buttonSize, buttonSize).build();

		widgets = new Button[]{
			boldText, italicizeText, strikeText, underlineText, obfuscateText, colorText
		};

		this.addRenderableWidget(boldText);
		this.addRenderableWidget(italicizeText);
		this.addRenderableWidget(strikeText);
		this.addRenderableWidget(underlineText);
		this.addRenderableWidget(obfuscateText);
		this.addRenderableWidget(colorText);
	}

	public void toggleWidgets(boolean active) {
		for (Button widget : widgets)
			widget.active = active;
	}

	public void insertTag(TextTag tag, boolean findShortest) {
		if(tag == null) return;
		//find the alias with the least amount of characters
		String name = tag.name();
		if(findShortest && tag.aliases().length > 1) {
			String shortest = Arrays.stream(tag.aliases()).min(Comparator.comparing(String::length)).get();
			name = Arrays.stream(tag.aliases()).min(Comparator.comparing(String::length)).get();
		}

		TextFieldHelper selectionManager = getSelectionManager();

		int selectedStart = selectionManager.getCursorPos();
		int selectedEnd = selectionManager.getSelectionPos();
		if(selectedStart != selectedEnd) {
			int selectedAmount = Math.abs(selectedEnd - selectedStart);
			//text is selected/highlighted - selection is determined based on the direction it happens, so an extra check is needed
			selectionManager.moveBy(selectedStart < selectedEnd ? 0 : -selectedAmount, false, TextFieldHelper.CursorStep.CHARACTER);
			selectionManager.insertText("<" + name + ">");
			selectionManager.moveBy(selectedAmount, false, TextFieldHelper.CursorStep.CHARACTER);
			selectionManager.insertText("</" + name + ">");
			selectionManager.moveBy(-name.length() - 3, false, TextFieldHelper.CursorStep.CHARACTER);
			selectionManager.setSelectionRange(selectedStart + name.length() + 2, selectedEnd + name.length() + 2);
		} else {
			selectionManager.insertText("<" + name + "></" + name + ">");
			selectionManager.moveBy(-name.length() - 3, false, TextFieldHelper.CursorStep.CHARACTER);
		}
	}

	@Override
	public void insertHexTag(String hex) {
		TextFieldHelper selectionManager = getSelectionManager();
		int selectedStart = selectionManager.getCursorPos();
		int selectedEnd = selectionManager.getSelectionPos();
		if(selectedStart != selectedEnd) {
			int selectedAmount = Math.abs(selectedEnd - selectedStart);
			//text is selected/highlighted - selection is determined based on the direction it happens, so an extra check is needed
			selectionManager.moveBy(selectedStart < selectedEnd ? 0 : -selectedAmount, false, TextFieldHelper.CursorStep.CHARACTER);
			selectionManager.insertText("<" + hex + ">");
			selectionManager.moveBy(selectedAmount, false, TextFieldHelper.CursorStep.CHARACTER);
			selectionManager.insertText("</" + hex + ">");
			selectionManager.moveBy(-hex.length() - 3, false, TextFieldHelper.CursorStep.CHARACTER);
			selectionManager.setSelectionRange(selectedStart + hex.length() + 2, selectedEnd + hex.length() + 2);
		} else {
			selectionManager.insertText("<" + hex + "></" + hex + ">");
			selectionManager.moveBy(-hex.length() - 3, false, TextFieldHelper.CursorStep.CHARACTER);
		}
	}

	@Override
	public void insertFormattingTag(ChatFormatting formatting) {
		insertTag(TagRegistry.SAFE.getTag(formatting.getName()), false);
	}
}
