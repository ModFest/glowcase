package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.SpriteBlock;
import dev.hephaestus.glowcase.block.entity.SpriteBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import dev.hephaestus.glowcase.client.util.ModMetaUtil;
import dev.hephaestus.glowcase.mixin.client.TextureManagerAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public record SpriteBlockEntityRenderer(
	BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<SpriteBlockEntity, SpriteBlockEntityRenderer.SpriteRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/sprite_block.png");
	private static final Map<String, Identifier> modIconCache = new ConcurrentHashMap<>();

	private static final Vector3f[] vertices = new Vector3f[]{
		new Vector3f(-0.5F, -0.5F, 0.0F),
		new Vector3f(0.5F, -0.5F, 0.0F),
		new Vector3f(0.5F, 0.5F, 0.0F),
		new Vector3f(-0.5F, 0.5F, 0.0F)
	};

	public static class SpriteRenderState extends BlockEntityRenderState {
		public ItemStackRenderState renderItem = new ItemStackRenderState();
		public Identifier sprite;
		public Direction facing;
		public int color;
		public float rotation;
		public TextBlockEntity.ZOffset zOffset;
		public float scale;
	}

	@Override
	public SpriteRenderState createRenderState() {
		return new SpriteRenderState();
	}

	@Override
	public void extractRenderState(SpriteBlockEntity blockEntity, SpriteRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

		// TODO: e-ink technology:tm: (aka, allow fullbright vs. not)

		if (blockEntity.getRenderItem() != null) {
			this.context.itemModelResolver().updateForTopItem(state.renderItem, blockEntity.getRenderItem(), ItemDisplayContext.FIXED, blockEntity.getLevel(), null, (int) state.blockPos.asLong());
			state.sprite = null;
		} else {
			state.renderItem.clear();
			state.sprite = meow(blockEntity.getSprite());
		}
		state.facing = blockEntity.getBlockState().getValue(SpriteBlock.FACING);
		state.color = blockEntity.color;
		state.rotation = blockEntity.rotation;
		state.zOffset = blockEntity.zOffset;
		state.scale = blockEntity.scale;
	}

	@Override
	public void submit(SpriteRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if ((state.sprite == null && state.renderItem.isEmpty()) || BlockEntityRenderUtil.shouldRenderPlaceholder(state.blockPos)) {
			BlockEntityRenderUtil.renderFacingPlaceholder(state, state.facing, ITEM_TEXTURE, 1.0F, matrices, submitNodeCollector);
		}

		matrices.pushPose();
		matrices.translate(0.5D, 0.5D, 0.5D);

		matrices.mulPose(state.facing.getRotation().mul(Axis.XP.rotationDegrees(-90.0F)));
		matrices.mulPose(Axis.ZN.rotationDegrees(state.rotation));

		switch (state.zOffset) {
			case FRONT -> matrices.translate(0D, 0D, 0.4D);
			case BACK -> matrices.translate(0D, 0D, -0.4D);
		}

		matrices.scale(state.scale, state.scale, state.scale);

		if (!state.renderItem.isEmpty()) {
			// FIXME: tint
			state.renderItem.submit(matrices, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		} else {
			submitNodeCollector.submitCustomGeometry(matrices, RenderTypes.entityCutout(state.sprite), (pose, vertexConsumer) -> {
				vertex(pose, vertexConsumer, vertices[0], 0, 1, state.lightCoords, state.color);
				vertex(pose, vertexConsumer, vertices[1], 1, 1, state.lightCoords, state.color);
				vertex(pose, vertexConsumer, vertices[2], 1, 0, state.lightCoords, state.color);
				vertex(pose, vertexConsumer, vertices[3], 0, 0, state.lightCoords, state.color);
			});
		}

		matrices.popPose();
	}

	private static Identifier meow(final String sprite) {
		final Minecraft client = Minecraft.getInstance();
		Identifier identifier = Identifier.tryBuild(Glowcase.MODID, "textures/sprite/" + sprite + ".png");

		if (isValid(client, false, identifier)) {
			return identifier;
		}

		boolean isMod = false; // Used for the invalid texture check further down
		// Identifiers ending in / are always invalid, but tryParse logs an error when attempting to parse.
		// Just force the identifier to null here instead.
		identifier = sprite.endsWith("/") ? null : Identifier.tryParse(sprite);

		if (isValid(client, false, identifier)) {
			return identifier;
		}

		if (identifier.getNamespace().equals("mod")) { // Special mod namespace uses mod icon.
			String modId = identifier.getPath();
			modIconCache.computeIfAbsent(modId, SpriteBlockEntityRenderer::fetchModIcon);
			if (!modIconCache.containsKey(modId)) {
				// Attempt to register mod icon and put it into the cache. Invalid icons will fall to
				// the else branch and use the invalid icon.
				Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer(modId)
					.or(() -> FabricLoader.getInstance().getModContainer(modId.replace("_", "-")))
					.or(() -> FabricLoader.getInstance().getModContainer(modId.replace("_", "")));
				DynamicTexture icon = mod.map(modContainer -> ModMetaUtil.getIcon(modContainer, 64 * client.options.guiScale().get())).orElse(null);
				if (icon != null) {
					// Needs to end in .png for the missing texture check further below.
					modIconCache.put(modId, Identifier.fromNamespaceAndPath(Glowcase.MODID, modId + "_icon.png"));
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

		if (!isValid(client, isMod, identifier)) {
			return Glowcase.id("textures/sprite/invalid.png");
		}
		return identifier;
	}

	private static boolean isValid(final Minecraft client, final boolean isMod, final @Nullable Identifier id) {
		if (id == null) {
			return false;
		}

		TextureManager textureManager = client.getTextureManager();
		ResourceManager resourceManager = ((TextureManagerAccessor) textureManager).glowcase$getResourceManager();

		/*
		 * If the texture (file) does not exist, just replace it.
		 * This happens a lot when editing a sprite block, so I'm adding it to avoid log spam
		 * - SkyNotTheLimit
		 */
		return (resourceManager.getResource(id).isPresent() || isMod) && id.getPath().endsWith(".png");
	}

	private static Identifier fetchModIcon(final String modId) {
		final Minecraft client = Minecraft.getInstance();

		// Attempt to register mod icon and put it into the cache. Invalid icons will fall to
		// the else branch and use the invalid icon.
		Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer(modId)
			.or(() -> FabricLoader.getInstance().getModContainer(modId.replace("_", "-")))
			.or(() -> FabricLoader.getInstance().getModContainer(modId.replace("_", "")));
		DynamicTexture icon = mod.map(modContainer -> ModMetaUtil.getIcon(modContainer, 64 * client.options.guiScale().get())).orElse(null);

		if (icon == null) {
			return Glowcase.id("textures/sprite/invalid.png");
		}

		// Needs to end in .png for the missing texture check.
		final Identifier iconId = Identifier.fromNamespaceAndPath(Glowcase.MODID, modId + "_icon.png");
		client.getTextureManager().register(iconId, icon);
		return iconId;
	}

	private void vertex(
		PoseStack.Pose matrix, VertexConsumer vertexConsumer, Vector3f vertex, float u, float v, int light, int color) {
		vertexConsumer.addVertex(matrix, vertex.x(), vertex.y(), vertex.z())
			.setColor(color)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(0, 1, 0);
	}
}
