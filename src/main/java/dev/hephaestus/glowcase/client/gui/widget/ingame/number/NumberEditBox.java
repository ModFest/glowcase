package dev.hephaestus.glowcase.client.gui.widget.ingame.number;

import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.util.InputFilters;
import dev.hephaestus.glowcase.util.ParseUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
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
//		this.setMaxLength(5); // Arbitrary character limit for ease of use
		this.updateEditBox(); // Set text to string of number value
	}

	public abstract void stepUp(boolean shift, boolean ctrl);
	public abstract void stepDown(boolean shift, boolean ctrl);

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isUp()) {
			this.stepUp(event.hasShiftDown(), event.hasControlDownWithQuirk());
			return true;
		} else if (event.isDown()) {
			this.stepDown(event.hasShiftDown(), event.hasControlDownWithQuirk());
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (scrollY > 0) {
			this.stepUp(Minecraft.getInstance().hasShiftDown(), Minecraft.getInstance().hasControlDown());
			return true;
		} else if (scrollY < 0) {
			this.stepDown(Minecraft.getInstance().hasShiftDown(), Minecraft.getInstance().hasControlDown());
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
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
				this.updateEditBox();
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
//		public final Consumer<Float> onValueChange;
//		public float floatValue;

		public FloatEditBox(
			Font font, int x, int y, int width, int height,
			float initValue, float minValue, float maxValue,
			float shiftStep, float shiftCtrlStep, float step, float ctrlStep,
			Filter inputFilter, Consumer<Float> onValueChange
		) {
			super(font, x, y, width, height, initValue, minValue, maxValue, shiftStep, shiftCtrlStep, step, ctrlStep, inputFilter, onValueChange);
//			this.numberValue = initValue;
//			this.onValueChange = onValueChange;
			this.setResponder(s -> {
				this.numberValue = Math.clamp(ParseUtil.parseOrDefault(s, initValue), this.minValue, this.maxValue);
				this.onValueChange.accept(this.numberValue);
			});
//			this.setFilter(InputFilters::realNumber);

//			this.minValue = -Float.MAX_VALUE;
//			this.maxValue = Float.MAX_VALUE;
//			this.shiftStep = 0.01f;
//			this.shiftCtrlStep = 0.125f;
//			this.step = 0.1f;
//			this.ctrlStep = 1.0f;
		}

		@Override
		public void stepUp(boolean shift, boolean ctrl) {
			float step = this.getStep(shift, ctrl);
			float round = 1f / step;
//			float floored = (float) (Math.floor((this.numberValue) * round) / round);
//			this.numberValue = Math.clamp(Math.round((floored + step) * round) / round, this.minValue, this.maxValue);
			this.numberValue = Math.clamp(Math.round((this.numberValue + step) * round) / round, this.minValue, this.maxValue);
			this.updateEditBox();
		}

		@Override
		public void stepDown(boolean shift, boolean ctrl) {
			float step = this.getStep(shift, ctrl);
			float round = 1f / step;
//			float ceilinged = (float) (Math.ceil((this.numberValue) * round) / round);
//			this.numberValue = Math.clamp(Math.round((ceilinged - step) * round) / round, this.minValue, this.maxValue);
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
//			double floored = Math.floor((this.numberValue) * round) / round;
			this.numberValue = Math.clamp(Math.round((this.numberValue + step) * round) / round, this.minValue, this.maxValue);
			this.updateEditBox();
		}

		@Override
		public void stepDown(boolean shift, boolean ctrl) {
			double step = this.getStep(shift, ctrl);
			double round = 1d / step;
//			double ceilinged = Math.ceil((this.numberValue) * round) / round;
//			this.numberValue = Math.clamp(Math.round((ceilinged - step) * round) / round, this.minValue, this.maxValue);
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
// ATTEMPT 2 - I don't know why this didn't work
//public class NumberEditBox<T extends Number> extends GlowcaseEditBox {
//	protected final Function<String, T> parseFunction;
//	protected final StepFunction<T> stepFunction;
//	protected final ClampFunction<T> clampFunction;
//	protected final Consumer<T> onValueChange;
//	protected T numberValue, initValue;
//	protected T minValue, maxValue;
//	// Steps for scrolling or pressing arrow keys.
//	// Per Blender's (and seemingly Microsoft's?) design standards:
//	// shift = precision, control = bigger increments, shift + control = mildly precise increments
//	protected T shiftStep, shiftCtrlStep, step, ctrlStep;
//
//	public static Builder<Integer> integerBuilder(Font font, Integer initValue, Consumer<Integer> onValueChange) {
//		return new Builder<>(
//			font, initValue, InputFilters::integerNumber, onValueChange,
//			s -> ParseUtil.parseOrDefault(s, initValue),
//			(value, stepAmount, subtract) -> value + stepAmount * (subtract ? -1 : 1),
//			Mth::clamp
//		).setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE).setSteps(5, 8, 1, 10);
//	}
//
//	public static Builder<Float> floatBuilder(Font font, Float initValue, Consumer<Float> onValueChange) {
//		return new Builder<>(
//			font, initValue, InputFilters::realNumber, onValueChange,
//			s -> ParseUtil.parseOrDefault(s, initValue),
//			(value, stepAmount, subtract) -> value + stepAmount * (subtract ? -1f : 1f),
//			(value1, min, max) -> (float) (Math.round(Mth.clamp(value1, min, max) * 100) / 100)
//		).setMinMax(Float.MIN_VALUE, Float.MAX_VALUE).setSteps(0.1f, 0.5f, 1f, 10f);
//	}
//
//	public static Builder<Double> doubleBuilder(Font font, Double initValue, Consumer<Double> onValueChange) {
//		return new Builder<>(
//			font, initValue, InputFilters::realNumber, onValueChange,
//			s -> ParseUtil.parseOrDefault(s, initValue),
//			(value, stepAmount, subtract) -> value + (stepAmount * (subtract ? -1 : 1)),
//			(value1, min, max) -> (double) (Math.round(Mth.clamp(value1, min, max) * 100) / 100)
//		).setMinMax(Double.MIN_VALUE, Double.MAX_VALUE).setSteps(0.1, 0.5, 1.0, 10.0);
//	}
//
//	protected NumberEditBox(
//		Font font, int x, int y, int width, int height,
//		T numberValue, T minValue, T maxValue,
//		T shiftStep, T shiftCtrlStep, T step, T ctrlStep,
//		Filter inputFilter, Consumer<T> onValueChange,
//		Function<String, T> parseFunction, StepFunction<T> stepFunction, ClampFunction<T> clampFunction
//	) {
//		super(font, x, y, width, height, Component.empty());
//		this.numberValue = numberValue;
//		this.initValue = numberValue;
//		this.minValue = minValue;
//		this.maxValue = maxValue;
//		this.shiftStep = shiftStep;
//		this.shiftCtrlStep = shiftCtrlStep;
//		this.step = step;
//		this.ctrlStep = ctrlStep;
//		this.onValueChange = onValueChange;
//		this.setFilter(inputFilter);
//
//		this.parseFunction = parseFunction;
//		this.stepFunction = stepFunction;
//		this.clampFunction = clampFunction;
//
//		this.setResponder(s -> {
//			T parsedNumber = this.parseString(s);
//			this.setNumberValue(parsedNumber);
//			this.onValueChange.accept(this.numberValue);
//		});
//
//		this.setNumberValue(this.initValue);
//		this.setValue(String.valueOf(this.initValue));
//	}
//
////	protected abstract T parseString(String string);
////	protected abstract T stepValue(T step, boolean subtract);
////	protected abstract T clampValue(T value);
//
//	@Override
//	public boolean keyPressed(KeyEvent event) {
//		if (event.isUp() || event.isDown()) {
//			T step = this.getStep(event.hasShiftDown(), event.hasControlDownWithQuirk());
//			T newNumberValue = this.stepValue(step, event.isDown());
//			this.setNumberValue(newNumberValue);
//			this.setValue(String.valueOf(newNumberValue));
//			return true;
//		}
//		return super.keyPressed(event);
//	}
//
//	@Override
//	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
//		if (scrollY != 0) {
//			T step = this.getStep(Minecraft.getInstance().hasShiftDown(), Minecraft.getInstance().hasControlDown());
//			T newNumberValue = this.stepValue(step, scrollY <= 0);
//			this.setNumberValue(newNumberValue);
//			this.setValue(String.valueOf(newNumberValue));
//			return true;
//		}
//		return super.mouseScrolled(x, y, scrollX, scrollY);
//	}
//
//	public T getStep(boolean shift, boolean ctrl) {
//		if (shift && ctrl) return this.shiftCtrlStep;
//		if (shift) return this.shiftStep;
//		if (ctrl) return this.ctrlStep;
//		return this.step;
//	}
//
//	public void setNumberValue(T value) {
//		this.numberValue = this.clampValue(value);
//	}
//
//	public T parseString(String string) {
//		return this.parseFunction.apply(string);
//	}
//
//	public T stepValue(T step, boolean subtract) {
//		return this.stepFunction.getStep(this.numberValue, step, subtract);
//	}
//
//	public T clampValue(T value) {
//		return this.clampFunction.clampValue(value, this.minValue, this.maxValue);
//	}
//
//	protected interface StepFunction<T extends Number> {
//		T getStep(T value, T stepAmount, boolean subtract);
//	}
//
//	protected interface ClampFunction<T extends Number> {
//		T clampValue(T value, T min, T max);
//	}
//
////	public static class IntegerEditBox extends NumberEditBox<Integer> {
////		public IntegerEditBox(
////			Font font, int x, int y, int width, int height,
////			Integer numberValue,
////			Integer minValue, Integer maxValue,
////			Integer shiftStep, Integer shiftCtrlStep, Integer step, Integer ctrlStep,
////			Filter inputFilter, Consumer<Integer> onValueChange
////		) {
////			super(font, x, y, width, height, numberValue, minValue, maxValue, shiftStep, shiftCtrlStep, step, ctrlStep, inputFilter, onValueChange);
////		}
////
////		@Override
////		protected Integer parseString(String string) {
////			return ParseUtil.parseOrDefault(string, this.initValue);
////		}
////
////		@Override
////		protected Integer stepValue(Integer step, boolean subtract) {
////			return this.numberValue + step * (subtract ? -1 : 1);
////		}
////
////		@Override
////		protected Integer clampValue(Integer value) {
////			return Mth.clamp(value, this.minValue, this.maxValue);
////		}
////	}
//
//	public static class Builder<T extends Number> {
//		private final Font font;
//		private final Filter inputFilter;
//		private final Consumer<T> onValueChange;
////		private final CreateNumberEditBox<T> creator;
//		private final Function<String, T> parseFunction;
//		private final StepFunction<T> stepFunction;
//		private final ClampFunction<T> clampFunction;
//		private T initValue;
//
//		private int x, y;
//		private int width = 150;
//		private int height = 20;
//
//		private T minValue, maxValue;
//		private T shiftStep, shiftCtrlStep, step, ctrlStep;
//
//		protected Builder(Font font, T initValue, Filter inputFilter, Consumer<T> onValueChange, Function<String, T> parseFunction, StepFunction<T> stepFunction, ClampFunction<T> clampFunction) {
//			this.font = font;
//			this.initValue = initValue;
//			this.inputFilter = inputFilter;
//			this.onValueChange = onValueChange;
//			this.parseFunction = parseFunction;
//			this.stepFunction = stepFunction;
//			this.clampFunction = clampFunction;
////			this.creator = creator;
//		}
//
//		public Builder<T> setBounds(int x, int y, int width, int height) {
//			this.setPos(x, y);
//			this.setSize(width, height);
//			return this;
//		}
//
//		public Builder<T> setPos(int x, int y) {
//			this.setX(x);
//			this.setY(y);
//			return this;
//		}
//
//		public Builder<T> setX(int x) {
//			this.x = x;
//			return this;
//		}
//
//		public Builder<T> setY(int y) {
//			this.y = y;
//			return this;
//		}
//
//		public Builder<T> setSize(int width, int height) {
//			this.setWidth(width);
//			this.setHeight(height);
//			return this;
//		}
//
//		public Builder<T> setWidth(int width) {
//			this.width = width;
//			return this;
//		}
//
//		public Builder<T> setHeight(int height) {
//			this.height = height;
//			return this;
//		}
//
//		public Builder<T> setMinMax(T minValue, T maxValue) {
//			this.setMinValue(minValue);
//			this.setMaxValue(maxValue);
//			return this;
//		}
//
//		public Builder<T> setMinValue(T minValue) {
//			this.minValue = minValue;
//			return this;
//		}
//
//		public Builder<T> setMaxValue(T maxValue) {
//			this.maxValue = maxValue;
//			return this;
//		}
//
//		public Builder<T> setSteps(T shiftStep, T shiftCtrlStep, T step, T ctrlStep) {
//			this.setShiftStep(shiftStep);
//			this.setShiftCtrlStep(shiftCtrlStep);
//			this.setStep(step);
//			this.setCtrlStep(ctrlStep);
//			return this;
//		}
//
//		public Builder<T> setShiftStep(T shiftStep) {
//			this.shiftStep = shiftStep;
//			return this;
//		}
//
//		public Builder<T> setShiftCtrlStep(T shiftCtrlStep) {
//			this.shiftCtrlStep = shiftCtrlStep;
//			return this;
//		}
//
//		public Builder<T> setStep(T step) {
//			this.step = step;
//			return this;
//		}
//
//		public Builder<T> setCtrlStep(T ctrlStep) {
//			this.ctrlStep = ctrlStep;
//			return this;
//		}
//
//		public NumberEditBox<T> build() {
//			return new NumberEditBox<>(
//				this.font,
//				this.x, this.y, this.width, this.height,
//				this.initValue, this.minValue, this.maxValue,
//				this.shiftStep, this.shiftCtrlStep, this.step, this.ctrlStep,
//				this.inputFilter, this.onValueChange,
//				this.parseFunction, this.stepFunction, this.clampFunction
//			);
//		}
//
////		protected interface CreateNumberEditBox<T extends Number> {
////			NumberEditBox<T> create(
////				Font font, int x, int y, int width, int height,
////				T numberValue, T minValue, T maxValue,
////				T shiftStep, T shiftCtrlStep, T step, T ctrlStep,
////				Filter inputFilter, Consumer<T> onValueChange
////			);
////		}
//	}
//
////	public static class IntBuilder extends Builder<Integer> {
////		protected IntBuilder(Font font, Integer initValue, Consumer<Integer> onValueChange) {
////			super(font, initValue, onValueChange, IntegerEditBox::new);
////		}
////	}
//}

// ATTEMPT 1
//public class NumberEditBox<T extends Number> extends GlowcaseEditBox {
//	public final Function<String, T> parseFunction;
//	public final Function<T, T> clampFunction;
//	public final Function3<T, T, Boolean, T> applyStepFunction; // Current value, step value, is negative
//	public final Consumer<T> onValueChange;
//	public T numberValue;
//	public T minValue, maxValue;
//	// Steps for scrolling or pressing arrow keys.
//	// Per Blender's (and seemingly Microsoft's?) design standards:
//	// shift = precision, control = bigger increments, shift + control = mildly precise increments
//	public T shiftStep, shiftCtrlStep, step, ctrlStep;
//
//	public static <T extends Number> Builder<T> builder(Font font, float initValue, Consumer<T> onValueChange) {
//		return new Builder(font, initValue, onValueChange);
//	}
//
//	public NumberEditBox(
//		Font font,
//		int x, int y, int width, int height,
//		T initValue, T minValue, T maxValue,
//		T shiftStep, T shiftCtrlStep, T step, T ctrlStep,
//		Filter inputFilter, Function<String, T> parseFunction, Function<T, T> clampFunction,
//		Function3<T, T, Boolean, T> applyStepFunction, Consumer<T> onValueChange
//	) {
//		super(font, x, y, width, height, Component.empty());
//		this.parseFunction = parseFunction;
//		this.clampFunction = clampFunction;
//		this.applyStepFunction = applyStepFunction;
//		this.onValueChange = onValueChange;
//		this.numberValue = initValue;
//		this.minValue = minValue;
//		this.maxValue = maxValue;
//		this.shiftStep = shiftStep;
//		this.shiftCtrlStep = shiftCtrlStep;
//		this.step = step;
//		this.ctrlStep = ctrlStep;
//
//		this.setFilter(inputFilter);
//		this.setResponder(this::parseNumber);
//	}
//
//	@Override
//	public boolean keyPressed(KeyEvent event) {
//		if (event.isUp()) {
//			T step = this.getStep(event.hasControlDownWithQuirk(), event.hasShiftDown());
//			T newNumber = this.applyStepFunction.apply(this.numberValue, step, false);
//			this.updateNumberValue(newNumber);
////			this.updateNumberValue(this.numberValue += this.getStep(event.hasControlDownWithQuirk(), event.hasShiftDown()));
//			return true;
//		} else if (event.isDown()) {
////			this.updateNumberValue(this.numberValue -= this.getStep(event.hasControlDownWithQuirk(), event.hasShiftDown()));
//			T step = this.getStep(event.hasControlDownWithQuirk(), event.hasShiftDown());
//			T newNumber = this.applyStepFunction.apply(this.numberValue, step, true);
//			this.updateNumberValue(newNumber);
//			return true;
//		}
//		return super.keyPressed(event);
//	}
//
//	@Override
//	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
//		if (scrollY != 0) {
//			T step = this.getStep(Minecraft.getInstance().hasControlDown(), Minecraft.getInstance().hasShiftDown());
//			T newNumber = this.applyStepFunction.apply(this.numberValue, step, scrollY > 0);
//			this.updateNumberValue(newNumber);
////			float step = this.getStep(Minecraft.getInstance().hasControlDown(), Minecraft.getInstance().hasShiftDown());
////			step *= scrollY > 0 ? 1f : -1f;
////			this.updateNumberValue(this.numberValue += step);
//			return true;
//		}
//		return super.mouseScrolled(x, y, scrollX, scrollY);
//	}
//
//	private T getStep(boolean ctrl, boolean shift) {
//		if (shift && ctrl) return this.shiftCtrlStep;
//		if (shift) return this.shiftStep;
//		if (ctrl) return this.ctrlStep;
//		return this.step;
//	}
//
//	public void updateNumberValue(T value) {
//		this.numberValue = this.clampFunction.apply(value);
//		this.setValue(String.valueOf(this.numberValue));
//	}
//
//	public void parseNumber(String inputString) {
//		T parsedValue = this.parseFunction.apply(inputString);
//		this.updateNumberValue(parsedValue);
//	}
//
//	public static class Builder<T extends Number> {
//		public final Font font;
//		public final Consumer<T> onValueChange;
//		public T initValue;
//
//		public int x, y;
//		public int width = 50;
//		public int height = 20;
//
//		public T min, max;
//		public T shiftStep, shiftCtrlStep, step, ctrlStep;
//
//		public Builder(Font font, T initValue, Consumer<T> onValueChange) {
//			this.font = font;
//			this.initValue = initValue;
//			this.onValueChange = onValueChange;
//		}
//
//		public Builder<T> bounds(int x, int y, int width, int height) {
//			this.pos(x, y);
//			this.size(width, height);
//			return this;
//		}
//
//		public Builder<T> pos(int x, int y) {
//			this.setX(x);
//			this.setY(y);
//			return this;
//		}
//
//		public Builder<T> setX(int x) {
//			this.x = x;
//			return this;
//		}
//
//		public Builder<T> setY(int y) {
//			this.y = y;
//			return this;
//		}
//
//		public Builder<T> size(int width, int height) {
//			this.setWidth(width);
//			this.setHeight(height);
//			return this;
//		}
//
//		public Builder<T> setWidth(int width) {
//			this.width = width;
//			return this;
//		}
//
//		public Builder<T> setHeight(int height) {
//			this.height = height;
//			return this;
//		}
//
//		public Builder<T> setMinMax(T min, T max) {
//			this.setMin(min);
//			this.setMax(max);
//			return this;
//		}
//
//		public Builder<T> setMin(T min) {
//			this.min = min;
//			return this;
//		}
//
//		public Builder<T> setMax(T max) {
//			this.max = max;
//			return this;
//		}
//
//		public Builder<T> setSteps(T shiftStep, T shiftCtrlStep, T step, T ctrlStep) {
//			this.setShiftStep(shiftStep);
//			this.setShiftCtrlStep(shiftCtrlStep);
//			this.setStep(step);
//			this.setCtrlStep(ctrlStep);
//			return this;
//		}
//
//		public Builder<T> setShiftStep(T shiftStep) {
//			this.shiftStep = shiftStep;
//			return this;
//		}
//
//		public Builder<T> setShiftCtrlStep(T shiftCtrlStep) {
//			this.shiftCtrlStep = shiftCtrlStep;
//			return this;
//		}
//
//		public Builder<T> setStep(T step) {
//			this.step = step;
//			return this;
//		}
//
//		public Builder<T> setCtrlStep(T ctrlStep) {
//			this.ctrlStep = ctrlStep;
//			return this;
//		}
//
//		public NumberEditBox<T> build() {
//			return new NumberEditBox<>(
//				this.font,
//				this.x, this.y, this.width, this.height,
//				this.initValue, this.min, this.max,
//				this.shiftStep, this.shiftCtrlStep, this.step, this.ctrlStep,
//				this.onValueChange
//			);
//		}
//	}
//}
