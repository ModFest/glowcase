package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.SoundPlayerBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;

public record SoundPlayerBlockEntityRenderer(
	BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<SoundPlayerBlockEntity, SoundPlayerBlockEntityRenderer.SoundPlayerRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/sound_block.png");

	public static class SoundPlayerRenderState extends BlockEntityRenderState {

	}

	@Override
	public SoundPlayerRenderState createRenderState() {
		return new SoundPlayerRenderState();
	}

	@Override
	public void submit(SoundPlayerRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {

	}
// FIXME 26.1
//	@Override
//	public void render(SoundPlayerBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Vec3 cameraPos) {
//		if (entity.getLevel() == null || entity.getLevel().getBlockState(entity.getBlockPos()).isAir()) return;
//		if (BlockEntityRenderUtil.shouldRenderPlaceholder(entity.getBlockPos(), false)) {
//			BlockEntityRenderUtil.renderBillboardPlaceholder(entity, ITEM_TEXTURE, 1.0F, matrices, vertexConsumers, context.getBlockEntityRenderDispatcher().camera);
//		}
//	}
}
