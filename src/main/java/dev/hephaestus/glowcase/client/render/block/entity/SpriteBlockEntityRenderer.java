package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.SpriteBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import dev.hephaestus.glowcase.client.util.ModMetaUtil;
import dev.hephaestus.glowcase.mixin.client.TextureManagerAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public record SpriteBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<SpriteBlockEntity> {
	public static ResourceLocation ITEM_TEXTURE = Glowcase.id("textures/item/sprite_block.png");
	private static final Map<String, ResourceLocation> modIconCache = new ConcurrentHashMap<>();

	private static final Vector3f[] vertices = new Vector3f[] {
		new Vector3f(-0.5F, -0.5F, 0.0F),
		new Vector3f(0.5F, -0.5F, 0.0F),
		new Vector3f(0.5F, 0.5F, 0.0F),
		new Vector3f(-0.5F, 0.5F, 0.0F)
	};

	public void render(SpriteBlockEntity entity, float f, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Vec3 cameraPos) {
		if (entity.getLevel() == null || entity.getLevel().getBlockState(entity.getBlockPos()).isAir()) return;
		matrices.pushPose();
		matrices.translate(0.5D, 0.5D, 0.5D);

		matrices.mulPose(entity.getBlockState().getValue(BlockStateProperties.FACING).getRotation().mul(Axis.XP.rotationDegrees(-90.0F)));
		matrices.mulPose(Axis.ZN.rotationDegrees(entity.rotation));

		switch (entity.zOffset) {
			case FRONT -> matrices.translate(0D, 0D, 0.4D);
			case BACK -> matrices.translate(0D, 0D, -0.4D);
		}

		matrices.scale(entity.scale, entity.scale, entity.scale);

		Minecraft client = Minecraft.getInstance();
		var entry = matrices.last();
		if (entity.getRenderItem() != null) {
			client.getItemRenderer().renderStatic(entity.getRenderItem(),
				ItemDisplayContext.FIXED, light, overlay, matrices, vertexConsumers, entity.getLevel(), 0);
		} else {
			ResourceLocation identifier = ResourceLocation.tryBuild(Glowcase.MODID, "textures/sprite/" + entity.getSprite() + ".png");
			boolean isMod = false; // Used for the invalid texture check further down
			if (identifier == null) {
				// Identifiers ending in / are always invalid, but tryParse logs an error when attempting to parse.
				// Just force the identifier to null here instead.
				identifier = entity.getSprite().endsWith("/") ? null : ResourceLocation.tryParse(entity.getSprite());
				if (identifier == null) {
					identifier = Glowcase.id("textures/sprite/invalid.png");
				} else if (identifier.getNamespace().equals("mod")) { // Special mod namespace uses mod icon.
					String modId = identifier.getPath();
					if (!modIconCache.containsKey(modId)) {
						// Attempt to register mod icon and put it into the cache. Invalid icons will fall to
						// the else branch and use the invalid icon.
						Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer(modId)
							.or(() -> FabricLoader.getInstance().getModContainer(modId.replace("_", "-")))
							.or(() -> FabricLoader.getInstance().getModContainer(modId.replace("_", "")));
						DynamicTexture icon = mod.map(modContainer -> ModMetaUtil.getIcon(modContainer, 64 * client.options.guiScale().get())).orElse(null);
						if (icon != null) {
							// Needs to end in .png for the missing texture check further below.
							modIconCache.put(modId, ResourceLocation.fromNamespaceAndPath(Glowcase.MODID, modId + "_icon.png"));
							client.getTextureManager().register(modIconCache.get(modId), icon);
							identifier = modIconCache.get(modId);
						} else {
							identifier = Glowcase.id("textures/sprite/invalid.png");
						}
					} else {
						// Mod is in the cache, just grab the identifier for it.
						identifier = modIconCache.get(modId);
						isMod = true;
					}
				}
			}
			TextureManager textureManager = client.getTextureManager();
			ResourceManager resourceManager = ((TextureManagerAccessor) textureManager).glowcase$getResourceManager();
			if (resourceManager.getResource(identifier).isEmpty() && !isMod || !identifier.getPath().endsWith(".png")) {
				/*
				If the texture (file) does not exist, just replace it.
				This happens a lot when editing a sprite block, so I'm adding it to avoid log spam
				- SkyNotTheLimit
				 */
				identifier = Glowcase.id("textures/sprite/invalid.png");
			}
			var vertexConsumer = vertexConsumers.getBuffer(RenderType.entityCutout(identifier));

			vertex(entry, vertexConsumer, vertices[0], 0, 1, entity.color);
			vertex(entry, vertexConsumer, vertices[1], 1, 1, entity.color);
			vertex(entry, vertexConsumer, vertices[2], 1, 0, entity.color);
			vertex(entry, vertexConsumer, vertices[3], 0, 0, entity.color);
		}

		matrices.popPose();

		if (entity.getSprite().isEmpty() || BlockEntityRenderUtil.shouldRenderPlaceholder(entity.getBlockPos())) BlockEntityRenderUtil.renderFacingPlaceholder(entity, ITEM_TEXTURE, 1.0F, matrices, vertexConsumers);
	}

	private void vertex(
		PoseStack.Pose matrix, VertexConsumer vertexConsumer, Vector3f vertex, float u, float v, int color) {
		vertexConsumer.addVertex(matrix, vertex.x(), vertex.y(), vertex.z())
			.setColor(color)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(LightTexture.FULL_BRIGHT)
			.setNormal(0, 1, 0);
	}
}
