package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.block.entity.GlowcaseBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

public interface C2SEditBlockEntity extends C2SLockingBlockReceiver {
	BlockPos pos();

	void receive(ServerLevel world, BlockEntity blockEntity);

	@Override
	default void receive(ServerPlayer player, ServerLevel level, GlowcaseBlockEntity blockEntity) {
		this.receive(level, blockEntity);
	}

	@Override
	@MustBeInvokedByOverriders
	default void reject(
		final PacketSender responseSender,
		final ServerLevel level,
		final @Nullable BlockEntity entity
	) {
		responseSender.sendPacket(new ClientboundBlockUpdatePacket(level, pos()));

		if (entity == null) {
			return;
		}

		final Packet<ClientGamePacketListener> packet = entity.getUpdatePacket();

		if (packet == null) {
			return;
		}

		responseSender.sendPacket(packet);
	}
}
