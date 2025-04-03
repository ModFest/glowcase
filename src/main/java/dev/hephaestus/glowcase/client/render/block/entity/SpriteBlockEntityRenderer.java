package dev.hephaestus.glowcase.client.render.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.SpriteBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import dev.hephaestus.glowcase.client.util.ModMetaUtil;
import dev.hephaestus.glowcase.mixin.client.TextureManagerAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public record SpriteBlockEntityRenderer(BlockEntityRendererFactory.Context context) implements BlockEntityRenderer<SpriteBlockEntity> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/sprite_block.png");
	private static final Map<String, Identifier> modIconCache = new ConcurrentHashMap<>();

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

		MinecraftClient client = MinecraftClient.getInstance();
		var entry = matrices.peek();
		if (entity.getRenderItem() != null) {
			client.getItemRenderer().renderItem(entity.getRenderItem(),
				ModelTransformationMode.FIXED, light, overlay, matrices, vertexConsumers, entity.getWorld(), 0);
		} else {
			Identifier identifier = Identifier.tryParse(Glowcase.MODID, "textures/sprite/" + entity.getSprite() + ".png");
			if (identifier == null) {
				// Identifiers ending in / are always invalid, but tryParse logs an error when attempting to parse.
				identifier = entity.getSprite().endsWith("/") ? null : Identifier.tryParse(entity.getSprite());
				if (identifier == null) {
					identifier = Glowcase.id("textures/sprite/invalid.png");
				} else if (identifier.getNamespace().equals("mod")) {
					String modId = identifier.getPath();
					if (!modIconCache.containsKey(modId)) {
						modIconCache.put(modId, Identifier.of(Glowcase.MODID, modId + "_icon"));
						Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer(modId)
							.or(() -> FabricLoader.getInstance().getModContainer(modId.replace("_", "-")))
							.or(() -> FabricLoader.getInstance().getModContainer(modId.replace("_", "")));
						NativeImageBackedTexture icon = mod.map(modContainer -> ModMetaUtil.getIcon(modContainer, 64 * client.options.getGuiScale().getValue())).orElse(null);
						if (icon != null) {
							client.getTextureManager().registerTexture(modIconCache.get(modId), icon);
						}
					}
					identifier = modIconCache.get(modId);
				}
			}
			TextureManager textureManager = client.getTextureManager();
			ResourceManager resourceManager = ((TextureManagerAccessor) textureManager).glowcase$getResourceManager();
			// TODO: Non-image resources, invalid mod IDs, and invalid items in the glowcase namespace show vanilla's
			// TODO: missing texture icon rather than Glowcase's.
			if (resourceManager.getResource(identifier).isEmpty() && !identifier.getNamespace().equals("glowcase")) {
				/*
				If the texture (file) does not exist, just replace it.
				This happens a lot when editing a sprite block, so I'm adding it to avoid log spam
				- SkyNotTheLimit
				 */
				identifier = Glowcase.id("textures/sprite/invalid.png");
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
