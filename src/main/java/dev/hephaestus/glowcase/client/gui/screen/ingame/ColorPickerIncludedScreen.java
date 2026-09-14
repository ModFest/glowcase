package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.client.gui.widget.ingame.ColorPickerWidget;
import net.minecraft.network.chat.TextColor;

public interface ColorPickerIncludedScreen {
	ColorPickerWidget colorPickerWidget();
	void toggleColorPicker(boolean active);
	void insertHexTag(String hex);
	void insertTextColorTag(TextColor textColor);
}
