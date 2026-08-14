package dev.hephaestus.glowcase.client.gui.widget.ingame.number;

import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseEditBox;
import dev.hephaestus.glowcase.util.InputFilters;

import java.util.List;
import java.util.function.Consumer;

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
import org.jetbrains.annotations.Nullable;

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
	private Tooltip[] tooltips;
	@Nullable
	private final Consumer<Vec3> onValueChange;

	private Vec3 value;

	public static Builder builder(Font font, Vec3 value) {
		return new Builder(font, value);
	}

	public Vec3FieldsWidget(
		Font font, Vec3 defaultValue,
		int x, int y, int width, int height,
		boolean colored, boolean isRotation, int editBoxCharacterLimit,
		@Nullable Tooltip[] tooltips, @Nullable Consumer<Vec3> onValueChange
	) {
		super(x, y, width, height, Component.empty(),  AbstractScrollArea.defaultSettings(10));
		this.font = font;
		this.colored = colored;
		this.isRotation = isRotation;
		this.tooltips = tooltips;
		this.editBoxCharacterLimit = editBoxCharacterLimit;
		this.onValueChange = onValueChange;

		this.value = defaultValue;

		this.xEditBox = this.createEditBox(0, defaultValue.x());
		this.yEditBox = this.createEditBox(1, defaultValue.y());
		this.zEditBox = this.createEditBox(2, defaultValue.z());

//		this.xEditBox.setValue(String.valueOf(defaultValue.x));
//		this.yEditBox.setValue(String.valueOf(defaultValue.y));
//		this.zEditBox.setValue(String.valueOf(defaultValue.z));
//
//		this.xEditBox.setResponder(s -> this.updateValue(new Vec3(ParseUtil.parseOrDefault(s, value.x), value.y , value.z)));
//		this.yEditBox.setResponder(s -> this.updateValue(new Vec3(value.x, ParseUtil.parseOrDefault(s, value.y), value.z)));
//		this.zEditBox.setResponder(s -> this.updateValue(new Vec3(value.x, value.y, ParseUtil.parseOrDefault(s, value.z))));
	}

	private GlowcaseEditBox createEditBox(int index, double initValue) {
		int width = this.getWidth() / 3;
		int height = this.getHeight();
		int x = this.getX() + (width * index);
		int y = this.getY();

		List<Consumer<Float>> changeListeners = List.of(
			xUpdate -> this.updateValue(new Vec3(xUpdate, this.value.y(), this.value.z())),
			yUpdate -> this.updateValue(new Vec3(this.value.x(), yUpdate, this.value.z())),
			zUpdate -> this.updateValue(new Vec3(this.value.x(), this.value.y(), zUpdate))
		);
		GlowcaseEditBox editBox;
		if (this.isRotation) {
//			editBox = new DegreeRotationEditBox(this.font, x, y, width, height, Component.empty());
			editBox = NumberEditBox.degreeFloatBuilder(this.font, (float) initValue, changeListeners.get(index))
				.setBounds(x, y, width, height)
				.build();
		} else {
//			editBox = new GlowcaseEditBox(this.font, x, y, width, height, Component.empty());;
//			editBox = NumberEditBox.doubleBuilder(this.font, initValue, changeListeners.get(index))
//				.setBounds(x, y, width, height).build();
//			editBox = new NumberEditBox.FloatEditBox(this.font, x, y, width, height, (float) initValue, changeListeners.get(index));
//			editBox = new NumberEditBox.FloatEditBox(
//				this.font, x, y, width, height,
//				(float) initValue, 0, Float.MAX_VALUE,
//				0.01f, 0.125f, 0.1f, 1.0f,
//				changeListeners.get(index)
//			);
			editBox = NumberEditBox.floatBuilder(this.font, (float) initValue, changeListeners.get(index))
				.setBounds(x, y, width, height)
				.build();
		}

		editBox.setMaxLength(this.editBoxCharacterLimit);

		editBox.setFilter(InputFilters::realNumber);
		if (this.colored) {
			int[] textColors = {X_COLOR, Y_COLOR, Z_COLOR};
			editBox.setTextColor(textColors[index]);
		}
		if (this.tooltips != null && this.tooltips.length == 3) {
			editBox.setTooltip(this.tooltips[index]);
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

//	public void setVec(Vec3 newVec) {
//		this.xEditBox.setValue(String.valueOf(newVec.x));
//		this.yEditBox.setValue(String.valueOf(newVec.y));
//		this.zEditBox.setValue(String.valueOf(newVec.z));
//	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		xEditBox.extractRenderState(graphics, mouseX, mouseY, delta);
		yEditBox.extractRenderState(graphics, mouseX, mouseY, delta);
		zEditBox.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		if (this.getChildAt(mx, my).isPresent() && this.getChildAt(mx, my).get().mouseScrolled(mx, my, scrollX, scrollY)) return true;
//		if (this.xEditBox.mouseScrolled(mx, my, scrollX, scrollY)
//			|| this.yEditBox.mouseScrolled(mx, my, scrollX, scrollY)
//			|| this.zEditBox.mouseScrolled(mx, my, scrollX, scrollY)
//		) return true;
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
		private Tooltip[] tooltips = null;
		@Nullable
		private Consumer<Vec3> onValueChange = null;

		public Builder(Font font, Vec3 value) {
			this.font = font;
			this.value = value;
		}

		public Builder setPos(int x, int y) {
			this.x = x;
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

		public void setSize(int width, int height) {
			this.setWidth(width);
			this.setHeight(height);
		}
		public Builder setTooltips(Tooltip xTooltip, Tooltip yTooltip, Tooltip zTooltip) {
			this.tooltips = new Tooltip[3];
			this.tooltips[0] = xTooltip;
			this.tooltips[1] = yTooltip;
			this.tooltips[2] = zTooltip;
			return this;
		}

		public Builder setColored(boolean colored) {
			this.colored = colored;
			return this;
		}

		public Builder setRotation(boolean rotation) {
			this.rotation = rotation;
			return this;
		}

		public Builder setEditBoxCharacterLimit(int editBoxCharacterLimit) {
			this.editBoxCharacterLimit = editBoxCharacterLimit;
			return this;
		}

		public Builder setOnValueChange(@Nullable Consumer<Vec3> onValueChange) {
			this.onValueChange = onValueChange;
			return this;
		}

		public Vec3FieldsWidget build() {
			return new Vec3FieldsWidget(
				this.font, this.value,
				this.x, this.y, this.width, this.height,
				this.colored, this.rotation, this.editBoxCharacterLimit,
				this.tooltips, this.onValueChange
			);
		}
	}
}
