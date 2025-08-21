package dev.hephaestus.glowcase.client.render.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.ItemProviderBlock;
import dev.hephaestus.glowcase.block.entity.ItemProviderBlockEntity;
import dev.hephaestus.glowcase.client.util.BlockEntityRenderUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.ColorHelper;
import org.apache.commons.lang3.time.DurationFormatUtils;

public record ItemProviderBlockEntityRenderer(BlockEntityRendererFactory.Context context) implements BlockEntityRenderer<ItemProviderBlockEntity> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/item_provider_block.png");

	@Override
	public void render(ItemProviderBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, Vec3d cameraPos) {
		if (entity.getWorld() == null || entity.getWorld().getBlockState(entity.getPos()).isAir()) return;
		Entity camera = MinecraftClient.getInstance().getCameraEntity();
		BlockState blockState = entity.getWorld().getBlockState(entity.getPos());

		if (camera == null) return;

		matrices.push();
		matrices.translate(0.5D, 0D, 0.5D);

		float yaw = 0F;
		float pitch = 0F;

		boolean isBack = false;
		boolean isBillboard = false;

		Direction facing = Direction.UP;

		if (blockState.isOf(Glowcase.ITEM_PROVIDER_BLOCK.get())) {
			facing = blockState.get(ItemProviderBlock.FACING);
		}

		switch (facing) {
			case DOWN, UP -> {
				if (entity.getStack().getItem() instanceof BlockItem) {
					Vec2f pitchAndYaw = BlockEntityRenderUtil.getTracking(camera, entity.getPos(), tickDelta);
					pitch = pitchAndYaw.x;
					yaw = pitchAndYaw.y;
					matrices.multiply(RotationAxis.POSITIVE_Y.rotation(yaw));
				} else {
					pitch = (float) Math.toRadians(camera.getPitch());
					yaw = (float) Math.toRadians(-camera.getYaw());
					matrices.multiply(RotationAxis.POSITIVE_Y.rotation(yaw));
					isBillboard = true;
				}
			}
			default -> {
				matrices.multiply(facing.getRotationQuaternion().mul(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F)));
				matrices.translate(0D, Math.sin(pitch) * -0.4, -0.4D);
				isBack = true;
			}
		}

			matrices.translate(0, 0.5, 0);
			matrices.scale(0.5F, 0.5F, 0.5F);
			matrices.multiply(RotationAxis.POSITIVE_X.rotation(pitch));

		if (!entity.isInvisible()) {
			matrices.push();
			if (facing.getAxis() != Direction.Axis.Y) {
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
			}

			context.getItemRenderer().renderItem(entity.getStack(), ItemDisplayContext.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 0);
			matrices.pop();
		}

		HitResult hitResult = MinecraftClient.getInstance().crosshairTarget;
		if (hitResult instanceof BlockHitResult && ((BlockHitResult) hitResult).getBlockPos().equals(entity.getPos())) {
			matrices.push();
			if (isBack) { // Dunno, matrices are hard
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
			} else {
				matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));
			}
			float scale = 0.025F;

			matrices.translate(0, -0.6, -0.3);

			matrices.scale(scale, scale, scale);

			ItemStack stack = entity.getStack();
			Text name = stack.isEmpty() ? Text.translatable("gui.glowcase.none") : (Text.literal("")).append(stack.getName()).formatted(stack.getRarity().getFormatting());
			int color = ColorHelper.fullAlpha(name.getStyle().getColor() == null ? 0xFFFFFF : name.getStyle().getColor().getRgb());
			matrices.push();
			matrices.translate(-context.getTextRenderer().getWidth(name) / 2F, -4, 0);
			context.getTextRenderer().draw(name, 0, 0, color, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
			matrices.pop();

			if (!stack.isEmpty()) {
				matrices.push();
				if (entity.canGiveTo(MinecraftClient.getInstance().player)) {
					Text countText = Text.literal("%dx".formatted(entity.getStack().getCount()));
					matrices.translate(-context.getTextRenderer().getWidth(countText) + 16, 32, 0);
					context.getTextRenderer().draw(countText, 0, 0, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
				} else {
					long cooldownMS = entity.getCooldownTicks(MinecraftClient.getInstance().player) * 50;
					Text countText = Text.literal("[%s]".formatted(entity.getGivesItem() == ItemProviderBlockEntity.GivesItem.TIMED ? DurationFormatUtils.formatDuration(cooldownMS, cooldownMS > 3600000 ? "HH:mm:ss" : "mm:ss") : "MAX")).formatted(Formatting.YELLOW);
					matrices.translate(-context.getTextRenderer().getWidth(countText) + 16, 24, 0);
					context.getTextRenderer().draw(countText, 0, 0, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
				}
				matrices.pop();
			}
			matrices.pop();
		}

		matrices.pop();

		if (!entity.hasItem() || BlockEntityRenderUtil.shouldRenderPlaceholder(entity.getPos())) {
			if (isBack) {
				BlockEntityRenderUtil.renderFacingPlaceholder(entity, ITEM_TEXTURE, 1.0F, matrices, vertexConsumers);
			} else if (isBillboard) {
				BlockEntityRenderUtil.renderBillboardPlaceholder(entity, ITEM_TEXTURE, 1.0F, matrices, vertexConsumers, context.getRenderDispatcher().camera);
			} else {
				BlockEntityRenderUtil.renderTrackingPlaceholder(entity, ITEM_TEXTURE, 1.0F, matrices, vertexConsumers, camera, tickDelta);
			}
		}
	}
}
