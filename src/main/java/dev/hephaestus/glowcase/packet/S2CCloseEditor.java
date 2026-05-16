package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.gui.screen.ingame.BlockEditor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * @author Ampflower
 **/
public record S2CCloseEditor(ResourceKey<Level> dimension, BlockPos pos) implements CustomPacketPayload {
	public static final Type<S2CCloseEditor> ID = new Type<>(Glowcase.id("close_editor"));
	public static final StreamCodec<RegistryFriendlyByteBuf, S2CCloseEditor> PACKET_CODEC = StreamCodec.composite(
		ResourceKey.streamCodec(Registries.DIMENSION), S2CCloseEditor::dimension,
		BlockPos.STREAM_CODEC, S2CCloseEditor::pos,
		S2CCloseEditor::new
	);

	@Override
	public Type<S2CCloseEditor> type() {
		return ID;
	}

	public void receive(ClientPlayNetworking.Context context) {
		final Minecraft client = context.client();
		if (
			!(client.screen instanceof BlockEditor<?> editor)
			|| !editor.contextMatches(this.dimension(), this.pos())
		) {
			// Tell the server we don't actually hold a lock there.
			// Not sure when this would ever happen, but it might!
			context.responseSender().sendPacket(new C2SUnlockEditor(this.dimension(), this.pos()));
			return;
		}
		// Politely closes the editor. Allows the client to save its work before bailing.
		client.screen.onClose();
		client.setScreen(null);
	}
}
