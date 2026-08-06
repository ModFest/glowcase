package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.IconButtonWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.HexColorEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.tab.GlowcaseTab;
import dev.hephaestus.glowcase.client.gui.widget.ingame.tab.GlowcaseTabNavBar;
import dev.hephaestus.glowcase.client.gui.widget.ingame.text.GlowcaseMultilineEditBox;
import dev.hephaestus.glowcase.packet.C2SEditTextBlock;
import dev.hephaestus.glowcase.util.InputFilters;
import dev.hephaestus.glowcase.util.ParseUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
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
			this.font, this.textBlockEntity.lines, 2, 25, this.width - 4, this.height - 28,
			parsedLines -> {
				this.textBlockEntity.lines = new ArrayList<>(parsedLines);
				this.textBlockEntity.rebake(true);
			}
		).build();
		// TODO - Text shadow & alignment will need to be manually updated if those options are present on this screen
		//  For now though, they are always updated here because init() is called after closing the Properties screen
		this.glowcaseEditBox.updateSettings(this.textBlockEntity.color, this.textBlockEntity.shadow, this.textBlockEntity.textAlignment);
		this.glowcaseEditBox.textField.seekCursor(Whence.ABSOLUTE, 0);
		this.addRenderableWidget(this.glowcaseEditBox);

		this.colorPickerWidget = this.createColorPickerWidget();

		List<AbstractWidget> editTabWidgets = this.initEditTabWidgets();
		List<AbstractWidget> viewTabWidgets = this.initViewTabWidgets();
		List<AbstractWidget> miscTabWidgets = this.initMiscTabWidgets();

		GlowcaseTabNavBar tabNavBar = GlowcaseTabNavBar.builder(
			this.width, height -> {
				this.glowcaseEditBox.setY(height + 3);
			})
			.setY(2)
			.setWidgetAreaPadding(2)
			.addTabs(
				new GlowcaseTab(
					Component.literal("Edit"), 20,
					editTabWidgets
				),
				new GlowcaseTab(
					Component.literal("View"), 20,
					viewTabWidgets
				),
				new GlowcaseTab(
					Component.literal("Misc"), 20,
					miscTabWidgets
				)
			).build();
		this.addRenderableWidget(tabNavBar);
	}

	public List<AbstractWidget> initEditTabWidgets() {
		// nav bar y padding (2) + tab button height (20) + widget area padding (1) = 23;
		int firstRowY = 23;

		TextScale.SliderWidget scaleSlider = new TextScale.SliderWidget(textBlockEntity, 0, 0, 113, 20);

		initFormattingButtons(0, 0, 0);

		this.colorEntryWidget = HexColorEditBox.builder(this.minecraft.font, 0, 0,
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

		this.backgroundColorEntryWidget = HexColorEditBox.builder(this.minecraft.font, 0, 0,
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
			.dimensions(0, 0, 20, 20, 16, 16)
			.build();
		moreOptionsButton.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.extra_properties")));

		this.textWidgets = List.of(
			this.colorEntryWidget,
			this.backgroundColorEntryWidget
		);

		this.addRenderableWidget(scaleSlider);
		this.addRenderableWidget(this.colorEntryWidget);
		this.addRenderableWidget(this.backgroundColorEntryWidget);

		LinearLayout editTabLayout = LinearLayout.horizontal().spacing(2);
		editTabLayout.addChild(scaleSlider, LayoutSettings.defaults().paddingRight(6));
		for (Button formattingButton : this.formattingButtons) {
			editTabLayout.addChild(formattingButton);
		}
		editTabLayout.addChild(this.colorEntryWidget, LayoutSettings.defaults().paddingLeft(6));
		editTabLayout.addChild(this.backgroundColorEntryWidget, LayoutSettings.defaults().paddingLeft(4));

		editTabLayout.arrangeElements(); // Setup initial positioning
		FrameLayout.centerInRectangle(editTabLayout, 0, firstRowY, this.width, firstRowY); // Finish positioning

		List<AbstractWidget> editTabWidgets = new ArrayList<>(this.formattingButtons);
		editTabWidgets.add(scaleSlider);
		editTabWidgets.add(this.colorEntryWidget);
		editTabWidgets.add(this.backgroundColorEntryWidget);
		return editTabWidgets;
	}

	public List<AbstractWidget> initViewTabWidgets() {
		int firstRowY = 23;

		CycleButton<TextBlockEntity.TextAlignment> textAlignmentButton = CycleButton.builder(
				alignment -> Component.literal(alignment.toString()),
				this.textBlockEntity.textAlignment
			)
			.withValues(
				// not `.values()` to not have CENTER_LEFT or CENTER_RIGHT, unless they get removed
				TextBlockEntity.TextAlignment.CENTER,
				TextBlockEntity.TextAlignment.LEFT,
				TextBlockEntity.TextAlignment.RIGHT
			)
			.create(
				Component.translatable("gui.glowcase.text_alignment"),
				(button, alignment) -> {
					this.textBlockEntity.textAlignment = alignment;
					this.textBlockEntity.rebake(true);
				}
			);

		CycleButton<TextBlockEntity.HorizontalAlignment> horizontalAnchorButton = CycleButton.builder(
				alignment -> Component.literal(alignment.toString()),
				this.textBlockEntity.horizontalAlignment
			)
			.withValues(TextBlockEntity.HorizontalAlignment.values())
			.create(
				Component.translatable("gui.glowcase.x_offset_label"),
				(button, alignment) -> {
					this.textBlockEntity.horizontalAlignment = alignment;
					this.textBlockEntity.rebake(true);
				}
			);

		CycleButton<TextBlockEntity.ZOffset> zOffsetButton = CycleButton.builder(
				offset -> Component.literal(offset.toString()),
				this.textBlockEntity.zOffset
			)
			.withValues(TextBlockEntity.ZOffset.values())
			.create(
				Component.translatable("gui.glowcase.z_offset_label"),
				(button, offset) -> {
					this.textBlockEntity.zOffset = offset;
					this.textBlockEntity.rebake(true);
				}
			);


		this.addRenderableWidget(textAlignmentButton);
		this.addRenderableWidget(horizontalAnchorButton);
		this.addRenderableWidget(zOffsetButton);

		LinearLayout viewTabFirstRowLayout = LinearLayout.horizontal().spacing(2);
		viewTabFirstRowLayout.addChild(textAlignmentButton);
		viewTabFirstRowLayout.addChild(horizontalAnchorButton);
		viewTabFirstRowLayout.addChild(zOffsetButton);

		viewTabFirstRowLayout.arrangeElements(); // Setup initial positioning
		FrameLayout.centerInRectangle(viewTabFirstRowLayout, 0, firstRowY, this.width, firstRowY); // Finish positioning

		return List.of(textAlignmentButton, horizontalAnchorButton, zOffsetButton);
	}

	public List<AbstractWidget> initMiscTabWidgets() {
		int firstRowY = 23;
		CycleButton<Boolean> textShadowButton = CycleButton.onOffBuilder(this.textBlockEntity.shadow).create(
			Component.translatable("gui.glowcase.text_shadow"),
			(button, shadow) -> {
				this.textBlockEntity.shadow = shadow;
				this.textBlockEntity.rebake(true);
			}
		);

		this.addRenderableWidget(textShadowButton);

		LinearLayout miscTabLayout = LinearLayout.horizontal().spacing(4);
		miscTabLayout.addChild(textShadowButton);

		miscTabLayout.arrangeElements(); // Setup initial positioning
		FrameLayout.centerInRectangle(miscTabLayout, 0, firstRowY, this.width, firstRowY); // Finish positioning

		return List.of(textShadowButton);
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

	@Override
	public TextBlockEntity getBlockEntity() {
		return this.textBlockEntity;
	}

	@Override
	public @Nullable CustomPacketPayload getUpdatePayload() {
		return C2SEditTextBlock.of(textBlockEntity);
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
