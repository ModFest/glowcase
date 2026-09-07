package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Main interface for any Screen wishing to implement a {@link ColorPickerWidget}.<br><br>
 * Each ColorPickerIncludedScreen has *one* Color Picker widget which gets shared among all things using it.<br>
 * Steps for adding a Color Picker to a screen:
 * <ol type="0">
 *     <li>Implement this interface, and return a private ColorPickerWidget in {@link ColorPickerIncludedScreen#getColorPickerWidget()}.</li>
 *     <li>Initialize your ColorPickerWidget in {@link Screen#init()} via {@link ColorPickerWidget#builder(ColorPickerIncludedScreen)}. You do *not* need to add it as a renderableWidget.</li>
 *     <li>Include {@link ColorPickerIncludedScreen#extractColorPicker(GuiGraphicsExtractor, int, int, float)} at the very bottom of {@link Screen#extractRenderState(GuiGraphicsExtractor, int, int, float)} to ensure it renders atop of everything else.</li>
 *     <li>Include {@link ColorPickerIncludedScreen#mouseClickedColorPicker(MouseButtonEvent, boolean)} at the very top of {@link Screen#mouseClicked(MouseButtonEvent, boolean)}, return true if color picker was clicked, otherwise continue with method.</li>
 *     <li>Include {@link ColorPickerIncludedScreen#keyPressedColorPicker(KeyEvent)} at the very top of {@link Screen#keyPressed(KeyEvent)}, return true if color picker key pressed, otherwise continue with method.</li>
 *     <li>Booyah!</li>
 * </ol>
 * @author Superkat32
 */
public interface ColorPickerIncludedScreen {
	ColorPickerWidget getColorPickerWidget();

	default ColorPickerWidget createColorPickerWidget() {
		return ColorPickerWidget.builder(this).build();
	}

	default void extractColorPicker(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		this.getColorPickerWidget().extractRenderState(graphics, mouseX, mouseY, delta);
	}

	default boolean mouseClickedColorPicker(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		ColorPickerWidget colorPickerWidget = this.getColorPickerWidget();

		if (colorPickerWidget.isActive() && colorPickerWidget.visible) {
			if (colorPickerWidget.isMouseOver(mouseX, mouseY)) {
				colorPickerWidget.mouseClicked(event, doubleClick);
				Screen self = (Screen) this;
				self.setFocused(colorPickerWidget);
				self.setDragging(true);
				return true;
			} else if (colorPickerWidget.targetElement == null || !colorPickerWidget.targetElement.isMouseOver(mouseX, mouseY)) {
				this.hideColorPickerWidget();
			}
		}
		return false;
	}

	default boolean keyPressedColorPicker(KeyEvent event) {
		int keyCode = event.key();
		ColorPickerWidget colorPickerWidget = this.getColorPickerWidget();
		if (colorPickerWidget.isActive()) {
			Screen self = (Screen) this;
			switch (keyCode) {
				case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> colorPickerWidget.confirm();
				case GLFW.GLFW_KEY_ESCAPE -> colorPickerWidget.cancel();
				default -> {
					GuiEventListener pickerTarget = colorPickerWidget.targetElement;
					if (pickerTarget != null) {
						self.setFocused(pickerTarget);
						pickerTarget.keyPressed(event);
						return true;
					}
				}
			}
			self.setFocused(null);
			return true;
		}
		return false;
	}

	default void hideColorPickerWidget() {
		this.getColorPickerWidget().hide();
	}
}
