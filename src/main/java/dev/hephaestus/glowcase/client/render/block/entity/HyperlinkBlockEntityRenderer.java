package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.HyperlinkBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import dev.hephaestus.glowcase.client.util.RenderText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record HyperlinkBlockEntityRenderer(
	BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<HyperlinkBlockEntity, HyperlinkBlockEntityRenderer.HyperlinkRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/hyperlink_block.png");

	public static class HyperlinkRenderState extends BlockEntityRenderState {
		public FormattedCharSequence title = FormattedCharSequence.EMPTY;
	}

	@Override
	public HyperlinkRenderState createRenderState() {
		return new HyperlinkRenderState();
	}

	@Override
	public void extractRenderState(HyperlinkBlockEntity blockEntity, HyperlinkRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		if (blockEntity.getUrl().isBlank()) {
			state.title = Component.translatable("gui.glowcase.warning.no_content").withStyle(ChatFormatting.RED).getVisualOrderText();
		} else {
			state.title = Component.literal(blockEntity.getText()).getVisualOrderText();
		}
	}

	@Override
	public void submit(HyperlinkRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		BlockEntityRenderUtil.renderBillboardPlaceholder(state, ITEM_TEXTURE, 0.5F, poseStack, submitNodeCollector, camera);

		if (BlockEntityRenderUtil.isBeingLookedAt(state.blockPos)) {
			RenderText.billboardCenteredText(
				submitNodeCollector,
				poseStack,
				camera,
				context.font(),
				0,
				-4,
				0.025F,
				state.title,
				0xFFFFFFFF
			);
		}
	}

}
