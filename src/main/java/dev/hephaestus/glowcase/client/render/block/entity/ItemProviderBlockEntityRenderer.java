package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.ItemProviderBlock;
import dev.hephaestus.glowcase.block.entity.ItemProviderBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record ItemProviderBlockEntityRenderer(
	BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<ItemProviderBlockEntity, ItemProviderBlockEntityRenderer.ItemProviderRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/item_provider_block.png");

	public static class ItemProviderRenderState extends BlockEntityRenderState {
		public Direction facing = Direction.UP;
		public boolean shouldRenderPlaceholder;
		public boolean isBlockItem;
		public boolean isInvisible;
	}

	@Override
	public ItemProviderRenderState createRenderState() {
		return new ItemProviderRenderState();
	}

	@Override
	public void extractRenderState(ItemProviderBlockEntity blockEntity, ItemProviderRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

		state.facing = blockEntity.getBlockState().getValue(ItemProviderBlock.FACING);
		state.isBlockItem = blockEntity.getStack().getItem() instanceof BlockItem;
		state.shouldRenderPlaceholder = !blockEntity.hasItem() || BlockEntityRenderUtil.shouldRenderPlaceholder(blockEntity.getBlockPos());
		state.isInvisible = blockEntity.isInvisible();
	}

	@Override
	public void submit(ItemProviderRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		Entity cameraEntity = Minecraft.getInstance().getCameraEntity();

		poseStack.pushPose();
		poseStack.translate(0.5D, 0D, 0.5D);

		float yaw = 0F;
		float pitch = 0F;

		boolean isBack = false;
		boolean isBillboard = false;

		switch (state.facing) {
			case DOWN, UP -> {
				if (state.isBlockItem) {
					Vec2 pitchAndYaw = BlockEntityRenderUtil.getTracking(cameraEntity, state.blockPos, /* FIXME tickDelta */ 0);
					pitch = pitchAndYaw.x;
					yaw = pitchAndYaw.y;
					poseStack.mulPose(Axis.YP.rotation(yaw));
				} else {
					pitch = (float) Math.toRadians(camera.xRot);
					yaw = (float) Math.toRadians(-camera.yRot);
					poseStack.mulPose(Axis.YP.rotation(yaw));
					isBillboard = true;
				}
			}
			default -> {
				poseStack.mulPose(state.facing.getRotation().mul(Axis.XP.rotationDegrees(-90.0F)));
				poseStack.translate(0D, Math.sin(pitch) * -0.4, -0.4D);
				isBack = true;
			}
		}

		poseStack.translate(0, 0.5, 0);
		poseStack.scale(0.5F, 0.5F, 0.5F);
		poseStack.mulPose(Axis.XP.rotation(pitch));

		if (!state.isInvisible) {
			poseStack.pushPose();
			if (state.facing.getAxis() != Direction.Axis.Y) {
				poseStack.mulPose(Axis.YP.rotationDegrees(180f));
			}

//			context.getItemRenderer().renderStatic(entity.getStack(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, vertexConsumers, entity.getLevel(), 0);

			poseStack.popPose();
		}
//
//		HitResult hitResult = Minecraft.getInstance().hitResult;
//		if (hitResult instanceof BlockHitResult && ((BlockHitResult) hitResult).getBlockPos().equals(entity.getBlockPos())) {
//			poseStack.pushPose();
//			if (isBack) { // Dunno, poseStack are hard
//				poseStack.mulPose(Axis.XP.rotationDegrees(180));
//			} else {
//				poseStack.mulPose(Axis.ZP.rotationDegrees(180));
//			}
//			float scale = 0.025F;
//
//			poseStack.translate(0, -0.6, -0.3);
//
//			poseStack.scale(scale, scale, scale);
//
//			ItemStack stack = entity.getStack();
//			Component name = stack.isEmpty() ? Component.translatable("gui.glowcase.none") : (Component.literal("")).append(stack.getHoverName()).withStyle(stack.getRarity().color());
//			int color = ARGB.opaque(name.getStyle().getColor() == null ? 0xFFFFFF : name.getStyle().getColor().getValue());
//			poseStack.pushPose();
//			poseStack.translate(-context.getFont().width(name) / 2F, -4, 0);
//			context.getFont().drawInBatch(name, 0, 0, color, false, poseStack.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, Lightmap.FULL_BRIGHT);
//			poseStack.popPose();
//
//			if (!stack.isEmpty()) {
//				poseStack.pushPose();
//				if (entity.canGiveTo(Minecraft.getInstance().player)) {
//					Component countText = Component.literal("%dx".formatted(entity.getStack().getCount()));
//					poseStack.translate(-context.getFont().width(countText) + 16, 32, 0);
//					context.getFont().drawInBatch(countText, 0, 0, 0xFFFFFFFF, false, poseStack.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, Lightmap.FULL_BRIGHT);
//				} else {
//					long cooldownMS = entity.getCooldownTicks(Minecraft.getInstance().player) * 50;
//					Component countText = Component.literal("[%s]".formatted(entity.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED ? DurationFormatUtils.formatDuration(cooldownMS, cooldownMS > 3600000 ? "HH:mm:ss" : "mm:ss") : "MAX")).withStyle(ChatFormatting.YELLOW);
//					poseStack.translate(-context.getFont().width(countText) + 16, 24, 0);
//					context.getFont().drawInBatch(countText, 0, 0, 0xFFFFFFFF, false, poseStack.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, Lightmap.FULL_BRIGHT);
//				}
//				poseStack.popPose();
//			}
//			poseStack.popPose();
//		}
//
		poseStack.popPose();


		if (state.shouldRenderPlaceholder) {
			if (isBack) {
				BlockEntityRenderUtil.renderFacingPlaceholder(state, state.facing, ITEM_TEXTURE, 1.0F, poseStack, submitNodeCollector);
			} else if (isBillboard) {
				BlockEntityRenderUtil.renderBillboardPlaceholder(state, ITEM_TEXTURE, 1.0F, poseStack, submitNodeCollector, camera);
			} else {
				BlockEntityRenderUtil.renderTrackingPlaceholder(state, ITEM_TEXTURE, 1.0F, poseStack, submitNodeCollector, cameraEntity, /* FIXME tickDelta */ 0);
			}
		}
	}
}
