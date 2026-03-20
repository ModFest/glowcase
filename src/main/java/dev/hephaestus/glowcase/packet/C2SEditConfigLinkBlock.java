package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ConfigLinkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public record C2SEditConfigLinkBlock(BlockPos pos, String title, String url) implements C2SEditBlockEntity {
	public static final Type<C2SEditConfigLinkBlock> ID = new Type<>(Glowcase.id("channel.configlink.save"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditConfigLinkBlock> PACKET_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, C2SEditConfigLinkBlock::pos,
		ByteBufCodecs.STRING_UTF8, C2SEditConfigLinkBlock::title,
		ByteBufCodecs.STRING_UTF8, C2SEditConfigLinkBlock::url,
		C2SEditConfigLinkBlock::new
	);

	public static C2SEditConfigLinkBlock of(ConfigLinkBlockEntity be) {
		return new C2SEditConfigLinkBlock(be.getBlockPos(), be.getTitle(), be.getUrl());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	@Override
	public void receive(ServerLevel world, BlockEntity blockEntity) {
		if (!(blockEntity instanceof ConfigLinkBlockEntity be)) return;
		if (this.title().length() <= ConfigLinkBlockEntity.TITLE_MAX_LENGTH) {
			be.setTitle(this.title());
		}
		if (this.url().length() <= ConfigLinkBlockEntity.URL_MAX_LENGTH) {
			be.setUrl(this.url());
		}
	}
}
