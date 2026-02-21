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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public record ItemAcceptorBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<ItemAcceptorBlockEntity, ItemAcceptorBlockEntityRenderer.ItemAcceptorRenderState> {
	private static final Quaternionf ITEM_LIGHT_ROTATION_3D = Axis.XP.rotationDegrees(-15).mul(Axis.YP.rotationDegrees(15));
	private static final Quaternionf ITEM_LIGHT_ROTATION_FLAT = Axis.XP.rotationDegrees(-45);

	public static class ItemAcceptorRenderState extends BlockEntityRenderState {
		public int count;
	}

	@Override
	public ItemAcceptorRenderState createRenderState() {
		return new ItemAcceptorRenderState();
	}

	@Override
	public void extractRenderState(ItemAcceptorBlockEntity blockEntity, ItemAcceptorRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		state.count = blockEntity.count;
	}

	@Override
	public void submit(ItemAcceptorRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		Minecraft client = Minecraft.getInstance();

		ItemRenderer itemRenderer = context.itemRenderer();
		TrackingItemStackRenderState renderState = new TrackingItemStackRenderState();

// FIXME 26.1
//		client.getItemModelResolver().updateForTopItem(renderState, entity.getDisplayItemStack(), ItemDisplayContext.GUI, entity.getLevel(), null, 0);

		// Render item
		float yaw = 0;
		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
// FIXME 26.1
//		BlockState blockState = entity.getLevel().getBlockState(entity.getBlockPos());
//		if (blockState.is(Glowcase.ITEM_ACCEPTOR_BLOCK.get())) {
//			yaw = getRotationYForSide2D(blockState.getValue(ItemAcceptorBlock.FACING));
//		}
		poseStack.last().pose().mul(new Matrix4f().rotateY(yaw).translate(-0.125f, 0.125f, 0.51F).scale(1, 1, 0.01F));
		poseStack.scale(0.5F, 0.5F, 0.5F);

		GpuBufferSlice shaderLights = RenderSystem.getShaderLights();

		if (renderState.usesBlockLight()) {
			poseStack.last().normal().rotate(ITEM_LIGHT_ROTATION_3D);
			client.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
		} else {
			poseStack.last().normal().rotate(ITEM_LIGHT_ROTATION_FLAT);
			client.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
		}
// FIXME 26.1
//		itemRenderer.renderStatic(entity.getDisplayItemStack(), ItemDisplayContext.GUI, light, OverlayTexture.NO_OVERLAY, poseStack, vertexConsumers, entity.getLevel(), 0);

		//FIXME: is this needed still?
		RenderSystem.setShaderLights(shaderLights);

		// Render count
		if (state.count > 1) {
			float scale = 0.0625F;
			poseStack.translate(0, 0, 1);
			poseStack.scale(scale, -scale, scale);

			Font textRenderer = context.font();
			String string = String.valueOf(state.count);

			submitNodeCollector.submitText(poseStack, 9 - textRenderer.width(string), 1, Component.literal(string).getVisualOrderText(), true, Font.DisplayMode.NORMAL, 0xFF, 0xFFFFFF, 0, 0);
		}

		poseStack.popPose();
	}


	private static float getRotationYForSide2D(Direction side) {
		return -side.toYRot() * (float) Math.PI / 180f;
	}
}
