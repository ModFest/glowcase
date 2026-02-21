package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.client.gui.widget.ingame.ColorPickerWidget;
import net.minecraft.ChatFormatting;

public interface ColorPickerIncludedScreen {
	ColorPickerWidget colorPickerWidget();
	void toggleColorPicker(boolean active);
	void insertHexTag(String hex);
	void insertFormattingTag(ChatFormatting formatting);
}
