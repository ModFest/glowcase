package dev.hephaestus.glowcase.client.gui.widget.ingame;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class GlowcaseEditBox extends EditBox {
	private Filter filter = null;

	public GlowcaseEditBox(Font textRenderer, int width, int height, Component text) {
		super(textRenderer, width, height, text);
	}

	public GlowcaseEditBox(Font textRenderer, int x, int y, int width, int height, Component text) {
		super(textRenderer, x, y, width, height, text);
	}

	public GlowcaseEditBox(Font textRenderer, int x, int y, int width, int height, @Nullable EditBox copyFrom, Component text) {
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

	public Filter getFilter() {
		return filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (filter != null && !filter.apply(this.getValue(), event.codepoint(), this.getCursorPosition())) return false;

		return super.charTyped(event);
	}

	@FunctionalInterface
	public interface Filter {
		boolean apply(String currentValue, final int newChar, final int cursorPos);
	}
}
