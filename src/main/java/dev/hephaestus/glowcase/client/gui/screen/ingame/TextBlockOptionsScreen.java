package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.screen.ingame.TextBlockEditScreen.TextScale;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.HexColorEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import dev.hephaestus.glowcase.packet.C2SEditTextBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NullMarked;

import java.util.List;

public class TextBlockOptionsScreen extends BlockEditorScreen<TextBlockEntity> implements ColorPickerIncludedScreen {
	private final Screen returnScreen;

	private ColorPickerWidget colorPickerWidget;
	public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	public TextOptionList options;

	public TextBlockOptionsScreen(Screen returnScreen, TextBlockEntity blockEntity) {
		super(blockEntity, Component.translatable("gui.glowcase.text_options"));
		this.returnScreen = returnScreen;
	}

	@Override
	public void init() {
		super.init();

		this.layout.addTitleHeader(this.title, this.font);
		this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, _ -> this.onClose()).width(200).build());

		this.options = this.layout.addToContents(new TextOptionList(this.minecraft, this.width, this.layout.getContentHeight(), this.layout.getHeaderHeight()));
		addScaleWidgetRow(this.options);

		this.options.add(
			CycleButton.builder(
					alignment -> Component.literal(alignment.toString()),
					blockEntity.horizontalAlignment
				)
				.withValues(TextBlockEntity.HorizontalAlignment.values())
				.create(
					Component.translatable("gui.glowcase.x_offset_label"),
					(_, alignment) -> {
						blockEntity.horizontalAlignment = alignment;
						blockEntity.rebake(true);
					}
				),
			CycleButton.builder(
					offset -> Component.literal(offset.toString()),
					blockEntity.zOffset
				)
				.withValues(TextBlockEntity.ZOffset.values())
				.create(
					Component.translatable("gui.glowcase.z_offset_label"),
					(_, offset) -> {
						blockEntity.zOffset = offset;
						blockEntity.rebake(true);
					}
				)
		);

		this.options.add(
			CycleButton.builder(
					alignment -> Component.literal(alignment.toString()),
					blockEntity.textAlignment
				)
				.withValues(
					// not `.values()` to not have CENTER_LEFT or CENTER_RIGHT, unless they get removed
					TextBlockEntity.TextAlignment.CENTER,
					TextBlockEntity.TextAlignment.LEFT,
					TextBlockEntity.TextAlignment.RIGHT
				)
				.create(
					Component.translatable("gui.glowcase.text_alignment"),
					(_, alignment) -> {
						blockEntity.textAlignment = alignment;
						blockEntity.rebake(true);
					}
				),
			CycleButton.onOffBuilder(blockEntity.shadow).create(
				Component.translatable("gui.glowcase.text_shadow"),
				(_, shadow) -> {
					blockEntity.shadow = shadow;
					blockEntity.rebake(true);
				}
			)
		);

		// Note: Color picker position is messed up on this screen, but it still works otherwise
		this.colorPickerWidget = createColorPickerWidget();
		this.options.addHeaders(Component.translatable("gui.glowcase.color"), Component.translatable("gui.glowcase.background_color"));
		HexColorEditBox colorEditBox = HexColorEditBox.builder(this.minecraft.font, 0, 0,
				() -> this.blockEntity.color, color -> {
					this.blockEntity.color = color;
					this.blockEntity.rebake(true);
				}
			)
			.setEditableAlpha(true)
			.setWidth(Button.DEFAULT_WIDTH)
			.setColorPickerWidget(this.colorPickerWidget)
			.build();

//		var colorEditBox = new EditBox(
//			this.font,
//			Button.DEFAULT_WIDTH,
//			Button.DEFAULT_HEIGHT,
//			Component.translatable("gui.glowcase.color")
//		);
//		colorEditBox.setValue(ColorUtil.toAlphaHex(this.blockEntity.color));
//		colorEditBox.setResponder(string -> ColorUtil.parse(string, blockEntity.color)
//			.ifSuccess(newColor -> {
//				blockEntity.color = newColor;
//				blockEntity.rebake(true);
//			}));

		HexColorEditBox backgroundEditBox = HexColorEditBox.builder(this.minecraft.font, 0, 0,
				() -> this.blockEntity.backgroundColor, color -> {
					this.blockEntity.backgroundColor = color;
					this.blockEntity.rebake(true);
				}
			)
			.setWidth(Button.DEFAULT_WIDTH)
			.setEditableAlpha(true)
			.setColorPickerWidget(this.colorPickerWidget)
			.build();
//		var backgroundEditBox = new EditBox(
//			this.font,
//			Button.DEFAULT_WIDTH,
//			Button.DEFAULT_HEIGHT,
//			Component.translatable("gui.glowcase.background_color")
//		);
//		backgroundEditBox.setValue(ColorUtil.toAlphaHex(this.blockEntity.backgroundColor));
//		backgroundEditBox.setResponder(string -> ColorUtil.parse(string, blockEntity.backgroundColor)
//			.ifSuccess(newColor -> {
//				blockEntity.backgroundColor = newColor;
//				blockEntity.rebake(true);
//			}));

		this.options.add(colorEditBox, backgroundEditBox);

		this.layout.visitWidgets(this::addRenderableWidget);
		this.layout.arrangeElements();
	}

	private void addScaleWidgetRow(TextOptionList options) {
		var slider = new TextScale.SliderWidget(this.blockEntity, -1, -1, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT);
		var input = new TextScale.InputWidget(
			this.blockEntity,
			this.font,
			-1,
			-1,
			Button.DEFAULT_WIDTH,
			Button.DEFAULT_HEIGHT
		);

		slider.setScaleResponder(scale -> input.setValue(String.valueOf(scale)));
		input.setScaleResponder(scale -> slider.updateValue((scale - TextScale.MIN_SCALE) / TextScale.SCALE_DELTA));

		options.add(slider, input);
	}

	@Override
	public CustomPacketPayload getUpdatePayload() {
		return C2SEditTextBlock.of(this.blockEntity);
	}

	@Override
	public void onClose() {
		super.onClose();
		this.minecraft.setScreen(this.returnScreen);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		this.extractColorPicker(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.mouseClickedColorPicker(event, doubleClick)) return true;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.keyPressedColorPicker(event)) return true;
		return super.keyPressed(event);
	}

	@Override
	public ColorPickerWidget getColorPickerWidget() {
		return this.colorPickerWidget;
	}

	@NullMarked
	public static class TextOptionList extends ContainerObjectSelectionList<TextOptionList.Entry> {
		public TextOptionList(Minecraft minecraft, int width, int height, int y) {
			super(minecraft, width, height, y, Button.DEFAULT_HEIGHT + 5);
		}

		public void addHeader(Component text) {
			int lineHeight = this.minecraft.font.lineHeight;
			int paddingTop = this.children().isEmpty() ? 0 : lineHeight * 2;
			this.addEntry(new HeaderEntry(new StringWidget(text, this.minecraft.font), paddingTop), paddingTop + lineHeight + 4);
		}

		public void addHeaders(Component leftHeader, Component rightHeader) {
			int lineHeight = this.minecraft.font.lineHeight;
			int paddingTop = this.children().isEmpty() ? 0 : lineHeight;
			this.addEntry(
				new DualHeaderEntry(
					new StringWidget(leftHeader, this.minecraft.font),
					new StringWidget(rightHeader, this.minecraft.font), paddingTop),
				paddingTop + lineHeight + 4
			);
		}

		public void add(AbstractWidget widget) {
			this.addEntry(new WidgetEntry(widget));
		}

		public void add(AbstractWidget leftWidget, AbstractWidget rightWidget) {
			this.addEntry(new TwoWidgetsEntry(leftWidget, rightWidget));
		}

		@Override
		public int getRowWidth() {
			return 310;
		}

		public static abstract class Entry extends ContainerObjectSelectionList.Entry<Entry> {}

		public static class HeaderEntry extends Entry {
			protected final StringWidget widget;
			protected final int paddingTop;

			public HeaderEntry(StringWidget widget, int paddingTop) {
				this.widget = widget;
				this.paddingTop = paddingTop;
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
				this.widget.setPosition(this.getContentX(), this.getContentY() + this.paddingTop);
				this.widget.extractRenderState(graphics, mouseX, mouseY, a);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return List.of(this.widget);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return List.of(this.widget);
			}
		}

		public static class DualHeaderEntry extends Entry {
			protected final StringWidget leftWidget;
			protected final StringWidget rightWidget;
			protected final int paddingTop;

			public DualHeaderEntry(StringWidget leftWidget, StringWidget rightWidget, int paddingTop) {
				this.leftWidget = leftWidget;
				this.rightWidget = rightWidget;
				this.paddingTop = paddingTop;
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
				this.leftWidget.setPosition(this.getContentX(), this.getContentY() + this.paddingTop);
				this.leftWidget.extractRenderState(graphics, mouseX, mouseY, a);
				this.rightWidget.setPosition(this.getContentX() + 160, this.getContentY() + this.paddingTop);
				this.rightWidget.extractRenderState(graphics, mouseX, mouseY, a);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return List.of(this.leftWidget, this.rightWidget);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return List.of(this.leftWidget, this.rightWidget);
			}
		}

		public static class WidgetEntry extends Entry {
			protected final AbstractWidget widget;

			public WidgetEntry(AbstractWidget widget) {
				this.widget = widget;
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
				this.widget.setPosition(this.getContentX(), this.getContentY());
				this.widget.extractRenderState(graphics, mouseX, mouseY, a);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return List.of(this.widget);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return List.of(this.widget);
			}
		}

		public static class TwoWidgetsEntry extends Entry {
			protected final AbstractWidget leftWidget;
			protected final AbstractWidget rightWidget;

			public TwoWidgetsEntry(AbstractWidget leftWidget, AbstractWidget rightWidget) {
				this.leftWidget = leftWidget;
				this.rightWidget = rightWidget;
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
				this.leftWidget.setPosition(this.getContentX(), this.getContentY());
				this.leftWidget.extractRenderState(graphics, mouseX, mouseY, a);
				this.rightWidget.setPosition(this.getContentX() + 160, this.getContentY());
				this.rightWidget.extractRenderState(graphics, mouseX, mouseY, a);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return List.of(this.leftWidget, this.rightWidget);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return List.of(this.leftWidget, this.rightWidget);
			}
		}
	}
}
