package dev.hephaestus.glowcase.client.gui.widget.ingame;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class AnchorPositionGridWidget extends AbstractContainerWidget {
	public static final Identifier TOP_LEFT_ID = Glowcase.id("anchor/top_left");
	public static final Identifier TOP_ID = Glowcase.id("anchor/top");
	public static final Identifier TOP_RIGHT_ID = Glowcase.id("anchor/top_right");
	public static final Identifier MIDDLE_LEFT_ID = Glowcase.id("anchor/middle_left");
	public static final Identifier MIDDLE_ID = Glowcase.id("anchor/middle");
	public static final Identifier MIDDLE_RIGHT_ID = Glowcase.id("anchor/middle_right");
	public static final Identifier BOTTOM_LEFT_ID = Glowcase.id("anchor/bottom_left");
	public static final Identifier BOTTOM_ID = Glowcase.id("anchor/bottom");
	public static final Identifier BOTTOM_RIGHT_ID = Glowcase.id("anchor/bottom_right");
	public static final List<Identifier> TEXTURES = List.of(TOP_LEFT_ID, TOP_ID, TOP_RIGHT_ID, MIDDLE_LEFT_ID, MIDDLE_ID, MIDDLE_RIGHT_ID, BOTTOM_LEFT_ID, BOTTOM_ID, BOTTOM_RIGHT_ID);

	public final List<Button> anchorButtons = new ArrayList<>();
	public final Consumer<TextBlockEntity.Anchor> onClick;
	public TextBlockEntity.Anchor anchor = TextBlockEntity.Anchor.MIDDLE;

	public AnchorPositionGridWidget(int x, int y, TextBlockEntity.HorizontalAlignment horizontalAlignment, Consumer<TextBlockEntity.Anchor> onClick) {
		// Two rows of normal buttons (20 * 2) + button row padding (2) = 42px
		// Each anchor button here is 14x14px (42 / 3 = 14)
		TextBlockEntity.Anchor realAnchor = switch (horizontalAlignment) {
			case LEFT -> TextBlockEntity.Anchor.MIDDLE_LEFT;
			case RIGHT -> TextBlockEntity.Anchor.MIDDLE_RIGHT;
			default -> TextBlockEntity.Anchor.MIDDLE;
		};
		this(x, y, 42, 42, realAnchor, onClick);
	}

	private AnchorPositionGridWidget(int x, int y, int width, int height, TextBlockEntity.Anchor anchor, Consumer<TextBlockEntity.Anchor> onClick) {
		super(x, y, width, height, Component.empty(), AbstractScrollArea.defaultSettings(9));
		this.onClick = onClick;
		this.anchor = anchor;

		int anchorButtonSize = 14; // 42 / 3
		TextBlockEntity.Anchor[] values = TextBlockEntity.Anchor.values();
		for (int i = 0; i < values.length; i++) {
			TextBlockEntity.Anchor anchorPos = values[i];
			Button button = IconButtonWidget.builder(TEXTURES.get(i), button1 -> {
					this.onClick.accept(anchorPos);
					this.anchor = anchorPos;
					this.updateSelectedButton();
			})
				.dimensions(0, 0, anchorButtonSize, anchorButtonSize, anchorButtonSize, anchorButtonSize)
				.build();
			this.anchorButtons.add(button);
		}
		this.setButtonPositions();
		this.updateSelectedButton();
	}

	public void updateSelectedButton() {
		TextBlockEntity.Anchor[] values = TextBlockEntity.Anchor.values();
		for (int i = 0; i < values.length; i++) {
			TextBlockEntity.Anchor anchorPos = values[i];
			boolean isSelected = anchorPos == this.anchor;
			this.anchorButtons.get(i).active = !isSelected;
		}
	}

	public void setButtonPositions() {
		int anchorButtonSize = 14;
		TextBlockEntity.Anchor[] values = TextBlockEntity.Anchor.values();
		for (int i = 0; i < values.length; i++) {
			TextBlockEntity.Anchor anchorPos = values[i];
			int buttonX = this.getX() + anchorButtonSize * (anchorPos.getX() + 1);
			int buttonY = this.getY() + anchorButtonSize * (anchorPos.getY() * -1 + 1);
			this.anchorButtons.get(i).setPosition(buttonX, buttonY);
		}
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		this.setButtonPositions();
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		this.setButtonPositions();
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		for (Button anchorButton : anchorButtons) {
			anchorButton.extractRenderState(graphics, mouseX, mouseY, a);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		for (Button anchorButton : anchorButtons) {
			anchorButton.updateWidgetNarration(output);
		}
	}

	@Override
	public @NonNull List<? extends GuiEventListener> children() {
		return this.anchorButtons;
	}

	@Override
	protected int contentHeight() {
		return this.height;
	}
}
