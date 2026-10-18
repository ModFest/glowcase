package dev.hephaestus.glowcase.client.gui.widget.ingame.color;

import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.functional.ColorGetter;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.functional.ColorSetter;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import dev.hephaestus.glowcase.client.util.TooltipCow;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

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

	private ColorUtil.Presentation presentation;

	@Nullable
	private Tooltip lastTooltip;
	private boolean editableAlpha;
	private final float pickerMinAlpha;
	private final float pickerDefaultAlpha;

	public static HexColorEditBox.Builder builder(Font textRenderer, int x, int y, ColorGetter colorGetter, ColorSetter colorSetter) {
		return new Builder(textRenderer, x, y, colorGetter, colorSetter);
	}

	private HexColorEditBox(
		final Font textRenderer,
		final int x,
		final int y,
		final int width,
		final int height,

		final boolean editableAlpha,
		final float pickerMinAlpha,
		final float pickerDefaultAlpha,
		final @Nullable ColorPickerWidget colorPickerWidget,

		final ColorGetter colorGetter,
		final ColorSetter colorSetter
	) {
		super(textRenderer, x, y, width, height, Component.empty());
		this.colorPickerWidget = colorPickerWidget;
		this.colorGetter = colorGetter;
		this.colorSetter = colorSetter;
		this.color = colorGetter.get();
		this.pickerMinAlpha = pickerMinAlpha;
		this.pickerDefaultAlpha = pickerDefaultAlpha;

		this.setResponder(string -> {
			this.presentation.parse(string).ifPresent(this.colorSetter::set);
		});

		this.setEditableAlpha(editableAlpha);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		super.onClick(event, doubleClick);
		if (this.colorPickerWidget != null) {
			this.colorPickerWidget.target(
				this,
				this.color,
				this.editableAlpha,
				this.pickerMinAlpha,
				this.pickerDefaultAlpha,
				this::setColor
			);
		}
	}

	@Override
	public boolean keyPressed(final KeyEvent event) {
		if (super.keyPressed(event)) {
			updatePresentation();
			return true;
		}
		return false;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if(super.charTyped(event)) {
			updatePresentation();
			return true;
		}
		return false;
	}

	private void updatePresentation() {
		if (this.colorPickerWidget != null) {
			this.presentation.parse(this.getValue()).ifPresent(parsedColor -> {
				this.color = parsedColor;
				this.colorPickerWidget.setColor(parsedColor);
			});
		}
	}

	public void setTextFromColor() {
		String hexString = this.presentation.present(this.color);
		this.setValue(hexString);
	}

	public void setColor(int color) {
		this.color = color;
		this.setTextFromColor();
	}

	public void setEditableAlpha(final boolean editableAlpha) {
		this.editableAlpha = editableAlpha;
		this.presentation = ColorUtil.getPresentation(editableAlpha);
		super.setHint(presentation.hint);
		this.setTextFromColor();
		this.updateTooltip();
	}

	@Override
	public void setTooltip(@Nullable final Tooltip tooltip) {
		this.lastTooltip = tooltip;
		this.updateTooltip();
	}

	private void updateTooltip() {
		if (this.lastTooltip == null) {
			super.setTooltip(this.presentation.tooltip);
			return;
		}

		if (this.lastTooltip instanceof TooltipCow cow) {
			super.setTooltip(cow.glowcase$appendLine(this.presentation.component));
			return;
		}

		// Our mixin is broken, just pass-through.
		super.setTooltip(this.lastTooltip);
	}


	/**
	 * Shall not be called, handled by the presentation.
	 */
	@Override
	@Deprecated(forRemoval = true)
	public final void setHint(final Component placeholder) {
		throw new UnsupportedOperationException();
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
		private float pickerDefaultAlpha = 1f;
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

		public Builder setPickerMinAlpha(@Range(from = 0, to = 1) final float pickerMinAlpha) {
			this.pickerMinAlpha = pickerMinAlpha;
			return this;
		}

		public Builder setPickerDefaultAlpha(@Range(from = 0, to = 1) final float pickerDefaultAlpha) {
			this.pickerDefaultAlpha = pickerDefaultAlpha;
			return this;
		}

		public Builder setColorPickerWidget(ColorPickerWidget colorPickerWidget) {
			this.colorPickerWidget = colorPickerWidget;
			return this;
		}

		public HexColorEditBox build() {
			return new HexColorEditBox(
				this.textRenderer,
				this.x,
				this.y,
				this.width,
				this.height,

				this.editableAlpha,
				this.pickerMinAlpha,
				this.pickerDefaultAlpha,
				this.colorPickerWidget,

				this.colorGetter,
				this.colorSetter
			);
		}
	}

}
