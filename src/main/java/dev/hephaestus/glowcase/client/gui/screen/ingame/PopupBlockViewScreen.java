package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.PopupBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.text.MultilineTextViewArea;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class PopupBlockViewScreen extends GlowcaseScreen {
	private final PopupBlockEntity popupBlockEntity;
	private MultilineTextViewArea textViewArea;

	public PopupBlockViewScreen(PopupBlockEntity popupBlockEntity) {
		this.popupBlockEntity = popupBlockEntity;
	}

	@Override
	protected void init() {
		int innerPadding = this.width / 100;
		this.textViewArea = new MultilineTextViewArea(
			this.font, this.popupBlockEntity.lines,
			2, 40 + innerPadding,
			this.width - 4, this.height - 40 - innerPadding,
			this.popupBlockEntity.color, this.popupBlockEntity.textAlignment
		);

		this.addRenderableWidget(this.textViewArea);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		if (!this.popupBlockEntity.viewScreenTitle || this.popupBlockEntity.title.isBlank()) return;
		int titleWidth = this.font.width(this.popupBlockEntity.title);
		graphics.text(
			this.font, this.popupBlockEntity.title,
			this.width / 2 - titleWidth / 2, 16,
			this.popupBlockEntity.color
		);

		int breakWidth = Math.min(titleWidth + 16, this.width / 2 - 16);
		graphics.fill(this.width / 2 - breakWidth, 27, this.width / 2 + breakWidth, 28, this.popupBlockEntity.color);
	}
}
