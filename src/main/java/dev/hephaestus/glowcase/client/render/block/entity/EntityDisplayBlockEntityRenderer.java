package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.EntityDisplayBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public record EntityDisplayBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<EntityDisplayBlockEntity, EntityDisplayBlockEntityRenderer.EntityDisplayRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/entity_display_block.png");

	public static class EntityDisplayRenderState extends BlockEntityRenderState {}

	@Override
	public EntityDisplayRenderState createRenderState() {
		return new EntityDisplayRenderState();
	}

	@Override
	public void submit(EntityDisplayRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
// FIXME update to 26.1
		//		if (entity.getLevel() == null || entity.getLevel().getBlockState(entity.getBlockPos()).isAir()) return;
//
//		matrices.pushPose();
//		matrices.translate(0.5D, 0D, 0.5D);
//		matrices.mulPose(Axis.YP.rotationDegrees(entity.getYaw()));
//		matrices.translate(entity.getOffset().x(), entity.getOffset().y(), entity.getOffset().z());
//		matrices.scale(entity.getScale().x(), entity.getScale().y(), entity.getScale().z());
//		matrices.mulPose(Axis.XP.rotationDegrees(entity.getPitch()));
//		Entity renderEntity = entity.getDisplayEntity();
//		if (renderEntity != null) {
//			//noinspection unchecked
//			EntityRenderer<Entity, EntityRenderState> entityRenderer = (EntityRenderer<Entity, EntityRenderState>) context.getEntityRenderer().getRenderer(renderEntity);
//			EntityRenderState entityRenderState = entityRenderer.createRenderState(renderEntity, 0);
//			entityRenderer.render(entityRenderState, matrices, vertexConsumers, light);
//		}
//
//		matrices.popPose();
//
//		if (entity.matchesStack(ItemStack.EMPTY) || BlockEntityRenderUtil.shouldRenderPlaceholder(entity.getBlockPos())) BlockEntityRenderUtil.renderCenteredPlaceholder(entity, ITEM_TEXTURE, 1.0F, Axis.YP.rotationDegrees(entity.getYaw()), matrices, vertexConsumers);

	}

}
