package dev.hephaestus.glowcase.client.gui.widget.ingame;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class GlowcaseEditBox extends EditBox {
	private @Nullable Filter filter = null;

	public GlowcaseEditBox(Font textRenderer, int width, int height, Component text) {
		super(textRenderer, width, height, text);
	}

	public GlowcaseEditBox(Font textRenderer, int x, int y, int width, int height, Component narration) {
		super(textRenderer, x, y, width, height, narration);
	}

	public GlowcaseEditBox(Font textRenderer, int x, int y, int width, int height, @Nullable EditBox copyFrom, Component narration) {
		super(textRenderer, x, y, width, height, copyFrom, narration);
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

	public @Nullable Filter getFilter() {
		return filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (this.filter != null
			&& !this.filter.apply(this.getValue(), event.codepoint(), this.getCursorPosition(), this.highlightPos)
		) return false;

		return super.charTyped(event);
	}

	@FunctionalInterface
	public interface Filter {
		boolean apply(String currentValue, final int newChar, final int cursorPos, final int highlightPos);
	}
}
