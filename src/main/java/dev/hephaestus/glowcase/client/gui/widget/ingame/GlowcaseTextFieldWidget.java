package dev.hephaestus.glowcase.client.gui.widget.ingame;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class GlowcaseTextFieldWidget extends EditBox {
	public GlowcaseTextFieldWidget(Font textRenderer, int width, int height, Component text) {
		super(textRenderer, width, height, text);
	}

	public GlowcaseTextFieldWidget(Font textRenderer, int x, int y, int width, int height, Component text) {
		super(textRenderer, x, y, width, height, text);
	}

	public GlowcaseTextFieldWidget(Font textRenderer, int x, int y, int width, int height, @Nullable EditBox copyFrom, Component text) {
		super(textRenderer, x, y, width, height, copyFrom, text);
	}

	@Override
	public void setFocused(boolean focused) {
		boolean wasFocused = isFocused();
		super.setFocused(focused);
		if (focused != wasFocused) {
			this.onValueChange(this.getValue());
		}
	}

	@Override
	public void setHint(Component placeholder) {
		super.setHint(placeholder.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}
}
