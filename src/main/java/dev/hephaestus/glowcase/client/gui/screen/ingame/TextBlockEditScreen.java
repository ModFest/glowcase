package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.IconButtonWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.HexColorEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.text.GlowcaseMultilineEditBox;
import dev.hephaestus.glowcase.packet.C2SEditTextBlock;
import dev.hephaestus.glowcase.util.InputFilters;
import dev.hephaestus.glowcase.util.ParseUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TextBlockEditScreen extends TextEditorScreen implements BlockEditor<TextBlockEntity> {
	private static final int INNER_PADDING = 4;
	private final TextBlockEntity textBlockEntity;

	private List<EditBox> textWidgets;

	private GlowcaseMultilineEditBox glowcaseEditBox;
	private HexColorEditBox colorEntryWidget;
	private HexColorEditBox backgroundColorEntryWidget;

	private ColorPickerWidget colorPickerWidget;

	public TextBlockEditScreen(TextBlockEntity textBlockEntity) {
		this.textBlockEntity = textBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		this.glowcaseEditBox = GlowcaseMultilineEditBox.builder(
			this.font, this.textBlockEntity.lines, 2, 24, this.width - 4, this.height - 24,
			parsedLines -> {
				this.textBlockEntity.lines = new ArrayList<>(parsedLines);
				this.textBlockEntity.rebake(true);
			}
		).build();
		// TODO - Text shadow & alignment will need to be manually updated if those options are present on this screen
		//  For now though, they are always updated here because init() is called after closing the Properties screen
		this.glowcaseEditBox.updateSettings(this.textBlockEntity.color, this.textBlockEntity.shadow, this.textBlockEntity.textAlignment);
		this.focusEditBox(); // Start ready to begin typing
		this.glowcaseEditBox.textField.seekCursor(Whence.ABSOLUTE, 0);

		int middle = width / 2;

		var scaleSlider = new TextScale.SliderWidget(textBlockEntity, middle - 203 - 5, INNER_PADDING, 113, 20);
		initFormattingButtons(middle - 90 + 6 - 5, INNER_PADDING, 0);

		this.colorPickerWidget = this.createColorPickerWidget();

		this.colorEntryWidget = HexColorEditBox.builder(this.minecraft.font, middle + 54 - 5, INNER_PADDING,
				() -> this.textBlockEntity.color, color -> {
					this.textBlockEntity.color = color;
					this.textBlockEntity.rebake(true);
					this.glowcaseEditBox.setTextColor(color);
				}
			)
			.setEditableAlpha(true)
			.setColorPickerWidget(this.colorPickerWidget)
			.build();
		this.colorEntryWidget.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.text_color")));

		this.backgroundColorEntryWidget = HexColorEditBox.builder(this.minecraft.font, middle + 54 + 64 + 6 - 5, INNER_PADDING,
				() -> this.textBlockEntity.backgroundColor, color -> {
					this.textBlockEntity.backgroundColor = color;
					this.textBlockEntity.rebake(true);
				}
			)
			.setEditableAlpha(true)
			.setColorPickerWidget(this.colorPickerWidget)
			.build();
		this.backgroundColorEntryWidget.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.background_color_argb")));

		Button moreOptionsButton = IconButtonWidget.builder(Glowcase.id("properties"), button -> {
			TextBlockOptionsScreen optionsScreen = new TextBlockOptionsScreen(this, textBlockEntity);
			Minecraft.getInstance().setScreen(optionsScreen);
		})
			.dimensions(middle + 124 + 64 + 6 - 5, INNER_PADDING, 20, 20, 16, 16)
			.build();
		moreOptionsButton.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.extra_properties")));

		this.textWidgets = List.of(
			this.colorEntryWidget,
			this.backgroundColorEntryWidget
		);

		this.addRenderableWidget(this.colorEntryWidget);
		this.addRenderableWidget(this.glowcaseEditBox);
		this.addRenderableWidget(this.backgroundColorEntryWidget);
		this.addRenderableWidget(scaleSlider);
		this.addRenderableWidget(moreOptionsButton);
	}

	@Override
	public TextBlockEntity getBlockEntity() {
		return this.textBlockEntity;
	}

	@Override
	public @Nullable CustomPacketPayload getUpdatePayload() {
		return C2SEditTextBlock.of(textBlockEntity);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		this.extractColorPicker(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		for (final var element : this.textWidgets) {
			if (element.charTyped(event)) {
				return true;
			}
		}

		return super.charTyped(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.keyPressedColorPicker(event)) return true;
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (final var text : textWidgets) {
			if (!text.mouseClicked(event, doubleClick)) {
				continue;
			}
			this.setFocused(text);
			break;
		}

		if (mouseClickedColorPicker(event, doubleClick)) return true;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public ColorPickerWidget getColorPickerWidget() {
		return this.colorPickerWidget;
	}

	@Override
	GlowcaseMultilineEditBox getGlowcaseMultilineEditBox() {
		return this.glowcaseEditBox;
	}

	public static class TextScale {
		public static final float MIN_SCALE = 0.125F;
		public static final float MAX_SCALE = 16;
		public static final float SCALE_DELTA = MAX_SCALE - MIN_SCALE;

		public static class SliderWidget extends AbstractSliderButton {
			private final TextBlockEntity entity;
			private Consumer<Float> scaleResponder;

			public SliderWidget(TextBlockEntity entity, int x, int y, int width, int height) {
				var initialValue = (entity.scale - MIN_SCALE) / SCALE_DELTA;
				super(x, y, width, height, Component.translatable("gui.glowcase.scale_value", entity.scale), initialValue);
				this.entity = entity;
			}

			@Override
			protected void updateMessage() {
				this.setMessage(Component.translatable("gui.glowcase.scale_value", entity.scale));
			}

			@Override
			protected void applyValue() {
				entity.scale = (float) Math.round(Mth.lerp(this.value, MIN_SCALE, MAX_SCALE) * 8F) / 8F;
				if (scaleResponder != null) scaleResponder.accept(entity.scale);

				entity.rebake(true);
			}

			public void setScaleResponder(Consumer<Float> responder) {
				this.scaleResponder = responder;
			}

			public void updateValue(double newValue) {
				this.value = Mth.clamp(newValue, 0.0, 1.0);
				updateMessage();
			}
		}

		public static class InputWidget extends GlowcaseEditBox {
			private Consumer<Float> scaleResponder;

			public InputWidget(TextBlockEntity entity, Font font, int x, int y, int width, int height) {
				super(font, x, y, width, height, Component.empty());
				this.setValue(String.valueOf(entity.scale));
				this.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.scale")));
				this.setFilter(InputFilters::realNumber);
				this.setResponder(input -> {
					entity.scale = (float) Math.clamp(ParseUtil.parseOrDefault(input, 1d), MIN_SCALE, MAX_SCALE);
					if (scaleResponder != null) scaleResponder.accept(entity.scale);

					entity.rebake(true);
				});
			}

			public void setScaleResponder(Consumer<Float> scaleResponder) {
				this.scaleResponder = scaleResponder;
			}
		}
	}
}
