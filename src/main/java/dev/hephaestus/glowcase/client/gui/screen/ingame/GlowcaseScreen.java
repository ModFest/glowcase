package dev.hephaestus.glowcase.client.gui.screen.ingame;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class GlowcaseScreen extends Screen {
	protected GlowcaseScreen() {
		super(Text.empty());
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		this.renderInGameBackground(context);
		context.applyBlur();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
