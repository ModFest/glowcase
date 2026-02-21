package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.block.GlowcaseBlock;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface C2SEditBlockEntity extends CustomPacketPayload {

	BlockPos pos();

	void receive(ServerLevel world, BlockEntity blockEntity);

	default void receive(ServerPlayNetworking.Context context) {
		if (!canEdit(context.player())) return;
		receive(context.player().level(), context.player().level().getBlockEntity(this.pos()));
	}

	default void send() {
		ClientPlayNetworking.send(this);
	}

	default boolean canEdit(ServerPlayer player) {
		if (!player.level().areEntitiesLoaded(ChunkPos.asLong(pos()))) return false;
		if (player.distanceToSqr(pos().getCenter()) > (12 * 12)) return false;
		return player.level().getBlockState(pos()).getBlock() instanceof GlowcaseBlock block && GlowcaseBlock.canEditGlowcase(player, pos());
	}
}
