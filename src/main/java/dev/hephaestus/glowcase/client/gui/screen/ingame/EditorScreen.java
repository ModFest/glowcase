package dev.hephaestus.glowcase.client.gui.screen.ingame;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

/**
 * @author Ampflower
 **/
public abstract class EditorScreen extends GlowcaseScreen implements Editor {
	protected EditorScreen() {
		super();
	}

	protected EditorScreen(Component title) {
		super(title);
	}

	@Override
	@MustBeInvokedByOverriders
	public void onClose() {
		this.sendUpdatePacket();
		super.onClose();
	}
}
