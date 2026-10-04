package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.util.Anchor;
import dev.hephaestus.glowcase.util.TextJustify;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public record C2SEditTextBlock(
	BlockPos pos,
	TextJustify justify,
	Anchor anchor,
	Vec3 renderOffsets,
	Vec3 rotation,
	TextBlockValues values
) implements C2SEditBlockEntity {
	public static final Type<C2SEditTextBlock> ID = new Type<>(Glowcase.id("channel.text_block"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditTextBlock> PACKET_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, C2SEditTextBlock::pos,
		ByteBufCodecs.BYTE.map(index -> TextJustify.values()[index], textAlignment -> (byte) textAlignment.ordinal()),
		C2SEditTextBlock::justify,
		Anchor.STREAM_CODEC, C2SEditTextBlock::anchor,
		Vec3.STREAM_CODEC, C2SEditTextBlock::renderOffsets,
		Vec3.STREAM_CODEC, C2SEditTextBlock::rotation,
		TextBlockValues.PACKET_CODEC, C2SEditTextBlock::values,
		C2SEditTextBlock::new
	);

	public static C2SEditTextBlock of(TextBlockEntity be) {
		return new C2SEditTextBlock(
			be.getBlockPos(),
			be.textAlignment,
			be.anchor,
			be.offset,
			be.rotation,
			new TextBlockValues(be.shadow, be.scale, be.backgroundColor, be.color, be.lines)
		);
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
		be.textAlignment = this.justify();
		be.anchor = this.anchor();
		be.backgroundColor = this.values().backgroundColor();
		be.color = this.values().color();
		be.offset = this.renderOffsets();
		be.rotation = this.rotation();

		be.setChanged();
	}

	// separated for tuple call
	public record TextBlockValues(boolean shadow, float scale, int backgroundColor, int color, List<Component> lines) {
		public static final StreamCodec<RegistryFriendlyByteBuf, TextBlockValues> PACKET_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, TextBlockValues::shadow,
			ByteBufCodecs.FLOAT, TextBlockValues::scale,
			ByteBufCodecs.INT, TextBlockValues::backgroundColor,
			ByteBufCodecs.INT, TextBlockValues::color,
			ByteBufCodecs.collection(ArrayList::new, ComponentSerialization.STREAM_CODEC), TextBlockValues::lines,
			TextBlockValues::new
		);
	}
}
