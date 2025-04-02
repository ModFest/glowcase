package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.SpriteBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public record C2SEditSpriteBlock(BlockPos pos, String sprite, int rotation, TextBlockEntity.ZOffset offset, int color, float scale) implements C2SEditBlockEntity {
	public static final Id<C2SEditSpriteBlock> ID = new Id<>(Glowcase.id("channel.sprite.save"));
	public static final PacketCodec<RegistryByteBuf, C2SEditSpriteBlock> PACKET_CODEC = PacketCodec.tuple(
		BlockPos.PACKET_CODEC, C2SEditSpriteBlock::pos,
		PacketCodecs.STRING, C2SEditSpriteBlock::sprite,
		PacketCodecs.INTEGER, C2SEditSpriteBlock::rotation,
		PacketCodecs.INTEGER.xmap(index -> TextBlockEntity.ZOffset.values()[index], TextBlockEntity.ZOffset::ordinal), C2SEditSpriteBlock::offset,
		PacketCodecs.INTEGER, C2SEditSpriteBlock::color,
		PacketCodecs.FLOAT, C2SEditSpriteBlock::scale,
		C2SEditSpriteBlock::new
	);

	public static C2SEditSpriteBlock of(SpriteBlockEntity be) {
		return new C2SEditSpriteBlock(be.getPos(), be.getSprite(), be.rotation, be.zOffset, be.color, be.scale);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}

	@Override
	public void receive(ServerWorld world, BlockEntity blockEntity) {
		if (!(blockEntity instanceof SpriteBlockEntity be)) return;

		be.setSprite(this.sprite());
		be.rotation = this.rotation();
		be.zOffset = this.offset();
		be.color = this.color();
		be.scale = this.scale();

		be.markDirty();
	}
}
