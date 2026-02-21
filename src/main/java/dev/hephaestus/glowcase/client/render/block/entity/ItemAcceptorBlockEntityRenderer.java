package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.ItemAcceptorBlock;
import dev.hephaestus.glowcase.block.entity.ItemAcceptorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public record ItemAcceptorBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<ItemAcceptorBlockEntity> {
	private static final Quaternionf ITEM_LIGHT_ROTATION_3D = Axis.XP.rotationDegrees(-15).mul(Axis.YP.rotationDegrees(15));
	private static final Quaternionf ITEM_LIGHT_ROTATION_FLAT = Axis.XP.rotationDegrees(-45);

	@Override
	public void render(ItemAcceptorBlockEntity entity, float tickProgress, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Vec3 cameraPos) {
		if (entity.getLevel() == null || entity.getLevel().getBlockState(entity.getBlockPos()).isAir()) return;
		Minecraft client = Minecraft.getInstance();

		ItemRenderer itemRenderer = context.getItemRenderer();
		TrackingItemStackRenderState renderState = new TrackingItemStackRenderState();
		client.getItemModelResolver().updateForTopItem(renderState, entity.getDisplayItemStack(), ItemDisplayContext.GUI, entity.getLevel(), null, 0);

		// Render item
		float yaw = 0;
		matrices.pushPose();
		matrices.translate(0.5, 0.5, 0.5);
		BlockState blockState = entity.getLevel().getBlockState(entity.getBlockPos());
		if (blockState.is(Glowcase.ITEM_ACCEPTOR_BLOCK.get())) {
			yaw = getRotationYForSide2D(blockState.getValue(ItemAcceptorBlock.FACING));
		}
		matrices.last().pose().mul(new Matrix4f().rotateY(yaw).translate(-0.125f, 0.125f, 0.51F).scale(1, 1, 0.01F));
		matrices.scale(0.5F, 0.5F, 0.5F);

		GpuBufferSlice shaderLights = RenderSystem.getShaderLights();

		if (renderState.usesBlockLight()) {
			matrices.last().normal().rotate(ITEM_LIGHT_ROTATION_3D);
			client.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
		} else {
			matrices.last().normal().rotate(ITEM_LIGHT_ROTATION_FLAT);
			client.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
		}

		itemRenderer.renderStatic(entity.getDisplayItemStack(), ItemDisplayContext.GUI, light, OverlayTexture.NO_OVERLAY, matrices, vertexConsumers, entity.getLevel(), 0);

		//FIXME: is this needed still?
		RenderSystem.setShaderLights(shaderLights);

		// Render count
		if (entity.count > 1) {
			float scale = 0.0625F;
			matrices.translate(0, 0, 1);
			matrices.scale(scale, -scale, scale);

			Font textRenderer = context.getFont();
			String string = String.valueOf(entity.count);
			textRenderer.drawInBatch(string, 9 - textRenderer.width(string), 1, CommonColors.WHITE, false, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, Lightmap.FULL_BRIGHT);
		}

		matrices.popPose();
	}

	private static float getRotationYForSide2D(Direction side) {
		return -side.toYRot() * (float) Math.PI / 180f;
	}
}
