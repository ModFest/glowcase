package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.OutlineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public record C2SEditOutlineBlock(BlockPos pos, Vec3i offset, Vec3i scale, int color) implements C2SEditBlockEntity {
	public static final StreamCodec<RegistryFriendlyByteBuf, Vec3i> VEC3I = StreamCodec.composite(
		ByteBufCodecs.INT, Vec3i::getX,
		ByteBufCodecs.INT, Vec3i::getY,
		ByteBufCodecs.INT, Vec3i::getZ,
		Vec3i::new
	);

	public static final Type<C2SEditOutlineBlock> ID = new Type<>(Glowcase.id("channel.outline.save"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditOutlineBlock> PACKET_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, C2SEditOutlineBlock::pos,
		VEC3I, C2SEditOutlineBlock::offset,
		VEC3I, C2SEditOutlineBlock::scale,
		ByteBufCodecs.INT, C2SEditOutlineBlock::color,
		C2SEditOutlineBlock::new
	);

	public static C2SEditOutlineBlock of(OutlineBlockEntity be) {
		return new C2SEditOutlineBlock(be.getBlockPos(), be.offset, be.scale, be.color);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	@Override
	public void receive(ServerLevel world, BlockEntity blockEntity) {
		if (!(blockEntity instanceof OutlineBlockEntity be)) return;

		be.offset = this.offset();
		be.scale = this.scale();
		be.color = this.color();

		be.setChanged();
	}
}
