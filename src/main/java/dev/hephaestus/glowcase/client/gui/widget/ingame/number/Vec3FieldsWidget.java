package dev.hephaestus.glowcase.client.gui.widget.ingame.number;

import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.client.util.Tooltips;
import dev.hephaestus.glowcase.util.CollectionUtils;
import dev.hephaestus.glowcase.util.Components;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;

public class Vec3FieldsWidget extends AbstractContainerWidget {

	private static final int X_COLOR = ARGB.color(255, 75, 75);
	private static final int Y_COLOR = ARGB.color(75, 255, 75);
	private static final int Z_COLOR = ARGB.color(75, 75, 255);

	private final Font font;
	private final GlowcaseEditBox xEditBox;
	private final GlowcaseEditBox yEditBox;
	private final GlowcaseEditBox zEditBox;

	private final boolean colored;
	private final boolean isRotation;
	private final int editBoxCharacterLimit;
	@Nullable
	private final List<Tooltip> tooltips;
	@Nullable
	private final List<Component> hints;
	@Nullable
	private final Consumer<Vec3> onValueChange;

	private Vec3 value;

	public static Builder builder(Font font, Vector3f value) {
		return new Builder(font, new Vec3(value));
	}

	public static Builder builder(Font font, Vec3 value) {
		return new Builder(font, value);
	}

	public Vec3FieldsWidget(
		Font font, Vec3 defaultValue,
		int x, int y, int width, int height,
		boolean colored, boolean isRotation, int editBoxCharacterLimit,
		@Nullable List<Tooltip> tooltips, @Nullable List<Component> hints, @Nullable Consumer<Vec3> onValueChange
	) {
		super(x, y, width, height, Component.empty(),  AbstractScrollArea.defaultSettings(10));
		this.font = font;
		this.colored = colored;
		this.isRotation = isRotation;
		this.tooltips = CollectionUtils.ofSizedElse(isRotation ? Tooltips.ROTATIONS : Tooltips.AXES, tooltips);
		this.hints = CollectionUtils.ofSizedElse(isRotation ? Components.ROTATIONS : Components.AXES, hints);
		this.editBoxCharacterLimit = editBoxCharacterLimit;
		this.onValueChange = onValueChange;

		this.value = defaultValue;

		this.xEditBox = this.createEditBox(0);
		this.yEditBox = this.createEditBox(1);
		this.zEditBox = this.createEditBox(2);
	}

	private GlowcaseEditBox createEditBox(int index) {
		double[] initValues = { this.value.x(), this.value.y(), this.value.z() };
		List<FloatConsumer> changeListeners = List.of(
			xUpdate -> this.updateValue(new Vec3(xUpdate, this.value.y(), this.value.z())),
			yUpdate -> this.updateValue(new Vec3(this.value.x(), yUpdate, this.value.z())),
			zUpdate -> this.updateValue(new Vec3(this.value.x(), this.value.y(), zUpdate))
		);

		int width = this.getWidth() / 3;
		int height = this.getHeight();
		int x = this.getX() + (width * index);
		int y = this.getY();

		GlowcaseEditBox editBox;
		double initValue = initValues[index];
		if (this.isRotation) {
			editBox = NumberEditBox.degreeFloatBuilder(this.font, (float) initValue, changeListeners.get(index))
				.setBounds(x, y, width, height)
				.build();
		} else {
			editBox = NumberEditBox.floatBuilder(this.font, (float) initValue, changeListeners.get(index))
				.setBounds(x, y, width, height)
				.build();
		}
		editBox.setMaxLength(this.editBoxCharacterLimit);

		if (this.colored) {
			int[] textColors = {X_COLOR, Y_COLOR, Z_COLOR};
			editBox.setTextColor(textColors[index]);
		}
		if (this.tooltips != null) {
			editBox.setTooltip(this.tooltips.get(index));
		}
		if (this.hints != null) {
			editBox.setHint(this.hints.get(index));
		}
		return editBox;
	}

	public void updateValue(Vec3 value) {
		this.value = value;
		if (this.onValueChange != null) {
			this.onValueChange.accept(this.value);
		}
	}

	public void positionWidgets() {
		this.xEditBox.setPosition(this.getX(), this.getY());
		this.yEditBox.setPosition(this.getX() + this.getWidth() / 3, this.getY());
		this.zEditBox.setPosition(this.getX() + (this.getWidth() / 3 * 2), this.getY());
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		this.positionWidgets();
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		this.positionWidgets();
	}

	/**
	 * Directly sets the edit boxes without calling the value change listener.
	 *
	 * @deprecated Use {@link #updateValue(Vec3)}
	 */
	@Deprecated
	public void setVec(Vec3 newVec) {
		this.xEditBox.setValue(String.valueOf(newVec.x));
		this.yEditBox.setValue(String.valueOf(newVec.y));
		this.zEditBox.setValue(String.valueOf(newVec.z));
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		xEditBox.extractRenderState(graphics, mouseX, mouseY, delta);
		yEditBox.extractRenderState(graphics, mouseX, mouseY, delta);
		zEditBox.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		if (this.getChildAt(mx, my).isPresent()
			&& this.getChildAt(mx, my).get().mouseScrolled(mx, my, scrollX, scrollY)) return true;
		return super.mouseScrolled(mx, my, scrollX, scrollY);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput builder) {
		xEditBox.updateWidgetNarration(builder);
		yEditBox.updateWidgetNarration(builder);
		zEditBox.updateWidgetNarration(builder);
	}

	public Vec3 value() {
		return this.value;
	}

	@Override
	protected int contentHeight() {
		return 9 + 4; //FIXME: get this right
	}

	@Override
	protected double scrollRate() {
		return 9.0 / 2.0;
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return List.of(xEditBox, yEditBox, zEditBox);
	}

	public static class Builder {
		private final Font font;
		private final Vec3 value;

		private int x, y;
		private int width = 150;
		private int height = 20;

		private boolean colored = true;
		private boolean rotation = false;
		private int editBoxCharacterLimit = 7;

		@Nullable
		private List<Tooltip> tooltips = null;
		@Nullable
		private List<Component> hints = null;
		@Nullable
		private Consumer<Vec3> onValueChange = null;

		public Builder(Font font, Vec3 value) {
			this.font = font;
			this.value = value;
		}

		@CheckReturnValue
		public Builder setPos(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
		}

		@CheckReturnValue
		public Builder setWidth(int width) {
			this.width = width;
			return this;
		}

		@CheckReturnValue
		public Builder setHeight(int height) {
			this.height = height;
			return this;
		}

		@CheckReturnValue
		public Builder setSize(int width, int height) {
			return this.setWidth(width).setHeight(height);
		}

		@CheckReturnValue
		public Builder setTooltips(Tooltip xTooltip, Tooltip yTooltip, Tooltip zTooltip) {
			this.tooltips = List.of(xTooltip, yTooltip, zTooltip);
			return this;
		}

		@CheckReturnValue
		public Builder setTooltips(List<Tooltip> tooltips) {
			if (tooltips.size() != 3) {
				throw new IllegalArgumentException("Must be 3 for XYZ or PYR");
			}
			this.tooltips = List.copyOf(tooltips);
			return this;
		}

		@CheckReturnValue
		public Builder setHints(Component xHint, Component yHint, Component zHint) {
			this.hints = List.of(xHint, yHint, zHint);
			return this;
		}


		@CheckReturnValue
		public Builder setHints(List<Component> hints) {
			if (hints.size() != 3) {
				throw new IllegalArgumentException("Must be 3 for XYZ or PYR");
			}
			this.hints = List.copyOf(hints);
			return this;
		}

		@CheckReturnValue
		public Builder setColored(boolean colored) {
			this.colored = colored;
			return this;
		}

		@CheckReturnValue
		public Builder setRotation(boolean rotation) {
			this.rotation = rotation;
			return this;
		}

		@CheckReturnValue
		public Builder setEditBoxCharacterLimit(int editBoxCharacterLimit) {
			this.editBoxCharacterLimit = editBoxCharacterLimit;
			return this;
		}

		@CheckReturnValue
		public Builder setOnValueChange(@Nullable Consumer<Vec3> onValueChange) {
			this.onValueChange = onValueChange;
			return this;
		}

		@CheckReturnValue
		public Vec3FieldsWidget build() {
			return new Vec3FieldsWidget(
				this.font, this.value,
				this.x, this.y, this.width, this.height,
				this.colored, this.rotation, this.editBoxCharacterLimit,
				this.tooltips, this.hints, this.onValueChange
			);
		}
	}
}
