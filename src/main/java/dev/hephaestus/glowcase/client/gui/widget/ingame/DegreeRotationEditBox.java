package dev.hephaestus.glowcase.client.gui.widget.ingame;

import dev.hephaestus.glowcase.util.InputFilters;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * An Edit Box which only accepts numbers, and appends the degree symbol° at the end of the value.
 */
public class DegreeRotationEditBox extends GlowcaseEditBox {
	private static final String DEGREE_SYMBOL = "°";
	private final Font font;
	private int textColor;
	public DegreeRotationEditBox(Font font, int x, int y, int width, int height, Component text) {
		super(font, x, y, width, height, text);
		this.font = font;
		this.setFilter(InputFilters::realNumber);
	}

	@Override
	public void setTextColor(int textColor) {
		super.setTextColor(textColor);
		this.textColor = textColor;
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractWidgetRenderState(graphics, mouseX, mouseY, a);

		if (this.isFocused() || this.getValue().isBlank()) return;
		int textX = this.getX() + this.font.width(this.getValue().substring(this.displayPos)) + 4;
		int textY = this.isBordered() ? this.getY() + (this.getHeight() - 8) / 2 : this.getY();
		graphics.text(this.font, DEGREE_SYMBOL, textX, textY, this.textColor);
	}
}
