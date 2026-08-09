package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.AnchorPositionGridWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.IconButtonWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.Vec3FieldsWidget;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TextBlockEditScreen extends TextEditorScreen implements BlockEditor<TextBlockEntity> {
	private static final int INNER_PADDING = 4;
	private final TextBlockEntity textBlockEntity;

	private GlowcaseMultilineEditBox glowcaseEditBox;
	private HexColorEditBox colorEntryWidget;
	private HexColorEditBox backgroundColorEntryWidget;

	private ColorPickerWidget colorPickerWidget;
	private Button zFrontButton;
	private Button zCenterButton;
	private Button zBackButton;
	private IconButtonWidget justifyLeftButton;
	private IconButtonWidget justifyCenterButton;
	private IconButtonWidget justifyRightButton;

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
		this.glowcaseEditBox.updateSettings(this.textBlockEntity.color, this.textBlockEntity.shadow, this.textBlockEntity.textAlignment);
		this.glowcaseEditBox.textField.seekCursor(Whence.ABSOLUTE, 0);
		this.addRenderableWidget(this.glowcaseEditBox);

		this.colorPickerWidget = this.createColorPickerWidget();

		List<AbstractWidget> editTabWidgets = this.initEditTabWidgets();
		List<AbstractWidget> viewTabWidgets = this.initViewTabWidgets();

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
					Component.literal("View"), 42,
					viewTabWidgets
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
		int firstRowY = 24;
		int secondRowY = firstRowY + 22;

		this.justifyLeftButton = IconButtonWidget.builder(
				Glowcase.id("text_alignment/left"), button -> {
					this.textBlockEntity.textAlignment = TextBlockEntity.TextAlignment.LEFT;
					this.textBlockEntity.rebake(true);
					this.glowcaseEditBox.setTextAlignment(TextBlockEntity.TextAlignment.LEFT);
					this.updateSelectedJustifyButton();
				})
				.dimensions(0, 0, 20, 20, 16, 16)
				.build();
		this.justifyLeftButton.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.justify_left")));
		this.justifyCenterButton = IconButtonWidget.builder(
				Glowcase.id("text_alignment/center"), button -> {
					this.textBlockEntity.textAlignment = TextBlockEntity.TextAlignment.CENTER;
					this.textBlockEntity.rebake(true);
					this.glowcaseEditBox.setTextAlignment(TextBlockEntity.TextAlignment.CENTER);
					this.updateSelectedJustifyButton();
				})
			.dimensions(0, 0, 20, 20, 16, 16)
			.build();
		this.justifyCenterButton.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.justify_center")));
		this.justifyRightButton = IconButtonWidget.builder(
				Glowcase.id("text_alignment/right"), button -> {
					this.textBlockEntity.textAlignment = TextBlockEntity.TextAlignment.RIGHT;
					this.textBlockEntity.rebake(true);
					this.glowcaseEditBox.setTextAlignment(TextBlockEntity.TextAlignment.RIGHT);
					this.updateSelectedJustifyButton();
				})
			.dimensions(0, 0, 20, 20, 16, 16)
			.build();
		this.justifyRightButton.setTooltip(Tooltip.create(Component.translatable("gui.glowcase.justify_right")));
		this.updateSelectedJustifyButton();

		this.zFrontButton = Button.builder(Component.translatable("gui.glowcase.front"), button -> {
			this.textBlockEntity.zOffset = TextBlockEntity.ZOffset.FRONT;
			this.textBlockEntity.rebake(true);
			this.updateSelectedZButton();
		}).size(50, 20).build();
		this.zCenterButton = Button.builder(Component.translatable("gui.glowcase.center"), button -> {
			this.textBlockEntity.zOffset = TextBlockEntity.ZOffset.CENTER;
			this.textBlockEntity.rebake(true);
			this.updateSelectedZButton();
		}).size(50, 20).build();
		this.zBackButton = Button.builder(Component.translatable("gui.glowcase.back"), button -> {
			this.textBlockEntity.zOffset = TextBlockEntity.ZOffset.BACK;
			this.textBlockEntity.rebake(true);
			this.updateSelectedZButton();
		}).size(50, 20).build();
		this.updateSelectedZButton();

		AnchorPositionGridWidget anchorGrid = new AnchorPositionGridWidget(0, 0, this.textBlockEntity.horizontalAlignment, anchor -> {
			if (anchor.getY() == 0) { // TODO - temp, add remaining anchors to block
				this.textBlockEntity.horizontalAlignment = TextBlockEntity.HorizontalAlignment.values()[anchor.getX() + 1];
				this.textBlockEntity.rebake(true);
			}
		});

		CycleButton<Boolean> textShadowButton = CycleButton.onOffBuilder(this.textBlockEntity.shadow).create(
			Component.translatable("gui.glowcase.text_shadow"),
			(button, shadow) -> {
				this.textBlockEntity.shadow = shadow;
				this.textBlockEntity.rebake(true);
				this.glowcaseEditBox.setTextShadow(shadow);
			}
		);
		textShadowButton.setWidth(100);

		Button insertFontButton = IconButtonWidget.builder(Component.literal("Aa"), button -> {})
			.bounds(0, 0, 20, 20)
			.tooltip(Tooltip.create(Component.translatable("gui.glowcase.insert_font")))
			.build();

		// TODO - set block values here
		Vec3FieldsWidget offsetWidgets = Vec3FieldsWidget.builder(this.font, Vec3.ZERO)
			.setWidth(106)
			.setTooltips(
				Tooltip.create(Component.translatable("gui.glowcase.x_offset_label")),
				Tooltip.create(Component.translatable("gui.glowcase.y_offset_label")),
				Tooltip.create(Component.translatable("gui.glowcase.z_offset_label"))
			).build();
		Vec3FieldsWidget rotationWidgets = Vec3FieldsWidget.builder(this.font, Vec3.ZERO)
			.setWidth(106)
			.setRotation(true)
			.setTooltips(
				Tooltip.create(Component.translatable("gui.glowcase.pitch_x_rot")),
				Tooltip.create(Component.translatable("gui.glowcase.yaw_y_rot")),
				Tooltip.create(Component.translatable("gui.glowcase.roll_z_rot"))
			).build();

		this.addRenderableWidget(this.justifyLeftButton);
		this.addRenderableWidget(this.justifyCenterButton);
		this.addRenderableWidget(this.justifyRightButton);
		this.addRenderableWidget(zFrontButton);
		this.addRenderableWidget(zCenterButton);
		this.addRenderableWidget(zBackButton);
		this.addRenderableWidget(anchorGrid);
		this.addRenderableWidget(textShadowButton);

		this.addRenderableWidget(offsetWidgets);
		this.addRenderableWidget(rotationWidgets);
		this.addRenderableWidget(insertFontButton);

		LinearLayout viewTabFirstRowLayout = LinearLayout.horizontal().spacing(2);
		viewTabFirstRowLayout.addChild(this.justifyLeftButton);
		viewTabFirstRowLayout.addChild(this.justifyCenterButton, LayoutSettings.defaults().paddingLeft(-2));
		viewTabFirstRowLayout.addChild(this.justifyRightButton, LayoutSettings.defaults().paddingLeft(-2).paddingRight(4));
		viewTabFirstRowLayout.addChild(zFrontButton);
		viewTabFirstRowLayout.addChild(zCenterButton, LayoutSettings.defaults().paddingLeft(-2));
		viewTabFirstRowLayout.addChild(zBackButton, LayoutSettings.defaults().paddingLeft(-2).paddingRight(2));
		viewTabFirstRowLayout.addChild(anchorGrid, LayoutSettings.defaults().paddingRight(4));
		viewTabFirstRowLayout.addChild(textShadowButton);

		viewTabFirstRowLayout.arrangeElements(); // Setup initial positioning
		FrameLayout.centerInRectangle(viewTabFirstRowLayout, 0, firstRowY, this.width, 42); // Finish positioning

		rotationWidgets.setPosition(anchorGrid.getX() - 4 - rotationWidgets.getWidth(), secondRowY);
		offsetWidgets.setPosition(rotationWidgets.getX() - 4 - offsetWidgets.getWidth(), secondRowY);
		insertFontButton.setPosition(anchorGrid.getRight() + 6, secondRowY);

		return List.of(
			justifyRightButton, justifyCenterButton, justifyLeftButton,
			zFrontButton, zCenterButton, zBackButton,
			anchorGrid, textShadowButton,
			offsetWidgets, rotationWidgets, insertFontButton
		);
	}

	public void updateSelectedJustifyButton() {
		TextBlockEntity.TextAlignment justify = this.textBlockEntity.textAlignment;
		this.justifyLeftButton.active = justify != TextBlockEntity.TextAlignment.LEFT;
		this.justifyCenterButton.active = justify != TextBlockEntity.TextAlignment.CENTER;
		this.justifyRightButton.active = justify != TextBlockEntity.TextAlignment.RIGHT;
	}

	public void updateSelectedZButton() {
		TextBlockEntity.ZOffset zOffset = this.textBlockEntity.zOffset;
		this.zFrontButton.active = zOffset != TextBlockEntity.ZOffset.FRONT;
		this.zCenterButton.active = zOffset != TextBlockEntity.ZOffset.CENTER;
		this.zBackButton.active = zOffset != TextBlockEntity.ZOffset.BACK;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		this.extractColorPicker(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.keyPressedColorPicker(event)) return true;
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
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
