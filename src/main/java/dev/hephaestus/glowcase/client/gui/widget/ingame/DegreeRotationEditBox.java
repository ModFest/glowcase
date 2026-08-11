package dev.hephaestus.glowcase.client.gui.widget.ingame;

import dev.hephaestus.glowcase.util.InputFilters;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * An Edit Box which only accepts numbers, and appends the degree symbol° at the end of the value.<br><br>
 * Additionally, it is the current home of the Creature of Whimsy °0.0°
 * @author Superkat32
 */
public class DegreeRotationEditBox extends GlowcaseEditBox {
	private static final WidgetSprites SPRITES = new WidgetSprites(
		Identifier.withDefaultNamespace("widget/text_field"), Identifier.withDefaultNamespace("widget/text_field_highlighted")
	);
	private static final String DEGREE_SYMBOL = "°";
	private static final String[] CREATURE_OF_WHIMSY = {"0.0", "-.-", ">.>", "<.<", "°0.0°", "°-.-°", "°0w0°", "°-w-°", "°^w^°"};
	private static final int IDLE_TIME_UNTIL_CREATURE_OF_WHIMSY_VISITS = 7000;

	private final Font font;
	private int textColor;
	private long idleTime = 0;
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
		long timeIdling = Util.getMillis() - this.idleTime;
		if (this.isFocused() && this.getValue().equals("0.0") && timeIdling >= IDLE_TIME_UNTIL_CREATURE_OF_WHIMSY_VISITS) {
			timeIdling -= IDLE_TIME_UNTIL_CREATURE_OF_WHIMSY_VISITS;
			if (this.isBordered()) { // Draw background from super.extractWidgetRenderState()
				Identifier sprite = SPRITES.get(this.isActive(), this.isFocused());
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
			}

			int face;
			if (timeIdling <= 500) {
				face = Mth.floor((float) (timeIdling % 500) / 400);
			} else if (timeIdling <= 5000) {
				face = Mth.floor((float) (timeIdling % 4000) / 3900);
			} else if (timeIdling <= 9000) {
				face = Mth.floor((float) ((timeIdling - 5000) % 2000) / 1200) + 2;
			} else if (timeIdling <= 10850) {
				face = timeIdling <= 10000 ? 0 : 1;
			} else if (timeIdling <= 16650) {
				face = Mth.floor((float) (timeIdling % 4000) / 3900) + 4;
			} else {
				if ((timeIdling - 16650) % 10000 <= 3000) {
					face = 8;
				} else {
					face = Mth.floor((float) (timeIdling % 4000) / 3900) + 6;
				}
			}

			String creatureOfWhimsy = CREATURE_OF_WHIMSY[face];
			int textX = this.getX() + (this.isBordered() ? 4 : 0);
			int textY = this.isBordered() ? this.getY() + (this.getHeight() - 8) / 2 : this.getY();
			graphics.text(this.font, creatureOfWhimsy, textX, textY, this.textColor);
			return;
		}

		super.extractWidgetRenderState(graphics, mouseX, mouseY, a);

		if (this.isFocused() || this.getValue().isBlank()) return;
		int textX = this.getX() + this.font.width(this.getValue().substring(this.displayPos)) + 4;
		int textY = this.isBordered() ? this.getY() + (this.getHeight() - 8) / 2 : this.getY();
		graphics.text(this.font, DEGREE_SYMBOL, textX, textY, this.textColor);
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (focused) {
			this.idleTime = Util.getMillis();
		}
	}

	@Override
	public void setValue(String value) {
		super.setValue(value);
		this.idleTime = Util.getMillis();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		this.idleTime = Util.getMillis();
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		this.idleTime = Util.getMillis();
		return super.charTyped(event);
	}
}
