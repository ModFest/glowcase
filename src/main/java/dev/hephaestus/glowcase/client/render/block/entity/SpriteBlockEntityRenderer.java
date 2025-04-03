package dev.hephaestus.glowcase.client.render.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.SpriteBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import dev.hephaestus.glowcase.mixin.client.TextureManagerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

public record SpriteBlockEntityRenderer(BlockEntityRendererFactory.Context context) implements BlockEntityRenderer<SpriteBlockEntity> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/sprite_block.png");

	private static final Vector3f[] vertices = new Vector3f[] {
		new Vector3f(-0.5F, -0.5F, 0.0F),
		new Vector3f(0.5F, -0.5F, 0.0F),
		new Vector3f(0.5F, 0.5F, 0.0F),
		new Vector3f(-0.5F, 0.5F, 0.0F)
	};

	public void render(SpriteBlockEntity entity, float f, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		if (entity.getWorld() == null || entity.getWorld().getBlockState(entity.getPos()).isAir()) return;
		matrices.push();
		matrices.translate(0.5D, 0.5D, 0.5D);

		matrices.multiply(entity.getCachedState().get(Properties.FACING).getRotationQuaternion().mul(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F)));
		matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(entity.rotation));

		switch (entity.zOffset) {
			case FRONT -> matrices.translate(0D, 0D, 0.4D);
			case BACK -> matrices.translate(0D, 0D, -0.4D);
		}

		matrices.scale(entity.scale, entity.scale, entity.scale);

		var entry = matrices.peek();
		if (entity.getRenderItem() != null) {
			MinecraftClient.getInstance().getItemRenderer().renderItem(entity.getRenderItem(),
				ModelTransformationMode.FIXED, light, overlay, matrices, vertexConsumers, entity.getWorld(), 0);
		} else {
			Identifier identifier = Identifier.tryParse(Glowcase.MODID, "textures/sprite/" + entity.getSprite() + ".png");
			if (identifier == null) {
				identifier = Identifier.tryParse(entity.getSprite());
				if (identifier == null) {
					identifier = Glowcase.id("textures/sprite/invalid.png");
				}
			} else {
				TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
				ResourceManager resourceManager = ((TextureManagerAccessor) textureManager).glowcase$getResourceManager();
				if (resourceManager.getResource(identifier).isEmpty()) {
				/*
				If the texture (file) does not exist, just replace it.
				This happens a lot when editing a sprite block, so I'm adding it to avoid log spam
				- SkyNotTheLimit
				 */
					identifier = Glowcase.id("textures/sprite/invalid.png");
				}
			}
			var vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(identifier));

			vertex(entry, vertexConsumer, vertices[0], 0, 1, entity.color);
			vertex(entry, vertexConsumer, vertices[1], 1, 1, entity.color);
			vertex(entry, vertexConsumer, vertices[2], 1, 0, entity.color);
			vertex(entry, vertexConsumer, vertices[3], 0, 0, entity.color);
		}

		matrices.pop();

		if (entity.getSprite().isEmpty() || BlockEntityRenderUtil.shouldRenderPlaceholder(entity.getPos())) BlockEntityRenderUtil.renderFacingPlaceholder(entity, ITEM_TEXTURE, 1.0F, matrices, vertexConsumers);
	}

	private void vertex(
		MatrixStack.Entry matrix, VertexConsumer vertexConsumer, Vector3f vertex, float u, float v, int color) {
		vertexConsumer.vertex(matrix, vertex.x(), vertex.y(), vertex.z())
			.color(color)
			.texture(u, v)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
			.normal(0, 1, 0);
	}
}
