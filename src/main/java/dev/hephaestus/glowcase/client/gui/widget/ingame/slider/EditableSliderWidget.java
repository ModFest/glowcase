package dev.hephaestus.glowcase.client.gui.widget.ingame.slider;

import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.util.InputFilters;
import dev.hephaestus.glowcase.util.ParseUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A SliderWidget Button which can be right-clicked to manually enter a value. With that, it includes built-in options for handling min/max and stepping the value.<br><br>
 * Everything is using floats instead of doubles because I kept having issues with lesser precise numbers becoming very very precise (e.g. 1.055 -> 1.054990584928 or something stupid)
 *
 * @apiNote It is not intuitive per normal Minecraft UI design to have right-click do anything special, so consider including a tooltip to let users know that this widget is right-clickable!
 * @author Superkat32
 */
public class EditableSliderWidget extends AbstractSliderButton {
	public final GlowcaseEditBox editBox;
	public final Font font;
	public final Function<Float, Component> getMessageFunc;
	public final Consumer<Float> onValueChange;
	public float minValue, maxValue, valueStep;

	public boolean editing = false;
	public float displayedValue; // Used to make sure the edit box value doesn't get rounded

	public static Builder builder(Font font, float initValue, float min, float max, Function<Float, Component> getMessage, Consumer<Float> onValueChange) {
		return new Builder(font, initValue, min, max, getMessage, onValueChange);
	}

	public EditableSliderWidget(
		Font font,
		int x, int y, int width, int height,
		float initialValue, float min, float max, float step,
		Function<Float, Component> getMessage, Consumer<Float> onValueChange
	) {
		super(x, y, width, height, getMessage.apply(initialValue), (initialValue - min) / (max - min));
		this.font = font;
		this.minValue = min;
		this.maxValue = max;
		this.valueStep = step;
		this.getMessageFunc = getMessage;
		this.onValueChange = onValueChange;

		this.displayedValue = initialValue;
		this.editBox = new GlowcaseEditBox(font, x, y, width, height, Component.empty());
		this.editBox.setFilter(InputFilters::realNumber);
		this.editBox.setResponder(s -> {
			float inputValue = (float) Math.clamp(ParseUtil.parseOrDefault(s, 1.0), this.minValue, this.maxValue);
			this.onValueChange.accept(inputValue);
			this.displayedValue = inputValue;

			float inputDelta = (inputValue - this.minValue) / (this.maxValue - this.minValue);
			this.updateValue(inputDelta);
			this.setMessage(this.getMessageFunc.apply(inputValue));
		});
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
		if (this.editing) this.editBox.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 1) { // If right-clicked
			this.editing = true;
			this.editBox.setValue(String.valueOf(this.displayedValue));
			this.editBox.mouseClicked(event, doubleClick);
			this.editBox.setFocused(true);
			return true;
		}
		if (this.editing) {
			if (this.editBox.mouseClicked(event, doubleClick)) return true;
			this.editing = false; // Disable editing if didn't click edit box
			return false;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.editing) {
			if (event.isEscape() || event.isConfirmation()) {
				this.editing = false;
				return true;
			}
			return this.editBox.keyPressed(event);
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (this.editing) return this.editBox.charTyped(event);
		return super.charTyped(event);
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused && this.editing) this.editing = false;
	}

	@Override
	protected void updateMessage() {
		this.setMessage(this.getMessageFunc.apply(this.calcValue()));
	}

	@Override
	protected void applyValue() {
		float calcValue = this.calcValue();
		this.onValueChange.accept(calcValue);
		this.displayedValue = calcValue;
	}

	public void updateValue(float newValue) {
		this.value = Mth.clamp(newValue, 0, 1);
		this.updateMessage();
	}

	public float calcValue() {
		float round = this.valueStep <= 0 ? 1 : 1 / this.valueStep; // No divide by zero mistakes from me today! Haha...
		return Math.round(Mth.lerp(this.value, this.minValue, this.maxValue) * round) / round;
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		this.editBox.setX(x);
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		this.editBox.setY(y);
	}

	@Override
	public void setWidth(int width) {
		super.setWidth(width);
		this.editBox.setWidth(width);
	}

	@Override
	public void setHeight(int height) {
		super.setHeight(height);
		this.editBox.setHeight(height);
	}

	public static class Builder {
		private final Font font;
		private final float initValue;
		private final float min, max;
		private final Function<Float, Component> getMessage;
		private final Consumer<Float> onValueChange;
		private float step = 0.1f;

		private int x, y;
		private int width = 113;
		private int height = 20;

		public Builder(Font font, float initValue, float min, float max, Function<Float, Component> getMessage, Consumer<Float> onValueChange) {
			this.font = font;
			this.initValue = initValue;
			this.min = min;
			this.max = max;
			this.getMessage = getMessage;
			this.onValueChange = onValueChange;
		}

		public Builder setStep(float step) {
			this.step = step;
			return this;
		}

		public Builder bounds(int x, int y, int width, int height) {
			this.setPos(x, y);
			this.setSize(width, height);
			return this;
		}

		public Builder setPos(int x, int y) {
			this.setX(x);
			this.setY(y);
			return this;
		}

		public Builder setSize(int width, int height) {
			this.setWidth(width);
			this.setHeight(height);
			return this;
		}

		public Builder setX(int x) {
			this.x = x;
			return this;
		}

		public Builder setY(int y) {
			this.y = y;
			return this;
		}

		public Builder setWidth(int width) {
			this.width = width;
			return this;
		}

		public Builder setHeight(int height) {
			this.height = height;
			return this;
		}

		public EditableSliderWidget build() {
			return new EditableSliderWidget(
				this.font, this.x, this.y, this.width, this.height,
				this.initValue, this.min, this.max, this.step,
				this.getMessage, this.onValueChange
			);
		}
	}
}
