package dev.hephaestus.glowcase.client.render.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.PopupBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public record PopupBlockEntityRenderer(BlockEntityRendererFactory.Context context) implements BlockEntityRenderer<PopupBlockEntity> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/popup_block.png");

	public void render(PopupBlockEntity entity, float f, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, Vec3d cameraPos) {
		if (entity.getWorld() == null || entity.getWorld().getBlockState(entity.getPos()).isAir()) return;
		Camera camera = context.getRenderDispatcher().camera;
		BlockEntityRenderUtil.renderBillboardPlaceholder(entity, ITEM_TEXTURE, 0.5F, matrices, vertexConsumers, camera);

		matrices.push();
		if (MinecraftClient.getInstance().crosshairTarget instanceof BlockHitResult bhr && bhr.getBlockPos().equals(entity.getPos())) {
			Text title;
			if (entity.lines.size() == 1 && entity.lines.getFirst().getContent().equals(PlainTextContent.EMPTY)) {
				title = Text.translatable("gui.glowcase.warning.no_content").formatted(Formatting.RED);
			} else {
				title = Text.literal(entity.title);
			}

			matrices.translate(0.5D, 0.5D, 0.5D);
			matrices.scale(0.5F, 0.5F, 0.5F);
			float n = -camera.getYaw();
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
			float scale = 0.025F;
			matrices.scale(scale, scale, scale);
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));
			matrices.translate(-context.getTextRenderer().getWidth(title) / 2F, -4, -scale);
			// Fixes shadow being rendered in front of actual text
			matrices.scale(1, 1, -1);
			context.getTextRenderer().draw(title, 0, 0, 0xFFFFFFFF, true, matrices.peek().getPositionMatrix(), vertexConsumers, TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
		}
		matrices.pop();
	}
}
