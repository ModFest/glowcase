package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ItemProviderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public record C2SEditItemProviderBlock(BlockPos pos, ItemProviderBlockEntity.GivesItem givesItem, long cooldown) implements C2SEditBlockEntity {
	public static final CustomPacketPayload.Type<C2SEditItemProviderBlock> ID = new CustomPacketPayload.Type<>(Glowcase.id("channel.item_provider"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditItemProviderBlock> PACKET_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, C2SEditItemProviderBlock::pos,
		ByteBufCodecs.BYTE.map(index -> ItemProviderBlockEntity.GivesItem.values()[index], givesItem -> (byte) givesItem.ordinal()), C2SEditItemProviderBlock::givesItem,
		ByteBufCodecs.VAR_LONG, C2SEditItemProviderBlock::cooldown,
		C2SEditItemProviderBlock::new
	);

	public static C2SEditItemProviderBlock of(ItemProviderBlockEntity be) {
		return new C2SEditItemProviderBlock(be.getBlockPos(), be.getGivesItem(), be.cooldown);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	@Override
	public void receive(ServerLevel world, BlockEntity blockEntity) {
		if (!(blockEntity instanceof ItemProviderBlockEntity be)) return;
		be.setGivesItem(this.givesItem());
		be.cooldown = this.cooldown;
	}
}
