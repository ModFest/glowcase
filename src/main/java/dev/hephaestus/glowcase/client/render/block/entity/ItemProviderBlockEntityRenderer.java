package dev.hephaestus.glowcase.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.ItemProviderBlock;
import dev.hephaestus.glowcase.block.entity.ItemProviderBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.time.DurationFormatUtils;

public record ItemProviderBlockEntityRenderer(BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<ItemProviderBlockEntity> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/item_provider_block.png");

	@Override
	public void render(ItemProviderBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, Vec3 cameraPos) {
		if (entity.getLevel() == null || entity.getLevel().getBlockState(entity.getBlockPos()).isAir()) return;
		Entity camera = Minecraft.getInstance().getCameraEntity();
		BlockState blockState = entity.getLevel().getBlockState(entity.getBlockPos());

		if (camera == null) return;

		matrices.pushPose();
		matrices.translate(0.5D, 0D, 0.5D);

		float yaw = 0F;
		float pitch = 0F;

		boolean isBack = false;
		boolean isBillboard = false;

		Direction facing = Direction.UP;

		if (blockState.is(Glowcase.ITEM_PROVIDER_BLOCK.get())) {
			facing = blockState.getValue(ItemProviderBlock.FACING);
		}

		switch (facing) {
			case DOWN, UP -> {
				if (entity.getStack().getItem() instanceof BlockItem) {
					Vec2 pitchAndYaw = BlockEntityRenderUtil.getTracking(camera, entity.getBlockPos(), tickDelta);
					pitch = pitchAndYaw.x;
					yaw = pitchAndYaw.y;
					matrices.mulPose(Axis.YP.rotation(yaw));
				} else {
					pitch = (float) Math.toRadians(camera.getXRot());
					yaw = (float) Math.toRadians(-camera.getYRot());
					matrices.mulPose(Axis.YP.rotation(yaw));
					isBillboard = true;
				}
			}
			default -> {
				matrices.mulPose(facing.getRotation().mul(Axis.XP.rotationDegrees(-90.0F)));
				matrices.translate(0D, Math.sin(pitch) * -0.4, -0.4D);
				isBack = true;
			}
		}

			matrices.translate(0, 0.5, 0);
			matrices.scale(0.5F, 0.5F, 0.5F);
			matrices.mulPose(Axis.XP.rotation(pitch));

		if (!entity.isInvisible()) {
			matrices.pushPose();
			if (facing.getAxis() != Direction.Axis.Y) {
				matrices.mulPose(Axis.YP.rotationDegrees(180f));
			}

			context.getItemRenderer().renderStatic(entity.getStack(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, matrices, vertexConsumers, entity.getLevel(), 0);
			matrices.popPose();
		}

		HitResult hitResult = Minecraft.getInstance().hitResult;
		if (hitResult instanceof BlockHitResult && ((BlockHitResult) hitResult).getBlockPos().equals(entity.getBlockPos())) {
			matrices.pushPose();
			if (isBack) { // Dunno, matrices are hard
				matrices.mulPose(Axis.XP.rotationDegrees(180));
			} else {
				matrices.mulPose(Axis.ZP.rotationDegrees(180));
			}
			float scale = 0.025F;

			matrices.translate(0, -0.6, -0.3);

			matrices.scale(scale, scale, scale);

			ItemStack stack = entity.getStack();
			Component name = stack.isEmpty() ? Component.translatable("gui.glowcase.none") : (Component.literal("")).append(stack.getHoverName()).withStyle(stack.getRarity().color());
			int color = ARGB.opaque(name.getStyle().getColor() == null ? 0xFFFFFF : name.getStyle().getColor().getValue());
			matrices.pushPose();
			matrices.translate(-context.getFont().width(name) / 2F, -4, 0);
			context.getFont().drawInBatch(name, 0, 0, color, false, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, Lightmap.FULL_BRIGHT);
			matrices.popPose();

			if (!stack.isEmpty()) {
				matrices.pushPose();
				if (entity.canGiveTo(Minecraft.getInstance().player)) {
					Component countText = Component.literal("%dx".formatted(entity.getStack().getCount()));
					matrices.translate(-context.getFont().width(countText) + 16, 32, 0);
					context.getFont().drawInBatch(countText, 0, 0, 0xFFFFFFFF, false, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, Lightmap.FULL_BRIGHT);
				} else {
					long cooldownMS = entity.getCooldownTicks(Minecraft.getInstance().player) * 50;
					Component countText = Component.literal("[%s]".formatted(entity.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED ? DurationFormatUtils.formatDuration(cooldownMS, cooldownMS > 3600000 ? "HH:mm:ss" : "mm:ss") : "MAX")).withStyle(ChatFormatting.YELLOW);
					matrices.translate(-context.getFont().width(countText) + 16, 24, 0);
					context.getFont().drawInBatch(countText, 0, 0, 0xFFFFFFFF, false, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, Lightmap.FULL_BRIGHT);
				}
				matrices.popPose();
			}
			matrices.popPose();
		}

		matrices.popPose();

		if (!entity.hasItem() || BlockEntityRenderUtil.shouldRenderPlaceholder(entity.getBlockPos())) {
			if (isBack) {
				BlockEntityRenderUtil.renderFacingPlaceholder(entity, ITEM_TEXTURE, 1.0F, matrices, vertexConsumers);
			} else if (isBillboard) {
				BlockEntityRenderUtil.renderBillboardPlaceholder(entity, ITEM_TEXTURE, 1.0F, matrices, vertexConsumers, context.getBlockEntityRenderDispatcher().camera);
			} else {
				BlockEntityRenderUtil.renderTrackingPlaceholder(entity, ITEM_TEXTURE, 1.0F, matrices, vertexConsumers, camera, tickDelta);
			}
		}
	}
}
