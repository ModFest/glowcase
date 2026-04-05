package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditTextBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
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
		this.options.add(new TextBlockEditScreen.TextScaleSliderWidget(this.entity, -1, -1, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT));
		this.options.add(new TextRenderDistanceSliderWidget(this.entity, -1, -1));

		this.layout.visitWidgets(this::addRenderableWidget);
		this.layout.arrangeElements();
	}

	public static class TextOptionList extends ContainerObjectSelectionList<TextOptionList.Entry> {
		public TextOptionList(Minecraft minecraft, int width, int height, int y) {
			super(minecraft, width, height, y, Button.DEFAULT_HEIGHT + 5);
		}

		public void add(AbstractWidget widget) {
			this.addEntry(new WidgetEntry(widget));
		}

		@Override
		public int getRowWidth() {
			return 310;
		}

		public static abstract class Entry extends ContainerObjectSelectionList.Entry<Entry> {

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
