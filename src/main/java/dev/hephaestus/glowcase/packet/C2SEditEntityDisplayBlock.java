package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.DisplayBlockEntity;
import dev.hephaestus.glowcase.block.entity.EntityDisplayBlockEntity;
import dev.hephaestus.glowcase.util.DisplayBlockSettings;
import dev.hephaestus.glowcase.util.PartPoseInfo;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record C2SEditEntityDisplayBlock(BlockPos pos, DisplayBlockSettings settings, Optional<Map<String, PartPoseInfo>> partPoses, Optional<Boolean> isBaby) implements C2SEditBlockEntity, C2SEditDisplayBlock {
	public static final Type<C2SEditEntityDisplayBlock> ID = new Type<>(Glowcase.id("channel.entity_display"));

	private static final StreamCodec<ByteBuf, Map<String, PartPoseInfo>> PART_POSES_CODEC =
		ByteBufCodecs.map(HashMap::new, ByteBufCodecs.stringUtf8(64), PartPoseInfo.PACKET_CODEC, 256);

	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditEntityDisplayBlock> PACKET_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, C2SEditEntityDisplayBlock::pos,
		DisplayBlockSettings.PACKET_CODEC, C2SEditEntityDisplayBlock::settings,
		ByteBufCodecs.optional(PART_POSES_CODEC), C2SEditEntityDisplayBlock::partPoses,
		ByteBufCodecs.optional(ByteBufCodecs.BOOL), C2SEditEntityDisplayBlock::isBaby,
		C2SEditEntityDisplayBlock::new
	);

	public static C2SEditEntityDisplayBlock of(DisplayBlockEntity be) {
		EntityDisplayBlockEntity entityDisplayBlock = (EntityDisplayBlockEntity)be;
		return new C2SEditEntityDisplayBlock(be.getBlockPos(), be.toSettings(), Optional.of(entityDisplayBlock.getPartPoses()), Optional.of(entityDisplayBlock.isBaby()));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	@Override
	public void receive(ServerLevel world, BlockEntity blockEntity) {
		if(blockEntity instanceof EntityDisplayBlockEntity entityDisplayBlock) {
			partPoses().ifPresent(entityDisplayBlock::loadPartPoses);
			isBaby().ifPresent(entityDisplayBlock::setBaby);
		}
		C2SEditDisplayBlock.super.receive(world, blockEntity);
	}
}
