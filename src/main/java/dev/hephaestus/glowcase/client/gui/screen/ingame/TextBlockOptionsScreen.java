package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditTextBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

public class TextBlockOptionsScreen extends GlowcaseScreen {
	private final Screen returnScreen;
	private final TextBlockEntity entity;

	public TextBlockOptionsScreen(Screen returnScreen, TextBlockEntity entity) {
		this.returnScreen = returnScreen;
		this.entity = entity;
	}

	@Override
	public void onClose() {
		super.onClose();
		C2SEditTextBlock.of(this.entity).send();
		Minecraft.getInstance().setScreen(this.returnScreen);
	}

	@Override
	public void init() {
		super.init();

		int middle = this.width / 2;

		this.addRenderableWidget(new TextBlockEditScreen.TextScaleSliderWidget(this.entity, middle - 120 - 2, 0, 120, 20));
		this.addRenderableWidget(new TextRenderDistanceSliderWidget(this.entity, middle + 2, 0, 120, 20));
	}

	private static class TextRenderDistanceSliderWidget extends AbstractSliderButton {
		private static final float INFINITE_VALUE = -1;
		private static final float MIN_VALUE = 1;
		private static final float MAX_VALUE = 256;

		private final TextBlockEntity entity;

		public TextRenderDistanceSliderWidget(TextBlockEntity entity, int x, int y, int width, int height) {
			double initialValue = entity.viewDistance == INFINITE_VALUE ? 1 : (entity.viewDistance - MIN_VALUE) / (MAX_VALUE - MIN_VALUE);
			super(x, y, width, height, createMessage(entity), initialValue);

			this.entity = entity;
		}

		private static MutableComponent createMessage(TextBlockEntity entity) {
			return entity.viewDistance == INFINITE_VALUE
				? Component.translatable("gui.glowcase.render_distance_value.infinite")
				: Component.translatable("gui.glowcase.render_distance_value", entity.viewDistance);
		}

		@Override
		protected void updateMessage() {
			this.setMessage(createMessage(this.entity));
		}

		@Override
		protected void applyValue() {
			if (this.value == 1) {
				this.entity.viewDistance = INFINITE_VALUE;
			} else {
				this.entity.viewDistance = (float) Math.round(Mth.lerp(this.value, MIN_VALUE, MAX_VALUE));
			}
			this.entity.renderDirty = true;
		}
	}
}
