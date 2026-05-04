package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.RecipeBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record RecipeBlockEntityRenderer(
	BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<RecipeBlockEntity, RecipeBlockEntityRenderer.RecipeRenderState> {
	private static final Identifier ITEM_TEXTURE = Glowcase.id("textures/item/recipe_block.png");

	public static class RecipeRenderState extends BlockEntityRenderState {
		public TextBlockEntity.ZOffset zOffset;
		public int rotation16;
	}

	@Override
	public RecipeRenderState createRenderState() {
		return new RecipeRenderState();
	}

	@Override
	public void extractRenderState(RecipeBlockEntity blockEntity, RecipeRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		state.zOffset = blockEntity.zOffset;
		state.rotation16 = blockEntity.getBlockState().getValue(BlockStateProperties.ROTATION_16);
	}

	@Override
	public void submit(RecipeRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
//		if (BlockEntityRenderUtil.shouldRenderPlaceholder(state.blockPos)) {
			BlockEntityRenderUtil.renderPlaceholderWithBlockRotation(state, state.rotation16, ITEM_TEXTURE, 1F, poseStack, submitNodeCollector, state.zOffset == TextBlockEntity.ZOffset.CENTER ? 0.01F : state.zOffset == TextBlockEntity.ZOffset.FRONT ? 0.4F : -0.4F);
//		}
	}
// FIXME 26.1
//	public void render(RecipeBlockEntity entity, float f, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Vec3 cameraPos) {
//		if (GlowcaseClient.EMI_LOADED) {
//			matrices.pushPose();
//			matrices.translate(0.5D, 0.5D, 0.5D);
//
//			float rotation = -(entity.getBlockState().getValue(BlockStateProperties.ROTATION_16) * 360) / 16.0F;
//
//			matrices.mulPose(Axis.YP.rotationDegrees(rotation));
//			matrices.mulPose(Axis.YP.rotationDegrees(180));
//			matrices.mulPose(Axis.YP.rotationDegrees(entity.rotationY));
//			matrices.mulPose(Axis.XP.rotationDegrees(entity.rotationX));
//
//			switch (entity.zOffset) {
//				case FRONT -> matrices.translate(0D, 0D, -0.4D);
//				case BACK -> matrices.translate(0D, 0D, 0.4D);
//				case CENTER -> matrices.translate(0D, 0D, 0D);
//			}
//
////			boolean rendered = EmiWorldRenderUtils.renderRecipe(matrices, entity.recipe, entity.getPos());
//			matrices.popPose();
////			if (rendered) return;
//		} else {
//			matrices.pushPose();
//			matrices.translate(0.5D, 0.5D, 0.5D);
//
//			float rotation = -(entity.getBlockState().getValue(BlockStateProperties.ROTATION_16) * 360) / 16.0F;
//
//			matrices.mulPose(Axis.YP.rotationDegrees(rotation));
//			matrices.mulPose(Axis.YP.rotationDegrees(180));
//			matrices.mulPose(Axis.YP.rotationDegrees(entity.rotationY));
//			matrices.mulPose(Axis.XP.rotationDegrees(entity.rotationX));
//
//			VertexConsumer buffer = vertexConsumers.getBuffer(RenderType.textBackground());
//			Matrix4f matrix4f = matrices.last().pose();
//
//			buffer.addVertex(matrix4f, -0.5f, -0.5f, 0f).setColor(0xFF111111).setLight(light);
//			buffer.addVertex(matrix4f, -0.5f, 0.5f, 0f).setColor(0xFF111111).setLight(light);
//			buffer.addVertex(matrix4f, 0.5f, 0.5f, 0f).setColor(0xFF111111).setLight(light);
//			buffer.addVertex(matrix4f, 0.5f, -0.5f, 0f).setColor(0xFF111111).setLight(light);
//
//			ScreenBlockEntityRenderer.renderTextCentered("EMI not loaded", 0xFFFF8888, 1f, 1f, matrices, vertexConsumers, this.context.getFont(), light);
//			matrices.popPose();
//		}

//	}
}
