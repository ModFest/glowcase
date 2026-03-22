package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.PopupBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record PopupBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<PopupBlockEntity, PopupBlockEntityRenderer.PopupRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/popup_block.png");

	public static class PopupRenderState extends BlockEntityRenderState {
		public Component title;
	}

	@Override
	public PopupBlockEntityRenderer.PopupRenderState createRenderState() {
		return new PopupBlockEntityRenderer.PopupRenderState();
	}

	@Override
	public void extractRenderState(PopupBlockEntity blockEntity, PopupRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		if (blockEntity.lines.size() == 1 && blockEntity.lines.getFirst().getContents().equals(PlainTextContents.EMPTY)) {
			state.title = Component.translatable("gui.glowcase.warning.no_content").withStyle(ChatFormatting.RED);
		} else {
			state.title = Component.literal(blockEntity.title);
		}
	}

	@Override
	public void submit(PopupBlockEntityRenderer.PopupRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		BlockEntityRenderUtil.renderBillboardPlaceholder(state, ITEM_TEXTURE, 0.5F, poseStack, submitNodeCollector, camera);

		poseStack.pushPose();
		if (Minecraft.getInstance().hitResult instanceof BlockHitResult bhr && bhr.getBlockPos().equals(state.blockPos)) {
			poseStack.translate(0.5D, 0.5D, 0.5D);
			poseStack.scale(0.5F, 0.5F, 0.5F);
			float n = -camera.yRot;
			poseStack.mulPose(Axis.YP.rotationDegrees(n));
			poseStack.mulPose(Axis.XP.rotationDegrees(camera.xRot));
			float scale = 0.025F;
			poseStack.scale(scale, scale, scale);
			poseStack.mulPose(Axis.ZP.rotationDegrees(180));
			poseStack.translate(-context.font().width(state.title) / 2F, -4, -scale);
			// Fixes shadow being rendered in front of actual text
			poseStack.scale(1, 1, -1);
			submitNodeCollector.submitText(poseStack, 0, 0, state.title.getVisualOrderText(), true, DisplayMode.NORMAL, state.lightCoords, 0xFFFFFFFF, 0, 0);
		}
		poseStack.popPose();
	}
}
