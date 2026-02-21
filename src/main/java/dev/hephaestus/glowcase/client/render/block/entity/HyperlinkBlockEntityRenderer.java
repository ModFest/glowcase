package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.HyperlinkBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public record HyperlinkBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<HyperlinkBlockEntity> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/hyperlink_block.png");

	public void render(HyperlinkBlockEntity entity, float f, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Vec3 cameraPos) {
		if (entity.getLevel() == null || entity.getLevel().getBlockState(entity.getBlockPos()).isAir()) return;
		Camera camera = context.getBlockEntityRenderDispatcher().camera;
		BlockEntityRenderUtil.renderBillboardPlaceholder(entity, ITEM_TEXTURE, 0.5F, matrices, vertexConsumers, camera);
		Component title;
		if (entity.getUrl().isBlank()) {
			title = Component.translatable("gui.glowcase.warning.no_content").withStyle(ChatFormatting.RED);
		} else {
			title = Component.literal(entity.getText());
		}

		matrices.pushPose();
		if (Minecraft.getInstance().hitResult instanceof BlockHitResult bhr && bhr.getBlockPos().equals(entity.getBlockPos())) {
			matrices.translate(0.5D, 0.5D, 0.5D);
			matrices.scale(0.5F, 0.5F, 0.5F);
			float n = -camera.getYRot();
			matrices.mulPose(Axis.YP.rotationDegrees(n));
			matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
			float scale = 0.025F;
			matrices.scale(scale, scale, scale);
			matrices.mulPose(Axis.ZP.rotationDegrees(180));
			matrices.translate(-context.getFont().width(title) / 2F, -4, -scale);
			// Fixes shadow being rendered in front of actual text
			matrices.scale(1, 1, -1);
			context.getFont().drawInBatch(title, 0, 0, 0xFFFFFFFF, true, matrices.last().pose(), vertexConsumers, DisplayMode.NORMAL, 0, Lightmap.FULL_BRIGHT);
		}
		matrices.popPose();
	}
}
