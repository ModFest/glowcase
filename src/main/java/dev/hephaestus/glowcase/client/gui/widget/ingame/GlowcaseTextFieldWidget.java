package dev.hephaestus.glowcase.client.gui.widget.ingame;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

public class GlowcaseTextFieldWidget extends TextFieldWidget {
	public GlowcaseTextFieldWidget(TextRenderer textRenderer, int width, int height, Text text) {
		super(textRenderer, width, height, text);
	}

	public GlowcaseTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
		super(textRenderer, x, y, width, height, text);
	}

	public GlowcaseTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, @Nullable TextFieldWidget copyFrom, Text text) {
		super(textRenderer, x, y, width, height, copyFrom, text);
	}

	@Override
	public void setFocused(boolean focused) {
		boolean wasFocused = isFocused();
		super.setFocused(focused);
		if (focused != wasFocused) {
			this.onChanged(this.getText());
		}
	}

	@Override
	public void setPlaceholder(Text placeholder) {
		super.setPlaceholder(placeholder.copy().formatted(Formatting.GRAY, Formatting.ITALIC));
	}
}
