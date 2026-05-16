package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.GlowcaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Notifies the server that the client has fully closed the editor, or never had it open.
 *
 * @author Ampflower
 */
public record C2SUnlockEditor(ResourceKey<Level> dimension, BlockPos pos) implements C2SLockingBlockReceiver {
	public static final Type<C2SUnlockEditor> ID = new Type<>(Glowcase.id("unlock_editor"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SUnlockEditor> PACKET_CODEC = StreamCodec.composite(
		ResourceKey.streamCodec(Registries.DIMENSION), C2SUnlockEditor::dimension,
		BlockPos.STREAM_CODEC, C2SUnlockEditor::pos,
		C2SUnlockEditor::new
	);

	public C2SUnlockEditor(BlockEntity entity) {
		this(entity.getLevel().dimension(), entity.getBlockPos());
	}

	@Override
	public Type<C2SUnlockEditor> type() {
		return ID;
	}

	@Override
	public void receive(final ServerPlayer player, final ServerLevel level, final GlowcaseBlockEntity blockEntity) {
		blockEntity.unlockEditor(player);
	}

	@Override
	public boolean lockEditor(final GlowcaseBlockEntity glowcase, final ServerPlayer player) {
		return false;
	}
}
