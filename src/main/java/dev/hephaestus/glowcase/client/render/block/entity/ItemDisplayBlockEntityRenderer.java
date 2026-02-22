package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ItemDisplayBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public record ItemDisplayBlockEntityRenderer(
	BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<ItemDisplayBlockEntity, ItemDisplayBlockEntityRenderer.ItemDisplayRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/item_display_block.png");

	public static class ItemDisplayRenderState extends BlockEntityRenderState {
		public ItemStackRenderState itemRenderState = new ItemStackRenderState();
		public boolean renderAsBlock;
		public boolean shouldRenderPlaceholder;
		public float yaw;
		public float pitch;
		public Vector3f offset = new Vector3f(0, 0, 0);
		public Vector3f scale = new Vector3f(0, 0, 0);
	}

	@Override
	public ItemDisplayRenderState createRenderState() {
		return new ItemDisplayRenderState();
	}

	@Override
	public void extractRenderState(ItemDisplayBlockEntity blockEntity, ItemDisplayRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

		this.context.itemModelResolver().updateForTopItem(state.itemRenderState, blockEntity.getStack(), ItemDisplayContext.FIXED, blockEntity.getLevel(), null, (int) state.blockPos.asLong());
		state.renderAsBlock = blockEntity.getRenderAsBlock() && blockEntity.getStack().getItem() instanceof BlockItem blockItem;
		state.shouldRenderPlaceholder = blockEntity.matchesStack(ItemStack.EMPTY) || BlockEntityRenderUtil.shouldRenderPlaceholder(blockEntity.getBlockPos());
		state.yaw = blockEntity.getYaw();
		state.pitch = blockEntity.getPitch();
		state.offset = blockEntity.getOffset();
		state.scale = blockEntity.getScale();
	}

	@Override
	public void submit(ItemDisplayRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5D, 0D, 0.5D);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F + state.yaw));
		poseStack.translate(state.offset.x(), state.offset.y(), state.offset.z());

		if (state.renderAsBlock) {
			//		FIXME 26.1
//			poseStack.translate(-0.5D, -0.5D, -0.5D);
//
//			poseStack.scale(entity.getScale().x(), entity.getScale().y(), entity.getScale().z());
//			poseStack.mulPose(Axis.XP.rotationDegrees(entity.getPitch()));
//
//			Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockItem.getBlock().defaultBlockState(), poseStack, vertexConsumers, light, overlay);
		} else {
			poseStack.translate(0D, 0.5D, 0D);

			poseStack.scale(state.scale.x(), state.scale.y(), state.scale.z());
			poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));

			state.itemRenderState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		}

		poseStack.popPose();

		if (state.shouldRenderPlaceholder) {
			BlockEntityRenderUtil.renderCenteredPlaceholder(state, ITEM_TEXTURE, 1.0F, Axis.YP.rotationDegrees(state.yaw), poseStack, submitNodeCollector);
		}
	}
}
