package dev.hephaestus.glowcase.client.gui.screen.ingame;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class GlowcaseScreen extends Screen {
	protected GlowcaseScreen() {
		super(Component.empty());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
		this.extractTransparentBackground(graphics);
		this.extractBlurredBackground(graphics);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
