package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.SoundPlayerBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record SoundPlayerBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<SoundPlayerBlockEntity> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/sound_block.png");

	@Override
	public void render(SoundPlayerBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Vec3 cameraPos) {
		if (entity.getLevel() == null || entity.getLevel().getBlockState(entity.getBlockPos()).isAir()) return;
		if (BlockEntityRenderUtil.shouldRenderPlaceholder(entity.getBlockPos(), false)) {
			BlockEntityRenderUtil.renderBillboardPlaceholder(entity, ITEM_TEXTURE, 1.0F, matrices, vertexConsumers, context.getBlockEntityRenderDispatcher().camera);
		}
	}
}
