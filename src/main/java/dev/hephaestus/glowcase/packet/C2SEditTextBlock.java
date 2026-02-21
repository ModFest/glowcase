package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
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

public record C2SEditTextBlock(BlockPos pos, TextBlockEntity.TextAlignment alignment, TextBlockEntity.ZOffset offset,
							   TextBlockValues values) implements C2SEditBlockEntity {
	public static final Type<C2SEditTextBlock> ID = new Type<>(Glowcase.id("channel.text_block"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditTextBlock> PACKET_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, C2SEditTextBlock::pos,
		ByteBufCodecs.BYTE.map(index -> TextBlockEntity.TextAlignment.values()[index], textAlignment -> (byte) textAlignment.ordinal()), C2SEditTextBlock::alignment,
		ByteBufCodecs.BYTE.map(index -> TextBlockEntity.ZOffset.values()[index], zOffset -> (byte) zOffset.ordinal()), C2SEditTextBlock::offset,
		TextBlockValues.PACKET_CODEC, C2SEditTextBlock::values,
		C2SEditTextBlock::new
	);

	public static C2SEditTextBlock of(TextBlockEntity be) {
		return new C2SEditTextBlock(be.getBlockPos(), be.textAlignment, be.zOffset, new TextBlockValues(be.shadow, be.scale, be.backgroundColor, be.color, be.lines, be.viewDistance));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	@Override
	public void receive(ServerLevel world, BlockEntity blockEntity) {
		if (!(blockEntity instanceof TextBlockEntity be)) return;

		be.shadow = this.values().shadow();
		be.scale = this.values().scale();
		be.lines = this.values().lines();
		be.textAlignment = this.alignment();
		be.backgroundColor = this.values().backgroundColor();
		be.color = this.values().color();
		be.zOffset = this.offset();
		be.viewDistance = this.values().viewDistance();

		be.setChanged();
	}

	// separated for tuple call
	public record TextBlockValues(boolean shadow, float scale, int backgroundColor, int color, List<Component> lines,
								  float viewDistance) {
		public static final StreamCodec<RegistryFriendlyByteBuf, TextBlockValues> PACKET_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, TextBlockValues::shadow,
			ByteBufCodecs.FLOAT, TextBlockValues::scale,
			ByteBufCodecs.INT, TextBlockValues::backgroundColor,
			ByteBufCodecs.INT, TextBlockValues::color,
			ByteBufCodecs.collection(ArrayList::new, ComponentSerialization.STREAM_CODEC), TextBlockValues::lines,
			ByteBufCodecs.FLOAT, TextBlockValues::viewDistance,
			TextBlockValues::new
		);
	}
}
