package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import dev.hephaestus.glowcase.packet.C2SEditTextBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

import java.util.List;

public class TextBlockOptionsScreen extends Screen {
	private final Screen returnScreen;
	private final TextBlockEntity entity;

	public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	public TextOptionList options;

	public TextBlockOptionsScreen(Screen returnScreen, TextBlockEntity entity) {
		super(Component.translatable("gui.glowcase.text_options"));
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

		this.layout.addTitleHeader(this.title, this.font);
		this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, _ -> this.onClose()).width(200).build());

		this.options = this.layout.addToContents(new TextOptionList(this.minecraft, this.width, this.layout.getContentHeight(), this.layout.getHeaderHeight()));
		this.options.add(
			new TextBlockEditScreen.TextScaleSliderWidget(this.entity, -1, -1, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT),
			new TextRenderDistanceSliderWidget(this.entity, -1, -1)
		);
		this.options.add(
			CycleButton.builder(
					alignment -> Component.literal(alignment.toString()),
					entity.horizontalAlignment
				)
				.withValues(TextBlockEntity.HorizontalAlignment.values())
				.create(
					Component.translatable("gui.glowcase.x_offset_label"),
					(_, alignment) -> {
						entity.horizontalAlignment = alignment;
						entity.renderDirty = true;
					}
				),
			CycleButton.builder(
					offset -> Component.literal(offset.toString()),
					entity.zOffset
				)
				.withValues(TextBlockEntity.ZOffset.values())
				.create(
					Component.translatable("gui.glowcase.z_offset_label"),
					(_, offset) -> {
						entity.zOffset = offset;
						entity.renderDirty = true;
					}
				)
		);
		this.options.add(
			CycleButton.builder(
					alignment -> Component.literal(alignment.toString()),
					entity.textAlignment
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
						entity.textAlignment = alignment;
						entity.renderDirty = true;
					}
				),
			CycleButton.onOffBuilder(entity.shadow).create(
				Component.translatable("gui.glowcase.text_shadow"),
				(_, shadow) -> {
					entity.shadow = shadow;
					entity.renderDirty = true;
				}
			)
		);

		this.options.addHeader(Component.translatable("gui.glowcase.color"));
		var colorEditBox = new EditBox(
			this.font,
			Button.DEFAULT_WIDTH,
			Button.DEFAULT_HEIGHT,
			Component.translatable("gui.glowcase.color")
		);
		colorEditBox.setValue(ColorUtil.toAlphaHex(this.entity.color));
		colorEditBox.setResponder(string -> ColorUtil.parse(string, entity.color)
			.ifSuccess(newColor -> {
				entity.color = newColor;
				entity.renderDirty = true;
			}));
		this.options.add(colorEditBox);

		this.options.addHeader(Component.translatable("gui.glowcase.background_color"));
		var backgroundEditBox = new EditBox(
			this.font,
			Button.DEFAULT_WIDTH,
			Button.DEFAULT_HEIGHT,
			Component.translatable("gui.glowcase.background_color")
		);
		backgroundEditBox.setValue(ColorUtil.toAlphaHex(this.entity.backgroundColor));
		backgroundEditBox.setResponder(string -> ColorUtil.parse(string, entity.backgroundColor)
			.ifSuccess(newColor -> {
				entity.backgroundColor = newColor;
				entity.renderDirty = true;
			}));
		this.options.add(backgroundEditBox);

		this.layout.visitWidgets(this::addRenderableWidget);
		this.layout.arrangeElements();
	}

	public static class TextOptionList extends ContainerObjectSelectionList<TextOptionList.Entry> {
		public TextOptionList(Minecraft minecraft, int width, int height, int y) {
			super(minecraft, width, height, y, Button.DEFAULT_HEIGHT + 5);
		}

		public void addHeader(Component text) {
			int lineHeight = this.minecraft.font.lineHeight;
			int paddingTop = this.children().isEmpty() ? 0 : lineHeight * 2;
			this.addEntry(new HeaderEntry(new StringWidget(text, this.minecraft.font), paddingTop), paddingTop + lineHeight + 4);
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

		public static abstract class Entry extends ContainerObjectSelectionList.Entry<Entry> {

		}

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

	private static class TextRenderDistanceSliderWidget extends AbstractSliderButton {
		private static final float INFINITE_VALUE = -1;
		private static final float MIN_VALUE = 1;
		private static final float MAX_VALUE = 256;

		private final TextBlockEntity entity;

		public TextRenderDistanceSliderWidget(TextBlockEntity entity, int x, int y) {
			double initialValue = entity.viewDistance == INFINITE_VALUE ? 1 : (entity.viewDistance - MIN_VALUE) / (MAX_VALUE - MIN_VALUE);
			super(x, y, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, createMessage(entity), initialValue);

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
