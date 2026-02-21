package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.OutlineBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public record OutlineBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<OutlineBlockEntity> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/outline_block.png");

	public void render(OutlineBlockEntity entity, float f, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Vec3 cameraPos) {
		if (entity.getLevel() == null || entity.getLevel().getBlockState(entity.getBlockPos()).isAir()) return;
		double x = entity.offset.getX();
		double y = entity.offset.getY();
		double z = entity.offset.getZ();
		double width = entity.scale.getX();
		double height = entity.scale.getY();
		double depth = entity.scale.getZ();

		ShapeRenderer.renderShape(
			matrices, vertexConsumers.getBuffer(RenderType.lines()),
			Shapes.box(x, y, z, x + width, y + height, z + depth),
			0, 0, 0, entity.color | 0xFF000000
		);

		if (entity.scale.equals(Vec3i.ZERO) || BlockEntityRenderUtil.shouldRenderPlaceholder(entity.getBlockPos())) BlockEntityRenderUtil.renderBillboardPlaceholder(entity, ITEM_TEXTURE, 1.0F, matrices, vertexConsumers, context.getBlockEntityRenderDispatcher().camera);
	}
}
