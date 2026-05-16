package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.GlowcaseBlockEntity;
import dev.hephaestus.glowcase.packet.C2SUnlockEditor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

/**
 * @author Ampflower
 **/
public abstract class BlockEditorScreen<E extends GlowcaseBlockEntity> extends EditorScreen implements BlockEditor<E> {
	protected final E blockEntity;

	protected BlockEditorScreen(E blockEntity) {
		this(blockEntity, blockEntity.getBlockState().getBlock().getName());
	}

	protected BlockEditorScreen(E blockEntity, Component title) {
		super(title);
		this.blockEntity = blockEntity;
	}

	@Override
	public final E getBlockEntity() {
		return this.blockEntity;
	}

	@Override
	@MustBeInvokedByOverriders
	public void removed() {
		super.removed();
		// DEPRECATED: inline on up-port or C<->S API break
		if (ClientPlayNetworking.canSend(C2SUnlockEditor.ID)) {
			ClientPlayNetworking.send(new C2SUnlockEditor(this.getBlockEntity()));
		}
	}
}
