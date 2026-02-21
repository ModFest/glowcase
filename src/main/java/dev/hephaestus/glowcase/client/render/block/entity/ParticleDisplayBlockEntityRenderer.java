package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ParticleDisplayBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import dev.hephaestus.glowcase.util.DeviatedInteger;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record ParticleDisplayBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<ParticleDisplayBlockEntity> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/particle_display.png");

	public void render(ParticleDisplayBlockEntity entity, float f, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Vec3 cameraPos) {
		if (entity.getLevel() == null || entity.getLevel().getBlockState(entity.getBlockPos()).isAir()) return;
		if (entity.count.equals(DeviatedInteger.ZERO) || BlockEntityRenderUtil.shouldRenderPlaceholder(entity.getBlockPos(), false)) {
			BlockEntityRenderUtil.renderBillboardPlaceholder(entity, ITEM_TEXTURE, 1.0F, matrices, vertexConsumers, context.getBlockEntityRenderDispatcher().camera);
		}
	}
}
