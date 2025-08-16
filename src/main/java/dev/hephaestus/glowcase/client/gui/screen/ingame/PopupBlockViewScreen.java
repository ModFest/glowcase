package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.PopupBlockEntity;
import net.minecraft.client.gui.DrawContext;

//TODO: multi-character selection at some point? it may be a bit complex but it'd be nice
public class PopupBlockViewScreen extends GlowcaseScreen {
	private final PopupBlockEntity popupBlockEntity;

	public PopupBlockViewScreen(PopupBlockEntity popupBlockEntity) {
		this.popupBlockEntity = popupBlockEntity;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if (this.client != null) {
			super.render(context, mouseX, mouseY, delta);

			context.getMatrices().pushMatrix();
			context.getMatrices().translate(0, 40 + 2 * this.width / 100F);
			for (int i = 0; i < this.popupBlockEntity.lines.size(); ++i) {
				var text = this.popupBlockEntity.lines.get(i);

				int lineWidth = this.textRenderer.getWidth(text);
				switch (this.popupBlockEntity.textAlignment) {
					case LEFT -> context.drawTextWithShadow(client.textRenderer, text, this.width / 10, i * 12, this.popupBlockEntity.color);
					case CENTER -> context.drawTextWithShadow(client.textRenderer, text, this.width / 2 - lineWidth / 2, i * 12, this.popupBlockEntity.color);
					case RIGHT -> context.drawTextWithShadow(client.textRenderer, text, this.width - this.width / 10 - lineWidth, i * 12, this.popupBlockEntity.color);
				}
			}

			context.getMatrices().popMatrix();
		}
	}
}
