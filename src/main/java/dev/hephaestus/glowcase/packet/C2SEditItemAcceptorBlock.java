package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ItemAcceptorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public record C2SEditItemAcceptorBlock(BlockPos pos, ResourceLocation item, int count, int pulse, boolean isItemTag, ItemAcceptorBlockEntity.OutputDirection outputDirection) implements C2SEditBlockEntity {
	public static final Type<C2SEditItemAcceptorBlock> ID = new Type<>(Glowcase.id("channel.item_acceptor.save"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditItemAcceptorBlock> PACKET_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, C2SEditItemAcceptorBlock::pos,
		ResourceLocation.STREAM_CODEC, C2SEditItemAcceptorBlock::item,
		ByteBufCodecs.INT, C2SEditItemAcceptorBlock::count,
		ByteBufCodecs.INT, C2SEditItemAcceptorBlock::pulse,
		ByteBufCodecs.BOOL, C2SEditItemAcceptorBlock::isItemTag,
		ByteBufCodecs.BYTE.map(index -> ItemAcceptorBlockEntity.OutputDirection.values()[index], outputDirection -> (byte) outputDirection.ordinal()), C2SEditItemAcceptorBlock::outputDirection,
		C2SEditItemAcceptorBlock::new
	);

	public static C2SEditItemAcceptorBlock of(ItemAcceptorBlockEntity be) {
		return new C2SEditItemAcceptorBlock(be.getBlockPos(), be.getItem(), be.count, be.pulse, be.isItemTag, be.outputDirection);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	@Override
	public void receive(ServerLevel world, BlockEntity blockEntity) {
		if (!(blockEntity instanceof ItemAcceptorBlockEntity be)) return;

		be.setItem(this.item());
		be.count = this.count();
		be.pulse = this.pulse();
		be.isItemTag = this.isItemTag();
		be.outputDirection = this.outputDirection();

		be.setChanged();
	}
}
