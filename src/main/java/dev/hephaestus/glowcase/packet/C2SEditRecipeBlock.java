package dev.hephaestus.glowcase.packet;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.RecipeBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public record C2SEditRecipeBlock(BlockPos pos, String recipe, TextBlockEntity.ZOffset offset, float rotationX, float rotationY) implements C2SEditBlockEntity {
	public static final Type<C2SEditRecipeBlock> ID = new Type<>(Glowcase.id("channel.recipe.save"));
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditRecipeBlock> PACKET_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, C2SEditRecipeBlock::pos,
		ByteBufCodecs.STRING_UTF8, C2SEditRecipeBlock::recipe,
		ByteBufCodecs.INT.map(index -> TextBlockEntity.ZOffset.values()[index], TextBlockEntity.ZOffset::ordinal), C2SEditRecipeBlock::offset,
		ByteBufCodecs.FLOAT, C2SEditRecipeBlock::rotationX,
		ByteBufCodecs.FLOAT, C2SEditRecipeBlock::rotationY,
		C2SEditRecipeBlock::new
	);

	public static C2SEditRecipeBlock of(RecipeBlockEntity be) {
		return new C2SEditRecipeBlock(be.getBlockPos(), be.recipe, be.zOffset, be.rotationX, be.rotationY);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	@Override
	public void receive(ServerLevel world, BlockEntity blockEntity) {
		if (!(blockEntity instanceof RecipeBlockEntity be)) return;

		be.setRecipe(this.recipe());
		be.zOffset = this.offset();
		be.rotationX = this.rotationX();
		be.rotationY = this.rotationY();

		be.setChanged();
	}
}
