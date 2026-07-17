package dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker;

import net.minecraft.resources.Identifier;

import java.util.List;

public class PickerButton extends PickerArea {
	private static final Identifier CONFIRM_TEXTURE = Identifier.withDefaultNamespace("pending_invite/accept");
	private static final Identifier CONFIRM_HIGHLIGHTED_TEXTURE = Identifier.withDefaultNamespace("pending_invite/accept_highlighted");
	private static final Identifier CANCEL_TEXTURE = Identifier.withDefaultNamespace("pending_invite/reject");
	private static final Identifier CANCEL_HIGHLIGHTED_TEXTURE = Identifier.withDefaultNamespace("pending_invite/reject_highlighted");

	private final Identifier texture;
	private final Identifier hoverTexture;
	private final Runnable clickListener;

	public static List<PickerButton> createButtons(ColorPickerWidget colorPickerWidget) {
		return List.of(
			new PickerButton(colorPickerWidget, CONFIRM_TEXTURE, CONFIRM_HIGHLIGHTED_TEXTURE, colorPickerWidget::confirm),
			new PickerButton(colorPickerWidget, CANCEL_TEXTURE, CANCEL_HIGHLIGHTED_TEXTURE, colorPickerWidget::cancel)
		);
	}

	public PickerButton(ColorPickerWidget colorPicker, Identifier texture, Identifier hoverTexture, Runnable clickListener) {
		super(colorPicker, true, null, null);
		this.texture = texture;
		this.hoverTexture = hoverTexture;
		this.clickListener = clickListener;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY) {
		if (this.isMouseOver(mouseX, mouseY)) {
			this.clickListener.run();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY);
	}

	public Identifier getTexture(int mouseX, int mouseY) {
		return this.isMouseOver(mouseX, mouseY) ? this.hoverTexture : this.texture;
	}
}
