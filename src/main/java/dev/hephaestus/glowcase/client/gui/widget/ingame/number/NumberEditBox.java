package dev.hephaestus.glowcase.client.gui.widget.ingame.number;

import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.util.InputFilters;
import dev.hephaestus.glowcase.util.ParseUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

/**
 * An edit box for numbers values (e.g. ints, floats, doubles), with builtin abilities to increase/decrease the value using the arrow keys and scroll wheel.<br><br>
 * This class is so incredibly cursed.
 * @see Vec3FieldsWidget
 * @see DegreeRotationEditBox
 * @see dev.hephaestus.glowcase.client.gui.widget.ingame.slider.EditableSliderWidget
 * @author Superkat32 (But if there's problems blame The Creature of No Whimsy)
 */
public abstract class NumberEditBox<T extends Number> extends GlowcaseEditBox {
	public final Consumer<T> onValueChange;
	public T numberValue;
	public T minValue, maxValue;
	// Steps for scrolling or pressing arrow keys.
	// Per Blender's (and seemingly Microsoft's?) design standards:
	// shift = precise, control = bigger increments, shift + control = mildly precise increments.
	public T shiftStep, shiftCtrlStep, step, ctrlStep;

	public static Builder<Integer> integerBuilder(Font font, int initValue, Consumer<Integer> onValueChange) {
		return new Builder<>(font, initValue, InputFilters::integerNumber, onValueChange, IntegerEditBox::new)
			.setMinMaxValues(-128, 128)
			.setSteps(5, 8, 1, 10);
	}

	public static Builder<Float> floatBuilder(Font font, float initValue, Consumer<Float> onValueChange) {
		return new Builder<>(font, initValue, InputFilters::realNumber, onValueChange, FloatEditBox::new)
			.setMinMaxValues(-128.0f, 128.0f) // Arbitrary limits, most cases shouldn't need any higher than this I think
			.setSteps(0.01f, 0.125f, 0.1f, 1.0f);
	}

	public static Builder<Double> doubleBuilder(Font font, double initValue, Consumer<Double> onValueChange) {
		return new Builder<>(font, initValue, InputFilters::realNumber, onValueChange, DoubleEditBox::new)
			.setMinMaxValues(-128.0d, 128.0d)
			.setSteps(0.01d, 0.125d, 0.1d, 1.0d);
	}

	public static Builder<Float> degreeFloatBuilder(Font font, float initValue, Consumer<Float> onValueChange) {
		return new Builder<>(font, initValue, InputFilters::realNumber, onValueChange, DegreeRotationEditBox::new)
			.setMinMaxValues(-360.0f, 360.0f)
			.setSteps(0.01f, 0.125f, 0.1f, 1.0f);
	}

	public NumberEditBox(
		Font font, int x, int y, int width, int height,
		T initValue, T minValue, T maxValue,
		T shiftStep, T shiftCtrlStep, T step, T ctrlStep,
		Filter inputFilter, Consumer<T> onValueChange
	) {
		super(font, x, y, width, height, Component.empty());
		this.numberValue = initValue;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.shiftStep = shiftStep;
		this.shiftCtrlStep = shiftCtrlStep;
		this.step = step;
		this.ctrlStep = ctrlStep;
		this.onValueChange = onValueChange;

		this.setFilter(inputFilter);
		this.updateEditBox(); // Set text to string of number value
	}

	public abstract void stepUp(boolean shift, boolean ctrl);

	public abstract void stepDown(boolean shift, boolean ctrl);

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isUp()) {
			this.stepUp(event.hasShiftDown(), event.hasControlDownWithQuirk());
			this.playClickSound(Minecraft.getInstance().getSoundManager());
			return true;
		} else if (event.isDown()) {
			this.stepDown(event.hasShiftDown(), event.hasControlDownWithQuirk());
			this.playClickSound(Minecraft.getInstance().getSoundManager());
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (scrollY > 0) {
			this.stepUp(Minecraft.getInstance().hasShiftDown(), Minecraft.getInstance().hasControlDown());
			this.playClickSound(Minecraft.getInstance().getSoundManager());
			return true;
		} else if (scrollY < 0) {
			this.stepDown(Minecraft.getInstance().hasShiftDown(), Minecraft.getInstance().hasControlDown());
			this.playClickSound(Minecraft.getInstance().getSoundManager());
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	public void playClickSound(SoundManager soundManager) {
		soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1f, 0.15f));
	}

	public void updateEditBox() {
		this.setValue(String.valueOf(this.numberValue));
	}

	public T getStep(boolean shift, boolean ctrl) {
		if (shift & ctrl) return this.shiftCtrlStep;
		if (shift) return this.shiftStep;
		if (ctrl) return this.ctrlStep;
		return this.step;
	}

	public T getNumberValue() {
		return this.numberValue;
	}

	public static class IntegerEditBox extends NumberEditBox<Integer> {
		public IntegerEditBox(
			Font font, int x, int y, int width, int height,
			Integer initValue, Integer minValue, Integer maxValue,
			Integer shiftStep, Integer shiftCtrlStep, Integer step, Integer ctrlStep,
			Filter inputFilter, Consumer<Integer> onValueChange
		) {
			super(font, x, y, width, height, initValue, minValue, maxValue, shiftStep, shiftCtrlStep, step, ctrlStep, inputFilter, onValueChange);
			this.setResponder(s -> {
				this.numberValue = Math.clamp(ParseUtil.parseOrDefault(s, initValue), this.minValue, this.maxValue);
				this.onValueChange.accept(this.numberValue);
			});
		}

		@Override
		public void stepUp(boolean shift, boolean ctrl) {
			int step = this.getStep(shift, ctrl);
			this.numberValue = Mth.clamp(this.numberValue + step, this.minValue, this.maxValue);
			this.updateEditBox();
		}

		@Override
		public void stepDown(boolean shift, boolean ctrl) {
			int step = this.getStep(shift, ctrl);
			this.numberValue = Mth.clamp(this.numberValue - step, this.minValue, this.maxValue);
			this.updateEditBox();
		}
	}

	public static class FloatEditBox extends NumberEditBox<Float> {
		public FloatEditBox(
			Font font, int x, int y, int width, int height,
			float initValue, float minValue, float maxValue,
			float shiftStep, float shiftCtrlStep, float step, float ctrlStep,
			Filter inputFilter, Consumer<Float> onValueChange
		) {
			super(font, x, y, width, height, initValue, minValue, maxValue, shiftStep, shiftCtrlStep, step, ctrlStep, inputFilter, onValueChange);
			this.setResponder(s -> {
				this.numberValue = Math.clamp(ParseUtil.parseOrDefault(s, initValue), this.minValue, this.maxValue);
				this.onValueChange.accept(this.numberValue);
			});
		}

		@Override
		public void stepUp(boolean shift, boolean ctrl) {
			float step = this.getStep(shift, ctrl);
			float round = 1f / step;
			this.numberValue = Math.clamp(Math.round((this.numberValue + step) * round) / round, this.minValue, this.maxValue);
			this.updateEditBox();
		}

		@Override
		public void stepDown(boolean shift, boolean ctrl) {
			float step = this.getStep(shift, ctrl);
			float round = 1f / step;
			this.numberValue = Math.clamp(Math.round((this.numberValue - step) * round) / round, this.minValue, this.maxValue);
			this.updateEditBox();
		}
	}

	public static class DoubleEditBox extends NumberEditBox<Double> {
		public DoubleEditBox(
			Font font, int x, int y, int width, int height,
			double initValue, double minValue, double maxValue,
			double shiftStep, double shiftCtrlStep, double step, double ctrlStep,
			Filter inputFilter, Consumer<Double> onValueChange
		) {
			super(font, x, y, width, height, initValue, minValue, maxValue, shiftStep, shiftCtrlStep, step, ctrlStep, inputFilter, onValueChange);
			this.setResponder(s -> {
				this.numberValue = Math.clamp(ParseUtil.parseOrDefault(s, initValue), this.minValue, this.maxValue);
				this.onValueChange.accept(this.numberValue);
			});
		}

		@Override
		public void stepUp(boolean shift, boolean ctrl) {
			double step = this.getStep(shift, ctrl);
			double round = 1f / step;
			this.numberValue = Math.clamp(Math.round((this.numberValue + step) * round) / round, this.minValue, this.maxValue);
			this.updateEditBox();
		}

		@Override
		public void stepDown(boolean shift, boolean ctrl) {
			double step = this.getStep(shift, ctrl);
			double round = 1d / step;
			this.numberValue = Math.clamp(Math.round((this.numberValue - step) * round) / round, this.minValue, this.maxValue);
			this.updateEditBox();
		}
	}

	public static class Builder<T extends Number> {
		private final Font font;
		private final Filter inputFilter;
		private final Consumer<T> onValueChange;
		private final WidgetCreator<T> widgetCreator;
		private final T initValue;

		private int x, y;
		private int width = 150;
		private int height = 20;

		private T minValue, maxValue;
		private T shiftStep, shiftCtrlStep, step, ctrlStep;

		public Builder(Font font, T initValue, Filter inputFilter, Consumer<T> onValueChange, WidgetCreator<T> widgetCreator) {
			this.font = font;
			this.initValue = initValue;
			this.inputFilter = inputFilter;
			this.onValueChange = onValueChange;
			this.widgetCreator = widgetCreator;
		}

		public Builder<T> setBounds(int x, int y, int width, int height) {
			this.setPos(x, y);
			this.setSize(width, height);
			return this;
		}

		public Builder<T> setPos(int x, int y) {
			this.setX(x);
			this.setY(y);
			return this;
		}

		public Builder<T> setX(int x) {
			this.x = x;
			return this;
		}

		public Builder<T> setY(int y) {
			this.y = y;
			return this;
		}

		public Builder<T> setSize(int width, int height) {
			this.setWidth(width);
			this.setHeight(height);
			return this;
		}

		public Builder<T> setWidth(int width) {
			this.width = width;
			return this;
		}

		public Builder<T> setHeight(int height) {
			this.height = height;
			return this;
		}

		public Builder<T> setMinMaxValues(T minValue, T maxValue) {
			this.setMinValue(minValue);
			this.setMaxValue(maxValue);
			return this;
		}

		public Builder<T> setMinValue(T minValue) {
			this.minValue = minValue;
			return this;
		}

		public Builder<T> setMaxValue(T maxValue) {
			this.maxValue = maxValue;
			return this;
		}

		public Builder<T> setSteps(T shiftStep, T shiftCtrlStep, T step, T ctrlStep) {
			this.setShiftStep(shiftStep);
			this.setShiftCtrlStep(shiftCtrlStep);
			this.setStep(step);
			this.setCtrlStep(ctrlStep);
			return this;
		}

		public Builder<T> setShiftStep(T shiftStep) {
			this.shiftStep = shiftStep;
			return this;
		}

		public Builder<T> setShiftCtrlStep(T shiftCtrlStep) {
			this.shiftCtrlStep = shiftCtrlStep;
			return this;
		}

		public Builder<T> setStep(T step) {
			this.step = step;
			return this;
		}

		public Builder<T> setCtrlStep(T ctrlStep) {
			this.ctrlStep = ctrlStep;
			return this;
		}

		public NumberEditBox<T> build() {
			return this.widgetCreator.create(
				this.font, this.x, this.y, this.width, this.height,
				this.initValue, this.minValue, this.maxValue,
				this.shiftStep, this.shiftCtrlStep, this.step, this.ctrlStep,
				this.inputFilter, this.onValueChange
			);
		}

		public interface WidgetCreator<T extends Number> {
			NumberEditBox<T> create(
				Font font, int x, int y, int width, int height,
				T initValue, T minValue, T maxValue,
				T shiftStep, T shiftCtrlStep, T step, T ctrlStep,
				Filter inputFilter, Consumer<T> onValueChange
			);
		}
	}
}
