package dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker;

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
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Main color picker widget, including a preview box, saturation & value box, hue slider, optional alpha slider, and color presets.<br><br>
 * Each {@link ColorPickerIncludedScreen} has *one* Color Picker, which each widget uses. This means that position, showAlpha, and color values will be updated constantly.<br><br>
 * @author Superkat32
 */
public class ColorPickerWidget extends AbstractButton {
	// Okay, let's do this one last time. My name is Color-Picker Parker, and I was bitten by a radioactive Gradle Elephant, and for the last like 5 minutes, I've been the one and only Glowcase Color Picker... it's like 2am right now

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
	public float prevHue, prevSaturation, prevValue, prevAlpha;

	public static ColorPickerWidget.Builder builder(ColorPickerIncludedScreen screen) {
		return new Builder(screen);
	}

	public ColorPickerWidget(ColorPickerIncludedScreen screen, int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
		this.screen = screen;

		this.previewArea = new PickerArea(this, false);
		this.satValueArea = new PickerArea(this, xLerp -> this.saturation = xLerp, yLerp -> this.value = 1f - yLerp);
		this.hueArea = new PickerArea(this, xLerp -> this.hue = xLerp);
		this.alphaArea = new PickerArea(this, xLerp -> this.alpha = xLerp);
		this.presetsArea = new PickerArea(this, false);
		this.formattingPresetAreas = PickerPreset.createFormattingPresets(this);
		this.alphaPresetAreas = PickerPreset.createAlphaPresets(this);
		this.buttonAreas = PickerButton.createButtons(this);
		this.updateAreas();

		this.clickableAreas = List.of(this.satValueArea, this.hueArea, this.alphaArea);
		this.hide(); // Start hidden
	}

	// Preset & button width & height: 16
	// Presets per row: 10 (2 rows by default, 3 if alpha is shown)
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
		if (this.showAlpha) this.alphaArea.lerpThumbX(this.alpha);
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
		// TODO - Precise tile of the background for my perfectionism here
		// Alpha background texture (if needed)
		if (this.showAlpha) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ALPHA_PREVIEW_TEXTURE, area.getX(), area.getY(), area.getWidth(), area.getHeight());
		// Current color
		graphics.fill(area.getX(), area.getY(), area.getX2(), area.getY2(), this.getColor());
	}

	public void extractSatValueArea(PickerArea area, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// White to current hue, left to right
		GuiGraphicsUtil.extractHorizontalGradient(graphics, area.getX(), area.getY(), area.getX2(), area.getY2(), ColorUtil.WHITE, this.getHueColor());
		// Transparent to black, top to bottom
		graphics.fillGradient(area.getX(), area.getY(), area.getX2(), area.getY2(), ColorUtil.TRANSPARENT, ColorUtil.BLACK);
		// Outline
		if (area.shouldOutline(mouseX, mouseY)) graphics.outline(area.getX(), area.getY(), area.getWidth(), area.getHeight(), ColorUtil.WHITE);
	}

	public void extractHueArea(PickerArea area, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// Hue gradient, starting and ending on red
		GuiGraphicsUtil.extractHueGradient(graphics, area.getX(), area.getY(), area.getX2(), area.getY2());
		// Outline
		if (area.shouldOutline(mouseX, mouseY)) graphics.outline(area.getX(), area.getY(), area.getWidth(), area.getHeight(), ColorUtil.WHITE);
	}

	public void extractAlphaArea(PickerArea area, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// Alpha background texture
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ALPHA_SLIDER_TEXTURE, area.getX(), area.getY(), area.getWidth(), area.getHeight());
		// Transparent to current color, left to right
		GuiGraphicsUtil.extractHorizontalGradient(graphics, area.getX(), area.getY(), area.getX2(), area.getY2(), ColorUtil.TRANSPARENT, this.getColorNoAlpha());
		// Outline
		if (area.shouldOutline(mouseX, mouseY)) graphics.outline(area.getX(), area.getY(), area.getWidth(), area.getHeight(), ColorUtil.WHITE);
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
		if (preset.shouldOutline(mouseX, mouseY)) graphics.outline(preset.getX(), preset.getY(), preset.getWidth(), preset.getHeight(), ColorUtil.WHITE);
	}

	public void extractButtonArea(PickerButton button, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// TODO - PLEASE precise texture this it'll annoy me so much
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, button.getTexture(mouseX, mouseY), button.getX(), button.getY(), button.getWidth(), button.getHeight());
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

	// endregion

	public void target(AbstractWidget widget, int initColor, boolean showAlpha, ColorSetter pickedColorListener) {
		this.target(widget, initColor, showAlpha, false, pickedColorListener);
	}

	/**
	 * Position & set up the Color Picker based on a widget and its needs. Positioning accounts for screen size, ensuring the color picker never goes off-screen.
	 * @param widget The Widget to position the Color Picker too.
	 * @param initColor The initial color the Color Picker should be, and should revert to if canceled/undone.
	 * @param showAlpha Whether the alpha should be editable, and the alpha slider visible.
	 * @param rightAligned If the Color Picker Widget should align itself to the right side of the widget instead of the left side  .
	 * @param pickedColorListener What to do with the picked color, as an integer.
	 */
	public void target(AbstractWidget widget, int initColor, boolean showAlpha, boolean rightAligned, ColorSetter pickedColorListener) {
		this.visible = true;
		this.active = true;
		this.targetElement = widget;

		// Ensure at least 2 pixels of padding between screen edges
		int x = Math.min(Minecraft.getInstance().screen.width - this.getWidth() - 2, widget.getX());
		if (rightAligned) x = Math.max(2, widget.getX() + widget.getWidth() - this.getWidth() + 2);
		int y = widget.getY() + widget.getHeight();
		this.setX(x);
		this.setY(y);

		this.setPrevColor(initColor);
		this.setColor(initColor);

		this.showAlpha = showAlpha;
		this.setHeight(this.showAlpha ? DEFAULT_ALPHA_HEIGHT : DEFAULT_HEIGHT);

		this.pickedColorListener = pickedColorListener;
		this.presetListener = preset -> { // Default preset behaviour, just set the picker color
			if (preset.isAlphaPreset()) {
				this.alpha = preset.getAlpha();
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

	// region Unused
	@Override
	public void onPress(@NonNull InputWithModifiers input) {}

	@Override
	protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {}
	// endregion

	public static class Builder {
		private final ColorPickerIncludedScreen screen;

		public Builder(ColorPickerIncludedScreen screen) {
			this.screen = screen;
		}

		public ColorPickerWidget build() {
			return new ColorPickerWidget(screen, 0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT, Component.nullToEmpty(""));
		}
	}


//	public final ColorPickerIncludedScreen screen;
//	public GuiEventListener targetElement;
//	public Color color = Color.red;
//	public boolean includePresets = true;
//	public ArrayList<ColorPresetWidget> presetWidgets = Lists.newArrayList();
//	public boolean confirmOrCancelButtonDown = false;
//	public IconButtonWidget confirmButton;
//	public IconButtonWidget cancelButton;
//	private Consumer<Color> changeListener;
//	private BiConsumer<Color, @Nullable ChatFormatting> presetListener;
//	private Consumer<ColorPickerWidget> onAccept;
//	private Consumer<ColorPickerWidget> onCancel;
//
//	private boolean mouseDown = false;
//	private int presetY, presetSize, presetPadding, presetHeight;
//	private boolean presetDown = false;
//	private int previewX, previewY, previewWidth, previewHeight;
//	private int hueX, hueY, hueWidth, hueHeight;
//	private boolean hueDown = false;
//	private int satLightX, satLightY, satLightWidth, satLightHeight;
//	private boolean satLightDown = false;
//	public int hueThumbX;
//	public int satLightThumbX, satLightThumbY;
//
//	private float[] HSL;
//	private float hue;
//	private float saturation;
//	private float light;
//
//	public static ColorPickerWidget.Builder builder(ColorPickerIncludedScreen screen, int x, int y) {
//		return new ColorPickerWidget.Builder(screen, x, y);
//	}
//
//	public ColorPickerWidget(ColorPickerIncludedScreen screen, int x, int y, int width, int height, Component message) {
//		super(x, y, width, height, message);
//		this.screen = screen;
//
//		this.confirmButton = IconButtonWidget.builder(CONFIRM_TEXTURE, action -> this.confirmColor())
//			.hoverIcon(CONFIRM_HIGHLIGHTED_TEXTURE).build();
//
//		this.cancelButton = IconButtonWidget.builder(CANCEL_TEXTURE, action -> this.cancel())
//			.hoverIcon(CANCEL_HIGHLIGHTED_TEXTURE).build();
//
//		updatePositions();
//		updateHSL();
//		updateThumbPositions();
//	}
//
//	public void setTargetElement(GuiEventListener element) {
//		this.targetElement = element;
//	}
//
//	public void setIncludePresets(boolean shouldInclude) {
//		this.includePresets = shouldInclude;
//	}
//
//	public void setPresets(boolean includeDefaultPresets, List<Color> addedPresets) {
//		if (includeDefaultPresets) {
//			addDefaultPresets();
//		}
//		if (!addedPresets.isEmpty()) {
//			for (Color preset : addedPresets) {
//				this.presetWidgets.add(ColorPresetWidget.fromColor(this, preset));
//			}
//		}
//	}
//
//	public void confirmColor() {
//		if (this.onAccept != null) {
//			this.onAccept.accept(this);
//		}
//
//		this.toggle(false);
//	}
//
//	public void cancel() {
//		if (this.onCancel != null) {
//			this.onCancel.accept(this);
//		}
//
//		this.toggle(false);
//	}
//
//	public void toggle(boolean active) {
//		this.active = active;
//		this.visible = active;
//		if (this.active) {
//			this.updatePositions();
//			this.updateHSL();
//			this.updateThumbPositions();
//		}
//	}
//
//	public void insertColor(Color color) {
//		String hex = getHexCode(color);
//		this.screen.insertHexTag(hex);
//	}
//
//	public void insertFormatting(ChatFormatting formatting) {
//		this.screen.insertFormattingTag(formatting);
//	}
//
//	public void setColor(Color color) {
//		this.color = color;
//		this.updateHSL();
//		this.updateThumbPositions();
//	}
//
//	@Override
//	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
//		if (!visible) return;
//		updateHSL();
//
//		//graphics.setShaderColor(1f, 1f, 1f, this.alpha);
//		/*RenderSystem.enableBlend();
//		RenderSystem.enableDepthTest();*/
//		Matrix3x2fStack matrices = graphics.pose();
//		//graphics.applyBlur();
//
//		graphics.nextStratum();
//		matrices.pushMatrix();
//
//		int x = this.getX();
//		int y = this.getY();
//		int z = 1;
//		int width = this.getWidth();
//		int height = this.getHeight();
//
//		//background
//		graphics.blit(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("textures/gui/inworld_menu_list_background.png"), x, y, 0, 0, width, height, 32, 32);
//		if (this.isHoveredOrFocused()) {
//			//outline
//			drawOutline(graphics, x, y, width, height, Color.white);
//		}
//
//		//color picker stuff
//		updatePositions();
//		this.confirmButton.setPosition(x + width - presetSize - presetPadding, y + height - presetSize - 2, z + 1, presetSize, presetSize + 2);
//		this.cancelButton.setPosition(x + width - presetSize * 2 - presetPadding * 2 - 1, y + height - presetSize - 2, z + 1, presetSize, presetSize + 2);
//
//		drawColorPreview(graphics, previewX, previewY, previewWidth, previewHeight);
//		drawSatLight(graphics, satLightX, satLightY, satLightWidth, satLightHeight);
//		drawHueBar(graphics, hueX, hueY, hueWidth, hueHeight, z + 1);
//		if (this.includePresets) {
//			//sorta dynamic but also really specific to keep it all aligned
//			//I'm not going to worry about it a lot though because I do not see the custom preset thing being used a lot if at all
//			drawPresets(graphics, mouseX, mouseY, delta, previewX, presetY, y + height - presetY, z + 1, presetSize, width / (presetSize + presetPadding), presetPadding);
//		}
//
//		this.confirmButton.extractRenderState(graphics, mouseX, mouseY, delta);
//		this.cancelButton.extractRenderState(graphics, mouseX, mouseY, delta);
//
//
//		matrices.popMatrix();
//
//		//graphics.setShaderColor(1f, 1f, 1f, 1f);
//	}
//
//	public void updatePositions() {
//		int x = this.getX();
//		int y = this.getY();
//		int width = this.getWidth();
//		int height = this.getHeight();
//
//		presetSize = (int) (height / 6.5);
//		presetPadding = 2;
//		presetHeight = presetSize * 2 + presetPadding * 2;
//
//		previewX = x + 2;
//		previewY = y + 2;
//		previewWidth = width / 3;
//		previewHeight = height - 16 - (includePresets ? presetHeight : 0);
//		satLightX = previewX + previewWidth + 2;
//		satLightY = y + 2;
//		satLightWidth = width - previewWidth - 6;
//		satLightHeight = previewHeight;
//		hueX = x + 2;
//		hueY = previewY + previewHeight + 2;
//		hueWidth = width - 4;
//		hueHeight = height - previewHeight - 6 - (includePresets ? presetHeight : 0);
//		presetY = hueY + hueHeight + presetPadding;
//	}
//
//	private void drawColorPreview(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
//		graphics.fill(x, y, x + width, y + height, this.color.getRGB());
//	}
//
//	private void drawHueBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int z) {
//		//rainbow gradient
//		int[] colors = new int[]{
//			Color.red.getRGB(), Color.yellow.getRGB(), Color.green.getRGB(),
//			Color.cyan.getRGB(), Color.blue.getRGB(), Color.magenta.getRGB(),
//			Color.red.getRGB()
//		};
//
//		int maxColors = colors.length - 1;
//		for (int color = 0; color < maxColors; color++) {
//			sidewaysGradient(
//				graphics,
//				x + (width / maxColors * (color)), y,
//				width / maxColors, height,
//				colors[color], colors[color + 1]
//			);
//		}
//
//		//thumb
//		graphics.fill(hueThumbX - 3, y - 1, hueThumbX + 3, y + height + 1, getRgbFromHueThumb());
//		drawOutline(graphics, hueThumbX - 3, y - 1, 6, height + 2, Color.white);
//	}
//
//	private void drawSatLight(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
//		//white to current color's hue, left to right
//		sidewaysGradient(graphics, x, y, width, height, Color.white.getRGB(), getRgbFromHueThumb());
//
//		//transparent to black, top to bottom
//		graphics.fillGradient(x, y, x + width, y + height, 0x00000000, Color.black.getRGB());
//
//		//thumb
//		graphics.fill(satLightThumbX - 4, satLightThumbY - 4, satLightThumbX + 4, satLightThumbY + 4, this.color.getRGB());
//		drawOutline(graphics, satLightThumbX - 4, satLightThumbY - 4, 8, 8, Color.white);
//	}
//
//	private void drawOutline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, Color outlineColor) {
//		int color = outlineColor.getRGB();
//		graphics.fill(x, y, x + width, y + 1, color);
//		graphics.fill(x, y, x + 1, y + height, color);
//		graphics.fill(x + width, y, x + width - 1, y + height, color);
//		graphics.fill(x, y + height, x + width, y + height - 1, color);
//	}
//
//	private void sidewaysGradient(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int startColor, int endColor) {
//		graphics.guiRenderState.addGuiElement(new GuiElementRenderState() {
//
//			@Override
//			public ScreenRectangle bounds() {
//				return new ScreenRectangle(x, y, width, height).transformMaxBounds(graphics.pose());
//			}
//
//			@Override
//			public void buildVertices(VertexConsumer vertices) {
//				Matrix3x2fStack matrix = graphics.pose();
//				vertices.addVertexWith2DPose(matrix, x, y).setColor(startColor);
//				vertices.addVertexWith2DPose(matrix, x, y + height).setColor(startColor);
//				vertices.addVertexWith2DPose(matrix, x + width, y + height).setColor(endColor);
//				vertices.addVertexWith2DPose(matrix, x + width, y).setColor(endColor);
//			}
//
//			@Override
//			public RenderPipeline pipeline() {
//				return RenderPipelines.GUI;
//			}
//
//			@Override
//			public TextureSetup textureSetup() {
//				return TextureSetup.noTexture();
//			}
//
//			@Override
//			public @Nullable ScreenRectangle scissorArea() {
//				return null;
//			}
//		});
//	}
//
//	private void drawPresets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, int x, int y, int height, int z, int presetSize, int presetsPerLine, int presetPadding) {
//		int presetX = x;
//		int presetY = y;
//		int renderedPresets = 0;
//		for (ColorPresetWidget preset : this.presetWidgets) {
//			preset.setPosition(presetX, presetY, z, presetSize);
//			preset.extractRenderState(graphics, mouseX, mouseY, delta);
//			presetX += presetSize + presetPadding;
//			renderedPresets++;
//			if (renderedPresets % presetsPerLine == 0) {
//				presetY += presetSize + presetPadding;
//				if (presetY > y + height) { //prevent overflow
//					return;
//				}
//				presetX = x;
//			}
//		}
//	}
//
//	//done manually to keep list order instead of looping through Formatting.values()
//	public void addDefaultPresets() {
//		ColorPresetWidget darkRed = ColorPresetWidget.fromFormatting(this, ChatFormatting.DARK_RED);
//		ColorPresetWidget red = ColorPresetWidget.fromFormatting(this, ChatFormatting.RED);
//		ColorPresetWidget gold = ColorPresetWidget.fromFormatting(this, ChatFormatting.GOLD);
//		ColorPresetWidget yellow = ColorPresetWidget.fromFormatting(this, ChatFormatting.YELLOW);
//		ColorPresetWidget green = ColorPresetWidget.fromFormatting(this, ChatFormatting.GREEN);
//		ColorPresetWidget darkGreen = ColorPresetWidget.fromFormatting(this, ChatFormatting.DARK_GREEN);
//		ColorPresetWidget aqua = ColorPresetWidget.fromFormatting(this, ChatFormatting.AQUA);
//		ColorPresetWidget darkAqua = ColorPresetWidget.fromFormatting(this, ChatFormatting.DARK_AQUA);
//		ColorPresetWidget blue = ColorPresetWidget.fromFormatting(this, ChatFormatting.BLUE);
//		ColorPresetWidget darkBlue = ColorPresetWidget.fromFormatting(this, ChatFormatting.DARK_BLUE);
//		ColorPresetWidget lightPurple = ColorPresetWidget.fromFormatting(this, ChatFormatting.LIGHT_PURPLE);
//		ColorPresetWidget darkPurple = ColorPresetWidget.fromFormatting(this, ChatFormatting.DARK_PURPLE);
//		ColorPresetWidget white = ColorPresetWidget.fromFormatting(this, ChatFormatting.WHITE);
//		ColorPresetWidget grey = ColorPresetWidget.fromFormatting(this, ChatFormatting.GRAY);
//		ColorPresetWidget darkGrey = ColorPresetWidget.fromFormatting(this, ChatFormatting.DARK_GRAY);
//		ColorPresetWidget black = ColorPresetWidget.fromFormatting(this, ChatFormatting.BLACK);
//		this.presetWidgets.addAll(List.of(darkRed, red, gold, yellow, green, darkGreen, aqua, darkAqua,
//			blue, darkBlue, lightPurple, darkPurple, white, grey, darkGrey, black));
//	}
//
//	@Override
//	public void onClick(MouseButtonEvent event, boolean doubleClick) {
//		this.mouseDown = true;
//		this.satLightDown = false;
//		this.hueDown = false;
//		this.presetDown = false;
//		this.confirmOrCancelButtonDown = false;
//		setColorFromMouse(event, doubleClick);
//	}
//
//	public void setColorFromMouse(MouseButtonEvent event, boolean doubleClick) {
//		int colorAlpha = color.getAlpha();
//
//		double mouseX = event.x();
//		double mouseY = event.y();
//		if (clickedSatLight(mouseX, mouseY)) {
//			setSatLightFromMouse(mouseX, mouseY);
//		} else if (clickedHue(mouseX, mouseY)) {
//			setHueFromMouse(mouseX);
//		} else if (this.confirmButton.isMouseOver(mouseX, mouseY)) {
//			if (satLightDown || hueDown || presetDown || confirmOrCancelButtonDown) return;
//			this.confirmButton.onClick(event, doubleClick);
//			confirmOrCancelButtonDown = true;
//		} else if (this.cancelButton.isMouseOver(mouseX, mouseY)) {
//			if (satLightDown || hueDown || presetDown || confirmOrCancelButtonDown) return;
//			this.cancelButton.onClick(event, doubleClick);
//			confirmOrCancelButtonDown = true;
//		} else {
//			//clickedPreset also sets the preset to avoid an extra calculation
//			checkAndSetPreset(event, doubleClick);
//		}
//
//		if (this.changeListener != null) {
//			this.changeListener.accept(this.color);
//		}
//	}
//
//	public boolean clickedSatLight(double mouseX, double mouseY) {
//		if (hueDown || presetDown || confirmOrCancelButtonDown) return false;
//
//		if (mouseX >= satLightX
//			&& mouseX <= satLightX + satLightWidth
//			&& mouseY >= satLightY
//			&& mouseY <= satLightY + satLightHeight) {
//			satLightDown = true;
//		}
//
//		if (satLightDown) {
//			satLightThumbX = (int) Math.clamp(mouseX, satLightX, satLightX + satLightWidth);
//			satLightThumbY = (int) Math.clamp(mouseY, satLightY, satLightY + satLightHeight);
//		}
//		return satLightDown;
//	}
//
//	public boolean clickedHue(double mouseX, double mouseY) {
//		if (satLightDown || presetDown || confirmOrCancelButtonDown) return false;
//
//		if (mouseY >= hueY && mouseY <= hueY + hueHeight
//			&& mouseX >= hueX && mouseX <= hueX + hueWidth) {
//			hueDown = true;
//		}
//
//		if (hueDown) {
//			hueThumbX = (int) Math.clamp(mouseX, hueX, hueX + hueWidth);
//		}
//		return hueDown;
//	}
//
//	public boolean checkAndSetPreset(MouseButtonEvent event, boolean doubleClick) {
//		if (satLightDown || hueDown || presetDown || confirmOrCancelButtonDown) return false;
//
//		//just checks for each preset here, and also sets here so it doesn't have to check again
//		for (ColorPresetWidget preset : this.presetWidgets) {
//			if (preset.isMouseOver(event.x(), event.y())) {
//				preset.onClick(event, doubleClick);
//				//even though the preset closes the color picker,
//				//this is added to prevent spamming tags when holding down the mouse button
//				presetDown = true;
//			}
//		}
//		return presetDown;
//	}
//
//	@Override
//	protected void onDrag(MouseButtonEvent event, double dx, double dy) {
//		if (mouseDown || isMouseOver(event.x(), event.y())) {
//			setColorFromMouse(event, false);
//		}
//	}
//
//	@Override
//	public void onRelease(MouseButtonEvent event) {
//		this.mouseDown = false;
//	}
//
//	@Override
//	public boolean isMouseOver(double mouseX, double mouseY) {
//		return super.isMouseOver(mouseX, mouseY);
//	}
//
//	@Override
//	public void onPress(InputWithModifiers input) {
//	}
//
//
//	public void setSatLightFromMouse(double mouseX, double mouseY) {
//		if (mouseX < satLightX) {
//			this.saturation = 0f;
//		} else if (mouseX > satLightX + satLightWidth) {
//			this.saturation = 1f;
//		} else {
//			float newSat = (float) (mouseX - satLightX) / satLightWidth;
//			this.saturation = Math.clamp(newSat, 0f, 1f);
//		}
//
//		if (mouseY < satLightY) {
//			this.light = 1f;
//		} else if (mouseY > satLightY + satLightHeight) {
//			this.light = 0f;
//		} else {
//			float newLight = (float) (mouseY - satLightY) / satLightHeight;
//			this.light = Math.clamp(1f - newLight, 0f, 1f);
//		}
//
//		setColorFromHSL();
//	}
//
//	public void setHueFromMouse(double mouseX) {
//		if (mouseX < hueX) {
//			this.hue = 0f;
//		} else if (mouseX > hueX + hueWidth) {
//			this.hue = 1f;
//		} else {
//			float newHue = (float) (mouseX - hueX) / hueWidth;
//			this.hue = Math.clamp(newHue, 0f, 1f);
//		}
//
//		setColorFromHSL();
//	}
//
//	public void updateThumbPositions() {
//		this.satLightThumbX = getSatLightThumbX();
//		this.satLightThumbY = getSatLightThumbY();
//		this.hueThumbX = getHueThumbX();
//	}
//
//	private int getSatLightThumbX() {
//		int min = satLightX;
//		int max = satLightX + satLightWidth;
//		int value = (int) (min + (satLightWidth * this.saturation));
//		return Math.clamp(value, min, max);
//	}
//
//	private int getSatLightThumbY() {
//		int min = satLightY;
//		int max = satLightY + satLightHeight;
//		int value = (int) (min + (satLightHeight * (1.0f - this.light)));
//		return Math.clamp(value, min, max);
//	}
//
//	private int getHueThumbX() {
//		int min = hueX;
//		int max = hueX + hueWidth;
//		int value = (int) (min + hueWidth * this.hue);
//		return Math.clamp(value, min, max);
//	}
//
//	public Color getCurrentColor() {
//		return this.color;
//	}
//
//	public void setColorFromHSL() {
//		float trueHue = (float) (hueThumbX - hueX) / hueWidth;
//		this.color = Color.getHSBColor(trueHue, this.saturation, this.light);
//	}
//
//	public int getRgbFromHueThumb() {
//		float trueHue = (float) (hueThumbX - hueX) / hueWidth;
//		return Color.HSBtoRGB(trueHue, 1, 1);
//	}
//
//	public void updateHSL() {
//		this.HSL = getHSL();
//		this.hue = HSL[0];
//		this.saturation = HSL[1];
//		this.light = HSL[2];
//	}
//
//	protected float[] getHSL() {
//		return Color.RGBtoHSB(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), null);
//	}
//
//	@Override
//	public void updateWidgetNarration(NarrationElementOutput builder) {
//		this.defaultButtonNarrationText(builder);
//	}
//
//	public void setChangeListener(Consumer<Color> changeListener) {
//		this.changeListener = changeListener;
//	}
//
//	public void setPresetListener(BiConsumer<Color, @Nullable ChatFormatting> presetListener) {
//		this.presetListener = presetListener;
//	}
//
//	public void setOnAccept(Consumer<ColorPickerWidget> onAccept) {
//		this.onAccept = onAccept;
//	}
//
//	public void setOnCancel(Consumer<ColorPickerWidget> onCancel) {
//		this.onCancel = onCancel;
//	}
//
//	public BiConsumer<Color, @Nullable ChatFormatting> getPresetListener() {
//		return presetListener;
//	}
//
//	public static String getHexCode(Color color) {
//		return "#" + String.format("%1$06X", color.getRGB() & 0x00FFFFFF);
//	}
//
//	@Environment(EnvType.CLIENT)
//	public static class Builder {
//		private final ColorPickerIncludedScreen screen;
//		private final int x;
//		private final int y;
//		private int width = 150;
//		private int height = 200;
//		private boolean includePresets = true;
//		private boolean includeDefaultPresets = true;
//		private final List<Color> presets = Lists.newArrayList();
//
//		public Builder(ColorPickerIncludedScreen screen, int x, int y) {
//			this.screen = screen;
//			this.x = x;
//			this.y = y;
//		}
//
//		public ColorPickerWidget.Builder size(int width, int height) {
//			this.width = width;
//			this.height = height;
//			return this;
//		}
//
//		public ColorPickerWidget.Builder includePresets(boolean shouldInclude) {
//			this.includePresets = shouldInclude;
//			return this;
//		}
//
//		public ColorPickerWidget.Builder withPreset(boolean includeDefault, Color... presets) {
//			this.includeDefaultPresets = includeDefault;
//			this.presets.addAll(Arrays.asList(presets));
//			return this;
//		}
//
//		public ColorPickerWidget build() {
//			ColorPickerWidget colorPickerWidget = new ColorPickerWidget(this.screen, this.x, this.y, this.width, this.height, Component.nullToEmpty(""));
//			colorPickerWidget.setIncludePresets(this.includePresets);
//			if (this.includePresets) {
//				colorPickerWidget.setPresets(this.includeDefaultPresets, this.presets);
//			}
//			return colorPickerWidget;
//		}
//	}
}
