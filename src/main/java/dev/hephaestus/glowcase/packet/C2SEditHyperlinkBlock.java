package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.HyperlinkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public record C2SEditHyperlinkBlock(BlockPos pos, String title, String url) implements C2SEditBlockEntity {
	public static final Type<C2SEditHyperlinkBlock> ID = new Type<>(Glowcase.id("channel.hyperlink.save"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditHyperlinkBlock> PACKET_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, C2SEditHyperlinkBlock::pos,
		ByteBufCodecs.STRING_UTF8, C2SEditHyperlinkBlock::title,
		ByteBufCodecs.STRING_UTF8, C2SEditHyperlinkBlock::url,
		C2SEditHyperlinkBlock::new
	);

	public static C2SEditHyperlinkBlock of(HyperlinkBlockEntity be) {
		return new C2SEditHyperlinkBlock(be.getBlockPos(), be.getTitle(), be.getUrl());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	@Override
	public void receive(ServerLevel world, BlockEntity blockEntity) {
		if (!(blockEntity instanceof HyperlinkBlockEntity be)) return;
		if (this.title().length() <= HyperlinkBlockEntity.TITLE_MAX_LENGTH) {
			be.setTitle(this.title());
		}
		if (this.url().length() <= HyperlinkBlockEntity.URL_MAX_LENGTH) {
			be.setUrl(this.url());
		}
	}
}
