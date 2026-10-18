package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.EntityDisplayBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import dev.hephaestus.glowcase.client.util.Quaternionsf;
import dev.hephaestus.glowcase.mixinsupport.PartPosedRenderState;
import dev.hephaestus.glowcase.util.PartPoseInfo;
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

import java.util.Map;

public record EntityDisplayBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<EntityDisplayBlockEntity, EntityDisplayBlockEntityRenderer.EntityDisplayRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/entity_display_block.png");

	public static class EntityDisplayRenderState extends BlockEntityRenderState {
		public float yaw;
		public float pitch;
		public float roll;

		public Vec3 offset;
		public Vector3f scale;

		public Entity entity;

		public Map<String, PartPoseInfo> partPoseInfoMap;
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
		state.roll = blockEntity.getRoll();
		// TODO: hmm, yes, shuttle the *entire entity* to the render state
		// Perhaps shuttle the entity renderer & its state instead?
		state.entity = blockEntity.getDisplayEntity();
		state.partPoseInfoMap = blockEntity.getPartPoses();
	}

	@Override
	public void submit(EntityDisplayRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5D, 0D, 0.5D);
		poseStack.rotate(Axis.YP.rotationDegrees(state.yaw));
		poseStack.translate(state.offset);
		poseStack.scale(state.scale.x, state.scale.y, state.scale.z);
		poseStack.rotate(Quaternionsf.rotateDegreesYXZ(0, state.pitch, state.roll));
		Entity renderEntity = state.entity;
		if (renderEntity != null) {
			//noinspection unchecked
			EntityRenderer<Entity, EntityRenderState> entityRenderer = (EntityRenderer<Entity, EntityRenderState>) context.entityRenderer().getRenderer(renderEntity);
			EntityRenderState entityRenderState = entityRenderer.createRenderState(renderEntity, 0);
			((PartPosedRenderState) entityRenderState).glowcase$setPartPoses(state.partPoseInfoMap.isEmpty() ? null : state.partPoseInfoMap);
			entityRenderer.submit(entityRenderState, poseStack, submitNodeCollector, camera);
		}

		poseStack.popPose();

		if (renderEntity == null || BlockEntityRenderUtil.shouldRenderPlaceholder(state.blockPos)) {
			BlockEntityRenderUtil.renderCenteredPlaceholder(state, ITEM_TEXTURE, 1.0F, Axis.YP.rotationDegrees(state.yaw), poseStack, submitNodeCollector);
		}
	}

}
