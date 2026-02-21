package dev.hephaestus.glowcase.client.gui.screen.ingame;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class GlowcaseScreen extends Screen {
	protected GlowcaseScreen() {
		super(Component.empty());
	}

	@Override
	public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
		this.renderTransparentBackground(context);
		context.blurBeforeThisStratum();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
