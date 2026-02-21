package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.DisplayBlockEntity;
import dev.hephaestus.glowcase.util.DisplayBlockSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public record C2SEditItemDisplayBlock(BlockPos pos, DisplayBlockSettings settings) implements C2SEditBlockEntity, C2SEditDisplayBlock {
	public static final Type<C2SEditItemDisplayBlock> ID = new Type<>(Glowcase.id("channel.item_display"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditItemDisplayBlock> PACKET_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, C2SEditItemDisplayBlock::pos,
		DisplayBlockSettings.PACKET_CODEC, C2SEditItemDisplayBlock::settings,
		C2SEditItemDisplayBlock::new
	);

	public static C2SEditEntityDisplayBlock of(DisplayBlockEntity be) {
		return new C2SEditEntityDisplayBlock(be.getBlockPos(), be.toSettings());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}


	@Override
	public void receive(ServerLevel world, BlockEntity blockEntity) {
		C2SEditDisplayBlock.super.receive(world, blockEntity);
	}
}
