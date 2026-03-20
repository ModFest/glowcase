package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.OutlineBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.Nullable;

public record OutlineBlockEntityRenderer(
	BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<OutlineBlockEntity, OutlineBlockEntityRenderer.OutlineRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/outline_block.png");

	public static class OutlineRenderState extends BlockEntityRenderState {
		public boolean shouldRenderPlaceholder;
		public Vec3i offset;
		public Vec3i scale;
		public int color;
	}

	@Override
	public OutlineRenderState createRenderState() {
		return new OutlineRenderState();
	}

	@Override
	public void extractRenderState(OutlineBlockEntity blockEntity, OutlineRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		state.shouldRenderPlaceholder = blockEntity.scale.equals(Vec3i.ZERO) || BlockEntityRenderUtil.shouldRenderPlaceholder(blockEntity.getBlockPos());
		state.offset = blockEntity.offset;
		state.scale = blockEntity.scale;
		state.color = blockEntity.color;
	}

	@Override
	public void submit(OutlineRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (state.shouldRenderPlaceholder) {
			BlockEntityRenderUtil.renderBillboardPlaceholder(state, ITEM_TEXTURE, 1.0F, poseStack, submitNodeCollector, camera);
		}

		double x = state.offset.getX();
		double y = state.offset.getY();
		double z = state.offset.getZ();
		double width = state.scale.getX();
		double height = state.scale.getY();
		double depth = state.scale.getZ();

		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), new SubmitNodeCollector.CustomGeometryRenderer() {
			@Override
			public void render(PoseStack.Pose pose, VertexConsumer buffer) {
				poseStack.pushPose();
				poseStack.mulPose(pose.pose());
				ShapeRenderer.renderShape(
					poseStack, buffer,
					Shapes.box(x, y, z, x + width, y + height, z + depth),
					0, 0, 0, state.color | 0xFF000000, 1
				);
				poseStack.popPose();
			}
		});
	}
}
