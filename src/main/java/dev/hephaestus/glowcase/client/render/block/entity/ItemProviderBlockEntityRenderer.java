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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jspecify.annotations.Nullable;

public record ItemProviderBlockEntityRenderer(
	BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<ItemProviderBlockEntity, ItemProviderBlockEntityRenderer.ItemProviderRenderState> {
	public static Identifier ITEM_TEXTURE = Glowcase.id("textures/item/item_provider_block.png");

	public static class ItemProviderRenderState extends BlockEntityRenderState {
		public ItemStackRenderState itemRenderState = new ItemStackRenderState();
		public Direction facing = Direction.UP;
		public boolean shouldRenderPlaceholder;
		public boolean isBlockItem;
		public boolean isInvisible;
		public Component name = Component.empty();
		public int textColor;
		public boolean canGive;
		public Component countText;
	}

	@Override
	public ItemProviderRenderState createRenderState() {
		return new ItemProviderRenderState();
	}

	@Override
	public void extractRenderState(ItemProviderBlockEntity blockEntity, ItemProviderRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

		var stack = blockEntity.getStack();
		this.context.itemModelResolver().updateForTopItem(state.itemRenderState, stack, ItemDisplayContext.FIXED, blockEntity.getLevel(), null, (int) state.blockPos.asLong());
		state.facing = blockEntity.getBlockState().getValue(ItemProviderBlock.FACING);
		state.isBlockItem = stack.getItem() instanceof BlockItem;
		state.shouldRenderPlaceholder = !blockEntity.hasItem() || BlockEntityRenderUtil.shouldRenderPlaceholder(blockEntity.getBlockPos());
		state.isInvisible = blockEntity.isInvisible();
		state.name = stack.isEmpty() ? Component.translatable("gui.glowcase.none") : (Component.literal("")).append(stack.getHoverName()).withStyle(stack.getRarity().color());
		state.textColor = ARGB.opaque(state.name.getStyle().getColor() == null ? 0xFFFFFF : state.name.getStyle().getColor().getValue());
		state.canGive = blockEntity.canGiveTo(Minecraft.getInstance().player);
		if (state.canGive) {
			state.countText = Component.literal("%dx".formatted(blockEntity.getStack().getCount()));
		} else {
			state.countText = switch (blockEntity.getGivesItem()) {
				case ONE -> Component.literal("[MAX]").withStyle(ChatFormatting.YELLOW);
				case TIMED -> {
					final long cooldownMS = blockEntity.getCooldownTicks(Minecraft.getInstance().player) * 50;
					if (cooldownMS <= 0) {
						yield Component.literal("[??:??]").withStyle(ChatFormatting.RED);
					}
					yield Component.literal("[%s]".formatted(DurationFormatUtils.formatDuration(cooldownMS, cooldownMS > 3600000 ? "HH:mm:ss" : "mm:ss"))).withStyle(ChatFormatting.YELLOW);
				}
				case ALWAYS ->
					Component.literal("[I'm sorry, I'm actually the item unprovider.]").withStyle(ChatFormatting.RED);
			};
		}
	}

	@Override
	public void submit(ItemProviderRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5D, 0D, 0.5D);

		float yaw = 0F;
		float pitch = 0F;

		boolean isBack = false;
		boolean isBillboard = false;

		switch (state.facing) {
			case DOWN, UP -> {
				if (state.isBlockItem) {
					Vec2 pitchAndYaw = BlockEntityRenderUtil.getTracking(camera.pos, state.blockPos);
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

			state.itemRenderState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

			poseStack.popPose();
		}

		HitResult hitResult = Minecraft.getInstance().hitResult;
		if (hitResult instanceof BlockHitResult && ((BlockHitResult) hitResult).getBlockPos().equals(state.blockPos)) {
			poseStack.pushPose();
			if (isBack) { // Dunno, poseStack are hard
				poseStack.mulPose(Axis.XP.rotationDegrees(180));
			} else {
				poseStack.mulPose(Axis.ZP.rotationDegrees(180));
			}

			poseStack.translate(0, -0.6, -0.3);
			float scale = 0.025F;
			poseStack.scale(scale, scale, scale);

			Component name = state.name;
			int color = ARGB.opaque(name.getStyle().getColor() == null ? 0xFFFFFF : name.getStyle().getColor().getValue());

			poseStack.pushPose();
			poseStack.translate(-context.font().width(name) / 2F, -4, 0);
			// Shadow fix - consider a helper function for this?
			poseStack.scale(1, 1, -1);
			submitNodeCollector.submitText(poseStack, 0, 0, state.name.getVisualOrderText(), true, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, color, 0, 0);

			poseStack.popPose();

			if (!state.itemRenderState.isEmpty()) {
				poseStack.pushPose();
				poseStack.translate(-context.font().width(state.countText) + 16, state.canGive ? 32 : 24, 0);
				poseStack.scale(1, 1, -1);
				submitNodeCollector.submitText(poseStack, 0, 0, state.countText.getVisualOrderText(), true, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, color, 0, 0);
				poseStack.popPose();
			}
			poseStack.popPose();
		}

		poseStack.popPose();


		if (state.shouldRenderPlaceholder) {
			if (isBack) {
				BlockEntityRenderUtil.renderFacingPlaceholder(state, state.facing, ITEM_TEXTURE, 1.0F, poseStack, submitNodeCollector);
			} else if (isBillboard) {
				BlockEntityRenderUtil.renderBillboardPlaceholder(state, ITEM_TEXTURE, 1.0F, poseStack, submitNodeCollector, camera);
			} else {
				BlockEntityRenderUtil.renderTrackingPlaceholder(state, ITEM_TEXTURE, 1.0F, poseStack, submitNodeCollector, camera.pos);
			}
		}
	}
}
