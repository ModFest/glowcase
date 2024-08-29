package dev.hephaestus.glowcase.client.render.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.HyperlinkBlockEntity;
import dev.hephaestus.glowcase.block.entity.PopupBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.RotationAxis;

public record PopupBlockEntityRenderer(BlockEntityRendererFactory.Context context) implements BlockEntityRenderer<PopupBlockEntity> {
	private static final MinecraftClient mc = MinecraftClient.getInstance();

	public static final ItemStack STACK = new ItemStack(Glowcase.POPUP_BLOCK.get());

	public void render(PopupBlockEntity entity, float f, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		Camera camera = context.getRenderDispatcher().camera;
		matrices.push();
		matrices.translate(0.5D, 0.5D, 0.5D);
		matrices.scale(0.5F, 0.5F, 0.5F);
		float n = -camera.getYaw();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
		context.getItemRenderer().renderItem(STACK, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 0);

		matrices.pop();
	}
}