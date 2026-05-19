package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.EntityDisplayBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public record EntityDisplayBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<EntityDisplayBlockEntity, EntityDisplayBlockEntityRenderer.EntityDisplayRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/entity_display_block.png");

	public static class EntityDisplayRenderState extends BlockEntityRenderState {
		public float yaw;
		public float pitch;

		public Vec3 offset;
		public Vector3f scale;

		public Entity entity;
	}

	@Override
	public EntityDisplayRenderState createRenderState() {
		return new EntityDisplayRenderState();
	}

	@Override
	public void extractRenderState(EntityDisplayBlockEntity blockEntity, EntityDisplayRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		state.offset = new Vec3(blockEntity.getOffset());
		state.scale = new Vector3f(blockEntity.getScale());
		state.yaw = blockEntity.getYaw();
		state.pitch = blockEntity.getPitch();
		// TODO: hmm, yes, shuttle the *entire entity* to the render state
		// Perhaps shuttle the entity renderer & its state instead?
		state.entity = blockEntity.getDisplayEntity();
	}

	@Override
	public void submit(EntityDisplayRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		matrices.pushPose();
		matrices.translate(0.5D, 0D, 0.5D);
		matrices.mulPose(Axis.YP.rotationDegrees(state.yaw));
		matrices.translate(state.offset);
		matrices.scale(state.scale.x, state.scale.y, state.scale.z);
		matrices.mulPose(Axis.XP.rotationDegrees(state.pitch));
		Entity renderEntity = state.entity;
		if (renderEntity != null) {
			//noinspection unchecked
			EntityRenderer<Entity, EntityRenderState> entityRenderer = (EntityRenderer<Entity, EntityRenderState>) context.entityRenderer().getRenderer(renderEntity);
			EntityRenderState entityRenderState = entityRenderer.createRenderState(renderEntity, 0);
			entityRenderer.submit(entityRenderState, matrices, submitNodeCollector, camera);
		}

		matrices.popPose();

		if (renderEntity == null || BlockEntityRenderUtil.shouldRenderPlaceholder(state.blockPos)) {
			BlockEntityRenderUtil.renderCenteredPlaceholder(state, ITEM_TEXTURE, 1.0F, Axis.YP.rotationDegrees(state.yaw), matrices, submitNodeCollector);
		}
	}

}
