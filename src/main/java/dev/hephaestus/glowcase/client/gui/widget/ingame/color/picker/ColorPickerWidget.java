package dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.gui.screen.ingame.ColorPickerIncludedScreen;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.functional.ColorSetter;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.functional.PickerPresetListener;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import dev.hephaestus.glowcase.client.util.GuiGraphicsUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;

/**
 * Main color picker widget, including a preview box, saturation & value box, hue slider, optional alpha slider, and color presets.<br><br>
 * Each {@link ColorPickerIncludedScreen} has *one* Color Picker, which each widget uses. This means that position, showAlpha, and color values will be updated constantly.<br><br>
 * @author Superkat32
 */
public class ColorPickerWidget extends AbstractButton {
	// To keep things simple (and keep me sane), sizes are fully locked and hardcoded
	// Width calculated by: ((presetScale + padding) * presetsPerLine) + padding -> ((16 + 2) * 10) + 2
	// Height calculated by: Whatever my heart desired at the moment -> 116 and 146, apparently
	// Alpha height difference: (presetScale + padding) + (alphaHeight + padding) -> (16 + 2) + (10 + 2)
	private static final int DEFAULT_WIDTH = 182;
	private static final int DEFAULT_HEIGHT = 116;
	private static final int DEFAULT_ALPHA_HEIGHT = 146;
	private static final Identifier BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");

	// Different alpha textures have different tile sizes to better match their background
	private static final Identifier ALPHA_PREVIEW_TEXTURE = Glowcase.id("color_picker/alpha_preview");
	private static final Identifier ALPHA_SLIDER_TEXTURE = Glowcase.id("color_picker/alpha_slider");
	private static final Identifier ALPHA_THUMB_TEXTURE = Glowcase.id("color_picker/alpha_thumb");
	private static final Identifier ALPHA_PRESET_TEXTURE = Glowcase.id("color_picker/alpha_preset");

	public final ColorPickerIncludedScreen screen;
	@Nullable
	public GuiEventListener targetElement = null;
	@Nullable
	public ColorSetter pickedColorListener = null;
	@Nullable
	public PickerPresetListener presetListener = null;
	@Nullable
	public Consumer<Integer> confirmListener = null;
	public boolean showAlpha = false;
	public float minAlpha = 0f;

	public final List<PickerArea> clickableAreas;
	public PickerArea previewArea;
	public PickerArea satValueArea;
	public PickerArea hueArea;
	public PickerArea alphaArea;
	public PickerArea presetsArea;
	public List<PickerPreset> formattingPresetAreas;
	public List<PickerPreset> alphaPresetAreas;
	public List<PickerButton> buttonAreas;

	@Nullable
	public PickerArea currentClickedArea = null; // Used for allowing mouse drags beyond an area's boundaries

	public float hue, saturation, value, alpha;
	public float prevHue, prevSaturation, prevValue, prevAlpha; // Used for cancelling, *not* interpolation

	public static ColorPickerWidget.Builder builder(ColorPickerIncludedScreen screen) {
		return new Builder(screen);
	}

	public ColorPickerWidget(ColorPickerIncludedScreen screen, int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
		this.screen = screen;

		this.previewArea = new PickerArea(this, false);
		this.satValueArea = new PickerArea(this, xLerp -> this.saturation = xLerp, yLerp -> this.value = 1f - yLerp);
		this.hueArea = new PickerArea(this, xLerp -> this.hue = xLerp);
		this.alphaArea = new PickerArea(this, xLerp -> this.alpha = (1f - this.minAlpha) * xLerp + this.minAlpha);
		this.presetsArea = new PickerArea(this, false);
		this.formattingPresetAreas = PickerPreset.createFormattingPresets(this);
		this.alphaPresetAreas = PickerPreset.createAlphaPresets(this);
		this.buttonAreas = PickerButton.createButtons(this);
		this.updateAreas();

		this.clickableAreas = List.of(this.satValueArea, this.hueArea, this.alphaArea);
		this.hide(); // Start hidden
	}

	// Preset & button width & height: 16
	// Presets per row: 10 (2 rows by default, 3 rows if alpha is shown)
	// Hue & Alpha slider heights: 10
	// Preview & Sat/value picker: Whatever height is left
	// Preview width: 1/3 of picker padded width
	// Sat/value picker: Whatever width is left from preview
	// Padding among everything: 2
	public void updateAreas() {
		int paddedX = this.getX() + 2;
		int bottomY = this.getY() + this.getHeight() - 2;
		int paddedWidth = this.getWidth() - 4;

		int presetsHeight = (16) * (this.showAlpha ? 3 : 2) + (this.showAlpha ? 4 : 2);
		this.presetsArea.set(paddedX, bottomY - presetsHeight, paddedWidth, presetsHeight);
		this.alphaArea.set(paddedX, presetsArea.getY() - 10 - 2, paddedWidth, this.showAlpha ? 10 : 0);
		this.hueArea.set(paddedX, alphaArea.getY() - (showAlpha  ? 10 + 2 : 0), paddedWidth, 10);

		int remainingHeight = this.hueArea.getY() - this.getY() - 4;
		int remainingY = hueArea.getY() - remainingHeight - 2;
		this.previewArea.set(paddedX, remainingY, paddedWidth / 3, remainingHeight);
		int satValueWidth = this.getX() + this.getWidth() - previewArea.getX2() - 4;
		this.satValueArea.set(previewArea.getX2() + 2, remainingY, satValueWidth, remainingHeight);

		for (int i = 0; i < this.formattingPresetAreas.size(); i++) {
			PickerPreset preset = this.formattingPresetAreas.get(i);
			int x = paddedX + ((i % 10) * 18);
			int y = presetsArea.getY() + (18 * (i / 10));
			preset.set(x, y, 16, 16);
		}

		if (this.showAlpha) {
			for (int i = 0; i < this.alphaPresetAreas.size(); i++) {
				 PickerPreset preset = this.alphaPresetAreas.get(i);
				 int x = paddedX + ((i % 10) * 18);
				 int y = presetsArea.getY() + (18 * ((i / 10) + 2));
				 preset.set(x, y, 16, 16);
			}
		}

		for (int i = 0; i < this.buttonAreas.size(); i++) {
			// First button is confirm, second is cancel, so start from far right and shift left
			PickerButton button = this.buttonAreas.get(i);
			int x = this.getX() + this.getWidth() - (18 * (i + 1));
			int y = this.presetsArea.getY() + (18 * (this.showAlpha ? 2 : 1));
			button.set(x, y, 16, 16);
		}
	}

	// Position the thumbs based on the current color
	public void updateAreaThumbs() {
		this.hueArea.lerpThumbX(this.hue);
		this.satValueArea.lerpThumbX(this.saturation);
		this.satValueArea.lerpThumbY(1f - this.value);
		if (this.showAlpha) this.alphaArea.lerpThumbX((this.alpha - this.minAlpha) / (1f - this.minAlpha));
	}

	// region R.E.P.O - Render, Extract, and Position Operations
	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		// Background
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(), 32, 32);

		// Render areas
		this.extractPreviewArea(this.previewArea, graphics);
		this.extractSatValueArea(this.satValueArea, graphics, mouseX, mouseY);
		this.extractHueArea(this.hueArea, graphics, mouseX, mouseY);
		if (showAlpha) this.extractAlphaArea(this.alphaArea, graphics, mouseX, mouseY);
		for (PickerPreset preset : this.formattingPresetAreas) {
			this.extractPresetArea(preset, graphics, false, mouseX, mouseY);
		}
		if (showAlpha) {
			for (PickerPreset preset : this.alphaPresetAreas) {
				this.extractPresetArea(preset, graphics, true, mouseX, mouseY);
			}
		}
		for (PickerButton button : buttonAreas) {
			this.extractButtonArea(button, graphics, mouseX, mouseY);
		}

		// Render thumbs (to ensure they are above everything despite overlaps)
		this.extractThumb(graphics, this.hueArea.getThumbX(), this.hueArea.getY() + 5, 6, 12, this.getHueColor());
		if (showAlpha) this.extractThumb(graphics, this.alphaArea.getThumbX(), this.alphaArea.getY() + 5, 6, 12, this.getColor(), true);
		this.extractThumb(graphics, this.satValueArea.getThumbX(), this.satValueArea.getThumbY(), 8, 8, this.getColorNoAlpha());
	}

	public void extractPreviewArea(PickerArea area, GuiGraphicsExtractor graphics) {
		// Alpha background texture (if needed)
		if (this.showAlpha) GuiGraphicsUtil.drawPreciseTile(graphics, ALPHA_PREVIEW_TEXTURE, area.getX(), area.getY(), area.getX2(), area.getY2(), 8, 7);
		// Current color
		graphics.fill(area.getX(), area.getY(), area.getX2(), area.getY2(), this.getColor());
	}

	public void extractSatValueArea(PickerArea area, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// White to current hue, left to right
		GuiGraphicsUtil.extractHorizontalGradient(graphics, area.getX(), area.getY(), area.getX2(), area.getY2(), ColorUtil.WHITE, this.getHueColor());
		// Transparent to black, top to bottom
		graphics.fillGradient(area.getX(), area.getY(), area.getX2(), area.getY2(), ColorUtil.TRANSPARENT, ColorUtil.BLACK);
		// Outline
		if (area.shouldOutline(mouseX, mouseY)) {
			graphics.outline(area.getX(), area.getY(), area.getWidth(), area.getHeight(), ColorUtil.WHITE);
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	public void extractHueArea(PickerArea area, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// Hue gradient, starting and ending on red
		GuiGraphicsUtil.extractHueGradient(graphics, area.getX(), area.getY(), area.getX2(), area.getY2());
		// Outline
		if (area.shouldOutline(mouseX, mouseY)) {
			graphics.outline(area.getX(), area.getY(), area.getWidth(), area.getHeight(), ColorUtil.WHITE);
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	public void extractAlphaArea(PickerArea area, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// Alpha background texture
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ALPHA_SLIDER_TEXTURE, area.getX(), area.getY(), area.getWidth(), area.getHeight());
		// Transparent to current color, left to right
		GuiGraphicsUtil.extractHorizontalGradient(graphics, area.getX(), area.getY(), area.getX2(), area.getY2(), ColorUtil.TRANSPARENT, this.getColorNoAlpha());
		// Outline
		if (area.shouldOutline(mouseX, mouseY)) {
			graphics.outline(area.getX(), area.getY(), area.getWidth(), area.getHeight(), ColorUtil.WHITE);
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	public void extractPresetArea(PickerPreset preset, GuiGraphicsExtractor graphics, boolean alphaPreset, int mouseX, int mouseY) {
		// Alpha background texture (if needed)
		if (alphaPreset) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ALPHA_PRESET_TEXTURE, preset.getX(), preset.getY(), preset.getWidth(), preset.getHeight());
		graphics.fill(preset.getX(), preset.getY(), preset.getX2(), preset.getY2(), preset.getPresetColor());
		// Alpha preset tooltip
		if (alphaPreset) {
			// I'll be honest I have no clue how this works, I just got lucky
			String hex = String.format("%1$02X", ARGB.alpha(preset.getPresetColor()));
			graphics.text(Minecraft.getInstance().font, hex, preset.getX() + 1, preset.getY() + 7, ColorUtil.WHITE);
		}
		// Outline
		if (preset.shouldOutline(mouseX, mouseY)) {
			graphics.outline(preset.getX(), preset.getY(), preset.getWidth(), preset.getHeight(), ColorUtil.WHITE);
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	public void extractButtonArea(PickerButton button, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, button.getTexture(mouseX, mouseY), button.getX() - button.getPadX(), button.getY() - button.getPadY(), button.getWidth() + button.getPadScale(), button.getHeight() + button.getPadScale());
		if (button.isMouseOver(mouseX, mouseY)) graphics.requestCursor(CursorTypes.POINTING_HAND);
	}

	public void extractThumb(GuiGraphicsExtractor graphics, int thumbX, int thumbY, int thumbWidth, int thumbHeight, int thumbColor) {
		this.extractThumb(graphics, thumbX, thumbY, thumbWidth, thumbHeight, thumbColor, false);
	}

	public void extractThumb(GuiGraphicsExtractor graphics, int thumbX, int thumbY, int thumbWidth, int thumbHeight, int thumbColor, boolean alphaBackground) {
		int halfWidth = thumbWidth / 2;
		int halfHeight = thumbHeight / 2;

		int x = thumbX - halfWidth;
		int y = thumbY - halfHeight;
		int x2 = thumbX + halfWidth;
		int y2 = thumbY + halfHeight;

		if (alphaBackground) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ALPHA_THUMB_TEXTURE, x, y, thumbWidth, thumbHeight);
		graphics.fill(x, y, x2, y2, thumbColor);
		graphics.outline(x, y, thumbWidth, thumbHeight, ColorUtil.WHITE);
	}

	@Override
	protected void handleCursor(GuiGraphicsExtractor graphics) {
		// NO-OP
	}

	// endregion

	public void target(AbstractWidget widget, int initColor, boolean showAlpha, ColorSetter pickedColorListener) {
		this.target(widget, initColor, showAlpha, 0f, false, pickedColorListener);
	}

	public void target(AbstractWidget widget, int initColor, boolean showAlpha, float minAlpha, ColorSetter pickedColorListener) {
		this.target(widget, initColor, showAlpha, minAlpha, false, pickedColorListener);
	}

	public void target(AbstractWidget widget, int initColor, boolean showAlpha, boolean rightAligned, ColorSetter pickedColorListener) {
		this.target(widget, initColor, showAlpha, 0f, rightAligned, pickedColorListener);
	}

	/**
	 * Position & set up the Color Picker based on a widget and its needs. Positioning accounts for screen size, ensuring the color picker never goes off-screen.
	 * @param widget The Widget to position the Color Picker too.
	 * @param initColor The initial color the Color Picker should be, and should revert to if canceled/undone.
	 * @param showAlpha Whether the alpha should be editable, and the alpha slider visible.
	 * @param minAlpha The minimum allowed alpha value of the alpha slider.
	 * @param rightAligned If the Color Picker Widget should align itself to the right side of the widget instead of the left side  .
	 * @param pickedColorListener What to do with the picked color, as an integer.
	 */
	public void target(AbstractWidget widget, int initColor, boolean showAlpha, @Range(from = 0, to = 1) float minAlpha, boolean rightAligned, ColorSetter pickedColorListener) {
		Screen screen = Minecraft.getInstance().screen;
		this.visible = true;
		this.active = true;
		this.targetElement = widget;

		// Ensure at least 2 pixels of padding between screen edges
		int x = Math.min(screen.width - this.getWidth() - 2, widget.getX());
		if (rightAligned) x = Math.max(2, widget.getX() + widget.getWidth() - this.getWidth() + 2);
		int y = widget.getY() + widget.getHeight(); // Beneath widget
		if (y + this.getHeight() > screen.height - 2) y = widget.getY() - this.getHeight(); // Above widget
		this.setX(x);
		this.setY(y);

		this.setPrevColor(initColor);
		this.setColor(initColor);

		this.showAlpha = showAlpha;
		this.minAlpha = minAlpha;
		this.setHeight(this.showAlpha ? DEFAULT_ALPHA_HEIGHT : DEFAULT_HEIGHT);

		this.pickedColorListener = pickedColorListener;
		this.presetListener = preset -> { // Default preset behaviour, just set the picker color
			if (preset.isAlphaPreset()) {
				this.alpha = Math.max(preset.getAlpha(), this.minAlpha);
				this.updateAreaThumbs();
			} else {
				this.setColor(preset.getPresetColor(), false);
			}
		};
		this.confirmListener = null;

		this.updateAreas();
		this.updateAreaThumbs();
	}

	public void setPresetListener(PickerPresetListener presetListener) {
		this.presetListener = presetListener;
	}

	public void setConfirmListener(Consumer<Integer> confirmListener) {
		this.confirmListener = confirmListener;
	}

	public void setColor(int color) {
		this.setColor(color, true);
	}

	/**
	 * Set the Color Picker's color from an integer. Intended for something like the {@link dev.hephaestus.glowcase.client.gui.widget.ingame.color.HexColorEditBox} when a user edits the HEX string, and the color picker needs to reflect that update.
	 */
	public void setColor(int color, boolean setAlpha) {
		float[] HSVA = ColorUtil.ARGBToHSVA(color);
		this.hue = HSVA[0];
		this.saturation = HSVA[1];
		this.value = HSVA[2];
		if (setAlpha) this.alpha = HSVA[3];

		this.updateAreaThumbs();
	}

	/**
	 * The color to revert/undo to in case the user presses cancel.
	 */
	public void setPrevColor(int color) {
		float[] HSVA = ColorUtil.ARGBToHSVA(color);
		this.prevHue = HSVA[0];
		this.prevSaturation = HSVA[1];
		this.prevValue = HSVA[2];
		this.prevAlpha = HSVA[3];
	}

	public void confirm() {
		if (this.confirmListener != null) this.confirmListener.accept(this.getColor());
		this.hide();
	}

	public void cancel() {
		int prevColor = ColorUtil.HSVAtoARGB(this.prevHue, this.prevSaturation, this.prevValue, this.showAlpha ? this.prevAlpha : 1f);
		this.setColor(prevColor);
		this.onColorPicked();
		this.hide();
	}

	public void hide() {
		this.active = false;
		this.visible = false;
		this.setFocused(false);

		this.pickedColorListener = null;
		this.presetListener = null;
		this.confirmListener = null;
	}

	@Override
	public void onClick(@NonNull MouseButtonEvent event, boolean doubleClick) {
		this.tryClickAreas(event);
	}

	@Override
	protected void onDrag(@NonNull MouseButtonEvent event, double dx, double dy) {
		this.tryClickAreas(event);
	}

	@Override
	public void onRelease(@NonNull MouseButtonEvent event) {
		if (this.currentClickedArea != null) {
			this.currentClickedArea.mouseReleased();
			this.currentClickedArea = null;
		}
	}

	public void tryClickAreas(MouseButtonEvent event) {
		double x = event.x();
		double y = event.y();

		// Current clicked area gets full click priority
		if (this.currentClickedArea != null) {
			if (this.currentClickedArea.mouseClicked(x, y)) {
				this.onColorPicked();
			}
			return;
		}

		// No area is currently clicked, so search for one to click, and return if found
		for (PickerArea area : clickableAreas) {
			if (area.mouseClicked(x, y)) {
				this.currentClickedArea = area;
				this.onColorPicked();
				return;
			}
		}

		for (PickerPreset preset : formattingPresetAreas) {
			if (preset.mouseClicked(x, y)) {
				this.currentClickedArea = preset;
				this.onColorPicked();
				return;
			}
		}

		for (PickerPreset preset : alphaPresetAreas) {
			if (preset.mouseClicked(x, y)) {
				this.currentClickedArea = preset;
				this.onColorPicked();
				return;
			}
		}

		for (PickerButton button : buttonAreas) {
			if (button.mouseClicked(x, y)) {
				this.currentClickedArea = button;
				return;
			}
		}
	}

	public void onPresetClick(PickerPreset preset) {
		if (this.presetListener != null) {
			this.presetListener.onPresetClick(preset);
		}
	}

	public void onColorPicked() {
		if (this.pickedColorListener != null) {
			int pickedColor = this.showAlpha ? this.getColor() : this.getColorNoAlpha();
			this.pickedColorListener.set(pickedColor);
		}
	}

	/**
	 * @return The Color Picker's picked color, as an integer
	 */
	public int getColor() {
		float returnedAlpha = this.showAlpha ? this.alpha : 1f;
		return ColorUtil.HSVAtoARGB(this.hue, this.saturation, this.value, returnedAlpha);
	}

	public int getColorNoAlpha() {
		return ColorUtil.HSVtoRGB(this.hue, this.saturation, this.value);
	}

	public int getHueColor() {
		return ColorUtil.HSVtoRGB(this.hue, 1f, 1f);
	}

	@Override
	public void onPress(@NonNull InputWithModifiers input) {
		// NO-OP
	}

	@Override
	protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {
		// NO-OP
	}

	public static class Builder {
		private final ColorPickerIncludedScreen screen;

		public Builder(ColorPickerIncludedScreen screen) {
			this.screen = screen;
		}

		public ColorPickerWidget build() {
			return new ColorPickerWidget(screen, 0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT, Component.nullToEmpty(""));
		}
	}
}
