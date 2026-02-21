package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ConfigLinkBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record ConfigLinkBlockEntityRenderer(
	BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<ConfigLinkBlockEntity, ConfigLinkBlockEntityRenderer.ConfigLinkRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/config_link_block.png");

	public static class ConfigLinkRenderState extends BlockEntityRenderState {
		public FormattedCharSequence text = FormattedCharSequence.EMPTY;
	}

	@Override
	public ConfigLinkRenderState createRenderState() {
		return new ConfigLinkRenderState();
	}

	@Override
	public void extractRenderState(ConfigLinkBlockEntity blockEntity, ConfigLinkRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		state.text = Component.literal(blockEntity.getText()).getVisualOrderText();
	}

	@Override
	public void submit(ConfigLinkRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		BlockEntityRenderUtil.renderBillboardPlaceholder(state, ITEM_TEXTURE, 0.5F, poseStack, submitNodeCollector, camera);

		poseStack.pushPose();
		if (Minecraft.getInstance().hitResult instanceof BlockHitResult bhr //
			&& bhr.getBlockPos().equals(state.blockPos)) {
			poseStack.translate(0.5D, 0.5D, 0.5D);
			poseStack.scale(0.5F, 0.5F, 0.5F);
			float n = -camera.yRot;
			poseStack.mulPose(Axis.YP.rotationDegrees(n));
			poseStack.mulPose(Axis.XP.rotationDegrees(camera.xRot));
			float scale = 0.025F;
			poseStack.scale(scale, scale, scale);
			poseStack.mulPose(Axis.ZP.rotationDegrees(180));
			poseStack.translate(-context.font().width(state.text) / 2F, -4, -scale);
			// Fixes shadow being rendered in front of actual text
			poseStack.scale(1, 1, -1);

			submitNodeCollector.submitText(poseStack, 0, 0, state.text, true, DisplayMode.NORMAL, 0xFF, 0xFFFFFFFF, 0, 0);
		}
		poseStack.popPose();
	}

}
