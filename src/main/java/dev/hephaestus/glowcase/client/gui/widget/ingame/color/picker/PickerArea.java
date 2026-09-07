package dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * Clickable area within a {@link ColorPickerWidget}, such as the hue slider, saturation/light picker, alpha slider, and presets.<br><br>
 * Allows for definable position & dimensions for a clickable area, and a listener for what to do when clicked (e.g. set ColorPicker hue/sat./light/alpha).
 *
 * @see ColorPickerWidget
 */
public class PickerArea {
	private final ColorPickerWidget colorPicker;
	@Nullable
	private final AreaSetter xLerpSetter;
	@Nullable
	private final AreaSetter yLerpSetter;
	private final boolean clickable;
	private int x, y, width, height;
	private int thumbX, thumbY;
	private boolean hasMouseDown = false;

	public PickerArea(ColorPickerWidget colorPicker, boolean clickable) {
		this(colorPicker, false, null, null);
	}

	public PickerArea(ColorPickerWidget colorPicker, @Nullable AreaSetter xLerp) {
		this(colorPicker, true, xLerp, null);
	}

	public PickerArea(ColorPickerWidget colorPicker, @Nullable AreaSetter xLerp, @Nullable AreaSetter yLerp) {
		this(colorPicker, true, xLerp, yLerp);
	}

	public PickerArea(ColorPickerWidget colorPicker, boolean clickable, @Nullable AreaSetter xLerp, @Nullable AreaSetter yLerp) {
		this.colorPicker = colorPicker;
		this.clickable = clickable;
		this.xLerpSetter = xLerp;
		this.yLerpSetter = yLerp;
	}

	public boolean mouseClicked(double mouseX, double mouseY) {
		if (!this.clickable) return false;

		if (isMouseOver(mouseX, mouseY)) {
			this.hasMouseDown = true;
		}

		if (this.hasMouseDown) {
			// Assume horizontal / x movement is possible
			if (this.xLerpSetter != null) {
				this.thumbX = (int) Mth.clamp(mouseX, this.getX(), this.getX2());

				float xLerp = calcLerp(mouseX, this.getX(), this.getX2());
				this.xLerpSetter.set(xLerp);
			}

			// Assume vertical / y movement is possible
			if (this.yLerpSetter != null) {
				this.thumbY = (int) Mth.clamp(mouseY, this.getY(), this.getY2());

				float yLerp = calcLerp(mouseY, this.getY(), this.getY2());
				this.yLerpSetter.set(yLerp);
			}
		}

		return this.hasMouseDown;
	}

	public void mouseReleased() {
		this.hasMouseDown = false;
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return (mouseY > this.getY() && mouseY < this.getY2())
			&& (mouseX > this.getX() && mouseX < this.getX2());
	}

	public boolean shouldOutline(int mouseX, int mouseY) {
		// Outline if mouse down, or if the mouse over AND no other area has the mouse down
		return this.hasMouseDown() || (this.isMouseOver(mouseX, mouseY) && this.getColorPicker().currentClickedArea == null);
	}

	private float calcLerp(double mousePos, int minPos, int maxPos) {
		if (mousePos < minPos) return 0f;
		if (mousePos > maxPos) return 1f;
		float width = maxPos - minPos;
		return Mth.clamp(((float) mousePos - minPos) / width, 0f, 1f);
	}

	public void lerpThumbX(float lerp) {
		this.thumbX = (int) Mth.clamp(this.getX() + (this.getWidth() * lerp), this.getX(), this.getX2());
	}

	public void lerpThumbY(float lerp) {
		this.thumbY = (int) Mth.clamp(this.getY() + (this.getHeight() * lerp), this.getY(), this.getY2());
	}

	// region Getters & Setters
	public void set(int x, int y, int width, int height) {
		this.setPos(x, y);
		this.setDimensions(width, height);
	}

	public void setPos(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void setDimensions(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getX2() {
		return this.getX() + this.getWidth();
	}

	public int getY2() {
		return this.getY() + this.getHeight();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getThumbX() {
		return thumbX;
	}

	public int getThumbY() {
		return thumbY;
	}

	public boolean hasMouseDown() {
		return this.hasMouseDown;
	}

	public ColorPickerWidget getColorPicker() {
		return colorPicker;
	}

	// endregion

	public interface AreaSetter {
		void set(float value);
	}
}
