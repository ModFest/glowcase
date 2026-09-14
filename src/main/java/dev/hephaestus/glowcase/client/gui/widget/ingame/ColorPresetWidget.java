package dev.hephaestus.glowcase.client.gui.widget.ingame;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class ColorPresetWidget extends AbstractButton {
	public final ColorPickerWidget colorPickerWidget;
	public final Color color;
	@Nullable
	public TextColor textColor = null;
	public int z = 0;

	public ColorPresetWidget(ColorPickerWidget colorPicker, int x, int y, int width, int height, Color color) {
		super(x, y, width, height, Component.nullToEmpty(""));
		this.colorPickerWidget = colorPicker;
		this.color = color;
	}

	public void setPosition(int x, int y, int z, int size) {
		this.setX(x);
		this.setY(y);
		this.setSize(size, size);
		this.z = z;
	}

	public static ColorPresetWidget fromTextColor(ColorPickerWidget colorPicker, TextColor textColor) {
		//noinspection DataFlowIssue
		ColorPresetWidget presetWidget = new ColorPresetWidget(colorPicker, 0, 0, 0, 0, new Color(textColor.getValue()));
		presetWidget.textColor = textColor;
		return presetWidget;
	}

	public static ColorPresetWidget fromColor(ColorPickerWidget colorPicker, Color color) {
		return new ColorPresetWidget(colorPicker, 0, 0, 0, 0, color);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.color.getRGB());
		if (isMouseOver(mouseX, mouseY)) {
			drawOutline(graphics, this.getX() - 1, this.getY() - 1, this.getWidth() + 2, this.getHeight() + 2, this.z + 1);
		}
	}

	private void drawOutline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int z) {
		int color = Color.white.getRGB();
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y, x + 1, y + height, color);
		graphics.fill(x + width, y, x + width - 1, y + height, color);
		graphics.fill(x, y + height, x + width, y + height - 1, color);
	}

	@Override
	public void onPress(InputWithModifiers input) {
		var presetListener = this.colorPickerWidget.getPresetListener();
		if (presetListener != null) {
			presetListener.accept(this.color, this.textColor);
		} else {
			if (this.textColor != null) {
				this.colorPickerWidget.color = this.color;
				this.colorPickerWidget.toggle(false);
			} else {
				this.colorPickerWidget.setColor(this.color);
			}
		}

	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput builder) {

	}
}
