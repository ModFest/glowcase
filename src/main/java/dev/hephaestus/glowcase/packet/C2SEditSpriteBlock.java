package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.SpriteBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public record C2SEditSpriteBlock(BlockPos pos, String sprite, int rotation, TextBlockEntity.ZOffset offset, int color, float scale) implements C2SEditBlockEntity {
	public static final Type<C2SEditSpriteBlock> ID = new Type<>(Glowcase.id("channel.sprite.save"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditSpriteBlock> PACKET_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, C2SEditSpriteBlock::pos,
		ByteBufCodecs.STRING_UTF8, C2SEditSpriteBlock::sprite,
		ByteBufCodecs.INT, C2SEditSpriteBlock::rotation,
		ByteBufCodecs.INT.map(index -> TextBlockEntity.ZOffset.values()[index], TextBlockEntity.ZOffset::ordinal), C2SEditSpriteBlock::offset,
		ByteBufCodecs.INT, C2SEditSpriteBlock::color,
		ByteBufCodecs.FLOAT, C2SEditSpriteBlock::scale,
		C2SEditSpriteBlock::new
	);

	public static C2SEditSpriteBlock of(SpriteBlockEntity be) {
		return new C2SEditSpriteBlock(be.getBlockPos(), be.getSprite(), be.rotation, be.zOffset, be.color, be.scale);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	@Override
	public void receive(ServerLevel world, BlockEntity blockEntity) {
		if (!(blockEntity instanceof SpriteBlockEntity be)) return;

		be.setSprite(this.sprite());
		be.rotation = this.rotation();
		be.zOffset = this.offset();
		be.color = this.color();
		be.scale = this.scale();

		be.setChanged();
	}
}
