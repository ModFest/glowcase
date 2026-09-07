package dev.hephaestus.glowcase.client.gui.widget.ingame.color;

import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.functional.ColorGetter;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.functional.ColorSetter;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * An EditBox Widget built for HEX colors, with automatic color (as an integer) getter & setter, and built-in {@link ColorPickerWidget} support.
 * @author Superkat32
 */
public class HexColorEditBox extends GlowcaseEditBox {
	@Nullable
	public final ColorPickerWidget colorPickerWidget;
	public final ColorGetter colorGetter;
	public final ColorSetter colorSetter;
	public int color;
	public boolean editableAlpha;
	public float pickerMinAlpha;

	public static HexColorEditBox.Builder builder(Font textRenderer, int x, int y, ColorGetter colorGetter, ColorSetter colorSetter) {
		return new Builder(textRenderer, x, y, colorGetter, colorSetter);
	}

	public HexColorEditBox(
		Font textRenderer, int x, int y, int width, int height,
		boolean editableAlpha, float pickerMinAlpha, @Nullable ColorPickerWidget colorPickerWidget,
		ColorGetter colorGetter, ColorSetter colorSetter
	) {
		super(textRenderer, x, y, width, height, Component.empty());
		this.colorPickerWidget = colorPickerWidget;
		this.colorGetter = colorGetter;
		this.colorSetter = colorSetter;
		this.color = colorGetter.get();
		this.editableAlpha = editableAlpha;
		this.pickerMinAlpha = pickerMinAlpha;

		this.setResponder(string -> {
			ColorUtil.parse(string, this.color).ifSuccess(this.colorSetter::set);
		});

		this.setTextFromColor();
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		super.onClick(event, doubleClick);
		if (this.colorPickerWidget != null) {
			this.colorPickerWidget.target(this, this.color, this.editableAlpha, this.pickerMinAlpha, this::setColor);
		}
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if(super.charTyped(event)) {
			if (this.colorPickerWidget != null) {
				ColorUtil.parse(this.getValue(), this.color).ifSuccess(parsedColor -> {
					this.color = parsedColor;
					this.colorPickerWidget.setColor(parsedColor);
				});
			}
			return true;
		}
		return false;
	}

	public void setTextFromColor() {
		String hexString = this.formatColorAsHex(this.color);
		this.setValue(hexString);
	}

	public String formatColorAsHex(int color) {
		if (this.editableAlpha) return ColorUtil.toAlphaHex(color);
		return ColorUtil.toHex(color);
	}

	public void setColor(int color) {
		this.color = color;
		this.setTextFromColor();
	}

	public static class Builder {
		private static final int DEFAULT_WIDTH = 50;
		private static final int DEFAULT_ALPHA_WIDTH = 64;

		private final ColorGetter colorGetter;
		private final ColorSetter colorSetter;
		private final Font textRenderer;
		private final int x, y;

		private int width = DEFAULT_WIDTH;
		private int height = 20;
		private boolean editableAlpha = false;
		private float pickerMinAlpha = 0f;
		private ColorPickerWidget colorPickerWidget = null;

		public Builder(Font textRenderer, int x, int y, ColorGetter colorGetter, ColorSetter colorSetter) {
			this.colorGetter = colorGetter;
			this.colorSetter = colorSetter;
			this.textRenderer = textRenderer;
			this.x = x;
			this.y = y;
		}

		public Builder setWidth(int width) {
			this.width = width;
			return this;
		}

		public Builder setHeight(int height) {
			this.height = height;
			return this;
		}

		public Builder setEditableAlpha(boolean editableAlpha) {
			this.editableAlpha = editableAlpha;
			if (this.width == DEFAULT_WIDTH) this.width = DEFAULT_ALPHA_WIDTH;
			return this;
		}

		public Builder setPickerMinAlpha(float pickerMinAlpha) {
			this.pickerMinAlpha = pickerMinAlpha;
			return this;
		}

		public Builder setColorPickerWidget(ColorPickerWidget colorPickerWidget) {
			this.colorPickerWidget = colorPickerWidget;
			return this;
		}

		public HexColorEditBox build() {
			return new HexColorEditBox(
				this.textRenderer, this.x, this.y, this.width, this.height,
				this.editableAlpha, this.pickerMinAlpha, this.colorPickerWidget,
				this.colorGetter, this.colorSetter
			);
		}
	}

}
