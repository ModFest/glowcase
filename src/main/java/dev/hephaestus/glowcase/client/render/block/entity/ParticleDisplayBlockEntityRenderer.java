package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ParticleDisplayBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import dev.hephaestus.glowcase.util.DeviatedInteger;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record ParticleDisplayBlockEntityRenderer(
	BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<ParticleDisplayBlockEntity, ParticleDisplayBlockEntityRenderer.ParticleDisplayRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/particle_display.png");

	public static class ParticleDisplayRenderState extends BlockEntityRenderState {
		public boolean shouldRenderPlaceholder;
	}

	@Override
	public ParticleDisplayRenderState createRenderState() {
		return new ParticleDisplayRenderState();
	}

	@Override
	public void extractRenderState(ParticleDisplayBlockEntity blockEntity, ParticleDisplayRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		state.shouldRenderPlaceholder = blockEntity.count.equals(DeviatedInteger.ZERO) || BlockEntityRenderUtil.shouldRenderPlaceholder(state.blockPos, false);
	}

	@Override
	public void submit(ParticleDisplayRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (state.shouldRenderPlaceholder) {
			BlockEntityRenderUtil.renderBillboardPlaceholder(state, ITEM_TEXTURE, 1.0F, poseStack, submitNodeCollector, camera);
		}
	}
}
