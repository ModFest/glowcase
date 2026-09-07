package dev.hephaestus.glowcase.client.gui.widget.ingame.text;

import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A viewable multiline text area which supports vertical scrolling, text alignment, and QuickText formatting.<br><br>
 * Note: This is only for viewing, see {@link GlowcaseMultilineEditBox} for editing
 * @author Superkat32
 * @see GlowcaseMultilineEditBox
 * @see dev.hephaestus.glowcase.client.gui.screen.ingame.PopupBlockViewScreen
 */
public class MultilineTextViewArea extends AbstractTextAreaWidget {
	public final Font font;
	public List<Component> lines;
	public int textColor;
	public TextBlockEntity.TextAlignment textAlignment;

	public int sideAlignmentPadding;
	public int lineHeight = 12;

	public MultilineTextViewArea(Font font, List<Component> lines, int x, int y, int width, int height, int textColor, TextBlockEntity.TextAlignment textAlignment) {
		super(x, y, width, height, Component.empty(), AbstractScrollArea.defaultSettings((int)(9.0 / 2.0)), false, true);
		this.font = font;
		this.lines = lines;
		this.textColor = textColor;
		this.textAlignment = textAlignment;

		this.sideAlignmentPadding = this.width / 10;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		for (int i = 0; i < this.lines.size(); i++) {
			Component line = this.lines.get(i);
			int lineY = this.getY() + this.innerPadding() + i * this.lineHeight;
			int lineX = this.getAlignmentX(this.font.width(line));

			graphics.text(this.font, line, lineX, lineY, this.textColor, true);
		}
	}

	public int getAlignmentX(int lineWidth) {
		int xOffset = switch(this.textAlignment) {
			case LEFT -> this.sideAlignmentPadding;
			case RIGHT -> this.width - this.sideAlignmentPadding - lineWidth;
			default -> this.width / 2 - lineWidth / 2;
		};
		return this.getX() + xOffset;
	}

	@Override
	protected int getInnerHeight() {
		return this.lines.size() * this.lineHeight;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		// NO-OP
	}
}
