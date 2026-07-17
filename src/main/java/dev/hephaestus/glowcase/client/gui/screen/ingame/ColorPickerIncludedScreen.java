package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Main interface for any Screen wishing to implement a {@link ColorPickerWidget}.<br><br>
 * Each ColorPickerIncludedScreen has *one* Color Picker widget which gets shared among all things using it.
 */
public interface ColorPickerIncludedScreen {
	ColorPickerWidget getColorPickerWidget();

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
				case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> colorPickerWidget.hide();
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

//	default void setColorPickerTarget(GuiEventListener element) {
//		this.getColorPickerWidget().target(element);
//	}

	default void hideColorPickerWidget() {
		this.getColorPickerWidget().hide();
	}

	// TODO - break this into FormattableScreen
	void insertHexTag(String hex);
	void insertFormattingTag(ChatFormatting formatting);
}
