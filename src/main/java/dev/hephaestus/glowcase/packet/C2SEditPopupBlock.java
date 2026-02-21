package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.HyperlinkBlockEntity;
import dev.hephaestus.glowcase.block.entity.PopupBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public record C2SEditPopupBlock(BlockPos pos, String title, List<Component> lines, TextBlockEntity.TextAlignment alignment, int color) implements C2SEditBlockEntity {
	public static final Type<C2SEditPopupBlock> ID = new Type<>(Glowcase.id("channel.popup_block"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditPopupBlock> PACKET_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, C2SEditPopupBlock::pos,
		ByteBufCodecs.STRING_UTF8, C2SEditPopupBlock::title,
		ByteBufCodecs.collection(ArrayList::new, ComponentSerialization.STREAM_CODEC), C2SEditPopupBlock::lines,
		ByteBufCodecs.BYTE.map(index -> TextBlockEntity.TextAlignment.values()[index], textAlignment -> (byte) textAlignment.ordinal()), C2SEditPopupBlock::alignment,
		ByteBufCodecs.INT, C2SEditPopupBlock::color,
		C2SEditPopupBlock::new
	);

	public static C2SEditPopupBlock of(PopupBlockEntity be) {
		return new C2SEditPopupBlock(be.getBlockPos(), be.title, be.lines, be.textAlignment, be.color);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	@Override
	public void receive(ServerLevel world, BlockEntity blockEntity) {
		if (!(blockEntity instanceof PopupBlockEntity be)) return;

		if (this.title().length() <= HyperlinkBlockEntity.TITLE_MAX_LENGTH) {
			be.title = this.title();
		}
		be.lines = this.lines();
		be.textAlignment = this.alignment();
		be.color = this.color();

		be.setChanged();
	}
}
